/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.database.sample;

// J2SE dependencies
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

// JAI dependencies
import javax.media.jai.JAI;
import javax.media.jai.util.Range;

// Geotools dependencies
import org.geotools.pt.Envelope;
import org.geotools.gc.GridCoverage;
import org.geotools.util.NumberRange;
import org.geotools.resources.Arguments;
import org.geotools.resources.XDimension2D;
import org.geotools.resources.ImageUtilities;
import org.geotools.resources.MonolineFormatter;

// SEAGIS dependencies
import fr.ird.database.sample.*;
import fr.ird.resources.Utilities;


/**
 * Génère des images de potentiel de pêche.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class PotentialImageGenerator extends ParameterCoverage3D {
    /**
     * Nombre de millisecondes dans une journée; utilisé pour l'arrondissement des dates.
     */
    private static final int DAY = (24*60*60*1000);

    /**
     * Heure de la journée à laquelle calculer les images.
     */
    private static final int DAY_TIME = (12*60*60*1000);

    /**
     * Le pas de temps entre deux images.
     */
    private static final int TIME_STEP = 7*DAY;

    /**
     * Région geographique d'intérêt. Par défaut de 30°E à 80°E et 30°S à 10°N.
     */
    private static final Rectangle2D GEOGRAPHIC_AREA = new Rectangle(30, -30, 50, 40);

    /**
     * Arrondie un nombre vers le haut.
     */
    private static long ceil(long amount, int divider) {
        if (divider < 0) {
            divider = -divider;
            amount  = -amount;
        }
        long n = (amount/divider) * divider;
        while (n > amount) { // Se produit si 'amount' était négatif.
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
     * Construit un générateur de carte de potentiel par défaut.
     */
    public PotentialImageGenerator() throws SQLException {
        super();
    }

    /**
     * Retourne une envelope englobant les coordonnées spatio-temporelles des données.
     * Cette envelope sera l'intersection de {@linkplain ParameterCoverage3D#getEnvelope()
     * l'envelope par défaut} et de l'envelope de la zone d'étude.
     */
    public Envelope getEnvelope() {
        final Envelope envelope = super.getEnvelope();
        for (int dimension=0; dimension<=1; dimension++) {
            final double min, max;
            switch (dimension) {
                case 0:  min=GEOGRAPHIC_AREA.getMinX(); max=GEOGRAPHIC_AREA.getMaxX(); break;
                case 1:  min=GEOGRAPHIC_AREA.getMinY(); max=GEOGRAPHIC_AREA.getMaxY(); break;
                default: throw new AssertionError(dimension);
            }
            envelope.setRange(dimension, Math.max(envelope.getMinimum(dimension), min),
                                         Math.min(envelope.getMaximum(dimension), max));
        }
        return envelope;
    }

    /**
     * Retourne la taille par défaut des pixels des images à produire.
     */
    protected Dimension2D getDefaultPixelSize() {
        return new XDimension2D.Double(0.125, 0.125);
    }

    /**
     * Lance la création d'une série d'images de potentiel de pêche
     * à partir de la ligne de commande. Les arguments sont:
     *
     * <ul>
     *   <li><code>-parameter=<var>P</var></code> où <var>P</var> est un des paramètre énuméré
     *       dans la table &quot;Paramètre&quot; de la base de données des échantillons (par
     *       exemple &quot;PP1&quot;).</li>
     *   <li><code>-directory=<var>D</var></code> où var>D</var> est le répertoire dans
     *       lequel enregistrer les images.</li>
     * </ul>
     *
     * @param  args Les paramètres transmis sur la ligne de commande.
     * @throws SQLException si une connexion à la base de données a échouée.
     * @throws IOException si l'image ne peut pas être enregistrée.
     */
    public static void main(final String[] args) throws SQLException, IOException {
        // HACK: Pour vérifier le décodeur d'image PNG.
        ImageUtilities.allowNativeCodec("png", false, false);
        ImageUtilities.allowNativeCodec("png", true , false);
        new org.geotools.gui.swing.About().showDialog(null);

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
            arguments.out.println("\" n'est pas un répertoire.");
            return;
        }
        /*
         * Procède à la création des images, puis à leur enregistrement.
         */
        final double offset = -2;
        final double scale  = 1.0/64;
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(256*1024*1024);
        final ParameterCoverage3D coverage3D = new PotentialImageGenerator();
        try {
            coverage3D.setParameter(parameter);
            coverage3D.setOutputRange(new NumberRange(1*scale+offset, 255*scale+offset));
            final DateFormat   df = new SimpleDateFormat("'PP'yyDDD'.png'", Locale.FRANCE);
            final Range timeRange = coverage3D.getTimeRange();
            final Date       time = (Date) timeRange.getMinValue();
            final Date    endTime = (Date) timeRange.getMaxValue();
            time   .setTime( ceil(   time.getTime(), DAY) + DAY_TIME);
            endTime.setTime(floor(endTime.getTime(), DAY));
            while (!time.after(endTime)) {
                final String filename = df.format(time);
                final File filepath = new File(path, filename);
                arguments.out.print("Création de ");
                arguments.out.println(filename);
                final GridCoverage coverage = coverage3D.getGridCoverage2D(time);
                Utilities.save(coverage.geophysics(false).getRenderedImage(), filepath.getPath());
                time.setTime(time.getTime() + TIME_STEP);
            }
        } finally {
            // Ferme la base de données seulement après la création de l'image,
            // sans quoi le calcul de l'image échouera sans message d'erreur.
            coverage3D.dispose();
        }
    }
}
