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
package fr.ird.database.coverage;

// J2SE et JAI
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;
import javax.swing.event.EventListenerList;

// Geotools
import org.geotools.pt.Envelope;
import org.geotools.gc.GridCoverage;
import org.geotools.gc.GridGeometry;
import org.geotools.cv.SampleDimension;
import org.geotools.cs.CoordinateSystem;

// Seagis
import fr.ird.database.Entry;


/**
 * Information sur une image. Un objet <code>CoverageEntry</code> correspond �
 * un enregistrement de la base de donn�es d'images. Ces informations sont retourn�es
 * par la m�thode {@link CoverageTable#getEntries}. Les objets
 * <code>CoverageEntry</code> sont imutables et s�curitaires dans un environnement
 * multi-threads.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CoverageEntry extends Entry {
    /**
     * Cl� sous laquelle m�moriser l'objet <code>CoverageEntry</code> source
     * dans les propri�t�s de {@link GridCoverage}. Cette propri�t�s permet de retrouver
     * l'objet <code>CoverageEntry</code> source par exemple dans le code suivant:
     *
     * <blockquote><pre>
     * CoverageEntry   entry = ...
     * GridCoverage coverage = entry.getGridCoverage(null);
     * Object         source = coverage.getProperty(CoverageEntry.SOURCE_KEY);
     * assert source == entry;
     * </pre></blockquote>
     */
    public static final String SOURCE_KEY = "fr.ird.database.CoverageEntry";

    /**
     * Retourne la s�rie � laquelle appartient cette image.
     */
    public abstract SeriesEntry getSeries();

    /**
     * Retourne le nom du fichier de l'image avec son chemin complet.
     * Si l'image n'est pas accessible localement (par exemple si elle
     * est produite par un serveur distant), alors cette m�thode peut
     * retourner <code>null</code>.
     */
    public abstract File getFile();

    /**
     * Retourne des informations sur la g�om�trie de l'image. Ces informations
     * comprennent notamment la taille de l'image  (en pixels)    ainsi que la
     * transformation � utiliser pour passer des coordonn�es pixels   vers les
     * coordonn�es du syst�me {@link #getCoordinateSystem}. Cette derni�re sera
     * le plus souvent une transformation affine.
     * <br><br>
     * Notez que la g�om�trie retourn�e par cette m�thode ne prend pas en compte un �ventuel
     * "clip" sp�cifi� par {@link CoverageTable#setGeographicArea}. Il s'agit de la g�om�trie
     * de l'image telle qu'elle est d�clar�e dans la base de donn�es, ind�pendamment de la
     * fa�on dont elle sera lue. L'image qui sera retourn�e par {@link #getGridCoverage}
     * peut avoir une g�om�trie diff�rente si un clip et/ou une d�cimation ont �t� appliqu�s.
     */
    public abstract GridGeometry getGridGeometry();

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
     * � interroger la base de donn�es d'images ({@link CoverageTable#getCoordinateSystem}).
     */
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Retourne les coordonn�es spatio-temporelles de l'image. Le syst�me de
     * coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}.
     * Notez que l'envelope retourn�e ne comprend pas le "clip" sp�cifi�e � la
     * table d'image (voir {@link CoverageTable#setGeographicArea}). La couverture
     * retourn�e par {@link #getGridCoverage} peut donc avoir une envelope plus
     * petite que celle retourn�e par cette m�thode.
     */
    public abstract Envelope getEnvelope();

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion couverte par l'image.
     * Les coordonn�es seront exprim�es en degr�s de longitudes et de latitudes
     * selon l'ellipso�de WGS 1984. Appeler cette m�thode �quivaut � n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et � transformer les
     * coordonn�es si n�cessaire.
     */
    public abstract Rectangle2D getGeographicArea();

    /**
     * Retourne la plage de temps couverte par l'image.   Cette plage sera d�limit�e
     * par des objets {@link Date}.  Appeler cette m�thode �quivaut � n'extraire que
     * la partie temporelle de {@link #getEnvelope} et � transformer les coordonn�es
     * si n�cessaire.
     */
    public abstract Range getTimeRange();

    /**
     * Retourne les bandes de l'image. Les objets {@link SampleDimension} indiquent
     * comment interpr�ter les valeurs des pixels. Par exemple, ils peuvent indiquer
     * que la valeur 9 d�signe des nuages.
     *
     * Contrairement � {@link FormatEntry#getSampleDimensions FormatEntry}, cette m�thode retourne
     * toujours la version des <code>SampleDimensions</code> correspondant aux valeurs r�elles
     * (<code>{@link SampleDimension#geophysics geophysics}(false)</code>), ceci afin d'�tre
     * coh�rent avec la m�thode {@link #getGridCoverage getGridCoverage(...)}.
     *
     * @return La liste des cat�gories g�ophysiques pour chaque bande de l'image.
     *         La longueur de ce tableau sera �gale au nombre de bandes.
     */
    public abstract SampleDimension[] getSampleDimensions();

    /**
     * Retourne l'image correspondant � cette entr�e. Si l'image avait d�j� �t� lue pr�c�demment
     * et qu'elle n'a pas encore �t� r�clam�e par le ramasse-miette, alors l'image existante sera
     * retourn�e sans qu'une nouvelle lecture du fichier ne soit n�cessaire. Si au contraire l'image
     * n'�tait pas d�j� en m�moire, alors un d�codage du fichier sera n�cessaire. Toutefois, cette
     * m�thode ne d�codera pas n�cessairement l'ensemble de l'image. Par d�faut, elle ne d�code que
     * la r�gion qui avait �t� indiqu�e � {@link CoverageTable#setEnvelope} et sous-�chantillonne
     * � la r�solution qui avait �t� indiqu�e � {@link CoverageTable#setPreferredResolution}
     * (<strong>note:</strong> cette r�gion et ce sous-�chantillonage sont ceux qui �taient actifs
     * au moment o� {@link CoverageTable#getEntries} a �t� appel�e; les changement subs�quents
     * des param�tres de {@link CoverageTable} n'ont pas d'effets sur les
     * <code>CoverageEntry</code> d�j� cr��s).
     *
     * @param  listenerList Liste des objets � informer des progr�s de la lecture ainsi que des
     *         �ventuels avertissements, ou <code>null</code> s'il n'y en a pas.  Cette m�thode
     *         prend en compte tous les objets qui ont �t� inscrits sous la classe
     *         {@link IIOReadWarningListener} ou {@link IIOReadProgressListener}, et ignore tous
     *         les autres. Cette m�thode s'engage � ne pas modifier l'objet {@link EventListenerList}
     *         donn�; il est donc s�curitaire de passer directement la liste
     *         {@link javax.swing.JComponent#listenerList} d'une interface utilisateur, m�me dans
     *         un environnement multi-threads. Un objet {@link EventListenerList} peut aussi �tre
     *         construit comme suit:
     *         <blockquote><pre>
     *         {@link IIOReadProgressListener} progressListener = ...
     *         {@link IIOReadWarningListener}   warningListener = ...
     *         {@link EventListenerList}  listenerList = new EventListenerList();
     *         listenerList.add(IIOReadProgressListener.class, progressListener);
     *         listenerList.add(IIOReadWarningListener.class,   warningListener);
     *         </pre></blockquote>
     *
     * @return Image lue, ou <code>null</code> si l'image n'intercepte pas la r�gion g�ographique
     *         ou la plage de temps qui avaient �t� sp�cifi�es � {@link CoverageTable}, ou si
     *         l'utilisateur a interrompu la lecture. Cette m�thode retourne toujours la version
     *         geophysiques de l'image (<code>{@link GridCoverage#geophysics geophysics}(true)</code>).
     * @throws IOException si le fichier n'a pas �t� trouv� ou si une autre erreur d'entr�s/sorties
     *         est survenue.
     * @throws IIOException s'il n'y a pas de d�codeur appropri� pour l'image, ou si l'image n'est
     *         pas valide.
     */
    public abstract GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException;

    /**
     * Annule la lecture de l'image. Cette m�thode peut �tre appel�e � partir de n'importe quel
     * thread.  Si la m�thode {@link #getGridCoverage} �tait en train de lire une image dans un
     * autre thread, elle s'arr�tera et retournera <code>null</code>.
     */
    public abstract void abort();


    /**
     * Classe de base des objets {@link CoverageEntry} qui d�l�gue tout ou une partir de leur
     * travail sur un autre objet {@link CoverageEntry}. L'impl�mentation par d�faut de
     * <code>Proxy</code> redirige tous les appels des m�thodes vers l'objet {@link CoverageEntry}
     * qui a �t� sp�cifi� lors de la construction. Les classes d�riv�es vont typiquement red�finir
     * quelques m�thodesafin d'ajouter ou de modifier certaines fonctionalit�es.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static class Proxy implements CoverageEntry, Serializable {
        /**
         * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
         */
        private static final long serialVersionUID = 1679051552440633120L;

        /** Image envelopp�e par ce proxy. */ protected final CoverageEntry entry;
        /** Construit un proxy.            */ protected Proxy(final CoverageEntry entry) {this.entry=entry; if (entry==null) throw new NullPointerException();}
        /** Redirige vers {@link #entry}.  */ public int               getID()               {return entry.getID();}
        /** Redirige vers {@link #entry}.  */ public SeriesEntry       getSeries()           {return entry.getSeries();}
        /** Redirige vers {@link #entry}.  */ public String            getName()             {return entry.getName();}
        /** Redirige vers {@link #entry}.  */ public String            getRemarks()          {return entry.getRemarks();}
        /** Redirige vers {@link #entry}.  */ public File              getFile()             {return entry.getFile();}
        /** Redirige vers {@link #entry}.  */ public GridGeometry      getGridGeometry()     {return entry.getGridGeometry();}
        /** Redirige vers {@link #entry}.  */ public CoordinateSystem  getCoordinateSystem() {return entry.getCoordinateSystem();}
        /** Redirige vers {@link #entry}.  */ public Envelope          getEnvelope()         {return entry.getEnvelope();}
        /** Redirige vers {@link #entry}.  */ public Range             getTimeRange()        {return entry.getTimeRange();}
        /** Redirige vers {@link #entry}.  */ public Rectangle2D       getGeographicArea()   {return entry.getGeographicArea();}
        /** Redirige vers {@link #entry}.  */ public SampleDimension[] getSampleDimensions() {return entry.getSampleDimensions();}
        /** Redirige vers {@link #entry}.  */ public String            toString()            {return entry.toString();}
        /** Redirige vers {@link #entry}.  */ public int               hashCode()            {return entry.hashCode();}
        /** Redirige vers {@link #entry}.  */ public void              abort()               {entry.abort();}

        /**
         * Redirige vers {@link #entry}.
         */
        public GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException {
            return entry.getGridCoverage(listenerList);
        }

        /**
         * Retourne <code>true</code> si les deux objets sont de la m�me classe
         * et enveloppent des objets {@link CoverageEntry} �gaux.
         */
        public boolean equals(final Object other) {
            return (other!=null && getClass().equals(other.getClass()) && ((Proxy) other).entry.equals(entry));
        }
    }
}
