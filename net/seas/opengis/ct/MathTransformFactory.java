/*
 * OpenGIS implementation in Java
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.opengis.ct;

// Miscellaneous
import javax.units.Unit;
import java.util.Locale;
import java.awt.geom.Point2D;
import net.seas.resources.Resources;
import net.seas.opengis.cs.Ellipsoid;
import java.util.NoSuchElementException;


/**
 * Creates math transforms. <code>MathTransformFactory</code> is a low level
 * factory that is used to create {@link MathTransform} objects.   Many high
 * level GIS applications will never need to use a <code>MathTransformFactory</code>
 * directly; they can use a {@link CoordinateTransformationFactory} instead.
 * However, the <code>MathTransformFactory</code> class is specified here,
 * since it can be used directly by applications that wish to transform other
 * types of coordinates (e.g. color coordinates, or image pixel coordinates).
 * <br><br>
 * A math transform is an object that actually does the work of applying
 * formulae to coordinate values.    The math transform does not know or
 * care how the coordinates relate to positions in the real world.  This
 * lack of semantics makes implementing <code>MathTransformFactory</code>
 * significantly easier than it would be otherwise.
 *
 * For example <code>MathTransformFactory</code> can create affine math
 * transforms. The affine transform applies a matrix to the coordinates
 * without knowing how what it is doing relates to the real world. So if
 * the matrix scales <var>Z</var> values by a factor of 1000, then it could
 * be converting meters into millimeters, or it could be converting kilometers
 * into meters.
 * <br><br>
 * Because math transforms have low semantic value (but high mathematical
 * value), programmers who do not have much knowledge of how GIS applications
 * use coordinate systems, or how those coordinate systems relate to the real
 * world can implement <code>MathTransformFactory</code>.
 *
 * The low semantic content of math transforms also means that they will be
 * useful in applications that have nothing to do with GIS coordinates.  For
 * example, a math transform could be used to map color coordinates between
 * different color spaces, such as converting (red, green, blue) colors into
 * (hue, light, saturation) colors.
 * <br><br>
 * Since a math transform does not know what its source and target coordinate
 * systems mean, it is not necessary or desirable for a math transform object
 * to keep information on its source and target coordinate systems.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.ct.CT_MathTransformFactory
 */
public class MathTransformFactory
{
    /**
     * List of registered math transforms.
     */
    private final MathTransform.Registration[] REGISTERED = new MathTransform.Registration[]
    {
        new         MercatorProjection.Registration(),
        new LambertConformalProjection.Registration(),
        new    StereographicProjection.Registration(),
        new    StereographicProjection.Registration(true),  // Polar
        new    StereographicProjection.Registration(false)  // Oblique
    };

    /**
     * Creates a transform by concatenating two existing transforms.
     * A concatenated transform acts in the same way as applying two
     * transforms, one after the other. The dimension of the output
     * space of the first transform must match the dimension of the
     * input space in the second transform. If you wish to concatenate
     * more than two transforms, then you can repeatedly use this method.
     *
     * @param  transform1 The first transform to apply to points.
     * @param  transform2 The second transform to apply to points.
     * @return The concatenated transform.
     */
    public MathTransform createConcatenatedTransform(final MathTransform transform1, final MathTransform transform2)
    {return null;} // TODO

    /**
     * Creates a transform which passes through a subset of ordinates to another transform.
     * This allows transforms to operate on a subset of ordinates.  For example,
     * if you have (Lat,Lon,Height) coordinates, then you may wish to convert the
     * height values from meters to feet without affecting the (Lat,Lon) values.
     * If you wanted to affect the (Lat,Lon) values and leave the Height values
     * alone, then you would have to swap the ordinates around to
     * (Height,Lat,Lon).  You can do this with an affine map.
     *
     * @param  firstAffectedOrdinate The lowest index of the affected ordinates.
     * @param  subTransform Transform to use for affected ordinates.
     * @return The pass through transform.
     */
    public MathTransform createPassThroughTransform(final int firstAffectedOrdinate, final MathTransform subTransform)
    {return null;} // TODO

    /**
     * Convenience method for creating a transform from a classification name and
     * parameters.
     *
     * @param classification The classification name of the transform (e.g. "Transverse_Mercator").
     * @param ellipsoid Ellipsoid parameter. "semi_major" and "semi_minor" parameters values will
     *                  be determined from ellipsoid's axis length and unit.
     * @param centroid  Centre de la projection. Souvent (mais <u>pas toujours</u>),
     *                  les coordonnées du centre seront celles qui, lorsque projetées,
     *                  donneraient les coordonnées (0,0). Notez que les coordonnées
     *                  qui seront retenues comme le centre de la carte ne seront pas
     *                  nécessairement identiques à celles qui auront été spécifiées.
     *                  Par exemple les projections transverses de Mercator placent la
     *                  longitude centrale au centre d'une de leurs "zones". D'autres
     *                  projections placent toujours la latitude centrale sur l'équateur.
     * @return The parameterized transform.
     * @throws NoSuchElementException if there is no transform for the specified classification.
     */
    public MathTransform createParameterizedTransform(final String classification, final Ellipsoid ellipsoid, final Point2D centroid) throws NoSuchElementException
    {
        final Unit axisUnit = ellipsoid.getAxisUnit();
        return createParameterizedTransform(classification, new Parameter[]
        {
            new Parameter("semi_major", Unit.METRE.convert(ellipsoid.getSemiMajorAxis(), axisUnit)),
            new Parameter("semi_minor", Unit.METRE.convert(ellipsoid.getSemiMinorAxis(), axisUnit))
        });
    }

    /**
     * Creates a transform from a classification name and parameters. The
     * client must ensure that all the linear parameters are expressed in
     * meters, and all the angular parameters are expressed in degrees.
     * Also, they must supply "semi_major" and "semi_minor" parameters
     * for cartographic projection transforms.
     *
     * @param  classification The classification name of the transform
     *         (e.g. "Transverse_Mercator"). Leading and trailing spaces
     *         are ignored, and comparaison is case-insensitive.
     * @param  parameters The parameter values in standard units.
     * @return The parameterized transform.
     * @throws NoSuchElementException if there is no transform for the specified classification.
     * @throws MissingParameterException if a parameter was required but not found.
     */
    public MathTransform createParameterizedTransform(final String classification, final Parameter[] parameters) throws NoSuchElementException, MissingParameterException
    {return getRegistration(classification).create(parameters);}

    /**
     * Returns a human readable name localized for the specified locale.
     * If no name is available for the specified locale, this method may
     * returns a name in an arbitrary locale.
     *
     * @param  classification The classification name of the transform
     *         (e.g. "Transverse_Mercator"). Leading and trailing spaces
     *         are ignored, and comparaison is case-insensitive.
     * @param  Locale The locale (e.g. {@link Locale#FRENCH}), or <code>null</code>
     *         for the current default locale.
     * @return Localized classification name (e.g. "<cite>Mercator transverse</cite>").
     * @throws NoSuchElementException if there is no transform for the specified classification.
     */
    public String getName(final String classification, final Locale locale) throws NoSuchElementException
    {return getRegistration(classification).getName(locale);}

    /**
     * Get the default parameters from a classification name. The
     * client may change any of those parameters and submit them
     * to {@link #createParameterizedTransform(String,Parameter[])}.
     *
     * @param  classification The classification name of the transform
     *         (e.g. "Transverse_Mercator"). Leading and trailing spaces
     *         are ignored, and comparaison is case-insensitive.
     * @return Default parameters for a transform of the specified classification.
     * @throws NoSuchElementException if there is no transform for the
     *         specified classification.
     */
    public Parameter[] getDefaultParameters(final String classification) throws NoSuchElementException
    {return getRegistration(classification).getDefaultParameters();}

    /**
     * Returns the registration for the specified classification.
     *
     * @param  classification The classification name of the transform
     *         (e.g. "Transverse_Mercator"). Leading and trailing spaces
     *         are ignored, and comparaison is case-insensitive.
     * @return The registration.
     * @throws NoSuchElementException if there is no registration
     *         for the specified classification.
     */
    private MathTransform.Registration getRegistration(String classification) throws NoSuchElementException
    {
        classification = classification.trim();
        for (int i=0; i<REGISTERED.length; i++)
            if (classification.equalsIgnoreCase(REGISTERED[i].classification))
                return REGISTERED[i];
        throw new NoSuchElementException(Resources.format(Clé.NO_TRANSFORM_FOR_CLASSIFICATION¤1, classification));
    }
}
