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
import java.sql.SQLWarning;
import java.sql.SQLException;

// Entr�s/sorties
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
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
import java.util.Collections;
import java.awt.image.RenderedImage;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;
import javax.media.jai.util.CaselessStringKey;

// Geotools (CTS)
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.TransformException;
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
import org.geotools.resources.XDimension2D;
import org.geotools.resources.XRectangle2D;

// Seagis
import fr.ird.resources.Resources;


/**
 * Information sur une image. Un objet <code>ImageEntry</code> correspond �
 * un enregistrement de la base de donn�es d'images. Ces informations sont
 * retourn�es par la m�thode {@link ImageTable#getEntries}.
 * <br><br>
 * Les objets <code>ImageEntry</code> sont imutables et s�curitaires dans un
 * environnement multi-threads.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class ImageEntryImpl implements ImageEntry, Serializable {
    /**
     * Cl� sous laquelle m�moriser l'objet {@link ImageEntry}
     * source dans les propri�t�s de {@link GridCoverage}.
     */
    private static final CaselessStringKey SOURCE_KEY = new CaselessStringKey(ImageEntry.SOURCE_KEY);

    /**
     * Compare deux entr�es selon le m�me crit�re que celui qui a apparait dans
     * l'instruction "ORDER BY" dans la r�qu�te SQL de {@link ImageTableImpl}).
     */
    boolean compare(final ImageEntryImpl other) {
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
     * L'objet � utiliser pour appliquer
     * des op�rations sur les images lues.
     */
    static final GridCoverageProcessor PROCESSOR =
                 GridCoverageProcessor.getDefault();

    /**
     * Ensemble des entr�s qui ont d�j� �t� retourn�es par {@link #intern()}
     * et qui n'ont pas encore �t� r�clam�es par le ramasse-miettes. La classe
     * {@link ImageTable} tentera autant que possible de retourner des entr�es
     * qui existent d�j� en m�moire afin de leur donner une chance de faire un
     * meilleur travail de cache sur les images.
     */
    private static final WeakHashSet pool = Table.pool;

    /**
     * Petite valeur utilis�e pour contourner
     * les erreurs d'arrondissement.
     */
    private static final double EPS = 1E-6;

    /**
     * Largeur et hauteur minimale des images, en pixels. Si l'utilisateur
     * demande une r�gion plus petite, la r�gion demand�e sera agradie pour
     * que l'image fasse cette taille.
     */
    private static final int MIN_SIZE = 8;

    /** Num�ro identifiant l'image.      */ private final    int ID;
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
     * plut�t qu'une r�f�rence directe vers {@link ImageTable} afin de ne pas
     * emp�cher le ramasse-miettes de d�truire la table et ses connections vers
     * la base de donn�es.
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
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    ImageEntryImpl(final ImageTableImpl table, final ResultSet result) throws SQLException {
        final int    seriesID;
        final int    formatID;
        final String pathname;
        final Date   startTime;
        final Date     endTime;
        ID         = result.getInt      (ImageTableImpl.ID);
        seriesID   = result.getInt      (ImageTableImpl.SERIES);
        pathname   = result.getString   (ImageTableImpl.PATHNAME).intern();
        filename   = result.getString   (ImageTableImpl.FILENAME);
        startTime  = table .getTimestamp(ImageTableImpl.START_TIME, result);
        endTime    = table .getTimestamp(ImageTableImpl.END_TIME,   result);
        xmin       = result.getFloat    (ImageTableImpl.XMIN);
        xmax       = result.getFloat    (ImageTableImpl.XMAX);
        ymin       = result.getFloat    (ImageTableImpl.YMIN);
        ymax       = result.getFloat    (ImageTableImpl.YMAX);
        width      = result.getShort    (ImageTableImpl.WIDTH);
        height     = result.getShort    (ImageTableImpl.HEIGHT);
        formatID   = result.getInt      (ImageTableImpl.FORMAT);
        parameters = table .getParameters(seriesID, formatID, pathname);
        // TODO: m�moriser les coordonn�es dans un Rectangle2D et lancer une exception s'il est vide.
        // NOTE: Les coordonn�es xmin, xmax, ymin et ymax ne sont PAS exprim�es selon le syst�me de
        //       coordonn�es de l'image, mais plut�t selon le syst�me de coordonn�es de la table
        //       d'images. La transformation sera effectu�e par 'getEnvelope()'.
        this.startTime = (startTime!=null) ? startTime.getTime() : Long.MIN_VALUE;
        this.  endTime = (  endTime!=null) ?   endTime.getTime() : Long.MAX_VALUE;
    }

    /**
     * Retourne le num�ro identifiant cette image dans la base de donn�es.
     * Dans une m�me base de donn�es, chaque image porte un num�ro unique.
     */
    public int getID() {
        return ID;
    }

    /**
     * Retourne la s�rie � laquelle appartient cette image.
     */
    public SeriesEntry getSeries() {
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
     * Retourne le nom du fichier de l'image avec son chemin complet.
     */
    public File getFile() {
        final File file = new File(parameters.pathname, filename+'.'+parameters.format.extension);
        if (!file.isAbsolute()) {
            final File directory = Table.directory;
            if (directory != null) {
                return new File(directory, file.getPath());
            }
        }
        return file;
    }

    /**
     * Retourne des informations sur la g�om�trie de l'image. Ces informations
     * comprennent notamment la taille de l'image  (en pixels)    ainsi que la
     * transformation � utiliser pour passer des coordonn�es pixels   vers les
     * coordonn�es du syst�me {@link #getCoordinateSystem}. Cette derni�re sera
     * le plus souvent une transformation affine.
     * <br><br>
     * Notez que la g�om�trie retourn�e par cette m�thode ne prend pas en compte un �ventuel
     * "clip" sp�cifi� par {@link ImageTable#setGeographicArea}. Il s'agit de la g�om�trie
     * de l'image telle qu'elle est d�clar�e dans la base de donn�es, ind�pendamment de la
     * fa�on dont elle sera lue. L'image qui sera retourn�e par {@link #getGridCoverage}
     * peut avoir une g�om�trie diff�rente si un clip et/ou une d�cimation ont �t� appliqu�s.
     */
    public GridGeometry getGridGeometry() {
        final GridRange gridRange = new GridRange(new int[3], new int[]{width,height,1});
        return new GridGeometry(gridRange, getEnvelope(), new boolean[]{false,true,false});
    }

    /**
     * Retourne le syst�me de coordonn�es de l'image. En g�n�ral, ce syst�me
     * de coordonn�es aura trois dimensions  (la derni�re dimension �tant le
     * temps), soit dans l'ordre:
     * <ul>
     *   <li>Les longitudes, en degr�s selon l'ellipso�de WGS 1984.</li>
     *   <li>Les latitudes,  en degr�s selon l'ellipso�de WGS 1984.</li>
     *   <li>Le temps, en jours juliens depuis le 01/01/1950 00:00 UTC.</li>
     * </ul>
     *
     * Notez que ce syst�me de coordonn�es peut ne pas �tre le m�me qui celui qui sert
     * � interroger la base de donn�es d'images ({@link ImageTable#getCoordinateSystem}).
     */
    public CoordinateSystem getCoordinateSystem() {
        return parameters.imageCS;
    }

    /**
     * Retourne les coordonn�es spatio-temporelles de l'image. Le syst�me de
     * coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}.
     * Notez que l'envelope retourn�e ne comprend pas le "clip" sp�cifi�e � la
     * table d'image (voir {@link ImageTable#setGeographicArea}). La couverture
     * retourn�e par {@link #getGridCoverage} peut donc avoir une envelope plus
     * petite que celle retourn�e par cette m�thode.
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
     * Retourne les coordonn�es g�ographiques de la r�gion couverte par l'image.
     * Les coordonn�es seront exprim�es en degr�s de longitudes et de latitudes
     * selon l'ellipso�de WGS 1984. Appeler cette m�thode �quivaut � n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et � transformer les
     * coordonn�es si n�cessaire.
     */
    public Rectangle2D getGeographicArea() {
        return new Rectangle2D.Float(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    /**
     * Retourne la plage de temps couverte par l'image.   Cette plage sera d�limit�e
     * par des objets {@link Date}.  Appeler cette m�thode �quivaut � n'extraire que
     * la partie temporelle de {@link #getEnvelope} et � transformer les coordonn�es
     * si n�cessaire.
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
     * Retourne les listes des bandes de l'image. Les objets {@link SampleDimension}
     * indiquent comment interpr�ter les valeurs des pixels. Par exemple, ils peuvent
     * indiquer que la valeur 9 d�signe des nuages.
     *
     * @return La liste des cat�gories pour chaque bande de l'image.
     *         La longueur de ce tableau sera �gale au nombre de bandes.
     */
    public SampleDimension[] getSampleDimensions() {
        return parameters.format.getSampleDimensions();
    }

    /**
     * Retourne l'image correspondant � cette entr�e. Si l'image avait d�j� �t� lue pr�c�demment
     * et qu'elle n'a pas encore �t� r�clam�e par le ramasse-miette, alors l'image existante sera
     * retourn�e sans qu'une nouvelle lecture du fichier ne soit n�cessaire. Si au contraire l'image
     * n'�tait pas d�j� en m�moire, alors un d�codage du fichier sera n�cessaire. Toutefois, cette
     * m�thode ne d�codera pas n�cessairement l'ensemble de l'image. Par d�faut, elle ne d�code que
     * la r�gion qui avait �t� indiqu�e � {@link ImageTable#setEnvelope} et sous-�chantillonne � la
     * r�solution qui avait �t� indiqu�e � {@link ImageTable#setPreferredResolution}
     * (<strong>note:</strong> cette r�gion et ce sous-�chantillonage sont ceux qui �taient actifs
     * au moment o� {@link ImageTable#getEntries} a �t� appel�e; les changement subs�quents des
     * param�tres de {@link ImageTable} n'ont pas d'effets sur les <code>ImageEntry</code> d�j�
     * cr��s).
     *
     * @param  listenerList Liste des objets � informer des progr�s de la lecture ainsi que des
     *         �ventuels avertissements, ou <code>null</code> s'il n'y en a pas.  Cette m�thode
     *         prend en compte tous les objets qui ont �t� inscrits sous la classe
     *         {@link IIOReadWarningListener} ou {@link IIOReadProgressListener}, et ignore tous
     *         les autres. Cette m�thode s'engage � ne pas modifier l'objet {@link EventListenerList}
     *         donn�.
     *
     * @return Image lue, ou <code>null</code> si l'image n'intercepte pas la r�gion g�ographique
     *         ou la plage de temps qui avaient �t� sp�cifi�es � {@link ImageTable}, ou si
     *         l'utilisateur a interrompu la lecture.
     * @throws IOException si le fichier n'a pas �t� trouv� ou si une autre erreur d'entr�s/sorties
     *         est survenue.
     * @throws IIOException s'il n'y a pas de d�codeur appropri� pour l'image, ou si l'image n'est
     *         pas valide.
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
         *     'format' afin de ne pas bloquer l'acc�s � la cache  pour un objet 'ImageEntry'
         *     donn� pendant qu'une lecture est en cours  sur un autre objet 'ImageEntry' qui
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
         *         ImageTable#getCoordinateSystem}. Ce n'est pas n�cessairement le m�me syst�me
         *         de coordonn�es que celui de l'image � lire.
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
         * {@link RenderedImage} peut �tre en m�moire m�me si {@link GridCoverage} ne l'est
         * plus si, par exemple, l'image est entr�e dans une cha�ne d'op�rations de JAI.
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
        final FormatEntryImpl format = parameters.format;
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
                    image = format.read(getFile(), imageIndex, param, listenerList,
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
        return coverage;
    }

    /**
     * Annule la lecture de l'image. Cette m�thode peut �tre appel�e � partir de n'importe quel
     * thread.  Si la m�thode {@link #getGridCoverage} �tait en train de lire une image dans un
     * autre thread, elle s'arr�tera et retournera <code>null</code>.
     */
    public void abort() {
        parameters.format.abort(this);
    }

    /**
     * Retourne une cha�ne de caract�res repr�sentant cette entr�e.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer(40);
        buffer.append("ImageEntry"); // Pour ne pas avoir le "Impl" � la fin...
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
     * Indique si cette entr�e est identique � l'entr�e sp�cifi�e. Cette m�thode
     * v�rifie tous les param�tres de <code>ImageEntry</code>, incluant le chemin
     * de l'image et les coordonn�es g�ographiques de la r�gion qui a �t� demand�e.
     * Si vous souhaitez seulement v�rifier si deux objets <code>ImageEntry</code>
     * d�crivent bien la m�me image (m�me si les coordonn�es de la r�gion demand�e
     * sont diff�rentes), comparez plut�t leur num�ros {@link #getID}. Notez que
     * cette derni�re solution n'est valide que si les deux objets <code>ImageEntry</code>
     * proviennent bien de la m�me base de donn�es.
     */
    public boolean equals(final Object o) {
        return (o instanceof ImageEntryImpl) && equalsStrict((ImageEntryImpl) o);
    }

    /**
     * Indique si cette entr�e est strictement �gale � l'entr�e sp�cifi�e. Tous
     * les champs sont pris en compte, y compris ceux qui ne proviennent pas de
     * la base de donn�es (comme les coordonn�es de la r�gion d�sir�e par
     * l'utilisateur).
     */
    private boolean equalsStrict(final ImageEntryImpl that) {
        return             this.ID       == that.ID          &&
          Utilities.equals(this.filename,   that.filename  ) &&
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
    private boolean sameCoordinates(final ImageEntryImpl that) {
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
    private boolean sameSize(final ImageEntryImpl that) {
        return (this.width==that.width) && (this.height==that.height);
    }

    /**
     * Retourne un code repr�sentant cette entr�e.
     */
    public int hashCode() {
        return ID;
    }

    /**
     * Apr�s la lecture binaire, v�rifie si
     * l'entr�e lue existait d�j� en m�moire.
     */
    private Object readResolve() throws ObjectStreamException {
        return intern();
    }

    /**
     * Retourne un exemplaire unique de cette entr�e. Une banque d'entr�es, initialement
     * vide, est maintenue de fa�on interne par la classe <code>ImageEntry</code>. Lorsque la
     * m�thode <code>intern</code> est appel�e, elle recherchera des entr�es �gales �
     * <code>this</code> au sens de la m�thode {@link #equals}. Si de telles entr�es
     * sont trouv�es, elles seront retourn�es. Sinon, les entr�es <code>this</code>
     * seront ajout�es � la banque de donn�es en utilisant une r�f�rence faible
     * et cette m�thode retournera <code>this</code>.
     * <br><br>
     * De cette m�thode il s'ensuit que pour deux entr�es <var>u</var> et <var>v</var>,
     * la condition <code>u.intern()==v.intern()</code> sera vrai si et seulement si
     * <code>u.equals(v)</code> est vrai.
     */
    final ImageEntryImpl intern() {
        return (ImageEntryImpl) pool.canonicalize(this);
    }

    /**
     * Applique {@link #intern()} sur un tableau d'entr�es.
     * Ce tableau peut contenir des �l�ments nuls.
     */
    static void intern(final ImageEntry[] entries) {
        pool.canonicalize(entries);
    }

    /**
     * Si les deux images couvrent les m�mes coordonn�es spatio-temporelles,
     * retourne celle qui a la plus basse r�solution. Si les deux images ne
     * couvrent pas les m�mes coordonn�es ou si leurs r�solutions sont
     * incompatibles, alors cette m�thode retourne <code>null</code>.
     */
    final ImageEntryImpl getLowestResolution(final ImageEntryImpl that) {
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
