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
package fr.ird.animat.gui.swing;

// J2SE dependencies
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collections;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.EventQueue;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.EventListenerList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

// OpenGIS (ancien)
import org.opengis.cv.CV_Coverage;

// OpenGIS
import org.opengis.referencing.operation.TransformException;

// Geotools dependencies
import org.geotools.gp.Adapters;
import org.geotools.cv.Coverage;
import org.geotools.gc.GridCoverage;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import org.geotools.gui.swing.ColorBar;

// Animats
import fr.ird.animat.Clock;
import fr.ird.animat.Parameter;
import fr.ird.animat.Population;
import fr.ird.animat.Environment;
import fr.ird.animat.Simulation;
import fr.ird.animat.event.EnvironmentChangeEvent;
import fr.ird.animat.event.EnvironmentChangeListener;


/**
 * Couche repr�sentant une image sur une carte. Cet objet impl�mente aussi une liste des
 * noms des couvertures utilis�es lors du dernier pas de temps.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class EnvironmentLayer extends RenderedGridCoverage implements ListModel, Runnable {
    /**
     * L'adapteur � utiliser pour convertir les couches 
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
     * La palette de couleurs � utiliser pour l'affichage de l'image,
     * ou <code>null</code> pour conserver la palette originale.
     */
    private Map[] colorMap = COLOR_MAP;

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
    protected final Environment environment;

    /**
     * Les noms des couvertures du pas de temps courant.
     */
    private String[] coverageNames;

    /**
     * Objet int�ress� � �tre inform� des changements apport�s � cet objet.
     */
    private final EventListenerList listenerList = new EventListenerList();

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
        try {
            setCoordinateSystem(GeographicCoordinateSystem.WGS84);
        } catch (TransformException exception) {
            throw new AssertionError(exception); // Should not happen
        }
        this.environment = environment;
        final Iterator<? extends Parameter> parameters = environment.getParameters().iterator();
        if (parameters.hasNext()) {
            parameter = parameters.next();
            setCoverage(adapters.wrap(environment.getCoverage(parameter)));
        } else {
            parameter = null;
        }
        listener = new Listener();
        environment.addEnvironmentChangeListener(listener);
        shutdownHook = new Thread(Simulation.THREAD_GROUP, this, "EnvironmentLayer shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
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
        if (coverage!=null && colorMap!=null) {
            coverage = processor.doOperation("Recolor", coverage, "ColorMaps", colorMap);
        }
        super.setGridCoverage(coverage);
        colors.setColors(coverage);
    }

    /**
     * D�finit la palette de couleurs � appliquer sur toutes les images affich�es par cette
     * couche. Une valeur nulle indique que l'afficheur devra conserver la palette originale
     * de chaque image.
     *
     * @param colorMap Les palettes de couleurs, ou <code>null</code>
     *        pour ne pas imposer de palette.
     */
    public void setColorMap(final Map[] colorMap) {
        this.colorMap = colorMap;
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
     * Returns the length of the list.
     */
    public int getSize() {
        return (coverageNames!=null) ? coverageNames.length : 0;
    }
    
    /**
     * Returns the value at the specified index.
     */
    public String getElementAt(final int index) {
        return coverageNames[index];
    }

    /**
     * Adds a listener to the list that's notified each time a change to the data model occurs.
     * @param listener the <code>ListDataListener</code> to be added
     */
    public void addListDataListener(final ListDataListener listener) {
        listenerList.add(ListDataListener.class, listener);
    }
    
    /**
     * Removes a listener from the list that's notified each time a change to the data model occurs.
     * @param l the <code>ListDataListener</code> to be removed
     */
    public void removeListDataListener(final ListDataListener listener) {
        listenerList.remove(ListDataListener.class, listener);
    }

    /**
     * Remet � jour la liste des noms des couvertures.
     */
    private void refreshCoverageNames() throws RemoteException {
        final String[] check = environment.getCoverageNames();
        if (!Arrays.equals(coverageNames, check)) {
            coverageNames = check;
            final ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED,
                                                          0, coverageNames.length);
            final Object[] listeners = listenerList.getListenerList();
            for (int i=listeners.length-2; i>=0; i-=2) {
                if (listeners[i] == ListDataListener.class) {
                    ((ListDataListener)listeners[i+1]).contentsChanged(event);
                }
            }
        }
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
         * Retourne le prochain �v�nement qui se trouvait dans la queue,
         * ou <code>null</code> s'il n'y en a pas.
         */
        private EnvironmentChangeEvent next() {
            final Coverage coverage;
            final EnvironmentChangeEvent event;
            synchronized (this) {
                if (events.isEmpty()) {
                    return null;
                }
                event = events.removeFirst();
                coverage = this.coverage;
                this.coverage = null;
            }
            if (coverage != null) {
                setCoverage(coverage);
            }
            return event;
        }

        /**
         * Proc�de au traitement des �v�nements. Cette m�thode sera ex�cut�e dans le thread
         * de Swing. Elle ne doit pas �tre synchronis�e sur <code>this</code> afin d'�viter
         * des "dead-locks" au moment d'appeller une m�thode qui se synchroniserait sur
         * <code>Environment.getTreeLock()</code>.
         */
        public void run() {
            EnvironmentChangeEvent event;
            while ((event=next()) != null) {
                if (event.changeOccured(EnvironmentChangeEvent.DATE_CHANGED)) {
                    final Date oldDate = date;
                    date = event.getEnvironmentDate();
                    listeners.firePropertyChange("date", oldDate, date);
                    try {
                        refreshCoverageNames();
                    } catch (RemoteException exception) {
                        failed("EnvironmentLayer", "environmentChanged", exception);
                    }
                }
                /*
                 * Signale les changements de populations. Bien que l'on travaille sur des ensembles,
                 * en g�n�ral il n'y aura qu'une seule population d'ajout�e ou de supprim�e.
                 */
                Set<Population> change = event.getPopulationRemoved();
                if (change != null) {
                    for (final Population population : change) {
                        listeners.firePropertyChange("population", population, null);
                    }
                }
                change = event.getPopulationAdded();
                if (change != null) {
                    for (final Population population : change) {
                        listeners.firePropertyChange("population", null, population);
                    }
                }
            }
        }
    }
}
