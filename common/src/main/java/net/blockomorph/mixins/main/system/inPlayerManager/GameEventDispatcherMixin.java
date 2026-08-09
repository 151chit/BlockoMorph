package net.blockomorph.mixins.main.system.inPlayerManager;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.storage.InPlayerGameEventListenersStorage;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GameEventDispatcher.class)
public class GameEventDispatcherMixin {
	@Shadow @Final private ServerLevel level;

	@FastInject(method = "post", at = @At("HEAD"))
	private boolean checkCfg(Holder<GameEvent> gameEvent, Vec3 position, GameEvent.Context context) {
		return !InPlayerBlockPos.isMorphedPlayerBlockX(position.x) || Config.get().gameEvents.getValue();
	}

	@ModifyVariable(method = "post", at = @At("STORE"))
	private boolean handleEvents(boolean used, Holder<GameEvent> gameEvent, Vec3 position, GameEvent.Context context, @Local GameEventListenerRegistry.ListenerVisitor visitListeners,
		@Local(ordinal = 1) int minX, @Local(ordinal = 2) int minY, @Local(ordinal = 3) int minZ,
		@Local(ordinal = 4) int maxX, @Local(ordinal = 5) int maxY, @Local(ordinal = 6) int maxZ) {
		if (!Config.get().gameEvents.getValue()) return used;
		used |= InPlayerGameEventListenersStorage.processGameEvents(this.level, gameEvent, position, context, visitListeners, minX, minY, minZ, maxX, maxY, maxZ);
		return used;
	}
}
