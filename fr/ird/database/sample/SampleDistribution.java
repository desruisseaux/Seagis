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
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
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
import javax.imageio.ImageIO;
import java.io.File;

// OpenGIS
import org.opengis.referencing.operation.TransformException;

// Geotools
import org.geotools.gui.swing.MapPane;
import org.geotools.renderer.j2d.Renderer;
import org.geotools.renderer.j2d.MarkIterator;
import org.geotools.renderer.j2d.RenderedMarks;
import org.geotools.renderer.j2d.RenderedGeometries;
import org.geotools.renderer.j2d.RenderingContext;
import org.geotools.renderer.geom.GeometryCollection;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.gc.GridGeometry;
import org.geotools.resources.Arguments;

// Seagis
import fr.ird.resources.XArray;
import fr.ird.io.map.GEBCOFactory;


/**
 * Affine une carte de la distribution spatiale des données.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
public class SampleDistribution extends RenderedMarks {
    /** Largeur par défaut */
    private static final int WIDTH = 500;

    /** Hauteur par défaut */
    private static final int HEIGHT = 500;

    /** Constante désignant une prospection sans pêches. */
    private static final byte NONE = 0;

    /** Constante désignant une pêche de petit poissons. */
    private static final byte C1 = 1;

    /** Constante désignant une pêche de gros poissons. */
    private static final byte C3 = 2;

    /** Constante désignant une pêche de petit et gros poissons. */
    private static final byte BOTH = 3;

    /**
     * Couleurs à utiliser pour chaque index de {@link #color}.
     */
    private static final Color[] COLOR_PALETTE = new Color[4];
    {
        COLOR_PALETTE[NONE] = Color.GRAY;
        COLOR_PALETTE[C1  ] = new Color( 51, 102, 255);
        COLOR_PALETTE[C3  ] = new Color(255, 153,   0);
        COLOR_PALETTE[BOTH] = Color.MAGENTA;
    };

    /**
     * La forme géométrique des marques.
     */
    private static /*final*/ Shape MARK;
    {
        final GeneralPath mark = new GeneralPath();
        mark.moveTo(-1,  0);
        mark.lineTo(+1,  0);
        mark.moveTo( 0, -1);
        mark.lineTo( 0, +1);
        MARK = mark;
    }

    /**
     * Coordonnées (x,y) de chaque position.
     */
    private final float[] coords;

    /**
     * Index de la couleur de chaque position.
     * La couleur est puisée dans le tableau {@link #COLOR_PALETTE}.
     */
    private final byte[] colors;

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
    public SampleDistribution(final int month) throws SQLException {
        super(GeographicCoordinateSystem.WGS84);
        final StringBuffer query = new StringBuffer("SELECT x, y, C1, C3 FROM [Présences par catégories] WHERE visite<>0 AND libre<>0");
        if (month != 0) {
            query.append(" AND MONTH(date)=");
            query.append(month);
            legend = new SimpleDateFormat("MMMM").format(new Date(100, month-1, 15));
        } else {
            legend = null;
        }
        byte [] colors = new byte [256];
        float[] coords = new float[colors.length*2];
        int     count  = 0;
        final Connection connection = DriverManager.getConnection("jdbc:odbc:SEAS-Sennes");
        try {
            final Statement statement = connection.createStatement();
            final ResultSet results = statement.executeQuery(query.toString());
            while (results.next()) {
                if (count >= colors.length) {
                    colors = XArray.resize(colors, 2*colors.length);
                    coords = XArray.resize(coords, 2*coords.length);
                }
                coords[count*2 + 0] =      results.getFloat(1);
                coords[count*2 + 1] =      results.getFloat(2);
                colors[count++] = (byte) ((results.getByte (3)!=0 ? C1 : NONE) |
                                          (results.getByte (4)!=0 ? C3 : NONE));
            }
            results.close();
            statement.close();
        } finally {
            connection.close();
        }
        this.coords = coords = XArray.resize(coords, 2*count);
        this.colors = colors = XArray.resize(colors,   count);
        /*
         * Cache les positions qui tombent sur la terre ferme.  Ce n'est pas idéal (on manque de
         * temps pour la finalisation de la thèse), mais quand même partiellement justifié étant
         * donnée que les points sur la terres n'auront pas de données océanographiques
         * correspondantes et ne seront donc pas pris en compte dans les analyses statistiques.
         */
        setZOrder(-1);
    }

    /**
     * Dessine les marques et la légende.
     */
    protected void paint(final RenderingContext context) throws TransformException {
        super.paint(context);
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
     * Retourne un itérateur pour les positions des pêches.
     */
    public MarkIterator getMarkIterator() {
        return new Iterator();
    }

    /**
     * Itérateur des positions des pêches.
     */
    private final class Iterator extends MarkIterator {
        /**
         * L'index de la marque courante.
         */
        private int index = -1;

        /**
         * Retourne la position courante de l'itérateur.
         */
        public int getIteratorPosition() {
            return index;
        }

        /**
         * Place l'itérateur à la position spécifiée.
         */
        public void setIteratorPosition(final int index) throws IllegalArgumentException {
            this.index = index;
        }

        /**
         * Avance l'itérateur à la marque suivante.
         */
        public boolean next() {
            return ++index < colors.length;
        }

        /**
         * Retourne la coordonnée géographique de la marque courante.
         *
         * @task TODO: Vérifier le système de coordonnées, projeter si nécessaire.
         */
        public Point2D position() throws TransformException {
            return new Point2D.Float(coords[index*2], coords[index*2+1]);
        }

        /**
         * Retourne la forme géométrique de la marque.
         */
        public Shape markShape() {
            return MARK;
        }

        /**
         * Retourne la couleur de la marque courante.
         */
        public Paint markPaint() {
            return COLOR_PALETTE[colors[index]];
        }

        /**
         * Dessine la marque.
         */
        protected void paint(final Graphics2D      graphics,
                             final Shape           geographicArea,
                             final Shape           markShape,
                             final RenderedImage   markIcon,
                             final AffineTransform iconXY,
                             final GlyphVector     label,
                             final Point2D.Float   labelXY)
        {
            if (markShape != null) {
                graphics.setPaint(markPaint());
                graphics.draw(markShape);
            }
        }
    }

    /**
     * Affiche la carte de distribution des données.
     * Les arguments sur la ligne de commandes peuvent être:
     *
     * -mois=[1-12]   (optionel) Le mois des données à utiliser.
     */
    public static void main(final String[] args) throws Exception {
        final Arguments arguments = new Arguments(args);
        final Integer       month = arguments.getOptionalInteger("-mois");
        arguments.getRemainingArguments(0);

        final Driver driver = (Driver)Class.forName("sun.jdbc.odbc.JdbcOdbcDriver").newInstance();
        final MapPane          mapPane = new MapPane(GeographicCoordinateSystem.WGS84);
        final GeometryCollection coast = new GEBCOFactory("Océan_Indien").get(0);
        final Renderer        renderer = mapPane.getRenderer();
        renderer.addLayer(new RenderedGeometries(coast));
        renderer.addLayer(new SampleDistribution(month!=null ? month.intValue() : 0));
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

        final JFrame frame = new JFrame("Distribution des pêches");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(mapPane.createScrollPane());
        frame.setSize(WIDTH, HEIGHT);
        frame.pack();
        frame.show();
    }
}
