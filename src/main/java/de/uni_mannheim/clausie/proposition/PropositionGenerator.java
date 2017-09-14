package de.uni_mannheim.clausie.proposition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import de.uni_mannheim.clausie.ClausIE;
import de.uni_mannheim.clausie.clause.Clause;
import de.uni_mannheim.clausie.constituent.Constituent;
import de.uni_mannheim.clausie.constituent.IndexedConstituent;
import de.uni_mannheim.clausie.constituent.PhraseConstituent;
import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.utils.coreNLP.DpUtils;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.semgraph.SemanticGraph;

/**
 * Handles the generation of propositions out of a given clause
 *
 * @author Luciano del Corro
 * @author Kiril Gashteovski
 *
 */
public abstract class PropositionGenerator {
    
    ClausIE clausIE;

    /** Relations to be excluded in every constituent of a clause except the verb */
    protected static final Set<GrammaticalRelation> EXCLUDE_RELATIONS;
    
    /** Relations to be excluded in the verb */
    protected static final Set<GrammaticalRelation> EXCLUDE_RELATIONS_VERB;

    static {
        EXCLUDE_RELATIONS = new HashSet<GrammaticalRelation>();
        EXCLUDE_RELATIONS.add(EnglishGrammaticalRelations.RELATIVE_CLAUSE_MODIFIER);
        EXCLUDE_RELATIONS.add(EnglishGrammaticalRelations.APPOSITIONAL_MODIFIER);
        EXCLUDE_RELATIONS.add(EnglishGrammaticalRelations.PARATAXIS);
        EXCLUDE_RELATIONS.add(EnglishGrammaticalRelations.valueOf("dep"));

        EXCLUDE_RELATIONS_VERB = new HashSet<GrammaticalRelation>();
        EXCLUDE_RELATIONS_VERB.addAll(EXCLUDE_RELATIONS);
        EXCLUDE_RELATIONS_VERB.add(EnglishGrammaticalRelations.valueOf("dep")); //without this asome adverbs or auxiliaries will end up in the relation
    }

    /** Constructs a proposition generator*/
    public PropositionGenerator(ClausIE clausIE) {
        this.clausIE = clausIE;
    }

    /** Generates propositions for a given clause*/
    public abstract void generate(Clause clause, SemanticGraph sGraph);

    /** Generates a textual representation of a given constituent plus a set of words*/
    private Phrase generatePhrase(IndexedConstituent constituent, Collection<IndexedWord> words, SemanticGraph sGraph) {
        Phrase phrase = new Phrase();
        
        if (constituent.isPrepositionalPhrase(sGraph)) {
            // TODO: before, it was: constituent.getRoot().originalText(). For some reason, in the case for
            // "in Los Angeles", the word "in" returns empty string for originalText(), and the actual word for word().
            // Check if this compromises the code in some way
            // TODO: see if you could find a faster way to make this check (not to go through the list of all excluded
            // words, for instance: use a flag as an input parameter)
            if (!constituent.excludedVertexes.contains(constituent.getRoot())){
                phrase.addWordToList(constituent.getRoot());
            }
        }

        for (IndexedWord word : words) {
            if (DpUtils.filterTokens(word))
                continue;
            phrase.addWordToList(word);
        }

        return phrase;
    }

    /** Generates a textual representation of a given constituent in a given clause*/
    public Phrase generate(Clause clause, int constituentIndex, SemanticGraph sGraph) {
        Set<GrammaticalRelation> excludeRelations = EXCLUDE_RELATIONS;
        if (clause.getVerbInd() == constituentIndex) {
            excludeRelations = EXCLUDE_RELATIONS_VERB;
        }

        return generate(clause, constituentIndex, excludeRelations, Collections.<GrammaticalRelation> emptySet(), sGraph);
    }

    /** Generates a textual representation of a given constituent in a given clause **/
    public Phrase generate(Clause clause, int constituentIndex, Collection<GrammaticalRelation> excludeRelations, 
            Collection<GrammaticalRelation> excludeRelationsTop, SemanticGraph sGraph) {
        
        Constituent constituent = clause.getConstituents().get(constituentIndex);

        if (constituent instanceof PhraseConstituent) {
            PhraseConstituent tConstituent = ((PhraseConstituent) constituent);
            return tConstituent.getPhrase();
        } else if (constituent instanceof IndexedConstituent) {
            IndexedConstituent iconstituent = (IndexedConstituent) constituent;
            SemanticGraph subgraph = iconstituent.createReducedSemanticGraph(); 
            DpUtils.removeEdges(subgraph, iconstituent.getRoot(), excludeRelations, excludeRelationsTop);
            Set<IndexedWord> words = new TreeSet<IndexedWord>(subgraph.descendants(iconstituent.getRoot()));
            
            for (IndexedWord v : iconstituent.getAdditionalVertexes()) {
                words.addAll(subgraph.descendants(v));
            }
            if (iconstituent.isPrepositionalPhrase(sGraph))
                words.remove(iconstituent.getRoot());
            
            Phrase phrase = generatePhrase(iconstituent, words, sGraph);
            return phrase;
        } else {
            throw new IllegalArgumentException();
        }
    }  
}
