package org.sgdtk;

/**
 * Offset for sparse storage.  Mainly internal use
 *
 * @author dpressel
 */
public class Offset
{
    public int index;
    public double value;

    /**
     * Default constructor
     */
    public Offset()
    {
        this(0, 0.);
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
