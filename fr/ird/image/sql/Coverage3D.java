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
package fr.ird.image.sql;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.cv.Coverage;
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.cv.SampleDimension;
import net.seas.opengis.pt.CoordinatePoint;
import net.seas.opengis.cv.PointOutsideCoverageException;

// Requêtes SQL et entrés/sorties
import java.sql.SQLException;
import java.io.IOException;
import java.io.ObjectInputStream;

// Evénements
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
 * Enveloppe une table d'images comme s'il s'agissait d'un espace à trois dimensions, la
 * troisième dimension étant le temps.  Cette classe offre une façon pratique d'extraire
 * des valeurs à des positions et des dates arbitraires. Les valeurs sont interpollées à
 * la fois dans l'espace et dans le temps.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Coverage3D extends Coverage
{
    /**
     * Liste des images à prendre en compte.
     */
    private final ImageEntry[] entries;

    /**
     * Enveloppe des données englobées par cet objet.
     */
    private final Envelope envelope;

    /**
     * Intervalle de temps maximal toléré entre deux images.
     * Si deux images sont séparées d'un intervalle de temps
     * supérieur, la donnée sera considérée comme manquante.
     */
    private final long thresold = 10*24*60*60*1000L;

    /**
     * Liste des objets intéressés à être informés
     * des progrès de la lecture des images.
     */
    private final EventListenerList listeners = new EventListenerList();

    /**
     * Données dont la date de début est inférieure ou égale à la date demandée.
     * Autant que possible, on essaiera de faire en sorte que la date du milieu
     * soit inférieure ou égale à la date demandée (mais ce second aspect n'est
     * pas garantie).
     */
    private transient GridCoverage lower;

    /**
     * Données dont la date de fin  est supérieure ou égale à la date demandée.
     * Autant que possible, on essaiera de faire en sorte que la date du milieu
     * soit supérieure ou égale à la date demandée (mais ce second aspect n'est
     * pas garantie).
     */
    private transient GridCoverage upper;

    /**
     * Date et heure du milieu des données {@link #lower} et {@link #upper},
     * en nombre de millisecondes écoulées depuis le 1er janvier 1970 UTC.
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
     * Construit une couverture à partir des données de la table spécifiée.
     * La entrées {@link ImageEntry} seront mémorisées immediatement. Toute
     * modification faite à la table après la construction de cet objet
     * <code>Coverage3D</code> (incluant la fermeture de la table) n'auront
     * aucun effet sur cet objet.
     *
     * @param  table Table d'où proviennent les données.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public Coverage3D(final ImageTable table) throws SQLException
    {
        super(table.getSeries().getName(), table.getCoordinateSystem(), null, null);
        envelope = table.getEnvelope();
        entries  = table.getEntries();
        Arrays.sort(entries, COMPARATOR);
    }

    /**
     * Comparateur à utiliser pour classer les images et effectuer
     * des recherches rapides. Ce comparateur utilise la date du
     * milieu comme critère.
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
     * Retourne la date de l'objet spécifiée. L'argument peut être un objet
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
     * Retourne la date du milieu de l'image spécifiée.  Si l'image ne couvre aucune
     * plage de temps (par exemple s'il s'agit de données qui ne varient pas avec le
     * temps, comme la bathymétrie), alors cette méthode retourne {@link Long#MIN_VALUE}.
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
     * Retourne l'enveloppe des données. Cet envelope donnée les valeurs légales
     * des coordonnées qui peuvent être passées au différentes méthodes de cette
     * classe.
     */
    public Envelope getEnvelope()
    {return envelope.clone();}

    /**
     * Retourne des informations sur les bandes des images. Note: l'appel de
     * cette méthode peut être très couteuse sur un objet <code>Coverage3D</code>,
     * étant donné qu'elle peut forcer la lecture de toute les images de la base
     * de données.
     */
    public List<SampleDimension> getSampleDimensions()
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Enregistre un évènements dans le journal.
     */
    private static void log(final int clé, final Object[] parameters)
    {
        final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINE, clé);
        record.setSourceClassName("Coverage3D");
        record.setSourceMethodName("evaluate");
        record.setParameters(parameters);
        Table.logger.log(record);
    }

    /**
     * Procède à la lecture des images nécessaires à l'interpolation
     * des données à la date spécifiée. Les images lues seront pointées
     * par {@link #lower} et {@link #upper}.
     *
     * @param  date La date demandée.
     * @return <code>true</code> si l'interpolation peut être faite.
     * @throws PointOutsideCoverageException si la date spécifiée est
     *         en dehors de la plage de temps des données disponibles.
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
         * Recherche l'index de l'image à utiliser
         * comme borne supérieure ({@link #upper}).
         */
        int index = Arrays.binarySearch(entries, date, COMPARATOR);
        try
        {
            if (index>=0)
            {
                final ImageEntry entry = entries[index];
                log(Clé.LOAD_ENTRY¤1, new Object[]{entry});
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
                    throw new PointOutsideCoverageException(Resources.format(Clé.DATE_OUTSIDE_COVERAGE¤1, date));
                }
            }
            if (index==0)
            {
                if (++index>=entries.length) return false;
                final Date startTime = (Date) entries[index-1].getTimeRange().getMinValue();
                if (startTime!=null && startTime.getTime()>time)
                {
                    throw new PointOutsideCoverageException(Resources.format(Clé.DATE_OUTSIDE_COVERAGE¤1, date));
                }
            }
            final ImageEntry entryLower = entries[index-1];
            final ImageEntry entryUpper = entries[index  ];
            final long timeLower = getTime(entryLower);
            final long timeUpper = getTime(entryUpper);
            if (timeUpper-timeLower > thresold)
            {
                // Si l'écart de temps entre les deux dates
                // est trop grand, on considèrera la donnée
                // comme manquante.
                return false;
            }
            log(Clé.LOAD_ENTRY¤2, new Object[]{entryLower, entryUpper});
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
            PointOutsideCoverageException e=new PointOutsideCoverageException(Resources.format(Clé.DATE_OUTSIDE_COVERAGE¤1, date));
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Retourne les valeurs en un point donné. Une interpolation
     * sera effectuée à la fois dans l'espace et dans le temps.
     *
     * @param  coord Les coordonnées spatiales (horizontales) du
     *               point où extraire les valeurs.
     * @param  time  La date et heure du point où extraire les valeurs.
     * @param  dest  Un tableau dans lequel placer les valeurs, ou
     *               <code>null</code> pour laisser cette méthode
     *               créer un nouveau tableau.
     * @return Le tableau de valeurs.
     * @throws PointOutsideCoverageException si <code>coord</code>
     *         est en dehors des plages de coordonnées permises.
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
     * Retourne les valeurs en un point donné. Une interpolation
     * sera effectuée à la fois dans l'espace et dans le temps.
     *
     * @param  coord Les coordonnées spatio-temporelles du point
     *               où extraire les valeurs.
     * @param  dest  Un tableau dans lequel placer les valeurs, ou
     *               <code>null</code> pour laisser cette méthode
     *               créer un nouveau tableau.
     * @return Le tableau de valeurs.
     * @throws PointOutsideCoverageException si <code>coord</code>
     *         est en dehors des plages de coordonnées permises.
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
