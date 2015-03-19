package org.sgdtk;


/**
 * Squared loss
 *
 * @author dpressel
 */
public class SquaredLoss implements Loss
{
    @Override
    public double loss(double p, double y)
    {
        double d = p - y;
        return 0.5 * d * d;
    }

    @Override
    public double dLoss(double p, double y)
    {
        return (p - y);
    }
}
