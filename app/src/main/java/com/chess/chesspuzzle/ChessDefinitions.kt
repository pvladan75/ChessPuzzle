package com.chess.chesspuzzle

import android.util.Log
import java.util.Date

// --- ENUMS ---
enum class PieceType {
    PAWN,
    KNIGHT,
    BISHOP, // <--- ISPRAVLJENO: Bilo je BISHop, sada je BISHOP
    ROOK,
    QUEEN,
    KING,
    NONE;

    companion object {
        fun fromChar(char: Char): PieceType {
            return when (char.uppercaseChar()) {
                'P' -> PAWN
                'N' -> KNIGHT
                'B' -> BISHOP
                'R' -> ROOK
                'Q' -> QUEEN
                'K' -> KING
                else -> NONE
            }
        }
    }
}

enum class PieceColor {
    WHITE,
    BLACK,
    NONE
}

// --- DATA CLASSES ---

data class Square(val file: Char, val rank: Int) {
    init {
        require(file in 'a'..'h') { "Fajl mora biti između 'a' i 'h', ali je '$file'" }
        require(rank in 1..8) { "Rank mora biti između 1 i 8, ali je $rank" }
    }

    val fileIndex: Int
        get() = file - 'a'
    val rankIndex: Int
        get() = rank - 1

    override fun toString(): String {
        return "$file$rank"
    }

    companion object {
        fun fromCoordinates(fileIndex: Int, rankIndex: Int): Square {
            return Square('a' + fileIndex, rankIndex + 1)
        }

        val ALL_SQUARES: List<Square> = (0..7).flatMap { fileIdx ->
            (0..7).map { rankIdx -> fromCoordinates(fileIdx, rankIdx) }
        }
    }
}

data class Piece(val type: PieceType, val color: PieceColor) {
    companion object {
        val NONE = Piece(PieceType.NONE, PieceColor.NONE)

        fun fromChar(char: Char): Piece {
            val type = PieceType.fromChar(char)
            val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            return Piece(type, color)
        }
    }

    fun toFenChar(): Char {
        val char = when (type) {
            PieceType.PAWN -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK -> 'r'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
            PieceType.NONE -> return ' '
        }
        return if (color == PieceColor.WHITE) char.uppercaseChar() else char
    }
}

data class ChessBoard(val pieces: Map<Square, Piece>) {

    fun getPiece(square: Square): Piece {
        return pieces[square] ?: Piece.NONE
    }

    // <--- OVDE JE PROMENJENO: placePiece je vraćen u setPiece
    fun setPiece(piece: Piece, square: Square): ChessBoard {
        val newPieces = pieces.toMutableMap()
        if (piece.type == PieceType.NONE) {
            newPieces.remove(square)
        } else {
            newPieces[square] = piece
        }
        return ChessBoard(newPieces)
    }

    fun removePiece(square: Square): ChessBoard {
        val newPieces = pieces.toMutableMap()
        newPieces.remove(square)
        return ChessBoard(newPieces)
    }

    fun copy(): ChessBoard {
        return ChessBoard(this.pieces.toMutableMap())
    }

    fun makeMoveAndCapture(from: Square, to: Square): ChessBoard {
        val newPieces = this.pieces.toMutableMap()
        val pieceToMove = newPieces[from] ?: return this

        newPieces.remove(from)
        newPieces.remove(to)
        newPieces[to] = pieceToMove

        return ChessBoard(newPieces.toMap())
    }

    companion object {
        fun createEmpty(): ChessBoard {
            return ChessBoard(emptyMap())
        }

        fun parseFenToBoard(fen: String): ChessBoard {
            val parts = fen.split(" ")
            val piecePlacement = parts[0]
            var board = createEmpty()

            var rank = 8
            var file = 'a'

            for (char in piecePlacement) {
                when {
                    char.isDigit() -> {
                        val emptySquares = char.toString().toInt()
                        file += emptySquares
                    }
                    char == '/' -> {
                        rank--
                        file = 'a'
                    }
                    else -> {
                        // Sada koristimo setPiece sa ispravnim redosledom argumenata
                        board = board.setPiece(Piece.fromChar(char), Square(file, rank))
                        file++
                    }
                }
            }
            return board
        }

        fun createStandardBoard(): ChessBoard {
            val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
            return parseFenToBoard(fen)
        }
    }

    fun getPiecesMapFromBoard(color: PieceColor? = null): Map<Square, Piece> {
        val map = mutableMapOf<Square, Piece>()
        for (entry in pieces) {
            val (square, piece) = entry
            if (piece.type != PieceType.NONE && (color == null || piece.color == color)) {
                map[square] = piece
            }
        }
        return map
    }

    fun toFEN(): String {
        val fenBuilder = StringBuilder()

        for (rank in 8 downTo 1) {
            var emptyCount = 0
            for (fileChar in 'a'..'h') {
                val square = Square(fileChar, rank)
                val piece = getPiece(square)

                if (piece.type == PieceType.NONE) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        fenBuilder.append(emptyCount)
                        emptyCount = 0
                    }
                    fenBuilder.append(piece.toFenChar())
                }
            }
            if (emptyCount > 0) {
                fenBuilder.append(emptyCount)
            }
            if (rank > 1) {
                fenBuilder.append("/")
            }
        }

        fenBuilder.append(" w - - 0 1")

        return fenBuilder.toString()
    }

    fun printBoard() {
        Log.d("ChessBoard", "--- Chess Board State ---")
        for (rank in 8 downTo 1) {
            val row = StringBuilder("$rank |")
            for (fileChar in 'a'..'h') {
                val piece = getPiece(Square(fileChar, rank))
                row.append(" ")
                row.append(when (piece.type) {
                    PieceType.PAWN -> if (piece.color == PieceColor.WHITE) "♙" else "♟︎"
                    PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) "♘" else "♞"
                    PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) "♗" else "♝"
                    PieceType.ROOK -> if (piece.color == PieceColor.WHITE) "♖" else "♜"
                    PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) "♕" else "♛"
                    PieceType.KING -> if (piece.color == PieceColor.WHITE) "♔" else "♚"
                    PieceType.NONE -> "."
                })
                row.append(" ")
            }
            Log.d("ChessBoard", row.toString())
        }
        Log.d("ChessBoard", "  -----------------------")
        Log.d("ChessBoard", "    a  b  c  d  e  f  g  h")
        Log.d("ChessBoard", "-------------------------")
    }
}

data class ScoreEntry(val playerName: String, val score: Int, val timestamp: Long = System.currentTimeMillis()) : Comparable<ScoreEntry> {
    override fun compareTo(other: ScoreEntry): Int {
        val scoreComparison = other.score.compareTo(this.score)
        if (scoreComparison != 0) {
            return scoreComparison
        }
        return this.timestamp.compareTo(other.timestamp)
    }
}