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
public class SparseFeatureVector implements FeatureVector
{

    private List<Offset> offsets;

    // http://stackoverflow.com/questions/3793838/which-is-the-first-integer-that-an-ieee-754-float-is-incapable-of-representing-e
    private double y;

    /**
     * Constructor for feature vectors that are ground truth
     * @param y label
     */
    public SparseFeatureVector(double y)
    {
        this.y = y;
        this.offsets = new ArrayList<Offset>();
    }

    /**
     * Constructor for feature vectors that are not ground truth
     */
    public SparseFeatureVector()
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

    @Override
    public void from(FeatureVector source)
    {
        this.y = source.getY();

        for (Offset offset : source.getNonZeroOffsets())
        {
            this.add(offset);
        }
    }

    /**
     * Length of feature vector
     * @return
     */
    @Override
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
    @Override
    public double getY()
    {
        return y;
    }

    @Override
    public void setY(double y)
    {
        this.y = y;
    }
    /**
     * Add a new offset to the feature vector (must not exceed size)
     * @param offset
     */
    @Override
    public final void add(Offset offset)
    {
        this.offsets.add(offset);
    }


    public final double dot(double[] vec)
    {
        double acc = 0.;
        for (Offset offset : getNonZeroOffsets())
        {
            acc += offset.value * vec[offset.index];
        }
        return acc;
        
    }

}
