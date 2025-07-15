package com.chess.chesspuzzle.rules

import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Move
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square
import com.chess.chesspuzzle.modul1.ChessCore // Uverite se da je putanja do ChessCore ispravna

class Module3Rules : PuzzleRules {

    /**
     * Proverava da li je dati potez validan za izvršavanje u kontekstu pravila Modula 3.
     * Pravilo: Bela figura posle svakog poteza treba da bude na polju koje nije trenutno napadano od strane crnih figura.
     *
     * @param move Potez koji se proverava.
     * @param boardState Trenutno stanje table (PRE nego što je potez izvršen).
     * @return true ako je potez validan po pravilima modula, inače false.
     */
    override fun isMoveValidForModule(move: Move, boardState: ChessBoard): Boolean {
        val pieceToMove = boardState.getPiece(move.start)

        // Samo bele figure mogu da prave poteze u ovim zagonetkama
        if (pieceToMove.color != PieceColor.WHITE) {
            return false
        }

        // Simulišemo stanje table NAKON poteza
        val boardAfterMove = boardState.makeMoveAndCapture(move.start, move.end)
        val landingSquare = move.end // Polje na koje figura sleće

        // Proveravamo da li je polje na koje je figura sletela napadnuto od strane CRNIH figura.
        // Važno: Proveravamo na simuliranoj tabli (boardAfterMove).
        val isLandingSquareAttacked = boardAfterMove.isSquareAttacked(landingSquare, PieceColor.BLACK)

        // Ako je odredišno polje napadnuto od strane crnih figura, potez NIJE validan.
        // Takođe, ako je cilj bio da se pojede crni kralj, taj potez mora biti siguran.
        return !isLandingSquareAttacked
    }

    /**
     * Proverava da li je cilj zagonetke (Modul 3) dostignut u datom stanju table.
     * Cilj: Pojesti crnog kralja belom figurom.
     *
     * @param boardState Trenutno stanje table.
     * @return true ako je crni kralj pojeden, inače false.
     */
    override fun isGoalReached(boardState: ChessBoard): Boolean {
        // Pronađi da li postoji crni kralj na tabli
        val blackKingPresent = boardState.pieces.any { (square, piece) ->
            piece.type == PieceType.KING && piece.color == PieceColor.BLACK
        }
        // Cilj je dostignut ako nema crnog kralja na tabli
        return !blackKingPresent
    }

    /**
     * Generiše sve šahovski legalne poteze za datog igrača,
     * i filtrira ih prema pravilima Modula 3.
     * Pravilo: Potez mora završiti na polju koje nije napadnuto od strane crnih figura,
     * I ako je moguće, favorizuje poteze koji vode ka hvatanju crnog kralja.
     *
     * @param boardState Trenutno stanje table.
     * @param playerColor Boja igrača za koga se generišu potezi (uvek WHITE za ove zagonetke).
     * @return Lista Move objekata koji predstavljaju dozvoljene poteze.
     */
    override fun getAllLegalChessMoves(boardState: ChessBoard, playerColor: PieceColor): List<Move> {
        // Počinjemo od svih osnovnih šahovskih poteza koje može da napravi beli igrač.
        // ChessCore.getAllLegalChessMoves već uzima u obzir prepreke i osnovno kretanje figura.
        val allPossibleMoves = ChessCore.getAllLegalChessMoves(boardState, playerColor)

        // Filter: Svaki potez mora biti takav da figura SLETI na sigurno polje.
        val safeMoves = allPossibleMoves.filter { move ->
            isMoveValidForModule(move, boardState)
        }

        // Dodatni Filter/Prioritet za Modul 3: Potez mora da ima cilj da pojede crnog kralja,
        // ako je crni kralj dostupan za hvatanje na tom sigurnom polju.
        val movesToCaptureKing = safeMoves.filter { move ->
            val targetPiece = boardState.getPiece(move.end)
            targetPiece.type == PieceType.KING && targetPiece.color == PieceColor.BLACK
        }

        // Strategija: Ako postoji siguran potez koji odmah hvata kralja, to je najbolji potez.
        // Inače, vraćamo sve sigurne poteze. Solver će nastaviti pretragu.
        if (movesToCaptureKing.isNotEmpty()) {
            return movesToCaptureKing
        }

        // Ako ne postoji direktan potez za hvatanje kralja, vraćamo sve sigurne poteze
        // (tj. poteze na polja koja nisu napadnuta), omogućavajući solveru da traži putanju.
        return safeMoves
    }
}