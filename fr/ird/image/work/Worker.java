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
package fr.ird.image.work;

// Base de donn�es et images
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;

// G�om�trie
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

// Entr�s/sorties
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
 * Effectue des op�rations sur une s�ries d'images.
 * L'utilisation de cette classe se fait en plusieurs �tapes:
 *
 * <ul>
 *   <li>Cr�er une classe d�riv�e qui effectue l'op�ration
 *       souhait�e. Cette classe d�riv�e doit red�finir la
 *       m�thode {@link #run(CoverageEntry[],Result)}.</li>
 *   <li>Appeler une des m�thodes <code>setCoverages(...)</code>
 *       pour sp�cifier les images sur lesquelles on veut
 *       appliquer l'op�ration.</li>
 *   <li>Appeler {@link #setDestination} pour sp�cifier le
 *       r�pertoire de destination dans lequel placer les
 *       sorties des op�rations.</li>
 *   <li>Appeler {@link #run()} pour lancer l'op�ration.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class Worker implements Runnable {
    /**
     * Liste d'objets {@link CoverageEntry} enregistr�s sur le disque.
     * Cette liste sera utilis�e si on n'a pas r�ussi � se connecter
     * � la base de donn�es.
     */
    private static final String FALL_BACK = "application-data/cache/Worker.serialized";

    /**
     * Objets � informer des progr�s du calcul.
     */
    private final List<ProgressListener> listenerList = new ArrayList<ProgressListener>();

    /**
     * Nom de l'op�ration.
     */
    private final String name;

    /**
     * S�ries d'images sur lesquelles effectuer
     * les op�rations, ou <code>null</code> si
     * cette information n'a pas encore �t� sp�cifi�e.
     */
    private CoverageEntry[] entries;

    /**
     * Chaque cl� repr�sente une liste <code>List<SampleDimension></code>, c'est-�-dire
     * les {@link SampleDimension} pour chaque bande d'une image. Les valeurs (des objets
     * {@link Integer}) repr�sentent le nombre de fois que ces bandes ont �t� utilis�es.
     */
    private final Map<List<SampleDimension>,Integer> bands = new HashMap<List<SampleDimension>,Integer>();

    /**
     * R�pertoire de destination. Les r�sultats des op�rations
     * seront enregistr�s en binaire ou en texte dans ce r�pertoire.
     */
    private File destination;

    /**
     * Indique si l'utilisateur a demand�
     * l'interruption de l'op�ration.
     */
    private transient volatile boolean stopped;

    /**
     * Construit un objet qui effectuera des op�rations sur une s�rie
     * d'images. Le type d'op�ration d�pendra de la classe d�riv�e.
     *
     * @param name Nom de l'op�ration.
     */
    public Worker(final String name) {
        this.name = name;
    }

    /**
     * Fait appara�tre un paneau indiquant qu'un calcul est en cours. Ce
     * paneau contiendra un bouton "Arr�ter" qui permettra � l'utilisateur
     * d'interrompre un calcul en cours.
     *
     * @param title Titre de la fen�tre.
     */
    public void showBusyPane(final String title) {
        try {
            new ProgressPanel(this).show(title);
        } catch (IOException exception) {
            exceptionOccurred("showBusyPane", exception);
        }
    }

    /**
     * Sp�cifie un objet � informer des progr�s du calcul.
     */
    public void addProgressListener(final ProgressListener progress) {
        listenerList.add(progress);
    }

    /**
     * Indique qu'un objet n'est plus int�ress�
     * � �tre inform� des progr�s du calcul.
     */
    public void removeProgressListener(final ProgressListener progress) {
        listenerList.remove(progress);
    }

    /**
     * Sp�cifie les images � utiliser en entr�s pour les op�rations.
     */
    public synchronized void setCoverages(final List<CoverageEntry> entries) {
        this.entries = entries.toArray(new CoverageEntry[entries.size()]);
    }

    /**
     * Sp�cifie une s�rie d'images � utiliser en entr�s pour les op�rations.
     * Cette m�thode se connectera � une base de donn�es par d�faut pour
     * obtenir les images de la s�rie nomm�e.
     *
     * @param series Nom de la s�rie. Les noms valides sont inscrites
     *               dans la base de donn�es. On y trouve entre autres:
     *               <ul>
     *                 <li>SST (synth�se)</li>
     *                 <li>Chlorophylle-a (R�union)</li>
     *               </ul>
     *
     * @throws SQLException si la connection � la base de donn�es a �chou�e.
     */
    public void setCoverages(final String series) throws SQLException {
        setCoverages(series, null, null, null);
    }

    /**
     * Sp�cifie une s�rie d'images � utiliser en entr�s pour les op�rations.
     * Cette m�thode se connectera � une base de donn�es par d�faut pour
     * obtenir les images de la s�rie nomm�e.
     *
     * @param series         Nom de la s�rie.
     * @param startTime      Date de la premi�re image � prendre en compte, ou <code>null</code>
     *                       pour commencer � la premi�re image de la s�rie.
     * @param endTime        Date de la derni�re image � prendre en compte, ou <code>null</code>
     *                       pour aller jusqu'� la derni�re image de la s�rie.
     * @param geographicArea Coordonnn�es g�ographiques de la r�gion � prendre en compte,
     *                       ou <code>null</code> pour ne pas imposer de limites g�ographiques.
     * @throws SQLException si la connection � la base de donn�es a �chou�e.
     */
    public synchronized void setCoverages(String series, Date startTime, Date endTime,
                                          final Rectangle2D geographicArea) throws SQLException
    {
        final CoverageDataBase database = new fr.ird.database.coverage.sql.CoverageDataBase();
        if (series == null) {
            series = "SST (synth�se)";
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
     * Obtient la liste des images � utiliser en entr�s. Si aucune image n'a �t�
     * sp�cifi�e (c'est-�-dire si aucune m�thode <code>setCoverages(...)</code> n'a
     * �t� appel�e), alors cette m�thode retournera les images d'une s�rie par
     * d�faut.
     */
    private synchronized CoverageEntry[] getCoverages() {
        if (entries==null) try {
            setCoverages((String)null);
            if (entries!=null) try {
                /*
                 * Si des images ont pu �tre obtenues, sauvegarde la liste des images afin de
                 * permettre plus tard une utilisation pas d'autres processus qui n'auraient
                 * pas acc�s � la base de donn�es.
                 */
                final ObjectOutputStream output=new ObjectOutputStream(new DeflaterOutputStream(new FileOutputStream(new File(FALL_BACK))));
                output.writeObject(entries);
                output.close();
            } catch (IOException exception) {
                exceptionOccurred("getCoverages", exception);
            }
        } catch (SQLException exception) {
            /*
             * Si la connection avec la base de donn�es a �chou�e, essaie d'utiliser
             * les images par d�fauts qui avaient �t� sauvegard�es en binaires lors
             * d'une ex�cution pr�c�dente.
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
     * Sp�cifie le r�pertoire dans lequel placer les r�sultats des op�rations.
     * Les r�sultats pourront �tre de la forme d'images PNG, de fichier texte
     * ou de fichiers binaires.
     */
    public void setDestination(final File directory) {
        this.destination = directory;
    }

    /**
     * Obtient le nom de fichier de sortie � utiliser pour l'image sp�cifi�e.
     * Ce nom de fichier portera le m�me nom que le nom de fichier de l'image,
     * mais sera dans le r�pertoire de destination qui aura �t� sp�cifi� avec
     * {@link #setDestination} plut�t que dans le r�pertoire de l'image.
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
     * D�marre le travail. L'impl�mentation par d�faut pr�vient les objets {@link ProgressListener}
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
         * Si le r�sultat peut produire une image,
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
     * D�marre le travail sur une s�rie d'images.
     *
     * @param  entries S�ries d'images sur lequel faire le travail.
     * @param  result  R�sultat obtenus la derni�res fois que ce travail avait �t� fait, ou
     *                 <code>null</code> si aucun r�sulat n'avait �t� pr�c�demment sauvegard�.
     * @return R�sultat de ce travail, ou <code>null</code> s'il n'y en a pas.
     */
    protected abstract Result run(final CoverageEntry[] entries, final Result result);

    /**
     * Interrompt l'op�ration. Cette m�thode signale aux m�thodes <code>run(...)</code>
     * qu'elles doivent cesser leur ex�cution et si possible retourner un {@link Result}
     * coh�rent, quoi que incomplet.
     */
    public final void stop() {
        stopped = true;
    }

    /**
     * Indique si l'utilisateur a demand� l'interruption de l'op�ration. La m�thode
     * {@link #run(CoverageEntry,Result)} devrait interroger cette m�thode � intervals
     * r�guliers.
     *
     * @see #run()
     * @see #stop()
     */
    protected final boolean isStopped() {
        return stopped;
    }

    /**
     * Sp�cifie une cha�ne de caract�re qui d�crit l'op�ration en cours.
     */
    protected void setDescription(final String description) {
        for (int i=listenerList.size(); --i>=0;) {
            listenerList.get(i).setDescription(description);
        }
    }

    /**
     * Indique l'�tat d'avancement de l'op�ration. Le progr�s est repr�sent� par un
     * pourcentage variant de 0 � 100 inclusivement. Si la valeur sp�cifi�e est en
     * dehors de ces limites, elle sera automatiquement ramen�e entre 0 et 100.
     */
    protected void progress(final float percent) {
        for (int i=listenerList.size(); --i>=0;) {
            listenerList.get(i).progress(percent);
        }
    }

    /**
     * Signale qu'une exception est survenue. L'exception sera signal�e
     * � l'utilisateur sans interrompre l'ex�cution du programme.
     *
     * @param method Nom de la m�thode dans laquelle est survenue l'exception.
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
     * Retourne l'image de l'entr� sp�cifi�e. Bien que ce ne soit pas obligatoire, il
     * est pr�f�rable d'appeler cette m�thode plut�t que {@link CoverageEntry#getGridCoverage}.
     *
     * @param  entry Entr� dont on veut l'image.
     * @return Image de l'entr� sp�cifi�e.
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
     * Retourne la liste de tous les <code>SampleDimension[]</code> utilis�s par les images.
     * Si toutes les images utilisent le m�me <code>SampleDimension[]</code>, alors le tableau
     * retourn� aura une longueur de 1. Sinon, les �l�ments du tableau seront class�s en ordre
     * d�croissant de fr�quence d'utilisation. Cela signifie que le premier �l�ment de la liste
     * sera le <code>SampleDimension[]</code> le plus utilis� par le plus grand nombre d'images.
     */
    private static List[] sortByFrequency(final Map<List<SampleDimension>,Integer> bands) {
        final Set<List<SampleDimension>> listSet = bands.keySet();
        final List[] listArray = listSet.toArray(new List[listSet.size()]);
        Arrays.sort(listArray, new Comparator<List<SampleDimension>>() {
            public int compare(final List<SampleDimension> list1, final List<SampleDimension> list2) {
                final Integer c1 = bands.get(list1);
                final Integer c2 = bands.get(list2);
                return c2.compareTo(c1); // Ordre d�croissant
            }
        });
        return listArray;
    }

    /**
     * Retourne les coordonn�es pixels d'une bo�te qui englobe la forme g�om�trique
     * <code>area</code>. Les coordonn�es de la bo�te retourn�e seront r�duites aux
     * limites de l'image si n�cessaire.
     *
     * @param  coverage Image pour laquelle on veut les limites de la r�gion � traiter.
     * @param  area Coordonn�es g�ographiques (en degr�s de longitude et de latitude) de
     *         la r�gion � prendre en compte dans le calcul.
     * @return Coordonn�es en pixels de la r�gion de <code>image</code> � traiter.
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
     * Modifie les limites du rectangle <code>bounds</code> sp�cifi�e de
     * fa�on � ce qu'il soit enti�rement compris � l'int�rieur de l'image.
     * Le rectangle <code>bounds</code> est retourn� par commodit�.
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
     * Cette m�thode est con�ue pour �tre appel�e par les m�thodes <code>main</code>
     * des classes d�riv�es. Les arguments actuellement reconnus sont:
     *
     * <ul>
     *   <li><code>-mail</code>  informe des progr�s par courriel.</li>
     *   <li><code>-print</code> informe des progr�s sur le p�riph�rique de sortie standard.</li>
     *   <li><code>-show</code>  affiche un paneau indiquant qu'un calcul est en cours. Ce paneau
     *                           contient un bouton permettant l'arr�t du calcul � tout moment.</li>
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
