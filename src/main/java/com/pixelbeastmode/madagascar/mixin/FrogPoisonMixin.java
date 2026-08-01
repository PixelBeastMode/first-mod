package com.pixelbeastmode.madagascar.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes frogs poisonous to touch.
 * <p>
 * Madagascar's mantellas are unrelated to South American dart frogs but evolved
 * the same warning colours and the same skin toxins, so bright yellow really does
 * mean do not handle.
 * <p>
 * Poison is exactly the right vanilla effect here: it damages down to half a
 * heart and then stops, so it can never actually kill. Brutal but survivable, and
 * a bucket of milk clears it.
 */
@Mixin(Frog.class)
public class FrogPoisonMixin {

	/** Long enough for Poison II to take a player from full health to half a heart. */
	private static final int POISON_TICKS = 300;

	/** Amplifier 1 is Poison II, which damages every 12 ticks rather than every 25. */
	private static final int POISON_LEVEL = 1;

	/** A little larger than the frog, so brushing past counts as contact. */
	private static final double TOUCH_MARGIN = 0.15;

	@Inject(method = "tick", at = @At("TAIL"))
	private void madagascar$poisonOnTouch(CallbackInfo callback) {
		Frog frog = (Frog) (Object) this;
		Level level = frog.level();

		if (level.isClientSide()) {
			return;
		}

		for (Player player : level.players()) {
			if (player.isCreative() || player.isSpectator()) {
				continue;
			}
			if (!player.getBoundingBox().intersects(frog.getBoundingBox().inflate(TOUCH_MARGIN))) {
				continue;
			}

			// Do not refresh a poisoning that is already at least this strong,
			// otherwise standing on a frog would hold you at half a heart forever.
			MobEffectInstance current = player.getEffect(MobEffects.POISON);
			if (current != null && current.getAmplifier() >= POISON_LEVEL) {
				continue;
			}

			player.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, POISON_LEVEL));
		}
	}
}
