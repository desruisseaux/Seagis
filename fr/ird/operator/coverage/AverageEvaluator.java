/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package fr.ird.operator.coverage;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.Utilities;

// G�om�trie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

// Java Advanced Imaging et divers
import java.awt.image.RenderedImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;


/**
 * Une fonction calculant la valeur moyenne
 * des pixels dans une r�gion g�ographique.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class AverageEvaluator extends AbstractEvaluator implements Evaluator
{
    /**
     * Construit un �valuateur par d�faut.
     */
    public AverageEvaluator()
    {}

    /**
     * Retourne le nom de cette op�ration.
     */
    public String getName()
    {return "Average";}

    /**
     * Evalue la fonction pour une zone g�ographique de la couverture sp�cifi�e.
     * Cette fonction est �valu�e pour chaque bande de la couverture (ou image).
     *
     * @param coverage La couverture sur laquelle appliquer la fonction.
     * @param area La r�gion g�ographique sur laquelle �valuer la fonction.
     *        Les coordonn�es de cette r�gion doivent �tre exprim�es selon
     *        le syst�me de coordonn�es de <code>coverage</code>.
     */
    public ParameterValue[] evaluate(final GridCoverage coverage, final Shape area)
    {
        final RenderedImage        data = coverage.getRenderedImage(true);
        final AffineTransform transform = (AffineTransform) coverage.getGridGeometry().getGridToCoordinateSystem2D();
        final Point2D.Double coordinate = new Point2D.Double();
        final Rectangle2D    areaBounds = area.getBounds2D();
        final Rectangle          bounds = getBounds(areaBounds, transform, data);
        final int[]               count = new int[data.getSampleModel().getNumBands()];
        final double[]              sum = new double[count.length];
        double[] values = null;
        if (!bounds.isEmpty())
        {
            final RectIter iterator = RectIterFactory.create(data, bounds);
            for (int y=bounds.y; !iterator.finishedLines(); y++)
            {
                for (int x=bounds.x; !iterator.finishedPixels(); x++)
                {
                    assert bounds.contains(x,y);
                    coordinate.x = x;
                    coordinate.y = y;
                    // TODO: que faire si 'area' intercepte le pixel mais pas le centre?
                    //       'Shape.intersects' risque de ne pas �tre assez pr�cis.
                    if (area.contains(transform.transform(coordinate, coordinate)))
                    {
                        values = iterator.getPixel(values);
                        for (int i=0; i<values.length; i++)
                        {
                            final double z = values[i];
                            if (!Double.isNaN(z))
                            {
                                sum[i] += z;
                                count[i]++;
                            }
                        }
                    }
                    iterator.nextPixel();
                }
                iterator.startPixels();
                iterator.nextLine();
            }
        }
        final ParameterValue[] result = new ParameterValue[sum.length];
        for (int i=0; i<result.length; i++)
        {
            result[i] = new ParameterValue.Double(coverage, this);
            result[i].setValue(sum[i]/count[i], null);
        }
        return result;
    }
}
