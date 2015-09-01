package org.sgdtk;

public class FixedLearningRateSchedule implements LearningRateSchedule
{
    double eta;

    public void reset(double eta0, double lambda)
    {
        this.eta = eta0;
    }
    public double update()
    {
        return eta;
    }
}
