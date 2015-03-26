package org.sgdtk;

public interface WeightModel extends Model
{
    double mag();

    void updateWeights(FeatureVector fv, double eta, double lambda, double dLoss);
}
