plugins {
    id("net.neoforged.moddev")
    id("neoforge-mutex")
    id("maven-publish")
}

val modVersion = providers.gradleProperty("version").orNull ?: sc.properties.get<String>("mod.version")
val mc = sc.current.version
val modern = sc.current.parsed >= "26.1"
version = modVersion
group = property("mod.group") as String
base.archivesName = "${property("mod.id")}-neoforge-$mc"

val requiredJava = when {
    modern -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

sourceSets.main {
    java.exclude("com/workshopcraft/workshopcore/fabric/**", "com/workshopcraft/workshopcore/forge/**")
}

neoForge {
    version = property("deps.neo_loader") as String
    accessTransformers.files.setFrom()
    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        register("client") {
            gameDirectory = rootProject.file("runs/${sc.current.project}")
            client()
        }
        register("server") {
            gameDirectory = rootProject.file("runs/${sc.current.project}-server")
            server()
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain.languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
}

tasks {
    processResources {
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to modVersion,
            "author" to sc.properties.get<String>("mod.author"),
            "description" to sc.properties.get<String>("mod.description"),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "neoforge" to if (modern) "[26.1.0.0-beta,)" else "[21.1,)",
            "java" to requiredJava.majorVersion,
        )
        inputs.properties(props)
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        exclude("fabric.mod.json", "META-INF/mods.toml")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        val name = project.property("mod.name")
        inputs.property("mod_name", name)
        from(rootProject.file("LICENSE")) { rename { "${it}_$name" } }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = base.archivesName.get()
        }
    }
    repositories {
        maven {
            name = "WorkshopCoreLocal"
            url = uri(rootProject.file("../workshopcore-maven"))
        }
    }
}
