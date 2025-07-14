// PositionGenerator.kt
package com.chess.chesspuzzle.modul2 // Premešteno u modul2 paket

import android.util.Log
import kotlin.random.Random
import com.chess.chesspuzzle.* // Uvozimo sve iz glavnog paketa za ChessBoard, Piece, Square, itd.

class PositionGenerator {
    private val random: Random = Random.Default
    private val TAG = "PositionGenerator"

    // Data klasa za generisanu zagonetku
    data class GeneratedPuzzle(
        val initialBoard: ChessBoard, // Sada vraća kompletnu ChessBoard
        val solutionPath: List<Move> // Ostaje, iako trenutno nije popunjeno
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

        val maxAttemptsForChainCheck = 200
        var attempt = 0
        var generatedWithChainCheck = false
        var result: GeneratedPuzzle? = null

        // Prvo pokušaj sa uslovom lanaca
        while (attempt < maxAttemptsForChainCheck) {
            try {
                result = tryGeneratePuzzleInternal(
                    numBlackBishops, numBlackRooks, numBlackKnights, numBlackPawns, checkChains = true
                )
                generatedWithChainCheck = true
                Log.d(TAG, "Puzzle generated successfully WITH chain check after $attempt attempts.")
                break
            } catch (e: RuntimeException) {
                Log.w(TAG, "Attempt ${attempt + 1} failed with chain check: ${e.message}")
                attempt++
            }
        }

        // Ako nije uspelo sa uslovom lanaca, pokušaj bez njega
        if (!generatedWithChainCheck) {
            Log.w(TAG, "Failed to generate puzzle with chain check after $maxAttemptsForChainCheck attempts. Trying without chain check.")
            attempt = 0
            val maxAttemptsWithoutChainCheck = 50
            while (attempt < maxAttemptsWithoutChainCheck) {
                try {
                    result = tryGeneratePuzzleInternal(
                        numBlackBishops, numBlackRooks, numBlackKnights, numBlackPawns, checkChains = false
                    )
                    Log.d(TAG, "Puzzle generated successfully WITHOUT chain check after $attempt attempts.")
                    break
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Critical: Attempt ${attempt + 1} failed WITHOUT chain check: ${e.message}")
                    attempt++
                }
            }
        }

        return result ?: throw IllegalStateException("Nije moguće generisati zagonetku nakon svih pokušaja. Proverite uslove generisanja.")
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
        val allSquares = Square.ALL_SQUARES.shuffled(random)
        val initialQueenSquare = allSquares.firstOrNull()
            ?: throw RuntimeException("Nema dostupnih kvadrata za kraljicu.")

        val initialQueen = Piece(PieceType.QUEEN, PieceColor.WHITE)
        currentBoard = currentBoard.setPiece(initialQueen, initialQueenSquare)
        occupiedSquares.add(initialQueenSquare)
        Log.d(TAG, "Queen placed at: ${initialQueenSquare}")

        // 2. Generiši crne figure (Topovi, Skakači, Lovci, Pešaci)
        val piecesToPlace = mutableListOf<PieceType>()
        repeat(numBlackRooks) { piecesToPlace.add(PieceType.ROOK) }
        repeat(numBlackKnights) { piecesToPlace.add(PieceType.KNIGHT) }
        repeat(numBlackBishops) { piecesToPlace.add(PieceType.BISHOP) }
        repeat(numBlackPawns) { piecesToPlace.add(PieceType.PAWN) }
        piecesToPlace.shuffle(random) // Mešaj redosled postavljanja

        var bishopOnLightSquare = false
        var bishopOnDarkSquare = false

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
                    val isDarkSquare = (position.fileIndex + position.rankIndex) % 2 != 0
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


        // C. Provera da li dama može da napadne barem jednu nebranjenu figuru
        val targetPiece = unprotectedBlackPieces.random(random) // Nasumično odaberi nebranjenu figuru
        val targetSquare = currentBoard.getSquareOfPiece(targetPiece)
            ?: throw IllegalStateException("Ciljna figura nije pronađena na tabli.")

        val possibleQueenStartPositions = findSafeQueenAttackPositions(
            targetPiece,
            targetSquare,
            currentBoard,
            initialQueenSquare, // Prosleđujemo inicijalni kvadrat kraljice
            allBlackPieces.filter { it != targetPiece } // Ostatak crnih figura
        )

        if (possibleQueenStartPositions.isEmpty()) {
            throw RuntimeException("Nema sigurne pozicije za damu da napadne prvu metu (${targetPiece.type} na ${targetSquare}).")
        }

        val finalQueenSquare = possibleQueenStartPositions.random(random)
        // Ažuriraj poziciju dame na tabli
        currentBoard = currentBoard.removePiece(initialQueenSquare) // Ukloni sa stare pozicije
        currentBoard = currentBoard.setPiece(initialQueen, finalQueenSquare) // Postavi na novu poziciju

        Log.d(TAG, "Final Queen position set to: ${finalQueenSquare} to attack ${targetPiece.type} at ${targetSquare}.")

        // Trenutno ne generišemo punu putanju rešenja, pa je ostavljamo praznu.
        val solutionPath = listOf(Move(finalQueenSquare, targetSquare))

        return GeneratedPuzzle(currentBoard, solutionPath)
    }

    /**
     * Proverava da li postoji kružna zavisnost u odbrani među crnim figurama.
     * Npr. A brani B, B brani A.
     */
    private fun hasCircularDependencies(blackPieces: List<Piece>, board: ChessBoard): Boolean {
        val adjList = mutableMapOf<Square, MutableList<Square>>()
        val pieceMap = blackPieces.associateBy { board.getSquareOfPiece(it)!! } // Mapa Square -> Piece

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
                if (tempBoardForCycleCheck.isSquareAttackedByAnyOpponent(targetSquare, PieceColor.WHITE)?.first == defenderSquare) {
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
                // Privremeno ukloni targetPiece sa table da vidiš da li defender može da ga napadne
                val tempBoard = board.removePiece(targetSquare)
                val attackInfo = tempBoard.isSquareAttackedByAnyOpponent(targetSquare, PieceColor.WHITE) // Proveri da li bela dama može da ga napadne

                // Ako je targetSquare napadnut od defenderPiece (koji je crn i sada brani), onda je branjen
                if (defenderPiece.type.getRawMoves(defenderSquare, PieceColor.BLACK).contains(targetSquare)) {
                    // Dodatna provera za klizeće figure
                    if (defenderPiece.type.isSlidingPiece()) {
                        if (tempBoard.isPathClear(defenderSquare, targetSquare)) {
                            isProtected = true
                            break
                        }
                    } else {
                        // Pešaci brane dijagonalno, a ne pravo napred
                        if (defenderPiece.type == PieceType.PAWN) {
                            val pawnAttackMoves = if (defenderPiece.color == PieceColor.WHITE) {
                                listOf(Square(defenderSquare.file + 1, defenderSquare.rank + 1), Square(defenderSquare.file - 1, defenderSquare.rank + 1))
                            } else {
                                listOf(Square(defenderSquare.file + 1, defenderSquare.rank - 1), Square(defenderSquare.file - 1, defenderSquare.rank - 1))
                            }
                            if (pawnAttackMoves.contains(targetSquare)) {
                                isProtected = true
                                break
                            }
                        } else {
                            isProtected = true
                            break
                        }
                    }
                }
            }
            if (!isProtected) {
                unprotected.add(targetPiece)
            }
        }
        return unprotected
    }


    /**
     * Pronalazi sigurne pozicije za belu damu odakle može da napadne datu ciljnu crnu figuru.
     * "Sigurna" znači da sa te pozicije damu ne napadaju preostale crne figure.
     */
    private fun findSafeQueenAttackPositions(
        targetPiece: Piece,
        targetSquare: Square,
        currentBoard: ChessBoard,
        initialQueenSquare: Square, // Originalna pozicija kraljice, da je uklonimo pri testiranju
        otherBlackPieces: List<Piece> // Crne figure OSEM ciljne figure
    ): List<Square> {
        val safePositions = mutableListOf<Square>()

        // 1. Privremena tabla bez ciljne figure (jer će biti pojedena)
        //    I bez kraljice na njenoj inicijalnoj poziciji
        var tempBoardForQueenCheck = currentBoard
            .removePiece(targetSquare)
            .removePiece(initialQueenSquare) // Ukloni i originalnu poziciju kraljice

        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val potentialQueenSquare = Square.fromCoordinates(c, r) // fileIndex pa rankIndex

                // Preskoči ako je ovo polje zauzeto drugom crnom figurom (ne ciljnom)
                val pieceOnPotentialSquare = tempBoardForQueenCheck.getPiece(potentialQueenSquare)
                if (pieceOnPotentialSquare.type != PieceType.NONE && pieceOnPotentialSquare.color == PieceColor.BLACK) {
                    continue
                }

                // Privremeno postavi damu na testiranu poziciju
                val testBoard = tempBoardForQueenCheck.setPiece(Piece(PieceType.QUEEN, PieceColor.WHITE), potentialQueenSquare)

                // Proveri da li dama sa te pozicije može da napadne ciljnu figuru
                val queen = testBoard.getPiece(potentialQueenSquare) // Dama na testBoard
                val rawMovesOfQueen = queen.type.getRawMoves(potentialQueenSquare, queen.color)

                if (rawMovesOfQueen.contains(targetSquare)) {
                    // Dodatna provera za klizeće figure - da li je putanja čista
                    if (queen.type.isSlidingPiece()) {
                        if (!testBoard.isPathClear(potentialQueenSquare, targetSquare)) {
                            continue // Putanja nije čista, ovaj potez nije legalan
                        }
                    }

                    // Ako može da napadne, proveri da li je to polje sigurno od napada crnih figura
                    // Koristimo isSquareAttackedByAnyOpponent za proveru da li je bela dama napadnuta od CRNIH figura
                    // Na testBoardu, damu je potrebno privremeno ukloniti da se simulira napad
                    val boardWithoutTempQueen = testBoard.removePiece(potentialQueenSquare)
                    val attackerInfo = boardWithoutTempQueen.isSquareAttackedByAnyOpponent(potentialQueenSquare, PieceColor.WHITE)

                    if (attackerInfo == null) {
                        safePositions.add(potentialQueenSquare)
                    }
                }
            }
        }
        return safePositions
    }
}

// Ekstenzija funkcija za dobijanje kvadrata figure
// Ova funkcija treba da se nalazi u ChessBoard.kt ili ChessDefinitions.kt
fun ChessBoard.getSquareOfPiece(piece: Piece): Square? {
    return this.pieces.entries.find { it.value == piece }?.key
}