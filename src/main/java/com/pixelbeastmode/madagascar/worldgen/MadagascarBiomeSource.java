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
 * STAGE 1 — placeholder. This is deliberately crude: a circle of "land" biome
 * around the origin, ocean everywhere else. The only question it answers is
 * whether the game calls our generator at all. The real Madagascar outline
 * arrives in stage 2.
 * <p>
 * Important: a BiomeSource decides <em>which biome</em>, not <em>what the
 * terrain looks like</em>. Terrain shape comes from the noise settings named in
 * the world preset. Because stage 1 borrows vanilla's overworld noise settings,
 * the land/sea shape will NOT match this circle yet — you will see ocean biome
 * colours on top of normal hills. That mismatch is expected, and fixing it is
 * what stage 2 is for.
 */
public class MadagascarBiomeSource extends BiomeSource {

	/**
	 * Tells the game how to read this biome source out of the world preset JSON.
	 * The field names here ("land", "ocean") are exactly the keys used in
	 * data/madagascar/worldgen/world_preset/madagascar.json.
	 */
	public static final MapCodec<MadagascarBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Biome.CODEC.fieldOf("land").forGetter(source -> source.land),
		Biome.CODEC.fieldOf("ocean").forGetter(source -> source.ocean)
	).apply(instance, MadagascarBiomeSource::new));

	/** Radius of the placeholder island, in blocks. */
	private static final int ISLAND_RADIUS = 1200;

	private final Holder<Biome> land;
	private final Holder<Biome> ocean;

	public MadagascarBiomeSource(Holder<Biome> land, Holder<Biome> ocean) {
		this.land = land;
		this.ocean = ocean;
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
		return Stream.of(land, ocean);
	}

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		// Biome coordinates are quarter-scale: one unit here is four blocks.
		// Forgetting this is a classic worldgen bug - everything comes out 4x too small.
		int blockX = QuartPos.toBlock(quartX);
		int blockZ = QuartPos.toBlock(quartZ);

		long distanceSq = (long) blockX * blockX + (long) blockZ * blockZ;
		return distanceSq <= (long) ISLAND_RADIUS * ISLAND_RADIUS ? land : ocean;
	}
}
