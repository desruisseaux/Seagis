/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Universidad de Las Palmas de Gran Canaria
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
 * Contact: Antonio Ramos
 *          Departamento de Biologia ULPGC
 *          Campus Universitario de Tafira
 *          Edificio de Ciencias Basicas
 *          35017
 *          Las Palmas de Gran Canaria
 *          Spain
 *
 *          mailto:antonio.ramos@biologia.ulpgc.es
 */
package fr.ird.image.pump;

// G�om�trie et images
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;

// Collection
import java.util.Set;
import java.util.Arrays;
import java.util.LinkedHashSet;

// Lecture de fichiers HDF
import ncsa.hdf.hdflib.HDFException;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.resources.Arguments;

// Miscellaneous
import fr.ird.io.hdf4.Parser;
import fr.ird.io.hdf4.DataSet;
import fr.ird.operator.image.AbstractImageFunction;


/**
 * Classe construisant une image repr�sentant l'amplitude
 * du vent mesur� par le satellite QuikScat. Cette classe
 * utilise les fichiers de donn�es de niveau L2B.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
class WindImageFunction extends AbstractImageFunction {
    /**
     * Liste des fichiers de donn�es � utiliser.
     */
    private final File[] files;

    /**
     * Index du prochain fichier � lire.
     */
    private int fileIndex;

    /**
     * Ensemble des coordonn�ess <var>x</var> des points.
     */
    protected DataSet longitude;

    /**
     * Ensemble des coordonn�ess <var>y</var> des points.
     */
    protected DataSet latitude;

    /**
     * Ensemble des vitesses du vent en chaque points.
     */
    protected DataSet windSpeed;

    /**
     * Ensemble des directions du vent en chaque points.
     */
    protected DataSet windDirection;

    /**
     * Nombre de points � lire.
     */
    private int count;

    /**
     * Index du prochain point � lire.
     */
    private int index;

    /**
     * Construit une fonction qui utilisera les donn�es du fichier sp�cifi�.
     * Si le fichier est un r�pertoire, alors tous les fichiers de ce r�pertoire
     * seront utilis�s.
     *
     * @param  file Fichier de donn�es QuikScat de niveau L1B.
     * @throws IOException si le ne peut pas �tre lu.
     */
    public WindImageFunction(final File file) {
        this(new File[] {file});
    }

    /**
     * Construit une fonction qui utilisera les donn�es des fichiers sp�cifi�s.
     *
     * @param  files Fichiers de donn�es QuikScat de niveau L1B.
     * @throws IOException si un ou des fichiers ne peuvent pas �tre lus.
     */
    public WindImageFunction(final File[] files) {
        super(GeographicCoordinateSystem.WGS84);
        final Set<File> list = new LinkedHashSet<File>();
        for (int i=0; i<files.length; i++) {
            final File file = files[i];
            if (file.isDirectory()) {
                list.addAll(Arrays.asList(file.listFiles()));
            } else {
                list.add(file);
            }
        }
        this.files = list.toArray(new File[list.size()]);
    }

    /**
     * Positionne le curseur au d�but du flot de donn�es. Lorsque <code>ImageFunction</code>
     * a besoin de conna�tre les donn�es qui constituent une image, il va d'abord appeller
     * <code>rewind()</code>, puis fera une s�rie d'appel � {@link #next}.
     *
     * @throws IOException si l'op�ration a �chou�e.
     */
    protected final void rewind() throws IOException {
        index = 0;
        if (fileIndex > 1) {
            count         = 0;
            fileIndex     = 0;
            longitude     = null;
            latitude      = null;
            windSpeed     = null;
            windDirection = null;
        }
    }

    /**
     * Retourne les coordonn�es (<var>x</var>,<var>y</var>,<var>z</var>) de la donn�e
     * courante, puis passe au point suivant. S'il ne reste plus de point � balayer,
     * alors cette m�thode retourne <code>null</code>.
     *
     * @throws IOException si la lecture a �chou�e.
     */
    protected final double[] next(final double[] data) throws IOException {
        while (index >= count) try {
            if (fileIndex >= files.length) {
                return null;
            }
            final QuikscatParser parser = new QuikscatParser(files[fileIndex]);
            try {
                count = load(parser);
            } finally {
                parser.close();
            }
            index = 0;
            fileIndex++;
        } catch (HDFException exception) {
            final IOException ioe = new IOException(exception.getLocalizedMessage());
            ioe.initCause(exception);
            throw ioe;
        }
        return compute(index++, data);
    }

    /**
     * Proc�de � la lecture des donn�es. Cette m�thode est appel�e automatiquement
     * par {@link #next} lorsqu'une nouvelle lecture de donn�es est n�cessaire.
     * L'impl�mentation par d�faut obtient les s�ries de donn�es pour les param�tres
     * <code>"wvc_lon"</code>, <code>"wvc_lat"</code>, <code>"wind_speed_selection"</code>
     * et <code>"wind_dir_selection"</code>. Il est de la responsabilit� de l'appellant de
     * fermer l'objet <code>parser</code> apr�s la lecture.
     *
     * @param  parser D�codeur de donn�es � utiliser.
     * @return Le nombre de valeurs qui pourront �tre calcul�es � partir des donn�es lues.
     *         Lors des appels � {@link #compute}, l'index variera de 0 inclusivement
     *         jusqu'� <var>n</var> exclusivement, ou <var>n</var> est la valeur retourn�e
     *         par cette m�thode.
     * @throws HDFException si la lecture du fichier a �chou�.
     */
    protected int load(final QuikscatParser parser) throws HDFException {
        longitude     = parser.getDataSet("wvc_lon");
        latitude      = parser.getDataSet("wvc_lat");
        windSpeed     = parser.getDataSet("wind_speed_selection");
        windDirection = parser.getDataSet("wind_dir_selection");
        return windSpeed.getRowCount() * windSpeed.getColumnCount();
    }

    /**
     * Calcule la valeur du param�tre � l'index sp�cifi�. La valeur de l'index
     * est comprise dans les limites des tableaux {@link #longitude}, {@link #latitude},
     * {@link #windSpeed} et {@link #windDirection}. L'impl�mentation par d�faut retourne
     * le vitesse du vent. Cette m�thode est appel�e automatiquement par {@link #next}
     * lorsque n�cessaire.
     */
    protected double[] compute(final int index, final double[] data) {
        final double x = longitude.getFlat(index);
        final double y =  latitude.getFlat(index);
        final double z = windSpeed.getFlat(index);
        if (data != null) {
            data[0] = x;
            data[1] = y;
            data[2] = z;
            return data;
        }
        return new double[] {x,y,z};
    }

    /**
     * Free all resources used by this <code>ImageFunction</code>.
     * Trying to use this object after <code>dispose()</code> may
     * fail.
     *
     * @throws IOException If an I/O operation was required and failed.
     */
    public synchronized void dispose() throws IOException {
        super.dispose();
        count         = 0;
        index         = 0;
        fileIndex     = 0;
        longitude     = null;
        latitude      = null;
        windSpeed     = null;
        windDirection = null;
    }

    /**
     * Retourne le poid � donner � une valeur en fonction de sa
     * distance.
     *
     * Cette m�thode est inspir�e de la m�thode utilis�e par la Nasa pour cr�er des images
     * d'anomalie de la hauteur de l'eau � partir des donn�es de Topex/Poseidon:
     *
     * <blockquote>
     *     The color images of TOPEX data are produced using a radial exponential weighting
     *     function to fill in the gaps between ground tracks. This fill-in is performed in
     *     order to obtain a smoother, more understandable presentation of the data.   Each
     *     pixel on the output graphical image is assigned a value,   which is derived from
     *     whatever raw TOPEX measurements fall within a 9 degree radius of the point being
     *     "filled in". Each raw data value which is within the cutoff radius is assigned a
     *     weighting value. Raw data points closest to the "fill-in" pixel get a value near
     *     1, while points near the cutoff radius get a value nearer 0.  The final weighted
     *     value at the "fill in" pixel is determined in two steps.   First, a numerator is
     *     formed, the sum of the various raw data values, each multiplied by its own
     *     weighting value.  Then that sum is divided by a denominator, which is the sum of
     *     the weighting values which were used in forming the numerator. 
     * </blockquote>
     *
     * <P align="right"><i>How TOPEX Images are Generated</i> in
     * <A HREF="http://podaac.jpl.nasa.gov/topex/www/process.html">http://podaac.jpl.nasa.gov/topex/www/process.html</A>
     *
     * @param  distance Distance entre la position d'une valeur et
     *         le centre du pixel, en m�tres.  Cette distance sera
     *         pr�cise � la condition qu'un syst�me de coordonn�es
     *         appropri�  ait �t� sp�cifi� lors de la construction
     *         de cet objet.
     * @return Un poid � donner aux mesures qui se trouvent � cette
     *         distance. Il peut s'agir de n'importe quel nombre r�el
     *         positif (pas n�cessairement entre 0 et 1).
     */
    protected double getWeight(final double distance) {
        // Le nombre au d�nominateur est la distance � laquelle le poid
        // n'est plus que de 37% de ce qu'il �tait au centre. Au double
        // de cette distance, le poid est de 14%.
        return Math.exp(distance / -25000);
    }

    /**
     * Display in a windows the wind image for the specified files. This method is
     * for testing purpose only. See {@link EkmanPumpingImageFunction#main} for a
     * more complete console application.
     */
    public static void main(String[] args) throws IOException {
        final Arguments arguments = new Arguments(args);
        if (args.length == 0) {
            arguments.out.println("Voir EkmanPumpingImageFunction pour la version compl�te.");
            return;
        }
        args = arguments.getRemainingArguments(Integer.MAX_VALUE);
        final File[] files = new File[args.length];
        for (int i=0; i<args.length; i++) {
            files[i] = new File(args[i]);
        }
        final WindImageFunction function = new WindImageFunction(files);
        if (false) {
            function.setGeographicArea(new Rectangle2D.Double(-180, -90, 360, 180));
        }
        function.show("Vent", 1024, 512);
        function.dispose();
    }
}
