package de.uni_mannheim.minie.minimize.subject;

import java.util.HashSet;
import java.util.Set;

import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.constant.REGEX;
import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.annotation.Quantity;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SubjAggressiveMinimization {
    
    /**
     * Always minimize the subject towards the root word
     * @param subject: subject phrase
     * @param sg: semantic graph of the sentence
     */
    public static void minimizeSubject(AnnotatedPhrase subject, SemanticGraph sg){
        // Don't minimize if the phrase contains one word or no  words (rare cases)
        if (subject.getWordList() == null || subject.getWordList().size() <= 1){
            return;
        }
        // Don't minimize if the phrase is a multi word NER or multiple nouns in a sequence
        String seqPosNer = CoreNLPUtils.wordsToPosMergedNerSeq(subject.getWordList());
        if (seqPosNer.matches(REGEX.MULTI_WORD_ENTITY) || seqPosNer.matches(REGEX.MULTI_WORD_NOUN)){
            return;
        }
        
        // Do safe minimization first
        SubjSafeMinimization.minimizeSubject(subject, sg);
        
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
        excludeRels.add(EnglishGrammaticalRelations.PREPOSITIONAL_MODIFIER);
        //excludeRels.add(EnglishGrammaticalRelations.AUX_MODIFIER);
        for (IndexedWord w: subject.getWordList()) {
            // Skip the words that were included afterwards (not part of the DP)
            if (w.index() < 0)
                continue;
            
            // Get the relevant modifiers to be dropped (their modifiers as well)
            Set<IndexedWord> modifiers = sg.getChildrenWithRelns(w, excludeRels);
            for (IndexedWord m: modifiers) {
                ObjectArrayList<IndexedWord> subModifiers = CoreNLPUtils.getSubTreeSortedNodes(m, sg, null);
                for (IndexedWord sm: subModifiers)
                    //if (!sm.tag().equals(POS_TAG.IN))
                        dropWords.add(sm);
            }
            dropWords.addAll(modifiers);
            
            // Drop quantities
            if (w.ner().equals(Quantity.ST_QUANTITY)) 
                dropWords.add(w);
        }
        subject.getWordList().removeAll(dropWords);
        dropWords.clear();        
        
        // If [IN|TO] .* [IN|TO] => drop [IN|TO] .*, i.e. -> drop PP attachments
        TokenSequencePattern tPattern = TokenSequencePattern.compile(REGEX.T_PREP_ALL_PREP);
        TokenSequenceMatcher tMatcher = tPattern.getMatcher(subject.getWordCoreLabelList());
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
        subject.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, dropWords));
        subject.addDroppedWords(dropWords);
        subject.getWordList().removeAll(dropWords);
        dropWords.clear();

        // TODO: if QUANT + NP + IN => drop "QUANT + NP" ?
        
        // If VB_1+ TO VB_2 => drop VB_1+ TO .*
        tPattern = TokenSequencePattern.compile(REGEX.T_VB_TO_VB);
        tMatcher = tPattern.getMatcher(subject.getWordCoreLabelList());
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
        subject.getWordList().removeAll(dropWords);
        // add words to dropped word list
        subject.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, dropWords));
        subject.addDroppedWords(dropWords);
        dropWords.clear();
        
        // Drop auxilaries
        for (IndexedWord w: subject.getWordList()) {
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
        subject.getWordList().removeAll(dropWords);
        // add words to dropped word list
        subject.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, dropWords));
        subject.addDroppedWords(dropWords);
        dropWords.clear();
        
        // Drop noun modifiers with different NERs
        for (IndexedWord w: subject.getWordList()) {
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
        subject.getWordList().removeAll(dropWords);
        // add words to dropped word list
        subject.addDroppedEdges(CoreNLPUtils.listOfIndexedWordsToParentEdges(sg, dropWords));
        subject.addDroppedWords(dropWords);
        dropWords.clear();
    }
}