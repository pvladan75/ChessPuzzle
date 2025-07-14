// Modul2GameScreenActivity.kt
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
import kotlin.random.Random // Koristimo Kotlinov Random

// Važna napomena: Difficulty enum mora biti definisan samo u ChessDefinitions.kt

class Modul2GameScreenActivity : ComponentActivity() {

    private lateinit var game: Game
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val difficultyName = intent.getStringExtra("difficulty") ?: Difficulty.EASY.name
        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"
        val difficulty = Difficulty.valueOf(difficultyName)

        val modul2OpponentTypesNames = intent.getStringArrayListExtra("modul2OpponentTypes")
        val modul2MinOpponents = intent.getIntExtra("modul2MinOpponents", 3)
        val modul2MaxOpponents = intent.getIntExtra("modul2MaxOpponents", 5)

        val modul2OpponentTypes = modul2OpponentTypesNames?.map { PieceType.valueOf(it) } ?: emptyList()

        Log.d("Modul2GameScreenActivity", "Starting Modul 2 game for player: $playerName, Difficulty: $difficulty")

        game = Game()
        val initialBoard = generateInitialBoardForModul2(
            difficulty,
            modul2MinOpponents,
            modul2MaxOpponents,
            modul2OpponentTypes
        )
        game.initializeGame(initialBoard)
        game.setModul2Mode(true)

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
                                putStringArrayListExtra("modul2OpponentTypes", modul2OpponentTypesNames)
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

    private fun generateInitialBoardForModul2(
        difficulty: Difficulty,
        minOpponents: Int,
        maxOpponents: Int,
        availableOpponentTypes: List<PieceType>
    ): ChessBoard {
        val random = Random(System.currentTimeMillis())
        var board = ChessBoard.createEmpty()
        val allSquares = Square.ALL_SQUARES.toMutableList()

        // Koristi .random() ekstenziju sa kotlin.random.Random
        val queenStartSquare = allSquares.random(random)
        board = board.setPiece(Piece(PieceType.QUEEN, PieceColor.WHITE), queenStartSquare)
        allSquares.remove(queenStartSquare)

        // Koristi nextInt(from, until) sintaksu za kotlin.random.Random
        val numberOfOpponents = random.nextInt(minOpponents, maxOpponents + 1)
        for (i in 0 until numberOfOpponents) {
            if (allSquares.isEmpty()) {
                Log.w("Modul2GameScreenActivity", "Not enough empty squares to place all black pieces.")
                break
            }
            // Koristi .random() ekstenziju
            val pieceType = availableOpponentTypes.random(random)
            val blackPiece = Piece(pieceType, PieceColor.BLACK)

            var chosenSquare: Square? = null
            val shuffledSquares = allSquares.shuffled(random)
            for (square in shuffledSquares) {
                val tempBoard = board.setPiece(blackPiece, square)
                if (!tempBoard.isSquareAttacked(square, PieceColor.WHITE)) {
                    chosenSquare = square
                    break
                }
            }

            if (chosenSquare != null) {
                board = board.setPiece(blackPiece, chosenSquare)
                allSquares.remove(chosenSquare)
            } else {
                Log.w("Modul2GameScreenActivity", "Could not find a safe square for black piece: $pieceType. Placing anywhere.")
                // Koristi .random() ekstenziju
                val fallbackSquare = allSquares.random(random)
                board = board.setPiece(blackPiece, fallbackSquare)
                allSquares.remove(fallbackSquare)
            }
        }
        return board
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
    val whiteQueen by game.whiteQueen.collectAsState()
    val blackPieces by game.blackPieces.collectAsState()
    val moveCount by game.moveCount.collectAsState()
    val respawnCount by game.respawnCount.collectAsState()
    val puzzleSolved by game.puzzleSolved.collectAsState()
    val isGameOver by game.isGameOver.collectAsState()
    val lastAttackerPosition by game.lastAttackerPosition.collectAsState()
    val puzzleTime by game.puzzleTime.collectAsState()

    val context = LocalContext.current

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
                        if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type == PieceType.QUEEN) {
                            selectedSquare = clickedSquare
                            highlightedMoves = game.getLegalMoves(clickedSquare)
                            Log.d("Modul2GameScreen", "Queen selected at ${clickedSquare}. Possible moves: ${highlightedMoves.map { it.toString() }}")
                        } else {
                            Toast.makeText(context, "Možete pomerati samo belu damu.", Toast.LENGTH_SHORT).show()
                            Log.d("Modul2GameScreen", "Clicked on non-queen or empty square: ${clickedSquare}. No selection made.")
                        }
                    } else {
                        if (clickedSquare == selectedSquare) {
                            selectedSquare = null
                            highlightedMoves = emptyList()
                            Log.d("Modul2GameScreen", "Queen deselected.")
                        } else {
                            val move = Move(selectedSquare!!, clickedSquare)
                            Log.d("Modul2GameScreen", "Attempting to make move from ${selectedSquare!!} to ${clickedSquare}.")
                            val moveSuccessful = game.makeMove(move)

                            if (moveSuccessful) {
                                selectedSquare = null
                                highlightedMoves = emptyList()
                                Log.d("Modul2GameScreen", "Move successful. UI updated.")
                            } else {
                                Log.d("Modul2GameScreen", "Move failed or was illegal.")
                            }
                        }
                    }
                }
            },
            selectedSquare = selectedSquare,
            highlightedMoves = highlightedMoves,
            lastAttackerSquare = lastAttackerPosition?.let { (rankIndex, fileIndex) -> Square.fromCoordinates(fileIndex, rankIndex) }
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
                        Log.d("Modul2GameScreen", "Undo performed. UI state reset.")
                    }
                },
                // Ispravka: Pristupamo vrednosti StateFlow-a sa .value
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

@Preview(showBackground = true)
@Composable
fun Modul2GameScreenPreview() {
    val game = remember { Game() }
    val initialBoard = ChessBoard.createEmpty()
        .setPiece(Piece(PieceType.QUEEN, PieceColor.WHITE), Square.fromCoordinates(4, 3)) // e4
        .setPiece(Piece(PieceType.PAWN, PieceColor.BLACK), Square.fromCoordinates(3, 4)) // d5
        .setPiece(Piece(PieceType.KNIGHT, PieceColor.BLACK), Square.fromCoordinates(5, 5)) // f6
    game.initializeGame(initialBoard)
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