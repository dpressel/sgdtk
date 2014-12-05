package org.sgdtk;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of feature vector in a sparse layout.
 * For binary data, the y label should be -1/1.  For multicategory
 * data, the numbers should enumerate from 1 to N
 *
 * @author dpressel
 *
 */
public class FeatureVector
{

    private List<Offset> offsets;

    private int y;

    /**
     * Constructor for feature vectors that are ground truth
     * @param y label
     */
    public FeatureVector(int y)
    {
        this.y = y;
        this.offsets = new ArrayList<Offset>();
    }

    /**
     * Constructor for feature vectors that are not ground truth
     */
    public FeatureVector()
    {
        this(0);
    }

    /**
     * Get all non-zero values and their indices
     * @return
     */
    public final List<Offset> getNonZeroOffsets()
    {
        return offsets;
    }

    /**
     * Length of feature vector
     * @return
     */
    public final int length()
    {
        int sz = offsets.size();
        return sz == 0 ? 0: (offsets.get(sz - 1).index + 1);
    }

    // Thankfully, this is not necessary as its implemented
    /*public double get(int i)
    {
        int compressedIndex = Arrays.binarySearch(ints, 0, ints.length, i);
        return values[compressedIndex];
    }*/

    /**
     * Get the label
     * @return return label
     */
    public int getY()
    {
        return y;
    }

    public void setY(int y)
    {
        this.y = y;
    }
    /**
     * Add a new offset to the feature vector (must not exceed size)
     * @param offset
     */
    public final void add(Offset offset)
    {
        this.offsets.add(offset);
    }

}
