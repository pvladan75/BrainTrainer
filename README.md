# Chess BrainTrainer

Android aplikacija sa šahovskim zagonetkama, objavljena na Google Play-u
(`com.program.braintrainer`).

Nije šahovska partija — svaki modul je zagonetka sa sopstvenim pravilom, gde igra
samo beli, a crne figure se ne pomeraju.

| Modul | Naziv | Cilj | Pravilo |
|---|---|---|---|
| Module1 | Spavači | Pojesti sve crne figure | Svaki potez mora biti uzimanje |
| Module2 | Izbegavanje | Pojesti sve crne figure | Zabranjeno stati na polje koje crni napada |
| Module3 | Lov na kralja | Ukloniti crnog kralja | Kao Modul 2, cilj je kralj |

Svaki modul ima tri težine (EASY / MEDIUM / HARD). Napredak se meri XP poenima,
rangovima i dostignućima.

---

## Tehnologije

| | |
|---|---|
| Jezik | Kotlin 2.1.20 |
| UI | Jetpack Compose (BOM 2025.07.00), Material 3 |
| Navigacija | navigation-compose 2.9.3 |
| Build | AGP 8.10.1, Gradle 8.11.1 |
| SDK | compileSdk 36, targetSdk 36, minSdk 27 |
| Podaci | DataStore Preferences + SharedPreferences |
| Naplata | Google Play Billing 8.0.0 |
| Serijalizacija | kotlinx.serialization |

Trenutna verzija: `versionCode = 6`, `versionName = "6.0"`.

---

## Struktura koda

```
app/src/main/java/com/program/braintrainer/
├── BrainTrainerApp.kt          Application: drži SettingsManager i BillingClientManager
├── MainActivity.kt             jedina Activity, hostuje Compose
├── chess/
│   ├── model/                  Board, Square, Piece, Problem, enumi
│   │   └── data/               SettingsManager, ProblemLoader, BillingClientManager
│   ├── parser/FenParser.kt     FEN <-> Board, parsiranje poteza
│   └── solver/                 BFS solver za hintove
├── rules/                      PuzzleRules + tri implementacije po modulu
├── score/
│   ├── ScoreCalculator.kt      čista računica bodova (testirana)
│   └── ScoreManager.kt         trajno čuvanje XP-a i statistike
├── gamification/               dostignuća, rangovi, ViewModel-i
├── ui/
│   ├── AppNavigation.kt        rute
│   ├── LocalizedNames.kt       prevedeni nazivi modula i težina
│   ├── screens/                ekrani
│   └── theme/                  Compose tema
└── util/SoundPlayer.kt         zvučni efekti
```

Zagonetke su u `app/src/main/assets/` — devet JSONL fajlova (modul × težina),
jedna minifikovana zagonetka po redu, ukupno **18.335 zagonetki**, oko 5 MB.
`ProblemLoader` iz njih uzima deset nasumičnih zagonetki po partiji, u jednom
prolazu kroz fajl i bez parsiranja onoga što nije odabrano (`ProblemSampler`).

### Build varijante

Dva flavor-a po dimenziji `version`:

- **`internal`** — `BuildConfig.IS_TEST_BUILD = true`, premium je automatski uključen.
  Za testere.
- **`googlePlay`** — verzija za objavu.

Release build koristi R8 (`isMinifyEnabled` + `isShrinkResources`).

---

## Komande

```bash
./gradlew test
```

Iste testove (78 komada) vrti i GitHub Actions na svaki push i pull request —
`.github/workflows/build.yml`.

Upiti nad bazom odigranih zagonetki traže pravi uređaj i ne idu na CI:

```bash
./gradlew :app:connectedInternalDebugAndroidTest
```

```bash
./gradlew :app:assembleGooglePlayDebug
```

```bash
./gradlew :app:assembleGooglePlayRelease
```

```bash
./gradlew :app:bundleGooglePlayRelease
```

Instalacija na povezan uređaj:

```bash
adb install -r app/build/outputs/apk/googlePlay/debug/app-googlePlay-debug.apk
```

Izlazni fajlovi:

| Šta | Putanja |
|---|---|
| Debug APK | `app/build/outputs/apk/googlePlay/debug/` |
| Release APK | `app/build/outputs/apk/googlePlay/release/` |
| Release AAB (za Play) | `app/build/outputs/bundle/googlePlayRelease/` |
| R8 mapping | `app/build/outputs/mapping/googlePlayRelease/mapping.txt` |

---

## Potpisivanje

Release build traži `keystore.properties` u **korenu projekta**. Fajl je
gitignore-ovan i mora se napraviti ručno:

```
storeFile=C:/putanja/do/kljuca.jks
storePassword=...
keyAlias=...
keyPassword=...
```

U `storeFile` koristi `/` ili `\\` — jedna obrnuta crta je escape karakter u
`.properties` formatu.

Bez tog fajla sve prolazi do koraka pakovanja, koji pukne sa
`SigningConfig "release" is missing required property "storeFile"`.

**Nikada ne commit-uj** `keystore.properties`, `*.jks` ni `*.keystore` — svi su u
`.gitignore`.

### Instalacija preko verzije sa Play-a

Lokalno potpisan APK se ne može instalirati preko verzije preuzete sa Play-a
(različiti potpisi — Play App Signing potpisuje svojim ključem). Android javlja
`signatures do not match`. Potrebno je prvo deinstalirati aplikaciju, **što briše
XP, rangove i dostignuća** na tom uređaju. Za testiranje koristi drugi uređaj ili
emulator ako ti je stanje bitno.

---

## Lokalizacija

- `res/values/` — **engleski** (podrazumevani jezik)
- `res/values-sr/` — srpski
- `res/xml/locales_config.xml` — lista jezika za izbor iz sistemskih podešavanja
  (Android 13+)

Kada dodaješ string, dodaj ga u **oba** fajla — `lintVital` prijavljuje
`MissingTranslation` i obara release build.

---

## Testovi

50 testova, svi prolaze.

| Fajl | Šta pokriva |
|---|---|
| `chess/BoardTest.kt` | kretanje figura, blokade, pešak, vezivanje, napadnuta polja |
| `chess/FenParserTest.kt` | FEN u oba smera, parsiranje poteza |
| `score/ScoreCalculatorTest.kt` | računica XP-a, kazne, niz savršenih rešenja |

Ovo je čista JVM logika bez Android zavisnosti — `./gradlew test` je brz i ne
traži uređaj.

---

## Trenutno stanje i planovi

Vidi [STANJE.md](STANJE.md).
