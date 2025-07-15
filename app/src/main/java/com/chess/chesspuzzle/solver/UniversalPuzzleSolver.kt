package com.chess.chesspuzzle.solver

import PuzzleSolution
import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Move
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.Square
import com.chess.chesspuzzle.rules.PuzzleRules // Uvezite svoj PuzzleRules interfejs
import java.util.ArrayDeque // Preferiramo ArrayDeque za efikasnu implementaciju reda (Queue)

/**
 * Univerzalni solver za šahovske zagonetke koji koristi BFS algoritam.
 * Rešava zagonetke bazirane na pravilima definisanim u instanci [PuzzleRules].
 *
 * @param rules Instanca [PuzzleRules] koja definiše specifična pravila i cilj zagonetke.
 */
class UniversalPuzzleSolver(private val rules: PuzzleRules) {

    private val TAG = "UniversalPuzzleSolver"

    /**
     * Data klasa koja predstavlja stanje pretrage unutar BFS algoritma.
     *
     * @param board Trenutno stanje šahovske table.
     * @param currentPath Lista poteza koji su doveli do [board] stanja.
     * @param whitePieceSquare Trenutna pozicija bele figure (ako je relevantno i ako je samo jedna).
     * Koristi se za efikasnije praćenje posećenih stanja.
     */
    data class SolverState(
        val board: ChessBoard,
        val currentPath: List<Move>,
        val whitePieceSquare: Square? // Pozicija ključne bele figure za praćenje stanja
    )

    /**
     * Pokušava da reši zagonetku počevši od [initialBoard] za datog [playerColor].
     *
     * @param initialBoard Početno stanje šahovske table.
     * @param playerColor Boja igrača koji rešava zagonetku (obično PieceColor.WHITE).
     * @return [PuzzleSolution] objekat koji sadrži rezultat rešavanja.
     */
    fun solve(initialBoard: ChessBoard, playerColor: PieceColor = PieceColor.WHITE): PuzzleSolution {
        val queue = ArrayDeque<SolverState>()
        // Koristimo FEN notaciju table i poziciju bele figure za praćenje posećenih stanja
        // da bismo izbegli cikluse i ponovnu obradu istih stanja.
        val visitedStates = mutableSetOf<Pair<String, Square?>>()

        // Pronalazimo poziciju bele figure na početnoj tabli.
        // Pretpostavljamo da je samo jedna bela figura relevantna za ove zagonetke.
        val initialWhitePieceEntry = initialBoard.pieces.entries.find { it.value.color == playerColor }
        val initialWhitePieceSquare = initialWhitePieceEntry?.key

        // Inicijalno stanje za BFS
        val initialState = SolverState(
            board = initialBoard,
            currentPath = emptyList(),
            whitePieceSquare = initialWhitePieceSquare
        )
        queue.offer(initialState)
        visitedStates.add(Pair(initialBoard.toFEN(), initialWhitePieceSquare))

        Log.d(TAG, "Pokrećem univerzalni solver sa pravilima: ${rules::class.simpleName}")
        initialBoard.printBoard() // Prikaz početnog stanja

        while (queue.isNotEmpty()) {
            val currentState = queue.removeFirst()
            val currentBoard = currentState.board
            val currentPath = currentState.currentPath
            val currentWhitePieceSq = currentState.whitePieceSquare // Pozicija bele figure u trenutnom stanju

            // Korak 1: Proveravamo da li je cilj dostignut sa trenutnom tablom
            if (rules.isGoalReached(currentBoard)) {
                Log.d(TAG, "Cilj dostignut! Putanja: ${currentPath.joinToString(" -> ")}")
                return PuzzleSolution(true, currentPath, currentBoard, "Zagonetka rešena!")
            }

            // Korak 2: Generišemo sve legalne poteze za trenutnog igrača (beli)
            // Ova metoda iz 'rules' interfejsa već primenjuje specifična pravila modula
            // (npr. hvatanja za Modul 1, sigurnost polja za Modul 2, itd.).
            val legalMovesForPlayer = rules.getAllLegalChessMoves(currentBoard, playerColor)

            for (move in legalMovesForPlayer) {
                // Korak 3: Pre-provera validnosti poteza prema pravilima modula.
                // Iako 'getAllLegalChessMoves' već filtrira, 'isMoveValidForModule'
                // može imati dodatne provere koje zahtevaju simulaciju poteza ili dublju analizu.
                // Bitno je da ova provera koristi ORIGINALNU 'currentBoard' jer se 'move' tek treba desiti.
                if (!rules.isMoveValidForModule(move, currentBoard)) {
                    // Log.v(TAG, "Potez ${move} odbačen od strane isMoveValidForModule za ${rules::class.simpleName}")
                    continue // Potez nije validan po pravilima modula, pređi na sledeći
                }

                // Korak 4: Simuliramo potez i dobijamo novo stanje table
                val nextBoard = currentBoard.movePiece(move)
                val newWhitePieceSquare = move.end // Bela figura se pomera na krajnje polje poteza

                // Korak 5: Kreiramo ključ za posećena stanja i proveravamo da li je stanje već posećeno
                val nextStateKey = Pair(nextBoard.toFEN(), newWhitePieceSquare)

                if (nextStateKey !in visitedStates) {
                    visitedStates.add(nextStateKey)
                    val newPath = currentPath + move // Dodajemo trenutni potez u putanju
                    // Dodajemo novo stanje u red za dalju pretragu
                    queue.offer(SolverState(nextBoard, newPath, newWhitePieceSquare))
                }
            }
        }

        // Ako se red isprazni i cilj nije dostignut, znači da rešenje ne postoji.
        Log.w(TAG, "Nije pronađeno rešenje za zagonetku sa pravilima: ${rules::class.simpleName}.")
        return PuzzleSolution(false, emptyList(), initialBoard, "Nije pronađeno rešenje.")
    }
}