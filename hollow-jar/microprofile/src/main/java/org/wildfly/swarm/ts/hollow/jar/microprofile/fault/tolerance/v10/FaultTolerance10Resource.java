package org.wildfly.swarm.ts.hollow.jar.microprofile.fault.tolerance.v10;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.io.IOException;

@Path("/ft10")
public class FaultTolerance10Resource {
    @Inject
    private MyContext context;

    @Inject
    private FaultTolerance10Service hello;

    @GET
    public Response op(@QueryParam("operation") String operation, @QueryParam("fail") boolean fail, @QueryParam("context") String context) {
        this.context.setValue(context);

        String response = "";
        try {
            switch (operation) {
                case "timeout":
                    response = hello.timeout(fail);
                    break;
                case "retry":
                    response = hello.retry(fail);
                    break;
                case "circuit-breaker":
                    response = hello.circuitBreaker(fail);
                    break;
                case "bulkhead":
                    response = hello.bulkhead(fail);
                    break;
            }

            return Response.ok().entity(response).build();
        } catch (InterruptedException | IOException e) {
            return Response.serverError().build();
        }
    }
}
