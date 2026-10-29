package edu.se201.tracker;


import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TeacherService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final SchoolService schools;
    private static final String SELECT = "SELECT p.*,u.email,s.name school_name,s.city FROM teacher_profile p JOIN user_account u ON u.id=p.account_id JOIN school s ON s.id=p.school_id";
    private static final RowMapper<TeacherProfile> MAPPER = (r,n) -> new TeacherProfile(
        r.getLong("account_id"),r.getString("name"),r.getString("email"),
        new School(r.getLong("school_id"),r.getString("school_name"),r.getString("city")),
        r.getString("application_message"),ApplicationStatus.valueOf(r.getString("status")),r.getString("feedback"));
    public TeacherService(JdbcTemplate jdbc, PasswordEncoder encoder, SchoolService schools) {
        this.jdbc=jdbc; this.encoder=encoder; this.schools=schools;
    }
    public static String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    @Transactional public long register(Forms.Registration form) {
        schools.findById(form.getSchoolId());
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c -> {
            var p=c.prepareStatement("INSERT INTO user_account(email,password_hash,role) VALUES (?,?,'TEACHER')",Statement.RETURN_GENERATED_KEYS);
            p.setString(1,normalizeEmail(form.getEmail())); p.setString(2,encoder.encode(form.getPassword())); return p;
        },key);
        long id=key.getKey().longValue();
        jdbc.update("INSERT INTO teacher_profile(account_id,name,school_id,application_message,status,feedback) VALUES (?,?,?,?,'PENDING','')",
            id,form.getName(),form.getSchoolId(),form.getApplicationMessage());
        return id;
    }
    public TeacherProfile findById(long id) {
        return jdbc.query(SELECT+" WHERE p.account_id=?",MAPPER,id).stream().findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"Teacher not found"));
    }
    public List<TeacherProfile> search(String query, ApplicationStatus status) {
        String q=query.strip().toLowerCase(Locale.ROOT);
        // Position treats %, _ and quotes as literal input, with bound SQL parameters.
        return jdbc.query(SELECT+" WHERE (LOCATE(?,LOWER(p.name))>0 OR LOCATE(?,u.email)>0)"+
            (status==null ? "" : " AND p.status=?")+" ORDER BY LOWER(p.name),p.account_id", MAPPER,
            status==null ? new Object[]{q,q} : new Object[]{q,q,status.name()});
    }
    @Transactional public void update(long id, Forms.Profile form) {
        findById(id); schools.findById(form.getSchoolId());
        jdbc.update("UPDATE user_account SET email=? WHERE id=?",normalizeEmail(form.getEmail()),id);
        jdbc.update("UPDATE teacher_profile SET name=?,school_id=?,application_message=? WHERE account_id=?",
            form.getName(),form.getSchoolId(),form.getApplicationMessage(),id);
    }
    @Transactional public void review(long id, ApplicationStatus status, String feedback) {
        if (jdbc.update("UPDATE teacher_profile SET status=?,feedback=? WHERE account_id=?",status.name(),feedback,id)==0)
            throw new ResponseStatusException(NOT_FOUND,"Teacher not found");
    }
    @Transactional public void delete(long id) {
        findById(id); jdbc.update("DELETE FROM user_account WHERE id=? AND role='TEACHER'",id);
    }
    public long count() { return jdbc.queryForObject("SELECT COUNT(*) FROM teacher_profile",Long.class); }
    public long pendingCount() { return jdbc.queryForObject("SELECT COUNT(*) FROM teacher_profile WHERE status='PENDING'",Long.class); }
    public boolean exists(long id) { return jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE id=?",Long.class,id)>0; }
}
