package com.LHZ.TripMate.security;

import com.LHZ.TripMate.entity.WxUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class WxUserDetails implements UserDetails {

    private final WxUser wxUser;

    public WxUser getWxUser() {
        return wxUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_WX_USER"));
    }

    @Override
    public String getPassword() {
        return null;
    }

    /** Returns openid — used as the JWT subject and the Security principal name. */
    @Override
    public String getUsername() {
        return wxUser.getOpenid();
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
