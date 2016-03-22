package org.sgdtk.exec;

import org.sgdtk.Learner;
import org.sgdtk.Model;

/**
 * Created by dpressel on 3/22/16.
 */
public interface TrainingEventListener
{
    void onEpochEnd(Learner learner, Model model, double epochSeconds);
}
