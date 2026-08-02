package com.pixelbeastmode.madagascar.block;

import com.pixelbeastmode.madagascar.MadagascarMod;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

/**
 * The mod's blocks.
 * <p>
 * The three workstations are plain cubes with a distinct top texture, which is
 * exactly what vanilla's own workstations are - the loom, smithing table and
 * cartography table are all full blocks.
 */
public final class ModBlocks {

	/** Where the mpahandro cooks. */
	public static final Block COOKING_POT = register("cooking_pot", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL));

	/** Where the ombiasy, the traditional healer, prepares remedies. */
	public static final Block HERB_TABLE = register("herb_table", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD));

	/** Where vanilla pods are cured. */
	public static final Block DRYING_RACK = register("drying_rack", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD));

	/**
	 * The vanilla orchid. Registered without an item of its own, because it is
	 * planted with the bean - the same way wheat is planted with seeds.
	 */
	public static final Block VANILLA_VINE = registerWithoutItem("vanilla_vine", VanillaVineBlock::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.PLANT)
			.noCollision()
			.randomTicks()
			.instabreak()
			.sound(SoundType.CROP)
			.pushReaction(PushReaction.DESTROY));

	/** Registers a block and the item you place it with. Those are separate registries. */
	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties) {
		Block block = registerWithoutItem(name, factory, properties);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, MadagascarMod.id(name));
		// useBlockDescriptionPrefix makes the name come from block.madagascar.<name>
		// rather than item.madagascar.<name>, matching how vanilla names blocks.
		Registry.register(BuiltInRegistries.ITEM, itemKey,
			new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));

		return block;
	}

	/** For blocks whose item is registered elsewhere, or which have none. */
	private static Block registerWithoutItem(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, MadagascarMod.id(name));
		Block block = factory.apply(properties.setId(blockKey));
		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	/** Called from {@link MadagascarMod#onInitialize()}. */
	public static void initialize() {
		// Registering a block does not put it in the creative menu. Without this
		// the only way to obtain one is the /give command, and searching the
		// creative inventory for it finds nothing.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
			.register(tab -> {
				tab.accept(COOKING_POT);
				tab.accept(HERB_TABLE);
				tab.accept(DRYING_RACK);
			});
	}

	private ModBlocks() {
	}
}
