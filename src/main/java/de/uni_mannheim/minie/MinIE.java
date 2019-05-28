package de.uni_mannheim.minie;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.uni_mannheim.clausie.ClausIE;
import de.uni_mannheim.clausie.clause.Clause;
import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.clausie.proposition.Proposition;
import de.uni_mannheim.constant.NE_TYPE;
import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.constant.REGEX;
import de.uni_mannheim.constant.SEPARATOR;
import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.annotation.AnnotatedProposition;
import de.uni_mannheim.minie.annotation.Attribution;
import de.uni_mannheim.minie.annotation.Modality;
import de.uni_mannheim.minie.annotation.Polarity;
import de.uni_mannheim.minie.minimize.object.ObjAggressiveMinimization;
import de.uni_mannheim.minie.minimize.object.ObjDictionaryMinimization;
import de.uni_mannheim.minie.minimize.object.ObjSafeMinimization;
import de.uni_mannheim.minie.minimize.relation.RelAggressiveMinimization;
import de.uni_mannheim.minie.minimize.relation.RelDictionaryMinimization;
import de.uni_mannheim.minie.minimize.relation.RelSafeMinimization;
import de.uni_mannheim.minie.minimize.subject.SubjAggressiveMinimization;
import de.uni_mannheim.minie.minimize.subject.SubjDictionaryMinimization;
import de.uni_mannheim.minie.minimize.subject.SubjSafeMinimization;
import de.uni_mannheim.minie.proposition.ImplicitExtractions;

import de.uni_mannheim.utils.phrase.PhraseUtils;
import de.uni_mannheim.utils.Dictionary;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.util.CoreMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * @author Kiril Gashteovski
 */
public class MinIE {
    /** List of annotated propositions **/
    private ObjectArrayList<AnnotatedProposition> propositions;

    /** The semantic graph of the whole sentence **/
    private SemanticGraph sentenceSemGraph;
    
    /** The whole sentence as a list of indexed words **/
    private ObjectArrayList<IndexedWord> sentence;

    /** The whole original sentence as a list of indexed words **/
    private ObjectArrayList<IndexedWord> originalSentence;
    
    /** Reusability variables **/
    private ObjectOpenHashSet<String> propsWithAttribution;
    
    /** Constructor **/
    public MinIE(ObjectArrayList<AnnotatedProposition> props){
        this.propositions = props;
    }
    
    /** MinIE mode **/
    public enum Mode {
        AGGRESSIVE,
        DICTIONARY,
        SAFE,
        COMPLETE
    }
    
    /** Default constructor **/
    public MinIE(){
        this.propositions = new ObjectArrayList<AnnotatedProposition>();
        this.sentenceSemGraph = new SemanticGraph();
        this.sentence = new ObjectArrayList<>();
        this.propsWithAttribution = new ObjectOpenHashSet<>();
    }
    
    /**
     * @param sentence - input sentence
     * @param parser - dependency parse pipeline of the sentence
     * @param mode - the minimization mode
     * @param d - dictionary of multi-word expressions (for MinIE-D)
     */
    public MinIE(String sentence, StanfordCoreNLP parser, Mode mode, Dictionary d) {
        // Initializations
        this.propositions = new ObjectArrayList<AnnotatedProposition>();
        this.sentenceSemGraph = new SemanticGraph();
        this.sentence = new ObjectArrayList<>();
        this.propsWithAttribution = new ObjectOpenHashSet<>();
        
        this.minimize(sentence, parser, mode, d);
    }
    
    /**
     * @param sentence - input sentence
     * @param parser - dependency parse pipeline of the sentence
     * @param mode - the minimization mode
     * 
     * NOTE: If mode is MinIE-D, then this will proceed as MinIE-D but with empty dictionary 
     * (i.e. will drop every word that is a candidate)
     */
    public MinIE(String sentence, StanfordCoreNLP parser, Mode mode) {
        this.propositions = new ObjectArrayList<AnnotatedProposition>();
        this.sentenceSemGraph = new SemanticGraph();
        this.sentence = new ObjectArrayList<>();
        this.propsWithAttribution = new ObjectOpenHashSet<>();
        
        this.minimize(sentence, parser, mode, new Dictionary());
    }
    
    /**
     * @param sentence - input sentence
     * @param sg - dependency parse graph of the sentence
     * @param mode - the minimization mode
     * 
     * NOTE: If mode is MinIE-D, then this will proceed as MinIE-D but with empty dictionary 
     * (i.e. will drop every word that is a candidate)
     */
    public MinIE(String sentence, SemanticGraph sg, Mode mode) {
        this.propositions = new ObjectArrayList<AnnotatedProposition>();
        this.sentenceSemGraph = new SemanticGraph();
        this.sentence = new ObjectArrayList<>();
        this.propsWithAttribution = new ObjectOpenHashSet<>();
        
        this.minimize(sentence, sg, mode, new Dictionary());
    }

    /**
     * @param sentence - input sentence
     * @param sg - dependency parse graph of the sentence
     * @param mode - the minimization mode
     * @param d - dictionary of multi-word expressions (for MinIE-D)
     */
    public MinIE(String sentence, SemanticGraph sg, Mode mode, Dictionary dict) {
        this.propositions = new ObjectArrayList<AnnotatedProposition>();
        this.sentenceSemGraph = new SemanticGraph();
        this.sentence = new ObjectArrayList<>();
        this.propsWithAttribution = new ObjectOpenHashSet<>();
        
        this.minimize(sentence, sg, mode, dict);
    }
    
    /** 
     * Given an input sentence, parser, mode and a dictionary, make extractions and then minimize them accordingly.
     * The parsing occurs INSIDE this function.
     * 
     * @param sentence - input sentence
     * @param parser - dependency parse pipeline for the sentence
     * @param mode - minimization mode
     * @param d - dictionary (for MinIE-D)
     */
    public void minimize(String sentence, StanfordCoreNLP parser, Mode mode, Dictionary d) {
        // Run ClausIE first
        ClausIE clausie = new ClausIE();
        clausie.setSemanticGraph(CoreNLPUtils.parse(parser, sentence));
        clausie.detectClauses();
        clausie.generatePropositions(clausie.getSemanticGraph());
        
        // Start minimizing by annotating
        this.setSemanticGraph(clausie.getSemanticGraph());
        this.setPropositions(clausie);
        this.setPolarity();
        this.setModality();
        
        // Minimize according to the modes (COMPLETE mode doesn't minimize) 
        if (mode == Mode.SAFE)
            this.minimizeSafeMode();
        else if (mode == Mode.DICTIONARY)
            this.minimizeDictionaryMode(d.words());
        else if (mode == Mode.AGGRESSIVE)
            this.minimizeAggressiveMode();
        
        this.removeDuplicates();
    }
    
    /** 
     * Given an input sentence, dependency parse, mode and a dictionary, make extractions and then minimize them accordingly.
     * The parsing occurs OUTSIDE this function.
     * 
     * @param sentence - input sentence
     * @param sg - semantic graph object (dependency parse of the sentence)
     * @param mode - minimization mode
     * @param d - dictionary (for MinIE-D)
     */
    public void minimize(String sentence, SemanticGraph sg, Mode mode, Dictionary d) {
        // Run ClausIE first
        ClausIE clausie = new ClausIE();
        clausie.setSemanticGraph(sg);
        clausie.detectClauses();
        clausie.generatePropositions(clausie.getSemanticGraph());
        
        // Start minimizing by annotating
        this.setSemanticGraph(clausie.getSemanticGraph());
        this.setPropositions(clausie);
        this.setPolarity();
        this.setModality();
        
        // Minimize according to the modes (COMPLETE mode doesn't minimize) 
        if (mode == Mode.SAFE)
            this.minimizeSafeMode();
        else if (mode == Mode.DICTIONARY)
            this.minimizeDictionaryMode(d.words());
        else if (mode == Mode.AGGRESSIVE)
            this.minimizeAggressiveMode();
        
        this.removeDuplicates();
    }
    
    /** Clear the variables **/
    public void clear(){
        this.propositions.clear();
        this.sentenceSemGraph = null;
        this.sentence.clear();
        this.propsWithAttribution.clear();
    }
    
    /**
     * Getter for the propositions
     * @return: list of propositions (which are a list of phrases)
     */
    public ObjectArrayList<AnnotatedProposition> getPropositions(){
        return this.propositions;
    }
    public AnnotatedProposition getProposition(int i){
        return this.propositions.get(i);
    }
    public SemanticGraph getSentenceSemanticGraph(){
        return this.sentenceSemGraph;
    }
    public int getPropositionSize(int i){
        return this.propositions.get(i).getTriple().size();
    }
    public ObjectArrayList<IndexedWord> getSentenceWords() {
        return this.sentence;
    }
    public ObjectArrayList<IndexedWord> getOriginalSentence() {
        return this.originalSentence;
    }
    
    /**
     * Getters for the negative, certain or possibility propositions
     */
    public boolean isPossibility(int i){
        return this.isPossibility(i);
    }
    public boolean isCertainty(int i){
        return this.isCertainty(i);
    }
    
    /**
     * Other getters
     */
    public AnnotatedPhrase getSubject(int i){
        return this.propositions.get(i).getSubject();
    }
    public AnnotatedPhrase getRelation(int i){
        return this.propositions.get(i).getRelation();
    }
    /** Assuming that the proposition is a triple, it will return the third constituent in the list */
    public AnnotatedPhrase getObject(int i){
        return this.propositions.get(i).getObject();
    }
    
    /**
     * Setters
     */
    public void setPropositions(ObjectArrayList<AnnotatedProposition> props){
        this.propositions = props;
    }
    public void setProposition(int i, AnnotatedProposition prop){
        this.propositions.set(i, prop);
    }
    public void setSubject(int i, AnnotatedPhrase subj){
        this.propositions.get(i).setSubject(subj);
    }
    public void setRelation(int i, AnnotatedPhrase rel){
        this.propositions.get(i).setRelation(rel);
    }
    public void setObject(int i, AnnotatedPhrase obj){
        this.propositions.get(i).setObject(obj);
    }
    public void setAttribution(int i, Attribution s){
        this.propositions.get(i).setAttribution(s);
    }
    public void setSentenceWords(ObjectArrayList<IndexedWord> s) {
        this.sentence = s;
    }
    
    /** Reset a attribution of the list **/
    public void resetAttribution(int i){
        this.propositions.get(i).setAttribution(new Attribution());
    }
    
    /**
     * Given a proposition, detect the attribution. Returns true if a attribution was detected and false otherwise.
     * @param proposition
     */
    public boolean detectAttribution(Proposition proposition){
        // If the proposition is of size 2, return (nothing to detect here, it's an SV)
        if (proposition.getConstituents().size() < 3)
            return false;
        
        // Attribution flag is set to 'false' by default
        boolean attributionDetected = false;
        
        // Reusable variables
        ClausIE clausieObj = new ClausIE();
        StringBuffer sb = new StringBuffer();
        ObjectArrayList<IndexedWord> tempListOfWords = new ObjectArrayList<IndexedWord>();
        
        // Elements of the triple
        AnnotatedPhrase subject = new AnnotatedPhrase(proposition.subject());
        AnnotatedPhrase relation = new AnnotatedPhrase(proposition.relation());
        AnnotatedPhrase object = new AnnotatedPhrase(proposition.object());
        
        // Get the root and if it's null, return 'true'
        relation.setRoot(CoreNLPUtils.getRootFromWordList(this.sentenceSemGraph, relation.getWordList()));
        IndexedWord root = relation.getRoot();
        if (root == null) return true;
        
        // Detect "according to..." patterns by checking the adverbials (i.e. the objects)
        if (object.getWordList().size() > 2){
            if (object.getWordList().get(0).word().toLowerCase().equals(Attribution.ACCORDING) && 
                    object.getWordList().get(1).tag().equals(POS_TAG.TO)){
                tempListOfWords.clear();
                tempListOfWords.addAll(subject.getWordList());
                tempListOfWords.addAll(relation.getWordList());
                SemanticGraph newsg = CoreNLPUtils.getSubgraphFromWords(this.sentenceSemGraph, tempListOfWords);
                        
                // The attribution predicate "according to"
                sb.append(Attribution.ACCORDING);
                sb.append(SEPARATOR.SPACE);
                sb.append(object.getWordList().get(1).word());
                
                this.generatePropositionsWithAttribution(clausieObj, newsg, new Attribution(
                                    new AnnotatedPhrase(object.getWordSubList(2, object.getWordList().size()-1)), 
                                    Polarity.Type.POSITIVE, 
                                    Modality.Type.CERTAINTY, 
                                    sb.toString().trim()));
                sb.setLength(0);
                attributionDetected = true;
            }    
        }
        
        // Modality and polarity of the attribution (detecting attribution with predicates)
        Polarity.Type pol = Polarity.Type.POSITIVE; // TODO: default value; this is temporary
        Modality.Type mod = null;
        IndexedWord relHead = relation.getRoot();
        if (Modality.VERB_CERTAINTY.contains(relHead.lemma().toLowerCase())){
            // By default, the modality is CERTAINTY unless proven otherwise
            mod = Modality.Type.CERTAINTY;
            
            // If the head verb of the relation is negated, set polarity to NEGATIVE
            if (sentenceSemGraph.getChildWithReln(relHead, EnglishGrammaticalRelations.NEGATION_MODIFIER) != null){
                pol = Polarity.Type.NEGATIVE;
            }
            
            // If there is a modal verb as a modifier of the head verb, make it a possibility modality type
            Set<IndexedWord> auxs = sentenceSemGraph.getChildrenWithReln(relHead, EnglishGrammaticalRelations.AUX_MODIFIER);
            if (!auxs.isEmpty()){
                for (IndexedWord w: auxs){
                    if (w.tag().equals(POS_TAG.MD)){
                        mod = Modality.Type.POSSIBILITY;
                    }
                }
            }
        }
        else if (Modality.VERB_POSSIBILITY.contains(relHead.lemma().toLowerCase())){
            mod = Modality.Type.POSSIBILITY;
            
            // If the head verb of the relation is negated, set polarity to NEGATIVE
            if (sentenceSemGraph.getChildWithReln(relHead, EnglishGrammaticalRelations.NEGATION_MODIFIER) != null){
                pol = Polarity.Type.NEGATIVE;
            }
            
            // If there is a modal verb as a modifier of the head verb, make it a possibility modality type
            Set<IndexedWord> auxs = sentenceSemGraph.getChildrenWithReln(relHead, EnglishGrammaticalRelations.AUX_MODIFIER);
            if (!auxs.isEmpty()){
                for (IndexedWord w: auxs){
                    if (w.tag().equals(POS_TAG.MD)){
                        mod = Modality.Type.POSSIBILITY;
                    }
                }
            }
        }
        
        
        // If a predicate is found
        List<SemanticGraphEdge> nsubjs;
        List<IndexedWord> nsubjChildren;
        if (mod != null){    
            // Stop searching if there's no verb in the object
            if (!CoreNLPUtils.verbInList(object.getWordList())){
                return false;
            }
                        
            // Get the subject relationships
            nsubjs = this.sentenceSemGraph.findAllRelns(EnglishGrammaticalRelations.NOMINAL_SUBJECT);
            nsubjs.addAll(this.sentenceSemGraph.findAllRelns(EnglishGrammaticalRelations.CLAUSAL_SUBJECT));
            nsubjs.addAll(this.sentenceSemGraph.findAllRelns(EnglishGrammaticalRelations.SUBJECT));
            nsubjs.addAll(this.sentenceSemGraph.findAllRelns(EnglishGrammaticalRelations.CLAUSAL_COMPLEMENT));
            
            nsubjChildren = new ArrayList<IndexedWord>();
            for (SemanticGraphEdge e: nsubjs){
                nsubjChildren.add(e.getDependent());
            }
                   
            // Iterate through the subjects
            for (IndexedWord child: nsubjChildren){
                // Process only the ones that have verbs in the object
                if (CoreNLPUtils.verbInList(object.getWordList()) && object.getWordList().contains(child)){   
                    SemanticGraph objSg = CoreNLPUtils.getSubgraphFromWords(this.sentenceSemGraph, object.getWordList());
                    this.generatePropositionsWithAttribution(clausieObj, objSg, new Attribution(subject, pol, mod, 
                                                                                      relation.getRoot().lemma()));
                    attributionDetected = true;
                }
            }
        }
        
        return attributionDetected;
    }
    
    /**
     * Given a ClausIE object, semantic graph object and a attribution, make new extractions from the object, 
     * add them in the list of propositions and add the attribution as well.
     * 
     * @param clausieObj: ClausIE object (reusable variable)
     * @param objSg: semantic graph object of the object
     * @param s: the attribution
     */
    public void generatePropositionsWithAttribution(ClausIE clausieObj, SemanticGraph objSg, Attribution s){
        // New clausie object
        clausieObj.clear();
        clausieObj.setSemanticGraph(objSg);
        clausieObj.detectClauses();
        clausieObj.generatePropositions(clausieObj.getSemanticGraph());
        
        // Reusable variable for annotated phrases
        AnnotatedPhrase aPhrase = new AnnotatedPhrase();
        
        for (Clause c: clausieObj.getClauses()){
            for (Proposition p: c.getPropositions()){
                // Add the proposition from ClausIE to the list of propositions of MinIE
                ObjectArrayList<AnnotatedPhrase> prop = new ObjectArrayList<AnnotatedPhrase>();
                for (int i = 0; i < p.getConstituents().size(); i++){
                    aPhrase = new AnnotatedPhrase(p.getConstituents().get(i));
                    aPhrase.detectQuantities(this.sentenceSemGraph, i);
                    aPhrase.annotateQuantities(i);
                    prop.add(aPhrase);
                }
                if (this.pruneAnnotatedProposition(prop))
                    continue;
                AnnotatedProposition aProp = new AnnotatedProposition(prop, new Attribution(s));
                this.pushWordsToRelation(aProp);
                
                this.propositions.add(aProp);
                this.propsWithAttribution.add(PhraseUtils.listOfAnnotatedPhrasesToString(prop));
            }
        }
    }
    
    public void pushWordsToRelationsInPropositions() {
        for (int i = 0; i < this.propositions.size(); i++) {
            this.pushWordsToRelation(this.propositions.get(i));
        }
    }
    
    /**
     * Given a ClausIE object, set the prepositions from ClausIE to MinIE (don't annotate neg. and poss.)
     * While assigning the propositions, these are things that are done:
     *  * detect attributions
     *  * push words to the relation (if possible)
     * @param clausie: clausie object containing clause types, propositions, sentence dependency parse, ... 
     */
    public void setPropositions(ClausIE clausie){
        // Attribution detection flag + set of strings for propositions with attribution 
        boolean attributionDetected = false;
        this.propsWithAttribution = new ObjectOpenHashSet<String>(); 
        StringBuffer sb = new StringBuffer();
        
        // Set the sentence, make the implicit extractions from it, and add them to the list of propositions
        this.sentence = new ObjectArrayList<IndexedWord> (clausie.getSemanticGraph().vertexListSorted());
        this.originalSentence = this.sentence;
        ImplicitExtractions extractions = new ImplicitExtractions(this.sentence, this.sentenceSemGraph);
        extractions.generateImplicitExtractions();
        int id = 0;
        for (AnnotatedProposition aProp: extractions.getImplicitExtractions()) {
            id++;
            aProp.setId(id);
            this.propositions.add(aProp);
        }
        
        // Set the propositions extracted from ClausIE to MinIE
        for (Clause clause: clausie.getClauses()){
            for (Proposition proposition: clause.getPropositions()){
                id++;
                // If a attribution is detected, add the content of the proposition to the list
                attributionDetected = this.detectAttribution(proposition);                
                //if (attributionDetected) {
                //    propsWithAttribution.add(proposition.object().getWords());
                //}
                
                // Don't add the proposition if an attribution is detected or its content has an attribution already 
                if (attributionDetected || this.propsWithAttribution.contains(proposition.propositionToString()))
                    continue;
                
                // Add the proposition from ClausIE to the list of propositions of MinIE
                ObjectArrayList<AnnotatedPhrase> prop = new ObjectArrayList<AnnotatedPhrase>();
                for (int i = 0; i < proposition.getConstituents().size(); i++){
                    AnnotatedPhrase aPhrase = new AnnotatedPhrase(proposition.getConstituents().get(i));
                    aPhrase.detectQuantities(clausie.getSemanticGraph(), i);
                    aPhrase.annotateQuantities(i);
                    prop.add(aPhrase);
                }
                
                // Prune proposition if needed
                if (this.pruneAnnotatedProposition(prop)){
                    continue;
                }
                
                //Annotated proposition
                AnnotatedProposition aProp = new AnnotatedProposition(prop, id);
                // Push words to relation
                this.pushWordsToRelation(aProp);
                
                // Handle possessives
                // TODO: check this out
                //this.processPoss(prop);
                
                this.propositions.add(aProp);
            }
        }
        
        // Remove proposiions which have no attributions, but they have duplicate propositions having an attribution
        // TODO: temporary solution, make this in removeDuplicates()
        ObjectArrayList<AnnotatedProposition> delProps = new ObjectArrayList<AnnotatedProposition>();
        ObjectArrayList<Attribution> delAttributions = new ObjectArrayList<>();
        ObjectOpenHashSet<String> propWithAttributions = new ObjectOpenHashSet<String>();
        String thisProp;
        for (int i = 0; i < this.propositions.size(); i++){
            thisProp = PhraseUtils.listOfAnnotatedPhrasesToString(this.propositions.get(i).getTriple());
            // Remove proposiions which have no attributions, but they have duplicate propositions having an attribution
            if (this.propsWithAttribution.contains(thisProp)){
                if (this.propositions.get(i).getAttribution().getAttributionPhrase() == null){
                    delAttributions.add(this.propositions.get(i).getAttribution());
                    delProps.add(this.propositions.get(i));
                } else {
                    sb.append(thisProp);
                    sb.append(SEPARATOR.SPACE);
                    sb.append(this.propositions.get(i).getAttribution().toString());
                    if (propWithAttributions.contains(sb.toString())){
                        delAttributions.add(this.propositions.get(i).getAttribution());
                        delProps.add(this.propositions.get(i));
                    }
                    else {
                        propWithAttributions.add(sb.toString());
                    }
                    sb.setLength(0);
                }
            }
        }
        this.propositions.removeAll(delProps);
    }
    
    /**
     * 
     * @param clausie
     */
    public void setPropositionsWithoutAnnotations(ClausIE clausie) {
        // Set the sentence, make the implicit extractions from it, and add them to the list of propositions
        this.sentence = new ObjectArrayList<IndexedWord> (clausie.getSemanticGraph().vertexListSorted());
        this.originalSentence = this.sentence;
        ImplicitExtractions extractions = new ImplicitExtractions(this.sentence, this.sentenceSemGraph);
        extractions.generateImplicitExtractions();
        int id = 0;
        for (AnnotatedProposition aProp: extractions.getImplicitExtractions()) {
            id++;
            aProp.setId(id);
            this.propositions.add(aProp);
        }
        
        // Set the propositions extracted from ClausIE to MinIE
        for (Clause clause: clausie.getClauses()){
            for (Proposition proposition: clause.getPropositions()){
                id++;
                
                // Add the proposition from ClausIE to the list of propositions of MinIE
                ObjectArrayList<AnnotatedPhrase> prop = new ObjectArrayList<AnnotatedPhrase>();
                for (int i = 0; i < proposition.getConstituents().size(); i++){
                    prop.add(new AnnotatedPhrase(proposition.getConstituents().get(i)));
                }
                
                //Annotated proposition
                AnnotatedProposition aProp = new AnnotatedProposition(prop, id);
                this.pushWordsToRelation(aProp);
                
                this.propositions.add(aProp);
            }
        }
        
        this.removeDuplicates();
    }
    
    /**
     * Process possessives in the object.
     * If we have ("SUBJ", "REL", "NP_1 POS NP_2"), then: ("SUBJ", "REL + NP_1 + of", "NP_2")
     * @param prop: proposition (list of annotated phrases)
     */
    public void processPoss(ObjectArrayList<AnnotatedPhrase> prop){
        // If there's no object (clause type SV), return
        if (prop.size() < 3)
            return;
        
        AnnotatedPhrase object = prop.get(2);
        AnnotatedPhrase rel = prop.get(1);
        TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.T_NP_POS_NP);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(object.getWordCoreLabelList());
        
        int posIndex = -1;
        
        while (tMatcher.find()){         
            List<CoreMap> match = tMatcher.groupNodes();
            
            // Check if the first/last word of the match is the first/last word of the object
            CoreLabel firstWord = new CoreLabel(match.get(0));
            CoreLabel lastWord = new CoreLabel(match.get(match.size() - 1));
            boolean check = false;
            if (firstWord.index() == object.getWordList().get(0).index()){
                if (lastWord.index() == object.getWordList().get(object.getWordList().size() - 1).index()){
                    check = true;
                }
            }
            if (!check) break;
            
            for (CoreMap cm: match){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.tag().equals(POS_TAG.POS) && (cl.ner().equals(NE_TYPE.NO_NER))){
                    posIndex = object.getWordCoreLabelList().indexOf(cl);
                    break;
                }
            }
        }
        
        if (posIndex > -1){
            IndexedWord of = new IndexedWord();
            of.setOriginalText("of");
            of.setLemma("of");
            of.setWord("of");
            of.setTag("IN");
            of.setNER("O");
            of.setIndex(-1);
            
            ObjectArrayList<IndexedWord> pushedWords = new ObjectArrayList<>();
            object.removeWordFromList(posIndex);
            for (int i = posIndex; i < object.getWordList().size(); i++){
                pushedWords.add(object.getWordList().get(i));
            }
            rel.addWordsToList(pushedWords);
            rel.addWordToList(of);
            object.removeWordsFromList(pushedWords);
        }
    }
    
    /**
     * Given an object phrase, check if it has infinitive verbs modifying a noun phrase or a named entity. 
     * If yes, then return "true", else -> "false"
     * @param object: the object phrase
     * @return
     */
    public boolean pushInfinitiveVerb(Phrase object){
        TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.T_TO_VB_NP_NER);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(object.getWordCoreLabelList());
        
        while (tMatcher.find()){         
            CoreLabel firstWordMatch = new CoreLabel(tMatcher.groupNodes().get(0));
            if (firstWordMatch.index() == object.getWordList().get(0).index()){
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if the adverb(s) from the object should be pushed to the relation (if the adverb is followed by preposition 
     * or 'to).
     * @param object: a phrase, the object of the proposition
     * @return true, if an adverb is followed by a preposition or "to"
     */
    public boolean pushAdverb(Phrase object){        
        TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.T_RB_OPT_IN_TO_OPT);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(object.getWordCoreLabelList());
        while (tMatcher.find()){         
            CoreLabel firstWordMatch = new CoreLabel(tMatcher.groupNodes().get(0));
            if (firstWordMatch.index() == object.getWordList().get(0).index() && 
                    object.getWordList().get(0).ner().equals(NE_TYPE.NO_NER)){
                return true;
            }
        }
        return false;
    }
    
    /** 
     * Given a proposition (list of annotated phrases), push words from objects to the relation if possible
     * @param prop: the proposition (list of annotated phrases)
     */
    public void pushWordsToRelation(AnnotatedProposition prop){
        IndexedWord firstObjectWord = null;
        ObjectArrayList<IndexedWord> pushWords = new ObjectArrayList<>(); 
        
        if (prop.getTriple().size() > 2 && prop.getObject().getWordList().size() > 0){
            firstObjectWord = prop.getObject().getWordList().get(0);

            while ((firstObjectWord != null) && 
                    (firstObjectWord.tag().equals(POS_TAG.IN) || firstObjectWord.tag().equals(POS_TAG.RB) || 
                     firstObjectWord.tag().equals(POS_TAG.WRB)) &&
                     prop.getObject().getWordList().size() > 1){
            
                // If it's an adverb, check if the adverb should be pushed
                if (firstObjectWord.tag().equals(POS_TAG.RB) && !this.pushAdverb(prop.getObject())){
                    break;
                }
                else
                    pushWords.add(firstObjectWord);
                
                // Add the word to the end of the relation, and remove it from the object
                prop.getRelation().addWordsToList(pushWords);
                prop.getObject().removeWordsFromList(pushWords);
                    
                if (prop.getObject().getWordList().size() > 0){
                    firstObjectWord = prop.getObject().getWordList().get(0);
                    pushWords.clear();
                }
                else
                    firstObjectWord = null;    
            }
            
            // If we have TO+ VB* .* NP .* => push TO+ VB* to the relation
            TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.T_TO_VP_IN);
            TokenSequenceMatcher tMatcher = tPattern.getMatcher(prop.getObject().getWordCoreLabelList());
            while (tMatcher.find()){
                List<CoreMap> matches = tMatcher.groupNodes();
                
                // Check if the first word of the matches is the same as the first object word
                CoreLabel firstWord = new CoreLabel(matches.get(0));
                if (firstWord.index() != prop.getObject().getWordList().get(0).index())
                    break;
                
                CoreLabel lastWord = new CoreLabel(matches.get(matches.size() - 1));
                for (CoreMap cm: matches){
                    CoreLabel cl = new CoreLabel(cm);
                    if (cl.ner().equals(NE_TYPE.NO_NER)){
                        // If adverb is not followed by preposition, don't push it
                        if (CoreNLPUtils.isAdverb(cl.tag())){
                            if (cl.index() == lastWord.index()){
                                break;
                            }
                        }
                        // Don't push the last word of the object
                        if (prop.getObject().getWordList().get(prop.getObject().getWordList().size() -1).index() == cl.index())
                            break;
                        // Add the pushed words to the list
                        pushWords.add(new IndexedWord(cl));
                    } else {
                        break;
                    }
                }
                
                // Push the words, clear the list
                prop.getRelation().addWordsToList(pushWords);
                prop.getObject().removeWordsFromList(pushWords);
                pushWords.clear();
            }
            
            
            // After the pushing of the words is done, check for PPs with one of their NPs being a NER
            pushWords.clear();
            tPattern = TokenSequencePattern.compile(REGEX.T_NP_IN_OPT_DT_RB_JJ_OPT_ENTITY);
            tMatcher = tPattern.getMatcher(prop.getObject().getWordCoreLabelList());
            while (tMatcher.find()){
                List<CoreMap> matches = tMatcher.groupNodes();
                CoreLabel firstWord = new CoreLabel(matches.get(0));
                if (firstWord.index() != prop.getObject().getWordList().get(0).index())
                    continue;
                
                CoreLabel prep = new CoreLabel();
                for (CoreMap cm: matches){
                    CoreLabel cl = new CoreLabel(cm);
                    if (!cl.tag().equals(POS_TAG.IN) && !cl.tag().equals(POS_TAG.TO)){
                        pushWords.add(new IndexedWord(cl));
                    } else {
                        pushWords.add(new IndexedWord(cl));
                        prep = cl;
                        break;
                    }
                }
                if (prep.ner().equals(NE_TYPE.NO_NER)){
                    // Add the word to the end of the relation, and remove it from the object
                    prop.getRelation().addWordsToList(pushWords);
                    prop.getObject().removeWordsFromList(pushWords);
                    pushWords.clear();
                }
            }
            
            //TODO: merge this with the previous pushing rules
            // Check if we have NP_1 IN NP_2, but nothing else (no additional prepositions). Push NP_1 to relation
            if (CoreNLPUtils.countPrepositionsInList(prop.getObject().getWordList()) == 1){
                pushWords.clear();
                int prepIndex = -1;
                for (int i = 0; i < prop.getObject().getWordList().size(); i++){
                    if (prop.getObject().getWordList().get(i).tag().equals(POS_TAG.IN) && 
                            prop.getObject().getWordList().get(i).ner().equals(NE_TYPE.NO_NER)){
                        if (prop.getObject().getWordList().get(i).index() == 
                                prop.getObject().getWordList().get(prop.getObject().getWordList().size() - 1).index()){
                            break;
                        }
                        prepIndex = i;
                        break;
                    }
                }
                for (int i = 0; i <= prepIndex; i++){
                    pushWords.add(prop.getObject().getWordList().get(i));
                }
                // Add the word to the end of the relation, and remove it from the object
                prop.getRelation().addWordsToList(pushWords);
                prop.getObject().removeWordsFromList(pushWords);
                pushWords.clear();
            }
        }
    }
    
    /**
     * Because of the annotations sometimes we get duplicates. Prune-out the duplicates
     * TODO
     */
    public void removeDuplicates(){
        ObjectOpenHashSet<String> propStrings = new ObjectOpenHashSet<>();
        ObjectOpenHashSet<String> propStringPS = new ObjectOpenHashSet<>();

        ObjectArrayList<AnnotatedProposition> remProps = new ObjectArrayList<>();
        String propString;
        for (AnnotatedProposition prop: this.propositions){
            if (prop.getModality().getModalityType() == Modality.Type.POSSIBILITY) {
                propStringPS.add(prop.propositionWordsToString());
            }
            propString = prop.toString();
            if (propStrings.contains(propString))
                remProps.add(prop);
            else
                propStrings.add(propString);
        }
        
        // Remove PS duplicates TODO: optimize this
        for (AnnotatedProposition prop: this.propositions) {
            if (prop.getModality().getModalityType() == Modality.Type.CERTAINTY) {
                if (propStringPS.contains(prop.propositionWordsToString())) {
                    remProps.add(prop);
                }
            }
        }
        
        // Also, remove the ones with empty object
        for (int i = 0; i < this.propositions.size(); i++){
            if (this.propositions.get(i).getSubject().getWordList().isEmpty())
                remProps.add(this.propositions.get(i));
            
            if (this.propositions.get(i).getTriple().size() == 3)
                if (this.propositions.get(i).getObject().getWordList().isEmpty())
                    remProps.add(this.propositions.get(i));
        }
        
        this.propositions.removeAll(remProps); 
    }
    
    /**
     * Given a proposition, check if it should be pruned or not.
     * @param proposition
     * @return true, if the proposition should be pruned, false otherwise
     */
    private boolean pruneAnnotatedProposition(ObjectArrayList<AnnotatedPhrase> proposition){
        AnnotatedPhrase subj = proposition.get(0);
        AnnotatedPhrase rel = proposition.get(1);

        // If there is no verb in the relation, prune
        // TODO: check why this is happening! In some of these cases, the verb gets deleted for some reason.
        // This happens when CCs are being processed. Empty relations too
        if (!CoreNLPUtils.hasVerb(rel.getWordList()))
            return true;

        // Empty subject
        if (subj.getWordList().isEmpty())
            return true;

        if (proposition.size() == 3){
            AnnotatedPhrase obj = proposition.get(2);
            // Check if the object is empty (shouldn't happen, but just in case)
            if (obj.getWordList().isEmpty())
                return true;

            // The last word of the object
            IndexedWord w = obj.getWordList().get(obj.getWordList().size()-1);

            // If the last word is preposition
            if (w.tag().equals(POS_TAG.IN) && w.ner().equals(NE_TYPE.NO_NER))
                return true;

            // When the object is consisted of one preposition
            if (obj.getWordList().size() == 1){
                // If the object is just one preposition - prune
                if (w.tag().equals(POS_TAG.IN) || w.tag().equals(POS_TAG.TO)){
                    return true;
                }
            }
            // When the object ends with one of the POS tags: WDT, WP$, WP or WRB
            if (w.tag().equals(POS_TAG.WDT) || w.tag().equals(POS_TAG.WP) ||
                    w.tag().equals(POS_TAG.WP_P) || w.tag().equals(POS_TAG.WRB)){
                return true;
            }

            // Prune if clause modifier detected
            if (this.detectClauseModifier(proposition)){
                return true;
            }

            // Prune if there are NERs on both sides of "be" relation
            // TODO: do this for implicit extractions only?
            if ((rel.getWordList().size() == 1)) {
                if (rel.getWordList().get(0).lemma().equals("be")) {
                    if (subj.isOneNER() && obj.isOneNER()) {
                        if (!obj.getWordList().get(0).ner().equals(NE_TYPE.MISC)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
    
    /**
     * Given an annotated proposition, check if it contains a clause modifier as an object. If so, return 'true', else
     * return 'false'
     * @param proposition: annotated proposition
     * @return: 'true' if the object is a clause modifier; 'false' otherwise
     */
    public boolean detectClauseModifier(ObjectArrayList<AnnotatedPhrase> proposition){
        /*for (IndexedWord word: proposition.get(1).getWordList()){
            if (word.index() == -2)
                continue;
            if (this.sentenceSemGraph.getParent(word) != null){
                SemanticGraphEdge edge = this.sentenceSemGraph.getEdge(this.sentenceSemGraph.getParent(word), word);
                if ((edge.getRelation() == EnglishGrammaticalRelations.SUBJECT) || 
                    (edge.getRelation() == EnglishGrammaticalRelations.NOMINAL_SUBJECT) ||
                    (edge.getRelation() == EnglishGrammaticalRelations.CLAUSAL_SUBJECT) ||
                    (edge.getRelation() == EnglishGrammaticalRelations.NOMINAL_PASSIVE_SUBJECT)){
                    return true;
                }
            }
        }*/
        
        if (CoreNLPUtils.verbInList(proposition.get(2).getWordList())){
            for (IndexedWord word: proposition.get(2).getWordList()){
                if (this.sentenceSemGraph.getParent(word) != null){
                    SemanticGraphEdge edge = this.sentenceSemGraph.getEdge(this.sentenceSemGraph.getParent(word), word);
                    if ((edge.getRelation() == EnglishGrammaticalRelations.SUBJECT) || 
                        (edge.getRelation() == EnglishGrammaticalRelations.NOMINAL_SUBJECT) ||
                        (edge.getRelation() == EnglishGrammaticalRelations.CLAUSAL_SUBJECT) ||
                        (edge.getRelation() == EnglishGrammaticalRelations.NOMINAL_PASSIVE_SUBJECT)){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /** Sets the polarity of each annotated proposition **/
    public void setPolarity(){
        Polarity pol = new Polarity();
        // Set polarity according to relations only
        for (int i = 0; i < this.propositions.size(); i++){
            // In some cases, there's only one word, in which case we don't drop anything
            if (this.propositions.get(i).getRelation().getWordList().size() == 1)
                continue;
            
            pol = Polarity.getPolarity(this.propositions.get(i).getRelation(), this.sentenceSemGraph);
            this.propositions.get(i).setPolarity(pol);
            
            // If the polarity is negative, drop the negative words
            this.propositions.get(i).getRelation().removeWordsFromList(pol.getNegativeWords());
        }
    } 
    
    /** Set the modality for all annotated propositions */
    public void setModality(){
        Modality mod = new Modality();
        // Set modality according to relations only
        for (int i = 0; i < this.propositions.size(); i++){
            // In some cases, there's only one word, in which case we don't drop anything
            if (this.propositions.get(i).getRelation().getWordList().size() == 1)
                continue;
            mod = Modality.getModality(this.propositions.get(i).getRelation(), this.sentenceSemGraph);
            this.propositions.get(i).setModality(mod);
            
            // If the modality is poss/cert, drop those words
            this.propositions.get(i).getRelation().removeWordsFromList(mod.getPossibilityWords());
            this.propositions.get(i).getRelation().removeWordsFromList(mod.getCertaintyWords());
        }
    }
    
    public void setSemanticGraph(SemanticGraph sg){
        this.sentenceSemGraph = sg;
    }
    
    /**
     * Adding words to constituents
     * @param i: the index of the constituent
     * @param word: the indexed word to be added
     */
    public void addWordToRelation(int i, IndexedWord word){
        this.propositions.get(i).getRelation().addWordToList(word);
    }
    
    /**
     * Remove the first word from object in proposition 'i'
     * @param i: index of the proposition
     */
    public void removeFirstWordFromObject(int i){
        this.propositions.get(i).getObject().removeWordFromList(0);
    }
    
    /** Dictionary mode minimization **/
    public void minimizeDictionaryMode(ObjectOpenHashSet<String> collocations){
        for (int i = 0; i < this.propositions.size(); i++){
            SubjDictionaryMinimization.minimizeSubject(this.getSubject(i), this.sentenceSemGraph, collocations);
            RelDictionaryMinimization.minimizeRelation(this.getRelation(i), this.sentenceSemGraph, collocations);
            ObjDictionaryMinimization.minimizeObject(this.getObject(i), this.sentenceSemGraph, collocations);
        }
        this.pushWordsToRelationsInPropositions();
    }
    
    /** Safe mode minimization **/
    public void minimizeSafeMode(){
        for (int i = 0; i < this.propositions.size(); i++){
            SubjSafeMinimization.minimizeSubject(this.getSubject(i), this.sentenceSemGraph);
            RelSafeMinimization.minimizeRelation(this.getRelation(i), this.sentenceSemGraph);
            ObjSafeMinimization.minimizeObject(this.getObject(i), this.sentenceSemGraph);
        }
        this.pushWordsToRelationsInPropositions();
    }
    
    /** Aggressive mode minimization **/
    public void minimizeAggressiveMode() {
        for (int i = 0; i < this.propositions.size(); i++) {
            SubjAggressiveMinimization.minimizeSubject(this.getSubject(i), this.sentenceSemGraph);
            RelAggressiveMinimization.minimizeRelation(this.getRelation(i), this.sentenceSemGraph);
            ObjAggressiveMinimization.minimizeObject(this.getObject(i), this.sentenceSemGraph);
        }
        this.pushWordsToRelationsInPropositions();
    }
}
