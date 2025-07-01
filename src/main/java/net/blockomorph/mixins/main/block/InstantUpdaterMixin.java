package net.blockomorph.mixins.main.block;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.InstantNeighborUpdater;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(InstantNeighborUpdater.class)
public class InstantUpdaterMixin {

    @Shadow @Final private Level level;

    @Inject(method = "shapeUpdate", at = @At("HEAD"), cancellable = true)
    public void redirectUpdate(Direction direction, BlockState state, BlockPos pos, BlockPos neighborPos, int updateFlags, int updateLimit, CallbackInfo ci) {
        InPlayerBlockPos.check(pos, (pl, realPos) -> {
            ci.cancel();
            MorphUtils.executeMorphedBlockShapeUpdate(this.level, direction, pos, neighborPos, state, updateFlags, updateLimit, pl.getBlockState(realPos));
        }, null, this.level);
    }

    @ModifyVariable(method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;)V", at = @At("HEAD"), ordinal = 0)
    public BlockPos changePos(BlockPos orig) {
        AtomicReference<BlockPos> value = new AtomicReference<>(orig);
        InPlayerBlockPos.check(orig, (pl, realPos) -> {
            if (!pl.getBlocksData2().containsKey(realPos)) {
                value.set(InPlayerBlockPos.checkOnReal(orig));
            }
        }, null, this.level);
        return value.get();
    }
}
