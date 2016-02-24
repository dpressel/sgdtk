package org.sgdtk.exec;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.sgdtk.io.CRFXXTemplateLoader;
import org.sgdtk.struct.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Train a CRF using a SequentialLearner on CONLL2000 data using SGD
 *
 * @author dpressel
 */
public class TrainStruct
{

    public static class Params
    {

        @Parameter(description = "Model to write to", names = {"--model", "-m"})
        public String model;

        @Parameter(description = "Model to write to", names = {"--features", "-fe"})
        public String featureMap = "feature.map";

        @Parameter(description = "Train on file", names = {"--train", "-t"}, required = true)
        public String train;

        @Parameter(description = "Total number of epochs (50)", names = {"--epochs", "-r"})
        public Integer epochs = 50;

        @Parameter(description = "Template file", names = {"--template" })
        public String templateFile;

        @Parameter(description = "Capacity control parameter (1.0)", names = {"-c"})
        public Double c = 1.;

        @Parameter(description = "Initial learning rate", names = {"--eta", "-s"})
        public Double eta = 0.;

        @Parameter(description = "Min occurs for each feature (3)", names = {"-f", "--cutoff"})
        public Integer cutoff = 3;


    }


    public static void main(String[] args)
    {
        try
        {
            Params params = new Params();
            JCommander jc = new JCommander(params, args);
            jc.parse();


            File templateFile = new File(params.templateFile);
            CRFXXTemplateLoader templateLoader = new CRFXXTemplateLoader();
            FeatureTemplate template = templateLoader.load(templateFile);

            JointFixedFeatureNameEncoder jointFeatureEncoder = ExecUtils.createJointEncoder(params.train, 3, template);
            List<FeatureVectorSequence> trainingData = ExecUtils.load(params.train, template, jointFeatureEncoder, false);

            SequentialLearner learner = new SGDSequentialLearner(params.c, params.eta);


            SequentialModel model = learner.create(jointFeatureEncoder.length(), jointFeatureEncoder.getLabelEncoder().length());
            double t0 = System.currentTimeMillis();
            double eElapsed = 0;
            for (int i = 0; i < params.epochs; ++i)
            {
                double e0 = System.currentTimeMillis();
                learner.trainEpoch(model, trainingData);

                double eNow = System.currentTimeMillis();
                eElapsed += (eNow - e0);
                System.out.println("Epoch " + (i + 1) + " (" + eElapsed/1000. + "s)");
            }

            double tNow = System.currentTimeMillis();
            System.out.println("Training time " + " (" + (tNow - t0)/1000. + "s)");

            if (params.model != null)
            {
                System.out.println("Writing model: " + params.model);
                model.save(new FileOutputStream(params.model));
                System.out.println("Writing feature encoding map: " + params.featureMap);
                jointFeatureEncoder.save(new FileOutputStream(params.featureMap));
            }



        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
    }



}