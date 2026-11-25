# Google Play Store Listing Package — GPS Scanner

## 1. Visual Assets (Ready in `store_assets/`)
- **App Icon**: `store_assets/play_store_icon_512x512.png` (512 × 512 px, 32-bit PNG, < 1 MB)
- **Feature Graphic**: `store_assets/feature_graphic_1024x500.png` (1024 × 500 px, 32-bit PNG)
- **Phone Screenshots (You capture from Emulator / Real Device)**:
  - Format: PNG or JPEG, min 2 screenshots (recommended 4–6), aspect ratio 16:9 or 9:16 (e.g. `1080 × 1920` or `1080 × 2400`).
  - Recommended 4 screens to capture:
    1. **Tracker Scan** — Radar sweep active + Magnetic Mount Detector readout
    2. **Bluetooth Tracker Scan** — Nearby BLE devices & proximity distance badges
    3. **Device GPS** — Hardware check + Live GNSS Satellite signal bars
    4. **GPS Scanner Pro** — Monthly & Annual subscription bottom sheet

---

## 2. Store Listing Copy (US / EU English, Play Policy Compliant)

### App Name (11 / 30 chars max)
`GPS Scanner`

### Short Description (75 / 80 chars max)
`Detect magnetic & BLE GPS trackers and inspect live device GNSS satellites.`

### Full Description (Copy-paste ready)
```text
GPS Scanner is a simple, fast utility to help you check for nearby wireless tracking beacons, detect abnormal magnetic fields from magnetic-mount GPS devices, and inspect your phone's built-in GPS hardware and satellite reception.

TWO ESSENTIAL CHECKS IN ONE SIMPLE APP

1. Tracker Scan (Anti-Tracking Check)
• Magnetic Mount Detector (No Permissions Needed): Uses your phone's built-in magnetometer to measure magnetic flux (μT) in real time. Move your device slowly near vehicle seats, gloveboxes, trunks, or bumper edges to spot strong magnetic spikes from hidden magnetic mounts, with one-tap baseline calibration.
• Bluetooth Tracker Scan (BLE): Scans nearby Bluetooth Low Energy advertisements and highlights known tracker signatures—including Apple AirTag / Find My beacons, Samsung SmartTag, Tile trackers, and close-range wireless signals with estimated distance.

2. Device GPS Check (Hardware & Satellite Signal)
• Hardware Verification: Instantly confirms whether your Android device includes a GPS hardware module and whether system location services are enabled.
• Live GNSS Satellite Signals: Displays visible and active satellites across GPS, BeiDou, GLONASS, Galileo, and QZSS with real-time signal-to-noise ratio (dB-Hz) and fix accuracy.

TRANSPARENT & HONEST BY DESIGN
Standard smartphone sensors cannot directly detect completely passive or cellular-only trackers that have neither magnets nor Bluetooth radios. GPS Scanner clearly explains what each sensor checks so you get honest, real-time hardware readings—never simulated results.

GPS SCANNER PRO (OPTIONAL SUBSCRIPTION)
Upgrade to GPS Scanner Pro via monthly or annual auto-renewing Google Play subscription to unlock:
• Continuous unlimited scanning (removes the 20-second auto-stop timer)
• Priority live tracker and magnetic field monitoring
• Clean, distraction-free Pro experience

Subscriptions are billed through your Google Play account and auto-renew unless cancelled at least 24 hours before the end of the current period. Manage or cancel anytime in Google Play Store settings.
```

---

## 3. Play Console Setup & Policy Forms Checklist

- **Package Name (`applicationId`)**: `com.aistudio.gpsdetector.qrxmvt`
- **Category**: `Tools`
- **Tags**: `Utilities`, `Tools`
- **Target Audience**: `18+` or `16+` (Avoid selecting children under 13 to keep Families policy out of scope)
- **Privacy Policy URL (Required)**: Host a public HTTPS Privacy Policy page stating:
  - Foreground Location (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) and Bluetooth Scan (`BLUETOOTH_SCAN`) are used solely on-device in real time for GNSS satellite inspection and local BLE beacon detection.
  - No location data, Bluetooth scan results, or personal identifiers are transmitted to external servers.
  - Subscription purchases are processed securely by Google Play Billing.
- **Data Safety Form**:
  - Does your app collect or share any required user data types?
    - You can declare **Purchase history** (processed by Google Play for subscription entitlement) and note that Location & BLE data are processed **ephemerally on-device only** and not collected or shared off-device.
- **Google Play Billing Setup (Monthly & Annual)**:
  1. Upload signed `.aab` to **Internal Testing** first.
  2. Go to **Monetize with Play -> Products -> Subscriptions**.
  3. Create subscription product ID: `gps_scanner_pro`.
  4. Add Base Plan 1: `monthly` (Billing period: 1 month).
  5. Add Base Plan 2: `annual` (Billing period: 1 year).
  6. Add Gmail test accounts under **Setup -> License testing** before testing purchases.
