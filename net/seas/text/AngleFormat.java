/*
 * OpenGIS implementation in Java
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
package net.seas.text;

// Text format
import java.util.Locale;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.DecimalFormatSymbols;

// Angles
import net.seas.opengis.pt.Angle;
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.pt.Longitude;

// Swing (for JSpinner)
import javax.swing.JSpinner;
import javax.swing.JFormattedTextField;
import javax.swing.AbstractSpinnerModel;
import javax.swing.text.InternationalFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.io.Serializable;

// Miscellaneous
import net.seas.util.XMath;
import net.seas.util.XClass;
import net.seas.util.XString;
import net.seas.resources.Resources;


/**
 * Écrit et interprète des angles exprimés en degrés selon un certain patron. Cette classe
 * travaille sur des angles mémorisés dans des objets de la classe {@link Angle}.
 * Les écritures des angles se font selon un patron bien défini. Par exemple le patron
 * "<code>D°MM.mm'</code>" écrira des angles comme "<code>48°04.39'</code>".
 * Les symboles autorisés sont:
 *
 * <blockquote><table>
 *     <tr><td><code>D</code></td><td>&nbsp;&nbsp;Désigne la partie entière des degrés</td></tr>
 *     <tr><td><code>M</code></td><td>&nbsp;&nbsp;Désigne la partie entière des minutes</td></tr>
 *     <tr><td><code>S</code></td><td>&nbsp;&nbsp;Désigne la partie entière des minutes</td></tr>
 * </table></blockquote><br>
 *
 * Alors que les lettres majuscules (<code>D</code>, <code>M</code> et <code>S</code>)
 * désignent les parties entières, les lettres minuscules désignent les parties fractionnaires. Ainsi
 * par exemple <code>m</code> désigne la partie fractionnaire des minutes. Les parties fractionnaires
 * doivent toujours apparaîtrent en dernier dans le patron, et immédiatement après leur partie entière.
 * Par exemple "<code>D.dd°MM'</code>" et "<code>D.mm</code>" sont tout deux illégaux, mais
 * "<code>D.dd°</code>" est légal. Les symboles non reconnus dans le patron (par exemple <code>°</code>,
 * <code>'</code> et <code>"</code>) sont écrits tels quels et peuvent apparaître à n'importe quel position.
 * Ces caractères séparateurs peuvent aussi être complètement absents. Ainsi les champs collés sont autorisés.
 * Par exemple "<code>DDDMMmm</code>" peut lire et écrire "<code>0480439</code>".
 * <br><br>
 * Le nombre de fois qu'est répété un code indique le nombre minimal de caractères qu'il doit
 * occuper. Par exemple, "<code>DD.ddd</code>" indique que la partie entière des degrés devra toujours occuper
 * au moins 2 chiffres et la partie fractionnaire exactement 3 chiffres. Un angle écrit selon ce patron pourrait
 * être "<code>08.930</code>".
 * <br><br>
 * Dans le patron, le point décimal entre la partie entière et fractionnaire représente toujours
 * le séparateur décimal par défaut pour une langue donnée (la virgule pour les francophones). Ce séparateur décimal
 * par défaut dépend de l'objet {@link DecimalFormatSymbols} qui a été spécifié lors de la construction. Par exemple
 * le patron "<code>D°MM.mm'</code>" peut écrire "<code>46°12,32'</code>" si la langue de l'utilisateur est le français.
 *
 * @see Angle
 * @see Latitude
 * @see Longitude
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class AngleFormat extends Format
{
    /**
     * Caractère représentant l'hémisphère nord.
     * Il doit obligatoirement être en majuscule.
     */
    private static final char NORTH='N';

    /**
     * Caractère représentant l'hémisphère sud.
     * Il doit obligatoirement être en majuscule.
     */
    private static final char SOUTH='S';

    /**
     * Caractère représentant l'hémisphère est.
     * Il doit obligatoirement être en majuscule.
     */
    private static final char EAST='E';

    /**
     * Caractère représentant l'hémisphère ouest.
     * Il doit obligatoirement être en majuscule.
     */
    private static final char WEST='W';

    /**
     * Constante indique que l'angle
     * à formater est une longitude.
     */
    static final int LONGITUDE=0;

    /**
     * Constante indique que l'angle
     * à formater est une latitude.
     */
    static final int LATITUDE=1;

    /**
     * Constante indique que le nombre
     * à formater est une altitude.
     */
    static final int ALTITUDE=2;

    /**
     * Constante désignant le champs des degrés. Cette valeur peut être
     * transmise au constructeur de {@link java.text.FieldPosition}, afin
     * que la méthode {@link #format(Object,StringBuffer,FieldPosition)}
     * puisse donner les index de la partie des degrés de l'angle écrit.
     */
    public static final int DEGREES_FIELD=1;

    /**
     * Constante désignant le champs des minutes. Cette valeur peut être
     * transmise au constructeur de {@link java.text.FieldPosition}, afin
     * que la méthode {@link #format(Object,StringBuffer,FieldPosition)}
     * puisse donner les index de la partie des minutes de l'angle écrit.
     */
    public static final int MINUTES_FIELD=2;

    /**
     * Constante désignant le champs des secondes. Cette valeur peut être
     * transmise au constructeur de {@link java.text.FieldPosition}, afin
     * que la méthode {@link #format(Object,StringBuffer,FieldPosition)}
     * puisse donner les index de la partie des secondes de l'angle écrit.
     */
    public static final int SECONDS_FIELD=3;

    /**
     * Constante désignant le champs de l'hémisphère. Cette valeur peut être
     * transmise au constructeur de {@link java.text.FieldPosition}, afin
     * que la méthode {@link #format(Object,StringBuffer,FieldPosition)}
     * puisse donner les index de l'hémisphère dans l'angle écrit.
     */
    public static final int HEMISPHERE_FIELD=4;

    /**
     * Symboles représentant les degrés (0),
     * minutes (1) et les secondes (2).
     */
    private static final char[] SYMBOLS = {'D', 'M', 'S'};
    
    /**
     * Formateur global utilisé pour {@link Angle#toString}.
     * Les paramètres de ce format ne devrait jamais être modifiés.
     */
    static final AngleFormat sharedInstance=new AngleFormat(); // Doit être le dernier champ statique!

    /**
     * Nombre minimal d'espaces que doivent occuper les parties
     * entières des degrés (0), minutes (1) et secondes (2). Le
     * champs <code>widthDecimal</code> indique la largeur fixe
     * que doit avoir la partie décimale. Il s'appliquera au
     * dernier champs non-zero dans <code>width0..2</code>.
     */
    private int width0=1, width1=2, width2=0, widthDecimal=0;
    
    /**
     * Caractères à insérer au début (<code>prefix</code>) et à la
     * suite des degrés, minutes et secondes (<code>suffix0..2</code>).
     * Ces champs doivent être <code>null</code> s'il n'y a rien à insérer.
     */
    private String prefix=null, suffix0="°", suffix1="'", suffix2="\"";

    /**
     * Indique s'il faut utiliser le séparateur décimal pour séparer la partie
     * entière de la partie fractionnaire. La valeur <code>false</code> indique
     * que les parties entières et fractionnaires doivent être écrites ensembles
     * (par exemple 34867 pour 34.867). La valeur par défaut est <code>true</code>.
     */
    private boolean decimalSeparator=true;

    /**
     * Format à utiliser pour écrire les nombres
     * (degrés, minutes ou secondes) à l'intérieur
     * de l'écriture d'un angle.
     */
    private final DecimalFormat numberFormat;
    
    /**
     * Objet à transmetre aux méthodes <code>DecimalFormat.format</code>.
     * Ce paramètre existe simplement pour éviter de créer cet objet trop
     * souvent, alors qu'on ne s'y intéresse pas.
     */
    private final transient FieldPosition dummy=new FieldPosition(0);

    /**
     * Retourne la largeur du champ spécifié.
     */
    private int getWidth(final int index)
    {
        switch (index)
        {
            case 0:  return width0;
            case 1:  return width1;
            case 2:  return width2;
            default: return 0; // Must be 0 (important)
        }
    }

    /**
     * Modifie la largeur du champ spécifié. La largeur
     * de tous les champ qui suivent sera mis à 0.
     */
    private void setWidth(final int index, int width)
    {
        switch (index)
        {
            case 0: width0=width; width=0; // fall through
            case 1: width1=width; width=0; // fall through
            case 2: width2=width;          // fall through
        }
    }

    /**
     * Retourne le suffix du champ spécifié.
     */
    private String getSuffix(final int index)
    {
        switch (index)
        {
            case -1: return prefix;
            case  0: return suffix0;
            case  1: return suffix1;
            case  2: return suffix2;
            default: return null;
        }
    }

    /**
     * Modifie le suffix du champs spécifié. Les suffix de tous
     * les champ qui suivent seront mis à leur valeur par défaut.
     */
    private void setSuffix(final int index, String s)
    {
        switch (index)
        {
            case -1:  prefix=s; s="°";  // fall through
            case  0: suffix0=s; s="'";  // fall through
            case  1: suffix1=s; s="\""; // fall through
            case  2: suffix2=s;         // fall through
        }
    }

    /**
     * Construit un objet qui effectuera les lectures et
     * écritures des angles dans un format par défaut.
     */
    public AngleFormat()
    {this("D°MM.m'");}

    /**
     * Construit un objet qui effectuera les lectures et écritures des angles.
     *
     * @param  pattern Patron à utiliser pour l'écriture des angles.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public AngleFormat(final String pattern) throws IllegalArgumentException
    {this(pattern, new DecimalFormatSymbols());}

    /**
     * Construit un objet qui effectuera les lectures et écritures des angles.
     *
     * @param  pattern Patron à utiliser pour l'écriture des angles.
     * @param  locale  Pays dont on voudra utiliser les conventions.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public AngleFormat(final String pattern, final Locale locale) throws IllegalArgumentException
    {this(pattern, new DecimalFormatSymbols(locale));}

    /**
     * Construit un objet qui effectuera les lectures et écritures des angles.
     *
     * @param pattern Patron à utiliser pour l'écriture des angles.
     * @param symbols Symboles à utiliser pour représenter les nombres.
     */
    public AngleFormat(final String pattern, final DecimalFormatSymbols symbols)
    {
        // NOTE: pour cette routine, il ne faut PAS que DecimalFormat
        //       reconnaisse la notation exponentielle, parce que ça
        //       risquerait d'être confondu avec le "E" de "Est".
        numberFormat=new DecimalFormat("#0", symbols);
        applyPattern(pattern);
    }

    /**
     * Spécifie un patron pour le format d'écriture des angles. Le format est spécifié
     * par une chaîne de caractères dans laquelle "D" représente les dégrés, "M" les
     * minutes et "S" les secondes. Les lettres minuscules "d", "m" ou "s" représentent
     * la partie fractionnaire de leur équivalent majuscule. Exemples:
     *
     * <pre>
     * &nbsp;   Spécification de format       Résultat
     * &nbsp;   -----------------------       ---------
     * &nbsp;     "DD°MM'SS\""                48°30'00"
     * &nbsp;     "DD°MM'"                    48°30'
     * &nbsp;     "DD.ddd"                    48.500
     * &nbsp;     "DDMM"                      4830
     * &nbsp;     "DDMMSS"                    483000
     * </pre>
     *
     * @param  Patron à utiliser pour les écritures des angles.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public synchronized void applyPattern(final String pattern) throws IllegalArgumentException
    {
        widthDecimal=0;
        decimalSeparator=true;
        int startPrefix=0;
        int symbolIndex=0;
        boolean parseFinished=false;
        final int length=pattern.length();
        for (int i=0; i<length; i++)
        {
            /*
             * On examine un à un tous les caractères du patron en
             * sautant ceux qui ne sont pas réservés ("D", "M", "S"
             * et leur équivalents en minuscules). Les caractères
             * non-reservés seront mémorisés comme suffix plus tard.
             */
            final char c=pattern.charAt(i);
            final char upperCaseC=Character.toUpperCase(c);
            for (int field=0; field<SYMBOLS.length; field++)
            {
                if (upperCaseC==SYMBOLS[field])
                {
                    /*
                     * Un caractère réservé a été trouvé. Vérifie maintenant
                     * s'il est valide. Par exemple il serait illegal d'avoir
                     * comme patron "MM.mm" sans qu'il soit précédé des degrés.
                     * On attend les lettres "D", "M" et "S" dans l'ordre. Si
                     * le caractère est en lettres minuscules, il doit être le
                     * même que le dernier code (par exemple "DD.mm" est illegal).
                     */
                    if (c==upperCaseC)
                    {
                        symbolIndex++;
                    }
                    if (field!=symbolIndex-1 || parseFinished)
                    {
                        setWidth(0, 1);
                        setSuffix(-1, null);
                        widthDecimal=0;
                        decimalSeparator=true;
                        throw new IllegalArgumentException(Resources.format(Clé.BAD_ANGLE_PATTERN¤1, pattern));
                    }
                    if (c==upperCaseC)
                    {
                        /*
                         * Mémorise les caractères qui précédaient ce code comme suffix
                         * du champs précédent. Puis on contera le nombre de fois que le
                         * code se répète, en mémorisera cette information comme largeur
                         * de ce champs.
                         */
                        setSuffix(field-1, (i>startPrefix) ? pattern.substring(startPrefix, i) : null);
                        int w=1; while (++i<length && pattern.charAt(i)==c) w++;
                        setWidth(field, w);
                    }
                    else
                    {
                        /*
                         * Si le caractère est une minuscule, ce qui le précédait sera le
                         * séparateur décimal plutôt qu'un suffix. On comptera le nombre
                         * d'occurences du caractères pour obtenir la précision.
                         */
                        switch (i-startPrefix)
                        {
                                case 0: decimalSeparator=false; break;
                                case 1: if (pattern.charAt(startPrefix)=='.')
                                        {
                                            decimalSeparator=true;
                                            break;
                                        }
                                default: throw new IllegalArgumentException(Resources.format(Clé.BAD_ANGLE_PATTERN¤1, pattern));
                        }
                        int w=1; while (++i<length && pattern.charAt(i)==c) w++;
                        widthDecimal=w;
                        parseFinished=true;
                    }
                    startPrefix = i--;
                    break; // Break 'j' and continue 'i'.
                }
            }
        }
        setSuffix(symbolIndex-1, (startPrefix<length) ? pattern.substring(startPrefix) : null);
    }

    /**
     * Renvoie le patron utilisé pour les écritures des angles.
     * La méthode {@link #applyPattern} décrit la façon dont le
     * patron est interprété par les objets <code>AngleFormat</code>.
     */
    public synchronized String toPattern()
    {
        char symbol='#';
        final StringBuffer buffer=new StringBuffer();
        for (int field=0; field<=SYMBOLS.length; field++)
        {
            final String previousSuffix=getSuffix(field-1);
            int w=getWidth(field);
            if (w>0)
            {
                /*
                 * Procède à l'écriture de la partie entière des degrés,
                 * minutes ou secondes. Le suffix du champs précédent
                 * sera écrit avant les degrés, minutes ou secondes.
                 */
                if (previousSuffix!=null)
                    buffer.append(previousSuffix);
                symbol=SYMBOLS[field];
                do buffer.append(symbol);
                while (--w>0);
            }
            else
            {
                /*
                 * Procède à l'écriture de la partie décimale des
                 * degrés, minutes ou secondes. Le suffix du ce
                 * champs sera écrit après cette partie fractionnaire.
                 */
                w=widthDecimal;
                if (w>0)
                {
                    if (decimalSeparator) buffer.append('.');
                    symbol=Character.toLowerCase(symbol);
                    do buffer.append(symbol);
                    while (--w>0);
                }
                if (previousSuffix!=null)
                    buffer.append(previousSuffix);
                break;
            }
        }
        return buffer.toString();
    }

    /**
     * Ecrit un angle dans une chaîne de caractères.
     * L'angle sera formaté en utilisant comme modèle
     * le patron spécifié lors du dernier appel de la
     * méthode {@link #applyPattern}.
     *
     * @param  angle Angle à écrire, en degrés.
     * @return Chaîne de caractères représentant cet angle.
     */
    public final String format(final double angle)
    {return format(angle, new StringBuffer(), null).toString();}
    
    /**
     * Procède à l'écriture d'un angle.
     * L'angle sera formaté en utilisant comme modèle
     * le patron spécifié lors du dernier appel de la
     * méthode {@link #applyPattern}.
     *
     * @param  angle      Angle à écrire, en degrés.
     * @param  toAppendTo Buffer dans lequel écrire l'angle.
     * @param  pos        En entré, le code du champs dont on désire les index
     *                    ({@link #DEGREES_FIELD},
     *                     {@link #MINUTES_FIELD} ou
     *                     {@link #SECONDS_FIELD}).
     *                    En sortie, les index du champs demandé. Ce paramètre
     *                    peut être nul si cette information n'est pas désirée.
     *
     * @return Le buffer <code>toAppendTo</code> par commodité.
     */
    public synchronized StringBuffer format(final double angle, StringBuffer toAppendTo, final FieldPosition pos)
    {
        double degrés = angle;
        /*
         * Calcule à l'avance les minutes et les secondes. Si les minutes et secondes
         * ne doivent pas être écrits, on mémorisera NaN. Notez que pour extraire les
         * parties entières, on utilise (int) au lieu de 'Math.floor' car (int) arrondie
         * vers 0 (ce qui est le comportement souhaité) alors que 'floor' arrondie vers
         * l'entier inférieur.
         */
        double minutes  = Double.NaN;
        double secondes = Double.NaN;
        if (width1!=0)
        {
            int tmp = (int) degrés; // Arrondie vers 0 même si négatif.
            minutes = Math.abs(degrés-tmp)*60;
            degrés  = tmp;
            if (minutes<0 || minutes>60)
            {
                // Erreur d'arrondissement (parce que l'angle est trop élevé)
                throw new IllegalArgumentException(Resources.format(Clé.ANGLE_OVERFLOW¤1, new Double(angle)));
            }
            if (width2!=0)
            {
                tmp      = (int) minutes; // Arrondie vers 0 même si négatif.
                secondes = (minutes-tmp)*60;
                minutes  = tmp;
                if (secondes<0 || secondes>60)
                {
                    // Erreur d'arrondissement (parce que l'angle est trop élevé)
                    throw new IllegalArgumentException(Resources.format(Clé.ANGLE_OVERFLOW¤1, new Double(angle)));
                }
                /*
                 * On applique maintenant une correction qui tiendra
                 * compte des problèmes d'arrondissements.
                 */
                final double puissance=XMath.pow10(widthDecimal);
                secondes=Math.rint(secondes*puissance)/puissance;
                tmp = (int) (secondes/60);
                secondes -= 60*tmp;
                minutes += tmp;
            }
            else
            {
                final double puissance=XMath.pow10(widthDecimal);
                minutes = Math.rint(minutes*puissance)/puissance;
            }
            tmp = (int) (minutes/60); // Arrondie vers 0 même si négatif.
            minutes -= 60*tmp;
            degrés += tmp;
        }
        /*
         * Les variables 'degrés', 'minutes' et 'secondes' contiennent
         * maintenant les valeurs des champs à écrire, en principe épurés
         * des problèmes d'arrondissements. Procède maintenant à l'écriture
         * de l'angle.
         */
        if (prefix!=null) toAppendTo.append(prefix);
        final int field;
        if (pos!=null)
        {
            field=pos.getField();
            pos.setBeginIndex(0);
            pos.setEndIndex(0);
        }
        else field=0;
                                     toAppendTo=formatField(degrés,   toAppendTo, (field==DEGREES_FIELD ? pos : null), width0, width1==0, suffix0);
        if (!Double.isNaN(minutes))  toAppendTo=formatField(minutes,  toAppendTo, (field==MINUTES_FIELD ? pos : null), width1, width2==0, suffix1);
        if (!Double.isNaN(secondes)) toAppendTo=formatField(secondes, toAppendTo, (field==SECONDS_FIELD ? pos : null), width2, true,      suffix2);
        return toAppendTo;
    }

    /**
     * Procède à l'écriture d'un champ de l'angle.
     *
     * @param value Valeur à écrire.
     * @param toAppendTo Buffer dans lequel écrire le champs.
     * @param pos Objet dans lequel mémoriser les index des premiers
     *        et derniers caractères écrits, ou <code>null</code>
     *        pour ne pas mémoriser ces index.
     * @param w Nombre de minimal caractères de la partie entière.
     * @param last <code>true</code> si ce champs est le dernier,
     *        et qu'il faut donc écrire la partie décimale.
     * @param s Suffix à écrire après le nombre (peut être nul).
     */
    private StringBuffer formatField(double value, StringBuffer toAppendTo, final FieldPosition pos, final int w, final boolean last, final String s)
    {
        final int startPosition=toAppendTo.length();
        if (!last)
        {
            numberFormat.setMinimumIntegerDigits(w);
            numberFormat.setMaximumFractionDigits(0);
            toAppendTo=numberFormat.format(value, toAppendTo, dummy);
        }
        else if (decimalSeparator)
        {
            numberFormat.setMinimumIntegerDigits(w);
            numberFormat.setMinimumFractionDigits(widthDecimal);
            numberFormat.setMaximumFractionDigits(widthDecimal);
            toAppendTo=numberFormat.format(value, toAppendTo, dummy);
        }
        else
        {
            value *= XMath.pow10(widthDecimal);
            numberFormat.setMaximumFractionDigits(0);
            numberFormat.setMinimumIntegerDigits(w+widthDecimal);
            toAppendTo=numberFormat.format(value, toAppendTo, dummy);
        }
        if (s!=null) toAppendTo.append(s);
        if (pos!=null)
        {
            pos.setBeginIndex(startPosition);
            pos.setEndIndex(toAppendTo.length()-1);
        }
        return toAppendTo;
    }

    /**
     * Procède à l'écriture d'un angle, d'une latitude ou d'une longitude. En principe l'objet <code>obj</code> doit
     * être de la classe {@link Angle} ou une d'une classe dérivée. Il peut s'agir notamment d'un objet de la classe
     * {@link Latitude} ou {@link Longitude}.  Dans ce cas, le symbole approprié ("N", "S", "E" ou "W") sera ajoutée
     * à la suite de l'angle.
     * <br>
     * <br>
     * Bien qu'en principe ce n'est pas le rôle de cette classe, cette méthode acceptera aussi des objets de la classe
     * {@link Number}. Ils seront alors écrits comme des nombres ordinaires. Cette aptitude est utile pour écrire par
     * exemple une profondeur après avoir écrit les angles d'une coordonnée géographique, mais en utilisant les mêmes
     * symboles décimaux.
     *
     * @param  angle      Angle ou nombre à écrire.
     * @param  toAppendTo Buffer dans lequel écrire l'angle.
     * @param  pos        En entré, le code du champs dont on désire les index
     *                    ({@link #DEGREES_FIELD},
     *                     {@link #MINUTES_FIELD},
     *                     {@link #SECONDS_FIELD} ou
     *                     {@link #HEMISPHERE_FIELD}).
     *                    En sortie, les index du champs demandé. Ce paramètre
     *                    peut être nul si cette information n'est pas désirée.
     *
     * @return Le buffer <code>toAppendTo</code> par commodité.
     * @throws IllegalArgumentException si <code>obj</code> n'est pas de la classe {@link Angle} ou {@link Number}.
     */
    public synchronized StringBuffer format(final Object obj, StringBuffer toAppendTo, final FieldPosition pos) throws IllegalArgumentException
    {
        if (obj instanceof Latitude ) return format( ((Latitude) obj).degrees(), toAppendTo, pos, NORTH, SOUTH);
        if (obj instanceof Longitude) return format(((Longitude) obj).degrees(), toAppendTo, pos, EAST,  WEST );
        if (obj instanceof Angle    ) return format(    ((Angle) obj).degrees(), toAppendTo, pos);
        if (obj instanceof Number)
        {
            numberFormat.setMinimumIntegerDigits (1);
            numberFormat.setMinimumFractionDigits(0);
            numberFormat.setMaximumFractionDigits(2);
            return numberFormat.format(obj, toAppendTo, (pos!=null) ? pos : dummy);
        }
        throw new IllegalArgumentException(Resources.format(Clé.NOT_AN_ANGLE_OBJECT¤2, new Integer(0), XClass.getShortClassName(obj)));
    }

    /**
     * Procède à l'écriture d'un angle, d'une latitude ou d'une longitude.
     *
     * @param  angle      Angle ou nombre à écrire.
     * @param  type       Type de l'angle ou du nombre:
     *                    {@link #LONGITUDE},
     *                    {@link #LATITUDE} ou
     *                    {@link #ALTITUDE}.
     * @param  toAppendTo Buffer dans lequel écrire l'angle.
     * @param  pos        En entré, le code du champs dont on désire les index
     *                    ({@link #DEGREES_FIELD},
     *                     {@link #MINUTES_FIELD},
     *                     {@link #SECONDS_FIELD} ou
     *                     {@link #HEMISPHERE_FIELD}).
     *                    En sortie, les index du champs demandé. Ce paramètre
     *                    peut être nul si cette information n'est pas désirée.
     *
     * @return Le buffer <code>toAppendTo</code> par commodité.
     */
    synchronized StringBuffer format(final double number, final int type, StringBuffer toAppendTo, final FieldPosition pos)
    {
        switch (type)
        {
            default:        throw new IllegalArgumentException(Integer.toString(type)); // Should not happen.
            case LATITUDE:  return format(number, toAppendTo, pos, NORTH, SOUTH);
            case LONGITUDE: return format(number, toAppendTo, pos, EAST,  WEST );
            case ALTITUDE:
            {
                numberFormat.setMinimumIntegerDigits (1);
                numberFormat.setMinimumFractionDigits(0);
                numberFormat.setMaximumFractionDigits(2);
                return numberFormat.format(number, toAppendTo, (pos!=null) ? pos : dummy);
            }
        }
    }

    /**
     * Procède à l'écriture d'un angle suivit d'un suffix 'N','S','E' ou 'W'.
     * L'angle sera formaté en utilisant comme modèle le patron spécifié lors
     * du dernier appel de la méthode {@link #applyPattern}.
     *
     * @param  angle      Angle à écrire, en degrés.
     * @param  toAppendTo Buffer dans lequel écrire l'angle.
     * @param  pos        En entré, le code du champs dont on désire les index
     *                    ({@link #DEGREES_FIELD},
     *                     {@link #MINUTES_FIELD},
     *                     {@link #SECONDS_FIELD} ou
     *                     {@link #HEMISPHERE_FIELD}).
     *                    En sortie, les index du champs demandé. Ce paramètre
     *                    peut être nul si cette information n'est pas désirée.
     * @param north       Caractères à écrire si l'angle est positif ou nul.
     * @param south       Caractères à écrire si l'angle est négatif.
     *
     * @return Le buffer <code>toAppendTo</code> par commodité.
     */
    private StringBuffer format(final double angle, StringBuffer toAppendTo, final FieldPosition pos, final char north, final char south)
    {
        toAppendTo=format(Math.abs(angle), toAppendTo, pos);
        final int start=toAppendTo.length();
        toAppendTo.append(angle<0 ? south : north);
        if (pos!=null && pos.getField()==HEMISPHERE_FIELD)
        {
            pos.setBeginIndex(start);
            pos.setEndIndex(toAppendTo.length()-1);
        }
        return toAppendTo;
    }
    
    /**
     * Ignore le suffix d'un nombre. Cette méthode est appellée par la méthode
     * {@link #parse} pour savoir quel champs il vient de lire. Par exemple si
     * l'on vient de lire les degrés dans "48°12'", alors cette méthode extraira
     * le "°" et retournera 0 pour indiquer que l'on vient de lire des degrés.
     *
     * Cette méthode se chargera d'ignorer les espaces qui précèdent le suffix.
     * Elle tentera ensuite de d'abord interpréter le suffix selon les symboles
     * du patron (spécifié avec {@link #applyPattern}. Si le suffix n'a pas été
     * reconnus, elle tentera ensuite de le comparer aux symboles standards
     * (° ' ").
     *
     * @param  source Chaîne dans laquelle on doit sauter le suffix.
     * @param  pos En entré, l'index du premier caractère à considérer dans la
     *         chaîne <code>pos</code>. En sortie, l'index du premier caractère
     *         suivant le suffix (c'est-à-dire index à partir d'où continuer la
     *         lecture après l'appel de cette méthode). Si le suffix n'a pas été
     *         reconnu, alors cette méthode retourne par convention <code>SYMBOLS.length</code>.
     * @param  field Champs à vérifier de préférences. Par exemple la valeur 1 signifie que les
     *         suffix des minutes et des secondes devront être vérifiés avant celui des degrés.
     * @return Le numéro du champs correspondant au suffix qui vient d'être extrait:
     *         -1 pour le préfix de l'angle, 0 pour le suffix des degrés, 1 pour le
     *         suffix des minutes et 2 pour le suffix des secondes. Si le texte n'a
     *         pas été reconnu, retourne <code>SYMBOLS.length</code>.
     */
    private int skipSuffix(final String source, final ParsePosition pos, int field)
    {
        /*
         * Essaie d'abord de sauter les suffix qui
         * avaient été spécifiés dans le patron.
         */
        final int length=source.length();
        int start=pos.getIndex();
        for (int j=SYMBOLS.length; j>=0; j--) // C'est bien j>=0 et non j>0.
        {
            int index=start;
            final String toSkip=getSuffix(field);
            if (toSkip!=null)
            {
                final int toSkipLength=toSkip.length();
                do if (source.regionMatches(index, toSkip, 0, toSkipLength))
                {
                    pos.setIndex(index+toSkipLength);
                    return field;
                }
                while (index<length && Character.isSpaceChar(source.charAt(index++)));
            }
            if (++field >= SYMBOLS.length) field=-1;
        }
        /*
         * Le texte trouvé ne correspondant à aucun suffix du patron,
         * essaie maintenant de sauter un des suffix standards (après
         * avoir ignoré les espaces qui le précédaient).
         */
        char c;
        do if (start>=length) return SYMBOLS.length;
        while (Character.isSpaceChar(c=source.charAt(start++)));
        switch (c)
        {
            case '°' : pos.setIndex(start); return 0; // Degrés.
            case '\'': pos.setIndex(start); return 1; // Minutes
            case '"' : pos.setIndex(start); return 2; // Secondes
            default  : return SYMBOLS.length;         // Inconnu.
        }
    }

    /**
     * Interprète une chaîne de caractères représentant un angle. Les règles d'interprétation de
     * cette méthode sont assez souples. Par exemple cettte méthode interprétera correctement la
     * chaîne "48°12.34'" même si le patron attendu était "DDMM.mm" (c'est-à-dire que la chaîne
     * aurait du être "4812.34"). Les espaces entre les degrés, minutes et secondes sont acceptés.
     * Si l'angle est suivit d'un symbole "N" ou "S", alors l'objet retourné sera de la classe
     * {@link Latitude}. S'il est plutot suivit d'un symbole "E" ou "W", alors l'objet retourné
     * sera de la classe {@link Longitude}. Sinon, il sera de la classe {@link Angle}.
     *
     * @param source Chaîne de caractères à lire.
     * @param pos    Position à partir d'où interpréter la chaîne.
     * @return       L'angle lu.
     */
    public Angle parse(final String source, final ParsePosition pos)
    {
        final Angle ang=parse(source, pos, false);
        return ang;
    }

    /**
     * Interprète une chaîne de caractères représentant un angle. Les règles d'interprétation de
     * cette méthode sont assez souples. Par exemple cettte méthode interprétera correctement la
     * chaîne "48°12.34'" même si le patron attendu était "DDMM.mm" (c'est-à-dire que la chaîne
     * aurait du être "4812.34"). Les espaces entre les degrés, minutes et secondes sont acceptés.
     * Si l'angle est suivit d'un symbole "N" ou "S", alors l'objet retourné sera de la classe
     * {@link Latitude}. S'il est plutot suivit d'un symbole "E" ou "W", alors l'objet retourné
     * sera de la classe {@link Longitude}. Sinon, il sera de la classe {@link Angle}.
     *
     * @param source           Chaîne de caractères à lire.
     * @param pos              Position à partir d'où interpréter la chaîne.
     * @param spaceAsSeparator Indique si l'espace est accepté comme séparateur à l'intérieur d'un angle. La
     *                         valeur <code>true</code> fait que l'angle "45 30" sera interprété comme "45°30".
     * @return L'angle lu.
     */
    private synchronized Angle parse(final String source, final ParsePosition pos, final boolean spaceAsSeparator)
    {
        double degrés   = Double.NaN;
        double minutes  = Double.NaN;
        double secondes = Double.NaN;
        final int length=source.length();
        ///////////////////////////////////////////////////////////////////////////////
        // BLOC A: Analyse la chaîne de caractères 'source' et affecte aux variables //
        //         'degrés', 'minutes' et 'secondes' les valeurs appropriées.        //
        //         Les premières accolades ne servent qu'à garder locales            //
        //         les variables sans intérêt une fois la lecture terminée.          //
        ///////////////////////////////////////////////////////////////////////////////
        {
            /*
             * Extrait le préfix, s'il y en avait un. Si on tombe sur un symbole des
             * degrés, minutes ou secondes alors qu'on n'a pas encore lu de nombre,
             * on considèrera que la lecture a échouée.
             */
            final int indexStart=pos.getIndex();
            int index=skipSuffix(source, pos, -1); // -1==préfix
            if (index>=0 && index<SYMBOLS.length)
            {
                pos.setErrorIndex(indexStart);
                pos.setIndex(indexStart);
                return null;
            }
            /*
             * Saute les espaces blancs qui
             * précèdent le champs des degrés.
             */
            index=pos.getIndex();
            while (index<length && Character.isSpaceChar(source.charAt(index))) index++;
            pos.setIndex(index);
            /*
             * Lit les degrés. Notez que si aucun séparateur ne séparait les degrés
             * des minutes des secondes, alors cette lecture pourra inclure plusieurs
             * champs (exemple: "DDDMMmmm"). La séparation sera faite plus tard.
             */
            Number fieldObject=numberFormat.parse(source, pos);
            if (fieldObject==null)
            {
                pos.setIndex(indexStart);
                if (pos.getErrorIndex()<indexStart)
                    pos.setErrorIndex(index);
                return null;
            }
            degrés=fieldObject.doubleValue();
            int indexEndField=pos.getIndex();
            boolean swapDM=true;
BigBoss:    switch (skipSuffix(source, pos, 0)) // 0==DEGRÉS
            {
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉS DEGRÉS
                 * ----------------------------------------------
                 * Les degrés étaient suivit du préfix d'un autre angle. Le préfix sera donc
                 * retourné dans le buffer pour un éventuel traitement par le prochain appel
                 * à la méthode 'parse' et on n'ira pas plus loin dans l'analyse de la chaîne.
                 */
                case -1: // -1==PREFIX
                {
                    pos.setIndex(indexEndField);
                    break BigBoss;
                }
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉS DEGRÉS
                 * ----------------------------------------------
                 * On a trouvé le symbole des secondes au lieu de celui des degrés. On fait
                 * la correction dans les variables 'degrés' et 'secondes' et on considère
                 * que la lecture est terminée.
                 */
                case 2: // 2==SECONDES
                {
                    secondes = degrés;
                    degrés = Double.NaN;
                    break BigBoss;
                }
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉS DEGRÉS
                 * ----------------------------------------------
                 * Aucun symbole ne suit les degrés. Des minutes sont-elles attendues?
                 * Si oui, on fera comme si le symbole des degrés avait été là. Sinon,
                 * on considèrera que la lecture est terminée.
                 */
                default:
                {
                    if (width1==0)         break BigBoss;
                    if (!spaceAsSeparator) break BigBoss;
                    // fall through
                }
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉS DEGRÉS
                 * ----------------------------------------------
                 * Un symbole des degrés a été explicitement trouvé. Les degrés sont peut-être
                 * suivit des minutes. On procèdera donc à la lecture du prochain nombre, puis
                 * à l'analyse du symbole qui le suit.
                 */
                case 0: // 0==DEGRÉS
                {
                    final int indexStartField = index = pos.getIndex();
                    while (index<length && Character.isSpaceChar(source.charAt(index))) index++;
                    if (!spaceAsSeparator && index!=indexStartField) break BigBoss;
                    pos.setIndex(index);
                    fieldObject=numberFormat.parse(source, pos);
                    if (fieldObject==null)
                    {
                        pos.setIndex(indexStartField);
                        break BigBoss;
                    }
                    indexEndField = pos.getIndex();
                    minutes = fieldObject.doubleValue();
                    switch (skipSuffix(source, pos, (width1!=0) ? 1 : -1))
                    {
                        /* ------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉES MINUTES
                         * ------------------------------------------------
                         * Le symbole trouvé est bel et bien celui des minutes.
                         * On continuera le bloc pour tenter de lire les secondes.
                         */
                        case 1: // 1==MINUTES
                        {
                            break; // continue outer switch
                        }
                        /* ------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉES MINUTES
                         * ------------------------------------------------
                         * Un symbole des secondes a été trouvé au lieu du symbole des minutes
                         * attendu. On fera la modification dans les variables 'secondes' et
                         * 'minutes' et on considèrera la lecture terminée.
                         */
                        case 2: // 2==SECONDES
                        {
                            secondes = minutes;
                            minutes = Double.NaN;
                            break BigBoss;
                        }
                        /* ------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉES MINUTES
                         * ------------------------------------------------
                         * Aucun symbole n'a été trouvé. Les minutes étaient-elles attendues?
                         * Si oui, on les acceptera et on tentera de lire les secondes. Si non,
                         * on retourne le texte lu dans le buffer et on termine la lecture.
                         */
                        default:
                        {
                            if (width1!=0) break; // Continue outer switch
                            // fall through
                        }
                        /* ------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉES MINUTES
                         * ------------------------------------------------
                         * Au lieu des minutes, le symbole lu est celui des degrés. On considère
                         * qu'il appartient au prochain angle. On retournera donc le texte lu dans
                         * le buffer et on terminera la lecture.
                         */
                        case 0:
                        {
                            pos.setIndex(indexStartField);
                            minutes=Double.NaN;
                            break BigBoss;
                        }
                        /* ------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉES MINUTES
                         * ------------------------------------------------
                         * Après les minutes (qu'on accepte), on a trouvé le préfix du prochain
                         * angle à lire. On retourne ce préfix dans le buffer et on considère la
                         * lecture terminée.
                         */
                        case -1: // -1==PRÉFIX
                        {
                            pos.setIndex(indexEndField);
                            break BigBoss;
                        }
                    }
                    swapDM=false;
                    // fall through
                }
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉS DEGRÉS
                 * ----------------------------------------------
                 * Un symbole des minutes a été trouvé au lieu du symbole des degrés attendu.
                 * On fera donc la modification dans les variables 'degrés' et 'minutes'. Ces
                 * minutes sont peut-être suivies des secondes. On tentera donc de lire le
                 * prochain nombre.
                 */
                case 1: // 1==MINUTES
                {
                    if (swapDM)
                    {
                        minutes = degrés;
                        degrés = Double.NaN;
                    }
                    final int indexStartField = index = pos.getIndex();
                    while (index<length && Character.isSpaceChar(source.charAt(index))) index++;
                    if (!spaceAsSeparator && index!=indexStartField) break BigBoss;
                    pos.setIndex(index);
                    fieldObject=numberFormat.parse(source, pos);
                    if (fieldObject==null)
                    {
                        pos.setIndex(indexStartField);
                        break;
                    }
                    indexEndField = pos.getIndex();
                    secondes = fieldObject.doubleValue();
                    switch (skipSuffix(source, pos, (width2!=0) ? 2 : -1))
                    {
                        /* -------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉES SECONDES
                         * -------------------------------------------------
                         * Un symbole des secondes explicite a été trouvée.
                         * La lecture est donc terminée.
                         */
                        case 2: // 2==SECONDES
                        {
                            break;
                        }
                        /* -------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉES SECONDES
                         * -------------------------------------------------
                         * Aucun symbole n'a été trouvée. Attendait-on des secondes? Si oui, les
                         * secondes seront acceptées. Sinon, elles seront retournées au buffer.
                         */
                        default:
                        {
                            if (width2!=0) break;
                            // fall through
                        }
                        /* -------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉES SECONDES
                         * -------------------------------------------------
                         * Au lieu des degrés, on a trouvé un symbole des minutes ou des
                         * secondes. On renvoie donc le nombre et son symbole dans le buffer.
                         */
                        case 1: // 1==MINUTES
                        case 0: // 0==DEGRÉS
                        {
                            pos.setIndex(indexStartField);
                            secondes=Double.NaN;
                            break;
                        }
                        /* -------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PRÉSUMÉES SECONDES
                         * -------------------------------------------------
                         * Après les secondes (qu'on accepte), on a trouvé le préfix du prochain
                         * angle à lire. On retourne ce préfix dans le buffer et on considère la
                         * lecture terminée.
                         */
                        case -1: // -1==SECONDES
                        {
                            pos.setIndex(indexEndField);
                            break BigBoss;
                        }
                    }
                    break;
                }
            }
        }
        ////////////////////////////////////////////////////////////////////
        // BLOC B: Prend en compte l'éventualité ou le séparateur décimal //
        //         aurrait été absent, puis calcule l'angle en degrés.    //
        ////////////////////////////////////////////////////////////////////
        if (minutes<0)
        {
            secondes = -secondes;
        }
        if (degrés<0)
        {
            minutes = -minutes;
            secondes = -secondes;
        }
        if (!decimalSeparator)
        {
            final double facteur=XMath.pow10(widthDecimal);
            if (width2!=0)
            {
                if (suffix1==null && Double.isNaN(secondes))
                {
                    if (suffix0==null && Double.isNaN(minutes))
                    {
                        degrés /= facteur;
                    }
                    else minutes /= facteur;
                }
                else secondes /= facteur;
            }
            else if (Double.isNaN(secondes))
            {
                if (width1!=0)
                {
                    if (suffix0==null && Double.isNaN(minutes))
                    {
                        degrés /= facteur;
                    }
                    else minutes /= facteur;
                }
                else if (Double.isNaN(minutes))
                {
                    degrés /= facteur;
                }
            }
        }
        /*
         * S'il n'y a rien qui permet de séparer les degrés des minutes (par exemple si
         * le patron est "DDDMMmmm"), alors la variable 'degrés' englobe à la fois les
         * degrés, les minutes et d'éventuelles secondes. On applique une correction ici.
         */
        if (suffix1==null && width2!=0 && Double.isNaN(secondes))
        {
            double facteur = XMath.pow10(width2);
            if (suffix0==null && width1!=0 && Double.isNaN(minutes))
            {
                ///////////////////
                //// DDDMMSS.s ////
                ///////////////////
                secondes = degrés;
                minutes  = (int) (degrés/facteur); // Arrondie vers 0
                secondes -= minutes*facteur;
                facteur  = XMath.pow10(width1);
                degrés   = (int) (minutes/facteur); // Arrondie vers 0
                minutes -= degrés*facteur;
            }
            else
            {
                ////////////////////
                //// DDD°MMSS.s ////
                ////////////////////
                secondes = minutes;
                minutes = (int) (minutes/facteur); // Arrondie vers 0
                secondes -= minutes*facteur;
            }
        }
        else if (suffix0==null && width1!=0 && Double.isNaN(minutes))
        {
            /////////////////
            //// DDDMM.m ////
            /////////////////
            final double facteur = XMath.pow10(width1);
            minutes = degrés;
            degrés = (int) (degrés/facteur); // Arrondie vers 0
            minutes -= degrés*facteur;
        }
        pos.setErrorIndex(-1);
        if ( Double.isNaN(degrés))   degrés=0;
        if (!Double.isNaN(minutes))  degrés += minutes/60;
        if (!Double.isNaN(secondes)) degrés += secondes/3600;
        /////////////////////////////////////////////////////
        // BLOC C: Vérifie maintenant si l'angle ne serait //
        //         pas suivit d'un symbole N, S, E ou W.   //
        /////////////////////////////////////////////////////
        for (int index=pos.getIndex(); index<length; index++)
        {
            final char c=source.charAt(index);
            switch (Character.toUpperCase(c))
            {
                case NORTH: pos.setIndex(index+1); return new Latitude ( degrés);
                case SOUTH: pos.setIndex(index+1); return new Latitude (-degrés);
                case EAST : pos.setIndex(index+1); return new Longitude( degrés);
                case WEST : pos.setIndex(index+1); return new Longitude(-degrés);
            }
            if (!Character.isSpaceChar(c)) break;
        }
        return new Angle(degrés);
    }

    /**
     * Interprète une chaîne de caractères représentant un angle.
     *
     * @param  source Chaîne de caractères à interpréter.
     * @return L'angle comme objet {@link Angle}, {@link Latitude} ou {@link Longitude}.
     * @throws ParseException si la chaîne n'a pas été complètement reconnue.
     */
    public Angle parse(final String source) throws ParseException
    {
        final ParsePosition pos = new ParsePosition(0);
        final Angle         ang = parse(source, pos, true);
        checkComplete(source, pos, false);
        return ang;
    }

    /**
     * Interprète une chaîne de caractères représentant un angle.
     *
     * @param  source Chaîne de caractères à interpréter.
     * @param  pos Position à partir d'où commencer l'interprétation de la chaîne <code>source</code>.
     * @return L'angle comme objet {@link Angle}, {@link Latitude} ou {@link Longitude}.
     */
    public Object parseObject(final String source, final ParsePosition pos)
    {return parse(source, pos);}

    /**
     * Interprète une chaîne de caractères représentant un angle. Cette méthode
     * est redéfinie afin d'obtenir un message d'erreur plus explicite que celui
     * de {@link Format#parseObject} en cas d'erreur.
     *
     * @param  source Chaîne de caractères à interpréter.
     * @return L'angle comme objet {@link Angle}, {@link Latitude} ou {@link Longitude}.
     * @throws ParseException si la chaîne n'a pas été complètement reconnue.
     */
    public Object parseObject(final String source) throws ParseException
    {return parse(source);}

    /**
     * Interprète une chaîne de caractères qui devrait représenter un nombre.
     * Cette méthode est utile pour lire une altitude après les angles.
     *
     * @param  source Chaîne de caractères à interpréter.
     * @param  pos Position à partir d'où commencer l'interprétation de la chaîne <code>source</code>.
     * @return Le nombre lu comme objet {@link Number}.
     */
    final Number parseNumber(final String source, final ParsePosition pos)
    {return numberFormat.parse(source, pos);}

    /**
     * Vérifie si l'interprétation d'une chaîne de caractères a été complète. Si ce n'était pas le
     * cas, lance une exception avec un message d'erreur soulignant les caractères problématiques.
     *
     * @param  source Chaîne de caractères qui était à interpréter.
     * @param  pos Position à laquelle s'est terminée l'interprétation de la chaîne <code>source</code>.
     * @param  isCoordinate <code>false</code> si on interprétait un angle, ou <code>true</code> si on
     *         interprétait une coordonnée.
     * @throws ParseException Si la chaîne <code>source</code> n'a pas été interprétée dans sa totalité.
     */
    static void checkComplete(final String source, final ParsePosition pos, final boolean isCoordinate) throws ParseException
    {
        final int length=source.length();
        final int origin=pos.getIndex();
        for (int index=origin; index<length; index++)
        {
            if (!Character.isWhitespace(source.charAt(index)))
            {
                final Integer flag=new Integer(isCoordinate ? 1 : 0);
                index=pos.getErrorIndex(); if (index<0) index=origin;
                int lower=index; while (lower<length &&  Character.isWhitespace(source.charAt(lower))) lower++;
                int upper=lower; while (upper<length && !Character.isWhitespace(source.charAt(upper))) upper++;
                throw new ParseException(Resources.format(Clé.PARSE_EXCEPTION_ANGLE¤3, flag, source, source.substring(lower, Math.min(lower+10, upper))), index);
            }
        }
    }
    
    /**
     * Renvoie un code "hash
     * value" pour cet objet.
     */
    public synchronized int hashCode()
    {
        int c=78236951;
        if (decimalSeparator)        c^= 0xFF;
        if (prefix           !=null) c^=        prefix.hashCode();
        if (suffix0          !=null) c = c*37 + suffix0.hashCode();
        if (suffix1          !=null) c^= c*37 + suffix1.hashCode();
        if (suffix2          !=null) c^= c*37 + suffix2.hashCode();
        return c ^ (((((width0 << 8) ^ width1) << 8) ^ width2) << 8) ^ widthDecimal;
    }
    
    /**
     * Vérifie si ce format est égal au format <code>obj</code> spécifié.
     * Ce sera le cas si les deux formats sont de la même classe et
     * utilisent le même patron ainsi que les mêmes symboles décimaux.
     */
    public synchronized boolean equals(final Object obj)
    {
        // On ne peut pas synchroniser "obj" si on ne veut
        // pas risquer un "deadlock". Voir RFE #4210659.
        if (obj==this) return true;
        if (obj!=null && getClass().equals(obj.getClass()))
        {
            final  AngleFormat cast = (AngleFormat) obj;
            return width0           == cast.width0            &&
                   width1           == cast.width1            &&
                   width2           == cast.width2            &&
                   widthDecimal     == cast.widthDecimal      &&
                   decimalSeparator == cast.decimalSeparator  &&
                   equals(prefix,      cast.prefix )          &&
                   equals(suffix0,     cast.suffix0)          &&
                   equals(suffix1,     cast.suffix1)          &&
                   equals(suffix2,     cast.suffix2)          &&
                   equals(numberFormat.getDecimalFormatSymbols(), cast.numberFormat.getDecimalFormatSymbols());
        }
        else return false;
    }
    
    /**
     * Vérifie l'égalité de deux objets qui peuvent être nuls.
     */
    static boolean equals(final Object o1, final Object o2)
    {return (o1==o2 || (o1!=null && o1.equals(o2)));}
    
    /**
     * Renvoie une copie de ce format.
     */
    public AngleFormat clone()
    {return (AngleFormat) super.clone();}

    /**
     * Renvoie une représentation de cet objet. Cette
     * méthode n'est utile qu'à des fins de déboguage.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+toPattern()+']';}




    /**
     * A <code>SpinnerModel</code> for sequences of angles.
     * This model work like {@link javax.swing.SpinnerNumberModel}.
     *
     * @see JSpinner
     * @see SpinnerNumberModel
     *
     * @version 1.0
     * @author Adapted from Hans Muller
     * @author Martin Desruisseaux
     */
    public static class SpinnerModel extends AbstractSpinnerModel implements Serializable
    {
        /**
         * The current value.
         */
        private Angle value;

        /**
         * The minimum and maximum values.
         */
        private double minimum, maximum;

        /**
         * The step size.
         */
        private double stepSize = 1;

        /**
         * Constructs a <code>SpinnerModel</code> that represents a closed sequence
         * of angles. Initial minimum and maximum values are choosen according the
         * <code>value</code> type:
         *
         * <table>
         *   <tr><td>{@link Longitude}&nbsp;</td> <td>-180° to 180°</td></tr>
         *   <tr><td>{@link Latitude}&nbsp;</td>  <td>-90° to 90°</td>  </tr>
         *   <tr><td>{@link Angle}&nbsp;</td>     <td>0° to 360°</td>   </tr>
         * </table>
         *
         * @param value the current (non <code>null</code>) value of the model
         */
        public SpinnerModel(final Angle value)
        {
            this.value = value;
            if (value instanceof Longitude)
            {
                minimum  = Longitude.MIN_VALUE;
                maximum  = Longitude.MAX_VALUE;
            }
            else if (value instanceof Latitude)
            {
                minimum  = Latitude.MIN_VALUE;
                maximum  = Latitude.MAX_VALUE;
            }
            else if (value!=null)
            {
                minimum = 0;
                maximum = 360;
            }
            else throw new IllegalArgumentException();
        }

        /**
         * Changes the lower bound for angles in this sequence.
         */
        public void setMinimum(final double minimum)
        {
            if (this.minimum != minimum)
            {
                this.minimum = minimum;
                fireStateChanged();
            }
        }

        /**
         * Returns the first angle in this sequence.
         */
        public double getMinimum()
        {return minimum;}

        /**
         * Changes the upper bound for angles in this sequence.
         */
        public void setMaximum(final double maximum)
        {
            if (this.maximum != maximum)
            {
                this.maximum = maximum;
                fireStateChanged();
            }
        }

        /**
         * Returns the last angle in the sequence.
         */
        public double getMaximum()
        {return maximum;}

        /**
         * Changes the size of the value change computed by the
         * <code>getNextValue</code> and <code>getPreviousValue</code>
         * methods.
         */
        public void setStepSize(final double stepSize)
        {
            if (this.stepSize != stepSize)
            {
                this.stepSize = stepSize;
                fireStateChanged();
            }
        }

        /**
         * Returns the size of the value change computed by the
         * <code>getNextValue</code> and <code>getPreviousValue</code> methods.
         */
        public double getStepSize()
        {return stepSize;}

        /**
         * Wrap the specified value into an {@link Angle} object.
         */
        final Angle toAngle(final double newValue)
        {
            if (value instanceof Longitude) return new Longitude(newValue);
            if (value instanceof  Latitude) return new  Latitude(newValue);
            return new Angle(newValue);
        }

        /**
         * Returns <code>value+factor*stepSize</code>.
         */
        private Angle getNextValue(final int factor)
        {
            final double newValue = value.degrees() + stepSize*factor;
            if (!(newValue>=minimum && newValue<=maximum)) return null;
            return toAngle(newValue);
        }

        /**
         * Returns the next angle in the sequence.
         */
        public Object getNextValue()
        {return getNextValue(+1);}

        /**
         * Returns the previous angle in the sequence.
         */
        public Object getPreviousValue()
        {return getNextValue(-1);}

        /**
         * Returns the value of the current angle of the sequence.
         */
        public Object getValue()
        {return value;}

        /**
         * Sets the current value for this sequence.
         */
        public void setValue(final Object value)
        {
            if (!(value instanceof Angle))
            {
                throw new IllegalArgumentException(Resources.format(Clé.ILLEGAL_ARGUMENT¤1, value));
            }
            if (!XClass.equals(value, this.value))
            {
                this.value = (Angle)value;
                fireStateChanged();
            }
        }
    }

    /**
     * This subclass of {@link javax.swing.InternationalFormatter} maps the
     * minimum/maximum properties to a {@link AngleFormat.SpinnerModel}.
     *
     * @version 1.0
     * @author Adapted from Hans Muller
     * @author Martin Desruisseaux
     */
    private static class EditorFormatter extends InternationalFormatter
    {
        /**
         * The spinner model.
         */
        private final SpinnerModel model;

        /**
         * Construct a formatter.
         */
        EditorFormatter(final SpinnerModel model, final AngleFormat format)
        {
            super(format);
            this.model = model;
            setAllowsInvalid(true);
            setCommitsOnValidEdit(false);
            setOverwriteMode(false);

            final Class classe;
            final Object value=model.getValue();
            if      (value instanceof Longitude) classe=Longitude.class;
            else if (value instanceof  Latitude) classe=Latitude.class;
            else                                 classe=Angle.class;
            setValueClass(classe);
        }

        /**
         * Returns the {@link Object} representation
         * of the {@link String} <code>text</code>.
         */
        public Object stringToValue(final String text) throws ParseException
        {
            final Object value = super.stringToValue(text);
            if (value instanceof Longitude) return value;
            if (value instanceof  Latitude) return value;
            if (value instanceof     Angle)
            {
                final Class valueClass = getValueClass();
                if (Longitude.class.isAssignableFrom(valueClass))
                    return new Longitude(((Angle)value).degrees());
                if (Latitude.class.isAssignableFrom(valueClass))
                    return new Latitude(((Angle)value).degrees());
            }
            return value;
        }

        /**
         * Sets the minimum value.
         */
        public void setMinimum(final Comparable min)
        {model.setMinimum(((Angle)min).degrees());}

        /**
         * Gets the minimum value.
         */
        public Comparable getMinimum()
        {return model.toAngle(model.getMinimum());}

        /**
         * Sets the maximum value.
         */
        public void setMaximum(final Comparable max)
        {model.setMaximum(((Angle)max).degrees());}

        /**
         * Gets the maximum value.
         */
        public Comparable getMaximum()
        {return model.toAngle(model.getMaximum());}
    }

    /**
     * An editor for a {@link javax.swing.JSpinner}. The value of the editor is
     * displayed with a {@link javax.swing.JFormattedTextField} whose format is
     * defined by a {@link javax.swing.text.InternationalFormatter} instance
     * whose minimum and maximum properties are mapped to the {@link SpinnerNumberModel}.
     *
     * @version 1.0
     * @author Adapted from Hans Muller
     * @author Martin Desruisseaux
     */
    public static class SpinnerEditor extends JSpinner.DefaultEditor
    {
        /**
         * Construct an editor for the specified format.
         */
        public SpinnerEditor(final JSpinner spinner, final AngleFormat format)
        {
            super(spinner);
            final javax.swing.SpinnerModel genericModel = spinner.getModel();
            if (!(genericModel instanceof SpinnerModel))
            {
                throw new IllegalArgumentException();
            }
            final SpinnerModel              model = (SpinnerModel) genericModel;
            final EditorFormatter       formatter = new EditorFormatter(model, format);
            final DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
            final JFormattedTextField       field = getTextField();
            field.setEditable(true);
            field.setFormatterFactory(factory);
            field.setHorizontalAlignment(JFormattedTextField.RIGHT);

            /* TBD - initializing the column width of the text field
             * is imprecise and doing it here is tricky because
             * the developer may configure the formatter later.
             */
            try
            {
                final String maxString = formatter.valueToString(formatter.getMinimum());
                final String minString = formatter.valueToString(formatter.getMaximum());
                field.setColumns(Math.max(maxString.length(), minString.length()));
            }
            catch (ParseException exception)
            {
                // TBD should throw a chained error here
            }
        }
    }
}
