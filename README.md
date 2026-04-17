# ⚡ JARVIS AI — Iron Man's Assistant on Your Phone

> A fully offline AI assistant that runs local LLMs, understands voice, speaks back, and controls your Android phone.

---

## 📱 Download the APK (No coding needed)

### Step 1 — Upload this project to GitHub

1. Go to [github.com](https://github.com) and sign in (or create a free account)
2. Click the **+** button → **New repository**
3. Name it `JarvisAI`, set it to **Public**, click **Create repository**
4. On the next page, click **uploading an existing file**
5. Drag and drop **all the files and folders** from this zip into the upload area
6. Click **Commit changes**

### Step 2 — Wait for GitHub to build the APK (~5 minutes)

1. Click the **Actions** tab in your repository
2. You'll see a workflow called **"Build JARVIS APK"** running (yellow circle = building)
3. Wait for it to turn **green ✅** (takes about 5 minutes on first run)

### Step 3 — Download your APK

1. Click the green workflow run
2. Scroll to the bottom of the page
3. Under **Artifacts**, click **JarvisAI-debug-XXXXXXXX.apk**
4. A zip file downloads — open it to get your APK
5. Transfer the APK to your Android phone and install it

> **First install:** Go to Settings → Security → allow "Install from unknown sources"

---

## 🚀 Features

| Feature | How it works |
|---|---|
| 🎤 Voice input | Android speech recognition (works offline) |
| 🔊 Voice output | Android TTS with JARVIS-style low pitch |
| 🧠 Local LLM | Google MediaPipe — runs 100% on device |
| 📱 Phone control | Accessibility service — opens apps, taps, scrolls, types |
| 👁️ Screen vision | MediaProjection — JARVIS can see your screen |
| 🌙 Background | Foreground service keeps JARVIS always listening |
| ⚡ Wake word | Say "Hey JARVIS" to activate hands-free |

---

## 🤖 Available AI Models (downloaded inside the app)

| Model | Size | Speed | Best for |
|---|---|---|---|
| **Gemma 3 1B** ⭐ | 600 MB | Fastest | Quick commands, phone control |
| **Gemma 2B** | 1.3 GB | Fast | General assistant, conversation |
| **Phi-2** | 1.5 GB | Medium | Reasoning, smart answers |
| **Falcon 1B** | 500 MB | Fastest | Lightweight, basic tasks |

All models run **100% offline** after download. No API keys. No subscription.

---

## 🔐 Permissions Explained

| Permission | Why JARVIS needs it |
|---|---|
| Microphone | To hear your voice commands |
| Accessibility Service | To control phone — open apps, tap buttons, type text |
| Display Over Apps | Floating JARVIS orb while using other apps |
| Notifications | Background status bar control |
| Storage | Saving AI model files |

---

## 💬 Example Voice Commands

Once a model is loaded and activated:

- *"Hey JARVIS, open YouTube"*
- *"What's on my screen?"*
- *"Open WhatsApp and send a message to John"*
- *"Scroll down"* / *"Go back"* / *"Go home"*
- *"What time is it in Tokyo?"*
- *"Write me a to-do list for today"*
- *"Turn off WiFi"* (with accessibility enabled)

---

## 🔁 Rebuild anytime

Every time you push a change to GitHub, a new APK is built automatically. Just go to Actions → latest run → Artifacts to download it.

---

## 🛠 Project Structure

```
JarvisAI/
├── .github/workflows/build.yml   ← GitHub Actions (auto-builds APK)
├── app/src/main/java/com/jarvis/ai/
│   ├── audio/          ← Voice recognition + TTS
│   ├── llm/            ← AI model download + inference
│   ├── services/       ← Background service + phone control
│   ├── ui/             ← Iron Man HUD interface
│   ├── vision/         ← Screen capture
│   └── viewmodel/      ← App logic
└── BUILD_INSTRUCTIONS.md
```
