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
package fr.ird.main.viewer;

// Database
import java.sql.SQLException;
import fr.ird.main.DataBase;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.image.SeriesEntry;

// OpenGIS implementation (SEAGIS)
import net.seagis.gc.GridCoverage;

// Map components
import fr.ird.layer.control.LayerControl;
import fr.ird.awt.StatusBar;

// User interface
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import net.seas.plot.RangeSet;
import net.seas.plot.RangeBars;
import net.seas.awt.ExceptionMonitor;

// Events and models
import javax.swing.BoundedRangeModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// Collections and time
import java.util.Date;
import java.util.TimeZone;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

// Formats
import java.text.DateFormat;
import java.text.ParseException;

// Resources
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Paneau pouvant contenir une mosa�que d'images satellitaires.  Ces images seront
 * de plusieurs types (temp�rature, vorticit�, etc...). Un glissoir dans le bas de
 * la fen�tre permettra de s�lectionner la date pour laquelle on souhaite voir les
 * images.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ImageMosaicPanel extends ImagePanel //implements ChangeListener
{
    /**
     * Nombres de lignes et de colonnes � donner � la mosa�que en
     * fonction du nombre d'images.  On fixera ce nombre une fois
     * pour toute lors de la construction afin d'�viter des
     * changements trop fr�quents lors des redimmensionnement.
     */
    private static final byte[] SIZE =
    {
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
                                                      SwingConstants.VERTICAL);

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
        rangeBars.setRangeAdjustable(true);
        rangeBars.getModel().addChangeListener(new ChangeListener()
        {
            public void stateChanged(final ChangeEvent event)
            {slidderChanged((BoundedRangeModel) event.getSource());}
        });
    }

    /**
     * Met � jour le texte de la barre d'�tat.  Cette m�thode est
     * appel�e automatiquement par exemple lorsque l'onglet de ce
     * panneau a �t� s�lectionn�.
     */
    protected void updateStatusBar()
    {
        if (isShowing()) // Si c'est un autre panneau qui est visible,
        {                // alors la barre d'�tat ne nous appartient pas.
            mosaic.statusBar.setText(null);
        }
    }

    /**
     * Remet � jour toutes les donn�es de cette composante.
     */
    public void refresh() throws SQLException
    {
        setSeries(series.toArray(new SeriesEntry[series.size()]));
    }

    /**
     * D�finit les s�ries � afficher. Les s�ries sp�cifi�es remplaceront
     * toutes les s�ries pr�c�demment affich�es.
     *
     * @param  series Series � afficher.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la
     *         base de donn�es.
     */
    public void setSeries(final SeriesEntry[] series) throws SQLException
    {
        synchronized (rangeBars)
        {
            rangeBars.clear();
            this.series.clear();
            mosaic.removeAllImages();
            for (int i=0; i<series.length; i++)
                addSeriesImpl(series[i]);
        }
        // Fixe une fois pour toute le nombre de
        // lignes et de colonnes de la mosa�que.
        int i = this.series.size()*2;
        if (i<SIZE.length)
        {
            mosaic.setGridSize(SIZE[i],SIZE[i+1]);
        }
        mosaic.setImageCount(series.length);
        resetSlidderPosition();
    }

    /**
     * Ajoute une s�rie � la liste des s�ries d�j� en m�moire.
     *
     * @param  series Serie � ajouter.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la
     *         base de donn�es.
     */
    private void addSeriesImpl(final SeriesEntry series) throws SQLException
    {
        synchronized (table)
        {
            final RangeSet ranges = new RangeSet(Date.class);
            table.setSeries(series);
            table.getRanges(null, null, ranges);
            this.series.add(series);

            final String name = series.getName();
            final int  length = name.length();
            int i=0;
            while (i<length && !Character.isSpaceChar(name.charAt(i))) i++;
            rangeBars.setRanges(name.substring(0, i), ranges);
        }
    }

    /**
     * Ajoute une s�rie � la liste des s�ries d�j� en m�moire.
     *
     * @param  series Serie � ajouter.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la
     *         base de donn�es.
     */
    public void addSeries(final SeriesEntry series) throws SQLException
    {
        addSeriesImpl(series);
        // Fixe une fois pour toute le nombre de
        // lignes et de colonnes de la mosa�que.
        final int size = this.series.size();
        final int i = size*2;
        if (i<SIZE.length)
        {
            mosaic.setGridSize(SIZE[i],SIZE[i+1]);
        }
        mosaic.setImageCount(size);
        final int check;
        assert size == (check=mosaic.getImageCount()) :
                       "series="+size+" images="+check;

        if (size==1)
        {
            resetSlidderPosition();
        }
    }

    /**
     * Reset the slidder position.
     */
    private void resetSlidderPosition()
    {
        final Date minimum = (Date) rangeBars.getMinimum();
        final Date maximum = (Date) rangeBars.getMaximum();
        if (minimum!=null && maximum!=null)
        {
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
    private void slidderChanged(final BoundedRangeModel model)
    {
        if (!model.getValueIsAdjusting())
        {
            final Date startTime=new Date(Math.round(rangeBars.getMinSelectedValue()));
            final Date   endTime=new Date(Math.round(rangeBars.getMaxSelectedValue()));
            synchronized (table)
            {
                try
                {
                    table.setTimeRange(startTime, endTime);
                    /*
                     * Pour chaque s�rie, obtient une image qui
                     * intercepte la plage de dates sp�cifi�es.
                     */
                    final int length = series.size();
                    final int check;
                    assert length == (check=mosaic.getImageCount()) :
                                   "series="+length+" images="+check;

                    for (int i=0; i<length; i++)
                    {
                        table.setSeries(series.get(i));
                        final ImageEntry   entry = table.getEntry();
                        final ImageCanvas canvas = mosaic.getImage(i);
                        if (canvas!=null)
                        {
                            if (entry!=null)
                            {
                                canvas.setImage(entry, getSelectedLayers(),
                                        mosaic.statusBar.getIIOReadProgressListener(entry.getName()));
                            }
                            else canvas.setImage((GridCoverage)null);
                        }
                    }
                }
                catch (SQLException exception)
                {
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
    protected void setTimeZone(final TimeZone timezone)
    {
        super    .setTimeZone(timezone);
        rangeBars.setTimeZone(timezone);
    }
}
