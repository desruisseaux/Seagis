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
package fr.ird.sql.coupling;

// G�om�trie
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// Ensembles
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Arrays;

// Divers
import java.util.Date;
import java.sql.SQLException;

// JAI
import javax.media.jai.ParameterList;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.pt.CoordinatePoint;
import org.geotools.resources.Utilities;
import org.geotools.cv.PointOutsideCoverageException;

// Requ�tes SQL
import fr.ird.sql.image.Coverage3D;
import fr.ird.sql.image.ImageTable;
import fr.ird.sql.image.ImageEntry;
import fr.ird.sql.image.ImageDataBase;
import fr.ird.sql.image.SeriesTable;
import fr.ird.sql.image.SeriesEntry;
import fr.ird.sql.fishery.CatchEntry;
import fr.ird.sql.fishery.CatchTable;
import fr.ird.sql.fishery.FisheryDataBase;
import fr.ird.sql.fishery.EnvironmentTable;

// Evaluateurs
import fr.ird.operator.coverage.Evaluator;
import fr.ird.operator.coverage.AverageEvaluator;
import fr.ird.operator.coverage.GradientEvaluator;


/**
 * Classe ayant la charge de remplir la table  "Environnement"  de la base de donn�es "P�ches" �
 * partir des donn�es satellitaires. La table "Environnement" contient les valeurs de param�tres
 * environnementaux (temp�rature, chlorophylle-a, hauteur de l'eau, etc.) aux positions de p�ches.
 * Lorsque <code>EnvironmentTableFiller</code> trouve une donn�es environnementale � une position
 * de p�che, il met � jour la cellule correspondante de la table "Environnement". S'il ne trouve
 * pas de donn�es ou que la donn�e est manquante (par exemple sous un nuage), alors
 * <code>EnvironmentTableFiller</code> <strong>laisse inchang�e</strong> la cellule correspondante.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class EnvironmentTableFiller {
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
     *       base de donn�es des p�ches. Cette derni�re contient toutes les informations
     *       n�cessaire � cet effet.
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
     * Jours o� extraire des donn�es, avant, pendant et apr�s le jour de la p�che.
     * Il ne s'agit que d'une liste de jours � utiliser par d�faut (si l'utilisateur
     * n'a pas modifi� la s�lection). Cette liste doit �tre class�e en ordre croissant.
     */
    private static final int[] DAYS_TO_EVALUATE = {-30, -25, -20, -15, -10, -5, 0, 5};

    /**
     * Liste des op�rations par d�faut et des noms de colonnes dans lesquelles
     * m�moriser le r�sultat.
     *
     * TODO: Ces operations devraient appara�tre dans la base de donn�es.
     *       Une nouvelle table devra �tre cr��e.
     */
    private static final Operation[] DEFAULT_OPERATIONS = {
        new Operation(null,                  "valeur",   "Valeur interpol�e"),
        new Operation("Interpolate",         "pixel",    "Valeur sans interpolation"),
        new Operation("GradientMagnitude",   "sobel",    "Magnitude du gradient")
    };

    /**
     * Liste des s�ries � traiter. Chaque s�ries est associ�e � une liste de noms de param�tres
     * dans la base de donn�es. Le premier param�tre de la liste contiendra la valeur de la bande
     * 0; le second param�tre de la liste contiendra la valeur de la bande 1, etc.
     */
    private final Map<SeriesEntry,String[]> series = new LinkedHashMap<SeriesEntry,String[]>();

    /**
     * Date de d�part et de fin d'�chantillonage.
     * La valeur <code>null</code> signifie qu'aucune plage de temps n'a �t� fix�e.
     */
    private Date startTime, endTime;

    /**
     * Coordonn�es g�ographiques de la zone d'�tude. La valeur
     * <code>null</code> signifie qu'aucune zone n'a �t� fix�e.
     */
    private Rectangle2D geographicArea;

    /**
     * Op�ration � appliquer sur les donn�es.
     */
    private Operation operation = DEFAULT_OPERATIONS[0];

    /**
     * Une liste optionnelle d'arguments.
     */
    private final Map<String,Object> arguments = new HashMap<String,Object>();

    /**
     * La colonne de destination dans la base de donn�es.
     * Il s'agit habituellement de {@link Operation#column}.
     */
    private String column = operation.column;

    /**
     * Jours o� extraire des donn�es, avant, pendant et apr�s le jour de la p�che.
     */
    private int[] daysToEvaluate = DAYS_TO_EVALUATE;

    /**
     * Connection vers la base de donn�es d'images.
     */
    final ImageDataBase images;

    /**
     * Connection vers la base de donn�es des p�ches.
     */
    final FisheryDataBase p�ches;

    /**
     * Construit une connexion par d�faut.  Cette connexion utilisera des
     * param�tres par d�faut qui peuvent �tre pr�alablement configur�s en
     * ex�cutant {@link SQLControler} � partir de la ligne de commande.
     *
     * @throws SQLException si la connexion a �chou�e.
     */
    public EnvironmentTableFiller() throws SQLException {
        images = new ImageDataBase();
        p�ches = new FisheryDataBase();
        final SeriesTable series = images.getSeriesTable();
        for (int i=0; i<DEFAULT_SERIES.length; i++) {
            final String[] param = DEFAULT_SERIES[i];
            final String[] list = new String[param.length-1];
            System.arraycopy(param, 1, list, 0, list.length);
            this.series.put(series.getSeries(param[0]), list);
        }
        series.close();
    }

    /**
     * Retourne la liste des s�ries � traiter. Une s�rie peut �tre retir�e de la liste des
     * s�ries � traiter en appellant {@link Set#remove}. On peut aussi ne retenir qu'un sous
     * ensemble de s�ries en appellant {@link Set#retainAll}.
     */
    public Set<SeriesEntry> getSeries() {
        return series.keySet();
    }

    /**
     * Retourne la liste des captures qui seront � traiter.
     *
     * @return Les captures pour lesquelles on calculera des param�tres environnementaux.
     * @throws SQLException si une erreur est survenue lors de l'acc�s � la base de donn�es.
     */
    public CatchEntry[] getCatchs() throws SQLException {
        final CatchTable table = p�ches.getCatchTable();
        if (startTime!=null && endTime!=null) {
            table.setTimeRange(startTime, endTime);
        }
        if (geographicArea != null) {
            table.setGeographicArea(geographicArea);
        }
        if (false) {
            table.setCatchRange(250, Double.POSITIVE_INFINITY);
        }
        final List<CatchEntry> list = table.getEntries();
        final CatchEntry[] catchs = list.toArray(new CatchEntry[list.size()]);
        table.close();
        return catchs;
    }

    /**
     * D�finit les coordonn�es g�ographiques de la zone d'�tude.
     * La valeur <code>null</code> signifie qu'aucune restriction n'est impos�e.
     */
    public void setGeographicArea(final Rectangle2D area) {
        if (area != null) {
            if (geographicArea == null) {
                geographicArea = new Rectangle2D.Double();
            }
            geographicArea.setRect(area);
        }
        else geographicArea = null;
    }

    /**
     * D�finit les dates de d�but et de fin de la p�riode d'int�r�t.
     */
    public void setTimeRange(final Date start, final Date end) {
        startTime = (start!=null) ? new Date(start.getTime()) : null;
          endTime = (  end!=null) ? new Date(  end.getTime()) : null;
    }

    /**
     * Sp�cifie une op�ration � appliquer sur les images, ainsi que
     * la colonne dans laquelle m�moriser le r�sultat.
     *
     * @param operation L'op�ration � appliquer
     * @param column Le nom de la colonne de destination dans la base
     *        de donn�es, ou <code>null</code> pour le nom par d�faut.
     * @param arguments Une liste optionnelle d'arguments (peut �tre nulle).
     *        Seul les noms d'arguments attendu par l'op�ration seront pris
     *        en compte. Les autres arguments seront ignor�s.
     */
    final void setOperation(final Operation operation, final String column,
                            final Map<String,Object> arguments)
    {
        this.operation = operation;
        this.column = (column!=null) ? column : operation.column;
        this.arguments.clear();
        if (arguments != null) {
            this.arguments.putAll(arguments);
        }
    }

    /**
     * Retourne une liste d'op�ration reconnues par {@link #setOperation}.
     */
    final Operation[] getAvailableOperations() {
        return (Operation[]) DEFAULT_OPERATIONS.clone();
    }

    /**
     * Retourne les jours o� extraire des donn�es,
     * avant, pendant et apr�s le jour de la p�che.
     */
    final int[] getDaysToEvaluate() {
        return (int[]) daysToEvaluate.clone();
    }

    /**
     * D�finie les jours o� extraire des donn�es,
     * avant, pendant et apr�s le jour de la p�che.
     */
    final void setDaysToEvaluate(final int[] days) {
        daysToEvaluate = (int[]) days.clone();
    }

    /**
     * Lance le remplissage de la table "Environnement".
     *
     * @throws SQLException si un probl�me est survenu
     *         lors des acc�s � la base de donn�es.
     */
    public void run() throws SQLException {
        final CatchEntry[]   catchs = getCatchs();
        final ImageTable imageTable = images.getImageTable();
        final ParameterList  params = imageTable.setOperation(operation.name);
        if (true) {
            final String[] names = params.getParameterListDescriptor().getParamNames();
            for (int i=0; i<names.length; i++) {
                final Object value = arguments.get(names[i]);
                if (value != null) {
                    params.setParameter(names[i], value);
                }
            }
        }

        // Calcule les donn�es environnementales pour une s�rie � la fois.
        for (final Iterator<Map.Entry<SeriesEntry,String[]>> it=series.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<SeriesEntry,String[]> series = it.next();
            imageTable.setSeries(series.getKey());
            final CatchCoverage    coverage = new CatchCoverage(imageTable);
            coverage.setInterpolationAllowed(true);
            final Task[]              tasks = Task.getTasks(catchs, coverage, daysToEvaluate);
            final String[]       parameters = series.getValue();
            final EnvironmentTable[] update = new EnvironmentTable[TEST_ONLY ? 0 : parameters.length];
            for (int i=0; i<update.length; i++) {
                update[i] = p�ches.getEnvironmentTable(parameters[i], column);
            }
            operation.compute(tasks, coverage, update);
            for (int i=0; i<update.length; i++) {
                update[i].close();
            }
        }
        imageTable.close();
    }

    /**
     * Ferme les connections avec les bases de donn�es.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la fermeture des connections.
     */
    public void close() throws SQLException {
        p�ches.close();
        images.close();
    }

    /**
     * Lance le remplissage de la table "Environnement"
     * � partir de la ligne de commande.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors des acc�s aux bases de donn�es.
     */
    public static void main(final String[] args) throws SQLException {
        final EnvironmentTableFiller worker = new EnvironmentTableFiller();
        worker.run();
        worker.close();
    }
}
