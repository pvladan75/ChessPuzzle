package com.chess.chesspuzzle

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
// Uklonjen import Date jer se ScoreEntry ne deklariše ovde

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
     * @param difficulty String koji predstavlja nivo težine (npr. "Lako", "Srednje", "Teško").
     */
    fun addScore(newScoreEntry: ScoreEntry, difficulty: String) {
        // Provera da li je sharedPreferences inicijalizovan
        if (!::sharedPreferences.isInitialized) {
            Log.e("ScoreManager", "ScoreManager nije inicijalizovan! Pozovite ScoreManager.init(context) pre upotrebe.")
            return
        }

        val key = "${difficulty}_high_scores" // Ključ za SharedPreferences za ovu težinu
        val json = sharedPreferences.getString(key, null)
        val type = object : TypeToken<MutableList<ScoreEntry>>() {}.type

        // Dohvati trenutne rezultate ili kreiraj novu praznu listu
        val currentScores: MutableList<ScoreEntry> = if (json != null) {
            gson.fromJson(json, type) ?: mutableListOf() // Dodato ?: mutableListOf() za sigurnost
        } else {
            mutableListOf()
        }

        currentScores.add(newScoreEntry)
        currentScores.sortDescending() // Koristi Comparable definisan u ScoreEntry

        // Zadrži samo MAX_HIGH_SCORES najboljih rezultata
        val updatedScores = currentScores.take(MAX_HIGH_SCORES)

        // Sačuvaj ažuriranu listu nazad u SharedPreferences
        val updatedJson = gson.toJson(updatedScores)
        sharedPreferences.edit().putString(key, updatedJson).apply()
        Log.d("ScoreManager", "Score added for $difficulty: $newScoreEntry. Current scores: $updatedScores")
    }

    /**
     * Dohvata listu najboljih rezultata za određeni nivo težine.
     * Rezultati su već sortirani opadajuće po skoru (zbog Comparable implementacije).
     * @param difficulty String koji predstavlja nivo težine.
     * @return List objekata [ScoreEntry] za dati nivo težine.
     */
    fun getHighScores(difficulty: String): List<ScoreEntry> {
        // Provera da li je sharedPreferences inicijalizovan
        if (!::sharedPreferences.isInitialized) {
            Log.e("ScoreManager", "ScoreManager nije inicijalizovan! Pozovite ScoreManager.init(context) pre upotrebe.")
            return emptyList()
        }

        val key = "${difficulty}_high_scores"
        val json = sharedPreferences.getString(key, null)
        val type = object : TypeToken<List<ScoreEntry>>() {}.type // Koristi List umesto MutableList za povratni tip

        // Vrati listu rezultata ili praznu listu
        return if (json != null) {
            gson.fromJson(json, type) ?: emptyList() // Dodato ?: emptyList() za sigurnost
        } else {
            emptyList()
        }
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
        //sharedPreferences.edit().clear().apply() // Ovo briše SVE, uključujući ostale preference
        // Bolje je brisati samo high scores ključeve ako ih ima više
        val editor = sharedPreferences.edit()
        val difficulties = listOf("Lako", "Srednje", "Teško")
        difficulties.forEach { difficulty ->
            editor.remove("${difficulty}_high_scores")
        }
        editor.apply()
        Log.d("ScoreManager", "All ChessPuzzleHighScores cleared.")
    }
}