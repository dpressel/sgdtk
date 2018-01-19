package org.sgdtk.struct;

import java.util.List;

public interface FeatureExtractorInterface {
    String[] run(List<State> states, int current);

    int getOrder();

    int size();
}
