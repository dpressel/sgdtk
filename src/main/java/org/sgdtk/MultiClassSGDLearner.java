package org.sgdtk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handle multi-class one-vs-all training.
 *
 * @author dpressel
 */
public class MultiClassSGDLearner implements Learner
{
    SGDLearner[] learners;

    private static final Logger log = LoggerFactory.getLogger(SGDLearner.class);

    Loss lossFunction;
    double lambda;
    double eta0;

    /**
     * Default constructor, use hinge loss
     */
    public MultiClassSGDLearner(int numClasses)
    {
        this(numClasses, new HingeLoss());
    }

    /**
     * Constructor with a loss function
     * @param loss loss function
     */
    public MultiClassSGDLearner(int numClasses, Loss loss)
    {
        this(numClasses, loss, 1e-5);
    }


    /**
     * Constructor with loss function, regularization param
     * @param loss loss function
     * @param lambda regularization param
     */
    public MultiClassSGDLearner(int numClasses, Loss loss, double lambda)
    {
        this(numClasses, loss, lambda, -1.);
    }

    public MultiClassSGDLearner(int numClasses, Loss loss, double lambda, double kEta)
    {
        this.lossFunction = loss;
        this.lambda = lambda;
        this.eta0 = kEta;

        learners = new SGDLearner[numClasses];
    }

    @Override
    public Model create(int wlength)
    {

        Model[] models = new Model[learners.length];
        for (int i = 0; i < learners.length; ++i)
        {
            learners[i] = new SGDLearner(lossFunction, lambda, eta0);
            models[i] = learners[i].create(wlength);
        }
        return new MultiClassLinearModel(models);
    }

    @Override
    public Model trainEpoch(Model model, List<FeatureVector> trainingExamples)
    {
        // Check if we have eta set already

        if (eta0 <= 0)
        {
            preprocess(model, trainingExamples.subList(0, Math.min(1000, trainingExamples.size())));
        }

        for (FeatureVector fv : trainingExamples)
        {
            trainOne(model, fv);
        }

        return model;
    }

    @Override
    public void trainOne(Model model, FeatureVector fv)
    {
        MultiClassLinearModel mclm = (MultiClassLinearModel)model;

        int yReal = fv.getY();
        for (int i = 0; i < mclm.models.length; ++i)
        {
            boolean isCorrect = (i + 1) == yReal;
            fv.setY(isCorrect ? 1: -1);
            learners[i].trainOne(mclm.models[i], fv);
        }
        fv.setY(yReal);
    }

    @Override
    public void preprocess(Model model, List<FeatureVector> sample)
    {
        int [] yReal = new int[sample.size()];
        for (int i = 0; i < yReal.length; ++i)
        {
            yReal[i] = sample.get(i).getY();
        }

        MultiClassLinearModel mclm = (MultiClassLinearModel)model;

        for (int i = 0; i < mclm.models.length; ++i)
        {
            for (int j = 0; j < yReal.length; ++j)
            {
                boolean isCorrect = (i + 1) == yReal[j];
                sample.get(j).setY(isCorrect ? 1 : -1);
            }

            learners[i].preprocess(mclm.models[i], sample);
        }
        for (int j = 0; j < yReal.length; ++j)
        {
            sample.get(j).setY(yReal[j]);
        }

    }

    @Override
    public void evalOne(Model model, FeatureVector fv, Metrics metrics)
    {
        MultiClassLinearModel mclm = (MultiClassLinearModel)model;

        int yReal = fv.getY();

        for (int i = 0; i < mclm.models.length; ++i)
        {
            boolean isCorrect = (i + 1) == yReal;
            fv.setY(isCorrect ? 1: -1);

            learners[i].evalOne(mclm.models[i], fv, isCorrect ? metrics : new Metrics());
        }
        fv.setY(yReal);

    }

    @Override
    public void eval(Model model, List<FeatureVector> testingExamples, Metrics metrics)
    {
        for (int i = 0, sz = testingExamples.size(); i < sz; ++i)
        {
            FeatureVector fv = testingExamples.get(i);
            evalOne(model, fv, metrics);
        }
    }
}
