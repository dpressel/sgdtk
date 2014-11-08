package org.sgdtk.struct;

/**
 * The y values or labels for each time step
 *
 * This simple class just tracks a sequence of y variables (encoded labels).
 * Notably it models the result of a {@link org.sgdtk.struct.SequentialModel#predict(FeatureVectorSequence)} call
 *
 * @author dpressel
 */
public class Path
{

    /**
     * Constructor.  A normal user is not going to make this him/herself
     *
     * @param length The sequence length
     * @param score The score of this path
     */
    public Path(int length, double score)
    {
        steps = new int[length];
        this.score = score;
    }
    final int[] steps;
    final double score;

    /**
     * Set value at step
     * @param pos step
     * @param y label
     */
    public void set(int pos, int y)
    {
        steps[pos] = y;
    }

    /**
     * Get value at step
     * @param i step
     * @return y index
     */
    public int at(int i)
    {
        return steps[i];
    }

    /**
     * Number of steps in sequence
     * @return length
     */
    public int size()
    {
        return steps.length;
    }

    /**
     * Get the score for this path
     * @return Score
     */
    public double getScore()
    {
        return score;
    }

}
