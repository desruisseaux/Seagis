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

// Gestion des entr�s/sorties
import java.io.Writer;
import java.io.IOException;
import java.io.EOFException;
import java.io.OutputStreamWriter;

// Divers
import java.awt.Shape;
import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Geotools
import org.geotools.units.Unit;
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.DatumType;
import org.geotools.cs.HorizontalDatum;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.resources.Resources;


/**
 * Interpr�teur des fichiers CORSSH d'AVISO. Cette classe fournit des m�thodes facilitant les lectures brutes
 * des donn�es d'un ficher <code>.DAT</code> d'AVISO. Le format de ces fichiers est d�crit � la page
 * <a href="http://sirius-ci.cst.cnes.fr:8090/HTML/information/frames/general/general_fr.html">http://sirius-ci.cst.cnes.fr:8090/HTML/information/frames/general/general_fr.html</a>
 * sous la rubrique "Expertise", Manuel CORSSH".
 *
 * Les fichiers CORSSH sont des fichiers binaires contenant des enregistrements de longueur
 * fixe, soit 32 octets. Chaque enregistrement contient 8 entiers sign�s de 4 octets, cod�s
 * selon la convention des syst�mes VMS. Les fichiers commencent par l'enregistrement d'un
 * en-t�te, suivit d'un nombre variable d'enregistrements de donn�es. Suit ensuite un autre
 * enregistrement d'en-t�te, et ainsi de suite. Les tableaux ci-dessous r�sument l'organisation
 * des fichiers CORSSH ainsi que la signification des champs dans les enregistrements.
 *
 * <CENTER><TABLE>
 * <TR><TD><TABLE border=3 bgcolor="#D9ECFF">
 * <TR><TD bgcolor="#91C8FF"><FONT SIZE="1"><B>1<SUP>st</SUP> PASS</B></FONT></TD> <TD bgcolor="#91C8FF"><FONT SIZE="1">HEADER record</FONT></TD></TR>
 * <TR><TD></TD><TD><FONT SIZE="1">DATA records<BR>
 *&nbsp;&nbsp;&nbsp;Scientific data record 1<BR>
 *&nbsp;&nbsp;&nbsp;.../...<BR>
 *&nbsp;&nbsp;&nbsp;Scientific data record N<sub>1</sub></FONT></TD></TR>
 *
 * <TR><TD bgcolor="#91C8FF"><FONT SIZE="1"><B>2<SUP>nd</SUP> PASS</B></FONT></TD> <TD bgcolor="#91C8FF"><FONT SIZE="1">HEADER record</FONT></TD></TR>
 * <TR><TD></TD><TD><FONT SIZE="1">DATA records<BR>
 *&nbsp;&nbsp;&nbsp;Scientific data record 1<BR>
 *&nbsp;&nbsp;&nbsp;.../...<BR>
 *&nbsp;&nbsp;&nbsp;Scientific data record N<sub>2</sub></FONT></TD></TR>
 * <TR><TD colspan=2 align="center"><FONT SIZE="1">.../...</FONT></TD></TR>
 *
 * <TR><TD bgcolor="#91C8FF"><FONT SIZE="1"><B>LAST PASS</B></FONT></TD><TD bgcolor="#91C8FF"><FONT SIZE="1">HEADER record</FONT></TD></TR>
 * <TR><TD></TD><TD><FONT SIZE="1">DATA records<BR>
 *&nbsp;&nbsp;&nbsp;Scientific data record 1<BR>
 *&nbsp;&nbsp;&nbsp;.../...<BR>
 *&nbsp;&nbsp;&nbsp;Scientific data record N<sub>last</sub></FONT></TD></TR>
 * </TABLE>
 * </TD><TD><P>&nbsp;&nbsp;&nbsp;&nbsp;</P></TD>
 * <TD><TABLE cellspacing="24"><TR>
 * <TD><TABLE BORDER=1 bgcolor="#D9ECFF">
 * <TR><TD COLSPAN=7 bgcolor="#91C8FF"><CENTER><B><I><FONT COLOR="#0000DD">HEADER RECORD</FONT></I></B></CENTER></TD></TR>
 * <TR><TD align="center"><strong>Field</strong></TD>  <TD align="center"><strong>Record<BR>Location</strong></TD>
 *     <TD align="center"><strong>Content</strong></TD><TD align="center"><strong>Units</strong></TD></TR>
 * <TR><TD> {@link #PASSNUMBER}    </TD><TD align="center">  0 </TD><TD> Pass number                  </TD><TD align="center"> /    </TD></TR>
 * <TR><TD> {@link #RECORD_COUNT}  </TD><TD align="center">  4 </TD><TD> Data count                   </TD><TD align="center"> /    </TD></TR>
 * <TR><TD> {@link #JULIANDAY}     </TD><TD align="center">  8 </TD><TD> Time, day part               </TD><TD align="center"> day  </TD></TR>
 * <TR><TD> {@link #SECOND}        </TD><TD align="center"> 12 </TD><TD> Time, second part            </TD><TD align="center"> s    </TD></TR>
 * <TR><TD> {@link #MICROSECOND}   </TD><TD align="center"> 16 </TD><TD> Time, microsecond part       </TD><TD align="center"> �s   </TD></TR>
 * <TR><TD>                        </TD><TD align="center"> 20 </TD><TD> Empty                        </TD><TD align="center"> /    </TD></TR>
 * <TR><TD>                        </TD><TD align="center"> 24 </TD><TD> Empty                        </TD><TD align="center"> /    </TD></TR>
 * <TR><TD>                        </TD><TD align="center"> 28 </TD><TD> Empty                        </TD><TD align="center"> /    </TD></TR>
 * </TABLE></TD>
 *
 * <TD><TABLE BORDER=1 bgcolor="#D9ECFF">
 * <TR><TD COLSPAN=7 bgcolor="#91C8FF"><CENTER><B><I><FONT COLOR="#0000DD">DATA RECORD</FONT></I></B></CENTER></TD></TR>
 * <TR><TD align="center"><strong>Field</strong></TD>  <TD align="center"><strong>Record<BR>Location</strong></TD>
 *     <TD align="center"><strong>Content</strong></TD><TD align="center"><strong>Units</strong></TD></TR>
 * <TR><TD> {@link #LATITUDE}    </TD><TD align="center">  0 </TD><TD> Latitude                     </TD><TD align="center"> �deg </TD></TR>
 * <TR><TD> {@link #LONGITUDE}   </TD><TD align="center">  4 </TD><TD> Longitude                    </TD><TD align="center"> �deg </TD></TR>
 * <TR><TD> {@link #JULIANDAY}   </TD><TD align="center">  8 </TD><TD> Time, day part               </TD><TD align="center"> day  </TD></TR>
 * <TR><TD> {@link #SECOND}      </TD><TD align="center"> 12 </TD><TD> Time, second part            </TD><TD align="center"> s    </TD></TR>
 * <TR><TD> {@link #MICROSECOND} </TD><TD align="center"> 16 </TD><TD> Time, microsecond part       </TD><TD align="center"> �s   </TD></TR>
 * <TR><TD> {@link #HEIGHT}      </TD><TD align="center"> 20 </TD><TD> Corrected Sea Surface Height </TD><TD align="center"> mm   </TD></TR>
 * <TR><TD> {@link #MEAN}        </TD><TD align="center"> 24 </TD><TD> Mean Sea Surface             </TD><TD align="center"> mm   </TD></TR>
 * <TR><TD> {@link #BAROMETRIC}  </TD><TD align="center"> 28 </TD><TD> Inverse barometer effect     </TD><TD align="center"> mm   </TD></TR>
 * </TABLE></TD>
 * </TR></TABLE>
 * </TD></TR></TABLE></CENTER>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Parser {
    /**
     * Journal vers lequel envoyer des informations sur les op�rations effectu�es.
     * Les op�rations r�pertori�es comprennent par exemple les ouvertures et les
     * fermetures de fichiers, ainsi que les recherches d'un enregistrement par
     * date.
     */
    static final Logger logger = Logger.getLogger("fr.ird.io.corssh");

    /**
     * Syst�me de coordonn�es utilis� pour le positionnement des mesures de la
     * hauteur de l'eau. L'ellipso�de de Topex/Poseidon utilis� ici est proche
     * de "WGS 1984".
     */
    private static GeographicCoordinateSystem coordinateSystem;

    /**
     * Facteur par lequel multiplier les degr�s (g�n�ralement la latitude ou la
     * longitude) pour obtenir les microdegr�s utilis�s dans les fichiers CORSSH.
     * Les degr�s sont m�moris�s sous forme d'entiers sous les �tiquettes {@link
     * #LATITUDE} et {@link #LONGITUDE}.
     */
    static final double DEGREES_TO_INT = 1E+6;

    /**
     * Facteur par lequel multiplier les hauteur en m�tres pour
     * obtenir les millim�tres utilis�s dans les fichiers CORSSH.
     * Les hauteurs sont m�moris�s sous forme d'entiers sous les
     * �tiquettes {@link #HEIGHT}, {@link #MEAN} ou {@link #BAROMETRIC}.
     */
    static final double METRES_TO_INT = 1E+3;

    /**
     * Valeur indiquant qu'une donn�e est situ�e au-dessus de la terre.
     * Dans les fichiers CORSSH, cette valeur est (malheureusement)
     * affect�e � la longitude.
     */
    static final int LONGITUDE_OVER_LAND = 0x7FFFFFFF;

    /**
     * Num�ro de passage du satellite. Ce champ
     * n'existe que dans l'en-t�te qui pr�c�de
     * les donn�es.
     *
     * @see #nextRecord
     * @see #getField
     */
    static final int PASSNUMBER = 0 << 2;

    /**
     * Nombre de donn�es qui suivront l'en-t�te.
     *
     * @see #nextRecord
     * @see #getField
     */
    static final int RECORD_COUNT = 1 << 2;

    /**
     * Constante d�signant le champ de la latitude.
     * Les latitudes sont exprim�es en microdegr�s.
     *
     * @see #nextRecord
     * @see #getField
     */
    public static final int LATITUDE = 0 << 2;

    /**
     * Constante d�signant le champ de la longitude.
     * Les longitudes sont exprim�es en microdegr�s.
     *
     * @see #nextRecord
     * @see #getField
     */
    public static final int LONGITUDE = 1 << 2;

    /**
     * Constante d�signant le champ du jour julien.
     * Le jour julien 1 correspond � l'�poch des
     * dates AVISO, soit le premier janvier 1950.
     *
     * @see #nextRecord
     * @see #getField
     * @see #getDate
     */
    public static final int JULIANDAY = 2 << 2;

    /**
     * Constante d�signant le champ
     * des secondes dans le jour.
     *
     * @see #nextRecord
     * @see #getField
     * @see #getDate
     */
    public static final int SECOND = 3 << 2;

    /**
     * Constante d�signant le champ des
     * microsecondes dans la secondes.
     *
     * @see #nextRecord
     * @see #getField
     * @see #getDate
     */
    public static final int MICROSECOND = 4 << 2;

    /**
     * Constante d�signant la hauteur corrig�e de
     * la surface de la mer. Ces hauteurs sont
     * exprim�es en millim�tres.
     *
     * @see #nextRecord
     * @see #getField
     */
    public static final int HEIGHT = 5 << 2;

    /**
     * Constante d�signant la hauteur moyenne de la surface de la mer. Cette information est utile
     * pour calculer l'anomalie de hauteur de la surface de la mer, � l'aide de l'expression
     * <code>getField({@link #HEIGHT})-getField({@link #MEAN})</code>. Toutes ces hauteurs
     * sont exprim�es en millim�tres.
     *
     * @see #nextRecord
     * @see #getField
     */
    public static final int MEAN = 6 << 2;

    /**
     * Effet de la pression atmosph�rique sur la hauteur de la surface de la mer. Cet effet
     * est exprim� en millim�tres.
     *
     * @see #nextRecord
     * @see #getField
     */
    public static final int BAROMETRIC = 7 << 2;

    /**
     * Construit un interpr�teur. Apr�s la construction de cet objet, les
     * enregistrements du fichiers peuvent �tre lus par des appels �
     * {@link #nextRecord}.
     */
    protected Parser() {
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
    public abstract void seek(final Date date) throws IOException;

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
    public abstract boolean nextRecord() throws IOException;

    /**
     * Indique si l'enregistrement courant est blanc. Un enregistrement est consid�r� blanc
     * si tous ses champs (tels que retourn�s par {@link #getField}) ont la valeur 0. Apr�s
     * la construction d'un objet {@link Parser}, l'enregistrement courant est initialement
     * blanc.
     */
    public abstract boolean isBlank();

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
    public abstract int getField(final int field);

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
    public abstract double getValue(final int field);

    /**
     * Retourne la date cod�e dans l'enregistrement courant,   en nombre de secondes
     * �coul�s depuis le 1er janvier 1970. Si la date n'est pas disponible, retourne
     * {@link Long#MIN_VALUE}.  Les classes d�riv�es peuvent red�finir cette m�thode
     * pour obtenir la date sans cr�er d'objet {@link Date} temporaire. En principe,
     * seul {@link InterleavedParser} (classe priv�e) red�finira cette m�thode afin
     * d'�viter les mauvaises surprises si l'utilisateur a red�finie la m�thode
     * {@link #getDate} d'une classe public. De plus, la convention voulant que
     * l'on retourne {@link Long#MIN_VALUE} pour une dat manquante est propre �
     * {@link InterleavedParser}.
     */
    long getTime() {
        final Date date=getDate();
        return (date!=null) ? date.getTime() : Long.MIN_VALUE;
    }

    /**
     * Retourne la date cod�e dans l'enregistrement courant. L'enregistrement courant
     * est celui qui a �t� lu lors du dernier appel de la m�thode {@link #nextRecord}.
     * La date sera exprim�e selon la convention du Java, c'est-�-dire en nombre de
     * millisecondes �coul�es depuis le 1er janvier 1970. Cette m�thode proc�de en
     * lisant les champs {@link #JULIANDAY}, {@link #SECOND} et {@link #MICROSECOND},
     * en supposant que ces champs utilisent le fuseau horaire UTC. La date retourn�e
     * peut �tre <code>null</code> si aucune donn�e n'est disponible.
     */
    public abstract Date getDate();

    /**
     * Retourne la date du premier enregistrement.
     *
     * L'appel de cette m�thode peut n�cessiter un balayage des donn�es jusqu'� la fin.
     * Les informations trouv�es seront conserv�es dans une cache interne afin d'�viter
     * un nouveau balayage lors des appels subs�quents.
     *
     * @return La date du premier enregistrement, ou <code>null</code> s'il n'y a pas de donn�es.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public abstract Date getStartTime() throws IOException;

    /**
     * Retourne la date du dernier enregistrement.
     *
     * L'appel de cette m�thode peut n�cessiter un balayage des donn�es jusqu'� la fin.
     * Les informations trouv�es seront conserv�es dans une cache interne afin d'�viter
     * un nouveau balayage lors des appels subs�quents.
     *
     * @return La date du dernier enregistrement, ou <code>null</code> s'il n'y a pas de donn�es.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public abstract Date getEndTime() throws IOException;

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
    public abstract int getPassCount() throws IOException;

    /**
     * Retourne le nombre d'enregistrements que contient le fichier,
     * en excluant les enregistrements d'en-t�te.
     *
     * L'appel de cette m�thode peut n�cessiter un balayage des donn�es jusqu'� la fin.
     * Les informations trouv�es seront conserv�es dans une cache interne afin d'�viter
     * un nouveau balayage lors des appels subs�quents.
     *
     * @return Le nombre de passe dans le fichier courant.
     * @throws IOException si une erreur est survenue lors de la lecture.
     */
    public abstract long getRecordCount() throws IOException;

    /**
     * Ferme le flot qui fournissait les donn�es. Apr�s l'appel de
     * cette m�thode, {@link #nextRecord} ne pourra plus �tre utilis�e.
     *
     * @throws IOException si une erreur est survenue lors de la fermeture.
     */
    public abstract void close() throws IOException;

    /**
     * Retourne une cha�ne de caract�res repr�sentant cet enregistrement.  Cette cha�ne de caract�res contient
     * des colonnes indiquant dans l'ordre la date de l'enregistrement, les coordonn�es (latitude et longitude),
     * la hauteur mesur�e, la hauteur moyenne et la correction barom�trique. La date est toujours �crite selon
     * le fuseau horaire UTC, et les hauteurs sont toujours exprim�es en m�tres. Un exemple de sortie est:
     *
     *  <pre>12/10/92 00:53:54   16�30,0'N  259�39,3'E   -14,447   -14,480     0,028</pre>
     */
    public abstract String toString();

    /**
     * Retourne l'objet � utiliser pour lire et �crire des dates.
     */
    static DateFormat getDateTimeInstance() {
        DateFormat dateFormat;
        dateFormat=DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

    /**
     * Retourne le syst�me de coordonn�es utilis� pour les
     * donn�es altim�triques des satellites Topex/Poseidon.
     */
    static synchronized GeographicCoordinateSystem getCoordinateSystem() {
        if (coordinateSystem == null) {
            final String name = "Topex/Poseidon";
            final Ellipsoid   ellipsoid = Ellipsoid.createEllipsoid(name, 6378136.3, 6356751.6, Unit.METRE);
            final HorizontalDatum datum = new HorizontalDatum(name, DatumType.GEOCENTRIC, ellipsoid, null);
            coordinateSystem = new GeographicCoordinateSystem(name, datum);
        }
        return coordinateSystem;
    }

    /**
     * Envoie une information vers le journal.
     *
     * @param classnam Nom de la classe qui appelle cette m�thode.
     * @param method   Nom de la m�thode qui appelle celle-ci.
     * @param message  Message � archiver.
     */
    static void log(final String classname, final String method, final String message) {
        final LogRecord record = new LogRecord(Level.FINE, message);
        record.setSourceClassName(classname);
        record.setSourceMethodName(method);
        logger.log(record);
    }

    /**
     * Envoie une information vers le journal.
     *
     * @param method  Nom de la m�thode qui appelle celle-ci.
     * @param message Message � archiver.
     */
    final void log(final String method, final String message) {
        log(getClass().getName(), method, message);
    }

    /**
     * Affiche les premiers enregistrements d'un fichier CORSSH d'AVISO.
     *
     * @param parser Objet � utiliser pour lire un ou des fichiers AVISO
     * @param start  Date du premier enregistrement � afficher.
     * @param n      Nombre maximal de lignes � afficher.
     * @throws IOException si une lecture a �chou�e.
     */
    static void main(final Parser parser, final Date start, int n) throws IOException {
        final Writer out = new OutputStreamWriter(System.out, "Cp850");
        final String lineSeparator = System.getProperty("line.separator", "\n");
        if (true) {
            String text;
            final NumberFormat format = NumberFormat.getNumberInstance();
            final double duration = (parser.getEndTime().getTime()-parser.getStartTime().getTime())/(24*60*60*1000.0);
            text=format.format(parser.getPassCount());   out.write("      Pass count="); out.write(Utilities.spaces(12-text.length())); out.write(text); out.write(lineSeparator);
            text=format.format(parser.getRecordCount()); out.write("    Record count="); out.write(Utilities.spaces(12-text.length())); out.write(text); out.write(lineSeparator);
            text=format.format(duration);                out.write(" Duration (days)="); out.write(Utilities.spaces(12-text.length())); out.write(text); out.write(lineSeparator);
        }
        if (start != null) {
            parser.seek(start);
            out.write(parser.toString());
            out.write(" (previous)");
            out.write(lineSeparator);
        }
        while (--n>=0 && parser.nextRecord()) {
            out.write(parser.toString());
            out.write(lineSeparator);
        }
        parser.close();
        out.close();
    }
}
