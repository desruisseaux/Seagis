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
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Divers
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Connection vers une table des formats d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class FormatTable extends Table
{
    /**
     * Requ�te SQL utilis�e par cette classe pour obtenir un format � partir d'un groupe.
     */
    static final String SQL_FOR_GROUP_ID = "SELECT format FROM "+GROUPS+" WHERE ID=?";

    /**
     * Requ�te SQL utilis�e pour obtenir le type MIME du format
     * (par exemple "image/png") dans la table des formats.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] ID         */ FORMATS+".ID, "         +
                                /*[02] NAME       */ FORMATS+".name, "       +
                                /*[03] MIME       */ FORMATS+".mime, "       +
                                /*[04] EXTENSION  */ FORMATS+".extension, "  +
                                /*[05] GEOPHYSICS */ FORMATS+".geophysics\n" +
    
                    "FROM "+FORMATS+" WHERE "+FORMATS+".ID=?";

    /** Num�ro de colonne. */ private static final int ID         = 1;
    /** Num�ro de colonne. */ private static final int NAME       = 2;
    /** Num�ro de colonne. */ private static final int MIME       = 3;
    /** Num�ro de colonne. */ private static final int EXTENSION  = 4;
    /** Num�ro de colonne. */ private static final int GEOPHYSICS = 5;
    /** Num�ro d'argument. */ private static final int ARG_ID     = 1;

    /**
     * Requ�te SQL retournant un format � partir d'un groupe.
     */
    private PreparedStatement selectByGroupID;

    /**
     * Requ�te SQL faisant le lien
     * avec la base de donn�es.
     */
    private final PreparedStatement statement;

    /**
     * Connexion vers la table des bandes. Cette connexion
     * ne sera �tablie que lorsqu'elle deviendra n�cessaire.
     */
    private transient BandTable bands;

    /**
     * Construit un objet en utilisant la connection sp�cifi�e.
     * @param connection Connection vers une base de donn�es d'images.
     * @throws SQLException si <code>FormatTable</code> n'a pas pu construire sa requ�te SQL.
     */
    protected FormatTable(final Connection connection) throws SQLException
    {statement = connection.prepareStatement(preferences.get(FORMATS, SQL_SELECT));}

    /**
     * Retourne le format correspondant � un groupe d'images.
     *
     * @param  groupID Num�ro ID de la table <code>Groups</code> de la base de donn�es.
     * @return Le format utilis� par le groupe sp�cifi�, ou <code>null</code> si aucun
     *         groupe identifi� par l'ID specifi� n'a �t� trouv�.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
     */
    public synchronized FormatEntry forGroupID(final int ID) throws SQLException
    {
        if (selectByGroupID==null)
        {
            selectByGroupID = statement.getConnection().prepareStatement(preferences.get("FORMAT_FOR_GROUP_ID", SQL_FOR_GROUP_ID));
        }
        selectByGroupID.setInt(1, ID);
        final ResultSet resultSet = selectByGroupID.executeQuery();
        FormatEntry entry = null;
        while (resultSet.next())
        {
            final int formatID = resultSet.getInt(1);
            if (entry==null)
            {
                entry = getEntry(formatID);
            }
            else if (entry.getID() != formatID)
            {
                throw new SQLException();
            }
        }
        resultSet.close();
        return entry;
    }

    /**
     * Retourne l'entr� correspondant au format identifi�
     * par le num�ro <code>formatID</code> sp�cifi�.
     *
     * @param  key Num�ro ID du format.
     * @return L'entr� correspondant au format sp�cifi�.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de
     *                      donn�es, ou si le format sp�cifi� n'a pas �t� trouv�.
     */
    public FormatEntryImpl getEntry(final int ID) throws SQLException
    {return getEntry(new Integer(ID));}

    /**
     * Retourne l'entr� correspondant au format identifi�
     * par le num�ro <code>formatID</code> sp�cifi�.
     *
     * @param  key Num�ro ID du format.
     * @return L'entr� correspondant au format sp�cifi�.
     * @throws SQLException si une erreur est survenu lors de l'acc�s � la base de
     *                      donn�es, ou si le format sp�cifi� n'a pas �t� trouv�.
     */
    final synchronized FormatEntryImpl getEntry(final Integer key) throws SQLException
    {
        statement.setInt(ARG_ID, key.intValue());
        ResultSet result=statement.executeQuery();
        if (!result.next())
        {
            result.close();
            throw new IllegalRecordException(FORMATS, Resources.format(ResourceKeys.ERROR_NO_IMAGE_FORMAT_$1, key));
        }
        final int      formatID  = result.getInt    (ID);
        final String       name  = result.getString (NAME);
        final String   mimeType  = result.getString (MIME);
        final String   extension = result.getString (EXTENSION);
        final boolean geophysics = result.getBoolean(GEOPHYSICS);
        while (result.next())
        {
            if (formatID  !=     result.getInt    (ID)         ||
              !      name.equals(result.getString (NAME))      ||
              !  mimeType.equals(result.getString (MIME))      ||
              ! extension.equals(result.getString (EXTENSION)) ||
               geophysics !=     result.getBoolean(GEOPHYSICS))
            {
                result.close();
                throw new IllegalRecordException(FORMATS, Resources.format(ResourceKeys.ERROR_TOO_MANY_IMAGE_FORMATS_$1, key));
            }
        }
        result.close();
        if (bands==null)
        {
            bands = new BandTable(statement.getConnection());
        }
        final FormatEntryImpl entry = new FormatEntryImpl(formatID, name, mimeType, extension,
                                              geophysics, bands.getSampleDimensions(formatID));
        logger.fine(Resources.format(ResourceKeys.CONSTRUCT_DECODER_$1, name));
        return entry;
    }

    /**
     * Lib�re les ressources utilis�es par cet objet.
     * Appelez cette m�thode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un probl�me est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {
        if (selectByGroupID != null)
        {
            selectByGroupID.close();
            selectByGroupID = null;
        }
        if (bands != null)
        {
            bands.close();
            bands = null;
        }
        statement.close();
        logger.fine(Resources.format(ResourceKeys.CLOSE_FORMAT_TABLE));
    }
}
