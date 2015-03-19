package org.sgdtk.exec;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.sgdtk.*;
import org.sgdtk.fileio.SVMLightFileFeatureProvider;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Train a classifier using some loss function using SGD.  Unlike Train, File IO is overlapped with processing
 * <p/>
 * This training method is modeled loosely on VW, where the file IO is overlapped (provided by a different
 * thread from the processor).  The way it works: the feeder reads the files in and pushes them onto a RingBuffer
 * (thank you LMAX!) as soon as it reads it.  If the RingBuffer is full, the insertion will block (back-pressure).
 * <p/>
 * On the first pass through the data, the Learner will process the data from the RingBuffer in a single threaded
 * fashion, and once its done, it will serialize it back to a file, and go to the next feature vector.
 * <p/>
 * The way its set up right now, you have to guess at eta0 to run the program.  This can be modified by reading a few
 * examples upfront and calling the {@link org.sgdtk.Learner#preprocess(org.sgdtk.Model, java.util.List)} instead
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

        @Parameter(description = "Number of classes", names = {"--nc"})
        public Integer numClasses = 2;
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
                lossFunction = new SquaredHingeLoss();
            }
            else
            {
                System.out.println("Using hinge loss");
                lossFunction = new HingeLoss();
            }

            // Now start a thread for File IO, and then pull data until we hit the number of epochs
            File cacheFile = new File(params.train + ".cache");


            Learner learner = params.numClasses > 2 ? new MultiClassSGDLearner(params.numClasses, lossFunction, params.lambda, params.eta0) :
                    new SGDLearner(lossFunction, params.lambda, params.eta0);

            OverlappedTrainingLifecycle trainingLifecycle = new OverlappedTrainingLifecycle(params.epochs, params.bufferSize, learner, dims.width, cacheFile);


            //Learner learner = new SGDLearner(lossFunction, params.lambda, params.eta0);

            SVMLightFileFeatureProvider fileReader = new SVMLightFileFeatureProvider();


            fileReader.open(trainFile);

            FeatureVector fv;

            //buffer = new byte[262144];
            while ((fv = fileReader.next()) != null)
            {
                trainingLifecycle.add(fv);
            }

            Model model = trainingLifecycle.finish();


            //if (evalSet != null)
            //{
            //    learner.eval(model, evalSet, metrics);
            //    showMetrics(metrics, "Test Set Eval Metrics");
            //}

            double elapsed = (System.currentTimeMillis() - t0) / 1000.;

            System.out.println("Overlapped training completed in " + elapsed + "s");

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