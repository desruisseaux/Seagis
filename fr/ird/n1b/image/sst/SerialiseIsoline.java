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
 * G�n�re � partir d'un fichier contenant des traits de c�te au format GEBCO un fichier 
 * s�rialis� contenant un <CODE>Isoline</CODE>.
 *
 * @author Remi EVE
 * @version $Id$
 */
public final class SerialiseIsoline implements ProgressListener
{
    /**
     * S�rialise.
     *
     * @param in        Fichier contenant le trait de cote au format GEBCO.
     * @param out       Fichier ou sera ecrit le fichier serialise.
     * @param assemble  <i>true</i> si les traits de c�te doivent �tre remplis.
     * @param listener  Un listener ou null.
     */
    public static void serialise(final File             in,
                                 final File             out,
                                 final boolean          assemble,
                                 final ProgressListener listener)
    {
        Utilities.serializeIsolineGebco(in, out, assemble, listener);
    }
    
        /** Indique que l'op�ration est termin�e. L'indicateur visuel informant des
     * progr�s sera ramen� � 100% ou dispara�tra, selon l'impl�mentation de la
     * classe d�riv�e. Si des messages d'erreurs ou d'avertissements �taient
     * en attente, ils seront �crits.
     *
     */
    public void complete() {
    }
    
    /** Lib�re les ressources utilis�es par cet objet. Si l'�tat d'avancement
     * �tait affich� dans une fen�tre, cette fen�tre peut �tre d�truite.
     *
     */
    public void dispose() {
    }
    
    /** Indique qu'une exception est survenue pendant le traitement de l'op�ration.
     * Cette m�thode peut afficher la trace de l'exception dans une fen�tre ou �
     * la console, d�pendemment de la classe d�riv�e.
     *
     */
    public void exceptionOccurred(Throwable exception) {
    }
    
    /** Retourne le message d'�crivant l'op�ration
     * en cours. Si aucun message n'a �t� d�finie,
     * retourne <code>null</code>.
     *
     */
    public String getDescription() 
    {
        return "Generate isoline";
    }
    
    /** Indique l'�tat d'avancement de l'op�ration. Le progr�s est repr�sent� par un
     * pourcentage variant de 0 � 100 inclusivement. Si la valeur sp�cifi�e est en
     * dehors de ces limites, elle sera automatiquement ramen�e entre 0 et 100.
     *
     */
    public void progress(float percent) 
    {
        System.out.println("Percent : " + percent);
    }
    
    /** Sp�cifie un message qui d�crit l'op�ration en cours.
     * Ce message est typiquement sp�cifi�e avant le d�but
     * de l'op�ration. Toutefois, cette m�thode peut aussi
     * �tre appel�e � tout moment pendant l'op�ration sans
     * que cela affecte le pourcentage accompli. La valeur
     * <code>null</code> signifie qu'on ne souhaite plus
     * afficher de description.
     *
     */
    public void setDescription(String description) 
    {
    }
    
    /** Indique que l'op�ration a commenc�e.
     *
     */
    public void started() 
    {
    }
    
    /** Envoie un message d'avertissement. Ce message pourra �tre envoy� vers le
     * p�riph�rique d'erreur standard, appara�tre dans une fen�tre ou �tre tout
     * simplement ignor�.
     *
     * @param source Cha�ne de caract�re d�crivant la source de l'avertissement.
     *        Il s'agira par exemple du nom du fichier dans lequel une anomalie
     *        a �t� d�tect�e. Peut �tre nul si la source n'est pas connue.
     * @param margin Texte � placer dans la marge de l'avertissement <code>warning</code>,
     *        ou <code>null</code> s'il n'y en a pas. Il s'agira le plus souvent du num�ro
     *        de ligne o� s'est produite l'erreur dans le fichier <code>source</code>.
     * @param warning Message d'avertissement � �crire.
     *
     */
    public void warningOccurred(String source, String margin, String warning) 
    {
    }    
    
    /**
     * G�n�re un fichier s�rialis� contenant un <CODE>Isoline</CODE> depuis un fichier 
     * contenant des informations au format GEBCO. Il est aussi possible d'indiquer au 
     * programme de remplir le trait de c�te.
     *
     * @param args[0]   Le nom du fichier source.
     * @param args[1]   Le nom du fichier serialise a generer.
     * @param args[2]   <i>true</i> si le trait de c�te doit-�tre remplie ou assembl� et 
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
            System.err.println("PLEIN                 --> true pour remplir le trait de la c�te et false sinon");            
            System.exit(-1);
        }                
        serialise(new File(args[0]), new File(args[1]), (Boolean.valueOf(args[2])).booleanValue(), new SerialiseIsoline());
    }    
}