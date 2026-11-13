# Workshop Core

A shared library for Workshopcraft mods. Ship it jar-in-jar inside the mods that use it so common code lives in one place.

Mod id: `workshopcore`. Version 0.1.0. The public API has no Minecraft or loader dependencies.

Targets: Minecraft 1.20.1 (Fabric, Forge), 1.21.1 (Fabric, NeoForge), 26.1.2, 26.2 and 26.3 (Fabric, NeoForge).

This is a library only. Do not put it in a mods folder on its own; bundle it as shown below.

## Public API

Package `com.workshopcraft.workshopcore`. Class `WorkshopCore`:

- `MOD_ID` - the mod id, `"workshopcore"`
- `API_VERSION` - the API version string, currently `"0.1.0"`
- `isAtLeast(String version)` - true if this library's API version is greater than or equal to `version`. Compares the first three numeric parts (`major.minor.patch`). A suffix after `-` is ignored.

Example:

```java
if (WorkshopCore.isAtLeast("0.1.0")) {
    // safe to call 0.1 API
}
```

## Maven coordinates

Builds are published to the local Maven folder `/workspace/mods/workshopcore-maven`.

- Group: `com.workshopcraft.workshopcore`
- Artifact id: `workshopcore-<loader>-<minecraft>`
- Version: the library version, for example `0.1.0`

Loaders are `fabric`, `neoforge` or `forge`. Pick the artifact that matches your game version and loader:

- `com.workshopcraft.workshopcore:workshopcore-fabric-26.1.2:0.1.0`
- `com.workshopcraft.workshopcore:workshopcore-neoforge-26.1.2:0.1.0`
- `com.workshopcraft.workshopcore:workshopcore-fabric-1.21.1:0.1.0`
- `com.workshopcraft.workshopcore:workshopcore-neoforge-1.21.1:0.1.0`
- `com.workshopcraft.workshopcore:workshopcore-fabric-1.20.1:0.1.0`
- `com.workshopcraft.workshopcore:workshopcore-forge-1.20.1:0.1.0`
- `com.workshopcraft.workshopcore:workshopcore-fabric-26.2:0.1.0`
- `com.workshopcraft.workshopcore:workshopcore-neoforge-26.2:0.1.0`
- `com.workshopcraft.workshopcore:workshopcore-fabric-26.3:0.1.0`
- `com.workshopcraft.workshopcore:workshopcore-neoforge-26.3:0.1.0`

0.1.0 is published for 26.1.2 Fabric and NeoForge. Other version nodes are configured in the build, but those artifacts are not published yet.

Repository:

```kotlin
repositories {
    maven {
        url = uri("/workspace/mods/workshopcore-maven")
    }
}
```

## Fabric (include)

```kotlin
dependencies {
    modImplementation("com.workshopcraft.workshopcore:workshopcore-fabric-26.1.2:0.1.0")
    include("com.workshopcraft.workshopcore:workshopcore-fabric-26.1.2:0.1.0")
}
```

Swap `26.1.2` for your game version (`1.20.1`, `1.21.1`, `26.2`, `26.3`).

## NeoForge and Forge (jarJar)

Use a version range so that if several mods bundle Workshop Core, the newest copy wins:

```kotlin
dependencies {
    jarJar(implementation("com.workshopcraft.workshopcore:workshopcore-neoforge-26.1.2") {
        version {
            strictly("[0.1,1.0)")
            prefer("0.1.0")
        }
    })
}
```

On Forge 1.20.1 the artifact is `workshopcore-forge-1.20.1` and the same `jarJar` block applies.

Widen or raise the range when you start using a new API, and keep the upper bound below the next major version you are not compatible with.
