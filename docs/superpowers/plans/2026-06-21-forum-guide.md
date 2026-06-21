# 攻略论坛功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有空白的 `guide` 页面实现旅游攻略论坛，支持图文发帖、分类浏览、点赞/收藏/评论互动，图片本地上传。

**Architecture:** 后端新增 4 张表（Post/PostLike/PostFavorite/PostComment）+ Upload 接口；前端在 guide 页实现 Feed 列表，新增详情页和发帖页，mine 页补充我的发布/收藏入口。所有图片存至服务器本地 `uploads/` 目录，通过静态资源路由访问。

**Tech Stack:** Spring Boot 4 / Java 21 / JPA / MySQL（后端）；UniApp + Vue 3 Composition API + TypeScript（前端）

## Global Constraints

- 后端统一响应体：`Result<T>` with `{ code, message, data }`，`Result.success(data)` / `Result.fail(message)`
- 前端 API 层：`useApi()` composable（`frontend/utils/useApi.ts`），Base URL `http://localhost:8080`
- JWT 认证：Controller 方法注入 `@AuthenticationPrincipal WxUserDetails userDetails`，`userDetails.getWxUser().getId()` 取 Long userId
- 所有 GET 帖子接口 `permitAll`（游客可浏览），POST/DELETE 需要 JWT
- 图片限制：单文件 ≤ 5MB，存储路径 `./uploads/<uuid>.<ext>`，访问路径 `/uploads/<filename>`
- 帖子软删除：`status` 字段设为 `"DELETED"`，列表只查 `PUBLISHED`
- likeCount / commentCount 使用原子 `@Modifying @Query` 更新，避免并发问题
- 前端不新增 TabBar 项，guide 页（🗺️）直接填充内容

---

## 文件清单

**后端 — 新建：**
- `src/main/java/com/LHZ/TripMate/entity/Post.java`
- `src/main/java/com/LHZ/TripMate/entity/PostLike.java`
- `src/main/java/com/LHZ/TripMate/entity/PostFavorite.java`
- `src/main/java/com/LHZ/TripMate/entity/PostComment.java`
- `src/main/java/com/LHZ/TripMate/entity/converter/StringListConverter.java`
- `src/main/java/com/LHZ/TripMate/dto/PostDTO.java`
- `src/main/java/com/LHZ/TripMate/dto/PostCreateDTO.java`
- `src/main/java/com/LHZ/TripMate/dto/PostCommentDTO.java`
- `src/main/java/com/LHZ/TripMate/dto/CommentCreateDTO.java`
- `src/main/java/com/LHZ/TripMate/dto/PageResult.java`
- `src/main/java/com/LHZ/TripMate/repository/PostRepository.java`
- `src/main/java/com/LHZ/TripMate/repository/PostLikeRepository.java`
- `src/main/java/com/LHZ/TripMate/repository/PostFavoriteRepository.java`
- `src/main/java/com/LHZ/TripMate/repository/PostCommentRepository.java`
- `src/main/java/com/LHZ/TripMate/service/PostService.java`
- `src/main/java/com/LHZ/TripMate/service/UploadService.java`
- `src/main/java/com/LHZ/TripMate/service/impl/PostServiceImpl.java`
- `src/main/java/com/LHZ/TripMate/service/impl/UploadServiceImpl.java`
- `src/main/java/com/LHZ/TripMate/controller/PostController.java`
- `src/main/java/com/LHZ/TripMate/controller/UploadController.java`
- `src/main/java/com/LHZ/TripMate/config/WebMvcConfig.java`

**后端 — 修改：**
- `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java`（添加 GET permitAll 规则）
- `src/main/resources/application.yaml`（添加 `upload.dir` 配置）

**前端 — 新建：**
- `frontend/api/post.ts`
- `frontend/api/upload.ts`
- `frontend/pages/guide/detail/detail.vue`
- `frontend/pages/guide/create/create.vue`

**前端 — 修改：**
- `frontend/utils/useApi.ts`（新增 `del` 方法）
- `frontend/pages/guide/guide.vue`（填充 Feed 内容）
- `frontend/pages/mine/mine.vue`（替换 onCollect，新增我的攻略入口）
- `frontend/pages.json`（注册 detail 和 create 两个新页面）

---

## Task 1: 后端实体、Repository、DTO、工具类

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/entity/Post.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/PostLike.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/PostFavorite.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/PostComment.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/converter/StringListConverter.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/PostDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/PostCreateDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/PostCommentDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/CommentCreateDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/PageResult.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/PostRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/PostLikeRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/PostFavoriteRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/PostCommentRepository.java`

**Interfaces:**
- Produces: `Post` entity（所有字段），`PostDTO` record（列表+详情），`PageResult<T>` record，全部 Repository 接口

- [ ] **Step 1: 创建 StringListConverter**

```java
// src/main/java/com/LHZ/TripMate/entity/converter/StringListConverter.java
package com.LHZ.TripMate.entity.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try { return MAPPER.writeValueAsString(list); } catch (Exception e) { return null; }
    }

    @Override
    public List<String> convertToEntityAttribute(String s) {
        if (s == null || s.isBlank()) return new ArrayList<>();
        try { return MAPPER.readValue(s, new TypeReference<>() {}); } catch (Exception e) { return new ArrayList<>(); }
    }
}
```

- [ ] **Step 2: 创建 Post 实体**

```java
// src/main/java/com/LHZ/TripMate/entity/Post.java
package com.LHZ.TripMate.entity;

import com.LHZ.TripMate.entity.converter.StringListConverter;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    @Convert(converter = StringListConverter.class)
    @Column(name = "image_urls", columnDefinition = "TEXT")
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "view_count")
    private int viewCount = 0;

    @Column(name = "like_count")
    private int likeCount = 0;

    @Column(name = "comment_count")
    private int commentCount = 0;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }
}
```

- [ ] **Step 3: 创建 PostLike、PostFavorite、PostComment 实体**

```java
// src/main/java/com/LHZ/TripMate/entity/PostLike.java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "post_like",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class PostLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }
}
```

```java
// src/main/java/com/LHZ/TripMate/entity/PostFavorite.java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "post_favorite",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class PostFavorite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }
}
```

```java
// src/main/java/com/LHZ/TripMate/entity/PostComment.java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "post_comment")
public class PostComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "post_id", nullable = false) private Long postId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 500) private String content;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); }
}
```

- [ ] **Step 4: 创建 DTO 和 PageResult**

```java
// src/main/java/com/LHZ/TripMate/dto/PageResult.java
package com.LHZ.TripMate.dto;

import java.util.List;

public record PageResult<T>(List<T> items, long total, int page, int size) {}
```

```java
// src/main/java/com/LHZ/TripMate/dto/PostCreateDTO.java
package com.LHZ.TripMate.dto;

import lombok.Data;
import java.util.List;

@Data
public class PostCreateDTO {
    private String title;
    private String content;
    private String category;
    private List<String> imageUrls;
}
```

```java
// src/main/java/com/LHZ/TripMate/dto/PostDTO.java
package com.LHZ.TripMate.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private String category;
    private String coverUrl;
    private List<String> imageUrls;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private AuthorDTO author;
    private boolean liked;
    private boolean favorited;

    @Data @Builder
    public static class AuthorDTO {
        private Long id;
        private String nickname;
        private String avatarUrl;
    }
}
```

```java
// src/main/java/com/LHZ/TripMate/dto/PostCommentDTO.java
package com.LHZ.TripMate.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PostCommentDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private PostDTO.AuthorDTO author;
}
```

```java
// src/main/java/com/LHZ/TripMate/dto/CommentCreateDTO.java
package com.LHZ.TripMate.dto;

import lombok.Data;

@Data
public class CommentCreateDTO {
    private String content;
}
```

- [ ] **Step 5: 创建 Repository**

```java
// src/main/java/com/LHZ/TripMate/repository/PostRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByStatus(String status, Pageable pageable);

    Page<Post> findByStatusAndCategory(String status, String category, Pageable pageable);

    Page<Post> findByStatusAndUserId(String status, Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :id")
    void incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
```

```java
// src/main/java/com/LHZ/TripMate/repository/PostLikeRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
```

```java
// src/main/java/com/LHZ/TripMate/repository/PostFavoriteRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.PostFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {
    Optional<PostFavorite> findByPostIdAndUserId(Long postId, Long userId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    Page<PostFavorite> findByUserId(Long userId, Pageable pageable);
}
```

```java
// src/main/java/com/LHZ/TripMate/repository/PostCommentRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    Page<PostComment> findByPostId(Long postId, Pageable pageable);
}
```

- [ ] **Step 6: 编译验证**

在项目根目录执行：
```bash
./mvnw compile -q
```
预期：BUILD SUCCESS，无编译错误。

- [ ] **Step 7: 提交**

```bash
git add src/main/java/com/LHZ/TripMate/entity/ src/main/java/com/LHZ/TripMate/dto/ src/main/java/com/LHZ/TripMate/repository/
git commit -m "feat: 论坛实体、Repository、DTO"
```

---

## Task 2: 图片上传（Upload）

**Files:**
- Modify: `src/main/resources/application.yaml`
- Create: `src/main/java/com/LHZ/TripMate/config/WebMvcConfig.java`
- Create: `src/main/java/com/LHZ/TripMate/service/UploadService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/UploadServiceImpl.java`
- Create: `src/main/java/com/LHZ/TripMate/controller/UploadController.java`

**Interfaces:**
- Consumes: 无
- Produces: `POST /api/upload` → `Result<Map<String, String>>` 其中 `data.url` 为可访问图片 URL

- [ ] **Step 1: 在 application.yaml 添加上传配置**

在 `src/main/resources/application.yaml` 末尾追加：
```yaml
upload:
  dir: ./uploads
  base-url: http://localhost:8080

spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

注意：`spring.servlet.multipart` 需要插入到已有的 `spring:` 块中，而不是新建一个 `spring:` 块。完整 `spring:` 节应为：

```yaml
spring:
  application:
    name: TripMate
  datasource:
    url: jdbc:mysql://47.109.38.44:3306/tripmate?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8
    username: lhz
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

同时在文件末尾追加：
```yaml
upload:
  dir: ./uploads
  base-url: http://localhost:8080
```

- [ ] **Step 2: 创建 WebMvcConfig（静态资源映射）**

```java
// src/main/java/com/LHZ/TripMate/config/WebMvcConfig.java
package com.LHZ.TripMate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
```

- [ ] **Step 3: 创建 UploadService 接口和实现**

```java
// src/main/java/com/LHZ/TripMate/service/UploadService.java
package com.LHZ.TripMate.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    /** 保存文件到 uploads/ 目录，返回可访问的完整 URL */
    String save(MultipartFile file);
}
```

```java
// src/main/java/com/LHZ/TripMate/service/impl/UploadServiceImpl.java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    @Value("${upload.dir:./uploads}")
    private String uploadDir;

    @Value("${upload.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public String save(MultipartFile file) {
        if (file.isEmpty()) throw new RuntimeException("文件不能为空");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("只支持图片格式");
        }

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.'))
                : ".jpg";

        String filename = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadDir).toAbsolutePath();

        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败");
        }

        return baseUrl + "/uploads/" + filename;
    }
}
```

- [ ] **Step 4: 创建 UploadController**

```java
// src/main/java/com/LHZ/TripMate/controller/UploadController.java
package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String url = uploadService.save(file);
            return Result.success(Map.of("url", url));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}
```

- [ ] **Step 5: 验证上传接口**

先启动后端：`./mvnw spring-boot:run`

获取登录 token（需要微信小程序端或已有 token）。然后测试上传（用任意 JPG）：

```bash
curl -X POST http://localhost:8080/api/upload \
  -H "Authorization: Bearer <你的JWT>" \
  -F "file=@/path/to/test.jpg"
```

预期响应：
```json
{ "code": 200, "message": "操作成功", "data": { "url": "http://localhost:8080/uploads/xxxx.jpg" } }
```

再访问返回的 URL，确认图片可正常显示。

- [ ] **Step 6: 提交**

```bash
git add src/main/resources/application.yaml \
        src/main/java/com/LHZ/TripMate/config/WebMvcConfig.java \
        src/main/java/com/LHZ/TripMate/service/UploadService.java \
        src/main/java/com/LHZ/TripMate/service/impl/UploadServiceImpl.java \
        src/main/java/com/LHZ/TripMate/controller/UploadController.java
git commit -m "feat: 图片上传接口 + 静态资源映射"
```

---

## Task 3: 后端帖子 CRUD + SecurityConfig 更新

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/service/PostService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/PostServiceImpl.java`
- Create: `src/main/java/com/LHZ/TripMate/controller/PostController.java`
- Modify: `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java`

**Interfaces:**
- Consumes: Task 1 的所有 Entity / Repository / DTO
- Produces:
  - `GET /api/posts` → `Result<PageResult<PostDTO>>`
  - `POST /api/posts` → `Result<PostDTO>`
  - `GET /api/posts/{id}` → `Result<PostDTO>`
  - `POST /api/posts/{id}/delete` → `Result<Void>`
  - `GET /api/posts/my` → `Result<PageResult<PostDTO>>`

- [ ] **Step 1: 创建 PostService 接口**

```java
// src/main/java/com/LHZ/TripMate/service/PostService.java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.*;
import org.springframework.data.domain.Pageable;

public interface PostService {
    PostDTO create(PostCreateDTO dto, Long userId);
    PageResult<PostDTO> list(String category, String sort, int page, int size, Long currentUserId);
    PostDTO detail(Long id, Long currentUserId);
    void delete(Long id, Long userId);

    Map<String, Object> toggleLike(Long postId, Long userId);
    Map<String, Object> toggleFavorite(Long postId, Long userId);

    PageResult<PostCommentDTO> listComments(Long postId, int page, int size);
    PostCommentDTO addComment(Long postId, String content, Long userId);

    PageResult<PostDTO> myPosts(Long userId, int page, int size);
    PageResult<PostDTO> myFavorites(Long userId, int page, int size);
}
```

注意：需要在文件顶部加 `import java.util.Map;`

- [ ] **Step 2: 创建 PostServiceImpl**

```java
// src/main/java/com/LHZ/TripMate/service/impl/PostServiceImpl.java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.*;
import com.LHZ.TripMate.entity.*;
import com.LHZ.TripMate.entity.WxUser;
import com.LHZ.TripMate.repository.*;
import com.LHZ.TripMate.service.PostService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepo;
    private final PostLikeRepository likeRepo;
    private final PostFavoriteRepository favoriteRepo;
    private final PostCommentRepository commentRepo;
    private final com.LHZ.TripMate.repository.WxUserRepository wxUserRepo;

    @Override
    @Transactional
    public PostDTO create(PostCreateDTO dto, Long userId) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent().trim());
        post.setCategory(dto.getCategory());
        List<String> imgs = dto.getImageUrls() != null ? dto.getImageUrls() : new ArrayList<>();
        post.setImageUrls(imgs);
        post.setCoverUrl(imgs.isEmpty() ? null : imgs.get(0));
        post = postRepo.save(post);
        WxUser user = wxUserRepo.findById(userId).orElseThrow();
        return toDTO(post, user, false, false);
    }

    @Override
    public PageResult<PostDTO> list(String category, String sort, int page, int size, Long currentUserId) {
        Sort s = "hot".equals(sort)
                ? Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createdAt"))
                : Sort.by(Sort.Order.desc("createdAt"));
        Pageable pageable = PageRequest.of(page, size, s);

        Page<Post> posts = (category == null || category.isBlank() || "ALL".equals(category))
                ? postRepo.findByStatus("PUBLISHED", pageable)
                : postRepo.findByStatusAndCategory("PUBLISHED", category, pageable);

        List<PostDTO> items = posts.getContent().stream().map(p -> {
            WxUser user = wxUserRepo.findById(p.getUserId()).orElse(null);
            boolean liked = currentUserId != null && likeRepo.existsByPostIdAndUserId(p.getId(), currentUserId);
            boolean faved = currentUserId != null && favoriteRepo.existsByPostIdAndUserId(p.getId(), currentUserId);
            return toDTO(p, user, liked, faved);
        }).toList();

        return new PageResult<>(items, posts.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public PostDTO detail(Long id, Long currentUserId) {
        Post post = postRepo.findById(id)
                .filter(p -> "PUBLISHED".equals(p.getStatus()))
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        postRepo.incrementViewCount(id);
        post.setViewCount(post.getViewCount() + 1);
        WxUser user = wxUserRepo.findById(post.getUserId()).orElse(null);
        boolean liked = currentUserId != null && likeRepo.existsByPostIdAndUserId(id, currentUserId);
        boolean faved = currentUserId != null && favoriteRepo.existsByPostIdAndUserId(id, currentUserId);
        return toDTO(post, user, liked, faved);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        Post post = postRepo.findById(id).orElseThrow(() -> new RuntimeException("帖子不存在"));
        if (!post.getUserId().equals(userId)) throw new RuntimeException("无权删除");
        post.setStatus("DELETED");
        postRepo.save(post);
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        Optional<PostLike> existing = likeRepo.findByPostIdAndUserId(postId, userId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepo.delete(existing.get());
            postRepo.decrementLikeCount(postId);
            liked = false;
        } else {
            PostLike like = new PostLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeRepo.save(like);
            postRepo.incrementLikeCount(postId);
            liked = true;
        }
        int count = postRepo.findById(postId).map(Post::getLikeCount).orElse(0);
        return Map.of("liked", liked, "likeCount", count);
    }

    @Override
    @Transactional
    public Map<String, Object> toggleFavorite(Long postId, Long userId) {
        Optional<PostFavorite> existing = favoriteRepo.findByPostIdAndUserId(postId, userId);
        boolean favorited;
        if (existing.isPresent()) {
            favoriteRepo.delete(existing.get());
            favorited = false;
        } else {
            PostFavorite fav = new PostFavorite();
            fav.setPostId(postId);
            fav.setUserId(userId);
            favoriteRepo.save(fav);
            favorited = true;
        }
        return Map.of("favorited", favorited);
    }

    @Override
    public PageResult<PostCommentDTO> listComments(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<PostComment> pg = commentRepo.findByPostId(postId, pageable);
        List<PostCommentDTO> items = pg.getContent().stream().map(c -> {
            WxUser user = wxUserRepo.findById(c.getUserId()).orElse(null);
            return PostCommentDTO.builder()
                    .id(c.getId())
                    .content(c.getContent())
                    .createdAt(c.getCreatedAt())
                    .author(toAuthorDTO(user))
                    .build();
        }).toList();
        return new PageResult<>(items, pg.getTotalElements(), page, size);
    }

    @Override
    @Transactional
    public PostCommentDTO addComment(Long postId, String content, Long userId) {
        postRepo.findById(postId)
                .filter(p -> "PUBLISHED".equals(p.getStatus()))
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        PostComment c = new PostComment();
        c.setPostId(postId);
        c.setUserId(userId);
        c.setContent(content.trim());
        c = commentRepo.save(c);
        postRepo.incrementCommentCount(postId);
        WxUser user = wxUserRepo.findById(userId).orElse(null);
        return PostCommentDTO.builder()
                .id(c.getId())
                .content(c.getContent())
                .createdAt(c.getCreatedAt())
                .author(toAuthorDTO(user))
                .build();
    }

    @Override
    public PageResult<PostDTO> myPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> pg = postRepo.findByStatusAndUserId("PUBLISHED", userId, pageable);
        WxUser user = wxUserRepo.findById(userId).orElse(null);
        List<PostDTO> items = pg.getContent().stream()
                .map(p -> toDTO(p, user, false, false)).toList();
        return new PageResult<>(items, pg.getTotalElements(), page, size);
    }

    @Override
    public PageResult<PostDTO> myFavorites(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostFavorite> pg = favoriteRepo.findByUserId(userId, pageable);
        List<PostDTO> items = pg.getContent().stream().map(fav -> {
            Post post = postRepo.findById(fav.getPostId())
                    .filter(p -> "PUBLISHED".equals(p.getStatus())).orElse(null);
            if (post == null) return null;
            WxUser user = wxUserRepo.findById(post.getUserId()).orElse(null);
            return toDTO(post, user, true, true);
        }).filter(Objects::nonNull).toList();
        return new PageResult<>(items, pg.getTotalElements(), page, size);
    }

    // ---- 私有辅助 ----

    private PostDTO toDTO(Post p, WxUser user, boolean liked, boolean favorited) {
        return PostDTO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .content(p.getContent())
                .category(p.getCategory())
                .coverUrl(p.getCoverUrl())
                .imageUrls(p.getImageUrls())
                .viewCount(p.getViewCount())
                .likeCount(p.getLikeCount())
                .commentCount(p.getCommentCount())
                .createdAt(p.getCreatedAt())
                .author(toAuthorDTO(user))
                .liked(liked)
                .favorited(favorited)
                .build();
    }

    private PostDTO.AuthorDTO toAuthorDTO(WxUser user) {
        if (user == null) return PostDTO.AuthorDTO.builder().build();
        return PostDTO.AuthorDTO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
```

> **注意：** 需要确认 `WxUserRepository` 已存在。检查 `src/main/java/com/LHZ/TripMate/repository/WxUserRepository.java`，若不存在则新建：
> ```java
> package com.LHZ.TripMate.repository;
> import com.LHZ.TripMate.entity.WxUser;
> import org.springframework.data.jpa.repository.JpaRepository;
> import java.util.Optional;
> public interface WxUserRepository extends JpaRepository<WxUser, Long> {
>     Optional<WxUser> findByOpenid(String openid);
> }
> ```

- [ ] **Step 3: 创建 PostController**

```java
// src/main/java/com/LHZ/TripMate/controller/PostController.java
package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.*;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public Result<PostDTO> create(@RequestBody PostCreateDTO dto,
                                  @AuthenticationPrincipal WxUserDetails userDetails) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) return Result.fail("标题不能为空");
        if (dto.getContent() == null || dto.getContent().isBlank()) return Result.fail("内容不能为空");
        if (dto.getCategory() == null || dto.getCategory().isBlank()) return Result.fail("请选择分类");
        try {
            return Result.success(postService.create(dto, userDetails.getWxUser().getId()));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping
    public Result<PageResult<PostDTO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getWxUser().getId() : null;
        return Result.success(postService.list(category, sort, page, size, userId));
    }

    @GetMapping("/my")
    public Result<PageResult<PostDTO>> myPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(postService.myPosts(userDetails.getWxUser().getId(), page, size));
    }

    @GetMapping("/my/favorites")
    public Result<PageResult<PostDTO>> myFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(postService.myFavorites(userDetails.getWxUser().getId(), page, size));
    }

    @GetMapping("/{id}")
    public Result<PostDTO> detail(@PathVariable Long id,
                                  @AuthenticationPrincipal WxUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getWxUser().getId() : null;
        try {
            return Result.success(postService.detail(id, userId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/delete")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal WxUserDetails userDetails) {
        try {
            postService.delete(id, userDetails.getWxUser().getId());
            return Result.success();
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> like(@PathVariable Long id,
                                            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(postService.toggleLike(id, userDetails.getWxUser().getId()));
    }

    @PostMapping("/{id}/favorite")
    public Result<Map<String, Object>> favorite(@PathVariable Long id,
                                                @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(postService.toggleFavorite(id, userDetails.getWxUser().getId()));
    }

    @GetMapping("/{id}/comments")
    public Result<PageResult<PostCommentDTO>> comments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(postService.listComments(id, page, size));
    }

    @PostMapping("/{id}/comments")
    public Result<PostCommentDTO> addComment(@PathVariable Long id,
                                             @RequestBody CommentCreateDTO dto,
                                             @AuthenticationPrincipal WxUserDetails userDetails) {
        if (dto.getContent() == null || dto.getContent().isBlank()) return Result.fail("评论不能为空");
        if (dto.getContent().length() > 500) return Result.fail("评论最多 500 字");
        try {
            return Result.success(postService.addComment(id, dto.getContent(), userDetails.getWxUser().getId()));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }
}
```

- [ ] **Step 4: 更新 SecurityConfig，开放 GET 帖子接口**

修改 `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java`，在 `.authorizeHttpRequests` 块中，在 `.requestMatchers("/api/**").authenticated()` **之前**添加两行：

```java
.requestMatchers(org.springframework.http.HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
.requestMatchers(org.springframework.http.HttpMethod.GET, "/uploads/**").permitAll()
```

最终 `.authorizeHttpRequests` 块为：
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/ws", "/ws/**").permitAll()
    .requestMatchers("/api/admin/login", "/api/wx/login", "/api/badges").permitAll()
    .requestMatchers("/api/spots/**", "/api/weather/**", "/api/translate").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll()
)
```

需要在文件顶部加 `import org.springframework.http.HttpMethod;`

- [ ] **Step 5: 启动并验证核心接口**

```bash
./mvnw spring-boot:run
```

测试帖子列表（不需要 token）：
```bash
curl "http://localhost:8080/api/posts?sort=new&page=0&size=5"
```
预期：`{ "code": 200, "data": { "items": [], "total": 0, "page": 0, "size": 5 } }`

测试发帖（需要 token）：
```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"title":"测试攻略","content":"这是内容","category":"SCENIC","imageUrls":[]}'
```
预期：`{ "code": 200, "data": { "id": 1, "title": "测试攻略", ... } }`

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/LHZ/TripMate/
git commit -m "feat: 帖子 CRUD、互动、评论接口"
```

---

## Task 4: 前端 API 层

**Files:**
- Modify: `frontend/utils/useApi.ts`（新增 `del` 方法）
- Create: `frontend/api/post.ts`
- Create: `frontend/api/upload.ts`

**Interfaces:**
- Produces:
  - `fetchPosts(params)`, `fetchPostDetail(id)`, `createPost(data)`, `deletePost(id)`, `toggleLike(id)`, `toggleFavorite(id)`, `fetchComments(id, params)`, `createComment(id, content)`, `fetchMyPosts(params)`, `fetchMyFavorites(params)`
  - `uploadImage(filePath): Promise<string>` — 返回图片 URL

- [ ] **Step 1: 在 useApi.ts 新增 `del` 方法**

打开 `frontend/utils/useApi.ts`，在 `return { get, post }` 前添加 `del` 函数，并在 return 中导出：

```typescript
function del<T = any>(
  url: string,
  options?: RequestOptions
): Promise<ApiResponse<T>> {
  return request<T>('POST', url + '/delete', undefined, options)
}
```

修改最后的 return 为：
```typescript
return { get, post, del }
```

- [ ] **Step 2: 创建 post.ts**

```typescript
// frontend/api/post.ts
import { useApi } from '@/utils/useApi'

const { get, post, del } = useApi()

export interface PostAuthor {
  id: number
  nickname: string
  avatarUrl: string
}

export interface PostItem {
  id: number
  title: string
  content: string
  category: string
  coverUrl: string
  imageUrls: string[]
  viewCount: number
  likeCount: number
  commentCount: number
  createdAt: string
  author: PostAuthor
  liked: boolean
  favorited: boolean
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface CommentItem {
  id: number
  content: string
  createdAt: string
  author: PostAuthor
}

export function fetchPosts(params: {
  category?: string
  sort?: 'new' | 'hot'
  page?: number
  size?: number
}) {
  return get<PageResult<PostItem>>('/api/posts', params as any)
}

export function fetchPostDetail(id: number) {
  return get<PostItem>(`/api/posts/${id}`)
}

export function createPost(data: {
  title: string
  content: string
  category: string
  imageUrls: string[]
}) {
  return post<PostItem>('/api/posts', data)
}

export function deletePost(id: number) {
  return post<void>(`/api/posts/${id}/delete`)
}

export function toggleLike(id: number) {
  return post<{ liked: boolean; likeCount: number }>(`/api/posts/${id}/like`)
}

export function toggleFavorite(id: number) {
  return post<{ favorited: boolean }>(`/api/posts/${id}/favorite`)
}

export function fetchComments(id: number, params: { page?: number; size?: number }) {
  return get<PageResult<CommentItem>>(`/api/posts/${id}/comments`, params as any)
}

export function createComment(id: number, content: string) {
  return post<CommentItem>(`/api/posts/${id}/comments`, { content })
}

export function fetchMyPosts(params: { page?: number; size?: number }) {
  return get<PageResult<PostItem>>('/api/posts/my', params as any)
}

export function fetchMyFavorites(params: { page?: number; size?: number }) {
  return get<PageResult<PostItem>>('/api/posts/my/favorites', params as any)
}
```

- [ ] **Step 3: 创建 upload.ts**

```typescript
// frontend/api/upload.ts
const BASE_URL = 'http://localhost:8080'

export function uploadImage(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: BASE_URL + '/api/upload',
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        try {
          const data = JSON.parse(res.data)
          if (data.code === 200 && data.data?.url) {
            resolve(data.data.url)
          } else {
            uni.showToast({ title: data.message || '上传失败', icon: 'none' })
            reject(new Error(data.message))
          }
        } catch {
          reject(new Error('解析响应失败'))
        }
      },
      fail: () => {
        uni.showToast({ title: '上传失败，请重试', icon: 'none' })
        reject(new Error('上传失败'))
      }
    })
  })
}
```

- [ ] **Step 4: 提交**

```bash
git add frontend/utils/useApi.ts frontend/api/post.ts frontend/api/upload.ts
git commit -m "feat: 前端帖子和上传 API 封装"
```

---

## Task 5: 注册新页面到 pages.json

**Files:**
- Modify: `frontend/pages.json`

**Interfaces:**
- Produces: `pages/guide/detail/detail` 和 `pages/guide/create/create` 路由可用

- [ ] **Step 1: 在 pages.json 的 pages 数组末尾追加两个页面**

打开 `frontend/pages.json`，在 `pages` 数组中，`pages/trip/trip` 条目**之后**追加：

```json
{
  "path": "pages/guide/detail/detail",
  "style": {
    "navigationBarTitleText": "攻略详情",
    "navigationBarBackgroundColor": "#ffffff",
    "navigationBarTextStyle": "black"
  }
},
{
  "path": "pages/guide/create/create",
  "style": {
    "navigationBarTitleText": "发布攻略",
    "navigationBarBackgroundColor": "#ffffff",
    "navigationBarTextStyle": "black"
  }
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/pages.json
git commit -m "feat: 注册攻略详情和发帖页面路由"
```

---

## Task 6: 攻略 Feed 主页（guide.vue）

**Files:**
- Modify: `frontend/pages/guide/guide.vue`

**Interfaces:**
- Consumes: `fetchPosts` from `frontend/api/post.ts`，`PostItem`，`PageResult`
- Produces: 可浏览帖子列表、分类筛选、排序切换、点击跳转详情、点击发帖按钮

- [ ] **Step 1: 替换 guide.vue 全部内容**

```vue
<!-- frontend/pages/guide/guide.vue -->
<template>
  <view class="page">

    <!-- 分类 Tab 横向滚动 -->
    <scroll-view class="category-bar" scroll-x>
      <view class="category-list">
        <view
          v-for="c in categories"
          :key="c.value"
          class="category-item"
          :class="{ active: activeCategory === c.value }"
          @click="onCategory(c.value)"
        >{{ c.label }}</view>
      </view>
    </scroll-view>

    <!-- 排序切换 -->
    <view class="sort-bar">
      <view class="sort-pill">
        <text
          class="sort-item"
          :class="{ active: sort === 'new' }"
          @click="onSort('new')"
        >最新</text>
        <text
          class="sort-item"
          :class="{ active: sort === 'hot' }"
          @click="onSort('hot')"
        >热门</text>
      </view>
    </view>

    <!-- 帖子列表 -->
    <scroll-view
      class="feed"
      scroll-y
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-if="posts.length === 0 && !loading" class="empty">
        <text class="empty-text">暂无攻略，来发布第一篇吧~</text>
      </view>

      <view
        v-for="item in posts"
        :key="item.id"
        class="post-card"
        @click="goDetail(item.id)"
      >
        <!-- 封面图 -->
        <image
          v-if="item.coverUrl"
          class="cover"
          :src="item.coverUrl"
          mode="aspectFill"
        />
        <view v-else class="cover cover-placeholder" />

        <!-- 分类角标 -->
        <view class="category-badge">{{ categoryLabel(item.category) }}</view>

        <!-- 内容 -->
        <view class="card-body">
          <text class="post-title">{{ item.title }}</text>
          <view class="card-meta">
            <view class="author-row">
              <image
                v-if="item.author?.avatarUrl"
                class="avatar"
                :src="item.author.avatarUrl"
                mode="aspectFill"
              />
              <view v-else class="avatar avatar-placeholder" />
              <text class="author-name">{{ item.author?.nickname || '旅行者' }}</text>
            </view>
            <view class="stats-row">
              <text class="stat">👍 {{ item.likeCount }}</text>
              <text class="stat">💬 {{ item.commentCount }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-tip"><text>加载中...</text></view>
      <view v-if="noMore && posts.length > 0" class="loading-tip"><text>已经到底了</text></view>
      <view class="tabbar-placeholder" />
    </scroll-view>

    <!-- 发帖浮动按钮 -->
    <view class="fab" @click="goCreate">
      <text class="fab-icon">✏️</text>
    </view>

    <TabBar active="guide" />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import TabBar from '@/components/TabBar/TabBar.vue'
import { fetchPosts, type PostItem } from '@/api/post'
import { useAuth } from '@/composables/useAuth'

const { authState } = useAuth()

const categories = [
  { value: 'ALL',          label: '全部' },
  { value: 'SCENIC',       label: '景点攻略' },
  { value: 'FOOD',         label: '美食推荐' },
  { value: 'TRANSPORT',    label: '交通住宿' },
  { value: 'FREE_TRAVEL',  label: '自由行' },
  { value: 'FAMILY',       label: '亲子游' },
]

const categoryMap: Record<string, string> = Object.fromEntries(
  categories.map(c => [c.value, c.label])
)
function categoryLabel(v: string) { return categoryMap[v] || v }

const activeCategory = ref('ALL')
const sort = ref<'new' | 'hot'>('new')
const posts = ref<PostItem[]>([])
const page = ref(0)
const loading = ref(false)
const noMore = ref(false)
const refreshing = ref(false)

onMounted(() => load(true))

function onCategory(v: string) {
  if (activeCategory.value === v) return
  activeCategory.value = v
  load(true)
}

function onSort(v: 'new' | 'hot') {
  if (sort.value === v) return
  sort.value = v
  load(true)
}

async function load(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 0; noMore.value = false }
  if (noMore.value) return
  loading.value = true
  try {
    const category = activeCategory.value === 'ALL' ? undefined : activeCategory.value
    const res = await fetchPosts({ category, sort: sort.value, page: page.value, size: 10 })
    if (res.code === 200) {
      const items = res.data.items
      posts.value = reset ? items : [...posts.value, ...items]
      if (posts.value.length >= res.data.total) noMore.value = true
      else page.value++
    }
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function loadMore() { load() }

function onRefresh() {
  refreshing.value = true
  load(true)
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/guide/detail/detail?id=${id}` })
}

function goCreate() {
  if (!authState.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/guide/create/create' })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; display: flex; flex-direction: column; }

/* 分类条 */
.category-bar { background: #fff; border-bottom: 1rpx solid #eee; flex-shrink: 0; }
.category-list { display: flex; flex-direction: row; padding: 0 16rpx; white-space: nowrap; }
.category-item {
  display: inline-block;
  padding: 24rpx 24rpx;
  font-size: 28rpx;
  color: #666;
  white-space: nowrap;
  border-bottom: 4rpx solid transparent;
}
.category-item.active { color: #1677ff; border-bottom-color: #1677ff; font-weight: 600; }

/* 排序 */
.sort-bar { display: flex; justify-content: flex-end; padding: 12rpx 24rpx; background: #f7f8fa; }
.sort-pill { display: flex; background: #ebebeb; border-radius: 32rpx; overflow: hidden; }
.sort-item { padding: 10rpx 28rpx; font-size: 24rpx; color: #666; }
.sort-item.active { background: #1677ff; color: #fff; border-radius: 32rpx; }

/* Feed 滚动区 */
.feed { flex: 1; }

/* 帖子卡片 */
.post-card {
  background: #fff;
  margin: 0 24rpx 20rpx;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
  position: relative;
}
.cover { width: 100%; height: 320rpx; display: block; }
.cover-placeholder { width: 100%; height: 180rpx; background: linear-gradient(135deg,#e0f0ff,#b3d9ff); }
.category-badge {
  position: absolute; top: 16rpx; left: 16rpx;
  background: rgba(22,119,255,0.85); color: #fff;
  font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 16rpx;
}
.card-body { padding: 20rpx 24rpx 24rpx; }
.post-title { font-size: 30rpx; font-weight: 600; color: #1a1a1a; display: block; margin-bottom: 20rpx; }
.card-meta { display: flex; justify-content: space-between; align-items: center; }
.author-row { display: flex; align-items: center; gap: 12rpx; }
.avatar { width: 48rpx; height: 48rpx; border-radius: 50%; }
.avatar-placeholder { width: 48rpx; height: 48rpx; border-radius: 50%; background: #e0e0e0; }
.author-name { font-size: 24rpx; color: #666; }
.stats-row { display: flex; gap: 20rpx; }
.stat { font-size: 24rpx; color: #999; }

/* 空状态 */
.empty { padding: 120rpx 0; text-align: center; }
.empty-text { font-size: 28rpx; color: #bbb; }

/* 加载提示 */
.loading-tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #bbb; }

/* 发帖 FAB */
.fab {
  position: fixed; right: 40rpx; bottom: 180rpx;
  width: 108rpx; height: 108rpx; border-radius: 54rpx;
  background: linear-gradient(135deg,#1677ff,#4facfe);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(22,119,255,0.4);
  z-index: 100;
}
.fab-icon { font-size: 44rpx; }

.tabbar-placeholder { height: 140rpx; }
</style>
```

- [ ] **Step 2: 在 HBuilderX 编译，在微信开发者工具中验证**

切换到「攻略」Tab，应看到空状态提示「暂无攻略，来发布第一篇吧~」，分类 Tab 和排序切换可以点击。

- [ ] **Step 3: 提交**

```bash
git add frontend/pages/guide/guide.vue
git commit -m "feat: 攻略 Feed 列表页"
```

---

## Task 7: 帖子详情页（detail.vue）

**Files:**
- Create: `frontend/pages/guide/detail/detail.vue`

**Interfaces:**
- Consumes: `fetchPostDetail`, `fetchComments`, `createComment`, `toggleLike`, `toggleFavorite` from `frontend/api/post.ts`
- 通过 URL 参数 `?id=xxx` 接收帖子 ID

- [ ] **Step 1: 创建 detail.vue**

```vue
<!-- frontend/pages/guide/detail/detail.vue -->
<template>
  <view class="page">

    <!-- 图片轮播 -->
    <swiper v-if="post && post.imageUrls && post.imageUrls.length > 0"
      class="swiper" indicator-dots autoplay circular>
      <swiper-item v-for="(url, i) in post.imageUrls" :key="i">
        <image class="swiper-img" :src="url" mode="aspectFill" />
      </swiper-item>
    </swiper>

    <scroll-view class="content-scroll" scroll-y>

      <view v-if="!post" class="loading-wrap"><text>加载中...</text></view>

      <view v-else class="content">

        <!-- 作者信息 -->
        <view class="author-row">
          <image v-if="post.author?.avatarUrl" class="avatar" :src="post.author.avatarUrl" mode="aspectFill" />
          <view v-else class="avatar avatar-placeholder" />
          <view class="author-info">
            <text class="author-name">{{ post.author?.nickname || '旅行者' }}</text>
            <text class="post-time">{{ formatTime(post.createdAt) }} · {{ categoryLabel(post.category) }}</text>
          </view>
        </view>

        <!-- 标题 + 正文 -->
        <text class="post-title">{{ post.title }}</text>
        <text class="post-content">{{ post.content }}</text>

        <!-- 统计 -->
        <view class="stat-row">
          <text class="stat-item">👁 {{ post.viewCount }} 阅读</text>
          <text class="stat-item">👍 {{ post.likeCount }} 点赞</text>
          <text class="stat-item">💬 {{ post.commentCount }} 评论</text>
        </view>

        <!-- 评论列表 -->
        <view class="section-title">全部评论</view>
        <view v-if="comments.length === 0" class="no-comment"><text>暂无评论，快来说两句~</text></view>
        <view v-for="c in comments" :key="c.id" class="comment-item">
          <image v-if="c.author?.avatarUrl" class="c-avatar" :src="c.author.avatarUrl" mode="aspectFill" />
          <view v-else class="c-avatar c-avatar-placeholder" />
          <view class="c-body">
            <text class="c-name">{{ c.author?.nickname || '旅行者' }}</text>
            <text class="c-content">{{ c.content }}</text>
            <text class="c-time">{{ formatTime(c.createdAt) }}</text>
          </view>
        </view>
        <view v-if="commentNoMore && comments.length > 0" class="load-more-tip"><text>已显示全部评论</text></view>
        <button v-if="!commentNoMore && comments.length > 0" class="load-more-btn" @click="loadMoreComments">加载更多评论</button>

        <view class="bottom-placeholder" />
      </view>
    </scroll-view>

    <!-- 底部固定操作栏 -->
    <view class="action-bar">
      <view class="comment-input-wrap" @click="focusComment">
        <text class="comment-placeholder">说点什么...</text>
      </view>
      <view class="action-btn" @click="onLike">
        <text>{{ post?.liked ? '❤️' : '🤍' }}</text>
        <text class="action-label">{{ post?.likeCount || 0 }}</text>
      </view>
      <view class="action-btn" @click="onFavorite">
        <text>{{ post?.favorited ? '⭐' : '☆' }}</text>
        <text class="action-label">收藏</text>
      </view>
    </view>

    <!-- 评论输入 popup -->
    <view v-if="showInput" class="comment-popup" @click.stop>
      <view class="comment-popup-mask" @click="showInput = false" />
      <view class="comment-popup-box">
        <textarea
          v-model="commentText"
          class="comment-textarea"
          placeholder="说点什么..."
          :maxlength="500"
          auto-focus
        />
        <view class="comment-popup-footer">
          <text class="char-count">{{ commentText.length }}/500</text>
          <button class="submit-btn" :disabled="submitting" @click="submitComment">发送</button>
        </view>
      </view>
    </view>

  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchPostDetail, fetchComments, createComment, toggleLike, toggleFavorite, type PostItem, type CommentItem } from '@/api/post'
import { useAuth } from '@/composables/useAuth'

const { authState } = useAuth()

const categoryMap: Record<string, string> = {
  SCENIC: '景点攻略', FOOD: '美食推荐', TRANSPORT: '交通住宿',
  FREE_TRAVEL: '自由行', FAMILY: '亲子游'
}
function categoryLabel(v: string) { return categoryMap[v] || v }

function formatTime(s: string) {
  if (!s) return ''
  return s.slice(0, 16).replace('T', ' ')
}

const postId = ref(0)
const post = ref<PostItem | null>(null)
const comments = ref<CommentItem[]>([])
const commentPage = ref(0)
const commentNoMore = ref(false)
const showInput = ref(false)
const commentText = ref('')
const submitting = ref(false)

onMounted(() => {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1] as any
  postId.value = Number(current.options?.id || 0)
  if (postId.value) {
    loadDetail()
    loadComments(true)
  }
})

async function loadDetail() {
  const res = await fetchPostDetail(postId.value)
  if (res.code === 200) post.value = res.data
}

async function loadComments(reset = false) {
  if (reset) { commentPage.value = 0; commentNoMore.value = false }
  const res = await fetchComments(postId.value, { page: commentPage.value, size: 20 })
  if (res.code === 200) {
    const items = res.data.items
    comments.value = reset ? items : [...comments.value, ...items]
    if (comments.value.length >= res.data.total) commentNoMore.value = true
    else commentPage.value++
  }
}

function loadMoreComments() { loadComments() }

function focusComment() {
  if (!authState.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  showInput.value = true
}

async function submitComment() {
  if (!commentText.value.trim()) return
  submitting.value = true
  try {
    const res = await createComment(postId.value, commentText.value)
    if (res.code === 200) {
      comments.value.unshift(res.data)
      if (post.value) post.value.commentCount++
      commentText.value = ''
      showInput.value = false
      uni.showToast({ title: '评论成功', icon: 'success' })
    }
  } finally {
    submitting.value = false
  }
}

async function onLike() {
  if (!authState.isLoggedIn) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  if (!post.value) return
  const res = await toggleLike(postId.value)
  if (res.code === 200 && post.value) {
    post.value.liked = res.data.liked
    post.value.likeCount = res.data.likeCount
  }
}

async function onFavorite() {
  if (!authState.isLoggedIn) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  if (!post.value) return
  const res = await toggleFavorite(postId.value)
  if (res.code === 200 && post.value) {
    post.value.favorited = res.data.favorited
    uni.showToast({ title: post.value.favorited ? '已收藏' : '已取消收藏', icon: 'none' })
  }
}
</script>

<style scoped>
.page { height: 100vh; display: flex; flex-direction: column; background: #fff; }
.swiper { height: 440rpx; flex-shrink: 0; }
.swiper-img { width: 100%; height: 100%; }
.content-scroll { flex: 1; overflow: hidden; }
.loading-wrap { padding: 80rpx; text-align: center; color: #bbb; }

.content { padding: 32rpx 32rpx 0; }
.author-row { display: flex; align-items: center; gap: 16rpx; margin-bottom: 24rpx; }
.avatar { width: 64rpx; height: 64rpx; border-radius: 50%; }
.avatar-placeholder { width: 64rpx; height: 64rpx; border-radius: 50%; background: #e0e0e0; }
.author-info { display: flex; flex-direction: column; }
.author-name { font-size: 28rpx; font-weight: 600; color: #1a1a1a; }
.post-time { font-size: 22rpx; color: #999; margin-top: 4rpx; }

.post-title { display: block; font-size: 36rpx; font-weight: 700; color: #1a1a1a; margin-bottom: 20rpx; }
.post-content { display: block; font-size: 28rpx; color: #333; line-height: 1.8; white-space: pre-wrap; }

.stat-row { display: flex; gap: 24rpx; padding: 24rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.stat-item { font-size: 24rpx; color: #999; }

.section-title { font-size: 30rpx; font-weight: 600; color: #1a1a1a; padding: 24rpx 0 16rpx; }
.no-comment { padding: 40rpx 0; text-align: center; font-size: 26rpx; color: #bbb; }

.comment-item { display: flex; gap: 16rpx; padding: 20rpx 0; border-bottom: 1rpx solid #f9f9f9; }
.c-avatar { width: 52rpx; height: 52rpx; border-radius: 50%; flex-shrink: 0; }
.c-avatar-placeholder { width: 52rpx; height: 52rpx; border-radius: 50%; background: #e0e0e0; flex-shrink: 0; }
.c-body { flex: 1; display: flex; flex-direction: column; gap: 8rpx; }
.c-name { font-size: 24rpx; font-weight: 600; color: #555; }
.c-content { font-size: 28rpx; color: #1a1a1a; }
.c-time { font-size: 22rpx; color: #bbb; }

.load-more-tip { text-align: center; padding: 20rpx; font-size: 24rpx; color: #bbb; }
.load-more-btn { margin: 16rpx 0; background: #f5f5f5; color: #666; font-size: 26rpx; border: none; border-radius: 8rpx; }
.bottom-placeholder { height: 160rpx; }

/* 底部操作栏 */
.action-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding-bottom: env(safe-area-inset-bottom);
  background: #fff; border-top: 1rpx solid #eee;
  display: flex; align-items: center; padding: 16rpx 24rpx;
  gap: 16rpx;
}
.comment-input-wrap {
  flex: 1; background: #f5f5f5; border-radius: 32rpx;
  padding: 16rpx 24rpx; height: 64rpx;
  display: flex; align-items: center;
}
.comment-placeholder { font-size: 26rpx; color: #bbb; }
.action-btn { display: flex; flex-direction: column; align-items: center; padding: 0 12rpx; }
.action-label { font-size: 20rpx; color: #666; margin-top: 4rpx; }

/* 评论弹窗 */
.comment-popup { position: fixed; inset: 0; z-index: 999; display: flex; flex-direction: column; justify-content: flex-end; }
.comment-popup-mask { flex: 1; background: rgba(0,0,0,0.4); }
.comment-popup-box { background: #fff; padding: 24rpx; padding-bottom: env(safe-area-inset-bottom); }
.comment-textarea { width: 100%; height: 180rpx; background: #f5f5f5; border-radius: 12rpx; padding: 16rpx; font-size: 28rpx; box-sizing: border-box; }
.comment-popup-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 16rpx; }
.char-count { font-size: 22rpx; color: #bbb; }
.submit-btn { background: #1677ff; color: #fff; border: none; border-radius: 32rpx; padding: 0 40rpx; height: 64rpx; line-height: 64rpx; font-size: 28rpx; }
</style>
```

- [ ] **Step 2: 在微信开发者工具中验证**

点击 Feed 中任意帖子（先通过发帖页创建一个），应正常跳转到详情页，图片、标题、正文显示正确，点赞/收藏按钮响应状态变化。

- [ ] **Step 3: 提交**

```bash
git add frontend/pages/guide/detail/
git commit -m "feat: 攻略详情页（图文+评论+点赞收藏）"
```

---

## Task 8: 发帖页（create.vue）

**Files:**
- Create: `frontend/pages/guide/create/create.vue`

**Interfaces:**
- Consumes: `createPost` from `frontend/api/post.ts`，`uploadImage` from `frontend/api/upload.ts`

- [ ] **Step 1: 创建 create.vue**

```vue
<!-- frontend/pages/guide/create/create.vue -->
<template>
  <view class="page">

    <!-- 标题输入 -->
    <view class="field">
      <input
        v-model="form.title"
        class="title-input"
        placeholder="标题（最多 100 字）"
        :maxlength="100"
      />
    </view>
    <view class="divider" />

    <!-- 分类选择 -->
    <view class="field">
      <text class="field-label">分类</text>
      <scroll-view class="cat-scroll" scroll-x>
        <view class="cat-list">
          <view
            v-for="c in categories"
            :key="c.value"
            class="cat-chip"
            :class="{ active: form.category === c.value }"
            @click="form.category = c.value"
          >{{ c.label }}</view>
        </view>
      </scroll-view>
    </view>
    <view class="divider" />

    <!-- 正文输入 -->
    <view class="field">
      <textarea
        v-model="form.content"
        class="content-textarea"
        placeholder="分享你的旅行心得..."
        :maxlength="1000"
      />
      <text class="char-count">{{ form.content.length }}/1000</text>
    </view>
    <view class="divider" />

    <!-- 图片上传 -->
    <view class="field">
      <text class="field-label">图片（最多 3 张）</text>
      <view class="img-list">
        <view
          v-for="(url, i) in form.imageUrls"
          :key="i"
          class="img-item"
        >
          <image class="img-preview" :src="url" mode="aspectFill" />
          <view class="img-del" @click="removeImage(i)">✕</view>
        </view>
        <view
          v-if="form.imageUrls.length < 3"
          class="img-add"
          @click="chooseImage"
        >
          <text class="img-add-icon">📷</text>
          <text class="img-add-label">添加图片</text>
        </view>
      </view>
    </view>

    <!-- 发布按钮 -->
    <view class="submit-wrap">
      <button class="submit-btn" :disabled="submitting" @click="submit">
        {{ submitting ? '发布中...' : '发布攻略' }}
      </button>
    </view>

  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { createPost } from '@/api/post'
import { uploadImage } from '@/api/upload'

const categories = [
  { value: 'SCENIC',      label: '景点攻略' },
  { value: 'FOOD',        label: '美食推荐' },
  { value: 'TRANSPORT',   label: '交通住宿' },
  { value: 'FREE_TRAVEL', label: '自由行' },
  { value: 'FAMILY',      label: '亲子游' },
]

const form = ref({
  title: '',
  content: '',
  category: 'SCENIC',
  imageUrls: [] as string[]
})

const submitting = ref(false)

function chooseImage() {
  uni.chooseImage({
    count: 3 - form.value.imageUrls.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: async (res) => {
      uni.showLoading({ title: '上传中...' })
      try {
        for (const path of res.tempFilePaths) {
          const url = await uploadImage(path)
          form.value.imageUrls.push(url)
          if (form.value.imageUrls.length >= 3) break
        }
      } finally {
        uni.hideLoading()
      }
    }
  })
}

function removeImage(index: number) {
  form.value.imageUrls.splice(index, 1)
}

async function submit() {
  if (!form.value.title.trim()) {
    uni.showToast({ title: '请输入标题', icon: 'none' }); return
  }
  if (!form.value.content.trim()) {
    uni.showToast({ title: '请输入内容', icon: 'none' }); return
  }
  submitting.value = true
  try {
    const res = await createPost({
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      category: form.value.category,
      imageUrls: form.value.imageUrls
    })
    if (res.code === 200) {
      uni.showToast({ title: '发布成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1200)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #fff; padding-bottom: 40rpx; }

.field { padding: 24rpx 32rpx; }
.field-label { display: block; font-size: 26rpx; color: #999; margin-bottom: 16rpx; }
.divider { height: 1rpx; background: #f0f0f0; }

.title-input {
  width: 100%; font-size: 34rpx; font-weight: 600;
  color: #1a1a1a; border: none; outline: none; height: 80rpx;
}

/* 分类 */
.cat-scroll { margin-top: 4rpx; }
.cat-list { display: flex; gap: 16rpx; flex-direction: row; padding: 4rpx 0; }
.cat-chip {
  display: inline-flex; align-items: center;
  padding: 12rpx 28rpx; border-radius: 32rpx;
  border: 1rpx solid #ddd; font-size: 26rpx; color: #666;
  white-space: nowrap;
}
.cat-chip.active { border-color: #1677ff; color: #1677ff; background: #e6f0ff; }

/* 正文 */
.content-textarea {
  width: 100%; min-height: 300rpx; font-size: 28rpx; color: #1a1a1a;
  border: none; outline: none; line-height: 1.8; box-sizing: border-box;
}
.char-count { display: block; text-align: right; font-size: 22rpx; color: #bbb; margin-top: 8rpx; }

/* 图片 */
.img-list { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 4rpx; }
.img-item { position: relative; width: 200rpx; height: 200rpx; border-radius: 12rpx; overflow: hidden; }
.img-preview { width: 100%; height: 100%; }
.img-del {
  position: absolute; top: 4rpx; right: 4rpx;
  width: 40rpx; height: 40rpx; border-radius: 50%;
  background: rgba(0,0,0,0.5); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 22rpx;
}
.img-add {
  width: 200rpx; height: 200rpx; border-radius: 12rpx;
  border: 2rpx dashed #ccc; display: flex;
  flex-direction: column; align-items: center; justify-content: center; gap: 8rpx;
}
.img-add-icon { font-size: 48rpx; }
.img-add-label { font-size: 24rpx; color: #999; }

/* 发布 */
.submit-wrap { padding: 40rpx 32rpx 0; }
.submit-btn {
  width: 100%; height: 88rpx; background: #1677ff; color: #fff;
  border: none; border-radius: 44rpx; font-size: 32rpx;
}
</style>
```

- [ ] **Step 2: 验证发帖完整流程**

在微信开发者工具中：
1. 点击 guide 页右下角 ✏️ 按钮 → 跳转发帖页
2. 填写标题、正文，选择分类，选择图片（会触发上传）
3. 点击「发布攻略」→ 显示"发布成功" → 自动返回
4. Feed 列表下拉刷新 → 新帖子出现在列表中

- [ ] **Step 3: 提交**

```bash
git add frontend/pages/guide/create/
git commit -m "feat: 发帖页（图文上传 + 分类）"
```

---

## Task 9: 更新「我的」页面

**Files:**
- Modify: `frontend/pages/mine/mine.vue`

**Interfaces:**
- Consumes: 无新接口；`/pages/guide/detail/detail` 和列表页已存在
- 改动：「我的收藏」菜单项跳转至真实收藏列表，新增「我的攻略」菜单项

- [ ] **Step 1: 替换 onCollect 和新增我的攻略入口**

在 `frontend/pages/mine/mine.vue` 中：

**① 替换 `onCollect` 函数：**
```typescript
function onCollect() {
  uni.navigateTo({ url: '/pages/guide/guide?tab=favorites' })
}
```

> 注意：这里跳转到 guide 页并传参 `tab=favorites`。但更简单的做法是直接在 guide.vue 的我的收藏 Tab 里显示。实际上，由于 guide 是 TabBar 页面需要用 `switchTab`，建议改为：
> ```typescript
> function onCollect() {
>   uni.navigateTo({ url: '/pages/mine/favorites' })
> }
> ```
> 但那需要新建页面。最简单的实现：直接用 uni.showToast 先跳转到 guide，让用户切换到收藏 Tab。**本计划采用以下方案**：将「我的攻略」和「我的收藏」直接作为列表页，各自复用一个轻量列表页面，而不是让 guide.vue 承担太多职责。

实际修改 `mine.vue`：将 `onCollect` 替换为真实实现，并在菜单中新增「我的攻略」：

在 template 的 `.menu-group` 最前面，**在已有的「我的收藏」之前**，插入「我的攻略」menu-item：

```html
<view class="menu-item" @click="onMyPosts">
  <text class="menu-label" :style="{ fontSize: rpx(28) }">我的攻略</text>
  <text class="menu-arrow">›</text>
</view>
<view class="divider" />
```

将已有的 `onCollect` 改为跳转实际收藏列表页：

```typescript
function onMyPosts() {
  uni.navigateTo({ url: '/pages/mine/my-posts/my-posts' })
}

function onCollect() {
  uni.navigateTo({ url: '/pages/mine/my-favorites/my-favorites' })
}
```

- [ ] **Step 2: 新建两个轻量列表页面**

新建 `frontend/pages/mine/my-posts/my-posts.vue`：

```vue
<!-- frontend/pages/mine/my-posts/my-posts.vue -->
<template>
  <view class="page">
    <scroll-view scroll-y class="list" @scrolltolower="loadMore">
      <view v-if="posts.length === 0 && !loading" class="empty">
        <text>还没有发布过攻略</text>
      </view>
      <view
        v-for="item in posts"
        :key="item.id"
        class="post-card"
        @click="goDetail(item.id)"
      >
        <text class="post-title">{{ item.title }}</text>
        <view class="post-meta">
          <text class="meta-text">{{ categoryMap[item.category] || item.category }}</text>
          <text class="meta-text">👍{{ item.likeCount }} 💬{{ item.commentCount }}</text>
        </view>
      </view>
      <view v-if="loading" class="tip"><text>加载中...</text></view>
      <view v-if="noMore && posts.length > 0" class="tip"><text>已经到底了</text></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchMyPosts, type PostItem } from '@/api/post'

const categoryMap: Record<string, string> = {
  SCENIC: '景点攻略', FOOD: '美食推荐', TRANSPORT: '交通住宿',
  FREE_TRAVEL: '自由行', FAMILY: '亲子游'
}

const posts = ref<PostItem[]>([])
const page = ref(0)
const loading = ref(false)
const noMore = ref(false)

onMounted(() => load(true))

async function load(reset = false) {
  if (loading.value || noMore.value) return
  loading.value = true
  try {
    const res = await fetchMyPosts({ page: page.value, size: 10 })
    if (res.code === 200) {
      const items = res.data.items
      posts.value = reset ? items : [...posts.value, ...items]
      if (posts.value.length >= res.data.total) noMore.value = true
      else page.value++
    }
  } finally { loading.value = false }
}

function loadMore() { load() }
function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/guide/detail/detail?id=${id}` })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; }
.list { height: 100vh; }
.empty { padding: 120rpx 0; text-align: center; font-size: 28rpx; color: #bbb; }
.post-card {
  background: #fff; margin: 16rpx 24rpx; padding: 28rpx 32rpx;
  border-radius: 16rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
}
.post-title { display: block; font-size: 30rpx; font-weight: 600; color: #1a1a1a; margin-bottom: 16rpx; }
.post-meta { display: flex; justify-content: space-between; }
.meta-text { font-size: 24rpx; color: #999; }
.tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #bbb; }
</style>
```

新建 `frontend/pages/mine/my-favorites/my-favorites.vue`，与 my-posts.vue 结构相同，仅改调用的函数：

```vue
<!-- frontend/pages/mine/my-favorites/my-favorites.vue -->
<template>
  <view class="page">
    <scroll-view scroll-y class="list" @scrolltolower="loadMore">
      <view v-if="posts.length === 0 && !loading" class="empty">
        <text>还没有收藏过攻略</text>
      </view>
      <view
        v-for="item in posts"
        :key="item.id"
        class="post-card"
        @click="goDetail(item.id)"
      >
        <image v-if="item.coverUrl" class="cover" :src="item.coverUrl" mode="aspectFill" />
        <text class="post-title">{{ item.title }}</text>
        <view class="post-meta">
          <text class="meta-text">{{ categoryMap[item.category] || item.category }}</text>
          <text class="meta-text">👍{{ item.likeCount }} 💬{{ item.commentCount }}</text>
        </view>
      </view>
      <view v-if="loading" class="tip"><text>加载中...</text></view>
      <view v-if="noMore && posts.length > 0" class="tip"><text>已经到底了</text></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchMyFavorites, type PostItem } from '@/api/post'

const categoryMap: Record<string, string> = {
  SCENIC: '景点攻略', FOOD: '美食推荐', TRANSPORT: '交通住宿',
  FREE_TRAVEL: '自由行', FAMILY: '亲子游'
}

const posts = ref<PostItem[]>([])
const page = ref(0)
const loading = ref(false)
const noMore = ref(false)

onMounted(() => load(true))

async function load(reset = false) {
  if (loading.value || noMore.value) return
  loading.value = true
  try {
    const res = await fetchMyFavorites({ page: page.value, size: 10 })
    if (res.code === 200) {
      const items = res.data.items
      posts.value = reset ? items : [...posts.value, ...items]
      if (posts.value.length >= res.data.total) noMore.value = true
      else page.value++
    }
  } finally { loading.value = false }
}

function loadMore() { load() }
function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/guide/detail/detail?id=${id}` })
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; }
.list { height: 100vh; }
.empty { padding: 120rpx 0; text-align: center; font-size: 28rpx; color: #bbb; }
.post-card {
  background: #fff; margin: 16rpx 24rpx; padding: 0;
  border-radius: 16rpx; overflow: hidden;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
}
.cover { width: 100%; height: 240rpx; display: block; }
.post-title { display: block; font-size: 30rpx; font-weight: 600; color: #1a1a1a; padding: 20rpx 24rpx 12rpx; }
.post-meta { display: flex; justify-content: space-between; padding: 0 24rpx 20rpx; }
.meta-text { font-size: 24rpx; color: #999; }
.tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #bbb; }
</style>
```

- [ ] **Step 3: 在 pages.json 注册两个新页面**

在 `frontend/pages.json` 的 `pages` 数组末尾追加：

```json
{
  "path": "pages/mine/my-posts/my-posts",
  "style": { "navigationBarTitleText": "我的攻略" }
},
{
  "path": "pages/mine/my-favorites/my-favorites",
  "style": { "navigationBarTitleText": "我的收藏" }
}
```

- [ ] **Step 4: 验证**

在「我的」页面点击「我的攻略」和「我的收藏」，分别跳转到对应列表页，点击帖子可进入详情。

- [ ] **Step 5: 提交**

```bash
git add frontend/pages/mine/ frontend/pages.json
git commit -m "feat: 我的攻略和我的收藏列表页"
```

---

## 验收清单

完成所有 Task 后，验证以下完整流程：

- [ ] 后端启动无报错，4 张新表（post / post_like / post_favorite / post_comment）自动创建
- [ ] `GET /api/posts` 无需 token 可访问，返回分页数据
- [ ] `POST /api/upload` 需要 token，上传成功返回可访问图片 URL
- [ ] 发帖 → Feed 出现新帖子 → 点击进入详情 → 图片正常显示
- [ ] 详情页点赞：❤️ / 🤍 切换，likeCount 实时变化
- [ ] 详情页收藏：⭐ / ☆ 切换，toast 提示
- [ ] 详情页评论：输入评论 → 发送 → 评论出现在列表顶部
- [ ] 分类筛选：选择「美食推荐」只显示 FOOD 类帖子
- [ ] 排序切换：热门 / 最新 切换后列表重新加载
- [ ] 「我的」页面 → 我的攻略 / 我的收藏 → 列表正确显示
