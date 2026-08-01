package com.pixelbeastmode.madagascar.worldgen;

import com.mojang.serialization.MapCodec;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Turns the island mask into terrain height.
 * <p>
 * A density function returns "how solid" a point is: positive is stone, negative
 * is air. The world's noise settings add this to a vertical gradient, so the
 * number returned here effectively raises or lowers the ground.
 * <p>
 * This is what makes the coastline real. In stage 2a the biomes followed the
 * mask but the land did not, because terrain came from vanilla's noise settings.
 * Now both read the same {@link IslandShape}, so the sea is where the sea is.
 */
public final class MadagascarTerrain implements DensityFunction.SimpleFunction {

	public static final MadagascarTerrain INSTANCE = new MadagascarTerrain();

	/**
	 * No configuration, so the JSON is just {"type": "madagascar:island"}.
	 * MapCodec.unit always decodes to this single shared instance.
	 */
	public static final KeyDispatchDataCodec<MadagascarTerrain> CODEC =
		KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

	// These map onto world heights through the gradient in the noise settings,
	// which spans y=0 to y=192. Surface height works out as 96 * (1 + value).
	private static final double DEEP_SEA = -0.62;    // sea floor near y=36
	private static final double SHORELINE = -0.344;  // exactly sea level, y=63
	private static final double INLAND = -0.16;      // base land height near y=80

	/** How tall the hills are. 0.12 gives roughly a 23-block spread. */
	private static final double HILL_HEIGHT = 0.12;

	/** Mask value that counts as the waterline. Must match IslandShape's sea level. */
	private static final float SEA = 0.5f;

	/** How far past the shoreline the land keeps rising. */
	private static final float INLAND_RAMP = 0.35f;

	private MadagascarTerrain() {
	}

	@Override
	public double compute(FunctionContext context) {
		int blockX = context.blockX();
		int blockZ = context.blockZ();

		float land = IslandShape.get().landAt(blockX, blockZ);

		if (land < SEA) {
			// Offshore: drop away from the shoreline down to the sea floor.
			double t = smoothstep(land / SEA);
			return lerp(DEEP_SEA, SHORELINE, t);
		}

		// Onshore: climb from the waterline up to the inland base height.
		double t = smoothstep(Math.min((land - SEA) / INLAND_RAMP, 1.0));
		double height = lerp(SHORELINE, INLAND, t);

		// Hills fade in as we move inland, so beaches stay flat and walkable.
		return height + IslandShape.get().terrainNoise(blockX, blockZ) * HILL_HEIGHT * t;
	}

	/**
	 * The game uses these to decide how much of a chunk it can skip. They must
	 * actually bound {@link #compute}, or terrain silently goes missing.
	 */
	@Override
	public double minValue() {
		return DEEP_SEA - HILL_HEIGHT;
	}

	@Override
	public double maxValue() {
		return INLAND + HILL_HEIGHT;
	}

	@Override
	public KeyDispatchDataCodec<? extends DensityFunction> codec() {
		return CODEC;
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	/** Eases the ends so the sea floor and the inland plateau meet the coast smoothly. */
	private static double smoothstep(double t) {
		return t * t * (3.0 - 2.0 * t);
	}
}
