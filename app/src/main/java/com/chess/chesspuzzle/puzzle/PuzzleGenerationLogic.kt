// puzzle/PuzzleGenerationLogic.kt
package com.chess.chesspuzzle.puzzle

import android.content.Context
import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Difficulty
import com.chess.chesspuzzle.PuzzleGenerationResult
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.PuzzleGenerator
import com.chess.chesspuzzle.ScoreEntry
import com.chess.chesspuzzle.ScoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun generateNewPuzzleLogic(
    appCtx: Context?,
    difficulty: Difficulty,
    selectedFigures: List<PieceType>,
    minPawns: Int,
    maxPawns: Int,
    gameStarted: Boolean,
    playerName: String,
    currentSessionScore: Int,
    puzzleCompleted: Boolean,
    noMoreMoves: Boolean,
    isTrainingMode: Boolean
): PuzzleGenerationResult = withContext(Dispatchers.Default) {
    if (appCtx == null) {
        Log.e("PuzzleGenerationLogic", "generateNewPuzzleLogic pozvan sa null Context-om. Verovatno u preview modu. Vraćam praznu tablu.")
        return@withContext PuzzleGenerationResult(
            newBoard = ChessBoard.createEmpty(),
            penaltyApplied = 0,
            success = false,
            gameStartedAfterGeneration = false,
            newSessionScore = currentSessionScore
        )
    }

    var penalty = 0
    var updatedSessionScore = currentSessionScore
    var gameStartedAfterGeneration = false

    if (gameStarted && !puzzleCompleted && !noMoreMoves) {
        penalty = 100
        updatedSessionScore = (currentSessionScore - penalty).coerceAtLeast(0)
        try {
            ScoreManager.addScore(ScoreEntry(playerName, updatedSessionScore), difficulty.name)
            Log.d("PuzzleGenerationLogic", "Skor uspešno sačuvan (Zagonetka preskočena, penal -100). Trenutni skor: $updatedSessionScore")
        } catch (e: Exception) {
            Log.e("PuzzleGenerationLogic", "Greška pri čuvanju skora (Zagonetka preskočena): ${e.message}", e)
        }
    }

    var newPuzzleBoard: ChessBoard = ChessBoard.createEmpty()
    var success = false
    try {
        if (isTrainingMode) {
            when (difficulty) {
                Difficulty.EASY -> newPuzzleBoard = PuzzleGenerator.generateEasyRandomPuzzle(appCtx, selectedFigures, minPawns, maxPawns)
                Difficulty.MEDIUM -> newPuzzleBoard = PuzzleGenerator.generateMediumRandomPuzzle(appCtx, selectedFigures, minPawns, maxPawns)
                Difficulty.HARD -> newPuzzleBoard = PuzzleGenerator.generateHardRandomPuzzle(appCtx, selectedFigures, minPawns, maxPawns)
            }
        } else {
            when (difficulty) {
                Difficulty.EASY -> newPuzzleBoard = PuzzleGenerator.loadEasyPuzzleFromJson(appCtx)
                Difficulty.MEDIUM -> newPuzzleBoard = PuzzleGenerator.loadMediumPuzzleFromJson(appCtx)
                Difficulty.HARD -> newPuzzleBoard = PuzzleGenerator.loadHardPuzzleFromJson(appCtx)
            }
        }
        success = newPuzzleBoard.getPiecesMapFromBoard(com.chess.chesspuzzle.PieceColor.WHITE).isNotEmpty() || newPuzzleBoard.getPiecesMapFromBoard(com.chess.chesspuzzle.PieceColor.BLACK).isNotEmpty()
        gameStartedAfterGeneration = success
    } catch (e: Exception) {
        Log.e("PuzzleGenerationLogic", "Greška prilikom generisanja/učitavanja zagonetke: ${e.message}", e)
        success = false
        gameStartedAfterGeneration = false
    }

    PuzzleGenerationResult(
        newBoard = newPuzzleBoard,
        penaltyApplied = penalty,
        success = success,
        gameStartedAfterGeneration = gameStartedAfterGeneration,
        newSessionScore = updatedSessionScore
    )
}