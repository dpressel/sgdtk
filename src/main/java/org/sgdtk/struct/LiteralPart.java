package org.sgdtk.struct;

import java.util.List;

/**
 * This is handles string literals that are requested as part of a feature
 *
 * @author dpressel
 */
public class LiteralPart implements FeatureExtractorPart
{
    final String literal;

    /**
     * There is no order, since no x value
     * @return
     */
    @Override
    public int getOrder()
    {
        return 0;
    }

    /**
     * Constructor takes in a literal
     * @param literal
     */
    public LiteralPart(String literal)
    {
        this.literal = literal;
    }

    /**
     * Write this literal into the feature for our parent
     * @param states The states in the sequence
     * @param current The current absolute position
     * @return
     */
    @Override
    public String run(List<State> states, int current)
    {
        return literal;
    }
}
