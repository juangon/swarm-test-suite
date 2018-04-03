package org.wildfly.swarm.ts.hollow.jar.full;

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.http.client.fluent.Request;
import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.swarm.ts.hollow.jar.full.remote.ejb.RemoteEjb;

import javax.mail.internet.MimeMultipart;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class FullHollowJarIT {
    private static GreenMail greenMail;

    @BeforeClass
    public static void setUpMailServer() {
        ServerSetup serverSetup = new ServerSetup(3026, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
        greenMail = new GreenMail(serverSetup);
        greenMail.start();
    }

    @AfterClass
    public static void tearDownMailServer() {
        greenMail.stop();
    }

    @Before
    public void setup() {
        Awaitility.setDefaultPollInterval(10, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testBatch() throws IOException {
        {
            String response = Request.Get("http://localhost:8080/batch?operation=start").execute().returnContent().asString();
            assertThat(response).isEqualTo("1");
        }

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            String response = Request.Get("http://localhost:8080/batch?operation=results").execute().returnContent().asString();
            assertThat(response).isEqualTo("0\n1\n2\n3\n4\n5\n6\n7\n8\n9\n");
        });
    }

    @Test
    public void testMessaging() throws IOException {
        String response = Request.Get("http://localhost:8080/messaging?operation=sendQueue").execute().returnContent().asString();
        assertThat(response).isEqualTo("OK");

        String resultsURL = "http://localhost:8080/messaging?operation=results";

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(Request.Get(resultsURL).execute().returnContent().asString()).isEqualTo("1 in queue\n");
        });

        response = Request.Get("http://localhost:8080/messaging?operation=sendTopic").execute().returnContent().asString();
        assertThat(response).isEqualTo("OK");

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(Request.Get(resultsURL).execute().returnContent().asString()).isEqualTo("2 in topic\n");
        });
    }

    @Test
    public void testRemoteEjb() throws NamingException {
        Hashtable<String, Object> jndiProps = new Hashtable<>();
        jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        jndiProps.put(Context.PROVIDER_URL, "remote+http://localhost:8080");

        Context ctx = null;
        try {
            ctx = new InitialContext(jndiProps);
            RemoteEjb service = (RemoteEjb) ctx.lookup("ejb:/ts-hollow-jar-full-1.0.0-SNAPSHOT/RemoteEjbImpl!org.wildfly.swarm.ts.hollow.jar.full.remote.ejb.RemoteEjb");
            assertThat(service.method()).isEqualTo("remote ejb method");
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }

    @Test
    public void testMail() throws IOException {
        String response = Request.Get("http://localhost:8080/mail").execute().returnContent().asString();
        assertThat(response).isEqualTo("OK");

        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> {
            GreenMailUser user = greenMail.setUser("swarm-test-suite@example.com", null);
            MailFolder inbox = greenMail.getManagers().getImapHostManager().getInbox(user);
            List<StoredMessage> messages = inbox.getMessages();
            assertThat(messages).isNotEmpty();
            MimeMultipart content = (MimeMultipart) messages.get(0).getMimeMessage().getContent();
            String body = content.getBodyPart(0).getContent().toString();
            assertThat(body).isEqualTo("FOO");
        });
    }

    @Test
    public void testResourceAdapter() throws IOException {
        String response = Request.Get("http://localhost:8080/ra").execute().returnContent().asString();
        assertThat(response).isEqualTo("OK");
    }

    @Test
    public void testJaxWS() throws IOException {
        String response = Request.Get("http://localhost:8080/TestWebService?wsdl").execute().returnContent().asString();
        assertThat(response).contains("TestWebService").contains("webMethod");
    }
}
