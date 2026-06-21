# TripMate 论坛扩展功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 在现有攻略论坛基础上，实现关注/粉丝系统、评论嵌套回复、消息通知/@用户、全文搜索、Admin 帖子审核共 5 大模块。

**Architecture:** Spring Boot 4 后端新增 UserFollow / Notification 实体；PostComment 追加 parentId 支持嵌套；Admin 面板新增帖子审核页；Mini-Program 新增通知页、搜索页，详情页支持关注 + 回复。

**Tech Stack:** Spring Boot 4 / JPA / MySQL 后端；UniApp Vue 3 前端；Vite + Element Plus Admin 面板

## Global Constraints

- 后端统一响应 `Result<T>` (`com.LHZ.TripMate.common.Result`)；枚举统一用字符串存储
- Mini-Program API 前缀 `/api/**`；Admin API 前缀 `/api/admin/**`
- `ddl-auto: update` 自动建表，无需手写 migration
- 所有需要登录的接口通过 JWT，`@AuthenticationPrincipal WxUserDetails` 获取当前用户
- `WxUserDetails.getUserId()` 返回 Long 类型 userId
- 前端 `useApi()` composable (`frontend/utils/useApi.ts`) 封装 `uni.request`，自动注入 Bearer token
- 前端页面必须注册到 `frontend/pages.json` 才能跳转
- Admin 面板新页面需在 `admin_frontend/src/router/index.ts` 注册路由，在 `AppLayout.vue` 加菜单项
- Spring Boot 4 Jackson 命名空间：`tools.jackson.*`（不是 `com.fasterxml.jackson.*`）
- 编译检查命令（后端）：`cd D:/code/作业/综合实践2/TripMate && ./mvnw compile -q`
- **所有 commit 必须使用中文或英文 commit message，不需要 Co-Authored-By 行**

---

### Task 1: 关注/粉丝 — 后端

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/entity/UserFollow.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/UserFollowRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/FollowStatsDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/service/FollowService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/FollowServiceImpl.java`
- Create: `src/main/java/com/LHZ/TripMate/controller/FollowController.java`
- Modify: `src/main/java/com/LHZ/TripMate/dto/PostDTO.java` — 在 `AuthorDTO` 中加 `boolean following` 字段
- Modify: `src/main/java/com/LHZ/TripMate/service/impl/PostServiceImpl.java` — `toDTO` 时填充 `following` 状态
- Modify: `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java` — 放行相关 GET 路径

**Interfaces:**
- Produces:
  - `POST /api/users/{userId}/follow` → `Result<Map<String,Object>>` `{following:boolean, followerCount:long}`
  - `GET /api/users/{userId}/stats` → `Result<FollowStatsDTO>` `{followerCount, followingCount, isFollowing}`
  - `PostDTO.AuthorDTO.following: boolean`

**Steps:**

- [ ] **Step 1: 创建 UserFollow 实体**

```java
// src/main/java/com/LHZ/TripMate/entity/UserFollow.java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "user_follow",
    uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id","following_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserFollow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "follower_id", nullable = false) private Long followerId;
    @Column(name = "following_id", nullable = false) private Long followingId;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 2: 创建 Repository**

```java
// src/main/java/com/LHZ/TripMate/repository/UserFollowRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    long countByFollowingId(Long followingId);  // 被关注者的粉丝数
    long countByFollowerId(Long followerId);    // 关注者的关注数
}
```

- [ ] **Step 3: 创建 FollowStatsDTO**

```java
// src/main/java/com/LHZ/TripMate/dto/FollowStatsDTO.java
package com.LHZ.TripMate.dto;

public record FollowStatsDTO(long followerCount, long followingCount, boolean isFollowing) {}
```

- [ ] **Step 4: 创建 Service 接口 + 实现**

```java
// src/main/java/com/LHZ/TripMate/service/FollowService.java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.FollowStatsDTO;
import java.util.Map;

public interface FollowService {
    Map<String, Object> toggleFollow(Long currentUserId, Long targetUserId);
    FollowStatsDTO getStats(Long targetUserId, Long currentUserId);
}
```

```java
// src/main/java/com/LHZ/TripMate/service/impl/FollowServiceImpl.java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.FollowStatsDTO;
import com.LHZ.TripMate.entity.UserFollow;
import com.LHZ.TripMate.repository.UserFollowRepository;
import com.LHZ.TripMate.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowRepository followRepo;

    @Override
    @Transactional
    public Map<String, Object> toggleFollow(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("不能关注自己");
        }
        Optional<UserFollow> existing = followRepo.findByFollowerIdAndFollowingId(currentUserId, targetUserId);
        boolean following;
        if (existing.isPresent()) {
            followRepo.delete(existing.get());
            following = false;
        } else {
            followRepo.save(UserFollow.builder()
                    .followerId(currentUserId).followingId(targetUserId).build());
            following = true;
        }
        long followerCount = followRepo.countByFollowingId(targetUserId);
        return Map.of("following", following, "followerCount", followerCount);
    }

    @Override
    public FollowStatsDTO getStats(Long targetUserId, Long currentUserId) {
        long followerCount = followRepo.countByFollowingId(targetUserId);
        long followingCount = followRepo.countByFollowerId(targetUserId);
        boolean isFollowing = currentUserId != null &&
                followRepo.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        return new FollowStatsDTO(followerCount, followingCount, isFollowing);
    }
}
```

- [ ] **Step 5: 创建 FollowController**

```java
// src/main/java/com/LHZ/TripMate/controller/FollowController.java
package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.FollowStatsDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}/follow")
    public Result<?> toggleFollow(@PathVariable Long userId,
                                  @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(followService.toggleFollow(userDetails.getUserId(), userId));
    }

    @GetMapping("/{userId}/stats")
    public Result<FollowStatsDTO> getStats(@PathVariable Long userId,
                                           @AuthenticationPrincipal(errorOnInvalidType = false)
                                           WxUserDetails userDetails) {
        Long currentId = userDetails != null ? userDetails.getUserId() : null;
        return Result.success(followService.getStats(userId, currentId));
    }
}
```

- [ ] **Step 6: 修改 PostDTO.AuthorDTO，加入 following 字段**

读取 `src/main/java/com/LHZ/TripMate/dto/PostDTO.java`，在 `AuthorDTO` builder 类中加入：
```java
private boolean following;
```

- [ ] **Step 7: 修改 PostServiceImpl.toDTO，填充 following**

在 `toDTO` 方法中，构建 `AuthorDTO` 时：
```java
boolean following = currentUserId != null && followRepo.existsByFollowerIdAndFollowingId(currentUserId, post.getUserId());
// AuthorDTO.builder()...following(following).build()
```
需要注入 `UserFollowRepository followRepo`。

- [ ] **Step 8: SecurityConfig 放行 GET /api/users/{id}/stats**

在 SecurityConfig 中，找到 `authorizeHttpRequests` 链，在合适位置加：
```java
.requestMatchers(HttpMethod.GET, "/api/users/*/stats").permitAll()
```
`POST /api/users/*/follow` 保持需要认证（走 `/api/**` authenticated 兜底规则即可）。

- [ ] **Step 9: 编译验证**

```bash
cd D:/code/作业/综合实践2/TripMate && ./mvnw compile -q
```
预期：无输出（BUILD SUCCESS）

- [ ] **Step 10: Commit**

```bash
git add src/
git commit -m "feat: 关注/粉丝系统后端"
```

---

### Task 2: 关注/粉丝 — 前端

**Files:**
- Create: `frontend/api/follow.ts`
- Modify: `frontend/pages/guide/detail/detail.vue` — 作者行加关注按钮，点击后切换状态

**Interfaces:**
- Consumes: `POST /api/users/{userId}/follow` → `{following, followerCount}`；`GET /api/users/{userId}/stats` → `{followerCount, followingCount, isFollowing}`
- Note: detail.vue 通过 getCurrentPages()[0].options.id 获取帖子 id；帖子 DTO 中 `post.author.id`、`post.author.following` 已有

**Steps:**

- [ ] **Step 1: 创建 follow.ts API 封装**

```typescript
// frontend/api/follow.ts
import { useApi } from '@/utils/useApi'

const { post: apiPost, get } = useApi()

export interface FollowStats {
  followerCount: number
  followingCount: number
  isFollowing: boolean
}

export function toggleFollow(userId: number) {
  return apiPost<{ following: boolean; followerCount: number }>(`/api/users/${userId}/follow`)
}

export function fetchFollowStats(userId: number) {
  return get<FollowStats>(`/api/users/${userId}/stats`)
}
```

- [ ] **Step 2: 修改 detail.vue — 作者区加关注按钮**

读取 `frontend/pages/guide/detail/detail.vue`，找到作者信息区域（author-row 类或类似位置），在昵称旁边加关注按钮：

在 `<script setup>` 中加：
```typescript
import { toggleFollow } from '@/api/follow'
import { useAuth } from '@/composables/useAuth'

const { authState } = useAuth()
const following = ref(false)

// 在 onLoad 加载帖子详情后，从 post.author.following 初始化：
// following.value = post.value?.author?.following ?? false
```

在模板作者区加：
```html
<view
  v-if="authState.isLoggedIn && post?.author?.id !== authState.userId"
  class="follow-btn"
  :class="{ followed: following }"
  @click="onFollow"
>{{ following ? '已关注' : '+ 关注' }}</view>
```

加 `onFollow` 方法：
```typescript
async function onFollow() {
  if (!post.value?.author?.id) return
  const res = await toggleFollow(post.value.author.id)
  if (res.code === 200) {
    following.value = res.data.following
  }
}
```

加样式：
```css
.follow-btn {
  padding: 8rpx 24rpx; border-radius: 32rpx;
  border: 1rpx solid #1677ff; color: #1677ff; font-size: 22rpx;
}
.follow-btn.followed { background: #1677ff; color: #fff; border-color: #1677ff; }
```

在 `loadDetail` 函数（加载帖子详情）成功后加：
```typescript
following.value = res.data.author?.following ?? false
```

- [ ] **Step 3: Commit**

```bash
git add frontend/
git commit -m "feat: 关注/粉丝系统前端"
```

---

### Task 3: 评论嵌套回复（后端 + 前端）

**Files:**
- Modify: `src/main/java/com/LHZ/TripMate/entity/PostComment.java` — 加 parentId 字段
- Modify: `src/main/java/com/LHZ/TripMate/repository/PostCommentRepository.java` — 加 findByPostIdAndParentIdIsNull / findByParentId
- Modify: `src/main/java/com/LHZ/TripMate/dto/PostCommentDTO.java` — 加 replies 列表
- Create: `src/main/java/com/LHZ/TripMate/dto/CommentCreateDTO.java` — 加 parentId 字段（如不存在则新建，如已存在则修改）
- Modify: `src/main/java/com/LHZ/TripMate/service/impl/PostServiceImpl.java` — getComments 返回嵌套结构
- Modify: `frontend/pages/guide/detail/detail.vue` — 评论展示嵌套结构，加回复按钮

**Interfaces:**
- Produces:
  - `GET /api/posts/{id}/comments` 返回：`PageResult<PostCommentDTO>`，`PostCommentDTO.replies` 为 `List<PostCommentDTO>`（仅一级）
  - `POST /api/posts/{id}/comments` body 加 `parentId?: number`

**Steps:**

- [ ] **Step 1: 修改 PostComment 实体，加 parentId**

读取 `src/main/java/com/LHZ/TripMate/entity/PostComment.java`，加字段：
```java
@Column(name = "parent_id")
private Long parentId;   // null = 顶层评论
```

- [ ] **Step 2: 修改 PostCommentRepository**

读取并加方法：
```java
List<PostComment> findByPostIdAndParentIdIsNull(Long postId, org.springframework.data.domain.Sort sort);
List<PostComment> findByParentId(Long parentId, org.springframework.data.domain.Sort sort);
```
同时保留原有的 `findByPostId(Long, Pageable)` 用于统计。

- [ ] **Step 3: 修改 PostCommentDTO，加 replies 字段**

读取 `src/main/java/com/LHZ/TripMate/dto/PostCommentDTO.java`，加：
```java
private List<PostCommentDTO> replies;
```
若是 record，改为 class + builder。注意：replies 只有一层，reply 的 replies 设为 empty list。

- [ ] **Step 4: 确认/修改 CommentCreateDTO 包含 parentId**

读取或创建 `src/main/java/com/LHZ/TripMate/dto/CommentCreateDTO.java`：
```java
package com.LHZ.TripMate.dto;

public record CommentCreateDTO(String content, Long parentId) {}
```

- [ ] **Step 5: 修改 PostServiceImpl.getComments — 嵌套组装**

在 `getComments` 方法中：
1. 查出帖子所有顶层评论（parentId IS NULL），按 createdAt ASC
2. 对每个顶层评论，查出其子评论（parentId = comment.id），按 createdAt ASC
3. 组装成 `PostCommentDTO`，replies 填入子评论列表
4. 分页逻辑改为：先取全部顶层评论数量作为 total，再按 page/size 分页顶层评论，每条带全部 replies（replies 不分页，通常数量少）

示例（伪代码）：
```java
Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
List<PostComment> roots = commentRepo.findByPostIdAndParentIdIsNull(postId, sort);
long total = roots.size();
// 手动分页 roots
int fromIdx = page * size;
int toIdx = Math.min(fromIdx + size, roots.size());
List<PostComment> pageRoots = roots.subList(fromIdx, toIdx);
List<PostCommentDTO> items = pageRoots.stream().map(root -> {
    List<PostComment> children = commentRepo.findByParentId(root.getId(), sort);
    List<PostCommentDTO> replyDTOs = children.stream().map(c -> toCommentDTO(c, List.of())).toList();
    return toCommentDTO(root, replyDTOs);
}).toList();
return new PageResult<>(items, total, page, size);
```

`toCommentDTO(PostComment c, List<PostCommentDTO> replies)` 方法组装 DTO。

在 `createComment` 方法中，从 `CommentCreateDTO` 读取 `parentId` 并设到实体。

- [ ] **Step 6: 编译验证**

```bash
cd D:/code/作业/综合实践2/TripMate && ./mvnw compile -q
```

- [ ] **Step 7: 修改 detail.vue — 评论展示回复**

读取 `frontend/pages/guide/detail/detail.vue`。

在 `CommentItem` 接口中（frontend/api/post.ts）加 `replies?: CommentItem[]`。

在 detail.vue 的评论列表模板中，每条评论下方展示 replies：
```html
<!-- 子评论 replies -->
<view v-for="reply in item.replies" :key="reply.id" class="reply-item">
  <text class="reply-author">{{ reply.author?.nickname || '旅行者' }}：</text>
  <text class="reply-content">{{ reply.content }}</text>
</view>
<!-- 回复按钮 -->
<text class="reply-btn" @click="onReply(item)">回复</text>
```

在脚本中加：
```typescript
const replyingTo = ref<CommentItem | null>(null)

function onReply(comment: CommentItem) {
  replyingTo.value = comment
  // 打开评论弹窗，pre-fill placeholder
  showComment.value = true
}
```

在 `submitComment` 中，发送时带 `parentId`：
```typescript
const res = await createComment(post.value!.id, commentText.value, replyingTo.value?.id)
// 发完后清空 replyingTo
replyingTo.value = null
```

`createComment` API 签名更新为：
```typescript
export function createComment(postId: number, content: string, parentId?: number) {
  return post<CommentItem>(`/api/posts/${postId}/comments`, { content, parentId })
}
```

评论弹窗 placeholder 动态：
```html
:placeholder="replyingTo ? `回复 ${replyingTo.author?.nickname || '旅行者'}...` : '说说你的想法（500字内）'"
```

加样式：
```css
.reply-item { padding: 8rpx 0 8rpx 48rpx; display: flex; flex-direction: row; flex-wrap: wrap; }
.reply-author { font-size: 24rpx; color: #1677ff; font-weight: 500; }
.reply-content { font-size: 24rpx; color: #333; }
.reply-btn { font-size: 22rpx; color: #999; margin-left: 16rpx; margin-top: 8rpx; }
```

- [ ] **Step 8: Commit**

```bash
git add src/ frontend/
git commit -m "feat: 评论嵌套回复（后端+前端）"
```

---

### Task 4: 消息通知 — 后端

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/entity/Notification.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/NotificationRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/NotificationDTO.java`
- Create: `src/main/java/com/LHZ/TripMate/service/NotificationService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/NotificationServiceImpl.java`
- Create: `src/main/java/com/LHZ/TripMate/controller/NotificationController.java`
- Modify: `src/main/java/com/LHZ/TripMate/service/impl/PostServiceImpl.java` — 点赞、评论时创建通知
- Modify: `src/main/java/com/LHZ/TripMate/service/impl/FollowServiceImpl.java` — 关注时创建通知
- Modify: `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java` — 放行通知接口

**Interfaces:**
- Produces:
  - `GET /api/notifications?page=0&size=20` → `Result<PageResult<NotificationDTO>>`
  - `GET /api/notifications/unread-count` → `Result<Map<String,Long>>` `{count: N}`
  - `POST /api/notifications/read-all` → `Result<Void>`
- NotificationDTO: `{id, type, fromUser{id,nickname,avatarUrl}, postId, postTitle, commentContent, isRead, createdAt}`
- NotificationType enum (STRING): `LIKE_POST`, `COMMENT_POST`, `NEW_FOLLOWER`, `MENTION_COMMENT`

**Steps:**

- [ ] **Step 1: 创建 Notification 实体**

```java
// src/main/java/com/LHZ/TripMate/entity/Notification.java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "notification",
    indexes = @Index(columnList = "to_user_id,is_read,created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    public enum Type { LIKE_POST, COMMENT_POST, NEW_FOLLOWER, MENTION_COMMENT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private Type type;

    @Column(name = "from_user_id") private Long fromUserId;
    @Column(name = "to_user_id", nullable = false) private Long toUserId;
    @Column(name = "post_id") private Long postId;
    @Column(name = "post_title") private String postTitle;
    @Column(name = "comment_content", length = 200) private String commentContent;

    @Column(name = "is_read", nullable = false) private boolean read = false;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt = LocalDateTime.now();
}
```

- [ ] **Step 2: 创建 NotificationRepository**

```java
// src/main/java/com/LHZ/TripMate/repository/NotificationRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByToUserIdOrderByCreatedAtDesc(Long toUserId, Pageable pageable);
    long countByToUserIdAndReadFalse(Long toUserId);
    @Modifying @Query("UPDATE Notification n SET n.read = true WHERE n.toUserId = :userId")
    void markAllReadByToUserId(Long userId);
}
```

- [ ] **Step 3: 创建 NotificationDTO**

```java
// src/main/java/com/LHZ/TripMate/dto/NotificationDTO.java
package com.LHZ.TripMate.dto;

import com.LHZ.TripMate.entity.Notification;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data @Builder
public class NotificationDTO {
    private Long id;
    private Notification.Type type;
    private PostDTO.AuthorDTO fromUser;
    private Long postId;
    private String postTitle;
    private String commentContent;
    private boolean read;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 4: 创建 NotificationService 接口**

```java
// src/main/java/com/LHZ/TripMate/service/NotificationService.java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.NotificationDTO;
import com.LHZ.TripMate.dto.PageResult;
import com.LHZ.TripMate.entity.Notification;

public interface NotificationService {
    void create(Notification.Type type, Long fromUserId, Long toUserId,
                Long postId, String postTitle, String commentContent);
    PageResult<NotificationDTO> list(Long userId, int page, int size);
    long unreadCount(Long userId);
    void markAllRead(Long userId);
}
```

- [ ] **Step 5: 创建 NotificationServiceImpl**

```java
// src/main/java/com/LHZ/TripMate/service/impl/NotificationServiceImpl.java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.NotificationDTO;
import com.LHZ.TripMate.dto.PageResult;
import com.LHZ.TripMate.dto.PostDTO;
import com.LHZ.TripMate.entity.Notification;
import com.LHZ.TripMate.entity.WxUser;
import com.LHZ.TripMate.repository.NotificationRepository;
import com.LHZ.TripMate.repository.WxUserRepository;
import com.LHZ.TripMate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notifRepo;
    private final WxUserRepository wxUserRepo;

    @Override
    public void create(Notification.Type type, Long fromUserId, Long toUserId,
                       Long postId, String postTitle, String commentContent) {
        if (fromUserId != null && fromUserId.equals(toUserId)) return; // 不给自己发通知
        notifRepo.save(Notification.builder()
                .type(type).fromUserId(fromUserId).toUserId(toUserId)
                .postId(postId).postTitle(postTitle).commentContent(commentContent)
                .build());
    }

    @Override
    public PageResult<NotificationDTO> list(Long userId, int page, int size) {
        Page<Notification> pg = notifRepo.findByToUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        var items = pg.getContent().stream().map(this::toDTO).toList();
        return new PageResult<>(items, pg.getTotalElements(), page, size);
    }

    @Override
    public long unreadCount(Long userId) {
        return notifRepo.countByToUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notifRepo.markAllReadByToUserId(userId);
    }

    private NotificationDTO toDTO(Notification n) {
        PostDTO.AuthorDTO fromUser = null;
        if (n.getFromUserId() != null) {
            WxUser u = wxUserRepo.findById(n.getFromUserId()).orElse(null);
            if (u != null) {
                fromUser = PostDTO.AuthorDTO.builder()
                        .id(u.getId()).nickname(u.getNickname()).avatarUrl(u.getAvatarUrl())
                        .build();
            }
        }
        return NotificationDTO.builder()
                .id(n.getId()).type(n.getType()).fromUser(fromUser)
                .postId(n.getPostId()).postTitle(n.getPostTitle())
                .commentContent(n.getCommentContent())
                .read(n.isRead()).createdAt(n.getCreatedAt())
                .build();
    }
}
```

- [ ] **Step 6: 创建 NotificationController**

```java
// src/main/java/com/LHZ/TripMate/controller/NotificationController.java
package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.NotificationDTO;
import com.LHZ.TripMate.dto.PageResult;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;

    @GetMapping
    public Result<PageResult<NotificationDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(notifService.list(userDetails.getUserId(), page, size));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(Map.of("count", notifService.unreadCount(userDetails.getUserId())));
    }

    @PostMapping("/read-all")
    public Result<Void> readAll(@AuthenticationPrincipal WxUserDetails userDetails) {
        notifService.markAllRead(userDetails.getUserId());
        return Result.success(null);
    }
}
```

- [ ] **Step 7: 在 PostServiceImpl 点赞和评论时触发通知**

在 `PostServiceImpl` 中注入 `NotificationService notifService`（构造器注入）。

在 `toggleLike` 成功点赞（liked=true）分支中（保存 PostLike 之后）加：
```java
// 通知帖子作者：有人点赞
notifService.create(Notification.Type.LIKE_POST, currentUserId, post.getUserId(),
        post.getId(), post.getTitle(), null);
```

在 `createComment` 保存评论后加：
```java
// 通知帖子作者：有人评论（如非作者自己）
notifService.create(Notification.Type.COMMENT_POST, currentUserId, post.getUserId(),
        post.getId(), post.getTitle(), saved.getContent());
// @mention 检测：扫描 content 中 @nickname 格式（简单实现，略过，此处不做）
```

- [ ] **Step 8: 在 FollowServiceImpl.toggleFollow 中触发关注通知**

注入 `NotificationService notifService`，在 `following = true` 分支中加：
```java
notifService.create(Notification.Type.NEW_FOLLOWER, currentUserId, targetUserId,
        null, null, null);
```

- [ ] **Step 9: SecurityConfig 放行通知接口**

所有 `/api/notifications/**` 均需认证，走现有 `/api/**` authenticated 兜底规则，无需额外配置。

- [ ] **Step 10: 编译验证**

```bash
cd D:/code/作业/综合实践2/TripMate && ./mvnw compile -q
```

- [ ] **Step 11: Commit**

```bash
git add src/
git commit -m "feat: 消息通知系统后端"
```

---

### Task 5: 消息通知 — 前端

**Files:**
- Create: `frontend/api/notification.ts`
- Create: `frontend/pages/notifications/notifications.vue`
- Modify: `frontend/pages.json` — 注册通知页面
- Modify: `frontend/components/TabBar/TabBar.vue` — 可选：加通知入口（或在首页 mine 入口）
- Modify: `frontend/pages/mine/mine.vue` — 加"消息通知"入口菜单项

**Interfaces:**
- Consumes: `GET /api/notifications`, `GET /api/notifications/unread-count`, `POST /api/notifications/read-all`

**Steps:**

- [ ] **Step 1: 创建 notification.ts**

```typescript
// frontend/api/notification.ts
import { useApi } from '@/utils/useApi'
import type { PageResult } from '@/api/post'
import type { PostDTO } from '@/api/post'

const { get, post } = useApi()

export interface NotificationItem {
  id: number
  type: 'LIKE_POST' | 'COMMENT_POST' | 'NEW_FOLLOWER' | 'MENTION_COMMENT'
  fromUser?: { id: number; nickname: string; avatarUrl?: string }
  postId?: number
  postTitle?: string
  commentContent?: string
  read: boolean
  createdAt: string
}

export function fetchNotifications(page = 0, size = 20) {
  return get<PageResult<NotificationItem>>(`/api/notifications?page=${page}&size=${size}`)
}

export function fetchUnreadCount() {
  return get<{ count: number }>('/api/notifications/unread-count')
}

export function markAllRead() {
  return post<void>('/api/notifications/read-all')
}
```

- [ ] **Step 2: 创建 notifications.vue**

```vue
<!-- frontend/pages/notifications/notifications.vue -->
<template>
  <view class="page">
    <view v-if="list.length === 0 && !loading" class="empty">
      <text class="empty-text">暂无消息</text>
    </view>
    <view v-for="item in list" :key="item.id"
          class="notif-item" :class="{ unread: !item.read }"
          @click="onTap(item)">
      <view class="avatar-wrap">
        <image v-if="item.fromUser?.avatarUrl" class="avatar" :src="item.fromUser.avatarUrl" mode="aspectFill" />
        <view v-else class="avatar avatar-placeholder" />
        <view class="type-badge">{{ typeBadge(item.type) }}</view>
      </view>
      <view class="content">
        <text class="actor">{{ item.fromUser?.nickname || '旅行者' }}</text>
        <text class="action-text">{{ actionText(item) }}</text>
        <text v-if="item.commentContent" class="comment-preview">{{ item.commentContent }}</text>
        <text class="time">{{ formatTime(item.createdAt) }}</text>
      </view>
      <view v-if="!item.read" class="unread-dot" />
    </view>
    <view v-if="loading" class="loading-tip"><text>加载中...</text></view>
    <view v-if="noMore && list.length > 0" class="loading-tip"><text>没有更多了</text></view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchNotifications, markAllRead, type NotificationItem } from '@/api/notification'

const list = ref<NotificationItem[]>([])
const page = ref(0)
const loading = ref(false)
const noMore = ref(false)

onMounted(async () => {
  await load()
  await markAllRead()
})

async function load(reset = false) {
  if (loading.value || noMore.value) return
  loading.value = true
  if (reset) { page.value = 0; noMore.value = false }
  try {
    const res = await fetchNotifications(page.value, 20)
    if (res.code === 200) {
      const items = res.data.items
      list.value = reset ? items : [...list.value, ...items]
      if (list.value.length >= res.data.total) noMore.value = true
      else page.value++
    }
  } finally { loading.value = false }
}

function typeBadge(type: string) {
  const map: Record<string, string> = {
    LIKE_POST: '👍', COMMENT_POST: '💬', NEW_FOLLOWER: '👤', MENTION_COMMENT: '@'
  }
  return map[type] || '🔔'
}

function actionText(item: NotificationItem) {
  switch (item.type) {
    case 'LIKE_POST': return `赞了你的攻略《${item.postTitle || ''}》`
    case 'COMMENT_POST': return `评论了你的攻略《${item.postTitle || ''}》`
    case 'NEW_FOLLOWER': return '关注了你'
    case 'MENTION_COMMENT': return `在评论中提到了你`
    default: return ''
  }
}

function formatTime(t: string) {
  const d = new Date(t)
  const now = new Date()
  const diff = (now.getTime() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  return `${Math.floor(diff / 86400)}天前`
}

function onTap(item: NotificationItem) {
  if (item.postId) {
    uni.navigateTo({ url: `/pages/guide/detail/detail?id=${item.postId}` })
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; }
.empty { padding: 120rpx 0; text-align: center; }
.empty-text { font-size: 28rpx; color: #bbb; }
.notif-item {
  display: flex; align-items: flex-start; gap: 24rpx;
  padding: 28rpx 32rpx; background: #fff;
  border-bottom: 1rpx solid #f0f0f0; position: relative;
}
.notif-item.unread { background: #f0f6ff; }
.avatar-wrap { position: relative; flex-shrink: 0; }
.avatar { width: 80rpx; height: 80rpx; border-radius: 50%; }
.avatar-placeholder { width: 80rpx; height: 80rpx; border-radius: 50%; background: #e0e0e0; }
.type-badge {
  position: absolute; bottom: -4rpx; right: -4rpx;
  width: 32rpx; height: 32rpx; border-radius: 50%;
  background: #1677ff; display: flex; align-items: center; justify-content: center;
  font-size: 18rpx;
}
.content { flex: 1; }
.actor { font-size: 28rpx; font-weight: 600; color: #1a1a1a; display: block; margin-bottom: 4rpx; }
.action-text { font-size: 26rpx; color: #555; display: block; }
.comment-preview {
  display: block; font-size: 24rpx; color: #999;
  margin-top: 8rpx; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical;
}
.time { display: block; font-size: 22rpx; color: #bbb; margin-top: 8rpx; }
.unread-dot {
  position: absolute; top: 28rpx; right: 24rpx;
  width: 16rpx; height: 16rpx; border-radius: 50%; background: #ff4d4f;
}
.loading-tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #bbb; }
</style>
```

- [ ] **Step 3: 注册页面到 pages.json**

读取 `frontend/pages.json`，在 pages 数组末尾加：
```json
{
  "path": "pages/notifications/notifications",
  "style": {
    "navigationBarTitleText": "消息通知",
    "navigationBarBackgroundColor": "#ffffff",
    "navigationBarTextStyle": "black"
  }
}
```

- [ ] **Step 4: 在 mine.vue 加消息通知入口**

读取 `frontend/pages/mine/mine.vue`，在现有菜单列表顶部加"消息通知"菜单项，点击跳转 `/pages/notifications/notifications`：
```html
<view class="menu-item" @click="uni.navigateTo({ url: '/pages/notifications/notifications' })">
  <text class="menu-label">消息通知</text>
  <text class="menu-arrow">›</text>
</view>
```

- [ ] **Step 5: Commit**

```bash
git add frontend/
git commit -m "feat: 消息通知前端"
```

---

### Task 6: 搜索功能（后端 + 前端）

**Files:**
- Modify: `src/main/java/com/LHZ/TripMate/repository/PostRepository.java` — 加搜索方法
- Modify: `src/main/java/com/LHZ/TripMate/service/PostService.java` — 加 search 方法
- Modify: `src/main/java/com/LHZ/TripMate/service/impl/PostServiceImpl.java` — 实现 search
- Modify: `src/main/java/com/LHZ/TripMate/controller/PostController.java` — 加 GET /api/posts/search
- Modify: `src/main/java/com/LHZ/TripMate/config/SecurityConfig.java` — 放行搜索
- Create: `frontend/pages/guide/search/search.vue`
- Modify: `frontend/pages.json` — 注册搜索页
- Modify: `frontend/api/post.ts` — 加 searchPosts 函数
- Modify: `frontend/pages/guide/guide.vue` — 加搜索入口图标

**Interfaces:**
- Produces: `GET /api/posts/search?q=关键词&page=0&size=10` → `Result<PageResult<PostDTO>>`
- Search logic: `status = 'PUBLISHED' AND (title LIKE %q% OR content LIKE %q%)`，按 createdAt DESC

**Steps:**

- [ ] **Step 1: PostRepository 加搜索查询**

```java
// 添加到 PostRepository
@Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' AND " +
       "(LOWER(p.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        "LOWER(p.content) LIKE LOWER(CONCAT('%', :q, '%'))) " +
       "ORDER BY p.createdAt DESC")
Page<Post> search(@Param("q") String q, Pageable pageable);
```

- [ ] **Step 2: PostService 接口加 search 方法**

```java
PageResult<PostDTO> search(String q, int page, int size, Long currentUserId);
```

- [ ] **Step 3: PostServiceImpl 实现 search**

```java
@Override
public PageResult<PostDTO> search(String q, int page, int size, Long currentUserId) {
    if (q == null || q.isBlank()) return new PageResult<>(List.of(), 0, page, size);
    Page<Post> pg = postRepo.search(q.trim(), PageRequest.of(page, size));
    var items = pg.getContent().stream().map(p -> toDTO(p, currentUserId)).toList();
    return new PageResult<>(items, pg.getTotalElements(), page, size);
}
```

- [ ] **Step 4: PostController 加搜索端点**

```java
@GetMapping("/search")
public Result<PageResult<PostDTO>> search(
        @RequestParam String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal(errorOnInvalidType = false) WxUserDetails userDetails) {
    Long uid = userDetails != null ? userDetails.getUserId() : null;
    return Result.success(postService.search(q, page, size, uid));
}
```

注意：在 SecurityConfig 中，`GET /api/posts/search` 需要在 `GET /api/posts/**` permitAll 规则之前，或者它本身已经被 `GET /api/posts/**` 覆盖到（因为 search 只需要 permitAll，与现有规则一致，无需额外配置）。

- [ ] **Step 5: SecurityConfig 确认 /api/posts/search 已 permitAll**

检查 SecurityConfig 中 `GET /api/posts/**` 是否已 permitAll。若是，`/api/posts/search` 也被覆盖，无需改动。

- [ ] **Step 6: 编译验证**

```bash
cd D:/code/作业/综合实践2/TripMate && ./mvnw compile -q
```

- [ ] **Step 7: 创建 search.vue**

```vue
<!-- frontend/pages/guide/search/search.vue -->
<template>
  <view class="page">
    <view class="search-bar">
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索攻略标题或内容..."
        confirm-type="search"
        @confirm="doSearch"
        :focus="true"
      />
      <text class="search-btn" @click="doSearch">搜索</text>
    </view>

    <view v-if="results.length === 0 && searched && !loading" class="empty">
      <text class="empty-text">未找到相关攻略</text>
    </view>

    <view v-for="item in results" :key="item.id" class="post-card"
          @click="uni.navigateTo({ url: `/pages/guide/detail/detail?id=${item.id}` })">
      <image v-if="item.coverUrl" class="cover" :src="item.coverUrl" mode="aspectFill" />
      <view v-else class="cover cover-placeholder" />
      <view class="card-body">
        <text class="post-title">{{ item.title }}</text>
        <text class="post-snippet">{{ item.content?.slice(0, 60) }}...</text>
        <view class="card-meta">
          <text class="author-name">{{ item.author?.nickname || '旅行者' }}</text>
          <view class="stats-row">
            <text class="stat">👍 {{ item.likeCount }}</text>
            <text class="stat">💬 {{ item.commentCount }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-if="!noMore && results.length > 0" class="load-more" @click="loadMore">
      <text>加载更多</text>
    </view>
    <view v-if="noMore && results.length > 0" class="loading-tip"><text>已经到底了</text></view>
    <view v-if="loading" class="loading-tip"><text>搜索中...</text></view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { searchPosts, type PostItem } from '@/api/post'

const keyword = ref('')
const results = ref<PostItem[]>([])
const page = ref(0)
const loading = ref(false)
const noMore = ref(false)
const searched = ref(false)

async function doSearch() {
  if (!keyword.value.trim()) return
  page.value = 0
  noMore.value = false
  results.value = []
  searched.value = true
  await load()
}

async function loadMore() {
  if (!noMore.value) await load()
}

async function load() {
  if (loading.value || noMore.value) return
  loading.value = true
  try {
    const res = await searchPosts(keyword.value.trim(), page.value, 10)
    if (res.code === 200) {
      results.value = page.value === 0 ? res.data.items : [...results.value, ...res.data.items]
      if (results.value.length >= res.data.total) noMore.value = true
      else page.value++
    }
  } finally { loading.value = false }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f7f8fa; }
.search-bar {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 24rpx; background: #fff;
  border-bottom: 1rpx solid #eee;
}
.search-input {
  flex: 1; height: 68rpx; background: #f5f5f5; border-radius: 34rpx;
  padding: 0 28rpx; font-size: 28rpx; color: #333;
}
.search-btn { font-size: 28rpx; color: #1677ff; white-space: nowrap; }
.empty { padding: 120rpx 0; text-align: center; }
.empty-text { font-size: 28rpx; color: #bbb; }
.post-card {
  background: #fff; margin: 0 24rpx 20rpx; margin-top: 20rpx;
  border-radius: 20rpx; overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.06);
}
.cover { width: 100%; height: 200rpx; display: block; }
.cover-placeholder { width: 100%; height: 120rpx; background: linear-gradient(135deg,#e0f0ff,#b3d9ff); }
.card-body { padding: 20rpx 24rpx; }
.post-title { font-size: 30rpx; font-weight: 600; color: #1a1a1a; display: block; margin-bottom: 8rpx; }
.post-snippet { font-size: 24rpx; color: #888; display: block; margin-bottom: 12rpx; }
.card-meta { display: flex; justify-content: space-between; align-items: center; }
.author-name { font-size: 24rpx; color: #666; }
.stats-row { display: flex; gap: 16rpx; }
.stat { font-size: 24rpx; color: #999; }
.load-more { text-align: center; padding: 24rpx; font-size: 26rpx; color: #1677ff; }
.loading-tip { text-align: center; padding: 24rpx; font-size: 24rpx; color: #bbb; }
</style>
```

- [ ] **Step 8: post.ts 加 searchPosts**

在 `frontend/api/post.ts` 末尾加：
```typescript
export function searchPosts(q: string, page = 0, size = 10) {
  return get<PageResult<PostItem>>(`/api/posts/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`)
}
```
注意：需要将 `get` 从 `useApi()` 解构出来（检查该文件是否已有 `const { get, ... } = useApi()`）。

- [ ] **Step 9: 注册搜索页 + guide.vue 加搜索入口**

在 `frontend/pages.json` 注册：
```json
{
  "path": "pages/guide/search/search",
  "style": {
    "navigationBarTitleText": "搜索攻略",
    "navigationBarBackgroundColor": "#ffffff",
    "navigationBarTextStyle": "black"
  }
}
```

在 `guide.vue` 模板顶部（分类条之前）加搜索按钮，或在现有导航栏右侧加搜索 icon。由于 UniApp 不能直接修改导航栏，在分类条 `<scroll-view>` 右边加一个🔍点击跳转：
```html
<view class="top-bar">
  <scroll-view class="category-bar" scroll-x>...</scroll-view>
  <view class="search-icon" @click="uni.navigateTo({ url: '/pages/guide/search/search' })">
    <text class="search-icon-text">🔍</text>
  </view>
</view>
```
对应样式：
```css
.top-bar { display: flex; align-items: center; background: #fff; border-bottom: 1rpx solid #eee; }
.category-bar { flex: 1; }
.search-icon { padding: 0 24rpx; flex-shrink: 0; }
.search-icon-text { font-size: 40rpx; }
```

- [ ] **Step 10: Commit**

```bash
git add src/ frontend/
git commit -m "feat: 攻略搜索功能（后端+前端）"
```

---

### Task 7: Admin 帖子审核

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/controller/admin/AdminPostController.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/admin/AdminPostDTO.java`
- Create: `admin_frontend/src/api/posts.ts`
- Create: `admin_frontend/src/views/posts/PostsView.vue`
- Modify: `admin_frontend/src/router/index.ts` — 加 /posts 路由（requiresSuperAdmin: true）
- Modify: `admin_frontend/src/components/AppLayout.vue` — 加"帖子管理"菜单项

**Interfaces:**
- Backend:
  - `GET /api/admin/posts?page=0&size=20&status=PUBLISHED` → `Result<PageResult<AdminPostDTO>>`
  - `POST /api/admin/posts/{id}/delete` → `Result<Void>`（软删除，status=DELETED）
  - `POST /api/admin/posts/{id}/restore` → `Result<Void>`（恢复，status=PUBLISHED）

**Steps:**

- [ ] **Step 1: 创建 AdminPostDTO**

```java
// src/main/java/com/LHZ/TripMate/dto/admin/AdminPostDTO.java
package com.LHZ.TripMate.dto.admin;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class AdminPostDTO {
    private Long id;
    private String title;
    private String content;    // 截取前 200 字
    private String category;
    private String coverUrl;
    private int likeCount;
    private int commentCount;
    private int viewCount;
    private String status;
    private LocalDateTime createdAt;
    private Long authorId;
    private String authorNickname;
}
```

- [ ] **Step 2: 创建 AdminPostController**

```java
// src/main/java/com/LHZ/TripMate/controller/admin/AdminPostController.java
package com.LHZ.TripMate.controller.admin;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.PageResult;
import com.LHZ.TripMate.dto.admin.AdminPostDTO;
import com.LHZ.TripMate.entity.Post;
import com.LHZ.TripMate.entity.WxUser;
import com.LHZ.TripMate.repository.PostRepository;
import com.LHZ.TripMate.repository.WxUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/posts")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostRepository postRepo;
    private final WxUserRepository wxUserRepo;

    @GetMapping
    public Result<PageResult<AdminPostDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Page<Post> pg;
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status != null && !status.isBlank()) {
            pg = postRepo.findByStatus(status, pageable);
        } else {
            pg = postRepo.findAll(pageable);
        }
        var items = pg.getContent().stream().map(p -> {
            WxUser u = wxUserRepo.findById(p.getUserId()).orElse(null);
            String content = p.getContent() != null && p.getContent().length() > 200
                    ? p.getContent().substring(0, 200) : p.getContent();
            return AdminPostDTO.builder()
                    .id(p.getId()).title(p.getTitle()).content(content)
                    .category(p.getCategory()).coverUrl(p.getCoverUrl())
                    .likeCount(p.getLikeCount()).commentCount(p.getCommentCount())
                    .viewCount(p.getViewCount()).status(p.getStatus())
                    .createdAt(p.getCreatedAt())
                    .authorId(u != null ? u.getId() : null)
                    .authorNickname(u != null ? u.getNickname() : "未知用户")
                    .build();
        }).toList();
        return Result.success(new PageResult<>(items, pg.getTotalElements(), page, size));
    }

    @PostMapping("/{id}/delete")
    public Result<Void> deletePost(@PathVariable Long id) {
        postRepo.findById(id).ifPresent(p -> {
            p.setStatus("DELETED");
            postRepo.save(p);
        });
        return Result.success(null);
    }

    @PostMapping("/{id}/restore")
    public Result<Void> restorePost(@PathVariable Long id) {
        postRepo.findById(id).ifPresent(p -> {
            p.setStatus("PUBLISHED");
            postRepo.save(p);
        });
        return Result.success(null);
    }
}
```

注意：`postRepo.findByStatus(String, Pageable)` 需要在 `PostRepository` 中支持 Pageable 版本。读取 PostRepository.java，若现有 `findByStatus` 只返回 `List`，需要加：
```java
Page<Post> findByStatus(String status, Pageable pageable);
```

- [ ] **Step 3: 编译验证**

```bash
cd D:/code/作业/综合实践2/TripMate && ./mvnw compile -q
```

- [ ] **Step 4: 创建 admin_frontend/src/api/posts.ts**

```typescript
// admin_frontend/src/api/posts.ts
import http from './http'

export interface AdminPost {
  id: number
  title: string
  content: string
  category: string
  coverUrl?: string
  likeCount: number
  commentCount: number
  viewCount: number
  status: string
  createdAt: string
  authorId: number
  authorNickname: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export function listPosts(params: { page?: number; size?: number; status?: string }) {
  return http.get<{ code: number; data: PageResult<AdminPost> }>('/api/admin/posts', { params })
}

export function deletePost(id: number) {
  return http.post(`/api/admin/posts/${id}/delete`)
}

export function restorePost(id: number) {
  return http.post(`/api/admin/posts/${id}/restore`)
}
```

- [ ] **Step 5: 创建 PostsView.vue**

```vue
<!-- admin_frontend/src/views/posts/PostsView.vue -->
<template>
  <div class="posts-view">
    <div class="header">
      <h2>帖子管理</h2>
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:140px" @change="loadData">
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已删除" value="DELETED" />
      </el-select>
    </div>

    <el-table :data="posts" v-loading="loading" border style="width:100%">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="authorNickname" label="作者" width="120" />
      <el-table-column prop="category" label="分类" width="110">
        <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
      </el-table-column>
      <el-table-column label="数据" width="150">
        <template #default="{ row }">
          <span>👍{{ row.likeCount }} 💬{{ row.commentCount }} 👁{{ row.viewCount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'danger'">
            {{ row.status === 'PUBLISHED' ? '已发布' : '已删除' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发布时间" width="160">
        <template #default="{ row }">{{ row.createdAt?.replace('T', ' ').slice(0,16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PUBLISHED'" type="danger" size="small"
                     @click="handleDelete(row)">删除</el-button>
          <el-button v-else type="success" size="small"
                     @click="handleRestore(row)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top:20px;justify-content:flex-end"
      @change="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPosts, deletePost, restorePost, type AdminPost } from '@/api/posts'

const posts = ref<AdminPost[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const statusFilter = ref('')

const categoryMap: Record<string, string> = {
  SCENIC: '景点攻略', FOOD: '美食推荐', TRANSPORT: '交通住宿',
  FREE_TRAVEL: '自由行', FAMILY: '亲子游'
}
function categoryLabel(v: string) { return categoryMap[v] || v }

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await listPosts({
      page: currentPage.value - 1,
      size: pageSize.value,
      status: statusFilter.value || undefined
    })
    posts.value = res.data.data.items
    total.value = res.data.data.total
  } finally { loading.value = false }
}

async function handleDelete(row: AdminPost) {
  await ElMessageBox.confirm(`确定要删除《${row.title}》吗？`, '确认删除', { type: 'warning' })
  await deletePost(row.id)
  ElMessage.success('已删除')
  loadData()
}

async function handleRestore(row: AdminPost) {
  await restorePost(row.id)
  ElMessage.success('已恢复')
  loadData()
}
</script>

<style scoped>
.posts-view { padding: 0; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header h2 { margin: 0; }
</style>
```

- [ ] **Step 6: 修改 router/index.ts — 加 /posts 路由**

读取 `admin_frontend/src/router/index.ts`，在 routes 数组中加（在 SUPER_ADMIN only 区域）：
```typescript
{
  path: '/posts',
  component: () => import('../views/posts/PostsView.vue'),
  meta: { requiresAuth: true, requiresSuperAdmin: true }
}
```

- [ ] **Step 7: 修改 AppLayout.vue — 加帖子管理菜单项**

读取 `admin_frontend/src/components/AppLayout.vue`，在菜单列表中（用户管理附近）加帖子管理入口：
```html
<el-menu-item index="/posts" v-if="authStore.isSuperAdmin">
  <el-icon><Document /></el-icon>
  <span>帖子管理</span>
</el-menu-item>
```
如需引入 Document 图标：`import { Document } from '@element-plus/icons-vue'` 并注册。

- [ ] **Step 8: Commit**

```bash
git add src/ admin_frontend/
git commit -m "feat: Admin 帖子审核管理"
```
