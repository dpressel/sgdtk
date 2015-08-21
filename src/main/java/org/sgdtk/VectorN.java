package org.sgdtk;

import java.util.List;

public interface VectorN
{
    int length();

    void add(Offset offset);

    void set(int i, double v);

    double dot(double[] vec);

    double dot(VectorN vec);

    double update(int i, double v);

    void add(double[] vec);

    void add(VectorN vec);

    void scale(double scalar);

    // This is poor SoC.  Dense vectors should know how to handle themselves
    List<Offset> getNonZeroOffsets();

    // Very inefficient for sparse!
    double at(int i);

    double mag();

    void from(VectorN source);

    void organize();

    void reset();
}
