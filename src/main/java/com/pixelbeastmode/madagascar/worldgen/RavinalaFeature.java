package com.pixelbeastmode.madagascar.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ravenala madagascariensis, the traveller's palm and Madagascar's national symbol.
 * <p>
 * This exists as Java rather than a data-driven tree because the plant's whole
 * identity is its shape: the fronds splay out in a single vertical plane, like a
 * hand fan stood on its edge. Every vanilla foliage placer builds foliage around
 * a trunk in all directions, which turns a ravinala into a generic shrub on a
 * stick.
 */
public class RavinalaFeature extends Feature<NoneFeatureConfiguration> {

	private static final int MIN_TRUNK = 3;
	private static final int MAX_TRUNK = 6;

	/** Odd, so one frond points straight up the middle of the fan. */
	private static final int FRONDS = 9;

	/** How far the outermost fronds lean from vertical. */
	private static final double SPREAD = Math.toRadians(80.0);

	private static final int MIN_FROND = 4;
	private static final int MAX_FROND = 7;

	public RavinalaFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();

		if (!level.getBlockState(origin.below()).is(BlockTags.DIRT)) {
			return false;
		}

		int trunkHeight = MIN_TRUNK + random.nextInt(MAX_TRUNK - MIN_TRUNK + 1);

		// Refuse rather than grow through terrain.
		for (int y = 0; y < trunkHeight + 2; y++) {
			if (!level.isEmptyBlock(origin.above(y))) {
				return false;
			}
		}

		BlockState log = Blocks.JUNGLE_LOG.defaultBlockState();
		// Persistent, because the outer fronds sit far enough from the trunk that
		// normal leaf decay would eat the ends of the fan.
		BlockState leaf = Blocks.JUNGLE_LEAVES.defaultBlockState()
			.setValue(BlockStateProperties.PERSISTENT, true);

		for (int y = 0; y < trunkHeight; y++) {
			setBlock(level, origin.above(y), log);
		}

		BlockPos crown = origin.above(trunkHeight - 1);

		// The fan lies in one vertical plane, so pick which way it faces.
		Direction facing = random.nextBoolean() ? Direction.EAST : Direction.SOUTH;
		int axisX = facing.getStepX();
		int axisZ = facing.getStepZ();

		for (int i = 0; i < FRONDS; i++) {
			// -1 at one edge of the fan, 0 up the middle, +1 at the other edge.
			double t = (2.0 * i / (FRONDS - 1)) - 1.0;
			double angle = t * SPREAD;

			double outward = Math.sin(angle);
			double upward = Math.cos(angle);

			int length = MIN_FROND + random.nextInt(MAX_FROND - MIN_FROND + 1);
			// Outer fronds are shorter, which is what gives the fan its curve.
			length = Math.max(2, (int) Math.round(length * (1.0 - 0.35 * Math.abs(t))));

			placeFrond(level, crown, axisX, axisZ, outward, upward, length, leaf);
		}

		return true;
	}

	private void placeFrond(WorldGenLevel level, BlockPos crown, int axisX, int axisZ,
			double outward, double upward, int length, BlockState leaf) {
		for (int step = 1; step <= length; step++) {
			int out = (int) Math.round(outward * step);
			int up = (int) Math.round(upward * step);

			BlockPos pos = crown.offset(axisX * out, up, axisZ * out);
			if (level.isEmptyBlock(pos)) {
				setBlock(level, pos, leaf);
			}

			// Thicken the inner half so the fan reads as leaves, not wire.
			if (step <= length / 2) {
				BlockPos below = pos.below();
				if (up > 0 && level.isEmptyBlock(below)) {
					setBlock(level, below, leaf);
				}
			}
		}
	}
}
