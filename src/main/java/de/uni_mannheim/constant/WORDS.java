package de.uni_mannheim.constant;

import java.io.IOException;

import de.uni_mannheim.utils.Dictionary;

/**
 * @author Kiril Gashteovski
 */
public class WORDS {
    // A set of non-subsective modal adjectives
    public static Dictionary NON_SUBSECTIVE_JJ_MODAL;
    static {
        try {
            NON_SUBSECTIVE_JJ_MODAL = new Dictionary("/minie-resources/non-subsective-adjectives-modal.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    // A set of non-subsective cf. adjectives
    public static Dictionary NON_SUBSECTIVE_JJ_CF;
    static {
        try {
            NON_SUBSECTIVE_JJ_CF = new Dictionary("/minie-resources/non-subsective-adjectives-cf.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    // A set of non-subsective temp. adjectives 
    public static Dictionary NON_SUBSECTIVE_JJ_TEMP;
    static {
        try {
            NON_SUBSECTIVE_JJ_TEMP = new Dictionary("/minie-resources/non-subsective-adjectives-temp.dict");
        } catch (IOException e) {
            throw new Error(e);
        } 
    }
    
    public static String word = "word";
    public static String idx = "idx";
    public static String factuality = "Factuality";
    public static String attribution = "Attribution";
}
