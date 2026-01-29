package net.blockomorph.utils.accessors;

import net.minecraft.server.network.ServerPlayerConnection;

import java.util.Set;
import java.util.function.Supplier;

public interface ServerEntityAccessor {

	void assignSeenBy$blockomorph(Supplier<Set<ServerPlayerConnection>> seenBy);
}