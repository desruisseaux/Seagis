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
 * Valeurs d'un {@link ParameterEntry param�tre} � des positions d'�chantillons.
 * Une couverture spatiale repr�sentant une combinaison de param�tres. Cette couverture peut
 * servir par exemple � r�sumer dans une seule carte de potentiel les informations pr�sentes
 * dans plusieurs cartes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class ParameterCoverage3D extends Coverage3D implements fr.ird.database.sample.Coverage3D {
    /**
     * Le prefix � ajouter devant les noms des op�rations pour ajouter l'op�ration
     * &quot;NodataFilter&quot;.
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
    private static final Color[] COLOR_PALETTE = {Color.BLUE, Color.WHITE, Color.RED};

    /**
     * La plage de valeurs � utiliser par d�faut pour les valeur index�es.
     */
    private static final NumberRange INDEX_RANGE = new NumberRange(1, 255);

    /**
     * Un objet {@link Map} vide a affecter � {@link #coverages}
     */
    private static Map<SeriesKey,Coverage3D> EMPTY_MAP = (Map) Collections.EMPTY_MAP;

    /**
     * Paire comprenant une {@linkplain SeriesEntry s�rie} avec {@linkplain OperationEntry
     * op�ration}. Ces paires sont utilis�es comme cl�s dans {@link #coverages}.
     *
     * @version $Id$
     * @author Martin Desruisseaux
     */
    private static final class SeriesKey {
        /** La s�rie. */                public SeriesEntry    series;
        /** L'op�ration � appliquer. */ public OperationEntry operation;

        /** Construit une nouvelle cl�. */
        public SeriesKey() {
        }

        /** Construit une nouvelle cl� pour la s�rie et l'op�ration sp�cifi�e. */
        public SeriesKey(final SeriesEntry series, final OperationEntry operation) {
            this.series    = series;
            this.operation = operation;
        }

        /** Retourne un code � peu pr�s unique pour cette cl�. */
        public int hashCode() {
            int code = series.hashCode();
            if (operation != null) {
                code += 37*operation.hashCode();
            }
            return code;
        }

        /** Compare cette cl� avec l'objet sp�cifi�. */
        public boolean equals(final Object object) {
            if (object instanceof SeriesKey) {
                final SeriesKey that = (SeriesKey) object;
                return Utilities.equals(this.series,    that.series) &&
                       Utilities.equals(this.operation, that.operation);
            }
            return false;
        }

        /** Retourne le nom de la s�rie et son op�ration. */
        public String toString() {
            if (operation == null) {
                return series.getName();
            }
            return operation.getName() + '[' + series.getName() + ']';
        }
    }

    /**
     * La base de donn�es d'images, ou <code>null</code> si <code>ParameterCoverage3D</code>
     * n'a pas construit lui-m�me cette base. Cette r�f�rence est conserv�e uniquement afin
     * d'�tre ferm�e par {@link #dispose}.
     *
     * @see #dispose
     */
    private DataBase database;

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
     * Le param�tre qui sera produit.
     *
     * @see #getParameter
     * @see #setParameter
     */
    private ParameterEntry target;

    /**
     * La liste des composantes du param�tre � produire.
     */
    private ParameterEntry.Component[] components;

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
     * Le processeur � utiliser pour appliquer des op�rations sur des images.
     */
    private final GridCoverageProcessor processor =
            fr.ird.database.coverage.sql.CoverageDataBase.getDefaultGridCoverageProcessor();

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera les
     * connections par d�faut. La m�thode {@link #dispose} fermera ces connections.
     *
     * @throws SQLException si la connexion � la base de donn�es a �chou�e.
     */
    public ParameterCoverage3D() throws SQLException {
        this(new fr.ird.database.coverage.sql.CoverageDataBase());
    }

    /**
     * Construit un objet <code>ParameterCoverage3D</code> qui utilisera la base de donn�es
     * sp�cifi�e. La m�thode {@link #dispose} fermera les connections. Note: ce constructeur
     * sera ins�r�e directement dans le code du constructeur pr�c�dent si Sun donnait suite au
     * RFE ##4093999 ("Relax constraint on placement of this()/super() call in constructors").
     *
     * @throws SQLException si la connexion � la base de donn�es a �chou�e.
     */
    private ParameterCoverage3D(final CoverageDataBase database) throws SQLException {
        this(database.getCoverageTable(), null);
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
     * @throws SQLException si la connexion � la base de donn�es a �chou�e.
     */
    public ParameterCoverage3D(final CoverageTable coverages)throws SQLException {
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
     * @throws SQLException si la connexion � la base de donn�es a �chou�e.
     */
    public ParameterCoverage3D(final CoverageTable coverages,
                               final CoordinateSystem cs)
            throws SQLException
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
            throws SQLException
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
     * Sp�cifie le param�tre � produire. Des couvertures spatiales seront produites � partir
     * des {@linplain ParameterEntry#getComponents composantes} de ce param�tre, s'il y en a.
     *
     * @param parameter Le param�tre � produire, ou <code>null</code> si aucun.
     * @throws SQLException si la connexion � la base de donn�es a �chou�e.
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
         * Obtient la liste des composantes du param�tre. Si ce param�tre n'a pas de composante,
         * alors il sera consid�r�e comme sa propre source le temps de construire l'ensemble des
         * objets Coverage3D.  Pour chaque s�rie impliqu�e, on construira un objet Coverage3D en
         * r�cup�rant ceux qui existent d�j� si possible.
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
         * associ�s. La table 'CoverageTable' ne sera interrog�e que si les donn�es n'�taient
         * pas d�j� disponibles. La s�rie de la table sera chang�e, mais toute op�ration et
         * zone spatio-temporelle pr�alablement s�lectionn�e dans la table seront conserv�es.
         * Notez que 'oldCoverages' ne doit pas �tre modifi�, car il est peut-�tre en cours
         * d'utilisation par 'evaluate' dans un autre thread.
         */
        for (int i=0; i<sources.length; i++) {
            final ParameterEntry source    = sources   [i];
            final OperationEntry operation = operations[i];
            final int band = source.getBand(); // Les num�ros commencent � 1.
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
         * Lib�re les ressources des 'Coverage3D' qui ne sont plus utilis�s. Ce bloc est pour
         * l'instant d�sactiv� car ces 'Coverage3D' peuvent �tre encore utilis�s par la m�thode
         * 'evaluate', qui n'est que partiellement synchronis�e sur 'this' afin de permettre des
         * acc�s simultan�s aux donn�es.
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
     * Retourne le descripteur du param�tre � produire. Si aucun param�tre n'a �t� sp�cifi�,
     * alors cette m�thode retourne <code>null</code>.
     */
    public ParameterEntry getParameter() {
        return target;
    }

    /**
     * Retourne une envelope englobant les coordonn�es spatio-temporelles des donn�es.
     * Cette envelope sera l'intersection des envelopes de toutes les composantes du
     * param�tre � calculer.
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
     * L'impl�mentation par d�faut recherche les valeurs minimales et maximales que
     * peuvent produire les composantes du param�tre, et calcule la combinaison de
     * ces extremums afin de d�terminer la plage de valeurs de la bande en sortie.
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
     * Retourne la valeur d'un �chantillon ou d'un point arbitraire. Cette valeur sera calcul�e
     * en combinant toutes les {@linkplain ParameterEntry#getComponents composantes} du param�tre
     * sp�cifi� lors du dernier appel � {@link #setParameter}. Si aucun param�tre n'a �t� sp�cifi�,
     * alors cette m�thode retourne {@link Double#NaN}.
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
         * Effectue la combinaison des param�tres. Le code qui suit n'a pas besoin d'�tre
         * synchronis�, puisque les seules r�f�rences utilis�es ('components','coverages'
         * et 'maxNumBands') ont �t� copi�es dans le bloc synchronis� pr�c�dent. Par design,
         * les instances r�f�r�s ne sont jamais modifi�s apr�s leur cr�ation par 'setParameters'.
         */
        double  value  = 0;                          // La valeur � retourner.
        boolean inside = false;                      // Vrai si au moins un param�tre a une valeur.
        PointOutsideCoverageException outside=null;  // La premi�re exception obtenue.
        final Point2D coord1 = new Point2D.Double(); // La coordonn�e spatiale d�cal�e.
        final Date     time1 = new Date(0);          // La coordonn�e temporelle d�cal�e.
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
            value += component.getWeight() * component.transform(buffer[band]);
        }
        /*
         * Calcul terminer. Lance une exception si la coordonn�e sp�cifi�e tombait en dehours
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
     * en combinant toutes les {@linkplain ParameterEntry#getComponents composantes} du param�tre
     * sp�cifi� lors du dernier appel � {@link #setParameter}. Si aucun param�tre n'a �t� sp�cifi�,
     * alors cette m�thode retourne {@link Double#NaN}.
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
            setParameter(null);
            if (database != null) {
                coverageTable.close(); // A fermer seulement si 'database' est non-nul.
                database.close();
            }
        } catch (SQLException exception) {
            // Des connexions n'ont pas pu �tre ferm�es. Mais puisque de toute fa�on
            // on ne va plus utiliser cet objet, ce n'est pas grave.
            Utilities.unexpectedException("fr.ird.database.sample", "ParameterCoverage3D",
                                          "dispose", exception);
        }
        coverageTable = null;
        database      = null;
        super.dispose();
    }
}
