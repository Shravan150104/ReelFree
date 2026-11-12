# ReelFree: personal Instagram Reels blocker (Phase 1 MVP)

ReelFree is an Android app (Kotlin, Jetpack Compose, MVVM) that uses an **AccessibilityService** to notice when Instagram shows its full-screen Reels viewer or the Reels tab. When it does, ReelFree presses Back for you, or taps the Home tab when you're on the Reels tab. The rest of Instagram (DMs, Stories, posts, profiles, search) keeps working.

> **Status: honest summary**
> - The project **compiles**, and the **24 JVM unit tests pass** (detection rules, confirmation gate, cooldown/loop guard).
> - It has **not been tested on a real phone with Instagram**. The detection rules use view-ID fragments (`clips_viewer`, `reel_viewer`, `row_thread_composer`) that are *community-reported*, not documented by Instagram. They must be confirmed on your device with the built-in **Dry run + Debug mode** tooling ([Calibration](#calibrating-detection-on-your-phone)) before you trust the blocker.
> - Phase 2 (statistics, more entry points, tuning) and Phase 3 (Explore, YouTube Shorts, settings screen, sensitivity) are **not built yet**, so the UI has no switches for them.

---

## 1. Architecture

```
Instagram UI ──AccessibilityEvent──▶ ReelFreeAccessibilityService   (main thread: only schedules)
                                          │ debounce (80–200 ms, 500 ms max latency)
                                          ▼  worker HandlerThread
                                   NodeSnapshotter  ── privacy filter: no editable/password fields,
                                          │            labels kept only if in LabelVocabulary
                                          ▼
                                   ScreenSnapshot (view IDs, classes, sizes, vocab labels)
                                          ▼
                       ScreenDetector ─▶ InstagramReelsDetector ─▶ DetectionRuleEngine(InstagramReelsRules)
                                          │  confidence = Σ signal weights (cap 100), any VETO ⇒ no block
                                          ▼
                                   ConfirmationGate   (2 consecutive positives within 1.5 s)
                                          ▼
                                   BlockingActionController (1.5 s cooldown; 4 actions/15 s ⇒ 30 s lockout)
                                          ▼
                       GLOBAL_ACTION_BACK  or  tap Home tab   (Dry run: log only)
```

| Component | File | Notes |
|---|---|---|
| `ReelFreeAccessibilityService` | `service/ReelFreeAccessibilityService.kt` | Thin shell: event scheduling, threading, lifecycle, actions |
| `NodeSnapshotter` | `service/NodeSnapshotter.kt` | Walks the node tree (≤1500 nodes, depth ≤50) and recycles nodes on API < 33 |
| `ScreenDetector` interface | `detection/ScreenDetector.kt` | One per target. Explore and Shorts will plug in here (Phase 3) |
| `InstagramReelsDetector` | `detection/instagram/InstagramReelsDetector.kt` | Chooses BACK or CLICK_HOME_TAB |
| `InstagramReelsRules` | `detection/instagram/InstagramReelsRules.kt` | **The file to edit when Instagram changes** |
| `DetectionRuleEngine`, `Condition`, `DetectionRule` | `detection/rules/` | Declarative, pure Kotlin, unit-tested |
| `ConfirmationGate`, `BlockingActionController` | `service/BlockingActionController.kt` | Pure Kotlin, unit-tested with a fake clock |
| `ProtectionPreferencesRepository` | `data/` | DataStore Preferences |
| `DiagnosticsLog` | `diagnostics/` | In-memory only; exported only when you tap Share |
| UI | `ui/`, `MainActivity.kt` | Compose + Material 3, dynamic colour, light/dark |

### Why these detection rules

| Rule | Kind | Weight | Idea | Confirmed on device |
|---|---|---|---|---|
| `ig.reels.clips_viewer_id` | signal | 70 | View whose ID contains `clips_viewer` and covers ≥90% width and ≥60% height (`clips_viewer_view_pager`). Instagram's code calls Reels "clips" | ✅ IG 455.0.0.51.72 |
| `ig.reels.clips_fullscreen_container` | signal | 30 | Any full-screen `clips*` view. Too weak to block on its own | ✅ |
| `ig.reels.tab_selected` | signal | 70 | Bottom-bar `clips_tab` labelled "Reels" with `isSelected` (bar sits at ~88% of the screen height) | ✅ |
| `ig.veto.story_viewer` | veto | – | `reel_viewer*` views (= **Stories** in Instagram's code) **without** the Reels pager. Never block. The Reels viewer opened from a DM also contains `reel_viewer_message_composer`, so `reel_viewer` alone is not enough | ⚠️ Stories not captured yet |
| `ig.veto.dm_composer` | veto | – | `row_thread_composer*` visible. Never block inside a chat | ✅ |

On the Reels tab, ReelFree taps the Home tab by its resource ID `feed_tab` (confirmed), falling back to a bottom-bar node labelled "Home".

Blocking threshold is 60. The size requirement is what separates a full-screen Reel from a Reel thumbnail in the feed or on a profile grid.

---

## 2. Permissions, and why each is needed

| Item | Why |
|---|---|
| Accessibility service (`BIND_ACCESSIBILITY_SERVICE`) | This is a protection on the service so that only Android can bind it. You switch it on yourself in Settings. ReelFree never enables it programmatically |
| `canRetrieveWindowContent` | Needed to read the view hierarchy (IDs, sizes, tab labels) |
| `flagReportViewIds` | Makes resource names such as `clips_viewer_*` visible. These are the strongest signals |
| `flagIncludeNotImportantViews` | Instagram marks many containers "not important for accessibility", which would hide the structure |
| `packageNames="com.instagram.android"` | Android delivers events **only** from Instagram |
| `<queries>` for Instagram | Shows the installed Instagram version on screen and in diagnostics |
| **Not requested** | INTERNET, notifications, storage, gestures, key events, screenshots, overlay |

---

## 3. Build

Requirements: JDK 17 and Android SDK platform 35. Android Studio Ladybug or newer bundles both.

**Android Studio:** File ▸ Open ▸ select this folder ▸ wait for Gradle sync ▸ Run ▶.

**Command line (Windows):**
```
set JAVA_HOME=C:\Users\<you>\.jdks\jdk-17.0.21+9
gradlew.bat assembleDebug        :: app\build\outputs\apk\debug\app-debug.apk
gradlew.bat assembleRelease      :: app\build\outputs\apk\release\app-release.apk (minified, debug-signed)
gradlew.bat testDebugUnitTest    :: unit tests
```
`local.properties` points at the SDK (`sdk.dir=...`). Android Studio rewrites it automatically.

## 4. Install on your phone

### Option A: download from GitHub Releases (recommended)

GitHub Actions (`.github/workflows/build-apk.yml`) runs the unit tests and builds the APK on every push. Pushing a tag such as `v0.1.0` also attaches the signed APK to a GitHub Release.

1. On the phone, open the repository's **Releases** page and download `ReelFree-vX.Y.Z.apk`.
2. Open the download and allow your browser to **Install unknown apps** when Android asks.
3. Updates: download the newer release APK and install it over the old one. Your settings are kept, because every release is signed with the same key.

To publish a new version from the PC:
```
git tag v0.1.1
git push origin v0.1.1
```

**Signing key:** release APKs are signed with `~/.reelfree/reelfree-release.jks` (outside the repo, never committed). CI gets it from the `REELFREE_KEYSTORE_BASE64` and `REELFREE_KEYSTORE_PASSWORD` repository secrets. **Back up that `.jks` file and its password** (`~/.reelfree/keystore.properties`). If you lose them, future builds can't update the installed app; you'd have to uninstall and reinstall.

### Option B: USB

1. On the phone: Settings ▸ About phone ▸ tap **Build number** 7× ▸ Developer options ▸ **USB debugging** on.
2. Run `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\release\app-release.apk`.
   Installs over USB are not treated as sideloads, so step 4 below usually isn't needed.

### Enable the service

3. Open **ReelFree** ▸ **Open Accessibility Settings** ▸ *Installed/Downloaded apps* ▸ **ReelFree Reels blocker** ▸ On ▸ Allow.
4. **Android 13+ sideload restriction:** if the switch is greyed out or says "Restricted setting", go to ReelFree ▸ **Open ReelFree App info** ▸ ⋮ ▸ **Allow restricted settings**, then repeat step 3.
5. The status card turns green: **Protection Active**.

Some vendors (Xiaomi, Samsung, Huawei, OnePlus) kill or unbind accessibility services to save battery. If the status turns to *"enabled in Settings but Android isn't running it"*, set ReelFree's battery usage to **Unrestricted** and toggle the service off and on.

---

## Calibrating detection on your phone

**Do this before relying on the blocker.** No one outside Instagram can know its current view hierarchy, and it changes between versions and A/B tests.

1. In ReelFree, turn on **Dry run** and **Debug mode**.
2. In Instagram, visit, in order: Home feed, a Story, a DM thread, a profile grid, a normal photo post, then the **Reels tab**, a Reel from the feed, a Reel from a profile, and a Reel shared in a DM.
3. Back in ReelFree, open **Diagnostics**:
   - **Detection events** shows each evaluation that scored above 0, which rules matched, and what *would* have happened ("DRY RUN - would BACK", "vetoed", "below threshold").
   - **Screen structures** shows each distinct screen as a tree of `Class #view_id label="reels" [selected] w=100% h=92% top=0%`. Free text appears only as `text(N chars)`.
4. Check:
   - Every Reels screen should show `DRY RUN - would …`. If one doesn't, find the view IDs that appear **only** on Reels screens in its structure dump.
   - No non-Reels screen should show `would …`. If one does, note which signal matched.
5. Edit `InstagramReelsRules.kt` (add, re-weight, or add a VETO), rebuild, and repeat. **Share report** sends the dump to wherever you choose (e.g. email it to yourself, or paste it to me) so the rules can be adjusted.
6. When all looks right, turn **Dry run** off.

**What the report contains:** ReelFree version, Instagram version, Android SDK level, rule IDs, view IDs, class names, relative sizes and positions, and labels from the fixed vocabulary (`reels`, `home`). **What it doesn't contain:** messages, captions, usernames, comments, or anything typed.

---

## 5. Manual test plan (Phase 1)

None of these have been run on a device yet. Fill in the "Result" column after you test.

| # | Case | Expected | Expected support level |
|---|---|---|---|
| 1 | Open Instagram normally | Nothing happens | Supported |
| 2 | Tap the Reels tab | Jumps back to Home tab within ~0.5 s | Supported *if* tab label/selection or `clips_viewer` is exposed |
| 3 | Reel from home feed | Viewer closes, back to feed | Supported *if* `clips_viewer` is exposed |
| 4 | Reel from a profile grid | Viewer closes, back to profile | Same |
| 5 | Reel shared in DMs | Viewer closes, back to chat | **Experimental**: the DM-composer veto may suppress it if the composer stays visible |
| 6 | Reel via external link | Instagram opens, viewer closes. Back may land on the source app | **Experimental**: Back can leave Instagram when the Reel is the first screen |
| 7 | Watch Stories | Never blocked | Supported (veto) |
| 8 | Normal photo posts | Never blocked | Supported |
| 9 | Use DMs | Never blocked | Supported (veto) |
| 10 | Switch apps mid-Reel | No stray Back in other apps (service only receives Instagram events) | Supported |
| 11 | Toggle protection off/on | Off: Reels play. On: blocked again | Supported |
| 12–13 | YouTube Shorts / videos | n/a | Phase 3 |

Also check: rotate the phone, lock and unlock while a Reel is open, and toggle dark mode.

## 6. Known limitations

- **No guarantee of compatibility.** One Instagram update can rename views. Diagnostics exist so you can see when that happens.
- **Reels inside other surfaces.** Reels that autoplay *inline* in the feed are not full-screen and are intentionally **not** blocked, to avoid false positives.
- **Deliberately rewatching.** If you open Reels 4 times in 15 s, the loop guard pauses blocking for 30 s. This stops ReelFree from fighting you or pressing Back until Instagram closes.
- **Back can exit Instagram** when a Reel is the first screen (e.g. opened from a link). On the Reels tab ReelFree taps Home instead.
- **~300–500 ms delay.** Two consecutive confirmations are required before acting, so you may see a frame of the Reel.
- **Service kills.** Aggressive OEM battery managers can stop the service. ReelFree shows this on the status card but cannot restart itself.
- **Diagnostics are in memory.** They are lost if Android kills ReelFree's process.

## 7. Troubleshooting

| Symptom | Fix |
|---|---|
| Accessibility switch is greyed out | Android 13+: App info ▸ ⋮ ▸ Allow restricted settings |
| Status says "isn't running" | Toggle the service off/on; set battery to Unrestricted; reboot |
| Reels not blocked | Enable Debug + Dry run and check Diagnostics. If no events appear, Instagram isn't exposing the expected IDs: share the report and update rules |
| Something non-Reels gets closed | Turn on Dry run immediately, find the matching signal in Diagnostics, add a VETO |
| Instagram keeps closing | Loop guard should stop it after 4 actions. Turn protection off and report the Diagnostics |
| Logcat | `adb logcat -s ReelFree` shows service connect/disconnect |

## 8. Privacy

All processing is on-device. There is no INTERNET permission, so the app cannot upload anything. ReelFree observes only Instagram, never reads editable or password fields, keeps only a fixed vocabulary of tab labels, never records the screen, and stores only four on/off settings (DataStore). Diagnostics are opt-in, held in memory, contain no free text, and leave the phone only when you tap **Share report**.

## Roadmap

- **Phase 2:** statistics (counted only when an action was actually performed), rules refined from your Diagnostics, more entry points, tests on recorded (anonymised) structures.
- **Phase 3:** Explore blocker (default off), YouTube Shorts detector module (default off), settings screen (sensitivity, notifications, reset stats, export logs, about), UI polish.
