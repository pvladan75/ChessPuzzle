# ChessPuzzle - Complete Method Documentation

**Total classes**: 28

**Total methods**: 21

## Classes and Methods


## Package: `com.chess.chesspuzzle`


### File: ExampleUnitTest.kt


#### Class: ExampleUnitTest

**Methods:**
- `public fun addition_isCorrect()`

### File: AppModels.kt


#### Class: GameStatusResult

#### Class: PieceType

#### Class: PieceColor

### File: ChessCore.kt


#### Class: ChessCore

**Methods:**
- `public fun getValidMoves(ChessBoard: board:, Piece: piece:, Square: fromSquare:) : List<Square>`

### File: ChessDefinitions.kt


#### Class: PieceColor

#### Class: Square

#### Class: ChessBoard

**Methods:**
- `public fun getPiece(Square: square:) : Piece`

#### Class: ScoreEntry

**Methods:**
- `public override fun compareTo(ScoreEntry: other:) : Int`

### File: ChessProblem.kt


### File: ChessSolver.kt


#### Class: ChessSolver

**Methods:**
- `public override fun toString() : String`

### File: CompetitionPuzzleLoader.kt


#### Class: CompetitionPuzzleLoader

**Methods:**
- `private fun loadAllPuzzlesIfNecessary(Context: context:)`

### File: FigureSelectionActivity.kt


#### Class: Difficulty

#### Class: FigureSelectionActivity

**Methods:**
- `public override fun onCreate(Bundle?: savedInstanceState:)`

### File: GameActivity.kt


#### Class: GameActivity

**Methods:**
- `public override fun onCreate(Bundle?: savedInstanceState:)`

### File: HighScoresActivity.kt


#### Class: HighScoresActivity

**Methods:**
- `public override fun onCreate(Bundle?: savedInstanceState:)`

### File: MainActivity.kt


#### Class: MainActivity

**Methods:**
- `public override fun onCreate(Bundle?: savedInstanceState:)`

### File: PositionCreationActivity.kt


#### Class: PositionCreationActivity

**Methods:**
- `public override fun onCreate(Bundle?: savedInstanceState:)`

### File: PositionGenerator.kt


#### Class: PositionGenerator

**Methods:**
- `public fun generate(Map<PieceType: whitePiecesConfig:, Int>: Any, Int: numBlackPieces:, 1000: =) : String?`

### File: PuzzleDataHandler.kt


#### Class: PuzzleDataHandler

**Methods:**
- `public fun loadUserPuzzles(Context: context:, String: fileName:) : List<ChessProblem>`

### File: PuzzleLoader.kt


#### Class: PuzzleLoader

**Methods:**
- `public fun loadPuzzlesFromJson(Context: context:, String: fileName:) : List<ChessProblem>`

### File: ScoreManager.kt


#### Class: ScoreManager

**Methods:**
- `public fun init(Context: context:)`

### File: SolutionDisplayActivity.kt


#### Class: ChessSolution

**Methods:**
- `public override fun onCreate(Bundle?: savedInstanceState:)`

### File: SoundManager.kt


#### Class: SoundManager

**Methods:**
- `public fun initialize(Context: context:)`

### File: SquareAdapter.kt


#### Class: SquareAdapter

**Methods:**
- `public override fun write(JsonWriter: out:, Square?: value:)`

### File: TrainingPuzzleManager.kt


#### Class: TrainingPuzzleManager

**Methods:**
- `public fun generateEasyRandomPuzzle(List<PieceType>: selectedFigures:, Int: minTotalPawns:, Int: maxTotalPawns:) : ChessBoard`

### File: ExampleInstrumentedTest.kt


#### Class: ExampleInstrumentedTest

**Methods:**
- `public fun useAppContext()`

## Package: `com.chess.chesspuzzle.logic`


### File: GameMechanics.kt


### File: MovePerformer.kt


## Package: `com.chess.chesspuzzle.viewmodel`


### File: GameViewModel.kt


#### Class: GameViewModel

#### Class: Factory

**Methods:**
- `public override fun create(Class<T>: modelClass:) : T`

## Package: `com.chess.chesspuzzle.ui.theme`


### File: Color.kt


### File: Theme.kt


### File: Type.kt


## Package: `com.chess.chesspuzzle.presentation.ui.components`


### File: ChessBoardComposable.kt


### File: GameControls.kt


### File: GameHeader.kt


### File: GameStatusBar.kt


## Package: `com.chess.chesspuzzle.presentation.ui.screens`


### File: GameScreen.kt
