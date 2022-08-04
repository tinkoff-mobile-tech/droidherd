package ru.tinkoff.testops.droidherd.api;

import java.util.Objects;
import java.util.StringJoiner;

public class EmulatorRequest{
    public String image;
    public int quantity;

    public EmulatorRequest() {
    }

    public EmulatorRequest(String image, int quantity) {
        this.image = image;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EmulatorRequest.class.getSimpleName() + "[", "]")
            .add("image='" + image + "'")
            .add("quantity=" + quantity)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmulatorRequest that = (EmulatorRequest) o;
        return quantity == that.quantity && Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(image, quantity);
    }
}

