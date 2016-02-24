package org.sgdtk;

import java.io.*;

/**
 * Linear model for classification using Dense Vectors
 *
 * @author dpressel
 */
public class SimpleLinearModel implements WeightModel
{
    protected ArrayDouble weights;
    protected double wbias;

    public static final double BIAS_LR_SCALE = 0.01;

    public void updateWeights(VectorN vectorN, double eta, double lambda, double dLoss, double y)
    {
        ArrayDouble x = ((DenseVectorN) vectorN).getX();
        weights.scale(1 - eta * lambda);

        for (int i = 0, sz = x.size(); i < sz; ++i)
        {
            weights.addi(i, -eta * dLoss * x.get(i));
        }
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
        objectOutputStream.writeDouble(wbias);
        long sz = (long) weights.size();
        objectOutputStream.writeLong(sz);
        for (int i = 0; i < sz; ++i)
        {
            objectOutputStream.writeDouble(weights.get(i));
        }
        objectOutputStream.close();
    }

    /**
     * Create empty but initialized model
     *
     * @param wlength The length of the feature vector
     */
    public SimpleLinearModel(int wlength)
    {
        this.weights = new ArrayDouble(wlength, 0);
        this.wbias = 0;
    }
    public SimpleLinearModel(ArrayDouble weights, double wbias)
    {
        this.weights = new ArrayDouble(weights.size());
        weights.copyTo(this.weights);
        this.wbias = wbias;
    }

    /**
     * Empty constructor
     */
    public SimpleLinearModel()
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
        return fv.dot(weights) + wbias;
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
        return new SimpleLinearModel(weights, wbias);
    }

    /**
     * Magnitude of weight vector
     *
     * @return mag
     */
    @Override
    public final double mag()
    {
        return weights.dot(weights);
    }


}
