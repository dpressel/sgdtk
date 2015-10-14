package org.sgdtk;

import java.util.*;

public class SparseVectorN implements VectorN
{
    private List<Offset> offsets;

    public SparseVectorN(VectorN source)
    {
        this.offsets = new ArrayList<Offset>();
        this.from(source);
    }

    /**
     * Constructor for sparse vector, no args
     */
    public SparseVectorN()
    {
        this.offsets = new ArrayList<Offset>();
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
    public void from(VectorN source)
    {

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

    // Dont use this for anything significant, slow...
    @Override
    public void set(int i, double v)
    {
        int j = realIndex(i);
        if (j < 0)
        {
            add(new Offset(i, v));
            Collections.sort(offsets);
        }
        else
        {
            offsets.get(j).value = v;
        }
    }

    public double update(int i, double v)
    {
        int j = realIndex(i);
        if (j < 0)
        {
            add(new Offset(i, v));
            Collections.sort(offsets);
            return v;
        }
        else
        {
            double acc = offsets.get(j).value + v;

            offsets.get(j).value = acc;
            return acc;
        }
    }

    public int realIndex(int i)
    {
        for (int j = 0, sz = offsets.size(); j < sz; ++j)
        {
            Offset offset = offsets.get(j);
            if (offset.index > i)
            {
                return -1;
            }
            else if (offset.index == i)
            {
                return j;
            }
        }
        return -1;
    }

    public double at(int i)
    {
        int j = realIndex(i);

        return j < 0 ? 0.: offsets.get(j).value;
    }

    @Override
    public void add(double[] vec)
    {
        for (int i = 0; i < vec.length; ++i)
        {
            if (vec[i] != 0.0)
            {
                update(i, vec[i]);
            }
        }
    }

    @Override
    public void scale(double scalar)
    {
        for (Offset offset : offsets)
        {
            offset.value *= scalar;
        }
    }

    @Override
    public void add(VectorN vec)
    {
        for (Offset offset : vec.getNonZeroOffsets())
        {
            update(offset.index, offset.value);

        }
    }

    @Override
    public double mag()
    {
        double acc = 0.0;
        for (Offset offset : offsets)
        {
            acc += offset.value * offset.value;
        }
        return acc;
    }

    // TODO: buffer This will be horribly slow for sparse on RHS, do not use for sparse-sparse dots!!!
    @Override
    public double dot(VectorN vectorN)
    {
        double acc = 0.;
        for (Offset offset : offsets)
        {
            acc += offset.value * vectorN.at(offset.index);
        }
        return acc;
    }

    @Override
    public void reset()
    {
        offsets.clear();
    }

    /**
     * Add a new offset to the feature vector (must not exceed size)
     * @param offset
     */
    @Override
    public final void add(Offset offset)
    {
        this.offsets.add(offset);
        //Collections.sort(this.offsets);
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

    public final void organize()
    {
        Collections.sort(offsets);
        Set<Integer> seen = new HashSet<Integer>();
        List<Offset> clean = new ArrayList<Offset>(offsets.size());
        for (Offset offset: offsets)
        {
            if (!seen.contains(offset.index))
            {
                seen.add(offset.index);
                clean.add(offset);
            }
        }
        this.offsets = new ArrayList<Offset>(clean);
    }

    @Override
    public Type getType()
    {
        return Type.SPARSE;
    }
}
