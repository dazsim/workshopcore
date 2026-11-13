import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/** Rewrites 1.21+ datapack paths and recipe/advancement shape for Minecraft 1.20.1. */
abstract class Data120 : DefaultTask() {
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Input
    abstract val forge: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    private val dirs = listOf(
        "recipe/" to "recipes/",
        "advancement/" to "advancements/",
        "tags/item/" to "tags/items/",
        "tags/block/" to "tags/blocks/",
    )

    @Suppress("UNCHECKED_CAST")
    private fun obj(o: Any?) = o as MutableMap<String, Any?>

    private fun mapTag(tag: String): String {
        if (tag != "c:ingots/iron") return tag
        return if (forge.get()) "forge:ingots/iron" else "c:iron_ingots"
    }

    private fun ingredient(v: Any?): Any? {
        when (v) {
            is String -> return if (v.startsWith("#")) {
                linkedMapOf("tag" to mapTag(v.substring(1)))
            } else {
                linkedMapOf("item" to v)
            }
            is Map<*, *> -> {
                val m = LinkedHashMap<String, Any?>()
                for ((k, dv) in v) {
                    val key = k as String
                    m[key] = if (key == "tag" && dv is String) mapTag(dv) else dv
                }
                return m
            }
            else -> return v
        }
    }

    private fun recipe(j: MutableMap<String, Any?>) {
        val r = j["result"] as? Map<*, *>
        if (r != null && "id" in r) {
            val n = LinkedHashMap<String, Any?>()
            n["item"] = r["id"]
            for ((k, v) in r) if (k != "id") n[k as String] = v
            j["result"] = n
        }
        @Suppress("UNCHECKED_CAST")
        val key = j["key"] as? MutableMap<String, Any?>
        if (key != null) {
            for (k in key.keys.toList()) key[k] = ingredient(key[k])
        }
        @Suppress("UNCHECKED_CAST")
        val ingredients = j["ingredients"] as? MutableList<Any?>
        if (ingredients != null) {
            for (i in ingredients.indices) ingredients[i] = ingredient(ingredients[i])
        }
        if (j.containsKey("keep")) j["keep"] = ingredient(j["keep"])
    }

    private fun advancement(j: MutableMap<String, Any?>) {
        val crit = j["criteria"] as? Map<*, *> ?: return
        for (c in crit.values) {
            val items = ((c as Map<*, *>)["conditions"] as? Map<*, *>)?.get("items") as? List<*> ?: continue
            for (p in items) {
                val m = obj(p)
                val v = m["items"] as? String
                if (v != null) {
                    if (v.startsWith("#")) {
                        m.remove("items")
                        m["tag"] = mapTag(v.substring(1))
                    } else {
                        m["items"] = listOf(v)
                    }
                }
                val tag = m["tag"] as? String
                if (tag != null) m["tag"] = mapTag(tag)
            }
        }
    }

    private fun write(f: File, j: Any?) {
        f.parentFile.mkdirs()
        f.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(j)) + "\n")
    }

    @TaskAction
    fun run() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        val data = File(out, "data")
        val done = HashSet<String>()
        val usedC = sortedSetOf<String>()
        val ref = Regex("\"#?c:([a-z0-9_/]+)\"")
        for (base in sources.files.map { File(it, "data") }.filter { it.isDirectory }) {
            base.walkTopDown().filter { it.isFile && it.name.endsWith(".json") }.sortedBy { it.path }.forEach { f ->
                val rel = f.relativeTo(base).invariantSeparatorsPath
                val ns = rel.substringBefore('/'); val rest = rel.substringAfter('/')
                val (a, b) = dirs.firstOrNull { rest.startsWith(it.first) } ?: return@forEach
                val new = "$ns/$b${rest.removePrefix(a)}"
                if (!done.add(new)) return@forEach
                val text = StripStonecutterResidue.strip(f.readText())
                var j = obj(JsonSlurper().parseText(text))
                val conds = j.remove("neoforge:conditions")
                if (forge.get()) j.remove("fabric:load_conditions")
                if (conds != null && forge.get()) {
                    j["conditions"] = conds
                }
                when (b) {
                    "recipes/" -> recipe(j)
                    "advancements/" -> advancement(j)
                }
                ref.findAll(JsonOutput.toJson(j)).forEach { usedC.add(it.groupValues[1]) }
                write(File(data, new), j)
            }
        }
        for (path in usedC) {
            val target = File(data, "c/tags/items/$path.json")
            val vals = ArrayList<Any?>()
            if (target.exists()) vals.addAll((JsonSlurper().parse(target) as Map<*, *>)["values"] as List<*>)
            if ((path == "ingots/iron" || path == "iron_ingots") && "minecraft:iron_ingot" !in vals) {
                vals.add("minecraft:iron_ingot")
            }
            val optionals = when (path) {
                "ingots/iron" -> listOf("#forge:ingots/iron", "#c:iron_ingots")
                "iron_ingots" -> listOf("#forge:ingots/iron", "#c:ingots/iron")
                "tools/screwdriver" -> listOf("#forge:tools/screwdriver")
                else -> emptyList()
            }
            for (t in optionals) {
                if (t !in vals && vals.none { it is Map<*, *> && it["id"] == t }) {
                    vals.add(linkedMapOf("id" to t, "required" to false))
                }
            }
            write(target, linkedMapOf("replace" to false, "values" to vals))
        }
        logger.info("Data120: ${done.size} files, ${usedC.size} c tags")
    }
}
