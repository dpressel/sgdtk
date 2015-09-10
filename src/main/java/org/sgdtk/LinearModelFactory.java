package org.sgdtk;

import java.lang.reflect.Constructor;

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
    Class<LinearModel> classValue = LinearModel.class;
    public LinearModelFactory()
    {

    }
    public LinearModelFactory(Class classValue)
    {
        this.classValue = classValue;
    }

    public Model newInstance(int wlength) throws Exception
    {
        return newInstance(new Integer(wlength));
    }

    // TODO: rework this so params is more flexible!
    @Override
    public Model newInstance(Object params) throws Exception
    {
        // Hack for now!
        Integer wlength = (Integer)params;
        Constructor cons = negotiateConstructor();
        LinearModel model = (LinearModel)cons.newInstance(wlength);
        return model;
    }

    private Constructor negotiateConstructor() throws NoSuchMethodException
    {
        Constructor[] allConstructors = classValue.getDeclaredConstructors();
        for (Constructor ctor : allConstructors) {
            Class<?>[] pType  = ctor.getParameterTypes();
            if (pType.length == 1 && pType[0].toGenericString().equals("int"))
            {
                return ctor;
            }
        }
        throw new NoSuchMethodException("No constructor found!");
    }
}
