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
package fr.ird.io.corssh;

// Entrés/sorties
import java.io.File;
import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;

// Coordonnées spatio-temporelles
import java.util.Date;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

// Formatage
import java.util.Locale;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

// Ensembles
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;

// Logging
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.LogManager;

// Geotools
import org.geotools.cs.Ellipsoid;
import org.geotools.resources.XMath;
import org.geotools.resources.Utilities;
import org.geotools.resources.XRectangle2D;
import org.geotools.util.ProgressListener;
import org.geotools.gui.headless.ProgressPrinter;
import org.geotools.gui.headless.ProgressMailer;

// Seagis
import fr.ird.resources.XArray;
import fr.ird.resources.experimental.Resources;
import fr.ird.resources.experimental.ResourceKeys;


/**
 * Calcule les semi-variances de la hauteur (ou de l'anomalie de hauteur) de l'eau.
 * Le calcul sera effectué comme suit:
 *
 * <ul>
 *   <li>Pour chaque point <var>P<sub>i</sub></var>,  recherche tous les points
 *       <var>P<sub>j</sub></var> qui se trouvent dans le voisinage (spatial et
 *       temporel) de <var>P<sub>i</sub></var>.</li>
 *
 *   <li>Calcule les carrés des différences de hauteurs (ou d'anomalie de hauteurs) de
 *       l'eau entre <var>P<sub>i</sub></var> et chaque point <var>P<sub>j</sub></var>.
 *       Cette différence sera dénoté <var>dz²</var>. Calcule aussi les distances
 *       spatiales (<var>dx</var>) et l'écart de temps (<var>dt</var>).</li>
 *
 *   <li>Ajoute les <var>dz²</var> calculés dans un tableau de fréquences à trois
 *       dimensions. Ce tableau de fréquences contiendra les <var>dz²</var> moyens
 *       en fonction de (<var>dx</var>,<var>dt</var>) et de la latitude.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SemiVariance {
    /**
     * Retourne une approximation de l'arc cosinus.  Cette méthode n'existe que pour contourner
     * l'HALUCINANTE LENTEUR de la fonction {@link Math#acos} standard. Cette dernière, appelée
     * une seule fois au milieu de 8 autres fonctions trigonométriques, bouffe à elle seule 60%
     * du CPU!!!  A cause d'elle, les calculs de distances orthodronomiques peuvent prendre des
     * mois!  Cette fonction <code>acos(double)</code> tente de contourner le problème en
     * calculant une approximation de l'arc-cosinus, avec l'équation <code>acos=sqrt(1-a²)</code>.
     * Cette approximation est valide à 1% près pour des valeurs de <code>a</code> supérieures à
     * 0.97, ce qui correspond à des angles inférieures à 14° (sur la Terre, cet angle correspond
     * à des distances de 1555 kilomètres à l'équateur). Pour les valeurs de <var>a</var> inférieures
     * ou égales à 0.97, cette méthode utilise la fonction <code>Math.acos(double)</code> standard.
     */
    private static double acos(final double a) {
        return (a>0.97) ? Math.sqrt(1-a*a) : Math.acos(a);
    }

    /**
     * Fichier dans lequel placer un objet {@link MultiFilesParser}
     * pré-initialisé. C'est plus rapide que d'en construire un nouveau.
     */
    private static final String CACHE = "application-data/cache/MultiFilesParser.serialized";

    /**
     * Nombre maximal d'enregistrements à charger en mémoire à la fois. Plutôt que de charger
     * d'un coup toutes les données demandées par l'utilisateur, on ne chargera qu'un maximum
     * de <code>maxRecordCount</code>. On fera ensuite glisser la fenêtre des données chargées
     * de la date de départ vers la date de fin. <strong>Le nombre d'enregistrements doit être
     * suffisant pour couvrir une plage de temps d'au moins <code>2*T_INCREMENT*T_COUNT</code></strong>
     * (un enregistrement correspond à environ une seconde).
     */
    private static final int maxRecordCount = 77*24*60*60; // Environ 2½ mois; environ 128 Megs

    /**
     * Indique s'il faut aussi calculer les semi-variances. Si ce champ est
     * <code>false</code>, alors seul l'histogramme des hauteurs sera calculé,
     * ce qui est beaucoup plus rapide.
     */
    private static final boolean computeVariances=true;

    /**
     * Boîte de dialogue (ou autre composante) dans laquelle informer
     * des progrès des calculs, ou <code>null</code> s'il n'y en a pas.
     */
    private ProgressListener progress;

    /**
     * Forme géométrique délimitant une région dans laquelle on cherchera des points.
     * La largeur, la hauteur et la position de cette forme changeront constamment en
     * fonction du point étudié.
     */
    private final RectangularShape area = new XRectangle2D() {
        public Rectangle2D getBounds2D() {
            return this; // Slight optimization (safe in the 'Buffer' context).
        }
    };



    /** Facteur par lequel diviser la latitude.         */ private static final double L_FACTOR = 1;                        // degrés --> degrés
    /** Facteur par lequel diviser les écarts de temps. */ private static final double T_FACTOR = (24*60*60*1000);          // millisecondes --> jours
    /** Facteur par lequel diviser les distances.       */ private static final double X_FACTOR = 1000;                     // mètres --> kilomètres
    /** Facteur par lequel diviser les hauteurs.        */ private static final double Z_FACTOR = Parser.METRES_TO_INT/100; // millimètres --> centimètres

    // IMPORTANT: La plage de temps T_INCREMENT*T_COUNT doit correspondre à un
    //            nombre d'échantillons inférieur à {@link #maxRecordCount}.
    /** Intervalle de latitude, en degrés.     */ private static final float L_INCREMENT = 0.25f;   // Quart de degré.
    /** Intervalle de temps, en millisecondes. */ private static final int   T_INCREMENT = 7319350; // Environ 2h02
    /** Intervalle de distance, en mètres.     */ private static final int   X_INCREMENT = 6200;    // Environ 6.2 km
    /** Intervalle de hauteur, en millimètres. */ private static final int   Z_INCREMENT = 1;       // 1 mm
    /** Nombre d'intervalles de latitude.      */ private static final int   L_COUNT     = 67*4;    // Couvre 0 à 67°
    /** Nombre d'intervalles de temps          */ private static final int   T_COUNT     = 236;     // Couvre 0 à 20 jours.
    /** Nombre d'intervalles de distance.      */ private static final int   X_COUNT     = 56;      // Couvre 0 à 347 km.
    /** Nombre d'intervalles de hauteur.       */ private static final int   Z_COUNT     = 6000;    // Couvre -300 à +300 cm.

    /** Nombre d'occurences de Z entre deux bornes. */ private final long  [] histogram = new long  [Z_COUNT];
    /** Somme des anomalies des dX (pour info).     */ private final double[] sumDXa    = new double[T_COUNT * X_COUNT * L_COUNT];
    /** Somme des anomalies des dT (pour info).     */ private final long  [] sumDTa    = new long  [T_COUNT * X_COUNT * L_COUNT];
    /** Somme des anomalies des dL (pour info).     */ private final float [] sumDLa    = new float [T_COUNT * X_COUNT * L_COUNT];
    /** Somme des (dZ)² pour un (dT,dX) donné.      */ private final long  [] sumDZsqr  = new long  [T_COUNT * X_COUNT * L_COUNT];
    /** Somme des |dZ| pour un (dT,dX) donné.       */ private final long  [] sumDZabs  = new long  [T_COUNT * X_COUNT * L_COUNT];
    /** Nombre de termes utilisés dans les sommes.  */ private final int   [] count     = new int   [T_COUNT * X_COUNT * L_COUNT];

    /*
     *     NOTE SUR LES RISQUES DE DEBORDEMENTS:
     *     -------------------------------------
     *     Les valeurs légales de la hauteur de l'eau dans un fichier CORSSH
     *     peuvent aller de -300000 à +300000 millimètres.  Dans le pire des
     *     cas, on peut additionner plus de  30744 milliards  de ces valeurs
     *     dans un entier de type 'long'. Avec une donnée toute les secondes,
     *     ça représente près d'un million d'années. Mais le nombre de données
     *     au carré que l'on peut représenter  est limitée à un peu moins de
     *     102 millions, soit 3 années. Dans la pratique, on peut travailler
     *     sur des périodes beaucoup plus longues puisque les hauteurs   (ou
     *     anomalies de hauteurs) de l'eau ont beaucoup moins de 300 mètres!
     */

    /**
     * Initialise un objet qui ne contiendra aucune donnée.
     */
    public SemiVariance() {
    }

    /**
     * Spécifie une boîte de dialogue (ou autre composante) dans laquelle informer
     * des progrès des calculs. La valeur <code>null</code> signifie que les progrès
     * ne doivent pas être reportés.
     */
    public synchronized void setProgress(final ProgressListener progress) {
        this.progress = progress;
    }

    /**
     * Retourne un objet {@link Parser} approprié pour lire les fichiers ou les répertoires spécifiés.
     * L'argument <code>files</code> peut contenir de simples fichiers de données, ou des répertoires
     * qui contiennent un ensemble de données. Dans ce dernier cas, tous les fichiers qui ont l'extension
     * ".DAT" seront pris en compte.
     *
     * @param  files Fichiers ou répertoires à ouvrir.
     * @throws IOException si des fichiers ou des répertoires n'ont pas pu être ouvert.
     */
    private static synchronized Parser getParser(final File[] files) throws IOException {
        Map<File,Parser>  parserMap = null;
        boolean            modified = false;
        final List<Parser>  parsers = new ArrayList<Parser>();
        final File        cacheFile = new File(CACHE);
        for (int i=0; i<files.length; i++) {
            Parser parser;
            final File file = files[i];
            if (file.isDirectory()) {
                /*
                 * Si le fichier spécifié est un répertoire, tente de récupérer
                 * un objet {@link MultiFileParser} qui aurait déjà été construit
                 * lors d'une exécution antérieure.
                 */
                if (parserMap == null) {
                    if (cacheFile.isFile()) try {
                        final ObjectInputStream in = new ObjectInputStream(new FileInputStream(cacheFile));
                        parserMap = (Map) in.readObject(); // TODO: unchecked assignment
                        in.close();
                    } catch (ClassNotFoundException exception) {
                        IOException e=new IOException(exception.getLocalizedMessage());
                        e.initCause(exception);
                        throw e;
                    }
                    if (parserMap == null) {
                        parserMap = new HashMap<File,Parser>();
                    }
                }
                parser = parserMap.get(file);
                if (parser == null) {
                    parser = new MultiFilesParser(file);
                    parserMap.put(file, parser);
                    modified = true;
                }
            } else if (file.isFile()) {
                parser = new FileParser(file);
            } else {
                throw new FileNotFoundException(Resources.format(
                            ResourceKeys.ERROR_FILE_NOT_FOUND_$1, file.getPath()));
            }
            parsers.add(parser);
        }
        if (modified) {
            final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cacheFile));
            out.writeObject(parserMap);
            out.close();
        }
        return InterleavedParser.getInstance((Parser[])parsers.toArray(new Parser[parsers.size()]));
    }

    /**
     * Lance le calcule des semi-variances en utilisant les points compris dans la plage de
     * coordonnées spatio-temporelles spécifiée. Les points seront puisés dans les fichiers ou
     * répertoires spécifiés. Si <code>file</code> est un répertoire, alors tous les fichiers
     * avec l'extension ".DAT" dans ce répertoire seront pris en compte.
     *
     * @param  files          Fichiers ou répertoires des données à utiliser.
     * @param  geographicArea Coordonnées de la région géographique d'étude. Toutes
     *                        les données qui ne sont pas dans cette région seront ignorées.
     * @param  startTime      Début (inclusif) de la plage de temps des données à considérer.
     * @param  endTime        Fin (exclusif) de la plage de temps des données à considérer.
     * @throws IOException    si le fichier ou le répertoire n'a pas pu être ouvert,
     *                        ou si une opération de lecture ou d'écriture a échouée.
     * @throws ArithmeticException s'il y a eu un débordement de capacité.
     */
    public void compute(final File[] files, final Shape geographicArea, Date startTime, Date endTime)
            throws IOException, ArithmeticException
    {
        final Parser parser = getParser(files);
        final Buffer buffer = new Buffer(parser, geographicArea);
        final Date  minTime = parser.getStartTime();
        final Date  maxTime = parser.getEndTime();
        maxTime.setTime(maxTime.getTime()+1);
        if (minTime!=null && minTime.after(startTime)) startTime=minTime;
        if (maxTime!=null && maxTime.before( endTime))   endTime=maxTime;
        compute(buffer, startTime, endTime);
        buffer.close();
    }

    /**
     * Lance le calcule des semi-variances en utilisant les points compris dans la plage
     * de temps spécifiée. Si d'anciennes statistiques étaient déjà en mémoire, alors les
     * nouvelles statistiques s'ajouteront aux anciennes.
     *
     * @param    buffer Buffer contenant les points à utiliser pour les calculs.
     * @param startTime Début (inclusif) de la plage de temps des données à considérer.
     * @param   endTime   Fin (exclusif) de la plage de temps des données à considérer.
     * @throws IOException si une opération de lecture ou d'écriture a échouée.
     * @throws ArithmeticException s'il y a eu un débordement de capacité.
     */
    private synchronized void compute(final Buffer buffer, final Date startTime, final Date endTime)
            throws IOException, ArithmeticException
    {
        Parser.logger.entering(getClass().getName(), "compute", new Date[] {startTime, endTime});
        /*
         * Initialisation. On utilisera des copies locales de la plupart des variables
         * afin d'améliorer leurs temps d'accès et aussi pour être certain qu'elles ne
         * seront pas modifiées en cours de route.
         */
        final RectangularShape      area = this.area;
        final ProgressListener  progress = this.progress;
        final long  []          sumDTa   = this.sumDTa;
        final double[]          sumDXa   = this.sumDXa;
        final float []          sumDLa   = this.sumDLa;
        final long  []          sumDZabs = this.sumDZabs;
        final long  []          sumDZsqr = this.sumDZsqr;
        final int   []          count    = this.count;
        final long  []         histogram = this.histogram;
        final boolean   computeVariances = this.computeVariances;
        final int                   zmin = histogram.length * Z_INCREMENT / -2;
        final long       startTimeMillis = startTime.getTime();
        final long         endTimeMillis =   endTime.getTime();
        final float        timeToPercent = 100f/(endTimeMillis-startTimeMillis);
        final double        searchRadius = ((double)X_INCREMENT)*(X_COUNT+1);
        final long            timeMargin = (  (long)T_INCREMENT)*(T_COUNT+1);
        final Date       startSearchTime = new Date(0);
        final Date         endSearchTime = new Date(0);
        final double semiMajorAxisLength;
        final double semiMinorAxisLength;
        if (true) {
            final Ellipsoid ellipsoid = Parser.getCoordinateSystem().getHorizontalDatum().getEllipsoid();
            semiMajorAxisLength       = ellipsoid.getSemiMajorAxis();
            semiMinorAxisLength       = ellipsoid.getSemiMinorAxis();
        }
        if (progress != null) {
            progress.setDescription(Resources.format(ResourceKeys.COMPUTING_STATISTICS));
            progress.started();
        }
        /*
         * Démarre la boucle principale.  Cette boucle remplira un buffer ({@link Buffer})
         * avec une partie seulement des données nécessaires. La partie des données chargée
         * fait une "fenêtre", que l'on déplacera progressivement dans le temps.  Il faudra
         * garder une marge de part et d'autre de la fenêtre (<code>timeMargin</code>) afin
         * de pouvoir faire les calculs qui ont besoin de données avant et après la date de
         * la donnée examinée.
         *
         * <blockquote><pre>
         *
         *     +---------------+---------------------+---------------+
         *     |  Zone tampon  |  Données à traiter  |  Zone tampon  |
         *     +---------------+---------------------+---------------+
         *     |^ TIME_MARGIN ^|                     |^ TIME_MARGIN ^|
         *
         * </pre></blockquote>
         */
        long lastTimeCheck = startTimeMillis-1;
        long startWindowTime = startTimeMillis;
window: while (startWindowTime < endTimeMillis) {
            final long endWindowTime;
            switch (buffer.setTimeRange(new Date(startWindowTime-timeMargin),
                                        new Date(  endTimeMillis+timeMargin), maxRecordCount))
            {
                case Buffer.EOF_REACHED:        // fall through
                case Buffer.ENDTIME_REACHED:    endWindowTime = endTimeMillis;                            break;
                case Buffer.MAXRECORDS_REACHED: endWindowTime = buffer.getEndTime().getTime()-timeMargin; break;
                default: throw new IllegalStateException(); // Should not happen
            }
            assert endWindowTime   <= endTimeMillis;
            assert startWindowTime <  endWindowTime;
            final int recordCount = buffer.getRecordCount();
            for (int i=buffer.search(new Date(startWindowTime)); i<recordCount; i++) {
                /*
                 * Construit un histogramme de la hauteur (ou de l'anomalie de la hauteur)
                 * de l'eau en utilisant tous les points du buffer qui se trouvent dans la
                 * fenêtre temporelle courante. Si on n'est plus dans cette fenêtre, alors
                 * on déplacera la fenêtre (boucle 'window').
                 */
                final Point2D point = buffer.getPoint (i);
                final long    time  = buffer.getMillis(i);
                final int     value = buffer.getField (i);
                if (progress!=null && (i&0x0FFF)==0) {
                    progress.progress((time-startTimeMillis)*timeToPercent);
                }
                if (time < lastTimeCheck) { // On accepte les dates identiques
                    throw new IOException(Resources.format(ResourceKeys.ERROR_DATES_NOT_INCREASING));
                }
                if (time >= endWindowTime) {
                    assert(startWindowTime!=time); // Boucle sans fin.
                    if (lastTimeCheck >= startWindowTime) {
                        final DateFormat  dateFormat = Parser.getDateTimeInstance();
                        Parser.log(getClass().getName(), "compute", Resources.format(ResourceKeys.COMPUTATION_DONE_$2,
                                   dateFormat.format(new Date(startWindowTime)),
                                   dateFormat.format(new Date(  lastTimeCheck))));
                    }
                    startWindowTime = time;
                    continue window;
                }
                lastTimeCheck = time;
                histogram[Math.max(0, Math.min(histogram.length-1, (value-zmin)/Z_INCREMENT))]++;
                /*
                 * Si l'utilisateur avait demandé à calculer non pas un simple histogramme de la
                 * hauteur de l'eau,  mais aussi des statistiques sur les différences de hauteur
                 * entre ce point et les points avoisinant, alors il faudra examiner la liste de
                 * tous les points qui se trouvent dans le voisinage de <code>point</code>.   On
                 * calulera ensuite  la différence de hauteur en fonction de l'écart de temps ou
                 * de la distance, comme une fonction <code>dz(dt,dx)</code>.
                 */
                if (computeVariances) {
                    final double             x_degrees = point.getX();
                    final double             y_degrees = point.getY();
                    double                   y_radians = Math.toRadians(y_degrees);
                    final double                 sin_y = Math.sin(y_radians);
                    final double                 cos_y = Math.cos(y_radians);
                    final double inverseApparentRadius = XMath.hypot(sin_y/semiMajorAxisLength, cos_y/semiMinorAxisLength);
                    final double      searchAreaHeight = Math.toDegrees(searchRadius*inverseApparentRadius);
                    final double      searchAreaWidth  = searchAreaHeight/cos_y;
                    /*
                     * Définit les limites spatio-temporelle de la région dans laquelle on recherchera
                     * des points.  Il s'agit d'une boîte tridimensionnelle (2 dimensions spatiales et
                     * une dimension temporelle) qui permet d'appliquer rapidement un premier filtrage
                     * des points avant de faire couteux calcul de distance plus bas.
                     */
                    area.setFrame(x_degrees-searchAreaWidth, y_degrees-searchAreaHeight, 2*searchAreaWidth, 2*searchAreaHeight);
                    startSearchTime.setTime(time+1);
                    endSearchTime  .setTime(time+timeMargin);
                    final Iterator nears=buffer.getPointsInside(area, startSearchTime, endSearchTime);
                    /*
                     * Examine maintenant tous les points qui ont été trouvés dans le voisinage du
                     * point 'point'. Dans cette boucle, les points examinés s'appellent 'nears'.
                     *
                     * NOTE: Le code ci-dessous contient des expressions constantes qui pourraient
                     *       être sorties de la boucle (notamment les calculs de 'dl' et 'indexDL').
                     *       On les laisse toutefois dans la boucle pour faciliter la lecture et la
                     *       maintenance du code. On compte sur le compilateur pour sortir lui-même
                     *       les expressions constantes de la boucle.
                     */
                    while (nears.next()) {
                        final long near_time=nears.getTime();
                        final long  dt = near_time-time; assert(dt>0);
                        final float dl = Math.abs((float)y_degrees);
                        final int   dz = nears.getField()-value; // (Donnée après)-(donnée maintenant)
                        y_radians = Math.toRadians(nears.getY());
                        final double dx = acos(sin_y*Math.sin(y_radians) +
                                               cos_y*Math.cos(y_radians) *
                                               Math.cos(Math.toRadians(nears.getX()-x_degrees))) / inverseApparentRadius;
                        if (!Double.isNaN(dx)) {
                            final int indexDT = (int)((dt+T_INCREMENT/2)/T_INCREMENT); if (indexDT>=T_COUNT) continue;
                            final int indexDX = (int)((dx+X_INCREMENT/2)/X_INCREMENT); if (indexDX>=X_COUNT) continue;
                            final int indexDL = (int)((dl+L_INCREMENT/2)/L_INCREMENT); if (indexDL>=L_COUNT) continue;
                            final int indexDZ = (indexDL*T_COUNT + indexDT)*X_COUNT + indexDX;
                            /*
                             * Statistiques calculées: |dz| et dz²
                             * Note: il est inutile de calculer dz (valeur signée) car elle est presque
                             *       toujours proche de 0.  De même, le calcul de la déviation standard
                             *       (environ sqrt(dz²-dz*dz)) est presque identique à la valeur RMS.
                             */
                            sumDTa  [indexDZ] += (dt - indexDT*T_INCREMENT);
                            sumDXa  [indexDZ] += (dx - indexDX*X_INCREMENT);
                            sumDLa  [indexDZ] += (dl - indexDL*L_INCREMENT);
                            sumDZabs[indexDZ] += Math.abs(dz);
                            sumDZsqr[indexDZ] += (long)dz*(long)dz;
                            count   [indexDZ]++;

                            if (sumDZsqr[indexDZ] < 0) {
                                // {@link #write} écrira "NaN" pour cette valeur (parce que Math.sqrt(-1)==NaN).
                                final ArithmeticException exception = new ArithmeticException(Resources.format(ResourceKeys.ERROR_OVERFLOW));
                                Parser.logger.throwing(getClass().getName(), "compute", exception);
                                throw exception;
                            }
                            assert count   [indexDZ] >= 0;
                            assert sumDZabs[indexDZ] >= 0;
                            assert sumDZsqr[indexDZ] >= 0; // Vérifie les débordements de capacité.
                        }
                    }
                }
            }
            /*
             * On a balayé tous les enregistrement en mémoire  sans arriver à la date
             * de fin. Cette situation ne devrait jamais se produire (puisque le test
             * <code>(time >= endWindowTime)</code> devrait arrêter la boucle au plus
             * tard sur le dernier enregistrement) sauf... si l'utilisateur a spécifié
             * une plage de temps dans laquelle il n'y a aucune donnée. Dans ce cas,
             * <code>recordCount==0</code>.
             */
            break;
        }
        if (progress != null) {
            progress.complete();
        }
        Parser.logger.exiting(getClass().getName(), "compute");
    }

    /**
     * Retourne le nombre d'enregistrements qui
     * ont servit au calcul des statistiques.
     */
    public long getRecordCount() {
        long n=0;
        for (int i=histogram.length; --i>=0;) {
            n += histogram[i];
        }
        return n;
    }

    /**
     * Retourne le nombre de paires d'enregistrements qui ont servit
     * aux calculs des statistiques. Les données prises en compte pour
     * chaque paire comprennent la distance entre deux points, l'écart
     * de temps et la différence de hauteur de d'anomalie de hauteur.
     */
    public long getPairCount() {
        long n=0;
        for (int i=count.length; --i>=0;) {
            n += count[i];
        }
        return n;
    }

    /**
     * Ecrit dans des fichiers les résultats des calculs. Cette méthode
     * peut produire deux fichiers:
     *
     * <ul>
     *   <li>Le premier fichier (<code>histogram</code>) contient un tableau des fréquences des hauteurs de l'eau
     *   <var>z</var> entre deux bornes <var>z<sub>i</sub></var> et <var>z<sub>i+1</sub></var>. Le tableau produit
     *   aura deux colonnes et plusieurs centaines de lignes. La première colonne contient la valeur <var>z<sub>i+½</sub></var>
     *   entre deux bornes, tandis que la seconde colonne contient le nombre d'occurence de <var>z</var> autour de
     *   cette valeur. Un traçage de la deuxième colonne en fonction de la première devrait donner une courbe en
     *   forme de cloche.</li>
     *
     *   <li>Le second fichier <code>semiVariance</code> contient une liste de semi-variances (<var>dz²</var>)
     *       en fonction de la latitude, de la distance (<var>dx</var>) et de l'écart de temps (<var>dt</var>)
     *       entre deux points.</li>
     * </ul>
     *
     * @param  histogram    Fichier dans lequel écrire l'histogramme, ou <code>null</code> pour ne pas l'écrire.
     * @param  semiVariance Fichier dans lequel écrire les semi-variances, ou <code>null</code> pour ne pas les écrires.
     * @throws IOException si l'écriture a échoué.
     */
    public synchronized void write(final File histogram, final File semiVariance) throws IOException {
        final String lineSeparator = System.getProperty("line.separator","\n");
        final NumberFormat  format = NumberFormat.getNumberInstance();
        format.setGroupingUsed(false);
        format.setMaximumFractionDigits(12);
        if (format instanceof DecimalFormat) {
            final DecimalFormat        decimal = (DecimalFormat) format;
            final DecimalFormatSymbols symbols = decimal.getDecimalFormatSymbols();
            symbols.setNaN("#N/A");
            decimal.setDecimalFormatSymbols(symbols);
        }
        /*
         * Ecrit un tableau des fréquences des hauteurs de l'eau <var>z</var> entre deux bornes
         * <var>z<sub>i</sub></var> et <var>z<sub>i+1</sub></var>. Le tableau produit aura deux
         * colonnes et plusieurs centaines de lignes. La première colonne contient la valeur
         * <var>z<sub>i+½</sub></var> entre deux bornes, tandis que la seconde colonne contient
         * le nombre d'occurence de <var>z</var> autour de cette valeur.
         */
        if (histogram != null) {
            final int   zmin = this.histogram.length * Z_INCREMENT / -2;
            final Writer out = new BufferedWriter(new FileWriter(histogram));
            out.write("Hauteur (cm)\tCompte");
            out.write(lineSeparator);
            for (int i=0; i<this.histogram.length; i++) {
                out.write(format.format(((i+0.5)*Z_INCREMENT + zmin) / Z_FACTOR));
                out.write('\t');
                out.write(format.format(this.histogram[i]));
                out.write(lineSeparator);
            }
            out.close();
        }
        /*
         * Ecrit un tableau de la différence moyenne de la hauteur de l'eau (<var>dz</var>)
         * entre deux points en fonction de la latitude, de la distance (<var>dx</var>) et
         * de l'écart de temps (<var>dt</var>) entre ces deux points.  Ce calcul n'est pas
         * disponible si <code>setComputeVariances(false)</code> a été appelée.
         */
        if (semiVariance != null) {
            final Writer out = new BufferedWriter(new FileWriter(semiVariance));
            out.write("Latitude (°)\t"            +
                      "Écart de temps (jours)\t"  +
                      "Distance (km)\t"           +
                      "Moyenne des écarts (cm)\t" +
                      "RMS (cm)\t"                +
                      "Nombre");
            out.write(lineSeparator);
            for (int li=0; li<L_COUNT; li++) {
                for (int ti=0; ti<T_COUNT; ti++) {
                    for (int xi=0; xi<X_COUNT; xi++) {
                        final int i=(li*T_COUNT + ti)*X_COUNT + xi;
                        final int n = count[i];
                        if (n != 0) {
                            for (int column=0; column<=5; column++) {
                                final double value;
                                switch (column) {
                                    case 0: value=(((double)sumDLa  [i])/n + li*L_INCREMENT)     / L_FACTOR; break;
                                    case 1: value=(((double)sumDTa  [i])/n + ti*T_INCREMENT)     / T_FACTOR; break;
                                    case 2: value=(((double)sumDXa  [i])/n + xi*X_INCREMENT)     / X_FACTOR; break;
                                    case 3: value=(((double)sumDZabs[i])/n)                      / Z_FACTOR; break;
                                    case 4: value=Math.sqrt(((double)sumDZsqr[i])/n)             / Z_FACTOR; break;
                                    case 5: value=n;                                                         break;
                                    default: throw new AssertionError(column);
                                }
                                if (column!=0) {
                                    out.write('\t');
                                }
                                out.write(format.format(value));
                            }
                            out.write(lineSeparator);
                        }
                    }
                }
            }
            out.close();
        }
    }

    /**
     * Retourne une chaîne de caractères donnant
     * quelques informations sur cet objet.
     */
    public String toString() {
        final StringBuffer buffer=new StringBuffer(Utilities.getShortClassName(this));
        buffer.append('[');
        buffer.append(getRecordCount());
        buffer.append(" records");
        if (computeVariances) {
            buffer.append(", ");
            buffer.append(getPairCount());
            buffer.append(" pairs");
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Démarre le calcul. Les arguments acceptés sont (dans l'ordre):
     *
     * <ul>
     *    <li>Date de début (valeur par défaut: <code>01/01/1992 00:00</code>)</li>
     *    <li>Date de fin   (valeur par défaut: <code>01/01/2002 00:00</code>)</li>
     *    <li>Répertoire des fichiers CORSSH d'un premier satellite (Topex-Poseidon).</li>
     *    <li>Répertoire des fichiers CORSSH d'un deuxième satellite (ERS-2).</li>
     *    <li>Répertoire des fichiers CORSSH d'un troisième satellite (ERS-1).</li>
     * </ul>
     *
     * Note: Sous Windows NT, le calcul peut être lancé en arrière-plan avec
     *
     * <blockquote><pre>
     *     start /b /wait /low java -server fr.ird.io.corssh.SemiVariance <i>[arguments]</i>
     * </pre></blockquote>
     */
    public static void main(final String[] args) throws Exception {
        String startTimeText = "01/01/1992 00:00";
        String   endTimeText = "01/01/2002 00:00";
        String[] directories = new String[] {
            "E:/PELOPS/Images/CORSSH/Topex-Poseidon",
            "E:/PELOPS/Images/CORSSH/ERS-1",
            "E:/PELOPS/Images/CORSSH/ERS-2"
        };
        switch (args.length) {
            default:  directories = XArray.remove((String[])args.clone(), 0, 2); // fall through
            case 2:   endTimeText = args[1]; // fall through
            case 1: startTimeText = args[0]; // fall through
            case 0:  break;
        }
        final DateFormat         format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.FRANCE);
        final Date            startTime = format.parse(startTimeText);
        final Date              endTime = format.parse(  endTimeText);
        final Shape      geographicArea = new Rectangle2D.Double(0, -90, 360, 180);
        final SemiVariance        stats = new SemiVariance();
        final ProgressListener progress = new ProgressMailer("smtp.ird.teledetection.fr", "martin.desruisseaux@teledetection.fr");
        final File[]     directoryFiles = new File[directories.length];
        for (int i=0; i<directories.length; i++) directoryFiles[i]=new File(directories[i]);
        stats.setProgress(progress);
        try {
            stats.compute(directoryFiles, geographicArea, startTime, endTime);
        }
        catch (ArithmeticException exception) {
            progress.exceptionOccurred(exception);
            // Enregistre quand-même
        }
        catch (Throwable error) {
            System.gc();
            progress.exceptionOccurred(error);
            return;
        }
        stats.write(new File("Histogram.txt"), new File("Statistics.txt"));
    }
}
