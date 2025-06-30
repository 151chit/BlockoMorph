package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = ServerPlayerGameMode.class, priority = 1020)
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

	@Inject(method = "useItemOn", at = @At(shift = At.Shift.BEFORE, value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"), cancellable = true)
	public void runTnt(ServerPlayer pl, Level lv, ItemStack stack, InteractionHand hand, BlockHitResult res, CallbackInfoReturnable<InteractionResult> cir) {
		InPlayerBlockPos.check(res.getBlockPos(), (player, realPos) -> {
			InteractionResult result = player.getTntHandler().clickTnt(pl, hand, realPos);
			if (result != null) {
				cir.setReturnValue(result);
			}
		}, null, false);
	}

	@ModifyVariable(method = "useItemOn", at = @At(value = "STORE"))
	private UseOnContext changeCtx(UseOnContext value) {
		return MorphUtils.checkOnRealIfOut(value, value.getItemInHand());
	}


	@Inject(method = "useItemOn", at = @At(ordinal = 2, shift = At.Shift.BEFORE, value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"), cancellable = true)
	public void checkAccess(ServerPlayer serverPlayer, Level level, ItemStack itemStack, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
		MorphUtils.onRightClick(serverPlayer, interactionHand, blockHitResult, cir);
	}
}
