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

// Géométrie et images
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;
import fr.ird.image.AbstractImageFunction;

// Lecture de fichiers HDF
import java.io.File;
import java.io.IOException;
import fr.ird.io.hdf4.Parser;
import fr.ird.io.hdf4.DataSet;
import ncsa.hdf.hdflib.HDFException;

// OpenGIS (implémentation seagis)
import net.seagis.gc.GridCoverage;
import net.seagis.cs.GeographicCoordinateSystem;


/**
 * Classe construisant une image représentant l'amplitude
 * du vent mesuré par le satellite QuikScat. Cette classe
 * utilise les fichiers de données de niveau L2B.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
class WindImageFunction extends AbstractImageFunction
{
    /**
     * Liste des fichiers de données à utiliser.
     */
    private final File[] files;

    /**
     * Index du prochain fichier à lire.
     */
    private int fileIndex;

    /**
     * Ensemble des coordonnéess <var>x</var> des points.
     */
    protected DataSet longitude;

    /**
     * Ensemble des coordonnéess <var>y</var> des points.
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
     * Nombre de points à lire.
     */
    private int count;

    /**
     * Index du prochain point à lire.
     */
    private int index;

    /**
     * Construit une fonction qui utilisera les données du fichier spécifié.
     * Si le fichier est un répertoire, alors tous les fichiers de ce répertoire
     * seront utilisés.
     *
     * @param file Fichier de données QuikScat de niveau L1B.
     */
    public WindImageFunction(final File file)
    {
        super(GeographicCoordinateSystem.WGS84);
        if (file.isDirectory())
        {
            files = file.listFiles();
        }
        else
        {
            files = new File[] {file};
        }
    }

    /**
     * Construit une fonction qui utilisera les données des fichiers spécifiés.
     *
     * @param  files Fichiers de données QuikScat de niveau L1B.
     */
    public WindImageFunction(final File[] files)
    {
        super(GeographicCoordinateSystem.WGS84);
        this.files = (File[]) files.clone();
    }

    /**
     * Positionne le curseur au début du flot de données. Lorsque <code>ImageFunction</code>
     * a besoin de connaître les données qui constituent une image, il va d'abord appeller
     * <code>rewind()</code>, puis fera une série d'appel à {@link #next}.
     *
     * @throws IOException si l'opération a échouée.
     */
    protected final void rewind() throws IOException
    {
        index = 0;
        if (fileIndex > 1)
        {
            count         = 0;
            fileIndex     = 0;
            longitude     = null;
            latitude      = null;
            windSpeed     = null;
            windDirection = null;
        }
    }

    /**
     * Retourne les coordonnées (<var>x</var>,<var>y</var>,<var>z</var>) de la donnée
     * courante, puis passe au point suivant. S'il ne reste plus de point à balayer,
     * alors cette méthode retourne <code>null</code>.
     *
     * @throws IOException si la lecture a échouée.
     */
    protected final double[] next(final double[] data) throws IOException
    {
        while (index >= count) try
        {
            if (fileIndex >= files.length)
            {
                return null;
            }
            final QuikscatParser parser = new QuikscatParser(files[fileIndex]);
            count = load(parser);
            parser.close();
            index = 0;
            fileIndex++;
        }
        catch (HDFException exception)
        {
            final IOException ioe = new IOException(exception.getLocalizedMessage());
            ioe.initCause(exception);
            throw ioe;
        }
        return compute(index++, data);
    }

    /**
     * Procède à la lecture des données. Cette méthode est appelée automatiquement
     * par {@link #next} lorsqu'une nouvelle lecture de données est nécessaire.
     * L'implémentation par défaut obtient les séries de données pour les paramètres
     * <code>"wvc_lon"</code>, <code>"wvc_lat"</code>, <code>"wind_speed_selection"</code>
     * et <code>"wind_dir_selection"</code>. Il est de la responsabilité de l'appellant de
     * fermer l'objet <code>parser</code> après la lecture.
     *
     * @param  parser Décodeur de données à utiliser.
     * @return Le nombre de valeurs qui pourront être calculées à partir des données lues.
     *         Lors des appels à {@link #compute}, l'index variera de 0 inclusivement
     *         jusqu'à <var>n</var> exclusivement, ou <var>n</var> est la valeur retournée
     *         par cette méthode.
     * @throws HDFException si la lecture du fichier a échoué.
     */
    protected int load(final QuikscatParser parser) throws HDFException
    {
        longitude     = parser.getDataSet("wvc_lon");
        latitude      = parser.getDataSet("wvc_lat");
        windSpeed     = parser.getDataSet("wind_speed_selection");
        windDirection = parser.getDataSet("wind_dir_selection");
        return windSpeed.getRowCount() * windSpeed.getColumnCount();
    }

    /**
     * Calcule la valeur du paramètre à l'index spécifié. La valeur de l'index
     * est comprise dans les limites des tableaux {@link #longitude}, {@link #latitude},
     * {@link #windSpeed} et {@link #windDirection}. L'implémentation par défaut retourne
     * le vitesse du vent. Cette méthode est appelée automatiquement par {@link #next}
     * lorsque nécessaire.
     */
    protected double[] compute(final int index, final double[] data)
    {
        final double x = longitude.get(index);
        final double y =  latitude.get(index);
        final double z = windSpeed.get(index);
        if (data!=null)
        {
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
    public synchronized void dispose() throws IOException
    {
        super.dispose();
        longitude     = null;
        latitude      = null;
        windSpeed     = null;
        windDirection = null;
    }

    /**
     * Run the program.
     */
    public static void main(String[] args) throws IOException
    {
        args = new String[]
        {
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00704.20001241608",
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00705.20001241609",
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00706.20001241609",
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00707.20001241609",
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00708.20001241653"
        };
        final File[] files = new File[args.length];
        for (int i=0; i<args.length; i++)
        {
            files[i] = new File(args[i]);
        }
//      final WindImageFunction function = new WindImageFunction(files[0]);
        final WindImageFunction function = new WindImageFunction(
              new File("E:/PELOPS/Images/QuikSCAT/L2B/1999/220/"));
        final GridCoverage image = function.show("Vent", 1024, 512);
        function.dispose();
    }
}
