package fr.ird.database;

/**
 * Une exception <i>CatalogException</i> est lev�e lorsqu'une exception autre que 
 * les exceptions standard h�ritant de la classe RemoteException est lev�e lors de 
 * l'acc�s � une ressource.
 *
 * Par exemple, si une connection est r�alis�e sur un serveur et qu'une exception
 * du type <code>SQLException</code> est lev�e lors de l'execution d'une requ�te,
 * une <code>CatalogException</code> est lev�e.
 *
 * @version $Id$
 * @author Remi Eve
 */
public class CatalogException extends java.rmi.RemoteException {   
    /** 
     * Constructeur. 
     *
     * @param cause     L'exception lev�e.
     */
    public CatalogException(final Throwable cause) {
        super(cause.getLocalizedMessage(), cause);
    }
    
    /** 
     * Constructeur. 
     *
     * @param message   Message de l'exception.
     */
    public CatalogException(final String message) {
        super(message);
    }    
}