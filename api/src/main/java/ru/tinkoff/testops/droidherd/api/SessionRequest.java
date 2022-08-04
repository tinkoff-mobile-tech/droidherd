package ru.tinkoff.testops.droidherd.api;

import java.util.List;
import java.util.StringJoiner;

public class SessionRequest {
    public ClientAttributes clientAttributes;
    public List<EmulatorRequest> requests;
    public List<EmulatorParameter> parameters;
    public boolean debug;

    public SessionRequest() {
    }

    public SessionRequest(ClientAttributes clientAttributes, List<EmulatorRequest> requests, List<EmulatorParameter> parameters, boolean debug) {
        this.clientAttributes = clientAttributes;
        this.requests = requests;
        this.parameters = parameters;
        this.debug = debug;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SessionRequest.class.getSimpleName() + "[", "]")
            .add("clientAttributes=" + clientAttributes)
            .add("requests=" + requests)
            .add("parameters=" + parameters)
            .add("debug=" + debug)
            .toString();
    }
}

