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
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.awt.geom.Point2D;
import java.sql.SQLException;

// Geotools
import org.geotools.pt.Envelope;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.TransformException;
import org.geotools.cv.Coverage;
import org.geotools.cv.Category;
import org.geotools.cv.SampleDimension;
import org.geotools.cv.CannotEvaluateException;
import org.geotools.gp.CannotReprojectException;
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.database.DataBase;
import fr.ird.database.coverage.Coverage3D;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageDataBase;


/**
 * Une couverture spatiale repr�sentant une combinaison de param�tres. Cette couverture peut
 * servir par exemple � r�sumer dans une seule carte de potentiel les informations pr�sentes
 * dans plusieurs cartes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class CombinationCoverage3D extends Coverage {
    /**
     * Un objet {@link Map} vide a affecter � {@link #coverages}
     */
    private static Map<SeriesEntry,Coverage3D> EMPTY_MAP = (Map) Collections.EMPTY_MAP;

    /**
     * La base de donn�es d'images, ou <code>null</code> si <code>CombinationCoverage3D</code>
     * n'a pas construit lui-m�me cette base. Cette r�f�rence est conserv�e uniquement afin
     * d'�tre ferm�e par {@link #dispose}.
     *
     * @see #dispose
     */
    private DataBase database;

    /**
     * La table des images, ou <code>null</code> si cet objet <code>CombinationCoverage3D</code>
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
     * Les objets {@link Coverage3D} disponibles pour chaque {@linkplain SeriesEntry s�ries}.
     * Ce dictionnaire sera construit � chaque appel de {@link #setParameter}.
     */
    private Map<SeriesEntry,Coverage3D> coverages = EMPTY_MAP;

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
     * Construit un objet <code>CombinationCoverage3D</code> qui utilisera les
     * connections par d�faut. La m�thode {@link #dispose} fermera ces connections.
     *
     * @throws SQLException si la connexion � la base de donn�es a �chou�e.
     */
    public CombinationCoverage3D() throws SQLException {
        this(new fr.ird.database.coverage.sql.CoverageDataBase());
    }

    /**
     * Construit un objet <code>CombinationCoverage3D</code>  qui utilisera la base de donn�es
     * sp�cifi�e. La m�thode {@link #dispose} fermera les connections.   Note: ce constructeur
     * sera ins�r�e directement dans le code du constructeur pr�c�dent si Sun donnait suite au
     * RFE ##4093999 ("Relax constraint on placement of this()/super() call in constructors").
     *
     * @throws SQLException si la connexion � la base de donn�es a �chou�e.
     */
    private CombinationCoverage3D(final CoverageDataBase database) throws SQLException {
        this(database.getCoverageTable(), null);
        coverageTable.setOperation("NodataFilter");
        this.database = database;
    }

    /**
     * Construit un objet <code>CombinationCoverage3D</code> qui utilisera la table d'image
     * sp�cifi�e. La {@linkplain CoverageTable#setGeographicArea zone g�ographique}, la
     * {@linkplain CoverageTable#setTimeRange plage de temps} et l'�ventuelle
     * {@linkplain CoverageTable#setOperation op�ration} d�finie sur cette table affecteront
     * toutes les images qui seront lus par cet objet <code>CombinationCoverage3D</code>. En
     * revanche, la {@linkplain CoverageTable#setSeries s�ries} s�lectionn�e sera ignor�e et
     * peut �tre �cras�e.
     *
     * @param  coverages La table des images � utiliser. Il sera de la responsabilit� de
     *         l'utilisateur de fermer cette table lorsque cet objet ne sera plus utilis�
     *         (�a ne sera pas fait automatiquement par {@link #dispose}, puisque la table
     *         sp�cifi�e n'appartient pas � cet objet <code>CombinationCoverage3D</code>).
     * @param  cs Le syst�me de coordonn�es pour cet objet {@link Coverage}, or <code>null</code>
     *         pour utiliser celui de la table <code>coverages</code>.
     * @throws SQLException si la connexion � la base de donn�es a �chou�e.
     */
    public CombinationCoverage3D(final CoverageTable coverages,
                                 final CoordinateSystem cs)
            throws SQLException
    {
        super("CombinationCoverage3D", cs!=null ? cs : coverages.getCoordinateSystem(), null, null);
        coverageTable = coverages;
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
        final Map<SeriesEntry,Coverage3D> oldCoverages = coverages;
        coverages   = EMPTY_MAP;
        target      = null;
        components  = null;
        maxNumBands = 0;
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
        coverages = new HashMap<SeriesEntry,Coverage3D>();
        final Collection<+ParameterEntry.Component> list = parameter.getComponents();
        final ParameterEntry[] sources;
        if (list == null) {
            sources = new ParameterEntry[] {target};
        } else {
            int i=0;
            components = new ParameterEntry.Component[list.size()];
            sources    = new ParameterEntry[components.length];
            for (final ParameterEntry.Component component : list) {
                components[i] = component;
                sources   [i] = component.getSource();
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
            final ParameterEntry source = sources[i];
            final int band = source.getBand();
            if (band >= maxNumBands) {
                maxNumBands = band+1;
            }
            SeriesEntry series;
            for (int seriesIndex=0; (series=source.getSeries(seriesIndex))!=null; seriesIndex++) {
                Coverage3D coverage = coverages.get(series);
                if (coverage == null) {
                    coverage = oldCoverages.get(series);
                    if (coverage == null) synchronized (coverageTable) {
                        coverageTable.setSeries(series);
                        try {
                            coverage = new Coverage3D(coverageTable, getCoordinateSystem());
                        } catch (TransformException e) {
                            // Ne devrait pas se produire, puisque le syst�me de coordonn�es
                            // est en principe le m�me que celui de la table.
                            throw new CannotReprojectException(e.getLocalizedMessage(), e);
                        }
                    }
                    coverages.put(series, coverage);
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
            for (final Map.Entry<SeriesEntry,Coverage3D> entry : oldCoverages.entrySet()) {
                if (!coverages.containsKey(entry.getKey())) {
                    entry.getValue().dispose();
                }
            }
        }
        /*
         * Obtient une description de l'unique bande de cet objet. A cet fin, on recherchera
         * les valeurs minimales et maximales que peuvent produire les composantes du param�tre.
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
     */
    public synchronized SampleDimension[] getSampleDimensions() {
        if (sampleDimension == null) {
            sampleDimension = new SampleDimension();
        }
        return new SampleDimension[] {sampleDimension};
    }

    /**
     * Retourne la valeur � la coordonn�e spatio-temporelle sp�cifi�e. Cette valeur sera calcul�e
     * en combinant toutes les {@linkplain ParameterEntry#getComponents composantes} du param�tre
     * sp�cifi� lors du dernier appel � {@link #setParameter}. Si aucun param�tre n'a �t� sp�cifi�,
     * alors cette m�thode retourne {@link Double#NaN}.
     *
     * Cette m�thode peut �tre appel�e simultan�ment par plusieurs threads.
     *
     * @param  coord La coordonn�e spatiale du point � �valuer.
     * @param  time  La date du point � �valuer.
     * @return La valeur du point aux coordonn�es spatio-temporelles sp�cifi�es.
     * @throws CannotEvaluateException si l'�valuation a �chou�e.
     */
    public double evaluate(final Point2D coord, final Date time) throws CannotEvaluateException {
        final ParameterEntry.Component[] components;
        final Map<SeriesEntry,Coverage3D> coverages;
        double[] buffer;
        synchronized (this) {
            buffer     = new double[maxNumBands];
            components = this.components;
            coverages  = this.coverages;
            if (components == null) {
                if (target == null) {
                    return Double.NaN;
                }
                final int band = target.getBand();
                int seriesIndex = 0;
                do {
                    final SeriesEntry series = target.getSeries(seriesIndex++);
                    if (series == null) {
                        break;
                    }
                    buffer = coverages.get(series).evaluate(coord, time, buffer);
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
        double value = 0;
        final Point2D coord1 = new Point2D.Double();
        final Date     time1 = new Date(0);
        for (int i=0; i<components.length; i++) {
            final ParameterEntry.Component component = components[i];
            final ParameterEntry source = component.getSource();
            if (source.isIdentity()) {
                value += component.getWeight();
                continue;
            }
            coord1.setLocation(coord);
            time1.setTime(time.getTime());
            component.getRelativePosition().applyOffset(coord1, time1);
            final int band = source.getBand();
            int seriesIndex = 0;
            do {
                final SeriesEntry series = source.getSeries(seriesIndex++);
                if (series == null) {
                    break;
                }
                buffer = coverages.get(series).evaluate(coord1, time1, buffer);
            } while (Double.isNaN(buffer[band]));
            value += component.getWeight() * component.transform(buffer[band]);
        }
        return value;
    }

    /**
     * Lib�re toutes les ressources utilis�es par cet objet. Cette m�thode devrait �tre appel�e
     * lorsque l'on sait que cet objet <code>CombinationCoverage3D</code> ne sera plus utilis�.
     * Notez que si une {@linkplain CoverageTable table des images} a �t� sp�cifi�e explicitement
     * au constructeur, elle ne sera pas ferm�e puisqu'elle n'appartient pas � cet objet
     * <code>CombinationCoverage3D</code>; il sera de la responsabilit� de l'utilisateur
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
            Utilities.unexpectedException("fr.ird.database.sample", "CombinationCoverage3D",
                                          "dispose", exception);
        }
        coverageTable = null;
        database      = null;
        super.dispose();
    }
}
