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
 * Bloc de param�tres pour une table {@link GridCoverageTable}. Les blocs de param�tres doivent
 * �tre imutables.  Ce principe d'imutabilit� s'applique aussi aux objets r�f�renc�s par
 * les champs publiques, m�me si ces objets sont en principe mutables ({@link Rectangle2D},
 * {@link Dimension2D}...).
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class Parameters implements Serializable {
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 6418640591318515042L;

    /**
     * R�ference vers la s�rie d'images. Cette r�f�rence
     * est construite � partir du champ ID dans la table
     * "Series" de la base de donn�es.
     */
    public final SeriesEntry series;

    /**
     * L'op�ration � appliquer sur les images lue,
     * ou <code>null</code> s'il n'y en a aucune.
     */
    public final Operation operation;

    /**
     * Param�tres � appliquer sur l'op�ration, ou
     * <code>null</code> s'il n'y a pas d'op�ration.
     */
    public final ParameterList parameters;

    /**
     * Format � utiliser pour lire les images.
     */
    public final FormatEntry format;

    /**
     * Chemin relatif des images.
     */
    public final String pathname;

    /**
     * Syst�me de coordonn�es de la table. Le syst�me de coordonn�es de t�te ("head")
     * doit obligatoirement �tre un objet {@link HorizontalCoordinateSystem}.  La
     * seconde partie ("tail") sera ignor�e;   il s'agira typiquement de l'axe du
     * temps ou de la profondeur.
     */
    public final CoordinateSystem tableCS;

    /**
     * Syst�me de coordonn�es de l'image. Ce sera habituellement (mais pas obligatoirement)
     * le m�me que {@link #tableCS}.
     */
    public final CoordinateSystem imageCS;

    /**
     * Coordonn�es horizontales de la r�gion d'int�ret.  Ces coordonn�es
     * sont exprim�es selon la partie horizontale ("head") du syst�me de
     * coordonn�es {@link #tableCS}.
     */
    public final Rectangle2D geographicArea;

    /**
     * Dimension logique d�sir�e des pixels de l'images.   Cette information
     * n'est qu'approximative. Il n'est pas garantie que la lecture produira
     * effectivement une image de cette r�solution. Une valeur nulle signifie
     * que la lecture doit se faire avec la meilleure r�solution possible.
     */
    public final Dimension2D resolution;

    /**
     * Formatteur � utiliser pour �crire des dates pour l'utilisateur. Les caract�res et
     * les conventions linguistiques d�pendront de la langue de l'utilisateur. Toutefois,
     * le fuseau horaire devrait �tre celui de la r�gion d'�tude plut�t que celui du pays
     * de l'utilisateur.
     */
    private final DateFormat dateFormat;

    /**
     * Construit un bloc de param�tres.
     *
     * @param series R�f�rence vers la s�rie d'images.
     * @param format Format � utiliser pour lire les images.
     * @param pathname Chemin relatif des images.
     * @param operation Op�ration � appliquer sur les images, ou <code>null</code>.
     * @param parameters Param�tres � appliquer sur l'op�ration, ou <code>null</code>
     *        s'il n'y a pas d'op�ration.
     * @param tableCS Syst�me de coordonn�es de la table. Le syst�me de
     *        coordonn�es de t�te ("head") doit obligatoirement �tre un
     *        objet {@link HorizontalCoordinateSystem}.
     * @param imageCS Syst�me de coordonn�es de l'image. Ce sera habituellement
     *        (mais pas obligatoirement) le m�me que {@link #tableCS}.
     * @param geographicArea Coordonn�es horizontales de la r�gion d'int�ret,
     *        dans le syst�me de coordonn�es <code>tableCS</code>.
     * @param resolution Dimension logique approximative d�sir�e des pixels,
     *        ou <code>null</code> pour la meilleure r�solution disponible.
     *        Doit �tre exprim� dans le syst�me de coordonn�es <code>tableCS</code>.
     * @param dateFormat Formatteur � utiliser pour �crire des dates pour l'utilisateur.
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
     * Indique si ce bloc de param�tres est identique au bloc sp�cifi�.
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
     * Formate la date sp�cifi�e.
     */
    public String format(final Date date) {
        synchronized (dateFormat) {
            return dateFormat.format(date);
        }
    }

    /**
     * Retourne un code repr�sentant ce bloc de param�tres.
     */
    public int hashCode() {
        int code = (int)serialVersionUID;
        if (geographicArea != null) code += geographicArea.hashCode();
        if (resolution     != null) code +=     resolution.hashCode();
        return code;
    }

    /**
     * Apr�s la lecture binaire, v�rifie si
     * l'objet lu existait d�j� en m�moire.
     */
    private Object readResolve() throws ObjectStreamException {
        return Table.POOL.canonicalize(this);
    }
}
