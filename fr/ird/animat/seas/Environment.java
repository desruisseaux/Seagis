/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.animat.seas;

// Divers
import java.awt.Shape;
import java.util.Date;
import java.awt.geom.Point2D;
import java.sql.SQLException;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.XDimension2D;

// Base de donn�es
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageDataBase;

// Animats
import fr.ird.animat.Animal;

// Ev�nements
import javax.swing.event.EventListenerList;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;

// Evaluateurs
import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.ParameterValue;
import fr.ird.operator.coverage.MaximumEvaluator;


/**
 * Repr�sentation de l'environnement dans lequel �volueront les animaux.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Environment implements fr.ird.animat.Environment
{
    /**
     * Liste des objets int�ress�s � �tre inform�s
     * des changements apport�s � cet environnement.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Ev�nement � lancer � chaque fois que la population change.
     */
    private final EnvironmentChangeEvent event = new EnvironmentChangeEvent(this);

    /**
     * Couvertures � utiliser pour chaque param�tres.
     */
    private final Coverage3D coverage;

    /**
     * Date courante des images.
     */
    private final Date time = new Date();

    /**
     * Object � utiliser pour �valuer les positions
     * de certains param�tres.
     */
    private final Evaluator evaluator = new MaximumEvaluator();

    /**
     * Un indicateur g�n�ral de la qualit� des conditions environnementales.
     * La valeur 1 signifie que les eaux sont des plus transparentes et que
     * l'animal per�oit son environnement jusqu'� la limite de la capacit�
     * de ses sens. La valeur 0 signifie que les eaux sont tr�s troubles et
     * que l'animal ne "voit" rien.
     */
    private final double condition = 1;

    /**
     * Construit un environnement qui utilisera
     * la base de donn�es d'images sp�cifi�e.
     *
     * @param  database Base de donn�es � utiliser.
     * @param  resolution R�solution en degr�s d'angles.
     * @throws SQLException si une erreur est survenue
     *         lors de l'acc�s � la base de donn�es.
     */
    public Environment(final ImageDataBase database, final double resolution) throws SQLException
    {
        final ImageTable table = database.getImageTable();
        table.setPreferredResolution(new XDimension2D.Double(resolution, resolution));
        coverage = new Coverage3D(table);
        table.close();
    }

    /**
     * Retourne la date courante.
     */
    public Date getTime()
    {
        return new Date(time.getTime());
    }

    /**
     * D�finit la date courante.
     */
    public synchronized void setTime(final Date newTime)
    {
        if (!time.equals(newTime))
        {
            time.setTime(newTime.getTime());
            fireEnvironmentChanged();
        }
    }

    /**
     * Retourne le nombre de param�tres compris
     * dans cet environnement.
     */
    public int getParameterCount()
    {
        return 1;
    }

    /**
     * Retourne l'image courante.
     *
     * @param  parameter Index du param�tre dont on veut l'image.
     * @return L'image courange, ou <code>null</code> s'il n'y en a pas.
     */
    public GridCoverage getGridCoverage(final int parameter)
    {
        return coverage.getGridCoverage2D(time);
    }

    /**
     * Retourne les valeurs des param�tres que per�oit l'animal sp�cifi�.
     * Ces valeurs d�pendront du rayon de perception de l'animal, tel que
     * retourn� par {@link Animal#getPerceptionArea}.
     *
     * @param  animal Animal pour lequel retourner les param�tres de
     *         l'environnement qui se trouvent dans son rayon de perception.
     * @return Les param�tres per�us, ou <code>null</code> s'il n'y en a pas.
     */
    public ParameterValue[] getParameters(final Animal animal)
    {
        final GridCoverage gc = coverage.getGridCoverage2D(time);
        if (gc!=null)
        {
            final Shape area = animal.getPerceptionArea(condition);
            return evaluator.evaluate(gc, area);
        }
        return null;
    }

    /**
     * A appeler � chaque fois que l'environnement change.
     */
    protected void fireEnvironmentChanged()
    {
        final Object[] listeners = listenerList.getListenerList();
        for (int i=listeners.length; (i-=2)>=0;)
        {
            if (listeners[i] == EnvironmentChangeListener.class)
            {
                ((EnvironmentChangeListener)listeners[i+1]).environmentChanged(event);
            }
        }
    }

    /**
     * D�clare un objet � informer des changements survenant dans cet
     * environnement. Ces changements surviennent souvent suite � un
     * appel de {@link #setTime}.
     */
    public synchronized void addEnvironmentChangeListener(final EnvironmentChangeListener listener)
    {listenerList.add(EnvironmentChangeListener.class, listener);}

    /**
     * Retire un objet � informer des changements survenant dans cet
     * environnement.
     */
    public synchronized void removeEnvironmentChangeListener(final EnvironmentChangeListener listener)
    {listenerList.remove(EnvironmentChangeListener.class, listener);}
}
