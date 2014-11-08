package org.sgdtk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Basic interface for encoding a feature to an index.
 *
 * The feature encoder converts features to indices, and back.  Additionally, feature encoders can
 * be saved and retrieved so that during classification the same features are guaranteed to be used.
 * Note that some implementations, like a hashed version cannot map back.
 *
 * @author dpressel
 *
 */
public interface FeatureNameEncoder extends Serializable
{
    /**
     * Load from serialized state
     * @param inputStream source
     * @throws IOException
     */
    public void load(InputStream inputStream) throws IOException;

    /**
     * Serialize
     * @param outputStream target
     * @throws IOException
     */
    public void save(OutputStream outputStream) throws IOException;

    /**
     * find this feature's index, or create (if possible).  Creation only works on implementations that support
     * lazy creation.
     * @param name feature name
     * @return feature index or -1 if it cannot be created
     */
    int lookupOrCreate(String name);

    /**
     * Find the feature's index if it exists
     * @param name feature name
     * @return feature index or null
     */
    Integer indexOf(String name);

    /**
     * Get the feature name for this index
     * @param fvOffset index
     * @return name
     * @throws UnsupportedOperationException
     */
    String nameOf(Integer fvOffset) throws UnsupportedOperationException;

    /**
     * Get the number of features
     * @return length
     */
    int length();
}
