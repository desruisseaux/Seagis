/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D?veloppement
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
 *          Maison de la t?l?d?tection
 *          Institut de Recherche pour le d?veloppement
 *          500 rue Jean-Fran?ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.n1b.io;

// J2SE dependencies
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.util.Date;
import java.util.Arrays;
import java.awt.Dimension;
import java.awt.geom.Point2D;

// Geotools dependencies
import org.geotools.pt.CoordinatePoint;
import org.geotools.gc.LocalizationGrid;
import org.geotools.ct.TransformException;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.MathTransform;
import org.geotools.ct.MathTransformFactory;


/**
 * Grille de localisation construites ? partir des informations pr?sentes dans un fichier
 * N1B. En plus des coordonn?es g?ographiques habituelles (longitude/latitude), cette grille
 * de localisation m?morise une date pour chacune des lignes de la grille.
 *
 * @version $Id$
 * @author Remi EVE
 * @author Martin Desruisseaux
 */
public final class LocalizationGridN1B extends LocalizationGrid
{
    /** Nombre de pixels par ligne ou passe du satellite. */
    private final int NB_PIXEL_LINE               = ImageReaderN1B.NB_PIXEL_LINE;
    
    /** Offset du premier point de controle. */
    private final int OFFSET_FIRST_CONTROL_POINT  = ImageReaderN1B.OFFSET_FIRST_CONTROL_POINT;
    
    /** Interval entre deux points de controles. */
    private final int INTERVAL_NEXT_CONTROL_POINT = ImageReaderN1B.INTERVAL_NEXT_CONTROL_POINT;
    
    /**
     * Dates d'acquisition des lignes du fichiers N1B, en nombre de
     * millisecondes ?coul?es depuis le 1er janvier 1970 00:00 UTC.
     */ 
    private long[] time;
    
    /**
     * Altitude d'acquisition des lignes du fichiers N1B, en km.
     */ 
    private float[] altitude;

    /** 
     * Construit une grille de localisation sp?cifique aux fichiers de type N1B.
     *
     * @param width  La largeur de la grille.
     * @param height La hauteur de la grille.
     */
    public LocalizationGridN1B(final int width, final int height)
    {
        super(width, height);        
        time = new long[height];
        altitude = new float[height];
    }

    /**
     * D?finie la date d'acquisition de la ligne sp?cifi?e.
     *
     * @param row  Num?ro de la ligne, ? partir de 0.
     * @param date Date d'acquisition de la ligne.
     */
    public void setTime(final int row, final Date date)
    {
        time[row] = date.getTime();
    }

    /**
     * Retourne la date d'acquisition de la ligne sp?cifi?e.
     *
     * @param  row num?ro de la ligne, ? partir de 0.
     * @return La date d'acquisition de la ligne..
     */
    public Date getTime(final int numRow)
    {
        return new Date(time[numRow]); 
    }

    /**
     * D?finie l'altitude d'acquisition de la ligne sp?cifi?e.
     *
     * @param row       Num?ro de la ligne, ? partir de 0.
     * @param altitude  Altitude d'acquisition de la ligne en km.
     */
    public void setAltitude(final int row, final float altitude)
    {
        this.altitude[row] = altitude;
    }

    /**
     * Retourne l'altitude d'acquisition de la ligne sp?cifi?e.
     *
     * @param  row num?ro de la ligne, ? partir de 0.
     * @return L'altitude d'acquisition de la ligne en km.
     */
    public float getAltitude(final int numRow)
    {
        return altitude[numRow]; 
    }

    /**
     * Indique si les ?l?ments du tableau sp?cifi? sont en ordre croissant.
     *
     * @param array Tableau de nombres ? v?rifier.
     * @return vrai si les ?l?ments du tableau sont en ordre croissant.
     */    
    private static boolean isSorted(final long[] array)
    {
        for (int i=1; i<array.length; i++)
            if (array[i] < array[i-1])
                return false;
        return true;
    }
    
    /**
     * Corrige les latitudes et longitudes des points de contr?le de la grille
     * en utilisant les donn?es d'un bulletin CLS. Le bulletin CLS est doit
     * contenir les positions "r?elles" du satellite. De plus, l'altitude du satellite est maintenant 
     * corrige par la meme occasion.
     *
     * @param bulletin le bulletin a utiliser.
     * @return la moyenne des corrections appliquees.
     */
    public Point2D applyCorrection(final Bulletin bulletin) throws TransformException
    {
        final Dimension size = getSize();
        final int width = (int)size.getWidth();        
        int indiceY = size.height;
        final MathTransform2D transform = (MathTransform2D)getMathTransform();                
        final Point2D gridPoint = new Point2D.Double();
        double avgCorrectionX = 0,
               avgCorrectionY = 0;
        
        // Compute coordinate x of nadir.
        final MathTransform mt = MathTransformFactory.getDefault().createAffineTransform(
                            new AffineTransform(INTERVAL_NEXT_CONTROL_POINT, 
                                                0, 
                                                0, 
                                                1, 
                                                OFFSET_FIRST_CONTROL_POINT-1, 0)).inverse();    
        final CoordinatePoint sourceNadir = new CoordinatePoint((NB_PIXEL_LINE-1)/2, 0);                                                   
        mt.transform(sourceNadir, sourceNadir); 
        
        while (--indiceY>=0)
        {            
            final Point2D source = new Point2D.Double(sourceNadir.getOrdinate(0), indiceY);            
            transform.transform(source, gridPoint);
            final CoordinatePoint refPoint = bulletin.getGeographicCoordinate(getTime(indiceY));
            /**
             * Correction de l'altitude.
             */
            setAltitude(indiceY, (float)(refPoint.getOrdinate(2) / 1000.0));
            
            /**
             * Correction des points de la grille par une translation de "offset" degre.
             */            
            final Point2D offset = new Point2D.Double(refPoint.getOrdinate(0) - gridPoint.getX(),
                                                      refPoint.getOrdinate(1) - gridPoint.getY());
            avgCorrectionX += offset.getX();
            avgCorrectionY += offset.getY();

            for (int indicePoint=0; indicePoint<width; indicePoint++) 
            {
                final Point ptSource   = new Point(indicePoint, indiceY);
                final Point2D ptTarget = getLocalizationPoint(ptSource);
                ptTarget.setLocation(ptTarget.getX() + offset.getX(), 
                                     ptTarget.getY() + offset.getY());
                setLocalizationPoint(ptSource, ptTarget);
        }
        }
        avgCorrectionX /= size.getHeight();
        avgCorrectionY /= size.getHeight();
        return new Point2D.Double(avgCorrectionX, avgCorrectionY);
    }
    
    /**
     * Corrige les latitudes et longitudes des points de contr?le de la grille
     * en utilisant un offset en latitude et en longitude. Cet offset sera applique a l'ensemble des
     * points de la grille.
     *
     * @param offset l'offset a applique en x et y.
     */
    public void applyCorrection(final Point2D offset) throws TransformException
    {        
        final int height = (int)getSize().getHeight();
        final int width  = (int)getSize().getWidth();

        for (int y=0 ; y<height ; y++)
        {
            for (int x=0 ; x<width ; x++)
            {
                final Point2D pt = getLocalizationPoint(new Point(x, y));
                setLocalizationPoint(x, y, pt.getX() + offset.getX(), pt.getY() + offset.getY());
            }
        }
    }            
    
    /**
     * Méthode temporaire : Applique une rotation sur la grille de points de localisation.
     *
     * @param teta  Angle de la rotation en radian.
     */
    public void rotateGrid(final double teta)
    {
        final AffineTransform at = AffineTransform.getRotateInstance(teta);
        
        final int height = (int)getSize().getHeight();
        final int width  = (int)getSize().getWidth();
        
        for (int col=0 ; col<width ; col++)
        {
            for (int row=0 ; row<height ; row++)
            {
                // Rotation appliquée sur chacun des points de localisation.
                final Point2D pt = getLocalizationPoint(new Point(col, row));
                at.transform(pt, pt);
                setLocalizationPoint(col, row, pt.getX(), pt.getY());                
            }        
        }
    }
}