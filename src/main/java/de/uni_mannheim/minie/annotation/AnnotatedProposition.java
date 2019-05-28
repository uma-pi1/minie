package de.uni_mannheim.minie.annotation;

import de.uni_mannheim.constant.CHARACTER;
import de.uni_mannheim.constant.SEPARATOR;
import de.uni_mannheim.constant.WORDS;
import edu.stanford.nlp.ling.IndexedWord;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * @author Kiril Gashteovski
 */
public class AnnotatedProposition {
    /** The annotated proposition is a triple, a list of annotated phrases **/
    private ObjectArrayList<AnnotatedPhrase> triple;
    /** The attribution of the triple if found any **/
    private Attribution attribution;
    /** Polarity of the triple **/
    private Polarity polarity;
    /** Modality of the triple **/
    private Modality modality;
    /** The ID is w.r.t the sentence from which the proposition is extracted. The default ID is -1 **/
    private int id; 
    
    /** Default constructor: empty triple, default attribution, polarity, modality **/
    public AnnotatedProposition(){
        this.triple = new ObjectArrayList<>();
        this.attribution = new Attribution();
        this.polarity = new Polarity();
        this.modality = new Modality();
        this.id = -1; // the default ID
    }
    
    /** Copy constructor **/
    public AnnotatedProposition(AnnotatedProposition p){
        this.triple = p.getTriple().clone();
        this.attribution = p.getAttribution();
        this.polarity = p.getPolarity();
        this.modality = p.getModality();
        this.id = p.getId();
    }
    
    /** Parametric constructor **/
    public AnnotatedProposition(ObjectArrayList<AnnotatedPhrase> t, Attribution s, Polarity pol, Modality mod, int id){
        this.triple = t;
        this.attribution = s;
        this.polarity = pol;
        this.modality = mod;
        this.id = id;
    }
    
    /** Constructor given list of phrases only **/
    public AnnotatedProposition(ObjectArrayList<AnnotatedPhrase> t){
        this.triple = t;
        this.attribution = new Attribution();
        this.polarity = new Polarity();
        this.modality = new Modality();
        this.id = -1;
    }
    
    /** Constructor given list of phrases and attribution only **/
    public AnnotatedProposition(ObjectArrayList<AnnotatedPhrase> t, Attribution s){
        this.triple = t;
        this.attribution = s;
        this.polarity = new Polarity();
        this.modality = new Modality();
        this.id = -1;
    }
    
    /** Constructor given list of phrases and id only **/
    public AnnotatedProposition(ObjectArrayList<AnnotatedPhrase> t, int id){
        this.triple = t;
        this.attribution = new Attribution();
        this.polarity = new Polarity();
        this.modality = new Modality();
        this.id = id;
    }
    
    /** Constructor given list of phrases, attribution and ID **/
    public AnnotatedProposition(ObjectArrayList<AnnotatedPhrase> t, Attribution s, int id){
        this.triple = t;
        this.attribution = s;
        this.polarity = new Polarity();
        this.modality = new Modality();
        this.id = id;
    }
    
    // Setters
    public void setTriple(ObjectArrayList<AnnotatedPhrase> t){
        this.triple = t.clone();
    }
    public void setAttribution(Attribution s){
        this.attribution = s;
    }
    public void setPolarity(Polarity p){
        this.polarity = p;
    }
    public void setModality(Modality m){
        this.modality = m;
    }
    public void setSubject(AnnotatedPhrase subj){
        this.triple.set(0, subj);
    }
    public void setRelation(AnnotatedPhrase rel){
        this.triple.set(1, rel);
    }
    public void setObject(AnnotatedPhrase obj){
        this.triple.set(2, obj);
    }
    public void setId(int id){
        this.id = id;
    }
    /**
     * Set the quantities for i-th annotated phrase
     * @param i: the index of the constituent 
     * @param q: the list of quantities to be set on the i-th annotated phrase
     */
    public void setQuantities(int i, ObjectArrayList<Quantity> q){
        this.getTriple().get(i).setQuantities(q);
    }
    
    // Getters
    public ObjectArrayList<AnnotatedPhrase> getTriple(){
        return this.triple;
    }
    public AnnotatedPhrase getSubject(){
        return this.triple.get(0);
    }
    public AnnotatedPhrase getRelation(){
        return this.triple.get(1);
    }
    /** If the proposition is consisted of 2 constituents, return empty phrase **/
    public AnnotatedPhrase getObject(){
        if (this.triple.size() == 3)
            return this.triple.get(2);
        else 
            return new AnnotatedPhrase();
    }
    public Attribution getAttribution(){
        return this.attribution;
    }
    public Polarity getPolarity(){
        return this.polarity;
    }
    public Modality getModality(){
        return this.modality;
    }
    public int getId(){
        return this.id;
    }
    
    /**
     * Get the quantities from the i-th phrase
     * @param i: the index of the annotated phrase
     * @return list of quantities for the phrase
     */
    public ObjectArrayList<Quantity> getQuantities(int i){
        return this.getTriple().get(i).getQuantities();
    }
    
    /**
     * Get all the quantities in the annotated proposition
     * @return
     */
    public ObjectArrayList<Quantity> getAllQuantities() {
        ObjectArrayList<Quantity> quantities = new ObjectArrayList<>();
        for (int i = 0; i < this.getTriple().size(); i++) {
            quantities.addAll(this.getTriple().get(i).getQuantities());
        }
        return quantities;
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        
        // Write the triple
        sb.append(this.getTripleAsString());
        
        // Write the attribution (if found any)
        if (this.getAttribution().getAttributionPhrase() != null){
            sb.append("\t Source:");
            sb.append(this.getAttribution());
        }
        
        // Write polarity (if negative)
        if (this.getPolarity().getType() == Polarity.Type.NEGATIVE){
            sb.append("\t Polarity: " + Polarity.ST_NEGATIVE);
        }
        
        // Write modality (if poss found)
        if (this.getModality().getModalityType() == Modality.Type.POSSIBILITY){
            sb.append("\t Modality:" + Modality.ST_POSSIBILITY);
        }
        
        // Write quantities if there are any
        for (int j = 0; j < this.getTriple().size(); j++){
            if (this.getQuantities(j).size() > 0){
                sb.append("\t Quantifiers:");
                for (Quantity q: this.getQuantities(j)){
                    sb.append(q.toString());
                }
            }
        }

        return sb.toString();
    }
    
    /** Get the extraction as string **/
    public String getTripleAsString() {
        StringBuilder sb = new StringBuilder();
        
        // Write the triple
        for (int j = 0; j < this.getTriple().size(); j++){
            sb.append(CHARACTER.QUOTATION_MARK);
            sb.append(getTriple().get(j).getWords());
            sb.append(CHARACTER.QUOTATION_MARK);
            sb.append(SEPARATOR.TAB);
        }
        
        return sb.toString();
    }
    
    /** Get factuality as a string in format "(POLARITY, MODALITY)" **/
    public String getFactualityAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CHARACTER.LPARENTHESIS);
        if (this.polarity.getType() == Polarity.Type.POSITIVE)
            sb.append(Polarity.ST_PLUS);
        else
            sb.append(Polarity.ST_MINUS);
        sb.append(CHARACTER.COMMA);
        if (this.modality.getModalityType() == Modality.Type.CERTAINTY)
            sb.append(Modality.ST_CT);
        else
            sb.append(Modality.ST_PS);
        sb.append(CHARACTER.RPARENTHESIS);
        return sb.toString();
    }
    
    public String toStringAllAnnotations(){
       StringBuilder sb = new StringBuilder();
        
       // Write the triple
       sb.append(CHARACTER.LPARENTHESIS);
       IndexedWord tempWord;
       for (int i = 0; i < this.getTriple().size(); i++){
           for (int j = 0; j < this.getTriple().get(i).getWordList().size(); j++) {
               tempWord =  this.getTriple().get(i).getWordList().get(j);
               sb.append(tempWord.word());
               if (j == this.getTriple().get(i).getWordList().size() - 1) {
                   if (i < 2) {
                       sb.append("; ");
                   }
               } else {
                   sb.append(" ");
               }
           }
           /*sb.append("\"");
           sb.append(this.getTriple().get(j).getWords());
           sb.append("\"\t");*/
       }
       sb.append(")");
        
       // Write factuality
       sb.append("\tfactuality: (");
       if (this.getPolarity().getType() == Polarity.Type.NEGATIVE) 
           sb.append("-,");
       else
           sb.append("+,");
       if (this.getModality().getModalityType() == Modality.Type.POSSIBILITY)
           sb.append("PS)");
       else
           sb.append("CT)");
        
       // Write quantities
       sb.append("\tquantities: (");
       ObjectArrayList<Quantity> quantities = new ObjectArrayList<>();
       for (int j = 0; j < this.getTriple().size(); j++){
           if (this.getQuantities(j).size() > 0){
               for (int k = 0; k < this.getQuantities(j).size(); k++){
                   Quantity q = this.getQuantities(j).get(k);
                   quantities.add(q);
                   /*sb.append(q.toString());
                   if (k < (this.getQuantities(j).size() - 1)) {
                       sb.append(" ");
                   }*/
               }/*
               for (Quantity q: this.getQuantities(j)){
                   sb.append(q.toString());
               }*/
           }
       }
       for (int i = 0; i < quantities.size(); i++) {
           sb.append(quantities.get(i).toString());
           if (i < quantities.size() - 1)
               sb.append(" ");
       }
       sb.append(")");
       
       // Write the attribution (if found any) 
       sb.append("\tattribution: ");
       if (this.getAttribution().getAttributionPhrase() != null){
            sb.append(this.getAttribution().toString());
       } else {
           sb.append("NO SOURCE DETECTED");
       }
       
       return sb.toString();
    }
    
    /**
     * Get the proposition's words to a string (separated by a space) without the annotations.
     * Each element of the proposition is separated by quotation marks
     * E.g. "John""lives in""Canada"
     * @return triple's words as a string separated by a space
     */
    public String propositionWordsToString(){
        StringBuilder sb = new StringBuilder();
        // Write the triple
        for (int i = 0; i < this.getTriple().size(); i++){
            for (int j = 0; j < this.getTriple().get(i).getWordList().size(); j++) {
                if (j == 0) {
                    sb.append(CHARACTER.QUOTATION_MARK);
                }
                sb.append(this.getTriple().get(i).getWordList().get(j).word());
                if (j == this.getTriple().get(i).getWordList().size()-1) {
                    sb.append(CHARACTER.QUOTATION_MARK);
                } else {
                    sb.append(SEPARATOR.SPACE);
                }
            }
        }
        return sb.toString().trim();
    }
    
    /**
     * Get the annotated triple's words convenient for aggregation:
     * - take lemmas of each word
     * - each word is presented in lowercase
     * - each quantity is presented as QUANT
     * 
     * The string is in the following format:
     * ("subj"; "rel"; "obj")   Factiality:(POLARITY, MODALITY)    Attribution:(phrase, predicate, (POLARITY, MODALITY) 
     * 
     * @return the "aggregation string" for the annotated triple
     */
    public String toAggregationString(){
        StringBuilder sb = new StringBuilder();
        
        // Write the triple
        IndexedWord word;
        for (int i = 0; i < this.triple.size(); i++) {
            sb.append(CHARACTER.QUOTATION_MARK);
            for (int j = 0; j< triple.get(i).getWordList().size(); j++) {
                word = triple.get(i).getWordList().get(j);
                
                // Write either QUANT for quantities or the word's lemma in lower case
                if (word.tag().equals(Quantity.ST_QUANTITY))
                    sb.append(Quantity.ST_QUANT);
                else
                    sb.append(word.lemma().toLowerCase());
                
                // Write either an empty space or "\t if it's the last word to be written
                if (j == triple.get(i).getWordList().size() - 1){
                    sb.append(CHARACTER.QUOTATION_MARK);
                    if (i < this.triple.size() - 1)
                        sb.append(CHARACTER.SEMI_COLON);
                }
                else {
                    sb.append(SEPARATOR.SPACE);
                }
            }
        }
        sb.append(SEPARATOR.TAB);
        
        // Write the factuality
        sb.append(WORDS.factuality + CHARACTER.COLON + CHARACTER.LPARENTHESIS);
        if (this.polarity.getType() == Polarity.Type.POSITIVE)
            sb.append(CHARACTER.PLUS);
        else 
            sb.append(CHARACTER.MINUS);
        sb.append(CHARACTER.COMMA);
        if (this.modality.getModalityType() == Modality.Type.CERTAINTY)
            sb.append(Modality.ST_CT);
        else
            sb.append(Modality.ST_PS);
        sb.append(CHARACTER.RPARENTHESIS + SEPARATOR.TAB);
        
        // Write the attribution
        sb.append(WORDS.attribution + CHARACTER.COLON + CHARACTER.LPARENTHESIS);
        if (this.attribution.getAttributionPhrase() == null) {
            sb.append("NONE");
        } else {
            // Write the attribution phrase
            for (int i = 0; i < this.attribution.getAttributionPhrase().getWordList().size(); i++){
                word = this.attribution.getAttributionPhrase().getWordList().get(i);
                sb.append(word.lemma().toLowerCase());
                if (i == this.attribution.getAttributionPhrase().getWordList().size() - 1){
                    sb.append(CHARACTER.COMMA);
                } else {
                    sb.append(SEPARATOR.SPACE);
                }
            }
            
            // Write the attribution predicate
            sb.append(this.attribution.getPredicateVerb());
            sb.append(CHARACTER.COMMA);
            
            // Write the factuality of the attribution
            sb.append(CHARACTER.LPARENTHESIS);
            if (this.attribution.getPolarityType() == Polarity.Type.POSITIVE)
                sb.append(CHARACTER.PLUS);
            else
                sb.append(CHARACTER.MINUS);
            sb.append(CHARACTER.COMMA);
            if (this.attribution.getModalityType() == Modality.Type.CERTAINTY)
                sb.append(Modality.ST_CT);
            else
                sb.append(Modality.ST_PS);
            
            sb.append(CHARACTER.RPARENTHESIS);
        }
        sb.append(CHARACTER.RPARENTHESIS);
        
        return sb.toString().trim();
    }
}
