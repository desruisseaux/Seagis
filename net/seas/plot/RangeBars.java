/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
 * Copyright (C) 1999 P�ches et Oc�ans Canada
 *               2002 Institut de Recherche pour le D�veloppement
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
package net.seas.plot;

// User interface
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.BoundedRangeModel;
import javax.swing.text.JTextComponent;
import javax.swing.DefaultBoundedRangeModel;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

// Graphics
import java.awt.Font;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;

// Geometry
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Collections
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.lang.reflect.Array;
import java.util.ConcurrentModificationException;

// Timezone and units
import java.util.Date;
import java.util.TimeZone;
import org.geotools.units.Unit;

// Formats
import java.text.Format;

// Axis and graduation
import net.seas.plot.axis.Axis;
import net.seas.plot.axis.Graduation;
import net.seas.plot.axis.DateGraduation;
import net.seas.plot.axis.NumberGraduation;
import net.seas.plot.axis.AbstractGraduation;

// Resources
import net.seas.resources.Resources;
import net.seas.resources.ResourceKeys;
import net.seas.awt.MouseReshapeTracker;

// Geotools dependencies
import org.geotools.gui.swing.ZoomPane;
import org.geotools.gui.swing.ExceptionMonitor;


/**
 * Paneau repr�sentant les plages des donn�es disponibles. Ces plages sont
 * repr�sent�es par des barres verticales. L'axe des <var>x</var> repr�sente
 * les valeurs, et sur l'axe des <var>y</var> on place les diff�rents types
 * de donn�es, un peu comme le ferait un histogramme.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="{@docRoot}/doc-files/images/window/RangeBars.gif"></p>
 * <p>&nbsp;</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class RangeBars extends ZoomPane
{
    /**
     * Donn�es des barres. Chaque entr� est constitu� d'une paire
     * (<em>�tiquette</em>, <em>tableau de donn�es</em>). Le tableau de
     * donn�e sera g�n�ralement (mais pas obligatoirement) un tableau de
     * type <code>long[]</code>. Les donn�es de ces tableaux seront organis�es
     * par paires de valeurs, de la forme (<i>d�but</i>,<i>fin</i>).
     */
    private final Map<String,RangeSet> ranges=new LinkedHashMap<String,RangeSet>();

    /**
     * Axe des <var>x</var> servant � �crire les valeurs des plages. Les
     * m�thodes de {@link Axis} peuvent �tre appell�es pour modifier le format
     * des nombres, une �tiquette, des unit�s ou pour sp�cifier "� la main" les
     * minimums et maximums.
     */
    private final Axis axis;

    /**
     * Valeur minimale � avoir �t� sp�cifi�e avec {@link #addRange}.
     * Cette valeur n'est pas valide si <code>(minimum<maximum)</code>
     * est <code>false</code>. Cette valeur peut etre calcul�e par un
     * appel � {@link #update}.
     */
    private transient double minimum;

    /**
     * Valeur maximale � avoir �t� sp�cifi�e avec {@link #addRange}.
     * Cette valeur n'est pas valide si <code>(minimum<maximum)</code>
     * est <code>false</code>. Cette valeur peut etre calcul�e par un
     * appel � {@link #update}.
     */
    private transient double maximum;

    /**
     * Coordonn�es (en pixels) de la r�gion dans laquelle seront dessin�es
     * les �tiquettes. Ce champ est nul si ces coordonn�es ne sont pas encore
     * connues. Ces coordonn�es sont calcul�es par
     *
     *                    {@link #paintComponent(Graphics2D)}
     *
     * (notez que cette derni�re accepte un argument {@link Graphics2D} nul).
     */
    private transient Rectangle labelBounds;

    /**
     * Coordonn�es (en pixels) de la r�gion dans laquelle sera dessin�e l'axe.
     * Ce champ est nul si ces coordonn�es ne sont pas encore connues. Ces
     * coordonn�es sont calcul�es par {@link #paintComponent(Graphics2D)}
     * (notez que cette derni�re accepte un argument {@link Graphics2D} nul).
     */
    private transient Rectangle axisBounds;

    /**
     * Indique si cette composante sera orient�e horizontalement ou
     * verticalement.
     */
    private final boolean horizontal;

    /**
     * Indique si la m�thode {@link #reset} a �t� appel�e
     * sur cet objet avec une dimension valide de la fen�tre.
     */
    private boolean valid;

    /**
     * Espaces (en pixels) � laisser de chaque c�t�s
     * du graphique. Ces dimensions seront retourn�es
     * par {@link #getInsets}.
     */
    private short top=12, left=12, bottom=6, right=15;

    /**
     * Hauteur (en pixels) des barres des histogrammes.
     */
    private final short barThickness=12;

    /**
     * Espace (en pixels) entre les �tiquettes et leurs barres.
     */
    private final short barOffset=6;

    /**
     * Espace (en pixels) � ajouter entre deux lignes.
     */
    private final short lineSpacing=6;

    /**
     * Empirical value (in pixels) to add to <code>labelBounds.width</code>
     * after painting vertical bars. I cant' understand why it is needed!
     */
    private static final int VERTICAL_ADJUSTMENT = 4;

    /**
     * The background color for the zoomable area (default to white).
     * This is different from the widget's background color (default
     * to gray), which is specified with {@link #setBackground(Color)}.
     */
    private final Color backgbColor = Color.white;

    /**
     * The bars color (default to orange).
     */
    private final Color barColor = new Color(255, 153, 51);

    /**
     * The slider color. Default to a transparent purple.
     */
    private final Color selColor = new Color(128,  64,  92, 64);

    /*
     * There is no field for label color. Label color can
     * be specified with {@link #setForeground(Color)}.
     */

    /**
     * The border to paint around the zoomable area.
     */
    private final Border border=BorderFactory.createEtchedBorder();

    /**
     * Plage de valeurs pr�sentement s�lectionn�e par l'utilisateur. Cette
     * plage appara�tra comme un rectangle transparent (une <em>visi�re</em>)
     * par dessus les barres. Ce champ est initialement nul. Une visi�re ne
     * sera cr��e que lorsqu'elle sera n�cessaire.
     */
    private transient MouseReshapeTracker slider;

    /**
     * Mod�le permettant de d�crire la position de la visi�re par un entier.
     * Ce model est fournit pour faciliter les interactions avec <i>Swing</i>.
     * Ce champ peut �tre nul si aucun model n'a encore �t� demand�.
     */
    private transient SwingModel swingModel;

    /**
     * Point utilis� temporairement lors
     * des transformations affine.
     */
    private transient Point2D.Double point;

    /**
     * Objet {@link #insets} � r�utiliser autant que possible.
     */
    private transient Insets insets;

    /**
     * Construit un paneau initialement vide qui repr�sentera des
     * nombres sans unit�s. Des donn�es pourront �tre ajout�es avec
     * la m�thode {@link #addRange} pour faire appara�tre des barres.
     */
    public RangeBars()
    {this((Unit)null, SwingConstants.HORIZONTAL);}

    /**
     * Construit un paneau initialement vide qui repr�sentera des
     * nombres selon les unit�s sp�cifi�es. Des donn�es pourront
     * �tre ajout�es avec la m�thode {@link #addRange} pour faire
     * appara�tre des barres.
     *
     * @param unit Unit of measure, or <code>null</code>.
     * @param orientation Either {@link SwingConstants#HORIZONTAL}
     *                        or {@link SwingConstants#VERTICAL}.
     */
    public RangeBars(final Unit unit, final int orientation)
    {this(new NumberGraduation(unit), isHorizontal(orientation));}

    /**
     * Construit un paneau initialement vide qui repr�sentera des
     * dates dans le fuseau horaire sp�cifi�. Des donn�es pourront
     * �tre ajout�es avec la m�thode {@link #addRange} pour faire
     * appara�tre des barres.
     *
     * @param timezone The timezone.
     * @param orientation Either {@link SwingConstants#HORIZONTAL}
     *                        or {@link SwingConstants#VERTICAL}.
     */
    public RangeBars(final TimeZone timezone, final int orientation)
    {this(new DateGraduation(timezone), isHorizontal(orientation));}

    /**
     * Construit un paneau initialement vide. Des donn�es pourront
     * �tre ajout�es avec la m�thode {@link #addRange} pour faire
     * appara�tre des barres.
     */
    private RangeBars(final AbstractGraduation graduation, final boolean horizontal)
    {
        super(horizontal ? (TRANSLATE_X|SCALE_X|RESET) : (TRANSLATE_Y|SCALE_Y|RESET));
        this.horizontal = horizontal;
        axis=new Axis(graduation);
        axis.setLabelClockwise(horizontal);
        axis.setLabelFont(new Font("SansSerif", Font.BOLD,  11));
        axis.setFont     (new Font("SansSerif", Font.PLAIN, 10));
        LookAndFeel.installColors(this, "Label.background", "Label.foreground");
        setMagnifierEnabled(false);
        /*
         * Resizing vertical bars is trickier than resizing horizontal bars,
         * because vertical bars are aligned on maximal X (right aligned) while
         * horizontal bars are aligned on minimal Y (top aligned). It is easier
         * to simply clear the cache on component resize.
         */
        if (!horizontal)
        {
            addComponentListener(new ComponentAdapter()
            {
                public void componentResized(final ComponentEvent event)
                {clearCache();}
            });
        }
        setPaintingWhileAdjusting(true);
    }

    /**
     * Check the orientation.
     *
     * @param orientation Either {@link SwingConstants#HORIZONTAL}
     *        or {@link SwingConstants#VERTICAL}.
     */
    private static boolean isHorizontal(final int orientation)
    {
        switch (orientation)
        {
            case SwingConstants.HORIZONTAL: return true;
            case SwingConstants.VERTICAL:   return false;
            default: throw new IllegalArgumentException();
        }
    }

    /**
     * Set the timezone for graduation label. This affect only the way
     * labels are displayed. This method can be invoked only if this
     * <code>RangeBars</code> has been constructed with the
     * <code>RangeBars(TimeZone)</code> constructor.
     *
     * @param  timezone The new time zone.
     * @throws IllegalStateException if this <code>RangeBars</code> has has
     *         not been constructed with the <code>RangeBars(TimeZone)</code>
     *         constructor.
     */
    public void setTimeZone(final TimeZone timezone)
    {
        final Graduation graduation = axis.getGraduation();
        if (graduation instanceof DateGraduation)
        {
            final DateGraduation dateGrad = (DateGraduation) graduation;
            final TimeZone    oldTimezone = dateGrad.getTimeZone();
            dateGrad.setTimeZone(timezone);
            clearCache();
            repaint();
            firePropertyChange("timezone", oldTimezone, timezone);
        }
        else throw new IllegalStateException();
    }

    /**
     * Efface toutes les barres qui �taient trac�es.
     */
    public synchronized void clear()
    {
        ranges.clear();
        clearCache();
        repaint();
    }

    /**
     * Efface les barres correspondant
     * � l'�tiquette sp�cifi�e.
     */
    public synchronized void remove(final String label)
    {
        ranges.remove(label);
        clearCache();
        repaint();
    }

    /**
     * Ajoute une plage de valeurs. Chaque plage de valeurs est associ�e � une
     * �tiquette. Il est possible de sp�cifier (dans n'importe quel ordre)
     * plusieurs plages � une m�me �tiquette. Si deux plages se chevauchent
     * pour une �tiquette donn�e, elles seront fusionn�es ensemble.
     *
     * @param label Etiquette d�signant la barre pour laquelle on veut ajouter
     *              une plage. Si cette �tiquette avait d�j� �t� utilis�e
     *              pr�c�demment, les donn�es seront ajout�es � la barre d�j�
     *              existante. Sinon, une nouvelle barre sera cr��e. Les
     *              diff�rences entres majuscules et minuscules sont prises
     *              en compte. La valeur <code>null</code> est autoris�e.
     * @param first D�but de la plage.
     * @param last  Fin de la plage.
     *
     * @throws NullPointerException Si <code>first</code> ou <code>last</code> est nul.
     * @throws IllegalArgumentException Si <code>first</code> et <code>last</code>
     *         ne sont pas de la m�me classe, ou s'ils ne sont pas de la classe
     *         des �l�ments pr�c�demment m�moris�s sous l'�tiquette <code>label</code>.
     */
    public synchronized void addRange(final String     label,
                                      final Comparable first,
                                      final Comparable last)
    {
        RangeSet rangeSet = ranges.get(label);
        if (rangeSet==null)
        {
            rangeSet = new RangeSet(first.getClass());
            ranges.put(label, rangeSet);
        }
        rangeSet.add(first, last);
        clearCache();
        repaint();
    }

    /**
     * D�finit les plages de valeurs pour l'�tiquette sp�cifi�e.
     * Les anciennes plages de valeurs pour cette �tiquette seront
     * oubli�es.
     *
     * @param label     Etiquette pour laquelle d�finir une plage de valeur.
     * @param newRanges Nouvelle plage de valeurs.
     */
    public synchronized void setRanges(final String label, final RangeSet newRanges)
    {
        if (newRanges!=null)
        {
            ranges.put(label, newRanges);
            clearCache();
            repaint();
        }
        else remove(label);
    }

    /**
     * Update {@link #minimum} and {@link #maximum} value if it was not already
     * done.   If minimum and maximum was already up to date, then nothing will
     * be done.  This update is performed using all intervals specified to this
     * <code>RangeBars</code>.
     *
     * @return <code>true</code> if {@link #minimum} and {@link #maximum} are
     *         valid after this call,  or <code>false</code> if an update was
     *         necessary but failed for whatever reasons (for example because
     *         there is no intervals in this <code>RangeBars</code>).
     */
    private boolean update()
    {
        if (minimum<maximum) return true;
        double min=Double.POSITIVE_INFINITY;
        double max=Double.NEGATIVE_INFINITY;
        for (final Iterator<RangeSet> it=ranges.values().iterator(); it.hasNext();)
        {
            final RangeSet rangeSet = it.next();
            final int length = rangeSet.getLength();
            if (length!=0)
            {
                double tmp;
                if ((tmp=rangeSet.getDouble(0       )) < min) min=tmp;
                if ((tmp=rangeSet.getDouble(length-1)) > max) max=tmp;
            }
        }
        if (min<max)
        {
            this.minimum = min;
            this.maximum = max;
            return true;
        }
        return false;
    }

    /**
     * D�clare qu'un changement a �t� fait et que ce changement
     * peut n�cessiter le recalcul d'informations conserv�es
     * dans une cache interne.
     */
    private void clearCache()
    {
        minimum=Double.NaN;
        maximum=Double.NaN;
        labelBounds = null;
        axisBounds  = null;
        if (swingModel!=null)
        {
            swingModel.updated=false;
        }
    }

    /**
     * Sp�cifie si la l�gende de l'axe peut �tre affich�e. Appeller cette
     * m�thode avec la valeur <code>false</code> d�sactivera l'affichage de
     * la l�gende m�me si {@link #getLegend} retourne une chaine non-nulle.
     */
    public void setLegendVisible(final boolean visible)
    {axis.setLabelVisible(visible);}

    /**
     * Sp�cifie la l�gende de l'axe.
     */
    public void setLegend(final String label) // No 'synchronized' needed here
    {((AbstractGraduation) axis.getGraduation()).setAxisLabel(label);}

    /**
     * Retourne la l�gende de l'axe.
     */
    public String getLegend() // No 'synchronized' needed here
    {return axis.getGraduation().getAxisLabel();}

    /**
     * Retourne la liste des �tiquettes en m�moire, dans l'ordre dans lequel
     * elles seront �crites. Le tableau retourn� est une copie des tableaux
     * internes. En cons�quence, les changements faits sur ce tableau n'auront
     * pas de r�percussions sur <code>this</code>.
     */
    public synchronized String[] getLabels()
    {return ranges.keySet().toArray(new String[ranges.size()]);}

    /**
     * Retourne la valeur minimale m�moris�e. Si plusieurs �tiquettes ont �t�
     * sp�cifi�es, elles seront tous prises en compte. Si aucune valeur n'a �t�
     * m�moris�e dans cet objet, alors cette m�thode retourne <code>null</code>.
     */
    public synchronized Comparable getMinimum()
    {return getMinimum(getLabels());}

    /**
     * Retourne la valeur maximale m�moris�e. Si plusieurs �tiquettes ont �t�
     * sp�cifi�es, elles seront tous prises en compte. Si aucune valeur n'a �t�
     * m�moris�e dans cet objet, alors cette m�thode retourne <code>null</code>.
     */
    public synchronized Comparable getMaximum()
    {return getMaximum(getLabels());}

    /**
     * Retourne la valeur minimale m�moris�e sous l'�tiquette sp�cifi�e. Si aucune
     * donn�e n'a �t� m�moris�e sous cette �tiquette, retourne <code>null</code>.
     */
    public Comparable getMinimum(final String label)
    {return getMinimum(new String[] {label});}

    /**
     * Retourne la valeur maximale m�moris�e sous l'�tiquette sp�cifi�e. Si aucune
     * donn�e n'a �t� m�moris�e sous cette �tiquette, retourne <code>null</code>.
     */
    public Comparable getMaximum(final String label)
    {return getMaximum(new String[] {label});}

    /**
     * Retourne la valeur minimale m�moris�e sous les �tiquettes sp�cifi�es.
     * Si aucune donn�e n'a �t� m�moris�e sous ces �tiquettes, retourne
     * <code>null</code>.
     */
    public synchronized Comparable getMinimum(final String labels[])
    {
        Comparable xmin=null;
        for (int i=0; i<labels.length; i++)
        {
            final RangeSet rangeSet = ranges.get(labels[i]);
            final int length = rangeSet.getLength();
            if (length!=0)
            {
                final Comparable tmp = rangeSet.get(0);
                if (xmin==null || xmin.compareTo(tmp)>0)
                    xmin = tmp;
            }
        }
        return xmin;
    }

    /**
     * Retourne la valeur maximale m�moris�e sous les �tiquettes sp�cifi�es.
     * Si aucune donn�e n'a �t� m�moris�e sous ces �tiquettes, retourne
     * <code>null</code>.
     */
    public synchronized Comparable getMaximum(final String labels[])
    {
        Comparable xmax=null;
        for (int i=0; i<labels.length; i++)
        {
            final RangeSet rangeSet = ranges.get(labels[i]);
            final int length = rangeSet.getLength();
            if (length!=0)
            {
                final Comparable tmp = rangeSet.get(length-1);
                if (xmax==null || xmax.compareTo(tmp)<0)
                    xmax = tmp;
            }
        }
        return xmax;
    }

    /**
     * D�clare qu'on aura besoin d'une visi�re. Cette m�thode V�rifie que
     * <code>slider</code> est non-nul. S'il �tait nul, une nouvelle visi�re
     * sera cr��e et positionn�e. Si on n'avait pas assez d'informations pour
     * positionner la visi�re, sa cr�ation sera annul�e.
     */
    private void ensureSliderCreated()
    {
        if (slider!=null) return;
        slider = new MouseReshapeTracker()
        {
            protected void stateChanged(final boolean isAdjusting)
            {if (swingModel!=null) swingModel.fireStateChanged(isAdjusting);}

            protected void clipChangeRequested(double xmin, double xmax, double ymin, double ymax)
            {setVisibleRange(xmin, xmax, ymin, ymax);}
        };
        addMouseListener(slider);
        addMouseMotionListener(slider);
        /*
         * Si un mod�le existait, on l'utilisera pour
         * d�finir la position initiale de la visi�re.
         * Sinon, on construira un nouveau mod�le.
         */
        if (swingModel==null)
        {
            if (update())
            {
                final double min=this.minimum;
                final double max=this.maximum;
                if (horizontal)
                {
                    slider.setX(min, min+0.25*(max-min));
                }
                else
                {
                    slider.setY(min, min+0.25*(max-min));
                }
            }
        }
        else swingModel.synchronize();
    }

    /**
     * Retourne la valeur au centre de la
     * plage s�lectionn�e par l'utilisateur.
     */
    public double getSelectedValue()
    {
        if (slider==null) return Double.NaN;
        return horizontal ? slider.getCenterX() : slider.getCenterY();
    }

    /**
     * Retourne la valeur au d�but de la
     * plage s�lectionn�e par l'utilisateur.
     */
    public double getMinSelectedValue()
    {
        if (slider==null) return Double.NaN;
        return horizontal ? slider.getMinX() : slider.getMinY();
    }

    /**
     * Retourne la valeur � la fin de la
     * plage s�lectionn�e par l'utilisateur.
     */
    public double getMaxSelectedValue()
    {
        if (slider==null) return Double.NaN;
        return horizontal ? slider.getMaxX() : slider.getMaxY();
    }

    /**
     * Sp�cifie la plage de valeurs � s�lectionner.
     * Cette plage de valeurs appara�tra comme un
     * rectangle transparent superpos� aux barres.
     */
    public void setSelectedRange(final double min, final double max)
    {
        ensureSliderCreated();
        repaint(slider.getBounds());
        if (horizontal)
        {
            slider.setX(min, max);
        }
        else
        {
            slider.setY(min, max);
        }
        /*
         * D�clare que la position de la visi�re � chang�e.
         * Les barres seront redessin�es et le model sera
         * pr�venu du changement
         */
        repaint(slider.getBounds());
        if (swingModel!=null)
        {
            swingModel.fireStateChanged(false);
        }
    }

    /**
     * Modifie le zoom du graphique de fa�on � faire appara�tre la
     * plage de valeurs sp�cifi�e. Si l'intervale sp�cifi� n'est pas
     * enti�rement compris dans la plage des valeurs en m�moire, cette
     * m�thode d�calera et/ou zoomera l'intervale sp�cifi� de fa�on �
     * l'inclure dans la plage des valeurs en m�moire.
     *
     * @param xmin Valeur minimale de <var>x</var>.
     * @param xmax Valeur maximale de <var>x</var>.
     */
    public void setVisibleRange(final double xmin, final double xmax)
    {
        if (horizontal)
        {
            setVisibleRange(xmin, xmax, Double.NaN, Double.NaN);
        }
        else
        {
            setVisibleRange(Double.NaN, Double.NaN, xmin, xmax);
        }
    }

    /**
     * Modifie le zoom du graphique de fa�on � faire appara�tre la
     * plage de valeurs sp�cifi�e. Si l'intervale sp�cifi� n'est pas
     * enti�rement compris dans la plage des valeurs en m�moire, cette
     * m�thode d�calera et/ou zoomera l'intervale sp�cifi� de fa�on �
     * l'inclure dans la plage des valeurs en m�moire.
     */
    private void setVisibleRange(double xmin, double xmax, double ymin, double ymax)
    {
        if (update())
        {
            final double minimim = this.minimum;
            final double maximum = this.maximum;
            final Insets insets  = this.insets = getInsets(this.insets);
            final int    top     = insets.top;
            final int    left    = insets.left;
            final int    bottom  = insets.bottom;
            final int    right   = insets.right;
            if (horizontal)
            {
                /*
                 * Note: "xmax -= (xmin-minimum)" is an abreviation of
                 *       "xmax  = (xmax-xmin) + minimum".  Setting new
                 *       values for "xmin" and "xmax" is an intentional
                 *       side effect of "if" clause, to be run only if
                 *       the first "if" term is true.
                 */
                if (xmin<minimum && maximum<(xmax -= xmin-(xmin=minimum))) xmax=maximum;
                if (xmax>maximum && minimum>(xmin -= xmax-(xmax=maximum))) xmin=minimum;
                if (xmin<xmax)
                {
                    setVisibleArea(new Rectangle2D.Double(xmin, top, xmax-xmin, Math.max(bottom-top, barThickness)));
                    if (slider!=null)
                    {
                        final int height = Math.max(barThickness, (labelBounds!=null) ?
                                                                   labelBounds.height :
                                                                   getHeight());
                        slider.setClipMinMax(xmin, xmax, top, top+height);
                    }
                }
            }
            else
            {
                if (ymin<minimum && maximum<(ymax -= ymin-(ymin=minimum))) ymax=maximum;
                if (ymax>maximum && minimum>(ymin -= ymax-(ymax=maximum))) ymin=minimum;
                if (ymin<ymax)
                {
                    setVisibleArea(new Rectangle2D.Double(left, ymin, Math.max(right-left, barThickness), ymax-ymin));
                    if (slider!=null)
                    {
                        final int width = Math.max(barThickness, (labelBounds!=null) ?
                                                                  labelBounds.width  :
                                                                  getWidth());
                        slider.setClipMinMax(left, left+width, ymin, ymax);
                    }
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if user is allowed to edit
     * or drag the slider's dimension. If <code>false</code>,
     * then the user can change the slider's location but not
     * its dimension.
     */
    public boolean isRangeAdjustable()
    {
        if (slider==null)
        {
            return false;
        }
        if (horizontal)
        {
            return slider.isAdjustable(SwingConstants.EAST) ||
                   slider.isAdjustable(SwingConstants.WEST);
        }
        else
        {
            return slider.isAdjustable(SwingConstants.NORTH) ||
                   slider.isAdjustable(SwingConstants.SOUTH);
        }
    }

    /**
     * Specify if the user is allowed to edit or drag the slider's dimension.
     * If <code>true</code>, then the user is allowed to change both slider's
     * dimension and location. If <code>false</code>, then the user is allowed
     * to change slider's location only.
     */
    public void setRangeAdjustable(final boolean b)
    {
        ensureSliderCreated();
        if (horizontal)
        {
            slider.setAdjustable(SwingConstants.EAST, b);
            slider.setAdjustable(SwingConstants.WEST, b);
        }
        else
        {
            slider.setAdjustable(SwingConstants.NORTH, b);
            slider.setAdjustable(SwingConstants.SOUTH, b);
        }
    }

    /**
     * Set the font for labels and graduations. This font is applied "as is"
     * to labels. However, graduations will use a slightly smaller and plain
     * font, even if the specified font was in bold or italic.
     */
    public void setFont(final Font font)
    {
        super.setFont(font);
        axis.setLabelFont(font);
        final int size = font.getSize();
        axis.setFont(font.deriveFont(Font.PLAIN, size-(size>=14 ? 2 : 1)));
        clearCache();
    }

    /**
     * Retourne le nombre de pixels � laisser entre la r�gion dans laquelle les
     * barres sont dessin�es et les bords de cette composante. <strong>Notez que
     * les marges retourn�es par <code>getInsets(Insets)</code> peuvent etre plus
     * grandes que celles qui ont �t� sp�cifi�es � {@link #setInsets}.</strong>
     * Un espace supl�mentaire peut avoir ajout� pour tenir compte d'une
     * �ventuelle bordure qui aurait �t� ajout�e � la composante.
     *
     * @param  insets Objet � r�utiliser si possible, ou <code>null</code>.
     * @return Les marges � laisser de chaque c�t� de la zone de tra�age.
     */
    public Insets getInsets(Insets insets)
    {
        insets = super.getInsets(insets);
        insets.top    += top;
        insets.left   += left;
        insets.bottom += bottom;
        insets.right  += right;
        return insets;
    }

    /**
     * D�fini le nombre de pixels � laisser entre la r�gion dans laquelle les
     * barres sont dessin�es et les bords de cette composante. Ce nombre de
     * pixels doit �tre suffisament grand pour laisser de la place pour les
     * �tiquettes de l'axe. Notez que {@link #getInsets} ne va pas
     * obligatoirement retourner exactement ces marges.
     */
    public void setInsets(final Insets insets)
    {
        top    = (short) insets.top;
        left   = (short) insets.left;
        bottom = (short) insets.bottom;
        right  = (short) insets.right;
        repaint();
    }

    /**
     * Returns the bounding box (in pixel coordinates) of the zoomable area.
     * This implementation returns bounding box covering only a sub-area of
     * this widget area, because space is needed for axis and labels. An extra
     * margin of {@link #getInsets} is also reserved.
     *
     * @param  bounds An optional pre-allocated rectangle, or <code>null</code>
     *                to create a new one. This argument is useful if the caller
     *                wants to avoid allocating a new object on the heap.
     * @return The bounding box of the zoomable area, in pixel coordinates
     *         relative to this <code>RangeBars</code> widget.
     */
    protected Rectangle getZoomableBounds(Rectangle bounds)
    {
        bounds = super.getZoomableBounds(bounds);
        /*
         * 'labelBounds' is the rectangle (in pixels) where legends are going
         * to be displayed.   If this rectangle has not been computed yet, it
         * can be computed now with 'paintComponent(null)'.
         */
        if (labelBounds==null)
        {
            if (!valid)
            {
                reset(bounds);
            }
            paintComponent(null, bounds.width  + (left+right),
                                 bounds.height + (top+bottom));
            if (labelBounds==null)
            {
                return bounds;
            }
        }
        if (horizontal)
        {
            bounds.x     += labelBounds.width;
            bounds.width -= labelBounds.width;
            bounds.height = labelBounds.height;
            // No changes to bounds.y: align on top.
        }
        else
        {
            bounds.y       += labelBounds.height;
            bounds.height  -= labelBounds.height;
            bounds.x       += bounds.width - labelBounds.width; // Align right.
            bounds.width    = labelBounds.width;
        }
        return bounds;
    }

    /**
     * Returns the default size for this component.  This is the size
     * returned by {@link #getPreferredSize} if no preferred size has
     * been explicitly set with {@link #setPreferredSize}.
     *
     * @return The default size for this component.
     */
    protected Dimension getDefaultSize()
    {
        final Insets insets = this.insets = getInsets(this.insets);
        final int top    = insets.top;
        final int left   = insets.left;
        final int bottom = insets.bottom;
        final int right  = insets.right;
        final Dimension size=super.getDefaultSize();
        if (labelBounds==null || axisBounds==null)
        {
            if (!valid)
            {
                /*
                 * Force immediate computation of an approximative affine
                 * transform (for the zoom). A more precise affine transform
                 * may be computed later.
                 */
                reset(new Rectangle(left, top,
                                    size.width  - (left+right),
                                    size.height - (top+bottom)));
            }
            paintComponent(null, size.width, size.height);
            if (labelBounds==null || axisBounds==null)
            {
                size.width  = 280;
                size.height =  60;
                return size;
            }
        }
        if (horizontal)
        {
            // height = [bottom of axis] - [top of labels] + [margin].
            size.height = (axisBounds.y + axisBounds.height) - labelBounds.y + (bottom + top);
        }
        else
        {
            // width = [right of labels] - [left of axis] + [margin].
            size.width = (labelBounds.x + labelBounds.width) - axisBounds.x + (right + left);
        }
        return size;
    }

    /**
     * Invoked when this component must be drawn but no data are available
     * yet. Default implementation paint the text "No data" in the middle
     * of the component.
     *
     * @param graphics The paint context to draw to.
     */
    protected void paintNodata(final Graphics2D graphics)
    {
        graphics.setColor(getForeground());
        final Resources  resources = Resources.getResources(getLocale());
        final String       message = resources.getString(ResourceKeys.NO_DATA_TO_DISPLAY);
        final FontRenderContext fc = graphics.getFontRenderContext();
        final GlyphVector   glyphs = getFont().createGlyphVector(fc, message);
        final Rectangle2D   bounds = glyphs.getVisualBounds();
        graphics.drawGlyphVector(glyphs, (float) (0.5*(getWidth()-bounds.getWidth())),
                                         (float) (0.5*(getHeight()+bounds.getHeight())));
    }

    /**
     * Draw the bars, labels and their graduation. Bars and labels are drawn in
     * the same order as they were specified to {@link #addRange}.
     *
     * @param graphics The paint context to draw to.
     */
    protected void paintComponent(final Graphics2D graphics)
    {
        paintComponent(graphics, getWidth(), getHeight());
    }

    /**
     * Implementation of {@link #paintComponent(Graphics2D)}.
     * This special implementation is invoked by {@link #getZoomableBounds})
     * and {@link #getDefaultSize}. It is not too much a problem if this method
     * is not in synchronization with {@link #paintComponent(Graphics2D)} (for
     * example because the user overrided it). The user can fix the problem by
     * overriding {@link #getZoomableBounds}) and {@link #getDefaultSize} too.
     *
     * @param graphics The paint context to draw to.
     * @param componentWidth Width of this component. This information is usually
     *        given by {@link #getWidth}, except when this method is invoked from
     *        a method computing this component's dimension!
     * @param componentHeight Height of this component. This information is usually
     *        given by {@link #getHeight}, except when this method is invoked from
     *        a method computing this component's dimension!
     */
    private void paintComponent(final Graphics2D graphics,
                                final int componentWidth,
                                final int componentHeight)
    {
        final int rangeCount = ranges.size();
        if (rangeCount==0 || !update())
        {
            if (graphics!=null)
            {
                paintNodata(graphics);
            }
            return;
        }
        final Insets insets  =  this.insets = getInsets(this.insets);
        final int                       top = insets.top;
        final int                      left = insets.left;
        final int                    bottom = insets.bottom;
        final int                     right = insets.right;
        final AbstractGraduation graduation = (AbstractGraduation) axis.getGraduation();
        final GlyphVector[]          glyphs = new GlyphVector[rangeCount];
        final double[]          labelAscent = new double     [rangeCount];
        final Font                     font = getFont();
        final Shape                    clip;
        final FontRenderContext          fc;
        if (graphics==null)
        {
            clip = null;
            fc   = new FontRenderContext(null, false, false);
            /*
             * Do not invoke reset() here because this block has probably
             * been executed in order to compute this component's size,
             * i.e. this method has probably been invoked by reset() itself!
             */
        }
        else
        {
            if (!valid) reset();
            clip = graphics.getClip();
            fc   = graphics.getFontRenderContext();
        }
        /*
         * Setup an array of "glyph vectors" for labels. Gylph vectors will be
         * drawn later.   Before drawing, we query all glyph vectors for their
         * size, then we compute a typical "slot" size that will be applied to
         * every label.
         */
        double labelSlotWidth;
        double labelSlotHeight;
        if (clip==null || labelBounds==null || clip.intersects(labelBounds))
        {
            final Font labelFont;
            if (horizontal)
            {
                labelFont       = font;
                labelSlotWidth  = 0;
                labelSlotHeight = barThickness;
            }
            else
            {
                // Rotate font 90�
                labelFont = font.deriveFont(new AffineTransform(0, 1, -1, 0, 0, 0));
                labelSlotWidth  = barThickness;
                labelSlotHeight = 0;
            }
            final Iterator<String> it=ranges.keySet().iterator();
            for (int i=0; i<rangeCount; i++)
            {
                final String label=it.next();
                if (label!=null)
                {
                    glyphs[i] = labelFont.createGlyphVector(fc, label);
                    final Rectangle2D rect = glyphs[i].getVisualBounds();
                    final double    height = rect.getHeight();
                    final double     width = rect.getWidth();
                    if (width >labelSlotWidth ) labelSlotWidth =width;
                    if (height>labelSlotHeight) labelSlotHeight=height;
                    labelAscent[i] = horizontal ? height : width;
                }
            }
            if (it.hasNext())
            {
                // Should not happen
                throw new ConcurrentModificationException();
            }
            if (labelBounds==null)
            {
                labelBounds=new Rectangle();
            }
            if (horizontal)
            {
                labelSlotWidth  += barOffset;
                labelSlotHeight += lineSpacing;
                labelBounds.setBounds(left, top,
                                      (int)Math.ceil(labelSlotWidth),
                                      (int)Math.ceil(labelSlotHeight*rangeCount));
            }
            else
            {
                labelSlotHeight += barOffset;
                labelSlotWidth  += lineSpacing;
                labelBounds.setBounds(componentWidth-right, top,
                                      (int)Math.ceil(labelSlotWidth*rangeCount),
                                      (int)Math.ceil(labelSlotHeight));
                labelBounds.width += VERTICAL_ADJUSTMENT; // Empirical adjustement
                labelBounds.x -= labelBounds.width;
            }
        }
        labelSlotWidth  = labelBounds.getWidth();
        labelSlotHeight = labelBounds.getHeight();
        if (horizontal)
        {
            labelSlotHeight /= rangeCount;
        }
        else
        {
            labelSlotWidth = (labelSlotWidth-VERTICAL_ADJUSTMENT) / rangeCount;
        }
        /*
         * Now, we know the space needed for all labels.  It is time to compute
         * the axis position. This axis will be below horizontal bars or at the
         * right of vertical bars.   We also calibrate the axis for its minimum
         * and maximum values, which are zoom dependent.
         */
        try
        {
            Point2D.Double point = this.point;
            if (point==null)
            {
                this.point = point = new Point2D.Double();
            }
            if (horizontal)
            {
                double y  = labelBounds.getMaxY();
                double x1 = labelBounds.getMaxX();
                double x2 = componentWidth - right;
                /*
                 * Compute the minimal logical value,
                 * which is at the left of the axis.
                 */
                point.setLocation(x1, y);
                zoom.inverseTransform(point, point);
                graduation.setMinimum(point.x);
                if (point.x<minimum)
                {
                    graduation.setMinimum(point.x=minimum);
                    zoom.transform(point, point);
                    x1=point.x;
                }
                /*
                 * Compute the maximal logical value,
                 * which is at the right of the axis.
                 */
                point.setLocation(x2, y);
                zoom.inverseTransform(point, point);
                graduation.setMaximum(point.x);
                if (point.x>maximum)
                {
                    graduation.setMaximum(point.x=maximum);
                    zoom.transform(point, point);
                    x2=point.x;
                }
                axis.setLine(x1, y, x2, y);
            }
            else
            {
                double x  = labelBounds.getMinX();
                double y1 = componentHeight - bottom;
                double y2 = labelBounds.getMaxY();
                /*
                 * Compute the minimal logical value,
                 * which is at the bottom of the axis.
                 */
                point.setLocation(x, y1);
                zoom.inverseTransform(point, point);
                graduation.setMinimum(point.y);
                if (point.y<minimum)
                {
                    graduation.setMinimum(point.y=minimum);
                    zoom.transform(point, point);
                    y1=point.y;
                }
                /*
                 * Compute the maximal logical value,
                 * which is at the top of the axis.
                 */
                point.setLocation(x, y2);
                zoom.inverseTransform(point, point);
                graduation.setMaximum(point.y);
                if (point.y>maximum)
                {
                    graduation.setMaximum(point.y=maximum);
                    zoom.transform(point, point);
                    y2=point.y;
                }
                axis.setLine(x, y1, x, y2);
            }
        }
        catch (NoninvertibleTransformException exception)
        {
            // Should not happen
            ExceptionMonitor.paintStackTrace(graphics, getBounds(), exception);
            return;
        }
        /*
         * Prepare the painting. Paint the border first,
         * then paint all labels. Paint bars next, and
         * paint axis last.
         */
        if (graphics!=null)
        {
            final Color         foreground = getForeground();
            final double       clipMinimum = graduation.getMinimum();
            final double       clipMaximum = graduation.getMaximum();
            final Rectangle zoomableBounds = getZoomableBounds(null);
            if (border!=null)
            {
                final Insets borderInsets=border.getBorderInsets(this);
                border.paintBorder(this, graphics, zoomableBounds.x-borderInsets.left,
                                                   zoomableBounds.y-borderInsets.top,
                                                   zoomableBounds.width+(borderInsets.left+borderInsets.right),
                                                   zoomableBounds.height+(borderInsets.top+borderInsets.bottom));
            }
            graphics.setColor(foreground);
            for (int i=0; i<rangeCount; i++)
            {
                if (glyphs[i]!=null)
                {
                    final float x,y;
                    if (horizontal)
                    {
                        x = labelBounds.x;
                        y = (float) (labelBounds.y + i*labelSlotHeight +
                                     0.5*(labelSlotHeight+labelAscent[i]));
                    }
                    else
                    {
                        y = labelBounds.y;
                        x = (float) (labelBounds.x + i*labelSlotWidth +
                                     0.5*labelAscent[i]);
                    }
                    graphics.drawGlyphVector(glyphs[i], x, y);
                }
            }
            graphics.setColor(backgbColor);
            graphics.fill    (zoomableBounds);
            graphics.clip    (zoomableBounds);
            graphics.setColor(barColor);
            final Iterator<RangeSet> it=ranges.values().iterator();
            final Rectangle2D.Double bar = new Rectangle2D.Double();
            final double scale, translate;
            if (horizontal)
            {
                scale      = zoom.getScaleX();
                translate  = zoom.getTranslateX();
                bar.y      = labelBounds.y + 0.5*(labelSlotHeight-barThickness);
                bar.height = barThickness;
            }
            else
            {
                scale     = zoom.getScaleY();
                translate = zoom.getTranslateY();
                bar.x     = labelBounds.x + 0.5*barThickness;
                bar.width = barThickness;
            }
            for (int i=0; i<rangeCount; i++)
            {
                final RangeSet rangeSet = it.next();
                final int        length = rangeSet.getLength();
                for (int j=0; j<length;)
                {
                    final double bar_min = rangeSet.getDouble(j++);
                    final double bar_max = rangeSet.getDouble(j++);
                    if (bar_min > clipMaximum) break; // Slight optimization
                    if (bar_max > clipMinimum)
                    {
                        if (horizontal)
                        {
                            bar.x      = bar_min;
                            bar.width  = bar_max-bar_min;
                            bar.width *= scale;
                            bar.x     *= scale;
                            bar.x     += translate;
                        }
                        else
                        {
                            bar.y       = bar_max;
                            bar.height  = bar_min-bar_max;
                            bar.height *= scale;
                            bar.y      *= scale;
                            bar.y      += translate;
                        }
                        graphics.fill(bar);
                    }
                }
                if (horizontal)
                {
                    bar.y += labelSlotHeight;
                }
                else
                {
                    bar.x += labelSlotWidth;
                }
            }
            if (it.hasNext())
            {
                // Should not happen
                throw new ConcurrentModificationException();
            }
            graphics.setClip(clip);
            graphics.setColor(foreground);
            axis.paint(graphics);
            /*
             * The component is now fully painted. If a slider is visible, paint
             * the slider on top of everything else. The slider must always been
             * painted, no matter what 'MouseReshapeTracker.isEmpty()' said.
             */
            if (slider!=null)
            {
                if (swingModel!=null)
                {
                    swingModel.synchronize();
                }
                if (horizontal)
                {
                    final double ymin = labelBounds.getMinY();
                    final double ymax = labelBounds.getMaxY();
                    slider.setClipMinMax(clipMinimum, clipMaximum, ymin, ymax);
                    slider.setY         (                          ymin, ymax);
                }
                else
                {
                    final double xmin = labelBounds.getMinX();
                    final double xmax = labelBounds.getMaxX();
                    slider.setClipMinMax(xmin, xmax, clipMinimum, clipMaximum);
                    slider.setX         (xmin, xmax);
                }
                graphics.clip(zoomableBounds);
                graphics.transform(zoom);
                graphics.setColor(selColor);
                graphics.fill(slider);
            }
        }
        /*
         * Recompute axis bounds again. It has already been computed sooner,
         * but bounds may be more precise after painting.  Next, we slightly
         * increase its size to avoid unpainted zones after {@link #repaint}
         * calls.
         */
        axisBounds=axis.getBounds();
        axisBounds.height++;
    }

    /**
     * Apply a transform on the {@linkplain #zoom zoom}. This method override
     * {@link ZoomPane#transform(AffineTransform)} in order to make sure that
     * the supplied transform will not get the bars out of the component.  If
     * the transform would push all bars out, then it will not be applied.
     *
     * @param  change The change to apply, in logical coordinates.
     * @throws UnsupportedOperationException if the transform <code>change</code>
     *         contains an unsupported transformation, for example a vertical
     *         translation while this component is drawing only horizontal bars.
     */
    public void transform(final AffineTransform change) throws UnsupportedOperationException
    {
        /*
         * First, make sure that the transformation is a supported one.
         * Shear and rotation are not allowed. Scale is allowed only
         * along the main axis direction.
         */
        if (change.getShearX()==0 && change.getShearY()==0)
        {
            if (horizontal ? (change.getScaleY()==1 && change.getTranslateY()==0) :
                             (change.getScaleX()==1 && change.getTranslateX()==0))
            {
                /*
                 * Check if applying the transform would push all bars out
                 * of the component. If so, then exit without applying the
                 * transform.
                 */
                final Rectangle labelBounds = this.labelBounds;
                final Rectangle  axisBounds = this. axisBounds;
                if (update() && labelBounds!=null && axisBounds!=null)
                {
                    Point2D.Double point = this.point;
                    if (point==null)
                    {
                        this.point = point = new Point2D.Double();
                    }
                    if (horizontal)
                    {
                        final int xLeft   = labelBounds.x + labelBounds.width;
                        final int xRight  = Math.max(getWidth()-right, xLeft);
                        final int margin = (xRight-xLeft)/4;
                        final double x1, x2, y=labelBounds.getCenterY();
                        point.x=minimum; point.y=y; change.transform(point,point); zoom.transform(point,point); x1=point.x;
                        point.x=maximum; point.y=y; change.transform(point,point); zoom.transform(point,point); x2=point.x;
                        if (Math.min(x1,x2)>(xRight-margin) || Math.max(x1,x2)<(xLeft+margin) || Math.abs(x2-x1)<margin)
                        {
                            return;
                        }
                    }
                    else
                    {
                        final int yTop    = labelBounds.y + labelBounds.height;
                        final int yBottom = Math.max(getHeight()-bottom, yTop);
                        final int margin  = (yBottom-yTop)/4;
                        final double y1, y2, x=labelBounds.getCenterX();
                        point.y=minimum; point.x=x; change.transform(point,point); zoom.transform(point,point); y1=point.y;
                        point.y=maximum; point.x=x; change.transform(point,point); zoom.transform(point,point); y2=point.y;
                        if (Math.min(y1,y2)>(yBottom-margin) || Math.max(y1,y2)<(yTop+margin) || Math.abs(y2-y1)<margin)
                        {
                            return;
                        }
                    }
                }
                /*
                 * Applique la transformation, met � jour la transformation
                 * de la visi�re et redessine l'axe en plus du graphique.
                 */
                super.transform(change);
                if (slider!=null) slider.setTransform(zoom);
                if (axisBounds!=null) repaint(axisBounds);
                return;
            }
        }
        throw new UnsupportedOperationException("Unexpected transform");
    }

    /**
     * R�initialise le zoom de fa�on � ce que tous
     * les intervalles apparaissent dans la fen�tre.
     */
    public void reset()
    {
        reset(getZoomableBounds(null));
        if (getWidth()>0 && getHeight()>0) valid=true;
    }

    /**
     * Reset the zoom in such a way that
     * every bars fit in the display area.
     */
    private void reset(final Rectangle zoomableBounds)
    {
        reset(zoomableBounds, !horizontal);
        if (slider!=null)
        {
            slider.setTransform(zoom);
        }
        if (axisBounds!=null)
        {
            repaint(axisBounds);
        }
    }

    /**
     * Returns logical coordinates for the display area.
     */
    public Rectangle2D getArea()
    {
        final Insets insets = this.insets = getInsets(this.insets);
        final int top    = insets.top;
        final int left   = insets.left;
        final int bottom = insets.bottom;
        final int right  = insets.right;
        if (update())
        {
            final double min=this.minimum;
            final double max=this.maximum;
            if (horizontal)
            {
                int height = getHeight();
                if (height==0)
                {
                    height = getMinimumSize().height;
                    // Height doesn't need to be exact,
                    // since it will be ignored anyway...
                }
                return new Rectangle2D.Double(min, top, max-min,
                                              Math.max(height-(top+bottom),16));
            }
            else
            {
                int width = getWidth();
                if (width==0)
                {
                    width = getMinimumSize().width;
                    // Width doesn't need to be exact,
                    // since it will be ignored anyway...
                }
                return new Rectangle2D.Double(left, min,
                                              Math.max(width-(left+right),16),
                                              max-min);
            }
        }
        /*
         * This block will be run only if logical coordinate of display area
         * can't be computed, because of not having enough informations.  It
         * make a simple guess, which is better than nothing.
         */
        final Rectangle bounds = getBounds();
        bounds.x       = left;
        bounds.y       = top;
        bounds.width  -= (left+right);
        bounds.height -= (top+bottom);
        return bounds;
    }

    /**
     * Retourne un model pouvant d�crire la position de la visi�re dans une
     * plage d'entiers. Ce model est fournit pour faciliter les interactions
     * avec <i>Swing</i>. Ses principales m�thodes sont d�finies comme suit:
     *
     * <p>{@link BoundedRangeModel#getValue}<br>
     *    Retourne la position du bord gauche de la visi�re, exprim�e par
     *    un entier compris entre le minimum et le maximum du model (0 et
     *    100 par d�faut).</p>
     *
     * <p>{@link BoundedRangeModel#getExtent}<br>
     *    Retourne la largeur de la visi�re, exprim�e selon les m�mes unit�s
     *    que <code>getValue()</code>.</p>
     *
     * <p>{@link BoundedRangeModel#setMinimum} / {@link BoundedRangeModel#setMaximum}<br>
     *    Modifie les valeurs enti�re minimale ou maximale retourn�es par <code>getValue()</code>.
     *    Cette modification n'affecte aucunement l'axe des barres affich�es; elle
     *    ne fait que modifier la fa�on dont la position de la visi�re est convertie
     *    en valeur enti�re par <code>getValue()</code>.</p>
     *
     * <p>{@link BoundedRangeModel#setValue} / {@link BoundedRangeModel#setExtent}<br>
     *    Modifie la position du bord gauche de la visi�re ou sa largeur.</p>
     */
    public synchronized LogicalBoundedRangeModel getModel()
    {
        if (swingModel==null)
        {
            ensureSliderCreated();
            swingModel=new SwingModel();
        }
        return swingModel;
    }

    /**
     * Mod�le assurant une interop�rabilit� de {@link RangeBars} avec <i>Swing</i>.
     * La m�thode <code>fireStateChanged(boolean)</code> sera appell�e automatiquement
     * chaque fois que l'utilisateur fait glisser la visi�re.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class SwingModel extends DefaultBoundedRangeModel implements LogicalBoundedRangeModel
    {
        /**
         * D�calage intervenant dans la conversion de la position
         * de la visi�re en valeur enti�re. Le calcul se fait par
         * <code>int_x=(x-offset)*scale</code>.
         */
        private double offset;

        /**
         * Facteur d'�chelle intervenant dans la conversion de la position de la visi�re
         * en valeur enti�re. Le calcul se fait par <code>int_x=x*scale+offset</code>.
         */
        private double scale;

        /**
         * Indique si les coefficients <code>offset</code> et <code>scale</code> sont � jour.
         * Ce champs sera mis � <code>false</code> chaque fois que des plages sont ajout�es
         * ou supprim�es dans l'objet {@link RangeBars}.
         */
        boolean updated;

        /**
         * Indique d'o� vient le dernier ajustement
         * de la valeur: du model ou de la visi�re.
         */
        private boolean lastAdjustFromModel;

        /**
         * La valeur <code>true</code> indique que {@link #fireStateChanged}
         * ne doit pas prendre en compte le prochain �v�nement. Ce champ est
         * utilis� lors des changements de la position de la visi�re.
         */
        private transient boolean ignoreEvent;

        /**
         * Valeur minimale. La valeur <code>NaN</code> indique qu'il
         * faut puiser le minimum dans les donn�es de {@link RangeBars}.
         */
        private double minimum=Double.NaN;

        /**
         * Valeur maximale. La valeur <code>NaN</code> indique qu'il
         * faut puiser le maximum dans les donn�es de {@link RangeBars}.
         */
        private double maximum=Double.NaN;

        /**
         * Construit un model avec par d�faut une plage allant de 0 � 100. Les valeurs
         * de cette plage sont toujours ind�pendantes de celles de {@link RangeBars}.
         */
        public SwingModel()
        {}

        /**
         * Sp�cifie les minimum et maximum des valeurs enti�res.
         * Une valeur {@link Double#NaN} signifie de prendre une
         * valeur par d�faut.
         */
        public void setLogicalRange(final double minimum, final double maximum)
        {
            this.minimum = minimum;
            this.maximum = maximum;
            updated = false;
        }

        /**
         * Convertit une valeur enti�re en nombre r�el.
         */
        public double toLogical(final int integer)
        {return (integer-offset)/scale;}

        /**
         * Convertit un nombre r�el en valeur enti�re.
         */
        public int toInteger(final double logical)
        {return (int) Math.round(logical*scale + offset);}

        /**
         * Positionne de la visi�re en fonction
         * de la valeur de ce model.
         */
        public void synchronize()
        {
            ensureUpdated();
            if (lastAdjustFromModel)
            {
                setSliderPosition();
            }
            else
            {
                final int value  = (int) Math.round(slider.getMinX()*scale+offset);
                final int extent = (int) Math.round(slider.getWidth()*scale);
                if (value!=super.getValue() || extent!=super.getExtent())
                {
                    super.setRangeProperties(value, extent, super.getMinimum(), super.getMaximum(), false);
                }
            }
        }

        /**
         * Met � jour les champs {@link #offset} et {@link #scale}. Les minimum
         * maximum ainsi que la valeur actuels du model seront r�utilis�s. C'est
         * de la responsabilit� du programmeur de mettre � jour ces propri�t�s si
         * c'est n�cessaire.
         */
        private void ensureUpdated()
        {update(super.getMinimum(), super.getMaximum());}

        /**
         * Met � jour les champs {@link #offset} et {@link #scale} en fonction
         * des minimum et maximum sp�cifi�s. Il sera de la responsabilit� de
         * l'appellant d'appeller l'�quivalent de <code>setMinimum(lower)</code>
         * et <code>setMaximum(upper)</code>. Apr�s l'appel de cette m�thode, la
         * position de la visi�re pourra �tre convertie en valeur enti�re par
         * <code>int_x=x*scale+offset</code>.
         */
        private void update(final int lower, final int upper)
        {
            double minimum=this.minimum;
            if (Double.isNaN(minimum))
            {
                final Comparable min=RangeBars.this.getMinimum();
                if (min instanceof Number)
                    minimum = ((Number)min).doubleValue();
            }
            double maximum=this.maximum;
            if (Double.isNaN(maximum))
            {
                final Comparable max=RangeBars.this.getMaximum();
                if (max instanceof Number)
                    maximum = ((Number)max).doubleValue();
            }
            if (!Double.isNaN(minimum) && !Double.isNaN(maximum))
            {
                scale  = (upper-lower)/(maximum-minimum);
                offset = lower-minimum*scale;
            }
            else
            {
                scale  = 1;
                offset = 0;
            }
            updated=true;
        }

        /**
         * Met � jour les champs internes de ce model et lance un
         * �v�nement pr�venant que la position ou la largeur de la
         * visi�re a chang�e. Cette m�thode est appell�e � partir
         * de {@link RangeBars} seulement.
         */
        protected void fireStateChanged(final boolean isAdjusting)
        {
            if (!ignoreEvent)
            {
                ensureUpdated();
                lastAdjustFromModel=false;
                boolean adjustSlider=false;
                int lower  = super.getMinimum();
                int upper  = super.getMaximum();
                int value  = (int) Math.round(slider.getMinX()*scale+offset);
                int extent = (int) Math.round(slider.getWidth()*scale);
                if (value<lower)
                {
                    final int range=upper-lower;
                    if (extent>range) extent=range;
                    value=lower;
                    adjustSlider=true;
                }
                else if (value>upper-extent)
                {
                    final int range=upper-lower;
                    if (extent>range) extent=range;
                    value=upper-extent;
                    adjustSlider=true;
                }
                super.setRangeProperties(value, extent, lower, upper, isAdjusting);
                if (adjustSlider)
                {
                    setSliderPosition();
                }
            }
        }

        /**
         * Met � jour les champs internes de ce model et lance un
         * �v�nement prevenant que la position ou la largeur de la
         * visi�re a chang�e.
         */
        private void setRangeProperties(final int lower, final int upper, final boolean isAdjusting)
        {
            update(lower, upper);
            if (lastAdjustFromModel)
            {
                super.setRangeProperties(super.getValue(), super.getExtent(), lower, upper, isAdjusting);
                setSliderPosition();
            }
            else
            {
                super.setRangeProperties((int) Math.round(slider.getMinX()*scale+offset),
                                         (int) Math.round(slider.getWidth()*scale),
                                         lower, upper, isAdjusting);
            }
        }

        /**
         * Modifie la valeur minimale retourn�e par {@link #getValue}.
         * La valeur retourn�e par cette derni�re sera modifi�e pour
         * qu'elle corresponde � la position de la visi�re dans les
         * nouvelles limites.
         */
        public void setMinimum(final int minimum)
        {setRangeProperties(minimum, super.getMaximum(), false);}

        /**
         * Modifie la valeur maximale retourn�e par {@link #getValue}.
         * La valeur retourn�e par cette derni�re sera modifi�e pour
         * qu'elle corresponde � la position de la visi�re dans les
         * nouvelles limites.
         */
        public void setMaximum(final int maximum)
        {setRangeProperties(super.getMinimum(), maximum, false);}

        /**
         * Retourne la position de la visi�re.
         */
        public int getValue()
        {
            if (!updated && !lastAdjustFromModel)
            {
                ensureUpdated();
                super.setValue((int) Math.round(slider.getMinX()*scale+offset));
            }
            return super.getValue();
        }

        /**
         * Modifie la position de la visi�re.
         */
        public void setValue(final int value)
        {
            lastAdjustFromModel=true;
            super.setValue(value);
            setSliderPosition();
        }

        /**
         * Retourne l'�tendu de la visi�re.
         */
        public int getExtent()
        {
            if (!updated && !lastAdjustFromModel)
            {
                ensureUpdated();
                super.setExtent((int) Math.round(slider.getWidth()*scale));
            }
            return super.getExtent();
        }

        /**
         * Modifie la largeur de la visi�re.
         */
        public void setExtent(final int extent)
        {
            lastAdjustFromModel=true;
            super.setExtent(extent);
            setSliderPosition();
        }

        /**
         * Modifie l'ensemble des param�tres d'un coups.
         */
        public void setRangeProperties(final int value, final int extent,
                                       final int lower, final int upper,
                                       final boolean isAdjusting)
        {
            update(lower, upper);
            lastAdjustFromModel=true;
            super.setRangeProperties(value, extent, lower, upper, isAdjusting);
            setSliderPosition();
        }

        /**
         * Modifie la position de la visi�re en fonction
         * des valeurs actuelles du mod�le.
         */
        private void setSliderPosition()
        {
            final double x=(super.getValue()-offset)/scale;
            try
            {
                ignoreEvent=true;
                slider.setX(x, x+super.getExtent()/scale);
            }
            finally
            {
                ignoreEvent=false;
            }
            repaint();
        }
    }

    /**
     * Returns a control panel for this <code>RangeBars</code>. The control
     * panel may contains buttons, editors and spinners.   It make possible
     * for users to enter exact values in editor fields using the keyboard.
     * The returned control panel do not contains this <code>RangeBars</code>:
     * caller must layout both the control panel and this <code>RangeBars</code>
     * (possibly in different windows) if he want to see both of them.
     *
     * @param format  The format to use for formatting the selected value range,
     *                or <code>null</code> for a default format. If non-null,
     *                then this format is usually a
     *                {@link java.text.NumberFormat} or a
     *                {@link java.text.DateFormat} instance.
     * @param minLabel The label to put in front of the editor for
     *                 minimum value, or <code>null</code> if none.
     * @param maxLabel The label to put in front of the editor for
     *                 maximum value, or <code>null</code> if none.
     */
    private JComponent createControlPanel(Format format,
                                          final String minLabel,
                                          final String maxLabel)
    {
        ensureSliderCreated();
        if (format==null)   format = axis.getGraduation().getFormat();
        final JComponent   editor1 = slider.addEditor(format, horizontal ? SwingConstants.WEST : SwingConstants.NORTH, this);
        final JComponent   editor2 = slider.addEditor(format, horizontal ? SwingConstants.EAST : SwingConstants.SOUTH, this);
        final JComponent     panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        /*
         * If the caller supplied labels, add
         * labels first. Then add the editors.
         */
        c.gridx=0;
        if (minLabel!=null || maxLabel!=null)
        {
            c.anchor = c.EAST;
            c.gridy=0; panel.add(new JLabel(horizontal ? minLabel : maxLabel), c);
            c.gridy=1; panel.add(new JLabel(horizontal ? maxLabel : minLabel), c);
            c.gridx=1;
            c.insets.left=3;
            c.anchor = c.CENTER;
        }
        c.weightx=1; c.fill=c.HORIZONTAL;
        c.gridy=0; panel.add(editor1, c);
        c.gridy=1; panel.add(editor2, c);
        /*
         * Adjust focus order.
         * FIXME: this code use deprecated API.
         */
        editor1.setNextFocusableComponent(editor2);
        editor2.setNextFocusableComponent(this   );
        this   .setNextFocusableComponent(editor1);
        this   .requestFocus();
        panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEtchedBorder(),
                        BorderFactory.createEmptyBorder(3,3,3,3)));
        final Dimension size = panel.getPreferredSize();
        size.width = 100;
        panel.setPreferredSize(size);
        panel.setMinimumSize  (size);
        return panel;
    }

    /**
     * Returns a new panel with that contains this <code>RangeBars</code> and
     * control widgets. Control widgets may include buttons, editors, spinners
     * and scroll bar. It make possible for users to enter exact values in
     * editor fields using the keyboard.
     *
     * @param format  The format to use for formatting the selected value range,
     *                or <code>null</code> for a default format. If non-null,
     *                then this format is usually a
     *                {@link java.text.NumberFormat} or a
     *                {@link java.text.DateFormat} instance.
     * @param minLabel The label to put in front of the editor for
     *                 minimum value, or <code>null</code> if none.
     * @param maxLabel The label to put in front of the editor for
     *                 maximum value, or <code>null</code> if none.
     */
    public JComponent createCombinedPanel(final Format format,
                                          final String minLabel,
                                          final String maxLabel)
    {
        final JComponent     panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx=0; c.weightx=1;
        c.gridy=0; c.weighty=1;
        c.fill = c.BOTH;
        panel.add(horizontal ? this : createScrollPane(), c);
        if (horizontal)
        {
            c.gridx  =1;
            c.weightx=0;
            c.insets.right = 6;
        }
        else
        {
            c.gridy  =1;
            c.weighty=0;
            c.insets.top = 6;
        }
        c.fill = c.HORIZONTAL;
        panel.add(createControlPanel(format, minLabel, maxLabel), c);
        return panel;
    }

    /**
     * Fait appara�tre dans une fen�tre quelques histogrammes
     * calcul�s au hasard. Cette m�thode sert � v�rifier le
     * bon fonctionnement de la classe <code>RangeBars</code>.
     */
    public static void main(final String[] args)
    {
        int orientation = SwingConstants.HORIZONTAL;
        if (args.length!=0)
        {
            final String arg = args[0];
            if (arg.equalsIgnoreCase("horizontal"))
            {
                orientation = SwingConstants.HORIZONTAL;
            }
            else if (arg.equalsIgnoreCase("vertical"))
            {
                orientation = SwingConstants.VERTICAL;
            }
            else
            {
                System.err.print("Unknow argument: ");
                System.err.println(arg);
                return;
            }
        }
        final JFrame frame=new JFrame("RangeBars");
        final RangeBars ranges=new RangeBars((Unit)null, orientation);
        for (int s�rie=1; s�rie<=4; s�rie++)
        {
            final String cl�="S�rie #"+s�rie;
            for (int i=0; i<100; i++)
            {
                final double x = 1000*Math.random();
                final double w =   30*Math.random();
                ranges.addRange(cl�, new Double(x), new Double(x+w));
            }
        }
        ranges.setSelectedRange(12, 38);
        ranges.setRangeAdjustable(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(ranges.createCombinedPanel(null,"Min:","Max:"));
        frame.pack();
        frame.show();
    }
}
