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
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Deflater;

// Images et divers
import java.awt.image.DataBuffer;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import javax.media.jai.PlanarImage;
import java.util.Hashtable;

// Geotools dependencies
import org.geotools.io.LineWriter;
import org.geotools.cv.SampleDimension;
import org.geotools.resources.Utilities;
import org.geotools.gui.swing.ExceptionMonitor;

// Seagis
import fr.ird.util.XArray;
import fr.ird.sql.image.ImageEntry;


/**
 * R�sultat d'une op�ration. Le contenu de cet
 * objet est laiss� libre aux classes d�riv�es.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Result implements Serializable {
    /**
     * Num�ro de s�rie (pour compatibilit� entre diff�rentes versions).
     */
    private static final long serialVersionUID = 4059406258349515732L;

    /**
     * Num�ros ID des images qui ont �t�
     * utilis�es pour construite cet objet.
     */
    private int[] imageIDs;

    /**
     * Construit un objet par d�faut.
     */
    public Result() {
    }

    /**
     * Lit un objet qui avait pr�c�demment �t� enregistr�
     * en binaire dans le fichier sp�cifi�.
     *
     * @param  file Fichier � lire.
     * @return Objet lu dans le fichiet sp�cifi�.
     * @throws IOException si une erreur est survenue lors de l'enregistrement.
     */
    public static Result load(final File file) throws IOException {
        final ObjectInputStream input = new ObjectInputStream(
                                        new InflaterInputStream(
                                        new FileInputStream(file)));
        try {
            final Result result = (Result) input.readObject();
            input.close();
            return result;
        } catch (ClassNotFoundException exception) {
            IOException e=new IOException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        } catch (ClassCastException exception) {
            IOException e=new IOException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Enregistre cet objet en binaire dans le fichier sp�cifi�.
     *
     * @param  file Fichier dans lequel enregistrer cet objet.
     * @throws IOException si une erreur est survenue lors de l'enregistrement.
     */
    public synchronized void save(final File file) throws IOException {
        final ObjectOutputStream output = new ObjectOutputStream(
                                          new DeflaterOutputStream(
                                          new FileOutputStream(file), new Deflater(Deflater.BEST_COMPRESSION)));
        output.writeObject(this);
        output.close();
    }

    /**
     * Retourne les donn�es sous forme d'une tuile d'une image. Si les donn�es ne peuvent pas
     * �tre repr�sent�es sous forme d'image, alors cette m�thode retourne <code>null</code>.
     * La tuile retourn�e devrait contenir les valeurs g�ophysiques de <code>this</code>, et
     * non des valeurs de pixels. Cela signifie que cette tuile utilisera la plupart du temps
     * des donn�es du type {@link DataBuffer#FLOAT}.
     * <br><br>
     * L'impl�mentation par d�faut retourne toujours <code>null</code>.
     */
    public WritableRaster getRaster() {
        return null;
    }

    /**
     * Retourne les donn�es sous forme d'une image. Si les donn�es ne peuvent pas �tre repr�sent�es
     * sous forme d'image, alors cette m�thode retourne <code>null</code>. L'impl�mentation par d�faut
     * enveloppe dans une image la tuile retourn�e par {@link #getRaster}.
     */
    public RenderedImage getImage(final SampleDimension[] bands) {
        final WritableRaster raster = getRaster(); if (raster==null) return null;
        final ColorModel     colors = PlanarImage.createColorModel(raster.getSampleModel());
        final Hashtable  properties = new Hashtable();
        final BufferedImage   image = new BufferedImage(colors, raster, false, properties);
        // TODO: appliquer l'op�ration GC_SampleTranscoding
        return image;
    }

    /**
     * Ecrit le contenu de cet objet sous forme de texte.
     *
     * @param  file Fichier dans lequel �crire une description de cet objet.
     * @throws IOException si une erreur est survenue lors de l'�criture.
     */
    public synchronized void write(final File file) throws IOException {
        final Writer output = new BufferedWriter(new FileWriter(file));
        write(output);
        output.close();
    }

    /**
     * Ecrit le contenu de cet objet sous forme de texte. L'impl�mentation
     * par d�faut �crit la liste des num�ros ID des images qui ont �t�
     * utilis�es pour ce calcul.
     *
     * @param  output Flot dans lequel �crire une description de cet objet.
     * @throws IOException si une erreur est survenue lors de l'�criture.
     */
    public synchronized void write(final Writer output) throws IOException {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        output.write("ID des images utilis�es:");
        output.write(lineSeparator);
        if (imageIDs.length != 0) {
            for (int i=0; i<imageIDs.length; i++) {
                output.write(String.valueOf(imageIDs[i]));
                output.write(lineSeparator);
            }
        }
    }

    /**
     * Retourne le contenu de cet objet sous forme de cha�ne de caract�res.
     * L'impl�mentation par d�faut appelle {@link #write(Writer)}.
     */
    public String toString() {
        final StringWriter output = new StringWriter();
        try {
            write(new LineWriter(output, "\n"));
        } catch (IOException exception) {
            // Should not happen.
            unexpectedException(Utilities.getShortClassName(this), "toString", exception);
        }
        return output.toString();
    }

    /**
     * Ajoute une image � la liste des images qui
     * ont �t� pris en compte pour la construction
     * de cet objet.
     */
    protected synchronized void add(final ImageEntry image) {
        final int ID = image.getID();
        if (imageIDs != null) {
            final int last = imageIDs.length;
            imageIDs = XArray.resize(imageIDs, last+1);
            imageIDs[last] = ID;
        } else {
            imageIDs = new int[] {ID};
        }
    }

    /**
     * Indique si l'image sp�cifi�e fait partie des images qui
     * ont �t� prises en compte dans le calcul des r�sultats.
     */
    protected synchronized boolean contains(final ImageEntry image) {
        if (imageIDs != null) {
            final int ID = image.getID();
            for (int i=0; i<imageIDs.length; i++) {
                if (imageIDs[i]==ID) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Signale qu'une erreur inatendue est survenue. Cette m�thode
     * �crira l'erreur dans un journal de l'application.
     */
    static void unexpectedException(final String className, final String method, final Throwable exception) {
        Utilities.unexpectedException("fr.ird.image.work", className, method, exception);
    }

    /**
     * Affiche sous forme de texte le contenu d'un objet {@link Result}.
     * Cette m�thode peut �tre appel�e � partir de la ligne de commande
     * avec deux arguments:
     *
     * <ul>
     *   <li>Le nom du fichier binaire d'entr�.</li>
     *   <li>Le nom du fichier texte de sortie. S'il est omis, la
     *       sortie se fera vers le p�riph�rique de sortie standard.</li>
     * </ul>
     */
    public static void main(final String[] args) {
        File   in = null;
        File  out = null;
        switch (args.length) {
            case 2: out = new File(args[1]); // fall through
            case 1: in  = new File(args[0]); // fall through
            case 0: break;
        }
        if (in == null) {
            System.out.println("Arguments: [input] [output]\n"+
                               "  - [input]  est le nom du fichier binaire d'entr�.\n"+
                               "  - [output] est le nom du fichier texte de sortie.\n"+
                               "             S'il est omis, la sortie se fera vers\n"+
                               "             le p�riph�rique de sortie standard.");
            return;
        }
        try {
            final Result result = load(in);
            final Writer output = (out!=null) ? (Writer) new BufferedWriter(new FileWriter(out))
                                              : (Writer) new OutputStreamWriter(System.out);
            result.write(output);
            output.close();
        } catch (IOException exception) {
            System.out.flush();
            System.err.print(Utilities.getShortClassName(exception));
            final String message=exception.getLocalizedMessage();
            if (message != null) {
                System.err.print(": ");
                System.err.print(message);
            }
            System.err.println();
        }
    }
}
