package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.accessors.ClipContextAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClipContext.class)
public class ClipContextMixin implements ClipContextAccessor {

    @Shadow @Final private CollisionContext collisionContext;
    @Shadow @Final private ClipContext.Block block;

    @Shadow @Final private Vec3 from;

    @Shadow @Final private Vec3 to;

    @Shadow @Final private ClipContext.Fluid fluid;

    public CollisionContext getContext() {
        return this.collisionContext;
    }

    public ClipContext.Block getMode() {
        return this.block;
    }

    public ClipContext normalize() {
        return new ClipContext(InPlayerBlockPos.checkOnReal(this.from), InPlayerBlockPos.checkOnReal(this.to), this.block, this.fluid, ((EntityCollisionContext) this.collisionContext).getEntity());
    }
}
