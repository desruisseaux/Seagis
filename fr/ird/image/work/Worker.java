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
package fr.ird.image.work;

// Base de données et images
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;

// Géométrie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

// Entrés/sorties
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;

// Ensembles
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

// Divers
import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import javax.media.jai.util.Range;
import javax.swing.event.EventListenerList;

// Geotools
import org.geotools.gc.GridRange;
import org.geotools.gc.GridCoverage;
import org.geotools.cv.SampleDimension;
import org.geotools.resources.Utilities;
import org.geotools.util.ProgressListener;
import org.geotools.resources.geometry.XAffineTransform;
import org.geotools.gui.headless.ProgressMailer;
import org.geotools.gui.headless.ProgressPrinter;

// Seagis
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageEntry;
import fr.ird.database.coverage.CoverageDataBase;


/**
 * Effectue des opérations sur une séries d'images.
 * L'utilisation de cette classe se fait en plusieurs étapes:
 *
 * <ul>
 *   <li>Créer une classe dérivée qui effectue l'opération
 *       souhaitée. Cette classe dérivée doit redéfinir la
 *       méthode {@link #run(CoverageEntry[],Result)}.</li>
 *   <li>Appeler une des méthodes <code>setCoverages(...)</code>
 *       pour spécifier les images sur lesquelles on veut
 *       appliquer l'opération.</li>
 *   <li>Appeler {@link #setDestination} pour spécifier le
 *       répertoire de destination dans lequel placer les
 *       sorties des opérations.</li>
 *   <li>Appeler {@link #run()} pour lancer l'opération.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Worker implements Runnable {
    /**
     * Liste d'objets {@link CoverageEntry} enregistrés sur le disque.
     * Cette liste sera utilisée si on n'a pas réussi à se connecter
     * à la base de données.
     */
    private static final String FALL_BACK = "application-data/cache/Worker.serialized";

    /**
     * Objets à informer des progrès du calcul.
     */
    private final List<ProgressListener> listenerList = new ArrayList<ProgressListener>();

    /**
     * Nom de l'opération.
     */
    private final String name;

    /**
     * Séries d'images sur lesquelles effectuer
     * les opérations, ou <code>null</code> si
     * cette information n'a pas encore été spécifiée.
     */
    private CoverageEntry[] entries;

    /**
     * Chaque clé représente une liste <code>List<SampleDimension></code>, c'est-à-dire
     * les {@link SampleDimension} pour chaque bande d'une image. Les valeurs (des objets
     * {@link Integer}) représentent le nombre de fois que ces bandes ont été utilisées.
     */
    private final Map<List<SampleDimension>,Integer> bands = new HashMap<List<SampleDimension>,Integer>();

    /**
     * Répertoire de destination. Les résultats des opérations
     * seront enregistrés en binaire ou en texte dans ce répertoire.
     */
    private File destination;

    /**
     * Indique si l'utilisateur a demandé
     * l'interruption de l'opération.
     */
    private transient volatile boolean stopped;

    /**
     * Construit un objet qui effectuera des opérations sur une série
     * d'images. Le type d'opération dépendra de la classe dérivée.
     *
     * @param name Nom de l'opération.
     */
    public Worker(final String name) {
        this.name = name;
    }

    /**
     * Fait apparaître un paneau indiquant qu'un calcul est en cours. Ce
     * paneau contiendra un bouton "Arrêter" qui permettra à l'utilisateur
     * d'interrompre un calcul en cours.
     *
     * @param title Titre de la fenêtre.
     */
    public void showBusyPane(final String title) {
        try {
            new ProgressPanel(this).show(title);
        } catch (IOException exception) {
            exceptionOccurred("showBusyPane", exception);
        }
    }

    /**
     * Spécifie un objet à informer des progrès du calcul.
     */
    public void addProgressListener(final ProgressListener progress) {
        listenerList.add(progress);
    }

    /**
     * Indique qu'un objet n'est plus intéressé
     * à être informé des progrès du calcul.
     */
    public void removeProgressListener(final ProgressListener progress) {
        listenerList.remove(progress);
    }

    /**
     * Spécifie les images à utiliser en entrés pour les opérations.
     */
    public synchronized void setCoverages(final List<CoverageEntry> entries) {
        this.entries = entries.toArray(new CoverageEntry[entries.size()]);
    }

    /**
     * Spécifie une série d'images à utiliser en entrés pour les opérations.
     * Cette méthode se connectera à une base de données par défaut pour
     * obtenir les images de la série nommée.
     *
     * @param series Nom de la série. Les noms valides sont inscrites
     *               dans la base de données. On y trouve entre autres:
     *               <ul>
     *                 <li>SST (synthèse)</li>
     *                 <li>Chlorophylle-a (Réunion)</li>
     *               </ul>
     *
     * @throws SQLException si la connection à la base de données a échouée.
     */
    public void setCoverages(final String series) throws SQLException {
        setCoverages(series, null, null, null);
    }

    /**
     * Spécifie une série d'images à utiliser en entrés pour les opérations.
     * Cette méthode se connectera à une base de données par défaut pour
     * obtenir les images de la série nommée.
     *
     * @param series         Nom de la série.
     * @param startTime      Date de la première image à prendre en compte, ou <code>null</code>
     *                       pour commencer à la première image de la série.
     * @param endTime        Date de la dernière image à prendre en compte, ou <code>null</code>
     *                       pour aller jusqu'à la dernière image de la série.
     * @param geographicArea Coordonnnées géographiques de la région à prendre en compte,
     *                       ou <code>null</code> pour ne pas imposer de limites géographiques.
     * @throws SQLException si la connection à la base de données a échouée.
     */
    public synchronized void setCoverages(String series, Date startTime, Date endTime,
                                          final Rectangle2D geographicArea) throws SQLException
    {
        final CoverageDataBase database = new fr.ird.database.coverage.sql.CoverageDataBase();
        if (series == null) {
            series = "SST (synthèse)";
        }
        final CoverageTable table = database.getCoverageTable(series);
        if (startTime!=null || endTime!=null) {
            final Range range = table.getTimeRange();
            if (startTime == null) startTime = (Date) range.getMinValue();
            if (  endTime == null)   endTime = (Date) range.getMaxValue();
            table.setTimeRange(startTime, endTime);
        }
        if (geographicArea != null) {
            table.setGeographicArea(geographicArea);
        }
        setCoverages(table.getEntries());
        table.close();
        database.close();
    }

    /**
     * Obtient la liste des images à utiliser en entrés. Si aucune image n'a été
     * spécifiée (c'est-à-dire si aucune méthode <code>setCoverages(...)</code> n'a
     * été appelée), alors cette méthode retournera les images d'une série par
     * défaut.
     */
    private synchronized CoverageEntry[] getCoverages() {
        if (entries==null) try {
            setCoverages((String)null);
            if (entries!=null) try {
                /*
                 * Si des images ont pu être obtenues, sauvegarde la liste des images afin de
                 * permettre plus tard une utilisation pas d'autres processus qui n'auraient
                 * pas accès à la base de données.
                 */
                final ObjectOutputStream output=new ObjectOutputStream(new DeflaterOutputStream(new FileOutputStream(new File(FALL_BACK))));
                output.writeObject(entries);
                output.close();
            } catch (IOException exception) {
                exceptionOccurred("getCoverages", exception);
            }
        } catch (SQLException exception) {
            /*
             * Si la connection avec la base de données a échouée, essaie d'utiliser
             * les images par défauts qui avaient été sauvegardées en binaires lors
             * d'une exécution précédente.
             */
            final InputStream input = getClass().getClassLoader().getResourceAsStream(FALL_BACK);
            if (input != null) {
                try {
                    final ObjectInputStream objectInput=new ObjectInputStream(new InflaterInputStream(input));
                    entries = (CoverageEntry[]) objectInput.readObject();
                    objectInput.close();
                } catch (IOException ioexception) {
                    exceptionOccurred("getCoverages", ioexception);
                } catch (ClassNotFoundException ioexception) {
                    exceptionOccurred("getCoverages", ioexception);
                }
            } else {
                exceptionOccurred("getCoverages", exception);
            }
        }
        return (entries!=null) ? (CoverageEntry[]) entries.clone() : new CoverageEntry[0];
    }

    /**
     * Spécifie le répertoire dans lequel placer les résultats des opérations.
     * Les résultats pourront être de la forme d'images PNG, de fichier texte
     * ou de fichiers binaires.
     */
    public void setDestination(final File directory) {
        this.destination = directory;
    }

    /**
     * Obtient le nom de fichier de sortie à utiliser pour l'image spécifiée.
     * Ce nom de fichier portera le même nom que le nom de fichier de l'image,
     * mais sera dans le répertoire de destination qui aura été spécifié avec
     * {@link #setDestination} plutôt que dans le répertoire de l'image.
     */
    final File getOutputFile(final CoverageEntry image) {
        final String filename = image.getFile().getName();
        final int    extIndex = filename.lastIndexOf('.');
        if (extIndex>0 && filename.charAt(extIndex-1)!='.') {
            return new File(destination, filename.substring(0, extIndex)+".data");
        } else {
            return null;
        }
    }

    /**
     * Démarre le travail. L'implémentation par défaut prévient les objets {@link ProgressListener}
     * que la lecture commence et appelle ensuite {@link #run(CoverageEntry[])}.
     *
     * @see #stop()
     */
    public synchronized void run() {
        final File destination=this.destination;
        final File imageFile = new File(destination, name+".png");
        if (imageFile.exists()) {
            System.out.print("Saute ");
            System.out.println(imageFile.getPath());
            return;
        }
        stopped = false;
        for (int i=listenerList.size(); --i>=0;) {
            listenerList.get(i).started();
        }
        final Result result = run(getCoverages(), null); // TODO: sauvegarder le travail?
        for (int i=listenerList.size(); --i>=0;) {
            listenerList.get(i).complete();
        }
        /*
         * Si le résultat peut produire une image,
         * enregistre cette image au format PNG.
         */
        if (result != null) {
            final List[] lists = sortByFrequency(this.bands);
            final List<SampleDimension> bands = (List<SampleDimension>)
                                               ((lists!=null && lists.length!=0) ? lists[0] : null);
            final RenderedImage image = result.getImage(bands.toArray(new SampleDimension[bands.size()]));
            if (image != null) try {
                ImageIO.write(image, "png", imageFile);
            } catch (IOException exception) {
                exceptionOccurred("run", exception);
            }
        }
    }

    /**
     * Démarre le travail sur une série d'images.
     *
     * @param  entries Séries d'images sur lequel faire le travail.
     * @param  result  Résultat obtenus la dernières fois que ce travail avait été fait, ou
     *                 <code>null</code> si aucun résulat n'avait été précédemment sauvegardé.
     * @return Résultat de ce travail, ou <code>null</code> s'il n'y en a pas.
     */
    protected abstract Result run(final CoverageEntry[] entries, final Result result);

    /**
     * Interrompt l'opération. Cette méthode signale aux méthodes <code>run(...)</code>
     * qu'elles doivent cesser leur exécution et si possible retourner un {@link Result}
     * cohérent, quoi que incomplet.
     */
    public final void stop() {
        stopped = true;
    }

    /**
     * Indique si l'utilisateur a demandé l'interruption de l'opération. La méthode
     * {@link #run(CoverageEntry,Result)} devrait interroger cette méthode à intervals
     * réguliers.
     *
     * @see #run()
     * @see #stop()
     */
    protected final boolean isStopped() {
        return stopped;
    }

    /**
     * Spécifie une chaîne de caractère qui décrit l'opération en cours.
     */
    protected void setDescription(final String description) {
        for (int i=listenerList.size(); --i>=0;) {
            listenerList.get(i).setDescription(description);
        }
    }

    /**
     * Indique l'état d'avancement de l'opération. Le progrès est représenté par un
     * pourcentage variant de 0 à 100 inclusivement. Si la valeur spécifiée est en
     * dehors de ces limites, elle sera automatiquement ramenée entre 0 et 100.
     */
    protected void progress(final float percent) {
        for (int i=listenerList.size(); --i>=0;) {
            listenerList.get(i).progress(percent);
        }
    }

    /**
     * Signale qu'une exception est survenue. L'exception sera signalée
     * à l'utilisateur sans interrompre l'exécution du programme.
     *
     * @param method Nom de la méthode dans laquelle est survenue l'exception.
     * @param exception L'exception survenue.
     */
    protected void exceptionOccurred(final String method, final Throwable exception) {
        if (listenerList.size() != 0) {
            for (int i=listenerList.size(); --i>=0;) {
                listenerList.get(i).exceptionOccurred(exception);
            }
        } else {
            Result.unexpectedException(Utilities.getShortClassName(this), method, exception);
        }
    }

    /**
     * Retourne l'image de l'entré spécifiée. Bien que ce ne soit pas obligatoire, il
     * est préférable d'appeler cette méthode plutôt que {@link CoverageEntry#getGridCoverage}.
     *
     * @param  entry Entré dont on veut l'image.
     * @return Image de l'entré spécifiée.
     * @throws IOException si une erreur de lecture est survenue.
     */
    protected GridCoverage getGridCoverage(final CoverageEntry entry) throws IOException {
        final GridCoverage image = entry.getGridCoverage(null);
        if (image != null) {
            final List<SampleDimension> list = Arrays.asList(image.getSampleDimensions());
            final Integer value = bands.get(list);
            final int count = (value!=null) ? value.intValue()+1 : 1;
            bands.put(list, new Integer(count));
        }
        return image;
    }

    /**
     * Retourne la liste de tous les <code>SampleDimension[]</code> utilisés par les images.
     * Si toutes les images utilisent le même <code>SampleDimension[]</code>, alors le tableau
     * retourné aura une longueur de 1. Sinon, les éléments du tableau seront classés en ordre
     * décroissant de fréquence d'utilisation. Cela signifie que le premier élément de la liste
     * sera le <code>SampleDimension[]</code> le plus utilisé par le plus grand nombre d'images.
     */
    private static List[] sortByFrequency(final Map<List<SampleDimension>,Integer> bands) {
        final Set<List<SampleDimension>> listSet = bands.keySet();
        final List[] listArray = listSet.toArray(new List[listSet.size()]);
        Arrays.sort(listArray, new Comparator<List<SampleDimension>>() {
            public int compare(final List<SampleDimension> list1, final List<SampleDimension> list2) {
                final Integer c1 = bands.get(list1);
                final Integer c2 = bands.get(list2);
                return c2.compareTo(c1); // Ordre décroissant
            }
        });
        return listArray;
    }

    /**
     * Retourne les coordonnées pixels d'une boîte qui englobe la forme géométrique
     * <code>area</code>. Les coordonnées de la boîte retournée seront réduites aux
     * limites de l'image si nécessaire.
     *
     * @param  coverage Image pour laquelle on veut les limites de la région à traiter.
     * @param  area Coordonnées géographiques (en degrés de longitude et de latitude) de
     *         la région à prendre en compte dans le calcul.
     * @return Coordonnées en pixels de la région de <code>image</code> à traiter.
     */
    static Rectangle getBounds(final GridCoverage coverage, final Shape area) {
        final GridRange  range = coverage.getGridGeometry().getGridRange();
        final Rectangle bounds = new Rectangle(range.getLower (0), range.getLower (1),
                                               range.getLength(0), range.getLength(1));
        if (area!=null) {
// TODO
//          final AffineTransform transform = coverage.getGridGeometry().getGridToCoordinateSystem2D();
//          transform.translate(-0.5, -0.5);
//          return clip((Rectangle)XAffineTransform.transform(transform, area.getBounds2D(), bounds), coverage.getRenderedImage(true));
        }
        return bounds;
    }

    /**
     * Modifie les limites du rectangle <code>bounds</code> spécifiée de
     * façon à ce qu'il soit entièrement compris à l'intérieur de l'image.
     * Le rectangle <code>bounds</code> est retourné par commodité.
     */
    static Rectangle clip(final Rectangle bounds, final RenderedImage image) {
        final int xmin = image.getMinX();
        final int ymin = image.getMinY();
        final int xmax = bounds.x + bounds.width;
        final int ymax = bounds.y + bounds.height;
        if (bounds.x < xmin) bounds.x=xmin;
        if (bounds.y < ymin) bounds.y=ymin;
        bounds.width  = Math.min(xmax-bounds.x, xmin+image.getWidth ());
        bounds.height = Math.min(ymax-bounds.y, ymin+image.getHeight());
        return bounds;
    }

    /**
     * Configure cet objet en fonction des arguments transmis sur la ligne de commande.
     * Cette méthode est conçue pour être appelée par les méthodes <code>main</code>
     * des classes dérivées. Les arguments actuellement reconnus sont:
     *
     * <ul>
     *   <li><code>-mail</code>  informe des progrès par courriel.</li>
     *   <li><code>-print</code> informe des progrès sur le périphérique de sortie standard.</li>
     *   <li><code>-show</code>  affiche un paneau indiquant qu'un calcul est en cours. Ce paneau
     *                           contient un bouton permettant l'arrêt du calcul à tout moment.</li>
     * </ul>
     */
    synchronized void setup(final String[] args) throws Exception {
        final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        Date startTime = null;
        Date   endTime = null;
        String  series = null;
        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            if (arg != null) {
                arg = arg.trim().toLowerCase();
                if (arg.equalsIgnoreCase("-mail")) {
                    addProgressListener(new ProgressMailer("salam.teledetection.fr",
                                                           "martin.desruisseaux@teledetection.fr"));
                    continue;
                }
                if (arg.equalsIgnoreCase("-print")) {
                    addProgressListener(new ProgressPrinter());
                    continue;
                }
                if (arg.equalsIgnoreCase("-show")) {
                    showBusyPane(name);
                    continue;
                }
                if (arg.startsWith("series=")) {
                    series = arg.substring(7);
                    continue;
                }
                if (arg.startsWith("start=")) {
                    startTime = dateFormat.parse(arg.substring(6));
                    continue;
                }
                if (arg.startsWith("end=")) {
                    endTime = dateFormat.parse(arg.substring(4));
                    continue;
                }
                System.err.print("Argument non-reconnu: ");
                System.err.println(arg);
                System.exit(0);
            }
        }
        if (series!=null || startTime!=null || endTime!=null) {
            setCoverages(series, startTime, endTime, null);
        }
    }
}
