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
package fr.ird.database.sample;

// G�om�trie
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Ensembles
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Arrays;

// Divers
import java.util.Date;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import javax.media.jai.ParameterList;

// Geotools
import org.geotools.gc.GridCoverage;
import org.geotools.pt.CoordinatePoint;
import org.geotools.ct.TransformException;
import org.geotools.gp.GridCoverageProcessor; // Pour javadoc
import org.geotools.cv.PointOutsideCoverageException;
import org.geotools.resources.Utilities;

// Seagis
import fr.ird.database.Table;
import fr.ird.database.coverage.SeriesTable;
import fr.ird.database.coverage.SeriesEntry;
import fr.ird.database.coverage.CoverageTable;
import fr.ird.database.coverage.CoverageDataBase;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.resources.seagis.Resources;


/**
 * Remplit la table &quot;Environnement&quot; de la base de donn�es des �chantillons � partir des
 * donn�es satellitaires. La table &quot;Environnement&quot; contient les valeurs de param�tres
 * environnementaux (temp�rature, chlorophylle-a, hauteur de l'eau, etc.) aux positions des
 * �chantillons. Lorsque <code>EnvironmentTableFiller</code> trouve une donn�es environnementale
 * � une position d'un �chantillon, il met � jour la cellule correspondante de la table
 * &quot;Environnement&quot;. S'il ne trouve pas de donn�es ou que la donn�e est manquante
 * (par exemple sous un nuage), alors <code>EnvironmentTableFiller</code> <strong>laisse
 * inchang�e</strong> la cellule correspondante.
 * <br><br>
 * <strong>Choix des donn�es � calculer</strong><br>
 * Par d�faut, <code>EnvironmentTableFiller</code> calcule les donn�es environnementales de toutes
 * les {@linkplain SeriesTable s�ries d'images} pour tous les {@linkplain SampleEntry �chantillons}.
 *
 * Les s�ries � traiter peuvent �tre sp�cifi�e en appellant
 * <code>{@link #getSeries}.retainAll(series)</code>.
 *
 * La plage de temps des �chantillons � traiter peut �tre sp�cifi�e avec
 * <code>{@link #getSampleTable}.setTimeRange(startTime, endTime)</code>,
 * et de m�me pour les coordonn�es g�ographiques.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class EnvironmentTableFiller implements Table {
    /**
     * <code>true</code> si on veut seulement tester cette classe sans �crire
     * dans la base de donn�es. Note: une connexion en lecture aux bases de
     * donn�es est tout de m�me n�cessaire.
     */
    private static final boolean TEST_ONLY = false;

    /**
     * S�ries de donn�es � utiliser pour le remplissage des colonnes.
     * Pour chaque tableau <code>String[]</code>, le premier �l�ment
     * repr�sente la s�rie et les �l�ments suivants les colonnes de
     * la table "Environnement" � remplir pour chaque canal des images
     * de la s�rie.
     *
     * TODO: Cette liste devrait �tre construite � partir de la table "Parameters" de la
     *       base de donn�es des �chantillons. Cette derni�re contient toutes les informations
     *       n�cessaires � cet effet.
     */
    private static final String[][] DEFAULT_SERIES = {
        {"SST (synth�se)",                    "SST"},
        {"Chlorophylle-a (Monde)",            "CHL"},
        {"Pompage d'Ekman",                   "EKP"},
        {"SLA (R�union - NRT)",               "SLA", "U", "V"},
        {"SLA (R�union)",                     "SLA", "U", "V"},
        {"SLA (Monde - TP)",                  "SLA"},
        {"SLA (Monde - TP/ERS)",              "SLA"},
        {"Bathym�trie de Sandwell (R�union)", "FLR"},
        {"Bathym�trie de Baudry (R�union)",   "FLR"}
    };

    /**
     * Liste des s�ries � traiter. Chaque s�ries est associ�e � une liste de noms de param�tres
     * dans la base de donn�es. Le premier param�tre de la liste contiendra la valeur de la bande
     * 0; le second param�tre de la liste contiendra la valeur de la bande 1, etc.
     */
    private final Map<SeriesEntry,String[]> series = new LinkedHashMap<SeriesEntry,String[]>();

    /**
     * Liste des op�rations applicables.
     */
    private final Set<OperationEntry> operations;

    /**
     * Jours o� extraire des donn�es, avant, pendant et apr�s le jour de l'�chantillon.
     */
    private final Set<RelativePositionEntry> positions;

    /**
     * Connection vers la base de donn�es d'images.
     */
    private final CoverageDataBase coverages;

    /**
     * Connection vers la base de donn�es des �chantillons.
     */
    private final SampleDataBase samples;

    /**
     * La table des images. Ne sera construite que lorsqu'elle sera n�cessaire.
     */
    private CoverageTable coverageTable;

    /**
     * La table des �chantillons. Ne sera construite que lorsqu'elle sera n�cessaire.
     */
    private SampleTable sampleTable;

    /**
     * La table des s�ries. Ne sera construite que lorsqu'elle sera n�cessaire.
     */
    private SeriesTable seriesTable;

    /**
     * <code>true</code> si cet objet poss�de les connections vers les bases de donn�es
     * d'images et d'�chantillons. Dans ce cas, {@link #close} fermera ces connections.
     */
    private boolean canClose;

    /**
     * Indique si les interpolations spatio-temporelles sont permises.
     */
    private boolean interpolationAllowed = true;

    /**
     * Construit un objet utlisant une connexion par d�faut.  Cette connexion utilisera
     * des param�tres par d�faut qui peuvent �tre pr�alablement configur�s en ex�cutant
     * {@link SQLControler} � partir de la ligne de commande.
     *
     * @throws SQLException si la connexion a �chou�e.
     */
    public EnvironmentTableFiller() throws SQLException {
        this(new fr.ird.database.coverage.sql.CoverageDataBase(),
             new fr.ird.database.sample.sql.SampleDataBase());
        canClose = true;
    }

    /**
     * Construit un objet utilisant les connections specifi�es.
     *
     * @param  coverages Connexion vers la base de donn�es d'images.
     * @param  samples Connexion vers la base de donn�es des d'�chantillons.
     * @throws SQLException si la connexion a �chou�e.
     */
    public EnvironmentTableFiller(final CoverageDataBase coverages,
                                  final SampleDataBase   samples)
        throws SQLException
    {
        this.coverages   = coverages;
        this.samples     = samples;
        this.seriesTable = coverages.getSeriesTable();
        for (int i=0; i<DEFAULT_SERIES.length; i++) {
            final String[] param = DEFAULT_SERIES[i];
            final String[] list = new String[param.length-1];
            System.arraycopy(param, 1, list, 0, list.length);
            series.put(seriesTable.getEntry(param[0]), list);
        }
        positions  = samples.getRelativePositions();
        operations = samples.getOperations();
    }

    /**
     * Retourne l'ensemble des s�ries � traiter. Une s�rie peut �tre retir�e de l'ensemble
     * en appellant {@link Set#remove}. On peut aussi ne retenir qu'un sous ensemble de
     * s�ries en appellant {@link Set#retainAll}.
     */
    public final Set<SeriesEntry> getSeries() {
        return series.keySet();
    }

    /**
     * Retourne les op�rations qui seront � appliquer sur les param�tres environnementaux.
     * Cet ensemble contient initialement toutes les op�rations disponibles. L'utilisateur
     * devrait appeller {@link Set#retainAll} avec en argument les seuls op�rations qu'il
     * souhaite conserver.
     */
    public Set<OperationEntry> getOperations() {
        return operations;
    }

    /**
     * Retourne les positions relatives auxquelles extraires les param�tres environnementaux.
     * Cet ensemble contient initialement toutes les positions disponibles. L'utilisateur
     * devrait appeller {@link Set#retainAll} avec en argument les seuls positions qu'il
     * souhaite conserver.
     */
    public Set<RelativePositionEntry> getRelativePositions() {
        return positions;
    }

    /**
     * Retourne la table des images qui sera utilis�e pour obtenir les donn�es environnementales.
     * Cette table peut �tre configur�e en appelant ses m�thodes telles que
     * {@link CoverageTable#setOperation}, ce qui affectera toutes les donn�es qui seront
     * calcul�es lors du prochain appel de {@link #run}.
     *
     * <strong>Ne fermez pas cette table</strong>; sa fermeture sera plut�t prise en charge par
     * la m�thode {@link #close} de cet objet <code>EnvironmentTableFiller</code>.
     *
     * @return La table des images environnementales.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la base de donn�es.
     *
     * @task TODO: Envelopper dans un proxy qui ignore les appels de setSeries(...) et close().
     */
    public synchronized CoverageTable getCoverageTable() throws SQLException {
        if (coverageTable == null) {
            coverageTable = coverages.getCoverageTable();
        }
        return coverageTable;
    }

    /**
     * Retourne la table qui sera utilis�e pour obtenir les �chantillons. Cette table peut �tre
     * configur�e en appelant ses m�thodes telles que {@link SampleTable#setGeographicArea} et
     * {@link SampleTable#setTimeRange}, ce qui r�duira le nombre d'�chantillons qui seront trait�s
     * lors du prochain appel de {@link #run}.
     *
     * <strong>Ne fermez pas cette table</strong>; sa fermeture sera plut�t prise en charge par
     * la m�thode {@link #close} de cet objet <code>EnvironmentTableFiller</code>.
     *
     * @return La table des �chantillons.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la base de donn�es.
     *
     * @task TODO: Envelopper dans un proxy qui ignore les appels de close().
     */
    public synchronized SampleTable getSampleTable() throws SQLException {
        if (sampleTable == null) {
            sampleTable = samples.getSampleTable();
        }
        return sampleTable;
    }

    /**
     * Indique si cet objet est autoris� � interpoller dans l'espace et dans le temps.
     * La valeur par d�faut est <code>true</code>.
     */
    public boolean isInterpolationAllowed() {
        return interpolationAllowed;
    }

    /**
     * Sp�cifie si cet objet est autoris� � interpoller dans l'espace et dans le temps.
     * La valeur par d�faut est <code>true</code>.
     */
    public synchronized void setInterpolationAllowed(final boolean flag) {
        this.interpolationAllowed = flag;
    }

    /**
     * Lance le remplissage de la table &quot;Environnement&quot;.
     *
     * @throws SQLException si un probl�me est survenu lors des acc�s � la base de donn�es.
     * @throws TransformException si une transformation de coordonn�es �tait n�cessaire et
     *         a �chou�e.
     */
    public void run() throws SQLException, TransformException {
        SampleDataBase.LOGGER.info("Pr�pare le remplissage de la table d'environnement.");
        final Collection<SampleEntry> sampleEntries = getSampleTable().getEntries();
        final CoverageTable           coverageTable = getCoverageTable();
        final Set<OperationEntry>        operations = getOperations();
        final Set<RelativePositionEntry>  positions = getRelativePositions();
        for (final OperationEntry operation : operations) {
            /*
             * Pour chaque op�rations, configure la table d'images en lui appliquant l'op�ration
             * souhait�e, puis copie les arguments de l'op�ration dans son objet 'ParameterList'.
             */
            final ParameterList operationParameters;
            operationParameters = coverageTable.setOperation(operation.getProcessorOperation());
            if (operationParameters != null) {
                final String[] names = operationParameters.getParameterListDescriptor().getParamNames();
                for (int i=0; i<names.length; i++) {
                    final Object value = operation.getParameter(names[i]);
                    if (value != null) {
                        operationParameters.setParameter(names[i], value);
                    }
                }
            }
            /*
             * Pour chaque s�ries, construit un objet EnvironmentTable qui contiendra toutes les
             * bandes (par exemple "SLA", "U" et "V").   On d�clarera aussi toutes les positions
             * relatives, mais ces positions seront transmises de mani�re explicites plus loin.
             */
            for (final Map.Entry<SeriesEntry,String[]> series : this.series.entrySet()) {
                final Coverage3D       coverage;
                final SamplePosition[]    tasks;

                coverageTable.setSeries(series.getKey());
                coverage = new Coverage3D(coverageTable);
                coverage.setInterpolationAllowed(interpolationAllowed);
                tasks = SamplePosition.getInstances(sampleEntries, positions, coverage);
                info(ResourceKeys.POSITIONS_TO_EVALUATE_$1, new Integer(tasks.length));
                final EnvironmentTable table = samples.getEnvironmentTable(seriesTable);
                final String[] parameters = series.getValue();
                for (final RelativePositionEntry position : positions) {
                    for (int i=0; i<parameters.length; i++) {
                        // TODO: Utiliser la m�thode qui prend des Entry plut�t que des String.
                        table.addParameter(parameters[i], operation.getColumn(), position.getName(), false);
                    }
                }
                /*
                 * Proc�de maintenant � l'�valuation de toutes les valeurs et leur
                 * �criture dans la base de donn�es.
                 */
                double[] values = null;
                for (int i=0; i<tasks.length; i++) {
                    final SampleEntry           sample   = tasks[i].sample;
                    final RelativePositionEntry position = tasks[i].position;
                    try {
                        values = coverage.evaluate(sample, position, values);
                        coverage.lastWarning = null;
                    } catch (PointOutsideCoverageException exception) {
                        warning(coverage, exception);
                        continue;
                    }
                    table.set(sample, position, values);
                }
                table.close();
            }
        }
        SampleDataBase.LOGGER.info("Remplissage de la table d'environnement termin�.");
    }

    /**
     * Ecrit un message dans le journal avec le niveau "info".
     */
    private static void info(final int key, final Object arg) {
        LogRecord record = Resources.getResources(null).getLogRecord(Level.INFO, key, arg);
        record.setSourceClassName("EnvironmentTableFiller");
        record.setSourceMethodName("run");
        SampleDataBase.LOGGER.log(record);
    }

    /**
     * Indique qu'un point est en dehors de la r�gion des donn�es couvertes.
     * Cette m�thode �crit un avertissement dans le journal, � la condition
     * qu'il n'y en avait pas d�j� un.
     */
    private static void warning(final Coverage3D source, final PointOutsideCoverageException exception) {
        final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
        record.setSourceClassName ("EnvironmentTableFiller");
        record.setSourceMethodName("run");
        record.setThrown(exception);
        if (source.lastWarning == null) {
            source.log(record);
        }
        source.lastWarning = record;
    }

    /**
     * Ferme les connections avec les bases de donn�es.
     *
     * @throws SQLException si un probl�me est survenu lors de la fermeture des connections.
     */
    public void close() throws SQLException {
        if (sampleTable != null) {
            sampleTable.close();
            sampleTable = null;
        }
        if (seriesTable != null) {
            seriesTable.close();
            seriesTable = null;
        }
        if (coverageTable != null) {
            coverageTable.close();
            coverageTable = null;
        }
        if (canClose) {
            samples.close();
            coverages.close();
            canClose = false;
        }
    }
}
