/*
 * SEAS - Surveillance de l'Environnement Assist�e par Satellites
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 * Contacts:
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement
 *             mailto:seasnet@teledetection.fr
 */
package net.seas.resources;

// Utilitaires
import java.util.Locale;
import java.util.Enumeration;
import java.util.ListResourceBundle;
import java.util.NoSuchElementException;
import java.util.MissingResourceException;

// Formats
import java.text.Format;
import java.text.MessageFormat;

// Entr�s/sorties
import java.io.IOException;
import java.io.DataInputStream;
import java.io.BufferedInputStream;


/**
 * {link java.util.ResourceBundle} implementation using integer key instead of strings.
 * This class make use of {@link MessageFormat} for string formatting.
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public class ResourceBundle extends java.util.ResourceBundle
{
	/**
	 * Longueur maximale des cha�nes de caract�res. Les cha�nes plus longues que
	 * cette longueur seront coup�es afin d'�viter de surcharger les messages.
	 * <code>ResourceBundle</code> tentera de trouver un endroit pas trop mauvais
	 * pour couper une phrase trop longue.
	 */
	private static final int MAX_STRING_LENGTH = 128;

	/**
	 * Nom du fichier contenant les ressources.
	 */
	private final String filename;

	/**
	 * Tableau des valeurs.
	 */
	private final String[] values;

	/**
	 * Construit une table des ressources.
	 * 
	 * @param  filename Nom du fichier binaire contenant les ressources.
	 * @throws IOException si les ressources n'ont pas pu �tre ouvertes.
	 */
	protected ResourceBundle(final String filename) throws IOException
	{
		this.filename=filename;
		final DataInputStream input=new DataInputStream(new BufferedInputStream(getClass().getClassLoader().getResourceAsStream(filename)));
		values = new String[input.readInt()];
		for (int i=0; i<values.length; i++)
		{
			values[i] = input.readUTF();
			if (values[i].length()==0)
				values[i]=null;
		}
		input.close();
	}

	/**
	 * Renvoie un �num�rateur qui balayera toutes
	 * les cl�s que poss�de cette liste de ressources.
	 */
	public final Enumeration getKeys()
	{
		return new Enumeration()
		{
			private int i=0;

			public boolean hasMoreElements()
			{
				while (true)
				{
					if (i>=values.length) return false;
					if (values[i]!=null) return true;
					i++;
				}
			}

			public Object nextElement()
			{
				while (true)
				{
					if (i>=values.length) throw new NoSuchElementException();
					if (values[i]!=null) return String.valueOf(i++);
					i++;
				}
			}
		};
	}

	/**
	 * Renvoie la ressource associ�e � une cl� donn�e. Cette m�thode est d�finie
	 * pour r�pondre aux exigences de la classe {@link java.util.ResourceBundle}
	 * et n'a g�n�ralement pas besoin d'�tre appell�e directement.
	 *
	 * @param  key Cl� d�signant la ressouce d�sir�e (ne doit pas �tre <code>null</code>).
	 * @return La ressource demand�e, ou <code>null</code> si aucune ressource n'est
	 *         d�finie pour cette cl�.
	 */
	protected final synchronized Object handleGetObject(final String key)
	{
		final int keyID;
		try
		{
			keyID=Integer.parseInt(key);
		}
		catch (NumberFormatException exception)
		{
			return null;
		}
		return (keyID>=0 && keyID<values.length) ? values[keyID] : null;
	}

	/**
	 * Renvoie la cha�ne <code>text</code> en s'assurant qu'elle n'aura pas plus
	 * de <code>maxLength</code> caract�res. Si la cha�ne <code>text</code> est
	 * suffisament courte, elle sera retourn�e telle quelle. Sinon, une cha�ne
	 * synth�se sera produite. Par exemple la cha�ne "Cette phrase donn�e en exemple
	 * est beaucoup trop longue" pourra devenir "Cette phrase (...) trop longue".
	 * Cette m�thode est utile pour �crire des phrases d'origine inconnue dans une
	 * bo�te de dialogue.
	 *
	 * @param  text Phrase � raccourcir si elle est trop longue.
	 * @param  maxLength Longueur maximale de la phrase � retourner.
	 * @return La phrase <code>text</code> (sans les espaces au d�but
	 *         et � la fin) si elle a moins de <code>maxLength</code>
	 *         caract�re, ou une phrase synth�se sinon.
	 */
	private static String summarize(String text, int maxLength)
	{
		text=text.trim();
		final int length=text.length();
		if (length<=maxLength) return text;
		/*
		 * On tentera de cr�er une cha�ne dont la moiti� provient du d�but
		 * de 'text' et l'autre moiti� de la fin de 'text'. Entre les deux,
		 * on placera " (...) ". On ajuste la variable 'maxLength' de fa�on
		 * � ce qu'elle refl�te maintenant la longueur maximale de ces moiti�es.
		 */
		maxLength = (maxLength-7) >> 1;
		if (maxLength<=0) return text;
		/*
		 * La partie � ignorer ira de 'break1' jusqu'� 'break2' exclusivement.
		 * On tentera de couper le texte vis-�-vis un espace. Les variables
		 * 'lower' et 'upper' seront les bornes au dela desquelles on renoncera
		 * � couper d'avantage de texte.
		 */
		int break1 = maxLength;
		int break2 = length-maxLength;
		for (final int lower=(maxLength>>1); break1>=lower; break1--)
		{
			if (!Character.isUnicodeIdentifierPart(text.charAt(break1)))
			{
				while (--break1>=lower && !Character.isUnicodeIdentifierPart(text.charAt(break1)));
				break;
			}
		}
		for (final int upper=length-(maxLength>>1); break2<upper; break2++)
		{
			if (!Character.isUnicodeIdentifierPart(text.charAt(break2)))
			{
				while (++break2<upper && !Character.isUnicodeIdentifierPart(text.charAt(break2)));
				break;
			}
		}
		return (text.substring(0,break1+1)+" (...) "+text.substring(break2)).trim();
	}

	/**
	 * Retourne <code>arguments</code> sous forme d'un tableau d'objets. Si <code>arguments</code>
	 * �tait d�j� un tableau,  il peut �tre retourn� tel quel.   S'il n'�tait qu'un objet, il sera
	 * envelopp� dans un tableau de longueur 1. Dans tous les cas, les �l�ments du tableaux seront
	 * v�rifi�s. Ceux qui correspondent � des cha�nes de caract�res d'une longueur sup�rieure �
	 * {@link #MAX_STRING_LENGTH} seront coup�es afin d'�viter de surcharger le message.
	 */
	private static Object[] toArray(final Object arguments)
	{
		Object[] array=(arguments instanceof Object[]) ? (Object[]) arguments : new Object[] {arguments};
		for (int i=0; i<array.length; i++)
		{
			if (array[i] instanceof String)
			{
				final String s0=(String) array[i];
				final String s1=summarize(s0, MAX_STRING_LENGTH);
				if (s0!=s1 && !s0.equals(s1))
				{
					if (array==arguments)
					{
						array=new Object[array.length];
						System.arraycopy(arguments, 0, array, 0, array.length);
					}
					array[i]=s1;
				}
			}
		}
		return array;
	}

	/**
	 * Retourne la ressource associ�e � la cl� sp�cifi�e.
	 * @param  keyID Cl� de la ressource � utiliser.
	 * @throws MissingResourceException si aucune ressource n'est associ�e � la cl� sp�cifi�e.
	 */
	public final String getString(final int keyID) throws MissingResourceException
	{return getString(String.valueOf(keyID));}

	/**
	 * Utilise la ressource d�sign�e par la cl� <code>key</code> pour �crire un objet <code>arg0</code>.
	 * Cette ressource peut �tre soit un objet d'une classe d�riv�e de {@link java.text.Format}, ou soit
	 * une cha�ne de caract�res.
	 *
	 * <blockquote>
	 *     Si la ressource d�sign� par <code>key</code> est un objet d'une classe d�riv�e
	 *     de {@link java.text.Format}, alors elle sera utilis�e pour formater l'argument
	 *     <code>arg0</code>. Ce sera comme si la sortie avait �t� produite par:
	 *
	 *         <blockquote>
	 *         <pre>Format f = (Format) getObject(key);
	 *              return f.format(arg0);</pre>
	 *         </blockquote>
	 *
	 *     Si la ressource d�sign�e par <code>key</code> est une cha�ne de caract�res, alors
	 *     un objet {@link java.text.MessageFormat} sera utilis� pour formater l'argument
	 *     <code>arg0</code> d'apr�s la cha�ne de caract�res. Ce sera comme si la sortie avait
	 *     �t� produite par:
	 *
	 *         <blockquote>
	 *         <pre>String pattern = getString(key);
	 *              Format f = new MessageFormat(pattern);
	 *              return f.format(arg0);</pre>
	 *         </blockquote>
	 * </blockquote>
	 *
	 * Dans le dernier cas (lorsque la ressource d�sign�e par la cl� est une cha�ne de caract�res),
	 * Cette m�thode n'exige pas que l'on double les appostrophes ('') chaque fois que l'on veut
	 * �crire un simple appostrophe, contrairement � la classe {@link java.text.MessageFormat}
	 * standard du Java. L'argument <code>arg0</code> peut aussi �tre un tableau d'objets
	 * (<code>Object[]</code>) au lieu d'un objet seul (<code>Object</code>). Dans ce cas, toutes
	 * les occurences de "{0}", "{1}", "{2}", etc. dans la cha�ne de caract�res seront remplac�es
	 * par <code>arg0[0]</code>, <code>arg0[1]</code>, <code>arg0[2]</code>, etc. respectivement.
	 *
	 * @param  keyID Cl� de la ressource � utiliser.
	 * @param  arg0 Objet ou tableau d'objets � substituer aux "{0}", "{1}", etc.
	 * @throws MissingResourceException si aucune ressource n'est associ�e � la cl� sp�cifi�e.
	 *
	 * @see #getString(String)
	 * @see #getString(String,Object,Object)
	 * @see #getString(String,Object,Object,Object)
	 * @see java.text.MessageFormat
	 */
	public final String getString(final int keyID, final Object arg0) throws MissingResourceException
	{
		/*
		 * Obtient la ressource demand�e. Si cette ressource est de la classe
		 * {@link Format}, alors on laissera ce format se charger de formater
		 * l'argument <code>arg0</code>.
		 */
		final String key=String.valueOf(keyID);
		final Object object=getObject(key);
		if (object instanceof Format)
		{
			final Format format=(Format) object;
			if (format instanceof MessageFormat)
				return format.format(toArray(arg0));
			else return format.format(arg0);
		}
		else
		{
			/*
			 * Cr�e un objet MessageFormat pour le formatage des arguments. Cet
			 * objet MessageFormat utilisera normalement les conventions locales
			 * de l'utilisateur. Cela permettra par exemple d'�crire les dates
			 * selon les conventions du Canada Fran�ais (si on se trouve au Canada)
			 * m�me si les ressources sont celles du fran�ais de France. Si toutefois
			 * la langue utilis�e n'est pas la m�me, alors un utilisera les conventions
			 * des ressources actuelles plut�t que celles de l'utilisateur.
			 */
			final Object[] arguments=toArray(arg0);
			final MessageFormat format=new MessageFormat(object.toString());
			final Locale locale=getLocale();
			if (!Locale.getDefault().getLanguage().equalsIgnoreCase(locale.getLanguage()))
			{
				format.setLocale(locale);
			}
			return format.format(arguments);
		}
	}
}
