package org.sgdtk.fileio;

import org.sgdtk.FeatureProvider;
import org.sgdtk.FeatureVector;
import org.sgdtk.Offset;
import org.sgdtk.SparseFeatureVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This reads in Sparse SVM light/Libsvm format data a stream (via a pull).
 *
 * We require a width to be provided for the feature vector.  This should be large enough to contain the vector.
 * This class is pretty quick and dirty, as its assumed that real-life problems will be more complex, and warrant
 * a different methodology and perhaps a {@link org.sgdtk.FeatureNameEncoder}, but for pre-processed sample data
 * which is already in vector form, encoding the features is not necessary, so this implementation can be trivial.
 *
 * @author dpressel
 */
public class SVMLightFileFeatureProvider implements FeatureProvider
{
    int largestVectorSeen = 0;

    @Override
    public int getLargestVectorSeen()
    {
        return largestVectorSeen;
    }

    public static class Dims
    {
        public final int width;
        public final int height;
        public Dims(int width, int height)
        {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * If you want to know the dimensions of an SVM light file, you can call this method, and it will give back
     * the number of vectors (as the height), and required feature vector size as the width to encompass all examples.
     *
     * @param file An SVM light type file
     * @return Number of feature vectors by number of features in feature vector
     * @throws IOException
     */
    public static Dims findDims(File file) throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        // If we arent going to hash or pick an arbitrary end in this format, we actually have to
        // read the file twice!
        int lastIdxTotal = 0;
        int n = 0;
        while ((line = reader.readLine()) != null)
        {
            List<String> strings = Arrays.asList(line.split(" "));
            String end = strings.get(strings.size() - 1);
            int lastIdx = Integer.valueOf(end.split(":")[0]);
            lastIdxTotal = Math.max(lastIdx, lastIdxTotal);
            ++n;
        }
        reader.close();
        Dims dims = new Dims(lastIdxTotal+1, n);
        return dims;
    }


    /**
     * Create a provider, and you need to cap it with a max number of features.
     * Anything beyond this feature vector length will be clipped out of the resultant FV.
     *
     * @param maxFeatures The feature vector width.
     */
    public SVMLightFileFeatureProvider(int maxFeatures)
    {
        this.maxFeatures = maxFeatures;
    }

    public SVMLightFileFeatureProvider()
    {
        this(0);
    }

    final int maxFeatures;

    BufferedReader reader;

    /**
     * Open a file for reading.  All files are read only up to maxFeatures.
     * @param file An SVM light type file
     * @throws IOException
     */
    public final void open(File file) throws IOException
    {
        //largestVectorSeen = 0;
        reader = new BufferedReader(new FileReader(file));
    }

    /**
     * Close the currently loaded file
     * @throws IOException
     */
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
    public final List<FeatureVector> load(File file) throws IOException
    {

        open(file);

        List<FeatureVector> fvs = new ArrayList<FeatureVector>();
        //CRSFactory factory = new CRSFactory();

        FeatureVector fv;

        while ((fv = next()) != null)
        {
            fvs.add( fv );
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

        // This appears to be much faster than
        final StringTokenizer tokenizer = new StringTokenizer(line, " ");
        final int lastIdxTotal = maxFeatures - 1;
        final int label = Integer.valueOf(tokenizer.nextToken());
        final SparseFeatureVector fv = new SparseFeatureVector(label);
        while (tokenizer.hasMoreTokens())
        {

            String subVec = tokenizer.nextToken();
            final int to = subVec.indexOf(':');
            final int idx = Integer.valueOf(subVec.substring(0, to));
            largestVectorSeen = Math.max(largestVectorSeen, idx + 1);
            if (lastIdxTotal > 0 && idx > lastIdxTotal)
                continue;

            final double value = Double.valueOf(subVec.substring(to+1));
            fv.add(new Offset(idx, value));
        }

        return fv;
    }

}
