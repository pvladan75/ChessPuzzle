package com.chess.chesspuzzle

import android.util.Log
import java.util.LinkedList // Potrebno za BFS u findCaptureTargetSquares

object ChessCore {

    private const val TAG = "ChessCore"

    /**
     * Vraća listu svih legalnih polja na koja se data figura može pomeriti
     * sa date početne pozicije na datoj tabli, u skladu sa osnovnim šahovskim pravilima KRETANJA.
     * NE uzima u obzir da li potez ostavlja/stavlja kralja u šah.
     *
     * @param board Trenutna šahovska tabla.
     * @param piece Figura za koju se traže potezi.
     * @param fromSquare Početna pozicija figure.
     * @return Lista Square objekata koji predstavljaju legalne destinacije.
     */
    fun getValidMoves(board: ChessBoard, piece: Piece, fromSquare: Square): List<Square> {
        val validMoves = mutableListOf<Square>()

        when (piece.type) {
            PieceType.PAWN -> {
                val direction = if (piece.color == PieceColor.WHITE) 1 else -1

                // Kretanje jedan korak napred
                val oneStepFwd = fromSquare.offset(0, direction)
                if (oneStepFwd != null && board.getPiece(oneStepFwd).type == PieceType.NONE) {
                    validMoves.add(oneStepFwd)
                }

                // Kretanje dva koraka napred (sa početne pozicije)
                if ((piece.color == PieceColor.WHITE && fromSquare.rank == 2) || (piece.color == PieceColor.BLACK && fromSquare.rank == 7)) {
                    val twoStepsFwd = fromSquare.offset(0, 2 * direction)
                    if (twoStepsFwd != null && board.getPiece(twoStepsFwd).type == PieceType.NONE && oneStepFwd != null && board.getPiece(oneStepFwd).type == PieceType.NONE) {
                        validMoves.add(twoStepsFwd)
                    }
                }

                // Hvatanje dijagonalno
                val attackOffsets = listOf(Pair(-1, direction), Pair(1, direction))
                for ((df, dr) in attackOffsets) {
                    val attackSquare = fromSquare.offset(df, dr)
                    if (attackSquare != null) {
                        val targetPiece = board.getPiece(attackSquare)
                        if (targetPiece.type != PieceType.NONE && targetPiece.color != piece.color) {
                            validMoves.add(attackSquare)
                        }
                    }
                }
            }
            PieceType.ROOK -> {
                val directions = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))

                for ((df, dr) in directions) {
                    var currentSquare: Square? = fromSquare.offset(df, dr)
                    while (currentSquare != null) {
                        val targetPiece = board.getPiece(currentSquare)

                        if (targetPiece.type == PieceType.NONE) {
                            validMoves.add(currentSquare)
                        } else {
                            if (targetPiece.color != piece.color) {
                                validMoves.add(currentSquare)
                            }
                            break // Zaustavi se ako je naišla na figuru
                        }
                        currentSquare = currentSquare.offset(df, dr)
                    }
                }
            }
            PieceType.KNIGHT -> {
                val knightOffsets = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for ((df, dr) in knightOffsets) {
                    val targetSquare = fromSquare.offset(df, dr)
                    if (targetSquare != null) {
                        val targetPiece = board.getPiece(targetSquare)
                        if (targetPiece.type == PieceType.NONE || targetPiece.color != piece.color) {
                            validMoves.add(targetSquare)
                        }
                    }
                }
            }
            PieceType.BISHOP -> {
                val directions = listOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))

                for ((df, dr) in directions) {
                    var currentSquare: Square? = fromSquare.offset(df, dr)
                    while (currentSquare != null) {
                        val targetPiece = board.getPiece(currentSquare)

                        if (targetPiece.type == PieceType.NONE) {
                            validMoves.add(currentSquare)
                        } else {
                            if (targetPiece.color != piece.color) {
                                validMoves.add(currentSquare)
                            }
                            break // Zaustavi se ako je naišla na figuru
                        }
                        currentSquare = currentSquare.offset(df, dr)
                    }
                }
            }
            PieceType.QUEEN -> {
                val directions = listOf(
                    Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1),
                    Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
                )

                for ((df, dr) in directions) {
                    var currentSquare: Square? = fromSquare.offset(df, dr)
                    while (currentSquare != null) {
                        val targetPiece = board.getPiece(currentSquare)

                        if (targetPiece.type == PieceType.NONE) {
                            validMoves.add(currentSquare)
                        } else {
                            if (targetPiece.color != piece.color) {
                                validMoves.add(currentSquare)
                            }
                            break // Zaustavi se ako je naišla na figuru
                        }
                        currentSquare = currentSquare.offset(df, dr)
                    }
                }
            }
            PieceType.KING -> {
                for (df in -1..1) {
                    for (dr in -1..1) {
                        if (df == 0 && dr == 0) continue
                        val targetSquare = fromSquare.offset(df, dr)
                        if (targetSquare != null) {
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

        var currentFileInt = from.file.code + df
        var currentRank = from.rank + dr

        while (true) {
            val currentSquare = Square.fromCoordinates(currentFileInt - 'a'.code, currentRank - 1)

            if (currentSquare == to) {
                break
            }
            // Provera da li je currentSquare validno polje pre dodavanja
            if (currentSquare.file !in 'a'..'h' || currentSquare.rank !in 1..8) {
                break
            }
            squaresBetween.add(currentSquare)
            currentFileInt += df
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
     * @param pieceType Tip figure (npr. PieceType.ROOK, PieceType.BISHop, PieceType.QUEEN).
     * @return True ako je putanja čista, False inače.
     */
    fun isPathClear(board: ChessBoard, fromSquare: Square, toSquare: Square, pieceType: PieceType): Boolean {
        if (pieceType == PieceType.KNIGHT || pieceType == PieceType.KING) {
            return true
        }

        // Za pešaka se isPathClear koristi samo za kretanje napred (jedan ili dva koraka)
        // Hvatanje je dijagonalno i ne zahteva proveru putanje.
        if (pieceType == PieceType.PAWN) {
            val squaresBetween = getSquaresBetween(fromSquare, toSquare)
            return squaresBetween.all { board.getPiece(it).type == PieceType.NONE }
        }

        // Za klizeće figure
        val squaresBetween = getSquaresBetween(fromSquare, toSquare)
        for (square in squaresBetween) {
            if (board.getPiece(square).type != PieceType.NONE) {
                return false
            }
        }
        return true
    }

    /**
     * Traži `numCaptures` jedinstvenih Praznih Polja koja data bela figura može da uhvati.
     * Putanja figure ne sme da se vraća na prethodno posećena polja (uključujući tranzitna).
     * Ova funkcija ne postavlja figure, samo pronalazi KANDIDATE za ciljna polja.
     *
     * @param board Trenutna šahovska tabla (sa postavljenim belim figurama).
     * @param piece Figura za koju se traže putanje.
     * @param startSquare Početna pozicija figure.
     * @param numCaptures Željeni broj hvatanja (ciljnih polja).
     * @param globalOccupiedAndTargetSquares Skup SVIH polja koja su već zauzeta (bele figure)
     * ili su već PREDVIĐENA kao mete za druge bele figure.
     * Ova polja NE SMEJU biti izabrana kao nove mete, niti se figura
     * sme kretati kroz njih ako je klizna figura.
     * @return Skup Square objekata koji predstavljaju predložena ciljna polja. Vraća null ako se ne pronađe dovoljno meta.
     */
    fun findCaptureTargetSquares(
        board: ChessBoard,
        piece: Piece, // Ovo je bela figura
        startSquare: Square,
        numCaptures: Int,
        globalOccupiedAndTargetSquares: Set<Square> // Uključuje i bele figure i već planirane crne mete
    ): Set<Square>? {
        if (numCaptures == 0) return emptySet()

        // Red za BFS: Triple (trenutno_polje, sakupljene_mete_za_ovu_putanju, posećena_polja_na_trenutnoj_putanji_ukljucujuci_tranzitna)
        val queue = LinkedList<Triple<Square, MutableSet<Square>, MutableSet<Square>>>()
        // Inicijalno stanje: Početno polje, prazan set meta, set posećenih polja sa samo startSquare
        queue.add(Triple(startSquare, mutableSetOf(), mutableSetOf(startSquare)))

        // Set za praćenje posećenih stanja u BFS-u, da se izbegnu duplikati i ciklusi:
        // (trenutno_polje, broj_sakupljenih_meta_do_sada)
        val visitedStates = mutableSetOf<Pair<Square, Int>>()
        visitedStates.add(startSquare to 0)

        Log.d(TAG, "Traženje $numCaptures ciljnih polja za ${piece.type} sa ${startSquare}. Globalno zauzeti: $globalOccupiedAndTargetSquares")

        while (queue.isNotEmpty()) {
            val (currentSquare, currentTargets, visitedInPath) = queue.removeFirst()

            // Ako smo pronašli dovoljno meta, vratimo ih
            if (currentTargets.size == numCaptures) {
                Log.d(TAG, "Pronađeno $numCaptures ciljnih polja za ${piece.type}: $currentTargets")
                return currentTargets
            }

            // Koristi getValidMoves za sirove šahovske poteze
            val possibleMoves = getValidMoves(board, piece, currentSquare)

            for (nextSquare in possibleMoves.shuffled()) { // Randomizacija može pomoći u raznolikosti generisanih pozicija
                // Predviđamo nova posećena polja za sledeće stanje
                val nextVisitedInPath = visitedInPath.toMutableSet()
                nextVisitedInPath.add(nextSquare)
                nextVisitedInPath.addAll(getSquaresBetween(currentSquare, nextSquare)) // Dodaj i tranzitna polja

                // Proveravamo validnost nextSquare za generisanje zagonetke:
                // 1. Ne sme se vraćati na polje koje je već posećeno u ovoj SPECIFIČNOJ putanji
                // (visitedInPath već uključuje currentSquare i prethodne tranzitne, nextSquare je novi)
                if (visitedInPath.contains(nextSquare)) {
                    // Log.v(TAG, "Odbačeno ${nextSquare}: Već posećeno u putanji")
                    continue
                }

                // 2. Ne sme da bude na polju koje je već zauzeto belom figurom ili planirano kao meta
                // (ovo je globalna provera za sve figure i planirane mete)
                if (globalOccupiedAndTargetSquares.contains(nextSquare)) {
                    // Log.v(TAG, "Odbačeno ${nextSquare}: Globalno zauzeto/rezervisano")
                    continue
                }

                // 3. Za klizne figure (ROOK, BISHOP, QUEEN), proveri da li putanja prelazi preko
                // globalno zauzetih ili već posećenih polja (osim startSquare i nextSquare).
                if (piece.type.isSlidingPiece()) { // Koristi isSlidingPiece ekstenziju
                    val squaresBetween = getSquaresBetween(currentSquare, nextSquare)
                    val isBlockedByObstacle = squaresBetween.any {
                        // Da li je tranzitno polje zauzeto bilo kojom figurom
                        board.getPiece(it).type != PieceType.NONE ||
                                // Ili da li je već planirana meta za drugu belu figuru
                                globalOccupiedAndTargetSquares.contains(it) ||
                                // Ili da li je već posećeno u ovoj putanji
                                visitedInPath.contains(it)
                    }
                    if (isBlockedByObstacle) {
                        // Log.v(TAG, "Odbačeno ${nextSquare}: Putanja blokirana preprekom/rezervisanim poljem")
                        continue
                    }
                }

                // Ključna pretpostavka: Ako je stigao do ovde, nextSquare je PRAZNO polje i potencijalna meta
                val nextTargets = currentTargets.toMutableSet()
                if (board.getPiece(nextSquare).type == PieceType.NONE) { // Moramo uhvatiti PRAZNO polje za generisanje
                    nextTargets.add(nextSquare)
                }

                // Dodaj stanje u red samo ako ga nismo već posetili sa istim ili boljim (više meta) rezultatom
                val newState = nextSquare to nextTargets.size
                if (visitedStates.add(newState)) { // Set.add vraća true ako je element dodat (nije već prisutan)
                    queue.add(Triple(nextSquare, nextTargets, nextVisitedInPath))
                }
            }
        }

        Log.d(TAG, "Nije pronađeno $numCaptures ciljnih polja za ${piece.type} sa ${startSquare}.")
        return null // Nije pronađena putanja željene dužine/broja meta
    }

    /**
     * Ekstenziona funkcija za Square koja vraća novo Square polje pomereno za date ofsete.
     * Vraća null ako je novo polje van table.
     */
    fun Square.offset(fileOffset: Int, rankOffset: Int): Square? {
        val newFileInt = this.file.code + fileOffset
        val newRank = this.rank + rankOffset

        // BOARD_SIZE je konstanta u top-level paketu com.chess.chesspuzzle
        if (newFileInt >= 'a'.code && newFileInt < ('a'.code + BOARD_SIZE) && newRank > 0 && newRank <= BOARD_SIZE) {
            return Square.fromCoordinates(newFileInt - 'a'.code, newRank - 1)
        }
        return null
    }

    /**
     * Pomoćna funkcija za parsiranje UCI stringa "fromSquaretoSquare" u Pair<Square, Square>.
     * Npr. "e2e4" -> Pair(Square('e',2), Square('e',4))
     */
    fun parseUciToSquares(uci: String): Pair<Square, Square>? {
        if (uci.length != 4) {
            Log.e(TAG, "Nevažeći UCI string: $uci")
            return null
        }
        try {
            val fromFile = uci[0]
            val fromRank = uci[1].toString().toInt()
            val toFile = uci[2]
            val toRank = uci[3].toString().toInt()
            return Pair(Square(fromFile, fromRank), Square(toFile, toRank))
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri parsiranju UCI stringa '$uci': ${e.message}", e)
            return null
        }
    }

    /**
     * Prebrojava figure određene boje na datoj šahovskoj tabli.
     *
     * @param board Šahovska tabla.
     * @param color Boja figura koje se broje.
     * @return Mapa gde je ključ PieceType, a vrednost broj figura tog tipa i boje.
     */
    fun getPieceCountsByColor(board: ChessBoard, color: PieceColor): Map<PieceType, Int> {
        val counts = mutableMapOf<PieceType, Int>()
        for (fileChar in 'a'..'h') {
            for (rankInt in 1..8) {
                val square = Square(fileChar, rankInt)
                val piece = board.getPiece(square)
                if (piece.type != PieceType.NONE && piece.color == color) {
                    counts[piece.type] = counts.getOrDefault(piece.type, 0) + 1
                }
            }
        }
        return counts
    }

    /**
     * Generiše SVE šahovski legalne poteze za datog igrača,
     * uzimajući u obzir samo pravila kretanja figura i prepreke.
     * NE proverava da li potez ostavlja/stavlja kralja u šah,
     * jer ta provera nije relevantna za zahteve vaših zagonetki (beli kralj nije u šahu, crni može biti).
     *
     * @param board Trenutna šahovska tabla.
     * @param playerColor Boja igrača za koga se generišu potezi.
     * @return Lista Move objakata koji predstavljaju sve šahovski legalne poteze.
     */
    fun getAllLegalChessMoves(board: ChessBoard, playerColor: PieceColor): List<Move> {
        val legalMoves = mutableListOf<Move>()
        val pieces = board.getPiecesMapFromBoard(playerColor) // Dobavi sve figure date boje

        for ((fromSquare, piece) in pieces) {
            val validDestinations = getValidMoves(board, piece, fromSquare) // Koristi vašu postojeću getValidMoves

            for (toSquare in validDestinations) {
                // S obzirom na to da ne proveravamo šah na belog kralja, svaki potez koji getValidMoves vrati
                // je "legalan" u kontekstu kretanja figura i prepreka.
                legalMoves.add(Move(fromSquare, toSquare))
            }
        }
        return legalMoves
    }
}