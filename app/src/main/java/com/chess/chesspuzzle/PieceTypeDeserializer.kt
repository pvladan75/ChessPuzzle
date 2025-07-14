package com.chess.chesspuzzle

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

// Potreban import za PieceType ako nije u istom paketu
// import com.chess.chesspuzzle.PieceType // Ako je PieceType u drugom paketu

class PieceTypeDeserializer : JsonDeserializer<PieceType> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PieceType {
        val pieceName = json?.asString
        return try {
            // Pokušaj da konvertuješ string u PieceType enum (ignorišući velika/mala slova)
            // Uverite se da se imena u JSON-u poklapaju sa imenima enum konstanti (npr. "Queen" -> QUEEN)
            PieceType.valueOf(pieceName!!.uppercase())
        } catch (e: IllegalArgumentException) {
            // Ako dođe do greške (npr. nepoznato ime figure), vrati NONE ili baci izuzetak
            PieceType.NONE
        }
    }
}