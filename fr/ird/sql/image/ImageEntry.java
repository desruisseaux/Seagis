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

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Envelope;
import net.seagis.cv.CategoryList;
import net.seagis.gc.GridCoverage;
import net.seagis.gc.GridGeometry;
import net.seagis.cs.CoordinateSystem;

// Entrés/sorties
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

// Divers
import fr.ird.sql.Entry;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;
import javax.swing.event.EventListenerList;


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
public interface ImageEntry extends Entry
{
    /**
     * Retourne la série à laquelle
     * appartient cette image.
     */
    public abstract SeriesEntry getSeries();

    /**
     * Retourne le nom du fichier de l'image avec son chemin complet.
     * Si l'image n'est pas accessible localement (par exemple si elle
     * est produite par un serveur distant), alors cette méthode peut
     * retourner <code>null</code>.
     */
    public abstract File getFile();

    /**
     * Retourne des informations sur la géométrie de l'image. Ces informations
     * comprennent notamment la taille de l'image  (en pixels)    ainsi que la
     * transformation à utiliser pour passer des coordonnées pixels   vers les
     * coordonnées du système {@link #getCoordinateSystem}. Cette dernière sera
     * le plus souvent une transformation affine.
     */
    public abstract GridGeometry getGridGeometry();

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
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Retourne les coordonnées spatio-temporelles de l'image. Le système de
     * coordonnées utilisé est celui retourné par {@link #getCoordinateSystem}.
     */
    public abstract Envelope getEnvelope();

    /**
     * Retourne la plage de temps couverte par l'image.   Cette plage sera délimitée
     * par des objets {@link Date}.  Appeler cette méthode équivant à n'extraire que
     * la partie temporelle de {@link #getEnvelope} et à transformer les coordonnées
     * si nécessaire.
     */
    public abstract Range getTimeRange();

    /**
     * Retourne les coordonnées géographiques de la région couverte par l'image.
     * Les coordonnées seront exprimées en degrés de longitudes et de latitudes
     * selon l'ellipsoïde WGS 1984. Appeler cette méthode équivaut à n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et à transformer les
     * coordonnées si nécessaire.
     */
    public abstract Rectangle2D getGeographicArea();

    /**
     * Retourne les listes de catégories pour toutes les bandes de l'image. Les objets
     * {@link CategoryList} indiquent comment interpréter les valeurs des pixels.  Par
     * exemple, ils peuvent indiquer que la valeur 9 désigne des nuages.
     *
     * @return La liste des catégories pour chaque bande de l'image.
     *         La longueur de ce tableau sera égale au nombre de bandes.
     */
    public abstract CategoryList[] getCategoryLists();

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
    public abstract GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException;

    /**
     * Annule la lecture de l'image. Cette méthode peut être appelée à partir de n'importe quel
     * thread.  Si la méthode {@link #getGridCoverage} était en train de lire une image dans un
     * autre thread, elle s'arrêtera et retournera <code>null</code>.
     */
    public abstract void abort();


    /**
     * Classe de base des objets {@link ImageEntry} qui délègue tout ou une partir de leur travail
     * sur un autre objet {@link ImageEntry}. L'implémentation par défaut de <code>Proxy</code>
     * redirige tous les appels des méthodes vers l'objet {@link ImageEntry} qui a été spécifié
     * lors de la construction. Les classes dérivées vont typiquement redéfinir quelques méthodes
     * afin d'ajouter ou de modifier certaines fonctionalitées.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    public static class Proxy implements ImageEntry, Serializable
    {
        /**
         * Numéro de série (pour compatibilité avec des versions antérieures).
         */
        private static final long serialVersionUID = 1679051552440633120L;

        /** Image enveloppée par ce proxy. */ protected final ImageEntry entry;
        /** Construit un proxy.            */ protected Proxy(final ImageEntry entry) {this.entry=entry; if (entry==null) throw new NullPointerException();}
        /** Redirige vers {@link #entry}.  */ public int              getID()               {return entry.getID();}
        /** Redirige vers {@link #entry}.  */ public SeriesEntry      getSeries()           {return entry.getSeries();}
        /** Redirige vers {@link #entry}.  */ public String           getName()             {return entry.getName();}
        /** Redirige vers {@link #entry}.  */ public File             getFile()             {return entry.getFile();}
        /** Redirige vers {@link #entry}.  */ public GridGeometry     getGridGeometry()     {return entry.getGridGeometry();}
        /** Redirige vers {@link #entry}.  */ public CoordinateSystem getCoordinateSystem() {return entry.getCoordinateSystem();}
        /** Redirige vers {@link #entry}.  */ public Envelope         getEnvelope()         {return entry.getEnvelope();}
        /** Redirige vers {@link #entry}.  */ public Range            getTimeRange()        {return entry.getTimeRange();}
        /** Redirige vers {@link #entry}.  */ public Rectangle2D      getGeographicArea()   {return entry.getGeographicArea();}
        /** Redirige vers {@link #entry}.  */ public CategoryList[]   getCategoryLists()    {return entry.getCategoryLists();}
        /** Redirige vers {@link #entry}.  */ public String           toString()            {return entry.toString();}
        /** Redirige vers {@link #entry}.  */ public int              hashCode()            {return entry.hashCode();}
        /** Redirige vers {@link #entry}.  */ public void             abort()               {entry.abort();}

        /**
         * Redirige vers {@link #entry}.
         */
        public GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException
        {return entry.getGridCoverage(listenerList);}

        /**
         * Retourne <code>true</code> si les deux objets sont de la
         * même classe et enveloppent des objets {@link ImageEntry}
         * égaux.
         */
        public boolean equals(final Object other)
        {return (other!=null && getClass().equals(other.getClass()) && ((Proxy) other).entry.equals(entry));}
    }
}
