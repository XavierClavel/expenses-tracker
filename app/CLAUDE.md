# CLAUDE.md

Guidance for working in this directory (`expenses-tracker/app`) — the **Bankable** native Android app.

## What this is

A personal expenses/finance tracker for Android, being migrated **from React Native (Expo Router) to Kotlin + Jetpack Compose**. The RN source still lives at `../frontend` and is the reference implementation: when building a screen that doesn't exist yet in Kotlin, read its RN counterpart there first, then improve the UX rather than copying it verbatim. The Ktor backend lives at `../backend` and is served in production at `https://moneymind.fyi`.

This directory is the Android Gradle project. The git repo root is one level up (`expenses-tracker/`), which also contains the backend and the RN frontend.

## Layout

The Gradle module is nested one level deep — the app module is `app/app/`, not `app/`.

```
app/                              ← this directory (Gradle root, settings.gradle.kts -> "Bankable")
  app/                            ← the :app module
    build.gradle.kts              ← module deps & SDK config
    src/main/java/com/xavierclavel/bankable/
      MainActivity.kt             ← single Activity; sets up Compose + locale
      BankableApplication.kt      ← Application; initHttpClient lives here
      navigation/AppNavigation.kt ← ALL routes + bottom nav (see "Navigation")
      api/                        ← one *Api.kt per feature + ApiClient.kt
      model/                      ← *In (request) / *Out (response) / *Dto data classes
      auth/ categories/ expenses/ accounts/ summary/ trends/ settings/   ← feature packages
      constants/                  ← AppColors, AppIcons, Currencies
      storage/                    ← TokenStorage, PersistentCookiesStorage, LocalePreferences
      ui/                         ← shared composables (e.g. SlidingToggle) + ui/theme/
    src/main/res/values[-fr]/strings.xml   ← localized strings (en + fr)
  gradle/libs.versions.toml       ← version catalog; add/upgrade deps here
```

Each feature package follows the same shape: a `FeatureViewModel.kt` plus one Compose file per screen (`*Screen.kt`).

## Build & run

Run Gradle from this directory (`app/`):

```
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device/emulator
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs a device)
./gradlew lint
```

On Windows use `gradlew.bat`. The debug build uses `applicationIdSuffix = ".debug"`, so the debug and release apps install side by side.

- minSdk 24, targetSdk/compileSdk 36, Java 11, Kotlin 2.2.x, Compose (Material 3).
- `local.properties` (SDK path) and signing secrets (`*.jks`, `keystore.properties`) are gitignored — never commit them.

## Architecture & conventions

**State / MVVM.** Each feature has a `ViewModel` extending `androidx.lifecycle.ViewModel`. Screens are stateless Composables that take the ViewModel(s) + `navController`. Patterns to match:
- List/collection state: `private val _x = MutableStateFlow(...)` exposed as `val x: StateFlow<...>`, collected with `collectAsState()`.
- Screen-local selection / form state: `var foo by mutableStateOf(...)  private set`, mutated via `setFoo(...)` helpers.
- Async work runs in `viewModelScope.launch { ... }` with try/catch; on error, `onError(e.message ?: "...")` is invoked rather than thrown. Loading is tracked with an `isLoading` flag.
- The "edit" screens are reused for create vs. update: the ViewModel exposes `prepareNewX()` / `prepareEditX(item)` before navigating, then `saveX(...)` branches on whether a `selectedX` is set.

**ViewModel lifecycle (important).** The five session-scoped ViewModels (Categories, Expenses, Accounts, Summary, Trends) are created in `MainNavGraphContent` and hung off a custom `SessionViewModelStoreOwner` that is cleared on logout (`DisposableEffect.onDispose`). This guarantees a fresh login rebuilds them with the new account's data. Don't move these into per-screen `viewModel()` scopes.

**Navigation.** All routes are registered centrally in `navigation/AppNavigation.kt` as string routes (e.g. `"category/edit"`, `"account/report/edit"`). There are no typed nav args — data is passed between screens via the shared ViewModels' `selected*` state, not route parameters. Top-level tabs are in `TOP_LEVEL_ROUTES`; the bottom bar only shows on those. To add a screen: add a `composable("...")` entry and navigate to it after calling the relevant `prepare*` on the ViewModel.

**Networking.** Ktor (`Android` engine) configured once in `api/ApiClient.kt`:
- One top-level `suspend fun apiXxx(...)` per endpoint in `api/<Feature>Api.kt`, named `apiList/apiCreate/apiUpdate/apiDelete...`. They use the global `httpClient` and `BASE_URL`.
- Always attach auth with the `authHeader()` request extension.
- The API layer often defines `private @Serializable` wire DTOs (`*Response`) and maps them to the public `model/` types, so backend shape changes stay contained in the Api file.
- Errors: a `HttpResponseValidator` throws `ApiException(status, body)` on any non-2xx; a 401 also emits `unauthorizedFlow`, which the auth layer observes to force logout. Don't add per-call status checks — let `ApiException` propagate to the ViewModel's catch.
- Session: cookie-based via `PersistentCookiesStorage` plus an optional bearer `sessionToken`. `clearSessionCookies()` is called on logout.

**Models.** Plain `data class`es in `model/`. `*In` = request bodies (must be `@Serializable`), `*Out` = UI-facing types, `*Dto` = analytics/aggregate payloads.

**UI.** Material 3, theme in `ui/theme/`. Reusable widgets go in `ui/`. Colors/icons/currencies are centralized in `constants/`. All user-facing text must be a `stringResource(R.string.…)` with both `values/strings.xml` (en) and `values-fr/strings.xml` (fr) entries — the app supports runtime locale switching via `LocaleManager` / `LocalePreferences`.

## Migration status

Done: Auth (incl. Google sign-in), Categories + Subcategories CRUD, color/icon pickers, Expenses, Accounts, Summary, Trends, Settings, 5-tab bottom nav. When a screen looks incomplete, compare against `../frontend` (RN) for intended behavior. The auto-memory `project-migration` tracks higher-level roadmap.

## Conventions to honor

- Kotlin official code style (`kotlin.code.style=official`).
- Match the surrounding file: trailing-comma argument lists, `apiXxx` naming, `prepare*/save*` ViewModel methods, stateless screens.
- Add new dependencies through `gradle/libs.versions.toml` (version catalog), not inline version strings.
- Don't introduce a DI framework, Retrofit, or extra state libs — the project deliberately uses plain ViewModels + Ktor + manual wiring.
