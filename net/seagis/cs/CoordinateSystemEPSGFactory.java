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

// Logging
//----- BEGIN JDK 1.4 DEPENDENCIES ----
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
//----- END OF JDK 1.4 DEPENDENCIES ---

// Miscellaneous
import javax.units.Unit;
import java.util.NoSuchElementException;

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
     * @return The string at the specified column.
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
            ResultSet result = stmt.executeQuery();
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
/* TEMPORARY */         System.err.println("Using method : createEllipsoid");
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
/* TEMPORARY */     System.err.println("Using method : createFlattenedSphere");
                    ellipsoid = factory.createFlattenedSphere(name, semiMajorAxis, inverseFlattening, unit);
                }
                /*
                 * Now that we have built an ellipsoid, compare
                 * it with the previous one (if any).
                 */
                if (returnValue!=null)
                {
                    if (!returnValue.equals(ellipsoid))
                    {
                        result.close();
                        throw new BackingStoreException(Resources.format(ResourceKeys.ERROR_DUPLICATED_VALUES_$1, code));
                    }
                }
                else returnValue = ellipsoid;
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
            ResultSet result = stmt.executeQuery();
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
                    /// Je sais pas quoi faire donc en attendant...
                    throw new UnsupportedOperationException("both factors (B & C) are NULL.\n"
                                                            + "That's OK, it's possible... but I don't know"
                                                            + " what to do...");
                }
                if (b==0 || c==0)
                {
                    throw new BackingStoreException(code);
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
                    throw new UnsupportedOperationException("Type scale not implemented in createUnit");
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
                if (returnValue!=null)
                {
                    if (!returnValue.equals(unit))
                    {
                        result.close();
                        throw new BackingStoreException(Resources.format(ResourceKeys.ERROR_DUPLICATED_VALUES_$1, code));
                    }
                }
                else returnValue = unit;
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
        try
        {
            PreparedStatement stmt = connection.prepareStatement("select PRIME_MERIDIAN_NAME,"
                                                                 + " GREENWICH_LONGITUDE,"
                                                                 + " UOM_CODE"
                                                                 + " from Prime_Meridian"
                                                                 + " where PRIME_MERIDIAN_CODE = ?");
            stmt.setString(1, code);
            ResultSet result = stmt.executeQuery();

            if (!result.next())
            {
                throw new NoSuchAuthorityCodeException(code);
            }

            String name      = getString(result, 1);
            double longitude = getDouble(result, 2);
            String unit_code = getString(result, 3);

            result.close();
            stmt.close();

            Unit unit = createUnit(unit_code);

            return factory.createPrimeMeridian(name, unit, longitude);
        }
        catch (SQLException e)
        {
            // TODO : On renvoit quoi ???
            NoSuchAuthorityCodeException exc = new NoSuchAuthorityCodeException(code);
            exc.initCause(e);
            throw exc;
        }
    }

    /**
     * Returns a datum which type from a code.
     * This could be a vertical, horizontal or local datum.
     *
     * @param  code Value allocated by authority.
     * @return The datum object.
     * @throws NoSuchAuthorityCodeException if this method can't find the requested code.
     * @throws BackingStoreException if some other kind of failure occured in the backing
     *         store. This exception usually have {@link SQLException} as its cause.
     */
    private Datum createDatum(final String code) throws BackingStoreException
    {
        try
        {
            PreparedStatement stmt = connection.prepareStatement("select DATUM_NAME,"
                                                                 + " DATUM_TYPE,"
                                                                 + " ELLIPSOID_CODE"
                                                                 + " from Datum"
                                                                 + " where DATUM_CODE = ?");
            stmt.setString(1, code);
            ResultSet result = stmt.executeQuery();

            if (!result.next())
            {
                throw new NoSuchAuthorityCodeException(code);
            }

            String name = getString(result, 1);
            // On recupere le type.
            String type = getString(result, 2);
            type = type.trim();

            // Et on determine lengthtype de Datum a créer.
            if (type.equalsIgnoreCase("vertical"))
            {
                return factory.createVerticalDatum(name, DatumType.Vertical.ELLIPSOIDAL);
            }
            else if (type.equalsIgnoreCase("geodetic"))
            {
                String ellipsoid_code = getString(result, 3);
                // TODO : remplacer par l'autre createHorizontalDatum (WGS84);
                return factory.createHorizontalDatum(name, createEllipsoid(ellipsoid_code));
            }
            else if (type.equalsIgnoreCase("engineering"))
            {
                // TODO ???
                //return factory.createLocalDatum(name, new DatumType.Local("bidon",0,0));
                throw new UnsupportedOperationException("DatumType.Local constant not found.");
            }
            else // Problème de consistance du type...
            {
                throw new NoSuchAuthorityCodeException(code + " is not a known datum type.");
            }
        }
        catch(SQLException e)
        {
            // TODO : On renvoit quoi ???
            NoSuchAuthorityCodeException exc = new NoSuchAuthorityCodeException(code);
            exc.initCause(e);
            throw exc;
        }
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

    public static void main(String [] args)
    {
        CoordinateSystemEPSGFactory test = null;
        System.out.println("      **********    Programme de Tests     *********\n");
        try
        {
            test = new CoordinateSystemEPSGFactory(new CoordinateSystemFactory(),
                                                   "jdbc:mysql://localhost/EPSG?user=root",
                                                   "org.gjt.mm.mysql.Driver");

            System.out.println("\nTest VerticalDatum 1 : code = 5100, ca existe");
            System.out.println(test.createDatum("5100"));

            //System.out.println("\nTest VerticalDatum 2 : code = 6124, pb : datum non vertical");
            //System.out.println(test.createVerticalDatum("6124"));

            System.out.println("\nTest HorizontalDatum 1 : code = 6124, ca existe");
            System.out.println(test.createDatum("6124"));

            System.out.println("\nTest LocalDatum 1 : code = 9301, ca existe");
            System.out.println(test.createDatum("9301"));

            //System.out.println("\nTest Ellipsoid 1 : code = 17001, ca existe pas...");
            //test.createEllipsoid("17001");

            System.out.println("\nTest Ellipsoid 2 : code = 7002, ca existe methode inverse");
            System.out.println(test.createEllipsoid("7002"));

            System.out.println("\nTest Ellipsoid 3 : code = 7008, ca existe methode minor");
            System.out.println(test.createEllipsoid("7008"));

            //marche pas a cause du facteur de l'unité != 1
            //System.out.println("\nTest Ellipsoid 3 : code = 7007, ca existe methode minor");
            //System.out.println(test.createEllipsoid("7007"));

            System.out.println("\nTest Ellipsoid 4 : code = 7004, ca existe");
            System.out.println(test.createEllipsoid("7004"));

            //System.out.println("\nTest primeMeridian 1 : code = 8901, ca existe");
            //test.createPrimeMeridian("8901");

            //System.out.println("\nTest primeMeridian 2 : code = 28901, ca existe pas...");
            //test.createPrimeMeridian("28901");

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
