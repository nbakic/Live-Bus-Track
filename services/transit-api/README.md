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
`ZET_GTFS_RT_URL`, `ZET_GTFS_STATIC_URL`, `GRAPHHOPPER_API_KEY`.

## Deploy (produkcija)

Backend trči u Dockeru na VPS-u i sluša **samo na loopbacku** (`127.0.0.1:8090`);
nginx terminira TLS i `proxy_pass`-a na njega. App nikad nije izravno izložen
internetu.

```
Internet ──TLS──▶ nginx :443 ──▶ 127.0.0.1:8090 ──▶ docker: transit-api :8080
                  (tranzit.bus-split.com)              (-Xmx1536m, healthcheck /health)
```

Produkcijska instanca: **https://tranzit.bus-split.com** (Ubuntu 24.04 VPS,
`/opt/transit`). Cert je Let's Encrypt preko `certbot --nginx` (auto-renew).

Datoteke za deploy (ovaj direktorij):

- `Dockerfile` — multi-stage build (JDK build → slim JRE runtime, non-root).
- `docker-compose.yml` — bind na `127.0.0.1:8090`, `restart: unless-stopped`,
  memory limit, log rotacija.
- `.env.example` — opcionalni override-i (GraphHopper ključ, ZET URL-ovi).
- `deploy/nginx-tranzit.conf` — nginx vhost (pre-TLS; certbot dopiše 443 blok).

Prvi deploy / update:

```bash
# 1. sync izvora na server (npr. tar | ssh, ili git pull) u /opt/transit
# 2. na serveru:
cd /opt/transit
cp -n .env.example .env          # po potrebi upiši GRAPHHOPPER_API_KEY
docker compose up -d --build     # build image + (re)start container
docker compose logs -f           # provjera

# nginx vhost (jednom):
sudo cp deploy/nginx-tranzit.conf /etc/nginx/sites-available/tranzit.bus-split.com
sudo ln -sf /etc/nginx/sites-available/tranzit.bus-split.com /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx

# TLS (jednom; certbot dalje sam obnavlja):
sudo certbot --nginx -d tranzit.bus-split.com
```

Provjera: `curl https://tranzit.bus-split.com/health` → `{"status":"ok"}`.

## Sljedeće

Preostale produkcijske rupe prate se u [`docs/TODO.md`](../../docs/TODO.md) —
ključno: hosting + CD (P5), GraphHopper API ključ (P1), self-hosted OSRM (P6),
te 24h feed-validacija na stvarnom ZET feedu (L1). Backend je potvrđeno
pokrenut protiv stvarnog feeda (vidi TODO §1.4).
