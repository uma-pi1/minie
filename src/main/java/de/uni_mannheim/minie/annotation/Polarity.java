package de.uni_mannheim.minie.annotation;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;

import de.uni_mannheim.constant.CHARACTER;
import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.constant.SEPARATOR;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;
import de.uni_mannheim.utils.Dictionary;

/**
 * Annotation for polarity
 *
 * @author Kiril Gashteovski
 */
public class Polarity {
    /** Annotations for polarity, can be just "POSTIIVE" or "NEGATIVE" */
    public enum Type {POSITIVE, NEGATIVE}
    
    /** Static strings for polarity **/
    public static String ST_POSITIVE = "POSITIVE";
    public static String ST_PLUS = "+";
    public static String ST_NEGATIVE = "NEGATIVE";
    public static String ST_MINUS = "-";
    
    /** List of negative words and edges (if found any) **/
    private ObjectArrayList<IndexedWord> negativeWords; 
    private ObjectArrayList<SemanticGraphEdge> negativeEdges;
    
    /** Polarity type **/
    private Polarity.Type polarityType;
    
    /** A set of all negative words **/
    public static Dictionary NEG_WORDS;
    static {
        try {
            NEG_WORDS = new Dictionary("/minie-resources/neg-words.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** A set of negative adverbs **/
    public static Dictionary NEG_ADVERBS;
    static {
        try {
            NEG_ADVERBS = new Dictionary("/minie-resources/neg-adverbs.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** Set of negative determiners **/
    public static Dictionary NEG_DETERMINERS;
    static {
        try {
            NEG_DETERMINERS = new Dictionary("/minie-resources/neg-determiners.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** Default constructor. Assumes positive polarity type by default **/
    public Polarity(){
        this.polarityType = Type.POSITIVE;
        this.negativeEdges = new ObjectArrayList<>();
        this.negativeWords = new ObjectArrayList<>();
    }
    
    /**
     * Constructor given the polarity type. Creates empty lists for negative words and edges
     * @param t: polarity type
     */
    public Polarity(Polarity.Type t){
        this.polarityType = t;
        this.negativeEdges = new ObjectArrayList<>();
        this.negativeWords = new ObjectArrayList<>();
    }
    /**
     * Copy constructor
     * @param p: polarity object
     */
    public Polarity(Polarity p){
        this.polarityType = p.getType();
        this.negativeEdges = p.getNegativeEdges();
        this.negativeWords = p.getNegativeWords();
    }
    /**
     * Parametric constructor, given the polarity types, negative words, negative edges
     * @param t: polarity type
     * @param negWords: list of negative words
     * @param negEdges: list of negative edges
     */
    public Polarity(Polarity.Type t, ObjectArrayList<IndexedWord> negWords, ObjectArrayList<SemanticGraphEdge> negEdges){
        this.polarityType = t;
        this.negativeEdges = negEdges;
        this.negativeWords = negWords;
    }
    
    /** Getters **/
    public Polarity.Type getType(){
        return this.polarityType;
    }
    public ObjectArrayList<IndexedWord> getNegativeWords(){
        return this.negativeWords;
    }
    public ObjectArrayList<SemanticGraphEdge> getNegativeEdges(){
        return this.negativeEdges;
    }
    
    /** Setters **/
    public void setType(Polarity.Type t){
        this.polarityType = t;
    }
    public void setNegativeWords(ObjectArrayList<IndexedWord> negWords){
        this.negativeWords = negWords;
    }
    public void setNegativeEdges(ObjectArrayList<SemanticGraphEdge> negEdges){
        this.negativeEdges = negEdges;
    }
    
    /** Adding elements to lists **/
    public void addNegativeEdge(SemanticGraphEdge e){
        this.negativeEdges.add(e);
    }
    public void addNegativeWord(IndexedWord w){
        this.negativeWords.add(w);
    }
    
    /** Clear the polarity object, i.e. set its default values (type = positive, neg. words and edges are empty lists) */
    public void clear(){
        this.polarityType = Type.POSITIVE;
        this.negativeEdges = new ObjectArrayList<>();
        this.negativeWords = new ObjectArrayList<>();
    }
    
    /**
     * Given a phrase and its sentence semantic graph, detect the polarity type. If negative polarity is found, add the 
     * negative words and edges to their appropriate lists from the Polarity class.
     * 
     * @param phrase: phrase (essentially, list of words, which are part of some sentence)
     * @param sentenceSemGraph: the semantic graph of the phrase's sentence
     * @return polarity object
     */
    public static Polarity getPolarity(AnnotatedPhrase phrase, SemanticGraph sentenceSemGraph){
        Polarity pol = new Polarity();
        
        for (int i = 0; i < phrase.getWordList().size(); i++){
            // Check for negative adverbs
            if (CoreNLPUtils.isAdverb(phrase.getWordList().get(i).tag())){
                if (Polarity.NEG_ADVERBS.contains(phrase.getWordList().get(i).lemma())){
                    Polarity.setNegPol(pol, phrase.getWordList().get(i), sentenceSemGraph.getEdge(
                                                                    sentenceSemGraph.getParent(phrase.getWordList().get(i)), 
                                                                    phrase.getWordList().get(i)));
                }
            }
            // Check for negative determiners
            else if (phrase.getWordList().get(i).tag().equals(POS_TAG.DT)){
                if (Polarity.NEG_DETERMINERS.contains(phrase.getWordList().get(i).lemma())){
                    Polarity.setNegPol(pol, phrase.getWordList().get(i), sentenceSemGraph.getEdge(
                            sentenceSemGraph.getParent(phrase.getWordList().get(i)), 
                            phrase.getWordList().get(i)));
                }
            }
        }
        
        return pol;
    }
    
    /**
     * Given a polarity object, negative word and a negative edge, set the polarity type to "negative" and add the 
     * negative words and edges to their appropriate lists
     * 
     * @param pol: polarity object
     * @param negWord: negative word
     * @param negEdge: negative edge
     */
    private static void setNegPol(Polarity pol, IndexedWord negWord, SemanticGraphEdge negEdge){
        pol.setType(Polarity.Type.NEGATIVE);
        pol.addNegativeWord(negWord);
        pol.addNegativeEdge(negEdge);
    }
    
    /** Given a polarity object, convert it into a string */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(CHARACTER.LPARENTHESIS);
        if (this.polarityType == Polarity.Type.POSITIVE)
            sb.append(CHARACTER.PLUS);
        else { 
            sb.append(CHARACTER.MINUS);
            sb.append(CHARACTER.COMMA);
            sb.append(SEPARATOR.SPACE);
            for (SemanticGraphEdge edge: this.negativeEdges){
                sb.append(edge.toString());
                sb.append(CHARACTER.COMMA);
                sb.append(SEPARATOR.SPACE);
            }
        }
        
        sb.append(CHARACTER.RPARENTHESIS);
        return sb.toString().trim();
    }
}
