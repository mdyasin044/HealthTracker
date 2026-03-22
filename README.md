# 🏥 HealthTracker

A comprehensive Android health monitoring app that integrates wearable devices and continuous glucose monitors into a single dashboard.

---

## Features

- **Nutrition Log** — Track daily macros (protein, carbs, fat) and log meals
- **Galaxy Watch Integration** — Real-time sync with Samsung Galaxy Watch for steps, heart rate, and accelerometer data
- **Blood Glucose Monitoring** — Connects to Dexcom G6 Pro CGM and displays readings at 5-minute intervals with High/Normal indicators
- **Live Sensor Data** — View accelerometer X/Y/Z axes directly from your watch
- **One-tap Sync** — Instantly refresh all device data with the Sync button

---

## Screenshots

| Dashboard |
|-----------|
![f95c139f-2ef3-42ec-b71c-6fb4d7687daf](https://github.com/user-attachments/assets/b82951fd-3734-4853-a950-94304602cfc7)


---

## Supported Devices

| Device | Data |
|--------|------|
| Samsung Galaxy Watch 6 | Steps, Heart Rate, Accelerometer |
| Dexcom G6 Pro | Blood Glucose (mg/dL) |

---

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- Android SDK 26+
- A physical Android device (recommended for Bluetooth/sensor features)
- Samsung Galaxy Watch 6 paired to your device
- Dexcom G6 Pro CGM (optional)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/mdyasin044/HealthTracker.git
   cd HealthTracker
   ```

2. **Open in Android Studio**
   - File → Open → select the project folder

3. **Build & Run**
   - Connect your Android device
   - Click **Run ▶** or use `Shift + F10`

---

## How It Works

### Nutrition Logging
Enter your meal's protein, carbs, and fat values in grams, then tap **LOG MEAL** to record it.

### Watch Sync
The app connects via Bluetooth to a paired Galaxy Watch 6. Tap the **Sync** button to fetch the latest steps, heart rate, and accelerometer readings. The last sync timestamp is displayed below the connection status.

### Blood Glucose
Once connected to a Dexcom G6 Pro, the app polls glucose readings every 5 minutes. Readings are color-coded:
- 🔴 **High** — above 140 mg/dL
- ⚪ **Normal** — within range
- 🔵 **Now** — the most recent reading

---

## Project Structure

```
HealthTracker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Kotlin/Java source files
│   │   │   ├── res/           # Layouts, drawables, strings
│   │   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
└── build.gradle
```

---

## Permissions

The app requires the following permissions:

- `BLUETOOTH` / `BLUETOOTH_CONNECT` — Galaxy Watch communication
- `BODY_SENSORS` — Heart rate and accelerometer data
- `ACTIVITY_RECOGNITION` — Step counter

---

## Author

**Md Yasin** — [@mdyasin044](https://github.com/mdyasin044)
