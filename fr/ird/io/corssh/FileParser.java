/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
 *          Maison de la télédétection
 *          Institut de Recherche pour le développement
 *          500 rue Jean-François Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.io.corssh;

// Entrés/sorties
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
 * Implémentation de la lecture d'un fichier CORSSH binaire.
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
     * L'année de l'époch des dates
     * codées selon le format AVISO.
     *
     * @see #getDate
     */
    private static final int EPOCH_YEAR = 1950;

    /**
     * Intervalle de temps typique entre deux enregistrements. Cette information
     * est approximative. Elle sera à accélérer le positionnement du flot sur un
     * enregistrement correspondant à une date précise.
     */
    private static final int TIME_INTERVAL = 1000;

    /**
     * Dernier enregistrement de
     * données à avoir été lu.
     *
     * @see #nextRecord
     * @see #getField
     * @see #getDate
     */
    private final byte[] record=new byte[RECORD_LENGTH];

    /**
     * Flot qui fournira les données binaires des enregistrements.
     */
    private final ImageInputStream in;

    /**
     * Nom désignant le fichier ouvert. Il n'est pas obligatoire que ce nom soit complet.
     * Il ne sera utilisé que pour la production d'un éventuel message d'erreur. Il peut
     * être nul s'il n'est pas connu.
     */
    private final String filename;

    /**
     * Position du début de flot. Le plus
     * souvent, ça sera la position 0.
     */
    private final long origine;

    /**
     * Nombre d'enregistrements restant
     * à lire avant le prochain en-tête.
     */
    private int recordLeft;

    /**
     * Nombre d'enregistrements (sans les en-têtes) que contient le fichier {@link #in}.
     * Cette information est obtenue lorsque nécessaire par la méthode {@link #count}.
     */
    private long recordCount;

    /**
     * Nombre de passes que contient le fichier {@link #in}. Cette information
     * est obtenue lorsque nécessaire par la méthode {@link #count}.
     */
    private int passCount;

    /**
     * Date et heure du premier enregistrement, en nombre de
     * millisecondes écoulées depuis le 1er janvier 1970 UTC.
     * La valeur {@link Long#MAX_VALUE} indique que cette
     * propriété n'a pas encore été définie.
     */
    private long startTime=Long.MAX_VALUE;

    /**
     * Date et heure du dernier enregistrement, en nombre de
     * millisecondes écoulées depuis le 1er janvier 1970 UTC.
     * La valeur {@link Long#MIN_VALUE} indique que cette
     * propriété n'a pas encore été définie.
     */
    private long endTime=Long.MIN_VALUE;

    /**
     * Objet utilisé pour écrire des valeurs. Cet objet est utilisé par la méthode {@link #toString}
     * pour produire une chaîne de caractères représentant un enregistrement avec ses valeurs.
     */
    private transient NumberFormat numberFormat;

    /**
     * Objet utilisé pour écrire des coordonnées. Cet objet est utilisé par la méthode {@link #toString}
     * pour produire une chaîne de caractères représentant un enregistrement avec ses valeurs.
     */
    private transient NumberFormat angleFormat;

    /**
     * Objet utilisé pour écrire des dates. Cet objet est utilisé par la méthode {@link #toString}
     * pour produire une chaîne de caractères représentant un enregistrement avec sa date.
     */
    private transient DateFormat dateFormat;

    /**
     * Calendrier utilisé pour convertir des dates du système AVISO vers la convention du Java.
     * Le Java exprime les dates en nombre de millisecondes écoulées depuis le 1er janvier 1970.
     *
     * @see #getDate
     */
    private final Calendar calendar=new GregorianCalendar(TimeZone.getTimeZone("UTC"));

    /**
     * Construit un interpréteur qui lira le fichier spécifié. Après la construction
     * de cet objet, les enregistrements du fichiers peuvent être lus par des appels
     * à {@link #nextRecord}.
     *
     * @param in Fichier CORSSH à lire.
     * @throws IOException si une erreur d'entré/sortie est survenue.
     */
    public FileParser(final File in) throws IOException {
        this(new FileImageInputStream(in), in.getPath());
    }

    /**
     * Construit un interpréteur qui lira le fichier spécifié. Après la construction
     * de cet objet, les enregistrements du fichiers peuvent être lus par des appels
     * à {@link #nextRecord}.
     *
     * @param  in Flot qui fournira les données binaires des enregistrements.
     * @param  filename Nom désignant le fichier ouvert. Il n'est pas obligatoire
     *         que ce nom soit complet. Il ne sera utilisé que pour la production
     *         d'un éventuel message d'erreur. Il peut être nul s'il n'est pas connu.
     * @throws IOException si une erreur d'entré/sortie est survenue.
     */
    private FileParser(final ImageInputStream in, final String filename) throws IOException {
        this.in=in;
        this.filename=filename;
        origine = in.getStreamPosition();
        log("<init>", getResource(ResourceKeys.OPEN_$1));
    }

    /**
     * Retourne le nom du fichier, ou
     * "(sans nom)" s'il n'a pas été nommé.
     */
    private final String getFilename() {
        return (filename!=null) ? filename : Resources.format(ResourceKeys.UNNAMED);
    }

    /**
     * Retourne la ressource identifiée par la clé spécifiée.
     * Le nom de fichier {@link #filename} sera utilisé pour
     * la construction de la ressource.
     */
    private final String getResource(final int clé) {
        return Resources.format(clé, getFilename());
    }

    /**
     * Retourne la ressource identifiée par la clé spécifiée.
     * Le nom de fichier {@link #filename} sera utilisé pour
     * la construction de la ressource.
     */
    private final String getResource(final int clé, Object arg) {
        if (arg instanceof Date) {
            arg = getDateTimeInstance().format(arg);
        }
        return Resources.format(clé, getFilename(), arg);
    }

    /**
     * Positionne le flot au début du premier enregistrement dont la date est égale ou supérieure à la
     * date spécifiée. Après l'appel de cette méthode, cet objet {@link Parser} se trouvera dans l'état
     * suivant:
     *
     * <ul>
     *   <li>L'enregistrement courant sera celui qui précédait immédiatement l'enregistrement recherché.
     *       On peut donc utiliser la méthode {@link #getField} pour connaître les propriétés du dernier
     *       enregistrement avant la date <code>date</code> spécifiée.   Si ce dernier enregistrement se
     *       trouvait sur la passe précédente, l'en-tête aura été automatiquement sauté.   Si ce dernier
     *       enregistrement est inconnu (par exemple parce qu'il ne se trouve pas sur ce fichier), alors
     *       tous les champs de l'enregistrement courant auront la valeur 0. Ce dernier état peut être
     *       testé en appelant la méthode {@link #isBlank}.</li>
     *   <li>Le prochain appel de la méthode {@link #nextRecord} retournera le premier enregistrement
     *       à ou après la date <code>date</code> spécifiée.  Les appels subséquents retourneront les
     *       enregistrements suivants, comme d'habitude.</li>
     * </ul>
     *
     * Notez que cette méthode suppose que tous les enregistrements
     * apparaissent dans le fichier en ordre croissant de dates. Si
     * ce n'est pas le cas, alors le résultat de cette méthode sera
     * indéterminé.
     *
     * @param  date Date à laquelle on souhaite positionner le flot.
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws EOFException si des enregistrements manquent dans le fichier, ou si
     *         la date demandée est postérieure aux dates de tous les enregistrements
     *         trouvés dans le fichier.
     */
    public final void seek(final Date date) throws IOException {
        seek(date, null);
    }

    /**
     * Implémentation de la recherche d'un enregistrement. Cette méthode respecte la même spécification
     * que la méthode {@link #seek(Date)}, mais prend comme argument supplémentaire un itérateur qui
     * peut retourner les noms des fichiers des objets <code>Parser</code> aux dates précédentes.
     *
     * @param  date Date à laquelle on souhaite positionner le flot.
     * @param  previous Itérateur de {@link File} qui retournera les noms des
     *         fichiers qui contiennent des données aux dates précédentes. Le
     *         premier élément doit être le fichier immédiatement avant celui
     *         de <code>this</code>, le second élément doit être l'autre fichier
     *         avant, etc. (en d'autres mots, les fichiers doivent être retournés
     *         en ordre décroissant de date).
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws EOFException si des enregistrements manquent dans le fichier, ou si
     *         la date demandée est postérieure aux dates de tous les enregistrements
     *         trouvés dans le fichier.
     */
    final void seek(final Date date, final Iterator<File> previous) throws IOException {
        /*
         * Procède à la lecture du premier en-tête du fichier. Cet
         * enregistrement doit exister, à moins que le fichier ne
         * soit vide...
         */
        in.seek(origine);
        recordLeft = 0;
        nextRecordMandatory();
        long timeInterval = TIME_INTERVAL;    // Intervalle de temps estimé entre deux enregistrements.
        final long   time = date.getTime();   // Date de l'enregistrement désiré.
        long      endTime = time;             // Date du dernier enregistrement trouvé ou permis.
        nextPass: do {
            /*
             * L'enregistrement courant  (lu par le dernier 'nextRecordOrHeader')  doit être un en-tête.
             * Si la date demandée est postérieure à la date de l'en-tête, alors il faudra chercher dans
             * le fichier quel enregistrement correspond à la date demandée.   Si au contraire l'en-tête
             * est déjà postérieure à la date demandée, alors il n'y a pas de recherche à faire et on
             * sortira de cette méthode en laissant le flot positionné sur le premier enregistrement.
             */
            final int  cpRecordCount = getField(RECORD_COUNT);  // Nombre d'enregistrements dans la passe courante.
            long         currentTime = getDate().getTime();     // Date et heure de l'enregistrement.
            final long   startOfPass = in.getStreamPosition();  // Position du flot sur le premier enregistrement de la passe courante.
            long       startOfRecord = startOfPass;             // Position du prochain enregistrement à lire.
            int              nRecord = 0;                       // Numéro de l'enregistrement courant (valide après le 'nextRecordOrHeader' plus bas).
            if (time >= currentTime) {
                /*
                 * Tente maintenant de prédire le numéro d'enregistrement qui devrait
                 * correspondre à la date spécifiée. Ce numéro est calculé en suposant
                 * que tous les enregistrements sont espacés d'un intervalle de temps
                 * égal (typiquement une seconde). Ca ne sera pas toujours le cas; on
                 * vérifiera donc plus loin en lisant la date de l'enregistrement. La
                 * position du flot sera ajustée de plus en plus finement en fonction
                 * des dates "prévues" et des dates trouvées dans les enregistrements.
                 */
                int nRecordLastTry = 0;                            // Numéro de l'enregistrement lors de la dernière lecture.
                int lower          = 0;                            // Numéro d'enregistrement minimal autorisé.
                int upper          = cpRecordCount-1;              // Numéro d'enregistrement maximal autorisé.
                nRecord = (int) ((time-currentTime)/timeInterval); // Numéro de l'enregistrement courant (valide après le 'nextRecordOrHeader' plus bas).
                do {
                    if (nRecord < lower) nRecord=lower;
                    if (nRecord > upper) nRecord=upper;
                    startOfRecord = startOfPass + nRecord*RECORD_LENGTH;
                    in.seek(startOfRecord);
                    nextRecordMandatory();
                    /*
                     * Nous venons de lire un enregistrement que nous croyons être à la date demandée. Il
                     * faut maintenant vérifier cette date, et éventuellement appliquer une correction à
                     * la position du flot en fonction de l'écart entre la date trouvée et la date demandée.
                     * On appliquera les critères suivants:
                     *
                     *  - Si l'enregistrement trouvé est antérieur à l'enregistrement désiré,
                     *    il faut avancer plus loin. Le numéros d'enregistrement courant fixe
                     *    la limite inférieure des numéros permis.
                     *  - Si l'enregistrement trouvé est postérieur à l'enregistrement désiré,
                     *    il faut reculer un peu. Le numéros d'enregistrement courant fixe la
                     *    limite supérieure des numéros permis.
                     */
                    final long timeLastTry = currentTime;
                    currentTime = getDate().getTime();
                    if (nRecordLastTry != nRecord) {
                        timeInterval = Math.max(1, (int) ((currentTime-timeLastTry)/(nRecord-nRecordLastTry)));
                    }
                    nRecordLastTry=nRecord;
                    if (currentTime < time) {
                        /*
                         * Il faut avancer. D'abord, on fixe la limite inférieure. Ensuite, s'il ne sera pas possible d'avancer
                         * plus loin (parce qu'on a déjà atteint la limite supérieure), alors on balayera la passe suivante à la
                         * condition qu'on était à la fin de la passe courante. Sinon (si on se trouve quelque part au milieu de
                         * la passe courante), on a trouvé l'enregistrement que l'on cherchait et on termine cette méthode. Note:
                         * on ne fait pas de 'seek' puisque le prochain enregistrement (celui à 'upper') est effectivement celui
                         * devant lequel on voulait se positionner.
                         *
                         * +----------------+----------------+----------------+----------------+----------------+
                         * |                |                |      upper     | date que l'on  |                |
                         * |                |                |                |sait postérieure|                |
                         * +----------------+----------------+----------------+----------------+----------------+
                         *                                                    |
                         *                                             position désirée
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
                         * Il faut reculer. Si on ne peut par reculer parce qu'on était déjà au début de la plage
                         * permise, alors on se repositionne au début de l'enregistrement et on termine la méthode.
                         * Sinon on fixe la limite supérieure et on continue...
                         *
                         * +----------------+----------------+----------------+----------------+----------------+
                         * |                | date que l'on  |      lower     |                |                |
                         * |                |sait antérieure |                |                |                |
                         * +----------------+----------------+----------------+----------------+----------------+
                         *                                   |
                         *                            position désirée
                         */
                        if (nRecord == lower) {
                            break;
                        }
                        endTime = currentTime;
                        upper = nRecord-1;
                    } else {
                        break; // On est tombé pile sur la date cherchée!
                    }
                    nRecord += (int) ((time-currentTime)/timeInterval);
                    assert lower <= upper : lower-upper;
                }
                while (true);
            }
            /*
             * On a maintenant déterminé que l'enregistrement commençant à <code>startOfRecord</code>
             * est le premier enregistrement dont la date est égale ou supérieure à la date demandée.
             * Il est donc le prochain enregistrement que devra retourner {@link #nextRecord}.   Pour
             * respecter la spécification de cette méthode, nous allons maintenant lire le contenu de
             * l'enregistrement immédiatement précédent (en ignorant un éventuel en-tête) et laisser
             * ce contenu dans le buffer.
             */
            final Date selectedDate = getDate();
            long previousRecord = startOfRecord-RECORD_LENGTH;
            if (previousRecord < startOfPass) {
                // Si on était au début de la passe,
                // il faut aussi sauter l'en-tête.
                previousRecord -= RECORD_LENGTH;
            }
            if (previousRecord >= origine) {
                in.seek(previousRecord);
                nextRecordMandatory();
            } else {
                /*
                 * Nous étions déjà au début du fichier. Il est donc impossible d'obtenir un enregistrement
                 * précédent dans ce fichier. S'il est possible de chercher un enregistrement dans le fichier
                 * précédent, on mémorisera cet enregistrement. Sinon, par convention on mettra tous les champs
                 * à 0.
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
     * Procède à la lecture de l'enregistrement suivant. Cet enregistrement peut être un en-tête.
     * Il sera de la responsabilité de l'appelant de tenir la variable {@link #recordLeft} à jour.
     *
     * @return <code>true</code> si l'enregistrement a été lu, ou <code>false</code>
     *         s'il ne restait plus d'enregistrements à lire.
     * @throws EOFException si un début d'enregistrement fut trouvé mais n'est pas complet.
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
     * Procède à la lecture de l'enregistrement suivant. Contrairement à la méthode
     * {@link #nextRecordOrHeader} classique,  celle-ci lance une exception s'il ne
     * reste plus d'enregistrements à lire.
     */
    private final void nextRecordMandatory() throws IOException {
        if (!nextRecordOrHeader()) throw new EOFException(getResource(ResourceKeys.ERROR_MISSING_RECORDS_$1));
        recordLeft--; // On n'utilise pas de 'assert' car 'seek' peut utiliser des valeurs momentanément invalides.
    }

    /**
     * Procède à la lecture de l'enregistrement suivant. Les en-têtes seront automatiquement sautés.
     * Cette méthode retourne <code>true</code> si un enregistrement complet a été obtenu.  On peut
     * alors utiliser la méthode {@link #getField} pour obtenir les valeurs des différents champs.
     * S'il n'y avait plus d'enregistrement à lire, alors cette méthode retourne <code>false</code>.
     * S'il y avait un début d'enregistrement mais qu'il n'est pas complet, cette méthode lance une
     * exception {@link EOFException}.
     *
     * @return <code>true</code> si l'enregistrement a été lu, ou <code>false</code>
     *         s'il ne restait plus d'enregistrements à lire.
     * @throws EOFException si un début d'enregistrement fut trouvé mais n'est pas complet.
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
     * Indique si l'enregistrement courant est blanc. Un enregistrement est considéré blanc
     * si tous ses champs (tels que retournés par {@link #getField}) ont la valeur 0. Après
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
     * Retourne la valeur codée du champ spécifié. La valeur du champ sera puisée dans l'enregistrement
     * le lors du dernier appel à la méthode {@link #nextRecord}. Le numéro du champ <code>field</code>
     * doit être une des constantes du tableau ci-dessous.
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
     * Retourne la valeur réelle du champ spécifié. La valeur du champ sera puisée dans l'enregistrement
     * lu lors du dernier appel à la méthode {@link #nextRecord}.  Le numéro du champ <code>field</code>
     * doit être une des constantes du tableau ci-dessous.
     *
     * <table align=center bgcolor=FloralWhite border=1 cellspacing=0 cellpadding=6>
     *   <tr><td align=center bgcolor=Moccasin><strong> FIELD </strong></td>
     *       <td align=center bgcolor=Moccasin><strong> Units </strong></td></tr>
     *
     *   <tr><td> {@link #LATITUDE}    </td><td> degrés </td></tr>
     *   <tr><td> {@link #LONGITUDE}   </td><td> degrés </td></tr>
     *   <tr><td> {@link #JULIANDAY}   </td><td>        </td></tr>
     *   <tr><td> {@link #SECOND}      </td><td>        </td></tr>
     *   <tr><td> {@link #MICROSECOND} </td><td>        </td></tr>
     *   <tr><td> {@link #HEIGHT}      </td><td> mètres </td></tr>
     *   <tr><td> {@link #MEAN}        </td><td> mètres </td></tr>
     *   <tr><td> {@link #BAROMETRIC}  </td><td> mètres </td></tr>
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
     * Retourne la date codée dans l'enregistrement courant. L'enregistrement courant
     * est celui qui a été lu lors du dernier appel de la méthode {@link #nextRecord}.
     * La date sera exprimée selon la convention du Java, c'est-à-dire en nombre de
     * millisecondes écoulées depuis le 1er janvier 1970. Cette méthode procède en
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
     * L'appel de cette méthode peut nécessiter un balayage des données jusqu'à la fin.
     * Les informations trouvées seront conservées dans une cache interne afin d'éviter
     * un nouveau balayage lors des appels subséquents.
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
     * L'appel de cette méthode peut nécessiter un balayage des données jusqu'à la fin.
     * Les informations trouvées seront conservées dans une cache interne afin d'éviter
     * un nouveau balayage lors des appels subséquents.
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
     * L'appel de cette méthode peut nécessiter un balayage des données jusqu'à la fin.
     * Les informations trouvées seront conservées dans une cache interne afin d'éviter
     * un nouveau balayage lors des appels subséquents.
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
     * en excluant les enregistrements d'en-tête.
     *
     * L'appel de cette méthode peut nécessiter un balayage des données jusqu'à la fin.
     * Les informations trouvées seront conservées dans une cache interne afin d'éviter
     * un nouveau balayage lors des appels subséquents.
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
     * Met à jour les champs {@link #passCount} et {@link #recordCount} en balayant
     * tous le fichier.  Le fichier sera remis à sa position initiale après l'appel
     * de cette méthode.
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
                if (recordLeft >= 1) { // Peut être 0 si le premier et le dernier enregistrement se confondent.
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
     * Saute un certain nombre d'enregistrements. Après l'appel de cette méthode,
     * {@link #nextRecord} devrait être appelée pour lire l'enregistrement qui suit
     * ceux que l'on vient de sauter.
     *
     * @param  n Nombre d'enregistrements à sauter (incluant les éventuels en-têtes).
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
     * Ferme le flot <code>in</code> qui fournissait les données. Après l'appel
     * de cette méthode, {@link #nextRecord} ne pourra plus être utilisées.
     *
     * @throws IOException si une erreur est survenue lors de la fermeture.
     */
    public void close() throws IOException {
        Arrays.fill(record, (byte)0);
        in.close();
        log("close", getResource(ResourceKeys.CLOSE_$1));
    }

    /**
     * Retourne une chaîne de caractères représentant cet enregistrement.  Cette chaîne de caractères contient
     * des colonnes indiquant dans l'ordre la date de l'enregistrement, les coordonnées (latitude et longitude),
     * la hauteur mesurée, la hauteur moyenne et la correction barométrique. La date est toujours écrite selon
     * le fuseau horaire UTC, et les hauteurs sont toujours exprimées en mètres. Un exemple de sortie est:
     *
     *  <pre>12/10/92 00:53:54   16°30,0'N  259°39,3'E   -14,447   -14,480     0,028</pre>
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
            return "(pas de données)"; // TODO: localize
        }
    }

    /**
     * Affiche les premiers enregistrements d'un fichier CORSSH d'AVISO. Cette
     * lecture peut être lancée à partir de la ligne de commande comme suit:
     *
     * <pre>java fr.ird.codec.corssh.Parser <var>filename</var> <var>[start time]</var> <var>[max]</var></pre>
     *
     * <var>filename</var>   est un argument obligatoire spécifiant le nom du fichier à lire.
     * <var>start time</var> est un argument optionel spécifiant la date du premier enregistrement à afficher.
     * <var>max</var>        est un argument optionel indiquant le nombre maximal de lignes à afficher. La valeur par défaut est de 10.
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
