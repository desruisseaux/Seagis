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

// Axes et mod�les
import net.seas.plot.axis.Axis;
import net.seas.plot.axis.Graduation;
import net.seas.plot.axis.DateGraduation;
import net.seas.plot.axis.NumberGraduation;
import net.seas.plot.axis.AbstractGraduation;

// Coordonn�es et unit�s
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

// G�om�trie
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
 * Paneau repr�sentant les plages des donn�es disponibles. Ces plages sont repr�sent�es par des
 * barres verticales. L'axe des <var>x</var> repr�sente les valeurs, et sur l'axe des <var>y</var>
 * on place les diff�rents types de donn�es, un peu comme le ferait un histogramme.
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
     * Donn�es des barres. Chaque entr� est constitu� d'une paire (<em>�tiquette</em>,
     * <em>tableau de donn�es</em>). Le tableau de donn�e sera g�n�ralement (mais pas
     * obligatoirement) un tableau de type <code>long[]</code>. Les donn�es de ces
     * tableaux seront organis�es par paires de valeurs, de la forme (<i>d�but</i>,<i>fin</i>).
     */
    private final Map<String,RangeSet> ranges=new LinkedHashMap<String,RangeSet>();

    /**
     * Axe des <var>x</var> servant � �crire les valeurs des plages. Les m�thodes de
     * {@link Axis} peuvent �tre appell�es pour modifier le format des nombres, une
     * �tiquette, des unit�s ou pour sp�cifier "� la main" les minimums et maximums.
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
     * Coordonn�es (en pixels) de la r�gion dans laquelle seront dessin�es les �tiquettes.
     * Ce champ est nul si ces coordonn�es ne sont pas encore connues. Ces coordonn�es sont
     * calcul�es par {@link #paintComponent(Graphics2D)}  (notez que cette derni�re accepte
     * un argument {@link Graphics2D} nul).
     */
    private transient Rectangle labelBounds;

    /**
     * Coordonn�es (en pixels) de la r�gion dans laquelle sera dessin�e l'axe.
     * Ce champ est nul si ces coordonn�es ne sont pas encore connues. Ces coordonn�es sont
     * calcul�es par {@link #paintComponent(Graphics2D)}  (notez que cette derni�re accepte
     * un argument {@link Graphics2D} nul).
     */
    private transient Rectangle axisBounds;

    /**
     * Indique si cette composante sera orient�e horizontalement ou
     * verticalement. Note: l'orientation verticale n'a pas �t� test�e.
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

    /** Hauteur (en pixels) des barres des histogrammes.         */ private final short   barHeight=8; // en pixels
    /** Espace (en pixels) entre les �tiquettes et leurs barres. */ private final short   barOffset=6; // en pixels
    /** Espace (en pixels) � ajouter entre deux lignes.          */ private final short lineSpacing=3; // en pixels
    /** Couleur de la r�gion de tra�age du graphique.            */ private final Color backgbColor=Color.white;
    /** Couleur des barres.                                      */ private final Color    barColor=new Color(255, 153,  51);
    /** Couleur de la zone des dates s�lectionn�es.              */ private final Color    selColor=new Color(128,  64,  92, 64);
    //  La couleur des �tiquettes peut �tre sp�cifi�e par un appel � setForeground(Color).

    /**
     * Bordure � placer tout le tour du graphique.
     */
    private final Border border=BorderFactory.createEtchedBorder();

    /**
     * Plage de valeurs pr�sentement s�lectionn�e par l'utilisateur. Cette plage
     * appara�tra comme un rectangle transparent (une <em>visi�re</em>) par dessus
     * les barres. Ce champ est initialement nul. Une visi�re ne sera cr��e que
     * lorsqu'elle sera n�cessaire.
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
    {this((Unit)null);}

    /**
     * Construit un paneau initialement vide qui repr�sentera des
     * nombres selon les unit�s sp�cifi�es. Des donn�es pourront
     * �tre ajout�es avec la m�thode {@link #addRange} pour faire
     * appara�tre des barres.
     */
    public RangeBars(final Unit unit)
    {this(new NumberGraduation(unit), true);}

    /**
     * Construit un paneau initialement vide qui repr�sentera des
     * dates dans le fuseau horaire sp�cifi�. Des donn�es pourront
     * �tre ajout�es avec la m�thode {@link #addRange} pour faire
     * appara�tre des barres.
     */
    public RangeBars(final TimeZone timezone)
    {this(new DateGraduation(timezone), true);}

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
     * Ajoute une plage de valeurs. Chaque plage de valeurs est associ�e � une �tiquette.
     * Il est possible de sp�cifier (dans n'importe quel ordre) plusieurs plages � une m�me
     * �tiquette. Si deux plages se chevauchent pour une �tiquette donn�e, elles seront
     * fusionn�es ensemble.
     *
     * @param label Etiquette d�signant la barre pour laquelle on veut ajouter une plage.
     *              Si cette �tiquette avait d�j� �t� utilis�e pr�c�demment, les donn�es
     *              seront ajout�es � la barre d�j� existante. Sinon, une nouvelle barre
     *              sera cr��e. Les diff�rences entres majuscules et minuscules sont prises
     *              en compte. La valeur <code>null</code> est autoris�e.
     * @param first D�but de la plage.
     * @param last  Fin de la plage.
     *
     * @throws NullPointerException Si <code>first</code> ou <code>last</code> est nul.
     * @throws IllegalArgumentException Si <code>first</code> et <code>last</code> ne
     *         sont pas de la m�me classe, ou s'ils ne sont pas de la classe des �l�ments
     *         pr�c�demment m�moris�s sous l'�tiquette <code>label</code>.
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
     * Met � jour les champs {@link #minimum} et {@link #maximum}, si ce n'�tait pas d�j� fait.
     * Cette mise-�-jour sera faite en fonction des intervalles en m�moire. Si la mise � jour
     * n'a pas �t� possible (parce exemple parce qu'aucune donn�e n'a �t� sp�cifi�e), alors
     * cette m�thode retourne <code>false</code>.
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
        labelBounds=null;
        axisBounds=null;
        if (swingModel!=null)
        {
            swingModel.updated=false;
        }
    }

    /**
     * Sp�cifie si la l�gende de l'axe peut affich�e. Appeller cette m�thode avec la valeur
     * <code>false</code> d�sactivera l'affichage de la l�gende m�me si {@link #getLegend}
     * retourne une chaine non-nulle.
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
     * Retourne la liste des �tiquettes en m�moire, dans l'ordre dans lequel elles seront
     * �crites. Le tableau retourn� est une copie des tableaux internes. En cons�quence,
     * les changements faits sur ce tableau n'auront pas de r�percussions sur <code>this</code>.
     */
    public synchronized String[] getLabels()
    {return ranges.keySet().toArray(new String[ranges.size()]);}

    /**
     * Retourne la valeur minimale m�moris�e. Si plusieurs �tiquettes ont �t� sp�cifi�es,
     * elles seront tous prises en compte. Si aucune valeur n'a �t� m�moris�e dans cet objet,
     * alors cette m�thode retourne <code>null</code>.
     */
    public synchronized Comparable getMinimum()
    {return getMinimum(getLabels());}

    /**
     * Retourne la valeur maximale m�moris�e. Si plusieurs �tiquettes ont �t� sp�cifi�es,
     * elles seront tous prises en compte. Si aucune valeur n'a �t� m�moris�e dans cet objet,
     * alors cette m�thode retourne <code>null</code>.
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
     * Retourne la valeur minimale m�moris�e sous les �tiquettes sp�cifi�es. Si aucune
     * donn�e n'a �t� m�moris�e sous ces �tiquettes, retourne <code>null</code>.
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
     * Retourne la valeur maximale m�moris�e sous les �tiquettes sp�cifi�es. Si aucune
     * donn�e n'a �t� m�moris�e sous ces �tiquettes, retourne <code>null</code>.
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
     * sera cr��e et positionn�. Si on n'avait pas assez d'informations pour
     * positionner la visi�re, sa cr�ation sera annul�e.
     */
    private void ensureSliderCreated()
    {
        if (slider!=null) return;
        /*
         * Si un model existait, on l'utilisera pour
         * d�finir la position initiale de la visi�re.
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
     * plage s�lectionn�e par l'utilisateur.
     */
    public double getSelectedValue()
    {return (slider==null) ? Double.NaN : (horizontal) ? slider.getCenterX() : slider.getCenterY();}

    /**
     * Retourne la valeur au d�but de la
     * plage s�lectionn�e par l'utilisateur.
     */
    public double getMinSelectedValue()
    {return (slider==null) ? Double.NaN : (horizontal) ? slider.getMinX() : slider.getMinY();}

    /**
     * Retourne la valeur � la fin de la
     * plage s�lectionn�e par l'utilisateur.
     */
    public double getMaxSelectedValue()
    {return (slider==null) ? Double.NaN : (horizontal) ? slider.getMaxX() : slider.getMaxY();}

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
            slider.setX(min, max);
        else
            slider.setY(min, max);
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
            setVisibleRange(xmin, xmax, Double.NaN, Double.NaN);
        else
            setVisibleRange(Double.NaN, Double.NaN, xmin, xmax);
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
            final double minimim=this.minimum;
            final double maximum=this.maximum;
            /*
             * Dans les lignes suivantes, on a r�duit
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
     * Indique si la largeur (ou hauteur) de la visi�re
     * peut �tre modifi�e par l'usager avec la souris.
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
     * Sp�cifie si la largeur (ou hauteur) de la visi�re
     * peut �tre modifi�e par l'usager avec la souris.
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
     * Retourne le nombre de pixels � laisser entre la r�gion dans laquelle les barres sont
     * dessin�es et les bords de cette composante.  <strong>Notez que les marges retourn�es
     * par <code>getInsets(Insets)</code>  peuvent etre plus grandes que celles qui ont �t�
     * sp�cifi�es � {@link #setInsets}.</strong>  Un espace supl�mentaire peut avoir ajout�
     * pour tenir compte s'une �ventuelle bordure qui aurait �t� ajout�e � la composante.
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
     * D�fini le nombre de pixels � laisser entre la r�gion dans laquelle les barres
     * sont dessin�es et les bords de cette composante. Ce nombre de pixels doit �tre
     * suffisament grand pour laisser de la place pour les �tiquettes de l'axe. Notez
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
     * Retourne un rectangle d�limitant la r�gion de cette composante dans laquelle seront d�ssin�s les zooms.
     * L'impl�mentation par d�faut retourne un rectangle englobant toute cette composante, moins les marges
     * retourn�es par {@link #getInsets} et moins l'espace n�cessaire pour �crire les �tiquettes � c�t� des
     * barres.
     *
     * @param  bounds Rectangle dans lequel placer le r�sultat, ou <code>null</code> pour en cr�er un nouveau.
     * @return Coordonn�es en pixels de la r�gion de {@link ZoomPane} dans laquelle dessiner les zooms.
     */
    protected Rectangle getZoomableBounds(Rectangle bounds)
    {
        bounds=super.getZoomableBounds(bounds);
        /*
         * 'labelBounds' est l'espace (en pixel) occup� par les l�gendes des
         * barres d'intervalles. Si cet espace n'avait pas d�j� �t� calcul�,
         * on forcera son calcul imm�diat en appelant 'paintComponent'.
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
            // Le tra�age vertical n'est pas encore impl�ment�.
        }
        return bounds;
    }

    /**
     * Retourne la dimension par d�faut de cette composante. Cette dimension sera
     * retourn�e par {@link #getPreferredSize} si aucune dimension pr�f�r�e n'a
     * �t� explicitement sp�cifi�e.
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
     * M�thode appell�e automatiquement lorsqu'il faut dessiner la composante
     * mais qu'il n'y a pas encore de donn�es. L'impl�mentation par d�faut �crit
     * "Aucune donn�es � afficher".
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
     * Dessine les barres. Les �tiquettes et la s�rie de barres qui leur sont associ�es seront
     * dessin�es dans le m�me ordre qu'elles ont �t� sp�cifi�es � la m�thode {@link #addRange}.
     */
    protected void paintComponent(final Graphics2D graphics)
    {paintComponent(graphics, getWidth());}

    /**
     * Impl�mentation du tra�age de cette composante.
     *
     * @param graphics Graphique dans lequel dessiner.
     * @param componentWidth Largeur de cette composante (habituellement {@link #getWidth},
     *        sauf lorsque cette m�thode est appel�e par {@link #getDefaultSize}).
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
            // Ne pas appeler reset() car cette m�thode a probablement �t� appel�e
            // pour calculer une dimension par d�faut, justement par 'reset()'!!
        }
        if (clip==null || labelBounds==null || clip.intersects(labelBounds))
        {
            /*
             * Construit un tableau des 'GlyphVector' correspondant aux �tiquettes
             * qu'il faudra �crire. On retient au passage les dimensions maximales
             * des �tiquettes. Cette information sera utilis�e plus tard pour disposer
             * les �lements (barres et �tiquettes) correctement lors du tra�age.
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
         * Maintenant que l'on connait l'espace occup� par les �tiquettes,   calcule
         * la position de l'axe. Cet axe sera plac� en-dessous de la zone de tra�age
         * des barres. Calcule aussi les valeurs logiques minimales et maximales qui
         * correspondent aux extr�mit�s de l'axe. Ces valeurs d�pendront du zoom.
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
                 * correspond � l'extr�mit� gauche de l'axe.
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
                 * correspond � l'extr�mit� droite de l'axe.
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
                // Le tra�age vertical n'est pas encore support�.
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
         * Dessine les �tiquettes, puis les barres
         * � la droite de chacune des �tiquettes.
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
                // Le tra�age vertical n'est pas encore support�.
                throw new UnsupportedOperationException();
            }
            graphics.setClip(clip);
            graphics.setColor(foreground);
            axis.paint(graphics);
            /*
             * Dessine par dessus tout �a la visi�re qui
             * couvre les valeurs s�lectionn�es par l'utilisateur.
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
                // Note: slider doit �tre dessin� inconditionnellement, sans tester {@link #isEmpty}.
            }
        }
        axisBounds=axis.getBounds(); // Peut �tre plus pr�cis apr�s que l'axe ait �t� trac�.
        axisBounds.height++;         // Pr�caution pour �viter qu'il reste des parties oubli�es.
    }

    /**
     * Applique une transformation sur le zoom.
     *
     * @param  change Changement � apporter au zoom, dans l'espace des coordonn�es logiques.
     * @throws UnsupportedOperationException si la matrice <code>change</code> contient une
     *         op�ration non-support�e, par exemple si elle contient une translation verticale
     *         alors que cette composante repr�sente des intervalles sur un axe horizontal.
     */
    public void transform(final AffineTransform change) throws UnsupportedOperationException
    {
        /*
         * V�rifie que la transformation demand�e
         * est une des transformations autoris�es.
         */
        if (change.getShearX()==0 && change.getShearY()==0)
        {
            if (horizontal ? (change.getScaleY()==1 && change.getTranslateY()==0) :
                             (change.getScaleX()==1 && change.getTranslateX()==0))
            {
                /*
                 * V�rifie que la transformation n'aura pas pour effet
                 * de faire sortir le graphique de la zone de tra�age.
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
                 * Applique la transformation, met � jour la transformation
                 * de la visi�re et redessine l'axe en plus du graphique.
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
     * R�initialise le zoom de fa�on � ce que tous
     * les intervalles apparaissent dans la fen�tre.
     */
    public void reset()
    {
        reset(getZoomableBounds(null));
        if (getWidth()>0 && getHeight()>0) valid=true;
    }

    /**
     * R�initialise le zoom de fa�on � ce que tous
     * les intervalles apparaissent dans la fen�tre.
     */
    private void reset(final Rectangle zoomableBounds)
    {
        reset(zoomableBounds, false);
        if (slider!=null) slider.setTransform(zoom);
        if (axisBounds!=null) repaint(axisBounds);
    }

    /**
     * Retourne les coordonn�es logiques de la r�gion dans
     * laquelle seront dessin�es les barres des intervalles.
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
                // La hauteur n'a pas vraiment besoin d'�tre pr�cise, puisqu'elle sera ignor�e...
                return new Rectangle2D.Double(min, top, max-min, Math.max(height-(top+bottom),16));
            }
            else
            {
                int width=getWidth();
                if (width==0) width=getMinimumSize().width;
                // La largeur n'a pas vraiment besoin d'�tre pr�cise, puisqu'elle sera ignor�e...
                return new Rectangle2D.Double(left, min, Math.max(width-(left+right),16), max-min);
            }
        }
        /*
         * Ce bloc n'est ex�cut� que si les coordonn�es logiques de la r�gion
         * ne peuvent pas �tre calcul�es, faute d'informations suffisantes.
         */
        final Rectangle bounds=getBounds();
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
     * Paneau contenant des champs et des boutons qui permettent � l'usager
     * d'�diter manuellement la position de la visi�re. Ce paneau peut �tre
     * flottant, c'est-�-dire dans une composante ind�pendante du graphique
     * � barres {@link RangeBars}. Il peut aussi appara�tre juste � la gauche
     * du graphique � barres, dans une composante retourn�e par la m�thode
     * {@link #getCombinedPane}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class Editors extends JComponent
    {
        /**
         * Construit un paneau des champs de textes et des boutons permettant
         * � l'usager d'�diter la position et l'�tendue de la visi�re.
         *
         * @param format Format � utiliser pour �crire les valeurs de la plage s�lectionn�e,
         *               ainsi que pour interpr�ter les valeurs entr�es par l'utilisateur.
         *               Ce format est souvent de la classe {@link java.text.NumberFormat}
         *               ou {@link java.text.DateFormat}. La valeur <code>null</code> indique
         *               qu'il faut utiliser un format par d�faut.
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
             * Place les champs de textes qui permettent d'�diter
             * la position d'un des bords de la visi�re.
             */
            c.gridx=0; c.weightx=1; c.fill=c.HORIZONTAL;
            c.gridy=0; add(editor1, c);
            c.gridy=1; add(editor2, c);
            /*
             * Ajuste l'ordre des focus
             * et termine cette m�thode.
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
     * Retourne un paneau qui comprendra <code>this</code> plus d'autres contr�les.
     * Ces autres contr�les peuvent comprendre des boutons ou des champs de textes
     * qui permettront � l'utilisateur d'entrer directement les valeurs de la plage
     * qui l'int�resse.
     *
     * @param format Format � utiliser pour �crire les valeurs de la plage s�lectionn�e,
     *               ainsi que pour interpr�ter les valeurs entr�es par l'utilisateur.
     *               Ce format est souvent de la classe {@link java.text.NumberFormat}
     *               ou {@link java.text.DateFormat}. La valeur <code>null</code> indique
     *               qu'il faut utiliser un format par d�faut.
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
     * Fait appara�tre dans une fen�tre quelques histogrammes
     * calcul�s au hasard. Cette m�thode sert � v�rifier le
     * bon fonctionnement de la classe <code>RangeBars</code>.
     */
    public static void main(final String[] args)
    {
        final JFrame frame=new JFrame("RangeBars");
        final RangeBars ranges=new RangeBars();
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
        frame.getContentPane().add(ranges.createControlPanel(null));
        frame.pack();
        frame.show();
    }
}
