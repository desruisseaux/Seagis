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
import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;

// Gestion des dates
import java.util.Date;
import java.text.DateFormat;
import java.text.FieldPosition;

// Collections
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;

// Divers
import java.awt.Shape;
import org.geotools.resources.Utilities;
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Ensemble de fichiers CORSSH. Cet ensemble sera consid�r� comme un flot
 * continu de donn�es, comme s'il s'agissait d'un seul fichier CORSSH.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class MultiFilesParser extends Parser implements Serializable {
    /**
     * Pour compatibilit�s entre diff�rentes versions de cette classe.
     */
    private static final long serialVersionUID = -6800281535750059316L;

    /**
     * Liste des fichiers CORSSH en
     * ordre chronologique de date.
     */
    private final DatedFile[] files;

    /**
     * Index de l'objet {@link Parser} courant, ou
     * <code>-1</code> si {@link #parser} est nul.
     */
    private transient int parserIndex = -1;

    /**
     * Objet {@link FileParser} en cours de lecture,
     * ou <code>null</code> s'il n'y en a pas.
     */
    private transient FileParser parser;

    /**
     * Met � jour les champs internes apr�s la lecture d'un objet {@link MultiFilesParser}.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        parserIndex = -1;
    }

    /**
     * Construit un fournisseur qui puisera les donn�es dans le r�pertoire sp�cifi�
     * ainsi que tous ses sous-r�pertoires. Seuls les fichiers avec l'extension
     * <code>.dat</code> seront pris en compte.
     *
     * @param  directory R�pertoire contenant les fichiers CORSSH � lire.
     * @throws FileNotFoundException si le r�pertoire <code>directory</code> n'a pas �t� trouv�.
     * @throws IOException si une erreur est survenue lors de la lecture d'un fichier CORSSH.
     * @throws IOException si une incoh�rence fut d�tect�e dans les dates des fichiers.
     */
    public MultiFilesParser(final File directory) throws IOException {
        this(getFiles(directory));
    }

    /**
     * Construit un fournisseur qui puisera les donn�es dans les fichiers sp�cifi�s.
     *
     * @param  list Liste des fichiers � utiliser.
     * @throws IOException si une erreur est survenue lors de la lecture d'un fichier CORSSH.
     * @throws IOException si une incoh�rence fut d�tect�e dans les dates des fichiers.
     */
    public MultiFilesParser(final File[] list) throws IOException {
        files=new DatedFile[list.length];
        for (int i=0; i<list.length; i++) {
            files[i] = new DatedFile(list[i]);
        }
        Arrays.sort(files);
        for (int i=1; i<files.length; i++) {
            if (files[i-1].endTime >= files[i].startTime) {
                throw new IOException(Resources.format(ResourceKeys.ERROR_DATES_OVERLAP_$3,
                                      files[i-1].file.getName(), files[i].file.getName(),
                                      getDateTimeInstance().format(new Date(files[i].startTime))));
            }
        }
        log("<init>", Resources.format(ResourceKeys.BUILD_INDEX_$1, new Integer(files.length)));
    }

    /**
     * Retourne la liste des fichiers avec l'extension <code>.dat</code>
     * dans le r�pertoire sp�cifi� ainsi que tous ses sous-r�pertoires.
     *
     * @param  directory R�pertoire dont on veut la liste des fichiers.
     * @return Liste des fichiers <code>.dat</code> dans le r�pertoire
     *         sp�cifi� ainsi que tous ses sous-r�pertoires.
     * @throws FileNotFoundException si le r�pertoire <code>directory</code> n'a pas �t� trouv�.
     */
    private static File[] getFiles(final File directory) throws FileNotFoundException {
        final List<File> files = new ArrayList<File>();
        getFiles(directory, files, new FileFilter() {
            public boolean accept(final File file) {
                if (file.isDirectory()) return true;
                if (!file.isFile()) return false;
                final String name=file.getName();
                final int index=name.lastIndexOf('.');
                return (index>0 && name.substring(index).equalsIgnoreCase(".dat"));
            }
        });
        return files.toArray(new File[files.size()]);
    }

    /**
     * Place dans la liste <code>files</code> le contenu du r�pertoire
     * <code>directory</code> ainsi que de tous ses sous-r�pertoires.
     *
     * @param  directory R�pertoire dont on veut la liste des fichiers.
     * @param  files Liste dans laquelle placer les fichiers .DAT trouv�s.
     * @param  filter Filtre � utiliser pour ne retenir que les fichiers .DAT.
     * @throws FileNotFoundException si le r�pertoire <code>directory</code> n'a pas �t� trouv�.
     */
    private static void getFiles(final File directory, final List<File> files, final FileFilter filter)
            throws FileNotFoundException
    {
        final File[] fileArray = directory.listFiles(filter);
        if (fileArray != null) {
            for (int i=0; i<fileArray.length; i++) {
                final File file=fileArray[i];
                if (file.isDirectory()) getFiles(file, files, filter);
                else if (file.isFile()) files.add(file);
            }
        } else {
            throw new FileNotFoundException(Resources.format(
                        ResourceKeys.ERROR_DIRECTORY_NOT_FOUND_$1, directory.getPath()));
        }
    }

    /**
     * Ferme l'objet {@link Parser} courant et ouvre
     * celui qui correspond � l'index sp�cifi�.
     *
     * @param  index Index du fichier d�sir�, ou -1 pour fermer
     *         le fichier courant sans en ouvrir un autre.
     * @throws IOException si une op�ration E/S a �chou�e.
     */
    private void setParser(final int index) throws IOException {
        if (parser != null) {
            parser.close();
            parser = null;
        }
        parserIndex= -1; // Au cas o� le bloc suivant �chouerait.
        if (index >= 0) {
            parser = new FileParser(files[index].file);
            parserIndex = index;
        }
    }

    /**
     * Positionne le flot au d�but du premier enregistrement
     * dont la date est �gale ou sup�rieure � la date sp�cifi�e.
     *
     * @param  date Date � laquelle on souhaite positionner le flot.
     * @throws IOException si une erreur est survenue lors de la lecture.
     * @throws EOFException si des enregistrements manquent dans le fichier, ou si
     *         la date demand�e est post�rieure aux dates de tous les enregistrements
     *         de tous les fichiers.
     */
    public void seek(final Date date) throws IOException {
        final long time = date.getTime();
        int index = Arrays.binarySearch(files, new DatedFile(time));
        if (index<0) {
            index = ~index;
        }
        assert index>=files.length || files[index].endTime>=time : index;
        if (index != parserIndex) {
            if (index >= files.length) {
                throw new EOFException(Resources.format(ResourceKeys.ERROR_NO_DATA_AFTER_DATE_$1, date));
            }
            setParser(index);
        }
        parser.seek(date, new Iterator<File>() {
            private int index = parserIndex-1;

            public boolean hasNext() {
                return index>=0;
            }

            public File next() {
                if (hasNext()) return files[index--].file;
                else throw new NoSuchElementException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        });
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
        if (parser!=null && parser.nextRecord()) {
            return true;
        }
        while (parserIndex+1 < files.length) {
            // 'setParser' mettre � jour la variable 'parserIndex'
            setParser(parserIndex+1);
            if (parser.nextRecord()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indique si l'enregistrement courant est blanc. Un enregistrement est consid�r� blanc
     * si tous ses champs (tels que retourn�s par {@link #getField}) ont la valeur 0. Apr�s
     * la construction d'un objet {@link Parser}, l'enregistrement courant est initialement
     * blanc.
     */
    public boolean isBlank() {
        return (parser==null || parser.isBlank());
    }

    /**
     * Retourne la valeur cod�e du champ sp�cifi�. La valeur du champ sera puis�e
     * dans l'enregistrement le lors du dernier appel � la m�thode {@link #nextRecord}.
     */
    public int getField(final int field) {
        return (parser!=null) ? parser.getField(field) : 0;
    }

    /**
     * Retourne la valeur r�elle du champ sp�cifi�. La valeur du champ sera puis�e
     * dans l'enregistrement lu lors du dernier appel � la m�thode {@link #nextRecord}.
     */
    public double getValue(final int field) {
        return (parser!=null) ? parser.getValue(field) : 0;
    }

    /**
     * Retourne la date cod�e dans l'enregistrement courant,   en nombre de secondes
     * �coul�s depuis le 1er janvier 1970. Si la date n'est pas disponible, retourne
     * {@link Long#MIN_VALUE}.
     *
     * Note: Si cette classe devait devenir public, il serait sage de supprimer cette
     *       red�finition et de se fier plut�t � l'impl�mentation par d�faut, qui est
     *       valide m�me lorsque l'utilisateur red�finie {@link #getDate}.
     */
    long getTime() {
        return (parser!=null) ? parser.getTime() : Long.MIN_VALUE;
    }

    /**
     * Retourne la date cod�e dans l'enregistrement courant.
     */
    public Date getDate() {
        return (parser!=null) ? parser.getDate() : null;
    }

    /**
     * Retourne la date du premier enregistrement dans l'ensemble des fichiers
     * disponibles. Si aucun fichier n'est disponible, retourne <code>null</code>.
     */
    public Date getStartTime() {
        return (files.length!=0) ? new Date(files[0].startTime) : null;
    }

    /**
     * Retourne la date du dernier enregistrement dans l'ensemble des fichiers
     * disponibles. Si aucun fichier n'est disponible, retourne <code>null</code>.
     */
    public Date getEndTime() {
        final int length=files.length;
        return (files.length!=0) ? new Date(files[length-1].endTime) : null;
    }

    /**
     * Retourne le nombre de passes que contient le fichier.
     */
    public int getPassCount() {
        int passCount = 0;
        for (int i=0; i<files.length; i++) {
            passCount += files[i].passCount;
        }
        return passCount;
    }

    /**
     * Retourne le nombre d'enregistrements,
     * excluant les enregistrements d'en-t�te.
     */
    public long getRecordCount() throws IOException {
        long recordCount = 0;
        for (int i=0; i<files.length; i++) {
            recordCount += files[i].recordCount;
        }
        return recordCount;
    }

    /**
     * Ferme le flot qui fournissait les donn�es.
     *
     * @throws IOException si une erreur est survenue lors de la fermeture.
     */
    public void close() throws IOException {
        setParser(-1);
    }

    /**
     * Retourne une cha�ne de caract�res repr�sentant l'enregistrement courant.
     */
    public String toString() {
        return (parser!=null) ? parser.toString() : Resources.format(ResourceKeys.BLANK);
    }

    /**
     * Nom d'un fichier CORSSH et date de son premier enregistrement.
     * Cette classe est utile pour classer des fichiers CORSSH par ordre chronologique.
     * Le crit�re de comparaison sera la date du <em>dernier</em> enregistrement.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class DatedFile implements Comparable, Serializable {
        /**
         * Pour compatibilit�s entre diff�rentes versions de cette classe.
         */
        private static final long serialVersionUID = 3834668440559363547L;

        /** Nom du fichier CORSSH.                       */ public final File file;
        /** Date du premier enregistrement du fichier.   */ public final long startTime;
        /** Date du dernier enregistrement du fichier.   */ public final long endTime;
        /** Nombre de passes que contient le fichier.    */ public final int  passCount;
        /** Nombre de d'enregistrements dans le fichier. */ public final long recordCount;

        /**
         * Construit un objet qui repr�sentera un nom de fichier CORSSH
         * avec les dates de ses premier et dernier enregistrement.
         */
        public DatedFile(final File file) throws IOException {
            final Parser parser=new FileParser(file);
            this.file        = file;
            this.startTime   = parser.getStartTime().getTime();
            this.endTime     = parser.getEndTime().getTime();
            this.passCount   = parser.getPassCount();
            this.recordCount = parser.getRecordCount();
            parser.close();
        }

        /**
         * Construit un objet qui repr�sentera une date, mais sans nom de
         * fichier. Ce constructeur sert � construire un objet qui pourra
         * �tre utilis� pour rep�rer le premier fichier CORSSH contenant
         * des donn�es � la date sp�cifi�e.
         */
        DatedFile(final long date) {
            this.file        = null;
            this.startTime   = date;
            this.endTime     = date;
            this.passCount   = 0;
            this.recordCount = 0;
        }

        /**
         * Compare les dates du dernier enregistrement
         * de deux objets <code>DatedFile</code>.
         */
        public int compareTo(final Object o) {
            final long other = ((DatedFile) o).endTime;
            if (endTime < other) return -1;
            if (endTime > other) return +1;
            return 0;
        }

        /**
         * Construit une cha�ne de caract�res repr�sentant cet objet. La cha�ne de caract�res contiendra
         * le nom du fichier ainsi que les dates des premiers et derniers enregistrements trouv�s dans
         * le fichier.
         */
        final StringBuffer toString(StringBuffer b, final DateFormat f, final Date d, final FieldPosition p) {
            int i;
            i=b.length(); b.append(file.getName());                b.append(   Utilities.spaces(Math.max(15-(b.length()-i), 1)));
            i=b.length(); d.setTime(startTime); b=f.format(d,b,p); b.insert(i, Utilities.spaces(Math.max(22-(b.length()-i), 1)));
            i=b.length(); d.setTime(  endTime); b=f.format(d,b,p); b.insert(i, Utilities.spaces(Math.max(22-(b.length()-i), 1)));
            i=b.length(); b.append(passCount);                     b.insert(i, Utilities.spaces(Math.max( 5-(b.length()-i), 1)));
            i=b.length(); b.append(recordCount);                   b.insert(i, Utilities.spaces(Math.max( 8-(b.length()-i), 1)));
            return b;
        }

        /**
         * Retourne une cha�ne de caract�res repr�sentant cet objet. La cha�ne de caract�res contiendra
         * le nom du fichier ainsi que les dates des premiers et derniers enregistrements trouv�s dans
         * le fichier.
         */
        public String toString() {
            return toString(new StringBuffer(), getDateTimeInstance(),
                            new Date(0), new FieldPosition(0)).toString();
        }
    }

    /**
     * Retourne une cha�ne de caract�res contenant la liste de tous les fichiers
     * lus ainsi que les dates de leurs premiers et derniers �chantillons. Ces
     * informations seront dispos�es dans un tableau de trois colonnes, chaque
     * colonne �tant s�par�s par des espaces. Les fichiers appara�tront dans
     * l'ordre chronologique de la date de leur dernier enregistrement.
     */
    public String list() {
        StringBuffer     buffer = new StringBuffer();
        final DateFormat format = getDateTimeInstance();
        final FieldPosition pos = new FieldPosition(0);
        final Date       date   = new Date(0);
        for (int i=0; i<files.length; i++) {
            buffer = files[i].toString(buffer, format, date, pos);
            buffer.append('\n');
        }
        return buffer.toString();
    }

    /**
     * Affiche la liste de tous les fichiers apparaissant dans le r�pertoire sp�cifi�e.
     * Chaque nom de fichier sera accompagn� des dates de ses premiers et derniers
     * enregistrements. Ces informations seront dispos�es dans un tableau de trois
     * colonnes, chaque colonne �tant s�par�es par des espaces. Les fichiers appara�tront
     * dans l'ordre chronologique de la date de leur dernier enregistrement.
     * <br><br>
     * De fa�on facultative, on peut sp�cifier une date de d�part. Les 10 premiers
     * enregistrements � partir de cette date seront alors affich�s.
     */
    public static void main(final String[] args) throws Exception {
        int n=10;
        Date start=null;
        switch (args.length) {
            default: System.out.println("Usage: MultiFilesParser [directory] [date] [max]"); break;
            case 3:  n=Integer.parseInt(args[2]);                // fallthrough
            case 2:  start=getDateTimeInstance().parse(args[1]); // fallthrough
            case 1: {
                final MultiFilesParser parser=new MultiFilesParser(new File(args[0]));
                System.out.println(parser.list());
                main(parser, start, n);
                break;
            }
        }
    }
}
