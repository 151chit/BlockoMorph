package net.blockomorph.mixins.main.system.virtualization.normalize.entityPos;

import net.blockomorph.core.coords.math.MorphNormalizer;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPacketListenerMixin {

	@ModifyVariable(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
	public PositionMoveRotation normalizePos(PositionMoveRotation value) {
		Vec3 vec = value.position();
		if (InPlayerBlockPos.isMorphedPlayerBlockX(vec.x)) {
			return new PositionMoveRotation(MorphNormalizer.normalize(vec), value.deltaMovement(), value.yRot(), value.xRot());
		}
		return value;
	}
}