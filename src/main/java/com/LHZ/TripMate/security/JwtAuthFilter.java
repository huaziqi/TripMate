package com.LHZ.TripMate.security;

import com.LHZ.TripMate.repository.AdminUserRepository;
import com.LHZ.TripMate.repository.WxUserRepository;
import com.LHZ.TripMate.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AdminUserRepository adminUserRepository;
    private final WxUserRepository wxUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        String subject = jwtUtil.extractUsername(token);
        String userType = jwtUtil.extractUserType(token);

        if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if ("WX_USER".equals(userType)) {
                wxUserRepository.findByOpenid(subject).ifPresent(wxUser -> {
                    if (jwtUtil.isValid(token, subject)) {
                        var details = new WxUserDetails(wxUser);
                        var auth = new UsernamePasswordAuthenticationToken(
                                details, null, details.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                });
            } else {
                adminUserRepository.findByUsername(subject).ifPresent(user -> {
                    if (jwtUtil.isValid(token, subject) && user.getStatus() == 1) {
                        var details = new AdminUserDetails(user);
                        var auth = new UsernamePasswordAuthenticationToken(
                                details, null, details.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                });
            }
        }
        chain.doFilter(request, response);
    }
}
