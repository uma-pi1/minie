<img src="https://uma-pi1.github.io/minie/images/minie_logo.png" align="right" width="150" />

# MinIE: Open Information Extraction system

* [Open Information Extraction - brief introduction](#open-information-extraction---brief-introduction)
* [MinIE - Open Information Extraction system](#minie---open-information-extraction-system)
* [Version](#version)
* [Demo](#demo)
* [MinIE service](#minie-service)
* [Python wrapper](#python-wrapper)
* [Resources](#resources)
* [MinIE in other downstream applications](#minie-in-other-downstream-applications)
* [Citing](#citing)

---

## Open Information Extraction - brief introduction

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

## MinIE - Open Information Extraction system

An Open Information Extraction system, providing useful extractions:
* represents contextual information with semantic annotations
* identifies and removes words that are considered overly specific
* high precision/recall 
* shorter, semantically enriched extractions

## Version

This is the latest version of MinIE, which may give you different (improved!) results than the original EMNLP-2017 version. The EMNLP-2017 version can be found [here](https://github.com/uma-pi1/minie/tree/5500282a4edd213910ecbcd95a94fca86a057e2d).

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

// Use MinIE
MinIE minie = new MinIE(sentence, parser, MinIE.Mode.DICTIONARY, collocationsDict);

```

In `resources/minie-resources/` you can find multi-word dictionaries constructed from WordNet (wn-mwe.txt) and from wiktionary (wiktionary-mw-titles.txt). This will give you some sort of functionality for MinIE-D. The multi-word dictionaries constructed with MinIE-S (as explained in the paper) are not here because of their size. If you want to use them, please refer to the download link in the section "Resources".

## MinIE Service

Code for exposing MinIE as a service (developed by [Pasquale Minervini](https://github.com/pminervini)).

Start with:

```bash
$ mvn clean compile exec:java
[..]

[INFO] --- exec-maven-plugin:1.6.0:java (default-cli) @ minie-service ---
MinIE Service
Mar 06, 2018 8:43:13 PM org.glassfish.grizzly.http.server.NetworkListener start
INFO: Started listener bound to [localhost:8080]
Mar 06, 2018 8:43:13 PM org.glassfish.grizzly.http.server.HttpServer start
INFO: [HttpServer] Started.
Application started.
Stop the application using CTRL+C
```

Use the service with:

```bash
$ curl 'http://localhost:8080/minie/query' -X POST -d 'Obama visited the white house.' | jq .
{
  "facts": [
    {
      "subject": "Obama",
      "predicate": "visited",
      "object": "white house"
    }
  ]
}
```

## Python wrapper

You can find a python wrapper for MinIE [here](https://github.com/mmxgn/miniepy). If you want to use MinIE with python, please follow the guidelines provided on the repo's README. 


## Resources

* **Documentation:** for more thorough documentation for the code, please visit [MinIE's project page](https://uma-pi1.github.io/minie/).
* **Paper:** _"MinIE: Minimizing Facts in Open Information Extraction"_ - appeared on EMNLP 2017 [[pdf]](http://aclweb.org/anthology/D/D17/D17-1278.pdf)
* **Dictionary:** Wikipedia: frequent relations and arguments [[zip]](https://www.uni-mannheim.de/media/Einrichtungen/dws/Files_Research/Software/MinIE/wiki-freq-args-rels.zip)
* **Experiments datasets:** datasets from the paper
  * [NYT](https://www.uni-mannheim.de/media/Einrichtungen/dws/Files_Research/Software/MinIE/NYT.zip)
  * [Wiki](https://www.uni-mannheim.de/media/Einrichtungen/dws/Files_Research/Software/MinIE/Wiki.zip)
  * [NYT-10k](https://www.uni-mannheim.de/media/Einrichtungen/dws/Files_Research/Software/MinIE/nyt10k.zip)

## MinIE in other downstream applications

* **Fact Salience:** MinIE is used for the task of "Fact Salience". Details can be found in the paper [*"Facts that Matter"*](http://www.aclweb.org/anthology/D18-1129) by Marco Ponza, Luciano Del Corro, Gerhard Weikum, published on EMNLP 2018. As a result, the fact salience system [SalIE](https://github.com/mponza/SalIE) was published.
* **Large-Scale OIE:** MinIE was used to create the largest OIE corpus to date - [OPIEC](https://www.uni-mannheim.de/dws/research/resources/opiec/). The corpus contains more than 341M triples. Details can be found in the paper [*"OPIEC: An Open Information Extraction Corpus"*](https://arxiv.org/pdf/1904.12324.pdf) by Kiril Gashteovski, Sebastian Wanner, Sven Hertling, Samuel Broscheit, Rainer Gemulla, published on AKBC 2019.
* **OIE from Scientific Publications:** An extension of MinIE was created which provides structured knowledge enriched with semantic information about citations - [MinScIE: Citation-centered Open Information Extraction](https://github.com/gkiril/MinSCIE). Details can be found in the paper [*"MinScIE: Citation-centered Open Information Extraction"*](https://madoc.bib.uni-mannheim.de/49216/1/_JCDL19Demo__MinScIE%20%284%29.pdf) by Anne Lauscher, Yide Song and Kiril Gashteovski, published on JCDL 2019.
* **Entity Aspect Linking:** MinIE was used for creating EAL: toolkit and dataset for entity-aspect linking. Details can be found in the paper [*"EAL: A Toolkit and Dataset for Entity-Aspect Linking"*](https://madoc.bib.uni-mannheim.de/49596/1/EAL.pdf) by Federico Nanni, Jingyi Zhang, Ferdinand Betz, Kiril Gashteovski, published on JCDL 2019.

## Citing
If you use MinIE in your work, please cite our paper:

```
@inproceedings{gashteovski2017minie,
  title={MinIE: Minimizing Facts in Open Information Extraction},
  author={Gashteovski, Kiril and Gemulla, Rainer and Del Corro, Luciano},
  booktitle={Proceedings of the 2017 Conference on Empirical Methods in Natural Language Processing},
  pages={2630--2640},
  year={2017}
}
```
