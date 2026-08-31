# Kaleidoscope Flora asset generator.
# Generates all 26 drinks (blockstates, models, placeholder textures,
# stockpot recipes, item tags, lang files) from the dandelion_tea template
# shipped with the framework, then deletes the placeholder drinks and every
# file belonging to the abandoned teapot/override approach.
# Re-run any time after real art replaces the placeholder textures.

$ErrorActionPreference = "Stop"
$root   = "D:\GAMES\MCMod\Kaleidoscope Flora\src\main\resources"
$assets = Join-Path $root "assets\kaleidoscope_flora"
$data   = Join-Path $root "data\kaleidoscope_flora"
$utf8   = New-Object System.Text.UTF8Encoding($false)
Add-Type -AssemblyName System.Drawing

# ----------------------------------------------------------------------
# 1. Load the geometry templates (BEFORE the placeholder drinks are deleted)
# ----------------------------------------------------------------------
$tplState  = Get-Content "$assets\blockstates\dandelion_tea.json" -Raw
$tplModels = @{}
Get-ChildItem "$assets\models\block\teacup\dandelion_tea" -Filter *.json | ForEach-Object {
    $tplModels[$_.BaseName] = Get-Content $_.FullName -Raw
}

# ----------------------------------------------------------------------
# 2. The drink table: id -> colour, ingredients, soup base, servings
# ----------------------------------------------------------------------
$drinks = [ordered]@{
    "when_the_wind_rises" = @{ color = "FED83D"; soup = "minecraft:water"; count = 8; items = @("minecraft:dandelion","minecraft:dandelion","minecraft:dandelion","minecraft:honey_bottle","minecraft:sugar") }
    "lullaby"             = @{ color = "B02E26"; soup = "minecraft:water"; count = 8; items = @("minecraft:poppy","minecraft:poppy","minecraft:poppy","minecraft:poppy","minecraft:honey_bottle") }
    "first_bloom"         = @{ color = "3AB3DA"; soup = "minecraft:water"; count = 8; items = @("minecraft:blue_orchid","minecraft:blue_orchid","minecraft:blue_orchid","minecraft:sugar","minecraft:glow_berries") }
    "fire_waltz"          = @{ color = "B57EDC"; soup = "minecraft:water"; count = 8; items = @("minecraft:allium","minecraft:allium","minecraft:allium","minecraft:allium","minecraft:glow_berries") }
    "the_unnoticed"       = @{ color = "E5E5DC"; soup = "minecraft:water"; count = 8; items = @("minecraft:azure_bluet","minecraft:azure_bluet","minecraft:azure_bluet","minecraft:azure_bluet","minecraft:azure_bluet","minecraft:phantom_membrane") }
    "crimson_heartbeat"   = @{ color = "E4413C"; soup = "minecraft:water"; count = 8; items = @("minecraft:red_tulip","minecraft:red_tulip","minecraft:red_tulip","minecraft:red_tulip","minecraft:sweet_berries","minecraft:sweet_berries") }
    "autumn_serenade"     = @{ color = "E8863A"; soup = "minecraft:water"; count = 8; items = @("minecraft:orange_tulip","minecraft:orange_tulip","minecraft:orange_tulip","minecraft:orange_tulip","minecraft:carrot","minecraft:sugar") }
    "absolution"          = @{ color = "E9E9E9"; soup = "minecraft:water"; count = 8; items = @("minecraft:white_tulip","minecraft:white_tulip","minecraft:white_tulip","minecraft:white_tulip","minecraft:honey_bottle") }
    "rosy_stride"         = @{ color = "EBA2C8"; soup = "minecraft:water"; count = 8; items = @("minecraft:pink_tulip","minecraft:pink_tulip","minecraft:pink_tulip","minecraft:pink_tulip","minecraft:sugar","minecraft:feather") }
    "loves_me_not"        = @{ color = "F2EFC5"; soup = "minecraft:water"; count = 8; items = @("minecraft:oxeye_daisy","minecraft:oxeye_daisy","minecraft:oxeye_daisy","minecraft:oxeye_daisy","minecraft:oxeye_daisy","minecraft:rabbit_foot") }
    "prussian_leap"       = @{ color = "4B6EE1"; soup = "minecraft:water"; count = 8; items = @("minecraft:cornflower","minecraft:cornflower","minecraft:cornflower","minecraft:cornflower","minecraft:feather","minecraft:sugar") }
    "may_kiss"            = @{ color = "E8F2E1"; soup = "minecraft:water"; count = 8; items = @("minecraft:lily_of_the_valley","minecraft:lily_of_the_valley","minecraft:lily_of_the_valley","minecraft:lily_of_the_valley","minecraft:honey_bottle") }
    "fleurs_du_mal"       = @{ color = "2E2A2E"; soup = "minecraft:water"; count = 8; items = @("minecraft:wither_rose","minecraft:wither_rose","minecraft:wither_rose","minecraft:wither_rose","minecraft:bone","minecraft:sugar") }
    "breath_of_ancients"  = @{ color = "F26419"; soup = "minecraft:lava"; count = 8; items = @("minecraft:torchflower","minecraft:torchflower","minecraft:honey_bottle","minecraft:glow_berries") }
    "the_sunward"         = @{ color = "FFC814"; soup = "minecraft:water"; count = 8; items = @("minecraft:sunflower","minecraft:sunflower","minecraft:sunflower","minecraft:glow_berries","minecraft:honey_bottle") }
    "spring_waltz"        = @{ color = "C5A3D0"; soup = "minecraft:water"; count = 8; items = @("minecraft:lilac","minecraft:lilac","minecraft:lilac","minecraft:sweet_berries","minecraft:sweet_berries","minecraft:honey_bottle") }
    "tender_thorns"       = @{ color = "A61E22"; soup = "minecraft:water"; count = 8; items = @("minecraft:rose_bush","minecraft:rose_bush","minecraft:rose_bush","minecraft:rose_bush","minecraft:sweet_berries","minecraft:sweet_berries") }
    "coronation"          = @{ color = "E56DB1"; soup = "minecraft:water"; count = 8; items = @("minecraft:peony","minecraft:peony","minecraft:peony","minecraft:gold_nugget","minecraft:sugar") }
    "voracious_urn"      = @{ color = "5E4F8C"; soup = "minecraft:cod_bucket"; count = 8; items = @("minecraft:pitcher_plant","minecraft:pitcher_plant","minecraft:pitcher_plant","minecraft:spider_eye","minecraft:spider_eye") }
    "hanami_tale"         = @{ color = "F7C6D9"; soup = "kaleidoscope_flora:milk"; count = 8; items = @("minecraft:pink_petals","minecraft:pink_petals","minecraft:pink_petals","minecraft:sugar","minecraft:honey_bottle") }
    "echo_of_the_end"     = @{ color = "D6CBB4"; soup = "minecraft:water"; count = 8; items = @("minecraft:chorus_flower","minecraft:chorus_flower","minecraft:chorus_flower","minecraft:ender_pearl") }
    "vernal_awakening"    = @{ color = "ED9FC0"; soup = "minecraft:water"; count = 8; items = @("minecraft:spore_blossom","minecraft:spore_blossom","minecraft:bone_meal","minecraft:glow_berries") }
    "the_gaze"            = @{ color = "D9E8EA"; soup = "minecraft:water"; count = 8; items = @(@{tag="kaleidoscope_flora:eyeblossoms"},@{tag="kaleidoscope_flora:eyeblossoms"},@{tag="kaleidoscope_flora:eyeblossoms"},"minecraft:ghast_tear","minecraft:sugar") }
    "as_you_wish"         = @{ color = "FFDD44"; soup = "minecraft:water"; count = 8; items = @(@{tag="kaleidoscope_flora:golden_dandelions"},@{tag="kaleidoscope_flora:golden_dandelions"},"minecraft:gold_nugget","minecraft:honey_bottle") }
    "springtime_stroll"   = @{ color = "8FD05A"; soup = "minecraft:water"; count = 8; items = @(@{tag="kaleidoscope_flora:wildflowers"},@{tag="kaleidoscope_flora:wildflowers"},"minecraft:bone_meal","minecraft:honey_bottle") }
    "fleeting_bloom"      = @{ color = "E3D45A"; soup = "minecraft:water"; count = 8; items = @(@{tag="kaleidoscope_flora:cactus_flowers"},@{tag="kaleidoscope_flora:cactus_flowers"},"minecraft:sugar","minecraft:glow_berries") }
}

# ----------------------------------------------------------------------
# 3. Placeholder textures (solid colour; real art replaces these later)
# ----------------------------------------------------------------------
function Make-ItemTexture([string]$path, [string]$hex) {
    $bmp = New-Object System.Drawing.Bitmap(16, 16)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $brush = New-Object System.Drawing.SolidBrush([System.Drawing.ColorTranslator]::FromHtml("#$hex"))
    $g.FillEllipse($brush, 1, 1, 14, 14)
    $g.Dispose(); $brush.Dispose()
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Make-BlockTexture([string]$path, [string]$hex) {
    $bmp = New-Object System.Drawing.Bitmap(32, 32)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $color = [System.Drawing.ColorTranslator]::FromHtml("#$hex")
    $brush = New-Object System.Drawing.SolidBrush($color)
    $g.FillRectangle($brush, 0, 0, 32, 32)
    # Slightly darker rim so the teacup silhouette is at least readable.
    $dark = [System.Drawing.Color]::FromArgb(255, [int]($color.R * 0.65), [int]($color.G * 0.65), [int]($color.B * 0.65))
    $pen = New-Object System.Drawing.Pen($dark, 2.0)
    $g.DrawRectangle($pen, 1, 1, 30, 30)
    $g.Dispose(); $brush.Dispose(); $pen.Dispose()
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# Stockpot surface strips: Cookery's water-based ripple animation recoloured
# to each drink's colour, so every drink visibly cooks in its own colour.
# Source: kaleidoscope_cookery-1.4.1.jar textures/stockpot/default_*.png
Add-Type -AssemblyName System.IO.Compression.FileSystem
$cookeryJar = Join-Path $PSScriptRoot "..\libs\kaleidoscope_cookery-1.4.1.jar"

function Get-CookeryStrip([string]$entryName) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $cookeryJar))
    try {
        $entry = $zip.GetEntry($entryName)
        if (-not $entry) { throw "missing $entryName in cookery jar" }
        $ms = New-Object System.IO.MemoryStream
        $entry.Open().CopyTo($ms); $ms.Position = 0
        $bmp = New-Object System.Drawing.Bitmap($ms)
        $ms.Dispose()
        return $bmp
    } finally { $zip.Dispose() }
}

function Convert-StripColour([System.Drawing.Bitmap]$bmp, [int[]]$dark, [int[]]$light) {
    $rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $bytes = New-Object byte[] ($data.Stride * $data.Height)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
    for ($i = 0; $i -lt $bytes.Length; $i += 4) {
        if ($bytes[$i + 3] -eq 0) { continue }
        # Keep the ripple/bubble structure: map source brightness onto the drink ramp.
        $r = $bytes[$i + 2]; $g = $bytes[$i + 1]; $b = $bytes[$i]     # BGRA
        $lum = [Math]::Max($r, [Math]::Max($g, $b))
        $n = [Math]::Pow($lum / 255.0, 0.85)
        $bytes[$i + 2] = [byte]($dark[0] + ($light[0] - $dark[0]) * $n)
        $bytes[$i + 1] = [byte]($dark[1] + ($light[1] - $dark[1]) * $n)
        $bytes[$i]     = [byte]($dark[2] + ($light[2] - $dark[2]) * $n)
    }
    [System.Runtime.InteropServices.Marshal]::Copy($bytes, 0, $data.Scan0, $bytes.Length)
    $bmp.UnlockBits($data)
}

function Make-SoupTextures([string]$id, [string]$hex) {
    $color = [System.Drawing.ColorTranslator]::FromHtml("#$hex")
    # Shadow end is the drink colour deepened; highlight end leans to white.
    $dark  = @([int]($color.R * 0.58), [int]($color.G * 0.58), [int]($color.B * 0.58))
    $light = @([int]($color.R * 0.7 + 76), [int]($color.G * 0.7 + 76), [int]($color.B * 0.7 + 76))
    $outDir = "$assets\textures\stockpot"
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    foreach ($pair in @{
        "default_cooking.png"  = @{ out = "${id}_cooking.png";  frametime = 1 }
        "default_finished.png" = @{ out = "${id}_finished.png"; frametime = 2 }
    }.GetEnumerator()) {
        $bmp = Get-CookeryStrip "assets/kaleidoscope_cookery/textures/stockpot/$($pair.Key)"
        Convert-StripColour $bmp $dark $light
        $bmp.Save("$outDir\$($pair.Value.out)", [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
        Set-Content -Path "$outDir\$($pair.Value.out).mcmeta" -Value "{`n  `"animation`": { `"frametime`": $($pair.Value.frametime) }`n}"
    }
}

# Milk base surface (white) shown while ingredients are being dropped into a
# milk-base pot, before cooking starts. Rendered by MilkSoupBaseRender.
function Make-MilkTexture() {
    $dark  = @(0xE8, 0xDF, 0xD3); $light = @(0xFF, 0xFD, 0xF7)
    $outDir = "$assets\textures\stockpot"
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $bmp = Get-CookeryStrip "assets/kaleidoscope_cookery/textures/stockpot/default_cooking.png"
    Convert-StripColour $bmp $dark $light
    $bmp.Save("$outDir\milk_cooking.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Set-Content -Path "$outDir\milk_cooking.png.mcmeta" -Value "{`n  `"animation`": { `"frametime`": 1 }`n}"
}

# ----------------------------------------------------------------------
# 4. Generate everything per drink
# ----------------------------------------------------------------------
New-Item -ItemType Directory -Force -Path "$assets\textures\block\teacup", "$assets\textures\item", "$data\recipe\stockpot", "$data\tags\item" | Out-Null

foreach ($id in $drinks.Keys) {
    $d = $drinks[$id]

    # 4.1 blockstate (48 variants from the template)
    [System.IO.File]::WriteAllText("$assets\blockstates\$id.json", $tplState.Replace("dandelion_tea", $id), $utf8)

    # 4.2 the 10 geometry-identical block models
    $dir = "$assets\models\block\teacup\$id"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    foreach ($k in $tplModels.Keys) {
        [System.IO.File]::WriteAllText("$dir\$k.json", $tplModels[$k].Replace("dandelion_tea", $id), $utf8)
    }

    # 4.3 item model
    $itemModel = "{`n  `"parent`": `"minecraft:item/generated`",`n  `"textures`": {`n    `"layer0`": `"kaleidoscope_flora:item/$id`"`n  }`n}`n"
    [System.IO.File]::WriteAllText("$assets\models\item\$id.json", $itemModel, $utf8)

    # 4.4 placeholder textures
    Make-ItemTexture "$assets\textures\item\$id.png" $d.color
    Make-BlockTexture "$assets\textures\block\teacup\$id.png" $d.color
    Make-SoupTextures $id $d.color

    # 4.5 stockpot recipe
    # NOTE: Cookery's StockpotRecipeSerializer binds StockpotVisuals.CODEC
    # directly at the recipe ROOT (MapCodec.forGetter without fieldOf), so
    # cooking_texture / finished_texture / *_bubble_color must be written as
    # top-level keys - nesting them under a "visuals" object gets silently
    # ignored and every pot falls back to the default pink water texture.
    $ings = @()
    foreach ($ing in $d.items) {
        if ($ing -is [string]) { $ings += @{ item = $ing } } else { $ings += $ing }
    }
    $bubble = 0xFF000000 + ([Convert]::ToInt32($d.color.Substring(0, 2), 16) -shl 16) +
              ([Convert]::ToInt32($d.color.Substring(2, 2), 16) -shl 8) +
              [Convert]::ToInt32($d.color.Substring(4, 2), 16)
    $recipe = [ordered]@{
        type                 = "kaleidoscope_cookery:stockpot"
        carrier              = @{ item = "kaleidoscope_cookery:empty_cup" }
        ingredients          = $ings
        soup_base            = $d.soup
        cooking_texture      = "kaleidoscope_flora:stockpot/${id}_cooking"
        finished_texture     = "kaleidoscope_flora:stockpot/${id}_finished"
        cooking_bubble_color = $bubble
        finished_bubble_color = $bubble
        time                 = 240
        result               = @{ count = $d.count; id = "kaleidoscope_flora:$id" }
    }
    [System.IO.File]::WriteAllText("$data\recipe\stockpot\$id.json", ($recipe | ConvertTo-Json -Depth 6) + "`n", $utf8)
}

# Milk base surface for the ingredient-drop stage of milk-base pots.
Make-MilkTexture

# ----------------------------------------------------------------------
# 5. Item tags for flowers that do not exist on vanilla 1.21.1.
#    Unknown ids inside a tag are simply ignored, so these recipes are
#    inert without VanillaBackport and alive with it (or on newer MC).
# ----------------------------------------------------------------------
$tags = [ordered]@{
    "eyeblossoms"        = @("minecraft:closed_eyeblossom", "minecraft:open_eyeblossom")
    "golden_dandelions"  = @("minecraft:golden_dandelion")
    "wildflowers"        = @("minecraft:wildflowers")
    "cactus_flowers"     = @("minecraft:cactus_flower")
}
foreach ($t in $tags.Keys) {
    $json = @{ replace = $false; values = $tags[$t] } | ConvertTo-Json -Depth 4
    [System.IO.File]::WriteAllText("$data\tags\item\$t.json", $json + "`n", $utf8)
}

# ----------------------------------------------------------------------
# 6. Lang files (names + maxims + effect names)
# ----------------------------------------------------------------------
$en = [ordered]@{
    "block.kaleidoscope_flora.when_the_wind_rises"   = "When the Wind Rises"
    "tooltip.kaleidoscope_flora.when_the_wind_rises.maxim" = "The earth delights to feel your bare feet, and the winds long to play with your hair. - Kahlil Gibran, The Prophet (Lebanon)"
    "block.kaleidoscope_flora.lullaby"               = "Lullaby"
    "tooltip.kaleidoscope_flora.lullaby.maxim" = "In dreams I forget I am a guest, and snatch a moment of joy. - Li Yu (China)"
    "block.kaleidoscope_flora.first_bloom"           = "First Bloom"
    "tooltip.kaleidoscope_flora.first_bloom.maxim" = "Coffee is our bread. - Amharic proverb (Ethiopia)"
    "block.kaleidoscope_flora.fire_waltz"            = "Fire Waltz"
    "tooltip.kaleidoscope_flora.fire_waltz.maxim" = "The bear that ate garlic and mugwort became human. - Dangun myth (Korea)"
    "block.kaleidoscope_flora.the_unnoticed"         = "The Unnoticed"
    "tooltip.kaleidoscope_flora.the_unnoticed.maxim" = "I'm Nobody! Who are you? - Emily Dickinson (USA)"
    "block.kaleidoscope_flora.crimson_heartbeat"     = "Crimson Heartbeat"
    "tooltip.kaleidoscope_flora.crimson_heartbeat.maxim" = "Where the lover's blood fell, red tulips grew. - Persian legend of Farhad (Persia)"
    "block.kaleidoscope_flora.autumn_serenade"       = "Autumn Serenade"
    "tooltip.kaleidoscope_flora.autumn_serenade.maxim" = "Before the gates of excellence, the gods have placed sweat. - Hesiod, Works and Days (Greece)"
    "block.kaleidoscope_flora.absolution"            = "Absolution"
    "tooltip.kaleidoscope_flora.absolution.maxim" = "The weak can never forgive; forgiveness is the attribute of the strong. - Gandhi (India)"
    "block.kaleidoscope_flora.rosy_stride"           = "Rosy Stride"
    "tooltip.kaleidoscope_flora.rosy_stride.maxim" = "The girl born inside a tulip sailed the stream on its petal. - Andersen, Thumbelina (Denmark)"
    "block.kaleidoscope_flora.loves_me_not"          = "Loves Me Not"
    "tooltip.kaleidoscope_flora.loves_me_not.maxim" = "Pluck one - loves me; pluck another - loves me not. - European divination game"
    "block.kaleidoscope_flora.prussian_leap"         = "The Prussian Leap"
    "tooltip.kaleidoscope_flora.prussian_leap.maxim" = "Once you have tasted flight, you will forever walk the earth with your eyes turned skyward. - Leonardo da Vinci (Italy)"
    "block.kaleidoscope_flora.may_kiss"              = "May Kiss"
    "tooltip.kaleidoscope_flora.may_kiss.maxim" = "I am the rose of Sharon, and the lily of the valleys. - Song of Songs (ancient Israel)"
    "block.kaleidoscope_flora.fleurs_du_mal"         = "Les Fleurs du Mal"
    "tooltip.kaleidoscope_flora.fleurs_du_mal.maxim" = "Some flowers bloom more brightly on graves than in gardens. - after Baudelaire, Les Fleurs du Mal (France)"
    "block.kaleidoscope_flora.breath_of_ancients"    = "Breath of the Ancients"
    "tooltip.kaleidoscope_flora.breath_of_ancients.maxim" = "To speak the name of the dead is to make them live again. - Egyptian Book of the Dead"
    "block.kaleidoscope_flora.the_sunward"           = "The Sunward"
    "tooltip.kaleidoscope_flora.the_sunward.maxim" = "The sunflower is mine. - Vincent van Gogh (Netherlands)"
    "block.kaleidoscope_flora.spring_waltz"          = "Spring Waltz"
    "tooltip.kaleidoscope_flora.spring_waltz.maxim" = "Spring has returned. The Earth is like a child that knows poems. - Rainer Maria Rilke (Austria)"
    "block.kaleidoscope_flora.tender_thorns"        = "Tender Thorns"
    "tooltip.kaleidoscope_flora.tender_thorns.maxim" = "O my luve is like a red, red rose. - Robert Burns (Scotland)"
    "block.kaleidoscope_flora.coronation"            = "Coronation"
    "tooltip.kaleidoscope_flora.coronation.maxim" = "Gold is the sweat of the sun. - Inca saying"
    "block.kaleidoscope_flora.voracious_urn"         = "The Voracious Urn"
    "tooltip.kaleidoscope_flora.voracious_urn.maxim" = "I care more about Drosera than the origin of all the species in the world. - Charles Darwin, letter (UK)"
    "block.kaleidoscope_flora.hanami_tale"           = "Hanami Tale"
    "tooltip.kaleidoscope_flora.hanami_tale.maxim" = "Falling cherry blossoms, blossoms yet to fall - all shall fall. - Ryokan, death poem (Japan)"
    "block.kaleidoscope_flora.echo_of_the_end"       = "Echo of the End"
    "tooltip.kaleidoscope_flora.echo_of_the_end.maxim" = "The gods gave death to mankind, and kept life for themselves. - Gilgamesh (Mesopotamia)"
    "block.kaleidoscope_flora.vernal_awakening"     = "Vernal Awakening"
    "tooltip.kaleidoscope_flora.vernal_awakening.maxim" = "When you plant a seed, believe in the whole spring. - farming proverb"
    "block.kaleidoscope_flora.the_gaze"             = "The Gaze"
    "tooltip.kaleidoscope_flora.the_gaze.maxim" = "If you gaze long into an abyss, the abyss gazes also into you. - Nietzsche (Germany)"
    "block.kaleidoscope_flora.as_you_wish"           = "As You Wish"
    "tooltip.kaleidoscope_flora.as_you_wish.maxim" = "He who polishes the lamp is granted one wish. - One Thousand and One Nights (Arabia)"
    "block.kaleidoscope_flora.springtime_stroll"     = "Springtime Stroll"
    "tooltip.kaleidoscope_flora.springtime_stroll.maxim" = "Wanderer, there is no road; the road is made by walking. - Antonio Machado (Spain)"
    "block.kaleidoscope_flora.fleeting_bloom"       = "Fleeting Bloom"
    "tooltip.kaleidoscope_flora.fleeting_bloom.maxim" = "Do we truly live on earth? Not forever on earth, only a little while here. - Nezahualcoyotl (Aztec)"
    # custom effect names
    "effect.kaleidoscope_flora.purge"         = "Purge"
    "effect.kaleidoscope_flora.fadeaway"      = "Fadeaway"
    "effect.kaleidoscope_flora.drowsy"        = "Drowsiness"
    "effect.kaleidoscope_flora.tastebloom"    = "Taste in Bloom"
    "effect.kaleidoscope_flora.firebrand"     = "Fiery Brand"
    "effect.kaleidoscope_flora.vampiric"      = "Vampiric"
    "effect.kaleidoscope_flora.harvest"       = "Harvest"
    "effect.kaleidoscope_flora.absolve"       = "Absolution"
    "effect.kaleidoscope_flora.petalwalk"     = "Petal Stride"
    "effect.kaleidoscope_flora.divination"    = "Divination"
    "effect.kaleidoscope_flora.featherfall"   = "Featherfall"
    "effect.kaleidoscope_flora.kiss"          = "Lingering Kiss"
    "effect.kaleidoscope_flora.wither_aura"   = "Withering Aura"
    "effect.kaleidoscope_flora.sniffer_soul"  = "Sniffer's Soul"
    "effect.kaleidoscope_flora.sunward"       = "Sunward"
    "effect.kaleidoscope_flora.thorns"        = "Thorns"
    "effect.kaleidoscope_flora.digestion"     = "Digestion"
    "effect.kaleidoscope_flora.petal_veil"    = "Petal Veil"
    "effect.kaleidoscope_flora.echo"          = "Echo"
    "effect.kaleidoscope_flora.sprout"        = "Sprouting"
    "effect.kaleidoscope_flora.gaze"          = "Gaze"
    "effect.kaleidoscope_flora.wish"          = "Wish Granted"
    "effect.kaleidoscope_flora.flower_path"   = "Flower Path"
}

$zh = [ordered]@{
    "block.kaleidoscope_flora.when_the_wind_rises"   = "风起时"
    "tooltip.kaleidoscope_flora.when_the_wind_rises.maxim" = "大地喜欢感受你赤足的触碰，风渴望与你发丝嬉戏。——纪伯伦《先知》（黎巴嫩）"
    "block.kaleidoscope_flora.lullaby"               = "安眠曲"
    "tooltip.kaleidoscope_flora.lullaby.maxim" = "梦里不知身是客，一晌贪欢。——李煜《浪淘沙令》（中国）"
    "block.kaleidoscope_flora.first_bloom"           = "初绽"
    "tooltip.kaleidoscope_flora.first_bloom.maxim" = "咖啡即是我们的面包。——阿姆哈拉谚语（埃塞俄比亚）"
    "block.kaleidoscope_flora.fire_waltz"            = "火焰圆舞"
    "tooltip.kaleidoscope_flora.fire_waltz.maxim" = "吃了蒜与艾草的熊，变成了人。——檀君神话（朝鲜半岛）"
    "block.kaleidoscope_flora.the_unnoticed"         = "无人知晓"
    "tooltip.kaleidoscope_flora.the_unnoticed.maxim" = "我是无名之辈。你是谁？——艾米莉·狄金森（美国）"
    "block.kaleidoscope_flora.crimson_heartbeat"     = "绯色心跳"
    "tooltip.kaleidoscope_flora.crimson_heartbeat.maxim" = "恋人的血落之处，开出红郁金香。——波斯传说《霍斯罗与希琳》（波斯）"
    "block.kaleidoscope_flora.autumn_serenade"       = "金秋小夜曲"
    "tooltip.kaleidoscope_flora.autumn_serenade.maxim" = "在卓越之门前，诸神安放了汗水。——赫西俄德《工作与时日》（古希腊）"
    "block.kaleidoscope_flora.absolution"            = "赦免"
    "tooltip.kaleidoscope_flora.absolution.maxim" = "弱者永不宽恕，宽恕是强者的属性。——甘地（印度）"
    "block.kaleidoscope_flora.rosy_stride"           = "霞光漫步"
    "tooltip.kaleidoscope_flora.rosy_stride.maxim" = "郁金香花苞里出生的姑娘，乘着花瓣渡过溪流。——安徒生《拇指姑娘》（丹麦）"
    "block.kaleidoscope_flora.loves_me_not"          = "爱我，不爱我"
    "tooltip.kaleidoscope_flora.loves_me_not.maxim" = "摘一片，爱我；再摘一片，不爱我。——欧洲占卜游戏"
    "block.kaleidoscope_flora.prussian_leap"         = "普鲁士之跃"
    "tooltip.kaleidoscope_flora.prussian_leap.maxim" = "尝过飞行滋味的人，行走大地时将永远仰望天空。——列奥纳多·达·芬奇（意大利）"
    "block.kaleidoscope_flora.may_kiss"              = "五月之吻"
    "tooltip.kaleidoscope_flora.may_kiss.maxim" = "我是沙仑的玫瑰，是谷中的百合。——《雅歌》（古以色列）"
    "block.kaleidoscope_flora.fleurs_du_mal"         = "恶之花"
    "tooltip.kaleidoscope_flora.fleurs_du_mal.maxim" = "有些花开在坟茔上，比花园里更艳。——夏尔·波德莱尔《恶之花》（法国）"
    "block.kaleidoscope_flora.breath_of_ancients"    = "上古之息"
    "tooltip.kaleidoscope_flora.breath_of_ancients.maxim" = "说出亡者的名字，便让他们再度活着。——《亡灵书》（古埃及）"
    "block.kaleidoscope_flora.the_sunward"           = "逐日者"
    "tooltip.kaleidoscope_flora.the_sunward.maxim" = "向日葵是属于我的花。——文森特·梵高（荷兰）"
    "block.kaleidoscope_flora.spring_waltz"          = "春之圆舞"
    "tooltip.kaleidoscope_flora.spring_waltz.maxim" = "春天回来了。大地像一个熟知诗篇的孩子。——里尔克（奥地利）"
    "block.kaleidoscope_flora.tender_thorns"        = "带刺的温柔"
    "tooltip.kaleidoscope_flora.tender_thorns.maxim" = "我的爱人像一朵红红的玫瑰。——罗伯特·彭斯（苏格兰）"
    "block.kaleidoscope_flora.coronation"            = "花之加冕"
    "tooltip.kaleidoscope_flora.coronation.maxim" = "黄金是太阳的汗水。——印加箴言（印加）"
    "block.kaleidoscope_flora.voracious_urn"         = "贪婪之瓮"
    "tooltip.kaleidoscope_flora.voracious_urn.maxim" = "此刻我关心食虫植物，胜过世上一切物种的起源。——查尔斯·达尔文书信（英国）"
    "block.kaleidoscope_flora.hanami_tale"           = "花见物语"
    "tooltip.kaleidoscope_flora.hanami_tale.maxim" = "凋落的樱，未落的樱，终将凋落。——良宽辞世句（日本）"
    "block.kaleidoscope_flora.echo_of_the_end"       = "末地回响"
    "tooltip.kaleidoscope_flora.echo_of_the_end.maxim" = "众神造人时，把死亡赐给人，把生命留在自己手中。——《吉尔伽美什》（美索不达米亚）"
    "block.kaleidoscope_flora.vernal_awakening"     = "万物萌发"
    "tooltip.kaleidoscope_flora.vernal_awakening.maxim" = "你埋下一粒种子，就要相信整个春天。——农谚（佚名）"
    "block.kaleidoscope_flora.the_gaze"             = "凝视"
    "tooltip.kaleidoscope_flora.the_gaze.maxim" = "当你凝视深渊时，深渊也在凝视你。——尼采《善恶的彼岸》（德国）"
    "block.kaleidoscope_flora.as_you_wish"           = "如愿"
    "tooltip.kaleidoscope_flora.as_you_wish.maxim" = "擦亮神灯的人，只许一个愿望。——《一千零一夜》（阿拉伯）"
    "block.kaleidoscope_flora.springtime_stroll"     = "踏春"
    "tooltip.kaleidoscope_flora.springtime_stroll.maxim" = "行者啊，世上本没有路，路是走出来的。——安东尼奥·马查多（西班牙）"
    "block.kaleidoscope_flora.fleeting_bloom"       = "刹那芳华"
    "tooltip.kaleidoscope_flora.fleeting_bloom.maxim" = "人当真活在大地上吗？不会永远在大地上，只是短暂停留。——内萨瓦尔科约特尔（阿兹特克）"
    "effect.kaleidoscope_flora.purge"         = "净化"
    "effect.kaleidoscope_flora.fadeaway"      = "消隐"
    "effect.kaleidoscope_flora.drowsy"        = "困倦"
    "effect.kaleidoscope_flora.tastebloom"    = "味觉绽放"
    "effect.kaleidoscope_flora.firebrand"     = "辛焰"
    "effect.kaleidoscope_flora.vampiric"      = "吸血"
    "effect.kaleidoscope_flora.harvest"       = "丰收"
    "effect.kaleidoscope_flora.absolve"       = "赦免"
    "effect.kaleidoscope_flora.petalwalk"     = "凌波"
    "effect.kaleidoscope_flora.divination"    = "占卜"
    "effect.kaleidoscope_flora.featherfall"   = "轻身"
    "effect.kaleidoscope_flora.kiss"          = "香吻"
    "effect.kaleidoscope_flora.wither_aura"   = "凋零光环"
    "effect.kaleidoscope_flora.sniffer_soul"  = "嗅探者之魂"
    "effect.kaleidoscope_flora.sunward"       = "向阳"
    "effect.kaleidoscope_flora.thorns"        = "荆棘"
    "effect.kaleidoscope_flora.digestion"     = "消化"
    "effect.kaleidoscope_flora.petal_veil"    = "落英"
    "effect.kaleidoscope_flora.echo"          = "回响"
    "effect.kaleidoscope_flora.sprout"        = "萌发"
    "effect.kaleidoscope_flora.gaze"          = "凝视"
    "effect.kaleidoscope_flora.wish"          = "如愿"
    "effect.kaleidoscope_flora.flower_path"   = "花开随行"
}

# Maxims keep their source attribution in the data tables above (for
# reference), but the shipped lang files show the bare quote only:
# "梦里不知身是客，一晌贪欢。" not "……。——李煜《浪淘沙令》（中国）".
foreach ($table in @($en, $zh)) {
    foreach ($k in @($table.Keys)) {
        if ($k -notmatch "\.maxim$") { continue }
        $v = $table[$k]
        $cut = $v.LastIndexOf("——"); if ($cut -gt 0) { $v = $v.Substring(0, $cut) }
        $cut = $v.LastIndexOf(" - "); if ($cut -gt 0) { $v = $v.Substring(0, $cut) }
        $table[$k] = $v.TrimEnd()
    }
}

# One-line mechanical descriptions shown on the drink tooltip, right below
# the maxim. They describe WHAT the drink does, not HOW; durations are
# already printed by the vanilla-style effect lines, so they are not
# repeated here. Order: id = zh, en.
$descriptions = [ordered]@{
    when_the_wind_rises  = @("饮用瞬间清除全部负面效果，并获得缓降。", "Instantly clears all negative effects and grants Slow Falling.")
    lullaby              = @("7格内的敌对生物陷入困倦，幻翼不再索敌。", "Hostiles within 7 blocks become drowsy; phantoms stop hunting.")
    first_bloom          = @("食用的食物额外恢复一半的饥饿与饱和。", "Eaten food restores half again as much hunger and saturation.")
    fire_waltz           = @("近战攻击点燃目标。", "Melee hits ignite the target.")
    the_unnoticed        = @("饮用瞬间清除所有生物对你的敌意。", "Instantly clears all mob hostility toward you.")
    crimson_heartbeat    = @("近战伤害的一部分转化为生命。", "A share of your melee damage returns as health.")
    autumn_serenade      = @("收获完全成熟的作物时掉落物翻倍。", "Harvesting mature crops multiplies their drops.")
    absolution           = @("免疫新施加的负面效果。", "Newly applied negative effects are blocked.")
    rosy_stride          = @("在水面行走、奔跑与跳跃，潜行可下沉。", "Walk, sprint and jump on water; sneak to sink.")
    loves_me_not         = @("每8秒获得一次随机的小眷顾。", "A small random boon every 8 seconds.")
    prussian_leap        = @("持续期间免疫坠落伤害。", "Immune to fall damage while active.")
    may_kiss             = @("受击时向四周释放毒云。", "Releases a poison cloud when struck.")
    fleurs_du_mal        = @("周围5格内的生物持续凋零，你自身免疫。", "Nearby beings wither within 5 blocks; you are immune.")
    breath_of_ancients   = @("手持刷子右键泥土族方块可挖掘古物。", "Brush dirt-family blocks to excavate artifacts.")
    the_sunward          = @("白天露天缓慢回复生命，夜晚照亮四周。", "Regenerates under the open day sky; glows at night.")
    spring_waltz         = @("免疫冻结与细雪伤害。", "Immune to freezing and powder snow.")
    tender_thorns        = @("攻击你的生物受到反伤与虚弱。", "Attackers take damage and Weakness.")
    coronation           = @("幸运眷顾，金色粒子环绕。", "Grants Luck with gold particles.")
    voracious_urn        = @("你的击杀视为掠夺等级提高一级。", "Your kills count as one level higher Looting.")
    hanami_tale          = @("花瓣环绕周身，格挡来袭的弹射物。", "Orbiting petals block incoming projectiles.")
    echo_of_the_end      = @("受到重击时随机传送。", "Teleports you when struck hard.")
    vernal_awakening     = @("周围的作物随机催熟。", "Ripens nearby crops at random.")
    the_gaze             = @("25格内的所有生物发光，可透视。", "All beings within 25 blocks glow through walls.")
    as_you_wish          = @("随机获得一个二级增益。", "Grants one random level-II boon.")
    springtime_stroll    = @("移动时在脚下绽放野花或铺设薄片方块。", "Walking leaves flowers or carpet in your steps.")
    fleeting_bloom       = @("同时获得速度、力量、急迫与抗性。", "Grants Speed, Strength, Haste and Resistance at once.")
}
foreach ($id in $descriptions.Keys) {
    $zh["tooltip.kaleidoscope_flora.$id.desc"] = $descriptions[$id][0]
    $en["tooltip.kaleidoscope_flora.$id.desc"] = $descriptions[$id][1]
}

[System.IO.File]::WriteAllText("$assets\lang\en_us.json", ($en | ConvertTo-Json -Depth 3) + "`n", $utf8)
[System.IO.File]::WriteAllText("$assets\lang\zh_cn.json", ($zh | ConvertTo-Json -Depth 3) + "`n", $utf8)

# ----------------------------------------------------------------------
# 7. Delete the placeholder drinks and the abandoned teapot approach
#    (-EA SilentlyContinue: these one-time leftovers are usually already gone)
# ----------------------------------------------------------------------
Remove-Item "$assets\blockstates\dandelion_tea.json", "$assets\blockstates\poppy_tea.json", "$assets\blockstates\cornflower_tea.json" -Force -ErrorAction SilentlyContinue
Remove-Item "$assets\models\block\teacup\dandelion_tea", "$assets\models\block\teacup\poppy_tea", "$assets\models\block\teacup\cornflower_tea" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "$assets\models\item\dandelion_tea.json", "$assets\models\item\poppy_tea.json", "$assets\models\item\cornflower_tea.json" -Force -ErrorAction SilentlyContinue
Remove-Item "$assets\textures\block\teacup\dandelion_tea.png", "$assets\textures\block\teacup\poppy_tea.png", "$assets\textures\block\teacup\cornflower_tea.png" -Force -ErrorAction SilentlyContinue
Remove-Item "$assets\textures\item\dandelion_tea.png", "$assets\textures\item\poppy_tea.png", "$assets\textures\item\cornflower_tea.png" -Force -ErrorAction SilentlyContinue
Remove-Item "$data\recipe\teapot" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $root "data\kaleidoscope_cookery") -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "$data\tags\item\uncovered_small_flowers.json" -Force -ErrorAction SilentlyContinue

Write-Host "Generated $($( $drinks.Keys).Count) drinks." -ForegroundColor Green
Write-Host "Resource files now: $((Get-ChildItem $root -Recurse -File).Count)" -ForegroundColor Green
