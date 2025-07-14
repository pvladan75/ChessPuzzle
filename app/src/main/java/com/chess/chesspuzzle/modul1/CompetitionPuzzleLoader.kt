package com.chess.chesspuzzle.modul1

import android.content.Context
import android.util.Log
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.PieceTypeDeserializer
import com.google.gson.GsonBuilder // DODATO ZA REGISTRACIJU CUSTOM DESERIALIZERA
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import kotlin.random.Random

object CompetitionPuzzleLoader {

    private const val TAG = "CompetitionPuzzleLoader"
    private var allLoadedPuzzles: List<ChessProblem> = emptyList()

    private fun loadAllPuzzlesIfNecessary(context: Context) {
        if (allLoadedPuzzles.isEmpty()) {
            Log.d(TAG, "$TAG: Loading puzzles from puzzles.json into cache...")

            // Kreiraj GsonBuilder i registruj custom deserializer za PieceType
            val gson = GsonBuilder()
                .registerTypeAdapter(PieceType::class.java, PieceTypeDeserializer())
                .create()

            val assetManager = context.assets
            var reader: InputStreamReader? = null
            try {
                reader = InputStreamReader(assetManager.open("puzzles.json"))
                val puzzleListType = object : TypeToken<List<ChessProblem>>() {}.type
                allLoadedPuzzles = gson.fromJson(reader, puzzleListType)
                Log.d(TAG, "$TAG: Loaded ${allLoadedPuzzles.size} puzzles into cache.")
            } catch (e: Exception) {
                Log.e(TAG, "$TAG: Error loading puzzles from JSON: ${e.message}", e)
                allLoadedPuzzles = emptyList()
            } finally {
                reader?.close()
            }
        } else {
            Log.d(TAG, "$TAG: Puzzles already loaded in cache (${allLoadedPuzzles.size} puzzles).")
        }
    }

    /**
     * Učitava zagonetku iz JSON fajla za takmičarski mod (Lako).
     * Filtrira zagonetke po "Lako" težini i solutionLength od 5 do 7.
     */
    fun loadEasyPuzzleFromJson(context: Context): ChessBoard {
        loadAllPuzzlesIfNecessary(context)
        Log.d(TAG, "$TAG: Attempting to load EASY puzzle from JSON. Solution length: 5-7.")

        val filteredPuzzles = allLoadedPuzzles.filter { puzzle ->
            puzzle.difficulty.equals("Lako", ignoreCase = true) &&
                    puzzle.solutionLength in 5..7
        }

        val randomPuzzle = filteredPuzzles.randomOrNull(Random.Default)

        return if (randomPuzzle != null) {
            Log.d(TAG, "$TAG: Loaded EASY puzzle ID: ${randomPuzzle.id}, FEN: ${randomPuzzle.fen}, Sol.Len: ${randomPuzzle.solutionLength}")
            ChessBoard.parseFenToBoard(randomPuzzle.fen)
        } else {
            Log.e(TAG, "$TAG: No EASY puzzles found matching criteria (solution length 5-7) in JSON. Returning empty board.")
            ChessBoard.createEmpty()
        }
    }

    /**
     * Učitava zagonetku iz JSON fajla za takmičarski mod (Srednje).
     * Filtrira zagonetke po "Srednje" težini i solutionLength od 8 do 10.
     */
    fun loadMediumPuzzleFromJson(context: Context): ChessBoard {
        loadAllPuzzlesIfNecessary(context)
        Log.d(TAG, "$TAG: Attempting to load MEDIUM puzzle from JSON. Solution length: 8-10.")

        val filteredPuzzles = allLoadedPuzzles.filter { puzzle ->
            puzzle.difficulty.equals("Srednje", ignoreCase = true) &&
                    puzzle.solutionLength in 8..10
        }

        val randomPuzzle = filteredPuzzles.randomOrNull(Random.Default)

        return if (randomPuzzle != null) {
            Log.d(TAG, "$TAG: Loaded MEDIUM puzzle ID: ${randomPuzzle.id}, FEN: ${randomPuzzle.fen}, Sol.Len: ${randomPuzzle.solutionLength}")
            ChessBoard.parseFenToBoard(randomPuzzle.fen)
        } else {
            Log.e(TAG, "$TAG: No MEDIUM puzzles found matching criteria (solution length 8-10) in JSON. Returning empty board.")
            ChessBoard.createEmpty()
        }
    }

    /**
     * Učitava zagonetku iz JSON fajla za takmičarski mod (Teško).
     * Filtrira zagonetke po "Teško" težini i solutionLength > 10,
     * i **isključuje pozicije sa belom damom**.
     */
    fun loadHardPuzzleFromJson(context: Context): ChessBoard {
        loadAllPuzzlesIfNecessary(context)
        Log.d(TAG, "$TAG: Attempting to load HARD puzzle from JSON. Solution length: >10. Excluding puzzles with White Queen.")

        val filteredPuzzles = allLoadedPuzzles.filter { puzzle ->
            puzzle.difficulty.equals("Teško", ignoreCase = true) &&
                    puzzle.solutionLength > 10 &&
                    // Pozivamo hasWhiteQueen() direktno na ChessProblem objektu
                    !puzzle.hasWhiteQueen()
        }

        val randomPuzzle = filteredPuzzles.randomOrNull(Random.Default)

        return if (randomPuzzle != null) {
            Log.d(TAG, "$TAG: Loaded HARD puzzle ID: ${randomPuzzle.id}, FEN: ${randomPuzzle.fen}, Sol.Len: ${randomPuzzle.solutionLength}")
            ChessBoard.parseFenToBoard(randomPuzzle.fen)
        } else {
            Log.e(TAG, "$TAG: No HARD puzzles found matching criteria (solution length >10, no White Queen) in JSON. Returning empty board.")
            ChessBoard.createEmpty()
        }
    }
}