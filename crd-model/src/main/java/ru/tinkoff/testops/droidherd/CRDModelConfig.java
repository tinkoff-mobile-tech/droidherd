package ru.tinkoff.testops.droidherd;

import java.io.File;
import java.net.URISyntaxException;

public class CRDModelConfig {
    private final static File crdFile;

    static {
        try {
            crdFile = new File(CRDModelConfig.class.getClassLoader().getResource("crd/testops.tinkoff.ru_droidherdsessions.yaml").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getCrdFile() {
        return crdFile;
    }
}
