package org.sgdtk;

public class LinearModelFactory implements ModelFactory
{
    @Override
    public Model newInstance(int wlength)
    {
        return new LinearModel(wlength);
    }
}
