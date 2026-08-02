# Villager trades are fully data-driven in 26.2. Each profession level needs
# three things:
#   villager_trade/<prof>/<level>/<name>.json   one file per trade
#   tags/villager_trade/<prof>/level_N.json     a tag listing those trades
#   trade_set/<prof>/level_N.json               the set, pointing at the tag
#
# Writing ~90 files by hand would be miserable, so they come from the table
# below. Trades use vanilla items only.

$data = Join-Path $PSScriptRoot '..\src\main\resources\data\madagascar'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Json($relPath, $obj) {
    $path = Join-Path $data $relPath
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    [System.IO.File]::WriteAllText($path, (($obj | ConvertTo-Json -Depth 10)), $utf8NoBom)
}

# buy  = villager pays emeralds for your goods
# sell = villager gives goods for your emeralds
function Buy($item, $count)          { @{ kind='buy';  item=$item; count=$count } }
function Sell($item, $count, $price) { @{ kind='sell'; item=$item; count=$count; price=$price } }

$professions = [ordered]@{
    # Mpahandro, the cook. Zebu beef and greens are romazava, the national dish;
    # pork with cassava leaves is ravitoto.
    'mpahandro' = @(
        @( (Buy 'wheat' 20),        (Sell 'bread' 5 1) ),
        @( (Buy 'beef' 12),         (Sell 'cooked_beef' 4 1) ),
        @( (Buy 'beetroot' 16),     (Sell 'cooked_chicken' 3 1) ),
        @( (Buy 'carrot' 10),       (Sell 'rabbit_stew' 1 2) ),
        @( (Buy 'potato' 24),       (Sell 'cooked_porkchop' 6 1) )
    )
    # Ombiasy, the traditional healer-diviner. Buys medicinal plants, sells
    # remedies. The milk bucket is the cure for a mantella's poison.
    'ombiasy' = @(
        @( (Buy 'brown_mushroom' 24), (Sell 'honey_bottle' 3 1) ),
        @( (Buy 'red_mushroom' 20),   (Sell 'milk_bucket' 1 2) ),
        @( (Buy 'sweet_berries' 16),  (Sell 'glistering_melon_slice' 1 4) ),
        @( (Buy 'glow_berries' 12),   (Sell 'golden_carrot' 1 3) ),
        @( (Buy 'kelp' 24),           (Sell 'golden_apple' 1 12) )
    )
    # Vanilla grower. Cocoa beans stand in for cured vanilla pods until the
    # vanilla vine block exists.
    'vanilla_grower' = @(
        @( (Buy 'cocoa_beans' 16),  (Sell 'bone_meal' 6 1) ),
        @( (Buy 'sugar_cane' 24),   (Sell 'cocoa_beans' 3 3) ),
        @( (Buy 'melon_slice' 20),  (Sell 'honey_bottle' 1 2) ),
        @( (Buy 'apple' 15),        (Sell 'cake' 1 4) ),
        @( (Buy 'sugar' 30),        (Sell 'pumpkin_pie' 2 6) )
    )
}

$written = 0
foreach ($prof in $professions.Keys) {
    $levels = $professions[$prof]
    for ($i = 0; $i -lt $levels.Count; $i++) {
        $level = $i + 1
        $tradeIds = @()

        foreach ($t in $levels[$i]) {
            if ($t.kind -eq 'buy') {
                $name = "$($t.item)_emerald"
                $json = [ordered]@{
                    gives               = [ordered]@{ id = 'minecraft:emerald' }
                    max_uses            = 16.0
                    reputation_discount = 0.05
                    wants               = [ordered]@{ count = [double]$t.count; id = "minecraft:$($t.item)" }
                    xp                  = 2.0
                }
            } else {
                $name = "emerald_$($t.item)"
                $gives = [ordered]@{ id = "minecraft:$($t.item)" }
                if ($t.count -gt 1) { $gives['count'] = [int]$t.count }
                $wants = [ordered]@{ id = 'minecraft:emerald' }
                if ($t.price -gt 1) { $wants['count'] = [double]$t.price }
                $json = [ordered]@{
                    gives               = $gives
                    max_uses            = 12.0
                    reputation_discount = 0.05
                    wants               = $wants
                }
            }

            Write-Json "villager_trade/$prof/$level/$name.json" $json
            $tradeIds += "madagascar:$prof/$level/$name"
            $written++
        }

        Write-Json "tags/villager_trade/$prof/level_$level.json" ([ordered]@{ values = $tradeIds })
        Write-Json "trade_set/$prof/level_$level.json" ([ordered]@{
            amount          = 2.0
            random_sequence = "madagascar:trade_set/$prof/level_$level"
            trades          = "#madagascar:$prof/level_$level"
        })
        $written += 2
    }
    Write-Output ("{0,-16} {1} levels" -f $prof, $levels.Count)
}
Write-Output "$written files written"
