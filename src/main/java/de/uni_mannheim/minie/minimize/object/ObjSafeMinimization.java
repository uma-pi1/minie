package de.uni_mannheim.minie.minimize.object;

import java.util.ArrayList;
import java.util.List;

import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.minimize.Minimization;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * @author Kiril Gashteovski
 */
public class ObjSafeMinimization {
    /**
     * Minimize only the objects that are considered to have "safe patterns"
     * @param object: the objects phrase
     * @param sg: the semantic graph of the whole sentence
     */
    public static void minimizeObject(AnnotatedPhrase object, SemanticGraph sg){
        Minimization simp = new Minimization(object, sg, new ObjectOpenHashSet<String>());
        
        // remWords: list of words to be removed (reusable variable)
        // matchWords: list of matched words from the regex (reusable variable)
        List<CoreMap> remWords = new ArrayList<>();
        List<CoreMap> matchWords = new ArrayList<>(); 
        
        // Safe minimization on the noun phrases and named entities
        simp.nounPhraseSafeMinimization(remWords, matchWords);
        simp.namedEntitySafeMinimization(remWords, matchWords);
    }
}

