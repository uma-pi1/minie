package de.uni_mannheim.utils.coreNLP;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.semgraph.SemanticGraphFactory;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.EnglishGrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Generics;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import de.uni_mannheim.constant.CHARACTER;
import de.uni_mannheim.constant.NE_TYPE;
import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.constant.SEPARATOR;
import de.uni_mannheim.constant.WORDS;
import de.uni_mannheim.utils.fastutils.FastUtil;

/**
 * @author Kiril Gashteovski
 */
public class CoreNLPUtils {
    /**
     * Initializes and returns StanfordCoreNLP pipeline
     * @return StanfordCoreNLP pipeline
     */
    public static StanfordCoreNLP StanfordDepNNParser(){
        Properties props = new Properties();

        props.put("language", "english");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
        props.put("depparse.model", "edu/stanford/nlp/models/parser/nndep/english_SD.gz");
        props.put("parse.originalDependencies", true);

        StanfordCoreNLP pipeline =  new StanfordCoreNLP(props);

        return pipeline;
    }
    
    /**
     * Given a CoreNLP pipeline and an input sentence, generate dependency parse for the sentence and return
     * the SemanticGraph object as a result
     * @param pipeline - CoreNLP pipeline
     * @param snt - input sentence
     * @return dependency parse in SemanticGraph object
     */
    public static SemanticGraph parse(StanfordCoreNLP pipeline, String snt) {
        Annotation document = new Annotation(snt);
        pipeline.annotate(document);
        
        //A CoreMap is a sentence with annotations
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        SemanticGraph semanticGraph = null;
        for(CoreMap sentence: sentences) {
            semanticGraph = sentence.get(BasicDependenciesAnnotation.class);
        }
        
        return semanticGraphUniversalEnglishToEnglish(semanticGraph);
    }
    
    /**
     * Given a sequence of indexed words, return a string in the format "[POS1|NER1] [POS2|NER2] ... [POSn|NERn]"
     * If a given word has a NER type -> write the type, else -> write the POS tag. 
     * When we have a verb, noun, adverb,...unify them under a "common" POS tag (e.g:VB for all verbs, NN for all nouns,etc.)
     * @param words: a list of indexed words
     * @return a string in the format "[POS1|NER1] [POS2|NER2] ... [POSn|NERn]"
     */
    public static String wordsToPosMergedNerSeq(ObjectArrayList<IndexedWord> words){
        StringBuffer sbSeq = new StringBuffer();
        for (int i = 0; i < words.size(); i++){
            if (words.get(i).ner().equals(NE_TYPE.NO_NER)){
                if (isAdj(words.get(i).tag()))
                    sbSeq.append(POS_TAG.JJ);
                else if (isAdverb(words.get(i).tag()))
                    sbSeq.append(POS_TAG.RB);
                else if (isNoun(words.get(i).tag()))
                    sbSeq.append(POS_TAG.NN);
                else if (isPronoun(words.get(i).tag()))
                    sbSeq.append(POS_TAG.PR);
                else if (isVerb(words.get(i).tag()))
                    sbSeq.append(POS_TAG.VB);
                else if (isWhPronoun(words.get(i).tag()))
                    sbSeq.append(POS_TAG.WP);
                else sbSeq.append(words.get(i).tag());
                    
                sbSeq.append(SEPARATOR.SPACE);
            } else {
                sbSeq.append(words.get(i).ner());
                sbSeq.append(SEPARATOR.SPACE);
            }
        }
        return sbSeq.toString().trim();
    }
    
    
    /**
     * Given a sequence of indexed words, return a string in the format "[POS1] [POS2] ... [POSn]"
     * Same as "wordsToPosMergedNerSeq", the difference being that this function returns sequence of POS tags only 
     * (ignores the NER types)  
     * When we have a verb, noun, adverb,...unify them under a "common" POS tag (e.g:VB for all verbs, NN for all nouns,etc.)
     * @param words: a list of indexed words
     * @return a string in the format "[POS1] [POS2] ... [POSn]"
     */
    public static String wordsToPosMergedSeq(ObjectArrayList<IndexedWord> words){
        StringBuffer sbSeq = new StringBuffer();
        for (int i = 0; i < words.size(); i++){
            if (isAdj(words.get(i).tag()))
                sbSeq.append(POS_TAG.JJ);
            else if (isAdverb(words.get(i).tag()))
                sbSeq.append(POS_TAG.RB);
            else if (isNoun(words.get(i).tag()))
                sbSeq.append(POS_TAG.NN);
            else if (isPronoun(words.get(i).tag()))
                sbSeq.append(POS_TAG.PR);
            else if (isVerb(words.get(i).tag()))
                sbSeq.append(POS_TAG.VB);
            else if (isWhPronoun(words.get(i).tag()))
                sbSeq.append(POS_TAG.WP);
            else sbSeq.append(words.get(i).tag());
                    
            sbSeq.append(SEPARATOR.SPACE); 
        }
        return sbSeq.toString().trim();
    }
    
    /**
     * Given a list of indexed words and a semantic graph, return the root word of the word list. We assume that
     * all the words from the list can be found in the semantic graph sg, and the words in wordList are connected
     * within the semantic graph of the sentence - sg, and that they all share a common root.
     * @param sg: semantic graph of the sentence
     * @param wordList: the phrase from the sentence, represented as a list of words
     * @return the root word from the phrase
     */
    public static IndexedWord getRootFromWordList(SemanticGraph sg, ObjectArrayList<IndexedWord> wordList){
        // If the word list is consisted of one word - return that word
        if (wordList.size() == 1) return wordList.get(0);
        
        IndexedWord constituentRoot = null;
        
        // We only search as high as grandparents
        // constituentRoot = sg.getCommonAncestor(wordList.get(0), wordList.get(wordList.size()-1));

        // If the commonancestor is deeper in the tree, the constituent root is the word with shortest distance
        // to the root of the sentence
        int minPathToRoot = Integer.MAX_VALUE;
        int pathToRoot = -1;
        for (int i = 0; i < wordList.size(); i++){
            // The words with index -2 are the ones that cannot be found in the semantic graph (synthetic words)
            // This happens in the relations (see in clausie.ClauseDetector.java), and those words are the head words
            if (wordList.get(i).index() == -2){
                return wordList.get(i);
            }
            pathToRoot = sg.getShortestDirectedPathNodes(sg.getFirstRoot(), wordList.get(i)).size();
            if (pathToRoot < minPathToRoot){ //TODO: throws NPE sometimes
                minPathToRoot = pathToRoot;
                constituentRoot = wordList.get(i);
            }
        }

        return constituentRoot;
    }
    
    /**
     * Given a list of core maps (each core map beeing a word) and a semantic graph of the sentence, return the 
     * root word of the word list (i.e. the one which is closest to the root of the semantic graph). We assume that
     * all the words from the list can be found in the semantic graph sg, and the words in wordList are connected
     * within the semantic graph of the sentence - sg, and that they all share a common root.
     * 
     * @param sg: semantic graph of the sentence
     * @param wordList: the phrase from the sentence, represented as a list of words
     * @return the root word from the phrase
     */
    public static IndexedWord getRootFromCoreMapWordList(SemanticGraph sg, List<CoreMap> wordList){
        ObjectArrayList<IndexedWord> indWordList = CoreNLPUtils.listOfCoreMapWordsToIndexedWordList(wordList);
        return getRootFromWordList(sg, indWordList);
    }
    
    /**
     * Given a list of indexed words and a semantic graph, return the root word of the word list. We assume that
     * all the words from the list can be found in the semantic graph sg, and the words in wordList are connected
     * within the semantic graph of the sentence - sg. If there are multiple words which have the shortest distance
     * to the sentence root, then choose the most-left verb. 
     * 
     * @param sg: sentence semantic graph
     * @param wordsList: list of words from which to choose "root" from
     * @return
     */
    public static IndexedWord getVerbRootFromWordList(SemanticGraph sg, ObjectArrayList<IndexedWord> wordList){
        IndexedWord constituentRoot = null;
        IntArrayList shortestDirectedPathDistances = new IntArrayList();
        
        int minPathToRoot = Integer.MAX_VALUE;
        int pathToRoot = -1;
        
        for (int i = 0; i < wordList.size(); i++){
            // The words with index -2 are the ones that cannot be found in the semantic graph (synthetic words)
            // This happens in the relations (see in clausie.ClauseDetector.java), and those words are the head words
            if (wordList.get(i).index() == -2){
                return wordList.get(i);
            }
            pathToRoot = sg.getShortestDirectedPathNodes(sg.getFirstRoot(), wordList.get(i)).size();
            if (pathToRoot < minPathToRoot){
                minPathToRoot = pathToRoot;
            }
            shortestDirectedPathDistances.add(pathToRoot);
        }
        
        // If the shortest path is one element, return it, else, return the first verb containing that index
        if (FastUtil.countElement(minPathToRoot, shortestDirectedPathDistances) == 1)
            return wordList.get(shortestDirectedPathDistances.indexOf(minPathToRoot));
        else {
            for (int i = 0; i < shortestDirectedPathDistances.size(); i++){
                if (shortestDirectedPathDistances.getInt(i) == minPathToRoot){
                    if (isVerb(wordList.get(i).tag())){
                        constituentRoot = wordList.get(i);
                        break;
                    }
                }
            }
        }
        
        return constituentRoot;
    }
    
    /**
     * Given a semantic graph of a whole sentence (sg) and a "local root" node, get the subgraph from 'sg' which has 
     * 'localRoot' as a root. 
     * @param sg: semantic graph of the whole sentence
     * @param localRoot: the root of the subgraph
     * @return semantic graph object which is the subgraph from 'sg'
     */
    public static SemanticGraph getSubgraph(SemanticGraph sg, IndexedWord localRoot){
        ObjectArrayList<TypedDependency> subGraphDependencies = getSubgraphTypedDependencies(sg, localRoot, 
                                                                            new ObjectArrayList<TypedDependency>());
        TreeGraphNode rootTGN = new TreeGraphNode(new CoreLabel(localRoot));
        EnglishGrammaticalStructure gs = new EnglishGrammaticalStructure(subGraphDependencies, rootTGN);
        return SemanticGraphFactory.generateUncollapsedDependencies(gs);
    }
    private static ObjectArrayList<TypedDependency> getSubgraphTypedDependencies(SemanticGraph sg, IndexedWord parent, 
            ObjectArrayList<TypedDependency> tds){
        Set<IndexedWord> children = sg.getChildren(parent);
        
        for (IndexedWord child: children){
            GrammaticalRelation gRel = sg.getEdge(parent, child).getRelation();
            tds.add(new TypedDependency(gRel, parent, child));
            if (sg.hasChildren(child))
                getSubgraphTypedDependencies(sg, child, tds);
        }
        
        return tds; 
    }
    
    /**
     * Given the sentence semantic graph and a list of words, get a subgraph containing just the words in the list
     * 'words'. Each typed dependency has each word from the list as a governor.
     * @param sg: sentence semantic graph
     * @param words: list of words which should contain the semantic graph
     * @return subgraph containing the words from 'words'
     * TODO: this needs to be double checked! In some cases we have weird graphs, where there are words missing. 
     * E.g. the sentence 120 from NYT "The International ... ". Try this for getting the subgraph when the source is 
     * detected.
     */
    public static SemanticGraph getSubgraphFromWords(SemanticGraph sg, ObjectArrayList<IndexedWord> words){        
        // Determining the root
        int minInd = Integer.MAX_VALUE;
        IndexedWord root = new IndexedWord();
        for (IndexedWord w: words){
            if (w.index() < minInd){
                minInd = w.index();
                root = w;
            }
        }
        
        // Getting the typed dependency
        ObjectArrayList<TypedDependency> tds = new ObjectArrayList<TypedDependency>();
        for (TypedDependency td: sg.typedDependencies()){
            if (words.contains(td.gov()) && words.contains(td.dep()))
                tds.add(td);
        }
        
        // Create the semantic graph
        TreeGraphNode rootTGN = new TreeGraphNode(new CoreLabel(root));
        EnglishGrammaticalStructure gs = new EnglishGrammaticalStructure(tds, rootTGN);
        SemanticGraph phraseSg = SemanticGraphFactory.generateUncollapsedDependencies(gs);
        
        return phraseSg;
    }
    
    
    /** 
     * Given a semantic graph and a node which is part of the graph, return the constituent subgraph which has as root
     * constituentRoot
     * @param sg: the semantic graph from which the constituent sub-graph should be derived
     * @param constituentRoot: the root node for the constituent
     * @return the subgraph with constituentRoot as a root
     */
    public static SemanticGraph getSubgraph(SemanticGraph sg, IndexedWord constituentRoot, 
            ObjectArrayList<IndexedWord> words){
        int maxPathLength = -1;
        int pathLength;
        for (IndexedWord word: words){
            pathLength = sg.getShortestDirectedPathEdges(sg.getFirstRoot(), word).size();
            if (pathLength > maxPathLength)
                maxPathLength = pathLength;
        }
        ObjectArrayList<TypedDependency> tds = new ObjectArrayList<TypedDependency>();
        return getSubgraph(tds, sg, constituentRoot, null, maxPathLength, words);
    }
    private static SemanticGraph getSubgraph(ObjectArrayList<TypedDependency> tds, SemanticGraph sg, IndexedWord parent,
            SemanticGraphEdge e, int maxPathLength, ObjectArrayList<IndexedWord> words){
        Set<IndexedWord> children = sg.getChildren(parent);
        
        for (IndexedWord child: children){
            if (((sg.getShortestDirectedPathEdges(sg.getFirstRoot(), child)).size() <= maxPathLength) &&
                    words.contains(child)){   
                e = sg.getEdge(parent, child);
                tds.add(new TypedDependency(e.getRelation(), parent, child));
                if (sg.hasChildren(child))
                    getSubgraph(tds, sg, child, e, maxPathLength, words);
            } // else break;
        }

        TreeGraphNode rootTGN = new TreeGraphNode(new CoreLabel(parent));
        EnglishGrammaticalStructure gs = new EnglishGrammaticalStructure(tds, rootTGN);
        return SemanticGraphFactory.generateUncollapsedDependencies(gs);
    }
    
    /**
     * Given a pivot word and a list of words, return a list of "chained words" (i.e. words with same tags, or NERs 
     * to the left and right of the pivot word in the list).
     * @param pivot: the pivot word being examined
     * @param words: list of words from which the pivot word is part of
     * @return
     */
    public static ObjectArrayList<IndexedWord> getChainedWords(IndexedWord pivot, ObjectArrayList<IndexedWord> words){    
        // TODO: double check how we generate chained words (considering the NERs)
        // In case the pivot word is not in the list - return empty list
        if (words.indexOf(pivot) == -1)
            return new ObjectArrayList<>();
        
        ObjectArrayList<IndexedWord> chainedWords = new ObjectArrayList<>();
        if (!pivot.ner().equals(NE_TYPE.NO_NER)) 
            chainedWords = getChainedNERs(words, words.indexOf(pivot));
        else if (CoreNLPUtils.isNoun(pivot.tag()))
            chainedWords = getChainedNouns(words, words.indexOf(pivot));
        else chainedWords = getChainedTagNoNER(words, words.indexOf(pivot));
        return chainedWords;
    }
    
    
    /**
     * Given a sequence of words and a pivot-word index, return the chained nouns from the left and from the right
     * of the pivot word.  
     * @param sequence: a sequence of words (list of IndexedWord)
     * @param wordInd: the index of the pivot word
     * @return a list of chained nouns to the left and the right of the pivot word (the pivot word is included)
     */
    public static ObjectArrayList<IndexedWord> getChainedNouns(ObjectArrayList<IndexedWord> sequence, int wordInd){
        IntArrayList chainedNounsInd = new IntArrayList();
        
        // Get the chained nouns from left and right
        IntArrayList chainedNounsLeft = getChainedNounsFromLeft(sequence, chainedNounsInd.clone(), wordInd);
        IntArrayList chainedNounsRight = getChainedNounsFromRight(sequence, chainedNounsInd.clone(), wordInd);
        
        // Add all the words to the chained nouns
        chainedNounsInd.addAll(chainedNounsLeft);
        chainedNounsInd.add(wordInd);
        chainedNounsInd.addAll(chainedNounsRight);
        
        // IndexedWord chained nouns
        ObjectArrayList<IndexedWord> iChainedNouns = new ObjectArrayList<IndexedWord>();
        for (int i: FastUtil.sort(chainedNounsInd)){
            iChainedNouns.add(sequence.get(i));
        }
        
        return iChainedNouns;
    }
    /**
     * Given a sequence of indexed words and a noun, get all the nouns 'chained' to the word from the left.
     * @param sequence: a list of words
     * @param wordInd: the word index from where the search starts 
     * @return a list of nouns which precede 'word'
     */
    private static IntArrayList getChainedNounsFromLeft(ObjectArrayList<IndexedWord> sequence, 
            IntArrayList chainedNouns, int wordInd){
        // If the word is the leftiest word or it's not a noun - return
        if (wordInd > 0 && isNoun(sequence.get(wordInd-1).tag())){
            chainedNouns.add(wordInd-1);
            getChainedNounsFromLeft(sequence, chainedNouns, wordInd-1);
        }
        
        return chainedNouns;
    }
    /**
     * Given a sequence of indexed words and a noun, get all the nouns 'chained' to the word from the right.
     * @param sequence: a list of words
     * @param wordInd: the word index from where the search starts 
     * @return a list of nouns which precede 'word'
     */
    private static IntArrayList getChainedNounsFromRight(ObjectArrayList<IndexedWord> sequence, 
            IntArrayList chainedNouns, int wordInd){
        // If the word is the rightiest word or it's not a noun - return
        if (wordInd < sequence.size()-1 && isNoun(sequence.get(wordInd+1).tag())){
            chainedNouns.add(wordInd + 1);
            getChainedNounsFromRight(sequence, chainedNouns, wordInd + 1);
        }
        
        return chainedNouns;
    }
    
    /**
     * Given a sequence of words and a pivot-word index, return the chained verbs from the left and from the right
     * of the pivot word.  
     * @param sequence: a sequence of words (list of IndexedWord)
     * @param wordInd: the index of the pivot word
     * @return a list of chained verbs to the left and the right of the pivot word (the pivot word is included)
     */
    public static ObjectArrayList<IndexedWord> getChainedVerbs(ObjectArrayList<IndexedWord> sequence, int wordInd){
        IntArrayList chainedVerbsInd = new IntArrayList();
        
        // Get the chained verbs from left and right
        IntArrayList chainedVerbsLeft = getChainedVerbsFromLeft(sequence, chainedVerbsInd.clone(), wordInd);
        IntArrayList chainedVerbsRight = getChainedVerbsFromRight(sequence, chainedVerbsInd.clone(), wordInd);
        
        // Add all the words to the chained verbs
        chainedVerbsInd.addAll(chainedVerbsLeft);
        chainedVerbsInd.add(wordInd);
        chainedVerbsInd.addAll(chainedVerbsRight);
        
        // IndexedWord chained verbs
        ObjectArrayList<IndexedWord> iChainedVerbs = new ObjectArrayList<IndexedWord>();
        for (int i: FastUtil.sort(chainedVerbsInd)){
            iChainedVerbs.add(sequence.get(i));
        }
        
        return iChainedVerbs;
    }
    /**
     * Given a sequence of indexed words and a verb, get all the verbs 'chained' to the word from the left.
     * @param sequence: a list of words
     * @param wordInd: the word index from where the search starts 
     * @return a list of verbs which precede 'word'
     */
    private static IntArrayList getChainedVerbsFromLeft(ObjectArrayList<IndexedWord> sequence, 
            IntArrayList chainedVerbs, int wordInd){
        // If the word is the leftiest word or it's not a verb - return
        if (wordInd > 0 && isVerb(sequence.get(wordInd - 1).tag())){
            chainedVerbs.add(wordInd-1);
            getChainedVerbsFromLeft(sequence, chainedVerbs, wordInd-1);
        }
        
        return chainedVerbs;
    }
    /**
     * Given a sequence of indexed words and a verb, get all the verbs 'chained' to the word from the right.
     * @param sequence: a list of words
     * @param wordInd: the word index from where the search starts 
     * @return a list of verbs which precede 'word'
     */
    private static IntArrayList getChainedVerbsFromRight(ObjectArrayList<IndexedWord> sequence, 
            IntArrayList chainedVerbs, int wordInd){
        // If the word is the rightiest word or it's not a verb - return
        if (wordInd < sequence.size()-1 && isVerb(sequence.get(wordInd + 1).tag())){
            chainedVerbs.add(wordInd + 1);
            getChainedVerbsFromRight(sequence, chainedVerbs, wordInd + 1);
        }
        
        return chainedVerbs;
    }
    
    
    /**
     * Given a sequence of words and a pivot-word index, return the "chained words" from the left and from the right
     * of the pivot word. "Chained words" are a list of words, which all of them share the same POS tag and have no 
     * NE types.
     * 
     * @param sequence: a sequence of words (list of IndexedWord)
     * @param wordInd: the index of the pivot word
     * @return a list of chained words to the left and the right of the pivot word (the pivot word is included)
     */
    public static ObjectArrayList<IndexedWord> getChainedTagNoNER(ObjectArrayList<IndexedWord> sequence, int wordInd){
        IntArrayList chainedPosWordsInd = new IntArrayList();
        
        // Get the chained nouns from left and right
        IntArrayList chainedPosWordsLeft = getChainedTagsFromLeftNoNER(sequence, chainedPosWordsInd.clone(), wordInd);
        IntArrayList chainedPosWordsRight = getChainedTagsFromRightNoNER(sequence, chainedPosWordsInd.clone(), wordInd);
        
        // Add all the words to the chained nouns
        chainedPosWordsInd.addAll(chainedPosWordsLeft);
        chainedPosWordsInd.add(wordInd);
        chainedPosWordsInd.addAll(chainedPosWordsRight);
        
        // IndexedWord chained nouns
        ObjectArrayList<IndexedWord> iChainedNouns = new ObjectArrayList<IndexedWord>();
        for (int i: FastUtil.sort(chainedPosWordsInd)){
            iChainedNouns.add(sequence.get(i));
        }
        
        return iChainedNouns;
    }
    /**
     * Given a sequence of indexed words and a pivot, get all the words 'chained' to the word from the left (i.e. having
     * the same POS tag as the pivot word). Also, the chained words should not have NE types.
     * 
     * @param sequence: a list of words
     * @param wordInd: the word index from where the search starts 
     * @return a list of words which precede 'word'
     */
    private static IntArrayList getChainedTagsFromLeftNoNER(ObjectArrayList<IndexedWord> sequence, 
            IntArrayList chainedPosWords, int wordInd){
        // If the word is the leftiest word or it's not with the same POS tag - return
        if (wordInd > 0 && sequence.get(wordInd).tag().equals(sequence.get(wordInd-1).tag()) && 
                sequence.get(wordInd-1).ner().equals(NE_TYPE.NO_NER)){
            chainedPosWords.add(wordInd-1);
            getChainedTagsFromLeftNoNER(sequence, chainedPosWords, wordInd-1);
        }
        
        return chainedPosWords;
    }
    /**
     * Given a sequence of indexed words and a noun, get all the nouns 'chained' to the word from the right.
     * Also, the chained nouns should not have NE types.
     * @param sequence: a list of words
     * @param wordInd: the word index from where the search starts 
     * @return a list of nouns which preced 'word'
     */
    private static IntArrayList getChainedTagsFromRightNoNER(ObjectArrayList<IndexedWord> sequence, 
            IntArrayList chainedNouns, int wordInd){
        // If the word is the rightiest word or it's not a noun - return
        if (wordInd < sequence.size()-1 && sequence.get(wordInd).tag().equals(sequence.get(wordInd+1).tag()) && 
                sequence.get(wordInd+1).ner().equals(NE_TYPE.NO_NER)){
            chainedNouns.add(wordInd + 1);
            getChainedTagsFromRightNoNER(sequence, chainedNouns, wordInd + 1);
        }
        
        return chainedNouns;
    }
    
    
    /**
     * Given a sequence of words and a pivot-word index, return the chained words of same NER, both from the left and 
     * from the right of the pivot word (it is assumed that the pivot word is also NER).  
     * @param sequence: a sequence of words (list of IndexedWord)
     * @param wordInd: the index of the pivot word
     * @return a list of chained nouns to the left and the right of the pivot word (the pivot word is included)
     */
    public static ObjectArrayList<IndexedWord> getChainedNERs(ObjectArrayList<IndexedWord> sequence, int wordInd){
        IntArrayList chainedNounsInd = new IntArrayList();
        
        // Get the chained nouns from left and right
        IntArrayList chainedNounsLeft = getChainedNERsFromLeft(sequence, chainedNounsInd.clone(), wordInd, 
                                                               sequence.get(wordInd).ner());
        IntArrayList chainedNounsRight = getChainedNERsFromRight(sequence, chainedNounsInd.clone(), wordInd,
                                                                 sequence.get(wordInd).ner());
        
        // Add all the words to the chained nouns
        chainedNounsInd.addAll(chainedNounsLeft);
        chainedNounsInd.add(wordInd);
        chainedNounsInd.addAll(chainedNounsRight);
        
        // IndexedWord chained nouns
        ObjectArrayList<IndexedWord> iChainedNouns = new ObjectArrayList<IndexedWord>();
        for (int i: FastUtil.sort(chainedNounsInd)){
            iChainedNouns.add(sequence.get(i));
        }
        
        return iChainedNouns;
    }
    /**
     * Given a sequence of indexed words and a NER word, get all the NERs 'chained' to the word from the left (they all 
     * must have the same NER).
     * @param sequence: a list of words
     * @param wordInd: the word index from where the search starts (the pivot word)
     * @param ner: the NE type of the pivot word
     * @return a list of nouns which preced 'word'
     */
    private static IntArrayList getChainedNERsFromLeft(ObjectArrayList<IndexedWord> sequence, 
            IntArrayList chainedNERs, int wordInd, String ner){
        // If the word is the leftiest word or it's not a noun - return
        if (wordInd > 0 && sequence.get(wordInd-1).ner().equals(ner)){
            chainedNERs.add(wordInd-1);
            getChainedNERsFromLeft(sequence, chainedNERs, wordInd-1, ner);
        }
        
        return chainedNERs;
    }
    /**
     * Given a sequence of indexed words and a NER word, get all the NERs 'chained' to the word from the right (they all 
     * must have the same NER).
     * @param sequence: a list of words
     * @param wordInd: the word index from where the search starts (the pivot word)
     * @param ner: the NE type of the pivot word
     * @return a list of nouns which preced 'word'
     */
    private static IntArrayList getChainedNERsFromRight(ObjectArrayList<IndexedWord> sequence, 
            IntArrayList chainedNERs, int wordInd, String ner){
        // If the word is the rightiest word or it's not a noun - return
        if (wordInd < sequence.size()-1 && sequence.get(wordInd+1).ner().equals(ner)){
            chainedNERs.add(wordInd + 1);
            getChainedNERsFromRight(sequence, chainedNERs, wordInd + 1, ner);
        }
        
        return chainedNERs;
    }
    
    
    /**
     * Checks if a word is some kind of a verb (i.e. if it has POS tag: VB, VBD, VBG, VBN, VBP or VBZ)
     * @param word: String the POS tag of the word
     * @return true if it is a verb, false otherwise
     */
    public static boolean isVerb(String pos){
        return pos.equals(POS_TAG.VB) || pos.equals(POS_TAG.VBD) || pos.equals(POS_TAG.VBG) || 
                pos.equals(POS_TAG.VBN) || pos.equals(POS_TAG.VBP) || pos.equals(POS_TAG.VBZ);
    }
    
    /**
     * Checks if a word is some kind of a noun (i.e. if it has POS tag: NN, NNS, NNP or NNPS)
     * @param word: String the POS tag of the word
     * @return true if it is a noun, false otherwise
     */
    public static boolean isNoun(String pos){
        return pos.equals(POS_TAG.NN) ||pos.equals(POS_TAG.NNS) || pos.equals(POS_TAG.NNP) || 
                pos.equals(POS_TAG.NNPS);
    }
    
    /**
     * Checks if a word is some kind of an adjective (i.e. if it has POS tag: JJ, JJR or JJS)
     * @param word: String the POS tag of the word
     * @return true if it is an adjective, false otherwise
     */
    public static boolean isAdj(String pos){
        return pos.equals(POS_TAG.JJ) || pos.equals(POS_TAG.JJR) || pos.equals(POS_TAG.JJS);
    }
    
    /**
     * Checks if a word is some kind of an adverb (i.e. if it has POS tag: RB, RBR or RBS)
     * @param word: String the POS tag of the word
     * @return true if it is an adverb, false otherwise
     */
    public static boolean isAdverb(String pos){
        return pos.equals(POS_TAG.RB) || pos.equals(POS_TAG.RBR) || pos.equals(POS_TAG.RBS);
    }
    
    /**
     * Checks if a word is some kind of a pronoun (i.e. if it has POS tag: PRP or PRP$)
     * @param word: String the POS tag of the word
     * @return true if it is a pronoun, false otherwise
     */
    public static boolean isPronoun(String pos){
        return pos.equals(POS_TAG.PRP) || pos.equals(POS_TAG.PRP_P);
    }
    
    /**
     * Checks if a word is some kind of a wh-pronoun (i.e. if it has POS tag: WP or WP$)
     * @param word: String the POS tag of the word
     * @return true if it is a wh-pronoun, false otherwise
     */
    public static boolean isWhPronoun(String pos){
        return pos.equals(POS_TAG.WP) || pos.equals(POS_TAG.WP_P);
    }
    
    /**
     * Given a semantic graph, recreate the sentence and return it as a string.
     * @param sg: the semantic graph of the sentence
     * @return the sentence (as a string)
     */
    public static String semGraphToSentence(SemanticGraph sg){
        StringBuffer sbSentence = new StringBuffer();
        for (int i = 0; i < sg.size(); i++){
            sbSentence.append(sg.getNodeByIndex(i + 1).word() + SEPARATOR.SPACE);
        }
        return sbSentence.toString();
    }
    
    /**
     * Given a list of words, check if there is a verb in the list
     * @param words: list of indexed words
     * @return true -> if there is a verb in the list of words, false -> otherwise
     */
    public static boolean verbInList(ObjectArrayList<IndexedWord> words){
        for (IndexedWord word: words){
            if (isVerb(word.tag()))
                return true;
        }
        return false;
    }
    
    /**
     * Given a list of words, check if there is a noun in the list
     * @param words: list of indexed words
     * @return true -> if there is a noun in the list of words, false -> otherwise
     */
    public static boolean nounInList(ObjectArrayList<IndexedWord> words){
        for (IndexedWord word: words){
            if (isNoun(word.tag()))
                return true;
        }
        return false;
    }
    
    /**
     * Given a list of words, return the phrase of words as a whole string, separated with empty space
     * @param words: list of words (e.g. [Kiril, lives, in, Mannheim])
     * @return string of the list of words separated by space (e.g. it returns "Kiril lives in Mannheim")
     */
    public static String listOfWordsToWordsString(ObjectArrayList<IndexedWord> words){
        StringBuffer sbSentence = new StringBuffer();
        for (int i = 0; i < words.size(); i++){
            sbSentence.append(words.get(i).word());
            sbSentence.append(SEPARATOR.SPACE);
        }
        return sbSentence.toString().trim();
    }
    
    /**
     * Given a list of indexed words, return a list of strings, which contain the words
     * @param words: list of indexed words
     * @return list of strings (the words from 'words')
     */
    public static ObjectArrayList<String> listOfWordsToWordsStringList(ObjectArrayList<IndexedWord> words){
        ObjectArrayList<String> stWords = new ObjectArrayList<String>();
        for (int i = 0; i < words.size(); i++){
            stWords.add(words.get(i).word());
        }
        return stWords;
    }
    
    /**
     * Given a list of words (as core maps), return the phrase of words as a whole string, separated with empty space
     * @param words: list of words (e.g. [She, is, pretty])
     * @return string of the list of words separated by space (e.g. it returns "She is pretty")
     */
    public static String listOfCoreMapWordsToWordString(List<CoreMap> cmList){
        StringBuffer sbSentence = new StringBuffer();
        CoreLabel cl;
        for (CoreMap cm: cmList){
            cl = new CoreLabel(cm);
            sbSentence.append(cl.word().toLowerCase());
            sbSentence.append(SEPARATOR.SPACE);
        }
        return sbSentence.toString().trim();
    }
    
    /**
     * Given a list of words (as core maps), return the phrase of words as a list of indexed word objects
     * @param words: list of words (e.g. [She, is, pretty])
     * @return list of words (as IndexedWord)
     */
    public static ObjectArrayList<IndexedWord> listOfCoreMapWordsToIndexedWordList(List<CoreMap> cmList){
        ObjectArrayList<IndexedWord> wordList = new ObjectArrayList<>();
        for (CoreMap cm: cmList){
            wordList.add(new IndexedWord(new CoreLabel(cm)));
        }
        return wordList;
    }

    /**
     *
     */
    public static ObjectArrayList<SemanticGraphEdge> listOfIndexedWordsToParentEdges(SemanticGraph semanticGraph, ObjectArrayList<IndexedWord> wordList) {
        ObjectArrayList<SemanticGraphEdge> result = new ObjectArrayList<>();
        for (IndexedWord word: wordList) {
            if (!semanticGraph.containsVertex(word)) continue;
            SemanticGraphEdge edge = semanticGraph.getEdge(semanticGraph.getParent(word), word);
            result.add(edge);
        }
        return result;
    }

    /**
     *
     */
    public static ObjectArrayList<SemanticGraphEdge> listOfIndexedWordsToParentEdges(SemanticGraph semanticGraph, ObjectOpenHashSet<IndexedWord> wordList) {
        ObjectArrayList<SemanticGraphEdge> result = new ObjectArrayList<>();
        for (IndexedWord word: wordList) {
            SemanticGraphEdge edge = semanticGraph.getEdge(semanticGraph.getParent(word), word);
            result.add(edge);
        }
        return result;
    }

    public static ObjectArrayList<SemanticGraphEdge> listOfCoreMapWordsToParentEdges(SemanticGraph semanticGraph, List<CoreMap> cmList) {
        return listOfIndexedWordsToParentEdges(semanticGraph, listOfCoreMapWordsToIndexedWordList(cmList));
    }

    /**
     * Given a list of words, return the phrase of words' lemmas as a whole string, separated with empty space
     * @param words: list of words (e.g. [She, is, pretty])
     * @return string of the list of words separated by space (e.g. it returns "She be pretty")
     */
    public static String listOfWordsToLemmaString(ObjectArrayList<IndexedWord> words){
        StringBuffer sbSentence = new StringBuffer();
        for (int i = 0; i < words.size(); i++){
            sbSentence.append(words.get(i).lemma());
            sbSentence.append(SEPARATOR.SPACE);
        }
        return sbSentence.toString().trim();
    }
    
    /**
     * Given a list of indexed words 'words', return an integer list of indices of the words
     * @param words: list of indexed words
     * @return list of indices of the words
     */
    public static IntArrayList listOfWordsToIndexList(ObjectArrayList<IndexedWord> words){
        IntArrayList indices = new IntArrayList();
        for (IndexedWord word: words){
            indices.add(word.index());
        }
        return indices;
    }
    
    /**
     * Given a list of words (as core maps), return the phrase of words' lemmas as a whole string, separated with empty space
     * @param words: list of words (e.g. [She, is, pretty])
     * @return string of the list of words separated by space (e.g. it returns "She be pretty")
     */
    public static String listOfCoreMapWordsToLemmaString(List<CoreMap> cmList){
        StringBuffer sbSentence = new StringBuffer();
        CoreLabel cl;
        for (CoreMap cm: cmList){
            cl = new CoreLabel(cm);
            sbSentence.append(cl.lemma().toLowerCase());
            sbSentence.append(SEPARATOR.SPACE);
        }
        return sbSentence.toString().trim();
    }
    
    /**
     * Given a list of words (as core maps), return the phrase of words' lemmas as a list of strings, which are the lemmas
     * of each word, lowercased.
     * @param cmList: list of core maps
     * @return list of strings, the lemmas of each word, lowercased
     */
    public static ObjectArrayList<String> listOfCoreMapWordsToLemmaStringList(List<CoreMap> cmList){
        CoreLabel cl;
        ObjectArrayList<String> lemmaList = new ObjectArrayList<>();
        for (CoreMap cm: cmList){
            cl = new CoreLabel(cm);
            lemmaList.add(cl.lemma().toLowerCase());
        }
        return lemmaList;
    }
    
    /**
     * Given a list of indexed words, return a list of strings, which contain the lemmas of the words
     * @param words: list of indexed words
     * @return list of strings (the lemmas of the words from 'words')
     */
    public static ObjectArrayList<String> listOfWordsToLemmasStringList(ObjectArrayList<IndexedWord> words){
        ObjectArrayList<String> stWords = new ObjectArrayList<String>();
        for (int i = 0; i < words.size(); i++){
            stWords.add(words.get(i).lemma());
        }
        return stWords;
    }
    
    /**
     * Given a starting vertice, grabs the subtree encapsulated by portion of the semantic graph, excluding
     * a given edge.  A tabu list is maintained, in order to deal with cyclical relations (such as between a
     * rcmod (relative clause) and its nsubj).
     * 
     * @param vertice: starting vertice from which the sub-tree needs to be returned
     * @param sg: semantic graph of the sentence
     * @param excludedEdge: excluded edge
     * 
     * Copied from: https://github.com/stanfordnlp/CoreNLP/blob/master/src/edu/stanford/nlp/semgraph/SemanticGraphUtils.java
     */
    public static Set<SemanticGraphEdge> getSubTreeEdges(IndexedWord vertice, SemanticGraph sg, 
            SemanticGraphEdge excludedEdge) {
        Set<SemanticGraphEdge> tabu = Generics.newHashSet();
        tabu.add(excludedEdge);
        getSubTreeEdgesHelper(vertice, sg, tabu);
        tabu.remove(excludedEdge); // Do not want this in the returned edges
        return tabu;
    }
    private static void getSubTreeEdgesHelper(IndexedWord vertice, SemanticGraph sg, Set<SemanticGraphEdge> tabuEdges) {
        for (SemanticGraphEdge edge : sg.outgoingEdgeIterable(vertice)) {
            if (!tabuEdges.contains(edge)) {
                IndexedWord dep = edge.getDependent();
                tabuEdges.add(edge);
                getSubTreeEdgesHelper(dep, sg, tabuEdges);
            }
        }
    }
    
    /**
     * Given a starting vertice, grabs the subtree encapsulated by portion of the semantic graph, excluding
     * a given edge. Returns the nodes of the subtree sorted by their indexes in the sentence.
     * 
     * @param vertice: starting vertice from which the sub-tree needs to be returned
     * @param sg: semantic graph of the sentence
     * @param excludedEdge: excluded edge
     * @return list of IndexedWord objects
     */
    public static ObjectArrayList<IndexedWord> getSubTreeSortedNodes(IndexedWord vertice, SemanticGraph sg,
            SemanticGraphEdge excludedEdge) {
        Set<SemanticGraphEdge> subTreeEdges = getSubTreeEdges(vertice, sg, null);
        return getSortedWordsFromListOfEdges(subTreeEdges); 
    }
    
    /**
     * Get the semgrex pattern for "{} < {idx:word.index()}", where we do the matching with the index of the word
     * @param word: {} is the dependent of a relation reln with 'word'
     * @return semgrex pattern string
     */
    public static String getSemgrexDependentOf(IndexedWord word){
        StringBuffer sb = new StringBuffer();
        sb.append(CHARACTER.LBRACE);
        sb.append(CHARACTER.RBRACE);
        sb.append(SEPARATOR.SPACE);
        sb.append(CHARACTER.LESS);
        sb.append(SEPARATOR.SPACE);
        sb.append(CHARACTER.LBRACE);
        sb.append(WORDS.idx);
        sb.append(CHARACTER.COLON);
        sb.append(word.index());
        sb.append(CHARACTER.RBRACE);
        return sb.toString().trim();
    }
    
    /**
     * Given a fast util object list of indexed words, return object array list of the same object list
     * @param: oWordList: list of indexed word (object list)
     * @return: an object array list object of oWordList
     */
    public static ObjectArrayList<IndexedWord> objectListToObjectArrayList(ObjectList<IndexedWord> oWordList){
        ObjectArrayList<IndexedWord> oaWordList = new ObjectArrayList<>();
        for (IndexedWord w: oWordList){
            oaWordList.add(w);
        }
        return oaWordList.clone();
    }
    
    /**
     * Given a list of edges, get all the indexed words from them (their nodes) and return them sorted by index
     * @param edges: list of edges
     * @return list of indexed words sorted by index
     */
    public static ObjectArrayList<IndexedWord> getSortedWordsFromListOfEdges(Set<SemanticGraphEdge> edges){
        ObjectOpenHashSet<IndexedWord> wordsSet = new ObjectOpenHashSet<>();
        for (SemanticGraphEdge e: edges){
            wordsSet.add(e.getGovernor());
            wordsSet.add(e.getDependent());
        }
        
        return getSortedWordsFromSetOfWords(wordsSet);
    }
    
    /**
     * Given a set of indexed words, sort them by sentence index, and return them as a list of indexed words 
     * @param wordSet: set of words to be sorted by sentence index
     * @return list of indexed words (wordSet sorted by sentence index)
     */
    public static ObjectArrayList<IndexedWord> getSortedWordsFromSetOfWords(Set<IndexedWord> wordSet){
        ObjectArrayList<IndexedWord> sortedWords = new ObjectArrayList<>();
        IntArrayList wordsIndices = new IntArrayList();
        for (IndexedWord w: wordSet){
            wordsIndices.add(w.index());
        }
        int [] sorted = FastUtil.sort(wordsIndices);
        for (int x: sorted){
            for (IndexedWord w: wordSet){
                if (w.index() == x){
                    sortedWords.add(w);
                }
            }
        }
        
        return sortedWords;
    }
    
    /**
     * Given a list of indexed words, sort them by sentence index, and return them as a list of indexed words 
     * @param wordSet: set of words to be sorted by sentence index
     * @return list of indexed words (wordSet sorted by sentence index)
     */
    public static ObjectArrayList<IndexedWord> getSortedListOfWords(ObjectArrayList<IndexedWord> wordList){
        ObjectArrayList<IndexedWord> sortedWords = new ObjectArrayList<>();
        IntArrayList wordsIndices = new IntArrayList();
        for (IndexedWord w: wordList){
            wordsIndices.add(w.index());
        }
        int [] sorted = FastUtil.sort(wordsIndices);
        for (int x: sorted){
            for (IndexedWord w: wordList){
                if (w.index() == x){
                    sortedWords.add(w);
                }
            }
        }
        
        return sortedWords;
    }
    
    /**
     * Get the number of prepositions in the list of words (TO is also counted)
     * @param wList: list of words
     * @return number of prepositions in the list
     */
    public static int countPrepositionsInList(ObjectArrayList<IndexedWord> wList){
        int prepCount = 0;
        for (IndexedWord w: wList){
            if (w.tag().equals(POS_TAG.IN) || w.tag().equals(POS_TAG.TO))
                prepCount++;
        }
        return prepCount;
    }
    
    public static ObjectArrayList<CoreLabel> getCoreLabelListFromCoreMapList(ObjectArrayList<CoreMap> coreMapList){
        ObjectArrayList<CoreLabel> coreLabelList = new ObjectArrayList<>();
        for (CoreMap cm: coreMapList){
            coreLabelList.add(new CoreLabel(cm));
        }
        return coreLabelList;
    }
    
    public static ObjectArrayList<CoreLabel> getCoreLabelListFromIndexedWordList(ObjectArrayList<IndexedWord> words) {
        ObjectArrayList<CoreLabel> coreLabelList = new ObjectArrayList<>();
        for (IndexedWord w: words) {
            coreLabelList.add(new CoreLabel(w));
        }
        return coreLabelList;
    }
    
    public static ObjectArrayList<IndexedWord> getWordListFromCoreMapList(List<CoreMap> coreMapList){
        ObjectArrayList<IndexedWord> coreLabelList = new ObjectArrayList<>();
        for (CoreMap cm: coreMapList){
            coreLabelList.add(new IndexedWord(new CoreLabel(cm)));
        }
        return coreLabelList;
    }
    public static ObjectOpenHashSet<IndexedWord> getWordSetFromCoreMapList(List<CoreMap> coreMapList){
        ObjectOpenHashSet<IndexedWord> coreLabelSet = new ObjectOpenHashSet<>();
        for (CoreMap cm: coreMapList){
            coreLabelSet.add(new IndexedWord(new CoreLabel(cm)));
        }
        return coreLabelSet;
    }
    
    public static SemanticGraph semanticGraphUniversalEnglishToEnglish(SemanticGraph semanticGraph) {
        for (SemanticGraphEdge edge: semanticGraph.edgeListSorted()) {
            GrammaticalRelation oldRel = edge.getRelation();
            edge.setRelation(EnglishGrammaticalRelations.shortNameToGRel.get(oldRel.getShortName()));
        }
        
        return semanticGraph;
    }

    public static boolean isOneNER(ObjectArrayList<IndexedWord> wordList) {
        String firstType = wordList.get(0).ner();
        if (firstType.equals(NE_TYPE.NO_NER)) {
            return false;
        }
        boolean isOneNER = true;
        for (IndexedWord w: wordList) {
            if (!w.ner().equals(firstType)) {
                isOneNER = false;
            }
        }
        return isOneNER;
    }

    /**
     * Given a list of words, check if there is a verb in the list
     * @param words: list of indexed words
     * @return true -> if there is a verb in the list of words, false -> otherwise
     */
    public static boolean hasVerb(ObjectArrayList<IndexedWord> words){
        for (IndexedWord word: words){
            if (isVerb(word.tag())) {
                return true;
            }
        }
        return false;
    }
}