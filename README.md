# 森罗物语：花香四溢 / Kaleidoscope Flora

用鲜花调制饮品，让每一种花都有属于自己的味道。

Brew drinks from flowers, and let every flower have a flavor of its own.

## 简介 / What is this

「森罗物语：花香四溢」是 [Kaleidoscope Cookery（森罗物语：厨房）](https://modrinth.com/mod/kaleidoscope-cookery) 的附属模组，以 Minecraft 原版花卉为核心拓展厨房玩法。

Kaleidoscope Flora is an addon for [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) that turns vanilla flowers into a full brewing experience.

**玩法循环 / Gameplay loop**

采集鲜花 → 汤锅调制 → 饮用获得花语效果

Gather flowers → brew them in the stockpot → drink and gain the flower's blessing.

- 26 种花语饮品：每种饮品有独立的配方、效果与一句花语格言
- 一锅 8 杯：一汤锅可盛出 8 份饮品，用森罗物语厨房的空茶杯盛出
- 特色效果：踏水而行的「凌波」、照亮黑夜的「向阳」、化身嗅探者挖掘古物的「嗅探者之魂」等
- 26 drinks, each with its own recipe, effect and a flower-language quote
- One pot yields 8 cups, ladled out with Cookery's empty teacups
- Signature effects such as walking on water, glowing at night, and digging artifacts with a sniffer's instincts

**状态 / Status**：开发中（in development）——数值与机制已基本完成，正式美术制作中。当前仓库中的贴图为程序生成的占位图，物品/方块/效果图标正在招募画师绘制。

## 环境要求 / Requirements

| 依赖 / Dependency | 版本 / Version | 必需 / Required |
|---|---|---|
| Minecraft | 1.21.1 | 是 |
| NeoForge | 21.1.248 | 是 |
| [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) | 1.4.1 | 是 |
| [Sodium Dynamic Lights](https://modrinth.com/mod/sodium-dynamic-lights) | 1.0.10 | 否（「向阳」夜间发光的完整体验） |
| Vanilla Backport | - | 否（解锁 3 种联动饮品） |

## 从源码构建 / Building from source

依赖的第三方模组 jar 通过 Modrinth maven 自动解析，无需手动下载：

Third-party mod jars are resolved automatically from the Modrinth maven - no manual download needed:

```
git clone https://github.com/Rlosking-C/kaleidoscope-flora.git
cd kaleidoscope-flora
./gradlew build
```

构建产物位于 `build/libs/`。开发调试可运行 `./gradlew runClient`。

The built jar lands in `build/libs/`. Use `./gradlew runClient` for a dev instance.

`tools/generate.ps1` 是配方 / 语言文件 / 贴图的单一数据源生成器，修改数据后重新生成全部数据驱动资源。

`tools/generate.ps1` regenerates every data-driven resource (recipes, lang files, textures) from a single source of truth.

## 许可 / License

- 代码 / Code：[MIT](LICENSE)
- 美术资源（正式版贴图）/ Final art assets：计划采用 CC BY-NC-SA 4.0（当前为占位图）

## 致谢 / Credits

- [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) — 本模组的上游与运行前置
- 森罗物语系列社区

Another mod by the same author: [Create: Colored Connections](https://www.curseforge.com/minecraft/mc-mods/create-colored-connections)
