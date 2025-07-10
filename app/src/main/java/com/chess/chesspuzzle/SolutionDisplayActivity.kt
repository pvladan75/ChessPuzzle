package com.chess.chesspuzzle

import android.os.Bundle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import kotlinx.coroutines.delay

// Data klasa za predstavljanje šahovskog problema.
// Pretpostavljamo da je ova klasa već definisana i da radi.
data class ChessProblem(
    val initialFen: String,
    val solutionMoves: List<Pair<Square, Square>> // Lista parova (fromSquare, toSquare)
)

class SolutionDisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Ovo su mock podaci za demonstraciju.
                    // Sada koristimo standardni početni FEN string
                    val mockProblem = remember {
                        ChessProblem(
                            initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // Standardna početna pozicija
                            solutionMoves = listOf(
                                // Beli skakač g1-f3
                                Pair(Square('g', 1), Square('f', 3)),
                                // Crni pešak e7-e5
                                Pair(Square('e', 7), Square('e', 5)),
                                // Beli skakač f3-e5 (pretpostavljamo da je crni pešak na e5)
                                Pair(Square('f', 3), Square('e', 5)),
                                // Crni pešak d7-d6
                                Pair(Square('d', 7), Square('d', 6))
                            )
                        )
                    }
                    SolutionScreen(problem = mockProblem)
                }
            }
        }
    }
}

@Composable
fun SolutionScreen(problem: ChessProblem) {
    // Stanje za trenutnu poziciju na tabli. Inicijalizujemo je sa parsiranim početnim FEN-om.
    var board: ChessBoard by remember { mutableStateOf(ChessCore.parseFenToBoard(problem.initialFen)) }

    // Stanje za indeks trenutnog poteza u rešenju
    var currentMoveIndex by remember { mutableStateOf(-1) } // -1 znači pre prvog poteza (početna pozicija)

    // Stanje za automatsku reprodukciju rešenja
    var isPlaying by remember { mutableStateOf(false) }

    // Stanje za obeležena polja (odakle-dokle potez)
    var highlightedSquares: Set<Square> by remember { mutableStateOf(emptySet()) }

    // Čuvamo kopiju početne table za resetovanje, parsiramo je samo jednom
    val initialSolutionBoard = remember(problem.initialFen) {
        ChessCore.parseFenToBoard(problem.initialFen)
    }

    // LaunchedEffect za kontrolu automatske reprodukcije
    LaunchedEffect(isPlaying, currentMoveIndex) {
        // Logika za automatsku reprodukciju
        if (isPlaying && currentMoveIndex < problem.solutionMoves.size - 1) {
            delay(1500L) // Pauza između poteza (1.5 sekundi)
            currentMoveIndex++ // Idi na sledeći potez
        } else if (isPlaying && currentMoveIndex >= problem.solutionMoves.size - 1) {
            // Ako je reprodukcija završena, zaustavi je
            isPlaying = false
        }
    }

    // DisposableEffect se pokreće kada se currentMoveIndex promeni
    // On ažurira stanje table i obeležava polja trenutnog poteza
    DisposableEffect(currentMoveIndex) {
        if (currentMoveIndex == -1) {
            // Ako je index -1, prikazujemo početnu poziciju (reset)
            board = initialSolutionBoard.copy() // Vraćamo se na originalnu FEN poziciju
            highlightedSquares = emptySet()
        } else if (currentMoveIndex >= 0 && currentMoveIndex < problem.solutionMoves.size) {
            // Ako je validan indeks poteza, simuliramo taj potez
            val (from, to) = problem.solutionMoves[currentMoveIndex]
            val pieceToMove = board.getPiece(from)

            if (pieceToMove.type != PieceType.NONE) {
                // Simulacija poteza: Ukloni figuru sa 'from' polja, postavi na 'to' polje.
                var newBoard = board.removePiece(from)
                // Proveri da li je hvatanje (ako je meta popunjena figurom druge boje)
                val pieceAtTarget = board.getPiece(to)
                if (pieceAtTarget.type != PieceType.NONE && pieceAtTarget.color != pieceToMove.color) {
                    newBoard = newBoard.removePiece(to) // Ukloni uhvaćenu figuru
                }
                newBoard = newBoard.setPiece(to, pieceToMove)
                board = newBoard
                highlightedSquares = setOf(from, to) // Obeleži 'from' i 'to' polja
            } else {
                // Ako figura ne postoji na 'from' polju, to ukazuje na problem u definiciji poteza
                // ili grešku u parsiranju. Očisti obeležavanja i zaustavi reprodukciju.
                highlightedSquares = emptySet()
                isPlaying = false // Zaustavi automatsku reprodukciju
                // Ovde bi se mogla dodati i neka Toast poruka korisniku
            }
        } else {
            // Ako je index van opsega poteza (npr. prešli smo kraj rešenja)
            highlightedSquares = emptySet() // Očisti obeležavanja na kraju
        }
        onDispose { /* Nema posebnog čišćenja ovde */ }
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Rešenje zagonetke",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Prikaz šahovske table
        ChessBoardComposable(
            board = board,
            selectedSquare = null, // Nema selektovanih figura u prikazu rešenja
            highlightedSquares = highlightedSquares, // Obeležavamo polja trenutnog poteza
            onSquareClick = { /* Nema interakcije klikom u prikazu rešenja */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Kontrole za navigaciju kroz rešenje
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    isPlaying = false // Zaustavi automatsku reprodukciju
                    if (currentMoveIndex > -1) {
                        currentMoveIndex-- // Prethodni potez
                    }
                },
                enabled = currentMoveIndex > -1, // Dugme je aktivno samo ako nismo na početku
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Text("Prethodni")
            }

            Button(
                onClick = { isPlaying = !isPlaying }, // Prebaci stanje play/pause
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text(if (isPlaying) "Pauza" else "Play")
            }

            Button(
                onClick = {
                    isPlaying = false // Zaustavi automatsku reprodukciju
                    if (currentMoveIndex < problem.solutionMoves.size - 1) {
                        currentMoveIndex++ // Sledeći potez
                    }
                },
                enabled = currentMoveIndex < problem.solutionMoves.size - 1, // Dugme je aktivno samo ako nismo na kraju
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("Sledeći")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                isPlaying = false // Zaustavi automatsku reprodukciju
                currentMoveIndex = -1 // Vrati na početnu poziciju
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resetuj rešenje")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SolutionScreenPreview() {
    ChessPuzzleTheme {
        val previewProblem = remember {
            ChessProblem(
                initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // Standardna početna pozicija
                solutionMoves = listOf(
                    Pair(Square('g', 1), Square('f', 3)), // Beli skakač g1-f3
                    Pair(Square('d', 7), Square('d', 5)), // Crni pešak d7-d5
                    Pair(Square('f', 3), Square('e', 5)), // Beli skakač uzima crnog pešaka na e5
                    Pair(Square('d', 7), Square('d', 6)) // Crni pešak d7-d6
                )
            )
        }
        SolutionScreen(problem = previewProblem)
    }
}