// logic/GameMechanics.kt
package com.chess.chesspuzzle.logic

import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.ChessCore
import com.chess.chesspuzzle.Difficulty
import com.chess.chesspuzzle.GameStatusResult
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.ScoreEntry
import com.chess.chesspuzzle.ScoreManager
import com.chess.chesspuzzle.Square
import com.chess.chesspuzzle.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun checkGameStatusLogic(
    currentBoardSnapshot: ChessBoard,
    currentTimeElapsed: Int,
    currentDifficulty: Difficulty,
    playerName: String,
    currentSessionScore: Int
): GameStatusResult = withContext(Dispatchers.Default) {
    val updatedBlackPiecesMap = currentBoardSnapshot.getPiecesMapFromBoard(PieceColor.BLACK)
    var solvedPuzzlesCountIncrement = 0
    var scoreForPuzzle = 0
    var newSessionScore = currentSessionScore
    var puzzleCompleted = false
    var noMoreMoves = false
    var gameStarted = true

    if (updatedBlackPiecesMap.isEmpty()) {
        puzzleCompleted = true
        solvedPuzzlesCountIncrement = 1
        scoreForPuzzle = calculateScoreInternal(currentTimeElapsed, currentDifficulty)
        newSessionScore += scoreForPuzzle

        try {
            ScoreManager.addScore(ScoreEntry(playerName, newSessionScore), currentDifficulty.name)
            Log.d("GameMechanics", "Skor uspešno sačuvan (Zagonetka rešena). Trenutni skor: $newSessionScore")
        } catch (e: Exception) {
            Log.e("GameMechanics", "Greška pri čuvanju skora (Zagonetka rešena): ${e.message}", e)
        }
        gameStarted = false
    } else {
        var canWhiteCaptureBlack = false
        val whitePiecesOnBoard = mutableMapOf<Square, Piece>()
        for (rankIdx in 0 until 8) {
            for (fileIdx in 0 until 8) {
                val square = Square(('a'.code + fileIdx).toChar(), rankIdx + 1)
                val piece = currentBoardSnapshot.getPiece(square)
                if (piece.color == PieceColor.WHITE && piece.type != PieceType.NONE) {
                    whitePiecesOnBoard[square] = piece
                }
            }
        }

        for ((whiteSquare, whitePiece) in whitePiecesOnBoard) {
            val legalMoves = ChessCore.getValidMoves(currentBoardSnapshot, whitePiece, whiteSquare)
            for (move in legalMoves) {
                val pieceAtTarget = currentBoardSnapshot.getPiece(move)
                if (pieceAtTarget.color == PieceColor.BLACK && pieceAtTarget.type != PieceType.NONE) {
                    canWhiteCaptureBlack = true
                    break
                }
            }
            if (canWhiteCaptureBlack) break
        }

        if (!canWhiteCaptureBlack) {
            noMoreMoves = true
            gameStarted = false

            try {
                ScoreManager.addScore(ScoreEntry(playerName, newSessionScore), currentDifficulty.name)
                Log.d("GameMechanics", "Skor uspešno sačuvan (Nema više poteza). Trenutni skor: $newSessionScore")
            } catch (e: Exception) {
                Log.e("GameMechanics", "Greška pri čuvanju skora (Nema više poteza): ${e.message}", e)
            }
        }
    }

    GameStatusResult(
        updatedBlackPieces = updatedBlackPiecesMap,
        puzzleCompleted = puzzleCompleted,
        noMoreMoves = noMoreMoves,
        solvedPuzzlesCountIncrement = solvedPuzzlesCountIncrement,
        scoreForPuzzle = scoreForPuzzle,
        gameStarted = gameStarted,
        newSessionScore = newSessionScore
    )
}

fun calculateScoreInternal(timeInSeconds: Int, currentDifficulty: Difficulty): Int {
    val maxTimeBonusSeconds: Int
    val pointsPerSecond: Int
    val basePointsPerPuzzle: Int

    when (currentDifficulty) {
        Difficulty.EASY -> {
            maxTimeBonusSeconds = 90
            pointsPerSecond = 5
            basePointsPerPuzzle = 300
        }
        Difficulty.MEDIUM -> {
            maxTimeBonusSeconds = 60
            pointsPerSecond = 10
            basePointsPerPuzzle = 600
        }
        Difficulty.HARD -> {
            maxTimeBonusSeconds = 30
            pointsPerSecond = 20
            basePointsPerPuzzle = 1000
        }
    }
    val timePoints = (maxTimeBonusSeconds - timeInSeconds).coerceAtLeast(0) * pointsPerSecond
    return basePointsPerPuzzle + timePoints
}