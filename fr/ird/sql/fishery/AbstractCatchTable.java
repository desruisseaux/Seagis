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
 *
 *
 * Contact: Michel Petit
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.sql.fishery;

// Base de donn�es
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
 * Objet interrogeant la base de donn�es pour obtenir la liste des p�ches
 * qu'elle contient. Ces p�ches pourront �tre s�lectionn�es dans une certaine
 * r�gion g�ographique et � certaines dates.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class AbstractCatchTable extends Table implements CatchTable
{
    /**
     * Calendrier utilis� pour pr�parer les dates.
     * Ce calendrier utilisera le fuseau horaire
     * sp�cifi� lors de la construction.
     */
    protected final Calendar calendar;

    /**
     * Work around for Sun's bug #4380653. Used by 'getTimestamp(...)'
     */
    private transient Calendar localCalendar;

    /**
     * Ensemble immutable des esp�ces. Le contenu d'un objet {@link SpeciesSet} ne doit
     * pas changer. Toutefois, <code>species</code> pourra se r�f�rer � d'autres objets
     * {@link SpeciesSet}. Les objets {@link SpeciesSet} enveloppe la liste des esp�ces
     * dans un tableau ({@link SpeciesSet#species}) qui sera acc�d� directement par les
     * classes {@link AbstractCatchEntry} et d�riv�es.
     */
    protected SpeciesSet species;

    /**
     * Construit une objet qui interrogera la
     * base de donn�es en utilisant la requ�te
     * sp�cifi�e.
     *
     * @param  statement Interrogation � soumettre � la base de donn�es.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @param  species Ensemble des esp�ces demand�es.
     * @throws SQLException si <code>FisheryTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected AbstractCatchTable(final PreparedStatement statement, final TimeZone timezone, final Set<Species> species) throws SQLException
    {
        super(statement);
        this.species  = new SpeciesSet(species);
        this.calendar = new GregorianCalendar(timezone);
    }

    /**
     * Compl�te la requ�te SQL en ajouter les noms de colonnes des esp�ces
     * sp�cifi�es juste avant la premi�re clause "FROM" dans la requ�te SQL.
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
     * Proc�de � l'extraction d'une date
     * en tenant compte du fuseau horaire.
     */
    final Date getTimestamp(final int field, final ResultSet result) throws SQLException
    {
        if (false)
        {
            // Cette ligne aurait suffit si ce n'�tait du bug #4380653...
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
     * Retourne l'ensemble des esp�ces comprises dans la requ�te de cette table.
     * L'ensemble retourn� est immutable.
     */
    public final Set<Species> getSpecies()
    {return species;}

    /**
     * Retourne le syst�me de coordonn�es utilis�es
     * pour les positions de p�ches dans cette table.
     */
    public final CoordinateSystem getCoordinateSystem()
    {return GeographicCoordinateSystem.WGS84;}

    /**
     * D�finit la plage de dates dans laquelle on veut rechercher des donn�es de p�ches.
     * Toutes les p�ches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des donn�es.
     *         Cette plage doit �tre constitu�e d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public final void setTimeRange(final Range timeRange) throws SQLException
    {setTimeRange((Date)timeRange.getMinValue(), (Date)timeRange.getMaxValue());}
}
