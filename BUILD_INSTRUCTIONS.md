# JARVIS AI — Build Instructions

## Prerequisites
- Android Studio Hedgehog (2023.1.1+) or later, **OR** just the Android SDK command-line tools
- JDK 17 (comes with Android Studio)
- Android SDK with API 34 downloaded
- At least 4 GB free RAM for build
- Internet connection (first build downloads dependencies ~500 MB)

---

## Option A — Build from Android Studio (Easiest)

1. Open Android Studio → **File → Open** → select the `JarvisAI` folder
2. Wait for Gradle sync to complete (~2–5 min on first run)
3. Click **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. Or press **Shift+F10** to build and run directly on a connected device

---

## Option B — Build from Terminal (your preferred way)

### Windows (cmd or PowerShell)
```bat
cd JarvisAI

:: Debug APK
gradlew.bat assembleDebug

:: The APK will be at:
:: app\build\outputs\apk\debug\app-debug.apk
```

### macOS / Linux
```bash
cd JarvisAI
chmod +x gradlew

# Debug APK
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (unsigned)
```bat
gradlew.bat assembleRelease
```

---

## Install on Device

### Via ADB (USB debugging enabled on phone)
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Manual install
1. Copy the APK to your phone
2. Open it from Files app
3. Allow "Install from unknown sources" when prompted

---

## First Launch Setup

1. **Grant Microphone** — tap the mic button, allow when prompted
2. **Download a Model** — tap the download icon in the top bar
   - Start with **Gemma 3 1B** (fastest, 600 MB)
   - Or **Gemma 2B** for better quality (1.3 GB)
3. **Activate the Model** — tap "Activate" after download completes
4. **Grant Accessibility** (for phone control):
   - Go to Permissions screen (shield icon)
   - Tap "Open Settings" under Accessibility Service
   - Find "JARVIS AI" → Enable it
5. **Speak to JARVIS** — tap the microphone button!

---

## Architecture Overview

```
JarvisAI/
├── app/src/main/java/com/jarvis/ai/
│   ├── JarvisApplication.kt       ← App singleton, DI root
│   ├── MainActivity.kt            ← Single activity
│   ├── audio/
│   │   ├── SpeechManager.kt       ← Android SpeechRecognizer wrapper
│   │   └── TTSEngine.kt           ← Android TextToSpeech wrapper
│   ├── data/
│   │   ├── AppDatabase.kt         ← Room DB (model metadata)
│   │   ├── AppPreferences.kt      ← DataStore settings
│   │   └── ModelInfo.kt           ← Data classes + model catalog
│   ├── llm/
│   │   ├── LLMEngine.kt           ← MediaPipe LLM inference
│   │   └── ModelDownloader.kt     ← OkHttp streaming download
│   ├── services/
│   │   ├── JarvisService.kt       ← Foreground service (background)
│   │   └── JarvisAccessibilityService.kt ← Phone control
│   ├── ui/
│   │   ├── components/
│   │   │   └── JarvisComponents.kt ← Reusable HUD widgets
│   │   ├── screens/
│   │   │   ├── ChatScreen.kt      ← Main voice/chat UI
│   │   │   ├── ModelsScreen.kt    ← Download & manage models
│   │   │   ├── SettingsScreen.kt  ← All settings
│   │   │   └── PermissionsScreen.kt ← Permission management
│   │   └── theme/                 ← Iron Man dark theme
│   ├── viewmodel/
│   │   └── MainViewModel.kt       ← Central state + business logic
│   └── vision/
│       └── ScreenCaptureManager.kt ← MediaProjection screen reader
```

---

## Supported Models (downloaded from HuggingFace)

| Model       | Size    | Speed  | Quality |
|-------------|---------|--------|---------|
| Gemma 3 1B  | ~600 MB | ⚡⚡⚡  | ★★★     |
| Gemma 2B    | ~1.3 GB | ⚡⚡    | ★★★★    |
| Phi-2       | ~1.5 GB | ⚡⚡    | ★★★★    |
| Falcon 1B   | ~500 MB | ⚡⚡⚡  | ★★★     |

All models run **100% on-device** — no internet required after download.

---

## Phone Control Commands (after enabling Accessibility)

| Voice Command                    | What JARVIS Does              |
|----------------------------------|-------------------------------|
| "Open YouTube"                   | Launches YouTube app          |
| "Go back"                        | Presses back button           |
| "Go home"                        | Returns to home screen        |
| "Scroll down"                    | Scrolls the current screen    |
| "Type hello world"               | Types text in focused field   |
| "Show recent apps"               | Opens recents                 |
| "What's on my screen?"           | Reads screen content to you   |
| "Open notifications"             | Pulls down notification panel |

---

## Troubleshooting

**Build fails: "SDK not found"**
→ Set `ANDROID_HOME` env var to your SDK path, or create `local.properties`:
```
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

**"Model not loading"**
→ Ensure you have at least 2 GB free RAM. Close other apps before loading.

**"Speech not working"**
→ Check microphone permission in Android Settings → Apps → JARVIS AI → Permissions

**Download stuck at 0%**
→ Models are hosted on HuggingFace. Check internet connection.
→ Some ISPs block HuggingFace — try a VPN or different network.

**Accessibility not showing in settings**
→ Uninstall and reinstall the app. The service should appear under
   Settings → Accessibility → Downloaded Apps.
