package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.accessors.TrackedEntityAccessor;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public class TrackedEntityMixin implements TrackedEntityAccessor {


	@Shadow @Final Set<ServerPlayerConnection> seenBy;

	@Override
	public Set<ServerPlayerConnection> getSeenBy$blockomorph() {
		return this.seenBy;
	}
}
