package org.sgdtk.struct;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A Sequential classifier model
 *
 * @author dpressel
 */
public interface SequentialModel
{
    /**
     * Load from a stream
     * @param inputStream stream
     * @throws IOException
     */
    void load(InputStream inputStream) throws IOException;

    /**
     * Save to a stream
     * @param outputStream stream
     * @throws IOException
     */
    void save(OutputStream outputStream) throws IOException;

    /**
     * Pick the best classification for a hidden state sequence
     * @param sequence feature vector sequence
     * @return
     */
    Path predict(FeatureVectorSequence sequence);

    /**
     * Create a deep copy of this
     * @return clone
     */
    SequentialModel prototype();
}
