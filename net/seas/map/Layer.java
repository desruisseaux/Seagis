/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.map;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.cs.GeographicCoordinateSystem;

// G�om�trie et graphisme
import java.awt.Shape;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;
import javax.media.jai.GraphicsJAI;
import java.awt.geom.AffineTransform;
import net.seas.util.XAffineTransform;

// Ev�nements
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.event.EventListenerList;

// Journal
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Interface utilisateur
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

// Divers
import java.util.Locale;
import java.io.Serializable;
import net.seas.util.XClass;
import net.seas.util.Version;
import net.seas.resources.Resources;
import net.seas.awt.ExceptionMonitor;


/**
 * Classe de base des couches pouvant �tre affich�es dans un objet {@link MapPanel}.
 * Cette couche peut avoir son propre syst�me de coordonn�es, pas n�cessairement le
 * m�me que celui de l'affichage.    Toutes les m�thodes (sauf une) de cette classe
 * re�oivent et retournent des coordonn�es exprim�es selon le syst�me de coordonn�es
 * propre � cette couche. La seule exception est la m�thode {@link #paint paint(...)},
 * la seule � qui incombe la responsabilit� de convertir les coordonn�es de cette couche
 * (exprim�es selon le syst�me retourn� par {@link #getCoordinateSystem}) en coordonn�es
 * d'affichage (exprim�es selon le syst�me retourn� par {@link MapPanel#getCoordinateSystem}).
 * La m�thode {@link #paint paint(...)} recevra en argument les informations n�cessaires pour
 * effectuer la conversion. A part cette m�thode, aucune autre m�thode de la classe
 * <code>Layer</code> n'a d'information sur le syst�me de coordonn�es de l'affichage.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class Layer implements Serializable
{
    /**
     * Graphique {@link MapPanel} auquel appartiennent les couches. Cette composante
     * ne devrait jamais �tre accessibles aux classes d�riv�es   (quelle que soit la
     * fa�on) afin de r�duire les risques de "deadlock".
     */
    transient JComponent mapPanel;

    /**
     * Forme g�om�trique englobant la r�gion dans laquelle la couche a �t� dessin�e lors du
     * dernier appel de la m�thode {@link #paint}.  Les coordonn�es de cette r�gion doivent
     * �tre en exprim�es en coordonn�es pixels de l'�cran.
     */
    private transient Shape shape;

    /**
     * Cha�ne de caract�res indiquant qu'un tra�age est en cours.
     * Cette n'est utilis�e que pour le journal et ne sera construite
     * que la premi�re fois o� elle sera n�cessaire.
     */
    private transient String painting;

    /**
     * Syst�me de coordonn�es utilis� pour cette couche. Les m�thodes {@link #getPreferredArea}
     * et {@link #setPreferredArea} utilisent ce syst�me de coordonn�es. Ce champ ne doit jamais
     * �tre nul.
     */
    private final CoordinateSystem coordinateSystem;

    /**
     * Coordonn�es g�ographiques couvertes par cette couche. Ces coordonn�es doivent
     * �tre exprim�ees selon le syst�me de coordonn�es <code>coordinateSystem</code>.
     * Une valeur nulle signifie que cette couche n'a pas de limites bien d�limit�es.
     */
    private Rectangle2D preferredArea;

    /**
     * Dimension pr�f�r�e des pixels pour un zoom rapproch�. Une valeur
     * nulle signifie qu'aucune dimension pr�f�r�e n'a �t� sp�cifi�e.
     */
    private Dimension2D preferredPixelSize;

    /**
     * Indique si cette couche est visible. Les couches sont invisibles par
     * d�faut. L'appel de {@link MapPanel#addLayer} appelera syst�matiquement
     * <code>setVisible(true)</code>.
     *
     * @see #setVisible
     */
    private boolean visible;

    /**
     * Ordre <var>z</var> � laquelle cette couche doit �tre dessin�e. Les couches avec un <var>z</var> �lev�
     * seront dessin�es par dessus les couches avec un <var>z</var> bas. Typiquement, cet ordre <var>z</var>
     * devrait �tre l'altitude en m�tres de la couche (par exemple -30 pour l'isobath � 30 m�tres de profondeur).
     * La valeur {@link Float#POSITIVE_INFINITY} fait dessiner une couche par dessus tout le reste, tandis que la
     * valeur {@link Float#NEGATIVE_INFINITY} la fait dessiner en dessous. La valeur {@link Float#NaN} n'est pas
     * valide. La valeur par d�faut est {@link Float#POSITIVE_INFINITY}.
     *
     * @see #getZOrder
     * @see #setZOrder
     */
    private float zOrder=Float.POSITIVE_INFINITY;

    /**
     * Liste des objets int�ress�s � �tre inform�s des
     * changements apport�s � cet objet <code>Layer</code>.
     */
    private final EventListenerList listenerList=new EventListenerList();

    /**
     * Construit une couche cartographique avec un syst�me
     * de coordonn�es par d�faut. Ce syst�me utilisera des
     * (<var>longitude</var>,<var>latitude</var>) exprim�es
     * selon l'ellipso�de "WGS 1984".
     */
    public Layer()
    {this(GeographicCoordinateSystem.WGS84);}

    /**
     * Construit une couche cartographique avec
     * le syst�me de coordonn�es sp�cifi�.
     *
     * @param coordinateSystem syst�me de coordonn�es de cette couche. Ce
     *        syst�me sera retourn� par la m�thode {@link #getCoordinateSystem}.
     */
    public Layer(final CoordinateSystem coordinateSystem)
    {this.coordinateSystem=coordinateSystem;}

    /**
     * Retourne le nom de cette couche. L'impl�mentation par
     * d�faut retourne le nom de la classe avec son ordre Z.
     */
    public String getName()
    {return XClass.getShortClassName(this)+'['+getZOrder()+']';}

    /**
     * Retourne le syst�me de coordonn�es de cette couche. Les coordonn�es g�ographiques
     * retourn�es par {@link #getPreferredArea} seront exprim�es selon ce syst�me.
     */
    public final CoordinateSystem getCoordinateSystem()
    {return coordinateSystem;}

    /**
     * Retourne les coordonn�es g�ographiques de cette couche. Les coordonn�es retourn�es ne sont
     * pas oblig�es d'englober toute la couche (quoique ce soit souvent le cas).  Elles indiquent
     * plut�t la partie de la couche que l'on souhaite voir appara�tre. Le rectangle retourn� sera
     * exprim� selon le syst�me de coordonn�es retourn� par {@link #getCoordinateSystem}. Si cette
     * couche n'a pas de limites g�ographiques bien d�finies (par exemple si elle n'est qu'une
     * l�gende ou l'�chelle de la carte), alors cette m�thode peut retourner <code>null</code>.
     */
    public final Rectangle2D getPreferredArea()
    {
        final Rectangle2D preferredArea=this.preferredArea;
        return (preferredArea!=null) ? (Rectangle2D) preferredArea.clone() : null;
    }

    /**
     * Modifie les coordonn�es g�ographiques de cette couche. L'appel de cette m�thode ne modifie
     * par le g�or�f�rencement; elle affecte simplement la r�gion qui sera affich�e par d�faut dans
     * une fen�tre.
     */
    public synchronized void setPreferredArea(final Rectangle2D area)
    {
        shape=null;
        firePropertyChange("preferredArea", preferredArea, preferredArea=(area!=null) ? (Rectangle2D) area.clone() : null);
    }

    /**
     * Returns the preferred pixel size for a close
     * zoom, or <code>null</code> if there is none.
     */
    public final Dimension2D getPreferredPixelSize()
    {
        final Dimension2D preferredPixelSize=this.preferredPixelSize;
        return (preferredPixelSize!=null) ? (Dimension2D) preferredPixelSize.clone() : null;
    }

    /**
     * Set the preferred pixel size for a close zoom. For image layers, the preferred pixel
     * size is the image's pixel size (in units of {@link #getCoordinateSystem}). For other
     * kind of layer, this "pixel" size should be some raisonable resolution matching layer's
     * capability. For example for a layer drawing an isoline, it may be set to the isoline's
     * resolution.
     *
     * @param size The preferred pixel size, or <code>null</code> if there is none.
     */
    public synchronized void setPreferredPixelSize(final Dimension2D size)
    {firePropertyChange("preferredPixelSize", preferredPixelSize, preferredPixelSize=(size!=null) ? (Dimension2D)size.clone() : null);}

    /**
     * Retourne l'ordre <var>z</var> � laquelle cette couche devrait �tre dessin�e.
     * Les couches avec un <var>z</var> �lev� seront dessin�es par dessus celles qui
     * ont <var>z</var> plus bas. La valeur retourn�e par d�faut est
     * {@link Float#POSITIVE_INFINITY}.
     *
     * @see #setZOrder
     */
    public final float getZOrder()
    {return zOrder;}

    /**
     * Modifie l'altitude <var>z</var> � laquelle sera dessin�e cette couche.
     * La valeur sp�cifi�e viendra remplacer la valeur par d�faut que retournait
     * normalement {@link #getZOrder}.
     *
     * @throws IllegalArgumentException si <code>zorder</code> est {@link Float#NaN}.
     */
    public synchronized void setZOrder(final float zOrder) throws IllegalArgumentException
    {
        if (!Float.isNaN(zOrder))
        {
            final float oldZOrder=this.zOrder;
            if (zOrder!=oldZOrder)
            {
                this.zOrder=zOrder;
                repaint();
                firePropertyChange("zOrder", new Float(oldZOrder), new Float(zOrder));
            }
        }
        else throw new IllegalArgumentException();
    }

    /**
     * Indique si cette couche
     * est visible ou non.
     */
    public final boolean isVisible()
    {return visible;}

    /**
     * Sp�cifie si cette couche doit �tre visible ou non. Cette m�thode peut �tre
     * appel�e pour cacher momentan�ment une couche. Elle est aussi appel�e de
     * fa�on syst�matique lorsque cette couche est ajout�e ou retir�e d'une carte
     * {@link MapPanel}:
     *
     * <ul>
     *   <li><code>{@link MapPanel#addLayer MapPanel.addLayer}(this)</code>
     *       appelera <code>setVisible(true)</code>. Les classes d�riv�es peuvent profiter
     *       de cette sp�cification pour s'enregistrer aupr�s de {@link MapPanel} comme �tant
     *       int�ress�es � suivre les mouvements de la souris par exemple.</li>
     *   <li><code>{@link MapPanel#removeLayer MapPanel.removeLayer}(this)</code>
     *       appelera <code>setVisible(false)</code>. Les classes d�riv�es peuvent profiter
     *       de cette sp�cification pour d�clarer � {@link MapPanel} qu'elles ne sont plus
     *       int�ress�es � suivre les mouvements de la souris par exemple.</li>
     * </ul>
     */
    public synchronized void setVisible(final boolean visible)
    {
        if (visible != this.visible)
        {
            this.visible=visible;
            repaint();
            if (visible) firePropertyChange("visible", Boolean.FALSE, Boolean.TRUE);
            else         firePropertyChange("visible", Boolean.TRUE, Boolean.FALSE);
        }
    }

    /**
     * Indique que cette couche a besoin d'�tre red�ssin�e. La couche ne sera
     * pas redessin�e im�diatement, mais seulement un peu plus tard. Cette m�thode
     * <code>repaint()</code> peut �tre appel�e � partir de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     */
    public void repaint()
    {repaint(shape);}

    /**
     * Impl�mente la m�thode {@link #repaint()}.
     */
    private void repaint(final Shape shape)
    {
        final JComponent mapPanel=this.mapPanel;
        if (mapPanel!=null)
        {
            if (EventQueue.isDispatchThread())
            {
                if (shape==null) mapPanel.repaint();
                else mapPanel.repaint(shape.getBounds());
            }
            else EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {repaint(shape);}
            });
        }
    }

    /**
     * Indique qu'une partie de cette couche a besoin d'�tre red�ssin�e.
     * Cette m�thode peut �tre appel�e � partir de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param bounds Coordonn�es (en points) de la partie � redessiner.
     */
    protected final void repaint(final Rectangle bounds)
    {
        final JComponent mapPanel=this.mapPanel;
        if (mapPanel!=null)
        {
            if (EventQueue.isDispatchThread())
            {
                if (bounds==null) mapPanel.repaint();
                else mapPanel.repaint(bounds);
            }
            else EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {repaint(bounds);}
            });
        }
    }

    /**
     * Proc�de au tra�age de cette couche. Cette m�thode sera appel�e automatiquement par
     * {@link fr.ird.map.MapPanel#paintComponent(java.awt.Graphics2D)} chaque fois que cette couche aura besoin
     * d'�tre redessin�e. Le tra�age doit se faire selon le syst�me de coordonn�es de l'affichage (g�n�ralement
     * des m�tres sur le terrain, � ne pas confondre avec des m�tres sur l'�cran!). Il sera de la responsabilit�
     * de cette m�thode d'effectuer les conversions n�cessaires si cette couche <code>Layer</code> utilise un
     * autre syst�me de coordonn�es que celui de l'afficheur {@link MapPanel}. Les conversions peuvent �tre faites
     * en utilisant comme suit les informations fournies dans l'objet {@link MapPaintContext} pass� en argument:
     *
     * <pre>context.{@link MapPaintContext#getProjectionFrom getProjectionFrom}({@link #getCoordinateSystem getCoordinateSystem}())</pre>
     * <blockquote>Pour transformer les coordonn�es de cette couche en coordonn�es logiques de l'afficheur. Les tra�ages
     *             dans l'objet {@link java.awt.Graphics2D} se font par d�faut en utilisant ces coordonn�es tansform�es.</blockquote>
     *
     * <pre>context.{@link MapPaintContext#getTransform getTransform}({@link MapPaintContext#FROM_WORLD_TO_POINT})</pre>
     * <blockquote>Pour transformer les coordonn�es logiques de l'afficheur en "points" (environ 1/72 de pouce).
     *             Cette transformation d�pend des zoom, translations et rotations qui ont �t� appliqu�es sur la
     *             carte.</blockquote>
     *
     * <pre>context.{@link MapPaintContext#getTransform getTransform}({@link MapPaintContext#FROM_POINT_TO_PIXEL})</pre>
     * <blockquote>Pour transformer les "points" (1/72 de pouce) en pixels. Cette transformation d�pend du
     *             p�riph�rique de sortie (par exemple l'�cran ou l'imprimante).</blockquote>
     *
     * <p>Cette m�thode re�oit en argument un objet {@link java.awt.Graphics2D} qui aura d�j� �t� configur�
     * en fonction de l'afficheur. En g�n�ral, tous les tra�ages fait sur cet objet le seront en <em>m�tres
     * sur le terrain</em>. C'est appropri� pour le tra�age d'une carte, mais pas pour l'�criture de textes.
     * Pour alterner entre le tra�age de couches et l'�criture de texte, on peut proc�der comme suit:</p>
     *
     * <blockquote><pre>
     * &nbsp;Shape paint(GraphicsJAI graphics, MapPaintContext context)
     * &nbsp;{
     * &nbsp;    // <i>Tracer ici les �l�ments g�ographiques</i>
     * &nbsp;    // <i>en coordonn�es utilisateurs (m ou �).</i>
     * &nbsp;    graphics.setTransform(context.getAffineTransform(MapPaintContext.FROM_POINT_TO_PIXEL));
     * &nbsp;    // <i>Ecrivez ici le texte ou les �tiquettes.</i>
     * &nbsp;    // <i>Les coordonn�es sont en <u>points</u>.</i>
     * &nbsp;    graphics.setTransform(context.getAffineTransform(MapPaintContext.FROM_WORLD_TO_POINT));
     * &nbsp;    // <i>Continuez le tra�age d'�l�ments</i>
     * &nbsp;    // <i>g�ographiques en m�tres ou degr�s.</i>
     * &nbsp;
     * &nbsp;    return myShapeInPoints;
     * &nbsp;}
     * </pre></blockquote>
     *
     * @param  graphics Graphique � utiliser pour tracer les couches.
     * @param  context  Suite des transformations n�cessaires � la conversion de coordonn�es
     *         g�ographiques (<var>longitude</var>,<var>latitude</var>) en coordonn�es pixels.
     *
     * @return Une approximation de la forme g�om�trique trac�e, en <strong>points</strong> (c'est-�-dire le
     *         r�sultat de la transformation {@link MapPaintContext#FROM_WORLD_TO_POINT}). Cette forme doit �tre en
     *         points plut�t qu'en coordonn�es logiques parce que certaines couche sont dessin�es � des positions
     *         fixes dans la fen�tre (par exemple l'�chelle de la carte) plut�t qu'� des positions qui varient
     *         avec le zoom. Toutefois, il n'est pas obligatoire que la forme retourn�e puisse restituer exactement
     *         ce qui a �t� dessin�. A la limite, un rectangle englobant la r�gion de cette couche est suffisant.
     *         Cette m�thode est autoris� � r�utiliser le m�me objet � chaque appel de <code>paint</code>. Elle
     *         peut aussi retourner <code>null</code> pour signifier que la forme peut convrir toute la fen�tre.
     *
     * @throws TransformException Si un probl�me est survenu lors d'une projection cartographique.
     */
    protected abstract Shape paint(final GraphicsJAI graphics, final MapPaintContext context) throws TransformException;

    /**
     * M�thode appel�e automatiquement pour construire une cha�ne de caract�res repr�sentant la valeur
     * point�e par la souris. En g�n�ral (mais pas obligatoirement), lorsque cette m�thode est appel�e,
     * le buffer <code>toAppendTo</code> contiendra d�j� une cha�ne de caract�res repr�sentant les
     * coordonn�es point�es par la souris. Cette m�thode est appel�e pour donner une chance aux couches
     * d'ajouter d'autres informations pertinentes. Par exemple les couches qui repr�sentent une image
     * satellitaire de temp�rature peuvent ajouter � <code>toAppendTo</code> un texte du genre "12�C"
     * (sans espaces au d�but).
     *
     * <p>L'impl�mentation par d�faut de cette m�thode retourne toujours <code>false</code> sans rien
     * faire.</p>
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @param  toAppendTo Le buffer dans lequel ajouter des informations.
     * @return <code>true</code> si cette m�thode a ajout� des informations dans <code>toAppendTo</code>.
     *         Dans ce cas, les couches en-dessous de <code>this</code> ne seront pas interrog�es.
     */
    protected boolean getLabel(final GeoMouseEvent event, final StringBuffer toAppendTo)
    {return false;}

    /**
     * Retourne le texte � afficher dans une bulle lorsque la souris tra�ne sur cette couche.
     * L'impl�mentation par d�faut retourne toujours <code>null</code>, ce qui signifie que
     * cette couche n'a aucun texte � afficher (les autres couches seront alors interrog�es).
     * Les classes d�riv�es peuvent red�finir cette m�thode pour retourner un texte apr�s avoir
     * v�rifi� que les coordonn�es de <code>event</code> correspondent bien � un point de cette
     * couche.
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @return Le texte � afficher lorsque la souris tra�ne sur cet �l�ment.
     *         Ce texte peut �tre nul pour signifier qu'il ne faut pas en �crire.
     */
    protected String getToolTipText(final GeoMouseEvent event)
    {return null;}

    /**
     * M�thode appell�e automatiquement chaque fois qu'il a �t� d�termin� qu'un menu contextuel devrait
     * �tre affich�. Sur Windows et Solaris, cette m�thode est appel�e lorsque l'utilisateur a appuy�
     * sur le bouton droit de la souris. Si cette couche d�sire faire appara�tre un menu, elle devrait
     * retourner le menu en question. Si non, elle devrait retourner <code>null</code>. L'impl�mentation
     * par d�faut retourne toujours <code>null</code>.
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @return Menu contextuel � faire appara�tre, ou <code>null</code>
     *         si cette couche ne propose pas de menu contextuel.
     */
    protected JPopupMenu getPopupMenu(final GeoMouseEvent event)
    {return null;}

    /**
     * M�thode appell�e chaque fois que le bouton de la souris a �t� cliqu� sur une couche qui pourrait
     * �tre <code>this</code>. L'impl�mentation par d�faut ne fait rien. Les classes d�riv�es qui souhaite
     * entrepredre une action doivent d'abord v�rifier si les coordonn�es de <code>event</code> correspondent
     * bien � un point de cette couche. Si oui, alors elles doivent aussi appeler {@link GeoMouseEvent#consume}
     * apr�s leur action, pour que le clic de la souris ne soit pas transmis aux autres couches en-dessous de
     * celle-ci.
     */
    protected void mouseClicked(final GeoMouseEvent event)
    {}

    /**
     * Efface les donn�es qui avaient �t� conserv�es dans une cache interne. L'appel
     * de cette m�thode permettra de lib�rer un peu de m�moire � d'autres fins. Elle
     * sera appel�e lorsque qu'on aura d�termin� que la couche <code>this</code>  ne
     * sera plus affich�e avant un certain temps.  Cette m�thode ne doit pas changer
     * le param�trage de cette couche;  son seul impact sera que le prochain tra�age
     * sera un peu plus lent.
     */
    protected void clearCache()
    {painting=null;}

    /**
     * Lib�re les ressources occup�es par cette couche. Cette m�thode est appel�e automatiquement
     * lorsqu'il a �t� d�termin� que cette couche sera bient�t d�truite.   Elle permet de lib�rer
     * les ressources plus rapidement que si l'on attend que le ramasse-miettes fasse son travail.
     */
    protected void dispose()
    {
        painting = null;
        shape    = null;
    }

    /**
     * Ajoute un objet int�ress� � �tre inform� chaque fois qu'une propri�t� de cet
     * objet <code>Layer</code> change. Les m�thodes {@link #setVisible} et
     * {@link #setZOrder} en particulier tiendront ces objets au courant des
     * changements qu'ils font.
     */
    public final void addPropertyChangeListener(final PropertyChangeListener listener)
    {listenerList.add(PropertyChangeListener.class, listener);}

    /**
     * Retire un objet qui n'est plus int�ress� � �tre inform� chaque fois
     * que change une propri�t� de cet objet <code>Layer</code>.
     */
    public final void removePropertyChangeListener(final PropertyChangeListener listener)
    {listenerList.remove(PropertyChangeListener.class, listener);}

    /**
     * Pr�vient tous les objets int�ress�s que l'�tat de cet objet a chang�.
     * La m�thode {@link PropertyChangeListener#propertyChange} de tous les
     * listeners sera appel�e, sauf si <code>oldValue</code> et
     * <code>newValue</code> sont identiques.
     *
     * @param propertyName nom de la propri�t� qui change (par exemple "preferredArea"} ou "zOrder"}).
     * @param oldValue Ancienne valeur (avant le changement).
     * @param newValue Nouvelle valeur (apr�s le changement).
     */
    protected void firePropertyChange(final String propertyName, final Object oldValue, final Object newValue)
    {
        if (oldValue!=newValue && (oldValue==null || !oldValue.equals(newValue)))
        {
            PropertyChangeEvent event = null;
            final Object[]  listeners = listenerList.getListenerList();
            for (int i=listeners.length; (i-=2)>=0;)
            {
                if (listeners[i]==PropertyChangeListener.class)
                {
                    if (event==null)
                    {
                        event=new PropertyChangeEvent(this, propertyName, oldValue, newValue);
                    }
                    try
                    {
                        ((PropertyChangeListener) listeners[i+1]).propertyChange(event);
                    }
                    catch (RuntimeException exception)
                    {
                        ExceptionMonitor.unexpectedException("fr.ird.map", "Layer", "firePropertyChange", exception);
                    }
                }
            }
        }
    }




    /////////////////////////////////////////////////////////////
    ////////                                             ////////
    ////////        METHODES RESERVEES A MapPanel        ////////
    ////////                                             ////////
    /////////////////////////////////////////////////////////////

    /**
     * M�thode appel�e automatiquement chaque fois que le zoom a chang�.
     * Cette m�thode met � jour les coordonn�es des formes g�om�triques
     * d�clar�es dans les objets {@link Layer}.
     *
     * @param modifier Transformation � utiliser pour transformer les
     *        coordonn�es, ou <code>null</code> si elle n'est pas connue.
     */
    final void zoomChanged(final AffineTransform modifier)
    {
        final Shape shape=this.shape;
        if (shape!=null)
        {
            if (modifier!=null)
            {
                final Rectangle2D bounds=shape.getBounds2D();
                this.shape=XAffineTransform.transform(modifier, bounds, bounds);
System.out.println(shape);
            }
            else this.shape=null;
        }
    }

    /**
     * Proc�de au tra�age de cette couche, � la condition qu'elle soit
     * visible � l'int�rieur du rectangle <code>clipBounds</code> sp�cifi�.
     *
     * @param graphics   Graphique dans lequel faire le tra�age.
     * @param context    Suite des transformations permettant de passer des
     *                   coordonn�es de cette couche en coordonn�es pixels.
     * @param clipBounds Coordonn�es en points de la portion de l'�cran � redessiner.
     */
    final synchronized void paint(final GraphicsJAI graphics, final MapPaintContext context, final Rectangle clipBounds) throws TransformException
    {
        if (visible)
        {
//TODO      if (shape==null || clipBounds==null || shape.intersects(clipBounds))
            {
                if (Version.MINOR>=4)
                {
                    if (painting==null)
                    {
                        painting = Resources.format(Cl�.PAINTING�1, getName());
                    }
                    Contour.logger.finest(painting);
                    // Use FINEST level since this event is
                    // likely to generate a lot of records.
                }
                final Shape shape=paint(graphics, context);
                if (!context.isPrinting())
                {
                    this.shape = shape;
                }
            }
        }
    }

    /**
     * Indique si cette couche contient le point sp�cifi�.
     *
     * @param  x Coordonn�es <var>x</var> du point.
     * @param  y Coordonn�es <var>y</var> du point.
     * @return <code>true</code> si cette composante est
     *         visible et contient le point sp�cifi�.
     */
    final boolean contains(final int x, final int y)
    {
        if (visible)
        {
            final Shape shape=this.shape;
            return (shape!=null) && shape.contains(x,y);
        }
        return false;
    }

    /**
     * Indique si la r�gion g�ographique <code>big</code> contient enti�rement la sous-r�gion <code>small</code>
     * sp�cifi�e. Un cas particuluer survient si un ou plusieurs bords de <code>small</code> co�ncide avec
     * les bords correspondants de <code>big</code>. L'argument <code>edge</code> indique si on consid�re
     * qu'il y a inclusion ou pas dans ces circonstances.
     *
     * @param big   R�gion g�ographique dont on veut v�rifier s'il contient une sous-r�gion.
     * @param small Sous-r�gion g�ographique dont on veut v�rifier l'inclusion dans <code>big</code>.
     * @param edge <code>true</code> pour consid�rer qu'il y a inclusion si ou ou plusieurs bords
     *        de <code>big</code> et <code>small</code> con�ncide, ou <code>false</code> pour exiger
     *        que <code>small</code> ne touche pas aux bords de <code>big</code>.
     */
    private static boolean contains(final Rectangle2D big, final Rectangle2D small, final boolean edge)
    {
        return edge ? (small.getMinX()>=big.getMinX() && small.getMaxX()<=big.getMaxX() && small.getMinY()>=big.getMinY() && small.getMaxY()<=big.getMaxY()):
                      (small.getMinX()> big.getMinX() && small.getMaxX()< big.getMaxX() && small.getMinY()> big.getMinY() && small.getMaxY()< big.getMaxY());
    }

    /**
     * Agrandi (si n�cessaire) une r�gion g�ographique en fonction de l'ajout, la supression ou la modification
     * des coordonn�es d'une sous-r�gion. Cette m�thode est appel�e lorsque les coordonn�es de la sous-r�gion
     * <code>oldSubArea</code> ont chang�es pour devenir <code>newSubArea</code>. Si ce changement s'est traduit
     * par un agrandissement de <code>area</code>, alors le nouveau rectangle agrandi sera retourn�. Si le changement
     * n'a aucun impact sur <code>area</code>, alors <code>area</code> sera retourn� tel quel. Si le changement PEUT
     * avoir diminu� la dimension de <code>area</code>, alors cette m�thode retourne <code>null</code> pour indiquer
     * qu'il faut recalculer <code>area</code> � partir de z�ro.
     *
     * @param  area       R�gion g�ographique qui pourrait �tre affect�e par le changement de coordonn�es d'une
     *                    sous-r�gion. En aucun cas ce rectangle <code>area</code> ne sera directement modifi�.
     *                    Si une modification est n�cessaire, elle sera faite sur un clone qui sera retourn�.
     *                    Cet argument peut �tre <code>null</code> si aucune r�gion n'�tait pr�c�demment d�finie.
     * @param  oldSubArea Anciennes coordonn�es de la sous-r�gion, ou <code>null</code> si la sous-r�gion n'existait
     *                    pas avant l'appel de cette m�thode. Ce rectangle ne sera jamais modifi� ni retourn�.
     * @param  newSubArea Nouvelles coordonn�es de la sous-r�gion, ou <code>null</code> si la sous-r�gion est
     *                    supprim�e. Ce rectangle ne sera jamais modifi� ni retourn�.
     *
     * @return Un rectangle contenant les coordonn�es mises-�-jour de <code>area</code>, si cette mise-�-jour
     *         a pu se faire. Si elle n'a pas pu �tre faite faute d'informations, alors cette m�thode retourne
     *         <code>null</code>. Dans ce dernier cas, il faudra recalculer <code>area</code> � partir de z�ro.
     */
    static Rectangle2D changeArea(Rectangle2D area, final Rectangle2D oldSubArea, final Rectangle2D newSubArea)
    {
        if (area==null)
        {
            /*
             * Si aucune r�gion n'avait �t� d�finie auparavant. La sous-r�gion
             * "newSubArea" repr�sente donc la totalit� de la nouvelle r�gion
             * "area". On construit un nouveau rectangle plut�t que de faire un
             * clone pour �tre certain d'avoir un type d'une pr�cision suffisante.
             */
            if (newSubArea!=null)
            {
                area=new Rectangle2D.Double();
                area.setRect(newSubArea);
            }
            return area;
        }
        if (newSubArea==null)
        {
            /*
             * Une sous-r�gion a �t� supprim�e ("newSubArea" est nulle). Si la sous-r�gion supprim�e ne
             * touchait pas au bord de "area",  alors sa suppression ne peut pas avoir diminu�e "area":
             * on retournera alors area. Si au contraire "oldSubArea" touchait au bord de "area", alors
             * on ne sait pas si la suppression de "oldSubArea" a diminu� "area".  Il faudra recalculer
             * "area" � partir de z�ro, ce que l'on indique en retournant NULL.
             */
            if (               oldSubArea==null  ) return area;
            if (contains(area, oldSubArea, false)) return area;
            return null;
        }
        if (oldSubArea!=null)
        {
            /*
             * Une sous-r�gion a chang�e ("oldSubArea" est devenu "newSubArea"). Si on d�tecte que ce
             * changement PEUT diminuer la superficie totale de "area", il faudra recalculer "area" �
             * partir de z�ro pour en �tre sur. On retourne donc NULL.  Si au contraire la superficie
             * totale de "area" ne peut pas avoir diminu�e, elle peut avoir augment�e. Ce calcul sera
             * fait � la fin de cette m�thode, qui poursuit son cours.
             */
            double t;
            if (((t=oldSubArea.getMinX()) <= area.getMinX() && t < newSubArea.getMinX()) ||
                ((t=oldSubArea.getMaxX()) >= area.getMaxX() && t > newSubArea.getMaxX()) ||
                ((t=oldSubArea.getMinY()) <= area.getMinY() && t < newSubArea.getMinY()) ||
                ((t=oldSubArea.getMaxY()) >= area.getMaxY() && t > newSubArea.getMaxY()))
            {
                return null; // Le changement PEUT avoir diminu� "area".
            }
        }
        /*
         * Une nouvelle sous-r�gion est ajout�e. Si elle �tait d�j�
         * enti�rement comprise dans "area", alors son ajout n'aura
         * aucun impact sur "area" et peut �tre ignor�.
         */
        if (!contains(area, newSubArea, true))
        {
            // Cloner est n�cessaire pour que "firePropertyChange"
            // puisse conna�tre l'ancienne valeur de "area".
            area = (Rectangle2D) area.clone();
            area.add(newSubArea);
        }
        return area;
    }
}
