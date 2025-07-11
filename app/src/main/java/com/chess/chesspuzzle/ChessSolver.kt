package com.chess.chesspuzzle

import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square

// Definisanje klase ChessSolver
class ChessSolver {

    // Data klasa za predstavljanje poteza sa pojedinom figurom
    data class MoveData(val fromSquare: Square, val toSquare: Square, val capturedPiece: Piece) {
        override fun toString(): String {
            return "${fromSquare}-${toSquare}"
        }
    }

    // Glavni ulaz za solver
    fun solve(initialBoard: ChessBoard): List<MoveData>? {
        val blackPiecesCount = initialBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
        val whitePieces = initialBoard.getPiecesMapFromBoard(PieceColor.WHITE)

        if (whitePieces.isEmpty()) {
            Log.e("ChessSolver", "Nema belih figura na tabli za rešavanje.")
            return null
        }

        // Trenutno pretpostavljamo jednu belu figuru. Ako ih ima više, mora se iterirati.
        val initialWhitePieceEntry = whitePieces.entries.first()
        val initialWhitePieceSquare = initialWhitePieceEntry.key
        val initialWhitePiece = initialWhitePieceEntry.value

        // Set za praćenje posećenih stanja table da bismo izbegli ponavljanje i petlje.
        val visitedStates = mutableSetOf<String>()

        Log.d("ChessSolver", "Pokrećem solver za poziciju: ${initialBoard.toFEN()} sa ${blackPiecesCount} crnih figura.")
        initialBoard.printBoard()

        return solveRecursive(
            currentBoard = initialBoard,
            currentWhitePieceSquare = initialWhitePieceSquare,
            currentWhitePiece = initialWhitePiece,
            path = mutableListOf(),
            targetCaptures = blackPiecesCount,
            visitedStates = visitedStates
        )
    }

    private fun solveRecursive(
        currentBoard: ChessBoard,
        currentWhitePieceSquare: Square,
        currentWhitePiece: Piece,
        path: MutableList<MoveData>,
        targetCaptures: Int,
        visitedStates: MutableSet<String>
    ): List<MoveData>? {

        val boardFen = currentBoard.toFEN()
        if (visitedStates.contains(boardFen)) {
            Log.d("ChessSolver", "Stanje već posećeno, preskačem: $boardFen")
            return null
        }
        visitedStates.add(boardFen)

        val remainingBlackPieces = currentBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
        Log.d("ChessSolver", "Trenutni broj pojedinih figura: ${path.size}. Preostalo crnih figura: $remainingBlackPieces")

        if (remainingBlackPieces == 0) {
            if (path.size == targetCaptures) {
                Log.d("ChessSolver", "REŠENJE PRONAĐENO! Putanja: $path")
                return path
            }
            Log.w("ChessSolver", "Rešenje pronađeno, ali broj poteza se ne poklapa sa brojem figura. Expected: $targetCaptures, Actual: ${path.size}")
            return null
        }

        if (path.size >= targetCaptures) {
            Log.d("ChessSolver", "Dubina pretrage prešla broj meta (${path.size} >= $targetCaptures). Nema rešenja ovde.")
            return null
        }

        val possibleEatingMoves = generateValidEatingMoves(currentBoard, currentWhitePieceSquare, currentWhitePiece)
        Log.d("ChessSolver", "Mogući potezi za ${currentWhitePiece.type} na ${currentWhitePieceSquare}: $possibleEatingMoves")


        for (moveAttempt in possibleEatingMoves) {
            val fromSquare = moveAttempt.first
            val toSquare = moveAttempt.second

            val eatenPiece = currentBoard.getPiece(toSquare)
            if (eatenPiece.type == PieceType.NONE || eatenPiece.color != PieceColor.BLACK) {
                Log.e("ChessSolver", "Greška u logici: Potez ${fromSquare}-${toSquare} ne hvata crnu figuru, a trebao bi.")
                continue
            }

            val currentMove = MoveData(fromSquare, toSquare, eatenPiece)

            var nextBoard = currentBoard.removePiece(fromSquare)
            nextBoard = nextBoard.removePiece(toSquare)
            nextBoard = nextBoard.setPiece(currentWhitePiece, toSquare)

            path.add(currentMove)
            Log.d("ChessSolver", "Pokušavam potez: $currentMove. Trenutna putanja: $path")
            nextBoard.printBoard()

            val solution = solveRecursive(
                currentBoard = nextBoard,
                currentWhitePieceSquare = toSquare,
                currentWhitePiece = currentWhitePiece,
                path = path,
                targetCaptures = targetCaptures,
                visitedStates = visitedStates
            )

            if (solution != null) {
                return solution
            }

            path.removeAt(path.lastIndex)
        }

        return null
    }

    /**
     * Generiše sve validne poteze za belu figuru koji rezultiraju JEDENJEM crne figure.
     * Koristi logiku iz ChessCore.getValidMoves i dodatno filtrira.
     *
     * @param board Trenutna šahovska tabla.
     * @param pieceSquare Pozicija bele figure.
     * @param piece Bela figura.
     * @return Lista parova (fromSquare, toSquare) koji predstavljaju validne poteze "jedenja".
     */
    private fun generateValidEatingMoves(board: ChessBoard, pieceSquare: Square, piece: Piece): List<Pair<Square, Square>> {
        val eatingMoves = mutableListOf<Pair<Square, Square>>()

        val allValidMoves = ChessCore.getValidMoves(board, piece, pieceSquare)

        for (targetSquare in allValidMoves) {
            val targetPiece = board.getPiece(targetSquare)
            if (targetPiece.type != PieceType.NONE && targetPiece.color == PieceColor.BLACK) {
                // Provera za klizne figure je bitna: da li je putanja do crne figure čista
                // (tj. da nema drugih figura IZMEĐU bele figure i crne figure).
                // `getValidMoves` već osigurava da nema tvojih figura na putu i da je cilj validan,
                // ali `isPathClear` eksplicitno proverava srednja polja za klizne figure.
                if (ChessCore.isPathClear(board, pieceSquare, targetSquare, piece.type)) {
                    eatingMoves.add(pieceSquare to targetSquare)
                }
            }
        }
        return eatingMoves
    }
}