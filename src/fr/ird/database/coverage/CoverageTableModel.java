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
 */
package fr.ird.database.coverage;

// Entrés/sorties
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.event.IIOReadProgressListener;
import java.rmi.server.RemoteStub;
import java.rmi.RemoteException;

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

// Tables et évenements
import java.awt.EventQueue;
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

// Géométrie et coordonnées
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
 * Modčle servant ŕ l'affichage d'informations sur des images.  Ce modčle fait
 * le pont entre une table {@link CoverageTable} et l'afficheur {@link JTable}
 * de <i>Swing</i>. Les données d'une table d'images peuvent ętre affichées comme
 * suit:
 *
 * <blockquote><pre>
 * final {@link CoverageTable} table = ...;
 * final {@link TableModel}    model = new CoverageTableModel(table);
 * final {@link JTable}         view = new JTable(model);
 * </pre></blockquote>
 *
 * Les cellules de la table peuvent ętre affichées de différentes couleurs. Par
 * exemple les images qui ont été vues peuvent ętre écrites en bleu, tandis que
 * les images manquantes peuvent ętre écrites en rouge. Cet affichage coloré en
 * fonction des images peut ętre activé avec le code suivant:
 *
 * <blockquote><pre>
 * {@link TableCellRenderer} renderer=new {@link CellRenderer}();
 * view.setDefaultRenderer({@link String}.class, renderer);
 * view.setDefaultRenderer(  {@link Date}.class, renderer);
 * </pre></blockquote>
 *
 * La classe <code>CoverageTableModel</code> garde une trace des images qui
 * sont ajoutées ou retirées de la table. Ces opérations peuvent ętre annulées.
 * Les fonctions "annuler" et "refaire" peuvent ętre activées avec le code suivant:
 *
 * <blockquote><pre>
 * final {@link UndoManager} undoManager=new UndoManager();
 * ((CoverageTableModel) model).addUndoableEditListener(undoManager);
 * </pre></blockquote>
 *
 * On peut ensuite utiliser les méthodes {@link UndoManager#undo} et
 * {@link UndoManager#redo} pour défaire ou refaire une opération.
 * <br>
 * La plupart des méthodes de cette classe peuvent ętre appelée de n'importe
 * quel thread (pas nécessairement celui de <i>Swing</i>).  Si l'appel d'une
 * méthode a changée le contenu de la table, <i>Swing</i> en sera informé dans
 * son propre thread męme si les méthodes ont été appelées d'un autre thread.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class CoverageTableModel extends AbstractTableModel {
    /**
     * Numéro de série (pour compatibilité avec des versions antérieures).
     */
    private static final long serialVersionUID = 6723633134014245147L;

    /**
     * Indique s'il faut inverser l'ordre des enregistrements.
     */
    private static final boolean REVERSE_ORDER = true;

    /** Numéro de colonne des noms de fichiers.   */ private static final int NAME     = 0;
    /** Numéro de colonne des dates des images.   */ private static final int DATE     = 1;
    /** Numéro de colonne de la durée des images. */ private static final int DURATION = 2;

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
     * Série d'images représenté par cette table.
     */
    private SeriesEntry series;

    /**
     * Liste des entrées contenues dans cette table. La longueur
     * de ce tableau est le nombre de lignes dans la table.
     */
    private CoverageEntry[] entries;

    /**
     * Objet ŕ utiliser pour formatter les dates des images.
     */
    private final DateFormat dateFormat;

    /**
     * Objet ŕ utiliser pour formatter les durées des images.
     */
    private final DateFormat timeFormat;

    /**
     * Objet ŕ utiliser pour formatter les nombres.
     */
    private final NumberFormat numberFormat;

    /**
     * Objet ŕ utiliser pour obtenir la position d'un champ formatté.
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
     * ętre ajoutées plus tard en appelant {@link #setEntries}.
     *
     * @param series Séries que représentera cette table, ou
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
     * Construit une table qui contiendra une copie du contenu de la table spécifiée.
     * La nouvelle table ne contiendra initialement aucun <code>Listener</code> (c'est
     * ŕ dire que les <code>Listener</code> de la table spécifiée ne seront pas copiés).
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
                    final ProxyEntry newProxy = new ProxyEntry(unwrap(oldProxy.entry));
                    newProxy.flags = oldProxy.flags;
                    entries[i] = newProxy;
                }
            }
        }
    }

    /**
     * Construit une table qui contiendra la liste des images de <code>table</code>.
     * Ce constructeur mémorise la série de <code>table</code>, telle que retournée
     * par {@link CoverageTable#getSeries}.
     *
     * @param  table Table dans laquelle puiser la liste des images.
     * @throws RemoteException si l'interrogation de la table a échouée.
     */
    public CoverageTableModel(final CoverageTable table) throws RemoteException {
        this(table.getSeries());
        final List<CoverageEntry> entryList = table.getEntries();
        entries = entryList.toArray(new CoverageEntry[entryList.size()]);
        if (REVERSE_ORDER) {
            reverse(entries);
        }
    }

    /**
     * Renverse l'ordre des éléments du tableau spécifié.
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
     * La série de <code>table</code> (telle que retournée par {@link CoverageTable#getSeries})
     * deviendra la série courante, celle que retourne {@link #getSeries}. Cette méthode
     * peut ętre appelée de n'importe quel thread (pas nécessairement celui de <i>Swing</i>).
     *
     * @param  table Table dans laquelle puiser la liste des images.
     * @throws RemoteException si l'interrogation de la table a échouée.
     */
    public void setEntries(final CoverageTable table) throws RemoteException {
        final List<CoverageEntry> entryList;
        synchronized (table) {
            entryList = table.getEntries();
            series    = table.getSeries();
        }
        setEntries(entryList);
    }

    /**
     * Remplace tous les enregistrements courants par les enregistrements
     * spécifiés. Cette méthode peut ętre appelée de n'importe quel thread
     * (pas nécessairement celui de <i>Swing</i>).
     *
     * @param entryList Liste des nouvelles entrées.
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
     * le tableau spécifié. Les valeurs seront les objets {@link ProxyEntry},
     * tandis que les clés seront les objets {@link CoverageEntry} qu'ils
     * enveloppent. Cette méthode retourne <code>null</code> si aucun objets
     * {@link ProxyEntry} n'a été trouvé.
     *
     * @param  entries Entrées dans lequel vérifier s'il y a des {@link ProxyEntry}.
     * @param  proxies Dictionnaire dans lequel ajouter les {@link ProxyEntry} trouvés,
     *         ou <code>null</code> si aucun dictionnaire n'a encore été créé.
     * @return L'argument <code>proxies</code>, ou un nouvel objet {@link Map}
     *         si <code>proxies</code> était nul.
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
                    proxies.put(proxy.entry, proxy);
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
            entry = ((ProxyEntry) entry).entry;
        }
        return entry;
    }

    /**
     * Retourne les entrées de toutes les images présentes dans la table. Les
     * opérations de lectures effectuées sur les entrées retournées ne seront
     * pas indiquées dans la table <code>this</code> (contrairement aux entrées
     * retournées par {@link #getEntryAt}, qui écrive en bleu les images lues).
     *
     * @return Les entrées de toutes les images de cette table. Ce tableau peut
     *         avoir une longueur de 0, mais ne sera jamais <code>null</code>.
     * @throws RemoteException si une interrogation de la base de données était
     *         nécessaire et a échoué.
     */
    public synchronized CoverageEntry[] getEntries() throws RemoteException {
        final CoverageEntry[] entries = this.entries;
        final CoverageEntry[] out = new CoverageEntry[(entries!=null) ? entries.length : 0];
        for (int i=out.length; --i>=0;) {
            out[i] = unwrap(entries[i]);
        }
        return out;
    }

    /**
     * Retourne l'entré de l'image qui se trouve ŕ la ligne spécifiée.
     * Pour économiser la mémoire, il est recommandé de ne pas retenir
     * cette référence plus longtemps que la durée de vie de cette table
     * <code>CoverageTableModel</code>.
     *
     * @param  row Index de l'entré désiré.
     */
    public synchronized CoverageEntry getEntryAt(final int row) {
        CoverageEntry entry = entries[row];
        if (!(entry instanceof ProxyEntry)) {
            entries[row] = entry = new ProxyEntry(entry);
        }
        return entry;
    }

    /**
     * Retourne les noms des images présentes dans cette table. Les noms sont obtenus
     * par {@link CoverageEntry#getName} et sont habituellement unique pour une série
     * donnée. Cette méthode peut retourner un tableau de longueur 0, mais ne retourne
     * jamais <code>null</code>.
     *
     * @throws RemoteException si une interrogation de la base de données était
     *         nécessaire et a échoué.
     */
    public synchronized String[] getEntryNames() throws RemoteException {
        final String[] names = new String[(entries!=null) ? entries.length : 0];
        for (int i=0; i<names.length; i++) {
            names[i] = entries[i].getName();
        }
        return names;
    }

    /**
     * Retourne les noms des images aux lignes spécifiées. Les noms sont obtenus
     * par {@link CoverageEntry#getName}. Cette méthode peut retourner un tableau
     * de longueur 0, mais ne retourne jamais <code>null</code>.
     *
     * @throws RemoteException si une interrogation de la base de données était
     *         nécessaire et a échoué.
     */
    public synchronized String[] getEntryNames(int[] rows) throws RemoteException {
        final String[] names = new String[rows.length];
        for (int i=0; i<names.length; i++) {
            names[i] = entries[rows[i]].getName();
        }
        return names;
    }

    /**
     * Retourne les numéros de lignes qui correspondent aux images spécifiées.
     * Les images sont désignées par leurs noms (tel que retourné par
     * {@link CoverageEntry#getName}). Cette méthode est l'inverse de
     * {@link #getEntryNames}.
     *
     * @param  names Noms des images.
     * @return Numéro de lignes des images demandées. Ce tableau aura toujours
     *         la męme longueur que <code>names</code>. Les images qui n'ont pas
     *         été trouvées dans la table auront l'index -1.
     * @throws RemoteException si une interrogation de la base de données était
     *         nécessaire et a échoué.
     */
    public synchronized int[] indexOf(final String[] names) throws RemoteException {
        final Map<String,int[]> map = new HashMap<String,int[]>(names.length*2);
        for (int i=0; i<names.length; i++) {
            int[] index = map.put(names[i], new int[]{i});
            if (index != null) {
                // Cas oů le męme nom serait demandé plusieurs fois.
                final int length = index.length;
                index = XArray.resize(index, length+1);
                index[length] = i;
                map.put(names[i], index);
            }
        }
        final int[] rows = new int[names.length];
        Arrays.fill(rows, -1);
        // Fait la boucle en sens inverse de façon ŕ ce qu'en cas de doublons,
        // l'occurence retenue soit la premičre apparaissant dans la liste.
        for (int i=entries.length; --i>=0;) {
            final int[] index = map.get(entries[i].getName());
            if (index != null) {
                for (int j=0; j<index.length; j++) {
                    rows[index[j]] = i;
                }
            }
        }
        return rows;
    }

    /**
     * Retire une entrée de cette table. Si <code>toRemove</code> est
     * nul ou n'apparaît pas dans la table, alors il sera ignoré.
     */
    public synchronized void remove(final CoverageEntry toRemove) {
        remove(Collections.singleton(unwrap(toRemove)));
    }

    /**
     * Retire l'entrée qui se trouve ŕ l'index spécifié. L'index
     * <code>row</code> correspond au numéro (ŕ partir de 0)  de
     * la ligne ŕ supprimer.
     */
    public synchronized void remove(final int row) {
        remove(entries[row]);
    }

    /**
     * Retire plusieurs entrées de cette table. Les entrées
     * nulles ainsi que celles qui n'apparaissent pas dans
     * cette table seront ignorées.
     */
    public synchronized void remove(final CoverageEntry[] toRemove) {
        final Set<CoverageEntry> toRemoveSet;
        toRemoveSet = new HashSet<CoverageEntry>(2*toRemove.length);
        for (int i=0; i<toRemove.length; i++) {
            toRemoveSet.add(unwrap(toRemove[i]));
        }
        remove(toRemoveSet);
    }

    /**
     * Retire plusieurs entrées désignés par les index des lignes.  Les index
     * <code>rows</code> correspondent aux numéros (ŕ partir de 0) des lignes
     * ŕ supprimer. Ces numéros de lignes peuvent ętre dans n'importe quel
     * ordre.
     */
    public synchronized void remove(final int[] rows) {
        final Set<CoverageEntry> toRemoveSet;
        toRemoveSet = new HashSet<CoverageEntry>(2*rows.length);
        for (int i=0; i<rows.length; i++) {
            toRemoveSet.add(unwrap(entries[rows[i]]));
        }
        remove(toRemoveSet);
    }

    /**
     * Retire plusieurs entrées de cette table. Les entrées
     * nulles ainsi que celles qui n'apparaissent pas dans
     * cette table seront ignorées. Cette méthode peut ętre
     * appelée de n'importe quel thread (pas nécessairement
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
                final int lower = i+1;
                if (upper != lower) {
                    if (entries == oldEntries) {
                        // Créé une copie, de façon ŕ ne pas modifier le tableau 'entries' original.
                        entries = XArray.remove(entries, lower, upper-lower);
                    } else {
                        // Si le tableau est déjŕ une copie, travaille directement sur lui.
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
     * Copie les données de certaines lignes dans un objet transférable. Cet
     * objet pourra ętre placé dans le presse papier pour ętre ensuite collé
     * dans un tableur commercial par exemple. Le presse-papier du systčme
     * peut ętre obtenu par un appel ŕ:
     *
     * <blockquote><pre>
     * Toolkit.getDefaultToolkit().getSystemClipboard()
     * </pre></blockquote>
     *
     * @param  rows Ligne ŕ copier.
     * @return Objet transférable contenant les lignes copiées.
     * @throws RemoteException si une interrogation de la base de données était
     *         nécessaire et a échoué.
     */
    public synchronized Transferable copy(final int[] rows) throws RemoteException {
        if (fieldPosition == null) {
            fieldPosition = new FieldPosition(0);
        }
        final StringBuffer buffer = new StringBuffer(256); // On n'utilise pas le buffer des cellules.
        final int[] clés = new int[] {
            ResourceKeys.NAME,
            ResourceKeys.START_TIME,
            ResourceKeys.END_TIME
        };
        for (int i=0; i<clés.length;) {
            buffer.append(Resources.format(clés[i++]));
            buffer.append((i<clés.length) ? '\t' : '\n');
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
            //       mais ça donne un résultat bizarre quand on colle dans Excel. Il
            //       met une ligne vierge entre chaque ligne de données.
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
     * Retourne le nom de la colonne spécifiée.
     */
    public String getColumnName(final int column) {
        return titles[column];
    }

    /**
     * Retourne la classe des objets de la colonne spécifiée.
     */
    public Class getColumnClass(final int column) {
        return CLASS[column];
    }

    /**
     * Retourne la valeur de la cellule aux index spécifiés.
     *
     * @param  row    Numéro de ligne de la cellule, ŕ partir de 0.
     * @param  column Numéro de colonne de la cellule, ŕ partir de 0.
     * @return Valeur de la cellule aux index spécifiés.
     */
    public synchronized Object getValueAt(final int row, final int column) {
        CoverageEntry entry = entries[row];
        if (!(entry instanceof ProxyEntry)) {
            entries[row] = entry = new ProxyEntry(entry);
        }
        try {
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
        } catch (RemoteException exception) {
            ((ProxyEntry) entry).setFlag(ProxyEntry.RMI_FAILURE, true);
            // TODO: Log a message with FINE level.
            return null;
        }
    }

    /**
     * Convertit une date en chaîne de caractčres.
     */
    private String format(final Date date) {
        if (buffer        == null) buffer        = new StringBuffer ( );
        if (fieldPosition == null) fieldPosition = new FieldPosition(0);
        buffer.setLength(0);
        dateFormat.format(date, buffer, fieldPosition);
        return buffer.toString();
    }

    /**
     * Retourne la série d'images représentée par cette table. Si la série n'est
     * pas connue ou si cette table contient des images de plusieurs séries
     * différentes, alors cette méthode peut retourner <code>null</code>.
     */
    public SeriesEntry getSeries() {
        return series;
    }

    /**
     * Retourne le fuseau horaire utilisé pour les écritures de dates.
     */
    public synchronized TimeZone getTimeZone() {
        return dateFormat.getTimeZone();
    }

    /**
     * Définit le fuseau horaire ŕ utiliser pour l'écriture des dates.
     */
    public synchronized void setTimeZone(final TimeZone timezone) {
        dateFormat.setTimeZone(timezone);
        if (entries.length != 0) {
            fireTableChanged(new TableModelEvent(this, 0, entries.length-1, DATE));
        }
    }

    /**
     * Ajoute un objet ŕ la liste des objets intéressés ŕ ętre
     * informés chaque fois qu'une édition anulable a été faite.
     */
    public void addUndoableEditListener(final UndoableEditListener listener) {
        listenerList.add(UndoableEditListener.class, listener);
    }

    /**
     * Retire un objet de la liste des objets intéressés ŕ ętre
     * informés chaque fois qu'une édition anulable a été faite.
     */
    public void removeUndoableEditListener(final UndoableEditListener listener) {
        listenerList.remove(UndoableEditListener.class, listener);
    }

    /**
     * Prend en compte des changements qui viennent d'ętre apportées ŕ la table.
     * Cette méthode mettra ŕ jour la variable {@link #backup} et préviendra tous
     * les objets qui étaient intéressés ŕ ętre informés des changements anulables.
     */
    private void commitEdit(final CoverageEntry[] oldEntries,
                            final CoverageEntry[] newEntries,
                            final int clé) // NO synchronized!
    {
        final String name = Resources.format(clé).toLowerCase();
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
     * Indique que l'enregistrement {@link #entry} a changé.  Cette méthode
     * recherche la ligne correspondant ŕ cette entrée et lance l'événement
     * appropriée. Cette méthode peut ętre appelée ŕ partir de n'importe quel
     * thread (pas nécessairement celui de <i>Swing</i>).
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
     * Classe des entrées des images.  Cette classe redirige la plupart des appels de ses méthodes
     * vers un autre objet {@link CoverageEntry}. La principale exception est la méthode
     * {@link #getGridCoverage}, qui intercepte les appels pour mettre ŕ jour des variables
     * internes indiquant si une image a été vue ou si sa lecture a échouée.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final class ProxyEntry extends CoverageEntry.Proxy {
        /**
         * Numéro de série (pour compatibilité avec des versions antérieures).
         */
        private static final long serialVersionUID = 8398851451224196337L;

        /** Drapeau indiquant qu'une image a été vue.        */ public static final byte VIEWED      = 1;
        /** Drapeau indiquant qu'un fichier est introuvable. */ public static final byte MISSING     = 2;
        /** Drapeau indiquant qu'un fichier est mauvais.     */ public static final byte CORRUPTED   = 4;
        /** Drapeau indiquant qu'un appel RMI a échoué.      */ public static final byte RMI_FAILURE = 8;
        /** Drapeau indiquant l'état de l'image courante.    */ public              byte flags;

        /**
         * Construit un proxy.
         */
        public ProxyEntry(final CoverageEntry entry) {
            super(entry);
            FileChecker.add(this);
        }

        /**
         * Procčde ŕ la lecture d'une image. Si la lecture a réussi sans avoir été
         * annulée par l'utilisateur, alors le drapeau {@link #VIEWED} sera levé.
         * Si la lecture a échoué, alors le drapeau {@link #CORRUPTED} sera levé.
         */
        public GridCoverage getGridCoverage(final EventListenerList listenerList) throws IOException {
            try {
                final GridCoverage image = entry.getGridCoverage(listenerList);
                setFlag((byte)(MISSING|CORRUPTED|RMI_FAILURE), false);
                setFlag(VIEWED, image!=null);
                return image;
            } catch (RemoteException exception) {
                setFlag(RMI_FAILURE, true);
                throw exception;
            } catch (FileNotFoundException exception) {
                setFlag(MISSING, true);
                throw exception;
            } catch (IOException exception) {
                setFlag(CORRUPTED, true);
                throw exception;
            }
        }

        /**
         * Place ou retire les drapeaux spécifiés. Si l'appel de cette méthode a modifié
         * l'état des drapeaux, alors {@link #fireTableRowsUpdated} sera appelée.
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
     * Classe du thread qui aura la charge de vérifier si les fichiers des images existent.
     * Lorsqu'un nouvel objet {@link ProxyEntry} est créé, il peut appeler la méthode statique
     * {@link #add} pour s'ajouter lui-męme ŕ la liste des images dont on vérifiera l'existence.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private final static class FileChecker extends Thread {
        /**
         * Thread ayant la charge de vérifier si des fichiers existent.
         */
        private static FileChecker thread;

        /**
         * Liste des fichiers dont on veut vérifier l'existence.
         */
        private final LinkedList<ProxyEntry> list = new LinkedList<ProxyEntry>();

        /**
         * Construit un thread qui vérifiera l'existence des fichiers. Le processus démarrera
         * immédiatement, mais bloquera presque aussitôt sur la méthode {@link #next}  (parce
         * qu'elle est synchronisée sur le męme moniteur que {@link #add}, la méthode qui
         * appelle ce constructeur). L'exécution continuera lorsque la méthode {@link #add}
         * aura terminé, ce qui garantit qu'il y aura au moins une image ŕ vérifier.
         */
        private FileChecker() {
            super("FileChecker");
            setPriority(MIN_PRIORITY);
            setDaemon(true);
            start();
        }

        /**
         * Ajoute une entrée ŕ la liste des images ŕ vérifier.
         */
        public static synchronized void add(final ProxyEntry entry) {
            if (thread == null) {
                thread = new FileChecker();
            }
            thread.list.add(entry);
        }

        /**
         * Retourne la prochaine image ŕ vérifier, ou <code>null</code>
         * s'il n'en reste plus. S'il ne reste plus d'images, alors cette
         * méthode signalera que le thread va mourrir en donnant la valeur
         * <code>null</code> ŕ {@link #thread].
         */
        private static synchronized ProxyEntry next(final LinkedList<ProxyEntry> list) {
            if (list.isEmpty()) {
                thread = null;
                return null;
            }
            return list.removeFirst();
        }

        /**
         * Vérifie si les fichiers de la liste existent. Si un fichier
         * n'existe pas, le drapeau {@link ProxyEntry#MISSING} sera lévé
         * pour l'objet {@link ProxyEntry} correspondant. Cette vérification
         * n'est pas effectuée pour les objets résidant sur un serveur distant.
         */
        public void run() {
            ProxyEntry entry;
            while ((entry=next(list)) != null) {
                CoverageEntry check = entry;
                do {
                    check = ((CoverageEntry.Proxy) check).entry;
                    if (check instanceof RemoteStub) {
                        return;
                    }
                } while (check instanceof CoverageEntry.Proxy);
                // No stub (we are running on the local machine).
                // Check for file existence.
                try {
                    final File file = entry.getFile();
                    if (file != null) {
                        entry.setFlag(ProxyEntry.MISSING, !file.isFile());
                    }
                    entry.setFlag(ProxyEntry.RMI_FAILURE, false);
                } catch (RemoteException exception) {
                    entry.setFlag(ProxyEntry.RMI_FAILURE, true);
                    // TODO: Log a message with FINE level.
                }
            }
        }
    }

    /**
     * Classe pour afficher des cellules de {@link CoverageTableModel} dans une table
     * {@link JTable}. Par défaut, cette classe affiche le texte des cellules avec
     * leur couleur habituelle (noir). Elle peut toutefois utiliser des couleurs
     * différentes si l'image a été vue (bleu) ou si elle est manquante (rouge).
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    public static class CellRenderer extends DefaultTableCellRenderer {
        /**
         * Couleur par défaut de la police.
         */
        private Color foreground;

        /**
         * Couleur par défaut de l'arričre plan.
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
         * Définit la couleur de la police.
         */
        public void setForeground(final Color foreground) {
            super.setForeground(this.foreground=foreground);
        }

        /**
         * Définit la couleur de l'arričre-plan.
         */
        public void setBackground(final Color background) {
            super.setBackground(this.background=background);
        }

        /**
         * Retourne une composante ŕ utiliser pour dessiner le contenu des
         * cellules de la table.  Cette méthode utilise une composante par
         * défaut, mais en changeant la couleur du texte si l'entrée correspond
         * ŕ une image qui a déjŕ été lue.
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
                        if ((flags & ProxyEntry.VIEWED     ) != 0) {foreground=Color.BLUE ;                         }
                        if ((flags & ProxyEntry.MISSING    ) != 0) {foreground=Color.RED  ;                         }
                        if ((flags & ProxyEntry.CORRUPTED  ) != 0) {foreground=Color.WHITE; background=Color.RED;   }
                        if ((flags & ProxyEntry.RMI_FAILURE) != 0) {foreground=Color.BLACK; background=Color.YELLOW;}
                    }
                }
            }
            super.setBackground(background);
            super.setForeground(foreground);
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }
}
