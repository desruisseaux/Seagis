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
package fr.ird.database.coverage;

// J2SE et JAI
import java.util.Date;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;
import javax.swing.event.EventListenerList;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;

// Geotools
import org.geotools.pt.Envelope;
import org.geotools.gc.GridCoverage;
import org.geotools.gc.GridGeometry;
import org.geotools.cv.SampleDimension;
import org.geotools.cs.CoordinateSystem;

// Seagis
import fr.ird.database.Entry;


/**
 * Information sur une image. Un objet <code>CoverageEntry</code> correspond à
 * un enregistrement de la base de données d'images. Ces informations sont retournées
 * par la méthode {@link CoverageTable#getEntries}. Les objets
 * <code>CoverageEntry</code> sont imutables et sécuritaires dans un environnement
 * multi-threads.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CoverageEntry extends Entry, Remote {
    /**
     * Clé sous laquelle mémoriser l'objet <code>CoverageEntry</code> source
     * dans les propriétés de {@link GridCoverage}. Cette propriétés permet de retrouver
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
     * Retourne la série à laquelle appartient cette image.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract SeriesEntry getSeries() throws RemoteException;

    /**
     * Retourne le chemin absolu de l'image, ou <code>null</code> si le fichier n'est
     * pas accessible localement. Dans ce dernier cas, {@link #getURL} devrait être
     * utilisé à la place.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract File getFile() throws RemoteException;

    /**
     * Retourne l'URL de l'image, ou <code>null</code> si le fichier n'est pas accessible.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract URL getURL() throws RemoteException;

    /**
     * Retourne des informations sur la géométrie de l'image. Ces informations
     * comprennent notamment la taille de l'image  (en pixels)    ainsi que la
     * transformation à utiliser pour passer des coordonnées pixels   vers les
     * coordonnées du système {@link #getCoordinateSystem}. Cette dernière sera
     * le plus souvent une transformation affine.
     * <br><br>
     * Notez que la géométrie retournée par cette méthode ne prend pas en compte un éventuel
     * "clip" spécifié par {@link CoverageTable#setGeographicArea}. Il s'agit de la géométrie
     * de l'image telle qu'elle est déclarée dans la base de données, indépendamment de la
     * façon dont elle sera lue. L'image qui sera retournée par {@link #getGridCoverage}
     * peut avoir une géométrie différente si un clip et/ou une décimation ont été appliqués.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract GridGeometry getGridGeometry() throws RemoteException;

    /**
     * Retourne le système de coordonnées de l'image. En général, ce système
     * de coordonnées aura trois dimensions  (la dernière dimension étant le
     * temps), soit dans l'ordre:
     * <ul>
     *   <li>Les longitudes, en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Les latitudes,  en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Le temps, en jours juliens depuis le 01/01/1950 00:00 UTC.</li>
     * </ul>
     *
     * Notez que ce système de coordonnées peut ne pas être le même qui celui qui sert
     * à interroger la base de données d'images ({@link CoverageTable#getCoordinateSystem}).
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract CoordinateSystem getCoordinateSystem() throws RemoteException;

    /**
     * Retourne les coordonnées spatio-temporelles de l'image. Le système de
     * coordonnées utilisé est celui retourné par {@link #getCoordinateSystem}.
     * Notez que l'envelope retournée ne comprend pas le "clip" spécifiée à la
     * table d'image (voir {@link CoverageTable#setGeographicArea}). La couverture
     * retournée par {@link #getGridCoverage} peut donc avoir une envelope plus
     * petite que celle retournée par cette méthode.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract Envelope getEnvelope() throws RemoteException;

    /**
     * Retourne les coordonnées géographiques de la région couverte par l'image.
     * Les coordonnées seront exprimées en degrés de longitudes et de latitudes
     * selon l'ellipsoïde WGS 1984. Appeler cette méthode équivaut à n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et à transformer les
     * coordonnées si nécessaire.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract Rectangle2D getGeographicArea() throws RemoteException;

    /**
     * Retourne la plage de temps couverte par l'image.   Cette plage sera délimitée
     * par des objets {@link Date}.  Appeler cette méthode équivaut à n'extraire que
     * la partie temporelle de {@link #getEnvelope} et à transformer les coordonnées
     * si nécessaire.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract Range getTimeRange() throws RemoteException;

    /**
     * Retourne les bandes de l'image. Les objets {@link SampleDimension} indiquent
     * comment interpréter les valeurs des pixels. Par exemple, ils peuvent indiquer
     * que la valeur 9 désigne des nuages.
     *
     * Contrairement à {@link FormatEntry#getSampleDimensions FormatEntry}, cette méthode retourne
     * toujours la version des <code>SampleDimensions</code> correspondant aux valeurs réelles
     * (<code>{@link SampleDimension#geophysics geophysics}(false)</code>), ceci afin d'être
     * cohérent avec la méthode {@link #getGridCoverage getGridCoverage(...)}.
     *
     * @return La liste des catégories géophysiques pour chaque bande de l'image.
     *         La longueur de ce tableau sera égale au nombre de bandes.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract SampleDimension[] getSampleDimensions() throws RemoteException;

    /**
     * Retourne l'image correspondant à cette entrée. Si l'image avait déjà été lue précédemment
     * et qu'elle n'a pas encore été réclamée par le ramasse-miette, alors l'image existante sera
     * retournée sans qu'une nouvelle lecture du fichier ne soit nécessaire. Si au contraire l'image
     * n'était pas déjà en mémoire, alors un décodage du fichier sera nécessaire. Toutefois, cette
     * méthode ne décodera pas nécessairement l'ensemble de l'image. Par défaut, elle ne décode que
     * la région qui avait été indiquée à {@link CoverageTable#setEnvelope} et sous-échantillonne
     * à la résolution qui avait été indiquée à {@link CoverageTable#setPreferredResolution}
     * (<strong>note:</strong> cette région et ce sous-échantillonage sont ceux qui étaient actifs
     * au moment où {@link CoverageTable#getEntries} a été appelée; les changement subséquents
     * des paramètres de {@link CoverageTable} n'ont pas d'effets sur les
     * <code>CoverageEntry</code> déjà créés).
     *
     * @param  listenerList Liste des objets à informer des progrès de la lecture ainsi que des
     *         éventuels avertissements, ou <code>null</code> s'il n'y en a pas.  Cette méthode
     *         prend en compte tous les objets qui ont été inscrits sous la classe
     *         {@link IIOReadWarningListener} ou {@link IIOReadProgressListener}, et ignore tous
     *         les autres. Cette méthode s'engage à ne pas modifier l'objet {@link EventListenerList}
     *         donné; il est donc sécuritaire de passer directement la liste
     *         {@link javax.swing.JComponent#listenerList} d'une interface utilisateur, même dans
     *         un environnement multi-threads. Un objet {@link EventListenerList} peut aussi être
     *         construit comme suit:
     *         <blockquote><pre>
     *         {@link IIOReadProgressListener} progressListener = ...
     *         {@link IIOReadWarningListener}   warningListener = ...
     *         {@link EventListenerList}  listenerList = new EventListenerList();
     *         listenerList.add(IIOReadProgressListener.class, progressListener);
     *         listenerList.add(IIOReadWarningListener.class,   warningListener);
     *         </pre></blockquote>
     *
     * @return Image lue, ou <code>null</code> si l'image n'intercepte pas la région géographique
     *         ou la plage de temps qui avaient été spécifiées à {@link CoverageTable}, ou si
     *         l'utilisateur a interrompu la lecture. Cette méthode retourne toujours la version
     *         geophysiques de l'image (<code>{@link GridCoverage#geophysics geophysics}(true)</code>).
     * @throws IOException si le fichier n'a pas été trouvé ou si une autre erreur d'entrés/sorties
     *         est survenue.
     * @throws IIOException s'il n'y a pas de décodeur approprié pour l'image, ou si l'image n'est
     *         pas valide.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException;

    /**
     * Annule la lecture de l'image. Cette méthode peut être appelée à partir de n'importe quel
     * thread.  Si la méthode {@link #getGridCoverage} était en train de lire une image dans un
     * autre thread, elle s'arrêtera et retournera <code>null</code>.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract void abort() throws RemoteException;


    /**
     * Classe de base des objets {@link CoverageEntry} qui délègue tout ou une partie de leur
     * travail sur un autre objet {@link CoverageEntry}. L'implémentation par défaut de
     * <code>Proxy</code> redirige tous les appels des méthodes vers l'objet {@link CoverageEntry}
     * qui a été spécifié lors de la construction. Les classes dérivées vont typiquement redéfinir
     * quelques méthodes afin d'ajouter ou de modifier certaines fonctionalitées.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static class Proxy implements CoverageEntry, Serializable {
        /**
         * Numéro de série (pour compatibilité avec des versions antérieures).
         */
        private static final long serialVersionUID = 1679051552440633120L;

        /**
         * Image enveloppée par ce proxy.
         */
        public final CoverageEntry entry;

        /**
         * Construit un proxy qui redirigera tous les appels vers l'entrée spécifiée.
         */
        protected Proxy(final CoverageEntry entry) {
            this.entry = entry;
            if (entry == null) {
                throw new NullPointerException();
            }
        }

        /** Redirige vers {@link #entry}. */ public SeriesEntry       getSeries()           throws RemoteException {return entry.getSeries();}
        /** Redirige vers {@link #entry}. */ public String            getName()             throws RemoteException {return entry.getName();}
        /** Redirige vers {@link #entry}. */ public String            getRemarks()          throws RemoteException {return entry.getRemarks();}
        /** Redirige vers {@link #entry}. */ public File              getFile()             throws RemoteException {return entry.getFile();}
        /** Redirige vers {@link #entry}. */ public URL               getURL()              throws RemoteException {return entry.getURL();}
        /** Redirige vers {@link #entry}. */ public GridGeometry      getGridGeometry()     throws RemoteException {return entry.getGridGeometry();}
        /** Redirige vers {@link #entry}. */ public CoordinateSystem  getCoordinateSystem() throws RemoteException {return entry.getCoordinateSystem();}
        /** Redirige vers {@link #entry}. */ public Envelope          getEnvelope()         throws RemoteException {return entry.getEnvelope();}
        /** Redirige vers {@link #entry}. */ public Range             getTimeRange()        throws RemoteException {return entry.getTimeRange();}
        /** Redirige vers {@link #entry}. */ public Rectangle2D       getGeographicArea()   throws RemoteException {return entry.getGeographicArea();}
        /** Redirige vers {@link #entry}. */ public SampleDimension[] getSampleDimensions() throws RemoteException {return entry.getSampleDimensions();}
        /** Redirige vers {@link #entry}. */
        public GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException {
            return entry.getGridCoverage(listenerList);
        }

        /** Redirige vers {@link #entry}. */ public void   abort() throws RemoteException {entry.abort();}
        /** Redirige vers {@link #entry}. */ public String toString() {return entry.toString();}
        /** Redirige vers {@link #entry}. */ public int    hashCode() {return entry.hashCode();}

        /**
         * Retourne <code>true</code> si les deux objets sont de la même classe
         * et enveloppent des objets {@link CoverageEntry} égaux.
         */
        public boolean equals(final Object other) {
            return (other!=null && getClass().equals(other.getClass()) && ((Proxy) other).entry.equals(entry));
        }
    }
}
