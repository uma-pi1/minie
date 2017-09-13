<img src="https://gkiril.github.io/minie/images/minie_logo.png" align="right" width="150" />

# MinIE - Open Information Extraction system

An Open Information Extraction system, providing useful extractions:
* represents contextual information with semantic annotations
* identifies and removing words that are considered overly specific
* high precision/recall 
* shorter, semantically enriched extractions

## Open Information Extraction (OIE)
Open Information Extraction (OIE) systems aim to extract unseen relations and their arguments from unstructured text in unsupervised manner. In its simplest form, given a natural language sentence, they extract information in the form of a triple, consisted of subject (S), relation (R) and object (O). 

Suppose we have the following input sentence:
```
AMD, which is based in U.S., is a technology company.
```

An OIE system aims to make the following extractions: 

```
("AMD"; "is based in"; "U.S.")
("AMD"; "is"; "technology company")
```

## Demo

In general, the code for running MinIE in all of its modes is almost the same. The only exception is MinIE-D, which requires additional input (list of multi-word dictionaries). You can still use MinIE-D without providing multi-word dictionaries, but then MinIE-D assumes that you provided an empty dictionary, thus minimizing all the words which are candidates for dropping. 

The following code demo is for MinIE-S (note that you can use the same for the rest of the modes, you just need to change `MinIE.Mode` accordingly):

```java
import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.minie.annotation.AnnotatedProposition;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class Demo {
    public static void main(String args[]) {
        // Dependency parsing pipeline initialization
        StanfordCoreNLP parser = CoreNLPUtils.StanfordDepNNParser();
        
        // Input sentence
        String sentence = "The Joker believes that the hero Batman was not actually born in 
                           foggy Gotham City.";
        
        // Generate the extractions (With SAFE mode)
        MinIE minie = new MinIE(sentence, parser, MinIE.Mode.SAFE);
        
        // Print the extractions
        System.out.println("\nInput sentence: " + sentence);
        System.out.println("=============================");
        System.out.println("Extractions:");
        for (AnnotatedProposition ap: minie.getPropositions()) {
            System.out.println("\tTriple: " + ap.getTripleAsString());
            System.out.print("\tFactuality: " + ap.getFactualityAsString());
            if (ap.getAttribution().getAttributionPhrase() != null) 
                System.out.print("\tAttribution: " + ap.getAttribution().toStringCompact());
            else
                System.out.print("\tAttribution: NONE");
            System.out.println("\n\t----------");
        }
        
        System.out.println("\n\nDONE!");
    }
}
```

If you want to use MinIE-D, then the only difference would be the way MinIE is called:

```java
import de.uni_mannheim.utils.Dictionary;
. . .

// Initialize dictionaries
String [] filenames = new String [] {"/minie-resources/wiki-freq-args-mw.txt", 
                                     "/minie-resources/wiki-freq-rels-mw.txt"};
Dictionary collocationsDict = new Dictionary(filenames);

MinIE minie = new MinIE(sentence, parser, MinIE.Mode.DICTIONARY, collocationsDict);

```

## Resources

* **Documentation:** for more thorough documentation for the code, please visit [MinIE's project page](https://gkiril.github.io/minie/).
* **Paper:** _"MinIE: Minimizing Facts in Open Information Extraction"_ - appeared on EMNLP 2017 [[pdf]](http://aclweb.org/anthology/D/D17/D17-1277.pdf)


## Citing
If you use MinIE in your work, please cite our paper:

```
@inproceedings{gashteovski2017minie,
  title={MinIE: Minimizing Facts in Open Information Extraction},
  author={Gashteovski, Kiril and Gemulla, Rainer and Del Corro, Luciano},
  booktitle={Proceedings of the 2017 Conference on Empirical Methods in Natural Language Processing},
  pages={2620--2630},
  year={2017}
}
```
