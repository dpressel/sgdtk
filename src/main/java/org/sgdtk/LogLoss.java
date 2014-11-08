package org.sgdtk;


/**
 * Log loss function
 * @author dpressel
 */
public class LogLoss implements Loss
{
    /**
     * Math.log(1 + Math.exp(-z))
     * @param p prediction
     * @param y actual
     * @return loss
     */
    @Override
    public final double loss(double p, double y)
    {

        double z = p * y;
        return z > 18 ? Math.exp(-z): z < -18 ? -z : Math.log(1 + Math.exp(-z));
    }

    /**
     * -y / (1 + Math.exp(z))
     * @param p prediction
     * @param y actual
     * @return derivative
     */

    @Override
    public final double dLoss(double p, double y)
    {
        double z = p * y;
        return z > 18 ? -y * Math.exp(-z) : z < -18 ? -y : -y / (1 + Math.exp(z));

    }
}
