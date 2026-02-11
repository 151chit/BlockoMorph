package net.blockomorph.mixins.main.level;

import com.google.common.collect.ImmutableList;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.ClipContextAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.hit.PlayerHitResult;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(value = {ClientLevel.class, ServerLevel.class}, priority = 1001)
public abstract class LevelOverrideMixin extends Level {
	protected LevelOverrideMixin(WritableLevelData p_270739_, ResourceKey<Level> p_270683_, RegistryAccess p_270200_, Holder<DimensionType> p_270240_, Supplier<ProfilerFiller> p_270692_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_) {
		super(p_270739_, p_270683_, p_270200_, p_270240_, p_270692_, p_270904_, p_270470_, p_270248_, p_270466_);
	}

	@Override
	public BlockHitResult clip(ClipContext ctx) {
		ctx = ClipContextAccessor.of(ctx).normalize();
		AtomicReference<BlockHitResult> res = new AtomicReference<>(super.clip(ctx));
		if (MorphUtils.needModedHit(clazz -> {
			return !BlockGetter.class.isAssignableFrom(clazz);
		}))
			PlayerHitResult.checkHitResult(res.get().getLocation(), ctx, res::set);
		return res.get();
	}

	@Override
	public BlockHitResult isBlockInLine(ClipBlockStateContext clipBlockStateContext) {
		ClipBlockStateContext ctx = new ClipBlockStateContext(
				InPlayerBlockPos.checkOnReal(clipBlockStateContext.getFrom()),
				InPlayerBlockPos.checkOnReal(clipBlockStateContext.getTo()),
				clipBlockStateContext.isTargetBlock());
		AtomicReference<BlockHitResult> res = new AtomicReference<>(super.isBlockInLine(ctx));
		PlayerHitResult.checkHitResult(this, res.get().getLocation(), ctx, res::set);
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
		MorphUtils.fillListWithPlayerCollisions(this, entity, aabb, shapes);
		ImmutableList.Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(shapes.size());
		return builder.addAll(shapes).build();
	}
}
