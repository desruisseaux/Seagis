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
package fr.ird.image.sst;

// Images
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;

// Entrés/sorties
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;

// Formattage de dates et des angles
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.geotools.pt.AngleFormat;
import org.geotools.pt.Longitude;
import org.geotools.pt.Latitude;

// Graphisme
import java.awt.Font;
import java.awt.Color;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;

// Divers
import org.geotools.resources.Arguments;


/**
 * Classe ayant la charge de placer une image de température sur une maquette.
 * Les éléments placées sur la maquette comprendront:
 *
 * <ul>
 *    <li>Une image de température.</li>
 *    <li>La date de l'image de température.</li>
 *    <li>Un grillage par dessus l'image de température.</li>
 * </ul>
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class Publisher
{
    /**
     * Nombre de pixels par degrés sur une image originale de température.
     */
    private static final int PIXELS_BY_DEGREES = 60;

    /**
     * Longitude du coin supérieur droit, en degrés.
     */
    private static final int XMIN = 35;

    /**
     * Latitude du coin supérieur droit, en degrés. Les
     * latitudes négatives correspondent à l'hémisphère
     * sud.
     */
    private static final int YMAX = 5;

    /**
     * Nombre de pixels à laisser entre chaque graduation secondaire.
     */
    private static final int SUB_GRAD_INTERVAL = PIXELS_BY_DEGREES;

    /**
     * Nombre de pixels à laisser entre chaque graduation principale.
     */
    private static final int MAIN_GRAD_INTERVAL = 5*SUB_GRAD_INTERVAL;

    /**
     * Nombre de pixels à donner aux pointillés et aux espaces entre les pointillés.
     */
    private static final float[] DASH_PATTERN = new float[] {1};

    /**
     * Coordonnées en pixels de la région de la maquette dans laquelle il
     * faudra placer la carte de température. La carte sera automatiquement
     * redimmensionnée pour entrer dans cette région.
     */
    private final Rectangle imageArea = new Rectangle(20, 385, 1040, 1040);

    /**
     * Coordonnées en pixels du coin inférieur gauche
     * à partir d'où écrire la date de l'image.
     */
    private final Point dateAnchor = new Point(450, 320);

    /**
     * Formateur à utiliser pour décoder les dates dans les noms de fichiers.
     * Ce patron doit apparaître au début du nom du fichier (après son chemin).
     * Les caractères qui le suivent, comme l'extension, seront ignorés.
     */
    private final DateFormat fileDateFormat;

    /**
     * Formateur à utiliser pour réécrire la date à l'intention du client.
     * Cette date formatée sera écrite dans l'image de destination.
     */
    private final DateFormat imageDateformat;

    /**
     * Objet à utiliser pour formatter les angles.
     */
    private final AngleFormat angleFormat;

    /**
     * Nom du fichier de l'image servant de fond.
     */
    private final String modelPathName;

    /**
     * Construit un formatteur.
     */
    public Publisher(final String modelPathName, final String fileDateFormat, final Locale locale)
    {
        this.modelPathName   = modelPathName;
        this.fileDateFormat  = new SimpleDateFormat(fileDateFormat, locale);
        this.imageDateformat = new SimpleDateFormat("dd MMMM yyyy", locale);
        this.angleFormat     = new AngleFormat     ("D°",           locale);
    }

    /**
     * Construit une image de température.
     *
     * @param  pathname Nom et chemin de l'image de température source.
     * @return Image de température dans sa maquette.
     * @throws IOException si la lecture a échouée.
     */
    public RenderedImage createImage(final File pathname) throws IOException
    {
        final BufferedImage maquette = ImageIO.read(new File(modelPathName));
        final Graphics2D    graphics = maquette.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        //
        // Dessine l'image par-dessus la maquette.
        //
        final RenderedImage image = ImageIO.read(pathname);
        final double scale = Math.min(imageArea.getWidth()/image.getWidth(), imageArea.getHeight()/image.getHeight());
        final AffineTransform transform = new AffineTransform(scale, 0, 0, scale, imageArea.getX(), imageArea.getY());
        graphics.drawRenderedImage(image, transform);
        //
        // Dessine la date à sa position prévue.
        //
        final Date date = fileDateFormat.parse(pathname.getName(), new ParsePosition(0));
        if (date==null)
        {
            System.err.print("Le nom de fichier contient une date non-reconnue:  ");
            System.err.println(pathname.getName());
            System.err.print("Exemple de nom attendu pour la date d'aujourd'hui: ");
            System.err.println(fileDateFormat.format(new Date()));
        }
        else
        {
            date.setTime(date.getTime() - 2L*24*60*60*1000);
            graphics.setColor(Color.black);
            graphics.setFont(new Font("Serif", Font.BOLD, 48));
            graphics.drawString(imageDateformat.format(date), dateAnchor.x, dateAnchor.y);
        }
        //
        // Dessine le quadrillage par dessus l'image.
        //
        final Line2D.Double  line  = new Line2D.Double();
        final Point2D.Double point = new Point2D.Double();
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 12));
        final Stroke normalLine = new BasicStroke(0);
        final Stroke dashedLine = new BasicStroke(0, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, DASH_PATTERN, 0);
        final int xmin = image.getMinX();
        final int xmax = image.getWidth()+xmin;
        final int ymin = image.getMinY();
        final int ymax = image.getHeight()+ymin;
        for (int x=xmin; x<=xmax; x+=SUB_GRAD_INTERVAL)
        {
            final boolean isMainGrad = (x % MAIN_GRAD_INTERVAL)==0;
            point.x=x; point.y=ymin; transform.transform(point, point); line.x1=point.x; line.y1=point.y;
            point.x=x; point.y=ymax; transform.transform(point, point); line.x2=point.x; line.y2=point.y;
            graphics.setStroke(isMainGrad ? normalLine     : dashedLine);
            graphics.setColor (isMainGrad ? Color.darkGray : Color.gray);
            graphics.draw(line);
            if (isMainGrad)
            {
                final double longitude = XMIN + (double)x/PIXELS_BY_DEGREES;
                graphics.setColor(Color.black);
                graphics.drawString(angleFormat.format(new Longitude(longitude)), (float)line.x2-12, (float)line.y2+15);
            }
        }
        for (int y=ymin; y<=ymax; y+=SUB_GRAD_INTERVAL)
        {
            final boolean isMainGrad = (y % MAIN_GRAD_INTERVAL)==0;
            point.x=xmin; point.y=y; transform.transform(point, point); line.x1=point.x; line.y1=point.y;
            point.x=xmax; point.y=y; transform.transform(point, point); line.x2=point.x; line.y2=point.y;
            graphics.setStroke(isMainGrad ? normalLine     : dashedLine);
            graphics.setColor (isMainGrad ? Color.darkGray : Color.gray);
            graphics.draw(line);
            if (isMainGrad)
            {
                final double latitude = YMAX - (double)y/PIXELS_BY_DEGREES;
                graphics.setColor(Color.black);
                graphics.drawString(angleFormat.format(new Latitude(latitude)), (float)line.x2+4, (float)line.y2+4);
            }
        }
        graphics.dispose();
        return maquette;
    }

    /**
     * Construit une carte de température à partir d'une image.
     * Appellez cette méthode sans arguments pour obtenir la liste
     * des options disponibles.
     */
    public static void main(String[] args) throws IOException
    {
        final Arguments arguments = new Arguments(args);
        if (args.length == 0) {
            help(arguments.out);
            return;
        }
        String model   = arguments.getRequiredString("-model");
        String pattern = arguments.getOptionalString("-pattern");
        if (pattern == null) {
            pattern = "'syn5J'yyyy_MM_dd";
        }
        args = arguments.getRemainingArguments(Integer.MAX_VALUE);
        if (args.length == 0)
        {
            help(arguments.out);
            return;
        }
        final Publisher publisher = new Publisher(model, pattern, arguments.locale);
        for (int i=0; i<args.length; i+=2)
        {
            final File  input = new File(args[i+0]);
            final File output = new File(args[i+1]);
            arguments.out.println("Lecture de "+input.getName());
            final RenderedImage image = publisher.createImage(input);
            arguments.out.println("Ecriture de "+output.getName());
            ImageIO.write(image, "png", output);
        }
        arguments.out.close();
    }

    /**
     * Affiche l'aide.
     */
    private static void help(final PrintWriter out)
    {
        out.println("Usage: -model=... -pattern=... [image source] [image destination]");
        out.println();
        out.println("  -model    spécifie le nom du fichier à utiliser comme maquette.");
        out.println("  -pattern  spécifie le codage des dates dans les noms de fichiers.");
        out.println("            La valeur par défaut est \"'syn5J'yyyy_MM_dd\".");
        out.println();
        out.println("Les arguments supplémentaires sont interprétés commme des paires");
        out.println("d'images source et destination. L'exemple suivant publie l'image");
        out.println("\"syn5j2002_08_12\":");
        out.println();
        out.println("java fr.ird.image.sst.Publisher -model=maquette.png syn5j2002_08_12.png out.png");
        out.flush();
    }
}
