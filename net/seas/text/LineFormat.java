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
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;

// Miscellaneous
import java.util.Arrays;
import net.seas.util.XArray;
import net.seas.util.ClassChanger;
import net.seas.resources.Resources;


/**
 * Classe facilitant la lecture de lignes de données en format texte. Le plus souvent,
 * on utilise cette classe pour lire les lignes d'une matrice. Certaines colonnes pourraient toutefois
 * contenir autre chose que des nombres. L'exemple ci-dessous créé un objet qui attendra des dates dans
 * la première colonne et des nombres dans les toutes autres.
 *
 * <blockquote><pre>
 * &nbsp;final LineParser parser=new LineFormat(new Format[]
 * &nbsp;{
 * &nbsp;    {@link java.text.DateFormat#getDateTimeInstance()},
 * &nbsp;    {@link java.text.NumberFormat#getNumberInstance()}
 * &nbsp;});
 * </pre></blockquote>
 *
 * On peut utiliser <code>LineFormat</code> pour lire une matrice dont on ignore le nombre
 * de colonnes, tout en imposant (si désirée) la contrainte que toutes les lignes aient le même nombre de
 * colonnes. L'exemple ci-dessous obtient le nombre de colonnes lors de la lecture de la première ligne. Si
 * une des lignes suivantes n'a pas le nombre requis de colonnes, une exception {@link ParseException} sera
 * lancée. La vérification du nombre de colonnes est faite par la méthode <code>getValues(double[])</code>
 * lorsque le tableau <code>data</code> n'est plus nul.
 *
 * <blockquote><pre>
 * &nbsp;double[] data=null;
 * &nbsp;final {@link java.io.BufferedReader} in=new {@link java.io.BufferedReader}(new {@link java.io.FileReader}("MATRIX.TXT")),
 * &nbsp;for ({@link String} line; (line=in.readLine())!=null;)
 * &nbsp;{
 * &nbsp;    parser.setLine(line);
 * &nbsp;    data=parser.getValues(data);
 * &nbsp;    // ... process 'data' here ...
 * &nbsp;});
 * </pre></blockquote>
 *
 * Ce code fonctionnera même si la première colonne contenait des dates. Dans ce cas, ce sera
 * le nombre de millisecondes écoulés depuis le 1er janvier 1970 qui sera mémorisé. Les conversions des objets
 * {@link java.util.Date} en nombres est assurée par {@link ClassChanger}, qui peut aussi convertir d'autres
 * types d'objets.
 * <br><br>
 * Une exception {@link ParseException} peut être lancée parce qu'une chaîne de caractère n'a
 * pas pu être interprétée, parce qu'un objet n'a pas pu être converti en nombre ou parce que
 * le nombre de colonnes trouvées n'était pas le nombre attendu. Dans tous les cas, il est
 * possible d'obtenir l'index de la partie fautive de la ligne avec
 * {@link ParseException#getErrorOffset}.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class LineFormat
{
    /**
     * Nombre de données valides dans le tableau {@link #data}.
     * Il s'agit du nombre de données lues lors du dernier appel
     * de la méthode {@link #setLine(String)}.
     */
    private int count;

    /**
     * Données lus lors du dernier appel de la méthode {@link #setLine(String)}.
     * Ces données seront restitués par des appels à {@link #getValues(float[])}.
     */
    private Object[] data;

    /**
     * Tableau de formats à utiliser. Chaque format de ce tableau correspond à une
     * colonne. Par exemple la donnée <code>data[4]</code> aura été lu avec le format
     * <code>format[4]</code>. Il n'est toutefois pas obligatoire qu'il y ait autant
     * de format que de colonnes. Si {@link #data} et plus long que {@link #format},
     * alors le dernier format sera réutilisé pour toutes les colonnes restantes.
     */
    private final Format[] format;

    /**
     * Objet {@link ParsePosition} utilisé lors de la lecture pour spécifier quelle
     * partie de la chaîne doit être interprétée.
     */
    private final ParsePosition position=new ParsePosition(0);

    /**
     * Index du caractère auquel commençaient les éléments qui ont été lus. Par exemple
     * <code>index[0]</code> contient l'index du premier caractère qui a été lu pour la
     * donnée <code>data[0]</code>, et ainsi de suite. Ce tableau doit <u>toujours</u>
     * avoir une longueur de <code>{@link #data}.length + 1</code>. Le dernier élément
     * de ce tableau sera la longueur de la ligne.
     */
    private int[] limits;

    /**
     * Dernière ligne de texte à avoir été spécifiée à la méthode {@link #setLine(String)}.
     */
    private String line;

    /**
     * Construit un objet qui lira des nombres
     * écrits selon les conventions locales.
     */
    public LineFormat()
    {this(NumberFormat.getNumberInstance());}

    /**
     * Construit un objet qui lira des nombres écrits selon les convention du
     * pays spécifié. Par exemple on peut spécifier {@link Locale#US} pour lire
     * des nombres qui utilisent le point comme séparateur décimal.
     */
    public LineFormat(final Locale locale)
    {this(NumberFormat.getNumberInstance(locale));}

    /**
     * Construit un objet qui lira des dates, des nombres ou
     * tous autres objets écrits selon le format spécifié.
     *
     * @param format Format à utiliser.
     * @throws NullPointerException si <code>format</code> est nul.
     */
    public LineFormat(final Format format) throws NullPointerException
    {
        this.data   = new Object[16];
        this.limits = new int   [16+1];
        this.format = new Format[] {format};
        if (format==null)
        {
            final Integer one=new Integer(1);
            throw new NullPointerException(Resources.format(Clé.NULL_FORMAT¤2, one, one));
        }
    }

    /**
     * Construit un objet qui lira des dates, des nombres ou tous autres objets écrits selon
     * les formats spécifiés. Le tableau de format spécifié en argument donne les formats
     * attendus des premières colonnes. Par exemple <code>formats[0]</code> donne le format
     * de la première colonne, <code>formats[1]</code> donne le format de la deuxième colonne,
     * etc. S'il y a plus de colonnes que de formats spécifiés, le dernier format sera réutilisé
     * pour toutes les colonnes restantes.
     *
     * @param formats Tableau de formats à utiliser.
     * @throws NullPointerException si <code>formats</code> est nul ou si si un des formats est nul.
     */
    public LineFormat(final Format[] formats) throws NullPointerException
    {
        this.data   = new Object[formats.length];
        this.format = new Format[formats.length];
        this.limits = new int   [formats.length+1];
        System.arraycopy(formats, 0, format, 0, formats.length);
        for (int i=0; i<format.length; i++)
            if (format[i]==null)
                throw new NullPointerException(Resources.format(Clé.NULL_FORMAT¤2, new Integer(i+1), new Integer(format.length)));
    }

    /**
     * Oublie toute les données mémorisées. Le prochain appel
     * de la méthode {@link #getValueCount} retournera 0.
     */
    public synchronized void clear()
    {
        line=null;
        Arrays.fill(data, null);
        count=0;
    }

    /**
     * Défini la prochaine ligne qui sera à interpréter.
     *
     * @param  line Ligne à interpréter.
     * @return Nombre d'éléments trouvés dans la ligne. Cette information peut
     *         aussi être obtenue par un appel à {@link #getValueCount}.
     * @throws ParseException si des éléments n'ont pas pu être interprétés.
     */
    public int setLine(final String line) throws ParseException
    {return setLine(line, 0, line.length());}

    /**
     * Défini la prochaine ligne qui sera à interpréter.
     *
     * @param  line  Ligne à interpréter.
     * @param  lower Index du premier caractère de <code>line</code> à prendre en compte.
     * @param  upper Index suivant celui du dernier caractère de <code>line</code> à prendre en compte.
     * @return Nombre d'éléments trouvés dans la ligne. Cette information peut
     *         aussi être obtenue par un appel à {@link #getValueCount}.
     * @throws ParseException si des éléments n'ont pas pu être interprétés.
     */
    public synchronized int setLine(final String line, int lower, final int upper) throws ParseException
    {
        /*
         * Retient la ligne que l'utilisateur nous demande
         * de lire et oublie toutes les anciennes valeurs.
         */
        this.line=line;
        Arrays.fill(data, null);
        count=0;
        /*
         * Procède au balayage de toutes les valeurs qui se trouvent sur la ligne spécifiée.
         * Le balayage s'arrêtera lorsque <code>lower</code> aura atteint <code>upper</code>.
         */
  load: while (true)
        {
            while (true)
            {
                if (lower>=upper) break load;
                if (!Character.isWhitespace(line.charAt(lower))) break;
                lower++;
            }
            /*
             * Procède à la lecture de la donnée. Si la lecture échoue, on produira un message d'erreur
             * qui apparaîtra éventuellement en HTML afin de pouvoir souligner la partie fautive.
             */
            position.setIndex(lower);
            final Object datum=format[Math.min(count, format.length-1)].parseObject(line, position);
            final int next=position.getIndex();
            if (datum==null || next<=lower)
            {
                final int error=position.getErrorIndex();
                int end=error;
                while (end<upper && !Character.isWhitespace(line.charAt(end))) end++;
                throw new ParseException(Resources.format(Clé.PARSE_EXCEPTION¤2, line.substring(lower, end).trim(), line.substring(error, Math.min(error+1, end))), error);
            }
            /*
             * Mémorise la nouvelle donnée, en agrandissant
             * l'espace réservée en mémoire si c'est nécessaire.
             */
            if (count>=data.length)
            {
                data   = XArray.resize(data,   count+Math.min(count, 256));
                limits = XArray.resize(limits, data.length+1);
            }
            limits[count]=lower;
            data[count++]=datum;
            lower=next;
        }
        limits[count]=lower;
        return count;
    }

    /**
     * Retourne le nombre de données trouvées dans la dernière
     * ligne à avoir été spécifiée à {@link #setLine(String)}.
     */
    public synchronized int getValueCount()
    {return count;}

    /**
     * Modifie ou ajoute une valeur. L'index de la valeur doit être compris de
     * 0 à {@link #getValueCount} inclusivement. Si l'index est égal au nombre
     * de données retourné par {@link #getValueCount}, alors <code>value</code>
     * sera ajouté à la fin des données existante et une colonne sera ajoutée.
     *
     * @param  index Index de la donnée à modifier ou ajouter.
     * @param  Value Nouvelle valeur à retenir.
     * @throws ArrayIndexOutOfBoundsException si l'index est en dehors de la plage permise.
     */
    public synchronized void setValue(final int index, final Object value) throws ArrayIndexOutOfBoundsException
    {
        if (index>count)
        {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        if (value==null)
        {
            throw new NullPointerException();
        }
        if (index==count)
        {
            if (index==data.length)
            {
                data = XArray.resize(data, index+Math.min(index, 256));
            }
            count++;
        }
        data[index]=value;
    }

    /**
     * Retourne la valeur à l'index spécifié. Cet index doit être
     * compris de 0 inclusivement jusqu'à {@link #getValueCount}
     * exclusivement.
     *
     * @param  index Index de la donnée demandée.
     * @return Valeur à l'index demandé.
     * @throws ArrayIndexOutOfBoundsException si l'index est en dehors de la plage permise.
     */
    public synchronized Object getValue(final int index) throws ArrayIndexOutOfBoundsException
    {
        if (index<count) return data[index];
        throw new ArrayIndexOutOfBoundsException(index);
    }

    /**
     * Retourne sous forme de nombre la valeur à l'index <code>index</code>.
     *
     * @param  index Index de la valeur demandée.
     * @return La valeur demandée sous forme d'objet {@link Number}.
     * @throws ParseException si la valeur n'est pas convertible en objet {@link Number}.
     */
    private Number getNumber(final int index) throws ParseException
    {
        final Number number=ClassChanger.toNumber(data[index]);
        if (number!=null)
        {
            return number;
        }
        else throw new ParseException(Resources.format(Clé.UNPARSABLE_NUMBER¤1, data[index]), limits[index]);
    }

    /**
     * Copie vers le tableau spécifié les valeurs lues dans la ligne. Cette méthode peut être
     * appelée après {@link #setLine(String)} pour copier vers <code>array</code> les valeurs
     * qui ont été lues. Si <code>array</code> est nul, cette méthode créera et retournera un
     * tableau qui aura la longueur tout juste suffisante pour contenir toutes les données.
     * Mais si <code>array</code> est non-nul, alors cette méthode exigera que la longueur du
     * tableau soit égale au nombre de données.
     *
     * @param  array Tableau dans lequel copier les valeurs.
     * @return <code>array</code> s'il était non-nul, ou un tableau nouvellement
     *         créé avec la bonne longueur si <code>array</code> était nul.
     * @throws ParseException si <code>array</code> était non-nul et que sa longueur
     *         ne correspond pas au nombre de données lues, ou si une des données lues
     *         n'est pas convertible en nombre.
     */
    public synchronized double[] getValues(double[] array) throws ParseException
    {
        if (array!=null)
            checkLength(array.length);
        else
            array=new double[count];
        for (int i=0; i<count; i++)
            array[i]=getNumber(i).doubleValue();
        return array;
    }

    /**
     * Copie vers le tableau spécifié les valeurs lues dans la ligne. Cette méthode peut être
     * appelée après {@link #setLine(String)} pour copier vers <code>array</code> les valeurs
     * qui ont été lues. Si <code>array</code> est nul, cette méthode créera et retournera un
     * tableau qui aura la longueur tout juste suffisante pour contenir toutes les données.
     * Mais si <code>array</code> est non-nul, alors cette méthode exigera que la longueur du
     * tableau soit égale au nombre de données.
     *
     * @param  array Tableau dans lequel copier les valeurs.
     * @return <code>array</code> s'il était non-nul, ou un tableau nouvellement
     *         créé avec la bonne longueur si <code>array</code> était nul.
     * @throws ParseException si <code>array</code> était non-nul et que sa longueur
     *         ne correspond pas au nombre de données lues, ou si une des données lues
     *         n'est pas convertible en nombre.
     */
    public synchronized float[] getValues(float[] array) throws ParseException
    {
        if (array!=null)
            checkLength(array.length);
        else
            array=new float[count];
        for (int i=0; i<count; i++)
            array[i]=getNumber(i).floatValue();
        return array;
    }

    /**
     * Copie vers le tableau spécifié les valeurs lues dans la ligne. Cette méthode peut être
     * appelée après {@link #setLine(String)} pour copier vers <code>array</code> les valeurs
     * qui ont été lues. Si <code>array</code> est nul, cette méthode créera et retournera un
     * tableau qui aura la longueur tout juste suffisante pour contenir toutes les données.
     * Mais si <code>array</code> est non-nul, alors cette méthode exigera que la longueur du
     * tableau soit égale au nombre de données.
     *
     * @param  array Tableau dans lequel copier les valeurs.
     * @return <code>array</code> s'il était non-nul, ou un tableau nouvellement
     *         créé avec la bonne longueur si <code>array</code> était nul.
     * @throws ParseException si <code>array</code> était non-nul et que sa longueur
     *         ne correspond pas au nombre de données lues, ou si une des données lues
     *         n'est pas convertible en nombre.
     */
    public synchronized long[] getValues(long[] array) throws ParseException
    {
        if (array!=null)
            checkLength(array.length);
        else
            array=new long[count];
        for (int i=0; i<count; i++)
            array[i]=getNumber(i).longValue();
        return array;
    }

    /**
     * Copie vers le tableau spécifié les valeurs lues dans la ligne. Cette méthode peut être
     * appelée après {@link #setLine(String)} pour copier vers <code>array</code> les valeurs
     * qui ont été lues. Si <code>array</code> est nul, cette méthode créera et retournera un
     * tableau qui aura la longueur tout juste suffisante pour contenir toutes les données.
     * Mais si <code>array</code> est non-nul, alors cette méthode exigera que la longueur du
     * tableau soit égale au nombre de données.
     *
     * @param  array Tableau dans lequel copier les valeurs.
     * @return <code>array</code> s'il était non-nul, ou un tableau nouvellement
     *         créé avec la bonne longueur si <code>array</code> était nul.
     * @throws ParseException si <code>array</code> était non-nul et que sa longueur
     *         ne correspond pas au nombre de données lues, ou si une des données lues
     *         n'est pas convertible en nombre.
     */
    public synchronized int[] getValues(int[] array) throws ParseException
    {
        if (array!=null)
            checkLength(array.length);
        else
            array=new int[count];
        for (int i=0; i<count; i++)
            array[i]=getNumber(i).intValue();
        return array;
    }

    /**
     * Copie vers le tableau spécifié les valeurs lues dans la ligne. Cette méthode peut être
     * appelée après {@link #setLine(String)} pour copier vers <code>array</code> les valeurs
     * qui ont été lues. Si <code>array</code> est nul, cette méthode créera et retournera un
     * tableau qui aura la longueur tout juste suffisante pour contenir toutes les données.
     * Mais si <code>array</code> est non-nul, alors cette méthode exigera que la longueur du
     * tableau soit égale au nombre de données.
     *
     * @param  array Tableau dans lequel copier les valeurs.
     * @return <code>array</code> s'il était non-nul, ou un tableau nouvellement
     *         créé avec la bonne longueur si <code>array</code> était nul.
     * @throws ParseException si <code>array</code> était non-nul et que sa longueur
     *         ne correspond pas au nombre de données lues, ou si une des données lues
     *         n'est pas convertible en nombre.
     */
    public synchronized short[] getValues(short[] array) throws ParseException
    {
        if (array!=null)
            checkLength(array.length);
        else
            array=new short[count];
        for (int i=0; i<count; i++)
            array[i]=getNumber(i).shortValue();
        return array;
    }

    /**
     * Copie vers le tableau spécifié les valeurs lues dans la ligne. Cette méthode peut être
     * appelée après {@link #setLine(String)} pour copier vers <code>array</code> les valeurs
     * qui ont été lues. Si <code>array</code> est nul, cette méthode créera et retournera un
     * tableau qui aura la longueur tout juste suffisante pour contenir toutes les données.
     * Mais si <code>array</code> est non-nul, alors cette méthode exigera que la longueur du
     * tableau soit égale au nombre de données.
     *
     * @param  array Tableau dans lequel copier les valeurs.
     * @return <code>array</code> s'il était non-nul, ou un tableau nouvellement
     *         créé avec la bonne longueur si <code>array</code> était nul.
     * @throws ParseException si <code>array</code> était non-nul et que sa longueur
     *         ne correspond pas au nombre de données lues, ou si une des données lues
     *         n'est pas convertible en nombre.
     */
    public synchronized byte[] getValues(byte[] array) throws ParseException
    {
        if (array!=null)
            checkLength(array.length);
        else
            array=new byte[count];
        for (int i=0; i<count; i++)
            array[i]=getNumber(i).byteValue();
        return array;
    }

    /**
     * Vérifie si le nombre de données lues correspond au nombre de données
     * attendues. Si ce n'est pas le cas, une exception sera lancée.
     *
     * @throws ParseException si le nombre de données lues ne correspond pas au nombre de données attendues.
     */
    private void checkLength(final int expected) throws ParseException
    {
        if (count!=expected)
        {
            final int lower=limits[Math.min(count, expected  )];
            final int upper=limits[Math.min(count, expected+1)];
            throw new ParseException(Resources.format(count<expected ? Clé.LINE_TOO_SHORT¤2 : Clé.LINE_TOO_LONG¤3,
                    new Integer(count), new Integer(expected), line.substring(lower,upper).trim()), lower);
        }
    }

    /**
     * Retourne les données sous forme de chaîne de caractères. Toutes les données seront formatées
     * en utilisant les formats déclarés au constructeur. Les colonnes seront séparées par des
     * tabulations. Il n'y aura pas de retour chariot à la fin de la ligne.
     */
    public String toString()
    {
        final FieldPosition field=new FieldPosition(0);
        StringBuffer buffer=new StringBuffer();
        for (int i=0; i<count; i++)
        {
            if (i!=0) buffer.append('\t');
            buffer=format[Math.min(format.length-1, i)].format(data[i], buffer, field);
        }
        return buffer.toString();
    }
}
