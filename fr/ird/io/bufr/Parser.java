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
package fr.ird.io.bufr;

// Entr�s/sorties
import java.io.IOException;
import java.io.ObjectInputStream;
import javax.imageio.IIOException;
import javax.imageio.stream.ImageInputStream;

// Temps
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.GregorianCalendar;

// Divers
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import fr.ird.util.XArray;


/**
 * La structure des fichiers BUFR est expliqu� aux pages suivantes:
 *
 * <p align="center">
 * <a href="http://www.wmo.ch/web/www/reports/Guide-binary-1A.html">http://www.wmo.ch/web/www/reports/Guide-binary-1A.html</a>
 * <br><br>
 * <a href="http://www.wmo.ch/web/www/reports/Guide-binary-1B.html">http://www.wmo.ch/web/www/reports/Guide-binary-1B.html</a>
 * </p>
 */
final class Parser
{
    //    0 = 0000    4 = 0100    8 = 1000    C = 1100
    //    1 = 0001    5 = 0101    9 = 1001    D = 1101
    //    2 = 0010    6 = 0110    A = 1010    E = 1110
    //    3 = 0011    7 = 0111    B = 1011    F = 1111

    /**
     * Journal vers lequel envoyer des informations sur les op�rations effectu�es.
     * Les op�rations r�pertori�es comprennent par exemple la lecture d'une nouvelle
     * section.
     */
    private static final Logger logger = Logger.getLogger("fr.ird.io.bufr");

    /**
     * BUFR Table A.  Les cl�s seront des objets {@link Byte} qui repr�sentent
     * le code inscrit dans la section 1 des fichiers BUFR. Les valeurs seront
     * des objets {@link String}.
     */
    private final Map<Byte,String> tableA;

    /**
     * BUFR Table B et D. Les cl�s seront des objets {@link Short} qui repr�sentent le code FXY (Section 3)
     * non-d�compress� (c'est-�-dire sur leur deux octets originaux). Les valeurs peuvent �tre un objet
     * {@link Descriptor}, ou un tableau <code>short[]</code> qui contient des cl�s vers d'autres valeurs
     * de cette m�me table.
     */
    private final Map<Short,Object> tableBD;

    /**
     * Calendrier � utiliser pour le d�codage des dates.
     * Ce calendrier utilise le fuseau horaire GMT.
     */
    private final Calendar calendar=new GregorianCalendar(TimeZone.getTimeZone("GMT"));

    /**
     * Buffer � utiliser pour m�moriser
     * temporairement les octets lus.
     */
    private final byte[] buffer=new byte[18];

    /**
     * Position du d�but du fichier.
     */
    private transient long origine;

    /**
     * Date d�clar�e dans le fichier (section 2), en nombre
     * de millisecondes �coul�es depuis le 1er janvier 1970.
     */
    private transient long date;

    /**
     * Longueur du fichier BUFR, ou -1 si cette longueur n'est pas connue.
     * Cette longueur sera de pr�f�rence celle qui �tait d�clar�e � l'int�rieur
     * du fichier.
     */
    private transient long length;

    /**
     * Num�ro de version de ce fichier BUFR.
     */
    private transient byte version;

    /**
     * Cat�gorie des donn�es (table A).
     */
    private transient byte category;

    /**
     * Indique si les donn�es sont des observations.
     * La valeur <code>false</code> signifie que les
     * donn�es proviennent par exemple d'une pr�diction.
     */
    private transient boolean observation;

    /**
     * Indique si les donn�es sont compress�es.
     */
    private transient boolean compressed;

    /**
     * Nombre d'enregistrements.
     */
    private int subsetCount;

    /**
     * Descriptions des param�tres. Ces descriptions sont cod�s dans la section 3
     * des fichiers BUFR et concernent la section 4 de ces m�mes fichiers.
     */
    private transient Descriptor[] descriptors;

    /**
     * Construit un d�codeur.
     *
     * @throws IOException si la construction du d�codeur a �chou�e.
     */
    public Parser() throws IOException
    {
        final ObjectInputStream in=new ObjectInputStream(getClass().getClassLoader().getResourceAsStream(TableCompiler.TABLES));
        try
        {
            tableA  = (Map) in.readObject();
            tableBD = (Map) in.readObject();
        }
        catch (ClassNotFoundException exception)
        {
            throw new IIOException(exception.getLocalizedMessage(), exception);
        }
        in.close();
    }

    /**
     * Affiche des informations sur la section en cours de lecture. Ces informations ne servent qu'a
     * des fins de d�boguage. Cette m�thode doit �tre appel�e au DEBUT d'une nouvelle section, avant
     * toute lecture r�elle (parce qu'elle peut �craser {@link #buffer}).
     */
    private void beginSection(final String method, final int section, final ImageInputStream input) throws IOException
    {
        final long pos=input.getStreamPosition();
        final StringBuffer msg=new StringBuffer("Reading section ");  // TODO: localize
        msg.append(section);
        msg.append(" from position ");
        msg.append(pos-origine);
        if (section>0 && section<5)
        {
            input.readFully(buffer, 0, 4);
            input.seek(pos);
            msg.append(" (");
            msg.append(getInt3(0));
            msg.append(" bytes)");
        }
        final LogRecord record = new LogRecord(Level.FINER, msg.toString());
        record.setSourceClassName(Parser.class.getName());
        record.setSourceMethodName(method);
        logger.log(record);
    }

    /**
     * Proc�de � la lecture des sections
     * 0 � 3 d'un fichier BUFR.
     *
     * @throws IOException si la lecture a �chou�e.
     */
    public void open(final ImageInputStream input) throws IOException
    {
        origine=input.getStreamPosition();
        calendar.clear();
        /*
         * D�code la section 0 du fichier BUFR.
         */
        beginSection("open",0,input);
        input.readFully(buffer, 0, 8); // La section 0 contient toujours 8 octets.
        if (!new String(buffer, 0, 4).equals("BUFR")) throw new IIOException("Not a BUFR"); // TODO: localize
        length=((version=buffer[7])>=2) ? getInt3(4) : Math.max(-1, input.length()-origine);
        /*
         * D�code la section 1 du fichier BUFR.
         */
        beginSection("open",1,input);
        input.readFully(buffer, 0, 18); // La section 1 contient au moins 18 octets.
        input.skipBytes(getInt3(0)-18); // Ignore imm�diatement les octets en trop.
        setMasterTable(buffer[3]);      // D�finit la table maitresse (normalement 0).
        category =   buffer[8];         // Cat�gorie des donn�es (BUFR Table A).
        calendar.set(buffer[12]+1900,   // Ann�e � partir de 1900.
                     buffer[13]-1,      // Mois (1-12).
                     buffer[14],        // Jour (1-31).
                     buffer[15],        // Heures (0-23).
                     buffer[16]);       // Minutes (0-59).
        date=calendar.getTime().getTime();
        if ((buffer[7] & 128)!=0)
        {
            /*
             * D�code la section 2 du fichier BUFR.
             */
            beginSection("open",2,input);
            input.readFully(buffer, 0, 4); // La section 2 contient au moins 4 octets.
            input.skipBytes(getInt3(0)-4); // Ignore imm�diatement tous les octets restants.
        }
        /*
         * D�code la section 3 du fichier BUFR.
         */
        beginSection("open",3,input);
        input.readFully(buffer, 0, 7);          // La section 3 commencent par 3 octets suivit des descriptions des param�tres.
        observation = ((buffer[6]&0x80)!=0);    // Indique si les donn�es proviennent d'observations.
        compressed  = ((buffer[6]&0x40)!=0);    // Indique si les donn�es sont compress�es.
        subsetCount = getInt2(4);               // ??? Qu'est-ce que c'est que ce truc ???? (TODO).
        descriptors = new Descriptor[64];       // Pr�pare le tableau des descripteurs de param�tres. On l'agrandira aux besoins.
        final int[] replication=new int[3];
        final int[] operators  =new int[6];
        int index=0,lengthLeft=getInt3(0)-7;
        while (lengthLeft>=2)
        {
            lengthLeft -= 2;
            input.readFully(buffer, 7, 2);
            index=addDescriptor(getInt2(7), index, replication, operators);
        }
        if (replication[0]!=0)
        {
            throw new IIOException("Op�rateur de copie (section 3) incomplet."); // TODO: localize
        }
        descriptors = XArray.resize(descriptors, index);
        input.skipBytes(lengthLeft); // Ignore les octets restants.
    }

    /**
     * Proc�de au d�codage d'un descripteur.
     *
     * @param  FXY         Code FXY du descripteur � ajouter au tableau {@link #descriptor}.
     * @param  index       Index dans le tableau {@link #descriptor} ou placer le descripteur.
     * @param  replication Tableau <code>int[3]</code> avec des valeurs initiales de 0.  Si le descripteur est un
     *                     op�rateur de copie ("replication operator", F=1), alors le tableau prendra les valeurs
     *                     suivants: 0) le nombre X d'�lements � copier, 1) le nombre Y de fois qu'il faut copier
     *                     et 2) l'index � partir d'o� il faudra faire la copie.  Ces informations sont v�rifi�es
     *                     chaque fois qu'un appel � <code>addDescriptor(...)<code> se termine pour v�rifier s'il
     *                     faut copier les descripteurs que l'on vient de lire.
     * @param  operators   Un tableau <code>int[6]</code> avec des valeurs initiales de 0. Cette m�thode y placera
     *                     les valeurs sp�cifi�es par les descripteurs d'op�rations ("operation descriptor", F=2).
     * @return Index du prochain �l�ment libre dans le tableau {@link #descriptor}.
     * @throws IIOException si le code FXY sp�cifi� n'a pas �t� reconnu.
     */
    private int addDescriptor(int FXY, int index, final int[] replication, final int[] operators) throws IIOException
    {
        final int F = (FXY & 0xC000) >>> 14; // Les 2 premiers bits.
        final int X = (FXY & 0x3F00) >>>  8; // Les 6 bits suivants.
        final int Y = (FXY & 0x00FF) >>>  0; // Les 8 bits suivants.
        switch (F)
        {
            case 1:
            {
                ////////////////////////////////
                ////  Replication operator  ////
                ////////////////////////////////
                if (Y==0)
                {
                    throw new IIOException("Delayed replication not supported"); // TODO: localize
                }
                if (replication[1]!=0)
                {
                    throw new IIOException("Op�rateurs de copies (section 3) imbriqu�s."); // TODO: localize
                }
                replication[0] = X;
                replication[1] = Y;
                replication[2] = index;
                return index;
            }
            case 2:
            {
                ////////////////////////////////
                ////  Operation descriptor  ////
                ////////////////////////////////
                switch (X)
                {
                    case 1:
                    {
                        // Change data width: Add (Y-128) bits to the data width for each data element
                        // in Table B, other than CCITT IA5 (character) data, code or flag tables.
                        operators[0] = Y-128;
                        return index;
                    }
                    case 2:
                    {
                        // Change scale: Multiply scale given for each non-code
                        // data elements in Table B by 10(Y-128).
                        operators[1] = Y-128;
                        return index;
                    }
                    case 3:
                    {
                        // Change reference: Subsequent element values descriptors define new reference
                        // values for corresponding Table B entries. Each new reference value is represented
                        // by Y bits in the Data Section. Definition of new reference values in concluded by
                        // encoding this operator with Y=255. Negative reference values shall be represented
                        // by a positive integer with the left-most bit (bit 1) set to 1.
                    }
                    case 4:
                    {
                        // Add associated: Precede each data element field with Y bits of information. This
                        // operation associates a data field (e.g. quality control infor-mation) of Y bits
                        // with each data element.
                    }
                    case 5:
                    {
                        // Signify character Y characters (CCITT international Alphabet No. 5) are inserted
                        // as a data field of Y x 8 bits in length.
                    }
                    case 6:
                    {
                        // Signify data width for the immediately following local descriptor:
                        // Y bits of data are described by the immediately following descriptor
                    }
                }
                // Fall through (pour lancer l'exception).
            }
            case 3: // Fall through
            case 0:
            {
                ///////////////////////////////
                ////  Sequence descriptor  ////
                ////  Element descriptor   ////
                ///////////////////////////////
                final Object value=tableBD.get(new Short((short)FXY));
                if (value instanceof Descriptor)
                {
                    if (index >= descriptors.length)
                        descriptors = XArray.resize(descriptors, index*2);
                    descriptors[index++] = ((Descriptor) value).rescale(operators[1], operators[0]);
                }
                else if (value instanceof short[])
                {
                    final short[] sequence=(short[]) value;
                    final int[] replicationNextLevel=new int[3];
                    for (int i=0; i<sequence.length; i++)
                    {
                        index=addDescriptor(sequence[i], index, replicationNextLevel, operators);
                    }
                    if (replicationNextLevel[0]!=0)
                    {
                        throw new IIOException("Op�rateur de copie (section 3) incomplet."); // TODO: localize
                    }
                }
                else
                {
                    final StringBuffer buffer=new StringBuffer();
                    buffer.append(F); buffer.append('\u00A0'); int length=buffer.length();
                    buffer.append(X); for (int i=buffer.length(); i<4; i++) buffer.insert(length, '0'); buffer.append('\u00A0'); length=buffer.length();
                    buffer.append(Y); for (int i=buffer.length(); i<8; i++) buffer.insert(length, '0');
                    throw new IIOException("Code FXY inconnu dans la section 3, descripteur "+(index+1)+": "+buffer); // TODO: localize
                }
                break;
            }
        }
        /*
         * Parvenu � ce stade, on vient de terminer l'ajout d'un descripteur.
         * V�rifie maintenant si on vient d'atteindre la fin d'une s�quence
         * que l'on doit recopier. Si oui, proc�de � la copie.
         */
        if (replication[0]>0)
        {
            if (--replication[0]==0)
            {
                final int lower=replication[2];
                final int count=index-lower;
                while (replication[1]>0)
                {
                    replication[1]--;
                    System.arraycopy(descriptors, lower, descriptors, index, count);
                    index += count;
                }
            }
        }
        return index;
    }

    /**
     * Proc�de � la lecture des donn�es de la table. Cette
     * m�thode ne peut �tre appel�e qu'apr�s {@link #open}.
     */
    public void read(final ImageInputStream input) throws IOException
    {
        final int subsetCount=this.subsetCount;
        final Descriptor[] descriptors=this.descriptors;
        final float[][] data=new float[descriptors.length][];
        /*
         * D�code la section 4 du fichier BUFR.
         */
        beginSection("read",4,input);
        input.readFully(buffer, 0, 4);
        final int length = getInt3(0);
        long bitsRead=0;
        if (!compressed)
        {
            /*
             * D�codage des donn�es dans leur forme non-compress�e. Ces donn�es sont constitu�es d'une suite
             * d'enregistrements (observations). Chaque enregistrement contient une valeur pour chaque param�tres
             *
             *                                    Section 4 data non-compressed
             *         +------------------------------------------------------------------------------+
             *         �                                                                              �
             *         �parameter 1,parameter 2,..parameter n   parameter 1,parameter 2,..parameter n �
             *         ��                                   �  �                                     ��
             *         �+-----------------------------------+  +-------------------------------------+�
             *         �          observation 1                         observation 2                 �
             *         �                                                                              �
             *         +------------------------------------------------------------------------------+
             */
            for (int j=0; j<descriptors.length; j++)
                data[j]=new float[subsetCount];
            for (int i=0; i<subsetCount; i++)
            {
                for (int j=0; j<descriptors.length; j++)
                {
                    final Descriptor d=descriptors[j];
                    data[j][i]=d.decode(input.readBits(d.width));
                    bitsRead += d.width;
                }
            }
        }
        else
        {
            /*
             * D�codage des donn�es dans leur forme compress�e. Contrairement � la forme non-compress�e,
             * la forme compress�e contient une suite de valeurs pour un param�tres donn�, plut�t qu'une
             * suite d'enregistrements complet (en d'autres termes, elle contient une suite de "colonnes"
             * plut�t qu'une suite de "lignes").
             *
             *                                     Section 4 data compressed
             *    +--------------------------------------------------------------------------------------+
             *    �                                                                                      �
             *    �minimum value, bit count, parameter 1,...  minimum value, bit count, parameter 2,...  �
             *    ��                                        � �                                       �  �
             *    �+----------------------------------------+ +---------------------------------------+  �
             *    �   observation 1,...observation n              observation 1,...observation n         �
             *    +--------------------------------------------------------------------------------------+
             */
            for (int j=0; j<descriptors.length; j++)
            {
                // NOTE POUR LE JDK 1.4 (TODO):
                // VERIFIER DANS LA VERSION FINALE QUE
                // input.readBits(0) RETOURNE 0 ET QUE
                // input.readBits(65) LANCE UNE EXCEPTION.
                final Descriptor  d = descriptors[j];
                final long  minimum = input.readBits(d.width);
                final int bitsWidth = (int)input.readBits(6); // Peut �tre 0 si toutes les valeurs sont �gales � 'minimum'.
                final long localPad = (1L << bitsWidth)-1;
                bitsRead += (d.width+6);
                for (int i=0; i<subsetCount; i++)
                {
                    // Cet algorithme g�re correctement les cas particuliers o� "bitsWidth==0"
                    // (toutes les valeurs sont identiques) et "minimum==missing value" (toutes
                    // les valeurs sont manquantes).
                    final long localValue = input.readBits(bitsWidth);
                    final float value;
                    if (localValue==localPad) value=Float.NaN;
                    else value = d.decode(localValue+minimum);
                }
                bitsRead += bitsWidth*subsetCount;
            }
        }
        /*
         * Ignore les octets restants.
         */
        long bytesFullyRead = bitsRead >> 3;
        if ((bitsRead & 0x07)!=0) bytesFullyRead--;
        input.skipBytes(length-14 - bytesFullyRead); // TODO: pourquoi 14 au lieu de 4?
        /*
         * D�code la section 5 du fichier BUFR.
         */
        beginSection("read",5,input);
        input.readFully(buffer, 0, 4);
        if (!new String(buffer, 0, 4).equals("7777")) throw new IIOException("Missing end section"); // TODO: localize
    }

    /**
     * Retourne en entier cod� sur 2 octets dans
     * {@link #buffer}, � partir de l'index sp�cifi�.
     */
    private int getInt2(final int offset)
    {return ((buffer[offset]&0xFF) << 8) | (buffer[offset+1]&0xFF);}

    /**
     * Retourne en entier cod� sur 3 octets dans
     * {@link #buffer}, � partir de l'index sp�cifi�.
     */
    private int getInt3(final int offset)
    {return ((buffer[offset]&0xFF) << 16) | ((buffer[offset+1]&0xFF) << 8) | (buffer[offset+2]&0xFF);}

    /**
     * D�finit la table ma�tresse du fichier. Cette m�thode est appel�e automatiquement
     * lors de la lecture du fichier BUFR d�s que le num�ro de cette table est connue.
     * La table 0 correspond � la table standard WMO FM 94 BUFR. Par d�faut, aucune autre
     * table n'est accept�e.
     *
     * @param  table Num�ro de la table maitresse.
     * @throws IIOException si la table sp�cifi�e n'est pas support�e.
     */
    protected void setMasterTable(final int table) throws IIOException
    {if (table!=0) throw new IIOException("Unsupported master table: "+table);} // TODO: localize

    /**
     * Retourne la date d�clar�e dans le fichier.
     */
    public Date getDate()
    {return new Date(date);}

    /**
     * Retourne la cat�gorie des donn�es du fichier.
     * Si la cat�gorie n'est pas connue, alors cette
     * m�thode retourne <code>null</code>.
     */
    public String getCategory()
    {return tableA.get(new Byte(category));}

    /**
     * Indique si les donn�es sont des observations.
     * La valeur <code>false</code> signifie que les
     * donn�es proviennent par exemple d'une pr�diction.
     */
    public boolean isObservation()
    {return observation;}

    /**
     * Indique si les donn�es sont compress�es.
     */
    public boolean isCompressed()
    {return compressed;}

    /**
     * Retourne une repr�sentation de ce d�codeur
     * sous forme de cha�ne de caract�res.
     */
    public String toString()
    {return "Parser[version "+version+", "+length+" bytes]";}

    /**
     *
     */
    public static void main(final String[] args) throws IOException
    {
        final String filename = (args.length!=0) ? args[0] : "E:/Martin Desruisseaux/Donn�es/Bufr/58.8X.trt1509.17.F";
        final ImageInputStream input=new javax.imageio.stream.FileImageInputStream(new java.io.File(filename));
//      input.skipBytes(32);
        final Parser parser=new Parser();
        parser.open(input);
        if (true)
        {
            System.out.println(parser);
            System.out.println(parser.getDate());
            System.out.println(parser.getCategory());
            System.out.println(parser.isObservation());
            System.out.println(parser.isCompressed());
            System.out.println(parser.subsetCount);
            for (int i=0; i<parser.descriptors.length; i++)
                System.out.println(parser.descriptors[i]);
        }
//      parser.read(input);
        input.close();
    }
}
