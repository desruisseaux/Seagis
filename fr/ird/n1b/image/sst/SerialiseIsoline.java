/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le D?veloppement
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
 *          Maison de la t?l?d?tection
 *          Institut de Recherche pour le d?veloppement
 *          500 rue Jean-Fran?ois Breton
 *          34093 Montpellier
 *          France
 *
 *          mailto:Michel.Petit@mpl.ird.fr
 */
package fr.ird.n1b.image.sst;

// J2SE.
import java.io.File;

// GEOTOOLS.
import org.geotools.util.ProgressListener;

/**
 * Génère à partir d'un fichier contenant des traits de côte au format GEBCO un fichier 
 * sérialisé contenant un <CODE>Isoline</CODE>.
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class SerialiseIsoline implements ProgressListener
{
    /**
     * Sérialise.
     *
     * @param in        Fichier contenant le trait de cote au format GEBCO.
     * @param out       Fichier ou sera ecrit le fichier serialise.
     * @param assemble  <i>true</i> si les traits de côte doivent être remplis.
     * @param listener  Un listener ou null.
     */
    public static void serialise(final File             in,
                                 final File             out,
                                 final boolean          assemble,
                                 final ProgressListener listener)
    {
        Utilities.serializeIsolineGebco(in, out, assemble, listener);
    }
    
        /** Indique que l'opération est terminée. L'indicateur visuel informant des
     * progrès sera ramené à 100% ou disparaîtra, selon l'implémentation de la
     * classe dérivée. Si des messages d'erreurs ou d'avertissements étaient
     * en attente, ils seront écrits.
     *
     */
    public void complete() {
    }
    
    /** Libère les ressources utilisées par cet objet. Si l'état d'avancement
     * était affiché dans une fenêtre, cette fenêtre peut être détruite.
     *
     */
    public void dispose() {
    }
    
    /** Indique qu'une exception est survenue pendant le traitement de l'opération.
     * Cette méthode peut afficher la trace de l'exception dans une fenêtre ou à
     * la console, dépendemment de la classe dérivée.
     *
     */
    public void exceptionOccurred(Throwable exception) {
    }
    
    /** Retourne le message d'écrivant l'opération
     * en cours. Si aucun message n'a été définie,
     * retourne <code>null</code>.
     *
     */
    public String getDescription() 
    {
        return "Generate isoline";
    }
    
    /** Indique l'état d'avancement de l'opération. Le progrès est représenté par un
     * pourcentage variant de 0 à 100 inclusivement. Si la valeur spécifiée est en
     * dehors de ces limites, elle sera automatiquement ramenée entre 0 et 100.
     *
     */
    public void progress(float percent) 
    {
        System.out.println("Percent : " + percent);
    }
    
    /** Spécifie un message qui décrit l'opération en cours.
     * Ce message est typiquement spécifiée avant le début
     * de l'opération. Toutefois, cette méthode peut aussi
     * être appelée à tout moment pendant l'opération sans
     * que cela affecte le pourcentage accompli. La valeur
     * <code>null</code> signifie qu'on ne souhaite plus
     * afficher de description.
     *
     */
    public void setDescription(String description) 
    {
    }
    
    /** Indique que l'opération a commencée.
     *
     */
    public void started() 
    {
    }
    
    /** Envoie un message d'avertissement. Ce message pourra être envoyé vers le
     * périphérique d'erreur standard, apparaître dans une fenêtre ou être tout
     * simplement ignoré.
     *
     * @param source Chaîne de caractère décrivant la source de l'avertissement.
     *        Il s'agira par exemple du nom du fichier dans lequel une anomalie
     *        a été détectée. Peut être nul si la source n'est pas connue.
     * @param margin Texte à placer dans la marge de l'avertissement <code>warning</code>,
     *        ou <code>null</code> s'il n'y en a pas. Il s'agira le plus souvent du numéro
     *        de ligne où s'est produite l'erreur dans le fichier <code>source</code>.
     * @param warning Message d'avertissement à écrire.
     *
     */
    public void warningOccurred(String source, String margin, String warning) 
    {
    }    
    
    /**
     * Génère un fichier sérialisé contenant un <CODE>Isoline</CODE> depuis un fichier 
     * contenant des informations au format GEBCO. Il est aussi possible d'indiquer au 
     * programme de remplir le trait de côte.
     *
     * @param args[0]   Le nom du fichier source.
     * @param args[1]   Le nom du fichier serialise a generer.
     * @param args[2]   <i>true</i> si le trait de côte doit-être remplie ou assemblé et 
     *                  <i>false</i> sinon.
     */
    public static void main(final String[] args)
    {
        final int count = args.length;
        if (count !=3 ) 
        {
            System.err.println("Format  : SerialiseIsoline SOURCE_COASTLINE DESTINATION PLEIN");
            System.err.println("SOURCE_COASTLINE      --> Fichier ascii gebco");
            System.err.println("DESTINATION           --> Fichier de destination");
            System.err.println("PLEIN                 --> true pour remplir le trait de la côte et false sinon");            
            System.exit(-1);
        }                
        serialise(new File(args[0]), new File(args[1]), (Boolean.valueOf(args[2])).booleanValue(), new SerialiseIsoline());
    }    
}