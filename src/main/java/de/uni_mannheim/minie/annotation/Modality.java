package de.uni_mannheim.minie.annotation;

import java.io.IOException;
import java.util.List;

import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.constant.CHARACTER;
import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.constant.REGEX;
import de.uni_mannheim.constant.SEPARATOR;
import de.uni_mannheim.utils.Dictionary;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

/**
 * Annotation for modality
 *
 * @author Kiril Gashteovski
 */
public class Modality {
    /** Annotations for modality, can be just "CERTAINTY" or "POSSIBILITY" */
    public enum Type {CERTAINTY, POSSIBILITY}
    
    /** Strings expressing modality types **/
    public static String ST_CERTAINTY = "CERTAINTY";
    public static String ST_CT = "CT";
    public static String ST_POSSIBILITY = "POSSIBILITY";
    public static String ST_PS = "PS";
    
    /** List of possibility words and edges (if found any) **/
    private ObjectOpenHashSet<IndexedWord> possibilityWords; 
    private ObjectOpenHashSet<SemanticGraphEdge> possibilityEdges;
    
    /** List of certainty words and edges (if found any) **/
    private ObjectOpenHashSet<IndexedWord> certaintyWords;
    private ObjectOpenHashSet<SemanticGraphEdge> certaintyEdges;
    
    /** Modality type **/
    private Modality.Type modalityType;
    
    /** A set of all words expressing possibility **/
    public static Dictionary POSS_WORDS;
    static {
        try {
            POSS_WORDS = new Dictionary("/minie-resources/poss-words.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    
    /** A set of negative possibility words **/
    public static Dictionary NEG_POSS_WORDS;
    static {
        try {
            NEG_POSS_WORDS = new Dictionary("/minie-resources/poss-neg-words.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** A set of adjectives expressing possibility **/
    public static Dictionary POSS_ADJ;
    static {
        try {
            POSS_ADJ = new Dictionary("/minie-resources/poss-adj.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** A set of certainty adverbs **/
    public static Dictionary CERTAINTY_WORDS;
    static {
        try {
            CERTAINTY_WORDS = new Dictionary("/minie-resources/certainty-words.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** A set of adverbs expressing possibility **/
    public static Dictionary POSS_ADVERBS;
    static {
        try {
            POSS_ADVERBS = new Dictionary("/minie-resources/poss-adverbs.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** A set of possibility modals **/ 
    public static Dictionary MODAL_POSSIBILITY;
    static {
        try {
            MODAL_POSSIBILITY = new Dictionary("/minie-resources/poss-modal.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** A set of certainty verbs **/
    public static Dictionary VERB_CERTAINTY;
    static {
        try {
            VERB_CERTAINTY = new Dictionary("/minie-resources/certainty-verbs.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** A set of possibility verbs **/
    public static Dictionary VERB_POSSIBILITY;
    static {
        try {
            VERB_POSSIBILITY = new Dictionary("/minie-resources/poss-verbs.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** Default constructor. Assumes 'certainty' modality type, creates empty lists of poss/cert words and edges **/
    public Modality(){
        this.modalityType = Modality.Type.CERTAINTY;
        this.possibilityEdges = new ObjectOpenHashSet<SemanticGraphEdge>();
        this.possibilityWords = new ObjectOpenHashSet<IndexedWord>();
        this.certaintyWords = new ObjectOpenHashSet<IndexedWord>();
        this.certaintyEdges = new ObjectOpenHashSet<SemanticGraphEdge>();
    }
    /**
     * Copy constructor
     * @param m: modality object
     */
    public Modality(Modality m){
        this.modalityType = m.getModalityType();
        this.possibilityEdges = m.getPossibilityEdges();
        this.possibilityWords = m.getPossibilityWords();
        this.certaintyEdges = m.getCertaintyEdges();
        this.certaintyWords = m.getCertaintyWords();
    }
    /**
     * Given the modality type, the list of edges and words are empty lists 
     * @param t: Modality type
     */
    public Modality(Modality.Type t){
        this.modalityType = t;
        this.possibilityEdges = new ObjectOpenHashSet<SemanticGraphEdge>();
        this.possibilityWords = new ObjectOpenHashSet<IndexedWord>();
        this.certaintyWords = new ObjectOpenHashSet<IndexedWord>();
        this.certaintyEdges = new ObjectOpenHashSet<SemanticGraphEdge>();
    }
    /**
     * Constructor with given the modality type, list of possibility words and possibility edges. The certainty 
     * lists of words and edges are empty. 
     * @param t: modality type
     * @param possWords: possibility words
     * @param possEdges: possibility edges
     */
    public Modality(Modality.Type t, ObjectOpenHashSet<IndexedWord> possWords, 
            ObjectOpenHashSet<SemanticGraphEdge> possEdges){
        this.modalityType = t;
        this.possibilityWords = possWords;
        this.possibilityEdges = possEdges;
        this.certaintyWords = new ObjectOpenHashSet<IndexedWord>();
        this.certaintyEdges = new ObjectOpenHashSet<SemanticGraphEdge>();
    }
    
    /** Getters **/
    public Modality.Type getModalityType(){
        return this.modalityType;
    }
    public ObjectOpenHashSet<IndexedWord> getCertaintyWords(){
        return this.certaintyWords;
    }
    public ObjectOpenHashSet<IndexedWord> getPossibilityWords(){
        return this.possibilityWords;
    }
    public ObjectOpenHashSet<SemanticGraphEdge> getCertaintyEdges(){
        return this.certaintyEdges;
    }
    public ObjectOpenHashSet<SemanticGraphEdge> getPossibilityEdges(){
        return this.possibilityEdges;
    }
    
    /** Setters **/
    public void setModalityType(Modality.Type t){
        this.modalityType = t;
    }
    public void setCertaintyWords(ObjectOpenHashSet<IndexedWord> certWords){
        this.certaintyWords = certWords;
    }
    public void setPossibilityWords(ObjectOpenHashSet<IndexedWord> possWords){
        this.possibilityWords = possWords;
    }
    public void setCertaintyEdges(ObjectOpenHashSet<SemanticGraphEdge> certEdges){
        this.certaintyEdges = certEdges;
    }
    public void setPossibilityEdges(ObjectOpenHashSet<SemanticGraphEdge> possEdges){
        this.possibilityEdges = possEdges;
    }
    
    /** Adding elements to lists **/
    public void addCertaintyEdge(SemanticGraphEdge e){
        this.certaintyEdges.add(e);
    }
    public void addCertaintyEdges(ObjectArrayList<SemanticGraphEdge> edges){
        this.certaintyEdges.addAll(edges);
    }
    public void addPossibilityEdge(SemanticGraphEdge e){
        this.possibilityEdges.add(e);
    }
    public void addPossibilityEdges(ObjectArrayList<SemanticGraphEdge> edges){
        this.possibilityEdges.addAll(edges);
    }
    public void addCertaintyWord(IndexedWord w){
        this.certaintyWords.add(w);
    }
    public void addCertaintyWords(ObjectArrayList<IndexedWord> words) {
        this.certaintyWords.addAll(words);
    }
    public void addPossibilityWord(IndexedWord w){
        this.possibilityWords.add(w);
    }
    public void addPossibilityWords(ObjectArrayList<IndexedWord> words){
        this.possibilityWords.addAll(words);
    }
    
    /**
     * Given a phrase and a sentence semantic graph, get the modality for the phrase
     * @param relation: a phrase (relation)
     * @param sentenceSemGraph: the semantic graph of the whole sentence
     * @return Modality of the phrase
     */
    public static Modality getModality(Phrase relation, SemanticGraph sentenceSemGraph){
        Modality mod = new Modality();
        ObjectArrayList<SemanticGraphEdge> possibilityEdges = new ObjectArrayList<>();
        ObjectArrayList<SemanticGraphEdge> certaintyEdges = new ObjectArrayList<>();
        ObjectArrayList<IndexedWord> certaintyWords = new ObjectArrayList<>();
        ObjectArrayList<IndexedWord> possibilityWords = new ObjectArrayList<>();
        
        // Add words/edges to the 
        for (int i = 0; i < relation.getWordList().size(); i++){
            if (CoreNLPUtils.isAdverb(relation.getWordList().get(i).tag())){
                // Check for possibility adverbs
                if (Modality.POSS_ADVERBS.contains(relation.getWordList().get(i).lemma())){
                    possibilityWords.add(relation.getWordList().get(i));
                    possibilityEdges.add(sentenceSemGraph.getEdge(
                                                        sentenceSemGraph.getParent(relation.getWordList().get(i)), 
                                                        relation.getWordList().get(i)
                                                    ));
                }
                // Check for certainty adverbs
                else if (Modality.CERTAINTY_WORDS.contains(relation.getWordList().get(i).lemma())){
                    certaintyWords.add(relation.getWordList().get(i));
                    certaintyEdges.add(sentenceSemGraph.getEdge(
                                sentenceSemGraph.getParent(relation.getWordList().get(i)), 
                                relation.getWordList().get(i)
                            ));
                }
            }
            // Check for possibility adjectives
            else if (CoreNLPUtils.isAdj(relation.getWordList().get(i).tag())){
                if (Modality.POSS_ADJ.contains(relation.getWordList().get(i).lemma())){
                    possibilityWords.add(relation.getWordList().get(i));
                    possibilityEdges.add(sentenceSemGraph.getEdge(
                            sentenceSemGraph.getParent(relation.getWordList().get(i)), 
                            relation.getWordList().get(i)
                        ));
                }
            }
            // Check for modals (possibility and certainty)
            else if (relation.getWordList().get(i).tag().equals(POS_TAG.MD)){
                if (Modality.MODAL_POSSIBILITY.contains(relation.getWordList().get(i).lemma())){
                    possibilityWords.add(relation.getWordList().get(i));
                    possibilityEdges.add(sentenceSemGraph.getEdge(
                            sentenceSemGraph.getParent(relation.getWordList().get(i)), 
                            relation.getWordList().get(i)
                        ));
                }
            }
        }
        
        // Check for modality verb phrases
        TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.T_POSS_VP);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(relation.getWordCoreLabelList());
        
        while (tMatcher.find()){         
            List<CoreMap> match = tMatcher.groupNodes();
            IndexedWord firstWord = new IndexedWord(new CoreLabel(match.get(0)));
            if (firstWord.index() != relation.getWordList().get(0).index())
                break;
            IndexedWord w = new IndexedWord();
            for (int i = 0; i < match.size(); i++) {
                w = new IndexedWord(new CoreLabel(match.get(i)));
                possibilityWords.add(w);
                if (w.index() > 0)
                    possibilityEdges.add(sentenceSemGraph.getEdge(sentenceSemGraph.getParent(w), w));
                if (w.tag().equals(POS_TAG.TO))
                    break;
            }
        }
        
        // If there are both possibility and certainty, certainty get subsummed by possibility
        if (!certaintyWords.isEmpty() && !possibilityWords.isEmpty()){
            mod.setModalityType(Modality.Type.POSSIBILITY);
            mod.addPossibilityEdges(possibilityEdges);
            mod.addPossibilityEdges(certaintyEdges);
            mod.addPossibilityWords(possibilityWords);
            mod.addPossibilityWords(certaintyWords);
        }
        else if (!certaintyWords.isEmpty() && possibilityWords.isEmpty()){
            mod.setModalityType(Modality.Type.CERTAINTY);
            mod.addCertaintyEdges(certaintyEdges);
            mod.addCertaintyWords(certaintyWords);
        }
        else if (certaintyWords.isEmpty() && !possibilityWords.isEmpty()) {
            mod.setModalityType(Modality.Type.POSSIBILITY);
            mod.addPossibilityEdges(possibilityEdges);
            mod.addPossibilityWords(possibilityWords);
        }
        else if (certaintyWords.isEmpty() && possibilityWords.isEmpty()){
            mod.setModalityType(Modality.Type.CERTAINTY);
        }
        
        return mod;
    }
    
    /** Given a modality object, convert it into a string */
    @Override
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append(CHARACTER.LPARENTHESIS);
        if (this.modalityType == Modality.Type.POSSIBILITY)
            sb.append(Modality.ST_POSSIBILITY);
        else
            sb.append(Modality.ST_CERTAINTY);
        sb.append(CHARACTER.COMMA);
        sb.append(SEPARATOR.SPACE);
        
        if (this.modalityType == Modality.Type.POSSIBILITY){
            for (SemanticGraphEdge edge: this.possibilityEdges){
                if (edge != null){ // TODO: this shouldn't be happening!
                    sb.append(edge.toString());
                    sb.append(CHARACTER.COMMA);
                    sb.append(SEPARATOR.SPACE);
                }
            }
        }
        else if (this.certaintyEdges.size() > 0) {
            for (SemanticGraphEdge edge: this.certaintyEdges){
                if (edge != null){ // TODO: this shouldn't be happening!
                    sb.append(edge.toString());
                    sb.append(CHARACTER.COMMA);
                    sb.append(SEPARATOR.SPACE);
                }
            }
        }
        
        sb.append(CHARACTER.RPARENTHESIS);
        return sb.toString().trim();
    }
}
