/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2003 Institut de Recherche pour le Développement
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
package fr.ird.database.sample;

// J2SE
import java.awt.Font;
import java.awt.Shape;
import java.awt.Paint;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.BorderLayout;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.imageio.ImageIO;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.File;

// OpenGIS
import org.opengis.referencing.operation.TransformException;

// Geotools
import org.geotools.gui.swing.MapPane;
import org.geotools.gui.swing.ColorBar;
import org.geotools.renderer.j2d.Renderer;
import org.geotools.renderer.j2d.RenderedLayer;
import org.geotools.renderer.j2d.RenderedGeometries;
import org.geotools.renderer.j2d.RenderedGridCoverage;
import org.geotools.renderer.j2d.RenderingContext;
import org.geotools.renderer.geom.GeometryCollection;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.gc.GridCoverage;
import org.geotools.gc.GridGeometry;
import org.geotools.pt.Envelope;
import org.geotools.resources.Arguments;

// Seagis
import fr.ird.resources.XArray;
import fr.ird.resources.Utilities;
import fr.ird.io.map.GEBCOFactory;


/**
 * Affine une carte de la densité des données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class SampleDensity extends RenderedLayer {
    /** Largeur par défaut */
    private static final int WIDTH = 500;

    /** Hauteur par défaut */
    private static final int HEIGHT = 500;

    /**
     * Périphérique de sortie standard.
     */
    private static PrintWriter out;

    /**
     * La légende à afficher (habituellement le mois),
     * ou <code>null</code> si aucune.
     */
    private final String legend;

    /**
     * Construit une couche par défaut.
     *
     * @param month Le mois pour lequel on veut les données (de 1 à 12),
     *              ou 0 pour prendre tous les mois.
     */
    public SampleDensity(final int month) throws SQLException {
        super(GeographicCoordinateSystem.WGS84);
        if (month != 0) {
            legend = new SimpleDateFormat("MMMM").format(new Date(100, month-1, 15));
        } else {
            legend = null;
        }
        setZOrder(+1); // Dessine par-dessus l'image de densité.
    }

    /**
     * Dessine les marques et la légende.
     */
    protected void paint(final RenderingContext context) throws TransformException {
        if (legend != null) {
            final Graphics2D graphics = context.getGraphics();
            final Font font = graphics.getFont();
            context.setCoordinateSystem(context.textCS);
            graphics.setColor(Color.BLACK);
            graphics.setFont(font.deriveFont(Font.BOLD, 20));
            graphics.drawString(legend, WIDTH-100, HEIGHT-20);
            context.setCoordinateSystem(context.mapCS);
        }
    }

    /**
     * Retourne une couverture contenant la densité d'échantillonage.
     *
     * @param bounds Coordonnées géographiques.
     * @param month  Le mois pour lequel on veut les données (de 1 à 12),
     *               ou 0 pour prendre tous les mois.
     */
    private static GridCoverage getDensityCoverage(final Rectangle2D bounds, final int month)
            throws IOException, SQLException
    {
        final StringBuffer query = new StringBuffer("SELECT x, y FROM [Présences par catégories] WHERE visite<>0");
        if (month != 0) {
            query.append(" AND MONTH(date)=");
            query.append(month);
        }
        final BufferedImage image = new BufferedImage((int)Math.round(bounds.getWidth())  *2,
                                                      (int)Math.round(bounds.getHeight()) *2,
                                                      BufferedImage.TYPE_BYTE_INDEXED,
                                                      Utilities.getPaletteFactory().getIndexColorModel("white-cyan-red"));
        final int    width   = image.getWidth();
        final int    height  = image.getHeight();
        final int    xmin    = image.getMinX();
        final int    ymin    = image.getMinY();
        final int    xmax    = width  + xmin;
        final int    ymax    = height + ymin;
        final double scaleX  = width  / bounds.getWidth();
        final double scaleY  = height / bounds.getHeight();
        final double xoffset = bounds.getMinX() + xmin/scaleX;
        final double yoffset = bounds.getMinY() + ymin/scaleY;
        final int[]  count   = new int[width*height];
        int max = 0;
        final Connection connection = DriverManager.getConnection("jdbc:odbc:SEAS-Sennes");
        try {
            final Statement statement = connection.createStatement();
            final ResultSet results = statement.executeQuery(query.toString());
            while (results.next()) {
                final int x = (int)Math.floor((results.getDouble(1)-xoffset) * scaleX);
                final int y = (int)Math.floor((results.getDouble(2)-yoffset) * scaleY);
                if (x>=xmin && x<xmax && y>=ymin && y<ymax) {
                    final int c = ++count[x + width*y];
                    if (c > max) {
                        max = c;
                    }
                }
            }
            results.close();
            statement.close();
        } finally {
            connection.close();
        }
        out.print("Densité maximale: "); out.println(max);
        final WritableRaster raster = image.getRaster();
        int index = 0;
        final int scale = 1;
        for (int y=ymax; --y>=ymin;) {
            for (int x=xmin; x<xmax; x++) {
                // Copie les valeurs en arrondissant vers le haut.
                raster.setSample(x, y, 0, Math.min((count[index++]+(scale-1))/scale, 255));
            }
        }
        return new GridCoverage("Densité d'échantillonage", image,
                                GeographicCoordinateSystem.WGS84, new Envelope(bounds));
    }

    /**
     * Affiche la carte de distribution des données.
     * Les arguments sur la ligne de commandes peuvent être:
     *
     * -mois=[1-12]   (optionel) Le mois des données à utiliser.
     */
    public static void main(final String[] args) throws Exception {
        final Arguments arguments = new Arguments(args);
        final Integer    monthObj = arguments.getOptionalInteger("-mois");
        final int           month = monthObj!=null ? monthObj.intValue() : 0;
        arguments.getRemainingArguments(0);
        out = arguments.out;

        final Driver driver = (Driver)Class.forName("sun.jdbc.odbc.JdbcOdbcDriver").newInstance();
        final MapPane           mapPane = new MapPane(GeographicCoordinateSystem.WGS84);
        final GeometryCollection  coast = new GEBCOFactory("Océan_Indien").get(0);
        final GridCoverage gridCoverage = getDensityCoverage(coast.getBounds(), month);
        // Note: on veut réellement getBounds() plutôt que getBounds2D() sur la ligne précédente,
        //       afin d'arrondir les coordonnées au degré près.
        final Renderer renderer = mapPane.getRenderer();
        renderer.addLayer(new RenderedGridCoverage(gridCoverage));
        renderer.addLayer(new RenderedGeometries(coast));
        renderer.addLayer(new SampleDensity(month));
        mapPane.setBackground(Color.WHITE);

        final BufferedImage    image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_BGR);
        final Graphics2D    graphics = image.createGraphics();
        final Rectangle   targetRect = new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());
        final Rectangle2D sourceRect = coast.getBounds2D();
        final GridGeometry  geometry = new GridGeometry(targetRect, sourceRect);
        graphics.setColor(Color.WHITE);
        graphics.fill(targetRect);
        renderer.paint(graphics, targetRect, (AffineTransform)(geometry.getGridToCoordinateSystem2D().inverse()), true);
        graphics.dispose();
        ImageIO.write(image, "png", new File("P:\\Distribution des données.png"));

        final JPanel panel = new JPanel(new BorderLayout());
        panel.add(mapPane.createScrollPane(), BorderLayout.CENTER);
        panel.add(new ColorBar(gridCoverage), BorderLayout.SOUTH);

        final JFrame frame = new JFrame("Distribution des pêches");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.setSize(WIDTH, HEIGHT);
        frame.pack();
        frame.show();
    }
}
