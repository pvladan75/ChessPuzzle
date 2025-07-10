package com.chess.chesspuzzle

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign // Dodaj ovaj import za TextAlign!

// Glavna aktivnost za igru
class GameActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("GameActivity", "Initializing SoundPool from GameActivity onCreate...")
        PuzzleGenerator.initializeSoundPool(applicationContext)

        // Dohvati podatke iz Intent-a
        val difficulty = intent.getStringExtra("difficulty") ?: "Lako"
        val selectedFiguresNames = intent.getStringArrayListExtra("selectedFigures") ?: arrayListOf()
        val selectedFigures = selectedFiguresNames.mapNotNull {
            try {
                PieceType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                Log.e("GameActivity", "Nepoznat PieceType: $it", e)
                null
            }
        }
        val minMoves = intent.getIntExtra("minMoves", 1)
        val maxMoves = intent.getIntExtra("maxMoves", 1)
        // KLJUČNO: Dohvati ime igrača iz Intent-a
        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"

        Log.d("GameActivity", "Pokrenut GameActivity sa težinom: $difficulty, figurama: ${selectedFigures.joinToString()}, poteza: $minMoves-$maxMoves, igrač: $playerName")

        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChessGameScreen(
                        difficulty = difficulty,
                        selectedFigures = selectedFigures,
                        minMoves = minMoves,
                        maxMoves = maxMoves,
                        playerName = playerName // Prosledi ime igrača Composable-u
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("GameActivity", "Releasing SoundPool from GameActivity onDestroy...")
        PuzzleGenerator.releaseSoundPool()
    }
}

// Dodat 'playerName' kao parametar ChessGameScreen-u
@Composable
fun ChessGameScreen(difficulty: String, selectedFigures: List<PieceType>, minMoves: Int, maxMoves: Int, playerName: String) {
    val context = LocalContext.current

    var board: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    var initialBoardBackup: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }

    var blackPieces: Map<Square, Boolean> by remember { mutableStateOf(emptyMap()) }

    var puzzleCompleted: Boolean by remember { mutableStateOf(false) }
    var noMoreMoves: Boolean by remember { mutableStateOf(false) }

    var timeElapsedSeconds by remember { mutableStateOf(0) }
    var gameStarted by remember { mutableStateOf(false) }
    var solvedPuzzlesCount by remember { mutableStateOf(0) }
    var currentSessionScore by remember { mutableStateOf(0) }

    var selectedSquare: Square? by remember { mutableStateOf(null) }

    LaunchedEffect(gameStarted) {
        while (gameStarted) {
            delay(1000L)
            timeElapsedSeconds++
            // Log.d("GameTimer", "Vreme proteklo: $timeElapsedSeconds s") // Smanjio log da ne spamuje konzolu
        }
    }

    val calculateScore: (Int, String) -> Int = { timeInSeconds, currentDifficulty ->
        val maxTimeBonusSeconds: Int
        val pointsPerSecond: Int
        val basePointsPerPuzzle: Int

        when (currentDifficulty) {
            "Lako" -> {
                maxTimeBonusSeconds = 90
                pointsPerSecond = 5
                basePointsPerPuzzle = 300
            }
            "Srednje" -> {
                maxTimeBonusSeconds = 60
                pointsPerSecond = 10
                basePointsPerPuzzle = 600
            }
            "Teško" -> {
                maxTimeBonusSeconds = 30
                pointsPerSecond = 20
                basePointsPerPuzzle = 1000
            }
            else -> {
                maxTimeBonusSeconds = 60
                pointsPerSecond = 5
                basePointsPerPuzzle = 500
            }
        }
        val timePoints = (maxTimeBonusSeconds - timeInSeconds).coerceAtLeast(0) * pointsPerSecond
        basePointsPerPuzzle + timePoints
    }

    val checkGameStatus: (ChessBoard) -> Unit = { currentBoardSnapshot ->
        val updatedBlackPiecesMap = currentBoardSnapshot.getPiecesMapFromBoard(PieceColor.BLACK)
        blackPieces = updatedBlackPiecesMap

        if (updatedBlackPiecesMap.isEmpty()) {
            puzzleCompleted = true
            gameStarted = false
            selectedSquare = null
            solvedPuzzlesCount++
            val scoreForPuzzle = calculateScore(timeElapsedSeconds, difficulty)
            currentSessionScore += scoreForPuzzle
            Toast.makeText(context, "Čestitamo! Rešili ste zagonetku! +$scoreForPuzzle bodova! Ukupno: $currentSessionScore", Toast.LENGTH_LONG).show()
            PuzzleGenerator.playSound(context, true)
            try {
                ScoreManager.addScore(ScoreEntry(playerName, currentSessionScore), difficulty) // Koristi prosleđeno ime
                Log.d("GameActivity", "Skor uspešno sačuvan (Zagonetka rešena).")
            } catch (e: Exception) {
                Log.e("GameActivity", "Greška pri čuvanju skora (Zagonetka rešena): ${e.message}", e)
                Toast.makeText(context, "Greška pri čuvanju skora: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
                selectedSquare = null
                Toast.makeText(context, "Nema više legalnih poteza za hvatanje crnih figura!", Toast.LENGTH_LONG).show()
                PuzzleGenerator.playSound(context, false)
                try {
                    ScoreManager.addScore(ScoreEntry(playerName, currentSessionScore), difficulty) // Koristi prosleđeno ime
                    Log.d("GameActivity", "Skor uspešno sačuvan (Nema više poteza).")
                } catch (e: Exception) {
                    Log.e("GameActivity", "Greška pri čuvanju skora (Nema više poteza): ${e.message}", e)
                    Toast.makeText(context, "Greška pri čuvanju skora: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                noMoreMoves = false
                gameStarted = true
            }
        }
        // currentBoardSnapshot.printBoard() // Smanjio log da ne spamuje konzolu
    }

    val generateNewPuzzle: () -> Unit = {
        Log.d("GameActivity", "Generišem novu zagonetku za težinu: $difficulty sa figurama: ${selectedFigures.joinToString()}, poteza: $minMoves-$maxMoves")

        if (gameStarted && !puzzleCompleted && !noMoreMoves) {
            val penalty = 100
            currentSessionScore = (currentSessionScore - penalty).coerceAtLeast(0)
            Toast.makeText(context, "Zagonetka preskočena! -$penalty bodova. Trenutni skor: $currentSessionScore", Toast.LENGTH_SHORT).show()
            try {
                ScoreManager.addScore(ScoreEntry(playerName, currentSessionScore), difficulty) // Koristi prosleđeno ime
                Log.d("GameActivity", "Skor uspešno sačuvan (Zagonetka preskočena).")
            } catch (e: Exception) {
                Log.e("GameActivity", "Greška pri čuvanju skora (Zagonetka preskočena): ${e.message}", e)
                Toast.makeText(context, "Greška pri čuvanju skora: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        val newPuzzleBoard = when (difficulty) {
            "Lako" -> PuzzleGenerator.generateEasyPuzzle(context, selectedFigures)
            "Srednje" -> PuzzleGenerator.generateMediumDifficultyPuzzle(context, selectedFigures)
            "Teško" -> PuzzleGenerator.generateHardDifficultyPuzzle(context, selectedFigures, minMoves, maxMoves)
            else -> ChessBoard.createEmpty()
        }
        board = newPuzzleBoard
        initialBoardBackup = newPuzzleBoard.copy()
        checkGameStatus(board)
        puzzleCompleted = false
        noMoreMoves = false
        timeElapsedSeconds = 0
        selectedSquare = null
        gameStarted = true
    }

    LaunchedEffect(Unit) {
        // Kada se GameActivity pokrene, odmah generiši zagonetku.
        if (board.getPiecesMapFromBoard(PieceColor.WHITE).isEmpty() && board.getPiecesMapFromBoard(PieceColor.BLACK).isEmpty()) {
            generateNewPuzzle()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp), // Dodato padding celoj koloni
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Poravnato na vrh
    ) {
        // Red sa informacijama o igraču i težini
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Igrač: $playerName", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            // ISPRAVLJENO: Uklonjen je '.also { /* align text to end */ }'
            Text(text = "Težina: $difficulty", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Red sa vremenom i rešenim zagonetkama/skorom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Vreme: ${timeElapsedSeconds}s", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(text = "Rešeno: ${solvedPuzzlesCount}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            // ISPRAVLJENO: Uklonjen je '.also { /* align text to end */ }'
            Text(text = "Skor: ${currentSessionScore}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Spacer(modifier = Modifier.height(16.dp)) // Više razmaka pre table

        // Statusna poruka (rešeno/nema više poteza/preostalo figura)
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp), // Fiksna visina za poruku
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
        Spacer(modifier = Modifier.height(8.dp)) // Razmak između poruke i table


        // Prikaz šahovske table
        // Board zauzima preostali prostor u koloni, ali održava 1:1 odnos stranica
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Održava kvadratni oblik (širina = visina)
                .weight(1f), // Omogućava tabli da zauzme dostupan prostor
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val lightSquareColor = Color(0xFFF0D9B5)
            val darkSquareColor = Color(0xFFB58863)

            for (row in 0 until 8) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until 8) {
                        val currentSquare = Square(file = 'a' + col, rank = 8 - row)

                        val isLightSquare = (row + col) % 2 == 0
                        val squareColor = if (isLightSquare) lightSquareColor else darkSquareColor

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(squareColor)
                                .border(
                                    width = if (currentSquare == selectedSquare) 3.dp else 0.dp,
                                    color = if (currentSquare == selectedSquare) Color.Blue else Color.Transparent
                                )
                                .clickable {
                                    if (puzzleCompleted || noMoreMoves) {
                                        Toast.makeText(context, "Igra je završena. Kliknite 'Nova Zagonetka' ili 'Resetuj poziciju'.", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }

                                    val pieceOnClickedSquare = board.getPiece(currentSquare)

                                    // Log.d("ChessGame", "Kliknuto na ${currentSquare.toString()}. Figura: ${pieceOnClickedSquare.type} ${pieceOnClickedSquare.color}")

                                    if (selectedSquare == null) {
                                        if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                                            selectedSquare = currentSquare
                                            Toast.makeText(context, "${pieceOnClickedSquare.color} ${pieceOnClickedSquare.type} selektovan na ${currentSquare.toString()}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Morate selektovati belu figuru!", Toast.LENGTH_SHORT).show()
                                            selectedSquare = null
                                        }
                                    } else {
                                        val fromSquare = selectedSquare!!
                                        val toSquare = currentSquare
                                        val pieceToMove = board.getPiece(fromSquare)

                                        if (fromSquare == toSquare) {
                                            selectedSquare = null
                                            Toast.makeText(context, "Figuura deselektovana.", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }

                                        if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                                            selectedSquare = currentSquare
                                            Toast.makeText(context, "Promenjena selektovana figura na ${pieceOnClickedSquare.color} ${pieceOnClickedSquare.type} na ${currentSquare.toString()}", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }

                                        val legalChessMovesForSelectedPiece = ChessCore.getValidMoves(board, pieceToMove, fromSquare)
                                        val isPureChessValidMove = legalChessMovesForSelectedPiece.contains(toSquare)

                                        val pieceAtTarget = board.getPiece(toSquare)
                                        val isCaptureOfBlackPiece = pieceAtTarget.type != PieceType.NONE &&
                                                pieceAtTarget.color == PieceColor.BLACK &&
                                                pieceToMove.color != pieceAtTarget.color

                                        val isPuzzleValidMove = isPureChessValidMove && isCaptureOfBlackPiece


                                        if (isPuzzleValidMove) {
                                            Toast.makeText(context, "Potez izvršen na ${toSquare.toString()}!", Toast.LENGTH_SHORT).show()
                                            performMove(
                                                fromSquare,
                                                toSquare,
                                                board,
                                                updateBoardState = { newBoard -> board = newBoard },
                                                checkGameStatus = checkGameStatus,
                                                capture = true,
                                                targetSquare = toSquare
                                            )
                                            // Ne resetuj selectedSquare odmah nakon poteza ako je potez validan.
                                            // To omogućava lancu hvatanja. SelectedSquare se resetuje u checkGameStatus
                                            // samo ako je zagonetka kompletirana ili nema više poteza.
                                            if (!puzzleCompleted && !noMoreMoves) {
                                                selectedSquare = toSquare
                                            }
                                            PuzzleGenerator.playSound(context, true) // Play success sound
                                        } else if (isPureChessValidMove && !isCaptureOfBlackPiece) {
                                            Toast.makeText(context, "U ovoj zagonetki morate pojesti crnu figuru!", Toast.LENGTH_LONG).show()
                                            selectedSquare = null
                                            PuzzleGenerator.playSound(context, false) // Play error sound
                                        } else {
                                            Toast.makeText(context, "Nije validan šahovski potez za odabranu figuru!", Toast.LENGTH_SHORT).show()
                                            selectedSquare = null
                                            PuzzleGenerator.playSound(context, false) // Play error sound
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val piece = board.getPiece(currentSquare)
                            if (piece.type != PieceType.NONE) {
                                val drawableResId = when (Pair(piece.type, piece.color)) {
                                    Pair(PieceType.KNIGHT, PieceColor.WHITE) -> R.drawable.wn
                                    Pair(PieceType.PAWN, PieceColor.BLACK) -> R.drawable.bp
                                    Pair(PieceType.ROOK, PieceColor.WHITE) -> R.drawable.wr
                                    Pair(PieceType.ROOK, PieceColor.BLACK) -> R.drawable.br
                                    Pair(PieceType.BISHOP, PieceColor.WHITE) -> R.drawable.wb
                                    Pair(PieceType.BISHOP, PieceColor.BLACK) -> R.drawable.bb
                                    Pair(PieceType.QUEEN, PieceColor.WHITE) -> R.drawable.wq
                                    Pair(PieceType.QUEEN, PieceColor.BLACK) -> R.drawable.bq
                                    Pair(PieceType.KING, PieceColor.WHITE) -> R.drawable.wk
                                    Pair(PieceType.KING, PieceColor.BLACK) -> R.drawable.bk
                                    else -> 0
                                }
                                if (drawableResId != 0) {
                                    Image(
                                        painter = painterResource(id = drawableResId),
                                        contentDescription = "${piece.color} ${piece.type}",
                                        modifier = Modifier.fillMaxSize(0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Razmak ispod table

        // Dugmad za kontrolu igre - uvek vidljiva i na dnu ekrana
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    selectedSquare = null // Poništi selekciju
                    board = initialBoardBackup.copy() // Vrati tablu na početno stanje
                    checkGameStatus(board) // Ponovo proveri status (ažurira broj pešaka, pokreće tajmer)
                    puzzleCompleted = false // Resetuj status
                    noMoreMoves = false // Resetuj status
                    timeElapsedSeconds = 0 // Resetuj vreme na 0 kod resetovanja pozicije
                    gameStarted = true // Pokreni tajmer ponovo
                    Toast.makeText(context, "Tabla resetovana i vreme resetovano!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f).padding(end = 4.dp) // Manji razmak
            ) {
                Text("Resetuj poziciju")
            }
            Button(
                onClick = {
                    selectedSquare = null // Poništi selekciju
                    generateNewPuzzle() // Generiši novu zagonetku i resetuj tajmer
                },
                modifier = Modifier.weight(1f).padding(start = 4.dp) // Manji razmak
            ) {
                Text("Nova Zagonetka")
            }
        }
    }
}


fun performMove(
    fromSquare: Square,
    toSquare: Square,
    currentBoard: ChessBoard,
    updateBoardState: (ChessBoard) -> Unit,
    checkGameStatus: (ChessBoard) -> Unit,
    capture: Boolean = false,
    targetSquare: Square? = null
) {
    val pieceToMove = currentBoard.getPiece(fromSquare)
    if (pieceToMove.type == PieceType.NONE) {
        Log.e("performMove", "Attempted to move a non-existent piece from $fromSquare")
        return
    }

    var newBoard = currentBoard.removePiece(fromSquare)
    if (capture && targetSquare != null) {
        newBoard = newBoard.removePiece(targetSquare)
    }
    newBoard = newBoard.setPiece(toSquare, pieceToMove)

    updateBoardState(newBoard)
    checkGameStatus(newBoard)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ChessPuzzleTheme {
        // U preview-u, moramo da simuliramo prosleđivanje imena igrača
        ChessGameScreen(
            difficulty = "Srednje",
            selectedFigures = listOf(PieceType.QUEEN, PieceType.KNIGHT),
            minMoves = 3,
            maxMoves = 6,
            playerName = "Preview Igrač" // Simulirano ime za preview
        )
    }
}