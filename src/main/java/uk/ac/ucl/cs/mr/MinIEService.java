package uk.ac.ucl.cs.mr;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author Pasquale Minervini
 */

public class MinIEService extends ResourceConfig {
    public MinIEService() {
        super(FactsResource.class, JacksonFeature.class);
    }
}
