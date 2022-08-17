package ru.tinkoff.testops.droidherd;

import java.io.File;
import java.net.URISyntaxException;

public class DroidherdCrdFileProvider {
    public File provide() {
        try {
            return new File(DroidherdCrdFileProvider.class.getClassLoader().getResource("crd/testops.tinkoff.ru_droidherdsessions.yaml").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
