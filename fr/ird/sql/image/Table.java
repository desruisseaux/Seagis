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
package fr.ird.sql.image;

// Base de données
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Journal
import java.util.logging.Logger;

// Divers
import java.io.File;
import java.util.Date;
import java.util.Calendar;
import java.util.prefs.Preferences;
import fr.ird.resources.Resources;
import net.seas.util.WeakHashSet;
import net.seas.util.XClass;


/**
 * Classe de base de toute les implémentations du paquet <code>fr.ird.sql.image</code>.
 * Cette classe ne définit pas de champs; elle ne définit que des constantes et des
 * méthodes statiques utilisées par la plupart des classes.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
abstract class Table implements fr.ird.sql.Table
{
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String AREAS       = "Areas";
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String IMAGES      = "Images_new"; // TODO
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String FORMATS     = "Formats";
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String BANDS       = "Bands";
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String CATEGORIES  = "Categories";
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String GROUPS      = "Groups";
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String SERIES      = "Series";
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String VECTORS     = "Vectors";
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String OPERATIONS  = "Operations";
    /* Nom de table <strong>par défaut</strong> dans les instructions SQL. */ static final String PARAMETERS  = "Parameters";

    /**
     * Constante désignant la valeur "True" dans la clause "WHERE". Pour Access, c'est "True".
     * Pour MySQL, c'est 'Y' (entre appostrophes). Vive les standards qui n'en sont pas!
     */
    static final String TRUE = "True";

    /**
     * Ensemble d'objets qui ont déjà été créés et qui n'ont pas encore été réclamés par
     * le ramasse-miettes. Cet ensemble sera utilisé avec des objets {@link ImageEntry},
     * {@link FormatEntry}, {@link CategoryList}, etc. pour tenter autant que possible
     * de retourner des objets qui existent déjà en mémoire et, ultimement, éviter de
     * recharger plusieurs fois la même image.
     */
    static final WeakHashSet<Object> pool=new WeakHashSet<Object>();

    /**
     * Journal des évènements.
     */
    static final Logger logger = Logger.getLogger("fr.ird.sql");

    /**
     * Propriétés de la base de données. Ces propriétés peuvent contenir
     * notamment les instructions SQL à utiliser pour interroger la base
     * de données d'images.
     */
    static final Preferences preferences=Preferences.systemNodeForPackage(Table.class);

    /**
     * Répertoire racine à partir d'où rechercher les
     * images, ou <code>null</code> pour utiliser le
     * répertoire courant.
     */
    static File directory;
    static
    {
        final String dir = preferences.get("directory", null);
        if (dir!=null) directory = new File(dir);
    }

    /**
     * Extrait la partie "SELECT ... FROM ..." de la requête spécifiée. Cette méthode
     * retourne la chaîne <code>query</code> à partir du début jusqu'au dernier caractère
     * précédant la première clause "WHERE". La clause "WHERE" et tout ce qui suit ne sera
     * pas inclue.
     */
    static String select(final String query)
    {
        final String clause="WHERE";
        final int length=clause.length();
        final int stop = query.length()-length;
        for (int i=1; i<stop; i++)
        {
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
     * Implémentation de {@link ImageTableImpl#getTimestamp} avec les calendriers spécifiés.
     * Cette méthode n'est fournie que comme workaround pour le bug #4380653.
     *
     * @param  calendar Calendrier de la base de données.
     * @param  localCalendar calendrier local.
     * @return Date en heure UTC.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    static Date getTimestamp(final int field, final ResultSet result, final Calendar calendar, final Calendar localCalendar) throws SQLException
    {
        Date date;
        try
        {
            date=result.getTimestamp(field, localCalendar);
        }
        catch (SQLException exception)
        {
            if (XClass.getShortClassName(exception).startsWith("NotImplemented"))
            {
                // Workaround for a bug in MySQL's JDBC:
                // org.gjt.mm.mysql.jdbc2.NotImplemented
                date=result.getTimestamp(field);
            }
            else throw exception;
        }
        if (date==null) return null;
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
