package org.sgdtk.struct;

/**
 * Class representing a single time step, with a vector of components internally and a label.
 *
 * For sentences, for example, think of a State as a word, and some attributes of that word, like POS, and perhaps
 * IOB tags for chunks or NER.  The terminology here is an attempt to be more general to cover other types of
 * structured problems.
 *
 * @author dpressel
 */
public class State
{
    /**
     * Constructor from components
     * @param components Components, e.g., word, POS, IOB chunk, etc
     */
    public State(String[] components)
    {
        this(components, -1);
    }

    /**
     * Constructor from components
     * @param components Components, e.g., word, POS, IOB chunk, etc
     * @param labelIndex which index into components yields the label
     */
    public State(String[] components, int labelIndex)
    {
        this.labelIndex = labelIndex;
        this.components = components;
    }
    private final String [] components;

    private final int labelIndex;

    /**
     * Get component at this index
     * @param i index
     * @return The component, e.g., word, POS
     */
    public String atIndex(int i)
    {
        return components[i];
    }

    /**
     * Get all components in this step
     * @return All components
     */
    public String[] getComponents()
    {
        return components;
    }

    /**
     * The number of dimensions of the components
     * @return dims
     */
    public int getDims()
    {
        return components.length;
    }

    /**
     * Get the label, using the index of the label to dereference the components
     * @return label
     */
    public String getLabel()
    {
        return labelIndex < 0 ? null: components[labelIndex];
    }
}
