sgdtk
=====

A library for Stochastic Gradient Descent


The starting point was a simple, modular implementation of Leon Bottou's SGD experiments as a Java library that can be extended and used from within application code.  It contains more stuff in some areas and less in others than the SGD experiments, but the results do match the original's results where they overlap.  This code is designed with a relatively simple API with the ability to embed this code into applications, while supporting the ability to reuse or extend easily and providing a clear concise implementation of SGD.  Attempts are made to encourage the JVM to optimize things wherever possible.

The design here is notionally split into two types of learning problems, unstructured classifiers (e.g., hinge-loss, log-loss etc) and structured classifiers (currently CRFs only as implemented in the SGD experiments).  It should be possible to add additional learners by extending the Learner/SequentialLearner and the Model/SequentialModel.

The library was developed and tested in Intellij using Java 8, but can be built, installed and run from Maven and should work on lower Java versions.  The only dependencies in the library currently are JCommander for easy command line parsing and slf4j/logback for logging.

