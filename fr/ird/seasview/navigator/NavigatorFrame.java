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
package fr.ird.seasview.navigator;

// Image et base de donn�es
import java.sql.SQLException;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.image.SeriesEntry;

// Interface utilisateur
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import fr.ird.awt.ImageTableModel;
import fr.ird.awt.CoordinateChooserDB;
import org.geotools.util.ProgressListener;
import org.geotools.gui.swing.ProgressWindow;

// Main framework
import fr.ird.seasview.Task;
import fr.ird.seasview.DataBase;
import fr.ird.seasview.InternalFrame;
import fr.ird.seasview.layer.control.LayerControl;

// Mod�les et �v�nements
import java.awt.EventQueue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;

// Divers
import java.util.Map;
import java.util.List;
import java.util.TimeZone;

import fr.ird.util.XArray;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;

// Geotools dependencies
import org.geotools.gui.swing.StatusBar;
import org.geotools.gui.swing.ExceptionMonitor;


/**
 * Fen�tre affichant une table des images disponibles, ainsi qu'une vue de ces
 * images.   L'utilisateur pourra choisir une ou plusieurs images de son choix
 * dans la table, et changer de s�ries d'images � l'aide des onglets dans le
 * bas de la fen�tre.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class NavigatorFrame extends InternalFrame implements ChangeListener {
    /**
     * Connection avec la base de donn�es d'images.
     */
    private final ImageTable table;

    /**
     * Bo�te de dialogue qui servira � demander �
     * l'utilisateur la plage de coordonn�es qui
     * l'int�resse.
     */
    private CoordinateChooserDB chooser;

    /**
     * Barre d'�tat � placer dans le bas de la fen�tre. Cette barre
     * d'�tat servira aussi � informer l'utilisateur des progr�s de
     * la lecture.
     */
    private final StatusBar statusBar=new StatusBar();

    /**
     * Composante qui contiendra les onglets pour chaque types d'images.
     * Les composantes de ce paneau seront pour la plupart de la classe
     * <code>ImagePanel</code>.
     */
    private final JTabbedPane tabs=new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);

    /**
     * Objet � utiliser pour dessiner les cellules des tables.
     * Toutes les tables de cette fen�tre partageront le m�me objet.
     */
    private final TableCellRenderer renderer=new ImageTableModel.CellRenderer();

    /**
     * Construit une fen�tre d'images.
     *
     * @param  database Base de donn�es sur laquelle se connecter.
     * @param  chooser Bo�te de dialogue qui servira � demander � l'utilisateur
     *         la plage de coordonn�es qui l'int�resse. La fen�tre sera initialis�e
     *         avec les s�ries actuellement s�lectionn�es dans <code>chooser</code>.
     *         Cette bo�te de dialogue sera ensuite transform�e et retenue afin de
     *         permettre � l'utilisateur de modifier ces coordonn�es. Cet argument
     *         peut �tre nul si aucune bo�te de dialogue n'a �t� construite.
     * @param  owner La composante parente (pour affichage des progr�s).
     * @throws SQLException Si l'acc�s � la base de donn�es a �chou�.
     */
    public NavigatorFrame(final DataBase           database,
                          final CoordinateChooserDB chooser,
                          final JComponent            owner) throws SQLException
    {
        super(Resources.format(ResourceKeys.IMAGES_LIST));
        final SeriesEntry[] series;
        this.chooser=chooser;
        if (chooser != null) {
            series = chooser.getSeries();
            chooser.setSeriesVisible(false);
        }
        else series = new SeriesEntry[0];
        table = database.getImageTable(series.length!=0 ? series[0] : null);
        configureTable();

        final Container panel=getContentPane();
        panel.setLayout(new BorderLayout());
        panel.add(tabs,      BorderLayout.CENTER);
        panel.add(statusBar, BorderLayout.SOUTH );
        tabs.setOpaque(true);

        final ThreadGroup readers = database.getThreadGroup();
        final LayerControl[] ctrl = database.getLayerControls();
        final ImageMosaicPanel mosaic = new ImageMosaicPanel(table, statusBar, readers, ctrl);
        tabs.addTab(Resources.format(ResourceKeys.MOSAIC), /*icon, */ mosaic);

        addSeries(database, series, owner);
        setFrameIcon(getIcon("org/javalobby/icons/20x20/Sheet.gif"));
    }

    /**
     * Retourne le paneau repr�sentant les mosa�ques
     * d'images, ou <code>null</code> s'il n'y en a pas.
     */
    private ImageMosaicPanel getMosaicPanel() {
        final Component[] tabs=this.tabs.getComponents();
        for (int i=0; i<tabs.length; i++) {
            final Component c=tabs[i];
            if (c instanceof ImageMosaicPanel) {
                return ((ImageMosaicPanel) c);
            }
        }
        return null;
    }

    /**
     * Reset the divider location. This is a workaround
     * for what seems to be a regression bugs in JDK 1.4.0.
     */
    public void resetDividerLocation() {
        setSize(getWidth()+1, getHeight()+1);
        final Component[] tabs=this.tabs.getComponents();
        for (int i=tabs.length; --i>=0;) {
            final Component c = tabs[i];
            if (c instanceof ImagePanel) {
                ((ImagePanel) c).resetDividerLocation();
            }
        }
    }

    /**
     * Construit le paneau {@link CoordinateChooserDB}
     * si ce paneau n'existait pas d�j�.
     */
    private void buildChooser() throws SQLException {
        if (chooser == null) {
            chooser = new CoordinateChooserDB(getDataBase().getImageDataBase());
            chooser.setSeriesVisible(false);
        }
    }

    /**
     * Configure la table en fonction des coordonn�es spatio-temporelles
     * qu'a sp�cifi� l'utilisateur. Les s�ries toutefois ne seront pas
     * modifi�es.
     *
     * @throws SQLException Si l'acc�s � la base de donn�es a �chou�.
     */
    private void configureTable() throws SQLException {
        if (chooser != null) {
            synchronized (table) {
                table.setTimeRange          (chooser.getStartTime(), chooser.getEndTime());
                table.setGeographicArea     (chooser.getGeographicArea());
                table.setPreferredResolution(chooser.getPreferredResolution());
            }
        }
    }

    /**
     * Proc�de � un nouveau chargement des entr�s d'images � partir de la base
     * de donn�es. Cette m�thode peut �tre appel�e apr�s que la base de donn�es
     * ait chang�, ou apr�s que l'utilisateur ait chang� les coordonn�es de la
     * r�gion qu'il a demand� � voir. Cette m�thode peut �tre appel�e � partir
     * de n'importe quel thread (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param  tabs Liste des paneau. Seul les paneaux de la classe {@link ImagePanel}
     *         seront pris en compte. Bien que la m�thode {@link Container#getComponents}
     *         soit thread-safe, elle devrait avoir �t� appel�e dans le thread de Swing
     *         pour plus de s�curit�.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    private void refresh(final Component[] tabs) throws SQLException {
        // Note: si 'refresh' est appel�e en m�me temps que 'addSeries'
        //       (dans deux threads s�par�s), toutes les tables seront
        //       remises � jour ('refresh') avant que 'addSeries' ne
        //       continue. Cette fonctionalit� voulue est le r�sultat
        //       de la position des 'synchronized(table)'.
        synchronized (table) {
            Map<SeriesEntry,List<ImageEntry>> entries = null;
            final ImageMosaicPanel mosaic = getMosaicPanel();
            if (mosaic != null) {
                entries = mosaic.refresh(table);
            }
            for (int i=0; i<tabs.length; i++) {
                if (tabs[i] instanceof ImageTablePanel) {
                    final ImageTablePanel panel = (ImageTablePanel) tabs[i];
                    final SeriesEntry    series = panel.getSeries();
                    if (entries != null) {
                        final List<ImageEntry> images = entries.get(series);
                        if (images != null) {
                            panel.setEntries(images);
                            continue;
                        }
                    }
                    table.setSeries(series);
                    panel.setEntries(table);
                }
            }
        }
    }

    /**
     * Ajoute des s�ries. Si une ou plusieurs des s�ries �
     * ajouter apparaissaient d�j�, elles seront ignor�es.
     *
     * @param  database Bases de donn�es.
     * @param  series S�ries � ajouter.
     * @param  owner La composante parente (pour affichage des progr�s).
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
     */
    private void addSeries(final DataBase database,
                           final SeriesEntry[] series,
                           final JComponent owner) throws SQLException
    {
        final ProgressWindow progress = new ProgressWindow(owner);
        progress.setTitle(Resources.format(ResourceKeys.LOOKING_INTO_DATABASE));
        progress.started();
        try {
loop:       for (int j=0; j<series.length; j++) {
                final SeriesEntry s�rie=series[j];
                final JTabbedPane tabs=this.tabs;
                final Component[] cmps=tabs.getComponents();
                for (int i=cmps.length; --i>=0;) {
                    final Component c=cmps[i];
                    if (c instanceof ImageTablePanel) {
                        if (s�rie.equals(((ImageTablePanel) c).getSeries())) {
                            continue loop;
                        }
                    }
                }
                final String           name   = s�rie.getName();
                final ImageMosaicPanel mosaic = getMosaicPanel();
                final ImageTablePanel  panel;
                final ImageTableModel  model;
                progress.setDescription(name);
                synchronized (table) {
                    table.setSeries(s�rie);
                    if (mosaic != null) {
                        model = new ImageTableModel(s�rie);
                        model.setEntries(mosaic.addSeries(table));
                    } else {
                        model = new ImageTableModel(table);
                    }
                }
                // Do not invokes following code inside the
                // synchronized block: it cause deadlock.
                panel = createPanel(model, database);
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        tabs.addTab(name, panel);
                    }
                });
            }
        } finally {
            progress.complete();
            progress.dispose();
        }
    }

    /**
     * Construit un paneau {@link ImageTablePanel} � partir d'une table
     * {@link ImageTableModel}. Le paneau construit ne sera pas ajout�
     * aux onglets; c'est � l'appelant de le faire.
     */
    private ImageTablePanel createPanel(final ImageTableModel model, final DataBase database) throws SQLException {
        final ThreadGroup readers = database.getThreadGroup();
        final LayerControl[] ctrl = database.getLayerControls();
        final ImageTablePanel panel=new ImageTablePanel(model, renderer, statusBar, readers, ctrl);
        panel.addChangeListener(this);
        return panel;
    }

    /**
     * Retourne les s�ries affich�es dans cette fen�tre. Cette m�thode
     * peut retourner un tableau de longueur 0, mais ne retournera
     * jamais <code>null</code>.
     */
    public SeriesEntry[] getSeries() {
        final Component[] tabs = this.tabs.getComponents();
        final SeriesEntry[] series = new SeriesEntry[tabs.length];
        int count=0;
        for (int i=0; i<tabs.length; i++) {
            final Component c=tabs[i];
            if (c instanceof ImageTablePanel) {
                series[count++] = ((ImageTablePanel) c).getSeries();
            }
        }
        return XArray.resize(series, count);
    }

    /**
     * Retourne le nombre de s�ries affich�es dans cette fen�tre.
     */
    private int getSeriesCount() {
        final Component[] tabs = this.tabs.getComponents();
        int count=0;
        for (int i=0; i<tabs.length; i++) {
            final Component c=tabs[i];
            if (c instanceof ImageTablePanel) {
                count++;
            }
        }
        return count;
    }

    /**
     * Supprime l'onglet � l'index sp�cifi�.
     */
    private final void removeTabAt(final int index) {
        final Component tab=tabs.getComponent(index);
        tabs.removeTabAt(index);
        if (tab instanceof ImagePanel) try {
            ((ImagePanel) tab).dispose();
        } catch (SQLException exception) {
            ExceptionMonitor.show(this, exception);
        }
    }

    /**
     * Indique si cette fen�tre peut traiter l'op�ration d�sign�e par le code sp�cifi�. Le
     * code <code>cl�</code> d�signe une des constantes de l'interface {@link ResourceKeys},
     * utilis� pour la construction des menus. Par exemple le code {@link ResourceKeys#EXPORT}
     * d�signe le menu "exporter".
     */
    protected boolean canProcess(final int cl�) {
        final Component c=tabs.getSelectedComponent();
        if (c instanceof ImagePanel) {
            if (((ImagePanel) c).canProcess(cl�)) {
                return true;
            }
        }
        switch (cl�) {
            default:                              return super.canProcess(cl�);
            case ResourceKeys.CLOSE_SERIES:       return (c instanceof ImageTablePanel);
            case ResourceKeys.ADD_SERIES:         // fall through
            case ResourceKeys.CHANGE_COORDINATES: return true;
            case ResourceKeys.COUPLING:           return getSeriesCount()!=0;
        }
    }

    /**
     * Ex�cute une action. Le code <code>cl�</code> de l'action est le m�me
     * que celui qui avait �t� pr�alablement transmis � {@link #canProcess}.
     * Si le code de l'action n'est pas reconnue, une exception sera lanc�e.
     *
     * @throws SQLException si une interrogation de la base de donn�es �tait
     *         n�cessaire et a �chou�e.
     */
    protected Task process(final int cl�) throws SQLException {
        final Component c=tabs.getSelectedComponent();
        if (c instanceof ImagePanel) {
            if (((ImagePanel) c).process(cl�)) {
                return null;
            }
        }
        final Resources resources = Resources.getResources(getLocale());
        Task task=null;
        switch (cl�) {
            default: {
                task = super.process(cl�);
                break;
            }
            ///////////////////////////////////
            ///  S�ries - Ajouter une s�rie ///
            ///////////////////////////////////
            case ResourceKeys.ADD_SERIES: {
                buildChooser();
                final SeriesEntry[] series = chooser.showSeriesDialog(this, resources.getString(ResourceKeys.ADD_SERIES));
                if (series != null) {
                    configureTable();
                    final DataBase database = getDataBase();
                    task = new Task(resources.getString(ResourceKeys.ADD_SERIES)) {
                        protected void run() throws SQLException {
                            addSeries(database, series, NavigatorFrame.this);
                        }
                    };
                }
                break;
            }
            /////////////////////////////////
            ///  S�ries - Fermer la s�rie ///
            /////////////////////////////////
            case ResourceKeys.CLOSE_SERIES: {
                removeTabAt(tabs.getSelectedIndex());
                break;
            }
            /////////////////////////////////////////
            ///  S�ries - Changer les coordonn�es ///
            /////////////////////////////////////////
            case ResourceKeys.CHANGE_COORDINATES: {
                buildChooser();
                if (chooser.showDialog(this)) {
                    configureTable();
                    final Component[] tabs=this.tabs.getComponents();
                    task = new Task(resources.getString(ResourceKeys.CHANGE_COORDINATES)) {
                        protected void run() throws SQLException {
                            refresh(tabs);
                        }
                    };
                }
                break;
            }
            //////////////////////////////////////////////////////////
            ///  Fichier - Nouveau - Couplage p�che/environnement  ///
            //////////////////////////////////////////////////////////
//          case ResourceKeys.COUPLING: {
//              final DataBase    database=getDataBase();
//              final SeriesEntry[] series=getSeries();
//              task=new Task(resources.getString(ResourceKeys.COUPLING)) {
//                  public void run() throws SQLException {
//                      show(new CouplingFrame(database, series));
//                  }
//              };
//              break;
//          }
        }
        return task;
    }

    /**
     * Modifie le fuseau horaire pour l'affichage et la saisie des dates.
     * Cette modification n'affecte pas le fuseau horaire des �ventuelles
     * bases de donn�es acc�d�es par cette fen�tre.
     */
    protected void setTimeZone(final TimeZone timezone) {
        super.setTimeZone(timezone);
        final Component[] tabs = this.tabs.getComponents();
        for (int i=tabs.length; --i>=0;) {
            final Component c = tabs[i];
            if (c instanceof ImagePanel) {
                ((ImagePanel) c).setTimeZone(timezone);
            }
        }
    }

    /**
     * Modifie la synchronisation des images. La valeur <code>true</code>
     * indique que tout zoom ou translation appliqu� sur une image d'une
     * mosa�que doit �tre r�pliqu� sur les autres.
     */
    protected void setImagesSynchronized(final boolean s) {
        super.setImagesSynchronized(s);
        final Component[] tabs = this.tabs.getComponents();
        for (int i=tabs.length; --i>=0;) {
            final Component c = tabs[i];
            if (c instanceof ImagePanel) {
                ((ImagePanel) c).setImagesSynchronized(s);
            }
        }
    }

    /**
     * Sp�cifie si les cartes doivent �tre redessin�es
     * durant les glissements des ascenceurs. Sp�cifier
     * <code>true</code> demandera plus de puissance de
     * la part de l'ordinateur.
     */
    protected void setPaintingWhileAdjusting(final boolean s) {
        super.setPaintingWhileAdjusting(s);
        final Component[] tabs = this.tabs.getComponents();
        for (int i=tabs.length; --i>=0;) {
            final Component c = tabs[i];
            if (c instanceof ImagePanel) {
                ((ImagePanel) c).setPaintingWhileAdjusting(s);
            }
        }
    }
    
    /**
     * Pr�viens cette fen�tre que l'�tat d'une de ses fen�tre-fille
     * a chang�. L'impl�mentation par d�faut appele simplement
     * {@link #stateChanged()}.
     */
    public void stateChanged(final ChangeEvent event) {
        stateChanged();
    }

    /**
     * Retourne un objet qui peut �tre sauvegard� et lu en binaire. Cet objet
     * peut servir � enregistrer temporairement l'�tat du bureau ou a envoyer
     * une fen�tre par le r�seau avec les RMI.
     */
    protected Task getSerializable() {
        return new Serializer(this);
    }

    /**
     * Lib�re les ressources utilis�es par cette fen�tre.
     * Cette m�thode est appel�e automatiquement lorsque
     * la fen�tre est ferm�e.
     */
    public void dispose() {
        SQLException exception=null;
        final Component[] tabs=this.tabs.getComponents();
        for (int i=tabs.length; --i>=0;) {
            final Component c = tabs[i];
            if (c instanceof ImagePanel) try {
                ((ImagePanel) c).dispose();
            } catch (SQLException e) {
                if (e != null) {
                    e=exception;
                }
            }
        }
        try {
            table.close();
        } catch (SQLException e) {
            exception = e;
        }
        if (exception != null) {
            ExceptionMonitor.show(this, exception);
        }
        super.dispose();
    }

    /**
     * Classe d'un objet capable de sauvegarder l'�tat de la fen�tre. Les instances
     * de cette classes peuvent �tre enregistr�es en binaire (<i>Serialized</i>).
     * Elles peuvent ensuite �tre relues et reconstruire une copie de la fen�tre
     * par un appel � {@link #run()}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Serializer extends Task {
        /**
         * Tables des images � sauvegarder.
         */
        private final ImageTableModel[] models;

        /**
         * Construit un objet binaire pour la fen�tre sp�cifi�e.
         * Aucune r�f�rence vers la fen�tre <code>frame</code>
         * ne sera retenue.
         */
        public Serializer(final NavigatorFrame frame) {
            super("Serializer");
            final Component[] tabs=frame.tabs.getComponents();
            ImageTableModel[] models=new ImageTableModel[tabs.length];
            int count=0;
            for (int i=0; i<tabs.length; i++) {
                if (tabs[i] instanceof ImageTablePanel) {
                    final ImageTablePanel panel = (ImageTablePanel) tabs[i];
                    models[count++] = panel.getModel();
                    // On obtient une copie du mod�le plut�t que le mod�le
                    // original afin de ne pas avoir les 'Listeners'.
                }
            }
            this.models = models = XArray.resize(models, count);
        }

        /**
         * Reconstruit une fen�tre.
         */
        protected void run() throws SQLException {
            final DataBase    database = getDataBase();
            final NavigatorFrame frame = new NavigatorFrame(database, null, null);
            for (int i=0; i<models.length; i++) {
                final ImageTableModel model = models[i];
                final SeriesEntry    series = model.getSeries();
                frame.tabs.addTab((series!=null) ? series.getName() : Resources.format(ResourceKeys.UNNAMED), frame.createPanel(model, database));
            }
            show(frame);
        }
    }
}
