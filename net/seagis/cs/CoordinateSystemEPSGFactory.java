/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2002, Institut de Recherche pour le Développement
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package net.seagis.cs;

// Database connection
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Blob;

// Logging
//----- BEGIN JDK 1.4 DEPENDENCIES ----
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
//----- END OF JDK 1.4 DEPENDENCIES ---

// Miscellaneous
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import javax.units.Unit;

// Resources
import net.seagis.resources.css.Resources;
import net.seagis.resources.css.ResourceKeys;


/**
 * Default implementation for a coordinate system factory backed
 * by the EPSG database. The EPSG database is freely available at
 * <A HREF="http://www.epsg.org">http://www.epsg.org</a>. Current
 * version of this class requires EPSG database version 6.1.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 * @author Yann Cézard
 */
public class CoordinateSystemEPSGFactory extends CoordinateSystemAuthorityFactory
{
    /**
     * The default coordinate system authority factory.
     * Will be constructed only when first requested.
     */
    private static CoordinateSystemAuthorityFactory DEFAULT;

    /**
     * Unit of arc-second.
     */
    private static final Unit ARC_SECOND = Unit.DEGREE.scale(1.0/3600);

    /**
     * The connection to the EPSG database.
     */
    protected final Connection connection;

    /**
     * Construct an authority factory using
     * the specified connection.
     *
     * @param factory    The underlying factory used for objects creation.
     * @param connection The connection to the underlying EPSG database.
     */
    public CoordinateSystemEPSGFactory(final CoordinateSystemFactory factory, final Connection connection)
    {
        super(factory);
        this.connection = connection;
        Info.ensureNonNull("connection", connection);
    }

    /**
     * Construct an authority factory using
     * the specified URL to an EPSG database.
     *
     * @param  factory The underlying factory used for objects creation.
     * @param  url     The url to the EPSG database. For example, a connection
     *                 using the ODBC-JDBC bridge may have an URL likes
     *                 <code>"jdbc:odbc:EPSG"</code>.
     * @param  driver  An optional driver to load, or <code>null</code> if none.
     *                 This is a convenience argument for the following pseudo-code:
     *                 <blockquote><code>
     *                 Class.forName(driver).newInstance();
     *                 </code></blockquote>
     *                 A message is logged to <code>"net.seagis.css"</code> whatever
     *                 the loading succed of fail. For JDBC-ODBC bridge, a typical value
     *                 for this argument is <code>"sun.jdbc.odbc.JdbcOdbcDriver"</code>.
     *                 This argument needs to be non-null only once for a specific driver.
     *
     * @throws SQLException if the constructor failed to connect to the
     *         EPSG database.
     */
    public CoordinateSystemEPSGFactory(final CoordinateSystemFactory factory,
                                       final String url, final String driver) throws SQLException
    {
        this(factory, getConnection(url, driver));
    }

    /**
     * Get the connection to an URL.
     *
     * @param  url     The url to the EPSG database.
     * @param  driver  The driver to load, or <code>null</code> if none.
     * @return The connection to the EPSG database.
     * @throws SQLException if the connection can't be etablished.
     */
    private static Connection getConnection(final String url, final String driver) throws SQLException
    {
        Info.ensureNonNull("url", url);
        if (driver!=null)
        {
//----- BEGIN JDK 1.4 DEPENDENCIES ----
            LogRecord record;
            try
            {
                final Driver drv = (Driver)Class.forName(driver).newInstance();
                record = Resources.getResources(null).getLogRecord(Level.CONFIG, ResourceKeys.LOADED_JDBC_DRIVER_$3);
                record.setParameters(new Object[]
                {
                    drv.getClass().getName(),
                    new Integer(drv.getMajorVersion()),
                    new Integer(drv.getMinorVersion())
                });
            }
            catch (Exception exception)
            {
                record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
                record.setThrown(exception);
                // Try to connect anyway. It is possible that
                // an other driver has already been loaded...
            }
            record.setSourceClassName("CoordinateSystemEPSGFactory");
            record.setSourceMethodName("<init>");
            Logger.getLogger("net.seagis.css").log(record);
/*----- END OF JDK 1.4 DEPENDENCIES ----
            try
            {
                Class.forName(driver).newInstance();
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }
//------- END OF JDK 1.3 FALLBACK --------*/
        }
        return DriverManager.getConnection(url);
    }

    /**
     * Returns a default coordinate system
     * factory backed by the EPSG database.
     *
     * @return The default factory.
     * @throws SQLException if the connection to the database can't
     *         be etablished.
     */
    public static synchronized CoordinateSystemAuthorityFactory getDefault() throws SQLException
    {
        if (DEFAULT==null)
        {
            final String url    = "jdbc:odbc:EPSG";
            final String driver = "sun.jdbc.odbc.JdbcOdbcDriver";
            // TODO: In a future version, we should fetch the
            //       above properties from system's preferences.
            DEFAULT = new CoordinateSystemEPSGFactory(CoordinateSystemFactory.getDefault(), url, driver);
            // TODO: What should we do with CoordinateSystemEPSGFactory.close()?
            //       We want to close the connection on exit, but we don't want
            //       to allow the user to close the connection by himself since
            //       this object may be shared by many users.
        }
        return DEFAULT;
    }

    /**
     * Returns the authority name, which is <code>"EPSG"</code>.
     */
    public String getAuthority()
    {return "EPSG";}

    /**
     * Gets the string from the specified {@link ResultSet}.
     * The string is required to be non-null. A null string
     * will throw an exception.
     *
     * @param  result The result set to fetch value from.
     * @param  columnIndex The column index (1-based).
     * @return The string at the specified column.
     * @throws SQLException if a SQL error occured,
     *         or if a null value was found.
     */
    private static String getString(final ResultSet result, final int columnIndex) throws SQLException
    {
        final String str = result.getString(columnIndex);
        if (result.wasNull())
        {
            throw new SQLException(Resources.format(ResourceKeys.ERROR_NULL_VALUE_$1,
                                   result.getMetaData().getColumnName(columnIndex)));
        }
        return str;
    }

    /**
     * Gets the value from the specified {@link ResultSet}.
     * The value is required to be non-null. A null value
     * (i.e. blank) will throw an exception.
     *
     * @param  result The result set to fetch value from.
     * @param  columnIndex The column index (1-based).
     * @return The double at the specified column.
     * @throws SQLException if a SQL error occured,
     *         or if a null value was found.
     */
    private static double getDouble(final ResultSet result, final int columnIndex) throws SQLException
    {
        final double value = result.getDouble(columnIndex);
        if (result.wasNull())
        {
            throw new SQLException(Resources.format(ResourceKeys.ERROR_NULL_VALUE_$1,
                                   result.getMetaData().getColumnName(columnIndex)));
        }
        return value;
    }

    /**
     * Gets the value from the specified {@link ResultSet}.
     * The value is required to be non-null. A null value
     * (i.e. blank) will throw an exception.
     *
     * @param  result The result set to fetch value from.
     * @param  columnIndex The column index (1-based).
     * @return The integer at the specified column.
     * @throws SQLException if a SQL error occured,
     *         or if a null value was found.
     */
    private static int getInt(final ResultSet result, final int columnIndex) throws SQLException
    {
        final int value = result.getInt(columnIndex);
        if (result.wasNull())
        {
            throw new SQLException(Resources.format(ResourceKeys.ERROR_NULL_VALUE_$1,
                                   result.getMetaData().getColumnName(columnIndex)));
        }
        return value;
    }

    /**
     * Make sure that an object construct from the database
     * is not duplicated.
     *
     * @param  newValue The newly constructed object.
     * @param  oldValue The object previously constructed,
     *         or <code>null</code> if none.
     * @param  The EPSG code (for formatting error message).
     * @throws BackingStoreException if a duplication has been detected.
     */
    private Object ensureSingleton(final Object newValue, final Object oldValue, final String code) throws BackingStoreException
    {
        if (oldValue == null)
        {
            return newValue;
        }
        if (oldValue.equals(newValue))
        {
            return oldValue;
        }
        throw new BackingStoreException(Resources.format(ResourceKeys.ERROR_DUPLICATED_VALUES_$1, code));
    }

    /**
     * Returns an {@link Ellipsoid} object from a code.
     *
     * @param  code The EPSG value.
     * @return The ellipsoid object.
     * @throws NoSuchAuthorityCodeException if this method can't find the requested code.
     * @throws BackingStoreException if some other kind of failure occured in the backing
     *         store. This exception usually have {@link SQLException} as its cause.
     */
    public Ellipsoid createEllipsoid(final String code) throws BackingStoreException
    {
        Ellipsoid returnValue = null;
        try
        {
            final PreparedStatement stmt;
            stmt = connection.prepareStatement("select ELLIPSOID_NAME,"
                                               + " SEMI_MAJOR_AXIS,"
                                               + " INV_FLATTENING,"
                                               + " SEMI_MINOR_AXIS,"
                                               + " UOM_CODE"
                                               + " from Ellipsoid"
                                               + " where ELLIPSOID_CODE = ?");
            stmt.setString(1, code);
            final ResultSet result = stmt.executeQuery();
            /*
             * If the supplied code exists in the database, then we
             * should find only one record.   However, we will do a
             * paranoiac check and verify if there is more records.
             */
            while (result.next())
            {
                /*
                 * One of 'semiMinorAxis' and 'inverseFlattening' values can be NULL in
                 * the database. Consequently, we don't use 'getString(ResultSet, int)'
                 * because we don't want to thrown an exception if a NULL value is found.
                 */
                final String name              = getString(result, 1);
                final double semiMajorAxis     = getDouble(result, 2);
                final double inverseFlattening = result.getDouble( 3);
                final double semiMinorAxis     = result.getDouble( 4);
                final String unitCode          = getString(result, 5);
                final Unit   unit              = createUnit(unitCode);
                final Ellipsoid ellipsoid;
                if (inverseFlattening == 0)
                {
                    if (semiMinorAxis == 0)
                    {
                        // Both are null, which is not allowed.
                        result.close();
                        throw new BackingStoreException(Resources.format(ResourceKeys.ERROR_NULL_VALUE_$1,
                                                        result.getMetaData().getColumnName(3)));
                    }
                    else
                    {
                        // We only have semiMinorAxis defined -> it's OK
/* TEMPORARY */         System.err.println("Ellipsoid : Using method createEllipsoid.");
                        ellipsoid = factory.createEllipsoid(name, semiMajorAxis, semiMinorAxis, unit);
                    }
                }
                else
                {
                    if (semiMinorAxis != 0)
                    {
                        // Both 'inverseFlattening' and 'semiMinorAxis' are defined.
                        // Log a warning and create the ellipsoid using the inverse flattening.
//----- BEGIN JDK 1.4 DEPENDENCIES ----
                        Logger.getLogger("net.seagis.css").warning(Resources.format(ResourceKeys.WARNING_AMBIGUOUS_ELLIPSOID));
//----- END OF JDK 1.4 DEPENDENCIES ---
                    }
/* TEMPORARY */     System.err.println("Ellipsoid : Using method createFlattenedSphere.");
                    ellipsoid = factory.createFlattenedSphere(name, semiMajorAxis, inverseFlattening, unit);
                }
                /*
                 * Now that we have built an ellipsoid, compare
                 * it with the previous one (if any).
                 */
                returnValue = (Ellipsoid) ensureSingleton(ellipsoid, returnValue, code);
            }
            result.close();
            stmt.close();
        }
        catch (SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        if (returnValue==null)
        {
             throw new NoSuchAuthorityCodeException(code);
        }
        return returnValue;
    }

    /**
     * Returns a {@link Unit} object from a code.
     *
     * @param  code Value allocated by authority.
     * @return The unit object.
     * @throws NoSuchAuthorityCodeException if this method can't find the requested code.
     * @throws BackingStoreException if some other kind of failure occured in the backing
     *         store. This exception usually have {@link SQLException} as its cause.
     */
    public Unit createUnit(final String code) throws BackingStoreException
    {
        Unit returnValue = null;
        try
        {
            final PreparedStatement stmt;
            stmt = connection.prepareStatement("select UNIT_OF_MEAS_TYPE,"
                                               + " FACTOR_B,"
                                               + " FACTOR_C"
                                               + " from Unit_of_Measure"
                                               + " where UOM_CODE = ?");
            stmt.setString(1, code);
            final ResultSet result = stmt.executeQuery();
            /*
             * If the supplied code exists in the database, then we
             * should find only one record.   However, we will do a
             * paranoiac check and verify if there is more records.
             */
            while (result.next())
            {
                final String type = getString(result, 1).trim();
                final double b    = result.getDouble( 2);
                final double c    = result.getDouble( 3);
                /*
                 * Factor b and c should not be 0. If they are,
                 * we will consider them as if they was null.
                 */
                if (b==0 && c==0)
                {
                    // TODO : unités qui utilisent des formules plus complexes.
                    //        Exemple: DDMMSS (ou DD=degrés, MM=minutes, SS=secondes).
                    result.close();
                    throw new UnsupportedOperationException("Non standard unit not yet implemented");
                }
                if (b==0 || c==0)
                {
                    result.close();
                    throw new BackingStoreException(Resources.format(ResourceKeys.ERROR_NULL_VALUE_$1,
                                                    result.getMetaData().getColumnName(b==0 ? 2 : 3)));
                }
                Unit unit;
                if (type.equalsIgnoreCase("length"))
                {
                    // Dans la table UOM de la base EPSG, toutes les longueurs sont basées sur le metre
                    unit = Unit.METRE;
                }
                else if (type.equalsIgnoreCase("angle"))
                {
                    // dans la table UOM de la base EPSG, toutes les longueurs sont basées sur le radian
                    unit = Unit.RADIAN;
                }
                else if (type.equalsIgnoreCase("scale"))
                {
                    // TODO : TYPE scale ???? !!!!
                    result.close();
                    throw new UnsupportedOperationException("Type scale not yet implemented");
                }
                else
                {
                    result.close();
                    throw new BackingStoreException(Resources.format(ResourceKeys.ERROR_UNKNOW_TYPE_$1, type));
                }
                /*
                 * Now that we have built an unit, scale it and
                 * compare it with the previous one (if any).
                 */
                unit = unit.scale(b/c);
                returnValue = (Unit) ensureSingleton(unit, returnValue, code);
            }
            result.close();
            stmt.close();
        }
        catch (SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        if (returnValue==null)
        {
             throw new NoSuchAuthorityCodeException(code);
        }
        return returnValue;
    }

    /**
     * Returns a prime meridian, relative to Greenwich.
     *
     * @param  code Value allocated by authority.
     * @return The prime meridian object.
     * @throws NoSuchAuthorityCodeException if this method can't find the requested code.
     * @throws BackingStoreException if some other kind of failure occured in the backing
     *         store. This exception usually have {@link SQLException} as its cause.
     */
    public PrimeMeridian createPrimeMeridian(final String code) throws BackingStoreException
    {
        PrimeMeridian returnValue = null;
        try
        {
            final PreparedStatement stmt;
            stmt = connection.prepareStatement("select PRIME_MERIDIAN_NAME,"
                                               + " GREENWICH_LONGITUDE,"
                                               + " UOM_CODE"
                                               + " from Prime_Meridian"
                                               + " where PRIME_MERIDIAN_CODE = ?");
            stmt.setString(1, code);
            final ResultSet result = stmt.executeQuery();
            /*
             * If the supplied code exists in the database, then we
             * should find only one record.   However, we will do a
             * paranoiac check and verify if there is more records.
             */
            while (result.next())
            {
                final String name      = getString(result, 1);
                final double longitude = getDouble(result, 2);
                final String unit_code = getString(result, 3);
                final Unit unit        = createUnit(unit_code);
                final PrimeMeridian primeMeridian = factory.createPrimeMeridian(name, unit, longitude);
                returnValue = (PrimeMeridian) ensureSingleton(primeMeridian, returnValue, code);
            }
            result.close();
            stmt.close();
        }
        catch (SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        if (returnValue==null)
        {
             throw new NoSuchAuthorityCodeException(code);
        }
        return returnValue;
    }

    /**
     * Returns a datum from a code. This method may
     * returns a vertical, horizontal or local datum.
     *
     * @param  code Value allocated by authority.
     * @return The datum object.
     * @throws NoSuchAuthorityCodeException if this method can't find the requested code.
     * @throws BackingStoreException if some other kind of failure occured in the backing
     *         store. This exception usually have {@link SQLException} as its cause.
     */
    private Datum createDatum(final String code) throws BackingStoreException
    {
        Datum returnValue = null;
        try
        {
            final PreparedStatement stmt;
            stmt = connection.prepareStatement("select DATUM_NAME,"
                                               + " DATUM_TYPE,"
                                               + " ELLIPSOID_CODE"
                                               + " from Datum"
                                               + " where DATUM_CODE = ?");
            stmt.setString(1, code);
            ResultSet result = stmt.executeQuery();
            /*
             * If the supplied code exists in the database, then we
             * should find only one record.   However, we will do a
             * paranoiac check and verify if there is more records.
             */
            while (result.next())
            {
                final String name = getString(result, 1);
                final String type = getString(result, 2).trim(); // On recupere le type.
                final Datum datum;

                // Et on determine le type de Datum a créer.
                if (type.equalsIgnoreCase("vertical"))
                {
                    final DatumType.Vertical dtype = DatumType.Vertical.ELLIPSOIDAL; // TODO
                    datum = factory.createVerticalDatum(name, dtype);
                }
                else if (type.equalsIgnoreCase("geodetic"))
                {
                    final Ellipsoid         ellipsoid = createEllipsoid(getString(result, 3));
                    final WGS84ConversionInfo[] infos = createWGS84ConversionInfo(code);
                    final WGS84ConversionInfo mainInf = (infos.length!=0) ? infos[0] : null;
                    final DatumType.Horizontal  dtype = DatumType.Horizontal.GEOCENTRIC; // TODO
                    // on utilise la premiere info seulement pour le moment.
 /*TEMPORAIRE */    System.err.println("Number of Conversion informations retrieved : " + infos.length);
 /*TEMPORAIRE */    System.err.println("HorizontalDatum :  Using WGS84 informations.");
                    datum = factory.createHorizontalDatum(name, dtype, ellipsoid, mainInf);
                }
                else if (type.equalsIgnoreCase("engineering"))
                {
                    // TODO ???
                    //return factory.createLocalDatum(name, new DatumType.Local("bidon",0,0));
                    result.close();
                    throw new UnsupportedOperationException("DatumType.Local constant not found.");
                }
                else // Problème de consistance du type...
                {
                    result.close();
                    throw new BackingStoreException(Resources.format(ResourceKeys.ERROR_UNKNOW_TYPE_$1, type));
                }
                returnValue = (Datum) ensureSingleton(datum, returnValue, code);
            }
            result.close();
            stmt.close();
        }
        catch (SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        if (returnValue==null)
        {
             throw new NoSuchAuthorityCodeException(code);
        }
        return returnValue;
    }

    /**
     * Returns the differents WGS84 Conversion Informations
     * for a {@link HorizontalDatum}. If the specified datum
     * has no WGS84 conversion informations, then this method
     * will returns either an empty array.
     *
     * @param  code the EPSG code of the {@link HorizontalDatum}.
     * @return an array of {@link WGS84ConversionInfo}, which may
     *         be empty.
     */
    private WGS84ConversionInfo[] createWGS84ConversionInfo(final String code) throws BackingStoreException
    {
        final List list = new ArrayList();
        try
        {
            /* TODO : recuperer la liste des codes des paramètres pour chacune
            des operations en utilisant la future fonction getParameter.
            Cette requete sera donc modifiée et devrait etre plus simple a lire.
            Squelette : On recupere les différentes Operations. A partir de la
            on peut recuperer les différentes listes de paramètres et valeurs.
            on crée donc alors l'objet renvoyé.

            final PreparedStatement stmt = connection.prepareStatement("SELECT co.COORD_OP_CODE,"
                                                                       + " area.AREA_OF_USE"
                                                                       + " FROM Coordinate_Operation AS co,"
                                                                       + " Coordinate_Reference_System AS crs,"
                                                                       + " Area AS area"
                                                                       + " WHERE crs.DATUM_CODE=?"
                                                                       + " AND co.SOURCE_CRS_CODE=crs.COORD_REF_SYS_CODE"
                                                                       + " AND co.TARGET_CRS_CODE=4326"
                                                                       + " AND area.AREA_CODE=co.AREA_OF_USE_CODE"
                                                                       + " ORDER BY co.COORD_OP_CODE");
             Ensuite on recupere la liste des parametres :
             Parameter [] liste = getParameter(code_COORD_OP_CODE);

             Puis pour chaque code on crée un WGSConversionInfo que l'on initialise avec
             les parametres correspondant obtenus par getParameter.

             */
            final PreparedStatement stmt = connection.prepareStatement("SELECT area.AREA_OF_USE,"
                                                                       + " co.COORD_OP_METHOD_CODE,"
                                                                       + " co.COORD_OP_CODE,"
                                                                       + " dx.PARAMETER_VALUE,"
                                                                       + " dy.PARAMETER_VALUE,"
                                                                       + " dz.PARAMETER_VALUE,"
                                                                       + " dx.UOM_CODE,"
                                                                       + " dy.UOM_CODE,"
                                                                       + " dz.UOM_CODE"
                                                                       + " FROM Coordinate_Operation_Parameter_Value AS dx,"
                                                                       + " Coordinate_Operation_Parameter_Value AS dy,"
                                                                       + " Coordinate_Operation_Parameter_Value AS dz,"
                                                                       + " Coordinate_Operation AS co,"
                                                                       + " Coordinate_Reference_System AS crs,"
                                                                       + " Area AS area"
                                                                       + " WHERE crs.DATUM_CODE=?"
                                                                       + " AND co.SOURCE_CRS_CODE=crs.COORD_REF_SYS_CODE"
                                                                       + " AND co.TARGET_CRS_CODE=4326"
                                                                       + " AND dx.COORD_OP_CODE=co.COORD_OP_CODE"
                                                                       + " AND dy.COORD_OP_CODE=co.COORD_OP_CODE"
                                                                       + " AND dz.COORD_OP_CODE=co.COORD_OP_CODE"
                                                                       + " AND area.AREA_CODE=co.AREA_OF_USE_CODE"
                                                                       + " AND dx.PARAMETER_CODE=8605"
                                                                       + " AND dy.PARAMETER_CODE=8606"
                                                                       + " AND dz.PARAMETER_CODE=8607"
                                                                       + " ORDER BY co.COORD_OP_CODE");

            /* This query is used when we have also access to ex, ey, ez and ppm as they are defined
             in WGS84ConversionInfo */
            final PreparedStatement stmt2 = connection.prepareStatement("SELECT ex.PARAMETER_VALUE,"
                                                                       + " ey.PARAMETER_VALUE,"
                                                                       + " ez.PARAMETER_VALUE,"
                                                                       + " ppm.PARAMETER_VALUE,"
                                                                       + " ex.UOM_CODE,"
                                                                       + " ey.UOM_CODE,"
                                                                       + " ez.UOM_CODE"
                                                                       + " FROM Coordinate_Operation_Parameter_Value AS ex,"
                                                                       + " Coordinate_Operation_Parameter_Value AS ey,"
                                                                       + " Coordinate_Operation_Parameter_Value AS ez,"
                                                                       + " Coordinate_Operation_Parameter_Value AS ppm"
                                                                       + " WHERE ex.COORD_OP_CODE=?"
                                                                       + " AND ey.COORD_OP_CODE=ex.COORD_OP_CODE"
                                                                       + " AND ez.COORD_OP_CODE=ex.COORD_OP_CODE"
                                                                       + " AND ppm.COORD_OP_CODE=ex.COORD_OP_CODE"
                                                                       + " AND ex.PARAMETER_CODE=8608"
                                                                       + " AND ey.PARAMETER_CODE=8609"
                                                                       + " AND ez.PARAMETER_CODE=8610"
                                                                       + " AND ppm.PARAMETER_CODE=8611");

            stmt.setString(1, code);
            ResultSet result = stmt.executeQuery();
            while (result.next())
            {
                final WGS84ConversionInfo info = new WGS84ConversionInfo();

                // First we get the description of the area of use
                info.areaOfUse = result.getString(1);

                /* Then we get the coordinates. For each one we convert the unit in meter */
                info.dx = Unit.METRE.convert(getDouble(result, 4), createUnit(getString(result, 7)));
                info.dy = Unit.METRE.convert(getDouble(result, 5), createUnit(getString(result, 8)));
                info.dz = Unit.METRE.convert(getDouble(result, 6), createUnit(getString(result, 9)));

                if (getString(result, 2).equals("9606"))
                {
                    // Here we know that the database provides four more informations
                    // for WGS84 conversion : ex, ey, ez and ppm
                    stmt2.setString(1, getString(result, 3));
                    ResultSet result2 = stmt2.executeQuery();

                    if(result2.next())
                    { // ATTENTION A LA CONVERSION !!!
                        info.ex  = ARC_SECOND.convert(getDouble(result2, 1), createUnit(getString(result2, 5)));
                        info.ey  = ARC_SECOND.convert(getDouble(result2, 2), createUnit(getString(result2, 6)));
                        info.ez  = ARC_SECOND.convert(getDouble(result2, 3), createUnit(getString(result2, 7)));
                        info.ppm = getDouble(result2, 4); // This one is in parts per million, no conversion needed
                    }
                    result2.close();
                    stmt2.close();
                }

                list.add(info);
                //System.err.println(info); /* temporaire */
            }
            result.close();
            stmt.close();
        }
        catch (SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        return (WGS84ConversionInfo[]) list.toArray(new WGS84ConversionInfo[list.size()]);
    }

    /**
     * Returns a coordinate system from a code.
     *
     * @param  code Value allocated by authority.
     * @return The coordinate system object.
     * @throws NoSuchAuthorityCodeException if this method can't find the requested code.
     * @throws BackingStoreException if some other kind of failure occured in the backing
     *         store. This exception usually have {@link SQLException} as its cause.
     */
    public CoordinateSystem createCoordinateSystem(String code) throws BackingStoreException
    {
        CoordinateSystem returnValue = null;
        /* first we retrieve the type of CoordinateSystem */
        final String type = getCoordinateSystemType(code);

        /* if this is a Compound we have an external method because it is
         * different from vertical and geographic */
        if (type.equalsIgnoreCase("compound"))
        {
            return createCompoundCoordinateSystem(code);
        }
        try
        {
            final PreparedStatement stmt;
            stmt = connection.prepareStatement("select DIMENSION,"
                                               + " cs.COORD_SYS_CODE,"
                                               + " COORD_REF_SYS_NAME,"
                                               + " crs.DATUM_CODE,"
                                               + " PRIME_MERIDIAN_CODE"
                                               + " from Coordinate_Reference_System AS crs,"
                                               + " Coordinate_System AS cs,"
                                               + " Datum as datum"
                                               + " where COORD_REF_SYS_CODE = ?"
                                               + " and cs.COORD_SYS_CODE=crs.COORD_SYS_CODE"
                                               + " and datum.DATUM_CODE=crs.DATUM_CODE");
            stmt.setString(1, code);
            final ResultSet result = stmt.executeQuery();
            /*
             * If the supplied code exists in the database, then we
             * should find only one record.   However, we will do a
             * paranoiac check and verify if there is more records.
             */
            while (result.next())
            {
                CoordinateSystem coordSys = null;
                String coordSysCode = getString(result, 2);
                /* we get the information for the axis */
                final AxisInfo[] axisInfos = getAxisInfo(getString(result, 2), getInt(result, 1));

                /* Then we construct the CoordinateSystem */
                if (type.equalsIgnoreCase("vertical"))
                {
                    coordSys = factory.createVerticalCoordinateSystem(getString(result, 3), (VerticalDatum) createDatum(getString(result, 4)), getUnit(coordSysCode), axisInfos[0]);
                }
                else if (type.equalsIgnoreCase("geographic 2D"))
                {
                    coordSys = factory.createGeographicCoordinateSystem(getString(result, 3), getUnit(coordSysCode), (HorizontalDatum) createDatum(getString(result, 4)), createPrimeMeridian(getString(result, 5)), axisInfos[0], axisInfos[1]);
                }
                else if (type.equalsIgnoreCase("projected"))
                {
                    // creer la Projection
                    // creer le systeme de coordonnées
                    coordSys = null;
                }
                returnValue = (CoordinateSystem) ensureSingleton(coordSys, returnValue, code);
            }
            result.close();
            stmt.close();
        }
        catch (SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        if (returnValue==null)
        {
             throw new NoSuchAuthorityCodeException(code);
        }
        return returnValue;
    }

    /**
     * Return the type of a CoordinateReferenceSystem.
     *
     * @param the EPSG code of the system.
     * @retrun the string that give the type of the system.
     */
    private String getCoordinateSystemType(final String code) throws BackingStoreException
    {
        String returnValue = null;
        try
        {
            final PreparedStatement stmt;
            stmt = connection.prepareStatement("select COORD_REF_SYS_KIND"
                                               + " from Coordinate_Reference_System"
                                               + " where COORD_REF_SYS_CODE = ?");
            stmt.setString(1, code);
            final ResultSet result = stmt.executeQuery();
            while (result.next())
            {
                final String type = getString(result, 1).trim();
                returnValue = (String) ensureSingleton(type, returnValue, code);
            }
        }
        catch(SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        if (returnValue==null)
        {
             throw new NoSuchAuthorityCodeException(code);
        }
        return returnValue;
    }

    /**
     * Create a compound CoordinateSystem from the EPSG code.
     *
     * @param code the EPSG code for the CS.
     * @return the compound CS which value was given.
     */
    private CompoundCoordinateSystem createCompoundCoordinateSystem(final String code) throws BackingStoreException
    {
        CompoundCoordinateSystem returnValue = null;
        try
        {
            final PreparedStatement stmt;
            stmt = connection.prepareStatement("select COORD_REF_SYS_NAME,"
                                               + " COORD_REF_SYS_KIND,"
                                               + " CMPD_HORIZCRS_CODE,"
                                               + " CMPD_VERTCRS_CODE"
                                               + " from Coordinate_Reference_System"
                                               + " where COORD_REF_SYS_CODE = ?");
            stmt.setString(1, code);
            final ResultSet result = stmt.executeQuery();
            while (result.next())
            {
                /* just to be very sure because we have already check that in createCoordSys */
                if (!getString(result, 2).equalsIgnoreCase("compound"))
                {
                    throw new NoSuchAuthorityCodeException(code);
                }
                final String                 name = getString(result, 1);
                final CoordinateSystem        cs1 = createCoordinateSystem(getString(result, 3));
                final CoordinateSystem        cs2 = createCoordinateSystem(getString(result, 4));
                final CompoundCoordinateSystem cs = factory.createCompoundCoordinateSystem(name, cs1, cs2);
                returnValue = (CompoundCoordinateSystem) ensureSingleton(cs, returnValue, code);
            }
            result.close();
            stmt.close();
        }
        catch (SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        if (returnValue==null)
        {
             throw new NoSuchAuthorityCodeException(code);
        }
        return returnValue;
    }

    /**
     * Returns the {@link AxisInfo}s from an
     * EPSG code for a {@link CoordinateSystem}.
     *
     * @param  code the EPSG code.
     * @param  dimension of the coordinate system, which is also the
     *         size of the returned Array.
     * @return an array of AxisInfo.
     */
    private AxisInfo[] getAxisInfo(final String code, final int dimension) throws BackingStoreException
    {
        final AxisInfo[] axis = new AxisInfo[dimension];
        try
        {
            final PreparedStatement stmt;
            stmt = connection.prepareStatement("select COORD_AXIS_NAME,"
                                               + " COORD_AXIS_ORIENTATION"
                                               + " from Coordinate_Axis AS ca,"
                                               + " Coordinate_Axis_Name AS can"
                                               + " where COORD_SYS_CODE = ?"
                                               + " and ca.COORD_AXIS_NAME_CODE=can.COORD_AXIS_NAME_CODE"
                                               // Attention au nom de la table le '_' est propre a MySQL qui
                                               // refuse ORDER comme nom de colonne !!!
                                               + " order by _ORDER");
            stmt.setString(1, code);
            final ResultSet result = stmt.executeQuery();
            int i = 0;
            while (result.next())
            {
                final String          name = getString(result, 1);
                final AxisOrientation enum = getAxisOrientation(getString(result, 2));
                axis[i++] = new AxisInfo(name, enum);
            }
            result.close();
            stmt.close();
            if (i!=axis.length)
            {
                throw new BackingStoreException(); // TODO: supply a message.
            }
        }
        catch (SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        return axis;
    }

    /**
     * Returns the {@link AxisOrientation} from on EPSG orientation name.
     */
    private static AxisOrientation getAxisOrientation(final String name) throws BackingStoreException
    {
        if (name.equalsIgnoreCase("north"))
        {
            return AxisOrientation.NORTH;
        }
        if (name.equalsIgnoreCase("south"))
        {
            return AxisOrientation.SOUTH;
        }
        if (name.equalsIgnoreCase("east"))
        {
            return AxisOrientation.EAST;
        }
        if (name.equalsIgnoreCase("west"))
        {
            return AxisOrientation.WEST;
        }
        if (name.equalsIgnoreCase("up"))
        {
            return AxisOrientation.UP;
        }
        if (name.equalsIgnoreCase("down"))
        {
            return AxisOrientation.DOWN;
        }
        throw new BackingStoreException("AxisOrientation \"" + name + "\" is not implemented.");
    }

    /**
     *  Returns the Unit for 1D and 2D CoordinateSystem
     */
    private Unit getUnit(String code) throws BackingStoreException
    {
        Unit returnValue = null;
        try
        {
            final PreparedStatement stmt;
            stmt = connection.prepareStatement("select UOM_CODE"
                                               + " from Coordinate_Axis AS ca"
                                               + " where COORD_SYS_CODE = ?");
            stmt.setString(1, code);
            final ResultSet result = stmt.executeQuery();
            while (result.next())
            {
                final Unit unit = createUnit(getString(result, 1));
                returnValue = (Unit) ensureSingleton(unit, returnValue, code);
            }
            result.close();
            stmt.close();
        }
        catch (SQLException exception)
        {
            throw new BackingStoreException(code, exception);
        }
        if (returnValue==null)
        {
             throw new NoSuchAuthorityCodeException(code);
        }
        return returnValue;
    }

    /**
     * Close the database connection and dispose any resources
     * hold by this object.
     *
     * @throws SQLException if an error occured while closing
     *         the connection.
     */
    public void close() throws SQLException
    {
        connection.close();
    }

    /**
     * Returns the parameter list for an operation method code.
     */
    private Parameter[] getParameter(String op_code) throws BackingStoreException
    {
        // TODO : return a list of Parameter for an operation code.
        // the parameter must be sort by order...
        return null;
    }

    /**
     */
    private static class Parameter
    {
        /**
         * The EPSG code for this Parameter
         */
        private String code;

        /**
         * The name of the parameter.
         */
        private String name;

        /**
         * The value of the parameter.
         */
        private double value;

        /**
         * The Unit for this parameter.
         */
        private Unit unit;

        /**
         * Main class constructor.
         */
        private Parameter(final String code, final String name, final double value, final Unit unit)
        {
            this.code = code;
            this.name = name;
            this.value = value;
            this.unit = unit;
        }

        public String getCode()
        {
            return code;
        }

        public void setCode(final String code)
        {
            this.code = code;
        }

        public String getName()
        {
            return name;
        }

        public void setName(final String name)
        {
            this.name = name;
        }

        public double getValue()
        {
            return value;
        }

        public void setValue(final double value)
        {
            this.value = value;
        }

        public Unit getUnit()
        {
            return unit;
        }

        public void setUnit(final Unit unit)
        {
            this.unit = unit;
        }
    }

    public static void main(String [] args)
    {
        CoordinateSystemEPSGFactory test = null;
        System.out.println("      **********    Programme de Tests     *********\n");
        try
        {
            test = new CoordinateSystemEPSGFactory(new CoordinateSystemFactory(),
                                                   "jdbc:mysql://localhost/EPSG?user=root",
                                                   "org.gjt.mm.mysql.Driver");

            //System.out.println("\n\n<------- Test VerticalDatum 1 : code = 5100, ca existe -------->\n");
            //System.out.println(test.createDatum("5100"));

            //System.out.println("\n\n<------- Test VerticalDatum 2 : code = 6124, pb : datum non vertical -------->\n");
            //System.out.println(test.createVerticalDatum("6124"));

            //System.out.println("\n\n<------- Test HorizontalDatum 1 : code = 6201, ca existe -------->\n");
            //System.out.println(test.createDatum("6201"));

            //System.out.println("\n\n<------- Test HorizontalDatum 2 : code = 6301, ca existe -------->\n");
            //System.out.println(test.createDatum("6301"));

            //System.out.println("\n\n<------- Test HorizontalDatum 3 : code = 6289, ca existe -------->\n");
            //System.out.println(test.createDatum("6289"));

            //System.out.println("\n\n<------- Test HorizontalDatum 4 : code = 6257, ca existe -------->\n");
            //System.out.println(test.createDatum("6257"));

            //System.out.println("\n\n<------- Test LocalDatum 1 : code = 9301, ca existe -------->\n");
            //System.out.println(test.createDatum("9301"));

            //System.out.println("\n\n<------- Test Ellipsoid 1 : code = 17001, ca existe pas... -------->\n");
            //test.createEllipsoid("17001");

            //System.out.println("\n\n<------- Test Ellipsoid 2 : code = 7002, ca existe methode inverse -------->\n");
            //System.out.println(test.createEllipsoid("7002"));

            //System.out.println("\n\n<------- Test Ellipsoid 3 : code = 7008, ca existe methode minor -------->\n");
            //System.out.println(test.createEllipsoid("7008"));

            //facteur de l'unité != 1
            //System.out.println("\n\n<------- Test Ellipsoid 4 : code = 7007, ca existe methode minor -------->\n");
            //System.out.println(test.createEllipsoid("7007"));

            //System.out.println("\n\n<------- Test Ellipsoid 5 : code = 7004, ca existe -------->\n");
            //System.out.println(test.createEllipsoid("7004"));

            //System.out.println("\n\n<------- Test primeMeridian 1 : code = 8901, ca existe -------->\n");
            //test.createPrimeMeridian("8901");

            //System.out.println("\n\n<------- Test primeMeridian 2 : code = 28901, ca existe pas... -------->\n");
            //test.createPrimeMeridian("28901");

            System.out.println("\n\n<------- Test verticalCoordinateSystem 1 : code = 5710 -------->\n");
            System.out.println(test.createCoordinateSystem("5710"));

            System.out.println("\n\n<------- Test GeographicCoordinateSystem 1 : code = 4807 -------->\n");
            System.out.println(test.createCoordinateSystem("4807"));

            System.out.println("\n\n<------- Test GeographicCoordinateSystem 2 : code = 4803 pb unité... -------->\n");
            System.out.println(test.createCoordinateSystem("4803"));

            System.out.println("\n\n<------- Test CompoundCoordinateSystem 1 : code = 7402 -------->\n");
            System.out.println(test.createCoordinateSystem("7402"));

            //System.out.println("\n\n<------- Test CompoundCoordinateSystem 2 : code = 7409 -------->\n");
            //System.out.println(test.createCoordinateSystem("7409"));

            test.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            try
            {
                test.close();
            }
            catch(SQLException exc)
            {
                System.err.println("Ben quand ça veut pas, ça veut pas...\n" + exc.getMessage());
            }
        }
    }
}
