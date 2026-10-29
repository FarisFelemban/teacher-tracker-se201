package edu.se201.tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TeacherService teachers;
    @Autowired SchoolService schools;
    @Autowired SettingsService settings;
    @Autowired AdmissionService admission;
    @Autowired PasswordEncoder encoder;
    long schoolId;
    @BeforeEach void setup() {
        var snapshot=admission.snapshot(); snapshot.active().forEach(admission::release); snapshot.waiting().forEach(admission::release);
        jdbc.update("DELETE FROM user_account WHERE role='TEACHER'");
        schoolId=schools.findAll().getFirst().id(); admission.configure(new AdmissionSettings(20,300,100));
    }
    Forms.Registration form(String email) {
        var f=new Forms.Registration(); f.setName("Demo Teacher"); f.setEmail(email); f.setSchoolId(schoolId);
        f.setPassword("TeacherDemo2026!"); f.setApplicationMessage("I teach introductory computing."); return f;
    }
    MockHttpSession login(String email) throws Exception {
        return (MockHttpSession)mvc.perform(formLogin("/login").user("email",email).password("TeacherDemo2026!"))
            .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
    MockHttpSession admin() throws Exception {
        return (MockHttpSession)mvc.perform(formLogin("/login").user("email","admin@demo.test").password("AdminDemo2026!"))
            .andExpect(authenticated().withRoles("ADMIN")).andReturn().getRequest().getSession(false);
    }
    @Test void registrationNormalizesEmailHashesPasswordAndIgnoresInjectedPrivileges() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("name","Teacher One").param("email","New@Demo.Test")
            .param("password","TeacherDemo2026!").param("schoolId",String.valueOf(schoolId)).param("applicationMessage","New application")
            .param("role","ADMIN").param("status","APPROVED").param("feedback","forged"))
            .andExpect(redirectedUrl("/login"));
        var profile=teachers.search("new@demo.test",null).getFirst(); assertThat(profile.status()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(profile.feedback()).isEmpty();
        var row=jdbc.queryForMap("SELECT * FROM user_account WHERE id=?",profile.accountId());
        assertThat(row.get("ROLE")).isEqualTo("TEACHER");
        assertThat(encoder.matches("TeacherDemo2026!",(String)row.get("PASSWORD_HASH"))).isTrue();
    }
    @Test void caseVariantDuplicateAndInvalidSchoolAreRejectedWithoutPartialAccounts() throws Exception {
        teachers.register(form("duplicate@demo.test")); long before=teachers.count();
        mvc.perform(post("/register").with(csrf()).param("name","Duplicate").param("email","DUPLICATE@DEMO.TEST")
            .param("password","TeacherDemo2026!").param("schoolId",String.valueOf(schoolId)).param("applicationMessage","Test"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("already registered")));
        mvc.perform(post("/register").with(csrf()).param("name","").param("email","invalid")
            .param("password","short").param("schoolId","999999").param("applicationMessage",""))
            .andExpect(model().attributeHasFieldErrors("form","name","email","password","schoolId","applicationMessage"));
        assertThat(teachers.count()).isEqualTo(before);
    }
    @Test void successfulAndIncorrectLoginAreHandled() throws Exception {
        teachers.register(form("login@demo.test")); login("LOGIN@DEMO.TEST");
        mvc.perform(formLogin("/login").user("email","login@demo.test").password("incorrect"))
            .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }
    @Test void teacherCannotAccessAdminAndOwnProfileCannotTargetAnotherAccount() throws Exception {
        long a=teachers.register(form("a@demo.test")); long b=teachers.register(form("b@demo.test")); var session=login("a@demo.test");
        mvc.perform(get("/admin/teachers").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/teacher/dashboard").session(session)).andExpect(status().isOk());
        mvc.perform(get("/teacher/profile/"+b).session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/teacher/profile").session(session).with(csrf()).param("id",String.valueOf(b)).param("accountId",String.valueOf(b))
            .param("name","Only my profile").param("email","renamed@demo.test").param("schoolId",String.valueOf(schoolId))
            .param("applicationMessage","Updated").param("role","ADMIN").param("status","APPROVED").param("feedback","forged"))
            .andExpect(redirectedUrl("/teacher/dashboard"));
        assertThat(teachers.findById(a).name()).isEqualTo("Only my profile"); assertThat(teachers.findById(b).name()).isEqualTo("Demo Teacher");
        assertThat(teachers.findById(a).status()).isEqualTo(ApplicationStatus.PENDING); assertThat(teachers.findById(a).feedback()).isEmpty();
        mvc.perform(get("/teacher/dashboard").session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("renamed@demo.test")));
        login("renamed@demo.test");
    }
    @Test void csrfRequiredForRegistrationReviewAndLogout() throws Exception {
        mvc.perform(post("/register")).andExpect(status().isForbidden());
        long id=teachers.register(form("csrf@demo.test")); var admin=admin();
        mvc.perform(post("/admin/teachers/"+id+"/review").session(admin).param("status","APPROVED")).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(admin)).andExpect(status().isForbidden());
    }
    @Test void adminApprovesRejectsAndFeedbackAppearsEscapedForTeacher() throws Exception {
        long id=teachers.register(form("review@demo.test")); var admin=admin();
        mvc.perform(post("/admin/teachers/"+id+"/review").session(admin).with(csrf()).param("status","APPROVED").param("feedback","<script>alert(1)</script>"))
            .andExpect(redirectedUrl("/admin/teachers/"+id));
        var teacher=login("review@demo.test");
        mvc.perform(get("/teacher/dashboard").session(teacher)).andExpect(content().string(containsString("Approved")))
            .andExpect(content().string(containsString("&lt;script&gt;"))).andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
        mvc.perform(post("/admin/teachers/"+id+"/review").session(admin).with(csrf()).param("status","REJECTED").param("feedback","Please provide more detail."))
            .andExpect(status().is3xxRedirection());
        assertThat(teachers.findById(id).status()).isEqualTo(ApplicationStatus.REJECTED);
        mvc.perform(get("/teacher/dashboard").session(teacher)).andExpect(content().string(containsString("Rejected")));
    }
    @Test void adminCreatesEditsSearchesAndDeletesTeacherWhileSchoolIsProtected() throws Exception {
        var admin=admin();
        mvc.perform(post("/admin/teachers/new").session(admin).with(csrf()).param("name","Original Name").param("email","created@demo.test")
            .param("password","TeacherDemo2026!").param("schoolId",String.valueOf(schoolId)).param("applicationMessage","Hello"))
            .andExpect(status().is3xxRedirection());
        long id=teachers.search("created@demo.test",null).getFirst().accountId();
        mvc.perform(get("/admin/teachers/"+id).session(admin)).andExpect(status().isOk());
        mvc.perform(post("/admin/teachers/"+id).session(admin).with(csrf()).param("name","Changed Name").param("email","created@demo.test")
            .param("schoolId",String.valueOf(schoolId)).param("applicationMessage","Edited")) .andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/teachers").session(admin).param("q","changed").param("status","PENDING"))
            .andExpect(content().string(containsString("Changed Name")));
        mvc.perform(post("/admin/schools/"+schoolId+"/delete").session(admin).with(csrf()))
            .andExpect(flash().attribute("problem",containsString("used by teachers")));
        admission.enter(id,true); var session=login("created@demo.test");
        mvc.perform(post("/admin/teachers/"+id+"/delete").session(admin).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(teachers.exists(id)).isFalse(); assertThat(admission.inspect(id).state()).isEqualTo(AdmissionService.State.EXPIRED);
        mvc.perform(get("/teacher/dashboard").session(session)).andExpect(redirectedUrl("/login?deleted"));
    }
    @Test void schoolCanBeAddedEditedAndDeletedWhenUnused() throws Exception {
        var admin=admin();
        mvc.perform(post("/admin/schools").session(admin).with(csrf()).param("name","Test School").param("city","Test City"))
            .andExpect(redirectedUrl("/admin/schools"));
        long id=schools.findAll().stream().filter(s -> s.name().equals("Test School")).findFirst().orElseThrow().id();
        mvc.perform(get("/admin/schools/"+id).session(admin)).andExpect(status().isOk());
        mvc.perform(post("/admin/schools/"+id).session(admin).with(csrf()).param("name","Edited School").param("city","Jeddah"))
            .andExpect(redirectedUrl("/admin/schools"));
        assertThat(schools.findById(id).city()).isEqualTo("Jeddah");
        mvc.perform(post("/admin/schools/"+id+"/delete").session(admin).with(csrf())).andExpect(flash().attributeExists("notice"));
        assertThat(schools.findAll()).noneMatch(s -> s.id()==id);
    }
    @Test void fullAreaEnforcesQueueOnDirectUrlsAndRejectsUnadmittedProfilePosts() throws Exception {
        admission.configure(new AdmissionSettings(1,300,10)); long a=teachers.register(form("first@demo.test"));
        long b=teachers.register(form("second@demo.test")); var first=login("first@demo.test"); var second=login("second@demo.test");
        mvc.perform(get("/teacher/dashboard").session(first)).andExpect(status().isOk());
        mvc.perform(get("/teacher/profile").session(second)).andExpect(redirectedUrl("/waiting"));
        mvc.perform(get("/waiting").session(second)).andExpect(content().string(containsString("Your queue position: 1")));
        mvc.perform(post("/teacher/profile").session(second).with(csrf()).param("name","Forbidden save"))
            .andExpect(status().isConflict()).andExpect(content().string(containsString("not saved")));
        assertThat(teachers.findById(b).name()).isEqualTo("Demo Teacher");
        mvc.perform(get("/admin/admission").session(admin())).andExpect(status().isOk())
            .andExpect(content().string(containsString("Demo Teacher")));
        assertThat(admission.snapshot().active()).containsExactly(a);
        mvc.perform(post("/logout").session(first).with(csrf())).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/admission/status").session(second).param("join","true"))
            .andExpect(jsonPath("$.state").value("ACTIVE"));
        mvc.perform(get("/teacher/dashboard").session(second)).andExpect(status().isOk());
    }
    @Test void invalidSettingsDoNotSaveAndValidChangesPersist() throws Exception {
        var admin=admin();
        mvc.perform(post("/admin/admission").session(admin).with(csrf()).param("maxActive","0").param("inactivitySeconds","-1").param("queueCapacity","9999"))
            .andExpect(model().attributeHasFieldErrors("form","maxActive","inactivitySeconds","queueCapacity"));
        assertThat(settings.get()).isEqualTo(new AdmissionSettings(20,300,100));
        mvc.perform(post("/admin/admission").session(admin).with(csrf()).param("maxActive","2").param("inactivitySeconds","25").param("queueCapacity","10"))
            .andExpect(redirectedUrl("/admin/admission"));
        assertThat(settings.get()).isEqualTo(new AdmissionSettings(2,25,10));
    }
    @Test void duplicateEmailDuringEditRollsBackOtherProfileChanges() throws Exception {
        long a=teachers.register(form("original@demo.test")); teachers.register(form("occupied@demo.test"));
        var session=login("original@demo.test"); mvc.perform(get("/teacher/profile").session(session));
        mvc.perform(post("/teacher/profile").session(session).with(csrf()).param("name","Must not save")
            .param("email","OCCUPIED@DEMO.TEST").param("schoolId",String.valueOf(schoolId)).param("applicationMessage","Changed"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("already registered")));
        assertThat(teachers.findById(a).email()).isEqualTo("original@demo.test");
        assertThat(teachers.findById(a).name()).isEqualTo("Demo Teacher");
    }
    @Test void loginSchoolAndRegistrationPagesRenderWithoutAuthentication() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()); mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(get("/admin/schools").session(admin())).andExpect(status().isOk());
        mvc.perform(get("/h2-console")).andExpect(status().is3xxRedirection());
    }
    @Test void optionalDemoSeedingIsRepeatableWithoutResettingAccounts() {
        var seed=new SeedData(jdbc,encoder,teachers,true); seed.run(null);
        long id=teachers.search("amina@demo.test",null).getFirst().accountId();
        var changed=Forms.Profile.from(teachers.findById(id)); changed.setName("Preserved demo edit"); changed.setEmail("changed-demo@demo.test"); teachers.update(id,changed);
        String hash=jdbc.queryForObject("SELECT password_hash FROM user_account WHERE email='admin@demo.test'",String.class);
        seed.run(null);
        assertThat(teachers.count()).isEqualTo(2);
        assertThat(teachers.findById(id).name()).isEqualTo("Preserved demo edit");
        assertThat(jdbc.queryForObject("SELECT password_hash FROM user_account WHERE email='admin@demo.test'",String.class)).isEqualTo(hash);
    }

}
