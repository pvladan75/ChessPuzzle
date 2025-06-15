package com.chess.chesspuzzle

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import kotlin.random.Random

// UVEZITE sve definicije iz novog fajla ChessDefinitions.kt
import com.chess.chesspuzzle.*

object PuzzleGenerator {

    private const val TAG = "PuzzleGen"

    private lateinit var soundPool: SoundPool
    private var successSoundId: Int = 0
    private var failureSoundId: Int = 0
    private var soundPoolLoaded: Boolean = false

    fun initializeSoundPool(context: Context) {
        if (::soundPool.isInitialized && soundPoolLoaded) {
            Log.d(TAG, "SoundPool already initialized and sounds loaded.")
            return
        } else if (::soundPool.isInitialized && !soundPoolLoaded) {
            Log.d(TAG, "SoundPool already initialized, but sounds not loaded yet. Waiting...")
            // This case might mean it's still loading from a previous call, or it failed.
            // We'll let the load listener handle the soundPoolLoaded flag.
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

        soundPool.setOnLoadCompleteListener { sp, sampleId, status ->
            if (status == 0) {
                loadedCount++
                Log.d(TAG, "Sound with ID $sampleId loaded successfully. Loaded $loadedCount/$totalSoundsToLoad")
                if (loadedCount == totalSoundsToLoad) {
                    soundPoolLoaded = true
                    Log.d(TAG, "All sounds loaded successfully!")
                }
            } else {
                Log.e(TAG, "Error loading sound with ID $sampleId. Status: $status")
            }
        }

        successSoundId = soundPool.load(context, R.raw.succes, 1)
        failureSoundId = soundPool.load(context, R.raw.failed, 1)

        Log.d(TAG, "SoundPool initialized. Initiating sound loading...")
    }

    fun releaseSoundPool() {
        if (::soundPool.isInitialized) {
            soundPool.release()
            soundPoolLoaded = false
            Log.d(TAG, "SoundPool released.")
        }
    }

    // THIS IS THE *ONLY* playSound function. It must be public to be called from GameActivity.
    fun playSound(context: Context, isSuccess: Boolean) {
        if (!::soundPool.isInitialized) {
            Log.e(TAG, "SoundPool not initialized. Attempting to initialize now (this should be done in Activity/Fragment onCreate!).")
            // Attempt initialization, but warn that it's not ideal if called repeatedly.
            initializeSoundPool(context)
            if (!soundPoolLoaded) {
                Log.w(TAG, "SoundPool still loading after attempted re-init. Cannot play sound yet.")
                return
            }
        }

        if (soundPoolLoaded) {
            if (isSuccess) {
                soundPool.play(successSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
                Log.d(TAG, "Playing success sound.")
            } else {
                soundPool.play(failureSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
                Log.d(TAG, "Playing failure sound.")
            }
        } else {
            Log.w(TAG, "SoundPool is initialized but sounds are still loading. Cannot play sound.")
        }
    }

    // Funkcija za LAKU zagonetku
    fun generateEasyPuzzle(context: Context, selectedFigures: List<PieceType>): ChessBoard {
        Log.d(TAG, "Lako: Pokrenuto generisanje zagonetke sa figurama: $selectedFigures")
        val maxAttempts = 1000
        val minMovesPerPiece = 3 // Fiksno: Svaka figura pravi min 3 poteza
        val maxMovesPerPiece = 6 // Fiksno: Svaka figura pravi max 6 poteza

        if (selectedFigures.isEmpty()) {
            Log.e(TAG, "Lako: Nema odabranih figura za generisanje zagonetke!")
            playSound(context, false)
            return ChessBoard.createEmpty()
        }

        val figuresToUse = if (selectedFigures.size >= 2) selectedFigures.take(2) else selectedFigures.take(1)
        if (figuresToUse.isEmpty()) {
            Log.e(TAG, "Lako: Nema dovoljno odabranih figura za generisanje zagonetke!")
            playSound(context, false)
            return ChessBoard.createEmpty()
        }

        val totalMinPawns = figuresToUse.size * minMovesPerPiece
        val totalMaxPawns = figuresToUse.size * maxMovesPerPiece

        val random = Random.Default

        for (attempt in 1..maxAttempts) {
            var currentBoardForSimulation = ChessBoard.createEmpty()
            val occupiedSquares = mutableSetOf<Square>()

            val allPassThroughSquares = mutableSetOf<Square>() // Polja kroz koja figura prolazi (bez početnih/krajnjih)
            val piecePaths = mutableMapOf<PieceType, List<Square>>()
            val initialPositions = mutableMapOf<PieceType, Square>()
            val allPawnTargetSquares = mutableSetOf<Square>() // Sva krajnja polja putanja (gde će biti pešaci)

            var allPathsValid = true
            val allGeneratedPaths = mutableListOf<List<Square>>() // Skladišti sve putanje (ciljna polja)
            var totalPawnsGenerated = 0

            // 1. Postavi bele figure
            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = findRandomEmptySquare(currentBoardForSimulation, occupiedSquares)
                if (startSquare == null) {
                    Log.d(TAG, "Lako: Nije moguće pronaći početno polje za ${pieceType}. Pokušaj $attempt")
                    allPathsValid = false
                    break
                }
                currentBoardForSimulation = currentBoardForSimulation.setPiece(startSquare, whitePiece)
                occupiedSquares.add(startSquare)
                initialPositions[pieceType] = startSquare
            }
            if (!allPathsValid) continue

            // 2. Generiši putanje i prikupi pass-through/target polja
            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = initialPositions[pieceType]!!

                val numMovesForThisPiece = Random.nextInt(minMovesPerPiece, maxMovesPerPiece + 1)
                Log.d(TAG, "Lako: Pokušaj $attempt - Generišem sa $pieceType i $numMovesForThisPiece poteza.")

                // Privremena tabla za generisanje putanje - sadrži samo bele figure na početnim pozicijama
                val tempBoardForPathGeneration = ChessBoard.createEmpty()
                for ((pType, sSquare) in initialPositions) {
                    tempBoardForPathGeneration.setPiece(sSquare, Piece(pType, PieceColor.WHITE))
                }

                val pathSegments = ChessCore.generatePiecePath(tempBoardForPathGeneration, whitePiece, startSquare, numMovesForThisPiece)

                if (pathSegments.size != numMovesForThisPiece) {
                    Log.d(TAG, "Lako: Neuspešna generacija putanje za $pieceType. Željeno: $numMovesForThisPiece, Dobijeno: ${pathSegments.size}. Pokušaj $attempt")
                    allPathsValid = false
                    break
                }

                // Provera: Putanja se ne sme preklapati sa početnim pozicijama drugih belih figura
                val conflictingStartSquares = (pathSegments.toSet() intersect initialPositions.values.toSet()) - setOf(startSquare)
                if (conflictingStartSquares.isNotEmpty()) {
                    Log.d(TAG, "Lako: Putanja figure $pieceType se preklapa sa drugom belom figurom na ${conflictingStartSquares}. Nije dozvoljeno. Pokušaj $attempt")
                    allPathsValid = false
                    break
                }

                // Provera: Putanje različitih belih figura se ne smeju preklapati
                for (existingPath in allGeneratedPaths) {
                    val intersection = pathSegments.toSet().intersect(existingPath.toSet())
                    if (intersection.isNotEmpty()) {
                        Log.d(TAG, "Lako: Putanje se preklapaju na: $intersection. Nije dozvoljeno. Pokušaj $attempt")
                        allPathsValid = false
                        break
                    }
                }
                if (!allPathsValid) break

                piecePaths[pieceType] = pathSegments
                allGeneratedPaths.add(pathSegments)
                allPawnTargetSquares.addAll(pathSegments) // Dodaj sva ciljna polja u set pešaka
                totalPawnsGenerated += pathSegments.size

                // Izračunaj "pass-through" polja za ovu putanju
                var currentPos: Square = startSquare
                for (moveTarget in pathSegments) {
                    // Dobijamo polja IZMEĐU (ne uključujući currentPos i moveTarget)
                    val passThrough = ChessCore.getSquaresBetween(currentPos, moveTarget)
                    if (pieceType != PieceType.KNIGHT) { // Skakač nema pass-through polja
                        allPassThroughSquares.addAll(passThrough)
                    }
                    currentPos = moveTarget
                }
            }
            if (!allPathsValid || totalPawnsGenerated < totalMinPawns || totalPawnsGenerated > totalMaxPawns) {
                Log.d(TAG, "Lako: Ukupan broj pešaka ($totalPawnsGenerated) ne odgovara željenom opsegu ($totalMinPawns-$totalMaxPawns) ili putanje nisu validne. Pokušaj $attempt")
                continue
            }

            // 3. Konačno postavljanje figura i provera validnosti table
            var finalPuzzleBoard = ChessBoard.createEmpty()
            var currentPuzzleSuccess = true

            // Prvo postavi bele figure na početne pozicije
            for ((pieceType, startSquare) in initialPositions) {
                finalPuzzleBoard = finalPuzzleBoard.setPiece(startSquare, Piece(pieceType, PieceColor.WHITE))
            }

            // Sada postavi crne pešake na sva ciljna polja
            for (pawnTarget in allPawnTargetSquares) {
                // Provera: Ciljno polje pešaka ne sme biti početna pozicija bele figure
                val isConflictingWithWhiteStart = initialPositions.values.any { it == pawnTarget }
                if (isConflictingWithWhiteStart) {
                    Log.d(TAG, "Lako: Pešak na $pawnTarget se preklapa sa početnom pozicijom bele figure. Nije dozvoljeno. Pokušaj $attempt")
                    currentPuzzleSuccess = false
                    break
                }
                // Provera: Ciljno polje pešaka ne sme biti već zauzeto (drugim pešakom - iako to već sprečava set)
                if (finalPuzzleBoard.getPiece(pawnTarget).type != PieceType.NONE) {
                    Log.d(TAG, "Lako: Ciljno polje $pawnTarget je već zauzeto. Nije dozvoljeno. Pokušaj $attempt")
                    currentPuzzleSuccess = false
                    break
                }
                finalPuzzleBoard = finalPuzzleBoard.setPiece(pawnTarget, Piece(PieceType.PAWN, PieceColor.BLACK))
            }
            if (!currentPuzzleSuccess) continue

            // Konačna provera: Sva "pass-through" polja moraju biti prazna
            for (square in allPassThroughSquares) {
                // Ovo polje ne sme biti ni početna pozicija bele figure
                if (initialPositions.values.contains(square)) {
                    Log.d(TAG, "Lako: Polje $square na prolaznoj putanji je početna pozicija bele figure. Nije dozvoljeno. Pokušaj ${attempt}.")
                    currentPuzzleSuccess = false
                    break
                }
                // Ovo polje ne sme biti ni krajnje polje pešaka
                if (allPawnTargetSquares.contains(square)) {
                    Log.d(TAG, "Lako: Polje $square na prolaznoj putanji je ciljno polje za pešaka. Nije dozvoljeno. Pokušaj ${attempt}.")
                    currentPuzzleSuccess = false
                    break
                }
                // I naravno, mora biti prazno na finalnoj tabli
                if (finalPuzzleBoard.getPiece(square).type != PieceType.NONE) {
                    Log.d(TAG, "Lako: Polje ${square} na prolaznoj putanji (koje mora biti prazno) je zauzeto nekom preprekom. Nije dozvoljeno. Pokušaj ${attempt}.")
                    currentPuzzleSuccess = false
                    break
                }
            }
            if (!currentPuzzleSuccess) continue

            Log.d(TAG, "Lako: Uspešno generisana zagonetka nakon $attempt pokušaja.")
            playSound(context, true)
            finalPuzzleBoard.printBoard()
            return finalPuzzleBoard
        }
        Log.e(TAG, "Lako: Nije moguće generisati Laku zagonetku nakon $maxAttempts pokušaja.")
        playSound(context, false)
        return ChessBoard.createEmpty()
    }

    // Funkcija za SREDNJU zagonetku
    fun generateMediumDifficultyPuzzle(context: Context, selectedFigures: List<PieceType>): ChessBoard {
        Log.d(TAG, "Srednje: Pokrenuto generisanje zagonetke sa figurama: $selectedFigures")
        val maxAttempts = 1000
        val minMovesPerPiece = 3 // Fiksno: Svaka figura pravi min 3 poteza
        val maxMovesPerPiece = 6 // Fiksno: Svaka figura pravi max 6 poteza

        if (selectedFigures.size < 2) {
            Log.e(TAG, "Srednje: Potrebne su najmanje 2 odabrane figure za Srednji nivo.")
            playSound(context, false)
            return ChessBoard.createEmpty()
        }
        val random = Random.Default
        val figuresToUse = selectedFigures.take(2)

        val totalMinPawns = figuresToUse.size * minMovesPerPiece
        val totalMaxPawns = figuresToUse.size * maxMovesPerPiece

        for (attempt in 1..maxAttempts) {
            var currentBoardForSimulation = ChessBoard.createEmpty()
            val occupiedSquares = mutableSetOf<Square>()

            val allPassThroughSquares = mutableSetOf<Square>()
            val piecePaths = mutableMapOf<PieceType, List<Square>>()
            val initialPositions = mutableMapOf<PieceType, Square>()
            val allPawnTargetSquares = mutableSetOf<Square>()

            var allPathsValid = true
            val allGeneratedPaths = mutableListOf<List<Square>>()
            var totalPawnsGenerated = 0

            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = findRandomEmptySquare(currentBoardForSimulation, occupiedSquares)
                if (startSquare == null) {
                    Log.d(TAG, "Srednje: Nije moguće pronaći početno polje za ${pieceType}. Pokušaj $attempt")
                    allPathsValid = false
                    break
                }
                currentBoardForSimulation = currentBoardForSimulation.setPiece(startSquare, whitePiece)
                occupiedSquares.add(startSquare)
                initialPositions[pieceType] = startSquare
            }
            if (!allPathsValid) continue

            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = initialPositions[pieceType]!!

                val numMovesForThisPiece = Random.nextInt(minMovesPerPiece, maxMovesPerPiece + 1)
                Log.d(TAG, "Srednje: Pokušaj $attempt - Generišem sa $pieceType i $numMovesForThisPiece poteza.")

                val tempBoardForPathGeneration = ChessBoard.createEmpty()
                for ((pType, sSquare) in initialPositions) {
                    tempBoardForPathGeneration.setPiece(sSquare, Piece(pType, PieceColor.WHITE))
                }

                val pathSegments = ChessCore.generatePiecePath(tempBoardForPathGeneration, whitePiece, startSquare, numMovesForThisPiece)

                if (pathSegments.size != numMovesForThisPiece) {
                    Log.d(TAG, "Srednje: Neuspešna generacija putanje za $pieceType. Željeno: $numMovesForThisPiece, Dobijeno: ${pathSegments.size}. Pokušaj $attempt")
                    allPathsValid = false
                    break
                }

                val conflictingSquares = (pathSegments.toSet() intersect initialPositions.values.toSet()) - setOf(startSquare)
                if (conflictingSquares.isNotEmpty()) {
                    Log.d(TAG, "Srednje: Putanja figure $pieceType se preklapa sa drugom belom figurom na ${conflictingSquares}. Nije dozvoljeno. Pokušaj $attempt")
                    allPathsValid = false
                    break
                }

                for (existingPath in allGeneratedPaths) {
                    val intersection = pathSegments.toSet().intersect(existingPath.toSet())
                    if (intersection.isNotEmpty()) {
                        Log.d(TAG, "Srednje: Putanje se preklapaju na: $intersection. Nije dozvoljeno. Pokušaj $attempt")
                        allPathsValid = false
                        break
                    }
                }
                if (!allPathsValid) break

                piecePaths[pieceType] = pathSegments
                allGeneratedPaths.add(pathSegments)
                allPawnTargetSquares.addAll(pathSegments)
                totalPawnsGenerated += pathSegments.size

                var currentPos: Square = startSquare
                for (moveTarget in pathSegments) {
                    val passThrough = ChessCore.getSquaresBetween(currentPos, moveTarget)
                    if (pieceType != PieceType.KNIGHT) {
                        allPassThroughSquares.addAll(passThrough)
                    }
                    currentPos = moveTarget
                }
            }
            if (!allPathsValid || totalPawnsGenerated < totalMinPawns || totalPawnsGenerated > totalMaxPawns) {
                Log.d(TAG, "Srednje: Ukupan broj pešaka ($totalPawnsGenerated) ne odgovara željenom opsegu ($totalMinPawns-$totalMaxPawns) ili putanje nisu validne. Pokušaj $attempt")
                continue
            }

            var finalPuzzleBoard = ChessBoard.createEmpty()
            var currentPuzzleSuccess = true

            for ((pieceType, startSquare) in initialPositions) {
                finalPuzzleBoard = finalPuzzleBoard.setPiece(startSquare, Piece(pieceType, PieceColor.WHITE))
            }

            for (pawnTarget in allPawnTargetSquares) {
                if (initialPositions.values.contains(pawnTarget)) {
                    Log.d(TAG, "Srednje: Pešak na $pawnTarget se preklapa sa početnom pozicijom bele figure. Nije dozvoljeno. Pokušaj $attempt")
                    currentPuzzleSuccess = false
                    break
                }
                if (finalPuzzleBoard.getPiece(pawnTarget).type != PieceType.NONE) {
                    Log.d(TAG, "Srednje: Ciljno polje $pawnTarget je već zauzeto. Nije dozvoljeno. Pokušaj $attempt")
                    currentPuzzleSuccess = false
                    break
                }
                finalPuzzleBoard = finalPuzzleBoard.setPiece(pawnTarget, Piece(PieceType.PAWN, PieceColor.BLACK))
            }
            if (!currentPuzzleSuccess) continue

            for (square in allPassThroughSquares) {
                if (initialPositions.values.contains(square)) {
                    continue
                }
                if (allPawnTargetSquares.contains(square)) {
                    continue
                }
                if (finalPuzzleBoard.getPiece(square).type != PieceType.NONE) {
                    Log.d(TAG, "Srednje: Polje ${square} na prolaznoj putanji je zauzeto na finalnoj tabli. Nije dozvoljeno. Pokušaj ${attempt + 1}.")
                    currentPuzzleSuccess = false
                    break
                }
            }
            if (!currentPuzzleSuccess) continue

            Log.d(TAG, "Srednje: Uspešno generisana zagonetka nakon $attempt pokušaja.")
            playSound(context, true)
            finalPuzzleBoard.printBoard()
            return finalPuzzleBoard
        }
        Log.e(TAG, "Srednje: Nije moguće generisati Srednju zagonetku nakon $maxAttempts pokušaja.")
        playSound(context, false)
        return ChessBoard.createEmpty()
    }

    // Funkcija za NAJTEŽU zagonetku
    fun generateHardDifficultyPuzzle(context: Context, selectedFigures: List<PieceType>, totalMinPawns: Int, totalMaxPawns: Int): ChessBoard {
        Log.d(TAG, "Teška: Pokrenuto generisanje zagonetke sa figurama: $selectedFigures, ukupan broj pešaka: $totalMinPawns-$totalMaxPawns")
        val maxAttempts = 5000
        val minMovesPerPiece = 3 // Fiksno: Svaka figura pravi min 3 poteza
        val maxMovesPerPiece = 6 // Fiksno: Svaka figura pravi max 6 poteza

        if (selectedFigures.size < 2) {
            Log.e(TAG, "Teška: Potrebne su najmanje 2 odabrane figure za Teški nivo.")
            playSound(context, false)
            return ChessBoard.createEmpty()
        }
        val figuresToUse = if (selectedFigures.size >= 3) selectedFigures.take(3) else selectedFigures.take(selectedFigures.size)
        val random = Random.Default

        for (attempt in 1..maxAttempts) {
            var currentBoardForSimulation = ChessBoard.createEmpty()
            val occupiedSquares = mutableSetOf<Square>()

            val allPassThroughSquares = mutableSetOf<Square>()
            val piecePaths = mutableMapOf<PieceType, List<Square>>()
            val initialPositions = mutableMapOf<PieceType, Square>()
            val finalPositions = mutableMapOf<PieceType, Square>() // This map seems unused for hard difficulty puzzle generation, consider removal if truly unused
            val allPawnTargetSquares = mutableSetOf<Square>()

            var allPathsValid = true
            val allGeneratedPaths = mutableListOf<List<Square>>()
            var totalPawnsGenerated = 0

            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = findRandomEmptySquare(currentBoardForSimulation, occupiedSquares)
                if (startSquare == null) {
                    Log.d(TAG, "Teška: Nije moguće pronaći početno polje za ${pieceType}. Pokušaj $attempt")
                    allPathsValid = false
                    break
                }
                currentBoardForSimulation = currentBoardForSimulation.setPiece(startSquare, whitePiece)
                occupiedSquares.add(startSquare)
                initialPositions[pieceType] = startSquare
            }
            if (!allPathsValid) continue

            for (pieceType in figuresToUse) {
                val whitePiece = Piece(pieceType, PieceColor.WHITE)
                val startSquare = initialPositions[pieceType]!!

                val numMovesForThisPiece = Random.nextInt(minMovesPerPiece, maxMovesPerPiece + 1)
                Log.d(TAG, "Teška: Pokušaj $attempt - Generišem sa $pieceType i $numMovesForThisPiece poteza.")

                val tempBoardForPathGeneration = ChessBoard.createEmpty()
                for ((pType, sSquare) in initialPositions) {
                    tempBoardForPathGeneration.setPiece(sSquare, Piece(pType, PieceColor.WHITE))
                }

                val pathSegments = ChessCore.generatePiecePath(tempBoardForPathGeneration, whitePiece, startSquare, numMovesForThisPiece)

                if (pathSegments.size != numMovesForThisPiece) {
                    Log.d(TAG, "Teška: Neuspešna generacija putanje za $pieceType. Željeno: $numMovesForThisPiece, Dobijeno: ${pathSegments.size}. Pokušaj $attempt")
                    allPathsValid = false
                    break
                }

                val conflictingStartSquares = (pathSegments.toSet() intersect initialPositions.values.toSet()) - setOf(startSquare)
                if (conflictingStartSquares.isNotEmpty()) {
                    Log.d(TAG, "Teška: Putanja figure $pieceType se preklapa sa početnom pozicijom druge bele figure na ${conflictingStartSquares}. Nije dozvoljeno. Pokušaj $attempt")
                    allPathsValid = false
                    break
                }

                for (existingPath in allGeneratedPaths) {
                    val intersection = pathSegments.toSet().intersect(existingPath.toSet())
                    if (intersection.isNotEmpty()) {
                        Log.d(TAG, "Teška: Putanja figure $pieceType se preklapa sa drugom putanjom na $intersection. Nije dozvoljeno. Pokušaj $attempt")
                        allPathsValid = false
                        break
                    }
                }
                if (!allPathsValid) break

                finalPositions[pieceType] = pathSegments.last()
                piecePaths[pieceType] = pathSegments
                totalPawnsGenerated += pathSegments.size
                allGeneratedPaths.add(pathSegments)
                allPawnTargetSquares.addAll(pathSegments)

                var currentPos: Square = startSquare
                for (moveTarget in pathSegments) {
                    val passThrough = ChessCore.getSquaresBetween(currentPos, moveTarget)
                    if (pieceType != PieceType.KNIGHT) {
                        allPassThroughSquares.addAll(passThrough)
                    }
                    currentPos = moveTarget
                }
            }
            if (!allPathsValid || totalPawnsGenerated < totalMinPawns || totalPawnsGenerated > totalMaxPawns) {
                Log.d(TAG, "Teška: Ukupan broj pešaka ($totalPawnsGenerated) ne odgovara željenom opsegu ($totalMinPawns-$totalMaxPawns) ili putanje nisu validne. Pokušaj $attempt")
                continue
            }

            var finalPuzzleBoard = ChessBoard.createEmpty()
            var currentPuzzleSuccess = true

            for ((pieceType, startSquare) in initialPositions) {
                finalPuzzleBoard = finalPuzzleBoard.setPiece(startSquare, Piece(pieceType, PieceColor.WHITE))
            }

            for (pawnTarget in allPawnTargetSquares) {
                val isConflictingWithWhiteStart = initialPositions.values.any { it == pawnTarget }
                if (isConflictingWithWhiteStart) {
                    Log.d(TAG, "Teška: Pešak na $pawnTarget se preklapa sa početnom pozicijom bele figure. Nije dozvoljeno. Pokušaj $attempt")
                    currentPuzzleSuccess = false
                    break
                }
                if (finalPuzzleBoard.getPiece(pawnTarget).type != PieceType.NONE) {
                    Log.d(TAG, "Teška: Ciljno polje $pawnTarget je već zauzeto. Nije dozvoljeno. Pokušaj $attempt")
                    currentPuzzleSuccess = false
                    break
                }
                finalPuzzleBoard = finalPuzzleBoard.setPiece(pawnTarget, Piece(PieceType.PAWN, PieceColor.BLACK))
            }
            if (!currentPuzzleSuccess) continue

            for (square in allPassThroughSquares) {
                if (initialPositions.values.contains(square)) {
                    continue
                }
                if (allPawnTargetSquares.contains(square)) {
                    continue
                }

                if (finalPuzzleBoard.getPiece(square).type != PieceType.NONE) {
                    Log.d(TAG, "Teška: Polje ${square} na prolaznoj putanji (koje mora biti prazno) je zauzeto nekom preprekom. Nije dozvoljeno. Pokušaj ${attempt + 1}.")
                    currentPuzzleSuccess = false
                    break
                }
            }
            if (!currentPuzzleSuccess) continue

            Log.d(TAG, "Teška: Uspešno generisana zagonetka nakon $attempt pokušaja sa $totalPawnsGenerated pešaka.")
            playSound(context, true)
            finalPuzzleBoard.printBoard()
            return finalPuzzleBoard
        }
        Log.e(TAG, "Teška: Nije moguće generisati Tešku zagonetku nakon $maxAttempts pokušaja.")
        playSound(context, false)
        return ChessBoard.createEmpty()
    }

    private fun findRandomEmptySquare(board: ChessBoard, existingOccupiedSquares: Set<Square>): Square? {
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