package de.uni_mannheim.clausie.clause;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import de.uni_mannheim.clausie.Options;
import de.uni_mannheim.clausie.constituent.Constituent;
import de.uni_mannheim.clausie.constituent.IndexedConstituent;
import de.uni_mannheim.clausie.constituent.Constituent.Status;
import de.uni_mannheim.clausie.proposition.Proposition;
import edu.stanford.nlp.ling.IndexedWord;

/**
 * A clause is a basic unit of a sentence. In ClausIE, a clause consists of a
 * set of constituents (at least a subject and a verb) and a type.
 *
 * @author Luciano del Corro
 * @author Kiril Gashteovski
 */
public class Clause {
    // -- Type definition
    // -------------------------------------------------------------------------

    /** Clause types */
    public enum Type {
        SV, SVC, SVA, SVO, SVOO, SVOC, SVOA, EXISTENTIAL, UNKNOWN;
    };

    /** -- member variables
	// -----------------------------------------------------------------------
     **/

    // Constituents of this clause
    ObjectArrayList<Constituent> constituents;

    // Type of this clause
    private Type type;

    // Position of subject in {@link #constituents}
    private int subject;

    /** Position of verb in {@link #constituents} */
    int verb;

    // They are lists because some times the parsers (probably an error) generates more than one constituent of each type
    // e.g., more than one dobj produced by parser for "The man who I told the fact is dead."
    /** Position(s) of direct object(s) in {@link #constituents}. */
    IntArrayList dobjects;

    /** Position(s) of indirect object in {@link #constituents} */
    IntArrayList iobjects;

    /** Position of complement in {@link #constituents} (for SVC / SVOC) */
    int complement;

    /** Position(s) of xcomps in {@link #constituents} */
    IntArrayList xcomps;

    /** Position(s) of ccomps in {@link #constituents} */
    IntArrayList ccomps;

    /** Position(s) of acomps in {@link #constituents} */
    IntArrayList acomps;

    /** Position(s) of adverbials in {@link #constituents} */
    IntArrayList adverbials;

    /** If a relative pronoun refers to an adverbial */
    boolean relativeAdverbial;

    /**
     * Parent clause of this clause, if any. For example, in
     * "He said this is true." the clause "this / is / true" has parent
     * "he / said / this is true".
     */
    Clause parentClause;

    /** Agent (for passive voice). Currently unused. */
    IndexedWord agent;

    /** Which of the constituents are required? */
    BooleanArrayList include;

    /** List of propositions generated from the clause **/
    ObjectArrayList<Proposition> propositions;

    // -- construction
    // ----------------------------------------------------------------------------

    // make package private
    Clause() {
        this.constituents = new ObjectArrayList<Constituent>();
        this.type = Type.UNKNOWN;
        this.subject = -1;
        this.verb = -1;
        this.dobjects = new IntArrayList();
        this.iobjects = new IntArrayList();
        this.complement = -1;
        this.xcomps = new IntArrayList();
        this.ccomps = new IntArrayList();
        this.acomps = new IntArrayList();
        this.adverbials = new IntArrayList();
        this.relativeAdverbial = false;
        this.parentClause = null;
        this.include = new BooleanArrayList();
        this.propositions = new ObjectArrayList<Proposition>();
    };

    @Override
    public Clause clone() {
        Clause clause = new Clause();
        clause.constituents = new ObjectArrayList<Constituent>(constituents);
        clause.type = type;
        clause.subject = subject;
        clause.verb = verb;
        clause.dobjects = new IntArrayList(dobjects);
        clause.iobjects = new IntArrayList(iobjects);
        clause.complement = complement;
        clause.xcomps = new IntArrayList(xcomps);
        clause.ccomps = new IntArrayList(ccomps);
        clause.acomps = new IntArrayList(acomps);
        clause.adverbials = new IntArrayList(adverbials);
        clause.relativeAdverbial = relativeAdverbial;
        clause.agent = agent;
        clause.parentClause = parentClause;
        clause.include = include;
        clause.propositions = propositions;
        return clause;
    }

    // -- methods
    // ---------------------------------------------------------------------------------

    /** Determines the type of this clause, if still unknown. */
    void detectType(Options options) {
        if (type != Type.UNKNOWN)
            return;

        // count the total number of complements (dobj, ccomp, xcomp)
        int noComplements = noComplements();

        // sometimes the parsers gives ccomp and xcomp instead of direct objects
        // e.g., "He is expected to tell the truth."
        IndexedWord root = ((IndexedConstituent) constituents.get(verb)).getRoot();
        boolean hasDirectObject = dobjects.size() > 0 || (complement < 0 && noComplements > 0 && !options.isCop(root));
        boolean hasIndirectObject = !iobjects.isEmpty();

        // Q1: Object?
        if (hasDirectObject || hasIndirectObject) {
            // Q7: dir. and indir. object?
            if (noComplements > 0 && hasIndirectObject) {
                type = Type.SVOO;
                return;
            }

            // Q8: Complement?
            if (noComplements > 1) {
                type = Type.SVOC;
                return;
            }

            // Q9: Candidate adverbial and direct objects?
            if (!(hasCandidateAdverbial() && hasDirectObject)) {
                type = Type.SVO;
                return;
            }

            // Q10: Potentially complex transitive?
            if (options.isComTran(root)) {
                type = Type.SVOA;
                return;
            }

            // Q11: Conservative?
            if (options.conservativeSVOA) {
                type = Type.SVOA;
                return;
            } else {
                type = Type.SVO;
                return;
            }
        } else {
            // Q2: Complement?
            // not sure about acomp, can a copular be transitive?
            if (complement >= 0 || noComplements > 0 && options.isCop(root) || !acomps.isEmpty()) {
                type = Type.SVC;
                return;
            }

            // Q3: Candidate adverbial
            if (!hasCandidateAdverbial()) {
                type = Type.SV;
                return;
            }

            // Q4: Known non ext. copuular
            if (options.isNotExtCop(root)) {
                type = Type.SV;
                return;
            }

            // Q5: Known ext. copular
            if (options.isExtCop(root)) {
                type = Type.SVA;
                return;
            }

            // Q6: Conservative
            if (options.conservativeSVA) {
                type = Type.SVA;
                return;
            } else {
                type = Type.SV;
                return;
            }
        }
    }

    /** Checks whether this clause has a candidate adverbial, i.e., an adverbial that can potentially be obligatory. */
    public boolean hasCandidateAdverbial() {
        if (adverbials.isEmpty())
            return false;
        if (relativeAdverbial)
            return true;

        // is there an adverbial that occurs after the verb?
        if (((IndexedConstituent) constituents.get(adverbials.getInt(adverbials.size() - 1))).getRoot().index() > 
        ((IndexedConstituent) constituents.get(verb)).getRoot().index())
            return true;
        return false;
    }

    /** 
     *  Determines the total number of complements (includes direct objects, subject complements, etc.) present in this 
     *  clause. 
     */
    int noComplements() {
        return dobjects.size() + (complement < 0 ? 0 : 1) + xcomps.size() + ccomps.size();
    }

    @Override
    public String toString() {
        return toString(null);
    }

    public String toString(Options options) {
        Clause clause = this;
        StringBuffer s = new StringBuffer();
        s.append(clause.type.name());
        s.append(" (");
        String sep = "";
        for (int index = 0; index < constituents.size(); index++) {
            Constituent constituent = constituents.get(index);

            s.append(sep);
            sep = ", ";
            switch (constituent.getType()) {
            case ACOMP:
                s.append("ACOMP");
                break;
            case ADVERBIAL:
                s.append("A");
                if (options != null) {
                    switch (getConstituentStatus(index, options)) {
                    case IGNORE:
                        s.append("-");
                        break;
                    case OPTIONAL:
                        s.append("?");
                        break;
                    case REQUIRED:
                        s.append("!");
                        break;
                    }
                }
                break;
            case CCOMP:
                s.append("CCOMP");
                break;
            case COMPLEMENT:
                s.append("C");
                break;
            case DOBJ:
                s.append("O");
                break;
            case IOBJ:
                s.append("IO");
                break;
            case SUBJECT:
                s.append("S");
                break;
            case UNKOWN:
                s.append("?");
                break;
            case VERB:
                s.append("V");
                break;
            case XCOMP:
                s.append("XCOMP");
                break;
            }
            s.append(": ");
            if (!(constituent instanceof IndexedConstituent)) {
                s.append("\"");
                s.append(constituent.rootString());
            }
            //s.append(constituent.rootString());
            if (constituent instanceof IndexedConstituent) {
                s.append(((IndexedConstituent) constituent).getRoot().word());
                s.append("@");
                s.append(((IndexedConstituent) constituent).getRoot().index());
            } else {
                s.append("\"");
            }
        }
        s.append(")");
        return s.toString();
    }

    /**
     * Determines the flag of the adverbial at position {@code index} in {@link #adverbials}, i.e., whether the adverbial is 
     * required, optional, or to be ignored.
     */
    public Status getConstituentStatus(int index, Options options) {
        boolean first = true;
        for (int i : adverbials) {
            if (i == index && isIgnoredAdverbial(i, options))
                return Status.IGNORE;
            else if (i == index && isIncludedAdverbial(i, options))
                return Status.REQUIRED;
            int adv = ((IndexedConstituent) constituents.get(i)).getRoot().index();
            if (constituents.get(verb) instanceof IndexedConstituent && 
                    adv < ((IndexedConstituent) constituents.get(verb)).getRoot().index() && !relativeAdverbial) {
                if (i == index) {
                    return Status.OPTIONAL;
                }
            } else {
                if (i == index) {
                    if (!first)
                        return Status.OPTIONAL;
                    return !(Type.SVA.equals(type) || Type.SVOA.equals(type)) ? Status.OPTIONAL : Status.REQUIRED;
                }
                first = false;
            }
        }
        return Status.REQUIRED;
    }

    /**
     * Checks whether the adverbial at position {@code index} in {@link #adverbials} is to be ignored by ClausIE.
     */
    private boolean isIgnoredAdverbial(int index, Options options) {
        Constituent constituent = constituents.get(index);
        String s;
        if (constituent instanceof IndexedConstituent) {
            IndexedConstituent indexedConstituent = (IndexedConstituent) constituent;
            IndexedWord root = indexedConstituent.getRoot();
            if (indexedConstituent.getSemanticGraph().hasChildren(root)) {
                // ||IndexedConstituent.sentSemanticGraph.getNodeByIndexSafe(root.index()
                // + 1) != null
                // &&
                // IndexedConstituent.sentSemanticGraph.getNodeByIndexSafe(root.index()
                // + 1).tag().charAt(0) == 'J') { //do not ignore if it modifies
                // an adjective. Adverbs can modify verbs or adjective no reason
                // to ignore them when they refer to adjectives (at lest in
                // triples). This is important in the case of adjectival
                // complements
                return false;
            }
            s = root.lemma();
        } else {
            s = constituent.rootString();
        }

        if (options.dictAdverbsIgnore.contains(s) || (options.processCcNonVerbs && options.dictAdverbsConj.contains(s)))
            return true;
        else
            return false;
    }

    /**
     * Checks whether the adverbial at position {@code index} in {@link #adverbials} is required to be output by ClausIE 
     * (e.g., adverbials indicating negation, such as "hardly").
     */
    private boolean isIncludedAdverbial(int index, Options options) {
        Constituent constituent = constituents.get(index);
        String s;
        if (constituent instanceof IndexedConstituent) {
            IndexedConstituent indexedConstituent = (IndexedConstituent) constituent;
            IndexedWord root = indexedConstituent.getRoot();
            if (indexedConstituent.getSemanticGraph().hasChildren(root)) {
                return false;
            }
            s = root.lemma();
        } else {
            s = constituent.rootString();
        }
        return options.dictAdverbsInclude.contains(s);
    }

    /** 
     *  Checks whether or not this clause has an adverbial which is after the dobject or xcomp (if there is any). 
     *  In case there are no adverbials or the first adverbial is after the first dobject or xcomp -> return false,
     *  in any other case -> return true 
     */
    boolean firstAdverbialChecks(){ 
        if (this.adverbials.size() <= 0){
            return false;
        } else if (this.dobjects.size() > 0){
            if (this.dobjects.getInt(0) < this.adverbials.getInt(0)){
                return false;
            }
        } else if (this.xcomps.size() > 0){
            if (this.xcomps.getInt(0) < this.adverbials.getInt(0)){
                return false;
            }
        } else if (this.iobjects.size() > 0){
            if (this.iobjects.getInt(0) < this.adverbials.getInt(0)){
                return false;
            }
        } else if (this.ccomps.size() > 0){
            if (this.ccomps.getInt(0) < this.adverbials.getInt(0)){
                return false;
            }
        } else if (this.acomps.size() > 0){
            if (this.acomps.getInt(0) < this.adverbials.getInt(0)){
                return false;
            }
        }

        return true;
    }

    /** Add proposition to the list of propositions **/
    public void addProposition(Proposition p){
        this.propositions.add(p);
    }
    public void addAllPropositions(List<Proposition> props){
        this.propositions.addAll(props);
    }

    /** Getters **/
    public Type getType(){
        return this.type;
    }
    public int getSubject(){
        return this.subject;
    }
    public Clause getParentClause(){
        return this.parentClause;
    }
    public ObjectArrayList<Constituent> getConstituents(){
        return this.constituents;
    }
    public int getSubjectInd(){
        return this.getSubject();
    }
    public int getVerbInd(){
        return this.verb;
    }
    public IntArrayList getDobjectsInds(){
        return this.dobjects;
    }
    public IntArrayList getIobjectsInds(){
        return this.iobjects;
    }
    public int getComplementInd(){
        return this.complement;
    }
    public IntArrayList getXcompsInds(){
        return this.xcomps;
    }
    public IntArrayList getCcompsInds(){
        return this.ccomps;
    }
    public IntArrayList getAcompsInds(){
        return this.acomps;
    }
    public IntArrayList getAdverbialInds(){
        return this.adverbials;
    }
    public boolean hasRelativeAdverbial(){
        return this.relativeAdverbial;
    }
    public BooleanArrayList getIncludedConstitsInds(){
        return this.include;
    }
    public ObjectArrayList<Proposition> getPropositions(){
        return this.propositions;
    }

    /** Setters **/
    public void setType(Type t){
        this.type = t;
    }
    public void setSubject(int sub){
        this.subject = sub;
    }
    public void setPropositions(ObjectArrayList<Proposition> props){
        this.propositions = props;
    }
    public void setIncludedConstitsInds(BooleanArrayList incl){
        this.include = incl;
    }
}
