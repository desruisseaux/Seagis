/*
 * Units specification.
 */
package javax.units;

// Miscellaneous
import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;


/**
 * Placeholder for future <code>Unit</code> class. This
 * skeleton will be removed when the real classes (from
 * <A HREF="http://www.jcp.org/jsr/detail/108.jsp">JSR-108:
 * Units specification</A>) will be publicly available.
 * <br><br>
 * <strong>IMPORTANT: future version will NOT be compatible
 * will this one. Remind, this is a temporary class!</strong>
 */
public final class Unit implements Serializable
{
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -897653548520920138L;

    /**
     * Pool of units.
     */
    private static final Map pool=new HashMap();

    /**
     * Unit of angle.
     */
    public static final Unit DEGREE = new Unit("°");

    /**
     * Base unit of length.
     */
    public static final Unit METRE = new Unit("m");

    /**
     * Base unit of time.
     */
    public static final Unit SECOND = new Unit("s");

    /**
     * Unit of time.
     */
    public static final Unit MILLISECOND = new Unit("ms", 0.001, SECOND);

    /**
     * Unit of time.
     */
    public static final Unit DAY = new Unit("day", 24*60*60, SECOND);

    /**
     * Unit of mass.
     */
    public static final Unit KILOGRAM = new Unit("kg");

    /**
     * Unit of mass.
     */
    public static final Unit TON = new Unit("ton", 1000, KILOGRAM);

    /**
     * The unit's symbol.
     */
    private final String symbol;

    /**
     * The scale factor.
     */
    private final double scale;

    /**
     * Base unit, or <code>this</code> if none.
     */
    private final Unit unit;

    /**
     * Returns an unit instance.
     */
    public static Unit get(final String symbol)
    {
        synchronized (pool)
        {
            final Unit unit    = new Unit(symbol);
            final Unit current = (Unit) pool.get(unit);
            if (current!=null) return current;
            pool.put(unit, unit);
            return unit;
        }
    }

    /**
     * Unit constructor. Don't allow user creation,
     * since this is not the official Unit class.
     */
    private Unit(final String symbol)
    {
        this.symbol = symbol;
        this.scale  = 1;
        this.unit   = this;
    }

    /**
     * Unit constructor. Don't allow user creation,
     * since this is not the official Unit class.
     */
    private Unit(final String symbol, final double scale, final Unit unit)
    {
        this.symbol = symbol;
        this.scale  = scale;
        this.unit   = unit;
    }

    /**
     * Check if amount of the specified unit
     * can be converted into amount of this unit.
     */
    public boolean canConvert(final Unit other)
    {return (unit==other.unit) || (unit!=null && unit.equals(other.unit));}

    /**
     * Convert a value from one unit to an other.
     * This method is not implemented (the JSR-108
     * will provide the reference implementation).
     */
    public double convert(final double value, final Unit unit)
    {
        if (canConvert(unit)) return value*unit.scale/scale;
        throw new IllegalArgumentException("Can't convert from \""+this+"\" to \""+unit+"\".");
    }

    /**
     * Returns a string representation of this unit's symbol.
     */
    public String toString()
    {return symbol;}

    /**
     * Returns a hash code value.
     */
    public int hashCode()
    {return symbol.hashCode();}

    /**
     * Compare this unit symbol with the specified object for equality.
     * Only symbols are compared; other parameters are ignored.
     */
    public boolean equals(final Object object)
    {
        if (object instanceof Unit)
        {
            final Unit that = (Unit)object;
            return symbol.equals(that.symbol);
        }
        return false;
    }
}
