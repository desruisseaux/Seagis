/*
 * Units specification.
 */
package javax.units;

// Miscellaneous
import java.io.Serializable;
import net.seas.util.WeakHashSet;


/**
 * Placeholder for future <code>Unit</code> class. This
 * skeleton will be removed when the real classes (from
 * <A HREF="http://www.jcp.org/jsr/detail/108.jsp">JSR-108:
 * Units specification</A>) will be publicly available.
 */
public final class Unit implements Serializable
{
    /**
     * Pool of units.
     */
    private static final WeakHashSet<Unit> pool=new WeakHashSet<Unit>();

    /**
     * Base unit of time.
     */
    public static final Unit SECOND = pool.intern(new Unit("s"));

    /**
     * Base unit of length.
     */
    public static final Unit METRE = pool.intern(new Unit("m"));

    /**
     * Unit of angle.
     */
    public static final Unit DEGREE = pool.intern(new Unit("°"));

    /**
     * The unit's symbol.
     */
    private final String symbol;

    /**
     * Returns an unit instance.
     */
    public static Unit get(final String symbol)
    {return pool.intern(new Unit(symbol));}

    /**
     * Don't allow instance creation, since
     * this is not the official Unit class.
     */
    private Unit(final String symbol)
    {this.symbol=symbol;}

    /**
     * Check if amount of the specified unit
     * can be converted into amount of this unit.
     */
    public boolean canConvert(final Unit unit)
    {return this==unit;}

    /**
     * Convert a value from one unit to an other.
     * This method is not implemented (the JSR-108
     * will provide the reference implementation).
     */
    public double convert(final double value, final Unit unit)
    {return value;}

    /**
     * Returns a string representation of this unit's symbol.
     */
    public String toString()
    {return symbol;}
}
