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
package fr.ird.sql.image;

// Base de donn�es
import java.sql.SQLException;

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Envelope;
import net.seagis.gp.Operation;
import net.seagis.cs.CoordinateSystem;
import net.seagis.gp.GridCoverageProcessor;
import net.seagis.gp.OperationNotFoundException;

// Coordonn�es et projections cartographiques
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
 * Connection vers une table d'images. Cette table contient des r�f�rences vers des images sous
 * forme d'objets {@link ImageEntry}.  Une table <code>ImageTable</code> est capable de fournir
 * la liste des entr�s {@link ImageEntry} qui interceptent une certaines r�gion g�ographique et
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
     * Retourne la r�f�rence vers la s�ries d'images.
     */
    public abstract SeriesEntry getSeries();

    /**
     * D�finit la s�rie dont on veut les images.
     *
     * @param  series       R�ference vers la s�rie d'images. Cette r�f�rence
     *                      est construite � partir du champ ID dans la table
     *                      "Series" de la base de donn�es.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la
     *                      base de donn�es, ou si <code>series</code> ne
     *                      se r�f�re pas � un enregistrement de la table
     *                      des s�ries.
     */
    public abstract void setSeries(final SeriesEntry series) throws SQLException;

    /**
     * Retourne le syst�me de coordonn�es utilis� pour les coordonn�es spatio-temporelles de
     * <code>[get/set]Envelope(...)</code>. En g�n�ral, ce syst�me de coordonn�es aura trois
     * dimensions (la derni�re dimension �tant le temps), soit dans l'ordre:
     * <ul>
     *   <li>Les longitudes, en degr�s selon l'ellipso�de WGS 1984.</li>
     *   <li>Les latitudes,  en degr�s selon l'ellipso�de WGS 1984.</li>
     *   <li>Le temps, en jours juliens depuis le 01/01/1950 00:00 UTC.</li>
     * </ul>
     */
    public abstract CoordinateSystem getCoordinateSystem();

    /**
     * Retourne les coordonn�es spatio-temporelles de la r�gion d'int�r�t. Le syst�me
     * de coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}.
     */
    public abstract Envelope getEnvelope();

    /**
     * D�finit les coordonn�es spatio-temporelles de la r�gion d'int�r�t. Le syst�me de
     * coordonn�es utilis� est celui retourn� par {@link #getCoordinateSystem}. Appeler
     * cette m�thode �quivaut � effectuer les transformations n�cessaires des coordonn�es
     * et � appeler {@link #setTimeRange} et {@link #setGeographicArea}.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract void setEnvelope(final Envelope envelope) throws SQLException;

    /**
     * Retourne la p�riode de temps d'int�r�t.  Cette plage sera d�limit�e par des objets
     * {@link Date}. Appeler cette m�thode �quivant � n'extraire que la partie temporelle
     * de {@link #getEnvelope} et � transformer les coordonn�es si n�cessaire.
     */
    public abstract Range getTimeRange();

    /**
     * D�finit la p�riode de temps d'int�r�t (dans laquelle rechercher des images).
     * Cette m�thode ne change que la partie temporelle de l'enveloppe recherch�e
     * (voir {@link #getEnvelope}).
     *
     * @param  range P�riode d'int�r�t dans laquelle rechercher des images.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract void setTimeRange(final Range range) throws SQLException;

    /**
     * D�finit la p�riode de temps d'int�r�t (dans laquelle rechercher des images).
     * Cette m�thode ne change que la partie temporelle de l'enveloppe recherch�e
     * (voir {@link #setEnvelope}).
     *
     * @param  startTime Date du  d�but de la plage de temps, inclusive.
     * @param  endTime   Date de la fin de la plage de temps, inclusive.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract void setTimeRange(final Date startTime, final Date endTime) throws SQLException;

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion d'int�r�t.
     * Les coordonn�es seront exprim�es en degr�s de longitudes et de latitudes
     * selon l'ellipso�de WGS 1984. Appeler cette m�thode �quivaut � n'extraire
     * que la partie horizontale de  {@link #getEnvelope}  et � transformer les
     * coordonn�es si n�cessaire.
     */
    public abstract Rectangle2D getGeographicArea();

    /**
     * D�finit les coordonn�es g�ographiques de la r�gion d'int�r�t   (dans laquelle rechercher des
     * images). Ces coordonn�es sont toujours exprim�es en degr�s de longitude et de latitude selon
     * l'ellipso�de WGS 1984. Cette m�thode ne change que la partie horizontale de l'enveloppe (voir
     * {@link #setEnvelope}).
     *
     * @param  rect Coordonn�es g�ographiques de la r�gion, selon l'ellipso�de WGS 1984.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract void setGeographicArea(final Rectangle2D rect) throws SQLException;

    /**
     * Retourne la dimension d�sir�e des pixels de l'images.
     *
     * @return R�solution pr�f�r�e, ou <code>null</code> si la lecture
     *         doit se faire avec la meilleure r�solution disponible.
     */
    public abstract Dimension2D getPreferredResolution();

    /**
     * D�finit la dimension d�sir�e des pixels de l'images.  Cette information n'est
     * qu'approximative. Il n'est pas garantie que la lecture produira effectivement
     * des images de cette r�solution. Une valeur nulle signifie que la lecture doit
     * se faire avec la meilleure r�solution disponible.
     *
     * @param  pixelSize Taille pr�f�r�e des pixels. Les unit�s sont les m�mes
     *         que celles de {@link #setGeographicArea}.
     * @throws SQLException si un acc�s � la base de donn�es �tait n�cessaire et a �chou�e.
     */
    public abstract void setPreferredResolution(final Dimension2D pixelSize) throws SQLException;

    /**
     * Retourne l'op�ration appliqu�e sur les images lues. L'op�ration retourn�e
     * peut repr�senter par exemple un gradient. Si aucune op�ration n'est appliqu�e
     * (c'est-�-dire si les images retourn�es repr�sentent les donn�es originales),
     * alors cette m�thode retourne <code>null</code>.
     */
    public abstract Operation getOperation();

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
     * @throws SQLException si un acc�s � la base de donn�es �tait n�cessaire et a �chou�e.
     */
    public abstract ParameterList setOperation(final Operation operation) throws SQLException;

    /**
     * D�finit l'op�ration � appliquer sur les images lues. Cette m�thode est �quivalente �
     * <code>setOperation({@link GridImageProcessor#getOperation GridImageProcessor.getOperation}(name))</code>.
     *
     * @param  operation L'op�ration � appliquer sur les images, ou <code>null</code> pour
     *         n'appliquer aucune op�ration.
     * @return Liste de param�tres par d�faut, ou <code>null</code> si <code>operation</code>
     *         �tait nul. Les modifications apport�es sur cette liste de param�tres influenceront
     *         les images obtenues lors du prochain appel d'une m�thode <code>getEntry</code>.
     * @throws SQLException si un acc�s � la base de donn�es �tait n�cessaire et a �chou�e.
     * @throws OperationNotFoundException si l'op�ration <code>operation</code> n'a pas �t� trouv�e.
     */
    public abstract ParameterList setOperation(final String operation) throws SQLException, OperationNotFoundException;

    /**
     * Retourne la liste des images disponibles dans la plage de coordonn�es
     * spatio-temporelles pr�alablement s�lectionn�es. Ces plages auront �t�
     * sp�cifi�es � l'aide des diff�rentes m�thodes <code>set...</code> de
     * cette classe.
     *
     * @return Liste d'images qui interceptent la plage de temps et la r�gion g�ographique d'int�r�t.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract List<ImageEntry> getEntries() throws SQLException;

    /**
     * Retourne une des images disponibles dans la plage de coordonn�es spatio-temporelles
     * pr�alablement s�lectionn�es. Si plusieurs images interceptent la r�gion et la plage
     * de temps   (c'est-�-dire si {@link #getEntries} retourne un tableau d'au moins deux
     * entr�es), alors le choix de l'image se fera en utilisant un objet {@link ImageComparator}
     * par d�faut. Ce choix peut �tre arbitraire.
     *
     * @return Une image choisie arbitrairement dans la r�gion et la plage de date
     *         s�lectionn�es, ou <code>null</code> s'il n'y a pas d'image dans ces plages.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract ImageEntry getEntry() throws SQLException;

    /**
     * Retourne l'image correspondant au num�ro ID sp�cifi�. L'argument <code>ID</code>
     * correspond au num�ro  {@link ImageEntry#getID}  d'une des images retourn�es par
     * {@link #getEntries()} ou {@link #getEntry()}.  L'image demand�e doit appartenir
     * � la s�rie acc�d�e par cette table (voir {@link #getSeries}). L'image retourn�e
     * sera d�coup�e de fa�on � n'inclure que les coordonn�es sp�cifi�es lors du dernier
     * appel de {@link #setGeographicArea}.
     *
     * @param  ID Num�ro identifiant l'image d�sir�e.
     * @return L'image demand�e, ou <code>null</code> si elle n'a pas �t� trouv�e.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract ImageEntry getEntry(final int ID) throws SQLException;

    /**
     * Retourne l'image nomm�e. L'argument <code>name</code> correspond au nom {@link ImageEntry#getName}
     * d'une des images retourn�es par {@link #getEntries} ou {@link #getEntry()}.  L'image demand�e doit
     * appartenir � la s�rie acc�d�e par cette table ({@link #getSeries}). L'image retourn�e sera d�coup�e
     * de fa�on � n'inclure que les coordonn�es sp�cifi�es lors du dernier appel de {@link #setGeographicArea}.
     *
     * @param  name Nom de l'image d�sir�e.
     * @return L'image demand�e, ou <code>null</code> si elle n'a pas �t� trouv�e.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract ImageEntry getEntry(final String name) throws SQLException;

    /**
     * Obtient les plages de temps et de coordonn�es
     * couvertes par les images de cette table.
     *
     * @param x Objet dans lequel ajouter les plages de longitudes, ou <code>null</code> pour ne pas extraire ces plages.
     * @param y Objet dans lequel ajouter les plages de latitudes,  ou <code>null</code> pour ne pas extraire ces plages.
     * @param t Objet dans lequel ajouter les plages de temps,      ou <code>null</code> pour ne pas extraire ces plages.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract void getRanges(final RangeSet x, final RangeSet y, final RangeSet t) throws SQLException;
}
