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

// J2SE and JAI dependencies
import java.util.Date;
import java.text.DateFormat;
import java.io.Serializable;
import java.io.ObjectStreamException;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import javax.media.jai.ParameterList;

// Geotools dependencies
import org.geotools.gp.Operation;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.HorizontalCoordinateSystem;
import org.geotools.resources.Utilities;

// Seagis dependencies
import fr.ird.database.coverage.SeriesEntry;


/**
 * Bloc de paramètres pour une table {@link GridCoverageTable}. Les blocs de paramètres doivent
 * être imutables.  Ce principe d'imutabilité s'applique aussi aux objets référencés par
 * les champs publiques, même si ces objets sont en principe mutables ({@link Rectangle2D},
 * {@link Dimension2D}...).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Parameters implements Serializable {
    /**
     * Numéro de série (pour compatibilité avec des versions antérieures).
     */
    private static final long serialVersionUID = 6418640591318515042L;

    /**
     * Réference vers la série d'images. Cette référence
     * est construite à partir du champ ID dans la table
     * "Series" de la base de données.
     */
    public final SeriesEntry series;

    /**
     * L'opération à appliquer sur les images lue,
     * ou <code>null</code> s'il n'y en a aucune.
     */
    public final Operation operation;

    /**
     * Paramètres à appliquer sur l'opération, ou
     * <code>null</code> s'il n'y a pas d'opération.
     */
    public final ParameterList parameters;

    /**
     * Format à utiliser pour lire les images.
     */
    public final FormatEntry format;

    /**
     * Chemin relatif des images.
     */
    public final String pathname;

    /**
     * Système de coordonnées de la table. Le système de coordonnées de tête ("head")
     * doit obligatoirement être un objet {@link HorizontalCoordinateSystem}.  La
     * seconde partie ("tail") sera ignorée;   il s'agira typiquement de l'axe du
     * temps ou de la profondeur.
     */
    public final CoordinateSystem tableCS;

    /**
     * Système de coordonnées de l'image. Ce sera habituellement (mais pas obligatoirement)
     * le même que {@link #tableCS}.
     */
    public final CoordinateSystem imageCS;

    /**
     * Coordonnées horizontales de la région d'intéret.  Ces coordonnées
     * sont exprimées selon la partie horizontale ("head") du système de
     * coordonnées {@link #tableCS}.
     */
    public final Rectangle2D geographicArea;

    /**
     * Dimension logique désirée des pixels de l'images.   Cette information
     * n'est qu'approximative. Il n'est pas garantie que la lecture produira
     * effectivement une image de cette résolution. Une valeur nulle signifie
     * que la lecture doit se faire avec la meilleure résolution possible.
     */
    public final Dimension2D resolution;

    /**
     * Formatteur à utiliser pour écrire des dates pour l'utilisateur. Les caractères et
     * les conventions linguistiques dépendront de la langue de l'utilisateur. Toutefois,
     * le fuseau horaire devrait être celui de la région d'étude plutôt que celui du pays
     * de l'utilisateur.
     */
    private final DateFormat dateFormat;

    /**
     * Construit un bloc de paramètres.
     *
     * @param series Référence vers la série d'images.
     * @param format Format à utiliser pour lire les images.
     * @param pathname Chemin relatif des images.
     * @param operation Opération à appliquer sur les images, ou <code>null</code>.
     * @param parameters Paramètres à appliquer sur l'opération, ou <code>null</code>
     *        s'il n'y a pas d'opération.
     * @param tableCS Système de coordonnées de la table. Le système de
     *        coordonnées de tête ("head") doit obligatoirement être un
     *        objet {@link HorizontalCoordinateSystem}.
     * @param imageCS Système de coordonnées de l'image. Ce sera habituellement
     *        (mais pas obligatoirement) le même que {@link #tableCS}.
     * @param geographicArea Coordonnées horizontales de la région d'intéret,
     *        dans le système de coordonnées <code>tableCS</code>.
     * @param resolution Dimension logique approximative désirée des pixels,
     *        ou <code>null</code> pour la meilleure résolution disponible.
     *        Doit être exprimé dans le système de coordonnées <code>tableCS</code>.
     * @param dateFormat Formatteur à utiliser pour écrire des dates pour l'utilisateur.
     */
    public Parameters(final SeriesEntry      series,
                      final FormatEntry      format,
                      final String           pathname,
                      final Operation        operation,
                      final ParameterList    parameters,
                      final CoordinateSystem tableCS,
                      final CoordinateSystem imageCS,
                      final Rectangle2D      geographicArea,
                      final Dimension2D      resolution,
                      final DateFormat       dateFormat)
    {
        this.series         = series;
        this.format         = format;
        this.pathname       = pathname;
        this.operation      = operation;
        this.parameters     = parameters;
        this.tableCS        = tableCS;
        this.imageCS        = imageCS;
        this.geographicArea = geographicArea;
        this.resolution     = resolution;
        this.dateFormat     = dateFormat;
    }

    /**
     * Indique si ce bloc de paramètres est identique au bloc spécifié.
     */
    public boolean equals(final Object o) {
        if (o instanceof Parameters) {
            final Parameters that = (Parameters) o;
            return Utilities.equals(this.series         , that.series          ) &&
                   Utilities.equals(this.format         , that.format          ) &&
                   Utilities.equals(this.pathname       , that.pathname        ) &&
                   Utilities.equals(this.operation      , that.operation       ) &&
                   Utilities.equals(this.parameters     , that.parameters      ) &&
                   Utilities.equals(this.tableCS        , that.tableCS         ) &&
                   Utilities.equals(this.imageCS        , that.imageCS         ) &&
                   Utilities.equals(this.geographicArea , that.geographicArea  ) &&
                   Utilities.equals(this.resolution     , that.resolution      ) &&
                   Utilities.equals(this.dateFormat     , that.dateFormat      );
        }
        return false;
    }

    /**
     * Formate la date spécifiée.
     */
    public String format(final Date date) {
        synchronized (dateFormat) {
            return dateFormat.format(date);
        }
    }

    /**
     * Retourne un code représentant ce bloc de paramètres.
     */
    public int hashCode() {
        int code = (int)serialVersionUID;
        if (geographicArea != null) code += geographicArea.hashCode();
        if (resolution     != null) code +=     resolution.hashCode();
        return code;
    }

    /**
     * Après la lecture binaire, vérifie si
     * l'objet lu existait déjà en mémoire.
     */
    private Object readResolve() throws ObjectStreamException {
        return Table.POOL.canonicalize(this);
    }
}
