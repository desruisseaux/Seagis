/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2001 Institut de Recherche pour le Développement
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
package fr.ird.io.coverage;

// OpenGIS
import org.opengis.referencing.operation.TransformException;

// Geotools dependencies
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.CoordinateTransformationFactory;
import org.geotools.ct.MathTransform;

import org.geotools.units.Unit;
import org.geotools.gc.GridRange;
import org.geotools.cv.SampleDimension;
import org.geotools.resources.CTSUtilities;
import org.geotools.io.coverage.PropertyException;


/**
 * A yet more ugly properties parser for text headers. This properties
 * parser has an ugly patch for transforming enveloppe expressed in the
 * wrong coordinate system.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class LenientPropertyParser2 extends LenientPropertyParser {
    /**
     * Construct a <code>LenientPropertyParser</code> object.
     *
     * @param  bands The bands to be returned by {@link #getSampleDimension}.
     * @param  outputPattern The pattern for output file. Example: <code>"'SST'yyDDD"</code>.
     * @throws PropertyException if the initialization failed.
     */
    public LenientPropertyParser2(SampleDimension[] bands, String outputPattern) throws PropertyException {
        super(bands, outputPattern);
    }

    /**
     * Returns the envelope.
     *
     * @throws NoSuchElementException if a required value is missing.
     */
    public synchronized Envelope getEnvelope() throws PropertyException {
        Envelope envelope = super.getEnvelope();
        if (Unit.METRE.canConvert(getUnits())) try {
            final CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();

            MathTransform transform;
            final CoordinateSystem sourceCS = GeographicCoordinateSystem.WGS84;
            final CoordinateSystem targetCS = ((CompoundCoordinateSystem) getCoordinateSystem()).getHeadCS();
            transform = factory.createFromCoordinateSystems(sourceCS, targetCS).getMathTransform();
            transform = factory.getMathTransformFactory().createPassThroughTransform(0, transform, 1);
            envelope  = CTSUtilities.transform(transform, envelope);
        } catch (TransformException exception) {
            throw new PropertyException(exception.getLocalizedMessage(), exception);
        }
        return envelope;
    }
}
