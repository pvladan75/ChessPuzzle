// MainActivity.kt - Ažurirana verzija
package com.chess.chesspuzzle

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme

// Dodatni importi za dijalog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.chess.chesspuzzle.modul1.FigureSelectionActivity
import com.chess.chesspuzzle.modul1.HighScoresActivity
import com.chess.chesspuzzle.modul1.SolutionDisplayActivity
import com.chess.chesspuzzle.modul2.Modul2GameActivity // <--- NOVI IMPORT!

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "Initializing SoundPool from MainActivity onCreate...")

        SoundManager.initialize(applicationContext) // <--- IZMENJENO OVDE
        ScoreManager.init(applicationContext)

        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showNameInputDialog by remember { mutableStateOf(true) }
                    var playerName by remember { mutableStateOf("Anonimni") }

                    if (showNameInputDialog) {
                        PlayerNameInputDialog(
                            onNameEntered = { name ->
                                playerName = name
                                showNameInputDialog = false
                                Log.d("MainActivity", "Player name entered: $playerName")
                            },
                            onDismiss = {
                                playerName = "Anonimni"
                                showNameInputDialog = false
                                Log.d("MainActivity", "Player name dialog dismissed. Using default: $playerName")
                            }
                        )
                    } else {
                        MainMenu(playerName = playerName)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "Releasing SoundPool from MainActivity onDestroy...")
        // Oslobodi SoundManager resurse kada se aktivnost uništi
        SoundManager.release() // <--- IZMENJENO OVDE
    }
}

// Ostatak MainActivity.kt fajla ostaje isti (MainMenu, PlayerNameInputDialog, Preview)
// ... (tvoj postojeći kod za MainMenu, PlayerNameInputDialog, Preview)
// Nastavi odavde:
// Glavni meni aplikacije - sada prima ime igrača
@Composable
fun MainMenu(playerName: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Dobrodošli, $playerName!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Button(
                onClick = {
                    val intent = Intent(context, FigureSelectionActivity::class.java)
                    intent.putExtra("playerName", playerName)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Modul 1: Uništi sve figure protivnika")
            }
            Button(
                // <--- IZMENA OVDE! Sada pokrećemo Modul2GameActivity
                onClick = {
                    val intent = Intent(context, Modul2GameActivity::class.java)
                    intent.putExtra("playerName", playerName) // Prosleđujemo ime igrača i Modulu 2
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = true // <--- OMOGUĆILI SMO DUGME
            ) {
                Text("Modul 2: Pomoć pri učenju")
            }
            Button(
                onClick = { /* Implementiraj logiku za Modul 3 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                enabled = false
            ) {
                Text("Modul 3: Rešavanje zagonetki (Kompleksnije)")
            }

            Button(
                onClick = {
                    val intent = Intent(context, HighScoresActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Najbolji Rezultati")
            }

            Button(
                onClick = {
                    val intent = Intent(context, SolutionDisplayActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Prikaži Rešenja Zagonetki (TEST)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerNameInputDialog(onNameEntered: (String) -> Unit, onDismiss: () -> Unit) {
    var nameInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unesite vaše ime") },
        text = {
            TextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Ime") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (nameInput.isNotBlank()) {
                        onNameEntered(nameInput)
                    } else {
                        onDismiss()
                    }
                })
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onNameEntered(nameInput)
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Potvrdi")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Otkazi")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    ChessPuzzleTheme {
        MainMenu(playerName = "Anonimni (Preview)")
    }
}