package org.sgdtk;

import org.sgdtk.fileio.Config;

import java.lang.reflect.Constructor;
import java.util.HashMap;
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
public class SGDLearnerCreator implements LearnerCreator
{
    public static final String TYPE = "type";
    public static final String NUM_CLASSES = "numClasses";
    public static final String LOSS = "lossFunction";
    public static final String LAMBDA = "lambda";
    public static final String ETA0 = "eta0";


    private static final Map<String, ModelFactory> models;

    static
    {
        models = new HashMap<String, ModelFactory>();
        installModel(LinearModelFactory.class.getSimpleName(), new LinearModelFactory());
    }

    public SGDLearnerCreator()
    {

    }

    public static void installModel(String name, ModelFactory modelFactory)
    {
        models.put(name, modelFactory);
    }

    // TODO: rework this so params is more flexible!
    @Override
    public Learner newInstance(Config params) throws Exception
    {
        Map<String, Object> modelParams = params.getModel();
        Map<String, Object> learnerParams = params.getLearner();

        String type = (String)modelParams.get(TYPE);
        if (type == null)
        {
            type = LinearModelFactory.class.getSimpleName();
            //throw new Exception("Model required!");
        }
        if (!type.endsWith("Factory"))
        {
            type += "Factory";
        }

        ModelFactory modelFactory = models.get(type);
        modelFactory.configure(modelParams);
        Double lambda = (Double)learnerParams.get(LAMBDA);
        if (lambda == null)
        {
            lambda = 1e-5;
        }
        Double kEta = (Double)learnerParams.get(ETA0);
        if (kEta == null)
        {
            kEta = -1.;
        }
        String lossFunctionName = (String)learnerParams.get(LOSS);
        Loss loss = null;
        if (lossFunctionName == null)
        {
            loss = new LogLoss();

        }
        else
        {

            Class lossClass = Class.forName(lossFunctionName);
            Constructor[] allConstructors = lossClass.getDeclaredConstructors();
            for (Constructor ctor : allConstructors)
            {
                Class<?>[] pType = ctor.getParameterTypes();
                if (pType.length == 0)
                {
                    loss = (Loss)ctor.newInstance();
                    break;
                }
            }
        }

        String lrUpdate = (String)learnerParams.get("learningRateSchedule");
        LearningRateSchedule sched = null;
        if (lrUpdate == null)
        {
            sched = new RobbinsMonroUpdateSchedule();

        }
        else
        {
            Class schedClass = Class.forName(lrUpdate);
            Constructor[] allConstructors = schedClass.getDeclaredConstructors();
            for (Constructor ctor : allConstructors)
            {
                Class<?>[] pType = ctor.getParameterTypes();
                if (pType.length == 0)
                {
                    sched = (LearningRateSchedule)ctor.newInstance();
                    break;
                }
            }
        }

        Integer numClasses = (Integer) learnerParams.get(NUM_CLASSES);
        if (numClasses == null || numClasses <= 2)
        {
            return new SGDLearner(loss, lambda, kEta, modelFactory, sched);
        }
        return new MultiClassSGDLearner(numClasses.intValue(), loss, lambda.doubleValue(), kEta.doubleValue());
    }

}
