/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.sample;

// J2SE dependencies
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

// JAI dependencies
import javax.media.jai.JAI;
import javax.media.jai.util.Range;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.util.NumberRange;
import org.geotools.resources.Arguments;
import org.geotools.resources.MonolineFormatter;

// SEAGIS dependencies
import fr.ird.database.sample.*;
import fr.ird.resources.Utilities;


/**
 * G�n�re des images de potentiel de p�che.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class PotentialImageGenerator {
    /**
     * Nombre de millisecondes dans une journ�e; utilis� pour l'arrondissement des dates.
     */
    private static final int DAY = (24*60*60*1000);

    /**
     * Heure de la journ�e � laquelle calculer les images.
     */
    private static final int DAY_TIME = 0;

    /**
     * Arrondie un nombre vers le haut.
     */
    private static long ceil(long amount, int divider) {
        if (divider < 0) {
            divider = -divider;
            amount  = -amount;
        }
        long n = (amount/divider) * divider;
        while (n > amount) { // Se produit si 'amount' �tait n�gatif.
            n -= divider;
        }
        while (n < amount) {
            n += divider;
        }
        return n;
    }

    /**
     * Arrondie un nombre vers le bas.
     */
    private static long floor(long amount, int divider) {
        if (divider < 0) {
            divider = -divider;
            amount  = -amount;
        }
        long n = (amount/divider) * divider;
        while (n < amount) {
            n += divider;
        }
        while (n > amount) {
            n -= divider;
        }
        return n;
    }

    /**
     * Lance la cr�ation d'une s�rie d'images de potentiel de p�che
     * � partir de la ligne de commande. Les arguments sont:
     *
     * <ul>
     *   <li><code>-parameter=<var>P</var></code> o� <var>P</var> est un des param�tre �num�r�
     *       dans la table &quot;Param�tre&quot; de la base de donn�es des �chantillons (par
     *       exemple &quot;PP1&quot;).</li>
     *   <li><code>-directory=<var>D</var></code> o� var>D</var> est le r�pertoire dans
     *       lequel enregistrer les images.</li>
     * </ul>
     *
     * @param  args Les param�tres transmis sur la ligne de commande.
     * @throws SQLException si une connexion � la base de donn�es a �chou�e.
     * @throws IOException si l'image ne peut pas �tre enregistr�e.
     */
    public static void main(final String[] args) throws SQLException, IOException {
        MonolineFormatter.init("org.geotools");
        MonolineFormatter.init("fr.ird");
        final Arguments arguments = new Arguments(args);
        final String    parameter = arguments.getRequiredString("-parameter");
        final String    directory = arguments.getRequiredString("-directory");
        arguments.getRemainingArguments(0);
        final File path = new File(directory);
        if (!path.isDirectory()) {
            arguments.out.print('"');
            arguments.out.print(directory);
            arguments.out.println("\" n'est pas un r�pertoire.");
            return;
        }
        /*
         * Proc�de � la cr�ation des images, puis � leur enregistrement.
         */
        final double offset = -2;
        final double scale  = 1.0/64;
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(256*1024*1024);
        final ParameterCoverage3D coverage3D = new ParameterCoverage3D();
        try {
            coverage3D.setParameter(parameter);
            coverage3D.setOutputRange(new NumberRange(1*scale+offset, 255*scale+offset));
            final DateFormat   df = new SimpleDateFormat("'PP'yyDDD'.png'", Locale.FRANCE);
            final Range timeRange = coverage3D.getTimeRange();
            final Date       time = (Date) timeRange.getMinValue();
            final Date    endTime = (Date) timeRange.getMaxValue();
            time   .setTime( ceil(   time.getTime(), DAY));
            endTime.setTime(floor(endTime.getTime(), DAY));
            while (!time.after(endTime)) {
                final String filename = df.format(time);
                final File filepath = new File(path, filename);
                arguments.out.print("Cr�ation de ");
                arguments.out.println(filename);
                final GridCoverage coverage = coverage3D.getGridCoverage2D(time);
                Utilities.save(coverage.geophysics(false).getRenderedImage(), filepath.getPath());
                time.setTime(time.getTime() + DAY);
            }
        } finally {
            // Ferme la base de donn�es seulement apr�s la cr�ation de l'image,
            // sans quoi le calcul de l'image �chouera sans message d'erreur.
            coverage3D.dispose();
        }
    }
}
