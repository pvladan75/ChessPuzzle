package com.chess.chesspuzzle

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope

// DODATI IMPORTI ZA NOVE FUNKCIJE
import com.chess.chesspuzzle.logic.checkGameStatusLogic
import com.chess.chesspuzzle.logic.performMove
import com.chess.chesspuzzle.puzzle.generateNewPuzzleLogic // Assuming it's in .puzzle package

// No import needed for CompetitionPuzzleLoader and TrainingPuzzleManager if they are in the same 'com.chess.chesspuzzle' package.

class GameActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoundManager.initializeSoundPool(applicationContext)
        ScoreManager.init(applicationContext)

        val difficultyString = intent.getStringExtra("difficulty") ?: "EASY"
        val difficulty = try {
            Difficulty.valueOf(difficultyString.uppercase())
        } catch (e: IllegalArgumentException) {
            Log.e("GameActivity", "Nepoznata težina: $difficultyString, Koristim EASY.", e)
            Difficulty.EASY
        }

        val selectedFiguresNames = intent.getStringArrayListExtra("selectedFigures") ?: arrayListOf()
        val selectedFigures = selectedFiguresNames.mapNotNull {
            try {
                PieceType.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                Log.e("GameActivity", "Nepoznat PieceType: $it", e)
                null
            }
        }

        val minPawns = intent.getIntExtra("minPawns", 3)
        val maxPawns = intent.getIntExtra("maxPawns", 5)

        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"
        val isTrainingMode = intent.getBooleanExtra("isTrainingMode", true)

        Log.d("GameActivity", "Pokrenut GameActivity sa težinom: $difficulty, figurama: ${selectedFigures.joinToString()}, minPawns: $minPawns, maxPawns: $maxPawns, igrač: $playerName, trening mod: $isTrainingMode")

        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChessGameScreen(
                        difficulty = difficulty,
                        selectedFigures = selectedFigures,
                        minPawns = minPawns,
                        maxPawns = maxPawns,
                        playerName = playerName,
                        isTrainingMode = isTrainingMode,
                        applicationContext = applicationContext
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.releaseSoundPool()
    }
}

// Data klase su i dalje ovde ili u zasebnom fajlu, po vašem izboru
data class GameStatusResult(
    val updatedBlackPieces: Map<Square, Piece>,
    val puzzleCompleted: Boolean,
    val noMoreMoves: Boolean,
    val solvedPuzzlesCountIncrement: Int = 0,
    val scoreForPuzzle: Int = 0,
    val gameStarted: Boolean,
    val newSessionScore: Int
)

data class PuzzleGenerationResult(
    val newBoard: ChessBoard,
    val penaltyApplied: Int = 0,
    val success: Boolean,
    val gameStartedAfterGeneration: Boolean = false,
    val newSessionScore: Int
)

@Composable
fun ChessGameScreen(
    difficulty: Difficulty,
    selectedFigures: List<PieceType>,
    minPawns: Int,
    maxPawns: Int,
    playerName: String,
    isTrainingMode: Boolean,
    applicationContext: Context?
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var board: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    var initialBoardBackup: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }

    var blackPieces: Map<Square, Piece> by remember { mutableStateOf(emptyMap()) }

    var puzzleCompleted: Boolean by remember { mutableStateOf(false) }
    var noMoreMoves: Boolean by remember { mutableStateOf(false) }

    var timeElapsedSeconds by remember { mutableStateOf(0) }
    var gameStarted by remember { mutableStateOf(0) }
    var solvedPuzzlesCount by remember { mutableStateOf(0) }
    var currentSessionScore by remember { mutableStateOf(0) }

    var selectedSquare: Square? by remember { mutableStateOf(null) }
    var highlightedSquares: Set<Square> by remember { mutableStateOf(emptySet()) }

    var isSolutionDisplaying by remember { mutableStateOf(false) }
    var solverSolutionPath: List<ChessSolver.MoveData>? by remember { mutableStateOf(null) }
    var currentSolutionStep by remember { mutableStateOf(0) }


    LaunchedEffect(gameStarted) {
        while (gameStarted == 1) {
            delay(1000L)
            timeElapsedSeconds++
        }
    }

    LaunchedEffect(Unit) {
        loadOrGenerateInitialPuzzle(
            coroutineScope,
            applicationContext,
            difficulty,
            selectedFigures,
            minPawns,
            maxPawns,
            playerName,
            isTrainingMode,
            updateBoard = { newBoard -> board = newBoard },
            updateInitialBoardBackup = { newBoard -> initialBoardBackup = newBoard },
            updatePuzzleCompleted = { completed -> puzzleCompleted = completed },
            updateNoMoreMoves = { noMoves -> noMoreMoves = noMoves },
            updateTimeElapsedSeconds = { time -> timeElapsedSeconds = time },
            updateSelectedSquare = { square -> selectedSquare = square },
            updateHighlightedSquares = { squares -> highlightedSquares = squares },
            updateCurrentSessionScore = { score -> currentSessionScore = score },
            updateGameStarted = { started -> gameStarted = started },
            updateIsSolutionDisplaying = { displaying -> isSolutionDisplaying = displaying },
            updateSolverSolutionPath = { path -> solverSolutionPath = path },
            updateCurrentSolutionStep = { step -> currentSolutionStep = step },
            updateBlackPieces = { pieces -> blackPieces = pieces },
            updateSolvedPuzzlesCount = { count -> solvedPuzzlesCount = count }
        )
    }

    Column( // <-- THIS COLUMN IS WHERE THE PROBLEM WAS LIKELY
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Igrač: $playerName",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Mod: ${if (isTrainingMode) "Trening" else "Takmičarski"}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Težina: ${difficulty.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vreme: ${timeElapsedSeconds}s",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Rešeno: ${solvedPuzzlesCount}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Skor: ${currentSessionScore}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (puzzleCompleted) {
                Text(
                    text = "Čestitamo! Rešili ste zagonetku!",
                    color = Color.Green,
                    style = MaterialTheme.typography.headlineMedium
                )
            } else if (noMoreMoves) {
                Text(
                    text = "Nema više legalnih poteza za hvatanje crnih figura!",
                    color = Color.Red,
                    style = MaterialTheme.typography.headlineMedium
                )
            } else if (isSolutionDisplaying && solverSolutionPath != null && solverSolutionPath!!.isNotEmpty()) {
                Text(
                    text = "Rešenje Solvera: ${solverSolutionPath!![currentSolutionStep].uci()}...",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall
                )
            } else {
                Text(
                    text = "Preostalo crnih figura: ${blackPieces.size}",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        ChessBoardComposable(
            board = board,
            selectedSquare = selectedSquare,
            highlightedSquares = highlightedSquares,
            onSquareClick = { clickedSquare ->
                if (puzzleCompleted || noMoreMoves || gameStarted == 0 || isSolutionDisplaying) {
                } else {
                    val pieceOnClickedSquare = board.getPiece(clickedSquare)

                    if (selectedSquare == null) {
                        if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                            selectedSquare = clickedSquare
                            val legalMoves =
                                ChessCore.getValidMoves(board, pieceOnClickedSquare, clickedSquare)
                            highlightedSquares = legalMoves.toSet()
                        } else {
                            selectedSquare = null
                            highlightedSquares = emptySet()
                        }
                    } else {
                        val fromSquare = selectedSquare!!
                        val toSquare = clickedSquare
                        val pieceToMove = board.getPiece(fromSquare)

                        highlightedSquares = emptySet()

                        if (fromSquare == toSquare) {
                            selectedSquare = null
                        } else if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                            selectedSquare = clickedSquare
                            val legalMoves =
                                ChessCore.getValidMoves(board, pieceOnClickedSquare, clickedSquare)
                            highlightedSquares = legalMoves.toSet()
                        } else {
                            val legalChessMovesForSelectedPiece =
                                ChessCore.getValidMoves(board, pieceToMove, fromSquare)
                            val isPureChessValidMove =
                                legalChessMovesForSelectedPiece.contains(toSquare)

                            val pieceAtTarget = board.getPiece(toSquare)
                            val isCaptureOfBlackPiece = pieceAtTarget.type != PieceType.NONE &&
                                    pieceAtTarget.color == PieceColor.BLACK &&
                                    pieceToMove.color != pieceAtTarget.color

                            val isPuzzleValidMove = isPureChessValidMove && isCaptureOfBlackPiece

                            if (isPuzzleValidMove) {
                                coroutineScope.launch {
                                    performMove(
                                        fromSquare,
                                        toSquare,
                                        board,
                                        updateBoardState = { newBoard -> board = newBoard },
                                        checkGameStatusLogic = ::checkGameStatusLogic,
                                        currentTimeElapsed = timeElapsedSeconds,
                                        currentDifficulty = difficulty,
                                        playerName = playerName,
                                        currentSessionScore = currentSessionScore,
                                        capture = true,
                                        targetSquare = toSquare
                                    ) { statusResult ->
                                        coroutineScope.launch(Dispatchers.Main) {
                                            blackPieces = statusResult.updatedBlackPieces
                                            puzzleCompleted = statusResult.puzzleCompleted
                                            noMoreMoves = statusResult.noMoreMoves
                                            solvedPuzzlesCount += statusResult.solvedPuzzlesCountIncrement
                                            currentSessionScore = statusResult.newSessionScore
                                            gameStarted = if (statusResult.gameStarted) 1 else 0

                                            if (statusResult.puzzleCompleted) {
                                                SoundManager.playSound(true)
                                            } else if (statusResult.noMoreMoves) {
                                                SoundManager.playSound(false)
                                            }
                                        }
                                    }
                                    selectedSquare = null
                                }
                            } else if (isPureChessValidMove && !isCaptureOfBlackPiece) {
                                selectedSquare = null
                            } else {
                                selectedSquare = null
                            }
                        }
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    isSolutionDisplaying = false
                    solverSolutionPath = null
                    currentSolutionStep = 0

                    selectedSquare = null
                    highlightedSquares = emptySet()
                    board = initialBoardBackup.copy()
                    puzzleCompleted = false
                    noMoreMoves = false
                    timeElapsedSeconds = 0
                    gameStarted = 1

                    coroutineScope.launch(Dispatchers.IO) {
                        val statusResult = checkGameStatusLogic(
                            board,
                            timeElapsedSeconds,
                            difficulty,
                            playerName,
                            currentSessionScore
                        )
                        withContext(Dispatchers.Main) {
                            blackPieces = statusResult.updatedBlackPieces
                            puzzleCompleted = statusResult.puzzleCompleted
                            noMoreMoves = statusResult.noMoreMoves
                            currentSessionScore = statusResult.newSessionScore
                            gameStarted = if (statusResult.gameStarted) 1 else 0
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text("Resetuj poziciju")
            }
            Button(
                onClick = {
                    isSolutionDisplaying = false
                    solverSolutionPath = null
                    currentSolutionStep = 0

                    selectedSquare = null
                    highlightedSquares = emptySet()

                    coroutineScope.launch(Dispatchers.IO) {
                        val result = generateNewPuzzleLogic(
                            applicationContext,
                            difficulty,
                            selectedFigures,
                            minPawns,
                            maxPawns,
                            gameStarted == 1,
                            playerName,
                            currentSessionScore,
                            puzzleCompleted,
                            noMoreMoves,
                            isTrainingMode
                        )
                        withContext(Dispatchers.Main) {
                            if (result.success) {
                                board = result.newBoard
                                initialBoardBackup = result.newBoard.copy()
                                puzzleCompleted = false
                                noMoreMoves = false
                                timeElapsedSeconds = 0
                                selectedSquare = null
                                highlightedSquares = emptySet()
                                currentSessionScore = result.newSessionScore
                                gameStarted = if (result.gameStartedAfterGeneration) 1 else 0

                                val statusResult = checkGameStatusLogic(
                                    board,
                                    timeElapsedSeconds,
                                    difficulty,
                                    playerName,
                                    currentSessionScore
                                )
                                blackPieces = statusResult.updatedBlackPieces
                                puzzleCompleted = statusResult.puzzleCompleted
                                noMoreMoves = statusResult.noMoreMoves
                                solvedPuzzlesCount += statusResult.solvedPuzzlesCountIncrement
                                currentSessionScore = statusResult.newSessionScore
                                gameStarted = if (statusResult.gameStarted) 1 else 0

                                Log.d("PUZZLE_BOARD_STATE", "New Puzzle FEN: ${board.toFEN()}")
                                val blackPiecesOnBoardAfterGen =
                                    board.getPiecesMapFromBoard(PieceColor.BLACK)
                                Log.d(
                                    "PUZZLE_BOARD_STATE",
                                    "Black pieces detected on new board (count: ${blackPiecesOnBoardAfterGen.size}):"
                                )
                                blackPiecesOnBoardAfterGen.forEach { (square, piece) ->
                                    Log.d(
                                        "PUZZLE_BOARD_STATE",
                                        "  - ${piece.color} ${piece.type} at ${square.file}${square.rank}"
                                    )
                                }

                                if (statusResult.gameStarted && result.success) {
                                    SoundManager.playSound(true)
                                } else if (!statusResult.gameStarted && !statusResult.puzzleCompleted && !statusResult.noMoreMoves) {
                                    SoundManager.playSound(false)
                                }

                            } else {
                                Log.e(
                                    "GameActivity",
                                    "Nije moguće generisati/učitati zagonetku, vraćena prazna tabla. Pokušajte ponovo ili promenite postavke."
                                )
                                gameStarted = 0
                                SoundManager.playSound(false)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text("Nova Zagonetka")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (isTrainingMode && gameStarted == 1 && !puzzleCompleted && !noMoreMoves) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSolutionDisplaying = true
                        currentSolutionStep = 0
                        solverSolutionPath = null
                        highlightedSquares = emptySet()

                        Log.d("GameActivity", "Pokrećem Solver za FEN: ${board.toFEN()}")
                        val solution = ChessSolver.solveCapturePuzzle(board.toFEN())

                        withContext(Dispatchers.Main) {
                            if (solution != null && solution.isNotEmpty()) {
                                Log.d(
                                    "GameActivity",
                                    "Solver pronašao rešenje: ${solution.map { it.uci() }}"
                                )
                                solverSolutionPath = solution
                                val firstMove = solution.first()
                                highlightedSquares = setOf(firstMove.fromSquare, firstMove.toSquare)
                            } else {
                                Log.d(
                                    "GameActivity",
                                    "Solver nije pronašao rešenje za trenutnu poziciju."
                                )
                                solverSolutionPath = null
                                highlightedSquares = emptySet()
                                isSolutionDisplaying = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Pomoć (Prikaži rešenje)")
            }

            if (isSolutionDisplaying && solverSolutionPath != null && solverSolutionPath!!.size > currentSolutionStep) {
                Button(
                    onClick = {
                        val nextStep = currentSolutionStep + 1
                        if (nextStep < solverSolutionPath!!.size) {
                            currentSolutionStep = nextStep
                            val nextMove = solverSolutionPath!![currentSolutionStep]
                            highlightedSquares = setOf(nextMove.fromSquare, nextMove.toSquare)
                        } else {
                            isSolutionDisplaying = false
                            solverSolutionPath = null
                            currentSolutionStep = 0
                            highlightedSquares = emptySet()
                            selectedSquare = null
                            Log.d("GameActivity", "Kraj rešenja Solvera. Vraćam na igru.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(if (currentSolutionStep < solverSolutionPath!!.size - 1) "Sledeći potez rešenja" else "Završi prikaz rešenja")
                }
            } else if (isSolutionDisplaying && (solverSolutionPath == null || solverSolutionPath!!.isEmpty())) {
                Button(
                    onClick = {
                        isSolutionDisplaying = false
                        solverSolutionPath = null
                        currentSolutionStep = 0
                        highlightedSquares = emptySet()
                        selectedSquare = null
                        Log.d("GameActivity", "Nema rešenja Solvera. Vraćam na igru.")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Nastavi sa igrom (Nema rešenja)")
                }
            }
        }
        Button(
            onClick = {
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Nazad na Odabir Figure")
        }
    } // <-- THIS CLOSING BRACE WAS LIKELY MISSING OR MISPLACED!
}

// Ensure the loadOrGenerateInitialPuzzle function is correctly defined outside ChessGameScreen
// (as it is in your previous paste)
suspend fun loadOrGenerateInitialPuzzle(
    coroutineScope: CoroutineScope,
    applicationContext: Context?,
    difficulty: Difficulty,
    selectedFigures: List<PieceType>,
    minPawns: Int,
    maxPawns: Int,
    playerName: String,
    isTrainingMode: Boolean,
    updateBoard: (ChessBoard) -> Unit,
    updateInitialBoardBackup: (ChessBoard) -> Unit,
    updatePuzzleCompleted: (Boolean) -> Unit,
    updateNoMoreMoves: (Boolean) -> Unit,
    updateTimeElapsedSeconds: (Int) -> Unit,
    updateSelectedSquare: (Square?) -> Unit,
    updateHighlightedSquares: (Set<Square>) -> Unit,
    updateCurrentSessionScore: (Int) -> Unit,
    updateGameStarted: (Int) -> Unit,
    updateIsSolutionDisplaying: (Boolean) -> Unit,
    updateSolverSolutionPath: (List<ChessSolver.MoveData>?) -> Unit,
    updateCurrentSolutionStep: (Int) -> Unit,
    updateBlackPieces: (Map<Square, Piece>) -> Unit,
    updateSolvedPuzzlesCount: (Int) -> Unit
) {
    withContext(Dispatchers.IO) {
        val newPuzzleBoard: ChessBoard? = if (isTrainingMode) {
            if (applicationContext == null) {
                Log.e("GameActivity", "ApplicationContext is null for training puzzle generation!")
                null
            } else {
                when (difficulty) {
                    Difficulty.EASY -> TrainingPuzzleManager.generateEasyRandomPuzzle(selectedFigures, minPawns, maxPawns)
                    Difficulty.MEDIUM -> TrainingPuzzleManager.generateMediumRandomPuzzle(selectedFigures, minPawns, maxPawns)
                    Difficulty.HARD -> TrainingPuzzleManager.generateHardRandomPuzzle(selectedFigures, minPawns, maxPawns)
                }
            }
        } else {
            if (applicationContext == null) {
                Log.e("GameActivity", "ApplicationContext je null pri učitavanju takmičarske zagonetke!")
                null
            } else {
                when (difficulty) {
                    Difficulty.EASY -> CompetitionPuzzleLoader.loadEasyPuzzleFromJson(applicationContext)
                    Difficulty.MEDIUM -> CompetitionPuzzleLoader.loadMediumPuzzleFromJson(applicationContext)
                    Difficulty.HARD -> CompetitionPuzzleLoader.loadHardPuzzleFromJson(applicationContext)
                }
            }
        }

        withContext(Dispatchers.Main) {
            if (newPuzzleBoard != null && newPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).isNotEmpty()) {
                updateBoard(newPuzzleBoard)
                updateInitialBoardBackup(newPuzzleBoard.copy())
                updatePuzzleCompleted(false)
                updateNoMoreMoves(false)
                updateTimeElapsedSeconds(0)
                updateSelectedSquare(null)
                updateHighlightedSquares(emptySet())

                updateCurrentSessionScore(0)
                val initialSessionScoreForCheck = 0

                updateGameStarted(1)

                updateIsSolutionDisplaying(false)
                updateSolverSolutionPath(null)
                updateCurrentSolutionStep(0)

                val statusResult = checkGameStatusLogic(
                    newPuzzleBoard,
                    0,
                    difficulty,
                    playerName,
                    initialSessionScoreForCheck
                )
                updateBlackPieces(statusResult.updatedBlackPieces)
                updatePuzzleCompleted(statusResult.puzzleCompleted)
                updateNoMoreMoves(statusResult.noMoreMoves)
                updateSolvedPuzzlesCount(statusResult.solvedPuzzlesCountIncrement)
                updateCurrentSessionScore(statusResult.newSessionScore)
                updateGameStarted(if (statusResult.gameStarted) 1 else 0)

                Log.d("PUZZLE_BOARD_STATE", "Loaded/Generated FEN: ${newPuzzleBoard.toFEN()}")
                val blackPiecesOnBoard = newPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK)
                Log.d(
                    "PUZZLE_BOARD_STATE",
                    "Black pieces detected on board (count: ${blackPiecesOnBoard.size}):"
                )
                blackPiecesOnBoard.forEach { (square, piece) ->
                    Log.d(
                        "PUZZLE_BOARD_STATE",
                        "  - ${piece.color} ${piece.type} at ${square.file}${square.rank}"
                    )
                }

                if (statusResult.gameStarted) {
                    SoundManager.playSound(true)
                } else if (!statusResult.puzzleCompleted && !statusResult.noMoreMoves) {
                    SoundManager.playSound(false)
                }

            } else {
                Log.e(
                    "GameActivity",
                    "Nije moguće generisati/učitati zagonetku, vraćena prazna tabla. Pokušajte ponovo ili promenite postavke."
                )
                updateGameStarted(0)
                SoundManager.playSound(false)
            }
        }
    }
}


@Composable
fun DefaultPreview() {
    val context = LocalContext.current

    ChessPuzzleTheme {
        ChessGameScreen(
            difficulty = Difficulty.MEDIUM,
            selectedFigures = listOf(PieceType.QUEEN, PieceType.KNIGHT),
            minPawns = 6,
            maxPawns = 9,
            playerName = "Preview Igrač",
            isTrainingMode = true,
            applicationContext = context.applicationContext
        )
    }
}