package ru.tinkoff.testops.droidherd.api;

import java.util.StringJoiner;

public class EmulatorParameter {
    public String name;
    public String value;

    public EmulatorParameter() {
    }

    public EmulatorParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EmulatorParameter.class.getSimpleName() + "[", "]")
            .add("name='" + name + "'")
            .add("value='" + value + "'")
            .toString();
    }
}
