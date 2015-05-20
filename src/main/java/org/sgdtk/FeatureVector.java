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
}
