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
 */
package fr.ird.database.coverage;

// J2SE et JAI
import java.util.List;
import java.util.Date;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;
import java.rmi.RemoteException;
import javax.media.jai.util.Range;
import javax.media.jai.ParameterList;

// Geotools
import org.geotools.pt.Envelope;
import org.geotools.gp.Operation;
import org.geotools.gc.GridCoverage;
import org.geotools.cs.CoordinateSystem;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.gp.OperationNotFoundException;
import org.geotools.util.RangeSet;

// Seagis
import fr.ird.database.Table;


/**
 * Connection vers une table d'images. Cette table contient des références vers des images sous
 * forme d'objets {@link CoverageEntry}. Une table <code>CoverageTable</code> est capable
 * de fournir la liste des entrés {@link CoverageEntry} qui interceptent une certaines région
 * géographique et une certaine plage de dates.
 *
 * @see CoverageDataBase#getCoverageTable
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface CoverageTable extends Table {
    /**
     * {@inheritDoc}
     */
    public abstract CoverageDataBase getDataBase() throws RemoteException;

    /**
     * Retourne la référence vers la séries d'images.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract SeriesEntry getSeries() throws RemoteException;

    /**
     * Définit la série dont on veut les images.
     *
     * @param  series Réference vers la série d'images. Cette référence
     *                est construite à partir de la table "Series" de la base de données.
     * @throws NoSuchRecordException si <code>series</code> ne se réfère pas à un enregistrement
     *         de la table des séries.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract void setSeries(final SeriesEntry series) throws RemoteException;

    /**
     * Retourne le système de coordonnées utilisé pour les coordonnées spatio-temporelles de
     * <code>[get/set]Envelope(...)</code>. En général, ce système de coordonnées aura trois
     * dimensions (la dernière dimension étant le temps), soit dans l'ordre:
     * <ul>
     *   <li>Les longitudes, en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Les latitudes,  en degrés selon l'ellipsoïde WGS 1984.</li>
     *   <li>Le temps, en jours juliens depuis le 01/01/1950 00:00 UTC.</li>
     * </ul>
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract CoordinateSystem getCoordinateSystem() throws RemoteException;

    /**
     * Retourne les coordonnées spatio-temporelles de la région d'intérêt. Le système
     * de coordonnées utilisé est celui retourné par {@link #getCoordinateSystem}.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract Envelope getEnvelope() throws RemoteException;

    /**
     * Définit les coordonnées spatio-temporelles de la région d'intérêt. Le système de
     * coordonnées utilisé est celui retourné par {@link #getCoordinateSystem}. Appeler
     * cette méthode équivaut à effectuer les transformations nécessaires des coordonnées
     * et à appeler {@link #setTimeRange} et {@link #setGeographicArea}.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract void setEnvelope(final Envelope envelope) throws RemoteException;

    /**
     * Retourne la période de temps d'intérêt.  Cette plage sera délimitée par des objets
     * {@link Date}. Appeler cette méthode équivant à n'extraire que la partie temporelle
     * de {@link #getEnvelope} et à transformer les coordonnées si nécessaire.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract Range getTimeRange() throws RemoteException;

    /**
     * Définit la période de temps d'intérêt (dans laquelle rechercher des images).
     * Cette méthode ne change que la partie temporelle de l'enveloppe recherchée
     * (voir {@link #getEnvelope}).
     *
     * @param  range Période d'intérêt dans laquelle rechercher des images.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract void setTimeRange(final Range range) throws RemoteException;

    /**
     * Définit la période de temps d'intérêt (dans laquelle rechercher des images).
     * Cette méthode ne change que la partie temporelle de l'enveloppe recherchée
     * (voir {@link #setEnvelope}).
     *
     * @param  startTime Date du  début de la plage de temps, inclusive.
     * @param  endTime   Date de la fin de la plage de temps, inclusive.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract void setTimeRange(final Date startTime, final Date endTime) throws RemoteException;

    /**
     * Retourne les coordonnées géographiques de la région d'intérêt.
     * Les coordonnées seront exprimées en degrés de longitudes et de latitudes
     * selon l'ellipsoïde WGS 1984. Appeler cette méthode équivaut à n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et à transformer les
     * coordonnées si nécessaire.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract Rectangle2D getGeographicArea() throws RemoteException;

    /**
     * Définit les coordonnées géographiques de la région d'intérêt   (dans laquelle rechercher des
     * images). Ces coordonnées sont toujours exprimées en degrés de longitude et de latitude selon
     * l'ellipsoïde WGS 1984. Cette méthode ne change que la partie horizontale de l'enveloppe (voir
     * {@link #setEnvelope}).
     *
     * @param  rect Coordonnées géographiques de la région, selon l'ellipsoïde WGS 1984.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract void setGeographicArea(final Rectangle2D rect) throws RemoteException;

    /**
     * Retourne la dimension désirée des pixels de l'images.
     *
     * @return Résolution préférée, ou <code>null</code> si la lecture doit se faire avec
     *         la meilleure résolution disponible.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     *         doit se faire avec la meilleure résolution disponible.
     */
    public abstract Dimension2D getPreferredResolution() throws RemoteException;

    /**
     * Définit la dimension désirée des pixels de l'images.  Cette information n'est
     * qu'approximative. Il n'est pas garantie que la lecture produira effectivement
     * des images de cette résolution. Une valeur nulle signifie que la lecture doit
     * se faire avec la meilleure résolution disponible.
     *
     * @param  pixelSize Taille préférée des pixels. Les unités sont les mêmes
     *         que celles de {@link #setGeographicArea}.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract void setPreferredResolution(final Dimension2D pixelSize) throws RemoteException;

    /**
     * Retourne l'opération appliquée sur les images lues. L'opération retournée
     * peut représenter par exemple un gradient. Si aucune opération n'est appliquée
     * (c'est-à-dire si les images retournées représentent les données originales),
     * alors cette méthode retourne <code>null</code>.
     *
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract Operation getOperation() throws RemoteException;

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
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract ParameterList setOperation(final Operation operation) throws RemoteException;

    /**
     * Définit l'opération à appliquer sur les images lues. Cette méthode est équivalente à
     * <code>setOperation({@link GridCoverageProcessor#getOperation GridCoverageProcessor.getOperation}(name))</code>.
     *
     * @param  operation L'opération à appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune opération.
     * @return Liste de paramètres par défaut, ou <code>null</code> si <code>operation</code>
     *         était nul. Les modifications apportées sur cette liste de paramètres influenceront
     *         les images obtenues lors du prochain appel d'une méthode <code>getEntry</code>.
     * @throws OperationNotFoundException si l'opération <code>operation</code> n'a pas été trouvée.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract ParameterList setOperation(final String operation) throws RemoteException, OperationNotFoundException;

    /**
     * Retourne la liste des images disponibles dans la plage de coordonnées
     * spatio-temporelles préalablement sélectionnées. Ces plages auront été
     * spécifiées à l'aide des différentes méthodes <code>set...</code> de
     * cette classe.
     *
     * @return Liste d'images qui interceptent la plage de temps et la région géographique d'intérêt.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract List<CoverageEntry> getEntries() throws RemoteException;

    /**
     * Retourne une des images disponibles dans la plage de coordonnées spatio-temporelles
     * préalablement sélectionnées. Si plusieurs images interceptent la région et la plage
     * de temps   (c'est-à-dire si {@link #getEntries} retourne un tableau d'au moins deux
     * entrées), alors le choix de l'image se fera en utilisant un objet {@link CoverageComparator}
     * par défaut. Ce choix peut être arbitraire.
     *
     * @return Une image choisie arbitrairement dans la région et la plage de date
     *         sélectionnées, ou <code>null</code> s'il n'y a pas d'image dans ces plages.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract CoverageEntry getEntry() throws RemoteException;

    /**
     * Retourne l'image nommée. L'argument <code>name</code> correspond au nom {@link CoverageEntry#getName}
     * d'une des images retournées par {@link #getEntries} ou {@link #getEntry()}.  L'image demandée doit
     * appartenir à la série accédée par cette table ({@link #getSeries}). L'image retournée sera découpée
     * de façon à n'inclure que les coordonnées spécifiées lors du dernier appel de {@link #setGeographicArea}.
     *
     * @param  name Nom de l'image désirée, sans son extension.
     * @return L'image demandée, ou <code>null</code> si elle n'a pas été trouvée.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public abstract CoverageEntry getEntry(final String name) throws RemoteException;

    /**
     * Obtient les plages de temps et de coordonnées des images, ainsi que la liste des entrées
     * correspondantes. L'objet retourné ne contiendra que les informations demandées. Par exemple
     * si {@link CoverageRanges#t} est <code>null</code>, alors la plage de temps ne sera pas
     * examinée.
     *
     * @param  L'objet dans lequel ajouter les plages de cette séries. Pour chaque champs nul
     *         dans cet objet, les informations correspondantes ne seront pas interroger.
     * @return Un objet contenant les plages demandées ansi que la liste des entrées. Il ne
     *         s'agira pas nécessairement du même objet que celui qui a été spécifié en argument;
     *         ça dépendra si cette méthode est appelée localement ou sur une machine distante.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public CoverageRanges getRanges(CoverageRanges ranges) throws RemoteException;

    /**
     * Ajoute une entrée dans la table "<code>GridCoverages</code>". La méthode
     * {@link #setSeries} doit d'abord avoir été appelée au moins une fois.
     *
     * @param  coverage L'image à ajouter. Cette image doit avoir au moins trois dimensions,
     *         la troisième dimension étant la date de l'image sur l'axe du temps.
     * @param  Le nom de l'image, sans son chemin ni son extension.
     * @return <code>true</code> si l'image a été ajoutée à la base de données, ou <code>false</code>
     *         si une image avec le même nom existait déjà pour la série courante. Dans ce dernier cas,
     *         la base de données ne sera pas modifiée et un message d'avertissement sera écrit.
     * @throws RemoteException si un problème est survenu lors de la communication avec le serveur.
     */
    public boolean addGridCoverage(final GridCoverage coverage, final String filename) throws RemoteException;
}
