# Stanje projekta

Živi dokument — gde smo stali i šta sledi. Za opis projekta i komande vidi
[README.md](README.md).

**Poslednje ažuriranje:** 19.08.2026.
**Radna grana:** `uklanjanje-reklama-i-popravke` (odvojena od `master`)

---

## Gde smo stali

`ChessViewModel` je završen i proveren na uređaju. Nema otvorenih blokada.

Poznata ograničenja i dug su prošireni; ostala su samo šahovska pravila
(promocija, en passant, rokada), koja za postojeće module nisu relevantna.

Model monetizacije je odlučen 19.8.2026 — svaka aplikacija za sebe, bez servera.
Sledeći korak je **nov sadržaj premiuma**, a prvi komad posla je lokalna baza
rezultata (sekcija „Sledeći koraci").

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

**Tada nije urađeno: preživljavanje process death.** ViewModel je preživljavao
promenu konfiguracije, ali `SavedStateHandle` nije bio uveden. Rešeno kasnije —
vidi „Raščišćen dug".

Provereno na uređaju kroz stvarnu igru: ulazak u modul, `Puzzle: 1/10`, tajmer,
hint, predaja sa ispravnim tekstom ishoda, prelazak na sledeću zagonetku
(`Puzzle: 2/10`, brojači resetovani) i reprodukcija rešenja koja se uredno
zaustavlja na kraju.

### Učitavanje zagonetki

`ProblemLoader` je čitao ceo fajl u `String` (do 4,2 MB), parsirao svih 5.800
objekata i mešao celu listu, pa je `ChessViewModel` mešao još jednom i uzimao
deset. Sada se učitava samo ono što se koristi.

- **Assets su prebačeni u JSONL** — jedna minifikovana zagonetka po redu, bez
  polja `baseScore` i `maxMovesAllowed` koja kod nigde ne čita. Fajlovi su
  preimenovani u `*_puzzles.jsonl`; 11,4 MB → 5,1 MB raspakovano.
- **`ProblemSampler`** radi rezervoarsko uzorkovanje: jedan prolaz kroz tok, u
  memoriji najviše deset redova, a JSON se parsira tek za odabrane. Umesto 5.800
  objekata parsira se deset.
- **`ProblemLoader.loadRandomProblems(module, difficulty, count)`** je zamenio
  `loadProblemsForModuleAndDifficulty`; `ChessViewModel` više ne meša ništa i
  samo prosleđuje `PUZZLES_PER_SESSION`.

Šest novih testova (`ProblemSamplerTest`) pokriva veličinu uzorka, slučaj kad
zagonetki ima manje nego što se traži, raspodelu kroz ceo fajl, preskakanje
praznih i pokvarenih redova, i ispravnost svih redova u jednom assets fajlu.

Merenje na JVM-u (`module1_hard`, 5.800 zagonetki): 163 ms → 58 ms na hladno,
34 ms → 4 ms kad je fajl u kešu. Na AAB se ovo skoro i ne vidi (**–0,08 MB**) —
razmaci iz pretty-print-a se ionako odlično kompresuju; dobitak je u parsiranju,
memoriji i prostoru na uređaju.

### Raščišćen dug

Pređeno redom po vrednosti.

**Dostignuća.** `AchievementManager` se pravio zasebno u tri factory-ja, pa je
`newlyUnlockedAchievementFlow` bio per-instanca — a nije ga niko ni slušao.
Sada je jedna instanca u `BrainTrainerApp`, `ChessViewModel` je sluša i emituje
`ChessUiEvent.AchievementUnlocked`, a ekran prikazuje snackbar sa prevedenim
nazivom. Prvi put da otključano dostignuće uopšte stigne do korisnika.

**Tajmer u pozadini.** Odbrojavao je i dok je aplikacija u pozadini, pa se gubio
vremenski bonus. Tiker sada radi samo kada zagonetka teče i ekran je u prvom
planu (`ON_START`/`ON_STOP`), bez promene u tome kada se tajmer logički pokreće.

**Performanse table.** `getLegalMoves` je za svaku figuru prolazio kroz svih 64
polja i za svako zvao `isValidMove` (kopira mapu figura, pa računa šah). Sada se
kandidati generišu iz pravila kretanja same figure — najviše 27. `isKingInCheck`
je gradio ceo skup napadnutih polja da bi proverio jedno; zamenjen je sa
`isSquareAttackedBy`, koji prekida na prvom napadaču, i isti poziv koriste
Module2 i Module3 pravila. `Square` interniše svih 64 polja umesto da alocira
novo pri svakom `fromCoordinates`.

Merenje na JVM-u (200 pozicija iz `module2_hard` i `module3_hard`, 20 prolaza):
177–194 ms → 68–70 ms, oko **2,6x**. Novo generisanje se u testovima poredi sa
starom logikom na stvarnim pozicijama iz assets-a, figuru po figuru.

**Solver.** BFS je dobio budžet: 3 s i 200.000 stanja. Dubina se namerno **ne**
ograničava — rešenja u assets-u idu do 38 poteza, pa bi svaki razuman limit
dubine odsekao teške zagonetke. Putanja se više ne kopira u svaki čvor, nego se
rekonstruiše preko roditelja.

**Process death.** U `SavedStateHandle` se čuva snapshot sesije: ID-jevi deset
zagonetki, indeks, niz tačnih, FEN zatečene table, vreme, potezi, greške i da li
je rešenje otkriveno. Piše se iz pretplate na `uiState`, pa ne može da odluta od
prikazanog stanja; na kraju sesije se briše. Zagonetke se pamte po ID-ju —
`ProblemLoader.loadProblemsByIds` parsira samo pogođene redove JSONL-a. Ako je
proces ubijen dok je stajao dijalog o ishodu, nastavlja se od sledeće zagonetke
jer je ta već obračunata.

Provereno na uređaju: potez, Home, `adb shell am kill com.program.braintrainer`,
povratak preko Recents-a — ista tabla, vreme i brojači. Povratak preko ikonice
umesto Recents-a ume da startuje nov task bez sačuvanog stanja, što nije do
aplikacije.

**Statusna traka.** `window.statusBarColor` je deprecated i na Android-u 15+ se
ignoriše, a aplikacija radi edge-to-edge. Ostalo je samo
`isAppearanceLightStatusBars`, sada ispravno okrenut — ranije su na Android-u
15+ ikonice bile bele na svetloj pozadini.

**Sitnice.** Obrisane su mrtva `Piece.getChar()` extension unutar same klase,
njen duplikat u `Board.kt` (ostaje `toFenChar`), `Piece.opposite()` koja vraća
`Color`, i `Square.isValid()` koja uvek vraća `true`.

**CI.** GitHub Actions workflow (`.github/workflows/build.yml`) vrti testove i
debug build na svaki push i pull request. Ne treba mu `keystore.properties` jer
se debug potpisuje debug ključem. Testova je sada **65**.

### Baza odigranih zagonetki

Prvi komad novog premiuma, i jedini koji poskupljuje ako se odloži: svaki dan
bez njega je dan odigranih zagonetki o kojima ne postoji nikakav trag. Ništa se
u UI-ju još ne vidi — zapisivanje kreće odmah, prikaz dolazi kasnije.

`ScoreManager` je i dalje tu i i dalje drži zbirne brojače u SharedPreferences;
nova baza ga ne zamenjuje nego beleži ono što on ne pamti — pojedinačan pokušaj.

- **Room + KSP**, jedna tabela `puzzle_attempts` (paket `stats/`). Šema se izvozi
  u `app/schemas` i **commit-uje**, pa svaka naredna verzija ima sa čim da uporedi
  migraciju.
- Modul, težina i ishod se čuvaju **kao tekst**, ne kao redni broj enum-a —
  dodavanje četvrtog modula ne sme da premesti značenje već upisanih redova. Red
  sa nepoznatom vrednošću se preskače umesto da sruši spisak.
- Zapisuje se **svaki** ishod: rešeno, rešeno uz rešenje, predato, bez poteza.
  Dnevnik grešaka živi baš od onih koje nisu rešene. Ponovno pokretanje iste
  zagonetke daje nov zapis — istorija je dnevnik pokušaja, ne stanje zagonetke.
- `AttemptRepository` vraća domenske modele, ne Room tipove, pa baza ne curi u UI.
- Upiti koji već postoje: skorašnji pokušaji, dnevni zbir (dan u **lokalnoj**
  zoni, ne UTC), otvorene greške i zbir po modulu i težini.

**Otvorena greška** je zagonetka čiji je *poslednji* pokušaj neuspešan: rešena pa
kasnije promašena je otvorena, promašena pa rešena nije. To je i najlakši upit da
se napiše naopako, pa je pokriven testom.

Testovi: četiri JVM testa za preslikavanje i „savršeno rešenje", i **pet
instrumentiranih** za same upite — Room proverava sintaksu pri prevođenju, ali ne
i da li upit znači ono što mislimo. Instrumentirani se ne vrte na CI-ju:

```bash
./gradlew :app:connectedInternalDebugAndroidTest
```

Prošli su na uređaju (SM-A137F) 19.8.2026.

### Dnevnik grešaka

Prva premium funkcija koja se vidi u aplikaciji. Ikonica u gornjoj traci glavnog
ekrana vodi na spisak zagonetki koje čekaju revanš, a dugme „Vežbaj" pokreće
sesiju sastavljenu baš od njih.

**Grupiše se po modulu i težini**, i to nije kozmetika: pravila su vezana za
modul, a bodovanje za težinu, pa jedna partija ne može da meša zagonetke iz
različitih grupa. Mešana sesija bi tražila da se pravila biraju po zagonetki
umesto po sesiji — moguće, ali to je drugi posao.

- `ChessViewModel` prima `puzzleIds`; prazno znači obična nasumična sesija,
  popunjeno znači revanš nad tačno tim zagonetkama (`loadProblemsByIds`).
- Ruta igre je dobila upitni deo `?puzzleIds=`, sa podrazumevanom praznom
  vrednošću — bez nje ruta bez upitnog dela ne bi bila pogođena.
- Kraj partije sada radi običan `popBackStack()` umesto povratka na glavni meni.
  Time se iz revanša vraća u dnevnik, koji se na `ON_RESUME` osvežava, pa ne nudi
  zagonetke koje su upravo rešene.
- Bez premiuma se vidi objašnjenje i dugme ka Podešavanjima, gde kupovina i
  živi. **Taj ekran nije viđen na uređaju** — `internal` flavor uvek ima premium.

Provereno na uređaju kroz stvarnu igru 19.8.2026: predata zagonetka se pojavila
u dnevniku kao „Sleepers / Easy — 1 puzzle", a „Vežbaj" je otvorio sesiju
`Puzzle: 1/1` sa baš tom zagonetkom.

Testovi: četiri JVM testa za grupisanje (isti modul i težina u istu grupu,
najskorija grupa prva, prazan spisak).

### Istorija napretka

Druga premium funkcija. Ulaz je dugme na ekranu Profila; ekran ima tri dela:
zbir (odigrano, rešeno, ukupno vreme), stubići aktivnosti za poslednjih 14 dana,
zbir po modulu i težini, i spisak poslednjih 30 zagonetki.

- Zbir za svih devet kombinacija stiže **jednim** upitom (`summaries()`), a ne
  sa devet prolaza kroz tabelu.
- **Dani bez vežbanja se popunjavaju nulama** (`fillMissingDays`), da grafik ne
  sabije nedelju dana pauze u jedan razmak; prazan dan ostaje vidljiv kao tanka
  crta u dnu.
- Grafik je namerno **bez brojeva na osi**. Odgovara na pitanje „da li vežbam
  redovno", ne na „koliko tačno" — za to služi zbir iznad njega.
- Stubić je ceo odigrano, puni deo rešeno.
- Trajanje se formatira preko `durationParts`, odvojeno od ekrana, pa granice
  (59 s, 60 s, 3599 s, 3600 s) imaju test.

Zaključavanje bez premiuma je isto kao u dnevniku grešaka, sa zajedničkim
stringovima `premium_locked_*`.

Viđeno na uređaju 19.8.2026 sa stvarnim podacima: zbir, stubići, grupa
„Sleepers / Easy — 0 od 3 rešeno" i spisak poslednjih pokušaja sa vremenom i
datumom. Kad nijedna nije rešena, red sa najboljim vremenom se ne prikazuje.

**Usput naučeno:** `connectedInternalDebugAndroidTest` **deinstalira aplikaciju**
kad završi, pa briše XP, dostignuća i bazu. Tako je nestao napredak sa test
telefona, a sa njim i otključani moduli — Avoidance i King Hunt traže rang iznad
početnog. Upozorenje je dodato u README uz samu komandu.

Testovi: pet JVM testova (popunjavanje dana, opseg, granice trajanja) i jedan
instrumentirani za `summaries()`.

### Trening po meri

Poslednji komad novog premiuma. Ulaz je dugme ispod kartica modula na glavnom
ekranu; bira se modul, težina, dužina sesije (5, 10 ili 20) i da li se vidi sat.

**Sat se sakriva, ne isključuje.** Vreme i dalje teče i i dalje ulazi u bodove —
skriven sat sklanja pritisak, ne bodove. Da se tajmer stvarno zaustavi, brzinski
bonus bi bio maksimalan u svakoj partiji, pa bi „trening bez tajmera" postao
najisplativiji način da se skuplja XP. To piše i na samom ekranu, ispod
prekidača.

`ChessViewModel` je dobio `sessionSize` i `hideTimer`; ruta partije nosi oba kao
upitne parametre, pa ih obnavljanje sesije posle ubijanja procesa dobija besplatno.

**Test build otključava sve module.** `RankManager.getAvailableDifficultiesFor`
u `IS_TEST_BUILD` verziji vraća sve težine — tester (ili autor) treba da proba
svaki modul, a ne da ga prvo zaradi. Ovo je i praktična posledica brisanja
napretka opisanog kod istorije.

Provereno na uređaju 19.8.2026: Avoidance/Medium se otvara sa nula XP-a, a
trening „King Hunt, Hard, 5 zagonetki, sakriven sat" daje partiju sa
`Puzzle: 1/5` i **bez** reda sa vremenom.

Testovi: četiri JVM testa za rutu partije (obična sesija, revanš sa spiskom
ID-jeva, trening sa dužinom i skrivenim satom, i da šablon pokriva sve parametre).

### Efekat na veličinu

| | AAB |
|---|---|
| Pre svega | 19,5 MB |
| Posle R8 | 9,1 MB |
| Posle uklanjanja reklamnih SDK-ova | **5,4 MB** |
| Sada (Crashlytics + minifikovane zagonetke) | 5,94 MB |

Najveći pojedinačni dobitak: R8 je uklonio 11.403 stavke iz
`androidx.compose.material.icons` — biblioteka nosi ceo Material icon set, a
aplikacija koristi tri ikonice.

---

## Sledeći koraci

Poređano po vrednosti.

### 1. Nov sadržaj premiuma

Model je odlučen **19.8.2026.** Ovo više nije otvoreno pitanje, nego posao.

**Svaka aplikacija naplaćuje za sebe** („model A"). Kupovina na Google Play-u
živi na paru „Google nalog + paket aplikacije" i ne može da pređe u drugu
aplikaciju bez naloga i servera. Pošto BrainTrainer ne prodaje ništa što se
izvršava na serveru, server mu **ne treba**: `queryPurchasesAsync` pri svakom
pokretanju već vraća kupovinu na novom telefonu i hvata refund. Boolean u
DataStore-u je keš te provere, a ne rupa — jedini scenario koji bi server
pokrio je modifikovan APK, što za dupli XP ne plaća uvođenje prijave.

Druge dve aplikacije (`chess_master`, `BlindfoldTrainer`) idu na zajednički
DigitalOcean droplet sa odvojenim tabelama po aplikaciji; tamo je premium po
prirodi serverski jer se prodaje baš usluga (čuvanje napretka, dnevni zadatak,
lestvica). BrainTrainer u tome ne učestvuje.

**Dupli XP izlazi iz premiuma.** Po pravilu koje već stoji u planu
BlindfoldTrainer-a: merilo koje nagrađuje prestaje da meri. XP kaže koliko je
neko odigrao; udvostručen za pare, više ne kaže ništa, a rangovi koji iz njega
izlaze postaju priča o tome ko je platio. Uz to je i slaba ponuda — kupcu ne
daje ništa novo da radi.

**Novi obim premiuma**, po vrednosti:

| | Zašto |
|---|---|
| Istorija i grafici napretka po modulu i težini | najkorisnija stvar koju igrač može da dobije, a besplatnoj verziji ne fali |
| Dnevnik grešaka — ponovo odigraj baš promašene zagonetke | jeftino otkad su zagonetke adresabilne po ID-ju |
| Trening po meri — sastav sesije, dužina, bez tajmera | menja kako se vežba, ne koliko |

Igranje ostaje neograničeno i besplatno; nijedan modul, težina ni zagonetka se
ne zaključava. Podela je ista kao u BlindfoldTrainer-u, pa tri aplikacije
govore istim jezikom: **alat je besplatan, plaća se uvid u sopstveni rad.**

Sve tri stvari iz novog premiuma su **urađene 19.8.2026** (vidi Urađeno).
Ostaje jedan zaseban posao: **gde se i kada premium uopšte ponudi** — danas se
pominje samo jednom rečenicom u Podešavanjima, a tri nove funkcije nude
zaključan ekran bez ijedne reči o ceni.

**Kupaca nema — provereno 19.8.2026.** Upravljanje porudžbinama pokazuje
**jednu jedinu** porudžbinu `premium_upgrade`-a, od 24.7.2025, autorovu
sopstvenu proveru da naplata radi, i to refundiranu. Dakle proizvod stoji
dostupan više od godinu dana i nije se prodao nijednom čoveku.

Posledica za plan: obaveza prema postojećim kupcima je **prazna**, pa se šta
`premium_upgrade` znači menja bez ikakvog duga prema ranijim kupcima. Princip
ostaje ako se neko pojavi pre nego što novi premium izađe — ista šifra
proizvoda, novo značenje, bez doplate.

Nula prodaja **ne dokazuje** sama po sebi da je ponuda loša; tri uzroka se
preklapaju: malo korisnika, dupli XP kao slaba ponuda, i to što se premium u
aplikaciji nigde ne nudi osim jednom rečenicom u Podešavanjima. Prva dva rešava
novi sadržaj premiuma; **treće je zaseban posao** — gde se i kada ponuda uopšte
pokaže.

**Dnevna zagonetka ne traži server** ako se ikad poželi: datum kao seme, seme
bira redni broj u JSONL fajlu, i svi dobiju istu zagonetku istog dana. Server bi
trebao samo za lestvicu, koja ovde nije potrebna.

**Play Integrity API** (Play Console → Zaštićeno pomoću Play-a) stoji na 0/7 i
namerno je tako ostavljen. Verdikt mora da se verifikuje na serveru — ako se
proverava u samoj aplikaciji, napadač koji je već modifikovao APK preskoči i tu
proveru. Bez backend-a daje privid zaštite. Realne scenarije (refund, deljenje
backup-a, promena naloga) već pokriva `queryPurchasesAsync` pri svakom
pokretanju. Vredi ga uvesti tek ako novi model donese serversku stranu.

### Kako se ovo testira pre objave

**Testeri se ne angažuju** (odluka 19.8.2026) — proveru radi autor sam. To ne
menja *kako* se testira naplata, samo ko to radi: license testing i dalje treba,
jer je jedini način da se kupovina prođe kroz pravi tok bez naplate.

Dve stvari se lako pomešaju, a nisu isto:

| | čemu služi |
|---|---|
| **internal testing traka** | build stiže preko Play-a i sa produkcijskim potpisom — bez toga se naplata ne ponaša kao u produkciji |
| **License testing** (Play Console → Setup) | ti isti nalozi kupuju `premium_upgrade` kroz **pravi** tok, bez naplate |

**Provera se ne sme raditi na buildu koji premium pali zastavicom.** Tada
jedina stvar koja zaista može da pukne — sam tok kupovine — ostaje neproverena, a
premium funkcije se gledaju u stanju u kom nijedan stvarni korisnik nikada nije.

Flavor `internal` (`IS_TEST_BUILD = true`) pali premium **i otključava sve module
bez obzira na rang**, i ostaje **samo za autorov uređaj preko `adb install`**. Ne
sme na Play traku: Play Billing traži da je aplikacija instalirana sa Play-a i
potpisana istim ključem, pa sideload sa debug ključem ionako ne ponaša se kao
produkcija. Za proveru naplate ide `googlePlay` build sa trake, uz nalog upisan
u License testing.

Spisak koji mora da prođe:

1. **Ažuriranje preko postojeće produkcijske instalacije**, ne čista instalacija
   — XP, dostignuća i premium status preživljavaju.
2. **Kupac od ranije** ostaje premium i dobija nove funkcije (grandfathering).
3. **Nov besplatan korisnik** — kupovina otključava bez restarta aplikacije.
4. **Refund/povlačenje kupovine** u Play Console-u — premium nestaje pri sledećem
   pokretanju.
5. **Bez interneta** — premium se ne gubi kada Play nije dostupan.

Refund se **ne može proveriti na autorovom telefonu**, jer tamo stoji `internal`
flavor koji pali premium bez obzira na kupovinu. Za tačku 4 treba `googlePlay`
build sa Play trake.

**„Ograničenja korišćenja ponude"** u zaštiti Play naplate je isključeno i ne
može da se uključi — odnosi se na promotivne ponude za pretplatu, a aplikacija
ima samo jednokratni `premium_upgrade` (`ProductType.INAPP`). Postaje relevantno
ako se uvede pretplata sa probnim periodom.

---

## Poznata ograničenja i dug

Nije hitno, ali je zabeleženo da se ne bi ponovo otkrivalo.

**Šahovska pravila.** Nema promocije pešaka (pešak na 8. redu ostaje pešak — ima
test koji to dokumentuje), nema en passant-a ni rokade. Za postojeće module nije
relevantno, a uvođenje promocije bi promenilo značenje postojećih zagonetki i
njihovih rešenja u assets-u.

---

## Istorija reklama

Reklame su uklonjene i **ne postoje u istoriji grane** — AdMob/UMP integracija je
živela samo u radnom stablu i nikada nije commit-ovana. `git revert` neće pomoći.
Ako se ikada vraćaju, pišu se iznova: UMP consent, gating na `canRequestAds()`,
`manifestPlaceholders` za AdMob app ID i razdvajanje test/produkcijskih jedinica.
