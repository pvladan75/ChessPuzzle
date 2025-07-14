package com.chess.chesspuzzle.modul1

import android.content.Context
import android.util.Log
import com.chess.chesspuzzle.PieceType
import com.chess.chesspuzzle.Square
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

object PuzzleLoader {

    private const val TAG = "PuzzleLoader" // Koristimo specifičniji TAG

    /**
     * Učitava listu šahovskih problema iz JSON fajla.
     * Novi format JSON-a sa detaljnim informacijama o zagonetki.
     *
     * @param context Context aplikacije, potreban za pristup assets folderu.
     * @param fileName Ime JSON fajla u assets folderu (npr. "puzzles.json").
     * @return Lista ChessProblem objekata, ili prazna lista ako dođe do greške.
     */
    fun loadPuzzlesFromJson(context: Context, fileName: String): List<ChessProblem> {
        val puzzles = mutableListOf<ChessProblem>()
        var jsonString: String? = null
        try {
            val inputStream: InputStream = context.assets.open(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            jsonString = String(buffer, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri čitanju JSON fajla '$fileName': ${e.message}", e)
            return emptyList()
        }

        if (jsonString == null) {
            Log.e(TAG, "JSON string je null nakon pokušaja učitavanja fajla '$fileName'")
            return emptyList()
        }

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val jsonObject: JSONObject = jsonArray.getJSONObject(i)

                // ID sada mora biti String, konvertujemo Int iz JSON-a u String
                val id = jsonObject.getInt("id").toString()
                val difficulty = jsonObject.getString("difficulty")
                val fen = jsonObject.getString("fen")

                // DOHVATI NOVE PARAMETRE SA DEFAULT VREDNOSTIMA AKO NE POSTOJE U STAROM JSON-U
                val puzzleName = jsonObject.optString("name", "Učitana Zagonetka") // Default ime ako nema u JSON-u
                val creationDate = jsonObject.optLong("creationDate", 0L) // Default datum (0L) ako nema u JSON-u

                // --- KLJUČNA LINIJA ZA DEBAGOVANJE ---
                // Proveravamo šta je učitano kao FEN string
                Log.d("FEN_LOAD_DEBUG", "Loaded puzzle ID $id, FEN: $fen")
                // --- KRAJ KLJUČNE LINIJE ---

                val solutionLength = jsonObject.getInt("solutionLength")
                val totalBlackCaptured = jsonObject.getInt("totalBlackCaptured")

                // Parsiranje whitePiecesConfig
                val whitePiecesConfigJson = jsonObject.getJSONObject("whitePiecesConfig")
                val whitePiecesConfig = mutableMapOf<PieceType, Int>()
                whitePiecesConfigJson.keys().forEach { key ->
                    try {
                        val pieceType = PieceType.valueOf(key.uppercase()) // Konvertuj string u enum
                        whitePiecesConfig[pieceType] = whitePiecesConfigJson.getInt(key)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Nepoznat PieceType u whitePiecesConfig za problem ID $id: $key")
                    }
                }

                // Parsiranje capturesByPiece
                val capturesByPieceJson = jsonObject.getJSONObject("capturesByPiece")
                val capturesByPiece = mutableMapOf<String, Int>()
                capturesByPieceJson.keys().forEach { key ->
                    capturesByPiece[key] = capturesByPieceJson.getInt(key)
                }

                // Parsiranje solutionMoves
                val jsonSolutionMoves = jsonObject.getJSONArray("solutionMoves")
                val solutionMoves = mutableListOf<SolutionMove>()
                for (j in 0 until jsonSolutionMoves.length()) {
                    val moveObject = jsonSolutionMoves.getJSONObject(j)
                    val moverPieceString = moveObject.getString("moverPiece")
                    val initialSquareString = moveObject.getString("initialSquare")
                    val moveUCI = moveObject.getString("moveUCI")

                    try {
                        val moverPiece = PieceType.valueOf(moverPieceString.uppercase())
                        // Konvertovanje stringa "e1" u Square('e', 1)
                        val initialSquare = Square(initialSquareString[0], initialSquareString[1].toString().toInt())
                        solutionMoves.add(SolutionMove(moverPiece, initialSquare, moveUCI))
                    } catch (e: Exception) {
                        Log.e(TAG, "Greška pri parsiranju SolutionMove za problem ID $id, potez ${moveUCI}: ${e.message}", e)
                        // Možeš odlučiti da preskočiš ovaj potez ili celu zagonetku ako želiš strože ponašanje
                        continue
                    }
                }

                // KLJUČNA IZMENA: Koristi imenovane argumente i dodaj name i creationDate
                puzzles.add(
                    ChessProblem(
                    id = id, // Sada je String
                    name = puzzleName, // Novo polje
                    difficulty = difficulty,
                    whitePiecesConfig = whitePiecesConfig,
                    fen = fen,
                    solutionLength = solutionLength,
                    totalBlackCaptured = totalBlackCaptured,
                    capturesByPiece = capturesByPiece,
                    solutionMoves = solutionMoves,
                    creationDate = creationDate // Novo polje
                )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri parsiranju JSON sadržaja iz fajla '$fileName': ${e.message}", e)
            return emptyList()
        }
        Log.d(TAG, "Uspešno učitano ${puzzles.size} zagonetki iz '$fileName'.")
        return puzzles
    }

    /**
     * Pomoćna funkcija za parsiranje UCI stringa "fromSquaretoSquare" u Pair<Square, Square>.
     * Npr. "e2e4" -> Pair(Square('e',2), Square('e',4))
     */
    fun parseUciToSquares(uci: String): Pair<Square, Square>? {
        if (uci.length != 4) {
            Log.e(TAG, "Nevažeći UCI string: $uci")
            return null
        }
        try {
            val fromFile = uci[0]
            val fromRank = uci[1].toString().toInt()
            val toFile = uci[2]
            val toRank = uci[3].toString().toInt()
            return Pair(Square(fromFile, fromRank), Square(toFile, toRank))
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri parsiranju UCI stringa '$uci': ${e.message}", e)
            return null
        }
    }
}