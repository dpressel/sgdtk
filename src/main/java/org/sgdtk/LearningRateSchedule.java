package org.sgdtk;

public interface LearningRateSchedule
{
    void reset(double eta0, double lambda);
    double update();
}
