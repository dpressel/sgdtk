package org.sgdtk.fileio;

import org.sgdtk.struct.SequenceProvider;
import org.sgdtk.struct.State;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to provide a sentence from a CONLL2000 file as a sequence of states
 *
 * This class provides the raw {@link org.sgdtk.struct.State states} or steps of a sequence, with each state comprised of
 * label and components.  It is pull-oriented and produces a single entry on each call to next()
 *
 * @author dpressel
 */
public class CONLLFileSentenceProvider implements SequenceProvider
{

    BufferedReader reader;

    /**
     * Default constructor, requires an open call prior to running
     */
    public CONLLFileSentenceProvider()
    {

    }

    /**
     * Load a file
     *
     * @param file CONLL2000 file
     * @throws IOException If something bad happens
     */
    public CONLLFileSentenceProvider(File file) throws IOException
    {
        open(file);
    }

    /**
     * Open the CONLL2000 file
     *
     * @param file CONLL2000 file
     * @throws IOException If something bad happens
     */
    public void open(File file) throws IOException
    {
        reader = new BufferedReader(new FileReader(file));
    }

    /**
     * Close the underlying file handle
     *
     * @throws IOException
     */
    public void close() throws IOException
    {
        reader.close();
    }

    /**
     * Get the next sequence from the file.  This will consist of reading multiple lines until
     * an empty line is enountered.  There is one {@link org.sgdtk.struct.State state} per word.
     *
     * @return A sequence of words and additional component info (such as POS, IOB chunk), or null if EOF
     * @throws IOException
     */
    public List<State> next() throws IOException
    {

        String line;
        List<State> sequence = new ArrayList<State>();

        while ((line = reader.readLine()) != null)
        {
            if (isEOS(line))
            {
                return sequence;
            }
            else
            {
                String [] tok = line.split(" ");
                State state = new State(tok, tok.length - 1);
                sequence.add(state);
            }
        }
        return null;


    }

    private boolean isEOS(String line)
    {
        return line.isEmpty();
    }

}
