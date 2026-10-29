package edu.se201.tracker;


import jakarta.validation.constraints.*;

public final class Forms {
    private Forms() {}
    public static class Profile {
        @NotBlank @Size(max=120) private String name = "";
        @NotBlank @Email @Size(max=254) private String email = "";
        @NotNull @Positive private Long schoolId;
        @NotBlank @Size(max=2000) private String applicationMessage = "";
        public String getName() { return name; }
        public void setName(String v) { name = v == null ? "" : v.strip(); }
        public String getEmail() { return email; }
        public void setEmail(String v) { email = v == null ? "" : v.strip(); }
        public Long getSchoolId() { return schoolId; }
        public void setSchoolId(Long v) { schoolId = v; }
        public String getApplicationMessage() { return applicationMessage; }
        public void setApplicationMessage(String v) { applicationMessage = v == null ? "" : v.strip(); }
        public static Profile from(TeacherProfile p) {
            Profile f = new Profile(); f.setName(p.name()); f.setEmail(p.email());
            f.setSchoolId(p.school().id()); f.setApplicationMessage(p.applicationMessage()); return f;
        }
    }
    public static class Registration extends Profile {
        @NotBlank @Size(min=10,max=64) @Pattern(regexp="[\\x00-\\x7F]*", message="Use ASCII characters for the local demo password.")
        private String password = "";
        public String getPassword() { return password; }
        public void setPassword(String v) { password = v; }
    }
    public static class Review {
        @NotNull private ApplicationStatus status;
        @Size(max=2000) private String feedback = "";
        public ApplicationStatus getStatus() { return status; }
        public void setStatus(ApplicationStatus v) { status = v; }
        public String getFeedback() { return feedback; }
        public void setFeedback(String v) { feedback = v == null ? "" : v.strip(); }
    }
    public static class SchoolForm {
        @NotBlank @Size(max=120) private String name = "";
        @NotBlank @Size(max=120) private String city = "";
        public String getName() { return name; }
        public void setName(String v) { name = v == null ? "" : v.strip(); }
        public String getCity() { return city; }
        public void setCity(String v) { city = v == null ? "" : v.strip(); }
    }
    public static class SettingsForm {
        @Min(1) @Max(1000) private int maxActive;
        @Min(10) @Max(3600) private int inactivitySeconds;
        @Min(1) @Max(1000) private int queueCapacity;
        public int getMaxActive() { return maxActive; }
        public void setMaxActive(int v) { maxActive = v; }
        public int getInactivitySeconds() { return inactivitySeconds; }
        public void setInactivitySeconds(int v) { inactivitySeconds = v; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int v) { queueCapacity = v; }
        public static SettingsForm from(AdmissionSettings s) {
            SettingsForm f = new SettingsForm(); f.maxActive=s.maxActive();
            f.inactivitySeconds=s.inactivitySeconds(); f.queueCapacity=s.queueCapacity(); return f;
        }
    }
}
