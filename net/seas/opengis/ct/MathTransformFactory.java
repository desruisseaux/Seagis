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

// OpenGIS dependencies
import org.opengis.pt.PT_Matrix;
import org.opengis.ct.CT_Parameter;
import org.opengis.ct.CT_MathTransform;
import org.opengis.ct.CT_MathTransformFactory;

// OpenGIS (SEAS) dependencies
import net.seas.opengis.pt.Matrix;
import net.seas.opengis.cs.Projection;

// Miscellaneous
import javax.units.Unit;
import java.util.Locale;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;
import javax.media.jai.ParameterList;
import java.util.NoSuchElementException;

// Remote Method Invocation
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

// Miscellaneous
import net.seas.util.XArray;
import net.seas.resources.Resources;
import net.seas.opengis.cs.Ellipsoid;


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
     * The default math transform factory.
     */
    public static final MathTransformFactory DEFAULT = new MathTransformFactory(new MathTransformProvider[]
    {
        new           MercatorProjection.Provider(),
        new   LambertConformalProjection.Provider(),
        new      StereographicProjection.Provider(),      // Automatic
        new      StereographicProjection.Provider(true),  // Polar
        new      StereographicProjection.Provider(false), // Oblique
        new TransverseMercatorProjection.Provider(false), // Universal
        new TransverseMercatorProjection.Provider(true)   // Modified
    });

    /**
     * List of registered math transforms.
     */
    private final MathTransformProvider[] providers;

    /**
     * Construct a factory using the specified providers.
     */
    public MathTransformFactory(final MathTransformProvider[] providers)
    {this.providers = (MathTransformProvider[]) providers.clone();}

    /**
     * Creates an affine transform from a matrix.
     *
     * @param matrix The matrix used to define the affine transform.
     * @return The affine transform.
     */
    public MathTransform createAffineTransform(final AffineTransform matrix)
    {return new AffineTransform2D(matrix);}

    /**
     * Creates an affine transform from a matrix.
     *
     * @param matrix The matrix used to define the affine transform.
     * @return The affine transform.
     *
     * @see org.opengis.ct.CT_MathTransformFactory#createAffineTransform
     */
    public MathTransform createAffineTransform(final Matrix matrix)
    {
        /*
         * If the user is requesting a 2D transform, delegate to the
         * highly optimized java.awt.geom.AffineTransform class.
         */
        if (matrix.getSize()==3)
        {
            return createAffineTransform(matrix.toAffineTransform2D());
        }
        /*
         * General case (slower).
         */
        return new MatrixTransform(matrix);
    }

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
     *
     * @see org.opengis.ct.CT_MathTransformFactory#createConcatenatedTransform
     */
    public MathTransform createConcatenatedTransform(final MathTransform transform1, final MathTransform transform2)
    {return transform1.concatenate(transform2);}

    /**
     * Creates a transform from a classification name and parameters.
     * The client must ensure that all the linear parameters are expressed
     * in meters, and all the angular parameters are expressed in degrees.
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
     *
     * @see org.opengis.ct.CT_MathTransformFactory#createParameterizedTransform
     */
    public MathTransform createParameterizedTransform(final String classification, final ParameterList parameters) throws NoSuchElementException, MissingParameterException
    {return getProvider(classification).create(parameters);}

    /**
     * Convenience method for creating a transform from a projection.
     *
     * @param  projection The projection.
     * @return The parameterized transform.
     * @throws NoSuchElementException if there is no transform for the specified projection.
     * @throws MissingParameterException if a parameter was required but not found.
     */
    public MathTransform createParameterizedTransform(final Projection projection) throws NoSuchElementException, MissingParameterException
    {return createParameterizedTransform(projection.getClassName(), projection.getParameters());}

    /**
     * Returns the classification names of every available transforms.
     * The returned array may have a zero length, but will never be null.
     */
    public String[] getAvailableTransforms()
    {
        final String[] names = new String[providers.length];
        for (int i=0; i<names.length; i++)
        {
            names[i] = providers[i].getClassName();
        }
        return names;
    }

    /**
     * Returns a human readable name localized for the specified locale.
     * If no name is available for the specified locale, this method may returns a name in an
     * arbitrary locale.
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
    {return getProvider(classification).getName(locale);}

    /**
     * Get the parameter list from a classification name.
     * The client may change any of those parameters and submit them to
     * {@link #createParameterizedTransform(String,ParameterList)}.
     *
     * @param  classification The classification name of the transform
     *         (e.g. "Transverse_Mercator"). Leading and trailing spaces
     *         are ignored, and comparaison is case-insensitive.
     * @return Default parameters for a transform of the specified classification.
     * @throws NoSuchElementException if there is no transform for the
     *         specified classification.
     */
    public ParameterList getParameterList(final String classification) throws NoSuchElementException
    {return getProvider(classification).getParameterList();}

    /**
     * Returns the provider for the specified classification.
     *
     * @param  classification The classification name of the transform
     *         (e.g. "Transverse_Mercator"). Leading and trailing spaces
     *         are ignored, and comparaison is case-insensitive.
     * @return The provider.
     * @throws NoSuchElementException if there is no registration
     *         for the specified classification.
     */
    private MathTransformProvider getProvider(String classification) throws NoSuchElementException
    {
        classification = classification.trim();
        for (int i=0; i<providers.length; i++)
            if (classification.equalsIgnoreCase(providers[i].getClassName().trim()))
                return providers[i];
        throw new NoSuchElementException(Resources.format(Clé.NO_TRANSFORM_FOR_CLASSIFICATION¤1, classification));
    }

    /**
     * Returns an OpenGIS interface for this transform factory.
     * The returned object is suitable for RMI use.
     *
     * Note: The returned type is a generic {@link Object} in order
     *       to avoid too early class loading of OpenGIS interface.
     */
    final Object toOpenGIS(final Object adapters)
    {return new Export(adapters);}




    /////////////////////////////////////////////////////////////////////////
    ////////////////                                         ////////////////
    ////////////////             OPENGIS ADAPTER             ////////////////
    ////////////////                                         ////////////////
    /////////////////////////////////////////////////////////////////////////

    /**
     * Wrap a {@link MathTransformFactory} for use with OpenGIS. This wrapper is a good
     * place to check for non-implemented OpenGIS methods (just check for methods throwing
     * {@link UnsupportedOperationException}). This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends RemoteObject implements CT_MathTransformFactory
    {
        /**
         * The originating adapter.
         */
        protected final Adapters adapters;

        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {this.adapters = (Adapters)adapters;}

        /**
         * Creates an affine transform from a matrix.
         */
        public CT_MathTransform createAffineTransform(final PT_Matrix matrix) throws RemoteException
        {return adapters.export(MathTransformFactory.this.createAffineTransform(adapters.PT.wrap(matrix)));}

        /**
         * Creates a transform by concatenating two existing transforms.
         */
        public CT_MathTransform createConcatenatedTransform(final CT_MathTransform transform1, final CT_MathTransform transform2) throws RemoteException
        {return adapters.export(MathTransformFactory.this.createConcatenatedTransform(adapters.wrap(transform1), adapters.wrap(transform2)));}

        /**
         * Creates a transform which passes through a subset of ordinates to another transform.
         */
        public CT_MathTransform createPassThroughTransform(final int firstAffectedOrdinate, final CT_MathTransform subTransform) throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Creates a transform from a classification name and parameters.
         */
        public CT_MathTransform createParameterizedTransform(final String classification, final CT_Parameter[] parameters) throws RemoteException
        {return adapters.export(MathTransformFactory.this.createParameterizedTransform(classification, adapters.wrap(parameters)));}

        /**
         * Creates a math transform from a Well-Known Text string.
         */
        public CT_MathTransform createFromWKT(final String wellKnownText) throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Creates a math transform from XML.
         */
        public CT_MathTransform createFromXML(final String xml) throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Tests whether parameter is angular.
         */
        public boolean isParameterAngular(final String parameterName) throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}

        /**
         * Tests whether parameter is linear.
         */
        public boolean isParameterLinear(final String parameterName) throws RemoteException
        {throw new UnsupportedOperationException("Not implemented");}
    }
}
