package org.sgdtk.io;

import org.sgdtk.FeatureProvider;
import org.sgdtk.FeatureVector;

import java.io.File;
import java.io.IOException;
import java.util.List;


public interface DatasetReader extends FeatureProvider
{
    List<FeatureVector> load(File... file) throws IOException;
    /**
     * Open a file for reading.  All files are read only up to maxFeatures.
     * @param file(s) Some files to read
     * @throws IOException
     */
    void open(File... file) throws IOException;

    /**
     * Close the currently loaded file
     * @throws IOException
     */
    void close() throws IOException;
}
