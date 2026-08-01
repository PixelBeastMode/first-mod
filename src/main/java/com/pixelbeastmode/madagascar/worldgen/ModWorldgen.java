package com.pixelbeastmode.madagascar.worldgen;

import com.pixelbeastmode.madagascar.MadagascarMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Registers this mod's world generation pieces.
 */
public final class ModWorldgen {

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
	}

	private ModWorldgen() {
	}
}
