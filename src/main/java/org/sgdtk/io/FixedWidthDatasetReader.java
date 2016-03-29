package org.sgdtk.io;

import org.sgdtk.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Produce a (currently sparse) training example from a TSV file
 *
 * @author dpressel
 */
public class FixedWidthDatasetReader implements DatasetReader
{

    HashFeatureEncoder hashFeatureEncoder;

    FeatureNameEncoder labelEncoder;

    @Override
    public int getLargestVectorSeen()
    {
        return hashFeatureEncoder.length();
    }


    public FixedWidthDatasetReader() throws IOException
    {
        this(null);
    }
    public FixedWidthDatasetReader(FeatureNameEncoder labelEncoder) throws IOException
    {
        this(labelEncoder, 24);
    }
    public FixedWidthDatasetReader(FeatureNameEncoder labelEncoder, int nbits) throws IOException
    {

        this.labelEncoder = labelEncoder == null ? new LazyFeatureDictionaryEncoder(): labelEncoder;
        this.hashFeatureEncoder = new HashFeatureEncoder(nbits);
    }

    BufferedReader reader;

    /**
     * Open a file for reading.  All files are read only up to maxFeatures.
     *
     * @param file An SVM light type file
     * @throws IOException
     */
    @Override
    public final void open(File... file) throws IOException
    {
        reader = new BufferedReader(new FileReader(file[0]));
    }

    /**
     * Close the currently loaded file
     *
     * @throws IOException
     */
    @Override
    public final void close() throws IOException
    {
        reader.close();
    }

    /**
     * Slurp the entire file into memory.  This is not the recommended way to read large datasets, use
     * {@link #next()} to stream features from the file one by one.
     *
     * @param file An SVM light type file
     * @return One feature vector per line in the file
     * @throws IOException
     */
    public final List<FeatureVector> load(File... file) throws IOException
    {

        open(file);

        List<FeatureVector> fvs = new ArrayList<FeatureVector>();

        FeatureVector fv;

        while ((fv = next()) != null)
        {
            fvs.add(fv);
        }

        close();
        // Read a line in, and then hash it into the bin vector
        return fvs;
    }

    /**
     * Get the next feature vector in the file
     *
     * @return The next feature vector, or null, if we are out of lines
     * @throws IOException
     */
    public final FeatureVector next() throws IOException
    {

        final String line = reader.readLine();

        if (line == null)
        {
            return null;
        }

        final StringTokenizer tokenizer = new StringTokenizer(line, " \t");

        String strLabel = tokenizer.nextToken();
        Integer label;
        try
        {
            label = Integer.valueOf(strLabel);
        }
        catch (NumberFormatException numEx)
        {
            label = labelEncoder.indexOf(strLabel);
            if (label == null)
            {
                return next();
            }
            // This is due to the zero offset assigned by the lazy encoder, we want 1-based
            label++;
        }

        // Parse a sentence and place results in a continuous Bag of Words
        SparseVectorN x = new SparseVectorN();

        while (tokenizer.hasMoreTokens())
        {
            String word = tokenizer.nextToken().toLowerCase();
            int idx = hashFeatureEncoder.indexOf(word);
            x.add(new Offset(idx, 1.0));
        }

        final FeatureVector fv = new FeatureVector(label, x);
        fv.getX().organize();
        return fv;
    }

}
