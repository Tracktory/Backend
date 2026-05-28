package com.hansung.tracktory.domain.user.service;

import com.hansung.tracktory.domain.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    @Getter
    private final Long userId;
    private final String email;
    private final String passwordHash;

    public UserPrincipal(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
    }

    private UserPrincipal(Long userId, String email) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = null;
    }

    public static UserPrincipal of(Long userId, String email) {
        return new UserPrincipal(userId, email);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
