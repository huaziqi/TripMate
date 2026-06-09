# Badge Hall 勋章馆 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现勋章馆功能——用户可在 TabBar 新增的"勋章"页查看景点勋章与成就勋章，已解锁勋章彩色展示，未解锁灰色剪影，点击已解锁勋章弹出详情卡片。

**Architecture:** 后端新增 `Badge`（勋章定义）和 `UserBadge`（用户已解锁记录）两张表，提供 `GET /api/badges`（返回所有勋章 + 当前用户解锁状态）和 `POST /api/badges/{id}/unlock`（解锁勋章）接口。前端新增 `pages/badges/badges.vue` 页面，参照参考截图分"最近获得 / 景点勋章 / 成就勋章"三个分区展示，并在 TabBar 加入"勋章"入口。

**Tech Stack:** Spring Boot 4 / JPA / Lombok（后端）；UniApp Vue 3 Composition API / TypeScript（前端）

---

## 文件清单

### 后端新增
| 文件 | 职责 |
|------|------|
| `entity/Badge.java` | 勋章定义实体 |
| `entity/UserBadge.java` | 用户解锁记录实体 |
| `entity/BadgeType.java` | 枚举：SPOT / ACHIEVEMENT |
| `entity/BadgeRarity.java` | 枚举：COMMON / RARE / EPIC / LEGENDARY |
| `repository/BadgeRepository.java` | Badge JPA 仓库 |
| `repository/UserBadgeRepository.java` | UserBadge JPA 仓库 |
| `dto/badge/BadgeDTO.java` | 返回给前端的勋章数据（含解锁状态） |
| `service/BadgeService.java` | 接口 |
| `service/impl/BadgeServiceImpl.java` | 实现：查勋章列表、解锁勋章 |
| `controller/BadgeController.java` | `/api/badges` 端点 |
| `config/BadgeDataInitializer.java` | 启动时写入默认勋章种子数据 |

### 后端修改
| 文件 | 改动 |
|------|------|
| `config/SecurityConfig.java` | 放行 `POST /api/badges/{id}/unlock` 无需额外改动（已有 /api/** authenticated 规则覆盖） |

### 前端新增
| 文件 | 职责 |
|------|------|
| `frontend/api/badge.ts` | 调用后端勋章接口的封装 |
| `frontend/pages/badges/badges.vue` | 勋章馆主页面 |

### 前端修改
| 文件 | 改动 |
|------|------|
| `frontend/pages.json` | 注册 `pages/badges/badges` 路由 |
| `frontend/components/TabBar/TabBar.vue` | 添加"勋章"tab（🏅，key=badges） |
| `frontend/locales/zh.json` | 添加 `tabbar.badges = "勋章"` |
| `frontend/locales/en.json` | 添加 `tabbar.badges = "Badges"` |

---

## Task 1：后端枚举 + 实体

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/entity/BadgeType.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/BadgeRarity.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/Badge.java`
- Create: `src/main/java/com/LHZ/TripMate/entity/UserBadge.java`

- [ ] **Step 1: 创建 BadgeType 枚举**

```java
// src/main/java/com/LHZ/TripMate/entity/BadgeType.java
package com.LHZ.TripMate.entity;

public enum BadgeType {
    SPOT,        // 景点勋章（扫码获得）
    ACHIEVEMENT  // 成就勋章（完成条件获得）
}
```

- [ ] **Step 2: 创建 BadgeRarity 枚举**

```java
// src/main/java/com/LHZ/TripMate/entity/BadgeRarity.java
package com.LHZ.TripMate.entity;

public enum BadgeRarity {
    COMMON,    // 普通 灰色
    RARE,      // 稀有 蓝色
    EPIC,      // 史诗 紫色
    LEGENDARY  // 传说 金色
}
```

- [ ] **Step 3: 创建 Badge 实体**

```java
// src/main/java/com/LHZ/TripMate/entity/Badge.java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "badge")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeRarity rarity;

    @Column(nullable = false, length = 8)
    private String icon; // emoji 图标，如 "🗼"

    @Column(name = "unlock_condition", nullable = false, length = 200)
    private String unlockCondition; // 解锁条件描述，展示给用户

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder; // 同类型内排序
}
```

- [ ] **Step 4: 创建 UserBadge 实体**

```java
// src/main/java/com/LHZ/TripMate/entity/UserBadge.java
package com.LHZ.TripMate.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_badge",
       uniqueConstraints = @UniqueConstraint(columnNames = {"openid", "badge_id"}))
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String openid;

    @Column(name = "badge_id", nullable = false)
    private Long badgeId;

    @Column(name = "unlocked_at", updatable = false)
    private LocalDateTime unlockedAt;

    @Column(length = 200)
    private String note; // 解锁时的备注，如"在故宫扫码获得"

    @PrePersist
    void prePersist() {
        unlockedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 5: 启动后端，确认 JPA 自动建表成功**

运行 `./mvnw spring-boot:run`，观察日志中出现：
```
Hibernate: create table badge (...)
Hibernate: create table user_badge (...)
```
无报错后停止服务。

- [ ] **Step 6: Commit**

```
git add src/main/java/com/LHZ/TripMate/entity/BadgeType.java
git add src/main/java/com/LHZ/TripMate/entity/BadgeRarity.java
git add src/main/java/com/LHZ/TripMate/entity/Badge.java
git add src/main/java/com/LHZ/TripMate/entity/UserBadge.java
git commit -m "feat: add Badge and UserBadge entities with type/rarity enums"
```

---

## Task 2：Repository + DTO

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/repository/BadgeRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/repository/UserBadgeRepository.java`
- Create: `src/main/java/com/LHZ/TripMate/dto/badge/BadgeDTO.java`

- [ ] **Step 1: 创建 BadgeRepository**

```java
// src/main/java/com/LHZ/TripMate/repository/BadgeRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.Badge;
import com.LHZ.TripMate.entity.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    List<Badge> findByTypeOrderBySortOrderAsc(BadgeType type);
    List<Badge> findAllByOrderBySortOrderAsc();
}
```

- [ ] **Step 2: 创建 UserBadgeRepository**

```java
// src/main/java/com/LHZ/TripMate/repository/UserBadgeRepository.java
package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByOpenidOrderByUnlockedAtDesc(String openid);
    Optional<UserBadge> findByOpenidAndBadgeId(String openid, Long badgeId);
    boolean existsByOpenidAndBadgeId(String openid, Long badgeId);
}
```

- [ ] **Step 3: 创建 BadgeDTO**

```java
// src/main/java/com/LHZ/TripMate/dto/badge/BadgeDTO.java
package com.LHZ.TripMate.dto.badge;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BadgeDTO {
    private Long id;
    private String name;
    private String description;
    private String type;       // "SPOT" or "ACHIEVEMENT"
    private String rarity;     // "COMMON" / "RARE" / "EPIC" / "LEGENDARY"
    private String icon;
    private String unlockCondition;
    private boolean unlocked;
    private LocalDateTime unlockedAt;
    private String note;
}
```

- [ ] **Step 4: Commit**

```
git add src/main/java/com/LHZ/TripMate/repository/BadgeRepository.java
git add src/main/java/com/LHZ/TripMate/repository/UserBadgeRepository.java
git add src/main/java/com/LHZ/TripMate/dto/badge/BadgeDTO.java
git commit -m "feat: add badge repositories and BadgeDTO"
```

---

## Task 3：BadgeService + BadgeServiceImpl

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/service/BadgeService.java`
- Create: `src/main/java/com/LHZ/TripMate/service/impl/BadgeServiceImpl.java`

- [ ] **Step 1: 创建 BadgeService 接口**

```java
// src/main/java/com/LHZ/TripMate/service/BadgeService.java
package com.LHZ.TripMate.service;

import com.LHZ.TripMate.dto.badge.BadgeDTO;
import java.util.List;

public interface BadgeService {
    List<BadgeDTO> listAllBadges(String openid);
    BadgeDTO unlockBadge(String openid, Long badgeId);
}
```

- [ ] **Step 2: 创建 BadgeServiceImpl**

```java
// src/main/java/com/LHZ/TripMate/service/impl/BadgeServiceImpl.java
package com.LHZ.TripMate.service.impl;

import com.LHZ.TripMate.dto.badge.BadgeDTO;
import com.LHZ.TripMate.entity.Badge;
import com.LHZ.TripMate.entity.UserBadge;
import com.LHZ.TripMate.repository.BadgeRepository;
import com.LHZ.TripMate.repository.UserBadgeRepository;
import com.LHZ.TripMate.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Override
    public List<BadgeDTO> listAllBadges(String openid) {
        List<Badge> allBadges = badgeRepository.findAllByOrderBySortOrderAsc();

        // openid 对应的所有已解锁记录，以 badgeId 为 key
        Map<Long, UserBadge> unlockedMap = userBadgeRepository
                .findByOpenidOrderByUnlockedAtDesc(openid)
                .stream()
                .collect(Collectors.toMap(UserBadge::getBadgeId, ub -> ub));

        return allBadges.stream()
                .map(badge -> toDTO(badge, unlockedMap.get(badge.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public BadgeDTO unlockBadge(String openid, Long badgeId) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "勋章不存在"));

        if (userBadgeRepository.existsByOpenidAndBadgeId(openid, badgeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该勋章已解锁");
        }

        UserBadge userBadge = new UserBadge();
        userBadge.setOpenid(openid);
        userBadge.setBadgeId(badgeId);
        userBadge.setNote("");
        userBadgeRepository.save(userBadge);

        return toDTO(badge, userBadge);
    }

    private BadgeDTO toDTO(Badge badge, UserBadge userBadge) {
        BadgeDTO dto = new BadgeDTO();
        dto.setId(badge.getId());
        dto.setName(badge.getName());
        dto.setDescription(badge.getDescription());
        dto.setType(badge.getType().name());
        dto.setRarity(badge.getRarity().name());
        dto.setIcon(badge.getIcon());
        dto.setUnlockCondition(badge.getUnlockCondition());
        dto.setUnlocked(userBadge != null);
        if (userBadge != null) {
            dto.setUnlockedAt(userBadge.getUnlockedAt());
            dto.setNote(userBadge.getNote());
        }
        return dto;
    }
}
```

- [ ] **Step 3: Commit**

```
git add src/main/java/com/LHZ/TripMate/service/BadgeService.java
git add src/main/java/com/LHZ/TripMate/service/impl/BadgeServiceImpl.java
git commit -m "feat: implement BadgeService with list and unlock logic"
```

---

## Task 4：BadgeController

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/controller/BadgeController.java`

- [ ] **Step 1: 创建 BadgeController**

```java
// src/main/java/com/LHZ/TripMate/controller/BadgeController.java
package com.LHZ.TripMate.controller;

import com.LHZ.TripMate.common.Result;
import com.LHZ.TripMate.dto.badge.BadgeDTO;
import com.LHZ.TripMate.security.WxUserDetails;
import com.LHZ.TripMate.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public Result<List<BadgeDTO>> listBadges(
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(badgeService.listAllBadges(userDetails.getUsername()));
    }

    @PostMapping("/{id}/unlock")
    public Result<BadgeDTO> unlock(
            @PathVariable Long id,
            @AuthenticationPrincipal WxUserDetails userDetails) {
        return Result.success(badgeService.unlockBadge(userDetails.getUsername(), id));
    }
}
```

- [ ] **Step 2: 启动后端，用 curl 验证接口（需先有 JWT token）**

使用已有的微信登录获取 token 后：
```
GET http://localhost:8080/api/badges
Authorization: Bearer <token>
```
此时返回空数组 `[]`（种子数据还没有），HTTP 200 即正确。

- [ ] **Step 3: Commit**

```
git add src/main/java/com/LHZ/TripMate/controller/BadgeController.java
git commit -m "feat: add BadgeController with list and unlock endpoints"
```

---

## Task 5：种子数据 DataInitializer

**Files:**
- Create: `src/main/java/com/LHZ/TripMate/config/BadgeDataInitializer.java`

- [ ] **Step 1: 创建 BadgeDataInitializer**

```java
// src/main/java/com/LHZ/TripMate/config/BadgeDataInitializer.java
package com.LHZ.TripMate.config;

import com.LHZ.TripMate.entity.Badge;
import com.LHZ.TripMate.entity.BadgeRarity;
import com.LHZ.TripMate.entity.BadgeType;
import com.LHZ.TripMate.repository.BadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BadgeDataInitializer {

    private final BadgeRepository badgeRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (badgeRepository.count() > 0) return; // 已有数据则跳过

        List<Badge> badges = List.of(
            // ── 景点勋章 ──────────────────────────────
            badge("初探故宫",   "踏入这座六百年的皇家宫殿",  BadgeType.SPOT, BadgeRarity.RARE,      "🏯", "扫描故宫景点二维码",   1),
            badge("长城守望者", "登上万里长城，俯瞰山河",    BadgeType.SPOT, BadgeRarity.EPIC,      "🧱", "扫描长城景点二维码",   2),
            badge("西湖漫步",   "走过断桥，赏过荷花",        BadgeType.SPOT, BadgeRarity.COMMON,    "🏞️", "扫描西湖景点二维码",   3),
            badge("黄山揽胜",   "云海、奇松、怪石、温泉",    BadgeType.SPOT, BadgeRarity.EPIC,      "⛰️", "扫描黄山景点二维码",   4),
            badge("外滩夜游",   "万国建筑群在霓虹中闪耀",    BadgeType.SPOT, BadgeRarity.RARE,      "🌃", "扫描外滩景点二维码",   5),
            badge("兵马俑见证", "与沉睡两千年的军阵相遇",    BadgeType.SPOT, BadgeRarity.LEGENDARY, "🏺", "扫描兵马俑景点二维码", 6),

            // ── 成就勋章 ──────────────────────────────
            badge("旅行起点",   "开启你的第一次打卡",        BadgeType.ACHIEVEMENT, BadgeRarity.COMMON,    "🌟", "完成第一次景点打卡",     10),
            badge("探索者",     "已打卡 5 个不同景点",       BadgeType.ACHIEVEMENT, BadgeRarity.RARE,      "🧭", "累计打卡 5 个景点",      11),
            badge("旅行达人",   "已打卡 20 个不同景点",      BadgeType.ACHIEVEMENT, BadgeRarity.EPIC,      "✈️", "累计打卡 20 个景点",     12),
            badge("传奇旅人",   "已打卡 50 个不同景点",      BadgeType.ACHIEVEMENT, BadgeRarity.LEGENDARY, "🌏", "累计打卡 50 个景点",     13),
            badge("夜行者",     "在晚上 10 点后完成打卡",    BadgeType.ACHIEVEMENT, BadgeRarity.RARE,      "🦉", "晚上 22:00 后完成打卡", 14),
            badge("城市征服者", "在同一城市打卡 3 个景点",   BadgeType.ACHIEVEMENT, BadgeRarity.EPIC,      "🏙️", "同城市打卡 3 个景点",   15)
        );

        badgeRepository.saveAll(badges);
    }

    private Badge badge(String name, String desc, BadgeType type, BadgeRarity rarity,
                        String icon, String condition, int order) {
        Badge b = new Badge();
        b.setName(name);
        b.setDescription(desc);
        b.setType(type);
        b.setRarity(rarity);
        b.setIcon(icon);
        b.setUnlockCondition(condition);
        b.setSortOrder(order);
        return b;
    }
}
```

- [ ] **Step 2: 重启后端，验证种子数据写入**

重启后请求：
```
GET http://localhost:8080/api/badges
Authorization: Bearer <token>
```
应返回 12 条勋章，`unlocked` 全部为 `false`。

- [ ] **Step 3: Commit**

```
git add src/main/java/com/LHZ/TripMate/config/BadgeDataInitializer.java
git commit -m "feat: seed 12 default badges (6 spot + 6 achievement)"
```

---

## Task 6：前端 API 封装 + i18n

**Files:**
- Create: `frontend/api/badge.ts`
- Modify: `frontend/locales/zh.json`
- Modify: `frontend/locales/en.json`

- [ ] **Step 1: 创建 badge.ts API 封装**

```typescript
// frontend/api/badge.ts
import { useApi } from '@/utils/useApi'

export interface BadgeDTO {
  id: number
  name: string
  description: string
  type: 'SPOT' | 'ACHIEVEMENT'
  rarity: 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY'
  icon: string
  unlockCondition: string
  unlocked: boolean
  unlockedAt?: string
  note?: string
}

export function useBadgeApi() {
  const { get, post } = useApi()

  function listBadges(): Promise<{ code: number; message: string; data: BadgeDTO[] }> {
    return get<BadgeDTO[]>('/api/badges')
  }

  function unlockBadge(id: number): Promise<{ code: number; message: string; data: BadgeDTO }> {
    return post<BadgeDTO>(`/api/badges/${id}/unlock`)
  }

  return { listBadges, unlockBadge }
}
```

- [ ] **Step 2: 更新 zh.json，添加 tabbar.badges**

在 `frontend/locales/zh.json` 的 `tabbar` 对象中添加：
```json
"badges": "勋章"
```

完整 tabbar 块变为：
```json
"tabbar": {
  "home": "首页",
  "guide": "攻略",
  "elder": "老年版",
  "language": "翻译",
  "badges": "勋章",
  "mine": "我的"
}
```

- [ ] **Step 3: 更新 en.json，添加 tabbar.badges**

```json
"tabbar": {
  "home": "Home",
  "guide": "Guide",
  "elder": "Elder",
  "language": "Translate",
  "badges": "Badges",
  "mine": "Mine"
}
```

- [ ] **Step 4: Commit**

```
git add frontend/api/badge.ts frontend/locales/zh.json frontend/locales/en.json
git commit -m "feat: add badge API wrapper and i18n keys"
```

---

## Task 7：pages.json + TabBar 更新

**Files:**
- Modify: `frontend/pages.json`
- Modify: `frontend/components/TabBar/TabBar.vue`

- [ ] **Step 1: 在 pages.json 注册 badges 路由**

在 `pages` 数组末尾（mine 之前）插入：
```json
{
  "path": "pages/badges/badges",
  "style": {
    "navigationBarTitleText": "勋章馆",
    "navigationBarBackgroundColor": "#1a1a2e",
    "navigationBarTextStyle": "white"
  }
}
```

- [ ] **Step 2: 在 TabBar.vue 的 tabs 数组加入 badges**

在 language 和 mine 之间插入：
```typescript
{
  key: 'badges',
  icon: '🏅',
  url: '/pages/badges/badges'
}
```

完整 tabs 数组变为：
```typescript
const tabs: TabItem[] = [
  { key: 'home',     icon: '🏠', url: '/pages/index/index' },
  { key: 'guide',    icon: '🗺️', url: '/pages/guide/guide' },
  { key: 'language', icon: '🌐', url: '/pages/language/language' },
  { key: 'badges',   icon: '🏅', url: '/pages/badges/badges' },
  { key: 'mine',     icon: '👤', url: '/pages/mine/mine' }
]
```

- [ ] **Step 3: Commit**

```
git add frontend/pages.json frontend/components/TabBar/TabBar.vue
git commit -m "feat: register badges page and add tab to TabBar"
```

---

## Task 8：勋章馆页面 badges.vue

**Files:**
- Create: `frontend/pages/badges/badges.vue`

稀有度对应颜色常量：
- COMMON → `#9e9e9e`
- RARE → `#2196f3`
- EPIC → `#9c27b0`
- LEGENDARY → `#ffd700`

- [ ] **Step 1: 创建 badges.vue**

```vue
<!-- frontend/pages/badges/badges.vue -->
<template>
  <view class="page">

    <!-- 顶部暗色 header -->
    <view class="header">
      <view class="header-avatar">
        <image v-if="avatarUrl" class="avatar" :src="avatarUrl" mode="aspectFill" />
        <view v-else class="avatar-placeholder">👤</view>
      </view>
      <text class="header-name">{{ nickname }}</text>
      <text class="header-count">已获得 {{ unlockedCount }} / {{ total }} 枚勋章</text>
    </view>

    <!-- 最近获得 -->
    <view v-if="recentBadges.length" class="section">
      <text class="section-title">最近获得</text>
      <scroll-view scroll-x class="recent-scroll">
        <view class="recent-list">
          <view
            v-for="badge in recentBadges"
            :key="badge.id"
            class="recent-item"
            @click="openDetail(badge)"
          >
            <BadgeCard :badge="badge" size="large" />
            <text class="badge-name">{{ badge.name }}</text>
            <text class="badge-date">{{ formatDate(badge.unlockedAt) }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 景点勋章 -->
    <view class="section">
      <text class="section-title">景点勋章</text>
      <view class="badge-grid">
        <view
          v-for="badge in spotBadges"
          :key="badge.id"
          class="grid-item"
          @click="badge.unlocked && openDetail(badge)"
        >
          <BadgeCard :badge="badge" size="medium" />
          <text class="badge-name" :class="{ locked: !badge.unlocked }">{{ badge.name }}</text>
          <text v-if="!badge.unlocked" class="badge-condition">{{ badge.unlockCondition }}</text>
        </view>
      </view>
    </view>

    <!-- 成就勋章 -->
    <view class="section">
      <text class="section-title">成就勋章</text>
      <view class="badge-grid">
        <view
          v-for="badge in achievementBadges"
          :key="badge.id"
          class="grid-item"
          @click="badge.unlocked && openDetail(badge)"
        >
          <BadgeCard :badge="badge" size="medium" />
          <text class="badge-name" :class="{ locked: !badge.unlocked }">{{ badge.name }}</text>
          <text v-if="!badge.unlocked" class="badge-condition">{{ badge.unlockCondition }}</text>
        </view>
      </view>
    </view>

    <!-- 详情弹窗 -->
    <view v-if="selectedBadge" class="modal-mask" @click.self="selectedBadge = null">
      <view class="modal">
        <BadgeCard :badge="selectedBadge" size="large" />
        <view class="modal-rarity-tag" :style="{ background: rarityColor(selectedBadge.rarity) }">
          {{ rarityLabel(selectedBadge.rarity) }}
        </view>
        <text class="modal-name">{{ selectedBadge.name }}</text>
        <text class="modal-desc">{{ selectedBadge.description }}</text>
        <text class="modal-date">🗓 获得于 {{ formatDate(selectedBadge.unlockedAt) }}</text>
        <text v-if="selectedBadge.note" class="modal-note">{{ selectedBadge.note }}</text>
        <view class="modal-close" @click="selectedBadge = null">关闭</view>
      </view>
    </view>

    <view class="tabbar-placeholder" />
    <TabBar active="badges" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import TabBar from '@/components/TabBar/TabBar.vue'
import BadgeCard from '@/components/BadgeCard/BadgeCard.vue'
import { useBadgeApi, type BadgeDTO } from '@/api/badge'
import { useAuth } from '@/composables/useAuth'

const { listBadges } = useBadgeApi()
const { authState } = useAuth()

const badges = ref<BadgeDTO[]>([])
const selectedBadge = ref<BadgeDTO | null>(null)

const nickname = computed(() => authState.userInfo?.nickname || '旅行者')
const avatarUrl = computed(() => authState.userInfo?.avatarUrl || '')

const spotBadges = computed(() => badges.value.filter(b => b.type === 'SPOT'))
const achievementBadges = computed(() => badges.value.filter(b => b.type === 'ACHIEVEMENT'))
const recentBadges = computed(() =>
  badges.value
    .filter(b => b.unlocked)
    .sort((a, b) => new Date(b.unlockedAt!).getTime() - new Date(a.unlockedAt!).getTime())
    .slice(0, 6)
)
const unlockedCount = computed(() => badges.value.filter(b => b.unlocked).length)
const total = computed(() => badges.value.length)

onMounted(async () => {
  if (!authState.isLoggedIn) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  try {
    const res = await listBadges()
    if (res.code === 200) badges.value = res.data
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
})

function openDetail(badge: BadgeDTO) {
  selectedBadge.value = badge
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${d.getMonth() + 1}.${d.getDate()}`
}

function rarityColor(rarity: string): string {
  const map: Record<string, string> = {
    COMMON: '#9e9e9e',
    RARE: '#2196f3',
    EPIC: '#9c27b0',
    LEGENDARY: '#ffd700'
  }
  return map[rarity] ?? '#9e9e9e'
}

function rarityLabel(rarity: string): string {
  const map: Record<string, string> = {
    COMMON: '普通', RARE: '稀有', EPIC: '史诗', LEGENDARY: '传说'
  }
  return map[rarity] ?? rarity
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f0f2f5;
}

/* Header */
.header {
  background: #1a1a2e;
  padding: 60rpx 32rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.header-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  overflow: hidden;
  border: 4rpx solid rgba(255,255,255,0.3);
  margin-bottom: 16rpx;
}
.avatar { width: 120rpx; height: 120rpx; }
.avatar-placeholder {
  width: 120rpx; height: 120rpx;
  background: #333; display: flex;
  align-items: center; justify-content: center;
  font-size: 60rpx;
}
.header-name { color: #fff; font-size: 32rpx; font-weight: 600; margin-bottom: 8rpx; }
.header-count { color: rgba(255,255,255,0.6); font-size: 24rpx; }

/* Section */
.section { margin: 24rpx 0 0; }
.section-title {
  font-size: 28rpx; font-weight: 600; color: #1a1a1a;
  padding: 0 32rpx 20rpx;
}

/* 最近获得横向滚动 */
.recent-scroll { width: 100%; }
.recent-list {
  display: flex; flex-direction: row;
  padding: 0 24rpx; white-space: nowrap;
}
.recent-item {
  display: inline-flex; flex-direction: column;
  align-items: center; margin-right: 24rpx;
  width: 160rpx;
}
.badge-date { font-size: 20rpx; color: #999; margin-top: 4rpx; }

/* 网格 */
.badge-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  padding: 0 24rpx;
}
.grid-item {
  display: flex; flex-direction: column;
  align-items: center;
}
.badge-name {
  font-size: 22rpx; color: #333; margin-top: 8rpx;
  text-align: center; line-height: 1.3;
}
.badge-name.locked { color: #bbb; }
.badge-condition {
  font-size: 18rpx; color: #bbb;
  text-align: center; margin-top: 4rpx; line-height: 1.3;
}

/* 弹窗 */
.modal-mask {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000;
}
.modal {
  background: #fff; border-radius: 24rpx;
  padding: 48rpx 40rpx 40rpx;
  width: 560rpx;
  display: flex; flex-direction: column; align-items: center;
}
.modal-rarity-tag {
  margin-top: 16rpx; padding: 4rpx 20rpx;
  border-radius: 20rpx; color: #fff; font-size: 22rpx;
}
.modal-name { font-size: 36rpx; font-weight: 700; color: #1a1a1a; margin-top: 16rpx; }
.modal-desc { font-size: 26rpx; color: #666; margin-top: 12rpx; text-align: center; }
.modal-date { font-size: 24rpx; color: #999; margin-top: 16rpx; }
.modal-note { font-size: 22rpx; color: #aaa; margin-top: 8rpx; }
.modal-close {
  margin-top: 32rpx; padding: 16rpx 80rpx;
  background: #1a1a2e; color: #fff;
  border-radius: 40rpx; font-size: 28rpx;
}

.tabbar-placeholder { height: 140rpx; }
</style>
```

- [ ] **Step 2: Commit**

```
git add frontend/pages/badges/badges.vue
git commit -m "feat: add badges hall page with spot/achievement sections and detail modal"
```

---

## Task 9：BadgeCard 组件

**Files:**
- Create: `frontend/components/BadgeCard/BadgeCard.vue`

- [ ] **Step 1: 创建 BadgeCard 组件**

```vue
<!-- frontend/components/BadgeCard/BadgeCard.vue -->
<template>
  <view class="badge-wrap" :class="[`size-${size}`, { locked: !badge.unlocked }]">
    <view class="badge-hex" :style="hexStyle">
      <text class="badge-icon">{{ badge.unlocked ? badge.icon : '❓' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BadgeDTO } from '@/api/badge'

const props = defineProps<{
  badge: BadgeDTO
  size?: 'medium' | 'large'
}>()

const rarityColor: Record<string, string> = {
  COMMON: '#9e9e9e',
  RARE: '#2196f3',
  EPIC: '#9c27b0',
  LEGENDARY: '#ffd700'
}

const hexStyle = computed(() => {
  if (!props.badge.unlocked) return { background: '#ddd' }
  const color = rarityColor[props.badge.rarity] ?? '#9e9e9e'
  return { background: `linear-gradient(135deg, ${color}cc, ${color})` }
})
</script>

<style scoped>
.badge-wrap { display: flex; align-items: center; justify-content: center; }

.badge-hex {
  display: flex; align-items: center; justify-content: center;
  border-radius: 20%;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
}

.size-medium .badge-hex { width: 100rpx; height: 116rpx; }
.size-large  .badge-hex { width: 140rpx; height: 162rpx; }

.badge-icon { font-size: 40rpx; }
.size-large .badge-icon { font-size: 56rpx; }

.locked .badge-hex { filter: grayscale(1); opacity: 0.5; }
</style>
```

- [ ] **Step 2: 在 HBuilderX 编译并在微信开发者工具中预览效果**

验证要点：
- TabBar 出现 🏅 勋章 tab
- 点击进入勋章馆，显示顶部暗色 header
- 景点勋章 / 成就勋章两个分区，12 枚勋章以 4 列网格展示
- 所有勋章均为灰色（因为都未解锁）
- 未登录时提示"请先登录"

- [ ] **Step 3: 手动解锁一枚勋章测试彩色展示**

用 Apifox / curl 调用：
```
POST http://localhost:8080/api/badges/1/unlock
Authorization: Bearer <token>
```
刷新页面，"初探故宫"变为彩色，出现在"最近获得"区域，点击弹出详情卡片。

- [ ] **Step 4: Commit**

```
git add frontend/components/BadgeCard/BadgeCard.vue
git commit -m "feat: add BadgeCard component with hexagon shape and rarity colors"
```

---

## 自检

- [x] 所有 `BadgeDTO` 字段在 Task 2 定义，Task 8/9 使用的字段名完全匹配
- [x] `unlockBadge` 方法签名在 Service 接口和 Impl 中一致
- [x] `WxUserDetails.getUsername()` 返回 openid，与现有 WxAuthController 用法一致
- [x] TabBar `active="badges"` 与 Task 7 中 tabs 的 key 匹配
- [x] 种子数据 `BadgeDataInitializer` 检查 `count() > 0` 防止重复插入
- [x] `BadgeCard` 接收的 `BadgeDTO` 类型与 `badge.ts` 中导出的接口一致
