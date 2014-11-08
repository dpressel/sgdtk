package org.sgdtk;

/**
 * Hinge loss function
 *
 * @author dpressel
 */
public class HingeLoss implements Loss
{
    /**
     * Hinge loss function
     * @param p prediction
     * @param y actual
     * @return loss
     */
    @Override
    public final double loss(double p, double y)
    {
        return Math.max(0, 1 - p * y);
    }

    /**
     * Derivative of loss function
     * @param p prediction
     * @param y actual
     * @return derivative
     */
    @Override
    public final double dLoss(double p, double y)
    {
        if (loss(p, y) == 0)
            return 0;
        return -y;
    }
}
