# Zagreb Transit App — Stanje i TODO

> **Što je ovo:** popis postojeće funkcionalnosti i onoga što nedostaje da
> aplikacija stvarno radi u produkciji. Plan je u
> [`zagreb-app-plan.md`](zagreb-app-plan.md); ovaj dokument prati izvedbu.
>
> **Grana:** sav rad je na grani `zet-app`. `master` se ne dira.
> **Datum:** 2026-05-18 (zadnje ažurirano 2026-05-29)

---

## 1. Što je napravljeno

Cijela **Faza A** (A0 + A1 + A2) je izvedena kao skeleton koji se kompajlira
i prolazi testove. Tri modula:

- `apps/zagreb-android/` — Android aplikacija (Kotlin, Compose, KMP)
- `services/transit-api/` — backend (Kotlin/Ktor)
- `apps/zagreb-ios/` — placeholder za Fazu C (iOS)

### 1.1 Android aplikacija — ekrani i funkcije

| Funkcija | Plan | Ekran / komponenta | Stanje |
|---|---|---|---|
| Live karta vozila | A0.1 | `MapScreen`, `VehicleLayer` | MapLibre karta, vozila kao GeoJSON sloj |
| Dolasci na stajalište | A0.2 | `StopDetailScreen` | RT predikcije vs statički fallback, jasno označeno |
| Omiljena stajališta | A0.3 | `FavoritesScreen`, zvjezdica na stajalištu | lokalno (SQLDelight) |
| Prometne obavijesti | A0.4 | `AlertsScreen` | kartice po ozbiljnosti |
| Najbliža stajališta | A0.5 / A1.6 | `NearbyScreen` | lokacija + pješačka udaljenost |
| Statički vozni red po stajalištu | A0.6 | dio `StopDetailScreen` | iz GTFS static |
| Home-screen widget | A0.7 | `NextArrivalWidget` | sljedeći dolazak na omiljenom stajalištu |
| Pregled svih linija | A1.1 | `RoutesScreen` | tramvaji + autobusi, obojeni čipovi |
| Geometrija rute na karti | A1.2 | `RouteDetailScreen`, `RouteShapeLayer` | iz `shapes.txt` |
| Pretraga | A1.3 | `SearchScreen` | stajališta + linije, dijakritika-neutralna |
| Vozni red po liniji | A1.4 | dio `RouteDetailScreen` | polasci po smjerovima |
| Rutine Dom/Posao/Škola | A1.5 | `RoutinesScreen` (jutarnji ekran) | rutine sa živim dolascima |
| Planiranje rute A→B | A2.1 | `PlanScreen` | varijante s presjedanjima |
| Push — registracija tokena | A2.2 | `PushTokenRegistrar` | token → backend (vidi rupe niže) |

Arhitektura: clean (UI / domain / data), MVVM, KMP shared sloj (`:shared`),
Koin DI, SQLDelight lokalna baza, Ktor mrežni sloj, Compose navigacija.

### 1.2 Backend `transit-api` — endpointi

Verzionirani `/v1` ugovor. Klijent gađa isključivo ovaj backend, nikad ZET.

| Endpoint | Funkcija | Stanje |
|---|---|---|
| `GET /health` | health check | radi |
| `GET /v1/vehicles` | žive pozicije vozila | pass-through cache nad GTFS-RT |
| `GET /v1/stops/{id}/arrivals` | dolasci na stajalište | iz GTFS-RT TripUpdate |
| `GET /v1/alerts` | prometne obavijesti | iz GTFS-RT Alert |
| `GET /v1/routes` | sve linije | iz GTFS static |
| `GET /v1/stops` | sva stajališta | iz GTFS static |
| `GET /v1/routes/{id}/shape` | geometrija rute | iz `shapes.txt` |
| `GET /v1/routes/{id}/schedule` | vozni red linije | iz `stop_times.txt` |
| `GET /v1/gtfs/static.zip` | sirovi GTFS ZIP | proxy za klijentski import |
| `GET /v1/walk` | pješačka ruta | OSRM `foot` proxy |
| `GET /v1/plan` | planiranje rute A→B | GraphHopper `pt` proxy (vidi rupe) |
| `POST /v1/notifications/register` | registracija FCM tokena | radi (vidi rupe) |

### 1.3 Testovi

- Backend: `TtlCacheTest`, `GtfsRtMapperTest`, `CsvParserTest`, `V1ContractTest`
- Android `:shared`: `GeoTest`, `GtfsStaticParserTest`, `TextNormalizerTest`
- Android `:app`: `EtaFormatTest`

Svi prolaze pod JDK 17.

### 1.4 Nedavno odrađeno (2026-05-29)

- **Backend OOM fix (L7):** `GtfsStaticFeedService` sad *streama* `stop_times.txt`
  (~120 MB raspakirano) red po red umjesto da ga cijelog učita kroz `CsvParser`
  (što je rušilo JVM na prvom `/v1/vehicles` i obaralo sve žive podatke).
  Potvrđeno protiv stvarnog ZET feeda: ~322 vozila, 155 linija, 3839 stajališta.
- **Redizajn karte:** plutajuća tražilica + horizontalno klizni čipovi + diskretna
  "live" pilula umjesto skupine FAB-ova.
- **Koherentna ZET-crvena tema (C7):** pun M3 token set, neutralne površine, dark mode.
- **Prvi start puni bazu odmah:** `GtfsSyncWorker.syncNow` (jednokratni sync) uz
  periodični; prije se lista linija/stajališta učitala prazno dok periodični posao
  (24 h) ne odradi. `GtfsImporter` serijaliziran mutexom da ne dvostruko preuzima.
- **Internet vs. poslužitelj poruke (C8):** `ConnectivityChecker` + `classifyLoadError`;
  karta i `RoutesScreen` razlikuju "nema interneta" od "poslužitelj nedostupan", uz retry.
- **HTTP timeout 8 s → 25 s** za spore hladne statičke dohvate.

---

## 2. Što NEDOSTAJE za produkciju

Ovo su konkretne rupe — funkcionalnost je napisana, ali traži vanjski
servis, credential ili konfiguraciju da bi *stvarno* radila.

### 2.1 Vanjski servisi i credentiali — BLOKIRA produkciju

| # | Što | Gdje | Što treba |
|---|---|---|---|
| P1 | **GraphHopper API ključ** | `Config.graphHopperApiKey` | Bez ključa `/v1/plan` vraća 503 — planiranje rute (A2.1) ne radi. Treba GraphHopper račun i ključ u env varijabli `GRAPHHOPPER_API_KEY`. |
| P2 | **Firebase projekt (FCM)** | `NotificationService.sendArrivalReminder`, `PushTokenRegistrar` | Registracija tokena radi, ali slanje push poruka NE. Treba: Firebase projekt, `google-services.json`, Firebase Gradle plugin + `firebase-messaging` dependency, `FirebaseMessagingService`, Firebase Admin SDK na backendu. Plugin namjerno nije dodan jer bi bez `google-services.json` slomio build. |
| P3 | **Map tile provider** | `MapLibreView.DEMO_STYLE_URL` | Karta koristi MapLibre demo stil. Za produkciju treba plaćeni tile provider (MapTiler / Stadia / self-hosted) i vlastiti stil pločica. Rizik R3 iz plana. |
| ~~P4~~ | ~~Produkcijski backend URL~~ | — | **RIJEŠENO.** Backend URL je u `BuildConfig.BACKEND_URL` — debug: `10.0.2.2:8080`, release default: `https://tranzit.bus-split.com` (override `-PbackendUrl=...`). |
| ~~P5~~ | ~~Backend hosting~~ | — | **RIJEŠENO.** `transit-api` deployan na VPS-u (`/opt/transit`, Docker Compose, sluša `127.0.0.1:8090`), nginx proxy + Let's Encrypt TLS na **https://tranzit.bus-split.com**. Potvrđeno živo: `/v1/vehicles` vraća žive pozicije. Ostaje automatizirani CD (L4 deploy korak) — trenutno deploy je tar-over-SSH (nema gita na serveru). |
| P6 | **OSRM foot instanca** | `Config.osrmFootUrl` | Default je javni demo OSRM server — nije za produkcijski promet. Treba self-hosted OSRM `foot` ili plaćeni plan. |

### 2.2 Nedovršene implementacije u kodu

| # | Što | Gdje | Napomena |
|---|---|---|---|
| C1 | FCM slanje poruka | `NotificationService.sendArrivalReminder` | Skeleton — samo logira. Implementirati preko Firebase Admin SDK kad P2 bude riješen. |
| C2 | RT→FCM pipeline | backend | A2.2 traži praćenje RT feeda po korisniku i slanje podsjetnika. Trenutno postoji samo registracija tokena. |
| C3 | iOS implementacija | `apps/zagreb-ios/`, `GtfsZipReader.ios.kt`, `Time.ios.kt` | iOS UI je Faza C. `GtfsZipReader.ios` baca `NotImplementedError` — namjerno, dok se ne napiše native ZIP/SHA. |
| ~~C4~~ | ~~FCM token store~~ | — | **RIJEŠENO.** Tokeni se perzistiraju u datoteku (`fcm-tokens.txt`, atomarni write) i preživljavaju restart. Pri rastu zamijeniti pravom bazom. |
| ~~C5~~ | ~~`mode` heuristika u RT feedu~~ | — | **RIJEŠENO.** `GtfsLookup` mapira `route_id` → mode/ime/headsign iz GTFS static; RT feed se time obogaćuje. Usput popravljeni i `routeShortName`/`headsign` placeholderi u dolascima. |
| C6 | Smjer-strelice vozila | `VehicleLayer` | Vozila su krugovi (CircleLayer). Prava ikona vozila + rotacija po bearingu traži registriranu ikonu u stilu — dolazi uz P3. |
| C7 | Design tokeni | `ui/theme/Theme.kt` | **DJELOMIČNO (2026-05-29).** Gola placeholder paleta zamijenjena koherentnim M3 setom iz ZET crvene (pun token set, neutralne površine, dark mode). Finalni Figma tokeni (sekcija 6) i dalje su cilj. |
| ~~C8~~ | ~~Internet vs. poslužitelj na ekranima koji dohvaćaju podatke~~ | `ui/common/LoadError.kt`, `EmptyState.kt`, ekrani | **RIJEŠENO (2026-06-06).** Razlikovanje "nema interneta" vs "poslužitelj nedostupan" izvedeno na svim ekranima koji dohvaćaju podatke: karta (`LiveStatusPill`), `RoutesScreen`, `AlertsScreen` (RT degradacija), `StopDetailScreen` (dolasci — `!isLive && prazno` → degradirano stanje), `RoutinesScreen` (živi dolasci po rutini — `liveByKind`), `NearbyScreen` (prazna lokalna baza → jednokratni sync + internet/poslužitelj klasifikacija, kao Linije). `FavoritesScreen`/`SearchScreen` namjerno netaknuti — prazna stanja su tamo legitimna. |
| C9 | Vizualna verifikacija novih UI stanja | — | Error/empty stanja kompajliraju i backend testovi prolaze, ali nova stanja nisu vizualno potvrđena (emulator bez RAM-a na dev stroju). Provjeriti na stvarnom uređaju ili emulatoru s više RAM-a. |

### 2.3 Procesno / prije launcha

| # | Što | Napomena |
|---|---|---|
| L1 | Feed-validacija (bivši Spike) | 24h polling RT feeda, pregled GTFS ZIP-a, MapLibre fps test, procjena troška tilesa — Faza 0 zadatak iz plana, još nije odrađeno na stvarnom ZET feedu. |
| L2 | Privacy policy | Prije launcha (GDPR, sekcija 10 plana). |
| L3 | Crash reporting | Crashlytics ili Sentry — odlučiti i integrirati prije launcha. |
| ~~L4~~ | ~~CI/CD~~ | **RIJEŠENO.** `.github/workflows/zet-app.yml` buildaj + testira backend i Android na svaki push/PR, uploada APK i test izvještaje. Deploy korak (CD) dolazi uz P5 (hosting). |
| L5 | Play Console | Registracija developera (€25), interni track za beta. |
| L6 | Beta-kohorta | 5–10 dnevnih putnika za A0 testiranje (DoD, sekcija 7 plana). |
| ~~L7~~ | ~~Realan GTFS test~~ | **DJELOMIČNO RIJEŠENO (2026-05-29).** Backend pokrenut protiv stvarnog ZET feeda, parsiranje potvrđeno (~322 vozila, 155 linija, 3839 stajališta); usput popravljen OOM (vidi 1.4). Ostaje 24h feed-stabilnost (L1). |

---

## 3. Sljedeći koraci po planu

Faza A je skeletonski gotova. Po `zagreb-app-plan.md`:

1. **Riješiti produkcijske rupe** P1–P6 (vanjski servisi).
2. **Faza 0 zaostatak** — feed-validacija (L1), design tokeni (C7), CI/CD (L4).
3. **Faza B** — diferencijacija: "hoću li stići" (B1), pametni fallback (B2),
   disruption layer (B4) itd.
4. **Faza C** — iOS UI nad KMP `:shared` slojem (C3).
