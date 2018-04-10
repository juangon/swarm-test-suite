package org.wildfly.swarm.ts.hollow.jar.microprofile.fault.tolerance.v10;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.util.concurrent.Future;

import static java.util.concurrent.CompletableFuture.completedFuture;

@ApplicationScoped
public class AsyncFaultTolerance10Service {
    @Asynchronous
    @Timeout
    @Fallback(AsyncFaultTolerance10Fallback.class)
    public Future<String> timeout(boolean fail) throws InterruptedException {
        if (fail) {
            Thread.sleep(2000);
        }

        return completedFuture("Hello from @Timeout method");
    }

    @Asynchronous
    @Retry
    @Fallback(AsyncFaultTolerance10Fallback.class)
    public Future<String> retry(boolean fail) throws IOException {
        if (fail) {
            throw new IOException("Simulated IO error");
        }

        return completedFuture("Hello from @Retry method");
    }

    @Asynchronous
    @CircuitBreaker
    @Fallback(AsyncFaultTolerance10Fallback.class)
    public Future<String> circuitBreaker(boolean fail) throws IOException {
        if (fail) {
            throw new IOException("Simulated IO error");
        }

        return completedFuture("Hello from @CircuitBreaker method");
    }

    @Asynchronous
    @Bulkhead
    @Fallback(AsyncFaultTolerance10Fallback.class)
    public Future<String> bulkhead(boolean fail) throws InterruptedException {
        if (fail) {
            Thread.sleep(2000);
        }

        return completedFuture("Hello from @Bulkhead method");
    }
}
