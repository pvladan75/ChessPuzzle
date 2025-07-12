package com.chess.chesspuzzle

import android.util.Log
// Uklonjen import LinkedList jer se ne koristi direktno ovde
import kotlin.random.Random

class PositionGenerator(private val solver: ChessSolver) {

    private val TAG = "PositionGenerator"

    private val ALL_BLACK_PIECE_TYPES = listOf(
        PieceType.PAWN,
        PieceType.KNIGHT,
        PieceType.BISHOP,
        PieceType.ROOK,
        PieceType.QUEEN
    )

    fun generate(
        whitePiecesConfig: Map<PieceType, Int>,
        numBlackPieces: Int,
        maxAttempts: Int = 1000
    ): String? {
        require(whitePiecesConfig.isNotEmpty()) { "Mora postojati bar jedna bela figura." }
        require(numBlackPieces > 0) { "Mora biti bar jedna crna figura." }

        for (attempt in 0 until maxAttempts) {
            Log.d(TAG, "Pokušaj generisanja pozicije: ${attempt + 1}")

            var currentBoard = ChessBoard.createEmpty()
            val whitePiecesOnBoard = mutableListOf<Pair<Piece, Square>>()

            // 1. Postavi sve bele figure na nasumična, nezauzeta polja
            val availableSquaresForWhite = Square.ALL_SQUARES.toMutableSet()
            var whitePlacementSuccessful = true

            for ((pieceType, count) in whitePiecesConfig) {
                for (i in 0 until count) {
                    if (availableSquaresForWhite.isEmpty()) {
                        Log.w(TAG, "Nema dovoljno slobodnih polja za bele figure. Prekidam trenutni pokušaj.")
                        whitePlacementSuccessful = false
                        break
                    }
                    val randomSquare = availableSquaresForWhite.random()
                    availableSquaresForWhite.remove(randomSquare)
                    val whitePiece = Piece(pieceType, PieceColor.WHITE)
                    currentBoard = currentBoard.setPiece(whitePiece, randomSquare)
                    whitePiecesOnBoard.add(whitePiece to randomSquare)
                }
                if (!whitePlacementSuccessful) break
            }
            if (!whitePlacementSuccessful) {
                continue
            }

            // Set za praćenje svih polja koja su već zauzeta belim figurama ili su planirana kao mete za crne figure
            // Ovo se prosleđuje ChessCore.findCaptureTargetSquares kako bi se izbegli konflikti.
            val globalOccupiedAndTargetSquares = mutableSetOf<Square>()
            whitePiecesOnBoard.forEach { globalOccupiedAndTargetSquares.add(it.second) } // Dodaj početne pozicije belih figura

            // Distribuiraj broj crnih figura koje svaka bela figura treba da "uhvati"
            val capturesPerWhitePiece = distributeCaptures(numBlackPieces, whitePiecesOnBoard.size)

            // Sakupljamo sve jedinstvene ciljne kvadrate koje su pronašle bele figure
            val allProposedBlackTargetSquares = mutableSetOf<Square>()

            // 2. Generiši ciljna polja za SVAKU belu figuru
            var pathGenerationSuccessful = true
            var pieceIndex = 0

            for ((whitePiece, whitePieceStartingSquare) in whitePiecesOnBoard) {
                val numCapturesForThisPiece = capturesPerWhitePiece[pieceIndex] ?: 0
                if (numCapturesForThisPiece == 0) {
                    pieceIndex++
                    continue
                }

                // Prosleđujemo trenutno stanje svih zauzetih/planiranih polja
                val generatedTargetSquaresForThisPiece = ChessCore.findCaptureTargetSquares(
                    currentBoard, // Tabla sa svim belim figurama
                    whitePiece,
                    whitePieceStartingSquare,
                    numCapturesForThisPiece,
                    globalOccupiedAndTargetSquares // <--- Sada se šalje ispravan set
                )

                if (generatedTargetSquaresForThisPiece == null || generatedTargetSquaresForThisPiece.size != numCapturesForThisPiece) {
                    Log.d(TAG, "Nije uspelo generisanje ${numCapturesForThisPiece} ciljnih polja za ${whitePiece.type} sa ${whitePieceStartingSquare}. Pokušavam ponovo.")
                    pathGenerationSuccessful = false
                    break
                }
                // Dodaj sva novogenerisana ciljna polja u globalni set i u set svih predloženih meta
                globalOccupiedAndTargetSquares.addAll(generatedTargetSquaresForThisPiece)
                allProposedBlackTargetSquares.addAll(generatedTargetSquaresForThisPiece)
                pieceIndex++
            }

            // Provera da li je ukupan broj generisanih jedinstvenih ciljnih polja dovoljan
            if (!pathGenerationSuccessful || allProposedBlackTargetSquares.size != numBlackPieces) {
                Log.d(TAG, "Ukupan broj generisanih jedinstvenih ciljnih polja (${allProposedBlackTargetSquares.size}) ne odgovara broju crnih figura (${numBlackPieces}). Pokušavam ponovo.")
                continue
            }

            Log.d(TAG, "Sve generisane putanje za bele figure, jedinstvena ciljna polja: $allProposedBlackTargetSquares")

            // 3. Postavljanje crnih figura na sakupljena polja putanje
            var finalBoardForSolver = currentBoard.copy() // Kloniramo tablu sa belim figurama

            var blackPlacementSuccessful = true
            val shuffledTargetSquares = allProposedBlackTargetSquares.shuffled() // Nasumično rasporedi ciljna polja

            for (targetSquare in shuffledTargetSquares) {
                // Generiši nasumičan tip crne figure
                val randomBlackPieceType = ALL_BLACK_PIECE_TYPES.random()
                val blackPiece = Piece(randomBlackPieceType, PieceColor.BLACK)

                // Proveri da li je pešak i da li je na zabranjenom redu
                if (blackPiece.type == PieceType.PAWN && (targetSquare.rank == 1 || targetSquare.rank == 8)) {
                    Log.w(TAG, "Pokušaj postavljanja crnog pešaka na zabranjeni red (${targetSquare}). Prekidam trenutni pokušaj.")
                    blackPlacementSuccessful = false
                    break
                }

                // Ovo polje NE BI trebalo da bude zauzeto, jer je ChessCore funkcija već uzela u obzir globalOccupiedAndTargetSquares
                // Ali ostavljam kao dodatnu sigurnosnu proveru.
                if (finalBoardForSolver.getPiece(targetSquare).type != PieceType.NONE) {
                    Log.w(TAG, "Ciljno polje ${targetSquare} je već zauzeto! Ovo ukazuje na problem u logici ChessCore.findCaptureTargetSquares.")
                    blackPlacementSuccessful = false
                    break
                }

                finalBoardForSolver = finalBoardForSolver.setPiece(blackPiece, targetSquare)
            }

            if (!blackPlacementSuccessful) {
                continue // Idi na sledeći pokušaj generisanja ako postavljanje crnih figura nije bilo uspešno
            }

            Log.d(TAG, "Potencijalno generisana pozicija (FEN): ${finalBoardForSolver.toFEN()}")
            finalBoardForSolver.printBoard()

            // 4. Proveri da li je generisana pozicija rešiva korišćenjem solvera
            val solution = solver.solve(finalBoardForSolver)

            if (solution != null && solution.size == numBlackPieces) {
                Log.d(TAG, "Uspešno generisana rešiva pozicija! FEN: ${finalBoardForSolver.toFEN()}")
                Log.d(TAG, "Rešenje: ${solution.map { it.toString() }}")
                return finalBoardForSolver.toFEN()
            } else {
                Log.d(TAG, "Generisana pozicija nije rešiva ili broj poteza ne odgovara. Pokušavam ponovo.")
                if (solution == null) {
                    Log.d(TAG, "Solver nije pronašao rešenje za ovu poziciju.")
                } else {
                    Log.d(TAG, "Solver je pronašao rešenje dužine ${solution.size}, očekivano: $numBlackPieces.")
                }
            }
        }

        Log.e(TAG, "Nakon $maxAttempts pokušaja, nije uspelo generisanje rešive pozicije.")
        return null
    }

    private fun distributeCaptures(totalCaptures: Int, numberOfWhitePieces: Int): List<Int> {
        if (numberOfWhitePieces == 0) return emptyList()
        val distribution = MutableList(numberOfWhitePieces) { 0 }
        for (i in 0 until totalCaptures) {
            distribution[i % numberOfWhitePieces]++
        }
        return distribution
    }
}