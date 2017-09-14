package de.uni_mannheim.clausie.phrase;

import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import de.uni_mannheim.constant.SEPARATOR;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

/**
 * An object representing a phrase
 * @param: wordList: A list of words of the phrase
 * @param: phraseGraph: the dependency parse graph of the phrase
 * @param: root: the root of the phrase
 * @param: tds: list of typed dependencies for the phrase
 *
 * @author Kiril Gashteovski
 */
public class Phrase {
    /** A list of words of the phrase **/
    protected ObjectArrayList<IndexedWord> wordList;
    /** The dependency parse graph of the phrase **/ 
    protected SemanticGraph phraseGraph;
    /** The root of the phrase **/
    protected IndexedWord root;
    /** List of typed dependencies for the phrase **/
    protected ObjectArrayList<TypedDependency> tds;
    
    /** Constructors **/
    public Phrase(){
        this.wordList = new ObjectArrayList<IndexedWord>();
        this.phraseGraph = new SemanticGraph();
        this.root = new IndexedWord();
        this.tds = new ObjectArrayList<TypedDependency>();
    }
    
    /** Copy constructor **/
    public Phrase(Phrase p){
        this(p.getWordList().clone(), new SemanticGraph(p.getPhraseGraph()), new IndexedWord(p.getRoot()), 
                p.getTypedDependencies().clone());
    }
    
    /** Other constructors **/
    /**
     * Parametric constructor with a list of words for the phrase. The root, phrase graph and the list of typed dependencies
     * are empty.
     * @param wList: list of words for the phrase
     */
    public Phrase(ObjectArrayList<IndexedWord> wList) {
        this.wordList = wList;
        this.phraseGraph = new SemanticGraph();
        this.root = new IndexedWord();
        this.tds = new ObjectArrayList<TypedDependency>();
    }
    
    /**
     * Parametric constructor with a list of words for the phrase and the semantic graph of the phrase. The root, 
     * and the list of typed dependencies are empty.
     * @param wList: list of words for the phrase
     * @param sg: semantic graph for the phrase
     */
    public Phrase(ObjectArrayList<IndexedWord> wList, SemanticGraph sg){
        this.wordList = wList;
        this.phraseGraph = sg;
        this.root = new IndexedWord();
        this.tds = new ObjectArrayList<TypedDependency>();
    }
    
    /**
     * Parametric constructor with a list of words for the phrase, the semantic graph of the phrase and the root of the 
     * phrase. The list of typed dependencies is empty.
     * @param wList: list of words for the phrase
     * @param sg: semantic graph of the phrase
     * @param r: root of the phrase
     */
    public Phrase(ObjectArrayList<IndexedWord> wList, SemanticGraph sg, IndexedWord r){
        this.wordList = wList;
        this.phraseGraph = sg;
        this.root = r;
        this.tds = new ObjectArrayList<TypedDependency>();
    }
    
    /**
     * Parametric constructor with a list of words for the phrase, and the root of the phrase. 
     * The list of typed dependencies and the semantic graph of the phrase are empty.
     * @param wList: list of words for the phrase
     * @param r: the root of the phrase
     */
    public Phrase(ObjectArrayList<IndexedWord> wList, IndexedWord r) {
        this.wordList = wList;
        this.root = r;
        this.phraseGraph = new SemanticGraph();
        this.tds = new ObjectArrayList<>();
    }
    
    /**
     * Parametric constructor with a list of words for the phrase, the semantic graph of the phrase, the root of the 
     * and the list of typed dependencies.
     * @param wList: list of words for the phrase
     * @param sg: the semantic graph of the phrase
     * @param r: the root of the phrase
     * @param td: list of typed dependencies for the phrase
     */
    public Phrase(ObjectArrayList<IndexedWord> wList, SemanticGraph sg, IndexedWord r, ObjectArrayList<TypedDependency> td){
        this.wordList = wList;
        this.phraseGraph = sg;
        this.root = r;
        this.tds = td;
    }
    
    /** Add indexed word to the word list **/
    public void addWordToList(IndexedWord word){
        this.wordList.add(word);
    }
    /** Add a list of indexed words to the word list **/
    public void addWordsToList(ObjectArrayList<IndexedWord> words){
        this.wordList.addAll(words);
    }

    
    /** Remove a word from the list (given the index) **/
    public void removeWordFromList(int i){
        this.wordList.remove(i);
    }
    /** Remove a word from the list (given a starting and ending index) **/
    public void removeWordsFromTo(int i, int j){
        this.wordList.removeElements(i, j);
    }
    /** Remove a set of words from the list (given a set of words) **/
    public void removeWordsFromList(ObjectOpenHashSet<IndexedWord> words){
        this.wordList.removeAll(words);
    }
    /** Remove a set of words represented as core labels from the list of indexed words **/
    public void removeCoreLabelWordsFromList(List<CoreMap> cmWords){
        ObjectArrayList<IndexedWord> rWords = new ObjectArrayList<>();
        for (CoreMap cm: cmWords){
            rWords.add(new IndexedWord(new CoreLabel(cm)));
        }
        this.removeWordsFromList(rWords);
    }
    
    public void removeWordsFromList(ObjectArrayList<IndexedWord> words){
        this.wordList.removeAll(words);
    }
    
    
    /** Getters **/
    public ObjectArrayList<IndexedWord> getWordList(){
        return this.wordList;
    }
    public ObjectArrayList<IndexedWord> getWordSubList(int from, int to){
        ObjectArrayList<IndexedWord> sublist = new ObjectArrayList<IndexedWord>();
        for (int i = from; i <= to; i++){
            sublist.add(this.wordList.get(i));
        }
        return sublist;
    }
    public IndexedWord getRoot(){
        return this.root;
    }
    public SemanticGraph getPhraseGraph(){
        return this.phraseGraph;
    }
    public ObjectArrayList<TypedDependency> getTypedDependencies(){
        return this.tds;
    }
    public ObjectArrayList<CoreLabel> getWordCoreLabelList(){
        ObjectArrayList<CoreLabel> clList = new ObjectArrayList<>();
        for (IndexedWord w: this.wordList){
            clList.add(new CoreLabel(w));
        }
        return clList;
    }
    
    /** Setters **/
    public void setWordList(ObjectArrayList<IndexedWord> words){
        this.wordList = words;
    }
    public void setRoot(IndexedWord r){
        this.root = r;
    }
    public void setPhraseGraph(SemanticGraph sg){
        this.phraseGraph = sg;
    }
    public void setTypedDependencies(ObjectArrayList<TypedDependency> tds){
        this.tds = tds;
    }
    /**
     * Replace the word in the list on the i-th position with the new word 
     * @param i: the position of the word in the wordlist to be replaced
     * @param newWord: the new word to be put in the i-th position 
     */
    public void setWordInWordList(int i, IndexedWord newWord){
        this.wordList.set(i, newWord);
    }
    
    /**
     * Given a typed dependency, add it to the list of typed dependencies
     * @param t: typed dependency to be added
     */
    public void addTypedDependency(TypedDependency t){
        this.tds.add(t);
    }
    
    /**
     * Given a list of words 'words', replace all of them with one word 'w'
     * @param words: the list of words to be replaced with one word
     * @param w: the word replacing the sublist
     */
    public void replaceWordsWithOneWord(ObjectArrayList<IndexedWord> words, IndexedWord w){
        // If the list of words is empty, return (nothing to replace)
        if (words.size() == 0)
            return;
        
        // Replace the first word from the list with the replacing word, then drop it from the list, then remove the rest
        int firstWordInd = this.getWordList().indexOf(words.get(0));
        if (firstWordInd == -1)
            return;
        this.setWordInWordList(firstWordInd, w);
        words.remove(0);
        if (words.size() > 0)
            this.removeWordsFromList(words);
    }
    
    /**
     * Given a sentence semantic graph, set the typed dependencies list of the phrase. For this to work, the list of 
     * words (this.wordlist) must be already known. Otherwise, the tds list will be empty. Each typed dependency in the list
     * must contain both the parent and the child in the wordslist. 
     * @param sg: sentence semantic graph (the phrase must be derived from this graph, i.e. all the nodes and edges of the 
     *            phrase must be found in this graph. Otherwise, the TDs list will be empty)
     */
    public void setTdsFromSentenceSemGraph(SemanticGraph sg){
        // If the semantic graph of the sentence or the list of words are empty, return
        if (sg.isEmpty() || this.wordList.isEmpty()){
            tds = new ObjectArrayList<TypedDependency>();
            return;
        }
            
        for (TypedDependency td: sg.typedDependencies()){
            if (this.wordList.contains(td.dep()) && this.wordList.contains(td.gov()))
                this.tds.add(td);
        }
    }
    
    /**
     * Return a string of words, containing the phrase, in the following format: word1 word2 ... wordn
     * @return a string in the format: word1 word2 ... wordn
     */
    public String getWords(){
        StringBuffer sbWords = new StringBuffer();
        
        for (IndexedWord word: this.wordList){
            sbWords.append(word.word());
            sbWords.append(SEPARATOR.SPACE);            
        }
        
        return sbWords.toString().trim();
    }
    
    /**
     * Return a string of words, containing the phrase, in the following format: word1 word2 ... wordn
     * The words are written in lowercase.
     * @return a string in the format: word1 word2 ... wordn
     */
    public String getWordsLowercase(){
        StringBuffer sbWords = new StringBuffer();
        
        for (IndexedWord word: this.wordList){
            sbWords.append(word.word().toLowerCase());
            sbWords.append(SEPARATOR.SPACE);            
        }
        
        return sbWords.toString().trim();
    }
    
    /**
     * Return a string of lemmas, containing the phrase, in the following format: lemma1 lemma2 ... lemman
     * The words are written in lowercase.
     * @return a string in the format: lemma1 lemma2 ... lemman
     */
    public String getWordsLemmaLowercase(){
        StringBuffer sbWords = new StringBuffer();
        
        for (IndexedWord word: this.wordList){
            sbWords.append(word.lemma().toLowerCase());
            sbWords.append(SEPARATOR.SPACE);            
        }
        
        return sbWords.toString().trim();
    }
    
    /**
     * Return a string of words from the phrase in the following format: word1 word2 ... wordn
     * The words are written in lowercase. 
     * @param from: starting index of the words returned
     * @param to: ending index of the words returned
     * @return a string in the format: word1 word2 ... wordn
     */
    public String getWordsLowercase(int from, int to){
        StringBuffer sbWords = new StringBuffer();
        
        for (; from < to; from++){
            sbWords.append(this.wordList.get(from).word().toLowerCase());
            sbWords.append(SEPARATOR.SPACE);            
        }
        
        return sbWords.toString().trim();
    }
    
    /**
     * Given the sentence semantic graph, detect the "root" of the phrase (i.e. the word with shortest path to the 
     * root word of the sentence)
     * @param sentenceSg: semantic graph of the sentence
     */
    public void detectRoot(SemanticGraph sentenceSg){
        this.root = CoreNLPUtils.getRootFromWordList(sentenceSg, this.wordList);
    }
    
    public void clear() {
        this.phraseGraph = new SemanticGraph();
        this.root = new IndexedWord();
        this.tds.clear();
        this.wordList.clear();
    }
}
