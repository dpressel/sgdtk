package org.sgdtk;

import java.util.Map;

public interface ModelFactory
{
    void configure(Map<String, Object> config) throws Exception;
    Model newInstance(Object params) throws Exception;
}
