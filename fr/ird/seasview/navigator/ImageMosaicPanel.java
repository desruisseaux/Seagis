/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.seasview.navigator;

// Database
import java.sql.SQLException;
import fr.ird.seasview.DataBase;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.image.SeriesEntry;

// User interface
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import fr.ird.awt.RangeSet;
import fr.ird.awt.RangeBars;
import fr.ird.seasview.layer.control.LayerControl;

// Events and models
import javax.swing.BoundedRangeModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Collections and time
import java.util.Date;
import java.util.TimeZone;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Formats and miscellaneous
import java.text.DateFormat;
import java.text.ParseException;
import javax.media.jai.util.Range;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.gui.swing.StatusBar;
import org.geotools.resources.SwingUtilities;
import org.geotools.gui.swing.ExceptionMonitor;

// Resources
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Paneau pouvant contenir une mosaïque d'images satellitaires.  Ces images seront
 * de plusieurs types (température, vorticité, etc...). Un glissoir dans le bas de
 * la fenêtre permettra de sélectionner la date pour laquelle on souhaite voir les
 * images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ImageMosaicPanel extends ImagePanel { //implements ChangeListener
    /**
     * Nombres de lignes et de colonnes à donner à la mosaïque en
     * fonction du nombre d'images.  On fixera ce nombre une fois
     * pour toute lors de la construction afin d'éviter des
     * changements trop fréquents lors des redimmensionnement.
     */
    private static final byte[] SIZE = {
        1,1 , 1,2 , 1,3 , 2,2 , 2,3 , 2,3 , 2,4 , 2,4 , 3,3
    };

    /**
     * Largeur par défaut de la plage de dates.
     * La valeur par défaut est de 5 jours.
     */
    private static final long TIME_RANGE = 5*24*60*60*1000L;

    /**
     * Graphiques représentant les plages de temps des données
     * disponibles. L'utilisateur pourra choisir les données à
     * afficher en faisant glisser une visière sur ce graphique.
     */
    private final RangeBars rangeBars = new RangeBars(TimeZone.getDefault(),
                                                      RangeBars.VERTICAL_EXCEPT_LABELS);

    /**
     * Connection avec la base de données d'images.
     */
    private final ImageTable table;

    /**
     * Liste des séries apparaissant dans cette mosaïque.
     */
    private final List<SeriesEntry> series = new ArrayList<SeriesEntry>();

    /**
     * Construit un panneau pour une mosaïque d'images. De la place sera réservée
     * pour des images et sa rampe de couleur, mais seront initialement vide.
     *
     * @param table     Connection avec la base de données d'images.
     * @param statusBar Barre d'état qui apparaîtra dans le bas de la fenêtre.
     * @param readers   Sous-groupe de <code>threads</code>Groupe dans lequel placer
     *                  les threads qui liront des images en arrière-plan.
     * @param layers    Couches que l'utilisateur pourra placer sur les images,
     *                  ou <code>null</code> pour n'afficher que les images.
     */
    public ImageMosaicPanel(final ImageTable    table, final StatusBar statusBar,
                            final ThreadGroup readers, final LayerControl[] layers)
    {
        super(statusBar, readers, layers);
        this.table = table;
        //
        // Construction de l'interface
        //
        final Resources  resources = Resources.getResources(null);
        final String     startTime = resources.getString(ResourceKeys.START_TIME);
        final String       endTime = resources.getString(ResourceKeys.  END_TIME);
        final JSplitPane controler = (JSplitPane) getLeftComponent();
        final JComponent      bars = rangeBars.createCombinedPanel(null, startTime, endTime);
        final Dimension       size = bars.getPreferredSize(); size.height=100;
        this     .setRightComponent(mosaic);
        controler.setTopComponent(bars);
        bars     .setMinimumSize(size);
        rangeBars.setLegendVisible(false);
        rangeBars.setRangeAdjustable(false);
        rangeBars.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent event) {
                slidderChanged((BoundedRangeModel) event.getSource());
            }
        });
    }

    /**
     * Met à jour le texte de la barre d'état.  Cette méthode est
     * appelée automatiquement par exemple lorsque l'onglet de ce
     * panneau a été sélectionné.
     */
    protected void updateStatusBar() {
        if (isShowing()) {
            // Si c'est un autre panneau qui est visible,
            // alors la barre d'état ne nous appartient pas.
            mosaic.statusBar.setText(null);
        }
    }

    /**
     * Remet à jour toutes les données de cette composante. Cette méthode
     * peut être appelée de n'importe quel thread (pas nécessairement celui
     * de Swing).
     *
     * @param  table Table à utiliser. Toutes les coordonnées spatio-temporelles
     *         de cette table doivent correspondre à la zone d'intérêt. La série
     *         active sera écrasée.
     * @throws SQLException si une erreur est survenue lors de l'accès à la
     *         base de données.
     */
    public Map<SeriesEntry,List<ImageEntry>> refresh(final ImageTable table) throws SQLException {
        final Map<SeriesEntry,List<ImageEntry>> entries;
        entries = new HashMap<SeriesEntry,List<ImageEntry>>();
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                series.clear();
                rangeBars.clear();
                mosaic.removeAllImages();
            }
        });
        synchronized (table) {
            for (final Iterator<SeriesEntry> it=series.iterator(); it.hasNext();) {
                final SeriesEntry series = it.next();
                table.setSeries(series);
                entries.put(series, addSeriesImpl(table));
            }
        }
        layoutMosaic();
        return entries;
    }

    /**
     * Ajoute une série à la liste des séries déjà en mémoire. Cette méthode
     * peut être appelée de n'importe quel thread (pas nécessairement celui
     * de Swing).
     *
     * @param  table Table à utiliser. Toutes les coordonnées spatio-temporelles
     *         de cette table doivent correspondre à la zone d'intérêt. La série
     *         active de la table doit correspondre à la série à ajouter.
     * @return Liste des entrées qui ont été ajoutées.
     * @throws SQLException si une erreur est survenue lors de l'accès à la
     *         base de données.
     */
    public List<ImageEntry> addSeries(final ImageTable table) throws SQLException {
        final List<ImageEntry> entries = addSeriesImpl(table);
        // Fixe une fois pour toute le nombre de
        // lignes et de colonnes de la mosaïque.
        layoutMosaic();
        return entries;
    }

    /**
     * Ajoute une série à la liste des séries déjà en mémoire. Cette méthode
     * peut être appelée de n'importe quel thread (pas nécessairement celui
     * de Swing).
     *
     * @param  table Table à utiliser. Toutes les coordonnées spatio-temporelles
     *         de cette table doivent correspondre à la zone d'intérêt. La série
     *         active de la table doit correspondre à la série à ajouter.
     * @return Liste des entrées qui ont été ajoutées.
     * @throws SQLException si une erreur est survenue lors de l'accès à la
     *         base de données.
     */
    private List<ImageEntry> addSeriesImpl(final ImageTable table) throws SQLException {
        final SeriesEntry    newSeries;
        final RangeSet          ranges = new RangeSet(Date.class);
        final List<ImageEntry> entries = new ArrayList<ImageEntry>();
        synchronized (table) {
            newSeries = table.getSeries();
            table.getRanges(null, null, ranges, entries);
        }
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                rangeBars.setRanges(newSeries.getName(), ranges);
                series.add(newSeries);
            }
        });
        return entries;
    }

    /**
     * Réarrange la mosaïque d'images. Cette méthode peut être appelée
     * de n'importe quel thread (pas nécessairement celui de Swing).
     */
    private void layoutMosaic() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                final int size = series.size();
                final int i = size*2;
                if (i < SIZE.length) {
                    mosaic.setGridSize(SIZE[i],SIZE[i+1]);
                }
                mosaic.setImageCount(size);
                final int check;
                assert size == (check=mosaic.getImageCount()) : "series="+size+" images="+check;
                if (size==1) {
                    resetSliderPosition();
                }
            }
        });
    }

    /**
     * Reset the slidder position.
     */
    private void resetSliderPosition() {
        final Date minimum = (Date) rangeBars.getMinimum();
        final Date maximum = (Date) rangeBars.getMaximum();
        if (minimum!=null && maximum!=null) {
            final long startTime  = minimum.getTime();
            final long   endTime  = maximum.getTime();
            rangeBars.setSelectedRange(Math.max(startTime, endTime-TIME_RANGE), endTime);
        }
    }

    /**
     * Méthode appelée automatiquement chaque fois
     * que l'utilisateur change la position de la
     * visière.
     */
    private void slidderChanged(final BoundedRangeModel model) {
        if (!model.getValueIsAdjusting()) {
            if (javax.swing.SwingUtilities.getWindowAncestor(this) == null) {
                /*
                 * Ne charge pas d'image si la composante n'apparaît pas encore dans une
                 * fenêtre. Ca nous évite de gaspiller de la mémoire et du CPU à charger
                 * une image que l'utilisateur n'a même pas encore demandée, lorsque cette
                 * méthode est appelée pendant la construction de l'interface utilisateur.
                 */
                return;
            }
            final Date startTime=new Date(Math.round(rangeBars.getMinSelectedValue()));
            final Date   endTime=new Date(Math.round(rangeBars.getMaxSelectedValue()));
            synchronized (table) {
                try {
                    final Range oldRange = table.getTimeRange();
                    try {
                        table.setTimeRange(startTime, endTime);
                        /*
                         * Pour chaque série, obtient une image qui
                         * intercepte la plage de dates spécifiées.
                         */
                        final int length = series.size();
                        final int check;
                        assert length == (check=mosaic.getImageCount()) : "series="+length+" images="+check;
                        for (int i=0; i<length; i++) {
                            table.setSeries(series.get(i));
                            final ImageEntry   entry = table.getEntry();
                            final ImageCanvas canvas = mosaic.getImage(i);
                            if (canvas != null) {
                                if (entry != null) {
                                    canvas.setImage(entry, getSelectedLayers(),
                                      mosaic.statusBar.getIIOReadProgressListener(entry.getName()));
                                } else {
                                    canvas.setImage((GridCoverage)null);
                                }
                            }
                        }
                    } finally {
                        table.setTimeRange(oldRange);
                    }
                } catch (SQLException exception) {
                    ExceptionMonitor.show(this, exception);
                }
            }
        }
    }

    /**
     * Modifie le fuseau horaire pour l'affichage et la saisie des dates.
     * Cette modification n'affecte pas le fuseau horaire des éventuelles
     * bases de données accédées par cette fenêtre.
     */
    protected void setTimeZone(final TimeZone timezone) {
        super    .setTimeZone(timezone);
        rangeBars.setTimeZone(timezone);
    }
}
