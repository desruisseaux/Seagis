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
package fr.ird.sql.fishery;

// P�ches et base de donn�es
import fr.ird.sql.Table;
import fr.ird.animat.Species;
import java.sql.SQLException;

// Ensembles
import java.util.Set;
import java.util.List;

// Coordonn�es spatio-temporelles
import java.util.Date;
import java.awt.geom.Rectangle2D;
import javax.media.jai.util.Range;


/**
 * Interface interrogeant la base de donn�es pour obtenir la liste des p�ches
 * qu'elle contient. Ces p�ches pourront �tre s�lectionn�es dans une certaine
 * r�gion g�ographique et � certaines dates.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public interface CatchTable extends Table
{
    /**
     * Retourne l'ensemble des esp�ces comprises dans la requ�te de cette table.
     *
     * @throws SQLException si un acc�s � la base de donn�es �tait n�cessaire et a �chou�.
     */
    public abstract Set<Species> getSpecies() throws SQLException;

    /**
     * Retourne les coordonn�es g�ographiques de la r�gion des captures.  Cette r�gion
     * ne sera pas plus grande que la r�gion qui a �t� sp�cifi�e lors du dernier appel
     * de la m�thode {@link #setGeographicArea}.  Elle peut toutefois �tre plus petite
     * de fa�on � n'englober que les donn�es de p�ches pr�sentes dans la base de donn�es.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract Rectangle2D getGeographicArea() throws SQLException;

    /**
     * D�finit les coordonn�es g�ographiques de la r�gion dans laquelle on veut rechercher des p�ches.
     * Les coordonn�es doivent �tre exprim�es en degr�s de longitude et de latitude selon l'ellipso�de
     * WGS&nbsp;1984. Toutes les p�ches qui interceptent cette r�gion seront prises en compte lors du
     * prochain appel de {@link #getEntries}.
     *
     * @param  geographicArea Coordonn�es g�ographiques de la r�gion, en degr�s de longitude et de latitude.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract void setGeographicArea(final Rectangle2D geographicArea) throws SQLException;

    /**
     * Retourne la plage de dates des p�ches. Cette plage de dates ne sera pas plus grande que
     * la plage de dates sp�cifi�e lors du dernier appel de la m�thode {@link #setTimeRange}.
     * Elle peut toutefois �tre plus petite de fa�on � n'englober que les donn�es de p�ches
     * pr�sentes dans la base de donn�es.
     *
     * @param  La plage de dates des donn�es de p�ches. Cette plage sera constitu�e d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract Range getTimeRange() throws SQLException;

    /**
     * D�finit la plage de dates dans laquelle on veut rechercher des donn�es de p�ches.
     * Toutes les p�ches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @param  timeRange Plage de dates dans laquelle rechercher des donn�es.
     *         Cette plage doit �tre constitu�e d'objets {@link Date}.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract void setTimeRange(final Range timeRange) throws SQLException;

    /**
     * D�finit la plage de dates dans laquelle on veut rechercher des donn�es de p�ches.
     * Toutes les p�ches qui interceptent cette plage de temps seront prises en compte
     * lors du prochain appel de {@link #getEntries}.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract void setTimeRange(final Date startTime, final Date endTime) throws SQLException;

    /**
     * Retourne la liste des captures connues dans la r�gion et dans la plage de
     * dates pr�alablement s�lectionn�es. Ces plages auront �t� sp�cifi�es � l'aide
     * des diff�rentes m�thodes <code>set...</code> de cette classe. Cette m�thode
     * ne retourne jamais <code>null</code>, mais peut retourner une liste de
     * longueur 0.
     *
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de donn�es.
     */
    public abstract List<CatchEntry> getEntries() throws SQLException;

    /**
     * D�finie une valeur r�elle pour une capture donn�es.  Cette m�thode peut �tre utilis�e
     * pour mettre � jour certaine informations relatives � la capture. La capture sp�cifi�e
     * doit exister dans la base de donn�es.
     *
     * @param capture    Capture � mettre � jour. Cette capture d�finit la ligne � mettre � jour.
     * @param columnName Nom de la colonne � mettre � jour.
     * @param value      Valeur � inscrire dans la base de donn�es � la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture sp�cifi�e n'existe pas, ou si la mise � jour
     *         de la base de donn�es a �chou�e pour une autre raison.
     */
    public abstract void setValue(final CatchEntry capture, final String columnName, final float value) throws SQLException;

    /**
     * D�finie une valeur bool�enne pour une capture donn�es. Cette m�thode peut �tre utilis�e
     * pour mettre � jour certaine informations relatives � la capture.   La capture sp�cifi�e
     * doit exister dans la base de donn�es.
     *
     * @param capture    Capture � mettre � jour. Cette capture d�finit la ligne � mettre � jour.
     * @param columnName Nom de la colonne � mettre � jour.
     * @param value      Valeur � inscrire dans la base de donn�es � la ligne de la capture
     *                   <code>capture</code>, colonne <code>columnName</code>.
     * @throws SQLException si la capture sp�cifi�e n'existe pas, ou si la mise � jour
     *         de la base de donn�es a �chou�e pour une autre raison.
     */
    public abstract void setValue(final CatchEntry capture, final String columnName, final boolean value) throws SQLException;
}
