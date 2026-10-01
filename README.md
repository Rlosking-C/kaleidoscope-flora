# Kaleidoscope Flora

![Banner](docs/banner.png)

Brew drinks from flowers, and let every flower have a flavor of its own.

An addon for [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) that turns vanilla flowers into a full brewing experience.

## Gameplay

Gather flowers, craft each one into a tea bag, brew a full pot in Cookery's teapot, then pour it out cup by cup and drink to gain the flower's blessing.

- **26 flower drinks** - each with its own recipe, effect, and a flower-language quote
- **22 out of the box, 4 more with Vanilla Backport** - the four 1.21.4+ flowers simply do not exist on 1.21.1, so their drinks, tea bags and recipes are only registered when the backport is installed
- **The teapot, not the stockpot** - one tea bag brews a whole pot in Cookery's teapot; pour it out with Cookery's empty teacups
- **Signature effects** - walk on water, glow through the night, dig up artifacts with a sniffer's instincts, and more
- **15 advancements**

## Requirements

| Dependency | Version | Required |
|---|---|---|
| Minecraft | 1.21.1 | Yes |
| NeoForge | 21.1.248 | Yes |
| [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) | 1.5.0 – 1.6.x | Yes |
| [Sodium Dynamic Lights](https://modrinth.com/mod/sodium-dynamic-lights) | 1.0.10 | No (full glow effect at night) |
| [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights) | 4.8.11+ | No (full glow effect at night) |
| [Vanilla Backport](https://modrinth.com/mod/vanillabackport) | - | No (unlocks 4 crossover drinks brewed from 1.21.4+ flowers) |

Note: the two dynamic lights mods are incompatible with each other — install one or the other, not both.

## Download

Grab the latest jar from [Releases](https://github.com/Rlosking-C/kaleidoscope-flora/releases) and drop it into your `mods` folder.

**Status**: v0.3.3 is released and feature complete - all 26 drinks, 23 custom effects and their artwork are in. v0.3.4 is in development: see the roadmap below.

## Roadmap

- **v0.2.0**
  - 26 flower drinks: tea bag recipes, teapot recipes, effects, item/cup textures
  - 23 custom effects (auras, gaze, sniffer-dig archaeology, water-walking, drop magnet, and more)
  - VanillaBackport crossover: 4 bonus drinks with 1.21.4+ flowers (recipes and drinks both gated cleanly)
  - 15 advancements
  - Rosy Stride reworked into true surface physics - smooth boarding of banks and shores
- **v0.3.0**
  - Server config: effect duration multiplier, aura range multiplier, vampiric heal percent, harvest drop cap
  - JEI info pages for every drink (maxim + effect description)
  - Breath of the Ancients rework: vanilla brush animation, continuous digging
- **v0.3.1**
  - LambDynamicLights integration: The Sunward lights up the night through LDL or Sodium Dynamic Lights
  - Dedicated server startup crash fix (milk soup base)
- **v0.3.2**
  - Cookery 1.5.0 compatibility: teacup/teapot verified unchanged; milk soup base now reuses Cookery's native `minecraft:milk`, dependency range tightened to [1.5.0, 1.6.0)
- **v0.3.3** (current)
  - **Brewing moved onto tea bags and the teapot, the only route** - one bag brews a whole pot in Cookery's teapot. The stockpot recipes, their bubble textures and the Java behind them were removed, along with the whole flower-drying chain (25 `dried_*` items and their recipes). Flower names now follow vanilla's official translations.
  - **Blossom Mooncake**, the Mid-Autumn item: a tray that stacks up to five, eaten for the Floating effect - the eater swims through the air
  - The flower perch feature was set aside to v0.4.0 rather than shipped half-finished
  - Fall damage no longer breaks farmland while Autumn Serenade is active
- **v0.3.4** (in development)
  - **Three flower cakes**: a raw cake (dough + any flower) thrown like a snowball to pin whatever it hits for 1 second; a toasted cake from the wok (attack and movement speed +20% for 40s, doubled hunger drain, sluggishness afterwards); a dew cake from the steamer (a mid-air jump, and a landing shockwave that scales with fall height up to 20 blocks)
  - **Balance pass from player feedback** - eight drinks pulled back toward vanilla (durations, capped immunities, per-harvest instead of per-minute), plus recipe costs raised on four of them
  - Dynamic light routing through either RyoamicLights or Sodium Dynamic Lights
- **Next**
  - **v0.4.0 - flower perches**: 27 blocks and 135 states, a five-stage growth cycle you prune with shears for a sustainable flower supply. This is where flowers stop being something you pick in the wild.
  - **v0.5.0 - fragrance system**: still, perfuming bench and scent bottles
  - **v0.6.0 - flowers and living things**
  - **v0.7.0 - hostile taming**
- **Later / exploratory**
  - Considered, not promised: the golden dandelion's own perch block once Vanilla Backport ships that flower, a rewritten bloom particle for the perches, localizations beyond en/zh

## License

- Code: [MIT](LICENSE)
- Art assets: [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)

## Porting & forks

Yes, you may port this mod - both the license and the author are fine with it:

- **Code is MIT**: port, modify, and redistribute freely; just keep the copyright notice.
- **Art is CC BY-NC-SA 4.0**: reuse requires credit and non-commercial use; adaptations must use the same license. If you would rather not carry those terms, redraw the textures.
- **Porting targets**: a Forge 1.20.1 port is welcome. Mind the two hard dependencies on modern APIs - Cookery's `TeacupRegistry` push-registration (runs in the mod constructor) and NeoForge's damage pipeline (`LivingIncomingDamageEvent` / `LivingDamageEvent.Pre`); both need Forge-era equivalents (RegistryObject flow / `LivingHurtEvent` + `LivingDamageEvent`).
- Please open an issue or ping the author so the port can be linked from this README.

## Credits

- [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) - upstream mod and required dependency

Another mod by the same author: [Create: Colored Connections](https://www.curseforge.com/minecraft/mc-mods/create-colored-connections)
