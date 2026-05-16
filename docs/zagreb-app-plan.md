# Zagreb Transit App — Detaljan plan (v6 — U IZRADI)

> **Status:** Rad je započeo. Spike više nije zasebna kapija — njegovi zadaci
> apsorbirani su u Fazu 0 (vidi sekciju 13).
> **Datum:** 2026-05-16
> **Evolucija:** v2 razbila Fazu A na A0/A1/A2 i uvela Metrike/Rizike/Design/Testing.
> v3 kalibrirala procjene, dodala konkurentsku analizu i operativni trošak. v4 pomaknula
> widget/rutine u MVP, uvela backend od dana 1, povisila A0 procjenu. v5 izdvojila Spike
> kao go/no-go kapiju, potvrdila iOS/KMP, podigla Faza 0 procjenu.
> **Promjene v5→v6:**
> · **Spike više nije zasebna kapija** — RT feed validacija, GTFS ZIP pregled, MapLibre
> fps test i procjena troška tilesa apsorbirani su u Fazu 0 kao njezini prvi zadaci.
> · Faza 0 procjena podignuta na 5–7 tjedana (apsorbira Spike posao).
> · Rad na skeletonima (Android KMP, Ktor backend) je započeo.

---

## 1. Kontekst i cilj

Postojeći repozitorij sadrži **Split Bus Tracker** — web/PWA aplikaciju za live praćenje
splitskog Prometa (React + Vite, Express proxy na `api.promet-split.hr`).

**Zadatak:** napraviti **Zagreb (ZET) aplikaciju** u **istom repozitoriju**, pri čemu se
postojeći Split kod **ne dira**. Cilj:

1. **Faza A** — vjerno klonirati funkcionalnosti postojećih ZET aplikacija
   (transitopia "ZET info" / "moj ZET" i `zet-uzivo.com`), razbijena na podfaze.
2. **Faza B** — oplemeniti dodatnim funkcijama.
3. **Platforma:** native Android prvo — Kotlin + Jetpack Compose, MVVM / clean arhitektura.
4. **Naglasak:** lijep dizajn i UX (zato dizajn ima vlastitu fazu — sekcija 6).

### Odluke (potvrđene)

| Pitanje | Odluka |
|---|---|
| Split kod | Ostaje netaknut |
| Zagreb app | Novi projekt u istom monorepu |
| Prva platforma | Native Android (Kotlin + Compose) |
| Arhitektura | Clean (UI / domain / data), MVVM na UI sloju |

### Odluke koje treba donijeti PRIJE prvog reda koda

Ove tri odluke mijenjaju arhitekturu, trošak i rok — vidi sekcije 3, 4, 5.
**Ne smiju se donositi usput.**

| # | Odluka | Preporuka u ovom planu |
|---|---|---|
| D1 | Backend: da/ne i kada | Lagani backend **od početka** (vidi 5) |
| D2 | Kotlin Multiplatform: da/ne, sada | **Da** — domain+data kao KMP od dana 1 (vidi 4.3) |
| D3 | Karta: MapLibre vs Mapbox | MapLibre + tile provider — uz protuargument (vidi 4.5) |

---

## 2. Izvor podataka — ZET API

ZET nudi **standardni GTFS** pod **Otvorenom dozvolom Republike Hrvatske**
(`data.gov.hr/otvorena-dozvola`). Standardni format → gotove biblioteke, parseri, ekosustav.

### Endpointi (s `https://zet.hr/gtfs2`)

| Feed | Endpoint | Sadržaj |
|---|---|---|
| **GTFS Realtime** | `/gtfs-rt-protobuf` | Žive pozicije, kašnjenja (TripUpdate), alerts (protobuf) |
| **GTFS Static** | `/gtfs-scheduled/latest` | Linije, stajališta, redovi vožnje, `shapes.txt` (ZIP) |

> **Provjera u Fazi 0:** potvrditi base-URL-ove i — bitno — status RT feeda. Stranica
> nosi oznaku "SAMO ZA POTREBE TESTIRANJA". To je **rizik R1** (sekcija 9): feed-validacija
> na početku Faze 0 (sekcija 13) testira je li produkcijski stabilan.

### GTFS static freshness — konkretan mehanizam

GTFS nema standardni version header. Mehanizam osvježavanja:
1. Periodički (npr. 1×/24 h, WorkManager) preuzeti `/gtfs-scheduled/latest`.
2. **Primarno: SHA-256 checksum ZIP-a** — `Last-Modified`/`Content-Length` na GTFS
   endpointima često nisu pouzdani, pa služe samo kao brza preliminarna provjera.
3. Potvrda: `feed_info.txt` (`feed_version`, `feed_start/end_date`) iz ZIP-a.
4. Ako se promijenilo → parsirati u staging bazu, atomarno zamijeniti.

---

## 3. Inventar funkcionalnosti i razbijanje Faze A

### 3.1 Postojeći Split app (referenca — što već znamo)

Live karta vozila · smjer-strelice · filter po liniji · detalji vozila · info panel
stajališta (reverse-geocoding) · geometrija ruta (OSM/OSRM) · slojevi karte · status bar ·
PWA install · demo fallback.

### 3.2 Faza A — razbijena na podfaze

V1 je imao 11 funkcija u jednoj "Fazi A" — od kojih su tri (planiranje rute, push,
prave RT predikcije) zasebni projekti. Razbijeno:

> **Napomena:** svi klijentski podaci idu kroz `transit-api` backend (sekcija 5),
> nikad izravno na ZET. Kolona "Backend rad" ispod označava treba li funkcija
> backend **logiku** osim pukog pass-through cachea.

#### A0 — Pravi MVP za dnevnog putnika

A0 je usklađen s **primarnom personom — dnevni putnik** (sekcija 11). Dnevni putnik
zna svoje linije; ne otvara app da bi tražio stajalište — treba informaciju koja ga
**dočeka**. Zato widget i rutine pripadaju MVP-u, ne kasnijoj diferencijaciji.

| # | Funkcija | Backend logika |
|---|---|---|
| A0.1 | Live karta vozila (tramvaji+busevi, klaster, smjer) | Cache |
| A0.2 | Dolasci na stajalište — GTFS-RT TripUpdate predikcije | Cache |
| A0.3 | Omiljena stajališta (lokalno) | Ne |
| A0.4 | Service alerts (GTFS-RT Alert) | Cache |
| A0.5 | Lokacija + "najbliža stanica" (vidi napomenu o pješačkom routingu niže) | Cache |
| A0.6 | Statički vozni red po stajalištu (GTFS static) | Cache |
| **A0.7** | **Home-screen widget — sljedeći dolazak na omiljenom stajalištu** (bivši B5) | Cache |

→ **A0 = MVP koji stvarno služi dnevnog putnika.** Definicija "Faza A MVP gotova".

> **Pješački routing za A0.5:** "najbliža stanica preko pješačkih putova" (obećanje iz
> konkurentske analize, sekcija 11) nije besplatna. **Odluka:** u A0 koristiti
> jednostavnu zračnu udaljenost + jasnu oznaku; pravi pješački routing dolazi u A1.6.
> Ne obećavati pješački routing u A0 UI-u.

#### A1 — Linije, pretraga, rutine

| # | Funkcija | Backend logika |
|---|---|---|
| A1.1 | Pregled svih linija, detalji linije, smjerovi | Cache |
| A1.2 | Geometrija rute iz `shapes.txt` | Cache |
| A1.3 | Pretraga: stajališta, linije, adrese (dijakritika-neutralna) | Ne |
| A1.4 | Kompletan statički red vožnje po liniji | Cache |
| **A1.5** | **Moje rutine (Dom/Posao/Škola) + jutarnji ekran** (bivši B3) | Cache |
| A1.6 | Pješački routing za "najbliža stanica" (nadogradnja A0.5) | **Da** — lagani pješački graf |

> **A1.6 ne smije povući OTP/GraphHopper u A1.** Puni transit routing engine (OTP)
> traži 2–4 GB RAM-a i podiže operativni trošak na razinu A2. Za samo pješački
> "najbliža stanica" to je prekomjerno. **Odluka:** A1.6 koristi **lagani pješački
> graf** (OSRM `foot` profil — znatno lakši) na backendu; puni transit engine (OTP/
> GraphHopper) stiže tek u A2.1. Tako operativni trošak ostaje nizak do A2 (sekcija 14).

#### A2 — Planiranje rute i notifikacije (svaka je zaseban projekt)

| # | Funkcija | Backend logika |
|---|---|---|
| A2.1 | Planiranje rute A→B | **Da** — OTP/GraphHopper |
| A2.2 | Push notifikacije (podsjetnici + promjene) | **Da** — prati RT, šalje FCM |

> A2.1: vlastiti GTFS router u Kotlinu je sam za sebe projekt → koristimo gotov engine.
> **OTP vs GraphHopper — odlučiti prije A2:** OTP je transit-native (GTFS prvorazredan),
> ali traži 2–4 GB RAM-a; GraphHopper je lakši, ali transit (GTFS) podrška mu je slabija.
> Preporuka: **GraphHopper** ako trošak hostinga dominira, **OTP** ako kvaliteta transit
> routinga dominira. Odluka mijenja RAM zahtjev i operativni trošak (sekcija 14).

### 3.3 Faza B — diferencijacija

> B3 (rutine) i B5 (widget) **premješteni u MVP** (A1.5, A0.7) — usklađivanje s
> primarnom personom. Faza B sad sadrži samo prave nadgradnje iznad MVP-a.

| # | Funkcija | Vrijednost |
|---|---|---|
| B1 | "Hoću li stići?" režim | Cilj+vrijeme → "kreni za X min", procjena rizika |
| B2 | Pametni fallback | "Ne čekaj X, idi 4 min do druge stanice, uzmi Y" |
| B4 | Disruption layer | Radovi/prekidi prevedeni u "što to znači za mene" |
| B6 | Multimodalno (HŽ, bicikl, pješice) | Najkasnija faza |
| B7 | Slojevi karte / satelit | Iz Split appa |
| B8 | Korisničke prijave (gužva/kašnjenje) | Kao transitopia |
| B9 | "Jednostavan način" — veliki gumbi Dom/Posao/Škola | Za starije/djecu |

> **a11y NIJE u Fazi B.** TalkBack, WCAG AA kontrast, dinamička veličina fonta,
> touch targeti — to je **baseline od Faze 0** (sekcija 6). B9 je zaseban koncept
> (pojednostavljeni UI mod), ne pristupačnost.

> **B8 cold-start problem:** korisničke prijave nemaju vrijednost bez kritične mase
> korisnika. Mitigacija: B8 se ne lansira dok app nema dovoljnu bazu; u međuvremenu
> prijave hrane samo internu telemetriju (validacija RT podataka), ne korisnički UI.
> Alternativa: seed-ati prijave iz GTFS-RT Alert feeda dok korisnička masa ne naraste.

---

## 4. Arhitektura — Android (Kotlin + Compose)

### 4.1 Slojevi

```
:app          — Compose UI, navigacija, DI wiring   (Android)
 ├─ ui/        — Composables, screens, theme, design system
 ├─ viewmodel/ — ViewModels (MVVM state holderi)
:domain        — čista Kotlin logika                (KMP — vidi 4.3)
 ├─ model/ usecase/ repository/ (interfaci)
:data          — implementacija data sloja          (KMP — vidi 4.3)
 ├─ remote/ local/ repository/ gtfs/
```

### 4.2 Tehnologije

| Sloj | Izbor | Razlog |
|---|---|---|
| UI | Compose + Material 3 | Tražen |
| Karta | **MapLibre** + plaćeni tile provider | Kontrola stila; **trošak — R5** |
| Mreža | Ktor (KMP) ili Retrofit | Ktor ako idemo KMP |
| GTFS-RT | `gtfs-realtime-bindings` (protobuf) | Službene bindinge |
| Lokalna baza | Room (KMP-kompatibilan) / SQLDelight | SQLDelight ako KMP |
| Postavke/favoriti | DataStore / Multiplatform Settings | KMP varijanta ako KMP |
| DI | Hilt (Android) / Koin (ako KMP) | KMP-kompatibilnost |
| Async | Coroutines + Flow | Reaktivni state |
| Push | FCM + WorkManager | Faza A2 |
| Crash | Crashlytics ili Sentry | Od dana 1 — sekcija 10 |

### 4.3 Razrješenje D2 — Kotlin Multiplatform: **DA** (odluka zaključena)

Otvorena je bila dvojba: Nenadova uputa *"framework za Android / iOS odmah"* može se
čitati dvoznačno — (a) dual-platform od starta, ili (b) "radi kao native app, ne web".
**Dvojba je eksplicitno razriješena: iOS JEST u opsegu 1.0.** Time KMP nije preuranjena
optimizacija, nego odgovor na potvrđeni zahtjev — odluka je zaključena, ne pretpostavka.

Posljedica: iOS nije "možda Faza C" — KMP shared sloj se gradi od dana 1. `:domain` i `:data` se pišu kao čisti Kotlin
(što plan ionako predviđa), pa KMP daje **60–70% iOS koda besplatno** — GTFS parser,
modeli, use-caseovi, repozitoriji, mrežni sloj se dijele; UI ostaje native
(Compose / SwiftUI).

**Posljedica za tooling:** Ktor umjesto Retrofit, SQLDelight umjesto Room (iako Room
sad ima i KMP podršku — SQLDelight ostaje sigurniji izbor), Koin umjesto Hilt,
Multiplatform Settings umjesto DataStore. Odlučiti **prije** prvog reda u
`:domain`/`:data`.

**Trošak KMP-a (pošten protuargument):** build je 1.5–2× sporiji od čistog Androida;
debugiranje Kotlin/Native za iOS je i dalje bolnije od native Swifta. **Prihvaćamo**
jer je iOS po Nenadovoj uputi obavezan — alternativa (pure-Android pa rewrite za iOS)
je skuplja. **Fallback:** ako se iOS naknadno ipak otkaže, čist Kotlin u
`:domain`/`:data` ostaje upotrebljiv; gubi se samo KMP toolchain overhead.

### 4.5 Razrješenje D3 — Karta: MapLibre (uz protuargument)

**Protuargument za Mapbox:** ima besplatni tier (~50k MAU) koji za rani app često
pokriva sav promet, i ljepši default vizualni dojam — uz cilj "lijep dizajn", to je
najjeftiniji put do lijepog izgleda u A0.

**Zašto svejedno MapLibre:** nema vendor lock-in; izlazni trošak je predvidiv (kad
Mapbox free tier presuši, cijena raste s korisnicima); puna kontrola stila pločica
(vlastiti vizualni identitet, ne "Mapbox look"). Lijep dizajn dolazi iz vlastitog
tile stila, ne iz vendora. **Trošak:** treba plaćeni tile provider (R3) i više
posla na stilu. **Preporuka: MapLibre** — ali D3 je realno otvoren; Mapbox free
tier je legitiman izbor za brži A0 ako se prihvati budući lock-in.

### 4.6 Tok podataka i polling

```
GTFS Static (ZIP) ──parse──► lokalna baza ──┐
                                             ├──► Repository ──► UseCase ──► ViewModel ──► UI
GTFS-RT (protobuf) ──adaptivni polling───────┘
```

**Adaptivni polling (ne naivnih 10–20 s):**
- Pauza kad je app u backgroundu.
- Frekvencija vezana na **zoom level**: zoom-out → rjeđe (korisnik ne prati pojedino
  vozilo); zoom-in na stajalište/vozilo → češće.
- Niža frekvencija kad u vidnom polju karte nema vozila.
- Exponential backoff na greške feeda.
- Opcija "ne osvježavaj na mobilnim podacima" u postavkama.
- Cilj: minimalan battery/data drain (rizik R4).

---

## 5. Razrješenje D1 — Backend: klijent ide kroz backend od dana 1

V2/v3 su rekli "backend postoji od Faze 0, ali A0/A1 rade bez njega — klijent gađa ZET
direktno". To je propuštena prilika. Direktno gađanje ZET-a iz svakog klijenta znači:
- svaki uređaj troši ZET rate-limit; ZET vidi promet kao mnogo IP-eva → rizik blokade,
- **R1 (RT feed nestane) postaje katastrofa** — nema cached state,
- nemoguća kasnija server-side agregacija/poboljšanja predikcija bez breaking changea.

**Ispravak: klijent od dana 1 gađa `transit-api` (vlastiti backend), nikad ZET izravno.**
U A0 je `transit-api` samo **pass-through cache od ~10 s** nad ZET feedovima — gotovo
bez dodatnog koda, a rješava sve gore navedeno. Kasnije isti endpoint dobiva routing,
push i agregaciju bez ijedne promjene na klijentskom ugovoru.

`transit-api` od početka preuzima:
- **A0**: pass-through cache RT + statičkog feeda (jedina ZET-okrenuta točka u sustavu),
- **A2**: routing (OTP ili GraphHopper s GTFS-om) i push (prati RT → FCM),
- kasnije: agregaciju korisničkih prijava (B8).

**Stack backenda:** Kotlin/Ktor — dijeli GTFS modele i parser s KMP `:domain`/`:data`
modulima (jedan jezik, dijeljeni kod). Alternativa Node/Go odbačena zbog gubitka
dijeljenja koda. Živi u monorepu kao `services/transit-api/`.

**Verzioniranje API-ja:** klijent↔backend ugovor verzioniran od v1 (`/v1/...`) — starije
instalacije aplikacije ostaju u upotrebi, pa backend mora podržavati više verzija.

---

## 6. Design proces (prije Compose koda)

"Lijep dizajn" ne smije biti slučajan ishod Material 3 defaulta. Koraci:

1. **Low-fi wireframei** — svi glavni ekrani, tokovi (karta, stajalište, linije, pretraga).
2. **High-fi mockupi** (Figma) — vizualni identitet, ZET-usklađena paleta, stanja.
3. **Design tokeni** — boje, tipografija, razmaci, ikonografija → izvezeno u kod.
4. **Compose implementacija** prema tokenima; `@Preview` za komponente.

**a11y kao baseline od Faze 0:** WCAG AA kontrast, TalkBack/semantika, dinamička
veličina fonta (sp), touch targeti ≥ 48dp, podrška za tamnu temu.

> Napomena: Compose `@Preview` renderira **komponentu po komponentu**, nije alat za
> pregled cijelog flowa. Za flow review koristiti Figma prototip i device mirroring.

---

## 7. Metrike i Definition of Done

| Metrika | Cilj |
|---|---|
| Cold start | < 2 s |
| **Time-to-first-vehicle** (tap → prvo vidljivo vozilo na karti) | < 3 s |
| Karta @ 200 vozila | drži 60 fps |
| Svježina dolaska (p95) | < 15 s od stvarnog RT vremena |
| Crash-free sesije | > 99.5 % |
| GTFS static parse | < 3 s na srednjem uređaju |
| App veličina (APK) | < 25 MB |
| a11y | WCAG AA prolaz na svim glavnim ekranima |

**Definition of Done po podfazi:**
- **A0 done** = svih 7 A0 funkcija radi, gornje metrike zadovoljene, a11y prolaz,
  testovi za GTFS parser + use-caseove zeleni, dostupno na Play internal track-u,
  **i — 5–10 stvarnih dnevnih putnika koristi A0 tjedan dana i dalo feedback.**
  Bez tog koraka gradimo u vakuumu i probleme otkrivamo tek na Open beti, skupo.
- **A1/A2 done** = analogno (uklj. korisnički feedback krug), plus DoD specifičan za podfazu.

---

## 8. Testing & CI/CD

**Klijent / Android testovi (od dana 1, ne na kraju):**
- Unit: use-caseovi i **GTFS static parser** (kritičan kod — prioritet).
- **Snapshot testovi GTFS-RT (protobuf) parsiranja** na sačuvanim payloadima — RT
  parser je jednako kritičan i podložniji promjenama feeda; snapshot hvata regresiju
  kad ZET promijeni format.
- Instrumentation: lokalna baza (SQLDelight/Room) migracije i upiti.
- Screenshot (Paparazzi): ključni Composeovi, regresija dizajna.
- Smoke E2E: jedan happy-path (otvori app → karta → stajalište → dolasci).

**Backend (`transit-api`) testovi — vlastiti plan, ne podskup Android testova:**
- Unit: logika cache invalidacije i TTL-a; logika version-routinga (`/v1`, `/v2`).
- Integration: GTFS feed proxy — preuzimanje, parsiranje, degradacija kad ZET ne odgovara.
- **Kontraktni testovi klijent↔backend** — `/v1/...` ugovor; starije instalacije
  aplikacije ne smiju puknuti kad backend evoluira.

**CI/CD (GitHub Actions, od dana 1):**
- **Android:** build + test + lint na svaki PR; signing config (keystore u secrets);
  auto-deploy na Play **internal track** za beta testere.
- **Backend:** build + test na svaki PR; **CD pipeline za deploy `transit-api`** na
  hosting (staging → produkcija); migracije i health-check pri deployu.

---

## 9. Rizici i mitigacije

| # | Rizik | Mitigacija |
|---|---|---|
| R1 | RT feed "samo za testiranje" — ukidanje/promjena formata | **Egzistencijalni rizik — vidi plan B niže.** Feed-validacija na početku Faze 0 testira stabilnost feeda; abstrakcija feeda iza interfacea; verzioniranje parsera |
| R2 | GTFS static ima greške/rupe (često kod manjih agencija) | Validacija pri parsiranju; graceful skip neispravnih zapisa; fallback na zadnji ispravan ZIP |
| R3 | Trošak map tilesa (MapLibre tiles nisu besplatni) | Procijeniti OpenMapTiles/Stadia/MapTiler cijene u Fazi 0; budžet |
| R4 | Battery/data drain od pollinga + renderiranja | Adaptivni polling (4.6); profiliranje; metrike iz sekcije 7 |
| R5 | App store policy oko realtime lokacije | Foreground-only lokacija; jasan opt-in; bez background trackinga u A0 |
| R6 | Routing engine (OTP) operativni trošak/složenost | Procijeniti hosting u Fazi 0; A2 je odvojiva faza ako se odgodi |
| R7 | Konkurenti jaki (100k+ downloada, 4.8 ocjena) | Ne frontalno; diferencijacija Fazom B; ciljati boljke konkurenata (sekcija 11) |

### Plan B za R1 — ako RT feed nestane

Cijela vrijednost A0 ovisi o GTFS-RT feedu. Ako ZET ukine feed ili ga trajno pokvari:
- **A0.2 (dolasci) i A0.4 (alerts) degradiraju na statički vozni red** — app i dalje
  funkcionira kao kvalitetan offline planer reda vožnje, samo bez "uživo" predikcija.
- **A0.1 (live karta vozila)** je jedina funkcija koja potpuno otpada bez RT-a.
- Arhitektura mora od početka tretirati RT kao **opcionalni sloj nad statičkim
  GTFS-om**, ne kao temelj — tako pad RT-a ne ruši app, samo mu uklanja "live" oznaku.
- UI poruka: "Podaci uživo trenutno nedostupni — prikazan red vožnje."

**Što ako ZET izričito zatraži da prestanemo?** Feed je objavljen pod Otvorenom
dozvolom RH, koja formalno ne dopušta naknadno zabraniti korištenje objavljenih
podataka. Ako se to pitanje ipak pojavi, prioritet je dijalog, ne konfrontacija
pozivanjem na licencu — ali to nije tema koju otvaramo proaktivno.

---

## 10. Privatnost, licence, analitika

- **GDPR/privatnost:** privacy policy prije launcha; lokacija = eksplicitan opt-in,
  **samo dok je app otvoren**; politika zadržavanja za favorite i (B8) prijave;
  podaci EU-hostani.
- **Atribucija Otvorene dozvole:** navesti izvor (ZET) u **About ekranu** i u
  **footeru karte**.
- **Analitika:** GDPR-prihvatljiv, lagani analytics (ne Firebase Analytics);
  crash reporting Crashlytics ili Sentry. Odluka **prije launcha**.
- **Lokalizacija:** HR + EN (turisti); pretraga neosjetljiva na dijakritiku
  ("Jelacica" ↔ "Jelačića").

---

## 11. Persone i konkurentska analiza

### 11.1 Persone — primarna fiksirana

| Razina | Persona | Vučeni prioriteti |
|---|---|---|
| **Primarna** | **Dnevni putnik** | Widget (A0.7), rutine (A1.5), jutarnji ekran, brzina, pouzdanost dolazaka |
| Sekundarna | Povremeni korisnik | Jednostavnost, pretraga, planiranje rute |
| Tercijarna | Turist | EN lokalizacija, vizualni ID linije — pokriven općim baselineom |
| Tercijarna | Stariji | Pokriven a11y baselineom + B9 (jednostavan način) |

**Primarna persona = dnevni putnik.** Ona je sidro za design proces (sekcija 6) i
prioritizaciju Faze B (B1, B4 prvi). Dnevni putnik zna svoje linije — ne treba mu
edukacija, treba mu **brz, pouzdan odgovor "kad i odakle"** i informacija koja ga
dočeka (widget/notifikacija), a ne app koji mora otvarati. Zato su widget i rutine
u MVP-u (A0.7/A1.5), a ne u Fazi B.

### 11.2 Konkurentska analiza (grubo istraživanje, Play Store recenzije)

| App | Snaga | Boljke koje vidimo iz recenzija |
|---|---|---|
| **ZET info** (transitopia) | 100k+ instalacija, 4.8, povjerenje | Live tracking nepouzdan (estimacija, često netočno); "Prijavi problem" onemogućen; obavijesti o prometnim promjenama zatrpane irelevantnim tekstom; "najbliža stanica" ne uzima u obzir pješačke putove |
| **moj ZET** (službeni ZET) | Službeni, kupnja karata | Login/auth greške ("interna greška"), reset lozinke ne radi; update-i znaju razbiti app |
| **Google Maps** | Default svima, planiranje rute, multimodalno | Generičan; nema live ZET pozicije vozila; nema widget/rutine/alerts orijentirane na ZET; nije "zagrebački" |
| **zet-uzivo.com** | Web, live karta (Faza A klon meta iz sekcije 1) | Web-only, nema native app/widget/push; ovisi o browseru |
| **Moovit / Citymapper** | Globalni, pokrivaju Zagreb, planiranje rute | Generični svjetski alati; nije lokalni fokus; live ZET pozicije ograničene; bez ZET-specifičnih rutina/widgeta |

**Posljedica za strategiju:** Google Maps, Moovit i Citymapper već pokrivaju **turista
i povremenog korisnika** (planiranje rute A→B). To znači da je obrambena niša **dnevni
putnik** — što potvrđuje izbor primarne persone i selidbu widgeta/rutina u MVP.
Generička "još jedna ZET aplikacija" gubi i od Google Mapsa i od ZET info; pobjeđuje
se samo na onome što oni ne rade: native live pozicije + widget + rutine + pouzdanost
+ lokalni fokus.

**Diferencijacijsko sidro (konkretizira R7):**
1. **Pouzdanost > sirovi live feed** — gdje je RT slab/nedostaje, jasno komunicirati
   povjerenje u podatak (estimacija vs. potvrđeno), ne lažirati "live".
2. **Funkcionalan "Prijavi problem"** (B8) — kod ZET info onemogućen.
3. **Obavijesti prevedene u "što to znači za mene"** (B4) — ne zid teksta.
4. **"Najbliža stanica" preko pješačkih putova**, ne zračne linije.
5. Bez prisilnog logina za osnovne funkcije — A0/A1 rade bez računa.

> Ovo je grubo istraživanje iz Play Store recenzija; Faza 0 ga produbljuje
> (više recenzija, r/croatia, iOS App Store).

---

## 12. Mjesto u repozitoriju

```
Live-Bus-Track/
├─ artifacts/bus-tracker/   ← Split web app (NE DIRAMO)
├─ artifacts/api-server/    ← Split API proxy (NE DIRAMO)
├─ apps/zagreb-android/     ← NOVO: Android UI (Compose) — Gradle root za KMP
│  └─ shared/               ← KMP shared moduli (:domain, :data) — dijeljeni s iOS-om
├─ apps/zagreb-ios/         ← NOVO: iOS UI (SwiftUI) — gradi se u Fazi C nad shared/
├─ services/transit-api/    ← NOVO: backend (cache, routing, push) — Kotlin/Ktor
└─ docs/zagreb-app-plan.md  ← ovaj dokument
```

`apps/zagreb-ios/` postoji od početka (barem kao mapa s README-om "iOS UI — Faza C;
shared kod živi u `zagreb-android/shared/`") da iOS ne bude skrivena pretpostavka koja
se otkriva tek u Fazi C. Nazivi su grad-neutralni (`transit-api`, ne `backend-zagreb`)
— ako se backend proširi na Split GTFS, naziv ostaje točan. Sve je neovisno o pnpm
monorepu / Vercel buildu.

---

## 13. Fazni plan s grubim procjenama

> **Kalibracija:** procjene pretpostavljaju jednog developera uz AI asistenciju **s
> iskustvom u KMP-u i Ktor server-sideu**. KMP toolchain ima vlastitu krivulju
> (Gradle konvencije, `expect`/`actual`, sporiji buildovi). **Bez tog iskustva dodati
> +50% na Fazu 0 i A0.** Rasponi su grubi (±50%).

### Faza 0 — feed validacija, design i skeleton

> **Spike više nije zasebna kapija.** Ranije verzije plana izdvajale su Spike kao
> izolirani tjedan 1 s go/no-go kapijom. v6 ga apsorbira u Fazu 0: feed-validacijski
> zadaci su sada **prvi zadaci Faze 0** i rade se paralelno sa skeleton/CI postavljanjem.
> Rizik R1 (RT feed) i dalje vrijedi — feed se validira rano u Fazi 0, a arhitektura
> tretira RT kao opcionalni sloj nad statičkim GTFS-om (Plan B, sekcija 9).

Feed-validacijski zadaci, koji se rade na početku Faze 0:
- 24 h kontinuiran polling RT feeda — broj vozila u špici/dolini, kvaliteta TripUpdate
  predikcija u praksi, stabilnost feeda;
- skinuti i pregledati GTFS static ZIP — struktura, kvaliteta, rupe;
- MapLibre fps test na realnom broju vozila;
- procjena troška map tilesa.

| Faza | Sadržaj | Gruba procjena |
|---|---|---|
| **Faza 0** | **Feed validacija (RT polling + GTFS ZIP + map fps + procjena tilesa)**, persone, konkurentska analiza, **kompletan design** (wireframe→mockup→tokeni), skeleton Android KMP + Ktor backend, CI/CD oba, a11y principi, **pokretanje beta-kohorte** | 5–7 tjedana |
| **A0** | MVP za dnevnog putnika: karta, dolasci, favoriti, alerts, lokacija, vozni red, **widget**; uklj. korisnički feedback krug | 8–12 tjedana |
| **A1** | Linije, geometrija ruta, pretraga, vozni red, **rutine**, pješački routing | 2–4 tjedna |
| **A2** | Transit routing (OTP/GraphHopper), push (FCM) | 2–4 tjedna |
| **B** | Diferencijacija (B1, B2, B4, B6–B9), prioritet B1/B4 | 4–8 tjedana |
| **C** | iOS (SwiftUI nad KMP shared slojem) | 3–5 tjedana |

> **Faza 0 je 5–7 tjedana, ne 2–3.** Sadrži feed-validacijske zadatke (ranije Spike —
> ~1 tjedan), kompletan design proces (high-fi mockupi svih glavnih ekrana su sami 1–2
> tjedna ako se rade ozbiljno — a plan inzistira da dizajn nije Material 3 default),
> plus tri skeletona (Android KMP, `:shared` modul, Ktor backend) i dva CI pipelinea.
> Feed validacija i skeleton/CI posao teku paralelno. Što u Fazi 0 ostaje na **skici**:
> detaljni ekrani sekundarnih tokova — finaliziraju se u A0. Što se **finalizira u
> Fazi 0**: design tokeni, glavni tokovi (karta, stajalište, widget), arhitektura
> skeletona.

> **A0 je realno 8–12 tjedana.** Svaka od 7 funkcija nije trivijalna: live karta @
> 60fps s custom MapLibre stilom ~tjedan; RT predikcije + adaptivni polling +
> edge-caseovi (kašnjenje/otkazano/prekid) ~tjedan; GTFS parser sa staging-swap ~3–5
> dana; alerts UI "prevedeno u smisao" ~tjedan; widget ~3–5 dana; plus snapshot
> testovi, a11y, polish, feedback krug. Donji kraj (8 tj.) pretpostavlja gladak tijek.

> **Beta-kohorta se gradi od Faze 0, ne tjedan prije A0 done.** DoD (sekcija 7) traži
> 5–10 dnevnih putnika koji testiraju A0. Ti se ljudi ne pojave sami — Faza 0 uključuje
> zadatak: okupiti krug 8–15 voljnih beta testera (prijatelji, kolege, r/Zagreb,
> lokalna zajednica). Ako se ne počne rano, A0 DoD blokira launch jer nema kohorte.

---

## 14. Operativni trošak

Gruba procjena mjesečnog troška — otkriva je li projekt održiv bez monetizacije.

| Stavka | Procjena | Napomena |
|---|---|---|
| Map tiles (provider) | €0–25 / mj | Besplatni tier pokriva rani volumen; raste s MAU (R3) |
| Backend hosting (A0/A1 — cache + lagani pješački graf) | €5–15 / mj | Mali VPS / serverless; OSRM `foot` profil je lagan i stane ovdje |
| Backend hosting (A2 — OTP/GraphHopper transit routing) | €20–50 / mj | OTP traži 2–4 GB RAM-a — nije lagan; glavna stavka, tek u A2 |
| FCM (push) | €0 | Besplatan do visokog volumena |
| Domena | ~€10 / god | — |
| Crash/analytics (Sentry self-host ili tier) | €0–25 / mj | Free tier dovoljan u početku |
| **Play Console** | **€25 jednokratno** | Registracija developera |

**Sažetak novčanog troška:** A0/A1 faza ~ **€5–40/mj**; nakon A2 (routing) ~ **€30–90/mj**.

> **Glavni trošak nije novac — nego vrijeme.** Cijela izrada (Faza 0 → A0 → A1 → A2)
> je realno **6–12 osoba-mjeseci** rada solo developera uz AI asistenciju (vidi
> procjene, sekcija 13). To je daleko najveća investicija u projektu. €30–90/mj je
> zanemarivo uz to. Plan treba čitati kao "ovo košta pola godine do godine posla",
> ne "ovo košta €60/mj". Novac je tek u zaokruživanju; održivost ovisi o tome je li
> to vrijeme dostupno.

Ako MAU naraste, map tiles i routing hosting su prve stavke koje skaliraju → tada
razmotriti monetizaciju ili sponzorstvo.

> **Regija backenda — odluka u Fazi 0.** `transit-api` mora biti hostan blizu
> korisnika (Frankfurt ili regionalni EU provider — Hetzner/Fly.io EU region). Cilj
> "time-to-first-vehicle < 3 s" (sekcija 7) ide preko backenda; backend u US-u dodaje
> 100–200 ms latencije i izjeda budžet. EU hosting je i GDPR-pozitivan (sekcija 10).

---

## 15. Launch i održavanje

### 15.1 Launch plan

> **Iskren okvir ambicije:** konkurent ima 100k+ instalacija i 4.8 ocjenu, plus
> Google Maps kao default. **Cilj 1.0 nije dominacija tržišta** — realan target je
> nekoliko tisuća korisnika koji biraju app zbog bolje UX-a (live pozicije, widget,
> rutine, pouzdanost). Rast je dugoročan i ovisi o zadržavanju, ne o launch-spikeu.

- **Closed beta** — Play internal track, mali krug testera (Faza 0 CI to već postavlja).
- **Open beta** — Play open testing, šira skupina, prikupljanje crash/feedback podataka.
- **1.0** — produkcija nakon što A0 metrike (sekcija 7) stabilno prolaze.
- **ASO** — naslov, opis, screenshoti, ključne riječi. **Ako se realno borimo za rank,
  ASO je ~tjedan posla, ne usputni zadatak** — uključuje istraživanje ključnih riječi,
  A/B screenshote, lokalizirani opis.
- **Distribucija** — r/croatia i Bug.hr coverage su jednokratni impulsi, ne plan.
  Ozbiljnije opcije za razmotriti: integracija u postojeću zajednicu, ciljano
  oglašavanje na startu. Bez jedne od tih, organski rast je spor — i to je u redu
  ako je ambicija (gore) postavljena iskreno.

### 15.2 Post-launch operativni model
- **GTFS format promjene** — snapshot testovi (sekcija 8) hvataju regresiju; verzioniran
  parser; abstrakcija feeda (R1) omogućuje brz popravak.
- **Recenzije** — odgovarati na Play/App Store recenzije (gradi povjerenje, vidi R7).
- **Crash SLA** — kritični crash → hotfix u danima; praćeno preko Crashlytics/Sentry.
- **GTFS static** — automatsko osvježavanje (sekcija 2) ne traži ručnu intervenciju.

---

## 16. Status odluka i sljedeći korak

**Potvrđene odluke (v5 — finalne):**
- D1 — backend: klijent ide kroz `transit-api` **od dana 1** (pass-through cache u A0).
- D2 — KMP: **da, zaključeno** — iOS je potvrđeno u opsegu 1.0; KMP shared sloj od dana 1.
- D3 — karta: **MapLibre** (Mapbox free tier ostaje legitimna alternativa; konačno u Fazi 0).
- Persona↔MVP: widget (A0.7) i rutine (A1.5) **u MVP**; primarna persona = dnevni putnik.
- **Spike apsorbiran u Fazu 0** (v6) — feed-validacija je prvi zadatak Faze 0, ne zasebna kapija.

**Otvoreno za odluku (ne blokira početak):**
- OTP vs GraphHopper za A2 transit routing — odlučiti prije A2 (RAM/trošak implikacija).
- Regija i provider backend hostinga — odlučiti u Fazi 0 (EU, sekcija 14).

**Faza 0 je u tijeku:**
1. Skeleton: Android KMP (`apps/zagreb-android` + `shared/`) i Ktor backend
   (`services/transit-api`) — postavlja se sada.
2. Feed-validacija (paralelno): 24 h polling RT feeda, pregled GTFS static ZIP-a,
   MapLibre fps test, procjena troška map tilesa.
3. Design proces (sekcija 6) i pokretanje beta-kohorte.
