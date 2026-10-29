package edu.se201.tracker;


public record AdmissionSettings(int maxActive, int inactivitySeconds, int queueCapacity) {
    public AdmissionSettings {
        if (maxActive < 1 || maxActive > 1000 || inactivitySeconds < 10 || inactivitySeconds > 3600
                || queueCapacity < 1 || queueCapacity > 1000) {
            throw new IllegalArgumentException("Capacity must be 1–1000 and timeout 10–3600 seconds.");
        }
    }
}
