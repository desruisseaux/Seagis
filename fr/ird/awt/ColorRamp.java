/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1999 Pêches et Océans Canada
 *               2000 Institut de Recherche pour le Développement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contacts: Observatoire du Saint-Laurent         Michel Petit
 *           Institut Maurice Lamontagne           Institut de Recherche pour le Développement
 *           850 de la Mer, C.P. 1000              500 rue Jean-François Breton
 *           Mont-Joli (Québec)                    34093 Montpellier
 *           G5H 3Z4                               France
 *           Canada
 *
 *           mailto:osl@osl.gc.ca                  mailto:Michel.Petit@teledetection.fr
 */
package fr.ird.awt;

// Graphical user interface
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;

// Graphics
import java.awt.Font;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

// Geometry
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

// Events
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

// Axis
import fr.ird.awt.axis.Graduation;
import fr.ird.awt.axis.TickIterator;
import fr.ird.awt.axis.NumberGraduation;
import fr.ird.awt.axis.AbstractGraduation;
import fr.ird.awt.axis.LogarithmicNumberGraduation;

// Geotools dependencies
import org.geotools.cv.Category;
import org.geotools.cv.CategoryList;
import org.geotools.cv.SampleDimension;
import org.geotools.gc.GridCoverage;

// Miscellaneous
import org.geotools.units.Unit;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import org.geotools.resources.Utilities;


/**
 * A color ramp with a graduation. The colors are specified with a {@link IndexColorModel}
 * object, and the graduation is specified with a {@link Graduation} object. The resulting
 * <code>ColorRamp</code> object is usually painted together with a remote sensing image,
 * for example a {@link fr.ird.map.MapPanel} object.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ColorRamp extends JComponent
{
    /**
     * Margin (in pixel) on each sides: top, left,
     * right and bottom of the color ramp.
     */
    private static final int MARGIN=10;

    /**
     * The graduation to write over the color ramp.
     */
    private Graduation graduation;

    /**
     * Graduation units. This is constructed from
     * {@link Graduation#getUnitLabel} and cached
     * for faster rendering.
     */
    private String units;

    /**
     * The colors to drawn. Color will be drawn from
     * {@link #lower} to {@link #upper} inclusive.
     */
    private IndexColorModel colors;

    /**
     * The index of the first colors
     * to drawn in {@link #colors}.
     */
    private int lower;

    /**
     * The index+1 of the last colors
     * to drawn in {@link #colors}.
     */
    private int upper;

    /**
     * <code>true</code> if tick label must be display.
     */
    private boolean labelVisibles=true;

    /**
     * <code>true</code> if tick label can be display with an automatic
     * color. The automatic color will be white or black depending the
     * background color.
     */
    private boolean autoForeground=true;

    /**
     * <code>true</code> if the color ramp should be drawn horizontally,
     * or <code>false</code> if it should be drawn vertically.
     */
    private boolean horizontal=true;

    /**
     * Rendering hints for the graduation. This include
     * the color ramp length, which is used for the
     * space between ticks.
     */
    private transient RenderingHints hints;

    /**
     * The tick iterator used during the last painting.
     * This iterator will be reused as mush as possible
     * in order to reduce garbage-collections.
     */
    private transient TickIterator reuse;

    /**
     * A temporary buffer for conversions from RGB to HSB
     * values. This is used by {@link #getForeground(int)}.
     */
    private transient float[] HSB;

    /**
     * The {@link ComponentUI} object for computing preferred
     * size, drawn the component and handle some events.
     */
    private final UI ui=new UI();

    /**
     * Construct an initially empty <code>ColorRamp</code>. Colors
     * can be set using on of the <code>setColors(...)</code> methods.
     */
    public ColorRamp()
    {
        setOpaque(true);
        setUI(ui);
    }

    /**
     * Returns the graduation to paint over colors.
     * If the graduation is not yet defined, then
     * this method returns <code>null</code>.
     */
    public Graduation getGraduation()
    {return graduation;}

    /**
     * Returns the underlying {@link IndexColorModel} used by this
     * <code>ColorRamp</code>.  The <code>ColorRamp</code> may not
     * paint all colors from the color model. The range of painted
     * colors are delimited by the <code>lower</code> and <code>upper</code>
     * bounds.
     */
    public IndexColorModel getColorRamp()
    {return colors;}

    /**
     * Tests if two {@link IndexColorModel} has the same colors.
     *
     * @param     c1 The first {@link IndexColorModel} to test.
     * @param   off1 The first color to check into <code>c1</code>.
     * @param     c2 The second {@link IndexColorModel} to test.
     * @param   off2 The first color to check into <code>c2</code>.
     * @param length Number of colors to check.
     */
    private static boolean equals(final IndexColorModel c1, int off1, final IndexColorModel c2, int off2, int length)
    {
        if (c1!=c2 || off1!=off2)
        {
            if (c1==null || c2==null)
            {
                return false;
            }
            while (--length >= 0)
                if (c1.getRGB(off1++) != c2.getRGB(off2++))
                    return false;
        }
        return true;
    }

    /**
     * Sets the colors to paint. Only indexed colors in the range
     * <code>lower</code> inclusive to <code>upper</code> exclusive
     * will be painted.
     *
     * @param graduation The graduation to paint over colors.
     * @param colors The colors to paint.
     * @param lower  First color index to paint, inclusive.
     * @param upper  Last  color index to paint, exclusive.
     * @return <code>true</code> if the state of this <code>ColorRamp</code>
     *         changed as a result of this call. If this method returns
     *         <code>false</code>, then this component doesn't
     *         need a repaint since the change is not visible.
     *
     * @see #setColorRamp(CategoryList)
     * @see #setColorRamp(GridCoverage)
     * @see #getColorRamp()
     * @see #getGraduation()
     */
    public boolean setColorRamp(final Graduation graduation, final IndexColorModel colors, final int lower, final int upper)
    {
        /*
         * Checks if the change may have a visual impact. This will
         * be <code>true</code> if the graduation changed, or if
         * some colors in the [lower..upper] range are different.
         */
        boolean changed = !Utilities.equals(graduation, this.graduation);
        if (changed)
        {
            final int length = upper-lower;
            changed = (length != this.upper-this.lower) ||
                      !equals(colors, lower, this.colors, this.lower, length);
        }
        /*
         * Register the new graduation.
         * and store the new values.
         */
        if (graduation != this.graduation)
        {
            if (this.graduation!=null)
                this.graduation.removePropertyChangeListener(ui);
            if (graduation!=null)
                graduation.addPropertyChangeListener(ui);
        }
        if (graduation != null)
        {
            this.units  = graduation.getUnitLabel();
        }
        this.graduation = graduation;
        this.colors     = colors;
        this.lower      = lower;
        this.upper      = upper;
        if (changed) repaint();
        return changed;
    }

    /**
     * Sets the colors to paint. The range of indexed colors and the
     * minimum and maximum values are fetched from the supplied
     * category list.
     *
     * @param categories The category list.
     * @param colors The colors to paint.
     * @return <code>true</code> if the state of this <code>ColorRamp</code>
     *         changed as a result of this call. If this method returns
     *         <code>false</code>, then this component doesn't
     *         need a repaint since the change is not visible.
     *
     * @see #setColorRamp(Graduation,IndexedColorModel)
     * @see #setColorRamp(CategoryList)
     * @see #setColorRamp(GridCoverage)
     * @see #getColorRamp()
     * @see #getGraduation()
     */
    private boolean setColorRamp(final CategoryList categories, final IndexColorModel colors)
    {
        /*
         * Looks for what seems to be the "main" category. We look for the
         * quantitative category (if there is one) with the widest sample range.
         */
        int range=0;
        Category category=null;
        for (int i=categories.size(); --i>=0;)
        {
            final Category candidate = categories.get(i);
            if (candidate!=null && candidate.isQuantitative())
            {
                final int candidateRange = candidate.upper - candidate.lower;
                if (candidateRange >= range)
                {
                    range = candidateRange;
                    category = candidate;
                }
            }
        }
        if (category==null)
        {
            return setColorRamp(null, null, 0, 0);
        }
        /*
         * Now that we know what seems to be the "main" category,
         * construct a graduation for it.
         */
        final int  lower = category.lower;
        final int  upper = category.upper;
        double min = category.toValue(lower);
        double max = category.toValue(upper);
        if (min > max)
        {
            // This case occurs typically when displaying a color ramp for
            // sea bathymetry, for which floor level are negative numbers.
            min = -min;
            max = -max;
        }
        if (!(min <= max))
        {
            throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_BAD_ARGUMENT_$2, "category", category));
        }
        AbstractGraduation graduation = (this.graduation instanceof AbstractGraduation) ? (AbstractGraduation) this.graduation : null;
        graduation = createGraduation(graduation, category, categories.getUnits());
        /*
         * Set the color ramp using the new graduation. We want that
         * EVERY lines below to be executed, even if one of them
         * returns 'false'. This is why we use '|' instead of '||'.
         */
        return graduation != this.graduation |  // NOT ||
               graduation.setMinimum(min)    |  // NOT ||
               graduation.setMaximum(max)    |  // NOT ||
               setColorRamp(graduation, colors, lower, upper);
    }

    /**
     * Sets the colors to paint. The range of indexed colors and the
     * minimum and maximum values are fetched from the supplied
     * category list.
     *
     * @param categories The category list, or <code>null</code>.
     * @return <code>true</code> if the state of this <code>ColorRamp</code>
     *         changed as a result of this call. If this method returns
     *         <code>false</code>, then this component doesn't
     *         need a repaint since the change is not visible.
     *
     * @see #setColorRamp(Graduation,IndexedColorModel)
     * @see #setColorRamp(GridCoverage)
     * @see #getColorRamp()
     * @see #getGraduation()
     */
    public boolean setColorRamp(final CategoryList categories)
    {
        if (categories==null)
        {
            return setColorRamp(null, null, 0, 0);
        }
        final ColorModel colors = categories.getColorModel(false);
        // TODO: What whould we do in case of ClassCastException? Open question...
        return setColorRamp(categories, (IndexColorModel) colors);
    }

    /**
     * Sets the colors to paint. The range of indexed colors and the
     * minimum and maximum values are fetched from the supplied
     * grid coverage.
     *
     * @param coverage The grid coverage, or <code>null</code>.
     * @return <code>true</code> if the state of this <code>ColorRamp</code>
     *         changed as a result of this call. If this method returns
     *         <code>false</code>, then this component doesn't
     *         need a repaint since the change is not visible.
     *
     * @see #setColorRamp(Graduation,IndexedColorModel)
     * @see #setColorRamp(CategoryList)
     * @see #getColorRamp()
     * @see #getGraduation()
     */
    public boolean setColorRamp(final GridCoverage coverage)
    {
        if (coverage==null)
        {
            return setColorRamp(null, null, 0, 0);
        }
        final ColorModel       colors = coverage.getRenderedImage(false).getColorModel();
        final CategoryList categories = coverage.getSampleDimensions()[0].getCategoryList();
        // TODO: What whould we do in case of ClassCastException? Open question...
        return setColorRamp(categories, (IndexColorModel) colors);
    }

    /**
     * Returns the component's orientation (horizontal or vertical).
     * It should be one of the following constants:
     * ({@link SwingConstants#HORIZONTAL} or {@link SwingConstants#VERTICAL}).
     */
    public int getOrientation()
    {return (horizontal) ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL;}

    /**
     * Set the component's orientation (horizontal or vertical).
     *
     * @param orient {@link SwingConstants#HORIZONTAL}
     *        or {@link SwingConstants#VERTICAL}.
     */
    public void setOrientation(final int orient)
    {
        switch (orient)
        {
            case SwingConstants.HORIZONTAL: horizontal=true;  break;
            case SwingConstants.VERTICAL:   horizontal=false; break;
            default: throw new IllegalArgumentException();
        }
    }

    /**
     * Tests if graduation labels are paint on top of the
     * colors ramp. Default value is <code>true</code>.
     */
    public boolean isLabelVisibles()
    {return labelVisibles;}

    /**
     * Sets whatever the graduation labels should
     * be painted on top of the colors ramp.
     */
    public void setLabelVisibles(final boolean visible)
    {labelVisibles=visible;}

    /**
     * Sets the label colors. A <code>null</code>
     * value reset the automatic color.
     *
     * @see #getForeground
     */
    public void setForeground(final Color color)
    {
        super.setForeground(color);
        autoForeground=(color==null);
    }

    /**
     * Returns a color for label at the specified index.
     * The default color will be black or white, depending
     * of the background color at the specified index.
     */
    private Color getForeground(final int colorIndex)
    {
        final int R=colors.getRed  (colorIndex);
        final int G=colors.getGreen(colorIndex);
        final int B=colors.getBlue (colorIndex);
        HSB = Color.RGBtoHSB(R,G,B, HSB);
        return (HSB[2]>=0.5f) ? Color.black : Color.white;
    }

    /**
     * Paint the color ramp. This method doesn't need to
     * restore {@link Graphics2D} to its initial state
     * once finished.
     *
     * @param  graphics The graphic context in which to paint.
     * @param  bounds   The bounding box where to paint the color ramp.
     * @return Bounding box of graduation labels (NOT takind in account
     *         the color ramp in behind them), or <code>null</code> if
     *         no label has been painted.
     */
    private Rectangle2D paint(final Graphics2D graphics, final Rectangle bounds)
    {
        final int lower = this.lower;
        final int upper = this.upper;
        if (lower<upper)
        {
            int R,G,B;
            int i=lower;
            int lastIndex=0;
            int CR=R=colors.getRed  (i);
            int CG=G=colors.getGreen(i);
            int CB=B=colors.getBlue (i);
            final int    ox = bounds.x+MARGIN;
            final int    oy = bounds.y+bounds.height-MARGIN;
            final double dx = (double)(bounds.width -2*MARGIN)/(upper-lower);
            final double dy = (double)(bounds.height-2*MARGIN)/(upper-lower);
            final Rectangle2D.Double rect=new Rectangle2D.Double();
            rect.setRect(bounds);
            while (++i<=upper)
            {
                if (i!=upper)
                {
                    CR=colors.getRed  (i);
                    CG=colors.getGreen(i);
                    CB=colors.getBlue (i);
                    if (R==CR && G==CG && B==CB)
                    {
                        continue;
                    }
                }
                final int index=i-lower;
                if (horizontal)
                {
                    rect.x      = ox+dx*lastIndex;
                    rect.width  = dx*(index-lastIndex);
                    if (lastIndex==0)
                    {
                        rect.x     -= MARGIN;
                        rect.width += MARGIN;
                    }
                    if (i==upper)
                    {
                        rect.width += MARGIN;
                    }
                }
                else
                {
                    rect.y      = oy-dy*index;
                    rect.height = dy*(index-lastIndex);
                    if (lastIndex==0)
                    {
                        rect.height += MARGIN;
                    }
                    if (i==upper)
                    {
                        rect.y      -= MARGIN;
                        rect.height += MARGIN;
                    }
                }
                graphics.setColor(new Color(R,G,B));
                graphics.fill(rect);
                lastIndex = index;
                R = CR;
                G = CG;
                B = CB;
            }
        }
        Rectangle2D labelBounds=null;
        if (labelVisibles && graduation!=null)
        {
            /*
             * Prépare l'écriture de la graduation. On vérifie quelle longueur
             * (en pixels) a la rampe de couleurs et on calcule les coéfficients
             * qui permettront de convertir les valeurs logiques en coordonnées
             * pixels.
             */
            double x=bounds.getCenterX();
            double y=bounds.getCenterY();
            final double axisRange   = graduation.getRange();
            final double axisMinimum = graduation.getMinimum();
            final double visualLength, scale, offset;
            if (horizontal)
            {
                visualLength = bounds.getWidth() - 2*MARGIN;
                scale        = visualLength/axisRange;
                offset       = (bounds.getMinX()+MARGIN) - scale*axisMinimum;
            }
            else
            {
                visualLength = bounds.getHeight() - 2*MARGIN;
                scale        = -visualLength/axisRange;
                offset       = (bounds.getMaxY()-MARGIN) + scale*axisMinimum;
            }
            if (hints==null)          hints = new RenderingHints(null);
            final RenderingHints      hints = this.hints;
            final double              ratio = (upper-lower)/axisRange;
            final Font                 font = getFont();
            final FontRenderContext context = graphics.getFontRenderContext();
            hints.put(Graduation.VISUAL_AXIS_LENGTH, new Float((float)visualLength));
            graphics.setColor(getForeground());
            /*
             * Procède à l'écriture de la graduation. Si l'utilisateur avait
             * demandé à pâlir un peu l'arrière plan de la graduation (ça aide
             * un peu à la lecture), il faudra préparer un
             */
            for (final TickIterator ticks=reuse=graduation.getTickIterator(hints, reuse); ticks.hasNext(); ticks.nextMajor())
            {
                if (ticks.isMajorTick())
                {
                    final GlyphVector glyph = font.createGlyphVector(context, ticks.getLabel());
                    final Rectangle2D rectg = glyph.getVisualBounds();
                    final double      width = rectg.getWidth();
                    final double     height = rectg.getHeight();
                    final double      value = ticks.getValue();
                    final double   position = value*scale+offset;
                    final int    colorIndex = Math.min(Math.max((int)Math.round((value-axisMinimum)*ratio),0)+lower, upper-1);
                    if (horizontal) x=position; else y=position;
                    rectg.setRect(x-0.5*width, y-0.5*height, width, height);
                    if (autoForeground)
                    {
                        graphics.setColor(getForeground(colorIndex));
                    }
                    graphics.drawGlyphVector(glyph, (float)rectg.getMinX(), (float)rectg.getMaxY());
                    if (labelBounds!=null) labelBounds.add(rectg);
                    else labelBounds=rectg;
                }
            }
            /*
             * Ecrit les unités.
             */
            if (units!=null)
            {
                final GlyphVector glyph = font.createGlyphVector(context, units);
                final Rectangle2D rectg = glyph.getVisualBounds();
                final double      width = rectg.getWidth();
                final double     height = rectg.getHeight();
                if (horizontal)
                {
                    double left = bounds.getMaxX()-width;
                    if (labelBounds!=null)
                    {
                        final double check = labelBounds.getMaxX()+4;
                        if (check<left) left=check;
                    }
                    rectg.setRect(left, y-0.5*height, width, height);
                }
                else
                {
                    rectg.setRect(x-0.5*width, bounds.getMinY()+height, width, height);
                }
                if (autoForeground)
                {
                    graphics.setColor(getForeground(Math.max(lower, upper-1)));
                }
                if (labelBounds==null || !labelBounds.intersects(rectg))
                {
                    graphics.drawGlyphVector(glyph, (float)rectg.getMinX(), (float)rectg.getMaxY());
                }
            }
        }
        return labelBounds;
    }

    /**
     * Returns a graduation for the specified category. This method must returns
     * a graduation of the appropriate class (e.g. {@link NumberGraduation} or
     * {@link LogarithmicNumberGraduation}), but doesn't have to set any graduation's
     * properties like minimum and maximum values. This will be handle by the caller.
     * <br><br>
     * If the supplied <code>reuse</code> object is non-null and is of the appropriate
     * class, then this method can returns <code>reuse</code> without creating a new
     * graduation. This help to reduce garbage collection.
     *
     * @param  reuse The graduation to reuse if possible.
     * @param  category The category to create graduation for.
     * @param  units The units for the graduation.
     * @return A graduation for the supplied category. The minimum, maximum
     *         and units doesn't need to bet set at this stage.
     */
    protected AbstractGraduation createGraduation(final AbstractGraduation reuse, final Category category, final Unit units)
    {
        // TODO: How to detect if the category is logarithmic?
        if (true)
        {
            if (reuse==null || !reuse.getClass().equals(NumberGraduation.class))
                return new NumberGraduation(units);
        }
        else
        {
            if (reuse==null || !reuse.getClass().equals(LogarithmicNumberGraduation.class))
                return new LogarithmicNumberGraduation(units);
        }
        return reuse;
    }

    /**
     * Returns a string representation for this color ramp.
     */
    public String toString()
    {
        int count=0;
        int i=lower;
        if (i<upper)
        {
            int last=colors.getRGB(i);
            while (++i<upper)
            {
                int c=colors.getRGB(i);
                if (c!=last)
                {
                    last=c;
                    count++;
                }
            }
        }
        return "ColorRamp["+count+" colors]";
    }

    /**
     * Notifies this component that it now has a parent component.
     * This method is invoked by <em>Swing</em> and shouldn't be
     * directly used.
     */
    public void addNotify()
    {
        super.addNotify();
        if (graduation!=null)
        {
            graduation.removePropertyChangeListener(ui); // Avoid duplication
            graduation.addPropertyChangeListener(ui);
        }
    }

    /**
     * Notifies this component that it no longer has a parent component.
     * This method is invoked by <em>Swing</em> and shouldn't be directly used.
     */
    public void removeNotify()
    {
        if (graduation!=null)
        {
            graduation.removePropertyChangeListener(ui);
        }
        super.removeNotify();
    }





    /**
     * Classe ayant la charge de dessiner la rampe de couleurs, ainsi que
     * de calculer l'espace qu'elle occupe. Cette classe peut aussi réagir
     * à certains événements.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class UI extends ComponentUI implements PropertyChangeListener
    {
        /**
         * Retourne la dimension minimale
         * de cette rampe de couleurs.
         */
        public Dimension getMinimumSize(final JComponent c)
        {return (((ColorRamp) c).horizontal) ? new Dimension(2*MARGIN,16) : new Dimension(16,2*MARGIN);}

        /**
         * Retourne la dimension préférée
         * de cette rampe de couleurs.
         */
        public Dimension getPreferredSize(final JComponent c)
        {return (((ColorRamp) c).horizontal) ? new Dimension(256,16) : new Dimension(16,256);}

        /**
         * Dessine la rampe de couleurs vers le graphique spécifié.  Cette méthode a
         * l'avantage d'être appelée automatiquement par <i>Swing</i> avec une copie
         * d'un objet {@link Graphics}, ce qui nous évite d'avoir à le remettre dans
         * son état initial lorsqu'on a terminé le traçage de la rampe de couleurs.
         * On n'a pas cet avantage lorsque l'on ne fait que redéfinir
         * {@link JComponent#paintComponent}.
         */
        public void paint(final Graphics graphics, final JComponent component)
        {
            final ColorRamp ramp = (ColorRamp) component;
            if (ramp.colors!=null)
            {
                final Rectangle bounds=ramp.getBounds();
                bounds.x=0;
                bounds.y=0;
                ramp.paint((Graphics2D) graphics, bounds);
            }
        }

        /**
         * Méthode appelée automatiquement chaque
         * fois qu'une propriété de l'axe a changée.
         */
        public void propertyChange(final PropertyChangeEvent event)
        {repaint();}
    }
}
