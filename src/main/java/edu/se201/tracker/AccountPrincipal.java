package edu.se201.tracker;


import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// Authentication stores the stable account ID, so editing email does not change ownership.
public record AccountPrincipal(long id, String email, String passwordHash, Role role) implements UserDetails {
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public String toString() { return "AccountPrincipal[id=" + id + ", role=" + role + "]"; }
}
