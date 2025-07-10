package com.chess.chesspuzzle

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.google.gson.Gson
import kotlin.random.Random

object PuzzleGenerator {

    private const val TAG = "PuzzleGen"

    private lateinit var soundPool: SoundPool
    private var successSoundId: Int = 0
    private var failureSoundId: Int = 0
    private var soundPoolLoaded: Boolean = false

    private var allLoadedPuzzles: List<ChessProblem> = emptyList()

    fun initializeSoundPool(context: Context) {
        if (::soundPool.isInitialized && soundPoolLoaded) {
            Log.d(TAG, "$TAG: SoundPool already initialized and sounds loaded.")
            loadAllPuzzlesIfNecessary(context.applicationContext)
            return
        } else if (::soundPool.isInitialized && !soundPoolLoaded) {
            Log.d(TAG, "$TAG: SoundPool already initialized, but sounds not loaded yet. Skipping re-init.")
            return
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        var loadedCount = 0
        val totalSoundsToLoad = 2

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedCount++
                Log.d(TAG, "$TAG: Sound with ID $sampleId loaded successfully. Loaded $loadedCount/$totalSoundsToLoad")
                if (loadedCount == totalSoundsToLoad) {
                    soundPoolLoaded = true
                    Log.d(TAG, "$TAG: All sounds loaded successfully!")
                    loadAllPuzzlesIfNecessary(context.applicationContext)
                }
            } else {
                Log.e(TAG, "$TAG: Error loading sound with ID $sampleId. Status: $status")
            }
        }

        successSoundId = soundPool.load(context, R.raw.succes, 1)
        failureSoundId = soundPool.load(context, R.raw.failed, 1)

        Log.d(TAG, "$TAG: SoundPool initialized. Initiating sound loading...")
    }

    fun releaseSoundPool() {
        if (::soundPool.isInitialized) {
            soundPool.release()
            soundPoolLoaded = false
            allLoadedPuzzles = emptyList()
            Log.d(TAG, "$TAG: SoundPool released and puzzle cache cleared.")
        }
    }

    fun playSound(isSuccess: Boolean) {
        if (!::soundPool.isInitialized) {
            Log.e(TAG, "$TAG: SoundPool not initialized. Cannot play sound.")
            return
        }

        if (soundPoolLoaded) {
            val soundIdToPlay = if (isSuccess) successSoundId else failureSoundId
            if (soundIdToPlay != 0) {
                soundPool.play(soundIdToPlay, 1.0f, 1.0f, 0, 0, 1.0f)
                Log.d(TAG, "$TAG: Playing ${if (isSuccess) "success" else "failure"} sound.")
            } else {
                Log.e(TAG, "$TAG: Sound ID is 0. Sound not loaded.")
            }
        } else {
            Log.w(TAG, "$TAG: SoundPool is initialized but sounds are still loading. Cannot play sound.")
        }
    }

    private fun loadAllPuzzlesIfNecessary(context: Context) {
        if (allLoadedPuzzles.isEmpty()) {
            Log.d(TAG, "$TAG: Loading puzzles from puzzles.json into cache...")
            allLoadedPuzzles = PuzzleLoader.loadPuzzlesFromJson(context, "puzzles.json")
            Log.d(TAG, "$TAG: Loaded ${allLoadedPuzzles.size} puzzles into cache.")
        } else {
            Log.d(TAG, "$TAG: Puzzles already loaded in cache (${allLoadedPuzzles.size} puzzles).")
        }
    }

    /**
     * Učitava zagonetku iz JSON fajla za takmičarski mod (Lako).
     * Filtrira zagonetke po "Lako" težini i solutionLength od 5 do 7.
     * Broj pešaka je zanemaren.
     */
    fun loadEasyPuzzleFromJson(context: Context): ChessBoard { // Uklonjeni minPawns i maxPawns
        loadAllPuzzlesIfNecessary(context)
        Log.d(TAG, "$TAG: Attempting to load EASY puzzle from JSON. Solution length: 5-7. Pawns criterion removed.")

        val filteredPuzzles = allLoadedPuzzles.filter { puzzle ->
            puzzle.difficulty.equals("Lako", ignoreCase = true) &&
                    puzzle.solutionLength in 5..7
        }

        val randomPuzzle = filteredPuzzles.randomOrNull(Random.Default)

        return if (randomPuzzle != null) {
            Log.d(TAG, "$TAG: Loaded EASY puzzle ID: ${randomPuzzle.id}, FEN: ${randomPuzzle.fen}, Sol.Len: ${randomPuzzle.solutionLength}")
            ChessCore.parseFenToBoard(randomPuzzle.fen)
        } else {
            Log.e(TAG, "$TAG: No EASY puzzles found matching criteria (solution length 5-7) in JSON. Returning empty board.")
            ChessBoard.createEmpty()
        }
    }

    /**
     * Učitava zagonetku iz JSON fajla za takmičarski mod (Srednje).
     * Filtrira zagonetke po "Srednje" težini i solutionLength od 8 do 10.
     * Broj pešaka je zanemaren.
     */
    fun loadMediumPuzzleFromJson(context: Context): ChessBoard { // Uklonjeni minPawns i maxPawns
        loadAllPuzzlesIfNecessary(context)
        Log.d(TAG, "$TAG: Attempting to load MEDIUM puzzle from JSON. Solution length: 8-10. Pawns criterion removed.")

        val filteredPuzzles = allLoadedPuzzles.filter { puzzle ->
            puzzle.difficulty.equals("Srednje", ignoreCase = true) &&
                    puzzle.solutionLength in 8..10
        }

        val randomPuzzle = filteredPuzzles.randomOrNull(Random.Default)

        return if (randomPuzzle != null) {
            Log.d(TAG, "$TAG: Loaded MEDIUM puzzle ID: ${randomPuzzle.id}, FEN: ${randomPuzzle.fen}, Sol.Len: ${randomPuzzle.solutionLength}")
            ChessCore.parseFenToBoard(randomPuzzle.fen)
        } else {
            Log.e(TAG, "$TAG: No MEDIUM puzzles found matching criteria (solution length 8-10) in JSON. Returning empty board.")
            ChessBoard.createEmpty()
        }
    }

    /**
     * Učitava zagonetku iz JSON fajla za takmičarski mod (Teško).
     * Filtrira zagonetke po "Teško" težini i solutionLength > 10.
     * Broj pešaka je zanemaren.
     */
    fun loadHardPuzzleFromJson(context: Context): ChessBoard { // Uklonjeni minPawns i maxPawns
        loadAllPuzzlesIfNecessary(context)
        Log.d(TAG, "$TAG: Attempting to load HARD puzzle from JSON. Solution length: >10. Pawns criterion removed.")

        val filteredPuzzles = allLoadedPuzzles.filter { puzzle ->
            puzzle.difficulty.equals("Teško", ignoreCase = true) &&
                    puzzle.solutionLength > 10
        }

        val randomPuzzle = filteredPuzzles.randomOrNull(Random.Default)

        return if (randomPuzzle != null) {
            Log.d(TAG, "$TAG: Loaded HARD puzzle ID: ${randomPuzzle.id}, FEN: ${randomPuzzle.fen}, Sol.Len: ${randomPuzzle.solutionLength}")
            ChessCore.parseFenToBoard(randomPuzzle.fen)
        } else {
            Log.e(TAG, "$TAG: No HARD puzzles found matching criteria (solution length >10) in JSON. Returning empty board.")
            ChessBoard.createEmpty()
        }
    }

    /**
     * Generiše nasumičnu laku zagonetku za trening mod.
     * Postavlja 1 belu figuru (od odabranih) i crne figure (targete) na putanji bele figure.
     * Uklonjeni su kraljevi iz logike generisanja.
     */
    fun generateEasyRandomPuzzle(context: Context, selectedFigures: List<PieceType>, minTotalPawns: Int, maxTotalPawns: Int): ChessBoard {
        Log.d(TAG, "$TAG: Easy: Starting RANDOM puzzle generation. Selected figures: $selectedFigures, Pawns: $minTotalPawns-$maxTotalPawns")
        val maxAttempts = 2000 // Povećan broj pokušaja
        val minMovesPerPiece = 1
        val maxMovesPerPiece = 3

        val figuresToUse = if (selectedFigures.isEmpty()) {
            listOf(PieceType.KNIGHT)
        } else {
            selectedFigures.shuffled(Random.Default).take(1)
        }

        if (figuresToUse.isEmpty()) {
            Log.e(TAG, "$TAG: Easy: No valid figures selected or default figure failed. Cannot generate puzzle.")
            return ChessBoard.createEmpty()
        }

        val random = Random.Default

        for (attempt in 1..maxAttempts) {
            var currentBoardForSimulation = ChessBoard.createEmpty()
            val occupiedSquares = mutableSetOf<Square>()

            val allPassThroughSquares = mutableSetOf<Square>()
            val initialPositions = mutableMapOf<PieceType, Square>()
            val allTargetSquares = mutableSetOf<Square>()

            var generationSuccessful = true
            val allGeneratedPaths = mutableListOf<List<Square>>()
            var totalTargetsGeneratedThisAttempt = 0

            // 1. Postavi odabranu belu figuru
            val whitePieceType = figuresToUse[0]
            val whitePiece = Piece(whitePieceType, PieceColor.WHITE)
            val startSquare = findRandomEmptySquare(currentBoardForSimulation, occupiedSquares)
            if (startSquare == null) { generationSuccessful = false; continue }

            startSquare.let { safeStartSquare ->
                currentBoardForSimulation = currentBoardForSimulation.setPiece(safeStartSquare, whitePiece)
                occupiedSquares.add(safeStartSquare)
                initialPositions[whitePieceType] = safeStartSquare

                // 2. Generiši putanju za belu figuru i prikupi ciljna polja
                val numMovesForThisPiece = random.nextInt(minMovesPerPiece, maxMovesPerPiece + 1)
                val pathSegments = ChessCore.generatePiecePath(currentBoardForSimulation, whitePiece, safeStartSquare, numMovesForThisPiece)

                if (pathSegments.size != numMovesForThisPiece) {
                    generationSuccessful = false
                } else {
                    allGeneratedPaths.add(pathSegments)
                    allTargetSquares.addAll(pathSegments)
                    totalTargetsGeneratedThisAttempt += pathSegments.size

                    if (totalTargetsGeneratedThisAttempt < minTotalPawns || totalTargetsGeneratedThisAttempt > maxTotalPawns) {
                        generationSuccessful = false
                    }

                    // Ažuriraj allPassThroughSquares na osnovu generisane putanje
                    var currentPathPos = safeStartSquare
                    for (moveTarget in pathSegments) {
                        val passThrough = ChessCore.getSquaresBetween(currentPathPos, moveTarget)
                        if (whitePieceType != PieceType.KNIGHT) {
                            allPassThroughSquares.addAll(passThrough)
                        }
                        currentPathPos = moveTarget
                    }

                    // Proveri konflikte sa početnim pozicijama belih figura na "pass-through" poljima
                    val conflictingPassThrough = allPassThroughSquares.any { sq -> initialPositions.values.contains(sq) }
                    if (conflictingPassThrough) {
                        generationSuccessful = false
                    }
                }
            }

            if (!generationSuccessful) continue

            // 3. Konačno postavljanje figura za zagonetku
            var finalPuzzleBoard = ChessBoard.createEmpty()
            val safeStartSquare = initialPositions[whitePieceType]
            if (safeStartSquare != null) {
                finalPuzzleBoard = finalPuzzleBoard.setPiece(safeStartSquare, whitePiece)
            } else {
                generationSuccessful = false
            }
            if (!generationSuccessful) continue

            // Postavi crne figure (meta) na allTargetSquares
            val availableBlackPieceTypes = mutableListOf(
                PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN
            )
            availableBlackPieceTypes.remove(PieceType.QUEEN)

            for (targetSquare in allTargetSquares) {
                if (finalPuzzleBoard.getPiece(targetSquare).type != PieceType.NONE || initialPositions.values.contains(targetSquare)) {
                    generationSuccessful = false
                    break
                }
                val blackPieceType = availableBlackPieceTypes.random(random)
                finalPuzzleBoard = finalPuzzleBoard.setPiece(targetSquare, Piece(blackPieceType, PieceColor.BLACK))
            }
            if (!generationSuccessful) continue

            // Proveri da li je bilo koje "pass-through" polje zauzeto
            for (square in allPassThroughSquares) {
                if (!initialPositions.values.contains(square) && !allTargetSquares.contains(square) && finalPuzzleBoard.getPiece(square).type != PieceType.NONE) {
                    generationSuccessful = false
                    break
                }
            }
            if (!generationSuccessful) continue

            val blackPiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
            val whitePiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).size

            if (blackPiecesCount > 0 && whitePiecesCount > 0) {
                Log.d(TAG, "$TAG: Easy (Random): Successfully generated puzzle after $attempt attempts. FEN: ${finalPuzzleBoard.toFEN()}")
                return finalPuzzleBoard
            }
        }
        Log.e(TAG, "$TAG: Easy (Random): Failed to GENERATE Easy puzzle after $maxAttempts attempts.")
        return ChessBoard.createEmpty()
    }

    /**
     * Generiše nasumičnu srednju zagonetku za trening mod.
     * Postavlja 1-2 bele figure (od odabranih) i crne figure (targete) na putanjama belih figura.
     * Uklonjeni su kraljevi iz logike generisanja.
     */
    fun generateMediumRandomPuzzle(context: Context, selectedFigures: List<PieceType>, minTotalPawns: Int, maxTotalPawns: Int): ChessBoard {
        Log.d(TAG, "$TAG: Medium: Starting RANDOM puzzle generation. Selected figures: $selectedFigures, Pawns: $minTotalPawns-$maxTotalPawns")
        val maxAttempts = 2000
        val minMovesPerPiece = 2
        val maxMovesPerPiece = 4

        val numWhitePieces = Random.nextInt(1, 3)
        val figuresToUse = if (selectedFigures.isEmpty()) {
            listOf(PieceType.QUEEN, PieceType.ROOK).shuffled(Random.Default).take(numWhitePieces)
        } else {
            selectedFigures.shuffled(Random.Default).take(numWhitePieces)
        }

        if (figuresToUse.isEmpty()) {
            Log.e(TAG, "$TAG: Medium: No valid figures selected or default figure failed. Cannot generate puzzle.")
            return ChessBoard.createEmpty()
        }

        val random = Random.Default

        for (attempt in 1..maxAttempts) {
            var currentBoardForSimulation = ChessBoard.createEmpty()
            val occupiedSquares = mutableSetOf<Square>()

            val allPassThroughSquares = mutableSetOf<Square>()
            val initialPositions = mutableMapOf<PieceType, Square>()
            val allTargetSquares = mutableSetOf<Square>()

            var generationSuccessful = true
            val allGeneratedPaths = mutableListOf<List<Square>>()
            var totalTargetsGeneratedThisAttempt = 0

            // Postavi bele figure
            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = findRandomEmptySquare(currentBoardForSimulation, occupiedSquares)
                if (startSquare == null) { generationSuccessful = false; break }

                startSquare.let { safeStartSquare ->
                    currentBoardForSimulation = currentBoardForSimulation.setPiece(safeStartSquare, whitePiece)
                    occupiedSquares.add(safeStartSquare)
                    initialPositions[pieceType] = safeStartSquare
                }
            }
            if (!generationSuccessful) continue

            // Generiši putanje
            for (entry in initialPositions) {
                val pieceType = entry.key
                val startSquare = entry.value
                val whitePiece = Piece(pieceType, PieceColor.WHITE)

                val numMovesForThisPiece = random.nextInt(minMovesPerPiece, maxMovesPerPiece + 1)
                val pathSegments = ChessCore.generatePiecePath(currentBoardForSimulation, whitePiece, startSquare, numMovesForThisPiece)

                if (pathSegments.size != numMovesForThisPiece) {
                    generationSuccessful = false
                    break
                }
                allGeneratedPaths.add(pathSegments)
                allTargetSquares.addAll(pathSegments)
                totalTargetsGeneratedThisAttempt += pathSegments.size

                var currentPathPos = startSquare
                for (moveTarget in pathSegments) {
                    val passThrough = ChessCore.getSquaresBetween(currentPathPos, moveTarget)
                    if (pieceType != PieceType.KNIGHT) {
                        allPassThroughSquares.addAll(passThrough)
                    }
                    currentPathPos = moveTarget
                }
            }
            if (!generationSuccessful) continue

            if (totalTargetsGeneratedThisAttempt < minTotalPawns || totalTargetsGeneratedThisAttempt > maxTotalPawns) {
                generationSuccessful = false
                continue
            }

            // Konačno postavljanje figura
            var finalPuzzleBoard = ChessBoard.createEmpty()
            for ((pieceType, startSquare) in initialPositions) {
                finalPuzzleBoard = finalPuzzleBoard.setPiece(startSquare, Piece(pieceType, PieceColor.WHITE))
            }

            val availableBlackPieceTypes = mutableListOf(
                PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN
            )

            for (targetSquare in allTargetSquares) {
                if (finalPuzzleBoard.getPiece(targetSquare).type != PieceType.NONE || initialPositions.values.contains(targetSquare)) {
                    generationSuccessful = false
                    break
                }
                val blackPieceType = availableBlackPieceTypes.random(random)
                finalPuzzleBoard = finalPuzzleBoard.setPiece(targetSquare, Piece(blackPieceType, PieceColor.BLACK))
            }
            if (!generationSuccessful) continue

            for (square in allPassThroughSquares) {
                if (!initialPositions.values.contains(square) && !allTargetSquares.contains(square) && finalPuzzleBoard.getPiece(square).type != PieceType.NONE) {
                    generationSuccessful = false
                    break
                }
            }
            if (!generationSuccessful) continue

            val blackPiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
            val whitePiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).size

            if (blackPiecesCount > 0 && whitePiecesCount > 0) {
                Log.d(TAG, "$TAG: Medium (Random): Successfully generated puzzle after $attempt attempts. FEN: ${finalPuzzleBoard.toFEN()}")
                return finalPuzzleBoard
            }
        }
        Log.e(TAG, "$TAG: Medium (Random): Failed to GENERATE Medium puzzle after $maxAttempts attempts.")
        return ChessBoard.createEmpty()
    }

    /**
     * Generiše nasumičnu tešku zagonetku za trening mod.
     * Postavlja 2-3 bele figure (od odabranih) i crne figure (targete) na putanjama belih figura.
     * Uklonjeni su kraljevi iz logike generisanja.
     */
    fun generateHardRandomPuzzle(context: Context, selectedFigures: List<PieceType>, minTotalPawns: Int, maxTotalPawns: Int): ChessBoard {
        Log.d(TAG, "$TAG: Hard: Starting RANDOM puzzle generation. Selected figures: $selectedFigures, Pawns: $minTotalPawns-$maxTotalPawns")
        val maxAttempts = 3000
        val minMovesPerPiece = 3
        val maxMovesPerPiece = 6

        val numWhitePieces = Random.nextInt(2, 4)
        val figuresToUse = if (selectedFigures.size < numWhitePieces) {
            val defaultPowerful = mutableListOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).shuffled(Random.Default)
            (selectedFigures + defaultPowerful).distinct().take(numWhitePieces)
        } else {
            selectedFigures.shuffled(Random.Default).take(numWhitePieces)
        }

        if (figuresToUse.isEmpty()) {
            Log.e(TAG, "$TAG: Hard: No valid figures selected or default figures failed. Cannot generate puzzle.")
            return ChessBoard.createEmpty()
        }

        val random = Random.Default

        for (attempt in 1..maxAttempts) {
            var currentBoardForSimulation = ChessBoard.createEmpty()
            val occupiedSquares = mutableSetOf<Square>()

            val allPassThroughSquares = mutableSetOf<Square>()
            val initialPositions = mutableMapOf<PieceType, Square>()
            val allTargetSquares = mutableSetOf<Square>()

            var generationSuccessful = true
            val allGeneratedPaths = mutableListOf<List<Square>>()
            var totalTargetsGeneratedThisAttempt = 0

            // Postavi bele figure
            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = findRandomEmptySquare(currentBoardForSimulation, occupiedSquares)
                if (startSquare == null) { generationSuccessful = false; break }

                startSquare.let { safeStartSquare ->
                    currentBoardForSimulation = currentBoardForSimulation.setPiece(safeStartSquare, whitePiece)
                    occupiedSquares.add(safeStartSquare)
                    initialPositions[pieceType] = safeStartSquare
                }
            }
            if (!generationSuccessful) continue

            // Generiši putanje
            for (entry in initialPositions) {
                val pieceType = entry.key
                val startSquare = entry.value
                val whitePiece = Piece(pieceType, PieceColor.WHITE)

                val numMovesForThisPiece = random.nextInt(minMovesPerPiece, maxMovesPerPiece + 1)
                val pathSegments = ChessCore.generatePiecePath(currentBoardForSimulation, whitePiece, startSquare, numMovesForThisPiece)

                if (pathSegments.size != numMovesForThisPiece) {
                    generationSuccessful = false
                    break
                }
                allGeneratedPaths.add(pathSegments)
                allTargetSquares.addAll(pathSegments)
                totalTargetsGeneratedThisAttempt += pathSegments.size

                var currentPathPos = startSquare
                for (moveTarget in pathSegments) {
                    val passThrough = ChessCore.getSquaresBetween(currentPathPos, moveTarget)
                    if (pieceType != PieceType.KNIGHT) {
                        allPassThroughSquares.addAll(passThrough)
                    }
                    currentPathPos = moveTarget
                }
            }
            if (!generationSuccessful) continue

            if (totalTargetsGeneratedThisAttempt < minTotalPawns || totalTargetsGeneratedThisAttempt > maxTotalPawns) {
                generationSuccessful = false
                continue
            }

            // Konačno postavljanje figura
            var finalPuzzleBoard = ChessBoard.createEmpty()
            for ((pieceType, startSquare) in initialPositions) {
                finalPuzzleBoard = finalPuzzleBoard.setPiece(startSquare, Piece(pieceType, PieceColor.WHITE))
            }

            val availableBlackPieceTypes = mutableListOf(
                PieceType.PAWN, PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN
            )

            for (targetSquare in allTargetSquares) {
                if (finalPuzzleBoard.getPiece(targetSquare).type != PieceType.NONE || initialPositions.values.contains(targetSquare)) {
                    generationSuccessful = false
                    break
                }
                val blackPieceType = availableBlackPieceTypes.random(random)
                finalPuzzleBoard = finalPuzzleBoard.setPiece(targetSquare, Piece(blackPieceType, PieceColor.BLACK))
            }
            if (!generationSuccessful) continue

            for (square in allPassThroughSquares) {
                if (!initialPositions.values.contains(square) && !allTargetSquares.contains(square) && finalPuzzleBoard.getPiece(square).type != PieceType.NONE) {
                    generationSuccessful = false
                    break
                }
            }
            if (!generationSuccessful) continue

            val blackPiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.BLACK).size
            val whitePiecesCount = finalPuzzleBoard.getPiecesMapFromBoard(PieceColor.WHITE).size

            if (blackPiecesCount > 0 && whitePiecesCount > 0) {
                Log.d(TAG, "$TAG: Hard (Random): Successfully generated puzzle after $attempt attempts. FEN: ${finalPuzzleBoard.toFEN()}")
                return finalPuzzleBoard
            }
        }
        Log.e(TAG, "$TAG: Hard (Random): Failed to GENERATE Hard puzzle after $maxAttempts attempts.")
        return ChessBoard.createEmpty()
    }

    private fun findRandomEmptySquare(board: ChessBoard, existingOccupiedSquares: Set<Square>, intendedPieceColor: PieceColor? = null): Square? {
        val emptySquares = mutableListOf<Square>()
        for (rank in 1..8) {
            for (fileChar in 'a'..'h') {
                val square = Square(fileChar, rank)
                if (board.getPiece(square).type == PieceType.NONE && !existingOccupiedSquares.contains(square)) {
                    emptySquares.add(square)
                }
            }
        }
        return emptySquares.randomOrNull(Random.Default)
    }

}