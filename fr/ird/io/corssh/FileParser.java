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

// Entr�s/sorties
import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.FileImageInputStream;

// Temps et formattage
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;

// Divers...
import java.awt.Shape;
import java.util.Arrays;
import java.util.Iterator;

// Geotools
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Impl�mentation de la lecture d'un fichier CORSSH binaire.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class FileParser extends Parser {
    /**
     * Longueur des enregistrements des
     * fichiers AVISO, en nombre d'octets.
     *
     * @see #nextRecord
     */
    private static final int RECORD_LENGTH = 8*4;

    /**
     * L'ann�e de l'�poch des dates
     * cod�es selon le format AVISO.
     *
     * @see #getDate
     */
    private static final int EPOCH_YEAR = 1950;

    /**
     * Intervalle de temps typique entre deux enregistrements. Cette information
     * est approximative. Elle sera � acc�l�rer le positionnement du flot sur un
     * enregistrement correspondant � une date pr�cise.
     */
    private static final int TIME_INTERVAL = 1000;

    /**
     * Dernier enregistrement de
     * donn�es � avoir �t� lu.
     *
     * @see #nextRecord
     * @see #getField
     * @see #getDate
     */
    private final byte[] record=new byte[RECORD_LENGTH];

    /**
     * Flot qui fournira les donn�es binaires des enregistrements.
     */
    private final ImageInputStream in;

    /**
     * Nom d�signant le fichier ouvert. Il n'est pas obligatoire que ce nom soit complet.
     * Il ne sera utilis� que pour la production d'un �ventuel message d'erreur. Il peut
     * �tre nul s'il n'est pas connu.
     */
    private final String filename;

    /**
     * Position du d�but de flot. Le plus
     * souvent, �a sera la position 0.
     */
    private final long origine;

    /**
     * Nombre d'enregistrements restant
     * � lire avant le prochain en-t�te.
     */
    private int recordLeft;

    /**
     * Nombre d'enregistrements (sans les en-t�tes) que contient le fichier {@link #in}.
     * Cette information est obtenue lorsque n�cessaire par la m�thode {@link #count}.
     */
    private long recordCount;

    /**
     * Nombre de passes que contient le fichier {@link #in}. Cette information
     * est obtenue lorsque n�cessaire par la m�thode {@link #count}.
     */
    private int passCount;

    /**
     * Date et heure du premier enregistrement, en nombre de
     * millisecondes �coul�es depuis le 1er janvier 1970 UTC.
     * La valeur {@link Long#MAX_VALUE} indique que cette
     * propri�t� n'a pas encore �t� d�finie.
     */
    private long startTime=Long.MAX_VALUE;

    /**
     * Date et heure du dernier enregistrement, en nombre de
     * millisecondes �coul�es depuis le 1er janvier 1970 UTC.
     * La valeur {@link Long#MIN_VALUE} indique que cette
     * propri�t� n'a pas encore �t� d�finie.
     */
    private long endTime=Long.MIN_VALUE;

    /**
     * Objet utilis� pour �crire des valeurs. Cet objet est utilis� par la m�thode {@link #toString}
     * pour produire une cha�ne de caract�res repr�sentant un enregistrement avec ses valeurs.
     */
    private transient NumberFormat numberFormat;

    /**
     * Objet utilis� pour �crire des coordonn�es. Cet objet est utilis� par la m�thode {@link #toString}
     * pour produire une cha�ne de caract�res repr�sentant un enregistrement avec ses valeurs.
     */
    private transient NumberFormat angleFormat;

    /**
     * Objet utilis� pour �crire des dates. Cet objet est utilis� par la m�thode {@link #toString}
     * pour produire une cha�ne de caract�res repr�sentant un enregistrement avec sa date.
     */
    private transient DateFormat dateFormat;

    /**
     * Calendrier utilis� pour convertir des dates du syst�me AVISO vers la convention du Java.
     * Le Java exprime les dates en nombre de millisecondes �coul�es depuis le 1er janvier 1970.
     *
     * @see #getDate
     */
    private final Calendar calendar=new GregorianCalendar(TimeZone.getTimeZone("UTC"));

    /**
     * Construit un interpr�teur qui lira le fichier sp�cifi�. Apr�s la construction
     * de cet objet, les enregistrements du fichiers peuvent �tre lus par des appels
     * � {@link #nextRecord}.
     *
     * @param in Fichier CORSSH � lire.
     * @throws IOException si une erreur d'entr�/sortie est survenue.
     */
    public FileParser(final File in) throws IOException {
        this(new FileImageInputStream(in), in.getPath());
    }

    /**
     * Construit un interpr�teur qui lira le fichier sp�cifi�. Apr�s la construction
     * de cet objet, les enregistrements du fichiers peuvent �tre lus par des appels
     * � {@link #nextRecord}.
     *
     * @param  in Flot qui fournira les donn�es binaires des enregistrements.
     * @param  filename Nom d�signant le fichier ouvert. Il n'est pas obligatoire
     *         que ce nom soit complet. Il ne sera utilis� que pour la production
     *         d'un �ventuel message d'erreur. Il peut �tre nul s'il n'est pas connu.
     * @throws IOException si une erreur d'entr�/sortie est survenue.
     */
    private FileParser(final ImageInputStream in, final String filename) throws IOException {
        this.in=in;
        this.filename=filename;
        origine = in.getStreamPosition();
        log("<init>", getResource(ResourceKeys.OPEN_$1));
    }

    /**
     * Retourne le nom du fichier, ou
     * "(sans nom)" s'il n'a pas �t� nomm�.
     */
    private final String getFilename() {
        return (filename!=null) ? filename : Resources.format(ResourceKeys.UNNAMED);
    }

    /**
     * Retourne la ressource identifi�e par la cl� sp�cifi�e.
     * Le nom de fichier {@link #filename} sera utilis� pour
     * la construction de la ressource.
     */
    private final String getResource(final int cl�) {
        return Resources.format(cl�, getFilename());
    }

    /**
     * Retourne la ressource identifi�e par la cl� sp�cifi�e.
     * Le nom de fichier {@link #filename} sera utilis� pour
     * la construction de la ressource.
     */
    private final String getResource(final int cl�, Object arg) {
        if (arg instanceof Date) {
            arg = getDateTimeInstance().format(arg);
        }
        return Resources.format(cl�, getFilename(), arg);
    }

    /**
     * Positionne le flot au d�but du premier enregistrement dont la date est �gale ou sup�rieure � la
     * date sp�cifi�e. Apr�s l'appel de cette m�thode, cet objet {@link Parser} se trouvera dans l'�tat
     * suivant:
     *
     * <ul>
     *   <li>L'enregistrement courant sera celui qui pr�c�dait imm�diatement l'enregistrement recherch�.
     *       On peut donc utiliser la m�thode {@link #getField} pour conna�tre les propri�t�s du dernier
     *       enregistrement avant la date <code>date</code> sp�cifi�e.   Si ce dernier enregistrement se
     *       trouvait sur la passe pr�c�dente, l'en-t�te aura �t� automatiquement saut�.   Si ce dernier
     *       enregistrement est inconnu (par exemple parce qu'il ne se trouve pas sur ce fichier), alors
     *       tous les champs de l'enregistrement courant auront la valeur 0. Ce dernier �tat peut �tre
     *       test� en appelant la m�thode {@link #isBlank}.</li>
     *   <li>Le prochain appel de la m�thode {@link #nextRecord} retournera le premier enregistrement
     *       � ou apr�s la date <code>date</code> sp�cifi�e.  Les appels subs�quents retourneront les
     *       enregistrements suivants, comme d'habitude.</li>
     * </ul>
     *
     * Notez que cette m�thode suppose que tous les enregistrements
     * apparaissent dans le fichier en ordre croissant de dates. Si
     * ce n'est pas le cas, alors le r�sultat de cette m�thode sera
     * ind�termin�.
     *
     * @param  date Date � laquelle on souhaite positionner le flot.
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws EOFException si des enregistrements manquent dans le fichier, ou si
     *         la date demand�e est post�rieure aux dates de tous les enregistrements
     *         trouv�s dans le fichier.
     */
    public final void seek(final Date date) throws IOException {
        seek(date, null);
    }

    /**
     * Impl�mentation de la recherche d'un enregistrement. Cette m�thode respecte la m�me sp�cification
     * que la m�thode {@link #seek(Date)}, mais prend comme argument suppl�mentaire un it�rateur qui
     * peut retourner les noms des fichiers des objets <code>Parser</code> aux dates pr�c�dentes.
     *
     * @param  date Date � laquelle on souhaite positionner le flot.
     * @param  previous It�rateur de {@link File} qui retournera les noms des
     *         fichiers qui contiennent des donn�es aux dates pr�c�dentes. Le
     *         premier �l�ment doit �tre le fichier imm�diatement avant celui
     *         de <code>this</code>, le second �l�ment doit �tre l'autre fichier
     *         avant, etc. (en d'autres mots, les fichiers doivent �tre retourn�s
     *         en ordre d�croissant de date).
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws EOFException si des enregistrements manquent dans le fichier, ou si
     *         la date demand�e est post�rieure aux dates de tous les enregistrements
     *         trouv�s dans le fichier.
     */
    final void seek(final Date date, final Iterator<File> previous) throws IOException {
        /*
         * Proc�de � la lecture du premier en-t�te du fichier. Cet
         * enregistrement doit exister, � moins que le fichier ne
         * soit vide...
         */
        in.seek(origine);
        recordLeft = 0;
        nextRecordMandatory();
        long timeInterval = TIME_INTERVAL;    // Intervalle de temps estim� entre deux enregistrements.
        final long   time = date.getTime();   // Date de l'enregistrement d�sir�.
        long      endTime = time;             // Date du dernier enregistrement trouv� ou permis.
        nextPass: do {
            /*
             * L'enregistrement courant  (lu par le dernier 'nextRecordOrHeader')  doit �tre un en-t�te.
             * Si la date demand�e est post�rieure � la date de l'en-t�te, alors il faudra chercher dans
             * le fichier quel enregistrement correspond � la date demand�e.   Si au contraire l'en-t�te
             * est d�j� post�rieure � la date demand�e, alors il n'y a pas de recherche � faire et on
             * sortira de cette m�thode en laissant le flot positionn� sur le premier enregistrement.
             */
            final int  cpRecordCount = getField(RECORD_COUNT);  // Nombre d'enregistrements dans la passe courante.
            long         currentTime = getDate().getTime();     // Date et heure de l'enregistrement.
            final long   startOfPass = in.getStreamPosition();  // Position du flot sur le premier enregistrement de la passe courante.
            long       startOfRecord = startOfPass;             // Position du prochain enregistrement � lire.
            int              nRecord = 0;                       // Num�ro de l'enregistrement courant (valide apr�s le 'nextRecordOrHeader' plus bas).
            if (time >= currentTime) {
                /*
                 * Tente maintenant de pr�dire le num�ro d'enregistrement qui devrait
                 * correspondre � la date sp�cifi�e. Ce num�ro est calcul� en suposant
                 * que tous les enregistrements sont espac�s d'un intervalle de temps
                 * �gal (typiquement une seconde). Ca ne sera pas toujours le cas; on
                 * v�rifiera donc plus loin en lisant la date de l'enregistrement. La
                 * position du flot sera ajust�e de plus en plus finement en fonction
                 * des dates "pr�vues" et des dates trouv�es dans les enregistrements.
                 */
                int nRecordLastTry = 0;                            // Num�ro de l'enregistrement lors de la derni�re lecture.
                int lower          = 0;                            // Num�ro d'enregistrement minimal autoris�.
                int upper          = cpRecordCount-1;              // Num�ro d'enregistrement maximal autoris�.
                nRecord = (int) ((time-currentTime)/timeInterval); // Num�ro de l'enregistrement courant (valide apr�s le 'nextRecordOrHeader' plus bas).
                do {
                    if (nRecord < lower) nRecord=lower;
                    if (nRecord > upper) nRecord=upper;
                    startOfRecord = startOfPass + nRecord*RECORD_LENGTH;
                    in.seek(startOfRecord);
                    nextRecordMandatory();
                    /*
                     * Nous venons de lire un enregistrement que nous croyons �tre � la date demand�e. Il
                     * faut maintenant v�rifier cette date, et �ventuellement appliquer une correction �
                     * la position du flot en fonction de l'�cart entre la date trouv�e et la date demand�e.
                     * On appliquera les crit�res suivants:
                     *
                     *  - Si l'enregistrement trouv� est ant�rieur � l'enregistrement d�sir�,
                     *    il faut avancer plus loin. Le num�ros d'enregistrement courant fixe
                     *    la limite inf�rieure des num�ros permis.
                     *  - Si l'enregistrement trouv� est post�rieur � l'enregistrement d�sir�,
                     *    il faut reculer un peu. Le num�ros d'enregistrement courant fixe la
                     *    limite sup�rieure des num�ros permis.
                     */
                    final long timeLastTry = currentTime;
                    currentTime = getDate().getTime();
                    if (nRecordLastTry != nRecord) {
                        timeInterval = Math.max(1, (int) ((currentTime-timeLastTry)/(nRecord-nRecordLastTry)));
                    }
                    nRecordLastTry=nRecord;
                    if (currentTime < time) {
                        /*
                         * Il faut avancer. D'abord, on fixe la limite inf�rieure. Ensuite, s'il ne sera pas possible d'avancer
                         * plus loin (parce qu'on a d�j� atteint la limite sup�rieure), alors on balayera la passe suivante � la
                         * condition qu'on �tait � la fin de la passe courante. Sinon (si on se trouve quelque part au milieu de
                         * la passe courante), on a trouv� l'enregistrement que l'on cherchait et on termine cette m�thode. Note:
                         * on ne fait pas de 'seek' puisque le prochain enregistrement (celui � 'upper') est effectivement celui
                         * devant lequel on voulait se positionner.
                         *
                         * +----------------+----------------+----------------+----------------+----------------+
                         * |                |                |      upper     | date que l'on  |                |
                         * |                |                |                |sait post�rieure|                |
                         * +----------------+----------------+----------------+----------------+----------------+
                         *                                                    |
                         *                                             position d�sir�e
                         */
                        lower=nRecord+1;
                        if (nRecord == upper) {
                            if (lower == cpRecordCount) {
                                endTime = currentTime;
                                continue nextPass;
                            }
                            recordLeft = cpRecordCount-lower;
                            log("seek", getResource(ResourceKeys.SEEK_TO_DATE_$2, new Date(endTime)));
                            return;
                        }
                    } else if (currentTime > time) {
                        /*
                         * Il faut reculer. Si on ne peut par reculer parce qu'on �tait d�j� au d�but de la plage
                         * permise, alors on se repositionne au d�but de l'enregistrement et on termine la m�thode.
                         * Sinon on fixe la limite sup�rieure et on continue...
                         *
                         * +----------------+----------------+----------------+----------------+----------------+
                         * |                | date que l'on  |      lower     |                |                |
                         * |                |sait ant�rieure |                |                |                |
                         * +----------------+----------------+----------------+----------------+----------------+
                         *                                   |
                         *                            position d�sir�e
                         */
                        if (nRecord == lower) {
                            break;
                        }
                        endTime = currentTime;
                        upper = nRecord-1;
                    } else {
                        break; // On est tomb� pile sur la date cherch�e!
                    }
                    nRecord += (int) ((time-currentTime)/timeInterval);
                    assert lower <= upper : lower-upper;
                }
                while (true);
            }
            /*
             * On a maintenant d�termin� que l'enregistrement commen�ant � <code>startOfRecord</code>
             * est le premier enregistrement dont la date est �gale ou sup�rieure � la date demand�e.
             * Il est donc le prochain enregistrement que devra retourner {@link #nextRecord}.   Pour
             * respecter la sp�cification de cette m�thode, nous allons maintenant lire le contenu de
             * l'enregistrement imm�diatement pr�c�dent (en ignorant un �ventuel en-t�te) et laisser
             * ce contenu dans le buffer.
             */
            final Date selectedDate = getDate();
            long previousRecord = startOfRecord-RECORD_LENGTH;
            if (previousRecord < startOfPass) {
                // Si on �tait au d�but de la passe,
                // il faut aussi sauter l'en-t�te.
                previousRecord -= RECORD_LENGTH;
            }
            if (previousRecord >= origine) {
                in.seek(previousRecord);
                nextRecordMandatory();
            } else {
                /*
                 * Nous �tions d�j� au d�but du fichier. Il est donc impossible d'obtenir un enregistrement
                 * pr�c�dent dans ce fichier. S'il est possible de chercher un enregistrement dans le fichier
                 * pr�c�dent, on m�morisera cet enregistrement. Sinon, par convention on mettra tous les champs
                 * � 0.
                 */
                Arrays.fill(record, (byte) 0);
                if (previous != null) {
                    while (previous.hasNext()) {
                        boolean hasFoundRecord = false;
                        final FileParser p=new FileParser((File) previous.next());
                        while (p.nextRecordOrHeader()) {
                            p.recordLeft = p.getField(RECORD_COUNT);
                            if (p.recordLeft != 0) {
                                p.skipRecords(p.recordLeft-1);
                                p.nextRecordMandatory();
                                assert p.recordLeft==0 : p.recordLeft;
                                hasFoundRecord = true;
                            }
                        }
                        if (hasFoundRecord) {
                            System.arraycopy(p.record, 0, record, 0, record.length);
                            break;
                        }
                        p.close();
                    }
                }
            }
            in.seek(startOfRecord);
            recordLeft = cpRecordCount-nRecord;
            assert selectedDate.getTime() >= time;
            assert isBlank() || getDate().getTime() < time;
            log("seek", getResource(ResourceKeys.SEEK_TO_DATE_$2, selectedDate));
            return;
        }
        while (nextRecordOrHeader());
        Arrays.fill(record, (byte)0);
        in.seek(origine);
        throw new EOFException(Resources.format(ResourceKeys.ERROR_DATE_TOO_LATE_$3,
                               getFilename(), date, new Date(endTime)));
    }

    /**
     * Proc�de � la lecture de l'enregistrement suivant. Cet enregistrement peut �tre un en-t�te.
     * Il sera de la responsabilit� de l'appelant de tenir la variable {@link #recordLeft} � jour.
     *
     * @return <code>true</code> si l'enregistrement a �t� lu, ou <code>false</code>
     *         s'il ne restait plus d'enregistrements � lire.
     * @throws EOFException si un d�but d'enregistrement fut trouv� mais n'est pas complet.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    private boolean nextRecordOrHeader() throws IOException {
        switch (in.read(record)) {
            case -1:            return false;
            case RECORD_LENGTH: return true;
            default: throw new EOFException(getResource(ResourceKeys.ERROR_MISSING_FIELDS_$1));
        }
    }

    /**
     * Proc�de � la lecture de l'enregistrement suivant. Contrairement � la m�thode
     * {@link #nextRecordOrHeader} classique,  celle-ci lance une exception s'il ne
     * reste plus d'enregistrements � lire.
     */
    private final void nextRecordMandatory() throws IOException {
        if (!nextRecordOrHeader()) throw new EOFException(getResource(ResourceKeys.ERROR_MISSING_RECORDS_$1));
        recordLeft--; // On n'utilise pas de 'assert' car 'seek' peut utiliser des valeurs momentan�ment invalides.
    }

    /**
     * Proc�de � la lecture de l'enregistrement suivant. Les en-t�tes seront automatiquement saut�s.
     * Cette m�thode retourne <code>true</code> si un enregistrement complet a �t� obtenu.  On peut
     * alors utiliser la m�thode {@link #getField} pour obtenir les valeurs des diff�rents champs.
     * S'il n'y avait plus d'enregistrement � lire, alors cette m�thode retourne <code>false</code>.
     * S'il y avait un d�but d'enregistrement mais qu'il n'est pas complet, cette m�thode lance une
     * exception {@link EOFException}.
     *
     * @return <code>true</code> si l'enregistrement a �t� lu, ou <code>false</code>
     *         s'il ne restait plus d'enregistrements � lire.
     * @throws EOFException si un d�but d'enregistrement fut trouv� mais n'est pas complet.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public boolean nextRecord() throws IOException {
        assert(recordLeft>=0);
        while (recordLeft==0) {
            if (!nextRecordOrHeader()) {
                Arrays.fill(record, (byte)0);
                return false;
            }
            recordLeft = getField(RECORD_COUNT);
            if (recordLeft < 0) {
                throw new IOException(Resources.format(ResourceKeys.ERROR_BAD_RECORD_COUNT_$1,
                                      new Integer(recordLeft)));
            }
        }
        nextRecordMandatory();
        return true;
    }

    /**
     * Indique si l'enregistrement courant est blanc. Un enregistrement est consid�r� blanc
     * si tous ses champs (tels que retourn�s par {@link #getField}) ont la valeur 0. Apr�s
     * la construction d'un objet {@link Parser}, l'enregistrement courant est initialement
     * blanc.
     */
    public boolean isBlank() {
        for (int i=0; i<record.length; i++) {
            if (record[i]!=0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retourne la valeur cod�e du champ sp�cifi�. La valeur du champ sera puis�e dans l'enregistrement
     * le lors du dernier appel � la m�thode {@link #nextRecord}. Le num�ro du champ <code>field</code>
     * doit �tre une des constantes du tableau ci-dessous.
     *
     * <table align=center bgcolor=FloralWhite border=1 cellspacing=0 cellpadding=6>
     *   <tr><td align=center bgcolor=Moccasin><strong> HEADER </strong></td>
     *       <td align=center bgcolor=Moccasin><strong> DATA   </strong></td></tr>
     *
     *   <tr><td> {@link #PASSNUMBER}   </td><td> {@link #LATITUDE}    </td></tr>
     *   <tr><td> {@link #RECORD_COUNT} </td><td> {@link #LONGITUDE}   </td></tr>
     *   <tr><td> {@link #JULIANDAY}    </td><td> {@link #JULIANDAY}   </td></tr>
     *   <tr><td> {@link #SECOND}       </td><td> {@link #SECOND}      </td></tr>
     *   <tr><td> {@link #MICROSECOND}  </td><td> {@link #MICROSECOND} </td></tr>
     *   <tr><td>                       </td><td> {@link #HEIGHT}      </td></tr>
     *   <tr><td>                       </td><td> {@link #MEAN}        </td></tr>
     *   <tr><td>                       </td><td> {@link #BAROMETRIC}  </td></tr>
     * </table>
     */
    public int getField(int field) {
        return ((int) (record[  field] & 0xFF) <<  0) |
               ((int) (record[++field] & 0xFF) <<  8) |
               ((int) (record[++field] & 0xFF) << 16) |
               ((int) (record[++field]       ) << 24);
    }

    /**
     * Retourne la valeur r�elle du champ sp�cifi�. La valeur du champ sera puis�e dans l'enregistrement
     * lu lors du dernier appel � la m�thode {@link #nextRecord}.  Le num�ro du champ <code>field</code>
     * doit �tre une des constantes du tableau ci-dessous.
     *
     * <table align=center bgcolor=FloralWhite border=1 cellspacing=0 cellpadding=6>
     *   <tr><td align=center bgcolor=Moccasin><strong> FIELD </strong></td>
     *       <td align=center bgcolor=Moccasin><strong> Units </strong></td></tr>
     *
     *   <tr><td> {@link #LATITUDE}    </td><td> degr�s </td></tr>
     *   <tr><td> {@link #LONGITUDE}   </td><td> degr�s </td></tr>
     *   <tr><td> {@link #JULIANDAY}   </td><td>        </td></tr>
     *   <tr><td> {@link #SECOND}      </td><td>        </td></tr>
     *   <tr><td> {@link #MICROSECOND} </td><td>        </td></tr>
     *   <tr><td> {@link #HEIGHT}      </td><td> m�tres </td></tr>
     *   <tr><td> {@link #MEAN}        </td><td> m�tres </td></tr>
     *   <tr><td> {@link #BAROMETRIC}  </td><td> m�tres </td></tr>
     * </table>
     */
    public double getValue(final int field) {
        final int z = getField(field);
        switch (field) {
            // Note: les valeurs minimales et maximales suivantes sont fournies dans la documentation d'AVISO.
            case LATITUDE:     return (z>= -90000000 && z<=  +90000000) ? z/DEGREES_TO_INT : Double.NaN; // microdegres
            case LONGITUDE:    return (z>=         0 && z<= +360000000) ? z/DEGREES_TO_INT : Double.NaN; // microdegres
            case HEIGHT:       return (z>=   -300000 && z<=    +300000) ? z/ METRES_TO_INT : Double.NaN; // millimeter
            case MEAN:         return (z>=   -300000 && z<=    +300000) ? z/ METRES_TO_INT : Double.NaN; // millimeter
            case BAROMETRIC:   return (z>=      -500 && z<=       +500) ? z/ METRES_TO_INT : Double.NaN; // millimeter
            default:           return z;
        }
    }

    /**
     * Retourne la date cod�e dans l'enregistrement courant. L'enregistrement courant
     * est celui qui a �t� lu lors du dernier appel de la m�thode {@link #nextRecord}.
     * La date sera exprim�e selon la convention du Java, c'est-�-dire en nombre de
     * millisecondes �coul�es depuis le 1er janvier 1970. Cette m�thode proc�de en
     * lisant les champs {@link #JULIANDAY}, {@link #SECOND} et {@link #MICROSECOND},
     * en supposant que ces champs utilisent le fuseau horaire UTC.
     */
    public Date getDate() {
        if (isBlank()) {
            return null;
        }
        calendar.clear();
        calendar.set(EPOCH_YEAR, 0, getField(JULIANDAY), 0, 0, getField(SECOND));
        final int ms=getField(MICROSECOND);
        final Date date=calendar.getTime();
        long time = date.getTime()+ms/1000;
        if (ms%1000 >= 500) time++;
        date.setTime(time);
        return date;
    }

    /**
     * Retourne la date du premier enregistrement.
     *
     * L'appel de cette m�thode peut n�cessiter un balayage des donn�es jusqu'� la fin.
     * Les informations trouv�es seront conserv�es dans une cache interne afin d'�viter
     * un nouveau balayage lors des appels subs�quents.
     *
     * @return La date du premier enregistrement.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public Date getStartTime() throws IOException {
        if (startTime == Long.MAX_VALUE) {
            count();
            if (startTime==Long.MAX_VALUE) {
                throw new EOFException(getResource(ResourceKeys.ERROR_MISSING_RECORDS_$1));
            }
        }
        return new Date(startTime);
    }

    /**
     * Retourne la date du dernier enregistrement.
     *
     * L'appel de cette m�thode peut n�cessiter un balayage des donn�es jusqu'� la fin.
     * Les informations trouv�es seront conserv�es dans une cache interne afin d'�viter
     * un nouveau balayage lors des appels subs�quents.
     *
     * @return La date du dernier enregistrement.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public Date getEndTime() throws IOException {
        if (endTime == Long.MIN_VALUE) {
            count();
            if (endTime == Long.MIN_VALUE) {
                throw new EOFException(getResource(ResourceKeys.ERROR_MISSING_RECORDS_$1));
            }
        }
        return new Date(endTime);
    }

    /**
     * Retourne le nombre de passes que contient le fichier.
     *
     * L'appel de cette m�thode peut n�cessiter un balayage des donn�es jusqu'� la fin.
     * Les informations trouv�es seront conserv�es dans une cache interne afin d'�viter
     * un nouveau balayage lors des appels subs�quents.
     *
     * @return Le nombre de passe dans le fichier courant.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public int getPassCount() throws IOException {
        if (passCount == 0) {
            count();
        }
        return passCount;
    }

    /**
     * Retourne le nombre d'enregistrements que contient le fichier,
     * en excluant les enregistrements d'en-t�te.
     *
     * L'appel de cette m�thode peut n�cessiter un balayage des donn�es jusqu'� la fin.
     * Les informations trouv�es seront conserv�es dans une cache interne afin d'�viter
     * un nouveau balayage lors des appels subs�quents.
     *
     * @return Le nombre d'enregistrement dans le fichier courant.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public long getRecordCount() throws IOException {
        if (recordCount == 0) {
            count();
        }
        return recordCount;
    }

    /**
     * Met � jour les champs {@link #passCount} et {@link #recordCount} en balayant
     * tous le fichier.  Le fichier sera remis � sa position initiale apr�s l'appel
     * de cette m�thode.
     */
    private void count() throws IOException {
        recordCount         = 0;
        passCount           = 0;
        startTime           = Long.MAX_VALUE;
        endTime             = Long.MIN_VALUE;
        boolean   firstpass = true;
        final long      pos = in.getStreamPosition();
        final byte[] backup = new byte[record.length];
        final int recordBak = recordLeft;
        in.seek(origine);
        while (nextRecordOrHeader()) {
            final int n = recordLeft = getField(RECORD_COUNT);
            if (n != 0) {
                if (firstpass) {
                    nextRecordMandatory();
                    startTime=getDate().getTime();
                    firstpass=false;
                }
                if (recordLeft >= 1) { // Peut �tre 0 si le premier et le dernier enregistrement se confondent.
                    skipRecords(recordLeft-1);
                    nextRecordMandatory();
                }
                endTime=getDate().getTime();
            }
            assert recordLeft==0 : recordLeft;
            recordCount += n;
            passCount++;
        }
        System.arraycopy(backup, 0, record, 0, backup.length);
        recordLeft = recordBak;
        in.seek(pos);
    }

    /**
     * Saute un certain nombre d'enregistrements. Apr�s l'appel de cette m�thode,
     * {@link #nextRecord} devrait �tre appel�e pour lire l'enregistrement qui suit
     * ceux que l'on vient de sauter.
     *
     * @param  n Nombre d'enregistrements � sauter (incluant les �ventuels en-t�tes).
     * @throws EOFException s'il ne restait pas au moins <code>n</code> enregistrements.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    private void skipRecords(final long n) throws IOException {
        if (n>=0 && n<Long.MAX_VALUE/RECORD_LENGTH) {
            Arrays.fill(record, (byte)0);
            final long  toSkip = n * (long)RECORD_LENGTH;
            final long skipped = in.skipBytes(toSkip);
            recordLeft -= (int)(skipped/RECORD_LENGTH);
            assert recordLeft >= 0 : recordLeft;
            if ((skipped % RECORD_LENGTH)!=0) {
                throw new EOFException(getResource(ResourceKeys.ERROR_MISSING_FIELDS_$1));
            }
            if (skipped != toSkip) {
                throw new EOFException(getResource(ResourceKeys.ERROR_MISSING_RECORDS_$1));
            }
        } else {
            throw new IllegalArgumentException(Long.toString(n));
        }
    }

    /**
     * Ferme le flot <code>in</code> qui fournissait les donn�es. Apr�s l'appel
     * de cette m�thode, {@link #nextRecord} ne pourra plus �tre utilis�es.
     *
     * @throws IOException si une erreur est survenue lors de la fermeture.
     */
    public void close() throws IOException {
        Arrays.fill(record, (byte)0);
        in.close();
        log("close", getResource(ResourceKeys.CLOSE_$1));
    }

    /**
     * Retourne une cha�ne de caract�res repr�sentant cet enregistrement.  Cette cha�ne de caract�res contient
     * des colonnes indiquant dans l'ordre la date de l'enregistrement, les coordonn�es (latitude et longitude),
     * la hauteur mesur�e, la hauteur moyenne et la correction barom�trique. La date est toujours �crite selon
     * le fuseau horaire UTC, et les hauteurs sont toujours exprim�es en m�tres. Un exemple de sortie est:
     *
     *  <pre>12/10/92 00:53:54   16�30,0'N  259�39,3'E   -14,447   -14,480     0,028</pre>
     */
    public String toString() {
        if (!isBlank()) {
            if (dateFormat == null) {
                dateFormat=getDateTimeInstance();
            }
            final FieldPosition pos=new FieldPosition(0);
            final StringBuffer buffer=new StringBuffer();
            dateFormat.format(getDate(), buffer, pos);
            if (angleFormat == null) {
                angleFormat = NumberFormat.getNumberInstance();
                angleFormat.setMinimumFractionDigits(4);
                angleFormat.setMaximumFractionDigits(4);
            }
            if (numberFormat == null) {
                numberFormat = NumberFormat.getNumberInstance();
                numberFormat.setMinimumFractionDigits(3);
                numberFormat.setMaximumFractionDigits(3);
            }
            int last;
            last=buffer.length(); angleFormat .format(getField(LATITUDE  )/DEGREES_TO_INT, buffer, pos); buffer.insert(last, Utilities.spaces(10-(buffer.length()-last)));
            last=buffer.length(); angleFormat .format(getField(LONGITUDE )/DEGREES_TO_INT, buffer, pos); buffer.insert(last, Utilities.spaces(10-(buffer.length()-last)));
            last=buffer.length(); numberFormat.format(getField(HEIGHT    )/ METRES_TO_INT, buffer, pos); buffer.insert(last, Utilities.spaces(10-(buffer.length()-last)));
            last=buffer.length(); numberFormat.format(getField(MEAN      )/ METRES_TO_INT, buffer, pos); buffer.insert(last, Utilities.spaces(10-(buffer.length()-last)));
            last=buffer.length(); numberFormat.format(getField(BAROMETRIC)/ METRES_TO_INT, buffer, pos); buffer.insert(last, Utilities.spaces(10-(buffer.length()-last)));
            return buffer.toString();
        } else {
            return "(pas de donn�es)"; // TODO: localize
        }
    }

    /**
     * Affiche les premiers enregistrements d'un fichier CORSSH d'AVISO. Cette
     * lecture peut �tre lanc�e � partir de la ligne de commande comme suit:
     *
     * <pre>java fr.ird.codec.corssh.Parser <var>filename</var> <var>[start time]</var> <var>[max]</var></pre>
     *
     * <var>filename</var>   est un argument obligatoire sp�cifiant le nom du fichier � lire.
     * <var>start time</var> est un argument optionel sp�cifiant la date du premier enregistrement � afficher.
     * <var>max</var>        est un argument optionel indiquant le nombre maximal de lignes � afficher. La valeur par d�faut est de 10.
     */
    public static void main(final String[] args) throws Exception {
        int n = 10;
        Date start=null;
        switch (args.length) {
            default: System.out.println("Usage: FileParser [filename] [date] [max]"); break;
            case 3:  n=Integer.parseInt(args[2]);                // fallthrough
            case 2:  start=getDateTimeInstance().parse(args[1]); // fallthrough
            case 1:  main(new FileParser(new File(args[0])), start, n); break;
        }
    }
}
