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
package fr.ird.image.sql;

// Base de données
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

// Divers
import fr.ird.resources.Resources;


/**
 * Connection vers une table des formats d'images.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class FormatTable extends Table
{
    /**
     * Requête SQL utilisée pour obtenir le type MIME du format
     * (par exemple "image/png") dans la table des formats.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] ID         */ FORMATS+".ID, "         +
                                /*[02] NAME       */ FORMATS+".name, "       +
                                /*[03] MIME       */ FORMATS+".mime, "       +
                                /*[04] EXTENSION  */ FORMATS+".extension, "  +
                                /*[05] GEOPHYSICS */ FORMATS+".geophysics\n" +
    
                    "FROM "+FORMATS+" WHERE "+FORMATS+".ID=?";

    /** Numéro de colonne. */ private static final int ID         = 1;
    /** Numéro de colonne. */ private static final int NAME       = 2;
    /** Numéro de colonne. */ private static final int MIME       = 3;
    /** Numéro de colonne. */ private static final int EXTENSION  = 4;
    /** Numéro de colonne. */ private static final int GEOPHYSICS = 5;
    /** Numéro d'argument. */ private static final int ARG_ID     = 1;

    /**
     * Requète SQL faisant le lien
     * avec la base de données.
     */
    private final PreparedStatement statement;

    /**
     * Connexion vers la table des bandes. Cette connexion
     * ne sera établie que lorsqu'elle deviendra nécessaire.
     */
    private transient BandTable bands;

    /**
     * Construit un objet en utilisant la connection spécifiée.
     * @param connection Connection vers une base de données d'images.
     * @throws SQLException si <code>FormatTable</code> n'a pas pu construire sa requête SQL.
     */
    protected FormatTable(final Connection connection) throws SQLException
    {statement = connection.prepareStatement(preferences.get(FORMATS, SQL_SELECT));}

    /**
     * Retourne l'entré correspondant au format identifié
     * par le numéro <code>formatID</code> spécifié.
     *
     * @param  key Numéro ID du format.
     * @return L'entré correspondant au format spécifié.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de
     *                      données, ou si le format spécifié n'a pas été trouvé.
     */
    public FormatEntry getEntry(final int ID) throws SQLException
    {return getEntry(new Integer(ID));}

    /**
     * Retourne l'entré correspondant au format identifié
     * par le numéro <code>formatID</code> spécifié.
     *
     * @param  key Numéro ID du format.
     * @return L'entré correspondant au format spécifié.
     * @throws SQLException si une erreur est survenu lors de l'accès à la base de
     *                      données, ou si le format spécifié n'a pas été trouvé.
     */
    final synchronized FormatEntry getEntry(final Integer key) throws SQLException
    {
        statement.setInt(ARG_ID, key.intValue());
        ResultSet result=statement.executeQuery();
        if (!result.next())
        {
            result.close();
            throw new IllegalRecordException(FORMATS, Resources.format(Clé.NO_IMAGE_FORMAT¤1, key));
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
                throw new IllegalRecordException(FORMATS, Resources.format(Clé.TOO_MANY_IMAGE_FORMATS¤1, key));
            }
        }
        result.close();
        if (bands==null)
        {
            bands = new BandTable(statement.getConnection());
        }
        final FormatEntry entry = new FormatEntry(formatID, name, mimeType, extension, geophysics, bands.getCategoryList(formatID));
        logger.fine(Resources.format(Clé.CONSTRUCTED_DECODER¤1, name));
        return entry;
    }

    /**
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws SQLException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws SQLException
    {
        if (bands!=null)
        {
            bands.close();
            bands=null;
        }
        statement.close();
        logger.fine(Resources.format(Clé.CLOSE_FORMAT_TABLE));
    }
}
