// logic/MovePerformer.kt
package com.chess.chesspuzzle.logic

import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.ChessCore
import com.chess.chesspuzzle.ChessSolver
import com.chess.chesspuzzle.Difficulty
import com.chess.chesspuzzle.GameStatusResult
import com.chess.chesspuzzle.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun performMove(
    fromSquare: Square,
    toSquare: Square,
    currentBoard: ChessBoard,
    updateBoardState: (ChessBoard) -> Unit,
    checkGameStatusLogic: suspend (ChessBoard, Int, Difficulty, String, Int) -> GameStatusResult,
    currentTimeElapsed: Int,
    currentDifficulty: Difficulty,
    playerName: String,
    currentSessionScore: Int,
    capture: Boolean = false,
    targetSquare: Square? = null,
    onStatusUpdate: (GameStatusResult) -> Unit
) = withContext(Dispatchers.Default) {
    val pieceToMove = currentBoard.getPiece(fromSquare)
    if (pieceToMove.type == com.chess.chesspuzzle.PieceType.NONE) {
        Log.e("MovePerformer", "Attempted to move a non-existent piece from $fromSquare")
        return@withContext
    }
    // Koristimo simulateCaptureMove iz ChessCore
    val newBoard = ChessCore.simulateCaptureMove(currentBoard, ChessSolver.MoveData(fromSquare, toSquare))

    withContext(Dispatchers.Main) {
        updateBoardState(newBoard)
    }
    // Pozivamo checkGameStatusLogic iz GameMechanics
    val statusResult = checkGameStatusLogic(newBoard, currentTimeElapsed, currentDifficulty, playerName, currentSessionScore)

    withContext(Dispatchers.Main) {
        onStatusUpdate(statusResult)
    }
}