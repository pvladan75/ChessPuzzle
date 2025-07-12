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
import android.util.Log
import androidx.compose.ui.platform.LocalContext // <-- OSTAVLJAMO OVAJ IMPORT
import java.lang.Exception

// Klasa ChessSolution se očekuje da je definisana globalno,
// npr. u ChessSolver.kt ili ChessDefinitions.kt
// data class ChessSolution(
//     val isSolved: Boolean,
//     val moves: List<String>,
//     val reason: String = ""
// )


class SolutionDisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val puzzleFen = intent.getStringExtra("puzzleFen") ?: ""
        val solutionMoves = intent.getStringArrayListExtra("solutionMoves") ?: arrayListOf()

        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SolutionScreen(
                        initialFen = puzzleFen,
                        solutionMoves = solutionMoves
                    )
                }
            }
        }
    }
}

@Composable
fun SolutionScreen(initialFen: String, solutionMoves: List<String>) {
    // Dohvatamo LocalContext izvan lambda funkcije onClick
    val context = LocalContext.current // <-- NOVO: Dohvatamo kontekst ovde

    var board: ChessBoard by remember(initialFen) {
        mutableStateOf(ChessBoard.parseFenToBoard(initialFen))
    }

    var currentMoveIndex by remember { mutableStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var highlightedSquares: Set<Square> by remember { mutableStateOf(emptySet()) }

    LaunchedEffect(currentMoveIndex, initialFen) {
        if (currentMoveIndex == -1) {
            board = ChessBoard.parseFenToBoard(initialFen)
            highlightedSquares = emptySet()
        }
    }

    LaunchedEffect(isPlaying, currentMoveIndex, solutionMoves) {
        if (isPlaying && currentMoveIndex < solutionMoves.size - 1) {
            delay(1500L)
            currentMoveIndex++
        } else if (isPlaying && currentMoveIndex >= solutionMoves.size - 1) {
            isPlaying = false
        }
    }

    DisposableEffect(currentMoveIndex, solutionMoves) {
        if (currentMoveIndex >= 0 && currentMoveIndex < solutionMoves.size) {
            val moveString = solutionMoves[currentMoveIndex]
            try {
                // Parsiranje poteza za format "e2-e4"
                val parts = moveString.split("-")
                if (parts.size == 2) {
                    val from = Square(parts[0][0], parts[0][1].digitToInt())
                    val to = Square(parts[1][0], parts[1][1].digitToInt())

                    val pieceToMove = board.getPiece(from)

                    if (pieceToMove.type != PieceType.NONE) {
                        board = board.makeMoveAndCapture(from, to)
                        highlightedSquares = setOf(from, to)
                    } else {
                        Log.e("SolutionScreen", "Figura ne postoji na $from za potez $moveString. Prekinuta reprodukcija.")
                        highlightedSquares = emptySet()
                        isPlaying = false
                    }
                } else {
                    Log.e("SolutionScreen", "Nevalidan format poteza '$moveString'. Očekivan format 'e2-e4'.")
                    highlightedSquares = emptySet()
                    isPlaying = false
                }
            } catch (e: Exception) {
                Log.e("SolutionScreen", "Greška pri parsiranju ili primeni poteza '$moveString': ${e.message}")
                highlightedSquares = emptySet()
                isPlaying = false
            }
        }
        onDispose { /* Nema posebnog čišćenja ovde */ }
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "FEN pozicija: $initialFen",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Broj poteza rešenja: ${solutionMoves.size}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (currentMoveIndex >= 0 && currentMoveIndex < solutionMoves.size) {
            Text(
                text = "Trenutni potez: ${solutionMoves[currentMoveIndex]}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Text(
                text = "Pozicija pre prvog poteza.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }


        ChessBoardComposable(
            board = board,
            selectedSquare = null,
            highlightedSquares = highlightedSquares,
            onSquareClick = { }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    isPlaying = false
                    if (currentMoveIndex > -1) {
                        currentMoveIndex--
                    }
                },
                enabled = currentMoveIndex > -1,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Text("Prethodni potez")
            }

            Button(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text(if (isPlaying) "Pauza" else "Play")
            }

            Button(
                onClick = {
                    isPlaying = false
                    if (currentMoveIndex < solutionMoves.size - 1) {
                        currentMoveIndex++
                    }
                },
                enabled = currentMoveIndex < solutionMoves.size - 1,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("Sledeći potez")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                isPlaying = false
                currentMoveIndex = -1
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resetuj rešenje")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            // Korišćenje 'context' promenljive dohvaćene izvan onClick lambda
            onClick = {
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nazad na Kreiranje Pozicije")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SolutionScreenPreview() {
    ChessPuzzleTheme {
        val previewFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        val previewMoves = listOf("e2-e4", "e7-e5", "g1-f3", "b8-c6") // Primer poteza sa crticom

        SolutionScreen(initialFen = previewFen, solutionMoves = previewMoves)
    }
}