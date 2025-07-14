package com.chess.chesspuzzle

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random // Koristimo kotlin.random.Random kao što je i dogovoreno

// Važno: svi enumi i data klase (PieceType, PieceColor, Square, Piece, ChessBoard, ScoreEntry, Difficulty, Move)
// treba da budu definisani isključivo u ChessDefinitions.kt
// SoundManager treba da bude definisan isključivo u SoundManager.kt
// Ovdje ih samo uvozimo.
import com.chess.chesspuzzle.SoundManager // Uvoz SoundManager objekta

class Game {
    private val _board = MutableStateFlow(ChessBoard.createEmpty())
    val board: StateFlow<ChessBoard> = _board.asStateFlow()

    private val _whiteQueen = MutableStateFlow<Piece?>(null) // Sada je Piece? jer može biti null na početku
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

        // Ažuriraj belu damu i crne figure iz inicijalne table
        // Sada tražimo i Square za belu damu da bismo je mogli lakše referencirati
        val whiteQueenEntry = initialBoard.getPiecesMapFromBoard(PieceColor.WHITE)
            .entries.find { it.value.type == PieceType.QUEEN }
        _whiteQueen.value = whiteQueenEntry?.value

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
        // PROMENJENO: Koristimo move.start i move.end jer je Move definisan u ChessDefinitions.kt sa tim svojstvima
        val fromSquare = move.start
        val toSquare = move.end
        val pieceToMove = _board.value.getPiece(fromSquare)

        if (pieceToMove.color != PieceColor.WHITE) {
            Log.d("Game", "Cannot move a non-white piece.")
            return false
        }

        // Koristimo getLegalMoves iz ove klase, koja će koristiti ekstenzije iz ChessDefinitions.kt
        val legalMoves = getLegalMoves(fromSquare)
        // Legalni potezi se sada sastoje od Square objekata, pa ih direktno upoređujemo
        if (!legalMoves.contains(toSquare)) {
            Log.d("Game", "Illegal move from ${fromSquare} to ${toSquare}.")
            return false
        }

        val capturedPiece = _board.value.getPiece(toSquare)
        // Koristimo movePiece koji smo dodali u ChessBoard u ChessDefinitions.kt
        var newBoard = _board.value.movePiece(move)

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
                // Poziv isSquareAttackedByAnyOpponent iz ChessDefinitions.kt
                val attackingPieceAndSquare = newBoard.isSquareAttackedByAnyOpponent(currentQueenSquare, PieceColor.WHITE)

                if (attackingPieceAndSquare != null) {
                    val (attackerSquare, _) = attackingPieceAndSquare
                    Log.d("Game", "White Queen attacked by ${newBoard.getPiece(attackerSquare).type} at ${attackerSquare}!")
                    // POZIV ZVUKA: false za šah (signal upozorenja)
                    SoundManager.playSound(false)

                    _respawnCount.value++
                    // attackerSquare je Square, pa treba konvertovati u Pair<Int, Int>
                    _lastAttackerPosition.value = Pair(attackerSquare.rankIndex, attackerSquare.fileIndex)

                    val respawnedBoard = respawnQueen(newBoard, currentQueenSquare)
                    _board.value = respawnedBoard
                    _lastAttackerPosition.value = null // Resetuj po završetku respawna
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

        // Koristimo getRawMoves kao ekstenziju PieceType
        val possibleMoves = piece.type.getRawMoves(fromSquare, piece.color)
        val legalMoves = mutableListOf<Square>()

        for (targetSquare in possibleMoves) {
            // Provera da li je potez unutar granica table i nije na isto polje
            if (targetSquare.file !in 'a'..'h' || targetSquare.rank !in 1..8 || targetSquare == fromSquare) {
                continue
            }

            // Provera putanje za klizeće figure
            if (piece.type.isSlidingPiece()) {
                // Koristimo isPathClear kao ekstenziju ChessBoard
                if (!_board.value.isPathClear(fromSquare, targetSquare)) {
                    continue
                }
            }

            val targetPiece = _board.value.getPiece(targetSquare)

            // Ne možeš hvatati svoje figure
            if (targetPiece.color == piece.color) {
                continue
            }

            // Provera šaha nakon poteza
            // Kreiraj privremenu tablu sa izvršenim potezom
            val tempBoardAfterMove = _board.value.movePiece(Move(fromSquare, targetSquare))
            val whiteKingOrQueenSquare = tempBoardAfterMove.getPiecesMapFromBoard(PieceColor.WHITE)
                .entries.find { it.value.type == PieceType.QUEEN || it.value.type == PieceType.KING }?.key // Tražimo kraljicu ili kralja

            // Ako bela figura (kraljica) ostaje u šahu, potez nije legalan
            if (whiteKingOrQueenSquare != null && tempBoardAfterMove.isSquareAttackedByAnyOpponent(whiteKingOrQueenSquare, PieceColor.WHITE) != null) {
                continue
            }

            // Dodatna logika za hvatanje u Modulu 2
            if (_isModul2Mode.value) {
                // U Modulu 2 bela dama treba da hvata crne figure
                if (targetPiece.type == PieceType.NONE || targetPiece.color == PieceColor.BLACK) {
                    legalMoves.add(targetSquare)
                }
            } else {
                // U regularnom modu, svi legalni potezi su dozvoljeni
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
        // Petlja za pronalaženje sigurnog polja za respawn
        while (!placed) {
            newQueenSquare = emptySquares.random(random) // Koristimo .random() iz kotlin.random
            // Proveri da li je novo polje napadnuto od CRNIH figura
            if (!newBoard.isSquareAttacked(newQueenSquare, PieceColor.BLACK)) { // Koristimo isSquareAttacked iz ChessDefinitions.kt
                newBoard = newBoard.setPiece(Piece(PieceType.QUEEN, PieceColor.WHITE), newQueenSquare)
                _whiteQueen.value = Piece(PieceType.QUEEN, PieceColor.WHITE) // Ažuriraj StateFlow
                placed = true
                Log.d("Game", "Queen respawned at ${newQueenSquare}")
            } else {
                Log.d("Game", "Attempted respawn at ${newQueenSquare} but it's attacked. Trying again.")
                emptySquares.remove(newQueenSquare) // Ukloni napadnuto polje iz liste kandidata
                if (emptySquares.isEmpty()) {
                    Log.e("Game", "All empty squares are attacked after respawn, forcing placement.")
                    // Fallback: ako su sva prazna polja napadnuta, stavi je bilo gde
                    // Uzmi prvo prazno polje, ili jednostavno forsiraj na random ako je sve napadnuto
                    newQueenSquare = Square.ALL_SQUARES.filter { currentBoard.getPiece(it).type == PieceType.NONE }.firstOrNull() ?: Square('a',1) // Uzmi prvo prazno ili a1 kao fallback
                    newBoard = newBoard.setPiece(Piece(PieceType.QUEEN, PieceColor.WHITE), newQueenSquare)
                    _whiteQueen.value = Piece(PieceType.QUEEN, PieceColor.WHITE) // Ažuriraj StateFlow
                    placed = true
                }
            }
        }
        return newBoard
    }
}