# WeChat Login Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add WeChat login to the TripMate mini-program, store user profile (openid/nickname/avatar), and build a full personal profile page at `/pages/mine/mine`.

**Architecture:** Backend adds a `WxUser` entity + `/api/wx/login` endpoint that calls the real WeChat `jscode2session` API to exchange a `code` for an `openid`, then issues a JWT with a `userType=WX_USER` claim. The existing `JwtAuthFilter` is extended to route by `userType`, looking up either `WxUser` or `AdminUser`. The mini-program gets a `useAuth` composable (module-level singleton, same pattern as `useElder`) and a fully rewritten `mine.vue` with profile display, feature menu, and elder-mode support.

**Tech Stack:** Spring Boot 4 / Java 21 / JPA / RestClient (backend); UniApp + Vue 3 Composition API + TypeScript (frontend)

**Spec:** `docs/superpowers/specs/2026-06-05-wechat-login-design.md`

---

## File Map

**Backend — new files:**
- `src/main/java/com/LHZ/TripMate/config/WxConfig.java`
- `src/main/java/com/LHZ/TripMate/entity/WxUser.java`
- `src/main/java/com/LHZ/TripMate/repository/WxUserRepository.java`
- `src/main/java/com/LHZ/TripMate/dto/wx/WxLoginRequestDTO.java`
- `src/main/java/com/LHZ/TripMate/dto/wx/WxLoginResponseDTO.java`
- `src/main/java/com/LHZ/TripMate/dto/wx/UpdateProfileRequestDTO.java`
- `src/main/java/com/LHZ/TripMate/security/WxUserDetails.java`
- `src/main/java/com/LHZ/TripMate/service/WxAuthService.java`
- `src/main/java/com/LHZ/TripMate/service/impl/WxAuthServiceImpl.java`
- `src/main/java/com/LHZ/TripMate/controller/WxAuthController.java`
- `src/test/java/com/LHZ/TripMate/controller/WxAuthControllerTest.java`

**Backend — modified files:**
- `src/main/resources/application.yaml` — add `wx.appid` / `wx.secret`
- `src/main/java/com/LHZ/TripMate/util/JwtUtil.java` — add `userType` claim
- `src/main/java/com/LHZ/TripMate/service/impl/AdminAuthServiceImpl.java` — update `generateToken` call
- `src/main/java/com/LHZ/TripMate/security/JwtAuthFilter.java` — dual-type routing
- `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java` — permit `/api/wx/login`
- `src/test/java/com/LHZ/TripMate/util/JwtUtilTest.java` — update + extend tests

**Frontend — new files:**
- `frontend/api/auth.ts`
- `frontend/composables/useAuth.ts`

**Frontend — modified files:**
- `frontend/pages/mine/mine.vue` — complete rewrite
- `frontend/App.vue` — call `loadFromStorage()` on launch

---

## Task 1: Backend config & data layer

**Files:**
- Create: `src/main/resources/application.yaml`
- Create: `src/main/java/com/LHZ/TripMate/config/WxConfig.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/WxUser.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/WxUserRepository.java`

- [ ] **Step 1: Add wx config to application.yaml**

Append to `src/main/resources/application.yaml` (after the existing `jwt:` block):

```yaml
wx:
  appid: YOUR_APP_ID
  secret: YOUR_APP_SECRET
```

> Replace `YOUR_APP_ID` and `YOUR_APP_SECRET` with your real values from the WeChat developer portal.

- [ ] **Step 2: Create WxConfig**

```java
// src/main/java/com/LHZ/TripMate/config/WxConfig.java
package com.LHZ.TripMate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wx")
public class WxConfig {
    private String appid;
    private String secret;
}
```

- [ ] **Step 3: Create WxUser entity**

```java
// src/main/java/com/LHZ/TripMate/entity/WxUser.java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "wx_user")
public class WxUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String openid;

    @Column(length = 64)
    private String nickname;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 4: Create WxUserRepository**

```java
// src/main/java/com/LHZ/TripMate/repository/WxUserRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.WxUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WxUserRepository extends JpaRepository<WxUser, Long> {
    Optional<WxUser> findByOpenid(String openid);
}
```

- [ ] **Step 5: Start backend and confirm table is auto-created**

Run: `./mvnw spring-boot:run`

Expected: no startup errors; MySQL should auto-create a `wx_user` table (JPA `ddl-auto: update`).

Stop the server with Ctrl+C.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/application.yaml \
        src/main/java/com/LHZ/TripMate/config/WxConfig.java \
        src/main/java/com/LHZ/TripMate/entity/WxUser.java \
        src/main/java/com/LHZ/TripMate/repository/WxUserRepository.java
git commit -m "feat: add WxUser entity, WxUserRepository and WxConfig"
```

---

## Task 2: DTOs

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/dto/wx/WxLoginRequestDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/wx/WxLoginResponseDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/wx/UpdateProfileRequestDTO.java`

- [ ] **Step 1: Create WxLoginRequestDTO**

```java
// src/main/java/com/LHZ/TripMate/dto/wx/WxLoginRequestDTO.java
package com.LHZ.TripMate.dto.wx;

import lombok.Data;

@Data
public class WxLoginRequestDTO {
    private String code;
}
```

- [ ] **Step 2: Create WxLoginResponseDTO**

```java
// src/main/java/com/LHZ/TripMate/dto/wx/WxLoginResponseDTO.java
package com.LHZ.TripMate.dto.wx;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WxLoginResponseDTO {
    private String token;
    private String openid;
    private String nickname;
    private String avatarUrl;
}
```

- [ ] **Step 3: Create UpdateProfileRequestDTO**

```java
// src/main/java/com/LHZ/TripMate/dto/wx/UpdateProfileRequestDTO.java
package com.LHZ.TripMate.dto.wx;

import lombok.Data;

@Data
public class UpdateProfileRequestDTO {
    private String nickname;
    private String avatarUrl;
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/dto/wx/
git commit -m "feat: add wx login DTOs"
```

---

## Task 3: WxUserDetails

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/security/WxUserDetails.java`

- [ ] **Step 1: Create WxUserDetails**

```java
// src/main/java/com/LHZ/TripMate/security/WxUserDetails.java
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
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/security/WxUserDetails.java
git commit -m "feat: add WxUserDetails for Spring Security integration"
```

---

## Task 4: JwtUtil — add userType claim

**Files:**
- Modify: `src/main/java/com/LHZ/TripMate/util/JwtUtil.java`
- Modify: `src/main/java/com/LHZ/TripMate/service/impl/AdminAuthServiceImpl.java`
- Modify: `src/test/java/com/LHZ/TripMate/util/JwtUtilTest.java`

- [ ] **Step 1: Update JwtUtilTest — update existing calls + add userType test**

Replace the full content of `src/test/java/com/LHZ/TripMate/util/JwtUtilTest.java`:

```java
package com.LHZ.TripMate.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "TestSecretKeyForJwtUtilTestTestSecretKeyForJwtUtilTest",
            86400000L
        );
    }

    @Test
    void generateToken_thenExtractUsername_returnsSame() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN", "ADMIN");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void generateToken_thenExtractRole_returnsSame() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN", "ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void generateToken_thenExtractUserType_returnsSame() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN", "ADMIN");
        assertThat(jwtUtil.extractUserType(token)).isEqualTo("ADMIN");
    }

    @Test
    void wxUserToken_extractsUserType_asWxUser() {
        String token = jwtUtil.generateToken("oXxxx123", "WX_USER", "WX_USER");
        assertThat(jwtUtil.extractUserType(token)).isEqualTo("WX_USER");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("oXxxx123");
    }

    @Test
    void validToken_isValid() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN", "ADMIN");
        assertThat(jwtUtil.isValid(token, "admin")).isTrue();
    }

    @Test
    void tokenWithWrongUsername_isNotValid() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN", "ADMIN");
        assertThat(jwtUtil.isValid(token, "other")).isFalse();
    }

    @Test
    void expiredToken_isNotValid() {
        JwtUtil expiredJwtUtil = new JwtUtil(
            "TestSecretKeyForJwtUtilTestTestSecretKeyForJwtUtilTest",
            -1000L
        );
        String token = expiredJwtUtil.generateToken("admin", "SUPER_ADMIN", "ADMIN");
        assertThat(expiredJwtUtil.isValid(token, "admin")).isFalse();
    }

    @Test
    void malformedToken_isNotValid() {
        assertThat(jwtUtil.isValid("not.a.valid.token", "admin")).isFalse();
    }

    @Test
    void nullToken_isNotValid() {
        assertThat(jwtUtil.isValid(null, "admin")).isFalse();
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail (new signature not yet implemented)**

```bash
./mvnw test -pl . -Dtest=JwtUtilTest
```

Expected: compile error or FAIL on `generateToken` calls — 3-arg method doesn't exist yet.

- [ ] **Step 3: Update JwtUtil**

Replace the full content of `src/main/java/com/LHZ/TripMate/util/JwtUtil.java`:

```java
package com.LHZ.TripMate.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject, String role, String userType) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("userType", userType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractUserType(String token) {
        return parseClaims(token).get("userType", String.class);
    }

    public boolean isValid(String token, String expectedUsername) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            return claims.getSubject().equals(expectedUsername)
                    && expiration != null
                    && !expiration.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
```

- [ ] **Step 4: Update AdminAuthServiceImpl — pass "ADMIN" as userType**

In `src/main/java/com/LHZ/TripMate/service/impl/AdminAuthServiceImpl.java`, change line 37:

Old:
```java
String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
```

New:
```java
String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), "ADMIN");
```

- [ ] **Step 5: Run JwtUtilTest to confirm all pass**

```bash
./mvnw test -pl . -Dtest=JwtUtilTest
```

Expected: `Tests run: 9, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/util/JwtUtil.java \
        src/main/java/com/LHZ/TripMate/service/impl/AdminAuthServiceImpl.java \
        src/test/java/com/LHZ/TripMate/util/JwtUtilTest.java
git commit -m "feat: add userType claim to JwtUtil; update AdminAuthServiceImpl"
```

---

## Task 5: JwtAuthFilter — dual-type routing

**Files:**
- Modify: `src/main/java/com/LHZ/TripMate/security/JwtAuthFilter.java`

- [ ] **Step 1: Replace JwtAuthFilter with dual-type implementation**

```java
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
```

- [ ] **Step 2: Build to confirm no compile errors**

```bash
./mvnw compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/security/JwtAuthFilter.java
git commit -m "feat: extend JwtAuthFilter to route by userType (ADMIN vs WX_USER)"
```

---

## Task 6: SecurityConfig — permit /api/wx/login

**Files:**
- Modify: `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java`

- [ ] **Step 1: Add /api/wx/login to permitAll**

In `SecurityConfig.java`, change:

Old:
```java
.requestMatchers("/api/admin/login").permitAll()
```

New:
```java
.requestMatchers("/api/admin/login", "/api/wx/login").permitAll()
```

- [ ] **Step 2: Build to confirm no errors**

```bash
./mvnw compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/config/SecurityConfig.java
git commit -m "feat: permit /api/wx/login in SecurityConfig"
```

---

## Task 7: WxAuthService — login + updateProfile

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/service/WxAuthService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/WxAuthServiceImpl.java`

- [ ] **Step 1: Create WxAuthService interface**

```java
// src/main/java/com/LHZ/TripMate/service/WxAuthService.java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.wx.WxLoginResponseDTO;

public interface WxAuthService {
    WxLoginResponseDTO login(String code);
    void updateProfile(String openid, String nickname, String avatarUrl);
}
```

- [ ] **Step 2: Create WxAuthServiceImpl**

```java
// src/main/java/com/LHZ/TripMate/service/impl/WxAuthServiceImpl.java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.config.WxConfig;
import com.LHZ.TripMate.dto.wx.WxLoginResponseDTO;
import com.LHZ.TripMate.entity.WxUser;
import com.LHZ.TripMate.repository.WxUserRepository;
import com.LHZ.TripMate.service.WxAuthService;
import com.LHZ.TripMate.util.JwtUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class WxAuthServiceImpl implements WxAuthService {

    private final WxConfig wxConfig;
    private final WxUserRepository wxUserRepository;
    private final JwtUtil jwtUtil;
    private final RestClient restClient;

    public WxAuthServiceImpl(WxConfig wxConfig,
                              WxUserRepository wxUserRepository,
                              JwtUtil jwtUtil) {
        this.wxConfig = wxConfig;
        this.wxUserRepository = wxUserRepository;
        this.jwtUtil = jwtUtil;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.weixin.qq.com")
                .build();
    }

    @Override
    public WxLoginResponseDTO login(String code) {
        String openid = fetchOpenid(code);

        WxUser wxUser = wxUserRepository.findByOpenid(openid)
                .orElseGet(() -> {
                    WxUser newUser = new WxUser();
                    newUser.setOpenid(openid);
                    newUser.setNickname("");
                    newUser.setAvatarUrl("");
                    return wxUserRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(openid, "WX_USER", "WX_USER");
        return new WxLoginResponseDTO(
                token,
                openid,
                wxUser.getNickname() != null ? wxUser.getNickname() : "",
                wxUser.getAvatarUrl() != null ? wxUser.getAvatarUrl() : ""
        );
    }

    @Override
    public void updateProfile(String openid, String nickname, String avatarUrl) {
        WxUser wxUser = wxUserRepository.findByOpenid(openid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        wxUser.setNickname(nickname);
        wxUser.setAvatarUrl(avatarUrl);
        wxUserRepository.save(wxUser);
    }

    private String fetchOpenid(String code) {
        Jscode2SessionResponse res = restClient.get()
                .uri(b -> b.path("/sns/jscode2session")
                           .queryParam("appid", wxConfig.getAppid())
                           .queryParam("secret", wxConfig.getSecret())
                           .queryParam("js_code", code)
                           .queryParam("grant_type", "authorization_code")
                           .build())
                .retrieve()
                .body(Jscode2SessionResponse.class);

        if (res == null || (res.getErrcode() != null && res.getErrcode() != 0)) {
            String msg = res != null ? res.getErrmsg() : "无响应";
            log.warn("微信 jscode2session 失败: {}", msg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "微信登录失败: " + msg);
        }
        return res.getOpenid();
    }

    @Data
    private static class Jscode2SessionResponse {
        private String openid;
        @JsonProperty("session_key")
        private String sessionKey;
        private Integer errcode;
        private String errmsg;
    }
}
```

- [ ] **Step 3: Build to confirm no errors**

```bash
./mvnw compile
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/service/WxAuthService.java \
        src/main/java/com/LHZ/TripMate/service/impl/WxAuthServiceImpl.java
git commit -m "feat: add WxAuthService with real jscode2session call"
```

---

## Task 8: WxAuthController + controller test

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/controller/WxAuthController.java`
- Create: `src/test/java/com/LHZ/TripMate/controller/WxAuthControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
// src/test/java/com/LHZ/TripMate/controller/WxAuthControllerTest.java
package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.dto.wx.WxLoginResponseDTO;
import com.LHZ.TripMate.service.WxAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WxAuthControllerTest {

    private MockMvc mockMvc;
    private WxAuthService wxAuthService;

    @BeforeEach
    void setUp() {
        wxAuthService = mock(WxAuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WxAuthController(wxAuthService))
                .build();
    }

    @Test
    void login_withValidCode_returnsToken() throws Exception {
        given(wxAuthService.login("test-code")).willReturn(
                new WxLoginResponseDTO("jwt-token", "openid-123", "用户昵称", "https://avatar.url"));

        mockMvc.perform(post("/api/wx/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"test-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.openid").value("openid-123"))
                .andExpect(jsonPath("$.data.nickname").value("用户昵称"));
    }
}
```

- [ ] **Step 2: Run test to confirm it fails (controller class doesn't exist)**

```bash
./mvnw test -pl . -Dtest=WxAuthControllerTest
```

Expected: compile error — `WxAuthController` not found.

- [ ] **Step 3: Create WxAuthController**

```java
// src/main/java/com/LHZ/TripMate/controller/WxAuthController.java
package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.wx.UpdateProfileRequestDTO;
import com.LHZ.TripMate.dto.wx.WxLoginRequestDTO;
import com.LHZ.TripMate.dto.wx.WxLoginResponseDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.WxAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wx")
@RequiredArgsConstructor
public class WxAuthController {

    private final WxAuthService wxAuthService;

    @PostMapping("/login")
    public Result<WxLoginResponseDTO> login(@RequestBody WxLoginRequestDTO req) {
        return Result.success(wxAuthService.login(req.getCode()));
    }

    @PostMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileRequestDTO req,
                                      @AuthenticationPrincipal WxUserDetails userDetails) {
        wxAuthService.updateProfile(userDetails.getUsername(), req.getNickname(), req.getAvatarUrl());
        return Result.success();
    }
}
```

- [ ] **Step 4: Run test to confirm it passes**

```bash
./mvnw test -pl . -Dtest=WxAuthControllerTest
```

Expected: `Tests run: 1, Failures: 0, Errors: 0`

- [ ] **Step 5: Run full test suite**

```bash
./mvnw test
```

Expected: all tests pass.

- [ ] **Step 6: Start the backend and manually test the login endpoint**

```bash
./mvnw spring-boot:run
```

In another terminal:
```bash
curl -s -X POST http://localhost:8080/api/wx/login \
  -H "Content-Type: application/json" \
  -d '{"code":"fake-code-for-testing"}'
```

Expected: `{"code":400,"message":"微信登录失败: ..."}` — this is correct; the real WeChat API rejects fake codes. The endpoint is wired correctly.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/controller/WxAuthController.java \
        src/test/java/com/LHZ/TripMate/controller/WxAuthControllerTest.java
git commit -m "feat: add WxAuthController with login and profile endpoints"
```

---

## Task 9: Frontend api/auth.ts

**Files:**
- Create: `frontend/api/auth.ts`

- [ ] **Step 1: Create auth.ts**

```typescript
// frontend/api/auth.ts
import { useApi } from '@/utils/useApi'

export interface WxLoginResponse {
  token: string
  openid: string
  nickname: string
  avatarUrl: string
}

export function wxLogin(code: string) {
  const { post } = useApi()
  return post<WxLoginResponse>('/api/wx/login', { code }, { withToken: false })
}

export function updateProfile(nickname: string, avatarUrl: string) {
  const { post } = useApi()
  return post<void>('/api/wx/profile', { nickname, avatarUrl })
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/api/auth.ts
git commit -m "feat: add frontend wx auth API wrappers"
```

---

## Task 10: Frontend composables/useAuth.ts

**Files:**
- Create: `frontend/composables/useAuth.ts`

- [ ] **Step 1: Create useAuth composable**

```typescript
// frontend/composables/useAuth.ts
import { reactive } from 'vue'
import { wxLogin, updateProfile } from '@/api/auth'

const USER_INFO_KEY = 'userInfo'

interface UserInfo {
  openid: string
  nickname: string
  avatarUrl: string
}

// Module-level singleton — all components share this state
const authState = reactive({
  isLoggedIn: false,
  userInfo: null as UserInfo | null
})

export function useAuth() {

  function loadFromStorage() {
    const token = uni.getStorageSync('token')
    const raw = uni.getStorageSync(USER_INFO_KEY)
    if (token && raw) {
      try {
        authState.userInfo = JSON.parse(raw) as UserInfo
        authState.isLoggedIn = true
      } catch {
        logout()
      }
    }
  }

  async function login(): Promise<void> {
    return new Promise((resolve, reject) => {
      uni.login({
        provider: 'weixin',
        success: async (loginRes) => {
          try {
            const res = await wxLogin(loginRes.code)
            if (res.code === 200) {
              const { token, openid, nickname, avatarUrl } = res.data
              uni.setStorageSync('token', token)
              const info: UserInfo = { openid, nickname, avatarUrl }
              uni.setStorageSync(USER_INFO_KEY, JSON.stringify(info))
              authState.userInfo = info
              authState.isLoggedIn = true
              resolve()
            } else {
              reject(new Error(res.message))
            }
          } catch (e) {
            reject(e)
          }
        },
        fail: (err) => reject(err)
      })
    })
  }

  function logout() {
    uni.removeStorageSync('token')
    uni.removeStorageSync(USER_INFO_KEY)
    authState.isLoggedIn = false
    authState.userInfo = null
  }

  async function saveProfile(nickname: string, avatarUrl: string): Promise<void> {
    await updateProfile(nickname, avatarUrl)
    if (authState.userInfo) {
      authState.userInfo.nickname = nickname
      authState.userInfo.avatarUrl = avatarUrl
      uni.setStorageSync(USER_INFO_KEY, JSON.stringify(authState.userInfo))
    }
  }

  return { authState, login, logout, loadFromStorage, saveProfile }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/composables/useAuth.ts
git commit -m "feat: add useAuth composable with login/logout/saveProfile"
```

---

## Task 11: mine.vue — full profile page

**Files:**
- Modify: `frontend/pages/mine/mine.vue`

- [ ] **Step 1: Rewrite mine.vue**

```vue
<template>
  <view class="page">

    <!-- 未登录 -->
    <view v-if="!authState.isLoggedIn" class="login-card">
      <view class="avatar-circle">
        <text class="avatar-icon">👤</text>
      </view>
      <text class="unlogged-tip" :style="{ fontSize: rpx(28) }">未登录</text>
      <button class="login-btn" :style="{ fontSize: rpx(30) }" @click="handleLogin">
        一键微信登录
      </button>
    </view>

    <!-- 已登录 -->
    <view v-else>

      <!-- 个人信息头部 -->
      <view class="profile-header">
        <button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
          <image
            v-if="authState.userInfo?.avatarUrl"
            class="avatar"
            :src="authState.userInfo.avatarUrl"
            mode="aspectFill"
          />
          <view v-else class="avatar-circle">
            <text class="avatar-icon">👤</text>
          </view>
        </button>
        <view class="profile-info">
          <input
            class="nickname-input"
            type="nickname"
            :value="authState.userInfo?.nickname || '微信用户'"
            :style="{ fontSize: rpx(34) }"
            placeholder="点击修改昵称"
            @blur="onNicknameBlur"
          />
          <text class="openid-text" :style="{ fontSize: rpx(22) }">
            ID: {{ authState.userInfo?.openid?.slice(0, 12) }}...
          </text>
        </view>
      </view>

      <!-- 功能菜单 -->
      <view class="menu-group">
        <view class="menu-item" @click="onCollect">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">我的收藏</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="divider" />
        <view class="menu-item" @click="onLanguage">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">语言设置</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="divider" />
        <view class="menu-item" @click="onElder">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">长辈模式</text>
          <text class="menu-arrow">›</text>
        </view>
        <view class="divider" />
        <view class="menu-item" @click="onAbout">
          <text class="menu-label" :style="{ fontSize: rpx(28) }">关于 TripMate</text>
          <text class="menu-arrow">›</text>
        </view>
      </view>

      <!-- 退出登录 -->
      <button class="logout-btn" :style="{ fontSize: rpx(28) }" @click="handleLogout">
        退出登录
      </button>

    </view>

    <view class="tabbar-placeholder" />
    <TabBar active="mine" />
  </view>
</template>

<script setup lang="ts">
import { useAuth } from '@/composables/useAuth'
import { useElder } from '@/composables/useElder'
import TabBar from '@/components/TabBar/TabBar.vue'

const { authState, login, logout, saveProfile } = useAuth()
const { rpx } = useElder()

async function handleLogin() {
  try {
    await login()
    uni.showToast({ title: '登录成功', icon: 'success' })
  } catch {
    uni.showToast({ title: '登录失败，请重试', icon: 'none' })
  }
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出登录吗？',
    success: (res) => {
      if (res.confirm) {
        logout()
        uni.showToast({ title: '已退出登录', icon: 'none' })
      }
    }
  })
}

async function onChooseAvatar(e: any) {
  const newAvatarUrl: string = e.detail.avatarUrl
  if (!newAvatarUrl) return
  try {
    await saveProfile(authState.userInfo?.nickname || '', newAvatarUrl)
    uni.showToast({ title: '头像已更新', icon: 'success' })
  } catch {
    uni.showToast({ title: '更新失败', icon: 'none' })
  }
}

async function onNicknameBlur(e: any) {
  const newNickname: string = e.detail.value?.trim()
  if (!newNickname || newNickname === authState.userInfo?.nickname) return
  try {
    await saveProfile(newNickname, authState.userInfo?.avatarUrl || '')
    uni.showToast({ title: '昵称已更新', icon: 'success' })
  } catch {
    uni.showToast({ title: '更新失败', icon: 'none' })
  }
}

function onCollect() {
  uni.showToast({ title: '敬请期待', icon: 'none' })
}

function onLanguage() {
  uni.navigateTo({ url: '/pages/language/language' })
}

function onElder() {
  uni.navigateTo({ url: '/pages/elder/elder' })
}

function onAbout() {
  uni.showModal({
    title: '关于 TripMate',
    content: 'TripMate 智能旅行助手\n版本 1.0.0\n\n让旅行更简单、更美好。',
    showCancel: false
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background-color: #f7f8fa;
}

/* 未登录 */
.login-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 32rpx 0;
}

.unlogged-tip {
  margin-top: 24rpx;
  color: #999;
}

.login-btn {
  margin-top: 48rpx;
  width: 480rpx;
  background-color: #07c160;
  color: #fff;
  border-radius: 48rpx;
  border: none;
}

/* 头像通用 */
.avatar-circle {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background-color: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-icon {
  font-size: 60rpx;
}

/* 已登录 — 个人信息头部 */
.profile-header {
  display: flex;
  align-items: center;
  background-color: #fff;
  padding: 48rpx 32rpx;
  margin-bottom: 24rpx;
}

.avatar-btn {
  padding: 0;
  margin: 0;
  background: none;
  border: none;
  line-height: 1;
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-btn::after {
  border: none;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
}

.profile-info {
  margin-left: 24rpx;
  display: flex;
  flex-direction: column;
}

.nickname-input {
  color: #1a1a1a;
  font-weight: 600;
  margin-bottom: 8rpx;
}

.openid-text {
  color: #bbb;
}

/* 功能菜单 */
.menu-group {
  background-color: #fff;
  border-radius: 16rpx;
  margin: 0 24rpx 24rpx;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 36rpx 32rpx;
}

.menu-label {
  color: #1a1a1a;
}

.menu-arrow {
  font-size: 40rpx;
  color: #ccc;
}

.divider {
  height: 1rpx;
  background-color: #f0f0f0;
  margin: 0 32rpx;
}

/* 退出登录 */
.logout-btn {
  margin: 0 24rpx;
  background-color: #fff;
  color: #f56c6c;
  border-radius: 16rpx;
  border: none;
}

.logout-btn::after {
  border: none;
}

/* TabBar 占位 */
.tabbar-placeholder {
  height: 140rpx;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/pages/mine/mine.vue
git commit -m "feat: rewrite mine.vue with WeChat login and profile page"
```

---

## Task 12: App.vue — load auth state on launch

**Files:**
- Modify: `frontend/App.vue`

- [ ] **Step 1: Update App.vue to restore login state on launch**

```vue
<script>
import { useAuth } from '@/composables/useAuth'

export default {
  onLaunch: function() {
    const { loadFromStorage } = useAuth()
    loadFromStorage()
  },
  onShow: function() {},
  onHide: function() {}
}
</script>

<style>
  /*每个页面公共css */
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/App.vue
git commit -m "feat: restore auth state from storage on app launch"
```

---

## Verification

1. **Fill in real credentials** in `application.yaml` — replace `YOUR_APP_ID` and `YOUR_APP_SECRET`.
2. **Start backend**: `./mvnw spring-boot:run`
3. **Open project in HBuilderX** → run to WeChat DevTools (mini-program simulator).
4. **Navigate to「我的」tab** → should show the login card.
5. **Tap「一键微信登录」** → WeChat calls `uni.login()`, code is sent to `/api/wx/login`, JWT is returned, profile page appears.
6. **Tap the avatar** → WeChat shows avatar picker; selecting one updates it immediately.
7. **Tap the nickname input** → WeChat shows nickname input; confirming updates it.
8. **Kill and reopen the mini-program** → profile page still shows (state restored from storage).
9. **Tap「退出登录」** → confirm dialog → returns to login card.
10. **Run backend tests**: `./mvnw test` → all pass.
