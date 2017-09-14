package de.uni_mannheim.minie.subconstituent;

import java.util.HashSet;
import java.util.Set;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.semgrex.SemgrexMatcher;
import edu.stanford.nlp.semgraph.semgrex.SemgrexPattern;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import de.uni_mannheim.constant.NE_TYPE;
import de.uni_mannheim.constant.SEPARATOR;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;
import de.uni_mannheim.utils.fastutils.FastUtil;

/**
 * @author Kiril Gashteovski
 */
public class SubConstituent {
    /** Sentence semantic graph **/
    private SemanticGraph sg;
    /** PhraseRoot: root of the phrase **/
    private IndexedWord phraseRoot;
    /**  Words in the phrase being examined **/
    private ObjectArrayList<IndexedWord> phraseWords;
    /** Set of chained candidates **/
    private ObjectOpenHashSet<ObjectArrayList<IndexedWord>> chainedCandidates;
    /** Subtree candidates **/
    private ObjectOpenHashSet<ObjectOpenHashSet<ObjectArrayList<IndexedWord>>> subTreeCandidates;
    /** Sibling candidates **/
    private ObjectOpenHashSet<String> siblingCandidates;
    /** Set of list of words (phrases), which make the sub-constituents **/
    private ObjectOpenHashSet<ObjectArrayList<IndexedWord>> subConstituents;
    /** Set of list of words (represented as strings), which make the sub-constituents **/
    private ObjectOpenHashSet<String> stSubconstituents;
    
    /** Default constructor (empty elements) **/
    public SubConstituent(){
        this.sg = new SemanticGraph();
        this.phraseRoot = new IndexedWord();
        this.phraseWords = new ObjectArrayList<>();
        this.chainedCandidates = new ObjectOpenHashSet<>();
        this.subTreeCandidates = new ObjectOpenHashSet<>();
        this.subConstituents = new ObjectOpenHashSet<>();
        this.stSubconstituents = new ObjectOpenHashSet<>();
        this.siblingCandidates = new ObjectOpenHashSet<>();
    }
    
    /** Parametric constructor **/
    public SubConstituent(SemanticGraph sentenceSg, IndexedWord pRoot, ObjectArrayList<IndexedWord> pWords){
        this.sg = sentenceSg;
        this.phraseRoot = pRoot;
        this.phraseWords = pWords;
        // The rest of the elements should be empty (there are functions for generating the candidate sub-constituents)
        this.chainedCandidates = new ObjectOpenHashSet<>();
        this.subTreeCandidates = new ObjectOpenHashSet<>();
        this.subConstituents = new ObjectOpenHashSet<>();
        this.stSubconstituents = new ObjectOpenHashSet<>();
        this.siblingCandidates = new ObjectOpenHashSet<>();
    }
    
    /** Initialize an object with semantic graph only. Everything else is empty. **/
    public SubConstituent(SemanticGraph sentenceSg){
        this.sg = sentenceSg;
        this.phraseRoot = new IndexedWord();
        this.phraseWords = new ObjectArrayList<>();
        this.chainedCandidates = new ObjectOpenHashSet<>();
        this.subTreeCandidates = new ObjectOpenHashSet<>();
        this.subConstituents = new ObjectOpenHashSet<>();
        this.stSubconstituents = new ObjectOpenHashSet<>();
        this.siblingCandidates = new ObjectOpenHashSet<>();
    }
    
    /**
     * Given a sentence semantic graph and a "phrase root", get a list of strings, representing phrases of all the 
     * sub-constituents. These are good only for the sub-constituents from the left (e.g. .* NN, everything on the left that
     * is modifying the noun). This method is not good for generating sub-constituents from the right (i.e. each word in 
     * "phraseWords" must be on the left of "phraseRoot" (except for the root itself)). Therefore, this is not good for 
     * generating sub-constituents where there are words on the right of the root (e.g. PPs).   
     * 
     * @return list of strings representing the subtrees of the phrase
     */
    public void generateSubConstituentsFromLeft(){
        // TODO: here the candidates with the siblings should be added (the ones on (d) in the white-board example)
        // Reusable variable
        ObjectList<IndexedWord> sublist;
        
        // Check whether the phrase root is NER or not and get the chained words
        ObjectArrayList<IndexedWord> chainedWords = this.getRootChainedWords();
        
        // Add the words one by one sequentially to the chained candidates set  
        for (int i = 0; i < chainedWords.size(); i++){
            sublist = chainedWords.subList(0, i + 1);
            this.chainedCandidates.add(CoreNLPUtils.objectListToObjectArrayList(sublist));
        }
        
        ObjectArrayList<IndexedWord> tempList = new ObjectArrayList<>();
        if (this.phraseRoot.index() > -1) {
            // Store the semgrex expression "{} < {idx:phraseRoot.index()}" in the string buffer
            String semGrex = CoreNLPUtils.getSemgrexDependentOf(this.phraseRoot);
        
            // SemGrex pattern and matcher and subtree edges  
            SemgrexPattern p = SemgrexPattern.compile(semGrex);
            SemgrexMatcher m = p.matcher(this.sg);
            Set<SemanticGraphEdge> subTreeEdges = new HashSet<>();
        
            // Match the semgrex pattern on the graph
            ObjectOpenHashSet<ObjectArrayList<IndexedWord>> tempSubTreeCand = new ObjectOpenHashSet<>();
            while (m.find()) {
                tempSubTreeCand.clear();
                // Get the subtree of the match, transform it into a list of sorted (by index) words
                subTreeEdges = CoreNLPUtils.getSubTreeEdges(m.getMatch(), this.sg, null);     
            
                // If the sub-tree is empty, include the parent of the match as well
                if (subTreeEdges.size() == 0){ 
                    if (chainedWords.contains(m.getMatch())){
                        subTreeEdges.add(this.sg.getEdge(this.sg.getParent(m.getMatch()), m.getMatch()));
                    }
                }
            
                if (subTreeEdges.size() == 0)
                    tempList.add(m.getMatch());
                else 
                    tempList = CoreNLPUtils.getSortedWordsFromListOfEdges(subTreeEdges);
            
                tempList.retainAll(this.phraseWords);
            
                // Create chained candidates if the subtree is part of the chained words
                if (chainedWords.containsAll(tempList)){
                    for (int i = 0; i < tempList.size(); i++){
                        sublist = tempList.clone().subList(i, tempList.size());
                        this.chainedCandidates.add(CoreNLPUtils.getSortedListOfWords(
                                CoreNLPUtils.objectListToObjectArrayList(sublist)));
                    }
                } 
                // Else, create the other subtree candidates
                else {
                    for (int i = 0; i < tempList.size(); i++){
                        sublist = tempList.subList(i, tempList.size());
                        tempSubTreeCand.add(CoreNLPUtils.getSortedListOfWords(
                                CoreNLPUtils.objectListToObjectArrayList(sublist)));
                    }
                }
                if (!tempSubTreeCand.isEmpty())
                    this.subTreeCandidates.add(tempSubTreeCand.clone());
                tempList.clear();
            }
        }
        
        // Generate the sibling candidates
        // TODO: make this a list of IndexedWord's, not list of strings
        ObjectArrayList<String> tempSiblingList = new ObjectArrayList<>();
        ObjectArrayList<ObjectArrayList<String>> siblingsLists = new ObjectArrayList<>();
        for (ObjectOpenHashSet<ObjectArrayList<IndexedWord>> set: this.subTreeCandidates){
            for (ObjectArrayList<IndexedWord> list: set){
                tempSiblingList.add(CoreNLPUtils.listOfWordsToLemmaString(list));
            }
            siblingsLists.add(tempSiblingList.clone());            
            tempSiblingList.clear();
        }
        ObjectArrayList<IntArrayList> combinationsInd = new ObjectArrayList<>();
        ObjectOpenHashSet<ObjectArrayList<String>> siblingCombinations;
        // Don't generate subconstituents with way too many siblings (higly likely it's not a real sentence, it will just 
        // mess up the memory)
        if (siblingsLists.size() <= 5)
            combinationsInd = FastUtil.getListsCombinationIndices(siblingsLists);
        siblingCombinations = FastUtil.getListsElementsCombinationSet(combinationsInd, siblingsLists);
        for (ObjectArrayList<String> stList: siblingCombinations){
            this.siblingCandidates.add(FastUtil.listOfStringsToString(stList, SEPARATOR.SPACE));
        }
        
        // Generate all possible sub-constituents
        for (ObjectArrayList<IndexedWord> chainedCand: this.chainedCandidates){
            for (ObjectOpenHashSet<ObjectArrayList<IndexedWord>> subTreeSet: this.subTreeCandidates){
                // TODO: see how sorting of the words will change things
                for (ObjectArrayList<IndexedWord> subtreeCand: subTreeSet){
                    tempList.clear();
                    tempList.addAll(subtreeCand);
                    tempList.addAll(chainedCand);
                    this.subConstituents.add(
                        CoreNLPUtils.getSortedListOfWords(
                                CoreNLPUtils.objectListToObjectArrayList(tempList.clone())));
                }
            }
            this.subConstituents.add(CoreNLPUtils.getSortedListOfWords(
                                        CoreNLPUtils.objectListToObjectArrayList(chainedCand.clone())));
        }
        
        // Add all the candidates to a set of strings where the strings are lemmas of the word sequences
        for (ObjectArrayList<IndexedWord> cand: this.subConstituents){
            this.stSubconstituents.add(CoreNLPUtils.listOfWordsToLemmaString(cand));
        }
        // Add siblings combinations + the chained words as sub-constituents
        // TODO: this should be done previously, just a dirty fix for now
        for (ObjectArrayList<IndexedWord> chainedCand: this.chainedCandidates){
            for (String sib: this.siblingCandidates){
                this.stSubconstituents.add(sib + SEPARATOR.SPACE + CoreNLPUtils.listOfWordsToLemmaString(chainedCand));
            }
        }
    }
    
    /**
     * Given a phrase and its root, find the chained words to the root
     * @return chained words to the root
     */
    public ObjectArrayList<IndexedWord> getRootChainedWords(){
        // TODO: double check how we generate chained words (considering the NERs)
        ObjectArrayList<IndexedWord> chainedWords = new ObjectArrayList<>();
        if (!this.phraseRoot.ner().equals(NE_TYPE.NO_NER)) 
            chainedWords = CoreNLPUtils.getChainedNERs(this.phraseWords, this.phraseWords.indexOf(this.phraseRoot));
        else if (CoreNLPUtils.isNoun(this.phraseRoot.tag()))
            chainedWords = CoreNLPUtils.getChainedNouns(this.phraseWords, this.phraseWords.indexOf(this.phraseRoot));
        else chainedWords = CoreNLPUtils.getChainedTagNoNER(this.phraseWords, this.phraseWords.indexOf(this.phraseRoot));
        return chainedWords;
    }
    
    // Getters
    public IndexedWord getRoot(){
        return this.phraseRoot;
    }
    public SemanticGraph getSentenceSemGraph(){
        return this.sg;
    }
    public ObjectArrayList<IndexedWord> getWords(){
        return this.phraseWords;
    }
    public ObjectOpenHashSet<ObjectArrayList<IndexedWord>> getChainedCandidates(){
        return this.chainedCandidates;
    }
    public ObjectOpenHashSet<ObjectOpenHashSet<ObjectArrayList<IndexedWord>>> getSubTreeCandidates(){
        return this.subTreeCandidates;
    }
    public ObjectOpenHashSet<ObjectArrayList<IndexedWord>> getSubConstituents(){
        return this.subConstituents;
    }
    public ObjectOpenHashSet<String> getStringSubConstituents(){
        return this.stSubconstituents;
    }
    
    // Setters
    public void setRoot(IndexedWord pRoot){
        this.phraseRoot = pRoot;
    }
    public void setWords(ObjectArrayList<IndexedWord> pWords){
        this.phraseWords = pWords;
    }
    public void setSentenceSemGraph(SemanticGraph sentenceSemGraph){
        this.sg = sentenceSemGraph;
    }
    public void setChainedCandidates(ObjectOpenHashSet<ObjectArrayList<IndexedWord>> cCand){
        this.chainedCandidates = cCand;
    }
    public void setCopiedChainedCandidates(ObjectOpenHashSet<ObjectArrayList<IndexedWord>> cCand){
        this.chainedCandidates = cCand.clone();
    }
    public void setSubTreeCandidates(ObjectOpenHashSet<ObjectOpenHashSet<ObjectArrayList<IndexedWord>>> sCand){
        this.subTreeCandidates = sCand;
    }
    public void setCopiedSubTreeCandidates(ObjectOpenHashSet<ObjectOpenHashSet<ObjectArrayList<IndexedWord>>> sCand){
        this.subTreeCandidates = sCand.clone();
    }
    public void setSubConstituents(ObjectOpenHashSet<ObjectArrayList<IndexedWord>> subConst){
        this.subConstituents = subConst;
    }
    public void setCopiedSubConstituents(ObjectOpenHashSet<ObjectArrayList<IndexedWord>> subConst){
        this.subConstituents = subConst.clone();
    }
    public void setStringSubConstituents(ObjectOpenHashSet<String> stSubConst){
        this.stSubconstituents = stSubConst;
    }
    public void setCopiedStringSubConstituents(ObjectOpenHashSet<String> stSubConst){
        this.stSubconstituents = stSubConst.clone();
    }
    
    /** 
     *  Clear the object (clear the lists, create empty indexed word for the root word and semantic graph for the 
     *  sentence semantic graph.
     **/
    public void clear(){
        this.chainedCandidates.clear();
        this.phraseRoot = new IndexedWord();
        this.phraseWords.clear();
        this.sg = new SemanticGraph();
        this.stSubconstituents.clear();
        this.subConstituents.clear();
        this.subTreeCandidates.clear();
    }
    
    /**
     * Clear the subconstituents' lists and the candidates' lists
     */
    public void clearSubConstituentsAndCandidates(){
        this.stSubconstituents.clear();
        this.subConstituents.clear();
        this.subTreeCandidates.clear();
        this.chainedCandidates.clear();
    }
}
