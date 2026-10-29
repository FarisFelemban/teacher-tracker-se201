package edu.se201.tracker;


public enum ApplicationStatus {
    PENDING, APPROVED, REJECTED;
    public String getLabel() { return name().charAt(0) + name().substring(1).toLowerCase(); }
}
