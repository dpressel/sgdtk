/*
 * Copyright (c) 2015 3CSI
 * All Rights Reserved
 *
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF 3CSI
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package org.sgdtk;

import java.util.List;

public class FeatureVector
{
    VectorN x;
    double y;

    public static final double UNLABELED = Double.MIN_VALUE;

    public static FeatureVector newSparse(double y)
    {
        return new FeatureVector(y, new SparseVectorN());
    }
    public static FeatureVector newDense(double y, int length)
    {
        return new FeatureVector(y, new DenseVectorN(length));
    }

    public static FeatureVector newSparse()
    {
        return new FeatureVector(UNLABELED, new SparseVectorN());
    }
    public static FeatureVector newDense(int length)
    {
        return new FeatureVector(UNLABELED, new DenseVectorN(length));
    }


    public FeatureVector(double y, VectorN repr)
    {
        this.y = y;
        this.x = repr;
    }

    public int length()
    {
        return x.length();
    }

    public VectorN getX()
    {
        return x;
    }
    public double getY()
    {
        return y;
    }

    public void setY(double y)
    {
        this.y = y;
    }
    public void add(Offset offset)
    {
        x.add(offset);
    }

    public double dot(double[] vec)
    {
        return x.dot(vec);
    }

    public double dot(ArrayDouble vec)
    {
        return x.dot(vec);
    }
    
    // This is poor SoC.  Dense vectors should know how to handle themselves
    public List<Offset> getNonZeroOffsets()
    {
        return x.getNonZeroOffsets();
    }
    
    public void from(FeatureVector source)
    {
        this.y = source.getY();
        this.x.from(source.getX());
    }


    public int getSerializationSize()
    {
        if (x.getType() == VectorN.Type.SPARSE)
        {
            return getSparseSerializationSize();
        }
        return getDenseSerializationSize();
    }

    public UnsafeMemory serialize(byte[] buffer)
    {
        if (x.getType() == VectorN.Type.SPARSE)
        {
            return sparseSerialize(buffer);
        }
        return denseSerialize(buffer);
    }


    public static FeatureVector deserializeSparse(byte[] buffer)
    {
        UnsafeMemory memory = new UnsafeMemory(buffer);
        double y = memory.getDouble();
        FeatureVector fv = FeatureVector.newSparse(y);
        int sparseSz = memory.getInt();

        for (int i = 0; i < sparseSz; ++i)
        {
            fv.add(new Offset(memory.getInt(), memory.getDouble()));
        }
        fv.getX().organize();
        return fv;
    }

    public static FeatureVector deserializeDense(byte[] buffer)
    {
        UnsafeMemory memory = new UnsafeMemory(buffer);
        double y = memory.getDouble();
        int denseSz = memory.getInt();

        DenseVectorN dv = new DenseVectorN(denseSz);

        for (int i = 0; i < denseSz; ++i)
        {
            dv.set(i, memory.getDouble());
        }
        return new FeatureVector(y, dv);
    }

    public int getSparseSerializationSize()
    {
        int numNonSparseElements = getNonZeroOffsets().size();
        int part = UnsafeMemory.SIZE_OF_DOUBLE + UnsafeMemory.SIZE_OF_INT;
        int total = part + numNonSparseElements * part;
        return total;
    }

    public UnsafeMemory sparseSerialize(byte[] buffer)
    {
        int total = getSerializationSize();
        if (buffer == null)
        {
            buffer = new byte[total];
        }
        List<Offset> offsets = getNonZeroOffsets();

        assert( total <= buffer.length );
        UnsafeMemory memory = new UnsafeMemory(buffer);
        memory.putDouble(getY());

        int sz = offsets.size();
        memory.putInt(sz);
        for (int i = 0; i < sz; ++i)
        {
            Offset offset = offsets.get(i);
            memory.putInt(offset.index);
            memory.putDouble(offset.value);
        }
        return memory;
    }

    protected int getDenseSerializationSize()
    {
        DenseVectorN dv = (DenseVectorN)x;
        int numElements = dv.getX().size();
        int head = UnsafeMemory.SIZE_OF_DOUBLE + UnsafeMemory.SIZE_OF_INT;
        int total = head + numElements * UnsafeMemory.SIZE_OF_DOUBLE;
        return total;
    }

    protected UnsafeMemory denseSerialize(byte[] buffer)
    {
        int total = getSerializationSize();
        if (buffer == null)
        {
            buffer = new byte[total];
        }

        assert( total <= buffer.length );
        UnsafeMemory memory = new UnsafeMemory(buffer);

        DenseVectorN dv = (DenseVectorN)x;
        ArrayDouble xa = dv.getX();
        memory.putDouble(y);

        int sz = xa.size();
        memory.putInt(sz);
        for (int i = 0; i < sz; ++i)
        {
            double v = xa.get(i);
            memory.putDouble(v);
        }
        return memory;
    }
}
