# 攻略论坛功能设计文档

**日期：** 2026-06-21  
**模块：** 攻略（guide）页面 — 旅游论坛 UGC 功能  
**状态：** 已确认，待实现

---

## 1. 概述

在现有空白的 `pages/guide/guide.vue` 页面中实现旅游攻略论坛。用户可以发布图文帖子、浏览他人攻略、进行点赞/收藏/评论互动。图片支持本地上传至后端服务器。

---

## 2. 数据模型（后端）

### 2.1 Post（帖子）

```java
@Entity @Table(name = "post")
Post {
  Long id (PK, AUTO)
  Long userId           // 关联 wx_user.id，NOT NULL
  String title          // 最大 100 字符，NOT NULL
  String content        // TEXT，NOT NULL
  String category       // 枚举值字符串，NOT NULL
  String coverUrl       // 封面图 URL，可为空（取第一张图）
  String imageUrls      // JSON 字符串，最多 3 张，可为空
  int viewCount         // 阅读数，默认 0
  int likeCount         // 点赞数，默认 0
  int commentCount      // 评论数，默认 0
  String status         // "PUBLISHED" | "DELETED"，默认 PUBLISHED
  LocalDateTime createdAt
}
```

**category 枚举值：**
- `SCENIC` — 景点攻略
- `FOOD` — 美食推荐
- `TRANSPORT` — 交通住宿
- `FREE_TRAVEL` — 自由行
- `FAMILY` — 亲子游

### 2.2 PostLike（点赞）

```java
@Entity @Table(name = "post_like",
  uniqueConstraints = @UniqueConstraint(columnNames = {"post_id","user_id"}))
PostLike {
  Long id
  Long postId
  Long userId
  LocalDateTime createdAt
}
```

### 2.3 PostFavorite（收藏）

```java
@Entity @Table(name = "post_favorite",
  uniqueConstraints = @UniqueConstraint(columnNames = {"post_id","user_id"}))
PostFavorite {
  Long id
  Long postId
  Long userId
  LocalDateTime createdAt
}
```

### 2.4 PostComment（评论）

```java
@Entity @Table(name = "post_comment")
PostComment {
  Long id
  Long postId
  Long userId
  String content        // 最大 500 字符
  LocalDateTime createdAt
}
```

---

## 3. 后端 API

所有接口返回 `Result<T>` 统一信封。需要登录的接口从 JWT 中解析 `userId`（`openid` → 查 `WxUser`）。

### 3.1 图片上传

```
POST /api/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>

Response: Result<{ url: String }>
```

- 文件存储路径：`uploads/<uuid>.<ext>`
- 静态资源访问：`GET /uploads/{filename}`（Spring Boot `addResourceHandlers` 配置）
- 限制：单文件 ≤ 5MB，仅接受 image/* 类型

### 3.2 帖子接口

```
POST /api/posts                          发布帖子（需登录）
  Body: { title, content, category, imageUrls: string[] }
  coverUrl 自动取 imageUrls[0]

GET /api/posts?page=0&size=10&category=&sort=new|hot
  sort=new  → 按 createdAt DESC
  sort=hot  → 按 likeCount DESC，同分按 createdAt DESC
  列表 DTO 含：id, title, coverUrl, category, likeCount, commentCount,
              viewCount, createdAt, author{id,nickname,avatarUrl},
              liked(是否已点赞), favorited(是否已收藏)

GET /api/posts/{id}                      详情（+1 viewCount）
  DTO 含：全部字段 + imageUrls[] + author + liked + favorited

DELETE /api/posts/{id}                   删除（仅作者，status→DELETED）

POST /api/posts/{id}/like                点赞 / 取消（toggle）
  Response: Result<{ liked: boolean, likeCount: int }>

POST /api/posts/{id}/favorite            收藏 / 取消（toggle）
  Response: Result<{ favorited: boolean }>

GET /api/posts/my?page=0&size=10         我发的帖子（需登录）
GET /api/posts/my/favorites?page=0&size=10  我收藏的帖子（需登录）
```

### 3.3 评论接口

```
GET  /api/posts/{id}/comments?page=0&size=20
  按 createdAt ASC（时间正序）
  DTO 含：id, content, createdAt, author{id,nickname,avatarUrl}

POST /api/posts/{id}/comments            发评论（需登录）
  Body: { content }
  成功后 post.commentCount +1
```

---

## 4. 后端代码结构

```
dto/
  PostDTO.java                 列表+详情用，含 liked/favorited 状态
  PostCreateDTO.java           发帖请求体
  PostCommentDTO.java          评论 DTO
  CommentCreateDTO.java        发评论请求体

entity/
  Post.java
  PostLike.java
  PostFavorite.java
  PostComment.java

repository/
  PostRepository.java          含分页查询（category过滤+排序）
  PostLikeRepository.java      findByPostIdAndUserId, existsByPostIdAndUserId
  PostFavoriteRepository.java  同上
  PostCommentRepository.java   findByPostId(Pageable)

service/
  PostService.java             接口
  UploadService.java           接口

service/impl/
  PostServiceImpl.java
  UploadServiceImpl.java       存文件到 uploads/ 目录

controller/
  PostController.java          /api/posts/**
  UploadController.java        /api/upload

config/
  WebMvcConfig.java            addResourceHandlers：/uploads/** 映射到本地目录
```

`SecurityConfig` 需要将 `GET /api/posts/**` 和 `GET /uploads/**` 加入 `permitAll`，其余 POST/DELETE 需要 JWT。

---

## 5. 前端页面结构

### 5.1 pages/guide/guide.vue（攻略主页 Feed）

**布局：**
- 顶部分类 Tab 横向滚动：全部 / 景点 / 美食 / 交通 / 自由行 / 亲子
- 右侧排序切换胶囊：最新 / 热门
- 帖子卡片列表（竖向滚动，下拉刷新 + 上拉加载更多）
- 右下角固定浮动「✏️ 发帖」按钮（需登录检测，未登录跳提示）

**帖子卡片字段：** 封面图（无图时显示默认渐变色块）/ 分类角标 / 标题 / 作者头像+昵称 / 👍点赞数 / 💬评论数

### 5.2 pages/guide/detail/detail.vue（帖子详情）

**布局：**
- 图片轮播（swiper，无图时不显示）
- 作者头像 + 昵称 + 发布时间 + 分类标签
- 正文（可滚动，保留换行）
- 评论列表（分页加载）
- 底部固定栏：阅读数 / 点赞按钮（有状态）/ 收藏按钮（有状态）/ 评论输入框

**通过 `uni.navigateTo({ url: '/pages/guide/detail/detail?id=xxx' })` 跳转**

### 5.3 pages/guide/create/create.vue（发帖页）

**布局：**
- 标题输入框（单行，最多 100 字）
- 正文输入（多行 textarea，最多 1000 字，字符计数）
- 图片区域：最多 3 张，点击格子 → `uni.chooseImage` → 调用 `/api/upload` → 显示预览缩略图（可删除）
- 分类选择（picker 或横向胶囊选择）
- 顶部导航栏「发布」按钮

### 5.4 pages/mine/mine.vue（改动）

新增两个入口卡片：
- 「我的攻略」→ 跳转列表页（复用 guide feed，过滤 my）
- 「我的收藏」→ 跳转列表页（复用 guide feed，过滤 favorites）

---

## 6. 前端 API 封装

```
frontend/api/post.ts
  fetchPosts(params)           → GET /api/posts
  fetchPostDetail(id)          → GET /api/posts/{id}
  createPost(data)             → POST /api/posts
  deletePost(id)               → DELETE /api/posts/{id}
  toggleLike(id)               → POST /api/posts/{id}/like
  toggleFavorite(id)           → POST /api/posts/{id}/favorite
  fetchComments(id, params)    → GET /api/posts/{id}/comments
  createComment(id, content)   → POST /api/posts/{id}/comments
  fetchMyPosts(params)         → GET /api/posts/my
  fetchMyFavorites(params)     → GET /api/posts/my/favorites

frontend/api/upload.ts
  uploadImage(filePath)        → POST /api/upload（uni.uploadFile）
```

---

## 7. 关键约束

- 图片上传：单文件 ≤ 5MB，格式 jpg/png/webp，服务端存 `${projectRoot}/uploads/`
- 未登录用户可浏览帖子（GET 接口 permitAll），点赞/收藏/评论/发帖需要 JWT
- 删除帖子为软删除（status = DELETED），列表查询只返回 PUBLISHED
- 分页默认 size=10，评论 size=20
- imageUrls 字段以 JSON 字符串存储（`["url1","url2"]`），Service 层负责序列化/反序列化
- `likeCount` / `commentCount` 使用数据库原子更新（`@Modifying @Query`），避免并发计数错误

---

## 8. 不在本次范围内

- 关注 / 粉丝系统
- 评论嵌套回复
- 消息通知 / @用户
- 搜索功能（可后续迭代）
- Admin 后台帖子审核（可通过现有 Admin 扩展）
