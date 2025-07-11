package com.chess.chesspuzzle

import android.util.Log

// Ne treba nam import za ChessBoard, Piece, itd. ako su u istom paketu
// import com.chess.chesspuzzle.Piece
// import com.chess.chesspuzzle.PieceColor
// import com.chess.chesspuzzle.PieceType
// import com.chess.chesspuzzle.Square
// import com.chess.chesspuzzle.ChessBoard

object ChessCore {

    private const val TAG = "ChessCore"

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

                    while (r in 1..8 && f.toChar() in 'a'..'h') {
                        val targetSquare = Square(f.toChar(), r)
                        val targetPiece = board.getPiece(targetSquare)

                        if (targetPiece.type == PieceType.NONE) {
                            validMoves.add(targetSquare)
                        } else {
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

                    while (r in 1..8 && f.toChar() in 'a'..'h') {
                        val targetSquare = Square(f.toChar(), r)
                        val targetPiece = board.getPiece(targetSquare)

                        if (targetPiece.type == PieceType.NONE) {
                            validMoves.add(targetSquare)
                        } else {
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
                for (dr in -1..1) {
                    for (df in -1..1) {
                        if (dr == 0 && df == 0) continue
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
            PieceType.NONE -> { /* Nema poteza za prazno polje */ }
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

        val isHorizontal = rankDiff == 0 && fileDiff != 0
        val isVertical = fileDiff == 0 && rankDiff != 0
        val isDiagonal = kotlin.math.abs(fileDiff) == kotlin.math.abs(rankDiff) && fileDiff != 0

        if (!isHorizontal && !isVertical && !isDiagonal) {
            return squaresBetween
        }

        val df = if (fileDiff == 0) 0 else if (fileDiff > 0) 1 else -1
        val dr = if (rankDiff == 0) 0 else if (rankDiff > 0) 1 else -1

        var currentFile = from.file.code + df
        var currentRank = from.rank + dr

        while (true) {
            val currentSquareChar = currentFile.toChar()
            if (currentSquareChar == to.file && currentRank == to.rank) {
                break
            }
            if (currentRank !in 1..8 || currentSquareChar !in 'a'..'h') {
                break
            }
            squaresBetween.add(Square(currentSquareChar, currentRank))
            currentFile += df
            currentRank += dr
        }
        return squaresBetween
    }

    /**
     * Proverava da li je putanja između fromSquare i toSquare čista za datu vrstu figure.
     * Ovo je ključno za klizajuće figure (top, lovac, kraljica) jer ne mogu da preskaču.
     * Vitezovi, kraljevi i pešaci (za hvatanje) uvek imaju "čistu putanju" u ovom kontekstu.
     *
     * @param board Trenutna tabla.
     * @param fromSquare Početno polje figure.
     * @param toSquare Krajnje polje figure.
     * @param pieceType Tip figure (npr. PieceType.ROOK, PieceType.BISHOP, PieceType.QUEEN).
     * @return True ako je putanja čista, False inače.
     */
    fun isPathClear(board: ChessBoard, fromSquare: Square, toSquare: Square, pieceType: PieceType): Boolean {
        // Vitezovi (KNIGHT) mogu da preskaču figure, pa je njihova putanja uvek "čista"
        if (pieceType == PieceType.KNIGHT) {
            return true
        }

        // Kralj i Pešak se kreću samo jedno polje.
        if (pieceType == PieceType.KING || pieceType == PieceType.PAWN) {
            return true
        }

        // Za klizajuće figure (ROOK, BISHOP, QUEEN) proveravamo međupolja
        val squaresBetween = getSquaresBetween(fromSquare, toSquare)
        for (square in squaresBetween) {
            if (board.getPiece(square).type != PieceType.NONE) {
                return false
            }
        }
        return true
    }

    /**
     * Generiše putanju figure za dati broj poteza, osiguravajući da se figura ne vraća
     * na prethodno posećena polja unutar te putanje (uključujući početno polje i sva tranzitna polja).
     * Ova logika je isključivo za GENERISANJE ZAGONETKE.
     *
     * @param initialBoard Početna šahovska tabla.
     * @param piece Figura za koju se generiše putanja.
     * @param startSquare Početna pozicija figure.
     * @param numMoves Željeni broj poteza u putanji.
     * @return Lista Square objekata koji predstavljaju putanju. Vraća praznu listu ako putanja ne može biti generisana.
     */
    fun generatePiecePath(initialBoard: ChessBoard, piece: Piece, startSquare: Square, numMoves: Int): List<Square> {
        val path = mutableListOf<Square>()
        var currentSquare = startSquare
        // Koristimo Set za brzo proveravanje da li je polje već posećeno
        val visitedSquaresInPath = mutableSetOf<Square>()

        // Dodatni set za praćenje svih polja koje je figura dotakla (uključujući tranzitna za klizne figure)
        // Ovo je ključno za sprečavanje vraćanja figure na BILO KOJE polje koje je bilo deo njene putanje.
        val allTouchedSquares = mutableSetOf<Square>()
        allTouchedSquares.add(startSquare)

        Log.d(TAG, "Generisanje putanje za ${piece.type} sa ${startSquare} za ${numMoves} poteza.")
        Log.d(TAG, "Početna allTouchedSquares: $allTouchedSquares")

        for (i in 0 until numMoves) {
            val possibleMoves = getValidMoves(initialBoard, piece, currentSquare)
            Log.d(TAG, "  Potez ${i+1}: Trenutno polje: ${currentSquare}")
            Log.d(TAG, "  Mogući potezi iz ChessCore.getValidMoves (osnovna šahovska pravila): $possibleMoves")

            val availableMoves = possibleMoves.filter { targetSquare ->
                // Ako je ciljno polje već dotaknuto u putanji, odbij ga
                if (allTouchedSquares.contains(targetSquare)) {
                    Log.d(TAG, "    Potez do $targetSquare odbijen (ZA GENERISANJE): Ciljno polje je već dotaknuto u putanji.")
                    false
                } else {
                    // Za klizne figure, proveri da li tranzitna polja kolidiraju sa već dotaknutim
                    if (piece.type == PieceType.ROOK || piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN) {
                        val squaresBetween = getSquaresBetween(currentSquare, targetSquare)
                        val conflictsWithTouched = squaresBetween.any { allTouchedSquares.contains(it) }
                        if (conflictsWithTouched) {
                            Log.d(TAG, "    Potez do $targetSquare odbijen (ZA GENERISANJE): Tranzitna polja se preklapaju sa već dotaknutim: ${squaresBetween.filter { allTouchedSquares.contains(it) }}")
                            false
                        } else {
                            true
                        }
                    } else {
                        true
                    }
                }
            }

            Log.d(TAG, "  Dostupni potezi nakon filtriranja (anti-vraćanje za GENERISANJE): $availableMoves")

            if (availableMoves.isEmpty()) {
                Log.d(TAG, "  Nema dostupnih validnih poteza sa ${currentSquare} koji nisu već dotaknuti ili blokirani tranzitnim poljima. Prekidam generisanje putanje.")
                return emptyList()
            }

            val nextMove = availableMoves.random()
            path.add(nextMove)

            // Ažuriraj sva dotaknuta polja (ciljno i tranzitna za klizne figure)
            allTouchedSquares.add(nextMove)
            if (piece.type == PieceType.ROOK || piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN) {
                val newTouchedBetween = getSquaresBetween(currentSquare, nextMove)
                allTouchedSquares.addAll(newTouchedBetween)
                Log.d(TAG, "    Dodata nova tranzitna polja u allTouchedSquares (ZA GENERISANJE): $newTouchedBetween")
            }

            currentSquare = nextMove
            Log.d(TAG, "  Dodato u putanju: ${nextMove}. Trenutna putanja: $path")
            Log.d(TAG, "  Sva dotaknuta polja za ovu putanju (ZA GENERISANJE): $allTouchedSquares")
        }

        Log.d(TAG, "Uspešno generisana putanja: $path")
        return path
    }
}