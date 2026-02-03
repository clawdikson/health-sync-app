# ClawdBot Health Sync

Android app that syncs Health Connect + Renpho data to ClawdBot.

## Features

### Health Connect
- 😴 Sleep data (duration, stages)
- ⚖️ Weight
- 🚶 Steps  
- 🔥 Body fat percentage
- ❤️ Heart rate

### Renpho (Direct API)
- ⚖️ Weight
- 🔥 Body fat %
- 💪 Muscle %
- 💧 Water %
- 🦴 Bone mass
- 🫀 Visceral fat
- ⚡ BMR
- 🥩 Protein %
- 🎂 Body age

### Auto-Sync
- ⏰ Daily auto-sync at 8 AM

## Build

1. Open project in Android Studio
2. Build → Build APK
3. Install on your phone

## Or Build via Command Line

```bash
cd health-sync-app
./gradlew assembleDebug
# APK will be at app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Install the APK on your phone
2. Open "ClawdBot Health" app
3. Tap "Sync Now" - grant Health Connect permissions when asked
4. Tap "Enable Daily Auto-Sync" for automatic daily syncs

## Server Endpoint

Data is sent to: `http://130.131.38.203:8080/_api/health/sync`

## Permissions Required
- Health Connect: Sleep, Steps, Weight, Body Fat, Heart Rate
- Internet: To send data to server
