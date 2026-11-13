import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

/** Drops 26.x-only fabric.mod.json keys (access widener, mixins, fabric-api) for older versions. */
class StripLegacyFabricJson(input: Reader) : FilterReader(StringReader(strip(input.readText()))) {
    companion object {
        private val MIXINS = Regex("""(?s),\s*"mixins"\s*:\s*\[.*?\]""")
        private val FABRIC_API = Regex("""(?m)^\s*"fabric-api"\s*:\s*".*?",?\s*\n""")
        private val ACCESS = Regex("""(?m)^\s*"accessWidener"\s*:\s*".*?",?\s*\n""")

        fun strip(text: String): String {
            return text.replace(MIXINS, "").replace(FABRIC_API, "").replace(ACCESS, "")
        }
    }
}
