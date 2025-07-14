package com.chess.chesspuzzle.modul2

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.random.Random

// --- CORRECTED IMPORTS ---
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor // <-- CORRECTED IMPORT!
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.BOARD_SIZE // Assuming BOARD_SIZE is also in ChessDefinitions.kt

// IZMENA: Move klasa definisana unutar ovog fajla (može biti i top-level, ali za sada ovde)
data class Move(val startSquare: Square, val endSquare: Square) {
    val startRow: Int get() = startSquare.rankIndex
    val startCol: Int get() = startSquare.fileIndex
    val endRow: Int get() = endSquare.rankIndex
    val endCol: Int get() = endSquare.fileIndex
}

// IZMENA: Ekstenziona funkcija za PieceType za dobijanje sirovih poteza
// Ovo je premešteno iz Piece klase jer Piece nema row/col
fun PieceType.getRawMoves(fromSquare: Square, color: PieceColor): List<Square> {
    val moves = mutableListOf<Square>()
    when (this) {
        PieceType.QUEEN -> {
            moves.addAll(getStraightMoves(fromSquare))
            moves.addAll(getDiagonalMoves(fromSquare))
        }
        PieceType.ROOK -> moves.addAll(getStraightMoves(fromSquare))
        PieceType.BISHOP -> moves.addAll(getDiagonalMoves(fromSquare))
        PieceType.KNIGHT -> {
            val knightDeltas = listOf(
                Pair(1, 2), Pair(1, -2), Pair(-1, 2), Pair(-1, -2),
                Pair(2, 1), Pair(2, -1), Pair(-2, 1), Pair(-2, -1)
            )
            for ((dx, dy) in knightDeltas) {
                val toSquare = fromSquare.move(dx, dy)
                if (toSquare.isValid()) {
                    moves.add(toSquare)
                }
            }
        }
        PieceType.PAWN -> {
            val direction = if (color == PieceColor.WHITE) 1 else -1 // +1 for white (up), -1 for black (down)
            // Pawn attacks are diagonal
            val attack1 = fromSquare.move(1, direction)
            val attack2 = fromSquare.move(-1, direction)

            if (attack1.isValid()) moves.add(attack1)
            if (attack2.isValid()) moves.add(attack2)

            // For straight pawn moves, you'd add:
            // val forwardOne = fromSquare.move(0, direction)
            // if (forwardOne.isValid() && board.getPiece(forwardOne).type == PieceType.NONE) moves.add(forwardOne)
            // For initial double pawn move:
            // val startRank = if (color == PieceColor.WHITE) 1 else 6 // Ranks 1-indexed, so 2nd and 7th rank (index 1 and 6)
            // if (fromSquare.rankIndex == startRank) {
            //     val forwardTwo = fromSquare.move(0, direction * 2)
            //     if (forwardTwo.isValid() && board.getPiece(forwardTwo).type == PieceType.NONE && board.getPiece(forwardOne).type == PieceType.NONE) moves.add(forwardTwo)
            // }
            // Note: Pawn capture logic is usually checked against existing pieces on the target square,
            // not just raw moves. This `getRawMoves` implies squares a piece *could* go to.
        }
        PieceType.KING -> {
            val kingDeltas = listOf(
                Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0),
                Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1)
            )
            for ((dx, dy) in kingDeltas) {
                val toSquare = fromSquare.move(dx, dy)
                if (toSquare.isValid()) {
                    moves.add(toSquare)
                }
            }
        }
        PieceType.NONE -> { /* No moves for NONE type */ }
    }
    return moves
}

// Pomoćne funkcije za klizne figure kao ekstenzije na Square
fun Square.move(dx: Int, dy: Int): Square {
    // Need to correctly map dx/dy to file/rank changes for Square
    // fileIndex is 0-7, rankIndex is 0-7
    val newFileIndex = fileIndex + dx
    val newRankIndex = rankIndex + dy
    return Square.fromCoordinates(newFileIndex, newRankIndex)
}

fun Square.isValid(): Boolean {
    return file in 'a'..'h' && rank in 1..8
}

// Pomoćne funkcije za klizne figure unutar ovog fajla (kao top-level)
private fun getStraightMoves(fromSquare: Square): List<Square> {
    val moves = mutableListOf<Square>()
    // Up
    for (i in 1 until BOARD_SIZE) fromSquare.move(0, i).let { if (it.isValid()) moves.add(it) }
    // Down
    for (i in 1 until BOARD_SIZE) fromSquare.move(0, -i).let { if (it.isValid()) moves.add(it) }
    // Right
    for (i in 1 until BOARD_SIZE) fromSquare.move(i, 0).let { if (it.isValid()) moves.add(it) }
    // Left
    for (i in 1 until BOARD_SIZE) fromSquare.move(-i, 0).let { if (it.isValid()) moves.add(it) }
    return moves
}

private fun getDiagonalMoves(fromSquare: Square): List<Square> {
    val moves = mutableListOf<Square>()
    // Up-Right
    for (i in 1 until BOARD_SIZE) fromSquare.move(i, i).let { if (it.isValid()) moves.add(it) }
    // Up-Left
    for (i in 1 until BOARD_SIZE) fromSquare.move(-i, i).let { if (it.isValid()) moves.add(it) }
    // Down-Right
    for (i in 1 until BOARD_SIZE) fromSquare.move(i, -i).let { if (it.isValid()) moves.add(it) }
    // Down-Left
    for (i in 1 until BOARD_SIZE) fromSquare.move(-i, -i).let { if (it.isValid()) moves.add(it) }
    return moves
}

// Provera da li je figura "klizna" (Dama, Top, Lovac)
fun PieceType.isSlidingPiece(): Boolean {
    return this == PieceType.QUEEN || this == PieceType.ROOK || this == PieceType.BISHOP
}

// Ekstenziona funkcija za Board za proveru da li je putanja čista
fun ChessBoard.isPathClear(fromSquare: Square, toSquare: Square): Boolean {
    val deltaFile = toSquare.fileIndex - fromSquare.fileIndex
    val deltaRank = toSquare.rankIndex - fromSquare.rankIndex

    // Check for straight or diagonal moves only
    if (deltaFile != 0 && deltaRank != 0 && Math.abs(deltaFile) != Math.abs(deltaRank)) {
        return false // Not a straight or diagonal path
    }

    val stepFile = if (deltaFile > 0) 1 else if (deltaFile < 0) -1 else 0
    val stepRank = if (deltaRank > 0) 1 else if (deltaRank < 0) -1 else 0

    var currentFileIndex = fromSquare.fileIndex + stepFile
    var currentRankIndex = fromSquare.rankIndex + stepRank

    while (Square.fromCoordinates(currentFileIndex, currentRankIndex) != toSquare) {
        val currentSquare = Square.fromCoordinates(currentFileIndex, currentRankIndex)
        if (getPiece(currentSquare).type != PieceType.NONE) { // Check if there's any piece blocking
            return false // Found a piece on the path
        }
        currentFileIndex += stepFile
        currentRankIndex += stepRank
    }
    return true // Path is clear
}

// Ekstenziona funkcija za Board za proveru napada na polje
fun ChessBoard.isSquareAttacked(square: Square, attackingColor: PieceColor): Boolean {
    // Iterate through all pieces on the board
    for ((pieceSquare, piece) in pieces) {
        if (piece.color == attackingColor) {
            // Get potential moves for this piece
            val rawMoves = piece.type.getRawMoves(pieceSquare, piece.color)

            // Check if any of these raw moves target the square in question
            if (rawMoves.contains(square)) {
                if (piece.type.isSlidingPiece()) {
                    // For sliding pieces, also check if the path is clear
                    if (this.isPathClear(pieceSquare, square)) {
                        return true
                    }
                } else {
                    // For non-sliding pieces (Knight, Pawn, King), raw move is sufficient (assuming pawn logic is for attacks)
                    return true
                }
            }
        }
    }
    return false
}


// Glavna Game klasa
class Game {
    companion object {
        private const val TAG = "ChessGameModule2"
        // Konstante za bodovanje
        const val BASE_POINTS_PER_PUZZLE = 100
        const val PENALTY_PER_RESPAWN = 20
        const val PERFECT_GAME_BONUS = 50
        const val TIME_BONUS_FACTOR = 0.05
        const val MAX_TIME_BONUS_MS = 25000
        const val POINTS_PER_BLACK_PIECE_FACTOR = 25

        const val SESSION_PUZZLE_COUNT = 5
    }

    var board: ChessBoard by mutableStateOf(ChessBoard.createEmpty()) // Koristi ChessBoard
        private set

    var whiteQueen: Piece? by mutableStateOf(null) // White Queen je Piece
        private set

    var blackPieces: List<Piece> by mutableStateOf(emptyList()) // Lista Piece objekata
        private set

    var moveCount: Int by mutableStateOf(0)
        private set

    var respawnCount: Int by mutableStateOf(0)

    var puzzleSolved: Boolean by mutableStateOf(false)

    var puzzleStartTime: Long = 0L
    var puzzleTime: Long by mutableStateOf(0L)
    var currentPuzzleScore: Int = 0
    private var initialBlackPiecesCount: Int = 0

    var lastAttackerPosition: Pair<Int, Int>? by mutableStateOf(null) // Row, Col
        private set

    private var history: MutableList<BoardState> = mutableListOf()
    private val random: Random = Random.Default

    // IZMENA: BoardState sada čuva celu mapu figura
    data class BoardState(
        val piecesMap: Map<Square, Piece>, // Sačuvaj kompletnu mapu figura
        val currentMoveCount: Int,
        val currentRespawnCount: Int,
        val currentPuzzleTime: Long
    )

    // IZMENA: Promenjen potpis initializeGame da prima ChessBoard
    fun initializeGame(initialBoard: ChessBoard) {
        Log.d(TAG, "Initializing game with given board.")
        currentPuzzleScore = 0

        board = initialBoard.copy() // Start with a deep copy of the provided initial board

        // Populate whiteQueen and blackPieces from the initial board
        whiteQueen = board.getPiecesMapFromBoard(PieceColor.WHITE).entries.find { it.value.type == PieceType.QUEEN }?.value
        blackPieces = board.getPiecesMapFromBoard(PieceColor.BLACK).values.toList()

        this.initialBlackPiecesCount = blackPieces.size

        moveCount = 0
        respawnCount = 0
        puzzleSolved = false
        puzzleStartTime = System.currentTimeMillis()
        puzzleTime = 0L

        lastAttackerPosition = null

        history.clear()
        saveState()
        Log.d(TAG, "Game initialized. Board state saved. Remaining black pieces: ${blackPieces.size}")
    }

    fun isGameOver(): Boolean {
        return blackPieces.isEmpty() && puzzleSolved
    }

    suspend fun makeMove(move: Move): Boolean {
        if (puzzleSolved) {
            Log.d(TAG, "makeMove: Puzzle already solved, no more moves allowed.")
            return false
        }

        if (lastAttackerPosition != null) {
            Log.d(TAG, "makeMove: Queen was just captured, waiting for UI to process. No new moves allowed yet.")
            return false
        }

        val currentQueen = whiteQueen ?: run {
            Log.e(TAG, "makeMove: White Queen is null, cannot make move.")
            return false
        }
        val currentQueenSquare = board.getPiecesMapFromBoard(PieceColor.WHITE).entries.find { it.value == currentQueen }?.key ?: run {
            Log.e(TAG, "makeMove: White Queen not found on board map, cannot make move.")
            return false
        }

        Log.d(TAG, "makeMove: Attempting move from (${currentQueenSquare.file},${currentQueenSquare.rank}) to (${move.endSquare.file},${move.endSquare.rank}).")

        // IZMENA: Provera da li je potez legalan za damu
        // Koristimo ekstenziju getRawMoves i proveru putanje
        if (!currentQueen.type.getRawMoves(currentQueenSquare, currentQueen.color).contains(move.endSquare) || !board.isPathClear(currentQueenSquare, move.endSquare)) {
            Log.d(TAG, "makeMove: Queen cannot legally move to target position (${move.endSquare.file},${move.endSquare.rank}) by chess rules or path is blocked.")
            return false
        }

        saveState()
        Log.d(TAG, "makeMove: Game state saved before move execution.")

        val pieceAtEndBeforeMove = board.getPiece(move.endSquare)
        Log.d(TAG, "makeMove: Piece at target (${move.endSquare.file},${move.endSquare.rank}) before move: ${pieceAtEndBeforeMove.type} (${pieceAtEndBeforeMove.color}).")

        // IZMENA: Sada se potez izvršava direktno na board objektu
        var updatedBoard = board.copy() // Ploča se kopira
        updatedBoard = updatedBoard.removePiece(currentQueenSquare) // Ukloni damu sa stare pozicije

        if (pieceAtEndBeforeMove.color == PieceColor.BLACK) { // CORRECTED Piece.PieceColor to PieceColor
            Log.d(TAG, "makeMove: Black piece ${pieceAtEndBeforeMove.type} at (${move.endSquare.file},${move.endSquare.rank}) was captured.")
            updatedBoard = updatedBoard.removePiece(move.endSquare) // Ukloni pojedenu crnu figuru sa table
            // Ažuriraj blackPieces listu filterom (ukloni pojedenu figuru iz liste)
            blackPieces = blackPieces.filter { it != pieceAtEndBeforeMove }
            Log.d(TAG, "makeMove: Black piece removed from list. Remaining black pieces: ${blackPieces.size}")
        }

        // Postavi damu na novu poziciju
        updatedBoard = updatedBoard.setPiece(currentQueen, move.endSquare)

        // Proveri da li je kraljica napadnuta na novoj poziciji
        if (updatedBoard.isSquareAttacked(move.endSquare, PieceColor.BLACK)) { // CORRECTED Piece.PieceColor to PieceColor
            Log.d(TAG, "makeMove: Queen moved to an ATTACKED square (${move.endSquare.file},${move.endSquare.rank}). Queen is CAPTURED!")
            handleQueenCapture(currentQueen, move.endSquare, pieceAtEndBeforeMove.takeIf { it.type != PieceType.NONE } )
        } else {
            Log.d(TAG, "makeMove: Queen moved to a SAFE square (${move.endSquare.file},${move.endSquare.rank}).")
            board = updatedBoard // Ažuriraj board state
        }

        moveCount++
        Log.d(TAG, "makeMove: Move processed. New moveCount: $moveCount. Remaining black pieces: ${blackPieces.size}")

        if (blackPieces.isEmpty()) {
            puzzleSolved = true
            puzzleTime = System.currentTimeMillis() - puzzleStartTime
            Log.d(TAG, "makeMove: Game Over! All black pieces captured. Time taken: $puzzleTime ms.")
            currentPuzzleScore = calculatePuzzleScore(puzzleTime, respawnCount)
            Log.d(TAG, "makeMove: Puzzle solved! Score: $currentPuzzleScore")
        }
        return true
    }

    fun calculatePuzzleScore(timeTakenMs: Long, respawns: Int): Int {
        var score = BASE_POINTS_PER_PUZZLE
        score -= (respawns * PENALTY_PER_RESPAWN)
        val effectiveTimeBonusMs = (MAX_TIME_BONUS_MS - timeTakenMs).coerceAtLeast(0L)
        val timeBonus = (effectiveTimeBonusMs * TIME_BONUS_FACTOR).toInt()
        score += timeBonus
        Log.d(TAG, "calculatePuzzleScore: Time bonus applied: $timeBonus (from $effectiveTimeBonusMs ms)")

        if (respawns == 0) {
            score += PERFECT_GAME_BONUS
            Log.d(TAG, "calculatePuzzleScore: Perfect game bonus applied: $PERFECT_GAME_BONUS")
        }

        score += (initialBlackPiecesCount * POINTS_PER_BLACK_PIECE_FACTOR)
        Log.d(TAG, "calculatePuzzleScore: Bonus for initial black pieces applied: ${initialBlackPiecesCount * POINTS_PER_BLACK_PIECE_FACTOR} (from $initialBlackPiecesCount pieces)")

        return score.coerceAtLeast(0)
    }

    fun clearLastAttackerPosition() {
        lastAttackerPosition = null
        Log.d(TAG, "clearLastAttackerPosition: lastAttackerPosition reset to null.")
    }

    suspend fun handleQueenCapture(queenPiece: Piece, queenCapturedSquare: Square, capturedBlackPiece: Piece?) {
        Log.d(TAG, "handleQueenCapture: Queen has been captured at (${queenCapturedSquare.file}, ${queenCapturedSquare.rank}). Finding attacking black piece and new safe position for queen.")

        respawnCount++

        // Ukloni pojedenu crnu figuru ako je bilo (iz liste i sa table)
        if (capturedBlackPiece != null && capturedBlackPiece.color == PieceColor.BLACK) { // CORRECTED Piece.PieceColor to PieceColor
            // Pronađi i ukloni iz blackPieces liste
            blackPieces = blackPieces.filter { it != capturedBlackPiece }
            // Ukloni sa table
            board = board.removePiece(queenCapturedSquare) // Uklanja se figura sa polja gde je dama uhvaćena
            Log.d(TAG, "handleQueenCapture: Captured black piece ${capturedBlackPiece.type} removed from list and board.")
        }

        // Pronađi sve crne figure koje napadaju polje gde je dama bila
        val attackingBlackPiecesInPlay = mutableListOf<Pair<Square, Piece>>() // Pair<Square, Piece> za napadača
        // Iteriraj kroz SVE preostale crne figure (sada su u `blackPieces` listi, ali moramo znati njihove pozicije sa table)
        for ((pieceSquare, piece) in board.getPiecesMapFromBoard(PieceColor.BLACK)) { // CORRECTED Piece.PieceColor to PieceColor
            if (piece.type.getRawMoves(pieceSquare, piece.color).contains(queenCapturedSquare) && board.isPathClear(pieceSquare, queenCapturedSquare)) {
                attackingBlackPiecesInPlay.add(pieceSquare to piece)
            }
        }

        var selectedAttackerPair: Pair<Square, Piece>? = null
        if (attackingBlackPiecesInPlay.isNotEmpty()) {
            selectedAttackerPair = attackingBlackPiecesInPlay.random(random)
            val (attackerSquare, selectedAttackerPiece) = selectedAttackerPair
            Log.d(TAG, "handleQueenCapture: Selected attacker: ${selectedAttackerPiece.type} at (${attackerSquare.file},${attackerSquare.rank})")

            lastAttackerPosition = attackerSquare.rankIndex to attackerSquare.fileIndex // Row, Col
            Log.d(TAG, "handleQueenCapture: lastAttackerPosition set to $lastAttackerPosition")

        } else {
            Log.w(TAG, "handleQueenCapture: No attacking black pieces found for queen capture at (${queenCapturedSquare.file}, ${queenCapturedSquare.rank}). This should not happen if queen was captured.")
            lastAttackerPosition = null
        }

        // 3. Pronađi sve sigurne pozicije za novu damu
        val safePositions = mutableListOf<Pair<Int, Int>>()
        val currentBoardState = board.copy() // Koristi ažuriranu tablu (nakon eventualnog uklanjanja pojedene figure)

        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val currentSquare = Square.fromCoordinates(c, r)
                // Proveri da li je polje prazno i sigurno (nije napadnuto od crnih figura)
                if (currentBoardState.getPiece(currentSquare).type == PieceType.NONE && !currentBoardState.isSquareAttacked(currentSquare, PieceColor.BLACK)) { // CORRECTED Piece.PieceColor to PieceColor
                    safePositions.add(r to c)
                }
            }
        }

        // 4. Postavi novu damu na nasumično odabrano sigurno polje
        if (safePositions.isNotEmpty()) {
            val randomPos = safePositions.random(random)
            val newQueenSquare = Square.fromCoordinates(randomPos.second, randomPos.first) // col, row

            whiteQueen?.let { queen ->
                // Ukloni damu sa stare pozicije pre nego što je respawnuješ
                // Moraš pronaći trenutnu poziciju bele dame na tabli
                val oldQueenSquare = board.getPiecesMapFromBoard(PieceColor.WHITE).entries.find { it.value == queen }?.key ?: queenCapturedSquare // CORRECTED Piece.PieceColor to PieceColor
                var updatedBoard = board.removePiece(oldQueenSquare)

                // Sada postavi istu instancu dame na novu poziciju
                updatedBoard = updatedBoard.setPiece(queen, newQueenSquare)
                board = updatedBoard
                Log.d(TAG, "handleQueenCapture: Queen respawned at (${newQueenSquare.file},${newQueenSquare.rank}).")
            } ?: run {
                Log.e(TAG, "handleQueenCapture: whiteQueen is null when trying to respawn, creating new instance.")
                whiteQueen = Piece(PieceType.QUEEN, PieceColor.WHITE) // No row/col in constructor
                whiteQueen?.let { board = board.setPiece(it, newQueenSquare) }
            }
            Log.d(TAG, "handleQueenCapture: Board state updated after Queen respawn.")

        } else {
            Log.e(TAG, "handleQueenCapture: No safe positions found for Queen respawn. Game might be stuck or over!")
        }

        delay(800L)
        clearLastAttackerPosition()
        Log.d(TAG, "handleQueenCapture: lastAttackerPosition cleared after delay.")
    }

    fun undoMove(): Boolean {
        if (history.size <= 1) { // Uvek mora biti barem početno stanje
            Log.d(TAG, "undoMove: No moves to undo. History size: ${history.size}")
            return false
        }
        history.removeAt(history.size - 1) // Ukloni trenutno stanje
        val prevState = history.last() // Uzmi prethodno stanje

        Log.d(TAG, "undoMove: Undoing to moveCount: ${prevState.currentMoveCount}, respawnCount: ${prevState.currentRespawnCount}")

        // IZMENA: Rekonstruiši board iz sačuvane mape figura
        board = ChessBoard(prevState.piecesMap.mapValues { it.value.copy() }) // Duboka kopija figura

        // IZMENA: Ažuriraj whiteQueen i blackPieces liste na osnovu vraćenog boarda
        whiteQueen = board.getPiecesMapFromBoard(PieceColor.WHITE).entries.find { it.value.type == PieceType.QUEEN }?.value // CORRECTED Piece.PieceColor to PieceColor
        blackPieces = board.getPiecesMapFromBoard(PieceColor.BLACK).values.toList() // CORRECTED Piece.PieceColor to PieceColor


        moveCount = prevState.currentMoveCount
        respawnCount = prevState.currentRespawnCount
        puzzleTime = prevState.currentPuzzleTime
        lastAttackerPosition = null // Resetuj napadača pri undo

        if (blackPieces.isNotEmpty()) { // Ako ima crnih figura, znači da nije rešena
            puzzleSolved = false
        }
        if (!puzzleSolved) {
            puzzleStartTime = System.currentTimeMillis() - puzzleTime
        }

        Log.d(TAG, "undoMove: State restored. Restored black pieces: ${blackPieces.size}")
        return true
    }

    private fun saveState() {
        // IZMENA: Sačuvaj duboku kopiju mape figura
        history.add(BoardState(
            board.pieces.mapValues { it.value.copy() }, // Pravi duboku kopiju svake Piece
            moveCount,
            respawnCount,
            if (puzzleSolved) puzzleTime else System.currentTimeMillis() - puzzleStartTime
        ))
        Log.d(TAG, "saveState: Current state saved. Total pieces on board: ${board.pieces.size}, respawnCount: $respawnCount")
    }
}