package org.sgdtk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Classification using SGD
 *
 * @author dpressel
 */
public class SGDLearner implements Learner
{
    private static final Logger log = LoggerFactory.getLogger(SGDLearner.class);
    public static double ETA_FACTOR = 2.;
    public static double LOW_ETA_0 = 1.;

    Loss lossFunction;
    double lambda;
    double eta0 = -1;
    double numSeenTotal = 0;

    /**
     * Default constructor, use hinge loss
     */
    public SGDLearner()
    {
        this(new HingeLoss());
    }

    /**
     * Constructor with a loss function
     * @param loss loss function
     */
    public SGDLearner(Loss loss)
    {
        this(loss, 1e-5);
    }

    /**
     * Constructor with loss function, regularization param
     * @param loss loss function
     * @param lambda regularization param
     */
    public SGDLearner(Loss loss, double lambda)
    {
        this.lossFunction = loss;
        this.lambda = lambda;
    }

    /**
     * Create the model, empty but initialized
     * @param wlength The length of the feature vector
     * @return
     */
    @Override
    public Model create(int wlength)
    {
        this.numSeenTotal = 0;
        LinearModel lm = new LinearModel(wlength, 1., 0.);
        return lm;
    }

    /**
     * Train a single pass.  On the first pass learning eta is set empirically
     *
     * @param model The model to update
     * @param trainingExamples The training examples
     * @return
     */
    @Override
    public Model trainEpoch(Model model, List<FeatureVector> trainingExamples)
    {
        // Check if we have eta set already
        if (eta0 <= 0)
        {
            initEta0(model, trainingExamples.subList(0, Math.min(1000, trainingExamples.size())));
            log.info("eta0=" + eta0);
        }
        LinearModel lm = (LinearModel)model;
        for (FeatureVector fv : trainingExamples)
        {
            double eta = eta0 / (1 + lambda * eta0 * numSeenTotal);
            trainOne(lm, fv, eta);
            ++numSeenTotal;
        }

        log.info("wnorm=" + lm.mag());
        return model;
    }

    private void trainOne(LinearModel lm, FeatureVector fv, double eta)
    {
        double y = fv.getY();
        double fx = lm.predict(fv);
        double wdiv = lm.getWdiv();

        wdiv /= (1 - eta * lambda);
        if (wdiv > 1e5)
        {

            final double sf = 1.0 / wdiv;
            lm.scaleInplace(sf);
            wdiv = 1.;

        }
        lm.setWdiv(wdiv);

        double d = lossFunction.dLoss(fx, y);
        double disp = -eta * d * wdiv;

        for (Offset offset : fv.getNonZeroOffsets())
        {
            lm.addInplace(offset.index, offset.value * disp);
        }

        double etab = eta * 0.01;
        double wbias = lm.getWbias();


        wbias += -etab * d;
        lm.setWbias(wbias);

    }

    private void initEta0(Model model, List<FeatureVector> sample)
    {
        double lowEta = LOW_ETA_0;
        double lowCost = evalEta(model, sample, lowEta);
        double highEta = lowEta * ETA_FACTOR;
        double highCost = evalEta(model, sample, highEta);
        if (lowCost < highCost)
        {
            while (lowCost < highCost)
            {
                highEta = lowEta;
                highCost = lowCost;
                lowEta = highEta / ETA_FACTOR;
                lowCost = evalEta(model, sample, lowEta);
            }
        }
        else if (highCost < lowCost)
        {
            while (highCost < lowCost)
            {
                lowEta = highEta;
                lowCost = highCost;
                highEta = lowEta * ETA_FACTOR;
                highCost = evalEta(model, sample, highEta);
            }
        }
        eta0 = lowEta;


    }

    private double evalEta(Model model, List<FeatureVector> sample, double eta)
    {
        LinearModel clone = (LinearModel)model.prototype();
        for (FeatureVector fv : sample)
        {
            trainOne(clone, fv, eta);
        }
        Metrics metrics = new Metrics();
        eval(clone, sample, metrics);
        return metrics.getCost();
    }

    /**
     * Eval the model
     * @param model The model to use for evaluation
     * @param fv The feature vector
     * @param metrics Metrics to add to
     * @return
     */
    @Override
    public double evalOne(Model model, FeatureVector fv, Metrics metrics)
    {

        double y = fv.getY();
        double fx = model.predict(fv);
        double loss = lossFunction.loss(fx, y);
        double error = (fx * y <= 0) ? 1 : 0;
        metrics.add(loss, error);
        return fx;
    }

    /**
     * Evaluate all examples
     * @param model The model to use for evaluation
     * @param testingExamples The examples
     * @param metrics Metrics to add to
     */
    @Override
    public void eval(Model model, List<FeatureVector> testingExamples, Metrics metrics)
    {
        for (FeatureVector fv : testingExamples)
        {
            evalOne(model, fv, metrics);
        }

        LinearModel lm = (LinearModel)model;
        double normW = lm.mag();
        double cost = metrics.getLoss() + 0.5 * lambda * normW;
        metrics.setCost(cost);

    }

}
