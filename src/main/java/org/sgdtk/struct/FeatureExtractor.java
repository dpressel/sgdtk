package org.sgdtk.struct;

import java.util.List;

/**
 * Extracts a single feature from a sequence
 *
 * @author dpressel
 */
public class FeatureExtractor
{
    private final String ns;
    private final List<FeatureExtractorPart> parts;

    /**
     * Constructor, you would probably not need to do this under normal circumstances
     *
     * @param ns namespace
     * @param atoms some parts that, when used together will give us a whole feature
     */
    public FeatureExtractor(String ns, List<FeatureExtractorPart> atoms)
    {
        this.ns = ns;
        this.parts = atoms;
    }

    /**
     * Given a sequence of {@link org.sgdtk.struct.State} objects which are multi-dimensional,
     * made up of components themselves, and a position in that sequence, extract a single feature
     *
     * @param states A sequence
     * @param current The current absolute position in the sequence
     * @return
     */
    public String run(List<State> states, int current)
    {
        StringBuffer sb = new StringBuffer();
        for (FeatureExtractorPart part : parts)
        {
            String aPart = part.run(states, current);
            if (aPart == null)
                return null;
            sb.append(aPart);
        }
        return ns + sb.toString();
    }

    /**
     * The order of this feature is the max of all of its parts
     * @return The order (1 or 2)
     */
    public int getOrder()
    {
        int maxOrder = 0;
        for (FeatureExtractorPart part : parts)
        {
            maxOrder = Math.max(maxOrder, part.getOrder());
        }
        return maxOrder;
    }

    /**
     * Get all parts of the extractor
     * @return All parts
     */
    public List<FeatureExtractorPart> getParts()
    {
        return parts;
    }

    /**
     * Get the number of parts in the extractor
     * @return
     */
    public int size()
    {
        return parts.size();
    }


}
