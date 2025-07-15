package com.chess.chesspuzzle.modul1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Uključuje SnackbarHostState, AlertDialog, DropdownMenu, FilterChip, ExposedDropdownMenuBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ChessBoard
import com.chess.chesspuzzle.ChessCore
import com.chess.chesspuzzle.Piece
import com.chess.chesspuzzle.PieceColor
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import kotlinx.coroutines.launch

// Enum za tip figura koje korisnik može da postavlja
// Ostavljamo ga ovde ako ga ne koristimo na drugom mestu
enum class CreationMode {
    NONE, // Prazno polje
    WHITE_QUEEN, WHITE_ROOK, WHITE_BISHOP, WHITE_KNIGHT, WHITE_PAWN, WHITE_KING,
    BLACK_QUEEN, BLACK_ROOK, BLACK_BISHOP, BLACK_KNIGHT, BLACK_PAWN, BLACK_KING
}

class PositionCreationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"

        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // ISPRAVLJENO: MaterialType.background u MaterialTheme.colorScheme.background
                ) {
                    PositionCreationScreen(playerName = playerName)
                }
            }
        }
    }
}

// Data klasa za rezultat Solvera (treba da bude definisana negde globalno, npr. u ChessSolver.kt ili ChessDefinitions.kt)
// Ako je već definisana u ChessSolver.kt kao što ste mi poslali, onda ovaj deo možete ukloniti.
// Ali ako nije, stavite je ovde ili u ChessDefinitions.kt
data class ChessSolution(
    val isSolved: Boolean,
    val moves: List<String>, // Lista poteza u standardnoj notaciji (npr. "e2e4")
    val reason: String = "" // Objašnjenje ako rešenja nema ili je pozicija nevalidna
)

// Novo: Enum za opcije čuvanja fajla (može i u zaseban fajl ako treba)
enum class SaveFileOption {
    NEW_FILE, EXISTING_FILE
}


@OptIn(ExperimentalMaterial3Api::class) // Potrebno za ExposedDropdownMenuBox i FilterChip
@Composable
fun PositionCreationScreen(playerName: String) {
    val context = LocalContext.current
    var board: ChessBoard by remember { mutableStateOf(ChessBoard.createEmpty()) }
    var currentSelectedSquare: Square? by remember { mutableStateOf(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var fenInput by rememberSaveable { mutableStateOf("") }

    val chessSolver = remember { ChessSolver() }

    // NOVO: Stanje za čuvanje FEN-a i poteza rešene zagonetke
    var solvedPuzzleFen: String? by remember { mutableStateOf(null) }
    var solvedPuzzleMoves: List<String>? by remember { mutableStateOf(null) }

    // NOVO: Stanje za prikaz dijaloga za čuvanje zagonetke
    var showSavePuzzleDialog by remember { mutableStateOf(false) }

    // NOVO: Stanja za inpute unutar dijaloga za čuvanje
    var puzzleNameInput by rememberSaveable { mutableStateOf("") }
    var fileNameInput by rememberSaveable { mutableStateOf("user_puzzles.json") } // Podrazumevano ime fajla
    var selectedSaveOption by remember { mutableStateOf(SaveFileOption.NEW_FILE) }
    var existingPuzzleFiles by remember { mutableStateOf(emptyList<String>()) }
    var selectedExistingFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) } // Za DropdownMenu za postojeće fajlove

    val pieceCycle = remember {
        listOf(
            Piece(PieceType.PAWN, PieceColor.BLACK),
            Piece(PieceType.ROOK, PieceColor.BLACK),
            Piece(PieceType.KNIGHT, PieceColor.BLACK),
            Piece(PieceType.BISHOP, PieceColor.BLACK),
            Piece(PieceType.QUEEN, PieceColor.BLACK),
            Piece(PieceType.KING, PieceColor.BLACK),
            Piece(PieceType.PAWN, PieceColor.WHITE),
            Piece(PieceType.ROOK, PieceColor.WHITE),
            Piece(PieceType.KNIGHT, PieceColor.WHITE),
            Piece(PieceType.BISHOP, PieceColor.WHITE),
            Piece(PieceType.QUEEN, PieceColor.WHITE),
            Piece(PieceType.KING, PieceColor.WHITE),
            Piece.NONE
        )
    }

    val cyclePieces = { square: Square ->
        val existingPiece = board.getPiece(square)
        val currentIndex = pieceCycle.indexOfFirst { it.type == existingPiece.type && it.color == existingPiece.color }

        val nextIndex = if (currentIndex == -1 || currentIndex == pieceCycle.size - 1) {
            0
        } else {
            currentIndex + 1
        }

        val newPiece = pieceCycle[nextIndex]

        if (newPiece.type == PieceType.NONE) {
            board = board.removePiece(square)
        } else {
            board = board.setPiece(newPiece, square)
        }
        fenInput = board.toFEN().split(" ")[0]
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Igrač: $playerName", style = MaterialTheme.typography.titleMedium)
        Text("Kreirajte svoju poziciju", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = fenInput,
                onValueChange = { fenInput = it },
                label = { Text("Unesite FEN poziciju") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                try {
                    board = ChessBoard.parseFenToBoard(fenInput)
                    currentSelectedSquare = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "FEN pozicija uspešno učitana!",
                            duration = SnackbarDuration.Short
                        )
                    }
                    // Resetuj rešenje kada se tabla promeni
                    solvedPuzzleFen = null
                    solvedPuzzleMoves = null
                } catch (e: Exception) {
                    Log.e("PositionCreation", "Greška pri parsiranju FEN-a: ${e.message}")
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Nevalidan FEN format! Pokušajte ponovo.",
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }) {
                Text("Učitaj FEN")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChessBoardComposable(
            board = board,
            selectedSquare = currentSelectedSquare,
            highlightedSquares = emptySet(),
            onSquareClick = { clickedSquare ->
                currentSelectedSquare = clickedSquare
                cyclePieces(clickedSquare)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                board = ChessBoard.createEmpty()
                currentSelectedSquare = null
                fenInput = ""
                solvedPuzzleFen = null
                solvedPuzzleMoves = null
            }) {
                Text("Obriši tablu")
            }
            Button(onClick = {
                board = ChessBoard.createStandardBoard()
                currentSelectedSquare = null
                fenInput = board.toFEN().split(" ")[0]
                solvedPuzzleFen = null
                solvedPuzzleMoves = null
            }) {
                Text("Standardna pozicija")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // MODIFIKOVANO DUGME "Potvrdi poziciju"
        Button(
            onClick = {
                val currentFen = board.toFEN()
                Log.d("PositionCreation", "Prosleđujem FEN Solveru: $currentFen")

                val moveList: List<ChessSolver.MoveData>? = chessSolver.solve(board)

                val solution: ChessSolution
                if (moveList != null && moveList.isNotEmpty()) {
                    solution = ChessSolution(
                        isSolved = true,
                        moves = moveList.map { it.toString() },
                        reason = "Rešenje pronađeno."
                    )
                    solvedPuzzleFen = currentFen // Sačuvaj FEN rešene pozicije
                    solvedPuzzleMoves = solution.moves // Sačuvaj poteze rešene pozicije
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Rešenje pronađeno! Možete sačuvati zagonetku.",
                            duration = SnackbarDuration.Long
                        )
                    }
                } else {
                    solution = ChessSolution(false, emptyList(), "Nema rešenja za ovu poziciju.")
                    solvedPuzzleFen = null // Resetuj ako nema rešenja
                    solvedPuzzleMoves = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = solution.reason,
                            duration = SnackbarDuration.Long
                        )
                    }
                }

                if (solution.isSolved) {
                    val intent = Intent(context, SolutionDisplayActivity::class.java).apply {
                        putExtra("puzzleFen", currentFen)
                        putExtra("solutionMoves", ArrayList(solution.moves))
                    }
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Potvrdi i proveri poziciju")
        }

        // NOVO DUGME: Sačuvaj Zagonetku
        Button(
            onClick = {
                if (solvedPuzzleFen != null && solvedPuzzleMoves != null) {
                    // Kada se otvara dijalog, inicijalizuj ime zagonetke i učitaj postojeće fajlove
                    puzzleNameInput = "Moja Zagonetka (${solvedPuzzleFen?.take(10)}...)" // Predloži ime
                    coroutineScope.launch {
                        existingPuzzleFiles = PuzzleDataHandler.getListOfUserPuzzleFiles(context)
                        if (existingPuzzleFiles.isNotEmpty()) {
                            selectedExistingFileName = existingPuzzleFiles.first()
                            selectedSaveOption =
                                SaveFileOption.EXISTING_FILE // Podrazumevano odaberi postojeći ako ih ima
                        } else {
                            selectedExistingFileName = null
                            selectedSaveOption =
                                SaveFileOption.NEW_FILE // Ako nema fajlova, uvek idi na novi
                        }
                        showSavePuzzleDialog = true
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Prvo pronađite rešenje za poziciju!",
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            // Omogući dugme samo ako je rešenje pronađeno
            enabled = solvedPuzzleFen != null && solvedPuzzleMoves != null
        ) {
            Text("Sačuvaj Zagonetku")
        }


        Button(
            onClick = {
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Nazad na Odabir Figure")
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.CenterHorizontally))
    }

    // --- Implementacija DIJALOGA ZA ČUVANJE ZAGONETKE ---
    if (showSavePuzzleDialog) {
        AlertDialog(
            onDismissRequest = { showSavePuzzleDialog = false },
            title = { Text("Sačuvaj Zagonetku") },
            text = {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Unos naziva zagonetke
                    OutlinedTextField(
                        value = puzzleNameInput,
                        onValueChange = { puzzleNameInput = it },
                        label = { Text("Naziv Zagonetke") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Odabir opcije: Novi fajl / Postojeći fajl
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        FilterChip(
                            selected = selectedSaveOption == SaveFileOption.NEW_FILE,
                            onClick = { selectedSaveOption = SaveFileOption.NEW_FILE },
                            label = { Text("Novi Fajl") }
                        )
                        FilterChip(
                            selected = selectedSaveOption == SaveFileOption.EXISTING_FILE,
                            onClick = {
                                if (existingPuzzleFiles.isNotEmpty()) {
                                    selectedSaveOption = SaveFileOption.EXISTING_FILE
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Nema postojećih fajlova za odabir!",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            label = { Text("Postojeći Fajl") },
                            enabled = existingPuzzleFiles.isNotEmpty() // Onemogući ako nema postojećih fajlova
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Polje za ime fajla (ako je odabran Novi Fajl)
                    if (selectedSaveOption == SaveFileOption.NEW_FILE) {
                        OutlinedTextField(
                            value = fileNameInput,
                            onValueChange = { fileNameInput = it },
                            label = { Text("Ime Fajla (npr. 'moje_zagonetke.json')") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // Odabir postojećeg fajla (ako je odabran Postojeći Fajl)
                    else { // selectedSaveOption == SaveFileOption.EXISTING_FILE
                        ExposedDropdownMenuBox(
                            expanded = expandedDropdown,
                            onExpandedChange = { expandedDropdown = !expandedDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedExistingFileName ?: "Nijedan fajl",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Odaberite fajl") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                existingPuzzleFiles.forEach { fileName ->
                                    DropdownMenuItem(
                                        text = { Text(fileName) },
                                        onClick = {
                                            selectedExistingFileName = fileName
                                            fileNameInput = fileName // Ažuriraj i fileNameInput za internu konzistenciju
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    var targetFileName = if (selectedSaveOption == SaveFileOption.NEW_FILE) {
                        fileNameInput.trim()
                    } else {
                        selectedExistingFileName?.trim() ?: ""
                    }

                    // NOVO: Automatski dodaj .json ekstenziju ako nedostaje
                    if (targetFileName.isNotEmpty() && !targetFileName.endsWith(".json", ignoreCase = true)) {
                        targetFileName += ".json"
                    }

                    if (targetFileName.isEmpty()) { // Proveravamo samo da li je prazan, format sad sami osiguravamo
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Ime fajla ne može biti prazno!",
                                duration = SnackbarDuration.Long
                            )
                        }
                        return@Button
                    }

                    // Pripremi podatke za ChessProblem
                    val currentFen = solvedPuzzleFen
                    val solutionMoves = solvedPuzzleMoves

                    if (currentFen == null || solutionMoves == null) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Nema rešenja za zagonetku za čuvanje. Pokušajte ponovo proveriti poziciju.",
                                duration = SnackbarDuration.Long
                            )
                        }
                        showSavePuzzleDialog = false
                        return@Button
                    }

                    coroutineScope.launch {
                        try {
                            // Učitaj postojeće zagonetke iz ciljanog fajla
                            val existingPuzzles = PuzzleDataHandler.loadUserPuzzles(
                                context,
                                targetFileName
                            ).toMutableList()

                            // Kreiraj novi ChessProblem objekat
                            val newPuzzle = ChessProblem(
                                name = puzzleNameInput.trim().ifEmpty { "Nova Zagonetka" }, // Koristi uneti naziv
                                difficulty = "Custom", // Možeš dodati logiku za izračun težine
                                whitePiecesConfig = ChessCore.getPieceCountsByColor(board, PieceColor.WHITE), // ISPRAVLJENO: Koristi ChessCore.getPieceCountsByColor
                                fen = currentFen,
                                solutionLength = solutionMoves.size,
                                totalBlackCaptured = 0, // Ova informacija trenutno nije dostupna iz ChessSolver. Neophodno je to dodati ako želite da bude precizno.
                                capturesByPiece = emptyMap(), // Ova informacija trenutno nije dostupna iz ChessSolver. Neophodno je to dodati ako želite da bude precizno.
                                solutionMoves = solutionMoves.map { solutionMoveString ->
                                    // Rekonstrukcija SolutionMove iz UCI stringa.
                                    // Ovo pretpostavlja da board stanje u trenutku čuvanja odgovara početnoj poziciji poteza.
                                    val parsedSquares = ChessCore.parseUciToSquares(solutionMoveString) // ISPRAVLJENO: Koristi ChessCore.parseUciToSquares
                                    val initialSquare = parsedSquares?.first ?: Square('a',1) // Fallback ako parsiranje ne uspe
                                    // Pokušaj da uzmeš figuru sa table na početnom polju tog poteza
                                    val moverPiece = board.getPiece(initialSquare).type // Uzmi tip figure sa table
                                    SolutionMove(moverPiece, initialSquare, solutionMoveString)
                                },
                                creationDate = System.currentTimeMillis()
                            )

                            // Proveri da li zagonetka sa istim FEN-om već postoji da se izbegnu duplikati
                            val existingPuzzleIndex = existingPuzzles.indexOfFirst { it.fen == newPuzzle.fen }
                            if (existingPuzzleIndex != -1) {
                                // Ažuriraj postojeću zagonetku
                                existingPuzzles[existingPuzzleIndex] = newPuzzle
                                snackbarHostState.showSnackbar(
                                    message = "Zagonetka je ažurirana u fajlu '${targetFileName}'!",
                                    duration = SnackbarDuration.Short
                                )
                            } else {
                                // Dodaj novu zagonetku
                                existingPuzzles.add(newPuzzle)
                                snackbarHostState.showSnackbar(
                                    message = "Zagonetka uspešno sačuvana u fajl '${targetFileName}'!",
                                    duration = SnackbarDuration.Short
                                )
                            }

                            // Sačuvaj ažuriranu listu
                            PuzzleDataHandler.saveUserPuzzles(
                                context,
                                targetFileName,
                                existingPuzzles
                            )

                        } catch (e: Exception) {
                            Log.e("PositionCreation", "Greška pri čuvanju zagonetke: ${e.message}", e)
                            snackbarHostState.showSnackbar(
                                message = "Greška pri čuvanju zagonetke: ${e.localizedMessage}",
                                duration = SnackbarDuration.Long
                            )
                        } finally {
                            showSavePuzzleDialog = false // Zatvori dijalog bez obzira na ishod
                        }
                    }
                }) {
                    Text("Sačuvaj")
                }
            },
            dismissButton = {
                Button(onClick = { showSavePuzzleDialog = false }) {
                    Text("Odustani")
                }
            }
        )
    }
}