package edu.se201.tracker;


import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedData implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final TeacherService teachers;
    private final boolean demoTeachers;
    public SeedData(JdbcTemplate jdbc, PasswordEncoder encoder, TeacherService teachers,
            @Value("${tracker.demo-teachers:false}") boolean demoTeachers) {
        this.jdbc=jdbc; this.encoder=encoder; this.teachers=teachers; this.demoTeachers=demoTeachers;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM admission_settings",Long.class)==0)
            jdbc.update("INSERT INTO admission_settings VALUES (1,20,300,100)");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM school",Long.class)==0) {
            jdbc.update("INSERT INTO school(name,city) VALUES ('Cedar Grove School','Riyadh'),('Horizon Academy','Jeddah'),('Palm Learning School','Dammam')");
        }
        // Existing accounts are never overwritten or reset during startup.
        if (jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE email='admin@demo.test'",Long.class)==0)
            jdbc.update("INSERT INTO user_account(email,password_hash,role) VALUES (?,?, 'ADMIN')","admin@demo.test",encoder.encode("AdminDemo2026!"));
        if (demoTeachers && jdbc.queryForObject("SELECT COUNT(*) FROM teacher_profile",Long.class)==0) {
            addDemoTeacher("Amina Cedar", "amina@demo.test", "Cedar Grove School");
            addDemoTeacher("Bilal Horizon", "bilal@demo.test", "Horizon Academy");
        }
    }
    private void addDemoTeacher(String name, String email, String schoolName) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE email=?",Long.class,email)>0) return;
        var ids=jdbc.queryForList("SELECT id FROM school WHERE name=? ORDER BY id",Long.class,schoolName);
        long schoolId=ids.isEmpty() ? jdbc.queryForObject("SELECT MIN(id) FROM school",Long.class) : ids.getFirst();
        var form=new Forms.Registration(); form.setName(name); form.setEmail(email); form.setSchoolId(schoolId);
        form.setPassword("TeacherDemo2026!"); form.setApplicationMessage("I teach introductory computing."); teachers.register(form);
    }
}
