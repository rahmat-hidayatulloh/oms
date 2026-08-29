package com.oms.auth.security;

import com.oms.auth.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;

/**
 * Adapter that wraps a {@link User} entity and implements {@link UserDetails}.
 * <p>
 * This separates the JPA entity representation from the Spring Security
 * principal interface (SOLID L-2: Liskov Substitution via Adapter).
 * The entity carries persistence concerns; this adapter carries security concerns.
 */
@Getter
public class UserDetailsAdapter implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final User user;

    public UserDetailsAdapter(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getAuthorities();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return user.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    /**
     * Returns the underlying User entity.
     * Use sparingly — prefer accessing via {@link UserDetails} interface methods.
     */
    public User getUser() {
        return user;
    }
}
