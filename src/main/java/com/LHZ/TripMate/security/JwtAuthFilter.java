package com.LHZ.TripMate.security;

import com.LHZ.TripMate.repository.AdminUserRepository;
import com.LHZ.TripMate.repository.WxUserRepository;
import com.LHZ.TripMate.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j // 新增日志注解
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AdminUserRepository adminUserRepository;
    private final WxUserRepository wxUserRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // 极简日志，只打印核心信息
        log.info("接口:{}, token头:{}", request.getRequestURI(), header);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        String subject;
        String userType;
        try {
            subject = jwtUtil.extractUsername(token);
            userType = jwtUtil.extractUserType(token);
            log.info("解析token：用户:{}, 类型:{}", subject, userType);
        } catch (Exception e) {
            log.warn("token解析失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"token无效或已过期\"}");
            return;
        }

        if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if ("WX_USER".equals(userType)) {
                wxUserRepository.findByOpenid(subject).ifPresent(wxUser -> {
                    if (jwtUtil.isValid(token, subject)) {
                        var details = new WxUserDetails(wxUser);
                        var auth = new UsernamePasswordAuthenticationToken(
                                details, null, details.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.info("认证成功，权限:{}", details.getAuthorities());
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
