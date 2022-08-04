package ru.tinkoff.testops.droidherd.api;

import java.util.Map;
import java.util.StringJoiner;

public class ClientAttributes {
    public String version;
    public String info;
    public CiAttributes ci;
    public Map<String, String> metadata;

    public ClientAttributes() {
    }

    public ClientAttributes(String version, String info, CiAttributes ci, Map<String, String> metadata) {
        this.version = version;
        this.info = info;
        this.ci = ci;
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ClientAttributes.class.getSimpleName() + "[", "]")
            .add("version='" + version + "'")
            .add("info='" + info + "'")
            .add("ci=" + ci)
            .add("metadata=" + metadata)
            .toString();
    }
}
