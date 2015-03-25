package org.sgdtk.exec;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.sgdtk.FeatureVector;
import org.sgdtk.Learner;
import org.sgdtk.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Use LMAX Disruptor ring buffer to implement an executor
 *
 * LMAX Disruptor is one of my favorite toys for processing.  It is simple to use and very effective performance-wise.
 * Here we can operate contention-free -- unlike VW we dont even have condition variables bounding our buffer!
 *
 * @author dpressel
 */
public class RingBufferTrainingExecutor implements TrainingExecutor
{

    private static final Logger log = LoggerFactory.getLogger(RingBufferTrainingExecutor.class);
    ExecutorService executor;
    Disruptor<MessageEvent> disruptor;
    MessageEventHandler handler;
    int numEpochs;
    private File cacheFile;
    private Strategy strategy;

    public enum Strategy { YIELD, BUSY };

    @Override
    public int getNumEpochs()
    {
        return numEpochs;
    }

    @Override
    public File getCacheFile()
    {
        return cacheFile;
    }

    /**
     * Create one
     */
    public RingBufferTrainingExecutor()
    {
        this(Strategy.YIELD);
    }

    /**
     * Create one
     */
    public RingBufferTrainingExecutor(Strategy strategy)
    {
        this.strategy = strategy;
    }

    /**
     * Class that holds our feature vector
     */
    public static class MessageEvent
    {
        private FeatureVector fv;

        public void set(FeatureVector fv)
        {
            this.fv = fv;
        }
    }

    /**
     * Class that produces our FV holder
     */
    public static class MessageEventFactory implements EventFactory<MessageEvent>
    {
        public MessageEvent newInstance()
        {
            return new MessageEvent();
        }
    }

    /**
     * This is our processor.  It is triggered when an event is placed onto the RingBuffer.
     *
     */
    public static class MessageEventHandler implements EventHandler<MessageEvent>
    {
        Learner learner;
        Model model;
        private long lastTime;
        private AtomicInteger currentEpoch = new AtomicInteger();

        /**
         * Take in the learner and model and train
         * @param learner The learner
         * @param model The initialized but empty model
         */
        public MessageEventHandler(Learner learner, Model model)
        {
            this.learner = learner;
            this.model = model;
            lastTime = System.currentTimeMillis();
        }

        /**
         * On a message, check if it is a null FV.  If so, we are at the end of an epoch.
         * Update book-keeping.  Otherwise, process the FV by using {@link org.sgdtk.Learner#trainOne(org.sgdtk.Model, org.sgdtk.SparseFeatureVector)}
         *
         * @param messageEvent An FV holder
         * @param l Sequence number (which is increasing)
         * @param b not used
         * @throws Exception
         */
        @Override
        public void onEvent(MessageEvent messageEvent, long l, boolean b) throws Exception
        {
            // get the message off the buffer and train on it

            if (messageEvent.fv == null)
            {
                long tNow = System.currentTimeMillis();
                double diff = (tNow - lastTime)/1000.;
                lastTime = tNow;
                int currentEpoch1Based = currentEpoch.incrementAndGet();

                log.info("Epoch " + currentEpoch1Based + " completed in " + diff + "s");
                return;

            }
            //if (l % 10 == 0)
            //{
            //    System.out.println(l);
            //}
            learner.trainOne(model, messageEvent.fv);

        }


        /**
         * Get the current epoch
         * @return
         */
        public int getCurrentEpoch()
        {
            return currentEpoch.get();
        }

    }

    /**
     * Initialize the Disruptor.  The buffer size must be a power of 2 or the RingBuffer will complain
     *
     * @param learner The learner
     * @param model The initialized but untrained model
     * @param numEpochs The number of epochs
     * @param cacheFile The cache file to use
     * @param bufferSize The size of the internal buffer to train from
     */
    @Override
    public void initialize(Learner learner, Model model, int numEpochs, File cacheFile, int bufferSize)
    {

        this.numEpochs = numEpochs;
        executor = Executors.newSingleThreadExecutor();
        MessageEventFactory factory = new MessageEventFactory();
        WaitStrategy waitStrategy = (strategy == Strategy.YIELD) ? new YieldingWaitStrategy(): new BusySpinWaitStrategy();
        disruptor = new Disruptor<MessageEvent>(factory, ExecUtils.nextPowerOf2(bufferSize), executor, ProducerType.SINGLE, waitStrategy);
        handler = new MessageEventHandler(learner, model);
        disruptor.handleEventsWith(handler);
        this.cacheFile = cacheFile;

    }

    /**
     * Start the disruptor
     */
    @Override
    public void start()
    {
        disruptor.start();

    }

    /**
     * Add a feature vector onto the RingBuffer
     * @param fv feature vector
     */
    @Override
    public void add(FeatureVector fv)
    {
        RingBuffer<MessageEvent> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try
        {
            MessageEvent event = ringBuffer.get(sequence);
            event.fv = fv;
        }
        finally
        {
            ringBuffer.publish(sequence);
        }

    }

    @Override
    public void kill()
    {
        disruptor.shutdown();
        executor.shutdownNow();
        try
        {
            executor.awaitTermination(100, TimeUnit.MICROSECONDS);
        }
        catch (InterruptedException intEx)
        {

        }

    }

    /**
     * Pretty much busy-wait our way through this check seeing if all epochs have passed yet
     * Then shutdown the disruptor and our ExecutorService.
     */
    @Override
    public void join()
    {

        while (handler.getCurrentEpoch() < this.numEpochs)
        {
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException intEx)
            {

            }
        }

        kill();
    }
}
