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
import java.sql.SQLWarning;
import java.sql.SQLException;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Envelope;
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.ct.CoordinateTransform;

import net.seas.opengis.cv.Category;
import net.seas.opengis.cv.CategoryList;
import net.seas.opengis.gp.Operation;
import net.seas.opengis.gc.GridRange;
import net.seas.opengis.gc.GridGeometry;
import net.seas.opengis.gc.GridCoverage;
import net.seas.opengis.gp.GridCoverageProcessor;

// Images
import java.awt.image.RenderedImage;

// Entrés/sorties
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;

// Evénements
import javax.swing.event.EventListenerList;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;

// Geométrie
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;
import net.seas.util.XDimension2D;

// Références faibles
import net.seas.util.WeakHashSet;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;

// Divers
import java.util.Date;
import java.util.Calendar;
import java.util.Collections;
import net.seas.util.OpenGIS;
import net.seas.util.XClass;
import fr.ird.resources.Resources;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.util.CaselessStringKey;


/**
 * Information sur une image. Un objet <code>ImageEntry</code> correspond à
 * un enregistrement de la base de données d'images. Ces informations sont
 * retournées par la méthode {@link ImageTable#getEntries}.
 * <br><br>
 * Les objets <code>ImageEntry</code> sont imutables et sécuritaires dans un
 * environnement multi-threads.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ImageEntryImpl implements ImageEntry, Serializable
{
    /**
     * Compare deux entrées selon le même critère que celui qui a apparait dans
     * l'instruction "ORDER BY" dans la réquête SQL de {@link ImageTableImpl}).
     */
    boolean compare(final ImageEntryImpl other)
    {return endTime==other.endTime;}

    /**
     * Numéro de série (pour compatibilité avec des versions antérieures).
     */
    private static final long serialVersionUID = 135730397985915935L;

    /**
     * Les interpolations à appliquer sur les images retournées. D'abord, une
     * interpolation bicubique. Si elle échoue, une interpolation bilinéaire.
     * Si cette dernière échoue aussi, alors le plus proche voisin.
     */
    private static final String[] INTERPOLATIONS=
    {
        "Bicubic",
        "Bilinear",
        "NearestNeighbor"
    };

    /**
     * Clé à utiliser pour mémoriser cette entrée dans les
     * propriétés de l'objet {@link GridCoverage} retourné.
     */
    private static final CaselessStringKey ENTRY_KEY = new CaselessStringKey("Entry");

    /**
     * Ensemble des entrés qui ont déjà été retournées par {@link #intern()}
     * et qui n'ont pas encore été réclamées par le ramasse-miettes. La classe
     * {@link ImageTable} tentera autant que possible de retourner des entrées
     * qui existent déjà en mémoire afin de leur donner une chance de faire un
     * meilleur travail de cache sur les images.
     */
    private static final WeakHashSet<Object> pool = Table.pool;

    /**
     * Petite valeur utilisée pour contourner
     * les erreurs d'arrondissement.
     */
    private static final double EPS = 1E-6;

    /**
     * Largeur et hauteur minimale des images, en pixels. Si l'utilisateur
     * demande une région plus petite, la région demandée sera agradie pour
     * que l'image fasse cette taille.
     */
    private static final int MIN_SIZE = 8;

    /** Numéro identifiant l'image.      */ private final    int ID;
    /** Nom du fichier.                  */ private final String filename;
    /** Date du début de l'acquisition.  */ private final   long startTime;
    /** Date de la fin de l'acquisition. */ private final   long endTime;
    /** Longitude minimale.              */ private final  float xmin;
    /** Longitude maximale.              */ private final  float xmax;
    /** Latitude minimale.               */ private final  float ymin;
    /** Latitude maximale.               */ private final  float ymax;
    /** Nombre de pixels en largeur.     */ private final  short width;
    /** Nombre de pixels en hauteur.     */ private final  short height;

    /**
     * Bloc de paramètres de la table d'images. On retient ce bloc de paramètres
     * plutôt qu'une référence directe vers {@link ImageTable} afin de ne pas
     * empêcher le ramasse-miettes de détruire la table et ses connections vers
     * la base de données.
     */
    private final Parameters parameters;

    /**
     * Référence molle vers l'image {@link GridCoverage} qui a été retournée lors
     * du dernier appel de {@link #getImage}. Cette référence est retenue afin
     * d'éviter de charger inutilement une autre fois l'image si elle est déjà
     * en mémoire.
     */
    private transient Reference gridCoverage;

    /**
     * Référence molle vers l'image {@link RenderedImage} qui a été retournée lors
     * du dernier appel de {@link #getImage}. Cette référence est retenue afin
     * d'éviter de charger inutilement une autre fois l'image si elle est déjà
     * en mémoire.
     */
    private transient Reference renderedImage;

    /**
     * Construit une entré contenant des informations sur une image.
     *
     * @param  table  Table d'où proviennent les enregistrements.
     * @param  result Prochain enregistrement à lire.
     * @throws SQLException si l'accès à la base de données a échoué.
     */
    ImageEntryImpl(final ImageTableImpl table, final ResultSet result) throws SQLException
    {
        final int    seriesID;
        final int    formatID;
        final String pathname;
        final String ellipsoid; // TODO: pas encore utilisé.
        final Date   startTime;
        final Date     endTime;
        ID         = result.getInt      (ImageTableImpl.ID);
        seriesID   = result.getInt      (ImageTableImpl.SERIES);
        pathname   = result.getString   (ImageTableImpl.PATHNAME).intern();
        filename   = result.getString   (ImageTableImpl.FILENAME);
        startTime  = table .getTimestamp(ImageTableImpl.START_TIME, result);
        endTime    = table .getTimestamp(ImageTableImpl.END_TIME,   result);
        ellipsoid  = result.getString   (ImageTableImpl.ELLIPSOID);
        xmin       = result.getFloat    (ImageTableImpl.XMIN);
        xmax       = result.getFloat    (ImageTableImpl.XMAX);
        ymin       = result.getFloat    (ImageTableImpl.YMIN);
        ymax       = result.getFloat    (ImageTableImpl.YMAX);
        width      = result.getShort    (ImageTableImpl.WIDTH);
        height     = result.getShort    (ImageTableImpl.HEIGHT);
        formatID   = result.getInt      (ImageTableImpl.FORMAT);
        parameters = table .getParameters(seriesID, formatID, pathname, null);
        // TODO: le dernier argument (null) devrait être le système de coordonnées.
        this.startTime = (startTime!=null) ? startTime.getTime() : Long.MIN_VALUE;
        this.  endTime = (  endTime!=null) ?   endTime.getTime() : Long.MAX_VALUE;
    }

    /**
     * Retourne le numéro identifiant cette image dans la
     * base de données. Dans une même base de données,
     * chaque image porte un numéro unique.
     */
    public int getID()
    {return ID;}

    /**
     * Retourne la série à laquelle
     * appartient cette image.
     */
    public SeriesEntry getSeries()
    {return parameters.series;}

    /**
     * Retourne un nom désignant cette image. Le choix du nom est arbitraire,
     * mais il s'agira le plus souvent du nom du fichier (avec ou sans son
     * extension).
     */
    public String getName()
    {return filename;}

    /**
     * Retourne le nom complet du fichier
     * de l'image avec son chemin complet.
     */
    public File getFile()
    {
        final File file=new File(parameters.pathname, filename+'.'+parameters.format.extension);
        if (!file.isAbsolute())
        {
            final File directory = Table.directory;
            if (directory!=null)
            {
                return new File(directory, file.getPath());
            }
        }
        return file;
    }

    /**
     * Retourne des informations sur la géométrie de l'image. Ces informations
     * comprennent notamment la taille de l'image  (en pixels)    ainsi que la
     * transformation à utiliser pour passer des coordonnées pixels   vers les
     * coordonnées du système {@link #getCoordinateSystem}. Cette dernière sera
     * le plus souvent une transformation affine.
     */
    public GridGeometry getGridGeometry()
    {
        final GridRange gridRange = new GridRange(new int[3], new int[]{width,height,1});
        return new GridGeometry(gridRange, getEnvelope(), new boolean[]{false,true,false});
    }

    /**
     * Retourne le système de coordonnées de l'image. En général, ce système
     * de coordonnées aura trois dimensions  (la dernière dimension étant le
     * temps), soit dans l'ordre:
     * <ul>
     *   <li>Les longitudes, en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Les latitudes,  en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Le temps, en jours juliens depuis le 01/01/1950 00:00 UTC.</li>
     * </ul>
     */
    public CoordinateSystem getCoordinateSystem()
    {return parameters.coordinateSystem;}

    /**
     * Retourne les coordonnées spatio-temporelles de l'image. Le système de
     * coordonnées utilisé est celui retourné par {@link #getCoordinateSystem}.
     */
    public Envelope getEnvelope()
    {
        final double[] min = new double[] {xmin, ymin, ImageTableImpl.toJulian(startTime)};
        final double[] max = new double[] {xmax, ymax, ImageTableImpl.toJulian(  endTime)};
        return new Envelope(min, max);
    }

    /**
     * Retourne les coordonnées géographiques de la région couverte par l'image.
     * Les coordonnées seront exprimées en degrés de longitudes et de latitudes
     * selon l'ellipsoïde WGS 1984. Appeler cette méthode équivaut à n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et à transformer les
     * coordonnées si nécessaire.
     */
    public Rectangle2D getGeographicArea()
    {
        // No transformation needed for current implementation.
        return new Rectangle2D.Float(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    /**
     * Retourne la plage de temps couverte par l'image.   Cette plage sera délimitée
     * par des objets {@link Date}.  Appeler cette méthode équivant à n'extraire que
     * la partie temporelle de {@link #getEnvelope} et à transformer les coordonnées
     * si nécessaire.
     */
    public Range getTimeRange()
    {
        return new Range(Date.class,
                         (startTime!=Long.MIN_VALUE) ? new Date(startTime) : null, true,
                         (  endTime!=Long.MAX_VALUE) ? new Date(  endTime) : null, false);
    }

    /**
     * Retourne les listes de catégories pour toutes les bandes de l'image. Les objets
     * {@link CategoryList} indiquent comment interpréter les valeurs des pixels.  Par
     * exemple, ils peuvent indiquer que la valeur 9 désigne des nuages.
     *
     * @return La liste des catégories pour chaque bande de l'image.
     *         La longueur de ce tableau sera égale au nombre de bandes.
     */
    public CategoryList[] getCategoryLists()
    {return parameters.format.getCategoryLists(null);}

    /**
     * Retourne l'image correspondant à cette entrée.     Si l'image avait déjà été lue précédemment et qu'elle n'a pas
     * encore été réclamée par le ramasse-miette,   alors l'image existante sera retournée sans qu'une nouvelle lecture
     * du fichier ne soit nécessaire. Si au contraire l'image n'était pas déjà en mémoire, alors un décodage du fichier
     * sera nécessaire. Toutefois, cette méthode ne décodera pas nécessairement l'ensemble de l'image. Par défaut, elle
     * ne décode que la région qui avait été indiquée à {@link ImageTable#setEnvelope} et sous-échantillonne à la
     * résolution qui avait été indiquée à {@link ImageTable#setPreferredResolution} (<strong>note:</strong> cette région
     * et ce sous-échantillonage sont ceux qui étaient actifs au moment où {@link ImageTable#getEntries} a été appelée;
     * les changement subséquents des paramètres de {@link ImageTable} n'ont pas d'effets sur les <code>ImageEntry</code>
     * déjà créés).
     *
     * @param  listenerList Liste des objets à informer des progrès de la lecture ainsi que des éventuels avertissements,
     *         ou <code>null</code> s'il n'y en a pas. Cette méthode prend en compte tous les objets qui ont été inscrits
     *         sous la classe {@link IIOReadWarningListener} ou {@link IIOReadProgressListener}, et ignore tous les autres.
     *         Cette méthode s'engage à ne pas modifier l'objet {@link EventListenerList} donné; il est donc sécuritaire de
     *         passer directement la liste {@link javax.swing.JComponent#listenerList} d'une interface utilisateur, même
     *         dans un environnement multi-threads. Un objet {@link EventListenerList} peut aussi être construit comme suit:
     *         <blockquote><pre>
     *         {@link IIOReadProgressListener} progressListener = ...
     *         {@link IIOReadWarningListener}   warningListener = ...
     *         {@link EventListenerList}  listenerList = new EventListenerList();
     *         listenerList.add(IIOReadProgressListener.class, progressListener);
     *         listenerList.add(IIOReadWarningListener.class,   warningListener);
     *         </pre></blockquote>
     *
     * @return Image lue, ou <code>null</code> si l'image n'intercepte pas la région géographique ou la plage de temps
     *         qui avaient été spécifiées à {@link ImageTable}, ou si l'utilisateur a interrompu la lecture.
     * @throws IOException si le fichier n'a pas été trouvé ou si une autre erreur d'entrés/sorties est survenue.
     * @throws IIOException s'il n'y a pas de décodeur approprié pour l'image, ou si l'image n'est pas valide.
     */
    public synchronized GridCoverage getImage(final EventListenerList listenerList) throws IOException
    {
        /*
         * NOTE SUR LES SYNCHRONISATIONS: Cette méthode est synchronisée à plusieurs niveau:
         *
         *  1) Toute la méthode sur 'this',  afin d'éviter qu'une image ne soit lue deux fois
         *     si un thread tente d'accéder à la cache alors que l'autre thread n'a pas eu le
         *     temps de placer le résultat de la lecture dans cette cache.   Synchroniser sur
         *     'this' ne devrait pas avoir d'impact significatif sur la performance,    étant
         *     donné que l'opération vraiment longue (la lecture de l'image) est synchronisée
         *     sur 'format' de toute façon (voir prochain item).
         *
         *  2) La lecture de l'image sur 'format'. On ne synchronise pas toute la méthode sur
         *     'format' afin de ne pas bloquer l'accès à la cache  pour un objet 'ImageEntry'
         *     donné pendant qu'une lecture est en cours  sur un autre objet 'ImageEntry' qui
         *     utiliserait le même format.
         *
         *  3) Les demandes d'annulation de lecture ({@link #abort}) sur
         *     <code>FormatEntry.getAbortLock()</code>, afine de pouvoir
         *     être faite pendant qu'une lecture est en cours. Cette
         *     synchronisation est gérée en interne par <code>FormatEntry</code>.
         */

        // TODO: si on permet d'obtenir des images à différents index, il
        //  faudra en tenir compte dans 'gridCoverage' et 'renderedImage'.
        final int imageIndex = 0;
        /*
         * Vérifie d'abord si l'image demandée se trouve déjà en mémoire. Si
         * oui, elle sera retournée et la méthode se termine immédiatement.
         */
        if (gridCoverage!=null)
        {
            final GridCoverage image = (GridCoverage) gridCoverage.get();
            if (image!=null) return image;
        }
        gridCoverage=null;
        /*
         * Obtient les coordonnées géographiques et la résolution désirées.
         * La classe <code>Parameters</code> a déjà projeté ces coordonnées
         * selon le système de l'image, si c'était nécessaire.
         */
        final Rectangle2D clipArea   = parameters.geographicArea;
        final Dimension2D resolution = parameters.resolution;
        /*
         * Procède à la lecture de l'image correspondant à cette entrée.   Si l'image n'intercepte pas le rectangle
         * <code>clipArea</code> spécifié ou si l'utilisateur a interrompu la lecture, alors cette méthode retourne
         * <code>null</code>. Les coordonnées de la région couverte par l'image retournée peuvent ne pas être
         * identiques aux coordonnées spécifiées. La méthode {@link GridCoverage#getEnvelope} permettra de
         * connaître les coordonnées exactes.
         *
         * @param  clipArea Coordonnées géographiques de la région désirée, ou <code>null</code> pour prendre
         *         l'image au complet. Les coordonnées doivent être exprimées selon le système de coordonnées
         *         de l'image, tel que retourné par {@link #getCoordinateSystem}. Ce n'est pas nécessairement
         *         le même système de coordonnées que {@link ImageTable}, quoique ce soit souvent le cas.
         * @param  resolution Dimension logique désirée des pixels de l'image, ou <code>null</code> pour
         *         demander la meilleure résolution possible. Les dimensions doivent être exprimées selon
         *         le système de coordonnées de l'image, tel que retourné par {@link #getCoordinateSystem}.
         *         Cette information n'est qu'approximative; Il n'est pas garantie que la lecture produira
         *         effectivement une image de la résolution demandée.
         */
        Rectangle2D clipLogical = new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
        Rectangle   clipPixel   = null;
        final int xSubsampling;
        final int ySubsampling;
        if (resolution!=null)
        {
            /*
             * Conversion [résolution logique désirée] --> [fréquence d'échantillonage des pixels].
             */
            xSubsampling = Math.max(1, Math.min(width >>8, (int)Math.round(width  * (resolution.getWidth () / clipLogical.getWidth ()))));
            ySubsampling = Math.max(1, Math.min(height>>8, (int)Math.round(height * (resolution.getHeight() / clipLogical.getHeight()))));
        }
        else
        {
            xSubsampling = 1;
            ySubsampling = 1;
        }
        if (clipArea!=null)
        {
            /*
             * Vérifie si le rectangle demandé (clipArea) intercepte la région géographique couverte par l'image.
             * On utilise un code spécial plutôt que de faire appel à {@link Rectangle2D#intersects} parce qu'on
             * veut accepter les cas où le rectangle demandé se résume à une ligne ou un point.
             */
            if (clipArea.getWidth()<0 || clipArea.getHeight()<0 || clipLogical.isEmpty()) return null;
            if (clipArea.getMaxX()<clipLogical.getMinX() ||
                clipArea.getMinX()>clipLogical.getMaxX() ||
                clipArea.getMaxY()<clipLogical.getMinY() ||
                clipArea.getMinY()>clipLogical.getMaxY()) return null;
            Rectangle2D.intersect(clipLogical, clipArea, clipLogical);
            /*
             * Conversion [coordonnées logiques] --> [coordonnées pixels].
             */
            final double scaleX =  width/(xmax-xmin);
            final double scaleY = height/(ymax-ymin);
            clipPixel=new Rectangle((int)Math.floor(scaleX*(clipLogical.getMinX()-xmin)+EPS),
                                    (int)Math.floor(scaleY*(ymax-clipLogical.getMaxY())+EPS),
                                    (int)Math.ceil (scaleX*clipLogical.getWidth()      -EPS),
                                    (int)Math.ceil (scaleY*clipLogical.getHeight()     -EPS));
            if (clipPixel.width < MIN_SIZE)
            {
                clipPixel.x    -= (MIN_SIZE-clipPixel.width)/2;
                clipPixel.width = MIN_SIZE;
            }
            if (clipPixel.height < MIN_SIZE)
            {
                clipPixel.y     -= (MIN_SIZE-clipPixel.height)/2;
                clipPixel.height = MIN_SIZE;
            }
            /*
             * Vérifie que les coordonnées obtenues sont bien
             * dans les limites de la dimension de l'image.
             */
            final int clipX2 = Math.min(this.width,  clipPixel.width  + clipPixel.x);
            final int clipY2 = Math.min(this.height, clipPixel.height + clipPixel.y);
            if (clipPixel.x < 0) clipPixel.x = 0;
            if (clipPixel.y < 0) clipPixel.y = 0;
            clipPixel.width  = clipX2-clipPixel.x;
            clipPixel.height = clipY2-clipPixel.y;
            /*
             * Vérifie que la largeur du rectangle est un
             * multiple entier de la fréquence d'échantillonage.
             */
            clipPixel.width  = (clipPixel.width /xSubsampling) * xSubsampling;
            clipPixel.height = (clipPixel.height/ySubsampling) * ySubsampling;
            if (clipPixel.isEmpty()) return null;
            /*
             * Conversion [coordonnées pixels] --> [coordonnées logiques].
             *
             * 'clipLogical' ne devrait pas beaucoup changer (mais parfois un peu).
             */
            clipLogical.setRect(xmin + clipPixel.getMinX()  /scaleX,
                                ymax - clipPixel.getMaxY()  /scaleY,
                                       clipPixel.getWidth() /scaleX,
                                       clipPixel.getHeight()/scaleY);
        }
        /*
         * Avant d'effectuer la lecture, vérifie si l'image est déjà en mémoire. Une image
         * {@link RenderedImage} peut être en mémoire même si {@link GridCoverage} ne l'est
         * plus si, par exemple, l'image est entrée dans une chaîne d'opérations de JAI.
         */
        RenderedImage image=null;
        if (renderedImage!=null)
        {
            image = (RenderedImage) renderedImage.get();
            if (image==null) renderedImage=null;
        }
        /*
         * A ce stade, nous disposons maintenant des coordonnées en pixels
         * de la région à charger. Procède maintenant à la lecture.
         */
        final FormatEntry format = parameters.format;
        final CategoryList[] categoryLists;
        synchronized (format)
        {
            final ImageReadParam param = format.getDefaultReadParam();
            if (clipPixel!=null) param.setSourceRegion(clipPixel);
            param.setSourceSubsampling(xSubsampling, ySubsampling, xSubsampling>>1, ySubsampling>>1);
            if (image==null)
            {
                image=format.read(getFile(), imageIndex, param, listenerList, new Dimension(width, height), this);
                if (image==null) return null;
            }
            categoryLists = format.getCategoryLists(param);
        }
        /*
         * La lecture est maintenant terminée et n'a pas été annulée.
         * On construit maintenant l'objet {@link GridCoverage}, on le
         * conserve dans une cache interne puis on le retourne.
         */
        final double[] min = new double[] {clipLogical.getMinX(), clipLogical.getMinY(), ImageTableImpl.toJulian(startTime)};
        final double[] max = new double[] {clipLogical.getMaxX(), clipLogical.getMaxY(), ImageTableImpl.toJulian(  endTime)};
        GridCoverage coverage = new GridCoverage(filename, image, parameters.coordinateSystem,
                                new Envelope(min, max), categoryLists, format.geophysics, null,
                                Collections.singletonMap(ENTRY_KEY, this));
        /*
         * Si l'utilisateur a spécifié une operation à appliquer
         * sur les images, applique cette opération maintenant.
         */
        GridCoverageProcessor processor = parameters.PROCESSOR;
        Operation             operation = parameters.operation;
        if (operation!=null)
        {
            coverage = processor.doOperation(operation, operation.getParameterList().setParameter("Source", coverage));
        }
        /*
         * Applique l'interpolation bicubique, conserve le
         * résultat dans une cache et retourne le résultat.
         */
        operation = processor.getOperation("Interpolate");
        coverage  = processor.doOperation(operation, operation.getParameterList().setParameter("Source", coverage).setParameter("Type", INTERPOLATIONS));
        renderedImage = new WeakReference(image);
        gridCoverage  = new SoftReference(coverage);
        return coverage;
    }

    /**
     * Annule la lecture de l'image. Cette méthode peut être appelée à partir de n'importe quel
     * thread. Si la méthode {@link #getImage} était en train de lire une image dans un autre
     * thread, elle s'arrêtera et retournera <code>null</code>.
     */
    public void abort()
    {parameters.format.abort(this);}

    /**
     * Retourne une chaîne de caractères représentant cette entrée.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(40);
        buffer.append("ImageEntry"); // Pour ne pas avoir le "Impl" à la fin...
        buffer.append('[');
        buffer.append(getName());
        if (startTime!=Long.MIN_VALUE && endTime!=Long.MAX_VALUE)
        {
            buffer.append(" (");
            buffer.append(parameters.format(new Date((startTime+endTime)/2)));
            buffer.append(')');
        }
        buffer.append(' ');
        buffer.append(OpenGIS.toWGS84String(parameters.coordinateSystem.getHeadCS(), getGeographicArea()));
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Indique si cette entrée est identique à l'entrée spécifiée. Cette méthode
     * vérifie tous les paramètres de <code>ImageEntry</code>, incluant le chemin
     * de l'image et les coordonnées géographiques de la région qui a été demandée.
     * Si vous souhaitez seulement vérifier si deux objets <code>ImageEntry</code>
     * décrivent bien la même image (même si les coordonnées de la région demandée
     * sont différentes), comparez plutôt leur numéros {@link #getID}. Notez que
     * cette dernière solution n'est valide que si les deux objets <code>ImageEntry</code>
     * proviennent bien de la même base de données.
     */
    public boolean equals(final Object o)
    {return (o instanceof ImageEntryImpl) && equalsStrict((ImageEntryImpl) o);}

    /**
     * Indique si cette entrée est strictement égale à l'entrée spécifiée. Tous
     * les champs sont pris en compte, y compris ceux qui ne proviennent pas de
     * la base de données (comme les coordonnées de la région désirée par
     * l'utilisateur).
     */
    private boolean equalsStrict(final ImageEntryImpl that)
    {
        return          this.ID       == that.ID          &&
          XClass.equals(this.filename,   that.filename  ) &&
          XClass.equals(this.parameters, that.parameters) &&
          sameSize(that) && sameCoordinates(that);
    }

    /**
     * Indique si l'image de cette entrée couvre la
     * même région géographique et la même plage de
     * temps que celles de l'entré spécifiée.   Les
     * deux entrés peuvent toutefois appartenir à
     * des séries différentes.
     */
    private boolean sameCoordinates(final ImageEntryImpl that)
    {
        return this.startTime == that.startTime && this.endTime == that.endTime   &&
               Float.floatToIntBits(this.xmin) == Float.floatToIntBits(that.xmin) &&
               Float.floatToIntBits(this.xmax) == Float.floatToIntBits(that.xmax) &&
               Float.floatToIntBits(this.ymin) == Float.floatToIntBits(that.ymin) &&
               Float.floatToIntBits(this.ymax) == Float.floatToIntBits(that.ymax) &&
               parameters.coordinateSystem.equivalents(that.parameters.coordinateSystem);
    }

    /**
     * Indique si l'image de cette entrée a la même dimension que l'image
     * spécifiée. Cette méthode ne vérifie pas si les deux images couvrent
     * la même région géographique.
     */
    private boolean sameSize(final ImageEntryImpl that)
    {return (this.width==that.width) && (this.height==that.height);}

    /**
     * Retourne un code représentant cette entrée.
     */
    public int hashCode()
    {return ID;}

    /**
     * Après la lecture binaire, vérifie si
     * l'entrée lue existait déjà en mémoire.
     */
    private Object readResolve() throws ObjectStreamException
    {return intern();}

    /**
     * Retourne un exemplaire unique de cette entrée. Une banque d'entrées, initialement
     * vide, est maintenue de façon interne par la classe <code>ImageEntry</code>. Lorsque la
     * méthode <code>intern</code> est appelée, elle recherchera des entrées égales à
     * <code>this</code> au sens de la méthode {@link #equals}. Si de telles entrées
     * sont trouvées, elles seront retournées. Sinon, les entrées <code>this</code>
     * seront ajoutées à la banque de données en utilisant une référence faible
     * et cette méthode retournera <code>this</code>.
     * <br><br>
     * De cette méthode il s'ensuit que pour deux entrées <var>u</var> et <var>v</var>,
     * la condition <code>u.intern()==v.intern()</code> sera vrai si et seulement si
     * <code>u.equals(v)</code> est vrai.
     */
    final ImageEntryImpl intern()
    {return (ImageEntryImpl) pool.intern(this);}

    /**
     * Applique {@link #intern()} sur un tableau d'entrées.
     * Ce tableau peut contenir des éléments nuls.
     */
    static void intern(final ImageEntry[] entries)
    {pool.intern(entries);}

    /**
     * Si les deux images couvrent les mêmes coordonnées spatio-temporelles,
     * retourne celle qui a la plus basse résolution. Si les deux images ne
     * couvrent pas les mêmes coordonnées ou si leurs résolutions sont
     * incompatibles, alors cette méthode retourne <code>null</code>.
     */
    final ImageEntryImpl getLowestResolution(final ImageEntryImpl that)
    {
        if (XClass.equals(this.parameters.series, that.parameters.series) && sameCoordinates(that))
        {
            if (this.width<=that.width && this.height<=that.height) return this;
            if (this.width>=that.width && this.height>=that.height) return that;
        }
        return null;
    }

    /**
     * Indique si cette image a au moins la résolution spécifiée.
     *
     * @param  resolution   Résolution désirée, exprimée selon le système de coordonnées
     *                      spécifié. La conversion vers le système de coordonnées de
     *                      l'image sera faite automatiquement.
     * @param  sourceCS     Système de coordonnées de <code>resolution</code>.
     * @return <code>true</code> si la résolution de cette image est égale ou supérieure à la
     *         résolution demandée. Cette méthode retourne <code>false</code> si <code>resolution</code>
     *         était nul ou si une projection cartographique a échouée.
     */
    final boolean hasEnoughResolution(final Dimension2D resolution, final CoordinateSystem sourceCS)
    {
        if (resolution!=null)
        {
            double  width  = resolution.getWidth();
            double  height = resolution.getHeight();
            final float dx = (xmax-xmin);
            final float dy = (ymax-ymin);
            final CoordinateSystem targetCS = getCoordinateSystem();
            if (!sourceCS.equivalents(targetCS))
            {
                throw new UnsupportedOperationException(); // Not implemented
            }
            if ((1+EPS)*width  >= dx/this.width &&
                (1+EPS)*height >= dy/this.height)
            {
                return true;
            }
        }
        return false;
    }
}
