package com.chess.chesspuzzle

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme

// Koristimo Kotlin enum za težinu radi bolje prakse
enum class Difficulty { EASY, MEDIUM, HARD }

class FigureSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // KLJUČNO: Dohvati ime igrača iz Intent-a koje je poslato iz MainActivity
        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"

        setContent {
            ChessPuzzleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Prosledi playerName Composable funkciji FigureSelectionScreen
                    FigureSelectionScreen(playerName = playerName)
                }
            }
        }
    }
}

@Composable
fun FigureSelectionScreen(playerName: String) {
    val context = LocalContext.current
    var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY) }
    val selectedFigures = remember { mutableStateListOf<PieceType>() }

    // NOVO: Stanje za odabir moda (Trening ili Takmičarski)
    var isTrainingMode by remember { mutableStateOf(true) } // Podrazumevano Trening Mod

    // Mape koje definišu potrebne parametre po nivou težine
    val requiredFigures = remember {
        mapOf(
            Difficulty.EASY to (1 to 2), // Lako: min 1, max 2 bele figure
            Difficulty.MEDIUM to (2 to 2), // Srednje: tačno 2 bele figure
            Difficulty.HARD to (2 to 3)    // Teško: min 2, max 3 bele figure
        )
    }

    // Mapa koja definiše opseg broja CRNIH PEŠAKA (pawns) na tabli.
    val numberOfPawnsRange = remember {
        mapOf(
            Difficulty.EASY to (3 to 5),  // Lako: 3-5 crnih pešaka
            Difficulty.MEDIUM to (6 to 9), // Srednje: 6-9 crnih pešaka
            Difficulty.HARD to (10 to 14) // Teško: 10-14 crnih pešaka (UKUPNO)
        )
    }

    val currentRequiredMinWhiteFigures = requiredFigures[selectedDifficulty]?.first ?: 1
    val currentRequiredMaxWhiteFigures = requiredFigures[selectedDifficulty]?.second ?: 1
    val currentMinBlackPawns = numberOfPawnsRange[selectedDifficulty]?.first ?: 1
    val currentMaxBlackPawns = numberOfPawnsRange[selectedDifficulty]?.second ?: 1


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Prikaz imena igrača na ekranu za odabir figura
        Text("Ime igrača: $playerName", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // --- NOVO: Odabir moda (Trening / Takmičarski) ---
        Text("Odaberite mod igre:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceAround) {
            // Trening Mod RadioButton
            Row(
                Modifier
                    .selectable(
                        selected = isTrainingMode,
                        onClick = { isTrainingMode = true },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = isTrainingMode, onClick = null) // onClick = null jer se klik detektuje na celom redu
                Text(text = "Trening")
            }

            // Takmičarski Mod RadioButton
            Row(
                Modifier
                    .selectable(
                        selected = !isTrainingMode,
                        onClick = { isTrainingMode = false },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = !isTrainingMode, onClick = null) // onClick = null
                Text(text = "Takmičarski")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text("Odaberite težinu:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD).forEach { difficulty ->
                Row(
                    Modifier
                        .selectable(
                            selected = (difficulty == selectedDifficulty),
                            onClick = {
                                selectedDifficulty = difficulty
                                // KLJUČNO: Resetuj odabrane figure pri promeni težine
                                selectedFigures.clear()
                            },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (difficulty == selectedDifficulty),
                        onClick = null // null click behavior for row overall select
                    )
                    Text(text = difficulty.name.capitalize())
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Prikaz odabira figura samo ako je "Trening Mod" aktivan
        if (isTrainingMode) {
            Text("Odaberite bele figure:", style = MaterialTheme.typography.titleLarge)
            Text(
                text = when {
                    currentRequiredMinWhiteFigures == currentRequiredMaxWhiteFigures ->
                        "Potrebno tačno $currentRequiredMinWhiteFigures bela figura/e."
                    else -> "Potrebno od $currentRequiredMinWhiteFigures do $currentRequiredMaxWhiteFigures bele figure."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Text(
                text = "Biće postavljeno od $currentMinBlackPawns do $currentMaxBlackPawns crnih figura.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            val availableFigures = listOf(PieceType.KNIGHT, PieceType.ROOK, PieceType.BISHOP, PieceType.QUEEN)
            Column {
                availableFigures.chunked(2).forEach { rowFigures ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        rowFigures.forEach { pieceType ->
                            val isSelected = selectedFigures.contains(pieceType)
                            val backgroundColor = if (isSelected) Color.Green.copy(alpha = 0.3f) else Color.Transparent
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable {
                                        if (isSelected) {
                                            selectedFigures.remove(pieceType)
                                        } else {
                                            // Dozvoli dodavanje do max figura za trenutnu težinu
                                            if (selectedFigures.size < currentRequiredMaxWhiteFigures) {
                                                selectedFigures.add(pieceType)
                                            } else {
                                                Toast.makeText(context, "Već ste odabrali maksimalan broj figura za ovaj nivo ($currentRequiredMaxWhiteFigures)!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .background(backgroundColor)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val drawableResId = when (pieceType) {
                                    PieceType.KNIGHT -> R.drawable.wn
                                    PieceType.ROOK -> R.drawable.wr
                                    PieceType.BISHOP -> R.drawable.wb
                                    PieceType.QUEEN -> R.drawable.wq
                                    else -> 0
                                }
                                if (drawableResId != 0) {
                                    Image(
                                        painter = painterResource(id = drawableResId),
                                        contentDescription = pieceType.name,
                                        modifier = Modifier.size(50.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        } else {
            // Ako je Takmičarski mod, prikaži poruku da se figure ne biraju
            Text(
                text = "U Takmičarskom modu figure se automatski biraju na osnovu težine.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }


        // Provera da li je broj selektovanih figura validan za trenutnu težinu
        // U takmičarskom modu, uvek je validno ako je odabran mod
        val canStartGame = if (isTrainingMode) {
            selectedFigures.size in currentRequiredMinWhiteFigures..currentRequiredMaxWhiteFigures && selectedFigures.isNotEmpty()
        } else {
            true // U takmičarskom modu ne biramo figure, pa je uvek validno
        }

        Button(
            onClick = {
                if (canStartGame) {
                    val intent = Intent(context, GameActivity::class.java).apply {
                        putExtra("difficulty", selectedDifficulty.name) // Prosleđujemo ENUM ime kao String
                        // U zavisnosti od moda, prosleđujemo odabrane figure ili praznu listu
                        putStringArrayListExtra(
                            "selectedFigures",
                            ArrayList(if (isTrainingMode) selectedFigures.map { it.name } else emptyList<String>())
                        )
                        putExtra("minPawns", currentMinBlackPawns) // Prosledi min broj crnih pešaka
                        putExtra("maxPawns", currentMaxBlackPawns) // Prosledi max broj crnih pešaka
                        putExtra("playerName", playerName) // Prosledi ime igrača
                        putExtra("isTrainingMode", isTrainingMode) // Prosledi da li je trening mod
                    }
                    context.startActivity(intent)
                } else {
                    val msg = when {
                        currentRequiredMinWhiteFigures == currentRequiredMaxWhiteFigures -> "Morate odabrati tačno $currentRequiredMinWhiteFigures figuru/e!"
                        else -> "Morate odabrati od $currentRequiredMinWhiteFigures do $currentRequiredMaxWhiteFigures figura!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            enabled = canStartGame
        ) {
            Text("Pokreni igru")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FigureSelectionPreview() {
    ChessPuzzleTheme {
        // U preview-u, simuliraćemo da je ime "Preview Igrač"
        FigureSelectionScreen(playerName = "Preview Igrač")
    }
}