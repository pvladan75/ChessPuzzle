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
import java.util.Locale

// Koristimo Kotlin enum za težinu radi bolje prakse
enum class Difficulty { EASY, MEDIUM, HARD }

class FigureSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // KLJUČNO: Dohvati ime igrača iz Intent-a koje je poslato iz MainActivity
        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"

        // Inicijalizujte SoundManager ovde, jer je to globalna funkcionalnost.
        // Možete ga inicijalizovati i u nekom Application klasi ako je SoundPool potreban ranije/stalno.
        // PROMENJENO OVDE: Koristi SoundManager.initialize()
        SoundManager.initialize(applicationContext)

        setContent {
            ChessPuzzleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Prosledi playerName Composable funkciji FigureSelectionScreen
                    FigureSelectionScreen(playerName = playerName)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Otpustite SoundPool resurse kada aktivnost više nije potrebna
        // PROMENJENO OVDE: Koristi SoundManager.release()
        SoundManager.release()
    }
}

@Composable
fun FigureSelectionScreen(playerName: String) {
    val context = LocalContext.current
    var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY) }
    val selectedFigures = remember { mutableStateListOf<PieceType>() }

    // Stanje za odabir moda (Trening ili Takmičarski)
    var isTrainingMode by remember { mutableStateOf(true) } // Podrazumevano Trening Mod

    // Mape koje definišu potrebne parametre po nivou težine
    // NAPOMENA: Ove vrednosti se KORISTE SAMO za generisanje zagonetki u trening modu.
    // Za takmičarski mod, težina se mapira na JSON konfiguraciju zagonetke.
    val requiredFigures = remember {
        mapOf(
            Difficulty.EASY to (1 to 1), // Lako: tačno 1 bela figura
            Difficulty.MEDIUM to (1 to 2), // Srednje: 1 do 2 bele figure
            Difficulty.HARD to (2 to 3)    // Teško: 2 do 3 bele figure
        )
    }

    val numberOfPawnsRange = remember {
        mapOf(
            Difficulty.EASY to (3 to 5),
            Difficulty.MEDIUM to (6 to 9),
            Difficulty.HARD to (10 to 14)
        )
    }

    val currentRequiredMinWhiteFigures = requiredFigures[selectedDifficulty]?.first ?: 1
    val currentRequiredMaxWhiteFigures = requiredFigures[selectedDifficulty]?.second ?: 1
    val currentMinBlackPawns = numberOfPawnsRange[selectedDifficulty]?.first ?: 1
    val currentMaxBlackPawns = numberOfPawnsRange[selectedDifficulty]?.second ?: 1


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Prikaz imena igrača na ekranu za odabir figura
        Text("Ime igrača: $playerName", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // --- Odabir moda (Trening / Takmičarski) ---
        Text("Odaberite mod igre:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
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
                RadioButton(selected = isTrainingMode, onClick = null)
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
                RadioButton(selected = !isTrainingMode, onClick = null)
                Text(text = "Takmičarski")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text("Odaberite težinu:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD).forEach { difficulty ->
                Row(
                    Modifier
                        .selectable(
                            selected = (difficulty == selectedDifficulty),
                            onClick = {
                                selectedDifficulty = difficulty
                                selectedFigures.clear() // Resetuj odabrane figure pri promeni težine
                            },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (difficulty == selectedDifficulty),
                        onClick = null
                    )
                    Text(text = difficulty.name.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(
                            Locale.getDefault()
                        ) else it.toString()
                    })
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
                        "Potrebno tačno ${currentRequiredMinWhiteFigures} bela figura/e."
                    else -> "Potrebno od ${currentRequiredMinWhiteFigures} do ${currentRequiredMaxWhiteFigures} bele figure."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Text(
                text = "Biće postavljeno od ${currentMinBlackPawns} do ${currentMaxBlackPawns} crnih figura.",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }


        // Provera da li je broj selektovanih figura validan za trenutnu težinu
        val canStartGame = if (isTrainingMode) {
            selectedFigures.size in currentRequiredMinWhiteFigures..currentRequiredMaxWhiteFigures && selectedFigures.isNotEmpty()
        } else {
            true // U takmičarskom modu ne biramo figure, pa je uvek validno
        }

        Button(
            onClick = {
                if (canStartGame) {
                    val intent = Intent(context, GameActivity::class.java).apply {
                        putExtra("difficulty", selectedDifficulty.name)
                        putExtra("isTrainingMode", isTrainingMode) // Ključna informacija za GameActivity

                        if (isTrainingMode) {
                            // Samo za trening mod, šaljemo odabrane figure i opseg pešaka
                            putStringArrayListExtra(
                                "selectedFigures",
                                ArrayList(selectedFigures.map { it.name })
                            )
                            putExtra("minPawns", currentMinBlackPawns)
                            putExtra("maxPawns", currentMaxBlackPawns)
                        }
                        // Inače, za takmičarski mod, GameActivity će koristiti samo "difficulty" za učitavanje iz JSON-a
                        putExtra("playerName", playerName)
                    }
                    context.startActivity(intent)
                } else {
                    val msg = when {
                        currentRequiredMinWhiteFigures == currentRequiredMaxWhiteFigures -> "Morate odabrati tačno ${currentRequiredMinWhiteFigures} figuru/e!"
                        else -> "Morate odabrati od ${currentRequiredMinWhiteFigures} do ${currentRequiredMaxWhiteFigures} figura!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            enabled = canStartGame,
            modifier = Modifier.fillMaxWidth() // Dugme zauzima celu širinu
        ) {
            Text("Pokreni igru")
        }

        Spacer(modifier = Modifier.height(8.dp)) // Razmak između dugmadi

        // --- NOVO DUGME: Kreiraj sam poziciju ---
        Button(
            onClick = {
                val intent = Intent(context, PositionCreationActivity::class.java).apply {
                    putExtra("playerName", playerName) // Prosledi ime igrača
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth() // Dugme zauzima celu širinu
        ) {
            Text("Kreiraj sam poziciju")
        }
        // --- KRAJ NOVOG DUGMETA ---
    }
}

@Preview(showBackground = true)
@Composable
fun FigureSelectionPreview() {
    ChessPuzzleTheme {
        FigureSelectionScreen(playerName = "Preview Igrač")
    }
}