package org.sgdtk.struct;

import org.sgdtk.struct.State;

import java.io.IOException;
import java.util.List;

/**
 * Streaming interface for sequence of states
 *
 * @author dpressel
 */
public interface SequenceProvider
{
    /**
     * Get the next sequence in the stream
     * @return next sequence or null if end of stream
     * @throws IOException
     */
    public List<State> next() throws IOException;
}
