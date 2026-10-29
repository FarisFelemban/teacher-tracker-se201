package edu.se201.tracker;


public record TeacherProfile(long accountId, String name, String email, School school,
                             String applicationMessage, ApplicationStatus status, String feedback) {}
