// ChessBoardUI.kt
package com.chess.chesspuzzle // Double-check this package path

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.* // Import all necessary classes from the main package (ChessDefinitions)

@Composable
fun ChessBoard(
    board: ChessBoard,
    modifier: Modifier = Modifier,
    onSquareClick: (Square) -> Unit = {},
    selectedSquare: Square? = null,
    highlightedMoves: List<Square> = emptyList(),
    lastAttackerSquare: Square? = null
) {
    Column(modifier = modifier.aspectRatio(1f)) {
        for (rankIndex in BOARD_SIZE - 1 downTo 0) { // Od 8. do 1. reda (0 do 7 za index)
            Row(modifier = Modifier.weight(1f)) {
                for (fileIndex in 0 until BOARD_SIZE) { // Od 'a' do 'h' kolone (0 do 7 za index)
                    val square = Square.fromCoordinates(fileIndex, rankIndex)
                    val piece = board.getPiece(square)

                    val isLightSquare = (fileIndex + rankIndex) % 2 == 0
                    val squareColor = if (isLightSquare) Color(0xFFF0D9B5) else Color(0xFFB58863)

                    val backgroundColor = when {
                        square == selectedSquare -> Color.Yellow.copy(alpha = 0.5f)
                        highlightedMoves.contains(square) -> Color.Green.copy(alpha = 0.5f)
                        square == lastAttackerSquare -> Color.Red.copy(alpha = 0.5f)
                        else -> squareColor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(backgroundColor)
                            .border(1.dp, Color.Black)
                            .clickable { onSquareClick(square) }
                    ) {
                        if (piece.type != PieceType.NONE) {
                            Image(
                                painter = painterResource(id = getPieceDrawableResId(piece)),
                                contentDescription = "${piece.color} ${piece.type}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getPieceDrawableResId(piece: Piece): Int {
    return when (piece.color) {
        PieceColor.WHITE -> when (piece.type) {
            PieceType.PAWN -> R.drawable.wp
            PieceType.KNIGHT -> R.drawable.wn
            PieceType.BISHOP -> R.drawable.wb
            PieceType.ROOK -> R.drawable.wr
            PieceType.QUEEN -> R.drawable.wq
            PieceType.KING -> R.drawable.wk
            PieceType.NONE -> 0
        }
        PieceColor.BLACK -> when (piece.type) {
            PieceType.PAWN -> R.drawable.bp
            PieceType.KNIGHT -> R.drawable.bn
            PieceType.BISHOP -> R.drawable.bb
            PieceType.ROOK -> R.drawable.br
            PieceType.QUEEN -> R.drawable.bq
            PieceType.KING -> R.drawable.bk
            PieceType.NONE -> 0 // <--- Već dodato, ali ponovo naglašeno
        }
        PieceColor.NONE -> 0 // Dodato za potpunu iscrpnost
    }
}