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

class GameActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicijalizacija SoundPool-a se dešava jednom prilikom kreiranja aktivnosti
        PuzzleGenerator.initializeSoundPool(applicationContext)

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

        // minPawns i maxPawns se i dalje mogu prosljeđivati, ali ih ne koristimo za učitavanje JSON zagonetki
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
                        minPawns = minPawns, // I dalje prosleđujemo za random generaciju
                        maxPawns = maxPawns, // I dalje prosleđujemo za random generaciju
                        playerName = playerName,
                        isTrainingMode = isTrainingMode,
                        applicationContext = applicationContext // Prosleđujemo applicationContext
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Otpuštanje SoundPool resursa
        PuzzleGenerator.releaseSoundPool()
    }
}

// Data class za rezultate provere statusa igre
data class GameStatusResult(
    val updatedBlackPieces: Map<Square, Piece>,
    val puzzleCompleted: Boolean,
    val noMoreMoves: Boolean,
    val solvedPuzzlesCountIncrement: Int = 0,
    val scoreForPuzzle: Int = 0,
    val gameStarted: Boolean,
    val newSessionScore: Int
)

// Data class za rezultate generisanja nove zagonetke
data class PuzzleGenerationResult(
    val newBoard: ChessBoard,
    val penaltyApplied: Int = 0,
    val success: Boolean,
    val gameStartedAfterGeneration: Boolean = false,
    val newSessionScore: Int
)

// checkGameStatusLogic sada prima sve podatke kao parametre i vraća GameStatusResult
suspend fun checkGameStatusLogic(
    currentBoardSnapshot: ChessBoard,
    currentTimeElapsed: Int,
    currentDifficulty: Difficulty,
    playerName: String,
    currentSessionScore: Int
): GameStatusResult = withContext(Dispatchers.Default) {
    val updatedBlackPiecesMap = currentBoardSnapshot.getPiecesMapFromBoard(PieceColor.BLACK)
    var solvedPuzzlesCountIncrement = 0
    var scoreForPuzzle = 0
    var newSessionScore = currentSessionScore
    var puzzleCompleted = false
    var noMoreMoves = false
    var gameStarted = true

    if (updatedBlackPiecesMap.isEmpty()) {
        puzzleCompleted = true
        solvedPuzzlesCountIncrement = 1
        scoreForPuzzle = calculateScoreInternal(currentTimeElapsed, currentDifficulty)
        newSessionScore += scoreForPuzzle

        try {
            ScoreManager.addScore(ScoreEntry(playerName, newSessionScore), currentDifficulty.name)
            Log.d("GameActivity", "Skor uspešno sačuvan (Zagonetka rešena). Trenutni skor: $newSessionScore")
        } catch (e: Exception) {
            Log.e("GameActivity", "Greška pri čuvanju skora (Zagonetka rešena): ${e.message}", e)
        }
        gameStarted = false
    } else {
        var canWhiteCaptureBlack = false
        val whitePiecesOnBoard = mutableMapOf<Square, Piece>()
        for (rankIdx in 0 until 8) {
            for (fileIdx in 0 until 8) {
                val square = Square(('a'.code + fileIdx).toChar(), rankIdx + 1)
                val piece = currentBoardSnapshot.getPiece(square)
                if (piece.color == PieceColor.WHITE && piece.type != PieceType.NONE) {
                    whitePiecesOnBoard[square] = piece
                }
            }
        }

        for ((whiteSquare, whitePiece) in whitePiecesOnBoard) {
            val legalMoves = ChessCore.getValidMoves(currentBoardSnapshot, whitePiece, whiteSquare)
            for (move in legalMoves) {
                val pieceAtTarget = currentBoardSnapshot.getPiece(move)
                if (pieceAtTarget.color == PieceColor.BLACK && pieceAtTarget.type != PieceType.NONE) {
                    canWhiteCaptureBlack = true
                    break
                }
            }
            if (canWhiteCaptureBlack) break
        }

        if (!canWhiteCaptureBlack) {
            noMoreMoves = true
            gameStarted = false

            try {
                ScoreManager.addScore(ScoreEntry(playerName, newSessionScore), currentDifficulty.name)
                Log.d("GameActivity", "Skor uspešno sačuvan (Nema više poteza). Trenutni skor: $newSessionScore")
            } catch (e: Exception) {
                Log.e("GameActivity", "Greška pri čuvanju skora (Nema više poteza): ${e.message}", e)
            }
        }
    }

    GameStatusResult(
        updatedBlackPieces = updatedBlackPiecesMap,
        puzzleCompleted = puzzleCompleted,
        noMoreMoves = noMoreMoves,
        solvedPuzzlesCountIncrement = solvedPuzzlesCountIncrement,
        scoreForPuzzle = scoreForPuzzle,
        gameStarted = gameStarted,
        newSessionScore = newSessionScore
    )
}

// generateNewPuzzleLogic sada prima sve podatke kao parametre i vraća PuzzleGenerationResult
suspend fun generateNewPuzzleLogic(
    appCtx: Context,
    difficulty: Difficulty,
    selectedFigures: List<PieceType>,
    minPawns: Int,
    maxPawns: Int,
    gameStarted: Boolean,
    playerName: String,
    currentSessionScore: Int,
    puzzleCompleted: Boolean,
    noMoreMoves: Boolean,
    isTrainingMode: Boolean
): PuzzleGenerationResult = withContext(Dispatchers.Default) {
    var penalty = 0
    var updatedSessionScore = currentSessionScore
    var gameStartedAfterGeneration = false

    if (gameStarted && !puzzleCompleted && !noMoreMoves) {
        penalty = 100
        updatedSessionScore = (currentSessionScore - penalty).coerceAtLeast(0)
        try {
            ScoreManager.addScore(ScoreEntry(playerName, updatedSessionScore), difficulty.name)
            Log.d("GameActivity", "Skor uspešno sačuvan (Zagonetka preskočena, penal -100). Trenutni skor: $updatedSessionScore")
        } catch (e: Exception) {
            Log.e("GameActivity", "Greška pri čuvanju skora (Zagonetka preskočena): ${e.message}", e)
        }
    }

    var newPuzzleBoard: ChessBoard = ChessBoard.createEmpty()
    var success = false
    try {
        if (isTrainingMode) {
            // Za trening mod i dalje koristimo minPawns i maxPawns jer se nasumične zagonetke mogu generisati sa tim kriterijumima
            when (difficulty) {
                Difficulty.EASY -> newPuzzleBoard = PuzzleGenerator.generateEasyRandomPuzzle(appCtx, selectedFigures, minPawns, maxPawns)
                Difficulty.MEDIUM -> newPuzzleBoard = PuzzleGenerator.generateMediumRandomPuzzle(appCtx, selectedFigures, minPawns, maxPawns)
                Difficulty.HARD -> newPuzzleBoard = PuzzleGenerator.generateHardRandomPuzzle(appCtx, selectedFigures, minPawns, maxPawns)
            }
        } else {
            // Za takmičarski mod, uklanjamo minPawns i maxPawns iz poziva
            when (difficulty) {
                Difficulty.EASY -> newPuzzleBoard = PuzzleGenerator.loadEasyPuzzleFromJson(appCtx)
                Difficulty.MEDIUM -> newPuzzleBoard = PuzzleGenerator.loadMediumPuzzleFromJson(appCtx)
                Difficulty.HARD -> newPuzzleBoard = PuzzleGenerator.loadHardPuzzleFromJson(appCtx)
            }
        }
        success = newPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).isNotEmpty() || newPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).isNotEmpty()
        gameStartedAfterGeneration = success
    } catch (e: Exception) {
        Log.e("GameActivity", "Greška prilikom generisanja/učitavanja zagonetke: ${e.message}", e)
        success = false
        gameStartedAfterGeneration = false
    }

    PuzzleGenerationResult(
        newBoard = newPuzzleBoard,
        penaltyApplied = penalty,
        success = success,
        gameStartedAfterGeneration = gameStartedAfterGeneration,
        newSessionScore = updatedSessionScore
    )
}

// Internal helper for score calculation, also moved outside Composable
fun calculateScoreInternal(timeInSeconds: Int, currentDifficulty: Difficulty): Int {
    val maxTimeBonusSeconds: Int
    val pointsPerSecond: Int
    val basePointsPerPuzzle: Int

    when (currentDifficulty) {
        Difficulty.EASY -> {
            maxTimeBonusSeconds = 90
            pointsPerSecond = 5
            basePointsPerPuzzle = 300
        }
        Difficulty.MEDIUM -> {
            maxTimeBonusSeconds = 60
            pointsPerSecond = 10
            basePointsPerPuzzle = 600
        }
        Difficulty.HARD -> {
            maxTimeBonusSeconds = 30
            pointsPerSecond = 20
            basePointsPerPuzzle = 1000
        }
    }
    val timePoints = (maxTimeBonusSeconds - timeInSeconds).coerceAtLeast(0) * pointsPerSecond
    return basePointsPerPuzzle + timePoints
}

@Composable
fun ChessGameScreen(
    difficulty: Difficulty,
    selectedFigures: List<PieceType>,
    minPawns: Int,
    maxPawns: Int,
    playerName: String,
    isTrainingMode: Boolean,
    applicationContext: Context
) {
    val coroutineScope = rememberCoroutineScope()

    var board: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    var initialBoardBackup: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }

    var blackPieces: Map<Square, Piece> by remember { mutableStateOf(emptyMap()) }

    var puzzleCompleted: Boolean by remember { mutableStateOf(false) }
    var noMoreMoves: Boolean by remember { mutableStateOf(false) }

    var timeElapsedSeconds by remember { mutableStateOf(0) }
    var gameStarted by remember { mutableStateOf(false) }
    var solvedPuzzlesCount by remember { mutableStateOf(0) }
    var currentSessionScore by remember { mutableStateOf(0) }

    var selectedSquare: Square? by remember { mutableStateOf(null) }
    var highlightedSquares: Set<Square> by remember { mutableStateOf(emptySet()) }

    // Tajmer za igru
    LaunchedEffect(gameStarted) {
        while (gameStarted) {
            delay(1000L)
            timeElapsedSeconds++
        }
    }

    // Pozovi prvu zagonetku kada se Composable inicijalizuje
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val result = generateNewPuzzleLogic(
                applicationContext,
                difficulty,
                selectedFigures,
                minPawns,
                maxPawns,
                gameStarted,
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
                    gameStarted = result.gameStartedAfterGeneration

                    val statusResult = checkGameStatusLogic(board, timeElapsedSeconds, difficulty, playerName, currentSessionScore)
                    blackPieces = statusResult.updatedBlackPieces
                    puzzleCompleted = statusResult.puzzleCompleted
                    noMoreMoves = statusResult.noMoreMoves
                    solvedPuzzlesCount += statusResult.solvedPuzzlesCountIncrement
                    currentSessionScore = statusResult.newSessionScore
                    gameStarted = statusResult.gameStarted

                    // --- DIAGNOSTIC LOGGING ---
                    Log.d("PUZZLE_BOARD_STATE", "Loaded/Generated FEN: ${board.toFEN()}")
                    val blackPiecesOnBoard = board.getPiecesMapFromBoard(PieceColor.BLACK)
                    Log.d("PUZZLE_BOARD_STATE", "Black pieces detected on board (count: ${blackPiecesOnBoard.size}):")
                    blackPiecesOnBoard.forEach { (square, piece) ->
                        Log.d("PUZZLE_BOARD_STATE", "  - ${piece.color} ${piece.type} at ${square.file}${square.rank}")
                    }
                    // --- END DIAGNOSTIC LOGGING ---

                    if (statusResult.gameStarted && result.success) {
                        PuzzleGenerator.playSound(true)
                    } else if (!statusResult.gameStarted && !statusResult.puzzleCompleted && !statusResult.noMoreMoves) {
                        PuzzleGenerator.playSound(false)
                    }

                } else {
                    Log.e("GameActivity", "Nije moguće generisati/učitati zagonetku, vraćena prazna tabla. Pokušajte ponovo ili promenite postavke.")
                    gameStarted = false
                    PuzzleGenerator.playSound(false)
                }
            }
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
            Text(text = "Igrač: $playerName", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(text = "Mod: ${if (isTrainingMode) "Trening" else "Takmičarski"}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(text = "Težina: ${difficulty.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Vreme: ${timeElapsedSeconds}s", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(text = "Rešeno: ${solvedPuzzlesCount}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(text = "Skor: ${currentSessionScore}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
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
                if (puzzleCompleted || noMoreMoves || !gameStarted) {
                    // Nema interakcije ako je igra završena ili nije započeta
                } else {
                    val pieceOnClickedSquare = board.getPiece(clickedSquare)

                    if (selectedSquare == null) {
                        if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                            selectedSquare = clickedSquare
                            val legalMoves = ChessCore.getValidMoves(board, pieceOnClickedSquare, clickedSquare)
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
                        }
                        else if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                            selectedSquare = clickedSquare
                            val legalMoves = ChessCore.getValidMoves(board, pieceOnClickedSquare, clickedSquare)
                            highlightedSquares = legalMoves.toSet()
                        }
                        else {
                            val legalChessMovesForSelectedPiece = ChessCore.getValidMoves(board, pieceToMove, fromSquare)
                            val isPureChessValidMove = legalChessMovesForSelectedPiece.contains(toSquare)

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
                                            gameStarted = statusResult.gameStarted

                                            if (statusResult.puzzleCompleted) {
                                                PuzzleGenerator.playSound(true)
                                            } else if (statusResult.noMoreMoves) {
                                                PuzzleGenerator.playSound(false)
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
                    selectedSquare = null
                    highlightedSquares = emptySet()
                    board = initialBoardBackup.copy()
                    puzzleCompleted = false
                    noMoreMoves = false
                    timeElapsedSeconds = 0
                    gameStarted = true

                    coroutineScope.launch(Dispatchers.IO) {
                        val statusResult = checkGameStatusLogic(board, timeElapsedSeconds, difficulty, playerName, currentSessionScore)
                        withContext(Dispatchers.Main) {
                            blackPieces = statusResult.updatedBlackPieces
                            puzzleCompleted = statusResult.puzzleCompleted
                            noMoreMoves = statusResult.noMoreMoves
                            currentSessionScore = statusResult.newSessionScore
                            gameStarted = statusResult.gameStarted
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
                    selectedSquare = null
                    highlightedSquares = emptySet()

                    coroutineScope.launch(Dispatchers.IO) {
                        val result = generateNewPuzzleLogic(
                            applicationContext,
                            difficulty,
                            selectedFigures,
                            minPawns,
                            maxPawns,
                            gameStarted,
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
                                gameStarted = result.gameStartedAfterGeneration

                                val statusResult = checkGameStatusLogic(board, timeElapsedSeconds, difficulty, playerName, currentSessionScore)
                                blackPieces = statusResult.updatedBlackPieces
                                puzzleCompleted = statusResult.puzzleCompleted
                                noMoreMoves = statusResult.noMoreMoves
                                solvedPuzzlesCount += statusResult.solvedPuzzlesCountIncrement
                                currentSessionScore = statusResult.newSessionScore
                                gameStarted = statusResult.gameStarted

                                // --- DIAGNOSTIC LOGGING ---
                                Log.d("PUZZLE_BOARD_STATE", "New Puzzle FEN: ${board.toFEN()}")
                                val blackPiecesOnBoardAfterGen = board.getPiecesMapFromBoard(PieceColor.BLACK)
                                Log.d("PUZZLE_BOARD_STATE", "Black pieces detected on new board (count: ${blackPiecesOnBoardAfterGen.size}):")
                                blackPiecesOnBoardAfterGen.forEach { (square, piece) ->
                                    Log.d("PUZZLE_BOARD_STATE", "  - ${piece.color} ${piece.type} at ${square.file}${square.rank}")
                                }
                                // --- END DIAGNOSTIC LOGGING ---

                                if (statusResult.gameStarted && result.success) {
                                    PuzzleGenerator.playSound(true)
                                } else if (!statusResult.gameStarted && !statusResult.puzzleCompleted && !statusResult.noMoreMoves) {
                                    PuzzleGenerator.playSound(false)
                                }

                            } else {
                                Log.e("GameActivity", "Nije moguće generisati/učitati zagonetku, vraćena prazna tabla. Pokušajte ponovo ili promenite postavke.")
                                gameStarted = false
                                PuzzleGenerator.playSound(false)
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
    }
}
suspend fun performMove(
    fromSquare: Square,
    toSquare: Square,
    currentBoard: ChessBoard,
    updateBoardState: (ChessBoard) -> Unit,
    checkGameStatusLogic: suspend (ChessBoard, Int, Difficulty, String, Int) -> GameStatusResult,
    currentTimeElapsed: Int,
    currentDifficulty: Difficulty,
    playerName: String,
    currentSessionScore: Int,
    capture: Boolean = false,
    targetSquare: Square? = null,
    onStatusUpdate: (GameStatusResult) -> Unit
) = withContext(Dispatchers.Default) {
    val pieceToMove = currentBoard.getPiece(fromSquare)
    if (pieceToMove.type == PieceType.NONE) {
        Log.e("performMove", "Attempted to move a non-existent piece from $fromSquare")
        return@withContext
    }
    var newBoard = currentBoard.removePiece(fromSquare)
    if (capture && targetSquare != null) {
        newBoard = newBoard.removePiece(targetSquare)
    }
    newBoard = newBoard.setPiece(toSquare, pieceToMove)

    withContext(Dispatchers.Main) {
        updateBoardState(newBoard)
    }
    val statusResult = checkGameStatusLogic(newBoard, currentTimeElapsed, currentDifficulty, playerName, currentSessionScore)

    withContext(Dispatchers.Main) {
        onStatusUpdate(statusResult)
    }
}
@Composable
fun DefaultPreview() {
    ChessPuzzleTheme {
        ChessGameScreen(
            difficulty = Difficulty.MEDIUM,
            selectedFigures = listOf(PieceType.QUEEN, PieceType.KNIGHT),
            minPawns = 6,
            maxPawns = 9,
            playerName = "Preview Igrač",
            isTrainingMode = true,
            applicationContext = LocalContext.current.applicationContext // Prosleđujemo context za Preview
        )
    }
}