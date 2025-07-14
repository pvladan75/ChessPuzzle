package com.chess.chesspuzzle.modul1 // Prilagodi paketu svoje aplikacije

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
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.R
import com.chess.chesspuzzle.Square

/**
 * Ponovo iskoristiva Composable komponenta za prikaz šahovske table sa figurama.
 * Ova komponenta je "glupa" i samo prikazuje stanje i obeležava polja.
 * Logika za validaciju poteza ili manipulaciju stanjem table nalazi se izvan nje.
 *
 * @param board Trenutno stanje šahovske table kao ChessBoard objekat.
 * @param selectedSquare Polje koje je trenutno selektovano od strane korisnika. Može biti null.
 * @param highlightedSquares Set polja koja treba da budu dodatno obeležena (npr. mogući potezi).
 * @param capturedTargetSquares Set polja koja su ciljna za hvatanje, da budu specifično obeležena.
 * @param onSquareClick Lambda koja se poziva kada korisnik klikne na neko polje, prosleđujući to polje.
 */
@Composable
fun ChessBoardComposable(
    board: ChessBoard,
    selectedSquare: Square?,
    highlightedSquares: Set<Square>,
    capturedTargetSquares: Set<Square> = emptySet(), // NOVO: Parametar za ciljna polja hvatanja
    onSquareClick: (Square) -> Unit
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
                    val squareBaseColor = if (isLightSquare) lightSquareColor else darkSquareColor

                    // Prioritet obeležavanja: Selektovano > Ciljno za hvatanje > Mogući potez
                    // Ovo osigurava da selektovano polje ima plavi border, ciljno žuti, a mogući potez zeleni,
                    // bez preklapanja boja pozadine (koje su transparentne ili polu-transparentne).
                    val highlightModifier = Modifier.run {
                        if (currentSquare == selectedSquare) {
                            // Selektovano polje ima samo plavi border, pozadina je bazna boja
                            this
                        } else if (capturedTargetSquares.contains(currentSquare)) {
                            // Ciljno polje za hvatanje - crvena pozadina (ili neka druga uočljiva)
                            background(Color.Red.copy(alpha = 0.4f)) // Malo transparentnija crvena
                        } else if (highlightedSquares.contains(currentSquare)) {
                            // Mogući potez - zelena pozadina
                            background(Color.Green.copy(alpha = 0.3f))
                        } else {
                            this // Bez dodatnog obeležavanja pozadine
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(squareBaseColor) // Bazna boja polja
                            .then(highlightModifier) // Primeni uslovno obeležavanje pozadine
                            .border(
                                width = if (currentSquare == selectedSquare) 3.dp else 0.dp,
                                color = if (currentSquare == selectedSquare) Color.Blue else Color.Transparent // Border samo za selektovano
                            )
                            .clickable { onSquareClick(currentSquare) },
                        contentAlignment = Alignment.Center
                    ) {
                        val piece = board.getPiece(currentSquare)
                        if (piece.type != PieceType.NONE) {
                            val drawableResId = getPieceDrawable(piece.type, piece.color)
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
}

/**
 * Pomoćna funkcija za dobijanje ID-a drawable resursa za datu figuru.
 * Izdvajanje ove logike čini Composable funkciju čitljivijom.
 */
@Composable
private fun getPieceDrawable(pieceType: PieceType, pieceColor: PieceColor): Int {
    return when (Pair(pieceType, pieceColor)) {
        Pair(PieceType.KNIGHT, PieceColor.WHITE) -> R.drawable.wn
        Pair(PieceType.KNIGHT, PieceColor.BLACK) -> R.drawable.bn
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
}

// NAPOMENA: R.drawable.bn je bio zakomentarisan sa zvezdicom, što je možda bila greška.
// Proveri da li imaš drawable resurs bn (black knight) i da li je ispravno imenovan.
// Uklonio sam zvezdicu da bi linija bila aktivna.