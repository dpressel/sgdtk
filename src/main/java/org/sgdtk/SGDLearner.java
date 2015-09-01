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

    LearningRateSchedule learningRateSchedule;
    Loss lossFunction;
    double lambda;
    double eta0 = -1;
    ///double numSeenTotal = 0;
    //boolean regularizedBias = false;
    private ModelFactory modelFactory = null;
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
        this(loss, lambda, kEta, new LinearModelFactory());
    }

    public SGDLearner(Loss loss, double lambda, double kEta, ModelFactory modelFactory)
    {
        this(loss, lambda, kEta, modelFactory, new RobbinsMonroUpdateSchedule());
    }

    public SGDLearner(Loss loss, double lambda, double kEta, ModelFactory modelFactory, LearningRateSchedule lrUpStrategy)
    {
        this.lossFunction = loss;
        this.lambda = lambda;
        this.eta0 = kEta;
        ///this.numSeenTotal = 0;
        this.modelFactory = modelFactory;
        this.learningRateSchedule = lrUpStrategy;

    }

    /**
     * Create the model, empty but initialized
     * @param wlength The length of the feature vector
     * @return
     */
    @Override
    public final Model create(int wlength)
    {
        ///numSeenTotal = 0;
        learningRateSchedule.reset(eta0, lambda);
        Model lm = modelFactory.newInstance(wlength);
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
        //if (regularizedBias)
        //{
        //    normW += lm.getWbias()*lm.getWbias();
        //}
        log.info("wnorm=" + normW);
        return model;
    }

    @Override
    public final void trainOne(Model model, FeatureVector fv)
    {
        WeightModel weightModel = (WeightModel)model;

        double eta = learningRateSchedule.update();
        /// Robbins-Monro update
        /// double eta = eta0 / (1 + lambda * eta0 * numSeenTotal);
        double y = fv.getY();
        double fx = weightModel.predict(fv);
        double dLoss = lossFunction.dLoss(fx, y);

        weightModel.updateWeights(fv.getX(), eta, lambda, dLoss, y);
        /// ++numSeenTotal;

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
            double y = fv.getY();
            double fx = clone.predict(fv);
            double dLoss = lossFunction.dLoss(fx, y);
            clone.updateWeights(fv.getX(), eta, lambda, dLoss, y);
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

        double[] scores = model.score(fv);
        double fx = scores[0];

        // True in binary case
        double error = (fx * y <= 0) ? 1 : 0;
        int best = 0;

        // If multi-class
        if (scores.length > 1)
        {
            // Support multi-label.  Assume for now that the cost function is going to want as input only the
            // index of the correct value. We can check that they are the same by testing the index.
            double maxv = -100000.0;

            for (int i = 0; i < scores.length; ++i)
            {
                if (scores[i] > maxv)
                {
                    // Label enum is index + 1
                    best = i + 1;
                    maxv = scores[i];
                }
            }
            if (best != y)
            {
                error = 1;
            }
            fx = scores[(int)y];
        }
        double loss = lossFunction.loss(fx, y);

        metrics.add(loss, error);
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
        //if (regularizedBias)
        //{
        //    normW += weightModel.getWbias() * weightModel.getWbias();
        //}
        double cost = metrics.getLoss() + 0.5 * lambda * normW;
        metrics.setCost(cost);

    }

    public ModelFactory getModelFactory()
    {
        return modelFactory;
    }

    public void setModelFactory(ModelFactory modelFactory)
    {
        this.modelFactory = modelFactory;
    }
}
