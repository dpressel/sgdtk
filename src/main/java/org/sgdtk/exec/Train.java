package org.sgdtk.exec;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.sgdtk.*;
import org.sgdtk.SGDLearner;
import org.sgdtk.fileio.SVMLightFileFeatureProvider;
import org.sgdtk.SquaredHingeLoss;
import org.sgdtk.MultiClassSGDLearner;

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * Train a classifier using some loss function using SGD
 *
 * @author dpressel
 */
public class Train
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

        @Parameter(description = "eta0, if not set, try and preprocess to find", names = {"--eta0", "-e0"})
        public Double eta0 = -1.;

        @Parameter(description = "Number of epochs", names = {"--epochs", "-epochs"})
        public Integer epochs = 5;

        @Parameter(description = "Number of classes", names = {"--nc"})
        public Integer numClasses = 2;

        @Parameter(description = "Learning method (sgd|adagrad)", names = {"--method"})
        public String method = "sgd";

	}

	public static void main(String[] args)
    {
        try
        {
            Params params = new Params();
            JCommander jc = new JCommander(params, args);
            jc.parse();

            File trainFile = new File(params.train);

            SVMLightFileFeatureProvider reader = new SVMLightFileFeatureProvider();

            long l0 = System.currentTimeMillis();
            List<FeatureVector> trainingSet = reader.load(trainFile);
            double elapsed = (System.currentTimeMillis() - l0)/1000.;
            System.out.println("Training data loaded in " + elapsed + "s");
            List<FeatureVector> evalSet = null;
            if (params.eval != null)
            {
                File evalFile = new File(params.eval);
                evalSet = reader.load(evalFile);
            }

            Loss lossFunction = null;
            if (params.loss.equalsIgnoreCase("log"))
            {
                System.out.println("Using log loss");
                lossFunction = new LogLoss();
            }
            else if (params.loss.startsWith("sqh"))
            {
                System.out.println("Using squared hinge loss");
                lossFunction = new SquaredHingeLoss();
            }
            else if (params.loss.startsWith("sq"))
            {
                System.out.println("Using square loss");
                lossFunction = new SquaredLoss();
            }
            else
            {
                System.out.println("Using hinge loss");
                lossFunction = new HingeLoss();
            }

            boolean isAdagrad = "adagrad".equals(params.method);


            Learner learner = params.numClasses > 2 ? new MultiClassSGDLearner(params.numClasses, lossFunction, params.lambda, params.eta0) :
                    new SGDLearner(lossFunction, params.lambda, params.eta0,
                            new LinearModelFactory(isAdagrad ? AdagradLinearModel.class: LinearModel.class),
                            isAdagrad ? new FixedLearningRateSchedule(): new RobbinsMonroUpdateSchedule());

            int vSz = reader.getLargestVectorSeen();
            System.out.println("Creating model with vector of size " + vSz);
            Model model = learner.create(vSz);
            double totalTrainingElapsed = 0.;
            for (int i = 0; i < params.epochs; ++i)
            {
                Collections.shuffle(trainingSet);
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                System.out.println("EPOCH: " + (i + 1));
                Metrics metrics = new Metrics();
                double t0 = System.currentTimeMillis();

                learner.trainEpoch(model, trainingSet);
                double elapsedThisEpoch = (System.currentTimeMillis() - t0) /1000.;
                System.out.println("Epoch training time " + elapsedThisEpoch + "s");
                totalTrainingElapsed += elapsedThisEpoch;

                learner.eval(model, trainingSet, metrics);
                showMetrics(metrics, "Training Set Eval Metrics");
                metrics.clear();

                if (evalSet != null)
                {
                    learner.eval(model, evalSet, metrics);
                    showMetrics(metrics, "Test Set Eval Metrics");
                }
            }

            System.out.println("Total training time " + totalTrainingElapsed + "s");
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

    private static void showMetrics(Metrics metrics, String pre)
    {
        System.out.println("========================================================");
        System.out.println(pre);
        System.out.println("========================================================");

        System.out.println("\tLoss = " + metrics.getLoss());
        System.out.println("\tCost = " + metrics.getCost());
        System.out.println("\tError = " + 100*metrics.getError());
        System.out.println("--------------------------------------------------------");
    }


}