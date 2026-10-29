package edu.se201.tracker;


import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SchoolService {
    private final JdbcTemplate jdbc;
    public SchoolService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<School> findAll() {
        return jdbc.query("SELECT * FROM school ORDER BY name, id", (r,n) -> new School(r.getLong("id"),r.getString("name"),r.getString("city")));
    }
    public School findById(long id) {
        return findAll().stream().filter(s -> s.id()==id).findFirst()
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"School not found"));
    }
    @Transactional public void add(String name, String city) {
        jdbc.update("INSERT INTO school(name,city) VALUES (?,?)",name,city);
    }
    @Transactional public void update(long id, String name, String city) {
        if (jdbc.update("UPDATE school SET name=?,city=? WHERE id=?",name,city,id)==0)
            throw new ResponseStatusException(NOT_FOUND,"School not found");
    }
    @Transactional public void delete(long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM teacher_profile WHERE school_id=?",Long.class,id)>0)
            throw new IllegalArgumentException("This school is used by teachers. Move those teachers to another school first.");
        if (jdbc.update("DELETE FROM school WHERE id=?",id)==0)
            throw new ResponseStatusException(NOT_FOUND,"School not found");
    }
}
