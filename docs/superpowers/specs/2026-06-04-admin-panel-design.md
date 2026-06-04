# TripMate 后台管理面板设计文档

**日期**：2026-06-04  
**状态**：已批准，待实现

---

## 概述

为 TripMate 小程序搭建一个独立的 Web 后台管理面板，供管理员通过浏览器管理系统数据与配置。前端为独立 Vite 项目，后端在现有 Spring Boot 基础上扩展。

---

## 架构

### 前端

- **目录**：`admin/`（与 `frontend/`、`src/` 并列）
- **端口**：开发环境 `localhost:5173`
- **技术栈**：Vue 3 + TypeScript + Vite + Element Plus + Vue Router 4 + Pinia + Axios
- **与后端通信**：所有请求打到 `http://localhost:8080/api/admin/**`，开发环境通过 Vite proxy 转发

### 后端扩展

- 在现有 Spring Boot 项目（`src/`）中新增管理模块
- 接口统一前缀 `/api/admin/**`，与小程序接口 `/api/**` 分开
- 新增依赖：`spring-boot-starter-data-jpa`、`mysql-connector-j`、`spring-boot-starter-security`、`jjwt`

---

## 数据库（MySQL）

### `admin_user` 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| username | VARCHAR(64) UNIQUE NOT NULL | 登录用户名 |
| password | VARCHAR(255) NOT NULL | bcrypt 加密密码 |
| role | ENUM('SUPER_ADMIN','ADMIN') NOT NULL | 角色 |
| status | TINYINT DEFAULT 1 | 1=启用，0=禁用 |
| created_at | DATETIME | 创建时间 |

### `system_config` 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| config_key | VARCHAR(128) UNIQUE NOT NULL | 配置键，如 `weather.api.key` |
| config_value | TEXT | 配置值 |
| description | VARCHAR(255) | 说明 |

> **初始超级管理员**：通过 SQL 脚本手动 INSERT，密码使用 bcrypt 加密后写入。

> **暂缓**：`wechat_user` 表和内容管理表等小程序用户登录及内容模块确定后再建。

---

## 权限模型

| 操作 | SUPER_ADMIN | ADMIN |
|------|-------------|-------|
| 查看 Dashboard | ✓ | ✓ |
| 管理员账号 CRUD | ✓ | ✗ |
| 系统配置读写 | ✓ | ✗ |
| 未来内容管理 | ✓ | ✓ |

---

## 前端页面

### 目录结构

```
admin/
├── src/
│   ├── api/
│   │   ├── auth.ts
│   │   ├── users.ts
│   │   ├── dashboard.ts
│   │   └── settings.ts
│   ├── views/
│   │   ├── login/
│   │   │   └── LoginView.vue
│   │   ├── dashboard/
│   │   │   └── DashboardView.vue
│   │   ├── users/
│   │   │   └── UsersView.vue
│   │   └── settings/
│   │       └── SettingsView.vue
│   ├── router/
│   │   └── index.ts        # 路由守卫：未登录跳 /login，权限不足跳 /dashboard
│   ├── stores/
│   │   └── auth.ts         # Pinia：存 token、用户信息、角色
│   ├── components/
│   │   └── AppLayout.vue   # 侧边栏 + 顶部栏布局
│   └── main.ts
├── index.html
├── vite.config.ts
└── package.json
```

### 页面清单

| 路由 | 组件 | 权限 | 说明 |
|------|------|------|------|
| `/login` | LoginView | 公开 | 账号密码登录，成功后存 token 跳转 `/dashboard` |
| `/dashboard` | DashboardView | 所有管理员 | 统计卡片（管理员数、系统配置项数、天气查询次数等） |
| `/users` | UsersView | 仅 SUPER_ADMIN | 管理员列表，支持新增、禁用/启用、删除、重置密码 |
| `/settings` | SettingsView | 仅 SUPER_ADMIN | 系统配置键值对的查看与编辑 |

### 路由守卫逻辑

1. 无 token → 跳 `/login`
2. 有 token 但角色为 `ADMIN` 访问 `/users` 或 `/settings` → 跳 `/dashboard`
3. 已登录访问 `/login` → 跳 `/dashboard`

---

## 后端新增接口

所有 `/api/admin/**` 接口（除登录外）需携带 `Authorization: Bearer <token>`。

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/admin/login` | 公开 | 返回 JWT token |
| GET | `/api/admin/dashboard` | 所有管理员 | 统计数据 |
| GET | `/api/admin/users` | SUPER_ADMIN | 管理员列表 |
| POST | `/api/admin/users` | SUPER_ADMIN | 新增管理员 |
| PUT | `/api/admin/users/{id}` | SUPER_ADMIN | 更新（状态/密码） |
| DELETE | `/api/admin/users/{id}` | SUPER_ADMIN | 删除管理员 |
| GET | `/api/admin/settings` | SUPER_ADMIN | 获取所有配置 |
| PUT | `/api/admin/settings/{key}` | SUPER_ADMIN | 更新配置值 |

---

## 数据流

```
Admin Browser (5173)
  └─ Axios + Bearer token
       └─ Vite proxy → Spring Boot (8080)
            └─ Spring Security 鉴权
                 └─ AdminController → Service → MySQL
```

---

## 扩展说明

- 侧边栏菜单由路由配置自动生成，新增模块只需加路由记录
- 后续加内容管理：新建 `content` 表 + `ContentController` + `ContentsView.vue`，无需改动现有结构
- 后续加小程序用户管理：新建 `wechat_user` 表 + 对应接口 + `WechatUsersView.vue`
