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
package fr.ird.io.hdf4;

// Entr�s/sorties
import java.io.File;

// Lecture HDF
import ncsa.hdf.hdflib.HDFLibrary;
import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFNativeData;

// Tableaux
import java.lang.reflect.Array;

// Divers
import fr.ird.util.XArray;


/**
 * Interpr�teur d'un fichier HDF. Cette classe utilise du code natif (via la biblioth�que
 * <code>ncsa.hdf.hdflib</code>) pour extraire une image d'un fichier HDF.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class Parser
{
    /**
     * Num�ro identifiant le fichier ouvert.
     */
    private final int sdID;

    /**
     * Indique si le fichier a �t� ferm�e.   Cette information
     * sert simplement � �viter de fermer un fichier deux fois
     * si {@link #close} est appel�e plus d'une fois.
     */
    private boolean closed;

    /**
     * Construit un objet qui lira le fichier HDF sp�cifi�.
     *
     * @param filepath Nom et chemin du fichier � ouvrir.
     * @throws HDFException si l'ouverture du fichier a �chou�e.
     */
    public Parser(final File filepath) throws HDFException
    {
        /*
         * Open the HDF input file and initiate the SD interface.
         */
        sdID = HDFLibrary.SDstart(filepath.getPath(), HDFConstants.DFACC_RDONLY);
    }

    /**
     * Retourne un des attributs globaux sous forme de cha�ne de caract�res.
     * Par exemple l'attribut "ShortName" devrait retourner "QSCATL2B" pour
     * les fichiers "QuikScat Level 2B".
     *
     * @param  name Nom de l'attribut d�sir�.
     * @return Valeur de l'attribut demand�.
     * @throws HDFException si la lecture a �chou�e.
     */
    protected final String getString(final String name) throws HDFException
    {
        final int index=HDFLibrary.SDfindattr(sdID, name);
        final String[] names=new String[] {" "};
        final int[] argv=new int[2];
        check(HDFLibrary.SDattrinfo(sdID, index, names, argv));
        final byte[] data=new byte[argv[1]];
        check(HDFLibrary.SDreadattr(sdID, index, data));
        final String lines=new String(data);
        return lines.substring(lines.indexOf('\n', lines.indexOf('\n')+1)+1).trim();
        // Note: 'indexOf' retourne -1 si le caract�re n'a pas �t� trouv�.
        //       Dans ce cas, 'indexOf(...)+1' donne 0, ce qui est correct.
        //       Ce code a pour but de retourner la deuxi�me ligne seulement.
    }

    /**
     * Retourne la longueur d'un tableau
     * des dimensions sp�cifi�es.
     */
    private static final int getLength(final int[] size)
    {
        int length=1;
        for (int i=size.length; --i>=0;)
            length*=size[i];
        return length;
    }

    /**
     * Returns the number of datasets.
     * @throws HDFException if this information can't be fetched.
     */
    public synchronized int getDataSetCount() throws HDFException
    {
        final int out[] = new int[2];
        check(HDFLibrary.SDfileinfo(sdID, out));
        return out[0];
    }

    /**
     * Retourne des informations sur un ensemble de donn�es.
     *
     * @param  index Index de l'ensemble de donn�es. Cet index va
     *         de 0 jusqu'� {@link #getDataSetCount} exclusivement.
     * @return L'ensemble de donn�es demand�.
     * @throws HDFException si l'information n'a pas pu �tre obtenue.
     */
    public synchronized DataSet getDataSet(final int index) throws HDFException
    {
        /*
         * Get the name, datatype and size.
         */
        final int sdsID=HDFLibrary.SDselect(sdID, index);
        final String[] name = new String[] {" "};
        final int[]    argv = new int[ 3];
              int[]    size = new int[16];
        check(HDFLibrary.SDgetinfo(sdsID, name, size, argv));
        size = XArray.resize(size, argv[0]);
        final int length = getLength(size);
        final int dataType = argv[1];
        /*
         * Get scaling and offset factors.
         */
        final double calibration[]=new double[4];
        final int[] nonCalibratedType=new int[1];
        check(HDFLibrary.SDgetcal(sdsID, calibration, nonCalibratedType));
        final double scale  = calibration[0];
        final double offset = calibration[2];
        /*
         * Read the data and close the dataset.
         */
        final int[]  start = new int[2];
        final int[] stride = null;
        final byte[] bytes = new byte[HDFConstants.getTypeSize(dataType)*length];
        if (bytes.length==0) throw new HDFException("Unknow datatype");
        check(HDFLibrary.SDreaddata(sdsID, start, stride, size, bytes));
        check(HDFLibrary.SDendaccess(sdsID));
        /*
         * Get the QualityCheck object and return the dataset.
         */
        final QualityCheck qualityCheck = getQualityCheck(name[0]);
        switch (dataType)
        {
            case HDFConstants.DFNT_INT8:   return new DataSet.Byte  (name[0], size,                                      bytes , scale, offset, qualityCheck);
            case HDFConstants.DFNT_INT16:  return new DataSet.Short (name[0], size, HDFNativeData.byteToShort(0, length, bytes), scale, offset, qualityCheck);
            case HDFConstants.DFNT_UINT16: return new DataSet.UShort(name[0], size, HDFNativeData.byteToShort(0, length, bytes), scale, offset, qualityCheck);
            default:                       throw new HDFException("Unsupported datatype: "+HDFConstants.getType(dataType));
        }
    }

    /**
     * Retourne des informations sur un ensemble de donn�es.
     *
     * @param  name Nom de l'ensemble de donn�es.
     * @return L'ensemble de donn�es demand�.
     * @throws HDFException si l'information n'a pas pu �tre obtenue.
     */
    public synchronized DataSet getDataSet(final String name) throws HDFException
    {return getDataSet(HDFLibrary.SDnametoindex(sdID, name));}

    /**
     * Retourne un objet charg� de v�rifier la qualit� des donn�es. L'impl�mentation
     * par d�faut retourne toujours <code>null</code>. Les classes d�riv�es devraient
     * red�finir cette m�thode pour retourner un v�rificateur appropri� pour le jeu
     * de donn�es <code>dataset</code> sp�cifi�.
     *
     * @param  dataset Nom de la s�rie de donn�es demand�e.
     * @return Un objet qui v�rifiera la qualit� des donn�es de la s�rie,
     *         <code>dataset</code>, ou <code>null</code> si cette s�rie
     *         n'a pas besoin que l'on v�rifie sa qualit�.
     * @throws HDFException si l'objet {@link QualityCheck} n'a pas pu �tre obtenu.
     */
    protected QualityCheck getQualityCheck(final String dataset) throws HDFException
    {return null;}

    /**
     * Ferme le fichier.
     *
     * @throws HDFException si la fermeture a �chou�e.
     */
    public synchronized void close() throws HDFException
    {
        if (!closed)
        {
            check(HDFLibrary.SDend(sdID));
            closed=true;
        }
    }

    /**
     * Lib�re les ressources utilis� par cet objet. Cette
     * m�thode appele {@link #close} si ce n'�tait pas d�j� fait.
     *
     * @throws Throwable si la fermeture du fichier a �chou�e.
     */
    protected void finalize() throws Throwable
    {
        close();
        super.finalize();
    }

    /**
     * V�rifie si un appel � une m�thode de la biblioth�que {@link HDFLibrary} a r�ussi.
     * @throws HDFException si <code>result</code> est <code>false</code>.
     */
    private static void check(final boolean result) throws HDFException
    {if (!result) throw new HDFException();}
}
