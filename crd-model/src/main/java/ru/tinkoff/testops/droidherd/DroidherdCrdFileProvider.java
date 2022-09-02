package ru.tinkoff.testops.droidherd;

import java.io.IOException;
import java.io.InputStream;

public class DroidherdCrdFileProvider {
    public String provide() {
        InputStream resource = DroidherdCrdFileProvider.class.getClassLoader().getResourceAsStream("crd/testops.tinkoff.ru_droidherdsessions.yaml");
        try {
            return new String(resource.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
