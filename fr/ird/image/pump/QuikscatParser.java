/*
 * Remote sensing images: database and visualisation
 * Copyright (C) 2000 Universidad de Las Palmas de Gran Canaria
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
 * Contact: Antonio Ramos
 *          Departamento de Biologia ULPGC
 *          Campus Universitario de Tafira
 *          Edificio de Ciencias Basicas
 *          35017
 *          Las Palmas de Gran Canaria
 *          Spain
 *
 *          mailto:antonio.ramos@biologia.ulpgc.es
 */
package fr.ird.image.pump;

// Entrés/sorties
import java.io.File;
import fr.ird.io.hdf4.Parser;
import fr.ird.io.hdf4.DataSet;
import fr.ird.io.hdf4.QualityCheck;
import ncsa.hdf.hdflib.HDFException;

// Date et heures
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

// Divers
import fr.ird.resources.Resources;
import fr.ird.resources.ResourceKeys;


/**
 * Interpréteur d'un fichier HDF QuikScat.
 *
 * @version $Id$
 * @author Martin Desruisseaux
 */
final class QuikscatParser extends Parser {
    /**
     * Objet ayant la charge de vérifier la qualité des données.
     * Cet objet sera créé la première fois ou il sera nécessaire.
     */
    private transient QualityCheck qualityCheck;

    /**
     * Objet à utiliser pour interpréter des dates au format
     * "yyyy-DDD HH:mm:ss.SSS" UTC. Cet objet sera construit
     * la première fois où il sera nécessaire.
     */
    private transient DateFormat dateFormat;

    /**
     * Construit un objet qui lira le fichier HDF spécifié.
     *
     * @param filepath Nom et chemin du fichier à ouvrir.
     * @throws HDFException si l'ouverture du fichier a échouée.
     */
    public QuikscatParser(final File filepath) throws HDFException {
        super(filepath);
        if (!getString("ShortName").equals("QSCATL2B")) {
            throw new HDFException(Resources.format(ResourceKeys.ERROR_BAD_FILE_FORMAT_$1, filepath.getName()));
        }
    }

    /**
     * Convertit des chaînes de caractères
     * date et heure en objet {@link Date}.
     */
    private Date getDate(final String date, final String hour) throws HDFException {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyyy-DDD HH:mm:ss.SSS", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        try {
            return dateFormat.parse(date+' '+hour);
        } catch (ParseException exception) {
            HDFException e=new HDFException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
    }

    /**
     * Obtient la date de début.
     */
    public Date getStartTime() throws HDFException {
        return getDate(getString("RangeBeginningDate"), getString("RangeBeginningTime"));
    }

    /**
     * Obtient la date de fin.
     */
    public Date getEndTime() throws HDFException {
        return getDate(getString("RangeEndingDate"), getString("RangeEndingTime"));
    }

    /**
     * Retourne un objet chargé de vérifier la qualité des données.
     * Cette méthode est automatiquement appelée la première fois
     * ou un objet {@link QualityCheck} est nécessaire.
     */
    protected QualityCheck getQualityCheck(final String dataset) throws HDFException {
        if (dataset.startsWith("wind")) {
            if (qualityCheck == null) {
                qualityCheck = new QualityCheck(getDataSet("wvc_quality_flag")) {
                    protected boolean accept(final int flag) {
                        return 0==(flag & (0x0001 |   // Bit 00: Not enough good sigma0s available for wind retrieval.
                                           0x0002 |   // Bit 01: Poor azimuth diversity among sigma0s for wind retrieval.
                                           0x0080 |   // Bit 07: Some portion of the wind vector cell is over land.
                                           0x0100 |   // Bit 08: Some portion of the wind vector cell is over ice.
                                           0x0200 |   // Bit 09: Wind retrieval not performed for wind vector cell.
                                           0x2000));  // Bit 13: Rain algorithm detects rain.
                    }
                };
            }
            return qualityCheck;
        }
        else return null;
    }
}
