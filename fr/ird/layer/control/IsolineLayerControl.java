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
package fr.ird.layer.control;

// Input/output and images
import java.io.IOException;
import java.sql.SQLException;
import fr.ird.sql.image.ImageEntry;
import java.io.FileNotFoundException;

// Map components
import net.seas.map.Layer;
import net.seas.map.Isoline;
import net.seas.map.layer.IsolineLayer;
import fr.ird.io.IsolineFactory;

// Graphical user interface
import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.event.EventListenerList;

// Geotools dependencies
import org.geotools.gui.swing.ExceptionMonitor;

// Miscellaneous
import java.util.Date;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Couche contenant une bathymétrie.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class IsolineLayerControl extends LayerControl
{
    /**
     * Factories for isolines.
     */
    private static final IsolineFactory[] FACTORIES;
    static
    {
        try
        {
            FACTORIES = new IsolineFactory[]
            {
                new IsolineFactory("Méditerranée")
            };
        }
        catch (FileNotFoundException exception)
        {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * The default set of selected values.
     */
    private static final float[] DEFAULT_VALUES = new float[1];

    /**
     * Objet à utiliser pour configurer l'affichage des images.
     */
    private transient IsolineControlPanel controler;

    /**
     * Construit une couche de la bathymétrie.
     */
    public IsolineLayerControl()
    {super(false);}

    /**
     * Retourne le nom de cette couche.
     */
    public String getName()
    {return Resources.format(ResourceKeys.BATHYMETRY);}

    /**
     * Retourne des couches appropriées pour l'image spécifié. Cette méthode peut être
     * appelée de n'importe quel Thread, généralement pas celui de <i>Swing</i>.
     *
     * @param  layers Couches à configurer. Si non-nul, alors ces couches doivent
     *         avoir été créé précédemment par ce même objet <code>LayerControl</code>.
     * @param  entry Image à afficher. Il s'agit d'une image sélectionnée par
     *         l'utilisateur dans la liste déroulante qui apparaît à gauche de
     *         la mosaïque d'images.
     * @param  listeners Objets à informer des progrès d'une éventuelle lecture.
     * @return Des couches proprement configurées, ou <code>null</code> si la configuration
     *         se traduirait à toute fin pratique par la disparition de la couche.
     * @throws SQLException si les accès à la base de données ont échoués.
     * @throws IOException si une erreur d'entré/sortie est survenue.
     */
    public Layer[] configLayers(final Layer[] layers, final ImageEntry entry, final EventListenerList listeners) throws SQLException, IOException
    {
        final float[] values;
        synchronized(this)
        {
            if (controler!=null)
            {
                values = controler.getSelectedValues();
            }
            else values = DEFAULT_VALUES;
        }
        IsolineFactory factory = FACTORIES[0]; // TODO: Select the right factory

        final Isoline[]       isolines = factory.get(values);
        final IsolineLayer[] isoLayers = new IsolineLayer[isolines.length];
        for (int i=0; i<isoLayers.length; i++)
        {
            isoLayers[i] = new IsolineLayer(isolines[i]);
            isoLayers[i].setContour(Color.white);  // TODO: Set colors
        }
        return isoLayers;
    }

    /**
     * Fait apparaître un paneau de configuration pour cette couche. Cette
     * méthode est responsable d'appeler {@link #fireStateChanged} si l'état
     * de cette couche a changé suite aux interventions de l'utilisateur.
     */
    protected void showControler(final JComponent owner)
    {
        final Object oldContent;
        synchronized(this)
        {
            if (controler==null)
            {
                controler = new IsolineControlPanel();
                for (int i=0; i<FACTORIES.length; i++) try
                {
                    controler.addValues(FACTORIES[i].getAvailableValues());
                }
                catch (IOException exception)
                {
                    ExceptionMonitor.show(owner, exception);
                }
                controler.setSelectedValues(DEFAULT_VALUES); // Must be last.
            }
            oldContent = controler.mark();
        }
        if (controler.showDialog(owner)) synchronized(this)
        {
            final Object newContent = controler.mark();
            fireStateChanged(new Edit()
            {
                protected void edit(final boolean redo)
                {
                    controler.reset(redo ? newContent : oldContent);
                }
            });
        }
    }
}
