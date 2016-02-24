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
    protected ArrayDouble weights;
    protected double wdiv;
    protected double wbias;
    public static final double BIAS_LR_SCALE = 0.01;

    // Performs L2 regularization scaling
    protected void scaleWeights(double eta, double lambda)
    {

        wdiv /= (1 - eta * lambda);

        if (wdiv > 1e5)
        {
            final double sf = 1.0 / wdiv;
            weights.scale(sf);
            wdiv = 1.;
        }

    }

    /**
     * This method performs an SGD update from a single training example.  This looks a little different
     * than most implementations I'm aware of, mostly because of a trick that speeds the code up drastically for sparse
     * vector by avoiding a rescale by the regularization parameters over the full vector.  You can see the typical
     * update approach in sofia-ml and scikit-learn.  However, here, a factoring of the weight vector is employed that
     * separates the scalar due to regularization from the loss gradient application.  The factoring is described
     * here (under section 5.1):
     *
     * @param
     * @param eta
     * @see <a href="http://research.microsoft.com/pubs/192769/tricks-2012.pdf">http://research.microsoft.com/pubs/192769/tricks-2012.pdf</a>
     */
    public void updateWeights(VectorN vectorN, double eta, double lambda, double dLoss, double y)
    {

        // The wdiv is a scalar factored out of the weight vector due to regularization
        // This prevents having to scale the entire weight vector, which is dense
        // To handle properly, we have to account for this factoring in the model and update the
        // weight vector on use.

        scaleWeights(eta, lambda);

        // When we factored wdiv out, we have to account for this in our gradient update as well
        for (Offset offset : vectorN.getNonZeroOffsets())
        {
            double grad = dLoss * offset.value;
            double thisEta = perWeightUpdate(offset.index, grad, eta);
            weights.addi(offset.index, offset.value * -thisEta * dLoss * wdiv);
        }

        // This is scaling referenced on Leon Bottou's SGD page
        wbias += -eta * BIAS_LR_SCALE * dLoss;

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
        weights = new ArrayDouble(sz);
        for (int i = 0; i < sz; ++i)
        {
            weights.set(i, objectInputStream.readDouble());
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
        long sz = (long) weights.size();
        objectOutputStream.writeLong(sz);
        for (int i = 0; i < sz; ++i)
        {
            objectOutputStream.writeDouble(weights.get(i));
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
        this.weights = new ArrayDouble(wlength, 0);
        this.wdiv = wdiv;
        this.wbias = wbias;
    }

    protected LinearModel(ArrayDouble weights, double wdiv, double wbias)
    {
        this.weights = new ArrayDouble(weights.size());
        weights.copyTo(this.weights);
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
        double dotProd = weights.dot(weights);
        return dotProd / wdiv / wdiv;
    }
    public double perWeightUpdate(int index, double grad, double eta)
    {
        return eta;
    }

}
