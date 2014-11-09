sgdtk
=====

A library for Stochastic Gradient Descent


The initial goal was a simple, modular implementation of Leon Bottou's SGD experiments as a Java library that can be extended and used from within application code.  It contains more stuff in some areas and less in others than the SGD experiments, but the results do match the original's results where they overlap.  This code is designed with a relatively simple API to enable embedding into applications and facilitate some reuse or extension while providing a clear concise implementation of SGD.  Attempts are made to encourage the JVM to optimize things wherever possible.

The design here is notionally split into two types of learning problems, unstructured classifiers (e.g., hinge-loss, log-loss etc) and structured classifiers (currently CRFs only as implemented in the SGD experiments).  I have tried to keep the interfaces similar for both types of problems.  It should be possible to add additional learners by extending the Learner/SequentialLearner and the Model/SequentialModel.

There is now experimental support for fast out-of-core processing, inspired by VW, where a thread loads the data from file, adds it to a ring buffer, and a processor trains the data.  For multiple passes, the data is reincarnated from a cache file (again, like VW) and loaded back onto the ring buffer from the cache.  Its also possible to reuse a cache from previous runs.

The library was developed and tested in Intellij using Java 8, but can be built, installed and run from Maven and should work on lower Java versions.  The only dependencies in the library currently are JCommander for easy command line parsing and slf4j/logback for logging.

