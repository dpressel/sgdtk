package org.sgdtk.struct;

import org.sgdtk.Offset;

import java.util.ArrayList;
import java.util.List;

/**
 * Class models a sequential problem, including unigram features, bigram features and optionally labels (for ground truth)
 *
 * This class is the input for a prediction using a {@link org.sgdtk.struct.SequentialModel}.  It contains the encoded
 * features, but also optionally the raw tokens -- this can be specified when the class is created.  The class is
 * analagous to {@link org.sgdtk.FeatureVector} for unstructured problems, but here think of it as an efficient
 * storage of arrays of feature vectors.
 *
 * @author dpressel
 */
public class FeatureVectorSequence
{

    /**
     * Create just a feature vector, without any tokens
     */
    public FeatureVectorSequence()
    {
        this(false);
    }

    /**
     * Create a feature vector, and optional specify storage for raw tokens
     * @param keepStates Should we keep raw state data around for hydration at a later time
     */
    public FeatureVectorSequence(boolean keepStates)
    {
        states = keepStates ? new ArrayList<State>(): null;
    }


    // Labels at every position
    private List<Integer> y = new ArrayList<Integer>();

    // The raw data if you want to set it
    private final List<State> states;
    private List<List<Offset>> u = new ArrayList<List<Offset>>();
    private List<List<Offset>> b = new ArrayList<List<Offset>>();

    /**
     * Get the label
     * @param at position in sequence
     * @return the encoded label (use the label encoder outside of here to convert back)
     */
    public int getY(int at)
    {
        return y.get(at);
    }

    /**
     * The sequence length
     * @return
     */
    public int length()
    {
        return y.size();
    }

    /**
     * Internally, the unigram features are sparsely represented pairs, get the list of indices at a time step
     * @param pos time step in sequence
     * @return A list of offsets for the features that are active at this step
     */
    List<Offset> getOffsetsForU(int pos)
    {
        return u.get(pos);
    }

    /**
     * Internally, the bigram features are sparsely represented pairs, get the list of indices at a time step
     * @param pos time step in sequence
     * @return A list of offsets for the features that are active at this step
     */
    List<Offset> getOffsetsForB(int pos)
    {
        return b.get(pos);
    }

    /**
     * Add a new step (AKA state) to the FV sequence, by handing it the label for that time step, the unigram features,
     * bigram features, and the raw state info (which for CONLL for example would include words and POS, etc).
     *
     * @param label Integer index of the label
     * @param us Unigram feature vector at this step
     * @param bs Bigram feature vector at this step
     * @param rawStateInfo The raw data at this step
     */
    public void addStep(int label, List<Offset> us, List<Offset> bs, State rawStateInfo)
    {
        y.add(label);
        u.add(us);
        b.add(bs);
        if (states != null && rawStateInfo != null)
        {
            states.add(rawStateInfo);
        }
    }

    /**
     * Get the steps or states back
     * @return States
     */
    public List<State> getStates()
    {
        return states;
    }

}
