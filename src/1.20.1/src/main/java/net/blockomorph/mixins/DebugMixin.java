package net.blockomorph.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BellBlock.class)
public abstract class DebugMixin {

    @Shadow protected abstract boolean isProperHit(BlockState p_49740_, Direction p_49741_, double p_49742_);

    @Inject(method = "onHit", at = @At(value = "HEAD"))
    public void hit(Level p_49702_, BlockState p_49703_, BlockHitResult p_49704_, Player p_49705_, boolean p_49706_, CallbackInfoReturnable<Boolean> cir) {
        Direction direction = p_49704_.getDirection();
        BlockPos blockpos = p_49704_.getBlockPos();
        boolean flag = this.isProperHit(p_49703_, direction, p_49704_.getLocation().y - (double)blockpos.getY());
        System.out.println(flag);
    }
}
