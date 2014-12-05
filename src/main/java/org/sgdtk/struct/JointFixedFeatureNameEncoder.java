package org.sgdtk.struct;

import org.sgdtk.CollectionsManip;
import org.sgdtk.FeatureNameEncoder;

import java.io.*;
import java.util.*;

/**
 * This {@link org.sgdtk.FeatureNameEncoder} is built for discriminative MaxEnt-type problems
 *
 * Joint features (NLTK?) AKA MaxEnt features are described by Ratnaparkhi in several papers on MaxEnt.  They apply
 * as well to CRFs.  Here we have to account for both the labels and the raw features, so this class contains
 * an internal {@link org.sgdtk.FeatureNameEncoder} for labels.
 * <p>
 * This encoder has two use-cases.  For training, we are usually going to build this dynamically as data is attested.
 * To do this, we hand the word frequencies over to this class, and let it prune low occurring features, and build up
 * our feature index.  Then when we are done with training we would persist this out by calling {@link #save(java.io.OutputStream)}
 * so we can get the exact same encodings back
 * <p>
 * In the second use-case, we have already trained our data, and persisted the feature encodings to a file, and now we
 * want to score some data.  Here we just call the default constructor, and then {@link #load(java.io.InputStream)}
 * the encoder again from the file that was saved in the training.
 *
 * @see org.sgdtk.exec.ExecUtils
 * @author dpressel
 */
public class JointFixedFeatureNameEncoder implements FeatureNameEncoder
{

    Map<String, Integer> featureIndex;
    // Lazy data structure allows us to preserve the feature positions without cycling the hash
    Map<Integer, String> nameToFeatureIndex;

    private FeatureNameEncoder labelEncoder;

    int featureOffset = 0;

    private void toFeatureIndex(Map<String, Integer> map, int minValue, int numLabels)
    {
        List<Map.Entry<String, Integer>> list =
                new ArrayList<Map.Entry<String, Integer>>( map.entrySet() );
        Collections.sort( list, new Comparator<Map.Entry<String, Integer>>()
        {
            public int compare( Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2 )
            {
                // TODO: change this to use the line below, by freq, not alphabetically
                return (o1.getKey().compareTo(o2.getKey()));
                //return (o2.getValue()).compareTo( o1.getValue() );
            }
        });

        featureIndex = new HashMap<String, Integer>();

        for (Map.Entry<String, Integer> entry : list)
        {
            Integer v = entry.getValue();
            if (v < minValue)
                continue;

            String k = entry.getKey();
            featureIndex.put(k, featureOffset);
            if (k.startsWith("B"))
            {
                featureOffset += numLabels * numLabels;
            }
            else
            {
                featureOffset += numLabels;
            }

        }


    }

    /**
     * Empty constructor, this is usually used prior to loading a previously stored encoder.
     */
    public JointFixedFeatureNameEncoder()
    {

    }

    /**
     * From a set of word frequencies, build up an encoder.  The label encoder is assumed to have been already fully loaded
     * prior to injection into this constructor.
     *
     * @param freqTable Word frequencies
     * @param minValue Cull words below this value
     * @param labelEncoder The label encoder, which should be already mapped
     * @see org.sgdtk.exec.ExecUtils#createJointEncoder(String, int, FeatureTemplate)
     */
    public JointFixedFeatureNameEncoder(HashMap<String, Integer> freqTable, int minValue, FeatureNameEncoder labelEncoder)
    {

        toFeatureIndex(freqTable, minValue, labelEncoder.length());
        this.labelEncoder = labelEncoder;

    }

    /**
     * Load this encoder from a serialized state
     *
     * @param inputStream A previously executed encoder stream to rehydrate from
     * @throws IOException
     */
    @Override
    public void load(InputStream inputStream) throws IOException
    {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        featureOffset = (int) objectInputStream.readLong();

        try
        {
            featureIndex = (Map<String, Integer>) objectInputStream.readObject();
            labelEncoder = (FeatureNameEncoder) objectInputStream.readObject();
            objectInputStream.close();
        }
        catch (ClassNotFoundException cfne)
        {
            throw new IOException(cfne);
        }
    }

    /**
     * Save to an output stream
     * @param outputStream An output stream
     * @throws IOException
     */
    @Override
    public void save(OutputStream outputStream) throws IOException
    {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeLong((long) featureOffset);
        objectOutputStream.writeObject(featureIndex);
        objectOutputStream.writeObject(labelEncoder);
        objectOutputStream.close();
    }

    /**
     * lookup a feature.  In this subclass impl., if the feature is not found, dont create it, as it should have
     * been created already during initialization
     *
     * @param name A feature name
     * @return A feature index or -1 if not found
     */
    @Override
    public int lookupOrCreate(String name)
    {
        Integer x = indexOf(name);
        return (x == null) ? -1: x;
    }

    /**
     * Get the index of a name
     * @param name name
     * @return index or null if not found
     */
    @Override
    public Integer indexOf(String name)
    {
        return featureIndex.get(name);
    }

    /**
     * Get the name back for a feature vector offset.  This method is lazy and creates an inverted map on first use
     * @param fvOffset feature index
     * @return The string name of the feature
     * @throws UnsupportedOperationException
     */
    @Override
    public String nameOf(Integer fvOffset) throws UnsupportedOperationException
    {
        if (nameToFeatureIndex == null)
        {
            nameToFeatureIndex = CollectionsManip.inverted(featureIndex);
        }
        return nameToFeatureIndex.get(fvOffset);
    }


    public int length()
    {
        return featureOffset;
    }

    public FeatureNameEncoder getLabelEncoder()
    {
        return labelEncoder;
    }
}
