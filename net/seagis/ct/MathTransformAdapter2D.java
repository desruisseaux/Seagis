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
 */
package net.seagis.ct;

// OpenGIS dependencies
import org.opengis.ct.CT_MathTransform;

// OpenGIS dependencies (SEAGIS)
import net.seagis.ct.TransformException;
import net.seagis.ct.NoninvertibleTransformException;

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;
import net.seagis.resources.Geometry;


/**
 * Wrap an {@link CT_MathTransform} into a {@link MathTransform2D}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class MathTransformAdapter2D extends MathTransformAdapter implements MathTransform2D
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 2870952997133267365L;

    /**
     * Construct an adapter.
     *
     * @throws RemoteException if a remote call failed.
     */
    public MathTransformAdapter2D(final CT_MathTransform transform) throws RemoteException
    {
        super(transform);
        if (getDimSource()!=2 || getDimTarget()!=2)
        {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Transforms the specified <code>ptSrc</code> and stores the result in <code>ptDst</code>.
     */
    public Point2D transform(final Point2D ptSrc, final Point2D ptDst) throws TransformException
    {
/*----- BEGIN JDK 1.4 DEPENDENCIES ----
        assert getDimSource()==2 && getDimTarget()==2;
------- END OF JDK 1.4 DEPENDENCIES ---*/
        try
        {
            double[] array = transform.transformList(new double[] {ptSrc.getX(), ptSrc.getY()});
            if (ptDst!=null)
            {
                ptDst.setLocation(array[0], array[1]);
                return ptDst;
            }
            else return new Point2D.Double(array[0], array[1]);
        }
        catch (RemoteException exception)
        {
            throw new TransformException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Transform the specified shape.
     */
    public Shape createTransformedShape(final Shape shape) throws TransformException
    {return MathTransform2D.Abstract.createTransformedShape(shape, null, this, null, Geometry.PARALLEL);}
}