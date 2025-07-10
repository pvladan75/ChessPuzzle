package com.chess.chesspuzzle

data class SolutionMove(
    val moverPiece: PieceType,
    val initialSquare: Square,
    val moveUCI: String
)

data class ChessProblem(
    val id: Int,
    val difficulty: String,
    val whitePiecesConfig: Map<PieceType, Int>,
    val fen: String,
    val solutionLength: Int,
    val totalBlackCaptured: Int,
    val capturesByPiece: Map<String, Int>,
    val solutionMoves: List<SolutionMove>
)