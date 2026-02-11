package net.blockomorph.mixins.main.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;

@Mixin(EuclideanGameEventListenerRegistry.class)
public interface GameEventRegistryAccessor {

	@Invoker("getPostableListenerPosition")
	static Optional<Vec3> handleGameEvent(ServerLevel serverLevel, Vec3 vec3, GameEventListener gameEventListener) {
		throw new AssertionError();
	}
}
