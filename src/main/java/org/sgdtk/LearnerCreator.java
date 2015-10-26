package org.sgdtk;

import org.sgdtk.fileio.Config;

public interface LearnerCreator
{
    Learner newInstance(Config params) throws Exception;
}
