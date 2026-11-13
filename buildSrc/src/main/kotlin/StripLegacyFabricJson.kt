import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

/** Drops the 26.x-only access widener key from older fabric.mod.json files. Mixins and fabric-api stay. */
class StripLegacyFabricJson(input: Reader) : FilterReader(StringReader(strip(input.readText()))) {
    companion object {
        private val ACCESS = Regex("""(?m)^\s*"accessWidener"\s*:\s*".*?",?\s*\n""")
        private val TRAILING_COMMA = Regex(""",(\s*})""")

        fun strip(text: String): String {
            return text.replace(ACCESS, "").replace(TRAILING_COMMA, "$1")
        }
    }
}
