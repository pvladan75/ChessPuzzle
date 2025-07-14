package com.chess.chesspuzzle.modul1

import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square

// Klase Piece, PieceColor, PieceType, Square, ChessBoard, ChessCore pretpostavljamo da su dostupne

// Definisanje klase ChessSolver
class ChessSolver {

    // Data klasa za predstavljanje poteza sa pojedinom figurom
    data class MoveData(val fromSquare: Square, val toSquare: Square, val capturedPiece: Piece) {
        override fun toString(): String {
            // Vraća format "e2-e4" kako bi SolutionDisplayActivity mogao da parsira
            return "${fromSquare.file}${fromSquare.rank}-${toSquare.file}${toSquare.rank}"
        }
    }

    // Glavni ulaz za solver - sada vraća List<MoveData>?
    fun solve(initialBoard: ChessBoard): List<MoveData>? {
        val blackPiecesCount = initialBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
        val whitePieces = initialBoard.getPiecesMapFromBoard(PieceColor.WHITE)

        // <<-- UKLONJENA PROVERA KRALJEVA -->>
        // val whiteKingCount = whitePieces.count { it.value.type == PieceType.KING }
        // val blackKingCount = initialBoard.getPiecesMapFromBoard(PieceColor.BLACK).count { it.value.type == PieceType.KING }
        // if (whiteKingCount != 1 || blackKingCount != 1) {
        //     Log.e("ChessSolver", "Tabla mora imati tačno jednog belog i jednog crnog kralja.")
        //     return null
        // }

        if (whitePieces.isEmpty()) {
            Log.e("ChessSolver", "Nema belih figura na tabli za rešavanje.")
            return null
        }

        val visitedStates = mutableSetOf<String>()

        Log.d("ChessSolver", "Pokrećem solver za poziciju: ${initialBoard.toFEN()} sa ${blackPiecesCount} crnih figura.")
        initialBoard.printBoard()

        return solveRecursive(
            currentBoard = initialBoard,
            path = mutableListOf(),
            targetCaptures = blackPiecesCount,
            visitedStates = visitedStates
        )
    }

    private fun solveRecursive(
        currentBoard: ChessBoard,
        path: MutableList<MoveData>,
        targetCaptures: Int,
        visitedStates: MutableSet<String>
    ): List<MoveData>? {

        val boardFen = currentBoard.toFEN()
        if (visitedStates.contains(boardFen)) {
            return null
        }
        visitedStates.add(boardFen)

        val remainingBlackPieces = currentBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
        Log.d("ChessSolver", "Trenutni broj uhvaćenih figura: ${path.size}. Preostalo crnih figura: $remainingBlackPieces")

        if (remainingBlackPieces == 0) {
            if (path.size == targetCaptures) {
                Log.d("ChessSolver", "REŠENJE PRONAĐENO! Putanja: $path")
                return path
            }
            Log.w("ChessSolver", "Sve crne figure uhvaćene, ali broj poteza se ne poklapa sa brojem figura. Očekivano: $targetCaptures, Aktuelno: ${path.size}")
            return null
        }

        if (path.size >= targetCaptures) {
            Log.d("ChessSolver", "Dubina pretrage prešla ciljani broj figura (${path.size} >= $targetCaptures). Nema rešenja ovde.")
            return null
        }

        val whitePiecesOnBoard = currentBoard.getPiecesMapFromBoard(PieceColor.WHITE)
        val relevantWhitePieces = whitePiecesOnBoard.filter { (square, piece) ->
            piece.color == PieceColor.WHITE &&
                    (piece.type == PieceType.KNIGHT ||
                            piece.type == PieceType.QUEEN ||
                            piece.type == PieceType.ROOK ||
                            piece.type == PieceType.BISHOP)
        }

        val sortedWhitePieces = relevantWhitePieces.entries.sortedWith(compareByDescending {
            when (it.value.type) {
                PieceType.QUEEN -> 5
                PieceType.ROOK -> 4
                PieceType.BISHOP -> 3
                PieceType.KNIGHT -> 2
                else -> 1
            }
        })

        for ((whitePieceSquare, whitePiece) in sortedWhitePieces) {
            val possibleEatingMoves = generateValidEatingMoves(currentBoard, whitePieceSquare, whitePiece)

            for (moveAttempt in possibleEatingMoves) {
                val fromSquare = moveAttempt.first
                val toSquare = moveAttempt.second

                val eatenPiece = currentBoard.getPiece(toSquare)
                if (eatenPiece.type == PieceType.NONE || eatenPiece.color != PieceColor.BLACK) {
                    Log.e("ChessSolver", "Greška u logici: Potez ${fromSquare}-${toSquare} ne hvata crnu figuru, a trebao bi.")
                    continue
                }

                val currentMove = MoveData(fromSquare, toSquare, eatenPiece)

                val nextBoard = currentBoard.makeMoveAndCapture(fromSquare, toSquare)

                path.add(currentMove)
                Log.d("ChessSolver", "Pokušavam potez: $currentMove. Trenutna putanja: $path")

                val solution = solveRecursive(
                    currentBoard = nextBoard,
                    path = path,
                    targetCaptures = targetCaptures,
                    visitedStates = visitedStates
                )

                if (solution != null) {
                    return solution
                }

                path.removeAt(path.lastIndex)
            }
        }

        return null
    }

    private fun generateValidEatingMoves(board: ChessBoard, pieceSquare: Square, piece: Piece): List<Pair<Square, Square>> {
        val eatingMoves = mutableListOf<Pair<Square, Square>>()
        val allValidMoves = ChessCore.getValidMoves(board, piece, pieceSquare)

        for (targetSquare in allValidMoves) {
            val targetPiece = board.getPiece(targetSquare)
            if (targetPiece.type != PieceType.NONE && targetPiece.color == PieceColor.BLACK) {
                eatingMoves.add(pieceSquare to targetSquare)
            }
        }
        return eatingMoves
    }
}