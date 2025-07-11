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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlin.random.Random

import com.chess.chesspuzzle.logic.checkGameStatusLogic
import com.chess.chesspuzzle.logic.performMove

class GameActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("GameActivity", "Initializing SoundManager from GameActivity onCreate...")
        SoundManager.initialize(applicationContext)
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
        Log.d("GameActivity", "Releasing SoundManager from GameActivity onDestroy...")
        SoundManager.release()
    }
}

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

    val chessSolver = remember { ChessSolver() }
    val puzzleGenerator = remember { PositionGenerator(chessSolver) }

    var board: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    var initialBoardBackup: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }

    var blackPieces: Map<Square, Piece> by remember { mutableStateOf(emptyMap()) }

    var puzzleCompleted: Boolean by remember { mutableStateOf(false) }
    var noMoreMoves: Boolean by remember { mutableStateOf(false) }

    val timeElapsedSecondsState = remember { mutableIntStateOf(0) } // Holds the current time in seconds

    val gameStartedState = remember { mutableStateOf(false) } // Holds whether the game timer is running

    var solvedPuzzlesCount by rememberSaveable { mutableIntStateOf(0) } // This is correctly `by` delegated
    var currentSessionScore by rememberSaveable { mutableIntStateOf(0) } // This is correctly `by` delegated

    val selectedSquareState = remember { mutableStateOf<Square?>(null) } // Manages the currently selected square
    val highlightedSquaresState = remember { mutableStateOf<Set<Square>>(emptySet()) } // Manages highlighted squares

    var isSolutionDisplaying by remember { mutableStateOf(false) }
    var solverSolutionPath: List<ChessSolver.MoveData>? by remember { mutableStateOf(null) }
    val currentSolutionStepState = remember { mutableIntStateOf(0) } // Use mutableIntStateOf for currentSolutionStep

    // This LaunchedEffect manages the timer
    LaunchedEffect(gameStartedState.value) {
        var isTimerActive = gameStartedState.value
        while (isTimerActive) {
            delay(1000L)
            timeElapsedSecondsState.intValue++
            isTimerActive = gameStartedState.value
        }
    }

    // This LaunchedEffect loads the initial puzzle
    LaunchedEffect(Unit) {
        generateAndLoadPuzzle(
            puzzleGenerator = puzzleGenerator,
            coroutineScope = coroutineScope,
            applicationContext = applicationContext,
            difficulty = difficulty,
            selectedFigures = selectedFigures,
            minPawns = minPawns,
            maxPawns = maxPawns,
            playerName = playerName,
            isTrainingMode = isTrainingMode,
            currentSessionScore = currentSessionScore,
            updateBoard = { newBoard -> board = newBoard },
            updateInitialBoardBackup = { newBoard -> initialBoardBackup = newBoard },
            updatePuzzleCompleted = { completed -> puzzleCompleted = completed },
            updateNoMoreMoves = { noMovesValue -> noMoreMoves = noMovesValue },
            updateTimeElapsedSeconds = { time -> timeElapsedSecondsState.intValue = time },
            updateSelectedSquare = { square -> selectedSquareState.value = square },
            updateHighlightedSquares = { squares -> highlightedSquaresState.value = squares },
            updateCurrentSessionScore = { score -> currentSessionScore = score },
            updateGameStarted = { started -> gameStartedState.value = started },
            updateIsSolutionDisplaying = { displaying -> isSolutionDisplaying = displaying },
            updateSolverSolutionPath = { path -> solverSolutionPath = path },
            updateCurrentSolutionStep = { step -> currentSolutionStepState.intValue = step },
            updateBlackPieces = { pieces -> blackPieces = pieces },
            updateSolvedPuzzlesCount = { count -> solvedPuzzlesCount = count }
        )
    }

    Column(
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
                text = "Vreme: ${timeElapsedSecondsState.intValue}s",
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
                    text = "Rešenje Solvera: ${solverSolutionPath!![currentSolutionStepState.intValue].fromSquare.toString()}-${solverSolutionPath!![currentSolutionStepState.intValue].toSquare.toString()}...",
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
            selectedSquare = selectedSquareState.value,
            highlightedSquares = highlightedSquaresState.value,
            onSquareClick = { clickedSquare ->
                if (puzzleCompleted || noMoreMoves || !gameStartedState.value || isSolutionDisplaying) {
                    // Don't allow interaction if the puzzle is solved, no moves are left,
                    // game hasn't started, or solution is being displayed.
                } else {
                    val pieceOnClickedSquare = board.getPiece(clickedSquare)

                    if (selectedSquareState.value == null) {
                        // Prvi klik: Ako je bela figura, selektuj je i prikaži poteze.
                        if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                            selectedSquareState.value = clickedSquare
                            val legalMoves =
                                ChessCore.getValidMoves(board, pieceOnClickedSquare, clickedSquare)
                            highlightedSquaresState.value = legalMoves.toSet()
                        } else {
                            // Kliknuto na prazno polje ili crnu figuru kada ništa nije selektovano - poništi selekciju.
                            selectedSquareState.value = null
                            highlightedSquaresState.value = emptySet()
                        }
                    } else {
                        // Drugi klik: Pokušaj da pomeriš selektovanu figuru.
                        val fromSquare = selectedSquareState.value!!
                        val toSquare = clickedSquare
                        val pieceToMove = board.getPiece(fromSquare)

                        if (fromSquare == toSquare) {
                            // Kliknuto na istu figuru - poništi selekciju.
                            selectedSquareState.value = null
                            highlightedSquaresState.value = emptySet()
                        } else if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                            // Kliknuto na drugu belu figuru - ponovo selektuj.
                            selectedSquareState.value = clickedSquare
                            val legalMoves = ChessCore.getValidMoves(board, pieceOnClickedSquare, clickedSquare)
                            highlightedSquaresState.value = legalMoves.toSet()
                        } else {
                            // Pokušaj pomeranja na ciljno polje (prazno ili crna figura).
                            val legalChessMovesForSelectedPiece = ChessCore.getValidMoves(board, pieceToMove, fromSquare)
                            val isPureChessValidMove = legalChessMovesForSelectedPiece.contains(toSquare)

                            val pieceAtTarget = board.getPiece(toSquare)
                            val isCaptureOfBlackPiece = pieceAtTarget.type != PieceType.NONE &&
                                    pieceAtTarget.color == PieceColor.BLACK &&
                                    pieceToMove.color != pieceAtTarget.color

                            val isPuzzleValidMove = isPureChessValidMove && isCaptureOfBlackPiece

                            if (isPuzzleValidMove) {
                                // Potez je validan za zagonetku (hvata crnu figuru).
                                // NE RESETUJEMO selectedSquareState I highlightedSquaresState OVDE!
                                // Očekujemo da figura ostane selektovana.
                                coroutineScope.launch {
                                    val statusResult = performMove(
                                        fromSquare,
                                        toSquare,
                                        board,
                                        updateBoardState = { newBoard -> board = newBoard },
                                        checkGameStatusLogic = ::checkGameStatusLogic,
                                        currentTimeElapsed = timeElapsedSecondsState.intValue,
                                        currentDifficulty = difficulty,
                                        playerName = playerName,
                                        currentSessionScore = currentSessionScore,
                                        capture = true,
                                        targetSquare = toSquare
                                    )

                                    withContext(Dispatchers.Main) {
                                        blackPieces = statusResult.updatedBlackPieces
                                        puzzleCompleted = statusResult.puzzleCompleted
                                        noMoreMoves = statusResult.noMoreMoves
                                        solvedPuzzlesCount += statusResult.solvedPuzzlesCountIncrement
                                        currentSessionScore = statusResult.newSessionScore
                                        gameStartedState.value = statusResult.gameStarted

                                        // Resetuj selekciju samo ako je zagonetka završena
                                        if (statusResult.puzzleCompleted || statusResult.noMoreMoves) {
                                            selectedSquareState.value = null
                                            highlightedSquaresState.value = emptySet()
                                        } else {
                                            // Ako zagonetka NIJE završena, i dalje ima crnih figura za hvatanje,
                                            // treba ažurirati samo highlightovane kvadrate za sledeći potez sa iste figure.
                                            // Pretpostavljamo da je pieceToMove i dalje ista figura, samo na novom polju.
                                            val updatedLegalMoves = ChessCore.getValidMoves(board, pieceToMove, toSquare)
                                            selectedSquareState.value = toSquare // Ažuriraj selektovano polje na novo
                                            highlightedSquaresState.value = updatedLegalMoves.toSet() // Ažuriraj highlightove
                                        }

                                        if (statusResult.puzzleCompleted) {
                                            SoundManager.playSound(true)
                                        } else if (statusResult.noMoreMoves) {
                                            SoundManager.playSound(false)
                                        }
                                    }
                                }
                            } else if (isPureChessValidMove && !isCaptureOfBlackPiece) {
                                // Legalan šahovski potez, ali ne hvata crnu figuru - poništi selekciju.
                                selectedSquareState.value = null
                                highlightedSquaresState.value = emptySet()
                            } else {
                                // Nelegalan potez ili klik na sopstvenu figuru koja nije meta za hvatanje.
                                selectedSquareState.value = null
                                highlightedSquaresState.value = emptySet()
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
                    currentSolutionStepState.intValue = 0

                    selectedSquareState.value = null
                    highlightedSquaresState.value = emptySet()
                    board = initialBoardBackup.copy()
                    puzzleCompleted = false
                    noMoreMoves = false
                    timeElapsedSecondsState.intValue = 0
                    gameStartedState.value = true

                    coroutineScope.launch(Dispatchers.IO) {
                        val statusResult = checkGameStatusLogic(
                            board,
                            timeElapsedSecondsState.intValue,
                            difficulty,
                            playerName,
                            currentSessionScore
                        )
                        withContext(Dispatchers.Main) {
                            blackPieces = statusResult.updatedBlackPieces
                            puzzleCompleted = statusResult.puzzleCompleted
                            noMoreMoves = statusResult.noMoreMoves
                            currentSessionScore = statusResult.newSessionScore
                            gameStartedState.value = statusResult.gameStarted
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
                    currentSolutionStepState.intValue = 0

                    selectedSquareState.value = null
                    highlightedSquaresState.value = emptySet()

                    coroutineScope.launch(Dispatchers.IO) {
                        generateAndLoadPuzzle(
                            puzzleGenerator = puzzleGenerator,
                            coroutineScope = coroutineScope,
                            applicationContext = applicationContext,
                            difficulty = difficulty,
                            selectedFigures = selectedFigures,
                            minPawns = minPawns,
                            maxPawns = maxPawns,
                            playerName = playerName,
                            isTrainingMode = isTrainingMode,
                            currentSessionScore = currentSessionScore,
                            updateBoard = { newBoard -> board = newBoard },
                            updateInitialBoardBackup = { newBoard -> initialBoardBackup = newBoard },
                            updatePuzzleCompleted = { completed -> puzzleCompleted = completed },
                            updateNoMoreMoves = { noMovesValue -> noMoreMoves = noMovesValue },
                            updateTimeElapsedSeconds = { time -> timeElapsedSecondsState.intValue = time },
                            updateSelectedSquare = { square -> selectedSquareState.value = square },
                            updateHighlightedSquares = { squares -> highlightedSquaresState.value = squares },
                            updateCurrentSessionScore = { score -> currentSessionScore = score },
                            updateGameStarted = { started -> gameStartedState.value = started },
                            updateIsSolutionDisplaying = { displaying -> isSolutionDisplaying = displaying },
                            updateSolverSolutionPath = { path -> solverSolutionPath = path },
                            updateCurrentSolutionStep = { step -> currentSolutionStepState.intValue = step },
                            updateBlackPieces = { pieces -> blackPieces = pieces },
                            updateSolvedPuzzlesCount = { count -> solvedPuzzlesCount = count }
                        )
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

        if (isTrainingMode && gameStartedState.value && !puzzleCompleted && !noMoreMoves) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSolutionDisplaying = true
                        currentSolutionStepState.intValue = 0
                        solverSolutionPath = null
                        highlightedSquaresState.value = emptySet()

                        Log.d("GameActivity", "Pokrećem Solver za FEN: ${board.toFEN()}")
                        val solution = chessSolver.solve(board)

                        withContext(Dispatchers.Main) {
                            if (solution != null && solution.isNotEmpty()) {
                                Log.d(
                                    "GameActivity",
                                    "Solver pronašao rešenje: ${solution.map { it.fromSquare.toString() + "-" + it.toSquare.toString() }}"
                                )
                                solverSolutionPath = solution
                                val firstMove = solution.first()
                                highlightedSquaresState.value = setOf(firstMove.fromSquare, firstMove.toSquare)
                            } else {
                                Log.d(
                                    "GameActivity",
                                    "Solver nije pronašao rešenje za trenutnu poziciju."
                                )
                                solverSolutionPath = null
                                highlightedSquaresState.value = emptySet()
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

            if (isSolutionDisplaying && solverSolutionPath != null && solverSolutionPath!!.size > currentSolutionStepState.intValue) {
                Button(
                    onClick = {
                        val nextStep = currentSolutionStepState.intValue + 1
                        if (nextStep < solverSolutionPath!!.size) {
                            currentSolutionStepState.intValue = nextStep
                            val nextMove = solverSolutionPath!![currentSolutionStepState.intValue]
                            highlightedSquaresState.value = setOf(nextMove.fromSquare, nextMove.toSquare)
                        } else {
                            isSolutionDisplaying = false
                            solverSolutionPath = null
                            currentSolutionStepState.intValue = 0
                            highlightedSquaresState.value = emptySet()
                            selectedSquareState.value = null
                            Log.d("GameActivity", "Kraj rešenja Solvera. Vraćam na igru.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(if (currentSolutionStepState.intValue < solverSolutionPath!!.size - 1) "Sledeći potez rešenja" else "Završi prikaz rešenja")
                }
            } else if (isSolutionDisplaying && (solverSolutionPath == null || solverSolutionPath!!.isEmpty())) {
                Button(
                    onClick = {
                        isSolutionDisplaying = false
                        solverSolutionPath = null
                        currentSolutionStepState.intValue = 0
                        highlightedSquaresState.value = emptySet()
                        selectedSquareState.value = null
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
    }
}

suspend fun generateAndLoadPuzzle(
    puzzleGenerator: PositionGenerator,
    coroutineScope: CoroutineScope,
    applicationContext: Context?,
    difficulty: Difficulty,
    selectedFigures: List<PieceType>,
    minPawns: Int,
    maxPawns: Int,
    playerName: String,
    isTrainingMode: Boolean,
    currentSessionScore: Int,
    updateBoard: (ChessBoard) -> Unit,
    updateInitialBoardBackup: (ChessBoard) -> Unit,
    updatePuzzleCompleted: (Boolean) -> Unit,
    updateNoMoreMoves: (Boolean) -> Unit,
    updateTimeElapsedSeconds: (Int) -> Unit,
    updateSelectedSquare: (Square?) -> Unit,
    updateHighlightedSquares: (Set<Square>) -> Unit,
    updateCurrentSessionScore: (Int) -> Unit,
    updateGameStarted: (Boolean) -> Unit,
    updateIsSolutionDisplaying: (Boolean) -> Unit,
    updateSolverSolutionPath: (List<ChessSolver.MoveData>?) -> Unit,
    updateCurrentSolutionStep: (Int) -> Unit,
    updateBlackPieces: (Map<Square, Piece>) -> Unit,
    updateSolvedPuzzlesCount: (Int) -> Unit
) {
    withContext(Dispatchers.IO) {
        var newPuzzleFen: String? = null
        if (isTrainingMode) {
            if (applicationContext == null) {
                Log.e("GameActivity", "ApplicationContext is null for training puzzle generation!")
            } else {
                val whitePieceType = selectedFigures.randomOrNull() ?: PieceType.QUEEN
                val numberOfBlackPieces = Random.nextInt(minPawns, maxPawns + 1)

                Log.d("generateAndLoadPuzzle", "Generišem trening zagonetku: figura: $whitePieceType, broj crnih: $numberOfBlackPieces")
                newPuzzleFen = puzzleGenerator.generate(whitePieceType, numberOfBlackPieces)
            }
        } else {
            if (applicationContext == null) {
                Log.e("GameActivity", "ApplicationContext je null pri učitavanju takmičarske zagonetke!")
            } else {
                Log.w("generateAndLoadPuzzle", "Takmičarski mod za sada generiše nasumičnu zagonetku jer nije implementirano učitavanje iz JSON-a unutar PositionGeneratora.")
                val defaultWhitePieceType = when (difficulty) {
                    Difficulty.EASY -> PieceType.KNIGHT
                    Difficulty.MEDIUM -> PieceType.ROOK
                    Difficulty.HARD -> PieceType.QUEEN
                }
                val defaultNumBlackPieces = when (difficulty) {
                    Difficulty.EASY -> 4
                    Difficulty.MEDIUM -> 7
                    Difficulty.HARD -> 10
                }
                newPuzzleFen = puzzleGenerator.generate(defaultWhitePieceType, defaultNumBlackPieces)
            }
        }

        withContext(Dispatchers.Main) {
            if (newPuzzleFen != null) {
                val newPuzzleBoard = ChessBoard.parseFenToBoard(newPuzzleFen)
                if (newPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).isNotEmpty()) {
                    updateBoard(newPuzzleBoard)
                    updateInitialBoardBackup(newPuzzleBoard.copy())
                    updatePuzzleCompleted(false)
                    updateNoMoreMoves(false)
                    updateTimeElapsedSeconds(0)
                    updateSelectedSquare(null)
                    updateHighlightedSquares(emptySet())

                    updateGameStarted(true)

                    updateIsSolutionDisplaying(false)
                    updateSolverSolutionPath(null)
                    updateCurrentSolutionStep(0)

                    val statusResult = checkGameStatusLogic(
                        newPuzzleBoard,
                        0,
                        difficulty,
                        playerName,
                        currentSessionScore
                    )
                    updateBlackPieces(statusResult.updatedBlackPieces)
                    updatePuzzleCompleted(statusResult.puzzleCompleted)
                    updateNoMoreMoves(statusResult.noMoreMoves)
                    updateSolvedPuzzlesCount(statusResult.solvedPuzzlesCountIncrement)
                    updateCurrentSessionScore(statusResult.newSessionScore)
                    updateGameStarted(statusResult.gameStarted)

                    Log.d("PUZZLE_BOARD_STATE", "Loaded/Generated FEN: ${newPuzzleFen}")
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
                    }

                } else {
                    Log.e(
                        "GameActivity",
                        "Nije moguće generisati/učitati zagonetku, generisana tabla nema belu figuru. Pokušajte ponovo ili promenite postavke."
                    )
                    updateGameStarted(false)
                    SoundManager.playSound(false)
                }
            } else {
                Log.e(
                    "GameActivity",
                    "Nije moguće generisati/učitati zagonetku, FEN je null. Pokušajte ponovo ili promenite postavke."
                )
                updateGameStarted(false)
                SoundManager.playSound(false)
            }
        }
    }
}


@Preview(showBackground = true)
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