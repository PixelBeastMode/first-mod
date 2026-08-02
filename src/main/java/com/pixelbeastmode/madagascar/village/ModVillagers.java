package com.pixelbeastmode.madagascar.village;

import com.google.common.collect.ImmutableSet;
import com.pixelbeastmode.madagascar.MadagascarMod;
import com.pixelbeastmode.madagascar.block.ModBlocks;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.Block;

/**
 * Three Malagasy villager professions.
 * <p>
 * A profession needs two registry entries: a point of interest, which is the
 * workstation block villagers claim, and the profession itself. The trades are
 * not here at all - since 26.2 they live in JSON under data/madagascar, and the
 * profession only names the trade set for each level.
 */
public final class ModVillagers {

	/** Villagers level up through five tiers, and each needs its own trade set. */
	private static final int MAX_LEVEL = 5;

	public static void initialize() {
		// Mpahandro, the cook. Romazava and ravitoto come off this fire.
		register("mpahandro", ModBlocks.COOKING_POT, SoundEvents.VILLAGER_WORK_BUTCHER);

		// Ombiasy, the traditional healer-diviner. Not a European doctor: the
		// ombiasy reads fate and prepares remedies from plants.
		register("ombiasy", ModBlocks.HERB_TABLE, SoundEvents.VILLAGER_WORK_CLERIC);

		// Vanilla grower. Madagascar produces most of the world's vanilla.
		register("vanilla_grower", ModBlocks.DRYING_RACK, SoundEvents.VILLAGER_WORK_FARMER);
	}

	private static void register(String name, Block workstation, SoundEvent workSound) {
		// The point of interest is what makes a block a job site. Every state of
		// the block counts, which for a plain cube is just the one.
		ResourceKey<PoiType> poiKey =
			ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, MadagascarMod.id(name));
		Registry.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, poiKey,
			new PoiType(ImmutableSet.copyOf(workstation.getStateDefinition().getPossibleStates()), 1, 1));

		// Point each level at data/madagascar/trade_set/<name>/level_<n>.json
		Int2ObjectMap<ResourceKey<TradeSet>> tradesByLevel = new Int2ObjectOpenHashMap<>();
		for (int level = 1; level <= MAX_LEVEL; level++) {
			tradesByLevel.put(level, ResourceKey.create(Registries.TRADE_SET,
				MadagascarMod.id(name + "/level_" + level)));
		}

		ResourceKey<VillagerProfession> professionKey =
			ResourceKey.create(Registries.VILLAGER_PROFESSION, MadagascarMod.id(name));
		Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, professionKey,
			new VillagerProfession(
				Component.translatable("entity.madagascar.villager." + name),
				holder -> holder.is(poiKey),   // job site it will keep working at
				holder -> holder.is(poiKey),   // job site it will take up
				ImmutableSet.of(),             // items it wants gathered
				ImmutableSet.of(),             // secondary blocks of interest
				workSound,
				tradesByLevel));
	}

	private ModVillagers() {
	}
}
