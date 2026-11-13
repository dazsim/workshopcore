# Workshop Core

A shared library for Workshopcraft mods. Ship it jar-in-jar inside the mods that use it so common code lives in one place.

Mod id: `workshopcore`. Version 0.3.0.

Targets: Minecraft 1.20.1 (Fabric, Forge), 1.21.1 (Fabric, NeoForge), 26.1.2, 26.2 and 26.3 (Fabric, NeoForge).

This is a library only. Do not put it in a mods folder on its own; bundle it as shown below. The 26.x Fabric artifact depends on Fabric API (do not jar-in-jar Fabric API).

`WorkshopCore` itself has no Minecraft or loader dependencies. The `compat` package is a version compatibility layer and depends on Minecraft.

## Public API

### WorkshopCore

Package `com.workshopcraft.workshopcore`.

- `MOD_ID` - the mod id, `"workshopcore"`
- `API_VERSION` - the API version string, currently `"0.3.0"`
- `isAtLeast(String version)` - true if this library's API version is greater than or equal to `version`. Compares the first three numeric parts (`major.minor.patch`). A suffix after `-` is ignored.

```java
if (WorkshopCore.isAtLeast("0.3.0")) {
    // safe to call 0.2 API
}
```

### Version compat layer

Package `com.workshopcraft.workshopcore.compat`, plus `com.workshopcraft.workshopcore.compat.client` for rendering. Use these types on every game version instead of version-specific Minecraft APIs.

#### Registration

Constructors that changed shape between versions:

```java
BlockEntityType<MyBe> type = Registration.blockEntity(MyBe::new, Set.of(MY_BLOCK));
RecipeSerializer<MyRecipe> serializer = Registration.serializer(MyRecipe.CODEC, MyRecipe.STREAM_CODEC);
```

On 1.20.1, `Registration.id(recipe)` / `Registration.withId(id, recipe)` keep recipe ids that the serializer reads from JSON or the network.

`Registration.blockEntity` takes `Registration.BlockEntityFactory`. If you already have a vanilla `BlockEntityType.BlockEntitySupplier`, call `Registration.blockEntitySupplier` instead. The method is named separately so `MyBe::new` is not ambiguous. On 26.x Fabric, Workshop Core ships an access widener so the vanilla `BlockEntityType` constructor is callable; NeoForge already opens it.

#### Saved data

`CodecSavedData` is the SavedData base class. `SavedDataType` holds the id, constructor, codec and data fixer. The id you pass is the storage key: on 26.x it is used as-is, on 1.21.1 and 1.20.1 it becomes `namespace_path`. Do not change an existing id if you need old world files to load.

```java
public class MyData extends CodecSavedData {
    public static final Codec<MyData> CODEC = ...;
    public static final SavedDataType<MyData> TYPE =
            new SavedDataType<>(new Identifier("mymod", "my_data"), MyData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    public static MyData get(MinecraftServer server) {
        return TYPE.get(server); // overworld / server storage
    }

    public static MyData get(ServerLevel level) {
        return TYPE.get(level); // that level's storage
    }
}
```

#### Value input and output

`ValueInput` and `ValueOutput` are the stable save/load types. On 26.x they wrap the vanilla objects; on older versions they read and write a `CompoundTag`. Keys you pass are stored unchanged.

```java
protected void loadAdditional(ValueInput in) {
    ticks = in.getIntOr("Ticks", 0);
    in.read("Item", ItemStack.CODEC).ifPresent(s -> item = s);
}

protected void saveAdditional(ValueOutput out) {
    out.putInt("Ticks", ticks);
    out.storeNullable("Item", ItemStack.CODEC, item);
}
```

`ItemContainers.loadAllItems` / `saveAllItems` move a list of stacks through those types.

#### Block, block entity and menu wrappers

Extend these instead of the raw Minecraft base classes:

- `CompatBlock` - Block, with split `useItemOn` / `useWithoutItem` and `affectNeighborsAfterRemoval` on older versions
- `CompatEntityBlock` - BaseEntityBlock, same hooks
- `CompatBlockEntity` - load/save through `ValueInput` / `ValueOutput`
- `CompatContainerBlockEntity` - the same, for container block entities
- `CompatMenu` - includes `addStandardInventorySlots(Inventory, x, y)` on every version

```java
public class MyBe extends CompatBlockEntity {
    public MyBe(BlockPos pos, BlockState state) {
        super(MyBlocks.MY_BE, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput in) { /* ... */ }

    @Override
    protected void saveAdditional(ValueOutput out) { /* ... */ }
}
```

On 1.20.1, override `componentStorageKey()` (for example `"SimpleMachines"`) if the block entity stores implicit item components in an existing NBT compound. Leave it null if you do not need that.

#### Players, interactions, chunk tickets

- `Players.overlay(player, message)` - action-bar / overlay text
- `Players.reach(player)` - block interaction range
- `Interactions.TRY_WITH_EMPTY_HAND` - from `useItemOn`, meaning "try the empty-hand hook next". On 1.21.1, `Interactions.item(result)` maps that to `ItemInteractionResult`.
- `ChunkTickets.add(level, pos, radius)` / `remove(...)` - loading and ticking tickets that are not written to the vanilla ticket file. These static methods share one type, `workshopcore:chunk_loader`. If two mods hold the same chunk and radius with them, one `remove` drops both. Prefer `ChunkTickets.create(id)` for an isolated type per owner:

```java
ChunkTickets.Owner tickets = ChunkTickets.create(Identifier.fromNamespaceAndPath("mymod", "chunk_loader"));
// 26.x only: register during loader init (Fabric Registry.register, or NeoForge RegisterEvent)
tickets.register((id, type) -> Registry.register(BuiltInRegistries.TICKET_TYPE, id, type));
tickets.add(level, pos, radius);
tickets.remove(level, pos, radius);
```

Workshop Core registers the shared type on 26.x, and any `create` owners already made at that point. Older versions do not need a registry call; `create` still returns an isolated type.

#### Item helpers

- `Comps.get/set/has/remove/getOrDefault(stack, type)` - item components. On 1.20.1 pass your existing compound key as well, e.g. `Comps.get(stack, "SimpleMachines", type)`, or set `storage("SimpleMachines")` when you build the type. The extra key is ignored on later versions.
- `Stacks.consume(stack, count, entity)`, `Stacks.hash(stack)`, `Stacks.limit(container, stack)`, `Stacks.OPTIONAL_CODEC`

#### GUI and vertices

- `Gui.blitSprite(graphics, sprite, x, y, w, h)` and the nine-argument form with texture size and UV. On 26.x the graphics type is `GuiGraphicsExtractor`; on older versions it is `GuiGraphics`.
- `Verts.add(consumer, x, y, z, argb, u, v, overlay, light, nx, ny, nz)`

#### RenderCompat

Package `com.workshopcraft.workshopcore.compat.client`.

- `cutoutBlocks()` / `translucentBlocks()` - block render types
- `light(level, pos)` - packed light
- `rotate(pose, quaternion)` - pose rotation

```java
RenderCompat.rotate(pose, rotation);
int light = RenderCompat.light(level, pos);
```

#### 1.20.1-only stand-ins

These exist so 1.20.1 code can keep the later component and network-codec names. On 1.21.1 and 26.x, use the vanilla packages instead (`net.minecraft.core.component`, `net.minecraft.network.codec`).

- `com.workshopcraft.workshopcore.compat.component.DataComponentType` (builder has `storage(key)` and `name(name)`)
- `com.workshopcraft.workshopcore.compat.component.DataComponentMap` / `DataComponents` (`BLOCK_STATE`, `DYE`)
- `com.workshopcraft.workshopcore.compat.item.BlockItemStateProperties`
- `com.workshopcraft.workshopcore.compat.codec.StreamCodec` / `ByteBufCodecs`

### Sky / backdrop renderer

Package `com.workshopcraft.workshopcore.backdrop`. Available on Minecraft 26.1.2 and later. Older version nodes still configure, but this feature is not compiled there yet.

A backdrop is a named, data-driven sky. When one is set on a level it replaces the vanilla sun, moon, stars and end sky, and vanilla clouds are hidden. Fog and weather stay vanilla. Clearing the backdrop (or falling back to vanilla) shows clouds again. If nothing is set, the vanilla sky is left alone. There is no crossfade between backdrops in this version; a set is instant. A later version may add a fade.

#### Data files

`data/<namespace>/workshopcore/backdrops/<name>.json`. The id is `<namespace>:<name>`.

```json
{
  "layers": [
    { "type": "color", "color": "#000010" },
    { "type": "gradient", "top": "#000010", "horizon": "#1a0033", "bottom": "#000000" },
    { "type": "stars", "count": 2000, "brightness": 1.0, "seed": 1, "rotate_with_time": true, "drift": 0.4 },
    { "type": "warp", "count": 80, "speed": 1.0, "length": 8.0, "color": "#a0c8ff", "direction": { "yaw": 0, "pitch": 0 } },
    { "type": "body", "texture": "mymod:textures/environment/planet.png", "size": 24, "azimuth": 210, "altitude": 18, "spin": 0.05 }
  ]
}
```

This example is documentation only. Workshop Core does not ship backdrop data.

Layer types:

- `color` (alias `sky_color`): solid sky disc. `color` is `#RRGGBB` or `#AARRGGBB`.
- `gradient`: vertical wash. `top`, optional `horizon`, `bottom`.
- `stars`: static starfield. `count`, `brightness`, optional `color`, `seed`. Optional `rotate_with_time` follows the day cycle. Optional `drift` is extra rotation in degrees per second.
- `warp`: streaks that all lie on rays from one fixed vanishing point in `direction` (default toward -Z: yaw 0 pitch 0). They travel from that point toward the viewer and grow longer and brighter as they pass, like a starfield jump. `count`, `speed`, `length`, optional `color`. Optional `spread` is the cone half-angle in degrees around that direction (default 25). Optional `direction` is `{ "yaw", "pitch" }` in degrees.
- `body`: a textured disc. `texture` is an assets path. `size` is apparent diameter in degrees. `azimuth` and `altitude` are degrees (azimuth 0 is -Z, altitude 90 is zenith). Optional `spin` is revolutions per second. Optional `orbit_speed` (degrees per second) and `orbit_axis` (`{ "yaw", "pitch" }`, default straight up).

Layers draw in list order, back to front. Textures come from resource packs.

Defaults: `data/<namespace>/workshopcore/backdrop_defaults.json`

```json
{
  "dimensions": {
    "minecraft:the_end": "mymod:end_dark"
  },
  "dimension_types": {
    "simplemachines:portal_void": "simplemachines:void_sky"
  }
}
```

A dimension id wins over a dimension type. Definitions use the highest-priority pack for each id. Default files from every pack are merged in pack order, later packs winning on the same key. On first load of a level, if nothing is saved, the default is applied and saved. After that the saved value wins, including after `/reload` and a server restart. `Backdrops.clear` (and the clear command) saves an explicit vanilla state, so a default is not reapplied until the save is gone. If a saved id is missing after a reload, the sky is vanilla until a valid id is set; the saved id is kept.

A backdrop will not show in a dimension whose type has `skybox: none`, because vanilla never runs the sky pass there.

#### API and command

```java
Backdrops.set(level, Identifier.parse("homebound:warp"));
Backdrops.clear(level);
Optional<Identifier> current = Backdrops.get(level);
boolean loaded = Backdrops.exists(Identifier.parse("homebound:warp"));

BackdropEvents.listen((level, oldId, newId) -> {
    // fired on set, clear, and first default apply
});
```

`Backdrops.set` throws if the id is not loaded.

```
/workshopcore backdrop get
/workshopcore backdrop set <id>
/workshopcore backdrop clear
```

Permission 2, current level. Simple Void can wrap this as `/void backdrop` later.

The current id is stored per level with `SavedDataType` under `workshopcore:backdrop`. Backdrop JSON lives in server data packs, so the client is sent the validated definitions (not just ids) on login, on datapack reload, and when a player changes dimension. The current assignment is also sent on set, clear, login, reload, and dimension change. When a level unloads, clients drop the cached assignment for that dimension key so a later runtime dimension with the same key cannot keep a stale sky. Disconnect also clears the client cache.

## Maven coordinates

Builds are published to the local Maven folder `/workspace/mods/workshopcore-maven`.

- Group: `com.workshopcraft.workshopcore`
- Artifact id: `workshopcore-<loader>-<minecraft>`
- Version: the library version, for example `0.3.0`

Loaders are `fabric`, `neoforge` or `forge`. Pick the artifact that matches your game version and loader:

- `com.workshopcraft.workshopcore:workshopcore-fabric-26.1.2:0.3.0`
- `com.workshopcraft.workshopcore:workshopcore-neoforge-26.1.2:0.3.0`
- `com.workshopcraft.workshopcore:workshopcore-fabric-1.21.1:0.3.0`
- `com.workshopcraft.workshopcore:workshopcore-neoforge-1.21.1:0.3.0`
- `com.workshopcraft.workshopcore:workshopcore-fabric-1.20.1:0.3.0`
- `com.workshopcraft.workshopcore:workshopcore-forge-1.20.1:0.3.0`
- `com.workshopcraft.workshopcore:workshopcore-fabric-26.2:0.3.0`
- `com.workshopcraft.workshopcore:workshopcore-neoforge-26.2:0.3.0`
- `com.workshopcraft.workshopcore:workshopcore-fabric-26.3:0.3.0`
- `com.workshopcraft.workshopcore:workshopcore-neoforge-26.3:0.3.0`

0.3.0 is published for 26.1.2 Fabric and NeoForge. Other version nodes are configured in the build, but those artifacts are not published yet.

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
    modImplementation("com.workshopcraft.workshopcore:workshopcore-fabric-26.1.2:0.3.0")
    include("com.workshopcraft.workshopcore:workshopcore-fabric-26.1.2:0.3.0")
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
            prefer("0.3.0")
        }
    })
}
```

On Forge 1.20.1 the artifact is `workshopcore-forge-1.20.1` and the same `jarJar` block applies.

Widen or raise the range when you start using a new API, and keep the upper bound below the next major version you are not compatible with.
