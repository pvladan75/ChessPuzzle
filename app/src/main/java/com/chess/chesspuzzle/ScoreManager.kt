package com.chess.chesspuzzle

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
// Uklonjen import Date jer se ScoreEntry ne deklariše ovde (to je pretpostavka, ako ga imaš drugde, ostavi ga)

/**
 * Predstavlja jedan rezultat u tabeli sa rezultatima.
 * Implementira Comparable interfejs kako bi se omogućilo sortiranje po skoru (opadajuće).
 * @property playerName Ime igrača.
 * @property score Postignuti skor.
 * @property timestamp Vreme kada je rezultat postignut, u milisekundama.
 */



/**
 * Singleton objekat za upravljanje visokim skorovima (high scores) koristeći SharedPreferences.
 * Omogućava dodavanje, dohvatanje i brisanje rezultata za različite nivoe težine.
 */
object ScoreManager {

    private const val PREFS_NAME = "ChessPuzzleScores" // Naziv SharedPreferences fajla
    const val MAX_HIGH_SCORES = 10 // Maksimalan broj rezultata koji se čuva za svaku težinu (javno dostupan)

    private lateinit var sharedPreferences: SharedPreferences // Inicijalizuje se u init metodi
    private val gson = Gson() // Gson instanca za JSON konverzije

    /**
     * Inicijalizuje ScoreManager. Ova metoda mora biti pozvana jednom na početku aplikacije (npr. u MainActivity).
     * @param context Context aplikacije.
     */
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d("ScoreManager", "ScoreManager initialized.")
    }

    /**
     * Dodaje novi rezultat za određeni nivo težine.
     * Rezultati se čuvaju sortirani opadajuće po skoru, i lista se ograničava na MAX_HIGH_SCORES.
     * @param newScoreEntry Objekat [ScoreEntry] koji se dodaje.
     * @param difficulty String koji predstavlja nivo težine (npr. "EASY", "MEDIUM", "HARD").
     */
    fun addScore(newScoreEntry: ScoreEntry, difficulty: String) {
        // Provera da li je sharedPreferences inicijalizovan
        if (!::sharedPreferences.isInitialized) {
            Log.e("ScoreManager", "ScoreManager nije inicijalizovan! Pozovite ScoreManager.init(context) pre upotrebe.")
            return
        }

        val key = "${difficulty.uppercase()}_high_scores" // Koristi uppercase za doslednost ključeva
        val json = sharedPreferences.getString(key, null)
        val type = object : TypeToken<MutableList<ScoreEntry>>() {}.type

        // Dohvati trenutne rezultate ili kreiraj novu praznu listu
        val currentScores: MutableList<ScoreEntry> = if (json != null) {
            gson.fromJson(json, type) ?: mutableListOf()
        } else {
            mutableListOf()
        }

        currentScores.add(newScoreEntry)
        currentScores.sortDescending() // Koristi Comparable definisan u ScoreEntry za sortiranje

        // Zadrži samo MAX_HIGH_SCORES najboljih rezultata
        val updatedScores = currentScores.take(MAX_HIGH_SCORES)

        // Sačuvaj ažuriranu listu nazad u SharedPreferences
        val updatedJson = gson.toJson(updatedScores)
        sharedPreferences.edit().putString(key, updatedJson).apply()
        Log.d("ScoreManager", "Score added for $difficulty: $newScoreEntry. Updated top scores: $updatedScores")
    }

    /**
     * Dohvata listu najboljih rezultata za određeni nivo težine.
     * Rezultati su garantovano sortirani opadajuće po skoru.
     * @param difficulty String koji predstavlja nivo težine.
     * @return List objekata [ScoreEntry] za dati nivo težine, sortirana opadajuće po skoru.
     */
    fun getHighScores(difficulty: String): List<ScoreEntry> {
        // Provera da li je sharedPreferences inicijalizovan
        if (!::sharedPreferences.isInitialized) {
            Log.e("ScoreManager", "ScoreManager nije inicijalizovan! Pozovite ScoreManager.init(context) pre upotrebe.")
            return emptyList()
        }

        val key = "${difficulty.uppercase()}_high_scores" // Koristi uppercase za doslednost ključeva
        val json = sharedPreferences.getString(key, null)
        val type = object : TypeToken<List<ScoreEntry>>() {}.type

        // Dohvati listu rezultata ili praznu listu
        val scores: List<ScoreEntry> = if (json != null) {
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }

        // KLJUČNA IZMENA: Osiguraj da je lista sortirana pre vraćanja
        return scores.sortedByDescending { it.score }
    }

    /**
     * Briše sve sačuvane rezultate iz SharedPreferences.
     * @param context Context aplikacije.
     */
    fun clearScores(context: Context) {
        if (!::sharedPreferences.isInitialized) {
            Log.e("ScoreManager", "ScoreManager nije inicijalizovan! Pozovite ScoreManager.init(context) pre upotrebe.")
            return
        }
        val editor = sharedPreferences.edit()
        // Pretpostavka je da koristite Difficulty.name (EASY, MEDIUM, HARD) kao string
        val difficulties = Difficulty.entries.map { it.name } // Dohvati sve nazive iz Difficulty enuma
        difficulties.forEach { difficulty ->
            editor.remove("${difficulty.uppercase()}_high_scores")
        }
        editor.apply()
        Log.d("ScoreManager", "All ChessPuzzleHighScores cleared.")
    }
}