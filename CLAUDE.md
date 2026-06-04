# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TripMate is a travel assistant mini-program (微信小程序) with a Spring Boot backend and a separate Vue 3 web admin panel. The mini-program frontend targets WeChat Mini Program via UniApp (uni-app) + Vue 3 Composition API; the backend is a Spring Boot 4 / Java 21 REST API; the admin panel is a standalone Vite + Vue 3 web app.

## Architecture

### Backend (`src/`)
- **Spring Boot 4**, Java 21, Maven, Lombok
- Single module: `com.LHZ.TripMate`
- Layer structure: `controller` → `service/impl` → repository / external HTTP
- `common/Result<T>` + `common/ResultCode` are the unified API response envelope used by every endpoint
- Weather data is fetched from QWeather (和风天气) via `RestClient`; config in `application.yaml` under `weather.api.path` / `weather.api.key`
- CORS is configured globally in `config/CorsConfig.java`
- **Database**: MySQL (`tripmate` database on `47.109.38.44:3306`); JPA with `ddl-auto: update`
- **Auth**: Spring Security 6 + JWT (jjwt 0.12); stateless; token signed with secret in `application.yaml` under `jwt.secret`
- Admin API prefix: `/api/admin/**` — all endpoints except `/api/admin/login` require `Authorization: Bearer <token>`
- Mini-program API prefix: `/api/**` (e.g. `/api/weather`)

#### Backend Package Structure
```
com.LHZ.TripMate
├── common/          Result<T>, ResultCode
├── config/          CorsConfig, SecurityConfig, WeatherConfig
├── controller/
│   ├── admin/       AdminAuthController, AdminUserController,
│   │                SystemConfigController, DashboardController
│   └── WeatherController
├── dto/
│   └── admin/       LoginRequestDTO, LoginResponseDTO, AdminUserDTO,
│                    CreateAdminUserDTO, UpdateAdminUserDTO, DashboardDTO
├── entity/          AdminUser (Role enum: SUPER_ADMIN/ADMIN), SystemConfig
├── repository/      AdminUserRepository, SystemConfigRepository
├── security/        AdminUserDetails, JwtAuthFilter
├── service/         AdminAuthService, AdminUserService,
│                    SystemConfigService, DashboardService, WeatherService
├── service/impl/    implementations
└── util/            JwtUtil
```

### Mini-Program Frontend (`frontend/`)
- **UniApp + Vue 3 Composition API + TypeScript**
- Target platform: WeChat Mini Program (`mp-weixin`); compiled output lives in `unpackage/dist/dev/mp-weixin/` — **do not edit compiled output**
- Entry: `main.ts` → registers `vue-i18n` → `App.vue`
- Pages declared in `pages.json`: `index` (home), `guide`, `elder`, `language`, `mine`
- Navigation uses `uni.switchTab()` via the custom `TabBar` component (pages must be declared as `tabBar` pages or use `uni.navigateTo`)

#### Mini-Program Key Patterns
- **API layer** (`frontend/api/`): thin wrappers over `useApi` composable (`frontend/utils/useApi.ts`), which wraps `uni.request`. Base URL is `http://localhost:8080`. Token is read from `uni.getStorageSync('token')` and sent as `Authorization: Bearer <token>`.
- **i18n**: `vue-i18n` with `zh` (default) and `en` locales in `frontend/locales/`. Weather condition keys follow `weather.condition.<中文天气字符串>` — backend returns raw Chinese strings, frontend translates them.
- **Elder mode** (`frontend/composables/useElder.ts`): module-level singleton reactive state; provides `rpx(base)` helper that scales rpx values by `fontScale` (1× normal, 1.4× elder). All font sizes in components must use `rpx()` instead of inline rpx literals to support elder mode.
- **Response contract**: `{ code: number, message: string, data: T }` — matches `com.LHZ.TripMate.common.Result<T>`.

### Admin Panel (`admin_frontend/`)
- **Vue 3 + TypeScript + Vite**, port 5173
- Tech stack: Element Plus + Vue Router 4 + Pinia + Axios
- Vite proxy: `/api` → `http://localhost:8080`
- Auth: JWT stored in `localStorage` (`admin_token`, `admin_username`, `admin_role`)

#### Admin Panel Structure
```
admin_frontend/src/
├── api/
│   ├── http.ts        Axios instance; request interceptor adds Bearer token;
│   │                  response interceptor redirects to /login on 401
│   ├── auth.ts        login()
│   ├── dashboard.ts   getDashboard()
│   ├── users.ts       listUsers / createUser / updateUser / deleteUser
│   └── settings.ts    listSettings / updateSetting
├── stores/
│   └── auth.ts        Pinia store: token, username, role, isLoggedIn, isSuperAdmin
├── router/
│   └── index.ts       Routes + guards: unauthenticated→/login,
│                      ADMIN role cannot access /users or /settings
├── components/
│   └── AppLayout.vue  Sidebar + topbar layout shell
└── views/
    ├── login/         LoginView.vue
    ├── dashboard/     DashboardView.vue
    ├── users/         UsersView.vue  (SUPER_ADMIN only)
    └── settings/      SettingsView.vue  (SUPER_ADMIN only)
```

#### Role-based Access
| Page | SUPER_ADMIN | ADMIN |
|------|-------------|-------|
| Dashboard | ✓ | ✓ |
| Users | ✓ | ✗ |
| Settings | ✓ | ✗ |

## Commands

### Backend
```bash
# Run (from repo root)
./mvnw spring-boot:run

# Build JAR
./mvnw package -DskipTests

# Run tests
./mvnw test
```

### Mini-Program Frontend
The mini-program is developed inside **HBuilderX** (Uni-App's official IDE). There is no standalone npm dev-server command — compilation is triggered from HBuilderX (`运行 → 运行到小程序模拟器 → 微信开发者工具`).

```bash
# Type-check only (from frontend/)
npx vue-tsc --noEmit
```

Compiled output (`frontend/unpackage/dist/dev/mp-weixin/`) is loaded into **WeChat DevTools** for preview and debugging.

### Admin Panel
```bash
# Dev server (from admin_frontend/)
cd admin_frontend && npm run dev

# Type-check
cd admin_frontend && npx vue-tsc --noEmit

# Build
cd admin_frontend && npm run build
```

## Adding a New Mini-Program Page

1. Create `frontend/pages/<name>/<name>.vue`
2. Register it in `frontend/pages.json` under `pages`
3. Add a navigation entry to `TabBar.vue` tabs array if it appears in the bottom bar

## Adding a New Backend API Endpoint

1. Add DTO classes under `src/main/java/.../dto/`
2. Implement `Service` interface + `ServiceImpl`
3. Expose via `@RestController` under `/api/` prefix (mini-program) or `/api/admin/` prefix (admin panel)
4. For admin endpoints requiring SUPER_ADMIN only: add `@PreAuthorize("hasRole('SUPER_ADMIN')")` on the controller class

## Adding a New Admin Panel Module

1. Add API functions in `admin_frontend/src/api/<domain>.ts`
2. Create view at `admin_frontend/src/views/<name>/<Name>View.vue`
3. Add route in `admin_frontend/src/router/index.ts` (add `meta: { requiresSuperAdmin: true }` if SUPER_ADMIN only)
4. Add menu item in `admin_frontend/src/components/AppLayout.vue`
5. Add corresponding backend Controller + Service + Entity if needed

## Initial Admin Setup (one-time)

After first `./mvnw spring-boot:run` (which auto-creates tables), insert the first super admin on MySQL:

```sql
INSERT INTO admin_user (username, password, role, status)
VALUES ('admin', '$2a$10$gz3vlC9sdkni7yf/qYq3ROK/BjdpnweuEze0cyXaXpl3FdvRAmBpS', 'SUPER_ADMIN', 1);
-- Default password: admin123 — change after first login
```
