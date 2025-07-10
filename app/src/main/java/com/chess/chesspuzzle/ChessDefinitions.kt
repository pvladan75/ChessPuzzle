package com.chess.chesspuzzle

import android.util.Log // Potrebno za ChessBoard.printBoard()
import java.util.Date // Potrebno za ScoreEntry timestamp, ako je ScoreEntry u ovom fajlu

// --- ENUMS ---
enum class PieceType {
    PAWN,
    KNIGHT,
    BISHOP,
    ROOK,
    QUEEN,
    KING,
    NONE // Za prazna polja
}

enum class PieceColor {
    WHITE,
    BLACK,
    NONE // Za prazna polja ili neutralno
}

// --- DATA CLASSES ---

// Predstavlja polje na tabli (npr., a1, h8)
data class Square(val file: Char, val rank: Int) {
    init {
        require(file in 'a'..'h') { "Fajl mora biti između 'a' i 'h', ali je '$file'" }
        require(rank in 1..8) { "Rank mora biti između 1 i 8, ali je $rank" }
    }

    override fun toString(): String {
        return "$file$rank"
    }
}

// Predstavlja šahovsku figuru (tip i boja)
data class Piece(val type: PieceType, val color: PieceColor) {
    companion object {
        val NONE = Piece(PieceType.NONE, PieceColor.NONE) // Prazna figura
    }
}

// Predstavlja stanje šahovske table
// Koristimo Map<Square, Piece> za fleksibilniju reprezentaciju
data class ChessBoard(val pieces: Map<Square, Piece>) {

    // Dobija figuru na određenom polju
    fun getPiece(square: Square): Piece {
        return pieces[square] ?: Piece.NONE // Vraća Piece.NONE ako polje nije u mapi (prazno je)
    }

    // Postavlja figuru na određeno polje i vraća novu ChessBoard instancu (za nemutabilnost)
    fun setPiece(square: Square, piece: Piece): ChessBoard {
        val newPieces = pieces.toMutableMap() // Kreira mutabilnu kopiju mape
        newPieces[square] = piece // Postavlja ili ažurira figuru
        return ChessBoard(newPieces) // Vraća novu ChessBoard instancu sa ažuriranom mapom
    }

    // Uklanja figuru sa određenog polja i vraća novu ChessBoard instancu
    fun removePiece(square: Square): ChessBoard {
        val newPieces = pieces.toMutableMap()
        newPieces.remove(square)
        return ChessBoard(newPieces)
    }

    // Kreira duboku kopiju table
    fun copy(): ChessBoard {
        return ChessBoard(this.pieces.toMutableMap()) // Kopira mapu figura
    }

    companion object {
        // Kreira praznu tablu
        fun createEmpty(): ChessBoard {
            return ChessBoard(emptyMap())
        }
    }

    // Ekstenziona funkcija za ChessBoard: Dobija mapu figura određene boje
    // Ovo je sada metoda ChessBoard klase, a ne ChessCore objekta
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

    // Pomaže kod debagovanja: ispisuje tablu u logcat
    fun printBoard() {
        Log.d("ChessBoard", "--- Chess Board State ---")
        for (rank in 8 downTo 1) { // Iteriraj od reda 8 nadole do 1 (od vrha ka dnu UI-ja)
            val row = StringBuilder("$rank |")
            for (fileChar in 'a'..'h') { // Iteriraj od fajla 'a' do 'h' (s leva na desno)
                val piece = getPiece(Square(fileChar, rank))
                row.append(" ")
                row.append(when (piece.type) {
                    PieceType.PAWN -> if (piece.color == PieceColor.WHITE) "♙" else "♟︎" // Beli/Crni Pešak
                    PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) "♘" else "♞" // Beli/Crni Skakač
                    PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) "♗" else "♝" // Beli/Crni Lovac
                    PieceType.ROOK -> if (piece.color == PieceColor.WHITE) "♖" else "♜" // Beli/Crni Top
                    PieceType.QUEEN -> if (piece.color == PieceColor.WHITE) "♕" else "♛" // Beli/Crni Kraljica
                    PieceType.KING -> if (piece.color == PieceColor.WHITE) "♔" else "♚" // Beli/Crni Kralj
                    PieceType.NONE -> "." // Prazno polje
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