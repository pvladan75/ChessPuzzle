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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme

// UVEZITE sve definicije iz novog fajla ChessDefinitions.kt
import com.chess.chesspuzzle.PieceType // Konkretan import umesto zvezdice, za bolju praksu

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
fun FigureSelectionScreen(playerName: String) { // FigureSelectionScreen sada prima playerName
    val context = LocalContext.current
    var selectedDifficulty by remember { mutableStateOf("Lako") }
    // Vraćeno na mutableStateListOf<PieceType>() da se figure resetuju pri promeni težine
    val selectedFigures = remember { mutableStateListOf<PieceType>() }

    // Mapa koja definiše koliko figura je potrebno za svaki nivo
    val requiredFigures = remember { // Dodato remember radi optimizacije
        mapOf(
            "Lako" to (1 to 2), // Lako: min 1, max 2 bele figure
            "Srednje" to (2 to 2), // Srednje: tačno 2 bele figure
            "Teško" to (2 to 3)    // Teško: min 2, max 3 bele figure
        )
    }

    // Mapa koja definiše opseg broja POTEZA (crnih figura).
    // Za Teško, ovo sada predstavlja UKUPAN broj pešaka na tabli.
    val numberOfMovesRange = remember { // Dodato remember radi optimizacije
        mapOf(
            "Lako" to (5 to 6),  // Lako: 5-6 crnih figura
            "Srednje" to (3 to 4), // Srednje: 3-4 crne figure
            "Teško" to (12 to 13) // Teško: 12-13 crnih figura (UKUPNO)
        )
    }

    val currentRequiredMin = requiredFigures[selectedDifficulty]?.first ?: 1
    val currentRequiredMax = requiredFigures[selectedDifficulty]?.second ?: 1
    val currentMovesMin = numberOfMovesRange[selectedDifficulty]?.first ?: 1
    val currentMovesMax = numberOfMovesRange[selectedDifficulty]?.second ?: 1

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Prikaz imena igrača na ekranu za odabir figura
        Text("Ime igrača: $playerName", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Odaberite težinu:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Lako", "Srednje", "Teško").forEach { text ->
                Row(
                    Modifier
                        .selectable(
                            selected = (text == selectedDifficulty),
                            onClick = {
                                selectedDifficulty = text
                                // KLJUČNO: Resetuj odabrane figure pri promeni težine
                                selectedFigures.clear()
                            },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (text == selectedDifficulty),
                        onClick = null // null click behavior for row overall select
                    )
                    Text(text = text)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Odaberite figure:", style = MaterialTheme.typography.titleLarge)
        Text(
            text = when {
                currentRequiredMin == currentRequiredMax -> "Potrebno tačno $currentRequiredMin bele figure."
                else -> "Potrebno od $currentRequiredMin do $currentRequiredMax bele figure."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Text(
            text = "Biće postavljeno od $currentMovesMin do $currentMovesMax crnih figura.",
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
                                        if (selectedFigures.size < currentRequiredMax) {
                                            selectedFigures.add(pieceType)
                                        } else {
                                            Toast.makeText(context, "Već ste odabrali maksimalan broj figura za ovaj nivo ($currentRequiredMax)!", Toast.LENGTH_SHORT).show()
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

        // Provera da li je broj selektovanih figura validan za trenutnu težinu
        val canStartGame = selectedFigures.size in currentRequiredMin..currentRequiredMax && selectedFigures.isNotEmpty()

        Button(
            onClick = {
                if (canStartGame) {
                    val intent = Intent(context, GameActivity::class.java).apply {
                        putExtra("difficulty", selectedDifficulty)
                        putStringArrayListExtra("selectedFigures", ArrayList(selectedFigures.map { it.name }))
                        // Dodaj min/max poteze u Intent
                        putExtra("minMoves", currentMovesMin)
                        putExtra("maxMoves", currentMovesMax)
                        // KLJUČNO: Prosledi ime igrača u GameActivity
                        putExtra("playerName", playerName)
                    }
                    context.startActivity(intent)
                } else {
                    val msg = when {
                        currentRequiredMin == currentRequiredMax -> "Morate odabrati tačno $currentRequiredMin figuru/e!"
                        else -> "Morate odabrati od $currentRequiredMin do $currentRequiredMax figura!"
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