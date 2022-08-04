package ru.tinkoff.testops.droidherd.api;

import java.util.Map;
import java.util.StringJoiner;

public class DroidherdStatus {

    public Map<String, Map<String, Integer>> sessionsByClient;

    public DroidherdStatus() {
    }

    public DroidherdStatus(Map<String, Map<String, Integer>> sessionsByClient) {
        this.sessionsByClient = sessionsByClient;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DroidherdStatus.class.getSimpleName() + "[", "]")
            .add("sessionsByClient=" + sessionsByClient)
            .toString();
    }
}
