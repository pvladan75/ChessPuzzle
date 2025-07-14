package com.chess.chesspuzzle.modul2

import android.util.Log
import com.chess.chesspuzzle.*
import java.util.*
import kotlin.random.Random

data class PuzzlePosition(
    val initialBoard: ChessBoard,
    val solutionPath: List<Move>
)

class PositionGenerator {

    private val TAG = "PositionGenerator"
    private val puzzleSolver = PuzzleSolver()

    /**
     * Generiše zagonetku za Modul 2.
     * Generiše tablu sa jednom belom figurom i određenim brojem crnih figura.
     * Osigurava da bela figura nije odmah napadnuta i da postoji rešenje.
     *
     * @param whitePieceType Tip bele figure za poziciju (npr. PieceType.QUEEN, PieceType.ROOK, PieceType.KNIGHT).
     * @param numBlackBishops Broj crnih lovaca.
     * @param numBlackRooks Broj crnih topova.
     * @param numBlackKnights Broj crnih skakača.
     * @param numBlackPawns Broj crnih pešaka.
     * @param maxAttempts Maksimalan broj pokušaja generisanja pre odustajanja.
     * @return Generisana PuzzlePosition.
     * @throws RuntimeException Ako se zagonetka ne može generisati unutar zadatih pokušaja.
     */
    fun generatePuzzle(
        whitePieceType: PieceType,
        numBlackBishops: Int,
        numBlackRooks: Int,
        numBlackKnights: Int,
        numBlackPawns: Int,
        maxAttempts: Int = 1000
    ): PuzzlePosition {
        val random = Random(System.currentTimeMillis())
        var attemptCount = 0

        while (attemptCount < maxAttempts) {
            attemptCount++
            var board = ChessBoard.createEmpty()
            val allSquares = Square.ALL_SQUARES.toMutableList()

            // 1. Postavi BELU FIGURU (DAMA, TOP ili SKAKAČ)
            val whitePieceSquare = allSquares.random(random)
            val whitePiece = Piece(whitePieceType, PieceColor.WHITE)
            board = board.setPiece(whitePiece, whitePieceSquare)
            allSquares.remove(whitePieceSquare)

            Log.d(TAG, "Attempt $attemptCount: Placed white ${whitePieceType} at $whitePieceSquare")

            // 2. Postavi CRNE FIGURE
            val blackPiecesToPlace = mutableListOf<Piece>()
            repeat(numBlackBishops) { blackPiecesToPlace.add(Piece(PieceType.BISHOP, PieceColor.BLACK)) }
            repeat(numBlackRooks) { blackPiecesToPlace.add(Piece(PieceType.ROOK, PieceColor.BLACK)) }
            repeat(numBlackKnights) { blackPiecesToPlace.add(Piece(PieceType.KNIGHT, PieceColor.BLACK)) }
            repeat(numBlackPawns) { blackPiecesToPlace.add(Piece(PieceType.PAWN, PieceColor.BLACK)) }

            blackPiecesToPlace.shuffle(random)

            var currentBoard = board
            val placedBlackSquares = mutableSetOf<Square>()

            for (blackPiece in blackPiecesToPlace) {
                var placed = false
                val shuffledAvailableSquares = allSquares.shuffled(random)

                for (square in shuffledAvailableSquares) {
                    val tempBoard = currentBoard.setPiece(blackPiece, square)

                    val boardWithoutWhitePiece = tempBoard.removePiece(whitePieceSquare)
                    val isWhitePieceAttacked = boardWithoutWhitePiece.isSquareAttackedByAnyOpponent(whitePieceSquare, PieceColor.WHITE) != null

                    // Uklonjena provera napada na belog kralja, jer bela figura u Modulu 2 nije kralj.
                    // if (!isWhitePieceAttacked && !isBlackPieceAttackingWhiteKing) {
                    if (!isWhitePieceAttacked) { // Ažuriran uslov
                        currentBoard = tempBoard
                        allSquares.remove(square)
                        placedBlackSquares.add(square)
                        placed = true
                        break
                    }
                }
                if (!placed) {
                    Log.d(TAG, "Could not safely place black ${blackPiece.type}. Restarting attempt.")
                    break
                }
            }

            // Provera da li su SVE figure zaista postavljene
            if (placedBlackSquares.size != blackPiecesToPlace.size) {
                Log.d(TAG, "Not all black pieces could be placed safely. Retrying.")
                continue
            }

            // 3. Proveri da li je bela figura napadnuta na početnoj poziciji
            val initialBoardWithWhitePiece = currentBoard
            val initialBoardWithoutWhitePiece = initialBoardWithWhitePiece.removePiece(whitePieceSquare)
            val initialAttacker = initialBoardWithoutWhitePiece.isSquareAttackedByAnyOpponent(whitePieceSquare, PieceColor.WHITE)

            if (initialAttacker != null) {
                Log.d(TAG, "White ${whitePieceType} is attacked on initial board by ${initialAttacker.second.type} at ${initialAttacker.first}. Retrying.")
                continue
            }

            // 4. Proveri da li zagonetka ima rešenje (pomoću PuzzleSolvera)
            Log.d(TAG, "Checking if puzzle has a solution...")
            val solutionPath = puzzleSolver.solve(initialBoardWithWhitePiece)

            if (solutionPath != null && solutionPath.isNotEmpty()) {
                Log.d(TAG, "Puzzle generated successfully with white ${whitePieceType} at $whitePieceSquare. Solution found in ${solutionPath.size} moves.")
                return PuzzlePosition(initialBoardWithWhitePiece, solutionPath)
            } else {
                Log.d(TAG, "No solution found for this configuration with white ${whitePieceType}. Retrying.")
            }
        }

        throw RuntimeException("Could not generate a solvable puzzle with the given parameters after $maxAttempts attempts for white piece type: $whitePieceType.")
    }

    private fun ChessBoard.getSquareOfPiece(pieceToFind: Piece): Square? {
        return this.pieces.entries.find { it.value == pieceToFind }?.key
    }
}