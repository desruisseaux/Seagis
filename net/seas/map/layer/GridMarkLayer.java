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
package net.seas.map.layer;

// OpenGIS dependencies (SEAGIS)
import net.seagis.cs.CoordinateSystem;
import net.seagis.ct.TransformException;

// Geometry
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import net.seagis.resources.XAffineTransform;
import net.seagis.resources.XMath;

// Graphics
import net.seas.map.RenderingContext;
import javax.media.jai.GraphicsJAI;

// Miscellaneous
import java.io.IOException;
import java.io.ObjectInputStream;
import net.seas.resources.Resources;


/**
 * Repr�sentation graphique d'une grille de marque apparaissant sur une carte.  Cette classe
 * reprend les fonctionalit�s de {@link MarkLayer} en ajoutant la contrainte que les marques
 * doivent �tre dispos�es sur une grille r�guli�re. Cette contrainte suppl�mentaire permet de:
 *
 * <ul>
 *   <li>Optimiser la vitesse d'affichage.</li>
 *   <li>D�cimer la densit� des marques en fonction du zoom.</li>
 * </ul>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 *
 * @see fr.ird.map.layer.GridMarkControler
 */
public abstract class GridMarkLayer extends MarkLayer
{
    /**
     * Nombre de points selon l'axe des <var>x</var>.
     */
    private int width;

    /**
     * Nombre de points selon l'axe des <var>y</var>.
     */
    private int height;

    /**
     * Nombre de points � moyenner selon l'axe des <var>x</var> et des <var>y</var>.
     * Ce nombre doit �tre sup�rieur � 0. La valeur <code>1</code> signifie qu'aucune
     * d�cimation ne sera faite.
     */
    private int decimateX=1, decimateY=1;

    /**
     * Espae minimal (en points) � laisser entre les points de la grille selon les axes
     * <var>x</var> et <var>y</var>. La valeur 0 d�sactive la d�cimation selon cet axe.
     */
    private int spaceX=0, spaceY=0;

    /**
     * Indique si la d�cimation est active. Ce champ prend la valeur
     * <code>true</code> si <code>decimateX</code> ou <code>decimateY</code>
     * sont sup�rieurs � 1.
     */
    private boolean decimate=false;

    /**
     * Indique si la d�cimation automatique est active. Ce champ prend la
     * valeur <code>true</code> lorsque {@link #setAutoDecimation} est
     * appell�e et que <code>spaceX</code> ou <code>spaceY</code> sont
     * sup�rieurs � 0.
     */
    private boolean autoDecimate=false;

    /**
     * Transformation affine servant � passer des indices
     * vers les coordonn�es (<var>x</var>,<var>y</var>).
     */
    private final AffineTransform transform=new AffineTransform();

    /**
     * Index du dernier �l�ment dont on a
     * obtenu les composantes U et V du vecteur.
     */
    private transient int lastIndex = -1;

    /**
     * Indices X et Y calcul�es lors du
     * dernier appel de {@link #computeUV}.
     */
    private transient double lastI, lastJ;

    /**
     * Composante U et V calcul�es lors du
     * dernier appel de {@link #computeUV}.
     */
    private transient double lastU, lastV;

    /**
     * Proc�de � la lecture binaire de cet objet,
     * puis initialise des champs internes.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        lastIndex = -1;
    }

    /**
     * Construit une grille de marques. Les coordonn�es de ces
     * marques seront exprim�es selon le syst�me de coordonn�es
     * par d�faut (WGS 1984).
     */
    protected GridMarkLayer()
    {super();}

    /**
     * Construit une grille de marques. Les coordonn�es de ces
     * marques seront exprim�es selon le syst�me de coordonn�es
     * sp�cifi�.
     */
    protected GridMarkLayer(final CoordinateSystem coordinateSystem)
    {super(coordinateSystem);}

    /**
     * D�finit la dimension de la grille (en nombre de points) ainsi
     * que la tranformation qui convertit les indices en coordonn�es.
     *
     * @param size Dimension de la grille. <code>width</code> est le nombre
     *             de points selon <var>x</var>, et <code>height</code> est
     *             le nombre de points selon <var>y</var>.
     * @param transform Transformation affine convertissant les indices (<var>i</var>,<var>j</var>) en
     *             coordonn�es (<var>x</var>,<var>y</var>). Les indices <var>i</var> et <var>j</var> sont
     *             des entiers compris dans les plages <code>[0..width-1]</code> et <code>[0..height-1]</code>
     *             respectivement. Les coordonn�es <var>x</var> et <var>y</var> sont des nombres r�els exprim�s
     *             selon le syst�me de coordonn�es sp�cifi� lors de la construction ("WGS 1984" par d�faut).
     */
    protected synchronized void setGrid(final Dimension size, final AffineTransform transform)
    {
        if (size.width<=1 || size.height<=1)
            throw new IllegalArgumentException(size.toString());
        this.width  = size.width;
        this.height = size.height;
        this.transform.setTransform(transform);
        repaint();
    }

    /**
     * D�finit la dimension de la grille (en nombre de points) ainsi les coordonn�es g�ographiques
     * de la r�gion couverte par les points de la grille. Le point situ� aux index (0,0) correspondra
     * au coin sup�rieur gauche de la r�gion <code>area</code>. Appeller cette methode est �quivalent
     * � appeller {@link #setGrid(Dimension, AffineTransform)} avec la transformation affine suivante:
     *
     * <blockquote><pre>
     * | dx/(width-1)      0           Xmin  |
     * |     0       -dy/(height-1)    Ymax  |
     * |     0             0             1   |
     * </pre></blockquote>
     *
     * o� <var>dx</var> et <var>dy</var> sont les largeur et hauteur de <code>area</code> (en coordonn�es
     * logiques), <var>X<sub>min</sub></var> et <var>Y<sub>max</sub></var> sont les coordonn�es <var>x</var>
     * et <var>y</var> minimale ou maximale de <code>area</code>, et <var>width</var> et <var>height</var>
     * sont le nombre de points selon <var>x</var> et <var>y</var> respectivement.
     *
     * @param size Dimension de la grille. <code>width</code> est le nombre
     *             de points selon <var>x</var>, et <code>height</code> est
     *             le nombre de points selon <var>y</var>.
     * @param area Coordonn�es g�ographiques de la r�gion couverte par les
     *             points de la grille. Ces coordonn�es doivent �tre exprim�es
     *             selon le syst�me de coordonn�es sp�cifi� lors de la construction
     *             ("WGS 1984" par d�faut).
     */
    protected synchronized void setGrid(final Dimension size, final Rectangle2D area)
    {
        double    dx     = area.getWidth ();
        double    dy     = area.getHeight();
        final int width  = size.width;
        final int height = size.height;
        if (dx>0 && dy>0)
        {
            if (width>=2 && height>=2)
            {
                dx /= (width  -1);
                dy /= (height -1);
            }
            else throw new IllegalArgumentException(size.toString());
        }
        else if (!(dx==0 && dy==0 && width==1 && height==1))
        {
            throw new IllegalArgumentException(area.toString());
        }
        transform.setTransform(dx, 0, 0, -dy, area.getMinX(), area.getMaxY());
        this.width  = width;
        this.height = height;
        repaint();
    }

    /**
     * Sp�cifie une d�cimation � appliquer sur la grille lors de l'affichage.   Cette
     * d�cimation n'affecte pas les indices <var>i</var> et <var>j</var> transmis aux
     * m�thodes {@link #getAmplitude(int,int)} et {@link #getDirection(int,int)}. Elle
     * affecte toutefois les index transmis aux m�thodes qui ne re�oivent qu'un argument
     * <code>index</code>, comme {@link #getAmplitude(int)} et {@link #getDirection(int)}.
     * Par d�faut, ces derni�res retourneront la moyenne des vecteurs d�cim�s.
     *
     * @param decimateX D�cimation selon <var>x</var>, ou 1 pour ne pas en faire.
     * @param decimateY D�cimation selon <var>y</var>, ou 1 pour ne pas en faire.
     */
    public synchronized void setDecimation(final int decimateX, final int decimateY)
    {
        if (decimateX <=0) throw new IllegalArgumentException(Resources.format(Cl�.NOT_GREATER_THAN_ZERO�1, new Integer(decimateX)));
        if (decimateY <=0) throw new IllegalArgumentException(Resources.format(Cl�.NOT_GREATER_THAN_ZERO�1, new Integer(decimateY)));
        if (decimateX!=this.decimateX || decimateY!=this.decimateY)
        {
            autoDecimate   = false;
            this.decimateX = decimateX;
            this.decimateY = decimateY;
            decimate = (decimateX!=1 || decimateY!=1);
            repaint();
        }
    }

    /**
     * D�cime automatiquement les points de la grille de fa�on � conserver un espace
     * d'au moins <code>spaceX</code> et <code>spaceY</code> entre chaque point.
     *
     * @param spaceX Espae minimale (en points) selon <var>x</var> � laisser entre les
     *        points de la grille. La valeur 0 d�sactive la d�cimation selon cet axe.
     * @param spaceY Espae minimale (en points) selon <var>y</var> � laisser entre les
     *        points de la grille. La valeur 0 d�sactive la d�cimation selon cet axe.
     */
    public synchronized void setAutoDecimation(final int spaceX, final int spaceY)
    {
        if (spaceX < 0) throw new IllegalArgumentException(Resources.format(Cl�.BAD_NUMBER�1, new Integer(spaceX)));
        if (spaceY < 0) throw new IllegalArgumentException(Resources.format(Cl�.BAD_NUMBER�1, new Integer(spaceY)));
        if (spaceX!=this.spaceX || spaceY!=this.spaceY)
        {
            this.spaceX  = spaceX;
            this.spaceY  = spaceY;
            autoDecimate = (spaceX!=0 || spaceY!=0);
            repaint();
        }
    }

    /**
     * Retourne le nombre de points selon <var>x</var> (<code>width</code>)
     * et selon <var>y</var> (<code>height</code>).
     */
    public final synchronized Dimension getSize()
    {return new Dimension(width, height);}

    /**
     * Retourne l'amplitude de la valeur � un point donn�.
     *
     * @param  i Index du point selon <var>x</var>, dans la plage <code>[0..width-1]</code>.
     * @param  j Index du point selon <var>y</var>, dans la plage <code>[0..height-1]</code>.
     * @return Amplitude de la valeur � la position sp�cifi�e, selon les unit�s {@link #getAmplitudeUnit}.
     */
    public abstract double getAmplitude(final int i, final int j);

    /**
     * Retourne la direction de la valeur � un point donn�. L'impl�mentation par
     * d�faut retourne toujours 0.
     *
     * @param  i Index du point selon <var>x</var>, dans la plage <code>[0..width-1]</code>.
     * @param  j Index du point selon <var>y</var>, dans la plage <code>[0..height-1]</code>.
     * @return Direction de la valeur � la position sp�cifi�e, en radians arithm�tiques.
     */
    public double getDirection(final int i, final int j)
    {return 0;}

    /**
     * Retourne les indices qui correspondent aux coordonn�es sp�cifi�es.
     * Ces indices seront utilis�es par {@link #isVisible(int,Rectangle)}
     * pour v�rifier si un point est dans la partie visible.
     *
     * @param visibleArea Coordonn�es logiques de la r�gion visible � l'�cran.
     */
    final Rectangle getUserClip(final Rectangle2D visibleArea)
    {
        if (visibleArea!=null) try
        {
            // Note: on profite du fait que {@link Rectangle#setRect}
            //       arrondie correctement vers les limites sup�rieures.
            final Rectangle bounds= (Rectangle) XAffineTransform.inverseTransform(transform, visibleArea, new Rectangle());
            bounds.x      = (bounds.x      -1) / decimateX;
            bounds.y      = (bounds.y      -1) / decimateY;
            bounds.width  = (bounds.width  +2) / decimateX +1;
            bounds.height = (bounds.height +2) / decimateY +1;
            return bounds;
        }
        catch (NoninvertibleTransformException exception)
        {
            // Retourne un clip englobant toutes les coordonn�es.
        }
        return new Rectangle(0, 0, width, height);
    }

    /**
     * Indique si la station � l'index sp�cifi� est visible
     * dans le clip sp�cifi�. Le rectangle <code>clip</code>
     * doit avoir �t� obtenu par {@link #getUserClip}.
     */
    final boolean isVisible(final int index, final Rectangle clip)
    {
        if (clip==null) return true;
        final int decWidth = width/decimateX;
        return clip.contains(index%decWidth, index/decWidth);
    }

    /**
     * Proc�de au tra�age des marques de cette grille.
     *
     * @throws TransformException si une projection
     *         cartographique �tait n�cessaire et a �chou�e.
     */
    protected synchronized Shape paint(final GraphicsJAI graphics, final RenderingContext context) throws TransformException
    {
        if (autoDecimate)
        {
            Point2D delta=new Point2D.Double(1,1);
            delta = transform.deltaTransform(delta, delta);
            delta = context.getAffineTransform(RenderingContext.WORLD_TO_POINT).deltaTransform(delta, delta);
            final int decimateX = Math.max(1, (int)Math.ceil(spaceX/delta.getX()));
            final int decimateY = Math.max(1, (int)Math.ceil(spaceY/delta.getY()));
            if (decimateX!=this.decimateX || decimateY!=this.decimateY)
            {
                this.decimateX = decimateX;
                this.decimateY = decimateY;
                decimate = (decimateX!=1 || decimateY!=1);
                clearCache();
            }
        }
        return super.paint(graphics, context);
    }

    /**
     * Retourne le nombre de points de cette grille. Le nombre de point retourn�
     * tiendra compte de la d�cimation sp�cifi�e avec {@link #setDecimation}.
     */
    public final synchronized int getCount()
    {return (width/decimateX)*(height/decimateY);}

    /**
     * Retourne les coordonn�es (<var>x</var>,<var>y</var>) d'un point de la grille.
     * Les coordonn�es <var>x</var> et <var>y</var> seront exprim�es selon le syst�me
     * de coordonn�es sp�cifi� lors de la construction (WGS 1984 par d�faut).
     *
     * Si une d�cimation a �t� sp�cifi�e avec la m�thode {@link #setDecimation},
     * alors la position retourn�e sera situ�e au milieu des points � moyenner.
     */
    public final Point2D getPosition(final int index)
    {
        final Point2D point;
        if (!decimate)
        {
            point = new Point2D.Double(index%width, index/width);
        }
        else
        {
            if (lastIndex!=index) computeUV(index);
            point = new Point2D.Double(lastI, lastJ);
        }
        return transform.transform(point, point);
    }

    /**
     * Retourne l'amplitude � la position d'une marque. Si aucune d�cimation n'est �
     * faire, alors cette m�thode ne fait qu'appeler {@link #getAmplitude(int,int)}.
     * Si une d�cimation a �t� sp�cifi�e avec la m�thode {@link #setDecimation}, alors
     * cette m�thode calcule la moyenne vectorielle (la moyenne des composantes
     * <var>u</var> et <var>v</var>) aux positions des marques � d�cimer, et retourne
     * l'amplitude du vecteur moyen.
     */
    public final double getAmplitude(final int index)
    {
        if (!decimate)
        {
            return getAmplitude(index%width, index/width);
        }
        if (lastIndex!=index) computeUV(index);
        return XMath.hypot(lastU, lastV);
    }

    /**
     * Retourne la direction de la valeur d'une marque. Si aucune d�cimation n'est �
     * faire, alors cette m�thode ne fait qu'appeler {@link #getAmplitude(int,int)}.
     * Si une d�cimation a �t� sp�cifi�e avec la m�thode {@link #setDecimation}, alors
     * cette m�thode calcule la moyenne vectorielle (la moyenne des composantes
     * <var>u</var> et <var>v</var>) aux positions des marques � d�cimer, et retourne
     * la direction du vecteur moyen.
     */
    public final double getDirection(final int index)
    {
        if (!decimate)
        {
            return getDirection(index%width, index/width);
        }
        if (lastIndex!=index) computeUV(index);
        return Math.atan2(lastV, lastU);
    }

    /**
     * Calcule les composantes U et V
     * du vecteur � l'index sp�cifi�.
     */
    private void computeUV(final int index)
    {
        int    count = 0;
        int    sumI  = 0;
        int    sumJ  = 0;
        double vectU = 0;
        double vectV = 0;
        final int decWidth = width/decimateX;
        final int imin = (index % decWidth)*decimateX;
        final int jmin = (index / decWidth)*decimateY;
        for (int i=imin+decimateX; --i>=imin;)
        {
            for (int j=jmin+decimateY; --j>=jmin;)
            {
                final double amplitude = getAmplitude(i, j);
                final double direction = getDirection(i, j);
                final double U = amplitude*Math.cos(direction);
                final double V = amplitude*Math.sin(direction);
                if (!Double.isNaN(U) && !Double.isNaN(V))
                {
                    vectU += U;
                    vectV += V;
                    sumI  += i;
                    sumJ  += j;
                    count++;
                }
            }
        }
        this.lastIndex = index;
        this.lastI     = (double)sumI / count;
        this.lastJ     = (double)sumJ / count;
        this.lastU     = vectU/count;
        this.lastV     = vectV/count;
    }
}
