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

// J2SE dependencies
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;
import javax.media.jai.util.Range;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.sql.SQLException;
import java.rmi.RemoteException;

// Geotools
import org.geotools.gp.GridCoverageProcessor;

// Seagis
import fr.ird.database.CatalogException;
import fr.ird.database.ConfigurationKey;
import fr.ird.database.gui.swing.SQLEditor;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.sql.AbstractDataBase;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Connexion avec la base de donn�es d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class CoverageDataBase extends AbstractDataBase implements fr.ird.database.coverage.CoverageDataBase {
    /**
     * Liste des cl�s de configurations. Ne sera construit que la premi�re fois o�
     * elle sera n�cessaire (afin d'�viter le chargement d'un grand nombre de classes).
     */
    private static ConfigurationKey[] KEYS;

    /**
     * La g�om�trie de l'ensemble des images de la base de donn�es,
     * ou <code>null</code> si elle n'a pas encore �t� calcul�e.
     */
    private transient GeographicBoundingBoxTable boundingBox;

    /**
     * S�ries d'images � proposer par d�faut. On tentera de d�terminer cette s�rie
     * une fois pour toute la premi�re fois que {@link #getCoverageTable()} sera appel�e.
     */
    private transient SeriesEntry series;

    /**
     * Ouvre une connection avec une base de donn�es par d�faut. Le nom de la base de
     * donn�es ainsi que le pilote � utiliser seront puis�s dans le fichier de
     * configuration.
     *
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    public CoverageDataBase() throws IOException, SQLException {
        this(null, null, null, null);
    }

    /**
     * Ouvre une connection avec la base de donn�es des images.
     * Chaque argument nul sera remplac� par la valeur sp�cifi�e dans le fichier de configuration.
     *
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @param  url Protocole et nom de la base de donn�es d'images.
     * @param  user Nom d'utilisateur de la base de donn�es.
     * @param  password Mot de passe.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    public CoverageDataBase(final TimeZone timezone,
                            final String   source,
                            final String   user,
                            final String   password) throws IOException, SQLException
    {
        super(timezone, source, user, password);
    }

    /**
     * V�rifie que la r�gion est valide, en puisant les
     * donn�es dans la base de donn�es si n�cessaire.
     */
    private void ensureGeometryValid() throws RemoteException, SQLException {
        if (boundingBox == null) {
            boundingBox = new GeographicBoundingBoxTable(this, connection, timezone);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Rectangle2D getGeographicArea() throws RemoteException {
        try {
            ensureGeometryValid();
            return boundingBox.getGeographicArea();
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Range getTimeRange() throws RemoteException {
        try {
            ensureGeometryValid();
            return boundingBox.getTimeRange();
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }            
    }

    /**
     * {@inheritDoc}
     */
    public synchronized SeriesTable getSeriesTable() throws RemoteException {
        return new fr.ird.database.coverage.sql.SeriesTable(this, connection);
    }
 
    /**
     * Retourne la table des formats.
     */
    final synchronized FormatTable getFormatTable() throws RemoteException {
        return new FormatTable(this, connection);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized CoverageTable getCoverageTable()
            throws RemoteException
    {
        if (series == null) {
            double minPeriod = Double.POSITIVE_INFINITY;
            final SeriesTable table = getSeriesTable();
            for (final SeriesEntry entry : table.getEntries()) {
                final double period = entry.getPeriod();
                if (period < minPeriod) {
                    this.series = entry;
                    minPeriod   = period;
                }
            }
            table.close();
            if (series == null) {
                throw new IllegalRecordException(Table.SERIES,
                          Resources.format(ResourceKeys.ERROR_NO_SERIES));
            }
        }
        return getCoverageTable(series);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized CoverageTable getCoverageTable(final SeriesEntry series)
            throws RemoteException
    {
        Rectangle2D geographicArea = getGeographicArea();
        Range            timeRange = getTimeRange();
        Date startTime, endTime;
        if (geographicArea == null) {
            geographicArea = new Rectangle2D.Double(-180, -90, 180, 360);
        }
        if (timeRange != null) {
            startTime = (Date)timeRange.getMinValue();
            endTime   = (Date)timeRange.getMaxValue();
        } else {
            startTime = new Date(0);
            endTime   = new Date( );
        }
        final CoverageTable table;
        try {
            table = new WritableGridCoverageTable(this, connection, timezone);
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }            
        // Initial setup of the table. We set the series last in order to
        // avoid logging of "setGeographicArea" and "setTimeRange". Those
        // two methods do not log anything as long as the series in null.
        table.setGeographicArea(geographicArea);
        table.setTimeRange     (startTime, endTime);
        table.setSeries        (series); // Should bet last
        return table;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized CoverageTable getCoverageTable(final String series) throws RemoteException {
        final SeriesTable table = getSeriesTable();       
        final SeriesEntry entry = table.getEntry(series);
        table.close();
        if (entry != null) {
            return getCoverageTable(entry);
        } else {
            throw new CatalogException(Resources.format(ResourceKeys.ERROR_SERIES_NOT_FOUND_$1, series));
        }
    }

    /**
     * Retourne le processeur par d�faut � utiliser pour appliquer des op�rations sur les images
     * lues. Les operations sont sp�cifi�es par {@link CoverageTable#setOperation(String)} et
     * appliqu�e lors de l'appel de {@link CoverageEntry#getGridCoverage}. Le processeur par
     * d�faut accepte la combinaison d'un certain nombre d'op�rations s�par�es par des point
     * virgules, par exemple <code>"NodataFilter;GradientMagnitude"</code>.
     *
     * @return Le processeur par d�faut.
     * @see CoverageTable#setOperation(String)
     * @see CoverageEntry#getGridCoverage
     */
    public static GridCoverageProcessor getDefaultGridCoverageProcessor() {
        return GridCoverageEntry.PROCESSOR;
    }

    /**
     * D�finit le processeur par d�faut � utiliser pour appliquer des op�rations sur les images
     * lues. Les operations sont sp�cifi�es par {@link CoverageTable#setOperation(String)} et
     * appliqu�e lors de l'appel de {@link CoverageEntry#getGridCoverage}.
     *
     * @param processor Le processeur par d�faut.
     * @see CoverageTable#setOperation(String)
     * @see CoverageEntry#getGridCoverage
     */
    public static void setDefaultGridCoverageProcessor(final GridCoverageProcessor processor) {
        GridCoverageEntry.PROCESSOR = processor;
    }

    /**
     * Construit et retourne un panneau qui permet � l'utilisateur de modifier
     * les instructions SQL. Les instructions modifi�es seront conserv�es dans
     * les pr�f�rences syst�mes et utilis�es pour interroger les tables de la
     * base de donn�es d'images.
     */
    public synchronized SQLEditor getSQLEditor() {
        if (KEYS == null) {
            KEYS = new ConfigurationKey[] {      DRIVER,
                                                 SOURCE,
                                                 USER,
                                                 PASSWORD,
                                                 TIMEZONE,
                                                 ROOT_DIRECTORY,
                                                 ROOT_URL,
                      GeographicBoundingBoxTable.SELECT,
        fr.ird.database.coverage.sql.SeriesTable.SELECT,
        fr.ird.database.coverage.sql.SeriesTable.SELECT_SUBSERIES,
        fr.ird.database.coverage.sql.SeriesTable.SELECT_TREE,
                                     FormatTable.SELECT,
                            SampleDimensionTable.SELECT,
                                   CategoryTable.SELECT,
                               GridCoverageTable.SELECT,
                               GridCoverageTable.SELECT_ID,
                       WritableGridCoverageTable.SELECT_BBOX,
                       WritableGridCoverageTable.INSERT_BBOX,
                       WritableGridCoverageTable.INSERT_COVERAGE};
        }
        final Resources resources = Resources.getResources(null);
        final SQLEditor editor = new SQLEditor(this, resources.getString(
              ResourceKeys.EDIT_SQL_COVERAGES_OR_SAMPLES_$1, new Integer(0)), LOGGER);
        for (int i=0; i<KEYS.length; i++) {
            editor.addSQL(KEYS[i]);
        }
        return editor;
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws IOException {
        if (boundingBox != null) {
            boundingBox.close();
        }
        super.close();
    }
    
    /**
     * {@inheritDoc}
     */
    protected String getConfigurationName() {
        return "CoverageConfiguration";
    }
    
    /**
     * {@inheritDoc}
     */
    protected Logger getLogger() {
        return LOGGER;
    }
}
