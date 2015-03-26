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

public interface FeatureVector
{
    int length();

    double getY();

    void setY(double y);

    void add(Offset offset);

    double dot(double[] vec);
    
    // This is poor SoC.  Dense vectors should know how to handle themselves
    public List<Offset> getNonZeroOffsets();
    
    void from(FeatureVector source);
}
