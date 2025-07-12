package com.chess.chesspuzzle

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException

class SquareAdapter : TypeAdapter<Square>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Square?) {
        if (value == null) {
            out.nullValue()
        } else {
            // Konvertuj Square objekt u string, npr. "a1", "h8"
            out.value("${value.file}${value.rank}")
        }
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): Square? {
        // Konvertuj string (npr. "a1") nazad u Square objekt
        reader.nextString()?.let { squareString ->
            if (squareString.length == 2) {
                val fileChar = squareString[0]
                val rankInt = squareString[1].digitToIntOrNull()
                if (rankInt != null) {
                    return Square(fileChar, rankInt)
                }
            }
        }
        return null // Vrati null za nevalidan format
    }
}