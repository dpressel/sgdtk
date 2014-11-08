package org.sgdtk.struct;

import org.sgdtk.CollectionsManip;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * CRF implementation of a {@link org.sgdtk.struct.SequentialModel}
 *
 * Provides the represention of just the CRF model itself, including the weight vector,
 * and the methods for persisting and restoring its weights, along with predicting a y for an x (fv)
 *
 * @author dpressel
 */
public class CRFModel implements SequentialModel
{

    private CRFModel(double[] weights, double wscale, int numLabels)
    {
        this.weights = new double[weights.length];
        System.arraycopy(weights, 0, this.weights, 0, weights.length);
        this.numLabels = numLabels;
        this.wscale = wscale;
    }

    /**
     * Default constructor.  This is usually only going to be called prior to a {@link #load(java.io.InputStream)} call
     */
    public CRFModel()
    {

    }

    /**
     * Construct a model prior to training. This just establishes the extent of the weight vector and the number of
     * sequence y values (labels).  Dont use this unless you understand what you are doing
     *
     * @param wlength This is the weight vector's width
     * @param wscale scaling
     * @param numLabels number of labels
     */
    public CRFModel(int wlength, double wscale, int numLabels)
    {

        this.weights = new double[wlength];
        Arrays.fill(weights, 0.);
        this.wscale = wscale;
        this.numLabels = numLabels;

    }

    /**
     * Load from a stream.  This is how you would load a model to use the classifier.
     *
     * @param inputStream
     * @throws IOException
     */
    @Override
    public void load(InputStream inputStream) throws IOException
    {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        wscale = objectInputStream.readDouble();
        numLabels = (int) objectInputStream.readLong();
        int length = (int) objectInputStream.readLong();
        weights = new double[length];
        for (int i = 0; i < length; ++i)
        {
            weights[i] = objectInputStream.readDouble();
        }
        objectInputStream.close();
    }

    /**
     * Save the weight vector, etc to a stream
     *
     * @param outputStream Stream to save to
     * @throws IOException
     */
    @Override
    public void save(OutputStream outputStream) throws IOException
    {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeDouble(wscale);
        objectOutputStream.writeLong((long)numLabels);
        objectOutputStream.writeLong((long)weights.length);
        for (int i = 0; i < weights.length; ++i)
        {
            objectOutputStream.writeDouble(weights[i]);
        }
        objectOutputStream.close();
    }

    private double[] weights;
    private double wscale;
    private int numLabels;

    /**
     * Use viterbi algorithm to find best path.  Note that this method does not hydrate the results, that would
     * violate the intended separation of concerns.  However, hydrating the results its shown in demonstration code
     * @see org.sgdtk.exec.EvalStruct#evalOneMaybePrint(SequentialLearner, SequentialModel, FeatureVectorSequence, org.sgdtk.FeatureNameEncoder, org.sgdtk.Metrics)
     * @param sequence The sequence to predict.
     */
    @Override
    public Path predict(FeatureVectorSequence sequence)
    {
        Scorer scorer = new Scorer(this, sequence);
        return scorer.viterbi();
    }


    /**
     * Create a deep copy of this exact model
     *
     * @return A clone
     */
    @Override
    public SequentialModel prototype()
    {
        return new CRFModel(weights, wscale, numLabels);
    }

    /**
     * Get weights
     * @return weights
     */
    public double[] getWeights()
    {
        return weights;
    }

    /**
     * Get wscale
     * @return wscale
     */
    public double getWscale()
    {
        return wscale;
    }

    /**
     * Get w' w scaled
     * @return mag
     */
    public double mag()
    {
        double dotProd = CollectionsManip.dot(weights, weights);
        return dotProd * wscale * wscale;
    }

    /**
     * Rescale the vector and reset the wscale
     */
    public void rescale()
    {
        if (wscale != 1.0)
        {
            for (int i = 0; i < weights.length; ++i)
            {
                weights[i] *= wscale;
            }
            wscale = 1;
        }
    }

    /**
     * Set the wscale
     * @param wscale the scaling
     */
    public void setWscale(double wscale)
    {
        this.wscale = wscale;
    }

    /**
     * Get the number of labels (or classes) in this model
     * @return
     */
    public int getNumLabels()
    {
        return numLabels;
    }
}
