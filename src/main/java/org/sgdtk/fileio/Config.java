package org.sgdtk.fileio;

import java.util.Map;

/**
 * Created by dpressel on 10/22/15.
 */
public class Config
{
    public Config()
    {

    }

    private Map<String, Object> learner;
    private Map<String, Object> model;
    private String name;

    public Map<String, Object> getLearner()
    {
        return learner;
    }

    public void setLearner(Map<String, Object> learner)
    {
        this.learner = learner;
    }

    public Map<String, Object> getModel()
    {
        return model;
    }

    public void setModel(Map<String, Object> model)
    {
        this.model = model;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
