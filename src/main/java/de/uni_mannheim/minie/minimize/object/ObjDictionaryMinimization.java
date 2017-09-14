package de.uni_mannheim.minie.minimize.object;

import java.util.ArrayList;
import java.util.List;

import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.minimize.Minimization;
import de.uni_mannheim.minie.minimize.object.ObjSafeMinimization;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * @author Kiril Gashteovski
 */
public class ObjDictionaryMinimization {
    /**
     * Minimize only the objects that are considered to have "non-frequent patterns"
     * @param obj: the object phrase
     * @param sg: semantic graph of the sentence
     * @param freqObjs: dictionary of multi-word expressions (frequent objects)
     */
    public static void minimizeObject(AnnotatedPhrase obj, SemanticGraph sg, ObjectOpenHashSet<String> collocations){
        // Do the safe minimization first
        ObjSafeMinimization.minimizeObject(obj, sg);
        
        // If the object is frequent, don't minimize anything
        if (collocations.contains(CoreNLPUtils.listOfWordsToLemmaString(obj.getWordList()).toLowerCase())){
            return;
        }
        
        // Minimization object
        Minimization simp = new Minimization(obj, sg, collocations);
        
        // remWords: list of words to be removed (reusable variable)
        // matchWords: list of matched words from the regex (reusable variable)
        List<CoreMap> remWords = new ArrayList<>();
        List<CoreMap> matchWords = new ArrayList<>(); 
        
        // Safe minimization on the noun phrases and named entities within the subj. phrase
        simp.nounPhraseDictMinimization(remWords, matchWords);
        simp.namedEntityDictionaryMinimization(remWords, matchWords);
    }
}