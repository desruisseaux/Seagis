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
package fr.ird.database.coverage;

// Entr�s/sorties
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;

// Collections
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Collections;

// Tables et �venements
import java.awt.EventQueue;
import java.sql.SQLException;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;
import javax.swing.table.TableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.lang.reflect.InvocationTargetException;

// Presse-papier
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;

// Interface utilisateur
import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;

// G�om�trie et coordonn�es
import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

// Divers
import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import javax.media.jai.util.Range;

// Geotools
import org.geotools.cs.AxisInfo;
import org.geotools.gc.GridCoverage;
import org.geotools.cs.CoordinateSystem;

// Seagis
import fr.ird.resources.XArray;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Mod�le servant � l'affichage d'informations sur des images.  Ce mod�le fait
 * le pont entre une table {@link CoverageTable} et l'afficheur {@link JTable}
 * de <i>Swing</i>. Les donn�es d'une table d'images peuvent �tre affich�es comme
 * suit:
 *
 * <blockquote><pre>
 * final {@link CoverageTable} table = ...;
 * final {@link TableModel}    model = new CoverageTableModel(table);
 * final {@link JTable}         view = new JTable(model);
 * </pre></blockquote>
 *
 * Les cellules de la table peuvent �tre affich�es de diff�rentes couleurs. Par
 * exemple les images qui ont �t� vues peuvent �tre �crites en bleu, tandis que
 * les images manquantes peuvent �tre �crites en rouge. Cet affichage color� en
 * fonction des images peut �tre activ� avec le code suivant:
 *
 * <blockquote><pre>
 * {@link TableCellRenderer} renderer=new {@link CellRenderer}();
 * view.setDefaultRenderer({@link String}.class, renderer);
 * view.setDefaultRenderer(  {@link Date}.class, renderer);
 * </pre></blockquote>
 *
 * La classe <code>CoverageTableModel</code> garde une trace des images qui
 * sont ajout�es ou retir�es de la table. Ces op�rations peuvent �tre annul�es.
 * Les fonctions "annuler" et "refaire" peuvent �tre activ�es avec le code suivant:
 *
 * <blockquote><pre>
 * final {@link UndoManager} undoManager=new UndoManager();
 * ((CoverageTableModel) model).addUndoableEditListener(undoManager);
 * </pre></blockquote>
 *
 * On peut ensuite utiliser les m�thodes {@link UndoManager#undo} et
 * {@link UndoManager#redo} pour d�faire ou refaire une op�ration.
 * <br>
 * La plupart des m�thodes de cette classe peuvent �tre appel�e de n'importe
 * quel thread (pas n�cessairement celui de <i>Swing</i>).  Si l'appel d'une
 * m�thode a chang�e le contenu de la table, <i>Swing</i> en sera inform� dans
 * son propre thread m�me si les m�thodes ont �t� appel�es d'un autre thread.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class CoverageTableModel extends AbstractTableModel {
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 6723633134014245147L;

    /**
     * Indique s'il faut inverser l'ordre des enregistrements.
     */
    private static final boolean REVERSE_ORDER = true;

    /** Num�ro de colonne des noms de fichiers.   */ private static final int NAME     = 0;
    /** Num�ro de colonne des dates des images.   */ private static final int DATE     = 1;
    /** Num�ro de colonne de la dur�e des images. */ private static final int DURATION = 2;

    /**
     * Liste des titres des colonnes.
     */
    private final String[] titles = new String[] {
        /*[0]*/ Resources.format(ResourceKeys.NAME),
        /*[1]*/ Resources.format(ResourceKeys.END_TIME),
        /*[2]*/ Resources.format(ResourceKeys.DURATION)
    };

    /**
     * Liste des classes des valeurs des colonnes.
     */
    private static final Class[] CLASS = new Class[] {
        /*[0]:Name*/ String.class,
        /*[1]:Date*/   Date.class,
        /*[2]:Time*/ String.class
    };

    /**
     * S�rie d'images repr�sent� par cette table.
     */
    private SeriesEntry series;

    /**
     * Liste des entr�es contenues dans cette table. La longueur
     * de ce tableau est le nombre de lignes dans la table.
     */
    private CoverageEntry[] entries;

    /**
     * Objet � utiliser pour formatter les dates des images.
     */
    private final DateFormat dateFormat;

    /**
     * Objet � utiliser pour formatter les dur�es des images.
     */
    private final DateFormat timeFormat;

    /**
     * Objet � utiliser pour formatter les nombres.
     */
    private final NumberFormat numberFormat;

    /**
     * Objet � utiliser pour obtenir la position d'un champ formatt�.
     */
    private transient FieldPosition fieldPosition;

    /**
     * Buffer dans lequel formater les champs.
     */
    private transient StringBuffer buffer;

    /**
     * Mot "jour" dans la langue de l'utilisateur.
     */
    private static final String DAY=Resources.format(ResourceKeys.DAY);

    /**
     * Mot "jours" dans la langue de l'utilisateur.
     */
    private static final String DAYS=Resources.format(ResourceKeys.DAYS);

    /**
     * Construit une table initialement vide. Des images pourront
     * �tre ajout�es plus tard en appelant {@link #setEntries}.
     *
     * @param series S�ries que repr�sentera cette table, ou
     *        <code>null</code> si elle n'est pas connue.
     */
    public CoverageTableModel(final SeriesEntry series) {
        this.series  = series;
        numberFormat = NumberFormat.getNumberInstance();
        dateFormat   = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        timeFormat   = new SimpleDateFormat("HH:mm");
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Construit une table qui contiendra une copie du contenu de la table sp�cifi�e.
     * La nouvelle table ne contiendra initialement aucun <code>Listener</code> (c'est
     * � dire que les <code>Listener</code> de la table sp�cifi�e ne seront pas copi�s).
     *
     * @param table Table dont on veut copier le contenu.
     */
    public CoverageTableModel(final CoverageTableModel table) {
        synchronized (table) {
            series       =                   table.series;
            numberFormat =    (NumberFormat) table.numberFormat.clone();
            dateFormat   =      (DateFormat) table.  dateFormat.clone();
            timeFormat   =      (DateFormat) table.  timeFormat.clone();
            entries      = (CoverageEntry[]) table.     entries.clone();

            final CoverageEntry[] entries = this.entries;
            for (int i=entries.length; --i>=0;) {
                if (entries[i] instanceof ProxyEntry) {
                    final ProxyEntry oldProxy = (ProxyEntry) entries[i];
                    final ProxyEntry newProxy = new ProxyEntry(unwrap(oldProxy.getEntry()));
                    newProxy.flags = oldProxy.flags;
                    entries[i] = newProxy;
                }
            }
        }
    }

    /**
     * Construit une table qui contiendra la liste des images de <code>table</code>.
     * Ce constructeur m�morise la s�rie de <code>table</code>, telle que retourn�e
     * par {@link CoverageTable#getSeries}.
     *
     * @param  table Table dans laquelle puiser la liste des images.
     * @throws SQLException si l'interrogation de la table a �chou�e.
     */
    public CoverageTableModel(final CoverageTable table) throws SQLException {
        this(table.getSeries());
        final List<CoverageEntry> entryList = table.getEntries();
        entries = entryList.toArray(new CoverageEntry[entryList.size()]);
        if (REVERSE_ORDER) {
            reverse(entries);
        }
    }

    /**
     * Renverse l'ordre des �l�ments du tableau sp�cifi�.
     */
    private static void reverse(final CoverageEntry[] entries) {
        for (int i=entries.length/2; --i>=0;) {
            final int j = entries.length-1-i;
            final CoverageEntry tmp = entries[i];
            entries[i] = entries[j];
            entries[j] = tmp;
        }
    }

    /**
     * Remplace tous les enregistrements courants par ceux de la table <code>table</code>.
     * La s�rie de <code>table</code> (telle que retourn�e par {@link CoverageTable#getSeries})
     * deviendra la s�rie courante, celle que retourne {@link #getSeries}. Cette m�thode
     * peut �tre appel�e de n'importe quel thread (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param  table Table dans laquelle puiser la liste des images.
     * @throws SQLException si l'interrogation de la table a �chou�e.
     */
    public void setEntries(final CoverageTable table) throws SQLException {
        final List<CoverageEntry> entryList;
        synchronized (table) {
            entryList = table.getEntries();
            series    = table.getSeries();
        }
        setEntries(entryList);
    }

    /**
     * Remplace tous les enregistrements courants par les enregistrements
     * sp�cifi�s. Cette m�thode peut �tre appel�e de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param entryList Liste des nouvelles entr�es.
     */
    public synchronized void setEntries(final List<CoverageEntry> entryList) {
        final CoverageEntry[] newEntries = entryList.toArray(new CoverageEntry[entryList.size()]);
        if (REVERSE_ORDER) {
            reverse(newEntries);
        }
        final CoverageEntry[] oldEntries = entries;
        this.entries = newEntries;
        final Map<CoverageEntry,ProxyEntry> proxies = getProxies(oldEntries, null);
        if (proxies != null) {
            for (int i=newEntries.length; --i>=0;) {
                final ProxyEntry proxy = proxies.get(newEntries[i]);
                if (proxy != null) {
                    newEntries[i] = proxy;
                }
            }
        }
        if (EventQueue.isDispatchThread()) {
            fireTableDataChanged();
            commitEdit(oldEntries, newEntries, ResourceKeys.DEFINE);
        } else EventQueue.invokeLater(new Runnable() {
            public void run() {
                fireTableDataChanged();
                commitEdit(oldEntries, newEntries, ResourceKeys.DEFINE);
            }
        });
    }

    /**
     * Retourne l'ensemble des objets {@link ProxyEntry} qui se trouvent dans
     * le tableau sp�cifi�. Les valeurs seront les objets {@link ProxyEntry},
     * tandis que les cl�s seront les objets {@link CoverageEntry} qu'ils
     * enveloppent. Cette m�thode retourne <code>null</code> si aucun objets
     * {@link ProxyEntry} n'a �t� trouv�.
     *
     * @param  entries Entr�es dans lequel v�rifier s'il y a des {@link ProxyEntry}.
     * @param  proxies Dictionnaire dans lequel ajouter les {@link ProxyEntry} trouv�s,
     *         ou <code>null</code> si aucun dictionnaire n'a encore �t� cr��.
     * @return L'argument <code>proxies</code>, ou un nouvel objet {@link Map}
     *         si <code>proxies</code> �tait nul.
     */
    private static Map<CoverageEntry,ProxyEntry> getProxies(final CoverageEntry[] entries,
                                                    Map<CoverageEntry,ProxyEntry> proxies)
    {
        if (entries != null) {
            for (int i=entries.length; --i>=0;) {
                final CoverageEntry entry = entries[i];
                if (entry instanceof ProxyEntry) {
                    if (proxies == null) {
                        proxies = new HashMap<CoverageEntry,ProxyEntry>();
                    }
                    final ProxyEntry proxy = (ProxyEntry) entry;
                    proxies.put(proxy.getEntry(), proxy);
                }
            }
        }
        return proxies;
    }

    /**
     * Si <code>entry</code> est de la classe {@link ProxyEntry},
     * retourne l'objet {@link CoverageEntry} qu'il enveloppait.
     */
    private static CoverageEntry unwrap(CoverageEntry entry) {
        while (entry instanceof ProxyEntry) {
            entry = ((ProxyEntry) entry).getEntry();
        }
        return entry;
    }

    /**
     * Retourne les entr�es de toutes les images pr�sentes dans la table. Les
     * op�rations de lectures effectu�es sur les entr�es retourn�es ne seront
     * pas indiqu�es dans la table <code>this</code> (contrairement aux entr�es
     * retourn�es par {@link #getEntryAt}, qui �crive en bleu les images lues).
     *
     * @return Les entr�es de toutes les images de cette table. Ce tableau peut
     *         avoir une longueur de 0, mais ne sera jamais <code>null</code>.
     * @throws SQLException si une interrogation de la base de donn�es �tait
     *         n�cessaire et a �chou�.
     */
    public synchronized CoverageEntry[] getEntries() throws SQLException {
        final CoverageEntry[] entries = this.entries;
        final CoverageEntry[] out = new CoverageEntry[(entries!=null) ? entries.length : 0];
        for (int i=out.length; --i>=0;) {
            out[i] = unwrap(entries[i]);
        }
        return out;
    }

    /**
     * Retourne l'entr� de l'image qui se trouve � la ligne sp�cifi�e.
     * Pour �conomiser la m�moire, il est recommand� de ne pas retenir
     * cette r�f�rence plus longtemps que la dur�e de vie de cette table
     * <code>CoverageTableModel</code>.
     *
     * @param  row Index de l'entr� d�sir�.
     */
    public synchronized CoverageEntry getEntryAt(final int row) {
        CoverageEntry entry = entries[row];
        if (!(entry instanceof ProxyEntry)) {
            entries[row] = entry = new ProxyEntry(entry);
        }
        return entry;
    }

    /**
     * Retourne les num�ros d'identification des images pr�sentes dans cette
     * table. Les num�ros d'identification sont les num�ros retourn�s par
     * {@link CoverageEntry#getID}. Cette m�thode peut retourner un tableau
     * de longueur 0, mais ne retourne jamais <code>null</code>.
     */
    public synchronized int[] getEntryIDs() {
        final CoverageEntry[] entries = this.entries;
        final int[] IDs = new int[(entries!=null) ? entries.length : 0];
        for (int i=0; i<IDs.length; i++) {
            IDs[i] = entries[i].getID();
        }
        return IDs;
    }

    /**
     * Retourne les num�ros d'identification des images aux lignes
     * sp�cifi�es.   Les num�ros d'identification sont les num�ros
     * retourn�s par {@link CoverageEntry#getID}. Cette m�thode
     * peut retourner un tableau de longueur 0, mais ne retourne jamais
     * <code>null</code>.
     */
    public synchronized int[] getEntryIDs(int[] rows) {
        final CoverageEntry[] entries = this.entries;
        final int[] IDs = new int[rows.length];
        for (int i=0; i<IDs.length; i++) {
            IDs[i] = entries[rows[i]].getID();
        }
        return IDs;
    }

    /**
     * Retourne les num�ros de lignes qui correspondent aux images sp�cifi�es.
     * Les images sont d�sign�es par leurs num�ros ID (le num�ro retourn� par
     * {@link CoverageEntry#getID}). Cette m�thode est l'inverse de
     * {@link #getEntryIDs}.
     *
     * @param  IDs Num�ros d'identifications des images.
     * @return Num�ro de lignes des images demand�es. Ce tableau aura toujours
     *         la m�me longueur que <code>IDs</code>. Les images qui n'ont pas
     *         �t� trouv�es dans la table auront l'index -1.
     */
    public synchronized int[] indexOf(final int[] IDs) {
        final CoverageEntry[] entries = this.entries;
        final int[] sortedIDs  = (int[]) IDs.clone();
        final int[] sortedRows = new int[IDs.length];
        Arrays.sort(sortedIDs);
        Arrays.fill(sortedRows, -1);
        for (int i=entries.length; --i>=0;) {
            final int index = Arrays.binarySearch(sortedIDs, entries[i].getID());
            if (index >= 0) {
                sortedRows[index]=i;
            }
        }
        /*
         * Replace les num�ros de ligne dans l'ordre qui correspond � 'IDs'.
         * L'algorithme ci-dessous n'est pas le plus efficace (il aurait �t�
         * plus rapide d'utiliser encore 'binarySearch(sortedIDs...)',  mais
         * il a l'avantage de fonctionner correctement s'il y a des doublons
         * dans 'IDs'.
         */
        final int[] rows = new int[sortedRows.length];
        Arrays.fill(rows, -1);
        for (int i=sortedIDs.length; --i>=0;) {
            final int ID  = sortedIDs [i];
            final int row = sortedRows[i];
            if (row >= 0) {
                for (int j=IDs.length; --j>=0;) {
                    if (IDs[j]==ID) rows[j]=row;
                }
            }
        }
        return rows;
    }

    /**
     * Retire une entr�e de cette table. Si <code>toRemove</code> est
     * nul ou n'appara�t pas dans la table, alors il sera ignor�.
     */
    public synchronized void remove(final CoverageEntry toRemove) {
        final Set<CoverageEntry> singleton = new HashSet<CoverageEntry>();
        singleton.add(toRemove);
        remove(singleton);

//  TODO: the following line should have been enough,
//        if the Collections class was generic...
//      remove(Collections.singleton(unwrap(toRemove)));
    }

    /**
     * Retire l'entr�e qui se trouve � l'index sp�cifi�. L'index
     * <code>row</code> correspond au num�ro (� partir de 0)  de
     * la ligne � supprimer.
     */
    public synchronized void remove(final int row) {
        remove(entries[row]);
    }

    /**
     * Retire plusieurs entr�es de cette table. Les entr�es
     * nulles ainsi que celles qui n'apparaissent pas dans
     * cette table seront ignor�es.
     */
    public synchronized void remove(final CoverageEntry[] toRemove) {
        final Set<CoverageEntry> toRemoveSet;
        toRemoveSet = new HashSet<CoverageEntry>(Math.max(2*toRemove.length, 11));
        for (int i=0; i<toRemove.length; i++) {
            toRemoveSet.add(unwrap(toRemove[i]));
        }
        remove(toRemoveSet);
    }

    /**
     * Retire plusieurs entr�es d�sign�s par les index des lignes.  Les index
     * <code>rows</code> correspondent aux num�ros (� partir de 0) des lignes
     * � supprimer. Ces num�ros de lignes peuvent �tre dans n'importe quel
     * ordre.
     */
    public synchronized void remove(final int[] rows) {
        final Set<CoverageEntry> toRemoveSet;
        toRemoveSet = new HashSet<CoverageEntry>(Math.max(2*rows.length, 11));
        for (int i=0; i<rows.length; i++) {
            toRemoveSet.add(unwrap(entries[rows[i]]));
        }
        remove(toRemoveSet);
    }

    /**
     * Retire plusieurs entr�es de cette table. Les entr�es
     * nulles ainsi que celles qui n'apparaissent pas dans
     * cette table seront ignor�es. Cette m�thode peut �tre
     * appel�e de n'importe quel thread (pas n�cessairement
     * celui de <i>Swing</i>).
     */
    private synchronized void remove(final Set<CoverageEntry> toRemove) {
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    remove(toRemove);
                }
            });
            return;
        }
        final CoverageEntry[] oldEntries = entries;
        CoverageEntry[] entries = oldEntries;
        int entriesLength = entries.length;
        int upper = entriesLength;
        for (int i=upper; --i>=-1;) {
            if (i<0 || !toRemove.contains(unwrap(entries[i]))) {
                final int lower=i+1;
                if (upper != lower) {
                    if (entries == oldEntries) {
                        // Cr�� une copie, de fa�on � ne pas modifier le tableau 'entries' original.
                        entries = XArray.remove(entries, lower, upper-lower);
                    } else {
                        // Si le tableau est d�j� une copie, travaille directement sur lui.
                        System.arraycopy(entries, upper, entries, lower, entriesLength-upper);
                    }
                    entriesLength -= (upper-lower);
                    fireTableRowsDeleted(lower, upper-1);
                }
                upper=i;
            }
        }
        this.entries = XArray.resize(entries, entriesLength);
        commitEdit(oldEntries, this.entries, ResourceKeys.DELETE);
    }

    /**
     * Copie les donn�es de certaines lignes dans un objet transf�rable. Cet
     * objet pourra �tre plac� dans le presse papier pour �tre ensuite coll�
     * dans un tableur commercial par exemple. Le presse-papier du syst�me
     * peut �tre obtenu par un appel �:
     *
     * <blockquote><pre>
     * Toolkit.getDefaultToolkit().getSystemClipboard()
     * </pre></blockquote>
     *
     * @param  rows Ligne � copier.
     * @return Objet transf�rable contenant les lignes copi�es.
     */
    public synchronized Transferable copy(final int[] rows) {
        if (fieldPosition == null) {
            fieldPosition = new FieldPosition(0);
        }
        final StringBuffer buffer = new StringBuffer(256); // On n'utilise pas le buffer des cellules.
        final int[] cl�s = new int[] {
            ResourceKeys.NAME,
            ResourceKeys.START_TIME,
            ResourceKeys.END_TIME
        };
        for (int i=0; i<cl�s.length;) {
            buffer.append(Resources.format(cl�s[i++]));
            buffer.append((i<cl�s.length) ? '\t' : '\n');
        }
        for (int i=0; i<rows.length; i++) {
            Date date;
            final CoverageEntry entry = unwrap(entries[rows[i]]);
            final Range timeRange = entry.getTimeRange();
            buffer.append(entry.getName());
            buffer.append('\t');
            if ((date=(Date)timeRange.getMinValue()) != null) {
                dateFormat.format(date, buffer, fieldPosition);
            }
            buffer.append('\t');
            if ((date=(Date)timeRange.getMaxValue()) != null) {
                dateFormat.format(date, buffer, fieldPosition);
            }
            buffer.append('\n');
            // Note: on devrait utiliser System.getProperty("line.separator", "\n"),
            //       mais �a donne un r�sultat bizarre quand on colle dans Excel. Il
            //       met une ligne vierge entre chaque ligne de donn�es.
        }
        return new StringSelection(buffer.toString());
        // TODO: Dans une version future, on pourra supporter une plus
        //       grande gamme de types: 'javaSerializedObjectMimeType',
        //       'javaJVMLocalObjectMimeType', etc...
    }

    /**
     * Retourne le nombre de lignes de ce tableau.
     */
    public int getRowCount() {
        return entries.length;
    }

    /**
     * Retourne le nombre de colonnes de ce tableau.
     */
    public int getColumnCount() {
        return titles.length;
    }

    /**
     * Retourne le nom de la colonne sp�cifi�e.
     */
    public String getColumnName(final int column) {
        return titles[column];
    }

    /**
     * Retourne la classe des objets de la colonne sp�cifi�e.
     */
    public Class getColumnClass(final int column) {
        return CLASS[column];
    }

    /**
     * Retourne la valeur de la cellule aux index sp�cifi�s.
     *
     * @param  row    Num�ro de ligne de la cellule, � partir de 0.
     * @param  column Num�ro de colonne de la cellule, � partir de 0.
     * @return Valeur de la cellule aux index sp�cifi�s.
     */
    public synchronized Object getValueAt(final int row, final int column) {
        CoverageEntry entry = entries[row];
        if (!(entry instanceof ProxyEntry)) {
            entries[row] = entry = new ProxyEntry(entry);
        }
        switch (column) {
            default:   return null;
            case NAME: return entry.getName();
            case DATE: return entry.getTimeRange().getMaxValue();
            case DURATION: {
                if (buffer        == null) buffer        = new StringBuffer ( );
                if (fieldPosition == null) fieldPosition = new FieldPosition(0);
                buffer.setLength(0);
                final Range range = entry.getTimeRange();
                final Date time   = (Date) range.getMaxValue();
                final Date start  = (Date) range.getMinValue();
                if (time!=null && start!=null) {
                    final long millis = time.getTime()-start.getTime();
                    final long days   = millis/(24L*60*60*1000);
                    time.setTime(millis);
                    numberFormat.format(days, buffer, fieldPosition);
                    buffer.append(' ');
                    buffer.append((days>1) ? DAYS : DAY);
                    buffer.append(' ');
                    timeFormat.format(time, buffer, fieldPosition);
                }
                return buffer.toString();
            }
        }
    }

    /**
     * Convertit une date en cha�ne de caract�res.
     */
    private String format(final Date date) {
        if (buffer        == null) buffer        = new StringBuffer ( );
        if (fieldPosition == null) fieldPosition = new FieldPosition(0);
        buffer.setLength(0);
        dateFormat.format(date, buffer, fieldPosition);
        return buffer.toString();
    }

    /**
     * Retourne la s�rie d'images repr�sent�e par cette table. Si la s�rie n'est
     * pas connue ou si cette table contient des images de plusieurs s�ries
     * diff�rentes, alors cette m�thode peut retourner <code>null</code>.
     */
    public SeriesEntry getSeries() {
        return series;
    }

    /**
     * Retourne le fuseau horaire utilis� pour les �critures de dates.
     */
    public synchronized TimeZone getTimeZone() {
        return dateFormat.getTimeZone();
    }

    /**
     * D�finit le fuseau horaire � utiliser pour l'�criture des dates.
     */
    public synchronized void setTimeZone(final TimeZone timezone) {
        dateFormat.setTimeZone(timezone);
        if (entries.length != 0) {
            fireTableChanged(new TableModelEvent(this, 0, entries.length-1, DATE));
        }
    }

    /**
     * Ajoute un objet � la liste des objets int�ress�s � �tre
     * inform�s chaque fois qu'une �dition anulable a �t� faite.
     */
    public void addUndoableEditListener(final UndoableEditListener listener) {
        listenerList.add(UndoableEditListener.class, listener);
    }

    /**
     * Retire un objet de la liste des objets int�ress�s � �tre
     * inform�s chaque fois qu'une �dition anulable a �t� faite.
     */
    public void removeUndoableEditListener(final UndoableEditListener listener) {
        listenerList.remove(UndoableEditListener.class, listener);
    }

    /**
     * Prend en compte des changements qui viennent d'�tre apport�es � la table.
     * Cette m�thode mettra � jour la variable {@link #backup} et pr�viendra tous
     * les objets qui �taient int�ress�s � �tre inform�s des changements anulables.
     */
    private void commitEdit(final CoverageEntry[] oldEntries,
                            final CoverageEntry[] newEntries,
                            final int cl�) // NO synchronized!
    {
        final String name = Resources.format(cl�).toLowerCase();
        if (oldEntries != newEntries) {
            final Object[] listeners=listenerList.getListenerList();
            if (listeners.length != 0) {
                UndoableEditEvent event = null;
                for (int i=listeners.length; (i-=2)>=0;) {
                    if (listeners[i]==UndoableEditListener.class) {
                        if (event==null) event=new UndoableEditEvent(this, new AbstractUndoableEdit() {
                            public void undo() throws CannotUndoException {super.undo(); entries=oldEntries; fireTableDataChanged();}
                            public void redo() throws CannotRedoException {super.redo(); entries=newEntries; fireTableDataChanged();}
                            public String getPresentationName() {return name;}
                        });
                        ((UndoableEditListener) listeners[i+1]).undoableEditHappened(event);
                    }
                }
            }
        }
    }

    /**
     * Indique que l'enregistrement {@link #entry} a chang�.  Cette m�thode
     * recherche la ligne correspondant � cette entr�e et lance l'�v�nement
     * appropri�e. Cette m�thode peut �tre appel�e � partir de n'importe quel
     * thread (pas n�cessairement celui de <i>Swing</i>).
     */
    private void fireTableRowsUpdated(CoverageEntry entry) { // NO synchronized
        entry = unwrap(entry);
        final CoverageEntry[] entries = this.entries;
        for (int i=entries.length; --i>=0;) {
            if (entry.equals(unwrap(entries[i]))) {
                final int row = i;
                if (EventQueue.isDispatchThread()) {
                    fireTableRowsUpdated(row, row);
                } else {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            fireTableRowsUpdated(row, row);
                        }
                    });
                }
            }
        }
    }

    /**
     * Classe des entr�es des images.  Cette classe redirige la plupart des appels de ses m�thodes
     * vers un autre objet {@link CoverageEntry}. La principale exception est la m�thode
     * {@link #getGridCoverage}, qui intercepte les appels pour mettre � jour des variables
     * internes indiquant si une image a �t� vue ou si sa lecture a �chou�e.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class ProxyEntry extends CoverageEntry.Proxy {
        /**
         * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
         */
        private static final long serialVersionUID = 8398851451224196337L;

        /** Drapeau indiquant qu'une image a �t� vue.        */ public static final byte VIEWED    = 1;
        /** Drapeau indiquant qu'un fichier est introuvable. */ public static final byte MISSING   = 2;
        /** Drapeau indiquant qu'un fichier est mauvais.     */ public static final byte CORRUPTED = 4;
        /** Drapeau indiquant l'�tat de l'image courante.    */ public              byte flags;

        /**
         * Construit un proxy.
         */
        public ProxyEntry(final CoverageEntry entry) {
            super(entry);
            FileChecker.add(this);
        }

        /**
         * Retourne l'entr� envelopp�e par ce proxy.
         */
        final CoverageEntry getEntry() {
            return entry;
        }

        /**
         * Proc�de � la lecture d'une image. Si la lecture a r�ussi sans avoir �t�
         * annul�e par l'utilisateur, alors le drapeau {@link #VIEWED} sera lev�.
         * Si la lecture a �chou�, alors le drapeau {@link #CORRUPTED} sera lev�.
         */
        public GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException {
            try {
                final GridCoverage image=entry.getGridCoverage(listenerList);
                if (image!=null) setFlag(VIEWED, true);
                setFlag((byte)(MISSING|CORRUPTED), false);
                return image;
            } catch (FileNotFoundException exception) {
                setFlag(MISSING, true);
                throw exception;
            } catch (IOException exception) {
                setFlag(CORRUPTED, true);
                throw exception;
            }
        }

        /**
         * Place ou retire les drapeaux sp�cifi�s. Si l'appel de cette m�thode a modifi�
         * l'�tat des drapeaux, alors {@link #fireTableRowsUpdated} sera appel�e.
         */
        public synchronized void setFlag(byte f, final boolean set) {
            if (set) f |= flags;
            else     f  = (byte) (flags & ~f);
            if (flags != f) {
                flags = f;
                fireTableRowsUpdated(entry);
            }
        }
    }

    /**
     * Classe du thread qui aura la charge de v�rifier si les fichiers des images existent.
     * Lorsqu'un nouvel objet {@link ProxyEntry} est cr��, il peut appeler la m�thode statique
     * {@link #add} pour s'ajouter lui-m�me � la liste des images dont on v�rifiera l'existence.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final static class FileChecker extends Thread {
        /**
         * Thread ayant la charge de v�rifier si des fichiers existent.
         */
        private static FileChecker thread;

        /**
         * Liste des fichiers dont on veut v�rifier l'existence.
         */
        private final LinkedList<ProxyEntry> list = new LinkedList<ProxyEntry>();

        /**
         * Construit un thread qui v�rifiera l'existence des fichiers. Le processus d�marrera
         * imm�diatement, mais bloquera presque aussit�t sur la m�thode {@link #next}  (parce
         * qu'elle est synchronis�e sur le m�me moniteur que {@link #add}, la m�thode qui
         * appelle ce constructeur). L'ex�cution continuera lorsque la m�thode {@link #add}
         * aura termin�, ce qui garantit qu'il y aura au moins une image � v�rifier.
         */
        private FileChecker() {
            super("FileChecker");
            setPriority(MIN_PRIORITY);
            setDaemon(true);
            start();
        }

        /**
         * Ajoute une entr�e � la liste des images � v�rifier.
         */
        public static synchronized void add(final ProxyEntry entry) {
            if (thread == null) {
                thread = new FileChecker();
            }
            thread.list.add(entry);
        }

        /**
         * Retourne la prochaine image � v�rifier, ou <code>null</code>
         * s'il n'en reste plus. S'il ne reste plus d'images, alors cette
         * m�thode signalera que le thread va mourrir en donnant la valeur
         * <code>null</code> � {@link #thread].
         */
        private static synchronized ProxyEntry next(final LinkedList<ProxyEntry> list) {
            if (list.isEmpty()) {
                thread = null;
                return null;
            }
            return list.removeFirst();
        }

        /**
         * V�rifie si les fichiers de la liste existent.   Si un fichier
         * n'existe pas, le drapeau {@link ProxyEntry#MISSING} sera l�v�
         * pour l'objet {@link ProxyEntry} correspondant.
         */
        public void run() {
            ProxyEntry entry;
            while ((entry=next(list)) != null) {
                final File file=entry.getFile();
                if (file != null) {
                    entry.setFlag(ProxyEntry.MISSING, !file.isFile());
                }
            }
        }
    }

    /**
     * Classe pour afficher des cellules de {@link CoverageTableModel} dans une table
     * {@link JTable}. Par d�faut, cette classe affiche le texte des cellules avec
     * leur couleur habituelle (noir). Elle peut toutefois utiliser des couleurs
     * diff�rentes si l'image a �t� vue (bleu) ou si elle est manquante (rouge).
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static class CellRenderer extends DefaultTableCellRenderer {
        /**
         * Couleur par d�faut de la police.
         */
        private Color foreground;

        /**
         * Couleur par d�faut de l'arri�re plan.
         */
        private Color background;

        /**
         * Construit un objet <code>CellRenderer</code>.
         */
        public CellRenderer() {
            super();
            foreground = super.getForeground();
            background = super.getBackground();
        }

        /**
         * D�finit la couleur de la police.
         */
        public void setForeground(final Color foreground) {
            super.setForeground(this.foreground=foreground);
        }

        /**
         * D�finit la couleur de l'arri�re-plan.
         */
        public void setBackground(final Color background) {
            super.setBackground(this.background=background);
        }

        /**
         * Retourne une composante � utiliser pour dessiner le contenu des
         * cellules de la table.  Cette m�thode utilise une composante par
         * d�faut, mais en changeant la couleur du texte si l'entr�e correspond
         * � une image qui a d�j� �t� lue.
         */
        public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected,
                                                       final boolean hasFocus, final int row, final int column)
        {
            Color foreground = this.foreground;
            Color background = this.background;
            if (row >= 0) {
                final TableModel model=table.getModel();
                if (model instanceof CoverageTableModel) {
                    final CoverageTableModel imageTable = (CoverageTableModel) model;
                    if (value instanceof Date) {
                        value = imageTable.format((Date) value);
                    }
                    final CoverageEntry entry = imageTable.entries[row];
                    if (entry instanceof ProxyEntry) {
                        final byte flags = ((ProxyEntry) entry).flags;
                        if ((flags & ProxyEntry.VIEWED   ) != 0) {foreground=Color.blue ;                      }
                        if ((flags & ProxyEntry.MISSING  ) != 0) {foreground=Color.red  ;                      }
                        if ((flags & ProxyEntry.CORRUPTED) != 0) {foreground=Color.white; background=Color.red;}
                    }
                }
            }
            super.setBackground(background);
            super.setForeground(foreground);
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
