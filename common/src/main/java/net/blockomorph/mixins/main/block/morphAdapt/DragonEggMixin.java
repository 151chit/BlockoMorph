package net.blockomorph.mixins.main.block.morphAdapt;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonEggBlock.class)
public class DragonEggMixin {

	@Inject(method = "teleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), cancellable = true)
	private void tpPlayer(BlockState p_52936_, Level lv, BlockPos origin, CallbackInfo ci, @Local(ordinal = 1) BlockPos pos) {
		InPlayerBlockPos.check(origin, (pl, realPos) -> { //is not a bug by default, it is user logic, like with TNT
			if (pl.player() instanceof ServerPlayer sp) {
				Vec3 vec = InPlayerBlockPos.checkOnReal(pos.getCenter(), origin.getCenter());
				sp.teleportTo(sp.serverLevel(), vec.x, vec.y, vec.z, RelativeMovement.ROTATION, sp.getYRot(), sp.getXRot());
				ci.cancel();
			}
		}, null, lv);
	}
}
