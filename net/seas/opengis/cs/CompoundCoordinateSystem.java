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
package net.seas.opengis.cs;

// OpenGIS dependencies
import org.opengis.cs.CS_CoordinateSystem;
import org.opengis.cs.CS_CompoundCoordinateSystem;

// Units and coordinates
import javax.units.Unit;
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.pt.CoordinatePoint;

// Miscellaneous
import java.util.Map;
import net.seas.util.XClass;
import net.seas.resources.Resources;
import java.rmi.RemoteException;


/**
 * An aggregate of two coordinate systems.
 * One of these is usually a two dimensional coordinate system such as a
 * geographic or a projected coordinate system with a horizontal datum.
 * The other is one-dimensional coordinate system with a vertical datum.
 *
 * @version 1.00
 * @author OpenGIS (www.opengis.org)
 * @author Martin Desruisseaux
 *
 * @see org.opengis.cs.CS_CompoundCoordinateSystem
 */
public class CompoundCoordinateSystem extends CoordinateSystem
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 6105762862621220984L;

    /**
     * First sub-coordinate system.
     */
    private final CoordinateSystem head;

    /**
     * Second sub-coordinate system.
     */
    private final CoordinateSystem tail;

    /**
     * Creates a compound coordinate system.
     *
     * @param name Name to give new object.
     * @param head Coordinate system to use for earlier ordinates.
     * @param tail Coordinate system to use for later ordinates.
     *
     * @see org.opengis.cs.CS_CoordinateSystemFactory#createCompoundCoordinateSystem
     */
    public CompoundCoordinateSystem(final String name, final CoordinateSystem head, final CoordinateSystem tail)
    {
        super(name);
        this.head = head;
        this.tail = tail;
        ensureNonNull("head", head);
        ensureNonNull("tail", tail);
        checkAxis(null);
    }

    /**
     * Creates a compound coordinate system.
     *
     * @param properties The set of properties (see {@link Info}).
     * @param head Coordinate system to use for earlier ordinates.
     * @param tail Coordinate system to use for later ordinates.
     */
    CompoundCoordinateSystem(final Map<String,Object> properties, final CoordinateSystem head, final CoordinateSystem tail)
    {
        super(properties);
        this.head = head;
        this.tail = tail;
        // Accept null values.
    }

    /**
     * Returns the first sub-coordinate system.
     *
     * @see org.opengis.cs.CS_CompoundCoordinateSystem#getHeadCS()
     */
    public CoordinateSystem getHeadCS()
    {return head;}

    /**
     * Returns the second sub-coordinate system.
     *
     * @see org.opengis.cs.CS_CompoundCoordinateSystem#getTailCS()
     */
    public CoordinateSystem getTailCS()
    {return tail;}

    /**
     * Returns the dimension of the coordinate system.
     *
     * @see org.opengis.cs.CS_CompoundCoordinateSystem#getDimension()
     */
    public int getDimension()
    {return head.getDimension()+tail.getDimension();}

    /**
     * Gets axis details for dimension within coordinate system.
     * Each dimension in the coordinate system has a corresponding axis.
     *
     * @see org.opengis.cs.CS_CompoundCoordinateSystem#getAxis(int)
     */
    public AxisInfo getAxis(final int dimension)
    {
        if (dimension >= 0)
        {
            final int headDim = head.getDimension();
            if (dimension < headDim)
            {
                return head.getAxis(dimension);
            }
            final int dim = dimension-headDim;
            if (dim < tail.getDimension())
            {
                return tail.getAxis(dim);
            }
        }
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Gets units for dimension within coordinate system.
     * Each dimension in the coordinate system has corresponding units.
     *
     * @see org.opengis.cs.CS_CompoundCoordinateSystem#getUnits(int)
     */
    public Unit getUnits(final int dimension)
    {
        if (dimension >= 0)
        {
            final int headDim = head.getDimension();
            if (dimension < headDim)
            {
                return head.getUnits(dimension);
            }
            final int dim = dimension-headDim;
            if (dim < tail.getDimension())
            {
                return head.getUnits(dim);
            }
        }
        throw new IndexOutOfBoundsException(Resources.format(Clé.INDEX_OUT_OF_BOUNDS¤1, new Integer(dimension)));
    }

    /**
     * Gets default envelope of coordinate system.
     *
     * @see org.opengis.cs.CS_CompoundCoordinateSystem#getDefaultEnvelope()
     */
    public Envelope getDefaultEnvelope()
    {
        final Envelope    headEnv = head.getDefaultEnvelope();
        final Envelope    tailEnv = tail.getDefaultEnvelope();
        final int         headDim = headEnv.getDimension();
        final int         tailDim = tailEnv.getDimension();
        final CoordinatePoint min = new CoordinatePoint(headDim+tailDim);
        final CoordinatePoint max = new CoordinatePoint(headDim+tailDim);
        for (int i=0; i<headDim; i++)
        {
            min.ord[i] = headEnv.getMinimum(i);
            max.ord[i] = headEnv.getMaximum(i);
        }
        for (int i=0; i<tailDim; i++)
        {
            min.ord[headDim+i] = tailEnv.getMinimum(i);
            max.ord[headDim+i] = tailEnv.getMaximum(i);
        }
        return new Envelope(min,max);
    }

    /**
     * Returns  <code>true</code> if this coordinate system is equivalents to
     * the specified coordinate system. Two coordinate systems are considered
     * equivalent if the {@link net.seas.opengis.ct.CoordinateTransform} from
     * <code>this</code> to  <code>cs</code>  would be the identity transform.
     *
     * @param  cs The coordinate system (may be <code>null</code>).
     * @return <code>true</code> if both coordinate systems are equivalent.
     */
    public boolean equivalents(final CoordinateSystem cs)
    {
        if (cs==this) return true;
        if (super.equivalents(cs))
        {
            final CompoundCoordinateSystem that = (CompoundCoordinateSystem) cs;
            return head.equivalents(that.head) &&
                   tail.equivalents(that.tail);
        }
        return false;
    }

    /**
     * Compares the specified object with
     * this coordinate system for equality.
     */
    public boolean equals(final Object object)
    {
        if (object==this) return true;
        if (super.equals(object))
        {
            final CompoundCoordinateSystem that = (CompoundCoordinateSystem) object;
            return XClass.equals(this.head, that.head) &&
                   XClass.equals(this.tail, that.tail);
        }
        return false;
    }

    /**
     * Returns an OpenGIS interface for this compound coordinate
     * system. The returned object is suitable for RMI use.
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
     * Wrap a {@link CompoundCoordinateSystem} object for use with OpenGIS.
     * This class is suitable for RMI use.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Export extends CoordinateSystem.Export implements CS_CompoundCoordinateSystem
    {
        /**
         * Construct a remote object.
         */
        protected Export(final Object adapters)
        {super(adapters);}

        /**
         * Gets first sub-coordinate system.
         */
        public CS_CoordinateSystem getHeadCS() throws RemoteException
        {return adapters.export(CompoundCoordinateSystem.this.getHeadCS());}

        /**
         * Gets second sub-coordinate system.
         */
        public CS_CoordinateSystem getTailCS() throws RemoteException
        {return adapters.export(CompoundCoordinateSystem.this.getTailCS());}
    }
}
