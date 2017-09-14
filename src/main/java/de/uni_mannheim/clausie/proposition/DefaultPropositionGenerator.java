package de.uni_mannheim.clausie.proposition;

import java.util.SortedSet;
import java.util.TreeSet;

import de.uni_mannheim.clausie.ClausIE;
import de.uni_mannheim.clausie.clause.Clause;
import de.uni_mannheim.clausie.constituent.Constituent;
import de.uni_mannheim.clausie.constituent.IndexedConstituent;
import de.uni_mannheim.clausie.constituent.PhraseConstituent;
import de.uni_mannheim.clausie.constituent.Constituent.Status;
import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.semgraph.SemanticGraph;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;


/**
 * Currently the default proposition generator generates 3-ary propositions out of a clause
 *
 * @author Luciano del Corro
 * @author Kiril Gashteovski
 *
 * */
public class DefaultPropositionGenerator extends PropositionGenerator {
    public DefaultPropositionGenerator(ClausIE clausIE) {
        super(clausIE);
    }

    /** 
     *  @param clause: the clause in which the proposition is generated (and added to the list of propositions in 'clause')
     *  @param sGraph: semantic graph of the sentence
     */
    @Override
    public void generate(Clause clause, SemanticGraph sGraph) {
        Proposition proposition = new Proposition();
        ObjectArrayList<Constituent.Type> constTypes = new ObjectArrayList<Constituent.Type>();

        // Process subject
        if (clause.getSubject() > -1 && clause.getIncludedConstitsInds().getBoolean(clause.getSubject())) { // subject is -1 when there is an xcomp
            Phrase subjPhrase = generate(clause, clause.getSubject(), sGraph);
            Constituent subjConstituent = clause.getConstituents().get(clause.getSubject());
            subjPhrase.setRoot(subjConstituent.getRoot());
            proposition.addPhrase(new Phrase(subjPhrase));
            constTypes.add(Constituent.Type.SUBJECT);
        } else {
            //throw new IllegalArgumentException();
        }

        // Process verb
        if (clause.getIncludedConstitsInds().getBoolean(clause.getVerbInd())) {
            Phrase relation = generate(clause, clause.getVerbInd(), sGraph);
            Constituent verb = clause.getConstituents().get(clause.getVerbInd());
            relation.setRoot(verb.getRoot());
            proposition.addPhrase(new Phrase(relation));
            constTypes.add(Constituent.Type.VERB);
        } else {
            throw new IllegalArgumentException();
        }

        // Process arguments
        SortedSet<Integer> sortedIndexes = new TreeSet<Integer>();
        sortedIndexes.addAll(clause.getIobjectsInds());
        sortedIndexes.addAll(clause.getDobjectsInds());
        sortedIndexes.addAll(clause.getXcompsInds());
        sortedIndexes.addAll(clause.getCcompsInds());
        sortedIndexes.addAll(clause.getAcompsInds());
        sortedIndexes.addAll(clause.getAdverbialInds());
        if (clause.getComplementInd() >= 0)
            sortedIndexes.add(clause.getComplementInd());
        for (int index: sortedIndexes) {
            Constituent verbConstituent = clause.getConstituents().get(clause.getVerbInd());
            Constituent indexConstituent = clause.getConstituents().get(index);
            boolean isVerbIndexedConstituent = verbConstituent instanceof IndexedConstituent;
            boolean adverbialsContainIndex = clause.getAdverbialInds().contains(index);
            if (isVerbIndexedConstituent && adverbialsContainIndex && 
                    indexConstituent.getRoot().index() < verbConstituent.getRoot().index()) 
                continue;

            if (clause.getIncludedConstitsInds().getBoolean(index)) {
                Phrase argument = generate(clause, index, sGraph);
                argument.setRoot(clause.getConstituents().get(index).getRoot());
                proposition.addPhrase(new Phrase(argument));       
                constTypes.add(clause.getConstituents().get(index).getType());
            }
        }

        // Process adverbials  before verb
        sortedIndexes.clear();
        sortedIndexes.addAll(clause.getAdverbialInds());
        for (Integer index : sortedIndexes) {
            Constituent verbConstituent = clause.getConstituents().get(clause.getVerbInd());
            Constituent indexConstituent = clause.getConstituents().get(index);
            boolean isVerbPhraseConstituent = verbConstituent instanceof PhraseConstituent;
            // If the verb is a TextConstituent or the current constituent's root index is greater than the
            // verb constituent's root index -> break  
            if (isVerbPhraseConstituent || (indexConstituent.getRoot().index() > verbConstituent.getRoot().index())) 
                break;
            if (clause.getIncludedConstitsInds().getBoolean(index)) {
                Phrase argument = generate(clause, index, sGraph);
                argument.setRoot(clause.getConstituents().get(index).getRoot());
                proposition.getPhrases().add(new Phrase(argument));
                constTypes.add(clause.getConstituents().get(index).getType());

                if (clause.getConstituentStatus(index, clausIE.getOptions()).equals(Status.OPTIONAL)) {
                    proposition.addOptionalConstituentIndex(proposition.getPhrases().size());
                }	
            }
        }

        // Make triple if specified + push necessary constituents to the relation
        if (!clausIE.getOptions().nary) {
            proposition.clearOptionalConstituentIndicesSet();
            if (proposition.getPhrases().size() > 3) {
                // Push the necessary constituents to the relation
                pushConstituentsToRelation(proposition, constTypes);

                // Merge the rest of the n-ary tuple to the 3rd constituent (making it a triple)
                Phrase argPhrase = new Phrase();
                argPhrase.setRoot(proposition.getPhrases().get(2).getRoot());
                for (int i = 2; i < proposition.getPhrases().size(); i++) {
                    argPhrase.addWordsToList(proposition.getPhrases().get(i).getWordList().clone());
                }
                proposition.setPhrase(2, argPhrase);
                for (int i = proposition.getPhrases().size() - 1; i > 2; i--) {
                    proposition.getPhrases().remove(i);
                }
            }
        }

        // We are done
        clause.addProposition(proposition);
    }

    /**
     * Given a constituent index i, push it to the relation of the proposition p
     * @param proposition: proposition 
     * @param i: push the i-th phrase to the relation of the proposition 
     */
    private static void pushConstituentToRelation(Proposition p, int i){
        // New relational phrase. The root of the relational phrase is the verb by default
        Phrase relation = new Phrase();
        relation.setRoot(p.getPhrases().get(1).getRoot()); 
        
        // Push
        relation.addWordsToList(p.getPhrases().get(1).getWordList().clone());
        relation.addWordsToList(p.getPhrases().get(i).getWordList().clone());
        p.setRelation(relation);
        
        // Clean the i-th constituent
        p.getPhrases().get(i).getWordList().clear();
    }

    /**
     * Given a proposition and a list of constituency types (corresponding the phrases of the proposition), 
     * push the constituents to the relation if needed
     * @param proposition
     * @param constTypes
     */
    private static void pushConstituentsToRelation(Proposition p, ObjectArrayList<Constituent.Type> types){
        // Push constituents to the relation if the 4th constituent is an adverbial 
        // (for SVA(A), SVC(A), SVO(A), SVOA)
        if (types.get(3) == Constituent.Type.ADVERBIAL){
            // If the adverbial is consisted of one adverb, don't push the previous constituent
            if (p.getPhrases().get(3).getWordList().size() > 1) {
                // If CCOMP don't push it
                if (types.get(2) == Constituent.Type.CCOMP) {
                    return;
                }
                pushConstituentToRelation(p, 2);
            }
            // If the adverbial is consisted of one adverb, push the adverb to the relation
            else if (p.getPhrases().get(3).getWordList().size() == 1){
                if (CoreNLPUtils.isAdverb(p.getPhrases().get(3).getWordList().get(0).tag()))
                    pushConstituentToRelation(p, 3);
                else
                    pushConstituentToRelation(p, 2);
            }
        }
        // If the 3rd constituent is an indirect/direct object or an adverbial (for SVOO/SVOC, SVOA)
        else if (types.get(2) == Constituent.Type.IOBJ || types.get(2) == Constituent.Type.DOBJ ||
                 types.get(2) == Constituent.Type.ADVERBIAL){
            pushConstituentToRelation(p, 2);
        }
    }
}
