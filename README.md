# Chess Puzzle - Android Aplikacija
Opis: 
## Struktura Projekta


### Paket: `com.chess.chesspuzzle`


#### ExampleUnitTest.kt

Putanja: `app\src\test\java\com\chess\chesspuzzle\ExampleUnitTest.kt`


**Klase:**

- `ExampleUnitTest`

**Funkcije:**

- `addition_isCorrect()`

#### ChessBoardComposable.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\ChessBoardComposable.kt`


**Funkcije:**

- `getPieceDrawable(PieceType: pieceType:, PieceColor: pieceColor:)`: Int

#### ChessCore.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\ChessCore.kt`


**Klase:**

- `ChessCore`
  - Vraća listu svih legalnih polja na koja se data figura može pomeriti
sa date početne pozicije na datoj tabli, u skladu sa osnovnim šahovskim pravilima.
NE uzima u obzir pravila specifična za zagonetku (poput "ne vraćanja na posećena polja")
niti je li potez hvatanje.

@param board Trenutna šahovska tabla.
@param piece Figura za koju se traže potezi.
@param fromSquare Početna pozicija figure.
@return Lista Square objekata koji predstavljaju legalne destinacije.

**Funkcije:**

- `getValidMoves(ChessBoard: board:, Piece: piece:, Square: fromSquare:)`: List<Square>
  - Vraća listu svih legalnih polja na koja se data figura može pomeriti
sa date početne pozicije na datoj tabli, u skladu sa osnovnim šahovskim pravilima.
NE uzima u obzir pravila specifična za zagonetku (poput "ne vraćanja na posećena polja")
niti je li potez hvatanje.

@param board Trenutna šahovska tabla.
@param piece Figura za koju se traže potezi.
@param fromSquare Početna pozicija figure.
@return Lista Square objekata koji predstavljaju legalne destinacije.

- `getSquaresBetween(Square: from:, Square: to:)`: List<Square>
  - Vraća listu svih polja koja se nalaze između dve date kocke.
Koristi se za klizne figure (top, lovac, kraljica).

@param from Početno polje.
@param to Krajnje polje.
@return Lista Square objekata koji predstavljaju međupolja.
Prazna lista ako nema međupolja (npr. za poteze skakača, ili susedna polja).

- `isPathClear(ChessBoard: board:, Square: fromSquare:, Square: toSquare:, PieceType: pieceType:)`: Boolean
  - Proverava da li je putanja između fromSquare i toSquare čista za datu vrstu figure.
Ovo je ključno za klizajuće figure (top, lovac, kraljica) jer ne mogu da preskaču.
Vitezovi, kraljevi i pešaci (za hvatanje) uvek imaju "čistu putanju" u ovom kontekstu.

@param board Trenutna tabla.
@param fromSquare Početno polje figure.
@param toSquare Krajnje polje figure.
@param pieceType Tip figure (npr. PieceType.ROOK, PieceType.BISHOP, PieceType.QUEEN).
@return True ako je putanja čista, False inače.

- `findCaptureTargetSquares(ChessBoard: board:, Piece: piece:, Square: startSquare:, Int: numCaptures:, mete: crne)`: Set<Square>?
  - Traži `numCaptures` jedinstvenih Praznih Polja koja data bela figura može da uhvati.
Putanja figure ne sme da se vraća na prethodno posećena polja (uključujući tranzitna).
Ova funkcija ne postavlja figure, samo pronalazi KANDIDATE za ciljna polja.

@param board Trenutna šahovska tabla (sa postavljenim belim figurama).
@param piece Figura za koju se traže putanje.
@param startSquare Početna pozicija figure.
@param numCaptures Željeni broj hvatanja (ciljnih polja).
@param globalOccupiedAndTargetSquares Skup SVIH polja koja su već zauzeta (bele figure)
ili su već PREDVIĐENA kao mete za druge bele figure.
Ova polja NE SMEJU biti izabrana kao nove mete, niti se figura
sme kretati kroz njih ako je klizna figura.
@return Skup Square objekata koji predstavljaju predložena ciljna polja. Vraća null ako se ne pronađe dovoljno meta.


#### ChessDefinitions.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\ChessDefinitions.kt`


**Klase:**

- `PieceColor`

**Funkcije:**

- `fromChar(Char: char:)`: PieceType
- `toString()`: String
- `fromCoordinates(Int: fileIndex:, Int: rankIndex:)`: Square
- `fromChar(Char: char:)`: Piece
- `toFenChar()`: Char
- `getPiece(Square: square:)`: Piece
- `setPiece(Piece: piece:, Square: square:)`: ChessBoard
  - // <--- OVDE JE PROMENJENO: placePiece je vraćen u setPiece

- `removePiece(Square: square:)`: ChessBoard
- `copy()`: ChessBoard
- `makeMoveAndCapture(Square: from:, Square: to:)`: ChessBoard
- `createEmpty()`: ChessBoard
- `parseFenToBoard(String: fen:)`: ChessBoard
- `createStandardBoard()`: ChessBoard
- `getPiecesMapFromBoard(null: =)`: Map<Square, Piece>
- `toFEN()`: String
- `printBoard()`
- `compareTo(ScoreEntry: other:)`: Int

#### ChessProblem.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\ChessProblem.kt`


#### ChessSolver.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\ChessSolver.kt`


**Klase:**

- `ChessSolver`

**Funkcije:**

- `toString()`: String
- `solve(ChessBoard: initialBoard:)`: List<MoveData>?
  - // Glavni ulaz za solver - sada vraća List<MoveData>?

- `solveRecursive(ChessBoard: currentBoard:, MutableList<MoveData>: path:, Int: targetCaptures:, MutableSet<String>: visitedStates:)`: List<MoveData>?
- `generateValidEatingMoves(ChessBoard: board:, Square: pieceSquare:, Piece: piece:)`: List<Pair<Square, Square>>

#### CompetitionPuzzleLoader.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\CompetitionPuzzleLoader.kt`


**Klase:**

- `CompetitionPuzzleLoader`

**Funkcije:**

- `loadAllPuzzlesIfNecessary(Context: context:)`
- `loadEasyPuzzleFromJson(Context: context:)`: ChessBoard
  - Učitava zagonetku iz JSON fajla za takmičarski mod (Lako).
Filtrira zagonetke po "Lako" težini i solutionLength od 5 do 7.

- `loadMediumPuzzleFromJson(Context: context:)`: ChessBoard
  - Učitava zagonetku iz JSON fajla za takmičarski mod (Srednje).
Filtrira zagonetke po "Srednje" težini i solutionLength od 8 do 10.

- `loadHardPuzzleFromJson(Context: context:)`: ChessBoard
  - Učitava zagonetku iz JSON fajla za takmičarski mod (Teško).
Filtrira zagonetke po "Teško" težini i solutionLength > 10.


#### FigureSelectionActivity.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\FigureSelectionActivity.kt`


**Klase:**

- `Difficulty`
- `FigureSelectionActivity`

**Funkcije:**

- `onCreate(Bundle?: savedInstanceState:)`
- `onDestroy()`
- `FigureSelectionScreen(String: playerName:)`
- `FigureSelectionPreview()`

#### GameActivity.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\GameActivity.kt`


**Klase:**

- `GameActivity`

**Funkcije:**

- `onCreate(Bundle?: savedInstanceState:)`
- `onDestroy()`
- `ChessGameScreen(Difficulty: difficulty:, List<PieceType>: selectedFigures:, Int: minPawns:, Int: maxPawns:, String: playerName:, Boolean: isTrainingMode:, Context?: applicationContext:)`
- `DefaultPreview()`

#### HighScoresActivity.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\HighScoresActivity.kt`


**Klase:**

- `HighScoresActivity`

**Funkcije:**

- `onCreate(Bundle?: savedInstanceState:)`
- `HighScoresScreen()`
- `ScoreItem(ScoreEntry: scoreEntry:, Int: rank:)`

#### MainActivity.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\MainActivity.kt`


**Klase:**

- `MainActivity`

**Funkcije:**

- `onCreate(Bundle?: savedInstanceState:)`
- `onDestroy()`
- `MainMenu(String: playerName:)`
- `MainMenuPreview()`

#### PositionCreationActivity.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\PositionCreationActivity.kt`


**Klase:**

- `CreationMode`
- `PositionCreationActivity`

**Funkcije:**

- `onCreate(Bundle?: savedInstanceState:)`
- `PositionCreationScreen(String: playerName:)`

#### PositionGenerator.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\PositionGenerator.kt`


**Funkcije:**

- `generate(Map<PieceType: whitePiecesConfig:, Int>: Any, Int: numBlackPieces:, 1000: =)`: String?
- `distributeCaptures(Int: totalCaptures:, Int: numberOfWhitePieces:)`: List<Int>

#### PuzzleLoader.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\PuzzleLoader.kt`


**Klase:**

- `PuzzleLoader`
  - Učitava listu šahovskih problema iz JSON fajla.
Novi format JSON-a sa detaljnim informacijama o zagonetki.

@param context Context aplikacije, potreban za pristup assets folderu.
@param fileName Ime JSON fajla u assets folderu (npr. "puzzles.json").
@return Lista ChessProblem objekata, ili prazna lista ako dođe do greške.

**Funkcije:**

- `loadPuzzlesFromJson(Context: context:, String: fileName:)`: List<ChessProblem>
  - Učitava listu šahovskih problema iz JSON fajla.
Novi format JSON-a sa detaljnim informacijama o zagonetki.

@param context Context aplikacije, potreban za pristup assets folderu.
@param fileName Ime JSON fajla u assets folderu (npr. "puzzles.json").
@return Lista ChessProblem objekata, ili prazna lista ako dođe do greške.

- `parseUciToSquares(String: uci:)`: Pair<Square, Square>?
  - Pomoćna funkcija za parsiranje UCI stringa "fromSquaretoSquare" u Pair<Square, Square>.
Npr. "e2e4" -> Pair(Square('e',2), Square('e',4))


#### ScoreManager.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\ScoreManager.kt`


**Klase:**

- `ScoreManager`
  - Inicijalizuje ScoreManager. Ova metoda mora biti pozvana jednom na početku aplikacije (npr. u MainActivity).
@param context Context aplikacije.

**Funkcije:**

- `init(Context: context:)`
  - Singleton objekat za upravljanje visokim skorovima (high scores) koristeći SharedPreferences.
Omogućava dodavanje, dohvatanje i brisanje rezultata za različite nivoe težine.
object ScoreManager {

    private const val PREFS_NAME = "ChessPuzzleScores" // Naziv SharedPreferences fajla
    const val MAX_HIGH_SCORES = 10 // Maksimalan broj rezultata koji se čuva za svaku težinu (javno dostupan)

    private lateinit var sharedPreferences: SharedPreferences // Inicijalizuje se u init metodi
    private val gson = Gson() // Gson instanca za JSON konverzije
Inicijalizuje ScoreManager. Ova metoda mora biti pozvana jednom na početku aplikacije (npr. u MainActivity).
@param context Context aplikacije.

- `addScore(ScoreEntry: newScoreEntry:, String: difficulty:)`
  - Dodaje novi rezultat za određeni nivo težine.
Rezultati se čuvaju sortirani opadajuće po skoru, i lista se ograničava na MAX_HIGH_SCORES.
@param newScoreEntry Objekat [ScoreEntry] koji se dodaje.
@param difficulty String koji predstavlja nivo težine (npr. "Lako", "Srednje", "Teško").

- `getHighScores(String: difficulty:)`: List<ScoreEntry>
  - Dohvata listu najboljih rezultata za određeni nivo težine.
Rezultati su već sortirani opadajuće po skoru (zbog Comparable implementacije).
@param difficulty String koji predstavlja nivo težine.
@return List objekata [ScoreEntry] za dati nivo težine.

- `clearScores(Context: context:)`
  - Briše sve sačuvane rezultate iz SharedPreferences.
@param context Context aplikacije.


#### SolutionDisplayActivity.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\SolutionDisplayActivity.kt`


**Klase:**

- `SolutionDisplayActivity`

**Funkcije:**

- `onCreate(Bundle?: savedInstanceState:)`
- `SolutionScreen(String: initialFen:, List<String>: solutionMoves:)`
- `SolutionScreenPreview()`

#### SoundManager.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\SoundManager.kt`


**Klase:**

- `SoundManager`

**Funkcije:**

- `initialize(Context: context:)`
- `release()`
- `playSound(Boolean: isSuccess:)`

#### TrainingPuzzleManager.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\TrainingPuzzleManager.kt`


**Klase:**

- `TrainingPuzzleManager`
  - Generiše nasumičnu laku zagonetku za trening mod.
Postavlja 1 belu figuru (od odabranih) i crne figure (targete) na putanji bele figure.

**Funkcije:**

- `generateEasyRandomPuzzle(List<PieceType>: selectedFigures:, Int: minTotalPawns:, Int: maxTotalPawns:)`: ChessBoard
  - Generiše nasumičnu laku zagonetku za trening mod.
Postavlja 1 belu figuru (od odabranih) i crne figure (targete) na putanji bele figure.

- `generateMediumRandomPuzzle(List<PieceType>: selectedFigures:, Int: minTotalPawns:, Int: maxTotalPawns:)`: ChessBoard
  - Generiše nasumičnu srednju zagonetku za trening mod.

- `generateHardRandomPuzzle(List<PieceType>: selectedFigures:, Int: minTotalPawns:, Int: maxTotalPawns:)`: ChessBoard
  - Generiše nasumičnu tešku zagonetku za trening mod.

- `findRandomEmptySquare(ChessBoard: board:, Set<Square>: occupiedSquares:)`: Square?

#### ExampleInstrumentedTest.kt

Putanja: `app\src\androidTest\java\com\chess\chesspuzzle\ExampleInstrumentedTest.kt`


**Klase:**

- `ExampleInstrumentedTest`

**Funkcije:**

- `useAppContext()`

### Paket: `com.chess.chesspuzzle.logic`


#### GameMechanics.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\logic\GameMechanics.kt`


**Funkcije:**

- `checkGameStatusLogic(ChessBoard: currentBoardSnapshot:, Int: currentTimeElapsed:, Difficulty: currentDifficulty:, String: playerName:, Int: currentSessionScore:)`: GameStatusResult = withContext(Dispatchers.Default)
- `calculateScoreInternal(Int: timeInSeconds:, Difficulty: currentDifficulty:)`: Int

#### MovePerformer.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\logic\MovePerformer.kt`


### Paket: `com.chess.chesspuzzle.ui.theme`


#### Color.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\ui\theme\Color.kt`


#### Theme.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\ui\theme\Theme.kt`


#### Type.kt

Putanja: `app\src\main\java\com\chess\chesspuzzle\ui\theme\Type.kt`
