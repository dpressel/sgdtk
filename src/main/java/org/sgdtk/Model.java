package org.sgdtk;

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
     * Predict y given feature vector
     * @param fv feature vector
     * @return prediction
     */
    double predict(FeatureVector fv);

    /**
     * Create a deep copy of this
     * @return clone
     */
    Model prototype();

}
