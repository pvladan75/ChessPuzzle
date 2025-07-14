package com.chess.chesspuzzle

import android.os.Bundle
import android.util.Log // Dodat import za Logcat
import android.widget.Toast // Dodat import za Toast poruke
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner // Dodat import za LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner // Dodat import za LocalLifecycleOwner

// Importi za rad sa datumom i vremenom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.ButtonDefaults // Dodat import za ButtonDefaults
import androidx.compose.ui.text.font.FontStyle.Companion.Italic // Dodat import za FontStyle.Italic

/**
 * Aktivnost za prikaz najboljih rezultata (High Scores).
 * Ova aktivnost prikazuje listu rezultata sortiranih po skoru,
 * omogućavajući korisniku da filtrira po težini.
 */
class HighScoresActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ključno: Inicijalizuj ScoreManager ovde, za svaki slučaj, iako je već u MainActivity.
        // Ovo osigurava da je ScoreManager spreman pre nego što se pokušaju dohvatiti skorovi.
        ScoreManager.init(applicationContext)

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
    val lifecycleOwner = LocalLifecycleOwner.current // Dohvata LifecycleOwner

    // Stanje za trenutno odabranu težinu, podrazumevano "Lako"
    // Koristimo Difficulty.EASY.name za doslednost sa ScoreManager ključevima
    var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY.name) }

    // Liste za čuvanje rezultata po težini, koristeći mutableStateOf za reaktivnost
    var easyScores by remember { mutableStateOf(emptyList<ScoreEntry>()) }
    var mediumScores by remember { mutableStateOf(emptyList<ScoreEntry>()) }
    var hardScores by remember { mutableStateOf(emptyList<ScoreEntry>()) }

    // Pomoćna funkcija za učitavanje svih rezultata
    val loadAllScores: () -> Unit = {
        easyScores = ScoreManager.getHighScores(Difficulty.EASY.name)
        mediumScores = ScoreManager.getHighScores(Difficulty.MEDIUM.name)
        hardScores = ScoreManager.getHighScores(Difficulty.HARD.name)
        Log.d("HighScoresScreen", "Scores reloaded from ScoreManager.")
    }

    // KLJUČNA IZMENA: Koristimo DisposableEffect i LifecycleEventObserver
    // da bismo osigurali da se skorovi učitavaju svaki put kada se aktivnost (ponovo) pokrene (onResume).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { // Učitaj skorove svaki put kada se aktivnost nastavi
                loadAllScores()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            Difficulty.entries.forEach { difficulty -> // Iteriramo kroz Difficulty enum
                Button(
                    onClick = { selectedDifficulty = difficulty.name }, // Postavljamo name enum vrednosti
                    modifier = Modifier
                        .weight(1f) // Svako dugme zauzima jednak prostor
                        .padding(horizontal = 4.dp),
                    // Onemogućava klik na dugme ako je ta težina već odabrana
                    enabled = selectedDifficulty != difficulty.name
                ) {
                    Text(difficulty.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) // Prikaz lepšeg imena
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
        // Sada koristimo reaktivne promenljive stanja
        val scoresToDisplay = when (selectedDifficulty) {
            Difficulty.EASY.name -> easyScores
            Difficulty.MEDIUM.name -> mediumScores
            Difficulty.HARD.name -> hardScores
            else -> emptyList() // Trebalo bi uvek da se poklopi
        }

        if (scoresToDisplay.isEmpty()) {
            // Poruka ako nema sačuvanih rezultata za trenutnu težinu
            Text(
                text = "Nema sačuvanih rezultata za ovu težinu.",
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = Italic // Dodao stil za italic tekst
            )
        } else {
            // Prikaz rezultata u LazyColumn-u (efikasno za duge liste)
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) { // Dodao heightIn
                // Prikazujemo samo top MAX_HIGH_SCORES rezultata
                items(scoresToDisplay.take(ScoreManager.MAX_HIGH_SCORES)) { scoreEntry ->
                    // Svaki rezultat se prikazuje koristeći ScoreItem Composable
                    ScoreItem(scoreEntry = scoreEntry, rank = scoresToDisplay.indexOf(scoreEntry) + 1)
                }
            }
        }

        // Spacer koji gura sledeće dugme na dno ekrana
        Spacer(modifier = Modifier.weight(1f))

        // POČETAK KODA ZA DUGME ZA BRISANJE - IZVUČENO IZ BLOKA I INTEGRISANO**
        // Dugme za brisanje svih rezultata
        Button(
            onClick = {
                ScoreManager.clearScores(context)
                // Ažuriraj sva stanja lista rezultata na prazne nakon brisanja
                easyScores = emptyList()
                mediumScores = emptyList()
                hardScores = emptyList()
                Toast.makeText(context, "Svi rezultati obrisani!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Obriši Sve Rezultate")
        }
        // KRAJ KODA ZA DUGME ZA BRISANJE**

        Spacer(modifier = Modifier.height(8.dp))

        // Dugme za povratak na glavni meni
        Button(
            onClick = {
                // Zatvara trenutnu aktivnost i vraća korisnika na prethodnu (MainActivity)
                (context as? ComponentActivity)?.finish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp) // Povećao padding za vizuelni razmak
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
            .padding(vertical = 4.dp) // Smanjen vertikalni padding za kompaktniji izgled
            .background(MaterialTheme.colorScheme.surfaceVariant) // Pozadina reda rezultata
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank.",
            modifier = Modifier.weight(0.5f),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = scoreEntry.playerName,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${scoreEntry.score}",
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Prikaz datuma i vremena, formatiranog iz timestamp-a
        Text(
            text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(scoreEntry.timestamp)),
            modifier = Modifier.weight(1f),
            fontSize = 13.sp, // Manji font za datum radi bolje čitljivosti
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}