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

// Entr�s/sorties et base de donn�es
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.sql.SQLException;

// Dates
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

// Interface utilisateur
import javax.swing.JFrame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

// Collections
import java.util.NoSuchElementException;

// Utilitaires
import org.geotools.resources.Arguments;
import org.geotools.resources.Utilities;


/**
 * Point d'entr� du programme de simulation.
 */
public final class Main extends Arguments
{
    /**
     * La dynamique du mod�le.
     */
    private Dynamic dynamic;

    /**
     * Construit une application.
     */
    private Main(final String[] args)
    {
        super(args);
        try
        {
            /////////////////////////////////////////////////
            ////    Obtient l'ensemble des propri�t�s    ////
            /////////////////////////////////////////////////
            final Properties properties;
            if (true)
            {
                final String filename = getOptionalString("-run");
                getRemainingArguments(1);
                if (filename==null)
                {
                    out.println("Arguments: -run [fichier de propri�t�s]");
                    System.exit(0);
                }
                final InputStream in = new FileInputStream(filename);
                properties = new Properties();
                properties.load(in);
                in.close();
            }
            ////////////////////////////////////////////////
            ////    Extrait les propri�t�s du mod�le    ////
            ////////////////////////////////////////////////
            final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
            final Date startTime = dateFormat.parse(getProperty(properties, "START_TIME"));

            final long timeStep = Math.round((24.0*60*60*1000)*
                    Double.parseDouble(getProperty(properties, "TIME_STEP")));

            final long pause = Math.round(1000*
                    Double.parseDouble(getProperty(properties, "PAUSE")));

            final double resolution =
                    Double.parseDouble(getProperty(properties, "RESOLUTION"))/60;

            final double moveDistance = (timeStep!=0 ? timeStep*1852 : 1852)*
                    Double.parseDouble(getProperty(properties, "DAILY_DISTANCE"));

            /////////////////////////////////////////////////
            ////    Construit la dynamique du mod�le     ////
            /////////////////////////////////////////////////
            dynamic = new Dynamic(resolution, moveDistance, startTime, timeStep, pause);
        }
        catch (Exception exception)
        {
            exception.printStackTrace(out);
            out.flush();
            System.exit(1);
        }
    }

    /**
     * Retourne la propri�t� sp�cifi�e.
     *
     * @throws NoSuchElementException si la propri�t� n'est pas d�finie.
     */
    private static String getProperty(final Properties properties, final String key) throws NoSuchElementException
    {
        final String property = properties.getProperty(key);
        if (property==null)
        {
            throw new NoSuchElementException("La propri�t� \""+key+"\" n'est pas d�finie.");
        }
        return property;
    }

    /**
     * Fait appara�tre une fen�tre repr�sentant
     * l'�volution de la dynamique. La fermeture
     * de cette fen�tre fermera aussi les connections JDBC.
     */
    public void show()
    {
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

    /**
     * Lance l'application.
     */
    public static void main(final String[] args)
    {
        final Main main = new Main(args);
        main.show();
        main.dynamic.run();
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY-1);
    }
}
