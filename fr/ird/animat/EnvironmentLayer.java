/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D�veloppement
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
package fr.ird.animat;

// Graphisme
import java.awt.Color;
import java.awt.Rectangle;

// Cartes
import fr.ird.map.RepaintManager;
import fr.ird.map.layer.GridCoverageLayer;

// Geotools dependencies
import org.geotools.gc.GridCoverage;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.cs.GeographicCoordinateSystem;

// Animats
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Couche repr�sentant une image sur une carte.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EnvironmentLayer extends GridCoverageLayer implements EnvironmentChangeListener
{
    /**
     * Couleur des valeurs les plus basses.
     */
    private static final Color LOWER_COLOR = new Color(16,32,64);

    /**
     * Couleur des valeurs les plus �lev�es.
     */
    private static final Color UPPER_COLOR = new Color(224,240,255);
    
    /**
     * L'objet � utiliser pour traiter les images.
     */
    private final GridCoverageProcessor processor = GridCoverageProcessor.getDefault();

    /**
     * L'index du param�tre � afficher.
     */
    private int parameter = 0;

    /**
     * L'objet � utiliser pour synchroniser les retra�aces.
     */
    private final RepaintManager manager;

    /**
     * <code>true</code> si on se trouve � l'int�rieur de la m�thode {@link #environmentChanged}.
     */
    private boolean processingEvent;

    /**
     * Construit une couche pour l'environnement sp�cifi�.
     */
    public EnvironmentLayer(final Environment environment, final RepaintManager manager)
    {
        super(GeographicCoordinateSystem.WGS84);
        this.manager = manager;
        environment.addEnvironmentChangeListener(this);
        setCoverage(environment.getGridCoverage(parameter));
    }

    /**
     * Appel�e quand un environnement a chang�.
     */
    public void environmentChanged(final EnvironmentChangeEvent event)
    {
        final Environment environment = event.getSource();
        try
        {
            processingEvent = true;
            setCoverage(environment.getGridCoverage(parameter));
        }
        finally
        {
            processingEvent = false;
        }
        manager.repaint(this);
    }

    /**
     * Set the grid coverage. A <code>null</code> value
     * will remove the current grid coverage.
     */
    public void setCoverage(GridCoverage coverage)
    {
        coverage = processor.doOperation("Colormap", coverage,
                                         "Colors", new Color[] {LOWER_COLOR, UPPER_COLOR});
        super.setCoverage(coverage);
    }

    /**
     * Redessine sette composante.
     */
    public void repaint()
    {
        if (!processingEvent)
            super.repaint();
    }

    /**
     * Redessine sette composante.
     */
    protected void repaint(final Rectangle bounds)
    {
        if (!processingEvent)
            super.repaint(bounds);
    }
}
