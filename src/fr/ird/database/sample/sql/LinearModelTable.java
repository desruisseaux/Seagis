/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le D�veloppement
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
package fr.ird.database.sample.sql;

// J2SE
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;


/**
 * Connexion vers la table des mod�les lin�aires.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class LinearModelTable extends Table {
    /**
     * La requ�te SQL servant � interroger la table.
     */
    static final String SQL_SELECT = "SELECT source1, source2, coefficient " +
                                     "FROM [" + LINEAR_MODELS + "] WHERE cible=?";

    /** Num�ro de colonne. */ private static final int SOURCE1     = 1;
    /** Num�ro de colonne. */ private static final int SOURCE2     = 2;
    /** Num�ro de colonne. */ private static final int COEFFICIENT = 3;
    /** Num�ro d'argument. */ private static final int TARGET_ARG  = 1;

    /**
     * La table des descripteurs du paysage oc�anique.
     */
    private DescriptorTable descriptors;

    /**
     * Construit une nouvelle connexion vers la table des mod�les lin�aires.
     *
     * @param  descriptors La table des descripteurs du paysage oc�anique.
     * @throws SQLException si la construction de cette table a �chou�e.
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
     * Retourne les termes de mod�le lin�aire pour le param�tre sp�cifi�. Si ce param�tre n'est
     * pas le r�sultat d'un mod�le lin�aire, alors cette m�thode retourne <code>null</code>.
     *
     * @param  target Le param�tre pour lequel on veut le mod�le lin�aire.
     * @return Les termes du mod�le lin�aire, ou <code>null</code> s'il n'y en a pas.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�e.
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
             * maintenant. Ca ne devrait �tre fait qu'� la fin de la bouche (une fois la
             * requ�te termin�e), afin d'�viter des appels r�cursifs qui cr�ent plusieurs
             * ResultSet pour le m�me Statement.
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
         * Maintenant que toutes les requ�tes sont ferm�es, compl�te les ParameterEntry
         * dont on n'avait lu que les num�ros ID.
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
