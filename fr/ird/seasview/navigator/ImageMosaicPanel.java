/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
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
 * Paneau pouvant contenir une mosa�que d'images satellitaires.  Ces images seront
 * de plusieurs types (temp�rature, vorticit�, etc...). Un glissoir dans le bas de
 * la fen�tre permettra de s�lectionner la date pour laquelle on souhaite voir les
 * images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ImageMosaicPanel extends ImagePanel { //implements ChangeListener
    /**
     * Nombres de lignes et de colonnes � donner � la mosa�que en
     * fonction du nombre d'images.  On fixera ce nombre une fois
     * pour toute lors de la construction afin d'�viter des
     * changements trop fr�quents lors des redimmensionnement.
     */
    private static final byte[] SIZE = {
        1,1 , 1,2 , 1,3 , 2,2 , 2,3 , 2,3 , 2,4 , 2,4 , 3,3
    };

    /**
     * Largeur par d�faut de la plage de dates.
     * La valeur par d�faut est de 5 jours.
     */
    private static final long TIME_RANGE = 5*24*60*60*1000L;

    /**
     * Graphiques repr�sentant les plages de temps des donn�es
     * disponibles. L'utilisateur pourra choisir les donn�es �
     * afficher en faisant glisser une visi�re sur ce graphique.
     */
    private final RangeBars rangeBars = new RangeBars(TimeZone.getDefault(),
                                                      RangeBars.VERTICAL_EXCEPT_LABELS);

    /**
     * Connection avec la base de donn�es d'images.
     */
    private final ImageTable table;

    /**
     * Liste des s�ries apparaissant dans cette mosa�que.
     */
    private final List<SeriesEntry> series = new ArrayList<SeriesEntry>();

    /**
     * Construit un panneau pour une mosa�que d'images. De la place sera r�serv�e
     * pour des images et sa rampe de couleur, mais seront initialement vide.
     *
     * @param table     Connection avec la base de donn�es d'images.
     * @param statusBar Barre d'�tat qui appara�tra dans le bas de la fen�tre.
     * @param readers   Sous-groupe de <code>threads</code>Groupe dans lequel placer
     *                  les threads qui liront des images en arri�re-plan.
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
     * Met � jour le texte de la barre d'�tat.  Cette m�thode est
     * appel�e automatiquement par exemple lorsque l'onglet de ce
     * panneau a �t� s�lectionn�.
     */
    protected void updateStatusBar() {
        if (isShowing()) {
            // Si c'est un autre panneau qui est visible,
            // alors la barre d'�tat ne nous appartient pas.
            mosaic.statusBar.setText(null);
        }
    }

    /**
     * Remet � jour toutes les donn�es de cette composante. Cette m�thode
     * peut �tre appel�e de n'importe quel thread (pas n�cessairement celui
     * de Swing).
     *
     * @param  table Table � utiliser. Toutes les coordonn�es spatio-temporelles
     *         de cette table doivent correspondre � la zone d'int�r�t. La s�rie
     *         active sera �cras�e.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la
     *         base de donn�es.
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
     * Ajoute une s�rie � la liste des s�ries d�j� en m�moire. Cette m�thode
     * peut �tre appel�e de n'importe quel thread (pas n�cessairement celui
     * de Swing).
     *
     * @param  table Table � utiliser. Toutes les coordonn�es spatio-temporelles
     *         de cette table doivent correspondre � la zone d'int�r�t. La s�rie
     *         active de la table doit correspondre � la s�rie � ajouter.
     * @return Liste des entr�es qui ont �t� ajout�es.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la
     *         base de donn�es.
     */
    public List<ImageEntry> addSeries(final ImageTable table) throws SQLException {
        final List<ImageEntry> entries = addSeriesImpl(table);
        // Fixe une fois pour toute le nombre de
        // lignes et de colonnes de la mosa�que.
        layoutMosaic();
        return entries;
    }

    /**
     * Ajoute une s�rie � la liste des s�ries d�j� en m�moire. Cette m�thode
     * peut �tre appel�e de n'importe quel thread (pas n�cessairement celui
     * de Swing).
     *
     * @param  table Table � utiliser. Toutes les coordonn�es spatio-temporelles
     *         de cette table doivent correspondre � la zone d'int�r�t. La s�rie
     *         active de la table doit correspondre � la s�rie � ajouter.
     * @return Liste des entr�es qui ont �t� ajout�es.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la
     *         base de donn�es.
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
     * R�arrange la mosa�que d'images. Cette m�thode peut �tre appel�e
     * de n'importe quel thread (pas n�cessairement celui de Swing).
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
     * M�thode appel�e automatiquement chaque fois
     * que l'utilisateur change la position de la
     * visi�re.
     */
    private void slidderChanged(final BoundedRangeModel model) {
        if (!model.getValueIsAdjusting()) {
            if (javax.swing.SwingUtilities.getWindowAncestor(this) == null) {
                /*
                 * Ne charge pas d'image si la composante n'appara�t pas encore dans une
                 * fen�tre. Ca nous �vite de gaspiller de la m�moire et du CPU � charger
                 * une image que l'utilisateur n'a m�me pas encore demand�e, lorsque cette
                 * m�thode est appel�e pendant la construction de l'interface utilisateur.
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
                         * Pour chaque s�rie, obtient une image qui
                         * intercepte la plage de dates sp�cifi�es.
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
     * Cette modification n'affecte pas le fuseau horaire des �ventuelles
     * bases de donn�es acc�d�es par cette fen�tre.
     */
    protected void setTimeZone(final TimeZone timezone) {
        super    .setTimeZone(timezone);
        rangeBars.setTimeZone(timezone);
    }
}
