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
package fr.ird.layer;

// OpenGIS dependencies (SEAGIS)
import net.seagis.gc.GridCoverage;
import net.seagis.ct.MathTransform2D;
import net.seagis.cs.CoordinateSystem;
import net.seagis.cv.SampleDimension;
import net.seagis.cv.CategoryList;
import net.seagis.resources.OpenGIS;

// Map components
import net.seas.map.layer.GridMarkLayer;

// Geometry
import java.awt.Shape;
import java.awt.Dimension;
import net.seas.awt.geom.Arrow2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

// Graphics
import java.awt.Paint;
import java.awt.Color;
import javax.media.jai.GraphicsJAI;

// Images
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

// Input/output
import java.io.IOException;
import java.io.ObjectInputStream;

// Miscellaneous
import javax.units.Unit;
import net.seagis.resources.XMath;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Repr�sentation graphique d'un champ de vecteur construit � partir des donn�es
 * d'images. Cette classe peut servir lorsque l'on dispose d'une image avec au
 * moins deux bandes. Une bande peut repr�senter la composante U d'un vecteur,
 * tandis que l'autre repr�sente la composante V.
 *
 * @author Martin Desruisseaux
 * @version 1.0
 */
public class VectorLayer extends GridMarkLayer
{
    /**
     * Image repr�sentant les composantes U et V des vecteurs.
     */
    private PlanarImage data;

    /**
     * Index des bandes U et V dans l'image.
     */
    private int bandU, bandV;

    /**
     * A category to use as a formatter for geophysics values.
     */
    private CategoryList theme;

    /**
     * Forme g�om�trique repr�sentant une fl�che.  Le d�but de cette fl�che
     * est l'origine � (0,0) et sa longueur est de 10 points. La fl�che est
     * point�e dans la direction des <var>x</var> positifs (soit � un angle
     * de 0 radians arithm�tiques).
     */
    private static final Shape DEFAULT_SHAPE = new Arrow2D(0, -5, 10, 10);

    /**
     * Couleur des fl�ches.
     */
    private Color color = new Color(0, 153, 255, 128);

    /**
     * Indices (<var>i</var>,<var>j</var>)
     * du dernier point interrog�.
     */
    private transient int lastI=-1, lastJ=-1;

    /**
     * Valeurs (<var>u</var>,<var>v</var>) du vecteur au
     * point <code>({@link #lastI}, {@link #lastJ})</code>.
     */
    private transient double lastU, lastV;

    /**
     * Proc�de � la lecture binaire de cet objet,
     * puis initialise des champs internes.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        lastI = -1;
        lastJ = -1;
    }

    /**
     * Construit un champ de vecteur qui ne
     * contient initialement aucune donn�e.
     */
    public VectorLayer()
    {super();}

    /**
     * Construit un champ de vecteur qui utilisera
     * le syst�me de coordonn�es sp�cifi�.
     *
     * @param coordinateSystem Syst�me de coordonn�es
     *        des positions (<var>x</var>,<var>y</var>).
     */
    public VectorLayer(final CoordinateSystem coordinateSystem)
    {super(coordinateSystem);}

    /**
     * Construit un champ de vecteur qui
     * utilisera l'image sp�cifi�e.
     *
     * @param  coverage Les donn�es � utiliser.
     * @param  bandU Bande de la composante U des vecteurs.
     * @param  bandV Bande de la composante V des vecteurs.
     */
    public VectorLayer(final GridCoverage coverage, final int bandU, final int bandV)
    {
        super(coverage.getCoordinateSystem());
        setData(coverage, bandU, bandV);
    }

    /**
     * D�finit l'image � utiliser comme source de donn�es.
     *
     * @param  coverage Les donn�es � utiliser.
     * @param  bandU Bande de la composante U des vecteurs.
     * @param  bandV Bande de la composante V des vecteurs.
     */
    public synchronized void setData(final GridCoverage coverage, final int bandU, final int bandV)
    {
        final CoordinateSystem cs = OpenGIS.getCoordinateSystem2D(coverage.getCoordinateSystem());
        if (!cs.equivalents(getCoordinateSystem()))
        {
            // TODO: Il faudrait ajouter une m�thode Layer.setCoordinateSystem
            //       et changer le syst�me de coordonn�es ici pour prendre celui
            //       des images.
            throw new IllegalArgumentException();
        }
        final SampleDimension[] samples = coverage.getSampleDimensions();
        final CategoryList  categoriesU = samples[bandU].getCategoryList();
        final CategoryList  categoriesV = samples[bandV].getCategoryList();
        final Unit unitU = categoriesU.getUnits();
        final Unit unitV = categoriesV.getUnits();
        if (!unitU.equals(unitV))
        {
            throw new IllegalArgumentException();
        }
        this.data  = PlanarImage.wrapRenderedImage(coverage.getRenderedImage(true));
        this.theme = categoriesU;
        this.bandU = bandU;
        this.bandV = bandV;
        final Dimension       size = new Dimension(data.getWidth(), data.getHeight());
        final MathTransform2D  mtr = coverage.getGridGeometry().getGridToCoordinateSystem2D();
        final AffineTransform gref = new AffineTransform((AffineTransform) mtr);
        gref.translate(-data.getMinX(), -data.getMinY());
        // La translation de -min(x,y) est pour faire commencer les indices � (0,0).
        setGrid(size, gref);
        setPreferredArea(coverage.getEnvelope().getSubEnvelope(0,2).toRectangle2D());
    }

    /**
     * D�finit la couleur de remplissage des fl�ches.
     */
    public void setColor(final Color color)
    {this.color=color;}

    /**
     * Retourne les unit�s de l'amplitude des vecteurs, ou <code>null</code>
     * si ces unit�s ne sont pas connues. Dans les cas des fl�ches de courant
     * par exemple, �a sera typiquement des "cm/s".
     */
    public Unit getAmplitudeUnit()
    {return theme.getUnits();}

    /**
     * Retourne l'amplitude typique des donn�es de cette couche.
     */
    public double getTypicalAmplitude()
    {return 25;} // TODO

    /**
     * Retourne l'amplitude horizontale d'un vecteur. Cette amplitude
     * sera exprim�e selon les unit�s de {@link #getAmplitudeUnit}.
     *
     * @param  i Index du point selon <var>x</var>, dans la plage <code>[0..width-1]</code>.
     * @param  j Index du point selon <var>y</var>, dans la plage <code>[0..height-1]</code>.
     * @return Amplitude du vecteur � la position sp�cifi�e, selon les unit�s {@link #getAmplitudeUnit}.
     */
    public double getAmplitude(final int i, final int j)
    {
        if (i!=lastI || j!=lastJ) computeUV(i,j);
        return XMath.hypot(lastU, lastV);
    }

    /**
     * Retourne la direction d'un vecteur, en radians arithmetiques.
     *
     * @param  i Index du point selon <var>x</var>, dans la plage <code>[0..width-1]</code>.
     * @param  j Index du point selon <var>y</var>, dans la plage <code>[0..height-1]</code>.
     * @return Direction du vecteur � la position sp�cifi�e, en radians arithm�tiques.
     */
    public double getDirection(final int i, final int j)
    {
        if (i!=lastI || j!=lastJ) computeUV(i,j);
        return Math.atan2(lastV, lastU);
    }

    /**
     * Calcule les composantes (<var>u</var>,<var>v</var>)
     * du vecteur � la position indiqu�e.
     */
    private void computeUV(final int i, final int j)
    {
        final Raster tile = data.getTile(data.XToTileX(i), data.YToTileY(j));
        final int x = i+data.getMinX();
        final int y = j+data.getMinY();
        lastU = tile.getSample(x, y, bandU);
        lastV = tile.getSample(x, y, bandV);
        lastI = i;
        lastJ = j;
    }

    /**
     * Retourne la forme g�om�trique servant de mod�le au tra�age des fl�ches.
     * Le d�but de cette fl�che � son origine � (0,0) et la fl�che pointe dans
     * la direction des <var>x</var> positifs (soit � un angle de 0 radians
     * arithm�tiques).
     */
    public Shape getMarkShape(final int i)
    {return DEFAULT_SHAPE;}

    /**
     * Proc�de au tra�age d'une fl�che.
     *
     * @param graphics Graphique � utiliser pour tracer la fl�che. L'espace de coordonn�es
     *                 de ce graphique sera les pixels en les points (1/72 de pouce).
     * @param shape    Forme g�om�trique repr�sentant la fl�che � tracer.
     * @param index    Index de la fl�che � tracer.
     */
    protected void paint(final GraphicsJAI graphics, final Shape shape, final int index)
    {
        graphics.setColor(color);
        graphics.fill(shape);
    }

    /**
     * Retourne l'amplitude de la fl�che.
     */
    protected synchronized String getToolTipText(final int index)
    {return theme.format(getAmplitude(index), null);}
}
