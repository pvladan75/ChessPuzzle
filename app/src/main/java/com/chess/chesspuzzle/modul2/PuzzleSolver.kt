package com.chess.chesspuzzle.modul2

import android.util.Log
import com.chess.chesspuzzle.* // Uvozimo sve iz glavnog paketa
import java.util.ArrayDeque // Preferiramo ArrayDeque za Queue

// Nova data klasa za praćenje stanja u solveru
data class SolverState(
    val board: ChessBoard,
    val whitePieceSquare: Square,
    val path: List<Move>,
    val blackPiecesCount: Int // Broj crnih figura na ovoj tabli
)

class PuzzleSolver {

    private val TAG = "PuzzleSolver"

    /**
     * Pokušava da reši zagonetku koristeći BFS algoritam.
     * Pronalazi najkraću sekvencu poteza bele figure da uhvati što više crnih figura,
     * a da pri tome bela figura nikada ne bude napadnuta od preostalih crnih figura.
     * Prioritet je uhvatiti sve figure (0 preostalih).
     *
     * @param initialBoard Početno stanje table.
     * @param whitePiece Početna bela figura (npr. dama).
     * @return Lista poteza koja predstavlja rešenje do stanja sa minimalnim brojem preostalih figura,
     * ili null ako rešenje ne postoji (npr. bela figura ne može nikoga da uhvati sigurno
     * ili se ne može doći do 0 preostalih figura).
     */
    fun solve(initialBoard: ChessBoard, whitePiece: Piece): List<Move>? {
        val queue = ArrayDeque<SolverState>()
        // visitedStates sada prati par (ChessBoard, Square bele figure) da bi jedinstveno identifikovao stanje
        val visitedStates = mutableSetOf<Pair<ChessBoard, Square>>()

        val initialWhitePieceSquare = initialBoard.getSquareOfPiece(whitePiece)
            ?: run {
                Log.e(TAG, "Initial white piece not found on the board.")
                return null
            }

        val initialBlackPiecesCount = initialBoard.pieces.values.count { it.color == PieceColor.BLACK }

        // Inicijalizuj red sa početnim stanjem
        val initialState = SolverState(initialBoard, initialWhitePieceSquare, emptyList(), initialBlackPiecesCount)
        queue.offer(initialState)
        visitedStates.add(Pair(initialBoard, initialWhitePieceSquare)) // Dodaj početno stanje u posećene

        var minRemainingBlackPieces = initialBlackPiecesCount
        var bestPath: List<Move>? = null

        // Ako je početno stanje već rešenje (nema crnih figura), to je idealno
        if (initialBlackPiecesCount == 0) {
            return emptyList()
        }

        while (queue.isNotEmpty()) {
            val currentState = queue.poll()
            val currentBoard = currentState.board
            val currentWhitePieceSquare = currentState.whitePieceSquare
            val currentPath = currentState.path
            val currentBlackPiecesCount = currentState.blackPiecesCount

            // Ažuriraj najbolje pronađeno rešenje
            // Dajemo prednost kraćim putevima ako je broj figura isti,
            // inače uvek uzimamo stanje sa manje figura.
            if (currentBlackPiecesCount < minRemainingBlackPieces) {
                minRemainingBlackPieces = currentBlackPiecesCount
                bestPath = currentPath
                if (minRemainingBlackPieces == 0) {
                    Log.d(TAG, "Puzzle solved! All pieces captured in ${bestPath.size} moves.")
                    return bestPath // Rešenje pronađeno (sve figure uhvaćene)
                }
            } else if (currentBlackPiecesCount == minRemainingBlackPieces) {
                // Ako je isti broj figura, ali je putanja kraća, uzmi nju
                if (bestPath == null || currentPath.size < bestPath.size) {
                    bestPath = currentPath
                }
            }

            // Ako je ovo stanje već rešilo zagonetku (0 figura), nema potrebe da se dalje istražuje
            if (currentBlackPiecesCount == 0) {
                continue
            }

            // Generiši sve moguće legalne poteze za belu figuru sa trenutne pozicije
            val whitePieceOnBoard = currentBoard.getPiece(currentWhitePieceSquare)
            // Get all possible moves, including non-capturing ones, to find optimal path
            val possibleMoves = getAllLegalMoves(currentBoard, currentWhitePieceSquare, whitePieceOnBoard)

            // Sortiraj poteze da hvatanja imaju prednost (opciono, ali pomaže u pronalaženju kraćih rešenja za hvatanje)
            val sortedMoves = possibleMoves.sortedByDescending { move ->
                currentBoard.getPiece(move.end).color == PieceColor.BLACK // Hvatanje crne figure
            }

            for (move in sortedMoves) {
                val nextBoard = currentBoard.movePiece(move)
                val newWhitePieceSquare = move.end

                // Proveri da li je bela figura napadnuta na novoj poziciji od preostalih crnih figura
                // Važno: Proveravamo na nextBoard, ali bez bele figure da bi simulirali napad crnih figura na prazno polje
                val boardWithoutTempWhitePiece = nextBoard.removePiece(newWhitePieceSquare)
                val attackerInfo = boardWithoutTempWhitePiece.isSquareAttackedByAnyOpponent(newWhitePieceSquare, PieceColor.WHITE)

                if (attackerInfo == null) { // Ako bela figura NIJE napadnuta na novoj poziciji
                    val newBlackPiecesCount = nextBoard.pieces.values.count { it.color == PieceColor.BLACK }
                    val newStateKey = Pair(nextBoard, newWhitePieceSquare)

                    if (newStateKey !in visitedStates) {
                        visitedStates.add(newStateKey)
                        queue.offer(SolverState(nextBoard, newWhitePieceSquare, currentPath + move, newBlackPiecesCount))
                    }
                } else {
                    // Log.d(TAG, "White piece (${whitePieceOnBoard.type}) would be attacked at ${newWhitePieceSquare} by ${attackerInfo.second.type} at ${attackerInfo.first} after move ${move}. Ignoring this path.")
                }
            }
        }

        // Ako se pretraga završi i nismo pronašli rešenje koje uklanja sve figure (minRemainingBlackPieces > 0)
        // I bestPath je null (što znači da bela figura nije mogla da napravi nijedan siguran potez)
        // Onda zaista nema rešenja.
        if (bestPath == null || minRemainingBlackPieces > 0) {
            Log.w(TAG, "No solution found where all black pieces are captured. Best solution found leaves ${minRemainingBlackPieces} pieces.")
            // Vraćamo null ako nije pronađeno rešenje koje hvata sve figure, što je uslov za validnu zagonetku.
            return null
        }

        // Ovo bi trebalo da se desi samo ako minRemainingBlackPieces == 0, ali smo stigli ovde
        // (tj. uslov return bestPath unutar while petlje je već ispunjen).
        // Ako stignemo ovde, a minRemainingBlackPieces je 0, to znači da je rešenje pronađeno.
        return bestPath
    }

    /**
     * Pomoćna funkcija koja vraća sve legalne poteze bele figure,
     * uključujući i hvatanja i pomeranja na prazna polja.
     * Ova logika je slična onoj iz Game klase, ali je izdvojena ovde za potrebe solvera.
     */
    private fun getAllLegalMoves(board: ChessBoard, startSquare: Square, whitePiece: Piece): List<Move> {
        val legalMoves = mutableListOf<Move>()
        val allPossibleMoves = whitePiece.type.getRawMoves(startSquare, whitePiece.color)

        for (endSquare in allPossibleMoves) {
            val potentialMove = Move(startSquare, endSquare)
            val pieceOnTarget = board.getPiece(endSquare)

            // Za potrebe solvera, dozvoljavamo hvatanje crne figure
            // ili pomeranje na prazno polje.
            // Provera da li je novo polje napadnuto vrši se u solve funkciji.
            if (pieceOnTarget.color == PieceColor.BLACK || pieceOnTarget.type == PieceType.NONE) {
                // Dodatna provera za klizeće figure
                if (whitePiece.type.isSlidingPiece()) {
                    if (!board.isPathClear(startSquare, endSquare)) {
                        continue // Putanja nije čista, nije legalan potez
                    }
                }
                legalMoves.add(potentialMove)
            }
        }
        return legalMoves
    }
}