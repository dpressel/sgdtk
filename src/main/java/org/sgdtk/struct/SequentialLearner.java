package org.sgdtk.struct;

import org.sgdtk.Metrics;

import java.util.List;

/**
 * Training class for {@link org.sgdtk.struct.SequentialModel}
 *
 * This sets up the interface for online gradient descent type algorithms, which train in passes or epochs.
 * Of course, its possible that we could have some non-SGD like algorithm which could be done as a single pass, but
 * here the main concern is SGD.
 *
 * @author dpressel
 */
public interface SequentialLearner
{
    /**
     * This starts us off with a model.  We just need to know the number of features (or feature width), which we
     * can typically ask the {@link org.sgdtk.struct.JointFixedFeatureNameEncoder} for
     * @param wlength The number of features
     * @param numLabels The number of labels
     * @return An empty, but initialized model
     */
    SequentialModel create(int wlength, int numLabels);

    /**
     * Train a single pass of the model on these examples -- no shuffling is done internally, its assumed this would
     * be handled by the caller.  On the first pass, typically the learning schedule will be adjusted internally.
     * This is hidden from the API/end user for simplicity.
     *
     * @param model A sequential model, which will be empty on the first pass, but will have valued weights on future passes
     * @param trainingExamples A set of training examples
     * @return
     */
    SequentialModel trainEpoch(SequentialModel model, List<FeatureVectorSequence> trainingExamples);

    /**
     * Evaluate a single feature vector using a model.  This is basically wrapping
     * the {@link org.sgdtk.struct.SequentialModel#predict(FeatureVectorSequence)} call, but with some error metrics
     * accounted for as well.
     *
     * @see org.sgdtk.exec.EvalStruct#evalOneMaybePrint(SequentialLearner, SequentialModel, FeatureVectorSequence, org.sgdtk.FeatureNameEncoder, org.sgdtk.Metrics)
     * @param model A model
     * @param sequence A feature vector sequence
     * @param metric Error information to append to
     * @return The most likely label path through the sequence
     */
    Path evalOne(SequentialModel model, FeatureVectorSequence sequence, Metrics metric);

    /**
     * Evaluate some test data
     * @param model A model
     * @param testingExamples A set of feature vector sequences
     * @param metrics Error information to accrue
     */
    void eval(SequentialModel model, List<FeatureVectorSequence> testingExamples, Metrics metrics);
}
