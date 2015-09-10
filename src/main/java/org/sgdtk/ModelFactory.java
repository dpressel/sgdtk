package org.sgdtk;

public interface ModelFactory
{
    Model newInstance(Object params) throws Exception;
}
