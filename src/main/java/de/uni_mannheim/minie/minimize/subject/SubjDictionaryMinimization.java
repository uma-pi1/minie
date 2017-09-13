package de.uni_mannheim.minie.minimize.subject;

import java.util.ArrayList;
import java.util.List;

import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.minimize.Minimization;
import de.uni_mannheim.minie.minimize.subject.SubjSafeMinimization;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * Minimize the subject according to the 'Dictionary minimization' rules. 
 * @param subject: the subject phrase
 * @param sg: semantic graph of the whole sentence
 * @param freqSubj: a multi-word dictionary of frequent subjects
 * @param collocations: a dictionary of collocations
**/
public class SubjDictionaryMinimization {
    public static void minimizeSubject(AnnotatedPhrase subject, SemanticGraph sg, ObjectOpenHashSet<String> collocations){
        // Do the safe minimization first
        SubjSafeMinimization.minimizeSubject(subject, sg);
        
        // If the subject is frequent, don't minimize anything
        if (collocations.contains(CoreNLPUtils.listOfWordsToLemmaString(subject.getWordList()).toLowerCase())){
            return;
        }
        
        // Minimization object
        Minimization simp = new Minimization(subject, sg, collocations);
        
        // remWords: list of words to be removed (reusable variable)
        // matchWords: list of matched words from the regex (reusable variable)
        List<CoreMap> remWords = new ArrayList<>();
        List<CoreMap> matchWords = new ArrayList<>(); 
        
        // Safe minimization on the noun phrases and named entities within the subj. phrase
        simp.nounPhraseDictMinimization(remWords, matchWords);
        simp.removeVerbsBeforeNouns(remWords, matchWords);
        simp.namedEntityDictionaryMinimization(remWords, matchWords);
    }
}