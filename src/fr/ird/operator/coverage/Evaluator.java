/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
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
package fr.ird.operator.coverage;

// Miscellaneous
import java.awt.Color;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.RectangularShape;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.RenderedImage;
import java.util.Locale;
import java.util.Arrays;
import javax.vecmath.MismatchedSizeException;

// Geotools dependencies
import org.geotools.units.Unit;
import org.geotools.pt.Envelope;
import org.geotools.cv.Category;
import org.geotools.cv.Coverage;
import org.geotools.cv.SampleDimension;
import org.geotools.gc.GridCoverage;
import org.geotools.ct.MathTransform1D;
import org.geotools.pt.CoordinatePoint;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cv.CannotEvaluateException;
import org.geotools.resources.geometry.XAffineTransform;
import org.geotools.resources.Utilities;
import org.geotools.util.NumberRange;


/**
 * Fonction � �valuer sur des r�gions d'un objet {@link GridCoverage}. Par exemple la fonction
 * {@link AverageEvaluator} calcule la moyenne des valeurs de pixels trouv�es � l'int�rieur
 * de la r�gion g�ographique sp�cifi�e au constructor.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Evaluator extends Coverage {
    /**
     * La plage de valeurs des pixels sans donn�es manquantes.
     */
    private static final NumberRange SAMPLE_RANGE;

    /**
     * La plage de valeurs des pixels excluant la valeur de la donn�e manquante.
     * Par convention, les donn�es manquantes auront la valeur 0. Cette valeur 0
     * ne doit pas �tre confondue avec le 0 des valeurs g�ophysiques,  qui elles
     * sont trait�es diff�rements.
     */
    private static final NumberRange SAMPLE_RANGE_X;

    /**
     * Cat�gorie des donn�es manquantes. La place de valeurs de cette cat�gorie
     * ne comprend que le 0.
     */
    private static final Category CATEGORY_NODATA;

    /**
     * Couleurs par d�faut � utiliser pour les bandes des coordonn�es
     * (<var>x</var>,<var>y</var>,<var>z</var>...).
     */
    private static final Color[] COLORS = null;
    static {
        final NumberRange SAMPLE_MISSING;
        final Integer zero  = new Integer(  0);
        final Integer upper = new Integer(256);
        SAMPLE_RANGE    = new NumberRange(Integer.class, zero, true , upper, false);
        SAMPLE_RANGE_X  = new NumberRange(Integer.class, zero, false, upper, false);
        SAMPLE_MISSING  = new NumberRange(Integer.class, zero, true , zero , true );
        CATEGORY_NODATA = new Category("No data", null, SAMPLE_MISSING, (MathTransform1D) null);
    }

    /**
     * Le nom de cette op�ration. Ce nom sera ajout� comme pr�fix au nom de l'image
     * {@link #coverage}. Par exemple si le nom de l'image source est "Temp�rature" et
     * que le nom de cette op�ration est "Minimum", alors la m�thode {@link #getName}
     * retournera "Minimum[Temp�rature]".
     */
    private final String name;

    /**
     * Les donn�es sur lesquelle appliquer cet �valuateur.
     */
    protected final GridCoverage coverage;

    /**
     * Description des bandes de cet objet {@link Coverage}. En g�n�ral, il y aura au
     * moins une bande pour la valeur �valu�e, et 2 ou 3 bandes pour les coordonn�es
     * (<var>x</var>,<var>y</var>,<var>z</var>...) de la fonction �valu�e. Ainsi, on
     * peut avoir par exemple la valeur maximale trouv�e dans une certaine r�gion ainsi
     * que les coordonn�es de cette valeur maximale.
     */
    protected SampleDimension[] bands;

    /**
     * Forme g�om�trique de la r�gion � calculer. Cette forme sera modifi�e � chaque
     * appel de {@link #evaluate(CoordinatePoint,double[])}.
     */
    private final RectangularShape area;

    /**
     * The coordinate of the {@link #area}'s center, usually (0,0).
     */
    private final double xOffset, yOffset;

    /**
     * Construit un �valuateur pour l'image sp�cifi�e. Les premi�res bandes de l'images auront
     * la m�me plage de valeurs et les m�mes unit�s que l'image source (<code>coverage</code>),
     * et peuvent �tre suivit (si <code>numAxisSet &gt; 0} de bandes contenant les coordonn�es
     * g�ographiques (<var>x</var>,<var>y</var>,<var>z</var>...) repr�sentatives des valeurs
     * �valu�es. Si ces bandes ne conviennent pas, les constructeurs des classes d�riv�es peuvent
     * modifier directement le tableau {@link #bands} � leur guise, � la condition de le laisser
     * inchang� une fois la construction termin�e.
     *
     * @param name Le nom de cette op�ration. Ce nom sera ajout� comme pr�fix au nom de l'image
     *             {@link #coverage}. Par exemple si le nom de l'image source est "Temp�rature" et
     *             que le nom de cette op�ration est "Minimum", alors la m�thode {@link #getName}
     *             retournera "Minimum[Temp�rature]".
     * @param numAxisSet Nombre de fois que les bandes des coordonn�es <var>x</var>, <var>y</var>,
     *             <var>z</var>, etc. doivent �tre r�p�t�es. Par exemple la valeur 2 ajoutera les
     *             bandes suivantes: <var>x</var>,<var>x</var>, <var>y</var>,<var>y</var>, etc.
     * @param coverage Les donn�es sources.
     * @param area La forme g�om�trique de la r�gion � �valuer. La coordonn�es (0,0) de cette
     *             forme correspondra au point sur lequel la fonction sera appliqu�e. Tous les
     *             points avoisinant qui se trouvent � l'int�rieur de cette forme <code>area</code>
     *             seront pris en compte pour le calcul.
     */
    public Evaluator(final String           name,
                     final int        numAxisSet,
                     final GridCoverage coverage,
                     final RectangularShape area)
    {
        super(coverage);
        if (numAxisSet < 0) {
            throw new IllegalArgumentException(String.valueOf(numAxisSet));
        }
        this.name     = name;
        this.coverage = coverage;
        this.area     = (RectangularShape) area.clone();
        this.xOffset  = area.getCenterX();
        this.yOffset  = area.getCenterY();

        final Locale       locale = null;
        final Envelope   envelope = getEnvelope();
        final CoordinateSystem cs = getCoordinateSystem();
        final String[]   dimNames = getDimensionNames(locale);
        final SampleDimension[] s = coverage.getSampleDimensions();
        final StringBuffer buffer = new StringBuffer(name); buffer.append('[');
        final int      bufferBase = buffer.length();
        bands = new SampleDimension[s.length + dimNames.length*numAxisSet];
        /*
         * Ajoute des bandes calqu�es sur celles de l'image 'coverage'.
         * La m�me plage de valeurs et les m�mes unit�s sont utilis�es.
         */
        Category[] category;
        category    = new Category[2];
        category[0] = CATEGORY_NODATA;
        for (int i=0; i<s.length; i++) {
            final SampleDimension  src = s[i].geophysics(true);
            final NumberRange geoRange = src.getRange();
            final Category        main = src.getCategory((((Number)geoRange.getMinValue()).doubleValue() +
                                                          ((Number)geoRange.getMaxValue()).doubleValue())/2);
            buffer.setLength(bufferBase);
            buffer.append(main.getName(locale));
            buffer.append(']');
            category[1] = new Category(buffer.toString(), main.geophysics(false).getColors(),
                                       SAMPLE_RANGE_X, geoRange);
            bands[i] = new SampleDimension(category, src.getUnits());
        }
        /*
         * Ajoute des bandes pour chaque axes du syst�me de coordonn�es.
         */
        category = new Category[1];
        for (int i=0; i<dimNames.length; i++) {
            category[0] = new Category(dimNames[i], COLORS, SAMPLE_RANGE,
                          new NumberRange(envelope.getMinimum(i), true,
                                          envelope.getMaximum(i), false));
            final int k = s.length + numAxisSet*i;
            Arrays.fill(bands, k, k+numAxisSet, new SampleDimension(category, cs.getUnits(i)));
        }
    }
    
    /**
     * Returns the coverage name, localized for the supplied locale.
     *
     * @param  locale The desired locale, or <code>null</code> for a default locale.
     * @return The coverage name in the specified locale, or in an arbitrary locale
     *         if the specified localization is not available.
     */
    public String getName(final Locale locale) {
        return name + '[' + coverage.getName(locale) + ']';
    }

    /**
     * Returns The bounding box for the coverage domain in coordinate system coordinates.
     */
    public Envelope getEnvelope() {
        final Envelope envelope = coverage.getEnvelope();
        // Shrink the envelope here if necessary.
        return envelope;
    }

    /**
     * Retourne une description des bandes de cet objet {@link Coverage}.
     * L'impl�mentation par d�faut retourne un clone de {@link #bands}.
     */
    public SampleDimension[] getSampleDimensions() {
        return (SampleDimension[]) bands.clone();
    }

    /**
     * Calcule la valeur de cette fonction autour de la position sp�cifi�e. L'impl�mentation
     * par d�faut appelle {@link #evaluate(Shape,double[])} avec la forme g�om�trique de la
     * r�gion � calculer. Cette forme sera calqu�e sur le modele sp�cifi� au constructeur.
     *
     * @param  coord Coordonn�e du point autour duquel �valuer la fonction.
     * @param  dest  Tableau dans lequel m�moriser le r�sultat, ou <code>null</code>.
     * @return Les r�sultats par bandes.
     * @throws CannotEvaluateException si la fonction ne peut pas �tre �valu�e, par exemple parce
     *         que la coordonn�e est en dehors de la couverture de l'image {@link #coverage}.
     */
    public double[] evaluate(CoordinatePoint coord, double[] dest) throws CannotEvaluateException {
        final double width  = area.getWidth();
        final double height = area.getHeight();
        area.setFrame(coord.getOrdinate(0) + xOffset - width/2,
                      coord.getOrdinate(1) + yOffset - height/2,
                      width, height);
        return evaluate(area, dest);
    }

    /**
     * Calcule la valeur de cette fonction dans la r�gion g�ographique sp�cifi�e.
     *
     * @param  area  R�gion g�ographique autour de laquelle �valuer la fonction.
     * @param  dest  Tableau dans lequel m�moriser le r�sultat, ou <code>null</code>.
     * @return Les r�sultats par bandes.
     * @throws CannotEvaluateException si la fonction ne peut pas �tre �valu�e, par exemple parce
     *         que la coordonn�e est en dehors de la couverture de l'image {@link #coverage}.
     */
    public abstract double[] evaluate(final Shape area, double[] dest) throws CannotEvaluateException;

    /**
     * Calcule la valeur de cette fonction dans la r�gion g�ographique sp�cifi�e.
     *
     * @param  area  R�gion g�ographique autour de laquelle �valuer la fonction.
     * @param  dest  Tableau dans lequel m�moriser le r�sultat, ou <code>null</code>.
     * @return Les r�sultats par bandes.
     * @throws CannotEvaluateException si la fonction ne peut pas �tre �valu�e, par exemple parce
     *         que la coordonn�e est en dehors de la couverture de l'image {@link #coverage}.
     */
    public final float[] evaluate(final Shape area, float[] dest) throws CannotEvaluateException {
        double[] buffer = null;
        buffer = evaluate(area, buffer);
        if (dest == null) {
            dest = new float[buffer.length];
        }
        for (int i=0; i<buffer.length; i++) {
            dest[i] = (float)buffer[i];
        }
        return dest;
    }

    /**
     * Transform a geographic bounding box into a grid bounding box.
     * The resulting bounding box will be clipped to image's bounding
     * box.
     *
     * @param areaBounds The geographic bounding box.
     * @param transform The grid to coordinate system transform. The inverse
     *        transform will be used for transforming <code>areaBounds</code>.
     * @param data The rendered image for which the bounding box is computed.
     */
    static Rectangle getBounds(final Rectangle2D     areaBounds,
                               final AffineTransform transform,
                               final RenderedImage   data)
    {
        // 'Rectangle' performs the correct rounding.
        Rectangle bounds = new Rectangle();
        try {
            bounds = (Rectangle)XAffineTransform.inverseTransform(transform, areaBounds, bounds);
            int xmin = data.getMinX();
            int ymin = data.getMinY();
            int xmax = data.getWidth()  + xmin;
            int ymax = data.getHeight() + ymin;
            int t;
            if ((t =bounds.x     ) > xmin) xmin = t;
            if ((t+=bounds.width ) < xmax) xmax = t;
            if ((t =bounds.y     ) > ymin) ymin = t;
            if ((t+=bounds.height) < ymax) ymax = t;
            bounds.x      = xmin;
            bounds.y      = ymin;
            bounds.width  = xmax-xmin;
            bounds.height = ymax-ymin;
        } catch (NoninvertibleTransformException exception) {
            Utilities.unexpectedException("fr.ird.operator", "Evaluator", "evaluate", exception);
            // Returns an empty bounds.
        }
        return bounds;
    }
}
