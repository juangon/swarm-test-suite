package org.wildfly.swarm.ts.microprofile.fault.tolerance.v10;

import org.apache.http.client.fluent.Request;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.arquillian.DefaultDeployment;
import org.wildfly.swarm.undertow.WARArchive;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RunWith(Arquillian.class)
public class MicroProfileFaultTolerance10Test {

    @Deployment
    public static Archive<?> createDeployment() {
        WARArchive deployment = ShrinkWrap.create(WARArchive.class);
        deployment.addAsResource("META-INF/beans.xml");
        deployment.addAsResource("project-defaults-sync.yml","project-defaults.yml");
        deployment.addPackage(HelloService.class.getPackage());
        return deployment;
    }

    @Test
    @RunAsClient
    public void timeoutOk() throws IOException {
        String response = Request.Get("http://localhost:8080/?operation=timeout&context=foobar").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @Timeout method, context = foobar");
    }

    @Test
    @RunAsClient
    public void timeoutFailure() throws IOException {
        String response = Request.Get("http://localhost:8080/?operation=timeout&context=foobar&fail=true").execute().returnContent().asString();
        assertThat(response).isEqualTo("Fallback Hello, context = foobar");
    }

    @Test
    @RunAsClient
    public void timeoutOkAsync() throws IOException {
        String response = Request.Get("http://localhost:8080/async?operation=timeout").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @Timeout method");
    }

    @Test
    @RunAsClient
    public void timeoutFailureAsync() throws IOException {
        String response = Request.Get("http://localhost:8080/async?operation=timeout&fail=true").execute().returnContent().asString();
        assertThat(response).isEqualTo("Fallback Hello");
    }

    @Test
    @RunAsClient
    public void retryOk() throws IOException {
        String response = Request.Get("http://localhost:8080/?operation=retry&context=foobar").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @Retry method, context = foobar");
    }

    @Test
    @RunAsClient
    public void retryFailure() throws IOException {
        String response = Request.Get("http://localhost:8080/?operation=retry&context=foobar&fail=true").execute().returnContent().asString();
        assertThat(response).isEqualTo("Fallback Hello, context = foobar");
    }

    @Test
    @RunAsClient
    public void retryOkAsync() throws IOException {
        String response = Request.Get("http://localhost:8080/async?operation=retry").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @Retry method");
    }

    @Test
    @RunAsClient
    public void retryFailureAsync() throws IOException {
        String response = Request.Get("http://localhost:8080/async?operation=retry&fail=true").execute().returnContent().asString();
        assertThat(response).isEqualTo("Fallback Hello");
    }

    @Test
    @RunAsClient
    public void circuitBreakerOk() throws IOException {
        String response = Request.Get("http://localhost:8080/?operation=circuit-breaker&context=foobar").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @CircuitBreaker method, context = foobar");
    }

    @Test
    @RunAsClient
    public void circuitBreakerFailure() throws IOException {
        testCircuitBreakerFailure("http://localhost:8080/?operation=circuit-breaker&context=foobar",
                "Fallback Hello, context = foobar",
                "Hello from @CircuitBreaker method, context = foobar");
    }

    @Test
    @RunAsClient
    public void circuitBreakerOkAsync() throws IOException {
        String response = Request.Get("http://localhost:8080/async?operation=circuit-breaker").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @CircuitBreaker method");
    }

    @Ignore("SWARM-1945")
    @Test
    @RunAsClient
    public void circuitBreakerFailureAsync() throws IOException {
        testCircuitBreakerFailure("http://localhost:8080/async?operation=circuit-breaker",
                "Fallback Hello",
                "Hello from @CircuitBreaker method");
    }

    private static void testCircuitBreakerFailure(String url, String expectedFallbackResponse, String expectedOkResponse) throws IOException {
        for (int i = 0; i < 20; i++) {
            String response = Request.Get(url + "&fail=true").execute().returnContent().asString();
            assertThat(response).isEqualTo(expectedFallbackResponse);
        }

        for (int i = 0; i < 10; i++) {
            String response = Request.Get(url).execute().returnContent().asString();
            assertThat(response).isEqualTo(expectedFallbackResponse);
        }

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            String response = Request.Get(url).execute().returnContent().asString();
            assertThat(response).isEqualTo(expectedOkResponse);
        });
    }

    @Test
    @RunAsClient
    public void bulkheadOk() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method, context = foobar", 10);

        // 10 allowed invocations
        // 11 invocations would already trigger fallback
        testBulkhead(10, "http://localhost:8080/?operation=bulkhead&context=foobar", expectedResponses);
    }

    @Test
    @RunAsClient
    public void bulkheadFailure() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method, context = foobar", 10);
        expectedResponses.put("Fallback Hello, context = foobar", 20);

        // 30 = 10 allowed invocations + 20 not allowed invocations that lead to fallback
        // 31 invocations would already trigger fallback rejection
        testBulkhead(30, "http://localhost:8080/?operation=bulkhead&context=foobar&fail=true", expectedResponses);
    }

    @Test
    @RunAsClient
    public void bulkheadOkAsync() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method", 20);

        // 20 = 10 allowed invocations + 10 queued invocations
        // 21 invocations would already trigger fallback
        testBulkhead(20, "http://localhost:8080/async?operation=bulkhead", expectedResponses);
    }

    @Test
    @RunAsClient
    public void bulkheadFailureAsync() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method", 20);
        expectedResponses.put("Fallback Hello", 20);

        // 40 = 10 allowed invocations + 10 queued invocations + 20 not allowed invocations that lead to fallback
        // 41 invocations would already trigger fallback rejection
        testBulkhead(40, "http://localhost:8080/async?operation=bulkhead&fail=true", expectedResponses);
    }

    private static void testBulkhead(int parallelRequests, String url, Map<String, Integer> expectedResponses) throws InterruptedException {
        Set<String> violations = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Queue<String> seenResponses = new ConcurrentLinkedQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        for (int i = 0; i < parallelRequests; i++) {
            executor.submit(() -> {
                try {
                    String response = Request.Get(url).execute().returnContent().asString();
                    seenResponses.add(response);
                } catch (Exception e) {
                    violations.add("Unexpected exception: " + e.getMessage());
                }
            });
        }
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        for (String seenResponse : seenResponses) {
            if (!expectedResponses.containsKey(seenResponse)) {
                violations.add("Unexpected response: " + seenResponse);
            }
        }

        for (Map.Entry<String, Integer> expectedResponse : expectedResponses.entrySet()) {
            int count = 0;
            for (String seenResponse : seenResponses) {
                if (expectedResponse.getKey().equals(seenResponse)) {
                    count++;
                }
            }
            if (count != expectedResponse.getValue()) {
                violations.add("Expected to see " + expectedResponse.getValue() + " occurence(s) but seen " + count
                        + ": " + expectedResponse.getKey());
            }
        }

        assertThat(violations).isEmpty();
    }
}
