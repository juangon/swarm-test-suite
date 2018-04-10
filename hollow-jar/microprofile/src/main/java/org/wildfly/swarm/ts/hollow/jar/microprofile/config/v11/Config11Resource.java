package org.wildfly.swarm.ts.hollow.jar.microprofile.config.v11;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import java.io.IOException;

@Path("/config11")
public class Config11Resource {
    @Inject
    @ConfigProperty(name = "app.timeout")
    private long appTimeout;

    @Inject
    @ConfigProperty(name = "missing.property", defaultValue = "it's present anyway")
    private String missingProperty;

    @Inject
    private Config config;

    @Inject
    @ConfigProperty(name = "prop.with.config.source")
    private String propWithConfigSource;

    @Inject
    @ConfigProperty(name = "prop.with.ordinal.config.source")
    private String propWithOrdinalConfigSource;

    @GET
    public Response op() throws IOException {
        String response = "Value of app.timeout: " + appTimeout + "\n"
            + "Value of missing.property: " + missingProperty + "\n"
            + "Config contains app.timeout: " + config.getOptionalValue("app.timeout", String.class).isPresent() + "\n"
            + "Config contains missing.property: " + config.getOptionalValue("missing.property", String.class).isPresent() + "\n"
            + "Custom config source prop.with.config.source: " + propWithConfigSource + "\n"
            + "Custom config source prop.with.ordinal.config.source: " + propWithOrdinalConfigSource + "\n";

        return Response.ok().entity(response).build();
    }
}
