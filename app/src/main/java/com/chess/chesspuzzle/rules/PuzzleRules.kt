// Pakovanje gde se nalazi PuzzleRules interfejs (pretpostavljam da je u 'rules' podpaketu unutar glavnog paketa)
package com.chess.chesspuzzle.rules

// Ispravljeni importi koji ukazuju na vaše definicije u top-level paketu
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Move
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.Square // Square je vaša 'Position' klasa

interface PuzzleRules {
    /**
     * Proverava da li je dati potez legalan za izvršavanje **u kontekstu pravila ovog modula**.
     * Ovo je mesto za dodatne provere izvan osnovnih šahovskih pravila (npr. "ne smeš u branjeno polje").
     * Pretpostavlja se da je potez već šahovski legalan.
     */
    fun isMoveValidForModule(move: Move, boardState: ChessBoard): Boolean

    /**
     * Proverava da li je cilj zagonetke dostignut u datom stanju table.
     */
    fun isGoalReached(boardState: ChessBoard): Boolean

    /**
     * Generiše sve **šahovski legalne poteze** za datog igrača u trenutnom stanju table.
     * Ova metoda ne bi trebalo da sadrži logiku specifičnu za modul, već samo osnovna šahovska pravila
     * (kretanje figura, provere šaha, blokade, itd.).
     * Implementacija bi verovatno pozivala neku globalnu ChessCore ili GameMechanics klasu.
     */
    fun getAllLegalChessMoves(boardState: ChessBoard, playerColor: PieceColor): List<Move>
}