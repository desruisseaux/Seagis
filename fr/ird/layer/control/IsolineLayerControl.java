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
 * Couche contenant une bathym�trie.
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
                new IsolineFactory("M�diterran�e")
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
     * Objet � utiliser pour configurer l'affichage des images.
     */
    private transient IsolineControlPanel controler;

    /**
     * Construit une couche de la bathym�trie.
     */
    public IsolineLayerControl()
    {super(false);}

    /**
     * Retourne le nom de cette couche.
     */
    public String getName()
    {return Resources.format(ResourceKeys.BATHYMETRY);}

    /**
     * Retourne des couches appropri�es pour l'image sp�cifi�. Cette m�thode peut �tre
     * appel�e de n'importe quel Thread, g�n�ralement pas celui de <i>Swing</i>.
     *
     * @param  layers Couches � configurer. Si non-nul, alors ces couches doivent
     *         avoir �t� cr�� pr�c�demment par ce m�me objet <code>LayerControl</code>.
     * @param  entry Image � afficher. Il s'agit d'une image s�lectionn�e par
     *         l'utilisateur dans la liste d�roulante qui appara�t � gauche de
     *         la mosa�que d'images.
     * @param  listeners Objets � informer des progr�s d'une �ventuelle lecture.
     * @return Des couches proprement configur�es, ou <code>null</code> si la configuration
     *         se traduirait � toute fin pratique par la disparition de la couche.
     * @throws SQLException si les acc�s � la base de donn�es ont �chou�s.
     * @throws IOException si une erreur d'entr�/sortie est survenue.
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
     * Fait appara�tre un paneau de configuration pour cette couche. Cette
     * m�thode est responsable d'appeler {@link #fireStateChanged} si l'�tat
     * de cette couche a chang� suite aux interventions de l'utilisateur.
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
