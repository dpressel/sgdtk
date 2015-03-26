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
    boolean regularizedBias = false;
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
        this(loss, lambda, -1.);
    }

    /**
     * Constructor with loss function, regularization param
     * @param loss loss function
     * @param lambda regularization param
     */
    public SGDLearner(Loss loss, double lambda, double kEta)
    {
        this(loss, lambda, kEta, false);
    }

    /**
     * Constructor with loss function, regularization param
     * @param loss loss function
     * @param lambda regularization param
     */
    public SGDLearner(Loss loss, double lambda, double kEta, boolean regularizedBias)
    {
        this.lossFunction = loss;
        this.lambda = lambda;
        this.eta0 = kEta;
        this.regularizedBias = regularizedBias;
    }

    /**
     * Create the model, empty but initialized
     * @param wlength The length of the feature vector
     * @return
     */
    @Override
    public final Model create(int wlength)
    {
        this.numSeenTotal = 0;
        WeightModel lm = new WeightModel(wlength);
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
    public final Model trainEpoch(Model model, List<FeatureVector> trainingExamples)
    {
        // Check if we have eta set already
        if (eta0 <= 0)
        {
            preprocess(model, trainingExamples.subList(0, Math.min(1000, trainingExamples.size())));
            log.info("eta0=" + eta0);
        }

        for (FeatureVector fv : trainingExamples)
        {
            trainOne(model, fv);
        }

        WeightModel lm = (WeightModel)model;
        double normW = lm.mag();
        if (regularizedBias)
        {
            normW += lm.getWbias()*lm.getWbias();
        }
        log.info("wnorm=" + normW);
        return model;
    }


    @Override
    public final void trainOne(Model model, FeatureVector fv)
    {
        WeightModel lm = (WeightModel)model;
        double eta = eta0 / (1 + lambda * eta0 * numSeenTotal);

        trainOneWithEta(lm, fv, eta);

        ++numSeenTotal;
    }

    /**
     * This method performs an SGD update from a single training example.  This looks a little different
     * than most implementations I'm aware of, mostly because of a trick that speeds the code up drastically for sparse
     * vector by avoiding a rescale by the regularization parameters over the full vector.  You can see the typical 
     * update approach in sofia-ml and scikit-learn.  However, here, a factoring of the weight vector is employed that
     * separates the scalar due to regularization from the loss gradient application.  The factoring is described
     * here (under section 5.1):
     * 
     * @see <a href="http://research.microsoft.com/pubs/192769/tricks-2012.pdf">http://research.microsoft.com/pubs/192769/tricks-2012.pdf</a>
     * @param weightModel
     * @param fv
     * @param eta
     */
    private final void trainOneWithEta(WeightModel weightModel, FeatureVector fv, double eta)
    {
        double y = fv.getY();
        double fx = weightModel.predict(fv);
       

        double dLdp = lossFunction.dLoss(fx, y);
        double[] weights = weightModel.getWeights();
        double wdiv = weightModel.getWdiv();

        // The wdiv is a scalar factored out of the weight vector due to regularization
        // This prevents having to scale the entire weight vector, which is dense
        // To handle properly, we have to account for this factoring in the model and update the
        // weight vector on use.
        wdiv /= (1 - eta * lambda);
        
        // This rescales for stability, may not be necessary
        if (wdiv > 1e5)
        {
            final double sf = 1.0 / wdiv;
            CollectionsManip.scaleInplace(weights, sf);
            wdiv = 1.;
        }
        weightModel.setWdiv(wdiv);

        // When we factored wdiv out, we have to account for this in our gradient update as well
        fv.updateWeights(weights, -eta * dLdp * wdiv);

        // This is referenced on Leon Bottou's SGD page
        double etab = eta * 0.01;
        double wbias = weightModel.getWbias();

        // The SGD code supports bias regularization only with an ifdef which defaults to off
        if (regularizedBias)
        {
            wbias *= (1 - etab * lambda);
        }
        wbias += -etab * dLdp;

        weightModel.setWbias(wbias);
    }

    @Override
    public final void preprocess(Model model, List<FeatureVector> sample)
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

        log.info("selected eta0=" + eta0);

    }

    private double evalEta(Model model, List<FeatureVector> sample, double eta)
    {
        WeightModel clone = (WeightModel)model.prototype();
        for (FeatureVector fv : sample)
        {
            trainOneWithEta(clone, fv, eta);
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
    public final void evalOne(Model model, FeatureVector fv, Metrics metrics)
    {

        double y = fv.getY();
        double fx = model.predict(fv);
        double loss = lossFunction.loss(fx, y);
        double error = (fx * y <= 0) ? 1 : 0;
        metrics.add(loss, error);
        ;
    }

    /**
     * Evaluate all examples
     * @param model The model to use for evaluation
     * @param testingExamples The examples
     * @param metrics Metrics to add to
     */
    @Override
    public final void eval(Model model, List<FeatureVector> testingExamples, Metrics metrics)
    {
        for (FeatureVector fv : testingExamples)
        {
            evalOne(model, fv, metrics);
        }

        WeightModel weightModel = (WeightModel)model;
        double normW = weightModel.mag();
        if (regularizedBias)
        {
            normW += weightModel.getWbias() * weightModel.getWbias();
        }
        double cost = metrics.getLoss() + 0.5 * lambda * normW;
        metrics.setCost(cost);

    }

}
