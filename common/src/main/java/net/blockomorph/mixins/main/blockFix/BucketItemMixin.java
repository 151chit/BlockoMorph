package net.blockomorph.mixins.main.blockFix;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.utils.DamageHandler;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketItemMixin {

	@Shadow @Final private Fluid content;

	@Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
	public void prePick(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir, @Local BlockHitResult hitResult) {
		if (hitResult instanceof MorphedPlayerHitResult mHit) {
			mHit.getPlayer().breakingModeStart(true);//prevent unmorph after die
		}
	}

	@Inject(method = "use", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
	public void postPick(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir, @Local BlockHitResult hitResult) {
		if (hitResult instanceof MorphedPlayerHitResult mHit) {
			PlayerAccessor pl = mHit.getPlayer();
			boolean flag = pl.isBreaking();
			pl.breakingModeStart(false);
			if (!level.isClientSide() && flag && pl.getBlocksData2().size() == 1 && pl.getBlockState(InPlayerBlockPos.ZERO) == Blocks.VOID_AIR.defaultBlockState()) {
				DamageHandler.destroy(pl, player);
			}
		}
	}

	@ModifyVariable(method = "use", at = @At("STORE"))
	public BlockHitResult placeOut(BlockHitResult hitResult) {
		if (hitResult instanceof MorphedPlayerHitResult playerHitResult && this.content != Fluids.EMPTY) {
			ConfigEnums.PlaceMode mode = Config.get().placeMode.getValue();
			Vec3 vec = playerHitResult.getRealLocation();
			Vec3 tolerance = Vec3.atLowerCornerOf(playerHitResult.getDirection().getUnitVec3i()).scale(-1.0E-7);
			BlockPos pos = BlockPos.containing(vec.add(tolerance));
			if (mode == ConfigEnums.PlaceMode.OUT) {
				return new BlockHitResult(vec, playerHitResult.getDirection(), pos, playerHitResult.isInside());
			} else if (mode == ConfigEnums.PlaceMode.DISABLED) {
				return BlockHitResult.miss(vec, playerHitResult.getDirection(), pos);
			}
		}
		return hitResult;
	}
}
