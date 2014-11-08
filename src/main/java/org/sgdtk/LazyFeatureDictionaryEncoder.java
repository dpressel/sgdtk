package org.sgdtk;

import java.io.*;
import java.util.*;

/**
 * Lazily add features as they are attested
 *
 * @author dpressel
 */
public class LazyFeatureDictionaryEncoder implements FeatureNameEncoder
{

    Map<String, Integer> featureIndex = new HashMap<String, Integer>();

    // this data structure allows us to preserve the feature positions without cycling the hash
    List<String> names = new ArrayList<String>();

    /**
     * Find or create
     * @param name feature name
     * @return index
     */
    @Override
    public int lookupOrCreate(String name)
    {
        Integer id = indexOf(name);
        if (id == null)
        {
            int sz = names.size();
            names.add(name);
            featureIndex.put(name, sz);
            return sz;
        }
        return id;
    }

    /**
     * Reload
     * @param inputStream source
     * @throws IOException
     */
    @Override
    public void load(InputStream inputStream) throws IOException
    {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        try
        {
            featureIndex = (Map<String, Integer>) objectInputStream.readObject();
            names = (List<String>)objectInputStream.readObject();
            objectInputStream.close();
        }
        catch (ClassNotFoundException cfne)
        {
            throw new IOException(cfne);
        }
    }

    /**
     * Save
     * @param outputStream target
     * @throws IOException
     */
    @Override
    public void save(OutputStream outputStream) throws IOException
    {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(featureIndex);
        objectOutputStream.writeObject(names);
        objectOutputStream.close();
    }

    /**
     * Get int index
     * @param name feature name
     * @return index
     */
    @Override
    public Integer indexOf(String name)
    {
        return featureIndex.get(name);
    }

    /**
     * Get name for index
     * @param fvOffset index
     * @return name
     * @throws UnsupportedOperationException
     */
    @Override
    public String nameOf(Integer fvOffset) throws UnsupportedOperationException
    {
        return names.get(fvOffset);
    }

    /**
     * Empty constructor
     */
    public LazyFeatureDictionaryEncoder()
    {

    }

    /**
     * Length
     * @return
     */
    public int length()
    {
        return names.size();
    }

}
