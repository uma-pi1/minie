package de.uni_mannheim.minie.main;

import de.uni_mannheim.clausie.ClausIE;
import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.utils.Dictionary;
import de.uni_mannheim.utils.minie.Utils;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import java.io.IOException;

/**
 * This class acts as a generic interface to the MinIE system
 *
 * @author Martin Achenbach
 * @author Kiril Gashteovski
 */
public class Extractor {
    private StanfordCoreNLP parser;
    private ClausIE clausIE;
    private MinIE minIE;
    private Dictionary dictionary;

    /**
     * default constructor
     */
    public Extractor() {
        // initialize the parser
        this.parser = CoreNLPUtils.StanfordDepNNParser();

        // initialize ClausIE
        this.clausIE = new ClausIE();

        // initialize MinIE
        this.minIE = new MinIE();

        // set up default dictionary
        try {
            this.setDictionary(new Dictionary(Utils.DEFAULT_DICTIONARIES));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * constructor with dictionary, helpful when running in dictionary mode
     * @param dictionary: dictionary
     */
    public Extractor(Dictionary dictionary) {
        // initialize the parser
        this.parser = CoreNLPUtils.StanfordDepNNParser();

        // initialize ClausIE
        this.clausIE = new ClausIE();

        // initialize MinIE
        this.minIE = new MinIE();

        // set dictionary
        this.setDictionary(dictionary);
    }

    /**
     * set the dictionary for dictionary mode
     * @param dictionary: dictionary to use
     */
    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * analyze a sentence using a specific mode
     * @param sentence: sentence to analyze
     * @param mode: MinIE mode
     * @return the results of MinIE
     */
    public MinIE analyzeSentence(String sentence, MinIE.Mode mode) {
        // first reset objects
        this.clausIE.clear();
        this.minIE.clear();

        // parse the sentence
        this.clausIE.setSemanticGraph(CoreNLPUtils.parse(this.parser, sentence));
        // detect clauses
        this.clausIE.detectClauses();
        // generate propositions
        this.clausIE.generatePropositions(this.clausIE.getSemanticGraph());

        // start minimizing
        this.minIE.setSemanticGraph(this.clausIE.getSemanticGraph());
        this.minIE.setPropositions(this.clausIE);
        this.minIE.setPolarity();
        this.minIE.setModality();
        
        // minimize in given mode
        switch (mode) {
            case AGGRESSIVE:
                this.minIE.minimizeAggressiveMode();
                break;
            case DICTIONARY:
                this.minIE.minimizeDictionaryMode(this.dictionary.words());
                break;
            case SAFE:
                this.minIE.minimizeSafeMode();
                break;
            case COMPLETE:
                break;
        }
        // remove duplicates
        this.minIE.removeDuplicates();
        return this.minIE;
    }
}
