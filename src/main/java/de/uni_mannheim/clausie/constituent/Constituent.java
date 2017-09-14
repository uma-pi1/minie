package de.uni_mannheim.clausie.constituent;

import edu.stanford.nlp.ling.IndexedWord;

/**
 * A constituent of a clause.
 *
 * @author Luciano del Corro
 * @author Kiril Gashteovski
 *
 */
public abstract class Constituent {

    // -- types -----------------------------------------------------------------------------------

    /** Constituent types */
    public enum Type {
        SUBJECT, VERB, DOBJ, IOBJ, COMPLEMENT, CCOMP, XCOMP, ACOMP, ADVERBIAL, UNKOWN
    };

    /** Constituent status (could be one of the three: required, optional or ignore */
    public enum Status {
        REQUIRED, OPTIONAL, IGNORE
    };

    /** The root vertex of this constituent in {@link #semanticGraph}. This vertex and all its
     * descendants are part of the constituent (unless they appear in {@link #excludedVertexes}). */
    protected IndexedWord root;
    
    // -- member variables ------------------------------------------------------------------------
    
    /** Type of this constituent */
    protected Type type;

    
    // -- construction ----------------------------------------------------------------------------
    
    /** Constructs a constituent of the specified type. */
    protected Constituent(Type type) {
        this.type = type;
    }

    /** Constructs a constituent of unknown type. */
    protected Constituent() {
        this.type = Type.UNKOWN;
    }
    
    // -- getters/setters -------------------------------------------------------------------------

    /** Returns the type of this constituent. */
    public Type getType() {
        return type;
    }
    
    public IndexedWord getRoot() { 
        return this.root;
    }
    
    // -- utility methods -------------------------------------------------------------------------
    
    /** Returns a textual representation of the root word of this constituent. */
    public abstract String rootString();
}
