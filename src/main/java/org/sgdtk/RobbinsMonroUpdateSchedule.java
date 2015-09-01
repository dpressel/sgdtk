package org.sgdtk;

public class RobbinsMonroUpdateSchedule implements LearningRateSchedule
{
    long numSeenTotal;
    double eta0;
    double lambda;

    public RobbinsMonroUpdateSchedule()
    {

    }
    @Override
    public void reset(double eta0, double lambda)
    {
        this.lambda = lambda;
        this.eta0 = eta0;
        numSeenTotal = 0;
    }

    @Override
    public double update()
    {
        double eta = eta0 / (1 + lambda * eta0 * numSeenTotal);
        ++numSeenTotal;
        return eta;
    }
}
