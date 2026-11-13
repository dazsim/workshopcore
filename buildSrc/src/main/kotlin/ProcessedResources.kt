import org.gradle.api.Project
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

/** The active Stonecutter version compiles from unprocessed sources. Resources still have to be
 *  processed, or `//?` comments leak into json/toml that the game cannot parse. */
fun Project.preferProcessedStonecutterResources(active: Boolean) {
    val generated = layout.buildDirectory.dir("generated/stonecutter/main/resources")
    tasks.named("processResources", ProcessResources::class.java) {
        dependsOn("stonecutterGenerate")
        filesMatching(listOf("**/*.json", "**/*.toml", "**/*.mcmeta")) {
            filter(StripStonecutterResidue::class.java)
        }
        if (active) {
            val raw = "${File.separator}src${File.separator}main${File.separator}resources${File.separator}"
            exclude { details ->
                val n = details.name
                details.file.absolutePath.contains(raw)
                    && (n.endsWith(".json") || n.endsWith(".toml") || n.endsWith(".mcmeta"))
            }
            from(generated) {
                include("**/*.json", "**/*.toml", "**/*.mcmeta")
            }
        }
    }
}
