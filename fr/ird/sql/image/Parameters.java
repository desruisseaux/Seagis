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

// Coordonn�es spatio-temporelles
import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

// Entr�s/sorties
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
 * Bloc de param�tres pour une table {@link ImageTable}. Les blocs de param�tres doivent
 * �tre imutables.  Ce principe d'imutabilit� s'applique aussi aux objets r�f�renc�s par
 * les champs publiques, m�me si ces objets sont en principe mutables ({@link Rectangle2D},
 * {@link Dimension2D}...).
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class Parameters implements Serializable
{
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
//  private static final long serialVersionUID = -3570512426526015662L; // TODO

    /**
     * Objet � utiliser par d�faut pour construire des transformations de coordonn�es.
     */
    public static final CoordinateTransformationFactory TRANSFORMS = CoordinateTransformationFactory.getDefault();

    /**
     * L'objet � utiliser pour appliquer
     * des op�rations sur les images lues.
     */
    public static final GridCoverageProcessor PROCESSOR = GridCoverageProcessor.getDefault();

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
    public final FormatEntryImpl format;

    /**
     * Chemin relatif des images.
     */
    public final String pathname;

    /**
     * Syst�me de coordonn�es utilis�. Le syst�me de coordonn�es de t�te ("head")
     * doit obligatoirement �tre un objet {@link HorizontalCoordinateSystem}.  La
     * seconde partie ("tail") sera ignor�e;   il s'agira typiquement de l'axe du
     * temps ou de la profondeur.
     */
    public final CompoundCoordinateSystem coordinateSystem;

    /**
     * Coordonn�es horizontales de la r�gion d'int�ret.  Ces coordonn�es
     * sont exprim�es selon la partie horizontale ("head") du syst�me de
     * coordonn�es {@link #coordinateSystem}.
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
     * @param coordinateSystem Syst�me de coordonn�es utilis�.  Le syst�me de
     *        coordonn�es de t�te ("head") doit obligatoirement �tre un objet
     *        {@link HorizontalCoordinateSystem}.
     * @param geographicArea Coordonn�es horizontales de la r�gion d'int�ret.
     * @param resolution Dimension logique approximative d�sir�e des pixels,
     *        ou <code>null</code> pour la meilleure r�solution disponible.
     * @param dateFormat Formatteur � utiliser pour �crire des dates pour l'utilisateur.
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
     * Retourne un bloc de param�tres avec les m�mes coordonn�es g�ographiques
     * que celui-ci, mais qui utilisera un syst�me de coordonn�es horizontales
     * diff�rent.
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
     * Indique si ce bloc de param�tres est identique au bloc sp�cifi�.
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
     * Formate la date sp�cifi�e.
     */
    public String format(final Date date)
    {
        synchronized (dateFormat)
        {
            return dateFormat.format(date);
        }
    }

    /**
     * Retourne un code repr�sentant ce bloc de param�tres.
     */
    public int hashCode()
    {
        int code=367891234;
        if (geographicArea != null) code += geographicArea.hashCode();
        if (resolution     != null) code +=     resolution.hashCode();
        return code;
    }

    /**
     * Apr�s la lecture binaire, v�rifie si
     * l'objet lu existait d�j� en m�moire.
     */
    private Object readResolve() throws ObjectStreamException
    {return Table.pool.intern(this);}
}
