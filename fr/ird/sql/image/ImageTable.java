/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Institut de Recherche pour le Développement
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
package fr.ird.sql.image;

// Base de données
import java.sql.SQLException;

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Envelope;
import net.seagis.gp.Operation;
import net.seagis.cs.CoordinateSystem;
import net.seagis.gp.GridCoverageProcessor;
import net.seagis.gp.OperationNotFoundException;

// Coordonnées et projections cartographiques
import java.util.Date;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;

// Divers
import java.util.List;
import fr.ird.sql.Table;
import net.seas.plot.RangeSet;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;


/**
 * Connection vers une table d'images. Cette table contient des références vers des images sous
 * forme d'objets {@link ImageEntry}.  Une table <code>ImageTable</code> est capable de fournir
 * la liste des entrés {@link ImageEntry} qui interceptent une certaines région géographique et
 * une certaine plage de dates.
 *
 * @see ImageDataBase#getImageTable
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface ImageTable extends Table
{
    /**
     * Retourne la référence vers la séries d'images.
     */
    public abstract SeriesEntry getSeries();

    /**
     * Définit la série dont on veut les images.
     *
     * @param  series       Réference vers la série d'images. Cette référence
     *                      est construite à partir du champ ID dans la table
     *                      "Series" de la base de données.
     * @throws SQLException si une erreur est survenu lors de l'accès à la
     *                      base de données, ou si <code>series</code> ne
     *                      se réfère pas à un enregistrement de la table
     *                      des séries.
     */
    public abstract void setSeries(final SeriesEntry series) throws SQLException;

    /**
     * Retourne le système de coordonnées utilisé pour les coordonnées spatio-temporelles de
     * <code>[get/set]Envelope(...)</code>. En général, ce système de coordonnées aura trois
     * dimensions (la dernière dimension étant le temps), soit dans l'ordre:
     * <ul>
     *   <li>Les longitudes, en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Les latitudes,  en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Le temps, en jours juliens depuis le 01/01/1950 00:00 UTC.</li>
     * </ul>
     */
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Retourne les coordonnées spatio-temporelles de la région d'intérêt. Le système
     * de coordonnées utilisé est celui retourné par {@link #getCoordinateSystem}.
     */
    public abstract Envelope getEnvelope();

    /**
     * Définit les coordonnées spatio-temporelles de la région d'intérêt. Le système de
     * coordonnées utilisé est celui retourné par {@link #getCoordinateSystem}. Appeler
     * cette méthode équivaut à effectuer les transformations nécessaires des coordonnées
     * et à appeler {@link #setTimeRange} et {@link #setGeographicArea}.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract void setEnvelope(final Envelope envelope) throws SQLException;

    /**
     * Retourne la période de temps d'intérêt.  Cette plage sera délimitée par des objets
     * {@link Date}. Appeler cette méthode équivant à n'extraire que la partie temporelle
     * de {@link #getEnvelope} et à transformer les coordonnées si nécessaire.
     */
    public abstract Range getTimeRange();

    /**
     * Définit la période de temps d'intérêt (dans laquelle rechercher des images).
     * Cette méthode ne change que la partie temporelle de l'enveloppe recherchée
     * (voir {@link #getEnvelope}).
     *
     * @param  range Période d'intérêt dans laquelle rechercher des images.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract void setTimeRange(final Range range) throws SQLException;

    /**
     * Définit la période de temps d'intérêt (dans laquelle rechercher des images).
     * Cette méthode ne change que la partie temporelle de l'enveloppe recherchée
     * (voir {@link #setEnvelope}).
     *
     * @param  startTime Date du  début de la plage de temps, inclusive.
     * @param  endTime   Date de la fin de la plage de temps, inclusive.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract void setTimeRange(final Date startTime, final Date endTime) throws SQLException;

    /**
     * Retourne les coordonnées géographiques de la région d'intérêt.
     * Les coordonnées seront exprimées en degrés de longitudes et de latitudes
     * selon l'ellipsoïde WGS 1984. Appeler cette méthode équivaut à n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et à transformer les
     * coordonnées si nécessaire.
     */
    public abstract Rectangle2D getGeographicArea();

    /**
     * Définit les coordonnées géographiques de la région d'intérêt   (dans laquelle rechercher des
     * images). Ces coordonnées sont toujours exprimées en degrés de longitude et de latitude selon
     * l'ellipsoïde WGS 1984. Cette méthode ne change que la partie horizontale de l'enveloppe (voir
     * {@link #setEnvelope}).
     *
     * @param  rect Coordonnées géographiques de la région, selon l'ellipsoïde WGS 1984.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract void setGeographicArea(final Rectangle2D rect) throws SQLException;

    /**
     * Retourne la dimension désirée des pixels de l'images.
     *
     * @return Résolution préférée, ou <code>null</code> si la lecture
     *         doit se faire avec la meilleure résolution disponible.
     */
    public abstract Dimension2D getPreferredResolution();

    /**
     * Définit la dimension désirée des pixels de l'images.  Cette information n'est
     * qu'approximative. Il n'est pas garantie que la lecture produira effectivement
     * des images de cette résolution. Une valeur nulle signifie que la lecture doit
     * se faire avec la meilleure résolution disponible.
     *
     * @param  pixelSize Taille préférée des pixels. Les unités sont les mêmes
     *         que celles de {@link #setGeographicArea}.
     * @throws SQLException si un accès à la base de données était nécessaire et a échouée.
     */
    public abstract void setPreferredResolution(final Dimension2D pixelSize) throws SQLException;

    /**
     * Retourne l'opération appliquée sur les images lues. L'opération retournée
     * peut représenter par exemple un gradient. Si aucune opération n'est appliquée
     * (c'est-à-dire si les images retournées représentent les données originales),
     * alors cette méthode retourne <code>null</code>.
     */
    public abstract Operation getOperation();

    /**
     * Définit l'opération à appliquer sur les images lues. Si des paramètres doivent
     * être spécifiés  en plus de l'opération,   ils peuvent l'être en appliquant des
     * méthodes <code>setParameter</code> sur la référence retournée. Par exemple, la
     * ligne suivante transforme tous les pixels des images à lire en appliquant
     * l'équation linéaire <code>value*constant+offset</code>:
     *
     * <blockquote><pre>
     * setOperation("Rescale").setParameter("constants", new double[]{10})
     *                        .setParameter("offsets"  , new double[]{50]);
     * </pre></blockquote>
     *
     * @param  operation L'opération à appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune opération.
     * @return Liste de paramètres par défaut, ou <code>null</code> si <code>operation</code>
     *         était nul. Les modifications apportées sur cette liste de paramètres influenceront
     *         les images obtenues lors du prochain appel d'une méthode <code>getEntry</code>.
     * @throws SQLException si un accès à la base de données était nécessaire et a échouée.
     */
    public abstract ParameterList setOperation(final Operation operation) throws SQLException;

    /**
     * Définit l'opération à appliquer sur les images lues. Cette méthode est équivalente à
     * <code>setOperation({@link GridImageProcessor#getOperation GridImageProcessor.getOperation}(name))</code>.
     *
     * @param  operation L'opération à appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune opération.
     * @return Liste de paramètres par défaut, ou <code>null</code> si <code>operation</code>
     *         était nul. Les modifications apportées sur cette liste de paramètres influenceront
     *         les images obtenues lors du prochain appel d'une méthode <code>getEntry</code>.
     * @throws SQLException si un accès à la base de données était nécessaire et a échouée.
     * @throws OperationNotFoundException si l'opération <code>operation</code> n'a pas été trouvée.
     */
    public abstract ParameterList setOperation(final String operation) throws SQLException, OperationNotFoundException;

    /**
     * Retourne la liste des images disponibles dans la plage de coordonnées
     * spatio-temporelles préalablement sélectionnées. Ces plages auront été
     * spécifiées à l'aide des différentes méthodes <code>set...</code> de
     * cette classe.
     *
     * @return Liste d'images qui interceptent la plage de temps et la région géographique d'intérêt.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract List<ImageEntry> getEntries() throws SQLException;

    /**
     * Retourne une des images disponibles dans la plage de coordonnées spatio-temporelles
     * préalablement sélectionnées. Si plusieurs images interceptent la région et la plage
     * de temps   (c'est-à-dire si {@link #getEntries} retourne un tableau d'au moins deux
     * entrées), alors le choix de l'image se fera en utilisant un objet {@link ImageComparator}
     * par défaut. Ce choix peut être arbitraire.
     *
     * @return Une image choisie arbitrairement dans la région et la plage de date
     *         sélectionnées, ou <code>null</code> s'il n'y a pas d'image dans ces plages.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract ImageEntry getEntry() throws SQLException;

    /**
     * Retourne l'image correspondant au numéro ID spécifié. L'argument <code>ID</code>
     * correspond au numéro  {@link ImageEntry#getID}  d'une des images retournées par
     * {@link #getEntries()} ou {@link #getEntry()}.  L'image demandée doit appartenir
     * à la série accédée par cette table (voir {@link #getSeries}). L'image retournée
     * sera découpée de façon à n'inclure que les coordonnées spécifiées lors du dernier
     * appel de {@link #setGeographicArea}.
     *
     * @param  ID Numéro identifiant l'image désirée.
     * @return L'image demandée, ou <code>null</code> si elle n'a pas été trouvée.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract ImageEntry getEntry(final int ID) throws SQLException;

    /**
     * Retourne l'image nommée. L'argument <code>name</code> correspond au nom {@link ImageEntry#getName}
     * d'une des images retournées par {@link #getEntries} ou {@link #getEntry()}.  L'image demandée doit
     * appartenir à la série accédée par cette table ({@link #getSeries}). L'image retournée sera découpée
     * de façon à n'inclure que les coordonnées spécifiées lors du dernier appel de {@link #setGeographicArea}.
     *
     * @param  name Nom de l'image désirée.
     * @return L'image demandée, ou <code>null</code> si elle n'a pas été trouvée.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract ImageEntry getEntry(final String name) throws SQLException;

    /**
     * Obtient les plages de temps et de coordonnées
     * couvertes par les images de cette table.
     *
     * @param x Objet dans lequel ajouter les plages de longitudes, ou <code>null</code> pour ne pas extraire ces plages.
     * @param y Objet dans lequel ajouter les plages de latitudes,  ou <code>null</code> pour ne pas extraire ces plages.
     * @param t Objet dans lequel ajouter les plages de temps,      ou <code>null</code> pour ne pas extraire ces plages.
     *
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de données.
     */
    public abstract void getRanges(final RangeSet x, final RangeSet y, final RangeSet t) throws SQLException;
}
