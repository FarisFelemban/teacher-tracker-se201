package edu.se201.tracker;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {
    private final JdbcTemplate jdbc;
    public SettingsService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public AdmissionSettings get() {
        return jdbc.queryForObject("SELECT * FROM admission_settings WHERE id=1",(r,n) ->
            new AdmissionSettings(r.getInt("max_active"),r.getInt("inactivity_seconds"),r.getInt("queue_capacity")));
    }
    public void save(AdmissionSettings s) {
        jdbc.update("UPDATE admission_settings SET max_active=?,inactivity_seconds=?,queue_capacity=? WHERE id=1",
            s.maxActive(),s.inactivitySeconds(),s.queueCapacity());
    }
}
