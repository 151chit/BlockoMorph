package net.blockomorph.mixins.main.level;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelTicks.class)
public class LevelTicksMixin<T> {

    @Shadow @Final private Long2ObjectMap<LevelChunkTicks<T>> allContainers;

    @Shadow @Final private Long2LongMap nextTickForContainer;

    @Inject(method = "schedule", at = @At(value = "HEAD"), cancellable = true)
    public void schedule(ScheduledTick<T> tick, CallbackInfo ci) {
        InPlayerBlockPos.check(tick.pos(), (pl, realPos) -> {
            ci.cancel();
            BlockPos pos = BlockPos.containing(MorphUtils.getRealBlockPos(pl, realPos));
            long i = ChunkPos.asLong(pos);
            LevelChunkTicks<T> levelchunkticks = this.allContainers.get(i);
            if (levelchunkticks != null) {
                levelchunkticks.schedule(tick);
            }
        }, ci::cancel, false);
    }

    @Inject(method = "updateContainerScheduling", at = @At(value = "HEAD"), cancellable = true)
    public void add(ScheduledTick<T> tick, CallbackInfo ci) {
        InPlayerBlockPos.check(tick.pos(), (pl, realPos) -> {
            ci.cancel();
            BlockPos pos = BlockPos.containing(MorphUtils.getRealBlockPos(pl, realPos));
            long i = ChunkPos.asLong(pos);
            nextTickForContainer.put(i, tick.triggerTick());
        }, ci::cancel, false);
    }
}
