package ru.tinkoff.testops.droidherd.api;

import java.util.Collection;
import java.util.StringJoiner;

public class DroidherdSessionDetails {
    public String id;
    public int quantity;
    public String createdAt;
    public long running;
    public Collection<Emulator> emulatorsList;
    public CiAttributes ciAttributes;

    public DroidherdSessionDetails() {
    }

    public DroidherdSessionDetails(String id, int quantity, String createdAt, long running, Collection<Emulator> emulatorsList, CiAttributes ciAttributes) {
        this.id = id;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.running = running;
        this.emulatorsList = emulatorsList;
        this.ciAttributes = ciAttributes;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DroidherdSessionDetails.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("quantity=" + quantity)
            .add("createdAt='" + createdAt + "'")
            .add("running=" + running)
            .add("emulatorsList=" + emulatorsList)
            .add("ciAttributes=" + ciAttributes)
            .toString();
    }
}

