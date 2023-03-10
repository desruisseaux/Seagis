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
package fr.ird.database.gui.swing;

// Interface utilisateur
import java.rmi.RemoteException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;
import javax.swing.event.EventListenerList;

// Formattage
import java.util.Date;
import java.util.Locale;

// Entrés/sorties
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.event.IIOReadWarningListener;

// Collections
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

// Geotools
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.TemporalCoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.gc.GridCoverage;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.gui.swing.ProgressWindow;
import org.geotools.gui.swing.ExceptionMonitor;
import org.geotools.io.coverage.PropertyParser;
import org.geotools.util.ProgressListener;
import org.geotools.resources.Utilities;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.SwingUtilities;

// Seagis
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;


/**
 * Boîte de dialogue invitant l'utilisateur ŕ sélectionner un répertoire
 * de destination et un format d'image.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/CoverageExportChooser.png"></p>
 * <p>&nbsp;</p>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class CoverageExportChooser extends JPanel {
    /**
     * Objet ŕ utiliser pour sélectionner un répertoire de destination.
     */
    private final JFileChooser chooser;

    /**
     * Ensemble des images ŕ écrire. L'ordre des éléments doit ętre préservés.
     */
    private final Set<CoverageEntry> entries = new LinkedHashSet<CoverageEntry>(256);

    /**
     * Etiquette indiquant le nombre d'images ŕ exporter.
     */
    private final JLabel count = new JLabel();

    /**
     * Resources pour la construction des étiquettes.
     */
    private final Resources resources = Resources.getResources(getLocale());

    /**
     * Construit une boîte de dialogue.
     *
     * @param directory Répertoire de destination par défaut, ou <code>null</code>
     *                  pour utiliser le répertoire du compte de l'utilisateur.
     */
    public CoverageExportChooser(final File directory) {
        super(new GridBagLayout());
        count.setOpaque(true);
        count.setBackground(Color.BLACK);
        count.setForeground(Color.YELLOW);
        count.setHorizontalAlignment(SwingConstants.CENTER);
        ///
        /// Configure le paneau servant ŕ choisir un répertoire.
        ///
        chooser = new JFileChooser(directory);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle(resources.getString(ResourceKeys.OUT_DIRECTORY));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setControlButtonsAreShown(false);
        chooser.setAcceptAllFileFilterUsed(false);
        ///
        /// Ajoute les filtres de fichiers
        ///
        final ImageFileFilter[] fileFilters = ImageFileFilter.getWriterFilters(null);
        for (int i=0; i<fileFilters.length; i++) {
            chooser.addChoosableFileFilter(fileFilters[i]);
        }
        ///
        /// Construit le paneau d'options
        ///
        final JPanel options = new JPanel(new GridBagLayout());
        final GridBagConstraints c=new GridBagConstraints();
        options.setBorder(BorderFactory.createCompoundBorder(
                          BorderFactory.createTitledBorder(resources.getString(ResourceKeys.OPTIONS)),
                          BorderFactory.createEmptyBorder(/*top*/6,/*left*/9,/*bottom*/6,/*right*/9)));
        c.gridx=0; c.fill=c.BOTH; c.insets.right=6;
        c.gridy=0; options.add(new JLabel(resources.getString(ResourceKeys.NOT_AVAILABLE)), c);
        ///
        /// Place les composantes
        ///
        c.gridx=0; c.weightx=1;
        c.gridy=0;              c.insets.top= 6; add(count,   c);
        c.gridy=1; c.weighty=1; c.insets.top=15; add(chooser, c);
        c.gridy=2; c.weighty=0; c.insets.top=12; add(options, c);
        updateCount();
    }

    /**
     * Met ŕ jour l'étiquette qui indique le nombre d'images ŕ exporter.
     */
    private void updateCount() {
        count.setText(resources.getString(ResourceKeys.COVERAGES_TO_EXPORT_COUNT_$1,
                                          new Integer(entries.size())));
    }

    /**
     * Ajoute les entrées spécifiées ŕ la liste des images ŕ écrire. Les
     * images seront écrites dans l'ordre qu'elles apparaissent dans le
     * tableau <code>entries</code>. Toutefois, les doublons seront ignorés.
     */
    public synchronized void addEntries(final CoverageEntry[] entries)  {
        for (int i=0; i<entries.length; i++) {
            this.entries.add(entries[i]);
        }
        updateCount();
    }

    /**
     * Retire les entrées spécifiées de la liste des images ŕ écrire.
     */
    public synchronized void removeEntries(final CoverageEntry[] entries) {
        for (int i=entries.length; --i>=0;) {
            this.entries.remove(entries[i]);
        }
        updateCount();
    }

    /**
     * Retire toutes les entrées de la liste des images ŕ écrire.
     */
    public synchronized void removeAllEntries() {
        entries.clear();
        updateCount();
    }

    /**
     * Retourne les entrées des images qui seront ŕ écrire.
     */
    public synchronized CoverageEntry[] getEntries() {
        return entries.toArray(new CoverageEntry[entries.size()]);
    }

    /**
     * Retourne le répertoire dans lequel écrire les images. Ce répertoire a
     * été spécifiée lors de la construction de cet objet, mais peut avoir été
     * modifié par l'utilisateur.
     */
    public File getDestinationDirectory() {
        return chooser.getSelectedFile();
    }

    /**
     * Fait apparaître la boîte de dialogue. Si l'utilisateur n'a pas annulé
     * l'opération en cours de route, l'exportation des images sera lancée
     * dans un thread en arričre-plan. Cette méthode peut donc retourner pendant
     * que les exportations sont en cours. Les progrčs seront affichées dans
     * une fenętre.
     *
     * @param  owner Composante parente dans laquelle faire apparaître la
     *         boîte de dialogue, ou <code>null</code> s'il n'y en a pas.
     * @param  threadGroup Groupe de threads dans lequel placer celui qu'on
     *         va lancer.
     * @return <code>true</code> si l'utilisateur a lancé les exportations,
     *         ou <code>false</code> s'il a annulé l'opération.
     */
    public boolean showDialogAndStart(final Component owner, final ThreadGroup threadGroup) {
        while (SwingUtilities.showOptionDialog(owner, this, chooser.getDialogTitle())) {
            final Worker worker=new Worker(this);
            if (worker.getUserConfirmation(owner)) {
                try {
                    worker.start(threadGroup, owner);
                    return true;
                } catch (IOException exception) {
                    ExceptionMonitor.show(owner, exception);
                }
            }
        }
        return false;
    }

    /**
     * Classe ayant la charge d'exporter les images en arričre plan.  Le constructeur de cette
     * classe fait une copie de tous les paramčtres pertinents de {@link CoverageExportChooser},
     * tels qu'ils étaient au moment de la construction. Par la suite, aucune référence vers
     * {@link CoverageExportChooser} n'est conservée.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class Worker implements Runnable, IIOReadWarningListener {
        /**
         * Fenętre dans laquelle écrire les progrčs de l'opération.
         * Cette fenętre ne sera créée que la premičre fois oů elle
         * sera nécessaire.
         */
        private ProgressListener progress;

        /**
         * Encodeur ŕ utiliser pour écrire les images. Cet encodeur
         * ne sera créé que lorsque les écritures d'images démarreront.
         */
        private ImageWriter writer;

        /**
         * Entré en cours de lecture, ou <code>null</code>
         * s'il n'y en a pas encore.
         */
        private CoverageEntry current;

        /**
         * Liste des images ŕ écrire.
         */
        private final CoverageEntry[] entries;

        /**
         * Répertoire de destination dans lequel
         * seront écrites les images.
         */
        private final File directory;

        /**
         * Extension des fichiers d'images. Cette extension remplacera l'extension des
         * fichiers d'images sources. La chaîne de caractčres <code>extension</code>
         * ne doit pas commencer par un point. Ce champ peut ętre <code>null</code>
         * s'il n'y a pas d'extension connue pour le type de fichier ŕ écrire.
         */
        private final String extension;

        /**
         * Objet qui avait la charge de filtrer les fichiers ŕ afficher dans la
         * boîte de dialogue. Cet objet connaît le format choisit par l'utilisateur
         * et est capable de construire l'encodeur {@link ImageWriter} approprié.
         */
        private final ImageFileFilter filter;

        /**
         * Buffer temporaire. Ce buffer est utilisé pour construire
         * chacun des noms de fichier de destination des images.
         */
        private final StringBuilder buffer = new StringBuilder();

        /**
         * Objet {@link PropertyParser} ŕ utiliser pour écrire les propriétés d'un
         * {@link GridCoverage}.
         */
        private transient PropertyParser propertyParser;

        /**
         * Construit un objet qui procčdera aux écritures des images en arričre plan.
         * Ce constructeur fera une copie des paramčtres de la boîte de dialogue
         * {@link CoverageExportChooser} spécifiée.
         *
         * @param chooser Boîte de dialogue qui demandait ŕ l'utilisateur
         *        de choisir un répertoire de destination ainsi qu'un format.
         */
        public Worker(final CoverageExportChooser chooser) {
            synchronized (chooser) {
                this.filter    = (ImageFileFilter) chooser.chooser.getFileFilter();
                this.entries   = chooser.getEntries();
                this.directory = chooser.getDestinationDirectory();
                this.extension =  filter.getExtension();
            }
        }

        /**
         * Retourne le nom et le chemin du fichier de destination pour l'image spécifiée.
         */
        private File getDestinationFile(final int index) throws RemoteException {
            return getDestinationFile(index, extension);
        }

        /**
         * Retourne le nom et le chemin du fichier de destination pour l'image spécifiée.
         */
        private File getDestinationFile(final int index, final String extension)
                throws RemoteException
        {
            File file = entries[index].getFile();
            if (file == null) {
                file = new File(entries[index].getURL().getPath());
            }
            final String filename = file.getName();
            buffer.setLength(0);
            buffer.append(filename);
            final int extPos = filename.lastIndexOf('.');
            if (extPos >= 0) {
                buffer.setLength(extPos);
            }
            if (extension!=null && extension.length() != 0) {
                buffer.append('.');
                buffer.append(extension);
            }
            return new File(directory, buffer.toString());
        }

        /**
         * Vérifie si les images peuvent ętre écrites dans le répertoire choisi. Cette méthode
         * vérifie d'abord si le répertoire est valide. Elle vérifie ensuite si le répertoire
         * contient déjŕ des images qui risquent d'ętre écrasées. Si c'est le cas, alors cette
         * méthode fait apparaître une boîte de dialogue qui demande ŕ l'utilisateur de confirmer
         * les écrasements. Cette méthode devrait toujours ętre appelée avant de lancer les exportations
         * des fichiers.
         *
         * @param  owner Composante parente dans laquelle faire apparaître les éventuelles boîtes de dialogue.
         * @return <code>true</code> si on peut procéder aux écritures des images, ou <code>false</code> si
         *         l'utilisateur a demandé ŕ arręter l'opération.
         */
        public boolean getUserConfirmation(final Component owner) {
            final Resources resources = Resources.getResources(owner.getLocale());
            if (directory==null || !directory.isDirectory()) {
                SwingUtilities.showMessageDialog(owner, resources.getString(ResourceKeys.ERROR_BAD_DIRECTORY),
                                 resources.getString(ResourceKeys.ERROR_BAD_ENTRY), JOptionPane.ERROR_MESSAGE);
                return false;
            }
            int existing = 0;
            for (int i=0; i<entries.length; i++) {
                try {
                    if (getDestinationFile(i).exists()) {
                        existing++;
                    }
                } catch (RemoteException exception) {
                    // Ignore. This file will not be counted.
                }
            }
            if (existing != 0) {
                if (!SwingUtilities.showConfirmDialog(owner, resources.getString(ResourceKeys.CONFIRM_OVERWRITE_ALL_$2, new Integer(existing),
                                    new Integer(entries.length)), resources.getString(ResourceKeys.CONFIRM_OVERWRITE), JOptionPane.WARNING_MESSAGE))
                {
                    return false;
                }
            }
            return true;
        }

        /**
         * Démarre les exportations d'images. Cette méthode fait apparaître une fenętre
         * dans laquelle seront affichées les progrčs de l'opération. Elle appčle ensuite
         * {@link #run} dans un thread séparé, afin de faire les écritures en arričre plan.
         * <strong>Plus aucune autre méthode de <code>Worker</code> ne devrait ętre appelée
         * aprčs <code>start</code>.</strong>
         *
         * @param  threadGroup Groupe de threads dans lequel placer celui qu'on va lancer.
         * @param  owner Composante parente dans laquelle faire apparaître la fenętre des progrčs.
         * @throws IOException si une erreur a empęché le démarrage des exportations.
         */
        public void start(final ThreadGroup threadGroup, final Component owner) throws IOException {
            final Resources resources = Resources.getResources(owner.getLocale());
            writer   = filter.getImageWriter();
            progress = new ProgressWindow(owner);
            final Thread thread=new Thread(threadGroup, this, resources.getString(ResourceKeys.EXPORT));
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        /**
         * Procčde aux exportations d'images. Si une erreur survient en cours de route,
         * un avertissement sera écrit dans la fenętre des progrčs. N'appelez pas cette
         * méthode directement. Appelez plutôt {@link #start}, qui se chargera d'appeller
         * <code>run()</code> dans un thread en arričre-plan.
         */
        public void run() {
            final EventListenerList listeners = new EventListenerList();
            listeners.add(IIOReadWarningListener.class, this);
            progress.started();
            final GridCoverageProcessor processor = GridCoverageProcessor.getDefault();
            for (int i=0; i<entries.length; i++) {
                final CoverageEntry entry = entries[i];
                String name = "";
                try {
                    name = entry.getName();
                    progress.setDescription(Resources.format(ResourceKeys.EXPORTING_$1, name));
                    progress.progress((i*100f)/entries.length);
                    GridCoverage image = entry.getGridCoverage(listeners).geophysics(false);
                    CoordinateSystem sourceCS = image.getCoordinateSystem();
                    CoordinateSystem targetCS = GeographicCoordinateSystem.WGS84;
                    final int sourceDim = sourceCS.getDimension();
                    final int targetDim = targetCS.getDimension();
                    if (sourceDim > targetDim) {
                        final CoordinateSystem tailCS = CTSUtilities.getSubCoordinateSystem(
                                                        sourceCS, targetDim, sourceDim);
                        if (tailCS != null) {
                            targetCS = new CompoundCoordinateSystem(
                                           targetCS.getName().getCode(), targetCS, tailCS);
                        }
                    }
                    image = processor.doOperation("Resample", image, "CoordinateSystem", targetCS);
                    final ImageOutputStream output = ImageIO.createImageOutputStream(getDestinationFile(i));
                    writer.setOutput(output);
                    writer.write(image.getRenderedImage());
                    output.close();
                    writeProperties(image, getDestinationFile(i, "txt"));
                } catch (Exception exception) {
                    String message = exception.getLocalizedMessage();
                    if (message == null) {
                        message = Utilities.getShortClassName(exception);
                    }
                    progress.warningOccurred(name, null, message);
                }
                writer.reset();
            }
            progress.complete();
            writer.dispose();
        }
        
        /**
         * Méthode appelée automatiquement lorsqu'un avertissement
         * est survenu pendant la lecture d'une image.
         */
        public void warningOccurred(final ImageReader source, final String warning) {
            String name = null;
            final ProgressListener progress = this.progress;
            if (progress != null) {
                final CoverageEntry entry = current;
                if (entry != null) try {
                    name = entry.getName();
                } catch (RemoteException e) {
                    name = "<error>";
                }
                progress.warningOccurred(name, null, warning);
            }
        }

        /**
         * Ecrit les propriétés de l'image spécifiée. L'implémentation par défaut écrit les coordonnées
         * géographiques des quatres coins de l'image, sa taille, nombre de bandes, etc.
         *
         * @param coverage L'image pour laquelle écrire les propriétés.
         * @param file Le fichier de destination. Ca sera généralement un fichier avec l'extension
         *             <code>".txt"</code>.
         */
        protected void writeProperties(final GridCoverage coverage, final File file) throws IOException {
            if (propertyParser == null) {
                propertyParser = new PropertyParser();
                propertyParser.setFormatPattern(Date.class, "yyyy/MM/dd HH:mm zz");
                propertyParser.setFormatPattern(Number.class, "#0.######");
                propertyParser.addAlias(PropertyParser.Z_MINIMUM,    "Date de début");
                propertyParser.addAlias(PropertyParser.Z_MAXIMUM,    "Date de fin");
                propertyParser.addAlias(PropertyParser.PROJECTION,   "Projection");
                propertyParser.addAlias(PropertyParser.ELLIPSOID,    "Ellipsoďde");
                propertyParser.addAlias(PropertyParser.Y_MAXIMUM,    "Limite Nord");
                propertyParser.addAlias(PropertyParser.Y_MINIMUM,    "Limite Sud");
                propertyParser.addAlias(PropertyParser.X_MAXIMUM,    "Limite Est");
                propertyParser.addAlias(PropertyParser.X_MINIMUM,    "Limite Ouest");
                propertyParser.addAlias(PropertyParser.Y_RESOLUTION, "Résolution en latitude");
                propertyParser.addAlias(PropertyParser.X_RESOLUTION, "Résolution en longitude");
                propertyParser.addAlias(PropertyParser.WIDTH,        "Largeur (en pixels)");
                propertyParser.addAlias(PropertyParser.HEIGHT,       "Hauteur (en pixels)");
            }
            final Locale locale        = Locale.getDefault();
            final String lineSeparator = System.getProperty("line.separator", "\n");
            final Writer out           = new BufferedWriter(new FileWriter(file));
            out.write("#"); out.write(lineSeparator);
            out.write("# Description du format de l'image \"");
            out.write(coverage.getName(locale));
            out.write('"');
            out.write(lineSeparator);
            out.write("#"); out.write(lineSeparator);
            out.write(lineSeparator);
            propertyParser.clear();
            propertyParser.add(coverage);
            propertyParser.listProperties(out);
            propertyParser.clear();
            out.close();
        }
    }
}
