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
package fr.ird.sql.image;

// OpenGIS dependencies (SEAGIS)
import net.seagis.pt.Envelope;

import net.seagis.cs.CoordinateSystem;
import net.seagis.cs.CompoundCoordinateSystem;
import net.seagis.cs.TemporalCoordinateSystem;
import net.seagis.cs.HorizontalCoordinateSystem;
import net.seagis.cs.GeographicCoordinateSystem;

// OpenGIS dependencies (SEAGIS)
import net.seagis.ct.MathTransform2D;
import net.seagis.ct.TransformException;
import net.seagis.ct.CoordinateTransformationFactory;

// OpenGIS dependencies (SEAGIS)
import net.seagis.gp.Operation;
import net.seagis.gp.GridCoverageProcessor;

// Coordonnées spatio-temporelles
import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

// Entrés/sorties
import java.io.File;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;

// Divers
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;
import javax.media.jai.ParameterList;
import java.util.logging.LogRecord;
import java.util.logging.Level;

import net.seagis.resources.OpenGIS;
import net.seagis.resources.Utilities;
import net.seagis.resources.XDimension2D;


/**
 * Bloc de paramètres pour une table {@link ImageTable}. Les blocs de paramètres doivent
 * être imutables.  Ce principe d'imutabilité s'applique aussi aux objets référencés par
 * les champs publiques, même si ces objets sont en principe mutables ({@link Rectangle2D},
 * {@link Dimension2D}...).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Parameters implements Serializable
{
    /**
     * Numéro de série (pour compatibilité avec des versions antérieures).
     */
//  private static final long serialVersionUID = -3570512426526015662L; // TODO

    /**
     * Objet à utiliser par défaut pour construire des transformations de coordonnées.
     */
    public static final CoordinateTransformationFactory TRANSFORMS = CoordinateTransformationFactory.getDefault();

    /**
     * L'objet à utiliser pour appliquer
     * des opérations sur les images lues.
     */
    public static final GridCoverageProcessor PROCESSOR = GridCoverageProcessor.getDefault();

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
    public final FormatEntryImpl format;

    /**
     * Chemin relatif des images.
     */
    public final String pathname;

    /**
     * Système de coordonnées utilisé. Le système de coordonnées de tête ("head")
     * doit obligatoirement être un objet {@link HorizontalCoordinateSystem}.  La
     * seconde partie ("tail") sera ignorée;   il s'agira typiquement de l'axe du
     * temps ou de la profondeur.
     */
    public final CompoundCoordinateSystem coordinateSystem;

    /**
     * Coordonnées horizontales de la région d'intéret.  Ces coordonnées
     * sont exprimées selon la partie horizontale ("head") du système de
     * coordonnées {@link #coordinateSystem}.
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
     * @param coordinateSystem Système de coordonnées utilisé.  Le système de
     *        coordonnées de tête ("head") doit obligatoirement être un objet
     *        {@link HorizontalCoordinateSystem}.
     * @param geographicArea Coordonnées horizontales de la région d'intéret.
     * @param resolution Dimension logique approximative désirée des pixels,
     *        ou <code>null</code> pour la meilleure résolution disponible.
     * @param dateFormat Formatteur à utiliser pour écrire des dates pour l'utilisateur.
     */
    public Parameters(final SeriesEntry              series,
                      final FormatEntryImpl          format,
                      final String                   pathname,
                      final Operation                operation,
                      final ParameterList            parameters,
                      final CompoundCoordinateSystem coordinateSystem,
                      final Rectangle2D              geographicArea,
                      final Dimension2D              resolution,
                      final DateFormat               dateFormat)
    {
        this.series           = series;
        this.format           = format;
        this.pathname         = pathname;
        this.operation        = operation;
        this.parameters       = parameters;
        this.coordinateSystem = coordinateSystem;
        this.geographicArea   = geographicArea;
        this.resolution       = resolution;
        this.dateFormat       = dateFormat;
    }

    /**
     * Retourne un bloc de paramètres avec les mêmes coordonnées géographiques
     * que celui-ci, mais qui utilisera un système de coordonnées horizontales
     * différent.
     */
    public Parameters createTransformed(final HorizontalCoordinateSystem cs) throws TransformException
    {
        final CoordinateSystem headCS = coordinateSystem.getHeadCS();
        if (!headCS.equivalents(cs))
        {
            final MathTransform2D transform = (MathTransform2D) TRANSFORMS.createFromCoordinateSystems(headCS, cs).getMathTransform();
            final Rectangle2D newGeographicArea = OpenGIS.transform(transform, geographicArea, null);
            final Dimension2D newResolution;

            if (resolution!=null)
            {
                final double width  = resolution.getWidth();
                final double height = resolution.getHeight();
                Rectangle2D   pixel = new Rectangle2D.Double(geographicArea.getCenterX()-0.5*width,
                                                             geographicArea.getCenterY()-0.5*height,
                                                             width, height);
                pixel = OpenGIS.transform(transform, pixel, pixel);
                newResolution = new XDimension2D.Double(pixel.getWidth(), pixel.getHeight());
            }
            else newResolution=null;

            final CompoundCoordinateSystem ccs = new CompoundCoordinateSystem(coordinateSystem.getName(null), cs, coordinateSystem.getTailCS());
            final Parameters parameters = new Parameters(series, format, pathname,
                                                         operation, this.parameters,
                                                         ccs, newGeographicArea, newResolution,
                                                         dateFormat);

            Table.logger.fine(Resources.format(ResourceKeys.TRANSFORMATION_TO_CS_$1, cs.getName(null)));
            return (Parameters) Table.pool.intern(parameters);
        }
        return this;
    }

    /**
     * Indique si ce bloc de paramètres est identique au bloc spécifié.
     */
    public boolean equals(final Object o)
    {
        if (o instanceof Parameters)
        {
            final Parameters that = (Parameters) o;
            return Utilities.equals(this.series          , that.series          ) &&
                   Utilities.equals(this.format          , that.format          ) &&
                   Utilities.equals(this.pathname        , that.pathname        ) &&
                   Utilities.equals(this.operation       , that.operation       ) &&
                   Utilities.equals(this.parameters      , that.parameters      ) &&
                   Utilities.equals(this.coordinateSystem, that.coordinateSystem) &&
                   Utilities.equals(this.geographicArea  , that.geographicArea  ) &&
                   Utilities.equals(this.resolution      , that.resolution      ) &&
                   Utilities.equals(this.dateFormat      , that.dateFormat      );
        }
        return false;
    }

    /**
     * Formate la date spécifiée.
     */
    public String format(final Date date)
    {
        synchronized (dateFormat)
        {
            return dateFormat.format(date);
        }
    }

    /**
     * Retourne un code représentant ce bloc de paramètres.
     */
    public int hashCode()
    {
        int code=367891234;
        if (geographicArea != null) code += geographicArea.hashCode();
        if (resolution     != null) code +=     resolution.hashCode();
        return code;
    }

    /**
     * Après la lecture binaire, vérifie si
     * l'objet lu existait déjà en mémoire.
     */
    private Object readResolve() throws ObjectStreamException
    {return Table.pool.intern(this);}
}
