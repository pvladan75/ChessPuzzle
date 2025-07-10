package com.chess.chesspuzzle

import android.util.Log // Potrebno za ChessBoard.printBoard()
import java.util.Date // Potrebno za ScoreEntry timestamp, ako je ScoreEntry u ovom fajlu
// kotlin.random.Random - Nije potrebno ovde, samo u ChessCore.kt


// --- ENUMS ---
enum class PieceType {
    PAWN,
    KNIGHT,
    BISHOP,
    ROOK,
    QUEEN,
    KING,
    NONE;

    companion object {
        // Pomoćna funkcija za mapiranje FEN karaktera u PieceType
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
    NONE // Za prazna polja ili neutralno
}

// --- DATA CLASSES ---

// Predstavlja polje na tabli (npr., a1, h8)
data class Square(val file: Char, val rank: Int) {
    init {
        require(file in 'a'..'h') { "Fajl mora biti između 'a' i 'h', ali je '$file'" }
        require(rank in 1..8) { "Rank mora biti između 1 i 8, ali je $rank" }
    }

    // Pomoćna svojstva za lakši rad sa koordinatama (0-indexed)
    val fileIndex: Int
        get() = file - 'a' // 0-7
    val rankIndex: Int
        get() = rank - 1 // 0-7 (bottom to top, like an array index)

    override fun toString(): String {
        return "$file$rank"
    }

    companion object {
        // Konstruktor iz 0-indeksiranih koordinata
        fun fromCoordinates(fileIndex: Int, rankIndex: Int): Square {
            return Square('a' + fileIndex, rankIndex + 1)
        }

        // Sve validne pozicije na tabli
        val ALL_SQUARES: List<Square> = (0..7).flatMap { fileIdx ->
            (0..7).map { rankIdx -> fromCoordinates(fileIdx, rankIdx) }
        }
    }
}

// Predstavlja šahovsku figuru (tip i boja)
data class Piece(val type: PieceType, val color: PieceColor) {
    companion object {
        val NONE = Piece(PieceType.NONE, PieceColor.NONE) // Prazna figura

        // Pomoćna funkcija za kreiranje Piece objekta iz FEN karaktera
        fun fromChar(char: Char): Piece {
            val type = PieceType.fromChar(char) // Koristi fromChar iz PieceType enum-a
            val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            return Piece(type, color)
        }
    }

    // Dodaj toFenChar() metodu direktno u Piece klasu
    fun toFenChar(): Char {
        val char = when (type) {
            PieceType.PAWN -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK -> 'r'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
            PieceType.NONE -> return ' ' // ' ' nije standardno za FEN, ali indikuje odsustvo
        }
        return if (color == PieceColor.WHITE) char.uppercaseChar() else char
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
        if (piece.type == PieceType.NONE) {
            newPieces.remove(square) // Ukloni figuru ako je tip NONE
        } else {
            newPieces[square] = piece // Postavlja ili ažurira figuru
        }
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

        // Metoda za parsiranje FEN stringa u ChessBoard objekat
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
                        board = board.setPiece(Square(file, rank), Piece.fromChar(char))
                        file++
                    }
                }
            }
            return board
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

    // *** OVO JE TO FEN() FUNKCIJA KOJA TI FALI! ***
    fun toFEN(): String {
        val fenBuilder = StringBuilder()

        // 1. Board representation
        for (rank in 8 downTo 1) { // Od 8. reda nadole
            var emptyCount = 0
            for (fileChar in 'a'..'h') { // Od 'a' do 'h'
                val square = Square(fileChar, rank)
                val piece = getPiece(square) // Uzmi figuru sa tog polja

                if (piece.type == PieceType.NONE) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        fenBuilder.append(emptyCount)
                        emptyCount = 0
                    }
                    fenBuilder.append(piece.toFenChar()) // Koristi toFenChar() iz Piece klase
                }
            }
            if (emptyCount > 0) {
                fenBuilder.append(emptyCount)
            }
            if (rank > 1) {
                fenBuilder.append("/")
            }
        }

        // Dodajte ostale FEN delove
        // Za jednostavne zagonetke obično je dovoljno:
        // - Active color (čiji je potez, "w" za beli)
        // - Castling availability ("-" ako nema rokade)
        // - En passant target square ("-" ako nema en passant)
        // - Halfmove clock (broj poteza bez uzimanja figure ili pomeranja pešaka, obično 0 za zagonetke)
        // - Fullmove number (broj celih poteza, obično 1 za početak zagonetke)
        fenBuilder.append(" w - - 0 1")

        return fenBuilder.toString()
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