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
    private const val QUOTE = '"'

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

    /**
     * Vraća zagonetke sa zadatim ID-jevima, redosledom kojim su traženi.
     * Parsira samo pogođene redove — koristi se pri obnavljanju sesije posle
     * ubijanja procesa.
     */
    fun selectByIds(input: InputStream, ids: Collection<String>): List<Problem> {
        if (ids.isEmpty()) return emptyList()

        val wanted = ids.toHashSet()
        val found = HashMap<String, Problem>(wanted.size)

        input.reader().buffered(BUFFER_SIZE).forEachLine { line ->
            if (found.size == wanted.size) return@forEachLine
            val id = idOf(line) ?: return@forEachLine
            if (id in wanted && id !in found) {
                parseOrNull(line)?.let { found[id] = it }
            }
        }

        return ids.mapNotNull { found[it] }
    }

    /**
     * ID reda bez parsiranja JSON-a: `id` je prvo polje, pa je njegova vrednost
     * između trećeg i četvrtog navodnika u redu.
     */
    private fun idOf(line: String): String? {
        var quotesSeen = 0
        var valueStart = -1

        for (index in line.indices) {
            if (line[index] != QUOTE) continue
            quotesSeen++
            if (quotesSeen == 3) valueStart = index + 1
            if (quotesSeen == 4) return line.substring(valueStart, index)
        }
        return null
    }

    private fun parseOrNull(line: String): Problem? = try {
        json.decodeFromString<Problem>(line)
    } catch (exception: Exception) {
        println("ERROR: Could not parse puzzle line. ${exception.message}")
        null
    }
}
