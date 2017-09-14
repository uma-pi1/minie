package de.uni_mannheim.clausie.clause;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.uni_mannheim.clausie.ClausIE;
import de.uni_mannheim.clausie.Options;
import de.uni_mannheim.clausie.conjunction.ProcessConjunctions;
import de.uni_mannheim.clausie.constituent.Constituent;
import de.uni_mannheim.clausie.constituent.IndexedConstituent;
import de.uni_mannheim.clausie.constituent.PhraseConstituent;
import de.uni_mannheim.clausie.constituent.XcompConstituent;
import de.uni_mannheim.clausie.constituent.Constituent.Type;
import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.constant.NE_TYPE;
import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.utils.coreNLP.DpUtils;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;

import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * {@link ClauseDetector} contains the methods dealing with the detection of clauses.
 * After the detection is performed a set of {@link Clause} is created.
 * 
 * {@code detectClauses} first detects the type of clause to be generated based on syntactic relations
 * and once a clause is detected a given method is used to create a {@link Clause}.
 *
 * @author Luciano del Corro
 * @author Kiril Gashteovski
 *
 */
public class ClauseDetector {

    /** Set of dependency relations that do not belong to a complement */
    protected static final Set<GrammaticalRelation> EXCLUDE_RELATIONS_COMPLEMENT;
    static {
        HashSet<GrammaticalRelation> temp = new HashSet<GrammaticalRelation>();
        temp.add(EnglishGrammaticalRelations.AUX_MODIFIER);
        temp.add(EnglishGrammaticalRelations.AUX_PASSIVE_MODIFIER);
        temp.add(EnglishGrammaticalRelations.SUBJECT);
        temp.add(EnglishGrammaticalRelations.COPULA);
        temp.add(EnglishGrammaticalRelations.ADVERBIAL_MODIFIER);
        EXCLUDE_RELATIONS_COMPLEMENT = Collections.unmodifiableSet(temp);
    }

    /** Set of dependency relations that belong to the verb */
    protected static final Set<GrammaticalRelation> INCLUDE_RELATIONS_VERB;
    static {
        HashSet<GrammaticalRelation> temp = new HashSet<GrammaticalRelation>();
        temp.add(EnglishGrammaticalRelations.AUX_MODIFIER);
        temp.add(EnglishGrammaticalRelations.AUX_PASSIVE_MODIFIER);
        temp.add(EnglishGrammaticalRelations.NEGATION_MODIFIER);
        INCLUDE_RELATIONS_VERB = Collections.unmodifiableSet(temp);
    }

    private ClauseDetector() {
    };

    /** Detects clauses in the input sentence */
    public static void detectClauses(ClausIE clausIE) {
        //IndexedConstituent.sentSemanticGraph = clausIE.getSemanticGraph();
        List<IndexedWord> roots = new ArrayList<IndexedWord>();
        
        for (SemanticGraphEdge edge : clausIE.getSemanticGraph().edgeIterable()) {
            // check whether the edge identifies a clause
            if (DpUtils.isAnySubj(edge)) {
                // clauses with a subject
                IndexedWord subject = edge.getDependent();
                IndexedWord root = edge.getGovernor();
                addNsubjClause(clausIE, roots, clausIE.getClauses(), subject, root, false);
            } else if (clausIE.getOptions().processAppositions && DpUtils.isAppos(edge)) {
                // clauses for appositions
                // TODO: appositions (synthetic clauses)
                IndexedWord subject = edge.getGovernor();
                IndexedWord object = edge.getDependent();
                addApposClause(clausIE, subject, object);
                roots.add(null);
                // If the object is a PERSON, then create another extraction where subject and object switch places
                if (object.ner().equals(NE_TYPE.PERSON)){
                    addApposClause(clausIE, object, subject);
                    roots.add(null);
                }
            } else if (clausIE.getOptions().processPossessives && DpUtils.isPoss(edge)) {
                // clauses for possessives
                IndexedWord subject = edge.getDependent();
                IndexedWord object = edge.getGovernor();
                addPossessiveClause(clausIE, subject, object);
                roots.add(null);
            } else if (clausIE.getOptions().processPartmods && DpUtils.isVerbMod(edge)) {
                // clauses for participial modifiers
                IndexedWord subject = edge.getGovernor();
                IndexedWord object = edge.getDependent();
                addPartmodClause(clausIE, subject, object, roots);
            } 
        }

        // postprocess clauses
        // TODO
        for (int i = 0; i < clausIE.getClauses().size(); i++) {
            Clause clause = clausIE.getClauses().get(i);

            // set parents (slow and inefficient for now)
            IndexedWord root = roots.get(i);
            if (root != null) {
                int index = ancestorOf(clausIE.getSemanticGraph(), root, roots); // recursion needed to
                                                                            // deal
                // with xcomp; more stable
                if (index >= 0) {
                    // System.out.println("Clause " + clause.toString() + " has parent " +
                    // clausIE.clauses.get(index).toString());
                    clause.parentClause = clausIE.getClauses().get(index);
                }
            }

            // exclude vertexes (each constituent needs to excludes vertexes of the other constituents)
            excludeVertexes(clause);
        }
    }

    /** Adds in the exclude vertex of a clause the head of the rest of the clauses */
    private static void excludeVertexes(Clause clause) {

        for (int j = 0; j < clause.constituents.size(); j++) {
            if (!(clause.constituents.get(j) instanceof IndexedConstituent))
                continue;
            IndexedConstituent constituent = (IndexedConstituent) clause.constituents.get(j);

            for (int k = 0; k < clause.constituents.size(); k++) {
                if (k == j || !(clause.constituents.get(k) instanceof IndexedConstituent))
                    continue;
                IndexedConstituent other = (IndexedConstituent) clause.constituents.get(k);

                constituent.getExcludedVertexes().add(other.getRoot());
                constituent.getExcludedVertexes().addAll(other.getAdditionalVertexes());
            }
        }
    }

    /** TODO */
    private static int ancestorOf(SemanticGraph semanticGraph, IndexedWord node, List<IndexedWord> ancestors) {
        for (SemanticGraphEdge e : semanticGraph.getIncomingEdgesSorted(node)) {
            int index = ancestors.indexOf(node);
            if (index >= 0)
                return index;
            index = ancestorOf(semanticGraph, e.getGovernor(), ancestors);
            if (index >= 0)
                return index;
        }
        return -1;
    }

    /** Selects constituents of a clause for clauses with internal subject or coming from a participial modifier  
     * @param roots The list of roots of the clauses in the sentence
     * @param clauses The list of clauses in the sentence
     * @param subject The subject of the clause
     * @param clauseRoot The root of the clause, either a verb or a complement
     * @param partmod Indicates if the clause is generated from a partmod relation
     */
    private static void addNsubjClause(ClausIE clausIE, List<IndexedWord> roots,
            ObjectArrayList<Clause> clauses, IndexedWord subject, IndexedWord clauseRoot, boolean partmod) {
    	
        SemanticGraph semanticGraph = new SemanticGraph(clausIE.getSemanticGraph());
        Options options = clausIE.getOptions();

        List<SemanticGraphEdge> toRemove = new ArrayList<SemanticGraphEdge>();
        //to store the heads of the clauses according to the CCs options
        
        
        List<IndexedWord> ccs = ProcessConjunctions.getIndexedWordsConj(semanticGraph, clauseRoot, 
                EnglishGrammaticalRelations.CONJUNCT, toRemove, options);
        
        for (SemanticGraphEdge edge : toRemove)
            semanticGraph.removeEdge(edge);
        
        //A new clause is generated for each clause head
        for (int i = 0; i < ccs.size(); i++) {
            IndexedWord root = ccs.get(i);
            // TODO: this part (outgoingEdges) is empty for 'distributes' in the "Bell sentence". In the old version of
            // CoreNLP, it gives dobj and nsubj as a result. This was a bug actually. The reason why it doesn't extract
            // "Bell" "distributes" "electronic products" ... is because of this.  
            List<SemanticGraphEdge> outgoingEdges = semanticGraph.getOutEdgesSorted(root);
            List<SemanticGraphEdge> incomingEdges = semanticGraph.getIncomingEdgesSorted(root);
            
            // initialize clause
            Clause clause = new Clause();
            clause.verb = -1;
            SemanticGraphEdge cop = DpUtils.findFirstOfRelation(outgoingEdges, EnglishGrammaticalRelations.COPULA);
            Set<IndexedWord> exclude = null;
            Set<IndexedWord> include = null;
            if (cop != null) {
                exclude = DpUtils.exclude(semanticGraph, EXCLUDE_RELATIONS_COMPLEMENT, root);
                include = DpUtils.exclude(semanticGraph, INCLUDE_RELATIONS_VERB, root);
            } else {
                exclude = new HashSet<IndexedWord>();
            }

            // relative clause?
            SemanticGraphEdge rcmod = DpUtils.findFirstOfRelation(incomingEdges, 
                                                                  EnglishGrammaticalRelations.RELATIVE_CLAUSE_MODIFIER);
            SemanticGraphEdge poss = null;
            if (rcmod != null) {
                poss = DpUtils.findDescendantRelativeRelation(semanticGraph, root, 
                                                              EnglishGrammaticalRelations.POSSESSION_MODIFIER);
            }
            
            // determine constituents of clause
            //ArrayList<IndexedWord> coordinatedConjunctions = new ArrayList<IndexedWord>(); // to
            // store
            // potential
            // conjunctions

//------------------------Set verb or complement, and subject.-------------------------------------------------
            Constituent constRoot = null;
            if (cop != null) {
                clause.complement = clause.constituents.size();
                constRoot = new IndexedConstituent(semanticGraph, root, Collections.<IndexedWord> emptySet(), exclude, 
                                                   Constituent.Type.COMPLEMENT);
                clause.constituents.add(constRoot);

                clause.verb = clause.constituents.size();
                if (!partmod) {
                    clause.constituents.add(new IndexedConstituent(semanticGraph, cop.getDependent(), include, 
                            Collections.<IndexedWord> emptySet(), Constituent.Type.VERB));
                } else {
                    // Make a phrase consisted of be + clauseRoot
                    Phrase bePhrase = new Phrase();
                    IndexedWord beWord = new IndexedWord();
                    beWord.setWord("be");
                    beWord.setTag(POS_TAG.VB);
                    beWord.setNER(NE_TYPE.NO_NER);
                    beWord.setLemma("be");
                    beWord.setValue("be");
                    beWord.setIndex(-2);
                    bePhrase.addWordToList(beWord);
                    bePhrase.addWordToList(clauseRoot);
                    bePhrase.setRoot(clauseRoot);
                    
                    clause.constituents.add(new PhraseConstituent(bePhrase, Constituent.Type.VERB));
                }

            } else {
                clause.verb = clause.constituents.size();
                if (!partmod) {
                    constRoot = new IndexedConstituent(semanticGraph, root, Collections.<IndexedWord> emptySet(), exclude, 
                                                       Constituent.Type.VERB);
                } else {
                    // Make a phrase consisted of be + clauseRoot
                    Phrase bePhrase = new Phrase();
                    IndexedWord beWord = new IndexedWord();
                    beWord.setWord("be");
                    beWord.setOriginalText("be");
                    beWord.setTag(POS_TAG.VB);
                    beWord.setNER(NE_TYPE.NO_NER);
                    beWord.setLemma("be");
                    beWord.setValue("be");
                    beWord.setIndex(-2);
                    bePhrase.addWordToList(beWord);
                    bePhrase.addWordToList(clauseRoot);
                    bePhrase.setRoot(clauseRoot);
                    
                    constRoot = new PhraseConstituent(bePhrase, Constituent.Type.VERB);
                }

                clause.constituents.add(constRoot);
            }

            clause.setSubject(clause.constituents.size());
            if (subject.tag().charAt(0) == 'W' && rcmod != null) {
                clause.constituents.add(createRelConstituent(semanticGraph, rcmod.getGovernor(), Type.SUBJECT));
                ((IndexedConstituent) constRoot).getExcludedVertexes().add(subject);
                rcmod = null;
            } else if (poss != null && poss.getGovernor().equals(subject) && rcmod != null) {
                clause.constituents.add(createPossConstituent(semanticGraph, poss, rcmod, subject, Type.SUBJECT));
                rcmod = null;
            } else if (partmod && subject.tag().charAt(0) == 'V') {
                List<SemanticGraphEdge> outsub = clausIE.getSemanticGraph().getOutEdgesSorted(subject);
                SemanticGraphEdge sub = DpUtils.findFirstOfRelationOrDescendent(outsub, EnglishGrammaticalRelations.SUBJECT);
                if (sub != null)
                    clause.constituents.add(new IndexedConstituent(semanticGraph, sub.getDependent(), 
                                                                   Constituent.Type.SUBJECT));
                else
                    clause.constituents.add(new IndexedConstituent(semanticGraph, subject, Constituent.Type.SUBJECT));

            } else
                clause.constituents.add(new IndexedConstituent(semanticGraph, subject, Constituent.Type.SUBJECT));
            
            //If the clause comes from a partmod construction exclude necesary vertex
            if (partmod) {
                ((IndexedConstituent) clause.constituents.get(clause.getSubject())).excludedVertexes.add(clauseRoot);
                // He is the man crying the whole day.
                List<SemanticGraphEdge> outsub = clausIE.getSemanticGraph().getOutEdgesSorted(subject);
                SemanticGraphEdge coppm = DpUtils.findFirstOfRelationOrDescendent(outsub, 
                                                                                  EnglishGrammaticalRelations.COPULA);
                if (coppm != null) {
                    ((IndexedConstituent) clause.constituents.get(clause.getSubject())).excludedVertexes.add(
                                                                                                      coppm.getDependent());
                    SemanticGraphEdge spm = DpUtils.findFirstOfRelationOrDescendent(outsub, 
                                                                                    EnglishGrammaticalRelations.SUBJECT);
                    if (spm != null){
                        ((IndexedConstituent) clause.constituents.get(clause.getSubject())).excludedVertexes.add(
                                                                                                      spm.getDependent());
                    }
                }
            }

 //------------------------Select constituents of the predicate-------------------------------------------------
            for (SemanticGraphEdge outgoingEdge : outgoingEdges) {
                
                IndexedWord dependent = outgoingEdge.getDependent();

                // to avoid compl or mark in a main clause. "I doubt if she was sure whether this was important".
                if (DpUtils.isMark(outgoingEdge)) {
                    // For TextConstituent, no need to add excluded vertexes, since the constituent is just a text
                    if (constRoot instanceof IndexedConstituent)
                        ((IndexedConstituent) constRoot).getExcludedVertexes().add(dependent);
                //Indirect Object
                } else if (DpUtils.isIobj(outgoingEdge)) {
                    clause.iobjects.add(clause.constituents.size());
                    //If it is a relative clause headed by a relative pronoun.
                    if (dependent.tag().charAt(0) == 'W' && rcmod != null) {
                        clause.constituents.add(createRelConstituent(semanticGraph, rcmod.getGovernor(), Type.IOBJ));
                        ((IndexedConstituent) constRoot).getExcludedVertexes().add(dependent);
                        rcmod = null;
                    //to deal with the possessive relative pronoun     
                    } else if (poss != null && poss.getGovernor().equals(dependent) && rcmod != null) {
                        clause.constituents.add(createPossConstituent(semanticGraph, poss, rcmod, dependent, Type.IOBJ));
                        rcmod = null;
                    // "regular case"    
                    } else
                        clause.constituents.add(new IndexedConstituent(semanticGraph, dependent, Constituent.Type.IOBJ));
                //Direct Object
                } else if (DpUtils.isDobj(outgoingEdge)) {
                    clause.dobjects.add(clause.constituents.size());
                    if (dependent.tag().charAt(0) == 'W' && rcmod != null) {
                        clause.constituents.add(createRelConstituent(semanticGraph, rcmod.getGovernor(), Type.DOBJ));
                        ((IndexedConstituent) constRoot).getExcludedVertexes().add(dependent);
                        rcmod = null;
                    } else if (poss != null && poss.getGovernor().equals(dependent) && rcmod != null) {
                        clause.constituents.add(createPossConstituent(semanticGraph, poss, rcmod, dependent, Type.DOBJ));
                        rcmod = null;
                    } else
                        clause.constituents.add(new IndexedConstituent(semanticGraph, dependent, Constituent.Type.DOBJ));
                //CCOMPS
                } else if (DpUtils.isCcomp(outgoingEdge)) {
                    clause.ccomps.add(clause.constituents.size());
                    clause.constituents.add(new IndexedConstituent(semanticGraph, dependent, Constituent.Type.CCOMP));
                //XCOMPS (Note: Need special treatment, they won't form a new clause so optional/obligatory constituents
                // are managed within the context of its parent clause)
                } else if (DpUtils.isXcomp(outgoingEdge)) {              	
                    List<IndexedWord> xcomproots = new ArrayList<IndexedWord>();
                    ObjectArrayList<Clause> xcompclauses = new ObjectArrayList<Clause>();
                    IndexedWord xcompsubject = null;
                    SemanticGraphEdge xcsub = DpUtils.findFirstOfRelationOrDescendent(
                            semanticGraph.getOutEdgesSorted(outgoingEdge.getDependent()), 
                                                            EnglishGrammaticalRelations.SUBJECT);
                    if (xcsub != null)
                        xcompsubject = xcsub.getDependent();
                    //Need to identify the internal structure of the clause
                    addNsubjClause(clausIE, xcomproots, xcompclauses, subject, outgoingEdge.getDependent(), false);
                    for (Clause cl : xcompclauses) {
                        if (xcompsubject != null) {
                            int verb = cl.verb;
                            ((IndexedConstituent) cl.constituents.get(verb)).addVertexToAdditionalVertexes(xcompsubject);
                        }
                        excludeVertexes(cl);
                    }
                    clause.xcomps.add(clause.constituents.size());
                    clause.constituents.add(new XcompConstituent(semanticGraph, dependent, Constituent.Type.XCOMP, 
                                                                 xcompclauses));
                 //Adjective complement
                } else if (DpUtils.isAcomp(outgoingEdge)) { 
                    clause.acomps.add(clause.constituents.size());
                    clause.constituents.add(new IndexedConstituent(semanticGraph, dependent, Constituent.Type.ACOMP));
                 //Various Adverbials
                } else if ((DpUtils.isAnyPrep(outgoingEdge) || DpUtils.isPobj(outgoingEdge) || DpUtils.isTmod(outgoingEdge)
                        || DpUtils.isAdvcl(outgoingEdge) || DpUtils.isNpadvmod(outgoingEdge))) {
					int constint = clause.constituents.size();
					clause.adverbials.add(constint);
					clause.constituents.add(new IndexedConstituent(semanticGraph, dependent, Constituent.Type.ADVERBIAL));
				//Advmod
				} else if (DpUtils.isAdvmod(outgoingEdge)) {
                    int constint = clause.constituents.size();
                    clause.adverbials.add(constint);
                    clause.constituents.add(new IndexedConstituent(semanticGraph, dependent, Constituent.Type.ADVERBIAL));
                 //Vmod    
                } else if (DpUtils.isVerbMod(outgoingEdge)) { 
                    int constint = clause.constituents.size();
                    clause.adverbials.add(constint);
                    clause.constituents.add(new IndexedConstituent(semanticGraph, dependent, Constituent.Type.ADVERBIAL));
                 // Rel appears in certain cases when relative pronouns act as prepositional objects "I saw the house in 
                 // which I grew".
                 // We generate a new clause out of the relative clause   
                } else if (DpUtils.isRel(outgoingEdge)) {
                	processRel(outgoingEdge, semanticGraph, dependent, rcmod, clause);
                	rcmod = null;
                	
                //To process passive voice (!Not done here)
               // } else if (DpUtils.isAgent(outgoingEdge))
               //     clause.agent = dependent;
               // else if (DpUtils.isMark(outgoingEdge) || DpUtils.isComplm(outgoingEdge)) {
                    // clause.subordinateConjunction = dependent;
                } else if (DpUtils.isExpl(outgoingEdge))
                    clause.setType(Clause.Type.EXISTENTIAL);
              //  else if (options.processCcAllVerbs && DpUtils.isAnyConj(outgoingEdge))
               //     coordinatedConjunctions.add(dependent);
            }

 //------------------------To process relative clauses with implicit (zero) relative pronoun-------------------------
            if (rcmod != null) { //"I saw the house I grew up in", "I saw
                                 // the house I like", "I saw the man I gave the book" ...
                Constituent candidate = searchCandidateAdverbial(clause);
                if (candidate != null) {
                    SemanticGraph newSemanticGraph = new SemanticGraph(((IndexedConstituent) candidate).getSemanticGraph());
                    IndexedConstituent tmpconst = createRelConstituent(newSemanticGraph, rcmod.getGovernor(), Type.ADVERBIAL);
                    newSemanticGraph.addEdge(((IndexedConstituent) candidate).getRoot(), rcmod.getGovernor(), 
                            EnglishGrammaticalRelations.PREPOSITIONAL_OBJECT, rcmod.getWeight(), false);
                    ((IndexedConstituent) candidate).getExcludedVertexes().addAll(tmpconst.getExcludedVertexes());
                    ((IndexedConstituent) candidate).setSemanticGraph(newSemanticGraph);
                    rcmod = null;
                } else if (DpUtils.findFirstOfRelation(outgoingEdges, EnglishGrammaticalRelations.DIRECT_OBJECT) == null) {
                    clause.dobjects.add(clause.constituents.size());
                    clause.constituents.add(createRelConstituent(semanticGraph, rcmod.getGovernor(), Type.DOBJ));
                    rcmod = null;
                } else if (DpUtils.findFirstOfRelation(outgoingEdges, EnglishGrammaticalRelations.INDIRECT_OBJECT) == null) {
                    clause.iobjects.add(clause.constituents.size());
                    clause.constituents.add(createRelConstituent(semanticGraph, rcmod.getGovernor(), Type.IOBJ));
                    rcmod = null;
                }
            }
            
//------------------------------------------------------------------------------------------------------------------          
            //To deal with parataxis            
            SemanticGraphEdge parataxis = DpUtils.findFirstOfRelation(incomingEdges, EnglishGrammaticalRelations.PARATAXIS);
            if (parataxis != null && clause.constituents.size() < 3) {
                addParataxisClause(clausIE, parataxis.getGovernor(), parataxis.getDependent(), roots);
                return; // to avoid generating (John, said) in "My dog, John said, is great" //To
                        // deal with the type of parataxis. Parataxis are either like in the example
                        // above or subclauses comming from ":" or ";" this is here because is
                        // difficult to identify the type upfront. Otherwise we can count the potential
                        // constituents upfront and move this up.
            }
            
            
            //Detect type and maintain clause lists
            roots.add(root);
            if (!partmod) {
                clause.detectType(options);
            } else {
                clause.setType(Clause.Type.SVA);
            }
            clauses.add(clause);
        }
    }

    /** Process relation rel, it creates a new clause out of the relative clause 
     * @param outgoingEdge The rel labeled edge
     * @param semanticGraph The semantic graph
     * @param dependent The dependent of the relation
     * @param rcmod The relative clause modifier of the relation refered by rel
     * @param clause A clause
     */
    private static void processRel(SemanticGraphEdge outgoingEdge, SemanticGraph semanticGraph, IndexedWord dependent, 
    		SemanticGraphEdge rcmod, Clause clause) {
    	
    	 SemanticGraph newSemanticGraph = new SemanticGraph(semanticGraph);
         List<SemanticGraphEdge> outdep = newSemanticGraph.getOutEdgesSorted(dependent);
         SemanticGraphEdge pobed = DpUtils.findFirstOfRelation(outdep, EnglishGrammaticalRelations.PREPOSITIONAL_OBJECT);

         SemanticGraphEdge posspobj = null;
         if (pobed != null && pobed.getDependent().tag().charAt(0) != 'W') {
             List<SemanticGraphEdge> outpobj = newSemanticGraph.getOutEdgesSorted(dependent);
             posspobj = DpUtils.findFirstOfRelation(outpobj, EnglishGrammaticalRelations.POSSESSION_MODIFIER);
         }

         if (pobed != null && pobed.getDependent().tag().charAt(0) == 'W' && rcmod != null) {
             newSemanticGraph.addEdge(dependent, rcmod.getGovernor(), EnglishGrammaticalRelations.PREPOSITIONAL_OBJECT,
                     pobed.getWeight(), false);
             newSemanticGraph.removeEdge(pobed);
             int constint = clause.constituents.size();
             clause.adverbials.add(constint);
             clause.constituents.add(createRelConstituent(newSemanticGraph, rcmod.getGovernor(), Type.SUBJECT));
             ((IndexedConstituent) clause.constituents.get(constint)).setRoot(dependent);
             clause.relativeAdverbial = true;
             rcmod = null;
         } else if (pobed != null && posspobj != null && rcmod != null) {
             newSemanticGraph.addEdge(posspobj.getGovernor(), rcmod.getGovernor(),
                     EnglishGrammaticalRelations.POSSESSION_MODIFIER, posspobj.getWeight(), false);
             newSemanticGraph.removeEdge(posspobj);
             int constint = clause.constituents.size();
             clause.adverbials.add(constint);
             // search pobj copy edge.
             clause.constituents.add(createRelConstituent(newSemanticGraph, rcmod.getGovernor(), Type.SUBJECT));
             ((IndexedConstituent) clause.constituents.get(constint)).setRoot(dependent);
             clause.relativeAdverbial = true;
         }
	}

	/** Finds the adverbial to which the relative clause is referring to*/
    private static Constituent searchCandidateAdverbial(Clause clause) {
        for (Constituent c : clause.constituents) {
            IndexedWord root = ((IndexedConstituent) c).getRoot();
            if (root.tag().equals(POS_TAG.IN) && !((IndexedConstituent) c).getSemanticGraph().hasChildren(root))
                return c;
        }
        return null;
    }

    /** Creates a constituent for a possessive relative clause
     * @param semanticGraph The semantic graph
     * @param poss The edge referring to the possessive relation
     * @param rcmod The relative clause modifier of the relation
     * @param constGovernor The root of the constituent
     * @param type The type of the constituent
    */
    private static Constituent createPossConstituent(SemanticGraph semanticGraph,
            SemanticGraphEdge poss, SemanticGraphEdge rcmod, IndexedWord constGovernor, Type type) {
        SemanticGraph newSemanticGraph = new SemanticGraph(semanticGraph);
        double weight = poss.getWeight();
        newSemanticGraph.addEdge(poss.getGovernor(), rcmod.getGovernor(), EnglishGrammaticalRelations.POSSESSION_MODIFIER, 
                                 weight, false);
        Set<IndexedWord> exclude = DpUtils.exclude(newSemanticGraph, EXCLUDE_RELATIONS_COMPLEMENT, rcmod.getGovernor());
        newSemanticGraph.removeEdge(poss);
        newSemanticGraph.removeEdge(rcmod);
        return new IndexedConstituent(newSemanticGraph, constGovernor, Collections.<IndexedWord> emptySet(), exclude, type);
    }

    /** Creates a constituent for the relative clause implied by rel 
     * @param semanticGraph The semantic graph
     * @param root The root of the constituent
     * @param type The type of the constituent
     */
    private static IndexedConstituent createRelConstituent(SemanticGraph semanticGraph, IndexedWord root, Type type) {
        List<SemanticGraphEdge> outrcmod = semanticGraph.getOutEdgesSorted(root);
        SemanticGraphEdge rccop = DpUtils.findFirstOfRelation(outrcmod, EnglishGrammaticalRelations.COPULA);
        if (rccop != null) {
            Set<IndexedWord> excludercmod = DpUtils.exclude(semanticGraph, EXCLUDE_RELATIONS_COMPLEMENT, root);
            return new IndexedConstituent(semanticGraph, root, Collections.<IndexedWord> emptySet(), excludercmod, type);
        } else
            return new IndexedConstituent(semanticGraph, root, type);
    }

    /** Generates a clause from an apposition 
     * @param subject The subject of the clause (first argument of the appos relation)
     * @param object  The object of the clause (second argument of the appos relation)
     */
    private static void addApposClause(ClausIE clausIE, IndexedWord subject, IndexedWord object) {
        Clause clause = new Clause();
        clause.setSubject(0);
        clause.verb = 1;
        clause.complement = 2;
        Clause.Type clauseType = Clause.Type.SVC;
        Constituent.Type argumentConstType = Constituent.Type.COMPLEMENT;
        
        // Create a relation phrase with the possessive verb 'is' 
        Phrase apposPhrase = new Phrase();
        IndexedWord appositionVerb = new IndexedWord();
        appositionVerb.setWord("is");
        appositionVerb.setTag(POS_TAG.VBZ);
        appositionVerb.setNER(NE_TYPE.NO_NER);
        appositionVerb.setLemma("be");
        appositionVerb.setValue("be");
        appositionVerb.setIndex(-2);
        apposPhrase.addWordToList(appositionVerb);
        apposPhrase.setRoot(appositionVerb);
        
        PhraseConstituent verbConstit = new PhraseConstituent(apposPhrase, Constituent.Type.VERB);
        
        if (subject.ner().equals(NE_TYPE.DATE) || object.ner().equals(NE_TYPE.DATE))
            return;
        if (subject.ner().equals(NE_TYPE.TIME) || object.ner().equals(NE_TYPE.TIME))
            return;
        
        // If both the subject and the objects are LOCATION-ners, then clause type is SVA and the appos. verb "is in" 
        if ((subject.ner().equals(NE_TYPE.ORGANIZATION) || 
                subject.ner().equals(NE_TYPE.LOCATION)) && object.ner().equals(NE_TYPE.LOCATION)){
            clauseType = Clause.Type.SVA;
            
            // Create a relation phrase with the verb 'is' and the preposition 'in'
            IndexedWord prepWord = new IndexedWord();
            prepWord.setWord("in");
            prepWord.setTag(POS_TAG.IN);
            prepWord.setNER(NE_TYPE.NO_NER);
            prepWord.setLemma("in");
            prepWord.setValue("in");
            prepWord.setIndex(-2);
            apposPhrase.addWordToList(prepWord);
            
            verbConstit = new PhraseConstituent(apposPhrase, Constituent.Type.VERB);
            argumentConstType = Constituent.Type.ADVERBIAL;
            clause.adverbials.add(2);
            clause.complement = -1;
        }
        
        clause.constituents.add(new IndexedConstituent(clausIE.getSemanticGraph(), subject, Constituent.Type.SUBJECT));
        clause.constituents.add(verbConstit);
        clause.constituents.add(new IndexedConstituent(clausIE.getSemanticGraph(), object, argumentConstType));
        clause.setType(clauseType);
        clausIE.getClauses().add(clause);
    }

    /** Generates a clause from a possessive relation
    * @param subject The subject of the clause
    * @param object  The object of the clause 
    */
    private static void addPossessiveClause(ClausIE clausIE, IndexedWord subject, IndexedWord object) {
        Clause clause = new Clause();
        SemanticGraph newSemanticGraph = new SemanticGraph(clausIE.getSemanticGraph());
        clause.setSubject(0);
        clause.verb = 1;
        clause.dobjects.add(2);
        Set<IndexedWord> excludesub = new TreeSet<IndexedWord>();
        Set<IndexedWord> excludeobj = new TreeSet<IndexedWord>();

        excludeobj.add(subject);
        List<SemanticGraphEdge> outedobj = newSemanticGraph.getOutEdgesSorted(object);
        excludeVertexPoss(outedobj, excludeobj, clausIE);

        SemanticGraphEdge rcmod = null;
        if (subject.tag().charAt(0) == 'W') {
            IndexedWord root = newSemanticGraph.getParent(object); 
            if (root != null){
                if (root.tag().equals(POS_TAG.IN)){
                    root = newSemanticGraph.getParent(root); // "I saw the man in whose wife I trust"
                }
                List<SemanticGraphEdge> inedges = newSemanticGraph.getIncomingEdgesSorted(root);
                rcmod = DpUtils.findFirstOfRelation(inedges, EnglishGrammaticalRelations.RELATIVE_CLAUSE_MODIFIER);
            }
        } else {
            List<SemanticGraphEdge> outedges = newSemanticGraph.getOutEdgesSorted(subject);
            SemanticGraphEdge ps = DpUtils.findFirstOfRelation(outedges, EnglishGrammaticalRelations.POSSESSIVE_MODIFIER);
            if (ps != null){
                excludesub.add(ps.getDependent());
            }
        }

        if (rcmod != null) {
            clause.constituents.add(createRelConstituent(newSemanticGraph, rcmod.getGovernor(), Type.SUBJECT));
            // To avoid the s in  "Bill's clothes are great".
            ((IndexedConstituent) clause.constituents.get(0)).getExcludedVertexes().addAll(excludesub);
        } else {
            clause.constituents.add(new IndexedConstituent(newSemanticGraph, subject, Collections.<IndexedWord> emptySet(), 
                                                            excludesub, Type.SUBJECT));
        }
        
        // Create a relation phrase with the possessive verb 'has'
        Phrase possPhrase = new Phrase();
        IndexedWord possessiveVerb = new IndexedWord();
        possessiveVerb.setWord("has");
        possessiveVerb.setTag(POS_TAG.VBZ);
        possessiveVerb.setNER(NE_TYPE.NO_NER);
        possessiveVerb.setLemma("have");
        possessiveVerb.setValue("has");
        possessiveVerb.setIndex(-2);
        possPhrase.addWordToList(possessiveVerb);
        possPhrase.setRoot(possessiveVerb);
        
        clause.constituents.add(new PhraseConstituent(possPhrase, Constituent.Type.VERB));
        clause.constituents.add(new IndexedConstituent(newSemanticGraph, object, Collections.<IndexedWord> emptySet(), 
                                                        excludeobj, Constituent.Type.DOBJ));
        clause.setType(Clause.Type.SVO);
        clausIE.getClauses().add(clause);
    }

    /** Excludes vertexes for the object of a "possessive clause"
     * @param outedobj relations to be examined for exclusion
     * @param excludeobj The vertexes to be excluded
    */
    private static void excludeVertexPoss(List<SemanticGraphEdge> outedobj, Set<IndexedWord> excludeobj, ClausIE clausIE) {
        for (SemanticGraphEdge ed : outedobj) {
            if (DpUtils.isAdvcl(ed) || DpUtils.isAdvmod(ed)
                    || DpUtils.isAnyObj(ed) // currently everything is
                                            // excluded except prep and infmod
                    || DpUtils.isAnySubj(ed) || DpUtils.isAux(ed) || DpUtils.isCop(ed)
                    || DpUtils.isTmod(ed) || DpUtils.isAnyConj(ed) || DpUtils.isNeg(ed)
                    && clausIE.getOptions().processCcNonVerbs)
                excludeobj.add(ed.getDependent());
        }
    }

    /** Creates a clause from a partmod relation 
     * @param subject The subject of the clause
     * @param object  The object of the clause 
     * @param roots List of clause roots
     */
    private static void addPartmodClause(ClausIE clausIE, IndexedWord subject, IndexedWord verb, List<IndexedWord> roots) {
        IndexedWord partmodsub = subject;
        addNsubjClause(clausIE, roots, clausIE.getClauses(), partmodsub, verb, true);
    }

    /** Creates a clause from a parataxis relation 
     * @param root Head of the parataxis relation
     * @param parroot  Dependent of the parataxis relation
     * @param roots List of clause roots
     */
    private static void addParataxisClause(ClausIE clausIE, IndexedWord root, IndexedWord parroot, List<IndexedWord> roots) {
        Constituent verb = new IndexedConstituent(clausIE.getSemanticGraph(), parroot, Type.VERB);
        List<SemanticGraphEdge> outedges = clausIE.getSemanticGraph().getOutEdgesSorted(parroot);
        SemanticGraphEdge subject = DpUtils.findFirstOfRelationOrDescendent(outedges, EnglishGrammaticalRelations.SUBJECT);
        if (subject != null) {
            Constituent subjectConst = new IndexedConstituent(clausIE.getSemanticGraph(), subject.getDependent(), 
                                                                Type.SUBJECT);
            Constituent object = new IndexedConstituent(clausIE.getSemanticGraph(), root, Type.DOBJ);
            ((IndexedConstituent) object).excludedVertexes.add(parroot);
            Clause clause = new Clause();
            clause.setSubject(0);
            clause.verb = 1;
            clause.dobjects.add(2);
            clause.constituents.add(subjectConst);
            clause.constituents.add(verb);
            clause.constituents.add(object);
            clause.setType(Clause.Type.SVO);
            clausIE.getClauses().add(clause);
            roots.add(null);
        }
    }
}
