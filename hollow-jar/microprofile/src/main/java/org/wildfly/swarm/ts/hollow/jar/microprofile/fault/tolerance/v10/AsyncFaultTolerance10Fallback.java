package org.wildfly.swarm.ts.hollow.jar.microprofile.fault.tolerance.v10;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import java.util.concurrent.Future;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class AsyncFaultTolerance10Fallback implements FallbackHandler<Future<String>> {
    @Override
    public Future<String> handle(ExecutionContext context) {
        return completedFuture("Fallback Hello");
    }
}
