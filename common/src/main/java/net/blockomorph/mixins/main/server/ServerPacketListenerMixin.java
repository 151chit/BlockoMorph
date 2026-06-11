package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.playerSection.PlayersMultiSectionStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPacketListenerMixin {

	@Inject(method = "noBlocksAround", at = @At("RETURN"), cancellable = true)
	public void test(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) {
			AABB roundAABB = entity.getBoundingBox().inflate(0.0625F).expandTowards(0.0F, -0.55, 0.0F);
			for (PlayerAccessor pl : PlayersMultiSectionStorage.fromLevel(this.player.level()).findMorphed(entity, roundAABB)) {
				if (!cir.getReturnValue()) return;
				pl.getBlocksData2InArea(roundAABB, (realPos, block, vec3) -> {
					cir.setReturnValue(false);
				});
			}
		}
	}

	@Shadow public ServerPlayer player;

	@Inject(method = "handleUseItemOn", at = @At(ordinal = 1, value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"), cancellable = true)
	private void redirectUpdate(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
		BlockHitResult res = packet.getHitResult();
		BlockPos pos = res.getBlockPos().relative(res.getDirection());
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			ci.cancel();
			this.player.connection.send(new ClientboundBlockUpdatePacket(pos, pl.getBlockState(realPos)));
		}, null, this.player.level());
	}

	@Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isItemEnabled(Lnet/minecraft/world/flag/FeatureFlagSet;)Z"), cancellable = true)
	public void checkAccess(ServerboundUseItemOnPacket pkt, CallbackInfo ci) {
		BlockHitResult hit = pkt.getHitResult();
		InPlayerBlockPos.check(hit.getBlockPos(), (pl, realPos) -> {
			if (!pl.isActive()) {
				ci.cancel();
			}
		}, null, this.player.level());
		if (!ci.isCancelled() && MorphUtils.needRejectUse(this.player.level(), hit)) {
			ci.cancel();
		}
	}

	@Inject(method = "handleInteract", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"), cancellable = true)
	public void checkAccess(ServerboundInteractPacket pkt, CallbackInfo ci) {
		Entity entity = pkt.getTarget(this.player.level());
		if (entity instanceof PlayerAccessor pl && pl.isActive()) {
			if (!Config.get().hitReaction.getValue().hand) ci.cancel();
		}
	}

	@Inject(method = "handlePlayerAction", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"), cancellable = true)
	public void checkAccess(ServerboundPlayerActionPacket pkt, CallbackInfo ci) {
		switch (pkt.getAction()) {
			case START_DESTROY_BLOCK, ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK: {
				InPlayerBlockPos.check(pkt.getPos(), (pl, realPos) -> {
					if (!pl.isFullActive() || Config.get().hitReaction.getValue() != ConfigEnums.HitReaction.BRAKING) {
						ci.cancel();
					}
				}, null, this.player.level());
			}
			default:
		}
	}

	@ModifyVariable(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
	public PositionMoveRotation normalizePos(PositionMoveRotation value) {
		Vec3 vec = value.position();
		if (InPlayerBlockPos.isMorphedPlayerX(vec.x)) {
			return new PositionMoveRotation(InPlayerBlockPos.checkOnReal(vec), value.deltaMovement(), value.yRot(), value.xRot());
		}
		return value;
	}
}
