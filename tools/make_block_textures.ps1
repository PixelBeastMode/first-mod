Add-Type -AssemblyName System.Drawing

# Workstation block textures, drawn as 16x16 pixel art from character maps.
# One character per pixel, looked up in each texture's own palette.
#
# Note: PowerShell hash keys are case-insensitive, so 'g' and 'G' are the same
# key. Palette characters must therefore differ by more than case.

$outDir = Join-Path $PSScriptRoot '..\src\main\resources\assets\madagascar\textures\block'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }

$textures = @{
    # Mpahandro's pot: looking down into romazava simmering.
    'cooking_pot_top' = @{
        palette = @{
            'o' = '4A4A52'; 'r' = '6E6E78'; 'k' = '23232A'
            's' = '7A4620'; 'b' = '96602C'; 'g' = '4C7030'; 'h' = 'B07A3C'
        }
        rows = @(
            'oooooooooooooooo',
            'orrrrrrrrrrrrrro',
            'orkkkkkkkkkkkkko',
            'orksssssbsssssko',
            'orkssbsssssgssko',
            'orksgsssbsssssko',
            'orkssssshsssbsko',
            'orksbsssssssgsko',
            'orkssssshsssssko',
            'orksgssssbssssko',
            'orksssbsssgsssko',
            'orkssssssssbssko',
            'orksbsssgsssssko',
            'orkkkkkkkkkkkkko',
            'orrrrrrrrrrrrrro',
            'oooooooooooooooo'
        )
    }
    # Pot exterior: dark clay over a low fire.
    'cooking_pot_side' = @{
        palette = @{
            'o' = '4A4A52'; 'd' = '3A3A42'; 'l' = '5E5E68'; 'k' = '23232A'
            'f' = 'C8641E'; 'y' = 'E8A83A'; 'a' = '2A2226'
        }
        rows = @(
            'llllllllllllllll',
            'oooooooooooooooo',
            'oddddddddddddddo',
            'odlllllllllllldo',
            'oddddddddddddddo',
            'oddddddddddddddo',
            'odddddkkkkdddddo',
            'oddddkddddkddddo',
            'oddddkddddkddddo',
            'odddddkkkkdddddo',
            'oddddddddddddddo',
            'oaaaaaaaaaaaaaao',
            'afyffafyyfaffyfa',
            'ayffyafyfyaffffa',
            'affafayffaafafaa',
            'aaaaaaaaaaaaaaaa'
        )
    }
    # Ombiasy's herb table: dried plants and divination beads on worn wood.
    'herb_table_top' = @{
        palette = @{
            'w' = '8A6A42'; 'd' = '6E5232'; 'l' = 'A08050'; 'k' = '4A3722'
            'g' = '4E7A34'; '1' = '69A046'; 'r' = 'B03C3C'; 'y' = 'D8B44A'; 'p' = '8C4A9E'
        }
        rows = @(
            'kkkkkkkkkkkkkkkk',
            'klwwlwwlwwlwwlwk',
            'kwddwwddwwddwwdk',
            'kwg1gwwlwwrrwwwk',
            'kwg1gwwdwwrrwwdk',
            'kwdggwwwwwwwwwwk',
            'kwwwwwlwwppwwwdk',
            'kwwlwwdwwppwwwwk',
            'kwyywwwwwwww1gwk',
            'kwyywwdwwwww11gk',
            'kwwwwwwwwlwwggwk',
            'kwdwwwwrrwwwwwdk',
            'kwwwwlwwrrwwlwwk',
            'kwddwwddwwddwwdk',
            'klwwlwwlwwlwwlwk',
            'kkkkkkkkkkkkkkkk'
        )
    }
    'herb_table_side' = @{
        palette = @{
            'w' = '8A6A42'; 'd' = '6E5232'; 'l' = 'A08050'; 'k' = '4A3722'; 'g' = '4E7A34'
        }
        rows = @(
            'llllllllllllllll',
            'kkkkkkkkkkkkkkkk',
            'wwddwwddwwddwwdd',
            'dwwddwwddwwddwwd',
            'kkkkkkkkkkkkkkkk',
            'wddwwddwwddwwddw',
            'wwddwwddwwddwwdd',
            'kkkkkkkkkkkkkkkk',
            'dwgwwddwgwddwwgd',
            'wwddwwddwwddwwdd',
            'kkkkkkkkkkkkkkkk',
            'wddwwddwwddwwddw',
            'dwwddwwddwwddwwd',
            'kkkkkkkkkkkkkkkk',
            'wwddwwddwwddwwdd',
            'kkkkkkkkkkkkkkkk'
        )
    }
    # Vanilla drying rack: cured pods laid across slats.
    'drying_rack_top' = @{
        palette = @{
            'w' = '9A7A4E'; 'd' = '7A5C38'; 'k' = '3E2E1C'
            'v' = '2E2118'; '2' = '4A3524'
        }
        rows = @(
            'kkkkkkkkkkkkkkkk',
            'kwwwwwwwwwwwwwwk',
            'kddddddddddddddk',
            'kvvvvvvvvvvvvvvk',
            'k22222222222222k',
            'kwwwwwwwwwwwwwwk',
            'kddddddddddddddk',
            'kvvvvvvvvvvvvvvk',
            'k22222222222222k',
            'kwwwwwwwwwwwwwwk',
            'kddddddddddddddk',
            'kvvvvvvvvvvvvvvk',
            'k22222222222222k',
            'kwwwwwwwwwwwwwwk',
            'kddddddddddddddk',
            'kkkkkkkkkkkkkkkk'
        )
    }
    'drying_rack_side' = @{
        palette = @{
            'w' = '9A7A4E'; 'd' = '7A5C38'; 'k' = '3E2E1C'; 'h' = '6A4E30'; 'v' = '2E2118'
        }
        rows = @(
            'wwwwwwwwwwwwwwww',
            'kkkkkkkkkkkkkkkk',
            'hddddddddddddddh',
            'hdwwdwwdwwdwwddh',
            'hddddddddddddddh',
            'kkkkkkkkkkkkkkkk',
            'hvhhhhhhhhhhhhvh',
            'hddddddddddddddh',
            'hdwwdwwdwwdwwddh',
            'hddddddddddddddh',
            'kkkkkkkkkkkkkkkk',
            'hvhhhhhhhhhhhhvh',
            'hddddddddddddddh',
            'hdwwdwwdwwdwwddh',
            'hddddddddddddddh',
            'kkkkkkkkkkkkkkkk'
        )
    }
}

function ConvertTo-Color($hex) {
    return [System.Drawing.Color]::FromArgb(255,
        [Convert]::ToInt32($hex.Substring(0,2),16),
        [Convert]::ToInt32($hex.Substring(2,2),16),
        [Convert]::ToInt32($hex.Substring(4,2),16))
}

# Anything not in a palette comes out magenta, so mistakes are impossible to miss.
$missing = [System.Drawing.Color]::FromArgb(255, 255, 0, 255)
$problems = 0

foreach ($name in ($textures.Keys | Sort-Object)) {
    $spec = $textures[$name]
    $palette = @{}
    foreach ($k in $spec.palette.Keys) { $palette[$k] = ConvertTo-Color $spec.palette[$k] }

    if ($spec.rows.Count -ne 16) {
        Write-Output "  $name has $($spec.rows.Count) rows, expected 16"; $problems++
    }

    $bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 16; $y++) {
        $row = $spec.rows[$y]
        if ($row.Length -ne 16) { Write-Output "  $name row $y is $($row.Length) chars"; $problems++ }
        for ($x = 0; $x -lt 16; $x++) {
            $ch = if ($x -lt $row.Length) { $row.Substring($x, 1) } else { ' ' }
            $col = if ($palette.ContainsKey($ch)) { $palette[$ch] } else { $missing; $problems++ }
            $bmp.SetPixel($x, $y, $col)
        }
    }

    $bmp.Save((Join-Path $outDir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Output "wrote $name.png"
}

if ($problems -gt 0) { Write-Output "$problems problems - look for magenta pixels" } else { Write-Output 'all textures clean' }
