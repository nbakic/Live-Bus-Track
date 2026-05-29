# transit-api

Backend za ZET transit aplikaciju — Kotlin/Ktor. **Jedina ZET-okrenuta točka** u
cijelom sustavu: klijenti (Android, iOS) gađaju isključivo ovaj backend, nikad ZET
izravno (sekcija 5 plana — D1).

Naziv je grad-neutralan namjerno — ako se proširi na druge GTFS izvore, ostaje točan.

## Što radi

ZET-okrenuti sloj: dohvaća GTFS-RT protobuf i GTFS static ZIP, servira JSON.
TTL cache (~10 s za RT, single-flight) znači da ZET vidi backend kao jednog
potrošača neovisno o broju klijenata. `/v1` je verzionirani ugovor — breaking
promjene idu u `/v2`, ne mijenjaju `/v1`.

| Endpoint | Funkcija | Izvor |
|---|---|---|
| `GET /health` | health check za deploy | — |
| `GET /v1/vehicles` | žive pozicije vozila | GTFS-RT |
| `GET /v1/stops/{id}/arrivals` | predviđeni dolasci | GTFS-RT TripUpdate |
| `GET /v1/alerts` | prometne obavijesti | GTFS-RT Alert |
| `GET /v1/routes` | sve linije | GTFS static |
| `GET /v1/stops` | sva stajališta | GTFS static |
| `GET /v1/routes/{id}/shape` | geometrija rute | `shapes.txt` |
| `GET /v1/routes/{id}/schedule` | vozni red linije | `stop_times.txt` (streaming) |
| `GET /v1/gtfs/static.zip` | sirovi GTFS ZIP | proxy za klijentski import |
| `GET /v1/walk` | pješačka ruta | OSRM `foot` proxy |
| `GET /v1/plan` | planiranje rute A→B | GraphHopper `pt` (treba ključ) |
| `POST /v1/notifications/register` | registracija FCM tokena | — |

RT feed se obogaćuje iz GTFS statica (`GtfsLookup`: `route_id` → ime / mode /
headsign). **Napomena:** `stop_times.txt` (~120 MB raspakirano) se **streama** red
po red pri gradnji voznog reda — učitavanje cijele datoteke kroz `CsvParser` je
rušilo JVM (OOM na prvom `/v1/vehicles`). Heap je ograničen u `applicationDefaultJvmArgs`.

## Preduvjeti

- **JDK 17+** (Kotlin 2.0 / Ktor 2.3; Java 8 NE radi).

## Pokretanje

```bash
./gradlew run            # pokreće na portu 8080
./gradlew test           # unit testovi (TTL cache, GTFS-RT mapper)
./gradlew installDist    # build distribucije za deploy
```

Konfiguracija preko env varijabli (vidi `Config.kt`): `PORT`, `RT_CACHE_TTL`,
`ZET_GTFS_RT_URL`, `ZET_GTFS_STATIC_URL`.

## Sljedeće

Preostale produkcijske rupe prate se u [`docs/TODO.md`](../../docs/TODO.md) —
ključno: hosting + CD (P5), GraphHopper API ključ (P1), self-hosted OSRM (P6),
te 24h feed-validacija na stvarnom ZET feedu (L1). Backend je potvrđeno
pokrenut protiv stvarnog feeda (vidi TODO §1.4).
