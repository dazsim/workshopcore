import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

/** Drops leftover `//?` directives and `/* */` comment blocks that Stonecutter keeps after
 *  choosing a branch. Those tokens are fine in Java, but json and toml parsers reject them. */
class StripStonecutterResidue(input: Reader) : FilterReader(StringReader(strip(input.readText()))) {
    companion object {
        private val DIRECTIVE = Regex("""//\?.*""")
        private val BLOCK = Regex("""/\*.*?\*/""", setOf(RegexOption.DOT_MATCHES_ALL))
        private val BLANK = Regex("""\n{3,}""")

        fun strip(text: String): String {
            return text.replace(DIRECTIVE, "").replace(BLOCK, "").replace(BLANK, "\n\n")
        }
    }
}
