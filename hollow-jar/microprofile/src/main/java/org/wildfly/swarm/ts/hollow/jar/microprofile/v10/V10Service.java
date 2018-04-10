package org.wildfly.swarm.ts.hollow.jar.microprofile.v10;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class V10Service {
    public String hello() {
        return "Hello, World!";
    }
}
