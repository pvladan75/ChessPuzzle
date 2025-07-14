package com.chess.chesspuzzle.modul1

import java.util.UUID
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square

data class SolutionMove(
    val moverPiece: PieceType,
    val initialSquare: Square,
    val moveUCI: String
)

data class ChessProblem(
    // Promenjeno: id je sada String i generiše se UUID ako nije definisan
    val id: String = UUID.randomUUID().toString(),
    // Novo: name polje za korisnički definisan naziv zagonetke
    val name: String = "Nova Zagonetka", // Podrazumevano ime
    val difficulty: String,
    val whitePiecesConfig: Map<PieceType, Int>,
    val fen: String,
    val solutionLength: Int,
    val totalBlackCaptured: Int,
    val capturesByPiece: Map<String, Int>,
    val solutionMoves: List<SolutionMove>,
    // Novo: creationDate polje za timestamp
    val creationDate: Long = System.currentTimeMillis()
){
// DODATA HASWHITEQUEEN() METODA
fun hasWhiteQueen(): Boolean {
    // Koristi PieceType.QUEEN za pristup mapi, jer je whitePiecesConfig tipa Map<PieceType, Int>
    return whitePiecesConfig.getOrDefault(PieceType.QUEEN, 0) > 0
}
}