package org.sgdtk.exec;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.sgdtk.FeatureNameEncoder;
import org.sgdtk.LazyFeatureDictionaryEncoder;
import org.sgdtk.Metrics;
import org.sgdtk.fileio.CRFXXTemplateLoader;
import org.sgdtk.struct.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 *  Example to mark up and evaluate a CONLL2000 test file using SequentialLearner.  Based loosely on crfsgd.cpp
 *  with -t option
 *
 *  @author dpressel
 *
 */
public class EvalStruct
{
    public static class Params
    {

        @Parameter(description = "File to evaluate", names = {"--eval", "-f"})
        public String eval;

        @Parameter(description = "Model to use", names = {"--model", "-s"})
        public String model;

        @Parameter(description = "Feature map to use", names = {"--features", "-fe"})
        public String featureMap = "feature.map";

        @Parameter(description = "Template file", names = {"--template" })
        public String templateFile;

    }

    public static void evalOneMaybePrint(SequentialLearner learner, SequentialModel model, FeatureVectorSequence sequence, FeatureNameEncoder labelEncoder, Metrics metrics)
    {

        Path path = learner.evalOne(model, sequence, metrics);

        List<State> states = sequence.getStates();
        if (states != null)
        {
            for (int i = 0, sz = path.size(); i < sz; ++i)
            {
                State state = states.get(i);
                String surfaceTerm = state.getComponents()[0];
                String labelGuess = labelEncoder.nameOf(path.at(i));
                System.out.print(surfaceTerm + "_" + labelGuess + " ");
            }
            System.out.println();
        }

    }


    public static void main(String[] args)
    {
        try
        {
            Params params = new Params();
            JCommander jc = new JCommander(params, args);
            jc.parse();

            SequentialModel model = new CRFModel();
            model.load(new FileInputStream(params.model));


            File templateFile = new File(params.templateFile);
            CRFXXTemplateLoader templateLoader = new CRFXXTemplateLoader();
            FeatureTemplate template = templateLoader.load(templateFile);

            JointFixedFeatureNameEncoder jointFeatureEncoder = new JointFixedFeatureNameEncoder();
            jointFeatureEncoder.load(new FileInputStream(params.featureMap));
            List<FeatureVectorSequence> data = ExecUtils.load(params.eval, template, jointFeatureEncoder, true);

            Metrics metrics = new Metrics();
            SGDSequentialLearner evaluator = new SGDSequentialLearner();
            FeatureNameEncoder labelEncoder = jointFeatureEncoder.getLabelEncoder();
            for (FeatureVectorSequence sequence : data)
            {
                evalOneMaybePrint(evaluator, model, sequence, labelEncoder, metrics);
            }
            System.out.println("\nloss=" + metrics.getLoss());
            double pctError = metrics.getError() * 100.;
            System.out.println(String.format("error=%.2f%%", pctError));

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
