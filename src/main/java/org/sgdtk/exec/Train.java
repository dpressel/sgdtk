package org.sgdtk.exec;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.sgdtk.*;
import org.sgdtk.SGDLearner;
import org.sgdtk.io.Config;
import org.sgdtk.io.FixedWidthDatasetReader;
import org.sgdtk.io.JsonConfigReader;
import org.sgdtk.io.SVMLightFileFeatureProvider;
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
        public String loss = LossType.HINGE.toString();

        @Parameter(description = "lambda", names = {"--lambda", "-lambda"})
        public Double lambda = 1e-5;

        @Parameter(description = "eta0, if not set, try and preprocess to find", names = {"--eta0", "-e0"})
        public Double eta0 = -1.;

        @Parameter(description = "Number of epochs", names = {"--epochs", "-epochs"})
        public Integer epochs = 5;

        @Parameter(description = "Number of classes", names = {"--nc"})
        public Integer numClasses = 2;

        @Parameter(description = "Learning method (sgd|adagrad)", names = {"--method"})
        public String method = LearningMethod.SGD.toString();

        @Parameter(description = "Config file", names = {"--config", "--conf"})
        public String configFile;

        @Parameter(description = "File type", names = {"--ftype"})
        public String fileType = FileType.SVM.toString();

    }

    int featureVectorWidth = 0;
    int epoch = 1;
    Learner learner;
    Model model;

    enum LossType
    {
        LOG, LR, SQH, SQ, HINGE
    }

    enum LearningMethod
    {
        SGD, ADAGRAD
    }
    public static Loss lossFor(String loss)
    {
        LossType lossType = LossType.valueOf(loss.toUpperCase());
        System.out.println("Using " + lossType.toString() + " loss");
        if (lossType == LossType.LOG || lossType == LossType.LR)
        {
            return new LogLoss();
        }
        if (lossType == LossType.SQH)
        {
            return new SquaredHingeLoss();
        }
        else if (lossType == LossType.SQ)
        {
            return new SquaredLoss();
        }
        return new HingeLoss();
    }

    enum FileType { SVM, TSV, TXT }

    public List<FeatureVector> load(String file, String ftype) throws IOException
    {
        if (file == null)
        {
            return null;
        }
        FileType fileType = FileType.valueOf(ftype.toUpperCase());
        FeatureProvider reader;

        long l0 = System.currentTimeMillis();
        List<FeatureVector> dataset;

        if (fileType == FileType.TSV || fileType == FileType.TXT)
        {
            System.out.println("Loading 2-class TSV (-1, 1)\tContent");
            FixedWidthDatasetReader fixedWidthDatasetReader = new FixedWidthDatasetReader();
            reader = fixedWidthDatasetReader;
            dataset = fixedWidthDatasetReader.load(new File(file));
        }
        else
        {
            System.out.println("Loading SVM light file");
            SVMLightFileFeatureProvider svmLight = new SVMLightFileFeatureProvider();
            reader = svmLight;
            dataset = svmLight.load(new File(file));
        }
        int largest = reader.getLargestVectorSeen();
        if (largest > featureVectorWidth)
        {
            featureVectorWidth = largest;
        }
        double elapsed = (System.currentTimeMillis() - l0) / 1000.;
        System.out.println(file + " loaded in " + elapsed + "s");


        return dataset;
    }

    void initFromConfig(String configFile) throws Exception
    {
        LearnerCreator creator = new SGDLearnerCreator();
        JsonConfigReader configReader = new JsonConfigReader();
        Config config = configReader.read(new File(configFile));
        learner = creator.newInstance(config);
        System.out.println("Creating model with vector of size " + featureVectorWidth);
        model = learner.create(featureVectorWidth);
    }

    void init(Learner learner) throws Exception
    {
        this.learner = learner;
        System.out.println("Creating model with vector of size " + featureVectorWidth);
        model = learner.create(featureVectorWidth);
    }

    double runEpoch(List<FeatureVector> trainingSet, List<FeatureVector> evalSet)
    {
        Collections.shuffle(trainingSet);
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        System.out.println("EPOCH: " + epoch);
        Metrics metrics = new Metrics();
        double t0 = System.currentTimeMillis();

        learner.trainEpoch(model, trainingSet);
        double elapsedThisEpoch = (System.currentTimeMillis() - t0) / 1000.;
        System.out.println("Epoch training time " + elapsedThisEpoch + "s");

        learner.eval(model, trainingSet, metrics);
        showMetrics(metrics, "Training Set Eval Metrics");
        metrics.clear();

        if (evalSet != null)
        {
            learner.eval(model, evalSet, metrics);
            showMetrics(metrics, "Test Set Eval Metrics");
        }
        ++epoch;
        return elapsedThisEpoch;
    }

    public void saveIf(String modelName) throws IOException
    {
        if (modelName != null)
        {
            System.out.println("Saving: " + modelName);
            model.save(new FileOutputStream(modelName));
        }
    }

    public static Class learningMethodFor(String method)
    {
        LearningMethod learningMethod = LearningMethod.valueOf(method.toUpperCase());
        System.out.println("Using " + learningMethod.toString() + " learning method");
        return learningMethod == LearningMethod.ADAGRAD ? AdagradLinearModel.class : LinearModel.class;
    }
    public static void main(String[] args)
    {
        try
        {
            Params params = new Params();
            JCommander jc = new JCommander(params, args);
            jc.parse();

            Train trainer = new Train();

            List<FeatureVector> trainingSet = trainer.load(params.train, params.fileType);
            List<FeatureVector> evalSet = trainer.load(params.eval, params.fileType);

            // Read all params from a config stream (easy, way)
            if (params.configFile != null)
            {
                trainer.initFromConfig(params.configFile);
            }
            // Build up model from command line
            else
            {
                Loss lossFunction = lossFor(params.loss);
                boolean isAdagrad = "adagrad".equals(params.method);
                ModelFactory modelFactory = new LinearModelFactory(learningMethodFor(params.method));
                trainer.init(params.numClasses > 2 ? new MultiClassSGDLearner(params.numClasses, lossFunction, params.lambda, params.eta0) :
                        new SGDLearner(lossFunction, params.lambda, params.eta0,
                                modelFactory,
                                isAdagrad ? new FixedLearningRateSchedule() : new RobbinsMonroUpdateSchedule()));
            }


            double totalTrainingElapsed = 0.;
            for (int i = 0; i < params.epochs; ++i)
            {

                totalTrainingElapsed += trainer.runEpoch(trainingSet, evalSet);
            }

            System.out.println("Total training time " + totalTrainingElapsed + "s");

            trainer.saveIf(params.model);

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
        System.out.println("\tError = " + 100 * metrics.getError());
        System.out.println("--------------------------------------------------------");
    }


}