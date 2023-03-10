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
import fr.ird.database.coverage.GridCoverage3D;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Valeurs d'un {@link ParameterEntry paramčtre} ŕ des positions d'échantillons. Ce paramčtre
 * peut ętre le résultat d'un {@linkplain ParameterEntry#getLinearModel modčle linéaire}. Une
 * telle couverture peut servir par exemple ŕ résumer dans une seule carte de potentiel les
 * informations présentes dans plusieurs cartes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ParameterCoverage3D extends Coverage3D implements fr.ird.database.sample.Coverage3D {
    /**
     * Le prefix ŕ ajouter devant les noms des opérations pour ajouter l'opération
     * "NodataFilter".
     *
     * @see fr.ird.database.coverage.sql.GridCoverageProcessor#NODATA_FILTER
     * @see #getProcessorOperation
     */
    private static final String NODATA_FILTER = "NodataFilter";

    /**
     * Le séparateur ŕ utiliser entre les noms d'opérations.
     *
     * @see fr.ird.database.coverage.sql.GridCoverageProcessor#SEPARATOR
     * @see #getProcessorOperation
     */
    private static final char SEPARATOR = ';';

    /**
     * La palette de couleurs ŕ utiliser par défaut pour le résultat.
     */
    private static final Color[] COLOR_PALETTE =
            {Color.BLUE, Color.CYAN, Color.WHITE, Color.YELLOW, Color.RED};

    /**
     * La plage de valeurs ŕ utiliser par défaut pour les valeur indexées.
     */
    private static final NumberRange INDEX_RANGE = new NumberRange(1, 255);

    /**
     * Un objet {@link Map} vide ŕ affecter ŕ {@link #coverages}
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
        /** L'opération ŕ appliquer. */ public OperationEntry operation;
        /** Décalage temporel.       */ public float          timeOffset;

        /**
         * Construit une nouvelle clé initialisée avec tous les champs ŕ <code>null</code> ou 0.
         * Il est de la responsabilité de l'appellant d'affecter une valeur non-nulle au moins
         * au champ {@link #series}. Les autres peuvent rester nuls.
         */
        public SeriesKey() {
        }

        /**
         * Construit une nouvelle clé pour la série et l'opération spécifiée. L'argument
         * <code>series</code> ne devrait pas ętre nul; les autres peuvent l'ętre.
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
         * Retourne un code ŕ peu prčs unique pour cette clé. L'écart de temps {@link #timeOffset}
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
     * n'a pas construit lui-męme cette base. Cette référence est conservée uniquement afin
     * d'ętre fermée par {@link #dispose}.
     *
     * @see #dispose
     */
    private CoverageDataBase database;

    /**
     * La table des images, ou <code>null</code> si cet objet <code>ParameterCoverage3D</code>
     * a été {@linkplain #dispose disposé}. Si non-null, cette table ne sera disposée qu'ŕ
     * la condition que <code>database</code> soit non-nul. Si ce n'est pas le cas, c'est
     * que la table aura été spécifiée explicitement par l'utilisateur et ne nous appartient
     * donc pas.
     *
     * @see #dispose
     */
    private CoverageTable coverageTable;

    /**
     * Le paramčtre qui sera produit. Ce paramčtre sera le męme que celui que retourne
     * {@link LinearModelTerm#getTarget} pour chacun des termes de {@link #linearModel}.
     *
     * @see #getParameter
     * @see #setParameter
     */
    private ParameterEntry target;

    /**
     * La liste des termes du modčle linéaire, ou <code>null</code> s'il n'y en a pas.
     */
    private LinearModelTerm[] linearModel;

    /**
     * Les objets {@link Coverage3D} disponibles pour chaque {@linkplain SeriesEntry séries}
     * et son {@linkplain OperationEntry opération}. Ce dictionnaire sera construit ŕ chaque
     * appel de {@link #setParameter}.
     */
    private Map<SeriesKey,Coverage3D> coverages = EMPTY_MAP;

    /**
     * Nombre maximal de bandes dans les objets {@link Coverage3D}.
     * Ce nombre sera calculé ŕ chaque appel de {@link #setParameter}.
     */
    private int maxNumBands;

    /**
     * Une description de l'unique bande produite en sortie par cet objet.
     */
    private SampleDimension sampleDimension;

    /**
     * L'envelope de cette couverture. Ne sera calculée que la premičre fois oů elle
     * sera demandée.
     *
     * @see #getEnvelope
     */
    private Envelope envelope;

    /**
     * Le processeur ŕ utiliser pour appliquer des opérations sur des images.
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
     * spécifiée et son systčme de coordonnées. Ce constructeur délčgue le travail ŕ celui
     * qui prend en argument un systčme de coordonnées.
     *
     * @param  coverages La table des images ŕ utiliser. Il sera de la responsabilité de
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
     * peut ętre écrasées.
     *
     * @param  coverages La table des images ŕ utiliser. Il sera de la responsabilité de
     *         l'utilisateur de fermer cette table lorsque cet objet ne sera plus utilisé
     *         (ça ne sera pas fait automatiquement par {@link #dispose}, puisque la table
     *         spécifiée n'appartient pas ŕ cet objet <code>ParameterCoverage3D</code>).
     * @param  cs Le systčme de coordonnées ŕ utiliser pour cet obet {@link Coverage}.
     *         Ce systčme de coordonnées doit obligatoirement comprendre un axe temporel.
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
     * Obtient les données sous forme d'objet {@link Coverage3D} pour le paramčtre spécifié.
     * Cette méthode est appelée automatiquement par {@link #setParameter setParameter} pour
     * obtenir les données qui composent un paramčtre. L'implémentation par défaut construit
     * un objet {@link Environment3D}. Les classes dérivées peuvent redéfinir cette méthode
     * pour construire un autre type de couverture, incluant un autre {@link ParameterCoverage3D}.
     *
     * @param parameter Le paramčtre environnemental pour lequel on veut les données.
     * @param table Une table d'images pré-configurée. Cette table est déjŕ configurée avec la
     *              série d'image ŕ lire, l'opération ŕ appliquer ainsi que les coordonnées
     *              spatio-temporelles de la région d'intéręt.
     */
    protected Coverage3D createCoverage3D(final ParameterEntry parameter,
                                          final CoverageTable  table)
            throws RemoteException
    {
        try {
            return new Environment3D(table, getCoordinateSystem());
        } catch (TransformException e) {
            // Ne devrait pas se produire, puisque le systčme de coordonnées
            // est en principe le męme que celui de la table.
            throw new CannotReprojectException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Retourne une clone de la couverture spécifiée, si possible. L'utilisation de clones
     * pour la męme série d'images ŕ des positions relatives différentes permet d'éviter que
     * la męme image soit rechargée plusieurs fois, puisque chaque {@link Environment3D}
     * gardera une référence forte vers la derničre image lue.
     */
    private static Coverage3D clone(Coverage3D coverage) {
        if (coverage instanceof GridCoverage3D) {
            coverage = new Environment3D((GridCoverage3D) coverage);
        }
        return coverage;
    }

    /**
     * Retourne le nom de l'opération ŕ utiliser. L'appel de cette méthode équivaut ŕ l'appel
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
     * Retourne l'ensemble des paramčtres disponibles. Cette méthode n'est disponible que si
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
     * Spécifie le paramčtre ŕ produire. Des couvertures spatiales seront produites ŕ partir
     * des termes du {@linplain ParameterEntry#getLinearModel modčle linéaire} de ce paramčtre,
     * s'il y en a un. Cette méthode n'est disponible que si cet objet
     * <code>ParameterCoverage3D</code> a été contruit avec le constructeur sans argument.
     *
     * @param parameter Le paramčtre ŕ produire, ou <code>null</code> si aucun.
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
     * Spécifie le paramčtre ŕ produire. Des couvertures spatiales seront produites ŕ partir
     * du {@linplain ParameterEntry#getLinearModel modčle linéaire} de ce paramčtre, s'il y
     * en a un.
     *
     * @param parameter Le paramčtre ŕ produire, ou <code>null</code> si aucun.
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
         * Obtient tous les descripteurs du modčle linéaire.  Si ce paramčtre n'a pas de modčle,
         * alors il sera considérée comme sa propre source le temps de construire l'ensemble des
         * objets Coverage3D.  Pour chaque série impliquée, on construira un objet Coverage3D en
         * récupérant ceux qui existent déjŕ si possible.
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
         * pas déjŕ disponibles. La série de la table sera changée, mais toute opération et
         * zone spatio-temporelle préalablement sélectionnée dans la table seront conservées.
         * Notez que 'oldCoverages' ne doit pas ętre modifié, car il est peut-ętre en cours
         * d'utilisation par 'evaluate' dans un autre thread.
         */
        for (int i=0; i<sources.length; i++) {
            final ParameterEntry source = sources[i];
            final int band = source.getBand(); // Les numéros commencent ŕ 1.
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
                    // puisqu'il y est déjŕ.  C'est la seule exception;  tous les autres cas
                    // ci-dessous placeront leurs résultats dans 'coverages'.
                    continue;
                }
                coverage = oldCoverages.get(key);
                if (coverage == null) {
                    /*
                     * La série demandée n'existe pas déjŕ pour le décalage temporel spécifié.
                     * Vérifie si elle existerait pour n'importe quel autre décalage temporel.
                     * Cloner une série existante est beaucoup plus rapide que d'en construire
                     * une identique ŕ partir de la base de données.
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
                             * La série n'existait pas déjŕ, quel que soit le décalage temporel.
                             * Procčde maintenant ŕ la construction d'un nouvel objet Coverage3D
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
         * Note: on aurait pu libčrer les ressources des 'Coverage3D' qui ne sont plus utilisés
         * (c'est-ŕ-dire ceux qui sont encore dans 'oldCoverages' mais plus dans 'coverages').
         * Mais on ne le fait pas car ces 'Coverage3D' peuvent ętre encore utilisés par la méthode
         * 'evaluate', qui n'est que partiellement synchronisée sur 'this' afin de permettre des
         * accčs multi-thread aux données.
         */
    }

    /**
     * Retourne le descripteur du paramčtre ŕ produire. Si aucun paramčtre n'a été spécifié,
     * alors cette méthode retourne <code>null</code>.
     */
    public ParameterEntry getParameter() {
        return target;
    }

    /**
     * Retourne une envelope englobant les coordonnées spatio-temporelles des données.
     * Cette envelope sera l'intersection des envelopes de toutes les descripteurs du
     * paramčtre ŕ calculer.
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
     * sera donnée ŕ l'objet {@link Category} qui représentera le résultat du calcul.
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
     * peuvent produire les composantes du paramčtre, et calcule la combinaison de
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
     * en combinant tous les termes du {@linkplain ParameterEntry#getLinearModel modčle linéaire}
     * du paramčtre spécifié lors du dernier appel ŕ {@link #setParameter}. Si aucun paramčtre n'a
     * été spécifié, alors cette méthode retourne {@link Double#NaN}.
     *
     * Cette méthode peut ętre appelée simultanément par plusieurs threads.
     *
     * @param  sample L'échantillon dont on veut la valeur du paramčtre environnemental,
     *         ou <code>null</code>. Si non-null, alors les arguments <code>coordinate</code>
     *         et <code>time</code> <strong>doivent</strong> correspondre ŕ la position de
     *         l'échantillon (ça ne sera pas vérifié, sauf si les assertions sont activées).
     * @param  coordinate La coordonnée spatiale du point ŕ évaluer.
     * @param  time La date du point ŕ évaluer.
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
         * Effectue la combinaison des paramčtres. Le code qui suit n'a pas besoin d'ętre
         * synchronisé, puisque les seules références utilisées ('linearModel','coverages'
         * et 'maxNumBands') ont été copiées dans le bloc synchronisé précédent. Par design,
         * les instances référés ne sont jamais modifiés aprčs leur création par 'setParameters'.
         */
        double  value  = 0;                          // La valeur ŕ retourner.
        boolean inside = false;                      // Vrai si au moins un paramčtre a une valeur.
        PointOutsideCoverageException outside=null;  // La premičre exception obtenue.
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
                     * Obtient la couverture spatio-temporelle GridCoverage3D correspondant ŕ la
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
     * L'implémentation par défaut délčgue le travail ŕ
     * <code>{@link #evaluate(SampleEntry,Point2D,Date) evaluate}(sample, ...)</code>.
     * Cette méthode peut ętre appelée simultanément par plusieurs threads.
     *
     * @param  sample Échantillon pour lequel on veut les paramčtres environnementaux.
     * @param  position Doit ętre <code>null</code> pour l'implémentation actuelle.
     * @param  dest Tableau de destination, ou <code>null</code>.
     * @return Les paramčtres environnementaux pour l'échantillon spécifié.
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
     * Retourne la valeur ŕ la coordonnée spatio-temporelle spécifiée. Cette valeur sera calculée
     * en combinant tous les termes du {@linkplain ParameterEntry#getLinearModel modčle linéaire}
     * du paramčtre spécifié lors du dernier appel ŕ {@link #setParameter}. Si aucun paramčtre n'a
     * été spécifié, alors cette méthode retourne {@link Double#NaN}.
     *
     * L'implémentation par défaut délčgue le travail ŕ
     * <code>{@link #evaluate(SampleEntry,Point2D,Date) evaluate}(null, coord, time)</code>.
     * Cette méthode peut ętre appelée simultanément par plusieurs threads.
     *
     * @param  coord La coordonnée spatiale du point ŕ évaluer.
     * @param  time  La date du point ŕ évaluer.
     * @return La valeur du point aux coordonnées spatio-temporelles spécifiées.
     * @throws CannotEvaluateException si l'évaluation a échouée.
     */
    public double evaluate(final Point2D coord, final Date time) throws CannotEvaluateException {
        return evaluate(null, coord, time);
    }

    /**
     * Retourne la valeur ŕ la coordonnée spatio-temporelle spécifiée.
     * L'implémentation par défaut délčgue le travail ŕ
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
     * Retourne la valeur ŕ la coordonnée spatio-temporelle spécifiée.
     * L'implémentation par défaut délčgue le travail ŕ
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
     * Retourne la valeur ŕ la coordonnée spatio-temporelle spécifiée.
     * L'implémentation par défaut délčgue le travail ŕ
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
     * Libčre toutes les ressources utilisées par cet objet. Cette méthode devrait ętre appelée
     * lorsque l'on sait que cet objet <code>ParameterCoverage3D</code> ne sera plus utilisé.
     * Notez que si une {@linkplain CoverageTable table des images} a été spécifiée explicitement
     * au constructeur, elle ne sera pas fermée puisqu'elle n'appartient pas ŕ cet objet
     * <code>ParameterCoverage3D</code>; il sera de la responsabilité de l'utilisateur
     * de la fermer lui-męme.
     */
    public synchronized void dispose() {
        try {
            setParameter((ParameterEntry)null);
            if (database != null) {
                coverageTable.close(); // A fermer seulement si 'database' est non-nul.
                database.close();
            }
        } catch (RemoteException exception) {
            // Des connexions n'ont pas pu ętre fermées. Mais puisque de toute façon
            // on ne va plus utiliser cet objet, ce n'est pas grave.
            Utilities.unexpectedException("fr.ird.database.sample", "ParameterCoverage3D",
                                          "dispose", exception);
        }
        coverageTable = null;
        database      = null;
        super.dispose();
    }

    /**
     * Lance la création d'images de potentiel de pęche ŕ partir de la ligne de commande.
     * Les arguments sont:
     *
     * <ul>
     *   <li><code>-parameter=<var>P</var></code> oů <var>P</var> est un des paramčtre énuméré
     *       dans la table "Paramčtre" de la base de données des échantillons (par exemple "PP1").</li>
     *   <li><code>-date=<var>date</var> est la date (en heure universelle) de l'image ŕ générer
     *       (par exemple "18/08/1999"). Son format dépend des conventions du systčme,
     *       qui peut ętre spécifié par l'argument <code>-locale</code>.</li>
     *   <li><code>-file=<var>file</var> est un nom de fichier optionel dans lequel enregistrer
     *       le résultat. Si cet argument est omis, alors l'image sera affichée ŕ l'écran sans
     *       ętre enregistrée.</li>
     *   <li><code>-locale=<var>locale</var> désigne les conventions locales ŕ utiliser pour lire
     *       l'argument <code>-date</code>. Si cet argument est omis, les conventions courantes du
     *       systčme sont utilisées.</li>
     *   <li><code>-generator=<var>classname</var></code> est le nom d'une sous-classe de {@link
     *        ParameterCoverage3D} ŕ utiliser pour générer les images de potentiel. Si cet
     *        argument est omis, alors <code>fr.ird.database.sample.ParameterCoverage3D</code>
     *        est utilisé directement.</li>
     * </ul>
     *
     * @param  args Les paramčtres transmis sur la ligne de commande.
     *
     * @throws ParseException si le format de la date n'est pas légal.
     * @throws RemoteException si une connexion ŕ la base de données a échouée.
     * @throws IOException si l'image ne peut pas ętre enregistrée.
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
         * Procčde ŕ la création de l'image, ŕ son enregistrement puis ŕ son affichage.
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
                // fermée la connexion ŕ la base de données.
                coverage.getRenderedImage().getData();
            }
            FrameFactory.show(coverage);
        } finally {
            // Ferme la base de données seulement aprčs la création de l'image,
            // sans quoi le calcul de l'image échouera sans message d'erreur.
            coverage3D.dispose();
        }
    }
}
