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

## Flower cake art — the project's own work

The flower cakes' item icons, placed-block textures and placed-block models are
made for this mod (the models were authored in Blockbench). Nothing third-party is
involved, so no separate attribution is required.

> **History, for the record.** While the block textures were still pending, the raw
> and dew block textures existed as *program-generated colour variants* of the
> toasted one (a hue/saturation/value transform derived from the artist's item
> icons: raw +0.8°/×0.48/×1.10, dew +124.5°/×0.46/×1.06, with the base plate left
> untouched). The artist has since drawn all three, so no generated art ships any
> more. The transform is recorded only because it is how those interim files were
> made.

## This mod's own licence

`license = "MIT"` in `neoforge.mods.toml` covers this mod's own code and assets,
and remains accurate: **nothing from a more restrictive licence is redistributed
here.** The only obligation carried over from upstream is the MIT attribution
noted above.
