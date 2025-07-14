# ChessPuzzle - Method Documentation

**Total files**: 32  

**Total classes**: 28  

**Total functions**: 89

## Classes and Methods


## Package: `com.chess.chesspuzzle`


### File: ExampleUnitTest.kt


#### public class ExampleUnitTest

**Methods:**
- `fun addition_isCorrect()`

### File: AppModels.kt


#### public class GameStatusResult

#### public enum class PieceType

#### public enum class PieceColor

### File: ChessCore.kt


#### public object ChessCore

**Methods:**
- `fun getValidMoves(ChessBoard: board:, Piece: piece:, Square: fromSquare:) : List<Square>`

### File: ChessDefinitions.kt


#### public enum class PieceColor

#### public class Square

#### public class ChessBoard

**Methods:**
- `fun getPiece(Square: square:) : Piece`

#### public class ScoreEntry

**Methods:**
- `override fun compareTo(ScoreEntry: other:) : Int`

### File: ChessProblem.kt


### File: ChessSolver.kt


#### public class ChessSolver

**Methods:**
- `override fun toString() : String`

### File: CompetitionPuzzleLoader.kt


#### public object CompetitionPuzzleLoader

**Methods:**
- `fun loadAllPuzzlesIfNecessary(Context: context:)`

### File: FigureSelectionActivity.kt


#### public enum class Difficulty

#### public class FigureSelectionActivity

**Methods:**
- `override fun onCreate(Bundle?: savedInstanceState:)`

### File: GameActivity.kt


#### public class GameActivity

**Methods:**
- `override fun onCreate(Bundle?: savedInstanceState:)`

### File: HighScoresActivity.kt


#### public class HighScoresActivity

**Methods:**
- `override fun onCreate(Bundle?: savedInstanceState:)`

### File: MainActivity.kt


#### public class MainActivity

**Methods:**
- `override fun onCreate(Bundle?: savedInstanceState:)`

### File: PositionCreationActivity.kt


#### public class PositionCreationActivity

**Methods:**
- `override fun onCreate(Bundle?: savedInstanceState:)`

### File: PositionGenerator.kt


#### public class PositionGenerator

**Methods:**
- `fun generate(Map<PieceType: whitePiecesConfig:, Int>: Any, Int: numBlackPieces:, 1000: =) : String?`

### File: PuzzleDataHandler.kt


#### public object PuzzleDataHandler

**Methods:**
- `fun loadUserPuzzles(Context: context:, String: fileName:) : List<ChessProblem>`

### File: PuzzleLoader.kt


#### public object PuzzleLoader

**Methods:**
- `fun loadPuzzlesFromJson(Context: context:, String: fileName:) : List<ChessProblem>`

### File: ScoreManager.kt


#### public object ScoreManager

**Methods:**
- `fun init(Context: context:)`

### File: SolutionDisplayActivity.kt


#### public class ChessSolution

**Methods:**
- `override fun onCreate(Bundle?: savedInstanceState:)`

### File: SoundManager.kt


#### public object SoundManager

**Methods:**
- `fun initialize(Context: context:)`

### File: SquareAdapter.kt


#### public class SquareAdapter

**Methods:**
- `override fun write(JsonWriter: out:, Square?: value:)`

### File: TrainingPuzzleManager.kt


#### public object TrainingPuzzleManager

**Methods:**
- `fun generateEasyRandomPuzzle(List<PieceType>: selectedFigures:, Int: minTotalPawns:, Int: maxTotalPawns:) : ChessBoard`

### File: ExampleInstrumentedTest.kt


#### public class ExampleInstrumentedTest

**Methods:**
- `fun useAppContext()`

## Package: `com.chess.chesspuzzle.logic`


### File: GameMechanics.kt


**Top-level Functions:**
- `suspend fun checkGameStatusLogic(ChessBoard: currentBoardSnapshot:, Int: currentTimeElapsed:, Difficulty: currentDifficulty:, String: playerName:, Int: currentSessionScore:) : GameStatusResult = withContext(Dispatchers.Default)`
- `fun calculateScoreInternal(Int: timeInSeconds:, Difficulty: currentDifficulty:) : Int`

### File: MovePerformer.kt


## Package: `com.chess.chesspuzzle.viewmodel`


### File: GameViewModel.kt


#### public class GameViewModel

#### public class Factory

**Methods:**
- `override fun create(Class<T>: modelClass:) : T`

## Package: `com.chess.chesspuzzle.ui.theme`


### File: Color.kt


### File: Theme.kt


### File: Type.kt


## Package: `com.chess.chesspuzzle.presentation.ui.components`


### File: ChessBoardComposable.kt


**Top-level Functions:**
- `fun getPieceDrawable(PieceType: pieceType:, PieceColor: pieceColor:) : Int`

### File: GameControls.kt


### File: GameHeader.kt


**Top-level Functions:**
- `fun GameHeader(String: playerName:, Boolean: isTrainingMode:, Difficulty: difficulty:)`

### File: GameStatusBar.kt


**Top-level Functions:**
- `fun GameStatusBar(Int: timeElapsedSeconds:, Int: solvedPuzzlesCount:, Int: currentSessionScore:)`

## Package: `com.chess.chesspuzzle.presentation.ui.screens`


### File: GameScreen.kt


**Top-level Functions:**
- `fun GameScreen(Difficulty: difficulty:, List<PieceType>: selectedFigures:, Int: minPawns:, Int: maxPawns:, String: playerName:, Boolean: isTrainingMode:, Activity-ja: iz)`
- `fun DefaultGameScreenPreview()`