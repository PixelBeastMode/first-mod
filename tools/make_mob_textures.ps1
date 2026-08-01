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

    # Ring-tailed lemur: grey body, pale face and underside.
    @{ src='fox/fox.png';                tint=@(172,172,178); gain=1.05 },
    @{ src='fox/fox_baby.png';           tint=@(172,172,178); gain=1.05 },
    @{ src='fox/fox_sleep.png';          tint=@(172,172,178); gain=1.05 },
    @{ src='fox/fox_sleep_baby.png';     tint=@(172,172,178); gain=1.05 }
)

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

    $outPath = Join-Path $modAssets $job.src
    $outDir = Split-Path $outPath -Parent
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose(); $src.Dispose(); $ms.Dispose()

    Write-Output ("{0,-30} -> {1}" -f $job.src, $outPath.Substring($modAssets.Length + 1))
}

$zip.Dispose()
