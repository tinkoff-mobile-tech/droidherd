package ru.tinkoff.testops.droidherd.api;

import java.util.StringJoiner;

public class CiAttributes {
    public String name;
    public String reference;
    public String repository;
    public String jobUrl;
    public String triggeredBy;

    public static final CiAttributes EMPTY = new CiAttributes("", "", "", "", "");

    public CiAttributes() {
    }

    public CiAttributes(String name, String reference, String repository, String jobUrl, String triggeredBy) {
        this.name = name;
        this.reference = reference;
        this.repository = repository;
        this.jobUrl = jobUrl;
        this.triggeredBy = triggeredBy;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CiAttributes.class.getSimpleName() + "[", "]")
            .add("name='" + name + "'")
            .add("reference='" + reference + "'")
            .add("repository='" + repository + "'")
            .add("jobUrl='" + jobUrl + "'")
            .add("triggeredBy='" + triggeredBy + "'")
            .toString();
    }
}
