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

// Java standard
import java.util.Date;
import java.awt.geom.Point2D;
import java.sql.SQLException;

// Collections
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;

// Textes
import java.text.DateFormat;
import java.text.ParseException;

// Interface utilisateur
import javax.swing.JFrame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

// Pêches
import fr.ird.animat.Viewer;
import fr.ird.sql.DataBase;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;
import fr.ird.sql.image.ImageDataBase;

// Divers
import net.seas.util.XArray;


/**
 * Représentation d'une population d'animaux et de sa dynamique.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Dynamic
{
    /**
     * Liste des espèces d'intérêt pour ce modèle.
     */
    private static final String[] SPECIES =
    {
        "YFT",  // Albacore
//      "SKJ"   // Listao
    };

    /**
     * Pas de temps pour le modèle.
     */
    private static final long TIME_STEP = 24*60*60*1000L;

    /**
     * Liste des bases de données à fermer.
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
    private final Population population = new Population();

    /**
     * L'afficheur de cette dynamique.
     */
    private Viewer viewer;

    /**
     * Construit une dynamique avec les bases de données par defaut.
     *
     * @throws SQLException si une connexion avec une des base de données
     *         a échouée.
     */
    public Dynamic() throws SQLException
    {
        this(new ImageDataBase(), new FisheryDataBase(), true);
    }

    /**
     * Construit une dynamique avec les bases de données spécifiées.
     *
     * @throws SQLException si une connexion avec une des base de données
     *         a échouée.
     */
    public Dynamic(final ImageDataBase   images,
                   final FisheryDataBase fisheries) throws SQLException
    {
        this(images, fisheries, false);
    }

    /**
     * Construit une dynamique avec les bases de données spécifiées.
     *
     * @throws SQLException si une connexion avec une des base de données
     *         a échouée.
     */
    private Dynamic(final ImageDataBase   images,
                    final FisheryDataBase fisheries,
                    final boolean         close) throws SQLException
    {
        this.catchs      = fisheries.getCatchTable(SPECIES);
        this.environment = new Environment(images);
        toClose = close ? new DataBase[] {images, fisheries} : null;
    }

    /**
     * Initialise le modèle à la date spécifiée. Des thons
     * seront ajoutées pour chaque espèces à chaque position
     * de pêches.
     */
    public synchronized void init(final Date time) throws SQLException
    {
        environment.setTime(time);
        catchs.setTimeRange(time, new Date(time.getTime()+TIME_STEP));
        population.addTunas(catchs.getEntries());
    }

    /**
     * Retourne l'afficheur
     * pour cette dynamique.
     */
    public synchronized Viewer getViewer()
    {
        if (viewer == null)
        {
            viewer = new Viewer(environment, population);
        }
        return viewer;
    }

    /**
     * Libère les ressources utilisées par cet objet.
     */
    public synchronized void close() throws SQLException
    {
        catchs.close();
        if (toClose!=null)
        {
            for (int i=0; i<toClose.length; i++)
                toClose[i].close();
        }
    }

    /**
     * Lance la modélisation.
     */
    public static void main(final String[] args) throws SQLException
    {
        Date time = null;
        if (args.length!=0)
        {
            final DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
            try
            {
                time = df.parse(args[0]);
            }
            catch (ParseException exception)
            {
                System.out.println(exception.getLocalizedMessage());
                System.out.print("Exemple: ");
                System.out.print(df.format(new Date()));
                return;
            }
        }

        // Initialise l'objet qui contrôlera
        // la dynamique des populations.
        final Dynamic dynamic = new Dynamic();
        if (time!=null)
        {
            dynamic.init(time);
        }

        // Construit la fenêtre. La fermeture de cette
        // fenêtre fermera aussi les connections JDBC.
        final JFrame frame = new JFrame("Simulation");
        frame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e) {
                try {
                    dynamic.close();
                    System.exit(0);
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            }
        });
        frame.getContentPane().add(dynamic.getViewer().getView());
        frame.pack();
        frame.show();
    }
}
