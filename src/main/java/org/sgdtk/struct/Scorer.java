package org.sgdtk.struct;

import org.sgdtk.CollectionsManip;
import org.sgdtk.Offset;

import java.util.Arrays;

/**
 * Workhorse of CRF model.  This class probably should be made into an inner class of {@link org.sgdtk.struct.CRFModel}
 *
 * TODO: make inner class of CRFModel?
 *
 * This is organized similarly to Bottou's version.  Each scorer holds a ref to a sequence, and a model.
 * This is really the guts of the model training, and isnt going to be necessary publicly in most cases.
 *
 * @author dpressel
 */

class Scorer
{
    // Unigram scores
    double [][] u;

    // Bigram scores
    double [][][] b;

    // Each scorer works on a single sequence
    final FeatureVectorSequence sequence;

    // Copy of model.getNumLabels();
    final int numLabels;

    final CRFModel model;

    /**
     * Create a scorer for this sequence
     * @param model A model
     * @param sequence A feature vector sequence
     */
    public Scorer(CRFModel model, FeatureVectorSequence sequence)
    {
        this.model = model;
        this.sequence = sequence;
        this.numLabels = model.getNumLabels();
        compute();
    }


    void updateU(double[] g, int pos, int lasty, int numy, double eta)
    {
        double wscale = model.getWscale();
        double[] weights = model.getWeights();
        int off = lasty;
        double gain = eta / wscale;

        for (Offset offset : sequence.getOffsetsForU(pos))
        {
            int l = offset.index + off;
            for (int k = 0; k < numy; ++k, ++l)
            {
                weights[l] += g[k] * offset.value * gain;
            }
        }
    }

    void updateB(double[] g, int pos, int lasty, int y, int numy, double eta)
    {

        double wscale = model.getWscale();
        double[] weights = model.getWeights();
        double gain = eta / wscale;

        int off = y * numLabels + lasty;

        for (Offset offset : sequence.getOffsetsForB(pos))
        {

            int l = offset.index + off;
            for (int k = 0; k < numy; ++k, ++l)
            {
                weights[l] += g[k] * offset.value * gain;
            }
        }
    }

    /**
     * Find the most likely path through the data using viterbi algorithm
     *
     * @return Most likely path
     */
    public Path viterbi()
    {
        int nPos = sequence.length();
        int[][] backpointer = new int[nPos][numLabels];

        double [] scores = new double[numLabels];
        double[] us = new double[numLabels];
        double[] bs = new double[numLabels];

        System.arraycopy(u[0], 0, scores, 0, numLabels);
        for (int pos = 1; pos < nPos; ++pos)
        {
            System.arraycopy(u[pos], 0, us, 0, numLabels);

            for (int yi = 0; yi < numLabels; ++yi)
            {
                // bs = b[pos-1][yi] + scores
                CollectionsManip.copyAdd(bs, b[pos - 1][yi], scores);

                double best = bs[0];
                int bestj = 0;
                for (int yj = 1; yj < numLabels; ++yj)
                {
                    if (bs[yj] > best)
                    {
                        best = bs[yj];
                        bestj = yj;
                    }
                }

                backpointer[pos][yi] = bestj;
                us[yi] += best;
            }
            System.arraycopy(us, 0, scores, 0, us.length);

        }

        int bestj = 0;
        double best = scores[0];

        for (int yj = 1; yj < numLabels; ++yj)
        {
            // Find the maximum score at yj
            if (scores[yj] > best)
            {
                best = scores[yj];
                bestj = yj;
            }
        }
        Path path = new Path(nPos, best);
        for (int pos = nPos - 1; pos >= 0; pos--)
        {
            path.set(pos, bestj);
            bestj = backpointer[pos][bestj];
        }

        return path;
    }

    /**
     * Mass of correct path through sequence
     * @return sum
     */
    public double computeCorrect()
    {
        int nPos = sequence.length();

        int y = sequence.getY(0);
        double sum = u[0][y];
        for (int pos = 1; pos < nPos; ++pos)
        {
            int lasty = y;
            y = sequence.getY(pos);
            // -1 as sentinel?
            if (y >= 0 && lasty >= 0)
            {
                sum += b[pos-1][y][lasty];
            }
            if (y >= 0)
            {
                sum += u[pos][y];
            }
        }
        return sum;
    }

    // Warning this function modifies the internal weights
    double gradCorrect(double g, double eta)
    {
        int nPos = sequence.length();

        int y = sequence.getY(0);
        double[] grad = new double[]{g};
        updateU(grad, 0, y, 1, eta);
        double sum = u[0][y];
        for (int pos = 1; pos < nPos; ++pos)
        {
            int lasty = y;
            y = sequence.getY(pos);
            if (y >= 0 && lasty >= 0)
            {
                sum += b[pos-1][y][lasty];
            }
            if (y >= 0)
            {
                sum += u[pos][y];
            }
            if (y >= 0 && lasty >= 0)
            {
                updateB(grad, pos - 1, lasty, y, 1, eta);
            }
            if (y >= 0)
            {
                updateU(grad, pos, y, 1, eta);
            }
        }

        return sum;
    }
    // Warning this function modifies the internal weights
    double gradForward(double g, double eta)
    {

        int nPos = sequence.length();
        double [][] scores = new double[nPos][numLabels];
        double [] uAcc = new double[numLabels];
        double [] bAcc = new double[numLabels];
        System.arraycopy(u[0], 0, scores[0], 0, numLabels);
        for (int pos = 1; pos < nPos; ++pos)
        {
            System.arraycopy(u[pos], 0, uAcc, 0, numLabels);
            for (int yi = 0; yi < numLabels; ++yi)
            {
                // bs = b[pos-1][yi] + scores[pos-1]
                CollectionsManip.copyAdd(bAcc, b[pos - 1][yi], scores[pos - 1]);
                uAcc[yi] += CollectionsManip.logSum(bAcc);

            }

            System.arraycopy(uAcc, 0, scores[pos], 0, uAcc.length);


        }
        double score = CollectionsManip.logSum(scores[nPos - 1]);

        double [] tmp = new double[numLabels];
        double [] grads = new double[numLabels];


        CollectionsManip.dLogSum(g, scores[nPos - 1], grads);
        for (int pos = nPos - 1; pos > 0; pos--)
        {
            Arrays.fill(uAcc, 0);
            updateU(grads, pos, 0, numLabels, eta);
            for (int yi = 0; yi < numLabels; ++yi)
            {
                if (grads[yi] != 0)
                {

                    CollectionsManip.copyAdd(bAcc, b[pos - 1][yi], scores[pos - 1]);
                    CollectionsManip.dLogSum(grads[yi], bAcc, tmp);
                    updateB(tmp, pos-1, 0, yi, numLabels, eta);
                    CollectionsManip.addInplace(uAcc, tmp);
                }
            }
            System.arraycopy(uAcc, 0, grads, 0, numLabels);

        }
        updateU(grads, 0, 0, numLabels, eta);

        return score;

    }

    /**
     * Forward computation
     *
     * @return sum
     */
    public double computeForward()
    {
        int nPos = sequence.length();

        double [] scores = new double[numLabels];
        double [] us = new double[numLabels];
        double [] bs = new double[numLabels];

        System.arraycopy(u[0], 0, scores, 0, numLabels);

        for (int pos = 1; pos < nPos; ++pos)
        {
            System.arraycopy(u[pos], 0, us, 0, numLabels);
            for (int yi = 0; yi < numLabels; ++yi)
            {
                // bs = b[pos-1][yi] + scores
                CollectionsManip.copyAdd(bs, b[pos - 1][yi], scores);
                double ls = CollectionsManip.logSum(bs);
                us[yi] += ls;
            }

            System.arraycopy(us, 0, scores, 0, numLabels);
        }

        return CollectionsManip.logSum(scores);
    }


    private void compute()
    {
        double[] weights = model.getWeights();
        double wscale = model.getWscale();
        // Unlike the C++ code in sgd, we would like to be RAII
        assert (u == null && b == null);
        int nPos = sequence.length();

        u = new double[nPos][numLabels];
        for (int pos = 0; pos < nPos; ++pos)
        {
            // For each feature from the sequence at position pos
            for (Offset offset : sequence.getOffsetsForU(pos))
            {
                int l = offset.index;
                for (int y = 0; y < numLabels; ++y, ++l)
                {
                    // Really tentative.  Right now weights is actually
                    // a matrix of FVxL, which is why this looks funny
                    // This is the total score for outcome y at position pos for unigram features
                    double acc = weights[l] * offset.value;
                    u[pos][y] += acc;
                }
            }

            CollectionsManip.scaleInplace(u[pos], wscale);

        }

        // First dimension is the last position
        // Second dimension is label
        // Third is the last label
        b = new double[nPos-1][numLabels][numLabels];
        for (int pos = 0; pos < nPos-1; ++pos)
        {
            for (Offset offset : sequence.getOffsetsForB(pos))
            {

                int l = offset.index;

                for (int yi = 0; yi < numLabels; ++yi)
                {
                    for (int yj = 0; yj < numLabels; ++yj, ++l)
                    {
                        double acc = weights[l] * offset.value;
                        b[pos][yi][yj] += acc;
                    }
                }
            }
            CollectionsManip.scaleInplace(b[pos], wscale);

        }
    }

}
