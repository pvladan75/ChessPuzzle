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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "Initializing SoundPool from MainActivity onCreate...")
        PuzzleGenerator.initializeSoundPool(applicationContext) // Inicijalizuj SoundPool
        ScoreManager.init(applicationContext) // Inicijalizacija ScoreManagera sa kontekstom aplikacije

        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Stanja za dijalog i ime igrača
                    // 'true' po defaultu, što znači da će se dijalog prikazati odmah
                    var showNameInputDialog by remember { mutableStateOf(true) }
                    // Ime igrača će biti "Anonimni" dok se ne unese
                    var playerName by remember { mutableStateOf("Anonimni") }

                    // Uslovno prikazivanje dijaloga ili glavnog menija
                    if (showNameInputDialog) {
                        PlayerNameInputDialog(
                            onNameEntered = { name ->
                                playerName = name
                                showNameInputDialog = false // Sakrij dijalog nakon unosa imena
                            },
                            onDismiss = {
                                playerName = "Anonimni" // Koristi default ako se dijalog odbije
                                showNameInputDialog = false // Sakrij dijalog
                            }
                        )
                    } else {
                        // Prikaz glavnog menija nakon unosa imena
                        // Prosleđujemo 'playerName' na MainMenu Composable
                        MainMenu(playerName = playerName)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "Releasing SoundPool from MainActivity onDestroy...")
        PuzzleGenerator.releaseSoundPool() // Oslobodi SoundPool resurse
    }
}

// Glavni meni aplikacije - sada prima ime igrača
@Composable
fun MainMenu(playerName: String) { // Dodat playerName kao parametar
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Prikaz imena igrača na glavnom meniju
        Text(text = "Dobrodošli, $playerName!", style = MaterialTheme.typography.headlineLarge)
        // Razmak
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Button(
                onClick = {
                    val intent = Intent(context, FigureSelectionActivity::class.java)
                    // KLJUČNO: Prosledi ime igrača u FigureSelectionActivity
                    intent.putExtra("playerName", playerName)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text("Modul 1: Uništi sve figure protivnika")
            }
            Button(
                onClick = { /* Implementiraj logiku za Modul 2 */ },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = false // Onemogućeno dok se ne implementira
            ) {
                Text("Modul 2: Pomoć pri učenju")
            }
            Button(
                onClick = { /* Implementiraj logiku za Modul 3 */ },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = false // Onemogućeno dok se ne implementira
            ) {
                Text("Modul 3: Rešavanje zagonetki (Kompleksnije)")
            }

            Button(
                onClick = {
                    val intent = Intent(context, HighScoresActivity::class.java)
                    // Opciono: Prosledi ime igrača i u HighScoresActivity ako je potrebno
                    // intent.putExtra("playerName", playerName)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text("Najbolji Rezultati")
            }
        }
    }
}

// Funkcija za prikaz dijaloga za unos imena igrača
// Označena sa @OptIn(ExperimentalMaterial3Api::class) jer koristi TextField unutar AlertDialog-a
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerNameInputDialog(onNameEntered: (String) -> Unit, onDismiss: () -> Unit) {
    var nameInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, // Šta se dešava ako korisnik klikne van dijaloga ili pritisne back
        title = { Text("Unesite vaše ime") },
        text = {
            TextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Ime") },
                singleLine = true, // Samo jedan red teksta
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done), // Tastatura će imati "Done" dugme
                keyboardActions = KeyboardActions(onDone = {
                    if (nameInput.isNotBlank()) {
                        onNameEntered(nameInput) // Pozovi callback ako je ime uneto
                    } else {
                        onDismiss() // Otkazi ako je prazno
                    }
                })
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        onNameEntered(nameInput) // Pozovi callback ako je ime uneto
                    } else {
                        onDismiss() // Otkazi ako je prazno
                    }
                }
            ) {
                Text("Potvrdi")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { // Dugme za otkazivanje
                Text("Otkazi")
            }
        }
    )
}


// Preview za MainActivity
@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    ChessPuzzleTheme {
        // U preview-u, simuliraćemo da je dijalog već zatvoren da bismo videli glavni meni
        // i da je ime "Anonimni (Preview)"
        MainMenu(playerName = "Anonimni (Preview)")
    }
}