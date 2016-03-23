package org.sgdtk.exec;

import org.sgdtk.*;
import org.sgdtk.io.CONLLFileSentenceProvider;
import org.sgdtk.io.SequenceToFeatures;
import org.sgdtk.struct.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *  This stuff is mainly just for command line programs, not for library use.
 *
 *  @author dpressel
 */
public final class ExecUtils
{

    public static double logOddsToProb(double logOdds)
    {
        double odds = Math.pow(Math.E, logOdds);
        return odds / (odds + 1.0);
    }

    public static int nextPowerOf2(int n)
    {
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n;
    }

    /**
     * Load up a list of feature vectors from the file provided.
     * This method is not intended for general purpose uses, but just as a utility for some of the demonstration
     * programs.
     *
     * @param fileName This is a CONLL2000 file to read
     * @param template This is a CRF++ style template representation
     * @param featureEncoder A joint feature encoder
     * @param saveRawInfo Should the method store the raw states in the feature vector entries
     * @return A list of feature vectors, line sequential as read from the file
     * @throws IOException
     */
    public static List<FeatureVectorSequence> load(String fileName, FeatureTemplate template,
                                                   JointFixedFeatureNameEncoder featureEncoder, boolean saveRawInfo) throws IOException
    {
        // Contract to provide a sequence

        SequenceProvider sequenceProvider = new CONLLFileSentenceProvider(new File(fileName));

        // Contract to provide a sequence of feature vectors
        SequentialFeatureProvider featureProvider = new SequenceToFeatures(sequenceProvider, template, featureEncoder, saveRawInfo);

        List<FeatureVectorSequence> features = new ArrayList<FeatureVectorSequence>();
        while (true)
        {
            FeatureVectorSequence sequence = featureProvider.next();
            if (sequence == null)
            {
                System.out.println("Done loading training data");
                break;
            }
            features.add(sequence);
        }
        return features;
    }


    /**
     * Create a joint encoder from a CONLL2000 training file.
     * This method is actually fairly useful, and probably should be extracted into the library section
     * (possibly io).
     *
     * @param trainingFileName
     * @param minValue
     * @param featureTemplate
     * @return
     * @throws IOException
     */
    public static JointFixedFeatureNameEncoder createJointEncoder(String trainingFileName, int minValue,
                                                                  FeatureTemplate featureTemplate) throws IOException
    {
        SequenceProvider sequenceProvider = new CONLLFileSentenceProvider(new File(trainingFileName));

        HashMap<String, Integer> ftable = new HashMap<String, Integer>();
        List<State> states;

        LazyFeatureDictionaryEncoder attestedLabels = new LazyFeatureDictionaryEncoder();
        while ((states = sequenceProvider.next()) != null)
        {
            int nPos = states.size();
            for (int pos = 0; pos < nPos; ++pos)
            {
                State state = states.get(pos);
                String label = state.getLabel();
                attestedLabels.lookupOrCreate(label);
                //attestedLabels.add(label);
                for (FeatureExtractor extractor : featureTemplate.getExtractors())
                {
                    String feature = extractor.run(states, pos);
                    Integer x = ftable.get(feature);
                    if (x == null)
                    {
                        x = 0;
                    }
                    ftable.put(feature, x + 1);
                }
            }
        }

        return new JointFixedFeatureNameEncoder(ftable, minValue, attestedLabels);
    }


}
