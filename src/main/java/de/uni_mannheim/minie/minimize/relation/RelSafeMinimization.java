package de.uni_mannheim.minie.minimize.relation;

import java.util.ArrayList;
import java.util.List;

import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.constant.REGEX;
import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.annotation.Polarity;
import de.uni_mannheim.minie.minimize.Minimization;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * @author Kiril Gashteovski
 */
public class RelSafeMinimization {
    /**
     * Minimize only the relations that are considered to have "safe patterns"
     * @param rel: the relation phrase
     * @param sg: semantic graph of the sentence
     */
    public static void minimizeRelation(AnnotatedPhrase rel, SemanticGraph sg){
        // Minimize left/right of the verb 
        minimizationLeftFromVerb(rel, sg);
        minimizationRightFromVerb(rel, sg);
    }
    
    /**
     * Minimize the relations considered to have "safe patterns", and occur from the left of the verb
     * @param rel: the relation phrase
     * @param sg: the semantic graph of the sentence
     */
    private static void minimizationLeftFromVerb(AnnotatedPhrase rel, SemanticGraph sg){
        // Minimization object
        Minimization simp = new Minimization(rel, sg, new ObjectOpenHashSet<String>());
        
        // remWords: list of words to be removed (reusable variable)
        // matchWords: list of matched words from the regex (reusable variable)
        List<CoreMap> remWords = new ArrayList<>();
        List<CoreMap> matchWords = new ArrayList<>(); 
        
        simp.verbPhraseSafeMinimization(remWords, matchWords);
    }
    
    /**
     * Minimize the relations considered to have "safe patterns", and occur from the right of the verb
     * @param rel: the relation phrase
     * @param sg: the semantic graph of the sentence
     */
    private static void minimizationRightFromVerb(AnnotatedPhrase rel, SemanticGraph sg){
        // Minimization object
        Minimization simp = new Minimization(rel, sg, new ObjectOpenHashSet<String>());
        
        // remWords: list of words to be removed (reusable variable)
        // matchWords: list of matched words from the regex (reusable variable)
        List<CoreMap> remWords = new ArrayList<>();
        List<CoreMap> matchWords = new ArrayList<>(); 
        
        // Safe minimization on the noun phrases and named entities within the rel. phrase
        simp.nounPhraseSafeMinimization(remWords, matchWords);
        simp.namedEntitySafeMinimization(remWords, matchWords);
        rel = simp.getPhrase();
        
        // Reusable variables
        ObjectOpenHashSet<IndexedWord> droppedWords = new ObjectOpenHashSet<IndexedWord>();
        ObjectArrayList<IndexedWord> matchedWords = new ObjectArrayList<>();
        ObjectArrayList<IndexedWord> verbs = new ObjectArrayList<>();
        List<IndexedWord> children;
        
        // Flags for checking certain conditions
        boolean containsNEG;
        boolean isAdverb;
        
        // If ^VB+ RB+ VB+ => drop RB+
        TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.T_VB_RB_VB);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(rel.getWordCoreLabelList());
        while (tMatcher.find()){         
            matchedWords = CoreNLPUtils.listOfCoreMapWordsToIndexedWordList(tMatcher.groupNodes());
            // Check if the first word of the matched words is the first word of the relation
            if (matchedWords.get(0).index() != rel.getWordList().get(0).index())
                break;
            
            verbs = CoreNLPUtils.getChainedTagNoNER(rel.getWordList(), 0);
            for (int i = 0; i < matchedWords.size(); i++){
                isAdverb = matchedWords.get(i).tag().equals(POS_TAG.RB);
                containsNEG = Polarity.NEG_WORDS.contains(matchedWords.get(i).lemma().toLowerCase());
                
                if (isAdverb && !containsNEG) {
                    // If the adverb is the head word, don't drop it
                    children = sg.getChildList(rel.getWordList().get(i));
                    children.retainAll(verbs);
                    if (children.size() == 0) {
                        droppedWords.addAll(CoreNLPUtils.getChainedTagNoNER(rel.getWordList(), i));
                    }
                    break;
                }
            }
            
            if (droppedWords.size() > 0){
                rel.removeWordsFromList(droppedWords);
                // add words to dropped word list
                rel.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, droppedWords));
                rel.addDroppedWords(droppedWords);
                droppedWords = new ObjectOpenHashSet<IndexedWord>();
            }
        }
        
        // If ^VB+ RB+ => drop RB+
        tPattern = TokenSequencePattern.compile(REGEX.T_VB_RB);
        tMatcher = tPattern.getMatcher(rel.getWordCoreLabelList());
        while (tMatcher.find()){         
            matchedWords = CoreNLPUtils.listOfCoreMapWordsToIndexedWordList(tMatcher.groupNodes());
            // Check if the first word of the matched words is the first word of the relation
            if (matchedWords.get(0).index() != rel.getWordList().get(0).index())
                break;
            
            verbs = CoreNLPUtils.getChainedTagNoNER(rel.getWordList(), 0);
            for (int i = 0; i < matchedWords.size(); i++){
                isAdverb = matchedWords.get(i).tag().equals(POS_TAG.RB);
                containsNEG = Polarity.NEG_WORDS.contains(matchedWords.get(i).lemma().toLowerCase());

                if (isAdverb && !containsNEG) {
                    // If the adverb is the head word, don't drop it
                    children = sg.getChildList(rel.getWordList().get(i));
                    children.retainAll(verbs);
                    if (children.size() == 0) {
                        droppedWords.addAll(CoreNLPUtils.getChainedTagNoNER(rel.getWordList(), i));
                    }
                    break;
                }
            }
            
            if (droppedWords.size() > 0){
                rel.removeWordsFromList(droppedWords);
                // add words to dropped word list
                rel.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, droppedWords));
                rel.addDroppedWords(droppedWords);
                droppedWords = new ObjectOpenHashSet<IndexedWord>();
            }
        }
    }
}
