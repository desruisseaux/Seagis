/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.database.sample;

// J2SE
import java.util.Map;
import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.awt.geom.Point2D;
import java.sql.SQLException;
import java.awt.Color;

// JAI
import javax.media.jai.ParameterList;

// Geotools
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.TransformException;
import org.geotools.cv.Coverage; // Pour Javadoc
import org.geotools.cv.Category;
import org.geotools.cv.SampleDimension;
import org.geotools.cv.CannotEvaluateException;
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.gp.CannotReprojectException;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.resources.Utilities;
import org.geotools.util.NumberRange;

// Seagis
import fr.ird.database.DataBase;
import fr.ird.database.Coverage3D;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Valeurs d'un {@link ParameterEntry paramètre} à des positions d'échantillons.
 * Une couverture spatiale représentant une combinaison de paramètres. Cette couverture peut
 * servir par exemple à résumer dans une seule carte de potentiel les informations présentes
 * dans plusieurs cartes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ParameterCoverage3D extends Coverage3D implements fr.ird.database.sample.Coverage3D {
    /**
     * Le prefix à ajouter devant les noms des opérations pour ajouter l'opération
     * &quot;NodataFilter&quot;.
     *
     * @see fr.ird.database.coverage.sql.GridCoverageProcessor#NODATA_FILTER
     * @see #getProcessorOperation
     */
    private static final String NODATA_FILTER = "NodataFilter";

    /**
     * Le séparateur à utiliser entre les noms d'opérations.
     *
     * @see fr.ird.database.coverage.sql.GridCoverageProcessor#SEPARATOR
     * @see #getProcessorOperation
     */
    private static final char SEPARATOR = ';';

    /**
     * La palette de couleurs à utiliser par défaut pour le résultat.
     */
    private static final Color[] COLOR_PALETTE = {Color.BLUE, Color.WHITE, Color.RED};

    /**
     * La plage de valeurs à utiliser par défaut pour les valeur indexées.
     */
    private static final NumberRange INDEX_RANGE = new NumberRange(1, 255);

    /**
     * Un objet {@link Map} vide a affecter à {@link #coverages}
     */
    private static Map<SeriesKey,Coverage3D> EMPTY_MAP = (Map) Collections.EMPTY_MAP;

    /**
     * Paire comprenant une {@linkplain SeriesEntry série} avec {@linkplain OperationEntry
     * opération}. Ces paires sont utilisées comme clés dans {@link #coverages}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class SeriesKey {
        /** La série. */                public SeriesEntry    series;
        /** L'opération à appliquer. */ public OperationEntry operation;

        /** Construit une nouvelle clé. */
        public SeriesKey() {
        }

        /** Construit une nouvelle clé pour la série et l'opération spécifiée. */
        public SeriesKey(final SeriesEntry series, final OperationEntry operation) {
            this.series    = series;
            this.operation = operation;
        }

        /** Retourne un code à peu près unique pour cette clé. */
        public int hashCode() {
            int code = series.hashCode();
            if (operation != null) {
                code += 37*operation.hashCode();
            }
            return code;
        }

        /** Compare cette clé avec l'objet spécifié. */
        public boolean equals(final Object object) {
            if (object instanceof SeriesKey) {
                final SeriesKey that = (SeriesKey) object;
                return Utilities.equals(this.series,    that.series) &&
                       Utilities.equals(this.operation, that.operation);
            }
            return false;
        }

        /** Retourne le nom de la série et son opération. */
        public String toString() {
            if (operation == null) {
                return series.getName();
            }
            return operation.getName() + '[' + series.getName() + ']';
        }
    }

    /**
     * La base de données d'images, ou <code>null</code> si <code>ParameterCoverage3D</code>
     * n'a pas construit lui-même cette base. Cette référence est conservée uniquement afin
     * d'être fermée par {@link #dispose}.
     *
     * @see #dispose
     */
    private DataBase database;

    /**
     * La table des images, ou <code>null</code> si cet objet <code>ParameterCoverage3D</code>
     * a été {@linkplain #dispose disposé}. Si non-null, cette table ne sera disposée qu'à
     * la condition que <code>database</code> soit non-nul. Si ce n'est pas le cas, c'est
     * que la table aura été spécifiée explicitement par l'utilisateur et ne nous appartient
     * donc pas.
     *
     * @see #dispose
     */
    private CoverageTable coverageTable;

    /**
     * Le paramètre qui sera produit.
     *
     * @see #getParameter
     * @see #setParameter
     */
    private ParameterEntry target;

    /**
     * La liste des composantes du paramètre à produire.
     */
    private ParameterEntry.Component[] components;

    /**
     * Les objets {@link Coverage3D} disponibles pour chaque {@linkplain SeriesEntry séries}
     * et son {@linkplain OperationEntry opération}. Ce dictionnaire sera construit à chaque
     * appel de {@link #setParameter}.
     */
    private Map<SeriesKey,Coverage3D> coverages = EMPTY_MAP;

    /**
     * Nombre maximal de bandes dans les objets {@link Coverage3D}.
     * Ce nombre sera calculé à chaque appel de {@link #setParameter}.
     */
    private int maxNumBands;

    /**
     * Une description de l'unique bande produite en sortie par cet objet.
     */
    private SampleDimension sampleDimension;

    /**
     * Le processeur à utiliser pour appliquer des opérations sur des images.
     */
    private final GridCoverageProcessor processor =
            fr.ird.database.coverage.sql.CoverageDataBase.getDefaultGridCoverageProcessor();

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera les
     * connections par défaut. La méthode {@link #dispose} fermera ces connections.
     *
     * @throws SQLException si la connexion à la base de données a échouée.
     */
    public ParameterCoverage3D() throws SQLException {
        this(new fr.ird.database.coverage.sql.CoverageDataBase());
    }

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera la base de données
     * spécifiée. La méthode {@link #dispose} fermera les connections. Note: ce constructeur
     * sera insérée directement dans le code du constructeur précédent si Sun donnait suite au
     * RFE ##4093999 ("Relax constraint on placement of this()/super() call in constructors").
     *
     * @throws SQLException si la connexion à la base de données a échouée.
     */
    private ParameterCoverage3D(final CoverageDataBase database) throws SQLException {
        this(database.getCoverageTable(), null);
        coverageTable.setOperation("NodataFilter");
        this.database = database;
    }

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera la table d'image
     * spécifiée et son système de coordonnées. Ce constructeur délègue le travail à celui
     * qui prend en argument un système de coordonnées.
     *
     * @param  coverages La table des images à utiliser. Il sera de la responsabilité de
     *         l'utilisateur de fermer cette table lorsque cet objet ne sera plus utilisé.
     * @throws SQLException si la connexion à la base de données a échouée.
     */
    public ParameterCoverage3D(final CoverageTable coverages)throws SQLException {
        this(coverages, coverages.getCoordinateSystem());
    }

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera la table d'image
     * spécifiée. La {@linkplain CoverageTable#setGeographicArea zone géographique} et la
     * {@linkplain CoverageTable#setTimeRange plage de temps} définies sur cette table
     * affecteront toutes les images qui seront lus par cet objet <code>ParameterCoverage3D</code>.
     * En revanche, la {@linkplain CoverageTable#setSeries séries sélectionnée} ainsi que
     * l'éventuelle {@linkplain CoverageTable#setOperation opération} seront ignorées et
     * peut être écrasées.
     *
     * @param  coverages La table des images à utiliser. Il sera de la responsabilité de
     *         l'utilisateur de fermer cette table lorsque cet objet ne sera plus utilisé
     *         (ça ne sera pas fait automatiquement par {@link #dispose}, puisque la table
     *         spécifiée n'appartient pas à cet objet <code>ParameterCoverage3D</code>).
     * @param  cs Le système de coordonnées à utiliser pour cet obet {@link Coverage}.
     *         Ce système de coordonnées doit obligatoirement comprendre un axe temporel.
     * @throws SQLException si la connexion à la base de données a échouée.
     */
    public ParameterCoverage3D(final CoverageTable coverages,
                               final CoordinateSystem cs)
            throws SQLException
    {
        super("ParameterCoverage3D", cs);
        coverageTable = coverages;
    }

    /**
     * Obtient les données sous forme d'objet {@link Coverage3D} pour le paramètre spécifié.
     * Cette méthode est appelée automatiquement par {@link #setParameter setParameter} pour
     * obtenir les données qui composent un paramètre. L'implémentation par défaut construit
     * un objet {@link SeriesCoverage3D}. Les classes dérivées peuvent redéfinir cette méthode
     * pour construire un autre type de couverture, incluant un autre {@link ParameterCoverage3D}.
     *
     * @param parameter Le paramètre environnemental pour lequel on veut les données.
     * @param table Une table d'images pré-configurée. Cette table est déjà configurée avec la
     *              série d'image à lire, l'opération à appliquer ainsi que les coordonnées
     *              spatio-temporelles de la région d'intérêt.
     */
    protected Coverage3D createCoverage3D(final ParameterEntry parameter,
                                          final CoverageTable  table)
            throws SQLException
    {
        try {
            return new SeriesCoverage3D(table, getCoordinateSystem());
        } catch (TransformException e) {
            // Ne devrait pas se produire, puisque le système de coordonnées
            // est en principe le même que celui de la table.
            throw new CannotReprojectException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Retourne le nom de l'opération à utiliser. L'appel de cette méthode équivaut à l'appel
     * de {@link OperationEntry#getProcessorOperation}, mais peut ajouter en plus l'opération
     * "NodataFilter".
     */
    private static String getProcessorOperation(final OperationEntry operation) {
        if (operation == null) {
            return NODATA_FILTER;
        }
        String name = operation.getProcessorOperation();
        if (name == null) {
            return NODATA_FILTER;
        }
        name = name.trim();
        if (!name.equalsIgnoreCase(NODATA_FILTER)) {
            name = NODATA_FILTER + SEPARATOR + name;
        }
        return name;
    }

    /**
     * Spécifie le paramètre à produire. Des couvertures spatiales seront produites à partir
     * des {@linplain ParameterEntry#getComponents composantes} de ce paramètre, s'il y en a.
     *
     * @param parameter Le paramètre à produire, ou <code>null</code> si aucun.
     * @throws SQLException si la connexion à la base de données a échouée.
     */
    public synchronized void setParameter(final ParameterEntry parameter) throws SQLException {
        if (Utilities.equals(parameter, target)) {
            return;
        }
        final Map<SeriesKey,Coverage3D> oldCoverages = coverages;
        coverages       = EMPTY_MAP;
        target          = null;
        components      = null;
        sampleDimension = null;
        maxNumBands     = 0;
        if (parameter == null) {
            return;
        }
        /*
         * Obtient la liste des composantes du paramètre. Si ce paramètre n'a pas de composante,
         * alors il sera considérée comme sa propre source le temps de construire l'ensemble des
         * objets Coverage3D.  Pour chaque série impliquée, on construira un objet Coverage3D en
         * récupérant ceux qui existent déjà si possible.
         */
        target = parameter;
        coverages = new HashMap<SeriesKey,Coverage3D>();
        final Collection<+ParameterEntry.Component> list = parameter.getComponents();
        final ParameterEntry[] sources;
        final OperationEntry[] operations;
        if (list == null) {
            sources    = new ParameterEntry[] {target};
            operations = new OperationEntry[] {null};
        } else {
            int i=0;
            components = new ParameterEntry.Component[list.size()];
            sources    = new ParameterEntry[components.length];
            operations = new OperationEntry[components.length];
            for (final ParameterEntry.Component component : list) {
                components[i] = component;
                sources   [i] = component.getSource();
                operations[i] = component.getOperation();
                i++;
            }
            assert i == sources.length;
        }
        /*
         * A ce stade, on dispose de la liste des sources. Obtient maintenant les 'Coverage3D'
         * associés. La table 'CoverageTable' ne sera interrogée que si les données n'étaient
         * pas déjà disponibles. La série de la table sera changée, mais toute opération et
         * zone spatio-temporelle préalablement sélectionnée dans la table seront conservées.
         * Notez que 'oldCoverages' ne doit pas être modifié, car il est peut-être en cours
         * d'utilisation par 'evaluate' dans un autre thread.
         */
        for (int i=0; i<sources.length; i++) {
            final ParameterEntry source    = sources   [i];
            final OperationEntry operation = operations[i];
            final int band = source.getBand(); // Les numéros commencent à 1.
            if (band > maxNumBands) {
                maxNumBands = band;
            }
            if (source.isIdentity()) {
                continue;
            }
            SeriesEntry series;
            for (int seriesIndex=0; (series=source.getSeries(seriesIndex))!=null; seriesIndex++) {
                final SeriesKey key = new SeriesKey(series, operation);
                Coverage3D coverage = coverages.get(key);
                if (coverage == null) {
                    coverage = oldCoverages.get(key);
                    if (coverage == null) {
                        synchronized (coverageTable) {
                            final ParameterList param;
                            coverageTable.setSeries(series);
                            param = coverageTable.setOperation(getProcessorOperation(operation));
                            if (operation != null) {
                                final String[] names;
                                names = param.getParameterListDescriptor().getParamNames();
                                for (int j=0; j<names.length; j++) {
                                    final String name  = names[j];
                                    final Object value = operation.getParameter(name);
                                    if (value != null) {
                                        param.setParameter(name, value);
                                    }
                                }
                            }
                            coverage = createCoverage3D(source, coverageTable);
                        }
                    }
                    coverages.put(key, coverage);
                }
            }
        }
        /*
         * Libère les ressources des 'Coverage3D' qui ne sont plus utilisés. Ce bloc est pour
         * l'instant désactivé car ces 'Coverage3D' peuvent être encore utilisés par la méthode
         * 'evaluate', qui n'est que partiellement synchronisée sur 'this' afin de permettre des
         * accès simultanés aux données.
         */
        if (false) {
            for (final Map.Entry<SeriesKey,Coverage3D> entry : oldCoverages.entrySet()) {
                if (!coverages.containsKey(entry.getKey())) {
                    entry.getValue().dispose();
                }
            }
        }
    }

    /**
     * Retourne le descripteur du paramètre à produire. Si aucun paramètre n'a été spécifié,
     * alors cette méthode retourne <code>null</code>.
     */
    public ParameterEntry getParameter() {
        return target;
    }

    /**
     * Retourne une envelope englobant les coordonnées spatio-temporelles des données.
     * Cette envelope sera l'intersection des envelopes de toutes les composantes du
     * paramètre à calculer.
     */
    public synchronized Envelope getEnvelope() {
        Envelope envelope = null;
        for (final Coverage3D coverage : coverages.values()) {
            final Envelope next = coverage.getEnvelope();
            if (envelope == null) {
                envelope = next;
            } else {
                envelope.intersect(next);
            }
        }
        if (envelope == null) {
            envelope = super.getEnvelope();
        }
        return envelope;
    }

    /**
     * Retourne une description de l'unique bande produite en sortie par cet objet.
     * L'implémentation par défaut recherche les valeurs minimales et maximales que
     * peuvent produire les composantes du paramètre, et calcule la combinaison de
     * ces extremums afin de déterminer la plage de valeurs de la bande en sortie.
     */
    public synchronized SampleDimension[] getSampleDimensions() {
        if (sampleDimension == null) {
            final SeriesKey key = new SeriesKey();
            if (components == null) {
                key.series = target.getSeries(0);
                sampleDimension = coverages.get(key).getSampleDimensions()[target.getBand()-1];
            } else {
                double minimum = 0;
                double maximum = 0;
                for (int i=0; i<components.length; i++) {
                    final ParameterEntry.Component component = components[i];
                    final ParameterEntry source = component.getSource();
                    final double         weight = component.getWeight();
                    if (source.isIdentity()) {
                        minimum += weight;
                        maximum += weight;
                        continue;
                    }
                    final SampleDimension sd;
                    key.series = source.getSeries(0);
                    key.operation = component.getOperation();
                    sd = coverages.get(key).getSampleDimensions()[source.getBand()-1];
                    double min = weight * component.transform(sd.getMinimumValue());
                    double max = weight * component.transform(sd.getMaximumValue());
                    if (min > max) {
                        final double tmp = min;
                        min = max;
                        max = tmp;
                    }
                    minimum += min;
                    maximum += max;
                }
                if (minimum < maximum) {
                    sampleDimension = new SampleDimension(new Category[] {
                        new Category(Resources.format(ResourceKeys.POTENTIAL),
                                     COLOR_PALETTE, INDEX_RANGE, new NumberRange(minimum, maximum)),
                        Category.NODATA
                    }, null);
                } else {
                    sampleDimension = new SampleDimension();
                }
                sampleDimension = sampleDimension.geophysics(true);
            }
        }
        return new SampleDimension[] {sampleDimension};
    }

    /**
     * Retourne la valeur d'un échantillon ou d'un point arbitraire. Cette valeur sera calculée
     * en combinant toutes les {@linkplain ParameterEntry#getComponents composantes} du paramètre
     * spécifié lors du dernier appel à {@link #setParameter}. Si aucun paramètre n'a été spécifié,
     * alors cette méthode retourne {@link Double#NaN}.
     *
     * Cette méthode peut être appelée simultanément par plusieurs threads.
     *
     * @param  sample L'échantillon dont on veut la valeur du paramètre environnemental,
     *         ou <code>null</code>. Si non-null, alors les arguments <code>coordinate</code>
     *         et <code>time</code> <strong>doivent</strong> correspondre à la position de
     *         l'échantillon (ça ne sera pas vérifié, sauf si les assertions sont activées).
     * @param  coordinate La coordonnée spatiale du point à évaluer.
     * @param  time La date du point à évaluer.
     * @return La valeur du point aux coordonnées spatio-temporelles spécifiées.
     * @throws CannotEvaluateException si l'évaluation a échouée.
     */
    protected double evaluate(final SampleEntry sample,
                              final Point2D     coordinate,
                              final Date        time) throws CannotEvaluateException
    {
        if (sample != null) {
            assert sample.getCoordinate().equals(coordinate) : coordinate;
            assert sample.getTime()      .equals(time)       : time;
        }
        final SeriesKey key = new SeriesKey();
        final ParameterEntry.Component[] components;
        final Map<SeriesKey,Coverage3D>  coverages;
        double[] buffer;
        synchronized (this) {
            buffer     = new double[maxNumBands];
            components = this.components;
            coverages  = this.coverages;
            if (components == null) {
                if (target == null) {
                    return Double.NaN;
                }
                final int band = target.getBand()-1;
                int seriesIndex = 0;
                do {
                    key.series = target.getSeries(seriesIndex++);
                    if (key.series == null) {
                        break;
                    }
                    final Coverage3D coverage = coverages.get(key);
                    if (sample!=null && coverage instanceof fr.ird.database.sample.Coverage3D) {
                        buffer = ((fr.ird.database.sample.Coverage3D)coverage)
                                 .evaluate(sample, null, buffer);
                    } else {
                        buffer = coverage.evaluate(coordinate, time, buffer);
                    }
                } while (Double.isNaN(buffer[band]));
                return buffer[band];
            }
        }
        /*
         * Effectue la combinaison des paramètres. Le code qui suit n'a pas besoin d'être
         * synchronisé, puisque les seules références utilisées ('components','coverages'
         * et 'maxNumBands') ont été copiées dans le bloc synchronisé précédent. Par design,
         * les instances référés ne sont jamais modifiés après leur création par 'setParameters'.
         */
        double  value  = 0;                          // La valeur à retourner.
        boolean inside = false;                      // Vrai si au moins un paramètre a une valeur.
        PointOutsideCoverageException outside=null;  // La première exception obtenue.
        final Point2D coord1 = new Point2D.Double(); // La coordonnée spatiale décalée.
        final Date     time1 = new Date(0);          // La coordonnée temporelle décalée.
        for (int i=0; i<components.length; i++) {
            final ParameterEntry.Component component = components[i];
            final ParameterEntry source = component.getSource();
            if (source.isIdentity()) {
                value += component.getWeight();
                continue;
            }
            final RelativePositionEntry relativePosition;
            relativePosition = component.getRelativePosition();
            key.operation    = component.getOperation();
            coord1.setLocation(coordinate);
            time1.setTime(time.getTime());
            relativePosition.applyOffset(coord1, time1);
            final int band = source.getBand()-1;
            int seriesIndex = 0;
            do {
                /*
                 * Obtient la couverture spatio-temporelle SeriesCoverage3D correspondant à la
                 * série principale de la composante courante.  Si aucune valeur n'est trouvée
                 * pour cette série, alors seulement on examinera les séries "de secours".
                 */
                key.series = source.getSeries(seriesIndex++);
                if (key.series == null) {
                    break;
                }
                final Coverage3D coverage = coverages.get(key);
                try {
                    if (sample!=null && coverage instanceof fr.ird.database.sample.Coverage3D) {
                        buffer = ((fr.ird.database.sample.Coverage3D)coverage)
                                 .evaluate(sample, relativePosition, buffer);
                    } else {
                        buffer = coverage.evaluate(coord1, time1, buffer);
                    }
                    inside = true;
                } catch (PointOutsideCoverageException exception) {
                    if (outside == null) {
                        outside = exception;
                    }
                    Arrays.fill(buffer, Double.NaN);
                }
            } while (Double.isNaN(buffer[band]));
            value += component.getWeight() * component.transform(buffer[band]);
        }
        /*
         * Calcul terminer. Lance une exception si la coordonnée spécifiée tombait en dehours
         * de la couverture de *toutes* les séries. Autrement, les séries pour lesquelles le
         * point tombait en dehors auront simplement été considérés comme des données manquantes.
         */
        if (!inside && outside!=null) {
            throw outside;
        }
        return value;
    }

    /**
     * Évalue les valeurs de la couverture pour un échantillon.
     *
     * L'implémentation par défaut délègue le travail à
     * <code>{@link #evaluate(SampleEntry,Point2D,Date) evaluate}(sample, ...)</code>.
     * Cette méthode peut être appelée simultanément par plusieurs threads.
     *
     * @param  sample Échantillon pour lequel on veut les paramètres environnementaux.
     * @param  position Doit être <code>null</code> pour l'implémentation actuelle.
     * @param  dest Tableau de destination, ou <code>null</code>.
     * @return Les paramètres environnementaux pour l'échantillon spécifié.
     * @throws CannotEvaluateException si l'évaluation a échouée.
     */
    public double[] evaluate(final SampleEntry sample, final RelativePositionEntry position, double[] dest)
            throws CannotEvaluateException
    {
        if (position != null) {
            throw new UnsupportedOperationException("Not yet implemented");
        }
        if (dest == null) {
            dest = new double[1];
        }
        dest[0] = evaluate(sample, sample.getCoordinate(), sample.getTime());
        return dest;
    }


    /**
     * Retourne la valeur à la coordonnée spatio-temporelle spécifiée. Cette valeur sera calculée
     * en combinant toutes les {@linkplain ParameterEntry#getComponents composantes} du paramètre
     * spécifié lors du dernier appel à {@link #setParameter}. Si aucun paramètre n'a été spécifié,
     * alors cette méthode retourne {@link Double#NaN}.
     *
     * L'implémentation par défaut délègue le travail à
     * <code>{@link #evaluate(SampleEntry,Point2D,Date) evaluate}(null, coord, time)</code>.
     * Cette méthode peut être appelée simultanément par plusieurs threads.
     *
     * @param  coord La coordonnée spatiale du point à évaluer.
     * @param  time  La date du point à évaluer.
     * @return La valeur du point aux coordonnées spatio-temporelles spécifiées.
     * @throws CannotEvaluateException si l'évaluation a échouée.
     */
    public double evaluate(final Point2D coord, final Date time) throws CannotEvaluateException {
        return evaluate(null, coord, time);
    }

    /**
     * Retourne la valeur à la coordonnée spatio-temporelle spécifiée.
     * L'implémentation par défaut délègue le travail à
     * <code>{@link #evaluate(Point2D,Date) evaluate}(coord, time)</code>.
     */
    public int[] evaluate(final Point2D coord, final Date time, int[] dest)
            throws CannotEvaluateException
    {
        if (dest == null) {
            dest = new int[1];
        }
        dest[0] = (int)Math.round(evaluate(coord, time));
        return dest;
    }

    /**
     * Retourne la valeur à la coordonnée spatio-temporelle spécifiée.
     * L'implémentation par défaut délègue le travail à
     * <code>{@link #evaluate(Point2D,Date) evaluate}(coord, time)</code>.
     */
    public float[] evaluate(final Point2D coord, final Date time, float[] dest)
            throws CannotEvaluateException
    {
        if (dest == null) {
            dest = new float[1];
        }
        dest[0] = (float)evaluate(coord, time);
        return dest;
    }

    /**
     * Retourne la valeur à la coordonnée spatio-temporelle spécifiée.
     * L'implémentation par défaut délègue le travail à
     * <code>{@link #evaluate(Point2D,Date) evaluate}(coord, time)</code>.
     */
    public double[] evaluate(final Point2D coord, final Date time, double[] dest)
            throws CannotEvaluateException
    {
        if (dest == null) {
            dest = new double[1];
        }
        dest[0] = evaluate(coord, time);
        return dest;
    }

    /**
     * Libère toutes les ressources utilisées par cet objet. Cette méthode devrait être appelée
     * lorsque l'on sait que cet objet <code>ParameterCoverage3D</code> ne sera plus utilisé.
     * Notez que si une {@linkplain CoverageTable table des images} a été spécifiée explicitement
     * au constructeur, elle ne sera pas fermée puisqu'elle n'appartient pas à cet objet
     * <code>ParameterCoverage3D</code>; il sera de la responsabilité de l'utilisateur
     * de la fermer lui-même.
     */
    public synchronized void dispose() {
        try {
            setParameter(null);
            if (database != null) {
                coverageTable.close(); // A fermer seulement si 'database' est non-nul.
                database.close();
            }
        } catch (SQLException exception) {
            // Des connexions n'ont pas pu être fermées. Mais puisque de toute façon
            // on ne va plus utiliser cet objet, ce n'est pas grave.
            Utilities.unexpectedException("fr.ird.database.sample", "ParameterCoverage3D",
                                          "dispose", exception);
        }
        coverageTable = null;
        database      = null;
        super.dispose();
    }
}
