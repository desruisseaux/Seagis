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
package fr.ird.image.work;

// Entr�s/sorties
import java.io.File;
import java.io.IOException;

// Seagis
import fr.ird.resources.XArray;
import fr.ird.database.coverage.CoverageEntry;


/**
 * Effectue des op�rations sur une s�ries d'images. Chaque image de la s�rie sera trait�e
 * individuellement, ind�pendemment des autres. L'op�ration produira un objet {@link Result}
 * par image. L'utilisation de cette classe se fait en plusieurs �tapes:
 *
 * <ul>
 *   <li>Cr�er une classe d�riv�e qui effectue l'op�ration
 *       souhait�e. Cette classe d�riv�e doit red�finir la
 *       m�thode {@link #run(CoverageEntry,Result)}.</li>
 *   <li>Appeler une des m�thodes <code>setCoverages(...)</code>
 *       pour sp�cifier les images sur lesquelles on veut
 *       appliquer l'op�ration.</li>
 *   <li>Appeler {@link #setDestination} pour sp�cifier le
 *       r�pertoire de destination dans lequel placer les
 *       sorties des op�rations.</li>
 *   <li>Appeler {@link #run()} pour lancer l'op�ration.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class SimpleWorker extends Worker {
    /**
     * Construit un objet qui effectuera des op�rations sur une s�rie
     * d'images. Le type d'op�ration d�pendra de la classe d�riv�e.
     *
     * @param name Nom de l'op�ration.
     */
    public SimpleWorker(final String name) {
        super(name);
    }

    /**
     * D�marre le travail sur une s�rie d'images. L'impl�mentation par d�faut
     * appelle {@link #run(CoverageEntry,Result)} pour chaque image de la s�rie.
     * Certaines impl�mentations peuvent r�partir le travail sur plusieurs
     * threads.
     *
     * @param  entries S�ries d'images sur lequel faire le travail.
     * @param  result  R�sultat obtenus la derni�res fois que ce travail avait �t� fait, ou
     *                 <code>null</code> si aucun r�sulat n'avait �t� pr�c�demment sauvegard�.
     * @return R�sultat de ce travail, ou <code>null</code> s'il n'y en a pas.
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
     * D�marre le travail sur une image.
     *
     * @param  image L'image sur laquelle effectuer l'op�ration.
     * @param  Si cette op�ration avait d�j� �t� faite pr�c�demment, le r�sultat qu'elle
     *         avait produit. Sinon, <code>null</code>.  En g�n�ral, les impl�mentations
     *         de cette m�thode vont choisir l'action � prendre sur la base des crit�res
     *         suivants:
     *         <ul>
     *           <li>Si <code>result</code> est nul, alors cette op�ration n'a jamais �t�
     *               faite. Cette m�thode va alors d�marrer l'op�ration sur l'image
     *               <code>image</code> et retourner un nouveau {@link Result}.</li>
     *           <li>Si <code>result</code> est non-nul, alors cette op�ration a d�j� �t�
     *               faite auparavant mais n'a pas peut-�tre pas �t� compl�t�e. Cette
     *               m�thode peut alors examiner les champs internes de <code>result</code>
     *               et d�cider de l'action � prendre selon les crit�res suivants:</li>
     *           <ul>
     *             <li>Si <code>result</code> n'est pas un objet valide, alors cette m�thode
     *                 l'ignorera. Elle d�marrera l'op�ration et retournera un nouvel objet
     *                 {@link Result}.</li>
     *             <li>Si <code>result</code> est valide mais incomplet, alors cette m�thode
     *                 tentera de le compl�ter et retournera <code>result</code>.</li>
     *             <li>Si <code>result</code> est valide et complet, alors cette m�thode
     *                 n'a rien � faire et retournera <code>null</code>.</li>
     *           </ul>
     *         </ul>
     * @return Un objet repr�sentant le r�sultat de l'op�ration, ou <code>null</code> s'il
     *         n'y en a pas. Si l'objet retourn� est non-nul, il sera enregistr� en binaire
     *         dans le r�pertoire de destination ({@link #setDestinationDirectory}) sous le
     *         m�me nom que l'image mais avec l'extension ".data". L'objet retourn� peut
     *         �tre incomplet si la m�thode {@link #stop} a �t� appel�e pendant l'op�ration,
     *         mais il doit toujours �tre dans un �tat coh�rent.
     * @throws IOException si une op�ration d'entr�s/sorties �tait n�cessaire et a �chou�e.
     *
     * @see #run()
     * @see #stop()
     * @see #progress
     */
    protected abstract Result run(final CoverageEntry image, final Result result) throws IOException;

    /**
     * Enregistre les donn�es de l'image sp�cifi�e. Cette m�thode peut �tre appel�e
     * � diff�rentes �tapes du calcul pour enregistrer l'�tat actuel des travaux,
     * sans �tre oblig� de tout recommencer la prochaine fois que l'on red�marrera
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
