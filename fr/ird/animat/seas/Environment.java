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
import java.util.Date;
import java.sql.SQLException;

// Implémentation d'OpenGIS
import net.seagis.gc.GridCoverage;

// Base de données
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageDataBase;

// Evénements
import javax.swing.event.EventListenerList;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Représentation de l'environnement dans lequel évolueront les animaux.
 *
 * @version 1.0
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
     * Construit un environnement qui utilisera
     * la base de données d'images spécifiée.
     *
     * @throws SQLException si une erreur est survenue
     *         lors de l'accès à la base de données.
     */
    public Environment(final ImageDataBase database) throws SQLException
    {
        final ImageTable table = database.getImageTable();
        coverage = new Coverage3D(table);
        table.close();
    }

    /**
     * Définit la date courante.
     */
    public void setTime(final Date newTime)
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
     * environnement. Ces changements suviennent souvent suite à un
     * appel de {@link #setTime}.
     */
    public void addEnvironmentChangeListener(final EnvironmentChangeListener listener)
    {listenerList.add(EnvironmentChangeListener.class, listener);}

    /**
     * Retire un objet à informer des changements survenant dans cet
     * environnement.
     */
    public void removeEnvironmentChangeListener(final EnvironmentChangeListener listener)
    {listenerList.remove(EnvironmentChangeListener.class, listener);}
}
