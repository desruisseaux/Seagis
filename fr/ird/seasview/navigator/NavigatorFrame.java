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
package fr.ird.seasview.navigator;

// Image et base de données
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

// Modèles et événements
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
 * Fenêtre affichant une table des images disponibles, ainsi qu'une vue de ces
 * images.   L'utilisateur pourra choisir une ou plusieurs images de son choix
 * dans la table, et changer de séries d'images à l'aide des onglets dans le
 * bas de la fenêtre.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class NavigatorFrame extends InternalFrame implements ChangeListener {
    /**
     * Connection avec la base de données d'images.
     */
    private final ImageTable table;

    /**
     * Boîte de dialogue qui servira à demander à
     * l'utilisateur la plage de coordonnées qui
     * l'intéresse.
     */
    private CoordinateChooserDB chooser;

    /**
     * Barre d'état à placer dans le bas de la fenêtre. Cette barre
     * d'état servira aussi à informer l'utilisateur des progrès de
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
     * Objet à utiliser pour dessiner les cellules des tables.
     * Toutes les tables de cette fenêtre partageront le même objet.
     */
    private final TableCellRenderer renderer=new ImageTableModel.CellRenderer();

    /**
     * Construit une fenêtre d'images.
     *
     * @param  database Base de données sur laquelle se connecter.
     * @param  chooser Boîte de dialogue qui servira à demander à l'utilisateur
     *         la plage de coordonnées qui l'intéresse. La fenêtre sera initialisée
     *         avec les séries actuellement sélectionnées dans <code>chooser</code>.
     *         Cette boîte de dialogue sera ensuite transformée et retenue afin de
     *         permettre à l'utilisateur de modifier ces coordonnées. Cet argument
     *         peut être nul si aucune boîte de dialogue n'a été construite.
     * @param  owner La composante parente (pour affichage des progrès).
     * @throws SQLException Si l'accès à la base de données a échoué.
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
     * Retourne le paneau représentant les mosaïques
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
     * si ce paneau n'existait pas déjà.
     */
    private void buildChooser() throws SQLException {
        if (chooser == null) {
            chooser = new CoordinateChooserDB(getDataBase().getImageDataBase());
            chooser.setSeriesVisible(false);
        }
    }

    /**
     * Configure la table en fonction des coordonnées spatio-temporelles
     * qu'a spécifié l'utilisateur. Les séries toutefois ne seront pas
     * modifiées.
     *
     * @throws SQLException Si l'accès à la base de données a échoué.
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
     * Procède à un nouveau chargement des entrés d'images à partir de la base
     * de données. Cette méthode peut être appelée après que la base de données
     * ait changé, ou après que l'utilisateur ait changé les coordonnées de la
     * région qu'il a demandé à voir. Cette méthode peut être appelée à partir
     * de n'importe quel thread (pas nécessairement celui de <i>Swing</i>).
     *
     * @param  tabs Liste des paneau. Seul les paneaux de la classe {@link ImagePanel}
     *         seront pris en compte. Bien que la méthode {@link Container#getComponents}
     *         soit thread-safe, elle devrait avoir été appelée dans le thread de Swing
     *         pour plus de sécurité.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    private void refresh(final Component[] tabs) throws SQLException {
        // Note: si 'refresh' est appelée en même temps que 'addSeries'
        //       (dans deux threads séparés), toutes les tables seront
        //       remises à jour ('refresh') avant que 'addSeries' ne
        //       continue. Cette fonctionalité voulue est le résultat
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
     * Ajoute des séries. Si une ou plusieurs des séries à
     * ajouter apparaissaient déjà, elles seront ignorées.
     *
     * @param  database Bases de données.
     * @param  series Séries à ajouter.
     * @param  owner La composante parente (pour affichage des progrès).
     * @throws SQLException si l'interrogation de la base de données a échoué.
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
                final SeriesEntry série=series[j];
                final JTabbedPane tabs=this.tabs;
                final Component[] cmps=tabs.getComponents();
                for (int i=cmps.length; --i>=0;) {
                    final Component c=cmps[i];
                    if (c instanceof ImageTablePanel) {
                        if (série.equals(((ImageTablePanel) c).getSeries())) {
                            continue loop;
                        }
                    }
                }
                final String           name   = série.getName();
                final ImageMosaicPanel mosaic = getMosaicPanel();
                final ImageTablePanel  panel;
                final ImageTableModel  model;
                progress.setDescription(name);
                synchronized (table) {
                    table.setSeries(série);
                    if (mosaic != null) {
                        model = new ImageTableModel(série);
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
     * Construit un paneau {@link ImageTablePanel} à partir d'une table
     * {@link ImageTableModel}. Le paneau construit ne sera pas ajouté
     * aux onglets; c'est à l'appelant de le faire.
     */
    private ImageTablePanel createPanel(final ImageTableModel model, final DataBase database) throws SQLException {
        final ThreadGroup readers = database.getThreadGroup();
        final LayerControl[] ctrl = database.getLayerControls();
        final ImageTablePanel panel=new ImageTablePanel(model, renderer, statusBar, readers, ctrl);
        panel.addChangeListener(this);
        return panel;
    }

    /**
     * Retourne les séries affichées dans cette fenêtre. Cette méthode
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
     * Retourne le nombre de séries affichées dans cette fenêtre.
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
     * Supprime l'onglet à l'index spécifié.
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
     * Indique si cette fenêtre peut traiter l'opération désignée par le code spécifié. Le
     * code <code>clé</code> désigne une des constantes de l'interface {@link ResourceKeys},
     * utilisé pour la construction des menus. Par exemple le code {@link ResourceKeys#EXPORT}
     * désigne le menu "exporter".
     */
    protected boolean canProcess(final int clé) {
        final Component c=tabs.getSelectedComponent();
        if (c instanceof ImagePanel) {
            if (((ImagePanel) c).canProcess(clé)) {
                return true;
            }
        }
        switch (clé) {
            default:                              return super.canProcess(clé);
            case ResourceKeys.CLOSE_SERIES:       return (c instanceof ImageTablePanel);
            case ResourceKeys.ADD_SERIES:         // fall through
            case ResourceKeys.CHANGE_COORDINATES: return true;
            case ResourceKeys.COUPLING:           return getSeriesCount()!=0;
        }
    }

    /**
     * Exécute une action. Le code <code>clé</code> de l'action est le même
     * que celui qui avait été préalablement transmis à {@link #canProcess}.
     * Si le code de l'action n'est pas reconnue, une exception sera lancée.
     *
     * @throws SQLException si une interrogation de la base de données était
     *         nécessaire et a échouée.
     */
    protected Task process(final int clé) throws SQLException {
        final Component c=tabs.getSelectedComponent();
        if (c instanceof ImagePanel) {
            if (((ImagePanel) c).process(clé)) {
                return null;
            }
        }
        final Resources resources = Resources.getResources(getLocale());
        Task task=null;
        switch (clé) {
            default: {
                task = super.process(clé);
                break;
            }
            ///////////////////////////////////
            ///  Séries - Ajouter une série ///
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
            ///  Séries - Fermer la série ///
            /////////////////////////////////
            case ResourceKeys.CLOSE_SERIES: {
                removeTabAt(tabs.getSelectedIndex());
                break;
            }
            /////////////////////////////////////////
            ///  Séries - Changer les coordonnées ///
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
            ///  Fichier - Nouveau - Couplage pêche/environnement  ///
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
     * Cette modification n'affecte pas le fuseau horaire des éventuelles
     * bases de données accédées par cette fenêtre.
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
     * indique que tout zoom ou translation appliqué sur une image d'une
     * mosaïque doit être répliqué sur les autres.
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
     * Spécifie si les cartes doivent être redessinées
     * durant les glissements des ascenceurs. Spécifier
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
     * Préviens cette fenêtre que l'état d'une de ses fenêtre-fille
     * a changé. L'implémentation par défaut appele simplement
     * {@link #stateChanged()}.
     */
    public void stateChanged(final ChangeEvent event) {
        stateChanged();
    }

    /**
     * Retourne un objet qui peut être sauvegardé et lu en binaire. Cet objet
     * peut servir à enregistrer temporairement l'état du bureau ou a envoyer
     * une fenêtre par le réseau avec les RMI.
     */
    protected Task getSerializable() {
        return new Serializer(this);
    }

    /**
     * Libère les ressources utilisées par cette fenêtre.
     * Cette méthode est appelée automatiquement lorsque
     * la fenêtre est fermée.
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
     * Classe d'un objet capable de sauvegarder l'état de la fenêtre. Les instances
     * de cette classes peuvent être enregistrées en binaire (<i>Serialized</i>).
     * Elles peuvent ensuite être relues et reconstruire une copie de la fenêtre
     * par un appel à {@link #run()}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Serializer extends Task {
        /**
         * Tables des images à sauvegarder.
         */
        private final ImageTableModel[] models;

        /**
         * Construit un objet binaire pour la fenêtre spécifiée.
         * Aucune référence vers la fenêtre <code>frame</code>
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
                    // On obtient une copie du modèle plutôt que le modèle
                    // original afin de ne pas avoir les 'Listeners'.
                }
            }
            this.models = models = XArray.resize(models, count);
        }

        /**
         * Reconstruit une fenêtre.
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
