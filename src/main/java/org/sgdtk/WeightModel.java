package org.sgdtk;

import java.io.*;
import java.util.Arrays;

/**
 * Linear model for classification
 *
 * @author dpressel
 */
public class WeightModel implements Model
{
    private double[] weights;
    private double wdiv;
    private double wbias;

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

    public WeightModel(int wlength)
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
    public WeightModel(int wlength, double wdiv, double wbias)
    {
        this.weights = new double[wlength];
        Arrays.fill(this.weights, 0);
        this.wdiv = wdiv;
        this.wbias = wbias;
    }

    protected WeightModel(double[] weights, double wdiv, double wbias)
    {
        this.weights = new double[weights.length];
        System.arraycopy(weights, 0, this.weights, 0, weights.length);
        this.wdiv = wdiv;
        this.wbias = wbias;
    }

    /**
     * Empty constructor
     */
    public WeightModel()
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
        return new double[]{predict(fv)};
    }

    /**
     * Get wdiv
     *
     * @return wdiv
     */
    public final double getWdiv()
    {
        return wdiv;
    }

    /**
     * Set wdiv
     *
     * @param wdiv
     */
    public final void setWdiv(double wdiv)
    {
        this.wdiv = wdiv;
    }

    /**
     * Get wbias
     *
     * @return wbias
     */
    public final double getWbias()
    {
        return wbias;
    }

    /**
     * Set wbias
     *
     * @param wbias
     */
    public final void setWbias(double wbias)
    {
        this.wbias = wbias;
    }

    /**
     * Create a deep copy of this
     *
     * @return clone
     */
    public Model prototype()
    {
        return new WeightModel(weights, wdiv, wbias);
    }

    /**
     * Magnitude of weight vector
     *
     * @return mag
     */
    public final double mag()
    {
        double dotProd = CollectionsManip.dot(weights, weights);
        return dotProd / wdiv / wdiv;
    }

    public double[] getWeights()
    {
        return weights;
    }

}
