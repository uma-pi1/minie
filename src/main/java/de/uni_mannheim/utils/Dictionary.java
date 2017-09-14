package de.uni_mannheim.utils;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import edu.stanford.nlp.ling.IndexedWord;

/**
 * A dictionary stores a set of strings.
 *
 * @author Kiril Gashteovski
 */
public class Dictionary {

    /** Stores the strings */
    public ObjectOpenHashSet<String> words;

    /** Default constructor **/
    public Dictionary() {
        this.words = new ObjectOpenHashSet<String>();
    }

    /** Opens an empty set of strings (the dictionary) and then loads the dictionary from the input stream **/
    public Dictionary(InputStream in) throws IOException {
        this.words = new ObjectOpenHashSet<String>();
        this.load(in);
    }

    /** Opens an empty set of strings (the dictionary) and then loads the dictionary from the resource path **/
    public Dictionary(String resourcePath) throws IOException {
        this.words = new ObjectOpenHashSet<String>();
        this.load(resourcePath);
    }

    /** Opens an empty set of strings (the dictionary) and then loads the dictionary from the multiple resources 
      * @throws IOException **/
    public Dictionary(String [] resourcePaths) throws IOException {
        this.words = new ObjectOpenHashSet<String>();
        this.load(resourcePaths);
    }

    /** The size of the dictionary (number of words) **/
    public int size() {
        return this.words.size();
    }

    /** Checks if a certain word (as a string) is in the dictionary **/
    public boolean contains(String word) {
        return this.words.contains(word);
    }

    /** Checks if a certain word (IndexedWord object) is in the dictionary in its lemmatized form **/
    public boolean containsLemmatized(IndexedWord word) {
        return this.words.contains(word.lemma());
    }

    private InputStream getInputStreamFromResource(String resourceName) throws IOException {
        return this.getClass().getResource(resourceName).openStream();
    }

    /** Loads a dictionary from a resource path
     * @throws IOException 
     **/
    public void load(String resourcePath) throws IOException {
        this.load(this.getInputStreamFromResource(resourcePath));
    }
    
    /** Loads a dictionary from several resource paths 
     * @throws IOException **/
    public void load(String [] resourcePaths) throws IOException {
        for (String path: resourcePaths) {
            this.load(path);
        }
    }
    
    /** Loads the dictionary out of an {@link InputStream}. 
     *  Each line of the original file should contain an entry to the dictionary
     */
    public void load(InputStream in) throws IOException {
        DataInput data = new DataInputStream(in);
        String line = data.readLine();
        while (line != null) {
            line = line.trim();
            if (line.length() > 0) {
                this.words.add(line);
            }
            line = data.readLine();
        }
    }
	
    /** Get the set of words **/
    public ObjectOpenHashSet<String> words() {
        return this.words;
    }
	
    /** Add entries to the dictionary **/
    public void addWords(ObjectOpenHashSet<String> ws) {
        this.words.addAll(ws);
    }
}
