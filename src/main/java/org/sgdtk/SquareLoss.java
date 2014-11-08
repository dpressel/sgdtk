package org.sgdtk;

/**
 * Square loss
 *
 * @author dpressel
 */
public class SquareLoss implements Loss
{
    /**
     * Square loss
     * @param p predicted
     * @param y actual
     * @return loss
     */
    @Override
    public final double loss(double p, double y)
    {
        double z = p * y;
        double d = 1 - z;
        return 0.5 * d * d;
    }

    /**
     * Derivative square loss
     * @param p predicted
     * @param y actual
     * @return derivative
     */
    @Override
    public final double dLoss(double p, double y)
    {
        double z = p * y;
        double d = 1 - z;
        return -y * d;
    }
}
