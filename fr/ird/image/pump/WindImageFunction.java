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
final class WindImageFunction extends AbstractImageFunction
{
    /**
     * Ensemble des coordonnéess <var>x</var> des points.
     */
    private final DataSet longitude;

    /**
     * Ensemble des coordonnéess <var>y</var> des points.
     */
    private final DataSet latitude;

    /**
     * Ensemble des vitesses du vent en chaque points.
     */
    private final DataSet windSpeed;

    /**
     * Ensemble des directions du vent en chaque points.
     */
    private final DataSet windDirection;

    /**
     * Nombre de points à lire.
     */
    private final int count;

    /**
     * Index du prochain point à lire.
     */
    private int index;

    /**
     * Construit une fonction qui utilisera
     * les données du fichier spécifié.
     *
     * @param  filename Fichier de données QuikScat de niveau L1B.
     * @throws HDFException si la lecture du fichier a échouée.
     */
    public WindImageFunction(final File filename) throws HDFException
    {
        super(GeographicCoordinateSystem.WGS84);
        final Parser parser = new QuikscatParser(filename);
        longitude           = parser.getDataSet("wvc_lon");
        latitude            = parser.getDataSet("wvc_lat");
        windSpeed           = parser.getDataSet("wind_speed_selection");
        windDirection       = parser.getDataSet("wind_dir_selection");
        parser.close();

        count = windSpeed.getRowCount() * windSpeed.getColumnCount();
    }

    /**
     * Positionne le curseur au début du flot de données. Lorsque <code>ImageFunction</code>
     * a besoin de connaître les données qui constituent une image, il va d'abord appeller
     * <code>rewind()</code>, puis fera une série d'appel à {@link #next}.
     */
    protected void rewind()
    {
        index = 0;
    }

    /**
     * Retourne les coordonnées (<var>x</var>,<var>y</var>,<var>z</var>) de la donnée
     * courante, puis passe au point suivant. S'il ne reste plus de point à balayer,
     * alors cette méthode retourne <code>null</code>.
     */
    protected double[] next(final double[] data)
    {
        if (index >= count) return null;
        final double x = longitude.get(index);
        final double y =  latitude.get(index);
        final double z = windSpeed.get(index);
        index++;
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
     * Lance la création des images.
     */
    final void run() throws IOException
    {
//      setGeographicArea(new Rectangle2D.Double(0, -75, 360, 150));
//      setImageSize     (new Dimension(1024,512));
//      setValueRange    (-5E-6, 5E-6);
//      setColors        ("applicationData/colors/RedBlue.pal");
//      final RenderedImage image = maker.getImage();
        final GridCoverage image = getGridCoverage("Vent", 1024, 512);
        if (image!=null)
        {
            java.awt.Frame frame=frame=new java.awt.Frame("Une passe");
            frame.add(new javax.media.jai.widget.ScrollingImagePanel(image.getRenderedImage(false), 400, 400));
            frame.pack();
            frame.show();
//          ImageIO.write(image, "png", new File("wind.png"));
        }
    }

    /**
     * Convert a set of string into a set of files.
     * This is used for {@link #main} methods only.
     */
    static File[] toFiles(final String[] args)
    {
        final File[] files = new File[args.length];
        for (int i=0; i<args.length; i++)
            files[i] = new File(args[i]);
        return files;
    }

    /**
     * Run the program.
     */
    public static void main(String[] args) throws IOException, HDFException
    {
        args = new String[]
        {
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00704.20001241608",
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00705.20001241609",
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00706.20001241609",
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00707.20001241609",
            "E:/PELOPS/Images/QuikSCAT/L2B/1999/220/QS_S2B00708.20001241653"
        };
        final WindImageFunction function = new WindImageFunction(toFiles(args)[0]);
        function.run();
    }
}
