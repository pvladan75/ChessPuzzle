package com.chess.chesspuzzle.modul2

import android.util.Log
import kotlin.random.Random
import com.chess.chesspuzzle.* // Uvozimo sve iz glavnog paketa za ChessBoard, Piece, Square, itd.

class PositionGenerator {
    private val random: Random = Random.Default
    private val TAG = "PositionGenerator"
    private val puzzleSolver = PuzzleSolver() // Instanca solvera

    // Data klasa za generisanu zagonetku
    data class GeneratedPuzzle(
        val initialBoard: ChessBoard, // Sada vraća kompletnu ChessBoard
        val solutionPath: List<Move>, // SADA ĆE SE OVO POPUNITI STVARNIM REŠENJEM IZ SOLVERA
        val whitePiece: Piece // Dodato da bi znali koja je bela figura i gde je na pocetku
    )

    /**
     * Glavna funkcija za generisanje zagonetke sa definisanim brojem crnih figura.
     * Uvek osigurava da se validna pozicija vrati, čak i ako se zanemari uslov lanaca.
     *
     * @param numBlackBishops Željeni broj crnih lovaca (max 2).
     * @param numBlackRooks Željeni broj crnih topova (max 2).
     * @param numBlackKnights Željeni broj crnih skakača (max 2).
     * @param numBlackPawns Željeni broj crnih pešaka (max 8).
     * @return GeneratedPuzzle objekat sa početnim pozicijama.
     */
    fun generatePuzzle(
        numBlackBishops: Int = 0,
        numBlackRooks: Int = 0,
        numBlackKnights: Int = 0,
        numBlackPawns: Int = 0
    ): GeneratedPuzzle {
        // Validacija unosa za broj figura
        require(numBlackBishops >= 0 && numBlackBishops <= 2) { "Broj crnih lovaca mora biti između 0 i 2." }
        require(numBlackRooks >= 0 && numBlackRooks <= 2) { "Broj crnih topova mora biti između 0 i 2." }
        require(numBlackKnights >= 0 && numBlackKnights <= 2) { "Broj crnih skakača mora biti između 0 i 2." }
        require(numBlackPawns >= 0 && numBlackPawns <= 8) { "Broj crnih pešaka mora biti između 0 i 8." }

        val totalBlackPieces = numBlackBishops + numBlackRooks + numBlackKnights + numBlackPawns
        require(totalBlackPieces > 0) { "Mora postojati barem jedna crna figura na tabli." }

        val maxAttempts = 500 // Povećan broj pokušaja zbog solvera
        var attempt = 0
        var result: GeneratedPuzzle? = null

        while (attempt < maxAttempts) {
            try {
                // U ovoj fazi uvek generišemo sa kraljicom i uvek proveravamo lance
                val generated = tryGeneratePuzzleInternal(
                    numBlackBishops, numBlackRooks, numBlackKnights, numBlackPawns, checkChains = true
                )

                // *** KLJUČNI DEO: Provera rešivosti sa solverom ***
                Log.d(TAG, "Attempting to solve generated puzzle...")
                // Prosleđujemo generisanu početnu tablu i belu figuru solveru
                val solution = puzzleSolver.solve(generated.initialBoard)

                if (solution != null) { // Ako solver pronađe bilo kakvu putanju (i do 0 figura)
                    // Proverimo da li je rešenje dovelo do 0 crnih figura
                    var finalBoard = generated.initialBoard
                    for (move in solution) {
                        finalBoard = finalBoard.movePiece(move)
                    }
                    val remainingBlackPieces = finalBoard.pieces.values.count { it.color == PieceColor.BLACK }

                    if (remainingBlackPieces == 0) {
                        Log.d(TAG, "Puzzle is solvable! All pieces captured in ${solution.size} moves. Returning this puzzle.")
                        result = GeneratedPuzzle(generated.initialBoard, solution, generated.whitePiece)
                        break // Pronašli smo rešivu zagonetku
                    } else {
                        Log.w(TAG, "Generated puzzle is solvable, but not all pieces are captured (${remainingBlackPieces} remaining). Retrying for a perfect solution.")
                    }
                } else {
                    Log.w(TAG, "Generated puzzle is NOT solvable. Retrying...")
                }
            } catch (e: RuntimeException) {
                Log.w(TAG, "Attempt ${attempt + 1} failed during generation or initial checks: ${e.message}")
            }
            attempt++
        }

        return result ?: throw IllegalStateException("Nije moguće generisati rešivu zagonetku nakon svih pokušaja. Proverite uslove generisanja i logiku solvera.")
    }

    /**
     * Interna funkcija za pokušaj generisanja zagonetke.
     * Može baciti RuntimeException ako generisanje ne uspe.
     */
    private fun tryGeneratePuzzleInternal(
        numBlackBishops: Int,
        numBlackRooks: Int,
        numBlackKnights: Int,
        numBlackPawns: Int,
        checkChains: Boolean
    ): GeneratedPuzzle {
        var currentBoard = ChessBoard.createEmpty() // Koristimo tvoju ChessBoard
        val occupiedSquares = mutableSetOf<Square>()

        // 1. Generiši belu damu na nasumičnoj poziciji
        val initialWhitePiece = Piece(PieceType.QUEEN, PieceColor.WHITE) // Fiksirano na QUEEN za sada

        val allSquares = Square.ALL_SQUARES.shuffled(random)
        val initialWhitePieceSquare = allSquares.firstOrNull()
            ?: throw RuntimeException("Nema dostupnih kvadrata za belu figuru (${initialWhitePiece.type}).")

        currentBoard = currentBoard.setPiece(initialWhitePiece, initialWhitePieceSquare)
        occupiedSquares.add(initialWhitePieceSquare)
        Log.d(TAG, "White ${initialWhitePiece.type} placed at: ${initialWhitePieceSquare}")

        // 2. Generiši crne figure (Topovi, Skakači, Lovci, Pešaci)
        val piecesToPlace = mutableListOf<PieceType>()
        repeat(numBlackRooks) { piecesToPlace.add(PieceType.ROOK) }
        repeat(numBlackKnights) { piecesToPlace.add(PieceType.KNIGHT) }
        repeat(numBlackBishops) { piecesToPlace.add(PieceType.BISHOP) }
        repeat(numBlackPawns) { piecesToPlace.add(PieceType.PAWN) }
        piecesToPlace.shuffle(random) // Mešaj redosled postavljanja

        var bishopOnLightSquare = false
        var bishopOnDarkSquare = false

        // Inicijalizuj dostupna polja za crne figure
        val availableSquaresForBlack = Square.ALL_SQUARES.filter { it !in occupiedSquares }.toMutableList().shuffled(random).toMutableList()
        var currentBlackPieceIndex = 0

        for (pieceType in piecesToPlace) {
            var placed = false
            for (attemptCount in 0 until availableSquaresForBlack.size) { // Pokušaj sva dostupna polja
                if (currentBlackPieceIndex >= availableSquaresForBlack.size) {
                    throw RuntimeException("Nema dovoljno praznih polja za sve figure. Pokušavam ponovo.")
                }
                val position = availableSquaresForBlack[currentBlackPieceIndex]

                // Preskoči zauzeta polja (moguće ako se lista ne regeneriše na pravi način, ali sa remove bi trebalo da je ok)
                if (position in occupiedSquares) {
                    currentBlackPieceIndex++
                    continue
                }

                val newPiece = Piece(pieceType, PieceColor.BLACK)

                // Posebna logika za lovce
                if (pieceType == PieceType.BISHOP && numBlackBishops == 2) {
                    val isDarkSquare = (position.fileIndex + position.rankIndex) % 2 != 0 // Ispravna provera tamnog polja
                    if (!bishopOnLightSquare && !isDarkSquare) { // Ako nema lovca na svetlom i ovo je svetlo polje
                        bishopOnLightSquare = true
                        currentBoard = currentBoard.setPiece(newPiece, position)
                        occupiedSquares.add(position)
                        placed = true
                        Log.d(TAG, "Placed black ${newPiece.type} at ${position} (light square).")
                        break // Figura je postavljena, pređi na sledeću
                    } else if (!bishopOnDarkSquare && isDarkSquare) { // Ako nema lovca na tamnom i ovo je tamno polje
                        bishopOnDarkSquare = true
                        currentBoard = currentBoard.setPiece(newPiece, position)
                        occupiedSquares.add(position)
                        placed = true
                        Log.d(TAG, "Placed black ${newPiece.type} at ${position} (dark square).")
                        break // Figura je postavljena, pređi na sledeću
                    } else {
                        // Polje nije odgovarajuće boje, pokušaj sledeće prazno polje
                        currentBlackPieceIndex++
                        continue // Nastavi petlju for (attemptCount)
                    }
                } else { // Za ostale figure ili ako je samo 1 lovac
                    currentBoard = currentBoard.setPiece(newPiece, position)
                    occupiedSquares.add(position)
                    placed = true
                    Log.d(TAG, "Placed black ${newPiece.type} at ${position}.")
                    break // Figura je postavljena, pređi na sledeću
                }
            }
            if (!placed) {
                throw RuntimeException("Nema dovoljno slobodnih polja za postavljanje figure ${pieceType}. Pokušavam ponovo.")
            }
            // Ažuriraj listu dostupnih polja
            availableSquaresForBlack.removeAll(occupiedSquares)
            availableSquaresForBlack.shuffle(random) // Ponovo izmešaj dostupna polja
            currentBlackPieceIndex = 0 // Resetuj index za sledeću figuru
        }

        // Provera da li su svi lovci postavljeni na ispravne boje polja ako je traženo 2
        if (numBlackBishops == 2 && !(bishopOnLightSquare && bishopOnDarkSquare)) {
            throw RuntimeException("Nisu postavljena dva lovca na različite boje polja. Pokušavam ponovo.")
        }

        // PROVERE ZA VALIDNOST POZICIJE
        val allBlackPieces = currentBoard.getPiecesMapFromBoard(PieceColor.BLACK).values.toList()

        // A. Provera lanaca (samo ako je checkChains true)
        if (checkChains && hasCircularDependencies(allBlackPieces, currentBoard)) {
            throw RuntimeException("Detektovan ciklus u odnosima odbrane crnih figura. Ponovo generišem.")
        }

        // B. Provera da li postoji barem jedna nebranjena figura
        val unprotectedBlackPieces = findUnprotectedBlackPieces(currentBoard, allBlackPieces)
        if (unprotectedBlackPieces.isEmpty()) {
            throw RuntimeException("Nema nebranjenih crnih figura za napad. Generisanje neuspešno.")
        }
        Log.d(TAG, "Unprotected black pieces: ${unprotectedBlackPieces.joinToString { "${it.type} at ${currentBoard.getSquareOfPiece(it)}" }}")


        // C. Provera da li bela figura može da napadne barem jednu nebranjenu figuru
        val targetPiece = unprotectedBlackPieces.random(random) // Nasumično odaberi nebranjenu figuru
        val targetSquare = currentBoard.getSquareOfPiece(targetPiece)
            ?: throw IllegalStateException("Ciljna figura nije pronađena na tabli.")

        val possibleWhitePieceStartPositions = findSafeQueenAttackPositions(
            targetPiece,
            targetSquare,
            currentBoard,
            initialWhitePieceSquare, // Prosleđujemo inicijalni kvadrat bele figure
            allBlackPieces.filter { it != targetPiece } // Ostatak crnih figura
        )

        if (possibleWhitePieceStartPositions.isEmpty()) {
            throw RuntimeException("Nema sigurne pozicije za belu figuru (${initialWhitePiece.type}) da napadne prvu metu (${targetPiece.type} na ${targetSquare}).")
        }

        val finalWhitePieceSquare = possibleWhitePieceStartPositions.random(random)
        // Ažuriraj poziciju bele figure na tabli
        currentBoard = currentBoard.removePiece(initialWhitePieceSquare) // Ukloni sa stare pozicije
        currentBoard = currentBoard.setPiece(initialWhitePiece, finalWhitePieceSquare) // Postavi na novu poziciju

        Log.d(TAG, "Final White ${initialWhitePiece.type} position set to: ${finalWhitePieceSquare} to attack ${targetPiece.type} at ${targetSquare}.")

        // Path je null jer ga rešava solver
        return GeneratedPuzzle(currentBoard, emptyList(), initialWhitePiece) // Vraćamo i belu figuru
    }

    /**
     * Proverava da li postoji kružna zavisnost u odbrani među crnim figurama.
     * Npr. A brani B, B brani A.
     */
    private fun hasCircularDependencies(blackPieces: List<Piece>, board: ChessBoard): Boolean {
        val adjList = mutableMapOf<Square, MutableList<Square>>()
        // Ovu liniju (pieceMap) si imao, ali je nekorišćena. Možemo je izbaciti.
        // val pieceMap = blackPieces.associateBy { board.getSquareOfPiece(it)!! }

        blackPieces.forEach { piece ->
            val square = board.getSquareOfPiece(piece)!!
            adjList[square] = mutableListOf()
        }

        for (defenderPiece in blackPieces) {
            val defenderSquare = board.getSquareOfPiece(defenderPiece)!!
            val tempBoardForCycleCheck = board.removePiece(defenderSquare) // Ukloni defendera sa kopije

            for (targetPiece in blackPieces) {
                if (defenderPiece == targetPiece) continue

                val targetSquare = board.getSquareOfPiece(targetPiece)!!

                // Proveri da li defender napada targeta na privremenoj tabli
                // KORISTI TVOJU EKSTENZIJU ZA PROVERU NAPADA
                // Bilo je isSquareAttackedByAnyOpponent(targetSquare, PieceColor.WHITE) - to proverava napad BELE na TARGET SQUARE
                // Nama treba napad CRNE figure (defenderPiece) na targetSquare
                val attackerInfo = tempBoardForCycleCheck.isSquareAttackedByAnyOpponent(targetSquare, PieceColor.BLACK)
                if (attackerInfo?.first == defenderSquare) { // Ako je defenderSquare napadač
                    adjList[defenderSquare]?.add(targetSquare)
                }
            }
        }

        val visited = mutableSetOf<Square>()
        val recursionStack = mutableSetOf<Square>()

        for (piece in blackPieces) {
            val square = board.getSquareOfPiece(piece)!!
            if (!visited.contains(square)) {
                if (dfsCheckCycle(square, adjList, visited, recursionStack)) {
                    return true // Ciklus detektovan
                }
            }
        }
        return false // Nema ciklusa
    }

    /**
     * Pomoćna funkcija za DFS algoritam detekcije ciklusa (koristi Square).
     */
    private fun dfsCheckCycle(
        currentSquare: Square,
        adjList: Map<Square, List<Square>>,
        visited: MutableSet<Square>,
        recursionStack: MutableSet<Square>
    ): Boolean {
        visited.add(currentSquare)
        recursionStack.add(currentSquare)

        for (neighborSquare in adjList[currentSquare] ?: emptyList()) {
            if (!visited.contains(neighborSquare)) {
                if (dfsCheckCycle(neighborSquare, adjList, visited, recursionStack)) {
                    return true
                }
            } else if (recursionStack.contains(neighborSquare)) {
                return true
            }
        }
        recursionStack.remove(currentSquare)
        return false
    }

    /**
     * Pronalazi sve crne figure koje nisu branjene od strane drugih crnih figura.
     */
    private fun findUnprotectedBlackPieces(board: ChessBoard, blackPieces: List<Piece>): List<Piece> {
        val unprotected = mutableListOf<Piece>()
        for (targetPiece in blackPieces) {
            val targetSquare = board.getSquareOfPiece(targetPiece)
                ?: continue // Should not happen if blackPieces is from board

            val otherBlackPieces = blackPieces.filter { it != targetPiece }
            var isProtected = false

            // Proveri da li je targetPiece branjen od strane bilo koje druge crne figure
            for (defenderPiece in otherBlackPieces) {
                val defenderSquare = board.getSquareOfPiece(defenderPiece)!!
                // Privremeno ukloni targetPiece sa table da simulira proveru odbrane
                val tempBoard = board.removePiece(targetSquare)

                // Koristimo isSquareAttackedByAnyOpponent za proveru da li je targetSquare napadnut od defenderPiece
                // Proveravamo da li je targetSquare napadnut od crne figure (defenderPiece je crn)
                val attackerInfo = tempBoard.isSquareAttackedByAnyOpponent(targetSquare, PieceColor.BLACK)
                if (attackerInfo?.first == defenderSquare) { // Ako je defenderSquare napadač
                    isProtected = true
                    break
                }
            }
            if (!isProtected) {
                unprotected.add(targetPiece)
            }
        }
        return unprotected
    }


    /**
     * Pronalazi sigurne pozicije za belu figuru odakle može da napadne datu ciljnu crnu figuru.
     * "Sigurna" znači da sa te pozicije belu figuru ne napadaju preostale crne figure.
     *
     * @param targetPiece Ciljna crna figura koju treba napasti.
     * @param targetSquare Kvadrat ciljne crne figure.
     * @param currentBoard Trenutna tabla.
     * @param initialWhitePieceSquare Inicijalni kvadrat bele figure pre izračunavanja.
     * @param otherBlackPieces Sve crne figure osim ciljne figure.
     */
    private fun findSafeQueenAttackPositions(
        targetPiece: Piece, // Dama je ovde fiksna, pa ne treba whitePieceType
        targetSquare: Square,
        currentBoard: ChessBoard,
        initialWhitePieceSquare: Square, // Originalna pozicija bele figure, da je uklonimo pri testiranju
        otherBlackPieces: List<Piece>
    ): List<Square> {
        val safePositions = mutableListOf<Square>()

        // 1. Privremena tabla bez ciljne figure (jer će biti pojedena)
        //    I bez bele figure na njenoj inicijalnoj poziciji
        var tempBoardForWhitePieceCheck = currentBoard
            .removePiece(targetSquare) // Ukloni metu jer će biti pojedena
            .removePiece(initialWhitePieceSquare) // Ukloni inicijalnu poziciju bele figure

        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val potentialWhitePieceSquare = Square.fromCoordinates(c, r) // fileIndex pa rankIndex

                // Preskoči ako je ovo polje zauzeto drugom crnom figurom
                val pieceOnPotentialSquare = tempBoardForWhitePieceCheck.getPiece(potentialWhitePieceSquare)
                if (pieceOnPotentialSquare.type != PieceType.NONE && pieceOnPotentialSquare.color == PieceColor.BLACK) {
                    continue
                }

                // Privremeno postavi belu figuru (QUEEN) na testiranu poziciju
                val testWhitePiece = Piece(PieceType.QUEEN, PieceColor.WHITE) // Uvek QUEEN
                val testBoard = tempBoardForWhitePieceCheck.setPiece(testWhitePiece, potentialWhitePieceSquare)

                // Proveri da li bela figura sa te pozicije može da napadne ciljnu figuru
                val rawMovesOfWhitePiece = testWhitePiece.type.getRawMoves(potentialWhitePieceSquare, testWhitePiece.color)

                if (rawMovesOfWhitePiece.contains(targetSquare)) {
                    // Dodatna provera za klizeće figure (kraljica, top, lovac)
                    if (testWhitePiece.type.isSlidingPiece()) {
                        if (!testBoard.isPathClear(potentialWhitePieceSquare, targetSquare)) {
                            continue // Putanja nije čista, ovaj potez nije legalan
                        }
                    }

                    // Ako može da napadne, proveri da li je to polje sigurno od napada preostalih crnih figura
                    // Koristimo isSquareAttackedByAnyOpponent za proveru da li je bela figura napadnuta od CRNIH figura
                    // Na testBoardu, belu figuru je potrebno privremeno ukloniti da se simulira napad
                    val boardWithoutTempWhitePiece = testBoard.removePiece(potentialWhitePieceSquare)
                    val attackerInfo = boardWithoutTempWhitePiece.isSquareAttackedByAnyOpponent(potentialWhitePieceSquare, PieceColor.WHITE)

                    if (attackerInfo == null) {
                        safePositions.add(potentialWhitePieceSquare)
                    }
                }
            }
        }
        return safePositions
    }
}

// Ekstenzija funkcija za dobijanje kvadrata figure
// Ova funkcija treba da se nalazi u ChessBoard.kt ili ChessDefinitions.kt
// Ako je već tamo, ne dodaj dvaput!
fun ChessBoard.getSquareOfPiece(piece: Piece): Square? {
    return this.pieces.entries.find { it.value == piece }?.key
}