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
package fr.ird.image.sql;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.cv.Coverage;
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.cv.SampleDimension;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.cv.PointOutsideCoverageException;

// Requ�tes SQL et entr�s/sorties
import java.sql.SQLException;
import java.io.IOException;
import java.io.ObjectInputStream;

// Ev�nements
import javax.swing.event.EventListenerList;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;

// Journal
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Divers
import java.util.List;
import java.util.Date;
import java.util.Arrays;
import java.util.Comparator;
import java.awt.geom.Point2D;
import javax.media.jai.util.Range;
import fr.ird.resources.Resources;


/**
 * Enveloppe une table d'images comme s'il s'agissait d'un espace � trois dimensions, la
 * troisi�me dimension �tant le temps.  Cette classe offre une fa�on pratique d'extraire
 * des valeurs � des positions et des dates arbitraires. Les valeurs sont interpoll�es �
 * la fois dans l'espace et dans le temps.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Coverage3D extends Coverage
{
    /**
     * Liste des images � prendre en compte.
     */
    private final ImageEntry[] entries;

    /**
     * Enveloppe des donn�es englob�es par cet objet.
     */
    private final Envelope envelope;

    /**
     * Intervalle de temps maximal tol�r� entre deux images.
     * Si deux images sont s�par�es d'un intervalle de temps
     * sup�rieur, la donn�e sera consid�r�e comme manquante.
     */
    private final long thresold = 10*24*60*60*1000L;

    /**
     * Liste des objets int�ress�s � �tre inform�s
     * des progr�s de la lecture des images.
     */
    private final EventListenerList listeners = new EventListenerList();

    /**
     * Donn�es dont la date de d�but est inf�rieure ou �gale � la date demand�e.
     * Autant que possible, on essaiera de faire en sorte que la date du milieu
     * soit inf�rieure ou �gale � la date demand�e (mais ce second aspect n'est
     * pas garantie).
     */
    private transient GridCoverage lower;

    /**
     * Donn�es dont la date de fin  est sup�rieure ou �gale � la date demand�e.
     * Autant que possible, on essaiera de faire en sorte que la date du milieu
     * soit sup�rieure ou �gale � la date demand�e (mais ce second aspect n'est
     * pas garantie).
     */
    private transient GridCoverage upper;

    /**
     * Date et heure du milieu des donn�es {@link #lower} et {@link #upper},
     * en nombre de millisecondes �coul�es depuis le 1er janvier 1970 UTC.
     */
    private transient long timeLower=Long.MAX_VALUE, timeUpper=Long.MIN_VALUE;

    /**
     * Initialize fields after deserialization.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        timeLower = Long.MAX_VALUE;
        timeUpper = Long.MIN_VALUE;
    }

    /**
     * Construit une couverture � partir des donn�es de la table sp�cifi�e.
     * La entr�es {@link ImageEntry} seront m�moris�es immediatement. Toute
     * modification faite � la table apr�s la construction de cet objet
     * <code>Coverage3D</code> (incluant la fermeture de la table) n'auront
     * aucun effet sur cet objet.
     *
     * @param  table Table d'o� proviennent les donn�es.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public Coverage3D(final ImageTable table) throws SQLException
    {
        super(table.getSeries().getName(), table.getCoordinateSystem(), null, null);
        envelope = table.getEnvelope();
        entries  = table.getEntries();
        Arrays.sort(entries, COMPARATOR);
    }

    /**
     * Comparateur � utiliser pour classer les images et effectuer
     * des recherches rapides. Ce comparateur utilise la date du
     * milieu comme crit�re.
     */
    private static final Comparator<Object> COMPARATOR = new Comparator<Object>()
    {
        public int compare(final Object entry1, final Object entry2)
        {
            final long time1 = getObjectTime(entry1);
            final long time2 = getObjectTime(entry2);
            if (time1 < time2) return -1;
            if (time1 > time2) return +1;
            return 0;
        }
    };

    /**
     * Retourne la date de l'objet sp�cifi�e. L'argument peut �tre un objet
     * {@link Date} ou {@link ImageEntry}.  Dans ce dernier cas, la date du
     * sera extraite avec {@link #getTime}.
     */
    private static long getObjectTime(final Object object)
    {
        if (object instanceof Date)
        {
            return ((Date) object).getTime();
        }
        return getTime((ImageEntry) object);
    }

    /**
     * Retourne la date du milieu de l'image sp�cifi�e.  Si l'image ne couvre aucune
     * plage de temps (par exemple s'il s'agit de donn�es qui ne varient pas avec le
     * temps, comme la bathym�trie), alors cette m�thode retourne {@link Long#MIN_VALUE}.
     */
    private static long getTime(final ImageEntry entry)
    {
        final Range timeRange = entry.getTimeRange();
        if (timeRange!=null)
        {
            final Date startTime = (Date) timeRange.getMinValue();
            final Date   endTime = (Date) timeRange.getMaxValue();
            if (startTime!=null)
            {
                if (endTime!=null)
                {
                    return (endTime.getTime()+startTime.getTime())/2;
                }
                else return startTime.getTime();
            }
            else if (endTime!=null)
            {
                return endTime.getTime();
            }
        }
        return Long.MIN_VALUE;
    }

    /**
     * Retourne l'enveloppe des donn�es. Cet envelope donn�e les valeurs l�gales
     * des coordonn�es qui peuvent �tre pass�es au diff�rentes m�thodes de cette
     * classe.
     */
    public Envelope getEnvelope()
    {return envelope.clone();}

    /**
     * Retourne des informations sur les bandes des images. Note: l'appel de
     * cette m�thode peut �tre tr�s couteuse sur un objet <code>Coverage3D</code>,
     * �tant donn� qu'elle peut forcer la lecture de toute les images de la base
     * de donn�es.
     */
    public List<SampleDimension> getSampleDimensions()
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Enregistre un �v�nements dans le journal.
     */
    private static void log(final int cl�, final Object[] parameters)
    {
        final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINE, cl�);
        record.setSourceClassName("Coverage3D");
        record.setSourceMethodName("evaluate");
        record.setParameters(parameters);
        Table.logger.log(record);
    }

    /**
     * Proc�de � la lecture des images n�cessaires � l'interpolation
     * des donn�es � la date sp�cifi�e. Les images lues seront point�es
     * par {@link #lower} et {@link #upper}.
     *
     * @param  date La date demand�e.
     * @return <code>true</code> si l'interpolation peut �tre faite.
     * @throws PointOutsideCoverageException si la date sp�cifi�e est
     *         en dehors de la plage de temps des donn�es disponibles.
     */
    private boolean seek(final Date date) throws PointOutsideCoverageException
    {
        /*
         * Check if images currently loaded
         * are valid for the requested date.
         */
        final long time = date.getTime();
        if (time>=timeLower && time<=timeUpper)
        {
            return true;
        }
        /*
         * Recherche l'index de l'image � utiliser
         * comme borne sup�rieure ({@link #upper}).
         */
        int index = Arrays.binarySearch(entries, date, COMPARATOR);
        try
        {
            if (index>=0)
            {
                final ImageEntry entry = entries[index];
                log(Cl�.LOAD_ENTRY�1, new Object[]{entry});
                lower = upper = entry.getImage(listeners);
                timeLower = timeUpper = getTime(entry);
                return true;
            }
            index = ~index; // Insertion point (note: ~ is NOT the minus sign).
            if (index==entries.length)
            {
                if (--index<=0) return false;
                final Date endTime = (Date) entries[index].getTimeRange().getMaxValue();
                if (endTime!=null && endTime.getTime()<time)
                {
                    throw new PointOutsideCoverageException(Resources.format(Cl�.DATE_OUTSIDE_COVERAGE�1, date));
                }
            }
            if (index==0)
            {
                if (++index>=entries.length) return false;
                final Date startTime = (Date) entries[index-1].getTimeRange().getMinValue();
                if (startTime!=null && startTime.getTime()>time)
                {
                    throw new PointOutsideCoverageException(Resources.format(Cl�.DATE_OUTSIDE_COVERAGE�1, date));
                }
            }
            final ImageEntry entryLower = entries[index-1];
            final ImageEntry entryUpper = entries[index  ];
            final long timeLower = getTime(entryLower);
            final long timeUpper = getTime(entryUpper);
            if (timeUpper-timeLower > thresold)
            {
                // Si l'�cart de temps entre les deux dates
                // est trop grand, on consid�rera la donn�e
                // comme manquante.
                return false;
            }
            log(Cl�.LOAD_ENTRY�2, new Object[]{entryLower, entryUpper});
            final GridCoverage lower = entryLower.getImage(listeners);
            final GridCoverage upper = entryUpper.getImage(listeners);
            this.lower     = lower; // Set only when BOTH images are OK.
            this.upper     = upper;
            this.timeLower = timeLower;
            this.timeUpper = timeUpper;
            return true;
        }
        catch (IOException exception)
        {
            PointOutsideCoverageException e=new PointOutsideCoverageException(Resources.format(Cl�.DATE_OUTSIDE_COVERAGE�1, date));
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Retourne les valeurs en un point donn�. Une interpolation
     * sera effectu�e � la fois dans l'espace et dans le temps.
     *
     * @param  coord Les coordonn�es spatiales (horizontales) du
     *               point o� extraire les valeurs.
     * @param  time  La date et heure du point o� extraire les valeurs.
     * @param  dest  Un tableau dans lequel placer les valeurs, ou
     *               <code>null</code> pour laisser cette m�thode
     *               cr�er un nouveau tableau.
     * @return Le tableau de valeurs.
     * @throws PointOutsideCoverageException si <code>coord</code>
     *         est en dehors des plages de coordonn�es permises.
     */
    public double[] evaluate(final Point2D coord, final Date time, double[] dest) throws PointOutsideCoverageException
    {
        if (!seek(time))
        {
            if (dest==null) dest=new double[3]; // TODO
            Arrays.fill(dest, Double.NaN); // TODO
            return dest;
        }
        final long t=time.getTime();
        if (Math.abs(t-timeLower) < Math.abs(t-timeUpper))
        {
            return lower.evaluate(coord, dest);
        }
        else
        {
            return upper.evaluate(coord, dest);
        }
    }

    /**
     * Retourne les valeurs en un point donn�. Une interpolation
     * sera effectu�e � la fois dans l'espace et dans le temps.
     *
     * @param  coord Les coordonn�es spatio-temporelles du point
     *               o� extraire les valeurs.
     * @param  dest  Un tableau dans lequel placer les valeurs, ou
     *               <code>null</code> pour laisser cette m�thode
     *               cr�er un nouveau tableau.
     * @return Le tableau de valeurs.
     * @throws PointOutsideCoverageException si <code>coord</code>
     *         est en dehors des plages de coordonn�es permises.
     */
    public double[] evaluate(final CoordinatePoint coord, double[] dest) throws PointOutsideCoverageException
    {
        return null;
    }

    /**
     * Adds an {@link IIOReadWarningListener} to
     * the list of registered warning listeners.
     */
    public void addIIOReadWarningListener(final IIOReadWarningListener listener)
    {listeners.add(IIOReadWarningListener.class, listener);}

    /**
     * Removes an {@link IIOReadWarningListener} from
     * the list of registered warning listeners.
     */
    public void removeIIOReadWarningListener(final IIOReadWarningListener listener)
    {listeners.remove(IIOReadWarningListener.class, listener);}

    /**
     * Adds an {@link IIOReadProgressListener} to
     * the list of registered progress listeners.
     */
    public void addIIOReadProgressListener(final IIOReadProgressListener listener)
    {listeners.add(IIOReadProgressListener.class, listener);}

    /**
     * Removes an {@link IIOReadProgressListener} from
     * the list of registered progress listeners.
     */
    public void removeIIOReadProgressListener(final IIOReadProgressListener listener)
    {listeners.remove(IIOReadProgressListener.class, listener);}
}
