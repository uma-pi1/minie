package de.uni_mannheim.utils.phrase;

import de.uni_mannheim.clausie.phrase.Phrase;
import de.uni_mannheim.constant.SEPARATOR;
import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * @author Kiril Gashteovski
 */
public class PhraseUtils {
    /**
     * Given a list of phrases, return their words concatenated into one string.
     * @param phraseList: list of phrases
     * @return string (words from the phrase list concatenated)
     */
    public static String listOfPhrasesToString(ObjectArrayList<Phrase> phraseList){
        StringBuffer sb = new StringBuffer();
        for (Phrase phrase: phraseList){
            sb.append(phrase.getWords());
            sb.append(SEPARATOR.SPACE);
        }
        return sb.toString().trim();
    }
    
    /**
     * Given a list of annoteted phrases, return their words concatenated into one string.
     * @param phraseList: list of phrases
     * @return string (words from the phrase list concatenated)
     */
    public static String listOfAnnotatedPhrasesToString(ObjectArrayList<AnnotatedPhrase> phraseList){
        StringBuffer sb = new StringBuffer();
        for (AnnotatedPhrase aPhrase: phraseList){
            sb.append(aPhrase.getWords());
            sb.append(SEPARATOR.SPACE);
        }
        return sb.toString().trim();
    }
}
