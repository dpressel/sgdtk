package org.sgdtk;

/**
 * Square loss
 *
 * @author dpressel
 */
public class SquaredHingeLoss implements Loss
{
    /**
     * Squared hinge loss
     * @param p predicted
     * @param y actual
     * @return loss
     */
    @Override
    public final double loss(double p, double y)
    {
        double z = p * y;
        if (z > 1.0)
            return 0.;
        double d = 1 - z;
        return 0.5 * d * d;
    }

    /**
     * Derivative squared hinge loss
     * @param p predicted
     * @param y actual
     * @return derivative
     */
    @Override
    public final double dLoss(double p, double y)
    {
        double z = p * y;
        if (z > 1.0)
            return 0.;
        double d = 1 - z;
        return -y * d;
    }
}
