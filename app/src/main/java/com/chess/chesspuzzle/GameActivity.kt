package com.chess.chesspuzzle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
    val newSessionScore: Int // Dodajemo novi skor sesije
)

// Data class za rezultate generisanja nove zagonetke
data class PuzzleGenerationResult(
    val newBoard: ChessBoard,
    val penaltyApplied: Int = 0,
    val success: Boolean,
    val gameStartedAfterGeneration: Boolean = false,
    val newSessionScore: Int // Dodajemo novi skor sesije
)

// checkGameStatusLogic sada prima sve podatke kao parametre i vraća GameStatusResult
suspend fun checkGameStatusLogic(
    currentBoardSnapshot: ChessBoard,
    currentTimeElapsed: Int,
    currentDifficulty: Difficulty,
    playerName: String,
    currentSessionScore: Int // Pass currentSessionScore explicitly
): GameStatusResult = withContext(Dispatchers.Default) { // Run this logic on Default dispatcher
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
        newSessionScore += scoreForPuzzle // Dodaj osvojen skor na trenutni skor sesije

        try {
            // Ensure ScoreManager.addScore is also thread-safe or runs on appropriate dispatcher if it writes to disk
            ScoreManager.addScore(ScoreEntry(playerName, newSessionScore), currentDifficulty.name)
            Log.d("GameActivity", "Skor uspešno sačuvan (Zagonetka rešena). Trenutni skor: $newSessionScore")
        } catch (e: Exception) {
            Log.e("GameActivity", "Greška pri čuvanju skora (Zagonetka rešena): ${e.message}", e)
        }
        gameStarted = false // Zagonetka je završena, igra prestaje
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
            gameStarted = false // Nema više poteza, igra prestaje

            // Skor se ne menja, ali se beleži trenutni skor sesije
            try {
                // Ensure ScoreManager.addScore is also thread-safe or runs on appropriate dispatcher if it writes to disk
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
    appCtx: Context, // Context je potreban za PuzzleGenerator.load... i generate...
    difficulty: Difficulty,
    selectedFigures: List<PieceType>,
    minPawns: Int,
    maxPawns: Int,
    gameStarted: Boolean,
    playerName: String,
    currentSessionScore: Int,
    puzzleCompleted: Boolean,
    noMoreMoves: Boolean,
    isTrainingMode: Boolean // Add isTrainingMode
): PuzzleGenerationResult = withContext(Dispatchers.Default) { // Run this logic on Default dispatcher
    var penalty = 0
    var updatedSessionScore = currentSessionScore
    var gameStartedAfterGeneration = false

    if (gameStarted && !puzzleCompleted && !noMoreMoves) { // Ako je igra u toku i preskačemo zagonetku
        penalty = 100
        updatedSessionScore = (currentSessionScore - penalty).coerceAtLeast(0)
        try {
            // Ensure ScoreManager.addScore is also thread-safe or runs on appropriate dispatcher if it writes to disk
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
            when (difficulty) {
                Difficulty.EASY -> newPuzzleBoard = PuzzleGenerator.generateEasyRandomPuzzle(appCtx, selectedFigures, minPawns, maxPawns)
                Difficulty.MEDIUM -> newPuzzleBoard = PuzzleGenerator.generateMediumRandomPuzzle(appCtx, selectedFigures, minPawns, maxPawns)
                Difficulty.HARD -> newPuzzleBoard = PuzzleGenerator.generateHardRandomPuzzle(appCtx, selectedFigures, minPawns, maxPawns)
            }
        } else {
            when (difficulty) {
                Difficulty.EASY -> newPuzzleBoard = PuzzleGenerator.loadEasyPuzzleFromJson(appCtx, minPawns, maxPawns)
                Difficulty.MEDIUM -> newPuzzleBoard = PuzzleGenerator.loadMediumPuzzleFromJson(appCtx, minPawns, maxPawns)
                Difficulty.HARD -> newPuzzleBoard = PuzzleGenerator.loadHardPuzzleFromJson(appCtx, minPawns, maxPawns)
            }
        }
        success = newPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).isNotEmpty() || newPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).isNotEmpty()
        gameStartedAfterGeneration = success // Igra počinje ako je generisanje uspešno
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
        newSessionScore = updatedSessionScore // Prosleđujemo ažurirani skor
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
    applicationContext: Context // applicationContext je neophodan za logiku van Composable-a
) {
    val coroutineScope = rememberCoroutineScope() // Get a CoroutineScope bound to this Composable's lifecycle

    var board: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    var initialBoardBackup: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }

    var blackPieces: Map<Square, Piece> by remember { mutableStateOf(emptyMap()) }

    var puzzleCompleted: Boolean by remember { mutableStateOf(false) }
    var noMoreMoves: Boolean by remember { mutableStateOf(false) }

    var timeElapsedSeconds by remember { mutableStateOf(0) }
    var gameStarted by remember { mutableStateOf(false) }
    var solvedPuzzlesCount by remember { mutableStateOf(0) }
    var currentSessionScore by remember { mutableStateOf(0) } // Inicijalizovan ovde

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
    LaunchedEffect(Unit) { // Use Unit as key to run only once
        coroutineScope.launch(Dispatchers.IO) { // Launch on IO dispatcher for heavy operations
            val result = generateNewPuzzleLogic(
                applicationContext, // Prosleđujemo applicationContext
                difficulty,
                selectedFigures,
                minPawns,
                maxPawns,
                gameStarted, // Biće false na prvom pokretanju
                playerName,
                currentSessionScore,
                puzzleCompleted,
                noMoreMoves,
                isTrainingMode
            )
            withContext(Dispatchers.Main) { // Ažuriranje UI stanja na glavnoj niti
                if (result.success) {
                    board = result.newBoard
                    initialBoardBackup = result.newBoard.copy()
                    puzzleCompleted = false
                    noMoreMoves = false
                    timeElapsedSeconds = 0
                    selectedSquare = null
                    highlightedSquares = emptySet()
                    currentSessionScore = result.newSessionScore // Ažuriramo skor sa rezultatom generisanja
                    gameStarted = result.gameStartedAfterGeneration

                    // Proveri status odmah nakon generisanja (za slučaj da je generisana prazna tabla npr.)
                    val statusResult = checkGameStatusLogic(board, timeElapsedSeconds, difficulty, playerName, currentSessionScore)
                    blackPieces = statusResult.updatedBlackPieces
                    puzzleCompleted = statusResult.puzzleCompleted
                    noMoreMoves = statusResult.noMoreMoves
                    solvedPuzzlesCount += statusResult.solvedPuzzlesCountIncrement
                    currentSessionScore = statusResult.newSessionScore // Ažuriramo skor nakon provere statusa
                    gameStarted = statusResult.gameStarted

                    // Pusti zvuk uspeha samo ako je zagonetka uspešno generisana/učitana i igra počela
                    if (statusResult.gameStarted && result.success) {
                        PuzzleGenerator.playSound(true)
                    } else if (!statusResult.gameStarted && !statusResult.puzzleCompleted && !statusResult.noMoreMoves) {
                        // Nije uspelo generisanje, ili je odmah rešeno/bez poteza
                        PuzzleGenerator.playSound(false)
                    }

                } else {
                    Log.e("GameActivity", "Nije moguće generisati/učitati zagonetku, vraćena prazna tabla. Pokušajte ponovo ili promenite postavke.")
                    gameStarted = false
                    // Pusti zvuk neuspeha ako generisanje/učitavanje nije uspelo
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
                // Ako je zagonetka završena, jednostavno ne radimo ništa dalje
                if (puzzleCompleted || noMoreMoves || !gameStarted) { // Dodato !gameStarted zaustavlja interakciju
                    // Nema return-a ovde, jednostavno se lambda završi
                    // i ne izvršava se ostatak koda unutar nje.
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

                        // Ako je isto polje kliknuto, resetuj izbor i ne radi ništa više
                        if (fromSquare == toSquare) {
                            selectedSquare = null
                            // Nema return-a, logika se jednostavno završava ovde za ovaj uslov.
                        }
                        // Ako je kliknuto belo polje sa figurom, tretiraj to kao novi odabir
                        else if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                            selectedSquare = clickedSquare
                            val legalMoves = ChessCore.getValidMoves(board, pieceOnClickedSquare, clickedSquare)
                            highlightedSquares = legalMoves.toSet()
                            // Nema return-a, logika se završava ovde za ovaj uslov.
                        }
                        // U suprotnom, pokušaj da izvršiš potez
                        else {
                            val legalChessMovesForSelectedPiece = ChessCore.getValidMoves(board, pieceToMove, fromSquare)
                            val isPureChessValidMove = legalChessMovesForSelectedPiece.contains(toSquare)

                            val pieceAtTarget = board.getPiece(toSquare)
                            val isCaptureOfBlackPiece = pieceAtTarget.type != PieceType.NONE &&
                                    pieceAtTarget.color == PieceColor.BLACK &&
                                    pieceToMove.color != pieceAtTarget.color

                            val isPuzzleValidMove = isPureChessValidMove && isCaptureOfBlackPiece

                            if (isPuzzleValidMove) {
                                coroutineScope.launch { // Use the Composable's coroutineScope
                                    performMove(
                                        fromSquare,
                                        toSquare,
                                        board,
                                        updateBoardState = { newBoard -> board = newBoard },
                                        checkGameStatusLogic = ::checkGameStatusLogic, // Referenca na top-level funkciju
                                        currentTimeElapsed = timeElapsedSeconds,
                                        currentDifficulty = difficulty,
                                        playerName = playerName,
                                        currentSessionScore = currentSessionScore,
                                        capture = true,
                                        targetSquare = toSquare
                                    ) { statusResult ->
                                        // Callback za ažuriranje UI stanja nakon performMove i checkGameStatusLogic
                                        // Ensure all UI updates are on Main dispatcher
                                        coroutineScope.launch(Dispatchers.Main) {
                                            blackPieces = statusResult.updatedBlackPieces
                                            puzzleCompleted = statusResult.puzzleCompleted
                                            noMoreMoves = statusResult.noMoreMoves
                                            solvedPuzzlesCount += statusResult.solvedPuzzlesCountIncrement
                                            currentSessionScore = statusResult.newSessionScore // Ažuriramo skor
                                            gameStarted = statusResult.gameStarted

                                            // Pusti zvuk na osnovu ishoda poteza
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
                    board = initialBoardBackup.copy() // Resetuje tablu na početnu poziciju
                    puzzleCompleted = false
                    noMoreMoves = false
                    timeElapsedSeconds = 0
                    gameStarted = true // Ponovo pokreni tajmer

                    coroutineScope.launch(Dispatchers.IO) { // Pokreni na IO niti
                        val statusResult = checkGameStatusLogic(board, timeElapsedSeconds, difficulty, playerName, currentSessionScore)
                        withContext(Dispatchers.Main) {
                            blackPieces = statusResult.updatedBlackPieces
                            puzzleCompleted = statusResult.puzzleCompleted
                            noMoreMoves = statusResult.noMoreMoves
                            // solvedPuzzlesCount se ne menja pri resetu
                            currentSessionScore = statusResult.newSessionScore // Ažuriramo skor
                            gameStarted = statusResult.gameStarted // Ažuriramo gameStarted
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

                    coroutineScope.launch(Dispatchers.IO) { // Generisanje na pozadinskoj niti
                        val result = generateNewPuzzleLogic(
                            applicationContext, // Prosleđujemo applicationContext
                            difficulty,
                            selectedFigures,
                            minPawns,
                            maxPawns,
                            gameStarted, // Prosleđujemo trenutno stanje igre
                            playerName,
                            currentSessionScore,
                            puzzleCompleted,
                            noMoreMoves,
                            isTrainingMode
                        )
                        withContext(Dispatchers.Main) { // Ažuriranje UI stanja na glavnoj niti
                            if (result.success) {
                                board = result.newBoard
                                initialBoardBackup = result.newBoard.copy()
                                puzzleCompleted = false
                                noMoreMoves = false
                                timeElapsedSeconds = 0
                                selectedSquare = null
                                highlightedSquares = emptySet()
                                currentSessionScore = result.newSessionScore // Ažuriramo skor sa rezultatom generisanja
                                gameStarted = result.gameStartedAfterGeneration

                                // Proveri status odmah nakon generisanja (za slučaj da je generisana prazna tabla npr.)
                                val statusResult = checkGameStatusLogic(board, timeElapsedSeconds, difficulty, playerName, currentSessionScore)
                                blackPieces = statusResult.updatedBlackPieces
                                puzzleCompleted = statusResult.puzzleCompleted
                                noMoreMoves = statusResult.noMoreMoves
                                solvedPuzzlesCount += statusResult.solvedPuzzlesCountIncrement
                                currentSessionScore = statusResult.newSessionScore // Ažuriramo skor nakon provere statusa
                                gameStarted = statusResult.gameStarted

                                // Pusti zvuk uspeha samo ako je zagonetka uspešno generisana/učitana i igra počela
                                if (statusResult.gameStarted && result.success) {
                                    PuzzleGenerator.playSound(true)
                                } else if (!statusResult.gameStarted && !statusResult.puzzleCompleted && !statusResult.noMoreMoves) {
                                    // Nije uspelo generisanje, ili je odmah rešeno/bez poteza
                                    PuzzleGenerator.playSound(false)
                                }

                            } else {
                                Log.e("GameActivity", "Nije moguće generisati/učitati zagonetku, vraćena prazna tabla. Pokušajte ponovo ili promenite postavke.")
                                gameStarted = false
                                // Pusti zvuk neuspeha ako generisanje/učitavanje nije uspelo
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

// Promenjena performMove funkcija da koristi reference na top-level funkcije
suspend fun performMove(
    fromSquare: Square,
    toSquare: Square,
    currentBoard: ChessBoard,
    updateBoardState: (ChessBoard) -> Unit,
    checkGameStatusLogic: suspend (ChessBoard, Int, Difficulty, String, Int) -> GameStatusResult, // Reference to top-level
    currentTimeElapsed: Int,
    currentDifficulty: Difficulty,
    playerName: String,
    currentSessionScore: Int,
    capture: Boolean = false,
    targetSquare: Square? = null,
    onStatusUpdate: (GameStatusResult) -> Unit // Callback za ažuriranje UI stanja
) = withContext(Dispatchers.Default) { // Run this logic on Default dispatcher
    val pieceToMove = currentBoard.getPiece(fromSquare)
    if (pieceToMove.type == PieceType.NONE) {
        Log.e("performMove", "Attempted to move a non-existent piece from $fromSquare")
        return@withContext // Return from the withContext block
    }

    var newBoard = currentBoard.removePiece(fromSquare)
    if (capture && targetSquare != null) {
        newBoard = newBoard.removePiece(targetSquare)
    }
    newBoard = newBoard.setPiece(toSquare, pieceToMove)

    // Ažuriraj stanje table na glavnoj niti putem callbacka
    withContext(Dispatchers.Main) {
        updateBoardState(newBoard)
    }

    // Pozovi top-level logičku funkciju za proveru statusa
    val statusResult = checkGameStatusLogic(newBoard, currentTimeElapsed, currentDifficulty, playerName, currentSessionScore)

    // Ažuriraj UI stanje putem callbacka na glavnoj niti
    withContext(Dispatchers.Main) {
        onStatusUpdate(statusResult)
    }
}

@Preview(showBackground = true)
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