/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat.seas;

// Divers
import java.awt.Shape;
import java.util.Date;
import java.awt.geom.Point2D;
import java.sql.SQLException;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.resources.XDimension2D;

// Base de données
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageDataBase;

// Animats
import fr.ird.animat.Animal;

// Evénements
import javax.swing.event.EventListenerList;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;

// Evaluateurs
import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.ParameterValue;
import fr.ird.operator.coverage.MaximumEvaluator;


/**
 * Représentation de l'environnement dans lequel évolueront les animaux.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Environment implements fr.ird.animat.Environment
{
    /**
     * Liste des objets intéressés à être informés
     * des changements apportés à cet environnement.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Evénement à lancer à chaque fois que la population change.
     */
    private final EnvironmentChangeEvent event = new EnvironmentChangeEvent(this);

    /**
     * Couvertures à utiliser pour chaque paramètres.
     */
    private final Coverage3D coverage;

    /**
     * Date courante des images.
     */
    private final Date time = new Date();

    /**
     * Object à utiliser pour évaluer les positions
     * de certains paramètres.
     */
    private final Evaluator evaluator = new MaximumEvaluator();

    /**
     * Un indicateur général de la qualité des conditions environnementales.
     * La valeur 1 signifie que les eaux sont des plus transparentes et que
     * l'animal perçoit son environnement jusqu'à la limite de la capacité
     * de ses sens. La valeur 0 signifie que les eaux sont très troubles et
     * que l'animal ne "voit" rien.
     */
    private final double condition = 1;

    /**
     * Construit un environnement qui utilisera
     * la base de données d'images spécifiée.
     *
     * @param  database Base de données à utiliser.
     * @param  resolution Résolution en degrés d'angles.
     * @throws SQLException si une erreur est survenue
     *         lors de l'accès à la base de données.
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
     * Définit la date courante.
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
     * Retourne le nombre de paramètres compris
     * dans cet environnement.
     */
    public int getParameterCount()
    {
        return 1;
    }

    /**
     * Retourne l'image courante.
     *
     * @param  parameter Index du paramètre dont on veut l'image.
     * @return L'image courange, ou <code>null</code> s'il n'y en a pas.
     */
    public GridCoverage getGridCoverage(final int parameter)
    {
        return coverage.getGridCoverage2D(time);
    }

    /**
     * Retourne les valeurs des paramètres que perçoit l'animal spécifié.
     * Ces valeurs dépendront du rayon de perception de l'animal, tel que
     * retourné par {@link Animal#getPerceptionArea}.
     *
     * @param  animal Animal pour lequel retourner les paramètres de
     *         l'environnement qui se trouvent dans son rayon de perception.
     * @return Les paramètres perçus, ou <code>null</code> s'il n'y en a pas.
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
     * A appeler à chaque fois que l'environnement change.
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
     * Déclare un objet à informer des changements survenant dans cet
     * environnement. Ces changements surviennent souvent suite à un
     * appel de {@link #setTime}.
     */
    public synchronized void addEnvironmentChangeListener(final EnvironmentChangeListener listener)
    {listenerList.add(EnvironmentChangeListener.class, listener);}

    /**
     * Retire un objet à informer des changements survenant dans cet
     * environnement.
     */
    public synchronized void removeEnvironmentChangeListener(final EnvironmentChangeListener listener)
    {listenerList.remove(EnvironmentChangeListener.class, listener);}
}
