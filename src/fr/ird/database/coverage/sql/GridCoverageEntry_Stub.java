/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
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
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageEntry;


/**
 * Un <code>Stub</code> pour utilisation via les RMI. Par rapport aux stub classiques,
 * celui-ci cachent toutes les valeurs.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 *
 * @todo G�n�rer le code avec rmic et copier ici.
 */
public class GridCoverageEntry_Stub extends CoverageEntry.Proxy {
    /** Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures). */
//    private static final long serialVersionUID = -8988647973055903642L;

    /** La s�rie. */
    private transient SeriesEntry series;

    /** Le nom de l'image. */
    private transient String name;

    /** Remarques associ�es � l'image. */
    private transient String remarks;

    /** Le nom de fichier de l'image. */
    private transient File file;

    /** La g�om�trie de l'image. */
    private transient GridGeometry gridGeometry;

    /** Le syst�me de r�f�rence des coordonn�es de l'image. */
    private transient CoordinateSystem coordinateSystem;

    /** L'envelope de l'image. */
    private transient Envelope envelope;

    /** La plage de temps de l'image. */
    private transient Range timeRange;

    /** Les coordonn�es g�ographiques de l'image. */
    private transient Rectangle2D geographicArea;

    /** Les bandes de l'image. */
    private transient SampleDimension[] sampleDimensions;

    /** L'image. */
    private transient GridCoverage gridCoverage;
    
    /** Creates a new instance of GridCoverageEntry_Stub */
    public GridCoverageEntry_Stub(final CoverageEntry entry) {
        super(entry);
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public SeriesEntry getSeries() throws RemoteException {
        if (series == null) {
            series = super.getSeries();
        }
        return series;
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public String getName() throws RemoteException {
        if (name == null) {
            name = super.getName();
        }
        return name;
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public String getRemarks() throws RemoteException {
        if (remarks == null) {
            remarks = super.getRemarks();
        }
        return remarks;
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public File getFile() throws RemoteException {
        if (file == null) {
            file = super.getFile();
        }
        return file;
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public GridGeometry getGridGeometry() throws RemoteException {
        if (gridGeometry == null) {
            gridGeometry = super.getGridGeometry();
        }
        return gridGeometry;
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public CoordinateSystem getCoordinateSystem() throws RemoteException {
        if (coordinateSystem == null) {
            coordinateSystem = super.getCoordinateSystem();
        }
        return coordinateSystem;
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public Envelope getEnvelope() throws RemoteException {
        if (envelope == null) {
            envelope = super.getEnvelope();
        }
        return (Envelope) envelope.clone();
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public Range getTimeRange() throws RemoteException {
        if (timeRange == null) {
            timeRange = super.getTimeRange();
        }
        return timeRange;
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public Rectangle2D getGeographicArea() throws RemoteException {
        if (geographicArea == null) {
            geographicArea = super.getGeographicArea();
        }
        return (Rectangle2D) geographicArea.clone();
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public SampleDimension[] getSampleDimensions() throws RemoteException {
        if (sampleDimensions == null) {
            sampleDimensions = super.getSampleDimensions();
        }
        return (SampleDimension[]) sampleDimensions.clone();
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException {
        if (gridCoverage == null) {
            gridCoverage = super.getGridCoverage(listenerList);
        }
        return gridCoverage;
    }

    /**
     * Si cette m�thode a d�j� �t� appel�e, retourne la valeur conserv�e dans la cache.
     * Sinon, redirige l'appel vers {@link #entry} et conserve le r�sultat dans la cache
     * pour les appels subs�quents de cette m�thode.
     */
    public void abort() throws RemoteException {
        gridCoverage = null;
        super.abort();
    }
}
