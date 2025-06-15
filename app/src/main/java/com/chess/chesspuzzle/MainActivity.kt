package com.chess.chesspuzzle

import android.content.Intent
import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inicijalizacija ScoreManagera sa kontekstom aplikacije
        ScoreManager.init(applicationContext) // <-- ISPRAVLJENO: Koristi applicationContext

        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainMenu()
                }
            }
        }
    }
}

@Composable
fun MainMenu() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Dobrodošli u Chess Puzzle!", style = MaterialTheme.typography.headlineLarge)
        // Razmak
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Button(
                onClick = {
                    // Direktan skok na FigureSelectionActivity za Modul 1
                    val intent = Intent(context, FigureSelectionActivity::class.java)
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

            // --- DODATO NOVO DUGME ZA NAJBOLJE REZULTATE ---
            Button(
                onClick = {
                    val intent = Intent(context, HighScoresActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text("Najbolji Rezultati")
            }
            // --- KRAJ DODATOG DUGMETA ---
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    ChessPuzzleTheme {
        MainMenu()
    }
}