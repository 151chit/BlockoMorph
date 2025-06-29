package net.blockomorph.mixins.main.level;

import net.blockomorph.utils.accessors.LevelAcc;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Mixin(Level.class)
public abstract class LevelInjectsMixin {

	@Inject(method = "isInWorldBoundsHorizontal", at = @At(value = "HEAD"), cancellable = true)
	private static void acceptIfPlayerPos(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (InPlayerBlockPos.isMorphedPlayerX(pos.getX()))
			cir.setReturnValue(true);
	}

	@ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
	public BlockState changeState(BlockState original, BlockPos pos) {
		AtomicReference<BlockState> value = new AtomicReference<>(original);
		InPlayerBlockPos.check(pos, (pl, realPos) -> {

			boolean flag = original.getBlock() instanceof LiquidBlock;
			if (pl.isBreaking() && realPos.equals(InPlayerBlockPos.ZERO) && (original == Blocks.AIR.defaultBlockState() || flag)) {
				value.set(Blocks.VOID_AIR.defaultBlockState());
			} else if (flag) {
				value.set(Blocks.AIR.defaultBlockState());
			}
		}, null, LevelAcc.of(this));
		return value.get();
	}

	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
	public void rejectBreak(BlockPos pos, BlockState blockState, int i, int j, CallbackInfoReturnable<Boolean> cir) {
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			if (pl.isOnLoadingBlocks() && blockState == Blocks.AIR.defaultBlockState() && realPos.equals(InPlayerBlockPos.ZERO))
				cir.setReturnValue(false);
		}, () -> cir.setReturnValue(false), LevelAcc.of(this));

	}

	@ModifyVariable(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At(value = "STORE"), ordinal = 2)
	public BlockState getState(BlockState value, BlockPos pos) {
		AtomicReference<BlockState> state = new AtomicReference<>(value);
		InPlayerBlockPos.check(pos, (pl, realPos) -> {
			state.set(pl.getBlockState(realPos));
		}, null, LevelAcc.of(this));
		return state.get();
	}

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("HEAD"))
	private Predicate<Entity> eraseOwner(Predicate<Entity> original, Entity ent, AABB aabb) {
		AtomicReference<Player> player = new AtomicReference<>();
		InPlayerBlockPos.check(BlockPos.containing(aabb.getCenter()), (pl, realPos) -> {
			player.set(pl.player());
		}, null, LevelAcc.of(this));
		return original.and((entity -> entity != player.get()));
	}

	@ModifyVariable(method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V", at = @At("HEAD"))
	private <T extends Entity> Predicate<Entity> eraseOwner(Predicate<Entity> original, EntityTypeTest<Entity, T> type, AABB aabb) {
		AtomicReference<Player> player = new AtomicReference<>();
		InPlayerBlockPos.check(BlockPos.containing(aabb.getCenter()), (pl, realPos) -> {
			player.set(pl.player());
		}, null, LevelAcc.of(this));
		return original.and((entity -> entity != player.get()));
	}
}
