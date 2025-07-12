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

        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"

        SoundManager.initialize(applicationContext)

        setContent {
            ChessPuzzleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FigureSelectionScreen(playerName = playerName)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}

@Composable
fun FigureSelectionScreen(playerName: String) {
    val context = LocalContext.current
    var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY) }
    // Koristimo remember mutableStateListOf jer se broj odabranih figura menja
    val selectedFigures = remember { mutableStateListOf<PieceType>() }

    var isTrainingMode by remember { mutableStateOf(true) }

    // Inicijalizacija PositionGeneratora NIJE POTREBNA ovde ako se ne koristi za generisanje FEN-a
    // Direktno iz ove aktivnosti. U vasem slucaju sam je uklonio jer je GameActivity sada zaduzen za generisanje.
    // val chessSolver = remember { ChessSolver() }
    // val positionGenerator = remember { PositionGenerator(chessSolver) } // Uklonjeno

    val requiredFigures = remember {
        mapOf(
            Difficulty.EASY to (1 to 1),
            Difficulty.MEDIUM to (1 to 2),
            Difficulty.HARD to (2 to 3)
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
    val currentMinBlackPawns = numberOfPawnsRange[selectedDifficulty]?.first ?: 3 // Default
    val currentMaxBlackPawns = numberOfPawnsRange[selectedDifficulty]?.second ?: 5 // Default


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            Row(
                Modifier
                    .selectable(
                        selected = isTrainingMode,
                        onClick = {
                            isTrainingMode = true
                            selectedFigures.clear() // Resetuj odabrane figure pri promeni moda
                        },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = isTrainingMode, onClick = null)
                Text(text = "Trening")
            }

            Row(
                Modifier
                    .selectable(
                        selected = !isTrainingMode,
                        onClick = {
                            isTrainingMode = false
                            selectedFigures.clear() // Resetuj odabrane figure pri promeni moda
                        },
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

        val canStartGame = if (isTrainingMode) {
            selectedFigures.size in currentRequiredMinWhiteFigures..currentRequiredMaxWhiteFigures && selectedFigures.isNotEmpty()
        } else {
            true // Takmičarski mod ne zahteva ručni odabir figura
        }

        Button(
            onClick = {
                if (canStartGame) {
                    val intent = Intent(context, GameActivity::class.java).apply {
                        putExtra("difficulty", selectedDifficulty.name)
                        putExtra("isTrainingMode", isTrainingMode)
                        putExtra("playerName", playerName)

                        if (isTrainingMode) {
                            // KLJUČNA IZMENA: Šaljemo listu IMENA figura kao String ArrayList
                            val selectedFigureNames = ArrayList(selectedFigures.map { it.name })
                            putStringArrayListExtra("selectedFigures", selectedFigureNames)

                            // Slanje minPawns i maxPawns za trening mod
                            putExtra("minPawns", currentMinBlackPawns)
                            putExtra("maxPawns", currentMaxBlackPawns)
                        }
                        // Za takmičarski mod, GameActivity će koristiti svoje podrazumevane figure
                        // i broj pešaka na osnovu težine, pa ne treba slati selectedFigures
                        // minPawns i maxPawns u ovom slučaju.
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pokreni igru")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val intent = Intent(context, PositionCreationActivity::class.java).apply {
                    putExtra("playerName", playerName)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kreiraj sam poziciju")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FigureSelectionPreview() {
    ChessPuzzleTheme {
        FigureSelectionScreen(playerName = "Preview Igrač")
    }
}