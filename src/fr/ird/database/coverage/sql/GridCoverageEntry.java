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

// Base de données
import java.sql.ResultSet;
import java.sql.SQLWarning;
import java.sql.SQLException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// Entrés/sorties
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.net.URL;
import java.net.MalformedURLException;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.swing.event.EventListenerList;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;

// Geométrie
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;

// Références faibles
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.ref.SoftReference;

// Divers
import java.util.Date;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Collections;
import java.awt.image.RenderedImage;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.util.CaselessStringKey;

// OpenGIS
import org.opengis.referencing.operation.TransformException;

// Geotools (CTS)
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.CoordinateTransformationFactory;

// Geotools (GCS)
import org.geotools.cv.Category;
import org.geotools.cv.SampleDimension;
import org.geotools.gc.GridRange;
import org.geotools.gc.GridGeometry;
import org.geotools.gc.GridCoverage;
import org.geotools.gp.Operation;
import org.geotools.gp.GridCoverageProcessor;

// Geotools (resources)
import org.geotools.util.WeakHashSet;
import org.geotools.resources.XArray;
import org.geotools.resources.Utilities;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.geometry.XDimension2D;
import org.geotools.resources.geometry.XRectangle2D;

// Seagis
import fr.ird.database.CatalogException;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.Resources;


/**
 * Information sur une image. Un objet <code>GridCoverageEntry</code> correspond à
 * un enregistrement de la base de données d'images. Ces informations sont
 * retournées par la méthode {@link GridCoverageTable#getEntries}.
 * <br><br>
 * Les objets <code>GridCoverageEntry</code> sont imutables et sécuritaires dans un
 * environnement multi-threads.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class GridCoverageEntry extends UnicastRemoteObject implements CoverageEntry {
    /**
     * Clé sous laquelle mémoriser l'objet {@link CoverageEntry}
     * source dans les propriétés de {@link GridCoverage}.
     */
    private static final CaselessStringKey SOURCE_KEY =
            new CaselessStringKey(CoverageEntry.SOURCE_KEY);

    /**
     * Compare deux entrées selon le même critère que celui qui a apparait dans
     * l'instruction "ORDER BY" dans la réquête SQL de {@link GridCoverageTable}).
     * Les entrés sans dates sont une exception: elles sont considérées comme non-ordonnées.
     */
    boolean compare(final GridCoverageEntry other) {
        if (startTime==Long.MIN_VALUE && endTime==Long.MAX_VALUE) {
            return false;
        }
        return endTime == other.endTime;
    }

    /**
     * Numéro de série (pour compatibilité avec des versions antérieures).
     */
    private static final long serialVersionUID = 135730397985915935L;

    /**
     * Les interpolations à appliquer sur les images retournées. D'abord, une
     * interpolation bicubique. Si elle échoue, une interpolation bilinéaire.
     * Si cette dernière échoue aussi, alors le plus proche voisin.
     */
    private static final String[] INTERPOLATIONS = {
        "Bicubic",
        "Bilinear",
        "NearestNeighbor"
    };

    /**
     * Objet à utiliser par défaut pour construire des transformations de coordonnées.
     */
    private static final CoordinateTransformationFactory TRANSFORMS =
                         CoordinateTransformationFactory.getDefault();

    /**
     * L'objet à utiliser pour appliquer des opérations sur les images lues.
     *
     * @see CoverageDataBase#getDefaultGridCoverageProcessor
     * @see CoverageDataBase#setDefaultGridCoverageProcessor
     */
    static GridCoverageProcessor PROCESSOR = new fr.ird.database.coverage.sql.GridCoverageProcessor();

    /**
     * Ensemble des entrés qui ont déjà été retournées par {@link #canonicalize()}
     * et qui n'ont pas encore été réclamées par le ramasse-miettes. La classe
     * {@link GridCoverageTable} tentera autant que possible de retourner des entrées
     * qui existent déjà en mémoire afin de leur donner une chance de faire un
     * meilleur travail de cache sur les images.
     */
    private static final WeakHashSet POOL = Table.POOL;
    
    /**
     * Liste des derniers {@link GridCoverageEntry} pour lesquels la méthode
     * {@link #getGridCoverage} a été appelée. Lorsqu'une nouvelle image est lue, les références
     * molles des plus anciennes sont changées en références faibles afin d'augmenter les chances
     * que le ramasse-miette se débarasse des images les plus anciennes avant que la mémoire ne
     * sature.
     */
    private static final LinkedList<GridCoverageEntry> LAST_INVOKED = new LinkedList<GridCoverageEntry>();

    /**
     * Nombre maximal d'entrés à conserver dans la liste {@link #LAST_INVOKED}.
     *
     * @task TODO: Une meilleure mesure serait la mémoire occupée...
     */
    private static final int ENTRY_CAPACITY = 8;

    /**
     * Petite valeur utilisée pour contourner les erreurs d'arrondissement.
     */
    private static final double EPS = 1E-6;

    /**
     * Largeur et hauteur minimale des images, en pixels. Si l'utilisateur
     * demande une région plus petite, la région demandée sera agradie pour
     * que l'image fasse cette taille.
     */
    private static final int MIN_SIZE = 8;

    /** Nom du fichier.                  */ private final String filename;
    /** Date du début de l'acquisition.  */ private final   long startTime;
    /** Date de la fin de l'acquisition. */ private final   long endTime;
    /** Longitude minimale.              */         final  float xmin;
    /** Longitude maximale.              */         final  float xmax;
    /** Latitude minimale.               */         final  float ymin;
    /** Latitude maximale.               */         final  float ymax;
    /** Nombre de pixels en largeur.     */ private final  short width;
    /** Nombre de pixels en hauteur.     */ private final  short height;

    /**
     * Bloc de paramètres de la table d'images. On retient ce bloc de paramètres
     * plutôt qu'une référence directe vers {@link GridCoverageTable} afin de ne
     * pas empêcher le ramasse-miettes de détruire la table et ses connections
     * vers la base de données.
     */
    private final Parameters parameters;

    /**
     * Référence molle vers l'image {@link GridCoverage} qui a été retournée lors
     * du dernier appel de {@link #getGridCoverage}.  Cette référence est retenue
     * afin d'éviter de charger inutilement une autre fois l'image si elle est déjà
     * en mémoire.
     */
    private transient Reference<GridCoverage> gridCoverage;

    /**
     * Référence molle vers l'image {@link RenderedImage} qui a été retournée lors
     * du dernier appel de {@link #getGridCoverage}.   Cette référence est retenue
     * afin d'éviter de charger inutilement une autre fois l'image si elle est déjà
     * en mémoire.
     */
    private transient Reference<RenderedImage> renderedImage;

    /**
     * Construit une entré contenant des informations sur une image.
     *
     * @param  table  Table d'où proviennent les enregistrements.
     * @param  result Prochain enregistrement à lire.
     * @throws SQLException si l'accès au catalogue a échoué.
     */
    GridCoverageEntry(final GridCoverageTable table, final ResultSet result)
            throws RemoteException, SQLException
    {
        final String seriesID;
        final String formatID;
        final String crsID;
        final String pathname;
        final Date   startTime;
        final Date     endTime;
        seriesID   = result.getString    (GridCoverageTable.SERIES);
        pathname   = result.getString    (GridCoverageTable.PATHNAME).intern();
        filename   = result.getString    (GridCoverageTable.FILENAME);
        startTime  = table .getTimestamp (GridCoverageTable.START_TIME, result);
        endTime    = table .getTimestamp (GridCoverageTable.END_TIME,   result);
        xmin       = result.getFloat     (GridCoverageTable.XMIN);
        xmax       = result.getFloat     (GridCoverageTable.XMAX);
        ymin       = result.getFloat     (GridCoverageTable.YMIN);
        ymax       = result.getFloat     (GridCoverageTable.YMAX);
        width      = result.getShort     (GridCoverageTable.WIDTH);
        height     = result.getShort     (GridCoverageTable.HEIGHT);
        crsID      = result.getString    (GridCoverageTable.CRS);
        formatID   = result.getString    (GridCoverageTable.FORMAT);
        parameters = table .getParameters(seriesID, formatID, crsID, pathname);
        // TODO: mémoriser les coordonnées dans un Rectangle2D et lancer une exception s'il est vide.
        // NOTE: Les coordonnées xmin, xmax, ymin et ymax ne sont PAS exprimées selon le système de
        //       coordonnées de l'image, mais plutôt selon le système de coordonnées de la table
        //       d'images. La transformation sera effectuée par 'getEnvelope()'.
        this.startTime = (startTime!=null) ? startTime.getTime() : Long.MIN_VALUE;
        this.  endTime = (  endTime!=null) ?   endTime.getTime() : Long.MAX_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    public fr.ird.database.coverage.SeriesEntry getSeries() {
        return parameters.series;
    }

    /**
     * Retourne un nom désignant cette image. Le choix du nom est arbitraire,
     * mais il s'agira le plus souvent du nom du fichier (avec ou sans son
     * extension).
     */
    public String getName() {
        return filename;
    }

    /**
     * Retourne <code>null</code>, étant donné que les images ne sont pas
     * accompagnées de description.
     */
    public String getRemarks() {
        return null;
    }

    /**
     * Returns the source as a {@link File} or an {@link URL}, in this preference order.
     *
     * @param  local <code>true</code> if the file are going to be read from a local machine,
     *         or <code>false</code> if it is going to be read through internet.
     * @return The input, usually a {@link File} object if <code>local</code> was
     *         <code>true</code> and an {@link URL} object if <code>local</code> was
     *         <code>false</code>.
     */
    private Object getInput(final boolean local) throws MalformedURLException {
        final File file = new File(parameters.pathname, filename+'.'+parameters.format.extension);
        if (!file.isAbsolute()) {
            if (local) {
                if (parameters.rootDirectory != null) {
                    return new File(parameters.rootDirectory, file.getPath());
                }
            }
            if (parameters.rootURL != null) {
                String path = file.getPath().replace(File.separatorChar, '/');
                final int lg = parameters.rootURL.length();
                if (lg != 0) {
                    final StringBuilder buffer = new StringBuilder(parameters.rootURL);
                    if (buffer.charAt(lg-1) != '/') {
                        buffer.append('/');
                    }
                    buffer.append(path);
                    path = buffer.toString();
                }
                return new URL(path);
            }
        }
        return (local) ? file : file.toURL();
    }

    /**
     * {@inheritDoc}
     */
    public File getFile() {
        try {
            final Object input = getInput(true);
            if (input instanceof File) {
                return (File) input;
            }
        } catch (MalformedURLException exception) {
            Utilities.unexpectedException(CoverageDataBase.LOGGER.getName(),
                                 "GridCoverageEntry", "getFile", exception);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public URL getURL() {
        try {
            final Object input = getInput(false);
            if (input instanceof URL) {
                return (URL) input;
            }
        } catch (MalformedURLException exception) {
            Utilities.unexpectedException(CoverageDataBase.LOGGER.getName(),
                                 "GridCoverageEntry", "getFile", exception);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public GridGeometry getGridGeometry() {
        final GridRange gridRange = new GridRange(new int[3], new int[]{width,height,1});
        return new GridGeometry(gridRange, getEnvelope(), new boolean[]{false,true,false});
    }

    /**
     * {@inheritDoc}
     */
    public CoordinateSystem getCoordinateSystem() {
        return parameters.imageCS;
    }

    /**
     * {@inheritDoc}
     */
    public Envelope getEnvelope() {
        try {
            final Rectangle2D area = tableToCoverageCS(new XRectangle2D(xmin, ymin, xmax-xmin, ymax-ymin));
            final double[] min = new double[] {area.getMinX(), area.getMinY(), CoordinateSystemTable.toJulian(startTime)};
            final double[] max = new double[] {area.getMaxX(), area.getMaxY(), CoordinateSystemTable.toJulian(  endTime)};
            return new Envelope(min, max);
        } catch (TransformException exception) {
            // Should not happen if the coordinate in the database are valids.
            final IllegalStateException e = new IllegalStateException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Projète la table spécifiée du système de coordonnées de la table vers le système
     * de coordonnées de l'image.
     */
    private Rectangle2D tableToCoverageCS(Rectangle2D area) throws TransformException {
        CoordinateSystem sourceCS = parameters.tableCS;
        CoordinateSystem targetCS = parameters.imageCS;
        if (sourceCS != targetCS) {
            sourceCS = CTSUtilities.getCoordinateSystem2D(sourceCS);
            targetCS = CTSUtilities.getCoordinateSystem2D(targetCS);
            area = CTSUtilities.transform((MathTransform2D)
                   TRANSFORMS.createFromCoordinateSystems(sourceCS, targetCS).getMathTransform(),
                   area, area);
        }
        return area;
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle2D getGeographicArea() {
        return new Rectangle2D.Float(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    /**
     * {@inheritDoc}
     */
    public Range getTimeRange() {
        return new Range(Date.class, getStartTime(), true, getEndTime(), false);
    }

    /**
     * Retourne la date de début d'échantillonage de l'image,
     * ou <code>null</code> si elle n'est pas connue.
     */
    public Date getStartTime() {
        return (startTime!=Long.MIN_VALUE) ? new Date(startTime) : null;
    }

    /**
     * Retourne la date de fin d'échantillonage de l'image,
     * ou <code>null</code> si elle n'est pas connue.
     */
    public Date getEndTime() {
        return (endTime!=Long.MAX_VALUE) ? new Date(endTime) : null;
    }

    /**
     * Palette de couleurs pour le bricolage temporaire dans {@link #getSampleDimensions}.
     * A supprimer si on trouve une meilleur solution.
     */
    private static final SampleDimension SAMPLE_DIMENSION_HACK = new SampleDimension(
            new org.geotools.cv.Category[] {org.geotools.cv.Category.NODATA,
                new org.geotools.cv.Category("Gradient", new java.awt.Color[] {
                                                             java.awt.Color.DARK_GRAY,
                                                             java.awt.Color.LIGHT_GRAY},
                                                             1, 256, 0.5, -0.5)}, null);

    /**
     * {@inheritDoc}
     *
     * @task TODO: If faudrait trouver un moyen de déterminer la plage de valeurs lorsqu'une
     *             opération est appliquée. Peut-être avec OperationJAI.deriveSampleDimensions?
     */
    public SampleDimension[] getSampleDimensions() {
        final SampleDimension[] bands = parameters.format.getSampleDimensions();

        // HACK BEGIN
        final Operation operation = parameters.operation;
        if (operation != null) {
            // Note: le nom peut apparaître n'importe où (ex: "NodataFilter;GradientMagnitude").
            if (operation.getName().indexOf("GradientMagnitude") >= 0) {
                java.util.Arrays.fill(bands, SAMPLE_DIMENSION_HACK);
            }
        }
        // HACK END

        for (int i=0; i<bands.length; i++) {
            bands[i] = bands[i].geophysics(true);
        }
        return bands;
    }

    /**
     * {@inheritDoc}
     */
    public GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException {
        try {
            return getGridCoverage(0, listenerList);
        } catch (TransformException exception) {
            throw new IIOException(exception.getLocalizedMessage(), exception);
        }
    }

    /**
     * Procède à la lecture d'une image à l'index spécifié.
     *
     * @param imageIndex Index de l'image à lire.
     *        NOTE: si on permet d'obtenir des images à différents index, il faudra en
     *              tenir compte dans {@link #gridCoverage} et {@link #renderedImage}.
     * @param listenerList Liste des objets à informer des progrès de la lecture.
     */
    private synchronized GridCoverage getGridCoverage(final int imageIndex,
                                                      final EventListenerList listenerList)
            throws IOException, TransformException
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
         *     'format' afin de ne pas bloquer l'accès à la cache  pour un objet 'CoverageEntry'
         *     donné pendant qu'une lecture est en cours  sur un autre objet 'CoverageEntry' qui
         *     utiliserait le même format.
         *
         *  3) Les demandes d'annulation de lecture ({@link #abort}) sur
         *     <code>FormatEntryImpl.enqueued</code>, afin de pouvoir
         *     être faite pendant qu'une lecture est en cours. Cette
         *     synchronisation est gérée en interne par <code>FormatEntryImpl</code>.
         */

        /*
         * Vérifie d'abord si l'image demandée se trouve déjà en mémoire. Si
         * oui, elle sera retournée et la méthode se termine immédiatement.
         */
        if (gridCoverage != null) {
            final GridCoverage image = gridCoverage.get();
            if (image != null) {
                return image;
            }
        }
        gridCoverage = null;
        /*
         * Obtient les coordonnées géographiques et la résolution désirées. Notez que ces
         * rectangles ne sont pas encore exprimées dans le système de coordonnées de l'image.
         * Cette projection sera effectuée par 'tableToCoverageCS(...)' seulement après avoir
         * pris en compte le clip. Ca nous évite d'avoir à projeter le clip, ce qui aurait été
         * problématique avec les projections qui n'ont pas un domaine de validité suffisament
         * grand (par exemple jusqu'aux pôles).
         */
        final Rectangle2D clipArea   = parameters.geographicArea;
        final Dimension2D resolution = parameters.resolution;
        /*
         * Procède à la lecture de l'image correspondant à cette entrée. Si l'image n'intercepte
         * pas le rectangle <code>clipArea</code> spécifié ou si l'utilisateur a interrompu la
         * lecture, alors cette méthode retourne <code>null</code>. Les coordonnées de la région
         * couverte par l'image retournée peuvent ne pas être identiques aux coordonnées spécifiées.
         * La méthode {@link GridCoverage#getEnvelope} permettra de connaître les coordonnées exactes.
         *
         * @param  clipArea Coordonnées géographiques de la région désirée, ou <code>null</code>
         *         pour prendre l'image au complet. Les coordonnées doivent être exprimées selon
         *         le système de coordonnées de la table d'images, tel que retourné par {@link
         *         CoverageTable#getCoordinateSystem}. Ce n'est pas nécessairement le même
         *         système de coordonnées que celui de l'image à lire.
         * @param  resolution Dimension logique désirée des pixels de l'image, ou <code>null</code>
         *         pour demander la meilleure résolution possible. Les dimensions doivent être
         *         exprimées selon le même système de coordonnées que <code>area</code>. Cette
         *         information n'est qu'approximative; Il n'est pas garantie que la lecture
         *         produira effectivement une image de la résolution demandée.
         */
        Rectangle2D clipLogical = new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
        Rectangle   clipPixel   = null;
        final int xSubsampling;
        final int ySubsampling;
        if (resolution != null) {
            /*
             * Conversion [résolution logique désirée] --> [fréquence d'échantillonage des pixels].
             */
            xSubsampling = Math.max(1, Math.min(width >>8, (int)Math.round(width  * (resolution.getWidth () / clipLogical.getWidth ()))));
            ySubsampling = Math.max(1, Math.min(height>>8, (int)Math.round(height * (resolution.getHeight() / clipLogical.getHeight()))));
        } else {
            xSubsampling = 1;
            ySubsampling = 1;
        }
        if (clipArea == null) {
            clipLogical = tableToCoverageCS(clipLogical);
        } else {
            /*
             * Vérifie si le rectangle demandé (clipArea) intercepte la région géographique
             * couverte par l'image. On utilise un code spécial plutôt que de faire appel à
             * {@link Rectangle2D#intersects} parce qu'on veut accepter les cas où le rectangle
             * demandé se résume à une ligne ou un point.
             */
            if (clipArea.getWidth()<0 || clipArea.getHeight()<0 || clipLogical.isEmpty()) {
                return null;
            }
            if (clipArea.getMaxX() < clipLogical.getMinX() ||
                clipArea.getMinX() > clipLogical.getMaxX() ||
                clipArea.getMaxY() < clipLogical.getMinY() ||
                clipArea.getMinY() > clipLogical.getMaxY())
            {
                return null;
            }
            final Rectangle2D fullArea = tableToCoverageCS((Rectangle2D)clipLogical.clone());
            Rectangle2D.intersect(clipLogical, clipArea, clipLogical);
            clipLogical = tableToCoverageCS(clipLogical);
            /*
             * Conversion [coordonnées logiques] --> [coordonnées pixels].
             */
            final double scaleX =  width/fullArea.getWidth();
            final double scaleY = height/fullArea.getHeight();
            clipPixel = new Rectangle((int)Math.floor(scaleX*(clipLogical.getMinX()-fullArea.getMinX())+EPS),
                                      (int)Math.floor(scaleY*(fullArea.getMaxY()-clipLogical.getMaxY())+EPS),
                                      (int)Math.ceil (scaleX*clipLogical.getWidth() -EPS),
                                      (int)Math.ceil (scaleY*clipLogical.getHeight()-EPS));
            if (clipPixel.width < MIN_SIZE) {
                clipPixel.x    -= (MIN_SIZE-clipPixel.width)/2;
                clipPixel.width = MIN_SIZE;
            }
            if (clipPixel.height < MIN_SIZE) {
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
            if (clipPixel.isEmpty()) {
                return null;
            }
            /*
             * Conversion [coordonnées pixels] --> [coordonnées logiques].
             *
             * 'clipLogical' ne devrait pas beaucoup changer (mais parfois un peu).
             */
            clipLogical.setRect(fullArea.getMinX() + clipPixel.getMinX()  /scaleX,
                                fullArea.getMaxY() - clipPixel.getMaxY()  /scaleY,
                                                     clipPixel.getWidth() /scaleX,
                                                     clipPixel.getHeight()/scaleY);
        }
        /*
         * Avant d'effectuer la lecture, vérifie si l'image est déjà en mémoire. Une image
         * {@link RenderedGridCoverage} peut être en mémoire même si {@link GridCoverage}
         * ne l'est plus si, par exemple, l'image est entrée dans une chaîne d'opérations de JAI.
         */
        RenderedImage image = null;
        if (renderedImage != null) {
            image = renderedImage.get();
            if (image == null) {
                renderedImage = null;
            }
        }
        /*
         * A ce stade, nous disposons maintenant des coordonnées en pixels
         * de la région à charger. Procède maintenant à la lecture.
         */
        final FormatEntry format = parameters.format;
        final SampleDimension[] bands;
        try {
            format.setReading(this, true);
            synchronized (format) {
                final ImageReadParam param = format.getDefaultReadParam();
                if (clipPixel != null) {
                    param.setSourceRegion(clipPixel);
                }
                param.setSourceSubsampling(xSubsampling,   ySubsampling,
                                           xSubsampling/2, ySubsampling/2);
                if (image == null) {
                    image = format.read(getInput(true), imageIndex, param, listenerList,
                                        new Dimension(width, height), this);
                    if (image == null) {
                        return null;
                    }
                }
                bands = format.getSampleDimensions(param);
            }
        } finally {
            format.setReading(this, false);
        }
        /*
         * La lecture est maintenant terminée et n'a pas été annulée.
         * On construit maintenant l'objet {@link GridCoverage}, on le
         * conserve dans une cache interne puis on le retourne.
         */
        CoordinateSystem imageCS = parameters.imageCS;
        double[] min = new double[] {clipLogical.getMinX(), clipLogical.getMinY(), CoordinateSystemTable.toJulian(startTime)};
        double[] max = new double[] {clipLogical.getMaxX(), clipLogical.getMaxY(), CoordinateSystemTable.toJulian(  endTime)};
        if (Double.isInfinite(min[2]) && Double.isInfinite(max[2])) {
            // No time range specified.
            min = XArray.resize(min, 2);
            max = XArray.resize(max, 2);
            imageCS = CTSUtilities.getCoordinateSystem2D(imageCS);
        }
        GridCoverage coverage = new GridCoverage(filename, image, imageCS,
                                new Envelope(min, max), bands, null,
                                Collections.singletonMap(SOURCE_KEY, this));
        /*
         * Retourne toujours la version "géophysique" de l'image.
         */
        coverage = coverage.geophysics(true);
        /*
         * Si l'utilisateur a spécifié une operation à appliquer
         * sur les images, applique cette opération maintenant.
         */
        Operation operation = parameters.operation;
        boolean interpolationDone = false;
        if (operation != null) {
            synchronized (operation) {
                try {
                    ParameterList param = parameters.parameters.setParameter("Source", coverage);
                    coverage = PROCESSOR.doOperation(operation, param);
                } finally {
                    parameters.parameters.setParameter("Source", null);
                }
                if (operation.getName().equalsIgnoreCase("Interpolate")) {
                    interpolationDone = true;
                }
            }
        }
        /*
         * Applique l'interpolation bicubique, conserve le
         * résultat dans une cache et retourne le résultat.
         */
        if (!interpolationDone) {
            coverage  = PROCESSOR.doOperation("Interpolate", coverage, "Type", INTERPOLATIONS);
        }
        renderedImage = new WeakReference<RenderedImage>(image);
        gridCoverage  = new SoftReference<GridCoverage>(coverage);
        synchronized (LAST_INVOKED) {
            LAST_INVOKED.addLast(this);
            while (LAST_INVOKED.size() > ENTRY_CAPACITY) {
                LAST_INVOKED.removeFirst().clearSoftReference();
            }
        }
        return coverage;
    }

    /**
     * Remplace la référence molle de {@link #gridCoverage} par une référence faible.
     * Cette méthode est appelée par quand on a déterminé que la mémoire allouée par
     * un {@link GridCoverage} devrait être libérée.
     */
    private synchronized void clearSoftReference() {
        if (gridCoverage instanceof SoftReference) {
            final GridCoverage coverage = gridCoverage.get();
            gridCoverage = (coverage!=null) ? new WeakReference<GridCoverage>(coverage) : null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void abort() throws RemoteException {
        parameters.format.abort(this);
    }

    /**
     * Retourne une chaîne de caractères représentant cette entrée.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer(40);
        buffer.append(Utilities.getShortClassName(this));
        buffer.append('[');
        buffer.append(getName());
        if (startTime!=Long.MIN_VALUE && endTime!=Long.MAX_VALUE) {
            buffer.append(" (");
            buffer.append(parameters.format(new Date((startTime+endTime)/2)));
            buffer.append(')');
        }
        buffer.append(' ');
        buffer.append(CTSUtilities.toWGS84String(parameters.tableCS, getGeographicArea()));
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Indique si cette entrée est identique à l'entrée spécifiée. Cette méthode vérifie tous
     * les paramètres de <code>GridCoverageEntry</code>, incluant le chemin de l'image et les
     * coordonnées géographiques de la région qui a été demandée. Si vous souhaitez seulement
     * vérifier si deux objets <code>GridCoverageEntry</code> décrivent bien la même image
     * (même si les coordonnées de la région demandée sont différentes), comparez plutôt leur
     * numéros {@link #getID}. Notez que cette dernière solution n'est valide que si les deux
     * objets <code>GridCoverageEntry</code> proviennent bien de la même base de données.
     */
    public boolean equals(final Object object) {
        return (object instanceof GridCoverageEntry) && equalsStrict((GridCoverageEntry) object);
    }

    /**
     * Indique si cette entrée est strictement égale à l'entrée spécifiée. Tous
     * les champs sont pris en compte, y compris ceux qui ne proviennent pas de
     * la base de données (comme les coordonnées de la région désirée par
     * l'utilisateur).
     */
    private boolean equalsStrict(final GridCoverageEntry that) {
        return Utilities.equals(this.filename,   that.filename  ) &&
               Utilities.equals(this.parameters, that.parameters) &&
               sameSize(that) && sameCoordinates(that);
    }

    /**
     * Indique si l'image de cette entrée couvre la
     * même région géographique et la même plage de
     * temps que celles de l'entré spécifiée.   Les
     * deux entrés peuvent toutefois appartenir à
     * des séries différentes.
     */
    private boolean sameCoordinates(final GridCoverageEntry that) {
        return this.startTime == that.startTime && this.endTime == that.endTime   &&
               Float.floatToIntBits(this.xmin) == Float.floatToIntBits(that.xmin) &&
               Float.floatToIntBits(this.xmax) == Float.floatToIntBits(that.xmax) &&
               Float.floatToIntBits(this.ymin) == Float.floatToIntBits(that.ymin) &&
               Float.floatToIntBits(this.ymax) == Float.floatToIntBits(that.ymax) &&
               parameters.tableCS.equals(that.parameters.tableCS, false);
    }

    /**
     * Indique si l'image de cette entrée a la même dimension que l'image
     * spécifiée. Cette méthode ne vérifie pas si les deux images couvrent
     * la même région géographique.
     */
    private boolean sameSize(final GridCoverageEntry that) {
        return (this.width==that.width) && (this.height==that.height);
    }

    /**
     * Retourne un code représentant cette entrée.
     */
    public int hashCode() {
        return (int)serialVersionUID ^ filename.hashCode();
    }

    /**
     * Après la lecture binaire, vérifie si
     * l'entrée lue existait déjà en mémoire.
     */
    private Object readResolve() throws ObjectStreamException {
        return canonicalize();
    }

    /**
     * Retourne un exemplaire unique de cette entrée. Une banque d'entrées, initialement
     * vide, est maintenue de façon interne par la classe <code>GridCoverageEntry</code>.
     * Lorsque la méthode <code>canonicalize</code> est appelée, elle recherchera des entrées
     * égales à <code>this</code> au sens de la méthode {@link #equals}. Si de telles entrées
     * sont trouvées, elles seront retournées. Sinon, les entrées <code>this</code>
     * seront ajoutées à la banque de données en utilisant une référence faible
     * et cette méthode retournera <code>this</code>.
     * <br><br>
     * De cette méthode il s'ensuit que pour deux entrées <var>u</var> et <var>v</var>,
     * la condition <code>u.canonicalize()==v.canonicalize()</code> sera vrai si et seulement si
     * <code>u.equals(v)</code> est vrai.
     */
    final GridCoverageEntry canonicalize() {
        return (GridCoverageEntry) POOL.canonicalize(this);
    }

    /**
     * Applique {@link #canonicalize()} sur un tableau d'entrées.
     * Ce tableau peut contenir des éléments nuls.
     */
    static void canonicalize(final CoverageEntry[] entries) {
        POOL.canonicalize(entries);
    }

    /**
     * Si les deux images couvrent les mêmes coordonnées spatio-temporelles,
     * retourne celle qui a la plus basse résolution. Si les deux images ne
     * couvrent pas les mêmes coordonnées ou si leurs résolutions sont
     * incompatibles, alors cette méthode retourne <code>null</code>.
     */
    final GridCoverageEntry getLowestResolution(final GridCoverageEntry that) {
        if (Utilities.equals(this.parameters.series, that.parameters.series) && sameCoordinates(that)) {
            if (this.width<=that.width && this.height<=that.height) return this;
            if (this.width>=that.width && this.height>=that.height) return that;
        }
        return null;
    }

    /**
     * Indique si cette image a au moins la résolution spécifiée.
     *
     * @param  resolution Résolution désirée, exprimée selon le système de coordonnées
     *                    de la table d'images.
     * @return <code>true</code> si la résolution de cette image est égale ou supérieure à la
     *         résolution demandée. Cette méthode retourne <code>false</code> si <code>resolution</code>
     *         était nul.
     */
    final boolean hasEnoughResolution(final Dimension2D resolution) {
        if (resolution != null) {
            double  width  = resolution.getWidth();
            double  height = resolution.getHeight();
            final float dx = (xmax-xmin);
            final float dy = (ymax-ymin);
            if ((1+EPS)*width  >= dx/this.width &&
                (1+EPS)*height >= dy/this.height)
            {
                return true;
            }
        }
        return false;
    }
}
