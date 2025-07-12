package com.chess.chesspuzzle

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Uključuje SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import kotlinx.coroutines.launch // Potrebno za coroutineScope.launch

// Enum za tip figura koje korisnik može da postavlja
// Ostavljamo ga ovde ako ga ne koristimo na drugom mestu
enum class CreationMode {
    NONE, // Prazno polje
    WHITE_QUEEN, WHITE_ROOK, WHITE_BISHOP, WHITE_KNIGHT, WHITE_PAWN, WHITE_KING,
    BLACK_QUEEN, BLACK_ROOK, BLACK_BISHOP, BLACK_KNIGHT, BLACK_PAWN, BLACK_KING
}

class PositionCreationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"

        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PositionCreationScreen(playerName = playerName)
                }
            }
        }
    }
}

// Data klasa za rezultat Solvera (treba da bude definisana negde globalno, npr. u ChessSolver.kt ili ChessDefinitions.kt)
// Ako je već definisana u ChessSolver.kt kao što ste mi poslali, onda ovaj deo možete ukloniti.
// Ali ako nije, stavite je ovde ili u ChessDefinitions.kt
data class ChessSolution(
    val isSolved: Boolean,
    val moves: List<String>, // Lista poteza u standardnoj notaciji (npr. "e2e4")
    val reason: String = "" // Objašnjenje ako rešenja nema ili je pozicija nevalidna
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionCreationScreen(playerName: String) {
    val context = LocalContext.current
    var board: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    var currentSelectedSquare: Square? by remember { mutableStateOf(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var fenInput by rememberSaveable { mutableStateOf("") }

    // NOVO: Kreiranje instance ChessSolvera
    val chessSolver = remember { ChessSolver() } // Kreiramo instancu Solvera

    val pieceCycle = remember {
        listOf(
            Piece(PieceType.PAWN, PieceColor.BLACK),
            Piece(PieceType.ROOK, PieceColor.BLACK),
            Piece(PieceType.KNIGHT, PieceColor.BLACK),
            Piece(PieceType.BISHOP, PieceColor.BLACK),
            Piece(PieceType.QUEEN, PieceColor.BLACK),
            Piece(PieceType.KING, PieceColor.BLACK),
            Piece(PieceType.PAWN, PieceColor.WHITE),
            Piece(PieceType.ROOK, PieceColor.WHITE),
            Piece(PieceType.KNIGHT, PieceColor.WHITE),
            Piece(PieceType.BISHOP, PieceColor.WHITE),
            Piece(PieceType.QUEEN, PieceColor.WHITE),
            Piece(PieceType.KING, PieceColor.WHITE),
            Piece.NONE
        )
    }

    val cyclePieces = { square: Square ->
        val existingPiece = board.getPiece(square)
        val currentIndex = pieceCycle.indexOfFirst { it.type == existingPiece.type && it.color == existingPiece.color }

        val nextIndex = if (currentIndex == -1 || currentIndex == pieceCycle.size - 1) {
            0
        } else {
            currentIndex + 1
        }

        val newPiece = pieceCycle[nextIndex]

        if (newPiece.type == PieceType.NONE) {
            board = board.removePiece(square)
        } else {
            board = board.setPiece(newPiece, square)
        }
        fenInput = board.toFEN().split(" ")[0]
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Igrač: $playerName", style = MaterialTheme.typography.titleMedium)
        Text("Kreirajte svoju poziciju", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = fenInput,
                onValueChange = { fenInput = it },
                label = { Text("Unesite FEN poziciju") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                try {
                    board = ChessBoard.parseFenToBoard(fenInput)
                    currentSelectedSquare = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "FEN pozicija uspešno učitana!",
                            duration = SnackbarDuration.Short
                        )
                    }
                } catch (e: Exception) {
                    Log.e("PositionCreation", "Greška pri parsiranju FEN-a: ${e.message}")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Nevalidan FEN format! Pokušajte ponovo.",
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }) {
                Text("Učitaj FEN")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChessBoardComposable(
            board = board,
            selectedSquare = currentSelectedSquare,
            highlightedSquares = emptySet(),
            onSquareClick = { clickedSquare ->
                currentSelectedSquare = clickedSquare
                cyclePieces(clickedSquare)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                board = ChessBoard.createEmpty()
                currentSelectedSquare = null
                fenInput = ""
            }) {
                Text("Obriši tablu")
            }
            Button(onClick = {
                board = ChessBoard.createStandardBoard()
                currentSelectedSquare = null
                fenInput = board.toFEN().split(" ")[0]
            }) {
                Text("Standardna pozicija")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // MODIFIKOVANO DUGME "Potvrdi poziciju"
        Button(
            onClick = {
                val currentFen = board.toFEN()
                Log.d("PositionCreation", "Prosleđujem FEN Solveru: $currentFen")

                // POZIV SOLVERA - SADA KORISTIMO INSTANCU I METODU 'solve'
                val moveList: List<ChessSolver.MoveData>? = chessSolver.solve(board)

                val solution: ChessSolution // Deklaracija promenljive solution
                if (moveList != null && moveList.isNotEmpty()) {
                    // Rešenje pronađeno
                    solution = ChessSolution(
                        isSolved = true,
                        moves = moveList.map { it.toString() }, // Konvertujemo MoveData u String
                        reason = "Rešenje pronađeno."
                    )
                } else {
                    // Nema rešenja. Sada samo prikazujemo generičku poruku bez specifične provere kraljeva.
                    solution = ChessSolution(false, emptyList(), "Nema rešenja za ovu poziciju.")
                }

                if (solution.isSolved) {
                    val intent = Intent(context, SolutionDisplayActivity::class.java).apply {
                        putExtra("puzzleFen", currentFen)
                        putExtra("solutionMoves", ArrayList(solution.moves))
                    }
                    context.startActivity(intent)
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = solution.reason, // Prikazuje "Nema rešenja za ovu poziciju."
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Potvrdi i proveri poziciju")
        }

        Button(
            onClick = {
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Nazad na Odabir Figure")
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}