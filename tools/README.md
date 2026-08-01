# tools

Generators for assets that would be painful to maintain by hand. Everything here
writes into `src/main/resources/`, and **the generated files are committed** — you
never need to run these to build the mod. Run them when you want to change the
thing they generate.

Paths are relative to this folder, and the Minecraft version is read from
`gradle.properties`, so nothing here is tied to one machine.

## make_island_mask.ps1

Draws `src/main/resources/madagascar/island_mask.png`, the coastline that decides
where land is.

The important part is the `$coast` array near the top: **36 real longitude and
latitude points** tracing Madagascar, from Cap d'Ambre in the north clockwise
round to the north-west. Edit those to reshape the island. `$W` sets the
resolution; the height is computed from the bounding box's real aspect ratio so
the island cannot come out stretched.

You can also just open the PNG in a paint program and redraw the coast — white is
land, black is sea. The script only matters if you want to go back to real
coordinates.

```powershell
.\tools\make_island_mask.ps1
```

## make_biomes.ps1

Writes all eight biome files in `data/madagascar/worldgen/biome/`.

Each biome inherits `features`, `spawners` and `carvers` from the closest vanilla
biome — that is why every feature and spawner id in them is guaranteed valid —
then overrides temperature, downfall, colours, vegetation and creature spawns from
the `$biomes` table.

Edit the table, not the JSON. Changing one colour across all eight biomes is a
one-line edit here and eight file edits otherwise.

```powershell
.\tools\make_biomes.ps1
```

## make_mob_textures.ps1

Recolours vanilla mob textures into Madagascar's fauna, writing to
`assets/minecraft/textures/entity/`:

| Vanilla | Becomes |
|---------|---------|
| cow     | zebu    |
| ocelot  | fossa   |
| fox     | ring-tailed lemur |

It reads each pixel's brightness and replaces the hue with a target tint, keeping
the original shading. Working from the real textures means the UV layout stays
correct, so nothing lands in the wrong place on the model.

```powershell
.\tools\make_mob_textures.ps1
```

## PreviewMap.java

Renders the island without launching Minecraft. Prints region coverage, elevation
statistics, `/tp` coordinates for every region, and a crease check.

The geometry is **copied from `IslandShape`**. If you change that class, re-copy
it here or the preview will quietly lie to you.

The crease check is the useful part: it samples terrain along a line, measures
curvature, and buckets it by `z mod 14`. If one offset dominates, the mask grid is
showing through as visible terraces. Ratios near 1.0 are clean; 2.0 and above
means the grid is visible. It is also what caught a `Math.pow` NaN that would have
punched silent holes in the world.

```powershell
java tools\PreviewMap.java src\main\resources\madagascar\island_mask.png region.png elevation.png
```

## ScanRegion.java

Counts block and biome names in a generated world, to answer "is this actually
generating?" without flying around looking.

Region files store chunks zlib-compressed; block palettes hold names as plain
UTF-8, so once a chunk is inflated the names can be found directly without a full
NBT parser.

```powershell
java tools\ScanRegion.java "run\saves\<world>\dimensions\minecraft\overworld\region" minecraft:cherry_log madagascar:menabe_dry_forest
```

This is how we established that baobabs were generating fine and only 0.7% of the
explored world was the biome they grow in.
