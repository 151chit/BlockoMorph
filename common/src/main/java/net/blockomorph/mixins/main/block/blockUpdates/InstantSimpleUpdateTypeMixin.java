package net.blockomorph.mixins.main.block.blockUpdates;

import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.InstantNeighborUpdater;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InstantNeighborUpdater.class)
public class InstantSimpleUpdateTypeMixin {

	@Shadow @Final private Level level;

	@ModifyVariable(method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V", at = @At("STORE"))
	public BlockState disableWorldAutoread(BlockState orig, BlockPos origPos) {
		return MorphUtils.lockExternalMorphedGetter(orig, this.level, origPos);
	}

	@ModifyVariable(method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V", at = @At("HEAD"), ordinal = 0)
	public BlockPos disableWorldAutoread(BlockPos orig) {
		return MorphUtils.normalizeToRealOnOutline(orig, this.level);
	}

	@Inject(method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V", at = @At("TAIL"))
	private void checkPlayers(BlockPos blockPos, Block block, BlockPos blockPos2, CallbackInfo ci) {
		MorphUtils.checkMorphs(this.level, blockPos, blockPos2, block);
	}
}
