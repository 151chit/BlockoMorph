package net.blockomorph.mixins.main.level;

import com.google.common.collect.ImmutableList;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.ClipContextAccessor;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.blockomorph.utils.hit.PlayerHitResult;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(value = {ClientLevel.class, ServerLevel.class}, priority = 1001)
public abstract class LevelOverrideMixin extends Level {
    private static final AABB CUBE = Shapes.block().bounds().inflate(1);

    protected LevelOverrideMixin(WritableLevelData p_270739_, ResourceKey<Level> p_270683_, RegistryAccess p_270200_, Holder<DimensionType> p_270240_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_) {
        super(p_270739_, p_270683_, p_270200_, p_270240_, p_270904_, p_270470_, p_270248_, p_270466_);
    }

    @Override
    public BlockHitResult clip(ClipContext ctx) {
        ctx = ClipContextAccessor.of(ctx).normalize();
        AtomicReference<BlockHitResult> res = new AtomicReference<>(super.clip(ctx));
        PlayerHitResult.checkHitResult(res.get().getLocation(), ctx, res::set);
        return res.get();
    }

    @Override
    public Player getNearestPlayer(double x, double y, double z, double reach, @Nullable Predicate<Entity> predicate) {
        AtomicReference<Player> player = new AtomicReference<>();
        InPlayerBlockPos.check(BlockPos.containing(x, y, z), (pl, realPos) -> {
            player.set(pl.player());
        }, null, this);
        return super.getNearestPlayer(x, y, z, reach, predicate != null ? predicate.and((entity) -> entity != player.get()) : null);
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB aabb) {
        List<VoxelShape> shapes = new ArrayList<>(super.getEntityCollisions(entity, aabb));
        if (!(aabb.getSize() < 1.0E-7D)) {
            this.getEntities().get(aabb.inflate(2), (entity1 -> {
                if (entity1 != entity && EntitySelector.NO_SPECTATORS.test(entity1) && entity1 instanceof PlayerAccessor pl && pl.isFullActive()) {
                    this.addCustomShapes(shapes, pl, aabb);
                }
            }));
        }
        ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(shapes.size());
        return builder.addAll(shapes).build();
    }

    @Unique
    private void addCustomShapes(List<VoxelShape> builder, PlayerAccessor pl, AABB aabb) {
        List<VoxelShape> blocks = new ArrayList<>();
        AABB aabbPl = aabb.inflate(1.0E-7);
        for (InPlayerBlockPos offset : pl.getBlocksData2().keySet()) {
            Vec3 realpos = MorphUtils.getRealBlockPos(pl, offset);
            AABB movedCube = CUBE.move(realpos);
            if (aabbPl.intersects(movedCube)) {
                blocks.add(pl.getShape(offset, realpos));
            }
        }
        builder.addAll(blocks);
    }

    @Override
    public boolean noCollision(@Nullable Entity entity, AABB aabb) { //TODO
        aabb = InPlayerBlockPos.checkOnReal(aabb);
        for(VoxelShape voxelshape : this.getBlockCollisions(entity, aabb)) {
            if (!voxelshape.isEmpty()) {
                return false;
            }
        }
        for (VoxelShape entityShape : this.getEntityCollisions(entity, aabb)) {
            if (Shapes.joinIsNotEmpty(entityShape, Shapes.create(aabb), BooleanOp.AND)) {
                return false;
            }
        }
        if (entity == null) {
            return true;
        } else {
            WorldBorder worldborder = this.getWorldBorder();
            VoxelShape voxelshape1 = worldborder.isInsideCloseToBorder(entity, aabb) ? worldborder.getCollisionShape() : null;;
            return voxelshape1 == null || !Shapes.joinIsNotEmpty(voxelshape1, Shapes.create(aabb), BooleanOp.AND);
        }
    }
}
