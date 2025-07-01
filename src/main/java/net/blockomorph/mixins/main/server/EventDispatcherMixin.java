package net.blockomorph.mixins.main.server;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameEventDispatcher.class)
public class EventDispatcherMixin {

    @Shadow @Final private ServerLevel level;

    @Inject(method = "post", at = @At(value = "HEAD"), cancellable = true)
    public void handle(Holder<GameEvent> p_316151_, Vec3 vec, GameEvent.Context p_251777_, CallbackInfo ci) {
        BlockPos pos = BlockPos.containing(vec);
        InPlayerBlockPos.check(pos, (pl, realPos) -> {
            ci.cancel();
            //TODO!!!!!!!!!!!
        }, ci::cancel, this.level);
    }
}
