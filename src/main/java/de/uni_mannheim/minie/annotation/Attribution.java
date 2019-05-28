package de.uni_mannheim.minie.annotation;

import de.uni_mannheim.constant.CHARACTER;
import de.uni_mannheim.constant.SEPARATOR;

/**
 * @author Kiril Gashteovski
 */

public class Attribution {
    /**
     * A class representing the attribution
     *
     * attributionPhrase: a phrase containing the words for the attribution
     * modality: the modality of the attribution (possibility or certainty)
     * polarity: the polarity of the attribution (positive or negative)
     * predicateVerb: the predicate verb (as a string in its lemma version)
     */
    private AnnotatedPhrase attributionPhrase;
    private Modality.Type modality;
    private Polarity.Type polarity;
    private String predicateVerb;
    
    /** Some string constants necessary for detecting the attribution **/
    public static String ACCORDING = "according";
    
    /** Default constructor: modality == certainty, polarity == positive, attributionPhrase == null */
    public Attribution(){
        this.attributionPhrase = null;
        this.modality = Modality.Type.CERTAINTY;
        this.polarity = Polarity.Type.POSITIVE;
        this.predicateVerb = CHARACTER.EMPTY_STRING;
    }
    
    /** Constructor with a given attribution phrase. The modality and polarity are by default 'certainty' and 'positive' 
     *  respectively
     *  
     *  @param attributionPhrase: the attribution phrase
     *  @param pVerb: the predicate verb (a string)
     */
    public Attribution(AnnotatedPhrase attributionPhrase, String pVerb){
        this.attributionPhrase = attributionPhrase;
        this.modality = Modality.Type.CERTAINTY;
        this.polarity = Polarity.Type.POSITIVE;
        this.predicateVerb = pVerb;
    }
    
    /**
     * Fully parameterized constructor  
     * @param attributionPhrase: the attribution phrase
     * @param pol: polarity type
     * @param mod: modality type
     * @param pVerb: predicate verb
     */
    public Attribution(AnnotatedPhrase attributionPhrase, Polarity.Type pol, Modality.Type mod, String pVerb){
        this.attributionPhrase = attributionPhrase;
        this.modality = mod;
        this.polarity = pol;
        this.predicateVerb = pVerb;
    }
    /** Copy constructor **/
    public Attribution(Attribution s){
        this.attributionPhrase = s.getAttributionPhrase();
        this.modality = s.getModalityType();
        this.polarity = s.getPolarityType();
        this.predicateVerb = s.getPredicateVerb();
    }
    
    // Getters
    public AnnotatedPhrase getAttributionPhrase(){
        return this.attributionPhrase;
    }
    public Modality.Type getModalityType(){
        return this.modality;
    }
    public Polarity.Type getPolarityType(){
        return this.polarity;
    }
    public String getPredicateVerb(){
        return this.predicateVerb;
    }
    
    // Setters
    public void setAttributionPhrase(AnnotatedPhrase s){
        this.attributionPhrase = s;
    }
    public void setModalityType(Modality.Type t){
        this.modality = t;
    }
    public void setPolarityType(Polarity.Type t){
        this.polarity = t;
    }
    public void setPredicateVerb(String pVerb){
        this.predicateVerb = pVerb;
    }
    
    // Clear the attribution
    public void clear(){
        this.attributionPhrase = null;
        this.modality = Modality.Type.CERTAINTY;
        this.polarity = Polarity.Type.POSITIVE;
        this.predicateVerb = CHARACTER.EMPTY_STRING;
    }
    
    // Write down the attribution in the format (attribution_phrase, predicate, polarity, modality)
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(CHARACTER.LPARENTHESIS);

        // Append the attribution phrase
        for (int i = 0; i < this.attributionPhrase.getWordList().size(); i++) {
            sb.append(this.attributionPhrase.getWordList().get(i).word());
            if (i < this.attributionPhrase.getWordList().size() - 1)
                sb.append(SEPARATOR.SPACE);
        }

        sb.append(SEPARATOR.COMMA);
        sb.append(SEPARATOR.SPACE);
        
        // Append the predicate verb
        sb.append("Predicate: ");
        sb.append(this.predicateVerb);
        sb.append(SEPARATOR.COMMA);
        sb.append(SEPARATOR.SPACE);
        
        // Append the polarity
        sb.append("POLARITY:  ");
        if (this.polarity == Polarity.Type.POSITIVE)
            sb.append(Polarity.ST_POSITIVE);
        else 
            sb.append(Polarity.ST_NEGATIVE);
        sb.append(SEPARATOR.SPACE);
        sb.append(SEPARATOR.COMMA);
        sb.append(SEPARATOR.SPACE);
        
        // Append the modality
        sb.append("MODALITY:  ");
        if (this.modality == Modality.Type.CERTAINTY)
            sb.append(Modality.ST_CERTAINTY);
        else
            sb.append(Modality.ST_POSSIBILITY);
        sb.append(CHARACTER.RPARENTHESIS);
        
        return sb.toString().trim();
    }
    
    /** Return the attribution as a string in format "(Attribution Phrase, (POLARITY, MODALITY)) **/
    public String toStringCompact() {
        StringBuilder sb = new StringBuilder();
        sb.append(CHARACTER.LPARENTHESIS);

        // Append the attribution phrase
        for (int i = 0; i < this.attributionPhrase.getWordList().size(); i++) {
            sb.append(this.attributionPhrase.getWordList().get(i).word());
            if (i < this.attributionPhrase.getWordList().size() - 1)
                sb.append(SEPARATOR.SPACE);
        }
        
        sb.append(SEPARATOR.COMMA);
        sb.append(SEPARATOR.SPACE);
        
        // Append the factuality
        sb.append(CHARACTER.LPARENTHESIS);
        if (this.polarity == Polarity.Type.POSITIVE)
            sb.append(Polarity.ST_PLUS);
        else 
            sb.append(Polarity.ST_MINUS);
        sb.append(SEPARATOR.COMMA);
        if (this.modality == Modality.Type.CERTAINTY)
            sb.append(Modality.ST_CT);
        else
            sb.append(Modality.ST_PS);
        sb.append(CHARACTER.RPARENTHESIS);
        sb.append(CHARACTER.RPARENTHESIS);
        
        return sb.toString();
    }
}
