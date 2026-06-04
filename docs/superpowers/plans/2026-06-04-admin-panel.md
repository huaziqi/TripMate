# TripMate Admin Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Vue 3 web admin panel (port 5173) with JWT-protected Spring Boot backend extensions, supporting login, dashboard, admin user management, and system config.

**Architecture:** Phase 1 extends the existing Spring Boot app with MySQL + Spring Security + JWT under `/api/admin/**`. Phase 2 creates an independent Vite project in `admin/` that proxies to port 8080. The two phases are sequential — complete Phase 1 before starting Phase 2.

**Tech Stack:** Spring Boot 4 + Spring Security 6 + jjwt 0.12 + Spring Data JPA + MySQL (backend); Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router 4 + Axios (frontend)

---

## File Map

### Backend — new files
| File | Responsibility |
|------|---------------|
| `src/main/java/.../entity/AdminUser.java` | JPA entity for admin_user table |
| `src/main/java/.../entity/SystemConfig.java` | JPA entity for system_config table |
| `src/main/java/.../repository/AdminUserRepository.java` | Spring Data repository for AdminUser |
| `src/main/java/.../repository/SystemConfigRepository.java` | Spring Data repository for SystemConfig |
| `src/main/java/.../dto/admin/LoginRequestDTO.java` | Login request body |
| `src/main/java/.../dto/admin/LoginResponseDTO.java` | Login response (token + role) |
| `src/main/java/.../dto/admin/AdminUserDTO.java` | Admin user read DTO |
| `src/main/java/.../dto/admin/CreateAdminUserDTO.java` | Create admin user request body |
| `src/main/java/.../dto/admin/UpdateAdminUserDTO.java` | Update admin user request body |
| `src/main/java/.../dto/admin/DashboardDTO.java` | Dashboard statistics |
| `src/main/java/.../util/JwtUtil.java` | JWT generation + validation |
| `src/main/java/.../security/AdminUserDetails.java` | UserDetails adapter for AdminUser |
| `src/main/java/.../security/JwtAuthFilter.java` | OncePerRequestFilter that reads Bearer token |
| `src/main/java/.../config/SecurityConfig.java` | Spring Security filter chain + CORS |
| `src/main/java/.../service/AdminAuthService.java` | Login service interface |
| `src/main/java/.../service/AdminUserService.java` | Admin user CRUD interface |
| `src/main/java/.../service/SystemConfigService.java` | Config read/write interface |
| `src/main/java/.../service/DashboardService.java` | Stats aggregation interface |
| `src/main/java/.../service/impl/AdminAuthServiceImpl.java` | Login implementation |
| `src/main/java/.../service/impl/AdminUserServiceImpl.java` | User CRUD implementation |
| `src/main/java/.../service/impl/SystemConfigServiceImpl.java` | Config implementation |
| `src/main/java/.../service/impl/DashboardServiceImpl.java` | Stats implementation |
| `src/main/java/.../controller/admin/AdminAuthController.java` | POST /api/admin/login |
| `src/main/java/.../controller/admin/AdminUserController.java` | CRUD /api/admin/users |
| `src/main/java/.../controller/admin/SystemConfigController.java` | GET/PUT /api/admin/settings |
| `src/main/java/.../controller/admin/DashboardController.java` | GET /api/admin/dashboard |
| `src/main/resources/init-admin.sql` | One-time SQL to insert first super admin |
| `src/test/java/.../util/JwtUtilTest.java` | JWT unit tests |
| `src/test/java/.../controller/admin/AdminAuthControllerTest.java` | Login endpoint tests |

### Backend — modified files
| File | Change |
|------|--------|
| `pom.xml` | Add JPA, MySQL, Security, jjwt dependencies |
| `src/main/resources/application.yaml` | Add datasource, JPA, JWT config sections |

### Frontend — new files (all under `admin/`)
| File | Responsibility |
|------|---------------|
| `admin/package.json` | Dependencies |
| `admin/vite.config.ts` | Vite config with proxy to :8080 |
| `admin/tsconfig.json` | TypeScript config |
| `admin/index.html` | Entry HTML |
| `admin/src/main.ts` | App bootstrap |
| `admin/src/App.vue` | Root component with `<router-view>` |
| `admin/src/router/index.ts` | Routes + navigation guard |
| `admin/src/stores/auth.ts` | Pinia: token, username, role |
| `admin/src/api/http.ts` | Axios instance with token interceptor |
| `admin/src/api/auth.ts` | login() API call |
| `admin/src/api/dashboard.ts` | getDashboard() API call |
| `admin/src/api/users.ts` | listUsers / createUser / updateUser / deleteUser |
| `admin/src/api/settings.ts` | listSettings / updateSetting |
| `admin/src/components/AppLayout.vue` | Sidebar + topbar shell |
| `admin/src/views/login/LoginView.vue` | Login form |
| `admin/src/views/dashboard/DashboardView.vue` | Stats cards |
| `admin/src/views/users/UsersView.vue` | Admin users table + dialogs |
| `admin/src/views/settings/SettingsView.vue` | Config key-value editor |

---

## PHASE 1 — Backend

### Task 1: Add Maven dependencies

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add dependencies inside `<dependencies>`**

```xml
<!-- JPA + MySQL -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

- [ ] **Step 2: Verify dependencies download**

```bash
./mvnw dependency:resolve -q
```
Expected: BUILD SUCCESS, no errors.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add JPA, MySQL, Security, jjwt dependencies"
```

---

### Task 2: Configure application.yaml

**Files:**
- Modify: `src/main/resources/application.yaml`

- [ ] **Step 1: Add datasource, JPA, and JWT config**

Replace the entire file with:

```yaml
spring:
  application:
    name: TripMate
  datasource:
    url: jdbc:mysql://YOUR_CLOUD_SERVER_IP:3306/tripmate?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: YOUR_DB_USER
    password: YOUR_DB_PASSWORD
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

weather:
  api:
    path: https://qk5ctumxuw.re.qweatherapi.com
    key: 785df59a4de346d8afedd974a573dc8f

jwt:
  secret: TripMateAdminSecretKey2026TripMateAdminSecretKey2026
  expiration-ms: 86400000
```

> Replace `YOUR_CLOUD_SERVER_IP`, `YOUR_DB_USER`, `YOUR_DB_PASSWORD` with actual values.  
> The database `tripmate` must be created on the MySQL server first: `CREATE DATABASE tripmate CHARACTER SET utf8mb4;`

- [ ] **Step 2: Verify app starts (MySQL must be running)**

```bash
./mvnw spring-boot:run
```
Expected: Application starts on port 8080 without errors. Hibernate will create tables automatically on first run.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yaml
git commit -m "config: add MySQL datasource and JWT settings"
```

---

### Task 3: AdminUser entity + repository

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/entity/AdminUser.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/AdminUserRepository.java`

- [ ] **Step 1: Create `AdminUser.java`**

```java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "admin_user")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Integer status = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public enum Role {
        SUPER_ADMIN, ADMIN
    }
}
```

- [ ] **Step 2: Create `AdminUserRepository.java`**

```java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

- [ ] **Step 3: Restart app to verify table is created**

```bash
./mvnw spring-boot:run
```
Then on MySQL: `SHOW TABLES;` — you should see `admin_user`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/entity/AdminUser.java \
        src/main/java/com/LHZ/TripMate/repository/AdminUserRepository.java
git commit -m "feat: add AdminUser entity and repository"
```

---

### Task 4: SystemConfig entity + repository

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/entity/SystemConfig.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/SystemConfigRepository.java`

- [ ] **Step 1: Create `SystemConfig.java`**

```java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 128)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(length = 255)
    private String description;
}
```

- [ ] **Step 2: Create `SystemConfigRepository.java`**

```java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/entity/SystemConfig.java \
        src/main/java/com/LHZ/TripMate/repository/SystemConfigRepository.java
git commit -m "feat: add SystemConfig entity and repository"
```

---

### Task 5: JWT utility

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/util/JwtUtil.java`
- Create: `src/test/java/com/LHZ/TripMate/util/JwtUtilTest.java`

- [ ] **Step 1: Write the failing test**

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
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void generateToken_thenExtractRole_returnsSame() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void validToken_isValid() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN");
        assertThat(jwtUtil.isValid(token, "admin")).isTrue();
    }

    @Test
    void tokenWithWrongUsername_isNotValid() {
        String token = jwtUtil.generateToken("admin", "SUPER_ADMIN");
        assertThat(jwtUtil.isValid(token, "other")).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=JwtUtilTest
```
Expected: FAIL — `JwtUtil` not found.

- [ ] **Step 3: Create `JwtUtil.java`**

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

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
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

    public boolean isValid(String token, String expectedUsername) {
        try {
            String username = extractUsername(token);
            return username.equals(expectedUsername)
                    && !parseClaims(token).getExpiration().before(new Date());
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

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw test -Dtest=JwtUtilTest
```
Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/util/JwtUtil.java \
        src/test/java/com/LHZ/TripMate/util/JwtUtilTest.java
git commit -m "feat: add JwtUtil with token generation and validation"
```

---

### Task 6: Spring Security config + JWT filter

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/security/AdminUserDetails.java`
- Create: `src/main/java/com/LHZ/TripMate/security/JwtAuthFilter.java`
- Create: `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java`

- [ ] **Step 1: Create `AdminUserDetails.java`**

```java
package com.LHZ.TripMate.security;

import com.LHZ.TripMate.entity.AdminUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class AdminUserDetails implements UserDetails {

    private final AdminUser adminUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + adminUser.getRole().name()));
    }

    @Override public String getPassword() { return adminUser.getPassword(); }
    @Override public String getUsername() { return adminUser.getUsername(); }
    @Override public boolean isEnabled() { return adminUser.getStatus() == 1; }
}
```

- [ ] **Step 2: Create `JwtAuthFilter.java`**

```java
package com.LHZ.TripMate.security;

import com.LHZ.TripMate.repository.AdminUserRepository;
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
        String username = jwtUtil.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            adminUserRepository.findByUsername(username).ifPresent(user -> {
                if (jwtUtil.isValid(token, username) && user.getStatus() == 1) {
                    var details = new AdminUserDetails(user);
                    var auth = new UsernamePasswordAuthenticationToken(
                            details, null, details.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            });
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Create `SecurityConfig.java`**

```java
package com.LHZ.TripMate.config;

import com.LHZ.TripMate.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/login").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 4: Restart app and verify it starts**

```bash
./mvnw spring-boot:run
```
Expected: Starts on port 8080. Now `GET /api/weather` requires a token (will return 403).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/security/ \
        src/main/java/com/LHZ/TripMate/config/SecurityConfig.java
git commit -m "feat: configure Spring Security with JWT stateless auth"
```

---

### Task 7: Login endpoint

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/dto/admin/LoginRequestDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/admin/LoginResponseDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/service/AdminAuthService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/AdminAuthServiceImpl.java`
- Create: `src/main/java/com/LHZ/TripMate/controller/admin/AdminAuthController.java`
- Create: `src/test/java/com/LHZ/TripMate/controller/admin/AdminAuthControllerTest.java`

- [ ] **Step 1: Create DTOs**

```java
// LoginRequestDTO.java
package com.LHZ.TripMate.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @NotBlank private String username;
    @NotBlank private String password;
}
```

```java
// LoginResponseDTO.java
package com.LHZ.TripMate.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String role;
    private String username;
}
```

- [ ] **Step 2: Create service interface**

```java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.admin.LoginRequestDTO;
import com.LHZ.TripMate.dto.admin.LoginResponseDTO;

public interface AdminAuthService {
    LoginResponseDTO login(LoginRequestDTO request);
}
```

- [ ] **Step 3: Create service implementation**

```java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.common.ResultCode;
import com.LHZ.TripMate.dto.admin.LoginRequestDTO;
import com.LHZ.TripMate.dto.admin.LoginResponseDTO;
import com.LHZ.TripMate.entity.AdminUser;
import com.LHZ.TripMate.repository.AdminUserRepository;
import com.LHZ.TripMate.service.AdminAuthService;
import com.LHZ.TripMate.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        AdminUser user = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ResultCode.USER_NOT_FOUND.getMessage()));

        if (user.getStatus() != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ResultCode.PASSWORD_ERROR.getMessage());
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new LoginResponseDTO(token, user.getRole().name(), user.getUsername());
    }
}
```

- [ ] **Step 4: Create controller**

```java
package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.LoginRequestDTO;
import com.LHZ.TripMate.dto.admin.LoginResponseDTO;
import com.LHZ.TripMate.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return Result.success(adminAuthService.login(request));
    }
}
```

- [ ] **Step 5: Create initial super admin SQL and run it**

Create `src/main/resources/init-admin.sql`:

```sql
-- Run this once on your MySQL server to create the first super admin.
-- Password is: admin123  (change after first login!)
INSERT INTO admin_user (username, password, role, status)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SUPER_ADMIN', 1);
```

> To generate a different password hash, run in a temporary Spring Boot test:
> `new BCryptPasswordEncoder().encode("yourpassword")`

Run the script on MySQL:
```bash
mysql -u YOUR_DB_USER -p tripmate < src/main/resources/init-admin.sql
```

- [ ] **Step 6: Test login endpoint manually**

Start the app: `./mvnw spring-boot:run`

```bash
curl -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Expected:
```json
{"code":200,"message":"操作成功","data":{"token":"eyJ...","role":"SUPER_ADMIN","username":"admin"}}
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/dto/admin/ \
        src/main/java/com/LHZ/TripMate/service/AdminAuthService.java \
        src/main/java/com/LHZ/TripMate/service/impl/AdminAuthServiceImpl.java \
        src/main/java/com/LHZ/TripMate/controller/admin/AdminAuthController.java \
        src/main/resources/init-admin.sql
git commit -m "feat: add admin login endpoint with JWT response"
```

---

### Task 8: Admin user management endpoint

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/dto/admin/AdminUserDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/admin/CreateAdminUserDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/admin/UpdateAdminUserDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/service/AdminUserService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/AdminUserServiceImpl.java`
- Create: `src/main/java/com/LHZ/TripMate/controller/admin/AdminUserController.java`

- [ ] **Step 1: Create DTOs**

```java
// AdminUserDTO.java
package com.LHZ.TripMate.dto.admin;

import com.LHZ.TripMate.entity.AdminUser;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminUserDTO {
    private Long id;
    private String username;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;

    public static AdminUserDTO from(AdminUser u) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setRole(u.getRole().name());
        dto.setStatus(u.getStatus());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }
}
```

```java
// CreateAdminUserDTO.java
package com.LHZ.TripMate.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateAdminUserDTO {
    @NotBlank private String username;
    @NotBlank private String password;
    @Pattern(regexp = "SUPER_ADMIN|ADMIN") private String role;
}
```

```java
// UpdateAdminUserDTO.java
package com.LHZ.TripMate.dto.admin;

import lombok.Data;

@Data
public class UpdateAdminUserDTO {
    private Integer status;    // 0=禁用, 1=启用
    private String password;   // 非空时重置密码
}
```

- [ ] **Step 2: Create service interface**

```java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.admin.AdminUserDTO;
import com.LHZ.TripMate.dto.admin.CreateAdminUserDTO;
import com.LHZ.TripMate.dto.admin.UpdateAdminUserDTO;
import java.util.List;

public interface AdminUserService {
    List<AdminUserDTO> listAll();
    AdminUserDTO create(CreateAdminUserDTO dto);
    AdminUserDTO update(Long id, UpdateAdminUserDTO dto);
    void delete(Long id);
}
```

- [ ] **Step 3: Create service implementation**

```java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.admin.AdminUserDTO;
import com.LHZ.TripMate.dto.admin.CreateAdminUserDTO;
import com.LHZ.TripMate.dto.admin.UpdateAdminUserDTO;
import com.LHZ.TripMate.entity.AdminUser;
import com.LHZ.TripMate.repository.AdminUserRepository;
import com.LHZ.TripMate.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<AdminUserDTO> listAll() {
        return adminUserRepository.findAll().stream().map(AdminUserDTO::from).toList();
    }

    @Override
    public AdminUserDTO create(CreateAdminUserDTO dto) {
        if (adminUserRepository.existsByUsername(dto.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        AdminUser user = new AdminUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(AdminUser.Role.valueOf(dto.getRole()));
        return AdminUserDTO.from(adminUserRepository.save(user));
    }

    @Override
    public AdminUserDTO update(Long id, UpdateAdminUserDTO dto) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "管理员不存在"));
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return AdminUserDTO.from(adminUserRepository.save(user));
    }

    @Override
    public void delete(Long id) {
        if (!adminUserRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "管理员不存在");
        }
        adminUserRepository.deleteById(id);
    }
}
```

- [ ] **Step 4: Create controller**

```java
package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.AdminUserDTO;
import com.LHZ.TripMate.dto.admin.CreateAdminUserDTO;
import com.LHZ.TripMate.dto.admin.UpdateAdminUserDTO;
import com.LHZ.TripMate.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Result<List<AdminUserDTO>> list() {
        return Result.success(adminUserService.listAll());
    }

    @PostMapping
    public Result<AdminUserDTO> create(@Valid @RequestBody CreateAdminUserDTO dto) {
        return Result.success(adminUserService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<AdminUserDTO> update(@PathVariable Long id,
                                       @RequestBody UpdateAdminUserDTO dto) {
        return Result.success(adminUserService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return Result.success();
    }
}
```

- [ ] **Step 5: Test with curl (use token from Task 7 Step 6)**

```bash
# Replace TOKEN with actual token from login
curl http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer TOKEN"
```

Expected: `{"code":200,"data":[{"id":1,"username":"admin","role":"SUPER_ADMIN",...}]}`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/dto/admin/ \
        src/main/java/com/LHZ/TripMate/service/AdminUserService.java \
        src/main/java/com/LHZ/TripMate/service/impl/AdminUserServiceImpl.java \
        src/main/java/com/LHZ/TripMate/controller/admin/AdminUserController.java
git commit -m "feat: add admin user management CRUD endpoints"
```

---

### Task 9: System config endpoint

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/service/SystemConfigService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/SystemConfigServiceImpl.java`
- Create: `src/main/java/com/LHZ/TripMate/controller/admin/SystemConfigController.java`

- [ ] **Step 1: Create service interface**

```java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.entity.SystemConfig;
import java.util.List;

public interface SystemConfigService {
    List<SystemConfig> listAll();
    SystemConfig update(String key, String value);
}
```

- [ ] **Step 2: Create service implementation**

```java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.entity.SystemConfig;
import com.LHZ.TripMate.repository.SystemConfigRepository;
import com.LHZ.TripMate.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    @Override
    public List<SystemConfig> listAll() {
        return systemConfigRepository.findAll();
    }

    @Override
    public SystemConfig update(String key, String value) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "配置项不存在: " + key));
        config.setConfigValue(value);
        return systemConfigRepository.save(config);
    }
}
```

- [ ] **Step 3: Create controller**

```java
package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.entity.SystemConfig;
import com.LHZ.TripMate.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public Result<List<SystemConfig>> list() {
        return Result.success(systemConfigService.listAll());
    }

    @PutMapping("/{key}")
    public Result<SystemConfig> update(@PathVariable String key,
                                       @RequestBody Map<String, String> body) {
        return Result.success(systemConfigService.update(key, body.get("value")));
    }
}
```

- [ ] **Step 4: Insert a sample config row to test**

```sql
INSERT INTO system_config (config_key, config_value, description)
VALUES ('weather.api.key', '785df59a4de346d8afedd974a573dc8f', '和风天气 API Key');
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/service/SystemConfigService.java \
        src/main/java/com/LHZ/TripMate/service/impl/SystemConfigServiceImpl.java \
        src/main/java/com/LHZ/TripMate/controller/admin/SystemConfigController.java
git commit -m "feat: add system config read/write endpoints"
```

---

### Task 10: Dashboard endpoint

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/dto/admin/DashboardDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/service/DashboardService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/DashboardServiceImpl.java`
- Create: `src/main/java/com/LHZ/TripMate/controller/admin/DashboardController.java`

- [ ] **Step 1: Create `DashboardDTO.java`**

```java
package com.LHZ.TripMate.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardDTO {
    private long adminUserCount;
    private long configCount;
}
```

- [ ] **Step 2: Create service interface**

```java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.admin.DashboardDTO;

public interface DashboardService {
    DashboardDTO getStats();
}
```

- [ ] **Step 3: Create service implementation**

```java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.admin.DashboardDTO;
import com.LHZ.TripMate.repository.AdminUserRepository;
import com.LHZ.TripMate.repository.SystemConfigRepository;
import com.LHZ.TripMate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AdminUserRepository adminUserRepository;
    private final SystemConfigRepository systemConfigRepository;

    @Override
    public DashboardDTO getStats() {
        return new DashboardDTO(
                adminUserRepository.count(),
                systemConfigRepository.count()
        );
    }
}
```

- [ ] **Step 4: Create controller**

```java
package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.admin.DashboardDTO;
import com.LHZ.TripMate.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardDTO> stats() {
        return Result.success(dashboardService.getStats());
    }
}
```

- [ ] **Step 5: Test with curl**

```bash
curl http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer TOKEN"
```

Expected: `{"code":200,"data":{"adminUserCount":1,"configCount":1}}`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/LHZ/TripMate/dto/admin/DashboardDTO.java \
        src/main/java/com/LHZ/TripMate/service/DashboardService.java \
        src/main/java/com/LHZ/TripMate/service/impl/DashboardServiceImpl.java \
        src/main/java/com/LHZ/TripMate/controller/admin/DashboardController.java
git commit -m "feat: add dashboard stats endpoint"
```

---

## PHASE 2 — Frontend Admin Panel

### Task 11: Scaffold Vite project

**Files:**
- Create: `admin/package.json`
- Create: `admin/vite.config.ts`
- Create: `admin/tsconfig.json`
- Create: `admin/index.html`
- Create: `admin/src/main.ts`
- Create: `admin/src/App.vue`

- [ ] **Step 1: Initialize Vite project**

```bash
cd D:\code\作业\综合实践2\TripMate
npm create vite@latest admin -- --template vue-ts
cd admin
npm install
```

- [ ] **Step 2: Install dependencies**

```bash
npm install element-plus @element-plus/icons-vue
npm install pinia vue-router axios
npm install -D unplugin-auto-import unplugin-vue-components
```

- [ ] **Step 3: Replace `admin/vite.config.ts`**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 4: Replace `admin/src/main.ts`**

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import 'element-plus/dist/index.css'

createApp(App).use(createPinia()).use(router).mount('#app')
```

- [ ] **Step 5: Replace `admin/src/App.vue`**

```vue
<template>
  <router-view />
</template>
```

- [ ] **Step 6: Verify dev server starts**

```bash
cd admin && npm run dev
```

Expected: Server running at `http://localhost:5173`. Browser shows blank page (no routes yet).

- [ ] **Step 7: Commit**

```bash
cd ..
git add admin/
git commit -m "feat: scaffold admin Vite project with Element Plus and proxy"
```

---

### Task 12: Auth store + router + Axios

**Files:**
- Create: `admin/src/stores/auth.ts`
- Create: `admin/src/api/http.ts`
- Create: `admin/src/api/auth.ts`
- Create: `admin/src/router/index.ts`

- [ ] **Step 1: Create `admin/src/stores/auth.ts`**

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('admin_token') ?? '')
  const username = ref<string>(localStorage.getItem('admin_username') ?? '')
  const role = ref<string>(localStorage.getItem('admin_role') ?? '')

  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')

  function setAuth(t: string, u: string, r: string) {
    token.value = t
    username.value = u
    role.value = r
    localStorage.setItem('admin_token', t)
    localStorage.setItem('admin_username', u)
    localStorage.setItem('admin_role', r)
  }

  function clearAuth() {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_username')
    localStorage.removeItem('admin_role')
  }

  return { token, username, role, isLoggedIn, isSuperAdmin, setAuth, clearAuth }
})
```

- [ ] **Step 2: Create `admin/src/api/http.ts`**

```typescript
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const http = axios.create({ baseURL: '/api' })

http.interceptors.request.use(config => {
  const auth = useAuthStore()
  if (auth.token) config.headers.Authorization = `Bearer ${auth.token}`
  return config
})

http.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.clearAuth()
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

export default http
```

- [ ] **Step 3: Create `admin/src/api/auth.ts`**

```typescript
import http from './http'

export interface LoginResponse {
  token: string
  role: string
  username: string
}

export function login(username: string, password: string) {
  return http.post<{ code: number; data: LoginResponse }>('/admin/login', { username, password })
}
```

- [ ] **Step 4: Create `admin/src/router/index.ts`**

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/views/login/LoginView.vue') },
    {
      path: '/',
      component: () => import('@/components/AppLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue') },
        { path: 'users', component: () => import('@/views/users/UsersView.vue'), meta: { requiresSuperAdmin: true } },
        { path: 'settings', component: () => import('@/views/settings/SettingsView.vue'), meta: { requiresSuperAdmin: true } },
      ],
    },
  ],
})

router.beforeEach(to => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.isLoggedIn) return '/login'
  if (to.path === '/login' && auth.isLoggedIn) return '/dashboard'
  if (to.meta.requiresSuperAdmin && !auth.isSuperAdmin) return '/dashboard'
})

export default router
```

- [ ] **Step 5: Commit**

```bash
git add admin/src/stores/ admin/src/api/http.ts admin/src/api/auth.ts admin/src/router/
git commit -m "feat: add auth store, axios instance, and router with guards"
```

---

### Task 13: AppLayout component

**Files:**
- Create: `admin/src/components/AppLayout.vue`

- [ ] **Step 1: Create `AppLayout.vue`**

```vue
<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background:#001529">
      <div style="color:#fff;padding:20px;font-size:18px;font-weight:700">TripMate 管理</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="#ccc"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isSuperAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>管理员账号</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isSuperAdmin" index="/settings">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="display:flex;align-items:center;justify-content:flex-end;border-bottom:1px solid #eee">
        <span style="margin-right:16px">{{ auth.username }}</span>
        <el-button size="small" @click="logout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DataLine, User, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.clearAuth()
  router.push('/login')
}
</script>
```

- [ ] **Step 2: Commit**

```bash
git add admin/src/components/AppLayout.vue
git commit -m "feat: add AppLayout with sidebar and topbar"
```

---

### Task 14: LoginView

**Files:**
- Create: `admin/src/views/login/LoginView.vue`

- [ ] **Step 1: Create `LoginView.vue`**

```vue
<template>
  <div style="display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f2f5">
    <el-card style="width:400px">
      <template #header>
        <span style="font-size:20px;font-weight:700">TripMate 后台管理</span>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
                    :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" style="width:100%" :loading="loading">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }],
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form.username, form.password)
    const { token, role, username } = res.data.data
    auth.setAuth(token, username, role)
    router.push('/dashboard')
  } catch {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}
</script>
```

- [ ] **Step 2: Test login in browser**

Open `http://localhost:5173/login`, enter `admin` / `admin123`.
Expected: Redirected to `/dashboard` (blank page is fine — view not built yet).

- [ ] **Step 3: Commit**

```bash
git add admin/src/views/login/LoginView.vue
git commit -m "feat: add admin login page"
```

---

### Task 15: DashboardView

**Files:**
- Create: `admin/src/api/dashboard.ts`
- Create: `admin/src/views/dashboard/DashboardView.vue`

- [ ] **Step 1: Create `admin/src/api/dashboard.ts`**

```typescript
import http from './http'

export interface DashboardData {
  adminUserCount: number
  configCount: number
}

export function getDashboard() {
  return http.get<{ code: number; data: DashboardData }>('/admin/dashboard')
}
```

- [ ] **Step 2: Create `DashboardView.vue`**

```vue
<template>
  <div>
    <h2 style="margin-bottom:24px">数据统计</h2>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <el-statistic title="管理员账号数" :value="stats.adminUserCount" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="系统配置项数" :value="stats.configCount" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { getDashboard } from '@/api/dashboard'

const stats = reactive({ adminUserCount: 0, configCount: 0 })

onMounted(async () => {
  const res = await getDashboard()
  Object.assign(stats, res.data.data)
})
</script>
```

- [ ] **Step 3: Verify in browser**

Open `http://localhost:5173/dashboard` — should show two stat cards with numbers from the backend.

- [ ] **Step 4: Commit**

```bash
git add admin/src/api/dashboard.ts admin/src/views/dashboard/DashboardView.vue
git commit -m "feat: add dashboard stats view"
```

---

### Task 16: UsersView

**Files:**
- Create: `admin/src/api/users.ts`
- Create: `admin/src/views/users/UsersView.vue`

- [ ] **Step 1: Create `admin/src/api/users.ts`**

```typescript
import http from './http'

export interface AdminUser {
  id: number
  username: string
  role: string
  status: number
  createdAt: string
}

export function listUsers() {
  return http.get<{ code: number; data: AdminUser[] }>('/admin/users')
}

export function createUser(data: { username: string; password: string; role: string }) {
  return http.post('/admin/users', data)
}

export function updateUser(id: number, data: { status?: number; password?: string }) {
  return http.put(`/admin/users/${id}`, data)
}

export function deleteUser(id: number) {
  return http.delete(`/admin/users/${id}`)
}
```

- [ ] **Step 2: Create `UsersView.vue`**

```vue
<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>管理员账号</h2>
      <el-button type="primary" @click="openCreate">新增管理员</el-button>
    </div>

    <el-table :data="users" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="role" label="角色" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button size="small" @click="openReset(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增对话框 -->
    <el-dialog v-model="createVisible" title="新增管理员" width="400px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role">
            <el-option label="普通管理员" value="ADMIN" />
            <el-option label="超级管理员" value="SUPER_ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="400px">
      <el-input v-model="newPassword" type="password" placeholder="新密码" show-password />
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, createUser, updateUser, deleteUser, type AdminUser } from '@/api/users'

const users = ref<AdminUser[]>([])

async function load() {
  const res = await listUsers()
  users.value = res.data.data
}

onMounted(load)

// 新增
const createVisible = ref(false)
const createFormRef = ref()
const createForm = reactive({ username: '', password: '', role: 'ADMIN' })
const createRules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }],
  role: [{ required: true }],
}

function openCreate() { createVisible.value = true }

async function handleCreate() {
  await createFormRef.value.validate()
  await createUser(createForm)
  ElMessage.success('创建成功')
  createVisible.value = false
  load()
}

// 启用/禁用
async function toggleStatus(row: AdminUser) {
  await updateUser(row.id, { status: row.status === 1 ? 0 : 1 })
  load()
}

// 重置密码
const resetVisible = ref(false)
const newPassword = ref('')
let resetTargetId = 0

function openReset(row: AdminUser) {
  resetTargetId = row.id
  newPassword.value = ''
  resetVisible.value = true
}

async function handleReset() {
  if (!newPassword.value) return ElMessage.warning('请输入新密码')
  await updateUser(resetTargetId, { password: newPassword.value })
  ElMessage.success('密码已重置')
  resetVisible.value = false
}

// 删除
async function handleDelete(row: AdminUser) {
  await ElMessageBox.confirm(`确认删除管理员 ${row.username}？`, '确认', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  load()
}
</script>
```

- [ ] **Step 3: Verify in browser**

Open `http://localhost:5173/users` — table should show the admin account. Test create / toggle status / delete.

- [ ] **Step 4: Commit**

```bash
git add admin/src/api/users.ts admin/src/views/users/UsersView.vue
git commit -m "feat: add admin users management view"
```

---

### Task 17: SettingsView

**Files:**
- Create: `admin/src/api/settings.ts`
- Create: `admin/src/views/settings/SettingsView.vue`

- [ ] **Step 1: Create `admin/src/api/settings.ts`**

```typescript
import http from './http'

export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  description: string
}

export function listSettings() {
  return http.get<{ code: number; data: SystemConfig[] }>('/admin/settings')
}

export function updateSetting(key: string, value: string) {
  return http.put(`/admin/settings/${key}`, { value })
}
```

- [ ] **Step 2: Create `SettingsView.vue`**

```vue
<template>
  <div>
    <h2 style="margin-bottom:24px">系统配置</h2>
    <el-table :data="configs" border>
      <el-table-column prop="configKey" label="配置键" width="240" />
      <el-table-column label="配置值">
        <template #default="{ row }">
          <el-input v-if="editing === row.configKey" v-model="editValue" size="small" />
          <span v-else>{{ row.configValue }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <template v-if="editing === row.configKey">
            <el-button size="small" type="primary" @click="saveEdit(row)">保存</el-button>
            <el-button size="small" @click="editing = ''">取消</el-button>
          </template>
          <el-button v-else size="small" @click="startEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listSettings, updateSetting, type SystemConfig } from '@/api/settings'

const configs = ref<SystemConfig[]>([])
const editing = ref('')
const editValue = ref('')

onMounted(async () => {
  const res = await listSettings()
  configs.value = res.data.data
})

function startEdit(row: SystemConfig) {
  editing.value = row.configKey
  editValue.value = row.configValue
}

async function saveEdit(row: SystemConfig) {
  await updateSetting(row.configKey, editValue.value)
  row.configValue = editValue.value
  editing.value = ''
  ElMessage.success('保存成功')
}
</script>
```

- [ ] **Step 3: Verify in browser**

Open `http://localhost:5173/settings` — should show the `weather.api.key` row. Click edit, change value, save.

- [ ] **Step 4: Commit**

```bash
git add admin/src/api/settings.ts admin/src/views/settings/SettingsView.vue
git commit -m "feat: add system settings view"
```

---

## Final Verification

- [ ] Backend: Run `./mvnw test` — all tests pass
- [ ] Frontend: Run `cd admin && npm run build` — build succeeds with no TypeScript errors
- [ ] Full flow: Login → Dashboard → Users → Settings all work end-to-end in browser
- [ ] Commit any remaining changes

```bash
git add -A
git commit -m "feat: admin panel complete - backend + frontend"
```
