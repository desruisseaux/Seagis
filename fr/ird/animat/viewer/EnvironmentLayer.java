/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.animat.viewer;

// J2SE dependencies
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Collections;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.rmi.RemoteException;

// OpenGIS dependencies
import org.opengis.cv.CV_Coverage;

// Geotools dependencies
import org.geotools.gp.Adapters;
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.ct.TransformException;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import org.geotools.gui.swing.ColorBar;

// Animats
import fr.ird.animat.Parameter;
import fr.ird.animat.Environment;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Couche représentant une image sur une carte.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EnvironmentLayer extends RenderedGridCoverage implements EnvironmentChangeListener {
    /**
     * L'adapter à utiliser pour convertir les couches 
     */
    private final Adapters adapters = Adapters.getDefault();

    /**
     * Palette de couleurs à utiliser pour l'affichage de l'image.
     * On utilisera une palette en tons de gris plutôt que la palette
     * par défaut (colorée) de l'image afin de rendre les autres éléments
     * (positions des thons, trajectoire, etc.) plus facilement visibles.
     */
    private static final Map[] COLOR_MAP = new Map[] {
        Collections.singletonMap(null, new Color[] {
            new Color(16,   32,  64),
            new Color(224, 240, 255)})
    };
    
    /**
     * L'objet à utiliser pour traiter les images.
     */
    private final GridCoverageProcessor processor = GridCoverageProcessor.getDefault();

    /**
     * La date et heure de l'image actuellement affichée,
     * ou <code>null</code> si aucune image n'est affichée.
     */
    private Date date;

    /**
     * Paramètre à afficher.
     */
    private Parameter parameter;

    /**
     * La barre de couleurs à afficher en dessous de la carte.
     */
    final ColorBar colors = new ColorBar();

    /**
     * Construit une couche pour l'environnement spécifié.
     *
     * @param  environment Environnement à afficher.
     *         Il peut provenir d'une machine distante.
     */
    public EnvironmentLayer(final Environment environment) throws RemoteException {
        super(null);
        final Set<Parameter> parameters = environment.getParameters();
        this.parameter = parameters.iterator().next();
        environment.addEnvironmentChangeListener(this);
        setCoverage(environment.getCoverage(parameter));
    }

    /**
     * Appelée quand un environnement a changé. Si l'image affichée a changée, elle sera retracée.
     */
    public void environmentChanged(final EnvironmentChangeEvent event) throws RemoteException {
        final Environment environment = event.getSource();
        final Date oldDate = date;
        date = environment.getClock().getTime();
        setCoverage(environment.getCoverage(parameter));
        listeners.firePropertyChange("date", oldDate, date);
    }

    /**
     * Définit la couverture à afficher.
     */
    private void setCoverage(final CV_Coverage coverage) throws RemoteException {
        final Coverage cv = adapters.wrap(coverage);
        final GridCoverage grid;
        if (cv instanceof GridCoverage) {
            grid = (GridCoverage) cv;
        } else {
            grid = null;
        }
        try {
            setGridCoverage(grid);
        } catch (TransformException exception) {
            LogRecord record = new LogRecord(Level.WARNING, "Systèmes de coordonnées incompatibles");
            record.setSourceClassName("EnvironmentLayer");
            record.setSourceMethodName("setCoverage");
            record.setThrown(exception);
            Logger.getLogger("fr.ird.animat").log(record);
        }
    }

    /**
     * Set the grid coverage. A <code>null</code> value
     * will remove the current grid coverage.
     */
    public void setGridCoverage(GridCoverage coverage) throws TransformException {
        coverage = processor.doOperation("Recolor", coverage, "ColorMaps", COLOR_MAP);
        super.setGridCoverage(coverage);
        colors.setColors(coverage);
    }
}
