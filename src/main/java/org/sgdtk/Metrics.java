package org.sgdtk;

/**
 * Book-keeping for testing
 *
 * @author dpressel
 */
public class Metrics
{
    private double cost;
    private double totalLoss;
    private double numExamplesSeen;
    private double numEventsSeen;
    private double totalError;

    public Metrics()
    {
        clear();
    }

    public final void clear()
    {
        cost = totalLoss = numEventsSeen = numExamplesSeen = totalError = 0;
    }

    public final double getCost()
    {
        return cost;
    }

    public final void setCost(double cost)
    {
        this.cost = cost;
    }

    public final double getTotalLoss()
    {
        return totalLoss;
    }

    public final void setTotalLoss(double totalLoss)
    {
        this.totalLoss = totalLoss;
    }

    public final void addToTotalLoss(double loss)
    {
        this.totalLoss += loss;
    }

    public final double getNumExamplesSeen()
    {
        return numExamplesSeen;
    }

    public final void setNumExamplesSeen(double numExamplesSeen)
    {
        this.numExamplesSeen = numExamplesSeen;
    }

    public final void addToTotalExamples(int length)
    {
        this.numExamplesSeen += length;
    }

    public final double getTotalError()
    {
        return totalError;
    }

    public final void setTotalError(double totalError)
    {
        this.totalError = totalError;
    }

    public final void addToTotalError(double error)
    {
        this.totalError += error;
    }

    // Call if you have an unstructured model (ie events == examples)
    public final void add(double loss, double error)
    {
        ++this.numExamplesSeen;
        ++this.numEventsSeen;

        this.totalLoss += loss;
        this.totalError += error;
    }

    // Loss defined over number of examples
    public final double getLoss()
    {
        double seen = numExamplesSeen > 0. ? numExamplesSeen : 1;
        return totalLoss / seen;
    }

    // Error defined over number of individual events, which for unstructured si the same
    public final double getError()
    {
        double seen = numEventsSeen > 0. ? numEventsSeen : 1;
        return totalError / seen;
    }

    public final double getNumEventsSeen()
    {
        return numEventsSeen;
    }

    public final void setNumEventsSeen(double numEventsSeen)
    {
        this.numEventsSeen = numEventsSeen;
    }

    public final void addToTotalEvents(int length)
    {
        this.numEventsSeen += length;
    }
}
