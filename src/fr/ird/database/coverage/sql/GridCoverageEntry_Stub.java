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
 */
package fr.ird.database.coverage.sql;

// J2SE et JAI
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteRef;
import java.rmi.server.RemoteStub;
import java.rmi.UnexpectedException;
import java.lang.reflect.Method;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;
import javax.swing.event.EventListenerList;

// Geotools
import org.geotools.pt.Envelope;
import org.geotools.gc.GridCoverage;
import org.geotools.gc.GridGeometry;
import org.geotools.cv.SampleDimension;
import org.geotools.cs.CoordinateSystem;

// Seagis dependencies
import fr.ird.database.Entry;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageEntry;


/**
 * Un <code>Stub</code> pour utilisation via les RMI. Par rapport aux stub classiques,
 * celui-ci cachent toutes les valeurs. Une autre différence importante est que pour
 * un stub, {@link #getFile} retourne toujours <code>null</code> par design.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class GridCoverageEntry_Stub extends RemoteStub implements CoverageEntry {
    /** Numéro de série (pour compatibilité avec des versions antérieures). */
    private static final long serialVersionUID = -8988647973055903642L;

    private static final Method abort;
    private static final Method getCoordinateSystem;
    private static final Method getEnvelope;
    private static final Method getGeographicArea;
    private static final Method getGridCoverage;
    private static final Method getGridGeometry;
    private static final Method getName;
    private static final Method getRemarks;
    private static final Method getSampleDimensions;
    private static final Method getSeries;
    private static final Method getTimeRange;
    private static final Method getURL;
    static {
        try {
            abort               = CoverageEntry.class.getMethod("abort",               (Class[]) null);
            getCoordinateSystem = CoverageEntry.class.getMethod("getCoordinateSystem", (Class[]) null);
            getEnvelope         = CoverageEntry.class.getMethod("getEnvelope",         (Class[]) null);
            getGeographicArea   = CoverageEntry.class.getMethod("getGeographicArea",   (Class[]) null);
            getGridCoverage     = CoverageEntry.class.getMethod("getGridCoverage",     EventListenerList.class);
            getGridGeometry     = CoverageEntry.class.getMethod("getGridGeometry",     (Class[]) null);
            getName             =         Entry.class.getMethod("getName",             (Class[]) null);
            getRemarks          =         Entry.class.getMethod("getRemarks",          (Class[]) null);
            getSampleDimensions = CoverageEntry.class.getMethod("getSampleDimensions", (Class[]) null);
            getSeries           = CoverageEntry.class.getMethod("getSeries",           (Class[]) null);
            getTimeRange        = CoverageEntry.class.getMethod("getTimeRange",        (Class[]) null);
            getURL              = CoverageEntry.class.getMethod("getURL",              (Class[]) null);
        } catch (NoSuchMethodException cause) {
            NoSuchMethodError e = new NoSuchMethodError("stub class initialization failed");
            e.initCause(cause);
            throw e;
        }
    }

    /** La série. */
    private transient SeriesEntry series;

    /** Le nom de l'image. */
    private transient String name;

    /** Remarques associées à l'image. */
    private transient String remarks;

    /** L'URL de l'image. */
    private transient URL url;

    /** La géométrie de l'image. */
    private transient GridGeometry gridGeometry;

    /** Le système de référence des coordonnées de l'image. */
    private transient CoordinateSystem coordinateSystem;

    /** L'envelope de l'image. */
    private transient Envelope envelope;

    /** La plage de temps de l'image. */
    private transient Range timeRange;

    /** Les coordonnées géographiques de l'image. */
    private transient Rectangle2D geographicArea;

    /** Les bandes de l'image. */
    private transient SampleDimension[] sampleDimensions;

    /** L'image. */
    private transient GridCoverage gridCoverage;
    
    /**
     * Creates a new instance of <code>GridCoverageEntry_Stub</code>.
     */
    public GridCoverageEntry_Stub(final RemoteRef ref) {
        super(ref);
    }

    /**
     * {@inheritDoc}
     */
    public SeriesEntry getSeries() throws RemoteException {
        if (series == null) try {
            series = (SeriesEntry)
                ref.invoke(this, getSeries, null, 3268385987975562315L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return series;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() throws RemoteException {
        if (name == null) try {
            name = (String)
                ref.invoke(this, getName, null, 6317137956467216454L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public String getRemarks() throws RemoteException {
        if (remarks == null) try {
            remarks = (String)
                ref.invoke(this, getRemarks, null, -9027161330452182081L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return remarks;
    }

    /**
     * Always returns <code>null</code>, since a file is not accessible remotely.
     * It should force the user to ask for {@link #getURL} instead.
     */
    public File getFile() throws RemoteException {
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public URL getURL() throws RemoteException {
        if (url == null) try {
            url = (URL)
                ref.invoke(this, getURL, null, -24390314744332875L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return url;
    }    

    /**
     * {@inheritDoc}
     */
    public GridGeometry getGridGeometry() throws RemoteException {
        if (gridGeometry == null) try {
            gridGeometry = (GridGeometry)
                ref.invoke(this, getGridGeometry, null, -2252777953050252017L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return gridGeometry;
    }

    /**
     * {@inheritDoc}
     */
    public CoordinateSystem getCoordinateSystem() throws RemoteException {
        if (coordinateSystem == null) try {
            coordinateSystem = (CoordinateSystem)
                ref.invoke(this, getCoordinateSystem, null, 5331755492035958287L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return coordinateSystem;
    }

    /**
     * {@inheritDoc}
     */
    public Envelope getEnvelope() throws RemoteException {
        if (envelope == null) try {
            envelope = (Envelope)
                ref.invoke(this, getEnvelope, null, -8737979946620619377L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return (Envelope) envelope.clone();
    }

    /**
     * {@inheritDoc}
     */
    public Range getTimeRange() throws RemoteException {
        if (timeRange == null) try {
            timeRange = (Range)
                ref.invoke(this, getTimeRange, null, 4602105287821927103L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return timeRange;
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle2D getGeographicArea() throws RemoteException {
        if (geographicArea == null) try {
            geographicArea = (Rectangle2D)
                ref.invoke(this, getGeographicArea, null, -2094094125586382812L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return (Rectangle2D) geographicArea.clone();
    }

    /**
     * {@inheritDoc}
     */
    public SampleDimension[] getSampleDimensions() throws RemoteException {
        if (sampleDimensions == null) try {
            sampleDimensions = (SampleDimension[])
                ref.invoke(this, getSampleDimensions, null, -34470738654264305L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return (SampleDimension[]) sampleDimensions.clone();
    }

    /**
     * {@inheritDoc}
     */
    public GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException {
        if (gridCoverage == null) try {
            gridCoverage = (GridCoverage)
                ref.invoke(this, getGridCoverage, new Object[]{listenerList}, 8747006095648054128L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
        return gridCoverage;
    }

    /**
     * {@inheritDoc}
     */
    public void abort() throws RemoteException {
        gridCoverage = null;
        try {
            ref.invoke(this, abort, null, 3595063641135311172L);
        } catch (RuntimeException e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("undeclared checked exception", e);
        }
    }
}
