package com.chess.chesspuzzle

import android.util.Log
import java.util.LinkedList

object ChessCore {

    private const val TAG = "ChessCore"

    // --- Postojeće funkcije getValidMoves, getSquaresBetween, isPathClear, Square.offset ostaju nepromenjene ---

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
                            break
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
                            break
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
                            break
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
     * @param pieceType Tip figure (npr. PieceType.ROOK, PieceType.BISHOP, PieceType.QUEEN).
     * @return True ako je putanja čista, False inače.
     */
    fun isPathClear(board: ChessBoard, fromSquare: Square, toSquare: Square, pieceType: PieceType): Boolean {
        if (pieceType == PieceType.KNIGHT || pieceType == PieceType.KING) {
            return true
        }

        if (pieceType == PieceType.PAWN) {
            val squaresBetween = getSquaresBetween(fromSquare, toSquare)
            return squaresBetween.all { board.getPiece(it).type == PieceType.NONE }
        }

        val squaresBetween = getSquaresBetween(fromSquare, toSquare)
        for (square in squaresBetween) {
            if (board.getPiece(square).type != PieceType.NONE) {
                return false
            }
        }
        return true
    }

    // --- NOVA FUNKCIJA ZA PRONALAŽENJE CILJNIH POLJA ZA HVATANJE ---
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

        Log.d(TAG, "Traženje ${numCaptures} ciljnih polja za ${piece.type} sa ${startSquare}. Globalno zauzeti: $globalOccupiedAndTargetSquares")

        while (queue.isNotEmpty()) {
            val (currentSquare, currentTargets, visitedInPath) = queue.removeFirst()

            // Ako smo pronašli dovoljno meta, vratimo ih
            if (currentTargets.size == numCaptures) {
                Log.d(TAG, "Pronađeno ${numCaptures} ciljnih polja za ${piece.type}: $currentTargets")
                return currentTargets
            }

            val possibleMoves = getValidMoves(board, piece, currentSquare)

            for (nextSquare in possibleMoves.shuffled()) {
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
                // Knight i King ne moraju da brinu o "čistoj" putanji u ovom smislu, već samo o odredištu.
                if (piece.type == PieceType.ROOK || piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN) {
                    val squaresBetween = getSquaresBetween(currentSquare, nextSquare)
                    val isBlockedByObstacle = squaresBetween.any {
                        // Da li je tranzitno polje zauzeto bilo kojom figurom
                        board.getPiece(it).type != PieceType.NONE ||
                                // Ili da li je već planirana meta za drugu belu figuru
                                globalOccupiedAndTargetSquares.contains(it) ||
                                // Ili da li je već posećeno u ovoj putanji (ovo bi trebalo da bude redundantno s obzirom na visitedInPath.contains(nextSquare) iznad)
                                visitedInPath.contains(it)
                    }
                    if (isBlockedByObstacle) {
                        // Log.v(TAG, "Odbačeno ${nextSquare}: Putanja blokirana preprekom/rezervisanim poljem")
                        continue
                    }
                }

                // Ključna pretpostavka: Ako je stigao do ovde, nextSquare je PRAZNO polje i potencijalna meta
                val nextTargets = currentTargets.toMutableSet()
                if (board.getPiece(nextSquare).type == PieceType.NONE) { // Moramo uhvatiti PRAZNO polje
                    nextTargets.add(nextSquare)
                }

                // Dodaj stanje u red samo ako ga nismo već posetili sa istim ili boljim (više meta) rezultatom
                val newState = nextSquare to nextTargets.size
                if (visitedStates.add(newState)) { // Set.add returns true if element was added (not already present)
                    queue.add(Triple(nextSquare, nextTargets, nextVisitedInPath))
                }
            }
        }

        Log.d(TAG, "Nije pronađeno ${numCaptures} ciljnih polja za ${piece.type} sa ${startSquare}.")
        return null // Nije pronađena putanja željene dužine/broja meta
    }


    /**
     * Ekstenziona funkcija za Square koja vraća novo Square polje pomereno za date ofsete.
     * Vraća null ako je novo polje van table.
     */
    fun Square.offset(fileOffset: Int, rankOffset: Int): Square? {
        val newFileInt = this.file.code + fileOffset
        val newRank = this.rank + rankOffset

        if (newFileInt >= 'a'.code && newFileInt <= 'h'.code && newRank in 1..8) {
            return Square.fromCoordinates(newFileInt - 'a'.code, newRank - 1)
        }
        return null
    }
}