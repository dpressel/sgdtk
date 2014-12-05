package org.sgdtk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Model for classification
 *
 * @author dpressel
 */
public interface Model
{
    /**
     * Load model to a file
     *
     * @param file
     * @throws IOException
     */
    public void load(File file) throws IOException;

    /**
     * Save model to a file
     * @param file
     * @throws IOException
     */
    public void save(File file) throws IOException;

    /**
     * Load the model from the stream
     * @param inputStream stream
     * @throws IOException
     */
    void load(InputStream inputStream) throws IOException;

    /**
     * Save the model to a stream
     * @param outputStream stream
     * @throws IOException
     */
    void save(OutputStream outputStream) throws IOException;

    /**
     * Predict y given feature vector.  In a binary model, this predicts greater than 0 if true, and less than
     * 0 if false.  In a multi-class model, this predicts the label (with no score) of the best choice
     *
     * @param fv feature vector
     * @return prediction
     */
    double predict(FeatureVector fv);

    /**
     * Classify with scores behind each label.  The largest outcome label is the best guess
     * The index of the label is the category or level and the value is the score
     *
     * @param fv
     * @return
     */
    double[] score(FeatureVector fv);

    /**
     * Create a deep copy of this
     * @return clone
     */
    Model prototype();

}
