# zagreb-android

Native Android aplikacija za ZET (Zagrebački električni tramvaj) — Kotlin + Jetpack
Compose, clean arhitektura. Vidi [`docs/zagreb-app-plan.md`](../../docs/zagreb-app-plan.md).

## Moduli

| Modul | Sloj | Platforma |
|---|---|---|
| `:app` | Compose UI, ViewModeli, DI wiring | Android |
| `:shared` | `:domain` (logika) + `:data` (implementacija) | KMP — dijeli se s iOS-om |

`:shared` se kompajlira i u iOS framework (`Shared`) — vidi [`apps/zagreb-ios`](../zagreb-ios).

## Preduvjeti

- **JDK 17+** (AGP 8.5 / Kotlin 2.0 traže ga; Java 8 NE radi).
- Android SDK (`compileSdk 34`, `minSdk 26`).
- `local.properties` sa `sdk.dir=...` (Android Studio ga generira automatski).

## Build

```bash
./gradlew :app:assembleDebug      # Android APK
./gradlew :shared:testDebugUnitTest   # shared sloj unit testovi
```

> KMP build je 1.5–2× sporiji od pure-Androida (svjesno prihvaćeno — sekcija 4.3 plana).

## Stanje

Faza 0 skeleton. `:shared` ima domain modele, repository ugovore, use-caseove,
SQLDelight schemu i Ktor klijent za `transit-api`. `:app` ima Compose skeleton s
placeholder karta-ekranom. **Sljedeće:** MapLibre integracija, GTFS importer,
backend feed-validacija.

Klijent gađa ISKLJUČIVO `transit-api` backend, nikad ZET izravno (sekcija 5 plana).
