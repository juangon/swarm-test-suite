package org.wildfly.swarm.ts.hollow.jar.microprofile.config.v11;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class Config11ConfigSource implements ConfigSource {

    private Map<String, String> props;

    private static final String CONFIG11_CONFIGSOURCE = "config11configsource";

    public Config11ConfigSource() {
        props = new HashMap<String, String>();
        props.put("prop.with.config.source", CONFIG11_CONFIGSOURCE);
        props.put("prop.with.ordinal.config.source", CONFIG11_CONFIGSOURCE);
    }

    @Override
    public Map<String, String> getProperties() {
        return props;
    }

    @Override
    public String getValue(String propertyName) {
        return props.get(propertyName);
    }

    @Override
    public String getName() {
        return CONFIG11_CONFIGSOURCE;
    }

}
