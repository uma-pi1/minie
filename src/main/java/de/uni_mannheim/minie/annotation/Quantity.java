package de.uni_mannheim.minie.annotation;

import java.io.IOException;

import de.uni_mannheim.constant.CHARACTER;
import de.uni_mannheim.utils.Dictionary;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Annotation for quantity
 *
 * @author Kiril Gashteovski
 */
public class Quantity {
    /** The quantity words */
    private ObjectArrayList<IndexedWord> qWords; 
    /** The quantity edges **/
    private ObjectArrayList<SemanticGraphEdge> qEdges;
    /** The quantity ID */
    private String id; 
    
    /** A set of quantity determiners **/
    public static Dictionary DT_QUANTITIES;
    static {
        try {
            DT_QUANTITIES = new Dictionary("/minie-resources/quantities-determiners.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** A set of quantity adjectives **/
    public static Dictionary JJ_QUANTITIES;
    static {
        try {
            JJ_QUANTITIES = new Dictionary("/minie-resources/quantities-adjectives.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    /** Static strings used for quantities **/
    public static String ST_QUANTITY = "QUANTITY";
    public static String ST_QUANT = "QUANT";
    
    /** Strings used for IDs for quantities **/
    public static String SUBJECT_ID = "S";
    public static String RELATION_ID = "R";
    public static String OBJECT_ID = "O";
    
    /** Default constructor **/
    public Quantity() {
        this.qWords = new ObjectArrayList<>();
        this.qEdges = new ObjectArrayList<>();
        this.id = CHARACTER.EMPTY_STRING;
    }
    /** Copy constructor **/
    public Quantity(Quantity q){
        this.qWords = q.getQuantityWords();
        this.qEdges = q.getQuantityEdges();
        this.id = q.getId();
    }
    /**
     * Given a list of indexed words and a list of semantic graph edges, create a quantity object which will have
     * qWords as quantity words and qEdges as quantity edges (no ID = empty string)
     * @param qWords: quantity words
     * @param qEdges: quantity edges
     */
    public Quantity(ObjectArrayList<IndexedWord> qWords, ObjectArrayList<SemanticGraphEdge> qEdges){
        this.qWords = qWords.clone();
        this.qEdges = qEdges.clone();
        this.id = CHARACTER.EMPTY_STRING;
    }
    /**
     * Given a list of indexed words, a list of semantic graph edges and an ID, create a quantity object which will have
     * qWords as quantity words and qEdges as quantity edges and ID as an id
     * @param qWords: quantity words
     * @param qEdges: quantity edges
     * @param id: the ID of the quantity
     */
    public Quantity(ObjectArrayList<IndexedWord> qWords, ObjectArrayList<SemanticGraphEdge> qEdges, String id){
        this.qWords = qWords.clone();
        this.qEdges = qEdges.clone();
        this.id = id;
    }
    
    /** Get the quantity words **/
    public ObjectArrayList<IndexedWord> getQuantityWords(){
        return this.qWords;
    }
    /** Get the quantity edges **/
    public ObjectArrayList<SemanticGraphEdge> getQuantityEdges(){
        return this.qEdges;
    }
    /** Get the quantity ID **/
    public String getId(){
        return this.id;
    }
    
    /** Set the quantity words **/
    public void setWords(ObjectArrayList<IndexedWord> words){
        this.qWords = words;
    }
    /** Set the quantity edges **/
    public void setEdges(ObjectArrayList<SemanticGraphEdge> edges){
        this.qEdges = edges;
    }
    /** Set the quantity ID **/
    public void setId(String id){
        this.id = id;
    }
    
    /** Add word to the word list of quantities **/
    public void addWord(IndexedWord w) {
        this.qWords.add(w);
    }
    
    /** Given a quantity object, convert it into a string */
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        
        // Write the words in the format (WORD_1 WORD_2 ... WORD_n) 
        //sb.append(CHARACTER.LPARENTHESIS);
        //sb.append(Quantity.ST_QUANT);
        //sb.append(CHARACTER.UNDERSCORE);
        sb.append(this.id);
        sb.append(CHARACTER.EQUAL);
        for (int i = 0; i < this.qWords.size(); i++){
            sb.append(this.qWords.get(i).word());
            if (i < this.qWords.size() - 1)
                sb.append(CHARACTER.SPACE);
        }
        //sb.append(CHARACTER.RPARENTHESIS);
        
        return sb.toString().trim();
    }
}
