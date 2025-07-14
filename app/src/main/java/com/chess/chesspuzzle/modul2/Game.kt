// Game.kt
package com.chess.chesspuzzle

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random // Koristimo kotlin.random.Random kao što je i dogovoreno

// Važno: svi enumi i data klase (PieceType, PieceColor, Square, Piece, ChessBoard, ScoreEntry, Difficulty)
// treba da budu definisani isključivo u ChessDefinitions.kt
// SoundManager treba da bude definisan isključivo u SoundManager.kt
// Ovdje ih samo uvozimo.
import com.chess.chesspuzzle.SoundManager // Uvoz SoundManager objekta
// Ne treba importovati SoundType ovde, jer ga SoundManager.kt ne definiše kao poseban enum

class Game {
    private val _board = MutableStateFlow(ChessBoard.createEmpty())
    val board: StateFlow<ChessBoard> = _board.asStateFlow()

    private val _whiteQueen = MutableStateFlow<Piece?>(null)
    val whiteQueen: StateFlow<Piece?> = _whiteQueen.asStateFlow()

    private val _blackPieces = MutableStateFlow<Map<Square, Piece>>(emptyMap())
    val blackPieces: StateFlow<Map<Square, Piece>> = _blackPieces.asStateFlow()

    private val _moveCount = MutableStateFlow(0)
    val moveCount: StateFlow<Int> = _moveCount.asStateFlow()

    private val _respawnCount = MutableStateFlow(0)
    val respawnCount: StateFlow<Int> = _respawnCount.asStateFlow()

    private val _puzzleSolved = MutableStateFlow(false)
    val puzzleSolved: StateFlow<Boolean> = _puzzleSolved.asStateFlow()

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver.asStateFlow()

    private val _lastAttackerPosition = MutableStateFlow<Pair<Int, Int>?>(null)
    val lastAttackerPosition: StateFlow<Pair<Int, Int>?> = _lastAttackerPosition.asStateFlow()

    private val _puzzleTime = MutableStateFlow(0)
    val puzzleTime: StateFlow<Int> = _puzzleTime.asStateFlow()

    private val _currentPuzzleScore = MutableStateFlow(0)
    val currentPuzzleScore: StateFlow<Int> = _currentPuzzleScore.asStateFlow()

    private val _isModul2Mode = MutableStateFlow(false)
    val isModul2Mode: StateFlow<Boolean> = _isModul2Mode.asStateFlow()


    fun initializeGame(initialBoard: ChessBoard) {
        _board.value = initialBoard
        _moveCount.value = 0
        _respawnCount.value = 0
        _puzzleSolved.value = false
        _isGameOver.value = false
        _lastAttackerPosition.value = null
        _puzzleTime.value = 0
        _currentPuzzleScore.value = 0
        _isModul2Mode.value = false // Resetuj mod prilikom inicijalizacije

        val whitePieces = initialBoard.getPiecesMapFromBoard(PieceColor.WHITE)
        _whiteQueen.value = whitePieces.entries.find { it.value.type == PieceType.QUEEN }?.value

        val blackPiecesMap = initialBoard.getPiecesMapFromBoard(PieceColor.BLACK)
        _blackPieces.value = blackPiecesMap
    }

    fun setModul2Mode(isModul2: Boolean) {
        _isModul2Mode.value = isModul2
    }

    fun incrementPuzzleTime() {
        _puzzleTime.value = _puzzleTime.value + 1
    }

    suspend fun makeMove(move: Move): Boolean {
        val fromSquare = move.from
        val toSquare = move.to
        val pieceToMove = _board.value.getPiece(fromSquare)

        if (pieceToMove.color != PieceColor.WHITE) {
            Log.d("Game", "Cannot move a non-white piece.")
            return false
        }

        val legalMoves = getLegalMoves(fromSquare)
        if (!legalMoves.contains(toSquare)) {
            Log.d("Game", "Illegal move from ${fromSquare} to ${toSquare}.")
            return false
        }

        val capturedPiece = _board.value.getPiece(toSquare) // Proveri da li ima figure na ciljnom polju
        var newBoard = _board.value.makeMoveAndCapture(fromSquare, toSquare) // Koristi makeMoveAndCapture

        _board.value = newBoard
        _moveCount.value++

        if (capturedPiece.type != PieceType.NONE && capturedPiece.color == PieceColor.BLACK) {
            // POZIV ZVUKA: true za uspeh (hvatanje)
            SoundManager.playSound(true)
            val updatedBlackPieces = _blackPieces.value.toMutableMap()
            updatedBlackPieces.remove(toSquare)
            _blackPieces.value = updatedBlackPieces.toMap()

            if (_blackPieces.value.isEmpty()) {
                _puzzleSolved.value = true
                _isGameOver.value = true
                Log.d("Game", "Puzzle Solved! All black pieces captured.")
            }
        } else {
            // POZIV ZVUKA: false za neuspeh/regularan potez
            SoundManager.playSound(false)
        }

        if (_isModul2Mode.value && pieceToMove.type == PieceType.QUEEN) {
            val currentQueenSquare = newBoard.getPiecesMapFromBoard(PieceColor.WHITE)
                .entries.find { it.value.type == PieceType.QUEEN }?.key

            if (currentQueenSquare != null) {
                val attackingPieceAndSquare = newBoard.isSquareAttackedByAnyOpponent(currentQueenSquare, PieceColor.WHITE)

                if (attackingPieceAndSquare != null) {
                    val (attackerSquare, _) = attackingPieceAndSquare
                    Log.d("Game", "White Queen attacked by ${newBoard.getPiece(attackerSquare).type} at ${attackerSquare}!")
                    // POZIV ZVUKA: false za šah (signal upozorenja)
                    SoundManager.playSound(false)

                    _respawnCount.value++
                    _lastAttackerPosition.value = Pair(attackerSquare.rankIndex, attackerSquare.fileIndex)

                    val respawnedBoard = respawnQueen(newBoard, currentQueenSquare)
                    _board.value = respawnedBoard
                    _lastAttackerPosition.value = null
                    // POZIV ZVUKA: true za spawn (uspeh respawna)
                    SoundManager.playSound(true)

                } else {
                    _lastAttackerPosition.value = null
                }
            }
        }
        return true
    }

    suspend fun undoMove(): Boolean {
        // Implementiraj logiku za undo, trenutno samo dekrementira broj poteza
        // Potrebna je istorija table i poteza za pravilan undo
        Log.w("Game", "Undo not fully implemented. Consider implementing a full board history.")
        if (_moveCount.value > 0) {
            _moveCount.value--
            // Ako imaš stack prošlih tabli, ovde bi vratio prethodno stanje:
            // _board.value = previousBoardState
            // _blackPieces.value = previousBlackPiecesState
            // itd.
            return true
        }
        return false
    }

    fun getLegalMoves(fromSquare: Square): List<Square> {
        val piece = _board.value.getPiece(fromSquare)
        if (piece.type == PieceType.NONE || piece.color == PieceColor.NONE) {
            return emptyList()
        }

        val possibleMoves = piece.type.getRawMoves(fromSquare, piece.color)
        val legalMoves = mutableListOf<Square>()

        for (targetSquare in possibleMoves) {
            if (piece.type.isSlidingPiece()) {
                if (!_board.value.isPathClear(fromSquare, targetSquare)) {
                    continue
                }
            }

            val targetPiece = _board.value.getPiece(targetSquare)

            if (targetPiece.color == piece.color) {
                continue
            }

            if (_isModul2Mode.value) {
                if (targetPiece.type == PieceType.NONE || targetPiece.color == PieceColor.BLACK) {
                    legalMoves.add(targetSquare)
                }
            } else {
                legalMoves.add(targetSquare)
            }
        }
        return legalMoves
    }

    private fun respawnQueen(currentBoard: ChessBoard, oldQueenSquare: Square): ChessBoard {
        var newBoard = currentBoard.removePiece(oldQueenSquare)
        val random = Random(System.currentTimeMillis())

        val emptySquares = Square.ALL_SQUARES.filter { newBoard.getPiece(it).type == PieceType.NONE }
            .toMutableList()

        if (emptySquares.isEmpty()) {
            Log.e("Game", "No empty squares to respawn queen!")
            return currentBoard
        }

        var newQueenSquare: Square
        var placed = false
        while (!placed) {
            newQueenSquare = emptySquares.random(random) // Koristimo .random() iz kotlin.random
            if (!newBoard.isSquareAttacked(newQueenSquare, PieceColor.BLACK)) { // Proveri da li je napadnuto od CRNIH figura
                newBoard = newBoard.setPiece(Piece(PieceType.QUEEN, PieceColor.WHITE), newQueenSquare)
                _whiteQueen.value = Piece(PieceType.QUEEN, PieceColor.WHITE)
                placed = true
                Log.d("Game", "Queen respawned at ${newQueenSquare}")
            } else {
                Log.d("Game", "Attempted respawn at ${newQueenSquare} but it's attacked. Trying again.")
                emptySquares.remove(newQueenSquare)
                if (emptySquares.isEmpty()) {
                    Log.e("Game", "All empty squares are attacked after respawn, forcing placement.")
                    // Fallback: ako su sva prazna polja napadnuta, stavi je bilo gde
                    newQueenSquare = Square.ALL_SQUARES.filter { currentBoard.getPiece(it).type == PieceType.NONE }.random(random)
                    newBoard = newBoard.setPiece(Piece(PieceType.QUEEN, PieceColor.WHITE), newQueenSquare)
                    _whiteQueen.value = Piece(PieceType.QUEEN, PieceColor.WHITE)
                    placed = true
                }
            }
        }
        return newBoard
    }
}

// --- EXTENSION FUNCTIONS (Mogu biti i u ChessDefinitions.kt ili u posebnom fajlu npr. ChessExtensions.kt) ---
// Ove funkcije treba da budu na odgovarajućem mestu (kao top-level funkcije ili ekstenzije u ChessDefinitions.kt)

// Moraju biti definisane van Game klase i u ChessDefinitions.kt ili drugom zajedničkom fajlu
fun PieceType.getRawMoves(fromSquare: Square, pieceColor: PieceColor): List<Square> {
    val moves = mutableListOf<Square>()
    val (file, rank) = fromSquare

    when (this) {
        PieceType.PAWN -> {
            val direction = if (pieceColor == PieceColor.WHITE) 1 else -1
            val startRank = if (pieceColor == PieceColor.WHITE) 2 else 7

            // Standardni potez napred (bez hvatanja)
            if (rank + direction in 1..8) {
                // Za pešaka, getRawMoves bi trebalo da vrati samo "putanje"
                // Validacija da li je polje prazno ili zauzeto protivničkom figurom radi se u makeMove/getLegalMoves
                moves.add(Square(file, rank + direction))
            }
            // Dupli potez sa početne pozicije
            if (rank == startRank && rank + 2 * direction in 1..8) {
                moves.add(Square(file, rank + 2 * direction))
            }
            // Dijagonalni potezi (hvatanje) - pešak se drugačije hvata od ostalih figura
            // Ovi potezi su uvek "sirovi", provera da li je figura na tom polju radi se kasnije
            if (file - 1 >= 'a' && rank + direction in 1..8) {
                moves.add(Square(file - 1, rank + direction))
            }
            if (file + 1 <= 'h' && rank + direction in 1..8) {
                moves.add(Square(file + 1, rank + direction))
            }
        }
        PieceType.KNIGHT -> {
            val knightMoves = listOf(
                Pair(1, 2), Pair(1, -2), Pair(-1, 2), Pair(-1, -2),
                Pair(2, 1), Pair(2, -1), Pair(-2, 1), Pair(-2, -1)
            )
            for ((df, dr) in knightMoves) {
                val newFile = file + df
                val newRank = rank + dr
                if (newFile in 'a'..'h' && newRank in 1..8) {
                    moves.add(Square(newFile, newRank))
                }
            }
        }
        PieceType.BISHOP -> {
            // Dijagonalni potezi
            for (i in 1..7) { // Gore-desno
                if (file + i <= 'h' && rank + i <= 8) moves.add(Square(file + i, rank + i))
            }
            for (i in 1..7) { // Dole-desno
                if (file + i <= 'h' && rank - i >= 1) moves.add(Square(file + i, rank - i))
            }
            for (i in 1..7) { // Gore-levo
                if (file - i >= 'a' && rank + i <= 8) moves.add(Square(file - i, rank + i))
            }
            for (i in 1..7) { // Dole-levo
                if (file - i >= 'a' && rank - i >= 1) moves.add(Square(file - i, rank - i))
            }
        }
        PieceType.ROOK -> {
            // Horizontalni i vertikalni potezi
            for (fChar in 'a'..'h') { // Horizontalno
                if (fChar != file) moves.add(Square(fChar, rank))
            }
            for (r in 1..8) { // Vertikalno
                if (r != rank) moves.add(Square(file, r))
            }
        }
        PieceType.QUEEN -> {
            // Kombinacija topa i lovca (horizontalni, vertikalni, dijagonalni)
            for (fChar in 'a'..'h') {
                if (fChar != file) moves.add(Square(fChar, rank))
            }
            for (r in 1..8) {
                if (r != rank) moves.add(Square(file, r))
            }
            for (i in 1..7) { // Dijagonalno (Gore-desno)
                if (file + i <= 'h' && rank + i <= 8) moves.add(Square(file + i, rank + i))
            }
            for (i in 1..7) { // Dijagonalno (Dole-desno)
                if (file + i <= 'h' && rank - i >= 1) moves.add(Square(file + i, rank - i))
            }
            for (i in 1..7) { // Dijagonalno (Gore-levo)
                if (file - i >= 'a' && rank + i <= 8) moves.add(Square(file - i, rank + i))
            }
            for (i in 1..7) { // Dijagonalno (Dole-levo)
                if (file - i >= 'a' && rank - i >= 1) moves.add(Square(file - i, rank - i))
            }
        }
        PieceType.KING -> {
            // Potezi u svim pravcima za 1 polje
            for (df in -1..1) {
                for (dr in -1..1) {
                    if (df == 0 && dr == 0) continue
                    val newFile = file + df
                    val newRank = rank + dr
                    if (newFile in 'a'..'h' && newRank in 1..8) {
                        moves.add(Square(newFile, newRank))
                    }
                }
            }
        }
        PieceType.NONE -> { /* No moves */ }
    }
    return moves
}

fun PieceType.isSlidingPiece(): Boolean {
    return this == PieceType.QUEEN || this == PieceType.ROOK || this == PieceType.BISHOP
}

fun ChessBoard.isPathClear(from: Square, to: Square): Boolean {
    val (fromFile, fromRank) = from
    val (toFile, toRank) = to

    // Provera horizontalne putanje
    if (fromRank == toRank) {
        val fileStep = if (toFile > fromFile) 1 else -1
        var currentFile = fromFile + fileStep
        while (currentFile != toFile) {
            if (getPiece(Square(currentFile, fromRank)).type != PieceType.NONE) {
                return false
            }
            currentFile += fileStep
        }
    }
    // Provera vertikalne putanje
    else if (fromFile == toFile) {
        val rankStep = if (toRank > fromRank) 1 else -1
        var currentRank = fromRank + rankStep
        while (currentRank != toRank) {
            if (getPiece(Square(fromFile, currentRank)).type != PieceType.NONE) {
                return false
            }
            currentRank += rankStep
        }
    }
    // Provera dijagonalne putanje
    else if (kotlin.math.abs(fromFile.code - toFile.code) == kotlin.math.abs(fromRank - toRank)) {
        val fileStep = if (toFile > fromFile) 1 else -1
        val rankStep = if (toRank > fromRank) 1 else -1
        var currentFile = fromFile + fileStep
        var currentRank = fromRank + rankStep
        while (currentFile.toChar() != toFile || currentRank != toRank) { // Prilagođeno za char file
            if (getPiece(Square(currentFile.toChar(), currentRank)).type != PieceType.NONE) {
                return false
            }
            currentFile += fileStep
            currentRank += rankStep
        }
    }
    return true
}

fun ChessBoard.isSquareAttackedByAnyOpponent(square: Square, myColor: PieceColor): Pair<Square, Piece>? {
    val opponentColor = if (myColor == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
    for (rankIndex in 0 until BOARD_SIZE) {
        for (fileIndex in 0 until BOARD_SIZE) {
            val attackerSquare = Square.fromCoordinates(fileIndex, rankIndex)
            val attackerPiece = this.getPiece(attackerSquare)

            if (attackerPiece.color == opponentColor && attackerPiece.type != PieceType.NONE) {
                val rawMoves = attackerPiece.type.getRawMoves(attackerSquare, attackerPiece.color)
                if (rawMoves.contains(square)) {
                    if (attackerPiece.type.isSlidingPiece()) {
                        if (this.isPathClear(attackerSquare, square)) {
                            return Pair(attackerSquare, attackerPiece)
                        }
                    } else {
                        // Za nelinearne figure (konj, kralj, pešak), putanja nije bitna
                        // Bitno je samo da se nalazi u listi "raw" poteza
                        // Pesački napad je poseban - dijagonalno, ne pravo napred
                        if (attackerPiece.type == PieceType.PAWN) {
                            val pawnAttackMoves = if (attackerPiece.color == PieceColor.WHITE) {
                                listOf(Square(attackerSquare.file + 1, attackerSquare.rank + 1), Square(attackerSquare.file - 1, attackerSquare.rank + 1))
                            } else {
                                listOf(Square(attackerSquare.file + 1, attackerSquare.rank - 1), Square(attackerSquare.file - 1, attackerSquare.rank - 1))
                            }
                            if (pawnAttackMoves.contains(square)) {
                                return Pair(attackerSquare, attackerPiece)
                            }
                        } else {
                            return Pair(attackerSquare, attackerPiece)
                        }
                    }
                }
            }
        }
    }
    return null
}

fun ChessBoard.isSquareAttacked(square: Square, attackingColor: PieceColor): Boolean {
    for (rankIndex in 0 until BOARD_SIZE) {
        for (fileIndex in 0 until BOARD_SIZE) {
            val potentialAttackerSquare = Square.fromCoordinates(fileIndex, rankIndex)
            val potentialAttackerPiece = this.getPiece(potentialAttackerSquare)

            if (potentialAttackerPiece.color == attackingColor && potentialAttackerPiece.type != PieceType.NONE) {
                val attackerMoves = potentialAttackerPiece.type.getRawMoves(potentialAttackerSquare, potentialAttackerPiece.color)

                if (attackerMoves.contains(square)) {
                    if (potentialAttackerPiece.type.isSlidingPiece()) {
                        if (this.isPathClear(potentialAttackerSquare, square)) {
                            return true
                        }
                    } else {
                        // Za nelinearne figure (konj, kralj, pešak)
                        // Pešački napad je specifičan: samo dijagonalno hvatanje
                        if (potentialAttackerPiece.type == PieceType.PAWN) {
                            val pawnAttackMoves = if (potentialAttackerPiece.color == PieceColor.WHITE) {
                                listOf(Square(potentialAttackerSquare.file + 1, potentialAttackerSquare.rank + 1), Square(potentialAttackerSquare.file - 1, potentialAttackerSquare.rank + 1))
                            } else {
                                listOf(Square(potentialAttackerSquare.file + 1, potentialAttackerSquare.rank - 1), Square(potentialAttackerSquare.file - 1, potentialAttackerSquare.rank - 1))
                            }
                            if (pawnAttackMoves.contains(square)) {
                                return true
                            }
                        } else {
                            return true
                        }
                    }
                }
            }
        }
    }
    return false
}

// Data klasa za potez (ovo treba da bude u ChessDefinitions.kt)
data class Move(val from: Square, val to: Square)

// Menadžer zvuka (OVO JE UKLONJENO JER SE DUPLIRA SA SoundManager.kt)
// object SoundManager { ... }