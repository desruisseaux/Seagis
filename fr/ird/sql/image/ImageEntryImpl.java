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

// Entr�s/sorties
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;

// Ev�nements
import javax.swing.event.EventListenerList;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;

// Geom�trie
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;
import net.seas.util.XDimension2D;

// R�f�rences faibles
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
 * Information sur une image. Un objet <code>ImageEntry</code> correspond �
 * un enregistrement de la base de donn�es d'images. Ces informations sont
 * retourn�es par la m�thode {@link ImageTable#getEntries}.
 * <br><br>
 * Les objets <code>ImageEntry</code> sont imutables et s�curitaires dans un
 * environnement multi-threads.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class ImageEntryImpl implements ImageEntry, Serializable
{
    /**
     * Compare deux entr�es selon le m�me crit�re que celui qui a apparait dans
     * l'instruction "ORDER BY" dans la r�qu�te SQL de {@link ImageTableImpl}).
     */
    boolean compare(final ImageEntryImpl other)
    {return endTime==other.endTime;}

    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 135730397985915935L;

    /**
     * Les interpolations � appliquer sur les images retourn�es. D'abord, une
     * interpolation bicubique. Si elle �choue, une interpolation bilin�aire.
     * Si cette derni�re �choue aussi, alors le plus proche voisin.
     */
    private static final String[] INTERPOLATIONS=
    {
        "Bicubic",
        "Bilinear",
        "NearestNeighbor"
    };

    /**
     * Cl� � utiliser pour m�moriser cette entr�e dans les
     * propri�t�s de l'objet {@link GridCoverage} retourn�.
     */
    private static final CaselessStringKey ENTRY_KEY = new CaselessStringKey("Entry");

    /**
     * Ensemble des entr�s qui ont d�j� �t� retourn�es par {@link #intern()}
     * et qui n'ont pas encore �t� r�clam�es par le ramasse-miettes. La classe
     * {@link ImageTable} tentera autant que possible de retourner des entr�es
     * qui existent d�j� en m�moire afin de leur donner une chance de faire un
     * meilleur travail de cache sur les images.
     */
    private static final WeakHashSet<Object> pool = Table.pool;

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
    /** Longitude minimale.              */ private final  float xmin;
    /** Longitude maximale.              */ private final  float xmax;
    /** Latitude minimale.               */ private final  float ymin;
    /** Latitude maximale.               */ private final  float ymax;
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
     * du dernier appel de {@link #getImage}. Cette r�f�rence est retenue afin
     * d'�viter de charger inutilement une autre fois l'image si elle est d�j�
     * en m�moire.
     */
    private transient Reference gridCoverage;

    /**
     * R�f�rence molle vers l'image {@link RenderedImage} qui a �t� retourn�e lors
     * du dernier appel de {@link #getImage}. Cette r�f�rence est retenue afin
     * d'�viter de charger inutilement une autre fois l'image si elle est d�j�
     * en m�moire.
     */
    private transient Reference renderedImage;

    /**
     * Construit une entr� contenant des informations sur une image.
     *
     * @param  table  Table d'o� proviennent les enregistrements.
     * @param  result Prochain enregistrement � lire.
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�.
     */
    ImageEntryImpl(final ImageTableImpl table, final ResultSet result) throws SQLException
    {
        final int    seriesID;
        final int    formatID;
        final String pathname;
        final String ellipsoid; // TODO: pas encore utilis�.
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
        // TODO: le dernier argument (null) devrait �tre le syst�me de coordonn�es.
        this.startTime = (startTime!=null) ? startTime.getTime() : Long.MIN_VALUE;
        this.  endTime = (  endTime!=null) ?   endTime.getTime() : Long.MAX_VALUE;
    }

    /**
     * Retourne le num�ro identifiant cette image dans la
     * base de donn�es. Dans une m�me base de donn�es,
     * chaque image porte un num�ro unique.
     */
    public int getID()
    {return ID;}

    /**
     * Retourne la s�rie � laquelle
     * appartient cette image.
     */
    public SeriesEntry getSeries()
    {return parameters.series;}

    /**
     * Retourne un nom d�signant cette image. Le choix du nom est arbitraire,
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
     * Retourne des informations sur la g�om�trie de l'image. Ces informations
     * comprennent notamment la taille de l'image  (en pixels)    ainsi que la
     * transformation � utiliser pour passer des coordonn�es pixels   vers les
     * coordonn�es du syst�me {@link #getCoordinateSystem}. Cette derni�re sera
     * le plus souvent une transformation affine.
     */
    public GridGeometry getGridGeometry()
    {
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
     */
    public CoordinateSystem getCoordinateSystem()
    {return parameters.coordinateSystem;}

    /**
     * Retourne les coordonn�es spatio-temporelles de l'image. Le syst�me de
     * coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}.
     */
    public Envelope getEnvelope()
    {
        final double[] min = new double[] {xmin, ymin, ImageTableImpl.toJulian(startTime)};
        final double[] max = new double[] {xmax, ymax, ImageTableImpl.toJulian(  endTime)};
        return new Envelope(min, max);
    }

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion couverte par l'image.
     * Les coordonn�es seront exprim�es en degr�s de longitudes et de latitudes
     * selon l'ellipso�de WGS 1984. Appeler cette m�thode �quivaut � n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et � transformer les
     * coordonn�es si n�cessaire.
     */
    public Rectangle2D getGeographicArea()
    {
        // No transformation needed for current implementation.
        return new Rectangle2D.Float(xmin, ymin, xmax-xmin, ymax-ymin);
    }

    /**
     * Retourne la plage de temps couverte par l'image.   Cette plage sera d�limit�e
     * par des objets {@link Date}.  Appeler cette m�thode �quivant � n'extraire que
     * la partie temporelle de {@link #getEnvelope} et � transformer les coordonn�es
     * si n�cessaire.
     */
    public Range getTimeRange()
    {
        return new Range(Date.class,
                         (startTime!=Long.MIN_VALUE) ? new Date(startTime) : null, true,
                         (  endTime!=Long.MAX_VALUE) ? new Date(  endTime) : null, false);
    }

    /**
     * Retourne les listes de cat�gories pour toutes les bandes de l'image. Les objets
     * {@link CategoryList} indiquent comment interpr�ter les valeurs des pixels.  Par
     * exemple, ils peuvent indiquer que la valeur 9 d�signe des nuages.
     *
     * @return La liste des cat�gories pour chaque bande de l'image.
     *         La longueur de ce tableau sera �gale au nombre de bandes.
     */
    public CategoryList[] getCategoryLists()
    {return parameters.format.getCategoryLists(null);}

    /**
     * Retourne l'image correspondant � cette entr�e.     Si l'image avait d�j� �t� lue pr�c�demment et qu'elle n'a pas
     * encore �t� r�clam�e par le ramasse-miette,   alors l'image existante sera retourn�e sans qu'une nouvelle lecture
     * du fichier ne soit n�cessaire. Si au contraire l'image n'�tait pas d�j� en m�moire, alors un d�codage du fichier
     * sera n�cessaire. Toutefois, cette m�thode ne d�codera pas n�cessairement l'ensemble de l'image. Par d�faut, elle
     * ne d�code que la r�gion qui avait �t� indiqu�e � {@link ImageTable#setEnvelope} et sous-�chantillonne � la
     * r�solution qui avait �t� indiqu�e � {@link ImageTable#setPreferredResolution} (<strong>note:</strong> cette r�gion
     * et ce sous-�chantillonage sont ceux qui �taient actifs au moment o� {@link ImageTable#getEntries} a �t� appel�e;
     * les changement subs�quents des param�tres de {@link ImageTable} n'ont pas d'effets sur les <code>ImageEntry</code>
     * d�j� cr��s).
     *
     * @param  listenerList Liste des objets � informer des progr�s de la lecture ainsi que des �ventuels avertissements,
     *         ou <code>null</code> s'il n'y en a pas. Cette m�thode prend en compte tous les objets qui ont �t� inscrits
     *         sous la classe {@link IIOReadWarningListener} ou {@link IIOReadProgressListener}, et ignore tous les autres.
     *         Cette m�thode s'engage � ne pas modifier l'objet {@link EventListenerList} donn�; il est donc s�curitaire de
     *         passer directement la liste {@link javax.swing.JComponent#listenerList} d'une interface utilisateur, m�me
     *         dans un environnement multi-threads. Un objet {@link EventListenerList} peut aussi �tre construit comme suit:
     *         <blockquote><pre>
     *         {@link IIOReadProgressListener} progressListener = ...
     *         {@link IIOReadWarningListener}   warningListener = ...
     *         {@link EventListenerList}  listenerList = new EventListenerList();
     *         listenerList.add(IIOReadProgressListener.class, progressListener);
     *         listenerList.add(IIOReadWarningListener.class,   warningListener);
     *         </pre></blockquote>
     *
     * @return Image lue, ou <code>null</code> si l'image n'intercepte pas la r�gion g�ographique ou la plage de temps
     *         qui avaient �t� sp�cifi�es � {@link ImageTable}, ou si l'utilisateur a interrompu la lecture.
     * @throws IOException si le fichier n'a pas �t� trouv� ou si une autre erreur d'entr�s/sorties est survenue.
     * @throws IIOException s'il n'y a pas de d�codeur appropri� pour l'image, ou si l'image n'est pas valide.
     */
    public synchronized GridCoverage getImage(final EventListenerList listenerList) throws IOException
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
         *     <code>FormatEntry.getAbortLock()</code>, afine de pouvoir
         *     �tre faite pendant qu'une lecture est en cours. Cette
         *     synchronisation est g�r�e en interne par <code>FormatEntry</code>.
         */

        // TODO: si on permet d'obtenir des images � diff�rents index, il
        //  faudra en tenir compte dans 'gridCoverage' et 'renderedImage'.
        final int imageIndex = 0;
        /*
         * V�rifie d'abord si l'image demand�e se trouve d�j� en m�moire. Si
         * oui, elle sera retourn�e et la m�thode se termine imm�diatement.
         */
        if (gridCoverage!=null)
        {
            final GridCoverage image = (GridCoverage) gridCoverage.get();
            if (image!=null) return image;
        }
        gridCoverage=null;
        /*
         * Obtient les coordonn�es g�ographiques et la r�solution d�sir�es.
         * La classe <code>Parameters</code> a d�j� projet� ces coordonn�es
         * selon le syst�me de l'image, si c'�tait n�cessaire.
         */
        final Rectangle2D clipArea   = parameters.geographicArea;
        final Dimension2D resolution = parameters.resolution;
        /*
         * Proc�de � la lecture de l'image correspondant � cette entr�e.   Si l'image n'intercepte pas le rectangle
         * <code>clipArea</code> sp�cifi� ou si l'utilisateur a interrompu la lecture, alors cette m�thode retourne
         * <code>null</code>. Les coordonn�es de la r�gion couverte par l'image retourn�e peuvent ne pas �tre
         * identiques aux coordonn�es sp�cifi�es. La m�thode {@link GridCoverage#getEnvelope} permettra de
         * conna�tre les coordonn�es exactes.
         *
         * @param  clipArea Coordonn�es g�ographiques de la r�gion d�sir�e, ou <code>null</code> pour prendre
         *         l'image au complet. Les coordonn�es doivent �tre exprim�es selon le syst�me de coordonn�es
         *         de l'image, tel que retourn� par {@link #getCoordinateSystem}. Ce n'est pas n�cessairement
         *         le m�me syst�me de coordonn�es que {@link ImageTable}, quoique ce soit souvent le cas.
         * @param  resolution Dimension logique d�sir�e des pixels de l'image, ou <code>null</code> pour
         *         demander la meilleure r�solution possible. Les dimensions doivent �tre exprim�es selon
         *         le syst�me de coordonn�es de l'image, tel que retourn� par {@link #getCoordinateSystem}.
         *         Cette information n'est qu'approximative; Il n'est pas garantie que la lecture produira
         *         effectivement une image de la r�solution demand�e.
         */
        Rectangle2D clipLogical = new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
        Rectangle   clipPixel   = null;
        final int xSubsampling;
        final int ySubsampling;
        if (resolution!=null)
        {
            /*
             * Conversion [r�solution logique d�sir�e] --> [fr�quence d'�chantillonage des pixels].
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
             * V�rifie si le rectangle demand� (clipArea) intercepte la r�gion g�ographique couverte par l'image.
             * On utilise un code sp�cial plut�t que de faire appel � {@link Rectangle2D#intersects} parce qu'on
             * veut accepter les cas o� le rectangle demand� se r�sume � une ligne ou un point.
             */
            if (clipArea.getWidth()<0 || clipArea.getHeight()<0 || clipLogical.isEmpty()) return null;
            if (clipArea.getMaxX()<clipLogical.getMinX() ||
                clipArea.getMinX()>clipLogical.getMaxX() ||
                clipArea.getMaxY()<clipLogical.getMinY() ||
                clipArea.getMinY()>clipLogical.getMaxY()) return null;
            Rectangle2D.intersect(clipLogical, clipArea, clipLogical);
            /*
             * Conversion [coordonn�es logiques] --> [coordonn�es pixels].
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
            if (clipPixel.isEmpty()) return null;
            /*
             * Conversion [coordonn�es pixels] --> [coordonn�es logiques].
             *
             * 'clipLogical' ne devrait pas beaucoup changer (mais parfois un peu).
             */
            clipLogical.setRect(xmin + clipPixel.getMinX()  /scaleX,
                                ymax - clipPixel.getMaxY()  /scaleY,
                                       clipPixel.getWidth() /scaleX,
                                       clipPixel.getHeight()/scaleY);
        }
        /*
         * Avant d'effectuer la lecture, v�rifie si l'image est d�j� en m�moire. Une image
         * {@link RenderedImage} peut �tre en m�moire m�me si {@link GridCoverage} ne l'est
         * plus si, par exemple, l'image est entr�e dans une cha�ne d'op�rations de JAI.
         */
        RenderedImage image=null;
        if (renderedImage!=null)
        {
            image = (RenderedImage) renderedImage.get();
            if (image==null) renderedImage=null;
        }
        /*
         * A ce stade, nous disposons maintenant des coordonn�es en pixels
         * de la r�gion � charger. Proc�de maintenant � la lecture.
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
         * La lecture est maintenant termin�e et n'a pas �t� annul�e.
         * On construit maintenant l'objet {@link GridCoverage}, on le
         * conserve dans une cache interne puis on le retourne.
         */
        final double[] min = new double[] {clipLogical.getMinX(), clipLogical.getMinY(), ImageTableImpl.toJulian(startTime)};
        final double[] max = new double[] {clipLogical.getMaxX(), clipLogical.getMaxY(), ImageTableImpl.toJulian(  endTime)};
        GridCoverage coverage = new GridCoverage(filename, image, parameters.coordinateSystem,
                                new Envelope(min, max), categoryLists, format.geophysics, null,
                                Collections.singletonMap(ENTRY_KEY, this));
        /*
         * Si l'utilisateur a sp�cifi� une operation � appliquer
         * sur les images, applique cette op�ration maintenant.
         */
        GridCoverageProcessor processor = parameters.PROCESSOR;
        Operation             operation = parameters.operation;
        if (operation!=null)
        {
            coverage = processor.doOperation(operation, operation.getParameterList().setParameter("Source", coverage));
        }
        /*
         * Applique l'interpolation bicubique, conserve le
         * r�sultat dans une cache et retourne le r�sultat.
         */
        operation = processor.getOperation("Interpolate");
        coverage  = processor.doOperation(operation, operation.getParameterList().setParameter("Source", coverage).setParameter("Type", INTERPOLATIONS));
        renderedImage = new WeakReference(image);
        gridCoverage  = new SoftReference(coverage);
        return coverage;
    }

    /**
     * Annule la lecture de l'image. Cette m�thode peut �tre appel�e � partir de n'importe quel
     * thread. Si la m�thode {@link #getImage} �tait en train de lire une image dans un autre
     * thread, elle s'arr�tera et retournera <code>null</code>.
     */
    public void abort()
    {parameters.format.abort(this);}

    /**
     * Retourne une cha�ne de caract�res repr�sentant cette entr�e.
     */
    public String toString()
    {
        final StringBuffer buffer=new StringBuffer(40);
        buffer.append("ImageEntry"); // Pour ne pas avoir le "Impl" � la fin...
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
     * Indique si cette entr�e est identique � l'entr�e sp�cifi�e. Cette m�thode
     * v�rifie tous les param�tres de <code>ImageEntry</code>, incluant le chemin
     * de l'image et les coordonn�es g�ographiques de la r�gion qui a �t� demand�e.
     * Si vous souhaitez seulement v�rifier si deux objets <code>ImageEntry</code>
     * d�crivent bien la m�me image (m�me si les coordonn�es de la r�gion demand�e
     * sont diff�rentes), comparez plut�t leur num�ros {@link #getID}. Notez que
     * cette derni�re solution n'est valide que si les deux objets <code>ImageEntry</code>
     * proviennent bien de la m�me base de donn�es.
     */
    public boolean equals(final Object o)
    {return (o instanceof ImageEntryImpl) && equalsStrict((ImageEntryImpl) o);}

    /**
     * Indique si cette entr�e est strictement �gale � l'entr�e sp�cifi�e. Tous
     * les champs sont pris en compte, y compris ceux qui ne proviennent pas de
     * la base de donn�es (comme les coordonn�es de la r�gion d�sir�e par
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
     * Indique si l'image de cette entr�e couvre la
     * m�me r�gion g�ographique et la m�me plage de
     * temps que celles de l'entr� sp�cifi�e.   Les
     * deux entr�s peuvent toutefois appartenir �
     * des s�ries diff�rentes.
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
     * Indique si l'image de cette entr�e a la m�me dimension que l'image
     * sp�cifi�e. Cette m�thode ne v�rifie pas si les deux images couvrent
     * la m�me r�gion g�ographique.
     */
    private boolean sameSize(final ImageEntryImpl that)
    {return (this.width==that.width) && (this.height==that.height);}

    /**
     * Retourne un code repr�sentant cette entr�e.
     */
    public int hashCode()
    {return ID;}

    /**
     * Apr�s la lecture binaire, v�rifie si
     * l'entr�e lue existait d�j� en m�moire.
     */
    private Object readResolve() throws ObjectStreamException
    {return intern();}

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
    final ImageEntryImpl intern()
    {return (ImageEntryImpl) pool.intern(this);}

    /**
     * Applique {@link #intern()} sur un tableau d'entr�es.
     * Ce tableau peut contenir des �l�ments nuls.
     */
    static void intern(final ImageEntry[] entries)
    {pool.intern(entries);}

    /**
     * Si les deux images couvrent les m�mes coordonn�es spatio-temporelles,
     * retourne celle qui a la plus basse r�solution. Si les deux images ne
     * couvrent pas les m�mes coordonn�es ou si leurs r�solutions sont
     * incompatibles, alors cette m�thode retourne <code>null</code>.
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
     * Indique si cette image a au moins la r�solution sp�cifi�e.
     *
     * @param  resolution   R�solution d�sir�e, exprim�e selon le syst�me de coordonn�es
     *                      sp�cifi�. La conversion vers le syst�me de coordonn�es de
     *                      l'image sera faite automatiquement.
     * @param  sourceCS     Syst�me de coordonn�es de <code>resolution</code>.
     * @return <code>true</code> si la r�solution de cette image est �gale ou sup�rieure � la
     *         r�solution demand�e. Cette m�thode retourne <code>false</code> si <code>resolution</code>
     *         �tait nul ou si une projection cartographique a �chou�e.
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
