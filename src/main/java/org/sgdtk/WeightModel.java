package org.sgdtk;

public interface WeightModel extends Model
{
    double mag();

    void updateWeights(VectorN vectorN, double eta, double lambda, double dLoss, double y);
}
