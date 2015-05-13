package org.sgdtk;

import java.util.ArrayList;
import java.util.List;

public class DenseVectorN implements VectorN
{
    double[] x;
    int length;

    public DenseVectorN(VectorN source)
    {
        this.from(source);
    }
    public DenseVectorN()
    {

    }
    public DenseVectorN(int length)
    {
        this.length = length;
        x = new double[length];
    }
    public DenseVectorN(double[] x)
    {
        this.x = new double[x.length];
        length = x.length;
        System.arraycopy(x, 0, this.x, 0, length);
    }
    public double [] getX()
    {
        return x;

    }

    @Override
    public int length()
    {
        return length;
    }


    @Override
    public void add(Offset offset)
    {
        if (offset.index > length)
        {
            throw new RuntimeException("Index out of bounds! " + offset.index + ". Max is " + length);
        }
        x[offset.index] = offset.value;
    }

    @Override
    public void set(int i, double v)
    {
        x[i] = v;
    }

    @Override
    public double dot(double[] vec)
    {
        return CollectionsManip.dot(x, vec);
    }

    @Override
    public List<Offset> getNonZeroOffsets()
    {
        List<Offset> offsetList = new ArrayList<Offset>();
        for (int i = 0; i < length; ++i)
        {
            if (x[i] != 0.0)
            {
                offsetList.add(new Offset(i, x[i]));
            }
        }
        return offsetList;
    }

    @Override
    public void from(VectorN source)
    {
        length = source.length();
        x = new double[length];

        for (Offset offset : source.getNonZeroOffsets())
        {
            x[offset.index] = offset.value;
        }
    }

    public double at(int i)
    {
        return x[i];
    }

    public void organize()
    {

    }

}
