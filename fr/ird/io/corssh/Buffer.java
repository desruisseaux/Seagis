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

// Coordonn�es spatio-temporelle
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Entr�s/sorties et format
import java.io.IOException;
import java.text.DateFormat;
import java.text.FieldPosition;

// Divers...
import java.util.ConcurrentModificationException;
import java.io.Serializable;
import java.util.Arrays;
import net.seas.util.XArray;
import net.seas.util.XClass;
import fr.ird.resources.Resources;


/**
 * Repr�sentation en m�moire des enregistrements d'un fichier CORSSH d'AVISO.   Cette classe peut
 * �tre vue comme une copie en m�moire d'un ou de plusieurs fichiers CORSSH. Toutefois, seuls les
 * enregistrements qui apparaissent dans une r�gion g�ographique sp�cifi�e ainsi que dans un certain
 * interval de temps seront retenus. Ces deux axes (spatial et temporelle) sont sp�cifi�s d'une fa�on
 * diff�rente:
 *
 * <ul>
 *   <li>La r�gion g�ographique doit �tre sp�cifi�e au moment de la construction. Il s'agit le plus
 *       souvent d'un simple rectangle, sp�cifi� par un objet {@link java.awt.geom.Rectangle2D}. Il
 *       est toutefois possible de sp�cifier des formes plus complexes comme {@link java.awt.geom.Area},
 *       qui pourrait par exemple repr�senter un rectangle o� on a retranch� certaines r�gions.</li>
 *   <li>Les plages de dates des donn�es � retenir sont sp�cifi�es � chaque fois que la m�thode
 *       {@link #setTimeRange} est appel�e. A chaque fois que cette m�thode est appel�e, les nouvelles
 *       donn�es remplaceront les anciennes. Cette m�thode tente de minimiser la quantit� de donn�es �
 *       lire en r�utilisant les donn�es d�j� en m�moire. En appellant cette m�thode plusieurs fois de
 *       suite avec des plages de temps qui se chevauchent, on peut faire glisser efficacement une
 *       fen�tre temporelle.</li>
 *   <li>Si n�cessaire, un contr�le supl�mentaire peut �tre fait en red�finissant la m�thode
 *       {@link #getField} pour qu'elle retourne {@link Float#NaN} lorsqu'une donn�e ne doit
 *       pas �tre prise en compte.</li>
 * </ul>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class Buffer implements Serializable
{
    // TODO: Cette classe v�rifie si les dates sont en ordre strictement croissant.
    //       Si on permet d'intercaller des donn�es de plusieurs satellites, il faudra
    //       assouplir la v�rification pour se contenter d'un ordre croissant.

    /** Quantit� minimale par laquelle agrandir la m�moire. */ private static final int MIN_INCREASE = (1024*1024/4) /  8; // 128 ko
    /** Quantit� maximale par laquelle agrandir la m�moire. */ private static final int MAX_INCREASE = (1024*1024/4) * 64; //  64 Megs
    /** Nombre de cellules verticales dans {@link #index}.  */ private static final int ROW_COUNT    = 64;
    /** Nombre de cellules horizontales dans {@link #index}.*/ private static final int COLUMN_COUNT = 64; // Total de 4096 cellules.

    /**
     * Constante indiquant que la lecture des donn�es s'est
     * termin�e parce que la date limite a �t� atteinte.
     */
    public static final int ENDTIME_REACHED = 1;

    /**
     * Constante indiquant que la lecture des donn�es s'est
     * termin�e parce qu'on a lu le nombre maximal autoris�
     * d'enregistrements.
     */
    public static final int MAXRECORDS_REACHED = 2;

    /**
     * Constante indiquant que la lecture des donn�es s'est
     * termin�e parce qu'on a atteint la fin du fichier (ou
     * des fichiers si on lisait le contenu d'un ensemble
     * de fichiers ou d'un r�pertoire).
     */
    public static final int EOF_REACHED = 3;

    /**
     * Constante servant � acc�der � la latitude
     * d'une mesure dans le tableau {@link #data}.
     * Pour un <em>enregistrement</em> point� par
     * <code>index</code>, cette information est
     * obtenue par la ligne suivante:
     *
     * <pre>
     * final int latitude = {@link #data}[index+{@link #LATITUDE}];
     * </pre>
     */
    private static final int LATITUDE = 0;

    /**
     * Constante servant � acc�der � la longitude
     * d'une mesure dans le tableau {@link #data}.
     * Pour un <em>enregistrement</em> point� par
     * <code>index</code>, cette information est
     * obtenue par la ligne suivante:
     *
     * <pre>
     * final int longitude = {@link #data}[index+{@link #LONGITUDE}];
     * </pre>
     */
    private static final int LONGITUDE = 1;

    /**
     * Constante servant � acc�der aux bits les plus significatifs
     * de la date d'une mesure dans le tableau {@link #data}. Pour
     * un <em>enregistrement</em> point� par <code>index</code>, la
     * date est obtenue par la ligne suivante:
     *
     * <pre>
     * final long time = (((long) {@link #data}[index+{@link #DATAHI}]) << 32) + ((long) {@link #data}[index+{@link #DATALOW}]);
     * </pre>
     */
    private static final int DATEHI = 2;

    /**
     * Constante servant � acc�der aux bits les moins significatifs
     * de la date d'une mesure dans le tableau {@link #data}. Pour
     * un <em>enregistrement</em> point� par <code>index</code>, la
     * date est obtenue par la ligne suivante:
     *
     * <pre>
     * final long time = (((long) {@link #data}[index+{@link #DATAHI}]) << 32) + ((long) {@link #data}[index+{@link #DATALOW}]);
     * </pre>
     */
    private static final int DATELOW = 3;

    /**
     * Constante servant � acc�der � la valeur (altitude ou
     * anomalie) d'une mesure dans le tableau {@link #data}.
     * La valeur m�moris�e d�pend de la d�finition de la m�thode
     * {@link #getValue}. Pour un <em>enregistrement</em> point�
     * par <code>index</code>, la valeur est obtenue par la ligne
     * suivante:
     *
     * <pre>
     * final int value = {@link #data}[index+{@link #VALUE}];
     * </pre>
     */
    private static final int VALUE = 4;

    /**
     * Nombre d'�l�ments apparaissant dans
     * une mesure du tableau {@link #data}.
     */
    private static final int RECORD_LENGTH = 5;

    /**
     * Objet � utiliser pour lire les donn�es d'un ou plusieurs fichiers CORSSH.
     */
    private final Parser parser;

    /**
     * Indique si on peut continuer � ajouter des donn�es � partir de la position
     * actuelle de {@link #parser}.   Cette variable est consult�e et mise � jour
     * par la m�thode {@link #setTimeRange} seulement. La valeur <code>false</code>
     * indique que cette derni�re devra appeller {@link Parser#seek} avant de faire
     * de nouvelles lectures.
     */
    private boolean parserPositioned;

    /**
     * Nombre de donn�es valides dans {@link #data}. La quantit�
     * <code>recordCount*RECORD_LENGTH</code> doit toujours �tre
     * inf�rieur ou �gal � la capacit� <code>data.length</code>.
     */
    private int recordCount;

    /**
     * Tableau des donn�es lues � partir de {@link #parser}. Ce tableau peut �tre vu
     * comme une copie des fichiers CORSSH, mais o� les en-t�tes ont �t� exclus et
     * les donn�es r�organis�es un peu diff�rement afin d'�conomiser de la m�moire.
     * Seules les donn�es apparaissant dans la r�gion g�ographique {@link #geographicArea}
     * seront retenues. Les donn�es prises au dessus des terres seront exclues. Les
     * enregistrements de ce tableau sont organis�s comme suit:
     *
     * <table align=center bgcolor=FloralWhite border=1 cellspacing=0 cellpadding=6>
     *   <tr><td align=center bgcolor=Moccasin><strong> RECORD </strong></td></tr>
     *
     *   <tr><td> {@link #LATITUDE}    </td></tr>
     *   <tr><td> {@link #LONGITUDE}   </td></tr>
     *   <tr><td> {@link #DATEHI}      </td></tr>
     *   <tr><td> {@link #DATELOW}     </td></tr>
     *   <tr><td> {@link #VALUE}       </td></tr>
     * </table>
     *
     * La constante {@link #RECORD_LENGTH} donne
     * le nombre d'�l�ments par enregistrement.
     */
    private int[] data=new int[0];

    /**
     * Tableaux d'index des enregistrements de {@link #data}. Ce tableau d'index
     * acc�l�re consid�rablement le rep�rage des donn�es � des positions donn�es.
     * Pour une position (<var>x</var>,<var>y</var>) quelconque  (o� <var>x</var>
     * est la longitude et <var>y</var> la latitude en degr�s), on calcul d'abord
     * l'index de la cellule correspondante:
     *
     * <blockquote><pre>
     *     int column = (data[i+LONGITUDE]-xmin) / width;
     *     int row    = (data[i+ LATITUDE]-ymin) / height;
     *     int i      = row*COLUMN_COUNT + column;
     * </pre></blockquote>
     *
     * Note: le calcul pr�c�dent �tait fait dans un espace de coordonn�es enti�res,
     *       en utilisant des variables pr�-calcul�es. Un calcul �quivalent utilisant
     *       les coordonn�es r�elles serait:
     *
     * <blockquote><pre>
     *     int column = (int) Math.floor((x-geographicArea.x) / geographicArea.width  * COLUMN_COUNT);
     *     int row    = (int) Math.floor((y-geographicArea.y) / geographicArea.height *    ROW_COUNT);
     *     int i      = row*COLUMN_COUNT + column;
     * </pre></blockquote>
     *
     * Le tableau <code>index[i]</code> donne la liste des index des enregistrement
     * de {@link #data} qui sont comprises dans la cellule aux bords suivants:
     *
     * <blockquote><pre>
     *     double xmin = geographicArea.x + (column  )*(geographicArea.width  / COLUMN_COUNT);
     *     double xmax = geographicArea.x + (column+1)*(geographicArea.width  / COLUMN_COUNT);
     *     double ymin = geographicArea.y + (row     )*(geographicArea.height /    ROW_COUNT);
     *     double ymax = geographicArea.y + (row+1   )*(geographicArea.height /    ROW_COUNT);
     * </pre></blockquote>
     *
     * Les bords <code>xmin</code> et <code>ymin</code> sont inclusifs, tandis que les bords
     * <code>xmax</code> et <code>ymax</code> sont exclusifs.  A l'int�rieur d'un tableau
     * <code>index[i]</code> (pour un <var>i</var> quelconque), tous les �l�ments doivent
     * �tre class�s en ordre croissant. De plus, ces index auront d�j� �t� multipli�s par
     * {@link #RECORD_LENGTH}. Un tableau <code>index[i]</code> peut �tre <code>null</code>
     * s'il ne contient pas d'�l�ments.
     */
    private final int[][] index=new int[ROW_COUNT * COLUMN_COUNT][];

    /**
     * Indique si le tableau {@link #index} est valide. La valeur <code>false</code>
     * indique que la m�thode {@link #buildIndex} a besoin d'�tre appel�e.
     */
    private boolean indexValid;

    /**
     * R�gion g�ographiques � extraire. Les coordonn�es de cette r�gion doivent �tre exprim�es
     * en degr�s de latitude et de longitude, selon l'ellipsoide de r�f�rence de Topex/Poseidon.
     * La m�thode {@link Shape#contains(double,double)} sera utilis�e pour d�terminer si une
     * coordonn�e (<i>latitude</i>,<i>longitude</i>) est � l'int�rieur de la r�gion g�ographique.
     */
    private final Shape geographicArea;

    /**
     * Coordonn�es minimales de la r�gion g�ographique {@link #geographicArea}.  Ces
     * coordonn�es auront �t� multpli�es par {@link Parser#DEGREES_TO_INT}, de fa�on
     * � les exprimer selon le m�me syst�me de coordonn�es qui celui qui est utilis�
     * dans les fichiers CORSSH.
     */
    private final int xmin, ymin;

    /**
     * Dimensions d'une cellule (voir {@link #index}). Ces dimensions auront �t� multpli�es
     * par {@link Parser#DEGREES_TO_INT},  de fa�on � les exprimer selon le m�me syst�me de
     * coordonn�es qui celui qui est utilis� dans les fichiers CORSSH.
     */
    private final int width, height;

    /**
     * Format temporaire utilis� par {@link #toString}.
     */
    private transient DateFormat dateFormat;

    /**
     * Buffer temporaire r�serv�e � un usage interne par {@link #getPointsInside}.
     * On tentera autant que possible de cr�er ce buffer une seule fois et de le
     * r�utiliser � chaque appel de {@link #getPointsInside}.
     */
    private transient int[] indexInside;

    /**
     * Compte le nombre de fois que {@link #setTimeRange} a �t� appel�e. Cette information est
     * utilis�e par les it�rateurs afin de lancer une {@link ConcurrentModificationException}
     * si les donn�es ont �t� chang�es pendant qu'un it�rateur travaillait.
     */
    private int modification;

    /**
     * Construit un buffer qui contiendra les enregistrements des points apparaissant
     * dans la r�gion g�ographique sp�cifi�e. Les points en dehors de cette r�gion
     * seront ignor�es.
     *
     * @param parser         Objet � utiliser pour lire les donn�es d'un ou plusieurs fichiers CORSSH.
     * @param geographicArea R�gion g�ographiques � extraire. Les coordonn�es de cette r�gion doivent
     *                       �tre exprim�es en degr�s de latitude et de longitude, selon l'ellipsoide
     *                       de r�f�rence de Topex/Poseidon. La m�thode {@link Shape#contains(double,double)}
     *                       sera utilis�e pour d�terminer si une coordonn�e (<i>latitude</i>,<i>longitude</i>)
     *                       est � l'int�rieur de la r�gion g�ographique.
     */
    public Buffer(final Parser parser, final Shape geographicArea)
    {
        this.parser         = parser;
        this.geographicArea = geographicArea;
        final Rectangle2D areaBounds = geographicArea.getBounds2D();
        this.xmin   = (int)Math.floor( Parser.DEGREES_TO_INT * areaBounds.getMinX());
        this.ymin   = (int)Math.floor( Parser.DEGREES_TO_INT * areaBounds.getMinY());
        this.width  = (int)Math.ceil ((Parser.DEGREES_TO_INT * areaBounds.getMaxX() - xmin) / COLUMN_COUNT);
        this.height = (int)Math.ceil ((Parser.DEGREES_TO_INT * areaBounds.getMaxY() - ymin) /    ROW_COUNT);
    }

    /**
     * Sp�cifie la plage de temps des donn�es � charger. Seules les donn�es comprises dans la r�gion
     * g�ographique sp�cifi�e au constructeur seront retenues, ainsi que celles qui sont comprises
     * dans la plage de temps <code>[startTime..endTime]</code>.
     *
     * @param  startTime Date minimale (inclusive) de la premi�re donn�e � retenir.
     * @param    endTime Date maximale (exclusive) de la derni�re donn�e � retenir.
     * @return Constante indiquant pour quelle raison la lecture des donn�es s'est
     *         termin�e. Cette constante peut �tre {@link #ENDTIME_REACHED} ou
     *         {@link #EOF_REACHED}.
     * @throws IOException Si un probl�me est survenu lors de la lecture, ou si les
     *         dates lues dans le fichier ne sont pas en ordre strictement croissant.
     */
    public int setTimeRange(final Date startTime, final Date endTime) throws IOException
    {return setTimeRange(startTime, endTime, Integer.MAX_VALUE);}

    /**
     * Sp�cifie la plage de temps des donn�es � charger. Seules les donn�es comprises dans la r�gion
     * g�ographique sp�cifi�e au constructeur seront retenues, ainsi que celles qui sont comprises
     * dans la plage de temps <code>[startTime..endTime]</code>.
     *
     * @param  startTime Date minimale (inclusive) de la premi�re donn�e � retenir.
     * @param    endTime Date maximale (exclusive) de la derni�re donn�e � retenir.
     * @param  maxRecordCount Nombre maximal d'enregistrements � lire. Si ce nombre
     *         d'enregistrements a �t� atteint avant la date <code>endTime</code>,
     *         la lecture cessera. Utilisez {@link #getEndTime} pour cona�tre la
     *         date du dernier enregistrement lu.
     * @return Constante indiquant pour quelle raison la lecture des donn�es s'est
     *         termin�e. Cette constante peut �tre {@link #ENDTIME_REACHED},
     *         {@link #MAXRECORDS_REACHED} ou {@link #EOF_REACHED}.
     * @throws IOException Si un probl�me est survenu lors de la lecture, ou si les
     *         dates lues dans le fichier ne sont pas en ordre strictement croissant.
     */
    public synchronized int setTimeRange(final Date startTime, final Date endTime, final int maxRecordCount) throws IOException
    {
        modification++; // Must be first.
        final DateFormat  dateFormat = Parser.getDateTimeInstance();
        final StringBuffer logBuffer = new StringBuffer();

        Arrays.fill(index, null); indexValid=false;
        final Shape geographicArea = this.geographicArea;
        final long startTimeMillis = startTime.getTime();
        final long   endTimeMillis =   endTime.getTime();
        int          stopCondition =   ENDTIME_REACHED;
        int[] data=this.data;
        if (recordCount!=0)
        {
            /*
             * Rep�re le d�but des donn�es qui existent d�j�. Si les donn�es demand�es par l'utilisateur
             * commencent � ou apr�s la date du premier enregistrement en m�moire, on peut recopier au
             * d�but du tableau les donn�es qui sont d�j� en m�moire et charger seulement celles qui
             * manquent.
             */
            final int oldRecordCount=recordCount;
            final int index=search(startTimeMillis);
            if (index<recordCount && getMillis(data, index*RECORD_LENGTH)>=startTimeMillis)
            {
                final int copyCount = recordCount-index;
                System.arraycopy(data, index*RECORD_LENGTH, data, 0, copyCount*RECORD_LENGTH);
                recordCount = copyCount;
                recordCount = search(endTimeMillis);
                if (maxRecordCount < recordCount)
                {
                    recordCount   = maxRecordCount;
                    stopCondition = MAXRECORDS_REACHED;
                }

                // En cas de bug, vaux mieux avoir des 0 que des fausses donn�es.
                Arrays.fill(data, copyCount*RECORD_LENGTH, data.length, 0);
                assert(isOrdered());

                // Pr�pare un enregistrement qui sera ajout� au journal des �v�nements.
                logBuffer.append(Resources.format(Cl�.KEEP�3, dateFormat.format(getStartTime()),
                                                              dateFormat.format(getEndTime()),
                                                              new Double((double)recordCount/(double)oldRecordCount)));
                if (recordCount < copyCount)
                {
                    // L'utilisateur a rapproch� la date de fin.
                    // Il est inutile de proc�der � une lecture,
                    // mais la position de 'parser' ne sera plus
                    // valide.
                    parserPositioned = false;
                    log("setTimeRange", logBuffer.toString());
                    return stopCondition;
                }
                else logBuffer.append("; ");
            }
            else
            {
                // L'utilisateur a demand� une date ant�rieure � celle de
                // la premi�re donn�e en m�moire. Il faudra tout relire.
                recordCount      = 0;
                parserPositioned = false;
            }
        }
        else parserPositioned=false;
        /*
         * Date du dernier enregistrement. Cette information servira initialement � positionner
         * le flot apr�s la derni�re donn�e en m�moire. Elle servira ensuite � v�rifier que les
         * donn�es sont bien en ordre croissant de date. Note: le positionnement du flot ne sera
         * pas n�cessaire si on poursuit la lecture � partir de l� o� on s'�tait arr�t� la derni�re
         * fois.
         */
        stopCondition       = EOF_REACHED;
        long       lastTime = (recordCount!=0) ? getMillis(data, (recordCount-1)*RECORD_LENGTH) : startTimeMillis-1;
        long      firstTime = lastTime;
        final Parser parser = this.parser;
        if (parser.isBlank() || !parserPositioned)
        {
            parser.seek(new Date(lastTime+1));
            parserPositioned = parser.nextRecord();
        }
        if (parserPositioned)
        {
            firstTime=parser.getTime();
            assert(firstTime!=Long.MIN_VALUE);
            do // while (parser.nextRecord())
            {
                final long time=parser.getTime();
                assert(time!=Long.MIN_VALUE);
                if (time>=endTimeMillis)
                {
                    stopCondition = ENDTIME_REACHED;
                    break;
                }
                if (recordCount>=maxRecordCount)
                {
                    stopCondition = MAXRECORDS_REACHED;
                    break;
                }
                if (time<lastTime) // Accept identical time
                {
                    throw new IOException(Resources.format(Cl�.DATES_NOT_INCREASING));
                }
                lastTime = time;
                final int longitude = parser.getField(Parser.LONGITUDE); if (longitude==Parser.LONGITUDE_OVER_LAND) continue;
                final int latitude  = parser.getField(Parser.LATITUDE);
                if (geographicArea.contains(longitude/Parser.DEGREES_TO_INT, latitude/Parser.DEGREES_TO_INT))
                {
                    /*
                     * Copie en m�moire (dans le tableau {@link #data}) les informations qui nous
                     * int�ressent de l'enregistrement courant de l'interpr�teur <code>parser</code>.
                     */
                    final int index  = recordCount * RECORD_LENGTH;
                    final int length = index       + RECORD_LENGTH;
                    if (length>data.length)
                    {
                        if (maxRecordCount==Integer.MAX_VALUE || data.length!=0)
                        {
                            final int minCapacity = Math.max(data.length+MIN_INCREASE, length);
                            final int maxCapacity = Math.min(data.length+MAX_INCREASE, Math.min(maxRecordCount, Integer.MAX_VALUE/RECORD_LENGTH)*RECORD_LENGTH);
                            final int    capacity = Math.max(minCapacity, Math.min(maxCapacity, 2*data.length));
                            data = resize(capacity);
                        }
                        else data = resize(maxRecordCount);
                    }
                    data[index +  LATITUDE] = latitude;
                    data[index + LONGITUDE] = longitude;
                    data[index +    DATEHI] = (int) (time >>> 32);
                    data[index +   DATELOW] = (int) (time       );
                    data[index +     VALUE] = getField(parser);
                    recordCount++;
                    assert(getMillis(data, index)==time);
                }
            }
            while (parser.nextRecord());
        }
        /*
         * Lib�re un peu de m�moire s'il y en a trop de r�serv�e.
         */
        final int capacity = (recordCount*RECORD_LENGTH) + (MIN_INCREASE*4);
        if (capacity<data.length) data = resize(capacity);
        assert(isOrdered());

        if (firstTime < lastTime)
        {
            logBuffer.append(Resources.format(Cl�.LOAD�2,
                                              dateFormat.format(new Date(firstTime)),
                                              dateFormat.format(new Date( lastTime))));
        }
        else if (logBuffer.length()==0)
        {
            logBuffer.append(Resources.format(Cl�.NO_DATA_BETWEEN�2,
                                              dateFormat.format(startTime),
                                              dateFormat.format(  endTime)));
        }
        log("setTimeRange", logBuffer.toString());
        return stopCondition;
    }

    /**
     * Donne au tableau {@link #data} la capacit� sp�cifi�e. Si le tableau est agrandit,
     * les nouvelles valeurs seront 0. S'il est r�duit, les valeurs en trop seront perdues.
     * Cette m�thode va aussi envoyer un enregistrement au journal pour informer de cette
     * op�ration. Le nouveau tableau {@link #data} est retourn� par commodit�.
     */
    private int[] resize(final int capacity)
    {
        final int oldCapacity = data.length;
        data = XArray.resize(data, capacity);
        log("setTimeRange", Resources.format(Cl�.RESIZE�2, new Integer(oldCapacity/1024), new Integer(capacity/1024)));
        return data;
    }

    /**
     * Envoie une information vers le journal.
     *
     * @param method  Nom de la m�thode qui appelle celle-ci.
     * @param message Message � archiver.
     */
    private void log(final String method, final String message)
    {Parser.log(getClass().getName(), method, message);}

    /**
     * Indique si les enregistrements de {@link #data} sont en ordre croissant
     * de dates. En g�n�ral, c'est toujours le cas. Cette m�thode ne sera qu'�
     * v�rifier si un bug ne se serait pas introduit dans cette classe...
     */
    private boolean isOrdered()
    {
        long dateCheck=Long.MAX_VALUE;
        final int[] data=this.data;
        for (int i=recordCount*RECORD_LENGTH; (i-=RECORD_LENGTH)>=0;)
        {
            final long time = getMillis(data,i);
            if (time > dateCheck) return false; // Accept identical time
            dateCheck = time;
        }
        return true;
    }

    /**
     * Indique si les enregistrements de <code>data</code>
     * sont en ordre croissant. Cette v�rification sert
     * dans des instructions <code>assert</code>.
     */
    private static boolean isOrdered(final int[] data)
    {
        int z = Integer.MAX_VALUE;
        for (int i=data.length; --i>=0;)
        {
            if (data[i] >= z) return false;
            z=data[i];
        }
        return true;
    }

    /**
     * Retourne la date du premier enregistrement en m�moire. Si aucune
     * donn�e ne se trouve pr�sentement en m�moire, alors cette m�thode
     * retourne <code>null</code>.
     */
    public synchronized Date getStartTime()
    {return (recordCount!=0) ? getTime(0) : null;}

    /**
     * Retourne la date du dernier enregistrement en m�moire. Si aucune
     * donn�e ne se trouve pr�sentement en m�moire, alors cette m�thode
     * retourne <code>null</code>.
     */
    public synchronized Date getEndTime()
    {return (recordCount!=0) ? getTime(recordCount-1) : null;}

    /**
     * Retourne la date de l'enregistrement sp�cifi�.
     *
     * @param n Num�ro de l'enregistrement, de 0 jusqu'�
     *          <code>{@link #getRecordCount}-1</code>.
     */
    public synchronized Date getTime(final int n)
    {
        if (n>=0 && n<recordCount) return new Date(getMillis(data, n*RECORD_LENGTH));
        else throw new ArrayIndexOutOfBoundsException(n);
    }

    /**
     * Retourne la date de l'enregistrement sp�cifi�.
     *
     * @param n Num�ro de l'enregistrement, de 0 jusqu'�
     *          <code>{@link #getRecordCount}-1</code>.
     */
    final long getMillis(final int n) // TODO: m�thode temporaire pour acc�l�rer SemiVariance
    {return getMillis(data, n*RECORD_LENGTH);}

    /**
     * Retourne la date d'un enregistrement, en nombre de
     * millisecondes �coul�es depuis le 1er janvier 1970.
     *
     * @param data  Le tableau {@link #data} dans lequel lire la date.
     * @param index Index de l'enregistrement dont on veut la date. Cet
     *        index doit avoir �t� d�j� multipli� par {@link #RECORD_LENGTH}.
     */
    private static long getMillis(final int[] data, final int index)
    {
        assert((index % RECORD_LENGTH)==0);
        return (((long)data[index+DATEHI ]) << 32) |
               (((long)data[index+DATELOW]) & 0xFFFFFFFFL);
    }

    /**
     * Retourne la hauteur ou l'anomalie de la hauteur de l'eau m�moris�
     * dans l'enregistrement sp�cifi�. Le param�tre retourn� (hauteur,
     * anomalie de hauteur, etc.) d�pend de l'impl�mentation de
     * {@link #getField}.
     *
     * @param n Num�ro de l'enregistrement, de 0 jusqu'�
     *          <code>{@link #getRecordCount}-1</code>.
     */
    public synchronized float getHeight(final int n)
    {
        if (n>=0 && n<recordCount)
        {
            return (float) (data[n*RECORD_LENGTH+VALUE] / Parser.METRES_TO_INT);
        }
        else throw new ArrayIndexOutOfBoundsException(n);
    }

    /**
     * Retourne la hauteur ou l'anomalie de la hauteur de l'eau m�moris�
     * dans l'enregistrement sp�cifi�. Le param�tre retourn� (hauteur,
     * anomalie de hauteur, etc.) d�pend de l'impl�mentation de
     * {@link #getField}.
     *
     * @param n Num�ro de l'enregistrement, de 0 jusqu'�
     *          <code>{@link #getRecordCount}-1</code>.
     */
    final int getField(final int n) // TODO: m�thode temporaire pour acc�l�rer SemiVariance
    {return data[n*RECORD_LENGTH+VALUE];}

    /**
     * Retourne les coordonn�es de l'enregistrement sp�cifi�.
     *
     * @param n Num�ro de l'enregistrement, de 0 jusqu'�
     *          <code>{@link #getRecordCount}-1</code>.
     */
    public synchronized Point2D getPoint(int n)
    {
        if (n>=0 && n<recordCount)
        {
            n *= RECORD_LENGTH;
            final int[] data=this.data;
            return new Point2D.Double(data[n+LONGITUDE]/Parser.DEGREES_TO_INT, data[n+LATITUDE]/Parser.DEGREES_TO_INT);
        }
        else throw new ArrayIndexOutOfBoundsException(n);
    }

    /**
     * Retourne le nombre d'enregistrements qui ont �t� index�s. En principe,
     * ce nombre doit toujours �tre strictement �gal � {@link #recordCount}
     * (sauf si l'index n'a pas encore �t� construit).
     */
    private int getIndexCount()
    {
        int count = 0;
        final int[][] index = this.index;
        for (int i=index.length; --i>=0;)
        {
            final int[] cell=index[i];
            if (cell!=null)
                count += cell.length;
        }
        return count;
    }

    /**
     * Construit les index � partir de toutes les donn�es en m�moire. Le nouvel
     * index remplacera compl�tement l'ancien. Cette m�thode doit �tre appel�e
     * chaque fois que les donn�es en m�moire ont chang�es.
     */
    private void buildIndex()
    {
        final int[][] index = this.index;
        final int[]    data = this.data;
        final int     count = this.recordCount * RECORD_LENGTH;
        final int      xmin = this.xmin;
        final int      ymin = this.ymin;
        final int     width = this.width;
        final int    height = this.height;
        Arrays.fill(index, null); // Normalement d�j� fait.
        for (int i=0; i<count; i+=RECORD_LENGTH) // On doit utiliser l'ordre croissant.
        {
            final int k = Math.max(0, Math.min(   ROW_COUNT-1, (data[i+ LATITUDE]-ymin)/height))*COLUMN_COUNT +
                          Math.max(0, Math.min(COLUMN_COUNT-1, (data[i+LONGITUDE]-xmin)/width));
            int[] cell=index[k];
            if (cell!=null)
            {
                final int length = cell.length;
                cell = XArray.resize(cell, length+1);
                cell[length] = i; // Conserve l'ordre croissant.
            }
            else cell=new int[] {i};
            index[k] = cell;
        }
        assert(getIndexCount()==recordCount);
        indexValid = true;
    }

    /**
     * Retourne tous les enregistrements qui se trouvent dans la r�gion g�ographique
     * sp�cifi�e. Les coordonn�es de cette r�gion doivent �tre des degr�s de longitude
     * et de latitude selon l'ellipso�de de r�f�rence de Topex/Poseidon. Les points
     * retourn�s ne seront pas n�cessairement class�s selon aucun ordre particulier.
     *
     * @param  shape       R�gion g�ographique pour laquelle on veut des enregistrements.
     * @param  bounds      Rectangle englobant compl�tement la forme <code>shape</code>.
     *                     Ce rectangle se calcul par <code>shape.getBounds2D()</code>.
     * @param  lowerRecord Num�ro du premier enregistrement � prendre en compte.
     * @param  upperRecord Num�ro suivant celui du dernier enregistrement � prendre en compte.
     * @param  sorted      <code>true</code> pour balayer les points en ordre croissant de date,
     *                     ou <code>false</code> pour les balayer dans n'importe quel ordre.
     * @return Iterateur balayant les enregistrements compris dans la r�gion <code>shape</code>.
     */
    private Iterator getPointsInside(final Shape shape, final Rectangle2D bounds, int lowerRecord, int upperRecord, final boolean sorted)
    {
        lowerRecord *= RECORD_LENGTH;
        upperRecord *= RECORD_LENGTH;
        if (!indexValid) buildIndex();

        int count=0;
        int[] indexInside = this.indexInside;
        if (indexInside==null) this.indexInside = indexInside = new int[64];

        final int[]        data = this.data;
        final int[][]     index = this.index;
        final boolean delimited = (lowerRecord>0 || upperRecord<recordCount);
        final int      minXCell = Math.max(0, Math.min(COLUMN_COUNT-1, ((int)Math.floor(bounds.getMinX()*Parser.DEGREES_TO_INT)-xmin) / width ));
        final int      maxXCell = Math.max(0, Math.min(COLUMN_COUNT-1, ((int)Math.floor(bounds.getMaxX()*Parser.DEGREES_TO_INT)-xmin) / width ));
        final int      minYCell = Math.max(0, Math.min(   ROW_COUNT-1, ((int)Math.floor(bounds.getMinY()*Parser.DEGREES_TO_INT)-ymin) / height));
        final int      maxYCell = Math.max(0, Math.min(   ROW_COUNT-1, ((int)Math.floor(bounds.getMaxY()*Parser.DEGREES_TO_INT)-ymin) / height));

        for (int yCell=minYCell; yCell<=maxYCell; yCell++)
        {
            final int row = yCell*COLUMN_COUNT;
            for (int xCell=minXCell; xCell<=maxXCell; xCell++)
            {
                final int[] cell = index[row+xCell];
                if (cell != null)
                {
                    assert(isOrdered(cell));
                    int i;
                    if (delimited)
                    {
                        i = Arrays.binarySearch(cell, lowerRecord);
                        if (i<0) i = ~i;
                    }
                    else i=0;
                    while (i<cell.length)
                    {
                        final int recordPos = cell[i++];
                        assert((recordPos % RECORD_LENGTH)==0);
                        assert( recordPos >= lowerRecord);
                        if (recordPos >= upperRecord) break;
                        if (shape.contains(data[recordPos+LONGITUDE]/Parser.DEGREES_TO_INT,
                                           data[recordPos+ LATITUDE]/Parser.DEGREES_TO_INT))
                        {
                            if (count >= indexInside.length) this.indexInside = indexInside = XArray.resize(indexInside, count*2);
                            indexInside[count++] = recordPos;
                        }
                    }
                }
            }
        }
        final int[] points=new int[count];
        System.arraycopy(indexInside, 0, points, 0, count);
        if (sorted) Arrays.sort(indexInside);
        final int mod = modification;
        return new Iterator()
        {
            private int i   = -1;
            private int pos = -1;
            public boolean  next() {if (++i<points.length) {pos=points[i]; return true;} else return false;}
            public double   getX() {final double x=data[pos+LONGITUDE]/Parser.DEGREES_TO_INT; check(); return x;}
            public double   getY() {final double y=data[pos+ LATITUDE]/Parser.DEGREES_TO_INT; check(); return y;}
            public int  getField() {final    int z=data[pos+    VALUE];                       check(); return z;}
            public long  getTime() {final   long t=getMillis(data, pos);                      check(); return t;}

            private final void check() throws ConcurrentModificationException
            {if (mod!=modification) throw new ConcurrentModificationException();}
        };
    }

    /**
     * Retourne tous les enregistrements qui se trouvent dans la r�gion g�ographique
     * sp�cifi�e. Les coordonn�es de cette r�gion doivent �tre des degr�s de longitude
     * et de latitude selon l'ellipso�de de r�f�rence de Topex/Poseidon. Les �l�ments
     * du tableau retourn� ne seront pas class� selon un ordre particulier.
     *
     * @param  shape R�gion g�ographique pour laquelle on veut des enregistrements.
     * @return It�rateur balayant les enregistrements compris dans la r�gion <code>shape</code>.
     */
    public synchronized Iterator getPointsInside(final Shape shape)
    {return getPointsInside(shape, shape.getBounds2D(), 0, recordCount, false);}

    /**
     * Retourne tous les enregistrements qui se trouvent dans la r�gion g�ographique
     * et la plage de temps sp�cifi�es.  La plage de temps doit intercepter la plage
     * sp�cifi�e lors du dernier appel de {@link #setTimeRange}, sinon aucune donn�e
     * ne sera trouv�e.
     *
     * @param  shape R�gion g�ographique pour laquelle on veut des enregistrements.
     * @param  startTime Date et heure de la premi�re donn�e � prendre en compte.
     * @param  endTime Date et heure suivant celle de la premi�re donn�e � prendre en compte.
     * @return It�rateur balayant les enregistrements compris dans la r�gion <code>shape</code>
     *         et la plage de temps <code>[startTime..endTime]</code>.
     */
    public synchronized Iterator getPointsInside(final Shape shape, final Date startTime, final Date endTime)
    {return getPointsInside(shape, shape.getBounds2D(), search(startTime.getTime()), search(endTime.getTime()), false);}

    /**
     * Trouve le num�ro de l'enregistrement dont la date est �gale ou sup�rieur � la date sp�cifi�e.
     * Si aucun enregistrement n'a exactement la date sp�cifi�e,     alors cette m�thode retourne le
     * "point d'insertion"    (qui peut �tre 0 ou {@link #getRecordCount()} si la date sp�cifi�e est
     * avant la premi�re donn�e ou apr�s la derni�re donn�e, respectivement). Si aucune donn�e n'est
     * actuellement m�moris�e, alors cette m�thode retourne 0 (ce qui est coh�rent avec les autres cas).
     */
    public synchronized int search(final Date date)
    {
        assert(isOrdered());
        return search(date.getTime());
    }

    /**
     * Trouve le num�ro de l'enregistrement dont la date est �gale ou sup�rieur � la date sp�cifi�e.
     * Cette m�thode effectue une recherche bi-lin�aire, ce qui suppose que les enregistrements sont
     * en ordre croissant de date.   Si aucun enregistrement n'a exactement la date sp�cifi�e, alors
     * cette m�thode retourne le "point d'insertion"  (qui peut �tre 0 ou {@link #recordCount} si la
     * date sp�cifi�e est avant la premi�re donn�e ou apr�s la derni�re donn�e, respectivement).  Si
     * aucune donn�e n'est actuellement m�moris�e, alors cette m�thode retourne 0 (ce qui est coh�rent
     * avec les autres cas).
     */
    private int search(final long date)
    {
        // Note: placer 'assert(isOrdered())' ici aurait un co�t prohibitif.
        final int[] data=this.data;
        int index = 0;
        int lower = 0;
        int upper = recordCount-1;
        while (lower <= upper)
        {
            index = (lower + upper) >>> 1;
            assert(index>=0 && index<recordCount) : index;
            final long time=getMillis(data, index*RECORD_LENGTH);
            if (time<date) {lower=index+1; continue;}
            if (time>date) {upper=index-1; continue;}
            // If an exact match has been found,
            // move to the first ocurence.
            while (index!=0 && getMillis(data, (index-1)*RECORD_LENGTH)==time) index--;
            return index;
        }
        assert(lower>=0 && lower<=recordCount);
        assert(lower == 0           || getMillis(data, (lower-1)*RECORD_LENGTH) < date);
        assert(lower >= recordCount || getMillis(data, (lower  )*RECORD_LENGTH) > date);
        return lower;
    }

    /**
     * Retourne le nombre d'enregistrement pr�sentement en m�moire.
     */
    public synchronized int getRecordCount()
    {return recordCount;}

    /**
     * Retourne la mesure cod�e dans l'enregistrement courant de <code>parser</code>.
     * L'enregistrement courant est celui qui a �t� lu lors du dernier appel de la m�thode
     * {@link Parser#nextRecord}. L'impl�mentation par d�faut retourne l'anomalie altim�trique,
     * obtenue comme suit:
     *
     * <pre>parser.getField({@link Parser#HEIGHT})-parser.getField({@link Parser#MEAN})</pre>
     *
     * Les classes d�riv�es peuvent red�finir cette m�thode pour retourner une
     * autre mesure. La mesure retourn�e doit �tre exprim�e en millim�tres.
     */
    protected int getField(final Parser parser)
    {return parser.getField(Parser.HEIGHT)-parser.getField(Parser.MEAN);}

    /**
     * Ferme les fichiers qui �taient ouverts
     * et lib�re la m�moire qui �tait r�serv�e.
     *
     * @throws IOException si la fermeture a �chou�.
     */
    public void close() throws IOException
    {
        parser.close();
        data=new int[0];
    }

    /**
     * Renvoie une cha�ne de caract�re contenant quelques
     * informations sur l'�tat de cet objet {@link Buffer}.
     */
    public synchronized String toString()
    {
        final StringBuffer buffer=new StringBuffer(XClass.getShortClassName(this));
        buffer.append('[');
        buffer.append(recordCount);
        buffer.append(" records");
        if (recordCount!=0)
        {
            final FieldPosition dummy=new FieldPosition(0);
            if (dateFormat==null) dateFormat=Parser.getDateTimeInstance();
            buffer.append(" from "); dateFormat.format(getStartTime(), buffer, dummy);
            buffer.append(" to ");   dateFormat.format(  getEndTime(), buffer, dummy);
        }
        buffer.append(']');
        return buffer.toString();
    }
}
