import org.gradle.api.tasks.SourceSet
import java.io.File

// Whole-file Java overlays, highest priority first. A file in an earlier dir hides the same path in later dirs and in main.
fun SourceSet.applyOverlays(root: File, names: List<String>) {
    val dirs = names.map { File(root, "src/legacy-java/$it") }.filter { it.isDirectory }
    dirs.forEach { java.srcDir(it) }
    val rel = dirs.map { d -> d.walk().filter { it.isFile }.map { it.relativeTo(d).invariantSeparatorsPath }.toSet() }
    java.exclude { e ->
        val idx = dirs.indexOfFirst { e.file.absolutePath.startsWith(it.absolutePath + File.separator) }
        val higher = if (idx < 0) rel else rel.subList(0, idx)
        !e.isDirectory && higher.any { e.relativePath.pathString in it }
    }
}

fun overlayChain(mc: String, loader: String): List<String> = when (mc) {
    "1.20.1" -> listOf("1.20.1-$loader", "1.20.1")
    "1.21.1" -> listOf("1.21.1-$loader", "1.21.1")
    else -> emptyList()
}
