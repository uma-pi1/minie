package de.uni_mannheim.clausie.constituent;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.uni_mannheim.utils.coreNLP.DpUtils;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

/** A constituent of a clause described by a {@link SemanticGraph}.
 * 
 * Each constituent has a root vertex. The root together with its the descendants form the
 * constituent. In some cases, additional vertexes need to be included or excluded; 
 * these vertexes are also recorded within this class.
 * 
 * Note that the {@link SemanticGraph} may or may not match the graph of the input sentences or the
 * other constituents of the same clause. For example, the semantic graphs are modified when
 * processing of coordinating conjunctions.
 *
 * @author Luciano del Corro
 * @author Kiril Gashteovski
 */
public class IndexedConstituent extends Constituent {

    // -- member variables ------------------------------------------------------------------------

	/** Semantic graph for this sentence */
    //protected static SemanticGraph sentSemanticGraph;
	//protected SemanticGraph sentSemanticGraph;
	
    /** Semantic graph for this constituent */
    private SemanticGraph semanticGraph;

    /** Additional root vertexes that form this constituent. These vertexes and all their descendants
     * are part of the constituent (unless they appear in {@link #excludedVertexes}). */
    private Set<IndexedWord> additionalVertexes;

    /** Vertexes that are excluded from this constituent. All descendants are excluded as well
     * (unless they appear in {@link #root} or {@link additionalRoots}). */
    public Set<IndexedWord> excludedVertexes;

    // -- construction ----------------------------------------------------------------------------

    protected IndexedConstituent() {
    }

    /** Constructs a new indexed constituent.
     * 
     * @param semanticGraph Semantic graph for this constituent ({@see #semanticGraph})
     * @param root The root vertex of this constituent ({@see {@link #root})
     * @param additionalVertexes Additional root vertexes that form this constituent ({@see
     *            {@link #additionalVertexes})
     * @param excludedVertexes Vertexes that are excluded from this constituent ({@see
     *            {@link #excludedVertexes})
     * @param type type of this constituent 
     */
    public IndexedConstituent(SemanticGraph semanticGraph, IndexedWord root, Set<IndexedWord> additionalVertexes, 
            Set<IndexedWord> excludedVertexes, Type type) {
        super(type);
        this.semanticGraph = semanticGraph;
        this.root = root;
        this.additionalVertexes = new TreeSet<IndexedWord>(additionalVertexes);
        this.excludedVertexes = new TreeSet<IndexedWord>(excludedVertexes);
    }

    /** Constructs a simple indexed constituent without additional additional or excluded vertexes.
     * 
     * @param semanticGraph Semantic graph for this constituent ({@see #semanticGraph})
     * @param root The root vertex of this constituent ({@see {@link #root})
     * @param type type of this constituent 
     */
    public IndexedConstituent(SemanticGraph semanticGraph, IndexedWord root, Type type) {
        this(semanticGraph, root, new TreeSet<IndexedWord>(), new TreeSet<IndexedWord>(), type);
    }

    /** Creates a deep copy of this indexed constituent. */
    @Override
	public IndexedConstituent clone() {
        IndexedConstituent clone = new IndexedConstituent();
        clone.type = type;
        clone.semanticGraph = new SemanticGraph(semanticGraph);
        clone.root = this.root;
        clone.additionalVertexes = new TreeSet<IndexedWord>(this.additionalVertexes);
        clone.excludedVertexes = new TreeSet<IndexedWord>(this.excludedVertexes);
        return clone;
    }

    // -- getters/setters -------------------------------------------------------------------------

    /** Returns the semantic graph for this constituent ({@see #semanticGraph}). */
    public SemanticGraph getSemanticGraph() {
        return semanticGraph;
    }
    
    /** Returns the semantic graph for this sentence ({@see #sentSemanticGraph}). */
    /*public SemanticGraph getSentSemanticGraph() {
        return sentSemanticGraph;
    }*/

    /** Sets the semantic graph for this constituent ({@see #semanticGraph}). */
    public void setSemanticGraph(SemanticGraph newSemanticGraph) {
        this.semanticGraph = newSemanticGraph;
    }

    /** Returns the root vertex of this constituent ({@see {@link #root}). */
    public IndexedWord getRoot() {
        return root;
    }

    /** Sets the root vertex of this constituent ({@see {@link #root}). */
    public void setRoot(IndexedWord newRoot) {
        root = newRoot;
    }

    /** Returns additional root vertexes that form this constituent ({@see
     * {@link #additionalVertexes}). */
    public Set<IndexedWord> getAdditionalVertexes() {
        return additionalVertexes;
    }

    /** Returns vertexes that are excluded from this constituent ({@see {@link #excludedVertexes}). */
    public Set<IndexedWord> getExcludedVertexes() {
        return excludedVertexes;
    }

    /** Checks whether this constituent is a prepositional phrase (i.e., starts with a preposition). */
    public boolean isPrepositionalPhrase(SemanticGraph sentSemanticGraph) { //This is a mess, find other way of fixing. This is purelly heuristic. 
    	//It needs to know the semantic graph for the sentence after this is fixed the member variable sentSemanticGraph 
    	//can be removed
    	List<IndexedWord> parents = semanticGraph.getParentList(root); 	//This is not the cleanest way semantics messed up. 
    																	//specially with the rel we cannot just check if 
    																	//the head is a preposition 
    																	//(return root.tag().equals("IN")) because the 
    																	//parser some times includes a preposition in the 
    																	//verbal phrase "He is about to win"
    	for(IndexedWord parent: parents) {
    		SemanticGraphEdge edge = semanticGraph.getEdge(parent, root);
    		if(DpUtils.isRel(edge))
    			return true;
    		if(DpUtils.isAnyPrep(edge)) {
    			List<IndexedWord> ancestors = sentSemanticGraph.getParentList(parent);
    			
    			for(IndexedWord ancestor: ancestors) {
    				SemanticGraphEdge ed = sentSemanticGraph.getEdge(ancestor, parent);
    				if(DpUtils.isRcmod(ed))
    					return true;
    			}
    			
    		}
    	}
    	return false;
        //return root.tag().equals("IN");
    }

    // -- utility methods -------------------------------------------------------------------------

    /** Returns a textual representation of the root word of this constituent. */
    public String rootString() {
        return root.originalText();
    }

    /** Returns a copy of the semantic graph of this constituent in which all edges (from any 
     * included vertex) to excluded vertexes have been removed. Useful for proposition generation. */
    public SemanticGraph createReducedSemanticGraph() {
        SemanticGraph result = new SemanticGraph(semanticGraph);
        DpUtils.removeEdges(result,  root,  excludedVertexes);
        for (IndexedWord v : additionalVertexes) {
            DpUtils.removeEdges(result,  v,  excludedVertexes);
        }
        return result;
    }
    
    public void setAdditionalVertexes(Set<IndexedWord> aVertexes){
        this.additionalVertexes = aVertexes;
    }
    public void addVertexToAdditionalVertexes(IndexedWord w){
        this.additionalVertexes.add(w);
    }
    
    public Type getConstituentType(){
        return this.type;
    }
}
