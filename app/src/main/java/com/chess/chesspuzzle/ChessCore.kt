package com.chess.chesspuzzle

import android.util.Log

import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square
import com.chess.chesspuzzle.ChessBoard

object ChessCore {

    private const val TAG = "ChessCore"

    /**
     * Parsira FEN string i kreira ChessBoard objekat.
     * Trenutno podržava samo deo FEN-a koji se odnosi na poziciju figura.
     *
     * @param fenString FEN string za parsiranje (npr. "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
     * @return ChessBoard objekat sa postavljenim figurama.
     */
    fun parseFenToBoard(fenString: String): ChessBoard {
        var board = ChessBoard.createEmpty() // Započni sa praznom tablom
        val parts = fenString.split(" ")
        val piecePlacement = parts[0] // Prvi deo FEN-a je pozicija figura

        var rank = 8 // Počinjemo od 8. reda (top)
        var fileIndex = 0 // Koristimo indeks od 0 do 7 za fajlove (a=0, h=7)

        for (char in piecePlacement) {
            when {
                char == '/' -> {
                    rank-- // Prelazimo na sledeći red
                    fileIndex = 0 // Resetujemo indeks fajla na početak
                }
                char.isDigit() -> {
                    // Prazna polja, preskoči broj polja
                    fileIndex += char.toString().toInt()
                }
                else -> {
                    // Figura: Koristi Piece.fromChar() metodu za kreiranje figure
                    // Ona već zna kako da odredi tip i boju iz karaktera (npr. 'n' za crnog skakača)
                    val piece = Piece.fromChar(char)
                    if (piece.type != PieceType.NONE) {
                        // Kreiraj Square koristeći karakter fajla i red
                        val currentFileChar = 'a' + fileIndex // Pretvara indeks (0-7) u 'a'-'h' karakter
                        board = board.setPiece(Square(currentFileChar, rank), piece)
                    }
                    fileIndex++ // Prelazimo na sledeći fajl (indeks)
                }
            }
        }
        Log.d(TAG, "FEN Parsed. Initial board state for FEN: $fenString")
        board.printBoard() // Loguj stanje table nakon parsiranja
        return board
    }

    /**
     * Konvertuje ChessBoard objekat u FEN string.
     * Za sada, fokusira se samo na deo pozicije figura.
     * (Potpuni FEN uključuje i active color, castling rights, en passant, halfmove clock, fullmove number)
     * Ova funkcija je duplikat u odnosu na ChessBoard.toFEN() i biće uklonjena kada se prebaci na poziv board.toFEN().
     * Ostavljena je privremeno ovde radi kompatibilnosti ako je negde van PuzzleGeneratora koristis.
     */
    fun convertBoardToFen(board: ChessBoard): String {
        val fenBuilder = StringBuilder()
        for (rank in 8 downTo 1) { // Iteriraj od 8. do 1. reda
            var emptySquares = 0
            for (fileChar in 'a'..'h') { // Iteriraj od 'a' do 'h' fajla
                val square = Square(fileChar, rank)
                val piece = board.getPiece(square)

                if (piece.type == PieceType.NONE) {
                    emptySquares++ // Broji prazna polja
                } else {
                    if (emptySquares > 0) {
                        fenBuilder.append(emptySquares) // Dodaj broj praznih polja
                        emptySquares = 0
                    }
                    // Dodaj karakter figure (veliko slovo za bele, malo za crne)
                    fenBuilder.append(when (piece.color) {
                        PieceColor.WHITE -> when (piece.type) {
                            PieceType.PAWN -> 'P'
                            PieceType.KNIGHT -> 'N'
                            PieceType.BISHOP -> 'B'
                            PieceType.ROOK -> 'R'
                            PieceType.QUEEN -> 'Q'
                            PieceType.KING -> 'K'
                            else -> ' ' // Nikad se ne bi trebalo desiti
                        }
                        PieceColor.BLACK -> when (piece.type) {
                            PieceType.PAWN -> 'p'
                            PieceType.KNIGHT -> 'n'
                            PieceType.BISHOP -> 'b'
                            PieceType.ROOK -> 'r'
                            PieceType.QUEEN -> 'q'
                            PieceType.KING -> 'k'
                            else -> ' ' // Nikad se ne bi trebalo desiti
                        }
                        else -> ' ' // Nikad se ne bi trebalo desiti za PieceColor.NONE
                    })
                }
            }
            if (emptySquares > 0) {
                fenBuilder.append(emptySquares) // Dodaj preostali broj praznih polja za red
            }
            if (rank > 1) {
                fenBuilder.append('/') // Odvoji redove sa '/'
            }
        }
        // Dodatni FEN delovi (za sada fiksni, kasnije možeš proširiti)
        fenBuilder.append(" w - - 0 1") // Dodaj aktivnu boju, prava na rokiranje itd.
        return fenBuilder.toString()
    }


    /**
     * Vraća listu svih legalnih polja na koja se data figura može pomeriti
     * sa date početne pozicije na datoj tabli, u skladu sa osnovnim šahovskim pravilima.
     * NE uzima u obzir pravila specifična za zagonetku (poput "ne vraćanja na posećena polja")
     * niti je li potez hvatanje.
     *
     * @param board Trenutna šahovska tabla.
     * @param piece Figura za koju se traže potezi.
     * @param fromSquare Početna pozicija figure.
     * @return Lista Square objekata koji predstavljaju legalne destinacije.
     */
    fun getValidMoves(board: ChessBoard, piece: Piece, fromSquare: Square): List<Square> {
        val validMoves = mutableListOf<Square>()
        val (file, rank) = fromSquare

        when (piece.type) {
            PieceType.PAWN -> {
                val direction = if (piece.color == PieceColor.WHITE) 1 else -1

                // Kretanje jedan korak napred
                val oneStepFwd = Square(file, rank + direction)
                if (oneStepFwd.rank in 1..8 && board.getPiece(oneStepFwd).type == PieceType.NONE) {
                    validMoves.add(oneStepFwd)
                }

                // Kretanje dva koraka napred (sa početne pozicije)
                if ((piece.color == PieceColor.WHITE && rank == 2) || (piece.color == PieceColor.BLACK && rank == 7)) {
                    val twoStepsFwd = Square(file, rank + 2 * direction)
                    if (twoStepsFwd.rank in 1..8 && board.getPiece(twoStepsFwd).type == PieceType.NONE && board.getPiece(oneStepFwd).type == PieceType.NONE) {
                        validMoves.add(twoStepsFwd)
                    }
                }

                // Hvatanje dijagonalno
                val attackFiles = listOf(file - 1, file + 1)
                for (df in attackFiles) {
                    val attackSquare = Square(df.toChar(), rank + direction)
                    // Proveri da li je attackSquare validno polje pre nego što ga koristiš
                    if (attackSquare.file in 'a'..'h' && attackSquare.rank in 1..8) {
                        val targetPiece = board.getPiece(attackSquare)
                        if (targetPiece.type != PieceType.NONE && targetPiece.color != piece.color) {
                            validMoves.add(attackSquare)
                        }
                    }
                }
            }
            PieceType.ROOK -> {
                // Horizontalno i vertikalno kretanje
                val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1)) // Gore, Dole, Desno, Levo

                for ((dr, df) in directions) {
                    var r = rank + dr
                    var f = file + df

                    while (r in 1..8 && f.toChar() in 'a'..'h') { // Konvertuj f u Char pre provere
                        val targetSquare = Square(f.toChar(), r)
                        val targetPiece = board.getPiece(targetSquare)

                        if (targetPiece.type == PieceType.NONE) {
                            validMoves.add(targetSquare)
                        } else {
                            // Može da jede protivničku figuru (ne kralja, jer kralja nema)
                            if (targetPiece.color != piece.color) {
                                validMoves.add(targetSquare)
                            }
                            break // Zaustavi se ako naiđe na figuru (svoju ili protivničku)
                        }
                        r += dr
                        f += df
                    }
                }
            }
            PieceType.KNIGHT -> {
                // Skakač - L-oblik potezi
                val knightMoves = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for ((dr, df) in knightMoves) {
                    val targetRank = rank + dr
                    val targetFileChar = file.code + df
                    if (targetRank in 1..8 && targetFileChar.toChar() in 'a'..'h') {
                        val targetSquare = Square(targetFileChar.toChar(), targetRank)
                        val targetPiece = board.getPiece(targetSquare)
                        // Skakač može da preskače figure, ali ne može da ide na polje svoje figure
                        if (targetPiece.type == PieceType.NONE || targetPiece.color != piece.color) {
                            validMoves.add(targetSquare)
                        }
                    }
                }
            }
            PieceType.BISHOP -> {
                // Lovac - Dijagonalno kretanje
                val directions = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)) // Gore-desno, Gore-levo, Dole-desno, Dole-levo

                for ((dr, df) in directions) {
                    var r = rank + dr
                    var f = file + df

                    while (r in 1..8 && f.toChar() in 'a'..'h') { // Konvertuj f u Char pre provere
                        val targetSquare = Square(f.toChar(), r)
                        val targetPiece = board.getPiece(targetSquare)

                        if (targetPiece.type == PieceType.NONE) {
                            validMoves.add(targetSquare)
                        } else {
                            // Može da jede protivničku figuru (ne kralja, jer kralja nema)
                            if (targetPiece.color != piece.color) {
                                validMoves.add(targetSquare)
                            }
                            break // Zaustavi se ako naiđe na figuru (svoju ili protivničku)
                        }
                        r += dr
                        f += df
                    }
                }
            }
            PieceType.QUEEN -> {
                // Kraljica - Kombinacija Topa i Lovca
                val directions = listOf(
                    Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1), // Topovski
                    Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1) // Lovački
                )

                for ((dr, df) in directions) {
                    var r = rank + dr
                    var f = file + df

                    while (r in 1..8 && f.toChar() in 'a'..'h') {
                        val targetSquare = Square(f.toChar(), r)
                        val targetPiece = board.getPiece(targetSquare)

                        if (targetPiece.type == PieceType.NONE) {
                            validMoves.add(targetSquare)
                        } else {
                            // Može da jede protivničku figuru (ne kralja, jer kralja nema)
                            if (targetPiece.color != piece.color) {
                                validMoves.add(targetSquare)
                            }
                            break
                        }
                        r += dr
                        f += df
                    }
                }
            }
            PieceType.KING -> {
                // Ako si potpuno uklonio kraljeve iz igre, ova grana se nikada neće izvršiti.
                // Ako ih ipak koristiš u nekom drugom kontekstu, a ne za generisanje zagonetki,
                // onda ova logika kretanja ostaje ispravna.
                for (dr in -1..1) {
                    for (df in -1..1) {
                        if (dr == 0 && df == 0) continue // Ne pomera se
                        val targetRank = rank + dr
                        val targetFileChar = file.code + df
                        if (targetRank in 1..8 && targetFileChar.toChar() in 'a'..'h') {
                            val targetSquare = Square(targetFileChar.toChar(), targetRank)
                            val targetPiece = board.getPiece(targetSquare)
                            if (targetPiece.type == PieceType.NONE || targetPiece.color != piece.color) {
                                validMoves.add(targetSquare)
                            }
                        }
                    }
                }
            }
            PieceType.NONE -> {
                // Nema poteza za prazno polje
            }
        }
        return validMoves
    }

    /**
     * Vraća listu svih polja koja se nalaze između dve date kocke.
     * Koristi se za klizne figure (top, lovac, kraljica).
     *
     * @param from Početno polje.
     * @param to Krajnje polje.
     * @return Lista Square objekata koji predstavljaju međupolja.
     * Prazna lista ako nema međupolja (npr. za poteze skakača, ili susedna polja).
     */
    fun getSquaresBetween(from: Square, to: Square): List<Square> {
        val squaresBetween = mutableListOf<Square>()

        if (from == to) return squaresBetween

        val fileDiff = to.file.code - from.file.code
        val rankDiff = to.rank - from.rank

        // Proveri da li je potez horizontalan, vertikalan ili dijagonalan
        val isHorizontal = rankDiff == 0 && fileDiff != 0
        val isVertical = fileDiff == 0 && rankDiff != 0
        val isDiagonal = kotlin.math.abs(fileDiff) == kotlin.math.abs(rankDiff) && fileDiff != 0 // Koristi kotlin.math.abs

        if (!isHorizontal && !isVertical && !isDiagonal) {
            // Nije klizni potez, nema polja između (npr. potez skakača, ili susedna polja)
            return squaresBetween
        }

        val df = if (fileDiff == 0) 0 else if (fileDiff > 0) 1 else -1 // Smer po fajlu
        val dr = if (rankDiff == 0) 0 else if (rankDiff > 0) 1 else -1 // Smer po redu

        var currentFile = from.file.code + df
        var currentRank = from.rank + dr

        while (true) {
            val currentSquareChar = currentFile.toChar()
            // Provera da li je trenutno polje meta ili van table
            if (currentSquareChar == to.file && currentRank == to.rank) {
                break // Stigli smo do ciljnog polja, ne dodajemo ga u listu "između"
            }
            if (currentRank !in 1..8 || currentSquareChar !in 'a'..'h') {
                break // Van table
            }
            squaresBetween.add(Square(currentSquareChar, currentRank))
            currentFile += df
            currentRank += dr
        }
        return squaresBetween
    }

    /**
     * Generiše putanju figure za dati broj poteza, osiguravajući da se figura ne vraća
     * na prethodno posećena polja unutar te putanje (uključujući početno polje i sva tranzitna polja).
     * Ova logika je isključivo za GENERISANJE ZAGONETKE.
     *
     * @param board Trenutna šahovska tabla.
     * @param piece Figura za koju se generiše putanja.
     * @param startSquare Početna pozicija figure.
     * @param numMoves Željeni broj poteza u putanji.
     * @return Lista Square objekata koji predstavljaju putanju. Vraća praznu listu ako putanja ne može biti generisana.
     */
    fun generatePiecePath(board: ChessBoard, piece: Piece, startSquare: Square, numMoves: Int): List<Square> {
        val path = mutableListOf<Square>()
        var currentSquare = startSquare
        // Koristimo Set za brzo proveravanje da li je polje već posećeno
        val visitedSquaresInPath = mutableSetOf<Square>()

        visitedSquaresInPath.add(startSquare) // Početno polje se smatra posećenim

        Log.d(TAG, "Generisanje putanje za ${piece.type} sa ${startSquare} za ${numMoves} poteza.")
        Log.d(TAG, "Početna visitedSquaresInPath: $visitedSquaresInPath")

        for (i in 0 until numMoves) {
            // Koristi getValidMoves za osnovne šahovske poteze
            val possibleMoves = getValidMoves(board, piece, currentSquare)
            Log.d(TAG, "  Potez ${i+1}: Trenutno polje: ${currentSquare}")
            Log.d(TAG, "  Mogući potezi iz ChessCore.getValidMoves (osnovna šahovska pravila): $possibleMoves")

            // Filtriraj poteze koji bi se vratili na već posećeno polje unutar ove PUTANJE ZAGONETKE
            val availableMoves = possibleMoves.filter { targetSquare ->
                // Potez je nevažeći ako je ciljno polje već posećeno
                if (visitedSquaresInPath.contains(targetSquare)) {
                    Log.d(TAG, "    Potez do $targetSquare odbijen (ZA GENERISANJE): Ciljno polje je već posećeno u putanji.")
                    false // Nije dostupno
                } else {
                    // Za klizne figure, proveri da li tranzitna polja kolidiraju sa posećenim
                    if (piece.type == PieceType.ROOK || piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN) {
                        val squaresBetween = getSquaresBetween(currentSquare, targetSquare)
                        val conflictsWithVisited = squaresBetween.any { visitedSquaresInPath.contains(it) }
                        if (conflictsWithVisited) {
                            Log.d(TAG, "    Potez do $targetSquare odbijen (ZA GENERISANJE): Tranzitna polja se preklapaju sa posećenim: ${squaresBetween.filter { visitedSquaresInPath.contains(it) }}")
                            false // Nije dostupno
                        } else {
                            true // Dostupno
                        }
                    } else {
                        true // Dostupno za ne-klizne figure
                    }
                }
            }

            Log.d(TAG, "  Dostupni potezi nakon filtriranja (anti-vraćanje za GENERISANJE): $availableMoves")

            if (availableMoves.isEmpty()) {
                Log.d(TAG, "  Nema dostupnih validnih poteza sa ${currentSquare} koji nisu već posećeni ili blokirani tranzitnim poljima. Prekidam generisanje putanje.")
                return emptyList()
            }

            // Nasumično odaberi sledeći potez
            val nextMove = availableMoves.random()
            path.add(nextMove)

            // Ažuriraj posećena polja
            visitedSquaresInPath.add(nextMove)
            if (piece.type == PieceType.ROOK || piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN) {
                val newVisitedBetween = getSquaresBetween(currentSquare, nextMove)
                visitedSquaresInPath.addAll(newVisitedBetween)
                Log.d(TAG, "    Dodata nova tranzitna polja u visited (ZA GENERISANJE): $newVisitedBetween")
            }

            currentSquare = nextMove // Ažuriraj trenutno polje za sledeću iteraciju
            Log.d(TAG, "  Dodato u putanju: ${nextMove}. Trenutna putanja: $path")
            Log.d(TAG, "  Sva posećena polja za ovu putanju (ZA GENERISANJE): $visitedSquaresInPath")
        }

        Log.d(TAG, "Uspešno generisana putanja: $path")
        return path
    }
}