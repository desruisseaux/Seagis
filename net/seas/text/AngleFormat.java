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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
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
 * �crit et interpr�te des angles exprim�s en degr�s selon un certain patron. Cette classe
 * travaille sur des angles m�moris�s dans des objets de la classe {@link Angle}.
 * Les �critures des angles se font selon un patron bien d�fini. Par exemple le patron
 * "<code>D�MM.mm'</code>" �crira des angles comme "<code>48�04.39'</code>".
 * Les symboles autoris�s sont:
 *
 * <blockquote><table>
 *     <tr><td><code>D</code></td><td>&nbsp;&nbsp;D�signe la partie enti�re des degr�s</td></tr>
 *     <tr><td><code>M</code></td><td>&nbsp;&nbsp;D�signe la partie enti�re des minutes</td></tr>
 *     <tr><td><code>S</code></td><td>&nbsp;&nbsp;D�signe la partie enti�re des minutes</td></tr>
 * </table></blockquote><br>
 *
 * Alors que les lettres majuscules (<code>D</code>, <code>M</code> et <code>S</code>)
 * d�signent les parties enti�res, les lettres minuscules d�signent les parties fractionnaires. Ainsi
 * par exemple <code>m</code> d�signe la partie fractionnaire des minutes. Les parties fractionnaires
 * doivent toujours appara�trent en dernier dans le patron, et imm�diatement apr�s leur partie enti�re.
 * Par exemple "<code>D.dd�MM'</code>" et "<code>D.mm</code>" sont tout deux ill�gaux, mais
 * "<code>D.dd�</code>" est l�gal. Les symboles non reconnus dans le patron (par exemple <code>�</code>,
 * <code>'</code> et <code>"</code>) sont �crits tels quels et peuvent appara�tre � n'importe quel position.
 * Ces caract�res s�parateurs peuvent aussi �tre compl�tement absents. Ainsi les champs coll�s sont autoris�s.
 * Par exemple "<code>DDDMMmm</code>" peut lire et �crire "<code>0480439</code>".
 * <br><br>
 * Le nombre de fois qu'est r�p�t� un code indique le nombre minimal de caract�res qu'il doit
 * occuper. Par exemple, "<code>DD.ddd</code>" indique que la partie enti�re des degr�s devra toujours occuper
 * au moins 2 chiffres et la partie fractionnaire exactement 3 chiffres. Un angle �crit selon ce patron pourrait
 * �tre "<code>08.930</code>".
 * <br><br>
 * Dans le patron, le point d�cimal entre la partie enti�re et fractionnaire repr�sente toujours
 * le s�parateur d�cimal par d�faut pour une langue donn�e (la virgule pour les francophones). Ce s�parateur d�cimal
 * par d�faut d�pend de l'objet {@link DecimalFormatSymbols} qui a �t� sp�cifi� lors de la construction. Par exemple
 * le patron "<code>D�MM.mm'</code>" peut �crire "<code>46�12,32'</code>" si la langue de l'utilisateur est le fran�ais.
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
     * Caract�re repr�sentant l'h�misph�re nord.
     * Il doit obligatoirement �tre en majuscule.
     */
    private static final char NORTH='N';

    /**
     * Caract�re repr�sentant l'h�misph�re sud.
     * Il doit obligatoirement �tre en majuscule.
     */
    private static final char SOUTH='S';

    /**
     * Caract�re repr�sentant l'h�misph�re est.
     * Il doit obligatoirement �tre en majuscule.
     */
    private static final char EAST='E';

    /**
     * Caract�re repr�sentant l'h�misph�re ouest.
     * Il doit obligatoirement �tre en majuscule.
     */
    private static final char WEST='W';

    /**
     * Constante indique que l'angle
     * � formater est une longitude.
     */
    static final int LONGITUDE=0;

    /**
     * Constante indique que l'angle
     * � formater est une latitude.
     */
    static final int LATITUDE=1;

    /**
     * Constante indique que le nombre
     * � formater est une altitude.
     */
    static final int ALTITUDE=2;

    /**
     * Constante d�signant le champs des degr�s. Cette valeur peut �tre
     * transmise au constructeur de {@link java.text.FieldPosition}, afin
     * que la m�thode {@link #format(Object,StringBuffer,FieldPosition)}
     * puisse donner les index de la partie des degr�s de l'angle �crit.
     */
    public static final int DEGREES_FIELD=1;

    /**
     * Constante d�signant le champs des minutes. Cette valeur peut �tre
     * transmise au constructeur de {@link java.text.FieldPosition}, afin
     * que la m�thode {@link #format(Object,StringBuffer,FieldPosition)}
     * puisse donner les index de la partie des minutes de l'angle �crit.
     */
    public static final int MINUTES_FIELD=2;

    /**
     * Constante d�signant le champs des secondes. Cette valeur peut �tre
     * transmise au constructeur de {@link java.text.FieldPosition}, afin
     * que la m�thode {@link #format(Object,StringBuffer,FieldPosition)}
     * puisse donner les index de la partie des secondes de l'angle �crit.
     */
    public static final int SECONDS_FIELD=3;

    /**
     * Constante d�signant le champs de l'h�misph�re. Cette valeur peut �tre
     * transmise au constructeur de {@link java.text.FieldPosition}, afin
     * que la m�thode {@link #format(Object,StringBuffer,FieldPosition)}
     * puisse donner les index de l'h�misph�re dans l'angle �crit.
     */
    public static final int HEMISPHERE_FIELD=4;

    /**
     * Symboles repr�sentant les degr�s (0),
     * minutes (1) et les secondes (2).
     */
    private static final char[] SYMBOLS = {'D', 'M', 'S'};
    
    /**
     * Formateur global utilis� pour {@link Angle#toString}.
     * Les param�tres de ce format ne devrait jamais �tre modifi�s.
     */
    static final AngleFormat sharedInstance=new AngleFormat(); // Doit �tre le dernier champ statique!

    /**
     * Nombre minimal d'espaces que doivent occuper les parties
     * enti�res des degr�s (0), minutes (1) et secondes (2). Le
     * champs <code>widthDecimal</code> indique la largeur fixe
     * que doit avoir la partie d�cimale. Il s'appliquera au
     * dernier champs non-zero dans <code>width0..2</code>.
     */
    private int width0=1, width1=2, width2=0, widthDecimal=0;
    
    /**
     * Caract�res � ins�rer au d�but (<code>prefix</code>) et � la
     * suite des degr�s, minutes et secondes (<code>suffix0..2</code>).
     * Ces champs doivent �tre <code>null</code> s'il n'y a rien � ins�rer.
     */
    private String prefix=null, suffix0="�", suffix1="'", suffix2="\"";

    /**
     * Indique s'il faut utiliser le s�parateur d�cimal pour s�parer la partie
     * enti�re de la partie fractionnaire. La valeur <code>false</code> indique
     * que les parties enti�res et fractionnaires doivent �tre �crites ensembles
     * (par exemple 34867 pour 34.867). La valeur par d�faut est <code>true</code>.
     */
    private boolean decimalSeparator=true;

    /**
     * Format � utiliser pour �crire les nombres
     * (degr�s, minutes ou secondes) � l'int�rieur
     * de l'�criture d'un angle.
     */
    private final DecimalFormat numberFormat;
    
    /**
     * Objet � transmetre aux m�thodes <code>DecimalFormat.format</code>.
     * Ce param�tre existe simplement pour �viter de cr�er cet objet trop
     * souvent, alors qu'on ne s'y int�resse pas.
     */
    private final transient FieldPosition dummy=new FieldPosition(0);

    /**
     * Retourne la largeur du champ sp�cifi�.
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
     * Modifie la largeur du champ sp�cifi�. La largeur
     * de tous les champ qui suivent sera mis � 0.
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
     * Retourne le suffix du champ sp�cifi�.
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
     * Modifie le suffix du champs sp�cifi�. Les suffix de tous
     * les champ qui suivent seront mis � leur valeur par d�faut.
     */
    private void setSuffix(final int index, String s)
    {
        switch (index)
        {
            case -1:  prefix=s; s="�";  // fall through
            case  0: suffix0=s; s="'";  // fall through
            case  1: suffix1=s; s="\""; // fall through
            case  2: suffix2=s;         // fall through
        }
    }

    /**
     * Construit un objet qui effectuera les lectures et
     * �critures des angles dans un format par d�faut.
     */
    public AngleFormat()
    {this("D�MM.m'");}

    /**
     * Construit un objet qui effectuera les lectures et �critures des angles.
     *
     * @param  pattern Patron � utiliser pour l'�criture des angles.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public AngleFormat(final String pattern) throws IllegalArgumentException
    {this(pattern, new DecimalFormatSymbols());}

    /**
     * Construit un objet qui effectuera les lectures et �critures des angles.
     *
     * @param  pattern Patron � utiliser pour l'�criture des angles.
     * @param  locale  Pays dont on voudra utiliser les conventions.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public AngleFormat(final String pattern, final Locale locale) throws IllegalArgumentException
    {this(pattern, new DecimalFormatSymbols(locale));}

    /**
     * Construit un objet qui effectuera les lectures et �critures des angles.
     *
     * @param pattern Patron � utiliser pour l'�criture des angles.
     * @param symbols Symboles � utiliser pour repr�senter les nombres.
     */
    public AngleFormat(final String pattern, final DecimalFormatSymbols symbols)
    {
        // NOTE: pour cette routine, il ne faut PAS que DecimalFormat
        //       reconnaisse la notation exponentielle, parce que �a
        //       risquerait d'�tre confondu avec le "E" de "Est".
        numberFormat=new DecimalFormat("#0", symbols);
        applyPattern(pattern);
    }

    /**
     * Sp�cifie un patron pour le format d'�criture des angles. Le format est sp�cifi�
     * par une cha�ne de caract�res dans laquelle "D" repr�sente les d�gr�s, "M" les
     * minutes et "S" les secondes. Les lettres minuscules "d", "m" ou "s" repr�sentent
     * la partie fractionnaire de leur �quivalent majuscule. Exemples:
     *
     * <pre>
     * &nbsp;   Sp�cification de format       R�sultat
     * &nbsp;   -----------------------       ---------
     * &nbsp;     "DD�MM'SS\""                48�30'00"
     * &nbsp;     "DD�MM'"                    48�30'
     * &nbsp;     "DD.ddd"                    48.500
     * &nbsp;     "DDMM"                      4830
     * &nbsp;     "DDMMSS"                    483000
     * </pre>
     *
     * @param  Patron � utiliser pour les �critures des angles.
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
             * On examine un � un tous les caract�res du patron en
             * sautant ceux qui ne sont pas r�serv�s ("D", "M", "S"
             * et leur �quivalents en minuscules). Les caract�res
             * non-reserv�s seront m�moris�s comme suffix plus tard.
             */
            final char c=pattern.charAt(i);
            final char upperCaseC=Character.toUpperCase(c);
            for (int field=0; field<SYMBOLS.length; field++)
            {
                if (upperCaseC==SYMBOLS[field])
                {
                    /*
                     * Un caract�re r�serv� a �t� trouv�. V�rifie maintenant
                     * s'il est valide. Par exemple il serait illegal d'avoir
                     * comme patron "MM.mm" sans qu'il soit pr�c�d� des degr�s.
                     * On attend les lettres "D", "M" et "S" dans l'ordre. Si
                     * le caract�re est en lettres minuscules, il doit �tre le
                     * m�me que le dernier code (par exemple "DD.mm" est illegal).
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
                        throw new IllegalArgumentException(Resources.format(Cl�.BAD_ANGLE_PATTERN�1, pattern));
                    }
                    if (c==upperCaseC)
                    {
                        /*
                         * M�morise les caract�res qui pr�c�daient ce code comme suffix
                         * du champs pr�c�dent. Puis on contera le nombre de fois que le
                         * code se r�p�te, en m�morisera cette information comme largeur
                         * de ce champs.
                         */
                        setSuffix(field-1, (i>startPrefix) ? pattern.substring(startPrefix, i) : null);
                        int w=1; while (++i<length && pattern.charAt(i)==c) w++;
                        setWidth(field, w);
                    }
                    else
                    {
                        /*
                         * Si le caract�re est une minuscule, ce qui le pr�c�dait sera le
                         * s�parateur d�cimal plut�t qu'un suffix. On comptera le nombre
                         * d'occurences du caract�res pour obtenir la pr�cision.
                         */
                        switch (i-startPrefix)
                        {
                                case 0: decimalSeparator=false; break;
                                case 1: if (pattern.charAt(startPrefix)=='.')
                                        {
                                            decimalSeparator=true;
                                            break;
                                        }
                                default: throw new IllegalArgumentException(Resources.format(Cl�.BAD_ANGLE_PATTERN�1, pattern));
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
     * Renvoie le patron utilis� pour les �critures des angles.
     * La m�thode {@link #applyPattern} d�crit la fa�on dont le
     * patron est interpr�t� par les objets <code>AngleFormat</code>.
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
                 * Proc�de � l'�criture de la partie enti�re des degr�s,
                 * minutes ou secondes. Le suffix du champs pr�c�dent
                 * sera �crit avant les degr�s, minutes ou secondes.
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
                 * Proc�de � l'�criture de la partie d�cimale des
                 * degr�s, minutes ou secondes. Le suffix du ce
                 * champs sera �crit apr�s cette partie fractionnaire.
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
     * Ecrit un angle dans une cha�ne de caract�res.
     * L'angle sera format� en utilisant comme mod�le
     * le patron sp�cifi� lors du dernier appel de la
     * m�thode {@link #applyPattern}.
     *
     * @param  angle Angle � �crire, en degr�s.
     * @return Cha�ne de caract�res repr�sentant cet angle.
     */
    public final String format(final double angle)
    {return format(angle, new StringBuffer(), null).toString();}
    
    /**
     * Proc�de � l'�criture d'un angle.
     * L'angle sera format� en utilisant comme mod�le
     * le patron sp�cifi� lors du dernier appel de la
     * m�thode {@link #applyPattern}.
     *
     * @param  angle      Angle � �crire, en degr�s.
     * @param  toAppendTo Buffer dans lequel �crire l'angle.
     * @param  pos        En entr�, le code du champs dont on d�sire les index
     *                    ({@link #DEGREES_FIELD},
     *                     {@link #MINUTES_FIELD} ou
     *                     {@link #SECONDS_FIELD}).
     *                    En sortie, les index du champs demand�. Ce param�tre
     *                    peut �tre nul si cette information n'est pas d�sir�e.
     *
     * @return Le buffer <code>toAppendTo</code> par commodit�.
     */
    public synchronized StringBuffer format(final double angle, StringBuffer toAppendTo, final FieldPosition pos)
    {
        double degr�s = angle;
        /*
         * Calcule � l'avance les minutes et les secondes. Si les minutes et secondes
         * ne doivent pas �tre �crits, on m�morisera NaN. Notez que pour extraire les
         * parties enti�res, on utilise (int) au lieu de 'Math.floor' car (int) arrondie
         * vers 0 (ce qui est le comportement souhait�) alors que 'floor' arrondie vers
         * l'entier inf�rieur.
         */
        double minutes  = Double.NaN;
        double secondes = Double.NaN;
        if (width1!=0)
        {
            int tmp = (int) degr�s; // Arrondie vers 0 m�me si n�gatif.
            minutes = Math.abs(degr�s-tmp)*60;
            degr�s  = tmp;
            if (minutes<0 || minutes>60)
            {
                // Erreur d'arrondissement (parce que l'angle est trop �lev�)
                throw new IllegalArgumentException(Resources.format(Cl�.ANGLE_OVERFLOW�1, new Double(angle)));
            }
            if (width2!=0)
            {
                tmp      = (int) minutes; // Arrondie vers 0 m�me si n�gatif.
                secondes = (minutes-tmp)*60;
                minutes  = tmp;
                if (secondes<0 || secondes>60)
                {
                    // Erreur d'arrondissement (parce que l'angle est trop �lev�)
                    throw new IllegalArgumentException(Resources.format(Cl�.ANGLE_OVERFLOW�1, new Double(angle)));
                }
                /*
                 * On applique maintenant une correction qui tiendra
                 * compte des probl�mes d'arrondissements.
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
            tmp = (int) (minutes/60); // Arrondie vers 0 m�me si n�gatif.
            minutes -= 60*tmp;
            degr�s += tmp;
        }
        /*
         * Les variables 'degr�s', 'minutes' et 'secondes' contiennent
         * maintenant les valeurs des champs � �crire, en principe �pur�s
         * des probl�mes d'arrondissements. Proc�de maintenant � l'�criture
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
                                     toAppendTo=formatField(degr�s,   toAppendTo, (field==DEGREES_FIELD ? pos : null), width0, width1==0, suffix0);
        if (!Double.isNaN(minutes))  toAppendTo=formatField(minutes,  toAppendTo, (field==MINUTES_FIELD ? pos : null), width1, width2==0, suffix1);
        if (!Double.isNaN(secondes)) toAppendTo=formatField(secondes, toAppendTo, (field==SECONDS_FIELD ? pos : null), width2, true,      suffix2);
        return toAppendTo;
    }

    /**
     * Proc�de � l'�criture d'un champ de l'angle.
     *
     * @param value Valeur � �crire.
     * @param toAppendTo Buffer dans lequel �crire le champs.
     * @param pos Objet dans lequel m�moriser les index des premiers
     *        et derniers caract�res �crits, ou <code>null</code>
     *        pour ne pas m�moriser ces index.
     * @param w Nombre de minimal caract�res de la partie enti�re.
     * @param last <code>true</code> si ce champs est le dernier,
     *        et qu'il faut donc �crire la partie d�cimale.
     * @param s Suffix � �crire apr�s le nombre (peut �tre nul).
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
     * Proc�de � l'�criture d'un angle, d'une latitude ou d'une longitude. En principe l'objet <code>obj</code> doit
     * �tre de la classe {@link Angle} ou une d'une classe d�riv�e. Il peut s'agir notamment d'un objet de la classe
     * {@link Latitude} ou {@link Longitude}.  Dans ce cas, le symbole appropri� ("N", "S", "E" ou "W") sera ajout�e
     * � la suite de l'angle.
     * <br>
     * <br>
     * Bien qu'en principe ce n'est pas le r�le de cette classe, cette m�thode acceptera aussi des objets de la classe
     * {@link Number}. Ils seront alors �crits comme des nombres ordinaires. Cette aptitude est utile pour �crire par
     * exemple une profondeur apr�s avoir �crit les angles d'une coordonn�e g�ographique, mais en utilisant les m�mes
     * symboles d�cimaux.
     *
     * @param  angle      Angle ou nombre � �crire.
     * @param  toAppendTo Buffer dans lequel �crire l'angle.
     * @param  pos        En entr�, le code du champs dont on d�sire les index
     *                    ({@link #DEGREES_FIELD},
     *                     {@link #MINUTES_FIELD},
     *                     {@link #SECONDS_FIELD} ou
     *                     {@link #HEMISPHERE_FIELD}).
     *                    En sortie, les index du champs demand�. Ce param�tre
     *                    peut �tre nul si cette information n'est pas d�sir�e.
     *
     * @return Le buffer <code>toAppendTo</code> par commodit�.
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
        throw new IllegalArgumentException(Resources.format(Cl�.NOT_AN_ANGLE_OBJECT�2, new Integer(0), XClass.getShortClassName(obj)));
    }

    /**
     * Proc�de � l'�criture d'un angle, d'une latitude ou d'une longitude.
     *
     * @param  angle      Angle ou nombre � �crire.
     * @param  type       Type de l'angle ou du nombre:
     *                    {@link #LONGITUDE},
     *                    {@link #LATITUDE} ou
     *                    {@link #ALTITUDE}.
     * @param  toAppendTo Buffer dans lequel �crire l'angle.
     * @param  pos        En entr�, le code du champs dont on d�sire les index
     *                    ({@link #DEGREES_FIELD},
     *                     {@link #MINUTES_FIELD},
     *                     {@link #SECONDS_FIELD} ou
     *                     {@link #HEMISPHERE_FIELD}).
     *                    En sortie, les index du champs demand�. Ce param�tre
     *                    peut �tre nul si cette information n'est pas d�sir�e.
     *
     * @return Le buffer <code>toAppendTo</code> par commodit�.
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
     * Proc�de � l'�criture d'un angle suivit d'un suffix 'N','S','E' ou 'W'.
     * L'angle sera format� en utilisant comme mod�le le patron sp�cifi� lors
     * du dernier appel de la m�thode {@link #applyPattern}.
     *
     * @param  angle      Angle � �crire, en degr�s.
     * @param  toAppendTo Buffer dans lequel �crire l'angle.
     * @param  pos        En entr�, le code du champs dont on d�sire les index
     *                    ({@link #DEGREES_FIELD},
     *                     {@link #MINUTES_FIELD},
     *                     {@link #SECONDS_FIELD} ou
     *                     {@link #HEMISPHERE_FIELD}).
     *                    En sortie, les index du champs demand�. Ce param�tre
     *                    peut �tre nul si cette information n'est pas d�sir�e.
     * @param north       Caract�res � �crire si l'angle est positif ou nul.
     * @param south       Caract�res � �crire si l'angle est n�gatif.
     *
     * @return Le buffer <code>toAppendTo</code> par commodit�.
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
     * Ignore le suffix d'un nombre. Cette m�thode est appell�e par la m�thode
     * {@link #parse} pour savoir quel champs il vient de lire. Par exemple si
     * l'on vient de lire les degr�s dans "48�12'", alors cette m�thode extraira
     * le "�" et retournera 0 pour indiquer que l'on vient de lire des degr�s.
     *
     * Cette m�thode se chargera d'ignorer les espaces qui pr�c�dent le suffix.
     * Elle tentera ensuite de d'abord interpr�ter le suffix selon les symboles
     * du patron (sp�cifi� avec {@link #applyPattern}. Si le suffix n'a pas �t�
     * reconnus, elle tentera ensuite de le comparer aux symboles standards
     * (� ' ").
     *
     * @param  source Cha�ne dans laquelle on doit sauter le suffix.
     * @param  pos En entr�, l'index du premier caract�re � consid�rer dans la
     *         cha�ne <code>pos</code>. En sortie, l'index du premier caract�re
     *         suivant le suffix (c'est-�-dire index � partir d'o� continuer la
     *         lecture apr�s l'appel de cette m�thode). Si le suffix n'a pas �t�
     *         reconnu, alors cette m�thode retourne par convention <code>SYMBOLS.length</code>.
     * @param  field Champs � v�rifier de pr�f�rences. Par exemple la valeur 1 signifie que les
     *         suffix des minutes et des secondes devront �tre v�rifi�s avant celui des degr�s.
     * @return Le num�ro du champs correspondant au suffix qui vient d'�tre extrait:
     *         -1 pour le pr�fix de l'angle, 0 pour le suffix des degr�s, 1 pour le
     *         suffix des minutes et 2 pour le suffix des secondes. Si le texte n'a
     *         pas �t� reconnu, retourne <code>SYMBOLS.length</code>.
     */
    private int skipSuffix(final String source, final ParsePosition pos, int field)
    {
        /*
         * Essaie d'abord de sauter les suffix qui
         * avaient �t� sp�cifi�s dans le patron.
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
         * Le texte trouv� ne correspondant � aucun suffix du patron,
         * essaie maintenant de sauter un des suffix standards (apr�s
         * avoir ignor� les espaces qui le pr�c�daient).
         */
        char c;
        do if (start>=length) return SYMBOLS.length;
        while (Character.isSpaceChar(c=source.charAt(start++)));
        switch (c)
        {
            case '�' : pos.setIndex(start); return 0; // Degr�s.
            case '\'': pos.setIndex(start); return 1; // Minutes
            case '"' : pos.setIndex(start); return 2; // Secondes
            default  : return SYMBOLS.length;         // Inconnu.
        }
    }

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant un angle. Les r�gles d'interpr�tation de
     * cette m�thode sont assez souples. Par exemple cettte m�thode interpr�tera correctement la
     * cha�ne "48�12.34'" m�me si le patron attendu �tait "DDMM.mm" (c'est-�-dire que la cha�ne
     * aurait du �tre "4812.34"). Les espaces entre les degr�s, minutes et secondes sont accept�s.
     * Si l'angle est suivit d'un symbole "N" ou "S", alors l'objet retourn� sera de la classe
     * {@link Latitude}. S'il est plutot suivit d'un symbole "E" ou "W", alors l'objet retourn�
     * sera de la classe {@link Longitude}. Sinon, il sera de la classe {@link Angle}.
     *
     * @param source Cha�ne de caract�res � lire.
     * @param pos    Position � partir d'o� interpr�ter la cha�ne.
     * @return       L'angle lu.
     */
    public Angle parse(final String source, final ParsePosition pos)
    {
        final Angle ang=parse(source, pos, false);
        return ang;
    }

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant un angle. Les r�gles d'interpr�tation de
     * cette m�thode sont assez souples. Par exemple cettte m�thode interpr�tera correctement la
     * cha�ne "48�12.34'" m�me si le patron attendu �tait "DDMM.mm" (c'est-�-dire que la cha�ne
     * aurait du �tre "4812.34"). Les espaces entre les degr�s, minutes et secondes sont accept�s.
     * Si l'angle est suivit d'un symbole "N" ou "S", alors l'objet retourn� sera de la classe
     * {@link Latitude}. S'il est plutot suivit d'un symbole "E" ou "W", alors l'objet retourn�
     * sera de la classe {@link Longitude}. Sinon, il sera de la classe {@link Angle}.
     *
     * @param source           Cha�ne de caract�res � lire.
     * @param pos              Position � partir d'o� interpr�ter la cha�ne.
     * @param spaceAsSeparator Indique si l'espace est accept� comme s�parateur � l'int�rieur d'un angle. La
     *                         valeur <code>true</code> fait que l'angle "45 30" sera interpr�t� comme "45�30".
     * @return L'angle lu.
     */
    private synchronized Angle parse(final String source, final ParsePosition pos, final boolean spaceAsSeparator)
    {
        double degr�s   = Double.NaN;
        double minutes  = Double.NaN;
        double secondes = Double.NaN;
        final int length=source.length();
        ///////////////////////////////////////////////////////////////////////////////
        // BLOC A: Analyse la cha�ne de caract�res 'source' et affecte aux variables //
        //         'degr�s', 'minutes' et 'secondes' les valeurs appropri�es.        //
        //         Les premi�res accolades ne servent qu'� garder locales            //
        //         les variables sans int�r�t une fois la lecture termin�e.          //
        ///////////////////////////////////////////////////////////////////////////////
        {
            /*
             * Extrait le pr�fix, s'il y en avait un. Si on tombe sur un symbole des
             * degr�s, minutes ou secondes alors qu'on n'a pas encore lu de nombre,
             * on consid�rera que la lecture a �chou�e.
             */
            final int indexStart=pos.getIndex();
            int index=skipSuffix(source, pos, -1); // -1==pr�fix
            if (index>=0 && index<SYMBOLS.length)
            {
                pos.setErrorIndex(indexStart);
                pos.setIndex(indexStart);
                return null;
            }
            /*
             * Saute les espaces blancs qui
             * pr�c�dent le champs des degr�s.
             */
            index=pos.getIndex();
            while (index<length && Character.isSpaceChar(source.charAt(index))) index++;
            pos.setIndex(index);
            /*
             * Lit les degr�s. Notez que si aucun s�parateur ne s�parait les degr�s
             * des minutes des secondes, alors cette lecture pourra inclure plusieurs
             * champs (exemple: "DDDMMmmm"). La s�paration sera faite plus tard.
             */
            Number fieldObject=numberFormat.parse(source, pos);
            if (fieldObject==null)
            {
                pos.setIndex(indexStart);
                if (pos.getErrorIndex()<indexStart)
                    pos.setErrorIndex(index);
                return null;
            }
            degr�s=fieldObject.doubleValue();
            int indexEndField=pos.getIndex();
            boolean swapDM=true;
BigBoss:    switch (skipSuffix(source, pos, 0)) // 0==DEGR�S
            {
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�S DEGR�S
                 * ----------------------------------------------
                 * Les degr�s �taient suivit du pr�fix d'un autre angle. Le pr�fix sera donc
                 * retourn� dans le buffer pour un �ventuel traitement par le prochain appel
                 * � la m�thode 'parse' et on n'ira pas plus loin dans l'analyse de la cha�ne.
                 */
                case -1: // -1==PREFIX
                {
                    pos.setIndex(indexEndField);
                    break BigBoss;
                }
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�S DEGR�S
                 * ----------------------------------------------
                 * On a trouv� le symbole des secondes au lieu de celui des degr�s. On fait
                 * la correction dans les variables 'degr�s' et 'secondes' et on consid�re
                 * que la lecture est termin�e.
                 */
                case 2: // 2==SECONDES
                {
                    secondes = degr�s;
                    degr�s = Double.NaN;
                    break BigBoss;
                }
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�S DEGR�S
                 * ----------------------------------------------
                 * Aucun symbole ne suit les degr�s. Des minutes sont-elles attendues?
                 * Si oui, on fera comme si le symbole des degr�s avait �t� l�. Sinon,
                 * on consid�rera que la lecture est termin�e.
                 */
                default:
                {
                    if (width1==0)         break BigBoss;
                    if (!spaceAsSeparator) break BigBoss;
                    // fall through
                }
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�S DEGR�S
                 * ----------------------------------------------
                 * Un symbole des degr�s a �t� explicitement trouv�. Les degr�s sont peut-�tre
                 * suivit des minutes. On proc�dera donc � la lecture du prochain nombre, puis
                 * � l'analyse du symbole qui le suit.
                 */
                case 0: // 0==DEGR�S
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
                         * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�ES MINUTES
                         * ------------------------------------------------
                         * Le symbole trouv� est bel et bien celui des minutes.
                         * On continuera le bloc pour tenter de lire les secondes.
                         */
                        case 1: // 1==MINUTES
                        {
                            break; // continue outer switch
                        }
                        /* ------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�ES MINUTES
                         * ------------------------------------------------
                         * Un symbole des secondes a �t� trouv� au lieu du symbole des minutes
                         * attendu. On fera la modification dans les variables 'secondes' et
                         * 'minutes' et on consid�rera la lecture termin�e.
                         */
                        case 2: // 2==SECONDES
                        {
                            secondes = minutes;
                            minutes = Double.NaN;
                            break BigBoss;
                        }
                        /* ------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�ES MINUTES
                         * ------------------------------------------------
                         * Aucun symbole n'a �t� trouv�. Les minutes �taient-elles attendues?
                         * Si oui, on les acceptera et on tentera de lire les secondes. Si non,
                         * on retourne le texte lu dans le buffer et on termine la lecture.
                         */
                        default:
                        {
                            if (width1!=0) break; // Continue outer switch
                            // fall through
                        }
                        /* ------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�ES MINUTES
                         * ------------------------------------------------
                         * Au lieu des minutes, le symbole lu est celui des degr�s. On consid�re
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
                         * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�ES MINUTES
                         * ------------------------------------------------
                         * Apr�s les minutes (qu'on accepte), on a trouv� le pr�fix du prochain
                         * angle � lire. On retourne ce pr�fix dans le buffer et on consid�re la
                         * lecture termin�e.
                         */
                        case -1: // -1==PR�FIX
                        {
                            pos.setIndex(indexEndField);
                            break BigBoss;
                        }
                    }
                    swapDM=false;
                    // fall through
                }
                /* ----------------------------------------------
                 * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�S DEGR�S
                 * ----------------------------------------------
                 * Un symbole des minutes a �t� trouv� au lieu du symbole des degr�s attendu.
                 * On fera donc la modification dans les variables 'degr�s' et 'minutes'. Ces
                 * minutes sont peut-�tre suivies des secondes. On tentera donc de lire le
                 * prochain nombre.
                 */
                case 1: // 1==MINUTES
                {
                    if (swapDM)
                    {
                        minutes = degr�s;
                        degr�s = Double.NaN;
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
                         * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�ES SECONDES
                         * -------------------------------------------------
                         * Un symbole des secondes explicite a �t� trouv�e.
                         * La lecture est donc termin�e.
                         */
                        case 2: // 2==SECONDES
                        {
                            break;
                        }
                        /* -------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�ES SECONDES
                         * -------------------------------------------------
                         * Aucun symbole n'a �t� trouv�e. Attendait-on des secondes? Si oui, les
                         * secondes seront accept�es. Sinon, elles seront retourn�es au buffer.
                         */
                        default:
                        {
                            if (width2!=0) break;
                            // fall through
                        }
                        /* -------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�ES SECONDES
                         * -------------------------------------------------
                         * Au lieu des degr�s, on a trouv� un symbole des minutes ou des
                         * secondes. On renvoie donc le nombre et son symbole dans le buffer.
                         */
                        case 1: // 1==MINUTES
                        case 0: // 0==DEGR�S
                        {
                            pos.setIndex(indexStartField);
                            secondes=Double.NaN;
                            break;
                        }
                        /* -------------------------------------------------
                         * ANALYSE DU SYMBOLE SUIVANT LES PR�SUM�ES SECONDES
                         * -------------------------------------------------
                         * Apr�s les secondes (qu'on accepte), on a trouv� le pr�fix du prochain
                         * angle � lire. On retourne ce pr�fix dans le buffer et on consid�re la
                         * lecture termin�e.
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
        // BLOC B: Prend en compte l'�ventualit� ou le s�parateur d�cimal //
        //         aurrait �t� absent, puis calcule l'angle en degr�s.    //
        ////////////////////////////////////////////////////////////////////
        if (minutes<0)
        {
            secondes = -secondes;
        }
        if (degr�s<0)
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
                        degr�s /= facteur;
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
                        degr�s /= facteur;
                    }
                    else minutes /= facteur;
                }
                else if (Double.isNaN(minutes))
                {
                    degr�s /= facteur;
                }
            }
        }
        /*
         * S'il n'y a rien qui permet de s�parer les degr�s des minutes (par exemple si
         * le patron est "DDDMMmmm"), alors la variable 'degr�s' englobe � la fois les
         * degr�s, les minutes et d'�ventuelles secondes. On applique une correction ici.
         */
        if (suffix1==null && width2!=0 && Double.isNaN(secondes))
        {
            double facteur = XMath.pow10(width2);
            if (suffix0==null && width1!=0 && Double.isNaN(minutes))
            {
                ///////////////////
                //// DDDMMSS.s ////
                ///////////////////
                secondes = degr�s;
                minutes  = (int) (degr�s/facteur); // Arrondie vers 0
                secondes -= minutes*facteur;
                facteur  = XMath.pow10(width1);
                degr�s   = (int) (minutes/facteur); // Arrondie vers 0
                minutes -= degr�s*facteur;
            }
            else
            {
                ////////////////////
                //// DDD�MMSS.s ////
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
            minutes = degr�s;
            degr�s = (int) (degr�s/facteur); // Arrondie vers 0
            minutes -= degr�s*facteur;
        }
        pos.setErrorIndex(-1);
        if ( Double.isNaN(degr�s))   degr�s=0;
        if (!Double.isNaN(minutes))  degr�s += minutes/60;
        if (!Double.isNaN(secondes)) degr�s += secondes/3600;
        /////////////////////////////////////////////////////
        // BLOC C: V�rifie maintenant si l'angle ne serait //
        //         pas suivit d'un symbole N, S, E ou W.   //
        /////////////////////////////////////////////////////
        for (int index=pos.getIndex(); index<length; index++)
        {
            final char c=source.charAt(index);
            switch (Character.toUpperCase(c))
            {
                case NORTH: pos.setIndex(index+1); return new Latitude ( degr�s);
                case SOUTH: pos.setIndex(index+1); return new Latitude (-degr�s);
                case EAST : pos.setIndex(index+1); return new Longitude( degr�s);
                case WEST : pos.setIndex(index+1); return new Longitude(-degr�s);
            }
            if (!Character.isSpaceChar(c)) break;
        }
        return new Angle(degr�s);
    }

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant un angle.
     *
     * @param  source Cha�ne de caract�res � interpr�ter.
     * @return L'angle comme objet {@link Angle}, {@link Latitude} ou {@link Longitude}.
     * @throws ParseException si la cha�ne n'a pas �t� compl�tement reconnue.
     */
    public Angle parse(final String source) throws ParseException
    {
        final ParsePosition pos = new ParsePosition(0);
        final Angle         ang = parse(source, pos, true);
        checkComplete(source, pos, false);
        return ang;
    }

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant un angle.
     *
     * @param  source Cha�ne de caract�res � interpr�ter.
     * @param  pos Position � partir d'o� commencer l'interpr�tation de la cha�ne <code>source</code>.
     * @return L'angle comme objet {@link Angle}, {@link Latitude} ou {@link Longitude}.
     */
    public Object parseObject(final String source, final ParsePosition pos)
    {return parse(source, pos);}

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant un angle. Cette m�thode
     * est red�finie afin d'obtenir un message d'erreur plus explicite que celui
     * de {@link Format#parseObject} en cas d'erreur.
     *
     * @param  source Cha�ne de caract�res � interpr�ter.
     * @return L'angle comme objet {@link Angle}, {@link Latitude} ou {@link Longitude}.
     * @throws ParseException si la cha�ne n'a pas �t� compl�tement reconnue.
     */
    public Object parseObject(final String source) throws ParseException
    {return parse(source);}

    /**
     * Interpr�te une cha�ne de caract�res qui devrait repr�senter un nombre.
     * Cette m�thode est utile pour lire une altitude apr�s les angles.
     *
     * @param  source Cha�ne de caract�res � interpr�ter.
     * @param  pos Position � partir d'o� commencer l'interpr�tation de la cha�ne <code>source</code>.
     * @return Le nombre lu comme objet {@link Number}.
     */
    final Number parseNumber(final String source, final ParsePosition pos)
    {return numberFormat.parse(source, pos);}

    /**
     * V�rifie si l'interpr�tation d'une cha�ne de caract�res a �t� compl�te. Si ce n'�tait pas le
     * cas, lance une exception avec un message d'erreur soulignant les caract�res probl�matiques.
     *
     * @param  source Cha�ne de caract�res qui �tait � interpr�ter.
     * @param  pos Position � laquelle s'est termin�e l'interpr�tation de la cha�ne <code>source</code>.
     * @param  isCoordinate <code>false</code> si on interpr�tait un angle, ou <code>true</code> si on
     *         interpr�tait une coordonn�e.
     * @throws ParseException Si la cha�ne <code>source</code> n'a pas �t� interpr�t�e dans sa totalit�.
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
                throw new ParseException(Resources.format(Cl�.PARSE_EXCEPTION_ANGLE�3, flag, source, source.substring(lower, Math.min(lower+10, upper))), index);
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
     * V�rifie si ce format est �gal au format <code>obj</code> sp�cifi�.
     * Ce sera le cas si les deux formats sont de la m�me classe et
     * utilisent le m�me patron ainsi que les m�mes symboles d�cimaux.
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
     * V�rifie l'�galit� de deux objets qui peuvent �tre nuls.
     */
    static boolean equals(final Object o1, final Object o2)
    {return (o1==o2 || (o1!=null && o1.equals(o2)));}
    
    /**
     * Renvoie une copie de ce format.
     */
    public AngleFormat clone()
    {return (AngleFormat) super.clone();}

    /**
     * Renvoie une repr�sentation de cet objet. Cette
     * m�thode n'est utile qu'� des fins de d�boguage.
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
         *   <tr><td>{@link Longitude}&nbsp;</td> <td>-180� to 180�</td></tr>
         *   <tr><td>{@link Latitude}&nbsp;</td>  <td>-90� to 90�</td>  </tr>
         *   <tr><td>{@link Angle}&nbsp;</td>     <td>0� to 360�</td>   </tr>
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
                throw new IllegalArgumentException(Resources.format(Cl�.ILLEGAL_ARGUMENT�1, value));
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
