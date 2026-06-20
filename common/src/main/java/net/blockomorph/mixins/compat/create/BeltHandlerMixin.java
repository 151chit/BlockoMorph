package net.blockomorph.mixins.compat.create;

import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.content.kinetics.belt.item.BeltConnectorHandler")
public class BeltHandlerMixin {
	@Unique private static DataComponentType<?> FIRST_BLOCK;
	@Unique private static boolean lookuped;

	@Inject(method = "tick", at= @At("HEAD"), cancellable = true)
	private static void check(CallbackInfo ci) {
		initKey();
		if (FIRST_BLOCK == null) return;
		if (GuiUtils.MC.hitResult instanceof BlockHitResult first && GuiUtils.MC.player != null && GuiUtils.MC.screen == null) {
			for (InteractionHand hand : InteractionHand.values()) {
				ItemStack heldItem = GuiUtils.MC.player.getItemInHand(hand);
				if (heldItem.has(FIRST_BLOCK)) {
					if (heldItem.get(FIRST_BLOCK) instanceof BlockPos second &&
							InPlayerBlockPos.isMorphedPlayerX(first.getBlockPos().getX()) != InPlayerBlockPos.isMorphedPlayerX(second.getX())) {
						ci.cancel();
						return;
					}
				}
			}
		}
	}

	@Unique
	private static void initKey() {
		if (FIRST_BLOCK == null && !lookuped) {
			lookuped = true;
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath("create", "belt_first_shaft");
			FIRST_BLOCK = BuiltInRegistries.DATA_COMPONENT_TYPE.get(id);
			if (FIRST_BLOCK == null)
				MorphUtils.LOGGER.error("Unable to find the item ID \"belt_shaft\" from the Create mod, coordinate normalization is disabled, your game will freeze and crash when attempting to connect the shaft from the morph to the world.");
		}
	}
}
