package net.blockomorph.mixins.main.client;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
	@Shadow protected abstract void startPrediction(ClientLevel p_233730_, PredictiveAction p_233731_);
	@Shadow @Final private Minecraft minecraft;

	@ModifyVariable(method = "destroyBlock", at = @At(value = "STORE"))
	public FluidState crackBlockStart(FluidState value, BlockPos pos) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			pl.breakingModeStart(true);
		}, null, true);
		return value;
	}

	@Inject(method = "destroyBlock", at = @At(value = "RETURN", ordinal = 4))
	public void crackBlockEnd(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			pl.breakingModeStart(false);
		}, null, true);
	}

	@Inject(method = "useItemOn", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;ensureHasSentCarriedItem()V"), cancellable = true)
	public void checkAccess(LocalPlayer pl, InteractionHand hand, BlockHitResult res, CallbackInfoReturnable<InteractionResult> cir) {
		if (MorphUtils.needRejectUse(pl.level(), res)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}

	@Inject(method = "useItemOn", at = @At(shift = At.Shift.BEFORE, value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V"), cancellable = true)
	public void checkAccessToPlace(LocalPlayer pl, InteractionHand hand, BlockHitResult res, CallbackInfoReturnable<InteractionResult> cir) {
		InPlayerBlockPos.check(res.getBlockPos(), (player, realPos) -> {
			InteractionResult result = player.getTntHandler().clickTnt(pl, hand, realPos);
			if (result != null) {
				this.startPrediction(this.minecraft.level, (id) -> {
					cir.setReturnValue(result);
					return new ServerboundUseItemOnPacket(hand, res, id);
				});
			}
		}, null, true);
	}

	@ModifyVariable(method = "performUseItemOn", at = @At(value = "STORE"))
	private UseOnContext changeCtx(UseOnContext value) {
		return MorphUtils.checkOnRealIfOut(value, value.getItemInHand());
	}



	@Inject(method = "performUseItemOn", at = @At(shift = At.Shift.BEFORE, value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"), cancellable = true)
	public void checkAccessOnPlace(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
		MorphUtils.onRightClick(localPlayer, interactionHand, blockHitResult, cir);
	}
}
