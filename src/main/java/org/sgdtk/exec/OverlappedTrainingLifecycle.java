package org.sgdtk.exec;


import org.sgdtk.FeatureVector;
import org.sgdtk.Learner;
import org.sgdtk.Model;
import org.sgdtk.UnsafeMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

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
public class OverlappedTrainingLifecycle
{
    private int epochs;
    TrainingExecutor trainEx;
    File cacheFile;
    RandomAccessFile randomAccessFile;
    byte[] packBuffer;
    Model model;

    public OverlappedTrainingLifecycle(int epochs, int bufferSz, Learner learner, int fvWidth, File cacheFile) throws IOException
    {

        this.epochs = epochs;
        this.cacheFile = cacheFile;
        model = learner.create(fvWidth);

        trainEx = new RingBufferTrainingExecutor();

        initCache();
        trainEx.initialize(learner, model, epochs, cacheFile, bufferSz);
        trainEx.start();
    }

    private static final Logger log = LoggerFactory.getLogger(OverlappedTrainingLifecycle.class);


    private void passN() throws IOException
    {

        // Get FV from file
        randomAccessFile = new RandomAccessFile(cacheFile, "r");

        while (randomAccessFile.getFilePointer() < randomAccessFile.length())
        {
            int recordLength = (int) randomAccessFile.readLong();
            packBuffer = growIfNeeded(packBuffer, recordLength);
            randomAccessFile.read(packBuffer, 0, recordLength);
            FeatureVector fv = ExecUtils.readFeatureVectorFromBuffer(packBuffer);
            // add to ring buffer
            trainEx.add(fv);

        }

        randomAccessFile.close();

        signalEndEpoch();

    }

    private void initCache() throws IOException
    {
        if (epochs > 1)
        {
            packBuffer = new byte[262144];
            //cacheFile = File.createTempFile("sgd", ".cache", cacheDir);
            randomAccessFile = new RandomAccessFile(cacheFile, "rw");
        }
    }

    public void add(FeatureVector fv) throws IOException
    {
        try
        {
            trainEx.add(fv);
            saveCachedFeatureVector(fv);


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
        if (epochs > 1)
        {
            // Figure out how many bytes we need to make this work
            int numBytes = ExecUtils.getByteSizeForFeatureVector(fv.getNonZeroOffsets().size());
            packBuffer = growIfNeeded(packBuffer, numBytes);
            // Serialize
            UnsafeMemory memory = ExecUtils.writeFeatureVectorToBuffer(fv, packBuffer);

            // Write bytes out
            long inBuffer = memory.getPos();
            randomAccessFile.writeLong(inBuffer);
            randomAccessFile.write(memory.getBuffer(), 0, (int) inBuffer);
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

    public Model finish() throws IOException
    {

        try
        {
            if (randomAccessFile != null)
            {
                randomAccessFile.close();
            }

            signalEndEpoch();
            for (int i = 1; i < epochs; ++i)
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

    private void signalEndEpoch()
    {
        trainEx.add(null);
    }
}
