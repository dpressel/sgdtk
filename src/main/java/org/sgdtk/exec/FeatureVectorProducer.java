
package org.sgdtk.exec;

import org.sgdtk.FeatureProvider;
import org.sgdtk.FeatureVector;
import org.sgdtk.Offset;
import org.sgdtk.UnsafeMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * This is a non-core based feature producer.  It uses a cache to persist read and write FeatureVectors in-between iterations
 *
 * This class is influenced by Martin Thompson's article on fast serialization:
 * http://mechanical-sympathy.blogspot.com/2012/07/native-cc-like-performance-for-java.html and of course, by
 * VW.  It reads in a stream of vectors, presumably from file, and pushes them onto the TrainingExecutor.
 * It then caches those vectors to a file in a native binary format as quickly as it can.
 *
 * On future epochs, the files are read back in, which is extremely fast compared to the native formats, and then
 * pushes them back onto the queue.  This works because the TrainingExecutor must block on an add that cannot be fulfilled,
 * creating back-pressure.
 *
 * Note that its possible to reuse caches, like in VW, and that the SVM light file reader is WAY slower than reading
 * from our internal cache.  When this doesnt happen, we may find that initial IO reads are actually the bottleneck, not
 * training!
 *
 * @author dpressel
 *
 */
public class FeatureVectorProducer
{
    private static final Logger log = LoggerFactory.getLogger(FeatureVectorProducer.class);
    private final int numEpochs;
    private final FeatureProvider featureProvider;
    private final File cacheFile;
    private final TrainingExecutor executor;
    private final boolean reuseCache;
    int maxNonZeroOffset = 0;

    /**
     * Constructor
     * @param featureProvider A feature provider to wrap
     * @param executor An executor to use
     * @throws IOException
     */
    public FeatureVectorProducer(FeatureProvider featureProvider, TrainingExecutor executor) throws IOException
    {
        this(featureProvider, executor, true);
    }

    /**
     * Constructor
     * @param featureProvider A feature provider to wrap
     * @param executor An executor to use
     * @param reuseCache Should we reuse an existing cache if it exists
     * @throws IOException
     */
    public FeatureVectorProducer(FeatureProvider featureProvider, TrainingExecutor executor, boolean reuseCache) throws IOException
    {
        this.numEpochs = executor.getNumEpochs();
        this.featureProvider = featureProvider;
        this.cacheFile = executor.getCacheFile();
        this.executor = executor;
        this.reuseCache = reuseCache;

    }

    /**
     * Run over the data once for each epoch.  On the first epoch, the data will be read from the
     * featureProvider, unless reuseCache is set to true and a suitable cache is found.  This method
     * uses a single buffer that is always sized to a power of 2, and is used for reading a single feature
     * vector at a time from the provider or the cache.  This is facilitated by fast serialization using
     * {@link org.sgdtk.UnsafeMemory}, which a simple wrapper written by Martin Thompson around {@link sun.misc.Unsafe}.
     * Additionally java NIO is used for caching
     *
     * @throws IOException
     */
    public void run() throws IOException
    {

        byte[] serializationBuffer = pass0();

        for (int i = 1; i < numEpochs; ++i)
        {
            serializationBuffer = passN(serializationBuffer);

        }
    }

    private byte[] passN(byte[] buffer) throws IOException
    {

        // Get FV from file
        RandomAccessFile raf = new RandomAccessFile(cacheFile, "r");

        while (raf.getFilePointer() < raf.length())
        //for (long j = 0; j < seen; ++j)
        {
            int recordLength = (int) raf.readLong();
            buffer = growIfNeeded(buffer, recordLength);
            raf.read(buffer, 0, recordLength);
            FeatureVector fv = ExecUtils.readFeatureVectorFromBuffer(buffer);
            // add to ring buffer
            executor.add(fv);

        }

        raf.close();

        executor.add(null);


        return buffer;
    }

    private byte[] pass0() throws IOException
    {
        // Check if there is a cache file



        if (cacheFile.exists() && cacheFile.isFile() && reuseCache)
        {
            log.info("Reusing Existing cache");
            return passN(null);
        }

        long t0 = System.currentTimeMillis();
        FeatureVector fv;
        //seen = 0;
        byte[] buffer = null;
        try
        {

            RandomAccessFile raf = new RandomAccessFile(cacheFile, "rw");

            //buffer = new byte[262144];
            while ((fv = featureProvider.next()) != null)
            {
                List<Offset> offsetList = fv.getNonZeroOffsets();
                //++seen;
                maxNonZeroOffset = Math.max(maxNonZeroOffset, offsetList.size());
                executor.add(fv);

                // Figure out how many bytes we need to make this work
                int numBytes = ExecUtils.getByteSizeForFeatureVector(offsetList.size());
                buffer = growIfNeeded(buffer, numBytes);


                // Serialize
                UnsafeMemory memory = ExecUtils.writeFeatureVectorToBuffer(fv, buffer);

                // Write bytes out
                long inBuffer = memory.getPos();
                raf.writeLong(inBuffer);
                raf.write(memory.getBuffer(), 0, (int) inBuffer);


            }
            raf.close();
            executor.add(null);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
        double elapsed = (System.currentTimeMillis() - t0) / 1000.;
        log.info("File load pass completed in " + elapsed + "s");
        return buffer;
    }

    private byte[] growIfNeeded(byte[] buffer, int numBytes)
    {
        // Reallocate serialization buffer if necessary
        if (buffer == null || numBytes > buffer.length)
        {
            int newBufSz = ExecUtils.nextPowerOf2(numBytes);
            log.debug("Reallocating serialization buffer to " + newBufSz);
            buffer = new byte[newBufSz];
        }
        return buffer;
    }

}