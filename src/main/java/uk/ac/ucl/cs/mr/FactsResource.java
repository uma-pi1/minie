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

        for (AnnotatedProposition ap : minie.getPropositions()) {
            List<AnnotatedPhrase> triple = ap.getTriple();

            String s = "";
            String p = "";
            String o = "";
            try {
                s = triple.get(0).toString();
                p = triple.get(1).toString();
                o = triple.get(2).toString();
            } catch (IndexOutOfBoundsException ignored){}

            Fact fact = new Fact(s, p, o);
            facts.add(fact);
        }

        return new FactsBean(facts);
    }
}
