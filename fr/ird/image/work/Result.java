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
 * Résultat d'une opération. Le contenu de cet
 * objet est laissé libre aux classes dérivées.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class Result implements Serializable {
    /**
     * Numéro de série (pour compatibilité entre différentes versions).
     */
    private static final long serialVersionUID = 4059406258349515732L;

    /**
     * Numéros ID des images qui ont été
     * utilisées pour construite cet objet.
     */
    private int[] imageIDs;

    /**
     * Construit un objet par défaut.
     */
    public Result() {
    }

    /**
     * Lit un objet qui avait précédemment été enregistré
     * en binaire dans le fichier spécifié.
     *
     * @param  file Fichier à lire.
     * @return Objet lu dans le fichiet spécifié.
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
     * Enregistre cet objet en binaire dans le fichier spécifié.
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
     * Retourne les données sous forme d'une tuile d'une image. Si les données ne peuvent pas
     * être représentées sous forme d'image, alors cette méthode retourne <code>null</code>.
     * La tuile retournée devrait contenir les valeurs géophysiques de <code>this</code>, et
     * non des valeurs de pixels. Cela signifie que cette tuile utilisera la plupart du temps
     * des données du type {@link DataBuffer#FLOAT}.
     * <br><br>
     * L'implémentation par défaut retourne toujours <code>null</code>.
     */
    public WritableRaster getRaster() {
        return null;
    }

    /**
     * Retourne les données sous forme d'une image. Si les données ne peuvent pas être représentées
     * sous forme d'image, alors cette méthode retourne <code>null</code>. L'implémentation par défaut
     * enveloppe dans une image la tuile retournée par {@link #getRaster}.
     */
    public RenderedImage getImage(final SampleDimension[] bands) {
        final WritableRaster raster = getRaster(); if (raster==null) return null;
        final ColorModel     colors = PlanarImage.createColorModel(raster.getSampleModel());
        final Hashtable  properties = new Hashtable();
        final BufferedImage   image = new BufferedImage(colors, raster, false, properties);
        // TODO: appliquer l'opération GC_SampleTranscoding
        return image;
    }

    /**
     * Ecrit le contenu de cet objet sous forme de texte.
     *
     * @param  file Fichier dans lequel écrire une description de cet objet.
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    public synchronized void write(final File file) throws IOException {
        final Writer output = new BufferedWriter(new FileWriter(file));
        write(output);
        output.close();
    }

    /**
     * Ecrit le contenu de cet objet sous forme de texte. L'implémentation
     * par défaut écrit la liste des numéros ID des images qui ont été
     * utilisées pour ce calcul.
     *
     * @param  output Flot dans lequel écrire une description de cet objet.
     * @throws IOException si une erreur est survenue lors de l'écriture.
     */
    public synchronized void write(final Writer output) throws IOException {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        output.write("ID des images utilisées:");
        output.write(lineSeparator);
        if (imageIDs.length != 0) {
            for (int i=0; i<imageIDs.length; i++) {
                output.write(String.valueOf(imageIDs[i]));
                output.write(lineSeparator);
            }
        }
    }

    /**
     * Retourne le contenu de cet objet sous forme de chaîne de caractères.
     * L'implémentation par défaut appelle {@link #write(Writer)}.
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
     * Ajoute une image à la liste des images qui
     * ont été pris en compte pour la construction
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
     * Indique si l'image spécifiée fait partie des images qui
     * ont été prises en compte dans le calcul des résultats.
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
     * Signale qu'une erreur inatendue est survenue. Cette méthode
     * écrira l'erreur dans un journal de l'application.
     */
    static void unexpectedException(final String className, final String method, final Throwable exception) {
        Utilities.unexpectedException("fr.ird.image.work", className, method, exception);
    }

    /**
     * Affiche sous forme de texte le contenu d'un objet {@link Result}.
     * Cette méthode peut être appelée à partir de la ligne de commande
     * avec deux arguments:
     *
     * <ul>
     *   <li>Le nom du fichier binaire d'entré.</li>
     *   <li>Le nom du fichier texte de sortie. S'il est omis, la
     *       sortie se fera vers le périphérique de sortie standard.</li>
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
                               "  - [input]  est le nom du fichier binaire d'entré.\n"+
                               "  - [output] est le nom du fichier texte de sortie.\n"+
                               "             S'il est omis, la sortie se fera vers\n"+
                               "             le périphérique de sortie standard.");
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
