package org.sgdtk.struct;

import java.util.ArrayList;
import java.util.List;

/**
 * An in-memory representation of the collection of {@link org.sgdtk.struct.FeatureExtractor}
 *
 * @author dpressel
 */
public class FeatureTemplate
{
    private List<FeatureExtractorInterface> extractors;

    /**
     * Construct a feature template, we try and attempt to size the list to a likely initial size
     */
    public FeatureTemplate()
    {
        // Most likely next power of 2
        this.extractors = new ArrayList<FeatureExtractorInterface>(32);
    }

    /**
     * Construct a template from a pre-existing list of extractors
     * @param featureExtractors The list
     */
    public FeatureTemplate(List<FeatureExtractorInterface> featureExtractors)
    {
        this.extractors = featureExtractors;
    }

    /**
     * Get the list of extractors
     * @return The list
     */
    public List<FeatureExtractorInterface> getExtractors()
    {
        return extractors;
    }

    /**
     * Set the list of extractors
     * @param featureExtractors The list
     */
    public void setExtractors(List<FeatureExtractorInterface> featureExtractors)
    {
        this.extractors = featureExtractors;
    }

    /**
     * Add a single extractor
     * @param extractor An extractor
     */
    public void addExtractor(FeatureExtractorInterface extractor)
    {
        this.extractors.add(extractor);
    }
}
