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
 * Une couverture spatiale représentant une combinaison de paramètres. Cette couverture peut
 * servir par exemple à résumer dans une seule carte de potentiel les informations présentes
 * dans plusieurs cartes.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class CombinationCoverage3D extends Coverage {
    /**
     * Un objet {@link Map} vide a affecter à {@link #coverages}
     */
    private static Map<SeriesEntry,Coverage3D> EMPTY_MAP = (Map) Collections.EMPTY_MAP;

    /**
     * La base de données d'images, ou <code>null</code> si <code>CombinationCoverage3D</code>
     * n'a pas construit lui-même cette base. Cette référence est conservée uniquement afin
     * d'être fermée par {@link #dispose}.
     *
     * @see #dispose
     */
    private DataBase database;

    /**
     * La table des images, ou <code>null</code> si cet objet <code>CombinationCoverage3D</code>
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
     * Les objets {@link Coverage3D} disponibles pour chaque {@linkplain SeriesEntry séries}.
     * Ce dictionnaire sera construit à chaque appel de {@link #setParameter}.
     */
    private Map<SeriesEntry,Coverage3D> coverages = EMPTY_MAP;

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
     * Construit un objet <code>CombinationCoverage3D</code> qui utilisera les
     * connections par défaut. La méthode {@link #dispose} fermera ces connections.
     *
     * @throws SQLException si la connexion à la base de données a échouée.
     */
    public CombinationCoverage3D() throws SQLException {
        this(new fr.ird.database.coverage.sql.CoverageDataBase());
    }

    /**
     * Construit un objet <code>CombinationCoverage3D</code>  qui utilisera la base de données
     * spécifiée. La méthode {@link #dispose} fermera les connections.   Note: ce constructeur
     * sera insérée directement dans le code du constructeur précédent si Sun donnait suite au
     * RFE ##4093999 ("Relax constraint on placement of this()/super() call in constructors").
     *
     * @throws SQLException si la connexion à la base de données a échouée.
     */
    private CombinationCoverage3D(final CoverageDataBase database) throws SQLException {
        this(database.getCoverageTable(), null);
        coverageTable.setOperation("NodataFilter");
        this.database = database;
    }

    /**
     * Construit un objet <code>CombinationCoverage3D</code> qui utilisera la table d'image
     * spécifiée. La {@linkplain CoverageTable#setGeographicArea zone géographique}, la
     * {@linkplain CoverageTable#setTimeRange plage de temps} et l'éventuelle
     * {@linkplain CoverageTable#setOperation opération} définie sur cette table affecteront
     * toutes les images qui seront lus par cet objet <code>CombinationCoverage3D</code>. En
     * revanche, la {@linkplain CoverageTable#setSeries séries} sélectionnée sera ignorée et
     * peut être écrasée.
     *
     * @param  coverages La table des images à utiliser. Il sera de la responsabilité de
     *         l'utilisateur de fermer cette table lorsque cet objet ne sera plus utilisé
     *         (ça ne sera pas fait automatiquement par {@link #dispose}, puisque la table
     *         spécifiée n'appartient pas à cet objet <code>CombinationCoverage3D</code>).
     * @param  cs Le système de coordonnées pour cet objet {@link Coverage}, or <code>null</code>
     *         pour utiliser celui de la table <code>coverages</code>.
     * @throws SQLException si la connexion à la base de données a échouée.
     */
    public CombinationCoverage3D(final CoverageTable coverages,
                                 final CoordinateSystem cs)
            throws SQLException
    {
        super("CombinationCoverage3D", cs!=null ? cs : coverages.getCoordinateSystem(), null, null);
        coverageTable = coverages;
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
        final Map<SeriesEntry,Coverage3D> oldCoverages = coverages;
        coverages   = EMPTY_MAP;
        target      = null;
        components  = null;
        maxNumBands = 0;
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
         * associés. La table 'CoverageTable' ne sera interrogée que si les données n'étaient
         * pas déjà disponibles. La série de la table sera changée, mais toute opération et
         * zone spatio-temporelle préalablement sélectionnée dans la table seront conservées.
         * Notez que 'oldCoverages' ne doit pas être modifié, car il est peut-être en cours
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
                            // Ne devrait pas se produire, puisque le système de coordonnées
                            // est en principe le même que celui de la table.
                            throw new CannotReprojectException(e.getLocalizedMessage(), e);
                        }
                    }
                    coverages.put(series, coverage);
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
            for (final Map.Entry<SeriesEntry,Coverage3D> entry : oldCoverages.entrySet()) {
                if (!coverages.containsKey(entry.getKey())) {
                    entry.getValue().dispose();
                }
            }
        }
        /*
         * Obtient une description de l'unique bande de cet objet. A cet fin, on recherchera
         * les valeurs minimales et maximales que peuvent produire les composantes du paramètre.
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
     */
    public synchronized SampleDimension[] getSampleDimensions() {
        if (sampleDimension == null) {
            sampleDimension = new SampleDimension();
        }
        return new SampleDimension[] {sampleDimension};
    }

    /**
     * Retourne la valeur à la coordonnée spatio-temporelle spécifiée. Cette valeur sera calculée
     * en combinant toutes les {@linkplain ParameterEntry#getComponents composantes} du paramètre
     * spécifié lors du dernier appel à {@link #setParameter}. Si aucun paramètre n'a été spécifié,
     * alors cette méthode retourne {@link Double#NaN}.
     *
     * Cette méthode peut être appelée simultanément par plusieurs threads.
     *
     * @param  coord La coordonnée spatiale du point à évaluer.
     * @param  time  La date du point à évaluer.
     * @return La valeur du point aux coordonnées spatio-temporelles spécifiées.
     * @throws CannotEvaluateException si l'évaluation a échouée.
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
         * Effectue la combinaison des paramètres. Le code qui suit n'a pas besoin d'être
         * synchronisé, puisque les seules références utilisées ('components','coverages'
         * et 'maxNumBands') ont été copiées dans le bloc synchronisé précédent. Par design,
         * les instances référés ne sont jamais modifiés après leur création par 'setParameters'.
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
     * Libère toutes les ressources utilisées par cet objet. Cette méthode devrait être appelée
     * lorsque l'on sait que cet objet <code>CombinationCoverage3D</code> ne sera plus utilisé.
     * Notez que si une {@linkplain CoverageTable table des images} a été spécifiée explicitement
     * au constructeur, elle ne sera pas fermée puisqu'elle n'appartient pas à cet objet
     * <code>CombinationCoverage3D</code>; il sera de la responsabilité de l'utilisateur
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
            Utilities.unexpectedException("fr.ird.database.sample", "CombinationCoverage3D",
                                          "dispose", exception);
        }
        coverageTable = null;
        database      = null;
        super.dispose();
    }
}
