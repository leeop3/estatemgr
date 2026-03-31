# Estate Manager — Android App

Offline-first oil palm estate management app using **Reticulum (RNS) over LoRa (RNode)**.

## Architecture

```
Field Devices (LoRa)
  ├── Bunch Checkers    → BR: packets
  ├── Tractor Drivers   → TL: packets
  ├── Pest & Disease    → GT: packets
  └── Fertilizing Gang  → GT: packets
          │
       RNode (USB-OTG)
          │
   rns_backend.py  (Chaquopy)
          │
    MessageRouter.kt
          │
   ┌──────┴──────┐
   Room (SQLite)  ViewModels
          │
     Compose UI (3 tabs + Settings)
```

## Tabs

| Tab | Content |
|-----|---------|
| **Harvest** | Block summary dashboard, bunch map, tractor tracking, alerts, chat |
| **Pest & Disease** | Path coverage map (red polylines), supervisor chat |
| **Fertilizing** | Path coverage map (green polylines), supervisor chat |
| **Settings** | RNode radio params, node identity QR, peer management |

## Message Prefix Protocol

| Prefix | Type | Direction |
|--------|------|-----------|
| `BR:` | BunchRecord | Field → Manager |
| `TL:` | TractorLocation | Field → Manager |
| `GT:` | GangTrack | Field → Manager |
| `CM:` | ChatMessage | Bidirectional |
| `AL:` | Alert | Bidirectional |
| `IMG:` | Image (base64) | Field → Manager |
| `ACK:` | Delivery ACK | Auto (rns_backend) |

## Build

### GitHub Actions (automatic)
Push to `main` → workflow builds debug APK → download from Actions → Artifacts.

### Local build
```bash
# Requires JDK 17, Android SDK
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## RNode Radio Defaults

| Parameter | Value |
|-----------|-------|
| Frequency | 865 MHz |
| Bandwidth | 125 kHz |
| TX Power  | 17 dBm |
| SF        | 9 |
| CR        | 5 |

Override in **Settings** tab → saved to SharedPreferences → applied on next app start.

## RNS Identity Convention

Field devices must set their display name as `ROLE:Name`, e.g.:
- `CHECKER:Ali`
- `TRACTOR:Driver01`
- `PEST:GangLeader`
- `FERT:Supervisor`

The manager uses `Manager:Name`.

## Offline Map Tiles

Pre-load OSM tiles before field deployment:
1. Open the app while on Wi-Fi
2. Pan/zoom over the estate area
3. Tiles are cached automatically in `files/cache/osm_tiles/`

## Project Structure

```
app/src/main/
├── java/com/estate/manager/
│   ├── rns/           ← RNS bridge (RnsManager, MessageRouter, PacketSerializer)
│   ├── data/          ← Models, DAOs, Repositories, AppDatabase
│   ├── ui/            ← Compose screens + ViewModels per tab
│   └── util/          ← ImageCompressor, GpsUtils
├── python/
│   └── rns_backend.py ← Unmodified RNS reference implementation
└── res/               ← Drawables, strings, themes
```
