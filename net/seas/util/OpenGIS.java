/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package net.seas.util;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cs.*;
import net.seas.opengis.ct.*;
import net.seas.opengis.pt.*;

// Miscellaneous
import javax.units.Unit;


/**
 * A set of static methods working on OpenGIS objects.  Some of those methods
 * are useful, but not really rigorous. This is why they do not appear in the
 * "official" package, but instead in this private one. <strong>Do not rely on
 * this API!</strong> It may change in incompatible way in any future version.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class OpenGIS
{
    /**
     * Do not allow creation of
     * instances of this class.
     */
    private OpenGIS()
    {}

    /**
     * Returns the first horizontal datum found in a coordinate system,
     * or <code>null</code> if there is none. Note: if a future version,
     * we may implement this method directly into {@link CoordinateSystem}
     * (not sure yet if it would be a good idea).
     */
    public static HorizontalDatum getHorizontalDatum(final CoordinateSystem cs)
    {
        if (cs instanceof HorizontalCoordinateSystem)
        {
            return ((HorizontalCoordinateSystem) cs).getHorizontalDatum();
        }
        if (cs instanceof CompoundCoordinateSystem)
        {
            HorizontalDatum datum;
            final CompoundCoordinateSystem comp = (CompoundCoordinateSystem) cs;
            if ((datum=getHorizontalDatum(comp.getHeadCS())) != null) return datum;
            if ((datum=getHorizontalDatum(comp.getTailCS())) != null) return datum;
        }
        return null;
    }

    /**
     * Returns the first vertical datum found in a coordinate system,
     * or <code>null</code> if there is none. Note: if a future version,
     * we may implement this method directly into {@link CoordinateSystem}
     * (not sure yet if it would be a good idea).
     */
    public static VerticalDatum getVerticalDatum(final CoordinateSystem cs)
    {
        if (cs instanceof VerticalCoordinateSystem)
        {
            return ((VerticalCoordinateSystem) cs).getVerticalDatum();
        }
        if (cs instanceof CompoundCoordinateSystem)
        {
            VerticalDatum datum;
            final CompoundCoordinateSystem comp = (CompoundCoordinateSystem) cs;
            if ((datum=getVerticalDatum(comp.getHeadCS())) != null) return datum;
            if ((datum=getVerticalDatum(comp.getTailCS())) != null) return datum;
        }
        return null;
    }

    /**
     * Returns the first temporal datum found in a coordinate system,
     * or <code>null</code> if there is none. Note: if a future version,
     * we may implement this method directly into {@link CoordinateSystem}
     * (not sure yet if it would be a good idea).
     */
    public static TemporalDatum getTemporalDatum(final CoordinateSystem cs)
    {
        if (cs instanceof TemporalCoordinateSystem)
        {
            return ((TemporalCoordinateSystem) cs).getTemporalDatum();
        }
        if (cs instanceof CompoundCoordinateSystem)
        {
            TemporalDatum datum;
            final CompoundCoordinateSystem comp = (CompoundCoordinateSystem) cs;
            if ((datum=getTemporalDatum(comp.getHeadCS())) != null) return datum;
            if ((datum=getTemporalDatum(comp.getTailCS())) != null) return datum;
        }
        return null;
    }

    /**
     * Transform an envelope. The transformation is only approximative.
     *
     * @param  transform The transform to use.
     * @param  envelope Envelope to transform. This envelope will not be modified.
     * @return The transformed envelope. It may not have the same number of dimensions
     *         than the original envelope.
     * @throws TransformException if a transform failed.
     */
    public static Envelope transform(final MathTransform transform, final Envelope envelope) throws TransformException
    {
        final int sourceDim = transform.getDimSource();
        final int targetDim = transform.getDimTarget();
        if (envelope.getDimension() != sourceDim)
        {
            throw new MismatchedDimensionException(sourceDim, envelope.getDimension());
        }
        int           coordinateNumber = 0;
        Envelope           transformed = null;
        CoordinatePoint       targetPt = null;
        final CoordinatePoint sourcePt = new CoordinatePoint(sourceDim);
        for (int i=sourceDim; --i>=0;) sourcePt.ord[i]=envelope.getMinimum(i);

  loop: do
        {
            // Transform a point and add the transformed
            // point to the destination envelope.
            targetPt=transform.transform(sourcePt, targetPt);
            if (transformed!=null) transformed.add(targetPt);
            else transformed=new Envelope(targetPt,targetPt);

            // Get the next point's coordinate.   The 'coordinateNumber' variable should
            // be seen as a number in base 3 where the number of digits is equals to the
            // number of dimensions. For example, a 4-D space would have numbers ranging
            // from "0000" to "2222". The digits are then translated into minimal, central
            // or maximal ordinates.
            int n = ++coordinateNumber;
            for (int i=sourceDim; --i>=0;)
            {
                switch (n % 3)
                {
                    case 0: sourcePt.ord[i] = envelope.getMinimum(i); n/=3; break;
                    case 1: sourcePt.ord[i] = envelope.getCenter (i); continue loop;
                    case 2: sourcePt.ord[i] = envelope.getMaximum(i); continue loop;
                }
            }
            break;
        }
        while (true);
        return transformed;
    }
}
