/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2002 Institut de Recherche pour le Développement
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
package fr.ird.io.n1b;

// Collections
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

// Entrés/sorties
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.EOFException;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.FileNotFoundException;

// Interprétation des chaînes de caractères
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.geotools.io.LineFormat;

// Coordonnées et unités
import org.geotools.units.Unit;
import org.geotools.pt.CoordinatePoint;

// Systèmes de coordonnées
import org.geotools.cs.PrimeMeridian;
import org.geotools.cs.HorizontalDatum;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.GeocentricCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;

// Transformations de coordonnées
import org.geotools.ct.MathTransform;
import org.geotools.ct.TransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.ct.CoordinateTransformationFactory;


/**
 * Bulletin contenant les positions réelles d'un satellite sur une certaine durée.
 * Un bulletin d'orbite couvre 36h depuis Oh du jour J. Les heures sont exprimées
 * dans le fuseau horaire "Europe/Paris".
 * <br><br>
 * Les données sont lues à partir d'un fichier de CLS en appelant la méthode
 * {@link #load}. Le nom du fichier est automatiquement déterminé à partir de
 * la date spécifiée en argument. Le fichier disponible à un jour J s'appelle
 * <code>"SATPOS_NOAAXX_AAAAMMJJ.TXT"</code>, où :
 * <UL>
 *  <LI>NOAAXX est le nom du satellite. XX represente le numero de satellite.</LI>
 *  <LI>AAAAMMJJ est la date calendaire du jour J.</LI>
 * </UL>
 * <br><br>
 * Les fichiers de CLS contiennent les coordonnées et la vitesse du satellite exprimées
 * dans un système de coordonnées géocentriques (c'est-à-dire un système de coordonnées
 * cartésien centré sur la Terre). Cette class fournit des méthodes pour obtenir les
 * coordonnées géocentriques du satellite à une date quelconque, ou les données géographiques
 * exprimées selon l'ellipsoïde WGS84. Cette classe se charge d'effectuer les transformations
 * de coordonnées nécessaires.
 * <br><br>
 * <strong>Note :</strong><BR>
 * <UL>
 *  <LI>L'horloge de bord du satellite fournie une date décalee par rapport au temps UTC.
 *  Cette horloge est recalée régulierement (environ tous les 3 mois). Il faudrait vérifier
 *  s'il faut réaliser une correction de la date de bord avant d'exploiter celle-ci dans les
 *  calculs.</LI>
 *  <LI>Realiser des tests plus approfondis.</LI>
 * </UL>
 * 
 * @version $Id$
 * @author Remi EVE
 * @author Martin Desruisseaux
 */
final class Bulletin 
{       
    /**
     * Liste des méta-informations présentes dans l'entête, sous forme de paires
     * "Nom de la propriété ({@link String}) - Classe de la propriété ({@link Class})".
     * Les éléments de cette liste doivent obligatoirement être dans le même ordre que
     * l'ordre dans lequel ils apparaissent dans l'en-tête.
     */
    private static final Object[] METAINF = new Object[]
    {
        "satellite name",    String.class,  // 02: (tvNomSat)     Nom du satellite NOAA.
        "station name",      String.class,  // 03: (tvSpGrsnam)   Nom de la station de reception.
        "start time",        Date  .class,  // 04: (tvChStDate)   Date de début des éphémérides.
        "duration",          Number.class,  // 05: (dvSpNbDays)   Durée des éphémérides en jours.
        "interval",          Number.class,  // 06: (dvSpDt)       Pas de tabulation en secondes.
        "origin",            String.class,  // 07: (tvSpBuTyp)    Origine du bulletin.
        "criterion",         Number.class,  // 08: (jvSpBulsear)  Critère de recherche du bulletin.
        "filename",          String.class,  // 09: (tvSpBulfilna) Nom du fichier TBUS.
        "epoch",             Number.class,  // 10: (tvChEpoch)    Epoque du bulletin TBUS (jour julien).
        "semi major",        Number.class,  // 11: (dvSpSemiAxis) Demi grand axe (km).
        "eccentricity",      Number.class,  // 12: (dvSpEccent)   Excentricité.
        "inclination",       Number.class,  // 13: (dvSpInclin)   Inclinaison (radians).
        "perigee",           Number.class,  // 14: (dvSpArgper)   Argument du perigée (radians).
        "ascension",         Number.class,  // 15: (dvSpRigasc)   Ascension droite (radians).
        "anomaly",           Number.class,  // 16: (dvSpMeanano)  Anomalie moyenne.
        "position", CoordinatePoint.class,  // 17: (dvSpPosition) Position (x,y,z) du satellite.
        "velocity", CoordinatePoint.class,  // 18: (dvSpVelocity) Vitesse  (u,v,x) du satellite.
        "station latitude",  Number.class,  // 19: (dvSpLatstat)
        "station longitude", Number.class,  // 20: (dvSpLonstat)
        "station altitude",  Number.class,  // 21: (dvSpAltstat)
        "site",              Number.class   // 22: (dvSpSitestat)
    };

    /**
     * Le satellite de ce bulletin.
     */
    private final Satellite satellite;

    /**
     * Fuseau horaire des bulletin.
     */
    private final TimeZone timezone = TimeZone.getTimeZone("Europe/Paris");

    /**
     * Les informations contenus dans l'en-tête.
     */
    private final Map<String,Object> header = new HashMap<String,Object>();

    /**
     * Liste de chaque lignes constituant les éphémérides.
     */
    private final List<EpheremisRecord> epheremis = new ArrayList<EpheremisRecord>();

    /** 
     * Transformation permettant de passer du système de coordonnées
     * géocentrique vers le système géographique.
     */
    private final CoordinateTransformation transformation;

    /**
     * Construit un nouveau bulletin
     * Initialisation avec le nom du fichier contenant les orbites corriges.
     *
     * @param satellite numero identifiant le satellite.
     * @param date date du bulletin a 00h00m 0s 0ms.
     * @exception FileNotFoundException le fichier est introuvable.
     * @exception IOException un probleme de lecture du fichier est survenu.
     * @exception ParseException un probleme de formatage d'une date.
     */
    public Bulletin(final Satellite satellite)
    {
        this.satellite = satellite;
        /*
         * Le système de coordonnées géocentrique. Le méridien d'origine est celui de Greenwich
         * et les unités sont les <strong>kilomètres</strong>. L'axe des <var>x</var> pointe vers
         * le méridient de Greenwich, L'axe des <var>y</var> pointe l'est et l'axe des <var>z</var>
         * pointe vers nord.
         */
        final CoordinateSystem sourceCS = new GeocentricCoordinateSystem
              ("Géocentrique (km)", Unit.KILOMETRE, HorizontalDatum.WGS84, PrimeMeridian.GREENWICH);
        /*
         * Le système de coordonnées géographique (longitude / latitude / altitude)
         */
        final CoordinateSystem targetCS = CompoundCoordinateSystem.WGS84;
        /*
         * Construit la transformation des coordonnées géocentriques vers les coordonnées
         * géographiques. Cette construction ne devrait jamais échouer, puisque nous avons
         * définis des systèmes de coordonnées bien connus.
         */
        try 
        {
            CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();
            transformation = factory.createFromCoordinateSystems(sourceCS, targetCS);
        }
        catch (CannotCreateTransformException exception)
        {
            throw new AssertionError(exception);
        }                 
    }

    /**
     * Retourne le nom de fichier des éphérémides à la
     * date spécifiée pour le satellite de ce bulletin.
     *
     * @param  date La date du bulletin dériré.
     * @return Le nom de fichier du bulletin.
     */
    private String getFilename(final Date date)
    {
        final DateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.FRANCE);
        format.setTimeZone(timezone);
        final StringBuffer buffer = new StringBuffer("satpos_");
        buffer.append(satellite.getCodeName());
        buffer.append('_');
        format.format(date, buffer, new FieldPosition(0));
        buffer.append(".txt");
        return buffer.toString();
    }

    /**
     * Procède à la lecture des données du bulletin. Ces données comprennent un en-tête
     * suivit d'une série d'enregistrements qui constituent les éphérémides.
     *
     * @param  directory Le répertoire de base des fichiers à lire.
     * @date   La date des éphérémides désirés. Le nom du fichier sera construit à partir
     *         de cette date et du satellite spécifié au constructeur. Par exemple pour le
     *         satelitte NOAA 16 et la date du 22 février 2002, le nom du fichier sera
     *         <code>"satpos_noaa16_20020222"</code>.
     * @throws IOException si la lecture a échouée.
     */
    public void load(final File directory, final Date date) throws IOException
    {
        final String filename = getFilename(date);
        final LineNumberReader in = new LineNumberReader(new FileReader(new File(directory, filename)));
        in.setLineNumber(1);
        load(in, filename);
        in.close();
    }

    /**
     * Procède à la lecture des données à partir du flot spécifié. Le flot ne sera
     * pas fermé après la lecture; le fermer est la responsabilité de l'appellant.
     *
     * @param  in Le flot d'entré à utiliser.
     * @param  filename Le nom du fichier. Cette information est à titre informatif
     *         seulement. Elle est utilisée en cas d'erreur pour produire un message.
     * @throws IOException si la lecture a échouée.
     */
    private void load(final LineNumberReader in, final String filename) throws IOException
    {
        header.clear();
        epheremis.clear();
        final Locale locale             = Locale.US;
        final DateFormat     dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
        final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        final LineFormat     lineFormat = new LineFormat(numberFormat);
        final int dimension = transformation.getSourceCS().getDimension();
        dateFormat.setTimeZone(timezone);
        try
        {
            /*
             * Procède à la lecture de l'en-tête. Les valeurs sont décodées en tant que date,
             * nombre ou simple chaîne de caractères en fonction de la classe attendue, puis
             * mémorisées dans un objet Map. Si une ligne est vide, la valeur 'null' est tout
             * de même mémorisé afin de signifier qu'une valeur était attendue mais n'a pas
             * été trouvée.
             */
            for (int i=0; i<METAINF.length; i+=2)
            {
                String line;
                do
                {
                    line = in.readLine();
                    if (line == null)
                    {
                        throw new EOFException();
                    }
                    line = line.trim();
                }
                while (line.length()!=0 && line.charAt(0)=='#');
                final String  name = (String) METAINF[i+0];
                final Class classe =  (Class) METAINF[i+1];
                final Object value;
                if (line.length() == 0)
                {
                    value = null;
                }
                else if (Date.class.isAssignableFrom(classe))
                {
                    value = dateFormat.parse(line);
                }
                else if (Number.class.isAssignableFrom(classe))
                {
                    value = numberFormat.parse(line);
                }
                else if (CoordinatePoint.class.isAssignableFrom(classe))
                {
                    lineFormat.setLine(line);
                    value = new CoordinatePoint(lineFormat.getValues(new double[dimension]));
                }
                else if (CharSequence.class.isAssignableFrom(classe))
                {
                    value = line;
                }
                else
                {
                    // Should not happen
                    throw new ClassCastException(classe.getName());
                }
                header.put(name, value);
            }
            /*
             * Procède à la lecture des données (éphérémides) jusqu'à la fin du fichier.
             * Les lignes vierges seront ignorées, ou qu'elles soient (au début ou à la
             * fin du fichier). Les données écrites dans le fichier doivent obligatoirement
             * apparaître dans l'ordre attendu par le constructeur de EpheremisRecord.
             */
            String line;
            double[] data = null;
            while ((line = in.readLine()) != null)
            {
                if (lineFormat.setLine(line) != 0)
                {
                    data = lineFormat.getValues(data);
                    epheremis.add(new EpheremisRecord(data));
                }
            }
        }
        catch (ParseException exception)
        {
            final IOException e = new IOException("Erreur à la ligne "+in.getLineNumber()+
                                                  "du fichier \""+filename+"\".");
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Retourne les coordonnées géocentriques du satellite à la date spécifiée.
     * Ce calcul fait intervenir les positions et les vitesses du satellite avant
     * et après la date spécifiée. Nous supposons que pendant ce cours interval de
     * temps (de l'ordre de 2 minutes), l'<em>accélération</em> du satellite est
     * constante.
     *
     * @param  date Date désirée de la coordonnée.
     * @return Coordonnées géocentriques du satellite à la date spécifiée.
     */
    public CoordinatePoint getGeocentricCoordinate(final Date date)
    {
        long time = date.getTime();       // Obtient la date et heure UTC.
        time += timezone.getOffset(time); // Conversion en heure locale.
        time %= (24L*60*60*1000);         // Elimine le jour; ne retient que l'heure.
        final int row = (int) (time / EpheremisRecord.TIME_INTERVAL);
        time -= (row * (long)EpheremisRecord.TIME_INTERVAL); // Temps par rapport à l'éphérémide.
        final EpheremisRecord e0 = epheremis.get(row);
        final CoordinatePoint p0 = e0.getPosition();
        if (time == 0)
        {
            // La date et heure demandée est exactement
            // la date et heure d'une des éphérémides.
            return p0;
        }
        final EpheremisRecord e1 = epheremis.get(row+1);
        final CoordinatePoint p1 = e1.getPosition();
        final double[] v0 = e0.getSpeed();
        final double[] v1 = e1.getSpeed();
        final double[] x0 = p0.ord;
        final double[] x1 = p1.ord;
        final double dt = EpheremisRecord.TIME_INTERVAL / 1000.0;
        final double  t = time / 1000.0;
        // Note: on exprime les temps 't' et 'dt' en secondes plutôt qu'en
        //       millisecondes, parce que les vitesses sont en km/s.
        for (int i=0; i<x0.length; i++)
        {
            // Calcul intégral: x = x0 + v0*t + 0.5*at²,
            // où 'a' est l'accélération que l'on suppose constante.
            final double acceleration = (v1[i] - v0[i]) / dt;
            x0[i] += (v0[i] + 0.5*acceleration*t) * t;
        }
        // Nous venons de changer le tableau x0, qui appartenait au
        // point p0. Nous pouvons donc retourner ce point directement.
        return p0;
    }

    /**
     * Retourne les coordonnées géographiques du satellite à la date spécifiée.
     * Cette méthode obtient les coordonnées géocentriques et les transforme en
     * coordonnées géographiques.
     *
     * @param  date Date désirée de la coordonnée.
     * @return Coordonnées géographiques du satellite à la date spécifiée.
     */    
    public CoordinatePoint getGeographicCoordinate(final Date date)
    {
        try
        {
            final CoordinatePoint point = getGeocentricCoordinate(date);
            transformation.getMathTransform().transform(point, point);
            return point;
        }
        catch (TransformException exception)
        {
            // Should not happen
            IllegalStateException e = new IllegalStateException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }       
    }
}
