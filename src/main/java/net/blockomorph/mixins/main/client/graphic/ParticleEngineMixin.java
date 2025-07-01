package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow protected ClientLevel level;
    @Shadow @Final private RandomSource random;
    @Shadow public abstract void add(Particle p_107345_);

    @Inject(method = "destroy", at = @At(value = "HEAD"), cancellable = true)
    public void destroy(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (InPlayerBlockPos.isMorphedPlayerX(pos.getX())) {
            Vec3 vec = InPlayerBlockPos.checkOnReal(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
            this.destroyBlock(pos, state, vec);
            ci.cancel();
        }
    }

    @Inject(method = "crack", at = @At(value = "HEAD"), cancellable = true)
    public void crack(BlockPos pos, Direction dir, CallbackInfo ci) {
        if (InPlayerBlockPos.isMorphedPlayerX(pos.getX())) {
            Vec3 vec = InPlayerBlockPos.checkOnReal(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
            this.crackBlock(pos, dir, vec);
            ci.cancel();
        }
    }

    @Unique
    public void crackBlock(BlockPos pos, Direction dir, Vec3 realPos) {
        BlockState blockstate = this.level.getBlockState(pos);
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            double i = realPos.x;
            double j = realPos.y;
            double k = realPos.z;
            float f = 0.1F;
            double d = 0.2D;
            AABB aabb = blockstate.getShape(this.level, pos).bounds();
            double d0 = i + this.random.nextDouble() * (aabb.maxX - aabb.minX - d) + f + aabb.minX;
            double d1 = j + this.random.nextDouble() * (aabb.maxY - aabb.minY - d) + f + aabb.minY;
            double d2 = k + this.random.nextDouble() * (aabb.maxZ - aabb.minZ - d) + f + aabb.minZ;
            if (dir == Direction.DOWN) {
                d1 = j + aabb.minY - f;
            } else if (dir == Direction.UP) {
                d1 = j + aabb.maxY + f;
            } else if (dir == Direction.NORTH) {
                d2 = k + aabb.minZ - f;
            } else if (dir == Direction.SOUTH) {
                d2 = k + aabb.maxZ + f;
            } else if (dir == Direction.WEST) {
                d0 = i + aabb.minX - f;
            } else if (dir == Direction.EAST) {
                d0 = i + aabb.maxX + f;
            }

            this.add((new TerrainParticle(this.level, d0, d1, d2, 0.0D, 0.0D, 0.0D, blockstate, BlockPos.containing(realPos)).updateSprite(blockstate, pos)).setPower(0.2F).scale(0.6F));
        }
    }

    public void destroyBlock(BlockPos pos, BlockState state, Vec3 realPos) {
        var manager = (ParticleEngine)(Object)this;
        if (!state.isAir() && !IClientBlockExtensions.of(state).addDestroyEffects(state, this.level, pos, manager)) {
            VoxelShape voxelshape = state.getShape(this.level, pos);
            voxelshape.forAllBoxes((p_172273_, p_172274_, p_172275_, p_172276_, p_172277_, p_172278_) -> {
                double d1 = Math.min(1.0D, p_172276_ - p_172273_);
                double d2 = Math.min(1.0D, p_172277_ - p_172274_);
                double d3 = Math.min(1.0D, p_172278_ - p_172275_);
                int i = Math.max(2, Mth.ceil(d1 / 0.25D));
                int j = Math.max(2, Mth.ceil(d2 / 0.25D));
                int k = Math.max(2, Mth.ceil(d3 / 0.25D));

                for(int l = 0; l < i; ++l) {
                    for(int i1 = 0; i1 < j; ++i1) {
                        for(int j1 = 0; j1 < k; ++j1) {
                            double d4 = ((double)l + 0.5D) / (double)i;
                            double d5 = ((double)i1 + 0.5D) / (double)j;
                            double d6 = ((double)j1 + 0.5D) / (double)k;
                            double d7 = d4 * d1 + p_172273_;
                            double d8 = d5 * d2 + p_172274_;
                            double d9 = d6 * d3 + p_172275_;
                            this.add(new TerrainParticle(
                                    this.level,
                                    realPos.x + d7,
                                    realPos.y + d8,
                                    realPos.z + d9,
                                    d4 - 0.5D,
                                    d5 - 0.5D,
                                    d6 - 0.5D,
                                    state,
                                    BlockPos.containing(realPos)
                            ).updateSprite(state, pos));
                        }
                    }
                }

            });
        }
    }
}
