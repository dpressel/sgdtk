package org.sgdtk;

/**
 * Offset for sparse storage.  Mainly internal use
 *
 * @author dpressel
 */
public class Offset implements Comparable<Offset>
{
    public final int index;

    @Override
    public int compareTo(Offset o)
    {
        return Integer.compare(index, o.index);
    }

    public final double value;

    /**
     * Default constructor
     */
    public Offset()
    {
        this.index = 0;
        this.value = 0.;
    }

    /**
     * Initialize to index/value
     * @param index index
     * @param value value
     */
    public Offset(int index, double value)
    {
        this.index = index;
        this.value = value;
    }

}
