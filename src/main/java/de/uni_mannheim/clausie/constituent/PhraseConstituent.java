package de.uni_mannheim.clausie.constituent;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import de.uni_mannheim.clausie.phrase.Phrase;
import edu.stanford.nlp.ling.IndexedWord;

/**
 * A phrase expression of a constituent. The constituent is represented as a Phrase
 *
 * @author Kiril Gashteovski
 *
 */
public class PhraseConstituent extends Constituent {
    /** The constituent as a phrase **/
    private Phrase phrase;
	
    /** Constructs a constituent with a specified textual representation and type. */
    public PhraseConstituent(Phrase p, Type type) {
        super(type);
        this.phrase = p;
        this.root = p.getRoot();
    }

    /** Returns a textual representation of the constituent. */
    public String rootString() {
        return this.phrase.getWords();
    }
	
    /** Adding a word to the list of words of the phrase **/
    public void addWordToList(IndexedWord word){
        this.phrase.addWordToList(word);
    }
    /** Adding all the elements from a list of indexed words to the list of indexed words of the phrase **/
    public void addWordsToList(ObjectArrayList<IndexedWord> words){
        this.phrase.addWordsToList(words);
    }
    public Phrase getPhrase(){
        return this.phrase;
    }
}
