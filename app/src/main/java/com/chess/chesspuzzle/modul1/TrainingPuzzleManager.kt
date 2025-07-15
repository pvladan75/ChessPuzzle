package com.chess.chesspuzzle.modul1

import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.ChessCore
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square
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
        val minCapturesPerPiece = 1 // Renamed from minMovesPerPiece
        val maxCapturesPerPiece = 3 // Renamed from maxMovesPerPiece

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
            // occupiedSquares sada uključuje: početne pozicije belih figura I SVE PLANIRANE CRNE CILJNE POZICIJE.
            // Ovo se prosleđuje ChessCore.findCaptureTargetSquares
            val globalOccupiedAndTargetSquares = mutableSetOf<Square>()

            Log.d(TAG, "Attempt $attempt: Initializing for Easy puzzle. globalOccupiedAndTargetSquares is empty: ${globalOccupiedAndTargetSquares.isEmpty()}")

            val whitePiecesOnBoard = mutableListOf<Pair<Piece, Square>>() // Keep track of white piece positions

            var generationSuccessful = true

            // 1. Postavi odabranu belu figuru
            val whitePieceType = figuresToUse[0]
            val whitePiece = Piece(whitePieceType, PieceColor.WHITE)
            val startSquare = findRandomEmptySquare(finalPuzzleBoard, globalOccupiedAndTargetSquares) // Use global set for initial placement
            if (startSquare == null) {
                Log.d(TAG, "Attempt $attempt: Failed to find empty square for white piece. Retrying.")
                generationSuccessful = false
                continue
            }

            finalPuzzleBoard = finalPuzzleBoard.setPiece(whitePiece,startSquare)
            globalOccupiedAndTargetSquares.add(startSquare) // Add white piece's starting square to occupied set
            whitePiecesOnBoard.add(whitePiece to startSquare) // Store for later
            Log.d(TAG, "Attempt $attempt: White piece ${whitePiece.type} placed at $startSquare. globalOccupiedAndTargetSquares: $globalOccupiedAndTargetSquares")


            // 2. Generiši ciljna polja za crne figure za ovu belu figuru
            val numCapturesForThisPiece = random.nextInt(minCapturesPerPiece, maxCapturesPerPiece + 1)

            // POZIV NOVE FUNKCIJE: findCaptureTargetSquares
            val foundTargetSquares: Set<Square>? = ChessCore.findCaptureTargetSquares(
                finalPuzzleBoard, // Tabla sa belom figurom
                whitePiece,
                startSquare,
                numCapturesForThisPiece,
                globalOccupiedAndTargetSquares // PROMENJENO: Prosleđujemo set svih već zauzetih/planiranih polja
            )

            // PROMENJENO: Provera nullabilnosti i veličine pronađenih ciljnih polja
            if (foundTargetSquares == null || foundTargetSquares.size != numCapturesForThisPiece) {
                Log.d(TAG, "Attempt $attempt: Failed to find ${numCapturesForThisPiece} target squares for ${whitePiece.type} from ${startSquare}. Found: ${foundTargetSquares?.size ?: "null"}. Retrying.")
                generationSuccessful = false
                continue
            }

            // Dodaj pronađena ciljna polja u globalni set (da bi sledeće figure izbegle ove mete)
            globalOccupiedAndTargetSquares.addAll(foundTargetSquares)
            Log.d(TAG, "Attempt $attempt: Successfully found targets for ${whitePiece.type}: $foundTargetSquares. Global occupied now: $globalOccupiedAndTargetSquares")


            // Filter out targets that are on the same square as the white piece, or any other placed white piece
            // This is generally handled by findCaptureTargetSquares and findRandomEmptySquare, but a final check for robustness
            val validTargetSquares = foundTargetSquares.filter { it !in whitePiecesOnBoard.map { wp -> wp.second } }.toSet()

            if (validTargetSquares.size != numCapturesForThisPiece) {
                Log.d(TAG, "Attempt $attempt: Some generated target squares conflicted with white piece positions after filtering. Retrying.")
                generationSuccessful = false
                continue
            }

            // 3. Postavljanje crnih figura na sakupljena ciljna polja
            val availableBlackPieceTypes = mutableListOf(
                PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN
            )

            // Postavljaj crne figure na validTargetSquares
            for (targetSquare in validTargetSquares) {
                // Dodatna provera: Polje ne sme biti prazno samo po board.getPiece, već i po globalOccupiedAndTargetSquares
                // Ovo je redundantno ako findCaptureTargetSquares radi ispravno, ali je sigurnosna mera.
                if (finalPuzzleBoard.getPiece(targetSquare).type != PieceType.NONE || globalOccupiedAndTargetSquares.contains(targetSquare) && targetSquare !in foundTargetSquares) {
                    Log.d(TAG, "Attempt $attempt: Conflict: Target square $targetSquare is already occupied/reserved before placing black piece. Retrying.")
                    generationSuccessful = false
                    break
                }
                val blackPieceType = availableBlackPieceTypes.random(random)
                // Pešaci ne mogu biti na 1. ili 8. redu
                if (blackPieceType == PieceType.PAWN && (targetSquare.rank == 1 || targetSquare.rank == 8)) {
                    Log.d(TAG, "Attempt $attempt: Cannot place PAWN at $targetSquare (rank 1 or 8). Retrying.")
                    generationSuccessful = false
                    break
                }
                finalPuzzleBoard = finalPuzzleBoard.setPiece(Piece(blackPieceType, PieceColor.BLACK), targetSquare)
                // NEMA POTREBE DA SE DODAJU OVDE U occupiedSquares JER SU VEC DODATI KROZ foundTargetSquares.addAll()
                // I generalno, globalOccupiedAndTargetSquares se resetuje po pokusaju i gradi se tokom generisanja
                Log.d(TAG, "Attempt $attempt: Black piece ${blackPieceType} placed at $targetSquare.")
            }
            if (!generationSuccessful) continue

            // The `allPassThroughSquares` logic here is largely removed/simplified because ChessCore.findCaptureTargetSquares
            // handles path clarity internally. We only need to ensure *final* placed black pieces don't block other white pieces.
            // If findCaptureTargetSquares ensures this for its path, and targets are added to globalOccupiedAndTargetSquares,
            // then we don't need a separate pass-through check here for *already placed* pieces.

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
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Total unique target squares found: ${validTargetSquares.size}")


            if (blackPiecesCount > 0 && whitePiecesCount > 0 &&
                blackPiecesCount >= minTotalPawns && blackPiecesCount <= maxTotalPawns &&
                blackPiecesCount == validTargetSquares.size // Ensure total black pieces match generated targets
            ) {
                Log.d(TAG, "$TAG: Easy (Random): Successfully generated puzzle after $attempt attempts. FEN: ${finalPuzzleBoard.toFEN()}")
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
        val minCapturesPerPiece = 2
        val maxCapturesPerPiece = 4

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
            val globalOccupiedAndTargetSquares = mutableSetOf<Square>()

            Log.d(TAG, "Attempt $attempt: Initializing for Medium puzzle. globalOccupiedAndTargetSquares is empty: ${globalOccupiedAndTargetSquares.isEmpty()}")

            val whitePiecesOnBoard = mutableListOf<Pair<Piece, Square>>()

            var generationSuccessful = true

            // Postavljanje belih figura
            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = findRandomEmptySquare(finalPuzzleBoard, globalOccupiedAndTargetSquares)
                if (startSquare == null) {
                    Log.d(TAG, "Attempt $attempt: Failed to find empty square for white piece ${pieceType}. Retrying.")
                    generationSuccessful = false
                    break
                }

                finalPuzzleBoard = finalPuzzleBoard.setPiece(whitePiece, startSquare)
                globalOccupiedAndTargetSquares.add(startSquare)
                whitePiecesOnBoard.add(whitePiece to startSquare)
                Log.d(TAG, "Attempt $attempt: White piece ${whitePiece.type} placed at $startSquare. globalOccupiedAndTargetSquares: $globalOccupiedAndTargetSquares")
            }
            if (!generationSuccessful) continue

            // Generisanje putanja za svaku belu figuru
            for (entry in whitePiecesOnBoard) { // Iterate over whitePiecesOnBoard, not initialPositions map
                val (whitePiece, startSquare) = entry // Destructure the pair

                val numCapturesForThisPiece = random.nextInt(minCapturesPerPiece, maxCapturesPerPiece + 1)

                val foundTargetSquares: Set<Square>? = ChessCore.findCaptureTargetSquares(
                    finalPuzzleBoard,
                    whitePiece,
                    startSquare,
                    numCapturesForThisPiece,
                    globalOccupiedAndTargetSquares
                )

                if (foundTargetSquares == null || foundTargetSquares.size != numCapturesForThisPiece) {
                    Log.d(TAG, "Attempt $attempt: Failed to find ${numCapturesForThisPiece} target squares for ${whitePiece.type} from ${startSquare}. Found: ${foundTargetSquares?.size ?: "null"}. Retrying.")
                    generationSuccessful = false
                    break
                }
                globalOccupiedAndTargetSquares.addAll(foundTargetSquares)
                Log.d(TAG, "Attempt $attempt: Successfully found targets for ${whitePiece.type}: $foundTargetSquares. Global occupied now: $globalOccupiedAndTargetSquares")
            }
            if (!generationSuccessful) continue

            // All targets are now accumulated in globalOccupiedAndTargetSquares from findCaptureTargetSquares calls.
            // We need to extract just the *black target squares* from this global set.
            // The `globalOccupiedAndTargetSquares` set now contains both white piece starting positions
            // AND all proposed black target squares.
            // To get *only* the black target squares, we need to remove the white piece starting squares.
            val allProposedBlackTargetSquares = globalOccupiedAndTargetSquares.toMutableSet()
            whitePiecesOnBoard.forEach { (piece, square) ->
                allProposedBlackTargetSquares.remove(square)
            }


            if (allProposedBlackTargetSquares.size < minTotalPawns || allProposedBlackTargetSquares.size > maxTotalPawns) {
                Log.d(TAG, "Attempt $attempt: Number of target squares (${allProposedBlackTargetSquares.size}) out of pawn range ($minTotalPawns-$maxTotalPawns). Retrying.")
                generationSuccessful = false
                continue
            }

            // 3. Postavljanje crnih figura na sakupljena ciljna polja
            val availableBlackPieceTypes = mutableListOf(
                PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN
            )

            // Postavljaj crne figure na allProposedBlackTargetSquares
            for (targetSquare in allProposedBlackTargetSquares.shuffled(random)) { // Shuffle for randomness
                if (finalPuzzleBoard.getPiece(targetSquare).type != PieceType.NONE) { // This means a white piece is already there
                    Log.d(TAG, "Attempt $attempt: Conflict: Target square $targetSquare is occupied by a white piece. This indicates an issue in ChessCore.findCaptureTargetSquares or previous logic. Retrying.")
                    generationSuccessful = false
                    break
                }
                val blackPieceType = availableBlackPieceTypes.random(random)
                if (blackPieceType == PieceType.PAWN && (targetSquare.rank == 1 || targetSquare.rank == 8)) {
                    Log.d(TAG, "Attempt $attempt: Cannot place PAWN at $targetSquare (rank 1 or 8). Retrying.")
                    generationSuccessful = false
                    break
                }
                finalPuzzleBoard = finalPuzzleBoard.setPiece( Piece(blackPieceType, PieceColor.BLACK), targetSquare)
                Log.d(TAG, "Attempt $attempt: Black piece ${blackPieceType} placed at $targetSquare.")
            }
            if (!generationSuccessful) continue

            val blackPiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
            val whitePiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).size

            Log.d(TAG, "Attempt $attempt: FINAL CHECK - White pieces found on board: ${whitePiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: White Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Black pieces found on board: ${blackPiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: Black Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Total unique target squares generated: ${allProposedBlackTargetSquares.size}")


            if (blackPiecesCount > 0 && whitePiecesCount > 0 &&
                blackPiecesCount >= minTotalPawns && blackPiecesCount <= maxTotalPawns &&
                blackPiecesCount == allProposedBlackTargetSquares.size
            ) {
                Log.d(TAG, "$TAG: Medium (Random): Successfully generated puzzle after $attempt attempts. FEN: ${finalPuzzleBoard.toFEN()}")
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
        val minCapturesPerPiece = 3
        val maxCapturesPerPiece = 6

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
            val globalOccupiedAndTargetSquares = mutableSetOf<Square>()

            Log.d(TAG, "Attempt $attempt: Initializing for Hard puzzle. globalOccupiedAndTargetSquares is empty: ${globalOccupiedAndTargetSquares.isEmpty()}")

            val whitePiecesOnBoard = mutableListOf<Pair<Piece, Square>>()

            var generationSuccessful = true

            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = findRandomEmptySquare(finalPuzzleBoard, globalOccupiedAndTargetSquares)
                if (startSquare == null) {
                    Log.d(TAG, "Attempt $attempt: Failed to find empty square for white piece ${pieceType}. Retrying.")
                    generationSuccessful = false
                    break
                }

                finalPuzzleBoard = finalPuzzleBoard.setPiece(whitePiece, startSquare)
                globalOccupiedAndTargetSquares.add(startSquare)
                whitePiecesOnBoard.add(whitePiece to startSquare)
                Log.d(TAG, "Attempt $attempt: White piece ${whitePiece.type} placed at $startSquare. globalOccupiedAndTargetSquares: $globalOccupiedAndTargetSquares")
            }
            if (!generationSuccessful) continue

            for (entry in whitePiecesOnBoard) {
                val (whitePiece, startSquare) = entry

                val numCapturesForThisPiece = random.nextInt(minCapturesPerPiece, maxCapturesPerPiece + 1)

                val foundTargetSquares: Set<Square>? = ChessCore.findCaptureTargetSquares(
                    finalPuzzleBoard,
                    whitePiece,
                    startSquare,
                    numCapturesForThisPiece,
                    globalOccupiedAndTargetSquares
                )

                if (foundTargetSquares == null || foundTargetSquares.size != numCapturesForThisPiece) {
                    Log.d(TAG, "Attempt $attempt: Failed to find ${numCapturesForThisPiece} target squares for ${whitePiece.type} from ${startSquare}. Found: ${foundTargetSquares?.size ?: "null"}. Retrying.")
                    generationSuccessful = false
                    break
                }
                globalOccupiedAndTargetSquares.addAll(foundTargetSquares)
                Log.d(TAG, "Attempt $attempt: Successfully found targets for ${whitePiece.type}: $foundTargetSquares. Global occupied now: $globalOccupiedAndTargetSquares")
            }
            if (!generationSuccessful) continue

            val allProposedBlackTargetSquares = globalOccupiedAndTargetSquares.toMutableSet()
            whitePiecesOnBoard.forEach { (piece, square) ->
                allProposedBlackTargetSquares.remove(square)
            }


            if (allProposedBlackTargetSquares.size < minTotalPawns || allProposedBlackTargetSquares.size > maxTotalPawns) {
                Log.d(TAG, "Attempt $attempt: Number of target squares (${allProposedBlackTargetSquares.size}) out of pawn range ($minTotalPawns-$maxTotalPawns). Retrying.")
                generationSuccessful = false
                continue
            }

            val availableBlackPieceTypes = mutableListOf(
                PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN
            )

            for (targetSquare in allProposedBlackTargetSquares.shuffled(random)) {
                if (finalPuzzleBoard.getPiece(targetSquare).type != PieceType.NONE) {
                    Log.d(TAG, "Attempt $attempt: Conflict: Target square $targetSquare is occupied by a white piece. This indicates an issue in ChessCore.findCaptureTargetSquares or previous logic. Retrying.")
                    generationSuccessful = false
                    break
                }
                val blackPieceType = availableBlackPieceTypes.random(random)
                if (blackPieceType == PieceType.PAWN && (targetSquare.rank == 1 || targetSquare.rank == 8)) {
                    Log.d(TAG, "Attempt $attempt: Cannot place PAWN at $targetSquare (rank 1 or 8). Retrying.")
                    generationSuccessful = false
                    break
                }
                finalPuzzleBoard = finalPuzzleBoard.setPiece( Piece(blackPieceType, PieceColor.BLACK), targetSquare)
                Log.d(TAG, "Attempt $attempt: Black piece ${blackPieceType} placed at $targetSquare.")
            }
            if (!generationSuccessful) continue

            val blackPiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
            val whitePiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).size

            Log.d(TAG, "Attempt $attempt: FINAL CHECK - White pieces found on board: ${whitePiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: White Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Black pieces found on board: ${blackPiecesCount}")
            finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).forEach { (square, piece) ->
                Log.d(TAG, "  FINAL: Black Piece at $square: ${piece.type} ${piece.color}")
            }
            Log.d(TAG, "Attempt $attempt: FINAL CHECK - Total unique target squares generated: ${allProposedBlackTargetSquares.size}")


            if (blackPiecesCount > 0 && whitePiecesCount > 0 &&
                blackPiecesCount >= minTotalPawns && blackPiecesCount <= maxTotalPawns &&
                blackPiecesCount == allProposedBlackTargetSquares.size
            ) {
                Log.d(TAG, "$TAG: Hard (Random): Successfully generated puzzle after $attempt attempts. FEN: ${finalPuzzleBoard.toFEN()}")
                return finalPuzzleBoard
            }
        }
        Log.e(TAG, "$TAG: Hard (Random): Failed to GENERATE Hard puzzle after $maxAttempts attempts. Returning empty board.")
        return ChessBoard.createEmpty()
    }

    /**
     * Pomoćna funkcija za pronalaženje nasumičnog praznog polja na tabli.
     * Izbegava polja koja su već zauzeta.
     */
    private fun findRandomEmptySquare(board: ChessBoard, occupiedSquares: Set<Square>): Square? {
        val emptySquares = Square.ALL_SQUARES.filter { square ->
            !occupiedSquares.contains(square) && board.getPiece(square).type == PieceType.NONE
        }.shuffled(Random.Default)

        return emptySquares.firstOrNull()
    }
}