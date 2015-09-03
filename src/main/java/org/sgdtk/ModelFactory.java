package org.sgdtk;

public interface ModelFactory
{
    Model newInstance(int wlength) throws Exception;
}
