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
package fr.ird.io.corssh;

// Entr�s/sorties
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

// Coordonn�es spatio-temporelles
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
 * Le calcul sera effectu� comme suit:
 *
 * <ul>
 *   <li>Pour chaque point <var>P<sub>i</sub></var>,  recherche tous les points
 *       <var>P<sub>j</sub></var> qui se trouvent dans le voisinage (spatial et
 *       temporel) de <var>P<sub>i</sub></var>.</li>
 *
 *   <li>Calcule les carr�s des diff�rences de hauteurs (ou d'anomalie de hauteurs) de
 *       l'eau entre <var>P<sub>i</sub></var> et chaque point <var>P<sub>j</sub></var>.
 *       Cette diff�rence sera d�not� <var>dz�</var>. Calcule aussi les distances
 *       spatiales (<var>dx</var>) et l'�cart de temps (<var>dt</var>).</li>
 *
 *   <li>Ajoute les <var>dz�</var> calcul�s dans un tableau de fr�quences � trois
 *       dimensions. Ce tableau de fr�quences contiendra les <var>dz�</var> moyens
 *       en fonction de (<var>dx</var>,<var>dt</var>) et de la latitude.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SemiVariance {
    /**
     * Retourne une approximation de l'arc cosinus.  Cette m�thode n'existe que pour contourner
     * l'HALUCINANTE LENTEUR de la fonction {@link Math#acos} standard. Cette derni�re, appel�e
     * une seule fois au milieu de 8 autres fonctions trigonom�triques, bouffe � elle seule 60%
     * du CPU!!!  A cause d'elle, les calculs de distances orthodronomiques peuvent prendre des
     * mois!  Cette fonction <code>acos(double)</code> tente de contourner le probl�me en
     * calculant une approximation de l'arc-cosinus, avec l'�quation <code>acos=sqrt(1-a�)</code>.
     * Cette approximation est valide � 1% pr�s pour des valeurs de <code>a</code> sup�rieures �
     * 0.97, ce qui correspond � des angles inf�rieures � 14� (sur la Terre, cet angle correspond
     * � des distances de 1555 kilom�tres � l'�quateur). Pour les valeurs de <var>a</var> inf�rieures
     * ou �gales � 0.97, cette m�thode utilise la fonction <code>Math.acos(double)</code> standard.
     */
    private static double acos(final double a) {
        return (a>0.97) ? Math.sqrt(1-a*a) : Math.acos(a);
    }

    /**
     * Fichier dans lequel placer un objet {@link MultiFilesParser}
     * pr�-initialis�. C'est plus rapide que d'en construire un nouveau.
     */
    private static final String CACHE = "application-data/cache/MultiFilesParser.serialized";

    /**
     * Nombre maximal d'enregistrements � charger en m�moire � la fois. Plut�t que de charger
     * d'un coup toutes les donn�es demand�es par l'utilisateur, on ne chargera qu'un maximum
     * de <code>maxRecordCount</code>. On fera ensuite glisser la fen�tre des donn�es charg�es
     * de la date de d�part vers la date de fin. <strong>Le nombre d'enregistrements doit �tre
     * suffisant pour couvrir une plage de temps d'au moins <code>2*T_INCREMENT*T_COUNT</code></strong>
     * (un enregistrement correspond � environ une seconde).
     */
    private static final int maxRecordCount = 77*24*60*60; // Environ 2� mois; environ 128 Megs

    /**
     * Indique s'il faut aussi calculer les semi-variances. Si ce champ est
     * <code>false</code>, alors seul l'histogramme des hauteurs sera calcul�,
     * ce qui est beaucoup plus rapide.
     */
    private static final boolean computeVariances=true;

    /**
     * Bo�te de dialogue (ou autre composante) dans laquelle informer
     * des progr�s des calculs, ou <code>null</code> s'il n'y en a pas.
     */
    private ProgressListener progress;

    /**
     * Forme g�om�trique d�limitant une r�gion dans laquelle on cherchera des points.
     * La largeur, la hauteur et la position de cette forme changeront constamment en
     * fonction du point �tudi�.
     */
    private final RectangularShape area = new XRectangle2D() {
        public Rectangle2D getBounds2D() {
            return this; // Slight optimization (safe in the 'Buffer' context).
        }
    };



    /** Facteur par lequel diviser la latitude.         */ private static final double L_FACTOR = 1;                        // degr�s --> degr�s
    /** Facteur par lequel diviser les �carts de temps. */ private static final double T_FACTOR = (24*60*60*1000);          // millisecondes --> jours
    /** Facteur par lequel diviser les distances.       */ private static final double X_FACTOR = 1000;                     // m�tres --> kilom�tres
    /** Facteur par lequel diviser les hauteurs.        */ private static final double Z_FACTOR = Parser.METRES_TO_INT/100; // millim�tres --> centim�tres

    // IMPORTANT: La plage de temps T_INCREMENT*T_COUNT doit correspondre � un
    //            nombre d'�chantillons inf�rieur � {@link #maxRecordCount}.
    /** Intervalle de latitude, en degr�s.     */ private static final float L_INCREMENT = 0.25f;   // Quart de degr�.
    /** Intervalle de temps, en millisecondes. */ private static final int   T_INCREMENT = 7319350; // Environ 2h02
    /** Intervalle de distance, en m�tres.     */ private static final int   X_INCREMENT = 6200;    // Environ 6.2 km
    /** Intervalle de hauteur, en millim�tres. */ private static final int   Z_INCREMENT = 1;       // 1 mm
    /** Nombre d'intervalles de latitude.      */ private static final int   L_COUNT     = 67*4;    // Couvre 0 � 67�
    /** Nombre d'intervalles de temps          */ private static final int   T_COUNT     = 236;     // Couvre 0 � 20 jours.
    /** Nombre d'intervalles de distance.      */ private static final int   X_COUNT     = 56;      // Couvre 0 � 347 km.
    /** Nombre d'intervalles de hauteur.       */ private static final int   Z_COUNT     = 6000;    // Couvre -300 � +300 cm.

    /** Nombre d'occurences de Z entre deux bornes. */ private final long  [] histogram = new long  [Z_COUNT];
    /** Somme des anomalies des dX (pour info).     */ private final double[] sumDXa    = new double[T_COUNT * X_COUNT * L_COUNT];
    /** Somme des anomalies des dT (pour info).     */ private final long  [] sumDTa    = new long  [T_COUNT * X_COUNT * L_COUNT];
    /** Somme des anomalies des dL (pour info).     */ private final float [] sumDLa    = new float [T_COUNT * X_COUNT * L_COUNT];
    /** Somme des (dZ)� pour un (dT,dX) donn�.      */ private final long  [] sumDZsqr  = new long  [T_COUNT * X_COUNT * L_COUNT];
    /** Somme des |dZ| pour un (dT,dX) donn�.       */ private final long  [] sumDZabs  = new long  [T_COUNT * X_COUNT * L_COUNT];
    /** Nombre de termes utilis�s dans les sommes.  */ private final int   [] count     = new int   [T_COUNT * X_COUNT * L_COUNT];

    /*
     *     NOTE SUR LES RISQUES DE DEBORDEMENTS:
     *     -------------------------------------
     *     Les valeurs l�gales de la hauteur de l'eau dans un fichier CORSSH
     *     peuvent aller de -300000 � +300000 millim�tres.  Dans le pire des
     *     cas, on peut additionner plus de  30744 milliards  de ces valeurs
     *     dans un entier de type 'long'. Avec une donn�e toute les secondes,
     *     �a repr�sente pr�s d'un million d'ann�es. Mais le nombre de donn�es
     *     au carr� que l'on peut repr�senter  est limit�e � un peu moins de
     *     102 millions, soit 3 ann�es. Dans la pratique, on peut travailler
     *     sur des p�riodes beaucoup plus longues puisque les hauteurs   (ou
     *     anomalies de hauteurs) de l'eau ont beaucoup moins de 300 m�tres!
     */

    /**
     * Initialise un objet qui ne contiendra aucune donn�e.
     */
    public SemiVariance() {
    }

    /**
     * Sp�cifie une bo�te de dialogue (ou autre composante) dans laquelle informer
     * des progr�s des calculs. La valeur <code>null</code> signifie que les progr�s
     * ne doivent pas �tre report�s.
     */
    public synchronized void setProgress(final ProgressListener progress) {
        this.progress = progress;
    }

    /**
     * Retourne un objet {@link Parser} appropri� pour lire les fichiers ou les r�pertoires sp�cifi�s.
     * L'argument <code>files</code> peut contenir de simples fichiers de donn�es, ou des r�pertoires
     * qui contiennent un ensemble de donn�es. Dans ce dernier cas, tous les fichiers qui ont l'extension
     * ".DAT" seront pris en compte.
     *
     * @param  files Fichiers ou r�pertoires � ouvrir.
     * @throws IOException si des fichiers ou des r�pertoires n'ont pas pu �tre ouvert.
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
                 * Si le fichier sp�cifi� est un r�pertoire, tente de r�cup�rer
                 * un objet {@link MultiFileParser} qui aurait d�j� �t� construit
                 * lors d'une ex�cution ant�rieure.
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
     * coordonn�es spatio-temporelles sp�cifi�e. Les points seront puis�s dans les fichiers ou
     * r�pertoires sp�cifi�s. Si <code>file</code> est un r�pertoire, alors tous les fichiers
     * avec l'extension ".DAT" dans ce r�pertoire seront pris en compte.
     *
     * @param  files          Fichiers ou r�pertoires des donn�es � utiliser.
     * @param  geographicArea Coordonn�es de la r�gion g�ographique d'�tude. Toutes
     *                        les donn�es qui ne sont pas dans cette r�gion seront ignor�es.
     * @param  startTime      D�but (inclusif) de la plage de temps des donn�es � consid�rer.
     * @param  endTime        Fin (exclusif) de la plage de temps des donn�es � consid�rer.
     * @throws IOException    si le fichier ou le r�pertoire n'a pas pu �tre ouvert,
     *                        ou si une op�ration de lecture ou d'�criture a �chou�e.
     * @throws ArithmeticException s'il y a eu un d�bordement de capacit�.
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
     * de temps sp�cifi�e. Si d'anciennes statistiques �taient d�j� en m�moire, alors les
     * nouvelles statistiques s'ajouteront aux anciennes.
     *
     * @param    buffer Buffer contenant les points � utiliser pour les calculs.
     * @param startTime D�but (inclusif) de la plage de temps des donn�es � consid�rer.
     * @param   endTime   Fin (exclusif) de la plage de temps des donn�es � consid�rer.
     * @throws IOException si une op�ration de lecture ou d'�criture a �chou�e.
     * @throws ArithmeticException s'il y a eu un d�bordement de capacit�.
     */
    private synchronized void compute(final Buffer buffer, final Date startTime, final Date endTime)
            throws IOException, ArithmeticException
    {
        Parser.logger.entering(getClass().getName(), "compute", new Date[] {startTime, endTime});
        /*
         * Initialisation. On utilisera des copies locales de la plupart des variables
         * afin d'am�liorer leurs temps d'acc�s et aussi pour �tre certain qu'elles ne
         * seront pas modifi�es en cours de route.
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
         * D�marre la boucle principale.  Cette boucle remplira un buffer ({@link Buffer})
         * avec une partie seulement des donn�es n�cessaires. La partie des donn�es charg�e
         * fait une "fen�tre", que l'on d�placera progressivement dans le temps.  Il faudra
         * garder une marge de part et d'autre de la fen�tre (<code>timeMargin</code>) afin
         * de pouvoir faire les calculs qui ont besoin de donn�es avant et apr�s la date de
         * la donn�e examin�e.
         *
         * <blockquote><pre>
         *
         *     +---------------+---------------------+---------------+
         *     |  Zone tampon  |  Donn�es � traiter  |  Zone tampon  |
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
                 * fen�tre temporelle courante. Si on n'est plus dans cette fen�tre, alors
                 * on d�placera la fen�tre (boucle 'window').
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
                 * Si l'utilisateur avait demand� � calculer non pas un simple histogramme de la
                 * hauteur de l'eau,  mais aussi des statistiques sur les diff�rences de hauteur
                 * entre ce point et les points avoisinant, alors il faudra examiner la liste de
                 * tous les points qui se trouvent dans le voisinage de <code>point</code>.   On
                 * calulera ensuite  la diff�rence de hauteur en fonction de l'�cart de temps ou
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
                     * D�finit les limites spatio-temporelle de la r�gion dans laquelle on recherchera
                     * des points.  Il s'agit d'une bo�te tridimensionnelle (2 dimensions spatiales et
                     * une dimension temporelle) qui permet d'appliquer rapidement un premier filtrage
                     * des points avant de faire couteux calcul de distance plus bas.
                     */
                    area.setFrame(x_degrees-searchAreaWidth, y_degrees-searchAreaHeight, 2*searchAreaWidth, 2*searchAreaHeight);
                    startSearchTime.setTime(time+1);
                    endSearchTime  .setTime(time+timeMargin);
                    final Iterator nears=buffer.getPointsInside(area, startSearchTime, endSearchTime);
                    /*
                     * Examine maintenant tous les points qui ont �t� trouv�s dans le voisinage du
                     * point 'point'. Dans cette boucle, les points examin�s s'appellent 'nears'.
                     *
                     * NOTE: Le code ci-dessous contient des expressions constantes qui pourraient
                     *       �tre sorties de la boucle (notamment les calculs de 'dl' et 'indexDL').
                     *       On les laisse toutefois dans la boucle pour faciliter la lecture et la
                     *       maintenance du code. On compte sur le compilateur pour sortir lui-m�me
                     *       les expressions constantes de la boucle.
                     */
                    while (nears.next()) {
                        final long near_time=nears.getTime();
                        final long  dt = near_time-time; assert(dt>0);
                        final float dl = Math.abs((float)y_degrees);
                        final int   dz = nears.getField()-value; // (Donn�e apr�s)-(donn�e maintenant)
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
                             * Statistiques calcul�es: |dz| et dz�
                             * Note: il est inutile de calculer dz (valeur sign�e) car elle est presque
                             *       toujours proche de 0.  De m�me, le calcul de la d�viation standard
                             *       (environ sqrt(dz�-dz*dz)) est presque identique � la valeur RMS.
                             */
                            sumDTa  [indexDZ] += (dt - indexDT*T_INCREMENT);
                            sumDXa  [indexDZ] += (dx - indexDX*X_INCREMENT);
                            sumDLa  [indexDZ] += (dl - indexDL*L_INCREMENT);
                            sumDZabs[indexDZ] += Math.abs(dz);
                            sumDZsqr[indexDZ] += (long)dz*(long)dz;
                            count   [indexDZ]++;

                            if (sumDZsqr[indexDZ] < 0) {
                                // {@link #write} �crira "NaN" pour cette valeur (parce que Math.sqrt(-1)==NaN).
                                final ArithmeticException exception = new ArithmeticException(Resources.format(ResourceKeys.ERROR_OVERFLOW));
                                Parser.logger.throwing(getClass().getName(), "compute", exception);
                                throw exception;
                            }
                            assert count   [indexDZ] >= 0;
                            assert sumDZabs[indexDZ] >= 0;
                            assert sumDZsqr[indexDZ] >= 0; // V�rifie les d�bordements de capacit�.
                        }
                    }
                }
            }
            /*
             * On a balay� tous les enregistrement en m�moire  sans arriver � la date
             * de fin. Cette situation ne devrait jamais se produire (puisque le test
             * <code>(time >= endWindowTime)</code> devrait arr�ter la boucle au plus
             * tard sur le dernier enregistrement) sauf... si l'utilisateur a sp�cifi�
             * une plage de temps dans laquelle il n'y a aucune donn�e. Dans ce cas,
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
     * aux calculs des statistiques. Les donn�es prises en compte pour
     * chaque paire comprennent la distance entre deux points, l'�cart
     * de temps et la diff�rence de hauteur de d'anomalie de hauteur.
     */
    public long getPairCount() {
        long n=0;
        for (int i=count.length; --i>=0;) {
            n += count[i];
        }
        return n;
    }

    /**
     * Ecrit dans des fichiers les r�sultats des calculs. Cette m�thode
     * peut produire deux fichiers:
     *
     * <ul>
     *   <li>Le premier fichier (<code>histogram</code>) contient un tableau des fr�quences des hauteurs de l'eau
     *   <var>z</var> entre deux bornes <var>z<sub>i</sub></var> et <var>z<sub>i+1</sub></var>. Le tableau produit
     *   aura deux colonnes et plusieurs centaines de lignes. La premi�re colonne contient la valeur <var>z<sub>i+�</sub></var>
     *   entre deux bornes, tandis que la seconde colonne contient le nombre d'occurence de <var>z</var> autour de
     *   cette valeur. Un tra�age de la deuxi�me colonne en fonction de la premi�re devrait donner une courbe en
     *   forme de cloche.</li>
     *
     *   <li>Le second fichier <code>semiVariance</code> contient une liste de semi-variances (<var>dz�</var>)
     *       en fonction de la latitude, de la distance (<var>dx</var>) et de l'�cart de temps (<var>dt</var>)
     *       entre deux points.</li>
     * </ul>
     *
     * @param  histogram    Fichier dans lequel �crire l'histogramme, ou <code>null</code> pour ne pas l'�crire.
     * @param  semiVariance Fichier dans lequel �crire les semi-variances, ou <code>null</code> pour ne pas les �crires.
     * @throws IOException si l'�criture a �chou�.
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
         * Ecrit un tableau des fr�quences des hauteurs de l'eau <var>z</var> entre deux bornes
         * <var>z<sub>i</sub></var> et <var>z<sub>i+1</sub></var>. Le tableau produit aura deux
         * colonnes et plusieurs centaines de lignes. La premi�re colonne contient la valeur
         * <var>z<sub>i+�</sub></var> entre deux bornes, tandis que la seconde colonne contient
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
         * Ecrit un tableau de la diff�rence moyenne de la hauteur de l'eau (<var>dz</var>)
         * entre deux points en fonction de la latitude, de la distance (<var>dx</var>) et
         * de l'�cart de temps (<var>dt</var>) entre ces deux points.  Ce calcul n'est pas
         * disponible si <code>setComputeVariances(false)</code> a �t� appel�e.
         */
        if (semiVariance != null) {
            final Writer out = new BufferedWriter(new FileWriter(semiVariance));
            out.write("Latitude (�)\t"            +
                      "�cart de temps (jours)\t"  +
                      "Distance (km)\t"           +
                      "Moyenne des �carts (cm)\t" +
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
     * Retourne une cha�ne de caract�res donnant
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
     * D�marre le calcul. Les arguments accept�s sont (dans l'ordre):
     *
     * <ul>
     *    <li>Date de d�but (valeur par d�faut: <code>01/01/1992 00:00</code>)</li>
     *    <li>Date de fin   (valeur par d�faut: <code>01/01/2002 00:00</code>)</li>
     *    <li>R�pertoire des fichiers CORSSH d'un premier satellite (Topex-Poseidon).</li>
     *    <li>R�pertoire des fichiers CORSSH d'un deuxi�me satellite (ERS-2).</li>
     *    <li>R�pertoire des fichiers CORSSH d'un troisi�me satellite (ERS-1).</li>
     * </ul>
     *
     * Note: Sous Windows NT, le calcul peut �tre lanc� en arri�re-plan avec
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
            // Enregistre quand-m�me
        }
        catch (Throwable error) {
            System.gc();
            progress.exceptionOccurred(error);
            return;
        }
        stats.write(new File("Histogram.txt"), new File("Statistics.txt"));
    }
}
