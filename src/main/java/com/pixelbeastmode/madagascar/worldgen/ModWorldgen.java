package com.pixelbeastmode.madagascar.worldgen;

import com.pixelbeastmode.madagascar.MadagascarMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Registers this mod's world generation pieces.
 */
public final class ModWorldgen {

	/**
	 * The traveller's palm. Its fan shape cannot be built from vanilla's foliage
	 * placers, so it is a Java feature rather than a tree in JSON.
	 */
	public static final Feature<NoneFeatureConfiguration> RAVINALA =
		new RavinalaFeature(NoneFeatureConfiguration.CODEC);

	/**
	 * Called from {@link MadagascarMod#onInitialize()}.
	 * <p>
	 * Registering the codec is what lets the world preset JSON say
	 * {@code "type": "madagascar:island"} and have the game know what that means.
	 * Without this, the preset fails to parse and the world type silently
	 * disappears from the dropdown.
	 */
	public static void initialize() {
		Registry.register(BuiltInRegistries.BIOME_SOURCE, MadagascarMod.id("island"), MadagascarBiomeSource.CODEC);

		// Lets the noise settings say {"type": "madagascar:island"} to shape terrain.
		Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, MadagascarMod.id("island"),
			MadagascarTerrain.CODEC.codec());

		// Lets configured_feature/ravinala.json say {"type": "madagascar:ravinala"}.
		Registry.register(BuiltInRegistries.FEATURE, MadagascarMod.id("ravinala"), RAVINALA);
	}

	private ModWorldgen() {
	}
}
