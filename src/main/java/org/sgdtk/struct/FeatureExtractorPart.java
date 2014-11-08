package org.sgdtk.struct;

import java.util.List;

/**
 * Part of feature extractor (literal or extraction)
 *
 * This is the interface for extracting an atomic portion of the feature
 *
 * @author dpressel
 */
public interface FeatureExtractorPart
{
    /**
     * Get the order of this part
     * @return 1 if unigram, 2 if bigram
     */
    public int getOrder();

    /**
     * Extract a single feature at a position within the sequence.
     * This has access to the entire sequence to properly support building the context
     * window
     *
     * @param states The states in the sequence
     * @param current The current absolute position
     * @return
     */
    public String run(List<State> states, int current);
}
