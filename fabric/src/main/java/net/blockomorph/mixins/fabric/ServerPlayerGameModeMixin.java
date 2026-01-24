package net.blockomorph.mixins.fabric;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@Shadow @Final protected ServerPlayer player;

	@Inject(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;"))
	public void crackBlockStart(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> {
			pl.breakingModeStart(true);
		}, null, false);
	}

	@Inject(method = "destroyBlock", at = @At(shift = At.Shift.BEFORE, value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;isCreative()Z"))
	public void crackBlockEnd(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> {
			boolean flag = pl.isBreaking();
			pl.breakingModeStart(false);
			if (flag && pl.getBlocksData2().size() == 1 && pl.getBlockState(InPlayerBlockPos.ZERO) == Blocks.VOID_AIR.defaultBlockState()) {
				MorphUtils.destroy(pl, this.player);
			}
		}, null, false);
	}
}
