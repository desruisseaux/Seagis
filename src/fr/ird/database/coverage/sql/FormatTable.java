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
package fr.ird.database.coverage.sql;

// Base de données
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.rmi.RemoteException;

// Seagis
import fr.ird.database.CatalogException;
import fr.ird.database.ConfigurationKey;
import fr.ird.resources.seagis.Resources;
import fr.ird.resources.seagis.ResourceKeys;
import fr.ird.database.IllegalRecordException;
import fr.ird.database.coverage.CoverageDataBase;


/**
 * Connection vers une table des formats d'images.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class FormatTable extends Table {
    /**
     * Requête SQL utilisée pour obtenir le type MIME du format
     * (par exemple "image/png") dans la table des formats.
     */
    static final ConfigurationKey SELECT = createKey(FORMATS, ResourceKeys.SQL_FORMATS, 
            "SELECT name, "       +   // [01] NAME
                   "mime, "       +   // [02] MIME
                   "extension, "  +   // [03] EXTENSION
                   "geophysics\n" +   // [04] GEOPHYSICS
            "FROM "+SCHEMA+".\""+FORMATS+"\" WHERE name=?");


    /** Numéro de colonne. */ private static final int NAME       = 1;
    /** Numéro de colonne. */ private static final int MIME       = 2;
    /** Numéro de colonne. */ private static final int EXTENSION  = 3;
    /** Numéro de colonne. */ private static final int GEOPHYSICS = 4;
    /** Numéro d'argument. */ private static final int ARG_NAME   = 1;

    /**
     * Requète SQL pour interroger la base de données.
     */
    private final PreparedStatement statement;

    /**
     * Connexion vers la table des bandes. Cette connexion
     * ne sera établie que lorsqu'elle deviendra nécessaire.
     */
    private transient SampleDimensionTable bands;

    /**
     * Construit un objet en utilisant la connection spécifiée.
     *
     * @param database The database where this table come from.
     * @param connection Connection vers une base de données d'images.
     * @throws SQLException si <code>FormatTable</code> n'a pas pu construire sa requête SQL.
     */
    protected FormatTable(final CoverageDataBase database,
                          final Connection     connection)
            throws RemoteException
    {
        super(database);
        try {
            statement = connection.prepareStatement(getProperty(SELECT));
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }
    }

    /**
     * Retourne l'entré correspondant au format identifié par le nom spécifié.
     *
     * @param  key Numéro ID du format.
     * @return L'entré correspondant au format spécifié.
     * @throws RemoteException si une erreur est survenu lors de l'accès au catalogue.
     */
    public synchronized FormatEntry getEntry(final String key)
            throws SQLException, RemoteException
    {
        statement.setString(ARG_NAME, key);
        ResultSet result = statement.executeQuery();
        if (!result.next()) {
            result.close();
            throw new IllegalRecordException(FORMATS,
                    Resources.format(ResourceKeys.ERROR_NO_IMAGE_FORMAT_$1, key));
        }
        final String       name  = result.getString (NAME);
        final String   mimeType  = result.getString (MIME);
        final String   extension = result.getString (EXTENSION);
        final boolean geophysics = result.getBoolean(GEOPHYSICS);
        while (result.next()) {
            if (!      name.equals(result.getString (NAME))      ||
                !  mimeType.equals(result.getString (MIME))      ||
                ! extension.equals(result.getString (EXTENSION)) ||
                 geophysics !=     result.getBoolean(GEOPHYSICS))
            {
                result.close();
                throw new IllegalRecordException(FORMATS,
                        Resources.format(ResourceKeys.ERROR_TOO_MANY_IMAGE_FORMATS_$1, key));
            }
        }
        result.close();
        if (bands == null) {
            bands = new SampleDimensionTable(database, statement.getConnection());
        }
        final FormatEntry entry = new FormatEntry(name, mimeType, extension,
                                      geophysics, bands.getSampleDimensions(name));
        CoverageDataBase.LOGGER.fine(Resources.format(ResourceKeys.CONSTRUCT_DECODER_$1, name));
        return entry;
    }

    /**
     * Libère les ressources utilisées par cet objet.
     * Appelez cette méthode lorsque vous n'aurez plus
     * besoin de consulter cette table.
     *
     * @throws RemoteException si un problème est survenu
     *         lors de la disposition des ressources.
     */
    public synchronized void close() throws RemoteException {
        if (bands != null) {
            bands.close();
            bands = null;
        }
        try {
            statement.close();
        } catch (SQLException cause) {
            throw new CatalogException(cause);
        }            
        CoverageDataBase.LOGGER.fine(Resources.format(ResourceKeys.CLOSE_FORMAT_TABLE));
    }
}
