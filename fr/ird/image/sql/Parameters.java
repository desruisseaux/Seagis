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

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.pt.Envelope;

import net.seas.opengis.cs.CoordinateSystem;
import net.seas.opengis.cs.CompoundCoordinateSystem;
import net.seas.opengis.cs.TemporalCoordinateSystem;
import net.seas.opengis.cs.HorizontalCoordinateSystem;
import net.seas.opengis.cs.GeographicCoordinateSystem;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.ct.TransformException;
import net.seas.opengis.ct.CoordinateTransform;
import net.seas.opengis.ct.CoordinateTransformFactory;

// OpenGIS dependencies (SEAGIS)
import net.seas.opengis.gp.Operation;
import net.seas.opengis.gp.GridCoverageProcessor;

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
import net.seas.util.XClass;
import net.seas.util.OpenGIS;
import net.seas.util.WeakHashSet;
import net.seas.util.XDimension2D;
import net.seas.resources.Resources;
import java.util.logging.LogRecord;
import java.util.logging.Level;


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
    // private static final long serialVersionUID = -8035026364433027427L; // TODO

    /**
     * Objet à utiliser par défaut pour construire des transformations de coordonnées.
     */
    public static final CoordinateTransformFactory TRANSFORMS = CoordinateTransformFactory.DEFAULT;

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
     * Format à utiliser pour lire les images.
     */
    public final FormatEntry format;

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
     * @param coordinateSystem Système de coordonnées utilisé.  Le système de
     *        coordonnées de tête ("head") doit obligatoirement être un objet
     *        {@link HorizontalCoordinateSystem}.
     * @param geographicArea Coordonnées horizontales de la région d'intéret.
     * @param resolution Dimension logique approximative désirée des pixels,
     *        ou <code>null</code> pour la meilleure résolution disponible.
     * @param dateFormat Formatteur à utiliser pour écrire des dates pour l'utilisateur.
     */
    public Parameters(final SeriesEntry              series,
                      final FormatEntry              format,
                      final String                   pathname,
                      final Operation                operation,
                      final CompoundCoordinateSystem coordinateSystem,
                      final Rectangle2D              geographicArea,
                      final Dimension2D              resolution,
                      final DateFormat               dateFormat)
    {
        this.series           = series;
        this.format           = format;
        this.pathname         = pathname;
        this.operation        = operation;
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
            final CoordinateTransform transform = TRANSFORMS.createFromCoordinateSystems(headCS, cs);
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

            final CompoundCoordinateSystem  ccs = new CompoundCoordinateSystem(coordinateSystem.getName(null), cs, coordinateSystem.getTailCS());
            final Parameters         parameters = new Parameters(series, format, pathname, operation, ccs, newGeographicArea, newResolution, dateFormat);

            Table.logger.fine(Resources.format(Clé.TRANSFORMATION_TO_CS¤1, cs.getName(null)));
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
            return XClass.equals(this.series          , that.series          ) &&
                   XClass.equals(this.format          , that.format          ) &&
                   XClass.equals(this.pathname        , that.pathname        ) &&
                   XClass.equals(this.operation       , that.operation       ) &&
                   XClass.equals(this.coordinateSystem, that.coordinateSystem) &&
                   XClass.equals(this.geographicArea  , that.geographicArea  ) &&
                   XClass.equals(this.resolution      , that.resolution      ) &&
                   XClass.equals(this.dateFormat      , that.dateFormat      );
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
