package net.blockomorph.mixins.main.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.accessors.ServerLevelAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.gameEvent.PlayerDynamicGameEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

@Mixin(GameEventDispatcher.class)
public class EventDispatcherMixin {
	@Shadow @Final private ServerLevel level;

	@ModifyVariable(method = "post", at = @At("STORE"))
	private GameEventListenerRegistry.ListenerVisitor wrapListener(GameEventListenerRegistry.ListenerVisitor orig) {
		if (!enabled()) return orig;
		return new PlayerDynamicGameEventListener.ListenerVisitorWithHook(orig, new HashSet<>());
	}

	@Inject(method = "post", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gameevent/GameEventListenerRegistry;visitInRangeListeners(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;Lnet/minecraft/world/level/gameevent/GameEventListenerRegistry$ListenerVisitor;)Z"))
	private void run(Holder<GameEvent> holder, Vec3 vec3, GameEvent.Context context, CallbackInfo ci, @Local GameEventListenerRegistry.ListenerVisitor visitor, @Local(ordinal = 7) int x, @Local(ordinal = 8) int z, @Local(ordinal = 9) int y) {
		if (enabled() && this.level instanceof ServerLevelAccessor acc && visitor instanceof PlayerDynamicGameEventListener.ListenerVisitorWithHook wrapper) {
			long pos2 = ChunkPos.asLong(x, z);
			var sectionMap = acc.getPlayerGameEventListenerMap().get(pos2);
			if (sectionMap != null) {
				var registry = sectionMap.get(y);
				if (registry != null) {
					registry.visitInRangeListeners(vec3, wrapper);
				}
			}
		}
	}

	@Inject(method = "post", at = @At("HEAD"), cancellable = true)
	private void check(Holder<GameEvent> holder, Vec3 vec3, GameEvent.Context context, CallbackInfo ci) {
		if (InPlayerBlockPos.isMorphedPlayerX(vec3.x) && !enabled()) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "post", at = @At("STORE"))
	private BlockPos normalize(BlockPos orig, Holder<GameEvent> holder, Vec3 vec3) {
		if (!InPlayerBlockPos.isMorphedPlayerX(orig.getX())) {
			return orig;
		}
		return BlockPos.containing(InPlayerBlockPos.checkOnReal(vec3));
	}

	private static boolean enabled() {
		return Config.get().gameEvents.getValue();
	}
}
