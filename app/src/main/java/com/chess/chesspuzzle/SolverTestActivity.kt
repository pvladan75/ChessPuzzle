package com.chess.chesspuzzle

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import com.chess.chesspuzzle.modul2.PuzzleSolver // Import za Modul2 solver
import kotlinx.coroutines.delay
import com.chess.chesspuzzle.modul1.ChessBoardComposable

// --- SolverTestActivity klasa ---
class SolverTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SolverTestScreen()
                }
            }
        }
    }
}

// --- SolverTestScreen Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolverTestScreen() {
    val TAG = "SolverTestScreen"

    // Stanja za FEN input
    var fenInput by remember { mutableStateOf("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") } // Standardna početna pozicija
    var currentBoard: ChessBoard by remember { mutableStateOf(ChessBoard.parseFenToBoard(fenInput)) }

    // Stanja za solver i rešenje
    var solutionMoves: List<Move> by remember { mutableStateOf(emptyList()) }
    var currentMoveIndex by remember { mutableStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var highlightedSquares: Set<Square> by remember { mutableStateOf(emptySet()) }
    var solvingMessage by remember { mutableStateOf("") }

    // Stanje za odabir solvera (npr. "Modul 2 Solver")
    var selectedSolver by remember { mutableStateOf("Modul 2 Solver") } // Podrazumevano
    val solverOptions = listOf("Modul 2 Solver", "Modul 1 Solver (Nije implementiran)", "Modul 3 Solver (Nije implementiran)")
    var expanded by remember { mutableStateOf(false) } // Za DropdownMenu

    // Prikaz table na osnovu trenutnog poteza
    LaunchedEffect(currentMoveIndex, solutionMoves) {
        if (currentMoveIndex == -1) {
            // Resetuj na početnu FEN poziciju ako nema poteza ili je resetovano
            currentBoard = ChessBoard.parseFenToBoard(fenInput)
            highlightedSquares = emptySet()
        } else if (currentMoveIndex >= 0 && currentMoveIndex < solutionMoves.size) {
            // Primeni trenutni potez
            val move = solutionMoves[currentMoveIndex]
            currentBoard = ChessBoard.parseFenToBoard(fenInput) // Uvek resetuj tablu
            for (i in 0..currentMoveIndex) { // Primeni sve poteze do trenutnog
                val m = solutionMoves[i]
                currentBoard = currentBoard.makeMoveAndCapture(m.start, m.end)
            }
            highlightedSquares = setOf(move.start, move.end)
        } else {
            isPlaying = false // Zaustavi reprodukciju ako smo došli do kraja
        }
    }

    // Automatska reprodukcija poteza
    LaunchedEffect(isPlaying, currentMoveIndex, solutionMoves) {
        if (isPlaying && currentMoveIndex < solutionMoves.size - 1) {
            delay(1500L) // Kašnjenje između poteza
            currentMoveIndex++
        } else if (isPlaying && currentMoveIndex >= solutionMoves.size - 1) {
            isPlaying = false
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // FEN Unos
        OutlinedTextField(
            value = fenInput,
            onValueChange = { fenInput = it },
            label = { Text("Unesite FEN poziciju") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            singleLine = true
        )

        // Odabir solvera
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            OutlinedTextField(
                value = selectedSolver,
                onValueChange = {},
                readOnly = true,
                label = { Text("Odaberite Solver") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                solverOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedSolver = option
                            expanded = false
                        },
                        enabled = option == "Modul 2 Solver" // Samo Modul 2 je trenutno aktivan
                    )
                }
            }
        }

        // Dugme za pokretanje solvera
        Button(
            onClick = {
                solvingMessage = "Rešavam..."
                solutionMoves = emptyList()
                currentMoveIndex = -1
                isPlaying = false
                highlightedSquares = emptySet()
                try {
                    val initialBoardForSolver = ChessBoard.parseFenToBoard(fenInput)
                    // Pozivanje odgovarajućeg solvera na osnovu izbora
                    val solver = PuzzleSolver() // Kreiramo instancu PuzzleSolvera iz Modula 2
                    val solvedMoves = solver.solve(initialBoardForSolver)

                    if (solvedMoves != null) {
                        solutionMoves = solvedMoves
                        solvingMessage = "Rešenje pronađeno! ${solutionMoves.size} poteza."
                        Log.d(TAG, "Solver solution: ${solutionMoves.joinToString()}")
                    } else {
                        solvingMessage = "Nije pronađeno rešenje za ovu poziciju."
                        Log.w(TAG, "Solver found no solution for: $fenInput")
                    }
                } catch (e: Exception) {
                    solvingMessage = "Greška pri parsiranju FEN-a ili pokretanju solvera: ${e.message}"
                    Log.e(TAG, "Solver error: ${e.message}", e)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Reši Zagonetku")
        }

        // Poruka o rešavanju
        Text(text = solvingMessage, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))

        // Prikaz broja poteza rešenja
        if (solutionMoves.isNotEmpty()) {
            Text(
                text = "Poteza rešenja: ${solutionMoves.size}",
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
        } else {
            Text(
                text = "Unesite FEN i pritisnite 'Reši Zagonetku'",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Šahovska tabla Composable
        ChessBoardComposable(
            board = currentBoard,
            selectedSquare = null,
            highlightedSquares = highlightedSquares,
            onSquareClick = { /* Nema interakcije na tabli u ovom modu */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Kontrole za reprodukciju poteza
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
                enabled = currentMoveIndex > -1 && solutionMoves.isNotEmpty(),
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Text("Prethodni")
            }

            Button(
                onClick = { isPlaying = !isPlaying },
                enabled = solutionMoves.isNotEmpty(),
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
                enabled = currentMoveIndex < solutionMoves.size - 1 && solutionMoves.isNotEmpty(),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("Sledeći")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                isPlaying = false
                currentMoveIndex = -1
                solutionMoves = emptyList() // Resetuj i rešenje
                solvingMessage = ""
            },
            enabled = solutionMoves.isNotEmpty() || currentMoveIndex != -1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resetuj prikaz")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dugme za povratak
        val context = LocalContext.current // Koristimo LocalContext
        Button(
            onClick = {
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nazad na Glavni Meni")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SolverTestScreenPreview() {
    ChessPuzzleTheme {
        SolverTestScreen()
    }
}