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

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Envelope;
import net.seagis.cv.CategoryList;
import net.seagis.gc.GridCoverage;
import net.seagis.gc.GridGeometry;
import net.seagis.cs.CoordinateSystem;

// Entr�s/sorties
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

// Divers
import fr.ird.sql.Entry;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;
import javax.swing.event.EventListenerList;


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
public interface ImageEntry extends Entry
{
    /**
     * Retourne la s�rie � laquelle
     * appartient cette image.
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
     */
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Retourne les coordonn�es spatio-temporelles de l'image. Le syst�me de
     * coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}.
     */
    public abstract Envelope getEnvelope();

    /**
     * Retourne la plage de temps couverte par l'image.   Cette plage sera d�limit�e
     * par des objets {@link Date}.  Appeler cette m�thode �quivant � n'extraire que
     * la partie temporelle de {@link #getEnvelope} et � transformer les coordonn�es
     * si n�cessaire.
     */
    public abstract Range getTimeRange();

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion couverte par l'image.
     * Les coordonn�es seront exprim�es en degr�s de longitudes et de latitudes
     * selon l'ellipso�de WGS 1984. Appeler cette m�thode �quivaut � n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et � transformer les
     * coordonn�es si n�cessaire.
     */
    public abstract Rectangle2D getGeographicArea();

    /**
     * Retourne les listes de cat�gories pour toutes les bandes de l'image. Les objets
     * {@link CategoryList} indiquent comment interpr�ter les valeurs des pixels.  Par
     * exemple, ils peuvent indiquer que la valeur 9 d�signe des nuages.
     *
     * @return La liste des cat�gories pour chaque bande de l'image.
     *         La longueur de ce tableau sera �gale au nombre de bandes.
     */
    public abstract CategoryList[] getCategoryLists();

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
    public abstract GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException;

    /**
     * Annule la lecture de l'image. Cette m�thode peut �tre appel�e � partir de n'importe quel
     * thread.  Si la m�thode {@link #getGridCoverage} �tait en train de lire une image dans un
     * autre thread, elle s'arr�tera et retournera <code>null</code>.
     */
    public abstract void abort();


    /**
     * Classe de base des objets {@link ImageEntry} qui d�l�gue tout ou une partir de leur travail
     * sur un autre objet {@link ImageEntry}. L'impl�mentation par d�faut de <code>Proxy</code>
     * redirige tous les appels des m�thodes vers l'objet {@link ImageEntry} qui a �t� sp�cifi�
     * lors de la construction. Les classes d�riv�es vont typiquement red�finir quelques m�thodes
     * afin d'ajouter ou de modifier certaines fonctionalit�es.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    public static class Proxy implements ImageEntry, Serializable
    {
        /**
         * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
         */
        private static final long serialVersionUID = 1679051552440633120L;

        /** Image envelopp�e par ce proxy. */ protected final ImageEntry entry;
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
         * m�me classe et enveloppent des objets {@link ImageEntry}
         * �gaux.
         */
        public boolean equals(final Object other)
        {return (other!=null && getClass().equals(other.getClass()) && ((Proxy) other).entry.equals(entry));}
    }
}
