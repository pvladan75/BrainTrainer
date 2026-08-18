# Stanje projekta

Živi dokument — gde smo stali i šta sledi. Za opis projekta i komande vidi
[README.md](README.md).

**Poslednje ažuriranje:** 19.08.2026.
**Radna grana:** `uklanjanje-reklama-i-popravke` (odvojena od `master`)

---

## Gde smo stali

Poslednji korak u toku je **Crashlytics** i blokiran je — čeka se
`google-services.json`. Detalji niže, u sekciji „U toku".

Sve pre toga je završeno, build-ovano, testirano i push-ovano.

---

## Urađeno

Tri commit-a na grani `uklanjanje-reklama-i-popravke`:

### `ac70b4e` — Reklame, lokalizacija, premium, R8

**Lokalizacija.** Podrazumevani jezik je bio srpski, pa je svaki korisnik van
srpskog locale-a dobijao srpski interfejs. Engleski je premešten u `values/`,
srpski u `values-sr/`, dodat `locales_config.xml`.

**Premium i naplata.**
- `BillingClientManager` je premešten u novu `BrainTrainerApp` klasu i sada je
  app-scoped. Ranije je živeo u `SettingsViewModel`, pa su se kupovine
  proveravale tek kada korisnik otvori Podešavanja — refund nikada nije bio
  detektovan.
- Premium se oduzima **isključivo** kada Play uspešno odgovori da aktivne
  kupovine nema. Greška u komunikaciji (offline) ne dira status.
- DataStore `settings` je izuzet iz backup-a — restore na drugom uređaju je
  davao premium bez kupovine.

**Reklame uklonjene u potpunosti.** AdMob i UMP SDK, interstitial, rewarded
reklame za hint i dupli XP, dugme „Dupliraj XP", `AD_ID` dozvola, AdMob
`APPLICATION_ID`. Hint je posledično **besplatan za sve igrače**.

**R8.** Uključeni `isMinifyEnabled` i `isShrinkResources` uz keep pravila za
kotlinx.serialization, model klase i enume čija se imena čuvaju u DataStore-u.
Preduslov je bio da `getPieceDrawableResId` prestane da koristi
`resources.getIdentifier()`, koji R8 ne vidi.

**Popravke.** `MediaPlayer` se oslobađa po završetku (curilo na tri mesta).
`MainScreen` ponovo čita XP i rang na `ON_RESUME` — ranije je posle odigrane
sesije prikazivao zastareo rang i zaključane težine.

### `ac394f5` — Refaktor ChessScreen

`ChessScreen.kt`: **942 → 616 linija**.

- `ScoreCalculator` (novo) — čista računica bodova bez Android zavisnosti.
  Ranije je `checkGameStatus` istovremeno računao poene i lepio lokalizovani
  tekst, pa se bodovanje nije moglo testirati.
- `ChessScreenComponents` (novo) — tabla, info panel, kontrole, dijalozi.
- `LocalizedNames` (novo) — `moduleTitle` i `difficultyLabel` na jednom mestu;
  postojali su u tri kopije.

### `ada1c47` — Testovi za Board i FenParser

41 novi test (ukupno 50). Prvi pravi testovi u projektu — do tada je postojao
samo `assertEquals(4, 2 + 2)`.

Uz to: `FenParser.parseFenToBoard` je čitao `parts[1]` bez provere, pa bi FEN bez
polja za aktivnog igrača srušio aplikaciju. Polje je sada opciono.

### Efekat na veličinu

| | AAB |
|---|---|
| Pre svega | 19,5 MB |
| Posle R8 | 9,1 MB |
| Posle uklanjanja reklamnih SDK-ova | **5,4 MB** |

Najveći pojedinačni dobitak: R8 je uklonio 11.403 stavke iz
`androidx.compose.material.icons` — biblioteka nosi ceo Material icon set, a
aplikacija koristi tri ikonice.

---

## U toku: Crashlytics (blokirano)

**Zašto:** od kad je R8 uključen, stack trace-ovi iz produkcije su obfuskovani.
Bez Crashlytics-a i upload-a mapping fajla padovi su praktično nečitljivi.

**Urađeno:** stavke dodate u `gradle/libs.versions.toml` — neaktivne, build je
zelen. Verzije su provereno aktuelne (`dl.google.com/dl/android/maven2`):

| | |
|---|---|
| `com.google.gms.google-services` | 4.5.0 |
| `com.google.firebase.crashlytics` | 3.0.7 |
| `firebase-bom` | 34.17.0 |

**Blokada:** treba `google-services.json`. Firebase projekat se pravi pod
Google nalogom vlasnika.

1. [console.firebase.google.com](https://console.firebase.google.com) →
   Create a project
2. Add app → Android, package name tačno `com.program.braintrainer`
   (SHA-1 nije potreban za Crashlytics)
3. Download `google-services.json` → snimiti u **`app/google-services.json`**
4. U Firebase konzoli otvoriti Crashlytics → Enable

**Preostalo posle toga:**
- uključiti oba plugin-a i dodati `firebase-bom` + `firebase-crashlytics`
- uključiti automatski upload R8 mapping fajla za release
- isključiti slanje izveštaja iz debug build-a
- namerni test-pad radi provere da izveštaj stiže u konzolu
- **ažurirati Play Data Safety** — Crashlytics prikuplja crash logove i
  dijagnostiku; mora se prijaviti u Play Console → App content → Data safety

---

## Sledeći koraci

Poređano po vrednosti.

### 1. `ChessViewModel`

Preostalih 616 linija u `ChessScreen` je stvarna mašina stanja partije — 24
`remember` promenljive i pet `LaunchedEffect`-a. Izvlačenje u ViewModel donosi:

- preživljavanje process death (trenutno rotaciju spašava samo `configChanges`
  u manifestu)
- mesto gde nova monetizacija može da se zakači bez daljeg naduvavanja UI-ja
- mogućnost testiranja toka partije

Testovi za `Board` i `FenParser` su mreža ispod ovog refaktora — zato je
urađen tim redom.

### 2. Učitavanje zagonetki

`ProblemLoader` čita ceo fajl u `String` (do 4,2 MB), parsira do 5.800 objekata
i uradi `.shuffled()` nad celom listom — a `ChessScreen` zatim uradi **još
jedan** `.shuffled().take(10)`. Sve to za deset zagonetki.

Rešenje: minifikovati JSON u assets-u (trenutno je pretty-printed, oko dvostruko
veći nego što treba), preći na `Json.decodeFromStream`, i po mogućstvu indeks
umesto parsiranja svega. Ovo je sada najveći uzrok sporog starta partije.

### 3. Nova strategija monetizacije

Reklame su uklonjene, planira se novi model. Zatečeno stanje:

- Google Play Billing radi ispravno i proverava kupovine pri svakom pokretanju
- proizvod `premium_upgrade` je aktivan i daje **dupli XP** za rešenu zagonetku
- tekst u Podešavanjima opisuje baš to i ništa više

Pre nego što se monetizacija zakomplikuje, vredi rešiti da se premium status
čuva kao običan boolean u DataStore-u, bez serverske verifikacije.

---

## Poznata ograničenja i dug

Nije hitno, ali je zabeleženo da se ne bi ponovo otkrivalo.

**Šahovska pravila.** Nema promocije pešaka (pešak na 8. redu ostaje pešak — ima
test koji to dokumentuje), nema en passant-a ni rokade. Za postojeće module nije
relevantno.

**Performanse table.** `getLegalMoves` prolazi svih 64 polja i za svako zove
`isValidMove`, koji radi `applyMove` (kopira mapu) plus `isKingInCheck`. Sve na
glavnoj niti u `onSquareClick`.

**Solver.** `UniversalPuzzleSolver` je BFS bez ograničenja dubine i bez timeout-a,
sa `visitedStates` setom FEN stringova. Na teškoj poziciji može dugo da traje.

**Tajmer.** Ne pauzira kada aplikacija ode u pozadinu — vreme teče i korisnik
gubi vremenski bonus.

**`AchievementManager`.** Instancira se zasebno u svakom factory-ju i u
`ChessScreen`, pa je `_newlyUnlockedAchievementFlow` per-instanca i notifikacija
o otključanom dostignuću nikada ne stigne do ekrana koji sluša. Praktično mrtav
kod.

**Sitnice.** `Square` je data class sa `Char` + `Int`, pa se alocira objekat za
svako polje u svakoj petlji solvera. `Piece.kt` sadrži extension funkciju unutar
same klase (nedostupna) i `opposite()` koja vraća `Color` umesto `Piece`.
`Square.isValid()` uvek vraća `true`. `window.statusBarColor` u `Theme.kt` je
deprecated i ignorisan na Android 15+.

**Nema CI-ja.** Testovi se pokreću ručno.

---

## Istorija reklama

Reklame su uklonjene i **ne postoje u istoriji grane** — AdMob/UMP integracija je
živela samo u radnom stablu i nikada nije commit-ovana. `git revert` neće pomoći.
Ako se ikada vraćaju, pišu se iznova: UMP consent, gating na `canRequestAds()`,
`manifestPlaceholders` za AdMob app ID i razdvajanje test/produkcijskih jedinica.
