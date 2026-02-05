package net.blockomorph.mixins.main.blockFix;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BeaconRenderer.class)
public class BeaconRendererFixMixin<T extends BlockEntity & BeaconBeamOwner> { //TODO camPos in BE renderers with key-blockpos

	@ModifyVariable(method = "render", at = @At(value = "STORE"), ordinal = 1)
	public float getF(float orig, T BE, float delta, PoseStack stack, MultiBufferSource buffer, int light, int overlay, Vec3 camPos) {
		return (float) camPos.subtract(InPlayerBlockPos.checkOnReal(BE.getBlockPos()).getCenter()).horizontalDistance();
	}
}