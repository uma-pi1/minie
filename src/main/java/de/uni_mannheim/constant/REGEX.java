package de.uni_mannheim.constant;

/**
 * @author Kiril Gashteovski
 */
public class REGEX {
    /** Regex for "DT* VB+ NN+" **/
    public static final String T_DT_VB_NN_END = "[{tag:/DT/}]*[{tag:/VB.*/}]+[{tag:/NN.*/}]+";
    
    /** Regex for "TO VB+ .* [NN|NER]+ .*" **/
    public static final String T_TO_VB_NP_NER = "[{tag:TO}][{tag:/VB.*/}]+[{tag:/.*/}]*[{tag:/NN.*/} | {ner:ORGANIZATION} | "
            + "{ner:PERSON} | {ner:LOCATION} | {ner:DATE} | {ner:NUMBER} | {ner:MISC} | {ner:DURATION} | {ner:MONEY} | "
            + "{ner:TIME} | {ner:ORDINAL} | {ner:SET} | {tag:/JJ.*/}][{tag:/.*/}]*";
    
    /** Regex for "NP POS NP" **/
    public static final String T_NP_POS_NP = "[{tag:DT} | {tag:/JJ.*/} | {tag:/RB.*/}]*[{tag:/NN.*/}]+[{tag:POS}]"
                                           + "[{tag:DT} | {tag:/JJ.*/} | {tag:/RB.*/}]*[{tag:/NN.*/}]+";
    
    /** Regex for "NP PERSON" **/ 
    public static final String T_NP_PERSON = "[{tag:/DT/; ner:/O/} | {tag:/JJ.*/; ner:/O/} | {tag:/RB.*/; ner:/O/} | "
                                            + "{tag:/PRP.*/; ner:/O/}]* [{tag:/NN.*/; ner:/O/}]+ [{ner:PERSON}]+";
    
    /** Regex for "ORG NP PERSON ([,|and|or] PERSON)*" **/
    public static final String T_ORG_NP_PERSON = "[{ner:ORGANIZATION}]+ [{tag:POS; ner:/O/}]? [{tag:/DT/; ner:/O/} | "
            + "{tag:/JJ.*/; ner:/O/} | {tag:/RB.*/; ner:/O/} | {tag:/PRP.*/; ner:/O/}]* "
            + "[{tag:/NN.*/; ner:/O/}]+ [{ner:PERSON}]+ "
            + "([{lemma:/,/; ner:/O/} | {lemma:and; ner:/O/} | {lemma:or; ner:/O/}] [{ner:PERSON}]+)*";
    
    /** Regex for "ORG IN LOC" **/
    public static final String T_ORG_IN_LOC = "[{ner:ORGANIZATION}]+[{tag:IN; ner:O}]+[{ner:LOCATION}]+";

    /** Regex for "ORG POS? PERSON" **/
    public static final String T_ORG_PERSON = "[{ner:ORGANIZATION}]+ [{tag:POS; ner:/O/}]? [{ner:PERSON}]+";
    
    /** Regex for "PERSON among other? NP" **/
    public static final String T_PERSON_AMONG_NP = "[{ner:PERSON}]+ [{lemma:among}] [{lemma:other}]? "
            + "[{tag:/DT/; ner:/O/} | {tag:/JJ.*/; ner:/O/} | {tag:/RB.*/; ner:/O/} | {tag:/PRP.*/; ner:/O/}]* "
            + "[{tag:/NN.*/; ner:/O/}]+";
    
    /** Regex for the Hearst Pattern: NP_1 such as NP_2, NP_3, ... [and|or] NP_n **/
    public static final String T_HEARST_1 = "[{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* [{tag:/NN.*/}]+"
               + "[{lemma:such}][{lemma:as}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* [{tag:/NN.*/}]+"
               + "([{lemma:/,/} | {lemma:and} | {lemma:or}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* "
               + "[{tag:/NN.*/}]+)*";
    
    /** Regex for the Hearst Pattern: NP_1 like NP_2, NP_3, ... [and|or] NP_n**/
    public static final String T_HEARST_2 = "[{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* [{tag:/NN.*/}]+"
            + "[{lemma:like; tag:IN}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* [{tag:/NN.*/}]+"
            + "([{lemma:/,/} | {lemma:and} | {lemma:or}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* "
            + "[{tag:/NN.*/}]+)*";
    
    /** Regex for the Hearst Pattern: such NP_1 as NP_1, NP_2, ... [and|or] NP_n **/
    public static final String T_HEARST_2_2 = "[{lemma:such; ner:O}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* "
            + "[{tag:/NN.*/}]+"
            + "[{lemma:as}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* [{tag:/NN.*/}]+"
            + "([{lemma:/,/} | {lemma:and} | {lemma:or}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* "
            + "[{tag:/NN.*/}]+)*";
    
    /** Regex for the Hearst Pattern: NP_1, NP_2, ... [,|and|or] other NP_n**/
    public static final String T_HEARST_3 = "[{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* [{tag:/NN.*/}]+"
            + "([{lemma:/,/} | {lemma:and} | {lemma:or}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* "
            + "[{tag:/NN.*/}]+)*"
            + "[{lemma:/,/}]?  [{lemma:and} | {lemma:or}] "
            + "[{lemma:other}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* [{tag:/NN.*/}]+";
    
    /** Regex for the Hearst Pattern: NP , including (NP ,)* [or|and] NP **/
    public static final String T_HEARST_4 = "[{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* [{tag:/NN.*/}]+"
            + "[{lemma:/,/; ner:/O/}] [{word:including; ner:/O/} | {word:especially; ner:/O/}] [{tag:/DT/} | {tag:/JJ.*/} | "
            + "{tag:/RB.*/} | {tag:/PRP.*/}]* [{tag:/NN.*/}]+"
            + "([{lemma:and; ner:/O/} | {lemma:or; ner:/O/} | {lemma:/,/; ner:/O/}] [{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | "
            + "{tag:/PRP.*/}]* [{tag:/NN.*/}]+)*";
    
    /** Regex for "city of LOC" **/
    public static final String T_CITY_OF_LOC = "[{tag:/DT/} | {tag:/JJ.*/} | {tag:/RB.*/} | {tag:/PRP.*/}]* [{lemma:/city|town/}] "
            + "[{lemma:of; tag:IN}] [{ner:LOCATION}]+";
    
    /** Regex for "be? [go|plan|intend|mean|try|think|schedule|expect|want] to VB+" **/
    public static final String T_POSS_VP = "[{lemma:be}]? [{lemma:/go|plan|intend|mean|try|think|schedule|expect|want/}] /to/ "
            + "[{tag:/VB.*/}]+";
    
    /** Regex that checks if all the words are the same named-entity types **/
    public static final String MULTI_WORD_ENTITY = "((^(PERSON\\s)+PERSON$)|"
                                                  + "(^(LOCATION\\s)+LOCATION$)|"
                                                  + "(^(ORGANIZATION\\s)+ORGANIZATION$)|"
                                                  + "(^(DATE\\s)+DATE$)|"
                                                  + "(^(NUMBER\\s)+NUMBER$)|"
                                                  + "(^(MISCC\\s)+MISC$)|"
                                                  + "(^(DURATION\\s)+DURATION$)|"
                                                  + "(^(MONEY\\s)+MONEY$)|"
                                                  + "(^(TIME\\s)+TIME$)|"
                                                  + "(^(ORDINAL\\s)+ORDINAL$)|"
                                                  + "(^(SET\\s)+SET$)|"
                                                  + "(^(PERCENT\\s)+PERCENT$))";
    
    /** Regex that checks if all the words are nouns **/
    public static final String MULTI_WORD_NOUN = "^(NN\\s)+NN$";
    
    /** Regex for RB+ VB+ **/
    public static final String T_RB_VB = "[{tag:/RB/}]+[{tag:/VB.*/}]+";
    
    /** Regex for  "DT+ [RB|JJ|VB]* NN+" **/
    public static final String T_DT_OPT_RB_JJ_VB_OPT_NN = "[{tag:/DT/}]+ [{tag:/RB.*/} | {tag:/JJ.*/} | {tag:/VB.*/}]*"
                                                        + "[{tag:/NN.*/}]+";
    
    /** Regex for "VB RB VB" **/
    public static final String T_VB_RB_VB = "[{tag:/VB.*/}][{tag:/RB.*/}][{tag:/VB.*/}]";
    
    /** Regex for "[IN|TO] .* IN TO" **/
    public static final String T_PREP_ALL_PREP = "[{tag:IN; ner:O} | {tag:TO; ner:O}] [{tag:/.*/}]+ "
                                                + "[{tag:IN; ner:O} | {tag:TO; ner:O}]";
    
    /** Regex for "VB+ TO VB+" **/
    public static final String T_VB_TO_VB = "[{tag:/VB.*/}]+ [{tag:TO}] [{tag:/VB.*/}]+";
    
    /** Regex for "VB+ RB+" **/
    public static final String T_VB_RB = "[{tag:/VB.*/}]+[{tag:/RB.*/}]+";
    
    /** Regex for "[DT|RB|JJ|PR]+ NN+" */
    public static final String T_DT_RB_JJ_PR_NN = "[{tag:/DT/} | {tag:/RB.*/} | {tag:/JJ.*/} | {tag:/PR.*/}]+[{tag:/NN.*/}]+";
    
    /** Regex for "RB+ [IN|TO]" **/
    public static final String T_RB_OPT_IN_TO_OPT = "[{tag:/RB*/}]+[{tag:/IN/} | {tag:/TO/}]";
    
    /** Regex for "[DT|RB|JJ|VB]* PR+ NP+" **/
    public static final String T_PR_NP = "[{tag:/DT/} | {tag:/RB.*/} | {tag:/JJ.*/} | {tag:/VB.*/}]* [{tag:/PR.*/}]+ "
            + "[{tag:/DT/} | {tag:/RB.*/} | {tag:/JJ.*/} | {tag:/VB.*/}]* [{tag:/NN.*/}]+";

    /** Regex for "DT+ [RB|JJ]* ENTITY" */    
    public static final String T_DT_OPT_RB_JJ_OPT_ENTITY = "[{tag:DT}]+ [{tag:/RB.*/} | {tag:/JJ.*/}]* "
            + "([{ner:ORGANIZATION}]+ | [{ner:PERSON}]+ | [{ner:LOCATION}]+ | [{ner:DATE}]+ | [{ner:NUMBER}]+ | "
            + " [{ner:MISC}]+ | [{ner:DURATION}]+ | [{ner:MONEY}]+ | [{ner:TIME}]+ | [{ner:ORDINAL}]+ | "
            + " [{ner:SET}]+)";
    
    /** Regex for "NP [IN|TO] [DT|RB|JJ]* NER" **/
    public static final String T_NP_IN_OPT_DT_RB_JJ_OPT_ENTITY = "[{tag:/RB.*/} | {tag:/JJ.*/} | {tag:DT} | {tag:/PRP.*/}]* "
            + "[{tag:/NN.*/}]+"
            + "[{tag:IN} | {tag:TO}] [{tag:DT} | {tag:/RB.*/} | {tag:/JJ.*/}]* "
            + "([{ner:ORGANIZATION}]+ | [{ner:PERSON}]+ | [{ner:LOCATION}]+ | [{ner:DATE}]+ | [{ner:NUMBER}]+ | "
            + " [{ner:MISC}]+ | [{ner:DURATION}]+ | [{ner:MONEY}]+ | [{ner:TIME}]+ | [{ner:ORDINAL}]+ | "
            + " [{ner:SET}]+)";
    
    /** Regex for "TO [VB|RP]* RB? IN" **/
    public static final String T_TO_VP_IN = "[{tag:TO}] [{tag:/VB.*/} | {tag:RP}]* [{tag:/RB.*/}]?[{tag:IN}]*";

    /** Regex for "[RB|JJ]+ NER" **/
    public static final String T_RB_JJ_NER = "[{tag:/RB.*/} | {tag:/JJ.*/}]+ "
            + "([{ner:ORGANIZATION}]+ | [{ner:PERSON}]+ | [{ner:LOCATION}]+ | [{ner:DATE}]+ | [{ner:NUMBER}]+ | "
            + " [{ner:MISC}]+ | [{ner:DURATION}]+ | [{ner:MONEY}]+ | [{ner:TIME}]+ | [{ner:ORDINAL}]+ | "
            + " [{ner:SET}]+)";
    
    /** Token regex pattern: detect already annotated quantities which are next to each other **/
    public static final String ADJACENT_QUANTITIES = "[{tag:/QUANTITY/}][{lemma:of}]?[{tag:/PRP.*/}]?[{tag:/QUANTITY/}]";
    
    /** Token regex pattern: for detecting quantities **/
    public static final String QUANTITY_SEQUENCE = "[ {lemma:/some|all|any|each|every|half|many|various|several|numerous|few|much/} "
                                                    + "| {word::IS_NUM} | {tag:CD} | {ner:PERCENT}]+";
    
    /** Token regex pattern same as QUANTITY_SEQUENCE, only it also includes 'no' as a quantity (e.g. 'no people') **/
    public static final String QUANTITY_SEQUENCE_WITH_NO = "[ {lemma:/some|all|any|no|each|every|half|many|various|several|"
                                                            + "numerous|few|much/} | {word::IS_NUM} | {tag:CD} | {ner:PERCENT}]+";
    
    /** Regex for spotting 'safe determiners' i.e. 'a', 'an' or 'the' **/
    public static final String SAFE_DETERMINER = "[{lemma:/a/} | {lemma:/an/} | {lemma:/the/}]";
}
