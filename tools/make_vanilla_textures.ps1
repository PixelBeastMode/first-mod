Add-Type -AssemblyName System.Drawing

# Textures for the vanilla orchid: four growth stages plus the bean and pod
# items. '.' means transparent, which crops need since they render as two
# crossed planes rather than a solid cube.

$assets = Join-Path $PSScriptRoot '..\src\main\resources\assets\madagascar\textures'

$palette = @{
    'g' = '4A7A32'   # vine green
    '1' = '6A9E44'   # lighter leaf
    'f' = 'F0E6C8'   # flower, cream white
    'p' = '3E2A18'   # cured pod, near black brown
    'q' = '6A4A2A'   # pod highlight
}

$textures = @{
    'block/vanilla_vine_stage0' = @(
        '................','................','................','................',
        '................','................','................','................',
        '................','................','......g.g.......','.....gg.gg......',
        '......ggg.......','.......g........','.......g........','.......g........'
    )
    'block/vanilla_vine_stage1' = @(
        '................','................','................','................',
        '.....g....g.....','....gg....gg....','.....gg..gg.....','......g..g......',
        '.......gg.......','....gg.gg.gg....','...gg...g...gg..','.......gg.......',
        '.......g........','.......g........','.......g........','.......g........'
    )
    # Flowering. This is where growth stops until somebody pollinates it.
    'block/vanilla_vine_stage2' = @(
        '................','................','................','.....f....f.....',
        '....ff....ff....','.....fg..gf.....','....ffg..gff....','.....f.gg.f.....',
        '.......gg.......','....gg.gg.gg....','...gg...g...gg..','.......gg.......',
        '.......g........','.......g........','.......g........','.......g........'
    )
    # Ripe: long dark pods hanging off the vine.
    'block/vanilla_vine_stage3' = @(
        '................','................','................','................',
        '.....p....p.....','.....p....p.....','.....pg..gp.....','....qpg..gpq....',
        '.....p.gg.p.....','.....q.gg.q.....','....gg.gg.gg....','...gg...g...gg..',
        '.......gg.......','.......g........','.......g........','.......g........'
    )
    'item/vanilla_bean' = @(
        '................','................','................','................',
        '................','......pp........','.....pqqp.......','....pqqqp.......',
        '....pqqqp.......','....pqqp........','.....pp.........','................',
        '................','................','................','................'
    )
    'item/vanilla_pod' = @(
        '................','.........pp.....','........pqp.....','........pqp.....',
        '.......pqqp.....','.......pqp......','......pqqp......','......pqp.......',
        '.....pqqp.......','.....pqp........','....pqqp........','....pqp.........',
        '....pp..........','................','................','................'
    )
}

function ConvertTo-Color($hex) {
    return [System.Drawing.Color]::FromArgb(255,
        [Convert]::ToInt32($hex.Substring(0,2),16),
        [Convert]::ToInt32($hex.Substring(2,2),16),
        [Convert]::ToInt32($hex.Substring(4,2),16))
}

$colors = @{}
foreach ($k in $palette.Keys) { $colors[$k] = ConvertTo-Color $palette[$k] }
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)
$missing = [System.Drawing.Color]::FromArgb(255, 255, 0, 255)
$problems = 0

foreach ($name in ($textures.Keys | Sort-Object)) {
    $rows = $textures[$name]
    if ($rows.Count -ne 16) { Write-Output "  $name has $($rows.Count) rows"; $problems++ }

    $bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 16; $y++) {
        $row = $rows[$y]
        if ($row.Length -ne 16) { Write-Output "  $name row $y is $($row.Length) chars"; $problems++ }
        for ($x = 0; $x -lt 16; $x++) {
            $ch = if ($x -lt $row.Length) { $row.Substring($x,1) } else { '.' }
            $col = if ($ch -eq '.') { $clear } elseif ($colors.ContainsKey($ch)) { $colors[$ch] } else { $missing; $problems++ }
            $bmp.SetPixel($x, $y, $col)
        }
    }

    $path = Join-Path $assets "$name.png"
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "wrote $name.png"
}

if ($problems -gt 0) { Write-Output "$problems problems" } else { Write-Output 'all clean' }
