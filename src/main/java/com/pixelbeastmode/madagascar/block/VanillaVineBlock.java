package com.pixelbeastmode.madagascar.block;

import com.mojang.serialization.MapCodec;
import com.pixelbeastmode.madagascar.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Vanilla, the orchid.
 * <p>
 * Madagascar grows most of the world's vanilla, and every flower of it is
 * pollinated by hand. The plant's natural pollinator is a bee that lives in
 * Mexico and not here, so without a person the flower simply fails. The
 * technique was worked out in 1841 by Edmond Albius, a twelve-year-old enslaved
 * boy on Reunion, and it is still done the same way.
 * <p>
 * So this crop grows on its own up to flowering and then stops. It will never
 * ripen unless a player right-clicks the flower. That is the whole point: a crop
 * you cannot walk away from.
 */
public class VanillaVineBlock extends CropBlock {

	public static final MapCodec<VanillaVineBlock> CODEC = simpleCodec(VanillaVineBlock::new);

	/** Stage 0 sprout, 1 vine, 2 flowering, 3 ripe pods. */
	public static final int MAX_AGE = 3;

	/** Growth halts here until a player pollinates the flower. */
	public static final int FLOWERING_AGE = 2;

	public VanillaVineBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends CropBlock> codec() {
		return CODEC;
	}

	@Override
	protected IntegerProperty getAgeProperty() {
		return BlockStateProperties.AGE_3;
	}

	@Override
	public int getMaxAge() {
		return MAX_AGE;
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return ModItems.VANILLA_BEAN;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.AGE_3);
	}

	/** Time alone will not get a flower past pollination. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (getAge(state) == FLOWERING_AGE) {
			return;
		}
		super.randomTick(state, level, pos, random);
	}

	/** Neither will bone meal, otherwise it would trivially skip the whole mechanic. */
	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return getAge(state) != FLOWERING_AGE && super.isValidBonemealTarget(level, pos, state);
	}

	/** Right-clicking a flowering vine pollinates it by hand. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
			Player player, BlockHitResult hit) {
		if (getAge(state) != FLOWERING_AGE) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide()) {
			level.setBlock(pos, getStateForAge(FLOWERING_AGE + 1), Block.UPDATE_CLIENTS);
			level.playSound(null, pos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 0.8F, 1.2F);
		}

		return InteractionResult.SUCCESS;
	}
}
