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

// Entr�s/sorties
import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

// Lecture de fichiers HDF
import fr.ird.io.hdf4.Parser;
import fr.ird.io.hdf4.DataSet;
import ncsa.hdf.hdflib.HDFException;

// Date et heures
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

// SEAGIS dependencies
import net.seagis.gc.GridCoverage;
import net.seagis.ct.TransformException;


/**
 * Classe construisant une image repr�sentant le pompage d'Ekman
 * calcul� � partir des donn�es de vent mesur� par le satellite
 * QuikScat. Cette classe utilise les fichiers de donn�es de niveau L2B.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class EkmanPumpingImageFunction extends WindImageFunction
{
    /**
     * Objet ayant la charge de calculer la vorticit�
     * � partir des donn�es qu'on lui fournira.
     */
    private final EkmanPumpingCalculator calculator = new EkmanPumpingCalculator4P();

    /**
     * Latitude maximale au del� de laquelle on ne calculera plus
     * la vorticit�.
     */
    private final double maxLatitude = 72;

    /**
     * Gives the step point to calculate vorticity.
     * 1 means use the nearest points in the wind data grid.
     */
    private final int deltaIndex = 1;

    /**
     * Nombre de donn�es dans chaque ligne. En d'autres
     * mots, il s'agit du nombre de colonnes.
     */
    private int rowLength;

    /**
     * Nombre de donn�es dans chaque ligne moins le nombre
     * de donn�es n�cessaire pour calculer la vorticit�
     * (@link #deltaIndex}).
     */
    private int reducedRowLength;
    
    /**
     * Start time for current data.
     */
    private Date beginningTime;

    /**
     * Construct an image
     */
    public EkmanPumpingImageFunction(final File file) throws IOException
    {
        super(file);
        setGeographicArea(new Rectangle2D.Double(-180, -maxLatitude, 360, 2*maxLatitude));
        setMinimum       (-5E-6);
        setMaximum       (+5E-6);
        setColorPalette  ("RedBlue");
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
    protected int load(final QuikscatParser parser) throws HDFException
    {
        super.load(parser);
        beginningTime    = parser.getStartTime();
        rowLength        = windSpeed.getColumnCount();
        reducedRowLength = rowLength - deltaIndex;
        return (windSpeed.getRowCount()-deltaIndex) * (windSpeed.getColumnCount()-deltaIndex);
    }

    /**
     * Compute Ekman pumping at the specified index.
     */
    protected double[] compute(final int index, final double[] data)
    {
        final int sourceIndex1 = (index/reducedRowLength)*rowLength + (index % reducedRowLength);
        final int sourceIndex2 = sourceIndex1+deltaIndex;
        final int sourceIndex3 = sourceIndex1+rowLength;
        final int sourceIndex4 = sourceIndex3+deltaIndex;

        double x1 = longitude.get(sourceIndex1);
        double x2 = longitude.get(sourceIndex2);
        double x3 = longitude.get(sourceIndex3);
        double x4 = longitude.get(sourceIndex4);

        final double y1 =  latitude.get(sourceIndex1);
        final double y2 =  latitude.get(sourceIndex2);
        final double y3 =  latitude.get(sourceIndex3);
        final double y4 =  latitude.get(sourceIndex4);

        final double a1 = windSpeed.get(sourceIndex1);
        final double a2 = windSpeed.get(sourceIndex2);
        final double a3 = windSpeed.get(sourceIndex3);
        final double a4 = windSpeed.get(sourceIndex4);

        final double d1 = Math.toRadians(windDirection.get(sourceIndex1));
        final double d2 = Math.toRadians(windDirection.get(sourceIndex2));
        final double d3 = Math.toRadians(windDirection.get(sourceIndex3));
        final double d4 = Math.toRadians(windDirection.get(sourceIndex4));

        if (Math.abs(x1-x2)>=180 || Math.abs(x1-x3)>=180 || Math.abs(x1-x4)>=180
                                 || Math.abs(x2-x3)>=180 || Math.abs(x2-x4)>=180
                                                         || Math.abs(x3-x4)>=180)   
        {
            if (x1 >= 180) x1 -= 360;
            if (x2 >= 180) x2 -= 360;
            if (x3 >= 180) x3 -= 360;
            if (x4 >= 180) x4 -= 360;
        }

        double xo = 0.25*(x1 + x2 + x3 + x4);
        double yo = 0.25*(y1 + y2 + y3 + y4);
        double pumping;

        if (Math.abs(yo) >= maxLatitude)
        {
            pumping = Double.NaN;
        }
        else try
        {
            pumping = calculator.pumping(x1, y1, a1*Math.sin(d1), a1*Math.cos(d1),
                                         x2, y2, a2*Math.sin(d2), a2*Math.cos(d2),
                                         x3, y3, a3*Math.sin(d3), a3*Math.cos(d3),
                                         x4, y4, a4*Math.sin(d4), a4*Math.cos(d4),
                                         xo, yo);
        }
        catch (TransformException exception)
        {
            pumping = Double.NaN;
            exception.printStackTrace();
        }
        if (xo<0) xo += 360;
        if (data!=null)
        {
            data[0] = xo;
            data[1] = yo;
            data[2] = pumping;
            return data;
        }
        return new double[] {xo, yo, pumping};
    }

    /**
     * Run the program.
     */
    public static void main(String[] args) throws IOException
    {
        final EkmanPumpingImageFunction function = new EkmanPumpingImageFunction(
              new File("E:/PELOPS/Images/QuikSCAT/L2B/1999/220/"));
        final GridCoverage image = function.show("Pompage", 1600, 640);
        function.dispose();
    }
}