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

// J2SE
import java.util.Date;
import java.util.Calendar;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// Geotools dependencies
import org.geotools.util.WeakHashSet;

// Seagis dependencies
import fr.ird.database.ConfigurationKey;
import fr.ird.resources.seagis.Resources;
import fr.ird.database.coverage.CoverageDataBase;


/**
 * Base class for all implementations provided in the
 * <code>fr.ird.database.coverage.sql</code> package.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class Table extends UnicastRemoteObject implements fr.ird.database.Table {
    /* Default schema name for SQL instructions. */ static final String SCHEMA            = "images";
    /* Default table  name for SQL instructions. */ static final String PHENOMENONS       = "Parameters";
    /* Default table  name for SQL instructions. */ static final String PROCEDURES        = "Operations";
    /* Default table  name for SQL instructions. */ static final String SERIES            = "Series";
    /* Default table  name for SQL instructions. */ static final String SUBSERIES         = "SubSeries";
    /* Default table  name for SQL instructions. */ static final String GRID_COVERAGES    = "GridCoverages";
    /* Default table  name for SQL instructions. */ static final String BOUNDING_BOX      = "GeographicBoundingBoxes";
    /* Default table  name for SQL instructions. */ static final String FORMATS           = "Formats";
    /* Default table  name for SQL instructions. */ static final String SAMPLE_DIMENSIONS = "SampleDimensions";
    /* Default table  name for SQL instructions. */ static final String CATEGORIES        = "Categories";
    
    /**
     * Ensemble d'objets qui ont déjà été créés et qui n'ont pas encore été réclamés par
     * le ramasse-miettes. Cet ensemble sera utilisé avec des objets {@link GridCoverageEntry},
     * {@link FormatEntry}, {@link CategoryList}, etc. pour tenter autant que possible
     * de retourner des objets qui existent déjà en mémoire et, ultimement, éviter de
     * recharger plusieurs fois la même image.
     */
    static final WeakHashSet POOL = new WeakHashSet();

    /**
     * The database where this table come from.
     */
    protected final CoverageDataBase database;

    /**
     * Construct a new table.
     *
     * @param database The database where this table come from.
     */
    protected Table(final CoverageDataBase database) throws RemoteException {
        this.database = database;
    }

    /**
     * Extrait la partie "SELECT ... FROM ..." de la requête spécifiée. Cette méthode
     * retourne la chaîne <code>query</code> à partir du début jusqu'au dernier caractère
     * précédant la première clause "WHERE". La clause "WHERE" et tout ce qui suit ne sera
     * pas inclue.
     */
    static String selectWithoutWhere(final String query) {
        final String clause = "WHERE";
        final int length = clause.length();
        final int stop = query.length()-length;
        for (int i=1; i<stop; i++) {
            if (Character.isWhitespace (query.charAt     (i-1))      &&
                Character.isWhitespace (query.charAt     (i+length)) &&
                clause.equalsIgnoreCase(query.substring(i,i+length)))
            {
                return query.substring(0, i).trim();
            }
        }
        return query;
    }

    /**
     * Create a configuration key for the specified attributes.
     */
    static ConfigurationKey createKey(final String name, final int text, final String SQL) {
        return new ConfigurationKey(name, Resources.formatInternational(text), SQL);
    }

    /**
     * Obtient une date selon le calendrier spécifié.
     * Cette méthode n'est fournie que comme workaround pour le bug #4380653.
     *
     * @param  calendar Calendrier de la base de données.
     * @param  localCalendar calendrier local.
     * @return Date en heure UTC.
     * @throws SQLException si l'interrogation du catalogue a échoué.
     */
    static Date getTimestamp(final int       field,
                             final ResultSet result,
                             final Calendar  calendar,
                             final Calendar  localCalendar)
        throws SQLException
    {
        // NOTE: on ne peut pas utiliser result.getTimestamp(field,localCalendar)
        //       parce que la "correction" de Sun pour le bug #4380653 a en fait
        //       empiré le problème!!!!
        final Date date = result.getTimestamp(field);
        if (date == null) {
            return null;
        }
        localCalendar.setTime(date);
        calendar.     setTime(date);
        calendar.set(Calendar.ERA,         localCalendar.get(Calendar.ERA        ));
        calendar.set(Calendar.YEAR,        localCalendar.get(Calendar.YEAR       ));
        calendar.set(Calendar.DAY_OF_YEAR, localCalendar.get(Calendar.DAY_OF_YEAR));
        calendar.set(Calendar.HOUR_OF_DAY, localCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE,      localCalendar.get(Calendar.MINUTE     ));
        calendar.set(Calendar.SECOND,      localCalendar.get(Calendar.SECOND     ));
        calendar.set(Calendar.MILLISECOND, localCalendar.get(Calendar.MILLISECOND));
        return calendar.getTime();
    }

    /**
     * Returns a property value for the specified key. This method work for a null database,
     * which is convenient for testing purpose.
     */
    protected String getProperty(final ConfigurationKey key) throws RemoteException {
        return (database!=null) ? database.getProperty(key) : key.defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    public final CoverageDataBase getDataBase() throws RemoteException {
        return database;
    }
}
