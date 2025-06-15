package com.chess.chesspuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme

// Potrebni Compose importi za UI elemente i layout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Importi za rad sa datumom i vremenom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Aktivnost za prikaz najboljih rezultata (High Scores).
 * Ova aktivnost prikazuje listu rezultata sortiranih po skoru,
 * omogućavajući korisniku da filtrira po težini.
 */
class HighScoresActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HighScoresScreen() // Prikazujemo glavni Composable za rezultate
                }
            }
        }
    }
}

/**
 * Composable funkcija koja prikazuje ekran sa najboljim rezultatima.
 * Uključuje dugmad za odabir težine, zaglavlje tabele i listu rezultata.
 */
@Composable
fun HighScoresScreen() {
    val context = LocalContext.current // Potrebno za poziv finish()
    // Stanje za trenutno odabranu težinu, podrazumevano "Lako"
    var selectedDifficulty by remember { mutableStateOf("Lako") }

    // Lista dostupnih težina za filtriranje
    val difficulties = listOf("Lako", "Srednje", "Teško")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally // Centriranje sadržaja horizontalno
    ) {
        // Naslov ekrana
        Text(
            text = "Najbolji Rezultati",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Dugmad za odabir težine (Lako, Srednje, Teško)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround // Raspoređivanje dugmadi ravnomerno
        ) {
            difficulties.forEach { difficulty ->
                Button(
                    onClick = { selectedDifficulty = difficulty },
                    modifier = Modifier
                        .weight(1f) // Svako dugme zauzima jednak prostor
                        .padding(horizontal = 4.dp),
                    // Onemogućava klik na dugme ako je ta težina već odabrana
                    enabled = selectedDifficulty != difficulty
                ) {
                    Text(difficulty)
                }
            }
        }

        // Zaglavlje tabele sa kolonama: Rang, Ime, Skor, Datum
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer) // Pozadina zaglavlja
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rang",
                modifier = Modifier.weight(0.5f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Ime",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Skor",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Datum",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Dohvatanje i prikaz liste rezultata za odabranu težinu
        // Rezultati se dohvataju iz ScoreManager-a i sortiraju opadajuće po skoru
        val scores = ScoreManager.getHighScores(selectedDifficulty) // <-- ISPRAVLJENO: Nema context parametra

        if (scores.isEmpty()) {
            // Poruka ako nema sačuvanih rezultata za trenutnu težinu
            Text(
                text = "Nema sačuvanih rezultata za ovu težinu.",
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Prikaz rezultata u LazyColumn-u (efikasno za duge liste)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // Prikazujemo samo top MAX_HIGH_SCORES rezultata
                items(scores.take(ScoreManager.MAX_HIGH_SCORES)) { scoreEntry ->
                    // Svaki rezultat se prikazuje koristeći ScoreItem Composable
                    ScoreItem(scoreEntry = scoreEntry, rank = scores.indexOf(scoreEntry) + 1)
                }
            }
        }

        // Spacer koji gura sledeće dugme na dno ekrana
        Spacer(modifier = Modifier.weight(1f))

        // Dugme za povratak na glavni meni
        Button(
            onClick = {
                // Zatvara trenutnu aktivnost i vraća korisnika na prethodnu (MainActivity)
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Nazad na glavni meni")
        }
    }
}

/**
 * Composable funkcija koja prikazuje jedan red rezultata u tabeli.
 *
 * @param scoreEntry Objekat [ScoreEntry] koji sadrži podatke o rezultatu.
 * @param rank Rang (pozicija) rezultata na listi.
 */
@Composable
fun ScoreItem(scoreEntry: ScoreEntry, rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant) // Pozadina reda rezultata
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank.",
            modifier = Modifier.weight(0.5f),
            fontSize = 15.sp
        )
        Text(
            text = scoreEntry.playerName,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp
        )
        Text(
            text = "${scoreEntry.score}",
            modifier = Modifier.weight(1f),
            fontSize = 15.sp
        )
        // Prikaz datuma i vremena, formatiranog iz timestamp-a
        Text(
            text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(scoreEntry.timestamp)),
            modifier = Modifier.weight(1f),
            fontSize = 13.sp // Manji font za datum radi bolje čitljivosti
        )
    }
}