# zagreb-ios

iOS UI za ZET transit aplikaciju — **gradi se u Fazi C** (sekcija 13 plana).

Ova mapa postoji od početka namjerno: iOS je potvrđeno u opsegu 1.0 (sekcija 4.3 plana —
odluka D2), pa ne smije biti skrivena pretpostavka koja se otkriva tek u Fazi C.

## Arhitektura

- **Shared kod** (domain + data — GTFS parser, modeli, use-caseovi, repozitoriji,
  mrežni sloj) dolazi iz KMP `:shared` modula u
  [`apps/zagreb-android/shared`](../zagreb-android/shared). KMP daje ~60–70% iOS koda
  besplatno.
- **UI sloj** je native SwiftUI — ne dijeli se s Androidom.
- `:shared` se kompajlira u iOS framework `Shared` (`isStatic = true`,
  vidi `shared/build.gradle.kts`).

## Stanje

Placeholder. Xcode projekt, SwiftUI ekrani i integracija `Shared` frameworka
kreiraju se u Fazi C, nakon što je Android (A0–A2) validiran.

Klijent gađa isključivo `transit-api` backend, nikad ZET izravno (sekcija 5 plana).
