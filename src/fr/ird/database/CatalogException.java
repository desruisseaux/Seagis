package fr.ird.database;

/**
 * Une exception <i>CatalogException</i> est levée lorsqu'une exception autre que 
 * les exceptions standard héritant de la classe RemoteException est levée lors de 
 * l'accés à une ressource.
 *
 * Par exemple, si une connection est réalisée sur un serveur et qu'une exception
 * du type <code>SQLException</code> est levée lors de l'execution d'une requête,
 * une <code>CatalogException</code> est levée.
 *
 * @version $Id$
 * @author Remi Eve
 */
public class CatalogException extends java.rmi.RemoteException {   
    /** 
     * Constructeur. 
     *
     * @param cause     L'exception levée.
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