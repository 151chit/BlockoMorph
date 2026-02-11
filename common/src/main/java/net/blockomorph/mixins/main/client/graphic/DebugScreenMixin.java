package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Locale;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenMixin {

	@Shadow
	private HitResult block;

	@Shadow
	private HitResult liquid;

	@Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
	public boolean redirectAddBlock(List<String> instance, Object e) {
		return this.redirectBlockInfo(instance, e, false);
	}

	@Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 5))
	public boolean redirectAddFluid(List<String> instance, Object e) {
		return this.redirectBlockInfo(instance, e, true);
	}

	private boolean redirectBlockInfo(List<String> instance, Object e, boolean fluid) {
		String out = GuiUtils.redirectBlockInfo(fluid, fluid ? this.liquid : block);
		if (out != null) {
			return instance.add(ChatFormatting.UNDERLINE + out);
		}
		return instance.add((String) e);
	}
}
