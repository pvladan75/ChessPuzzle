// ChessBoardUI.kt
package com.chess.chesspuzzle

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

// Uverite se da je BOARD_SIZE definisan u ChessDefinitions.kt ili sličnom globalnom fajlu
// const val BOARD_SIZE = 8 // Primer, ako nije globalno definisan

@Composable
fun ChessBoard( // Zadržano originalno ime ChessBoard
    board: ChessBoard,
    modifier: Modifier = Modifier,
    onSquareClick: (Square) -> Unit,
    selectedSquare: Square? = null,
    highlightedMoves: List<Square> = emptyList(), // Zadržan List<Square>
    lastAttackerSquare: Square? = null // Zadržan lastAttackerSquare
) {
    Column(
        modifier = modifier
            .aspectRatio(1f) // Osigurava kvadratni oblik table
            .padding(4.dp)   // Padding oko cele table
    ) {
        for (rankIndex in BOARD_SIZE - 1 downTo 0) { // Od reda 8 do 1
            Row(modifier = Modifier.weight(1f)) { // Svaki red zauzima jednak prostor
                for (fileIndex in 0 until BOARD_SIZE) { // Od kolone 'a' do 'h'
                    val square = Square.fromCoordinates(fileIndex, rankIndex)
                    val piece = board.getPiece(square)
                    val isLightSquare = (fileIndex + rankIndex) % 2 == 0
                    val squareColor = if (isLightSquare) Color(0xFFF0D9B5) else Color(0xFFB58863)

                    val backgroundColor = when {
                        square == selectedSquare -> Color(0x6644FF44) // Zelena za selektovano polje (transparentna)
                        highlightedMoves.contains(square) -> Color(0x664488FF) // Plava za istaknute poteze (transparentna)
                        square == lastAttackerSquare -> Color(0x66FF0000) // Crvena za polje napadača (transparentna)
                        else -> squareColor // Uobičajena boja polja
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f) // Svako polje zauzima jednak prostor u redu
                            .fillMaxHeight() // Popunjava visinu reda
                            .background(backgroundColor) // Boja pozadine polja
                            .border( // Tanki okvir oko svakog polja
                                width = 0.5.dp,
                                color = Color.DarkGray.copy(alpha = 0.5f)
                            )
                            .clickable { onSquareClick(square) } // Omogućava klik na polje
                    ) {
                        if (piece.type != PieceType.NONE) {
                            Image(
                                painter = painterResource(id = getPieceDrawableResId(piece)),
                                contentDescription = "${piece.color} ${piece.type}",
                                modifier = Modifier.fillMaxSize() // Slika figure popunjava polje
                            )
                        }
                    }
                }
            }
        }
    }
}

// Funkcija koja mapira figuru na njen resurs slike (drawable)
// Ovo takođe treba da bude u ChessBoardUI.kt ili drugom prikladnom UI fajlu
fun getPieceDrawableResId(piece: Piece): Int {
    return when (piece.color) {
        PieceColor.WHITE -> when (piece.type) {
            PieceType.PAWN -> R.drawable.wp // Ispravljeno: wp
            PieceType.KNIGHT -> R.drawable.wn // Ispravljeno: wn
            PieceType.BISHOP -> R.drawable.wb // Ispravljeno: wb
            PieceType.ROOK -> R.drawable.wr // Ispravljeno: wr
            PieceType.QUEEN -> R.drawable.wq // Ispravljeno: wq
            PieceType.KING -> R.drawable.wk // Ispravljeno: wk
            PieceType.NONE -> 0 // Nema slike za prazno polje
        }
        PieceColor.BLACK -> when (piece.type) {
            PieceType.PAWN -> R.drawable.bp // Ispravljeno: bp
            PieceType.KNIGHT -> R.drawable.bn // Ispravljeno: bn
            PieceType.BISHOP -> R.drawable.bb // Ispravljeno: bb
            PieceType.ROOK -> R.drawable.br // Ispravljeno: br
            PieceType.QUEEN -> R.drawable.bq // Ispravljeno: bq
            PieceType.KING -> R.drawable.bk // Ispravljeno: bk
            PieceType.NONE -> 0
        }
        PieceColor.NONE -> 0
    }
}