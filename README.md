# Chess Puzzle - Android Aplikacija

<!-- BEGIN_DESCRIPTION -->## Opis Projekta

Ovaj projekat se sastoji od 3 modula, od kojih je razvijen samo modul 1.

Modul 1: Implementitra zagonetku koja je zasnovana samo na kretanju figura u šahu. Ostala šahovska pravila se ne implementiraju (šah, mat, rokada...).
Pred igrača se postavlja šahovska pozicija sa nekoliko crnih figura i jednom ili više belih figura (dama, skakač, top ili lovac). Na tabli nema kraljeva. Cilj je pojesti sve crne figure po sledećim pravilima: a) beli je uvek na potezu, crne figure ne izvode poteze b) svaki potez belom figurom je "jedenje" crne figure, nema "praznih" poteza c) pozicija je rešena ako na kraju ostanu samo bele figure na tabli
Implementiran je generator pozicija i solver. Dva moda igre:
a) trening mod (generator pozicije generiše pozicije na osnovu nivoa težine, solver proverava da li je pozicija rešiva i nudi rešenje ako to korisnik zahteva)
b) takmičarski mod (pozicije se učitavaju iz json fajla po nivoima težine koje korisnik izabere, pozicije su rešene)
U planu je i online mod.

Modul 2 (nije imolementiran): Isto kao u modulu 1, nema šahovskih pravila , samo kretanje figura. Razlika igre je u tome što bela figura (osim možda na početnoj poziciji) ne sme da odigra potez tako da bude napadnuta od strane neke crne figure. Dakle, jedina razlika u odnosu na modul 1 je što potez belom figurom mora da bude takav da ne stane na polje koje je branjeno nekom crnom figurom i što su dozvoljeni "prazni" potezi. Sve ostalo je isto što se tiče generatora pozicija i solvera i modova igre kao u modulu 1.

Modul 3 (nije implementiran): Nadogradnja je modula 2, s tom razlikom što sad nije cilj da se pojedu sve crne figure, već da se belom figurom dodje na ciljno polje na kojem se nalazi crni kralj (jedino u ovom modulu imam kralja, i to crnog na tabli). Cilj je pojesti kralja, ali bezbedno: ako ima crnih figura koje brane kralja,
potrebno je prvo pojesti te crne figure bezbedno, pa tek onda pojesti crnog kralja- Isto imamo generator pozicija i solver i dva moda, kao u prethodna dva modula

U nastavku je prikazana struktura projekta koji implementira modul 1<!-- END_DESCRIPTION -->

## Struktura Projekta
```text
├── .gradle/
│   ├── 8.14.2/
│   │   ├── checksums/
│   │   ├── executionHistory/
│   │   ├── expanded/
│   │   ├── fileChanges/
│   │   ├── fileHashes/
│   │   └── vcsMetadata/
│   ├── buildOutputCleanup/
│   ├── kotlin/
│   │   └── errors/
│   └── vcs-1/
├── .kotlin/
│   ├── errors/
│   └── sessions/
└── app/
    ├── sampledata/
    └── src/
        ├── androidTest/
        │   └── java/
        │       └── com/
        └── main/
            ├── assets/
            ├── java/
            │   └── com/
            └── res/
                ├── drawable/
                ├── layout/
                ├── mipmap-anydpi/
                ├── mipmap-anydpi-v26/
                ├── mipmap-hdpi/
                ├── mipmap-mdpi/
                ├── mipmap-xhdpi/
                ├── mipmap-xxhdpi/
                ├── mipmap-xxxhdpi/
                ├── raw/
                ├── values/
                └── xml/
```

## Detalji Klasa


### Paket: `com.chess.chesspuzzle`


#### Klasa: `ChessCore`

Vraća listu svih legalnih polja na koja se data figura može pomeriti
sa date početne pozicije na datoj tabli, u skladu sa osnovnim šahovskim pravilima.
NE uzima u obzir pravila specifična za zagonetku (poput "ne vraćanja na posećena polja")
niti je li potez hvatanje.

@param board Trenutna šahovska tabla.
@param piece Figura za koju se traže potezi.
@param fromSquare Početna pozicija figure.
@return Lista Square objekata koji predstavljaju legalne destinacije.


**Metode:**


#### Klasa: `PieceColor`


**Metode:**


#### Klasa: `ChessSolver`


**Metode:**


#### Klasa: `CompetitionPuzzleLoader`


**Metode:**


#### Klasa: `Difficulty`


**Metode:**


#### Klasa: `FigureSelectionActivity`


**Metode:**


#### Klasa: `GameActivity`


**Metode:**


#### Klasa: `HighScoresActivity`


**Metode:**


#### Klasa: `MainActivity`


**Metode:**


#### Klasa: `CreationMode`


**Metode:**


#### Klasa: `PositionCreationActivity`


**Metode:**


#### Klasa: `SaveFileOption`


**Metode:**


#### Klasa: `PuzzleDataHandler`

Učitava listu korisničkih šahovskih problema iz JSON fajla.
@param context Context aplikacije, potreban za pristup internom skladištu.
@param fileName Ime JSON fajla u internom skladištu (npr. "user_puzzles.json").
@return Lista ChessProblem objekata, ili prazna lista ako dođe do greške ili fajl ne postoji.


**Metode:**


#### Klasa: `PuzzleLoader`

Učitava listu šahovskih problema iz JSON fajla.
Novi format JSON-a sa detaljnim informacijama o zagonetki.

@param context Context aplikacije, potreban za pristup assets folderu.
@param fileName Ime JSON fajla u assets folderu (npr. "puzzles.json").
@return Lista ChessProblem objekata, ili prazna lista ako dođe do greške.


**Metode:**


#### Klasa: `ScoreManager`

Inicijalizuje ScoreManager. Ova metoda mora biti pozvana jednom na početku aplikacije (npr. u MainActivity).
@param context Context aplikacije.


**Metode:**


#### Klasa: `SolutionDisplayActivity`


**Metode:**


#### Klasa: `SoundManager`


**Metode:**


#### Klasa: `SquareAdapter`


**Metode:**


#### Klasa: `TrainingPuzzleManager`

Generiše nasumičnu laku zagonetku za trening mod.
Postavlja 1 belu figuru (od odabranih) i crne figure (targete) na putanji bele figure.


**Metode:**


### Paket: `com.chess.chesspuzzle.logic`


### Paket: `com.chess.chesspuzzle.ui.theme`


<!-- BEGIN_FEATURES -->## Ključne Funkcionalnosti

Dodajte liste funkcionalnosti ovde.<!-- END_FEATURES -->

<!-- BEGIN_INSTALLATION -->## Instalacija

Dodajte instrukcije za instalaciju ovde.<!-- END_INSTALLATION -->

<!-- BEGIN_LICENSE -->## Licenca

Dodajte informacije o licenci ovde.<!-- END_LICENSE -->