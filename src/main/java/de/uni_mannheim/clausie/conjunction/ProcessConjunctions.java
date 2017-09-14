package de.uni_mannheim.clausie.conjunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.uni_mannheim.clausie.Options;
import de.uni_mannheim.clausie.clause.Clause;
import de.uni_mannheim.clausie.constituent.Constituent;
import de.uni_mannheim.clausie.constituent.IndexedConstituent;
import de.uni_mannheim.clausie.constituent.Constituent.Type;
import de.uni_mannheim.utils.coreNLP.DpUtils;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/** This is a provisory implementation of the processing of coordinating conjunctions.
 * 
 * Coordinating conjunctions are still a difficult issue for the parser and therefore 
 * the source of a significant loss in precision by ClausIE.
 * 
 * Code is not clean or optimally efficient. More work needs to be done in how to handle CCs.
 *
 * @author Luciano del Corro
 * @author Kiril Gashteovski
 *
 */
public class ProcessConjunctions {
	
    /** Process CCs of a given constituent */
    public static ObjectArrayList<Constituent> processCC(Clause clause, Constituent constituent, int index) {
        return generateConstituents(clause, (IndexedConstituent) constituent, index);
    }

    /** Generates a set of constituents from a CC for a given constituent */    
    private static ObjectArrayList<Constituent> generateConstituents(Clause clause, IndexedConstituent constituent, 
            int index) {
        IndexedConstituent copy = constituent.clone();
        copy.setSemanticGraph( copy.createReducedSemanticGraph() );
        ObjectArrayList<Constituent> result = new ObjectArrayList<Constituent>();
        result.add(copy);
        generateConstituents(copy.getSemanticGraph(), copy, copy.getRoot(), result, true);
        return result;
    }

    // Process CCs by exploring the graph from one constituent and generating more constituents as it encounters ccs
    private static void generateConstituents(SemanticGraph semanticGraph, IndexedConstituent constituent, IndexedWord root, 
            ObjectArrayList<Constituent> constituents, boolean firstLevel) {

        List<SemanticGraphEdge> outedges = semanticGraph.getOutEdgesSorted(root);
        List<SemanticGraphEdge> conjunct = DpUtils.getEdges(outedges, EnglishGrammaticalRelations.COORDINATION);
        
        Boolean processCC = true;
        SemanticGraphEdge predet = null;
        
        // To avoid processing under certain circumstances must be design properly when final setup is decided 
        if (conjunct != null) {
            SemanticGraphEdge con = DpUtils.findFirstOfRelation(outedges, EnglishGrammaticalRelations.QUANTIFIER_MODIFIER);
            if (con != null && con.getDependent().lemma().equals("between"))
                processCC = false;
            List<SemanticGraphEdge> inedg = semanticGraph.getIncomingEdgesSorted(root);
            SemanticGraphEdge pobj = DpUtils.findFirstOfRelation(inedg, EnglishGrammaticalRelations.PREPOSITIONAL_OBJECT);
			// this wont work with collapsed dependencies
			if (pobj != null && pobj.getGovernor().lemma().equals("between"))
				processCC = false;
			
            Collection<IndexedWord> sibs = semanticGraph.getSiblings(root);
            for (IndexedWord sib : sibs) {
                List<SemanticGraphEdge> insib = semanticGraph.getIncomingEdgesSorted(sib);
                predet = DpUtils.findFirstOfRelation(insib, EnglishGrammaticalRelations.PREDETERMINER);
                if (predet == null)
                    predet = DpUtils.findFirstOfRelation(insib, EnglishGrammaticalRelations.DETERMINER);
                if (predet != null)
                    break;
            }
        }
       
        for (SemanticGraphEdge edge : outedges) {
            if (DpUtils.isParataxis(edge) || DpUtils.isRcmod(edge) || DpUtils.isAppos(edge) ||(DpUtils.isDep(edge) && 
                    constituent.getType().equals(Type.VERB) ) ) 
                continue; //to avoid processing relative clauses and appositions which are included as an independent 
        				  //clause in the clauses list of the sentence, also no dep in verbs are processed. To reproduce 
        	              //the results of the paper comment this line and eliminate the duplicate propositions that may be 
        				  //generated.
        	
            if (DpUtils.isAnyConj(edge) && processCC) {           	
                boolean cont = false;
                for(SemanticGraphEdge c : conjunct) {
                    if(c.getDependent().lemma().equals("&") && 
                       nextToVerb(root, edge.getDependent(), c.getDependent(), semanticGraph, 
                                  semanticGraph.getOutEdgesSorted(root))) {
                        cont = true;
                        break;
                    }
                }
            	
                if(cont)
            		continue;
            	
                IndexedWord newRoot = edge.getDependent();
                SemanticGraph newSemanticGraph = new SemanticGraph(semanticGraph);
                if(predet != null && predet.getDependent().lemma().equals("both"))
                    constituent.getExcludedVertexes().add(predet.getDependent()); 
                
                IndexedConstituent newConstituent = constituent.clone();
                newConstituent.setSemanticGraph(newSemanticGraph);
                if (firstLevel)
                    newConstituent.setRoot(newRoot);
                constituents.add(newConstituent);
                
                // Assign all the parents to the conjoint
                Collection<IndexedWord> parents = newSemanticGraph.getParents(root);
                for (IndexedWord parent : parents) {
                    GrammaticalRelation reln = newSemanticGraph.reln(parent, root);
                    double weight = newSemanticGraph.getEdge(parent, root).getWeight();
                    newSemanticGraph.addEdge(parent, newRoot, reln, weight, false);
                }

                // Checks if the children also belong to the conjoint and if they do, it assigns them
                for (SemanticGraphEdge ed : outedges) {
                    IndexedWord child = ed.getDependent();
                    if(DpUtils.isPredet(ed) && ed.getDependent().lemma().equals("both")) { //if it is one level down
                    	semanticGraph.removeEdge(ed);
                    } else if (!DpUtils.isAnyConj(ed) && !DpUtils.isCc(ed) && !DpUtils.isPreconj(ed)
                            && isDescendant(newRoot, root, child, semanticGraph)) {
                    	/*
                    	 * Tree parse, IndexedWord root, IndexedWord conj, 
    		SemanticGraph semGraph, List<SemanticGraphEdge> outedges, SemanticGraphEdge edge, GrammaticalRelation rel
                    	 */
                        GrammaticalRelation reln = newSemanticGraph.reln(root, child);
                        double weight = newSemanticGraph.getEdge(root, child).getWeight();
                        newSemanticGraph.addEdge(newRoot, child, reln, weight, false);
                    }
                }

                // Disconnect the root of the conjoint from the new graph
                List<SemanticGraphEdge> inedges = newSemanticGraph.getIncomingEdgesSorted(root);
                for (SemanticGraphEdge redge : inedges)
                    newSemanticGraph.removeEdge(redge);
                semanticGraph.removeEdge(edge);

                // It passes the constituent with the correct root, if it is the first level it should be the new 
                // constituent
                if (firstLevel) {
                    generateConstituents(newSemanticGraph, newConstituent, newRoot, constituents, false);
                } else {
                    generateConstituents(newSemanticGraph, constituent, newRoot, constituents, false);
                }
                
                // deletes the edge containing the conjunction e.g. and, or, but, etc
            } else if ((DpUtils.isCc(edge) || DpUtils.isPreconj(edge)) && processCC && 
                        !edge.getDependent().lemma().equals("&")) {
                semanticGraph.removeEdge(edge);
            } else if(!DpUtils.isPredet(edge) && !constituent.excludedVertexes.contains(edge.getDependent())){
            	generateConstituents(semanticGraph, constituent, edge.getDependent(), constituents, false);
            }
        }
    }

    /** Retrieves the heads of the clauses according to the CCs processing options. 
     *  The result contains verbs conjoined and a complement if it is conjoined with a verb.
     */
    public static List<IndexedWord> getIndexedWordsConj(SemanticGraph semanticGraph, IndexedWord root, 
            GrammaticalRelation rel, List<SemanticGraphEdge> toRemove, Options option) {
        List<IndexedWord> ccs = new ArrayList<IndexedWord>(); // to store the conjoints
        ccs.add(root);
        List<SemanticGraphEdge> outedges = semanticGraph.outgoingEdgeList(root);
        for (SemanticGraphEdge edge : outedges) {
            if (edge.getRelation().equals(rel)) {
                List<SemanticGraphEdge> outed = semanticGraph.outgoingEdgeList(edge.getDependent());
                // First condition tests if verbs are involved in the conjoints. Conjunctions between complements are 
                // treated elsewhere. 
                boolean ccVerbs = edge.getDependent().tag().charAt(0) == 'V' || edge.getGovernor().tag().charAt(0) == 'V';
                //This condition will check if there is a cop conjoined with a verb
                boolean ccCop = DpUtils.findFirstOfRelationOrDescendent(outed, EnglishGrammaticalRelations.COPULA) != null;               
                // this condition checks if there are two main clauses conjoined by the CC
                boolean ccMainClauses = DpUtils.findFirstOfRelationOrDescendent(outed,
                        EnglishGrammaticalRelations.SUBJECT) != null ||  
                        DpUtils.findFirstOfRelationOrDescendent(outed, EnglishGrammaticalRelations.EXPLETIVE) != null;
                
                // This flag will check if the cc should be processed according to the flag and the
                // shared elements.
                boolean notProcess = !option.processCcAllVerbs && outed.isEmpty()
                        && shareAll(outedges, root, edge.getDependent(), semanticGraph, rel);

                if ((ccVerbs || ccCop) && !ccMainClauses && !notProcess) {
                	ccs.add(edge.getDependent());
                 }
                    
                // Disconnects the conjoints. Independent clauses are always disconnected.
                if (((ccVerbs || ccCop) && !notProcess) || ccMainClauses) {
                    toRemove.add(edge);
                    
                    //To remove the coordination
                    if (option.processCcAllVerbs || !notProcess) {
                        List<SemanticGraphEdge> conjunct = DpUtils.getEdges(outedges, 
                                EnglishGrammaticalRelations.COORDINATION);
                        for (SemanticGraphEdge e : conjunct) {
                            if (e.getDependent().index() > edge.getDependent().index())
                                continue;
                            if (nextToVerb(root, edge.getDependent(), e.getDependent(), semanticGraph, 
                            		semanticGraph.getOutEdgesSorted(root))) {
                                toRemove.add(e);
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        if(ccs.size() > 1)
            rewriteGraph(semanticGraph, ccs);
        
        return ccs;
    }

    /** Rewrites the graph so that each conjoint is independent from each other.
     *  They will be disconnected and each dependent correspondignly assigned. 
     */
    private static void rewriteGraph(SemanticGraph semanticGraph, List<IndexedWord> ccs) {
        
    	for(int i = 0; i < ccs.size(); i++) {
    		for(int j = i + 1; j < ccs.size(); j++) {
    			for (SemanticGraphEdge ed : semanticGraph.getIncomingEdgesSorted(ccs.get(i))) {
    	        	if(semanticGraph.getParents(ccs.get(j)).contains(ed.getGovernor())) continue;
    	        	semanticGraph.addEdge(ed.getGovernor(), ccs.get(j), ed.getRelation(), ed.getWeight(), false);
    	        }

    	        for (SemanticGraphEdge ed : semanticGraph.getOutEdgesSorted(ccs.get(i))) {
    	            IndexedWord child = ed.getDependent();
    	            if(semanticGraph.getChildren(ccs.get(j)).contains(child)) continue;
    	            if (!DpUtils.isAnyConj(ed) && !DpUtils.isCc(ed) && 
    	                    isDescendant(ccs.get(j), ccs.get(i), child, semanticGraph)) {
    	                semanticGraph.addEdge(ccs.get(j), child, ed.getRelation(), ed.getWeight(), false);
    	            }
    	        }
    		}
    	}    	
	}
    
    /** Checks if two nodes are conjoined by a given conjunction */
    private static boolean nextToVerb(IndexedWord firstVerb, IndexedWord secondVerb, IndexedWord conj, 
            SemanticGraph semGraph, List<SemanticGraphEdge> sortedOutEdges){
    	// The index of the 'conj' relation in 'sortedOutEdges'. Initially, set to -1.
    	int conjIndex = -1;
    	
    	// Go to the "conj" relation linking the first and the second conjoin.
    	// Store the relation index in 'conjIndex'
    	for (int i=0; i<=sortedOutEdges.size(); i++){
    		SemanticGraphEdge edge = sortedOutEdges.get(i);

    		if (edge.getSource().equals(firstVerb) && edge.getTarget().equals(secondVerb) && 
    				edge.getRelation().getShortName().equals("conj")){
    			conjIndex = i;
    			break;
    		}
    	}
    	
    	// Move to the first relation to the left. If it is is a "cc" relation, this is the relation corresponding to 
    	// the coordinating conjunction (return true), else -> return false
    	if (conjIndex > 0){
    		SemanticGraphEdge leftConjEdge = sortedOutEdges.get(conjIndex-1);
    		if (leftConjEdge.getRelation().getShortName().equals("cc")){
    			return true;
    		} else {
    			return false;
    		}
    	} else {
    		return false;
    	}
    }

    /** Checks if two conjoints verbs share all dependents */
    private static boolean shareAll(List<SemanticGraphEdge> outedges, IndexedWord root, IndexedWord conj, 
            SemanticGraph semGraph, GrammaticalRelation rel) {
        for (SemanticGraphEdge edge : outedges) {
            if (DpUtils.isAnySubj(edge) || edge.getDependent().equals(conj))
                continue;
            else if (!isDescendant(conj, root, edge.getDependent(), semGraph))
                return false;
        }

        return true;
    }
    
    /** Checks if a node depending on one conjoint also depends to the other */
    //"He buys and sells electronic products" "Is products depending on both sells and buys?"
    private static boolean isDescendant(IndexedWord checkWord, IndexedWord pivotWord, IndexedWord elementWord, 
            SemanticGraph semGraph) {
        Collection <IndexedWord> roots = semGraph.getRoots();
        
        while (!roots.contains(elementWord)){
            if (!semGraph.getShortestUndirectedPathNodes(elementWord, pivotWord).isEmpty())
                break;
            elementWord = semGraph.getParent(elementWord);
        }
        List<SemanticGraphEdge> path = semGraph.getShortestDirectedPathEdges(elementWord, checkWord);
        if (path == null)
            return false;
        else
            return true;
    }
}
