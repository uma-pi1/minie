package de.uni_mannheim.utils.minie;

import edu.stanford.nlp.ling.IndexedWord;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import joptsimple.OptionSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringJoiner;

import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.minie.annotation.AnnotatedProposition;
import de.uni_mannheim.minie.annotation.Attribution;
import de.uni_mannheim.minie.annotation.Quantity;
import de.uni_mannheim.utils.Dictionary;

/**
 * Helper class for MinIE
 *
 * @author Martin Achenbach
 * @author Kiril Gashteovski
 */
public class Utils {
    /** MinIE default dictionaries **/
    public static String [] DEFAULT_DICTIONARIES = new String [] {"/minie-resources/wn-mwe.txt", 
                                                                  "/minie-resources/wiktionary-mw-titles.txt"};

    /**
     * formats an annotated proposition in Ollie style
     * @param proposition: annotated proposition to format
     * @return formatted proposition
     */
    public static String formatProposition(AnnotatedProposition proposition) {
        // First the triple
        StringJoiner tripleJoiner = new StringJoiner(";", "(", ")");
        String subject = proposition.getSubject().toString();
        if (!subject.isEmpty()) tripleJoiner.add(subject);
        String relation = proposition.getRelation().toString();
        if (!relation.isEmpty()) tripleJoiner.add(relation);
        String object = proposition.getObject().toString();
        if (!object.isEmpty()) tripleJoiner.add(object);
        
        // Factuality
        String factualityString = "";
        String factuality = formatFactuality(proposition.getPolarity().getType().toString(), proposition.getModality().getModalityType().toString());
        if (!factuality.isEmpty()) factualityString = String.format("[factuality=%s]", factuality);
        
        /*String clausalModifier = proposition.getClauseModifier().toString();
        if (!clausalModifier.isEmpty()) annotations.add("clausalModifier=" + clausalModifier);*/

        // Attribution
        Attribution attribution = proposition.getAttribution();
        String attributionString = "";
        
        // Only process the attribution if there is a attribution phrase TODO is this suitable?
        if (attribution != null && attribution.getAttributionPhrase() != null) {
            StringJoiner attributionAttributesJoiner = new StringJoiner(";");
            String attributionPhrase = attribution.getAttributionPhrase().toString();
            if (!attributionPhrase.isEmpty()) attributionAttributesJoiner.add("phrase:" + attributionPhrase);
            String attributionPredicate = attribution.getPredicateVerb().toString();
            if (!attributionPredicate.isEmpty()) attributionAttributesJoiner.add("predicate:" + attributionPredicate);
            String attributionFactuality = formatFactuality(attribution.getPolarityType().toString(), attribution.getModalityType().toString());
            if (!attributionFactuality.isEmpty()) attributionAttributesJoiner.add("factuality:" + attributionFactuality);
            attributionString = String.format("[attribution=%s]", attributionAttributesJoiner.toString());
        }

        // Quantities
        StringJoiner quantityJoiner = new StringJoiner(";");
        String quantitiesString = "";
        ObjectArrayList<Quantity> quantities = new ObjectArrayList<Quantity>();

        // Add all quantities
        quantities.addAll(proposition.getSubject().getQuantities());
        quantities.addAll(proposition.getRelation().getQuantities());
        quantities.addAll(proposition.getObject().getQuantities());
        if (quantities.size() > 0) {
            for (Quantity q : quantities) {
                StringJoiner quantityPhrase = new StringJoiner(" ");
                for (IndexedWord w : q.getQuantityWords()) {
                    quantityPhrase.add(w.originalText());
                }
                quantityJoiner.add(String.format("QUANT_%s:%s", q.getId(),quantityPhrase.toString()));
            }
            quantitiesString = String.format("[quantities=%s]", quantityJoiner.toString());
        }
        String output = tripleJoiner.toString() + factualityString + attributionString + quantitiesString;
        return output;
    }

    /**
     * format a factuality pair
     * @param polarity: polarity to format
     * @param modality: modality to format
     * @return formatted factuality
     */
    private static String formatFactuality(String polarity, String modality) {
        String factuality = "";
        if (!polarity.isEmpty() && !modality.isEmpty()) {
            if (polarity.equalsIgnoreCase("POSITIVE")) {
                polarity = "+";
            } else {
                polarity = "-";
            }
            if (modality.equalsIgnoreCase("CERTAINTY")) {
                modality = "CT";
            } else {
                modality = "PS";
            }
            factuality = String.format("(%s,%s)", polarity, modality);
        }
        return factuality;
    }

    /**
     * parses a string to a MinIE mode
     * @param s: string to parse
     * @return MinIE mode
     */
    public static MinIE.Mode getMode(String s) {
        MinIE.Mode mode;
        if (s.equalsIgnoreCase("aggressive")) {
            mode = MinIE.Mode.AGGRESSIVE;
        } else if (s.equalsIgnoreCase("dictionary")) {
            mode = MinIE.Mode.DICTIONARY;
        } else if (s.equalsIgnoreCase("complete")) {
            mode = MinIE.Mode.COMPLETE;
        } else {
            mode = MinIE.Mode.SAFE;
        }
        return mode;
    }

    /**
     * load a dictionary from a given location in the option set
     * @param options: option set to read the locations from
     * @return a dictionary read from the specified locations
     * @throws IOException
     */
    public static Dictionary loadDictionary(OptionSet options) throws IOException {
        Dictionary collocationDictionary = null;
        ArrayList<String> filenames = new ArrayList<String>();
        if (!options.has("dict-overwrite")) {
            // if the overwrite option is not set, add the default dictionaries
            filenames.addAll(Arrays.asList(DEFAULT_DICTIONARIES));
        }
        if (options.has("dict")) {
            filenames.addAll((Collection<? extends String>) options.valuesOf("dict"));
        }
        String[] filenamesArray = Arrays.copyOf(filenames.toArray(), filenames.size(), String[].class);
        //logger.info("Loading dictionaries from " + Arrays.toString(filenamesArray));
        collocationDictionary = new Dictionary(filenamesArray);
        //logger.info("Finished loading dictionaries");
        return collocationDictionary;
    }
}
