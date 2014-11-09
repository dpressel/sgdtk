package org.sgdtk;

import sun.misc.Unsafe;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of feature vector in a sparse layout.
 *
 * TODO: possibly roll the raw content in optionally?
 *
 * @author dpressel
 *
 */
public class FeatureVector
{

    private List<Offset> offsets;

    private final double y;

    private final int size;

    /**
     * Constructor for feature vectors that are ground truth
     * @param y label
     * @param size feature vector width
     */
    public FeatureVector(double y, int size)
    {
        this.y = y;
        this.size = size;
        this.offsets = new ArrayList<Offset>();
    }

    /**
     * Constructor for feature vectors that are not ground truth
     * @param size width of vector
     */
    public FeatureVector(int size)
    {
        this(-1, size);
    }

    /**
     * Get all non-zero values and their indices
     * @return
     */
    public List<Offset> getNonZeroOffsets()
    {
        return offsets;
    }

    /**
     * Length of feature vector
     * @return
     */
    public int length()
    {
        return size;
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
    public double getY()
    {
        return y;
    }

    /**
     * Add a new ofset to the feature vector (must not exceed size)
     * @param offset
     */
    public void add(Offset offset)
    {
        this.offsets.add(offset);
    }

}
