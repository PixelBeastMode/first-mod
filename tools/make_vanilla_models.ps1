# Blockstate, models, item definitions and loot table for the vanilla orchid.
# Four growth stages, so four of most things.

$res = Join-Path $PSScriptRoot '..\src\main\resources'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Json($relPath, $obj) {
    $path = Join-Path $res $relPath
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, ($obj | ConvertTo-Json -Depth 20), $utf8NoBom)
    Write-Output "  $relPath"
}

# One variant per age value.
$variants = [ordered]@{}
for ($age = 0; $age -le 3; $age++) {
    $variants["age=$age"] = [ordered]@{ model = "madagascar:block/vanilla_vine_stage$age" }
}
Write-Json 'assets/madagascar/blockstates/vanilla_vine.json' ([ordered]@{ variants = $variants })

# minecraft:block/crop is the crossed-planes model wheat and carrots use.
for ($age = 0; $age -le 3; $age++) {
    Write-Json "assets/madagascar/models/block/vanilla_vine_stage$age.json" ([ordered]@{
        parent   = 'minecraft:block/crop'
        textures = [ordered]@{ crop = "madagascar:block/vanilla_vine_stage$age" }
    })
}

# The two items are flat sprites.
foreach ($item in @('vanilla_bean', 'vanilla_pod')) {
    Write-Json "assets/madagascar/models/item/$item.json" ([ordered]@{
        parent   = 'minecraft:item/generated'
        textures = [ordered]@{ layer0 = "madagascar:item/$item" }
    })
    Write-Json "assets/madagascar/items/$item.json" ([ordered]@{
        model = [ordered]@{ type = 'minecraft:model'; model = "madagascar:item/$item" }
    })
}

# Loot: a ripe vine gives pods plus a bean back, an unripe one just the bean.
# Modelled on vanilla's beetroots table, which also uses age 0..3.
$ripe = @(
    [ordered]@{
        block      = 'madagascar:vanilla_vine'
        condition  = 'minecraft:block_state_property'
        properties = [ordered]@{ age = '3' }
    }
)

Write-Json 'data/madagascar/loot_table/blocks/vanilla_vine.json' ([ordered]@{
    type      = 'minecraft:block'
    functions = @( [ordered]@{ function = 'minecraft:explosion_decay' } )
    pools     = @(
        # Always returns one bean, so replanting is possible.
        [ordered]@{
            rolls   = 1.0
            entries = @( [ordered]@{ type = 'minecraft:item'; name = 'madagascar:vanilla_bean' } )
        },
        # Pods only from a fully ripened, hand-pollinated vine.
        [ordered]@{
            rolls      = 1.0
            conditions = $ripe
            entries    = @(
                [ordered]@{
                    type      = 'minecraft:item'
                    name      = 'madagascar:vanilla_pod'
                    functions = @(
                        [ordered]@{
                            function = 'minecraft:set_count'
                            count    = [ordered]@{ type = 'minecraft:uniform'; min = 1.0; max = 3.0 }
                        },
                        [ordered]@{
                            function    = 'minecraft:apply_bonus'
                            enchantment = 'minecraft:fortune'
                            formula     = 'minecraft:uniform_bonus_count'
                            parameters  = [ordered]@{ bonusMultiplier = 1 }
                        }
                    )
                }
            )
        }
    )
    random_sequence = 'madagascar:blocks/vanilla_vine'
})
