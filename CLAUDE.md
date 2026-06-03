# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TripMate is a travel assistant mini-program (微信小程序) with a Spring Boot backend. The frontend targets WeChat Mini Program via UniApp (uni-app) + Vue 3 Composition API; the backend is a Spring Boot 4 / Java 21 REST API.

## Architecture

### Backend (`src/`)
- **Spring Boot 4**, Java 21, Maven, Lombok
- Single module: `com.LHZ.TripMate`
- Layer structure: `controller` → `service/impl` → external HTTP (QWeather API)
- `common/Result<T>` + `common/ResultCode` are the unified API response envelope used by every endpoint
- Weather data is fetched from QWeather (和风天气) via `RestClient`; config in `application.yaml` under `weather.api.path` / `weather.api.key`
- CORS is configured globally in `config/CorsConfig.java`
- No database yet — data is fetched live from third-party APIs

### Frontend (`frontend/`)
- **UniApp + Vue 3 Composition API + TypeScript**
- Target platform: WeChat Mini Program (`mp-weixin`); compiled output lives in `unpackage/dist/dev/mp-weixin/` — **do not edit compiled output**
- Entry: `main.ts` → registers `vue-i18n` → `App.vue`
- Pages declared in `pages.json`: `index` (home), `guide`, `elder`, `language`, `mine`
- Navigation uses `uni.switchTab()` via the custom `TabBar` component (pages must be declared as `tabBar` pages or use `uni.navigateTo`)

### Frontend Key Patterns
- **API layer** (`frontend/api/`): thin wrappers over `useApi` composable (`frontend/utils/useApi.ts`), which wraps `uni.request`. Base URL is `http://localhost:8080`. Token is read from `uni.getStorageSync('token')` and sent as `Authorization: Bearer <token>`.
- **i18n**: `vue-i18n` with `zh` (default) and `en` locales in `frontend/locales/`. Weather condition keys follow `weather.condition.<中文天气字符串>` — backend returns raw Chinese strings, frontend translates them.
- **Elder mode** (`frontend/composables/useElder.ts`): module-level singleton reactive state; provides `rpx(base)` helper that scales rpx values by `fontScale` (1× normal, 1.4× elder). All font sizes in components must use `rpx()` instead of inline rpx literals to support elder mode.
- **Response contract**: `{ code: number, message: string, data: T }` — matches `com.LHZ.TripMate.common.Result<T>`.

## Commands

### Backend
```bash
# Run (from repo root)
./mvnw spring-boot:run

# Build JAR
./mvnw package -DskipTests

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=WeatherServiceTest
```

### Frontend
The frontend is developed inside **HBuilderX** (Uni-App's official IDE). There is no standalone npm dev-server command — compilation is triggered from HBuilderX (`运行 → 运行到小程序模拟器 → 微信开发者工具`).

```bash
# Type-check only (from frontend/)
npx vue-tsc --noEmit
```

Compiled output (`frontend/unpackage/dist/dev/mp-weixin/`) is loaded into **WeChat DevTools** for preview and debugging.

## Adding a New Page

1. Create `frontend/pages/<name>/<name>.vue`
2. Register it in `frontend/pages.json` under `pages`
3. Add a navigation entry to `TabBar.vue` tabs array if it appears in the bottom bar

## Adding a New API Endpoint

1. Add DTO classes under `src/main/java/.../dto/`
2. Implement `Service` interface + `ServiceImpl`
3. Expose via `@RestController` under `/api/` prefix
4. Add a corresponding function in `frontend/api/<domain>.ts` using `useApi().post()`
