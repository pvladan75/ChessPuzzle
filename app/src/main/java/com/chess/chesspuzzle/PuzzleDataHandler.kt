package com.chess.chesspuzzle

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.io.FileNotFoundException
import java.util.UUID // Dodao import za UUID

// Singleton objekat za rukovanje JSON fajlovima korisničkih zagonetki
object PuzzleDataHandler {

    private const val TAG = "PuzzleDataHandler"
    // Inicijalizacija GSON-a sa prilagođenim SquareAdapterom
    // KLJUČNA PROMENA: Registracija SquareAdaptera
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Square::class.java, SquareAdapter())
        .setPrettyPrinting() // Opciono: za lepši JSON izlaz
        .create()

    /**
     * Učitava listu korisničkih šahovskih problema iz JSON fajla.
     * @param context Context aplikacije, potreban za pristup internom skladištu.
     * @param fileName Ime JSON fajla u internom skladištu (npr. "user_puzzles.json").
     * @return Lista ChessProblem objekata, ili prazna lista ako dođe do greške ili fajl ne postoji.
     */
    fun loadUserPuzzles(context: Context, fileName: String): List<ChessProblem> {
        return try {
            context.openFileInput(fileName).bufferedReader().use { reader ->
                val listType = object : TypeToken<List<ChessProblem>>() {}.type
                gson.fromJson(reader, listType) ?: emptyList()
            }
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "Fajl sa korisničkim zagonetkama '$fileName' nije pronađen. Vraća se prazna lista.")
            emptyList()
        } catch (e: IOException) {
            Log.e(TAG, "Greška pri čitanju korisničkih zagonetki iz '$fileName': ${e.message}", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Neočekivana greška pri učitavanju korisničkih zagonetki iz '$fileName': ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Čuva listu korisničkih šahovskih problema u JSON fajl.
     * @param context Context aplikacije, potreban za pristup internom skladištu.
     * @param fileName Ime JSON fajla u internom skladištu (npr. "user_puzzles.json").
     * @param puzzles Lista ChessProblem objekata za čuvanje.
     */
    fun saveUserPuzzles(context: Context, fileName: String, puzzles: List<ChessProblem>) {
        val jsonString = gson.toJson(puzzles)
        try {
            // MODE_PRIVATE osigurava da je fajl privatan za ovu aplikaciju
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { writer ->
                writer.write(jsonString.toByteArray())
            }
            Log.d(TAG, "Zagonetke uspešno sačuvane u '$fileName'")
        } catch (e: IOException) {
            Log.e(TAG, "Greška pri čuvanju korisničkih zagonetki u '$fileName': ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Neočekivana greška pri čuvanju korisničkih zagonetki u '$fileName': ${e.message}", e)
        }
    }

    /**
     * Dohvata listu naziva JSON fajlova sa zagonetkama u internom skladištu aplikacije.
     * @param context Context aplikacije.
     * @return Lista stringova koji predstavljaju nazive JSON fajlova.
     */
    fun getListOfUserPuzzleFiles(context: Context): List<String> {
        // ISPRAVKA: Uklonjene zagrade () iza fileList
        return context.fileList().filter { it.endsWith(".json") }.toList()
    }

    /**
     * Briše specifičan fajl sa zagonetkama iz internog skladišta aplikacije.
     * @param context Context aplikacije.
     * @param fileName Ime fajla za brisanje.
     * @return True ako je fajl uspešno obrisan, false inače.
     */
    fun deleteUserPuzzleFile(context: Context, fileName: String): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Fajl '$fileName' uspešno obrisan.")
                } else {
                    Log.e(TAG, "Greška pri brisanju fajla '$fileName'.")
                }
                deleted
            } else {
                Log.d(TAG, "Fajl '$fileName' ne postoji.")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Neočekivana greška pri brisanju fajla '$fileName': ${e.message}", e)
            false
        }
    }
}