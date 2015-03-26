sgdtk
=====

# A library for Stochastic Gradient Descent

## Design

The initial goal was a simple, modular implementation of Leon Bottou's SGD experiments as a Java library that can be 
extended and used from within application code.  It contains more stuff in some areas and less in others than the SGD 
experiments, but the results do match the original's results where they overlap.  This code is designed with a
relatively simple API to enable embedding into applications and facilitate some reuse or extension while providing a
clear concise implementation of SGD.  Attempts are made to encourage the JVM to optimize things wherever possible.

The design here is notionally split into two types of learning problems, unstructured classifiers (e.g., hinge-loss,
log-loss etc) and structured classifiers (currently CRFs only as implemented in the SGD experiments).  
I have tried to keep the interfaces similar for both types of problems.  It should be possible to add additional
learners by extending the Learner/SequentialLearner and the Model/SequentialModel.

The code supports fast out-of-core processing, inspired by VW, where a thread loads the data from file, 
adds it to a ring buffer, and a processor trains the data.  For multiple passes, the data is reincarnated from a 
cache file (again, like VW) and loaded back onto the ring buffer from the cache.

There is support for OVA multi-class classification, which is implemented on top of the base routines.
The interface follows the same patterns as binary.  In the case of multi-class classification, the labels will 
not be -1 or 1, but an integer value from 1 ... numClasses stored in the y value of the feature vector.  
Each score can be retrieved using the Model.score() function, which is an array where each index into the 
array represents the class integer value (-1 to make it zero based).

The library was developed and tested in Intellij using Java 8, but can be built, installed and run from Maven and 
should work on lower Java versions.  The only dependencies in the library currently are JCommander for easy command
line parsing, slf4j/logback for logging, and LMAX disruptor for fast contention-free ring buffers.

## Performance

A significant amount of time has gone into profiling the code and optimizing performance.  
The primary bottleneck for performance on large datasets using a good SGD SVM implementation tends to be the IO portion
(not the computation). Note this is true in Leon Bottou's SGD code, which reads all of the data into memory upfront.
Due to the IO bottleneck, I tried to ensure that reading the input file is as fast as possible, and like in VW,
overlapping the IO and the computation via a shared ring buffer allow simultaneous reading/loading and processing.

Regarding the computational aspect, this employs many of the tricks from Leon Bottou's original implementation which makes
it significantly faster than naive implementations (though perhaps more complex).

I considered switching the basic linear algebra routines over to use jblas, but due to the overhead of JNI transfer,
the native operations are actually slower and the jblas package's JavaBlas class (which performs the typical BLAS
operations in java) is equivalent to what is performed here, so for simplicity, all operations are performed within the library.

