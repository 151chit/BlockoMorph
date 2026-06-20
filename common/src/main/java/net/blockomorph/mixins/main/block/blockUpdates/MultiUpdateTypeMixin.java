package net.blockomorph.mixins.main.block.blockUpdates;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.redstone.CollectingNeighborUpdater$MultiNeighborUpdate")
public class MultiUpdateTypeMixin {

	@Shadow @Final private Block sourceBlock;
	@Shadow @Final private BlockPos sourcePos;

	@ModifyVariable(method = "runNext", at = @At("STORE"))
	public BlockState disableWorldAutoread(BlockState orig, Level lv, @Local BlockPos origPos) {
		return MorphUtils.lockExternalMorphedGetter(orig, lv, origPos);
	}

	@ModifyVariable(method = "runNext", at = @At("STORE"))
	public BlockPos disableWorldAutoread(BlockPos orig, Level lv) {
		return MorphUtils.normalizeToRealOnOutline(orig, lv);
	}

	@Inject(method = "runNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/redstone/NeighborUpdater;executeUpdate(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V"))
	private void updatePlayers(Level lv, CallbackInfoReturnable<Boolean> cir, @Local BlockPos target) {
		MorphUtils.checkMorphs(lv, target, this.sourcePos, this.sourceBlock);
	}
}
