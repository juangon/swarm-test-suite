package org.wildfly.swarm.ts.hollow.jar.microprofile.fault.tolerance.v10;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Path("/asyncft10")
public class AsyncFaultTolerance10Resource {
    @Inject
    private AsyncFaultTolerance10Service asyncHello;

    @GET
    public Response op(@QueryParam("operation") String operation, @QueryParam("fail") boolean fail) throws IOException {

        String response = "";
        try {
            switch (operation) {
                case "timeout":                    
                    response = asyncHello.timeout(fail).get();
                    break;
                case "retry":
                    response = asyncHello.retry(fail).get();
                    break;
                case "circuit-breaker":
                    response = asyncHello.circuitBreaker(fail).get();
                    break;
                case "bulkhead":
                    response = asyncHello.bulkhead(fail).get();
                    break;
            }

            return Response.ok().entity(response).build();
        } catch (InterruptedException | ExecutionException e) {
            return Response.serverError().build();
        }
    }
}
