/*
 * OpenGIS implementation in Java
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
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
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
import net.seas.util.SwingUtilities;

// Input/output
import java.net.URL;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import javax.imageio.ImageIO;

// Manifest
import java.util.jar.Manifest;
import java.util.jar.Attributes;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Miscellaneous
import java.util.Date;
import java.util.Arrays;
import java.util.Locale;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import net.seas.resources.Resources;
import net.seas.util.XArray;


/**
 * A "About" dialog box. This dialog box contains the application title and some
 * system informations (Java and OS version, free memory, image readers and writers,
 * running threads, etc.).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class About extends JPanel
{
    /**
     * Unit� � utiliser pour les affichages
     * de la quantit� de m�moire utilis�e.
     */
    private static final float MEMORY_UNIT = (1024f*1024f);

    /**
     * The entry for timestamp in the manifest file.
     */
    private static final String TIMESTAMP = "Timestamp";

    /**
     * Thread qui aura la charge de faire des mises � jour en arri�re-plan.
     * Ce champ sera <code>null</code> s'il n'y en a pas.
     */
    private final ThreadList updater;

    /**
     * Construct a new dialog box for the specified class. Application name
     * will be fetch from the manifest file associated with the class's package.
     *
     * @param logo        The application's logo. It may be a {@link JComponent},
     *                    an {@link Icon} object or a {@link String} representing
     *                    the pathname to an image.
     * @param classe      The application's class. Application name will be fetch from
     *                    the manifest file (<code>META-INF/Manifest.mf</code>).
     * @param tasks       Group of running threads, or <code>null</code> if there is none.
     */
    public About(final Object logo, final Class classe, final ThreadGroup tasks)
    {this(logo, classe, null, null, tasks);}

    /**
     * Construct a new dialog box with the specified application's name.
     *
     * @param logo        The application's logo. It may be a {@link JComponent},
     *                    an {@link Icon} object or a {@link String} representing
     *                    the pathname to an image.
     * @param application Application name and version number.
     * @param vendor      The application vendor.
     * @param tasks       Group of running threads, or <code>null</code> if there is none.
     */
    public About(final Object logo, final String application, final String vendor, final ThreadGroup tasks)
    {this(logo, null, application, vendor, tasks);}

    /**
     * Construct a new dialog box.
     *
     * @param logo        The application's logo. It may be a {@link JComponent},
     *                    an {@link Icon} object or a {@link String} representing
     *                    the pathname to an image.
     * @param classe      The application's class, or <code>null</code> if none.
     * @param application Application name and version number.
     * @param vendor      The application vendor.
     * @param tasks       Group of running threads, or <code>null</code> if there is none.
     */
    private About(final Object logo, Class classe, String application, String vendor, final ThreadGroup tasks)
    {
        super(new BorderLayout());
        /*
         * If application name is unset, fetch it
         * from the class's package description.
         */
        if (classe!=null)
        {
            final Package pack = classe.getPackage();
            if (pack!=null)
            {
                if (application==null)
                {
                    application = pack.getImplementationTitle();
                    if (application==null)
                        application = pack.getSpecificationTitle();
                }
                if (vendor==null)
                {
                    vendor = pack.getImplementationVendor();
                    if (vendor==null)
                        vendor = pack.getSpecificationVendor();
                }
            }
            System.out.println(pack);
        }
        else classe=getClass();
        final ClassLoader loader=classe.getClassLoader();
        /*
         * Construct GUI panels.
         */
        final JTabbedPane tabs=new JTabbedPane();
        final Runtime system=Runtime.getRuntime();
        system.gc();
        final float        freeMemory = system.freeMemory()  / MEMORY_UNIT;
        final float       totalMemory = system.totalMemory() / MEMORY_UNIT;
        final JLabel totalMemoryLabel = new JLabel(Resources.format(Cl�.TOTAL_MEMORY�1, new Float(totalMemory)));
        final JLabel percentUsedLabel = new JLabel(Resources.format(Cl�.MEMORY_USE�1,   new Float(1-freeMemory/totalMemory)));
        if (true)
        {
            final Date timestamp = getTimestamp(loader);
            if (timestamp!=null)
            {
                StringBuffer buffer=new StringBuffer(application);
                buffer.append(" (");
                buffer=DateFormat.getDateInstance(DateFormat.LONG).format(timestamp, buffer, new FieldPosition(0));
                buffer.append(')');
                application = buffer.toString();
            }
            final JPanel pane = new JPanel(new GridBagLayout());
            final GridBagConstraints c=new GridBagConstraints();
            c.gridx=0; c.weightx=1;
            c.gridy=0;                  pane.add(new JLabel(application), c);
            c.gridy++;                  pane.add(new JLabel(vendor     ), c);
            c.gridy++; c.insets.top= 6; pane.add(new JLabel(Resources.format(Cl�.JAVA_VERSION�1, System.getProperty("java.version"))), c);
            c.gridy++; c.insets.top= 0; pane.add(new JLabel(Resources.format(Cl�.JAVA_VENDOR�1,  System.getProperty("java.vendor" ))), c);
            c.gridy++; c.insets.top= 6; pane.add(new JLabel(Resources.format(Cl�.OS_NAME�1,      System.getProperty("os.name"     ))), c);
            c.gridy++; c.insets.top= 0; pane.add(new JLabel(Resources.format(Cl�.OS_VERSION�2,   System.getProperty("os.version"),
                                                                                                 System.getProperty("os.arch"     ))), c);
            c.gridy++; c.insets.top=12; pane.add(totalMemoryLabel, c);
            c.gridy++; c.insets.top= 0; pane.add(percentUsedLabel, c);
            tabs.addTab(Resources.format(Cl�.SYSTEM), pane);
        }
        if (tasks!=null)
        {
            final JPanel pane = new JPanel(new BorderLayout());
            final JList  list = new JList(updater=new ThreadList(tasks, totalMemoryLabel, percentUsedLabel));
            pane.add(new JLabel(Resources.format(Cl�.RUNNING_TASKS)), BorderLayout.NORTH);
            pane.add(new JScrollPane(list), BorderLayout.CENTER);
            tabs.addTab(Resources.format(Cl�.TASKS), pane);
        }
        else updater=null;
        if (true)
        {
            final JPanel   pane = new JPanel(new GridLayout(1,2,3,3));
            final String[] readers = ImageIO.getReaderMIMETypes();
            final String[] writers = ImageIO.getWriterMIMETypes();
            Arrays.sort(readers);
            Arrays.sort(writers);
            Box c;
            c=Box.createVerticalBox(); c.add(Box.createVerticalStrut(3)); c.add(new JLabel(Resources.format(Cl�.DECODERS), JLabel.CENTER)); c.add(Box.createVerticalStrut(3)); c.add(new JScrollPane(new JList(readers))); pane.add(c);
            c=Box.createVerticalBox(); c.add(Box.createVerticalStrut(3)); c.add(new JLabel(Resources.format(Cl�.ENCODERS), JLabel.CENTER)); c.add(Box.createVerticalStrut(3)); c.add(new JScrollPane(new JList(writers))); pane.add(c);
            tabs.addTab(Resources.format(Cl�.IMAGES), pane);
        }
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
        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Mod�le repr�sentant la liste des processus actif dans un {@link ThreadGroup}.
     * Cette liste se mettre automatiquement � jour de fa�on p�riodique.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class ThreadList extends AbstractListModel implements Runnable
    {
        /**
         * Processus qui met � jour <code>ThreadList</code>,
         * ou <code>null</code> s'il n'y en a pas. On peut
         * tuer le processus actif en donnant la valeur
         * <code>null</code> � cette variable.
         */
        public transient Thread worker;

        /**
         * Liste des processus en cours.
         */
        private final ThreadGroup tasks;

        /**
         * Liste des noms des processus en cours. Cette
         * liste sera mises � jour p�riodiquement.
         */
        private String[] names=new String[0];

        /**
         * Texte dans lequel �crire la m�moire totale r�serv�e.
         */
        private final JLabel totalMemory;

        /**
         * Texte dans lequel �crire le pourcentage de m�moire utilis�e.
         */
        private final JLabel percentUsed;

        /**
         * Construit une liste des processus actifs
         * dans le groupe <code>tasks</code> sp�cifi�.
         */
        public ThreadList(final ThreadGroup tasks, final JLabel totalMemory, final JLabel percentUsed)
        {
            this.tasks       = tasks;
            this.totalMemory = totalMemory;
            this.percentUsed = percentUsed;
        }

        /**
         * Retourne le nombre d'�l�ments dans la liste.
         */
        public int getSize() // NO synchronized here
        {return names.length;}

        /**
         * Retourne un des �l�ments de la liste.
         */
        public Object getElementAt(final int index) // NO synchronized here
        {return names[index];}

        /**
         * Ajoute un objet � la liste des objets int�ress�s
         * � �tre inform� des changements apport�s � la liste.
         */
        public synchronized void addListDataListener(final ListDataListener listener)
        {super.addListDataListener(listener);}

        /**
         * D�marre le thread.
         */
        public synchronized void start()
        {
            if (worker==null)
            {
                worker=new Thread(this, Resources.format(Cl�.ABOUT));
                worker.setPriority(Thread.MIN_PRIORITY);
                worker.setDaemon(true);
                worker.start();
            }
        }

        /**
         * Met � jour le contenu de la liste � interval r�gulier.
         * Cette m�thode est ex�cut�e dans une boucle jusqu'� ce
         * qu'elle soit interrompue en donnant la valeur nulle �
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
                String   totalMemoryText = Resources.format(Cl�.TOTAL_MEMORY�1, new Float(totalMemoryN));
                String   percentUsedText = Resources.format(Cl�.MEMORY_USE�1,   new Float(1-freeMemoryN/totalMemoryN));

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
                    // Quelqu'un a r�veill� ce thread.
                    // Retourne donc au travail.
                }
            }
            worker=null;
        }

        /**
         * Met � jour le contenu de la liste. Cette m�thode
         * est appel�e p�riodiquement dans le thread de Swing.
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
            SwingUtilities.showMessageDialog(owner, this, Resources.trailing(Cl�.ABOUT), JOptionPane.PLAIN_MESSAGE);
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
        // Le thread avait une r�f�rence indirecte vers 'this' via 'ListDataListener'
    }

    /**
     * Retourne la date cod�e dans le fichier <code>META-INF/Manifest.mf</code> sous
     * le nom "Timestamp". Si aucune date n'a �t� trouv�e ou si elle est illisible,
     * alors cette m�thode retourne <code>null</code>.
     */
    private static Date getTimestamp(final ClassLoader loader)
    {
        if (loader!=null)
        {
            final InputStream stream = loader.getResourceAsStream("META-INF/Manifest.mf");
            if (stream!=null) try
            {
                final Manifest manifest = new Manifest(stream);
                stream.close();
                String date = manifest.getMainAttributes().getValue(TIMESTAMP);
                if (date!=null) return getTimestampFormat().parse(date);
            }
            catch (Exception exception)
            {
                final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
                record.setThrown(exception);
                Logger.getLogger("net.seas.util").log(record);
            }
        }
        return null;
    }

    /**
     * Returns the date format for timestamp.
     */
    private static DateFormat getTimestampFormat()
    {return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.CANADA);}

    /**
     * Ecrit dans la date courante dans le fichier <code>META-INF/timestamp.txt</code>.
     * Cette date sera �crite en nombre de millisecondes �coul�es depuis le 1er janvier
     * 1970. Ce fichier sera consult� par la bo�te de dialogue {@link About} pour obtenir
     * la date � afficher.
     *
     * @throws IOException si la date n'a pas pu �tre �crite.
     */
    public static void main(final String[] args) throws IOException
    {
        switch (args.length)
        {
            case 0:
            {
                new About(null, About.class, Thread.currentThread().getThreadGroup()).showDialog(null);
                System.exit(0);
                break;
            }
            case 1: if (args[0].equalsIgnoreCase("-touch"))
            {
                final Writer out=new FileWriter(TIMESTAMP);
                out.write(String.valueOf(System.currentTimeMillis()));
                out.write(System.getProperty("line.separator", "\n"));
                out.close();
                break;
            }
            default: System.out.println("Usage: About -touch");
        }
    }
}
