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
import java.util.Date;
import java.sql.SQLException;

// Impl�mentation d'OpenGIS
import net.seagis.gc.GridCoverage;

// Base de donn�es
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageDataBase;

// Ev�nements
import javax.swing.event.EventListenerList;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Repr�sentation de l'environnement dans lequel �volueront les animaux.
 *
 * @version 1.0
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
     * Construit un environnement qui utilisera
     * la base de donn�es d'images sp�cifi�e.
     *
     * @throws SQLException si une erreur est survenue
     *         lors de l'acc�s � la base de donn�es.
     */
    public Environment(final ImageDataBase database) throws SQLException
    {
        final ImageTable table = database.getImageTable();
        coverage = new Coverage3D(table);
        table.close();
    }

    /**
     * D�finit la date courante.
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
     * environnement. Ces changements suviennent souvent suite � un
     * appel de {@link #setTime}.
     */
    public void addEnvironmentChangeListener(final EnvironmentChangeListener listener)
    {listenerList.add(EnvironmentChangeListener.class, listener);}

    /**
     * Retire un objet � informer des changements survenant dans cet
     * environnement.
     */
    public void removeEnvironmentChangeListener(final EnvironmentChangeListener listener)
    {listenerList.remove(EnvironmentChangeListener.class, listener);}
}
