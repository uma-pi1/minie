package de.uni_mannheim.minie.minimize.relation;

import java.util.ArrayList;
import java.util.List;

import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.minimize.Minimization;
import de.uni_mannheim.minie.minimize.relation.RelSafeMinimization;

import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

/**
 * @author Kiril Gashteovski
 */
public class RelDictionaryMinimization {
    /**
     * Minimize only the relations that are considered to have "non-frequent patterns"
     * @param rel: the relation phrase
     * @param sg: semantic graph of the sentence
     * @param freqRels: dictionary of multi-word expressions (frequent relations)
     */
    public static void minimizeRelation(AnnotatedPhrase rel, SemanticGraph sg, ObjectOpenHashSet<String> collocations){
        // Do the safe minimization first
        RelSafeMinimization.minimizeRelation(rel, sg);
        
        // If the subject is frequent, don't minimize anything
        if (collocations.contains(CoreNLPUtils.listOfWordsToLemmaString(rel.getWordList()).toLowerCase())){
            return;
        }
        
        // Do the safe minimization first
        RelSafeMinimization.minimizeRelation(rel, sg);
        
        // remWords: list of words to be removed (reusable variable)
        // matchWords: list of matched words from the regex (reusable variable)
        List<CoreMap> remWords = new ArrayList<>();
        List<CoreMap> matchWords = new ArrayList<>(); 
        
        // Move to the dict. minimization of the noun phrases within the relation
        Minimization simp = new Minimization(rel, sg, collocations);
        simp.nounPhraseDictMinimization(remWords, matchWords);
        simp.namedEntityDictionaryMinimization(remWords, matchWords);
    }
}