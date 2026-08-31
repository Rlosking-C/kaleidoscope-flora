# Generates the 23 placeholder MobEffect icons (18x18) for Kaleidoscope Flora.
# Style: dark rounded-square backdrop tinted by the effect colour, plus a
# simple coloured glyph so every effect is distinguishable in the HUD even
# before real art replaces these. Re-run any time.
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$out = "D:\GAMES\MCMod\Kaleidoscope Flora\src\main\resources\assets\kaleidoscope_flora\textures\mob_effect"
New-Item -ItemType Directory -Force -Path $out | Out-Null

# id -> colour + glyph shape
$effects = [ordered]@{
    "purge"        = @{ color = "F5F0C8"; shape = "diamond" }
    "fadeaway"     = @{ color = "D8D8C8"; shape = "ring" }
    "drowsy"       = @{ color = "9C8FB8"; shape = "moon" }
    "tastebloom"   = @{ color = "F7E3A1"; shape = "flower5" }
    "firebrand"    = @{ color = "E86A17"; shape = "flame" }
    "vampiric"     = @{ color = "B02E26"; shape = "drop" }
    "harvest"      = @{ color = "E8A13A"; shape = "wheat" }
    "absolve"      = @{ color = "F2F2F2"; shape = "shield" }
    "petalwalk"    = @{ color = "EBA2C8"; shape = "wave" }
    "divination"   = @{ color = "EFE9C0"; shape = "flower4" }
    "featherfall"  = @{ color = "9ED9F0"; shape = "feather" }
    "kiss"         = @{ color = "DCEDC2"; shape = "heart" }
    "wither_aura"  = @{ color = "6A6A6A"; shape = "skullring" }
    "sniffer_soul" = @{ color = "C8814F"; shape = "snout" }
    "sunward"      = @{ color = "FFD835"; shape = "sun" }
    "thorns"       = @{ color = "C62828"; shape = "spike" }
    "digestion"    = @{ color = "7BAF6A"; shape = "urn" }
    "petal_veil"   = @{ color = "F5B5D9"; shape = "petal" }
    "echo"         = @{ color = "A59BB8"; shape = "rings" }
    "sprout"       = @{ color = "7BBF4A"; shape = "sprout" }
    "gaze"         = @{ color = "9FB6D9"; shape = "eye" }
    "wish"         = @{ color = "FFD700"; shape = "star" }
    "flower_path"  = @{ color = "9CDF6A"; shape = "flowers3" }
}

function Color-From([string]$hex, [double]$mul = 1.0) {
    $r = [Math]::Min(255, [int]([Convert]::ToInt32($hex.Substring(0, 2), 16) * $mul))
    $g = [Math]::Min(255, [int]([Convert]::ToInt32($hex.Substring(2, 2), 16) * $mul))
    $b = [Math]::Min(255, [int]([Convert]::ToInt32($hex.Substring(4, 2), 16) * $mul))
    [System.Drawing.Color]::FromArgb(255, $r, $g, $b)
}

foreach ($id in $effects.Keys) {
    $e = $effects[$id]
    $main = Color-From $e.color
    $dark = Color-From $e.color 0.32
    $bright = Color-From $e.color 1.25
    $bmp = New-Object System.Drawing.Bitmap(18, 18)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    # Backdrop: rounded square in a dark tint of the effect colour.
    $bg = New-Object System.Drawing.SolidBrush($dark)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $r = 4
    $path.AddArc(1, 1, $r * 2, $r * 2, 180, 90)
    $path.AddArc(15 - $r * 2, 1, $r * 2, $r * 2, 270, 90)
    $path.AddArc(15 - $r * 2, 15 - $r * 2, $r * 2, $r * 2, 0, 90)
    $path.AddArc(1, 15 - $r * 2, $r * 2, $r * 2, 90, 90)
    $path.CloseFigure()
    $g.FillPath($bg, $path)
    $fg = New-Object System.Drawing.SolidBrush($main)

    switch ($e.shape) {
        "diamond" {  # Purge: cleansed diamond
            $pts = @( (New-Object System.Drawing.Point(9, 3)), (New-Object System.Drawing.Point(14, 9)), (New-Object System.Drawing.Point(9, 15)), (New-Object System.Drawing.Point(4, 9)) )
            $g.FillPolygon($fg, $pts)
        }
        "ring" {  # Fadeaway: hollow outline
            $pen = New-Object System.Drawing.Pen($main, 2.0)
            $g.DrawEllipse($pen, 5, 5, 8, 8)
            $pen.Dispose()
        }
        "moon" {  # Drowsy: crescent
            $g.FillEllipse($fg, 4, 3, 10, 10)
            $bg2 = New-Object System.Drawing.SolidBrush($dark)
            $g.FillEllipse($bg2, 6, 4, 10, 10)
            $bg2.Dispose()
        }
        "flower5" {  # Tastebloom: five-petal daisy
            for ($i = 0; $i -lt 5; $i++) {
                $a = $i * 2 * [Math]::PI / 5 - [Math]::PI / 2
                $g.FillEllipse($fg, [float](8.5 + [Math]::Cos($a) * 3.5 - 1.8), [float](8.5 + [Math]::Sin($a) * 3.5 - 1.8), 3.6, 3.6)
            }
        }
        "flame" {  # Firebrand: flame
            $pts = @( (New-Object System.Drawing.Point(9, 3)), (New-Object System.Drawing.Point(13, 9)), (New-Object System.Drawing.Point(11, 14)), (New-Object System.Drawing.Point(7, 14)), (New-Object System.Drawing.Point(5, 9)) )
            $g.FillPolygon($fg, $pts)
            $fb = New-Object System.Drawing.SolidBrush($bright)
            $g.FillEllipse($fb, 7, 9, 4, 4)
            $fb.Dispose()
        }
        "drop" {  # Vampiric: blood drop
            $pts = @( (New-Object System.Drawing.Point(9, 3)), (New-Object System.Drawing.Point(13, 10)), (New-Object System.Drawing.Point(9, 15)), (New-Object System.Drawing.Point(5, 10)) )
            $g.FillPolygon($fg, $pts)
        }
        "wheat" {  # Harvest: grain ear
            $pen = New-Object System.Drawing.Pen($main, 1.6)
            $g.DrawLine($pen, 9, 15, 9, 4)
            for ($y = 5; $y -le 13; $y += 2) {
                $g.FillEllipse($fg, 6, $y - 1, 2.4, 2.4)
                $g.FillEllipse($fg, 9.6, $y - 1, 2.4, 2.4)
            }
            $pen.Dispose()
        }
        "shield" {  # Absolve: shield
            $pts = @( (New-Object System.Drawing.Point(9, 3)), (New-Object System.Drawing.Point(14, 5)), (New-Object System.Drawing.Point(13, 12)), (New-Object System.Drawing.Point(9, 15)), (New-Object System.Drawing.Point(5, 12)), (New-Object System.Drawing.Point(4, 5)) )
            $g.FillPolygon($fg, $pts)
        }
        "wave" {  # Petalwalk: water surface
            $pen = New-Object System.Drawing.Pen($main, 2.2)
            for ($i = 0; $i -lt 2; $i++) {
                $y = 7 + $i * 4
                $g.DrawLine($pen, 3, $y, 6, $y)
                $g.DrawLine($pen, 12, $y, 15, $y)
                $g.DrawLine($pen, 7.5, $y + 1.4, 10.5, $y + 1.4)
            }
            $pen.Dispose()
        }
        "flower4" {  # Divination: plucking daisy
            for ($i = 0; $i -lt 4; $i++) {
                $a = $i * [Math]::PI / 2
                $g.FillEllipse($fg, [float](8.5 + [Math]::Cos($a) * 3.8 - 2.0), [float](8.5 + [Math]::Sin($a) * 3.8 - 2.0), 4, 4)
            }
        }
        "feather" {  # Featherfall: feather
            $g.FillEllipse($fg, 4, 5, 8, 4.5)
            $pen = New-Object System.Drawing.Pen($main, 1.4)
            $g.DrawLine($pen, 5, 14, 13, 6)
            $pen.Dispose()
        }
        "heart" {  # Kiss: heart
            $g.FillEllipse($fg, 4, 5, 4.5, 4.5)
            $g.FillEllipse($fg, 9.5, 5, 4.5, 4.5)
            $pts = @( (New-Object System.Drawing.Point(4, 7.5)), (New-Object System.Drawing.Point(14, 7.5)), (New-Object System.Drawing.Point(9, 14.5)) )
            $g.FillPolygon($fg, $pts)
        }
        "skullring" {  # Wither aura: dark broken ring
            $pen = New-Object System.Drawing.Pen($main, 2.4)
            $g.DrawArc($pen, 4, 4, 10, 10, 30, 290)
            $pen.Dispose()
        }
        "snout" {  # Sniffer soul: probing snout
            $g.FillEllipse($fg, 5, 4, 8, 5)
            $g.FillEllipse($fg, 7.5, 8, 3, 6)
        }
        "sun" {  # Sunward: sun disc + rays
            $g.FillEllipse($fg, 6, 6, 6, 6)
            $pen = New-Object System.Drawing.Pen($main, 1.4)
            foreach ($a in @(0, 45, 90, 135, 180, 225, 270, 315)) {
                $rad = $a * [Math]::PI / 180
                $g.DrawLine($pen, [float](9 + [Math]::Cos($rad) * 4.5), [float](9 + [Math]::Sin($rad) * 4.5), [float](9 + [Math]::Cos($rad) * 7.5), [float](9 + [Math]::Sin($rad) * 7.5))
            }
            $pen.Dispose()
        }
        "spike" {  # Thorns: barbed spike
            $pts = @( (New-Object System.Drawing.Point(9, 2)), (New-Object System.Drawing.Point(12, 10)), (New-Object System.Drawing.Point(9, 16)), (New-Object System.Drawing.Point(6, 10)) )
            $g.FillPolygon($fg, $pts)
            $pen = New-Object System.Drawing.Pen($main, 1.4)
            $g.DrawLine($pen, 9, 6, 12, 4)
            $g.DrawLine($pen, 9, 12, 6, 14)
            $pen.Dispose()
        }
        "urn" {  # Digestion: pitcher urn
            $g.FillEllipse($fg, 5, 7, 8, 7)
            $g.FillRectangle($fg, 6, 3, 6, 4)
            $pen = New-Object System.Drawing.Pen($main, 1.4)
            $g.DrawLine($pen, 6, 4, 3, 6)
            $pen.Dispose()
        }
        "petal" {  # Petal veil: sakura petal with notch
            $pts = @( (New-Object System.Drawing.Point(9, 3)), (New-Object System.Drawing.Point(14, 8)), (New-Object System.Drawing.Point(9, 15)), (New-Object System.Drawing.Point(4, 8)) )
            $g.FillPolygon($fg, $pts)
            $bg2 = New-Object System.Drawing.SolidBrush($dark)
            $g.FillEllipse($bg2, 7.6, 2.0, 2.8, 2.8)
            $bg2.Dispose()
        }
        "rings" {  # Echo: concentric rings
            $pen = New-Object System.Drawing.Pen($main, 1.6)
            $g.DrawEllipse($pen, 6, 6, 6, 6)
            $pen2 = New-Object System.Drawing.Pen($main, 1.2)
            $g.DrawEllipse($pen2, 3.5, 3.5, 11, 11)
            $g.FillEllipse($fg, 8, 8, 2, 2)
            $pen.Dispose(); $pen2.Dispose()
        }
        "sprout" {  # Sprouting: seedling
            $pen = New-Object System.Drawing.Pen($main, 1.6)
            $g.DrawLine($pen, 9, 15, 9, 8)
            $pen.Dispose()
            $g.FillEllipse($fg, 3, 4, 6, 4)
            $g.FillEllipse($fg, 9, 5, 6, 4)
        }
        "eye" {  # Gaze: wide eye
            $g.FillEllipse($fg, 3, 6, 12, 6)
            $bg2 = New-Object System.Drawing.SolidBrush($dark)
            $g.FillEllipse($bg2, 7, 7, 4, 4)
            $bg2.Dispose()
            $wb = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
            $g.FillEllipse($wb, 8.4, 8.4, 1.6, 1.6)
            $wb.Dispose()
        }
        "star" {  # Wish: four-pointed star
            $pts = @( (New-Object System.Drawing.Point(9, 2)), (New-Object System.Drawing.Point(11, 7)), (New-Object System.Drawing.Point(16, 9)), (New-Object System.Drawing.Point(11, 11)), (New-Object System.Drawing.Point(9, 16)), (New-Object System.Drawing.Point(7, 11)), (New-Object System.Drawing.Point(2, 9)), (New-Object System.Drawing.Point(7, 7)) )
            $g.FillPolygon($fg, $pts)
        }
        "flowers3" {  # Flower path: three small blooms
            $g.FillEllipse($fg, 3, 3, 5, 5)
            $g.FillEllipse($fg, 10, 3, 5, 5)
            $g.FillEllipse($fg, 6.5, 9, 5, 5)
        }
    }

    $g.Dispose(); $bg.Dispose(); $fg.Dispose(); $path.Dispose()
    $bmp.Save("$out\$id.png", [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "icon: $id"
}
Write-Host "Done: $($effects.Count) effect icons -> $out"
