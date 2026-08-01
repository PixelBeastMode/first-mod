Add-Type -AssemblyName System.IO.Compression.FileSystem

# Minecraft version comes from gradle.properties so this keeps working across updates.
$mcVersion = (Select-String -Path (Join-Path $PSScriptRoot '..\gradle.properties') -Pattern '^minecraft_version=(.+)$').Matches[0].Groups[1].Value.Trim()
$jar = Join-Path $env:USERPROFILE ".gradle\caches\fabric-loom\$mcVersion\minecraft-client.jar"
$outDir = Join-Path $PSScriptRoot '..\src\main\resources\data\madagascar\worldgen\biome'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }

function Get-VanillaBiome($name) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
    $e = $zip.Entries | Where-Object { $_.FullName -eq "data/minecraft/worldgen/biome/$name.json" }
    $sr = New-Object System.IO.StreamReader($e.Open())
    $t = $sr.ReadToEnd(); $sr.Close(); $zip.Dispose()
    return ($t | ConvertFrom-Json)
}

# base   = vanilla biome to inherit features/spawners/carvers from
# music  = vanilla background music track to keep
$biomes = @(
    @{ name='eastern_rainforest'; base='jungle';     temp=0.95; downfall=0.95; rain=$true;
       grass='#1f7a2e'; foliage='#1a6b28'; water='#3a7a8c'; sky='#77a8ff'; fog='#b6d0f5';
       addVeg=@('madagascar:ravinala');
       creatures=@(
         @{ type='minecraft:fox';     w=30; min=2; max=4 },
         @{ type='minecraft:parrot';  w=20; min=1; max=2 },
         @{ type='minecraft:chicken'; w=8;  min=2; max=4 },
         @{ type='minecraft:cow';     w=8;  min=2; max=4 },
         @{ type='minecraft:ocelot';  w=5;  min=1; max=2 }
       ) },

    # Deforested within living memory, so the highlands get grass and no trees.
    @{ name='central_highlands';  base='savanna';    temp=0.70; downfall=0.50; rain=$true;
       grass='#8fa04a'; foliage='#7d8f3f'; water='#8a6642'; sky='#86b4ff'; fog='#d7cdb8';
       dropVeg='trees_';
       creatures=@(
         @{ type='minecraft:cow';     w=30; min=3; max=5 },
         @{ type='minecraft:chicken'; w=10; min=2; max=4 },
         @{ type='minecraft:fox';     w=5;  min=1; max=2 }
       ) },

    @{ name='menabe_dry_forest';  base='savanna';    temp=1.10; downfall=0.30; rain=$true;
       grass='#c2b352'; foliage='#ad9f43'; water='#4a7fa5'; sky='#8fc0ff'; fog='#e0d4ad';
       dropVeg='trees_'; addVeg=@('madagascar:baobab');
       creatures=@(
         @{ type='minecraft:cow';    w=20; min=2; max=4 },
         @{ type='minecraft:fox';    w=15; min=2; max=3 },
         @{ type='minecraft:ocelot'; w=6;  min=1; max=2 }
       ) },

    @{ name='spiny_forest';       base='badlands';   temp=1.30; downfall=0.05; rain=$false;
       grass='#a8b07a'; foliage='#8f9a5c'; water='#4a86a8'; sky='#9fc8ff'; fog='#e8dcc0';
       dropVeg='trees_'; addVeg=@('madagascar:octopus_tree');
       creatures=@(
         @{ type='minecraft:fox';    w=8; min=1; max=2 },
         @{ type='minecraft:ocelot'; w=4; min=1; max=1 }
       ) },

    @{ name='tsingy';             base='savanna';    temp=1.00; downfall=0.35; rain=$true;
       grass='#93a67e'; foliage='#7f9268'; water='#4d94a8'; sky='#8ab8ff'; fog='#d8dcd0';
       dropVeg='trees_';
       creatures=@(
         @{ type='minecraft:fox';    w=25; min=2; max=3 },
         @{ type='minecraft:ocelot'; w=5;  min=1; max=1 }
       ) },

    @{ name='isalo';              base='badlands';   temp=1.10; downfall=0.15; rain=$false;
       grass='#b09a55'; foliage='#9e8a4a'; water='#4a7fa5'; sky='#95bcff'; fog='#e8c9a0';
       dropVeg='trees_';
       creatures=@(
         @{ type='minecraft:fox';    w=30; min=2; max=4 },
         @{ type='minecraft:ocelot'; w=4;  min=1; max=1 }
       ) },

    @{ name='coral_coast';        base='beach';      temp=1.00; downfall=0.40; rain=$true;
       grass='#90b464'; foliage='#7fa356'; water='#1fb8c4'; sky='#7fc4ff'; fog='#cfeaf2' },

    @{ name='madagascar_reef';    base='warm_ocean'; temp=0.90; downfall=0.50; rain=$true;
       grass='#8fbf6a'; foliage='#7fae5c'; water='#1fc4cc'; sky='#7fc4ff'; fog='#bfe8ef' }
)

foreach ($b in $biomes) {
    $o = Get-VanillaBiome $b.base

    $o.temperature = $b.temp
    $o.downfall = $b.downfall
    $o.has_precipitation = $b.rain

    # Rebuild effects with our colours, keeping any modifier the base had.
    $effects = [ordered]@{
        grass_color   = $b.grass
        foliage_color = $b.foliage
        water_color   = $b.water
    }
    if ($o.effects.PSObject.Properties.Name -contains 'grass_color_modifier') {
        $effects['grass_color_modifier'] = $o.effects.grass_color_modifier
    }
    if ($o.effects.PSObject.Properties.Name -contains 'dry_foliage_color') {
        $effects['dry_foliage_color'] = $o.effects.dry_foliage_color
    }
    $o.effects = [pscustomobject]$effects

    # Attributes: keep whatever the base had, then set our sky and fog.
    $attrs = [ordered]@{}
    if ($o.attributes) {
        $o.attributes.PSObject.Properties | ForEach-Object { $attrs[$_.Name] = $_.Value }
    }
    $attrs['minecraft:visual/sky_color'] = $b.sky
    $attrs['minecraft:visual/fog_color'] = $b.fog
    $o | Add-Member -NotePropertyName attributes -NotePropertyValue ([pscustomobject]$attrs) -Force

    # Creature spawns. cow=zebu, ocelot=fossa, fox=ring-tailed lemur.
    if ($b.creatures) {
        $o.spawners.creature = @(
            $b.creatures | ForEach-Object {
                [pscustomobject]@{
                    type     = $_.type
                    maxCount = $_.max
                    minCount = $_.min
                    weight   = $_.w
                }
            }
        )
    }

    # Vegetation: drop the vanilla trees that do not belong here, then add ours.
    $veg = @($o.features[9])
    if ($b.dropVeg) { $veg = $veg | Where-Object { $_ -notmatch $b.dropVeg } }
    if ($b.addVeg) { $veg = @($veg) + @($b.addVeg) }
    $o.features[9] = @($veg)

    $path = Join-Path $outDir "$($b.name).json"
    # Plain UTF-8, no BOM: Set-Content -Encoding utf8 adds one in PS 5.1 and
    # Minecraft's JSON parser will not accept it.
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($path, ($o | ConvertTo-Json -Depth 40), $utf8NoBom)
    Write-Output ("{0,-22} <- {1,-12} temp {2}  veg {3}" -f $b.name, $b.base, $b.temp, @($o.features[9]).Count)
}
