# Credits and third-party licences

## Kaleidoscope World Liquor — the creative tab's section column

The section column in this mod's creative tab (the buttons down the left of the
tab that switch between **花饮 / Flower Drinks** and **其他 / Other**) was
designed after **Kaleidoscope World Liquor** by **"111"**.

- Upstream licence, as declared in its `neoforge.mods.toml`:
  **`MIT + CC BY-NC-ND 4.0`** — the *code* is MIT, the *assets* are Creative
  Commons Attribution-NonCommercial-NoDerivatives 4.0.
- **What this mod reuses: the code-level design only** — the two-section model,
  the placement (a 32×26 button 28 pixels left of the panel, first button 17 down,
  pitch 27, icon at +8/+5) and the select-then-refresh flow, read from its
  `CreativeTabFilter`. That is MIT, so it may be reused **with attribution**,
  which the `credits` field in `neoforge.mods.toml` and this file provide.
- **What this mod does NOT ship: any of its assets.** Its two sprites
  (`filter_tab_selected.png`, `filter_tab_unselected.png`) are **not** in this
  repository's `src/` and are **not** in any built jar. Verified by listing the
  jar: the only `textures/gui/` entries are this mod's own two files.

### Why the assets were not taken

Verbatim copies of a CC BY-NC-ND work *are* permitted (ND forbids derivatives,
not copies) provided they carry attribution and are used non-commercially. That
would nonetheless have made **the whole package** non-commercial, which conflicts
with this mod's `license = "MIT"` and would rule out paid distribution and
revenue-sharing programmes. So instead the reference was **measured and redrawn**:

- structure and metrics are recorded in
  `docs/策划文档/美术交付/草稿-侧栏按钮/README.md`;
- the shipped sprites are **this mod's own work**, in this mod's pink/plum bevel
  palette, and have different SHA256 digests from the upstream files;
- upstream textures remain available for study in the project's existing
  "look, never ship" corpus, under
  `docs/策划文档/美术交付/参考图/森罗系-官方贴图/世界酒__kaleidoscope-world-liquor/`.

## Flower cake block textures — two of the three are program-generated

`textures/block/toasted_flower_cake.png` is the artist's own work. The other two,
`textures/block/raw_flower_cake.png` and `textures/block/dew_flower_cake.png`,
are **program-generated colour variants of it** — the same pixels, with a
hue/saturation/value transform applied per pixel (alpha copied verbatim, so the
UV layout and every edge stay exactly where the artist put them).

The transform was **derived from the artist's own item icons**, not chosen by
hand: the mean hue, saturation and value of the chromatic pixels of
`textures/item/toasted_flower_cake.png` were compared with those of the raw and
dew item icons, and that ratio was applied to the block texture. The three placed
cakes therefore read as one object in three states:

| target | hue | saturation | value |
| --- | --- | --- | --- |
| raw | +0.8° | ×0.48 | ×1.10 |
| dew | +124.5° | ×0.46 | ×1.06 |

**They are placeholders.** No hand-drawn block textures for the raw and dew cakes
have been delivered; when they are, they replace these files and this section
goes away. The numbers above are recorded so the variants can be regenerated if
the toasted texture is revised (there is no shipped tool for it — the one-off
script was not kept).

## This mod's own licence

`license = "MIT"` in `neoforge.mods.toml` covers this mod's own code and assets,
and remains accurate: **nothing from a more restrictive licence is redistributed
here.** The only obligation carried over from upstream is the MIT attribution
noted above.
