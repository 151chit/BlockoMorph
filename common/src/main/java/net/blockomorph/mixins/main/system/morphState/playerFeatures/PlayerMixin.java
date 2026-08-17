package net.blockomorph.mixins.main.system.morphState.playerFeatures;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.utils.accessors.Accessors;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class) @SuppressWarnings("Invalid_FI_return_type")
public abstract class PlayerMixin implements PlayerAccessor {
	@Unique private static final LivingEntity.Fallsounds EMPTY = new LivingEntity.Fallsounds(SoundEvents.EMPTY, SoundEvents.EMPTY);

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	public void rejectAttacks(Entity entity, CallbackInfo ci) {
		if (!Config.get().hitReaction.getValue().hand && entity instanceof PlayerAccessor pl && pl.isBlockomorphActive())
			ci.cancel();
	}

	@ModifyReturnValue(method = "getDeathSound", at = @At("RETURN"))
	protected SoundEvent getDeathSound(SoundEvent original) {
		if (this.isBlockomorphActive()) return null;
		return original;
	}

	@ModifyReturnValue(method = "getHurtSound", at = @At("RETURN"))
	protected SoundEvent getHurtSound(SoundEvent original) {
		if (this.isBlockomorphActive()) return null;
		return original;
	}

	@FastInject(method = "getFallSounds", at = @At("HEAD"))
	protected Object getFallSoundsOverride() {
		if (this.isBlockomorphActive()) return EMPTY;
		return FastInject.CONTINUE_EXECUTION;
	}

	@Inject(method = "causeFallDamage", at = @At("HEAD"))
	public void checkAnvils(float fallDistance, float damageModifier, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
		if (Config.get().fallingBlockEffects.getValue() && this.player().level() instanceof ServerLevel lv && this.size() == 1) {
			BlockInPlayer2 block = this.getBlocksStorage().randomSortedBlockOrThrow();
			if (block.getBlockState().getBlock() instanceof Fallable fl) {
				FallingBlockEntity fallingBlockEntity = FallingBlockEntity.fall(lv, block.getPos(), block.getBlockState());//suppressed level by adapt fix
				if (block.getBlockState().getBlock() instanceof FallingBlock bl) {
					Accessors.FallingBlockAccessor.of(bl).prepareEntity$bm(fallingBlockEntity);
				}
				if (fl instanceof PointedDripstoneBlock) {
					if (block.getBlockState().is(Blocks.POINTED_DRIPSTONE) && block.getBlockState().getValue(PointedDripstoneBlock.TIP_DIRECTION) == Direction.DOWN) {
						fallingBlockEntity.setHurtsEntities(6, 40);
					}
				}
				fallingBlockEntity.causeFallDamage(fallDistance, damageModifier, damageSource);
				BlockState newState = fallingBlockEntity.getBlockState();
				BlockState worldState = lv.getBlockState(BlockPos.containing(MorphMath.getRealBlockPos(this, block.getOffset())));
				if (newState != block.getBlockState()) {
					lv.setBlock(block.getPos(), newState, 3);
				}
				fl.onLand(lv, block.getPos(), newState, worldState, fallingBlockEntity);
			}
		}
	}
}
