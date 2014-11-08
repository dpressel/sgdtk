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

    public void clear()
    {
        cost = totalLoss = numEventsSeen = numExamplesSeen = totalError = 0;
    }

    public double getCost()
    {
        return cost;
    }

    public void setCost(double cost)
    {
        this.cost = cost;
    }

    public double getTotalLoss()
    {
        return totalLoss;
    }

    public void setTotalLoss(double totalLoss)
    {
        this.totalLoss = totalLoss;
    }

    public void addToTotalLoss(double loss)
    {
        this.totalLoss += loss;
    }

    public double getNumExamplesSeen()
    {
        return numExamplesSeen;
    }

    public void setNumExamplesSeen(double numExamplesSeen)
    {
        this.numExamplesSeen = numExamplesSeen;
    }

    public void addToTotalExamples(int length)
    {
        this.numExamplesSeen += length;
    }

    public double getTotalError()
    {
        return totalError;
    }

    public void setTotalError(double totalError)
    {
        this.totalError = totalError;
    }

    public void addToTotalError(double error)
    {
        this.totalError += error;
    }

    // Call if you have an unstructured model (ie events == examples)
    public void add(double loss, double error)
    {
        ++this.numExamplesSeen;
        ++this.numEventsSeen;

        this.totalLoss += loss;
        this.totalError += error;
    }

    // Loss defined over number of examples
    public double getLoss()
    {
        double seen = numExamplesSeen > 0. ? numExamplesSeen : 1;
        return totalLoss / seen;
    }

    // Error defined over number of individual events, which for unstructured si the same
    public double getError()
    {
        double seen = numEventsSeen > 0. ? numEventsSeen : 1;
        return totalError / seen;
    }

    public double getNumEventsSeen()
    {
        return numEventsSeen;
    }

    public void setNumEventsSeen(double numEventsSeen)
    {
        this.numEventsSeen = numEventsSeen;
    }

    public void addToTotalEvents(int length)
    {
        this.numEventsSeen += length;
    }
}
