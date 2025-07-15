package com.chess.chesspuzzle.rules

import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Move
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.ChessCore // Uverite se da je putanja do ChessCore ispravna

class Module2Rules : PuzzleRules {

    /**
     * Proverava da li je dati potez validan za izvršavanje u kontekstu pravila Modula 2.
     * Pravilo: Bela figura posle poteza ne sme biti na polju koje je napadnuto od strane protivnika.
     *
     * @param move Potez koji se proverava.
     * @param boardState Trenutno stanje table (PRE nego što je potez izvršen).
     * @return true ako je potez validan po pravilima modula, inače false.
     */
    override fun isMoveValidForModule(move: Move, boardState: ChessBoard): Boolean {
        val pieceToMove = boardState.getPiece(move.start) // Figura koja se pomera (mora biti bela)

        // Pretpostavljamo da je samo bela figura rešava zagonetke
        if (pieceToMove.color != PieceColor.WHITE) {
            return false // Ne bi trebalo da se desi ako getAllLegalChessMoves radi ispravno
        }

        // Simulišemo stanje table NAKON poteza
        val boardAfterMove = boardState.makeMoveAndCapture(move.start, move.end)
        val landingSquare = move.end // Polje na koje figura sleće

        // Proveravamo da li je polje na koje je figura sletela napadnuto od strane CRNIH figura.
        // Važno: Proveravamo na simuliranoj tabli (boardAfterMove).
        val isLandingSquareAttacked = boardAfterMove.isSquareAttacked(landingSquare, PieceColor.BLACK)

        // Potez je validan za modul 2 ako odredišno polje NIJE napadnuto od protivnika.
        return !isLandingSquareAttacked
    }

    /**
     * Proverava da li je cilj zagonetke (Modul 2) dostignut u datom stanju table.
     * Cilj: Pojesti sve crne figure.
     *
     * @param boardState Trenutno stanje table.
     * @return true ako na tabli nema više crnih figura, inače false.
     */
    override fun isGoalReached(boardState: ChessBoard): Boolean {
        // Cilj je dostignut ako nema više crnih figura na tabli
        return boardState.getPiecesMapFromBoard(PieceColor.BLACK).isEmpty()
    }

    /**
     * Generiše sve šahovski legalne poteze za datog igrača,
     * i filtrira ih prema pravilima Modula 2.
     * Pravilo: Potez mora završiti na polju koje nije napadnuto od strane crnih figura.
     *
     * @param boardState Trenutno stanje table.
     * @param playerColor Boja igrača za koga se generišu potezi (uvek WHITE za ove zagonetke).
     * @return Lista Move objekata koji predstavljaju dozvoljene poteze.
     */
    override fun getAllLegalChessMoves(boardState: ChessBoard, playerColor: PieceColor): List<Move> {
        // Počinjemo od svih osnovnih šahovskih poteza koje može da napravi beli igrač.
        val allBaseChessMoves = ChessCore.getAllLegalChessMoves(boardState, playerColor)

        // Sada filtriramo te poteze prema pravilu Modula 2:
        // Potez mora završiti na polju koje nije napadnuto od strane protivničke figure.
        return allBaseChessMoves.filter { move ->
            // Koristimo isMoveValidForModule za proveru sigurnosti odredišnog polja.
            // Ova provera mora biti obavljena za SVAKI potencijalni potez.
            isMoveValidForModule(move, boardState)
        }
    }
}