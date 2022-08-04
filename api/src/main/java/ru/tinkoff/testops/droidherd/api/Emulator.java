package ru.tinkoff.testops.droidherd.api;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class Emulator {
    public String id;
    public String image;
    public String adb;
    public Map<String, String> extraUris;

    public Emulator() {
    }

    public Emulator(String id, String image, String adb, Map<String, String> extraUris) {
        this.id = id;
        this.image = image;
        this.adb = adb;
        this.extraUris = extraUris;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Emulator.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("image='" + image + "'")
            .add("adb='" + adb + "'")
            .add("extraUris=" + extraUris)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Emulator emulator = (Emulator) o;
        return Objects.equals(id, emulator.id) && Objects.equals(image, emulator.image) && Objects.equals(adb, emulator.adb) && Objects.equals(extraUris, emulator.extraUris);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, image, adb, extraUris);
    }
}
