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

// J2SE et JAI
import java.rmi.RemoteException;
import java.util.Set;
import java.util.Date;
import java.util.Collection;
//import java.sql.SQLException;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;

// Geotools
import org.geotools.cs.CoordinateSystem;

// Seagis
import fr.ird.database.Table;
import fr.ird.animat.Species;


/**
 * Interface interrogeant la base de donn�es pour obtenir la liste des �chantillons
 * qu'elle contient. Ces �chantillons pourront �tre s�lectionn�es dans une certaine
 * r�gion g�ographique et � certaines dates.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public interface SampleTable extends Table {
    /**
     * Retourne l'ensemble des esp�ces comprises dans la requ�te de cette table.
     *
     * @throws RemoeteException si un acc�s au catalogue �tait n�cessaire et a �chou�.
     */
    public abstract Set<Species> getSpecies() throws RemoteException;

    /**
     * Sp�cifie l'ensemble des esp�ces � prendre en compte lors des interrogations de
     * la base de donn�es. Les objets {@link SampleEntry} retourn�s par cette table ne
     * contiendront des informations que sur ces esp�ces, et la m�thode {@link SampleEntry#getValue()}
     * (qui retourne la quantit� totale d'individus observ�s ou captur�s) ignorera toute esp�ce qui
     * n'apparait pas dans l'ensemble <code>species</code>.
     *
     * @param species Ensemble des esp�ces � prendre en compte.
     * @throws RemoteException si une erreur est survenu lors de l'acc�s au catalogue.
     */
    public void setSpecies(final Set<Species> species) throws RemoteException;

    /**
     * Retourne le syst�me de coordonn�es utilis�es
     * pour les positions de p�ches dans cette table.
     *
     * @throws RemoteException si une erreur est survenu lors de l'acc�s au catalogue.
     */
    public abstract CoordinateSystem getCoordinateSystem() throws RemoteException;

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion des �chantillons. Cette r�gion
     * ne sera pas plus grande que la r�gion qui a �t� sp�cifi�e lors du dernier appel
     * de la m�thode {@link #setGeographicArea}.  Elle peut toutefois �tre plus petite
     * de fa�on � n'englober que les �chantillons pr�sents dans la base de donn�es.
     *
     * @throws RemoteException si une erreur est survenu lors de l'acc�s au catalogue.
     */
    public abstract Rectangle2D getGeographicArea() throws RemoteException;

    /**
     * D�finit les coordonn�es g�ographiques de la r�gion dans laquelle on veut rechercher des
     * �chantillons. Les coordonn�es doivent �tre exprim�es en degr�s de longitude et de latitude
     * selon l'ellipso�de WGS&nbsp;1984. Tous les �chantillons qui interceptent cette r�gion seront
     * pris en compte lors du prochain appel de {@link #getEntries}.
     *
     * @param  geographicArea Coordonn�es g�ographiques de la r�gion, en degr�s de longitude et de latitude.
     * @throws RemoteException si une erreur est survenu lors de l'acc�s au catalogue.
     */
    public abstract void setGeographicArea(final Rectangle2D geographicArea) throws RemoteException;

    /**
     * Retourne la plage de dates des �chantillons. Cette plage de dates ne sera pas plus grande que
     * la plage de dates sp�cifi�e lors du dernier appel de la m�thode {@link #setTimeRange}. Elle
     * peut toutefois �tre plus petite de fa�on � n'englober que les donn�es des �chantillons
     * pr�sentes dans la base de donn�es.
     *
     * @param  La plage de dates des donn�es de p�ches. Cette plage sera constitu�e d'objets {@link Date}.
     * @throws RemoteException si une erreur est survenu lors de l'acc�s au catalogue.
     */
    public abstract Range getTimeRange() throws RemoteException;

    /**
     * D�finit la plage de dates dans laquelle on veut rechercher des donn�es des �chantillons.
     * Tous les �chantillons qui interceptent cette plage de temps seront pris en compte lors
     * du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des donn�es.
     *         Cette plage doit �tre constitu�e d'objets {@link Date}.
     * @throws RemoteException si une erreur est survenu lors de l'acc�s au catalogue.
     */
    public abstract void setTimeRange(final Range timeRange) throws RemoteException;

    /**
     * D�finit la plage de dates dans laquelle on veut rechercher des donn�es des �chantillons.
     * Tous les �chantillons qui interceptent cette plage de temps seront pris en compte lors
     * du prochain appel de {@link #getEntries}.
     *
     * @param  startTime Date de d�but (inclusive) de la p�riode d'int�r�t.
     * @param  startTime Date de fin   (inclusive) de la p�riode d'int�r�t.
     * @throws RemoteException si une erreur est survenu lors de l'acc�s au catalogue.
     */
    public abstract void setTimeRange(final Date startTime, final Date endTime) throws RemoteException;

    /**
     * Retourne la plage de valeurs des �chantillons d'int�r�t.
     * Il peut s'agit par exemple de la quantit� de poissons p�ch�s
     * en tonnes ou en nombre d'individus.
     */
    public abstract Range getValueRange() throws RemoteException;

    /**
     * D�finit la plage de valeurs d'�chantillons d'int�r�t. Seules les �chantillons dont la valeur
     * (le tonnage ou le nombre d'individus, d�pendament du type de p�che) est compris dans cette
     * plage seront retenus.
     */
    public abstract void setValueRange(final Range valueRange) throws RemoteException;

    /**
     * D�finit la plage de valeurs des �chantillons d'int�r�t. Cette m�thode est �quivalente
     * � {@link #setValueRange(Range)}.
     *
     * @param minimum Valeur minimale, inclusif.
     * @param maximum Valeur maximale, inclusif.
     */
    public abstract void setValueRange(double minimum, final double maximum) throws RemoteException;

    /**
     * Retourne la liste des �chantillons connus dans la r�gion et dans la plage de
     * dates pr�alablement s�lectionn�es. Ces plages auront �t� sp�cifi�es � l'aide
     * des diff�rentes m�thodes <code>set...</code> de cette classe. Cette m�thode
     * ne retourne jamais <code>null</code>, mais peut retourner un ensemble vide.
     *
     * @throws RemoteException si une erreur est survenu lors de l'acc�s au catalogue.
     */
    public abstract Collection<SampleEntry> getEntries() throws RemoteException;

    /**
     * D�finie une valeur r�elle pour un �chantillon sp�cifi�. Cette m�thode peut �tre utilis�e
     * pour mettre � jour certaine informations relatives � l'�chantillon. L'�chantillon sp�cifi�
     * doit exister dans la base de donn�es.
     *
     * @param sample     Echantillon � mettre � jour. Cetargument d�finit la ligne � mettre � jour.
     * @param columnName Nom de la colonne � mettre � jour.
     * @param value      Valeur � inscrire dans la base de donn�es � la ligne de l'�chantillon
     *                   <code>sample</code>, colonne <code>columnName</code>.
     * @throws RemoteException si l'�chantillon sp�cifi� n'existe pas, ou si la mise � jour
     *                   du catalogue a �chou� pour une autre raison.
     */
    public abstract void setValue(final SampleEntry sample, final String columnName, final float value) throws RemoteException;

    /**
     * D�finie une valeur bool�enne pour un �chantillon sp�cifi�. Cette m�thode peut �tre utilis�e
     * pour mettre � jour certaine informations relatives � l'�chantillon. L'�chantillon sp�cifi�
     * doit exister dans la base de donn�es.
     *
     * @param sample     Echantillon � mettre � jour. Cetargument d�finit la ligne � mettre � jour.
     * @param columnName Nom de la colonne � mettre � jour.
     * @param value      Valeur � inscrire dans la base de donn�es � la ligne de l'�chantillon
     *                   <code>sample</code>, colonne <code>columnName</code>.
     * @throws RemoteException si l'�chantillon sp�cifi� n'existe pas, ou si la mise � jour
     *                   du catalogue a �chou� pour une autre raison.
     */
    public abstract void setValue(final SampleEntry sample, final String columnName, final boolean value) throws RemoteException;
}
