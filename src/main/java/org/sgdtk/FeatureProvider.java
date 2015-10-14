package org.sgdtk;

import java.io.IOException;

/**
 * Streaming interface for getting a FeatureVector back
 *
 * @author dpressel
 */
public interface FeatureProvider
{
    /**
     * Get the next feature vector from the source
     * @return feature vector or null of end of stream reached
     * @throws IOException
     */
    public FeatureVector next() throws IOException;

    public int getLargestVectorSeen();
}
