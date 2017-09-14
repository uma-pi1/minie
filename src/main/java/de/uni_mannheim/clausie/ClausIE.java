package de.uni_mannheim.clausie;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.semgraph.SemanticGraph;
import de.uni_mannheim.clausie.clause.Clause;
import de.uni_mannheim.clausie.clause.ClauseDetector;
import de.uni_mannheim.clausie.conjunction.ProcessConjunctions;
import de.uni_mannheim.clausie.constituent.Constituent;
import de.uni_mannheim.clausie.constituent.IndexedConstituent;
import de.uni_mannheim.clausie.constituent.PhraseConstituent;
import de.uni_mannheim.clausie.constituent.XcompConstituent;
import de.uni_mannheim.clausie.constituent.Constituent.Status;
import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.clausie.proposition.DefaultPropositionGenerator;
import de.uni_mannheim.clausie.proposition.Proposition;
import de.uni_mannheim.clausie.proposition.PropositionGenerator;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Main class for ClausIE
 *
 * @author Luciano del Corro
 * @author Kiril Gashteovski
 */
public class ClausIE {
    // This should be a collection
    SemanticGraph semanticGraph;
    ObjectArrayList<Clause> clauses = new ObjectArrayList<Clause>();
    PropositionGenerator propositionGenerator = new DefaultPropositionGenerator(this);
    Options options;
    
    // Indicates if the clause processed comes from an xcomp constituent of the original sentence
    boolean xcomp = false;

    // -- construction
    // ----------------------------------------------------------------------------

    public ClausIE(Options options) {
        this.options = options;
    }

    public ClausIE() {
        this(new Options());
    }

    // -- misc method
    // -----------------------------------------------------------------------------

    public Options getOptions() {
        return options;
    }

    public void clear() {
        clauses.clear();
        //propositions.clear();
    }

    // -- clause detection
    // ------------------------------------------------------------------------

    /** Detects clauses in the sentence. */
    public void detectClauses() {
        ClauseDetector.detectClauses(this);
    }

    /** Returns clauses in the sentence. */
    public ObjectArrayList<Clause> getClauses() {
        return clauses;
    }

    // -- proposition generation
    // ------------------------------------------------------------------

    /** Generates propositions from the clauses in the sentence. */
    public void generatePropositions(final SemanticGraph sGraph) {
        // Holds alternative options for each constituents (obtained by processing coordinated conjunctions and xcomps)
        final ObjectArrayList<ObjectArrayList<Constituent>> constituents = new ObjectArrayList<ObjectArrayList<Constituent>>();

        // Which of the constituents are required?
        final ObjectArrayList<Status> flags = new ObjectArrayList<Status>();
        final BooleanArrayList include = new BooleanArrayList();

        // Holds all valid combination of constituents for which a proposition is to be generated
        final ObjectArrayList<BooleanArrayList> includeConstituents = new ObjectArrayList<BooleanArrayList>();

        // Let's start
        for (Clause clause : clauses) {
            // process coordinating conjunctions
            constituents.clear();

            for (int i = 0; i < clause.getConstituents().size(); i++) {
                // if(xcomp && clause.subject == i) continue; //An xcomp does 
                // not have an internal subject so should not be processed here
                Constituent constituent = clause.getConstituents().get(i);
                ObjectArrayList<Constituent> alternatives;
                if (!(xcomp && clause.getSubject() == i)
                        && constituent instanceof IndexedConstituent
                        // the processing of the xcomps is done in Default
                        // proposition generator. 
                        // Otherwise we get duplicate propositions.
                        && !clause.getXcompsInds().contains(i)
                        && ((i == clause.getVerbInd() && options.processCcAllVerbs) || 
                                (i != clause.getVerbInd() && options.processCcNonVerbs))) {
                    alternatives = ProcessConjunctions.processCC(clause, constituent, i);
                } else if (!(xcomp && clause.getSubject() == i) && clause.getXcompsInds().contains(i)) {
                    alternatives = new ObjectArrayList<Constituent>();
                    ClausIE xclausIE = new ClausIE(options);
                    xclausIE.xcomp = true;
                    xclausIE.clauses = ((XcompConstituent) clause.getConstituents().get(i)).getClauses();
                    xclausIE.generatePropositions(sGraph);
                    for (Clause cl: xclausIE.getClauses()){
                        for (Proposition p : cl.getPropositions()) {
                            Phrase phrase = new Phrase();
                            for (int j = 0; j < p.getPhrases().size(); j++) {
                                if (j == 0)   // to avoid including the subjecct, We
                                    continue; // could also generate the prop
                                              // without the subject                                            
                                phrase.addWordsToList(p.getPhrases().get(j).getWordList().clone());
                            }
                            alternatives.add(new PhraseConstituent(phrase, constituent.getType()));
                        }
                    }
                } else {
                    alternatives = new ObjectArrayList<Constituent>(1);
                    alternatives.add(constituent);
                }
                constituents.add(alternatives);
            }

            // Create a list of all combinations of constituents for which a proposition should be generated
            includeConstituents.clear();
            flags.clear();
            include.clear();
            for (int i = 0; i < clause.getConstituents().size(); i++) {
                Status flag = clause.getConstituentStatus(i, options);
                flags.add(flag);
                include.add(!flag.equals(Status.IGNORE));
            }
            if (options.nary) {
                // We always include all constituents for n-ary output (optional parts marked later)
                includeConstituents.add(include);
            } else {
                // Triple mode; determine which parts are required
                for (int i = 0; i < clause.getConstituents().size(); i++) {
                    include.set(i, flags.get(i).equals(Status.REQUIRED));
                }

                // Create combinations of required/optional constituents
                new Runnable() {
                    int noOptional;

                    @Override
                    public void run() {
                        noOptional = 0;
                        for (Status f : flags) {
                            if (f.equals(Status.OPTIONAL))
                                noOptional++;
                        }
                        run(0, 0, new ArrayList<Boolean>());
                    }

                    private void run(int pos, int selected, List<Boolean> prefix) {
                        if (pos >= include.size()) {
                            if (selected >= Math.min(options.minOptionalArgs, noOptional)
                                    && selected <= options.maxOptionalArgs) {
                                includeConstituents.add(new BooleanArrayList(prefix));
                            }
                            return;
                        }
                        prefix.add(true);
                        if (include.getBoolean(pos)) {
                            run(pos + 1, selected, prefix);
                        } else {
                            if (!flags.get(pos).equals(Status.IGNORE)) {
                                run(pos + 1, selected + 1, prefix);
                            }
                            prefix.set(prefix.size() - 1, false);
                            run(pos + 1, selected, prefix);
                        }
                        prefix.remove(prefix.size() - 1);
                    }
                }.run();
            }

            // Create a temporary clause for which to generate a proposition
            final Clause tempClause = clause.clone();
                    
            // Generate propositions
            new Runnable() {
                @Override
                public void run() {
                    // Select which constituents to include
                    for (BooleanArrayList include : includeConstituents) {
                        // Now select an alternative for each constituent
                        selectConstituent(0, include);
                    }
                }

                void selectConstituent(int i, BooleanArrayList include) {
                    if (i < constituents.size()) {
                        if (include.getBoolean(i)) {
                            List<Constituent> alternatives = constituents.get(i);
                            for (int j = 0; j < alternatives.size(); j++) {
                                tempClause.getConstituents().set(i, alternatives.get(j));
                                selectConstituent(i + 1, include);
                            }
                        } else {
                            selectConstituent(i + 1, include);
                        }
                    } else {
                        // Everything selected; generate
                        tempClause.setIncludedConstitsInds(include);
                        propositionGenerator.generate(tempClause, sGraph);
                    }
                }
            }.run();
        }
    }
    
    public void setSemanticGraph(SemanticGraph semGraph){
        this.semanticGraph = semGraph;
    }
    
    public void clearClauses(){
        clauses.clear();
    }
    
    /** Returns the dependency tree for the sentence. */
    public SemanticGraph getSemanticGraph() {
        return semanticGraph;
    }
    
    /** Get the list of propositions, without the information about the clauses they are derived from **/
    public ObjectArrayList<Proposition> getPropositions() {
        ObjectArrayList<Proposition> props = new ObjectArrayList<>();
        for (Clause cl: this.clauses) {
            props.addAll(cl.getPropositions());
        }
        return props;
    }
}