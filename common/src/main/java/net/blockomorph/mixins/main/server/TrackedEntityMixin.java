package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.accessors.ServerEntityAccessor;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public class TrackedEntityMixin {
	@Shadow @Final ServerEntity serverEntity;
	@Shadow @Final private Set<ServerPlayerConnection> seenBy;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void init(ChunkMap this$0, Entity p_140478_, int p_140479_, int p_140480_, boolean p_140481_, CallbackInfo ci) {
		if (this.serverEntity instanceof ServerEntityAccessor acc) {
			acc.assignSeenBy$blockomorph(() -> this.seenBy);
		}
	}
}
