package com.chess.chesspuzzle

import java.util.UUID

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
)