# Stanje projekta

Živi dokument — gde smo stali i šta sledi. Za opis projekta i komande vidi
[README.md](README.md).

**Poslednje ažuriranje:** 19.08.2026.
**Radna grana:** `uklanjanje-reklama-i-popravke` (odvojena od `master`)

---

## Gde smo stali

`ChessViewModel` je završen i proveren na uređaju. Nema otvorenih blokada.

Sledeći korak po planu: **učitavanje zagonetki** (sekcija „Sledeći koraci").

Jedno preostalo zaduženje van koda: **ažurirati Play Data Safety** pre sledećeg
objavljivanja — Crashlytics prikuplja crash logove i dijagnostiku, što se mora
prijaviti u Play Console → App content → Data safety.

---

## Urađeno

Hronološki, po commit-ima na grani `uklanjanje-reklama-i-popravke`:

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

### `2e3aac9` — Dokumentacija

`README.md` i `STANJE.md`. Projekat do tada nije imao nikakvu dokumentaciju.

### Crashlytics

**Zašto:** od kad je R8 uključen, stack trace-ovi iz produkcije su obfuskovani.
Bez Crashlytics-a i upload-a mapping fajla padovi su nečitljivi.

Firebase projekat: **`braintrainer-8eda3`**.
Konfiguracija je u `app/google-services.json` i **commit-ovana je** — sadrži
client config i API ključ ograničen imenom paketa i potpisom, nije tajna. Bez nje
projekat ne može da se build-uje na čistom klonu.

Verzije, provereno aktuelne na `dl.google.com/dl/android/maven2`:

| | |
|---|---|
| `com.google.gms.google-services` | 4.5.0 |
| `com.google.firebase.crashlytics` | 3.0.7 |
| `firebase-bom` | 34.17.0 |

Podešeno:
- upload R8 mapping fajla uključen za release, isključen za debug
  (`mappingFileUploadEnabled`)
- slanje izveštaja isključeno u debug build-u
  (`setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)` u `BrainTrainerApp`),
  da test padovi ne zagađuju konzolu

Provereno na uređaju: namerni pad je poslat, logcat pokazuje
`Initializing Firebase Crashlytics 20.1.0` i zahtev ka
`crashlyticsreports-pa.googleapis.com`. Privremene izmene za taj test su
vraćene i nisu commit-ovane. Release build pokreće task
`uploadCrashlyticsMappingFileGooglePlayRelease`.

**Napomena za testiranje:** pošto je slanje isključeno u debug-u, pad iz debug
build-a se **neće** pojaviti u konzoli. Za ponovnu proveru treba privremeno
postaviti `setCrashlyticsCollectionEnabled(true)`.

### `ChessViewModel`

`ChessScreen` je bio composable od 616 linija sa 24 `remember` promenljive i pet
`LaunchedEffect`-a. Sve stanje partije je izvučeno u ViewModel.

Novi paket `ui/screens/chess/`:

| Fajl | Linija | Uloga |
|---|---|---|
| `ChessViewModel.kt` | 512 | ceo tok sesije |
| `ChessScreen.kt` | 322 | prikaz, bez stanja |
| `ChessScreenComponents.kt` | 306 | tabla, paneli, dijalozi |
| `ChessUiState.kt` | 96 | stanje, ishodi, događaji |
| `ChessViewModelFactory.kt` | 35 | |

Ključne odluke:

- **ViewModel ne drži Context.** Poruke se emituju kao `PuzzleOutcome` i
  `ChessUiEvent` (uključujući zvuk), a UI ih prevodi preko `stringResource`.
  Ranije je `checkGameStatus` sastavljao gotov lokalizovan tekst.
- **Tajmer, hint i reprodukcija rešenja su `Job`-ovi**, ne `LaunchedEffect`-i.
  Otkazuju se eksplicitno i u `onCleared()`.
- `ChessScreenContent` je odvojen od `ChessScreen` — prima samo stanje i
  `ChessActions`, pa se može pregledati u `@Preview`-u bez ViewModel-a.
- Komponente više ne primaju `Problem` ni `List<Problem>`, nego `sessionSize` i
  `solutionMoveCount`.

**Nije urađeno: preživljavanje process death.** ViewModel preživljava promenu
konfiguracije, ali `SavedStateHandle` nije uveden — posle ubijanja procesa
sesija kreće ispočetka. Ranija verzija ovog dokumenta je to navodila kao dobitak
ovog koraka, što nije tačno; za to bi trebalo čuvati ID-jeve zagonetki u sesiji,
FEN table i brojače.

Provereno na uređaju kroz stvarnu igru: ulazak u modul, `Puzzle: 1/10`, tajmer,
hint, predaja sa ispravnim tekstom ishoda, prelazak na sledeću zagonetku
(`Puzzle: 2/10`, brojači resetovani) i reprodukcija rešenja koja se uredno
zaustavlja na kraju.

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

## Sledeći koraci

Poređano po vrednosti.

### 1. Učitavanje zagonetki

`ProblemLoader` čita ceo fajl u `String` (do 4,2 MB), parsira do 5.800 objekata
i uradi `.shuffled()` nad celom listom — a `ChessViewModel` zatim uradi **još
jedan** `.shuffled().take(10)`. Sve to za deset zagonetki.

Rešenje: minifikovati JSON u assets-u (trenutno je pretty-printed, oko dvostruko
veći nego što treba), preći na `Json.decodeFromStream`, i po mogućstvu indeks
umesto parsiranja svega. Ovo je sada najveći uzrok sporog starta partije.

### 2. Nova strategija monetizacije

Reklame su uklonjene, planira se novi model. Zatečeno stanje:

- Google Play Billing radi ispravno i proverava kupovine pri svakom pokretanju
- proizvod `premium_upgrade` je aktivan i daje **dupli XP** za rešenu zagonetku
- tekst u Podešavanjima opisuje baš to i ništa više

Pre nego što se monetizacija zakomplikuje, vredi rešiti da se premium status
čuva kao običan boolean u DataStore-u, bez serverske verifikacije.

**Play Integrity API** (Play Console → Zaštićeno pomoću Play-a) stoji na 0/7 i
namerno je tako ostavljen. Verdikt mora da se verifikuje na serveru — ako se
proverava u samoj aplikaciji, napadač koji je već modifikovao APK preskoči i tu
proveru. Bez backend-a daje privid zaštite. Realne scenarije (refund, deljenje
backup-a, promena naloga) već pokriva `queryPurchasesAsync` pri svakom
pokretanju. Vredi ga uvesti tek ako novi model donese serversku stranu.

**„Ograničenja korišćenja ponude"** u zaštiti Play naplate je isključeno i ne
može da se uključi — odnosi se na promotivne ponude za pretplatu, a aplikacija
ima samo jednokratni `premium_upgrade` (`ProductType.INAPP`). Postaje relevantno
ako se uvede pretplata sa probnim periodom.

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
