package net.blockomorph.mixins.main.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(value = DebugScreenOverlay.class)
public class HitResultF3Mixin {
	@Shadow private HitResult block;
	@Shadow private HitResult liquid;

	@WrapOperation(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
	public boolean redirectAddBlock(List<String> instance, Object e, Operation<Boolean> original) {
		return this.redirectBlockInfo(instance, e, false);
	}

	@WrapOperation(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 5))
	public boolean redirectAddFluid(List<String> instance, Object e, Operation<Boolean> original) {
		return this.redirectBlockInfo(instance, e, true);
	}

	@Unique
	private boolean redirectBlockInfo(List<String> instance, Object e, boolean fluid) {
		String out = GuiUtils.hideMorphedBlocksKeyPos(fluid, fluid ? this.liquid : block);
		if (out != null) {
			return instance.add(ChatFormatting.UNDERLINE + out);
		}
		return instance.add((String) e);
	}
}