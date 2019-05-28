package uk.ac.ucl.cs.mr;

import java.util.List;
import java.util.ArrayList;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.uni_mannheim.minie.MinIE;
import de.uni_mannheim.minie.annotation.AnnotatedPhrase;
import de.uni_mannheim.minie.annotation.AnnotatedProposition;
import de.uni_mannheim.utils.coreNLP.CoreNLPUtils;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * @author Pasquale Minervini
 */

@Path("/query")
public class FactsResource {

    private static final StanfordCoreNLP parser = CoreNLPUtils.StanfordDepNNParser();

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    public FactsBean query(String sentence) {
        MinIE minie = new MinIE(sentence, FactsResource.parser, MinIE.Mode.SAFE);

        List<Fact> facts = new ArrayList<>();

        for (AnnotatedProposition ap: minie.getPropositions()) {
            List<AnnotatedPhrase> triple = ap.getTriple();

            String s = triple.get(0).toString();
            String p = triple.get(1).toString();
            String o = triple.get(2).toString();

            Fact fact = new Fact(s, p, o);
            facts.add(fact);
        }

        return new FactsBean(facts);
    }
}
