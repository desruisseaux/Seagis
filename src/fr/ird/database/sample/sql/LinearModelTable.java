/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.database.sample.sql;

// J2SE
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;


/**
 * Connexion vers la table des modèles linéaires.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class LinearModelTable extends Table {
    /**
     * La requête SQL servant à interroger la table.
     */
    static final String SQL_SELECT = "SELECT source1, source2, coefficient " +
                                     "FROM [" + LINEAR_MODELS + "] WHERE cible=?";

    /** Numéro de colonne. */ private static final int SOURCE1     = 1;
    /** Numéro de colonne. */ private static final int SOURCE2     = 2;
    /** Numéro de colonne. */ private static final int COEFFICIENT = 3;
    /** Numéro d'argument. */ private static final int TARGET_ARG  = 1;

    /**
     * La table des descripteurs du paysage océanique.
     */
    private DescriptorTable descriptors;

    /**
     * Construit une nouvelle connexion vers la table des modèles linéaires.
     *
     * @param  descriptors La table des descripteurs du paysage océanique.
     * @throws SQLException si la construction de cette table a échouée.
     */
    protected LinearModelTable(final DescriptorTable descriptors) throws SQLException {
        super(descriptors.getConnection().prepareStatement(preferences.get(LINEAR_MODELS, SQL_SELECT)));
        this.descriptors = descriptors;
    }

    /**
     * Retourne la table des descripteurs.
     */
    public DescriptorTable getDescriptorTable() {
        return descriptors;
    }

    /**
     * Retourne les termes de modèle linéaire pour le paramètre spécifié. Si ce paramètre n'est
     * pas le résultat d'un modèle linéaire, alors cette méthode retourne <code>null</code>.
     *
     * @param  target Le paramètre pour lequel on veut le modèle linéaire.
     * @return Les termes du modèle linéaire, ou <code>null</code> s'il n'y en a pas.
     * @throws SQLException si l'interrogation de la base de données a échouée.
     */
    public synchronized List<LinearModelTerm> getTerms(final ParameterEntry target) throws SQLException {
        ArrayList<LinearModelTerm> terms = null;
        statement.setInt(TARGET_ARG, target.getID());
        final ResultSet results = statement.executeQuery();
        while (results.next()) {
            final String source1     = results.getString(SOURCE1);
            final String source2     = results.getString(SOURCE2);
            final double coefficient = results.getDouble(COEFFICIENT);
            /*
             * Construit les descripteurs, mais sans interroger la table 'ParameterTable'
             * maintenant. Ca ne devrait être fait qu'à la fin de la bouche (une fois la
             * requête terminée), afin d'éviter des appels récursifs qui créent plusieurs
             * ResultSet pour le même Statement.
             */
            final DescriptorEntry descriptor1 = descriptors.getEntry(source1);
            final DescriptorEntry descriptor2 = descriptors.getEntry(source2);
            final DescriptorEntry[] term;
            if (descriptor1.isIdentity()) {
                term = new DescriptorEntry[] {descriptor2};
            } else if (descriptor2.isIdentity()) {
                term = new DescriptorEntry[] {descriptor1};
            } else {
                term = new DescriptorEntry[] {descriptor1, descriptor2};
            }
            if (terms == null) {
                terms = new ArrayList<LinearModelTerm>();
            }
            terms.add(new LinearModelTerm(target, term, coefficient));
        }
        results.close();
        /*
         * Maintenant que toutes les requêtes sont fermées, complète les ParameterEntry
         * dont on n'avait lu que les numéros ID.
         */
        if (terms != null) {
            terms.trimToSize();
            final ParameterTable parameters = descriptors.getParameterTable(ParameterTable.BY_ID);
            for (final LinearModelTerm term : terms) {
                for (final DescriptorEntry descriptor : term.descriptors) {
                    parameters.completeEntry(descriptor.parameter);
                }
            }
        }
        return terms;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws SQLException {
        if (descriptors != null) {
            descriptors.close();
            descriptors = null;
        }
        super.close();
    }
}
