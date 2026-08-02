Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Minecraft version comes from gradle.properties so this keeps working across updates.
$mcVersion = (Select-String -Path (Join-Path $PSScriptRoot '..\gradle.properties') -Pattern '^minecraft_version=(.+)$').Matches[0].Groups[1].Value.Trim()
$jar = Join-Path $env:USERPROFILE ".gradle\caches\fabric-loom\$mcVersion\minecraft-client.jar"
$modAssets = Join-Path $PSScriptRoot '..\src\main\resources\assets\minecraft\textures\entity'

# Each job recolours a vanilla texture by "colorising": keep the original
# shading (luminance) but replace the hue with a target tint. That preserves
# every pixel's role in the UV layout, so the model still reads correctly.
$jobs = @(
    # Zebu: a gradient rather than a single tint. Colourising with one colour can
    # only ever produce shades of that colour, which came out flat and albino.
    # Mapping dark pixels to charcoal and light pixels to cream keeps the black,
    # white and grey range the real animal has. gamma < 1 pushes the midtones
    # towards the pale end, because a zebu is mostly light with darker points.
    @{ src='cow/cow_temperate.png';      dark=@(74,72,70); light=@(242,238,229); gamma=0.72 },
    @{ src='cow/cow_warm.png';           dark=@(74,72,70); light=@(242,238,229); gamma=0.72 },
    @{ src='cow/cow_cold.png';           dark=@(74,72,70); light=@(242,238,229); gamma=0.72 },
    @{ src='cow/cow_temperate_baby.png'; dark=@(74,72,70); light=@(242,238,229); gamma=0.72 },
    @{ src='cow/cow_warm_baby.png';      dark=@(74,72,70); light=@(242,238,229); gamma=0.72 },
    @{ src='cow/cow_cold_baby.png';      dark=@(74,72,70); light=@(242,238,229); gamma=0.72 },

    # Fossa: uniform rich reddish-brown. The ocelot's spots survive as subtle
    # tonal variation, which is close enough to the real animal's coat.
    @{ src='cat/ocelot.png';             tint=@(150, 96, 62); gain=1.05 },
    @{ src='cat/ocelot_baby.png';        tint=@(150, 96, 62); gain=1.05 },

    # Ring-tailed lemur: grey body, then the markings that actually identify it -
    # a banded tail and a white face with black eye patches. See Paint-Lemur below.
    @{ src='fox/fox.png';                tint=@(168,168,174); gain=1.05; lemur=$true },
    @{ src='fox/fox_baby.png';           tint=@(168,168,174); gain=1.05; lemur=$true },
    @{ src='fox/fox_sleep.png';          tint=@(168,168,174); gain=1.05; lemur=$true },
    @{ src='fox/fox_sleep_baby.png';     tint=@(168,168,174); gain=1.05; lemur=$true },

    # Golden mantella: deep yellow body, black limbs. See Paint-Mantella.
    @{ src='frog/frog_warm.png';      dark=@(120,88,8); light=@(238,198,36); gamma=0.85; mantella=$true },
    @{ src='frog/frog_temperate.png'; dark=@(120,88,8); light=@(238,198,36); gamma=0.85; mantella=$true },
    @{ src='frog/frog_cold.png';      dark=@(120,88,8); light=@(238,198,36); gamma=0.85; mantella=$true },

    # Villager professions. Based on vanilla professions that have no .mcmeta
    # beside them, so there is no companion metadata to copy across.
    @{ src='villager/profession/leatherworker.png'; tint=@(206,102,72); gain=1.05;
       outAsset='madagascar/textures/entity/villager/profession/mpahandro.png' },
    @{ src='villager/profession/cleric.png';        tint=@(150,116,196); gain=1.05;
       outAsset='madagascar/textures/entity/villager/profession/ombiasy.png' },
    @{ src='villager/profession/mason.png';         tint=@(146,174,94); gain=1.05;
       outAsset='madagascar/textures/entity/villager/profession/vanilla_grower.png' },

    # Albino zebu: a rare variant, so it lives in our own namespace rather than
    # overriding a vanilla texture. Near-white with only the faintest shading,
    # and pink eyes.
    @{ src='cow/cow_temperate.png';      dark=@(226,223,221); light=@(253,252,251); gamma=0.6;
       albino=$true; outAsset='madagascar/textures/entity/cow/albino_zebu.png' },
    @{ src='cow/cow_temperate_baby.png'; dark=@(226,223,221); light=@(253,252,251); gamma=0.6;
       albino=$true; outAsset='madagascar/textures/entity/cow/albino_zebu_baby.png' }
)

# A ring-tailed lemur is grey like a grey fox until it has the two markings that
# actually identify it. Both are painted straight onto UV rectangles worked out
# from the fox model's box definitions on its 48x32 texture:
#
#   tail  texOffs(30,0)  box 4x9x5  -> side faces at x 30..48, y 5..14
#   head  texOffs(1,5)   box 8x6x6  -> front face  at x  7..15, y 11..17
#
# Minecraft lays a box out as: a top row of down/up faces d tall, then a row of
# east/front/west/back faces h tall. The tail's long axis is its height, so
# horizontal stripes in the texture wrap the tail as rings.
function Paint-Lemur($bmp) {
    $black = [System.Drawing.Color]::FromArgb(255, 34, 32, 34)
    $white = [System.Drawing.Color]::FromArgb(255, 236, 234, 230)

    # Tail rings. Alternating single-pixel bands, dark at the tip, which real
    # ring-tailed lemurs always have.
    for ($y = 5; $y -lt 14; $y++) {
        $dark = ((($y - 5) % 2) -eq 0)
        for ($x = 30; $x -lt 48; $x++) {
            if ($x -ge $bmp.Width -or $y -ge $bmp.Height) { continue }
            if ($bmp.GetPixel($x, $y).A -eq 0) { continue }
            $bmp.SetPixel($x, $y, $(if ($dark) { $black } else { $white }))
        }
    }

    # White face.
    for ($y = 11; $y -lt 17; $y++) {
        for ($x = 7; $x -lt 15; $x++) {
            if ($x -ge $bmp.Width -or $y -ge $bmp.Height) { continue }
            if ($bmp.GetPixel($x, $y).A -eq 0) { continue }
            $bmp.SetPixel($x, $y, $white)
        }
    }
    # Black eye patches and muzzle on top of it.
    foreach ($r in @(@(8,12,2,2), @(12,12,2,2), @(10,15,2,2))) {
        for ($y = $r[1]; $y -lt $r[1] + $r[3]; $y++) {
            for ($x = $r[0]; $x -lt $r[0] + $r[2]; $x++) {
                if ($x -ge $bmp.Width -or $y -ge $bmp.Height) { continue }
                if ($bmp.GetPixel($x, $y).A -eq 0) { continue }
                $bmp.SetPixel($x, $y, $black)
            }
        }
    }
}

# Albino eyes. The cow's head is texOffs(0,0) on an 8x8x6 box, so its front face
# sits at x 6..14, y 6..14. Rather than hardcode where the eyes are, find the
# darkest pixels in that rectangle on the ORIGINAL texture - those are the eyes -
# and paint exactly those pink.
function Get-EyePixels($src) {
    $eyes = @()
    for ($y = 6; $y -lt 14; $y++) {
        for ($x = 6; $x -lt 14; $x++) {
            if ($x -ge $src.Width -or $y -ge $src.Height) { continue }
            $p = $src.GetPixel($x, $y)
            if ($p.A -eq 0) { continue }
            $lum = (0.299 * $p.R + 0.587 * $p.G + 0.114 * $p.B)
            if ($lum -lt 80) { $eyes += ,@($x, $y) }
        }
    }
    return $eyes
}

# Golden mantella. Madagascar's toxic frogs are unrelated to South American dart
# frogs but evolved the same warning colours. Body is deep yellow, limbs black.
# On the 48x48 frog texture the limbs sit in the lower third: legs in a band at
# y 24..32, then arms, hands and feet below y 32.
$MANTELLA_LIMBS = @(
    @(0, 24, 28, 8),   # both legs
    @(0, 32, 48, 16)   # arms, hands and webbed feet
)

function Paint-Mantella($bmp, $src) {
    foreach ($r in $MANTELLA_LIMBS) {
        for ($y = $r[1]; $y -lt $r[1] + $r[3]; $y++) {
            for ($x = $r[0]; $x -lt $r[0] + $r[2]; $x++) {
                if ($x -ge $bmp.Width -or $y -ge $bmp.Height) { continue }
                $o = $src.GetPixel($x, $y)
                if ($o.A -eq 0) { continue }
                # Re-shade from the ORIGINAL brightness so the limbs keep their
                # form instead of becoming a flat silhouette.
                $lum = (0.299 * $o.R + 0.587 * $o.G + 0.114 * $o.B) / 255.0
                $t = [Math]::Pow($lum, 1.1)
                $r2 = [int](10 + 46 * $t); $g2 = [int](10 + 44 * $t); $b2 = [int](12 + 48 * $t)
                $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($o.A, $r2, $g2, $b2))
            }
        }
    }
}

$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)

foreach ($job in $jobs) {
    $entryName = "assets/minecraft/textures/entity/$($job.src)"
    $entry = $zip.Entries | Where-Object { $_.FullName -eq $entryName }
    if (-not $entry) { Write-Output "MISSING $entryName"; continue }

    $ms = New-Object System.IO.MemoryStream
    $entry.Open().CopyTo($ms)
    $ms.Position = 0
    $src = [System.Drawing.Image]::FromStream($ms)
    $bmp = New-Object System.Drawing.Bitmap $src.Width, $src.Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    for ($y = 0; $y -lt $src.Height; $y++) {
        for ($x = 0; $x -lt $src.Width; $x++) {
            $p = $src.GetPixel($x, $y)
            if ($p.A -eq 0) { $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0,0,0,0)); continue }

            # Perceived brightness of the original pixel, 0..1.
            $lum = (0.299 * $p.R + 0.587 * $p.G + 0.114 * $p.B) / 255.0

            if ($job.ContainsKey('dark')) {
                # Gradient mode: interpolate between two colours across the
                # brightness range, so the result keeps a real dark-to-light spread.
                $t = [Math]::Pow($lum, $job.gamma)
                $r = [int][Math]::Round($job.dark[0] + ($job.light[0] - $job.dark[0]) * $t)
                $g = [int][Math]::Round($job.dark[1] + ($job.light[1] - $job.dark[1]) * $t)
                $b = [int][Math]::Round($job.dark[2] + ($job.light[2] - $job.dark[2]) * $t)
            } else {
                # Tint mode: keep the original shading, replace the hue.
                $lum = [Math]::Min(1.0, $lum * $job.gain)
                $r = [Math]::Min(255, [int][Math]::Round($job.tint[0] * $lum))
                $g = [Math]::Min(255, [int][Math]::Round($job.tint[1] * $lum))
                $b = [Math]::Min(255, [int][Math]::Round($job.tint[2] * $lum))
            }
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($p.A, $r, $g, $b))
        }
    }

    if ($job.ContainsKey('lemur')) { Paint-Lemur $bmp }

    if ($job.ContainsKey('mantella')) { Paint-Mantella $bmp $src }

    if ($job.ContainsKey('albino')) {
        $pink = [System.Drawing.Color]::FromArgb(255, 244, 138, 156)
        foreach ($e in (Get-EyePixels $src)) { $bmp.SetPixel($e[0], $e[1], $pink) }
    }

    if ($job.ContainsKey('outAsset')) {
        $outPath = Join-Path (Join-Path $PSScriptRoot '..\src\main\resources\assets') $job.outAsset
    } else {
        $outPath = Join-Path $modAssets $job.src
    }
    $outDir = Split-Path $outPath -Parent
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose(); $src.Dispose(); $ms.Dispose()

    Write-Output ("{0,-30} -> {1}" -f $job.src, (Split-Path $outPath -Leaf))
}

$zip.Dispose()
