package org.wildfly.swarm.ts.hollow.jar.microprofile.v10;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/v10")
public class V10Resource {
    @Inject
    private V10Service hello;

    @GET
    public JsonObject hello() {
        return Json.createObjectBuilder()
                .add("content", hello.hello())
                .build();
    }
}
