/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 * Copyright (C) 1999 Pêches et Océans Canada
 *               2002 Institut de Recherche pour le Développement
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
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.plot;

// Axes et modèles
import net.seas.plot.axis.Axis;
import net.seas.plot.axis.Graduation;
import net.seas.plot.axis.DateGraduation;
import net.seas.plot.axis.NumberGraduation;
import net.seas.plot.axis.AbstractGraduation;

// Coordonnées et unités
import java.util.Date;
import java.util.TimeZone;
import javax.units.Unit;

// Formats
import java.text.Format;

// Interface utilisateur
import net.seas.awt.ZoomPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

// Interface utilisateur (Swing)
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.BoundedRangeModel;
import javax.swing.text.JTextComponent;
import javax.swing.DefaultBoundedRangeModel;

// Graphisme
import java.awt.Font;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;

// Géométrie
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Ensembles
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.lang.reflect.Array;
import java.util.ConcurrentModificationException;

// Divers
import net.seas.resources.Resources;
import net.seas.resources.ResourceKeys;
import net.seas.awt.ExceptionMonitor;
import net.seas.awt.MouseReshapeTracker;


/**
 * Paneau représentant les plages des données disponibles. Ces plages sont représentées par des
 * barres verticales. L'axe des <var>x</var> représente les valeurs, et sur l'axe des <var>y</var>
 * on place les différents types de données, un peu comme le ferait un histogramme.
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
     * Données des barres. Chaque entré est constitué d'une paire (<em>étiquette</em>,
     * <em>tableau de données</em>). Le tableau de donnée sera généralement (mais pas
     * obligatoirement) un tableau de type <code>long[]</code>. Les données de ces
     * tableaux seront organisées par paires de valeurs, de la forme (<i>début</i>,<i>fin</i>).
     */
    private final Map<String,RangeSet> ranges=new LinkedHashMap<String,RangeSet>();

    /**
     * Axe des <var>x</var> servant à écrire les valeurs des plages. Les méthodes de
     * {@link Axis} peuvent être appellées pour modifier le format des nombres, une
     * étiquette, des unités ou pour spécifier "à la main" les minimums et maximums.
     */
    private final Axis axis;

    /**
     * Valeur minimale à avoir été spécifiée avec {@link #addRange}.
     * Cette valeur n'est pas valide si <code>(minimum<maximum)</code>
     * est <code>false</code>. Cette valeur peut etre calculée par un
     * appel à {@link #update}.
     */
    private transient double minimum;

    /**
     * Valeur maximale à avoir été spécifiée avec {@link #addRange}.
     * Cette valeur n'est pas valide si <code>(minimum<maximum)</code>
     * est <code>false</code>. Cette valeur peut etre calculée par un
     * appel à {@link #update}.
     */
    private transient double maximum;

    /**
     * Coordonnées (en pixels) de la région dans laquelle seront dessinées les étiquettes.
     * Ce champ est nul si ces coordonnées ne sont pas encore connues. Ces coordonnées sont
     * calculées par {@link #paintComponent(Graphics2D)}  (notez que cette dernière accepte
     * un argument {@link Graphics2D} nul).
     */
    private transient Rectangle labelBounds;

    /**
     * Coordonnées (en pixels) de la région dans laquelle sera dessinée l'axe.
     * Ce champ est nul si ces coordonnées ne sont pas encore connues. Ces coordonnées sont
     * calculées par {@link #paintComponent(Graphics2D)}  (notez que cette dernière accepte
     * un argument {@link Graphics2D} nul).
     */
    private transient Rectangle axisBounds;

    /**
     * Indique si cette composante sera orientée horizontalement ou
     * verticalement. Note: l'orientation verticale n'a pas été testée.
     */
    private final boolean horizontal;

    /**
     * Indique si la méthode {@link #reset} a été appelée
     * sur cet objet avec une dimension valide de la fenêtre.
     */
    private boolean valid;

    /**
     * Espaces (en pixels) à laisser de chaque côtés
     * du graphique. Ces dimensions seront retournées
     * par {@link #getInsets}.
     */
    private short top=12, left=12, bottom=6, right=15;

    /** Hauteur (en pixels) des barres des histogrammes.         */ private final short   barHeight=8; // en pixels
    /** Espace (en pixels) entre les étiquettes et leurs barres. */ private final short   barOffset=6; // en pixels
    /** Espace (en pixels) à ajouter entre deux lignes.          */ private final short lineSpacing=3; // en pixels
    /** Couleur de la région de traçage du graphique.            */ private final Color backgbColor=Color.white;
    /** Couleur des barres.                                      */ private final Color    barColor=new Color(255, 153,  51);
    /** Couleur de la zone des dates sélectionnées.              */ private final Color    selColor=new Color(128,  64,  92, 64);
    //  La couleur des étiquettes peut être spécifiée par un appel à setForeground(Color).

    /**
     * Bordure à placer tout le tour du graphique.
     */
    private final Border border=BorderFactory.createEtchedBorder();

    /**
     * Plage de valeurs présentement sélectionnée par l'utilisateur. Cette plage
     * apparaîtra comme un rectangle transparent (une <em>visière</em>) par dessus
     * les barres. Ce champ est initialement nul. Une visière ne sera créée que
     * lorsqu'elle sera nécessaire.
     */
    private transient MouseReshapeTracker slider;

    /**
     * Modèle permettant de décrire la position de la visière par un entier.
     * Ce model est fournit pour faciliter les interactions avec <i>Swing</i>.
     * Ce champ peut être nul si aucun model n'a encore été demandé.
     */
    private transient SwingModel swingModel;

    /**
     * Point utilisé temporairement lors
     * des transformations affine.
     */
    private transient Point2D.Double point;

    /**
     * Objet {@link #insets} à réutiliser autant que possible.
     */
    private transient Insets insets;

    /**
     * Construit un paneau initialement vide qui représentera des
     * nombres sans unités. Des données pourront être ajoutées avec
     * la méthode {@link #addRange} pour faire apparaître des barres.
     */
    public RangeBars()
    {this((Unit)null);}

    /**
     * Construit un paneau initialement vide qui représentera des
     * nombres selon les unités spécifiées. Des données pourront
     * être ajoutées avec la méthode {@link #addRange} pour faire
     * apparaître des barres.
     */
    public RangeBars(final Unit unit)
    {this(new NumberGraduation(unit), true);}

    /**
     * Construit un paneau initialement vide qui représentera des
     * dates dans le fuseau horaire spécifié. Des données pourront
     * être ajoutées avec la méthode {@link #addRange} pour faire
     * apparaître des barres.
     */
    public RangeBars(final TimeZone timezone)
    {this(new DateGraduation(timezone), true);}

    /**
     * Construit un paneau initialement vide. Des données pourront
     * être ajoutées avec la méthode {@link #addRange} pour faire
     * apparaître des barres.
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
    }

    /**
     * Efface toutes les barres qui étaient tracées.
     */
    public synchronized void clear()
    {
        ranges.clear();
        clearCache();
        repaint();
    }

    /**
     * Efface les barres correspondant
     * à l'étiquette spécifiée.
     */
    public synchronized void remove(final String label)
    {
        ranges.remove(label);
        clearCache();
        repaint();
    }

    /**
     * Ajoute une plage de valeurs. Chaque plage de valeurs est associée à une étiquette.
     * Il est possible de spécifier (dans n'importe quel ordre) plusieurs plages à une même
     * étiquette. Si deux plages se chevauchent pour une étiquette donnée, elles seront
     * fusionnées ensemble.
     *
     * @param label Etiquette désignant la barre pour laquelle on veut ajouter une plage.
     *              Si cette étiquette avait déjà été utilisée précédemment, les données
     *              seront ajoutées à la barre déjà existante. Sinon, une nouvelle barre
     *              sera créée. Les différences entres majuscules et minuscules sont prises
     *              en compte. La valeur <code>null</code> est autorisée.
     * @param first Début de la plage.
     * @param last  Fin de la plage.
     *
     * @throws NullPointerException Si <code>first</code> ou <code>last</code> est nul.
     * @throws IllegalArgumentException Si <code>first</code> et <code>last</code> ne
     *         sont pas de la même classe, ou s'ils ne sont pas de la classe des éléments
     *         précédemment mémorisés sous l'étiquette <code>label</code>.
     */
    public synchronized void addRange(final String label, final Comparable first, final Comparable last)
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
     * Met à jour les champs {@link #minimum} et {@link #maximum}, si ce n'était pas déjà fait.
     * Cette mise-à-jour sera faite en fonction des intervalles en mémoire. Si la mise à jour
     * n'a pas été possible (parce exemple parce qu'aucune donnée n'a été spécifiée), alors
     * cette méthode retourne <code>false</code>.
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
     * Déclare qu'un changement a été fait et que ce changement
     * peut nécessiter le recalcul d'informations conservées
     * dans une cache interne.
     */
    private void clearCache()
    {
        minimum=Double.NaN;
        maximum=Double.NaN;
        labelBounds=null;
        axisBounds=null;
        if (swingModel!=null)
        {
            swingModel.updated=false;
        }
    }

    /**
     * Spécifie si la légende de l'axe peut affichée. Appeller cette méthode avec la valeur
     * <code>false</code> désactivera l'affichage de la légende même si {@link #getLegend}
     * retourne une chaine non-nulle.
     */
    public void setLegendVisible(final boolean visible)
    {axis.setLabelVisible(visible);}

    /**
     * Spécifie la légende de l'axe.
     */
    public void setLegend(final String label) // No 'synchronized' needed here
    {((AbstractGraduation) axis.getGraduation()).setAxisLabel(label);}

    /**
     * Retourne la légende de l'axe.
     */
    public String getLegend() // No 'synchronized' needed here
    {return axis.getGraduation().getAxisLabel();}

    /**
     * Retourne la liste des étiquettes en mémoire, dans l'ordre dans lequel elles seront
     * écrites. Le tableau retourné est une copie des tableaux internes. En conséquence,
     * les changements faits sur ce tableau n'auront pas de répercussions sur <code>this</code>.
     */
    public synchronized String[] getLabels()
    {return ranges.keySet().toArray(new String[ranges.size()]);}

    /**
     * Retourne la valeur minimale mémorisée. Si plusieurs étiquettes ont été spécifiées,
     * elles seront tous prises en compte. Si aucune valeur n'a été mémorisée dans cet objet,
     * alors cette méthode retourne <code>null</code>.
     */
    public synchronized Comparable getMinimum()
    {return getMinimum(getLabels());}

    /**
     * Retourne la valeur maximale mémorisée. Si plusieurs étiquettes ont été spécifiées,
     * elles seront tous prises en compte. Si aucune valeur n'a été mémorisée dans cet objet,
     * alors cette méthode retourne <code>null</code>.
     */
    public synchronized Comparable getMaximum()
    {return getMaximum(getLabels());}

    /**
     * Retourne la valeur minimale mémorisée sous l'étiquette spécifiée. Si aucune
     * donnée n'a été mémorisée sous cette étiquette, retourne <code>null</code>.
     */
    public Comparable getMinimum(final String label)
    {return getMinimum(new String[] {label});}

    /**
     * Retourne la valeur maximale mémorisée sous l'étiquette spécifiée. Si aucune
     * donnée n'a été mémorisée sous cette étiquette, retourne <code>null</code>.
     */
    public Comparable getMaximum(final String label)
    {return getMaximum(new String[] {label});}

    /**
     * Retourne la valeur minimale mémorisée sous les étiquettes spécifiées. Si aucune
     * donnée n'a été mémorisée sous ces étiquettes, retourne <code>null</code>.
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
     * Retourne la valeur maximale mémorisée sous les étiquettes spécifiées. Si aucune
     * donnée n'a été mémorisée sous ces étiquettes, retourne <code>null</code>.
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
     * Déclare qu'on aura besoin d'une visière. Cette méthode Vérifie que
     * <code>slider</code> est non-nul. S'il était nul, une nouvelle visière
     * sera créée et positionné. Si on n'avait pas assez d'informations pour
     * positionner la visière, sa création sera annulée.
     */
    private void ensureSliderCreated()
    {
        if (slider!=null) return;
        /*
         * Si un model existait, on l'utilisera pour
         * définir la position initiale de la visière.
         */
        slider = new MouseReshapeTracker()
        {
            protected void stateChanged(final boolean isAdjusting)
            {if (swingModel!=null) swingModel.fireStateChanged(isAdjusting);}

            protected void clipChangeRequested(double xmin, double xmax, double ymin, double ymax)
            {setVisibleRange(xmin, xmax, ymin, ymax);}
        };
        addMouseListener(slider);
        addMouseMotionListener(slider);
        if (swingModel==null)
        {
            if (update())
            {
                final double min=this.minimum;
                final double max=this.maximum;
                if (horizontal)
                    slider.setX(min, min+0.25*(max-min));
                else
                    slider.setY(min, min+0.25*(max-min));
            }
        }
        else swingModel.synchronize();
    }

    /**
     * Retourne la valeur au centre de la
     * plage sélectionnée par l'utilisateur.
     */
    public double getSelectedValue()
    {return (slider==null) ? Double.NaN : (horizontal) ? slider.getCenterX() : slider.getCenterY();}

    /**
     * Retourne la valeur au début de la
     * plage sélectionnée par l'utilisateur.
     */
    public double getMinSelectedValue()
    {return (slider==null) ? Double.NaN : (horizontal) ? slider.getMinX() : slider.getMinY();}

    /**
     * Retourne la valeur à la fin de la
     * plage sélectionnée par l'utilisateur.
     */
    public double getMaxSelectedValue()
    {return (slider==null) ? Double.NaN : (horizontal) ? slider.getMaxX() : slider.getMaxY();}

    /**
     * Spécifie la plage de valeurs à sélectionner.
     * Cette plage de valeurs apparaîtra comme un
     * rectangle transparent superposé aux barres.
     */
    public void setSelectedRange(final double min, final double max)
    {
        ensureSliderCreated();
        repaint(slider.getBounds());
        if (horizontal)
            slider.setX(min, max);
        else
            slider.setY(min, max);
        /*
         * Déclare que la position de la visière à changée.
         * Les barres seront redessinées et le model sera
         * prévenu du changement
         */
        repaint(slider.getBounds());
        if (swingModel!=null)
        {
            swingModel.fireStateChanged(false);
        }
    }

    /**
     * Modifie le zoom du graphique de façon à faire apparaître la
     * plage de valeurs spécifiée. Si l'intervale spécifié n'est pas
     * entièrement compris dans la plage des valeurs en mémoire, cette
     * méthode décalera et/ou zoomera l'intervale spécifié de façon à
     * l'inclure dans la plage des valeurs en mémoire.
     *
     * @param xmin Valeur minimale de <var>x</var>.
     * @param xmax Valeur maximale de <var>x</var>.
     */
    public void setVisibleRange(final double xmin, final double xmax)
    {
        if (horizontal)
            setVisibleRange(xmin, xmax, Double.NaN, Double.NaN);
        else
            setVisibleRange(Double.NaN, Double.NaN, xmin, xmax);
    }

    /**
     * Modifie le zoom du graphique de façon à faire apparaître la
     * plage de valeurs spécifiée. Si l'intervale spécifié n'est pas
     * entièrement compris dans la plage des valeurs en mémoire, cette
     * méthode décalera et/ou zoomera l'intervale spécifié de façon à
     * l'inclure dans la plage des valeurs en mémoire.
     */
    private void setVisibleRange(double xmin, double xmax, double ymin, double ymax)
    {
        if (update())
        {
            final double minimim=this.minimum;
            final double maximum=this.maximum;
            /*
             * Dans les lignes suivantes, on a réduit
             * "xmax=(xmax-xmin)+minimum" par "xmax -= xmin-minimum".
             */
            final Insets insets=this.insets=getInsets(this.insets);
            final int top    = insets.top;
            final int left   = insets.left;
            final int bottom = insets.bottom;
            final int right  = insets.right;
            if (horizontal)
            {
                if (xmin<minimum && maximum<(xmax -= xmin-(xmin=minimum))) xmax=maximum;
                if (xmax>maximum && minimum>(xmin -= xmax-(xmax=maximum))) xmin=minimum;
                if (xmin<xmax)
                {
                    setVisibleArea(new Rectangle2D.Double(xmin, top, xmax-xmin, Math.max(bottom-top, barHeight)));
                    if (slider!=null)
                    {
                        slider.setClipMinMax(xmin, xmax, top, top+Math.max(barHeight, ((labelBounds!=null) ? labelBounds.height : getHeight())));
                    }
                }
            }
            else
            {
                if (ymin<minimum && maximum<(ymax -= ymin-(ymin=minimum))) ymax=maximum;
                if (ymax>maximum && minimum>(ymin -= ymax-(ymax=maximum))) ymin=minimum;
                if (ymin<ymax)
                {
                    setVisibleArea(new Rectangle2D.Double(left, ymin, Math.max(right-left, barHeight), ymax-ymin));
                    if (slider!=null)
                    {
                        slider.setClipMinMax(left, left+Math.max(barHeight, getWidth()), ymin, ymax);
                    }
                }
            }
        }
    }

    /**
     * Indique si la largeur (ou hauteur) de la visière
     * peut être modifiée par l'usager avec la souris.
     */
    public boolean isRangeAdjustable()
    {
        if (slider==null) return false;
        if (horizontal)
            return slider.isAdjustable(SwingConstants.EAST)  || slider.isAdjustable(SwingConstants.WEST);
        else
            return slider.isAdjustable(SwingConstants.NORTH) || slider.isAdjustable(SwingConstants.SOUTH);
    }

    /**
     * Spécifie si la largeur (ou hauteur) de la visière
     * peut être modifiée par l'usager avec la souris.
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
     * Retourne le nombre de pixels à laisser entre la région dans laquelle les barres sont
     * dessinées et les bords de cette composante.  <strong>Notez que les marges retournées
     * par <code>getInsets(Insets)</code>  peuvent etre plus grandes que celles qui ont été
     * spécifiées à {@link #setInsets}.</strong>  Un espace suplémentaire peut avoir ajouté
     * pour tenir compte s'une éventuelle bordure qui aurait été ajoutée à la composante.
     *
     * @param  insets Objet à réutiliser si possible, ou <code>null</code>.
     * @return Les marges à laisser de chaque côté de la zone de traçage.
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
     * Défini le nombre de pixels à laisser entre la région dans laquelle les barres
     * sont dessinées et les bords de cette composante. Ce nombre de pixels doit être
     * suffisament grand pour laisser de la place pour les étiquettes de l'axe. Notez
     * que {@link #getInsets} ne va pas obligatoirement retourner ces marges exactes.
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
     * Retourne un rectangle délimitant la région de cette composante dans laquelle seront déssinés les zooms.
     * L'implémentation par défaut retourne un rectangle englobant toute cette composante, moins les marges
     * retournées par {@link #getInsets} et moins l'espace nécessaire pour écrire les étiquettes à côté des
     * barres.
     *
     * @param  bounds Rectangle dans lequel placer le résultat, ou <code>null</code> pour en créer un nouveau.
     * @return Coordonnées en pixels de la région de {@link ZoomPane} dans laquelle dessiner les zooms.
     */
    protected Rectangle getZoomableBounds(Rectangle bounds)
    {
        bounds=super.getZoomableBounds(bounds);
        /*
         * 'labelBounds' est l'espace (en pixel) occupé par les légendes des
         * barres d'intervalles. Si cet espace n'avait pas déjà été calculé,
         * on forcera son calcul immédiat en appelant 'paintComponent'.
         */
        if (labelBounds==null)
        {
            if (!valid) reset(bounds);
            paintComponent((Graphics2D) null, bounds.width+(left+right));
            if (labelBounds==null) return bounds;
        }
        if (horizontal)
        {
            bounds.x     += labelBounds.width;
            bounds.width -= labelBounds.width;
            bounds.height = labelBounds.height;
        }
        else
        {
            throw new UnsupportedOperationException();
            // Le traçage vertical n'est pas encore implémenté.
        }
        return bounds;
    }

    /**
     * Retourne la dimension par défaut de cette composante. Cette dimension sera
     * retournée par {@link #getPreferredSize} si aucune dimension préférée n'a
     * été explicitement spécifiée.
     */
    protected Dimension getDefaultSize()
    {
        final Insets insets=this.insets=getInsets(this.insets);
        final int top    = insets.top;
        final int left   = insets.left;
        final int bottom = insets.bottom;
        final int right  = insets.right;
        final Dimension size=super.getDefaultSize();
        if (axisBounds==null)
        {
            if (!valid)
            {
                // Force le calcul d'une transformation affine approximative (au moins temporairement).
                reset(new Rectangle(left, top, size.width-(left+right), size.height-(top+bottom)));
            }
            paintComponent((Graphics2D)null, size.width);
            if (axisBounds==null)
            {
                size.width  = 280;
                size.height =  60;
                return size;
            }
        }
        if (horizontal)
        {
            size.height = axisBounds.y + axisBounds.height + bottom;
        }
        else
        {
            size.width = axisBounds.x + axisBounds.width + right;
        }
        return size;
    }

    /**
     * Méthode appellée automatiquement lorsqu'il faut dessiner la composante
     * mais qu'il n'y a pas encore de données. L'implémentation par défaut écrit
     * "Aucune données à afficher".
     */
    protected void paintNodata(final Graphics2D graphics)
    {
        graphics.setColor(getForeground());
        final FontRenderContext fc=graphics.getFontRenderContext();
        final GlyphVector   glyphs=getFont().createGlyphVector(fc, Resources.format(ResourceKeys.NO_DATA_TO_DISPLAY));
        final Rectangle2D   bounds=glyphs.getVisualBounds();
        graphics.drawGlyphVector(glyphs, (float) (0.5*(getWidth()-bounds.getWidth())),
                                         (float) (0.5*(getHeight()+bounds.getHeight())));
    }

    /**
     * Dessine les barres. Les étiquettes et la série de barres qui leur sont associées seront
     * dessinées dans le même ordre qu'elles ont été spécifiées à la méthode {@link #addRange}.
     */
    protected void paintComponent(final Graphics2D graphics)
    {paintComponent(graphics, getWidth());}

    /**
     * Implémentation du traçage de cette composante.
     *
     * @param graphics Graphique dans lequel dessiner.
     * @param componentWidth Largeur de cette composante (habituellement {@link #getWidth},
     *        sauf lorsque cette méthode est appelée par {@link #getDefaultSize}).
     */
    private void paintComponent(final Graphics2D graphics, final int componentWidth)
    {
        final int rangeCount = ranges.size();
        if (rangeCount==0 || !update())
        {
            if (graphics!=null)
                paintNodata(graphics);
            return;
        }
        final Insets insets=this.insets=getInsets(this.insets);
        final int top    = insets.top;
        final int left   = insets.left;
        final int bottom = insets.bottom;
        final int right  = insets.right;

        final AbstractGraduation graduation = (AbstractGraduation) axis.getGraduation();
        final GlyphVector[]          glyphs = new GlyphVector[rangeCount];
        final double[]          labelHeight = new double     [rangeCount];
        final Font                     font = getFont();
        final Shape                    clip;
        final FontRenderContext          fc;
        if (graphics!=null)
        {
            if (!valid) reset();
            clip = graphics.getClip();
            fc   = graphics.getFontRenderContext();
        }
        else
        {
            clip = null;
            fc   = new FontRenderContext(null, false, false);
            // Ne pas appeler reset() car cette méthode a probablement été appelée
            // pour calculer une dimension par défaut, justement par 'reset()'!!
        }
        if (clip==null || labelBounds==null || clip.intersects(labelBounds))
        {
            /*
             * Construit un tableau des 'GlyphVector' correspondant aux étiquettes
             * qu'il faudra écrire. On retient au passage les dimensions maximales
             * des étiquettes. Cette information sera utilisée plus tard pour disposer
             * les élements (barres et étiquettes) correctement lors du traçage.
             */
            double labelSlotWidth=0;
            double labelSlotHeight=barHeight;
            final Iterator<String> it=ranges.keySet().iterator();
            for (int i=0; i<rangeCount; i++)
            {
                final String label=it.next();
                if (label!=null)
                {
                    glyphs[i]=font.createGlyphVector(fc, label);
                    final Rectangle2D rect = glyphs[i].getVisualBounds();
                    final double    height = rect.getHeight();
                    final double     width = rect.getWidth();
                    if (width >labelSlotWidth ) labelSlotWidth =width;
                    if (height>labelSlotHeight) labelSlotHeight=height;
                    labelHeight[i]=height;
                }
            }
            if (it.hasNext())
            {
                // Should not happen
                throw new ConcurrentModificationException();
            }
            labelSlotWidth  += barOffset;
            labelSlotHeight += lineSpacing;
            if (labelBounds==null) labelBounds=new Rectangle();
            labelBounds.setBounds(left, top, (int)Math.ceil(labelSlotWidth), (int)Math.ceil(labelSlotHeight*rangeCount));
        }
        final double labelSlotWidth  = labelBounds.getWidth();
        final double labelSlotHeight = labelBounds.getHeight()/rangeCount;
        /*
         * Maintenant que l'on connait l'espace occupé par les étiquettes,   calcule
         * la position de l'axe. Cet axe sera placé en-dessous de la zone de traçage
         * des barres. Calcule aussi les valeurs logiques minimales et maximales qui
         * correspondent aux extrémités de l'axe. Ces valeurs dépendront du zoom.
         */
        try
        {
            Point2D.Double point=this.point;
            if (point==null) this.point=point=new Point2D.Double();
            if (horizontal)
            {
                double y  = rangeCount*labelSlotHeight+top;
                double x1 = labelSlotWidth+left;
                double x2 = componentWidth-right;
                /*
                 * Calcule la valeur logique minimale qui
                 * correspond à l'extrémité gauche de l'axe.
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
                 * Calcule la valeur logique maximale qui
                 * correspond à l'extrémité droite de l'axe.
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
                // Le traçage vertical n'est pas encore supporté.
                throw new UnsupportedOperationException();
            }
        }
        catch (NoninvertibleTransformException exception)
        {
            // Should not happen
            ExceptionMonitor.paintStackTrace(graphics, getBounds(), exception);
            return;
        }
        /*
         * Dessine les étiquettes, puis les barres
         * à la droite de chacune des étiquettes.
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
            /*
             * Dessine maintenant les barres des intervalles.
             */
            if (horizontal)
            {
                final double      scaleX = zoom.getScaleX();
                final double  translateX = zoom.getTranslateX();
                final Rectangle2D.Double bar = new Rectangle2D.Double(0, top+0.5*(labelSlotHeight-barHeight), 0, barHeight);
                for (int i=0; i<rangeCount; i++)
                {
                    if (glyphs[i]!=null)
                    {
                        graphics.setColor(foreground);
                        graphics.drawGlyphVector(glyphs[i], left,
                                 (float) (top + (i*labelSlotHeight) + 0.5*(labelSlotHeight+labelHeight[i])));
                    }
                }
                graphics.setColor(backgbColor);
                graphics.fill    (zoomableBounds);
                graphics.clip    (zoomableBounds);
                graphics.setColor(barColor);
                final Iterator<RangeSet> it=ranges.values().iterator();
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
                            bar.x      = bar_min;
                            bar.width  = bar_max-bar_min;
                            bar.width *= scaleX;
                            bar.x     *= scaleX;
                            bar.x     += translateX;
                            graphics.fill(bar);
                        }
                    }
                    bar.y += labelSlotHeight;
                }
                if (it.hasNext())
                {
                    // Should not happen
                    throw new ConcurrentModificationException();
                }
            }
            else
            {
                // Le traçage vertical n'est pas encore supporté.
                throw new UnsupportedOperationException();
            }
            graphics.setClip(clip);
            graphics.setColor(foreground);
            axis.paint(graphics);
            /*
             * Dessine par dessus tout ça la visière qui
             * couvre les valeurs sélectionnées par l'utilisateur.
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
                // Note: slider doit être dessiné inconditionnellement, sans tester {@link #isEmpty}.
            }
        }
        axisBounds=axis.getBounds(); // Peut être plus précis après que l'axe ait été tracé.
        axisBounds.height++;         // Précaution pour éviter qu'il reste des parties oubliées.
    }

    /**
     * Applique une transformation sur le zoom.
     *
     * @param  change Changement à apporter au zoom, dans l'espace des coordonnées logiques.
     * @throws UnsupportedOperationException si la matrice <code>change</code> contient une
     *         opération non-supportée, par exemple si elle contient une translation verticale
     *         alors que cette composante représente des intervalles sur un axe horizontal.
     */
    public void transform(final AffineTransform change) throws UnsupportedOperationException
    {
        /*
         * Vérifie que la transformation demandée
         * est une des transformations autorisées.
         */
        if (change.getShearX()==0 && change.getShearY()==0)
        {
            if (horizontal ? (change.getScaleY()==1 && change.getTranslateY()==0) :
                             (change.getScaleX()==1 && change.getTranslateX()==0))
            {
                /*
                 * Vérifie que la transformation n'aura pas pour effet
                 * de faire sortir le graphique de la zone de traçage.
                 */
                final Rectangle labelBounds = this.labelBounds;
                final Rectangle  axisBounds = this. axisBounds;
                if (update() && labelBounds!=null && axisBounds!=null)
                {
                    Point2D.Double point=this.point;
                    if (point==null) this.point=point=new Point2D.Double();
                    if (horizontal)
                    {
                        final int xLeft   = labelBounds.x+labelBounds.width;
                        final int xRight  = Math.max(getWidth()-right, xLeft);
                        final int margin = (xRight-xLeft)/4;
                        final double x1, x2, y=labelBounds.getCenterY();
                        point.x=minimum; point.y=y; change.transform(point,point); zoom.transform(point,point); x1=point.x;
                        point.x=maximum; point.y=y; change.transform(point,point); zoom.transform(point,point); x2=point.x;
                        if (Math.min(x1,x2)>(xRight-margin) || Math.max(x1,x2)<(xLeft+margin) || Math.abs(x2-x1)<margin) return;
                    }
                    else
                    {
                        final int yTop    = labelBounds.y+labelBounds.height;
                        final int yBottom = Math.max(getHeight()-bottom, yTop);
                        final int margin  = (yBottom-yTop)/4;
                        final double y1, y2, x=labelBounds.getCenterX();
                        point.y=minimum; point.x=x; zoom.transform(point,point); change.transform(point,point); y1=point.y;
                        point.y=maximum; point.x=x; zoom.transform(point,point); change.transform(point,point); y2=point.y;
                        if (Math.min(y1,y2)>(yBottom-margin) || Math.max(y1,y2)<(yTop+margin) || Math.abs(y2-y1)<margin) return;
                    }
                }
                /*
                 * Applique la transformation, met à jour la transformation
                 * de la visière et redessine l'axe en plus du graphique.
                 */
                super.transform(change);
                if (slider!=null) slider.setTransform(zoom);
                if (axisBounds!=null) repaint(axisBounds);
                return;
            }
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Réinitialise le zoom de façon à ce que tous
     * les intervalles apparaissent dans la fenêtre.
     */
    public void reset()
    {
        reset(getZoomableBounds(null));
        if (getWidth()>0 && getHeight()>0) valid=true;
    }

    /**
     * Réinitialise le zoom de façon à ce que tous
     * les intervalles apparaissent dans la fenêtre.
     */
    private void reset(final Rectangle zoomableBounds)
    {
        reset(zoomableBounds, false);
        if (slider!=null) slider.setTransform(zoom);
        if (axisBounds!=null) repaint(axisBounds);
    }

    /**
     * Retourne les coordonnées logiques de la région dans
     * laquelle seront dessinées les barres des intervalles.
     */
    public Rectangle2D getArea()
    {
        final Insets insets=this.insets=getInsets(this.insets);
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
                int height=getHeight();
                if (height==0) height=getMinimumSize().height;
                // La hauteur n'a pas vraiment besoin d'être précise, puisqu'elle sera ignorée...
                return new Rectangle2D.Double(min, top, max-min, Math.max(height-(top+bottom),16));
            }
            else
            {
                int width=getWidth();
                if (width==0) width=getMinimumSize().width;
                // La largeur n'a pas vraiment besoin d'être précise, puisqu'elle sera ignorée...
                return new Rectangle2D.Double(left, min, Math.max(width-(left+right),16), max-min);
            }
        }
        /*
         * Ce bloc n'est exécuté que si les coordonnées logiques de la région
         * ne peuvent pas être calculées, faute d'informations suffisantes.
         */
        final Rectangle bounds=getBounds();
        bounds.x       = left;
        bounds.y       = top;
        bounds.width  -= (left+right);
        bounds.height -= (top+bottom);
        return bounds;
    }

    /**
     * Retourne un model pouvant décrire la position de la visière dans une
     * plage d'entiers. Ce model est fournit pour faciliter les interactions
     * avec <i>Swing</i>. Ses principales méthodes sont définies comme suit:
     *
     * <p>{@link BoundedRangeModel#getValue}<br>
     *    Retourne la position du bord gauche de la visière, exprimée par
     *    un entier compris entre le minimum et le maximum du model (0 et
     *    100 par défaut).</p>
     *
     * <p>{@link BoundedRangeModel#getExtent}<br>
     *    Retourne la largeur de la visière, exprimée selon les mêmes unités
     *    que <code>getValue()</code>.</p>
     *
     * <p>{@link BoundedRangeModel#setMinimum} / {@link BoundedRangeModel#setMaximum}<br>
     *    Modifie les valeurs entière minimale ou maximale retournées par <code>getValue()</code>.
     *    Cette modification n'affecte aucunement l'axe des barres affichées; elle
     *    ne fait que modifier la façon dont la position de la visière est convertie
     *    en valeur entière par <code>getValue()</code>.</p>
     *
     * <p>{@link BoundedRangeModel#setValue} / {@link BoundedRangeModel#setExtent}<br>
     *    Modifie la position du bord gauche de la visière ou sa largeur.</p>
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
     * Modèle assurant une interopérabilité de {@link RangeBars} avec <i>Swing</i>.
     * La méthode <code>fireStateChanged(boolean)</code> sera appellée automatiquement
     * chaque fois que l'utilisateur fait glisser la visière.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class SwingModel extends DefaultBoundedRangeModel implements LogicalBoundedRangeModel
    {
        /**
         * Décalage intervenant dans la conversion de la position
         * de la visière en valeur entière. Le calcul se fait par
         * <code>int_x=(x-offset)*scale</code>.
         */
        private double offset;

        /**
         * Facteur d'échelle intervenant dans la conversion de la position de la visière
         * en valeur entière. Le calcul se fait par <code>int_x=x*scale+offset</code>.
         */
        private double scale;

        /**
         * Indique si les coefficients <code>offset</code> et <code>scale</code> sont à jour.
         * Ce champs sera mis à <code>false</code> chaque fois que des plages sont ajoutées
         * ou supprimées dans l'objet {@link RangeBars}.
         */
        boolean updated;

        /**
         * Indique d'où vient le dernier ajustement
         * de la valeur: du model ou de la visière.
         */
        private boolean lastAdjustFromModel;

        /**
         * La valeur <code>true</code> indique que {@link #fireStateChanged}
         * ne doit pas prendre en compte le prochain événement. Ce champ est
         * utilisé lors des changements de la position de la visière.
         */
        private transient boolean ignoreEvent;

        /**
         * Valeur minimale. La valeur <code>NaN</code> indique qu'il
         * faut puiser le minimum dans les données de {@link RangeBars}.
         */
        private double minimum=Double.NaN;

        /**
         * Valeur maximale. La valeur <code>NaN</code> indique qu'il
         * faut puiser le maximum dans les données de {@link RangeBars}.
         */
        private double maximum=Double.NaN;

        /**
         * Construit un model avec par défaut une plage allant de 0 à 100. Les valeurs
         * de cette plage sont toujours indépendantes de celles de {@link RangeBars}.
         */
        public SwingModel()
        {}

        /**
         * Spécifie les minimum et maximum des valeurs entières.
         * Une valeur {@link Double#NaN} signifie de prendre une
         * valeur par défaut.
         */
        public void setLogicalRange(final double minimum, final double maximum)
        {
            this.minimum = minimum;
            this.maximum = maximum;
            updated = false;
        }

        /**
         * Convertit une valeur entière en nombre réel.
         */
        public double toLogical(final int integer)
        {return (integer-offset)/scale;}

        /**
         * Convertit un nombre réel en valeur entière.
         */
        public int toInteger(final double logical)
        {return (int) Math.round(logical*scale + offset);}

        /**
         * Positionne de la visière en fonction
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
         * Met à jour les champs {@link #offset} et {@link #scale}. Les minimum
         * maximum ainsi que la valeur actuels du model seront réutilisés. C'est
         * de la responsabilité du programmeur de mettre à jour ces propriétés si
         * c'est nécessaire.
         */
        private void ensureUpdated()
        {update(super.getMinimum(), super.getMaximum());}

        /**
         * Met à jour les champs {@link #offset} et {@link #scale} en fonction
         * des minimum et maximum spécifiés. Il sera de la responsabilité de
         * l'appellant d'appeller l'équivalent de <code>setMinimum(lower)</code>
         * et <code>setMaximum(upper)</code>. Après l'appel de cette méthode, la
         * position de la visière pourra être convertie en valeur entière par
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
         * Met à jour les champs internes de ce model et lance un
         * évènement prévenant que la position ou la largeur de la
         * visière a changée. Cette méthode est appellée à partir
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
         * Met à jour les champs internes de ce model et lance un
         * évènement prevenant que la position ou la largeur de la
         * visière a changée.
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
         * Modifie la valeur minimale retournée par {@link #getValue}.
         * La valeur retournée par cette dernière sera modifiée pour
         * qu'elle corresponde à la position de la visière dans les
         * nouvelles limites.
         */
        public void setMinimum(final int minimum)
        {setRangeProperties(minimum, super.getMaximum(), false);}

        /**
         * Modifie la valeur maximale retournée par {@link #getValue}.
         * La valeur retournée par cette dernière sera modifiée pour
         * qu'elle corresponde à la position de la visière dans les
         * nouvelles limites.
         */
        public void setMaximum(final int maximum)
        {setRangeProperties(super.getMinimum(), maximum, false);}

        /**
         * Retourne la position de la visière.
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
         * Modifie la position de la visière.
         */
        public void setValue(final int value)
        {
            lastAdjustFromModel=true;
            super.setValue(value);
            setSliderPosition();
        }

        /**
         * Retourne l'étendu de la visière.
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
         * Modifie la largeur de la visière.
         */
        public void setExtent(final int extent)
        {
            lastAdjustFromModel=true;
            super.setExtent(extent);
            setSliderPosition();
        }

        /**
         * Modifie l'ensemble des paramètres d'un coups.
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
         * Modifie la position de la visière en fonction
         * des valeurs actuelles du modèle.
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
     * Paneau contenant des champs et des boutons qui permettent à l'usager
     * d'éditer manuellement la position de la visière. Ce paneau peut être
     * flottant, c'est-à-dire dans une composante indépendante du graphique
     * à barres {@link RangeBars}. Il peut aussi apparaître juste à la gauche
     * du graphique à barres, dans une composante retournée par la méthode
     * {@link #getCombinedPane}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class Editors extends JComponent
    {
        /**
         * Construit un paneau des champs de textes et des boutons permettant
         * à l'usager d'éditer la position et l'étendue de la visière.
         *
         * @param format Format à utiliser pour écrire les valeurs de la plage sélectionnée,
         *               ainsi que pour interpréter les valeurs entrées par l'utilisateur.
         *               Ce format est souvent de la classe {@link java.text.NumberFormat}
         *               ou {@link java.text.DateFormat}. La valeur <code>null</code> indique
         *               qu'il faut utiliser un format par défaut.
         */
        public Editors(final RangeBars rangeBars, Format format)
        {
            rangeBars.ensureSliderCreated();
            if (format==null)     format = rangeBars.axis.getGraduation().getFormat();
            final JTextComponent editor1 = rangeBars.slider.addEditor(format, SwingConstants.WEST, rangeBars);
            final JTextComponent editor2 = rangeBars.slider.addEditor(format, SwingConstants.EAST, rangeBars);
            final GridBagConstraints   c = new GridBagConstraints();
            setLayout(new GridBagLayout());
            /*
             * Place les champs de textes qui permettent d'éditer
             * la position d'un des bords de la visière.
             */
            c.gridx=0; c.weightx=1; c.fill=c.HORIZONTAL;
            c.gridy=0; add(editor1, c);
            c.gridy=1; add(editor2, c);
            /*
             * Ajuste l'ordre des focus
             * et termine cette méthode.
             */
            editor1.  setNextFocusableComponent(editor2  );
            editor2.  setNextFocusableComponent(rangeBars);
            rangeBars.setNextFocusableComponent(editor1  );
            rangeBars.requestFocus();
            setBorder(BorderFactory.createCompoundBorder(
                      BorderFactory.createEtchedBorder(),
                      BorderFactory.createEmptyBorder(3,3,3,3)));
            final Dimension size=getPreferredSize();
            size.width = 100;
            setPreferredSize(size);
            setMinimumSize  (size);
        }
    }

    /**
     * Retourne un paneau qui comprendra <code>this</code> plus d'autres contrôles.
     * Ces autres contrôles peuvent comprendre des boutons ou des champs de textes
     * qui permettront à l'utilisateur d'entrer directement les valeurs de la plage
     * qui l'intéresse.
     *
     * @param format Format à utiliser pour écrire les valeurs de la plage sélectionnée,
     *               ainsi que pour interpréter les valeurs entrées par l'utilisateur.
     *               Ce format est souvent de la classe {@link java.text.NumberFormat}
     *               ou {@link java.text.DateFormat}. La valeur <code>null</code> indique
     *               qu'il faut utiliser un format par défaut.
     */
    public JComponent createControlPanel(final Format format)
    {
        final JComponent panel=new JPanel(new GridBagLayout());
        final GridBagConstraints c=new GridBagConstraints();
        c.gridy=0; c.weighty=1;
        c.gridx=0; c.weightx=1; c.fill=c.BOTH;       panel.add(this, c);
        c.gridx=1; c.weightx=0; c.fill=c.HORIZONTAL;
        c.insets.right=6; panel.add(new Editors(this, format), c);
        return panel;
    }

    /**
     * Fait apparaître dans une fenêtre quelques histogrammes
     * calculés au hasard. Cette méthode sert à vérifier le
     * bon fonctionnement de la classe <code>RangeBars</code>.
     */
    public static void main(final String[] args)
    {
        final JFrame frame=new JFrame("RangeBars");
        final RangeBars ranges=new RangeBars();
        for (int série=1; série<=4; série++)
        {
            final String clé="Série #"+série;
            for (int i=0; i<100; i++)
            {
                final double x = 1000*Math.random();
                final double w =   30*Math.random();
                ranges.addRange(clé, new Double(x), new Double(x+w));
            }
        }
        ranges.setSelectedRange(12, 38);
        ranges.setRangeAdjustable(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(ranges.createControlPanel(null));
        frame.pack();
        frame.show();
    }
}
