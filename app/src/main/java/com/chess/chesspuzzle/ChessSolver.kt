package com.chess.chesspuzzle

import android.content.Context
import android.util.Log
import java.util.ArrayDeque
import java.util.Deque
import kotlinx.coroutines.*

object ChessSolver {

    private const val TAG = "ChessSolver"

    /**
     * Pokušava da reši zagonetku hvatanja svih crnih figura.
     * Broj poteza koje solver sme da ispita (dubina pretrage) dinamički se određuje
     * kao ukupan broj crnih figura na početnoj poziciji.
     *
     * @param fenPosition Početna FEN pozicija zagonetke.
     * @param timeoutMillis Maksimalno vreme u milisekundama za rešavanje zagonetke.
     * @return Lista objekata klase Move (koju ćemo definisati), ili null ako rešenje nije pronađeno.
     */
    suspend fun solveCapturePuzzle(
        fenPosition: String,
        timeoutMillis: Long = 30000 // 30 sekundi timeout
    ): List<MoveData>? = withContext(Dispatchers.Default) {
        val queue: Deque<Pair<String, List<MoveData>>> = ArrayDeque()
        val visitedStates = mutableSetOf<String>()

        val initialBoard = ChessCore.parseFenToBoard(fenPosition)
        val maxMoves = initialBoard.getPiecesMapFromBoard(PieceColor.BLACK).size

        // Specijalni slučaj: Ako nema crnih figura, rešenje je prazna lista poteza.
        if (maxMoves == 0) {
            Log.d(TAG, "Nema crnih figura na početnoj poziciji. Rešenje: Prazna lista.")
            return@withContext emptyList()
        }

        val initialStateKey = getSimplifiedFen(initialBoard)
        val initialStateTuple = Pair(initialBoard.toFEN(), emptyList<MoveData>())

        queue.add(initialStateTuple)
        visitedStates.add(initialStateKey)

        Log.d(TAG, "Pokrećem rešavanje zagonetke za FEN: $fenPosition")
        Log.d(TAG, "Cilj: Uhvatiti SVE crne figure (bilo kojom belom figurom).")
        Log.d(TAG, "Maksimalna dubina pretrage (maxMoves) postavljena na: $maxMoves (na osnovu broja crnih figura)")
        Log.d(TAG, "----------------------------------------------------------------------")

        var nodesExplored = 0
        val startTime = System.currentTimeMillis()

        while (queue.isNotEmpty() && !this.coroutineContext.job.isCancelled) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                Log.w(TAG, "Vreme rešavanja zagonetke isteklo (${timeoutMillis}ms) za FEN: $fenPosition")
                return@withContext null
            }

            val (currentFen, currentPath) = queue.pop()
            val currentBoard = ChessCore.parseFenToBoard(currentFen)
            nodesExplored++

            val blackPiecesCount = currentBoard.getPiecesMapFromBoard(PieceColor.BLACK).size

            if (blackPiecesCount == 0) {
                Log.d(TAG, "Rešenje pronađeno nakon $nodesExplored istraženih čvorova!")
                currentPath.forEachIndexed { i, move ->
                    Log.d(TAG, "Korak ${i + 1}: ${move.uci()}")
                }
                return@withContext currentPath
            }

            if (currentPath.size >= maxMoves) {
                continue // Preskoči proširenje ovog čvora ako je putanja predugačka
            }

            val candidateMoves = mutableListOf<MoveData>()

            val whitePieces = currentBoard.getPiecesMapFromBoard(PieceColor.WHITE)
            for ((startSquare, piece) in whitePieces) {
                val possibleDestinations = ChessCore.generateAllPossibleMovesForPiece(currentBoard, startSquare, piece)

                for (targetSquare in possibleDestinations) {
                    val targetPiece = currentBoard.getPiece(targetSquare)

                    // 1. Proveri da li je ciljno polje zauzeto CRNOM figurom (obavezno hvatanje)
                    if (targetPiece.type != PieceType.NONE && targetPiece.color == PieceColor.BLACK) {
                        // 2. Proveri da li je putanja čista (samo za klizajuće figure, vitezovi skaču)
                        val isPathClear = ChessCore.isPathClear(currentBoard, startSquare, targetSquare, piece.type)
                        if (isPathClear) {
                            candidateMoves.add(MoveData(startSquare, targetSquare))
                        } else {
                            // Log.d(TAG, "Putanja nije čista za ${piece.type} sa ${startSquare} do ${targetSquare}") // Debug log
                        }
                    } else {
                        // Log.d(TAG, "Meta nije crna figura za ${piece.type} sa ${startSquare} do ${targetSquare}") // Debug log
                    }
                }
            }

            // Nema potrebe za sortiranjem, BFS pronalazi najkraći put
            val sortedMoves = candidateMoves

            if (currentPath.size < maxMoves && sortedMoves.isNotEmpty()) { // Loguj poteze ako putanja nije predugačka
                Log.d(TAG, "  Potezi iz ${currentBoard.toFEN()} (path len: ${currentPath.size}):")
                for (m in sortedMoves) {
                    Log.d(TAG, "    ${m.uci()}")
                }
            }

            for (moveData in sortedMoves) {
                val nextBoard = ChessCore.simulateCaptureMove(currentBoard, moveData)

                val newFen = nextBoard.toFEN()
                val newSimplifiedFen = getSimplifiedFen(nextBoard)

                val newPath = currentPath + moveData

                if (newSimplifiedFen !in visitedStates) {
                    queue.add(Pair(newFen, newPath))
                    visitedStates.add(newSimplifiedFen)
                }
            }
        }

        if (this.coroutineContext.job.isCancelled) {
            Log.d(TAG, "Rešavanje zagonetke otkazano za FEN: $fenPosition")
        } else {
            Log.d(TAG, "Nema rešenja pronađenog za datu zagonetku nakon $nodesExplored čvorova. FEN: $fenPosition")
        }
        return@withContext null
    }

    // Pomoćna data klasa za reprezentaciju poteza
    data class MoveData(val fromSquare: Square, val toSquare: Square) {
        fun uci(): String {
            return "${fromSquare.file}${fromSquare.rank}${toSquare.file}${toSquare.rank}"
        }
    }

    private fun getSimplifiedFen(board: ChessBoard): String {
        return board.toFEN().split(' ')[0]
    }

    // Funkcija za testiranje iz Activity-ja/ViewModel-a
    fun testSolver(context: Context, fenToTest: String) {
        val scope = CoroutineScope(Dispatchers.Main)

        scope.launch {
            Log.d(TAG, "\n\n*** Rešavanje test zagonetke ***")
            val solutionPath = solveCapturePuzzle(fenToTest)

            if (solutionPath != null) {
                Log.d(TAG, "\n--- Vizualizacija rešenja za test zagonetku ---")
                var board = ChessCore.parseFenToBoard(fenToTest)
                Log.d(TAG, "Početna tabla:\n${board.toFEN()}")

                solutionPath.forEachIndexed { j, move ->
                    board = ChessCore.simulateCaptureMove(board, move)
                    Log.d(TAG, "\nPosle poteza ${j + 1} (${move.uci()}):")
                    Log.d(TAG, board.toFEN())
                }
            } else {
                Log.d(TAG, "\nNije pronađeno rešenje za test zagonetku.")
            }
        }
    }
}