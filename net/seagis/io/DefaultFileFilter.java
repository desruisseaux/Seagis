/*
 * SEAGIS - An OpenSource implementation of OpenGIS specification
 *          (C) 2001, Institut de Recherche pour le Développement
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
 */
package net.seagis.io;

// Miscellaneous
import java.io.File;
import java.io.FilenameFilter;
import javax.swing.filechooser.FileFilter;

// Regular expression
//----- BEGIN JDK 1.4 DEPENDENCIES -----
import java.util.regex.Pattern;
import java.util.regex.Matcher;
//----- END OF JDK 1.4 DEPENDENCIES ----


/**
 * A {@link FileFilter} implementation using Unix-style wildcards.
 * <br><br>
 * Note: This implementation require J2SE 1.4. It will
 *       filter nothing on the J2S3 1.3 version of SEAGIS.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class DefaultFileFilter extends FileFilter implements java.io.FileFilter, FilenameFilter
{
    /**
     * The description of this filter, usually
     * for graphical user interfaces.
     */
    private final String description;

    /**
     * The pattern to matchs to filenames.
     */
//----- BEGIN JDK 1.4 DEPENDENCIES -----
    private final Pattern pattern;
//----- END OF JDK 1.4 DEPENDENCIES ----

    /**
     * Construct a file filter for the specified pattern.
     * Pattern may contains the "*" and "?" wildcards.
     *
     * @param pattern The pattern (e.g. "*.png").
     */
    public DefaultFileFilter(final String pattern)
    {this(pattern, new File(pattern).getName());}

    /**
     * Construct a file filter for the specified pattern
     * and description. Pattern may contains the "*" and
     * "?" wildcards.
     *
     * @param pattern The pattern (e.g. "*.png").
     * @param description The description of this filter,
     *        usually for graphical user interfaces.
     */
    public DefaultFileFilter(final String pattern, final String description)
    {
        this.description = description.trim();
        final int length = pattern.length();
        final StringBuffer buffer = new StringBuffer(length+8);
        for (int i=0; i<length; i++)
        {
            final char c = pattern.charAt(i);
            if (!Character.isLetterOrDigit(c))
            {
                switch (c)
                {
                    case '?': // Fall through
                    case '*': buffer.append('.');  break;
                    default : buffer.append('\\'); break;
                }
            }
            buffer.append(c);
        }
//----- BEGIN JDK 1.4 DEPENDENCIES -----
        this.pattern = Pattern.compile(buffer.toString());
//----- END OF JDK 1.4 DEPENDENCIES ----
    }

    /**
     * Returns the description of this filter.
     * For example: "PNG images"
     */
    public String getDescription()
    {return description;}

    /**
     * Tests if a specified file matches the pattern.
     *
     * @param  file The file to be tested.
     * @return <code>true</code> if and only if
     *         the name matches the pattern.
     */
    public boolean accept(final File file)
    {
        if (file!=null)
        {
//----- BEGIN JDK 1.4 DEPENDENCIES -----
            return pattern.matcher(file.getName()).matches();
/*----- END OF JDK 1.4 DEPENDENCIES ----
            return true;
------- END OF JDK 1.3 FALLBACK --------*/
        }
        return false;
    }
    
    /**
     * Tests if a specified file matches the pattern.
     *
     * @param  dir    the directory in which the file was found.
     * @param  name   the name of the file.
     * @return <code>true</code> if and only if
     *         the name matches the pattern.
     */
    public boolean accept(File dir, String name)
    {
        if (name!=null)
        {
//----- BEGIN JDK 1.4 DEPENDENCIES -----
            return pattern.matcher(name).matches();
/*----- END OF JDK 1.4 DEPENDENCIES ----
            return true;
------- END OF JDK 1.3 FALLBACK --------*/
        }
        return false;
    }
}
