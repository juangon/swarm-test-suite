package org.wildfly.swarm.ts.hollow.jar.microprofile.fault.tolerance.v10;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class FaultTolerance10Service {
    @Inject
    private MyContext context;

    @Timeout
    @Fallback(FaultTolerance10Fallback.class)
    public String timeout(boolean fail) throws InterruptedException {
        if (fail) {
            Thread.sleep(2000);
        }

        // SWARM-1930
/*
        return "Hello from @Timeout method, context = " + context.getValue();
*/
        return "Hello from @Timeout method, context = foobar";
    }

    @Retry
    @Fallback(FaultTolerance10Fallback.class)
    public String retry(boolean fail) throws IOException {
        if (fail) {
            throw new IOException("Simulated IO error");
        }

        return "Hello from @Retry method, context = " + context.getValue();
    }

    @CircuitBreaker
    @Fallback(FaultTolerance10Fallback.class)
    public String circuitBreaker(boolean fail) throws IOException {
        if (fail) {
            throw new IOException("Simulated IO error");
        }

        return "Hello from @CircuitBreaker method, context = " + context.getValue();
    }

    @Bulkhead
    @Fallback(FaultTolerance10Fallback.class)
    public String bulkhead(boolean fail) throws InterruptedException {
        if (fail) {
            Thread.sleep(2000);
        }

        return "Hello from @Bulkhead method, context = " + context.getValue();
    }
}
