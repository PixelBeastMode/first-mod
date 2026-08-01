package com.pixelbeastmode.madagascar.mixin.client;

import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns every cow into a zebu by giving it a shoulder hump and long horns.
 * <p>
 * Recolouring alone left them reading as cows, because a zebu's silhouette is
 * the thing that identifies it. This is a model change rather than a new entity,
 * so vanilla breeding, drops and spawning all keep working untouched.
 * <p>
 * Hooking {@code createBaseCowModel} covers every variant: the warm, cold and
 * temperate cow models all call it before adding their own details.
 */
@Mixin(CowModel.class)
public class CowModelMixin {

	/**
	 * Vanilla horn texture region. Reused so the new horn segments are
	 * horn-coloured rather than sampling something arbitrary.
	 */
	private static final int HORN_U = 22;
	private static final int HORN_V = 0;

	/** Vanilla body texture region, so the hump looks like hide. */
	private static final int BODY_U = 18;
	private static final int BODY_V = 4;

	@Inject(method = "createBaseCowModel", at = @At("RETURN"))
	private static void madagascar$makeZebu(CallbackInfoReturnable<MeshDefinition> callback) {
		PartDefinition root = callback.getReturnValue().getRoot();
		PartDefinition body = root.getChild("body");
		PartDefinition head = root.getChild("head");

		// The body is rotated a quarter turn about X. In its local space the long
		// axis (y) runs front to back, and +z is UP - which vanilla's own udder
		// cube confirms, sitting at z=-8 and hanging below the animal. The back of
		// the cow is therefore local z=3, and the hump has to climb from there.
		body.addOrReplaceChild("zebu_hump",
			CubeListBuilder.create()
				.texOffs(BODY_U, BODY_V).addBox(-3.5F, -9.0F, 2.0F, 7.0F, 6.0F, 4.0F)
				.texOffs(BODY_U, BODY_V).addBox(-2.5F, -8.0F, 6.0F, 5.0F, 4.0F, 2.0F),
			PartPose.ZERO);

		// Horns, 2x2 thick and sweeping outward before turning up. Segments overlap
		// rather than meeting corner to corner, which would leave visible gaps. The
		// first one is sized to swallow vanilla's thin horn stub, still present in
		// the head's cube list and otherwise poking out of the side.
		head.addOrReplaceChild("zebu_horns",
			CubeListBuilder.create()
				// right horn: over the stub, then out, then up
				.texOffs(HORN_U, HORN_V).addBox(-6.0F, -6.0F, -5.0F, 2.0F, 3.0F, 2.0F)
				.texOffs(HORN_U, HORN_V).addBox(-7.5F, -8.0F, -5.0F, 2.0F, 3.0F, 2.0F)
				.texOffs(HORN_U, HORN_V).addBox(-8.5F, -11.0F, -5.0F, 2.0F, 4.0F, 2.0F)
				// left horn, mirrored
				.texOffs(HORN_U, HORN_V).addBox(4.0F, -6.0F, -5.0F, 2.0F, 3.0F, 2.0F)
				.texOffs(HORN_U, HORN_V).addBox(5.5F, -8.0F, -5.0F, 2.0F, 3.0F, 2.0F)
				.texOffs(HORN_U, HORN_V).addBox(6.5F, -11.0F, -5.0F, 2.0F, 4.0F, 2.0F),
			PartPose.ZERO);
	}
}
