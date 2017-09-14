package de.uni_mannheim.minie.minimize;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.uni_mannheim.constant.NE_TYPE;
import de.uni_mannheim.constant.POS_TAG;
import de.uni_mannheim.constant.REGEX;
import de.uni_mannheim.constant.WORDS;
import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.annotation.Polarity;
import de.uni_mannheim.minie.subconstituent.SubConstituent;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * A class containing elements for minimization.
 * @param phrase: the phrase being considered for minimization
 * @param sg: the semantic graph of the sentence from which the phrase is derived
 * @param droppedWords: set of words being dropped
 * @param tPattern: reusable variable (token regex pattern)
 * @param tMatcher: reusable variable (token regex matcher)
 * @param mwe: dictionary of multi-word expressions
 *
 * @author Kiril Gashteovski
 */
public class Minimization {

    private AnnotatedPhrase phrase;
    private SemanticGraph sg;
    private ObjectOpenHashSet<String> mwe;
    private TokenSequencePattern tPattern;
    private TokenSequenceMatcher tMatcher;
    
    
    /** Default constructor **/
    public Minimization(){
        this.sg = null;
        this.phrase = null;
        this.mwe = new ObjectOpenHashSet<String>();
        this.tPattern = null;
        this.tMatcher = null;
    }
    
    /** When phrase and semantic graph are given, initialize those, but the rest are empty fields **/
    public Minimization(AnnotatedPhrase phrase, SemanticGraph sg, ObjectOpenHashSet<String> mwe) {
        this.tPattern = null;
        this.tMatcher = null;
        this.phrase = phrase;
        this.sg = sg;
        this.mwe = mwe;
    }
    
    /** Given a phrase, if it contains a noun phrase, make a noun phrase safe minimization */
    public void nounPhraseSafeMinimization(List<CoreMap> remWords, List<CoreMap> matchCoreMaps){
        // Flags for checking certain conditions
        boolean isDT;
        boolean isNotNER;
        boolean containsNEG;
        
        // If (DT+ [RB|JJ|VB]* NN+) => drop DT+
        this.tPattern = TokenSequencePattern.compile(REGEX.T_DT_OPT_RB_JJ_VB_OPT_NN);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){         
            matchCoreMaps = tMatcher.groupNodes();
                        
            for (CoreMap cm: matchCoreMaps){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.lemma() == null) cl.setLemma(cl.word());
                
                isDT = cl.tag().equals(POS_TAG.DT);
                isNotNER = cl.ner().equals(NE_TYPE.NO_NER);
                containsNEG = Polarity.NEG_WORDS.contains(cl.lemma().toLowerCase());
                if (isDT && isNotNER && !containsNEG){
                    remWords.add(cm);   
                }
            }
            this.dropWords(remWords, matchCoreMaps);
        }
        
        // Clean the other safe determiners
        this.tPattern = TokenSequencePattern.compile(REGEX.SAFE_DETERMINER);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){   
            matchCoreMaps = tMatcher.groupNodes();
            CoreLabel cl = new CoreLabel(matchCoreMaps.get(0));
            if (cl.lemma() == null) cl.setLemma(cl.word());
            isDT = cl.tag().equals(POS_TAG.DT);
            isNotNER = cl.ner().equals(NE_TYPE.NO_NER);
            containsNEG = Polarity.NEG_WORDS.contains(cl.lemma().toLowerCase());
            
            if (isDT && isNotNER && !containsNEG){
                remWords.add(matchCoreMaps.get(0));
            }
            
            // Drop the words not found in dict. 
            this.dropWords(remWords, matchCoreMaps);
            remWords.clear();
        }
        
        // If ([DT|RB|JJ|VB]* PRP$ [DT|RB|JJ|VB]* NN+) => drop PRP$
        this.tPattern = TokenSequencePattern.compile(REGEX.T_PR_NP);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){         
            matchCoreMaps = tMatcher.groupNodes();
                        
            for (CoreMap cm: matchCoreMaps){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.lemma() == null) cl.setLemma(cl.word());
                if (cl.tag().equals(POS_TAG.PRP_P) && cl.ner().equals(NE_TYPE.NO_NER)){
                    remWords.add(cm);   
                }
            }
            this.dropWords(remWords, matchCoreMaps);
        }
        
        /*
        // If (DT* NUMBER+ [RB|JJ]* NN+), drop DT* NUMBER+
        this.droppedWords = MinimizationUtils.dropFirstTwoMatchedTags(this.pattern, this.matcher, 
                                                                        REGEX.OPT_DT_OPT_NUMBER_OPT_RB_JJ_OPT_NN, 
                                                                        this.seqPosNer, phrase, this.mwe);
        */
    }
    
    /**
     * Given a list of matched core maps (a phrase) and a list of words which are candidates for dropping ('remWords'), 
     * check if some of them form sub-constituents of 'matchCoreMaps' which are found in the dictionary.
     * If there are, remove them from 'remWords'. The words left in 'remWords' are the ones that couldn't be matched
     * with a sub-constituent found in the dictionary, i.e. those are the ones that we drop.
     * @param matchCoreMaps: list of words as a list of CoreMap object (a phrase)
     * @param remWords: list of candidates to be dropped (each word in 'remWord' can also be found in 'matchCoreMaps')
     */
    public void dropWordsNotFoundInDict(List<CoreMap> matchCoreMaps, List<CoreMap> remWords){
        // Get all the sub-constituents
        ObjectArrayList<IndexedWord> words = CoreNLPUtils.listOfCoreMapWordsToIndexedWordList(matchCoreMaps);
        SubConstituent sc = new SubConstituent(this.sg, CoreNLPUtils.getRootFromWordList(this.sg, words), words);
        sc.generateSubConstituentsFromLeft();
        ObjectOpenHashSet<String> subconstituents = sc.getStringSubConstituents();
        
        // Sub-constituents' strings found in the dictionary
        ObjectArrayList<String> scStringsInDict = new ObjectArrayList<>();
        for (String s: subconstituents){
            if (this.mwe.contains(s)){
                scStringsInDict.add(s);
            }
        }
        
        // If sub-constituents's strings are found in the dictionary, detect the words associated with them
        // and remove them.
        if (scStringsInDict.size() > 0){
            Iterator<CoreMap> iter = remWords.iterator();
            for (String stInDict: scStringsInDict){
                while (iter.hasNext()){   
                    CoreMap cm = iter.next();
                    CoreLabel cl = new CoreLabel(cm);
                    if (stInDict.contains(cl.lemma().toLowerCase())){
                        iter.remove();
                    }
                }
            }
        }
        
        // Drop the words not found in frequent/collocation sub-constituents
        this.dropWords(remWords, matchCoreMaps);
    }
    
    /**
     * Given a list of words to be removed and a list of matched nodes, remove the words to be removed from the phrase and
     * empty that list, also empty the list of matched nodes
     * @param remWords
     * @param matchedNodes
     */
    public void dropWords(List<CoreMap> remWords, List<CoreMap> matchWords){
        matchWords.clear();
        // in addition to removing the words, save them in a separate list
        ObjectArrayList<SemanticGraphEdge> droppedEdges = CoreNLPUtils.listOfCoreMapWordsToParentEdges(this.sg, remWords);
        /*ObjectArrayList<SemanticGraphEdge> droppedEdges = new ObjectArrayList<SemanticGraphEdge>();
        for (IndexedWord word: remWordsArray) {
            SemanticGraphEdge edge = this.sg.getEdge(this.sg.getParent(word), word);
            droppedEdges.add(edge);
        }*/
        this.phrase.addDroppedEdges(droppedEdges);
        this.phrase.addDroppedWords(CoreNLPUtils.getWordListFromCoreMapList(remWords));
        // remove words
        this.phrase.removeCoreLabelWordsFromList(remWords);
        remWords.clear();
    }
    
    /** Given a phrase, if there is (DT* VB+ NN+), remove (DT* VB+) */
    public void removeVerbsBeforeNouns(List<CoreMap> remWords, List<CoreMap> matchWords){
        // Flags for checking certain conditions
        boolean isDT;
        boolean isVerb;
        boolean isNotNER;
        boolean containsNEG;
        boolean hasDT = false;
        
        this.tPattern = TokenSequencePattern.compile(REGEX.T_DT_VB_NN_END);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){
            matchWords = tMatcher.groupNodes();
            
            for (CoreMap cm: matchWords){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.lemma() == null) cl.setLemma(cl.word());
                // Check if the word is a determiner, no ner and the first word in the phrase
                isDT = cl.tag().equals(POS_TAG.DT);
                isNotNER = cl.ner().equals(NE_TYPE.NO_NER);
                containsNEG = Polarity.NEG_WORDS.contains(cl.lemma().toLowerCase());
                isVerb = CoreNLPUtils.isVerb(cl.tag());
                
                if (isDT && isNotNER && !containsNEG){
                    if (cl.index() == this.phrase.getWordCoreLabelList().get(0).index()){
                        remWords.add(cm);
                        hasDT = true;
                    } else break;
                }
                // Check if the word is a verb, no ner
                else if (isVerb && isNotNER && !containsNEG){
                    // If it's not preceded by DT, check if it's the first word in the phrase
                    if (!hasDT) {
                        if (cl.index() == this.phrase.getWordCoreLabelList().get(0).index()){
                            if (!this.sg.hasChildren(new IndexedWord(cl))){
                                remWords.add(cm);
                            }
                        } else break;
                    } else {
                        if (!this.sg.hasChildren(new IndexedWord(cl))){
                            remWords.add(cm);
                        }
                    }
                }
            }
            
            // If the multi-word expression is found in the dictionary - don't drop it
            if (this.isCoreMapListInDictionary(matchWords)){
                matchWords.clear();
                remWords.clear();
                continue;
            }
            
            this.dropWords(remWords, matchWords);
        }
    }
    
    
    /**
     * Given a phrase, if it contains a noun phrase, make a noun phrase dictionary minimization.
     * @param remWords: list of words to be removed (reusable variable)
     * @param matchWords: list of matched words from the regex (reusable variable)
     */
    public void nounPhraseDictMinimization(List<CoreMap> remWords, List<CoreMap> matchCoreMaps){   
        // Do the safe minimization
        this.nounPhraseSafeMinimization(remWords, matchCoreMaps);
        
        // If (ORGANIZATION+|MISC+|ORDINAL+) NN+ => drop (ORGANIZATION+|MISC+|ORDINAL+)
        /*
        this.tPattern = TokenSequencePattern.compile(REGEX.T_NER2_NN);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){         
            matchCoreMaps = tMatcher.groupNodes();
            
            for (CoreMap cm: matchCoreMaps){
                CoreLabel cl = new CoreLabel(cm);
                // Check if the word is ORG, ORDINAL or MISC
                if (cl.ner().equals(NER_CONSTANT.ORGANIZATION) || cl.ner().equals(NER_CONSTANT.MISC) || 
                        cl.ner().equals(NER_CONSTANT.ORDINAL)){    
                    remWords.add(cm);   
                }
            }
            
            // If the multi-word expression is found in the dictionary - don't drop it
            if (this.isCoreMapListInDictionary(matchCoreMaps)){
                matchCoreMaps.clear();
                remWords.clear();
                continue;
            }
            
            this.dropWords(remWords, matchCoreMaps);
        }*/
        
        // Flags for checking certain conditions
        boolean isDT;
        boolean isAdverb;
        boolean isNotNER;
        boolean containsNEG;
        boolean isAdj;
        boolean isPRP;
        
        // If ([DT|RB|JJ|PR]* NN+) => drop [DT|RB|JJ|PR]+
        this.tPattern = TokenSequencePattern.compile(REGEX.T_DT_RB_JJ_PR_NN);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){         
            matchCoreMaps = tMatcher.groupNodes();

            for (CoreMap cm: matchCoreMaps){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.lemma() == null) cl.setLemma(cl.word());
                isAdj = CoreNLPUtils.isAdj(cl.tag());
                isNotNER = cl.ner().equals(NE_TYPE.NO_NER);
                isAdverb = CoreNLPUtils.isAdverb(cl.tag());
                containsNEG = Polarity.NEG_WORDS.contains(cl.lemma().toLowerCase());
                isDT = cl.tag().equals(POS_TAG.DT);
                isPRP = cl.tag().equals(POS_TAG.PRP_P);
                
                // Check if the word is an adjective which is not a NER
                if (isAdj && isNotNER){
                    // Check if it is non-subsective (keep these)
                    if (!this.isNonSubsectiveAdj(cl))
                        remWords.add(cm);   
                }
                // Check if the word is an adverb/determiner/pronoun which is not a NER
                else if (isAdverb && isNotNER && !containsNEG){
                    remWords.add(cm);
                }
                else if (isDT && isNotNER && !containsNEG){
                    remWords.add(cm);
                }
                else if (isPRP && isNotNER){
                    remWords.add(cm);
                }
            }
            
            // Drop the words not found in dict. 
            this.dropWordsNotFoundInDict(matchCoreMaps, remWords);
            remWords.clear();
        }
        
        /*
        // If there is a pattern (PR+ NN+) => drop PR+
        this.tPattern = TokenSequencePattern.compile(REGEX.T_PR_NN);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){   
            matchCoreMaps = tMatcher.groupNodes();
            
            for (CoreMap cm: matchCoreMaps){
                CoreLabel cl = new CoreLabel(cm);
                // Check if the word is PR which is not a NER
                if (CoreNLPUtils.isPronoun(cl.tag()) && cl.ner().equals(NER_CONSTANT.NO_NER)){
                    remWords.add(cm);   
                }
            }
            this.dropWords(remWords, matchCoreMaps);
        }*/
    }
    
    /** Given a phrase, if it contains a verb phrase, make a verb phrase safe minimization **/
    public void verbPhraseSafeMinimization(List<CoreMap> remWords, List<CoreMap> matchWords){
        // Flags for checking certain conditions
        boolean isAdverb;
        boolean isNotNER;
        boolean containsNEG;
        
        // If the relation starts with a RB+ VB+, drop RB+
        this.tPattern = TokenSequencePattern.compile(REGEX.T_RB_VB);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){   
            matchWords = tMatcher.groupNodes();
            
            for (CoreMap cm: matchWords){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.lemma() == null) cl.setLemma(cl.word());
                
                isAdverb = CoreNLPUtils.isAdverb(cl.tag());
                isNotNER = cl.ner().equals(NE_TYPE.NO_NER);
                containsNEG = Polarity.NEG_WORDS.contains(cl.lemma().toLowerCase());
                
                // Check if the word is RB which is not a NER
                if (isAdverb && isNotNER && !containsNEG){
                    remWords.add(cm);   
                }
            }
            this.dropWords(remWords, matchWords);
        }
    }
    
    /** Given a phrase, if it contains NERs, make a safe minimization around them */
    public void namedEntitySafeMinimization(List<CoreMap> remWords, List<CoreMap> matchWords){
        // Flags for checking certain conditions
        boolean isNotNER;
        boolean containsNEG;
        boolean isDT;
        
        ObjectArrayList<IndexedWord> dropWords = new ObjectArrayList<>();
        Set<GrammaticalRelation> excludeRels = new HashSet<>();
        excludeRels.add(EnglishGrammaticalRelations.ADJECTIVAL_MODIFIER);
        excludeRels.add(EnglishGrammaticalRelations.ADVERBIAL_MODIFIER);
        // If PERSON is modified by an adjective, drop the adjective and its subtree
        for (IndexedWord w: this.phrase.getWordList()) {
            if (w.ner().equals(NE_TYPE.PERSON)) {
                Set<IndexedWord> modifiers = sg.getChildrenWithRelns(w, excludeRels);
                for (IndexedWord wm: modifiers) {
                    if (wm.ner().equals(NE_TYPE.NO_NER)) {
                        dropWords.add(wm);
                        dropWords.addAll(CoreNLPUtils.getSubTreeSortedNodes(wm, sg, null));
                    }
                }
            }
        }
        this.phrase.removeWordsFromList(dropWords);
        
              
        // If (.* DT+ [RB|JJ]* NER+ .*) => drop (DT+)
        this.tPattern = TokenSequencePattern.compile(REGEX.T_DT_OPT_RB_JJ_OPT_ENTITY);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){         
            matchWords = tMatcher.groupNodes();
            
            for (CoreMap cm: matchWords){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.lemma() == null) cl.setLemma(cl.word());
                
                isDT = cl.tag().equals(POS_TAG.DT);
                isNotNER = cl.ner().equals(NE_TYPE.NO_NER);
                containsNEG = Polarity.NEG_WORDS.contains(cl.lemma().toLowerCase()); 
                        
                // Check if the word is DT, drop it
                if (isDT && isNotNER && !containsNEG){
                    remWords.add(cm);   
                }
            }
            
            this.dropWords(remWords, matchWords);
        }
        remWords.clear();
        
        // If NP PERSON+ => drop NP
        this.tPattern = TokenSequencePattern.compile(REGEX.T_NP_PERSON);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){
            matchWords = tMatcher.groupNodes();
            for (CoreMap cm: matchWords){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.lemma() == null) cl.setLemma(cl.word());
                
                if (!cl.ner().equals(NE_TYPE.PERSON))
                    remWords.add(cm);
            }
            this.dropWords(remWords, matchWords);
        }
        
        // If ORG+ POS? NP PERSON+ => "PERSON" "is NP of" "ORG" drop (ORG+ POS? NP)
        this.tPattern = TokenSequencePattern.compile(REGEX.T_ORG_PERSON);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){
            matchWords = tMatcher.groupNodes();
            for (CoreMap cm: matchWords){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.lemma() == null) cl.setLemma(cl.word());
                
                if (!cl.ner().equals(NE_TYPE.PERSON))
                    remWords.add(cm);
            }
            this.dropWords(remWords, matchWords);
        }
        
        // Hearst pattern 1: if "NP_1 such as NP_2", drop "NP_1 such as"
        this.tPattern = TokenSequencePattern.compile(REGEX.T_HEARST_1);
        this.tMatcher = this.tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){  
            matchWords = tMatcher.groupNodes();
            
            // Determine the droping index (drop everything until 'dropUntilInd')
            int dropUntilInd = -1;
            for (int i = 0; i < matchWords.size(); i++){
                CoreLabel cl = new CoreLabel(matchWords.get(i));
                if (cl.lemma() == null) cl.setLemma(cl.word());
                
                if (cl.lemma().equals("such")){
                    CoreLabel cl2 = new CoreLabel(matchWords.get(i+1));
                    if (cl2.lemma().equals("as")){
                        dropUntilInd = i+1;
                        break;
                    }
                }
            }
            
            // Add the droping words to the list and drop them
            for (int i = 0; i <= dropUntilInd; i++) {
                remWords.add(matchWords.get(i));
            }
            this.dropWords(remWords, matchWords);
        }
        
        // Hearst pattern 2: if "NP_1 like NP_2" => drop "NP_1 like" 
        this.tPattern = TokenSequencePattern.compile(REGEX.T_HEARST_2);
        this.tMatcher = this.tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){  
            matchWords = tMatcher.groupNodes();
            for (int i = 0; i < matchWords.size(); i++){
                CoreLabel cl = new CoreLabel(matchWords.get(i));
                if (cl.lemma() == null) cl.setLemma(cl.word());
                
                if (cl.lemma().equals("like") && cl.ner().equals(NE_TYPE.NO_NER)){
                    remWords.add(matchWords.get(i));
                    break;
                }
                remWords.add(matchWords.get(i));
            }
            this.dropWords(remWords, matchWords);
        }
        
        // If ORG IN LOC => drop IN LOC
        this.tPattern = TokenSequencePattern.compile(REGEX.T_ORG_IN_LOC);
        this.tMatcher = this.tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){  
            matchWords = tMatcher.groupNodes();
            for (int i = 0; i < matchWords.size(); i++){
                CoreLabel cl = new CoreLabel(matchWords.get(i));
                if (cl.lemma() == null) cl.setLemma(cl.word());
                
                if (cl.tag().equals(POS_TAG.IN) && cl.ner().equals(NE_TYPE.NO_NER)){
                    remWords.add(matchWords.get(i));
                }
                else if (cl.ner().equals(NE_TYPE.LOCATION))
                    remWords.add(matchWords.get(i));
            }
            this.dropWords(remWords, matchWords);
        }
        
        // If  (.* NN+ PERSON+ .*) => drop (NN+)
        // TODO: if the noun is modified by something else (an adjective), drop it as well
        // TODO: double check these cases, not working "safe" yet!
        /*this.pattern = Pattern.compile(REGEX.NN_PERSON);
        this.dropFirstMatchedTagNoNER();
        
        // If (.* RB+ NER+ .*) => drop (RB+)
        this.pattern = Pattern.compile(REGEX.RB_ENTITY);
        this.matcher = pattern.matcher(this.seqPosNer);
        this.dropFirstMatchedTagNoNER();
        */
    }
    
    /** Given a phrase, if it contains NERs, make a dictionary minimization around them **/
    public void namedEntityDictionaryMinimization(List<CoreMap> remWords, List<CoreMap> matchWords){
        // If (.* DT+ [RB|JJ]* NER+ .*) => drop (DT+)
        this.tPattern = TokenSequencePattern.compile(REGEX.T_RB_JJ_NER);
        this.tMatcher = tPattern.getMatcher(this.phrase.getWordCoreLabelList());
        while (this.tMatcher.find()){         
            matchWords = tMatcher.groupNodes();
            
            for (CoreMap cm: matchWords){
                CoreLabel cl = new CoreLabel(cm);
                if (cl.lemma() == null) cl.setLemma(cl.word());
                
                // Check if the word is DT, drop it
                if ((CoreNLPUtils.isAdj(cl.tag()) || CoreNLPUtils.isAdverb(cl.tag())) 
                        && cl.ner().equals(NE_TYPE.NO_NER)){
                    remWords.add(cm);   
                }
            }
            
            // Drop the words not found in dict. 
            this.dropWordsNotFoundInDict(matchWords, remWords);
        }
        
        // Do the safe minimization
        this.namedEntitySafeMinimization(remWords, matchWords);
    }
    
    /**
     * Given a list of words, check if they are contained in the dictionary
     * @param words
     * @return
     */
    public boolean isInDictionary(ObjectArrayList<IndexedWord> words){
        if (this.mwe.contains(CoreNLPUtils.listOfWordsToLemmaString(words)))
            return true;
        if (this.mwe.contains(CoreNLPUtils.listOfWordsToWordsString(words)))
            return true;
        return false;
    }
    
    /**
     * Given a list of words as core maps, check if they are contained in the dictionary
     * @param words
     * @return
     */
    public boolean isCoreMapListInDictionary(List<CoreMap> cmWords){
        if (this.mwe.contains(CoreNLPUtils.listOfCoreMapWordsToLemmaString(cmWords)))
            return true;
        if (this.mwe.contains(CoreNLPUtils.listOfCoreMapWordsToWordString(cmWords)))
            return true;
        return false;
    }
    
    /**
     * Given an adjective (a CoreLabel object) check if it is non-subsective 
     * @param adj: a word (an adjective)
     * @return true, if the adjective is non-subsective, false otherwise
     */
    private boolean isNonSubsectiveAdj(CoreLabel adj){
        if (WORDS.NON_SUBSECTIVE_JJ_CF.contains(adj.lemma()))
            return true; 
        else if (WORDS.NON_SUBSECTIVE_JJ_CF.contains(adj.word()))
            return true; 
        else if (WORDS.NON_SUBSECTIVE_JJ_MODAL.contains(adj.lemma()))
            return true; 
        else if (WORDS.NON_SUBSECTIVE_JJ_MODAL.contains(adj.word()))
            return true; 
        else if (WORDS.NON_SUBSECTIVE_JJ_TEMP.contains(adj.word()))
            return true; 
        else if (WORDS.NON_SUBSECTIVE_JJ_TEMP.contains(adj.lemma()))
            return true; 
        return false;
    }
    
    /**
     * getters
     */
    public AnnotatedPhrase getPhrase(){
        return this.phrase;
    }
    public SemanticGraph getSemanticGraph(){
        return this.sg;
    }
    public ObjectOpenHashSet<String> getMwe(){
        return this.mwe;
    }
}
