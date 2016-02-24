package org.sgdtk;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * Construct a linear model
 *
 * This class is extraodinarly complex without adding much flexibility.  Needs some refactor love.  In general, just
 * tell it what class you want it to use to create, then pass in the Object params, which as of now, is going to
 * always be the length of the weight vector.  Right now its not possible to pass other desirable parameters that may
 * be needed, but this can fixed easily
 *
 * @author dpressel
 */
public class LinearModelFactory implements ModelFactory
{
    public static final String OPTIM = "optim";
    public static final String W_LENGTH = "wlength";

    String className = LinearModel.class.getTypeName();
    Integer wLength = null;

    public LinearModelFactory()
    {

    }
    public LinearModelFactory(String className)
    {
        this.className = className;
    }

    public LinearModelFactory(Class className)
    {
        this.className = className.toString().replaceAll("class ", "");
    }

    @Override
    public void configure(Map<String, Object> config) throws Exception
    {
        if (config.containsKey(OPTIM))
        {
            className = (String)config.get(OPTIM);
        }
    }

    // TODO: rework this so params is more flexible!
    @Override
    public Model newInstance(Object params) throws Exception
    {
        // Hack for now!
        Integer v = (Integer)params;

        Constructor cons = negotiateConstructor();
        WeightModel model = (WeightModel)cons.newInstance(v == null ? wLength : v);
        return model;
    }

    private Constructor negotiateConstructor() throws NoSuchMethodException
    {
        try
        {
            Class classV = Class.forName(className);
            Constructor[] allConstructors = classV.getDeclaredConstructors();
            for (Constructor ctor : allConstructors)
            {
                Class<?>[] pType = ctor.getParameterTypes();
                if (pType.length == 1 && pType[0].toGenericString().equals("int"))
                {
                    return ctor;
                }
            }
        }
        catch (ClassNotFoundException classNoEx)
        {
            throw new NoSuchMethodException(classNoEx.getMessage());
        }
        throw new NoSuchMethodException("No constructor found!");
    }
}
