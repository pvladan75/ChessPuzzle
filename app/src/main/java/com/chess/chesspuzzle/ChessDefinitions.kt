package com.chess.chesspuzzle

import android.util.Log
import java.util.Date
// Postavljamo BOARD_SIZE kao top-level konstantu
const val BOARD_SIZE = 8
// --- ENUMS ---
enum class Difficulty { EASY, MEDIUM, HARD }
enum class PieceType {
    PAWN,
    KNIGHT,
    BISHOP,
    ROOK,
    QUEEN,
    KING,
    NONE;

    // PREMEŠTENO OVDE: isSlidingPiece je sada metoda PieceType
    /**
     * Proverava da li je figura "klizeća" (dama, top, lovac).
     */
    fun isSlidingPiece(): Boolean {
        return this == QUEEN || this == ROOK || this == BISHOP
    }

    // PREMEŠTENO OVDE: getRawMoves je sada metoda PieceType
    /**
     * Vraća listu "sirovih" (mogućih, bez provere da li ima figura na putu)
     * ciljnih polja za figuru sa datog početnog polja.
     * NE proverava prepreke ili druge figure!
     */
    fun getRawMoves(startSquare: Square, pieceColor: PieceColor): List<Square> {
        val moves = mutableListOf<Square>()
        val (startX, startY) = startSquare.fileIndex to startSquare.rankIndex

        fun addMove(x: Int, y: Int) {
            if (x in 0 until BOARD_SIZE && y in 0 until BOARD_SIZE) {
                moves.add(Square.fromCoordinates(x, y))
            }
        }

        when (this) {
            PAWN -> {
                val direction = if (pieceColor == PieceColor.WHITE) 1 else -1
                // Pešak se u getRawMoves kreće pravo i dijagonalno za hvatanje
                // Normalan potez napred (kasnije se proverava da li je prazno polje)
                addMove(startX, startY + direction)
                // Početni dvostruki potez (kasnije se proverava da li je prazno polje)
                if (pieceColor == PieceColor.WHITE && startY == 1) { // Pešaci na 2. redu (rankIndex 1)
                    addMove(startX, startY + 2 * direction)
                } else if (pieceColor == PieceColor.BLACK && startY == 6) { // Pešaci na 7. redu (rankIndex 6)
                    addMove(startX, startY + 2 * direction)
                }
                // Potencijalni hvatajući potezi (kasnije se proverava da li ima figure za hvatanje)
                addMove(startX + 1, startY + direction)
                addMove(startX - 1, startY + direction)
            }
            KNIGHT -> {
                val knightMoves = listOf(
                    -2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1
                )
                knightMoves.forEach { (dx, dy) ->
                    addMove(startX + dx, startY + dy)
                }
            }
            BISHOP -> {
                for (i in 1 until BOARD_SIZE) { addMove(startX + i, startY + i) }
                for (i in 1 until BOARD_SIZE) { addMove(startX + i, startY - i) }
                for (i in 1 until BOARD_SIZE) { addMove(startX - i, startY + i) }
                for (i in 1 until BOARD_SIZE) { addMove(startX - i, startY - i) }
            }
            ROOK -> {
                for (i in 1 until BOARD_SIZE) { addMove(startX + i, startY) }
                for (i in 1 until BOARD_SIZE) { addMove(startX - i, startY) }
                for (i in 1 until BOARD_SIZE) { addMove(startX, startY + i) }
                for (i in 1 until BOARD_SIZE) { addMove(startX, startY - i) }
            }
            QUEEN -> {
                // Kombinacija poteza topa i lovca
                for (i in 1 until BOARD_SIZE) { addMove(startX + i, startY + i) }
                for (i in 1 until BOARD_SIZE) { addMove(startX + i, startY - i) }
                for (i in 1 until BOARD_SIZE) { addMove(startX - i, startY + i) }
                for (i in 1 until BOARD_SIZE) { addMove(startX - i, startY - i) }
                for (i in 1 until BOARD_SIZE) { addMove(startX + i, startY) }
                for (i in 1 until BOARD_SIZE) { addMove(startX - i, startY) }
                for (i in 1 until BOARD_SIZE) { addMove(startX, startY + i) }
                for (i in 1 until BOARD_SIZE) { addMove(startX, startY - i) }
            }
            KING -> {
                val kingMoves = listOf(
                    -1 to -1, -1 to 0, -1 to 1,
                    0 to -1,           0 to 1,
                    1 to -1,  1 to 0,  1 to 1
                )
                kingMoves.forEach { (dx, dy) ->
                    addMove(startX + dx, startY + dy)
                }
            }
            NONE -> { /* No moves for NONE piece type */ }
        }
        return moves.filter { it != startSquare } // Ukloni polje sa kojeg je figura krenula, da se ne računa kao "potez" na isto polje
    }

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

        val ALL_SQUARES: List<Square> = (0 until BOARD_SIZE).flatMap { fileIdx ->
            (0 until BOARD_SIZE).map { rankIdx -> fromCoordinates(fileIdx, rankIdx) }
        }
    }
}

data class Move(val start: Square, val end: Square) {
    override fun toString(): String {
        return "${start}-${end}"
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
    // Obe fun isSlidingPiece i fun getRawMoves su premeštene iz Piece klase u PieceType enum,
    // jer su logički vezane za TIP figure, a ne za boju/instancu.
    // Dakle, nema ih više ovde!
}

data class ChessBoard(val pieces: Map<Square, Piece>) {

    fun getPiece(square: Square): Piece {
        return pieces[square] ?: Piece.NONE
    }

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

    fun movePiece(move: Move): ChessBoard {
        val pieceToMove = getPiece(move.start)
        if (pieceToMove.type == PieceType.NONE) {
            Log.w("ChessBoard", "Attempted to move a piece from an empty square: ${move.start}")
            return this
        }

        val newPieces = pieces.toMutableMap()

        newPieces.remove(move.start)

        val targetPiece = getPiece(move.end)
        if (targetPiece.type != PieceType.NONE && targetPiece.color != pieceToMove.color) {
            newPieces.remove(move.end)
        }

        newPieces[move.end] = pieceToMove

        return ChessBoard(newPieces)
    }


    fun copy(): ChessBoard {
        return ChessBoard(this.pieces.toMutableMap())
    }

    // Funkcija makeMoveAndCapture je zadržana ali je `movePiece` preferiran za opšte poteze
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

    /**
     * Proverava da li je putanja između dve tačke čista za klizeće figure.
     * Pretpostavlja se da su startSquare i endSquare na istoj liniji (horizontalno, vertikalno, ili dijagonalno).
     */
    fun isPathClear(startSquare: Square, endSquare: Square): Boolean {
        if (startSquare == endSquare) return true

        val dx = (endSquare.fileIndex - startSquare.fileIndex).coerceIn(-1, 1)
        val dy = (endSquare.rankIndex - startSquare.rankIndex).coerceIn(-1, 1)

        var currentFileIndex = startSquare.fileIndex + dx
        var currentRankIndex = startSquare.rankIndex + dy

        while (currentFileIndex != endSquare.fileIndex || currentRankIndex != endSquare.rankIndex) {
            val intermediateSquare = Square.fromCoordinates(currentFileIndex, currentRankIndex)
            if (getPiece(intermediateSquare).type != PieceType.NONE) {
                return false // Putanja nije čista
            }
            currentFileIndex += dx
            currentRankIndex += dy
        }
        return true
    }

    /**
     * Proverava da li je dato polje napadnuto od strane bilo koje protivničke figure.
     *
     * @param targetSquare Polje koje se proverava.
     * @param attackingColor Boja figura koje napadaju (npr. PieceColor.BLACK ako proveravamo napade crnih figura).
     * @return Pair<Square, Piece>? - Vraća par (kvadrat napadača, figura napadača) ako je polje napadnuto, inače null.
     */
    fun isSquareAttackedByAnyOpponent(targetSquare: Square, attackingColor: PieceColor): Pair<Square, Piece>? {
        val opponentColor = if (attackingColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE

        // Prođi kroz sve figure protivničke boje
        for ((pieceSquare, piece) in pieces) {
            if (piece.color == opponentColor) {
                // Dobavi "sirove" (moguće) poteze za tu figuru
                // POZIV NOVE LOKACIJE: piece.type.getRawMoves
                val rawMoves = piece.type.getRawMoves(pieceSquare, piece.color)

                // Proveri da li se targetSquare nalazi među sirovim potezima
                if (rawMoves.contains(targetSquare)) {
                    // Dodatna provera za klizeće figure (kraljica, top, lovac)
                    // POZIV NOVE LOKACIJE: piece.type.isSlidingPiece
                    if (piece.type.isSlidingPiece()) {
                        if (isPathClear(pieceSquare, targetSquare)) {
                            return Pair(pieceSquare, piece) // Napadnuto i putanja je čista
                        }
                    } else {
                        // Za ostale figure (skakač, kralj, pešak), putanja nije bitna
                        // Posebna provera za pešake, jer oni napadaju dijagonalno, a kreću se pravo
                        if (piece.type == PieceType.PAWN) {
                            val pawnAttackMoves = if (piece.color == PieceColor.WHITE) {
                                listOf(Square.fromCoordinates(pieceSquare.fileIndex + 1, pieceSquare.rankIndex + 1),
                                    Square.fromCoordinates(pieceSquare.fileIndex - 1, pieceSquare.rankIndex + 1))
                            } else {
                                listOf(Square.fromCoordinates(pieceSquare.fileIndex + 1, pieceSquare.rankIndex - 1),
                                    Square.fromCoordinates(pieceSquare.fileIndex - 1, pieceSquare.rankIndex - 1))
                            }
                            if (pawnAttackMoves.contains(targetSquare)) {
                                return Pair(pieceSquare, piece)
                            }
                        } else {
                            return Pair(pieceSquare, piece) // Napadnuto
                        }
                    }
                }
            }
        }
        return null // Nije napadnuto
    }

    /**
     * Jednostavnija verzija isSquareAttackedByAnyOpponent koja vraća samo true/false.
     */
    fun isSquareAttacked(square: Square, attackingColor: PieceColor): Boolean {
        return isSquareAttackedByAnyOpponent(square, attackingColor) != null
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