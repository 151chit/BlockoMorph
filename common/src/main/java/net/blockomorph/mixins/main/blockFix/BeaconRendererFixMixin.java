package net.blockomorph.mixins.main.blockFix;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BeaconRenderer.class)
public class BeaconRendererFixMixin<T extends BlockEntity & BeaconBeamOwner> { //TODO camPos in BE renderers with key-blockpos

	@ModifyVariable(method = "extract", at = @At(value = "STORE"), ordinal = 1)
	private static <T extends BlockEntity & BeaconBeamOwner> float getF(float value, T BE, BeaconRenderState beaconRenderState, float delta, Vec3 camPos) {
		return (float) camPos.subtract(InPlayerBlockPos.checkOnReal(BE.getBlockPos()).getCenter()).horizontalDistance();
	}
}