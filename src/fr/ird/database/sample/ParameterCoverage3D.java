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
import java.util.Set;
import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Collection;
import java.util.Collections;
import java.awt.geom.Point2D;
import java.text.ParseException;
import java.text.DateFormat;
import java.io.IOException;
import java.awt.Color;
import java.rmi.RemoteException;
import java.sql.SQLException;

// JAI
import javax.media.jai.JAI;
import javax.media.jai.ParameterList;

// OpenGIS
import org.opengis.referencing.operation.TransformException;
import org.opengis.coverage.CannotEvaluateException;

// Geotools
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cv.Coverage; // Pour Javadoc
import org.geotools.cv.Category;
import org.geotools.cv.SampleDimension;
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.gc.GridCoverage;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.gp.CannotReprojectException;
import org.geotools.gui.swing.FrameFactory;
import org.geotools.util.MonolineFormatter;
import org.geotools.resources.Utilities;
import org.geotools.resources.Arguments;
import org.geotools.util.NumberRange;

// Seagis
import fr.ird.database.CatalogException;
import fr.ird.database.DataBase;
import fr.ird.database.Coverage3D;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Valeurs d'un {@link ParameterEntry paramètre} à des positions d'échantillons. Ce paramètre
 * peut être le résultat d'un {@linkplain ParameterEntry#getLinearModel modèle linéaire}. Une
 * telle couverture peut servir par exemple à résumer dans une seule carte de potentiel les
 * informations présentes dans plusieurs cartes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ParameterCoverage3D extends Coverage3D implements fr.ird.database.sample.Coverage3D {
    /**
     * Le prefix à ajouter devant les noms des opérations pour ajouter l'opération
     * "NodataFilter".
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
    private static final Color[] COLOR_PALETTE =
            {Color.BLUE, Color.CYAN, Color.WHITE, Color.YELLOW, Color.RED};

    /**
     * La plage de valeurs à utiliser par défaut pour les valeur indexées.
     */
    private static final NumberRange INDEX_RANGE = new NumberRange(1, 255);

    /**
     * Un objet {@link Map} vide à affecter à {@link #coverages}
     */
    private static Map<SeriesKey,Coverage3D> EMPTY_MAP = (Map) Collections.EMPTY_MAP;

    /**
     * Paire comprenant une {@linkplain SeriesEntry série} avec une {@linkplain OperationEntry
     * opération}. Ces paires sont utilisées comme clés dans {@link #coverages}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class SeriesKey {
        /** La série. */                public SeriesEntry    series;
        /** L'opération à appliquer. */ public OperationEntry operation;
        /** Décalage temporel.       */ public float          timeOffset;

        /**
         * Construit une nouvelle clé initialisée avec tous les champs à <code>null</code> ou 0.
         * Il est de la responsabilité de l'appellant d'affecter une valeur non-nulle au moins
         * au champ {@link #series}. Les autres peuvent rester nuls.
         */
        public SeriesKey() {
        }

        /**
         * Construit une nouvelle clé pour la série et l'opération spécifiée. L'argument
         * <code>series</code> ne devrait pas être nul; les autres peuvent l'être.
         */
        public SeriesKey(final SeriesEntry           series,
                         final OperationEntry        operation,
                         final RelativePositionEntry position)
        {
            this.series     = series;
            this.operation  = operation;
            this.timeOffset = (position != null) ? position.getTypicalTimeOffset() : Float.NaN;
        }

        /**
         * Retourne un code à peu près unique pour cette clé. L'écart de temps {@link #timeOffset}
         * n'est pas pris en compte, ce qui permettre d'utiliser la valeur {@link Float#NaN} comme
         * un joker.
         */
        public int hashCode() {
            int code = series.hashCode();
            if (operation != null) {
                code += 37*operation.hashCode();
            }
            return code;
        }

        /**
         * Compare cette clé avec l'objet spécifié. Si la valeur {@link #timeOffset} est
         * {@link Float#NaN} pour au moins une des deux clés, cet écart de temps ne sera
         * pas pris en compte dans la comparaison. Autrement dit, NaN nous sert de joker.
         */
        public boolean equals(final Object object) {
            if (object instanceof SeriesKey) {
                final SeriesKey that = (SeriesKey) object;
                if (this.timeOffset != that.timeOffset) {
                    if (!Float.isNaN(timeOffset) && !Float.isNaN(that.timeOffset)) {
                        return false;
                    }
                    // Si une des deux valeurs est NaN, ne la prend pas en compte
                    // dans la comparaison. Autrement dit, NaN nous sert de joker.
                }
                return Utilities.equals(this.series,    that.series) &&
                       Utilities.equals(this.operation, that.operation);
            }
            return false;
        }

        /**
         * Retourne le nom de la série et son opération, ainsi que l'écart de temps en jours.
         */
        public String toString() {
            try {
                final StringBuffer buffer = new StringBuffer();
                if (operation != null) {
                    buffer.append(operation.getName());
                    buffer.append('[');
                }
                buffer.append(series.getName());
                if (timeOffset != 0) {
                    buffer.append(' ');
                    if (timeOffset >= 0) {
                        buffer.append('+');
                    } else if (Float.isNaN(timeOffset)) {
                        buffer.append("-?");
                    }
                    buffer.append(timeOffset);
                    buffer.append(" jours");
                }
                if (operation != null) {
                    buffer.append(']');
                }
                return buffer.toString();
            } catch (RemoteException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /**
     * La base de données d'images, ou <code>null</code> si <code>ParameterCoverage3D</code>
     * n'a pas construit lui-même cette base. Cette référence est conservée uniquement afin
     * d'être fermée par {@link #dispose}.
     *
     * @see #dispose
     */
    private CoverageDataBase database;

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
     * Le paramètre qui sera produit. Ce paramètre sera le même que celui que retourne
     * {@link LinearModelTerm#getTarget} pour chacun des termes de {@link #linearModel}.
     *
     * @see #getParameter
     * @see #setParameter
     */
    private ParameterEntry target;

    /**
     * La liste des termes du modèle linéaire, ou <code>null</code> s'il n'y en a pas.
     */
    private LinearModelTerm[] linearModel;

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
     * L'envelope de cette couverture. Ne sera calculée que la première fois où elle
     * sera demandée.
     *
     * @see #getEnvelope
     */
    private Envelope envelope;

    /**
     * Le processeur à utiliser pour appliquer des opérations sur des images.
     */
    private final GridCoverageProcessor processor =
            fr.ird.database.coverage.sql.CoverageDataBase.getDefaultGridCoverageProcessor();

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera les
     * connections par défaut. La méthode {@link #dispose} fermera ces connections.
     *
     * @throws RemoteException si la connexion au catalogue a échoué.
     */
    public ParameterCoverage3D() throws RemoteException {
        this(getCoverageDataBase());
    }

    /**
     * Retourne un CoverageDatabase.
     */
    private static final fr.ird.database.coverage.sql.CoverageDataBase getCoverageDataBase() 
        throws RemoteException 
    {
        return new fr.ird.database.coverage.sql.CoverageDataBase();
    }
    
    /**
     * Retourne un SampleDatabase.
     */
    private static final fr.ird.database.sample.sql.SampleDataBase getSampleDataBase() 
        throws RemoteException 
    {
        return new fr.ird.database.sample.sql.SampleDataBase();
    }
    
    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera la base de données
     * spécifiée. La méthode {@link #dispose} fermera les connections. Note: ce constructeur
     * sera insérée directement dans le code du constructeur précédent si Sun donnait suite au
     * RFE ##4093999 ("Relax constraint on placement of this()/super() call in constructors").
     *
     * @throws RemoteException si la connexion au catalogue a échoué.
     */
    private ParameterCoverage3D(final CoverageDataBase database) throws RemoteException {
        this(database.getCoverageTable());
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
     * @throws RemoteException si la connexion au catalogue a échoué.
     */
    public ParameterCoverage3D(final CoverageTable coverages) throws RemoteException {
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
     * @throws RemoteException si la connexion au catalogue a échoué.
     */
    public ParameterCoverage3D(final CoverageTable coverages,
                               final CoordinateSystem cs)
            throws RemoteException
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
            throws RemoteException
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
     * Retourne une clone de la couverture spécifiée, si possible. L'utilisation de clones
     * pour la même série d'images à des positions relatives différentes permet d'éviter que
     * la même image soit rechargée plusieurs fois, puisque chaque {@link SeriesCoverage3D}
     * gardera une référence forte vers la dernière image lue.
     */
    private static Coverage3D clone(Coverage3D coverage) {
        if (coverage instanceof fr.ird.database.coverage.SeriesCoverage3D) {
            coverage = new SeriesCoverage3D((fr.ird.database.coverage.SeriesCoverage3D) coverage);
        }
        return coverage;
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
     * Retourne l'ensemble des paramètres disponibles. Cette méthode n'est disponible que si
     * cet objet <code>ParameterCoverage3D</code> a été contruit avec le constructeur sans
     * argument. Elle utilisera un objet {@link SampleDataBase} temporaire.
     */
    final Set<? extends ParameterEntry> getParameters() throws RemoteException {
        final SampleDataBase database = getSampleDataBase();
        final SeriesTable    series   = this.database.getSeriesTable();
        try {
            return database.getParameters(series);
        } finally {
            series.close();
            database.close();
        }
    }

    /**
     * Spécifie le paramètre à produire. Des couvertures spatiales seront produites à partir
     * des termes du {@linplain ParameterEntry#getLinearModel modèle linéaire} de ce paramètre,
     * s'il y en a un. Cette méthode n'est disponible que si cet objet
     * <code>ParameterCoverage3D</code> a été contruit avec le constructeur sans argument.
     *
     * @param parameter Le paramètre à produire, ou <code>null</code> si aucun.
     * @throws RemoteException si la connexion au catalogue a échoué.
     */
    final void setParameter(final String parameter) throws RemoteException {
        if (parameter == null) {
            setParameter((ParameterEntry) null);
            return;
        }
        for (final ParameterEntry param : getParameters()) {
            if (parameter.equalsIgnoreCase(param.getName())) {
                setParameter(param);
                return;
            }
        }
        throw new IllegalArgumentException(parameter);
    }

    /**
     * Spécifie le paramètre à produire. Des couvertures spatiales seront produites à partir
     * du {@linplain ParameterEntry#getLinearModel modèle linéaire} de ce paramètre, s'il y
     * en a un.
     *
     * @param parameter Le paramètre à produire, ou <code>null</code> si aucun.
     * @throws RemoteException si la connexion au catalogue a échoué.
     */
    public synchronized void setParameter(final ParameterEntry parameter) throws RemoteException {
        if (Utilities.equals(parameter, target)) {
            return;
        }
        final Map<SeriesKey,Coverage3D> oldCoverages = coverages;
        coverages       = EMPTY_MAP;
        target          = null;
        linearModel     = null;
        envelope        = null;
        sampleDimension = null;
        maxNumBands     = 0;
        if (parameter == null) {
            return;
        }
        /*
         * Obtient tous les descripteurs du modèle linéaire.  Si ce paramètre n'a pas de modèle,
         * alors il sera considérée comme sa propre source le temps de construire l'ensemble des
         * objets Coverage3D.  Pour chaque série impliquée, on construira un objet Coverage3D en
         * récupérant ceux qui existent déjà si possible.
         */
        target = parameter;
        coverages = new HashMap<SeriesKey,Coverage3D>();
        final Collection<? extends LinearModelTerm> list = parameter.getLinearModel();
        final DescriptorEntry[] descriptors;
        final ParameterEntry [] sources;
        if (list == null) {
            descriptors = null;
            sources = new ParameterEntry[] {target};
        } else {
            int i=0, n=0;
            linearModel = new LinearModelTerm[list.size()];
            for (final LinearModelTerm term : list) {
                linearModel[i++] = term;
                n += term.getDescriptors().size();
            }
            assert i == linearModel.length;
            descriptors = new DescriptorEntry[n];
            sources     = new ParameterEntry [n];
            while (--i >= 0) {
                for (final DescriptorEntry descriptor : linearModel[i].getDescriptors()) {
                    sources  [--n] = descriptor.getParameter();
                    descriptors[n] = descriptor;
                }
            }
            assert n == 0 : n;
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
            final ParameterEntry source = sources[i];
            final int band = source.getBand(); // Les numéros commencent à 1.
            if (band > maxNumBands) {
                maxNumBands = band;
            }
            if (source.isIdentity()) {
                continue;
            }
            final OperationEntry       operation;
            final RelativePositionEntry position;
            if (descriptors != null) {
                final DescriptorEntry descriptor;
                descriptor = descriptors[i];
                operation  = descriptor.getOperation();
                position   = descriptor.getRelativePosition();
            } else {
                operation = null;
                position  = null;
            }
            SeriesEntry series;
            for (int seriesIndex=0; (series=source.getSeries(seriesIndex))!=null; seriesIndex++) {
                final SeriesKey key = new SeriesKey(series, operation, position);
                Coverage3D coverage = coverages.get(key);
                if (coverage != null) {
                    // Continue sans mémoriser le Coverage3D dans le dictonnaire 'coverages',
                    // puisqu'il y est déjà.  C'est la seule exception;  tous les autres cas
                    // ci-dessous placeront leurs résultats dans 'coverages'.
                    continue;
                }
                coverage = oldCoverages.get(key);
                if (coverage == null) {
                    /*
                     * La série demandée n'existe pas déjà pour le décalage temporel spécifié.
                     * Vérifie si elle existerait pour n'importe quel autre décalage temporel.
                     * Cloner une série existante est beaucoup plus rapide que d'en construire
                     * une identique à partir de la base de données.
                     */
                    final SeriesKey joker = new SeriesKey(series, operation, null);
                    coverage = coverages.get(joker);
                    if (coverage != null) {
                        coverage = clone(coverage);
                    } else {
                        coverage = oldCoverages.get(joker);
                        if (coverage != null) {
                            coverage = clone(coverage);
                        } else {
                            /*
                             * La série n'existait pas déjà, quel que soit le décalage temporel.
                             * Procède maintenant à la construction d'un nouvel objet Coverage3D
                             * pour cette série.
                             */
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
                    }
                }
                coverages.put(key, coverage);
            }
        }
        /*
         * Note: on aurait pu libèrer les ressources des 'Coverage3D' qui ne sont plus utilisés
         * (c'est-à-dire ceux qui sont encore dans 'oldCoverages' mais plus dans 'coverages').
         * Mais on ne le fait pas car ces 'Coverage3D' peuvent être encore utilisés par la méthode
         * 'evaluate', qui n'est que partiellement synchronisée sur 'this' afin de permettre des
         * accès multi-thread aux données.
         */
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
     * Cette envelope sera l'intersection des envelopes de toutes les descripteurs du
     * paramètre à calculer.
     */
    public synchronized Envelope getEnvelope() {
        if (envelope == null) {
            if (linearModel != null) {
                final SeriesKey key = new SeriesKey();
                for (final LinearModelTerm term : linearModel) {
                    for (final DescriptorEntry descriptor : term.getDescriptors()) {
                        final ParameterEntry source = descriptor.getParameter();
                        if (source.isIdentity()) {
                            continue;
                        }
                        final RelativePositionEntry position = descriptor.getRelativePosition();
                        Envelope toIntersect = null;
                        key.operation  = descriptor.getOperation();
                        key.timeOffset = position.getTypicalTimeOffset();
                        for (int i=0; ((key.series=source.getSeries(i))!=null); i++) {
                            final Envelope toAdd = coverages.get(key).getEnvelope();
                            final Date startTime = temporalCS.toDate(toAdd.getMinimum(temporalDimension));
                            final Date   endTime = temporalCS.toDate(toAdd.getMaximum(temporalDimension));
                            position.applyOppositeOffset(null, startTime);
                            position.applyOppositeOffset(null,   endTime);
                            toAdd.setRange(temporalDimension,
                                           temporalCS.toValue(startTime),
                                           temporalCS.toValue(  endTime));
                            if (toIntersect == null) {
                                toIntersect = toAdd;
                            } else {
                                toIntersect.add(toAdd);
                            }
                        }
                        if (envelope == null) {
                            envelope = toIntersect;
                        } else {
                            envelope.intersect(toIntersect);
                        }
                    }
                }
            } else {
                for (final Coverage3D coverage : coverages.values()) {
                    final Envelope toIntersect = coverage.getEnvelope();
                    if (envelope == null) {
                        envelope = toIntersect;
                    } else {
                        envelope.intersect(toIntersect);
                    }
                }
            }
            if (envelope == null) {
                envelope = super.getEnvelope();
            }
        }
        return (Envelope) envelope.clone();
    }

    /**
     * Définit la plage des valeurs qui seront produit en sortie. Cette plage de valeur
     * sera donnée à l'objet {@link Category} qui représentera le résultat du calcul.
     * Par défaut, cette plage est calculée automatiquement.
     *
     * @param range La plage de valeur de la sortie, ou <code>null</code> pour la calculer
     *              automatiquement.
     */
    public synchronized void setOutputRange(final NumberRange range) {
        if (range == null) {
            sampleDimension = null;
        } else {
            sampleDimension = new SampleDimension(new Category[] {
                Category.NODATA,
                new Category(Resources.format(ResourceKeys.POTENTIAL),
                             COLOR_PALETTE, INDEX_RANGE, range)}, null).geophysics(true);
        }
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
            if (linearModel == null) {
                key.series = target.getSeries(0);
                sampleDimension = coverages.get(key).getSampleDimensions()[target.getBand()-1];
            } else {
                double minimum = 0;
                double maximum = 0;
                for (final LinearModelTerm term : linearModel) {
                    double min = term.getCoefficient();
                    double max = min;
                    for (final DescriptorEntry descriptor : term.getDescriptors()) {
                        final ParameterEntry source = descriptor.getParameter();
                        if (source.isIdentity()) {
                            continue;
                        }
                        final SampleDimension sd;
                        key.series     = source.getSeries(0);
                        key.operation  = descriptor.getOperation();
                        key.timeOffset = descriptor.getRelativePosition().getTypicalTimeOffset();
                        sd = coverages.get(key).getSampleDimensions()[source.getBand()-1];
                        double smin = descriptor.normalize(sd.getMinimumValue());
                        double smax = descriptor.normalize(sd.getMaximumValue());
                        if ((Math.abs(smin) > Math.abs(smax)) != (Math.abs(min) > Math.abs(max))) {
                            final double tmp = smin;
                            smin = smax;
                            smax = tmp;
                        }
                        min *= smin;
                        max *= smax;
                    }
                    if (min > max) {
                        final double tmp = min;
                        min = max;
                        max = tmp;
                    }
                    minimum += min;
                    maximum += max;
                }
                if (minimum < maximum) {
                    setOutputRange(new NumberRange(minimum, maximum));
                } else {
                    sampleDimension = new SampleDimension();
                }
            }
        }
        return new SampleDimension[] {sampleDimension};
    }

    /**
     * Retourne la valeur d'un échantillon ou d'un point arbitraire. Cette valeur sera calculée
     * en combinant tous les termes du {@linkplain ParameterEntry#getLinearModel modèle linéaire}
     * du paramètre spécifié lors du dernier appel à {@link #setParameter}. Si aucun paramètre n'a
     * été spécifié, alors cette méthode retourne {@link Double#NaN}.
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
        final LinearModelTerm[] linearModel;
        final Map<SeriesKey,Coverage3D> coverages;
        double[] buffer;
        synchronized (this) {
            buffer      = new double[maxNumBands];
            linearModel = this.linearModel;
            coverages   = this.coverages;
            if (linearModel == null) {
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
         * synchronisé, puisque les seules références utilisées ('linearModel','coverages'
         * et 'maxNumBands') ont été copiées dans le bloc synchronisé précédent. Par design,
         * les instances référés ne sont jamais modifiés après leur création par 'setParameters'.
         */
        double  value  = 0;                          // La valeur à retourner.
        boolean inside = false;                      // Vrai si au moins un paramètre a une valeur.
        PointOutsideCoverageException outside=null;  // La première exception obtenue.
        final Point2D coord1 = new Point2D.Double(); // La coordonnée spatiale décalée.
        final Date     time1 = new Date(0);          // La coordonnée temporelle décalée.
        for (final LinearModelTerm term : linearModel) {
            double termValue = term.getCoefficient();
            for (final DescriptorEntry descriptor : term.getDescriptors()) {
                final ParameterEntry source = descriptor.getParameter();
                if (source.isIdentity()) {
                    continue;
                }
                final RelativePositionEntry relativePosition;
                relativePosition = descriptor.getRelativePosition();
                key.operation    = descriptor.getOperation();
                key.timeOffset   = relativePosition.getTypicalTimeOffset();
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
                termValue *= descriptor.normalize(buffer[band]);
            }
            value += termValue;
        }
        /*
         * Calcul terminer. Lance une exception si la coordonnée spécifiée tombait en dehors
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
     * en combinant tous les termes du {@linkplain ParameterEntry#getLinearModel modèle linéaire}
     * du paramètre spécifié lors du dernier appel à {@link #setParameter}. Si aucun paramètre n'a
     * été spécifié, alors cette méthode retourne {@link Double#NaN}.
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
            setParameter((ParameterEntry)null);
            if (database != null) {
                coverageTable.close(); // A fermer seulement si 'database' est non-nul.
                database.close();
            }
        } catch (RemoteException exception) {
            // Des connexions n'ont pas pu être fermées. Mais puisque de toute façon
            // on ne va plus utiliser cet objet, ce n'est pas grave.
            Utilities.unexpectedException("fr.ird.database.sample", "ParameterCoverage3D",
                                          "dispose", exception);
        }
        coverageTable = null;
        database      = null;
        super.dispose();
    }

    /**
     * Lance la création d'images de potentiel de pêche à partir de la ligne de commande.
     * Les arguments sont:
     *
     * <ul>
     *   <li><code>-parameter=<var>P</var></code> où <var>P</var> est un des paramètre énuméré
     *       dans la table "Paramètre" de la base de données des échantillons (par exemple "PP1").</li>
     *   <li><code>-date=<var>date</var> est la date (en heure universelle) de l'image à générer
     *       (par exemple "18/08/1999"). Son format dépend des conventions du système,
     *       qui peut être spécifié par l'argument <code>-locale</code>.</li>
     *   <li><code>-file=<var>file</var> est un nom de fichier optionel dans lequel enregistrer
     *       le résultat. Si cet argument est omis, alors l'image sera affichée à l'écran sans
     *       être enregistrée.</li>
     *   <li><code>-locale=<var>locale</var> désigne les conventions locales à utiliser pour lire
     *       l'argument <code>-date</code>. Si cet argument est omis, les conventions courantes du
     *       système sont utilisées.</li>
     *   <li><code>-generator=<var>classname</var></code> est le nom d'une sous-classe de {@link
     *        ParameterCoverage3D} à utiliser pour générer les images de potentiel. Si cet
     *        argument est omis, alors <code>fr.ird.database.sample.ParameterCoverage3D</code>
     *        est utilisé directement.</li>
     * </ul>
     *
     * @param  args Les paramètres transmis sur la ligne de commande.
     *
     * @throws ParseException si le format de la date n'est pas légal.
     * @throws RemoteException si une connexion à la base de données a échouée.
     * @throws IOException si l'image ne peut pas être enregistrée.
     * @throws ClassNotFoundException si la classe spécifiée dans l'argument
     *         <code>-generator</code> n'a pas été trouvée.
     * @throws ClassCastException si la classe spécifiée dans l'argument
     *         <code>-generator</code> n'est pas une sous-classe de {@link ParameterCoverage3D}.
     * @throws InstantiationException si la classe spécifiée dans l'argument
     *         <code>-generator</code> est abstraite.
     * @throws IllegalAccessException si la classe spécifiée dans l'argument
     *         <code>-generator</code> n'a pas de constructeur public sans argument.
     */
    public static void main(final String[] args)
            throws ParseException, RemoteException, IOException,
                   ClassNotFoundException, ClassCastException,
                   InstantiationException, IllegalAccessException
    {
        MonolineFormatter.init("org.geotools");
        MonolineFormatter.init("fr.ird");
        final Arguments arguments = new Arguments(args);
        final String    parameter = arguments.getRequiredString("-parameter");
        final DateFormat   format = DateFormat.getDateInstance(DateFormat.SHORT, arguments.locale);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final Date           date = format.parse(arguments.getRequiredString("-date"));
        final String     filename = arguments.getOptionalString("-file");
        final String    generator = arguments.getOptionalString("-generator");
        arguments.getRemainingArguments(0);
        /*
         * Procède à la création de l'image, à son enregistrement puis à son affichage.
         */
        final double offset = -2;
        final double scale  = 1.0/64;
        final GridCoverage coverage;
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(256*1024*1024);
        final ParameterCoverage3D coverage3D;
        if (generator != null) {
            coverage3D = (ParameterCoverage3D) Class.forName(generator).newInstance();
        } else {
            coverage3D = new ParameterCoverage3D();
        }
        try {
            coverage3D.setParameter(parameter);
            coverage3D.setOutputRange(new NumberRange(1*scale+offset, 255*scale+offset));
            coverage = coverage3D.getGridCoverage2D(date);
            if (filename != null) {
                fr.ird.resources.Utilities.save(coverage.geophysics(false).getRenderedImage(), filename);
            } else {
                // Force le calul immédiat, avant que ne soit
                // fermée la connexion à la base de données.
                coverage.getRenderedImage().getData();
            }
            FrameFactory.show(coverage);
        } finally {
            // Ferme la base de données seulement après la création de l'image,
            // sans quoi le calcul de l'image échouera sans message d'erreur.
            coverage3D.dispose();
        }
    }
}
