package com.chess.chesspuzzle.modul1

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
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.ScoreEntry
import com.chess.chesspuzzle.ScoreManager
import com.chess.chesspuzzle.SoundManager
import com.chess.chesspuzzle.Square
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import com.chess.chesspuzzle.modul1.logic.checkGameStatusLogic
import com.chess.chesspuzzle.modul1.logic.performMove
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
        ScoreManager.init(applicationContext) // Inicijalizujemo ScoreManager

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
    val newSessionScore: Int // Ovo je akumulirani skor sesije
)

data class PuzzleGenerationResult(
    val newBoard: ChessBoard,
    val penaltyApplied: Int = 0,
    val success: Boolean,
    val gameStartedAfterGeneration: Boolean = false,
    val newSessionScore: Int // Akumulirani skor sesije nakon generisanja
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

    val timeElapsedSecondsState = remember { mutableIntStateOf(0) }

    val gameStartedState = remember { mutableStateOf(false) }

    var solvedPuzzlesCount by rememberSaveable { mutableIntStateOf(0) }
    var currentSessionScore by rememberSaveable { mutableIntStateOf(0) } // Akumulirani skor sesije
    var puzzlesInSessionCount by rememberSaveable { mutableIntStateOf(0) } // Brojač zagonetki u sesiji

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

    LaunchedEffect(Unit) {
        val result = generateAndLoadPuzzle(
            puzzleGenerator = puzzleGenerator,
            coroutineScope = coroutineScope,
            applicationContext = applicationContext,
            difficulty = difficulty,
            selectedFiguresFromActivity = selectedFigures,
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
        // Ažuriraj stanje nakon inicijalnog generisanja
        currentSessionScore = result.newSessionScore
        gameStartedState.value = result.gameStartedAfterGeneration
        if (!result.success) {
            Log.e("GameActivity", "Neuspešno generisanje početne pozicije.")
            Toast.makeText(context, "Nije moguće generisati početnu zagonetku. Pokušajte ponovo.", Toast.LENGTH_LONG).show()
        }
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
            // Izmenjen prikaz broja rešenih zagonetki/sesije
            Text(
                text = if (isTrainingMode) "Rešeno: ${solvedPuzzlesCount}" else "Zagonetka: ${puzzlesInSessionCount}/10",
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

                                        if (statusResult.puzzleCompleted || statusResult.noMoreMoves) {
                                            if (!isTrainingMode) { // Takmičarski mod
                                                puzzlesInSessionCount++
                                                Log.d("GameActivity", "Zagonetka ${puzzlesInSessionCount}/10 završena u takmičarskom modu. Akumulirani skor: $currentSessionScore")

                                                if (puzzlesInSessionCount >= 10) { // Sesija završena (10 zagonetki)
                                                    ScoreManager.addScore(ScoreEntry(playerName, currentSessionScore), difficulty.name)
                                                    Log.d("GameActivity", "Takmičarska sesija od 10 zagonetki završena! Ukupan skor: $currentSessionScore je sačuvan.")
                                                    Toast.makeText(context, "Sesija od 10 zagonetki završena! Vaš skor: $currentSessionScore je sačuvan.", Toast.LENGTH_LONG).show()

                                                    puzzlesInSessionCount = 0
                                                    currentSessionScore = 0
                                                    solvedPuzzlesCount = 0

                                                    val genResult = generateAndLoadPuzzle(
                                                        puzzleGenerator = puzzleGenerator,
                                                        coroutineScope = coroutineScope,
                                                        applicationContext = applicationContext,
                                                        difficulty = difficulty,
                                                        selectedFiguresFromActivity = selectedFigures,
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
                                                    gameStartedState.value = genResult.gameStartedAfterGeneration

                                                } else { // Sesija još uvek traje, generiši sledeću zagonetku
                                                    Toast.makeText(context, "Rešena zagonetka ${puzzlesInSessionCount}/10. Nastavite!", Toast.LENGTH_SHORT).show()
                                                    val genResult = generateAndLoadPuzzle(
                                                        puzzleGenerator = puzzleGenerator,
                                                        coroutineScope = coroutineScope,
                                                        applicationContext = applicationContext,
                                                        difficulty = difficulty,
                                                        selectedFiguresFromActivity = selectedFigures,
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
                                                    gameStartedState.value = genResult.gameStartedAfterGeneration
                                                }
                                            } else { // Trening mod
                                                Toast.makeText(context, "Zagonetka rešena! Nastavite sa treningom.", Toast.LENGTH_SHORT).show()
                                                val genResult = generateAndLoadPuzzle(
                                                    puzzleGenerator = puzzleGenerator,
                                                    coroutineScope = coroutineScope,
                                                    applicationContext = applicationContext,
                                                    difficulty = difficulty,
                                                    selectedFiguresFromActivity = selectedFigures,
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
                                                gameStartedState.value = genResult.gameStartedAfterGeneration
                                            }
                                            selectedSquareState.value = null
                                            highlightedSquaresState.value = emptySet()

                                            if (statusResult.puzzleCompleted) {
                                                SoundManager.playSound(true)
                                            } else if (statusResult.noMoreMoves) {
                                                SoundManager.playSound(false)
                                            }
                                        } else {
                                            val updatedLegalMoves = ChessCore.getValidMoves(board, board.getPiece(toSquare), toSquare)
                                            selectedSquareState.value = toSquare
                                            highlightedSquaresState.value = updatedLegalMoves.toSet()
                                        }
                                    }
                                }
                            } else if (isPureChessValidMove && !isCaptureOfBlackPiece) {
                                selectedSquareState.value = null
                                highlightedSquaresState.value = emptySet()
                            } else {
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
                            // Ensure game starts after reset
                            gameStartedState.value = true // Force start after reset
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
                        val result = generateAndLoadPuzzle(
                            puzzleGenerator = puzzleGenerator,
                            coroutineScope = this,
                            applicationContext = applicationContext,
                            difficulty = difficulty,
                            selectedFiguresFromActivity = selectedFigures,
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
                            // OVO JE KLJUČNO: gameStartedState.value će se ažurirati na osnovu rezultata generisanja
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

        if (isTrainingMode && gameStartedState.value && !puzzleCompleted && !noMoreMoves) {
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        if (!isSolutionDisplaying && !puzzleCompleted && !noMoreMoves) {
                            val solution = chessSolver.solve(board)
                            withContext(Dispatchers.Main) {
                                if (solution != null && solution.isNotEmpty()) {
                                    solverSolutionPath = solution
                                    isSolutionDisplaying = true
                                    currentSolutionStepState.intValue = 0
                                    selectedSquareState.value = null
                                    highlightedSquaresState.value = emptySet()

                                    val firstMove = solution[0]
                                    val nextBoard = board.makeMoveAndCapture(firstMove.fromSquare, firstMove.toSquare)
                                    board = nextBoard
                                    selectedSquareState.value = firstMove.toSquare
                                    val updatedLegalMoves = ChessCore.getValidMoves(
                                        nextBoard,
                                        nextBoard.getPiece(firstMove.toSquare),
                                        firstMove.toSquare
                                    )
                                    highlightedSquaresState.value = updatedLegalMoves.toSet()
                                } else {
                                    Log.w("GameActivity", "Solver nije pronašao rešenje za ovu poziciju.")
                                    Toast.makeText(context, "Solver nije pronašao rešenje za ovu poziciju.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                },
                enabled = !isSolutionDisplaying && !puzzleCompleted && !noMoreMoves,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Pomoć (Prikaži rešenje)")
            }

            if (isSolutionDisplaying && solverSolutionPath != null && solverSolutionPath!!.isNotEmpty()) {
                Button(
                    onClick = {
                        currentSolutionStepState.intValue++
                        if (currentSolutionStepState.intValue < solverSolutionPath!!.size) {
                            val nextMove = solverSolutionPath!![currentSolutionStepState.intValue]
                            val nextBoard = board.makeMoveAndCapture(nextMove.fromSquare, nextMove.toSquare)
                            board = nextBoard
                            selectedSquareState.value = nextMove.toSquare
                            val updatedLegalMoves = ChessCore.getValidMoves(
                                nextBoard,
                                nextBoard.getPiece(nextMove.toSquare),
                                nextMove.toSquare
                            )
                            highlightedSquaresState.value = updatedLegalMoves.toSet()
                        } else {
                            isSolutionDisplaying = false
                            solverSolutionPath = null
                            currentSolutionStepState.intValue = 0
                            board = initialBoardBackup.copy()
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
                Button(
                    onClick = {
                        isSolutionDisplaying = false
                        solverSolutionPath = null
                        currentSolutionStepState.intValue = 0
                        highlightedSquaresState.value = emptySet()
                        selectedSquareState.value = null
                        board = initialBoardBackup.copy()
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
    selectedFiguresFromActivity: List<PieceType>,
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
    var newSessionScoreForReturn = currentSessionScore
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

            val whitePiecesConfigMap: Map<PieceType, Int> = selectedFiguresFromActivity
                .groupingBy { it }
                .eachCount()
                .mapValues { it.value }

            if (whitePiecesConfigMap.isNotEmpty()) {
                generatedFen = puzzleGenerator.generate(
                    whitePiecesConfig = whitePiecesConfigMap,
                    numBlackPieces = numberOfBlackPiecesToGenerate,
                    maxAttempts = 5000
                )
            } else {
                Log.e("generateAndLoadPuzzle", "Nema odabranih belih figura za generisanje trening zagonetke!")
                Toast.makeText(context, "Nije moguće generisati novu zagonetku bez odabranih figura. Odaberite figure u prethodnom ekranu.", Toast.LENGTH_LONG).show()
                generatedFen = null
            }
        }
    } else {
        val competitionWhitePiecesConfig: Map<PieceType, Int>
        val competitionMinBlackPawns: Int
        val competitionMaxBlackPawns: Int

        when (difficulty) {
            Difficulty.EASY -> {
                competitionWhitePiecesConfig = mapOf(PieceType.QUEEN to 1)
                competitionMinBlackPawns = 3
                competitionMaxBlackPawns = 5
            }
            Difficulty.MEDIUM -> {
                competitionWhitePiecesConfig = mapOf(PieceType.ROOK to 1, PieceType.BISHOP to 1)
                competitionMinBlackPawns = 6
                competitionMaxBlackPawns = 9
            }
            Difficulty.HARD -> {
                competitionWhitePiecesConfig = mapOf(PieceType.QUEEN to 1, PieceType.KNIGHT to 1, PieceType.BISHOP to 1)
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

        // KLJUČNA IZMENA: Ako je generisanje uspešno, nova igra UVEK počinje.
        gameStartedAfterGeneration = true

        val statusResult = checkGameStatusLogic(
            finalBoard,
            currentTimeElapsed = 0,
            currentDifficulty = difficulty,
            playerName = playerName,
            currentSessionScore = newSessionScoreForReturn
        )

        withContext(Dispatchers.Main) {
            updateBlackPieces(statusResult.updatedBlackPieces)
            updatePuzzleCompleted(statusResult.puzzleCompleted)
            updateNoMoreMoves(statusResult.noMoreMoves)
            updateCurrentSessionScore(statusResult.newSessionScore)
            // Sada gameStartedState.value dolazi od gameStartedAfterGeneration, ne od statusResult.gameStarted
            updateGameStarted(gameStartedAfterGeneration)
        }
        return PuzzleGenerationResult(finalBoard, penalty, true, gameStartedAfterGeneration, newSessionScoreForReturn)
    } else {
        Log.e("GameActivity", "Generisanje pozicije nije uspelo! Vraćam praznu tablu i penal.")
        penalty = 100
        if (!isTrainingMode) {
            newSessionScoreForReturn -= penalty
            if (newSessionScoreForReturn < 0) newSessionScoreForReturn = 0
        }
        updateCurrentSessionScore(newSessionScoreForReturn)
        updateGameStarted(false) // Ako generisanje ne uspe, igra ostaje zaustavljena
        return PuzzleGenerationResult(ChessBoard.createEmpty(), penalty, false, false, newSessionScoreForReturn)
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