package fr.ird.n1b.io;

/**
 * Décrit un canal. 
 *
 * @author  Remi Eve
 * @version $Id$
 */
public final class Channel 
{    
    /** Identifie le type d'un canal. */
    private static final int VISIBLE = 0,
                             THERMAL = 1;
    
    /** Canaux disponibles. */
    public static final Channel CHANNEL_1  = get(0, "Canal 1 ", VISIBLE),
                                CHANNEL_2  = get(1, "Canal 2 ", VISIBLE),
                                CHANNEL_3  = get(2, "Canal 3 ", VISIBLE),
                                CHANNEL_4  = get(3, "Canal 4 ", THERMAL),
                                CHANNEL_5  = get(4, "Canal 5 ", THERMAL),
                                CHANNEL_3A = get(5, "Canal 3a", VISIBLE),
                                CHANNEL_3B = get(6, "Canal 3b", THERMAL);    
    
    /** Identifiant interne. */
    private final int id;
    
    /** Nom. */
    private final String name;
    
    /** Caractéristique du canal. */ 
    private final int caracteristic;
    
    /** 
     * Constructeur.
     *
     * @param id    Identifiant interne du canal.
     * @param name  Nom du canal.
     */
    private Channel(final int    id, 
                    final String name,
                    final int    caracteristic) 
    {
        this.id   = id;
        this.name = name;
        this.caracteristic = caracteristic;
    }    
    
    /**
     * Retourne un canal.
     *
     * @param id    Identifiant interne du canal.
     * @param name  Nom du canal.
     * @return un canal.
     */
    private static Channel get (final int    id, 
                                final String name,
                                final int    caracteristic)
    {
        return new Channel(id, name, caracteristic);
    }        
    
    /**
     * Retourne <i>true</i> si le canal contient des informations VISIBLE.
     * @return <i>true</i> si le canal contient des informations VISIBLE.
     */
    public boolean isVisible()
    {
        return (caracteristic == VISIBLE);
    }
    
    /**
     * Retourne <i>true</i> si le canal contient des informations THERMAL.
     * @return <i>true</i> si le canal contient des informations THERMAL.
     */
    public boolean isThermal()
    {
        return (caracteristic == THERMAL);
    }    
}