package org.sgdtk;

import org.sgdtk.exec.TrainingEventListener;

import java.io.IOException;

/**
 * Created by dpressel on 10/23/15.
 */
public interface AsyncTrainingRunner
{
    void add(FeatureVector fv) throws IOException;

    void start() throws Exception;

    Model finish() throws IOException;

    void addListener(TrainingEventListener listener);
}
