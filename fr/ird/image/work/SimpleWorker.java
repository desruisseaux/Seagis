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
package fr.ird.image.work;

// Entrés/sorties
import java.io.File;
import java.io.IOException;

// Seagis
import fr.ird.resources.XArray;
import fr.ird.database.coverage.CoverageEntry;


/**
 * Effectue des opérations sur une séries d'images. Chaque image de la série sera traitée
 * individuellement, indépendemment des autres. L'opération produira un objet {@link Result}
 * par image. L'utilisation de cette classe se fait en plusieurs étapes:
 *
 * <ul>
 *   <li>Créer une classe dérivée qui effectue l'opération
 *       souhaitée. Cette classe dérivée doit redéfinir la
 *       méthode {@link #run(CoverageEntry,Result)}.</li>
 *   <li>Appeler une des méthodes <code>setCoverages(...)</code>
 *       pour spécifier les images sur lesquelles on veut
 *       appliquer l'opération.</li>
 *   <li>Appeler {@link #setDestination} pour spécifier le
 *       répertoire de destination dans lequel placer les
 *       sorties des opérations.</li>
 *   <li>Appeler {@link #run()} pour lancer l'opération.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class SimpleWorker extends Worker {
    /**
     * Construit un objet qui effectuera des opérations sur une série
     * d'images. Le type d'opération dépendra de la classe dérivée.
     *
     * @param name Nom de l'opération.
     */
    public SimpleWorker(final String name) {
        super(name);
    }

    /**
     * Démarre le travail sur une série d'images. L'implémentation par défaut
     * appelle {@link #run(CoverageEntry,Result)} pour chaque image de la série.
     * Certaines implémentations peuvent répartir le travail sur plusieurs
     * threads.
     *
     * @param  entries Séries d'images sur lequel faire le travail.
     * @param  result  Résultat obtenus la dernières fois que ce travail avait été fait, ou
     *                 <code>null</code> si aucun résulat n'avait été précédemment sauvegardé.
     * @return Résultat de ce travail, ou <code>null</code> s'il n'y en a pas.
     *
     * @see #stop()
     */
    protected Result run(CoverageEntry[] entries, final Result ignore) {
        while (entries.length != 0) {
            final int index = (int)(entries.length*Math.random());
            if (index<0 || index>=entries.length) continue;
            final CoverageEntry entry = entries[index];
            entries = XArray.remove(entries, index, 1);
            setDescription(entry.getName());

            Result result=null;
            String operation="run";
            final File outputFile=getOutputFile(entry);
            try {
                operation="load"; if (outputFile.exists()) result=Result.load(outputFile);
                operation="run";  result=run(entry, result);
                operation="save"; if (result!=null) result.save(outputFile);
            } catch (IOException exception) {
                exceptionOccurred(operation, exception);
            }
            if (isStopped()) {
                break;
            }
        }
        return null;
    }

    /**
     * Démarre le travail sur une image.
     *
     * @param  image L'image sur laquelle effectuer l'opération.
     * @param  Si cette opération avait déjà été faite précédemment, le résultat qu'elle
     *         avait produit. Sinon, <code>null</code>.  En général, les implémentations
     *         de cette méthode vont choisir l'action à prendre sur la base des critères
     *         suivants:
     *         <ul>
     *           <li>Si <code>result</code> est nul, alors cette opération n'a jamais été
     *               faite. Cette méthode va alors démarrer l'opération sur l'image
     *               <code>image</code> et retourner un nouveau {@link Result}.</li>
     *           <li>Si <code>result</code> est non-nul, alors cette opération a déjà été
     *               faite auparavant mais n'a pas peut-être pas été complétée. Cette
     *               méthode peut alors examiner les champs internes de <code>result</code>
     *               et décider de l'action à prendre selon les critères suivants:</li>
     *           <ul>
     *             <li>Si <code>result</code> n'est pas un objet valide, alors cette méthode
     *                 l'ignorera. Elle démarrera l'opération et retournera un nouvel objet
     *                 {@link Result}.</li>
     *             <li>Si <code>result</code> est valide mais incomplet, alors cette méthode
     *                 tentera de le compléter et retournera <code>result</code>.</li>
     *             <li>Si <code>result</code> est valide et complet, alors cette méthode
     *                 n'a rien à faire et retournera <code>null</code>.</li>
     *           </ul>
     *         </ul>
     * @return Un objet représentant le résultat de l'opération, ou <code>null</code> s'il
     *         n'y en a pas. Si l'objet retourné est non-nul, il sera enregistré en binaire
     *         dans le répertoire de destination ({@link #setDestinationDirectory}) sous le
     *         même nom que l'image mais avec l'extension ".data". L'objet retourné peut
     *         être incomplet si la méthode {@link #stop} a été appelée pendant l'opération,
     *         mais il doit toujours être dans un état cohérent.
     * @throws IOException si une opération d'entrés/sorties était nécessaire et a échouée.
     *
     * @see #run()
     * @see #stop()
     * @see #progress
     */
    protected abstract Result run(final CoverageEntry image, final Result result) throws IOException;

    /**
     * Enregistre les données de l'image spécifiée. Cette méthode peut être appelée
     * à différentes étapes du calcul pour enregistrer l'état actuel des travaux,
     * sans être obligé de tout recommencer la prochaine fois que l'on redémarrera
     * le calcul.
     */
    protected void save(final CoverageEntry entry, final Result result) {
        try {
            if (result != null) {
                result.save(getOutputFile(entry));
            }
        } catch (IOException exception) {
            exceptionOccurred("save", exception);
        }
    }
}
