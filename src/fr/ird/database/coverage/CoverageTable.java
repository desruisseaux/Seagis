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
 * Connection vers une table d'images. Cette table contient des r�f�rences vers des images sous
 * forme d'objets {@link CoverageEntry}. Une table <code>CoverageTable</code> est capable
 * de fournir la liste des entr�s {@link CoverageEntry} qui interceptent une certaines r�gion
 * g�ographique et une certaine plage de dates.
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
     * Retourne la r�f�rence vers la s�ries d'images.
     *
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract SeriesEntry getSeries() throws RemoteException;

    /**
     * D�finit la s�rie dont on veut les images.
     *
     * @param  series R�ference vers la s�rie d'images. Cette r�f�rence
     *                est construite � partir de la table "Series" de la base de donn�es.
     * @throws NoSuchRecordException si <code>series</code> ne se r�f�re pas � un enregistrement
     *         de la table des s�ries.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract void setSeries(final SeriesEntry series) throws RemoteException;

    /**
     * Retourne le syst�me de coordonn�es utilis� pour les coordonn�es spatio-temporelles de
     * <code>[get/set]Envelope(...)</code>. En g�n�ral, ce syst�me de coordonn�es aura trois
     * dimensions (la derni�re dimension �tant le temps), soit dans l'ordre:
     * <ul>
     *   <li>Les longitudes, en degr�s selon l'ellipso�de WGS 1984.</li>
     *   <li>Les latitudes,  en degr�s selon l'ellipso�de WGS 1984.</li>
     *   <li>Le temps, en jours juliens depuis le 01/01/1950 00:00 UTC.</li>
     * </ul>
     *
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract CoordinateSystem getCoordinateSystem() throws RemoteException;

    /**
     * Retourne les coordonn�es spatio-temporelles de la r�gion d'int�r�t. Le syst�me
     * de coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}.
     *
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract Envelope getEnvelope() throws RemoteException;

    /**
     * D�finit les coordonn�es spatio-temporelles de la r�gion d'int�r�t. Le syst�me de
     * coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}. Appeler
     * cette m�thode �quivaut � effectuer les transformations n�cessaires des coordonn�es
     * et � appeler {@link #setTimeRange} et {@link #setGeographicArea}.
     *
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract void setEnvelope(final Envelope envelope) throws RemoteException;

    /**
     * Retourne la p�riode de temps d'int�r�t.  Cette plage sera d�limit�e par des objets
     * {@link Date}. Appeler cette m�thode �quivant � n'extraire que la partie temporelle
     * de {@link #getEnvelope} et � transformer les coordonn�es si n�cessaire.
     *
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract Range getTimeRange() throws RemoteException;

    /**
     * D�finit la p�riode de temps d'int�r�t (dans laquelle rechercher des images).
     * Cette m�thode ne change que la partie temporelle de l'enveloppe recherch�e
     * (voir {@link #getEnvelope}).
     *
     * @param  range P�riode d'int�r�t dans laquelle rechercher des images.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract void setTimeRange(final Range range) throws RemoteException;

    /**
     * D�finit la p�riode de temps d'int�r�t (dans laquelle rechercher des images).
     * Cette m�thode ne change que la partie temporelle de l'enveloppe recherch�e
     * (voir {@link #setEnvelope}).
     *
     * @param  startTime Date du  d�but de la plage de temps, inclusive.
     * @param  endTime   Date de la fin de la plage de temps, inclusive.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract void setTimeRange(final Date startTime, final Date endTime) throws RemoteException;

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion d'int�r�t.
     * Les coordonn�es seront exprim�es en degr�s de longitudes et de latitudes
     * selon l'ellipso�de WGS 1984. Appeler cette m�thode �quivaut � n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et � transformer les
     * coordonn�es si n�cessaire.
     *
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract Rectangle2D getGeographicArea() throws RemoteException;

    /**
     * D�finit les coordonn�es g�ographiques de la r�gion d'int�r�t   (dans laquelle rechercher des
     * images). Ces coordonn�es sont toujours exprim�es en degr�s de longitude et de latitude selon
     * l'ellipso�de WGS 1984. Cette m�thode ne change que la partie horizontale de l'enveloppe (voir
     * {@link #setEnvelope}).
     *
     * @param  rect Coordonn�es g�ographiques de la r�gion, selon l'ellipso�de WGS 1984.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract void setGeographicArea(final Rectangle2D rect) throws RemoteException;

    /**
     * Retourne la dimension d�sir�e des pixels de l'images.
     *
     * @return R�solution pr�f�r�e, ou <code>null</code> si la lecture doit se faire avec
     *         la meilleure r�solution disponible.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     *         doit se faire avec la meilleure r�solution disponible.
     */
    public abstract Dimension2D getPreferredResolution() throws RemoteException;

    /**
     * D�finit la dimension d�sir�e des pixels de l'images.  Cette information n'est
     * qu'approximative. Il n'est pas garantie que la lecture produira effectivement
     * des images de cette r�solution. Une valeur nulle signifie que la lecture doit
     * se faire avec la meilleure r�solution disponible.
     *
     * @param  pixelSize Taille pr�f�r�e des pixels. Les unit�s sont les m�mes
     *         que celles de {@link #setGeographicArea}.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract void setPreferredResolution(final Dimension2D pixelSize) throws RemoteException;

    /**
     * Retourne l'op�ration appliqu�e sur les images lues. L'op�ration retourn�e
     * peut repr�senter par exemple un gradient. Si aucune op�ration n'est appliqu�e
     * (c'est-�-dire si les images retourn�es repr�sentent les donn�es originales),
     * alors cette m�thode retourne <code>null</code>.
     *
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract Operation getOperation() throws RemoteException;

    /**
     * D�finit l'op�ration � appliquer sur les images lues. Si des param�tres doivent
     * �tre sp�cifi�s  en plus de l'op�ration,   ils peuvent l'�tre en appliquant des
     * m�thodes <code>setParameter</code> sur la r�f�rence retourn�e. Par exemple, la
     * ligne suivante transforme tous les pixels des images � lire en appliquant
     * l'�quation lin�aire <code>value*constant+offset</code>:
     *
     * <blockquote><pre>
     * setOperation("Rescale").setParameter("constants", new double[]{10})
     *                        .setParameter("offsets"  , new double[]{50]);
     * </pre></blockquote>
     *
     * @param  operation L'op�ration � appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune op�ration.
     * @return Liste de param�tres par d�faut, ou <code>null</code> si <code>operation</code>
     *         �tait nul. Les modifications apport�es sur cette liste de param�tres influenceront
     *         les images obtenues lors du prochain appel d'une m�thode <code>getEntry</code>.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract ParameterList setOperation(final Operation operation) throws RemoteException;

    /**
     * D�finit l'op�ration � appliquer sur les images lues. Cette m�thode est �quivalente �
     * <code>setOperation({@link GridCoverageProcessor#getOperation GridCoverageProcessor.getOperation}(name))</code>.
     *
     * @param  operation L'op�ration � appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune op�ration.
     * @return Liste de param�tres par d�faut, ou <code>null</code> si <code>operation</code>
     *         �tait nul. Les modifications apport�es sur cette liste de param�tres influenceront
     *         les images obtenues lors du prochain appel d'une m�thode <code>getEntry</code>.
     * @throws OperationNotFoundException si l'op�ration <code>operation</code> n'a pas �t� trouv�e.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract ParameterList setOperation(final String operation) throws RemoteException, OperationNotFoundException;

    /**
     * Retourne la liste des images disponibles dans la plage de coordonn�es
     * spatio-temporelles pr�alablement s�lectionn�es. Ces plages auront �t�
     * sp�cifi�es � l'aide des diff�rentes m�thodes <code>set...</code> de
     * cette classe.
     *
     * @return Liste d'images qui interceptent la plage de temps et la r�gion g�ographique d'int�r�t.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract List<CoverageEntry> getEntries() throws RemoteException;

    /**
     * Retourne une des images disponibles dans la plage de coordonn�es spatio-temporelles
     * pr�alablement s�lectionn�es. Si plusieurs images interceptent la r�gion et la plage
     * de temps   (c'est-�-dire si {@link #getEntries} retourne un tableau d'au moins deux
     * entr�es), alors le choix de l'image se fera en utilisant un objet {@link CoverageComparator}
     * par d�faut. Ce choix peut �tre arbitraire.
     *
     * @return Une image choisie arbitrairement dans la r�gion et la plage de date
     *         s�lectionn�es, ou <code>null</code> s'il n'y a pas d'image dans ces plages.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract CoverageEntry getEntry() throws RemoteException;

    /**
     * Retourne l'image nomm�e. L'argument <code>name</code> correspond au nom {@link CoverageEntry#getName}
     * d'une des images retourn�es par {@link #getEntries} ou {@link #getEntry()}.  L'image demand�e doit
     * appartenir � la s�rie acc�d�e par cette table ({@link #getSeries}). L'image retourn�e sera d�coup�e
     * de fa�on � n'inclure que les coordonn�es sp�cifi�es lors du dernier appel de {@link #setGeographicArea}.
     *
     * @param  name Nom de l'image d�sir�e, sans son extension.
     * @return L'image demand�e, ou <code>null</code> si elle n'a pas �t� trouv�e.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public abstract CoverageEntry getEntry(final String name) throws RemoteException;

    /**
     * Obtient les plages de temps et de coordonn�es des images, ainsi que la liste des entr�es
     * correspondantes. L'objet retourn� ne contiendra que les informations demand�es. Par exemple
     * si {@link CoverageRanges#t} est <code>null</code>, alors la plage de temps ne sera pas
     * examin�e.
     *
     * @param  L'objet dans lequel ajouter les plages de cette s�ries. Pour chaque champs nul
     *         dans cet objet, les informations correspondantes ne seront pas interroger.
     * @return Un objet contenant les plages demand�es ansi que la liste des entr�es. Il ne
     *         s'agira pas n�cessairement du m�me objet que celui qui a �t� sp�cifi� en argument;
     *         �a d�pendra si cette m�thode est appel�e localement ou sur une machine distante.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public CoverageRanges getRanges(CoverageRanges ranges) throws RemoteException;

    /**
     * Ajoute une entr�e dans la table "<code>GridCoverages</code>". La m�thode
     * {@link #setSeries} doit d'abord avoir �t� appel�e au moins une fois.
     *
     * @param  coverage L'image � ajouter. Cette image doit avoir au moins trois dimensions,
     *         la troisi�me dimension �tant la date de l'image sur l'axe du temps.
     * @param  Le nom de l'image, sans son chemin ni son extension.
     * @return <code>true</code> si l'image a �t� ajout�e � la base de donn�es, ou <code>false</code>
     *         si une image avec le m�me nom existait d�j� pour la s�rie courante. Dans ce dernier cas,
     *         la base de donn�es ne sera pas modifi�e et un message d'avertissement sera �crit.
     * @throws RemoteException si un probl�me est survenu lors de la communication avec le serveur.
     */
    public boolean addGridCoverage(final GridCoverage coverage, final String filename) throws RemoteException;
}
