Add-Type -AssemblyName System.Drawing

# Madagascar coastline, traced clockwise from the northern tip (Cap d'Ambre).
# Real longitude / latitude, simplified to ~35 points.
$coast = @(
    @(49.25, -11.95),  # Cap d'Ambre - northern tip
    @(49.90, -12.90),
    @(50.00, -13.70),
    @(49.85, -14.60),
    @(50.20, -15.00),
    @(50.50, -15.35),  # Masoala peninsula - easternmost point
    @(49.95, -15.95),  # Bay of Antongil indent
    @(49.80, -16.60),
    @(49.55, -17.30),
    @(49.40, -18.10),  # Toamasina
    @(48.95, -19.50),
    @(48.60, -20.50),
    @(48.20, -21.80),
    @(47.80, -22.80),
    @(47.40, -23.80),
    @(47.10, -25.00),  # Tolagnaro / Fort Dauphin
    @(46.20, -25.30),
    @(45.20, -25.60),  # Cap Sainte-Marie - southern tip
    @(44.60, -25.30),
    @(44.10, -24.70),
    @(43.60, -23.40),  # Toliara
    @(43.50, -22.30),
    @(43.70, -21.40),
    @(44.00, -20.30),  # Morondava
    @(43.90, -19.40),
    @(44.40, -18.40),
    @(44.00, -17.50),  # Maintirano - western bulge
    @(44.50, -16.70),
    @(45.60, -16.20),
    @(46.30, -15.70),  # Mahajanga
    @(46.40, -15.20),
    @(47.00, -14.60),
    @(47.50, -14.30),
    @(48.00, -13.60),
    @(48.30, -13.30),
    @(48.50, -12.60)
)

# Bounds padded slightly beyond the coastline so the island is ringed by ocean.
$lonMin = 43.00; $lonMax = 50.70
$latMax = -11.70; $latMin = -25.90   # latMax is the NORTH edge (less negative)

# Resolution must match the bounding box's real-world aspect or the island
# comes out stretched. At ~19 deg south, 1 deg lon is ~105 km, 1 deg lat ~111 km.
$kmWide = ($lonMax - $lonMin) * 105.2
$kmTall = ($latMax - $latMin) * 111.3
$W = 140
$H = [int][Math]::Round($W * $kmTall / $kmWide)
Write-Output ("bounding box: {0:N0} km x {1:N0} km  -> mask {2} x {3}" -f $kmWide, $kmTall, $W, $H)

$points = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'
foreach ($c in $coast) {
    $x = ($c[0] - $lonMin) / ($lonMax - $lonMin) * $W
    $y = ($c[1] - $latMax) / ($latMin - $latMax) * $H
    $points.Add((New-Object System.Drawing.PointF([single]$x, [single]$y)))
}

$bmp = New-Object System.Drawing.Bitmap $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::Black)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$g.FillPolygon($brush, $points.ToArray())
$g.Dispose()

$outDir = Join-Path $PSScriptRoot '..\src\main\resources\madagascar'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }
$outPath = Join-Path $outDir 'island_mask.png'
$bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)

# Report land coverage so we can sanity-check the shape rasterised at all.
$land = 0
for ($y = 0; $y -lt $H; $y++) {
    for ($x = 0; $x -lt $W; $x++) {
        if ($bmp.GetPixel($x, $y).R -gt 127) { $land++ }
    }
}
$bmp.Dispose()

Write-Output "Wrote $outPath  ($W x $H)"
Write-Output ("land pixels: {0} / {1} ({2:N1}%)" -f $land, ($W * $H), (100.0 * $land / ($W * $H)))
