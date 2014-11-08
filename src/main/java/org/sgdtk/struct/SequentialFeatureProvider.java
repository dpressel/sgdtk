package org.sgdtk.struct;

import java.io.IOException;

/**
 * Interface for streaming {@link org.sgdtk.struct.FeatureVectorSequence} from a source to a sink
 *
 * @author dpressel
 */
public interface SequentialFeatureProvider
{
    /**
     * Get the next feature vector sequence from a source
     * @return A sequence or null if the source is consumed fully
     * @throws IOException
     */
    public FeatureVectorSequence next() throws IOException;
}
