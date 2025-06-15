package com.chess.chesspuzzle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chess.chesspuzzle.ui.theme.ChessPuzzleTheme

class DifficultySelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessPuzzleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DifficultySelectionScreen()
                }
            }
        }
    }
}

@Composable
fun DifficultySelectionScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Odaberi Težinu",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Dugme za težinu "Lako".
        Button(
            onClick = {
                // Sada pokrećemo FigureSelectionActivity i prosleđujemo odabranu težinu
                val intent = Intent(context, FigureSelectionActivity::class.java)
                intent.putExtra("difficulty", "Lako")
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(60.dp)
        ) {
            Text("Lako", fontSize = 20.sp)
        }

        // Dugme za težinu "Srednje".
        Button(
            onClick = {
                // Pokrećemo FigureSelectionActivity i prosleđujemo odabranu težinu
                val intent = Intent(context, FigureSelectionActivity::class.java)
                intent.putExtra("difficulty", "Srednje")
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(60.dp)
        ) {
            Text("Srednje", fontSize = 20.sp)
        }

        // Dugme za težinu "Teško".
        Button(
            onClick = {
                // Pokrećemo FigureSelectionActivity i prosleđujemo odabranu težinu
                val intent = Intent(context, FigureSelectionActivity::class.java)
                intent.putExtra("difficulty", "Teško")
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text("Teško", fontSize = 20.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DifficultySelectionPreview() {
    ChessPuzzleTheme {
        DifficultySelectionScreen()
    }
}