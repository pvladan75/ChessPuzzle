// Modul2GameActivity.kt
package com.chess.chesspuzzle.modul2

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.R // Za resurse slika
import com.chess.chesspuzzle.SoundManager
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme
import java.util.Locale

import com.chess.chesspuzzle.Difficulty

// Ponovo definišemo Difficulty ili ga importujemo ako je već globalno definisan u projektu
// Ako je Difficulty definisan u zajedničkom fajlu (npr. ChessDefinitions.kt ili slično),
// onda ga samo importujte:
// import com.chess.chesspuzzle.Difficulty

class Modul2GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playerName = intent.getStringExtra("playerName") ?: "Anonimni"
        Log.d("Modul2GameActivity", "Modul 2 Game Activity created for player: $playerName")

        SoundManager.initialize(applicationContext)

        setContent {
            ChessPuzzleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Modul2SelectionScreen(playerName = playerName)
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
fun Modul2SelectionScreen(playerName: String) {
    val context = LocalContext.current
    var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY) }

    val opponentPieceTypes = remember {
        mapOf(
            Difficulty.EASY to listOf(PieceType.PAWN),
            Difficulty.MEDIUM to listOf(PieceType.PAWN, PieceType.BISHOP, PieceType.KNIGHT),
            Difficulty.HARD to listOf(PieceType.PAWN, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK) // Bez kraljice, fokus na queen vs. pieces
        )
    }

    val numberOfOpponentPiecesRange = remember {
        mapOf(
            Difficulty.EASY to (3 to 5),  // 3-5 pešaka
            Difficulty.MEDIUM to (4 to 7), // 4-7 kombinovanih figura
            Difficulty.HARD to (6 to 9)   // 6-9 kombinovanih figura
        )
    }

    val currentMinOpponents = numberOfOpponentPiecesRange[selectedDifficulty]?.first ?: 3
    val currentMaxOpponents = numberOfOpponentPiecesRange[selectedDifficulty]?.second ?: 5


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Ime igrača: $playerName", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Modul 2: Pomoć pri učenju", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Uništi sve crne figure koristeći samo belu damu. Dama se respawnuje ako je napadnuta.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // --- Odabir težine ---
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
                            onClick = { selectedDifficulty = difficulty },
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

        // Prikaz koje će se crne figure pojaviti
        Text("Crne figure koje će se pojaviti:", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Biće postavljeno od $currentMinOpponents do $currentMaxOpponents figura.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))

        val currentOpponentTypes = opponentPieceTypes[selectedDifficulty] ?: emptyList()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            currentOpponentTypes.forEach { pieceType ->
                val drawableResId = when (pieceType) {
                    PieceType.PAWN -> R.drawable.bp
                    PieceType.KNIGHT -> R.drawable.bn
                    PieceType.BISHOP -> R.drawable.bb
                    PieceType.ROOK -> R.drawable.br
                    PieceType.QUEEN -> R.drawable.bq
                    else -> 0
                }
                if (drawableResId != 0) {
                    Image(
                        painter = painterResource(id = drawableResId),
                        contentDescription = pieceType.name,
                        modifier = Modifier
                            .size(60.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // Pokreni Modul2GameScreenActivity
                val intent = Intent(context, Modul2GameScreenActivity::class.java).apply {
                    putExtra("difficulty", selectedDifficulty.name)
                    putExtra("playerName", playerName)
                    // Prosleđujemo i listu figura koje se mogu pojaviti kao protivnici
                    // i opseg broja tih figura. Modul2GameScreenActivity će to koristiti za generisanje table.
                    val opponentTypeNames = ArrayList(currentOpponentTypes.map { it.name })
                    putStringArrayListExtra("modul2OpponentTypes", opponentTypeNames)
                    putExtra("modul2MinOpponents", currentMinOpponents)
                    putExtra("modul2MaxOpponents", currentMaxOpponents)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pokreni Modul 2 Igru")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Modul2SelectionPreview() {
    ChessPuzzleTheme {
        Modul2SelectionScreen(playerName = "Preview Igrač")
    }
}