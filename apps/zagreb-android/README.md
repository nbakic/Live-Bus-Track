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

Faza A izvedena. Detaljan popis funkcija i preostalih produkcijskih rupa je u
[`docs/TODO.md`](../../docs/TODO.md). Ukratko:

- **Karta:** MapLibre s OSM raster pločicama, živi sloj vozila, tap na stajalište.
  Plutajuća tražilica + horizontalno klizni akcijski čipovi + diskretna "live"
  pilula (broj vozila uživo ili razlog nedostupnosti).
- **Podaci:** `:shared` (domain + data) sa SQLDelight bazom koju puni GTFS importer
  (`GtfsSyncWorker` — jednokratni sync pri prvom startu + dnevni za svježinu).
  Žive pozicije / dolasci / obavijesti idu kroz Ktor klijent na `transit-api`.
- **UX:** koherentna ZET-crvena Material 3 tema (light + dark). Ekrani koji
  dohvaćaju podatke razlikuju "nema interneta" od "poslužitelj nedostupan"
  (dijelom — preostali ekrani su C8 u TODO-u).

Klijent gađa ISKLJUČIVO `transit-api` backend, nikad ZET izravno (sekcija 5 plana).

> **Backend URL:** debug build → `http://10.0.2.2:8080` (emulator → host stroj).
> Release → `-PbackendUrl=https://...` kad backend bude hostan (P5 u TODO-u).
