package org.wildfly.swarm.ts.hollow.jar.microprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.fluent.Request;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MicroprofileHollowJarIT {

    @Before
    public void setup() {
        Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testv10() throws IOException {
        String response = Request.Get("http://localhost:8080/v10").execute().returnContent().asString();
        JsonElement json = new JsonParser().parse(response);
        assertThat(json.isJsonObject()).isTrue();
        assertThat(json.getAsJsonObject().size()).isEqualTo(1);
        assertThat(json.getAsJsonObject().has("content")).isTrue();
        assertThat(json.getAsJsonObject().get("content").getAsString()).isEqualTo("Hello, World!");
    }

    @Test
    public void testConfigv11() throws IOException {
        String response = Request.Get("http://localhost:8080/config11").execute().returnContent().asString();
        assertThat(response).isEqualTo(""
                + "Value of app.timeout: 314159\n"
                + "Value of missing.property: it's present anyway\n"
                + "Config contains app.timeout: true\n"
                + "Config contains missing.property: false\n"
                + "Custom config source prop.with.config.source: config11configsource\n"
                + "Custom config source prop.with.ordinal.config.source: config11configsource\n"
        );
    }

    @Test
    public void testFT10TimeoutOk() throws IOException {
        String response = Request.Get("http://localhost:8080/ft10?operation=timeout&context=foobar").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @Timeout method, context = foobar");
    }

    @Test
    public void testFT10TimeoutFailure() throws IOException {
        String response = Request.Get("http://localhost:8080/ft10?operation=timeout&context=foobar&fail=true").execute().returnContent().asString();
        assertThat(response).isEqualTo("Fallback Hello, context = foobar");
    }

    @Test
    public void testFT10TimeoutOkAsync() throws IOException {
        String response = Request.Get("http://localhost:8080/asyncft10?operation=timeout").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @Timeout method");
    }

    @Test
    public void testFT10TimeoutFailureAsync() throws IOException {
        String response = Request.Get("http://localhost:8080/asyncft10?operation=timeout&fail=true").execute().returnContent().asString();
        assertThat(response).isEqualTo("Fallback Hello");
    }

    @Test
    public void testFT10RetryOk() throws IOException {
        String response = Request.Get("http://localhost:8080/ft10?operation=retry&context=foobar").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @Retry method, context = foobar");
    }

    @Test
    public void testFT10RetryFailure() throws IOException {
        String response = Request.Get("http://localhost:8080/ft10?operation=retry&context=foobar&fail=true").execute().returnContent().asString();
        assertThat(response).isEqualTo("Fallback Hello, context = foobar");
    }

    @Test
    public void testFT10RetryOkAsync() throws IOException {
        String response = Request.Get("http://localhost:8080/asyncft10?operation=retry").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @Retry method");
    }

    @Test
    public void testFT10CircuitBreakerOk() throws IOException {
        String response = Request.Get("http://localhost:8080/ft10?operation=circuit-breaker&context=foobar").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @CircuitBreaker method, context = foobar");
    }

    @Test
    public void testFT10CircuitBreakerFailure() throws IOException {
        testCircuitBreakerFailure("http://localhost:8080/ft10?operation=circuit-breaker&context=foobar",
                "Fallback Hello, context = foobar",
                "Hello from @CircuitBreaker method, context = foobar");
    }

    @Test
    public void testFT10CircuitBreakerOkAsync() throws IOException {
        String response = Request.Get("http://localhost:8080/asyncft10?operation=circuit-breaker").execute().returnContent().asString();
        assertThat(response).isEqualTo("Hello from @CircuitBreaker method");
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
    public void testFT10BulkheadOk() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method, context = foobar", 10);

        // 10 allowed invocations
        // 11 invocations would already trigger fallback
        testBulkhead(10, "http://localhost:8080/ft10?operation=bulkhead&context=foobar", expectedResponses);
    }

    @Test
    public void testFT10BBulkheadFailure() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method, context = foobar", 10);
        expectedResponses.put("Fallback Hello, context = foobar", 10);

        // 20 = 10 allowed invocations + 10 not allowed invocations that lead to fallback
        // 21 invocations would already trigger SWARM-1946
        testBulkhead(20, "http://localhost:8080/ft10?operation=bulkhead&context=foobar&fail=true", expectedResponses);
    }

    @Test
    public void testFT10BulkheadOkAsync() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method", 20);

        // 20 = 10 allowed invocations + 10 queued invocations
        // 21 invocations would already trigger fallback
        testBulkhead(20, "http://localhost:8080/asyncft10?operation=bulkhead", expectedResponses);
    }

    @Test
    public void testFT10BulkheadFailureAsync() throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead method", 20);
        expectedResponses.put("Fallback Hello", 10);

        // 30 = 10 allowed invocations + 10 queued invocations + 10 not allowed invocations that lead to fallback
        // 31 invocations would already trigger SWARM-1946
        testBulkhead(30, "http://localhost:8080/asyncft10?operation=bulkhead&fail=true", expectedResponses);
    }

    private static void testBulkhead(int count, String url, Map<String, Integer> expectedResponses) throws InterruptedException {
        Thread[] threads = new Thread[count];
        Set<String> violations = Collections.newSetFromMap(new ConcurrentHashMap<>());

        Map<String, Integer> actualResponses = new ConcurrentHashMap<>();

        for (int i = 0; i < count; i++) {
            threads[i] = new Thread(() -> {
                try {
                    String response = Request.Get(url).execute().returnContent().asString();
                    synchronized (actualResponses) {
                        actualResponses.put(response, actualResponses.get(response) != null ? actualResponses.get(response) + 1 : 1);
                    }
                } catch (Exception e) {
                    violations.add("Unexpected exception: " + e.getMessage());
                }
            });
        }

        for (int i = 0; i < count; i++) {
            threads[i].start();
        }
        for (int i = 0; i < count; i++) {
            threads[i].join();
        }

        for (String expectedResponse : expectedResponses.keySet()) {
            if (!actualResponses.containsKey(expectedResponse)) {
                violations.add("Never seen expected response:" + expectedResponse);
                continue;
            }

            for (String actualResponse : actualResponses.keySet()) {
                Integer expectedNumber = expectedResponses.get(expectedResponse);
                Integer actualNumber = actualResponses.get(actualResponse);
                if (expectedResponse.equals(actualResponse) && 
                        !expectedNumber.equals(actualNumber)) {
                    violations.add("Response expected number mismatch: " + expectedResponse + ".Expected:" + expectedNumber + ", actual:" + actualNumber);
                }
            }
        }
        assertThat(violations).isEmpty();
    }

    @Test
    public void testHealtCheck() throws IOException {
        String response = Request.Get("http://localhost:8080/health").execute().returnContent().asString();
        JsonElement json = new JsonParser().parse(response);
        assertThat(json.isJsonObject()).isTrue();
        assertThat(json.getAsJsonObject().has("outcome")).isTrue();
        assertThat(json.getAsJsonObject().get("outcome").getAsString()).isEqualTo("UP");
        assertThat(json.getAsJsonObject().has("checks")).isTrue();
        JsonArray checks = json.getAsJsonObject().getAsJsonArray("checks");
        assertThat(checks.size()).isEqualTo(1);
        JsonObject check = checks.get(0).getAsJsonObject();
        assertThat(check.has("name")).isTrue();
        assertThat(check.get("name").getAsString()).isEqualTo("elvis-lives");
        assertThat(check.has("state")).isTrue();
        assertThat(check.get("state").getAsString()).isEqualTo("UP");
        assertThat(check.has("data")).isTrue();
        assertThat(check.get("data").getAsJsonObject().get("it's").getAsString()).isEqualTo("true");
    }
}
