package org.sgdtk.fileio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by dpressel on 10/22/15.
 */
public interface ConfigReader
{
    Config read(InputStream inputStream) throws IOException;
    Config read(File file) throws IOException;
}
