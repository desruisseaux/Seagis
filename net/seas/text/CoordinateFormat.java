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
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.DecimalFormatSymbols;

// Angles
import net.seas.opengis.pt.Angle;
import net.seas.opengis.pt.Latitude;
import net.seas.opengis.pt.Longitude;
import net.seas.opengis.pt.CoordinatePoint;

// Miscellaneous
import net.seas.util.XClass;
import net.seas.resources.Resources;


/** 
 * Ecrit et lit des coordonn�es exprim�es selon un certain patron. Les coordonn�es comprennent
 * une latitude et une longitude (possiblement dans l'ordre inverse) et de fa�on facultative une
 * altitude. Les coordonn�es lues seront plac�es dans des objets {@link CoordinatePoint}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class CoordinateFormat extends Format
{
    /**
     * Constante pour {@link #setDimension} indiquant que le nombre de
     * dimension des coordonn�es pourra �tre d�termin� automatiquement.
     */
    public static final int AUTO_DIMENSION = -1;

    /**
     * Constante pour {@link #setOrder} indiquant que les coordonn�es
     * seront �crites dans l'ordre <var>longitude</var>,<var>latitude</var>
     * et �ventuellement <var>altitude</var>.
     */
    public static final int XY_ORDER = -2;

    /**
     * Constante pour {@link #setOrder} indiquant que les coordonn�es
     * seront �crites dans l'ordre <var>latitude</var>,<var>longitude</var>
     * et �ventuellement <var>altitude</var>.
     */
    public static final int YX_ORDER = -3;

    /**
     * Constante pour {@link FieldPosition}
     * d�signant le champ de la latitude.
     */
    public static final int LATITUDE_FIELD = 1;

    /**
     * Constante pour {@link FieldPosition}
     * d�signant le champ de la longitude.
     */
    public static final int LONGITUDE_FIELD = 2;

    /**
     * Constante pour {@link FieldPosition}
     * d�signant le champ de l'altitude.
     */
    public static final int ALTITUDE_FIELD = 3;

    /**
     * Format utilis� pour lire et �crire les angles.
     */
    private final AngleFormat angleFormat;

    /**
     * Cha�ne de caract�res � utiliser
     * pour s�parer les angles entre eux.
     */
    private String separator = " ";

    /**
     * Cha�ne de caract�res � placer devant l'altitude.
     * Ces caract�res s�pareront l'altitude du dernier
     * angle �crit.
     */
    private String zPrefix = separator;

    /**
     * Cha�ne de caract�res � placer apr�s l'altitude.
     * Il s'agira parfois des unit�s comme "m".
     */
    private String zSuffix = "";

    /**
     * Nombre � �crire � la place de l'altitude lorsqu'elle
     * �tait requise (<code>dimension==3<code>) mais qu'elle
     * n'�tait pas sp�cifi�e.
     */
    private double missing = Double.NaN;

    /**
     * Nombre de dimension des coordonn�es. Les valeurs
     * permises sont 2, 3 ou {@link #AUTO_DIMENSION}.
     */
    private int dimension = AUTO_DIMENSION;

    /**
     * Ordre des ordonn�es (latitude et longitude). Les valeurs
     * permises sont {@link #XY_ORDER} et {@link #YX_ORDER}.
     */
    private int order = YX_ORDER;

    /**
     * Construit un objet par d�faut pour
     * lire et �crire des coordonn�es.
     */
    public CoordinateFormat()
    {angleFormat=new AngleFormat();}

    /**
     * Construit un objet qui lira des coordonn�es en utilisant le patron sp�cifi� pour les angles. La
     * syntaxe du patron est sp�cifi� dans la description de la m�thode {@link AngleFormat#applyPattern}.
     *
     * @param  pattern Patron � utiliser pour l'�criture des angles.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public CoordinateFormat(final String pattern) throws IllegalArgumentException
    {angleFormat=new AngleFormat(pattern);}

    /**
     * Construit un objet qui lira des coordonn�es en utilisant le patron sp�cifi� pour les angles. La
     * syntaxe du patron est sp�cifi� dans la description de la m�thode {@link AngleFormat#applyPattern}.
     *
     * @param  pattern Patron � utiliser pour l'�criture des angles.
     * @param  locale  Pays dont on voudra utiliser les conventions.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public CoordinateFormat(final String pattern, final Locale locale) throws IllegalArgumentException
    {angleFormat=new AngleFormat(pattern, locale);}

    /**
     * Construit un objet qui lira des coordonn�es en utilisant le patron sp�cifi� pour les angles. La
     * syntaxe du patron est sp�cifi� dans la description de la m�thode {@link AngleFormat#applyPattern}.
     *
     * @param pattern Patron � utiliser pour l'�criture des angles.
     * @param symbols Symboles � utiliser pour repr�senter les nombres.
     */
    public CoordinateFormat(final String pattern, final DecimalFormatSymbols symbols)
    {angleFormat=new AngleFormat(pattern, symbols);}

    /**
     * Construit un objet qui lira des coordonn�es en
     * utilisant le formateur sp�cifi� pour les angles.
     *
     * @param angleFormat Objet � utiliser pour lire et �crire les angles.
     */
    public CoordinateFormat(final AngleFormat angleFormat)
    {this.angleFormat=angleFormat;}


    /**
     * Sp�cifie un patron pour le format d'�criture des angles. La syntaxe du patron
     * est sp�cifi� dans la description de la m�thode {@link AngleFormat#applyPattern}.
     *
     * @param  Patron � utiliser pour les �critures des angles.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public void setAnglePattern(final String pattern) throws IllegalArgumentException
    {angleFormat.applyPattern(pattern);}

    /**
     * Renvoie le patron utilis� pour les �critures des angles.
     */
    public String getAnglePattern()
    {return angleFormat.toPattern();}

    /**
     * D�fini le nombre de dimensions des coordonn�es
     * attendues. Les valeurs permises sont:
     *
     * <ul>
     *   <li>2 pour lire des coordonn�es qui ne comprennent qu'une latitude et une longitude.</li>
     *   <li>3 pour lire des coordonn�es qui comprennent une latitude, une longitude et une altitude.</li>
     *   <li>{@link #AUTO_DIMENSION} pour d�terminer automatiquement si une coordonn�e comprend une
     *       altitude ou pas. C'est la valeur par d�faut.</li>
     * </ul>
     *
     * @throws IllegalArgumentException Si <code>dimension</code> n'est pas une des constantes ci-haut mentionn�es.
     */
    public void setDimension(final int dimension) throws IllegalArgumentException
    {
        synchronized (angleFormat)
        {
            switch (dimension)
            {
                case 2: // fallthrough
                case 3: // fallthrough
                case AUTO_DIMENSION:
                {
                    this.dimension=dimension;
                    break;
                }
                default: throw new IllegalArgumentException(Resources.format(Cl�.ILLEGAL_ARGUMENT�1, new Integer(dimension)));
            }
        }
    }

    /**
     * Retourne le nombre de dimensions
     * des coordonn�es attendues.
     */
    public int getDimension()
    {return dimension;}

    /**
     * D�fini l'ordre des ordonn�es <var>latitude</var> et
     * <var>longitude</var>. Les valeurs permises sont:
     *
     * <ul>
     *   <li>{@link #XY_ORDER} pour �crire les ordonn�es dans l'ordre (<var>longitude</var>,<var>latitude</var>).</li>
     *   <li>{@link #YX_ORDER} pour �crire les ordonn�es dans l'ordre (<var>latitude</var>,<var>longitude</var>).
     *       C'est la valeur par d�faut.</li>
     * </ul>
     *
     * @throws IllegalArgumentException Si <code>order</code> n'est pas une des constantes ci-haut mentionn�es.
     */
    public void setOrder(final int order) throws IllegalArgumentException
    {
        synchronized (angleFormat)
        {
            switch (order)
            {
                case XY_ORDER: // fallthrough
                case YX_ORDER:
                {
                    this.order=order;
                    break;
                }
                default: throw new IllegalArgumentException(Resources.format(Cl�.ILLEGAL_ARGUMENT�1, new Integer(order)));
            }
        }
    }

    /**
     * Retourne l'ordre des ordonn�es.
     */
    public int getOrder()
    {return order;}

    /**
     * Proc�de � l'�criture d'une coordonn�e.
     *
     * @param  longitude Longitude en degr�s.
     * @param  latitude  Latitude en degr�s.
     * @param  toAppendTo Buffer dans lequel �crire la coordonn�e.
     * @param  pos Champ (latitude, longitude ou altitude) dont on veut conna�tre les index dans la cha�ne.
     * @return Le buffer dans lequel a �t� ecrit la coordonn�e (habituellement <code>toAppendTo</code>).
     */
    public StringBuffer format(final double longitude, final double latitude, StringBuffer toAppendTo, final FieldPosition pos)
    {
        synchronized (angleFormat)
        {
            int begin = 0;
            int end   = 0;
            final int field = (pos!=null) ? pos.getField() : 0;
            switch (order)
            {
                case XY_ORDER:
                {
                    if (field==LONGITUDE_FIELD) begin=toAppendTo.length();
                    toAppendTo=angleFormat.format(new Longitude(longitude), toAppendTo, null);
                    if (field==LONGITUDE_FIELD) end=toAppendTo.length()-1;
                    toAppendTo.append(separator);
                    if (field==LATITUDE_FIELD) begin=toAppendTo.length();
                    toAppendTo=angleFormat.format(new Latitude(latitude), toAppendTo, null);
                    if (field==LATITUDE_FIELD) end=toAppendTo.length()-1;
                    break;
                }
                case YX_ORDER:
                {
                    if (field==LATITUDE_FIELD) begin=toAppendTo.length();
                    toAppendTo=angleFormat.format(new Latitude(latitude), toAppendTo, null);
                    if (field==LATITUDE_FIELD) end=toAppendTo.length()-1;
                    toAppendTo.append(separator);
                    if (field==LONGITUDE_FIELD) begin=toAppendTo.length();
                    toAppendTo=angleFormat.format(new Longitude(longitude), toAppendTo, null);
                    if (field==LONGITUDE_FIELD) end=toAppendTo.length()-1;
                    break;
                }
                default: throw new AssertionError(); // Should not happen
            }
            if (pos!=null)
            {
                pos.setBeginIndex(begin);
                pos.setEndIndex(end);
            }
        }
        return toAppendTo;
    }

    /**
     * Proc�de � l'�criture d'une coordonn�e.
     *
     * @param  obj Coordonn�e � �crire. Il doit s'agir d'un objet {@link CoordinatePoint}.
     * @param  toAppendTo Buffer dans lequel �crire la coordonn�e.
     * @param  pos Champ (latitude, longitude ou altitude) dont on veut conna�tre les index dans la cha�ne.
     * @return Le buffer dans lequel a �t� ecrit la coordonn�e (habituellement <code>toAppendTo</code>).
     * @throws IllegalArgumentException si <code>obj</code> n'est pas de la classe {@link CoordinatePoint}.
     */
    public StringBuffer format(final Object obj, StringBuffer toAppendTo, final FieldPosition pos) throws IllegalArgumentException
    {
        if (obj instanceof CoordinatePoint)
        {
            return format((CoordinatePoint)obj, toAppendTo, pos);
        }
        else throw new IllegalArgumentException(Resources.format(Cl�.NOT_AN_ANGLE_OBJECT�2, new Integer(1), XClass.getShortClassName(obj)));
    }

    /**
     * Proc�de � l'�criture d'une coordonn�e.
     *
     * @param  coord Coordonn�e � �crire..
     * @param  toAppendTo Buffer dans lequel �crire la coordonn�e.
     * @param  pos Champ (latitude, longitude ou altitude) dont on veut conna�tre les index dans la cha�ne.
     * @return Le buffer dans lequel a �t� ecrit la coordonn�e (habituellement <code>toAppendTo</code>).
     * @throws IllegalArgumentException si <code>obj</code> n'est pas de la classe {@link CoordinatePoint}.
     */
    public StringBuffer format(final CoordinatePoint coord, StringBuffer toAppendTo, final FieldPosition pos) throws IllegalArgumentException
    {
        synchronized (angleFormat)
        {
            toAppendTo=format(coord.getX(), coord.getY(), toAppendTo, pos);
            if (dimension!=2)
            {
                Number z=null;
                if (coord.getDimension()>=3)
                {
                    z = new Double(coord.getZ());
                }
                else if (dimension==3)
                {
                    z = new Double(missing);
                }
                if (z!=null)
                {
                    toAppendTo.append(zPrefix);
                    final int field = (pos!=null) ? pos.getField() : 0;
                    if (field==ALTITUDE_FIELD && pos!=null) pos.setBeginIndex(toAppendTo.length());
                    toAppendTo=angleFormat.format(z, toAppendTo, null);
                    if (field==ALTITUDE_FIELD && pos!=null) pos.setEndIndex(toAppendTo.length()-1);
                    toAppendTo.append(zSuffix);
                }
            }
            return toAppendTo;
        }
    }

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant une coordonn�e. Cette cha�ne doit contenir une
     * latitude et une longitude (possiblement dans l'ordre inverse), ainsi qu'une altitude facultative.
     * En cas de succ�s, l'objet retourn� sera de la classe {@link CoordinatePoint}. En cas d'�chec,
     * l'objet retourn� sera <code>null</code> si aucun angle n'a pu �tre lu ou {@link Angle} si un
     * seul angle a pu �tre lu.
     *
     * La position <code>pos</code> sera laiss�e � sa position initiale si une des conditions suivantes
     * est remplie:
     *
     * <ul>
     *   <li>La m�thode retourne <code>null</code>.</li>
     *   <li>La m�thode retourne un objet {@link Angle}.</li>
     *   <li>La m�thode retourne un objet {@link CoordinatePoint}, mais l'utilisateur a indiqu�
     *       qu'il exigeait aussi une altitude (donc un objet {@link Coordinate3D}).</li>
     *   <li>Une exception {@link ParseException} a �t� lanc�e.</li>
     * </ul>
     *
     * @param  source Cha�ne de caract�res � interpr�ter.
     * @param  pos Position � partir d'o� commencer l'interpr�tation de la cha�ne <code>source</code>.
     * @return La coordonn�e comme objet {@link CoordinatePoint} si au moins deux angles ont pu �tre
     *         interpr�t�s, un objet {@link Angle} si seul le premier angle a pu �tre interpr�t� ou
     *         <code>null</code> si aucun angle n'a pu �tre interpr�t�.
     * @throws ParseException si la coordonn�e a pu �tre interpr�t� mais qu'elle contient deux latitudes
     *         ou deux longitudes. Dans ce cas, <code>pos</code> sera remis � sa position initiale.
     */
    private Object parseImpl(final String source, final ParsePosition pos) throws ParseException
    {
        synchronized (angleFormat)
        {
            final int px=pos.getIndex();
            Angle x=angleFormat.parse(source, pos);
            if (x==null)
            {
                final int index=pos.getIndex();
                if (pos.getErrorIndex()<index)
                    pos.setErrorIndex(index);
                pos.setIndex(px);
                return x;
            }
            Angle y=angleFormat.parse(source, pos);
            if (y==null)
            {
                final int index=pos.getIndex();
                if (pos.getErrorIndex()<index)
                    pos.setErrorIndex(index);
                pos.setIndex(px);
                return x;
            }
            /*
             * Parvenu � ce stade, les deux premiers nombres (la longitude et la
             * latitude) on �t� lus avec succ�s. On v�rifie maintenant s'il faut
             * inverser leur ordre.
             */
            switch (order)
            {
                case XY_ORDER:
                {
                    // do nothing
                    break;
                }
                case YX_ORDER:
                {
                    // inverse order
                    Angle tmp=x;
                    x=y; y=tmp;
                    break;
                }
                default: throw new AssertionError(); // Should not happen
            }
            /*
             * Tente maintenant de lire l'altitude, sauf si l'utilisateur
             * a sp�cifi�e qu'il n'y a pas d'altitude � lire.
             */
            final int pz=pos.getIndex();
            Number z=null;
            if (dimension!=2)
            {
                final int length=source.length();
                int i=pz; while (i<length && Character.isSpaceChar(source.charAt(i))) i++;
                // Pour ignorer les espaces, on n'utilise pas 'isWhitespace' car on ne veut pas sauter les retours chariots.
                pos.setIndex(i);
                z=angleFormat.parseNumber(source, pos);
                if (z==null)
                {
                    pos.setIndex(pz);
                }
            }
            try
            {
                if (z!=null)
                {
                    pos.setErrorIndex(-1); // Clear error index.
                    return new CoordinatePoint(x,y,z);
                }
                else
                {
                    if (dimension==3)
                    {
                        final int index=pos.getIndex();
                        if (pos.getErrorIndex()<index)
                            pos.setErrorIndex(index);
                        pos.setIndex(px);
                    }
                    else
                    {
                        pos.setIndex(pz);
                        pos.setErrorIndex(-1); // Clear error index.
                    }
                    return new CoordinatePoint(x,y);
                }
            }
            catch (IllegalArgumentException exception)
            {
                pos.setIndex(px);
                pos.setErrorIndex(px);
                ParseException e=new ParseException(exception.getLocalizedMessage(), px);
                e.initCause(exception);
                throw e;
            }
        }
    }

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant une coordonn�e. Cette cha�ne doit contenir
     * une latitude et une longitude (possiblement dans l'ordre inverse), ainsi qu'une altitude
     * facultative. L'objet retourn� sera de la classe {@link CoordinatePoint}.
     *
     * @param  source Cha�ne de caract�res � interpr�ter.
     * @param  pos Position � partir d'o� commencer l'interpr�tation de la cha�ne <code>source</code>.
     * @return La coordonn�e comme objet {@link CoordinatePoint}, ou <code>null</code> si les angles
     *         n'ont pas pu �tre interpr�t�s. <code>null</code> sera aussi retourn� si la cha�ne
     *         contenait deux latitudes ou deux longitudes.
     */
    public CoordinatePoint parse(final String source, final ParsePosition pos)
    {
        try
        {
            final Object coord=parseImpl(source, pos);
            if (pos.getErrorIndex()<0) return (CoordinatePoint) coord;
        }
        catch (ParseException exception)
        {
            // Ignore: il faut seulement retourner 'null'
            //         pour indiquer que 'parse' a �chou�.
        }
        return null;
    }

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant une coordonn�e. Cette cha�ne doit contenir
     * une latitude et une longitude (possiblement dans l'ordre inverse), ainsi qu'une altitude
     * facultative. L'objet retourn� sera de la classe {@link CoordinatePoint}.
     *
     * @param  source Cha�ne de caract�res � interpr�ter.
     * @param  pos Position � partir d'o� commencer l'interpr�tation de la cha�ne <code>source</code>.
     * @return La coordonn�e comme objet {@link CoordinatePoint}.
     * @throws ParseException si les angles n'ont pas pu �tre interpr�t�s, ou si la coordonn�e a pu �tre
     *         interpr�t� mais qu'elle contient deux latitudes ou deux longitudes.
     */
    public CoordinatePoint parse(final String source) throws ParseException
    {
        final ParsePosition pos = new ParsePosition(0);
        final Object      coord = parseImpl(source, pos);
        int error=pos.getErrorIndex();
        if (error>=0)
        {
            /*
             * S'il y a eu une erreur lors de l'interpr�tation de la cha�ne, v�rifie si c'est
             * parce qu'on est arriv� au bout de la cha�ne alors qu'il restait des informations
             * � lire. Si c'est la cas, alors le message d'erreur sera diff�rent de celui qu'on
             * aurait �crit si l'erreur venait de caract�res non-reconnus.
             */
            final int length=source.length();
            do if (error>=length)
            {
                final int missingField;
                     if (coord instanceof CoordinatePoint) missingField=2; // Missing altitude
                else if (coord instanceof Longitude)       missingField=1; // Missing latitude
                else if (coord instanceof Latitude)        missingField=0; // Missing longitude
                else if (coord instanceof Angle)           missingField= (order==XY_ORDER) ? 1 : 0;
                else                                       missingField= (order==XY_ORDER) ? 0 : 1;
                throw new ParseException(Resources.format(Cl�.MISSING_ANGLE�2, new Integer(missingField), source.trim()), error);
            }
            while (Character.isWhitespace(source.charAt(error++)));
        }
        AngleFormat.checkComplete(source, pos, true);
        return (CoordinatePoint) coord;
    }

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant une coordonn�e.
     *
     * @param  source Cha�ne de caract�res � interpr�ter.
     * @param  pos Position � partir d'o� commencer l'interpr�tation de la cha�ne <code>source</code>.
     * @return La coordonn�e comme objet {@link CoordinatePoint}.
     */
    public Object parseObject(final String source, final ParsePosition pos)
    {return parse(source, pos);}

    /**
     * Interpr�te une cha�ne de caract�res repr�sentant une coordonn�e. Cette m�thode
     * est red�finie afin d'obtenir un message d'erreur plus explicite que celui de
     * {@link Format#parseObject} en cas d'erreur.
     *
     * @param  source Cha�ne de caract�res � interpr�ter.
     * @return La coordonn�e comme objet {@link CoordinatePoint}.
     * @throws ParseException si la cha�ne n'a pas �t� compl�tement reconnue.
     */
    public Object parseObject(final String source) throws ParseException
    {return parse(source);}
    
    /**
     * Renvoie un code "hash
     * value" pour cet objet.
     */
    public synchronized int hashCode()
    {return (dimension ^ order) ^ angleFormat.hashCode();}
    
    /**
     * V�rifie si ce format est �gal au format <code>obj</code> sp�cifi�.
     * Ce sera le cas si les deux formats sont de la m�me classe et
     * utilisent le m�me patron ainsi que les m�mes symboles d�cimaux.
     */
    public boolean equals(final Object obj)
    {
        synchronized (angleFormat)
        {
            // On ne peut pas synchroniser "obj" si on ne veut
            // pas risquer un "deadlock". Voir RFE #4210659.
            if (obj==this) return true;
            if (obj!=null && getClass().equals(obj.getClass()))
            {
                final  CoordinateFormat cast            = (CoordinateFormat) obj;
                return order                            == cast.order                            &&
                       dimension                        == cast.dimension                        &&
                       Double.doubleToLongBits(missing) == Double.doubleToLongBits(cast.missing) &&
                       AngleFormat.equals(zSuffix,         cast.zSuffix    )                     &&
                       AngleFormat.equals(zPrefix,         cast.zPrefix    )                     &&
                       AngleFormat.equals(separator,       cast.separator  )                     &&
                       AngleFormat.equals(angleFormat,     cast.angleFormat);
            }
            else return false;
        }
    }
    
    /**
     * Renvoie une copie de ce format.
     */
    public CoordinateFormat clone()
    {return (CoordinateFormat) super.clone();}

    /**
     * Renvoie une repr�sentation de cet objet. Cette
     * m�thode n'est utile qu'� des fins de d�boguage.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+getAnglePattern()+']';}
}
