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
 */
package fr.ird.database.coverage.sql;

// Base de donn�es
import java.sql.ResultSet;
import java.sql.SQLWarning;
import java.sql.SQLException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// Entr�s/sorties
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

// Geom�trie
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;

// R�f�rences faibles
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
 * Information sur une image. Un objet <code>GridCoverageEntry</code> correspond �
 * un enregistrement de la base de donn�es d'images. Ces informations sont
 * retourn�es par la m�thode {@link GridCoverageTable#getEntries}.
 * <br><br>
 * Les objets <code>GridCoverageEntry</code> sont imutables et s�curitaires dans un
 * environnement multi-threads.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class GridCoverageEntry extends UnicastRemoteObject implements CoverageEntry {
    /**
     * Cl� sous laquelle m�moriser l'objet {@link CoverageEntry}
     * source dans les propri�t�s de {@link GridCoverage}.
     */
    private static final CaselessStringKey SOURCE_KEY =
            new CaselessStringKey(CoverageEntry.SOURCE_KEY);

    /**
     * Compare deux entr�es selon le m�me crit�re que celui qui a apparait dans
     * l'instruction "ORDER BY" dans la r�qu�te SQL de {@link GridCoverageTable}).
     * Les entr�s sans dates sont une exception: elles sont consid�r�es comme non-ordonn�es.
     */
    boolean compare(final GridCoverageEntry other) {
        if (startTime==Long.MIN_VALUE && endTime==Long.MAX_VALUE) {
            return false;
        }
        return endTime == other.endTime;
    }

    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 135730397985915935L;

    /**
     * Les interpolations � appliquer sur les images retourn�es. D'abord, une
     * interpolation bicubique. Si elle �choue, une interpolation bilin�aire.
     * Si cette derni�re �choue aussi, alors le plus proche voisin.
     */
    private static final String[] INTERPOLATIONS = {
        "Bicubic",
        "Bilinear",
        "NearestNeighbor"
    };

    /**
     * Objet � utiliser par d�faut pour construire des transformations de coordonn�es.
     */
    private static final CoordinateTransformationFactory TRANSFORMS =
                         CoordinateTransformationFactory.getDefault();

    /**
     * L'objet � utiliser pour appliquer des op�rations sur les images lues.
     *
     * @see CoverageDataBase#getDefaultGridCoverageProcessor
     * @see CoverageDataBase#setDefaultGridCoverageProcessor
     */
    static GridCoverageProcessor PROCESSOR = new fr.ird.database.coverage.sql.GridCoverageProcessor();

    /**
     * Ensemble des entr�s qui ont d�j� �t� retourn�es par {@link #canonicalize()}
     * et qui n'ont pas encore �t� r�clam�es par le ramasse-miettes. La classe
     * {@link GridCoverageTable} tentera autant que possible de retourner des entr�es
     * qui existent d�j� en m�moire afin de leur donner une chance de faire un
     * meilleur travail de cache sur les images.
     */
    private static final WeakHashSet POOL = Table.POOL;
    
    /**
     * Liste des derniers {@link GridCoverageEntry} pour lesquels la m�thode
     * {@link #getGridCoverage} a �t� appel�e. Lorsqu'une nouvelle image est lue, les r�f�rences
     * molles des plus anciennes sont chang�es en r�f�rences faibles afin d'augmenter les chances
     * que le ramasse-miette se d�barasse des images les plus anciennes avant que la m�moire ne
     * sature.
     */
    private static final LinkedList<GridCoverageEntry> LAST_INVOKED = new LinkedList<GridCoverageEntry>();

    /**
     * Nombre maximal d'entr�s � conserver dans la liste {@link #LAST_INVOKED}.
     *
     * @task TODO: Une meilleure mesure serait la m�moire occup�e...
     */
    private static final int ENTRY_CAPACITY = 8;

    /**
     * Petite valeur utilis�e pour contourner les erreurs d'arrondissement.
     */
    private static final double EPS = 1E-6;

    /**
     * Largeur et hauteur minimale des images, en pixels. Si l'utilisateur
     * demande une r�gion plus petite, la r�gion demand�e sera agradie pour
     * que l'image fasse cette taille.
     */
    private static final int MIN_SIZE = 8;

    /** Nom du fichier.                  */ private final String filename;
    /** Date du d�but de l'acquisition.  */ private final   long startTime;
    /** Date de la fin de l'acquisition. */ private final   long endTime;
    /** Longitude minimale.              */         final  float xmin;
    /** Longitude maximale.              */         final  float xmax;
    /** Latitude minimale.               */         final  float ymin;
    /** Latitude maximale.               */         final  float ymax;
    /** Nombre de pixels en largeur.     */ private final  short width;
    /** Nombre de pixels en hauteur.     */ private final  short height;

    /**
     * Bloc de param�tres de la table d'images. On retient ce bloc de param�tres
     * plut�t qu'une r�f�rence directe vers {@link GridCoverageTable} afin de ne
     * pas emp�cher le ramasse-miettes de d�truire la table et ses connections
     * vers la base de donn�es.
     */
    private final Parameters parameters;

    /**
     * R�f�rence molle vers l'image {@link GridCoverage} qui a �t� retourn�e lors
     * du dernier appel de {@link #getGridCoverage}.  Cette r�f�rence est retenue
     * afin d'�viter de charger inutilement une autre fois l'image si elle est d�j�
     * en m�moire.
     */
    private transient Reference<GridCoverage> gridCoverage;

    /**
     * R�f�rence molle vers l'image {@link RenderedImage} qui a �t� retourn�e lors
     * du dernier appel de {@link #getGridCoverage}.   Cette r�f�rence est retenue
     * afin d'�viter de charger inutilement une autre fois l'image si elle est d�j�
     * en m�moire.
     */
    private transient Reference<RenderedImage> renderedImage;

    /**
     * Construit une entr� contenant des informations sur une image.
     *
     * @param  table  Table d'o� proviennent les enregistrements.
     * @param  result Prochain enregistrement � lire.
     * @throws SQLException si l'acc�s au catalogue a �chou�.
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
        // TODO: m�moriser les coordonn�es dans un Rectangle2D et lancer une exception s'il est vide.
        // NOTE: Les coordonn�es xmin, xmax, ymin et ymax ne sont PAS exprim�es selon le syst�me de
        //       coordonn�es de l'image, mais plut�t selon le syst�me de coordonn�es de la table
        //       d'images. La transformation sera effectu�e par 'getEnvelope()'.
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
     * Retourne un nom d�signant cette image. Le choix du nom est arbitraire,
     * mais il s'agira le plus souvent du nom du fichier (avec ou sans son
     * extension).
     */
    public String getName() {
        return filename;
    }

    /**
     * Retourne <code>null</code>, �tant donn� que les images ne sont pas
     * accompagn�es de description.
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
     * Proj�te la table sp�cifi�e du syst�me de coordonn�es de la table vers le syst�me
     * de coordonn�es de l'image.
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
     * Retourne la date de d�but d'�chantillonage de l'image,
     * ou <code>null</code> si elle n'est pas connue.
     */
    public Date getStartTime() {
        return (startTime!=Long.MIN_VALUE) ? new Date(startTime) : null;
    }

    /**
     * Retourne la date de fin d'�chantillonage de l'image,
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
     * @task TODO: If faudrait trouver un moyen de d�terminer la plage de valeurs lorsqu'une
     *             op�ration est appliqu�e. Peut-�tre avec OperationJAI.deriveSampleDimensions?
     */
    public SampleDimension[] getSampleDimensions() {
        final SampleDimension[] bands = parameters.format.getSampleDimensions();

        // HACK BEGIN
        final Operation operation = parameters.operation;
        if (operation != null) {
            // Note: le nom peut appara�tre n'importe o� (ex: "NodataFilter;GradientMagnitude").
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
     * Proc�de � la lecture d'une image � l'index sp�cifi�.
     *
     * @param imageIndex Index de l'image � lire.
     *        NOTE: si on permet d'obtenir des images � diff�rents index, il faudra en
     *              tenir compte dans {@link #gridCoverage} et {@link #renderedImage}.
     * @param listenerList Liste des objets � informer des progr�s de la lecture.
     */
    private synchronized GridCoverage getGridCoverage(final int imageIndex,
                                                      final EventListenerList listenerList)
            throws IOException, TransformException
    {
        /*
         * NOTE SUR LES SYNCHRONISATIONS: Cette m�thode est synchronis�e � plusieurs niveau:
         *
         *  1) Toute la m�thode sur 'this',  afin d'�viter qu'une image ne soit lue deux fois
         *     si un thread tente d'acc�der � la cache alors que l'autre thread n'a pas eu le
         *     temps de placer le r�sultat de la lecture dans cette cache.   Synchroniser sur
         *     'this' ne devrait pas avoir d'impact significatif sur la performance,    �tant
         *     donn� que l'op�ration vraiment longue (la lecture de l'image) est synchronis�e
         *     sur 'format' de toute fa�on (voir prochain item).
         *
         *  2) La lecture de l'image sur 'format'. On ne synchronise pas toute la m�thode sur
         *     'format' afin de ne pas bloquer l'acc�s � la cache  pour un objet 'CoverageEntry'
         *     donn� pendant qu'une lecture est en cours  sur un autre objet 'CoverageEntry' qui
         *     utiliserait le m�me format.
         *
         *  3) Les demandes d'annulation de lecture ({@link #abort}) sur
         *     <code>FormatEntryImpl.enqueued</code>, afin de pouvoir
         *     �tre faite pendant qu'une lecture est en cours. Cette
         *     synchronisation est g�r�e en interne par <code>FormatEntryImpl</code>.
         */

        /*
         * V�rifie d'abord si l'image demand�e se trouve d�j� en m�moire. Si
         * oui, elle sera retourn�e et la m�thode se termine imm�diatement.
         */
        if (gridCoverage != null) {
            final GridCoverage image = gridCoverage.get();
            if (image != null) {
                return image;
            }
        }
        gridCoverage = null;
        /*
         * Obtient les coordonn�es g�ographiques et la r�solution d�sir�es. Notez que ces
         * rectangles ne sont pas encore exprim�es dans le syst�me de coordonn�es de l'image.
         * Cette projection sera effectu�e par 'tableToCoverageCS(...)' seulement apr�s avoir
         * pris en compte le clip. Ca nous �vite d'avoir � projeter le clip, ce qui aurait �t�
         * probl�matique avec les projections qui n'ont pas un domaine de validit� suffisament
         * grand (par exemple jusqu'aux p�les).
         */
        final Rectangle2D clipArea   = parameters.geographicArea;
        final Dimension2D resolution = parameters.resolution;
        /*
         * Proc�de � la lecture de l'image correspondant � cette entr�e. Si l'image n'intercepte
         * pas le rectangle <code>clipArea</code> sp�cifi� ou si l'utilisateur a interrompu la
         * lecture, alors cette m�thode retourne <code>null</code>. Les coordonn�es de la r�gion
         * couverte par l'image retourn�e peuvent ne pas �tre identiques aux coordonn�es sp�cifi�es.
         * La m�thode {@link GridCoverage#getEnvelope} permettra de conna�tre les coordonn�es exactes.
         *
         * @param  clipArea Coordonn�es g�ographiques de la r�gion d�sir�e, ou <code>null</code>
         *         pour prendre l'image au complet. Les coordonn�es doivent �tre exprim�es selon
         *         le syst�me de coordonn�es de la table d'images, tel que retourn� par {@link
         *         CoverageTable#getCoordinateSystem}. Ce n'est pas n�cessairement le m�me
         *         syst�me de coordonn�es que celui de l'image � lire.
         * @param  resolution Dimension logique d�sir�e des pixels de l'image, ou <code>null</code>
         *         pour demander la meilleure r�solution possible. Les dimensions doivent �tre
         *         exprim�es selon le m�me syst�me de coordonn�es que <code>area</code>. Cette
         *         information n'est qu'approximative; Il n'est pas garantie que la lecture
         *         produira effectivement une image de la r�solution demand�e.
         */
        Rectangle2D clipLogical = new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
        Rectangle   clipPixel   = null;
        final int xSubsampling;
        final int ySubsampling;
        if (resolution != null) {
            /*
             * Conversion [r�solution logique d�sir�e] --> [fr�quence d'�chantillonage des pixels].
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
             * V�rifie si le rectangle demand� (clipArea) intercepte la r�gion g�ographique
             * couverte par l'image. On utilise un code sp�cial plut�t que de faire appel �
             * {@link Rectangle2D#intersects} parce qu'on veut accepter les cas o� le rectangle
             * demand� se r�sume � une ligne ou un point.
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
             * Conversion [coordonn�es logiques] --> [coordonn�es pixels].
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
             * V�rifie que les coordonn�es obtenues sont bien
             * dans les limites de la dimension de l'image.
             */
            final int clipX2 = Math.min(this.width,  clipPixel.width  + clipPixel.x);
            final int clipY2 = Math.min(this.height, clipPixel.height + clipPixel.y);
            if (clipPixel.x < 0) clipPixel.x = 0;
            if (clipPixel.y < 0) clipPixel.y = 0;
            clipPixel.width  = clipX2-clipPixel.x;
            clipPixel.height = clipY2-clipPixel.y;
            /*
             * V�rifie que la largeur du rectangle est un
             * multiple entier de la fr�quence d'�chantillonage.
             */
            clipPixel.width  = (clipPixel.width /xSubsampling) * xSubsampling;
            clipPixel.height = (clipPixel.height/ySubsampling) * ySubsampling;
            if (clipPixel.isEmpty()) {
                return null;
            }
            /*
             * Conversion [coordonn�es pixels] --> [coordonn�es logiques].
             *
             * 'clipLogical' ne devrait pas beaucoup changer (mais parfois un peu).
             */
            clipLogical.setRect(fullArea.getMinX() + clipPixel.getMinX()  /scaleX,
                                fullArea.getMaxY() - clipPixel.getMaxY()  /scaleY,
                                                     clipPixel.getWidth() /scaleX,
                                                     clipPixel.getHeight()/scaleY);
        }
        /*
         * Avant d'effectuer la lecture, v�rifie si l'image est d�j� en m�moire. Une image
         * {@link RenderedGridCoverage} peut �tre en m�moire m�me si {@link GridCoverage}
         * ne l'est plus si, par exemple, l'image est entr�e dans une cha�ne d'op�rations de JAI.
         */
        RenderedImage image = null;
        if (renderedImage != null) {
            image = renderedImage.get();
            if (image == null) {
                renderedImage = null;
            }
        }
        /*
         * A ce stade, nous disposons maintenant des coordonn�es en pixels
         * de la r�gion � charger. Proc�de maintenant � la lecture.
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
         * La lecture est maintenant termin�e et n'a pas �t� annul�e.
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
         * Retourne toujours la version "g�ophysique" de l'image.
         */
        coverage = coverage.geophysics(true);
        /*
         * Si l'utilisateur a sp�cifi� une operation � appliquer
         * sur les images, applique cette op�ration maintenant.
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
         * r�sultat dans une cache et retourne le r�sultat.
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
     * Remplace la r�f�rence molle de {@link #gridCoverage} par une r�f�rence faible.
     * Cette m�thode est appel�e par quand on a d�termin� que la m�moire allou�e par
     * un {@link GridCoverage} devrait �tre lib�r�e.
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
     * Retourne une cha�ne de caract�res repr�sentant cette entr�e.
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
     * Indique si cette entr�e est identique � l'entr�e sp�cifi�e. Cette m�thode v�rifie tous
     * les param�tres de <code>GridCoverageEntry</code>, incluant le chemin de l'image et les
     * coordonn�es g�ographiques de la r�gion qui a �t� demand�e. Si vous souhaitez seulement
     * v�rifier si deux objets <code>GridCoverageEntry</code> d�crivent bien la m�me image
     * (m�me si les coordonn�es de la r�gion demand�e sont diff�rentes), comparez plut�t leur
     * num�ros {@link #getID}. Notez que cette derni�re solution n'est valide que si les deux
     * objets <code>GridCoverageEntry</code> proviennent bien de la m�me base de donn�es.
     */
    public boolean equals(final Object object) {
        return (object instanceof GridCoverageEntry) && equalsStrict((GridCoverageEntry) object);
    }

    /**
     * Indique si cette entr�e est strictement �gale � l'entr�e sp�cifi�e. Tous
     * les champs sont pris en compte, y compris ceux qui ne proviennent pas de
     * la base de donn�es (comme les coordonn�es de la r�gion d�sir�e par
     * l'utilisateur).
     */
    private boolean equalsStrict(final GridCoverageEntry that) {
        return Utilities.equals(this.filename,   that.filename  ) &&
               Utilities.equals(this.parameters, that.parameters) &&
               sameSize(that) && sameCoordinates(that);
    }

    /**
     * Indique si l'image de cette entr�e couvre la
     * m�me r�gion g�ographique et la m�me plage de
     * temps que celles de l'entr� sp�cifi�e.   Les
     * deux entr�s peuvent toutefois appartenir �
     * des s�ries diff�rentes.
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
     * Indique si l'image de cette entr�e a la m�me dimension que l'image
     * sp�cifi�e. Cette m�thode ne v�rifie pas si les deux images couvrent
     * la m�me r�gion g�ographique.
     */
    private boolean sameSize(final GridCoverageEntry that) {
        return (this.width==that.width) && (this.height==that.height);
    }

    /**
     * Retourne un code repr�sentant cette entr�e.
     */
    public int hashCode() {
        return (int)serialVersionUID ^ filename.hashCode();
    }

    /**
     * Apr�s la lecture binaire, v�rifie si
     * l'entr�e lue existait d�j� en m�moire.
     */
    private Object readResolve() throws ObjectStreamException {
        return canonicalize();
    }

    /**
     * Retourne un exemplaire unique de cette entr�e. Une banque d'entr�es, initialement
     * vide, est maintenue de fa�on interne par la classe <code>GridCoverageEntry</code>.
     * Lorsque la m�thode <code>canonicalize</code> est appel�e, elle recherchera des entr�es
     * �gales � <code>this</code> au sens de la m�thode {@link #equals}. Si de telles entr�es
     * sont trouv�es, elles seront retourn�es. Sinon, les entr�es <code>this</code>
     * seront ajout�es � la banque de donn�es en utilisant une r�f�rence faible
     * et cette m�thode retournera <code>this</code>.
     * <br><br>
     * De cette m�thode il s'ensuit que pour deux entr�es <var>u</var> et <var>v</var>,
     * la condition <code>u.canonicalize()==v.canonicalize()</code> sera vrai si et seulement si
     * <code>u.equals(v)</code> est vrai.
     */
    final GridCoverageEntry canonicalize() {
        return (GridCoverageEntry) POOL.canonicalize(this);
    }

    /**
     * Applique {@link #canonicalize()} sur un tableau d'entr�es.
     * Ce tableau peut contenir des �l�ments nuls.
     */
    static void canonicalize(final CoverageEntry[] entries) {
        POOL.canonicalize(entries);
    }

    /**
     * Si les deux images couvrent les m�mes coordonn�es spatio-temporelles,
     * retourne celle qui a la plus basse r�solution. Si les deux images ne
     * couvrent pas les m�mes coordonn�es ou si leurs r�solutions sont
     * incompatibles, alors cette m�thode retourne <code>null</code>.
     */
    final GridCoverageEntry getLowestResolution(final GridCoverageEntry that) {
        if (Utilities.equals(this.parameters.series, that.parameters.series) && sameCoordinates(that)) {
            if (this.width<=that.width && this.height<=that.height) return this;
            if (this.width>=that.width && this.height>=that.height) return that;
        }
        return null;
    }

    /**
     * Indique si cette image a au moins la r�solution sp�cifi�e.
     *
     * @param  resolution R�solution d�sir�e, exprim�e selon le syst�me de coordonn�es
     *                    de la table d'images.
     * @return <code>true</code> si la r�solution de cette image est �gale ou sup�rieure � la
     *         r�solution demand�e. Cette m�thode retourne <code>false</code> si <code>resolution</code>
     *         �tait nul.
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
