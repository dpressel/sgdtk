package org.sgdtk;

import java.lang.reflect.Constructor;

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

    @Override
    public Model newInstance(int wlength) throws Exception
    {
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
