package org.sgdtk.exec;

import org.sgdtk.FeatureVector;
import org.sgdtk.Learner;
import org.sgdtk.Model;

import java.io.File;
import java.util.List;

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
     * Initialize the executor.
     *
     * @param learner The learner
     * @param model The initialized but untrained model
     * @param numEpochs The number of epochs
     * @param cacheFile The cache file to use
     * @param bufferSize The size of the internal buffer to train from
     * @param listeners Any listeners which should be triggered on key lifecycle events
     */
    void initialize(Learner learner, Model model, int numEpochs, File cacheFile, int bufferSize, List<TrainingEventListener> listeners);

    /**
     * Start the processing
     */
    void start();

    /**
     * Join on the executor
     */
    void join();

    void kill();
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
     * Add a feature vector to the executor. This function should block if there are no available slots for its underlying buffer.
     * This will create proper back-pressure and slow down the streaming.
     * @param featureVector feature vector
     */
    void add(FeatureVector featureVector);

}
