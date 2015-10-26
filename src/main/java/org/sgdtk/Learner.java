package org.sgdtk;

import java.util.List;

/**
 * A trainer for unstructured classification.
 *
 * @author dpressel
 */
public interface Learner
{
    /**
     * Create an empty but initialized model, with the length of the feature vector given
     * @param params Params needed to construct the model.  For the linear, this should be the length of feature vector
     * @return An empty but initialized model
     */
    Model create(Object params) throws Exception;

    //AsyncTrainingLifecycle initiate(Object params) throws Exception;

    /**
     * Train on a single pass
     * @param model The model to update
     * @param trainingExamples The training examples
     * @return The updated model
     */
    Model trainEpoch(Model model, List<FeatureVector> trainingExamples);

    void trainOne(Model model, FeatureVector fv);

    void preprocess(Model model, List<FeatureVector> sample);

    /**
     * Evaluate a single instance
     * @param model The model to use for evaluation
     * @param fv The feature vector
     * @param metrics Metrics to add to
     */
    void evalOne(Model model, FeatureVector fv, Metrics metrics);

    /**
     * Evaluate a set of examples
     * @param model The model to use for evaluation
     * @param testingExamples The examples
     * @param metrics Metrics to add to
     */
    void eval(Model model, List<FeatureVector> testingExamples, Metrics metrics);

}
