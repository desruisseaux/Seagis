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

// Base de donn�es
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.ResultSetMetaData;
import fr.ird.sql.SQLEditor;
import fr.ird.sql.DataBase;

// Temps
import java.util.TimeZone;

// Collections
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

// Divers
import fr.ird.animat.Species;
import fr.ird.resources.gui.Resources;
import fr.ird.resources.gui.ResourceKeys;


/**
 * Connection avec la base de donn�es de p�ches.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class FisheryDataBase extends DataBase
{
    /** TODO: Temporary switch */
    private static final boolean SEINES = true;

    /**
     * Retourne une des pr�f�rences du syst�me.  Cette m�thode d�finit les
     * param�tres par d�faut qui seront utilis�s lorsque l'utilisateur n'a
     * pas d�fini de pr�f�rence pour un param�tre.
     */
    private static String getPreference(final String name)
    {
        String def=null;
        if (name!=null)
        {
                 if (name.equalsIgnoreCase(DRIVER))   def = "sun.jdbc.odbc.JdbcOdbcDriver";
            else if (name.equalsIgnoreCase(SOURCE))   def = "jdbc:odbc:SEAS-P�ches";
            else if (name.equalsIgnoreCase(TIMEZONE)) def = "Indian/Reunion";
        }
        return Table.preferences.get(name, def);
    }

    /**
     * Liste des propri�t�es par d�faut. Les valeurs aux index pairs sont les index
     * des propri�t�es. Les valeurs aux index impairs sont les valeurs. Par exemple
     * la propri�t� "P�ches" donne l'instruction SQL � utiliser pour interroger la
     * table des p�ches.
     */
    private static final String[] DEFAULT_PROPERTIES=
    {
        Table.SPECIES,                SpeciesTable        .SQL_SELECT,
        Table.LONGLINES,              LonglineCatchTable  .SQL_SELECT,
        Table.SEINES,                 SeineCatchTable     .SQL_SELECT,
        Table.ENVIRONMENTS,           EnvironmentTableImpl.SQL_SELECT,
        Table.ENVIRONMENTS+".UPDATE", EnvironmentTableImpl.SQL_UPDATE,
        Table.ENVIRONMENTS+".INSERT", EnvironmentTableImpl.SQL_INSERT
    };

    /**
     * Liste des noms descriptifs � donner aux propri�t�s.
     * Ces noms sont identifi�s par des cl�s de ressources.
     * Ces cl�s doivent appara�trent dans le m�me ordre que
     * les �l�ments du tableau {@link #DEFAULT_PROPERTIES}.
     */
    private static final int[] PROPERTY_NAMES=
    {
        ResourceKeys.SQL_SPECIES,
        ResourceKeys.SQL_LONGLINES,
        ResourceKeys.SQL_SEINES,
        ResourceKeys.SQL_ENVIRONMENTS,
        ResourceKeys.SQL_ENVIRONMENTS_UPDATE,
        ResourceKeys.SQL_ENVIRONMENTS_INSERT
    };

    /**
     * Retourne l'URL par d�faut de la base de donn�es de p�ches.
     * Cet URL sera puis� dans les pr�f�rences de l'utilisateur
     * autant que possible.
     */
    private static String getDefaultURL()
    {
        Table.logger.log(loadDriver(getPreference(DRIVER)));
        if (SEINES) return "jdbc:odbc:SEAS-Sennes"; // TODO: temporary patch
        return getPreference(SOURCE);
    }

    /**
     * La table des captures � utiliser.
     */
    private final String catchTable = SEINES ? Table.SEINES : Table.LONGLINES;

    /**
     * Ouvre une connection avec une base de donn�es par d�faut.
     * Ce constructeur est surtout utilis� � des fins de test.
     *
     * @throws SQLException Si on n'a pas pu se connecter
     *         � la base de donn�es.
     */
    public FisheryDataBase() throws SQLException
    {super(getDefaultURL(), TimeZone.getTimeZone(getPreference(TIMEZONE)));}

    /**
     * Ouvre une connection avec la base de donn�es de p�ches.
     *
     * @param  name Protocole et nom de la base de donn�es des p�ches.
     * @param  timezone Fuseau horaire des dates inscrites dans la base
     *         de donn�es. Cette information est utilis�e pour convertir
     *         en heure GMT les dates �crites dans la base de donn�es.
     * @throws SQLException Si on n'a pas pu se connecter � la base de donn�es.
     */
    public FisheryDataBase(final String name, final TimeZone timezone) throws SQLException
    {super(name, timezone);}

    /**
     * Retourne les esp�ces �num�r�s dans la base de donn�es.
     *
     * @return Ensemble des esp�ces r�pertori�es dans la base de donn�es.
     * @throws SQLException si l'interrogation de la base de donn�es a �chou�.
     */
    public Set<Species> getSpecies() throws SQLException
    {
        final SpeciesTable   spTable = new SpeciesTable(connection);
        final Statement    statement = connection.createStatement();
        final ResultSet       result = statement.executeQuery("SELECT * FROM "+catchTable);
        final Set<Species>   species = spTable.getSpecies(result.getMetaData());
        result   .close();
        statement.close();
        spTable  .close();
        return species;
    }

    /**
     * Construit et retourne un objet qui interrogera la table des p�ches de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @param  species Esp�ces d'int�r�t dans la table.
     * @return La table des captures pour les esp�ces demand�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public CatchTable getCatchTable(final Collection<Species> species) throws SQLException
    {
        final Set<Species> speciesSet = (species instanceof Set<Species>) ?
              (Set<Species>) species : new LinkedHashSet<Species>(species);
        if (SEINES)
        {
            return new SeineCatchTable(connection, timezone, speciesSet);
        }
        else
        {
            return new LonglineCatchTable(connection, timezone, speciesSet);
        }
    }

    /**
     * Construit et retourne un objet qui interrogera la table des p�ches de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @param  species Code des esp�ces d'int�r�t dans la table (par exemple "SWO").
     * @return La table des captures pour les esp�ces demand�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public CatchTable getCatchTable(final String[] species) throws SQLException
    {
        final List<Species> list = new ArrayList<Species>(species.length);
        final SpeciesTable spSQL = new SpeciesTable(connection);
        for (int i=0; i<species.length; i++)
        {
            final Species sp = spSQL.getSpecies(species[i], i);
            if (sp!=null) list.add(sp);
        }
        spSQL.close();
        return getCatchTable(list);
    }

    /**
     * Construit et retourne un objet qui interrogera la table des p�ches de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @param  species Esp�ces d'int�r�t dans la table (par exemple "SWO").
     * @return La table des captures pour l'esp�ce demand�e.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public CatchTable getCatchTable(final String species) throws SQLException
    {return getCatchTable(new String[]{species});}

    /**
     * Construit et retourne un objet qui interrogera la table des p�ches de la base de donn�es.
     * Lorsque cette table ne sera plus n�cessaire, il faudra appeler {@link CatchTable#close}.
     *
     * @return La table des captures pour toute les esp�ces r�pertori�es.
     * @throws SQLException si la table n'a pas pu �tre construite.
     */
    public CatchTable getCatchTable() throws SQLException
    {return getCatchTable(getSpecies());}

    /**
     * Construit et retourne un objet qui interrogera un param�tre
     * environmental de la base de donn�es. Lorsque cette table ne
     * sera plus n�cessaire, il faudra appeler {@link EnvironmentTable#close}.
     *
     * @param parameter Param�tre (exemple "SST" ou "EKP"). La liste des param�tres
     *        disponibles peut �tre obtenu avec {@link #getAvailableParameters()}.
     * @param operation Op�ration (exemple "valeur" ou "sobel").
     */
    public EnvironmentTable getEnvironmentTable(final String parameter, final String operation) throws SQLException
    {return new EnvironmentTableImpl(connection, parameter, operation);}

    /**
     * Retourne la liste des param�tres disponibles. Ces param�tres peuvent
     * �tre sp�cifi� en argument � la m�thode {@link #getEnvironmentTable}.
     *
     * @throws SQLException si l'acc�s � la base de donn�es a �chou�e.
     */
    public String[] getAvailableParameters() throws SQLException {
        return EnvironmentTableImpl.getAvailableParameters(connection);
    }

    /**
     * Construit et retourne un panneau qui permet � l'utilisateur de modifier
     * les instructions SQL. Les instructions modifi�es seront utilis�es pour
     * interroger les tables de la base de donn�es de p�ches.
     */
    public static SQLEditor getSQLEditor()
    {
        assert(2*PROPERTY_NAMES.length == DEFAULT_PROPERTIES.length);
        final Resources resources = Resources.getResources(null);
        final SQLEditor editor=new SQLEditor(Table.preferences, resources.getString(ResourceKeys.EDIT_SQL_IMAGES_OR_FISHERIES_$1, new Integer(1)), Table.logger)
        {
            public String getProperty(final String name)
            {return getPreference(name);}
        };
        for (int i=0; i<PROPERTY_NAMES.length; i++)
        {
            editor.addSQL(resources.getString(PROPERTY_NAMES[i]), DEFAULT_PROPERTIES[(i<<1)+1], DEFAULT_PROPERTIES[i<<1]);
        }
        return editor;
    }
}
