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

// Java standard
import java.util.Date;
import java.awt.geom.Point2D;
import java.sql.SQLException;

// Collections
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;

// P�ches
import fr.ird.animat.Viewer;
import fr.ird.sql.DataBase;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;
import fr.ird.sql.image.ImageDataBase;

// Divers
import fr.ird.util.XArray;


/**
 * Repr�sentation d'une population d'animaux et de sa dynamique.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Dynamic implements Runnable
{
    /**
     * Liste des esp�ces d'int�r�t pour ce mod�le.
     */
    private static final String[] SPECIES =
    {
        "YFT",  // Albacore
//      "SKJ"   // Listao
    };

    /**
     * Plage de temps dans laquelle rechercher des donn�es de p�ches.
     */
    private static final long TIME_RANGE = 24*60*60*1000L;

    /**
     * Pas de temps pour le mod�le.
     */
    private final long timeStep;

    /**
     * Pause a effectuer entre deux pas de temps virtuels.
     */
    private final long pause;

    /**
     * Liste des bases de donn�es � fermer.
     */
    private final DataBase[] toClose;

    /**
     * Table des captures.
     */
    private final CatchTable catchs;

    /**
     * Environnement des thons.
     */
    private final Environment environment;

    /**
     * Population des thons.
     */
    private final Population population;

    /**
     * L'afficheur de cette dynamique.
     */
    private Viewer viewer;

    /**
     * <code>true</code> tant que le mod�le est
     * en cours d'ex�cution.
     */
    private boolean running;

    /**
     * Construit une dynamique avec les bases de donn�es par defaut.
     *
     * @param  resolution R�solution pr�f�r�e des images � charger,
     *         en degr�s d'angles.
     * @throws SQLException si une connexion avec une des base de donn�es
     *         a �chou�e.
     */
    public Dynamic(final double resolution,
                   final double moveDistance,
                   final Date   time,
                   final long   timeStep,
                   final long   pause) throws SQLException
    {
        final ImageDataBase   images    = new ImageDataBase();
        final FisheryDataBase fisheries = new FisheryDataBase();
        this.catchs      = fisheries.getCatchTable(SPECIES);
        this.environment = new Environment(images, time, resolution);
        this.population  = new Population(moveDistance);
        this.toClose     = new DataBase[] {images, fisheries};
        this.timeStep    = timeStep;
        this.pause       = pause;

        environment.setTime(time);
        catchs.setTimeRange(time, new Date(time.getTime()+TIME_RANGE));
        population.addTunas(catchs.getEntries());
    }

    /**
     * Ex�cute la dynamique.
     */
    public void run()
    {
        running=true;
        do
        {
            try
            {
                synchronized (this)
                {
                    population.move();
                    nextTimeStep();
                    population.observe(environment);
                }
                Thread.currentThread().sleep(pause);
                /*
                 * On a appell� 'nextTimeStep' apr�s avoir d�plac� les animaux de
                 * fa�on � ce que l'image courante de l'afficheur  soit celle qui
                 * influencera le prochain d�placement. C'est n�cessaire pour que
                 * l'affichage du "cercle de perception" des animaux  corresponde
                 * bien aux donn�es qu'ils prendront en compte.
                 */
            }
            catch (SQLException exception)
            {
                exception.printStackTrace();
            }
            catch (InterruptedException exception)
            {
                // Someone doesn't want to lets us sleep.
                // Go back to work.
            }
        }
        while (running);
    }

    /**
     * Avance d'un pas de temps.
     */
    private synchronized void nextTimeStep() throws SQLException
    {
        final Date time = environment.getTime();
        time.setTime(time.getTime()+timeStep);
        environment.setTime(time);
        catchs.setTimeRange(time, new Date(time.getTime()+TIME_RANGE));
    }

    /**
     * Retourne l'afficheur
     * pour cette dynamique.
     */
    public synchronized Viewer getViewer()
    {
        if (viewer == null)
        {
            viewer = new Viewer(environment, population, this);
        }
        return viewer;
    }

    /**
     * Lib�re les ressources utilis�es par cet objet.
     */
    public synchronized void close() throws SQLException
    {
        running=false;
        catchs.close();
        if (toClose!=null)
        {
            for (int i=0; i<toClose.length; i++)
                toClose[i].close();
        }
    }
}
