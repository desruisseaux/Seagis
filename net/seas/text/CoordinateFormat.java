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
 * Ecrit et lit des coordonnées exprimées selon un certain patron. Les coordonnées comprennent
 * une latitude et une longitude (possiblement dans l'ordre inverse) et de façon facultative une
 * altitude. Les coordonnées lues seront placées dans des objets {@link CoordinatePoint}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class CoordinateFormat extends Format
{
    /**
     * Constante pour {@link #setDimension} indiquant que le nombre de
     * dimension des coordonnées pourra être déterminé automatiquement.
     */
    public static final int AUTO_DIMENSION = -1;

    /**
     * Constante pour {@link #setOrder} indiquant que les coordonnées
     * seront écrites dans l'ordre <var>longitude</var>,<var>latitude</var>
     * et éventuellement <var>altitude</var>.
     */
    public static final int XY_ORDER = -2;

    /**
     * Constante pour {@link #setOrder} indiquant que les coordonnées
     * seront écrites dans l'ordre <var>latitude</var>,<var>longitude</var>
     * et éventuellement <var>altitude</var>.
     */
    public static final int YX_ORDER = -3;

    /**
     * Constante pour {@link FieldPosition}
     * désignant le champ de la latitude.
     */
    public static final int LATITUDE_FIELD = 1;

    /**
     * Constante pour {@link FieldPosition}
     * désignant le champ de la longitude.
     */
    public static final int LONGITUDE_FIELD = 2;

    /**
     * Constante pour {@link FieldPosition}
     * désignant le champ de l'altitude.
     */
    public static final int ALTITUDE_FIELD = 3;

    /**
     * Format utilisé pour lire et écrire les angles.
     */
    private final AngleFormat angleFormat;

    /**
     * Chaîne de caractères à utiliser
     * pour séparer les angles entre eux.
     */
    private String separator = " ";

    /**
     * Chaîne de caractères à placer devant l'altitude.
     * Ces caractères sépareront l'altitude du dernier
     * angle écrit.
     */
    private String zPrefix = separator;

    /**
     * Chaîne de caractères à placer après l'altitude.
     * Il s'agira parfois des unités comme "m".
     */
    private String zSuffix = "";

    /**
     * Nombre à écrire à la place de l'altitude lorsqu'elle
     * était requise (<code>dimension==3<code>) mais qu'elle
     * n'était pas spécifiée.
     */
    private double missing = Double.NaN;

    /**
     * Nombre de dimension des coordonnées. Les valeurs
     * permises sont 2, 3 ou {@link #AUTO_DIMENSION}.
     */
    private int dimension = AUTO_DIMENSION;

    /**
     * Ordre des ordonnées (latitude et longitude). Les valeurs
     * permises sont {@link #XY_ORDER} et {@link #YX_ORDER}.
     */
    private int order = YX_ORDER;

    /**
     * Construit un objet par défaut pour
     * lire et écrire des coordonnées.
     */
    public CoordinateFormat()
    {angleFormat=new AngleFormat();}

    /**
     * Construit un objet qui lira des coordonnées en utilisant le patron spécifié pour les angles. La
     * syntaxe du patron est spécifié dans la description de la méthode {@link AngleFormat#applyPattern}.
     *
     * @param  pattern Patron à utiliser pour l'écriture des angles.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public CoordinateFormat(final String pattern) throws IllegalArgumentException
    {angleFormat=new AngleFormat(pattern);}

    /**
     * Construit un objet qui lira des coordonnées en utilisant le patron spécifié pour les angles. La
     * syntaxe du patron est spécifié dans la description de la méthode {@link AngleFormat#applyPattern}.
     *
     * @param  pattern Patron à utiliser pour l'écriture des angles.
     * @param  locale  Pays dont on voudra utiliser les conventions.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public CoordinateFormat(final String pattern, final Locale locale) throws IllegalArgumentException
    {angleFormat=new AngleFormat(pattern, locale);}

    /**
     * Construit un objet qui lira des coordonnées en utilisant le patron spécifié pour les angles. La
     * syntaxe du patron est spécifié dans la description de la méthode {@link AngleFormat#applyPattern}.
     *
     * @param pattern Patron à utiliser pour l'écriture des angles.
     * @param symbols Symboles à utiliser pour représenter les nombres.
     */
    public CoordinateFormat(final String pattern, final DecimalFormatSymbols symbols)
    {angleFormat=new AngleFormat(pattern, symbols);}

    /**
     * Construit un objet qui lira des coordonnées en
     * utilisant le formateur spécifié pour les angles.
     *
     * @param angleFormat Objet à utiliser pour lire et écrire les angles.
     */
    public CoordinateFormat(final AngleFormat angleFormat)
    {this.angleFormat=angleFormat;}


    /**
     * Spécifie un patron pour le format d'écriture des angles. La syntaxe du patron
     * est spécifié dans la description de la méthode {@link AngleFormat#applyPattern}.
     *
     * @param  Patron à utiliser pour les écritures des angles.
     * @throws IllegalArgumentException si le patron n'est pas valide.
     */
    public void setAnglePattern(final String pattern) throws IllegalArgumentException
    {angleFormat.applyPattern(pattern);}

    /**
     * Renvoie le patron utilisé pour les écritures des angles.
     */
    public String getAnglePattern()
    {return angleFormat.toPattern();}

    /**
     * Défini le nombre de dimensions des coordonnées
     * attendues. Les valeurs permises sont:
     *
     * <ul>
     *   <li>2 pour lire des coordonnées qui ne comprennent qu'une latitude et une longitude.</li>
     *   <li>3 pour lire des coordonnées qui comprennent une latitude, une longitude et une altitude.</li>
     *   <li>{@link #AUTO_DIMENSION} pour déterminer automatiquement si une coordonnée comprend une
     *       altitude ou pas. C'est la valeur par défaut.</li>
     * </ul>
     *
     * @throws IllegalArgumentException Si <code>dimension</code> n'est pas une des constantes ci-haut mentionnées.
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
                default: throw new IllegalArgumentException(Resources.format(Clé.ILLEGAL_ARGUMENT¤1, new Integer(dimension)));
            }
        }
    }

    /**
     * Retourne le nombre de dimensions
     * des coordonnées attendues.
     */
    public int getDimension()
    {return dimension;}

    /**
     * Défini l'ordre des ordonnées <var>latitude</var> et
     * <var>longitude</var>. Les valeurs permises sont:
     *
     * <ul>
     *   <li>{@link #XY_ORDER} pour écrire les ordonnées dans l'ordre (<var>longitude</var>,<var>latitude</var>).</li>
     *   <li>{@link #YX_ORDER} pour écrire les ordonnées dans l'ordre (<var>latitude</var>,<var>longitude</var>).
     *       C'est la valeur par défaut.</li>
     * </ul>
     *
     * @throws IllegalArgumentException Si <code>order</code> n'est pas une des constantes ci-haut mentionnées.
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
                default: throw new IllegalArgumentException(Resources.format(Clé.ILLEGAL_ARGUMENT¤1, new Integer(order)));
            }
        }
    }

    /**
     * Retourne l'ordre des ordonnées.
     */
    public int getOrder()
    {return order;}

    /**
     * Procède à l'écriture d'une coordonnée.
     *
     * @param  longitude Longitude en degrés.
     * @param  latitude  Latitude en degrés.
     * @param  toAppendTo Buffer dans lequel écrire la coordonnée.
     * @param  pos Champ (latitude, longitude ou altitude) dont on veut connaître les index dans la chaîne.
     * @return Le buffer dans lequel a été ecrit la coordonnée (habituellement <code>toAppendTo</code>).
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
     * Procède à l'écriture d'une coordonnée.
     *
     * @param  obj Coordonnée à écrire. Il doit s'agir d'un objet {@link CoordinatePoint}.
     * @param  toAppendTo Buffer dans lequel écrire la coordonnée.
     * @param  pos Champ (latitude, longitude ou altitude) dont on veut connaître les index dans la chaîne.
     * @return Le buffer dans lequel a été ecrit la coordonnée (habituellement <code>toAppendTo</code>).
     * @throws IllegalArgumentException si <code>obj</code> n'est pas de la classe {@link CoordinatePoint}.
     */
    public StringBuffer format(final Object obj, StringBuffer toAppendTo, final FieldPosition pos) throws IllegalArgumentException
    {
        if (obj instanceof CoordinatePoint)
        {
            return format((CoordinatePoint)obj, toAppendTo, pos);
        }
        else throw new IllegalArgumentException(Resources.format(Clé.NOT_AN_ANGLE_OBJECT¤2, new Integer(1), XClass.getShortClassName(obj)));
    }

    /**
     * Procède à l'écriture d'une coordonnée.
     *
     * @param  coord Coordonnée à écrire..
     * @param  toAppendTo Buffer dans lequel écrire la coordonnée.
     * @param  pos Champ (latitude, longitude ou altitude) dont on veut connaître les index dans la chaîne.
     * @return Le buffer dans lequel a été ecrit la coordonnée (habituellement <code>toAppendTo</code>).
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
     * Interprète une chaîne de caractères représentant une coordonnée. Cette chaîne doit contenir une
     * latitude et une longitude (possiblement dans l'ordre inverse), ainsi qu'une altitude facultative.
     * En cas de succès, l'objet retourné sera de la classe {@link CoordinatePoint}. En cas d'échec,
     * l'objet retourné sera <code>null</code> si aucun angle n'a pu être lu ou {@link Angle} si un
     * seul angle a pu être lu.
     *
     * La position <code>pos</code> sera laissée à sa position initiale si une des conditions suivantes
     * est remplie:
     *
     * <ul>
     *   <li>La méthode retourne <code>null</code>.</li>
     *   <li>La méthode retourne un objet {@link Angle}.</li>
     *   <li>La méthode retourne un objet {@link CoordinatePoint}, mais l'utilisateur a indiqué
     *       qu'il exigeait aussi une altitude (donc un objet {@link Coordinate3D}).</li>
     *   <li>Une exception {@link ParseException} a été lancée.</li>
     * </ul>
     *
     * @param  source Chaîne de caractères à interpréter.
     * @param  pos Position à partir d'où commencer l'interprétation de la chaîne <code>source</code>.
     * @return La coordonnée comme objet {@link CoordinatePoint} si au moins deux angles ont pu être
     *         interprétés, un objet {@link Angle} si seul le premier angle a pu être interprété ou
     *         <code>null</code> si aucun angle n'a pu être interprété.
     * @throws ParseException si la coordonnée a pu être interprété mais qu'elle contient deux latitudes
     *         ou deux longitudes. Dans ce cas, <code>pos</code> sera remis à sa position initiale.
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
             * Parvenu à ce stade, les deux premiers nombres (la longitude et la
             * latitude) on été lus avec succès. On vérifie maintenant s'il faut
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
             * a spécifiée qu'il n'y a pas d'altitude à lire.
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
     * Interprète une chaîne de caractères représentant une coordonnée. Cette chaîne doit contenir
     * une latitude et une longitude (possiblement dans l'ordre inverse), ainsi qu'une altitude
     * facultative. L'objet retourné sera de la classe {@link CoordinatePoint}.
     *
     * @param  source Chaîne de caractères à interpréter.
     * @param  pos Position à partir d'où commencer l'interprétation de la chaîne <code>source</code>.
     * @return La coordonnée comme objet {@link CoordinatePoint}, ou <code>null</code> si les angles
     *         n'ont pas pu être interprétés. <code>null</code> sera aussi retourné si la chaîne
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
            //         pour indiquer que 'parse' a échoué.
        }
        return null;
    }

    /**
     * Interprète une chaîne de caractères représentant une coordonnée. Cette chaîne doit contenir
     * une latitude et une longitude (possiblement dans l'ordre inverse), ainsi qu'une altitude
     * facultative. L'objet retourné sera de la classe {@link CoordinatePoint}.
     *
     * @param  source Chaîne de caractères à interpréter.
     * @param  pos Position à partir d'où commencer l'interprétation de la chaîne <code>source</code>.
     * @return La coordonnée comme objet {@link CoordinatePoint}.
     * @throws ParseException si les angles n'ont pas pu être interprétés, ou si la coordonnée a pu être
     *         interprété mais qu'elle contient deux latitudes ou deux longitudes.
     */
    public CoordinatePoint parse(final String source) throws ParseException
    {
        final ParsePosition pos = new ParsePosition(0);
        final Object      coord = parseImpl(source, pos);
        int error=pos.getErrorIndex();
        if (error>=0)
        {
            /*
             * S'il y a eu une erreur lors de l'interprétation de la chaîne, vérifie si c'est
             * parce qu'on est arrivé au bout de la chaîne alors qu'il restait des informations
             * à lire. Si c'est la cas, alors le message d'erreur sera différent de celui qu'on
             * aurait écrit si l'erreur venait de caractères non-reconnus.
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
                throw new ParseException(Resources.format(Clé.MISSING_ANGLE¤2, new Integer(missingField), source.trim()), error);
            }
            while (Character.isWhitespace(source.charAt(error++)));
        }
        AngleFormat.checkComplete(source, pos, true);
        return (CoordinatePoint) coord;
    }

    /**
     * Interprète une chaîne de caractères représentant une coordonnée.
     *
     * @param  source Chaîne de caractères à interpréter.
     * @param  pos Position à partir d'où commencer l'interprétation de la chaîne <code>source</code>.
     * @return La coordonnée comme objet {@link CoordinatePoint}.
     */
    public Object parseObject(final String source, final ParsePosition pos)
    {return parse(source, pos);}

    /**
     * Interprète une chaîne de caractères représentant une coordonnée. Cette méthode
     * est redéfinie afin d'obtenir un message d'erreur plus explicite que celui de
     * {@link Format#parseObject} en cas d'erreur.
     *
     * @param  source Chaîne de caractères à interpréter.
     * @return La coordonnée comme objet {@link CoordinatePoint}.
     * @throws ParseException si la chaîne n'a pas été complètement reconnue.
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
     * Vérifie si ce format est égal au format <code>obj</code> spécifié.
     * Ce sera le cas si les deux formats sont de la même classe et
     * utilisent le même patron ainsi que les mêmes symboles décimaux.
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
     * Renvoie une représentation de cet objet. Cette
     * méthode n'est utile qu'à des fins de déboguage.
     */
    public String toString()
    {return XClass.getShortClassName(this)+'['+getAnglePattern()+']';}
}
