/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le D�veloppement
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.ct;

// OpenGIS dependencies
import org.opengis.pt.PT_Matrix;
import org.opengis.ct.CT_Parameter;
import org.opengis.ct.CT_MathTransform;
import org.opengis.ct.CT_MathTransformFactory;

// OpenGIS (SEAS) dependencies
import net.seagis.pt.Matrix;
import net.seagis.cs.Ellipsoid;
import net.seagis.cs.Projection;

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

// Resources
import net.seagis.resources.WeakHashSet;
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


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
     * The default math transform factory. This factory
     * will be constructed only when first needed.
     */
    private static MathTransformFactory DEFAULT;

    /**
     * A pool of math transform. This pool is used in order to
     * returns instance of existing math transforms when possible.
     */
    static final WeakHashSet pool = new WeakHashSet();

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
     * Returns the default math transform factory.
     */
    public static synchronized MathTransformFactory getDefault()
    {
        if (DEFAULT==null)
        {
            DEFAULT = new MathTransformFactory(new MathTransformProvider[]
            {
                new           MercatorProjection.Provider(),
                new   LambertConformalProjection.Provider(),
                new      StereographicProjection.Provider(),      // Automatic
                new      StereographicProjection.Provider(true),  // Polar
                new      StereographicProjection.Provider(false), // Oblique
                new TransverseMercatorProjection.Provider(false), // Universal
                new TransverseMercatorProjection.Provider(true)   // Modified
            });
        }
        return DEFAULT;
    }

    /**
     * Creates an identity transform of the specified dimension.
     *
     * @param  dimension The source and target dimension.
     * @return The identity transform.
     */
    public MathTransform createIdentityTransform(final int dimension)
    {
        final Matrix matrix = new Matrix(dimension);
        for (int i=0; i<dimension; i++)
            matrix.set(i, i, 1.0);
        return createAffineTransform(matrix);
    }

    /**
     * Creates an affine transform from a matrix.
     *
     * @param matrix The matrix used to define the affine transform.
     * @return The affine transform.
     */
    public MathTransform2D createAffineTransform(final AffineTransform matrix)
    {return (MathTransform2D) pool.intern(new AffineTransform2D(matrix));}

    /**
     * Creates an affine transform from a matrix.
     *
     * @param  matrix The matrix used to define the affine transform.
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
        if (matrix.getSize()==3 && matrix.isAffine())
        {
            return createAffineTransform(matrix.toAffineTransform2D());
        }
        /*
         * General case (slower). May not be a real
         * affine transform. We accept it anyway...
         */
        return (MathTransform) pool.intern(new MatrixTransform(matrix));
    }

    /**
     * Returns the underlying matrix for the specified transform,
     * or <code>null</code> if the matrix is unavailable.
     */
    private static Matrix getMatrix(final MathTransform transform)
    {
        if (transform instanceof AffineTransform) return new Matrix((AffineTransform) transform);
        if (transform instanceof MatrixTransform) return ((MatrixTransform) transform).getMatrix();
        return null;
    }

    /**
     * Creates a transform by concatenating two existing transforms.
     * A concatenated transform acts in the same way as applying two
     * transforms, one after the other. The dimension of the output
     * space of the first transform must match the dimension of the
     * input space in the second transform. If you wish to concatenate
     * more than two transforms, then you can repeatedly use this method.
     *
     * @param  tr1 The first transform to apply to points.
     * @param  tr2 The second transform to apply to points.
     * @return The concatenated transform.
     *
     * @see org.opengis.ct.CT_MathTransformFactory#createConcatenatedTransform
     */
    public MathTransform createConcatenatedTransform(MathTransform tr1, MathTransform tr2)
    {
        if (tr1.isIdentity()) return tr2;
        if (tr2.isIdentity()) return tr1;
        /*
         * If both transforms use matrix, then we can create
         * a single transform using the concatened matrix.
         */
        final Matrix matrix1 = getMatrix(tr1);
        if (matrix1!=null)
        {
            final Matrix matrix2 = getMatrix(tr2);
            if (matrix2!=null)
            {
                // May not be really affine, but work anyway...
                // This call will detect and optimize the special
                // case where an 'AffineTransform' can be used.
                return createAffineTransform(matrix2.multiply(matrix1));
            }
        }
        /*
         * If one or both math transform are instance of {@link ConcatenedTransform},
         * then maybe it is possible to efficiently concatenate <code>tr1</code> or
         * <code>tr2</code> with one of step transforms. Try that...
         */
        if (tr1 instanceof ConcatenedTransform)
        {
            final ConcatenedTransform ctr = (ConcatenedTransform) tr1;
            tr1 = ctr.transform1;
            tr2 = createConcatenatedTransform(ctr.transform2, tr2);
        }
        if (tr2 instanceof ConcatenedTransform)
        {
            final ConcatenedTransform ctr = (ConcatenedTransform) tr2;
            tr1 = createConcatenatedTransform(tr1, ctr.transform1);
            tr2 = ctr.transform2;
        }
        /*
         * The returned transform will implements {@link MathTransform2D} if source and
         * target dimensions are equal to 2.  {@link MathTransform} implementations are
         * available in two version: direct and non-direct. The "non-direct" version use
         * an intermediate buffer when performing transformations;   they are slower and
         * consume more memory. They are used only as a fallback when a "direct" version
         * can't be created.
         */
        final MathTransform transform;
        final int dimSource = tr1.getDimSource();
        final int dimTarget = tr2.getDimTarget();
        if (dimSource==2 && dimTarget==2)
        {
            if (tr1 instanceof MathTransform2D && tr2 instanceof MathTransform2D)
            {
                transform = new ConcatenedTransformDirect2D(this, (MathTransform2D)tr1, (MathTransform2D)tr2);
            }
            else transform = new ConcatenedTransform2D(this, tr1, tr2);
        }
        else if (dimSource==tr1.getDimTarget() && tr2.getDimSource()==dimTarget)
        {
            transform = new ConcatenedTransformDirect(this, tr1, tr2);
        }
        else transform = new ConcatenedTransform(this, tr1, tr2);
        return (MathTransform) pool.intern(transform);
    }

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
    {return (MathTransform) pool.intern(getProvider(classification).create(parameters));}

    /**
     * Creates a transform which passes through a subset of ordinates to another transform.
     * This allows transforms to operate on a subset of ordinates. For example, if you have
     * (<var>latitidue</var>,<var>longitude</var>,<var>height</var>) coordinates, then you
     * may wish to convert the height values from feet to meters without affecting the
     * latitude and longitude values.
     *
     * @param  firstAffectedOrdinate Index of the first affected ordinate.
     * @param  transform The sub transform.
     * @param  numTrailingOrdinates Number of trailing ordinates to pass through.
     *         Affected ordinates will range from <code>firstAffectedOrdinate</code>
     *         inclusive to <code>dimTarget-numTrailingOrdinates</code> exclusive.
     * @return A pass through transform with the following dimensions:<br>
     *         <pre>
     * Source: firstAffectedOrdinate + subTransform.getDimSource() + numTrailingOrdinates
     * Target: firstAffectedOrdinate + subTransform.getDimTarget() + numTrailingOrdinates</pre>
     *
     * @see org.opengis.ct.CT_MathTransformFactory#createPassThroughTransform
     */
    public MathTransform createPassThroughTransform(final int firstAffectedOrdinate, final MathTransform subTransform, final int numTrailingOrdinates)
    {
        if (firstAffectedOrdinate < 0) throw new IllegalArgumentException(String.valueOf(firstAffectedOrdinate));
        if (numTrailingOrdinates  < 0) throw new IllegalArgumentException(String.valueOf(numTrailingOrdinates ));
        if (firstAffectedOrdinate==0 && numTrailingOrdinates==0)
        {
            return subTransform;
        }
        if (subTransform.isIdentity())
        {
            final int dimension = subTransform.getDimSource();
            if (dimension == subTransform.getDimTarget())
            {
                // The AffineTransform is easier to concatenate with other transforms.
                return createIdentityTransform(firstAffectedOrdinate + dimension + numTrailingOrdinates);
            }
        }
        return (MathTransform) pool.intern(new PassThroughTransform(firstAffectedOrdinate, subTransform, numTrailingOrdinates));
    }

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
        throw new NoSuchElementException(Resources.format(ResourceKeys.ERROR_NO_TRANSFORM_FOR_CLASSIFICATION_$1, classification));
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
        {return adapters.export(MathTransformFactory.this.createPassThroughTransform(firstAffectedOrdinate, adapters.wrap(subTransform), 0));}

        /**
         * Creates a transform from a classification name and parameters.
         */
        public CT_MathTransform createParameterizedTransform(final String classification, final CT_Parameter[] parameters) throws RemoteException
        {return adapters.export(MathTransformFactory.this.createParameterizedTransform(classification, adapters.wrap(parameters)));}

        /**
         * Creates a math transform from a Well-Known Text string.
         */
        public CT_MathTransform createFromWKT(final String wellKnownText) throws RemoteException
        {throw new UnsupportedOperationException("WKT parsing not yet implemented");}

        /**
         * Creates a math transform from XML.
         */
        public CT_MathTransform createFromXML(final String xml) throws RemoteException
        {throw new UnsupportedOperationException("XML parsing not yet implemented");}

        /**
         * Tests whether parameter is angular.
         */
        public boolean isParameterAngular(final String parameterName) throws RemoteException
        {throw new UnsupportedOperationException("Not yet implemented");}

        /**
         * Tests whether parameter is linear.
         */
        public boolean isParameterLinear(final String parameterName) throws RemoteException
        {throw new UnsupportedOperationException("Not yet implemented");}
    }
}