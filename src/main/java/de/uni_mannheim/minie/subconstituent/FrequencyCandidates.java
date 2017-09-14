package de.uni_mannheim.minie.subconstituent;

import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.constant.REGEX;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.semgraph.SemanticGraph;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * @author Kiril Gashteovski
 */
public class FrequencyCandidates {
    /** The phrase from which the frequency candidates are generated from **/
    private Phrase phrase;
    /** The sentence semantic graph **/
    private SemanticGraph sg;
    /** The sub constituents' candidates from the phrase **/
    private ObjectOpenHashSet<String> candidates;
    
    /** Default constructor **/
    public FrequencyCandidates(){
        this.phrase = new Phrase();
        this.sg = new SemanticGraph();
        this.candidates = new ObjectOpenHashSet<>();
    }
    
    /** Parametric constructor **/
    public FrequencyCandidates(Phrase p, SemanticGraph sentenceSg){
        this.phrase = p;
        this.sg = sentenceSg;
        this.phrase.detectRoot(this.sg);
        this.candidates = new ObjectOpenHashSet<>();
    }
    
    /** Generate the frequency candidates by default: 
     *  1) the whole phrase itself
     *  2) the root word
     *  3) the chained words from the root 
     *  4) the chained sub-constituent candidates
     **/
    public void generateDefaultFreqCandidates(){
        // Stoping conditions (when the phrase is just one word or no words at all (sometimes it happens)
        if (this.phrase.getWordList().size() == 0){
            return;
        }
        else if (this.phrase.getWordList().size() == 1){
            this.candidates.add(this.phrase.getWordList().get(0).lemma().toLowerCase());
            return;
        }
        
        // 1) the whole phrase itself
        this.candidates.add(CoreNLPUtils.listOfWordsToLemmaString(this.phrase.getWordList()).toLowerCase());
        
        // 2) the root word
        this.candidates.add(this.phrase.getRoot().lemma().toLowerCase());
        
        // 3) the chained words from the root
        ObjectArrayList<IndexedWord> chainedRootWords = 
                CoreNLPUtils.getChainedWords(this.phrase.getRoot(), this.phrase.getWordList());
        this.candidates.add(CoreNLPUtils.listOfWordsToLemmaString(chainedRootWords).toLowerCase());
    }
    
    /** Generate candidates for each noun phrase within the phrase **/
    public void generateNounPhraseFreqCandidates(){
        SubConstituent sc = new SubConstituent(this.sg);
        
        // Generate candidates for [DT|RB|JJ]+ NN+
        TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.T_DT_RB_JJ_PR_NN);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        this.generateCandidatesFromTokenRegexMatch(tMatcher, sc);
    }
    
    /**
     * Given a token sequence matcher for regular expressions for sequences over tokens, get the sub-constituents and
     * store them in the sub-constituent object sc
     * @param tMatcher: token sequence matcher for regular expressions for sequences over tokens
     * @param sc: sub-constituent object
     */
    public void generateCandidatesFromTokenRegexMatch(TokenSequenceMatcher tMatcher, SubConstituent sc){
        // The matched list of words and their "root"
        ObjectArrayList<IndexedWord> matchWords;
        IndexedWord matchRoot;
        
        // Given a match, get the subconstituents
        while (tMatcher.find()){         
            matchWords = CoreNLPUtils.listOfCoreMapWordsToIndexedWordList(tMatcher.groupNodes());
            matchRoot = CoreNLPUtils.getRootFromWordList(this.sg, matchWords);
            sc.setRoot(matchRoot);
            sc.setWords(matchWords);
            sc.generateSubConstituentsFromLeft();
            for (String cand: sc.getStringSubConstituents()){
                this.candidates.add(cand);
            }
            sc.clearSubConstituentsAndCandidates();
        }
    }
    
    // Getters
    public Phrase getPhrase(){
        return this.phrase;
    }
    public SemanticGraph getSentenceSemGraph(){
        return this.sg;
    }
    public ObjectOpenHashSet<String> getCandidates(){
        return this.candidates;
    }
    
    // Setters
    public void setPhrase(Phrase p){
        this.phrase = p;
    }
    public void setSentenceSemGraph(SemanticGraph sentenceSg){
        this.sg = sentenceSg;
    }
    public void setCandidates(ObjectOpenHashSet<String> cands){
        this.candidates = cands;
    }
    
    /** Clear the frequency candidates object (empty phrase and semantic graph, and clear the list of candidates) **/
    public void clear(){
        this.phrase = new Phrase();
        this.sg = new SemanticGraph();
        this.candidates.clear();
    }
    /** Clear the candidates list **/
    public void clearCandidates(){
        this.candidates.clear();
    }
}
