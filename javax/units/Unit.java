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
 * <br><br>
 * <strong>IMPORTANT: future version will NOT be compatible
 * will this one. Remind, this is a temporary class!</strong>
 */
public final class Unit implements Serializable
{
    /**
     * Pool of units.
     */
    private static final WeakHashSet<Unit> pool=new WeakHashSet<Unit>();

    /**
     * Unit of angle.
     */
    public static final Unit DEGREE = pool.intern(new Unit("°"));

    /**
     * Base unit of length.
     */
    public static final Unit METRE = pool.intern(new Unit("m"));

    /**
     * Base unit of time.
     */
    public static final Unit SECOND = pool.intern(new Unit("s"));

    /**
     * Base unit of time.
     */
    public static final Unit DAY = pool.intern(new Unit("day", 24*60*60, SECOND));

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
    {return pool.intern(new Unit(symbol));}

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
    public boolean canConvert(final Unit unit)
    {return this.unit==unit.unit;}

    /**
     * Convert a value from one unit to an other.
     * This method is not implemented (the JSR-108
     * will provide the reference implementation).
     */
    public double convert(final double value, final Unit unit)
    {
        if (canConvert(unit)) return value*unit.scale/scale;
        throw new IllegalArgumentException(String.valueOf(unit));
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
