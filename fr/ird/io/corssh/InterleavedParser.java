/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le D�veloppement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contact: Michel Petit
 *          Maison de la t�l�d�tection
 *          Institut de Recherche pour le d�veloppement
 *          500 rue Jean-Fran�ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.io.corssh;

// Divers
import java.util.Date;
import java.io.IOException;
import java.io.EOFException;
import java.io.Serializable;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import org.geotools.resources.Utilities;


/**
 * Interpr�teur de fichiers CORSSH combinant les donn�es de deux satellites.
 * Cette classe alternera les lectures entre les deux flots de donn�es de
 * fa�on � toujours retourner les enregistrements en ordre chronologique.
 * Les flots sp�cifi�s peuvent �tre d'autres objets {@link InterleavedParser},
 * ce qui permet de faire une cha�ne de plus de deux flots de donn�es.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class InterleavedParser extends Parser implements Serializable
{
    /**
     * Pour compatibilit�s entre diff�rentes versions de cette classe.
     */
    private static final long serialVersionUID = -5170287153058953650L;

    /**
     * Premier (<code>parser1</code>) et second (<code>parser2</code>)
     * ensembles de donn�es CORSSH. Ces champs ne doivent jamais �tre
     * nuls.
     */
    private final Parser parser1, parser2;

    /**
     * Date des interpr�teurs <code>parser1</code> et <code>parser2</code>,
     * en nombre de millisecondes �coul�s depuis le 1er janvier 1970.  La
     * valeur {@link Long#MIN_VALUE} signifie qu'un interpr�teur n'a plus
     * de donn�es.
     */
    private long time1, time2;

    /**
     * {@link Parser} courant (s�lectionn� lors du dernier appel de
     * {@link #nextRecord}), ou <code>null</code> s'il n'y en a pas.
     */
    private transient Parser current;

    /**
     * Construit un interpr�teur qui combinera les donn�es
     * des deux interpr�teurs sp�cifi�s. Les donn�es seront
     * toujours lus en ordre chronologique.
     */
    public InterleavedParser(final Parser parser1, final Parser parser2)
    {
        this.parser1 = parser1;
        this.parser2 = parser2;
        if (parser1==null || parser2==null || parser1==parser2 || use(parser1,parser2,false))
        {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Retourne un objet {@link Parser} qui combinera
     * les donn�es de tous les interpr�teurs sp�cifi�s.
     */
    public static Parser getInstance(final Parser[] parsers)
    {return getInstance(parsers, 0, parsers.length);}

    /**
     * Construit l'arborescence des objets {@link Parser} qui
     * combinera les donn�es de tous les interpr�teurs sp�cifi�s.
     */
    private static Parser getInstance(final Parser[] parsers, final int lower, final int upper)
    {
        assert(lower<=upper);
        switch (upper-lower)
        {
            case  0: return null;
            case  1: return parsers[lower];
            default: return new InterleavedParser(getInstance(parsers, lower, (lower+upper)/2),
                                                  getInstance(parsers, (lower+upper)/2, upper));
        }
    }

    /**
     * Indique si cet objet utilise au moins un des interpr�teurs
     * <code>check1</code> et <code>check2</code>, directement ou
     * indirectement.
     */
    private boolean use(final Parser check1, final Parser check2, final boolean inclusive)
    {
        if (inclusive)
        {
            if (parser1==check1 || parser2==check2) return true;
            if (parser1==check2 || parser2==check1) return true;
        }
        if (parser1 instanceof InterleavedParser && ((InterleavedParser) parser1).use(check1,check2,true)) return true;
        if (parser2 instanceof InterleavedParser && ((InterleavedParser) parser2).use(check1,check2,true)) return true;
        return false;
    }

    /**
     * Positionne l'interpr�teur � la date sp�cifi�e et retourne la date courante.
     * Cette m�thode retourne {@link Long#MIN_VALUE} si l'interpr�teur n'a pas de
     * donn�es � ou apr�s la date sp�cifi�e.
     */
    private static boolean seek(final Parser parser, final Date date) throws IOException
    {
        final Date endTime = parser.getEndTime();
        if (endTime!=null && !endTime.before(date))
        {
            parser.seek(date);
            return true;
        }
        return false;
    }
    
    /**
     * Positionne le flot au d�but du premier enregistrement dont la date est �gale ou sup�rieure � la
     * date sp�cifi�e. Apr�s l'appel de cette m�thode, l'enregistrement courant sera celui qui pr�c�dait
     * imm�diatement l'enregistrement recherch�.
     *
     * @param  date Date � laquelle on souhaite positionner le flot.
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws EOFException si des enregistrements manquent dans le fichier, ou si
     *         la date demand�e est post�rieure aux dates de tous les enregistrements
     *         trouv�s dans le fichier.
     */
    public void seek(final Date date) throws IOException
    {
        final boolean find1, find2;
        current = null;
        time1   = Long.MIN_VALUE;
        time2   = Long.MIN_VALUE;
        find1   = seek(parser1, date);
        find2   = seek(parser2, date);
        time1   = find1 ? parser1.getTime() : Long.MIN_VALUE;
        time2   = find2 ? parser2.getTime() : Long.MIN_VALUE;
        /*
         * Identifie le {@link Parser} qui a la date la plus r�cente.
         * Si les deux {@link Parser} ont la m�me date,  on choisira
         * arbitrairement {@link #parser2}.
         */
        if (find1)
        {
            if (find2)
            {
                // Special case if record position has been set BEFORE first record
                // (i.e. the requested date is before any available record's date).
                // In this case,  we must select the earliest record instead of the
                // latest. Note that if only one one record time is MIN_VALUE, then
                // the second algorithm below (select the latest date) will work as
                // expected.
                if (time1==Long.MIN_VALUE && time2==Long.MIN_VALUE)
                {
                    final Date start1 = parser1.getStartTime();
                    final Date start2 = parser2.getStartTime();
                    if (start1==null && start2==null)
                    {
                        throw new EOFException(Resources.format(ResourceKeys.ERROR_NO_DATA_AFTER_DATE_$1, date));
                    }
                    if (start2==null || (start1!=null && start1.before(start2)))
                    {
                        current = parser1;
                        time2   = nextTime(parser2);
                    }
                    else
                    {
                        current = parser2;
                        time1   = nextTime(parser1);
                    }
                    return;
                }
                // Performs comparaison and select the latest record.
                // The selected record will be advanced during the next
                // 'nextRecord()' call. The other (non-selected) record
                // must be set on its first valid record now (no matter
                // if its date is before or after the requested date).
                if (time1 <= time2)
                {
                    current = parser2;
                    time1   = nextTime(parser1);
                }
                else
                {
                    current = parser1;
                    time2   = nextTime(parser2);
                }
                return;
            }
            current = parser1;
            return;
        }
        if (find2)
        {
            current = parser2;
            return;
        }
        throw new EOFException(Resources.format(ResourceKeys.ERROR_NO_DATA_AFTER_DATE_$1, date));
    }

    /**
     * Proc�de � la lecture de l'enregistrement suivant. Les en-t�tes seront automatiquement saut�s.
     * Cette m�thode retourne <code>true</code> si un enregistrement complet a �t� obtenu.
     *
     * @return <code>true</code> si l'enregistrement a �t� lu, ou <code>false</code>
     *         s'il ne restait plus d'enregistrements � lire.
     * @throws EOFException si un d�but d'enregistrement fut trouv� mais n'est pas complet.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public boolean nextRecord() throws IOException
    {
        if (current==null)
        {
            time1 = nextTime(parser1);
            time2 = nextTime(parser2);
        }
        else
        {
            if (current.nextRecord())
            {
                if (current==parser1) time1=parser1.getTime();
                if (current==parser2) time2=parser2.getTime();
            }
            else
            {
                if (current==parser1) time1=Long.MIN_VALUE;
                if (current==parser2) time2=Long.MIN_VALUE;
            }
        }
        /*
         * Identifie l'objet {@link Parser}  qui contient l'enregistrement
         * le plus ancien. Si les deux {@link Parser} ont la m�me date, on
         * choisira arbitrairement {@link #parser1}.
         */
        if (time1 != Long.MIN_VALUE)
        {
            if (time2 != Long.MIN_VALUE)
            {
                current = (time1 <= time2) ? parser1 : parser2;
                return true;
            }
            current = parser1;
            return true;
        }
        if (time2 != Long.MIN_VALUE)
        {
            current = parser2;
            return true;
        }
        current=null;
        return false;
    }

    /**
     * Avance l'interpr�teur sp�cifi� d'un enregistrement et retourne
     * la date de l'enregistrement courant, ou {@link Long#MIN_VALUE}
     * s'il n'y en a plus.
     */
    private static long nextTime(final Parser parser) throws IOException
    {return parser.nextRecord() ? parser.getTime() : Long.MIN_VALUE;}

    /**
     * Indique si l'enregistrement courant est blanc. Un enregistrement est consid�r� blanc
     * si tous ses champs (tels que retourn�s par {@link #getField}) ont la valeur 0.
     */
    public boolean isBlank()
    {return current==null || current.isBlank();}

    /**
     * Retourne la valeur cod�e du champ sp�cifi�.
     */
    public int getField(final int field)
    {return (current!=null) ? current.getField(field) : 0;}

    /**
     * Retourne la valeur r�elle du champ sp�cifi�.
     */
    public double getValue(final int field)
    {return (current!=null) ? current.getValue(field) : 0;}

    /**
     * Retourne la date cod�e dans l'enregistrement courant,   en nombre de secondes
     * �coul�s depuis le 1er janvier 1970. Si la date n'est pas disponible, retourne
     * {@link Long#MIN_VALUE}.
     *
     * Note: Si cette classe devait devenir public, il serait sage de supprimer cette
     *       red�finition et de se fier plut�t � l'impl�mentation par d�faut, qui est
     *       valide m�me lorsque l'utilisateur red�finie {@link #getDate}.
     */
    final long getTime()
    {
        if (current==parser1) return time1;
        if (current==parser2) return time2;
        return Long.MIN_VALUE;
    }

    /**
     * Retourne la date cod�e dans l'enregistrement courant.
     */
    public Date getDate()
    {
        final long time = getTime();
        final Date date = (time!=Long.MIN_VALUE) ? new Date(time) : null;
        assert Utilities.equals(date, current.getDate());
        return date;
    }

    /**
     * Retourne la date du premier enregistrement.
     *
     * @return La date du premier enregistrement, ou <code>null</code> s'il n'y a pas de donn�es.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public Date getStartTime() throws IOException
    {
        final Date startTime1 = parser1.getStartTime();
        final Date startTime2 = parser2.getStartTime();
        if (startTime1==null) return startTime2;
        if (startTime2==null) return startTime1;
        return (startTime1.before(startTime2)) ? startTime1 : startTime2;
    }

    /**
     * Retourne la date du dernier enregistrement.
     *
     * @return La date du dernier enregistrement, ou <code>null</code> s'il n'y a pas de donn�es.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public Date getEndTime() throws IOException
    {
        final Date endTime1 = parser1.getEndTime();
        final Date endTime2 = parser2.getEndTime();
        if (endTime1==null) return endTime2;
        if (endTime2==null) return endTime1;
        return (endTime1.after(endTime2)) ? endTime1 : endTime2;
    }

    /**
     * Retourne le nombre de passes que contient le fichier.
     *
     * @return Le nombre de passe dans le fichier courant.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public int getPassCount() throws IOException
    {return parser1.getPassCount() + parser2.getPassCount();}

    /**
     * Retourne le nombre d'enregistrements que contient le fichier,
     * en excluant les enregistrements d'en-t�te.
     *
     * @return Le nombre de passe dans le fichier courant.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public long getRecordCount() throws IOException
    {return parser1.getRecordCount() + parser2.getRecordCount();}

    /**
     * Ferme le flot qui fournissait les donn�es. Apr�s l'appel de
     * cette m�thode, {@link #nextRecord} ne pourra plus �tre utilis�e.
     *
     * @throws IOException si une erreur est survenue lors de la fermeture.
     */
    public void close() throws IOException
    {
        parser1.close();
        parser2.close();
    }

    /**
     * Retourne une cha�ne de caract�res repr�sentant cet enregistrement.
     */
    public String toString()
    {return (current!=null) ? current.toString() : Resources.format(ResourceKeys.BLANK);}
}
