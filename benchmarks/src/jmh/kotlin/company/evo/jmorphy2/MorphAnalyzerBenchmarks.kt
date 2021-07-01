package company.evo.jmorphy2

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
open class MorphAnalyzerBenchmarks {
    val morph = Jmorphy2TestsHelpers.newMorphAnalyzer("ru")
    val words = loadWords()

    companion object {
        private const val WORDS_FREQ_RESOURCE = "/company/evo/jmorphy2/unigrams.txt"

        fun loadWords(): Words {
            val wordsReader = Words::class.java.getResourceAsStream(WORDS_FREQ_RESOURCE)!!
                .bufferedReader()
            val words = arrayListOf<String>()
            val counts = arrayListOf<Int>()
            for (line in wordsReader.lines()) {
                val parts = line.split("\\s".toRegex())
                if (parts.size < 2) {
                    continue
                }
                words.add(parts[0].lowercase())
                counts.add(parts[1].toInt())
            }
            return Words(words.toTypedArray(), counts.toIntArray())
        }
    }

    class Words(val words: Array<String>, val counts: IntArray)

    @Benchmark
    open fun benchParse(blackhole: Blackhole) {
        for (word in words.words) {
            blackhole.consume(
                morph.parse(word)
            )
        }
    }
}
