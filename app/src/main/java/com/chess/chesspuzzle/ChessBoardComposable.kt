package com.chess.chesspuzzle // Prilagodi paketu svoje aplikacije

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// Uvezi ove klase ako su definisane u drugom fajlu (ChessCore.kt)
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square

/**
 * Ponovo iskoristiva Composable komponenta za prikaz šahovske table sa figurama.
 * Ova komponenta je "glupa" i samo prikazuje stanje i obeležava polja.
 * Logika za validaciju poteza ili manipulaciju stanjem table nalazi se izvan nje.
 *
 * @param board Trenutno stanje šahovske table kao ChessBoard objekat.
 * @param selectedSquare Polje koje je trenutno selektovano od strane korisnika. Može biti null.
 * @param highlightedSquares Set polja koja treba da budu dodatno obeležena (npr. mogući potezi).
 * @param onSquareClick Lambda koja se poziva kada korisnik klikne na neko polje, prosleđujući to polje.
 */
@Composable
fun ChessBoardComposable(
    board: ChessBoard,
    selectedSquare: Square?,
    highlightedSquares: Set<Square>, // Dodajemo parametar za obeležavanje
    onSquareClick: (Square) -> Unit // Callback za klik na polje
) {
    val lightSquareColor = Color(0xFFF0D9B5) // Svetlo braon
    val darkSquareColor = Color(0xFFB58863) // Tamno braon

    // Održava kvadratni oblik i zauzima preostali prostor
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0 until 8) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0 until 8) {
                    val currentSquare = Square(file = 'a' + col, rank = 8 - row)

                    val isLightSquare = (row + col) % 2 == 0
                    val squareColor = if (isLightSquare) lightSquareColor else darkSquareColor

                    // Boja za obeležavanje mogućih poteza/validnih destinacija
                    val highlightColor = if (highlightedSquares.contains(currentSquare)) {
                        Color.Green.copy(alpha = 0.3f) // Polu-transparentna zelena
                    } else {
                        Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(squareColor)
                            // Dodaj highlight boju iznad bazne boje polja, ali ispod bordera
                            .background(highlightColor)
                            .border(
                                width = if (currentSquare == selectedSquare) 3.dp else 0.dp,
                                color = if (currentSquare == selectedSquare) Color.Blue else Color.Transparent
                            )
                            .clickable { onSquareClick(currentSquare) }, // Koristi callback za klik
                        contentAlignment = Alignment.Center
                    ) {
                        val piece = board.getPiece(currentSquare)
                        if (piece.type != PieceType.NONE) {
                            val drawableResId = when (Pair(piece.type, piece.color)) {
                                Pair(PieceType.KNIGHT, PieceColor.WHITE) -> R.drawable.wn
                                Pair(PieceType.KNIGHT, PieceColor.BLACK) -> R.drawable.bn // <--- OSIGURAJ SE DA NEMA ZVEZDICA OVDE
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
                                    modifier = Modifier.fillMaxSize(0.8f) // Figura zauzima 80% polja
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}