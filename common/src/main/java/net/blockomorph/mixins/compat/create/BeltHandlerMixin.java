package net.blockomorph.mixins.compat.create;

import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.belt.item.BeltConnectorHandler", remap = false)
public class BeltHandlerMixin {

	@Inject(method = "tick", at= @At("HEAD"), cancellable = true)
	private static void check(CallbackInfo ci) {
		if (GuiUtils.MC.hitResult instanceof BlockHitResult first && GuiUtils.MC.player != null && GuiUtils.MC.screen == null) {
			for (InteractionHand hand : InteractionHand.values()) {
				ItemStack heldItem = GuiUtils.MC.player.getItemInHand(hand);
				if (heldItem.hasTag()) {
					CompoundTag tag = heldItem.getTag();
					if (tag != null && tag.contains("FirstPulley")) {
						BlockPos second = NbtUtils.readBlockPos(tag.getCompound("FirstPulley"));
						if (InPlayerBlockPos.isMorphedPlayerX(first.getBlockPos().getX()) != InPlayerBlockPos.isMorphedPlayerX(second.getX())) {
							ci.cancel();
							return;
						}
					}
				}
			}
		}
	}
}
