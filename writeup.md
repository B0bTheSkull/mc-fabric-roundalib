# The shared utilities behind my Minecraft mod ports

> Not every piece of code is meant to be a hero. Some of it just needs to exist so you don't write the same config loader for the fourth time.

## TL;DR

`mc-fabric-roundalib` is the internal shared library used by Roundaround's Minecraft Fabric mods. It handles config management, GUI widgets, an observable/reactive data layer, and a thin network layer — the kind of cross-cutting plumbing that either lives in a shared lib or gets copy-pasted until something drifts. It's consumed as a Gradle dependency and never installed directly.

---

## Why it exists

When you're maintaining more than one mod against the same game version, you start noticing patterns. Every mod needs a config file. Every mod needs a settings screen. Every mod needs typed options with validation and a way to react when the player changes something.

The alternative to a shared library is implementing all of that per-mod. That's fine for one mod. It's a maintenance problem for three, and a disaster for ten. RoundaLib exists to solve that once.

## What it does

The library is split into five Gradle modules:

- **`config`** — Typed config option builders (`IntConfigOption`, `BooleanConfigOption`, `EnumConfigOption`, `StringConfigOption`, `PositionConfigOption`), a `ModConfig` interface backed by `ModConfigImpl`, and serializers for TOML, YAML, JSONC, and `.properties`. File scoping is first-class: configs can be game-scoped, world-scoped, or read-only.

- **`config-gui`** — The in-game settings screens that render `ModConfig` without any per-mod boilerplate. Controls (sliders, toggles, enum cycles, sub-screen buttons) auto-wire to the matching `ConfigOption` type via a `ControlRegistry`.

- **`gui`** — Reusable Minecraft GUI primitives: `LinearLayoutWidget`, `ThreeSectionLayoutWidget`, `FlowListWidget`, `IconButtonWidget`, typed `Spacing`/`Coords`/`IntRect` value types, and a `BaseScreen` most mod screens extend.

- **`observables`** — A lightweight reactive layer: `Observable<T>`, `Subject<T>` (mutable), `Computed<T>` (derived), and `Observable.combine(...)` overloads up to 12 sources. Mod code subscribes to config values rather than polling them.

- **`network`** / **`core`** — Custom packet codecs (`RoundaLibPacketCodecs`), a `RequestTracker` for client-server round-trips, Mixin hooks for screen input and resource manager events.

## The API in practice

Config definition is a builder chain. Here's how `mc-fabric-custom-paintings` uses it:

```java
// CustomPaintingsConfig.java
public class CustomPaintingsConfig extends ModConfigImpl implements GameScopedFileStore {
  public BooleanConfigOption cacheImages;
  public IntConfigOption cacheTtl;

  @Override
  protected void registerOptions() {
    this.cacheImages = this.buildRegistration(
        BooleanConfigOption.yesNoBuilder(ConfigPath.of("cacheImages"))
            .setComment("Cache images from the server locally")
            .setDefaultValue(true)
            .build()
    ).clientOnly().commit();

    this.cacheTtl = this.buildRegistration(
        IntConfigOption.builder(ConfigPath.of("cacheTtl"))
            .setComment("Number of days to retain cached images")
            .setDefaultValue(14)
            .setMinValue(1)
            .setMaxValue(10000)
            .onUpdate((option) -> option.setDisabled(!this.cacheImages.getPendingValue()))
            .build()
    ).clientOnly().commit();
  }
}
```

The `onUpdate` callback on `cacheTtl` is the observable layer doing work: when `cacheImages` changes in the UI, `cacheTtl` disables itself automatically. That kind of inter-option dependency would otherwise require manual event wiring in every mod.

The GUI side of this requires zero additional code. `ConfigScreen` reads the registered options, looks up controls via `ControlRegistry`, and renders the whole settings screen from the `ModConfig` alone.

## What it does well

**The builder API is hard to misuse.** `IntConfigOption.sliderBuilder(path)` requires min/max at build time (it panics at startup if you forget); the type system prevents assigning a `FloatConfigOption` where an `IntConfigOption` is expected. Errors surface during development, not at runtime when a player opens settings.

**Scoping is a first-class concern.** A mod can declare that a config file belongs to a specific world (per-save settings) versus the whole game installation. That distinction matters for mods like custom paintings, where per-world overrides make sense for server-side config.

**The observable layer is lightweight.** It's not RxJava. It's a small, dependency-free reactive primitive that fits the Minecraft mod lifecycle — no framework overhead, no threading surprises, just subscriptions you close when the screen is done.

## What it doesn't try to do

RoundaLib is not a general-purpose Minecraft modding framework and it doesn't try to be. It has no opinions about game logic, entity behavior, item registration, or world generation. It's plumbing. It solves config, GUI, and reactive state for a specific set of mods and does nothing else.

It's also not versioned for external consumption. The API evolves with the mods that use it. If you're building your own Fabric mod, there's no stability guarantee here — this lib is an internal dependency, not a published SDK.

## Consuming it

Mods pull it in via Gradle:

```kotlin
// build.gradle.kts (consuming mod)
dependencies {
  modImplementation("me.roundaround:roundalib-config:${libs.versions.roundalib.get()}")
  modImplementation("me.roundaround:roundalib-config-gui:${libs.versions.roundalib.get()}")
  modImplementation("me.roundaround:roundalib-gui:${libs.versions.roundalib.get()}")
}
```

Each module is independent. A server-side mod that needs config but no GUI only pulls in `roundalib-config`.

## Takeaway

There's nothing glamorous about a shared utility library. The value isn't in any single feature — it's in the fact that every mod in the family gets validation, file-scoped persistence, and a settings screen for free, without repeating the same boilerplate. RoundaLib is the reason that work happens once.
