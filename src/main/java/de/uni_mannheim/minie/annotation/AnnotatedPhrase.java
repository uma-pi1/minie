package de.uni_mannheim.minie.annotation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.constant.CHARACTER;
import de.uni_mannheim.constant.NE_TYPE;
import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.constant.REGEX;
import de.uni_mannheim.constant.SEPARATOR;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.util.CoreMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

/**
 * The annotated phrase is a phrase that holds some sort of annotations. For now, the only annotation
 * that a phrase has are the quantities. Each phrase has a list of quantities.
 *
 * @author Kiril Gashteovski
 *
 */
public class AnnotatedPhrase extends Phrase {
    /** The list of quantities for the phrase **/
    private ObjectArrayList<Quantity> quantities;
    /** The list of dropped edges **/
    private ObjectOpenHashSet<SemanticGraphEdge> droppedEdges;
    /** List of dropped words **/
    private ObjectOpenHashSet<IndexedWord> droppedWords;
    
    /** Default constructor **/
    public AnnotatedPhrase(){
        super();
        this.quantities = new ObjectArrayList<>();
        this.droppedEdges = new ObjectOpenHashSet<>();
        this.droppedWords = new ObjectOpenHashSet<>();
    }
    
    /**
     * Parametric constructor 
     * @param p: the phrase to be annotated
     * @param q: the quantities for phrase 'p'
     */
    public AnnotatedPhrase(Phrase p, ObjectArrayList<Quantity> q){
        super(p);
        this.quantities = q;
        this.droppedEdges = new ObjectOpenHashSet<>();
        this.droppedWords = new ObjectOpenHashSet<>();
    }
    
    /**
     * Parametric constructor: given a list of indexed words, create annotated phrase with empty quantities list
     * @param wList: list of indexed words for the phrase
     */
    public AnnotatedPhrase(ObjectArrayList<IndexedWord> wList) {
        super(wList);
        this.quantities = new ObjectArrayList<>();
        this.droppedEdges = new ObjectOpenHashSet<>();
        this.droppedWords = new ObjectOpenHashSet<>();
    }
    
    /**
     * Parametric constructor: given a list of indexed words and the root of the phrase, create annotated phrase with 
     * empty quantities list.
     * @param wList: list of words for the phrase
     * @param root: the root of the phrase
     */
    public AnnotatedPhrase(ObjectArrayList<IndexedWord> wList, IndexedWord root) {
        super(wList, root);
        this.quantities = new ObjectArrayList<>();
        this.droppedEdges = new ObjectOpenHashSet<>();
        this.droppedWords = new ObjectOpenHashSet<>();
    }
    
    /**
     * Parametric constructor: given a list of indexed words and semantic graph, create annotated phrase, with empty
     * quantities list
     * @param wList: the list of words for the phrase
     * @param sg: the semantic graph of the phrase (should be the sentence subgraph)
     */
    public AnnotatedPhrase(ObjectArrayList<IndexedWord> wList, SemanticGraph sg){
        super(wList, sg);
        this.quantities = new ObjectArrayList<>();
        this.droppedEdges = new ObjectOpenHashSet<>();
        this.droppedWords = new ObjectOpenHashSet<>();
    }
    
    /**
     * Parametric constructor: given a list of indexed words, semantic graph, and a root word, create annotated phrase, with 
     * empty quantities list
     * @param wList: the list of words for the phrase
     * @param sg: the semantic graph of the phrase (should be the sentence subgraph)
     * @param root: the root of the phrase
     */
    public AnnotatedPhrase(ObjectArrayList<IndexedWord> wList, SemanticGraph sg, IndexedWord root){
        super(wList, sg, root);
        this.quantities = new ObjectArrayList<>();
        this.droppedEdges = new ObjectOpenHashSet<>();
        this.droppedWords = new ObjectOpenHashSet<>();
    }
    
    /**
     * Parametric constructor: given a phrase as a parameter, set it as a 'phrase', and make an empty quantities list
     * @param p: the phrase to be initialized
     */
    public AnnotatedPhrase(Phrase p){
        super(p);
        this.quantities = new ObjectArrayList<>();
        this.droppedEdges = new ObjectOpenHashSet<>();
        this.droppedWords = new ObjectOpenHashSet<>();
    }
    
    /**
     * Copy constructor
     * @param ap: object to be copied
     */
    public AnnotatedPhrase(AnnotatedPhrase ap){
        super(ap.getWordList());
        this.quantities = ap.getQuantities();
        this.droppedEdges = ap.getDroppedEdges();
        this.droppedWords = ap.getDroppedWords();
    }
   
    /** Get the quantities **/
    public ObjectArrayList<Quantity> getQuantities(){
        return this.quantities;
    }
    
    /** Set the quantities **/
    public void setQuantities(ObjectArrayList<Quantity> q){
        this.quantities = q;
    }
    
    /**
     * Add quantity to the list of quantities 
     * @param q: the quantity to be added
     */
    public void addQuantity(Quantity q){
        this.quantities.add(q);
    }
    
    /**
     * Remove quantity from the list 
     * @param i: the index of the quantity in the list to be removed
     */
    public void removeQuantity(int i){
        this.quantities.remove(i);
    }
    
    /**
     * Remove quantity from the list
     * @param q: quantity to be removed
     */
    public void removeQuantity(Quantity q){
        this.quantities.remove(q);
    }
    
    /**
     * Detect the quantities in a phrase (given the sentence semantic graph).
     * @param sentSemGraph: the sentence semantic graph
     */
    public void detectQuantities(SemanticGraph sentSemGraph, int i){
        // Quantity words and edges
        ObjectArrayList<IndexedWord> qWords;
        ObjectArrayList<SemanticGraphEdge> qEdges = new ObjectArrayList<>();
        
        // Tokens regex patterns
        String tokenRegexPattern;
        if (i == 1)
            tokenRegexPattern = REGEX.QUANTITY_SEQUENCE;
        else
            tokenRegexPattern = REGEX.QUANTITY_SEQUENCE_WITH_NO;
        
        TokenSequencePattern tPattern = TokenSequencePattern.compile(tokenRegexPattern);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(this.getWordCoreLabelList());
        
        // Some reusable variables
        List<CoreMap> matchCoreMaps;
        ObjectOpenHashSet<IndexedWord> wordsSet = new ObjectOpenHashSet<>();
        IndexedWord head;
        Set<SemanticGraphEdge> subtreeedges = new HashSet<>();
        int matchCounter = -1;
        
        // Annotate the matches and their subtrees
        while (tMatcher.find()){      
            matchCounter++;
            matchCoreMaps = tMatcher.groupNodes();
            
            // Get the head word of the phrase and see whether or not to add it to the quantities
            head = CoreNLPUtils.getRootFromCoreMapWordList(sentSemGraph, matchCoreMaps);
            if (head.ner().equals(NE_TYPE.DATE) || head.ner().equals(NE_TYPE.LOCATION) ||
                    head.ner().equals(NE_TYPE.MISC) || head.ner().equals(NE_TYPE.ORGANIZATION) || 
                    head.ner().equals(NE_TYPE.PERSON) || head.ner().equals(NE_TYPE.TIME))
                continue;
            
            // Add the sutree elements of the head word if the right relations are in force
            for (IndexedWord w: sentSemGraph.getChildren(head)){
                if ((sentSemGraph.reln(head, w) == EnglishGrammaticalRelations.QUANTIFIER_MODIFIER) ||
                    (sentSemGraph.reln(head, w) == EnglishGrammaticalRelations.ADVERBIAL_MODIFIER)){
                    wordsSet.add(w);
                    subtreeedges = CoreNLPUtils.getSubTreeEdges(w, sentSemGraph, null);
                }
            }
            
            // Add the quantity words found and annotate them within the phrase
            wordsSet.addAll(CoreNLPUtils.getWordSetFromCoreMapList(matchCoreMaps));
            wordsSet.addAll(CoreNLPUtils.getSortedWordsFromListOfEdges(subtreeedges));
            wordsSet.retainAll(this.getWordList());
            qWords = CoreNLPUtils.getSortedWordsFromSetOfWords(wordsSet);
            if (qWords.isEmpty())
                continue;
            this.setQuantitiesFromWordList(qWords.clone(), qEdges, sentSemGraph, i, matchCounter);
            
            // Reset
            qWords.clear();
            wordsSet.clear();
        }
    }
    
    /**
     * Given a quantity ID, return the quantity with that particular ID. If there is no quantity with that ID, 
     * return null.
     * @param qId: quantity ID
     */
    public Quantity getQuantityByID(String qId){
        for (int i = 0; i < this.quantities.size(); i++){
            if (this.quantities.get(i).getId().equals(qId)){
                return this.quantities.get(i);
            }
        }
        return null;
    }
    
    /**
     * Given a quantity ID, remove it from the list of quantities in the annotated phrase. If there is no quantity 
     * by that ID, nothing happens.
     * @param qId: quantity ID.
     */
    public void removeQuantityByID(String qId){
        int remInd = -1;
        for (int i = 0; i < this.quantities.size(); i++){
            if (this.quantities.get(i).getId().equals(qId)){
                remInd = i;
                break;
            }
        }
        if (remInd > -1){
            this.quantities.remove(remInd);
        }
    }
    
    /**
     * A helper function used in detectQuantities. When we have a list of quantity words, quantity edges and the 
     * sentence semantic graph, add quantities to the list of quantities and clear the reusable lists. 
     *  If there are quantities in the phrase, replace them with the word SOME_n_i, where i = the place of the quantity
     * (0 - subject, 1 - relation, 2 - object) and j = # of quantity within the phrase.
     * 
     * @param qWords: list of quantity indexed words
     * @param qEdges: list of semantic graph edges (reusable)
     * @param sentSemGraph: sentence semantic graph
     * @param i: used for ID-ying purposes of the quantities' annotations 
     * @param j: used for ID-ying purposes of the quantities' annotations 
     */
    private void setQuantitiesFromWordList(ObjectArrayList<IndexedWord> qWords, ObjectArrayList<SemanticGraphEdge> qEdges, 
                                            SemanticGraph sentSemGraph, int i, int j){
        // Quantity ID
        StringBuilder sbId = new StringBuilder();
        if (i == 0)
            sbId.append(Quantity.SUBJECT_ID);
        else if (i == 1)
            sbId.append(Quantity.RELATION_ID);
        else
            sbId.append(Quantity.OBJECT_ID);
        sbId.append(CHARACTER.UNDERSCORE);
        sbId.append(j + 1); // Indexing starts from 1
        
        for (IndexedWord w: qWords){
            qEdges.add(sentSemGraph.getEdge(sentSemGraph.getParent(w), w));
        }
        
        // Add the quantity to the list
        this.quantities.add(new Quantity(qWords, qEdges, sbId.toString()));
        
        // Clear the lists
        qWords.clear();
        qEdges.clear();
    }
    
    /**
     * If there are quantities in the phrase, replace them with the word QUANT_n_i, where i = the place of the quantity
     * (0 - subject, 1 - relation, 2 - object) and j = # of quantity within the phrase.
     * TODO: optimize this! 
     * 
     * @param j used for ID-ying purposes of the quantities' annotations 
     */
    public void annotateQuantities(int j){
        IndexedWord w;
        StringBuilder sbID = new StringBuilder();
        if (this.quantities.size() > 0){
            // Replacing word
            for (int i = 0; i < this.quantities.size(); i++){
                w = new IndexedWord(this.quantities.get(i).getQuantityWords().get(0));
                
                // The word should be QUANT_i_j, where i = the place (0 - subj, 1 - rel, 2 - obj) and 
                // j = # of quantity within the phrase
                sbID.append(Quantity.ST_QUANT);
                sbID.append(CHARACTER.UNDERSCORE);
                sbID.append(this.quantities.get(i).getId());
                
                w.setWord(sbID.toString());
                w.setLemma(sbID.toString());
                w.setTag(Quantity.ST_QUANTITY);
                w.setNER(Quantity.ST_QUANTITY);
                
                this.replaceWordsWithOneWord(this.quantities.get(i).getQuantityWords().clone(), w);
                sbID.setLength(0);
            }
        }
        
        // Merge the adjacent quantities
        this.mergeAdjacentQuantities();
    }

    public boolean isOneNER() {
        return CoreNLPUtils.isOneNER(this.wordList);
    }
    
    /**
     * When there are already annotated quantities, merge the ones which are right next to each other in a sequence.
     */
    public void mergeAdjacentQuantities(){
        // Reusable variables
        ObjectArrayList<IndexedWord> mergedQuantityWords = new ObjectArrayList<>();
        ObjectArrayList<SemanticGraphEdge> mergedEdges = new ObjectArrayList<>();
        ObjectArrayList<String> qIds = new ObjectArrayList<>();
        ObjectOpenHashSet<IndexedWord> remWords = new ObjectOpenHashSet<>();
        ObjectArrayList<IndexedWord> matches;
        
        // Token regex pattern and matcher
        TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.ADJACENT_QUANTITIES);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(this.getWordCoreLabelList());
        
        // Merge the quantities when matched
        while (tMatcher.find()){
            // Get the merged words and edges from the quantities that should be merged.
            matches = CoreNLPUtils.getWordListFromCoreMapList(tMatcher.groupNodes());
            
            for (int i = 0; i < matches.size(); i++){
                // If it has preposition bridging two quantities, add it to the mergedQuantityWords list
                if (matches.get(i).tag().equals(POS_TAG.IN)) {
                    mergedQuantityWords.add(matches.get(1));
                    remWords.add(matches.get(1));
                }
                
                // Merge the adjacent quantities
                for (Quantity q: this.getQuantities()){
                    if ((Quantity.ST_QUANT + CHARACTER.UNDERSCORE + q.getId()).equals(matches.get(i).word())){
                        qIds.add(q.getId());
                        mergedQuantityWords.addAll(q.getQuantityWords());
                        mergedEdges.addAll(q.getQuantityEdges());
                    }
                }
            }
            
            // Add all the words and edges from the merged quantities to the first one and remove the rest
            for (int i = 0; i < this.getWordList().size(); i++){
                if (this.getWordList().get(i).word().equals(Quantity.ST_QUANT + CHARACTER.UNDERSCORE + qIds.get(0))){
                    if (this.getQuantityByID(qIds.get(0)) != null){
                        this.getQuantityByID(qIds.get(0)).setWords(mergedQuantityWords);
                        this.getQuantityByID(qIds.get(0)).setEdges(mergedEdges);
                        for (int j = 1; j < qIds.size(); j++){
                            this.removeQuantityByID(qIds.get(j));
                            for (int k = i; k < this.getWordList().size(); k++){
                                if (this.getWordList().get(k).word().equals(Quantity.ST_QUANT + CHARACTER.UNDERSCORE + 
                                                                            qIds.get(j))){
                                    remWords.add(this.getWordList().get(k));
                                    continue;
                                }
                            }
                        }
                        break;
                    }
                }
            }
            
            // Remove and clear 
            this.removeWordsFromList(remWords);
            remWords.clear();
            qIds.clear();
        }
    }
    
    @Override
    public void clear() {
        this.phraseGraph = new SemanticGraph();
        this.root = new IndexedWord();
        this.tds.clear();
        this.wordList.clear();
        this.quantities.clear();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (IndexedWord w: this.wordList) {
            sb.append(w.word());
            sb.append(SEPARATOR.SPACE);
        }
        return sb.toString().trim();
    }

    public void addDroppedEdges(ObjectArrayList<SemanticGraphEdge> edges) {
        // check for duplicates
        for (SemanticGraphEdge edge: edges) {
            if (edge==null) continue;
            boolean edgeAlreadyListed = false;
            for (SemanticGraphEdge listedEdge: this.droppedEdges) {
                if (edge.getTarget().index() == listedEdge.getTarget().index()) {
                    edgeAlreadyListed = true;
                    break;
                }
            }
            if (!edgeAlreadyListed)  {
                this.droppedEdges.add(edge);
            }
        }
    }
    
    /**
     * Given a list of indexed words (the ones that need to be dropped), add all of them to the 
     * set of dropped words.
     * @param droppedWords: list of words (IndexedWord objects) that need to be dropped from the phrase
     */
    public void addDroppedWords(ObjectArrayList<IndexedWord> droppedWords) {
        for (IndexedWord w: droppedWords) {
            this.droppedWords.add(w);
        }
    }
    public void addDroppedWords(ObjectOpenHashSet<IndexedWord> droppedWords) {
        for (IndexedWord w: droppedWords) {
            this.droppedWords.add(w);
        }
    }

    public ObjectOpenHashSet<SemanticGraphEdge> getDroppedEdges() {
        return this.droppedEdges;
    }
    public ObjectOpenHashSet<IndexedWord> getDroppedWords() {
        return this.droppedWords;
    }
}
