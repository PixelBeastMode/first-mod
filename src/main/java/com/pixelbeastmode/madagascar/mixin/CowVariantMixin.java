package com.pixelbeastmode.madagascar.mixin;

import com.pixelbeastmode.madagascar.MadagascarMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.level.ServerLevelAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes roughly one zebu in fifty an albino.
 * <p>
 * Cow variants are data-driven in 26.2, but the only spawn conditions vanilla
 * offers are biome, structure and moon brightness - none of which can express a
 * flat chance. So the variant itself is declared in JSON with no spawn
 * conditions, meaning it is never chosen naturally, and the roll happens here.
 * <p>
 * finalizeSpawn is the right hook because it runs once when the animal first
 * enters the world. Rolling on entity load instead would re-roll the dice every
 * time the chunk was reloaded, and a zebu would change colour as you walked away
 * and came back.
 */
@Mixin(Cow.class)
public class CowVariantMixin {

	/** One in this many. */
	private static final int ALBINO_ONE_IN = 50;

	@Inject(method = "finalizeSpawn", at = @At("RETURN"))
	private void madagascar$maybeAlbino(ServerLevelAccessor level, DifficultyInstance difficulty,
			EntitySpawnReason reason, SpawnGroupData spawnData,
			CallbackInfoReturnable<SpawnGroupData> callback) {
		Cow cow = (Cow) (Object) this;

		if (cow.getRandom().nextInt(ALBINO_ONE_IN) != 0) {
			return;
		}

		// Injecting at RETURN means vanilla has already picked a variant from the
		// biome, so this overwrites it.
		Registry<CowVariant> variants = level.registryAccess().lookupOrThrow(Registries.COW_VARIANT);
		variants.get(MadagascarMod.id("albino_zebu")).ifPresent(cow::setVariant);
	}
}
