# 微信登录功能设计文档

**日期**：2026-06-05  
**项目**：TripMate 小程序  
**范围**：小程序端微信登录 + 个人主页（mine 页面）

---

## 1. 需求概述

为 TripMate 小程序添加微信登录功能，并实现完整的「我的」个人主页页面。

**功能要点：**
- 用户通过微信一键登录，无需注册
- 存储用户信息：openid、昵称、头像
- 个人主页展示用户信息及功能入口
- 支持 elder mode（字体随 fontScale 缩放）
- 使用真实微信 AppID/AppSecret 调用 jscode2session

---

## 2. 整体数据流

```
[小程序] uni.login() → code（有效期 5 分钟）
    ↓
POST /api/wx/login { code }
    ↓
[后端] 调微信 jscode2session API → openid
    ↓
WxUser 表 upsert（首次自动注册，nickname/avatarUrl 初始为空）
    ↓
生成 JWT（sub=openid, userType=WX_USER, 有效期 24h）
    ↓
返回 { token, openid, nickname, avatarUrl }
    ↓
[小程序] 存入 uni.storage → 后续请求自动携带 Bearer token
```

---

## 3. 后端设计

### 3.1 新增配置

**`application.yaml` 新增：**
```yaml
wx:
  appid: YOUR_APP_ID
  secret: YOUR_APP_SECRET
```

**新增 `config/WxConfig.java`**：`@ConfigurationProperties(prefix = "wx")`，读取 appid 和 secret。

### 3.2 新增实体 `entity/WxUser`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 自增主键 |
| openid | String | 微信唯一标识，unique |
| nickname | String | 用户昵称，可为空 |
| avatarUrl | String | 头像 URL，可为空 |
| createdAt | LocalDateTime | 首次登录时间 |

### 3.3 新增 `repository/WxUserRepository`

```java
Optional<WxUser> findByOpenid(String openid);
```

### 3.4 DTO

**`dto/wx/WxLoginRequestDTO`**
```json
{ "code": "string" }
```

**`dto/wx/WxLoginResponseDTO`**
```json
{ "token": "string", "openid": "string", "nickname": "string", "avatarUrl": "string" }
```

**`dto/wx/UpdateProfileRequestDTO`**
```json
{ "nickname": "string", "avatarUrl": "string" }
```

### 3.5 新增 `service/WxAuthService` + impl

- `login(code)`: 调微信 jscode2session → upsert WxUser → 生成 JWT → 返回 ResponseDTO
- `updateProfile(openid, nickname, avatarUrl)`: 更新 WxUser 昵称头像

微信 jscode2session 调用地址：
```
GET https://api.weixin.qq.com/sns/jscode2session
  ?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code
```
返回 `{ openid, session_key, ... }`，只使用 `openid`。

### 3.6 新增 `controller/WxAuthController`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/wx/login` | permitAll | 微信登录 |
| POST | `/api/wx/profile` | 需要 WX_USER token | 更新昵称头像 |

### 3.7 新增 `security/WxUserDetails`

实现 `UserDetails`，包装 `WxUser`，`getAuthorities()` 返回 `ROLE_WX_USER`。

### 3.8 修改 `util/JwtUtil`

- `generateToken(String subject, String role, String userType)` 新增 `userType` 参数
- 新增 `extractUserType(String token)` 方法
- 保持原有 admin 相关方法签名不变（传 `userType = "ADMIN"`）

### 3.9 修改 `security/JwtAuthFilter`

```
取出 token → extractUserType()
  ├─ "WX_USER" → WxUserRepository.findByOpenid() → WxUserDetails
  └─ "ADMIN"   → AdminUserRepository.findByUsername() → AdminUserDetails（原逻辑）
```

### 3.10 修改 `config/SecurityConfig`

```java
.requestMatchers("/api/admin/login", "/api/wx/login").permitAll()
.requestMatchers("/api/**").authenticated()
```

---

## 4. 前端设计

### 4.1 新增 `api/auth.ts`

```ts
wxLogin(code: string): Promise<ApiResponse<WxLoginResponse>>
updateProfile(nickname: string, avatarUrl: string): Promise<ApiResponse<void>>
```

### 4.2 新增 `composables/useAuth.ts`

Module-level 单例（与 `useElder.ts` 同一模式）。

**状态：**
```ts
isLoggedIn: boolean
userInfo: { openid: string; nickname: string; avatarUrl: string } | null
```

**方法：**
```ts
login()            // uni.login() → wxLogin(code) → 存 token + userInfo 到 storage
logout()           // 清除 token + userInfo
loadFromStorage()  // App.vue onLaunch 时调用，恢复登录状态
```

storage key：`token`（复用现有 key）、`userInfo`（新增）

### 4.3 改写 `pages/mine/mine.vue`

**未登录态：**
```
┌─────────────────────────────┐
│  灰色头像占位  未登录        │
│  [一键登录] 按钮             │
└─────────────────────────────┘
```

**已登录态：**
```
┌─────────────────────────────┐
│  用户头像  用户昵称          │  ← 点击触发昵称/头像更新
├─────────────────────────────┤
│  我的收藏  →                 │  占位，点击提示"敬请期待"
│  语言设置  →                 │  navigateTo /pages/language/language
│  长辈模式  →                 │  navigateTo /pages/elder/elder
│  关于 TripMate →             │  弹出版本信息 modal
├─────────────────────────────┤
│  退出登录                    │
└─────────────────────────────┘
```

**昵称头像更新：**  
点击头像区域 → `open-type="chooseAvatar"` 更新头像 → `<input type="nickname">` 更新昵称 → 调 `updateProfile` 同步后端。

**Elder mode：** 所有字体尺寸使用 `rpx()` 函数，不使用内联 rpx 字面量。

### 4.4 修改 `App.vue`

在 `onLaunch` 中调用 `loadFromStorage()` 恢复登录状态。

---

## 5. 错误处理

| 场景 | 处理方式 |
|------|---------|
| `uni.login()` 失败 | toast 提示"登录失败，请重试" |
| 微信 jscode2session 返回错误码 | 后端返回 400 + 错误信息，前端 toast 展示 |
| token 过期（401） | `useApi.ts` 已有处理：清除 token，toast 提示重新登录 |
| 网络异常 | `useApi.ts` 已有处理：toast "网络异常，请稍后重试" |

---

## 6. 不在本次范围内

- 手机号获取（需企业主体 + 付费）
- 「我的收藏」功能实现（仅占位入口）
- token 自动续期
