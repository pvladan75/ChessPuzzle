// TrainingPuzzleManager.kt
package com.chess.chesspuzzle

import android.content.Context
import android.util.Log
import kotlin.random.Random

object TrainingPuzzleManager {

    private const val TAG = "TrainingPuzzleManager"

    /**
     * Generiše nasumičnu laku zagonetku za trening mod.
     * Postavlja 1 belu figuru (od odabranih) i crne figure (targete) na putanji bele figure.
     */
    fun generateEasyRandomPuzzle(selectedFigures: List<PieceType>, minTotalPawns: Int, maxTotalPawns: Int): ChessBoard {
        Log.d(TAG, "$TAG: Easy: Starting RANDOM puzzle generation. Selected figures: $selectedFigures, Pawns: $minTotalPawns-$maxTotalPawns")
        val maxAttempts = 2000
        val minMovesPerPiece = 1
        val maxMovesPerPiece = 3

        val numWhitePiecesForEasy = 1
        val figuresToUse = if (selectedFigures.isEmpty()) {
            listOf(PieceType.KNIGHT).shuffled(Random.Default).take(numWhitePiecesForEasy)
        } else {
            selectedFigures.shuffled(Random.Default).take(numWhitePiecesForEasy.coerceAtMost(selectedFigures.size))
        }

        if (figuresToUse.isEmpty()) {
            Log.e(TAG, "$TAG: Easy: No valid figures selected or default figure failed. Cannot generate puzzle.")
            return ChessBoard.createEmpty()
        }

        val random = Random.Default

        for (attempt in 1..maxAttempts) {
            var finalPuzzleBoard = ChessBoard.createEmpty()
            val occupiedSquares = mutableSetOf<Square>() // Resetuje se na početku svakog pokušaja

            Log.d(TAG, "Attempt $attempt: Initializing for Easy puzzle. occupiedSquares is empty: ${occupiedSquares.isEmpty()}")

            val allPassThroughSquares = mutableSetOf<Square>()
            val initialPositions = mutableMapOf<PieceType, Square>()
            val targetSquaresForBlackPieces = mutableSetOf<Square>()

            var generationSuccessful = true
            val allGeneratedPaths = mutableListOf<List<Square>>()

            // 1. Postavi odabranu belu figuru
            val whitePieceType = figuresToUse[0]
            val whitePiece = Piece(whitePieceType, PieceColor.WHITE)
            val startSquare = findRandomEmptySquare(finalPuzzleBoard, occupiedSquares)
            if (startSquare == null) {
                Log.d(TAG, "Attempt $attempt: Failed to find empty square for white piece. Retrying.")
                generationSuccessful = false
                continue
            }

            finalPuzzleBoard = finalPuzzleBoard.setPiece(whitePiece,startSquare)
            occupiedSquares.add(startSquare)
            initialPositions[whitePieceType] = startSquare
            Log.d(TAG, "Attempt $attempt: White piece ${whitePiece.type} placed at $startSquare. occupiedSquares: $occupiedSquares")


            // 2. Generiši putanju za belu figuru
            val numMovesForThisPiece = random.nextInt(minMovesPerPiece, maxMovesPerPiece + 1)
            val pathSegments = ChessCore.generatePiecePath(finalPuzzleBoard, whitePiece, startSquare, numMovesForThisPiece)

            if (pathSegments.size != numMovesForThisPiece + 1) {
                Log.d(TAG, "Attempt $attempt: Path segments size (${pathSegments.size}) mismatch with expected moves (${numMovesForThisPiece + 1}). Retrying.")
                generationSuccessful = false
                continue
            }

            allGeneratedPaths.add(pathSegments)

            for (i in 1 until pathSegments.size) {
                val currentTargetSquare = pathSegments[i]
                if (initialPositions.values.contains(currentTargetSquare)) {
                    Log.d(TAG, "Attempt $attempt: Current target square $currentTargetSquare is an initial position. Retrying.")
                    generationSuccessful = false
                    break
                }
                targetSquaresForBlackPieces.add(currentTargetSquare)
            }
            if (!generationSuccessful) continue
            Log.d(TAG, "Attempt $attempt: Target squares for black pieces: $targetSquaresForBlackPieces")


            var currentPathPos: Square = startSquare
            for (i in 1 until pathSegments.size) {
                val moveTarget: Square = pathSegments[i]
                val passThrough = ChessCore.getSquaresBetween(currentPathPos, moveTarget)
                if (whitePieceType != PieceType.KNIGHT) {
                    allPassThroughSquares.addAll(passThrough)
                }
                currentPathPos = moveTarget
            }
            Log.d(TAG, "Attempt $attempt: All pass-through squares: $allPassThroughSquares")


            for (passThroughSquare in allPassThroughSquares) {
                if (initialPositions.values.contains(passThroughSquare)) continue
                if (targetSquaresForBlackPieces.contains(passThroughSquare)) continue

                if (finalPuzzleBoard.getPiece(passThroughSquare).type != PieceType.NONE || occupiedSquares.contains(passThroughSquare)) {
                    Log.d(TAG, "Attempt $attempt: Conflict: Pass-through square $passThroughSquare is unexpectedly occupied. Retrying.")
                    generationSuccessful = false
                    break
                }
            }
            if (!generationSuccessful) continue

            if (targetSquaresForBlackPieces.size < minTotalPawns || targetSquaresForBlackPieces.size > maxTotalPawns) {
                Log.d(TAG, "Attempt $attempt: Number of target squares (${targetSquaresForBlackPieces.size}) out of pawn range ($minTotalPawns-$maxTotalPawns). Retrying.")
                generationSuccessful = false
                continue
            }

            val availableBlackPieceTypes = mutableListOf(
                PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN
            )

            for (targetSquare in targetSquaresForBlackPieces) {
                if (finalPuzzleBoard.getPiece(targetSquare).type != PieceType.NONE || occupiedSquares.contains(targetSquare)) {
                    Log.d(TAG, "Attempt $attempt: Conflict: Target square $targetSquare is already occupied before placing black piece. Retrying.")
                    generationSuccessful = false
                    break
                }
                val blackPieceType = availableBlackPieceTypes.random(random)
                finalPuzzleBoard = finalPuzzleBoard.setPiece(Piece(blackPieceType, PieceColor.BLACK), targetSquare)
                occupiedSquares.add(targetSquare)
                Log.d(TAG, "Attempt $attempt: Black piece ${blackPieceType} placed at $targetSquare. occupiedSquares: $occupiedSquares")
            }
            if (!generationSuccessful) continue

            val blackPiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
            val whitePiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).size

            // Dodatni logovi za debagovanje - konačno stanje figure na tabli
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - White pieces found on board: ${whitePiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: White Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Black pieces found on board: ${blackPiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: Black Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Total occupied squares: $occupiedSquares")


            if (blackPiecesCount > 0 && whitePiecesCount > 0 &&
                blackPiecesCount >= minTotalPawns && blackPiecesCount <= maxTotalPawns
            ) {
                Log.d(TAG, "$TAG: Easy (Random): Successfully generated puzzle after $attempt attempts. FEN: ${finalPuzzleBoard.toFEN()}")
                Log.d(TAG, "Generated Path (for solver validation): ${allGeneratedPaths.joinToString { path -> path.joinToString { it.toString() } }}")
                return finalPuzzleBoard
            }
        }
        Log.e(TAG, "$TAG: Easy (Random): Failed to GENERATE Easy puzzle after $maxAttempts attempts. Returning empty board.")
        return ChessBoard.createEmpty()
    }

    /**
     * Generiše nasumičnu srednju zagonetku za trening mod.
     */
    fun generateMediumRandomPuzzle(selectedFigures: List<PieceType>, minTotalPawns: Int, maxTotalPawns: Int): ChessBoard {
        Log.d(TAG, "$TAG: Medium: Starting RANDOM puzzle generation. Selected figures: $selectedFigures, Pawns: $minTotalPawns-$maxTotalPawns")
        val maxAttempts = 2000
        val minMovesPerPiece = 2
        val maxMovesPerPiece = 4

        val numWhitePiecesForMedium = Random.nextInt(1, 3)
        val figuresToUse = if (selectedFigures.isEmpty()) {
            listOf(PieceType.QUEEN, PieceType.ROOK).shuffled(Random.Default).take(numWhitePiecesForMedium)
        } else {
            selectedFigures.shuffled(Random.Default).take(numWhitePiecesForMedium.coerceAtMost(selectedFigures.size))
        }

        if (figuresToUse.isEmpty()) {
            Log.e(TAG, "$TAG: Medium: No valid figures selected or default figure failed. Cannot generate puzzle.")
            return ChessBoard.createEmpty()
        }

        val random = Random.Default

        for (attempt in 1..maxAttempts) {
            var finalPuzzleBoard = ChessBoard.createEmpty()
            val occupiedSquares = mutableSetOf<Square>() // Resetuje se na početku svakog pokušaja

            Log.d(TAG, "Attempt $attempt: Initializing for Medium puzzle. occupiedSquares is empty: ${occupiedSquares.isEmpty()}")


            val allPassThroughSquares = mutableSetOf<Square>()
            val initialPositions = mutableMapOf<PieceType, Square>()
            val targetSquaresForBlackPieces = mutableSetOf<Square>()

            var generationSuccessful = true
            val allGeneratedPaths = mutableListOf<List<Square>>()

            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = findRandomEmptySquare(finalPuzzleBoard, occupiedSquares)
                if (startSquare == null) {
                    Log.d(TAG, "Attempt $attempt: Failed to find empty square for white piece ${pieceType}. Retrying.")
                    generationSuccessful = false
                    break
                }

                finalPuzzleBoard = finalPuzzleBoard.setPiece(whitePiece, startSquare)
                occupiedSquares.add(startSquare)
                initialPositions[pieceType] = startSquare
                Log.d(TAG, "Attempt $attempt: White piece ${whitePiece.type} placed at $startSquare. occupiedSquares: $occupiedSquares")

            }
            if (!generationSuccessful) continue

            for (entry in initialPositions) {
                val pieceType = entry.key
                val startSquare = entry.value
                val whitePiece = Piece(pieceType, PieceColor.WHITE)

                val numMovesForThisPiece = random.nextInt(minMovesPerPiece, maxMovesPerPiece + 1)
                val pathSegments = ChessCore.generatePiecePath(finalPuzzleBoard, whitePiece, startSquare, numMovesForThisPiece)

                if (pathSegments.size != numMovesForThisPiece + 1) {
                    Log.d(TAG, "Attempt $attempt: Path segments size (${pathSegments.size}) mismatch for ${pieceType} with expected moves (${numMovesForThisPiece + 1}). Retrying.")
                    generationSuccessful = false
                    break
                }
                allGeneratedPaths.add(pathSegments)

                for (i in 1 until pathSegments.size) {
                    val currentTargetSquare = pathSegments[i]
                    if (initialPositions.values.contains(currentTargetSquare)) {
                        Log.d(TAG, "Attempt $attempt: Current target square ${currentTargetSquare} is an initial position for ${pieceType}. Retrying.")
                        generationSuccessful = false
                        break
                    }
                    targetSquaresForBlackPieces.add(currentTargetSquare)
                }
                if (!generationSuccessful) continue

                var currentPathPos: Square = startSquare
                for (i in 1 until pathSegments.size) {
                    val moveTarget: Square = pathSegments[i]
                    val passThrough = ChessCore.getSquaresBetween(currentPathPos, moveTarget)
                    if (pieceType != PieceType.KNIGHT) {
                        allPassThroughSquares.addAll(passThrough)
                    }
                    currentPathPos = moveTarget
                }
            }
            if (!generationSuccessful) continue
            Log.d(TAG, "Attempt $attempt: Target squares for black pieces: $targetSquaresForBlackPieces")
            Log.d(TAG, "Attempt $attempt: All pass-through squares: $allPassThroughSquares")


            if (targetSquaresForBlackPieces.size < minTotalPawns || targetSquaresForBlackPieces.size > maxTotalPawns) {
                Log.d(TAG, "Attempt $attempt: Number of target squares (${targetSquaresForBlackPieces.size}) out of pawn range ($minTotalPawns-$maxTotalPawns). Retrying.")
                generationSuccessful = false
                continue
            }

            for (passThroughSquare in allPassThroughSquares) {
                if (initialPositions.values.contains(passThroughSquare)) continue
                if (targetSquaresForBlackPieces.contains(passThroughSquare)) continue

                if (finalPuzzleBoard.getPiece(passThroughSquare).type != PieceType.NONE || occupiedSquares.contains(passThroughSquare)) {
                    Log.d(TAG, "Attempt $attempt: Conflict: Pass-through square $passThroughSquare is unexpectedly occupied. Retrying.")
                    generationSuccessful = false
                    break
                }
            }
            if (!generationSuccessful) continue

            val availableBlackPieceTypes = mutableListOf(
                PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN
            )

            for (targetSquare in targetSquaresForBlackPieces) {
                if (finalPuzzleBoard.getPiece(targetSquare).type != PieceType.NONE || occupiedSquares.contains(targetSquare)) {
                    Log.d(TAG, "Attempt $attempt: Conflict: Target square $targetSquare is already occupied before placing black piece. Retrying.")
                    generationSuccessful = false
                    break
                }
                val blackPieceType = availableBlackPieceTypes.random(random)
                finalPuzzleBoard = finalPuzzleBoard.setPiece( Piece(blackPieceType, PieceColor.BLACK), targetSquare)
                occupiedSquares.add(targetSquare)
                Log.d(TAG, "Attempt $attempt: Black piece ${blackPieceType} placed at $targetSquare. occupiedSquares: $occupiedSquares")

            }
            if (!generationSuccessful) continue

            val blackPiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
            val whitePiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).size

            // Dodatni logovi za debagovanje - konačno stanje figure na tabli
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - White pieces found on board: ${whitePiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: White Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Black pieces found on board: ${blackPiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: Black Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Total occupied squares: $occupiedSquares")


            if (blackPiecesCount > 0 && whitePiecesCount > 0 &&
                blackPiecesCount >= minTotalPawns && blackPiecesCount <= maxTotalPawns
            ) {
                Log.d(TAG, "$TAG: Medium (Random): Successfully generated puzzle after $attempt attempts. FEN: ${finalPuzzleBoard.toFEN()}")
                Log.d(TAG, "Generated Path (for solver validation): ${allGeneratedPaths.joinToString { path -> path.joinToString { it.toString() } }}")
                return finalPuzzleBoard
            }
        }
        Log.e(TAG, "$TAG: Medium (Random): Failed to GENERATE Medium puzzle after $maxAttempts attempts. Returning empty board.")
        return ChessBoard.createEmpty()
    }

    /**
     * Generiše nasumičnu tešku zagonetku za trening mod.
     */
    fun generateHardRandomPuzzle(selectedFigures: List<PieceType>, minTotalPawns: Int, maxTotalPawns: Int): ChessBoard {
        Log.d(TAG, "$TAG: Hard: Starting RANDOM puzzle generation. Selected figures: $selectedFigures, Pawns: $minTotalPawns-$maxTotalPawns")
        val maxAttempts = 3000
        val minMovesPerPiece = 3
        val maxMovesPerPiece = 6

        val numWhitePiecesForHard = Random.nextInt(2, 4)
        val figuresToUse = if (selectedFigures.isEmpty()) {
            mutableListOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).shuffled(Random.Default).take(numWhitePiecesForHard)
        } else {
            selectedFigures.shuffled(Random.Default).take(numWhitePiecesForHard.coerceAtMost(selectedFigures.size))
        }

        if (figuresToUse.isEmpty()) {
            Log.e(TAG, "$TAG: Hard: No valid figures selected or default figures failed. Cannot generate puzzle.")
            return ChessBoard.createEmpty()
        }

        val random = Random.Default

        for (attempt in 1..maxAttempts) {
            var finalPuzzleBoard = ChessBoard.createEmpty()
            val occupiedSquares = mutableSetOf<Square>() // Resetuje se na početku svakog pokušaja

            Log.d(TAG, "Attempt $attempt: Initializing for Hard puzzle. occupiedSquares is empty: ${occupiedSquares.isEmpty()}")


            val allPassThroughSquares = mutableSetOf<Square>()
            val initialPositions = mutableMapOf<PieceType, Square>()
            val targetSquaresForBlackPieces = mutableSetOf<Square>()

            var generationSuccessful = true
            val allGeneratedPaths = mutableListOf<List<Square>>()

            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = findRandomEmptySquare(finalPuzzleBoard, occupiedSquares)
                if (startSquare == null) {
                    Log.d(TAG, "Attempt $attempt: Failed to find empty square for white piece ${pieceType}. Retrying.")
                    generationSuccessful = false
                    break
                }

                finalPuzzleBoard = finalPuzzleBoard.setPiece(whitePiece, startSquare)
                occupiedSquares.add(startSquare)
                initialPositions[pieceType] = startSquare
                Log.d(TAG, "Attempt $attempt: White piece ${whitePiece.type} placed at $startSquare. occupiedSquares: $occupiedSquares")

            }
            if (!generationSuccessful) continue

            for (entry in initialPositions) {
                val pieceType = entry.key
                val startSquare = entry.value
                val whitePiece = Piece(pieceType, PieceColor.WHITE)

                val numMovesForThisPiece = random.nextInt(minMovesPerPiece, maxMovesPerPiece + 1)
                val pathSegments = ChessCore.generatePiecePath(finalPuzzleBoard, whitePiece, startSquare, numMovesForThisPiece)

                if (pathSegments.size != numMovesForThisPiece + 1) {
                    Log.d(TAG, "Attempt $attempt: Path segments size (${pathSegments.size}) mismatch for ${pieceType} with expected moves (${numMovesForThisPiece + 1}). Retrying.")
                    generationSuccessful = false
                    break
                }
                allGeneratedPaths.add(pathSegments)

                for (i in 1 until pathSegments.size) {
                    val currentTargetSquare = pathSegments[i]
                    if (initialPositions.values.contains(currentTargetSquare)) {
                        Log.d(TAG, "Attempt $attempt: Current target square ${currentTargetSquare} is an initial position for ${pieceType}. Retrying.")
                        generationSuccessful = false
                        break
                    }
                    targetSquaresForBlackPieces.add(currentTargetSquare)
                }
                if (!generationSuccessful) continue

                var currentPathPos: Square = startSquare
                for (i in 1 until pathSegments.size) {
                    val moveTarget: Square = pathSegments[i]
                    val passThrough = ChessCore.getSquaresBetween(currentPathPos, moveTarget)
                    if (pieceType != PieceType.KNIGHT) {
                        allPassThroughSquares.addAll(passThrough)
                    }
                    currentPathPos = moveTarget
                }
            }
            if (!generationSuccessful) continue
            Log.d(TAG, "Attempt $attempt: Target squares for black pieces: $targetSquaresForBlackPieces")
            Log.d(TAG, "Attempt $attempt: All pass-through squares: $allPassThroughSquares")


            if (targetSquaresForBlackPieces.size < minTotalPawns || targetSquaresForBlackPieces.size > maxTotalPawns) {
                Log.d(TAG, "Attempt $attempt: Number of target squares (${targetSquaresForBlackPieces.size}) out of pawn range ($minTotalPawns-$maxTotalPawns). Retrying.")
                generationSuccessful = false
                continue
            }

            for (passThroughSquare in allPassThroughSquares) {
                if (initialPositions.values.contains(passThroughSquare)) continue
                if (targetSquaresForBlackPieces.contains(passThroughSquare)) continue

                if (finalPuzzleBoard.getPiece(passThroughSquare).type != PieceType.NONE || occupiedSquares.contains(passThroughSquare)) {
                    Log.d(TAG, "Attempt $attempt: Conflict: Pass-through square $passThroughSquare is unexpectedly occupied. Retrying.")
                    generationSuccessful = false
                    break
                }
            }
            if (!generationSuccessful) continue

            val availableBlackPieceTypes = mutableListOf(
                PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN
            )

            for (targetSquare in targetSquaresForBlackPieces) {
                if (finalPuzzleBoard.getPiece(targetSquare).type != PieceType.NONE || occupiedSquares.contains(targetSquare)) {
                    Log.d(TAG, "Attempt $attempt: Conflict: Target square $targetSquare is already occupied before placing black piece. Retrying.")
                    generationSuccessful = false
                    break
                }
                val blackPieceType = availableBlackPieceTypes.random(random)
                finalPuzzleBoard = finalPuzzleBoard.setPiece(Piece(blackPieceType, PieceColor.BLACK), targetSquare)
                occupiedSquares.add(targetSquare)
                Log.d(TAG, "Attempt $attempt: Black piece ${blackPieceType} placed at $targetSquare. occupiedSquares: $occupiedSquares")

            }
            if (!generationSuccessful) continue

            val blackPiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
            val whitePiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).size

            // Dodatni logovi za debagovanje - konačno stanje figure na tabli
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - White pieces found on board: ${whitePiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: White Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Black pieces found on board: ${blackPiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: Black Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Total occupied squares: $occupiedSquares")


            if (blackPiecesCount > 0 && whitePiecesCount > 0 &&
                blackPiecesCount >= minTotalPawns && blackPiecesCount <= maxTotalPawns
            ) {
                Log.d(TAG, "$TAG: Hard (Random): Successfully generated puzzle after $attempt attempts. FEN: ${finalPuzzleBoard.toFEN()}")
                Log.d(TAG, "Generated Path (for solver validation): ${allGeneratedPaths.joinToString { path -> path.joinToString { it.toString() } }}")
                return finalPuzzleBoard
            }
        }
        Log.e(TAG, "$TAG: Hard (Random): Failed to GENERATE Hard puzzle after $maxAttempts attempts. Returning empty board.")
        return ChessBoard.createEmpty()
    }

    private fun findRandomEmptySquare(board: ChessBoard, existingOccupiedSquares: Set<Square>): Square? {
        val emptySquares = mutableListOf<Square>()
        for (rank in 1..8) {
            for (fileChar in 'a'..'h') {
                val square = Square(fileChar, rank)
                // Provera da li je polje već zauzeto na trenutnoj tabli ILI u setu zauzetih polja
                if (board.getPiece(square).type == PieceType.NONE && !existingOccupiedSquares.contains(square)) {
                    emptySquares.add(square)
                }
            }
        }
        val randomEmpty = emptySquares.randomOrNull(Random.Default)
        if (randomEmpty == null) {
            Log.w(TAG, "findRandomEmptySquare: No empty squares found! Existing occupied squares: $existingOccupiedSquares")
        }
        return randomEmpty
    }
}