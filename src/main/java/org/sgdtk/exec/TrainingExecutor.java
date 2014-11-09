package org.sgdtk.exec;

import org.sgdtk.FeatureVector;
import org.sgdtk.Learner;
import org.sgdtk.Model;

import java.io.File;

/**
 * Simple interface for scheduling training data onto a queue and processing in overlapped fashion
 *
 * This can be cleaned up, but for now, its as simple as I could think of to embed VW-like capabilities
 * without having to have some main driver
 *
 * @author dpressel
 */
public interface TrainingExecutor
{
    /**
     * Initialize the executor.  This has to be done upfront, before a {@link org.sgdtk.exec.FeatureVectorProducer} is
     * created
     *
     * @param learner The learner
     * @param model The initialized but untrained model
     * @param numEpochs The number of epochs
     * @param cacheFile The cache file to use
     * @param bufferSize The size of the internal buffer to train from
     */
    void initialize(Learner learner, Model model, int numEpochs, File cacheFile, int bufferSize);

    /**
     * Start the processing
     */
    void start();

    /**
     * Join on the executor
     */
    void join();

    /**
     * Get the number of epochs
     * @return Number of epochs
     */
    int getNumEpochs();

    /**
     * Get the name of the cache file
     * @return Cache file
     */
    File getCacheFile();

    /**
     * Add a feature vector to the executor.  You only need to do this yourself if you arent using a
     * {@link org.sgdtk.exec.FeatureVectorProducer}, which will do this for you.  If you decide to use this
     * method directly, you should add the same vector once per epoch so the executor sees it each time.
     * When you are done loading an epoch, you should pass 'null' to this call to signal the end of an epoch
     * for the executor.  This function should block if there are no available slots for its underlying buffer.
     * This will create proper back-pressure and slow down the streaming.
     *
     * @see {@link FeatureVectorProducer#run()}
     *
     * @param featureVector feature vector
     */
    void add(FeatureVector featureVector);

}
