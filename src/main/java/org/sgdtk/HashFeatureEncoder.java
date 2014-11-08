package org.sgdtk;

import java.io.*;

/**
 * Experimental hash feature encoder using murmur32 and projecting down to number of bits
 *
 * @author dpressel
 */
public class HashFeatureEncoder implements FeatureNameEncoder
{


    int space;

    /**
     * Default constructor, hash space is 2^18
     */
    public HashFeatureEncoder()
    {
        this(18);
    }

    /**
     * Default constructor, how many bits to use
     * @param nbits number bits to use
     */
    public HashFeatureEncoder(int nbits)
    {
        this.space = (int)Math.pow(2, nbits) - 1;
    }

    /**
     * Load the space value which is a single int: 2^nbits
     * @param inputStream source
     * @throws IOException
     */
    @Override
    public void load(InputStream inputStream) throws IOException
    {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        space = (int)objectInputStream.readLong();
        objectInputStream.close();
    }

    /**
     * Serialize
     * @param outputStream target
     * @throws IOException
     */
    @Override
    public void save(OutputStream outputStream) throws IOException
    {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeLong((long) space);
        objectOutputStream.close();
    }

    /**
     * Constant time lookup into space
     * @param name feature name
     * @return
     */
    @Override
    public int lookupOrCreate(String name)
    {
        return MurmurHash.hash32(name) & space;
    }

    /**
     * Constant time lookup into space
     * @param name feature name
     * @return
     */
    @Override
    public Integer indexOf(String name)
    {
        return MurmurHash.hash32(name) & space;
    }

    /**
     * Unsupported
     * @param fvOffset index
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public String nameOf(Integer fvOffset) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("HashFeatureEncoder doesnt provide reverse lookup");
    }

    public int length()
    {
        return space;
    }
}
