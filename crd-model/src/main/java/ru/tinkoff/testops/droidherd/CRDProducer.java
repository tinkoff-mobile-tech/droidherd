package ru.tinkoff.testops.droidherd;

import java.io.File;
import java.net.URISyntaxException;

public class CRDProducer {
    public File produceCRDFile() {
        try {
            return new File(CRDProducer.class.getClassLoader().getResource("crd/testops.tinkoff.ru_droidherdsessions.yaml").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
