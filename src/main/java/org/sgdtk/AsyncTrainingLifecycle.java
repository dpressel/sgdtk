package org.sgdtk;

import java.io.IOException;

/**
 * Created by dpressel on 10/23/15.
 */
public interface AsyncTrainingLifecycle
{
    void add(FeatureVector fv) throws IOException;

    Model finish() throws IOException;
}
