
package de.uni_mannheim.clausie.proposition;

import java.util.HashSet;
import java.util.Set;

import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.constant.SEPARATOR;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/** Stores a proposition.
 * 
 * @author Luciano del Corro
 * @author Kiril Gashteovski
 *
 */
public class Proposition {
	
	/** Constituents of the proposition */
    private ObjectArrayList<Phrase> phrases = new ObjectArrayList<Phrase>();
	
    /** Position of optional constituents */
	private Set<Integer> optional = new HashSet<Integer>();
	
	// TODO: types of constituents (e.g., optionality) sentence ID etc.
	
	public Proposition() {
	}
	
	/**
	 * Removes a word from a constituent
	 * @param i: the constituent index
	 * @param j: the word index within the constituent
	 */
	public void removeWordFromConstituent(int i, int j){
	    this.phrases.get(i).removeWordFromList(j);
	}
	
	/** Returns a list of constituents of the proposition */
	public ObjectArrayList<Phrase> getConstituents(){
		return this.phrases;
	}
	
	/** Returns the subject of the proposition */
	public Phrase subject() {
		return this.phrases.get(0);
	}
	
	/** Returns the relation of the proposition */
	public Phrase relation() {
		return phrases.get(1);
	}
	
	/** Returns the object of the proposition (should be used when working with triples only!) */
	public Phrase object(){
	    return phrases.get(2);
	}
	
	/** Sets the relation of the proposition */
	public void setRelation(Phrase rel){
	    phrases.set(1, rel);
	}
	
	/** Returns a constituent in a given position */
	public Phrase argument(int i) {
		return phrases.get(i + 2);
	}
	
	/** Returns the number of arguments */
	public int noArguments() {
		return phrases.size() - 2;
	}
	
	/** Checks if an argument is optional */
	public boolean isOptionalArgument(int i) {
		return optional.contains(i + 2);
	}
	
	/**
	 * Given a proposition, this function turns it into a "sentence" by concatenating the constituents' strings
	 * @return
	 */
	public String propositionToString(){
	    StringBuffer sb = new StringBuffer();
	    for (int i = 0; i < phrases.size(); i++){
	        sb.append(phrases.get(i).getWords());
	        sb.append(SEPARATOR.SPACE);
	    }
	    return sb.toString().trim();
	}
	
	public ObjectArrayList<Phrase> getPhrases(){
	    return this.phrases;
	}
	public void addPhrase (Phrase p){
	    this.phrases.add(p);
	}
	public void setPhrase(int i, Phrase p){
	    this.phrases.set(i, p);
	}
	/** Get optional constituents' indices **/
	public Set<Integer> getOptinoalConstituentsIndices(){
	    return this.optional;
	}
	/** Add index of an optional constituent **/
	public void addOptionalConstituentIndex(int i){
	    this.optional.add(i);
	}
	/** Clear the set of optional constituent indices **/
	public void clearOptionalConstituentIndicesSet(){
	    this.optional.clear();
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		String sep = "(";
		
		for (int i=0; i < phrases.size(); i++) {
			String constituent = phrases.get(i).getWords();
			sb.append(sep);
			sep = ", ";
			sb.append("\"");
			sb.append(constituent);
			sb.append("\"");
			if (optional.contains(i)) {
				sb.append("?");
			}
		}
		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public Proposition clone() {
		Proposition clone = new Proposition();
		clone.phrases = new ObjectArrayList<Phrase>(this.phrases.clone());
		clone.optional = new HashSet<Integer>(this.optional);
		return clone;
	}
}
