package com.chess.chesspuzzle

import android.util.Log // Needed for ChessBoard.printBoard()
import java.util.Date // Needed for ScoreEntry timestamp

// --- ENUMS ---
enum class PieceType {
    PAWN,
    KNIGHT,
    BISHOP,
    ROOK,
    QUEEN,
    KING,
    NONE // For empty squares
}

enum class PieceColor {
    WHITE,
    BLACK,
    NONE // For empty squares or neutral
}

// --- DATA CLASSES ---

// Represents a square on the board (e.g., a1, h8)
data class Square(val file: Char, val rank: Int) {
    init {
        require(file in 'a'..'h') { "File must be between 'a' and 'h'" }
        require(rank in 1..8) { "Rank must be between 1 and 8" }
    }

    override fun toString(): String {
        return "$file$rank"
    }
}

// Represents a chess piece (type and color)
data class Piece(val type: PieceType, val color: PieceColor) {
    companion object {
        val EMPTY = Piece(PieceType.NONE, PieceColor.NONE)
    }
}

// Represents the state of the chessboard
class ChessBoard {
    // Internal representation of the board as a 2D array.
    // This is generally more performant for board operations than a Map when you need frequent access by coordinates.
    private val pieces: Array<Array<Piece>> // [rank-1][file_char.code - 'a'.code]

    init {
        pieces = Array(8) { Array(8) { Piece.EMPTY } }
    }

    // Gets the piece at a specific square
    fun getPiece(square: Square): Piece {
        // Ensure square coordinates are valid before accessing the array
        if (!isValidCoordinate(square.rank, square.file.code)) {
            Log.e("ChessBoard", "Attempted to get piece from invalid square: $square")
            return Piece.EMPTY // Return empty piece for invalid squares
        }
        return pieces[square.rank - 1][square.file.code - 'a'.code]
    }

    // Sets a piece at a specific square and returns a new ChessBoard instance (for immutability)
    fun setPiece(square: Square, piece: Piece): ChessBoard {
        val newBoard = copy() // Essential for immutability and state management
        // Ensure square coordinates are valid before accessing the array
        if (!isValidCoordinate(square.rank, square.file.code)) {
            Log.e("ChessBoard", "Attempted to set piece at invalid square: $square")
            return this // Return current board if square is invalid
        }
        newBoard.pieces[square.rank - 1][square.file.code - 'a'.code] = piece
        return newBoard
    }

    // Removes a piece from a specific square and returns a new ChessBoard instance
    fun removePiece(square: Square): ChessBoard {
        return setPiece(square, Piece.EMPTY)
    }

    // Creates a deep copy of the board
    fun copy(): ChessBoard {
        val newBoard = ChessBoard()
        for (rankIdx in 0 until 8) {
            for (fileIdx in 0 until 8) {
                // Copy references to Piece objects (since Piece is a data class, it's immutable
                // so shallow copy of references is fine here. If Piece were mutable, a deep copy would be needed).
                newBoard.pieces[rankIdx][fileIdx] = this.pieces[rankIdx][fileIdx]
            }
        }
        return newBoard
    }

    // Creates an empty board
    companion object {
        fun createEmpty(): ChessBoard {
            return ChessBoard()
        }

        private fun isValidCoordinate(rank: Int, file: Int): Boolean {
            return rank in 1..8 && file in 'a'.code..'h'.code
        }
    }

    // Gets a map of pieces of a specific color, used for tracking remaining black pieces.
    fun getPiecesMapFromBoard(color: PieceColor): Map<Square, Boolean> {
        val map = mutableMapOf<Square, Boolean>()
        for (rankIdx in 0 until 8) {
            for (fileIdx in 0 until 8) {
                val square = Square(('a'.code + fileIdx).toChar(), rankIdx + 1)
                val piece = getPiece(square)
                if (piece.color == color && piece.type != PieceType.NONE) {
                    map[square] = true
                }
            }
        }
        return map
    }

    // Helps with debugging: prints the board to the console
    fun printBoard() {
        Log.d("ChessBoard", "--- Chess Board State ---")
        for (rank in 8 downTo 1) { // Iterate from rank 8 down to 1 (top to bottom on UI)
            val row = StringBuilder("$rank |")
            for (fileChar in 'a'..'h') { // Iterate from file 'a' to 'h' (left to right)
                val piece = getPiece(Square(fileChar, rank))
                row.append(" ")
                row.append(when (piece.type) {
                    PieceType.PAWN -> if (piece.color == PieceColor.WHITE) "♟︎" else "♙" // White/Black Pawn
                    PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) "♞" else "♘" // White/Black Knight
                    PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) "♝" else "♗" // White/Black Bishop
                    PieceType.ROOK -> if (piece.color == PieceColor.WHITE) "♜" else "♖" // White/Black Rook
                    PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) "♛" else "♕" // White/Black Queen
                    PieceType.KING -> if (piece.color == PieceColor.WHITE) "♚" else "♔" // White/Black King
                    PieceType.NONE -> "." // Empty square
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

/**
 * Data klasa koja predstavlja jedan unos rezultata.
 * Sadrži ime igrača, postignuti skor i vremensku oznaku kada je rezultat zabeležen.
 *
 * Implementira Comparable da bi se omogućilo lako sortiranje lista ScoreEntry objekata.
 * Sortira se opadajuće po skoru, a za izjednačenje skora, rastuće po vremenu (ranije postignuti rezultat ima prednost).
 *
 * @property playerName Ime igrača.
 * @property score Postignuti skor.
 * @property timestamp Vremenska oznaka (u milisekundama od ere) kada je rezultat zabeležen. Podrazumevano je trenutno vreme.
 */
data class ScoreEntry(val playerName: String, val score: Int, val timestamp: Long = System.currentTimeMillis()) : Comparable<ScoreEntry> {
    override fun compareTo(other: ScoreEntry): Int {
        // Sortiraj po skoru (opadajuće)
        val scoreComparison = other.score.compareTo(this.score)
        if (scoreComparison != 0) {
            return scoreComparison
        }
        // Ako su skorovi isti, sortiraj po vremenu (rastuće) - stariji rezultat ima prednost
        return this.timestamp.compareTo(other.timestamp)
    }
}