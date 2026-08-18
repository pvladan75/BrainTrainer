package com.program.braintrainer.chess.model.data

import com.program.braintrainer.chess.model.Problem
import kotlinx.serialization.json.Json
import java.io.InputStream
import kotlin.random.Random

/**
 * Bira nasumičan uzorak zagonetki iz JSONL toka — jedna minifikovana zagonetka
 * po redu.
 *
 * Rezervoarsko uzorkovanje: kroz tok se prolazi tačno jednom, u memoriji ostaje
 * najviše `count` redova, a JSON se parsira tek za odabrane. Veličina fajla
 * zato utiče samo na čitanje bajtova — od 5.800 zagonetki parsira se deset.
 */
internal object ProblemSampler {

    private const val BUFFER_SIZE = 32 * 1024

    private val json = Json { ignoreUnknownKeys = true }

    fun sample(input: InputStream, count: Int, random: Random = Random.Default): List<Problem> {
        if (count <= 0) return emptyList()

        val reservoir = ArrayList<String>(count)
        var seen = 0

        input.reader().buffered(BUFFER_SIZE).forEachLine { line ->
            if (line.isEmpty()) return@forEachLine

            if (reservoir.size < count) {
                reservoir.add(line)
            } else {
                // Red broj `seen` (0-indeksirano) ulazi u uzorak sa verovatnoćom
                // count / (seen + 1) i izbacuje nasumično odabranog člana.
                val slot = random.nextInt(seen + 1)
                if (slot < count) reservoir[slot] = line
            }
            seen++
        }

        // Prvih `count` redova ostaje na svojim mestima dok ih neko ne zameni,
        // pa se redosled meša — ali samo za odabrane, ne za ceo fajl.
        return reservoir.mapNotNull { parseOrNull(it) }.shuffled(random)
    }

    private fun parseOrNull(line: String): Problem? = try {
        json.decodeFromString<Problem>(line)
    } catch (exception: Exception) {
        println("ERROR: Could not parse puzzle line. ${exception.message}")
        null
    }
}
