package ru.tinkoff.testops.droidherd.api;

import java.util.List;
import java.util.StringJoiner;

public class DroidherdClientStatus {
    public int quota;
    public int used;
    public int available;
    public List<DroidherdSessionDetails> sessions;

    public DroidherdClientStatus() {
    }

    public DroidherdClientStatus(int quota, int used, List<DroidherdSessionDetails> sessions) {
        this.quota = quota;
        this.used = used;
        this.available = quota - used;
        this.sessions = sessions;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DroidherdClientStatus.class.getSimpleName() + "[", "]")
            .add("quota=" + quota)
            .add("used=" + used)
            .add("available=" + available)
            .add("sessions=" + sessions)
            .toString();
    }
}
