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
package fr.ird.animat.viewer;

// J2SE dependencies
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collections;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
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
import fr.ird.animat.Population;
import fr.ird.animat.Environment;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Couche repr�sentant une image sur une carte.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EnvironmentLayer extends RenderedGridCoverage implements Runnable {
    /**
     * L'adapter � utiliser pour convertir les couches 
     */
    private final Adapters adapters = Adapters.getDefault();

    /**
     * Palette de couleurs � utiliser pour l'affichage de l'image.
     * On utilisera une palette en tons de gris plut�t que la palette
     * par d�faut (color�e) de l'image afin de rendre les autres �l�ments
     * (positions des thons, trajectoire, etc.) plus facilement visibles.
     */
    private static final Map[] COLOR_MAP = new Map[] {
        Collections.singletonMap(null, new Color[] {
            new Color(16,   32,  64),
            new Color(224, 240, 255)})
    };
    
    /**
     * L'objet � utiliser pour traiter les images.
     */
    private final GridCoverageProcessor processor = GridCoverageProcessor.getDefault();

    /**
     * La date et heure de l'image actuellement affich�e,
     * ou <code>null</code> si aucune image n'est affich�e.
     */
    private Date date;

    /**
     * Param�tre � afficher.
     */
    private final Parameter parameter;

    /**
     * La barre de couleurs � afficher en dessous de la carte.
     */
    final ColorBar colors = new ColorBar();

    /**
     * L'environnement dessin� par cette couche.
     */
    private final Environment environment;

    /**
     * Les instructions � ex�cuter si jamais la machine virtuelle �tait interrompue
     * par l'utilisateur avec [Ctrl-C] ou quelque autre signal du genre.  Ce thread
     * va retirer les "listeners" afin de ne pas encombrer le serveur, qui lui
     * continuera � fonctionner.
     */
    private final Thread shutdownHook;

    /**
     * L'objet charg� d'�couter les modifications survenant dans l'environnement.
     * Ces changements peuvent survenir sur une machine distante.
     */
    private final Listener listener;

    /**
     * Construit une couche pour l'environnement sp�cifi�.
     *
     * @param  environment Environnement � afficher.
     *         Il peut provenir d'une machine distante.
     */
    public EnvironmentLayer(final Environment environment) throws RemoteException {
        super(null);
        this.environment = environment;
        final Iterator<Parameter> parameters = environment.getParameters().iterator();
        if (parameters.hasNext()) {
            parameter = parameters.next();
            setCoverage(adapters.wrap(environment.getCoverage(parameter)));
        } else {
            parameter = null;
        }
        listener = new Listener();
        environment.addEnvironmentChangeListener(listener);
        Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread(this));
    }

    /**
     * Ex�cut� automatiquement lorsque la machine virtuelle est en cours de fermeture.
     */
    public void run() {
        try {
            environment.removeEnvironmentChangeListener(listener);
        } catch (RemoteException exception) {
            // Logging during shutdown may fail silently. May be better than nothing...
            failed("EnvironmentLayer", "shutdownHook", exception);
        }
    }

    /**
     * Lib�re les ressources utilis�es par cette couche.
     */
    public void dispose() {
        synchronized (getTreeLock()) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            run();
            super.dispose();
        }
    }

    /**
     * D�finit la couverture � afficher.
     */
    private void setCoverage(final Coverage coverage) {
        final GridCoverage grid;
        if (coverage instanceof GridCoverage) {
            grid = (GridCoverage) coverage;
        } else {
            grid = null;
        }
        try {
            setGridCoverage(grid);
        } catch (TransformException exception) {
            LogRecord record = new LogRecord(Level.WARNING, "Syst�mes de coordonn�es incompatibles");
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

    /**
     * Appel�e automatiquement lorsque l'ex�cution d'une m�thode RMI a �chou�e.
     */
    static void failed(final String classe, final String method, final RemoteException exception) {
        final LogRecord record = new LogRecord(Level.WARNING, exception.getLocalizedMessage());
        record.setSourceClassName(classe);
        record.setSourceMethodName(method);
        record.setThrown(exception);
        Logger.getLogger("fr.ird.animat.viewer").log(record);
    }

    /**
     * Objet ayant la charge de r�agir aux changements survenant dans l'environnement.
     * Ces changements peuvent se produire sur une machine distante.
     */
    private final class Listener extends UnicastRemoteObject implements EnvironmentChangeListener,
                                                                        Runnable
    {
        /**
         * Liste des changements survenus r�cemment dans l'environnement. Cette
         * liste sera trait�e dans l'ordre "premier arriv�, premier trait�".
         */
        private final LinkedList<EnvironmentChangeEvent> events = new LinkedList<EnvironmentChangeEvent>();

        /**
         * La nouvelle couverture � prendre en compte.
         */
        private transient Coverage coverage;

        /**
         * Construit un objet par d�faut. L'objet sera export�
         * imm�diatement pour un �ventuel usage avec les RMI.
         */
        public Listener() throws RemoteException {
        }

        /**
         * Appel�e quand l'environnement a chang�. Si la date a chang�e, l'image correspondant
         * au pas de temps courant sera charg�e et affich�e.  Les notifications d'ajouts et de
         * suppressions de populations seront transmis aux {@link #listeners} (afin de r�duire
         * le volume de donn�es �ventuellement transmis par une machine distante) mais ne recevront
         * pas de traitement particuliers dans cette m�thode.
         */
        public synchronized void environmentChanged(final EnvironmentChangeEvent event)
                throws RemoteException
        {
            events.addLast(event);
            if (parameter != null) {
                if (event.changeOccured(EnvironmentChangeEvent.DATE_CHANGED)) {
                    coverage = adapters.wrap(event.getSource().getCoverage(parameter));
                }
            }
            EventQueue.invokeLater(this);
        }

        /**
         * Proc�de au traitement des �v�nements. Cette m�thode sera ex�cut�e dans le thread
         * de Swing.
         */
        public synchronized void run() {
            while (!events.isEmpty()) {
                if (coverage != null) {
                    setCoverage(coverage);
                    coverage = null;
                }
                final EnvironmentChangeEvent event = events.removeFirst();
                if (event.changeOccured(EnvironmentChangeEvent.DATE_CHANGED)) {
                    final Date oldDate = date;
                    date = event.getEnvironmentDate();
                    listeners.firePropertyChange("date", oldDate, date);
                }
                /*
                 * Signale les changements de populations. Bien que l'on travaille sur des ensembles,
                 * en g�n�ral il n'y aura qu'une seule population d'ajout�e ou de supprim�e.
                 */
                Set<Population> change = event.getPopulationRemoved();
                if (change != null) {
                    for (final Iterator<Population> it=change.iterator(); it.hasNext();) {
                        listeners.firePropertyChange("population", it.next(), null);
                    }
                }
                change = event.getPopulationAdded();
                if (change != null) {
                    for (final Iterator<Population> it=change.iterator(); it.hasNext();) {
                        listeners.firePropertyChange("population", null, it.next());
                    }
                }
            }
        }
    }
}
