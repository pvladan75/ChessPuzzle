package com.chess.chesspuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import kotlinx.coroutines.delay
import android.util.Log // Dodaj import za Log

// NAPOMENA: ChessProblem i SolutionMove data klase bi trebalo da su definisane
// u zasebnom fajlu (npr., ChessProblem.kt) u istom paketu.
// NE SMEJU BITI DUPLIKATI OVDE AKO SU VEĆ DEFINISANE.
// Ako ih nemaš u zasebnom fajlu, kopiraj ih odavde i stavi u ChessProblem.kt:
/*
data class SolutionMove(
    val moverPiece: PieceType,
    val initialSquare: Square,
    val moveUCI: String
)

data class ChessProblem(
    val id: Int,
    val difficulty: String,
    val whitePiecesConfig: Map<PieceType, Int>,
    val fen: String,
    val solutionLength: Int,
    val totalBlackCaptured: Int,
    val capturesByPiece: Map<String, Int>,
    val solutionMoves: List<SolutionMove>
)
*/

class SolutionDisplayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Učitavamo sve zagonetke jednom kada se aktivnost kreira.
                    // Koristi se kontekst aktivnosti (this) za pristup assets folderu.
                    val allPuzzles = remember {
                        PuzzleLoader.loadPuzzlesFromJson(this, "puzzles.json")
                    }

                    if (allPuzzles.isNotEmpty()) {
                        // Ako ima zagonetki, prikaži SolutionScreen sa učitanim zagonetkama
                        SolutionScreen(puzzles = allPuzzles)
                    } else {
                        // Ako nema učitanih zagonetki, prikaži poruku o grešci
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nema učitanih zagonetki. Proverite 'puzzles.json' fajl i njegovu strukturu.",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SolutionScreen(puzzles: List<ChessProblem>) {
    // Stanje za trenutni indeks zagonetke u listi
    var currentPuzzleIndex by remember { mutableStateOf(0) }

    // Stanje za trenutni problem koji se prikazuje.
    // Koristimo derivedStateOf za efikasno ažuriranje kada se promeni currentPuzzleIndex.
    val currentProblem by remember(puzzles, currentPuzzleIndex) {
        derivedStateOf {
            // Osiguravamo da je currentPuzzleIndex uvek unutar granica liste zagonetki.
            // Ovo sprečava ArrayIndexOutOfBoundsException.
            if (puzzles.isNotEmpty() && currentPuzzleIndex >= 0 && currentPuzzleIndex < puzzles.size) {
                puzzles[currentPuzzleIndex]
            } else {
                // Ako nema zagonetki ili je indeks nevažeći, vraćamo "mock" problem
                // kako bi se izbegle greške, mada bi ovo trebalo da se obradi ranije.
                ChessProblem(
                    id = 0, difficulty = "N/A", whitePiecesConfig = emptyMap(),
                    fen = "8/8/8/8/8/8/8/8 w - - 0 1", // Prazna tabla
                    solutionLength = 0, totalBlackCaptured = 0, capturesByPiece = emptyMap(),
                    solutionMoves = emptyList()
                )
            }
        }
    }

    // Stanje za trenutnu poziciju na tabli. Inicijalizujemo je parsiranjem FEN-a
    // trenutnog problema. Koristimo remember(currentProblem.fen) da se ponovo
    // inicijalizuje samo kada se FEN string promeni (tj., kada se promeni zagonetka).
    var board: ChessBoard by remember(currentProblem.fen) {
        mutableStateOf(ChessCore.parseFenToBoard(currentProblem.fen))
    }

    // Stanje za indeks trenutnog poteza u rešenju.
    // -1 znači da je prikazana početna pozicija zagonetke.
    var currentMoveIndex by remember { mutableStateOf(-1) }

    // Stanje za kontrolu automatske reprodukcije rešenja (Play/Pause).
    var isPlaying by remember { mutableStateOf(false) }

    // Stanje za obeležena polja na tabli (odakle-dokle je bio poslednji potez).
    var highlightedSquares: Set<Square> by remember { mutableStateOf(emptySet()) }

    // LaunchedEffect za kontrolu automatske reprodukcije rešenja.
    // Pokreće se svaki put kada se isPlaying ili currentMoveIndex promene.
    LaunchedEffect(isPlaying, currentMoveIndex) {
        // Logika za automatsku reprodukciju:
        // Ako je reprodukcija aktivna i nismo stigli do kraja rešenja,
        // pauziraj 1.5 sekunde, pa pređi na sledeći potez.
        if (isPlaying && currentMoveIndex < currentProblem.solutionMoves.size - 1) {
            delay(1500L) // Pauza između poteza (1.5 sekundi)
            currentMoveIndex++ // Idi na sledeći potez
        } else if (isPlaying && currentMoveIndex >= currentProblem.solutionMoves.size - 1) {
            // Ako je reprodukcija aktivna i stigli smo do kraja rešenja, zaustavi je.
            isPlaying = false
        }
    }

    // DisposableEffect se pokreće kada se currentMoveIndex ili currentProblem promene.
    // Ovo osigurava da se stanje table i obeleženih polja pravilno ažurira.
    DisposableEffect(currentMoveIndex, currentProblem) {
        if (currentMoveIndex == -1) {
            // Ako je indeks -1 (reset), postavljamo tablu na početnu poziciju
            // zagonetke i uklanjamo obeležena polja.
            board = ChessCore.parseFenToBoard(currentProblem.fen) // Ponovo parsiraj FEN za reset
            highlightedSquares = emptySet()
        } else if (currentMoveIndex >= 0 && currentMoveIndex < currentProblem.solutionMoves.size) {
            // Ako je validan indeks poteza, simuliramo taj potez na tabli.
            val solutionMove = currentProblem.solutionMoves[currentMoveIndex]
            val (from, to) = PuzzleLoader.parseUciToSquares(solutionMove.moveUCI) ?: run {
                // Ako parsiranje UCI stringa ne uspe, loguj grešku i zaustavi reprodukciju.
                Log.e("SolutionScreen", "Nevažeći UCI potez: ${solutionMove.moveUCI} za problem ID ${currentProblem.id}")
                isPlaying = false
                return@DisposableEffect onDispose {} // Prekini dalje izvršavanje DisposableEffect-a
            }

            val pieceToMove = board.getPiece(from)

            if (pieceToMove.type != PieceType.NONE) {
                // Logika simulacije poteza:
                // 1. Kreiraj novu kopiju table da ne bi direktno menjao staro stanje.
                // 2. Ukloni figuru sa "from" polja.
                var newBoard = board.removePiece(from)
                // 3. Proveri da li je potez hvatanje (ako na ciljnom polju postoji figura suprotne boje).
                val pieceAtTarget = board.getPiece(to)
                if (pieceAtTarget.type != PieceType.NONE && pieceAtTarget.color != pieceToMove.color) {
                    newBoard = newBoard.removePiece(to) // Ukloni uhvaćenu figuru
                }
                // 4. Postavi figuru na "to" polje.
                newBoard = newBoard.setPiece(to, pieceToMove)
                // 5. Ažuriraj stanje table.
                board = newBoard
                // 6. Obeleži "from" i "to" polja za vizuelnu indikaciju.
                highlightedSquares = setOf(from, to)
            } else {
                // Ako figura ne postoji na "from" polju, to je indikator problema u definiciji
                // rešenja zagonetke. Loguj grešku i zaustavi reprodukciju.
                Log.e("SolutionScreen", "Figura ne postoji na ${from} za potez ${solutionMove.moveUCI} u zagonetki ID ${currentProblem.id}. Prekinuta reprodukcija.")
                highlightedSquares = emptySet()
                isPlaying = false
            }
        } else {
            // Ako je index van opsega poteza (npr., prešli smo kraj rešenja), očisti obeležavanja.
            highlightedSquares = emptySet()
        }
        onDispose { /* Nema posebnog čišćenja ovde */ }
    }


    // Glavni raspored ekrana
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Rešenje zagonetke ${currentPuzzleIndex + 1} / ${puzzles.size}",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "ID: ${currentProblem.id}, Težina: ${currentProblem.difficulty}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Početna pozicija (FEN): ${currentProblem.fen}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Ukupno uhvaćeno crnih: ${currentProblem.totalBlackCaptured}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Dužina rešenja: ${currentProblem.solutionLength} poteza",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Prikaz šahovske table putem izdvojene komponente ChessBoardComposable
        ChessBoardComposable(
            board = board,
            selectedSquare = null, // U prikazu rešenja nema selektovanih figura korisnika
            highlightedSquares = highlightedSquares, // Obeležavamo polja trenutnog poteza
            onSquareClick = { /* Nema interakcije klikom u prikazu rešenja */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Kontrole za navigaciju kroz različite zagonetke (Prethodna / Sledeća zagonetka)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    isPlaying = false // Zaustavi automatsku reprodukciju
                    currentMoveIndex = -1 // Resetuj prikaz poteza za novu zagonetku
                    if (currentPuzzleIndex > 0) {
                        currentPuzzleIndex-- // Pređi na prethodnu zagonetku
                    }
                },
                enabled = currentPuzzleIndex > 0, // Dugme je aktivno samo ako nismo na prvoj zagonetki
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Text("Prethodna zagonetka")
            }

            Button(
                onClick = {
                    isPlaying = false // Zaustavi automatsku reprodukciju
                    currentMoveIndex = -1 // Resetuj prikaz poteza za novu zagonetku
                    if (currentPuzzleIndex < puzzles.size - 1) {
                        currentPuzzleIndex++ // Pređi na sledeću zagonetku
                    }
                },
                enabled = currentPuzzleIndex < puzzles.size - 1, // Dugme je aktivno samo ako nismo na poslednjoj zagonetki
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("Sledeća zagonetka")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Kontrole za navigaciju kroz poteze rešenja (Prethodni / Play-Pauza / Sledeći potez)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    isPlaying = false // Zaustavi automatsku reprodukciju
                    if (currentMoveIndex > -1) {
                        currentMoveIndex-- // Vrati se na prethodni potez
                    }
                },
                enabled = currentMoveIndex > -1, // Dugme je aktivno samo ako nismo na početku rešenja
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Text("Prethodni potez")
            }

            Button(
                onClick = { isPlaying = !isPlaying }, // Prebaci stanje Play/Pauza
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text(if (isPlaying) "Pauza" else "Play")
            }

            Button(
                onClick = {
                    isPlaying = false // Zaustavi automatsku reprodukciju
                    if (currentMoveIndex < currentProblem.solutionMoves.size - 1) {
                        currentMoveIndex++ // Pređi na sledeći potez
                    }
                },
                enabled = currentMoveIndex < currentProblem.solutionMoves.size - 1, // Dugme je aktivno samo ako nismo na kraju rešenja
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("Sledeći potez")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dugme za resetovanje rešenja (vraća na početnu poziciju trenutne zagonetke)
        Button(
            onClick = {
                isPlaying = false // Zaustavi automatsku reprodukciju
                currentMoveIndex = -1 // Resetuj na početnu poziciju
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Resetuj rešenje")
        }
    }
}

// Previews za Compose. Omogućavaju vizualizaciju komponenti u Android Studiju.
@Preview(showBackground = true)
@Composable
fun SolutionScreenPreview() {
    ChessPuzzleTheme {
        val previewPuzzles = remember {
            listOf(
                ChessProblem(
                    id = 1,
                    difficulty = "Lako",
                    whitePiecesConfig = mapOf(PieceType.BISHOP to 1),
                    fen = "1B3q2/p5p1/7p/8/8/4r3/8/8 w - - 0 1",
                    solutionLength = 5,
                    totalBlackCaptured = 5,
                    capturesByPiece = mapOf("Bishop (b8)" to 5),
                    solutionMoves = listOf(
                        SolutionMove(PieceType.BISHOP, Square('b', 8), "b8a7"),
                        SolutionMove(PieceType.BISHOP, Square('a', 7), "a7e3")
                    )
                ),
                ChessProblem(
                    id = 2,
                    difficulty = "Lako",
                    whitePiecesConfig = mapOf(PieceType.QUEEN to 1),
                    fen = "8/1pn5/1pq5/Q1p5/4p3/8/8/8 w - - 0 1",
                    solutionLength = 6,
                    totalBlackCaptured = 6,
                    capturesByPiece = mapOf("Queen (a5)" to 6),
                    solutionMoves = listOf(
                        SolutionMove(PieceType.QUEEN, Square('a', 5), "a5b6"),
                        SolutionMove(PieceType.QUEEN, Square('b', 6), "b6c6")
                    )
                )
            )
        }
        SolutionScreen(puzzles = previewPuzzles)
    }
}