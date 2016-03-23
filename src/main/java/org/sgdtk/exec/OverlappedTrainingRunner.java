package org.sgdtk.exec;


import org.sgdtk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to make it easy to do VW-like overlapped trainining as part of an API.
 *
 * This class is called a 'lifecycle' since it gives the user hooks into the training cycle but hides the
 * overlapped IO craziness underneath.  To make it work, minimal inputs are required.  You need to tell us how many
 * epochs (passes) to run, how big to make the ring buffer, and where the cache file is.  We generate a cache
 * for each run (right now).
 *
 * This object follows an RAII pattern, and you will make one of these per training, and throw it out after the model
 * is produced.
 *
 *
 * The phases of the lifecycle are simply
 * <ul>
 *     <li>constructor</li>
 *     <li>add(featureVector) - (async adding, but blocks until done training) do this for each piece of training data</li>
 *     <li>finish() - (async) this runs the remaining passes over the training data.  Call as soon as add is done</li>
 * </ul>
 *
 * How it works:  This is a non-core based feature producer.  It uses a cache to persist read and write FeatureVectors in-between iterations
 *
 * This code is influenced by Martin Thompson's article on fast serialization:
 * http://mechanical-sympathy.blogspot.com/2012/07/native-cc-like-performance-for-java.html and of course, by
 * VW.  A user adds new training examples to the TrainingExecutor.  This class then caches those vectors to a file in a
 * native binary format as quickly as it can (currently it will not do this if there is only 1 pass over the data).
 *
 * On future epochs, the files are read back in, which is extremely fast compared to the native formats, and then
 * pushes them back onto the queue.  This works because the TrainingExecutor must block on an add that cannot be fulfilled,
 * creating back-pressure.
 *
 * @author dpressel
 */
public class OverlappedTrainingRunner implements AsyncTrainingRunner
{
    private int epochs = 5;
    TrainingExecutor trainEx;
    private File cacheFile;
    RandomAccessFile randomAccessFile;
    byte[] packBuffer;
    private Boolean dense = null;
    private List<TrainingEventListener> listeners = new ArrayList<>();
    private double probAdd = 1.0;
    private Learner learner;
    private Model model;
    private int bufferSz = 1024;
    private Object learnerUserData;
    // Create, don't start
    public OverlappedTrainingRunner(Learner learner)
    {
        this.learner = learner;
    }
    public void start() throws Exception
    {
        if (cacheFile == null)
        {
            cacheFile = File.createTempFile("oltc", "cache");
            cacheFile.deleteOnExit();
        }
        model = learner.create(getLearnerUserData());
        trainEx = new RingBufferTrainingExecutor();
        initCache();
        trainEx.initialize(learner, model, epochs, cacheFile, bufferSz, listeners);
        trainEx.start();
    }

    private static final Logger log = LoggerFactory.getLogger(OverlappedTrainingRunner.class);


    // Randomly sample in time
    private void addWithProb(FeatureVector fv)
    {
        if (probAdd >= 1.0 || Math.random() < probAdd)
        {
            trainEx.add(fv);
        }
    }

    private void passN() throws IOException
    {

        // Get FV from file
        randomAccessFile = new RandomAccessFile(getCacheFile(), "r");

        while (randomAccessFile.getFilePointer() < randomAccessFile.length())
        {
            int recordLength = (int) randomAccessFile.readLong();
            packBuffer = growIfNeeded(packBuffer, recordLength);
            randomAccessFile.read(packBuffer, 0, recordLength);
            FeatureVector fv = toFeatureVector();
            // add to ring buffer
            addWithProb(fv);

        }

        randomAccessFile.close();

        signalEndEpoch();

    }

    private FeatureVector toFeatureVector()
    {
        if (dense)
        {
            return FeatureVector.deserializeDense(packBuffer);
        }
        return FeatureVector.deserializeSparse(packBuffer);
    }

    private static final int PACK_BUFFER_SZ = 262144;
    private void initCache() throws IOException
    {
        if (getEpochs() > 1)
        {
            packBuffer = new byte[PACK_BUFFER_SZ];
            randomAccessFile = new RandomAccessFile(getCacheFile(), "rw");
        }
    }

    @Override
    public void add(FeatureVector fv) throws IOException
    {
        try
        {
            FeatureVector featureVector = fv;

            if (dense == null)
            {
                dense = fv.getX().getType() == VectorN.Type.DENSE;
            }
            // We can save this as-is even if sparse
            saveCachedFeatureVector(fv);
            addWithProb(featureVector);

        }
        catch (IOException ioEx)
        {
            kill();
            throw ioEx;
        }
    }

    private void kill() throws IOException
    {
        if (randomAccessFile != null)
        {
            randomAccessFile.close();
            randomAccessFile = null;
        }
        trainEx.kill();
    }

    private void saveCachedFeatureVector(FeatureVector fv) throws IOException
    {
        if (getEpochs() > 1)
        {
            // Figure out how many bytes we need to make this work
            int numBytes = fv.getSerializationSize();
            packBuffer = growIfNeeded(packBuffer, numBytes);
            // Serialize
            fv.serializeTo(randomAccessFile, packBuffer);
        }
    }

    private byte[] growIfNeeded(byte[] buffer, int numBytes)
    {
        // Reallocate serialization buffer if necessary
        if (buffer == null || numBytes > buffer.length)
        {
            int newBufSz = ExecUtils.nextPowerOf2(numBytes);
            log.debug("Reallocating serialization packBuffer to " + newBufSz);
            buffer = new byte[newBufSz];
        }
        return buffer;
    }

    @Override
    public Model finish() throws IOException
    {

        try
        {
            if (randomAccessFile != null)
            {
                randomAccessFile.close();
            }

            signalEndEpoch();
            for (int i = 1; i < getEpochs(); ++i)
            {
                passN();
                log.info("Completed pass " + (i + 1));
            }
            signalEndEpoch();
            trainEx.join();
            return model;
        }
        catch (IOException ioEx)
        {

            kill();
            throw new IOException(ioEx);

        }


    }

    @Override
    public void addListener(TrainingEventListener listener)
    {
        this.getListeners().add(listener);
    }


    private void signalEndEpoch()
    {
        trainEx.add(null);

    }

    public int getEpochs()
    {
        return epochs;
    }

    public void setEpochs(int epochs)
    {
        this.epochs = epochs;
    }

    public File getCacheFile()
    {
        return cacheFile;
    }

    public void setCacheFile(File cacheFile)
    {
        this.cacheFile = cacheFile;
    }

    public boolean isDense()
    {
        return dense;
    }

    public void setDense(boolean dense)
    {
        this.dense = dense;
    }

    public List<TrainingEventListener> getListeners()
    {
        return listeners;
    }

    public void setListeners(List<TrainingEventListener> listeners)
    {
        this.listeners = listeners;
    }

    public double getProbAdd()
    {
        return probAdd;
    }

    public void setProbAdd(double probAdd)
    {
        this.probAdd = probAdd;
    }

    public int getBufferSz()
    {
        return bufferSz;
    }

    public void setBufferSz(int bufferSz)
    {
        this.bufferSz = bufferSz;
    }

    public Object getLearnerUserData()
    {
        return learnerUserData;
    }

    public void setLearnerUserData(Object learnerUserData)
    {
        this.learnerUserData = learnerUserData;
    }
}
