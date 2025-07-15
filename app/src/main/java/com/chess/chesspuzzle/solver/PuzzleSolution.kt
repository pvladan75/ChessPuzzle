import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.Move

// com.chess.chesspuzzle.solver
data class PuzzleSolution(
    val isSolved: Boolean,
    val moves: List<Move>, // Lista poteza od početka do rešenja
    val finalBoard: ChessBoard,
    val message: String = "" // Dodatna poruka (npr. "Zagonetka rešena", "Nije pronađeno rešenje")
)