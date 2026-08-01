package com.pixelbeastmode.madagascar.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Decides which biome sits at each position in the Madagascar world.
 * <p>
 * The shape comes from {@link IslandShape}, which reads a traced outline of the
 * real coastline. This class only maps each region onto a biome.
 * <p>
 * STAGE 2 — the regions are real but the biomes are vanilla stand-ins, chosen to
 * be visually distinct from the air so the layout is easy to check. Purpose-built
 * Madagascar biomes replace them in stage 3.
 * <p>
 * Important: a BiomeSource decides <em>which biome</em>, not <em>where the land
 * is</em>. Terrain shape comes from the noise settings named in the world preset,
 * which are still vanilla's. So the coastline you see here is painted on, not
 * carved - ocean biome will still sit on top of hills. Making the terrain agree
 * with the mask is the second half of stage 2.
 */
public class MadagascarBiomeSource extends BiomeSource {

	/**
	 * Tells the game how to read this biome source out of the world preset JSON.
	 * The field names below are exactly the keys used in
	 * data/madagascar/worldgen/world_preset/madagascar.json.
	 */
	public static final MapCodec<MadagascarBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Biome.CODEC.fieldOf("ocean").forGetter(source -> source.ocean),
		Biome.CODEC.fieldOf("beach").forGetter(source -> source.beach),
		Biome.CODEC.fieldOf("rainforest").forGetter(source -> source.rainforest),
		Biome.CODEC.fieldOf("highlands").forGetter(source -> source.highlands),
		Biome.CODEC.fieldOf("dry_west").forGetter(source -> source.dryWest),
		Biome.CODEC.fieldOf("spiny_south").forGetter(source -> source.spinySouth),
		Biome.CODEC.fieldOf("tsingy").forGetter(source -> source.tsingy),
		Biome.CODEC.fieldOf("isalo").forGetter(source -> source.isalo)
	).apply(instance, MadagascarBiomeSource::new));

	private final Holder<Biome> ocean;
	private final Holder<Biome> beach;
	private final Holder<Biome> rainforest;
	private final Holder<Biome> highlands;
	private final Holder<Biome> dryWest;
	private final Holder<Biome> spinySouth;
	private final Holder<Biome> tsingy;
	private final Holder<Biome> isalo;

	public MadagascarBiomeSource(Holder<Biome> ocean, Holder<Biome> beach, Holder<Biome> rainforest,
			Holder<Biome> highlands, Holder<Biome> dryWest, Holder<Biome> spinySouth,
			Holder<Biome> tsingy, Holder<Biome> isalo) {
		this.ocean = ocean;
		this.beach = beach;
		this.rainforest = rainforest;
		this.highlands = highlands;
		this.dryWest = dryWest;
		this.spinySouth = spinySouth;
		this.tsingy = tsingy;
		this.isalo = isalo;
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	/**
	 * Every biome this source can ever return. The game uses this up front to
	 * build colour maps and locate structures, so anything missing here will
	 * misbehave even if getNoiseBiome returns it.
	 */
	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return Stream.of(ocean, beach, rainforest, highlands, dryWest, spinySouth, tsingy, isalo);
	}

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		// Biome coordinates are quarter-scale: one unit here is four blocks.
		// Forgetting this is a classic worldgen bug - everything comes out 4x too small.
		int blockX = QuartPos.toBlock(quartX);
		int blockZ = QuartPos.toBlock(quartZ);

		return forRegion(IslandShape.get().regionAt(blockX, blockZ));
	}

	private Holder<Biome> forRegion(IslandShape.Region region) {
		return switch (region) {
			case OCEAN -> ocean;
			case BEACH -> beach;
			case RAINFOREST -> rainforest;
			case HIGHLANDS -> highlands;
			case DRY_WEST -> dryWest;
			case SPINY_SOUTH -> spinySouth;
			case TSINGY -> tsingy;
			case ISALO -> isalo;
		};
	}
}
