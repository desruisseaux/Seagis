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
package fr.ird.sql.fishery;

// Base de données
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Temps
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;

// Cartographie
import net.seagis.resources.Utilities;
import net.seagis.cs.CoordinateSystem;
import net.seagis.cs.GeographicCoordinateSystem;

// Collections
import java.util.Set;
import java.util.Iterator;

// Divers
import fr.ird.animat.Species;
import javax.media.jai.util.Range;


/**
 * Objet interrogeant la base de données pour obtenir la liste des pêches
 * qu'elle contient. Ces pêches pourront être sélectionnées dans une certaine
 * région géographique et à certaines dates.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class AbstractCatchTable extends Table implements CatchTable
{
    /**
     * Calendrier utilisé pour préparer les dates.
     * Ce calendrier utilisera le fuseau horaire
     * spécifié lors de la construction.
     */
    protected final Calendar calendar;

    /**
     * Work around for Sun's bug #4380653. Used by 'getTimestamp(...)'
     */
    private transient Calendar localCalendar;

    /**
     * Ensemble immutable des espèces. Le contenu d'un objet {@link SpeciesSet} ne doit
     * pas changer. Toutefois, <code>species</code> pourra se référer à d'autres objets
     * {@link SpeciesSet}. Les objets {@link SpeciesSet} enveloppe la liste des espèces
     * dans un tableau ({@link SpeciesSet#species}) qui sera accédé directement par les
     * classes {@link AbstractCatchEntry} et dérivées.
     */
    protected SpeciesSet species;

    /**
     * Construit une objet qui interrogera la
     * base de données en utilisant la requête
     * spécifiée.
     *
     * @param  statement Interrogation à soumettre à la base de données.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de données. Cette information est utilisée pour convertir
     *         en heure GMT les dates écrites dans la base de données.
     * @param  species Ensemble des espèces demandées.
     * @throws SQLException si <code>FisheryTable</code> n'a pas pu construire sa requête SQL.
     */
    protected AbstractCatchTable(final PreparedStatement statement, final TimeZone timezone, final Set<Species> species) throws SQLException
    {
        super(statement);
        this.species  = new SpeciesSet(species);
        this.calendar = new GregorianCalendar(timezone);
    }

    /**
     * Complète la requète SQL en ajouter les noms de colonnes des espèces
     * spécifiées juste avant la première clause "FROM" dans la requête SQL.
     */
    static String completeQuery(String query, final String table, final Set<Species> species)
    {
        int index = query.toUpperCase().indexOf("FROM");
        if (index>=0)
        {
            while (index>=1 && Character.isWhitespace(query.charAt(index-1))) index--;
            final StringBuffer buffer=new StringBuffer(query.substring(0, index));
            for (final Iterator<Species> it=species.iterator(); it.hasNext();)
            {
                final String name = it.next().getName(null);
                if (name!=null)
                {
                    buffer.append(", ");
                    buffer.append(table);
                    buffer.append('.');
                    buffer.append(name);
                }
            }
            buffer.append(query.substring(index));
            query=buffer.toString();
        }
        return query;
    }

    /**
     * Procède à l'extraction d'une date
     * en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException
    {
        if (false)
        {
            // Cette ligne aurait suffit si ce n'était du bug #4380653...
            return result.getTimestamp(field, calendar);
        }
        else
        {
            if (localCalendar==null)
                localCalendar=new GregorianCalendar();
            Date date;
            try
            {
                date=result.getTimestamp(field, localCalendar);
            }
            catch (SQLException exception)
            {
                if (Utilities.getShortClassName(exception).startsWith("NotImplemented"))
                {
                    // Workaround for a bug in MySQL's JDBC:
                    // org.gjt.mm.mysql.jdbc2.NotImplemented
                    date=result.getTimestamp(field);
                }
                else throw exception;
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
    }

    /**
     * Retourne l'ensemble des espèces comprises dans la requête de cette table.
     * L'ensemble retourné est immutable.
     */
    public final Set<Species> getSpecies()
    {return species;}

    /**
     * Retourne le système de coordonnées utilisées
     * pour les positions de pêches dans cette table.
     */
    public final CoordinateSystem getCoordinateSystem()
    {return GeographicCoordinateSystem.WGS84;}

    /**
     * Définit la plage de dates dans laquelle on veut rechercher des données de pêches.
     * Toutes les pêches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des données.
     *         Cette plage doit être constituée d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public final void setTimeRange(final Range timeRange) throws SQLException
    {setTimeRange((Date)timeRange.getMinValue(), (Date)timeRange.getMaxValue());}
}
