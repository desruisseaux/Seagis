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
package fr.ird.awt;

// Interface utilisateur
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
import net.seas.util.SwingUtilities;

// Entrés/sorties
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.event.IIOReadWarningListener;

// Images et progrès
import fr.ird.sql.image.ImageEntry;
import net.seagis.gc.GridCoverage;
import net.seas.awt.progress.Progress;
import net.seas.awt.progress.WindowProgress;

// Collections
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

// Divers
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import net.seas.awt.ExceptionMonitor;
import javax.swing.event.EventListenerList;
import net.seagis.resources.Utilities;


/**
 * Boîte de dialogue invitant l'utilisateur à sélectionner un répertoire
 * de destination et un format d'image.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/ExportChooser.png"></p>
 * <p>&nbsp;</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class ExportChooser extends JPanel
{
    /**
     * Objet à utiliser pour sélectionner
     * un répertoire de destination.
     */
    private final JFileChooser chooser;

    /**
     * Ensemble des images à écrire. L'ordre des éléments doit être préservés.
     *
     * TODO: Pas moyen de compiler avec le "generic compiler" (JSR-14).
     *       Ce con nous invente un "name clash" qui n'existe pas entre
     *       Container et Component.setFocusTraversalKeys  (méthode que
     *       nous n'utilisons pourtant pas!).
     */
    private final Set/*<ImageEntry>*/ entries=new LinkedHashSet/*<ImageEntry>*/(256);

    /**
     * Etiquette indiquant le nombre d'images à exporter.
     */
    private final JLabel count=new JLabel();

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
    public ExportChooser(final File directory)
    {
        super(new GridBagLayout());
        count.setOpaque(true);
        count.setBackground(Color.black);
        count.setForeground(Color.yellow);
        count.setHorizontalAlignment(SwingConstants.CENTER);
        ///
        /// Configure le paneau servant
        /// à choisir un répertoire.
        ///
        chooser=new JFileChooser(directory);
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle(resources.getString(ResourceKeys.OUT_DIRECTORY));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setControlButtonsAreShown(false);
        chooser.setPreferredSize(new Dimension(400,300)); // La dimension par défaut est (500,300).
        chooser.setAcceptAllFileFilterUsed(false);
        ///
        /// Ajoute les filtres de fichiers
        ///
        final ImageFileFilter[] fileFilters=ImageFileFilter.getWriterFilters(null);
        for (int i=0; i<fileFilters.length; i++)
            chooser.addChoosableFileFilter(fileFilters[i]);
        ///
        /// Construit le paneau d'options
        ///
        final JPanel options=new JPanel(new GridBagLayout());
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
     * Met à jour l'étiquette qui indique
     * le nombre d'images à exporter.
     */
    private void updateCount()
    {count.setText(resources.getString(ResourceKeys.IMAGES_TO_EXPORT_COUNT_$1, new Integer(entries.size())));}

    /**
     * Ajoute les entrées spécifiées à la liste des images à écrire. Les
     * images seront écrites dans l'ordre qu'elles apparaissent dans le
     * tableau <code>entries</code>. Toutefois, les doublons seront ignorés.
     */
    public synchronized void addEntries(final ImageEntry[] entries)
    {
        for (int i=0; i<entries.length; i++)
            this.entries.add(entries[i]);
        updateCount();
    }

    /**
     * Retire les entrées spécifiées de la liste des images à écrire.
     */
    public synchronized void removeEntries(final ImageEntry[] entries)
    {
        for (int i=entries.length; --i>=0;)
            this.entries.remove(entries[i]);
        updateCount();
    }

    /**
     * Retire toutes les entrées de la liste des images à écrire.
     */
    public synchronized void removeAllEntries()
    {
        entries.clear();
        updateCount();
    }

    /**
     * Retourne les entrées des images qui seront à écrire.
     */
    public synchronized ImageEntry[] getEntries()
    {return (ImageEntry[]) entries.toArray(new ImageEntry[entries.size()]);}

    /**
     * Retourne le répertoire dans lequel écrire les images. Ce répertoire a
     * été spécifiée lors de la construction de cet objet, mais peut avoir été
     * modifié par l'utilisateur.
     */
    public File getDestinationDirectory()
    {return chooser.getSelectedFile();}

    /**
     * Fait apparaître la boîte de dialogue. Si l'utilisateur n'a pas annulé
     * l'opération en cours de route, l'exportation des images sera lancée
     * dans un thread en arrière-plan. Cette méthode peut donc retourner pendant
     * que les exportations sont en cours. Les progrès seront affichées dans
     * une fenêtre.
     *
     * @param  owner Composante parente dans laquelle faire apparaître la
     *         boîte de dialogue, ou <code>null</code> s'il n'y en a pas.
     * @param  threadGroup Groupe de threads dans lequel placer celui qu'on
     *         va lancer.
     * @return <code>true</code> si l'utilisateur a lancé les exportations,
     *         ou <code>false</code> s'il a annulé l'opération.
     */
    public boolean showDialogAndStart(final Component owner, final ThreadGroup threadGroup)
    {
        while (SwingUtilities.showOptionDialog(owner, this, chooser.getDialogTitle()))
        {
            final Worker worker=new Worker(this);
            if (worker.getUserConfirmation(owner))
            {
                try
                {
                    worker.start(threadGroup, owner);
                    return true;
                }
                catch (IOException exception)
                {
                    ExceptionMonitor.show(owner, exception);
                }
            }
        }
        return false;
    }

    /**
     * Classe ayant la charge d'exporter les images en arrière plan.  Le constructeur de cette classe fait
     * une copie de tous les paramètres pertinents de {@link ExportChooser}, tels qu'ils étaient au moment
     * de la construction. Par la suite, aucune référence vers {@link ExportChooser} n'est conservée.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class Worker implements Runnable, IIOReadWarningListener
    {
        /**
         * Fenêtre dans laquelle écrire les progrès de l'opération.
         * Cette fenêtre ne sera créée que la première fois où elle
         * sera nécessaire.
         */
        private Progress progress;

        /**
         * Encodeur à utiliser pour écrire les images. Cet encodeur
         * ne sera créé que lorsque les écritures d'images démarreront.
         */
        private ImageWriter writer;

        /**
         * Entré en cours de lecture, ou <code>null</code>
         * s'il n'y en a pas encore.
         */
        private ImageEntry current;

        /**
         * Liste des images à écrire.
         */
        private final ImageEntry[] entries;

        /**
         * Répertoire de destination dans lequel
         * seront écrites les images.
         */
        private final File directory;

        /**
         * Extension des fichiers d'images. Cette extension remplacera l'extension des
         * fichiers d'images sources. La chaîne de caractères <code>extension</code>
         * ne doit commencer par un point.
         */
        private final String extension;

        /**
         * Objet qui avait la charge de filtrer les fichiers à afficher dans la
         * boîte de dialogue. Cet objet connaît le format choisit par l'utilisateur
         * et est capable de construire l'encodeur {@link ImageWriter} approprié.
         */
        private final ImageFileFilter filter;

        /**
         * Buffer temporaire. Ce buffer est utilisé pour construire
         * chacun des noms de fichier de destination des images.
         */
        private final StringBuffer buffer=new StringBuffer();

        /**
         * Construit un objet qui procèdera aux écritures des images en arrière plan.
         * Ce constructeur fera une copie des paramètres de la boîte de dialogue
         * {@link ExportChooser} spécifiée.
         *
         * @param chooser Boîte de dialogue qui demandait à l'utilisateur
         *        de choisir un répertoire de destination ainsi qu'un format.
         */
        public Worker(final ExportChooser chooser)
        {
            synchronized (chooser)
            {
                this.filter    = (ImageFileFilter) chooser.chooser.getFileFilter();
                this.entries   = chooser.getEntries();
                this.directory = chooser.getDestinationDirectory();
                this.extension =  filter.getExtension();
            }
        }

        /**
         * Retourne le nom et le chemin du fichier
         * de destination pour l'image spécifiée.
         */
        private File getDestinationFile(final int index)
        {
            final String filename=entries[index].getFile().getName();
            buffer.setLength(0);
            buffer.append(filename);
            final int extPos=filename.lastIndexOf('.');
            if (extPos>=0) buffer.setLength(extPos);
            if (extension.length()!=0)
            {
                buffer.append('.');
                buffer.append(extension);
            }
            return new File(directory, buffer.toString());
        }

        /**
         * Vérifie si les images peuvent être écrites dans le répertoire choisit. Cette méthode
         * vérifie d'abord si le répertoire est valide. Elle vérifie ensuite si le répertoire
         * contient déjà des images qui risquent d'être écrasées. Si c'est le cas, alors cette
         * méthode fait apparaître une boîte de dialogue qui demande à l'utilisateur de confirmer
         * les écrasements. Cette méthode devrait toujours être appelée avant de lancer les exportations
         * des fichiers.
         *
         * @param  owner Composante parente dans laquelle faire apparaître les éventuelles boîtes de dialogue.
         * @return <code>true</code> si on peut procéder aux écritures des images, ou <code>false</code> si
         *         l'utilisateur a demandé à arrêter l'opération.
         */
        public boolean getUserConfirmation(final Component owner)
        {
            final Resources resources = Resources.getResources(owner.getLocale());
            if (directory==null || !directory.isDirectory())
            {
                SwingUtilities.showMessageDialog(owner, resources.getString(ResourceKeys.ERROR_BAD_DIRECTORY),
                                 resources.getString(ResourceKeys.ERROR_BAD_ENTRY), JOptionPane.ERROR_MESSAGE);
                return false;
            }
            int existing=0;
            for (int i=0; i<entries.length; i++)
            {
                if (getDestinationFile(i).exists())
                    existing++;
            }
            if (existing!=0)
            {
                if (!SwingUtilities.showConfirmDialog(owner, resources.getString(ResourceKeys.CONFIRM_OVERWRITE_ALL_$2, new Integer(existing),
                                    new Integer(entries.length)), resources.getString(ResourceKeys.CONFIRM_OVERWRITE), JOptionPane.WARNING_MESSAGE))
                {
                    return false;
                }
            }
            return true;
        }

        /**
         * Démarre les exportations d'images. Cette méthode fait apparaître une fenêtre
         * dans laquelle seront affichées les progrès de l'opération. Elle appèle ensuite
         * {@link #run} dans un thread séparé, afin de faire les écritures en arrière plan.
         * <strong>Plus aucune autre méthode de <code>Worker</code> ne devrait être appelée
         * après <code>start</code>.</strong>
         *
         * @param  threadGroup Groupe de threads dans lequel placer celui qu'on va lancer.
         * @param  owner Composante parente dans laquelle faire apparaître la fenêtre des progrès.
         * @throws IOException si une erreur a empêché le démarrage des exportations.
         */
        public void start(final ThreadGroup threadGroup, final Component owner) throws IOException
        {
            final Resources resources = Resources.getResources(owner.getLocale());
            writer   = filter.getImageWriter();
            progress = new WindowProgress(owner);
            final Thread thread=new Thread(threadGroup, this, resources.getString(ResourceKeys.EXPORT));
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        /**
         * Procède aux exportations d'images. Si une erreur survient en cours de route,
         * un avertissement sera écrit dans la fenêtre des progrès. N'appelez pas cette
         * méthode directement. Appelez plutôt {@link #start}, qui se chargera d'appeller
         * <code>run()</code> dans un thread en arrière-plan.
         */
        public void run()
        {
            final EventListenerList listeners = new EventListenerList();
            listeners.add(IIOReadWarningListener.class, this);

            progress.started();
            for (int i=0; i<entries.length; i++)
            {
                final ImageEntry entry=entries[i];
                progress.setDescription(Resources.format(ResourceKeys.EXPORTING_$1, entry.getName()));
                progress.progress(((float)(i*100))/entries.length);
                try
                {
                    final GridCoverage image = entry.getGridCoverage(listeners);
                    final ImageOutputStream output=ImageIO.createImageOutputStream(getDestinationFile(i));
                    writer.setOutput(output);
                    writer.write(image.getRenderedImage(false));
                    output.close();
                }
                catch (Exception exception)
                {
                    String message=exception.getLocalizedMessage();
                    if (message==null) message=Utilities.getShortClassName(exception);
                    progress.warningOccurred(entry.getName(), null, message);
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
        public void warningOccurred(final ImageReader source, final String warning)
        {
            final Progress progress=this.progress;
            if (progress!=null)
            {
                final ImageEntry entry=current;
                progress.warningOccurred((entry!=null) ? entry.getName() : null, null, warning);
            }
        }
    }
}
