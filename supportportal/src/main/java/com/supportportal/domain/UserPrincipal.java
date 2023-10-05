package com.supportportal.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;


public class UserPrincipal implements UserDetails {

    private AppUser appUser;

    public UserPrincipal(AppUser appUser) {
        this.appUser = appUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return stream(this.appUser.getAuthorities()).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return this.appUser.getPassword();
    }

    @Override
    public String getUsername() {
        return this.appUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.appUser.isNotLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.appUser.isActive();
    }
}
