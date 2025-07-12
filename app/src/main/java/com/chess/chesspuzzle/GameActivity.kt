package com.chess.chesspuzzle

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import com.chess.chesspuzzle.logic.checkGameStatusLogic
import com.chess.chesspuzzle.logic.performMove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

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

        // Dohvatanje liste izabranih figura (samo za trening mod ako je custom odabir)
        // U takmičarskom modu, ovo će biti prazna lista, figure se biraju po težini
        val selectedFiguresNames = intent.getStringArrayListExtra("selectedFigures") ?: arrayListOf()
        val selectedFigures = selectedFiguresNames.mapNotNull {
            try {
                PieceType.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                Log.e("GameActivity", "Nepoznat PieceType: $it", e)
                null
            }
        }

        // Dohvatanje minPawns i maxPawns
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
                        selectedFigures = selectedFigures, // Prosleđujemo listu
                        minPawns = minPawns, // Prosleđujemo minPawns
                        maxPawns = maxPawns, // Prosleđujemo maxPawns
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
    selectedFigures: List<PieceType>, // Sada prihvatamo listu
    minPawns: Int, // Prihvatamo minPawns
    maxPawns: Int, // Prihvatamo maxPawns
    playerName: String,
    isTrainingMode: Boolean,
    applicationContext: Context?
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current // Dohvatite kontekst za Toast

    val chessSolver = remember { ChessSolver() }
    val puzzleGenerator = remember { PositionGenerator(chessSolver) }

    var board: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    var initialBoardBackup: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }

    var blackPieces: Map<Square, Piece> by remember { mutableStateOf(emptyMap()) }

    var puzzleCompleted: Boolean by remember { mutableStateOf(false) }
    var noMoreMoves: Boolean by remember { mutableStateOf(false) }

    val timeElapsedSecondsState = remember { mutableIntStateOf(0) }

    val gameStartedState = remember { mutableStateOf(false) }

    var solvedPuzzlesCount by rememberSaveable { mutableIntStateOf(0) }
    var currentSessionScore by rememberSaveable { mutableIntStateOf(0) }

    val selectedSquareState = remember { mutableStateOf<Square?>(null) }
    val highlightedSquaresState = remember { mutableStateOf<Set<Square>>(emptySet()) }

    var isSolutionDisplaying by remember { mutableStateOf(false) }
    var solverSolutionPath: List<ChessSolver.MoveData>? by remember { mutableStateOf(null) }
    val currentSolutionStepState = remember { mutableIntStateOf(0) }

    LaunchedEffect(gameStartedState.value) {
        var isTimerActive = gameStartedState.value
        while (isTimerActive) {
            delay(1000L)
            timeElapsedSecondsState.intValue++
            isTimerActive = gameStartedState.value
        }
    }

    // Poziv generateAndLoadPuzzle kada se Composable prvi put učita
    LaunchedEffect(Unit) {
        generateAndLoadPuzzle(
            puzzleGenerator = puzzleGenerator,
            coroutineScope = coroutineScope,
            applicationContext = applicationContext,
            difficulty = difficulty,
            selectedFiguresFromActivity = selectedFigures, // PROMENJENO: Prosleđujemo listu sa aktivnosti
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
            updateSolvedPuzzlesCount = { count -> solvedPuzzlesCount = count },
            context = context
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
                    // Ne dozvoli interakciju ako je zagonetka rešena, nema više poteza,
                    // igra nije počela ili se prikazuje rešenje.
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
                                            val updatedLegalMoves = ChessCore.getValidMoves(board, board.getPiece(toSquare), toSquare)
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
                    // Resetuj stanje prikaza rešenja
                    isSolutionDisplaying = false
                    solverSolutionPath = null
                    currentSolutionStepState.intValue = 0

                    selectedSquareState.value = null
                    highlightedSquaresState.value = emptySet()
                    board = initialBoardBackup.copy() // Vrati na originalnu poziciju
                    puzzleCompleted = false
                    noMoreMoves = false
                    timeElapsedSecondsState.intValue = 0
                    gameStartedState.value = true // Pokreni tajmer ponovo

                    // Proveri status igre na resetovanoj tabli
                    coroutineScope.launch(Dispatchers.IO) {
                        val statusResult = checkGameStatusLogic(
                            board,
                            currentTimeElapsed = 0,
                            currentDifficulty = difficulty,
                            playerName = playerName,
                            currentSessionScore = currentSessionScore
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
                    // Resetuj stanje prikaza rešenja
                    isSolutionDisplaying = false
                    solverSolutionPath = null
                    currentSolutionStepState.intValue = 0

                    selectedSquareState.value = null
                    highlightedSquaresState.value = emptySet()

                    // Generisanje nove zagonetke
                    coroutineScope.launch(Dispatchers.IO) {
                        val result = generateAndLoadPuzzle(
                            puzzleGenerator = puzzleGenerator,
                            coroutineScope = this,
                            applicationContext = applicationContext,
                            difficulty = difficulty,
                            selectedFiguresFromActivity = selectedFigures, // PROMENJENO: Prosleđujemo listu sa aktivnosti
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
                            updateSolvedPuzzlesCount = { count -> solvedPuzzlesCount = count },
                            context = context
                        )

                        withContext(Dispatchers.Main) {
                            currentSessionScore = result.newSessionScore
                            gameStartedState.value = result.gameStartedAfterGeneration
                            if (!result.success) {
                                Log.e("GameActivity", "Neuspešno generisanje nove pozicije.")
                                Toast.makeText(context, "Nije moguće generisati novu zagonetku. Pokušajte ponovo.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text("Nova zagonetka")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Prikaz dugmeta "Pomoć (Prikaži rešenje)" i "Sledeći potez rešenja"
        if (isTrainingMode && gameStartedState.value && !puzzleCompleted && !noMoreMoves) {
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        // Samo ako već ne prikazujemo rešenje i ako igra nije gotova
                        if (!isSolutionDisplaying && !puzzleCompleted && !noMoreMoves) {
                            val solution = chessSolver.solve(board) // Koristi trenutnu tablu za rešavanje
                            withContext(Dispatchers.Main) {
                                if (solution != null && solution.isNotEmpty()) {
                                    solverSolutionPath = solution
                                    isSolutionDisplaying = true
                                    currentSolutionStepState.intValue = 0
                                    selectedSquareState.value = null // Poništi selekciju igrača
                                    highlightedSquaresState.value = emptySet() // Poništi highlightove igrača

                                    // Prikaži prvi potez rešenja
                                    val firstMove = solution[0]
                                    val nextBoard = board.makeMoveAndCapture(firstMove.fromSquare, firstMove.toSquare)
                                    board = nextBoard // Ažuriraj tablu na prvi potez rešenja
                                    selectedSquareState.value = firstMove.toSquare // Selektuj figuru na novom polju
                                    // Highlightuj sledeće poteze te figure (ako ih ima)
                                    val updatedLegalMoves = ChessCore.getValidMoves(nextBoard, nextBoard.getPiece(firstMove.toSquare), firstMove.toSquare)
                                    highlightedSquaresState.value = updatedLegalMoves.toSet()
                                } else {
                                    Log.w("GameActivity", "Solver nije pronašao rešenje za ovu poziciju.")
                                    Toast.makeText(context, "Solver nije pronašao rešenje za ovu poziciju.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                enabled = !isSolutionDisplaying && !puzzleCompleted && !noMoreMoves, // Onemogući ako je rešenje već prikazano ili je igra gotova
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Pomoć (Prikaži rešenje)")
            }

            if (isSolutionDisplaying && solverSolutionPath != null && solverSolutionPath!!.isNotEmpty()) {
                Button(
                    onClick = {
                        // Napreduj kroz poteze rešenja
                        currentSolutionStepState.intValue++
                        if (currentSolutionStepState.intValue < solverSolutionPath!!.size) {
                            val nextMove = solverSolutionPath!![currentSolutionStepState.intValue]
                            // Primenite potez na tablu
                            val nextBoard = board.makeMoveAndCapture(nextMove.fromSquare, nextMove.toSquare)
                            board = nextBoard
                            selectedSquareState.value = nextMove.toSquare
                            val updatedLegalMoves = ChessCore.getValidMoves(nextBoard, nextBoard.getPiece(nextMove.toSquare), nextMove.toSquare)
                            highlightedSquaresState.value = updatedLegalMoves.toSet()
                        } else {
                            // Rešenje je završeno, resetuj prikaz
                            isSolutionDisplaying = false
                            solverSolutionPath = null
                            currentSolutionStepState.intValue = 0
                            board = initialBoardBackup.copy() // Vrati na početnu poziciju puzzle
                            selectedSquareState.value = null
                            highlightedSquaresState.value = emptySet()
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
                // Dugme za povratak ako solver nije pronašao rešenje
                Button(
                    onClick = {
                        isSolutionDisplaying = false
                        solverSolutionPath = null
                        currentSolutionStepState.intValue = 0
                        highlightedSquaresState.value = emptySet()
                        selectedSquareState.value = null
                        board = initialBoardBackup.copy() // Vrati na početnu poziciju
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

// Globalna funkcija za generisanje i učitavanje zagonetke
suspend fun generateAndLoadPuzzle(
    puzzleGenerator: PositionGenerator,
    coroutineScope: CoroutineScope,
    applicationContext: Context?,
    difficulty: Difficulty,
    selectedFiguresFromActivity: List<PieceType>, // PROMENJENO IME PARAMETRA
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
    updateSolvedPuzzlesCount: (Int) -> Unit,
    context: Context
): PuzzleGenerationResult {
    var newSessionScore = currentSessionScore
    var gameStartedAfterGeneration = false

    updatePuzzleCompleted(false)
    updateNoMoreMoves(false)
    updateTimeElapsedSeconds(0)
    updateSelectedSquare(null)
    updateHighlightedSquares(emptySet())
    updateIsSolutionDisplaying(false)
    updateSolverSolutionPath(null)
    updateCurrentSolutionStep(0)
    updateGameStarted(false) // Zaustavi tajmer dok se generiše nova pozicija

    var generatedFen: String? = null
    var penalty = 0

    if (isTrainingMode) {
        if (applicationContext == null) {
            Log.e("GameActivity", "ApplicationContext is null for training puzzle generation!")
        } else {
            val numberOfBlackPiecesToGenerate = Random.nextInt(minPawns, maxPawns + 1)
            Log.d("generateAndLoadPuzzle", "Generišem trening zagonetku: figure: ${selectedFiguresFromActivity.joinToString()}, broj crnih: ${numberOfBlackPiecesToGenerate}")

            // KLJUČNA IZMENA OVDE: Konvertovanje List<PieceType> u Map<PieceType, Int>
            val whitePiecesConfigMap: Map<PieceType, Int> = selectedFiguresFromActivity
                .groupingBy { it }
                .eachCount()
                .mapValues { it.value } // Ensure it's Map<PieceType, Int>

            if (whitePiecesConfigMap.isNotEmpty()) {
                generatedFen = puzzleGenerator.generate(
                    whitePiecesConfig = whitePiecesConfigMap, // ISPRAVLJENO: Prosleđujemo mapu!
                    numBlackPieces = numberOfBlackPiecesToGenerate,
                    maxAttempts = 5000
                )
            } else {
                Log.e("generateAndLoadPuzzle", "Nema odabranih belih figura za generisanje trening zagonetke!")
                Toast.makeText(context, "Nema odabranih figura za generisanje zagonetke. Odaberite figure u prethodnom ekranu.", Toast.LENGTH_LONG).show()
                generatedFen = null
            }
        }
    } else {
        // Logika za takmičarski mod - figure se automatski biraju po težini
        val competitionWhitePiecesConfig: Map<PieceType, Int>
        val competitionMinBlackPawns: Int
        val competitionMaxBlackPawns: Int

        // Definiši figure i range za pešake za takmičarski mod po težini
        when (difficulty) {
            Difficulty.EASY -> {
                competitionWhitePiecesConfig = mapOf(PieceType.QUEEN to 1) // Npr. 1 kraljica za laku
                competitionMinBlackPawns = 3
                competitionMaxBlackPawns = 5
            }
            Difficulty.MEDIUM -> {
                competitionWhitePiecesConfig = mapOf(PieceType.ROOK to 1, PieceType.BISHOP to 1) // Npr. 1 top i 1 lovac za srednju
                competitionMinBlackPawns = 6
                competitionMaxBlackPawns = 9
            }
            Difficulty.HARD -> {
                competitionWhitePiecesConfig = mapOf(PieceType.QUEEN to 1, PieceType.KNIGHT to 1, PieceType.BISHOP to 1) // Npr. 1 kraljica, 1 skakač, 1 lovac za tešku
                competitionMinBlackPawns = 10
                competitionMaxBlackPawns = 14
            }
        }
        val numberOfBlackPiecesToGenerate = Random.nextInt(competitionMinBlackPawns, competitionMaxBlackPawns + 1)
        Log.d("GameActivity", "Generišem takmičarsku zagonetku: težina: $difficulty, figure: $competitionWhitePiecesConfig, broj crnih: $numberOfBlackPiecesToGenerate")

        generatedFen = puzzleGenerator.generate(
            whitePiecesConfig = competitionWhitePiecesConfig,
            numBlackPieces = numberOfBlackPiecesToGenerate,
            maxAttempts = 5000
        )
    }

    val finalBoard: ChessBoard
    if (generatedFen != null) {
        finalBoard = ChessBoard.parseFenToBoard(generatedFen)
        Log.d("GameActivity", "Učitana nova pozicija: ${generatedFen}")
        updateBoard(finalBoard)
        updateInitialBoardBackup(finalBoard.copy())

        val statusResult = checkGameStatusLogic(
            finalBoard,
            currentTimeElapsed = 0,
            currentDifficulty = difficulty,
            playerName = playerName,
            currentSessionScore = newSessionScore
        )

        withContext(Dispatchers.Main) {
            updateBlackPieces(statusResult.updatedBlackPieces)
            updatePuzzleCompleted(statusResult.puzzleCompleted)
            updateNoMoreMoves(statusResult.noMoreMoves)
            updateCurrentSessionScore(statusResult.newSessionScore)
            gameStartedAfterGeneration = statusResult.gameStarted
            updateGameStarted(gameStartedAfterGeneration)
        }
        return PuzzleGenerationResult(finalBoard, penalty, true, gameStartedAfterGeneration, newSessionScore)
    } else {
        Log.e("GameActivity", "Generisanje pozicije nije uspelo! Vraćam praznu tablu i penal.")
        penalty = 100
        newSessionScore -= penalty
        if (newSessionScore < 0) newSessionScore = 0
        updateCurrentSessionScore(newSessionScore)
        updateGameStarted(false)
        return PuzzleGenerationResult(ChessBoard.createEmpty(), penalty, false, false, newSessionScore)
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