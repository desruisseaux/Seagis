/*
 * Map and oceanographical data visualisation
 * Copyright (C) 1998 University Corporation for Atmospheric Research (Unidata)
 *               1998 Bill Hibbard & al. (VisAD)
 *               1999 P�ches et Oc�ans Canada
 *               2000 Institut de Recherche pour le D�veloppement
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Library General Public
 *    License as published by the Free Software Foundation; either
 *    version 2 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *    Library General Public License for more details (http://www.gnu.org/).
 *
 *
 * Contacts:
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *
 *    This package is inspired from the units package of VisAD.
 *    Unidata and Visad's work is fully acknowledged here.
 *
 *                   THIS IS A TEMPORARY CLASS
 *
 *    This is a placeholder for future <code>Unit</code> class.
 *    This skeleton will be removed when the real classes from
 *    JSR-108: Units specification will be publicly available.
 */
package javax.units;

// Entr�s/sorties
import java.io.ObjectStreamException;


/**
 * Repr�sente une transformation entre deux unit�s
 * identiques. Cette transformation ne fait rien.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
final class IdentityTransform extends UnitTransform
{
    /**
     * Construit un objet qui aura la charge de convertir
     * des donn�es exprim�es selon les unit�s sp�cifi�es.
     */
    private IdentityTransform(final Unit fromUnit, final Unit toUnit)
    {super(fromUnit, toUnit);}

    /**
     * Retourne une transformation identit�e si <code>fromUnit</code> est identique
     * � <code>this</code>. Cette m�thode sert � faciliter les impl�mentations des
     * m�thodes <code>get[Inverse]Transform(Unit)</code>.
     *
     * @throws UnitException Si <code>fromUnit</code> n'est pas identique � <code>this</code>.
     */
    public static UnitTransform getInstance(final Unit fromUnit, final Unit toUnit) throws UnitException
    {
        if (toUnit.equalsIgnoreSymbol(fromUnit))
            return new IdentityTransform(fromUnit, toUnit).intern();
        else throw toUnit.incompatibleUnitsException(fromUnit);
    }

    /**
     * Retourne <code>value</code> sans rien faire.
     */
    public double convert(final double value)
    {return value;}

    /**
     * Ne fait rien.
     */
    public void convert(final double[] values)
    {}

    /**
     * Ne fait rien.
     */
    public void convert(final float[] values)
    {}

    /**
     * Retourne <code>value</code> sans rien faire.
     */
    public double inverseConvert(final double value)
    {return value;}

    /**
     * Ne fait rien.
     */
    public void inverseConvert(final double[] values)
    {}

    /**
     * Ne fait rien.
     */
    public void inverseConvert(final float[] values)
    {}

    /**
     * Apr�s la lecture d'une transformation, v�rifie si cette transformation appara�t
     * d�j� dans la banque des unit�s {@link #pool}. Si oui, l'exemplaire de la banque
     * sera retourn� plut�t que de garder inutilement la transformation courante comme
     * copie.
     */
    private Object readResolve() throws ObjectStreamException
    {return intern();}
}