package org.sgdtk.io;

import org.sgdtk.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Produce a sum of BoW features
 * <p>
 * This is going to add the component wise features which will basically be a pre-projection layer and will cause a small
 * number of inputs.
 *
 * @author dpressel
 */
public class SumWordVecDatasetReader implements DatasetReader
{

    Word2VecModel word2vecModel;
    private long embeddingSize;
    FeatureNameEncoder labelEncoder;

    @Override
    public int getLargestVectorSeen()
    {
        return (int) embeddingSize;
    }


    public SumWordVecDatasetReader(String embeddings) throws IOException
    {
        this(embeddings, null);
    }
    public SumWordVecDatasetReader(String embeddings, FeatureNameEncoder labelEncoder) throws IOException
    {
        word2vecModel = Word2VecModel.loadWord2VecModel(embeddings);
        this.embeddingSize = word2vecModel.getSize();
        this.labelEncoder = labelEncoder;
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

        // If there is no label encoder, then just absorb this value
        String strLabel = tokenizer.nextToken();
        Integer label;
        if (labelEncoder == null)
        {
            label = Integer.valueOf(strLabel);
        }
        else // Use the label encoder
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
        DenseVectorN x = new DenseVectorN((int)embeddingSize);
        ArrayDouble xArray = x.getX();
        while (tokenizer.hasMoreTokens())
        {
            String word = tokenizer.nextToken().toLowerCase();

            float[] wordVector = word2vecModel.getVec(word);
            for (int j = 0, sz = xArray.size(); j < sz; ++j)
            {
                float fj = wordVector[j];
                boolean hadNaN = false;
                if (Float.isNaN(fj))
                {
                    hadNaN = true;
                    fj = 0;
                    //throw new IOException("Unexpected NaN");
                }
                if (hadNaN)
                {
                    System.err.println("NaN word: " + word);
                }
                xArray.addi(j, fj);
            }
        }

        final FeatureVector fv = new FeatureVector(label, x);
        fv.getX().organize();
        return fv;
    }

    public int getEmbeddingSize()
    {
        return (int)embeddingSize;
    }

    public void setEmbeddingSize(int embeddingSize)
    {
        this.embeddingSize = embeddingSize;
    }
}
