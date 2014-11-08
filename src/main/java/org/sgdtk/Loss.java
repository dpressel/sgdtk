package org.sgdtk;

/**
 * Interface for loss function
 * @author dpressel
 */
public interface Loss
{

    /**
     * Loss function
     * @param p predicted
     * @param y actual
     * @return loss
     */
    double loss(double p, double y);

    /**
     * Derivative of loss function
     * @param p predicted
     * @param y actual
     * @return derivative
     */

    double dLoss(double p, double y);
}