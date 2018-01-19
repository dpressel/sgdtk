package org.sgdtk.io;

import org.sgdtk.FeatureNameEncoder;
import org.sgdtk.Offset;
import org.sgdtk.struct.SequenceProvider;
import org.sgdtk.struct.*;

import java.io.IOException;
import java.util.*;

/**
 * A {@link org.sgdtk.struct.SequentialFeatureProvider} implementation using a {@link org.sgdtk.struct.SequenceProvider}
 *
 * The idea here is to make it simple and fast to provide a pipeline of feature vectors from a source
 * to the trainer or evaluator as a sink to support non in-core learning.
 *
 * @author dpressel
 */
public class SequenceToFeatures implements SequentialFeatureProvider
{
    private final JointFixedFeatureNameEncoder encoder;
    private final FeatureTemplate template;

    private final boolean keepStates;
    SequenceProvider sequenceProvider;

    /**
     * Constructor where no state labels are saved (only use for training)
     *
     * @param sequenceProvider Something that can provide a sequence, which will be consumed for each call to {@link #next()}
     * @param template An in-memory template for feature extraction
     * @param encoder A joint feature encoder, for discriminative MaxEnt type problems (here CRF)
     * @throws IOException
     */
    public SequenceToFeatures(SequenceProvider sequenceProvider, FeatureTemplate template, JointFixedFeatureNameEncoder encoder) throws IOException
    {
        this(sequenceProvider, template, encoder, false);
    }

    /**
     * Constructor where raw state info is included in the emitted {@link org.sgdtk.struct.FeatureVectorSequence}
     *
     * @param sequenceProvider Something that can provide a sequence, which will be consumed for each call to {@link #next()}
     * @param template An in-memory template for feature extraction
     * @param encoder A joint feature encoder, for MaxEnt type problems (here CRF)
     * @param keepStates Should we keep around the raw data inside our feature vectors
     * @throws IOException
     */
    public SequenceToFeatures(SequenceProvider sequenceProvider, FeatureTemplate template, JointFixedFeatureNameEncoder encoder, boolean keepStates) throws IOException
    {

        this.template = template;
        this.encoder = encoder;

        this.sequenceProvider = sequenceProvider;
        this.keepStates = keepStates;

    }

    /**
     * This method provides the next sequence, using its underlying {@link org.sgdtk.struct.SequenceProvider} to do the work.
     * Essentially, we are building a pipeline of conversion from text data to a feature vector sequences
     *
     * @return The next sequence or null if end of stream is reached.
     * @throws IOException
     */
    @Override
    public FeatureVectorSequence next() throws IOException
    {

        List<State> states = sequenceProvider.next();
        if (states == null)
        {
            return null;
        }
        int nPos = states.size();

        List<FeatureExtractorInterface> extractors = template.getExtractors();
        int [] orders = new int[extractors.size()];
        for (int i = 0; i < orders.length; ++i)
        {
            orders[i] = extractors.get(i).getOrder();
        }

        FeatureVectorSequence sequence = new FeatureVectorSequence(keepStates);
        FeatureNameEncoder labelEncoder = encoder.getLabelEncoder();

        for (int pos = 0; pos < nPos; ++pos)
        {
            List<Offset> uFeatures = new ArrayList<Offset>();
            List<Offset> bFeatures = new ArrayList<Offset>();

            String label = states.get(pos).getLabel();
            Integer y = labelEncoder.indexOf(label);
            if (y == null)
            {
                System.out.println("Invalid label: " + label);
                y = -1;
            }
            for (int i = 0; i < orders.length; ++i)
            {
                FeatureExtractorInterface extractor = extractors.get(i);
                String[] features = extractor.run(states, pos);
                // feature could be null if the extractor cannot fit at this point
                // just take it out of the picture
                if (features == null)
                    continue;

                for (String feature : features)
                {
                    // Note that all features are binary, if attested in this example, thats a 1
                    int featureIndex = encoder.lookupOrCreate(feature);
                    if (featureIndex == -1)
                    {
                        // Did not create feature since its frequency was too low
                        continue;
                    }
                    if (orders[i] == 1)
                    {
                        // Add this feature to the unigram feature vector
                        uFeatures.add(new Offset(featureIndex, 1.));
                    }
                    else if (orders[i] == 2)
                    {
                        // Add this feature to the bigram feature vector
                        bFeatures.add(new Offset(featureIndex, 1.));
                    }
                }
            }

            // This is probably unnecessary, since one would expect features with increasing index
            Collections.sort(uFeatures, new Comparator<Offset>()
            {
                @Override
                public int compare(Offset o1, Offset o2)
                {
                    Integer x = o1.index;
                    Integer y = o2.index;
                    return x.compareTo(y);
                }
            });
            Collections.sort(bFeatures, new Comparator<Offset>()
                {
                    @Override
                    public int compare(Offset o1, Offset o2)
                    {
                        Integer x = o1.index;
                        Integer y = o2.index;
                        return x.compareTo(y);
                    }
                });


            // Initialize our feature vector at pos for u, and for v
            int nzU = uFeatures.size();
            int nzB = pos > 0 ? bFeatures.size(): 0;

            int nFeatures = nzU + nzB;
            if (nFeatures == 0)
            {
                // If no features found throw this sample away
                return next();
            }

            sequence.addStep(y, uFeatures, bFeatures, states.get(pos));

        }


        return sequence;
    }
}
