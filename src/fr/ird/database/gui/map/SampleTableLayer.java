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
package fr.ird.database.gui.map;

// J2SE dependencies
import java.util.Date;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.sql.SQLException;

// JAI dependencies
import javax.media.jai.util.Range;

// Geotools dependencies
import org.geotools.resources.Utilities;
import org.geotools.ct.TransformException;
import org.geotools.cs.GeographicCoordinateSystem;

// Seagis dependencies
import fr.ird.database.sample.SampleEntry;
import fr.ird.database.sample.SampleTable;


/**
 * A catch layer backed by a {@link SampleTable}.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class SampleTableLayer extends SampleLayer {
    /**
     * Connection to the catch table.
     */
    private final SampleTable sampleTable;

    /**
     * Construct a new layer for the specified catch table. This constructor
     * query <code>sampleTable</code> for all catchs in its current time range.
     * <code>sampleTable</code> will be queried again each time the time range
     * is changed through {@link #setTimeRange}.
     *
     * @param  sampleTable Connection to a table containing catchs to display.
     * @throws SQLException If a SQL query failed.
     */
    public SampleTableLayer(final SampleTable sampleTable) throws SQLException {
        this.sampleTable = sampleTable;
        if (sampleTable != null) {
            try {
                setCoordinateSystem(sampleTable.getCoordinateSystem());
            } catch (TransformException exception) {
                // Should not happen, since we don't have any data yet.
                IllegalStateException e = new IllegalStateException(exception.getLocalizedMessage());
                e.initCause(exception);
                throw e;
            }
            setSamples(sampleTable.getEntries());
            updateTypicalAmplitude();
        }
    }

    /**
     * Construct a new layer with the same {@link SampleTable} and
     * the same icons than the specified layer.
     *
     * @param  layer Layer to take data and icons from.
     * @throws SQLException If a SQL query failed.
     */
    public SampleTableLayer(final SampleTableLayer layer) throws SQLException {
        super(layer, (layer.sampleTable!=null) ? layer.sampleTable.getCoordinateSystem()
                                               : GeographicCoordinateSystem.WGS84);
        this.sampleTable = layer.sampleTable;
    }

    /**
     * Query the underlying {@link SampleTable} for a new set of catchs to display.
     * This is a convenience method for {@link #setTimeRange(Date,Date)}.
     *
     * @param  timeRange the time range for catchs to display.
     * @throws SQLException If a SQL query failed.
     */
    public void setTimeRange(final Range timeRange) throws SQLException {
        setTimeRange((Date) timeRange.getMinValue(), (Date) timeRange.getMaxValue());
    }

    /**
     * Query the underlying {@link SampleTable} for a new set of catchs to display.
     * This method first invokes {@link FisheryTable#setTimeRange} with the specified
     * time range, and then query for all catchs in this time range.
     *
     * @param  startTime Time of the first catch to display.
     * @param  startTime Time of the end catch to display.
     * @throws SQLException If a SQL query failed.
     */
    public void setTimeRange(final Date startTime, final Date endTime) throws SQLException {
        if (sampleTable == null) {
            throw new SQLException("Aucune table des captures n'a été spécifiée.");
        }
        final Collection<SampleEntry> catchs;
        synchronized (sampleTable) {
            sampleTable.setTimeRange(startTime, endTime);
            catchs = sampleTable.getEntries();
        }
        setSamples(catchs);
    }
}
