package de.uni_mannheim.minie.minimize.relation;

import java.util.HashSet;
import java.util.Set;

import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.constant.REGEX;
import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.annotation.Quantity;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

/**
 * @author Kiril Gashteovski
 */
public class RelAggressiveMinimization {
    /**
     * Always minimize the subject towards the root word
     * @param relation: relation phrase
     * @param sg: sentence semantic graph (dependency parse graph)
     */
    public static void minimizeRelation(AnnotatedPhrase relation, SemanticGraph sg){
        // Don't minimize if the phrase contains one word or no  words (rare cases)
        if (relation.getWordList() == null || relation.getWordList().size() <= 1){
            return;
        }
        
        // Do safe minimization first
        RelSafeMinimization.minimizeRelation(relation, sg);
        
        // List of words to be dropped
        ObjectArrayList<IndexedWord> dropWords = new ObjectArrayList<>();
        
        // Drop some type of modifiers 
        Set<GrammaticalRelation> excludeRels = new HashSet<>();
        excludeRels.add(EnglishGrammaticalRelations.ADVERBIAL_MODIFIER);
        excludeRels.add(EnglishGrammaticalRelations.ADJECTIVAL_MODIFIER);
        excludeRels.add(EnglishGrammaticalRelations.DETERMINER);
        excludeRels.add(EnglishGrammaticalRelations.PREDETERMINER);
        excludeRels.add(EnglishGrammaticalRelations.NUMERIC_MODIFIER);
        excludeRels.add(EnglishGrammaticalRelations.NUMBER_MODIFIER);
        excludeRels.add(EnglishGrammaticalRelations.POSSESSION_MODIFIER);
        excludeRels.add(EnglishGrammaticalRelations.POSSESSIVE_MODIFIER);
        excludeRels.add(EnglishGrammaticalRelations.QUANTIFIER_MODIFIER);
        excludeRels.add(EnglishGrammaticalRelations.TEMPORAL_MODIFIER);
        excludeRels.add(EnglishGrammaticalRelations.NP_ADVERBIAL_MODIFIER);
        //excludeRels.add(EnglishGrammaticalRelations.AUX_MODIFIER);
        for (IndexedWord w: relation.getWordList()) {
            // Skip the words that were included afterwards (not part of the DP)
            if (w.index() < 0)
                continue;
            
            // Get the relevant modifiers to be dropped (their modifiers as well)
            Set<IndexedWord> modifiers = sg.getChildrenWithRelns(w, excludeRels);
            for (IndexedWord m: modifiers) {
                ObjectArrayList<IndexedWord> subModifiers = CoreNLPUtils.getSubTreeSortedNodes(m, sg, null);
                for (IndexedWord sm: subModifiers)
                    if (!sm.tag().equals(POS_TAG.IN))
                        dropWords.add(sm);
            }
            dropWords.addAll(modifiers);
            
            // Drop quantities
            if (w.ner().equals(Quantity.ST_QUANTITY)) 
                dropWords.add(w);
        }
        relation.getWordList().removeAll(dropWords);
        // add words to dropped word list
        relation.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, dropWords));
        relation.addDroppedWords(dropWords);
        dropWords.clear();
        
        // If [IN|TO] .* [IN|TO] => drop [IN|TO] .*, i.e. -> drop PP attachments
        TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.T_PREP_ALL_PREP);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(relation.getWordCoreLabelList());
        ObjectArrayList<IndexedWord> matchedWords = new ObjectArrayList<>();
        while (tMatcher.find()){
            matchedWords = CoreNLPUtils.listOfCoreMapWordsToIndexedWordList(tMatcher.groupNodes());
            for (int i = 0; i < matchedWords.size(); i++) {
                if (matchedWords.get(i).tag().equals(POS_TAG.IN) || matchedWords.get(i).tag().equals(POS_TAG.TO)) {
                    if (i == 0) {
                        if (matchedWords.get(i).tag().equals(POS_TAG.TO) && CoreNLPUtils.isVerb(matchedWords.get(i+1).tag()))
                            break;
                        dropWords.add(matchedWords.get(i));
                    } else break;
                } else {
                    dropWords.add(matchedWords.get(i));
                }
            }
        }
        relation.getWordList().removeAll(dropWords);
        // add words to dropped word list
        relation.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, dropWords));
        relation.addDroppedWords(dropWords);
        dropWords.clear();
        
        // TODO: if QUANT + NP + IN => drop "QUANT + NP" ?
        
        // If VB_1+ TO VB_2 => drop VB_1+ TO .*
        tPattern = TokenSequencePattern.compile(REGEX.T_VB_TO_VB);
        tMatcher = tPattern.getMatcher(relation.getWordCoreLabelList());
        matchedWords = new ObjectArrayList<>();
        while (tMatcher.find()){
            matchedWords = CoreNLPUtils.listOfCoreMapWordsToIndexedWordList(tMatcher.groupNodes());
            for (int i = 0; i < matchedWords.size(); i++) {
                if (matchedWords.get(i).tag().equals(POS_TAG.TO)) {
                    dropWords.add(matchedWords.get(i));
                    break;
                } else {
                    dropWords.add(matchedWords.get(i));
                }
            }
        }
        relation.getWordList().removeAll(dropWords);
        // add words to dropped word list
        relation.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, dropWords));
        relation.addDroppedWords(dropWords);
        dropWords.clear();
        
        // Drop auxilaries
        for (IndexedWord w: relation.getWordList()) {
            if (w.index() < 0)
                continue;
            Set<IndexedWord> modifiers = sg.getChildrenWithReln(w, EnglishGrammaticalRelations.AUX_MODIFIER);
            for (IndexedWord m: modifiers) {
                ObjectArrayList<IndexedWord> subModifiers = CoreNLPUtils.getSubTreeSortedNodes(m, sg, null);
                for (IndexedWord sm: subModifiers)
                    dropWords.add(sm);
            }
            dropWords.addAll(modifiers);
        }
        relation.getWordList().removeAll(dropWords);
        // add words to dropped word list
        relation.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, dropWords));
        relation.addDroppedWords(dropWords);
        dropWords.clear();
        
        // Drop noun modifiers with different NERs
        for (IndexedWord w: relation.getWordList()) {
            if (w.index() < 0)
                continue;
            Set<IndexedWord> modifiers = sg.getChildrenWithReln(w, EnglishGrammaticalRelations.NOUN_COMPOUND_MODIFIER);
            for (IndexedWord mw: modifiers) {
                if (!w.ner().equals(mw.ner())) {
                    dropWords.add(mw);
                    dropWords.addAll(CoreNLPUtils.getSubTreeSortedNodes(mw, sg, null));
                }
            }
        }
        relation.getWordList().removeAll(dropWords);
        // add words to dropped word list
        relation.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, dropWords));
        relation.addDroppedWords(dropWords);
        dropWords.clear();
    }
}
