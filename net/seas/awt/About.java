/*
 * SEAS - Surveillance de l'Environnement Assistée par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assistée par Satellite
 *             Institut de Recherche pour le Développement
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package net.seas.awt;

// User interface
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.event.ListDataListener;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.Icon;
import javax.swing.Box;

// Input/output
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

// Manifest
import java.util.jar.Manifest;
import java.util.jar.Attributes;

// Formatting
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;

// Miscellaneous
import java.util.Date;
import java.util.Locale;
import java.util.Arrays;
import java.util.Iterator;
import net.seas.util.XArray;
import net.seas.resources.Resources;
import net.seas.resources.ResourceKeys;

// Geotools dependencies
import org.geotools.resources.Utilities;
import org.geotools.resources.SwingUtilities;


/**
 * A "About" dialog box. This dialog box contains the application's title and some system
 * informations  (Java and OS version,  free memory,  image readers and writers,  running
 * threads, etc.). Those informations are fetched from a {@link Manifest} object, usually
 * build from the <code>META-INF/Manifest.mf</code> file.   This manifest should contains
 * entries for <code>Implementation-Title</code>, <code>Implementation-Version</code> and
 * <code>Implementation-Vendor</code> values, as suggested in the
 * <A HREF="http://java.sun.com/docs/books/tutorial/jar/basics/manifest.html#versioning">Java tutorial</A>.
 * In addition to the above-cited standard entries,   the <code>About</code> class also
 * understand the <code>Compilation-Date</code> entry. This entry can contains the date
 * (using the <code>"yyyy-MM-dd HH:mm:ss"</code> pattern) the package was compiled. If presents,
 * this date will be localized according user's locale and appended to the version number.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class About extends JPanel
{
    /**
     * Unité à utiliser pour les affichages
     * de la quantité de mémoire utilisée.
     */
    private static final float MEMORY_UNIT = (1024f*1024f);

    /**
     * The entry for timestamp in the manifest file.
     */
    private static final String TIMESTAMP = "Compilation-Date";

    /**
     * Thread qui aura la charge de faire des mises à jour en arrière-plan.
     * Ce champ sera <code>null</code> s'il n'y en a pas.
     */
    private final ThreadList updater;

    /**
     * The localized resources to use.
     */
    private final Resources resources = Resources.getResources(null);

    /**
     * Construct a new dialog box for the specified application class.  This constructor
     * use the class loader for loading the manifest file. It also use the class package
     * to find the right entry into the manifest.
     *
     * @param logo        The application's logo. It may be a {@link JComponent},
     *                    an {@link Icon} object or a {@link String} representing
     *                    the pathname to an image.
     * @param application The application's class. Application name will be fetch
     *                    from the manifest file (<code>META-INF/Manifest.mf</code>).
     * @param tasks       Group of running threads, or <code>null</code> if there is none.
     */
    public About(final Object logo, final Class application, final ThreadGroup tasks)
    {this(logo, getAttributes(application), application.getClassLoader(), tasks);}

    /**
     * Construct a new dialog box from the specified manifest attributes.
     *
     * @param logo        The application's logo. It may be a {@link JComponent},
     *                    an {@link Icon} object or a {@link String} representing
     *                    the pathname to an image.
     * @param attributes  The manifest attributes containing application name and version number.
     * @param tasks       Group of running threads, or <code>null</code> if there is none.
     */
    public About(final Object logo, final Attributes attributes, final ThreadGroup tasks)
    {this(logo, attributes, null, tasks);}

    /**
     * Construct a new dialog box.
     *
     * @param logo        The application's logo. It may be a {@link JComponent},
     *                    an {@link Icon} object or a {@link String} representing
     *                    the pathname to an image.
     * @param attributes  The manifest attributes containing application name and version number.
     * @param loader      The application's class loader.
     * @param tasks       Group of running threads, or <code>null</code> if there is none.
     */
    private About(final Object logo, final Attributes attributes, ClassLoader loader, final ThreadGroup tasks)
    {
        super(new BorderLayout());
        if (loader==null)
        {
            loader = getClass().getClassLoader();
            // TODO: it would be nice to fetch the caller's class loader instead
        }
        /*
         * Get the free memory before any futher work.
         */
        final Runtime    system = Runtime.getRuntime(); system.gc();
        final float  freeMemory = system.freeMemory()  / MEMORY_UNIT;
        final float totalMemory = system.totalMemory() / MEMORY_UNIT;
        /*
         * Get application's name, version and vendor from the manifest attributes.
         * If an implementation date is specified, append it to the version string.
         */
        String application = attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
        String version     = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        String vendor      = attributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
        try
        {
            final String dateString = attributes.getValue(TIMESTAMP);
            if (dateString!=null)
            {
                final Date         date = getDateFormat().parse(dateString);
                final DateFormat format = DateFormat.getDateInstance(DateFormat.LONG);
                if (version!=null && version.trim().length()!=0)
                {
                    StringBuffer buffer = new StringBuffer(version);
                    buffer.append(" (");
                    buffer=format.format(date, buffer, new FieldPosition(0));
                    buffer.append(')');
                    version = buffer.toString();
                }
                else version = format.format(date);
            }
        }
        catch (ParseException exception)
        {
            Utilities.unexpectedException("net.seas.awt", "About", "<init>", exception);
        }
        /*
         * If the user supplied a logo, load it and display it in the dialog's upper part (NORTH).
         * The tabbed pane will be added below the logo, in the dialog's central part (CENTER).
         */
        if (logo!=null)
        {
            final JComponent title;
            if (logo instanceof JComponent)
            {
                title = (JComponent) logo;
            }
            else if (logo instanceof Icon)
            {
                title = new JLabel((Icon) logo);
            }
            else
            {
                final String text = String.valueOf(logo);
                final URL url = loader.getResource(text);
                if (url==null)
                {
                    final JLabel label = new JLabel(text);
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setBorder(BorderFactory.createEmptyBorder(6/*top*/, 6/*left*/, 6/*bottom*/, 6/*right*/));
                    title = label;
                }
                else title = new JLabel(new ImageIcon(url));
            }
            title.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(0/*top*/, 0/*left*/, 6/*bottom*/, 0/*right*/),
                            BorderFactory.createCompoundBorder(
                            BorderFactory.createLoweredBevelBorder(), title.getBorder())));
            add(title, BorderLayout.NORTH);
        }
        final JTabbedPane        tabs = new JTabbedPane();
        final JLabel totalMemoryLabel = new JLabel(resources.getString(ResourceKeys.TOTAL_MEMORY_$1, new Float(totalMemory)));
        final JLabel percentUsedLabel = new JLabel(resources.getString(ResourceKeys.MEMORY_USE_$1,   new Float(1-freeMemory/totalMemory)));
        add(tabs, BorderLayout.CENTER);
        /*
         * MAIN TAB (Application name and version informations)
         */
        if (true)
        {
            final JPanel pane = new JPanel(new GridBagLayout());
            final GridBagConstraints c=new GridBagConstraints();
            c.gridx=0; c.weightx=1;
            c.gridy=0;                  pane.add(new JLabel(                                                  application), c);
            c.gridy++;                  pane.add(new JLabel(resources.getString(ResourceKeys.VERSION_$1,      version)   ), c);
            c.gridy++;                  pane.add(new JLabel(                                                  vendor     ), c);
            c.gridy++; c.insets.top= 6; pane.add(new JLabel(resources.getString(ResourceKeys.JAVA_VERSION_$1, System.getProperty("java.version"))), c);
            c.gridy++; c.insets.top= 0; pane.add(new JLabel(resources.getString(ResourceKeys.JAVA_VENDOR_$1,  System.getProperty("java.vendor" ))), c);
            c.gridy++; c.insets.top= 6; pane.add(new JLabel(resources.getString(ResourceKeys.OS_NAME_$1,      System.getProperty("os.name"     ))), c);
            c.gridy++; c.insets.top= 0; pane.add(new JLabel(resources.getString(ResourceKeys.OS_VERSION_$2,   System.getProperty("os.version"),
                                                                                                              System.getProperty("os.arch"     ))), c);
            c.gridy++; c.insets.top=12; pane.add(totalMemoryLabel, c);
            c.gridy++; c.insets.top= 0; pane.add(percentUsedLabel, c);
            tabs.addTab(resources.getString(ResourceKeys.SYSTEM), pane);
        }
        /*
         * RUNNING TASKS TAB
         */
        if (tasks!=null)
        {
            final JPanel pane = new JPanel(new BorderLayout());
            final JList  list = new JList(updater=new ThreadList(tasks, totalMemoryLabel, percentUsedLabel, resources));
            pane.add(new JLabel(resources.getString(ResourceKeys.RUNNING_TASKS)), BorderLayout.NORTH);
            pane.add(new JScrollPane(list), BorderLayout.CENTER);
            tabs.addTab(resources.getString(ResourceKeys.TASKS), pane);
        }
        else updater=null;
        /*
         * IMAGE ENCODERS/DECODERS TAB
         */
        final JPanel   pane = new JPanel(new GridLayout(1,2,3,3));
        final String[] readers = ImageIO.getReaderMIMETypes();
        final String[] writers = ImageIO.getWriterMIMETypes();
        Arrays.sort(readers);
        Arrays.sort(writers);
        Box c;
        c=Box.createVerticalBox(); c.add(Box.createVerticalStrut(3)); c.add(new JLabel(resources.getString(ResourceKeys.DECODERS), JLabel.CENTER)); c.add(Box.createVerticalStrut(3)); c.add(new JScrollPane(new JList(readers))); pane.add(c);
        c=Box.createVerticalBox(); c.add(Box.createVerticalStrut(3)); c.add(new JLabel(resources.getString(ResourceKeys.ENCODERS), JLabel.CENTER)); c.add(Box.createVerticalStrut(3)); c.add(new JScrollPane(new JList(writers))); pane.add(c);
        tabs.addTab(resources.getString(ResourceKeys.IMAGES), pane);
    }

    /**
     * Returns attribute for the specified class.
     */
    private static Attributes getAttributes(final Class classe)
    {
        final InputStream stream = classe.getClassLoader().getResourceAsStream("META-INF/Manifest.mf");
        if (stream!=null) try
        {
            final Manifest manifest = new Manifest(stream);
            stream.close();
            String name = classe.getName().replace('.','/');
            int index; while ((index=name.lastIndexOf('/'))>=0)
            {
                final Attributes attributes = manifest.getAttributes(name.substring(0, index+1));
                if (attributes!=null) return attributes;
                name = name.substring(0, index);
            }
            return manifest.getMainAttributes();
        }
        catch (IOException exception)
        {
            Utilities.unexpectedException("net.seas.awt", "About", "getAttributes", exception);
        }
        // Use empty manifest attributes.
        return new Attributes();
    }

    /**
     * Returns a neutral date format for timestamp.
     */
    private static DateFormat getDateFormat()
    {return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA);}

    /**
     * Modèle représentant la liste des processus actif dans un {@link ThreadGroup}.
     * Cette liste se mettre automatiquement à jour de façon périodique.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class ThreadList extends AbstractListModel implements Runnable
    {
        /**
         * Processus qui met à jour <code>ThreadList</code>,
         * ou <code>null</code> s'il n'y en a pas. On peut
         * tuer le processus actif en donnant la valeur
         * <code>null</code> à cette variable.
         */
        public transient Thread worker;

        /**
         * Liste des processus en cours.
         */
        private final ThreadGroup tasks;

        /**
         * Liste des noms des processus en cours. Cette
         * liste sera mises à jour périodiquement.
         */
        private String[] names=new String[0];

        /**
         * Texte dans lequel écrire la mémoire totale réservée.
         */
        private final JLabel totalMemory;

        /**
         * Texte dans lequel écrire le pourcentage de mémoire utilisée.
         */
        private final JLabel percentUsed;

        /**
         * The localized resources to use.
         */
        private final Resources resources;

        /**
         * Construit une liste des processus actifs
         * dans le groupe <code>tasks</code> spécifié.
         */
        public ThreadList(final ThreadGroup tasks, final JLabel totalMemory, final JLabel percentUsed, final Resources resources)
        {
            this.tasks       = tasks;
            this.totalMemory = totalMemory;
            this.percentUsed = percentUsed;
            this.resources   = resources;
        }

        /**
         * Retourne le nombre d'éléments dans la liste.
         */
        public int getSize() // NO synchronized here
        {return names.length;}

        /**
         * Retourne un des éléments de la liste.
         */
        public Object getElementAt(final int index) // NO synchronized here
        {return names[index];}

        /**
         * Ajoute un objet à la liste des objets intéressés
         * à être informé des changements apportés à la liste.
         */
        public synchronized void addListDataListener(final ListDataListener listener)
        {super.addListDataListener(listener);}

        /**
         * Démarre le thread.
         */
        public synchronized void start()
        {
            if (worker==null)
            {
                worker=new Thread(this, Resources.format(ResourceKeys.ABOUT));
                worker.setPriority(Thread.MIN_PRIORITY);
                worker.setDaemon(true);
                worker.start();
            }
        }

        /**
         * Met à jour le contenu de la liste à interval régulier.
         * Cette méthode est exécutée dans une boucle jusqu'à ce
         * qu'elle soit interrompue en donnant la valeur nulle à
         * {@link #tasks}.
         */
        public synchronized void run()
        {
            String oldTotalMemory = null;
            String oldPercentUsed = null;
            while (worker==Thread.currentThread() && listenerList.getListenerCount()!=0)
            {
                final Runtime     system = Runtime.getRuntime();
                final float  freeMemoryN = system.freeMemory()  / MEMORY_UNIT;
                final float totalMemoryN = system.totalMemory() / MEMORY_UNIT;
                String   totalMemoryText = resources.getString(ResourceKeys.TOTAL_MEMORY_$1, new Float(totalMemoryN));
                String   percentUsedText = resources.getString(ResourceKeys.MEMORY_USE_$1,   new Float(1-freeMemoryN/totalMemoryN));

                Thread[] threadArray = new Thread[tasks.activeCount()];
                String[] threadNames = new String[tasks.enumerate(threadArray)];
                int c=0; for (int i=0; i<threadNames.length; i++)
                    if (threadArray[i]!=worker)
                        threadNames[c++]=threadArray[i].getName();
                threadNames = XArray.resize(threadNames, c);

                if (Arrays.equals(names, threadNames))      threadNames    =null;
                if (totalMemoryText.equals(oldTotalMemory)) totalMemoryText=null; else oldTotalMemory=totalMemoryText;
                if (percentUsedText.equals(oldPercentUsed)) percentUsedText=null; else oldPercentUsed=percentUsedText;
                if (threadNames!=null || totalMemoryText!=null || percentUsedText!=null)
                {
                    final String[]     names = threadNames;
                    final String totalMemory = totalMemoryText;
                    final String percentUsed = percentUsedText;
                    EventQueue.invokeLater(new Runnable()
                    {
                        public void run()
                        {update(names, totalMemory, percentUsed);}
                    });
                }
                try
                {
                    wait(4000);
                }
                catch (InterruptedException exception)
                {
                    // Quelqu'un a réveillé ce thread.
                    // Retourne donc au travail.
                }
            }
            worker=null;
        }

        /**
         * Met à jour le contenu de la liste. Cette méthode
         * est appelée périodiquement dans le thread de Swing.
         */
        private synchronized void update(final String[] newNames, final String totalMemory, final String percentUsed)
        {
            if (newNames!=null)
            {
                final int count = Math.max(names.length, newNames.length);
                names = newNames;
                fireContentsChanged(this, 0, count-1);
            }
            if (totalMemory!=null) this.totalMemory.setText(totalMemory);
            if (percentUsed!=null) this.percentUsed.setText(percentUsed);
        }
    }

    /**
     * Popups the dialog box and wait for the user. This method
     * always invoke {@link #start} before showing the dialog,
     * and {@link #stop} after disposing it.
     */
    public void showDialog(final Component owner)
    {
        try
        {
            start();
            SwingUtilities.showMessageDialog(owner, this, resources.getMenuLabel(ResourceKeys.ABOUT), JOptionPane.PLAIN_MESSAGE);
        }
        finally
        {
            stop();
        }
    }

    /**
     * Start a daemon thread updating dialog box information. Updated information include
     * available memory and the list of running tasks. <strong>You <u>must</u> invoke the
     * {@link #stop} method after <code>start()</code></strong> (typically in a <code>try..finally</code>
     * construct) in order to free resources. <code>stop()</code> is not automatically
     * invoked by the garbage collector.
     */
    protected void start()
    {
        final ThreadList updater=this.updater;
        if (updater!=null) updater.start();
    }

    /**
     * Free any resources used by this dialog box.  <strong>This method must be invoked
     * after {@link #start}</strong> in order to free resources. <code>stop()</code> is
     * not automatically invoked by the garbage collector.
     */
    protected void stop()
    {
        final ThreadList updater=this.updater;
        if (updater!=null) updater.worker=null; // Stop the thread.
        // Le thread avait une référence indirecte vers 'this' via 'ListDataListener'
    }

    /**
     * Convenience method for setting the <code>Compilation-Date</code>
     * attributes to the current date.
     *
     * @param attributes Attributes in which setting the compilation date.
     */
    public static void touch(final Attributes attributes)
    {attributes.putValue(TIMESTAMP, getDateFormat().format(new Date()));}
}
