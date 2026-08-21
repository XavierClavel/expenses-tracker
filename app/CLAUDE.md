# CLAUDE.md

Guidance for working in this directory (`expenses-tracker/app`) — the **Bankable** app: Kotlin Multiplatform + Compose Multiplatform, shipping to Android and iOS from one codebase.

## What this is

A personal expenses/finance tracker, originally migrated **from React Native (Expo Router) to Kotlin + Jetpack Compose**, and now to **Kotlin Multiplatform**. The RN source still lives at `../frontend` and is the reference implementation: when building a screen that doesn't exist yet in Kotlin, read its RN counterpart there first, then improve the UX rather than copying it verbatim. The Ktor backend lives at `../backend` and is served in production at `https://moneymind.fyi`.

This directory is the Gradle project root. The git repo root is one level up (`expenses-tracker/`), which also contains the backend and the RN frontend.

## Layout

The Gradle module is nested one level deep — the module is `app/app/`, not `app/`. It's a single KMP module that also applies `com.android.application`, so the Android APK/AAB paths (and therefore fastlane and CI) are unchanged.

```
app/                              ← this directory (Gradle root, settings.gradle.kts -> "Bankable")
  app/                            ← the :app module
    build.gradle.kts              ← KMP targets, deps per source set, Android SDK config
    src/
      commonMain/kotlin/com/xavierclavel/bankable/
        App.kt                    ← BankableApp(): the whole UI, shared by both platforms
        navigation/AppNavigation.kt ← ALL routes + bottom nav (see "Navigation")
        api/                      ← one *Api.kt per feature + ApiClient.kt
        model/                    ← *In (request) / *Out (response) / *Dto data classes
        platform/                 ← `expect` declarations for everything platform-specific
        auth/ categories/ expenses/ accounts/ summary/ trends/ tags/ settings/  ← feature packages
        constants/                ← AppColors, AppIcons, AccountTypes, Currencies
        storage/                  ← TokenStorage, PersistentCookiesStorage, LocalePreferences
        ui/                       ← shared composables (e.g. SlidingToggle) + ui/theme/
        util/                     ← Dates, Numbers, Hsv, ExpressionEvaluator
      commonMain/composeResources/
        values/strings.xml        ← en (the default)
        values-fr/strings.xml     ← fr
        drawable/                 ← images referenced from Compose
      androidMain/kotlin/…        ← MainActivity, BankableApplication, `actual` implementations
      androidMain/AndroidManifest.xml
      androidMain/res/            ← launcher icons, splash theme, launcher label only
      iosMain/kotlin/…            ← MainViewController, `actual` implementations
  iosApp/                         ← the Xcode project that hosts the shared UI
  gradle/libs.versions.toml       ← version catalog; add/upgrade deps here
```

Each feature package follows the same shape: a `FeatureViewModel.kt` plus one Compose file per screen (`*Screen.kt`).

## Build & run

### Android

Run Gradle from this directory (`app/`):

```
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device/emulator
./gradlew test                   # JVM unit tests
./gradlew connectedAndroidTest   # instrumented tests (needs a device)
./gradlew lint
```

On Windows use `gradlew.bat`. The debug build uses `applicationIdSuffix = ".debug"`, so the debug and release apps install side by side.

- minSdk 24, targetSdk 36, compileSdk 36.1, Java 11, Kotlin 2.3.x, Compose Multiplatform (Material 3).
- `local.properties` (SDK path) and signing secrets (`*.jks`, `keystore.properties`) are gitignored — never commit them.

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme. The project's first build phase shells out to `./gradlew :app:embedAndSignAppleFrameworkForXcode`, which compiles `iosMain` + `commonMain` into a static `ComposeApp.framework` and embeds it — there's no separate step to run first.

Set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` before running on a device (it's empty by default, which is fine for the simulator).

To check the Kotlin side without Xcode's UI:

```
./gradlew compileKotlinIosSimulatorArm64      # type-check common + iosMain
./gradlew linkDebugFrameworkIosSimulatorArm64 # produce the framework
```

**A full Xcode install is required for anything that links** (`link*`, `embedAndSign*`) — Command Line Tools alone don't ship the iOS SDKs, and `xcrun` fails with exit 72. The `compileKotlin*` tasks work without it.

Targets are `iosArm64` (device) and `iosSimulatorArm64`. There's no `iosX64`: Compose Multiplatform no longer publishes an Intel-simulator build.

## Architecture & conventions

**Platform boundary.** Anything a platform does differently lives behind an `expect` in `commonMain/.../platform/`, with `actual`s in `androidMain` / `iosMain`. That's storage (`KeyValueStore` / `SecureStore` — SharedPreferences+EncryptedSharedPreferences vs. NSUserDefaults+Keychain), the Ktor engine (OkHttp vs. Darwin), locale-aware formatting (`java.text` vs. `NSNumberFormatter`/`NSDateFormatter`), toasts, dynamic color, language switching, and Google sign-in. Add to that package rather than reaching for a platform API from a screen. Android `actual`s that need a Context get it from `platform/AppContext.kt`, which `BankableApplication` and `MainActivity` populate.

**Resources.** All user-facing text is a Compose Multiplatform resource: `stringResource(Res.string.foo)`, with an entry in **both** `values/strings.xml` (en) and `values-fr/strings.xml` (fr). Accessors are extension properties, so each name needs its own import (`import com.xavierclavel.bankable.resources.foo`) alongside `…resources.Res`. `androidMain/res/` holds only what the Android platform itself needs — launcher icons, the splash theme, and the launcher label; the `values-fr` copy of the label is what tells Android the app supports French, which is what makes `LocalAppLocale` and resource lookup resolve to `fr` on a French device.

**Dates, numbers, locale.** No `java.util.Locale`/`SimpleDateFormat`/`String.format` in common code. Locales are BCP-47 tag `String`s read from `LocalAppLocale.current`; dates are "yyyy-MM-dd" handled by `util/Dates.kt` (kotlinx-datetime for arithmetic, `platform/Formatting.kt` for localized rendering); `"%.1f"` is `util/Numbers.kt`'s `formatFixed`.

**State / MVVM.** Each feature has a `ViewModel` extending `androidx.lifecycle.ViewModel` (the multiplatform artifact — same package name). Screens are stateless Composables that take the ViewModel(s) + `navController`. Patterns to match:
- List/collection state: `private val _x = MutableStateFlow(...)` exposed as `val x: StateFlow<...>`, collected with `collectAsState()`.
- Screen-local selection / form state: `var foo by mutableStateOf(...)  private set`, mutated via `setFoo(...)` helpers.
- Async work runs in `viewModelScope.launch { ... }` with try/catch; on error, `onError(e.message ?: "...")` is invoked rather than thrown. Loading is tracked with an `isLoading` flag.
- The "edit" screens are reused for create vs. update: the ViewModel exposes `prepareNewX()` / `prepareEditX(item)` before navigating, then `saveX(...)` branches on whether a `selectedX` is set.

**ViewModel lifecycle (important).** The session-scoped ViewModels (Categories, Expenses, Accounts, Summary, Trends, Tags) are created in `MainNavGraphContent` and hung off a custom `SessionViewModelStoreOwner` that is cleared on logout (`DisposableEffect.onDispose`). This guarantees a fresh login rebuilds them with the new account's data. Don't move these into per-screen `viewModel()` scopes.

**Navigation.** All routes are registered centrally in `navigation/AppNavigation.kt` as string routes (e.g. `"category/edit"`, `"account/report/edit"`). There are no typed nav args — data is passed between screens via the shared ViewModels' `selected*` state, not route parameters. Top-level tabs are in `TOP_LEVEL_ROUTES`; the bottom bar only shows on those. To add a screen: add a `composable("...")` entry and navigate to it after calling the relevant `prepare*` on the ViewModel.

**Networking.** Ktor, configured once in `api/ApiClient.kt` and built by each platform's entry point calling `initHttpClient()`:
- One top-level `suspend fun apiXxx(...)` per endpoint in `api/<Feature>Api.kt`, named `apiList/apiCreate/apiUpdate/apiDelete...`. They use the global `httpClient` and `BASE_URL`.
- Always attach auth with the `authHeader()` request extension.
- The API layer often defines `private @Serializable` wire DTOs (`*Response`) and maps them to the public `model/` types, so backend shape changes stay contained in the Api file.
- Errors: a `HttpResponseValidator` throws `ApiException(status, body)` on any non-2xx; a 401 also emits `unauthorizedFlow`, which the auth layer observes to force logout. Don't add per-call status checks — let `ApiException` propagate to the ViewModel's catch.
- Session: cookie-based via `PersistentCookiesStorage` plus an optional bearer `sessionToken`. `clearSessionCookies()` is called on logout.

**Models.** Plain `data class`es in `model/`. `*In` = request bodies (must be `@Serializable`), `*Out` = UI-facing types, `*Dto` = analytics/aggregate payloads.

**UI.** Material 3, theme in `ui/theme/`. Reusable widgets go in `ui/`. Colors/icons/currencies are centralized in `constants/`.

## Platform gaps to know about

- **Google sign-in is Android-only.** `platform/GoogleSignIn.kt` gates it behind `isGoogleSignInSupported`, and `GoogleSignInButton` renders nothing when it's false. Wiring up iOS needs the GoogleSignIn iOS SDK, its own OAuth client ID and a URL scheme.
- **Language switching applies immediately on Android, on next launch on iOS.** Android recreates the Activity (`AndroidLocaleManager.applyStoredLocale` in `attachBaseContext`); iOS can only write `AppleLanguages`, which NSLocale re-reads at launch. `languageChangeAppliesImmediately` drives the hint shown in Settings.
- **Material You** (dynamic color) is Android 12+ only; iOS falls back to the app palette.

## Conventions to honor

- Kotlin official code style (`kotlin.code.style=official`).
- Match the surrounding file: trailing-comma argument lists, `apiXxx` naming, `prepare*/save*` ViewModel methods, stateless screens.
- Add new dependencies through `gradle/libs.versions.toml` (version catalog), not inline version strings.
- Don't introduce a DI framework, Retrofit, or extra state libs — the project deliberately uses plain ViewModels + Ktor + manual wiring.
- `android.builtInKotlin=false` / `android.newDsl=false` in `gradle.properties` are required: AGP 9 refuses to apply `com.android.application` alongside the KMP plugin without them. Don't remove them without moving to the two-module (`com.android.kotlin.multiplatform.library` + app) layout.
