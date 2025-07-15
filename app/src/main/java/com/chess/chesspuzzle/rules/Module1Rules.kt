package com.chess.chesspuzzle.rules

import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Move
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square
import com.chess.chesspuzzle.modul1.ChessCore // Uverite se da je putanja do ChessCore ispravna

class Module1Rules : PuzzleRules {

    /**
     * Proverava da li je dati potez validan za izvršavanje u kontekstu pravila Modula 1.
     * Pravilo: Potez mora biti hvatanje crne figure.
     *
     * @param move Potez koji se proverava.
     * @param boardState Trenutno stanje table (PRE nego što je potez izvršen).
     * @return true ako je potez validan po pravilima modula, inače false.
     */
    override fun isMoveValidForModule(move: Move, boardState: ChessBoard): Boolean {
        val targetPiece = boardState.getPiece(move.end)
        // Potez je validan za modul 1 ako je to hvatanje crne figure
        return targetPiece.type != PieceType.NONE && targetPiece.color == PieceColor.BLACK
    }

    /**
     * Proverava da li je cilj zagonetke (Modul 1) dostignut u datom stanju table.
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
     * i filtrira ih prema pravilima Modula 1.
     * Pravilo: Svaki potez mora biti hvatanje crne figure.
     *
     * @param boardState Trenutno stanje table.
     * @param playerColor Boja igrača za koga se generišu potezi (uvek WHITE za ove zagonetke).
     * @return Lista Move objekata koji predstavljaju dozvoljene poteze.
     */
    override fun getAllLegalChessMoves(boardState: ChessBoard, playerColor: PieceColor): List<Move> {
        // Pozivamo getAllLegalChessMoves iz ChessCore-a, koja sada ne proverava šah na belog kralja.
        val allBaseChessMoves = ChessCore.getAllLegalChessMoves(boardState, playerColor)

        // Filtriramo ove poteze prema specifičnom pravilu Modula 1: svaki potez mora biti hvatanje crne figure.
        return allBaseChessMoves.filter { move ->
            // Koristimo isMoveValidForModule da proverimo pravilo (može i direktno ovde)
            isMoveValidForModule(move, boardState)
        }
    }
}