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
package fr.ird.io.n1b;

// J2SE dependencies
import java.awt.Point;
import java.util.Date;
import java.util.Arrays;
import java.awt.Dimension;
import java.awt.geom.Point2D;

// Geotools dependencies
import org.geotools.pt.CoordinatePoint;
import org.geotools.gc.LocalizationGrid;
import org.geotools.ct.TransformException;


/**
 * Grille de localisation construites à partir des informations présentes dans un fichier
 * N1B. En plus des coordonnées géographiques habituelles (longitude/latitude), cette grille
 * de localisation mémorise une date pour chacune des lignes de la grille.
 *
 * @version $Id$
 * @author Remi EVE
 * @author Martin Desruisseaux
 */
final class LocalizationGridN1B extends LocalizationGrid
{
    /**
     * Dates d'acquisition des lignes du fichiers N1B, en nombre de
     * millisecondes écoulées depuis le 1er janvier 1970 00:00 UTC.
     */ 
    private long[] time;
    
    /** 
     * Construit une grille de localisation spécifique aux fichiers de type N1B.
     *
     * @param width  La largeur de la grille.
     * @param height La hauteur de la grille.
     */
    public LocalizationGridN1B(final int width, final int height)
    {
        super(width, height);        
        time = new long[height];
    }

    /**
     * Définie la date d'acquisition de la ligne spécifiée.
     *
     * @param row  Numéro de la ligne, à partir de 0.
     * @param date Date d'acquisition de la ligne.
     */
    public void setTime(final int row, final Date date)
    {
        time[row] = date.getTime();
    }

    /**
     * Retourne la date d'acquisition de la ligne spécifiée.
     *
     * @param  row numéro de la ligne, à partir de 0.
     * @return La date d'acquisition de la ligne..
     */
    public Date getTime(final int numRow)
    {
        return new Date(time[numRow]);
    }

    /**
     * Indique si les éléments du tableau spécifié sont en ordre croissant.
     *
     * @param array Tableau de nombres à vérifier.
     * @return vrai si les éléments du tableau sont en ordre croissant.
     */    
    private static boolean isSorted(final long[] array)
    {
        for (int i=1; i<array.length; i++)
            if (array[i] < array[i-1])
                return false;
        return true;
    }
    
    /**
     * Corrige les latitudes et longitudes des points de contrôle de la grille
     * en utilisant les données d'un bulletin CLS. Le bulletin CLS est doit
     * contenir les positions "réelles" du satellite.
     */
    public void applyCorrection(final Bulletin bulletin) throws TransformException
    {
        final Dimension size = getSize();
        final Point   indice = new Point(size.width/2, size.height);
        while (--indice.y>=0)
        {
            final Point2D        gridPoint = getLocalizationPoint(indice);
            final CoordinatePoint refPoint = bulletin.getGeographicCoordinate(getTime(indice.y));
            System.out.println("\nRéel : " + refPoint);
            System.out.println("Grille : " + gridPoint);
//            System.out.println("Diff : " + new Point2D.Double((ptReel.getX() - ptN1B.getX()), (ptReel.getY() - ptN1B.getY())));
        }
    }
}
