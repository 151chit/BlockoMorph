package net.blockomorph.mixins.neoforge;

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

	@Inject(method = "removeBlock", at = @At(value = "HEAD"), remap = false)
	public void crackBlockStart(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			pl.breakingModeStart(true);
		}, null, false);
	}

	@Inject(method = "removeBlock", at = @At(value = "TAIL"), remap = false)
	public void crackBlockEnd(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			boolean flag = pl.isBreaking();
			pl.breakingModeStart(false);
			if (flag && pl.getBlocksData2().size() == 1 && pl.getBlockState(InPlayerBlockPos.ZERO) == Blocks.VOID_AIR.defaultBlockState()) {
				MorphUtils.destroy(pl, this.player);
			}
		}, null, false);
	}

	@Inject(method = "useItemOn", at = @At(ordinal = 1, value = "INVOKE", target = "Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$RightClickBlock;getUseItem()Lnet/minecraftforge/eventbus/api/Event$Result;", remap = false), cancellable = true)
	public void checkAccess(ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
		CommonPlatformUtils.onRightClick(serverPlayer, interactionHand, blockHitResult, cir);
	}
}
