package net.blockomorph.mixins.fabric;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
	@Shadow @Final protected ServerPlayer player;

	@Inject(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;preventsBlockDrops()Z"))
	public void crackBlockEnd(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(blockPos, (pl, realPos) -> {
			boolean flag = pl.isBreaking();
			pl.breakingModeStart(false);
			if (flag && pl.getBlocksData2().size() == 1 && pl.getBlockState(InPlayerBlockPos.ZERO) == Blocks.VOID_AIR.defaultBlockState()) {
				MorphUtils.destroy(pl, this.player);
			}
		}, null, false);
	}

	@Inject(method = "useItemOn", at = @At(ordinal = 2, value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"), cancellable = true)
	public void checkAccess(ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
		CommonPlatformUtils.onRightClick(serverPlayer, interactionHand, blockHitResult, cir);
	}
}
