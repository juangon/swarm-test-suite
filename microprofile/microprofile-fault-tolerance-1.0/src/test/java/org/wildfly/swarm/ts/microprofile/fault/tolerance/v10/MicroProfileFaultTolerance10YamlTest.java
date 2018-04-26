package org.wildfly.swarm.ts.microprofile.fault.tolerance.v10;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.fluent.Request;
import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.swarm.undertow.WARArchive;

import static org.awaitility.Awaitility.await;

@RunWith(Arquillian.class)
public class MicroProfileFaultTolerance10YamlTest {

    @Before
    public void setup() {
        Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS);
    }

    @Deployment
    public static Archive<?> createDeployment() {
        WARArchive deployment = ShrinkWrap.create(WARArchive.class);
        deployment.addAsResource("META-INF/beans.xml");
        deployment.addAsResource("project-defaults-async.yml","project-defaults.yml");
        deployment.addPackage(HelloService.class.getPackage());
        return deployment;
    }

    @Test
    @RunAsClient
    public void circuitBreakerFailure() throws IOException {
        testCircuitBreakerFailure("http://localhost:8080/?operation=circuit-breaker&context=foobar",
                "Fallback Hello, context = foobar",
                "Hello from @CircuitBreaker method, context = foobar");
    }

    private static void testCircuitBreakerFailure(String url, String expectedFallbackResponse, String expectedOkResponse) throws IOException {
        int failureCounts =0;
        int totalRequests = 3;
        {
            for (int i = 0; i < totalRequests; i++) {
                String response = Request.Get(url + "&fail=true").execute().returnContent().asString();
                assertThat(response).isEqualTo(expectedFallbackResponse);
            }
        }
        /*{
            for (int i = 0; i < 10; i++) {
                String response = Request.Get(url).execute().returnContent().asString();
                if (response.equals(expectedFallbackResponse)) {
                    failureCounts++;
                } else if (response.equals(expectedOkResponse)) {
                    okCounts++;
                }
            }
            assertThat(failureCounts).isLessThan(10);
        }*/

        /*for (int i = 0; i < 10; i++) {
            String response = Request.Get(url).execute().returnContent().asString();
            assertThat(response).isEqualTo(expectedFallbackResponse);
        }*/

        await().atMost(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            String response = Request.Get(url).execute().returnContent().asString();
            assertThat(response).isEqualTo(expectedOkResponse);
        });

        /*await().atMost(500, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            String response = Request.Get(url).execute().returnContent().asString();
            assertThat(response).isEqualTo(expectedFallbackResponse);
        });*/
    }
}
