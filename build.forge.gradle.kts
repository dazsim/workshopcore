plugins {
    id("net.neoforged.moddev.legacyforge")
    id("neoforge-mutex")
    id("maven-publish")
}

val modVersion = providers.gradleProperty("version").orNull ?: sc.properties.get<String>("mod.version")
val mc = sc.current.version
version = modVersion
group = property("mod.group") as String
base.archivesName = "${property("mod.id")}-forge-$mc"

sourceSets.main {
    java.exclude("com/workshopcraft/workshopcore/fabric/**", "com/workshopcraft/workshopcore/neoforge/**", "com/workshopcraft/workshopcore/backdrop/**", "com/workshopcraft/workshopcore/hazard/**", "com/workshopcraft/workshopcore/screwdriver/**", "com/workshopcraft/workshopcore/mixin/**")
    applyOverlays(rootProject.file("."), overlayChain(mc, "forge"))
}

legacyForge {
    version = property("deps.forge") as String
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
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
    toolchain.languageVersion = JavaLanguageVersion.of(17)
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
            "java" to "17",
        )
        inputs.properties(props)
        filesMatching("META-INF/mods.toml") { expand(props) }
        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml", "workshopcore.accesswidener", "workshopcore.mixins.json")
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
