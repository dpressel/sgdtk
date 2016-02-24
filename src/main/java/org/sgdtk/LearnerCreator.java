package org.sgdtk;

import org.sgdtk.io.Config;

public interface LearnerCreator
{
    Learner newInstance(Config params) throws Exception;
}
