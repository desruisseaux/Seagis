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
package fr.ird.sql.image;

// Base de donn�es
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import fr.ird.sql.DataBase;

// Journal
import java.util.logging.Logger;

// Divers
import java.io.File;
import java.util.Date;
import java.util.Calendar;
import java.util.prefs.Preferences;
import fr.ird.resources.Resources;
import fr.ird.util.WeakHashSet;
import org.geotools.resources.Utilities;


/**
 * Classe de base de toute les impl�mentations du paquet <code>fr.ird.sql.image</code>.
 * Cette classe ne d�finit pas de champs; elle ne d�finit que des constantes et des
 * m�thodes statiques utilis�es par la plupart des classes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
abstract class Table implements fr.ird.sql.Table {
    /**
     * Retourne une des pr�f�rences du syst�me.  Cette m�thode d�finit les
     * param�tres par d�faut qui seront utilis�s lorsque l'utilisateur n'a
     * pas d�fini de pr�f�rence pour un param�tre.
     */
    static String getPreference(final String name) {
        String def=null;
        if (name != null) {
                 if (name.equalsIgnoreCase(DataBase.DRIVER))   def = "sun.jdbc.odbc.JdbcOdbcDriver";
            else if (name.equalsIgnoreCase(DataBase.SOURCE))   def = "jdbc:odbc:SEAS-Images";
            else if (name.equalsIgnoreCase(DataBase.TIMEZONE)) def = "UTC";
        }
        return Table.preferences.get(name, def);
    }

    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String AREAS       = "Areas";
    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String IMAGES      = "Images";
    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String FORMATS     = "Formats";
    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String BANDS       = "Bands";
    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String CATEGORIES  = "Categories";
    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String GROUPS      = "Groups";
    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String SERIES      = "Series";
    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String VECTORS     = "Vectors";
    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String OPERATIONS  = "Operations";
    /* Nom de table <strong>par d�faut</strong> dans les instructions SQL. */ static final String PARAMETERS  = "Parameters";

    /**
     * Constante d�signant la valeur "True" dans la clause "WHERE". Pour Access, c'est "True".
     * Pour MySQL, c'est 'Y' (entre appostrophes). Vive les standards qui n'en sont pas!
     */
    static final String TRUE = "TRUE"; // PostgreSQL demande que "TRUE" et "FALSE" soient en majuscules.

    /**
     * Cl� � utiliser pour m�moriser dans les pr�f�rences
     * le r�pertoire racine des images.
     */
    static final String DIRECTORY = "Directory";

    /**
     * Ensemble d'objets qui ont d�j� �t� cr��s et qui n'ont pas encore �t� r�clam�s par
     * le ramasse-miettes. Cet ensemble sera utilis� avec des objets {@link ImageEntry},
     * {@link FormatEntry}, {@link CategoryList}, etc. pour tenter autant que possible
     * de retourner des objets qui existent d�j� en m�moire et, ultimement, �viter de
     * recharger plusieurs fois la m�me image.
     */
    static final WeakHashSet<Object> pool=new WeakHashSet<Object>();

    /**
     * Journal des �v�nements.
     */
    static final Logger logger = Logger.getLogger("fr.ird.sql");
    static {
        fr.ird.util.InterlineFormatter.init(logger);
    }

    /**
     * Propri�t�s de la base de donn�es. Ces propri�t�s peuvent contenir
     * notamment les instructions SQL � utiliser pour interroger la base
     * de donn�es d'images.
     */
    static final Preferences preferences=Preferences.systemNodeForPackage(Table.class);

    /**
     * R�pertoire racine � partir d'o� rechercher les
     * images, ou <code>null</code> pour utiliser le
     * r�pertoire courant.
     */
    static File directory;
    static {
        final String dir = preferences.get(DIRECTORY, null);
        if (dir != null) {
            directory = new File(dir);
        }
    }

    /**
     * Extrait la partie "SELECT ... FROM ..." de la requ�te sp�cifi�e. Cette m�thode
     * retourne la cha�ne <code>query</code> � partir du d�but jusqu'au dernier caract�re
     * pr�c�dant la premi�re clause "WHERE". La clause "WHERE" et tout ce qui suit ne sera
     * pas inclue.
     */
    static String select(final String query) {
        final String clause="WHERE";
        final int length=clause.length();
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
     * Impl�mentation de {@link ImageTableImpl#getTimestamp} avec les calendriers sp�cifi�s.
     * Cette m�thode n'est fournie que comme workaround pour le bug #4380653.
     *
     * @param  calendar Calendrier de la base de donn�es.
     * @param  localCalendar calendrier local.
     * @return Date en heure UTC.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    static Date getTimestamp(final int field, final ResultSet result, final Calendar calendar, final Calendar localCalendar) throws SQLException
    {
        Date date;
        try {
            date=result.getTimestamp(field, localCalendar);
        } catch (SQLException exception) {
            if (Utilities.getShortClassName(exception).startsWith("NotImplemented")) {
                // Workaround for a bug in MySQL's JDBC:
                // org.gjt.mm.mysql.jdbc2.NotImplemented
                date = result.getTimestamp(field);
            } else {
                throw exception;
            }
        }
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
}
