# Emits the blockstate, block model and item definition for each simple cube
# block with a distinct top texture. Vanilla workstations (loom, smithing table,
# cartography table) are all plain cubes too, so this matches the convention.

$assets = Join-Path $PSScriptRoot '..\src\main\resources\assets\madagascar'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Json($relPath, $obj) {
    $path = Join-Path $assets $relPath
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, (($obj | ConvertTo-Json -Depth 10)), $utf8NoBom)
    Write-Output "  $relPath"
}

$blocks = @('cooking_pot', 'herb_table', 'drying_rack')

foreach ($b in $blocks) {
    Write-Output "$b :"

    Write-Json "blockstates/$b.json" ([ordered]@{
        variants = [ordered]@{ '' = [ordered]@{ model = "madagascar:block/$b" } }
    })

    Write-Json "models/block/$b.json" ([ordered]@{
        parent   = 'minecraft:block/cube_bottom_top'
        textures = [ordered]@{
            top    = "madagascar:block/${b}_top"
            bottom = "madagascar:block/${b}_side"
            side   = "madagascar:block/${b}_side"
        }
    })

    # The item definition points straight at the block model, so no separate
    # models/item file is needed.
    Write-Json "items/$b.json" ([ordered]@{
        model = [ordered]@{
            type  = 'minecraft:model'
            model = "madagascar:block/$b"
        }
    })
}
