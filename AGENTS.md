# AGENTS.md: Bridge Fit

Instructions for AI coding agents (Claude Code, Codex, Gemini, etc.) and humans working on this repository.

## What this project is

**Bridge Fit** is a personal fork of [Gadgetbridge](https://codeberg.org/Freeyourgadget/Gadgetbridge) that:

1. Replaces the main user interface with a new one closely inspired by **Apple Fitness** (layout, hierarchy, activity
   rings, cards, typography rhythm), written in **Jetpack Compose**.
2. Will later add a **gesture control** feature for the Colmi R09 smart ring (accelerometer-based taps and hand
   movements, with an activation gesture, to control the phone, Home Assistant and other interfaces).

Everything else (device support, BLE protocol, sync, database, Health Connect export, device settings) **is
Gadgetbridge** and must keep working for every device Gadgetbridge supports, not only the R09.

The product vision (in Brazilian Portuguese) lives in [`docs/vision.pt-BR.md`](docs/vision.pt-BR.md).

## The one rule: stay mergeable with upstream

We regularly merge Gadgetbridge's `master` into this fork. Every change must be designed so these merges stay cheap.

- **Put new code in the `bridgefit` source set**: `app/src/bridgefit/`, Kotlin package `com.apenasangelo.bridgefit`.
  It is only compiled into the `bridgefit` product flavor.
- **Build configuration for the fork goes in [`app/bridgefit.gradle`](app/bridgefit.gradle)**, which
  `app/build.gradle` pulls in with a single `apply from:` line.
- **Do not edit upstream files** (`app/src/main/**`, `app/build.gradle`, `gradle/libs.versions.toml`, …) unless there is
  no other way. When it is unavoidable, keep the diff to a few lines, and list the file in
  [Upstream touch points](#upstream-touch-points) below.
- **Override instead of editing.** Use manifest merging (`tools:node="remove"` / `tools:replace`), resource overlays in
  `app/src/bridgefit/res/`, and composition (calling upstream classes) rather than changing them.
- **Reuse upstream data code; never re-implement it.** The UI reads data through the same helpers the upstream
  dashboard uses: the `GBWidget.loadData()` implementations in `nodomain.freeyourgadget.gadgetbridge.widgets.impl`,
  `WidgetDataScope`, `DeviceCoordinator` sample providers, and `DeviceManager` / `GBDevice` for device state. If
  upstream improves a calculation, we get it for free.
- **Keep the upstream screens reachable.** Device settings, preferences, charts, data management, debug and discovery
  screens are upstream activities that the new UI links to. The classic main screen (`ControlCenterv2`) stays
  available from Settings → "Classic interface".
- Do not rename packages, the `namespace`, or move upstream files.

### Upstream touch points

Every upstream file this fork modifies. Check this list when resolving merge conflicts.

| File | Change | Why |
| --- | --- | --- |
| `app/build.gradle` | `alias(libs.plugins.kotlin.compose)` in `plugins {}` and `apply from: "bridgefit.gradle"` | Compose compiler and fork build config |
| `gradle/libs.versions.toml` | `kotlin-compose` plugin entry | The Compose compiler plugin must share the Kotlin version |
| `build.gradle.kts` | `alias(libs.plugins.kotlin.compose) apply false` | Loads the plugin once, next to the other Kotlin plugins |

## Git workflow

Remotes:

- `origin`: `https://github.com/ApenasAngelo/bridge-fit` (ours, public, AGPL-3.0).
- `upstream`: `https://codeberg.org/Freeyourgadget/Gadgetbridge` (fetch only; push is disabled on purpose).

Branches:

- `main`: release branch. Only receives merges from `dev`. Each merge to `main` is something installable.
- `dev`: integration branch. Day-to-day work lands here.
- `feat/<topic>`, `fix/<topic>`, …: short-lived branches cut from `dev`, merged back into `dev`.

Syncing upstream (on `dev`, never rebase; always a merge commit so history stays honest):

```sh
git fetch upstream
git switch dev
git merge upstream/master        # resolve conflicts, see "Upstream touch points"
./gradlew :app:assembleBridgefitDebug
```

Never push to `upstream` and never open pull requests against Gadgetbridge from this fork.

### Commits

- [Conventional Commits](https://www.conventionalcommits.org/), **in English**: `feat(ui): add activity rings card`.
- The subject line is short, imperative and direct. Use the body only when the change needs explaining.
- Common scopes: `ui`, `theme`, `summary`, `health`, `devices`, `gestures`, `build`, `docs`.
- Do not add AI attribution trailers.

## Building

Requirements: JDK 21 (the upstream toolchain pins `languageVersion = 21`), Android SDK with platform 37.

```sh
./gradlew :app:assembleBridgefitDebug          # our app, installs next to Gadgetbridge
./gradlew :app:installBridgefitDebug           # install on the connected phone
./gradlew :app:assembleMainlineDebug           # plain upstream Gadgetbridge, to compare behaviour
./gradlew :app:testBridgefitDebugUnitTest      # unit tests (Robolectric)
```

Do not run a bare `./gradlew assemble` because it builds every flavor and build type.

The `bridgefit` flavor uses `applicationId = "com.apenasangelo.bridgefit"`, so it installs side by side with an
existing Gadgetbridge. Only one of them should be paired with a given device at a time, since two apps cannot share
one BLE connection. Data moves between them through Gadgetbridge's Data management → Export / Import database.

## Development practices (inherited from Gadgetbridge)

Gadgetbridge's [`CONTRIBUTING.md`](CONTRIBUTING.md) applies here too:

- Test what you change: on the real device when it touches device behaviour, and with unit tests (JUnit +
  Robolectric, under `app/src/test`) for logic. UI code should keep logic in plain, testable functions or ViewModels.
- Clean-room only: reverse engineering must come from public information or our own observation of the device
  (e.g. BLE captures of our own ring). No leaked, decompiled or proprietary sources.
- Keep the AGPL-3.0 license, keep every copyright header, and credit Gadgetbridge and its contributors in the app.
- Match the surrounding code style. New code is Kotlin.

## UI guidelines

The target is "Apple Fitness, for Android". Before creating a screen, check how Apple Fitness presents the same idea
(Summary, Activity rings, Trends, Sleep, Heart) and follow its information hierarchy.

- **Theme**: dark by default (true black background, `#1C1C1E` cards) and a light iOS-like variant (`#F2F2F7`
  background, white cards), following the system setting. Colors are tokens in `ui/theme/`; never hard-code a color
  in a screen.
- **Rings and metric colors**: Move (red-pink), Exercise (green-yellow), Stand/steps (cyan), plus fixed accent colors
  per metric (heart = red, sleep = indigo/teal, SpO₂ = blue, temperature = orange, …), all defined in the theme.
- **Typography**: large bold titles (`Summary`, `Sleep`), rounded numeric values with small unit labels next to them,
  and gray secondary text. The system sans-serif is used; **do not bundle Apple fonts (SF Pro) or SF Symbols**,
  because the repository is public. Use Material Symbols or our own vector drawings.
- **Charts** are drawn with Compose `Canvas` in the Apple style: thin rounded bars, no grid noise, and sparse axis
  labels.
- Strings go in `app/src/bridgefit/res/values/strings.xml` (English) with a `values-pt-rBR` translation. Reuse upstream
  string resources (`nodomain.freeyourgadget.gadgetbridge.R.string.*`) when an exact match exists.

## Code map

```
app/bridgefit.gradle                      fork build config: flavor, Compose dependencies
app/src/bridgefit/AndroidManifest.xml     new launcher activity; removes ControlCenterv2's launcher entry
app/src/bridgefit/java/com/apenasangelo/bridgefit/
  BridgeFitActivity.kt                    entry point (Compose host, first-run and permission flow)
  ClassicRedirect.kt                      sends upstream launches of ControlCenterv2 (notification, Up, pairing)
                                          to BridgeFitActivity, unless opened as "Classic interface"
  ui/theme/                               colors, typography, shapes (dark + light)
  ui/components/                          rings, cards, charts, headers
  ui/summary/                             Summary screen (rings + metric cards)
  ui/metric/                              per-metric detail screens
  ui/devices/                             devices screen (battery, connection, sync, device settings)
  data/                                   repositories that adapt upstream widgets/providers to UI state
```

## Known gaps of the new UI

- Weekly/monthly trends are not native yet; detail screens link to the upstream charts ("Show All Charts").
- Ring gradients show a small seam at 12 o'clock when a ring passes 100%.
- The launcher icon is replaced only on Android 8+ (adaptive icon); older versions keep the Gadgetbridge PNGs.
- Upstream screens (settings, device settings, charts, pairing) keep the Gadgetbridge Material look.

## Roadmap

1. **Now**: new Apple Fitness-style UI over the complete Gadgetbridge app, used daily instead of Gadgetbridge.
2. **Next**: gestures for the Colmi R09 (see `docs/vision.pt-BR.md`). Open research:
   - Which raw sensor streams the R09 exposes (accelerometer, a possible gyroscope, sampling rate), and the battery
     cost of streaming them. The in-tree driver is `devices/yawell/ring` and `service/devices/yawell/ring`. Community
     clients for the Colmi R02-family document raw accelerometer commands.
   - Whether upstream Gadgetbridge has gesture-related work. This has not been verified yet, so search the Codeberg
     issues and PRs before designing.
   - An activation gesture with a very low false-positive rate, trained on our own "natural use" negative data.
3. **Later**: Home Assistant integration, interface navigation, and CI release workflows on GitHub.
