package org.sgdtk;

import java.io.*;
import java.util.Arrays;

/**
 * Linear model for classification
 *
 * @author dpressel
 */
public class LinearModel implements WeightModel
{
    private double[] weights;
    private double wdiv;
    private double wbias;

    /**
     * This method performs an SGD update from a single training example.  This looks a little different
     * than most implementations I'm aware of, mostly because of a trick that speeds the code up drastically for sparse
     * vector by avoiding a rescale by the regularization parameters over the full vector.  You can see the typical
     * update approach in sofia-ml and scikit-learn.  However, here, a factoring of the weight vector is employed that
     * separates the scalar due to regularization from the loss gradient application.  The factoring is described
     * here (under section 5.1):
     *
     * @param fv
     * @param eta
     * @see <a href="http://research.microsoft.com/pubs/192769/tricks-2012.pdf">http://research.microsoft.com/pubs/192769/tricks-2012.pdf</a>
     */
    public final void updateWeights(VectorN vectorN, double eta, double lambda, double dLoss)
    {

        // The wdiv is a scalar factored out of the weight vector due to regularization
        // This prevents having to scale the entire weight vector, which is dense
        // To handle properly, we have to account for this factoring in the model and update the
        // weight vector on use.
        wdiv /= (1 - eta * lambda);

        if (wdiv > 1e5)
        {
            final double sf = 1.0 / wdiv;
            CollectionsManip.scaleInplace(weights, sf);
            wdiv = 1.;
        }

        // When we factored wdiv out, we have to account for this in our gradient update as well
        for (Offset offset : vectorN.getNonZeroOffsets())
        {
            weights[offset.index] += offset.value * -eta * dLoss * wdiv;
        }

        // This is referenced on Leon Bottou's SGD page
        double etab = eta * 0.01;

        // The SGD code supports bias regularization only with an ifdef which defaults to off
        //if (regularizedBias)
        //{
        //    wbias *= (1 - etab * lambda);
        //}
        wbias += -etab * dLoss;

    }

    @Override
    public void load(File file) throws IOException
    {
        load(new FileInputStream(file));
    }

    /**
     * Save model to a file
     *
     * @param file
     * @throws IOException
     */
    @Override
    public void save(File file) throws IOException
    {
        save(new FileOutputStream(file));
    }

    /**
     * Load from stream
     *
     * @param inputStream source
     * @throws IOException
     */
    @Override
    public void load(InputStream inputStream) throws IOException
    {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        wdiv = objectInputStream.readDouble();
        wbias = objectInputStream.readDouble();
        int sz = (int) objectInputStream.readLong();
        weights = new double[sz];
        for (int i = 0; i < sz; ++i)
        {
            weights[i] = objectInputStream.readDouble();
        }
        objectInputStream.close();

    }

    /**
     * Save to stream
     *
     * @param outputStream target
     * @throws IOException
     */
    @Override
    public void save(OutputStream outputStream) throws IOException
    {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeDouble(wdiv);
        objectOutputStream.writeDouble(wbias);
        long sz = (long) weights.length;
        objectOutputStream.writeLong(sz);
        for (int i = 0; i < weights.length; ++i)
        {
            objectOutputStream.writeDouble(weights[i]);
        }
        objectOutputStream.close();
    }

    public LinearModel(int wlength)
    {
        this(wlength, 1, 0);

    }

    /**
     * Create empty but initialized model
     *
     * @param wlength The length of the feature vector
     * @param wdiv    scaling
     * @param wbias   bias
     */
    public LinearModel(int wlength, double wdiv, double wbias)
    {
        this.weights = new double[wlength];
        Arrays.fill(this.weights, 0);
        this.wdiv = wdiv;
        this.wbias = wbias;
    }

    protected LinearModel(double[] weights, double wdiv, double wbias)
    {
        this.weights = new double[weights.length];
        System.arraycopy(weights, 0, this.weights, 0, weights.length);
        this.wdiv = wdiv;
        this.wbias = wbias;
    }

    /**
     * Empty constructor
     */
    public LinearModel()
    {

    }

    /**
     * Predict the classification for feature vector
     *
     * @param fv feature vector
     * @return
     */
    @Override
    public final double predict(final FeatureVector fv)
    {
        double acc = fv.dot(weights);
        return acc / wdiv + wbias;
    }

    @Override
    public double[] score(FeatureVector fv)
    {
        return new double[]{
                predict(fv)
        };
    }

    /**
     * Create a deep copy of this
     *
     * @return clone
     */
    @Override
    public Model prototype()
    {
        return new LinearModel(weights, wdiv, wbias);
    }

    /**
     * Magnitude of weight vector
     *
     * @return mag
     */
    @Override
    public final double mag()
    {
        double dotProd = CollectionsManip.dot(weights, weights);
        return dotProd / wdiv / wdiv;
    }


    //public double[] getWeights()
    //{
    //    return weights;
    //}

}
