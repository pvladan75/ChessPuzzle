package com.chess.chesspuzzle

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

// Imports for AlertDialog and TextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api // Required for TextField in AlertDialog

// UVEZITE sve definicije iz ChessDefinitions.kt
// NEMA POTREBE ZA UVOZOM JER SU SVE KLASE U ISTOM PAKETU chess.chesspuzzle
// import com.chess.chesspuzzle.*


class GameActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("GameActivity", "Initializing SoundPool from GameActivity onCreate...")
        PuzzleGenerator.initializeSoundPool(this)

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

        Log.d("GameActivity", "Pokrenut GameActivity sa težinom: $difficulty, figurama: ${selectedFigures.joinToString()}, poteza: $minMoves-$maxMoves")

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
                        maxMoves = maxMoves
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

@Composable
fun ChessGameScreen(difficulty: String, selectedFigures: List<PieceType>, minMoves: Int, maxMoves: Int) {
    val context = LocalContext.current

    var board: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    var initialBoardBackup: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    val updateBoardState: (ChessBoard) -> Unit = { newBoard ->
        board = newBoard
    }

    var blackPieces: Map<Square, Boolean> by remember { mutableStateOf(emptyMap()) }
    val updateBlackPieces: (Map<Square, Boolean>) -> Unit = { newMap ->
        blackPieces = newMap.toMutableMap()
    }

    var puzzleCompleted: Boolean by remember { mutableStateOf(false) }
    var noMoreMoves: Boolean by remember { mutableStateOf(false) }

    // --- Nova stanja za tajmer, skor i ime igrača ---
    var timeElapsedSeconds by remember { mutableStateOf(0) }
    var gameStarted by remember { mutableStateOf(false) } // Indikator da li je tajmer aktivan
    var solvedPuzzlesCount by remember { mutableStateOf(0) }
    var currentSessionScore by remember { mutableStateOf(0) } // Ukupan skor za trenutnu sesiju

    var showNameInputDialog by remember { mutableStateOf(true) } // Pokazaćemo dijalog odmah na početku
    var playerName by remember { mutableStateOf("Anonimni") } // Podrazumevano ime

    var selectedSquare: Square? by remember { mutableStateOf(null) }
    // --- Kraj novih stanja ---


    // Tajmer efekat: pokreće se samo kada je 'gameStarted' true
    LaunchedEffect(gameStarted) {
        while (gameStarted) {
            delay(1000L) // Čekaj 1 sekundu
            timeElapsedSeconds++
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
        updateBlackPieces(updatedBlackPiecesMap)

        if (updatedBlackPiecesMap.isEmpty()) {
            puzzleCompleted = true
            gameStarted = false // Zaustavi tajmer
            selectedSquare = null // Poništi selekciju na kraju zagonetke
            solvedPuzzlesCount++ // Inkrementiraj brojač rešenih
            val scoreForPuzzle = calculateScore(timeElapsedSeconds, difficulty) // Izračunaj bodove
            currentSessionScore += scoreForPuzzle // Dodaj ukupnom skoru
            Toast.makeText(context, "Čestitamo! Rešili ste zagonetku! +$scoreForPuzzle bodova!", Toast.LENGTH_SHORT).show()
            PuzzleGenerator.playSound(context, true)
            // SAČUVAJ REZULTAT KADA JE ZAGONETKA REŠENA
            try {
                ScoreManager.addScore(ScoreEntry(playerName, currentSessionScore), difficulty)
                Log.d("GameActivity", "Skor uspešno sačuvan (Zagonetka rešena).")
            } catch (e: Exception) {
                Log.e("GameActivity", "Greška pri čuvanju skora (Zagonetka rešena): ${e.message}", e)
                Toast.makeText(context, "Greška pri čuvanju skora: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            var canWhiteCaptureBlack = false
            val whitePieces = mutableMapOf<Square, Piece>()
            for (rankIdx in 0 until 8) {
                for (fileIdx in 0 until 8) {
                    val square = Square(('a'.code + fileIdx).toChar(), rankIdx + 1)
                    val piece = currentBoardSnapshot.getPiece(square)
                    if (piece.color == PieceColor.WHITE && piece.type != PieceType.NONE) {
                        whitePieces[square] = piece
                    }
                }
            }

            for ((whiteSquare, whitePiece) in whitePieces) {
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
                gameStarted = false // Zaustavi tajmer
                selectedSquare = null // Poništi selekciju na kraju zagonetke
                Toast.makeText(context, "Nema više legalnih poteza za hvatanje crnih figura!", Toast.LENGTH_LONG).show()
                PuzzleGenerator.playSound(context, false)
                // SAČUVAJ REZULTAT KADA NEMA VIŠE POTEZA
                try {
                    ScoreManager.addScore(ScoreEntry(playerName, currentSessionScore), difficulty)
                    Log.d("GameActivity", "Skor uspešno sačuvan (Nema više poteza).")
                } catch (e: Exception) {
                    Log.e("GameActivity", "Greška pri čuvanju skora (Nema više poteza): ${e.message}", e)
                    Toast.makeText(context, "Greška pri čuvanju skora: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                noMoreMoves = false
                gameStarted = true // Osiguraj da tajmer nastavi da teče ako ima poteza
            }
        }
        currentBoardSnapshot.printBoard()
    }

    val generateNewPuzzle: () -> Unit = {
        Log.d("GameActivity", "Generišem novu zagonetku za težinu: $difficulty sa figurama: ${selectedFigures.joinToString()}, poteza: $minMoves-$maxMoves")

        // Logika kazne za preskakanje nerešene zagonetke
        if (gameStarted && !puzzleCompleted && !noMoreMoves) {
            val penalty = 100 // Primer kazne za preskakanje
            currentSessionScore = (currentSessionScore - penalty).coerceAtLeast(0) // Oduzmi kaznu, ne idi ispod nule
            Toast.makeText(context, "Zagonetka preskočena! -$penalty bodova. Trenutni skor: $currentSessionScore", Toast.LENGTH_SHORT).show()
            try {
                ScoreManager.addScore(ScoreEntry(playerName, currentSessionScore), difficulty)
                Log.d("GameActivity", "Skor uspešno sačuvan (Zagonetka preskočena).")
            } catch (e: Exception) {
                Log.e("GameActivity", "Greška pri čuvanju skora (Zagonetka preskočena): ${e.message}", e)
                Toast.makeText(context, "Greška pri čuvanju skora: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        val initialPuzzleBoard = when (difficulty) {
            "Lako" -> PuzzleGenerator.generateEasyPuzzle(context, selectedFigures)
            "Srednje" -> PuzzleGenerator.generateMediumDifficultyPuzzle(context, selectedFigures)
            "Teško" -> PuzzleGenerator.generateHardDifficultyPuzzle(context, selectedFigures, minMoves, maxMoves)
            else -> ChessBoard.createEmpty()
        }
        board = initialPuzzleBoard
        initialBoardBackup = initialPuzzleBoard.copy()
        checkGameStatus(board)
        puzzleCompleted = false
        noMoreMoves = false
        timeElapsedSeconds = 0 // Resetuj tajmer za novu zagonetku
        selectedSquare = null // Poništi selekciju pri generisanju nove zagonetke
    }


    LaunchedEffect(Unit) {
        // Ovaj LaunchedEffect se ne bavi pokretanjem prve zagonetke.
        // Prva zagonetka se pokreće tek kada korisnik unese ime (ili odbije unos imena).
    }


    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showNameInputDialog) {
            PlayerNameInputDialog(
                onNameEntered = { name ->
                    playerName = name
                    showNameInputDialog = false
                    generateNewPuzzle() // Pokreni prvu zagonetku nakon unosa imena
                },
                onDismiss = {
                    // Ako korisnik odbije unos imena, koristi podrazumevano i pokreni igru
                    showNameInputDialog = false
                    generateNewPuzzle()
                }
            )
        } else {
            // Prikaz UI elemenata igre tek kada je ime uneto
            Text(text = "Ime igrača: $playerName", modifier = Modifier.padding(bottom = 8.dp))
            Text(text = "Težina: $difficulty", modifier = Modifier.padding(bottom = 8.dp))
            Text(text = "Vreme: ${timeElapsedSeconds}s", modifier = Modifier.padding(bottom = 8.dp))
            Text(text = "Rešeno: ${solvedPuzzlesCount}", modifier = Modifier.padding(bottom = 8.dp))
            Text(text = "Ukupni skor: ${currentSessionScore}", modifier = Modifier.padding(bottom = 16.dp))


            if (puzzleCompleted) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Čestitamo! Rešili ste zagonetku!",
                        color = Color.Green,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(onClick = {
                        selectedSquare = null // Poništi selekciju
                        generateNewPuzzle() // Ovo će resetovati tajmer i pokrenuti novi krug
                    }) {
                        Text("Nova Zagonetka")
                    }
                }
            } else if (noMoreMoves) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    // ISPRAVLJENO: Uklonjen dupli "Modifier ="
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Nema više legalnih poteza za hvatanje crnih figura!",
                        color = Color.Red,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = {
                            selectedSquare = null // Poništi selekciju
                            board = initialBoardBackup.copy()
                            checkGameStatus(board)
                            puzzleCompleted = false
                            noMoreMoves = false
                            // Vreme se NE resetuje ovde, nastavlja da teče
                            Toast.makeText(context, "Tabla resetovana! Vreme nastavlja da teče.", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Pokušaj ponovo")
                        }
                        Button(onClick = {
                            selectedSquare = null // Poništi selekciju
                            generateNewPuzzle()
                        }) {
                            Text("Nova Zagonetka")
                        }
                    }
                }
            }
            else {
                Text(
                    text = "Preostalo crnih figura: ${blackPieces.size}",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Column(modifier = Modifier.aspectRatio(1f).fillMaxWidth()) {
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
                                    // Dodajemo border ako je polje selektovano
                                    .border(
                                        width = if (currentSquare == selectedSquare) 3.dp else 0.dp,
                                        color = if (currentSquare == selectedSquare) Color.Blue else Color.Transparent
                                    )
                                    .clickable {
                                        if (puzzleCompleted || noMoreMoves) {
                                            Toast.makeText(context, "Igra je završena. Kliknite 'Nova Zagonetka' ili 'Pokušaj ponovo'.", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }

                                        val pieceOnClickedSquare = board.getPiece(currentSquare)

                                        Log.d("ChessGame", "Kliknuto na ${currentSquare.toString()}. Figura: ${pieceOnClickedSquare.type} ${pieceOnClickedSquare.color}")

                                        if (selectedSquare == null) {
                                            // NIJEDNA FIGURA NIJE SELEKTOVANA:
                                            if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                                                // Selektuj belu figuru
                                                selectedSquare = currentSquare
                                                Toast.makeText(context, "${pieceOnClickedSquare.color} ${pieceOnClickedSquare.type} selektovan na ${currentSquare.toString()}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                // Klik na crnu figuru ili prazno polje bez selektovane bele figure
                                                Toast.makeText(context, "Morate selektovati belu figuru!", Toast.LENGTH_SHORT).show()
                                                selectedSquare = null // Osiguraj da je deselektovano
                                            }
                                        } else {
                                            // FIGURA JE VEĆ SELEKTOVANA:
                                            val fromSquare = selectedSquare!!
                                            val toSquare = currentSquare
                                            val pieceToMove = board.getPiece(fromSquare)

                                            if (fromSquare == toSquare) {
                                                // Klik na istu selektovanu figuru - deselektuj je
                                                selectedSquare = null
                                                Toast.makeText(context, "Figuura deselektovana.", Toast.LENGTH_SHORT).show()
                                                return@clickable
                                            }

                                            if (pieceOnClickedSquare.color == PieceColor.WHITE && pieceOnClickedSquare.type != PieceType.NONE) {
                                                // Klik na drugu belu figuru - deselektuj staru i selektuj novu
                                                selectedSquare = currentSquare
                                                Toast.makeText(context, "Promenjena selektovana figura na ${pieceOnClickedSquare.color} ${pieceOnClickedSquare.type} na ${currentSquare.toString()}", Toast.LENGTH_SHORT).show()
                                                return@clickable
                                            }

                                            // Pokušaj da odigraš potez
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
                                                    updateBoardState,
                                                    checkGameStatus,
                                                    capture = isCaptureOfBlackPiece,
                                                    targetSquare = toSquare
                                                )
                                                if (!puzzleCompleted && !noMoreMoves) {
                                                    selectedSquare = toSquare
                                                }
                                            } else if (isPureChessValidMove && !isCaptureOfBlackPiece) {
                                                Toast.makeText(context, "U ovoj zagonetki morate pojesti crnu figuru!", Toast.LENGTH_LONG).show()
                                                selectedSquare = null // Poništi selekciju pri neuspešnom potezu
                                            } else {
                                                Toast.makeText(context, "Nije validan šahovski potez za odabranu figuru!", Toast.LENGTH_SHORT).show()
                                                selectedSquare = null // Poništi selekciju pri neuspešnom potezu
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Dodata PlayerNameInputDialog Composable funkcija
@OptIn(ExperimentalMaterial3Api::class) // Za TextField
@Composable
fun PlayerNameInputDialog(onNameEntered: (String) -> Unit, onDismiss: () -> Unit) {
    var nameInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unesite vaše ime") },
        text = {
            TextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Ime") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (nameInput.isNotBlank()) {
                        onNameEntered(nameInput)
                    } else {
                        // Ako je prazno, tretirati kao otkazivanje i koristiti podrazumevano ime
                        onDismiss()
                    }
                })
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onNameEntered(nameInput)
                    } else {
                        onDismiss() // Otkazi ako je prazno
                    }
                }
            ) {
                Text("Potvrdi")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Otkazi")
            }
        }
    )
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
        ChessGameScreen("Lako", listOf(PieceType.QUEEN), 5, 6)
    }
}