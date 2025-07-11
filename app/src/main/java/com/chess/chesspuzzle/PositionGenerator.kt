package com.chess.chesspuzzle

import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square
import kotlin.random.Random

class PositionGenerator(private val solver: ChessSolver) {

    private val TAG = "PositionGenerator"

    // Sada dozvoljavamo sve tipove crnih figura osim KING
    // PAWN je i dalje dozvoljen, ali sa ograničenjima redova
    private val ALL_BLACK_PIECE_TYPES = listOf(
        PieceType.PAWN,
        PieceType.KNIGHT,
        PieceType.BISHOP,
        PieceType.ROOK,
        PieceType.QUEEN
    )

    /**
     * Generiše rešivu poziciju za zagonetku "pojedi sve crne figure".
     *
     * @param whitePieceType Tip bele figure koja će biti na tabli (KNIGHT, BISHOP, ROOK, QUEEN).
     * @param numBlackPieces Željeni broj crnih figura (ovo određuje i dužinu rešenja).
     * @param maxAttempts Maksimalan broj pokušaja generisanja pozicije pre odustajanja.
     * @return FEN string rešive pozicije, ili null ako generisanje ne uspe.
     */
    fun generate(
        whitePieceType: PieceType,
        numBlackPieces: Int,
        maxAttempts: Int = 1000
    ): String? {
        require(whitePieceType in listOf(PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)) {
            "Bela figura mora biti skakač, lovac, top ili kraljica."
        }
        require(numBlackPieces > 0) { "Mora biti bar jedna crna figura." }

        for (attempt in 0 until maxAttempts) {
            Log.d(TAG, "Pokušaj generisanja pozicije: ${attempt + 1}")

            val whitePiece = Piece(whitePieceType, PieceColor.WHITE)
            val whitePieceStartingSquare = Square.ALL_SQUARES.random()

            // Počinjemo sa praznom tablom i postavljamo samo belu figuru.
            // Crne figure će biti postavljene na osnovu generisane putanje.
            var currentBoard = ChessBoard.createEmpty().setPiece(whitePiece, whitePieceStartingSquare)

            val blackPiecesToPlace = mutableListOf<Pair<Square, Piece>>()

            // Generišemo putanju za belu figuru
            val generatedPath = ChessCore.generatePiecePath(currentBoard, whitePiece, whitePieceStartingSquare, numBlackPieces)

            if (generatedPath.isEmpty() || generatedPath.size != numBlackPieces) {
                Log.d(TAG, "Generisanje putanje neuspešno ili nedovoljno dugačko. Pokušavam ponovo.")
                continue // Pokušaj ponovo
            }

            Log.d(TAG, "Generisana putanja za belu figuru: $generatedPath")

            // Postavljanje crnih figura na putanju
            // Temp tabla za proveru konflikata figura prilikom postavljanja
            var tempBoardForPlacement = ChessBoard.createEmpty().setPiece(whitePiece, whitePieceStartingSquare)
            var placementSuccessful = true

            for (i in 0 until generatedPath.size) {
                val targetSquare = generatedPath[i]

                // Generiši nasumičan tip crne figure
                val randomBlackPieceType = ALL_BLACK_PIECE_TYPES.random()
                val blackPiece = Piece(randomBlackPieceType, PieceColor.BLACK)

                // NOVO: Proveri da li je pešak i da li je na zabranjenom redu
                if (blackPiece.type == PieceType.PAWN && (targetSquare.rank == 1 || targetSquare.rank == 8)) {
                    Log.w(TAG, "Pokušaj postavljanja crnog pešaka na zabranjeni red (${targetSquare}). Prekidam trenutni pokušaj.")
                    placementSuccessful = false
                    break // Prekini ovu iteraciju i pokušaj ponovo od početka
                }

                // Proveri da li je ciljno polje već zauzeto
                if (tempBoardForPlacement.getPiece(targetSquare).type != PieceType.NONE) {
                    Log.w(TAG, "Ciljno polje ${targetSquare} već zauzeto prilikom postavljanja crne figure. Prekidam trenutni pokušaj.")
                    placementSuccessful = false
                    break // Prekini ovu iteraciju i pokušaj ponovo od početka
                }

                tempBoardForPlacement = tempBoardForPlacement.setPiece(blackPiece, targetSquare)
                blackPiecesToPlace.add(targetSquare to blackPiece)
            }

            if (!placementSuccessful) {
                continue // Idi na sledeći pokušaj generisanja ako postavljanje nije bilo uspešno
            }

            // Ako je blackPiecesToPlace prazno, to znači da je unutrašnja petlja prekinuta zbog zauzetog polja
            // (ovo je sada pokriveno sa !placementSuccessful provere, ali ostaje kao sigurnosna mera)
            if (blackPiecesToPlace.isEmpty() && numBlackPieces > 0) {
                continue
            }

            // Finalna tabla za rešavanje: Samo početna bela figura i sve generisane crne figure
            var finalBoardForSolver = ChessBoard.createEmpty().setPiece(whitePiece, whitePieceStartingSquare)
            for ((sq, p) in blackPiecesToPlace) {
                finalBoardForSolver = finalBoardForSolver.setPiece(p, sq)
            }

            Log.d(TAG, "Potencijalno generisana pozicija (FEN): ${finalBoardForSolver.toFEN()}")
            finalBoardForSolver.printBoard()

            // Proveri da li je generisana pozicija rešiva korišćenjem solvera
            val solution = solver.solve(finalBoardForSolver)

            if (solution != null && solution.size == numBlackPieces) {
                Log.d(TAG, "Uspešno generisana rešiva pozicija! FEN: ${finalBoardForSolver.toFEN()}")
                Log.d(TAG, "Rešenje: ${solution.map { it.toString() }}")
                return finalBoardForSolver.toFEN()
            } else {
                Log.d(TAG, "Generisana pozicija nije rešiva ili broj poteza ne odgovara. Pokušavam ponovo.")
            }
        }

        Log.e(TAG, "Nakon $maxAttempts pokušaja, nije uspelo generisanje rešive pozicije.")
        return null
    }
}