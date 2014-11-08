package org.sgdtk.struct;

import java.util.List;

/**
 * Extract part of a feature.
 *
 * Features can be made up of more than one component, and possibly literals.  This class represents one of these
 * contributing to the overall feature name.
 *
 * @author dpressel
 */
public class ExtractPart implements FeatureExtractorPart
{
    private final int nGram;
    private final int relPos;
    private final int index;

    public static final String[] BOS = {"_B-4", "_B-3", "_B-2", "_B-1"};
    public static final String[] EOS = {"_B+1", "_B+2", "_B+3", "_B+4"};

    /**
     * Constructor
     *
     * @param nGram 1=U, 2=B
     * @param relPos position is relative (0 is the center)
     * @param index index decides which component (e.g., for CONLL 0=word 1=pos 2=chunk) to extract
     */
    public ExtractPart(int nGram, int relPos, int index)
    {
        this.nGram = nGram;
        this.relPos = relPos;
        this.index = index;
    }

    private String atPosSafe(List<State> states, int absolutePos)
    {

        if (absolutePos < 0)
        {
            return BOS[BOS.length + absolutePos];
        }
        int sSz = states.size();

        if (absolutePos >= sSz)
        {
            int off = absolutePos - sSz;
            return EOS[off];
        }
        // Otherwise do what we were going to do before
        return states.get(absolutePos).atIndex(index);
    }

    /**
     * Extracts a feature from some components
     *
     * @param states
     * @param current
     * @return
     */
    public String run(List<State> states, int current)
    {
        int absolutePos = current + relPos;

        // This is a bit of a cheat for now
        // until I have a better understanding of my options here
        if (nGram > 1)
        {
            return "_" + nGram + "_";
        }

        return atPosSafe(states, absolutePos);



    }

    @Override
    public int getOrder()
    {
        return nGram;
    }
}
