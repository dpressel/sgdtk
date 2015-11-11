package org.sgdtk;

import java.util.ArrayList;
import java.util.List;

public class DenseVectorN implements VectorN
{
    ArrayDouble x;

    public DenseVectorN(VectorN source)
    {
        this.from(source);
    }
    public DenseVectorN()
    {
        x = new ArrayDouble();
    }
    public DenseVectorN(int length)
    {
        x = new ArrayDouble(length);
    }
    public DenseVectorN(double[] x)
    {
        this.x = new ArrayDouble(x);
    }
    public ArrayDouble getX()
    {
        return x;
    }

    @Override
    public int length()
    {
        return x.size();
    }


    @Override
    public void add(double[] vec)
    {
        assert(vec.length <= x.size());
        x.addn(vec);
    }

    @Override
    public double mag()
    {
        int sz = x.size();
        double acc = 0.0;
        for (int i = 0; i < sz; ++i)
        {
            double d = x.get(i);
            acc += d * d;
        }
        return acc;
    }

    @Override
    public double update(int i, double v)
    {
        return x.addi(i, v);

    }

    @Override
    public Type getType()
    {
        return Type.DENSE;
    }

    @Override
    public void add(VectorN vec)
    {
        if (vec.getType() == Type.SPARSE)
        {
            for (Offset offset : vec.getNonZeroOffsets())
            {
                x.addi(offset.index, offset.value);
            }
        }
        else
        {
            DenseVectorN dv = (DenseVectorN)vec;
            add(dv.getX().v);
        }
    }

    @Override
    public void add(Offset offset)
    {
        x.addi(offset.index, offset.value);
    }

    @Override
    public void set(int i, double v)
    {
        x.set(i, v);
    }

    @Override
    public double dot(VectorN vec)
    {
        double acc = 0.;
        if (vec instanceof SparseVectorN)
        {
            for (Offset offset : vec.getNonZeroOffsets())
            {
                acc += x.get(offset.index) * offset.value;
            }
        }
        else
        {
            DenseVectorN dv = (DenseVectorN)vec;
            return dot(dv.getX().v);
        }
        return acc;
    }

    @Override
    public void reset()
    {
        x.constant(0.);
    }

    @Override
    public double dot(double[] vec)
    {
        return CollectionsManip.dot(x.v, vec);
    }

    @Override
    public void scale(double scalar)
    {
        x.scale(scalar);
    }

    @Override
    public List<Offset> getNonZeroOffsets()
    {
        List<Offset> offsetList = new ArrayList<Offset>();
        int sz = x.size();
        for (int i = 0; i < sz; ++i)
        {
            double xi = x.get(i);
            if (xi != 0.0)
            {
                offsetList.add(new Offset(i, xi));
            }
        }
        return offsetList;
    }

    @Override
    public void from(VectorN source)
    {
        int sz = source.length();
        if (x == null)
        {
            x = new ArrayDouble(sz);
        }
        else
        {
            x.resize(sz);
        }
        for (Offset offset : source.getNonZeroOffsets())
        {
            x.set(offset.index, offset.value);
        }
    }

    public double at(int i)
    {
        return x.at(i);
    }

    public void organize()
    {

    }

}
