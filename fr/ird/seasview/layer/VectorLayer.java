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
package fr.ird.seasview.layer;

// Images
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

// Graphics
import java.awt.Paint;
import java.awt.Color;
import javax.media.jai.GraphicsJAI;

// Geometry
import java.awt.Shape;
import java.awt.Dimension;
import fr.ird.awt.geom.Arrow2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

// J2SE miscellaneous
import java.util.Locale;
import java.io.IOException;
import java.io.ObjectInputStream;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.ct.MathTransform2D;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cv.SampleDimension;
import org.geotools.pt.AngleFormat;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XMath;
import org.geotools.units.Unit;

// Miscellaneous
import fr.ird.map.layer.GridMarkLayer;
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Représentation graphique d'un champ de vecteur construit à partir des données
 * d'images. Cette classe peut servir lorsque l'on dispose d'une image avec au
 * moins deux bandes. Une bande peut représenter la composante U d'un vecteur,
 * tandis que l'autre représente la composante V.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class VectorLayer extends GridMarkLayer
{
    /**
     * Image représentant les composantes U et V des vecteurs.
     */
    private PlanarImage data;

    /**
     * Index des bandes U et V dans l'image.
     */
    private int bandU, bandV;

    /**
     * A band to use as a formatter for geophysics values.
     */
    private SampleDimension theme;

    /**
     * Forme géométrique représentant une flèche.  Le début de cette flèche
     * est l'origine à (0,0) et sa longueur est de 10 points. La flèche est
     * pointée dans la direction des <var>x</var> positifs (soit à un angle
     * de 0 radians arithmétiques).
     */
    private static final Shape DEFAULT_SHAPE = new Arrow2D(0, -5, 10, 10);

    /**
     * Couleur des flèches.
     */
    private Color color = new Color(0, 153, 255, 128);

    /**
     * Indices (<var>i</var>,<var>j</var>)
     * du dernier point interrogé.
     */
    private transient int lastI=-1, lastJ=-1;

    /**
     * Valeurs (<var>u</var>,<var>v</var>) du vecteur au
     * point <code>({@link #lastI}, {@link #lastJ})</code>.
     */
    private transient double lastU, lastV;

    /**
     * Buffer temporaire pour l'écriture des "tooltip".
     */
    private transient StringBuffer buffer;

    /**
     * Objet à utiliser pour l'écriture des angles.
     */
    private transient AngleFormat angleFormat;

    /**
     * Procède à la lecture binaire de cet objet,
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
     * contient initialement aucune donnée.
     */
    public VectorLayer()
    {super();}

    /**
     * Construit un champ de vecteur qui utilisera
     * le système de coordonnées spécifié.
     *
     * @param coordinateSystem Système de coordonnées
     *        des positions (<var>x</var>,<var>y</var>).
     */
    public VectorLayer(final CoordinateSystem coordinateSystem)
    {super(coordinateSystem);}

    /**
     * Construit un champ de vecteur qui
     * utilisera l'image spécifiée.
     *
     * @param  coverage Les données à utiliser.
     * @param  bandU Bande de la composante U des vecteurs.
     * @param  bandV Bande de la composante V des vecteurs.
     */
    public VectorLayer(final GridCoverage coverage, final int bandU, final int bandV)
    {
        super(coverage.getCoordinateSystem());
        setData(coverage, bandU, bandV);
    }

    /**
     * Définit l'image à utiliser comme source de données.
     *
     * @param  coverage Les données à utiliser.
     * @param  bandU Bande de la composante U des vecteurs.
     * @param  bandV Bande de la composante V des vecteurs.
     */
    public synchronized void setData(GridCoverage coverage, final int bandU, final int bandV)
    {
        coverage = coverage.geophysics(true);
        final CoordinateSystem cs = CTSUtilities.getCoordinateSystem2D(coverage.getCoordinateSystem());
        if (!cs.equivalents(getCoordinateSystem()))
        {
            // TODO: Il faudrait ajouter une méthode Layer.setCoordinateSystem
            //       et changer le système de coordonnées ici pour prendre celui
            //       des images.
            throw new IllegalArgumentException();
        }
        final SampleDimension[] samples = coverage.getSampleDimensions();
        final SampleDimension   sampleU = samples[bandU];
        final SampleDimension   sampleV = samples[bandV];
        final Unit unitU = sampleU.getUnits();
        final Unit unitV = sampleV.getUnits();
        if (!unitU.equals(unitV))
        {
            throw new IllegalArgumentException();
        }
        this.data  = PlanarImage.wrapRenderedImage(coverage.getRenderedImage());
        this.theme = sampleU;
        this.bandU = bandU;
        this.bandV = bandV;
        final Dimension       size = new Dimension(data.getWidth(), data.getHeight());
        final MathTransform2D  mtr = coverage.getGridGeometry().getGridToCoordinateSystem2D();
        final AffineTransform gref = new AffineTransform((AffineTransform) mtr);
        gref.translate(-data.getMinX(), -data.getMinY());
        // La translation de -min(x,y) est pour faire commencer les indices à (0,0).
        setGrid(size, gref);
        setPreferredArea(coverage.getEnvelope().getSubEnvelope(0,2).toRectangle2D());
    }

    /**
     * Définit la couleur de remplissage des flèches.
     */
    public void setColor(final Color color)
    {this.color=color;}

    /**
     * Retourne les unités de l'amplitude des vecteurs, ou <code>null</code>
     * si ces unités ne sont pas connues. Dans les cas des flèches de courant
     * par exemple, ça sera typiquement des "cm/s".
     */
    public Unit getAmplitudeUnit()
    {return theme.getUnits();}

    /**
     * Retourne l'amplitude typique des données de cette couche.
     */
    public double getTypicalAmplitude()
    {return 25;} // TODO

    /**
     * Retourne l'amplitude horizontale d'un vecteur. Cette amplitude
     * sera exprimée selon les unités de {@link #getAmplitudeUnit}.
     *
     * @param  i Index du point selon <var>x</var>, dans la plage <code>[0..width-1]</code>.
     * @param  j Index du point selon <var>y</var>, dans la plage <code>[0..height-1]</code>.
     * @return Amplitude du vecteur à la position spécifiée, selon les unités {@link #getAmplitudeUnit}.
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
     * @return Direction du vecteur à la position spécifiée, en radians arithmétiques.
     */
    public double getDirection(final int i, final int j)
    {
        if (i!=lastI || j!=lastJ) computeUV(i,j);
        return Math.atan2(lastV, lastU);
    }

    /**
     * Calcule les composantes (<var>u</var>,<var>v</var>)
     * du vecteur à la position indiquée.
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
     * Retourne la forme géométrique servant de modèle au traçage des flèches.
     * Le début de cette flèche à son origine à (0,0) et la flèche pointe dans
     * la direction des <var>x</var> positifs (soit à un angle de 0 radians
     * arithmétiques).
     */
    public Shape getMarkShape(final int i)
    {return DEFAULT_SHAPE;}

    /**
     * Procède au traçage d'une flèche.
     *
     * @param graphics Graphique à utiliser pour tracer la flèche. L'espace de coordonnées
     *                 de ce graphique sera les pixels en les points (1/72 de pouce).
     * @param shape    Forme géométrique représentant la flèche à tracer.
     * @param index    Index de la flèche à tracer.
     */
    protected void paint(final GraphicsJAI graphics, final Shape shape, final int index)
    {
        graphics.setColor(color);
        graphics.fill(shape);
    }

    /**
     * Retourne l'amplitude de la flèche.
     */
    protected synchronized String getToolTipText(final int index)
    {
        if (angleFormat==null)
        {
            buffer = new StringBuffer();
            angleFormat = new AngleFormat("D.dd°");
        }
        double amplitude = getAmplitude(index);
        double angle     = getDirection(index);
        angle = 90-Math.toDegrees(angle);
        angle -= 360*Math.floor(angle/360);

        buffer.setLength(0);
        buffer.append(theme.getLabel(amplitude, (Locale)null));
        buffer.append("  ");
        return angleFormat.format(angle, buffer, null).toString();
    }
}
