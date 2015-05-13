package org.sgdtk;

import java.util.List;

public interface VectorN
{
    int length();

    void add(Offset offset);

    void set(int i, double v);

    double dot(double[] vec);

    // This is poor SoC.  Dense vectors should know how to handle themselves
    List<Offset> getNonZeroOffsets();

    // Very inefficient for sparse!
    double at(int i);

    void from(VectorN source);

    void organize();
}
