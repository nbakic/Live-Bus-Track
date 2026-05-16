# transit-api

Backend za ZET transit aplikaciju — Kotlin/Ktor. **Jedina ZET-okrenuta točka** u
cijelom sustavu: klijenti (Android, iOS) gađaju isključivo ovaj backend, nikad ZET
izravno (sekcija 5 plana — D1).

Naziv je grad-neutralan namjerno — ako se proširi na druge GTFS izvore, ostaje točan.

## Što radi (Faza 0 skeleton)

Pass-through cache nad ZET GTFS-RT protobuf feedom:

| Endpoint | Funkcija plana | Opis |
|---|---|---|
| `GET /v1/vehicles` | A0.1 | Žive pozicije vozila |
| `GET /v1/stops/{stopId}/arrivals` | A0.2 | Predviđeni dolasci |
| `GET /v1/alerts` | A0.4 | Service alerts |
| `GET /health` | — | Health check za deploy |

Backend dohvaća protobuf, parsira ga i servira JSON. TTL cache (~10 s, single-flight)
znači da ZET vidi backend kao jednog potrošača neovisno o broju klijenata.

`/v1` je verzionirani ugovor — breaking promjene idu u `/v2`, ne mijenjaju `/v1`.

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

## Sljedeće (Faza 0 → A0)

- GTFS static ZIP proxy + parsiranje (`/v1/routes`, `/v1/stops`).
- Razrješenje `route_short_name`/`headsign`/`mode` iz GTFS static (sad placeholderi).
- Kontraktni testovi `/v1` ugovora; CD pipeline na EU hosting (sekcija 14 plana).
- A2: routing (OTP/GraphHopper) i push (FCM).
