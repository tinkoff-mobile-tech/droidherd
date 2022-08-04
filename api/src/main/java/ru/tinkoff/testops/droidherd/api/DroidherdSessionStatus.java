package ru.tinkoff.testops.droidherd.api;

import java.util.Collection;
import java.util.StringJoiner;

public class DroidherdSessionStatus {
    public Collection<Emulator> emulators;

    public DroidherdSessionStatus() {
    }

    public DroidherdSessionStatus(Collection<Emulator> emulators) {
        this.emulators = emulators;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DroidherdSessionStatus.class.getSimpleName() + "[", "]")
            .add("emulators=" + emulators)
            .toString();
    }
}
