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
package fr.ird.animat;

// J2SE
import java.util.Map;
import java.util.Date;
import java.util.Collections;
import java.awt.Color;
import java.awt.Rectangle;

// Geotools dependencies
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.cs.GeographicCoordinateSystem;

// Cartes
import fr.ird.map.RepaintManager;
import org.geotools.renderer.j2d.RenderedGridCoverage;

// Animats
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
     * L'objet à utiliser pour synchroniser les retraçaces.
     */
    private final RepaintManager manager;

    /**
     * <code>true</code> si on se trouve à l'intérieur de la méthode {@link #environmentChanged}.
     * Dans ces conditions, on reportera l'appel de {@link Layer#repaint} jusqu'à ce que la mise
     * à jour de toutes les couches soient terminées. Ce report se fait à l'aide de la classe
     * {@link RepaintManager}.
     */
    private transient boolean processingEvent;

    /**
     * Construit une couche pour l'environnement spécifié.
     *
     * @param  environment Environnement à afficher.
     *         Il peut provenir d'une machine distante.
     * @param  manager Objet à utiliser pour redessiner les cartes.
     */
    public EnvironmentLayer(final Environment environment,
                            final RepaintManager manager)
    {
        super(GeographicCoordinateSystem.WGS84);
        this.manager = manager;
        environment.addEnvironmentChangeListener(this);
        setCoverage(environment.getCoverage(parameter));
    }

    /**
     * Appelée quand un environnement a changé. Si l'image affichée
     * a changée, elle sera retracée.
     */
    public void environmentChanged(final EnvironmentChangeEvent event) {
        final Environment environment = event.getSource();
        final Date oldDate = date;
        date = environment.getTimeStep().getStartTime();
        try {
            processingEvent = true;
            setCoverage(environment.getCoverage(parameter));
        } finally {
            processingEvent = false;
        }
        manager.repaint(this);
        firePropertyChange("date", oldDate, date);
    }

    /**
     * Définit la couverture à afficher.
     */
    private void setCoverage(Coverage coverage) {
        final GridCoverage grid;
        if (coverage instanceof GridCoverage) {
            grid = (GridCoverage) coverage;
        } else {
            grid = null;
        }
        setCoverage(grid);
    }

    /**
     * Set the grid coverage. A <code>null</code> value
     * will remove the current grid coverage.
     */
    public void setCoverage(GridCoverage coverage) {
        coverage = processor.doOperation("Recolor", coverage, "ColorMaps", COLOR_MAP);
        super.setCoverage(coverage);
    }

    /**
     * Redessine sette composante.
     */
    public void repaint() {
        if (!processingEvent) {
            super.repaint();
        }
    }

    /**
     * Redessine sette composante.
     */
    protected void repaint(final Rectangle bounds) {
        if (!processingEvent) {
            super.repaint(bounds);
        }
    }
}
