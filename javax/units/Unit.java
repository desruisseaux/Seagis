/*
 * Units specification.
 */
package javax.units;

// Miscellaneous
import java.io.Serializable;


/**
 * Placeholder for future <code>Unit</code> class. This
 * skeleton will be removed when the real classes (from
 * <A HREF="http://www.jcp.org/jsr/detail/108.jsp">JSR-108:
 * Units specification</A>) will be publicly available.
 */
public final class Unit implements Serializable
{
    /**
     * Base unit of metre.
     */
    public static final Unit METRE = new Unit("m");

    /**
     * Unit of degree.
     */
    public static final Unit DEGREE = new Unit("°");

    /**
     * The unit's symbol.
     */
    private final String symbol;

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
