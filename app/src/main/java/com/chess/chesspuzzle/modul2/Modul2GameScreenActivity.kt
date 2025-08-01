package com.chess.chesspuzzle.modul2

import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.* // Importuj sve iz glavnog paketa (ChessDefinitions.kt, Game.kt, ChessBoardUI.kt itd.)

// Uvoz za PositionGenerator
import com.chess.chesspuzzle.modul2.PositionGenerator // Importuj tvoju novu PositionGenerator klasu
import kotlin.random.Random // Importuj Kotlinov Random za nasumični izbor

private val TAG = "Modul2GameScreenActivity"
// Važna napomena: Difficulty enum mora biti definisan samo u ChessDefinitions.kt
// PieceType enum se sada koristi direktno iz com.chess.chesspuzzle

class Modul2GameScreenActivity : ComponentActivity() {

    private lateinit var game: Game
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())
    // Instanciramo PositionGenerator ovde
    private val positionGenerator = PositionGenerator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoundManager.initialize(applicationContext) // Inicijalizacija SoundManagera

        val difficultyName = intent.getStringExtra("difficulty") ?: Difficulty.EASY.name
        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"
        val difficulty = Difficulty.valueOf(difficultyName)

        val modul2OpponentTypesNames = intent.getStringArrayListExtra("modul2OpponentTypes")
        val modul2MinOpponents = intent.getIntExtra("modul2MinOpponents", 3)
        val modul2MaxOpponents = intent.getIntExtra("modul2MaxOpponents", 5)

        val modul2OpponentTypes = modul2OpponentTypesNames?.map { PieceType.valueOf(it) } ?: emptyList()

        Log.d(TAG, "Starting Modul 2 game for player: $playerName, Difficulty: $difficulty")

        game = Game() // Kreiraj instancu Game klase

        // >>>>>>>>>>>>> LOGIKA ZA GENERISANJE POZICIJE <<<<<<<<<<<<<
        try {
            var numBishops = 0
            var numRooks = 0
            var numKnights = 0
            var numPawns = 0

            // Početna raspodela figura na osnovu težine i dozvoljenih tipova
            when (difficulty) {
                Difficulty.EASY -> {
                    if (modul2OpponentTypes.contains(PieceType.PAWN)) numPawns = 2
                    if (modul2OpponentTypes.contains(PieceType.KNIGHT)) numKnights = 1
                }
                Difficulty.MEDIUM -> {
                    if (modul2OpponentTypes.contains(PieceType.PAWN)) numPawns = 3
                    if (modul2OpponentTypes.contains(PieceType.KNIGHT)) numKnights = 1
                    if (modul2OpponentTypes.contains(PieceType.BISHOP)) numBishops = 1
                    if (modul2OpponentTypes.contains(PieceType.ROOK)) numRooks = 1
                }
                Difficulty.HARD -> {
                    if (modul2OpponentTypes.contains(PieceType.PAWN)) numPawns = 4
                    if (modul2OpponentTypes.contains(PieceType.KNIGHT)) numKnights = 2
                    if (modul2OpponentTypes.contains(PieceType.BISHOP)) numBishops = 2
                    if (modul2OpponentTypes.contains(PieceType.ROOK)) numRooks = 2
                }
            }

            // Ograničenja broja figura (max 2 za topove/lovce/skakače, max 8 za pešake)
            numBishops = numBishops.coerceAtMost(2)
            numRooks = numRooks.coerceAtMost(2)
            numKnights = numKnights.coerceAtMost(2)
            numPawns = numPawns.coerceAtMost(8)

            // Prilagođavanje ukupnog broja figura prema modul2MinOpponents i modul2MaxOpponents
            var currentTotalBlackPieces = numBishops + numRooks + numKnights + numPawns

            // Ako je ukupan broj figura manji od minimalnog, dodaj pešake
            while (currentTotalBlackPieces < modul2MinOpponents && numPawns < 8) {
                numPawns++
                currentTotalBlackPieces++
            }

            // Ako je ukupan broj figura veći od maksimalnog, pokušaj smanjiti (od pešaka, pa naviše)
            while (currentTotalBlackPieces > modul2MaxOpponents) {
                if (numPawns > 0) {
                    numPawns--
                } else if (numKnights > 0) {
                    numKnights--
                } else if (numBishops > 0) {
                    numBishops--
                } else if (numRooks > 0) {
                    numRooks--
                } else {
                    break // Nema više figura za uklanjanje
                }
                currentTotalBlackPieces--
            }

            // DODATA LOGIKA ZA ODABIR BELE FIGURE
            val whitePieceOptions = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.KNIGHT)
            val selectedWhitePieceType = whitePieceOptions.random(Random(System.currentTimeMillis())) // Koristimo Kotlinov Random
            Log.d(TAG, "Selected white piece type for puzzle: $selectedWhitePieceType")

            Log.d(TAG, "Attempting to generate puzzle with: White Piece=$selectedWhitePieceType, B=$numBishops, R=$numRooks, N=$numKnights, P=$numPawns")

            val generatedPuzzle = positionGenerator.generatePuzzle(
                whitePieceType = selectedWhitePieceType, // PROSLEĐUJEMO ODABRANI TIP BELE FIGURE
                numBlackBishops = numBishops,
                numBlackRooks = numRooks,
                numBlackKnights = numKnights,
                numBlackPawns = numPawns
            )
            game.initializeGame(generatedPuzzle.initialBoard)
            game.setModul2Mode(true)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri generisanju zagonetke: ${e.message}", e)
            Toast.makeText(this, "Greška pri generisanju zagonetke: ${e.message}. Molimo pokušajte ponovo.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        // ^^^^^^^^^^^^^^ KRAJ LOGIKE <<<<<<<<<<<<<<

        startTimer()

        setContent {
            com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Modul2GameScreen(
                        game = game,
                        playerName = playerName,
                        difficulty = difficulty,
                        onRestartGame = {
                            val restartIntent = Intent(this, Modul2GameScreenActivity::class.java).apply {
                                putExtra("difficulty", difficultyName)
                                putExtra("playerName", playerName)
                                putStringArrayListExtra("modul2OpponentTypes", modul2OpponentTypesNames as ArrayList<String>?)
                                putExtra("modul2MinOpponents", modul2MinOpponents)
                                putExtra("modul2MaxOpponents", modul2MaxOpponents)
                            }
                            finish()
                            startActivity(restartIntent)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        SoundManager.release()
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {
                    game.incrementPuzzleTime()
                }
            }
        }, 1000, 1000)
    }
}


@Composable
fun Modul2GameScreen(
    game: Game,
    playerName: String,
    difficulty: Difficulty,
    onRestartGame: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var highlightedMoves by remember { mutableStateOf(emptyList<Square>()) }
    var showRestartDialog by remember { mutableStateOf(false) }

    val currentBoard by game.board.collectAsState()
    // Promenjen naziv varijable radi jasnosti, ako u Game klasi prati "jedinu belu figuru"
    val whitePieceOnBoard by game.whiteQueen.collectAsState()
    val blackPieces by game.blackPieces.collectAsState()
    val moveCount by game.moveCount.collectAsState()
    val respawnCount by game.respawnCount.collectAsState()
    val puzzleSolved by game.puzzleSolved.collectAsState()
    val isGameOver by game.isGameOver.collectAsState()
    val lastAttackerPosition by game.lastAttackerPosition.collectAsState()
    val puzzleTime by game.puzzleTime.collectAsState()

    val context = LocalContext.current
    val TAG = "Modul2GameScreen"

    LaunchedEffect(puzzleSolved) {
        if (puzzleSolved) {
            Toast.makeText(context, "Zagonetka rešena!", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(isGameOver) {
        if (isGameOver && !puzzleSolved) {
            Toast.makeText(context, "Kraj igre!", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ime igrača: $playerName", style = MaterialTheme.typography.titleLarge)
            Text("Težina: ${difficulty.name}", style = MaterialTheme.typography.titleMedium)
            Text("Modul 2: Pomoć pri učenju", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("Potezi: $moveCount", style = MaterialTheme.typography.bodyLarge)
                Text("Respawn: $respawnCount", style = MaterialTheme.typography.bodyLarge)
                Text("Vreme: ${puzzleTime / 1000}s", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        ChessBoard(
            board = currentBoard,
            modifier = Modifier.fillMaxWidth(),
            onSquareClick = { clickedSquare ->
                coroutineScope.launch {
                    if (isGameOver) {
                        Toast.makeText(context, "Igra je završena!", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val pieceOnClickedSquare = currentBoard.getPiece(clickedSquare)

                    if (selectedSquare == null) {
                        if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                            selectedSquare = clickedSquare
                            highlightedMoves = game.getLegalMoves(clickedSquare)
                            Log.d(TAG, "${pieceOnClickedSquare.type} selected at ${clickedSquare}. Possible moves: ${highlightedMoves.map { it.toString() }}")
                        } else {
                            Toast.makeText(context, "Možete pomerati samo belu figuru.", Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "Clicked on non-white piece or empty square: ${clickedSquare}. No selection made.")
                        }
                    } else {
                        if (clickedSquare == selectedSquare) {
                            selectedSquare = null
                            highlightedMoves = emptyList()
                            Log.d(TAG, "White piece deselected.")
                        } else {
                            val move = Move(selectedSquare!!, clickedSquare)
                            Log.d(TAG, "Attempting to make move from ${selectedSquare!!} to ${clickedSquare}.")
                            val moveSuccessful = game.makeMove(move)

                            if (moveSuccessful) {
                                selectedSquare = null
                                highlightedMoves = emptyList()
                                Log.d(TAG, "Move successful. UI updated.")
                            } else {
                                Log.d(TAG, "Move failed or was illegal.")
                            }
                        }
                    }
                }
            },
            selectedSquare = selectedSquare,
            highlightedMoves = highlightedMoves,
            lastAttackerSquare = lastAttackerPosition?.let { (fileIndex, rankIndex) -> Square.fromCoordinates(fileIndex, rankIndex) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (puzzleSolved) {
                Text(
                    text = "Čestitamo! Rešili ste zagonetku!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Green,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Vaš rezultat: ${game.currentPuzzleScore}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else if (isGameOver && !puzzleSolved) {
                Text(
                    text = "Igra je završena!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        game.undoMove()
                        selectedSquare = null
                        highlightedMoves = emptyList()
                        Log.d(TAG, "Undo performed. UI state reset.")
                    }
                },
                enabled = game.moveCount.value > 0 && !isGameOver,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Poništi potez")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showRestartDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restartuj nivo")
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Restartuj igru") },
            text = { Text("Da li ste sigurni da želite da restartujete trenutnu igru?") },
            confirmButton = {
                Button(onClick = {
                    onRestartGame()
                    showRestartDialog = false
                }) {
                    Text("Restartuj")
                }
            },
            dismissButton = {
                Button(onClick = { showRestartDialog = false }) {
                    Text("Otkaži")
                }
            }
        )
    }
}

// ... (sav prethodni kod u Modul2GameScreenActivity.kt ostaje isti do @Preview) ...

@Preview(showBackground = true)
@Composable
fun Modul2GameScreenPreview() {
    val game = remember { Game() }
    val positionGenerator = remember { PositionGenerator() } // Instanciraj generator za Preview

    // Generišemo dummy puzzle za preview, slično kao u onCreate
    // Moramo definisati tip bele figure za preview
    val whitePieceTypeForPreview = PieceType.QUEEN // Možeš odabrati QUEEN, ROOK, ili KNIGHT za preview
    val numBlackBishops = 1
    val numBlackRooks = 1
    val numBlackKnights = 1
    val numBlackPawns = 2

    // Pozivamo generatePuzzle kao i u stvarnoj aktivnosti
    val initialBoardForPreview = try {
        positionGenerator.generatePuzzle(
            whitePieceType = whitePieceTypeForPreview,
            numBlackBishops = numBlackBishops,
            numBlackRooks = numBlackRooks,
            numBlackKnights = numBlackKnights,
            numBlackPawns = numBlackPawns,
            maxAttempts = 100 // Manje pokušaja za preview, jer ne mora biti savršeno rešivo
        ).initialBoard
    } catch (e: Exception) {
        // U slučaju greške, vratite praznu ili defaultnu tablu za preview
        Log.e("Preview", "Error generating puzzle for preview: ${e.message}")
        ChessBoard.createEmpty()
            .setPiece(Piece(PieceType.QUEEN, PieceColor.WHITE), Square.fromCoordinates(4, 3))
            .setPiece(Piece(PieceType.PAWN, PieceColor.BLACK), Square.fromCoordinates(3, 4))
    }

    game.initializeGame(initialBoardForPreview) // Koristimo generisanu tablu
    game.setModul2Mode(true)

    com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme {
        Modul2GameScreen(
            game = game,
            playerName = "Preview Igrač",
            difficulty = Difficulty.EASY,
            onRestartGame = {}
        )
    }
}