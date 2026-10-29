package edu.se201.tracker;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(JdbcTemplate jdbc) {
        return email -> jdbc.query("SELECT * FROM user_account WHERE email=?", (r,n) ->
            new AccountPrincipal(r.getLong("id"),r.getString("email"),r.getString("password_hash"),Role.valueOf(r.getString("role"))),
            TeacherService.normalizeEmail(email)).stream().findFirst()
            .orElseThrow(() -> new UsernameNotFoundException("Invalid login"));
    }
    @Bean SecurityFilterChain security(HttpSecurity http, AdmissionService admission) throws Exception {
        return http.authorizeHttpRequests(a -> a
            .requestMatchers("/","/login","/register","/css/**","/js/**","/error").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/teacher/**","/waiting","/admission/**").hasRole("TEACHER")
            .anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").usernameParameter("email").defaultSuccessUrl("/home",true).permitAll())
            .logout(l -> l.addLogoutHandler((req,res,auth) -> {
                if (auth!=null && auth.getPrincipal() instanceof AccountPrincipal p) admission.release(p.id());
            }).logoutSuccessUrl("/login?logout").permitAll())
            .build();
    }
}
