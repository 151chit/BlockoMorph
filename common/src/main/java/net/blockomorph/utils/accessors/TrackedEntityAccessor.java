package net.blockomorph.utils.accessors;

import net.minecraft.server.network.ServerPlayerConnection;

import java.util.Set;

public interface TrackedEntityAccessor {

	Set<ServerPlayerConnection> getSeenBy$blockomorph();
}
