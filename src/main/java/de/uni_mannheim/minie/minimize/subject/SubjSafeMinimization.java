package de.uni_mannheim.minie.minimize.subject;

import java.util.ArrayList;
import java.util.List;

import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.minimize.Minimization;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.util.CoreMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class SubjSafeMinimization {
    /**
     * Minimize only the subjects that are considered to have "safe patterns"
     * @param subject: the subject phrase
     * @param sg: the semantic graph of the whole sentence
     */
    public static void minimizeSubject(AnnotatedPhrase subject, SemanticGraph sg){
        Minimization simp = new Minimization(subject, sg, new ObjectOpenHashSet<String>());
        
        // remWords: list of words to be removed (reusable variable)
        // matchWords: list of matched words from the regex (reusable variable)
        List<CoreMap> remWords = new ArrayList<>();
        List<CoreMap> matchWords = new ArrayList<>(); 
        
        // Safe minimization on the noun phrases and named entities
        simp.nounPhraseSafeMinimization(remWords, matchWords);
        simp.namedEntitySafeMinimization(remWords, matchWords);
    }
}
