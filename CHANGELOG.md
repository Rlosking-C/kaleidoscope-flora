# Changelog

All notable changes to this project are documented in this file.

> **How this file was compiled.** The entries below were reconstructed from the
> dated author decisions and player reports recorded in the source itself
> (mostly `FloraDrinks`, `FloraTeas`, `ModEffects`, `FloraEvents`,
> `BlossomMooncakes`, `LegacyMilkSoupBase`). Where a change carries an exact
> date in the code the date is kept; where the code does not pin a change to a
> release, the entry is left under the version it belongs to with no invented
> detail. Earlier versions are still thin and should be filled in from the
> release notes as they are found.

## 0.3.4

### Changed

- **Brewing moved off the stockpot onto tea bags and the teapot (2026-09-22).**
  The production line now reads flower → tea bag → teapot → cup, and the teapot
  is the only route:
  - A tea bag is a plain shaped crafting recipe: the flower, Cookery's dried tea
    leaves in the middle, and that drink's old stockpot ingredients around it
    (2026-09-23).
  - One bag brews a full 12-cup pot in Cookery's teapot — over water, except
    Hanami Tale, which brews in milk.
  - The stockpot route was deleted whole: its 26 recipes, its bubble textures,
    and the Java that supported it (the honey-bottle container work-around, the
    teapot scoop-out, and the reflection that drained a finished pot). A
    `tools/` generator now owns pruning as well as generation.
  - The 25 intermediate `dried_*` items, their textures and models, the Bamboo
    Tray drying recipes and the whole `recipe/drying/` folder went with it.
- **Effects are 23, not 24.** Twelve effects carry their own logic in
  `applyEffectTick` / `onEffectStarted`; the other eleven are markers whose
  behaviour lives in `FloraEvents` via damage, loot and interaction hooks.

### Fixed

- **Farmer's Delight rice: the Vernal Awakening effect did nothing for it
  (player report, 2026-09-23).** `RiceBlock` extends `BushBlock`, so the plain
  `instanceof CropBlock` test walked straight past it.
- **Farmer's Delight rice panicles: the effect threw instead of growing it
  (player crash, 2026-09-23).** `rice_panicles` extends `CropBlock` but swaps
  the age property for its own `rice_age`, so poking `CropBlock.AGE` on that
  state threw `IllegalArgumentException: Cannot get property
  IntegerProperty{name=age, values=[0..7]} as it does not exist in
  Block{farmersdelight:rice_panicles}` straight out of the effect tick. Growing
  now goes through each block's own `BonemealableBlock` rules, exactly as bone
  meal would, so any modded crop grows by its own conventions. The integer
  `age` property test is deliberate: grass and saplings also accept bonemeal,
  and a spore blossom should not carpet the world in flowers.

### Notes

- **Drowsiness is additive per cup by design.** Vanilla's refresh rule only
  keeps the longer duration, so cups do not stack by themselves; the remaining
  time is reconstructed between drinks, which is what lets a whole pot be
  stockpiled for one long night.
- **The milk soup base alias stays.** Worlds saved by 0.3.1 and earlier can
  still hold `kaleidoscope_flora:milk` in a stockpot's NBT; this alias is what
  keeps Cookery's renderer from throwing on it. It was removed once and the
  crash came back — do not delete it again.

## 0.3.3

### Added

- **Blossom Mooncake**, the Mid-Autumn item: a tray block that stacks up to
  five, eaten for the Floating effect — the air becomes water, and the eater
  swims through it.

## 0.3.2

### Changed

- **Kaleidoscope Cookery 1.5.0 compatibility**: the teacup and teapot were
  verified unchanged. The milk soup base now reuses Cookery's native
  `minecraft:milk` instead of a mod-owned soup base, and the dependency range
  was tightened to `[1.5.0, 1.6.0)`.

## 0.3.1

### Added

- **Dynamic light integration**: The Sunward lights up the night through
  LambDynamicLights or Sodium Dynamic Lights, whichever is installed. The two
  are incompatible with each other, so only one should be present.

### Fixed

- **Dedicated server startup crash**, caused by the milk soup base.

## 0.3.0

### Added

- **Server config** (`kaleidoscope_flora-common.toml`), four knobs:
  `effectDurationMultiplier` (0.25–4.0, default 1.0),
  `auraRangeMultiplier` (0.25–3.0, default 1.0),
  `vampiricHealPercent` (0–100, default 20) and
  `harvestMaxDropMultiplier` (2–8, default 4).
- **JEI info pages** for every drink, showing its maxim and effect
  description.

### Changed

- **Breath of the Ancients reworked** onto the vanilla brush animation with
  continuous digging, paying for every find with buff duration.

## 0.2.0

### Added

- **26 flower drinks**, each with its own recipe, effect and a flower-language
  quote. (22 register on a plain install; the four brewed from 1.21.4+ flowers
  register only when Vanilla Backport is present, since those flowers do not
  exist on 1.21.1.)
- **23 custom effects** — auras, the gaze, sniffer-dig archaeology,
  water-walking, a drop magnet and more.
- **15 advancements.**
- **Signature effects**, including walking on water, glowing through the night
  and digging up artifacts with a sniffer's instincts.

### Changed

- **Rosy Stride reworked into true surface physics** — smooth boarding of banks
  and shores. The film is real geometry: the water block under the drinker is
  lent a collision box, so boarding a bank, wading ashore and jumping in place
  are plain vanilla physics, with no pin and no teleport for the engine to
  fight.

## Parked / not shipped

- **Flower perches** — a whole feature line (flower perch blocks in nine wood
  bases, 26 flowers × 4 growth stages, aroma particles, two advancements) was
  built and then set aside. Its Java and resources are kept outside the build
  tree, together with the vanilla reference textures used to draw it. Kept
  deliberately: it is the largest ready-made asset pool in the repository.
