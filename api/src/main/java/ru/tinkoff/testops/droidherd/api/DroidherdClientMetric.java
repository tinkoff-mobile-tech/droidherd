package ru.tinkoff.testops.droidherd.api;

public class DroidherdClientMetric {
    public String key;
    public double value;

    public DroidherdClientMetric() {}

    public DroidherdClientMetric(String key, double value) {
        this.key = key;
        this.value = value;
    }

    public enum MetricType {
        HISTOGRAM, GAUGE
    }
}
