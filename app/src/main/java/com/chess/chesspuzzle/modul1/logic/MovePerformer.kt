package com.chess.chesspuzzle.modul1.logic

import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.modul1.Difficulty
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.modul1.GameStatusResult
import com.chess.chesspuzzle.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// IZMENJENO: Funkcija sada vraća GameStatusResult i nema više onStatusUpdate callback
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
    targetSquare: Square? = null
): GameStatusResult = withContext(Dispatchers.Default) { // Dodata povratna vrednost
    val pieceToMove = currentBoard.getPiece(fromSquare)
    if (pieceToMove.type == com.chess.chesspuzzle.PieceType.NONE) {
        Log.e("MovePerformer", "Attempted to move a non-existent piece from $fromSquare")
        // U slučaju greške, vratićemo defaultni GameStatusResult ili odgovarajuću grešku
        // Za sada, vraćamo GameStatusResult koji signalizira da se ništa nije promenilo
        return@withContext GameStatusResult(
            updatedBlackPieces = currentBoard.getPiecesMapFromBoard(PieceColor.BLACK),
            puzzleCompleted = false,
            noMoreMoves = false,
            solvedPuzzlesCountIncrement = 0,
            scoreForPuzzle = 0,
            gameStarted = true, // Pretpostavljamo da je igra i dalje u toku
            newSessionScore = currentSessionScore
        )
    }

    // IZMENJENO: Koristimo novu metodu makeMoveAndCapture iz ChessBoard klase
    val newBoard = currentBoard.makeMoveAndCapture(fromSquare, toSquare)

    withContext(Dispatchers.Main) {
        updateBoardState(newBoard) // Ažuriraj stanje table odmah na glavnoj niti
    }

    // Pozivamo checkGameStatusLogic iz GameMechanics
    val statusResult = checkGameStatusLogic(newBoard, currentTimeElapsed, currentDifficulty, playerName, currentSessionScore)

    // IZMENJENO: Umesto pozivanja callbacka, direktno vraćamo statusResult
    return@withContext statusResult
}