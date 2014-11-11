package org.sgdtk.exec;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.sgdtk.*;
import org.sgdtk.fileio.SVMLightFileFeatureProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Train a classifier using some loss function using SGD.  Unlike Train, File IO is overlapped with processing
 *
 * This training method is modeled loosely on VW, where the file IO is overlapped (provided by a different
 * thread from the processor).  The way it works: the feeder reads the files in and pushes them onto a RingBuffer
 * (thank you LMAX!) as soon as it reads it.  If the RingBuffer is full, the insertion will block (back-pressure).
 *
 * On the first pass through the data, the Learner will process the data from the RingBuffer in a single threaded
 * fashion, and once its done, it will serialize it back to a file, and go to the next feature vector.
 *
 * The way its set up right now, you have to guess at eta0 to run the program.  This can be modified by reading a few
 * examples upfront and calling the {@link org.sgdtk.Learner#preprocess(org.sgdtk.Model, java.util.List)} instead
 *
 *
 * @author dpressel
 */
public class TrainOverlapped
{

    public static class Params
    {

        @Parameter(description = "Training file", names = {"--train", "-t"}, required = true)
        public String train;

    	@Parameter(description = "Testing file", names = {"--eval", "-e"})
        public String eval;

        @Parameter(description = "Model to write out", names = {"--model", "-s"})
        public String model;

        @Parameter(description = "Loss function", names = {"--loss", "-l"})
        public String loss = "hinge";

        @Parameter(description = "lambda", names = {"--lambda", "-lambda"})
        public Double lambda = 1e-5;

        @Parameter(description = "Number of epochs", names = {"--epochs", "-epochs"})
        public Integer epochs = 5;

        @Parameter(description = "eta0, if not set, try and preprocess to find", names = {"--eta0", "-e0"}, required = true)
        public Double eta0;

        @Parameter(description = "Width of feature vector", names = {"--wfv", "w"})
        public Integer widthFV;

        @Parameter(description = "Ring Buffer size", names = {"--buf", "-b"})
        public Integer bufferSize = 10000;

	}

	public static void main(String[] args)
    {
        try
        {
            Params params = new Params();
            JCommander jc = new JCommander(params, args);
            jc.parse();

            File trainFile = new File(params.train);
            SVMLightFileFeatureProvider.Dims dims;
            if (params.widthFV == null)
            {
                dims = SVMLightFileFeatureProvider.findDims(trainFile);
                System.out.println("Dims: " + dims.width + " x " + dims.height);
            }
            else
            {
                dims = new SVMLightFileFeatureProvider.Dims(params.widthFV, 0);
            }


            long t0 = System.currentTimeMillis();
            Loss lossFunction = null;
            if (params.loss.equalsIgnoreCase("log"))
            {
                System.out.println("Using log loss");
                lossFunction = new LogLoss();
            }
            else if (params.loss.startsWith("sq"))
            {
                System.out.println("Using square loss");
                lossFunction = new SquareLoss();
            }
            else
            {
                System.out.println("Using hinge loss");
                lossFunction = new HingeLoss();
            }

            Learner learner = new SGDLearner(lossFunction, params.lambda, params.eta0);

            SVMLightFileFeatureProvider fileReader = new SVMLightFileFeatureProvider();

            Model model = learner.create(dims.width);
            // Now start a thread for File IO, and then pull data until we hit the number of epochs
            File cacheFile = new File(params.train + ".cache");

            TrainingExecutor trainEx = new RingBufferTrainingExecutor(RingBufferTrainingExecutor.Strategy.BUSY);
            trainEx.initialize(learner, model, params.epochs, cacheFile, params.bufferSize);
            trainEx.start();


            fileReader.open(trainFile);
            FeatureVectorProducer fvp = new FeatureVectorProducer(fileReader, trainEx);

            fvp.run();

            double elapsed = (System.currentTimeMillis() - t0)/1000.;

            System.out.println("Overlapped training completed in " + elapsed + "s");

            LinearModel lm = (LinearModel)model;
            System.out.println("wnorm=" + lm.mag());

            trainEx.join();

            if (params.model != null)
            {
                model.save(new FileOutputStream(params.model));

            }
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
	}


}