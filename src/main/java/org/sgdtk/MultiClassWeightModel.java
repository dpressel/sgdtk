package org.sgdtk;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Multi-class linear model using one-vs-all classification
 *
 * @author dpressel
 */
public class MultiClassWeightModel implements Model
{
    public Model[] models;

    @Override
    public void load(File file) throws IOException
    {

        InputStream inputStream = new FileInputStream(file);
        load(inputStream);
    }

    @Override
    public void save(File file) throws IOException
    {
        FileOutputStream fos = new FileOutputStream(file);
        save(fos);
    }

    public MultiClassWeightModel()
    {

    }

    public MultiClassWeightModel(Model[] model)
    {
        models = new Model[model.length];
        for (int i = 0; i < model.length; ++i)
        {
            models[i] = model[i].prototype();
        }
    }

    @Override
    public void load(InputStream inputStream) throws IOException
    {
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry ze;
        byte[] buff = new byte[8192];

        Map<Integer, Model> modelMap = new HashMap<Integer, Model>();
        int numClasses = 0;
        while((ze = zis.getNextEntry()) != null)
        {

            Integer id = Integer.valueOf(ze.getName());
            int length;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            while( (length = zis.read(buff)) > 0)
            {
                baos.write(buff, 0, length);
            }

            ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
            Model model = new WeightModel();
            model.load(bis);
            baos.close();
            bis.close();
            modelMap.put(id, model);
            // 1-based
            numClasses = Math.max(numClasses, id);
        }

        zis.close();
        inputStream.close();

        models = new WeightModel[numClasses];
        for (int i = 1; i <= numClasses; ++i)
        {
            models[i - 1] = modelMap.get(i);
        }


    }

    @Override
    public void save(OutputStream outputStream) throws IOException
    {

        ZipOutputStream zos = new ZipOutputStream(outputStream);
        for (int i = 0; i < models.length; ++i)
        {
            String label = String.valueOf(i + 1);
            ZipEntry ze = new ZipEntry(label);
            zos.putNextEntry(ze);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Model model = models[i];
            model.save(baos);
            zos.write(baos.toByteArray());
            zos.closeEntry();
        }
        zos.close();
        outputStream.close();
    }

    @Override
    public double predict(FeatureVector fv)
    {
        double[] labels = score(fv);
        int mxIndex = -1;
        double mxValue = -10000000;
        for (int i = 0; i < labels.length; ++i)
        {
            if (labels[i] > mxValue)
            {
                mxIndex = i;
                mxValue = labels[i];
            }
        }
        return mxIndex;
    }

    @Override
    public double[] score(FeatureVector fv)
    {
        double[] labels = new double[models.length];
        for (int i = 0; i < models.length; ++i)
        {
            labels[i] = models[i].predict(fv);
        }
        return labels;
    }

    @Override
    public Model prototype()
    {
        return new MultiClassWeightModel(models);
    }
}
