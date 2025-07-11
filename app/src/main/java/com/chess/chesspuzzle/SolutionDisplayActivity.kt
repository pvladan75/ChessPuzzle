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

// Klase ChessProblem i SolutionMove pretpostavljamo da su sada u zasebnom fajlu,
// na primer u 'com.chess.chesspuzzle.data.ChessProblemData.kt' ili slično.
// U tom slučaju, ovde bi bio potreban import za njih:
// import com.chess.chesspuzzle.data.SolutionMove
// import com.chess.chesspuzzle.data.ChessProblem
// Ako su u istom paketu (com.chess.chesspuzzle) i u zasebnom fajlu, import nije eksplicitno potreban.

class SolutionDisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val allPuzzles = remember {
                        // Pretpostavka: PuzzleLoader je ili unutar com.chess.chesspuzzle paketa
                        // ili je potreban poseban import ako je u podpaketu (npr. .util)
                        PuzzleLoader.loadPuzzlesFromJson(this, "puzzles.json")
                    }

                    if (allPuzzles.isNotEmpty()) {
                        SolutionScreen(puzzles = allPuzzles)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nema učitanih zagonetki. Proverite 'puzzles.json' fajl i njegovu strukturu.",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SolutionScreen(puzzles: List<ChessProblem>) {
    var currentPuzzleIndex by remember { mutableStateOf(0) }

    val currentProblem by remember(puzzles, currentPuzzleIndex) {
        derivedStateOf {
            if (puzzles.isNotEmpty() && currentPuzzleIndex >= 0 && currentPuzzleIndex < puzzles.size) {
                puzzles[currentPuzzleIndex]
            } else {
                ChessProblem(
                    id = 0, difficulty = "N/A", whitePiecesConfig = emptyMap(),
                    fen = "8/8/8/8/8/8/8/8 w - - 0 1",
                    solutionLength = 0, totalBlackCaptured = 0, capturesByPiece = emptyMap(),
                    solutionMoves = emptyList()
                )
            }
        }
    }

    // AŽURIRANO: Koristimo ChessBoard.parseFenToBoard umesto ChessCore.parseFenToBoard
    var board: ChessBoard by remember(currentProblem.fen) {
        mutableStateOf(ChessBoard.parseFenToBoard(currentProblem.fen))
    }

    var currentMoveIndex by remember { mutableStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var highlightedSquares: Set<Square> by remember { mutableStateOf(emptySet()) }

    LaunchedEffect(isPlaying, currentMoveIndex) {
        if (isPlaying && currentMoveIndex < currentProblem.solutionMoves.size - 1) {
            delay(1500L)
            currentMoveIndex++
        } else if (isPlaying && currentMoveIndex >= currentProblem.solutionMoves.size - 1) {
            isPlaying = false
        }
    }

    DisposableEffect(currentMoveIndex, currentProblem) {
        if (currentMoveIndex == -1) {
            // AŽURIRANO: Koristimo ChessBoard.parseFenToBoard umesto ChessCore.parseFenToBoard
            board = ChessBoard.parseFenToBoard(currentProblem.fen)
            highlightedSquares = emptySet()
        } else if (currentMoveIndex >= 0 && currentMoveIndex < currentProblem.solutionMoves.size) {
            val solutionMove = currentProblem.solutionMoves[currentMoveIndex]
            val (from, to) = PuzzleLoader.parseUciToSquares(solutionMove.moveUCI) ?: run {
                Log.e("SolutionScreen", "Nevažeći UCI potez: ${solutionMove.moveUCI} za problem ID ${currentProblem.id}")
                isPlaying = false
                return@DisposableEffect onDispose {}
            }

            val pieceToMove = board.getPiece(from)

            if (pieceToMove.type != PieceType.NONE) {
                var newBoard = board.removePiece(from)
                val pieceAtTarget = board.getPiece(to)
                if (pieceAtTarget.type != PieceType.NONE && pieceAtTarget.color != pieceToMove.color) {
                    newBoard = newBoard.removePiece(to)
                }
                newBoard = newBoard.setPiece(to, pieceToMove)
                board = newBoard
                highlightedSquares = setOf(from, to)
            } else {
                Log.e("SolutionScreen", "Figura ne postoji na ${from} za potez ${solutionMove.moveUCI} u zagonetki ID ${currentProblem.id}. Prekinuta reprodukcija.")
                highlightedSquares = emptySet()
                isPlaying = false
            }
        } else {
            highlightedSquares = emptySet()
        }
        onDispose { /* Nema posebnog čišćenja ovde */ }
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Rešenje zagonetke ${currentPuzzleIndex + 1} / ${puzzles.size}",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "ID: ${currentProblem.id}, Težina: ${currentProblem.difficulty}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Početna pozicija (FEN): ${currentProblem.fen}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Ukupno uhvaćeno crnih: ${currentProblem.totalBlackCaptured}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Dužina rešenja: ${currentProblem.solutionLength} poteza",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
                    currentMoveIndex = -1
                    if (currentPuzzleIndex > 0) {
                        currentPuzzleIndex--
                    }
                },
                enabled = currentPuzzleIndex > 0,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Text("Prethodna zagonetka")
            }

            Button(
                onClick = {
                    isPlaying = false
                    currentMoveIndex = -1
                    if (currentPuzzleIndex < puzzles.size - 1) {
                        currentPuzzleIndex++
                    }
                },
                enabled = currentPuzzleIndex < puzzles.size - 1,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("Sledeća zagonetka")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                    if (currentMoveIndex < currentProblem.solutionMoves.size - 1) {
                        currentMoveIndex++
                    }
                },
                enabled = currentMoveIndex < currentProblem.solutionMoves.size - 1,
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
    }
}

@Preview(showBackground = true)
@Composable
fun SolutionScreenPreview() {
    ChessPuzzleTheme {
        val previewPuzzles = remember {
            listOf(
                ChessProblem(
                    id = 1,
                    difficulty = "Lako",
                    whitePiecesConfig = mapOf(PieceType.BISHOP to 1),
                    fen = "1B3q2/p5p1/7p/8/8/4r3/8/8 w - - 0 1",
                    solutionLength = 5,
                    totalBlackCaptured = 5,
                    capturesByPiece = mapOf("Bishop (b8)" to 5),
                    solutionMoves = listOf(
                        SolutionMove(PieceType.BISHOP, Square('b', 8), "b8a7"),
                        SolutionMove(PieceType.BISHOP, Square('a', 7), "a7e3")
                    )
                ),
                ChessProblem(
                    id = 2,
                    difficulty = "Lako",
                    whitePiecesConfig = mapOf(PieceType.QUEEN to 1),
                    fen = "8/1pn5/1pq5/Q1p5/4p3/8/8/8 w - - 0 1",
                    solutionLength = 6,
                    totalBlackCaptured = 6,
                    capturesByPiece = mapOf("Queen (a5)" to 6),
                    solutionMoves = listOf(
                        SolutionMove(PieceType.QUEEN, Square('a', 5), "a5b6"),
                        SolutionMove(PieceType.QUEEN, Square('b', 6), "b6c6")
                    )
                )
            )
        }
        SolutionScreen(puzzles = previewPuzzles)
    }
}