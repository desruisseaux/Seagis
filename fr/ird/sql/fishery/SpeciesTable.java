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
package fr.ird.sql.fishery;

// Base de données
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;

// Ensembles
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

// Divers
import java.awt.Color;
import java.util.Locale;
import fr.ird.animat.Species;
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Table des espèces.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class SpeciesTable extends Table {
    /**
     * Requête SQL utilisée par cette classe pour obtenir la table des espèces.
     * L'ordre des colonnes est essentiel. Ces colonnes sont référencées par
     * les constantes [@link #FRENCH}, [@link #LATIN} et compagnie.
     */
    static final String SQL_SELECT=
                    "SELECT "+  /*[01] ID              */ "ID, "        +
                                /*[02] DATE            */ "anglais, "   +
                                /*[03] START_LONGITUDE */ "français, "  +
                                /*[04] START_LATITUDE  */ "latin\n"     +

                    "FROM "+SPECIES+" WHERE ID=?";

    /** Numéro d'argument. */ private static final int ID_ARG =  1;

    /**
     * Liste des langues dans lesquelles sont exprimées les noms des espèces.
     * Ces langues doivent apparaître dans le même ordre que les colonnes de
     * la requête SQL.
     */
    private static final Locale[] locales=new Locale[] {
        null,                // Code FAO
        Locale.ENGLISH,      // Anglais
        Locale.FRENCH,       // Français
        Species.LATIN        // Latin
    };

    /**
     * Couleurs par défaut à utiliser comme marques devant les pêches.
     */
    private static final Color[] COLORS=new Color[] {
        Color.blue,
        Color.red,
        Color.orange,
        Color.yellow,
        Color.pink,
        Color.magenta,
        Color.green,
        Color.cyan,
        Color.lightGray,
        Color.gray
    };

    /**
     * Construit une table des espèces.
     *
     * @param  connection Connection avec la base de données.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public SpeciesTable(final Connection connection) throws SQLException {
        super(connection.prepareStatement(preferences.get(SPECIES, SQL_SELECT)));
    }

    /**
     * Retourne l'espèce correspondant au code spécifié.
     *
     * @param code Code de l'espèce.
     * @return L'espèce demandée.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    final synchronized Species getSpecies(final String code, int index) throws SQLException {
        Species species=null;
        statement.setString(ID_ARG, code);
        final ResultSet result=statement.executeQuery();
        while (result.next()) {
            final Species lastSpecies = species;
            final String[] names = new String[locales.length];
            for (int i=0; i<names.length; i++) {
                names[i] = result.getString(i+1);
            }
            index %= COLORS.length;
            species = new FishSpecies(locales, names, COLORS[index]);
            if (lastSpecies!=null && !lastSpecies.equals(species)) {
                throw new SQLException(Resources.format(ResourceKeys.ERROR_DUPLICATED_RECORD_$1,
                                                        species.getName()));
            }
        }
        result.close();
        return species;
    }

    /**
     * Retourne les espèces présente dans la requête spécifiée.    Cette méthode suppose
     * que les noms de colonnes de la requête correspondent aux codes de la FAO déclarés
     * dans le champ ID de la table des espèces.
     *
     * @param info Information sur une requête {@link ResultSet}.
     * @return Les espèces trouvées dans la requête. Les colonnes
     *         non-reconnues seront ignorés.
     * @throws SQLException si l'accès à la base de données a échouée.
     */
    public synchronized Set<Species> getSpecies(final ResultSetMetaData info) throws SQLException {
        final int count=info.getColumnCount();
        final List<Species> species=new ArrayList<Species>(count);
        for (int j=1; j<=count; j++) {
            statement.setString(ID_ARG, info.getColumnName(j));
            final ResultSet result=statement.executeQuery();
            while (result.next()) {
                final String[] names = new String[locales.length];
                for (int i=0; i<names.length; i++) {
                    names[i] = result.getString(i+1);
                }
                final Species sp = new FishSpecies(locales, names, COLORS[species.size() % COLORS.length]);
                species.add(sp);
            }
            result.close();
        }
        return new SpeciesSet(species);
    }
}
