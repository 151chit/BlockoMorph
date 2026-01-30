package net.blockomorph.mixins.neoforge;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@Shadow @Final protected ServerPlayer player;

	@Inject(method = "removeBlock", at = @At(value = "HEAD"), remap = false)
	public void crackBlockStart(BlockPos pos, BlockState state, boolean canHarvest, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			pl.breakingModeStart(true);
		}, null, false);
	}

	@Inject(method = "removeBlock", at = @At(value = "TAIL"), remap = false)
	public void crackBlockEnd(BlockPos pos, BlockState state, boolean canHarvest, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			boolean flag = pl.isBreaking();
			pl.breakingModeStart(false);
			if (flag && pl.getBlocksData2().size() == 1 && pl.getBlockState(InPlayerBlockPos.ZERO) == Blocks.VOID_AIR.defaultBlockState()) {
				MorphUtils.destroy(pl, this.player);
			}
		}, null, false);
	}
}
