/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
 * Valeurs d'un {@link ParameterEntry param�tre} � des positions d'�chantillons. Ce param�tre
 * peut �tre le r�sultat d'un {@linkplain ParameterEntry#getLinearModel mod�le lin�aire}. Une
 * telle couverture peut servir par exemple � r�sumer dans une seule carte de potentiel les
 * informations pr�sentes dans plusieurs cartes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ParameterCoverage3D extends Coverage3D implements fr.ird.database.sample.Coverage3D {
    /**
     * Le prefix � ajouter devant les noms des op�rations pour ajouter l'op�ration
     * "NodataFilter".
     *
     * @see fr.ird.database.coverage.sql.GridCoverageProcessor#NODATA_FILTER
     * @see #getProcessorOperation
     */
    private static final String NODATA_FILTER = "NodataFilter";

    /**
     * Le s�parateur � utiliser entre les noms d'op�rations.
     *
     * @see fr.ird.database.coverage.sql.GridCoverageProcessor#SEPARATOR
     * @see #getProcessorOperation
     */
    private static final char SEPARATOR = ';';

    /**
     * La palette de couleurs � utiliser par d�faut pour le r�sultat.
     */
    private static final Color[] COLOR_PALETTE =
            {Color.BLUE, Color.CYAN, Color.WHITE, Color.YELLOW, Color.RED};

    /**
     * La plage de valeurs � utiliser par d�faut pour les valeur index�es.
     */
    private static final NumberRange INDEX_RANGE = new NumberRange(1, 255);

    /**
     * Un objet {@link Map} vide � affecter � {@link #coverages}
     */
    private static Map<SeriesKey,Coverage3D> EMPTY_MAP = (Map) Collections.EMPTY_MAP;

    /**
     * Paire comprenant une {@linkplain SeriesEntry s�rie} avec une {@linkplain OperationEntry
     * op�ration}. Ces paires sont utilis�es comme cl�s dans {@link #coverages}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class SeriesKey {
        /** La s�rie. */                public SeriesEntry    series;
        /** L'op�ration � appliquer. */ public OperationEntry operation;
        /** D�calage temporel.       */ public float          timeOffset;

        /**
         * Construit une nouvelle cl� initialis�e avec tous les champs � <code>null</code> ou 0.
         * Il est de la responsabilit� de l'appellant d'affecter une valeur non-nulle au moins
         * au champ {@link #series}. Les autres peuvent rester nuls.
         */
        public SeriesKey() {
        }

        /**
         * Construit une nouvelle cl� pour la s�rie et l'op�ration sp�cifi�e. L'argument
         * <code>series</code> ne devrait pas �tre nul; les autres peuvent l'�tre.
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
         * Retourne un code � peu pr�s unique pour cette cl�. L'�cart de temps {@link #timeOffset}
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
         * Compare cette cl� avec l'objet sp�cifi�. Si la valeur {@link #timeOffset} est
         * {@link Float#NaN} pour au moins une des deux cl�s, cet �cart de temps ne sera
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
         * Retourne le nom de la s�rie et son op�ration, ainsi que l'�cart de temps en jours.
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
     * La base de donn�es d'images, ou <code>null</code> si <code>ParameterCoverage3D</code>
     * n'a pas construit lui-m�me cette base. Cette r�f�rence est conserv�e uniquement afin
     * d'�tre ferm�e par {@link #dispose}.
     *
     * @see #dispose
     */
    private CoverageDataBase database;

    /**
     * La table des images, ou <code>null</code> si cet objet <code>ParameterCoverage3D</code>
     * a �t� {@linkplain #dispose dispos�}. Si non-null, cette table ne sera dispos�e qu'�
     * la condition que <code>database</code> soit non-nul. Si ce n'est pas le cas, c'est
     * que la table aura �t� sp�cifi�e explicitement par l'utilisateur et ne nous appartient
     * donc pas.
     *
     * @see #dispose
     */
    private CoverageTable coverageTable;

    /**
     * Le param�tre qui sera produit. Ce param�tre sera le m�me que celui que retourne
     * {@link LinearModelTerm#getTarget} pour chacun des termes de {@link #linearModel}.
     *
     * @see #getParameter
     * @see #setParameter
     */
    private ParameterEntry target;

    /**
     * La liste des termes du mod�le lin�aire, ou <code>null</code> s'il n'y en a pas.
     */
    private LinearModelTerm[] linearModel;

    /**
     * Les objets {@link Coverage3D} disponibles pour chaque {@linkplain SeriesEntry s�ries}
     * et son {@linkplain OperationEntry op�ration}. Ce dictionnaire sera construit � chaque
     * appel de {@link #setParameter}.
     */
    private Map<SeriesKey,Coverage3D> coverages = EMPTY_MAP;

    /**
     * Nombre maximal de bandes dans les objets {@link Coverage3D}.
     * Ce nombre sera calcul� � chaque appel de {@link #setParameter}.
     */
    private int maxNumBands;

    /**
     * Une description de l'unique bande produite en sortie par cet objet.
     */
    private SampleDimension sampleDimension;

    /**
     * L'envelope de cette couverture. Ne sera calcul�e que la premi�re fois o� elle
     * sera demand�e.
     *
     * @see #getEnvelope
     */
    private Envelope envelope;

    /**
     * Le processeur � utiliser pour appliquer des op�rations sur des images.
     */
    private final GridCoverageProcessor processor =
            fr.ird.database.coverage.sql.CoverageDataBase.getDefaultGridCoverageProcessor();

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera les
     * connections par d�faut. La m�thode {@link #dispose} fermera ces connections.
     *
     * @throws RemoteException si la connexion au catalogue a �chou�.
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
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera la base de donn�es
     * sp�cifi�e. La m�thode {@link #dispose} fermera les connections. Note: ce constructeur
     * sera ins�r�e directement dans le code du constructeur pr�c�dent si Sun donnait suite au
     * RFE ##4093999 ("Relax constraint on placement of this()/super() call in constructors").
     *
     * @throws RemoteException si la connexion au catalogue a �chou�.
     */
    private ParameterCoverage3D(final CoverageDataBase database) throws RemoteException {
        this(database.getCoverageTable());
        coverageTable.setOperation("NodataFilter");
        this.database = database;
    }

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera la table d'image
     * sp�cifi�e et son syst�me de coordonn�es. Ce constructeur d�l�gue le travail � celui
     * qui prend en argument un syst�me de coordonn�es.
     *
     * @param  coverages La table des images � utiliser. Il sera de la responsabilit� de
     *         l'utilisateur de fermer cette table lorsque cet objet ne sera plus utilis�.
     * @throws RemoteException si la connexion au catalogue a �chou�.
     */
    public ParameterCoverage3D(final CoverageTable coverages) throws RemoteException {
        this(coverages, coverages.getCoordinateSystem());
    }

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera la table d'image
     * sp�cifi�e. La {@linkplain CoverageTable#setGeographicArea zone g�ographique} et la
     * {@linkplain CoverageTable#setTimeRange plage de temps} d�finies sur cette table
     * affecteront toutes les images qui seront lus par cet objet <code>ParameterCoverage3D</code>.
     * En revanche, la {@linkplain CoverageTable#setSeries s�ries s�lectionn�e} ainsi que
     * l'�ventuelle {@linkplain CoverageTable#setOperation op�ration} seront ignor�es et
     * peut �tre �cras�es.
     *
     * @param  coverages La table des images � utiliser. Il sera de la responsabilit� de
     *         l'utilisateur de fermer cette table lorsque cet objet ne sera plus utilis�
     *         (�a ne sera pas fait automatiquement par {@link #dispose}, puisque la table
     *         sp�cifi�e n'appartient pas � cet objet <code>ParameterCoverage3D</code>).
     * @param  cs Le syst�me de coordonn�es � utiliser pour cet obet {@link Coverage}.
     *         Ce syst�me de coordonn�es doit obligatoirement comprendre un axe temporel.
     * @throws RemoteException si la connexion au catalogue a �chou�.
     */
    public ParameterCoverage3D(final CoverageTable coverages,
                               final CoordinateSystem cs)
            throws RemoteException
    {
        super("ParameterCoverage3D", cs);
        coverageTable = coverages;
    }

    /**
     * Obtient les donn�es sous forme d'objet {@link Coverage3D} pour le param�tre sp�cifi�.
     * Cette m�thode est appel�e automatiquement par {@link #setParameter setParameter} pour
     * obtenir les donn�es qui composent un param�tre. L'impl�mentation par d�faut construit
     * un objet {@link SeriesCoverage3D}. Les classes d�riv�es peuvent red�finir cette m�thode
     * pour construire un autre type de couverture, incluant un autre {@link ParameterCoverage3D}.
     *
     * @param parameter Le param�tre environnemental pour lequel on veut les donn�es.
     * @param table Une table d'images pr�-configur�e. Cette table est d�j� configur�e avec la
     *              s�rie d'image � lire, l'op�ration � appliquer ainsi que les coordonn�es
     *              spatio-temporelles de la r�gion d'int�r�t.
     */
    protected Coverage3D createCoverage3D(final ParameterEntry parameter,
                                          final CoverageTable  table)
            throws RemoteException
    {
        try {
            return new SeriesCoverage3D(table, getCoordinateSystem());
        } catch (TransformException e) {
            // Ne devrait pas se produire, puisque le syst�me de coordonn�es
            // est en principe le m�me que celui de la table.
            throw new CannotReprojectException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Retourne une clone de la couverture sp�cifi�e, si possible. L'utilisation de clones
     * pour la m�me s�rie d'images � des positions relatives diff�rentes permet d'�viter que
     * la m�me image soit recharg�e plusieurs fois, puisque chaque {@link SeriesCoverage3D}
     * gardera une r�f�rence forte vers la derni�re image lue.
     */
    private static Coverage3D clone(Coverage3D coverage) {
        if (coverage instanceof fr.ird.database.coverage.SeriesCoverage3D) {
            coverage = new SeriesCoverage3D((fr.ird.database.coverage.SeriesCoverage3D) coverage);
        }
        return coverage;
    }

    /**
     * Retourne le nom de l'op�ration � utiliser. L'appel de cette m�thode �quivaut � l'appel
     * de {@link OperationEntry#getProcessorOperation}, mais peut ajouter en plus l'op�ration
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
     * Retourne l'ensemble des param�tres disponibles. Cette m�thode n'est disponible que si
     * cet objet <code>ParameterCoverage3D</code> a �t� contruit avec le constructeur sans
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
     * Sp�cifie le param�tre � produire. Des couvertures spatiales seront produites � partir
     * des termes du {@linplain ParameterEntry#getLinearModel mod�le lin�aire} de ce param�tre,
     * s'il y en a un. Cette m�thode n'est disponible que si cet objet
     * <code>ParameterCoverage3D</code> a �t� contruit avec le constructeur sans argument.
     *
     * @param parameter Le param�tre � produire, ou <code>null</code> si aucun.
     * @throws RemoteException si la connexion au catalogue a �chou�.
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
     * Sp�cifie le param�tre � produire. Des couvertures spatiales seront produites � partir
     * du {@linplain ParameterEntry#getLinearModel mod�le lin�aire} de ce param�tre, s'il y
     * en a un.
     *
     * @param parameter Le param�tre � produire, ou <code>null</code> si aucun.
     * @throws RemoteException si la connexion au catalogue a �chou�.
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
         * Obtient tous les descripteurs du mod�le lin�aire.  Si ce param�tre n'a pas de mod�le,
         * alors il sera consid�r�e comme sa propre source le temps de construire l'ensemble des
         * objets Coverage3D.  Pour chaque s�rie impliqu�e, on construira un objet Coverage3D en
         * r�cup�rant ceux qui existent d�j� si possible.
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
         * associ�s. La table 'CoverageTable' ne sera interrog�e que si les donn�es n'�taient
         * pas d�j� disponibles. La s�rie de la table sera chang�e, mais toute op�ration et
         * zone spatio-temporelle pr�alablement s�lectionn�e dans la table seront conserv�es.
         * Notez que 'oldCoverages' ne doit pas �tre modifi�, car il est peut-�tre en cours
         * d'utilisation par 'evaluate' dans un autre thread.
         */
        for (int i=0; i<sources.length; i++) {
            final ParameterEntry source = sources[i];
            final int band = source.getBand(); // Les num�ros commencent � 1.
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
                    // Continue sans m�moriser le Coverage3D dans le dictonnaire 'coverages',
                    // puisqu'il y est d�j�.  C'est la seule exception;  tous les autres cas
                    // ci-dessous placeront leurs r�sultats dans 'coverages'.
                    continue;
                }
                coverage = oldCoverages.get(key);
                if (coverage == null) {
                    /*
                     * La s�rie demand�e n'existe pas d�j� pour le d�calage temporel sp�cifi�.
                     * V�rifie si elle existerait pour n'importe quel autre d�calage temporel.
                     * Cloner une s�rie existante est beaucoup plus rapide que d'en construire
                     * une identique � partir de la base de donn�es.
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
                             * La s�rie n'existait pas d�j�, quel que soit le d�calage temporel.
                             * Proc�de maintenant � la construction d'un nouvel objet Coverage3D
                             * pour cette s�rie.
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
         * Note: on aurait pu lib�rer les ressources des 'Coverage3D' qui ne sont plus utilis�s
         * (c'est-�-dire ceux qui sont encore dans 'oldCoverages' mais plus dans 'coverages').
         * Mais on ne le fait pas car ces 'Coverage3D' peuvent �tre encore utilis�s par la m�thode
         * 'evaluate', qui n'est que partiellement synchronis�e sur 'this' afin de permettre des
         * acc�s multi-thread aux donn�es.
         */
    }

    /**
     * Retourne le descripteur du param�tre � produire. Si aucun param�tre n'a �t� sp�cifi�,
     * alors cette m�thode retourne <code>null</code>.
     */
    public ParameterEntry getParameter() {
        return target;
    }

    /**
     * Retourne une envelope englobant les coordonn�es spatio-temporelles des donn�es.
     * Cette envelope sera l'intersection des envelopes de toutes les descripteurs du
     * param�tre � calculer.
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
     * D�finit la plage des valeurs qui seront produit en sortie. Cette plage de valeur
     * sera donn�e � l'objet {@link Category} qui repr�sentera le r�sultat du calcul.
     * Par d�faut, cette plage est calcul�e automatiquement.
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
     * L'impl�mentation par d�faut recherche les valeurs minimales et maximales que
     * peuvent produire les composantes du param�tre, et calcule la combinaison de
     * ces extremums afin de d�terminer la plage de valeurs de la bande en sortie.
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
     * Retourne la valeur d'un �chantillon ou d'un point arbitraire. Cette valeur sera calcul�e
     * en combinant tous les termes du {@linkplain ParameterEntry#getLinearModel mod�le lin�aire}
     * du param�tre sp�cifi� lors du dernier appel � {@link #setParameter}. Si aucun param�tre n'a
     * �t� sp�cifi�, alors cette m�thode retourne {@link Double#NaN}.
     *
     * Cette m�thode peut �tre appel�e simultan�ment par plusieurs threads.
     *
     * @param  sample L'�chantillon dont on veut la valeur du param�tre environnemental,
     *         ou <code>null</code>. Si non-null, alors les arguments <code>coordinate</code>
     *         et <code>time</code> <strong>doivent</strong> correspondre � la position de
     *         l'�chantillon (�a ne sera pas v�rifi�, sauf si les assertions sont activ�es).
     * @param  coordinate La coordonn�e spatiale du point � �valuer.
     * @param  time La date du point � �valuer.
     * @return La valeur du point aux coordonn�es spatio-temporelles sp�cifi�es.
     * @throws CannotEvaluateException si l'�valuation a �chou�e.
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
         * Effectue la combinaison des param�tres. Le code qui suit n'a pas besoin d'�tre
         * synchronis�, puisque les seules r�f�rences utilis�es ('linearModel','coverages'
         * et 'maxNumBands') ont �t� copi�es dans le bloc synchronis� pr�c�dent. Par design,
         * les instances r�f�r�s ne sont jamais modifi�s apr�s leur cr�ation par 'setParameters'.
         */
        double  value  = 0;                          // La valeur � retourner.
        boolean inside = false;                      // Vrai si au moins un param�tre a une valeur.
        PointOutsideCoverageException outside=null;  // La premi�re exception obtenue.
        final Point2D coord1 = new Point2D.Double(); // La coordonn�e spatiale d�cal�e.
        final Date     time1 = new Date(0);          // La coordonn�e temporelle d�cal�e.
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
                     * Obtient la couverture spatio-temporelle SeriesCoverage3D correspondant � la
                     * s�rie principale de la composante courante.  Si aucune valeur n'est trouv�e
                     * pour cette s�rie, alors seulement on examinera les s�ries "de secours".
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
         * Calcul terminer. Lance une exception si la coordonn�e sp�cifi�e tombait en dehors
         * de la couverture de *toutes* les s�ries. Autrement, les s�ries pour lesquelles le
         * point tombait en dehors auront simplement �t� consid�r�s comme des donn�es manquantes.
         */
        if (!inside && outside!=null) {
            throw outside;
        }
        return value;
    }

    /**
     * �value les valeurs de la couverture pour un �chantillon.
     *
     * L'impl�mentation par d�faut d�l�gue le travail �
     * <code>{@link #evaluate(SampleEntry,Point2D,Date) evaluate}(sample, ...)</code>.
     * Cette m�thode peut �tre appel�e simultan�ment par plusieurs threads.
     *
     * @param  sample �chantillon pour lequel on veut les param�tres environnementaux.
     * @param  position Doit �tre <code>null</code> pour l'impl�mentation actuelle.
     * @param  dest Tableau de destination, ou <code>null</code>.
     * @return Les param�tres environnementaux pour l'�chantillon sp�cifi�.
     * @throws CannotEvaluateException si l'�valuation a �chou�e.
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
     * Retourne la valeur � la coordonn�e spatio-temporelle sp�cifi�e. Cette valeur sera calcul�e
     * en combinant tous les termes du {@linkplain ParameterEntry#getLinearModel mod�le lin�aire}
     * du param�tre sp�cifi� lors du dernier appel � {@link #setParameter}. Si aucun param�tre n'a
     * �t� sp�cifi�, alors cette m�thode retourne {@link Double#NaN}.
     *
     * L'impl�mentation par d�faut d�l�gue le travail �
     * <code>{@link #evaluate(SampleEntry,Point2D,Date) evaluate}(null, coord, time)</code>.
     * Cette m�thode peut �tre appel�e simultan�ment par plusieurs threads.
     *
     * @param  coord La coordonn�e spatiale du point � �valuer.
     * @param  time  La date du point � �valuer.
     * @return La valeur du point aux coordonn�es spatio-temporelles sp�cifi�es.
     * @throws CannotEvaluateException si l'�valuation a �chou�e.
     */
    public double evaluate(final Point2D coord, final Date time) throws CannotEvaluateException {
        return evaluate(null, coord, time);
    }

    /**
     * Retourne la valeur � la coordonn�e spatio-temporelle sp�cifi�e.
     * L'impl�mentation par d�faut d�l�gue le travail �
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
     * Retourne la valeur � la coordonn�e spatio-temporelle sp�cifi�e.
     * L'impl�mentation par d�faut d�l�gue le travail �
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
     * Retourne la valeur � la coordonn�e spatio-temporelle sp�cifi�e.
     * L'impl�mentation par d�faut d�l�gue le travail �
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
     * Lib�re toutes les ressources utilis�es par cet objet. Cette m�thode devrait �tre appel�e
     * lorsque l'on sait que cet objet <code>ParameterCoverage3D</code> ne sera plus utilis�.
     * Notez que si une {@linkplain CoverageTable table des images} a �t� sp�cifi�e explicitement
     * au constructeur, elle ne sera pas ferm�e puisqu'elle n'appartient pas � cet objet
     * <code>ParameterCoverage3D</code>; il sera de la responsabilit� de l'utilisateur
     * de la fermer lui-m�me.
     */
    public synchronized void dispose() {
        try {
            setParameter((ParameterEntry)null);
            if (database != null) {
                coverageTable.close(); // A fermer seulement si 'database' est non-nul.
                database.close();
            }
        } catch (RemoteException exception) {
            // Des connexions n'ont pas pu �tre ferm�es. Mais puisque de toute fa�on
            // on ne va plus utiliser cet objet, ce n'est pas grave.
            Utilities.unexpectedException("fr.ird.database.sample", "ParameterCoverage3D",
                                          "dispose", exception);
        }
        coverageTable = null;
        database      = null;
        super.dispose();
    }

    /**
     * Lance la cr�ation d'images de potentiel de p�che � partir de la ligne de commande.
     * Les arguments sont:
     *
     * <ul>
     *   <li><code>-parameter=<var>P</var></code> o� <var>P</var> est un des param�tre �num�r�
     *       dans la table "Param�tre" de la base de donn�es des �chantillons (par exemple "PP1").</li>
     *   <li><code>-date=<var>date</var> est la date (en heure universelle) de l'image � g�n�rer
     *       (par exemple "18/08/1999"). Son format d�pend des conventions du syst�me,
     *       qui peut �tre sp�cifi� par l'argument <code>-locale</code>.</li>
     *   <li><code>-file=<var>file</var> est un nom de fichier optionel dans lequel enregistrer
     *       le r�sultat. Si cet argument est omis, alors l'image sera affich�e � l'�cran sans
     *       �tre enregistr�e.</li>
     *   <li><code>-locale=<var>locale</var> d�signe les conventions locales � utiliser pour lire
     *       l'argument <code>-date</code>. Si cet argument est omis, les conventions courantes du
     *       syst�me sont utilis�es.</li>
     *   <li><code>-generator=<var>classname</var></code> est le nom d'une sous-classe de {@link
     *        ParameterCoverage3D} � utiliser pour g�n�rer les images de potentiel. Si cet
     *        argument est omis, alors <code>fr.ird.database.sample.ParameterCoverage3D</code>
     *        est utilis� directement.</li>
     * </ul>
     *
     * @param  args Les param�tres transmis sur la ligne de commande.
     *
     * @throws ParseException si le format de la date n'est pas l�gal.
     * @throws RemoteException si une connexion � la base de donn�es a �chou�e.
     * @throws IOException si l'image ne peut pas �tre enregistr�e.
     * @throws ClassNotFoundException si la classe sp�cifi�e dans l'argument
     *         <code>-generator</code> n'a pas �t� trouv�e.
     * @throws ClassCastException si la classe sp�cifi�e dans l'argument
     *         <code>-generator</code> n'est pas une sous-classe de {@link ParameterCoverage3D}.
     * @throws InstantiationException si la classe sp�cifi�e dans l'argument
     *         <code>-generator</code> est abstraite.
     * @throws IllegalAccessException si la classe sp�cifi�e dans l'argument
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
         * Proc�de � la cr�ation de l'image, � son enregistrement puis � son affichage.
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
                // Force le calul imm�diat, avant que ne soit
                // ferm�e la connexion � la base de donn�es.
                coverage.getRenderedImage().getData();
            }
            FrameFactory.show(coverage);
        } finally {
            // Ferme la base de donn�es seulement apr�s la cr�ation de l'image,
            // sans quoi le calcul de l'image �chouera sans message d'erreur.
            coverage3D.dispose();
        }
    }
}
