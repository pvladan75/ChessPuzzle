package com.chess.chesspuzzle.modul1.logic

import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.ChessCore
import com.chess.chesspuzzle.modul1.Difficulty
import com.chess.chesspuzzle.modul1.GameStatusResult
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.Square
import com.chess.chesspuzzle.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun checkGameStatusLogic(
    currentBoardSnapshot: ChessBoard,
    currentTimeElapsed: Int,
    currentDifficulty: Difficulty,
    playerName: String, // playerName se i dalje prosleđuje, ali se ne koristi za ScoreManager.addScore ovde
    currentSessionScore: Int // Ovo je akumulirani skor pre ove zagonetke
): GameStatusResult = withContext(Dispatchers.Default) {
    val updatedBlackPiecesMap = currentBoardSnapshot.getPiecesMapFromBoard(PieceColor.BLACK)
    var solvedPuzzlesCountIncrement = 0
    var scoreForPuzzle = 0
    var newSessionScore = currentSessionScore // Inicijalizujemo sa prosleđenim akumuliranim skorom
    var puzzleCompleted = false
    var noMoreMoves = false
    var gameStarted = true

    if (updatedBlackPiecesMap.isEmpty()) {
        // Ako su sve crne figure uhvaćene
        puzzleCompleted = true
        solvedPuzzlesCountIncrement = 1
        scoreForPuzzle = calculateScoreInternal(currentTimeElapsed, currentDifficulty)
        newSessionScore += scoreForPuzzle // Dodaj skor ove zagonetke na ukupan skor sesije

        // Uklonjeno: ScoreManager.addScore() se više ne poziva ovde!
        Log.d("GameMechanics", "Zagonetka rešena. Skor za zagonetku: $scoreForPuzzle. Trenutni akumulirani skor sesije: $newSessionScore")
        gameStarted = false // Igra se završava za ovu zagonetku
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

        // Proveri da li bela ima bilo kakav legalan potez za hvatanje crne figure
        for ((whiteSquare, whitePiece) in whitePiecesOnBoard) {
            val legalMoves = ChessCore.getValidMoves(currentBoardSnapshot, whitePiece, whiteSquare)
            for (move in legalMoves) {
                val pieceAtTarget = currentBoardSnapshot.getPiece(move)
                if (pieceAtTarget.color == PieceColor.BLACK && pieceAtTarget.type != PieceType.NONE) {
                    canWhiteCaptureBlack = true
                    break // Pronađen je potez hvatanja, nema potrebe da se proverava dalje
                }
            }
            if (canWhiteCaptureBlack) break // Pronađen je potez hvatanja, izađi iz spoljašnje petlje
        }

        if (!canWhiteCaptureBlack) {
            // Nema više legalnih poteza za hvatanje crnih figura
            noMoreMoves = true
            gameStarted = false
            // Uklonjeno: ScoreManager.addScore() se više ne poziva ovde!
            Log.d("GameMechanics", "Nema više legalnih poteza. Trenutni akumulirani skor sesije: $newSessionScore")
        }
    }

    // VRATI AŽURIRANI SKOR SESIJE
    GameStatusResult(
        updatedBlackPieces = updatedBlackPiecesMap,
        puzzleCompleted = puzzleCompleted,
        noMoreMoves = noMoreMoves,
        solvedPuzzlesCountIncrement = solvedPuzzlesCountIncrement,
        scoreForPuzzle = scoreForPuzzle,
        gameStarted = gameStarted,
        newSessionScore = newSessionScore // Vraćamo ažurirani akumulirani skor
    )
}

fun calculateScoreInternal(timeInSeconds: Int, currentDifficulty: Difficulty): Int {
    val maxTimeBonusSeconds: Int
    val pointsPerSecond: Int
    val basePointsPerPuzzle: Int

    when (currentDifficulty) {
        Difficulty.EASY -> {
            maxTimeBonusSeconds = 10
            pointsPerSecond = 5
            basePointsPerPuzzle = 400
        }
        Difficulty.MEDIUM -> {
            maxTimeBonusSeconds = 10
            pointsPerSecond = 10
            basePointsPerPuzzle = 700
        }
        Difficulty.HARD -> {
            maxTimeBonusSeconds = 20
            pointsPerSecond = 20
            basePointsPerPuzzle = 1000
        }
    }
    val timePoints = (maxTimeBonusSeconds - timeInSeconds) * pointsPerSecond
    return basePointsPerPuzzle + timePoints.coerceAtLeast(-400)
}