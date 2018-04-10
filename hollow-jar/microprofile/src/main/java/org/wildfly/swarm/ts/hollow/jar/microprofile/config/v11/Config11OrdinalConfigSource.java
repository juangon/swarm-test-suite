package org.wildfly.swarm.ts.hollow.jar.microprofile.config.v11;

public class Config11OrdinalConfigSource extends Config11ConfigSource {

    private static final String CONFIG11_ORDINAL_CONFIGSOURCE = "config11ordinalconfigsource";

    public Config11OrdinalConfigSource() {
        super();
        getProperties().put("prop.with.ordinal.config.source", CONFIG11_ORDINAL_CONFIGSOURCE);
    }

    @Override
    public String getName() {
        return CONFIG11_ORDINAL_CONFIGSOURCE;
    }

    @Override
    public int getOrdinal() {
       return 99;
    }

}
