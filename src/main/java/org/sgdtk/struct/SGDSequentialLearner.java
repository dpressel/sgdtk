package org.sgdtk.struct;

import org.sgdtk.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Use SGD to train a CRF using Leon Bottou's approach
 *
 * This implementation follows Leon Bottou's approach for crfsgd.cpp.  In the first pass of training,
 * if kEta is not provided, it will attempt to iteratively pick the learning rate.  This is approach is
 * described in Bottou's tutorials/writings on SGD.
 *
 * @author dpressel
 */
public class SGDSequentialLearner implements SequentialLearner
{
    private static final Logger log = LoggerFactory.getLogger(
            SGDSequentialLearner.class);

    // This is the schedule for adjusting the learning rate
    double t = 0;
    double lambda = -1;
    final double c;
    final double kEta;

    /**
     * Create a learner by providing params
     * @param c capacity control parameter
     * @param kEta user eta value
     */
    public SGDSequentialLearner(double c, double kEta)
    {
        this.c = c;
        this.kEta = kEta;
    }

    /**
     * Default constructor, which will lead to iteratively picking eta
     */
    public SGDSequentialLearner()
    {
        this(1, 0);
    }

    /**
     * Create an empty CRF model
     * @param wlength The number of features
     * @param numLabels The number of labels
     * @return an empty but initialized model for training
     */
    @Override
    public SequentialModel create(int wlength, int numLabels)
    {
        t = 0;
        lambda = -1;
        CRFModel crf = new CRFModel(wlength, 1., numLabels);
        return crf;
    }

    public static final double ETA_0 = 0.1;

    /**
     * Train a single pass
     * @param model A sequential model, which will be empty on the first pass, but will have valued weights on future passes
     * @param trainingExamples A set of training examples
     * @return updated model
     */
    @Override
    public SequentialModel trainEpoch(SequentialModel model, List<FeatureVectorSequence> trainingExamples)
    {
        CRFModel crf = (CRFModel)model;

        if (lambda == -1)
        {
            init(trainingExamples, crf);
            double eta = 1. / (lambda * t);
            log.info("Taking eta=" + eta + " t0=" + t);
        }

        for (FeatureVectorSequence sequence : trainingExamples)
        {
            double eta = 1. / (lambda * t);
            double wscale = crf.getWscale();
            Scorer scorer = new Scorer(crf, sequence);
            scorer.gradCorrect(1, eta);
            scorer.gradForward(-1, eta);
            wscale *= (1 - eta * lambda);
            crf.setWscale(wscale);
            ++t;
        }


        if (crf.getWscale() < 1e-5)
        {
            crf.rescale();
        }
        double wnorm = crf.mag();
        log.info("wnorm=" + wnorm);
        return crf;
    }

    public static final int MAX_SAMPLE_SZ = 1000;

    private void init(List<FeatureVectorSequence> trainingExamples, CRFModel model)
    {

        double s0 = System.currentTimeMillis();
        t = trainingExamples.size();
        lambda = 1.0 / (c * t);
        if (kEta != 0.0)
        {

            t = 1.0 / (kEta * lambda);
        }
        else
        {
            // Otherwise find it
            initSchedule(trainingExamples.subList(0, Math.min(MAX_SAMPLE_SZ, trainingExamples.size())), model, ETA_0);
        }

        double sNow = System.currentTimeMillis();

        log.info("Initialized in " + (sNow - s0) / 1000. + "s");
    }

    private double findObjBySampling(List<FeatureVectorSequence> sample, CRFModel model)
    {
        double loss = 0.;
        int sSz = sample.size();
        double wnorm = model.mag();
        log.info("wnorm=" + wnorm);
        for (int i = 0; i < sSz; ++i)
        {
            Scorer scorer = new Scorer(model, sample.get(i));
            double forward = scorer.computeForward();
            double correct = scorer.computeCorrect();
            loss += forward - correct;

        }
        return loss / sSz + 0.5 * wnorm * lambda;
    }

    private double tryEtaBySampling(List<FeatureVectorSequence> sample, CRFModel model, double eta)
    {


        int sSz = sample.size();
        for (int i = 0; i < sSz; ++i)
        {
            Scorer scorer = new Scorer(model, sample.get(i));
            scorer.gradCorrect(1, eta);
            scorer.gradForward(-1, eta);
            model.setWscale(model.getWscale() * (1 - eta * lambda));

        }

        return findObjBySampling(sample, model);

    }

    public static final double FACTOR = 2.;
    public static final double BEST_ETA_INIT = 1.;

    private void initSchedule(List<FeatureVectorSequence> sample, CRFModel model, double eta0)
    {
        double obj0 = findObjBySampling(sample, model);

        log.info("Initial objective=" + obj0);
        double bestEta = BEST_ETA_INIT;
        double bestObj = obj0;
        double etaGuess = eta0;

        boolean phase2 = false;

        for (int k = 10; k > 0 || !phase2; )
        {
            CRFModel clone = (CRFModel)model.prototype();
            double obj = tryEtaBySampling(sample, clone, etaGuess);
            boolean ok = (obj < obj0);

            log.info("Trying eta=" + etaGuess + " obj=" + obj);
            if (ok)
            {
                log.info(" (possible)");
            }
            else
            {
                log.info("(too large)");
            }
            if (ok)
            {
                --k;
                if (obj < bestObj)
                {
                    bestObj = obj;

                    bestEta = etaGuess;
                }
            }
            if (!phase2)
            {
                if (ok)
                {
                    etaGuess *= FACTOR;
                }
                else
                {
                    phase2 = true;
                    etaGuess = eta0;
                }
            }
            if (phase2)
            {
                etaGuess /= FACTOR;
            }

        }
        bestEta /= FACTOR;
        t = 1.0 / (bestEta * lambda);


    }

    /**
     * Evaluate a single sequence using a CRF model
     * @param model A model
     * @param sequence A feature vector sequence
     * @param metric Error information to append to
     * @return The best classification for each step
     */
    @Override
    public Path evalOne(SequentialModel model, FeatureVectorSequence sequence, Metrics metric)
    {
        Path path = model.predict(sequence);
        int nErrors = 0;

        int nPos = sequence.length();
        for (int pos = 0; pos < nPos; ++pos)
        {
            int yGuess = path.at(pos);
            int yActual = sequence.getY(pos);

            if (yGuess != yActual)
            {
                ++nErrors;
            }
        }
        metric.addToTotalError(nErrors);
        metric.addToTotalEvents(sequence.length());
        metric.addToTotalExamples(1);
        return path;
    }

    /**
     * Evaluate all sequences
     * @param model A model
     * @param testingExamples A set of feature vector sequences
     * @param metrics Error information to accrue
     */
    @Override
    public void eval(SequentialModel model, List<FeatureVectorSequence> testingExamples, Metrics metrics)
    {
        for (FeatureVectorSequence sequence : testingExamples)
        {
            evalOne(model, sequence, metrics);
        }

    }
}
